package com.selfblog.service.social;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.entity.BlogLike;
import com.selfblog.vo.post.LikeItemVO;
import com.selfblog.vo.post.LikeResultVO;
import com.selfblog.vo.post.LikeStatusVO;

import java.util.List;

/**
 * 点赞服务接口
 */
public interface BlogLikeService extends IService<BlogLike> {

    /**
     * 点赞/取消点赞
     * @param postId 文章ID
     * @param userId 用户ID
     * @return 操作结果
     */
    LikeResultVO toggleLike(Long postId, Long userId);

    /**
     * 检查是否已点赞
     * @param postId 文章ID
     * @param userId 用户ID
     * @return 点赞状态
     */
    LikeStatusVO checkLikeStatus(Long postId, Long userId);

    /**
     * 检查用户是否已点赞某文章
     */
    boolean hasLiked(Long postId, Long userId);

    /**
     * 获取我的点赞列表
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    IPage<LikeItemVO> getMyLikes(Long userId, Integer page, Integer pageSize);

    /**
     * 批量检查点赞状态
     * @param postIds 文章ID列表
     * @param userId 用户ID
     * @return 点赞状态列表
     */
    List<Boolean> checkLikeStatusBatch(List<Long> postIds, Long userId);
}
