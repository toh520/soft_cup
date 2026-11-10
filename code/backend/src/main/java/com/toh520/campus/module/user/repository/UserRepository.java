package com.toh520.campus.module.user.repository;

import com.toh520.campus.common.*;
import com.toh520.campus.module.user.dto.UserRequests;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  private final JdbcTemplate jdbc;
  private final JsonSupport json;
  private static final String PROFILE =
      "SELECT u.id,u.account AS student_number,u.name,u.role,u.class_id,c.name AS class_name,u.age,u.sex,u.height_cm,u.weight_kg,u.activity_level,u.diet_preference,u.avoided_ingredients FROM users u LEFT JOIN classes c ON c.id=u.class_id";

  public UserRepository(JdbcTemplate jdbc, JsonSupport json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Map<String, Object> find(long id) {
    var result =
        jdbc.query(PROFILE + " WHERE u.id=?", Rows.INSTANCE, id).stream()
            .findFirst()
            .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    result.put("avoidedIngredients", json.strings((String) result.get("avoidedIngredients")));
    return result;
  }

  public List<Map<String, Object>> students() {
    return jdbc.query(
        PROFILE.replace("SELECT u.id,", "SELECT u.password,u.id,")
            + " WHERE u.role='student' ORDER BY u.id",
        Rows.INSTANCE);
  }

  public List<Map<String, Object>> classes() {
    return jdbc.query("SELECT id,name FROM classes ORDER BY id", Rows.INSTANCE);
  }

  public void update(long id, UserRequests.Profile profile) {
    jdbc.update(
        "UPDATE users SET name=?,age=?,sex=?,height_cm=?,weight_kg=?,activity_level=?,diet_preference=?,avoided_ingredients=? WHERE id=?",
        profile.name().trim(),
        profile.age(),
        profile.sex(),
        profile.heightCm(),
        profile.weightKg(),
        profile.activityLevel(),
        profile.dietPreference(),
        json.write(profile.avoidedIngredients()),
        id);
  }

  public int updateStudent(long id, UserRequests.Student student) {
    return jdbc.update(
        "UPDATE users SET name=?,class_id=? WHERE id=? AND role='student'",
        student.name().trim(),
        student.classId(),
        id);
  }

  public int resetPassword(long id, String password) {
    return jdbc.update(
        "UPDATE users SET password=?,password_hash='' WHERE id=? AND role='student'", password, id);
  }
}
