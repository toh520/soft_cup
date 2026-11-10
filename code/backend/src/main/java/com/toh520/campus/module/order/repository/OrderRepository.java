package com.toh520.campus.module.order.repository;

import com.toh520.campus.common.Rows;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
  private final JdbcTemplate jdbc;

  public OrderRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insert(long userId, String now, List<Map<String, Object>> dishes) {
    jdbc.update("INSERT INTO orders(user_id,ordered_at) VALUES (?,?)", userId, now);
    long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    for (var dish : dishes)
      jdbc.update(
          "INSERT INTO order_items(order_id,dish_id,dish_name,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,quantity) VALUES (?,?,?,?,?,?,?,?,?)",
          id,
          dish.get("id"),
          dish.get("name"),
          dish.get("portionG"),
          dish.get("energyKcal"),
          dish.get("proteinG"),
          dish.get("fatG"),
          dish.get("carbohydrateG"),
          dish.getOrDefault("quantity", 1));
    return id;
  }

  public List<Map<String, Object>> list(Long userId) {
    var orders =
        jdbc.query(
            "SELECT o.*,u.name,u.account AS student_number FROM orders o JOIN users u ON u.id=o.user_id WHERE (? IS NULL OR o.user_id=?) ORDER BY o.id DESC",
            Rows.INSTANCE,
            userId,
            userId);
    for (var order : orders)
      order.put(
          "items",
          jdbc.query(
              "SELECT * FROM order_items WHERE order_id=? ORDER BY id",
              Rows.INSTANCE,
              order.get("id")));
    return orders;
  }
}
