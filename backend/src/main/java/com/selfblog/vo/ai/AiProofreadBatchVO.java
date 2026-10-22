package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量校对结果。
 *
 * <p>串行逐篇调用、逐篇隔离：某篇失败不会影响其它篇，失败原因写进 {@code failures}，
 * 成功的放进 {@code results}。因此 {@code succeeded + failed == requested}。</p>
 */
@Data
@Schema(description = "AI 批量校对结果")
public class AiProofreadBatchVO {

    @Schema(description = "请求处理的文章数")
    private Integer requested;

    @Schema(description = "成功篇数")
    private Integer succeeded;

    @Schema(description = "失败篇数")
    private Integer failed;

    @Schema(description = "成功的校对结果")
    private List<AiProofreadVO> results;

    @Schema(description = "失败明细")
    private List<Failure> failures;

    @Data
    @Schema(description = "单篇失败原因")
    public static class Failure {

        @Schema(description = "文章 ID")
        private Long postId;

        @Schema(description = "失败原因")
        private String reason;
    }
}
