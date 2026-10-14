package com.selfblog.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 文章分页查询请求 DTO
 */
@Data
@Schema(description = "文章分页查询请求")
public class PostQueryRequest {

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量最小值为1")
    @Max(value = 100, message = "每页数量最大值为100")
    private Integer pageSize = 10;

    @Schema(description = "搜索关键词（标题/内容）")
    private String keyword;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "作者用户ID")
    private Long userId;

    @Schema(description = "标签ID")
    private Long tagId;

    @Schema(description = "排序方式：latest-最新, hot-最热, essence-精华", example = "latest")
    private String sort;
}
