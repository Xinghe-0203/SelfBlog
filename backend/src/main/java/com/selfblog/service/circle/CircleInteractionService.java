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
}
