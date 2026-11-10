package com.toh520.campus.common;

import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  ResponseEntity<?> business(BusinessException error) {
    return ResponseEntity.status(error.status()).body(Map.of("message", error.getMessage()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
    java.time.DateTimeException.class
  })
  ResponseEntity<?> invalid(Exception error) {
    return ResponseEntity.badRequest().body(Map.of("message", "输入字段、格式或范围不正确，请检查后重试"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict(Exception error) {
    return ResponseEntity.status(409).body(Map.of("message", "数据重复、存在关联记录或状态已改变，请刷新后重试"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception error) {
    // 仅记异常类型，避免 SQL 参数、身体信息和供应方返回值进入日志。
    LoggerFactory.getLogger(getClass()).error("接口处理失败：{}", error.getClass().getSimpleName());
    return ResponseEntity.internalServerError().body(Map.of("message", "服务处理失败，请稍后重试"));
  }
}
