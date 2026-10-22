package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 标签推荐结果。
 *
 * <p>{@code tags} 只会包含已有标签池里的名字，顺序按模型给出的优先级。</p>
 */
@Data
@Schema(description = "AI 推荐的标签")
public class AiTagsVO {

    @Schema(description = "3-6 个已有标签名")
    private List<String> tags;

    @Schema(description = "实际使用的模型名")
    private String model;

    @Schema(description = "模型调用耗时（毫秒）")
    private Long latencyMs;
}
