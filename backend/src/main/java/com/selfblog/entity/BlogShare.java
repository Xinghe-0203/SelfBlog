package com.selfblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章分享记录实体类 (blog_share)
 */
@Data
@TableName("blog_share")
@Schema(description = "文章分享记录")
public class BlogShare implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分享ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "分享者用户ID")
    private Long userId;

    @Schema(description = "被分享的文章ID")
    private Long postId;

    @Schema(description = "分享平台：web=网页分享, weixin=微信, qq=QQ, weibo=微博等")
    private String platform;

    @Schema(description = "分享时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "逻辑删除：0=正常，1=删除")
    @TableLogic
    private Integer isDeleted;
}