package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 话题实体类 (blog_topic)
 */
@Data
@TableName("blog_topic")
public class Topic implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 话题名称（如 Java、Python）
     */
    private String name;

    /**
     * 话题描述
     */
    private String description;

    /**
     * 关联动态数
     */
    private Integer postCount;

    /**
     * 热度分数
     */
    private Integer trendingScore;

    /**
     * 状态：1=正常，0=禁用
     */
    private Integer status;

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
     * 逻辑删除：0-正常，1-删除
     */
    @TableLogic
    private Integer isDeleted;
}
