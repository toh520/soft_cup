package com.toh520.campus.module.nutrition.controller;

import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.nutrition.dto.DishRequest;
import com.toh520.campus.module.nutrition.service.NutritionService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class NutritionController {
  private final NutritionService nutrition;

  public NutritionController(NutritionService nutrition) {
    this.nutrition = nutrition;
  }

  @GetMapping("/nutrition/dishes")
  Object menu(@RequestAttribute LoginSession session) {
    return nutrition.availableDishes(session);
  }

  @GetMapping("/nutrition/readiness")
  Object readiness(@RequestAttribute LoginSession session) {
    return nutrition.readiness(session);
  }

  @PostMapping("/nutrition/recommendations")
  Object recommend(@RequestAttribute LoginSession session) {
    return nutrition.recommend(session);
  }

  @GetMapping("/admin/dishes")
  Object all(@RequestAttribute LoginSession session) {
    return nutrition.all(session);
  }

  @PostMapping("/admin/dishes")
  Object create(@RequestAttribute LoginSession session, @Valid @RequestBody DishRequest request) {
    nutrition.save(session, null, request);
    return Map.of("message", "菜品已新增");
  }

  @PutMapping("/admin/dishes/{id}")
  Object edit(
      @RequestAttribute LoginSession session,
      @PathVariable long id,
      @Valid @RequestBody DishRequest request) {
    nutrition.save(session, id, request);
    return Map.of("message", "菜品已更新");
  }

  @DeleteMapping("/admin/dishes/{id}")
  Object delete(@RequestAttribute LoginSession session, @PathVariable long id) {
    nutrition.delete(session, id);
    return Map.of("message", "菜品已删除");
  }
}
