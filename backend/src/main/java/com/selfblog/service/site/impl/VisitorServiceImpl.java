package com.selfblog.service.site.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.dto.site.TrackVisitorRequest;
import com.selfblog.entity.BlogPost;
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
import java.util.Comparator;
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

        List<VisitorSummaryVO.Daily> daily = new ArrayList<>();
        for (int offset = 13; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime until = date.plusDays(1).atStartOfDay();
            VisitorSummaryVO.Daily item = new VisitorSummaryVO.Daily();
            item.setDate(date.format(DATE_FORMAT));
            item.setVisitors(nvl(visitorMapper.countVisitorsBetween(from, until)));
            LambdaQueryWrapper<VisitorEvent> views = new LambdaQueryWrapper<>();
            views.ge(VisitorEvent::getCreateTime, from).lt(VisitorEvent::getCreateTime, until);
            item.setPageViews(nvl(visitorMapper.selectCount(views)));
            daily.add(item);
        }
        summary.setDaily(daily);
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

    private List<VisitorSummaryVO.TopPage> getTopPages() {
        LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
        LambdaQueryWrapper<VisitorEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(VisitorEvent::getPagePath).ge(VisitorEvent::getCreateTime, since)
                .last("LIMIT 20000");
        Map<String, Long> counts = new HashMap<>();
        for (VisitorEvent event : visitorMapper.selectList(wrapper)) {
            counts.merge(event.getPagePath(), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(8)
                .map(entry -> {
                    VisitorSummaryVO.TopPage item = new VisitorSummaryVO.TopPage();
                    item.setPath(entry.getKey());
                    item.setViews(entry.getValue());
                    return item;
                }).collect(Collectors.toList());
    }

    private long totalArticleViews() {
        LambdaQueryWrapper<BlogPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogPost::getStatus, 1).ne(BlogPost::getIsDeleted, 1)
                .select(BlogPost::getViewCount);
        return postMapper.selectList(wrapper).stream().mapToLong(post -> post.getViewCount() == null ? 0 : post.getViewCount()).sum();
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
