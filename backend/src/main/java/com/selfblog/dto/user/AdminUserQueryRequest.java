package com.selfblog.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理员用户查询请求 DTO
 */
@Data
@Schema(description = "管理员用户查询请求")
public class AdminUserQueryRequest {

    @Schema(description = "搜索关键词（用户名/昵称/邮箱）")
    private String keyword;

    @Schema(description = "账号状态：1=正常，0=禁用")
    private Integer status;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量最小值为1")
    @Max(value = 100, message = "每页数量最大值为100")
    private Integer pageSize = 10;
}