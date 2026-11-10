package com.toh520.campus.module.assistant.service;

import com.toh520.campus.common.*;
import com.toh520.campus.integration.llm.ChatModelClient;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.leave.dto.LeaveRequests;
import com.toh520.campus.module.leave.service.LeaveService;
import com.toh520.campus.module.library.service.LibraryService;
import com.toh520.campus.module.schedule.service.ScheduleService;
import com.toh520.campus.module.user.service.UserService;
import java.util.*;
import org.springframework.stereotype.Service;

/** LLM 仅返回白名单意图；本模块无 Repository、无知识检索和业务写入工具。 */
@Service
public class AssistantService {
  private final ChatModelClient chat;
  private final LeaveService leaves;
  private final LibraryService library;
  private final ScheduleService schedules;
  private final UserService users;

  public AssistantService(
      ChatModelClient chat,
      LeaveService leaves,
      LibraryService library,
      ScheduleService schedules,
      UserService users) {
    this.chat = chat;
    this.leaves = leaves;
    this.library = library;
    this.schedules = schedules;
    this.users = users;
  }

  public Object message(LoginSession session, String message) {
    session.requireStudent();
    synchronized (session) {
      // 新消息可能修改意图或条件，因此即使模型失败，旧的待确认卡片也不能继续提交。
      session.invalidate("leave");
      session.invalidate("borrow");
      var response =
          chat.complete(
              "assistant/intent",
              Map.of(
                  "now",
                  CampusTime.now().toString(),
                  "history",
                  session.assistantMessages,
                  "currentDraft",
                  session.assistantDraft == null ? Map.of() : session.assistantDraft,
                  "message",
                  message));
      String intent = response.path("intent").asText();
      String reply = response.path("reply").asText("请继续补充信息");
      var result = new LinkedHashMap<String, Object>();
      result.put("intent", intent);
      result.put("reply", reply);
      if (!Set.of("leave", "books", "schedule", "knowledge", "nutrition", "clarify")
          .contains(intent)) throw new BusinessException(502, "模型返回了不支持的业务意图，请重试");
      if (!intent.equals("leave")) session.assistantDraft = null;
      switch (intent) {
        case "leave" -> {
          var draft = new LinkedHashMap<String, Object>();
          for (String field : List.of("startAt", "endAt", "reason")) {
            String value = response.path(field).asText("");
            draft.put(field, value);
          }
          session.assistantDraft = new LinkedHashMap<>(draft);
          var profile = users.profile(session);
          draft.put("name", profile.get("name"));
          draft.put("studentNumber", profile.get("studentNumber"));
          if (List.of("startAt", "endAt", "reason").stream()
              .allMatch(field -> !((String) draft.get(field)).isBlank())) {
            try {
              draft.putAll(
                  leaves.draft(
                      session,
                      new LeaveRequests.Draft(
                          (String) draft.get("startAt"),
                          (String) draft.get("endAt"),
                          (String) draft.get("reason"))));
            } catch (BusinessException e) {
              result.put("reply", e.getMessage() + "，请修改请假信息。");
            }
          }
          result.put("draft", draft);
        }
        case "books" -> {
          String query = response.path("query").asText("");
          if (query.isBlank()) result.put("reply", "请提供想查询的书名、作者或内容关键词。");
          else {
            var books = library.search(query);
            result.put("books", books);
            result.put("reply", books.isEmpty() ? "未找到符合条件的图书，请换个关键词。" : "已查询图书，请选定一本并核对借阅确认卡片。");
          }
        }
        case "schedule" -> {
          var day = response.path("weekday");
          if (!day.isNull() && !day.isMissingNode() && !day.isIntegralNumber())
            throw new BusinessException(502, "模型返回的星期无效，请重试");
          result.put(
              "schedule",
              schedules.mine(
                  session,
                  day.isIntegralNumber() ? day.asInt() : null,
                  response.path("date").asText(null)));
          result.put("reply", "已读取本人班级的最新课表。");
        }
        case "knowledge" -> {
          result.put("reply", "校园规定和资料请前往校园知识问答查询。");
          result.put("redirect", "/knowledge");
        }
        case "nutrition" -> {
          result.put("reply", "请前往营养食堂生成套餐并在购物车下单。");
          result.put("redirect", "/nutrition");
        }
        default -> {}
      }
      session.assistantMessages.add(Map.of("role", "user", "content", message));
      session.assistantMessages.add(
          Map.of("role", "assistant", "content", (String) result.get("reply")));
      while (session.assistantMessages.size() > 12) session.assistantMessages.removeFirst();
      return result;
    }
  }

  public void clear(LoginSession session) {
    session.requireStudent();
    synchronized (session) {
      session.assistantMessages.clear();
      session.assistantDraft = null;
      session.invalidate("leave");
      session.invalidate("borrow");
    }
  }
}
