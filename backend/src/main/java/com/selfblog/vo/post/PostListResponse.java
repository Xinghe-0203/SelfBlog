package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表响应 VO
 */
@Data
@Schema(description = "文章列表响应")
public class PostListResponse {

    @Schema(description = "文章ID", example = "123")
    private Long id;

    @Schema(description = "作者ID", example = "1")
    private Long userId;

    @Schema(description = "作者昵称", example = "张三")
    private String nickname;

    @Schema(description = "作者头像", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "文章标题", example = "Spring Boot 最佳实践")
    private String title;

    @Schema(description = "文章摘要", example = "本文介绍了Spring Boot的开发技巧...")
    private String summary;

    @Schema(description = "话题ID", example = "1")
    private Long topicId;

    @Schema(description = "话题名称", example = "技术分享")
    private String topicName;

    @Schema(description = "文章分类", example = "后端")
    private String category;

    @Schema(description = "阅读量", example = "1000")
    private Long viewCount;

    @Schema(description = "点赞数", example = "50")
    private Integer likeCount;

    @Schema(description = "评论数", example = "10")
    private Integer commentCount;

    @Schema(description = "收藏数", example = "5")
    private Integer collectCount;

    @Schema(description = "分享数", example = "3")
    private Integer shareCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "标签列表")
    private List<TagVO> tags;

    @Schema(description = "文章封面图", example = "https://example.com/cover.jpg")
    private String coverImage;

    @Schema(description = "作者用户名", example = "zhangsan")
    private String username;

    @Schema(description = "标签信息")
    public static class TagVO {
        @Schema(description = "标签ID", example = "1")
        private Long id;
        @Schema(description = "标签名称", example = "Java")
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
