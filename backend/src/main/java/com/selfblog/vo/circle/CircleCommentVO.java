package com.selfblog.vo.circle;

import com.selfblog.vo.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 校友圈评论响应VO（支持树形结构）
 */
@Data
@Schema(description = "校友圈评论响应")
public class CircleCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "动态ID")
    private Long postId;

    @Schema(description = "评论者用户信息")
    private UserVO user;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "父评论ID（二级回复）")
    private Long parentId;

    @Schema(description = "回复给的用户ID")
    private Long replyToUserId;

    @Schema(description = "回复给的用户昵称")
    private String replyToNickname;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "子评论列表（二级回复）")
    private List<CircleCommentVO> replies;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "相对时间")
    private String timeAgo;
}
