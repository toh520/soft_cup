package com.toh520.campus.module.assistant.controller;

import com.toh520.campus.module.assistant.service.AssistantService;
import com.toh520.campus.module.auth.model.LoginSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
  public record Message(@NotBlank @Size(max = 1000) String message) {}

  private final AssistantService assistant;

  public AssistantController(AssistantService assistant) {
    this.assistant = assistant;
  }

  @PostMapping("/messages")
  Object message(@RequestAttribute LoginSession session, @Valid @RequestBody Message request) {
    return assistant.message(session, request.message());
  }

  @DeleteMapping("/conversation")
  Object clear(@RequestAttribute LoginSession session) {
    assistant.clear(session);
    return Map.of("message", "会话已清空");
  }
}
