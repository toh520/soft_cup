package com.toh520.campus.module.auth.model;

import com.toh520.campus.common.BusinessException;
import java.time.Instant;
import java.util.*;

/** 单进程、登录会话隔离的短期状态；业务确认检查、事务与结果缓存使用同一会话锁。 */
public class LoginSession {
  public final long userId;
  public final String role;
  public final Instant expiresAt = Instant.now().plusSeconds(8 * 3600);
  private final Map<String, Confirmation> confirmations = new HashMap<>();
  public final List<Map<String, String>> assistantMessages = new ArrayList<>();
  public final List<Map<String, String>> knowledgeMessages = new ArrayList<>();
  public Map<String, Object> assistantDraft;
  public List<Map<String, Object>> mealPlans = List.of();
  public Map<String, Object> cart;

  public LoginSession(long userId, String role) {
    this.userId = userId;
    this.role = role;
  }

  public void requireStudent() {
    if (!role.equals("student")) throw new BusinessException(403, "该功能仅限学生使用");
  }

  public void requireAdmin() {
    if (!role.equals("admin")) throw new BusinessException(403, "需要管理员权限");
  }

  public String issue(String kind, Object payload) {
    confirmations.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(Instant.now()));
    String token = UUID.randomUUID().toString();
    confirmations.put(token, new Confirmation(kind, payload));
    return token;
  }

  public Confirmation confirmation(String token, String kind) {
    Confirmation confirmation = confirmations.get(token);
    if (confirmation == null
        || !confirmation.kind.equals(kind)
        || confirmation.expiresAt.isBefore(Instant.now()))
      throw new BusinessException(409, "确认已失效，请重新生成并确认");
    return confirmation;
  }

  public void invalidate(String kind) {
    confirmations
        .entrySet()
        .removeIf(entry -> entry.getValue().kind.equals(kind) && entry.getValue().result == null);
  }

  public static class Confirmation {
    public final String kind;
    public final Object payload;
    public Object result;
    public final Instant expiresAt = Instant.now().plusSeconds(900);

    Confirmation(String kind, Object payload) {
      this.kind = kind;
      this.payload = payload;
    }
  }
}
