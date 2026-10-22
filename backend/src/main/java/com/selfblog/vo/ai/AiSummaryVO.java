package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章摘要生成结果。
 */
@Data
@Schema(description = "AI 生成的文章摘要")
public class AiSummaryVO {

    @Schema(description = "80-200 字中文摘要，纯文本无 Markdown")
    private String summary;

    @Schema(description = "实际使用的模型名")
    private String model;

    @Schema(description = "模型调用耗时（毫秒）")
    private Long latencyMs;
}
