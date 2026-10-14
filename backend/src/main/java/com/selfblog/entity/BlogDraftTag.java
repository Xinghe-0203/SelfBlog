package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 草稿-标签关联实体类 (blog_draft_tag)
 * 【说明】中间表，实现草稿和标签的多对多关系
 */
@Data
@TableName("blog_draft_tag")
public class BlogDraftTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 草稿ID
     */
    private Long draftId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除：0=正常，1=删除
     */
    @TableLogic
    private Integer isDeleted;
}
