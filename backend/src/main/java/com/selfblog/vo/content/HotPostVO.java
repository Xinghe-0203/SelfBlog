package com.selfblog.vo.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 热门文章 VO
 */
@Data
@Schema(description = "热门文章信息")
public class HotPostVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章分类")
    private String category;

    @Schema(description = "阅读数")
    private Long viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "热度评分")
    private Double score;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "文章封面图")
    private String coverImage;
}