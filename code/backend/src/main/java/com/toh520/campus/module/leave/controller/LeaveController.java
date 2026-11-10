package com.toh520.campus.module.leave.controller;

import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.leave.dto.LeaveRequests;
import com.toh520.campus.module.leave.service.LeaveService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LeaveController {
  private final LeaveService leaves;

  public LeaveController(LeaveService leaves) {
    this.leaves = leaves;
  }

  @PostMapping("/leave-requests/draft")
  Object draft(
      @RequestAttribute LoginSession session, @Valid @RequestBody LeaveRequests.Draft request) {
    return leaves.draft(session, request);
  }

  @PostMapping("/leave-requests/confirm")
  Object confirm(
      @RequestAttribute LoginSession session, @Valid @RequestBody LeaveRequests.Confirm request) {
    return leaves.confirm(session, request.token());
  }

  @PostMapping("/leave-requests/invalidate")
  Object invalidate(@RequestAttribute LoginSession session) {
    session.requireStudent();
    leaves.invalidate(session);
    return Map.of("message", "请修改后重新预览");
  }

  @GetMapping("/leave-requests")
  Object mine(@RequestAttribute LoginSession session) {
    return leaves.mine(session);
  }

  @GetMapping("/admin/leave-requests")
  Object all(
      @RequestAttribute LoginSession session, @RequestParam(required = false) String status) {
    return leaves.all(session, status);
  }

  @PostMapping("/admin/leave-requests/{id}/approval")
  Object approve(
      @RequestAttribute LoginSession session,
      @PathVariable long id,
      @Valid @RequestBody LeaveRequests.Approval request) {
    leaves.approve(session, id, request.status());
    return Map.of("message", "审批已完成");
  }
}
