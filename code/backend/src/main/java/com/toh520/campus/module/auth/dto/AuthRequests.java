package com.toh520.campus.module.auth.dto;

import jakarta.validation.constraints.*;

public final class AuthRequests {
  private AuthRequests() {}

  public record Register(
      @NotBlank @Size(max = 50) String name,
      @NotBlank @Pattern(regexp = "[A-Za-z0-9]{1,32}") String studentNumber,
      @NotBlank @Size(min = 6, max = 64) String password) {}

  public record Login(
      @NotBlank @Size(max = 32) String account, @NotBlank @Size(max = 64) String password) {}
}
