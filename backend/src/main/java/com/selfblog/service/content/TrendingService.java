package com.selfblog.service.content;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.vo.content.HotContentVO;
import com.selfblog.vo.content.HotPostVO;
import com.selfblog.vo.content.HotTagVO;

/**
 * 趋势/热门内容服务接口
 */
public interface TrendingService {

    /**
     * 获取热门文章列表（公开接口，支持分页）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 热门文章列表（按热度评分降序）
     */
    IPage<HotPostVO> getHotPosts(int pageNum, int pageSize);

    /**
     * 获取热门内容（文章+动态统一列表，按热度评分降序）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 统一热门内容列表
     */
    IPage<HotContentVO> getHotContent(int pageNum, int pageSize);

    /**
     * 获取热门标签列表（公开接口）
     * @return 热门标签列表（按使用次数降序）
     */
    IPage<HotTagVO> getHotTags();

    /**
     * 更新单篇文章的热度
     * @param postId 文章ID
     */
    void updatePostTrending(Long postId);

    void scheduledUpdateAllTrending();
}