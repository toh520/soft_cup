package com.toh520.campus.integration.embedding;

import com.fasterxml.jackson.databind.*;
import com.toh520.campus.common.*;
import com.toh520.campus.integration.ModelTransport;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 资料和问题共用向量空间；显式维度、有限值、非零范数校验防止不兼容向量混算。 */
@Component
public class EmbeddingClient {
  private final ModelTransport transport;
  private final String baseUrl, model, key;
  private final int dimensions, timeout;

  public EmbeddingClient(
      ModelTransport transport,
      @Value("${embedding.base-url}") String baseUrl,
      @Value("${embedding.model}") String model,
      @Value("${embedding.api-key}") String key,
      @Value("${embedding.dimensions}") int dimensions,
      @Value("${embedding.timeout-seconds}") int timeout) {
    this.transport = transport;
    this.baseUrl = baseUrl;
    this.model = model;
    this.key = key;
    this.dimensions = dimensions;
    this.timeout = timeout;
  }

  public String identity() {
    return baseUrl.replaceAll("/+$", "") + "|" + model + "|" + dimensions + "|title-content-v1";
  }

  public double[] embed(String input) {
    if (model.isBlank() || dimensions <= 0)
      throw new BusinessException(503, "请配置 embedding 模型和已验证的向量维度");
    JsonNode vector =
        transport
            .post(baseUrl, "/embeddings", key, Map.of("model", model, "input", input), timeout)
            .path("data")
            .path(0)
            .path("embedding");
    if (!vector.isArray() || vector.size() != dimensions)
      throw new BusinessException(502, "向量维度异常，请检查 embedding 配置");
    double[] values = new double[dimensions];
    for (int index = 0; index < dimensions; index++) {
      if (!vector.get(index).isNumber()) throw new BusinessException(502, "向量数值无效");
      values[index] = vector.get(index).asDouble();
    }
    validate(values);
    return values;
  }

  public void validate(double[] values) {
    double norm = 0;
    for (double value : values) {
      if (!Double.isFinite(value)) throw new BusinessException(502, "向量数值无效");
      norm += value * value;
    }
    if (values.length != dimensions || !Double.isFinite(norm) || norm <= 0)
      throw new BusinessException(503, "知识向量不兼容或无效，请管理员重建");
  }
}
