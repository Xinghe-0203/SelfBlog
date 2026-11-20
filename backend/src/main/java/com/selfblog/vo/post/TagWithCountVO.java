package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 标签 + 已发布文章数（公开标签索引页用）。
 *
 * <p>数量只统计**已发布**且未删除的文章：草稿与已删除文章既不公开，
 * 也不该出现在公开计数里，否则标签页会泄露「有几篇草稿」。</p>
 */
@Data
@Schema(description = "标签与文章计数")
public class TagWithCountVO {

    @Schema(description = "标签ID", example = "12")
    private Long id;

    @Schema(description = "标签名称", example = "工程实践")
    private String name;

    @Schema(description = "该标签下的已发布文章数", example = "3")
    private Long postCount;
}
