package com.toh520.campus.module.schedule.service;

import com.toh520.campus.common.*;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.schedule.dto.ScheduleRequest;
import com.toh520.campus.module.schedule.repository.ScheduleRepository;
import com.toh520.campus.module.user.service.UserService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 固定周课表按学生当前班级查询；课程冲突检查与保存处于同一事务。 */
@Service
public class ScheduleService {
  private final ScheduleRepository schedules;
  private final UserService users;
  private final TransactionTemplate transactions;

  public ScheduleService(
      ScheduleRepository schedules, UserService users, TransactionTemplate transactions) {
    this.schedules = schedules;
    this.users = users;
    this.transactions = transactions;
  }

  public Object mine(LoginSession session, Integer weekday, String date) {
    session.requireStudent();
    if (date != null && !date.isBlank()) weekday = LocalDate.parse(date).getDayOfWeek().getValue();
    BusinessException.require(weekday == null || weekday >= 1 && weekday <= 7, "星期必须为 1～7");
    Object classId = users.profile(session).get("classId");
    if (classId == null) return Map.of("courses", List.of(), "message", "尚未分配班级，请联系管理员");
    var courses = schedules.list(((Number) classId).longValue(), weekday);
    String message =
        courses.isEmpty()
            ? (schedules.list(((Number) classId).longValue(), null).isEmpty() ? "课表尚未配置" : "当天无课程")
            : "已读取最新课表";
    return Map.of("courses", courses, "message", message);
  }

  public Object all(LoginSession session, long classId) {
    session.requireAdmin();
    return schedules.list(classId, null);
  }

  public void save(LoginSession session, Long id, ScheduleRequest request) {
    session.requireAdmin();
    BusinessException.require(request.startTime().compareTo(request.endTime()) < 0, "开始时间必须早于结束时间");
    transactions.executeWithoutResult(
        status -> {
          BusinessException.require(schedules.conflicts(id, request) == 0, "同班级同星期课程时间冲突");
          schedules.save(id, request);
        });
  }

  public void delete(LoginSession session, long id) {
    session.requireAdmin();
    schedules.delete(id);
  }
}
