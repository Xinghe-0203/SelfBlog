package com.selfblog.service.seo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

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

    // ------------------------------------------------------------------
    // DeepSeek 服务商差异（A 组）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("complete_PromptWithoutJsonWord_AppendsJsonHintBeforeSending")
    void complete_PromptWithoutJsonWord_AppendsJsonHintBeforeSending() throws Exception {
        // 实测：DeepSeek 对 response_format=json_object 有文档外的硬校验 —— 提示词必须含
        // "json" 字样，否则 400。若等 400 再重试，每次调用都白跑一个往返、且第二次静默丢掉
        // JSON 模式。所以客户端必须提前补一句，并且只补一次请求。
        List<OpenAiCompatibleLlmClient.Message> noJson = List.of(
                new OpenAiCompatibleLlmClient.Message("system", "你是助手"),
                new OpenAiCompatibleLlmClient.Message("user", "请介绍一下这篇文章"));

        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            client.complete(config(server.baseUrl(), 10), noJson);

            assertEquals(1, server.callCount(), "补了提示词就不该再靠 400 重试兜底");
            JsonNode payload = json.readTree(server.bodies().get(0));
            String userPrompt = payload.path("messages").path(1).path("content").asText();
            assertTrue(userPrompt.toLowerCase(Locale.ROOT).contains("json"),
                    "发出去的提示词必须含 json 字样，否则 DeepSeek 直接 400");
            assertTrue(userPrompt.startsWith("请介绍一下这篇文章"),
                    "只能在末尾追加，不能改写调用方的提示词");
            assertEquals("json_object", payload.path("response_format").path("type").asText());
        }
    }

    @Test
    @DisplayName("complete_PromptAlreadyMentionsJson_IsLeftByteForByteUnchanged")
    void complete_PromptAlreadyMentionsJson_IsLeftByteForByteUnchanged() throws Exception {
        // 所有内置提示词本来就写了「只输出 JSON 对象」，这里必须原样透传：
        // 多追加一句会改变提示词语义，也会让已有提示词的测试断言漂移。
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            client.complete(config(server.baseUrl(), 10), messages());

            JsonNode payload = json.readTree(server.bodies().get(0));
            assertEquals("只输出 JSON", payload.path("messages").path(0).path("content").asText());
            assertEquals("为文章写 SEO", payload.path("messages").path(1).path("content").asText());
            assertEquals(1, server.callCount());
        }
    }

    @Test
    @DisplayName("probe_SendsPromptThatSatisfiesJsonModeRequirement")
    void probe_SendsPromptThatSatisfiesJsonModeRequirement() throws Exception {
        // probe 是唯一原本不含 json 字样的调用方（"ping"）：它过去每次都会先吃一个 400。
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("OK"))) {
            assertTrue(client.probe(config(server.baseUrl(), 10)).ok());

            assertEquals(1, server.callCount(), "连通性测试不该先失败一次再成功");
            String body = server.bodies().get(0);
            assertTrue(body.toLowerCase(Locale.ROOT).contains("json"));
        }
    }

    private static SeoLlmConfig deepSeek(String baseUrl, boolean thinking, String effort, int maxTokens) {
        return new SeoLlmConfig(true, false, baseUrl, API_KEY, "deepseek-flash", 10,
                SeoLlmConfig.PROVIDER_DEEPSEEK, thinking, effort, maxTokens);
    }

    @Test
    @DisplayName("complete_ThinkingMode_SendsThinkingAndEffortButNoTemperature")
    void complete_ThinkingMode_SendsThinkingAndEffortButNoTemperature() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            client.complete(deepSeek(server.baseUrl(), true, "high", 512), messages());

            JsonNode payload = json.readTree(server.bodies().get(0));
            assertEquals("enabled", payload.path("thinking").path("type").asText());
            assertEquals("high", payload.path("reasoning_effort").asText());
            assertEquals(512, payload.path("max_tokens").asInt());
            assertFalse(payload.has("temperature"),
                    "思考模式下 temperature 被 DeepSeek 静默忽略，发出去只会让人误以为设置了温度");
        }
    }

    @Test
    @DisplayName("complete_ThinkingDisabled_SendsTemperatureAndNoThinkingFields")
    void complete_ThinkingDisabled_SendsTemperatureAndNoThinkingFields() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            client.complete(deepSeek(server.baseUrl(), false, "low", 0), messages());

            JsonNode payload = json.readTree(server.bodies().get(0));
            assertEquals(0.4, payload.path("temperature").asDouble(), 0.0001);
            assertFalse(payload.has("thinking"));
            assertFalse(payload.has("reasoning_effort"));
            assertFalse(payload.has("max_tokens"), "maxTokens=0 表示沿用上游默认，不该发该字段");
        }
    }

    @Test
    @DisplayName("complete_CustomProvider_IgnoresThinkingEvenWhenEnabled")
    void complete_CustomProvider_IgnoresThinkingEvenWhenEnabled() throws Exception {
        // 自定义端点不认 thinking/reasoning_effort：发了会被当成未知参数（部分网关直接 400）。
        SeoLlmConfig custom = new SeoLlmConfig(true, false, "", API_KEY, "gpt-4o-mini", 10,
                SeoLlmConfig.PROVIDER_CUSTOM, true, "high", 0);
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            client.complete(new SeoLlmConfig(custom.enabled(), custom.autoOnPublish(), server.baseUrl(),
                    custom.apiKey(), custom.model(), custom.timeoutSeconds(), custom.provider(),
                    custom.thinking(), custom.reasoningEffort(), custom.maxTokens()), messages());

            JsonNode payload = json.readTree(server.bodies().get(0));
            assertFalse(payload.has("thinking"), "provider=custom 时绝不能发 thinking");
            assertEquals(0.4, payload.path("temperature").asDouble(), 0.0001);
        }
    }

    @Test
    @DisplayName("complete_PaymentRequired_ThrowsBalanceMessage")
    void complete_PaymentRequired_ThrowsBalanceMessage() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) ->
                StubLlmServer.Reply.status(402, "{\"error\":\"insufficient balance\"}"))) {

            LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                    () -> client.complete(config(server.baseUrl(), 10), messages()));

            assertEquals("账户余额不足，请前往模型平台充值后重试", error.getMessage());
        }
    }

    @Test
    @DisplayName("complete_UnprocessableEntity_ThrowsParameterMessage")
    void complete_UnprocessableEntity_ThrowsParameterMessage() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) ->
                StubLlmServer.Reply.status(422, "{\"error\":\"invalid parameter\"}"))) {

            LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                    () -> client.complete(config(server.baseUrl(), 10), messages()));

            assertEquals("模型拒绝了这些参数，请检查模型名与设置", error.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 图像输入（E 组）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("completeVision_SendsMultipartContentWithDataUrl")
    void completeVision_SendsMultipartContentWithDataUrl() throws Exception {
        String dataUrl = "data:image/png;base64,iVBORw0KGgo=";
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            client.completeVision(deepSeek(server.baseUrl(), false, "low", 0),
                    "你是图片描述助手", "描述这张图", dataUrl);

            JsonNode payload = json.readTree(server.bodies().get(0));
            JsonNode user = payload.path("messages").get(1);
            assertEquals("user", user.path("role").asText());
            assertEquals(2, user.path("content").size());
            assertEquals("text", user.path("content").get(0).path("type").asText());
            assertEquals("描述这张图", user.path("content").get(0).path("text").asText());
            assertEquals("image_url", user.path("content").get(1).path("type").asText());
            assertEquals(dataUrl, user.path("content").get(1).path("image_url").path("url").asText());
        }
    }

    @Test
    @DisplayName("completeVision_RejectedWith422_RetriesWithoutResponseFormat")
    void completeVision_RejectedWith422_RetriesWithoutResponseFormat() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) ->
                index == 0
                        ? StubLlmServer.Reply.status(422, "{\"error\":\"response_format unsupported\"}")
                        : StubLlmServer.Reply.ok("{}"))) {

            client.completeVision(deepSeek(server.baseUrl(), false, "low", 0),
                    "system", "user", "data:image/png;base64,AAA=");

            assertEquals(2, server.callCount());
            assertTrue(server.bodies().get(0).contains("response_format"));
            assertFalse(server.bodies().get(1).contains("response_format"));
        }
    }

    // ------------------------------------------------------------------
    // 模型列表（A 组的「探测可用模型」）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("listModels_ParsesIdsAndDerivesImageSupportFromInputModalities")
    void listModels_ParsesIdsAndDerivesImageSupportFromInputModalities() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            server.serveModels("{\"object\":\"list\",\"data\":["
                    + "{\"id\":\"deepseek-flash\",\"name\":\"DeepSeek-V4.1-Flash\","
                    + "\"input_modalities\":[\"text\",\"image\"]},"
                    + "{\"id\":\"deepseek-v4-pro\",\"name\":\"DeepSeek-V4-Pro\","
                    + "\"input_modalities\":[\"text\"]}]}");

            List<OpenAiCompatibleLlmClient.ModelInfo> models =
                    client.listModels(config(server.baseUrl(), 10));

            assertEquals(2, models.size());
            assertEquals("deepseek-flash", models.get(0).id());
            assertEquals("DeepSeek-V4.1-Flash", models.get(0).name());
            assertTrue(models.get(0).supportsImage());
            assertEquals("deepseek-v4-pro", models.get(1).id());
            assertFalse(models.get(1).supportsImage(),
                    "纯文本模型必须被识别为不支持图像：它不会报错，只会静默忽略图片");
            assertEquals("Bearer " + API_KEY, server.modelAuthorizations().get(0));
        }
    }

    @Test
    @DisplayName("listModels_MissingName_FallsBackToId")
    void listModels_MissingName_FallsBackToId() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            server.serveModels("{\"data\":[{\"id\":\"my-model\"}]}");

            List<OpenAiCompatibleLlmClient.ModelInfo> models =
                    client.listModels(config(server.baseUrl(), 10));

            assertEquals("my-model", models.get(0).name());
            assertFalse(models.get(0).supportsImage());
        }
    }

    @Test
    @DisplayName("listModels_Unauthorized_ThrowsSafeMessage")
    void listModels_Unauthorized_ThrowsSafeMessage() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            server.rejectModels(401, "{\"error\":\"invalid api key " + API_KEY + "\"}");

            LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                    () -> client.listModels(config(server.baseUrl(), 10)));

            assertEquals("模型鉴权失败，请检查 API Key 是否有效", error.getMessage());
            assertFalse(error.getMessage().contains(API_KEY));
        }
    }

    @Test
    @DisplayName("listModels_EmptyData_ThrowsNoModelsMessage")
    void listModels_EmptyData_ThrowsNoModelsMessage() throws Exception {
        try (StubLlmServer server = new StubLlmServer((index, body) -> StubLlmServer.Reply.ok("{}"))) {
            server.serveModels("{\"data\":[]}");

            LlmUpstreamException error = assertThrows(LlmUpstreamException.class,
                    () -> client.listModels(config(server.baseUrl(), 10)));

            assertEquals("模型服务没有返回可用的模型列表", error.getMessage());
        }
    }
}
