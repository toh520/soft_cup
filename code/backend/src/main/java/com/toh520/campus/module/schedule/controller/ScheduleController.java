package com.toh520.campus.module.schedule.controller;

import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.schedule.dto.ScheduleRequest;
import com.toh520.campus.module.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ScheduleController {
  private final ScheduleService schedules;

  public ScheduleController(ScheduleService schedules) {
    this.schedules = schedules;
  }

  @GetMapping("/schedules")
  Object mine(
      @RequestAttribute LoginSession session,
      @RequestParam(required = false) Integer weekday,
      @RequestParam(required = false) String date) {
    return schedules.mine(session, weekday, date);
  }

  @GetMapping("/admin/schedules")
  Object all(
      @RequestAttribute LoginSession session, @RequestParam(defaultValue = "1") long classId) {
    return schedules.all(session, classId);
  }

  @PostMapping("/admin/schedules")
  Object create(
      @RequestAttribute LoginSession session, @Valid @RequestBody ScheduleRequest request) {
    schedules.save(session, null, request);
    return Map.of("message", "课程已新增");
  }

  @PutMapping("/admin/schedules/{id}")
  Object edit(
      @RequestAttribute LoginSession session,
      @PathVariable long id,
      @Valid @RequestBody ScheduleRequest request) {
    schedules.save(session, id, request);
    return Map.of("message", "课程已更新");
  }

  @DeleteMapping("/admin/schedules/{id}")
  Object delete(@RequestAttribute LoginSession session, @PathVariable long id) {
    schedules.delete(session, id);
    return Map.of("message", "课程已删除");
  }
}
