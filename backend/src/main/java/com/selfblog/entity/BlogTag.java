package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 标签实体类 (blog_tag)
 */
@Data
@TableName("blog_tag")
@Schema(description = "标签信息")
public class BlogTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "标签ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称（唯一）
     */
    @Schema(description = "标签名称")
    private String name;

    /**
     * 文章数量
     * 【已冗余】通过 blog_post_tag 实时聚合，不再维护此字段
     */
    // @Schema(description = "文章数量")
    // private Integer postCount;

    /**
     * 逻辑删除：0-正常，1-删除
     */
    @Schema(description = "逻辑删除：0-正常，1-删除")
    @TableLogic
    private Integer isDeleted;
}
