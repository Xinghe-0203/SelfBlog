package com.selfblog.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图片描述请求。
 *
 * <p>封面用 {@code source=cover + postId}，相册用 {@code source=album + photoId}。</p>
 */
@Data
@Schema(description = "图片描述请求")
public class AiImageAltRequest {

    @Schema(description = "图片来源：cover（文章封面）或 album（相册照片）", example = "cover")
    private String source;

    @Schema(description = "文章 ID，source=cover 时必填", example = "12")
    private Long postId;

    @Schema(description = "相册照片 ID，source=album 时必填", example = "7")
    private Long photoId;
}
