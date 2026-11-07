package com.selfblog.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 行内写作请求：对编辑器选区做润色/续写/精简。
 *
 * <p>合法性（action 枚举、text 非空且不超过 3000 字符）由
 * {@code AiWritingService.inlineWrite} 校验并抛 400，与其他 AI 端点保持同一套错误文案风格。</p>
 */
@Schema(description = "AI 行内写作请求")
public record AiInlineWritingRequest(

        @Schema(description = "处理方式：polish（润色）、continue（续写）、shorten（精简）", example = "polish")
        String action,

        @Schema(description = "所选文字，去空白后不超过 3000 字符")
        String text) {
}
