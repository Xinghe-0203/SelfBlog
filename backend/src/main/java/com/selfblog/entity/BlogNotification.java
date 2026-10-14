package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知实体类 (blog_notification)
 */
@Data
@TableName("blog_notification")
public class BlogNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知类型：LIKE=点赞，COMMENT=评论，REPLY=回复，FOLLOW=关注，SYSTEM=系统通知
     */
    private String type;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 触发通知的用户ID（发送者）
     */
    private Long fromUserId;

    /**
     * 接收通知的用户ID（接收者）
     */
    @TableField("user_id")
    private Long toUserId;

    /**
     * 目标类型：POST=文章，COMMENT=评论
     */
    private String targetType;

    /**
     * 目标ID（文章ID或评论ID）
     */
    private Long targetId;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除：0-正常，1-删除
     */
    @TableLogic
    private Integer isDeleted;
}
