package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 单篇文章的校对结果。
 */
@Data
@Schema(description = "AI 校对结果")
public class AiProofreadVO {

    @Schema(description = "文章 ID")
    private Long postId;

    @Schema(description = "发现的问题，最多 20 条")
    private List<Issue> issues;

    @Schema(description = "正文是否因超过 6000 字被截断（截断后只检查了前 6000 字）")
    private Boolean truncated;

    @Schema(description = "实际使用的模型名")
    private String model;

    @Schema(description = "模型调用耗时（毫秒）")
    private Long latencyMs;

    @Data
    @Schema(description = "一处待修改的地方")
    public static class Issue {

        @Schema(description = "问题类型：typo（错别字）、grammar（语法）、wording（用词）、fact（事实存疑）")
        private String type;

        @Schema(description = "原文片段，最多 120 字")
        private String excerpt;

        @Schema(description = "修改建议，最多 200 字")
        private String suggestion;

        @Schema(description = "严重程度：low、medium、high")
        private String severity;
    }
}
