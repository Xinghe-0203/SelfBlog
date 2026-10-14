package com.selfblog.service.content.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.IsDeleted;
import com.selfblog.common.enums.PostStatus;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.*;
import com.selfblog.mapper.*;
import com.selfblog.service.content.TrendingService;
import com.selfblog.vo.content.HotContentVO;
import com.selfblog.vo.content.HotPostVO;
import com.selfblog.vo.content.HotTagVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 趋势/热门内容服务实现类
 */
@Service
public class TrendingServiceImpl extends ServiceImpl<BlogTrendingMapper, BlogTrending> implements TrendingService {

    @Autowired
    private BlogPostMapper blogPostMapper;

    @Autowired
    private BlogTagMapper blogTagMapper;

    @Autowired
    private BlogPostTagMapper blogPostTagMapper;

    @Autowired
    private CirclePostMapper circlePostMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private TopicMapper topicMapper;

    /**
     * 热度计算公式：score = views*1 + likes*5 + comments*10 + reposts*8
     */
    private static final int VIEW_WEIGHT = 1;
    private static final int LIKE_WEIGHT = 5;
    private static final int COMMENT_WEIGHT = 10;
    private static final int REPOST_WEIGHT = 8;

    /**
     * 热门文章默认获取最近7天的数据
     */
    private static final int TRENDING_DAYS = 7;

    @Override
    @Cacheable(value = CaffeineCacheConfig.TRENDING_CACHE, key = "'hotPosts:' + #pageNum + ':' + #pageSize")
    @Transactional(readOnly = true)
    public IPage<HotPostVO> getHotPosts(int pageNum, int pageSize) {
        Page<BlogTrending> page = new Page<>(pageNum, pageSize);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateStart = now.minusDays(TRENDING_DAYS);

        IPage<BlogTrending> trendingPage = baseMapper.selectHotPosts(page, dateStart, now);

        if (trendingPage.getRecords().isEmpty()) {
            Page<HotPostVO> emptyPage = new Page<>(pageNum, pageSize, 0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }

        // 获取文章信息
        List<Long> postIds = trendingPage.getRecords().stream()
                .map(BlogTrending::getPostId)
                .collect(Collectors.toList());

        Map<Long, BlogPost> postMap = blogPostMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(BlogPost::getId, p -> p));

        // 构建响应数据
        List<HotPostVO> result = new ArrayList<>();
        for (BlogTrending trending : trendingPage.getRecords()) {
            BlogPost post = postMap.get(trending.getPostId());
            if (post != null && post.getStatus() == PostStatus.PUBLISHED.getValue() && Objects.equals(post.getIsDeleted(), IsDeleted.NORMAL.getValue())) { // 只返回已发布的文章且未被删除
                HotPostVO vo = new HotPostVO();
                vo.setId(post.getId());
                vo.setTitle(post.getTitle());
                vo.setSummary(post.getSummary());
                vo.setCategory(post.getCategory());
                vo.setViewCount(trending.getViewCount() != null ? trending.getViewCount().longValue() : 0L);
                vo.setLikeCount(trending.getLikeCount());
                vo.setCommentCount(trending.getCommentCount());
                vo.setScore(trending.getScore());
                vo.setCreateTime(post.getCreateTime());
                vo.setCoverImage(post.getCoverUrl());
                result.add(vo);
            }
        }

        // 返回分页结果
        Page<HotPostVO> resultPage = new Page<>(trendingPage.getCurrent(), trendingPage.getSize(), trendingPage.getTotal());
        resultPage.setRecords(result);
        return resultPage;
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.TRENDING_CACHE, key = "'hotContent:' + #pageNum + ':' + #pageSize")
    @Transactional(readOnly = true)
    public IPage<HotContentVO> getHotContent(int pageNum, int pageSize) {
        int fetchSize = Math.max(pageSize * 2, 100);

        // 1. 获取热门文章（已有热度表）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateStart = now.minusDays(TRENDING_DAYS);
        Page<BlogTrending> articlePage = new Page<>(1, fetchSize);
        IPage<BlogTrending> trendingPage = baseMapper.selectHotPosts(articlePage, dateStart, now);
        List<HotContentVO> articleVOs = convertArticlesToHotContent(trendingPage.getRecords());

        // 2. 获取热门校友圈动态（按互动量排序，限制在最近7天内）
        LambdaQueryWrapper<CirclePost> circleWrapper = new LambdaQueryWrapper<>();
        circleWrapper.eq(CirclePost::getStatus, 1)
                .eq(CirclePost::getVisibility, 0)
                .ge(CirclePost::getCreateTime, dateStart)
                .orderByDesc(CirclePost::getLikeCount)
                .orderByDesc(CirclePost::getCommentCount)
                .orderByDesc(CirclePost::getRepostCount)
                .last("LIMIT " + fetchSize);
        List<CirclePost> hotCircles = circlePostMapper.selectList(circleWrapper);
        List<HotContentVO> circleVOs = convertCirclesToHotContent(hotCircles);

        // 3. 合并并按热度评分降序排列
        List<HotContentVO> merged = new ArrayList<>(articleVOs);
        merged.addAll(circleVOs);
        merged.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 4. 内存分页
        int total = merged.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        Page<HotContentVO> resultPage = new Page<>(pageNum, pageSize, total);
        if (fromIndex < merged.size()) {
            resultPage.setRecords(merged.subList(fromIndex, toIndex));
        } else {
            resultPage.setRecords(new ArrayList<>());
        }
        return resultPage;
    }

