package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 点赞操作结果VO
 */
@Data
@Schema(description = "点赞操作结果")
public class LikeResultVO {

    @Schema(description = "操作类型：like-点赞，unlike-取消点赞")
    private String action;

    @Schema(description = "当前文章点赞总数")
    private Integer likeCount;
}
