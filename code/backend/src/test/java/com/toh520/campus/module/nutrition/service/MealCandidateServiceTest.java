package com.toh520.campus.module.nutrition.service;

import static org.assertj.core.api.Assertions.*;

import com.toh520.campus.module.nutrition.model.MealRequirements;
import java.util.*;
import org.junit.jupiter.api.Test;

class MealCandidateServiceTest {
  final MealCandidateService service = new MealCandidateService();
  final List<Map<String, Object>> menu =
      List.of(
          dish(1, "staple", true, 350), dish(2, "staple", true, 350),
          dish(3, "protein", false, 250), dish(4, "protein", false, 250),
          dish(5, "protein", true, 200), dish(6, "vegetable", true, 150),
          dish(7, "vegetable", true, 150), dish(8, "soup", true, 60),
          dish(9, "soup", true, 70), dish(10, "soup", false, 80));

  Map<String, Object> dish(long id, String category, boolean vegetarian, double energy) {
    return Map.of(
        "id",
        id,
        "category",
        category,
        "isVegetarian",
        vegetarian,
        "energyKcal",
        energy,
        "proteinG",
        10,
        "fatG",
        5,
        "carbohydrateG",
        20);
  }

  List<Map<String, Object>> dishes(Map<String, Object> plan) {
    return (List<Map<String, Object>>) plan.get("dishes");
  }

  @Test
  void exactRiceTwoSidesOneMeatOneVegetarianAndOneSoup() {
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
            List.of(),
            Map.of(),
            List.of(),
            false);
    var plans = service.build(menu, rules, 800);
    assertThat(plans).hasSizeBetween(3, 12);
    for (var plan : plans) {
      var selected = dishes(plan);
      assertThat(selected).hasSize(4);
      assertThat(selected.stream().filter(d -> d.get("category").equals("staple")))
          .extracting(d -> d.get("id"))
          .containsExactly(1L);
      assertThat(selected.stream().filter(d -> d.get("category").equals("soup"))).hasSize(1);
      assertThat(
              selected.stream()
                  .filter(d -> List.of("protein", "vegetable").contains(d.get("category")))
                  .filter(d -> !(boolean) d.get("isVegetarian")))
          .hasSize(1);
      assertThat(selected.stream().map(d -> d.get("id")).distinct().count()).isEqualTo(4);
    }
    assertThat(
            plans.stream()
                .map(p -> dishes(p).stream().map(d -> d.get("id")).toList())
                .distinct()
                .count())
        .isEqualTo(plans.size());
  }

  @Test
  void vegetarianIncludesTofuButExcludesMeatSoupAndHonorsExcludedAndRequiredDishes() {
    var rules =
        new MealRequirements(
            1,
            2,
            1,
            0,
            Set.of(),
            Set.of(5L),
            Set.of(7L),
            true,
            false,
            List.of(),
            Map.of(),
            List.of(),
            false);
    var plans = service.build(menu, rules, 750);
    for (var plan : plans) {
      assertThat(dishes(plan)).allSatisfy(d -> assertThat(d.get("isVegetarian")).isEqualTo(true));
      assertThat(dishes(plan)).extracting(d -> d.get("id")).contains(5L).doesNotContain(7L, 10L);
    }
  }

  @Test
  void noSoupThreeSidesAndNoStapleAreSupported() {
    var rules =
        new MealRequirements(
            0, 3, 0, 1, Set.of(), Set.of(), Set.of(), false, false, List.of(), Map.of(), List.of(),
            false);
    for (var plan : service.build(menu, rules, 600)) {
      assertThat(dishes(plan))
          .hasSize(3)
          .noneSatisfy(d -> assertThat(d.get("category")).isIn("staple", "soup"));
    }
  }

  @Test
  void blankRequirementPreservesOneStapleOneProteinOneVegetable() {
    for (var plan : service.build(menu, MealRequirements.defaults(), 750))
      assertThat(dishes(plan))
          .extracting(d -> d.get("category"))
          .containsExactlyInAnyOrder("staple", "protein", "vegetable");
  }

  @Test
  void insufficientEnergySupplyOrImpossibleCompositionDoesNotRelaxRules() {
    var rules =
        new MealRequirements(
            1,
            2,
            1,
            1,
            Set.of(999L),
            Set.of(),
            Set.of(),
            false,
            false,
            List.of(),
            Map.of(),
            List.of(),
            false);
    assertThatThrownBy(() -> service.build(menu, rules, 800)).hasMessageContaining("没有同时符合");
    assertThatThrownBy(() -> service.build(menu, MealRequirements.defaults(), 3000))
        .hasMessageContaining("没有同时符合");
    var single =
        new MealRequirements(
            1,
            2,
            1,
            1,
            Set.of(1L),
            Set.of(3L, 6L, 8L),
            Set.of(),
            false,
            false,
            List.of(),
            Map.of(),
            List.of(),
            false);
    assertThat(service.build(menu, single, 800)).hasSize(1);
  }
}
