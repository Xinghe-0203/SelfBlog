package com.selfblog.vo.circle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 校友圈点赞操作结果VO
 */
@Data
@Schema(description = "校友圈点赞操作结果")
public class CircleLikeResultVO {

    @Schema(description = "操作类型：like-点赞，unlike-取消点赞")
    private String action;

    @Schema(description = "当前动态点赞总数")
    private Integer likeCount;
}
