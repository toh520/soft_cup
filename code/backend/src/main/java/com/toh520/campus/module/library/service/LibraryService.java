package com.toh520.campus.module.library.service;

import com.toh520.campus.common.*;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.library.dto.LibraryRequests;
import com.toh520.campus.module.library.repository.LibraryRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 借还书在短事务内更新库存和记录；身份来自会话，重复归还不再增加库存。 */
@Service
public class LibraryService {
  private final LibraryRepository library;
  private final TransactionTemplate transactions;

  public LibraryService(LibraryRepository library, TransactionTemplate transactions) {
    this.library = library;
    this.transactions = transactions;
  }

  public List<Map<String, Object>> search(String query) {
    BusinessException.require(query.length() <= 150, "检索内容过长");
    return library.search(query.trim());
  }

  public Map<String, Object> draft(LoginSession session, long bookId) {
    session.requireStudent();
    synchronized (session) {
      session.invalidate("borrow");
      session.invalidate("leave");
      var book = library.find(bookId);
      BusinessException.require(((Number) book.get("availableCount")).intValue() > 0, "暂无可借图书");
      return Map.of("book", book, "token", session.issue("borrow", bookId));
    }
  }

  public Object confirm(LoginSession session, String token) {
    session.requireStudent();
    synchronized (session) {
      var confirmation = session.confirmation(token, "borrow");
      if (confirmation.result != null) return confirmation.result;
      confirmation.result =
          transactions.execute(
              status -> {
                long bookId = (Long) confirmation.payload;
                BusinessException.require(library.take(bookId) == 1, "库存已不足，请重新查询");
                // 唯一在借索引失败会回滚前面的扣库存，不能吞掉此异常。
                long recordId = library.borrow(session.userId, bookId, CampusTime.now().toString());
                return Map.of("id", recordId, "message", "借书成功");
              });
      return confirmation.result;
    }
  }

  public Object returnBook(LoginSession session, long id) {
    session.requireStudent();
    return transactions.execute(
        status -> {
          var record = library.record(id, session.userId);
          if (library.returnRecord(id, session.userId, CampusTime.now().toString()) == 1)
            library.restore(((Number) record.get("bookId")).longValue());
          return Map.of("id", id, "message", "已归还");
        });
  }

  public List<Map<String, Object>> records(LoginSession session, boolean all) {
    if (all) session.requireAdmin();
    else session.requireStudent();
    return library.records(all ? null : session.userId);
  }

  public void save(LoginSession session, Long id, LibraryRequests.Book book) {
    session.requireAdmin();
    library.save(id, book);
  }

  public void delete(LoginSession session, long id) {
    session.requireAdmin();
    library.delete(id);
  }
}
