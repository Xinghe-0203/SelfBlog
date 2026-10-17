package com.selfblog.service.content.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.*;
import com.selfblog.mapper.*;
import com.selfblog.service.content.StatisticsService;
import com.selfblog.vo.content.StatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BlogPostMapper blogPostMapper;

    @Autowired
    private BlogCommentMapper blogCommentMapper;

    @Autowired
    private BlogLikeMapper blogLikeMapper;

    @Autowired
    private BlogCollectMapper blogCollectMapper;

    @Autowired
    private BlogFollowMapper blogFollowMapper;

    @Autowired
    private BlogNotificationMapper notificationMapper;

    @Autowired
    private BlogReportMapper reportMapper;

    @Autowired
    private CirclePostMapper circlePostMapper;

    @Autowired
    private CircleCommentMapper circleCommentMapper;

    @Autowired
    private CircleLikeMapper circleLikeMapper;

    @Autowired
    private CircleRepostMapper circleRepostMapper;

    @Autowired
    private BlogTagMapper blogTagMapper;

    @Autowired
    private TopicMapper topicMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Cacheable(value = CaffeineCacheConfig.STATS_CACHE, key = "'platformStats'", unless = "#result == null")
    @Transactional(readOnly = true)
    public StatisticsVO getPlatformStatistics() {
        StatisticsVO vo = new StatisticsVO();
        StatisticsVO.UserStats userStats = getUserStats();
        StatisticsVO.PostStats postStats = getPostStats();
        StatisticsVO.ReportStats reportStats = getReportStats();
        StatisticsVO.CircleStats circleStats = getCircleStats();
        StatisticsVO.TopicStats topicStats = getTopicStats();

        vo.setUserStats(userStats);
        vo.setPostStats(postStats);
        vo.setEngagementStats(getEngagementStats());
        vo.setCircleStats(circleStats);
        vo.setReportStats(reportStats);
        vo.setTopicStats(topicStats);
        vo.setUserGrowthTrend(getUserGrowthTrend());
        vo.setPostGrowthTrend(getPostGrowthTrend());
        vo.setStatsTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Set flat fields for frontend compatibility
        vo.setUserCount(userStats.getTotalUsers());
        vo.setPostCount(postStats.getTotalPosts());
        vo.setCommentCount(postStats.getTotalComments());
        vo.setPendingReportCount(reportStats.getPendingReports());
        vo.setTagCount(blogTagMapper.countTags());
        vo.setTopicCount(topicStats.getTotalTopics());
        vo.setCirclePostCount(circleStats.getTotalPosts());

        return vo;
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.STATS_CACHE, key = "'communityStats'", unless = "#result == null")
    @Transactional(readOnly = true)
    public StatisticsVO getCommunityStats() {
        StatisticsVO vo = new StatisticsVO();
        StatisticsVO.CircleStats circleStats = getCircleStats();
        StatisticsVO.TopicStats topicStats = getTopicStats();

        vo.setUserStats(getUserStats());
        vo.setPostStats(getPostStats());
        vo.setEngagementStats(getEngagementStats());
        vo.setCircleStats(circleStats);
        vo.setReportStats(getReportStats());
        vo.setTopicStats(topicStats);
        vo.setStatsTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setTagCount(blogTagMapper.countTags());

        // Set flat fields for frontend compatibility
        vo.setUserCount(vo.getUserStats().getTotalUsers());
        vo.setPostCount(vo.getPostStats().getTotalPosts());
        vo.setCommentCount(vo.getPostStats().getTotalComments());
        vo.setPendingReportCount(vo.getReportStats().getPendingReports());
        vo.setTopicCount(topicStats.getTotalTopics());
        vo.setCirclePostCount(circleStats.getTotalPosts());

        return vo;
    }

    private StatisticsVO.UserStats getUserStats() {
        StatisticsVO.UserStats stats = new StatisticsVO.UserStats();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthStart = now.minusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        stats.setTotalUsers(sysUserMapper.selectCount(null));
        LambdaQueryWrapper<SysUser> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(SysUser::getCreateTime, todayStart);
        stats.setTodayNewUsers(sysUserMapper.selectCount(todayWrapper));
        LambdaQueryWrapper<SysUser> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.ge(SysUser::getCreateTime, weekStart);
        stats.setWeekNewUsers(sysUserMapper.selectCount(weekWrapper));
        LambdaQueryWrapper<SysUser> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.ge(SysUser::getCreateTime, monthStart);
        stats.setMonthNewUsers(sysUserMapper.selectCount(monthWrapper));
        stats.setActiveUsers(getActiveUsersThisWeek());
        return stats;
    }

    private Long getActiveUsersThisWeek() {
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        Long postAuthors = blogPostMapper.countDistinctAuthorsSince(weekStart);
        Long commentAuthors = blogCommentMapper.countDistinctAuthorsSince(weekStart);
        Long circleAuthors = circlePostMapper.countDistinctAuthorsSince(weekStart);
        long postCount = postAuthors != null ? postAuthors : 0;
        long commentCount = commentAuthors != null ? commentAuthors : 0;
        long circleCount = circleAuthors != null ? circleAuthors : 0;
        if (postCount == 0 && commentCount == 0) return circleCount;
        if (postCount == 0 && circleCount == 0) return commentCount;
        if (commentCount == 0 && circleCount == 0) return postCount;
        double avgOverlap = (double)(Math.min(postCount, commentCount) + Math.min(postCount, circleCount) + Math.min(commentCount, circleCount)) / 3.0;
        return Math.round(postCount + commentCount + circleCount - avgOverlap);
    }

    private StatisticsVO.PostStats getPostStats() {
        StatisticsVO.PostStats stats = new StatisticsVO.PostStats();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthStart = now.minusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LambdaQueryWrapper<BlogPost> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(BlogPost::getIsDeleted, 0);
        stats.setTotalPosts(blogPostMapper.selectCount(totalWrapper));
        LambdaQueryWrapper<BlogPost> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(BlogPost::getIsDeleted, 0).ge(BlogPost::getCreateTime, todayStart);
        stats.setTodayNewPosts(blogPostMapper.selectCount(todayWrapper));
        LambdaQueryWrapper<BlogPost> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(BlogPost::getIsDeleted, 0).ge(BlogPost::getCreateTime, weekStart);
        stats.setWeekNewPosts(blogPostMapper.selectCount(weekWrapper));
        LambdaQueryWrapper<BlogPost> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(BlogPost::getIsDeleted, 0).ge(BlogPost::getCreateTime, monthStart);
        stats.setMonthNewPosts(blogPostMapper.selectCount(monthWrapper));
        LambdaQueryWrapper<BlogComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(BlogComment::getIsDeleted, 0);
        stats.setTotalComments(blogCommentMapper.selectCount(commentWrapper));
        LambdaQueryWrapper<BlogLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(BlogLike::getIsDeleted, 0);
        stats.setTotalLikes(blogLikeMapper.selectCount(likeWrapper));
        LambdaQueryWrapper<BlogCollect> collectWrapper = new LambdaQueryWrapper<>();
        collectWrapper.eq(BlogCollect::getIsDeleted, 0);
        stats.setTotalCollects(blogCollectMapper.selectCount(collectWrapper));
        return stats;
    }

    private StatisticsVO.EngagementStats getEngagementStats() {
        StatisticsVO.EngagementStats stats = new StatisticsVO.EngagementStats();
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        stats.setTotalFollows(blogFollowMapper.selectCount(null));
        LambdaQueryWrapper<BlogFollow> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(BlogFollow::getCreateTime, todayStart);
        stats.setTodayNewFollows(blogFollowMapper.selectCount(todayWrapper));
        stats.setTotalNotifications(notificationMapper.selectCount(null));
        LambdaQueryWrapper<BlogNotification> unreadWrapper = new LambdaQueryWrapper<>();
        unreadWrapper.eq(BlogNotification::getIsRead, 0);
        stats.setUnreadNotifications(notificationMapper.selectCount(unreadWrapper));
        return stats;
    }

    private StatisticsVO.CircleStats getCircleStats() {
        StatisticsVO.CircleStats stats = new StatisticsVO.CircleStats();
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LambdaQueryWrapper<CirclePost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(CirclePost::getStatus, 1);
        stats.setTotalPosts((long) circlePostMapper.selectCount(postWrapper));
        LambdaQueryWrapper<CirclePost> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(CirclePost::getStatus, 1).ge(CirclePost::getCreateTime, todayStart);
        stats.setTodayNewPosts((long) circlePostMapper.selectCount(todayWrapper));
        LambdaQueryWrapper<CircleComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(CircleComment::getIsDeleted, 0);
        stats.setTotalComments((long) circleCommentMapper.selectCount(commentWrapper));
        LambdaQueryWrapper<CircleComment> todayCommentWrapper = new LambdaQueryWrapper<>();
        todayCommentWrapper.eq(CircleComment::getIsDeleted, 0).ge(CircleComment::getCreateTime, todayStart);
        stats.setTodayNewComments((long) circleCommentMapper.selectCount(todayCommentWrapper));
        LambdaQueryWrapper<CircleLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(CircleLike::getIsDeleted, 0);
        stats.setTotalLikes((long) circleLikeMapper.selectCount(likeWrapper));
        LambdaQueryWrapper<CircleLike> todayLikeWrapper = new LambdaQueryWrapper<>();
        todayLikeWrapper.eq(CircleLike::getIsDeleted, 0).ge(CircleLike::getCreateTime, todayStart);
        stats.setTodayNewLikes((long) circleLikeMapper.selectCount(todayLikeWrapper));
        LambdaQueryWrapper<CircleRepost> repostWrapper = new LambdaQueryWrapper<>();
        repostWrapper.eq(CircleRepost::getIsDeleted, 0);
        stats.setTotalReposts((long) circleRepostMapper.selectCount(repostWrapper));
        return stats;
    }

    private StatisticsVO.ReportStats getReportStats() {
        StatisticsVO.ReportStats stats = new StatisticsVO.ReportStats();
        LocalDateTime monthStart = LocalDateTime.now().minusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LambdaQueryWrapper<BlogReport> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(BlogReport::getStatus, 0);
        stats.setPendingReports(reportMapper.selectCount(pendingWrapper));
        LambdaQueryWrapper<BlogReport> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.ge(BlogReport::getHandleTime, monthStart).ne(BlogReport::getStatus, 0);
        stats.setMonthHandledReports(reportMapper.selectCount(monthWrapper));
        stats.setTotalReports(reportMapper.selectCount(null));
        return stats;
    }

    private StatisticsVO.TopicStats getTopicStats() {
        StatisticsVO.TopicStats stats = new StatisticsVO.TopicStats();
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        stats.setTotalTopics(topicMapper.selectCount(null));
        LambdaQueryWrapper<Topic> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(Topic::getCreateTime, todayStart);
        stats.setTodayNewTopics(topicMapper.selectCount(todayWrapper));
        LambdaQueryWrapper<Topic> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.gt(Topic::getPostCount, 0);
        stats.setActiveTopics(topicMapper.selectCount(activeWrapper));
        return stats;
    }

    private List<StatisticsVO.DailyCount> getUserGrowthTrend() {
        LocalDateTime since = LocalDate.now().minusDays(29).atStartOfDay();
        List<Map<String, Object>> rawData = sysUserMapper.countUsersGroupByDate(since);
        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            String dateStr = row.get("date").toString();
            long count = ((Number) row.get("count")).longValue();
            countMap.put(dateStr, count);
        }
        List<StatisticsVO.DailyCount> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);
            Long count = countMap.get(dateStr);
            StatisticsVO.DailyCount dailyCount = new StatisticsVO.DailyCount();
            dailyCount.setDate(dateStr);
            dailyCount.setCount(count != null ? count : 0L);
            trend.add(dailyCount);
        }
        return trend;
    }

    private List<StatisticsVO.DailyCount> getPostGrowthTrend() {
        LocalDateTime since = LocalDate.now().minusDays(29).atStartOfDay();
        List<Map<String, Object>> rawData = blogPostMapper.countPostsGroupByDate(since);
        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            String dateStr = row.get("date").toString();
            long count = ((Number) row.get("count")).longValue();
            countMap.put(dateStr, count);
        }
        List<StatisticsVO.DailyCount> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);
            Long count = countMap.get(dateStr);
            StatisticsVO.DailyCount dailyCount = new StatisticsVO.DailyCount();
            dailyCount.setDate(dateStr);
            dailyCount.setCount(count != null ? count : 0L);
            trend.add(dailyCount);
        }
        return trend;
    }
}
