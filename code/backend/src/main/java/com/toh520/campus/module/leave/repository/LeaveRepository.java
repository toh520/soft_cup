package com.toh520.campus.module.leave.repository;

import com.toh520.campus.common.Rows;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LeaveRepository {
  private final JdbcTemplate jdbc;

  public LeaveRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insert(long userId, String start, String end, String reason) {
    jdbc.update(
        "INSERT INTO leave_requests(user_id,start_at,end_at,reason) VALUES (?,?,?,?)",
        userId,
        start,
        end,
        reason);
    return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
  }

  public List<Map<String, Object>> mine(long userId) {
    return jdbc.query(
        "SELECT * FROM leave_requests WHERE user_id=? ORDER BY id DESC", Rows.INSTANCE, userId);
  }

  public List<Map<String, Object>> all(String status) {
    return jdbc.query(
        "SELECT l.*,u.name,u.account AS student_number FROM leave_requests l JOIN users u ON u.id=l.user_id WHERE (? IS NULL OR l.status=?) ORDER BY l.id DESC",
        Rows.INSTANCE,
        status,
        status);
  }

  public int approve(long id, String status) {
    return jdbc.update(
        "UPDATE leave_requests SET status=? WHERE id=? AND status='pending'", status, id);
  }
}
