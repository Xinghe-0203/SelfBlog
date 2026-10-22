package com.selfblog.vo.seo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台 SEO 概览。
 *
 * <p>字段与前端 {@code frontend/src/types/seo.ts} 的 {@code SeoOverview} 一一对应，
 * 新增字段只能追加（前端按字段名读取）。</p>
 *
 * <p>统计口径：只统计未删除、已发布（{@code status = 1}）的文章。</p>
 */
@Data
@Schema(description = "SEO 概览")
public class SeoOverviewVO {

    @Schema(description = "已存密钥且已配置接口地址（真正可以调用模型）")
    private boolean llmConfigured;

    @Schema(description = "设置里是否打开 LLM 功能总开关")
    private boolean llmEnabled;

    @Schema(description = "发布后是否自动生成")
    private boolean autoOnPublish;

    @Schema(description = "当前模型名；未配置时为空字符串")
    private String model;

    @Schema(description = "已发布文章总数")
    private int totalPosts;

    @Schema(description = "已有 SEO 元数据的文章数")
    private int withSeo;

    @Schema(description = "SEO 覆盖率（百分比，四舍五入）")
    private int coveragePercent;

    @Schema(description = "既无自定义描述又无摘要的文章数")
    private int missingDescription;

    @Schema(description = "前台渲染标题超过 60 字符的文章数")
    private int longTitle;

    @Schema(description = "标题重复的文章数（按涉及的篇数计，不是重复组数）")
    private int duplicateTitle;

    @Schema(description = "既无分享图又无封面的文章数")
    private int missingCover;
}
