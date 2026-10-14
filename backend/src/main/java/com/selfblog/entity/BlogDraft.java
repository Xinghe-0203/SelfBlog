package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章草稿实体类 (blog_draft)
 */
@Data
@TableName("blog_draft")
public class BlogDraft implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 草稿标题
     */
    private String title;

    /**
     * 草稿内容
     */
    private String content;

    /**
     * 草稿摘要
     */
    private String summary;

    /**
     * 草稿分类
     */
    private String category;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 标签ID列表（逗号分隔存储）
     * 【待迁移】将迁移至 blog_draft_tag 关联表，符合1NF范式
     */
    private String tagIds;

    /**
     * 话题ID
     */
    private Long topicId;

    /**
     * 关联的文章ID（编辑已有文章时使用）
     */
    private Long postId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;
}
