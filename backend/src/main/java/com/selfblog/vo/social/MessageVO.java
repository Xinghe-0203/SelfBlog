package com.selfblog.vo.social;

import com.selfblog.vo.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信响应 VO
 */
@Data
@Schema(description = "私信响应")
public class MessageVO {

    @Schema(description = "私信ID")
    private Long id;

    @Schema(description = "发送者用户ID")
    private Long senderId;

    @Schema(description = "接收者用户ID")
    private Long receiverId;

    @Schema(description = "发送者用户信息")
    private UserVO sender;

    @Schema(description = "接收者用户信息")
    private UserVO receiver;

    @Schema(description = "私信内容")
    private String content;

    @Schema(description = "是否已读：0-未读，1-已读")
    private Integer isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "相对时间描述，如'刚刚'、'5分钟前'等")
    private String timeAgo;
}