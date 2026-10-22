package com.selfblog.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 以文章为输入的操作请求（摘要、标签、标题三个写作辅助端点共用）。
 */
@Data
@Schema(description = "写作辅助请求")
public class AiPostRequest {

    @Schema(description = "文章 ID", example = "12")
    private Long postId;
}
