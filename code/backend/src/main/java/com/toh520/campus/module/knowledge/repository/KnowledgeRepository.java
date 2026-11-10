package com.toh520.campus.module.knowledge.repository;

import com.toh520.campus.common.Rows;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeRepository {
  private final JdbcTemplate jdbc;

  public KnowledgeRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> all() {
    return jdbc.query("SELECT * FROM knowledge_entries ORDER BY id", Rows.INSTANCE);
  }

  public void insert(String title, String category, String content, String vector, String model) {
    jdbc.update(
        "INSERT INTO knowledge_entries(title,category,content,embedding,embedding_model) VALUES (?,?,?,?,?)",
        title,
        category,
        content,
        vector,
        model);
  }

  public int update(
      Map<String, Object> previous,
      String title,
      String category,
      String content,
      String vector,
      String model) {
    // 原文、类别、向量和模型共同充当乐观版本；编辑期间的删除不会被重新插入。
    return jdbc.update(
        "UPDATE knowledge_entries SET title=?,category=?,content=?,embedding=?,embedding_model=? WHERE id=? AND title=? AND category=? AND content=? AND embedding=? AND embedding_model=?",
        title,
        category,
        content,
        vector,
        model,
        previous.get("id"),
        previous.get("title"),
        previous.get("category"),
        previous.get("content"),
        previous.get("embedding"),
        previous.get("embeddingModel"));
  }

  public int delete(Map<String, Object> previous) {
    return jdbc.update(
        "DELETE FROM knowledge_entries WHERE id=? AND title=? AND category=? AND content=? AND embedding=? AND embedding_model=?",
        previous.get("id"),
        previous.get("title"),
        previous.get("category"),
        previous.get("content"),
        previous.get("embedding"),
        previous.get("embeddingModel"));
  }
}
