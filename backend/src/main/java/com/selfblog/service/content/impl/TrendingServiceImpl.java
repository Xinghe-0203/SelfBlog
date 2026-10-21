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

    /**
     * C22：热门内容聚合的抓取窗口。两个来源各取这么多条，在内存里合并排序后分页，
     * 因此 total 描述的是「窗口内条数」。窗口必须显式且有硬上限，不能让调用方的
     * pageSize 无限放大单次查询。
     */
    private static final int HOT_CONTENT_FETCH_SIZE = 100;
    private static final int HOT_CONTENT_MAX_FETCH_SIZE = 1000;
    private static final int DEFAULT_HOT_CONTENT_PAGE_SIZE = 10;
    private static final int MAX_HOT_CONTENT_PAGE_SIZE = 100;

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

    /**
     * 热门内容聚合分页。
     *
     * <p>C22 契约：本方法在内存里合并「热门文章 + 热门动态」两个来源，两者都只取一个
     * <b>抓取窗口</b>（{@link #HOT_CONTENT_FETCH_SIZE}，随 pageSize 放大但硬上限
     * {@link #HOT_CONTENT_MAX_FETCH_SIZE}），所以 total 是窗口内条数，不是全库条数。
     * 为保证调用方拿到的分页自洽：pageNum 会被钳制到窗口最后一页，而不是返回一个
     * 「空 records + 非零 total」的幽灵页。</p>
     */
    @Override
    @Cacheable(value = CaffeineCacheConfig.TRENDING_CACHE, key = "'hotContent:' + #pageNum + ':' + #pageSize")
    @Transactional(readOnly = true)
    public IPage<HotContentVO> getHotContent(int pageNum, int pageSize) {
        int safePageSize = pageSize < 1 ? DEFAULT_HOT_CONTENT_PAGE_SIZE : Math.min(pageSize, MAX_HOT_CONTENT_PAGE_SIZE);
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int fetchSize = Math.min(Math.max(safePageSize * 2, HOT_CONTENT_FETCH_SIZE), HOT_CONTENT_MAX_FETCH_SIZE);

        // 1. 获取热门文章（已有热度表）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateStart = now.minusDays(TRENDING_DAYS);
        Page<BlogTrending> articlePage = new Page<>(1, fetchSize);
        IPage<BlogTrending> trendingPage = baseMapper.selectHotPosts(articlePage, dateStart, now);
        List<HotContentVO> articleVOs = convertArticlesToHotContent(trendingPage.getRecords());

        // 2. 获取热门校友圈动态（按互动量排序，限制在最近7天内）
        // C22：不再用 .last("LIMIT " + fetchSize) 拼接 SQL，交给分页插件生成 LIMIT。
        LambdaQueryWrapper<CirclePost> circleWrapper = new LambdaQueryWrapper<>();
        circleWrapper.eq(CirclePost::getStatus, 1)
                .eq(CirclePost::getVisibility, 0)
                .ge(CirclePost::getCreateTime, dateStart)
                .orderByDesc(CirclePost::getLikeCount)
                .orderByDesc(CirclePost::getCommentCount)
                .orderByDesc(CirclePost::getRepostCount);
        IPage<CirclePost> circlePage = circlePostMapper.selectPage(new Page<>(1, fetchSize), circleWrapper);
        List<HotContentVO> circleVOs = convertCirclesToHotContent(circlePage.getRecords());

        // 3. 合并并按热度评分降序排列
        List<HotContentVO> merged = new ArrayList<>(articleVOs);
        merged.addAll(circleVOs);
        merged.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 4. 内存分页：total 只描述窗口，pageNum 钳制到最后一页，避免幽灵页
        int total = merged.size();
        int lastPage = total == 0 ? 1 : (total + safePageSize - 1) / safePageSize;
        int clampedPageNum = Math.min(safePageNum, lastPage);
        int fromIndex = (clampedPageNum - 1) * safePageSize;
        int toIndex = Math.min(fromIndex + safePageSize, total);

        Page<HotContentVO> resultPage = new Page<>(clampedPageNum, safePageSize, total);
        if (fromIndex < total) {
            resultPage.setRecords(new ArrayList<>(merged.subList(fromIndex, toIndex)));
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
        // MySQL/SQLite 的 like_count/comment_count 是 NOT NULL：这里必须显式落 0，不能绑定 null
        trending.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0);
        trending.setCommentCount(post.getCommentCount() != null ? post.getCommentCount() : 0);
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

        while (true) {
            Page<BlogPost> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BlogPost::getIsDeleted, 0);
            wrapper.eq(BlogPost::getStatus, 1); // 只统计已发布的文章

            Page<BlogPost> postPage = blogPostMapper.selectPage(page, wrapper);

            if (postPage.getRecords().isEmpty()) {
                break;
            }

            // 批量写入：原先在循环里对每条文章各发一次 upsert（N 次串行往返）。
            // 这里改为先把整页构造成 BlogTrending 列表，再分块批量 upsert。
            List<BlogTrending> trendings = postPage.getRecords().stream()
                    .map(post -> buildTrending(post, todayStart))
                    .collect(Collectors.toList());
            flushTrendingBatch(trendings);

            if (!postPage.hasNext()) {
                break;
            }
            pageNum++;
        }

        // 更新话题热度分数与动态数（基于关联的已发布文章和校友圈动态）
        // 说明：这两条是全表重算，但只在每天凌晨 00:00 执行一次，且 topicMapper 的语句本身
        // 是单条 UPDATE ... SET col = (相关子查询)，不存在 Java 侧循环，因此保留不降频。
        topicMapper.recalculateAllTrendingScore();
        topicMapper.recalculateAllPostCount();
        log.debug("定时任务: 更新所有话题的热度分数与动态数完成");
    }

    /** 单次批量 upsert 的最大行数：控制 SQL 长度与绑定变量数（SQLite 默认变量上限 999/32766）。 */
    private static final int TRENDING_BATCH_SIZE = 500;

    /**
     * 把整批趋势记录按 {@link #TRENDING_BATCH_SIZE} 分块写入，语义与逐条 upsert 等价
     * （同一篇文章同一天只保留一行；命中唯一键时复位 is_deleted）。
     */
    private void flushTrendingBatch(List<BlogTrending> trendings) {
        for (int from = 0; from < trendings.size(); from += TRENDING_BATCH_SIZE) {
            int to = Math.min(from + TRENDING_BATCH_SIZE, trendings.size());
            baseMapper.upsertBatch(trendings.subList(from, to));
        }
    }

    /**
     * 由文章快照构造当天的趋势记录（不做任何 IO）。
     */
    private BlogTrending buildTrending(BlogPost post, LocalDateTime todayStart) {
        // 计算热度评分（使用 null-safe 取值）
        int score = (post.getViewCount() != null ? post.getViewCount().intValue() : 0) * VIEW_WEIGHT
                + (post.getLikeCount() != null ? post.getLikeCount() : 0) * LIKE_WEIGHT
                + (post.getCommentCount() != null ? post.getCommentCount() : 0) * COMMENT_WEIGHT
                + (post.getShareCount() != null ? post.getShareCount() : 0) * REPOST_WEIGHT;

        BlogTrending trending = new BlogTrending();
        trending.setPostId(post.getId());
        trending.setScore((double)score);
        trending.setViewCount(post.getViewCount() != null ? post.getViewCount().intValue() : 0);
        // MySQL/SQLite 的 like_count/comment_count 是 NOT NULL：这里必须显式落 0，不能绑定 null
        trending.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0);
        trending.setCommentCount(post.getCommentCount() != null ? post.getCommentCount() : 0);
        trending.setStatDate(todayStart.toLocalDate());
        return trending;
    }
}