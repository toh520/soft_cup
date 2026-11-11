package com.toh520.campus.module.library.repository;

import com.toh520.campus.common.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LibraryRepository {
  private final JdbcTemplate jdbc;

  public LibraryRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> search(String query) {
    // 用户自然语言常省略书名空格；两端一致去除常见空白，仍按字面转义 LIKE 通配符。
    String normalized = query.replaceAll("[\\s\\p{Z}]+", "");
    String pattern =
        "%" + normalized.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    return jdbc.query(
        "SELECT * FROM books WHERE "
            + normalizedColumn("title")
            + " LIKE ? ESCAPE '!' OR "
            + normalizedColumn("author")
            + " LIKE ? ESCAPE '!' OR "
            + normalizedColumn("summary")
            + " LIKE ? ESCAPE '!' ORDER BY id",
        Rows.INSTANCE,
        pattern,
        pattern,
        pattern);
  }

  private String normalizedColumn(String column) {
    // 列名仅由上方固定调用传入，不接受请求参数作为SQL标识符。
    String expression = column;
    for (int whitespace : new int[] {32, 9, 10, 13, 160, 12288})
      expression = "replace(" + expression + ",char(" + whitespace + "),'')";
    return expression;
  }

  public Map<String, Object> find(long id) {
    return jdbc.query("SELECT * FROM books WHERE id=?", Rows.INSTANCE, id).stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "图书不存在"));
  }

  public void save(Long id, com.toh520.campus.module.library.dto.LibraryRequests.Book book) {
    if (id == null)
      jdbc.update(
          "INSERT INTO books(title,author,summary,available_count) VALUES (?,?,?,?)",
          book.title(),
          book.author(),
          book.summary(),
          book.availableCount());
    else
      BusinessException.require(
          jdbc.update(
                  "UPDATE books SET title=?,author=?,summary=?,available_count=? WHERE id=?",
                  book.title(),
                  book.author(),
                  book.summary(),
                  book.availableCount(),
                  id)
              == 1,
          "图书不存在");
  }

  public void delete(long id) {
    BusinessException.require(jdbc.update("DELETE FROM books WHERE id=?", id) == 1, "图书不存在");
  }

  public int take(long id) {
    return jdbc.update(
        "UPDATE books SET available_count=available_count-1 WHERE id=? AND available_count>0", id);
  }

  public long borrow(long userId, long bookId, String now) {
    jdbc.update(
        "INSERT INTO borrow_records(user_id,book_id,borrowed_at) VALUES (?,?,?)",
        userId,
        bookId,
        now);
    return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
  }

  public List<Map<String, Object>> records(Long userId) {
    return jdbc.query(
        "SELECT r.*,b.title,b.author,u.name,u.account AS student_number FROM borrow_records r JOIN books b ON b.id=r.book_id JOIN users u ON u.id=r.user_id WHERE (? IS NULL OR r.user_id=?) ORDER BY r.id DESC",
        Rows.INSTANCE,
        userId,
        userId);
  }

  public Map<String, Object> record(long id, long userId) {
    return jdbc
        .query("SELECT * FROM borrow_records WHERE id=? AND user_id=?", Rows.INSTANCE, id, userId)
        .stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "借阅记录不存在"));
  }

  public int returnRecord(long id, long userId, String now) {
    return jdbc.update(
        "UPDATE borrow_records SET returned_at=? WHERE id=? AND user_id=? AND returned_at IS NULL",
        now,
        id,
        userId);
  }

  public void restore(long bookId) {
    jdbc.update("UPDATE books SET available_count=available_count+1 WHERE id=?", bookId);
  }
}
