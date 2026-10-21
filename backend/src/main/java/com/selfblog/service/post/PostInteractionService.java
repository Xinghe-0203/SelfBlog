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
     * 用真实评论行数覆盖文章的评论数（C8 的绝对值同步）。
     *
     * <p>相对加减只能记账：历史漂移一旦发生（并发删除、重复提交、早期无守卫的减法），
     * 后续减多少都回不到真实值。这里直接读 {@code blog_comment} 的有效行数写回
     * {@code blog_post.comment_count}，让计数自我纠正。</p>
     *
     * @param postId 文章ID
     */
    void syncCommentCount(Long postId);

    /**
     * 增加收藏数
     */
    void incrementCollectCount(Long postId);

    /**
     * 减少收藏数
     */
    void decrementCollectCount(Long postId);
}
