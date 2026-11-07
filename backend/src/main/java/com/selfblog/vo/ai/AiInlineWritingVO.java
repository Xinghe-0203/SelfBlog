package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 行内写作结果：处理后的选区文本。
 *
 * <p>不写库——是否替换选区由管理员在编辑器里决定。</p>
 */
@Schema(description = "AI 行内写作结果")
public record AiInlineWritingVO(

        @Schema(description = "处理方式：polish、continue、shorten、translate")
        String action,

        @Schema(description = "处理后的文本")
        String text,

        @Schema(description = "实际使用的模型名")
        String model,

        @Schema(description = "模型调用耗时（毫秒）")
        Long latencyMs) {
}
