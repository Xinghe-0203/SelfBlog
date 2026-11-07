package com.selfblog.service.site.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.dto.site.TrackVisitorRequest;
import com.selfblog.dto.site.VisitorDailyStat;
import com.selfblog.dto.site.VisitorPathCount;
import com.selfblog.entity.VisitorEvent;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.VisitorEventMapper;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.service.site.VisitorService;
import com.selfblog.utils.LogUtils;
import com.selfblog.utils.VisitorHashUtils;
import com.selfblog.vo.site.VisitorRecordVO;
import com.selfblog.vo.site.VisitorSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VisitorServiceImpl implements VisitorService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final VisitorEventMapper visitorMapper;
    private final BlogPostMapper postMapper;
    private final SiteSettingsService settingsService;

    /**
     * 访客/IP 伪名化密钥（S7）。此前是 {@code ${JWT_SECRET:local-visitor-hash}}：既复用 JWT
     * 签名密钥，又在缺失时静默退化成一个人人可见的常量。现在由
     * {@code app.visitor-hash-secret}（默认回落 JWT_SECRET）提供。
     */
    @Value("${app.visitor-hash-secret:}")
    private String hashSecret;

    public VisitorServiceImpl(VisitorEventMapper visitorMapper, BlogPostMapper postMapper, SiteSettingsService settingsService) {
        this.visitorMapper = visitorMapper;
        this.postMapper = postMapper;
        this.settingsService = settingsService;
    }

    @Override
    @CacheEvict(value = CaffeineCacheConfig.VISITOR_SUMMARY_CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void track(TrackVisitorRequest request, HttpServletRequest httpRequest) {
        if (!settingsService.isVisitorTrackingEnabled()) return;
        String pagePath = cleanPath(request.getPath());
        String clientId = request.getVisitorId().trim();
        if (!clientId.matches("[A-Za-z0-9_-]{8,80}")) throw new BusinessException(400, "访客标识无效");
        // 与限流/审计同源：只用 LogUtils.getClientIp，它在可信代理下解析
        // X-Forwarded-For，否则回退到 remoteAddr。此前直接用 getRemoteAddr()，
        // 反向代理后记录到的是代理地址，与限流计数归属的 IP 不一致。
        String ip = LogUtils.getClientIp(httpRequest);
        String visitorHash = VisitorHashUtils.hash(clientId, hashSecret);

        // Avoid writing duplicate events caused by fast refreshes or duplicate route hooks.
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<VisitorEvent> duplicate = new LambdaQueryWrapper<>();
        duplicate.eq(VisitorEvent::getVisitorHash, visitorHash)
                .eq(VisitorEvent::getPagePath, pagePath)
                .ge(VisitorEvent::getCreateTime, now.minusSeconds(30));
        if (visitorMapper.selectCount(duplicate) > 0) return;

        VisitorEvent event = new VisitorEvent();
        event.setVisitorHash(visitorHash);
        event.setIpHash(VisitorHashUtils.hash(ip, hashSecret));
        event.setUserAgent(trimTo(httpRequest.getHeader("User-Agent"), 500));
        event.setPagePath(pagePath);
        event.setReferrer(cleanReferrer(request.getReferrer()));
        visitorMapper.insert(event);
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.VISITOR_SUMMARY_CACHE, key = "'dashboard'")
    @Transactional(readOnly = true)
    public VisitorSummaryVO getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
        VisitorSummaryVO summary = new VisitorSummaryVO();
        summary.setTotalVisitors(nvl(visitorMapper.countAllVisitors()));
        summary.setTodayVisitors(nvl(visitorMapper.countVisitorsBetween(todayStart, tomorrow)));
        summary.setTotalPageViews(nvl(visitorMapper.selectCount(null)));

        LambdaQueryWrapper<VisitorEvent> todayViews = new LambdaQueryWrapper<>();
        todayViews.ge(VisitorEvent::getCreateTime, todayStart).lt(VisitorEvent::getCreateTime, tomorrow);
        summary.setTodayPageViews(nvl(visitorMapper.selectCount(todayViews)));
        summary.setArticleViews(totalArticleViews());

        summary.setDaily(buildDailyStatistics(today));
        summary.setTopPages(getTopPages());
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<VisitorRecordVO> getRecords(Integer page, Integer pageSize) {
        int current = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Page<VisitorEvent> eventPage = new Page<>(current, size);
        LambdaQueryWrapper<VisitorEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(VisitorEvent::getCreateTime);
        IPage<VisitorEvent> result = visitorMapper.selectPage(eventPage, wrapper);
        IPage<VisitorRecordVO> records = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        records.setRecords(result.getRecords().stream().map(event -> {
            VisitorRecordVO vo = new VisitorRecordVO();
            vo.setId(event.getId());
            vo.setVisitor(event.getVisitorHash().substring(0, 12));
            vo.setNetwork(event.getIpHash().substring(0, 12));
            vo.setUserAgent(event.getUserAgent());
            vo.setPath(event.getPagePath());
            vo.setReferrer(event.getReferrer());
            vo.setTime(event.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return records;
    }

    /**
     * 热门页面 Top8：GROUP BY 下推到数据库，替代「LIMIT 20000 明细行 + 内存 HashMap」。
     * 窗口与旧实现一致（只限下界 30 天前的 0 点，无上界）。值语义唯一的差别：旧实现
     * 超过 20000 行时静默截断少算，新实现始终全量统计（这是修 bug，不是行为差异）；
     * 同浏览量路径之间的先后顺序旧实现依赖 HashMap 迭代序（本就未定义），新实现交给
     * 数据库，同样未定义——测试只断言计数与 Top 集合，不断言并列顺序。
     */
    private List<VisitorSummaryVO.TopPage> getTopPages() {
        LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
        return visitorMapper.selectTopPagePaths(since).stream().map(row -> {
            VisitorSummaryVO.TopPage item = new VisitorSummaryVO.TopPage();
            item.setPath(row.getPath());
            item.setViews(nvl(row.getViews()));
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 文章总浏览量：SUM 聚合下推，替代「全量已发布文章行拉到内存求和」。
     * 过滤条件与旧 wrapper 逐项一致（status=1，is_deleted=0 由逻辑删除过滤显式写出）。
     */
    private long totalArticleViews() {
        return nvl(postMapper.sumPublishedPostViews());
    }

    /**
     * 14 天趋势：一条按天 GROUP BY 的查询替代「14 天 × 2 条 COUNT」。
     * 窗口语义与旧循环完全一致——旧实现第 offset 天是 [当日 0 点, 次日 0 点)，
     * 14 段连续区间拼起来正好是 [today-13 0 点, tomorrow 0 点)，DATE(create_time)
     * 按自然日分桶与逐日窗口逐行等价；缺失的日期补 0，日期轴仍然是 offset 13..0。
     */
    private List<VisitorSummaryVO.Daily> buildDailyStatistics(LocalDate today) {
        LocalDateTime windowStart = today.minusDays(13).atStartOfDay();
        LocalDateTime windowEnd = today.plusDays(1).atStartOfDay();
        Map<String, VisitorDailyStat> byDay = new HashMap<>();
        for (VisitorDailyStat row : visitorMapper.selectDailyStats(windowStart, windowEnd)) {
            String day = dayValue(row.getDay());
            if (day != null) byDay.put(day, row);
        }
        List<VisitorSummaryVO.Daily> daily = new ArrayList<>();
        for (int offset = 13; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            VisitorDailyStat row = byDay.get(date.format(DATE_FORMAT));
            VisitorSummaryVO.Daily item = new VisitorSummaryVO.Daily();
            item.setDate(date.format(DATE_FORMAT));
            item.setVisitors(row == null || row.getVisitors() == null ? 0L : row.getVisitors());
            item.setPageViews(row == null || row.getViews() == null ? 0L : row.getViews());
            daily.add(item);
        }
        return daily;
    }

    /**
     * 日期列跨库取值归一：H2 返回 {@link LocalDate}，MySQL 返回 {@link java.sql.Date}，
     * SQLite 驱动返回字符串（与 {@code TagLastModified} 的 Object 约定一致）。
     * 字符串防御性截到前 10 位（ISO 日期），纯 NULL（create_time 为空的极端行）返回
     * null 并被调用方丢弃——旧实现的窗口比较同样不会统计这类行。
     */
    private String dayValue(Object raw) {
        if (raw == null) return null;
        if (raw instanceof LocalDate date) return date.format(DATE_FORMAT);
        if (raw instanceof java.sql.Date date) return date.toLocalDate().format(DATE_FORMAT);
        if (raw instanceof LocalDateTime dateTime) return dateTime.toLocalDate().format(DATE_FORMAT);
        String text = String.valueOf(raw);
        return text.length() > 10 ? text.substring(0, 10) : text;
    }

    private String cleanPath(String path) {
        String value = path == null ? "/" : path.trim().split("[?#]", 2)[0];
        if (!value.startsWith("/") || value.length() > 255 || value.contains("\\")) {
            throw new BusinessException(400, "页面路径无效");
        }
        return value;
    }

    private String cleanReferrer(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            String path = uri.getPath();
            return trimTo(path == null || path.isBlank() ? uri.getHost() : path, 500);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String trimTo(String value, int maxLength) {
        if (value == null) return null;
        String clean = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return clean.length() > maxLength ? clean.substring(0, maxLength) : clean;
    }

    private long nvl(Number value) {
        return value == null ? 0L : value.longValue();
    }
}
