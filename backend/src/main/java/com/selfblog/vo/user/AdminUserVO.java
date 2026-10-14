package com.selfblog.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员用户信息 VO
 */
@Data
@Schema(description = "管理员用户信息")
public class AdminUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色：user=普通用户，admin=管理员")
    private String role;

    @Schema(description = "账号状态：1=正常，0=禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "粉丝数")
    private Integer followerCount;

    @Schema(description = "关注数")
    private Integer followingCount;
}