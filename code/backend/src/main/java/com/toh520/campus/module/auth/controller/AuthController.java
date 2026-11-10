package com.toh520.campus.module.auth.controller;

import com.toh520.campus.module.auth.dto.AuthRequests;
import com.toh520.campus.module.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseCookie;
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
  Object login(@Valid @RequestBody AuthRequests.Login request, HttpServletResponse response) {
    String token = auth.login(request);
    response.addHeader("Set-Cookie", cookie(token, 28800));
    return Map.of("role", auth.authenticate(token).role);
  }

  @PostMapping("/auth/logout")
  Object logout(
      @CookieValue(name = "CAMPUS_SESSION", required = false) String token,
      HttpServletResponse response) {
    auth.logout(token);
    response.addHeader("Set-Cookie", cookie("", 0));
    return Map.of("message", "已退出");
  }

  private String cookie(String value, long seconds) {
    return ResponseCookie.from("CAMPUS_SESSION", value)
        .httpOnly(true)
        .sameSite("Strict")
        .path("/api")
        .maxAge(seconds)
        .build()
        .toString();
  }
}
