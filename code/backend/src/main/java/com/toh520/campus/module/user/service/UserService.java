package com.toh520.campus.module.user.service;

import com.toh520.campus.common.BusinessException;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.user.dto.UserRequests;
import com.toh520.campus.module.user.repository.UserRepository;
import java.util.*;
import org.springframework.stereotype.Service;

/** 个人资料仅从登录身份定位；学生不能修改学号、角色及班级。 */
@Service
public class UserService {
  private final UserRepository users;

  public UserService(UserRepository users) {
    this.users = users;
  }

  public Map<String, Object> profile(LoginSession session) {
    return users.find(session.userId);
  }

  public Map<String, Object> update(LoginSession session, UserRequests.Profile request) {
    session.requireStudent();
    users.update(session.userId, request);
    return profile(session);
  }

  public List<Map<String, Object>> students(LoginSession session) {
    session.requireAdmin();
    return users.students();
  }

  public List<Map<String, Object>> classes(LoginSession session) {
    session.requireAdmin();
    return users.classes();
  }

  public void updateStudent(LoginSession session, long id, UserRequests.Student request) {
    session.requireAdmin();
    BusinessException.require(users.updateStudent(id, request) == 1, "学生不存在");
  }

  public void resetPassword(LoginSession session, long id, String password) {
    session.requireAdmin();
    BusinessException.require(users.resetPassword(id, password) == 1, "学生不存在");
  }
}
