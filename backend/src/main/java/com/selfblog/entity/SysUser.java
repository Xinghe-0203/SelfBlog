package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类 (sys_user)
 * 【说明】
 *   对应数据库中的 sys_user 表，使用 MyBatis Plus 注解自动映射字段。
 *   使用 Lombok @Data 注解自动生成 Getter/Setter/toString/equals/hashCode。
 */
@Data
@TableName("sys_user")
@Schema(description = "用户信息")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增长
     */
    @Schema(description = "用户ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名，唯一
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 密码（加密存储）
     */
    @JsonIgnore
    @ToString.Exclude
    @Schema(description = "密码")
    private String password;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String nickname;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL")
    private String avatar;

    /**
     * 封面图URL
     */
    @Schema(description = "封面图URL")
    private String coverImage;

    /**
     * 个人简介
     */
    @Schema(description = "个人简介")
    private String bio;

    /**
     * 邮箱地址
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 用户角色：user=普通用户，admin=管理员
     */
    @Schema(description = "用户角色")
    private String role;

    /**
     * 账号状态：1=正常，0=禁用
     */
    @Schema(description = "账号状态")
    private Integer status;

    /**
     * 粉丝数
     */
    @Schema(description = "粉丝数")
    private Integer followerCount;

    /**
     * 关注数
     */
    @Schema(description = "关注数")
    private Integer followingCount;

    /**
     * 登录失败次数
     */
    @Schema(description = "登录失败次数")
    @TableField(fill = FieldFill.INSERT)
    private Integer loginFailCount;

    /**
     * 账户锁定截止时间
     */
    @Schema(description = "账户锁定截止时间")
    private LocalDateTime lockUntil;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0=正常，1=删除
     */
    @Schema(description = "逻辑删除")
    @TableLogic
    private Integer isDeleted;
}
