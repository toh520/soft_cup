package com.toh520.campus.module.knowledge.service;

import com.fasterxml.jackson.databind.*;
import com.toh520.campus.common.*;
import com.toh520.campus.integration.embedding.EmbeddingClient;
import com.toh520.campus.integration.llm.ChatModelClient;
import com.toh520.campus.module.auth.model.LoginSession;
import com.toh520.campus.module.knowledge.dto.KnowledgeRequests;
import com.toh520.campus.module.knowledge.repository.KnowledgeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 向量检索仅选择依据；无命中不调用聊天模型，来源逐项核对数据库正文。 */
@Service
public class KnowledgeService {
  private static final String NO_ANSWER = "暂未查询到相应的知识点。";
  private final KnowledgeRepository entries;
  private final EmbeddingClient embedding;
  private final ChatModelClient chat;
  private final JsonSupport json;
  private final ObjectMapper mapper;
  private final TransactionTemplate transactions;
  private final String minSimilarity;
  private final int topK;

  public KnowledgeService(
      KnowledgeRepository entries,
      EmbeddingClient embedding,
      ChatModelClient chat,
      JsonSupport json,
      ObjectMapper mapper,
      TransactionTemplate transactions,
      @Value("${rag.min-similarity:}") String minSimilarity,
      @Value("${rag.top-k}") int topK) {
    this.entries = entries;
    this.embedding = embedding;
    this.chat = chat;
    this.json = json;
    this.mapper = mapper;
    this.transactions = transactions;
    this.minSimilarity = minSimilarity;
    this.topK = topK;
  }

