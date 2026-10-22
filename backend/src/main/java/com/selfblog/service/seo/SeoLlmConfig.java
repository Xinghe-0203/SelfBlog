package com.selfblog.service.seo;

/**
 * 一次 LLM 调用所需的全部参数快照。
 *
 * <p><b>每次调用都重新读取</b>（由 {@link SeoLlmConfigProvider#current()} 从 {@code site_setting}
 * 组装）：管理员改完 Key 立刻生效，不需要重启，也不需要把密钥放进 {@code @ConfigurationProperties}
 * （那会把密钥固化进环境变量/配置文件，和「后台可改」的目标相反）。</p>
 *
 * @param enabled        总开关，当前用于「自动生成」闸门
 * @param autoOnPublish  发布时是否自动生成
 * @param baseUrl        形如 {@code https://api.openai.com/v1}，已去掉尾部斜杠与误粘的 /chat/completions
 * @param apiKey         Bearer 密钥；只存在于内存与本对象，绝不写日志
 * @param model          模型名
 * @param timeoutSeconds 单次请求超时（5-120 秒）
 */
public record SeoLlmConfig(boolean enabled, boolean autoOnPublish, String baseUrl, String apiKey,
                           String model, int timeoutSeconds) {

    /** 手动生成只需「地址 + 密钥」齐全；总开关只管自动生成。 */
    public boolean configured() {
        return baseUrl != null && !baseUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    /** 自动生成三个条件同时成立才触发。 */
    public boolean autoEnabled() {
        return enabled && autoOnPublish && configured();
    }

    /** 日志/响应里可以安全展示的端点（不含任何凭据）。 */
    public String safeBaseUrl() {
        return baseUrl == null ? "" : baseUrl;
    }
}
