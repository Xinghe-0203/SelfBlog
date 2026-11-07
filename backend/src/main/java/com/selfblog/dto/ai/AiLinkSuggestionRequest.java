package com.selfblog.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 内链建议请求：给一篇文章，从其他已发布文章里挑值得站内链接的目标。
 *
 * <p>postId 缺失报 400、文章不存在报 404，与摘要/标签/标题三个写作辅助端点同一套语义，
 * 校验在 {@code AiWritingService.suggestLinks} 里。</p>
 */
@Schema(description = "AI 内链建议请求")
public record AiLinkSuggestionRequest(

        @Schema(description = "当前文章 ID", example = "12")
        Long postId) {
}
