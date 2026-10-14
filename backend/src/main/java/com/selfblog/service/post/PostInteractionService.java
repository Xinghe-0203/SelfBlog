package com.selfblog.service.post;

/**
 * 帖子交互服务接口 — 负责点赞、收藏、浏览计数
 */
public interface PostInteractionService {

    /**
     * 增加阅读量（带防刷校验）
     */
    void incrementViewCount(Long postId, String userKey);

    /**
     * 增加阅读量
     */
    void incrementViewCount(Long postId);

    /**
     * 增加点赞数
     */
    void incrementLikeCount(Long postId);

    /**
     * 减少点赞数
     */
    void decrementLikeCount(Long postId);

    /**
     * 增加评论数
     */
    void incrementCommentCount(Long postId);

    /**
     * 减少评论数
     */
    void decrementCommentCount(Long postId, int count);

    /**
     * 增加收藏数
     */
    void incrementCollectCount(Long postId);

    /**
     * 减少收藏数
     */
    void decrementCollectCount(Long postId);
}
