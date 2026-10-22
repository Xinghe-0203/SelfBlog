package com.selfblog.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 站点体检请求。
 */
@Data
@Schema(description = "站点体检请求")
public class AiHealthReportRequest {

    @Schema(description = "送进模型的文章条数，默认 40，上限 80", example = "40")
    private Integer limit;
}
