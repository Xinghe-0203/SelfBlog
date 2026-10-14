package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报实体类 (blog_report)
 */
@Data
@TableName("blog_report")
public class BlogReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 举报人用户ID
     */
    private Long reporterId;

    /**
     * 被举报用户ID
     */
    private Long reportedUserId;

    /**
     * 举报目标类型：post=文章, comment=评论, user=用户
     */
    private String targetType;

    /**
     * 举报目标ID（文章ID、评论ID或用户ID）
     */
    private Long targetId;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 举报状态：0=待处理，1=已驳回，2=已核实
     */
    private Integer status;

    /**
     * 处理人ID（管理员）
     */
    private Long handlerId;

    /**
     * 处理结果说明
     */
    private String handlerResult;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

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