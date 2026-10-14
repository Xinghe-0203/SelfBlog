package com.selfblog.vo.social;

import com.selfblog.vo.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话列表项 VO
 * 用于消息列表页面展示会话
 */
@Data
@Schema(description = "会话列表项")
public class ConversationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话ID（对方用户ID）")
    private Long conversationId;

    @Schema(description = "对方用户信息")
    private UserVO user;

    @Schema(description = "最后一条消息内容")
    private String lastMessage;

    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "相对时间描述")
    private String timeAgo;

    @Schema(description = "未读消息数量")
    private Integer unreadCount;

    @Schema(description = "消息总数")
    private Integer messageCount;
}
