package com.selfblog.dto.social;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 媒体分页查询请求 DTO
 */
@Data
@Schema(description = "媒体分页查询请求")
public class MediaQueryRequest {

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量最小值为1")
    @Max(value = 100, message = "每页数量最大值为100")
    private Integer pageSize = 10;

    @Schema(description = "文件类型（image/video/audio）")
    private String fileType;
}
