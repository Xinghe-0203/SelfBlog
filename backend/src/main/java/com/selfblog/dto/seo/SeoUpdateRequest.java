package com.selfblog.dto.seo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台手填 SEO 元数据请求。
 *
 * <p>四个字段都可为 {@code null}/空：空表示「不自定义、回落到派生值」，
 * 服务层会存成 {@code NULL} 而不是空字符串，避免出现「有行但全空」的伪覆盖。</p>
 */
@Data
@Schema(description = "保存文章 SEO 元数据")
public class SeoUpdateRequest {

    @Schema(description = "自定义 SEO 标题，最长 200；留空则沿用文章标题")
    private String seoTitle;

    @Schema(description = "自定义 SEO 描述，最长 400；留空则沿用摘要/正文派生")
    private String seoDescription;

    @Schema(description = "关键词，逗号分隔，最长 300")
    private String seoKeywords;

    @Schema(description = "社交分享封面图，站内相对路径或以 http(s) 开头，最长 500")
    private String ogImageUrl;
}
