package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 校友圈转发记录实体类 (circle_repost)
 */
@Data
@TableName("blog_circle_repost")
public class CircleRepost implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 转发者用户ID
     */
    private Long userId;

    /**
     * 原始动态ID
     */
    private Long originalPostId;

    /**
     * 新动态ID（转发生成的新动态）
     */
    private Long newPostId;

    /**
     * 转发时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除：0-正常，1-删除
     */
    @TableLogic
    private Integer isDeleted;
}
