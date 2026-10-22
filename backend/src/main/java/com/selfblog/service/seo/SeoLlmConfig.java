package com.selfblog.service.seo;

/**
 * 一次 LLM 调用所需的全部参数快照。
 *
 * <p><b>每次调用都重新读取</b>（由 {@link SeoLlmConfigProvider#current()} 从 {@code site_setting}
 * 组装）：管理员改完 Key 立刻生效，不需要重启，也不需要把密钥放进 {@code @ConfigurationProperties}
 * （那会把密钥固化进环境变量/配置文件，和「后台可改」的目标相反）。</p>
 *
 * <p><b>服务商差异：</b>DeepSeek 的思考模式会<b>静默忽略</b> {@code temperature}（不报错也不生效），
 * 并且默认开启、effort 默认 high。对 SEO / 摘要 / 标签这类结构化任务，思考模式只是白烧输出
 * token 与延迟，所以默认 {@code thinking=false}；开启时改发 {@code thinking:{type:enabled}} +
 * {@code reasoning_effort}，<b>不发 temperature</b>。自定义 OpenAI 兼容端点没有这些字段，
 * 一律按普通模式发 temperature。</p>
 *
 * @param enabled         总开关，当前用于「自动生成」闸门
 * @param autoOnPublish   发布时是否自动生成
 * @param baseUrl         形如 {@code https://api.deepseek.com}，已去掉尾部斜杠与误粘的 /chat/completions
 * @param apiKey          Bearer 密钥；只存在于内存与本对象，绝不写日志
 * @param model           模型名
 * @param timeoutSeconds  单次请求超时（5-120 秒）
 * @param provider        {@code deepseek} 或 {@code custom}；只有 deepseek 才发思考模式字段
 * @param thinking        是否启用思考模式（仅 provider=deepseek 生效）
 * @param reasoningEffort 思考强度 {@code low|high|max}，仅思考模式开启时发送
 * @param maxTokens       输出上限；{@code 0} 表示不发该字段（沿用上游默认）
 */
public record SeoLlmConfig(boolean enabled, boolean autoOnPublish, String baseUrl, String apiKey,
                           String model, int timeoutSeconds,
                           String provider, boolean thinking, String reasoningEffort, int maxTokens) {

    /** DeepSeek 服务商标识。 */
    public static final String PROVIDER_DEEPSEEK = "deepseek";
    /** 自定义 OpenAI 兼容服务商标识。 */
    public static final String PROVIDER_CUSTOM = "custom";
    /** 思考强度合法值。 */
    public static final java.util.List<String> REASONING_EFFORTS = java.util.List.of("low", "high", "max");
    /** 输出上限硬上限（DeepSeek 当前最大 384K，这里取一个保守上限防误填）。 */
    public static final int MAX_TOKENS_LIMIT = 131_072;

    /**
     * 兼容构造：旧调用方（既有测试、只关心 6 个基础字段）按自定义服务商、非思考模式组装。
     *
     * <p>保留它是为了不把「新增服务商维度」这件事扩散成一次全仓构造点改写 —— 语义上等价于
     * 「一个普通的 OpenAI 兼容端点，按普通模式调用」。</p>
     */
    public SeoLlmConfig(boolean enabled, boolean autoOnPublish, String baseUrl, String apiKey,
                        String model, int timeoutSeconds) {
        this(enabled, autoOnPublish, baseUrl, apiKey, model, timeoutSeconds,
                PROVIDER_CUSTOM, false, "low", 0);
    }

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

    /** 是否是 DeepSeek 官方端点（只有它认 thinking / reasoning_effort）。 */
    public boolean isDeepSeek() {
        return provider != null && PROVIDER_DEEPSEEK.equalsIgnoreCase(provider.trim());
    }

    /** 是否要发送思考模式字段：DeepSeek 且用户显式开启。 */
    public boolean sendsThinking() {
        return isDeepSeek() && thinking;
    }

    /**
     * 是否要发送 temperature。
     *
     * <p>DeepSeek 思考模式下该字段被静默忽略，发出去只会让「我以为设了温度」变成误解，
     * 因此思考模式一律不发。</p>
     */
    public boolean sendsTemperature() {
        return !sendsThinking();
    }

    /** 是否要发送 max_tokens：只有显式配置了正整数才发。 */
    public boolean sendsMaxTokens() {
        return maxTokens > 0;
    }

    /** 归一化后的思考强度（非法值回落 low）。 */
    public String effectiveReasoningEffort() {
        return reasoningEffort != null && REASONING_EFFORTS.contains(reasoningEffort) ? reasoningEffort : "low";
    }
}
