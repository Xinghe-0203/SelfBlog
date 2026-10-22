package com.selfblog.vo.seo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 后台 SEO 列表行。
 *
 * <p>刻意<b>不</b>携带正文与摘要全文：列表页只需要标题和 SEO 状态，
 * 把 CLOB 正文带进分页响应会让一次列表查询搬运几百 KB 无用数据。</p>
 *
 * <p>{@code issues} 元素是 {@code SeoIssue} 的 code 字符串（camelCase），
 * 与前端 {@code SeoIssue} 联合类型严格一致。</p>
 */
@Data
@Schema(description = "SEO 列表行")
public class SeoPostRowVO {

    @Schema(description = "文章 ID")
    private Long postId;

    @Schema(description = "文章自身标题（用于展示，不是 SEO 标题）")
    private String title;

    @Schema(description = "文章状态：0 待审核 / 1 已发布 / 2 已拒绝")
    private Integer status;

    @Schema(description = "是否已有 SEO 元数据（四个字段至少一个非空）")
    private boolean hasSeo;

    private String seoTitle;

    private String seoDescription;

    private String seoKeywords;

    private String ogImageUrl;

    @Schema(description = "来源：manual / llm；无记录时为 null")
    private String source;

    @Schema(description = "模型生成时间，ISO-8601 本地时间；无记录时为 null")
    private String generatedAt;

    @Schema(description = "SEO 记录更新时间；无记录时为 null")
    private String updateTime;

    @Schema(description = "体检出的问题码，无问题时为空数组")
    private List<String> issues;
}
