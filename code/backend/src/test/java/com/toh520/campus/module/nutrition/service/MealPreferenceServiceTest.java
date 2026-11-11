package com.toh520.campus.module.nutrition.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.toh520.campus.common.BusinessException;
import com.toh520.campus.integration.llm.ChatModelClient;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MealPreferenceServiceTest {
  final ObjectMapper mapper = new ObjectMapper();
  final ChatModelClient chat = mock(ChatModelClient.class);
  final MealPreferenceService service = new MealPreferenceService(chat);
  final List<Map<String, Object>> menu =
      List.of(
          dish(1, "米饭", "staple", true),
          dish(2, "鸡肉", "protein", false),
          dish(3, "豆腐汤", "soup", true));

  Map<String, Object> dish(long id, String name, String category, boolean vegetarian) {
    return Map.of(
        "id",
        id,
        "name",
        name,
        "category",
        category,
        "isVegetarian",
        vegetarian,
        "ingredientTags",
        List.of(),
        "energyKcal",
        200,
        "proteinG",
        15,
        "fatG",
        5,
        "carbohydrateG",
        25);
  }

  ObjectNode valid() {
    var node = mapper.createObjectNode();
    node.put("stapleCount", 1)
        .put("sideCount", 2)
        .put("soupCount", 1)
        .put("meatCount", 1)
        .put("vegetarianOnly", false)
        .put("message", "");
    node.putArray("stapleIds").add(1);
    node.putArray("requiredDishIds");
    node.putArray("excludedDishIds");
    node.put("veganOnly", false);
    node.putArray("requiredAnyGroups");
    node.putArray("excludedIngredients");
    node.putObject("nutritionLimits");
    return node;
  }

  @Test
  void blankPreservesOriginalCombinationWithoutParsingCall() {
    assertThat(service.parse("", menu).defaultCombination()).isTrue();
    verifyNoInteractions(chat);
  }

  @Test
  void parsesTwoSidesSoupAndExactRice() {
    when(chat.complete(eq("nutrition/requirements"), any())).thenReturn(valid());
    var parsed = service.parse("两菜一汤，一荤一素，主食米饭", menu);
    assertThat(parsed.stapleIds()).containsExactly(1L);
    assertThat(parsed.sideCount()).isEqualTo(2);
    assertThat(parsed.meatCount()).isEqualTo(1);
    assertThat(parsed.soupCount()).isEqualTo(1);
  }

  static Stream<String> invalidFields() {
    return Stream.of(
        "stapleCount=-1",
        "stapleCount=2",
        "sideCount=5",
        "sideCount=-1",
        "soupCount=2",
        "soupCount=-1",
        "meatCount=3",
        "meatCount=-2",
        "sideCount=1.5",
        "sideCount=\"2\"",
        "sideCount=null",
        "meatCount=99999999999999",
        "vegetarianOnly=\"true\"",
        "message=null",
        "stapleIds=[999]",
        "stapleIds=[1,1]",
        "stapleIds=[1.5]",
        "stapleIds=null",
        "requiredDishIds=[999]",
        "excludedDishIds=[999]",
        "requiredAnyGroups=[[999]]",
        "requiredAnyGroups=[[]]",
        "requiredAnyGroups=null",
        "veganOnly=1",
        "excludedIngredients=[1]",
        "excludedIngredients=[\"\"]",
        "nutritionLimits=[]",
        "nutritionLimits={\"price\":{\"min\":null,\"max\":10}}",
        "nutritionLimits={\"energyKcal\":{\"min\":-1,\"max\":850}}",
        "nutritionLimits={\"energyKcal\":{\"min\":null,\"max\":null}}",
        "nutritionLimits={\"proteinG\":{\"min\":\"50\",\"max\":null}}");
  }

  @ParameterizedTest
  @MethodSource("invalidFields")
  void rejectsMalformedOrInventedConstraints(String setting) throws Exception {
    var parts = setting.split("=", 2);
    var response = valid();
    response.set(parts[0], mapper.readTree(parts[1]));
    when(chat.complete(anyString(), any())).thenReturn(response);
    assertThatThrownBy(() -> service.parse("用户要求", menu))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("解析无效");
  }

  @Test
  void rejectsConflictsRatherThanSilentlyRelaxingThem() {
    for (String conflict :
        List.of("vegetarianMeat", "requiredExcluded", "nonStaple", "noStaple", "excludedStaple")) {
      var node = valid();
      switch (conflict) {
        case "vegetarianMeat" -> node.put("vegetarianOnly", true);
        case "requiredExcluded" -> {
          node.putArray("requiredDishIds").add(2);
          node.putArray("excludedDishIds").add(2);
        }
        case "nonStaple" -> node.putArray("stapleIds").add(2);
        case "noStaple" -> node.put("stapleCount", 0);
        case "excludedStaple" -> node.putArray("excludedDishIds").add(1);
      }
      when(chat.complete(anyString(), any())).thenReturn(node);
      assertThatThrownBy(() -> service.parse("冲突要求", menu)).hasMessageContaining("冲突");
    }
  }

  @Test
  void explainsUnavailableDishAndUnsupportedRequest() {
    var node = valid().put("message", "今日没有牛排，请换一种菜品");
    when(chat.complete(anyString(), any())).thenReturn(node);
    assertThatThrownBy(() -> service.parse("要牛排", menu)).hasMessageContaining("今日没有牛排");
  }

  @Test
  void validatesAlternateGroupsAndNumericalRanges() throws Exception {
    var node = valid();
    node.set("requiredAnyGroups", mapper.readTree("[[2,3]]"));
    node.set(
        "nutritionLimits",
        mapper.readTree(
            "{\"energyKcal\":{\"min\":750,\"max\":850},\"proteinG\":{\"min\":50,\"max\":null}}"));
    when(chat.complete(anyString(), any())).thenReturn(node);
    var parsed = service.parse("牛肉或汤，750到850千卡，蛋白质至少50克", menu);
    assertThat(parsed.requiredAnyGroups()).containsExactly(Set.of(2L, 3L));
    assertThat(parsed.nutritionLimits().get("energyKcal").accepts(851)).isFalse();
    assertThat(service.describe(parsed, menu)).contains("750", "850", "50", "鸡肉", "豆腐汤");
    node.set("nutritionLimits", mapper.readTree("{\"energyKcal\":{\"min\":900,\"max\":850}}"));
    assertThatThrownBy(() -> service.parse("冲突", menu)).hasMessageContaining("上下限存在冲突");
  }
}
