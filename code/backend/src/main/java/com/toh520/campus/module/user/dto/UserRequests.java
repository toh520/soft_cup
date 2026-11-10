package com.toh520.campus.module.user.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public final class UserRequests {
  private UserRequests() {}

  public record Profile(
      @NotBlank @Size(max = 50) String name,
      @Min(18) @Max(80) Integer age,
      @Pattern(regexp = "male|female") String sex,
      @DecimalMin("100") @DecimalMax("250") Double heightCm,
      @DecimalMin("25") @DecimalMax("250") Double weightKg,
      @Pattern(regexp = "low|medium|high") String activityLevel,
      @NotNull @Pattern(regexp = "none|vegetarian") String dietPreference,
      @NotNull @Size(max = 30) List<@NotBlank @Size(max = 30) String> avoidedIngredients) {}

  public record Student(@NotBlank @Size(max = 50) String name, @Positive Long classId) {}

  public record Password(@NotBlank @Size(min = 6, max = 64) String password) {}
}
