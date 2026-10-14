package com.selfblog.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理员文章查询请求 DTO
 */
@Data
@Schema(description = "管理员文章查询请求")
public class AdminPostQueryRequest {

    @Schema(description = "搜索关键词（标题/内容）")
    private String keyword;

    @Schema(description = "文章状态：0=待审核，1=已发布，2=已驳回")
    private Integer status;

    @Schema(description = "作者用户ID")
    private Long userId;

    @Schema(description = "文章分类")
    private String category;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量最小值为1")
    @Max(value = 100, message = "每页数量最大值为100")
    private Integer pageSize = 10;
}