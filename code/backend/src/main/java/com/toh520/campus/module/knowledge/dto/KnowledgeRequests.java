package com.toh520.campus.module.knowledge.dto;

import jakarta.validation.constraints.*;

public final class KnowledgeRequests {
  private KnowledgeRequests() {}

  public record Entry(
      @NotBlank @Size(max = 150) String title,
      @NotNull @Pattern(regexp = "rule|course|activity|other") String category,
      @NotBlank @Size(max = 4000) String content,
      String revision) {}

  public record Question(@NotBlank @Size(max = 1000) String message) {}
}
