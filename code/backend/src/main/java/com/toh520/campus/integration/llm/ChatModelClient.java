package com.toh520.campus.integration.llm;

import com.fasterxml.jackson.databind.*;
import com.toh520.campus.common.*;
import com.toh520.campus.integration.ModelTransport;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** 提示词按业务资源隔离；只解析 JSON 意图和候选选择，绝不执行业务写入。 */
@Component
public class ChatModelClient {
  private final ModelTransport transport;
  private final ObjectMapper mapper;
  private final JsonSupport json;
  private final String baseUrl, model, key;
  private final int timeout;

  public ChatModelClient(
      ModelTransport transport,
      ObjectMapper mapper,
      JsonSupport json,
      @Value("${llm.base-url}") String baseUrl,
      @Value("${llm.model}") String model,
      @Value("${llm.api-key}") String key,
      @Value("${llm.timeout-seconds}") int timeout) {
    this.transport = transport;
    this.mapper = mapper;
    this.json = json;
    this.baseUrl = baseUrl;
    this.model = model;
    this.key = key;
    this.timeout = timeout;
  }

  public JsonNode complete(String prompt, Object input) {
    try {
      String system =
          new ClassPathResource("prompts/" + prompt + ".txt")
              .getContentAsString(StandardCharsets.UTF_8);
      var response =
          transport.post(
              baseUrl,
              "/chat/completions",
              key,
              Map.of(
                  "model",
                  model,
                  "messages",
                  List.of(
                      Map.of("role", "system", "content", system),
                      Map.of("role", "user", "content", json.write(input)))),
              timeout);
      String content =
          response.path("choices").path(0).path("message").path("content").asText("").trim();
      if (content.startsWith("```"))
        content = content.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
      JsonNode parsed = mapper.readTree(content);
      if (parsed == null || !parsed.isObject()) throw new IllegalArgumentException();
      return parsed;
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(502, "模型返回结构无效，请重试");
    }
  }
}
