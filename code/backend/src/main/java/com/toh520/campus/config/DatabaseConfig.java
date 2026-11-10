package com.toh520.campus.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfig {
  @Bean
  JdbcTemplate jdbcTemplate(DataSource dataSource) {
    var jdbc = new JdbcTemplate(dataSource);
    // SQLite 使用扩展错误码；低 8 位 19 表示唯一、外键或 CHECK 约束冲突。
    jdbc.setExceptionTranslator(
        (task, sql, error) ->
            (error.getErrorCode() & 255) == 19
                ? new DataIntegrityViolationException("数据约束冲突", error)
                : new UncategorizedSQLException(task, sql, error));
    return jdbc;
  }
}
