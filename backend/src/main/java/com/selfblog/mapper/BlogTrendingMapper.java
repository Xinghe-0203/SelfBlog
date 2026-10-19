package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.entity.BlogTrending;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 趋势数据Mapper接口
 */
@Mapper
public interface BlogTrendingMapper extends BaseMapper<BlogTrending> {

    /**
     * 根据文章ID和日期查询趋势记录
     * @param postId 文章ID
     * @param dateStart 日期范围开始
     * @param dateEnd 日期范围结束
     * @return 趋势记录列表
     */
    List<BlogTrending> selectByPostIdAndDate(@Param("postId") Long postId,
                                             @Param("dateStart") LocalDateTime dateStart,
                                             @Param("dateEnd") LocalDateTime dateEnd);

    /**
     * 批量根据文章ID和日期查询趋势记录（优化N+1问题）
     * @param postIds 文章ID列表
     * @param dateStart 日期范围开始
     * @param dateEnd 日期范围结束
     * @return 趋势记录列表
     */
    List<BlogTrending> selectByPostIdsAndDate(@Param("postIds") List<Long> postIds,
                                              @Param("dateStart") LocalDateTime dateStart,
                                              @Param("dateEnd") LocalDateTime dateEnd);

    /**
     * 获取热门文章列表（按热度评分降序）
     * @param page 分页参数
     * @param dateStart 日期范围开始
     * @param dateEnd 日期范围结束
     * @return 分页结果
     */
    IPage<BlogTrending> selectHotPosts(@Param("page") Page<BlogTrending> page,
                                       @Param("dateStart") LocalDateTime dateStart,
                                       @Param("dateEnd") LocalDateTime dateEnd);

    /**
     * 插入或更新趋势记录
     * @param trending 趋势记录
     */
    void upsert(@Param("trending") BlogTrending trending);

    /**
     * 批量插入或更新趋势记录，语义与 {@link #upsert} 完全一致：
     * 同一篇文章同一天只有一行，命中唯一键时把 {@code is_deleted} 复位为 0。
     *
     * <p>用于每日定时任务，把「循环内逐条 IO」收敛成一次往返。
     * @param trendings 趋势记录列表（调用方负责控制单批大小）
     */
    void upsertBatch(@Param("trendings") List<BlogTrending> trendings);

    /**
     * 获取所有文章的最新趋势数据
     * @return 所有趋势记录
     */
    List<BlogTrending> selectAllLatest();
}