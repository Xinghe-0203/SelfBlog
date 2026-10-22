package com.selfblog.service.seo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI 兼容 {@code POST {baseUrl}/chat/completions} 客户端。
 *
 * <p><b>为什么手写 JDK HttpClient 而不加依赖：</b>本项目 pom 里没有任何 LLM/HTTP-client
 * 依赖，新增一个会连带影响 Docker 镜像、CI 与依赖审计；JDK 自带的 {@code java.net.http}
 * 足够覆盖「POST 一段 JSON、读回一段 JSON」这个场景。同理不用 Spring
 * {@code RestClient}/{@code RestTemplate}（仓库里本来就没有，保持一致性）。</p>
 *
 * <p><b>安全红线：</b>API Key 只出现在请求头里，永不进日志、永不进异常消息。
 * 上游响应体只截取有限长度用于日志排错，且回给调用方的异常消息是固定安全文案。</p>
 */
@Slf4j
@Component
public class OpenAiCompatibleLlmClient {

    /** 上游响应体的最大读取字节数（防御超大/恶意响应）。 */
    static final int MAX_RESPONSE_BYTES = 1_000_000;
    /** 日志里保留的上游片段长度。 */
    private static final int LOG_EXCERPT = 300;

    private final ObjectMapper json;

    public OpenAiCompatibleLlmClient(ObjectMapper json) {
        this.json = json;
    }

    /**
     * 一次对话补全，返回模型的文本内容。
     *
     * @param config    端点/密钥/模型/超时
     * @param messages  {@code [{role, content}]} 消息列表
     * @throws LlmUpstreamException 任何网络、鉴权、限流、上游错误或响应结构异常
     */
    public String complete(SeoLlmConfig config, List<Message> messages) {
        String payload = buildPayload(config.model(), messages, true);
        Response response = post(config, payload);
        if (response.status() == 400) {
            // 部分 OpenAI 兼容服务（本地推理框架、旧版网关）不支持 response_format，
            // 直接 400。这类 400 与「参数本身错」无法区分，因此无条件去掉该字段重试一次；
            // 若确实还有别的参数问题，第二次仍会失败并给出安全文案。
            log.info("[SEO] 上游拒绝 response_format，去掉该字段重试一次：model={}", config.model());
            payload = buildPayload(config.model(), messages, false);
            response = post(config, payload);
        }
        if (response.status() < 200 || response.status() >= 300) {
            throw upstreamError(response);
        }
        return extractContent(response.body());
    }

    /** 连通性测试：发一条最小消息，返回耗时与模型名。 */
    public ProbeResult probe(SeoLlmConfig config) {
        long started = System.nanoTime();
        String content = complete(config, List.of(
                new Message("system", "你是连通性测试助手，请只回复 OK。"),
                new Message("user", "ping")));
        long latencyMs = (System.nanoTime() - started) / 1_000_000L;
        return new ProbeResult(content != null && !content.isBlank(), latencyMs);
    }

    private Response post(SeoLlmConfig config, String payload) {
        URI uri = URI.create(config.baseUrl() + "/chat/completions");
        Duration timeout = Duration.ofSeconds(config.timeoutSeconds());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + config.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<InputStream> raw = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            String body;
            try (InputStream stream = raw.body()) {
                body = readBounded(stream);
            }
            return new Response(raw.statusCode(), body);
        } catch (HttpTimeoutException e) {
            log.warn("[SEO] 模型请求超时：endpoint={}, timeout={}s", config.safeBaseUrl(), config.timeoutSeconds());
            throw new LlmUpstreamException("模型响应超时，请稍后重试", e);
        } catch (IOException e) {
            log.warn("[SEO] 模型请求网络失败：endpoint={}, error={}", config.safeBaseUrl(), e.getClass().getSimpleName());
            throw new LlmUpstreamException("无法连接模型服务，请检查接口地址与网络", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmUpstreamException("模型请求被中断，请稍后重试", e);
        } catch (IllegalArgumentException e) {
            // URI.create 失败：地址不是合法 URL。消息里不能回显地址（可能带凭据）。
            log.warn("[SEO] 模型接口地址非法：{}", e.getClass().getSimpleName());
            throw new LlmUpstreamException("模型接口地址无效，请检查设置", e);
        }
    }

    /** 构造请求体；{@code withResponseFormat=false} 时省略该字段。 */
    String buildPayload(String model, List<Message> messages, boolean withResponseFormat) {
        ObjectNode root = json.createObjectNode();
        if (model != null && !model.isBlank()) {
            root.put("model", model);
        }
        ArrayNode array = root.putArray("messages");
        for (Message message : messages) {
            ObjectNode item = array.addObject();
            item.put("role", message.role());
            item.put("content", message.content());
        }
        root.put("temperature", 0.4);
        if (withResponseFormat) {
            root.putObject("response_format").put("type", "json_object");
        }
        return root.toString();
    }

    /** 从 OpenAI 兼容响应里取 {@code choices[0].message.content}。 */
    String extractContent(String body) {
        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (Exception e) {
            log.warn("[SEO] 模型响应不是合法 JSON：{}", excerpt(body));
            throw new LlmUpstreamException("模型返回了无法解析的内容，请稍后重试", e);
        }
        JsonNode choices = root == null ? null : root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            log.warn("[SEO] 模型响应缺少 choices：{}", excerpt(body));
            throw new LlmUpstreamException("模型返回结构异常，请稍后重试");
        }
        JsonNode content = choices.get(0).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            log.warn("[SEO] 模型响应缺少 content：{}", excerpt(body));
            throw new LlmUpstreamException("模型没有返回内容，请稍后重试");
        }
        return content.asText();
    }

    /** 按状态码给出安全文案；上游响应体只写日志且截断。 */
    private LlmUpstreamException upstreamError(Response response) {
        int status = response.status();
        log.warn("[SEO] 模型上游返回 {}：{}", status, excerpt(response.body()));
        String message;
        if (status == 401 || status == 403) {
            message = "模型鉴权失败，请检查 API Key 是否有效";
        } else if (status == 429) {
            message = "模型请求过于频繁或额度不足，请稍后重试";
        } else if (status == 404) {
            message = "模型接口不存在，请检查接口地址与模型名";
        } else if (status >= 500) {
            message = "模型服务暂时不可用，请稍后重试";
        } else {
            message = "模型请求失败（HTTP " + status + "），请检查模型与接口设置";
        }
        return new LlmUpstreamException(message);
    }

    /** 有界读取：超过 {@link #MAX_RESPONSE_BYTES} 直接截断，避免内存被打满。 */
    private static String readBounded(InputStream stream) throws IOException {
        byte[] buffer = new byte[8192];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int total = 0;
        int read;
        while ((read = stream.read(buffer)) != -1) {
            int keep = Math.min(read, MAX_RESPONSE_BYTES - total);
            if (keep > 0) {
                out.write(buffer, 0, keep);
                total += keep;
            }
            if (total >= MAX_RESPONSE_BYTES) {
                break;
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String excerpt(String body) {
        if (body == null) return "";
        String value = body.replaceAll("\\s+", " ").trim();
        return value.length() <= LOG_EXCERPT ? value : value.substring(0, LOG_EXCERPT) + "…";
    }

    /** 一条对话消息。 */
    public record Message(String role, String content) {
    }

    /** 连通性测试结果。 */
    public record ProbeResult(boolean ok, long latencyMs) {
    }

    private record Response(int status, String body) {
    }
}
