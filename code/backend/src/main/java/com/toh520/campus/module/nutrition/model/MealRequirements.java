package com.toh520.campus.module.nutrition.model;

import java.util.*;

/** 菜的数量不含主食与汤；meatCount=-1 表示不限定荤菜数量，荤素以菜品标记为准。 */
public record MealRequirements(
    int stapleCount,
    int sideCount,
    int soupCount,
    int meatCount,
    Set<Long> stapleIds,
    Set<Long> requiredDishIds,
    Set<Long> excludedDishIds,
    boolean vegetarianOnly,
    boolean defaultCombination,
    List<Set<Long>> requiredAnyGroups,
    Map<String, NutrientRange> nutritionLimits,
    List<String> excludedIngredients,
    boolean veganOnly) {
  public static MealRequirements defaults() {
    return new MealRequirements(
        1, 2, 0, -1, Set.of(), Set.of(), Set.of(), false, true, List.of(), Map.of(), List.of(),
        false);
  }
}
