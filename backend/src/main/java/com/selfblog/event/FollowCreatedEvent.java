package com.selfblog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 关注创建事件，用于在事务提交后异步发送通知
 */
@Getter
public class FollowCreatedEvent extends ApplicationEvent {
    private final Long followerId;  // 关注者用户ID
    private final Long followingId; // 被关注者用户ID

    public FollowCreatedEvent(Long followerId, Long followingId) {
        super(followerId);
        this.followerId = followerId;
        this.followingId = followingId;
    }
}
