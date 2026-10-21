package com.selfblog.service.circle;

import com.selfblog.vo.circle.CircleCommentVO;
import com.selfblog.vo.circle.CircleLikeResultVO;

import java.util.List;

/**
 * 校友圈交互服务接口 — 负责点赞、评论、转发
 */
public interface CircleInteractionService {

    /**
     * 点赞/取消点赞
     */
    CircleLikeResultVO toggleLike(Long postId, Long userId);

    /**
     * 检查是否已点赞
     */
    Boolean checkLikeStatus(Long postId, Long userId);

    /**
     * 发表评论
     */
    Long createComment(Long postId, String content, Long parentId, Long replyToUserId, Long userId);

    /**
     * 获取动态评论列表（树形结构）
     */
    List<CircleCommentVO> getComments(Long postId, Long currentUserId);

    /**
     * 删除评论
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 转发动态
     */
    Long repostPost(Long originalPostId, String content, Long userId);

    /**
     * 检查是否已转发
     */
    Boolean checkRepostStatus(Long postId, Long userId);

    /**
     * 浏览量 +1。
     *
     * <p>刻意独立于详情读取：详情是只读事务，GET 请求不应该在只读事务里开写事务；
     * 若在读事务内部用 REQUIRES_NEW 另开一个写事务，当调用方（或测试）已经持有该行的
     * 未提交写锁时，内层写事务会一直等这把锁直到 innodb_lock_wait_timeout（实测 50s）
     * 后抛 CannotAcquireLockException。改为「先读、提交后再单独自增」后不再有跨连接
     * 持锁重叠。调用方应 best-effort 调用：失败不影响详情返回。</p>
     */
    void incrementViewCount(Long postId);
}
