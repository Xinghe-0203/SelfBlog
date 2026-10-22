package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 单条评论的审核结论。
 */
@Data
@Schema(description = "评论审核结论")
public class AiModerationVO {

    @Schema(description = "结论：pass（通过）、flag（可疑，待人工确认）、block（拦截）")
    private String verdict;

    @Schema(description = "判定理由，一句话中文说明")
    private String reason;

    @Schema(description = "命中的问题分类，可为空数组")
    private List<String> categories;

    @Schema(description = "实际使用的模型名")
    private String model;

    @Schema(description = "模型调用耗时（毫秒）")
    private Long latencyMs;
}
