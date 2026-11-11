package com.toh520.campus.module.nutrition.service;

import static org.assertj.core.api.Assertions.*;

import com.toh520.campus.module.nutrition.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 用独立枚举器核对所有合法组合，不以生产筛选器自己的输出作为期望。 */
class MealConstraintTest {
  private final MealCandidateService service = new MealCandidateService();

  private Map<String, Object> dish(
      long id,
      String name,
      String category,
      boolean vegetarian,
      int energy,
      int protein,
      String... ingredients) {
    return Map.of(
        "id",
        id,
        "name",
        name,
        "category",
        category,
        "isVegetarian",
        vegetarian,
        "energyKcal",
        energy,
        "proteinG",
        protein,
        "fatG",
        3,
        "carbohydrateG",
        20,
        "ingredientTags",
        List.of(ingredients),
        "isAvailable",
        true);
  }

  private List<Map<String, Object>> menu() {
    return List.of(
        dish(1, "米饭", "staple", true, 350, 8, "大米"),
        dish(2, "牛奶燕麦粥", "staple", true, 320, 14, "牛奶"),
        dish(3, "牛肉", "protein", false, 240, 30, "牛肉"),
        dish(4, "白灼虾", "protein", false, 160, 32, "虾"),
        dish(5, "豆腐", "protein", true, 220, 22, "大豆"),
        dish(6, "青菜", "vegetable", true, 120, 4, "白菜"),
        dish(7, "蛋炒菜", "vegetable", true, 170, 10, "鸡蛋"),
        dish(8, "冬瓜汤", "soup", true, 55, 2, "冬瓜"),
        dish(9, "鱼汤", "soup", false, 100, 12, "鲫鱼"));
  }

  @Test
  void alternateDishGroupsAndNutritionLimitsApplyToEveryReturnedPlan() {
    var rules =
        new MealRequirements(
            1,
            2,
            1,
            1,
            Set.of(1L),
            Set.of(),
            Set.of(),
            false,
            false,
            List.of(Set.of(3L, 4L)),
            Map.of(
                "energyKcal",
                new NutrientRange(700.0, 850.0),
                "proteinG",
                new NutrientRange(45.0, null)),
            List.of(),
            false);
    for (var plan : service.build(menu(), rules, 800)) {
      var selected = (List<Map<String, Object>>) plan.get("dishes");
      assertThat(selected).anyMatch(d -> Set.of(3L, 4L).contains(d.get("id")));
      var total = (Map<String, Double>) plan.get("totals");
      assertThat(total.get("energyKcal")).isBetween(700.0, 850.0);
      assertThat(total.get("proteinG")).isGreaterThanOrEqualTo(45);
    }
  }

  @Test
  void veganFiltersMilkAndEggEvenWhenMenuMarksThemVegetarian() {
    var rules =
        new MealRequirements(
            1, 2, 1, 0, Set.of(), Set.of(), Set.of(), true, false, List.of(), Map.of(), List.of(),
            true);
    var result = service.build(menu(), rules, 750);
    assertThat(result).hasSize(1);
    assertThat((List<Map<String, Object>>) result.getFirst().get("dishes"))
        .extracting(d -> d.get("id"))
        .containsExactly(1L, 5L, 6L, 8L);
  }

  @ParameterizedTest
  @CsvSource({
    "大豆,香菇豆干",
    "soy,豆腐",
    "乳制品,牛奶燕麦粥",
    "dairy,芝士",
    "蛋,番茄蛋花汤",
    "鸡蛋,蛋清",
    "海鲜,白灼虾",
    "seafood,清蒸鲫鱼",
    "鱼肉,鱼丸汤"
  })
  void ingredientAliasesUseDishNamesAndTags(String ingredient, String name) {
    assertThat(
            DishIngredientMatcher.contains(
                Map.of("name", name, "ingredientTags", List.of()), ingredient))
        .isTrue();
    assertThat(
            DishIngredientMatcher.contains(
                Map.of("name", "米饭", "ingredientTags", List.of("大米")), ingredient))
        .isFalse();
  }

