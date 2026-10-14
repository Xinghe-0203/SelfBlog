package com.selfblog.vo.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 热门标签 VO
 */
@Data
@Schema(description = "热门标签信息")
public class HotTagVO {

    @Schema(description = "标签ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "文章数量")
    private Long postCount;
}