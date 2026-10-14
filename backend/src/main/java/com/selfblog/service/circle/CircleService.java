package com.selfblog.service.circle;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.entity.CirclePost;
import com.selfblog.vo.circle.CircleCommentVO;
import com.selfblog.vo.circle.CircleLikeResultVO;
import com.selfblog.vo.circle.CirclePostVO;

import java.util.List;

/**
 * 校友圈服务接口
 */
public interface CircleService extends IService<CirclePost> {

    /**
     * 发布动态
     * @param content 内容
     * @param images 图片列表
     * @param videos 视频列表
     * @param location 位置
     * @param repostId 转发来源ID
     * @param userId 发布用户ID
     * @param visibility 可见性：0=公开，1=仅关注者，2=仅自己
     * @param allowComment 是否允许评论：1=允许，0=不允许
     * @param allowRepost 是否允许转发：1=允许，0=不允许
     * @return 创建的动态ID
     */
    Long createPost(String content, List<String> images, List<String> videos, String location, Long repostId,
                    Long userId, Integer visibility, Integer allowComment, Integer allowRepost,
                    List<Long> topicIds);

    /**
     * 删除动态
     * @param postId 动态ID
     * @param userId 操作用户ID
     */
    void deletePost(Long postId, Long userId);

    /**
     * 获取推荐流
     * @param page 页码
     * @param pageSize 每页数量
     * @param currentUserId 当前用户ID（可为空）
     * @return 动态列表
     */
    List<CirclePostVO> getRecommendFeed(int page, int pageSize, Long currentUserId);

    /**
     * 获取关注流
     * @param page 页码
     * @param pageSize 每页数量
     * @param userId 当前用户ID
     * @return 动态列表
     */
    List<CirclePostVO> getFollowingFeed(int page, int pageSize, Long userId);

    /**
     * 获取动态详情
     * @param postId 动态ID
     * @param currentUserId 当前用户ID（可为空）
     * @return 动态详情
     */
    CirclePostVO getPostDetail(Long postId, Long currentUserId);

    // ==================== 点赞相关方法 ====================

    /**
     * 点赞/取消点赞
     * @param postId 动态ID
     * @param userId 用户ID
     * @return 点赞操作结果
     */
    CircleLikeResultVO toggleLike(Long postId, Long userId);

    /**
     * 检查是否已点赞
     * @param postId 动态ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    Boolean checkLikeStatus(Long postId, Long userId);

    // ==================== 评论相关方法 ====================

    /**
     * 发表评论
     * @param postId 动态ID
     * @param content 评论内容
     * @param parentId 父评论ID（回复时使用）
     * @param replyToUserId 回复给的用户ID
     * @param userId 评论用户ID
     * @return 评论ID
     */
    Long createComment(Long postId, String content, Long parentId, Long replyToUserId, Long userId);

    /**
     * 获取动态评论列表（树形结构）
     * @param postId 动态ID
     * @param currentUserId 当前用户ID（用于权限检查）
     * @return 评论列表
     */
    List<CircleCommentVO> getComments(Long postId, Long currentUserId);

    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 操作用户ID
     */
    void deleteComment(Long commentId, Long userId);

    // ==================== 转发相关方法 ====================

    /**
     * 转发动态
     * @param originalPostId 原动态ID
     * @param content 转发时添加的内容
     * @param userId 操作用户ID
     * @return 新动态ID
     */
    /**
     * 更新动态
     * @param postId 动态ID
     * @param content 内容
     * @param images 图片列表
     * @param videos 视频列表
     * @param location 位置
     * @param topicIds 话题ID列表
     * @param visibility 可见性
     * @param allowComment 允许评论
     * @param allowRepost 允许转发
     * @param userId 操作用户ID
     */
    void updatePost(Long postId, String content, List<String> images, List<String> videos,
                    String location, List<Long> topicIds,
                    Integer visibility, Integer allowComment, Integer allowRepost,
                    Long userId);

    Long repostPost(Long originalPostId, String content, Long userId);

    /**
     * 检查是否已转发
     * @param postId 动态ID
     * @param userId 用户ID
     * @return 是否已转发
     */
    Boolean checkRepostStatus(Long postId, Long userId);

    // ==================== 搜索相关方法 ====================

    /**
     * 搜索动态
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页数量
     * @param currentUserId 当前用户ID（可为空）
     * @return 动态列表
     */
    List<CirclePostVO> searchPosts(String keyword, int page, int pageSize, Long currentUserId);

    // ==================== 话题相关方法 ====================

    /**
     * 获取话题下的动态列表
     * @param topicId 话题ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param currentUserId 当前用户ID（可为空）
     * @return 动态列表
     */
    List<CirclePostVO> getPostsByTopic(Long topicId, int page, int pageSize, Long currentUserId);

    /**
     * 获取指定用户的动态列表
     * @param targetUserId 目标用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param currentUserId 当前用户ID（可为空）
     * @return 分页动态列表
     */
    IPage<CirclePostVO> getUserPosts(Long targetUserId, int page, int pageSize, Long currentUserId);
}