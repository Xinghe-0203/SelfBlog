package com.selfblog.vo.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "热门内容VO（文章+动态统一）")
public class HotContentVO {

    @Schema(description = "内容ID")
    private Long id;

    @Schema(description = "标题（文章有，动态为空）")
    private String title;

    @Schema(description = "内容/摘要")
    private String content;

    @Schema(description = "作者ID")
    private Long userId;

    @Schema(description = "作者用户名")
    private String username;

    @Schema(description = "作者昵称")
    private String nickname;

    @Schema(description = "作者头像")
    private String avatar;

    @Schema(description = "类型：0=文章，1=动态")
    private Integer type;

    @Schema(description = "点赞数")
    private Long likeCount;

    @Schema(description = "评论数")
    private Long commentCount;

    @Schema(description = "分享/转发数")
    private Long shareCount;

    @Schema(description = "阅读数")
    private Long viewCount;

    @Schema(description = "图片列表（动态有，文章为空）")
    private List<String> images;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "话题列表（动态有，文章为空）")
    private List<String> topics;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "热度评分")
    private Double score;
}
