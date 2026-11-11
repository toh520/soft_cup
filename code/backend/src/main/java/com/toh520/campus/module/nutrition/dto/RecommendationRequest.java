package com.toh520.campus.module.nutrition.dto;

import jakarta.validation.constraints.Size;

public record RecommendationRequest(@Size(max = 300) String requirements) {}
