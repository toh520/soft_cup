package com.toh520.campus.module.nutrition.repository;

import com.toh520.campus.common.*;
import com.toh520.campus.module.nutrition.dto.DishRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DishRepository {
  private final JdbcTemplate jdbc;
  private final JsonSupport json;

  public DishRepository(JdbcTemplate jdbc, JsonSupport json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public List<Map<String, Object>> all() {
    var result = jdbc.query("SELECT * FROM dishes ORDER BY id", Rows.INSTANCE);
    for (var dish : result) {
      dish.put("ingredientTags", json.strings((String) dish.get("ingredientTags")));
      dish.put("isVegetarian", ((Number) dish.get("isVegetarian")).intValue() == 1);
      dish.put("isAvailable", ((Number) dish.get("isAvailable")).intValue() == 1);
    }
    return result;
  }

  public void save(Long id, DishRequest dish) {
    var args =
        new ArrayList<Object>(
            Arrays.asList(
                dish.name(),
                dish.category(),
                dish.portionG(),
                dish.energyKcal(),
                dish.proteinG(),
                dish.fatG(),
                dish.carbohydrateG(),
                json.write(dish.ingredientTags()),
                dish.isVegetarian() ? 1 : 0,
                dish.isAvailable() ? 1 : 0));
    if (id == null)
      jdbc.update(
          "INSERT INTO dishes(name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available) VALUES (?,?,?,?,?,?,?,?,?,?)",
          args.toArray());
    else {
      args.add(id);
      BusinessException.require(
          jdbc.update(
                  "UPDATE dishes SET name=?,category=?,portion_g=?,energy_kcal=?,protein_g=?,fat_g=?,carbohydrate_g=?,ingredient_tags=?,is_vegetarian=?,is_available=? WHERE id=?",
                  args.toArray())
              == 1,
          "菜品不存在");
    }
  }

  public void delete(long id) {
    BusinessException.require(jdbc.update("DELETE FROM dishes WHERE id=?", id) == 1, "菜品不存在");
  }
}
