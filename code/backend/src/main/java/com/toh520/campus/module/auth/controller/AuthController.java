package com.toh520.campus.module.auth.controller;

import com.toh520.campus.module.auth.dto.AuthRequests;
import com.toh520.campus.module.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @GetMapping("/health")
  Object health() {
    return Map.of("status", "ok");
  }

  @PostMapping("/auth/register")
  Object register(@Valid @RequestBody AuthRequests.Register request) {
    auth.register(request);
    return Map.of("message", "注册成功，请使用学号登录");
  }

  @PostMapping("/auth/login")
  Object login(@Valid @RequestBody AuthRequests.Login request) {
    String token = auth.login(request);
    // 新凭据由当前标签页保存，不能再回退到共享 Cookie 鉴权。
    return Map.of("role", auth.authenticate(token).role, "token", token);
  }

  @PostMapping("/auth/logout")
  Object logout(@RequestAttribute("sessionToken") String token) {
    auth.logout(token);
    return Map.of("message", "已退出");
  }
}
