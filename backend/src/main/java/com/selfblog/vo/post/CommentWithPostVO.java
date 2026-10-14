package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的评论列表项VO（包含评论所属文章信息）
 */
@Data
@Schema(description = "我的评论列表项")
public class CommentWithPostVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "文章ID")
    private Long postId;

    @Schema(description = "文章标题")
    private String postTitle;

    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}