package com.toh520.campus.module.knowledge.controller;

import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.knowledge.dto.KnowledgeRequests;
import com.toh520.campus.module.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class KnowledgeController {
  private final KnowledgeService knowledge;

  public KnowledgeController(KnowledgeService knowledge) {
    this.knowledge = knowledge;
  }

  @PostMapping("/knowledge/questions")
  Object question(
      @RequestAttribute LoginSession session,
      @Valid @RequestBody KnowledgeRequests.Question request) {
    return knowledge.answer(session, request.message());
  }

  @DeleteMapping("/knowledge/conversation")
  Object clear(@RequestAttribute LoginSession session) {
    knowledge.clear(session);
    return Map.of("message", "知识会话已清空");
  }

  @GetMapping("/admin/knowledge-entries")
  Object list(
      @RequestAttribute LoginSession session, @RequestParam(required = false) String category) {
    return knowledge.list(session, category);
  }

  @PostMapping("/admin/knowledge-entries")
  Object create(
      @RequestAttribute LoginSession session, @Valid @RequestBody KnowledgeRequests.Entry request) {
    knowledge.save(session, null, request);
    return Map.of("message", "资料及向量已保存");
  }

  @PutMapping("/admin/knowledge-entries/{id}")
  Object edit(
      @RequestAttribute LoginSession session,
      @PathVariable long id,
      @Valid @RequestBody KnowledgeRequests.Entry request) {
    knowledge.save(session, id, request);
    return Map.of("message", "资料及向量已更新");
  }

  @DeleteMapping("/admin/knowledge-entries/{id}")
  Object delete(
      @RequestAttribute LoginSession session,
      @PathVariable long id,
      @RequestParam String revision) {
    knowledge.delete(session, id, revision);
    return Map.of("message", "资料已删除");
  }

  @PostMapping("/admin/knowledge-entries/rebuild")
  Object rebuild(@RequestAttribute LoginSession session) {
    knowledge.rebuild(session);
    return Map.of("message", "全部向量已重建");
  }
}
