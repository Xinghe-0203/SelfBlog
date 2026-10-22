package com.selfblog.service.seo;

import com.selfblog.service.site.SiteSettingsService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 从站点设置里组装 {@link SeoLlmConfig}。
 *
 * <p>刻意不做本地缓存：{@code SiteSettingsService.getSettings()} 本身带
 * {@code siteSettingsCache}（10 分钟）且写入时会整体驱逐，所以这里每次读到的都是最新快照，
 * 同时不会给每次发布/每次生成多加一次查库。</p>
 */
@Component
public class SeoLlmConfigProvider {

    /** 未设置超时时的默认值（秒）。 */
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int MIN_TIMEOUT_SECONDS = 5;
    private static final int MAX_TIMEOUT_SECONDS = 120;

    /** 服务商标识：DeepSeek 官方端点，字段集与通用 OpenAI 兼容端点不同。 */
    public static final String PROVIDER_DEEPSEEK = "deepseek";
    /** 服务商标识：任意 OpenAI 兼容端点（含自建网关）。 */
    public static final String PROVIDER_CUSTOM = "custom";

    private static final int MIN_MAX_TOKENS = 1;
    private static final int MAX_MAX_TOKENS = 131_072;

    private final SiteSettingsService settings;

    public SeoLlmConfigProvider(SiteSettingsService settings) {
        this.settings = settings;
    }

    public SeoLlmConfig current() {
        Map<String, String> values = settings.getSettings();
        return new SeoLlmConfig(
                "true".equalsIgnoreCase(values.getOrDefault("llmEnabled", "false")),
                "true".equalsIgnoreCase(values.getOrDefault("llmAutoOnPublish", "false")),
                normalizeBaseUrl(values.get("llmBaseUrl")),
                trimToEmpty(values.get("llmApiKey")),
                trimToEmpty(values.get("llmModel")),
                parseTimeout(values.get("llmTimeoutSeconds")),
                normalizeProvider(values.get("llmProvider")),
                isOn(values.get("llmThinking")),
                normalizeEffort(values.get("llmReasoningEffort")),
                parseMaxTokens(values.get("llmMaxTokens")));
    }

    /** 思考模式开关：契约值 {@code enabled|disabled}，同时容忍布尔写法。 */
    private static boolean isOn(String raw) {
        String value = trimToEmpty(raw);
        return "enabled".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    /**
     * 访客评论自动审核的总开关：既要打开 {@code llmCommentModeration}，也要 LLM 真的可用。
     *
     * <p>「能不能用」只在这里判一次，评论写入路径与后台开关都调本方法，避免两处各写一遍
     * 而漂移。失败方向是<b>关</b>：配置不全时不审，评论照常发布。</p>
     */
    public boolean commentModerationEnabled() {
        Map<String, String> values = settings.getSettings();
        boolean on = "true".equalsIgnoreCase(values.getOrDefault("llmCommentModeration", "false"));
        return on && current().configured();
    }

    /** 服务商标识：{@code deepseek} 或 {@code custom}；非法值回落 {@code custom}（= 通用 OpenAI 兼容）。 */
    public static String normalizeProvider(String raw) {
        String value = trimToEmpty(raw).toLowerCase(java.util.Locale.ROOT);
        return PROVIDER_DEEPSEEK.equals(value) ? PROVIDER_DEEPSEEK : PROVIDER_CUSTOM;
    }

    /** 思考强度：{@code low|high|max}；非法值回落 {@code low}（SEO 类结构化任务不需要高投入）。 */
    public static String normalizeEffort(String raw) {
        String value = trimToEmpty(raw).toLowerCase(java.util.Locale.ROOT);
        return switch (value) {
            case "high" -> "high";
            case "max" -> "max";
            default -> "low";
        };
    }

    /** 最大输出 tokens：未设置返回 0（= 不发送该字段）；夹紧到 1..131072。 */
    private static int parseMaxTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                return 0;
            }
            return Math.min(MAX_MAX_TOKENS, value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 归一化端点：去空白、去尾部斜杠，并去掉误粘的 {@code /chat/completions}
     * （否则真正请求会变成 {@code .../chat/completions/chat/completions}）。
     * 保存时已归一化，这里再兜一次，兼容历史/手工写入的值。
     */
    public static String normalizeBaseUrl(String raw) {
        String value = trimToEmpty(raw);
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith("/chat/completions")) {
            value = value.substring(0, value.length() - "/chat/completions".length());
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static int parseTimeout(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return Math.max(MIN_TIMEOUT_SECONDS, Math.min(MAX_TIMEOUT_SECONDS, value));
        } catch (NumberFormatException e) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