  @Test
  void noMealIsReturnedWhenRequiredDishConflictsWithIngredientOrNutrientLimits() {
    var rules =
        new MealRequirements(
            1,
            2,
            1,
            1,
            Set.of(),
            Set.of(4L),
            Set.of(),
            false,
            false,
            List.of(),
            Map.of(),
            List.of("海鲜"),
            false);
    assertThatThrownBy(() -> service.build(menu(), rules, 800)).hasMessageContaining("没有同时符合");
    var impossible =
        new MealRequirements(
            1,
            2,
            1,
            1,
            Set.of(),
            Set.of(),
            Set.of(),
            false,
            false,
            List.of(),
            Map.of("energyKcal", new NutrientRange(null, 100.0)),
            List.of(),
            false);
    assertThatThrownBy(() -> service.build(menu(), impossible, 800)).hasMessageContaining("没有同时符合");
  }

  @Test
  void enumerationOracleCoversThirtySixCombinationsOfCountsMeatAndNutrition() {
    for (int sides = 1; sides <= 3; sides++)
      for (int soup = 0; soup <= 1; soup++)
        for (int meat = 0; meat <= sides; meat++)
          for (int cap : List.of(850, 1100)) {
            var rules =
                new MealRequirements(
                    1,
                    sides,
                    soup,
                    meat,
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    false,
                    false,
                    List.of(),
                    Map.of("energyKcal", new NutrientRange(null, (double) cap)),
                    List.of(),
                    false);
            var expected = new HashSet<Set<Long>>();
            var menu = menu();
            for (int mask = 1; mask < (1 << menu.size()); mask++) {
              var chosen = new ArrayList<Map<String, Object>>();
              for (int index = 0; index < menu.size(); index++)
                if ((mask & (1 << index)) != 0) chosen.add(menu.get(index));
              long mains = chosen.stream().filter(d -> d.get("category").equals("staple")).count();
              long soups = chosen.stream().filter(d -> d.get("category").equals("soup")).count();
              var side =
                  chosen.stream()
                      .filter(d -> List.of("protein", "vegetable").contains(d.get("category")))
                      .toList();
              long meats = side.stream().filter(d -> !(Boolean) d.get("isVegetarian")).count();
              int energy = chosen.stream().mapToInt(d -> (Integer) d.get("energyKcal")).sum();
              if (mains == 1
                  && soups == soup
                  && side.size() == sides
                  && meats == meat
                  && energy >= 600
                  && energy <= 1000
                  && energy <= cap) {
                var ids = new HashSet<Long>();
                chosen.forEach(d -> ids.add((Long) d.get("id")));
                expected.add(ids);
              }
            }
            if (expected.isEmpty())
              assertThatThrownBy(() -> service.build(menu, rules, 800))
                  .hasMessageContaining("没有同时符合");
            else {
              var actual = service.build(menu, rules, 800);
              assertThat(actual).hasSize(Math.min(12, expected.size()));
              for (var plan : actual) {
                var ids = new HashSet<Long>();
                ((List<Map<String, Object>>) plan.get("dishes"))
                    .forEach(d -> ids.add((Long) d.get("id")));
                assertThat(expected).contains(ids);
              }
            }
          }
  }

  @Test
  void twoExactSoupAlternativesReturnTwoDifferentPlansWithoutAddingOtherDishes() {
    var rules =
        new MealRequirements(
            1,
            2,
            1,
            1,
            Set.of(1L),
            Set.of(3L, 6L),
            Set.of(),
            false,
            false,
            List.of(Set.of(8L, 9L)),
            Map.of(),
            List.of(),
            false);
    var result = service.build(menu(), rules, 800);
    assertThat(result).hasSize(2);
    for (var plan : result) {
      var ids =
          ((List<Map<String, Object>>) plan.get("dishes"))
              .stream().map(d -> (Long) d.get("id")).toList();
      assertThat(ids).hasSize(4).contains(1L, 3L, 6L);
      assertThat(ids).containsAnyOf(8L, 9L);
    }
  }
}
