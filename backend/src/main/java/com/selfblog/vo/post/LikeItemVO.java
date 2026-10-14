package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的点赞列表项VO（包含文章详细信息）
 */
@Data
@Schema(description = "我的点赞列表项")
public class LikeItemVO {

    @Schema(description = "点赞ID")
    private Long likeId;

    @Schema(description = "文章ID")
    private Long postId;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章分类")
    private String category;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者昵称")
    private String authorNickname;

    @Schema(description = "作者头像")
    private String authorAvatar;

    @Schema(description = "阅读量")
    private Long viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "收藏数")
    private Integer collectCount;

    @Schema(description = "点赞时间")
    private LocalDateTime likeTime;
}