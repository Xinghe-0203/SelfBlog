package com.selfblog.service.ai;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.service.seo.SeoLlmConfig;
import com.selfblog.service.seo.SeoLlmConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 能力的可用性闸门。
 *
 * <p>判定顺序与文案由契约 §0 固定：先看总开关（{@code llmEnabled}），再看是否配齐
 * 「接口地址 + API Key」。两个条件分开报错，管理员才知道该去改哪一项。</p>
 *
 * <p>只做判定，不发请求：所有生成端点进门先过这里，避免在没配置时白跑一次网络 IO。</p>
 */
@Slf4j
@Component
public class AiAvailability {

    static final String DISABLED_MESSAGE = "AI 辅助功能已关闭，请先在设置中启用";
    static final String NOT_CONFIGURED_MESSAGE = "尚未配置可用的模型接口，请先在设置中填写";

    private final SeoLlmConfigProvider configProvider;

    public AiAvailability(SeoLlmConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    /** 不可用时抛 {@code BusinessException(400, ...)}，可用时返回配置。 */
    public SeoLlmConfig requireUsable() {
        SeoLlmConfig config = configProvider.current();
        if (!config.enabled()) {
            throw new BusinessException(400, DISABLED_MESSAGE);
        }
        if (!config.configured()) {
            throw new BusinessException(400, NOT_CONFIGURED_MESSAGE);
        }
        return config;
    }

    /**
     * 不抛错的版本：不可用（含读取设置本身失败）返回 {@code null}。
     *
     * <p>后台自动审核用它做静默判定——任何异常都不能让访客留言提交失败。</p>
     */
    public SeoLlmConfig usableOrNull() {
        try {
            SeoLlmConfig config = configProvider.current();
            return config.enabled() && config.configured() ? config : null;
        } catch (Exception e) {
            log.warn("[AI] 读取模型配置失败，按不可用处理：{}", e.getClass().getSimpleName());
            return null;
        }
    }
}
