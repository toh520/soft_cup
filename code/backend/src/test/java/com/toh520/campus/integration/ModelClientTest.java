package com.toh520.campus.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toh520.campus.common.*;
import com.toh520.campus.integration.embedding.EmbeddingClient;
import com.toh520.campus.integration.llm.ChatModelClient;
import org.junit.jupiter.api.Test;

class ModelClientTest {
  ObjectMapper mapper = new ObjectMapper();

  @Test
  void embeddingRejectsWrongDimensionsZeroAndNonNumericValues() throws Exception {
    var transport = mock(ModelTransport.class);
    var client = new EmbeddingClient(transport, "http://localhost", "demo", "private", 2, 30);
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(mapper.readTree("{\"data\":[{\"embedding\":[1]}]}"));
    assertThatThrownBy(() -> client.embed("资料"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("维度");
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(mapper.readTree("{\"data\":[{\"embedding\":[0,0]}]}"));
    assertThatThrownBy(() -> client.embed("资料")).isInstanceOf(BusinessException.class);
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(mapper.readTree("{\"data\":[{\"embedding\":[\"1\",2]}]}"));
    assertThatThrownBy(() -> client.embed("资料")).isInstanceOf(BusinessException.class);
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(mapper.readTree("{\"data\":[{\"embedding\":[1,2]}]}"));
    assertThat(client.embed("资料")).containsExactly(1, 2);
    assertThatThrownBy(() -> client.validate(new double[] {Double.NaN, 1}))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void chatAcceptsFencedJsonAndRejectsProse() throws Exception {
    var transport = mock(ModelTransport.class);
    var client =
        new ChatModelClient(
            transport, mapper, new JsonSupport(mapper), "http://localhost", "demo", "private", 30);
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(
            mapper.readTree(
                "{\"choices\":[{\"message\":{\"content\":\"```json\\n{\\\"intent\\\":\\\"clarify\\\"}\\n```\"}}]}"));
    assertThat(client.complete("assistant/intent", java.util.Map.of()).path("intent").asText())
        .isEqualTo("clarify");
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(mapper.readTree("{\"choices\":[{\"message\":{\"content\":\"不是JSON\"}}]}"));
    assertThatThrownBy(() -> client.complete("assistant/intent", java.util.Map.of()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void missingModelCredentialsFailExplicitly() {
    var transport = new ModelTransport(mapper);
    assertThatThrownBy(
            () ->
                transport.post("http://localhost", "/chat/completions", "", java.util.Map.of(), 30))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("尚未配置");
  }

  @Test
  void nutritionRetriesInvalidJsonOnceButNeverRetriesTimeoutOrAcceptsTrailingContent()
      throws Exception {
    var transport = mock(ModelTransport.class);
    var client =
        new ChatModelClient(
            transport, mapper, new JsonSupport(mapper), "http://localhost", "demo", "private", 30);
    var malformed = mapper.readTree("{\"choices\":[{\"message\":{\"content\":\"not JSON\"}}]}");
    var valid = mapper.readTree("{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}");
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(malformed, valid);
    assertThat(client.complete("nutrition/requirements", java.util.Map.of()).isObject()).isTrue();
    verify(transport, times(2)).post(anyString(), anyString(), anyString(), any(), anyInt());
    reset(transport);
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(malformed);
    assertThatThrownBy(() -> client.complete("nutrition/recommend", java.util.Map.of()))
        .hasMessageContaining("结构无效");
    verify(transport, times(2)).post(anyString(), anyString(), anyString(), any(), anyInt());
    reset(transport);
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenThrow(new BusinessException(504, "模型服务超时"));
    assertThatThrownBy(() -> client.complete("nutrition/requirements", java.util.Map.of()))
        .hasMessageContaining("超时");
    verify(transport, times(1)).post(anyString(), anyString(), anyString(), any(), anyInt());
    reset(transport);
    var trailing = mapper.readTree("{\"choices\":[{\"message\":{\"content\":\"{} {}\"}}]}");
    when(transport.post(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(trailing);
    assertThatThrownBy(() -> client.complete("assistant/intent", java.util.Map.of()))
        .hasMessageContaining("结构无效");
    verify(transport, times(1)).post(anyString(), anyString(), anyString(), any(), anyInt());
  }
}
