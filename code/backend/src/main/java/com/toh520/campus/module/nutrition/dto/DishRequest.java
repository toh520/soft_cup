package com.toh520.campus.module.nutrition.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record DishRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @Pattern(regexp = "staple|protein|vegetable") String category,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("3000") Double portionG,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("10000") Double energyKcal,
    @NotNull @DecimalMin("0") Double proteinG,
    @NotNull @DecimalMin("0") Double fatG,
    @NotNull @DecimalMin("0") Double carbohydrateG,
    @NotNull @Size(max = 30) List<@NotBlank @Size(max = 30) String> ingredientTags,
    @NotNull Boolean isVegetarian,
    @NotNull Boolean isAvailable) {}
