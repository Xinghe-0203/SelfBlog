package com.selfblog.vo.circle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 校友圈动态响应 VO
 */
@Data
@Schema(description = "校友圈动态响应")
public class CirclePostVO {

    @Schema(description = "动态ID", example = "1")
    private Long id;

    @Schema(description = "作者ID", example = "1")
    private Long userId;

    @Schema(description = "作者用户名", example = "zhangsan")
    private String userUsername;

    @Schema(description = "作者昵称", example = "张三")
    private String userNickname;

    @Schema(description = "作者头像", example = "https://example.com/avatar.jpg")
    private String userAvatar;

    @Schema(description = "动态内容", example = "今天天气真好！")
    private String content;

    @Schema(description = "内容类型：1=纯文本，2=图文，3=转发，4=视频", example = "1")
    private Integer contentType;

    @Schema(description = "图片URL列表")
    private List<String> images;

    @Schema(description = "视频URL列表")
    private List<String> videos;

    @Schema(description = "转发的原动态")
    private CirclePostVO repostPost;

    @Schema(description = "转发的原动态是否已隐藏（无权查看）", example = "false")
    private Boolean originalPostHidden;

    @Schema(description = "位置信息", example = "北京大学")
    private String location;

    @Schema(description = "话题名称列表")
    private List<String> topicNames;

    @Schema(description = "话题ID列表")
    private List<Long> topicIds;

    @Schema(description = "点赞数", example = "50")
    private Integer likeCount;

    @Schema(description = "评论数", example = "10")
    private Integer commentCount;

    @Schema(description = "转发数", example = "5")
    private Integer repostCount;

    @Schema(description = "阅读量", example = "1000")
    private Long viewCount;

    @Schema(description = "是否已点赞", example = "false")
    @JsonProperty("isLiked")
    private Boolean isLiked;

    @Schema(description = "是否已转发", example = "false")
    @JsonProperty("isReposted")
    private Boolean isReposted;

    @Schema(description = "是否置顶", example = "false")
    @JsonProperty("isTop")
    private Boolean isTop;

    @Schema(description = "可见性：0=公开，1=仅关注者，2=仅自己", example = "0")
    private Integer visibility;

    @Schema(description = "是否允许评论：1=允许，0=不允许", example = "1")
    private Integer allowComment;

    @Schema(description = "是否允许转发：1=允许，0=不允许", example = "1")
    private Integer allowRepost;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "时间 ago 描述", example = "5分钟前")
    private String timeAgo;
}