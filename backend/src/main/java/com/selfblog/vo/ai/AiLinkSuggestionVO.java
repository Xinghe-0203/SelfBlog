package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单条内链建议。
 *
 * <p>{@code targetTitle} 一律取自候选文章列表（服务端查库所得），
 * 绝不采信模型输出的标题——模型只允许回 postId/anchor/reason。</p>
 */
@Schema(description = "AI 内链建议")
public record AiLinkSuggestionVO(

        @Schema(description = "目标文章 ID（一定是已发布的候选文章）")
        Long targetPostId,

        @Schema(description = "目标文章标题（来自候选列表，不采信模型输出）")
        String targetTitle,

        @Schema(description = "建议的锚文本，2-12 字")
        String anchor,

        @Schema(description = "一句话推荐理由")
        String reason) {
}
