package com.selfblog.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 站点内容体检报告。
 *
 * <p>只读体检：采样已发布文章的元数据（标题/摘要/分类/标签/发布时间/字数），
 * 一次性交给模型评估，不逐篇调用、也不修改任何数据。</p>
 */
@Data
@Schema(description = "站点内容体检报告")
public class AiHealthReportVO {

    @Schema(description = "报告生成时间（LocalDateTime 字符串）")
    private String generatedAt;

    @Schema(description = "站点健康分，0-100 整数")
    private Integer score;

    @Schema(description = "总体评价，100-300 字")
    private String summary;

    @Schema(description = "3-8 条具体发现")
    private List<Finding> findings;

    @Schema(description = "实际送进模型的文章条数")
    private Integer sampledPosts;

    @Schema(description = "已发布文章总数")
    private Integer totalPosts;

    @Schema(description = "实际使用的模型名")
    private String model;

    @Schema(description = "模型调用耗时（毫秒）")
    private Long latencyMs;

    @Data
    @Schema(description = "一条体检发现")
    public static class Finding {

        @Schema(description = "问题标题，如「摘要缺失」")
        private String title;

        @Schema(description = "具体说明")
        private String detail;

        @Schema(description = "严重程度：low、medium、high")
        private String severity;

        @Schema(description = "建议的处理动作")
        private String action;

        @Schema(description = "涉及的文章数量；非计数类问题为 null")
        private Integer count;
    }
}
