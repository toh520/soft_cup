package com.toh520.campus.config;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** 编号迁移及首次演示数据初始化；已有数据库仅执行未应用的迁移，不覆盖业务记录。 */
@Component
public class DatabaseInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;
  private final String adminPassword;

  public DatabaseInitializer(
      JdbcTemplate jdbc,
      TransactionTemplate transactions,
      @Value("${campus.admin-password}") String adminPassword) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.adminPassword = adminPassword;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    jdbc.execute("CREATE TABLE IF NOT EXISTS schema_migrations (version INTEGER PRIMARY KEY)");
    boolean fresh =
        jdbc.queryForObject("SELECT count(*) FROM schema_migrations WHERE version=1", Integer.class)
            == 0;
    if (fresh) initializeFirstVersion();
    applyMigration(2, "002_plaintext_passwords.sql");
    if (fresh)
      jdbc.update(
          "UPDATE users SET password=?,password_hash='' WHERE account='admin'", adminPassword);
    applyMigration(3, "003_demo_materials.sql");
    applyMigration(4, "004_order_item_quantity.sql");
  }

  private void applyMigration(int version, String filename) throws Exception {
    if (jdbc.queryForObject(
            "SELECT count(*) FROM schema_migrations WHERE version=?", Integer.class, version)
        > 0) return;
    String sql =
        new ClassPathResource("db/migration/" + filename)
            .getContentAsString(StandardCharsets.UTF_8);
    transactions.executeWithoutResult(
        status -> {
          for (String statement : sql.split(";")) if (!statement.isBlank()) jdbc.execute(statement);
          jdbc.update("INSERT INTO schema_migrations(version) VALUES (?)", version);
        });
  }

  private void initializeFirstVersion() throws Exception {
    String schema =
        new ClassPathResource("db/migration/001_initial.sql")
            .getContentAsString(StandardCharsets.UTF_8);
    transactions.executeWithoutResult(
        status -> {
          for (String statement : schema.split(";"))
            if (!statement.isBlank()) jdbc.execute(statement);
          jdbc.update("INSERT INTO classes(name) VALUES (?)", "计算机演示班");
          jdbc.update(
              "INSERT INTO users(account,password_hash,name,role) VALUES (?,?,?,'admin')",
              "admin",
              "",
              "演示管理员");
          String[][] books = {
            {"Java 程序设计", "李明", "Java 基础、面向对象与集合", "4"},
            {"数据结构", "王华", "线性表、树与图", "1"},
            {"操作系统", "陈晨", "进程、内存与文件系统", "0"},
            {"数据库原理", "周宁", "SQL、事务与关系模型", "3"},
            {"计算机网络", "赵林", "网络协议与应用", "3"},
            {"Java 程序设计", "孙悦", "Java 实践案例", "2"}
          };
          for (String[] book : books)
            jdbc.update(
                "INSERT INTO books(title,author,summary,available_count) VALUES (?,?,?,?)",
                book[0],
                book[1],
                book[2],
                Integer.parseInt(book[3]));
          for (int day = 1; day <= 5; day++)
            jdbc.update(
                "INSERT INTO class_schedules(class_id,course_name,teacher,weekday,start_time,end_time,classroom) VALUES (1,?,?,?,'09:00','10:35',?)",
                new String[] {"Java 程序设计", "数据结构", "数据库原理", "操作系统", "计算机网络"}[day - 1],
                "陈老师",
                day,
                "教学楼 A20" + day);
          String[] categories = {"staple", "protein", "vegetable"};
          String[][] names = {
            {"米饭", "杂粮饭", "红薯", "荞麦面"},
            {"香煎鸡胸", "番茄豆腐", "清蒸鱼", "香菇豆干"},
            {"清炒西兰花", "蒜蓉菠菜", "胡萝卜玉米", "冬瓜木耳"}
          };
          for (int group = 0; group < 3; group++)
            for (int item = 0; item < 4; item++) {
              double energy = new double[] {350, 230, 130}[group] + item * 20;
              jdbc.update(
                  "INSERT INTO dishes(name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available) VALUES (?,?,250,?,?,?,?,?,?,1)",
                  names[group][item],
                  categories[group],
                  energy,
                  new double[] {8, 22, 5}[group],
                  new double[] {2, 12, 6}[group],
                  new double[] {72, 12, 16}[group],
                  group == 1
                      ? (item % 2 == 1 ? "[\"大豆\"]" : (item == 2 ? "[\"鱼\"]" : "[\"鸡肉\"]"))
                      : (item == 3 && group == 0 ? "[\"小麦\"]" : "[]"),
                  group == 1 && item % 2 == 0 ? 0 : 1);
            }
          jdbc.update(
              "INSERT INTO dishes(name,category,portion_g,energy_kcal,protein_g,fat_g,carbohydrate_g,ingredient_tags,is_vegetarian,is_available) VALUES ('下架花生拌菜','vegetable',180,200,8,10,20,'[\"花生\"]',1,0)");
          jdbc.update("INSERT INTO schema_migrations(version) VALUES (1)");
        });
  }
}
