package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私信实体类 (blog_message)
 */
@Data
@TableName("blog_message")
@Schema(description = "私信信息")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "私信ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送者用户ID
     */
    @Schema(description = "发送者用户ID")
    private Long senderId;

    /**
     * 接收者用户ID
     */
    @Schema(description = "接收者用户ID")
    private Long receiverId;

    /**
     * 私信内容
     */
    @Schema(description = "私信内容")
    private String content;

    /**
     * 是否已读：0-未读，1-已读
     */
    @Schema(description = "是否已读：0-未读，1-已读")
    private Integer isRead;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除：0-正常，1-删除
     */
    @Schema(description = "逻辑删除：0-正常，1-删除")
    @TableLogic
    private Integer isDeleted;
}