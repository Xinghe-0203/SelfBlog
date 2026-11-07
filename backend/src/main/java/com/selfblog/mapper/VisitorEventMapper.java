package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.dto.site.VisitorDailyStat;
import com.selfblog.dto.site.VisitorPathCount;
import com.selfblog.entity.VisitorEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * visitor_event 没有 is_deleted 列（实体/建表 SQL 均无），手写 SQL 无需软删除过滤。
 * DATE(create_time) 在 MySQL/SQLite/H2(MySQL 兼容模式) 三库均可执行——与
 * {@code BlogPostMapper.xml#countPostsGroupByDate} 的共享语句同一写法；
 * ORDER BY 用聚合表达式而不是别名，避免 MySQL 在非 ANSI_QUOTES 模式下把
 * 双引号别名当成字符串常量导致排序失效。
 */
@Mapper
public interface VisitorEventMapper extends BaseMapper<VisitorEvent> {
    @Select("SELECT COUNT(DISTINCT visitor_hash) FROM visitor_event")
    Long countAllVisitors();

    @Select("SELECT COUNT(DISTINCT visitor_hash) FROM visitor_event WHERE create_time >= #{since} AND create_time < #{until}")
    Long countVisitorsBetween(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    /** 热门页面 Top8：开区间窗口与旧实现一致（只限下界 create_time >= since，无上界）。 */
    @Select("SELECT page_path AS \"path\", COUNT(*) AS \"views\" FROM visitor_event "
            + "WHERE create_time >= #{since} GROUP BY page_path ORDER BY COUNT(*) DESC LIMIT 8")
    List<VisitorPathCount> selectTopPagePaths(@Param("since") LocalDateTime since);

    /** 每日独立访客数与浏览量，窗口 [since, until) 与旧的逐日 COUNT 循环完全一致。 */
    @Select("SELECT DATE(create_time) AS \"day\", COUNT(DISTINCT visitor_hash) AS \"visitors\", COUNT(*) AS \"views\" "
            + "FROM visitor_event WHERE create_time >= #{since} AND create_time < #{until} "
            + "GROUP BY DATE(create_time)")
    List<VisitorDailyStat> selectDailyStats(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);
}
