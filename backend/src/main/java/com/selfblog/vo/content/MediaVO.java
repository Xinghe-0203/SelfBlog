package com.selfblog.vo.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 媒体文件响应 VO
 */
@Data
@Schema(description = "媒体文件响应")
public class MediaVO {

    @Schema(description = "媒体ID")
    private Long id;

    @Schema(description = "文件访问URL")
    private String fileUrl;

    @Schema(description = "缩略图URL")
    private String thumbUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;
}