package com.toh520.campus.config;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteDataSource;

/** 使用独立 SQLite 检查首建、旧库迁移以及重复启动不会重新覆盖演示资料。 */
class DatabaseInitializerTest {
  @TempDir Path directory;
  JdbcTemplate jdbc;

  DatabaseInitializer initializer() {
    var source = new SQLiteDataSource();
    source.setEnforceForeignKeys(true);
    source.setUrl("jdbc:sqlite:" + directory.resolve("campus.db"));
    jdbc = new JdbcTemplate(source);
    return new DatabaseInitializer(
        jdbc, new TransactionTemplate(new DataSourceTransactionManager(source)), "initial123");
  }

  @Test
  void freshDatabaseContainsPlainPasswordsAndDemoMaterialsOnlyOnce() throws Exception {
    var initializer = initializer();
    initializer.run(null);
    assertThat(
            jdbc.queryForObject("SELECT password FROM users WHERE account='admin'", String.class))
        .isEqualTo("initial123");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(4);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM books", Integer.class)).isEqualTo(10);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM class_schedules", Integer.class))
        .isEqualTo(10);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM dishes", Integer.class)).isEqualTo(19);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM knowledge_entries WHERE embedding_model='pending'",
                Integer.class))
        .isEqualTo(12);
    jdbc.update("UPDATE users SET password='changed123' WHERE account='20260001'");
    jdbc.update("DELETE FROM knowledge_entries WHERE id=(SELECT min(id) FROM knowledge_entries)");
    initializer.run(null);
    assertThat(
            jdbc.queryForObject(
                "SELECT password FROM users WHERE account='20260001'", String.class))
        .isEqualTo("changed123");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM knowledge_entries", Integer.class))
        .isEqualTo(11);
  }

  @Test
  void versionOneUpgradePreservesExistingAccountsAndClassScheduleConflicts() throws Exception {
    var initializer = initializer();
    jdbc.execute("CREATE TABLE schema_migrations(version INTEGER PRIMARY KEY)");
    var schema =
        new ClassPathResource("db/migration/001_initial.sql")
            .getContentAsString(StandardCharsets.UTF_8);
    for (String sql : schema.split(";")) if (!sql.isBlank()) jdbc.execute(sql);
    jdbc.update("INSERT INTO classes(id,name) VALUES (1,'已有班级')");
    jdbc.update(
        "INSERT INTO users(account,password_hash,name,role,class_id) VALUES ('20260001','old-hash','已有姓名','student',1)");
    jdbc.update(
        "INSERT INTO class_schedules(class_id,course_name,teacher,weekday,start_time,end_time,classroom) VALUES (1,'已有课程','已有老师',1,'13:30','14:30','原教室')");
    jdbc.update(
        "INSERT INTO dishes(id,name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available) VALUES (1,'原始菜品','staple',200,350,8,2,72,'[]',1,1)");
    long userId = jdbc.queryForObject("SELECT id FROM users WHERE account='20260001'", Long.class);
    jdbc.update(
        "INSERT INTO orders(id,user_id,ordered_at) VALUES (1,?,'2026-10-16T12:00:00+08:00')",
        userId);
    jdbc.update(
        "INSERT INTO order_items(order_id,dish_id,dish_name,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g) VALUES (1,1,'旧菜品快照',200,350,8,2,72)");
    jdbc.update("INSERT INTO schema_migrations(version) VALUES (1)");
    jdbc.update("UPDATE sqlite_sequence SET seq=1000 WHERE name='dishes'");
    jdbc.update("UPDATE sqlite_sequence SET seq=2000 WHERE name='order_items'");
    initializer.run(null);
    assertThat(jdbc.queryForList("PRAGMA foreign_key_check")).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT seq FROM sqlite_sequence WHERE name='order_items'", Long.class))
        .isEqualTo(2000L);
    assertThat(jdbc.queryForObject("SELECT min(id) FROM dishes WHERE category='soup'", Long.class))
        .isGreaterThan(1000L);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM dishes WHERE id=1"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    initializer.run(null);
    assertThat(jdbc.queryForObject("SELECT name FROM users WHERE account='20260001'", String.class))
        .isEqualTo("已有姓名");
    assertThat(
            jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE account='20260001'", String.class))
        .isEqualTo("old-hash");
    assertThat(
            jdbc.queryForObject(
                "SELECT password FROM users WHERE account='20260001'", String.class))
        .isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM class_schedules WHERE weekday=1", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForList(
                "SELECT version FROM schema_migrations ORDER BY version", Integer.class))
        .containsExactly(1, 2, 3, 4, 5);
    assertThat(
            jdbc.queryForObject("SELECT quantity FROM order_items WHERE order_id=1", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject("SELECT dish_name FROM order_items WHERE order_id=1", String.class))
        .isEqualTo("旧菜品快照");
  }
}
