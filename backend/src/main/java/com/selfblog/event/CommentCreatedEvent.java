package com.selfblog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 评论创建事件，用于在事务提交后异步发送通知
 */
@Getter
public class CommentCreatedEvent extends ApplicationEvent {
    private final Long commentId;
    private final Long commenterUserId;  // 评论者用户ID
    private final Long postAuthorUserId; // 文章作者用户ID
    private final Long postId;
    private final String content;
    private final boolean isReply;  // 是否是回复

    public CommentCreatedEvent(Long commentId, Long commenterUserId, Long postAuthorUserId,
                               Long postId, String content, boolean isReply) {
        super(commentId);
        this.commentId = commentId;
        this.commenterUserId = commenterUserId;
        this.postAuthorUserId = postAuthorUserId;
        this.postId = postId;
        this.content = content;
        this.isReply = isReply;
    }
}
