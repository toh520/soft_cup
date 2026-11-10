package com.toh520.campus.module.schedule.dto;

import jakarta.validation.constraints.*;

public record ScheduleRequest(
    @Positive long classId,
    @NotBlank @Size(max = 100) String courseName,
    @NotBlank @Size(max = 50) String teacher,
    @Min(1) @Max(7) int weekday,
    @NotBlank @Pattern(regexp = "(?:[01][0-9]|2[0-3]):[0-5][0-9]") String startTime,
    @NotBlank @Pattern(regexp = "(?:[01][0-9]|2[0-3]):[0-5][0-9]") String endTime,
    @NotBlank @Size(max = 100) String classroom) {}
