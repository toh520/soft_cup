package com.toh520.campus.module.nutrition.service;

import com.toh520.campus.common.*;
import com.toh520.campus.integration.llm.ChatModelClient;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.nutrition.dto.DishRequest;
import com.toh520.campus.module.nutrition.repository.DishRepository;
import com.toh520.campus.module.user.service.UserService;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

/** 身体检查与禁忌过滤在调用模型前完成；模型只选择合法组合，营养合计由后端生成。 */
@Service
public class NutritionService {
  private final DishRepository dishes;
  private final UserService users;
  private final ChatModelClient chat;
  private final JsonSupport json;

  public NutritionService(
      DishRepository dishes, UserService users, ChatModelClient chat, JsonSupport json) {
    this.dishes = dishes;
    this.users = users;
    this.chat = chat;
    this.json = json;
  }

  public Object readiness(LoginSession session) {
    session.requireStudent();
    var profile = users.profile(session);
    var missing = missing(profile);
    return Map.of("ready", missing.isEmpty(), "missing", missing);
  }

  private List<String> missing(Map<String, Object> profile) {
    var result = new ArrayList<String>();
    String[][] fields = {
      {"age", "年龄"},
      {"sex", "生理性别"},
      {"heightCm", "身高"},
      {"weightKg", "体重"},
      {"activityLevel", "活动水平"}
    };
    for (var field : fields) if (profile.get(field[0]) == null) result.add(field[1]);
    return result;
  }

  public Object recommend(LoginSession session) {
    session.requireStudent();
    var profile = users.profile(session);
    var missing = missing(profile);
    if (!missing.isEmpty())
      throw new BusinessException(400, "请先前往个人中心补全：" + String.join("、", missing));
    // Mifflin-St Jeor 成年公式，kg/cm/岁；活动系数及单餐 35%、±25% 为课设估算默认值。
    double base =
        10 * number(profile, "weightKg")
            + 6.25 * number(profile, "heightCm")
            - 5 * number(profile, "age")
            + ("male".equals(profile.get("sex")) ? 5 : -161);
    double factor =
        Map.of("low", 1.2, "medium", 1.55, "high", 1.725)
            .get((String) profile.get("activityLevel"));
    double target = base * factor * 0.35;
    @SuppressWarnings("unchecked")
    List<String> avoided = (List<String>) profile.get("avoidedIngredients");
    var available =
        dishes.all().stream()
            .filter(d -> Boolean.TRUE.equals(d.get("isAvailable")))
            .filter(
                d ->
                    !"vegetarian".equals(profile.get("dietPreference"))
                        || Boolean.TRUE.equals(d.get("isVegetarian")))
            .filter(d -> allowed(d, avoided))
            .toList();
    var candidates = new ArrayList<Map<String, Object>>();
    for (var staple : available)
      if (staple.get("category").equals("staple"))
        for (var protein : available)
          if (protein.get("category").equals("protein"))
            for (var vegetable : available)
              if (vegetable.get("category").equals("vegetable")) {
                var selected = List.of(staple, protein, vegetable);
                var totals = totals(selected);
                double energy = totals.get("energyKcal");
                if (energy >= target * 0.75 && energy <= target * 1.25) {
                  var candidate = new LinkedHashMap<String, Object>();
                  candidate.put("candidateId", UUID.randomUUID().toString());
                  candidate.put("dishes", selected);
                  candidate.put("totals", totals);
                  candidates.add(candidate);
                }
              }
    candidates.sort(
        Comparator.comparingDouble(
            c -> Math.abs(((Map<String, Double>) c.get("totals")).get("energyKcal") - target)));
    var limited = candidates.stream().limit(12).toList();
    BusinessException.require(limited.size() >= 3, "符合身体、饮食及供应条件的套餐不足三个，请调整偏好或联系管理员补充菜品");
    // 候选已完成禁忌与能量校验；仅向模型发送选择所需信息，避免重复完整菜品数据导致超时。
    var modelCandidates =
        limited.stream()
            .map(
                candidate ->
                    Map.of(
                        "candidateId", candidate.get("candidateId"),
                        "dishes",
                            ((List<Map<String, Object>>) candidate.get("dishes"))
                                .stream()
                                    .map(
                                        dish ->
                                            Map.of(
                                                "name",
                                                dish.get("name"),
                                                "category",
                                                dish.get("category")))
                                    .toList(),
                        "totals", candidate.get("totals")))
            .toList();
    var response =
        chat.complete(
            "nutrition/recommend",
            Map.of("targetEnergyKcal", target, "candidates", modelCandidates));
    if (!response.path("plans").isArray() || response.path("plans").size() != 3)
      throw new BusinessException(502, "推荐结构无效，请重试");
    var seen = new HashSet<String>();
    var plans = new ArrayList<Map<String, Object>>();
    for (var choice : response.path("plans")) {
      String candidateId = choice.path("candidateId").asText("");
      String reason = choice.path("reason").asText("");
      var candidate =
          limited.stream()
              .filter(c -> candidateId.equals(c.get("candidateId")))
              .findFirst()
              .orElseThrow(() -> new BusinessException(502, "推荐包含未知菜品组合，请重试"));
      if (!seen.add(candidateId) || reason.isBlank() || reason.length() > 1000)
        throw new BusinessException(502, "推荐重复或理由无效，请重试");
      var plan = new LinkedHashMap<>(candidate);
      plan.remove("candidateId");
      plan.put("id", UUID.randomUUID().toString());
      plan.put("reason", reason);
      plan.put("expiresAt", Instant.now().plusSeconds(1800).toString());
      plan.put("fingerprint", fingerprint(profile, (List<Map<String, Object>>) plan.get("dishes")));
      plans.add(plan);
    }
    synchronized (session) {
      session.mealPlans = List.copyOf(plans);
    }
    return Map.of("plans", plans, "targetEnergyKcal", Math.round(target));
  }

