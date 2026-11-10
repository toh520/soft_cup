package com.toh520.campus.module.library.dto;

import jakarta.validation.constraints.*;

public final class LibraryRequests {
  private LibraryRequests() {}

  public record Book(
      @NotBlank @Size(max = 150) String title,
      @NotBlank @Size(max = 80) String author,
      @NotBlank @Size(max = 2000) String summary,
      @NotNull @Min(0) @Max(100000) Integer availableCount) {}

  public record Confirm(@NotBlank String token) {}
}
