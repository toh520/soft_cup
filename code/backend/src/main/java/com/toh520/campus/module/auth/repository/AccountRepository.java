package com.toh520.campus.module.auth.repository;

import com.toh520.campus.common.Rows;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {
  private final JdbcTemplate jdbc;

  public AccountRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> find(String account) {
    return jdbc
        .query(
            "SELECT id,account,password_hash,password,role FROM users WHERE account=?",
            Rows.INSTANCE,
            account)
        .stream()
        .findFirst()
        .orElse(null);
  }

  public void register(String name, String account, String password) {
    jdbc.update(
        "INSERT INTO users(name,account,password_hash,password,role,class_id) VALUES (?,?,'',?,'student',1)",
        name,
        account,
        password);
  }

  /** 旧版本只保存哈希，首次正确登录后记录用户实际输入的原密码，不能反推旧哈希。 */
  public void retainPassword(long userId, String password, String previousHash) {
    jdbc.update(
        "UPDATE users SET password=?,password_hash='' WHERE id=? AND password IS NULL AND password_hash=?",
        password,
        userId,
        previousHash);
  }
}
