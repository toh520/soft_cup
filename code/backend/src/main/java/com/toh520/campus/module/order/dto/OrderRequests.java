package com.toh520.campus.module.order.dto;

import jakarta.validation.constraints.*;

public final class OrderRequests {
  private OrderRequests() {}

  public record Selection(@NotBlank String planId, boolean replace) {}

  public record Confirm(@NotBlank String token) {}

  public record DishQuantity(@NotNull @Min(0) @Max(20) Integer quantity) {}
}
