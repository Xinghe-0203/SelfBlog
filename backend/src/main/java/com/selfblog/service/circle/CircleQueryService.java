package com.selfblog.service.circle;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.entity.CirclePost;
import com.selfblog.vo.circle.CirclePostVO;

import java.util.List;

/**
 * 校友圈查询服务接口 — 负责圈子查询、搜索
 */
public interface CircleQueryService extends IService<CirclePost> {

    /**
     * 获取推荐流
     */
    List<CirclePostVO> getRecommendFeed(int page, int pageSize, Long currentUserId);

    /**
     * 获取关注流
     */
    List<CirclePostVO> getFollowingFeed(int page, int pageSize, Long userId);

    /**
     * 获取动态详情
     */
    CirclePostVO getPostDetail(Long postId, Long currentUserId);

    /**
     * 搜索动态
     */
    List<CirclePostVO> searchPosts(String keyword, int page, int pageSize, Long currentUserId);

    /**
     * 获取话题下的动态列表
     */
    List<CirclePostVO> getPostsByTopic(Long topicId, int page, int pageSize, Long currentUserId);

    /**
     * 获取指定用户的动态列表
     */
    IPage<CirclePostVO> getUserPosts(Long targetUserId, int page, int pageSize, Long currentUserId);
}