    private List<HotContentVO> convertArticlesToHotContent(List<BlogTrending> trendings) {
        if (trendings.isEmpty()) return Collections.emptyList();

        List<Long> postIds = trendings.stream()
                .map(BlogTrending::getPostId)
                .collect(Collectors.toList());

        Map<Long, BlogPost> postMap = blogPostMapper.selectBatchIds(postIds).stream()
                .filter(p -> p.getStatus() == PostStatus.PUBLISHED.getValue() && Objects.equals(p.getIsDeleted(), IsDeleted.NORMAL.getValue()))
                .collect(Collectors.toMap(BlogPost::getId, p -> p, (a, b) -> a));

        if (postMap.isEmpty()) return Collections.emptyList();

        // 批量查询用户信息
        Set<Long> userIds = postMap.values().stream()
                .map(BlogPost::getUserId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 批量查询标签
        Map<Long, List<String>> postTagsMap = getPostTagsMap(postIds);

        List<HotContentVO> result = new ArrayList<>();
        for (BlogTrending trending : trendings) {
            BlogPost post = postMap.get(trending.getPostId());
            if (post == null) continue;

            HotContentVO vo = new HotContentVO();
            vo.setId(post.getId());
            vo.setTitle(post.getTitle());
            vo.setContent(post.getSummary());
            vo.setUserId(post.getUserId());

            SysUser user = userMap.get(post.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }

            vo.setType(0);
            vo.setLikeCount(trending.getLikeCount() != null ? trending.getLikeCount().longValue() : 0L);
            vo.setCommentCount(trending.getCommentCount() != null ? trending.getCommentCount().longValue() : 0L);
            vo.setShareCount(post.getShareCount() != null ? post.getShareCount().longValue() : 0L);
            vo.setViewCount(trending.getViewCount() != null ? trending.getViewCount().longValue() : 0L);
            vo.setImages(new ArrayList<>());
            vo.setTags(postTagsMap.getOrDefault(post.getId(), new ArrayList<>()));
            vo.setTopics(new ArrayList<>());
            vo.setCreateTime(post.getCreateTime());
            vo.setScore(trending.getScore());
            result.add(vo);
        }
        return result;
    }

    private List<HotContentVO> convertCirclesToHotContent(List<CirclePost> circles) {
        if (circles.isEmpty()) return Collections.emptyList();

        // 批量查询用户信息
        Set<Long> userIds = circles.stream()
                .map(CirclePost::getUserId)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 批量查询话题名称
        Map<Long, String> topicNameMap = getTopicNames(circles);

        List<HotContentVO> result = new ArrayList<>();
        for (CirclePost circle : circles) {
            int likes = circle.getLikeCount() != null ? circle.getLikeCount() : 0;
            int comments = circle.getCommentCount() != null ? circle.getCommentCount() : 0;
            int reposts = circle.getRepostCount() != null ? circle.getRepostCount() : 0;
            long views = circle.getViewCount() != null ? circle.getViewCount() : 0L;
            double score = views * 1.0 + likes * 5.0 + comments * 10.0 + reposts * 8.0;

            HotContentVO vo = new HotContentVO();
            vo.setId(circle.getId());
            vo.setTitle("");
            vo.setContent(circle.getContent());
            vo.setUserId(circle.getUserId());

            SysUser user = userMap.get(circle.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }

            vo.setType(1);
            vo.setLikeCount((long) likes);
            vo.setCommentCount((long) comments);
            vo.setShareCount((long) reposts);
            vo.setViewCount(circle.getViewCount() != null ? circle.getViewCount() : 0L);

            List<String> images = new ArrayList<>();
            if (StrUtil.isNotBlank(circle.getImageUrls())) {
                images = cn.hutool.json.JSONUtil.toList(circle.getImageUrls(), String.class);
            }
            vo.setImages(images);

            List<String> tags = new ArrayList<>();
            if (StrUtil.isNotBlank(circle.getTags())) {
                tags = cn.hutool.json.JSONUtil.toList(circle.getTags(), String.class);
            }
            vo.setTags(tags);

            List<String> topicNames = new ArrayList<>();
            if (circle.getTopicIds() != null) {
                for (Long topicId : circle.getTopicIds()) {
                    String name = topicNameMap.get(topicId);
                    if (name != null) topicNames.add(name);
                }
            }
            vo.setTopics(topicNames);

            vo.setCreateTime(circle.getCreateTime());
            vo.setScore(score);
            result.add(vo);
        }
        return result;
    }

    private Map<Long, List<String>> getPostTagsMap(List<Long> postIds) {
        if (postIds.isEmpty()) return Collections.emptyMap();

        LambdaQueryWrapper<BlogPostTag> ptWrapper = new LambdaQueryWrapper<>();
        ptWrapper.in(BlogPostTag::getPostId, postIds);
        List<BlogPostTag> postTags = blogPostTagMapper.selectList(ptWrapper);
        if (postTags.isEmpty()) return Collections.emptyMap();

        Set<Long> tagIds = postTags.stream()
                .map(BlogPostTag::getTagId)
                .collect(Collectors.toSet());

        Map<Long, String> tagNameMap = blogTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(BlogTag::getId, BlogTag::getName, (a, b) -> a));

        Map<Long, List<String>> result = new HashMap<>();
        for (BlogPostTag pt : postTags) {
            result.computeIfAbsent(pt.getPostId(), k -> new ArrayList<>())
                    .add(tagNameMap.getOrDefault(pt.getTagId(), ""));
        }
        return result;
    }

