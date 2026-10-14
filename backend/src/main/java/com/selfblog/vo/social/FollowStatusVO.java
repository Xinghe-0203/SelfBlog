package com.selfblog.vo.social;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注状态 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "关注状态")
public class FollowStatusVO {

    @Schema(description = "是否关注")
    private Boolean following;

    @Schema(description = "操作类型：follow-关注，unfollow-取消关注")
    private String action;

    @Schema(description = "粉丝数")
    private Integer followerCount;

    @Schema(description = "关注数")
    private Integer followingCount;
}