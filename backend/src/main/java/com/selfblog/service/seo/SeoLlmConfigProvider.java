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
                parseTimeout(values.get("llmTimeoutSeconds")));
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
