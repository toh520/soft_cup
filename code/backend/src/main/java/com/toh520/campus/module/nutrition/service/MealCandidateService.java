package com.toh520.campus.module.nutrition.service;

import com.toh520.campus.common.BusinessException;
import com.toh520.campus.module.nutrition.model.MealRequirements;
import java.util.*;
import org.springframework.stereotype.Service;

/** 按真实分类和荤素标记组合套餐；汤独立计数，不将蛋白类的豆腐误当荤菜。 */
@Service
public class MealCandidateService {
  public List<Map<String, Object>> build(
      List<Map<String, Object>> menu, MealRequirements requirements, double target) {
    var available =
        menu.stream()
            .filter(d -> Boolean.TRUE.equals(d.getOrDefault("isAvailable", true)))
            .filter(d -> !requirements.excludedDishIds().contains(id(d)))
            .filter(d -> DishIngredientMatcher.allowed(d, requirements.excludedIngredients()))
            .filter(
                d ->
                    !requirements.veganOnly()
                        || DishIngredientMatcher.allowed(d, List.of("蛋", "奶", "蜂蜜")))
            .filter(
                d ->
                    !(requirements.vegetarianOnly() || requirements.veganOnly())
                        || Boolean.TRUE.equals(d.get("isVegetarian")))
            .filter(
                d ->
                    !d.get("category").equals("staple")
                        || requirements.stapleIds().isEmpty()
                        || requirements.stapleIds().contains(id(d)))
            .toList();
    var search = new Search(available, requirements, target);
    search.choose(0, new ArrayList<>(), 0, 0, 0);
    var result = new ArrayList<>(search.best);
    result.sort(search.distance);
    BusinessException.require(
        !result.isEmpty(), "没有同时符合本次要求、身体能量范围、饮食及供应条件的套餐，请调整要求或联系管理员补充菜品；不会自动放宽要求");
    return result;
  }

  /** 所有营养按服务端标准份累加并保留一位小数，不采用模型返回数值。 */
  public static Map<String, Double> totals(List<Map<String, Object>> selected) {
    var result = new LinkedHashMap<String, Double>();
    for (String field : List.of("energyKcal", "proteinG", "fatG", "carbohydrateG"))
      result.put(
          field,
          Math.round(
                  selected.stream().mapToDouble(d -> ((Number) d.get(field)).doubleValue()).sum()
                      * 10)
              / 10.0);
    return result;
  }

  private static long id(Map<String, Object> dish) {
    return ((Number) dish.get("id")).longValue();
  }

  private static final class Search {
    final List<Map<String, Object>> menu;
    final MealRequirements requirements;
    final double target;
    final Comparator<Map<String, Object>> distance;
    final PriorityQueue<Map<String, Object>> best;
    int visited;

    Search(List<Map<String, Object>> menu, MealRequirements requirements, double target) {
      this.menu = menu;
      this.requirements = requirements;
      this.target = target;
      distance =
          Comparator.comparingDouble(
              c ->
                  Math.abs(
                      ((Number) ((Map<?, ?>) c.get("totals")).get("energyKcal")).doubleValue()
                          - target));
      best = new PriorityQueue<>(distance.reversed());
    }

    void choose(int start, List<Map<String, Object>> selected, int staples, int sides, int soups) {
      // 只保留最接近能量目标的十二组，限制组合搜索避免大菜单耗尽内存或长期占用请求。
      if (++visited > 200000) throw new BusinessException(400, "可组合菜品过多，请指定主食或具体菜品缩小推荐范围");
      int needed = requirements.stapleCount() + requirements.sideCount() + requirements.soupCount();
      if (selected.size() == needed) {
        var selectedIds = new HashSet<Long>();
        selected.forEach(d -> selectedIds.add(id(d)));
        if (!selectedIds.containsAll(requirements.requiredDishIds())) return;
        if (requirements.requiredAnyGroups().stream()
            .anyMatch(group -> Collections.disjoint(group, selectedIds))) return;
        var sideDishes =
            selected.stream()
                .filter(d -> List.of("protein", "vegetable").contains(d.get("category")))
                .toList();
        long meat =
            sideDishes.stream().filter(d -> !Boolean.TRUE.equals(d.get("isVegetarian"))).count();
        if (requirements.meatCount() >= 0 && meat != requirements.meatCount()) return;
        if (requirements.defaultCombination()
            && sideDishes.stream().filter(d -> d.get("category").equals("protein")).count() != 1)
          return;
        var totals = totals(selected);
        if (totals.get("energyKcal") < target * 0.75 || totals.get("energyKcal") > target * 1.25)
          return;
        if (requirements.nutritionLimits().entrySet().stream()
            .anyMatch(entry -> !entry.getValue().accepts(totals.get(entry.getKey())))) return;
        var candidate = new LinkedHashMap<String, Object>();
        candidate.put("candidateId", UUID.randomUUID().toString());
        candidate.put(
            "dishes",
            selected.stream()
                .sorted(
                    Comparator.comparingInt(
                        d ->
                            List.of("staple", "protein", "vegetable", "soup")
                                .indexOf(d.get("category"))))
                .toList());
        candidate.put("totals", totals);
        best.add(candidate);
        if (best.size() > 12) best.poll();
        return;
      }
      if (menu.size() - start < needed - selected.size()) return;
      for (int index = start; index < menu.size(); index++) {
        var dish = menu.get(index);
        String category = (String) dish.get("category");
        int nextStaples = staples + (category.equals("staple") ? 1 : 0);
        int nextSoups = soups + (category.equals("soup") ? 1 : 0);
        int nextSides = sides + (List.of("protein", "vegetable").contains(category) ? 1 : 0);
        if (nextStaples > requirements.stapleCount()
            || nextSides > requirements.sideCount()
            || nextSoups > requirements.soupCount()) continue;
        selected.add(dish);
        choose(index + 1, selected, nextStaples, nextSides, nextSoups);
        selected.removeLast();
      }
    }
  }
}