  private String revision(Map<String, Object> entry) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(json.write(entry).getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public Object list(LoginSession session, String category) {
    session.requireAdmin();
    return entries.all().stream()
        .filter(e -> category == null || category.isBlank() || category.equals(e.get("category")))
        .map(
            e ->
                Map.of(
                    "id",
                    e.get("id"),
                    "title",
                    e.get("title"),
                    "category",
                    e.get("category"),
                    "content",
                    e.get("content"),
                    "revision",
                    revision(e),
                    "compatible",
                    embedding.identity().equals(e.get("embeddingModel")),
                    "indexStatus",
                    "pending".equals(e.get("embeddingModel"))
                        ? "待向量化"
                        : (embedding.identity().equals(e.get("embeddingModel")) ? "已就绪" : "模型不兼容")))
        .toList();
  }

  private Map<String, Object> find(long id) {
    return entries.all().stream()
        .filter(e -> ((Number) e.get("id")).longValue() == id)
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "知识条目不存在"));
  }

  public void save(LoginSession session, Long id, KnowledgeRequests.Entry request) {
    session.requireAdmin();
    Map<String, Object> old = id == null ? null : find(id);
    if (old != null && !revision(old).equals(request.revision()))
      throw new BusinessException(409, "资料已变更，请刷新后重试");
    // 网络向量化在事务外完成；失败保留完整旧版本，事务只做最终一致写入。
    String vector = json.write(embedding.embed(request.title() + "\n" + request.content()));
    transactions.executeWithoutResult(
        status -> {
          if (old == null)
            entries.insert(
                request.title(),
                request.category(),
                request.content(),
                vector,
                embedding.identity());
          else if (entries.update(
                  old,
                  request.title(),
                  request.category(),
                  request.content(),
                  vector,
                  embedding.identity())
              != 1) throw new BusinessException(409, "资料在向量化期间被修改或删除，请刷新");
        });
  }

  public void delete(LoginSession session, long id, String expectedRevision) {
    session.requireAdmin();
    var old = find(id);
    if (!revision(old).equals(expectedRevision) || entries.delete(old) != 1)
      throw new BusinessException(409, "资料已变更，请刷新");
  }

  public void rebuild(LoginSession session) {
    session.requireAdmin();
    var snapshot = entries.all();
    var vectors = new ArrayList<String>();
    for (var entry : snapshot)
      vectors.add(json.write(embedding.embed(entry.get("title") + "\n" + entry.get("content"))));
    // 全库生成成功后一次切换；并发编辑或新增均拒绝此次重建，避免新旧空间混用。
    transactions.executeWithoutResult(
        status -> {
          if (!json.write(entries.all()).equals(json.write(snapshot)))
            throw new BusinessException(409, "重建期间资料发生变化，请重试");
          for (int index = 0; index < snapshot.size(); index++) {
            var old = snapshot.get(index);
            if (entries.update(
                    old,
                    (String) old.get("title"),
                    (String) old.get("category"),
                    (String) old.get("content"),
                    vectors.get(index),
                    embedding.identity())
                != 1) throw new BusinessException(409, "重建冲突，请重试");
          }
        });
  }

  public Object answer(LoginSession session, String question) {
    session.requireStudent();
    synchronized (session) {
      var result = (Map<String, Object>) answerCurrent(session, question);
      session.knowledgeMessages.add(Map.of("role", "user", "content", question));
      session.knowledgeMessages.add(
          Map.of("role", "assistant", "content", (String) result.get("answer")));
      while (session.knowledgeMessages.size() > 8) session.knowledgeMessages.removeFirst();
      return result;
    }
  }

  /** 知识上下文只用于理解指代；最终事实仍必须来自本次检索和引用校验。 */
  private Object answerCurrent(LoginSession session, String question) {
    session.requireStudent();
    if (question.matches("(?s).*(帮我.{0,8}(请假|借书|还书)|我的课表|我今天.{0,4}课|查.{0,5}课表).*"))
      return Map.of(
          "answer", "请前往校园服务助手办理或查询本人课表。", "sources", List.of(), "redirect", "/assistant");
    var all = entries.all();
    if (all.isEmpty()) return Map.of("answer", NO_ANSWER, "sources", List.of());
    if (all.stream().anyMatch(e -> "pending".equals(e.get("embeddingModel"))))
      throw new BusinessException(503, "演示知识资料已录入，尚未向量化。请配置 embedding 服务后由管理员重建全部向量。");
    double threshold;
    try {
      threshold = Double.parseDouble(minSimilarity);
      if (!Double.isFinite(threshold) || threshold < -1 || threshold > 1 || topK < 1 || topK > 10)
        throw new IllegalArgumentException();
    } catch (Exception e) {
      throw new BusinessException(503, "RAG 相关性门槛尚未校准，请管理员配置");
    }
    if (all.stream().anyMatch(e -> !embedding.identity().equals(e.get("embeddingModel"))))
      throw new BusinessException(503, "知识库向量与当前模型不兼容，请管理员重建后再提问");
    String retrievalQuestion = question;
    if (question.matches("(?s).*(它|那|这个|这样|上述|刚才).*")) {
      retrievalQuestion =
          session.knowledgeMessages.stream()
                  .filter(message -> message.get("role").equals("user"))
                  .map(message -> message.get("content"))
                  .reduce("", (left, right) -> left + right + "\n")
              + question;
    }
    double[] query = embedding.embed(retrievalQuestion);
    var ranked = new ArrayList<Map<String, Object>>();
    for (var entry : all) {
      double[] vector;
      try {
        vector = mapper.readValue((String) entry.get("embedding"), double[].class);
      } catch (Exception e) {
        throw new BusinessException(503, "知识库向量损坏，请管理员重建");
      }
      embedding.validate(vector);
      double score = cosine(query, vector);
      if (score >= threshold) {
        var candidate = new LinkedHashMap<String, Object>();
        candidate.put("id", entry.get("id"));
        candidate.put("title", entry.get("title"));
        candidate.put("content", entry.get("content"));
        candidate.put("score", score);
        ranked.add(candidate);
      }
    }
    ranked.sort(Comparator.comparingDouble(e -> -((Number) e.get("score")).doubleValue()));
    var candidates = ranked.stream().limit(topK).toList();
    if (candidates.isEmpty()) return Map.of("answer", NO_ANSWER, "sources", List.of());
    var response =
        chat.complete(
            "knowledge/answer",
            Map.of(
                "question", question, "entries", candidates, "history", session.knowledgeMessages));
    if (!response.path("answerable").isBoolean())
      throw new BusinessException(502, "知识回答结构校验失败，请重试");
    if (!response.path("answerable").asBoolean())
      return Map.of("answer", NO_ANSWER, "sources", List.of());
    var sources = new ArrayList<Map<String, Object>>();
    if (!response.path("citations").isArray() || response.path("answer").asText().isBlank())
      throw new BusinessException(502, "知识回答缺少有效依据，请重试");
    for (var citation : response.path("citations")) {
      var candidate =
          candidates.stream()
              .filter(
                  e ->
                      citation.path("id").isIntegralNumber()
                          && ((Number) e.get("id")).longValue() == citation.path("id").asLong())
              .findFirst()
              .orElseThrow(() -> new BusinessException(502, "知识回答引用来源校验失败"));
      String quote = citation.path("quote").asText("");
      if (quote.isBlank() || !((String) candidate.get("content")).contains(quote))
        throw new BusinessException(502, "知识回答原文校验失败");
      sources.add(
          Map.of("id", candidate.get("id"), "title", candidate.get("title"), "quote", quote));
    }
    if (sources.isEmpty()) throw new BusinessException(502, "知识回答没有引用依据");
    return Map.of("answer", response.path("answer").asText(), "sources", sources);
  }

  public void clear(LoginSession session) {
    session.requireStudent();
    synchronized (session) {
      session.knowledgeMessages.clear();
    }
  }

  public static double cosine(double[] left, double[] right) {
    double dot = 0, a = 0, b = 0;
    for (int index = 0; index < left.length; index++) {
      dot += left[index] * right[index];
      a += left[index] * left[index];
      b += right[index] * right[index];
    }
    return (dot / Math.sqrt(a)) / Math.sqrt(b);
  }
}
