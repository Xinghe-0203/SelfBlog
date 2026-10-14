package com.selfblog.listener;

import com.selfblog.event.CollectCreatedEvent;
import com.selfblog.event.CommentCreatedEvent;
import com.selfblog.event.FollowCreatedEvent;
import com.selfblog.event.LikeCreatedEvent;
import com.selfblog.service.social.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

/**
 * 通知事件监听器，处理评论、点赞、关注等事件的异步通知发送
 * 使用 @TransactionalEventListener 确保在事务提交后才发送通知
 */
@Slf4j
@Component
public class NotificationEventListener {

    @Autowired
    private NotificationService notificationService;

    /**
     * 处理评论创建事件
     * TransactionPhase.AFTER_COMMIT 确保事务提交后才执行
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        try {
            // 不给自己发通知
            if (Objects.equals(event.getCommenterUserId(), event.getPostAuthorUserId())) {
                return;
            }

            String notifyType = event.isReply() ? "reply" : "comment";
            String notifyTitle = event.isReply() ? "有人回复了你的评论" : "有人评论了你的文章";
            String notifyContent = event.isReply()
                    ? "用户回复了你的评论：" + event.getContent()
                    : "用户评论了你的文章";

            notificationService.sendNotification(
                    notifyType,
                    notifyTitle,
                    notifyContent,
                    event.getCommenterUserId(),
                    event.getPostAuthorUserId(),
                    "post",
                    event.getPostId()
            );
        } catch (Exception e) {
            // 记录日志但不要影响业务流程，通知失败不应该影响评论创建
            log.error("发送评论通知失败: commentId={}, error={}", event.getCommentId(), e.getMessage());
        }
    }

    /**
     * 处理点赞创建事件
     * TransactionPhase.AFTER_COMMIT 确保事务提交后才执行
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLikeCreated(LikeCreatedEvent event) {
        try {
            // 不给自己发通知
            if (Objects.equals(event.getUserId(), event.getPostAuthorId())) {
                return;
            }

            notificationService.sendNotification(
                    "like",
                    "有人点赞了你的文章",
                    "用户点赞了你的文章：" + event.getPostTitle(),
                    event.getUserId(),
                    event.getPostAuthorId(),
                    "post",
                    event.getPostId()
            );
        } catch (Exception e) {
            // 记录日志但不要影响业务流程
            log.error("发送点赞通知失败: postId={}, error={}", event.getPostId(), e.getMessage());
        }
    }

    /**
     * 处理关注创建事件
     * TransactionPhase.AFTER_COMMIT 确保事务提交后才执行
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFollowCreated(FollowCreatedEvent event) {
        try {
            // 不给自己发通知
            if (Objects.equals(event.getFollowerId(), event.getFollowingId())) {
                return;
            }

            notificationService.sendNotification(
                    "follow",
                    "有人关注了你",
                    "用户关注了你",
                    event.getFollowerId(),
                    event.getFollowingId(),
                    "user",
                    event.getFollowingId()
            );
        } catch (Exception e) {
            // 记录日志但不要影响业务流程
            log.error("发送关注通知失败: followerId={}, followingId={}, error={}",
                    event.getFollowerId(), event.getFollowingId(), e.getMessage());
        }
    }

    /**
     * 处理收藏创建事件
     * TransactionPhase.AFTER_COMMIT 确保事务提交后才执行
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCollectCreated(CollectCreatedEvent event) {
        try {
            // 不给自己发通知
            if (Objects.equals(event.getUserId(), event.getPostAuthorId())) {
                return;
            }

            notificationService.sendNotification(
                    "collect",
                    "有人收藏了你的文章",
                    "用户收藏了你的文章：" + event.getPostTitle(),
                    event.getUserId(),
                    event.getPostAuthorId(),
                    "post",
                    event.getPostId()
            );
        } catch (Exception e) {
            // 记录日志但不要影响业务流程
            log.error("发送收藏通知失败: postId={}, error={}", event.getPostId(), e.getMessage());
        }
    }
}
