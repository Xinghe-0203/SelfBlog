package com.selfblog.dto.social;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理举报请求 DTO
 */
@Data
@Schema(description = "处理举报请求")
public class HandleReportRequest {

    @Schema(description = "处理状态：1=已驳回，2=已核实", requiredMode = RequiredMode.REQUIRED)
    @NotNull(message = "处理状态不能为空")
    @Min(1)
    @Max(2)
    private Integer status;

    @Schema(description = "处理结果说明")
    @Size(max = 500, message = "处理结果说明不能超过500字符")
    private String handlerResult;
}