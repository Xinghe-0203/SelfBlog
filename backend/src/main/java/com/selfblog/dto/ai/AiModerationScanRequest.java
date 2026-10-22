package com.selfblog.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 批量扫描待审评论的请求。
 */
@Data
@Schema(description = "评论批量审核请求")
public class AiModerationScanRequest {

    @Schema(description = "最多扫描多少条已公开的访客留言，默认 20，上限 50", example = "20")
    private Integer limit;
}
