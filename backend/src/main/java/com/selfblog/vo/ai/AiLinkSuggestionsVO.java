package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 内链建议结果：最多 5 条，按模型给出的相关度排序。
 *
 * <p>候选文章都不相关时 {@code suggestions} 为空数组——这是合法结果，不是失败。</p>
 */
@Schema(description = "AI 内链建议列表")
public record AiLinkSuggestionsVO(

        @Schema(description = "建议列表，最多 5 条；没有合适的候选时为空数组")
        List<AiLinkSuggestionVO> suggestions,

        @Schema(description = "实际使用的模型名")
        String model,

        @Schema(description = "模型调用耗时（毫秒）")
        Long latencyMs) {

    public AiLinkSuggestionsVO {
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }
}
