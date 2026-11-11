package com.toh520.campus.module.nutrition.service;

import java.util.*;

/** 对已有菜名和食材标签做别名匹配，不推测未登记的配方或保证过敏安全。 */
public final class DishIngredientMatcher {
  private DishIngredientMatcher() {}

  private static final Map<String, List<String>> GROUPS =
      Map.of(
          "大豆", List.of("大豆", "黄豆", "毛豆", "豆腐", "豆干", "豆皮", "豆浆", "酱油"),
          "奶", List.of("奶", "乳", "黄油", "芝士"),
          "蛋", List.of("鸡蛋", "鸭蛋", "鹅蛋", "鹌鹑蛋", "蛋清", "蛋黄", "蛋花"),
          "海鲜", List.of("鱼", "虾", "蟹", "贝", "蛤", "蚝", "鱿", "章鱼", "龙虾"),
          "鱼", List.of("鱼"));

  public static boolean contains(Map<String, Object> dish, String ingredient) {
    String query = ingredient.trim().toLowerCase(Locale.ROOT);
    if (query.isEmpty()) return false;
    String key =
        switch (query) {
          case "豆制品", "大豆制品", "黄豆", "soy", "soybean" -> "大豆";
          case "牛奶", "乳制品", "奶制品", "milk", "dairy" -> "奶";
          case "鸡蛋", "蛋类", "egg", "eggs" -> "蛋";
          case "海产品", "seafood" -> "海鲜";
          case "鱼肉", "fish" -> "鱼";
          default -> query;
        };
    var words = GROUPS.getOrDefault(key, List.of(key));
    var labels = new ArrayList<String>();
    labels.add(String.valueOf(dish.getOrDefault("name", "")));
    for (Object tag : (List<?>) dish.getOrDefault("ingredientTags", List.of()))
      labels.add(tag.toString());
    return labels.stream()
        .map(label -> label.toLowerCase(Locale.ROOT))
        .anyMatch(label -> words.stream().anyMatch(label::contains));
  }

  public static boolean allowed(Map<String, Object> dish, List<String> excluded) {
    return excluded.stream().noneMatch(ingredient -> contains(dish, ingredient));
  }
}
