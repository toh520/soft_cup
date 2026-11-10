package com.toh520.campus.module.leave.service;

import com.toh520.campus.common.*;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.leave.dto.LeaveRequests;
import com.toh520.campus.module.leave.repository.LeaveRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 请假草稿不落库；确认只写入一次，审批采用条件更新且终态不可覆盖。 */
@Service
public class LeaveService {
  private final LeaveRepository leaves;
  private final TransactionTemplate transactions;

  public LeaveService(LeaveRepository leaves, TransactionTemplate transactions) {
    this.leaves = leaves;
    this.transactions = transactions;
  }

  public Map<String, Object> draft(LoginSession session, LeaveRequests.Draft request) {
    session.requireStudent();
    validate(request);
    request =
        new LeaveRequests.Draft(
            CampusTime.parse(request.startAt()).toString(),
            CampusTime.parse(request.endAt()).toString(),
            request.reason().trim());
    synchronized (session) {
      session.invalidate("leave");
      session.invalidate("borrow");
      // 表单修改后同步收集上下文，不把确认令牌或身份字段传给模型。
      session.assistantDraft =
          Map.of(
              "startAt", request.startAt(), "endAt", request.endAt(), "reason", request.reason());
      return Map.of(
          "token",
          session.issue("leave", request),
          "startAt",
          request.startAt(),
          "endAt",
          request.endAt(),
          "reason",
          request.reason());
    }
  }

  public void invalidate(LoginSession session) {
    synchronized (session) {
      session.invalidate("leave");
    }
  }

  private void validate(LeaveRequests.Draft request) {
    var start = CampusTime.parse(request.startAt());
    var end = CampusTime.parse(request.endAt());
    BusinessException.require(start.isBefore(end), "开始时间必须早于结束时间");
    BusinessException.require(!start.isBefore(CampusTime.now()), "首版不支持补假，请选择未来时间");
    BusinessException.require(
        request.reason() != null && !request.reason().isBlank() && request.reason().length() <= 500,
        "请填写 1～500 字请假理由");
  }

  public Object confirm(LoginSession session, String token) {
    session.requireStudent();
    synchronized (session) {
      var confirmation = session.confirmation(token, "leave");
      if (confirmation.result != null) return confirmation.result;
      var draft = (LeaveRequests.Draft) confirmation.payload;
      validate(draft);
      // 事务实际提交完成后才缓存成功，避免并发重试创建两条申请。
      confirmation.result =
          transactions.execute(
              status ->
                  Map.of(
                      "id",
                      leaves.insert(
                          session.userId,
                          CampusTime.parse(draft.startAt()).toString(),
                          CampusTime.parse(draft.endAt()).toString(),
                          draft.reason().trim()),
                      "status",
                      "pending"));
      return confirmation.result;
    }
  }

  public List<Map<String, Object>> mine(LoginSession session) {
    session.requireStudent();
    return leaves.mine(session.userId);
  }

  public List<Map<String, Object>> all(LoginSession session, String status) {
    session.requireAdmin();
    BusinessException.require(
        status == null || Set.of("pending", "approved", "rejected").contains(status), "审批状态无效");
    return leaves.all(status);
  }

  public void approve(LoginSession session, long id, String status) {
    session.requireAdmin();
    transactions.executeWithoutResult(
        tx -> {
          if (leaves.approve(id, status) != 1) throw new BusinessException(409, "申请不存在或已经审批");
        });
  }
}
