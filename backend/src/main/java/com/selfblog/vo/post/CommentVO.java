package com.selfblog.vo.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论响应VO（支持树形结构）
 */
@Data
@Schema(description = "评论响应")
public class CommentVO {

    @Schema(description = "评论ID", example = "1")
    private Long id;

    @Schema(description = "文章ID", example = "123")
    private Long postId;

    @Schema(description = "评论者用户ID", example = "1")
    private Long userId;

    @Schema(description = "评论者昵称（前端兼容）", example = "张三")
    private String nickname;

    @Schema(description = "评论者用户名（前端兼容）", example = "zhangsan")
    private String username;

    @Schema(description = "评论者头像", example = "https://example.com/avatar.jpg")
    private String userAvatar;

    @Schema(description = "父评论ID（为0表示顶级评论）", example = "0")
    private Long parentId;

    @Schema(description = "回复的用户昵称（如果是回复）", example = "李四")
    private String replyToNickname;

    @Schema(description = "评论内容", example = "写得很好，点赞！")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子评论列表（回复）")
    private List<CommentVO> replies;
}
