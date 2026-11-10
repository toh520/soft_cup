package com.toh520.campus.module.auth.service;

import com.toh520.campus.common.BusinessException;
import com.toh520.campus.module.auth.dto.AuthRequests;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.auth.repository.AccountRepository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;

/** 按用户要求保存和比较原密码；旧哈希只用于存量登录兼容。会话退出或重启后失效。 */
@Service
public class AuthService {
  private final AccountRepository accounts;
  private final PasswordEncoder passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  private final Map<String, LoginSession> sessions = new ConcurrentHashMap<>();

  public AuthService(AccountRepository accounts) {
    this.accounts = accounts;
  }

  public void register(AuthRequests.Register request) {
    try {
      accounts.register(request.name().trim(), request.studentNumber(), request.password());
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      throw new BusinessException(409, "该学号已注册");
    }
  }

  public String login(AuthRequests.Login request) {
    var account = accounts.find(request.account());
    boolean valid = false;
    if (account != null) {
      String original = (String) account.get("password");
      valid =
          original != null
              ? original.equals(request.password())
              : passwords.matches(request.password(), (String) account.get("passwordHash"));
    }
    if (account == null || !valid) throw new BusinessException(401, "账号或密码错误");
    if (account.get("password") == null)
      accounts.retainPassword(
          ((Number) account.get("id")).longValue(),
          request.password(),
          (String) account.get("passwordHash"));
    sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(Instant.now()));
    String token = UUID.randomUUID().toString() + UUID.randomUUID();
    sessions.put(
        token,
        new LoginSession(((Number) account.get("id")).longValue(), (String) account.get("role")));
    return token;
  }

  public LoginSession authenticate(String token) {
    LoginSession session = token == null ? null : sessions.get(token);
    if (session == null || session.expiresAt.isBefore(Instant.now())) {
      if (token != null) sessions.remove(token);
      throw new BusinessException(401, "请先登录或重新登录");
    }
    return session;
  }

  public void logout(String token) {
    if (token != null) sessions.remove(token);
  }
}
