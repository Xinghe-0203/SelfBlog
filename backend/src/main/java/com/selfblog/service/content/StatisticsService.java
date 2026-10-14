package com.selfblog.service.content;

import com.selfblog.vo.content.StatisticsVO;

/**
 * 数据统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取平台完整统计数据
     * @return 平台统计数据
     */
    StatisticsVO getPlatformStatistics();

    /**
     * 获取社区统计数据（公开接口）
     * @return 社区统计数据
     */
    StatisticsVO getCommunityStats();

    /**
     * 清空统计缓存。在管理员执行写操作（创建/删除帖子、用户、评论等）后调用。
     */
    void evictStatsCache();
}
