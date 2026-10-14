package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 收藏操作结果VO
 */
@Data
@Schema(description = "收藏操作结果")
public class CollectResultVO {

    @Schema(description = "操作类型：collect-收藏，uncollect-取消收藏")
    private String action;

    @Schema(description = "当前收藏数")
    private Integer collectCount;
}