    private Map<Long, String> getTopicNames(List<CirclePost> circles) {
        Set<Long> allTopicIds = new HashSet<>();
        for (CirclePost circle : circles) {
            if (circle.getTopicIds() != null) {
                allTopicIds.addAll(circle.getTopicIds());
            }
        }
        if (allTopicIds.isEmpty()) return Collections.emptyMap();

        return topicMapper.selectBatchIds(allTopicIds).stream()
                .collect(Collectors.toMap(Topic::getId, Topic::getName, (a, b) -> a));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CaffeineCacheConfig.HOT_TAGS_CACHE, key = "'hotTags'")
    public IPage<HotTagVO> getHotTags() {
        // 只查询有限的标签（避免全量加载导致OOM），按ID倒序取较新的标签
        LambdaQueryWrapper<BlogTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(BlogTag::getIsDeleted, 0)
                  .orderByDesc(BlogTag::getId)
                  .last("LIMIT 100"); // 最多处理100个标签
        List<BlogTag> limitedTags = blogTagMapper.selectList(tagWrapper);

        if (limitedTags.isEmpty()) {
            Page<HotTagVO> emptyPage = new Page<>(1, 20, 0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }

        // 收集这批标签的ID
        List<Long> tagIds = limitedTags.stream()
                .map(BlogTag::getId)
                .collect(Collectors.toList());

        // 批量统计每个标签关联的文章数量（只统计我们关心的标签）
        LambdaQueryWrapper<BlogPostTag> postTagWrapper = new LambdaQueryWrapper<>();
        postTagWrapper.in(BlogPostTag::getTagId, tagIds);
        List<BlogPostTag> postTags = blogPostTagMapper.selectList(postTagWrapper);

        Map<Long, Long> tagCountMap = postTags.stream()
                .collect(Collectors.groupingBy(BlogPostTag::getTagId, Collectors.counting()));

        // 转换为响应数据，按文章数量降序
        List<HotTagVO> result = limitedTags.stream()
                .map(tag -> {
                    HotTagVO vo = new HotTagVO();
                    vo.setId(tag.getId());
                    vo.setName(tag.getName());
                    vo.setPostCount(tagCountMap.getOrDefault(tag.getId(), 0L));
                    return vo;
                })
                .sorted((a, b) -> Long.compare(b.getPostCount(), a.getPostCount()))
                .collect(Collectors.toList());

        // 取前20个
        List<HotTagVO> top20 = result.size() > 20 ? result.subList(0, 20) : result;

        // 返回分页结果
        Page<HotTagVO> resultPage = new Page<>(1, 20, top20.size());
        resultPage.setRecords(top20);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePostTrending(Long postId) {
        BlogPost post = blogPostMapper.selectById(postId);
        if (post == null || Objects.equals(post.getIsDeleted(), IsDeleted.DELETED.getValue())) {
            return;
        }
        // 仅更新已发布的文章热度
        if (post.getStatus() != 1) {
            return;
        }

        // 计算热度评分（使用 null-safe 取值）
        int score = (post.getViewCount() != null ? post.getViewCount().intValue() : 0) * VIEW_WEIGHT
                + (post.getLikeCount() != null ? post.getLikeCount() : 0) * LIKE_WEIGHT
                + (post.getCommentCount() != null ? post.getCommentCount() : 0) * COMMENT_WEIGHT
                + (post.getShareCount() != null ? post.getShareCount() : 0) * REPOST_WEIGHT;

        // 查询是否已存在今天的趋势记录
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        List<BlogTrending> existingList = baseMapper.selectByPostIdAndDate(postId, todayStart, todayEnd);

        BlogTrending trending = new BlogTrending();
        trending.setPostId(postId);
        trending.setScore((double)score);
        trending.setViewCount(post.getViewCount() != null ? post.getViewCount().intValue() : 0);
        trending.setLikeCount(post.getLikeCount());
        trending.setCommentCount(post.getCommentCount());
        trending.setStatDate(todayStart.toLocalDate());

        baseMapper.upsert(trending);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {CaffeineCacheConfig.TRENDING_CACHE, CaffeineCacheConfig.HOT_TAGS_CACHE}, allEntries = true)
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Shanghai") // 每天凌晨执行
    public void scheduledUpdateAllTrending() {
        // 分页查询未删除的文章，避免一次性加载所有文章导致OOM
        int pageSize = 1000;
        int pageNum = 1;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        while (true) {
            Page<BlogPost> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlogPost::getIsDeleted, 0);
            wrapper.eq(BlogPost::getStatus, 1); // 只统计已发布的文章

            Page<BlogPost> postPage = blogPostMapper.selectPage(page, wrapper);

            if (postPage.getRecords().isEmpty()) {
                break;
            }

            // 批量预加载当天已有的trending记录，减少N+1查询
            List<Long> postIds = postPage.getRecords().stream()
                    .map(BlogPost::getId)
                    .collect(Collectors.toList());
            List<BlogTrending> existingTrendings = baseMapper.selectByPostIdsAndDate(postIds, todayStart, todayEnd);
            Map<Long, BlogTrending> trendingMap = existingTrendings.stream()
                    .collect(Collectors.toMap(BlogTrending::getPostId, t -> t));

            for (BlogPost post : postPage.getRecords()) {
                updatePostTrendingBatch(post, todayStart, trendingMap.get(post.getId()));
            }

            if (!postPage.hasNext()) {
                break;
            }
            pageNum++;
        }

        // 更新话题热度分数与动态数（基于关联的已发布文章和校友圈动态）
        topicMapper.recalculateAllTrendingScore();
        topicMapper.recalculateAllPostCount();
        log.debug("定时任务: 更新所有话题的热度分数与动态数完成");
    }

    private void updatePostTrendingBatch(BlogPost post, LocalDateTime todayStart, BlogTrending existingTrending) {
        // 计算热度评分（使用 null-safe 取值）
        int score = (post.getViewCount() != null ? post.getViewCount().intValue() : 0) * VIEW_WEIGHT
                + (post.getLikeCount() != null ? post.getLikeCount() : 0) * LIKE_WEIGHT
                + (post.getCommentCount() != null ? post.getCommentCount() : 0) * COMMENT_WEIGHT
                + (post.getShareCount() != null ? post.getShareCount() : 0) * REPOST_WEIGHT;

        BlogTrending trending = new BlogTrending();
        trending.setPostId(post.getId());
        trending.setScore((double)score);
        trending.setViewCount(post.getViewCount() != null ? post.getViewCount().intValue() : 0);
        trending.setLikeCount(post.getLikeCount());
        trending.setCommentCount(post.getCommentCount());
        trending.setStatDate(todayStart.toLocalDate());

        baseMapper.upsert(trending);
    }
}