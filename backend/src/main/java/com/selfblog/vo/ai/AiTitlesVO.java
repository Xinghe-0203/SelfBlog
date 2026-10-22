package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 标题候选结果（恰好 3 个，每个不超过 60 字，互不相同）。
 */
@Data
@Schema(description = "AI 生成的标题候选")
public class AiTitlesVO {

    @Schema(description = "3 个互不相同的标题候选")
    private List<String> titles;

    @Schema(description = "实际使用的模型名")
    private String model;

    @Schema(description = "模型调用耗时（毫秒）")
    private Long latencyMs;
}
