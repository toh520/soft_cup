package com.toh520.campus.module.leave.dto;

import jakarta.validation.constraints.*;

public final class LeaveRequests {
  private LeaveRequests() {}

  public record Draft(
      @NotBlank String startAt, @NotBlank String endAt, @NotBlank @Size(max = 500) String reason) {}

  public record Confirm(@NotBlank String token) {}

  public record Approval(@NotNull @Pattern(regexp = "approved|rejected") String status) {}
}
