package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章-媒体关联实体类 (blog_post_media)
 * 【说明】中间表，实现文章和媒体文件的多对多关系
 */
@Data
@TableName("blog_post_media")
public class BlogPostMedia implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    private Long postId;

    /**
     * 媒体ID
     */
    private Long mediaId;

    /**
     * 显示顺序
     */
    private Integer displayOrder;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除：0=正常，1=已删除
     */
    @TableLogic
    private Integer isDeleted;
}
