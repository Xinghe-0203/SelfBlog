package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论实体类 (blog_comment)
 */
@Data
@TableName("blog_comment")
@Schema(description = "评论信息")
public class BlogComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "评论ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属文章ID
     */
    @Schema(description = "所属文章ID")
    private Long postId;

    /**
     * 评论者用户ID
     */
    @Schema(description = "评论者用户ID")
    private Long userId;

    /**
     * 父评论ID（用于回复功能，NULL表示一级评论）
     */
    @Schema(description = "父评论ID")
    private Long parentId;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容")
    private String content;

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
