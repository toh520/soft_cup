package com.toh520.campus.common;

/** 可向用户展示的业务失败，不携带数据库或模型供应方的敏感响应。 */
public class BusinessException extends RuntimeException {
  private final int status;

  public BusinessException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return status;
  }

  public static void require(boolean condition, String message) {
    if (!condition) throw new BusinessException(400, message);
  }
}
