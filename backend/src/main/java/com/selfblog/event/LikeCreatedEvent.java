package com.selfblog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 点赞创建事件，用于在事务提交后异步发送通知
 */
@Getter
public class LikeCreatedEvent extends ApplicationEvent {
    private final Long userId;         // 点赞者用户ID
    private final Long postAuthorId;   // 文章作者用户ID
    private final Long postId;
    private final String postTitle;

    public LikeCreatedEvent(Long userId, Long postAuthorId, Long postId, String postTitle) {
        super(postId);
        this.userId = userId;
        this.postAuthorId = postAuthorId;
        this.postId = postId;
        this.postTitle = postTitle;
    }
}
