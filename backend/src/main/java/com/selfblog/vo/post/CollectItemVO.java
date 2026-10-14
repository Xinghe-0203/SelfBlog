package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 我的收藏列表项VO
 */
@Data
@Schema(description = "我的收藏列表项")
public class CollectItemVO {

    @Schema(description = "收藏ID")
    private Long collectId;

    @Schema(description = "文章ID")
    private Long postId;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章分类")
    private String category;

    @Schema(description = "作者昵称")
    private String authorNickname;

    @Schema(description = "阅读量")
    private Long viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "收藏数")
    private Integer collectCount;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "收藏时间")
    private LocalDateTime collectTime;
}
