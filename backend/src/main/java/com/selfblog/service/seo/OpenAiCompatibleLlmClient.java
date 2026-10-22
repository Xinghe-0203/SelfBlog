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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        List<Message> effective = withJsonHint(messages);
        String payload = buildPayload(config, effective, true);
        Response response = post(config, payload);
        if (response.status() == 400) {
            // 部分 OpenAI 兼容服务（本地推理框架、旧版网关）不支持 response_format，
            // 直接 400。这类 400 与「参数本身错」无法区分，因此无条件去掉该字段重试一次；
            // 若确实还有别的参数问题，第二次仍会失败并给出安全文案。
            log.info("[SEO] 上游拒绝 response_format，去掉该字段重试一次：model={}", config.model());
            payload = buildPayload(config, messages, false);
            response = post(config, payload);
        }
        if (response.status() < 200 || response.status() >= 300) {
            throw upstreamError(response);
        }
        return extractContent(response.body());
    }

    /**
     * 带图像输入的一次补全（OpenAI 兼容 {@code image_url} + {@code data:} 内联）。
     *
     * <p>DeepSeek 只有 {@code deepseek-flash} 支持图像输入；{@code deepseek-v4-pro} 等纯文本
     * 模型会以 400/422 拒绝，由调用方翻译成「当前模型不支持图像输入」。</p>
     *
     * @param imageDataUrl 形如 {@code data:image/png;base64,...} 的完整 data URL
     */
    public String completeVision(SeoLlmConfig config, String systemPrompt, String userPrompt,
                                 String imageDataUrl) {
        String payload = buildVisionPayload(config, systemPrompt, userPrompt, imageDataUrl, true);
        Response response = post(config, payload);
        if (response.status() == 400 || response.status() == 422) {
            // 同 complete()：先假设是 response_format 不被接受，去掉重试一次。
            log.info("[SEO] 视觉请求被拒，去掉 response_format 重试一次：model={}", config.model());
            payload = buildVisionPayload(config, systemPrompt, userPrompt, imageDataUrl, false);
            response = post(config, payload);
        }
        if (response.status() < 200 || response.status() >= 300) {
            throw upstreamError(response);
        }
        return extractContent(response.body());
    }

    /**
     * 探测上游 {@code GET {baseUrl}/models}，返回可用模型清单。
     *
     * <p>上游不支持或不可达时抛 {@link LlmUpstreamException}，由调用方回落到内置候选
     * （{@code /api/admin/seo/models} 因此恒返回 200）。</p>
     */
    public List<ModelInfo> listModels(SeoLlmConfig config) {
        URI uri = URI.create(config.baseUrl() + "/models");
        Duration timeout = Duration.ofSeconds(Math.min(config.timeoutSeconds(), 20));
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + config.apiKey())
                .GET()
                .build();
        Response response;
        try {
            HttpResponse<InputStream> raw = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            String body;
            try (InputStream stream = raw.body()) {
                body = readBounded(stream);
            }
            response = new Response(raw.statusCode(), body);
        } catch (HttpTimeoutException e) {
            throw new LlmUpstreamException("模型响应超时，请稍后重试", e);
        } catch (IOException e) {
            throw new LlmUpstreamException("无法连接模型服务，请检查接口地址与网络", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmUpstreamException("模型请求被中断，请稍后重试", e);
        } catch (IllegalArgumentException e) {
            throw new LlmUpstreamException("模型接口地址无效，请检查设置", e);
        }
        if (response.status() < 200 || response.status() >= 300) {
            throw upstreamError(response);
        }
        return parseModels(response.body());
    }

    /** 解析 {@code {"data":[{"id":...,"input_modalities":[...]}]}}。 */
    List<ModelInfo> parseModels(String body) {
        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (Exception e) {
            throw new LlmUpstreamException("模型返回了无法解析的内容，请稍后重试", e);
        }
        JsonNode data = root == null ? null : root.get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            throw new LlmUpstreamException("模型服务没有返回可用的模型列表");
        }
        List<ModelInfo> models = new java.util.ArrayList<>();
        for (JsonNode item : data) {
            String id = item.path("id").asText("");
            if (id.isBlank()) {
                continue;
            }
            String name = item.path("name").asText("");
            if (name.isBlank()) {
                name = id;
            }
            boolean supportsImage = false;
            JsonNode modalities = item.get("input_modalities");
            if (modalities != null && modalities.isArray()) {
                for (JsonNode modality : modalities) {
                    if ("image".equalsIgnoreCase(modality.asText(""))) {
                        supportsImage = true;
                        break;
                    }
                }
            }
            models.add(new ModelInfo(id, name, supportsImage));
        }
        if (models.isEmpty()) {
            throw new LlmUpstreamException("模型服务没有返回可用的模型列表");
        }
        return List.copyOf(models);
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

    /**
     * 保证提示词里出现 "json" 字样（DeepSeek 的硬要求，实测行为）。
     *
     * <p><b>为什么必须提前补、而不是等 400 再重试：</b>DeepSeek 对
     * {@code response_format:{"type":"json_object"}} 有一条文档外的硬校验 —— 提示词里必须
     * 出现 "json" 这个词，否则直接 400
     * {@code Prompt must contain the word 'json' in some form to use 'response_format'
     * of type 'json_object'}。原来的实现对这个 400 的反应是「去掉 response_format 重试一次」，
     * 于是每一次调用都白白多跑一个往返，而且<b>第二次请求静默丢掉了 JSON 模式</b> —— 上游返回
     * 什么形状全凭运气，解析器的宽容提取只是恰好兜住了。</p>
     *
     * <p>所有内置提示词（SEO 与 AI 各能力）本来就写了「只输出 JSON 对象」，因此这里是
     * 纯兜底：只有调用方真的给了不含该字样的提示词（例如连通性探测的 "ping"）才追加一句，
     * 不修改已有提示词的任何内容。</p>
     */
    private List<Message> withJsonHint(List<Message> messages) {
        for (Message message : messages) {
            if (message.content() != null && message.content().toLowerCase(Locale.ROOT).contains("json")) {
                return messages;
            }
        }
        if (messages.isEmpty()) {
            return messages;
        }
        List<Message> patched = new ArrayList<>(messages);
        int last = patched.size() - 1;
        Message tail = patched.get(last);
        patched.set(last, new Message(tail.role(), tail.content() + "\n\n请以 JSON 对象的形式输出结果。"));
        return List.copyOf(patched);
    }

    /** 构造请求体；{@code withResponseFormat=false} 时省略该字段。 */
    String buildPayload(SeoLlmConfig config, List<Message> messages, boolean withResponseFormat) {
        ObjectNode root = json.createObjectNode();
        String model = config.model();
        if (model != null && !model.isBlank()) {
            root.put("model", model);
        }
        ArrayNode array = root.putArray("messages");
        for (Message message : messages) {
            ObjectNode item = array.addObject();
            item.put("role", message.role());
            item.put("content", message.content());
        }
        applySampling(root, config);
        if (withResponseFormat) {
            root.putObject("response_format").put("type", "json_object");
        }
        return root.toString();
    }

    /** 构造视觉请求体：user 消息内容是多模态数组（文本 + image_url data URL）。 */
    String buildVisionPayload(SeoLlmConfig config, String systemPrompt, String userPrompt,
                              String imageDataUrl, boolean withResponseFormat) {
        ObjectNode root = json.createObjectNode();
        String model = config.model();
        if (model != null && !model.isBlank()) {
            root.put("model", model);
        }
        ArrayNode messages = root.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", systemPrompt);
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        ArrayNode parts = user.putArray("content");
        ObjectNode textPart = parts.addObject();
        textPart.put("type", "text");
        textPart.put("text", userPrompt == null ? "" : userPrompt);
        ObjectNode imagePart = parts.addObject();
        imagePart.put("type", "image_url");
        imagePart.putObject("image_url").put("url", imageDataUrl);
        applySampling(root, config);
        if (withResponseFormat) {
            root.putObject("response_format").put("type", "json_object");
        }
        return root.toString();
    }

    /**
     * 采样参数：思考模式与普通模式的字段集不同。
     *
     * <p>DeepSeek 思考模式下 {@code temperature} 被静默忽略（不报错也不生效），发它只会让人
     * 误以为设置了温度；因此思考模式发 {@code thinking} + {@code reasoning_effort} 而<b>不发</b>
     * temperature。自定义 OpenAI 兼容端点不认 thinking，一律按普通模式发 temperature。</p>
     */
    private static void applySampling(ObjectNode root, SeoLlmConfig config) {
        if (config.sendsThinking()) {
            root.putObject("thinking").put("type", "enabled");
            root.put("reasoning_effort", config.effectiveReasoningEffort());
        } else {
            root.put("temperature", 0.4);
        }
        if (config.sendsMaxTokens()) {
            root.put("max_tokens", config.maxTokens());
        }
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
        } else if (status == 402) {
            // DeepSeek 用它表示「余额不足」。走通用文案会让管理员去查 Key 和模型名，
            // 而真正要做的只是充值。
            message = "账户余额不足，请前往模型平台充值后重试";
        } else if (status == 422) {
            // 参数错误：常见于模型不支持该字段（如纯文本模型收到图像输入）。
            message = "模型拒绝了这些参数，请检查模型名与设置";
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

    /** 上游 {@code /models} 里的一项。{@code supportsImage} 由 {@code input_modalities} 含 image 推出。 */
    public record ModelInfo(String id, String name, boolean supportsImage) {
    }

    private record Response(int status, String body) {
    }
}
