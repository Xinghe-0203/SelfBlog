package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情响应 VO
 */
@Data
@Schema(description = "文章详情响应")
public class PostDetailResponse {

    @Schema(description = "文章ID", example = "123")
    private Long id;

    @Schema(description = "作者ID", example = "1")
    private Long userId;

    @Schema(description = "作者用户名", example = "zhangsan")
    private String username;

    @Schema(description = "作者昵称", example = "张三")
    private String nickname;

    @Schema(description = "作者头像", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "文章标题", example = "Spring Boot 最佳实践")
    private String title;

    @Schema(description = "文章摘要", example = "本文介绍了Spring Boot的开发技巧...")
    private String summary;

    @Schema(description = "文章内容（HTML格式）")
    private String content;

    @Schema(description = "话题ID", example = "1")
    private Long topicId;

    @Schema(description = "话题名称", example = "技术分享")
    private String topicName;

    @Schema(description = "文章分类", example = "后端")
    private String category;

    @Schema(description = "封面图片URL", example = "https://example.com/cover.jpg")
    private String coverImage;

    @Schema(description = "阅读量", example = "1000")
    private Long viewCount;

    @Schema(description = "点赞数", example = "50")
    private Integer likeCount;

    @Schema(description = "评论数", example = "10")
    private Integer commentCount;

    @Schema(description = "收藏数", example = "5")
    private Integer collectCount;

    @Schema(description = "状态：0=待审核，1=已发布，2=已驳回", example = "1")
    private Integer status;

    @Schema(description = "审核人ID", example = "100")
    private Long reviewerId;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "驳回原因", example = "内容不符合规范")
    private String rejectReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "标签列表")
    private List<TagVO> tags;

    @Data
    @Schema(description = "标签信息")
    public static class TagVO {
        @Schema(description = "标签ID", example = "1")
        private Long id;
        @Schema(description = "标签名称", example = "Java")
        private String name;
    }
}
