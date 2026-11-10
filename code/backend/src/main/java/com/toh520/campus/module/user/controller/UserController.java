package com.toh520.campus.module.user.controller;

import com.toh520.campus.module.auth.dto.AuthRequests;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.auth.service.AuthService;
import com.toh520.campus.module.user.dto.UserRequests;
import com.toh520.campus.module.user.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {
  private final UserService users;
  private final AuthService auth;

  public UserController(UserService users, AuthService auth) {
    this.users = users;
    this.auth = auth;
  }

  @GetMapping("/profile")
  Object profile(@RequestAttribute LoginSession session) {
    return users.profile(session);
  }

  @PutMapping("/profile")
  Object update(
      @RequestAttribute LoginSession session, @Valid @RequestBody UserRequests.Profile request) {
    return users.update(session, request);
  }

  @GetMapping("/admin/students")
  Object students(@RequestAttribute LoginSession session) {
    return users.students(session);
  }

  @GetMapping("/admin/classes")
  Object classes(@RequestAttribute LoginSession session) {
    return users.classes(session);
  }

  @PostMapping("/admin/students")
  Object create(
      @RequestAttribute LoginSession session, @Valid @RequestBody AuthRequests.Register request) {
    session.requireAdmin();
    auth.register(request);
    return Map.of("message", "学生已创建");
  }

  @PutMapping("/admin/students/{id}")
  Object edit(
      @RequestAttribute LoginSession session,
      @PathVariable long id,
      @Valid @RequestBody UserRequests.Student request) {
    users.updateStudent(session, id, request);
    return Map.of("message", "学生已更新");
  }

  @PostMapping("/admin/students/{id}/password")
  Object password(
      @RequestAttribute LoginSession session,
      @PathVariable long id,
      @Valid @RequestBody UserRequests.Password request) {
    users.resetPassword(session, id, request.password());
    return Map.of("message", "密码已重置");
  }
}
