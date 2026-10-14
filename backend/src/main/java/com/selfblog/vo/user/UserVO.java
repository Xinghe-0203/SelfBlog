package com.selfblog.vo.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息 VO（脱敏）
 * 用于返回用户信息，不包含敏感字段如密码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息（脱敏）")
public class UserVO {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "封面图URL", example = "https://example.com/cover.jpg")
    private String coverImage;

    @Schema(description = "个人简介", example = "热爱编程，专注Java后端开发")
    private String bio;

    @Schema(description = "邮箱（仅本人或管理员可见）", example = "user@example.com")
    private String email;

    @Schema(description = "用户角色", example = "ROLE_USER")
    private String role;

    @Schema(description = "账号状态：1=正常，0=禁用", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "粉丝数", example = "100")
    private Integer followerCount;

    @Schema(description = "关注数", example = "50")
    private Integer followingCount;

    @Schema(description = "是否关注（当前用户是否关注此用户）", example = "false")
    @JsonProperty("isFollowing")
    private Boolean isFollowing;

    @Schema(description = "用户ID（兼容字段）")
    @JsonProperty("userId")
    public Long getUserId() {
        return id;
    }
}