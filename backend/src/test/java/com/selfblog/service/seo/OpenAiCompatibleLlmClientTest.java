package com.selfblog.service.seo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OpenAiCompatibleLlmClient} 的真实 HTTP 语义测试。
 *
 * <p>全部走 {@link StubLlmServer}（本地真实 HTTP 服务），不发任何外网请求。</p>
 */
@DisplayName("OpenAiCompatibleLlmClient")
class OpenAiCompatibleLlmClientTest {

    private final ObjectMapper json = new ObjectMapper();
    private final OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(json);
    private static final String API_KEY = "sk-test-secret-value-1234567890";

    private static SeoLlmConfig config(String baseUrl, int timeoutSeconds) {
        return new SeoLlmConfig(true, false, baseUrl, API_KEY, "gpt-4o-mini", timeoutSeconds);
    }

    private static List<OpenAiCompatibleLlmClient.Message> messages() {
        return List.of(new OpenAiCompatibleLlmClient.Message("system", "只输出 JSON"),
                new OpenAiCompatibleLlmClient.Message("user", "为文章写 SEO"));
    }

    @Test
    @DisplayName("complete_HappyPath_SendsBearerAndReturnsContent")
    void complete_HappyPath_SendsBearerAndReturnsContent() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{\"title\":\"t\"}"))) {
            String content = client.complete(config(server.baseUrl(), 10), messages());

            assertEquals("{\"title\":\"t\"}", content);
            assertEquals(1, server.callCount());
            assertEquals("Bearer " + API_KEY, server.authorizations().get(0));

            JsonNode payload = json.readTree(server.bodies().get(0));
            assertEquals("gpt-4o-mini", payload.path("model").asText());
            assertEquals(2, payload.path("messages").size());
            assertEquals("system", payload.path("messages").get(0).path("role").asText());
            assertEquals(0.4, payload.path("temperature").asDouble(), 0.0001);
            assertEquals("json_object", payload.path("response_format").path("type").asText());
        }
    }

    @Test
    @DisplayName("complete_ResponseFormatRejectedWith400_RetriesWithoutIt")
    void complete_ResponseFormatRejectedWith400_RetriesWithoutIt() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) ->
                index == 0
                        ? StubLlmServer.Reply.status(400, "{\"error\":\"response_format is not supported\"}")
                        : StubLlmServer.Reply.ok("{\"title\":\"重试成功\"}"))) {

            String content = client.complete(config(server.baseUrl(), 10), messages());

            assertEquals("{\"title\":\"重试成功\"}", content);
            assertEquals(2, server.callCount());
            assertTrue(server.bodies().get(0).contains("response_format"));
            assertFalse(server.bodies().get(1).contains("response_format"),
                    "第二次请求必须去掉 response_format，否则同样的 400 会再来一次");
            assertEquals("Bearer " + API_KEY, server.authorizations().get(1));
        }
    }

    @Test
    @DisplayName("complete_Unauthorized_ThrowsSafeMessageWithoutKey")
    void complete_Unauthorized_ThrowsSafeMessageWithoutKey() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) ->
                StubLlmServer.Reply.status(401, "{\"error\":\"invalid api key " + API_KEY + "\"}"))) {

            LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                    () -> client.complete(config(server.baseUrl(), 10), messages()));

            assertEquals("模型鉴权失败，请检查 API Key 是否有效", error.getMessage());
            assertFalse(error.getMessage().contains(API_KEY), "异常消息绝不能回显密钥");
            assertFalse(error.getMessage().contains("invalid api key"), "异常消息不能带上游原文");
        }
    }

    @Test
    @DisplayName("complete_Timeout_ThrowsGenericTimeoutMessage")
    void complete_Timeout_ThrowsGenericTimeoutMessage() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) ->
                StubLlmServer.Reply.slow(4000, "{\"title\":\"too late\"}"))) {

            LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                    () -> client.complete(config(server.baseUrl(), 1), messages()));

            assertEquals("模型响应超时，请稍后重试", error.getMessage());
        }
    }

    @Test
    @DisplayName("complete_ServerError_ThrowsSafeMessage")
    void complete_ServerError_ThrowsSafeMessage() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) ->
                StubLlmServer.Reply.status(503, "upstream exploded: stack trace here"))) {

            LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                    () -> client.complete(config(server.baseUrl(), 10), messages()));

            assertEquals("模型服务暂时不可用，请稍后重试", error.getMessage());
            assertFalse(error.getMessage().contains("stack trace"));
        }
    }

    @Test
    @DisplayName("complete_NonJsonResponse_ThrowsParseMessage")
    void complete_NonJsonResponse_ThrowsParseMessage() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) ->
                StubLlmServer.Reply.status(200, "<html>not json</html>"))) {

            LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                    () -> client.complete(config(server.baseUrl(), 10), messages()));

            assertEquals("模型返回了无法解析的内容，请稍后重试", error.getMessage());
        }
    }

    @Test
    @DisplayName("complete_MissingChoices_ThrowsStructureMessage")
    void complete_MissingChoices_ThrowsStructureMessage() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) ->
                StubLlmServer.Reply.status(200, "{\"id\":\"x\"}"))) {

            LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                    () -> client.complete(config(server.baseUrl(), 10), messages()));

            assertEquals("模型返回结构异常，请稍后重试", error.getMessage());
        }
    }

    @Test
    @DisplayName("probe_ReturnsOkAndLatency")
    void probe_ReturnsOkAndLatency() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("OK"))) {
            OpenAiCompatibleLlmClient.ProbeResult result = client.probe(config(server.baseUrl(), 10));

            assertTrue(result.ok());
            assertTrue(result.latencyMs() >= 0);
            assertNotNull(server.bodies().get(0));
        }
    }

    @Test
    @DisplayName("complete_UnreachableHost_ThrowsSafeNetworkMessage")
    void complete_UnreachableHost_ThrowsSafeNetworkMessage() {
        // 端口 1 在本机必然拒绝连接：验证网络异常被折叠成安全文案而不是原始 IOException
        SeoLlmConfig unreachable = new SeoLlmConfig(true, false, "http://127.0.0.1:1/v1", API_KEY, "m", 2);

        LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                () -> client.complete(unreachable, messages()));

        assertEquals("无法连接模型服务，请检查接口地址与网络", error.getMessage());
    }
}