  /** 手动点餐只读取当前供应菜品，不依赖身体资料或模型服务。 */
  public List<Map<String, Object>> availableDishes(LoginSession session) {
    session.requireStudent();
    return dishes.all().stream()
        .filter(dish -> Boolean.TRUE.equals(dish.get("isAvailable")))
        .toList();
  }

  /** 手动餐按每份快照逐项复查，身体信息变化不影响用户自主选择。 */
  public void validateManualPlan(LoginSession session, Map<String, Object> plan) {
    session.requireStudent();
    var current = availableDishes(session);
    var selected = (List<Map<String, Object>>) plan.get("dishes");
    for (var item : selected) {
      var stored = new LinkedHashMap<>(item);
      stored.remove("quantity");
      var latest =
          current.stream()
              .filter(dish -> dish.get("id").equals(item.get("id")))
              .findFirst()
              .orElseThrow(() -> new BusinessException(409, "菜品已下架或删除，请移除后重新选择"));
      if (!latest.equals(stored)) throw new BusinessException(409, "菜品资料已变化，请移除后重新选择");
    }
  }

  private boolean allowed(Map<String, Object> dish, List<String> avoided) {
    var tags = (List<String>) dish.get("ingredientTags");
    return tags.stream()
        .noneMatch(
            tag -> avoided.stream().anyMatch(item -> tag.trim().equalsIgnoreCase(item.trim())));
  }

  private String fingerprint(Map<String, Object> profile, List<Map<String, Object>> selected) {
    var body = new LinkedHashMap<String, Object>();
    for (String field :
        List.of(
            "age",
            "sex",
            "heightCm",
            "weightKg",
            "activityLevel",
            "dietPreference",
            "avoidedIngredients")) body.put(field, profile.get(field));
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(
                      json.write(List.of(body, selected))
                          .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** 加入和下单均重读资料与菜品；返回服务端快照，不接受客户端提供的数值。 */
  public void validatePlan(LoginSession session, Map<String, Object> plan) {
    if (Instant.parse((String) plan.get("expiresAt")).isBefore(Instant.now()))
      throw new BusinessException(409, "套餐已过期，请重新推荐");
    var selected = (List<Map<String, Object>>) plan.get("dishes");
    var current = new ArrayList<Map<String, Object>>();
    var all = dishes.all();
    for (var old : selected) {
      var dish =
          all.stream()
              .filter(d -> d.get("id").equals(old.get("id")))
              .findFirst()
              .orElseThrow(() -> new BusinessException(409, "套餐菜品已删除，请重新推荐"));
      current.add(dish);
    }
    if (!fingerprint(users.profile(session), current).equals(plan.get("fingerprint")))
      throw new BusinessException(409, "身体信息或菜品已变更，请重新推荐");
  }

  public static Map<String, Double> totals(List<Map<String, Object>> selected) {
    var result = new LinkedHashMap<String, Double>();
    for (String field : List.of("energyKcal", "proteinG", "fatG", "carbohydrateG"))
      result.put(
          field,
          Math.round(selected.stream().mapToDouble(d -> number(d, field)).sum() * 10) / 10.0);
    return result;
  }

  private static double number(Map<String, Object> data, String field) {
    return ((Number) data.get(field)).doubleValue();
  }

  public Object all(LoginSession session) {
    session.requireAdmin();
    return dishes.all();
  }

  public void save(LoginSession session, Long id, DishRequest request) {
    session.requireAdmin();
    BusinessException.require(
        Double.isFinite(request.portionG())
            && Double.isFinite(request.energyKcal())
            && Double.isFinite(request.proteinG())
            && Double.isFinite(request.fatG())
            && Double.isFinite(request.carbohydrateG()),
        "营养数据必须为有限数值");
    BusinessException.require(
        request.proteinG() + request.fatG() + request.carbohydrateG() <= request.portionG(),
        "三大营养素克数合计不能大于份重");
    dishes.save(id, request);
  }

  public void delete(LoginSession session, long id) {
    session.requireAdmin();
    dishes.delete(id);
  }
}
