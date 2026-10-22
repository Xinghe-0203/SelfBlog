package com.selfblog.service.seo;

/**
 * 上游模型调用失败。
 *
 * <p>{@link #getMessage()} 是<b>可以直接回给管理员的安全文案</b>：绝不包含 API Key、
 * Authorization 头或上游原始响应体。真正的排查细节只写日志（见
 * {@code OpenAiCompatibleLlmClient} 与 {@code SeoGenerationService}）。</p>
 */
public class LlmUpstreamException extends RuntimeException {

    public LlmUpstreamException(String safeMessage) {
        super(safeMessage);
    }

    public LlmUpstreamException(String safeMessage, Throwable cause) {
        super(safeMessage, cause);
    }
}
