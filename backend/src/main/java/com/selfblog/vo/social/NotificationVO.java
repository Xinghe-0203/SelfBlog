package com.selfblog.vo.social;

import com.selfblog.vo.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知响应VO
 */
@Data
@Schema(description = "通知响应")
public class NotificationVO {

    @Schema(description = "通知ID", example = "1")
    private Long id;

    @Schema(description = "通知类型：LIKE=点赞，COMMENT=评论，REPLY=回复，FOLLOW=关注，SYSTEM=系统通知", example = "LIKE")
    private String type;

    @Schema(description = "通知标题", example = "收到点赞通知")
    private String title;

    @Schema(description = "通知内容", example = "用户张三点赞了你的文章")
    private String content;

    @Schema(description = "发送通知的用户信息")
    private UserVO fromUser;

    @Schema(description = "目标类型：POST=文章，COMMENT=评论", example = "POST")
    private String targetType;

    @Schema(description = "目标ID（文章ID或评论ID）", example = "123")
    private Long targetId;

    @Schema(description = "是否已读：0-未读，1-已读", example = "0")
    private Integer isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "相对时间描述，如'刚刚'、'5分钟前'等", example = "5分钟前")
    private String timeAgo;
}
