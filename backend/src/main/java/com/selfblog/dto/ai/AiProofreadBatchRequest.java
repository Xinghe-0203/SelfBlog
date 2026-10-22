package com.selfblog.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量校对请求。
 *
 * <p>不传 {@code postIds} 时按 {@code limit} 取最近的已发布文章，便于「一键检查最近写的几篇」。</p>
 */
@Data
@Schema(description = "批量校对请求")
public class AiProofreadBatchRequest {

    @Schema(description = "指定文章 ID；为空时按 limit 自动挑选最近的已发布文章")
    private List<Long> postIds;

    @Schema(description = "最多处理多少篇，默认 5，上限 10", example = "5")
    private Integer limit;
}
