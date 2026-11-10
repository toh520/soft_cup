package com.toh520.campus.integration;

import com.fasterxml.jackson.databind.*;
import com.toh520.campus.common.BusinessException;
import java.net.*;
import org.springframework.stereotype.Component;

/** 兼容接口的同步 HTTP 边界；供应方响应和密钥不向客户端或日志透传。 */
@Component
public class ModelTransport {
  private final ObjectMapper mapper;

  public ModelTransport(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public JsonNode post(String baseUrl, String path, String key, Object body, int timeoutSeconds) {
    if (baseUrl == null || baseUrl.isBlank() || key == null || key.isBlank())
      throw new BusinessException(503, "模型服务尚未配置，请联系管理员");
    HttpURLConnection connection = null;
    try {
      URI uri = URI.create(baseUrl.replaceAll("/+$", "") + path);
      if (!uri.getScheme().equals("https") && !uri.getScheme().equals("http"))
        throw new IllegalArgumentException();
      // 单次同步请求无需 NIO selector，避免部分 Windows 环境的 AF_UNIX 回环兼容问题。
      connection = (HttpURLConnection) uri.toURL().openConnection();
      connection.setConnectTimeout(Math.min(10000, timeoutSeconds * 1000));
      connection.setReadTimeout(timeoutSeconds * 1000);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Authorization", "Bearer " + key);
      connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      byte[] payload = mapper.writeValueAsBytes(body);
      connection.setFixedLengthStreamingMode(payload.length);
      try (var output = connection.getOutputStream()) {
        output.write(payload);
      }
      int status = connection.getResponseCode();
      if (status < 200 || status >= 300) throw new BusinessException(502, "模型服务调用失败，请检查服务权限或稍后重试");
      try (var input = connection.getInputStream()) {
        return mapper.readTree(input);
      }
    } catch (SocketTimeoutException e) {
      throw new BusinessException(504, "模型服务超时，请重试");
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(502, "模型服务连接或响应格式异常，请重试");
    } finally {
      if (connection != null) connection.disconnect();
    }
  }
}
