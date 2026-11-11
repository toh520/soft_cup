package com.toh520.campus.module.nutrition.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.toh520.campus.common.BusinessException;
import com.toh520.campus.integration.llm.ChatModelClient;
import com.toh520.campus.module.nutrition.model.MealRequirements;
import com.toh520.campus.module.nutrition.model.NutrientRange;
import java.util.*;
import org.springframework.stereotype.Service;

/** 模型只把本餐自然语言要求转换为约束；所有数量、菜品 ID 和冲突由服务端校验。 */
@Service
public class MealPreferenceService {
  private final ChatModelClient chat;

  public MealPreferenceService(ChatModelClient chat) {
    this.chat = chat;
  }

  public MealRequirements parse(String text, List<Map<String, Object>> menu) {
    if (text.isBlank()) return MealRequirements.defaults();
    var input =
        menu.stream()
            .map(
                d ->
                    Map.of(
                        "id",
                        d.get("id"),
                        "name",
                        d.get("name"),
                        "category",
                        d.get("category"),
                        "isVegetarian",
                        d.get("isVegetarian"),
                        "ingredientTags",
                        d.get("ingredientTags"),
                        "energyKcal",
                        d.get("energyKcal"),
                        "proteinG",
                        d.get("proteinG"),
                        "fatG",
                        d.get("fatG"),
                        "carbohydrateG",
                        d.get("carbohydrateG")))
            .toList();
    var result =
        chat.complete("nutrition/requirements", Map.of("requirements", text, "menu", input));
    if (!result.path("message").isTextual() || result.path("message").asText().length() > 300)
      throw invalid();
    if (!result.path("message").asText().isBlank())
      throw new BusinessException(400, "本次用餐要求无法满足：" + result.path("message").asText());
    int staple = count(result, "stapleCount", 0, 1);
    int sides = count(result, "sideCount", 0, 4);
    int soup = count(result, "soupCount", 0, 1);
    int meat = count(result, "meatCount", -1, sides);
    if (staple + sides + soup == 0 || !result.path("vegetarianOnly").isBoolean()) throw invalid();
    var stapleIds = ids(result, "stapleIds", menu);
    var required = ids(result, "requiredDishIds", menu);
    var excluded = ids(result, "excludedDishIds", menu);
    boolean vegetarian = result.path("vegetarianOnly").asBoolean();
    if (!result.path("veganOnly").isBoolean()) throw invalid();
    boolean vegan = result.path("veganOnly").asBoolean();
    var groups = new ArrayList<Set<Long>>();
    if (!result.path("requiredAnyGroups").isArray() || result.path("requiredAnyGroups").size() > 6)
      throw invalid();
    for (var group : result.path("requiredAnyGroups")) {
      var values = idValues(group, menu);
      if (values.isEmpty()) throw invalid();
      groups.add(values);
    }
    var ingredients = new ArrayList<String>();
    if (!result.path("excludedIngredients").isArray()
        || result.path("excludedIngredients").size() > 30) throw invalid();
    for (var value : result.path("excludedIngredients")) {
      if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > 30)
        throw invalid();
      ingredients.add(value.asText().trim());
    }
    var limits = nutritionLimits(result.path("nutritionLimits"));
    if ((!stapleIds.isEmpty() && staple == 0)
        || !Collections.disjoint(required, excluded)
        || (!stapleIds.isEmpty() && excluded.containsAll(stapleIds))
        || ((vegetarian || vegan) && meat > 0)
        || groups.stream().anyMatch(excluded::containsAll)
        || required.size() > staple + sides + soup
        || menu.stream()
            .anyMatch(d -> stapleIds.contains(id(d)) && !d.get("category").equals("staple")))
      throw new BusinessException(400, "本次用餐要求存在冲突，请调整主食、荤素或数量后重试");
    return new MealRequirements(
        staple,
        sides,
        soup,
        meat,
        stapleIds,
        required,
        excluded,
        vegetarian || vegan,
        false,
        List.copyOf(groups),
        Map.copyOf(limits),
        List.copyOf(ingredients),
        vegan);
  }

  private int count(JsonNode node, String name, int min, int max) {
    var value = node.path(name);
    if (!value.isIntegralNumber()
        || !value.canConvertToInt()
        || value.asInt() < min
        || value.asInt() > max) throw invalid();
    return value.asInt();
  }

  private Set<Long> ids(JsonNode node, String name, List<Map<String, Object>> menu) {
    return idValues(node.path(name), menu);
  }

  private Set<Long> idValues(JsonNode values, List<Map<String, Object>> menu) {
    if (!values.isArray() || values.size() > menu.size()) throw invalid();
    var result = new HashSet<Long>();
    for (var value : values) {
      if (!value.isIntegralNumber()
          || !value.canConvertToLong()
          || menu.stream().noneMatch(d -> id(d) == value.asLong())
          || !result.add(value.asLong())) throw invalid();
    }
    return Set.copyOf(result);
  }

  private Map<String, NutrientRange> nutritionLimits(JsonNode node) {
    if (!node.isObject()) throw invalid();
    var result = new LinkedHashMap<String, NutrientRange>();
    node.properties()
        .forEach(
            entry -> {
              if (!List.of("energyKcal", "proteinG", "fatG", "carbohydrateG")
                  .contains(entry.getKey())) throw invalid();
              var range = entry.getValue();
              if (!range.isObject() || range.size() != 2 || !range.has("min") || !range.has("max"))
                throw invalid();
              Double min = bound(range.get("min"));
              Double max = bound(range.get("max"));
              if (min == null && max == null) throw invalid();
              if (min != null && max != null && min > max)
                throw new BusinessException(400, "本次营养上下限存在冲突，请调整后重试");
              result.put(entry.getKey(), new NutrientRange(min, max));
            });
    return result;
  }

  private Double bound(JsonNode node) {
    if (node.isNull()) return null;
    if (!node.isNumber()
        || !Double.isFinite(node.asDouble())
        || node.asDouble() < 0
        || node.asDouble() > 10000) throw invalid();
    return node.asDouble();
  }

  /** 条件摘要来自已校验的结构和真实菜名，帮助用户发现自然语言理解偏差。 */
  public String describe(MealRequirements rules, List<Map<String, Object>> menu) {
    var parts = new ArrayList<String>();
    parts.add(
        "主食" + rules.stapleCount() + "份、配菜" + rules.sideCount() + "道、汤" + rules.soupCount() + "份");
    if (rules.meatCount() >= 0)
      parts.add("配菜" + rules.meatCount() + "荤" + (rules.sideCount() - rules.meatCount()) + "素");
    if (rules.veganOnly()) parts.add("纯素（排除蛋奶蜂蜜）");
    else if (rules.vegetarianOnly()) parts.add("全部素食");
    if (!rules.stapleIds().isEmpty()) parts.add("主食选" + names(rules.stapleIds(), menu));
    if (!rules.requiredDishIds().isEmpty()) parts.add("必含" + names(rules.requiredDishIds(), menu));
    for (var group : rules.requiredAnyGroups()) parts.add("至少包含其一：" + names(group, menu));
    if (!rules.excludedDishIds().isEmpty()) parts.add("排除" + names(rules.excludedDishIds(), menu));
    if (!rules.excludedIngredients().isEmpty())
      parts.add("忌口" + String.join("、", rules.excludedIngredients()));
    var labels =
        Map.of(
            "energyKcal",
            "热量(kcal)",
            "proteinG",
            "蛋白质(g)",
            "fatG",
            "脂肪(g)",
            "carbohydrateG",
            "碳水(g)");
    rules.nutritionLimits().entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              var range = entry.getValue();
              parts.add(
                  labels.get(entry.getKey())
                      + (range.min() == null ? "" : "≥" + range.min())
                      + (range.max() == null ? "" : "≤" + range.max()));
            });
    return String.join("；", parts);
  }

  private String names(Set<Long> ids, List<Map<String, Object>> menu) {
    return String.join(
        "、",
        menu.stream().filter(d -> ids.contains(id(d))).map(d -> (String) d.get("name")).toList());
  }

  private long id(Map<String, Object> dish) {
    return ((Number) dish.get("id")).longValue();
  }

  private BusinessException invalid() {
    return new BusinessException(502, "用餐要求解析无效，请换一种描述后重试");
  }
}
