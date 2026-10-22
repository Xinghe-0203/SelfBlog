package com.selfblog.service.seo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.enums.PostStatus;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.PostSeo;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.PostSeoMapper;
import com.selfblog.vo.seo.SeoDetailVO;
import com.selfblog.vo.seo.SeoOverviewVO;
import com.selfblog.vo.seo.SeoPostRowVO;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SEO 体检（只读）：概览计数、列表分页、单篇详情。
 *
 * <p><b>完全不调用模型</b>，所有问题判定都只依赖 {@code blog_post} 与 {@code post_seo}。
 * 数据量按「单人博客」量级设计：一次取全部已发布文章的元数据列 + 一次取全部 SEO 记录，
 * 之后全部在内存里算 —— 这样是固定的 2 条查询，没有 N+1，也不需要为
 * {@code CHAR_LENGTH}/{@code LENGTH}、{@code GROUP BY} 之类的方言差异写两套 SQL
 * （H2 没有 {@code databaseId} 映射，任何 MySQL 专有写法在测试里都不会被执行）。</p>
 *
 * <p>刻意<b>不</b>加载正文：列表与概览只需要标题、摘要、封面与 SEO 字段，
 * 正文是 CLOB，带进来会让一次列表查询搬运大量无用数据。单篇详情才读正文。</p>
 */
@Service
public class SeoAuditService {

    /** 前台「建议不超过」的标题长度（站点名后缀不计入）。 */
    static final int TITLE_LIMIT = 60;

    private final BlogPostMapper blogPostMapper;
    private final PostSeoMapper postSeoMapper;
    private final SeoLlmConfigProvider configProvider;

    public SeoAuditService(BlogPostMapper blogPostMapper, PostSeoMapper postSeoMapper,
                           SeoLlmConfigProvider configProvider) {
        this.blogPostMapper = blogPostMapper;
        this.postSeoMapper = postSeoMapper;
        this.configProvider = configProvider;
    }

    @Transactional(readOnly = true)
    public SeoOverviewVO overview() {
        List<BlogPost> published = publishedMetadata();
        Map<Long, PostSeo> seoByPost = seoByPostId();
        Set<String> duplicated = duplicatedTitles(published);

        SeoOverviewVO overview = new SeoOverviewVO();
        overview.setTotalPosts(published.size());
        int withSeo = 0;
        int missingDescription = 0;
        int longTitle = 0;
        int duplicateTitle = 0;
        int missingCover = 0;
        for (BlogPost post : published) {
            PostSeo seo = seoByPost.get(post.getId());
            boolean hasSeo = hasSeo(seo);
            if (hasSeo) withSeo++;
            if (!hasDescription(seo, post)) missingDescription++;
            if (effectiveTitle(seo, post).length() > TITLE_LIMIT) longTitle++;
            if (isDuplicateTitle(post, duplicated)) duplicateTitle++;
            if (!hasCover(seo, post)) missingCover++;
        }
        overview.setWithSeo(withSeo);
        overview.setCoveragePercent(published.isEmpty()
                ? 0 : (int) Math.round(withSeo * 100.0 / published.size()));
        overview.setMissingDescription(missingDescription);
        overview.setLongTitle(longTitle);
        overview.setDuplicateTitle(duplicateTitle);
        overview.setMissingCover(missingCover);

        SeoLlmConfig config = configProvider.current();
        overview.setLlmConfigured(config.configured());
        overview.setLlmEnabled(config.enabled());
        overview.setAutoOnPublish(config.autoOnPublish());
        overview.setModel(config.model() == null ? "" : config.model());
        return overview;
    }

    /**
     * 列表分页。
     *
     * @param issue 只保留含该问题的文章；{@code null} 表示不按问题过滤
     */
    @Transactional(readOnly = true)
    public Page<SeoPostRowVO> page(int pageNum, int pageSize, String keyword, SeoIssue issue) {
        List<BlogPost> published = publishedMetadata();
        Map<Long, PostSeo> seoByPost = seoByPostId();
        Set<String> duplicated = duplicatedTitles(published);

        // 契约排序：create_time desc, id desc。create_time 允许为空，空值排最后。
        List<BlogPost> ordered = new ArrayList<>(published);
        ordered.sort(Comparator
                .comparing(BlogPost::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BlogPost::getId, Comparator.reverseOrder()));

        String needle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<SeoPostRowVO> rows = new ArrayList<>();
        for (BlogPost post : ordered) {
            if (!needle.isEmpty() && !nullToEmpty(post.getTitle()).toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            PostSeo seo = seoByPost.get(post.getId());
            SeoPostRowVO row = toRow(post, seo, duplicated);
            if (issue != null && !row.getIssues().contains(issue.code())) {
                continue;
            }
            rows.add(row);
        }

        int total = rows.size();
        int from = (int) Math.min((long) (pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        Page<SeoPostRowVO> result = new Page<>(pageNum, pageSize, total);
        result.setRecords(new ArrayList<>(rows.subList(from, to)));
        return result;
    }

    /**
     * 单篇详情。文章不存在或还没有 SEO 记录时都抛 404。
     *
     * <p>没有 SEO 记录时返回 404 而不是「200 + 全 null」，是前端
     * {@code PostEdit.vue} 的既定用法：它把 404 当作「尚未填写」，其它状态码才算错误。</p>
     */
    @Transactional(readOnly = true)
    public SeoDetailVO detail(Long postId) {
        BlogPost post = requirePost(postId);
        PostSeo seo = postSeoMapper.selectById(postId);
        if (!hasSeo(seo)) {
            throw new BusinessException(404, "该文章还没有 SEO 元数据");
        }
        return buildDetail(post, seo, duplicatedTitles(publishedMetadata()));
    }

    /**
     * 与 {@link #detail(Long)} 相同，但没有 SEO 记录时返回「全 null 的合法详情」而不是 404。
     *
     * <p>用于写接口的响应：保存/生成/清除之后前端需要完整详情对象来回填表单，
     * 此时「记录不存在」是正常结果，不是错误。</p>
     */
    @Transactional(readOnly = true)
    public SeoDetailVO detailOrDerived(Long postId) {
        BlogPost post = requirePost(postId);
        PostSeo seo = postSeoMapper.selectById(postId);
        return buildDetail(post, seo, duplicatedTitles(publishedMetadata()));
    }

    /** 文章存在性校验（软删除的行对 MyBatis-Plus 不可见，自动算不存在）。 */
    public BlogPost requirePost(Long postId) {
        if (postId == null) {
            throw new BusinessException(404, "文章不存在");
        }
        BlogPost post = blogPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        return post;
    }

    // ------------------------------------------------------------------
    // 供写侧复用的判定/映射（同一套规则，避免概览与列表口径漂移）
    // ------------------------------------------------------------------

    /** 已发布文章的元数据列（不含正文）。 */
    List<BlogPost> publishedMetadata() {
        return blogPostMapper.selectList(new LambdaQueryWrapper<BlogPost>()
                .select(BlogPost::getId, BlogPost::getTitle, BlogPost::getSummary,
                        BlogPost::getCoverUrl, BlogPost::getStatus, BlogPost::getCreateTime)
                .eq(BlogPost::getStatus, PostStatus.PUBLISHED.getValue()));
    }

    Map<Long, PostSeo> seoByPostId() {
        Map<Long, PostSeo> map = new HashMap<>();
        for (PostSeo row : postSeoMapper.selectList(null)) {
            map.put(row.getPostId(), row);
        }
        return map;
    }

    /**
     * 标题重复集合：只统计已发布文章里出现两次及以上的标题。
     *
     * <p>概览的 {@code duplicateTitle} 与列表行的 {@code duplicateTitle} 都来自这里，
     * 保证「总数」和「逐篇标记」永远一致。</p>
     */
    Set<String> duplicatedTitles(List<BlogPost> published) {
        Map<String, Integer> counts = new HashMap<>();
        for (BlogPost post : published) {
            String title = nullToEmpty(post.getTitle()).trim();
            if (!title.isEmpty()) {
                counts.merge(title, 1, Integer::sum);
            }
        }
        Set<String> duplicated = new HashSet<>();
        counts.forEach((title, count) -> {
            if (count > 1) duplicated.add(title);
        });
        return duplicated;
    }

    SeoPostRowVO toRow(BlogPost post, PostSeo seo, Set<String> duplicated) {
        SeoPostRowVO row = new SeoPostRowVO();
        row.setPostId(post.getId());
        row.setTitle(post.getTitle());
        row.setStatus(post.getStatus());
        row.setHasSeo(hasSeo(seo));
        row.setSeoTitle(blankToNull(seo == null ? null : seo.getSeoTitle()));
        row.setSeoDescription(blankToNull(seo == null ? null : seo.getSeoDescription()));
        row.setSeoKeywords(blankToNull(seo == null ? null : seo.getSeoKeywords()));
        row.setOgImageUrl(blankToNull(seo == null ? null : seo.getOgImageUrl()));
        row.setSource(blankToNull(seo == null ? null : seo.getSource()));
        row.setGeneratedAt(time(seo == null ? null : seo.getGeneratedAt()));
        row.setUpdateTime(time(seo == null ? null : seo.getUpdateTime()));
        row.setIssues(issues(post, seo, duplicated));
        return row;
    }

    List<String> issues(BlogPost post, PostSeo seo, Set<String> duplicated) {
        List<String> issues = new ArrayList<>(4);
        if (!hasSeo(seo)) {
            issues.add(SeoIssue.NO_SEO.code());
        }
        if (!hasDescription(seo, post)) {
            issues.add(SeoIssue.MISSING_DESCRIPTION.code());
        }
        if (effectiveTitle(seo, post).length() > TITLE_LIMIT) {
            issues.add(SeoIssue.LONG_TITLE.code());
        }
        if (isDuplicateTitle(post, duplicated)) {
            issues.add(SeoIssue.DUPLICATE_TITLE.code());
        }
        if (!hasCover(seo, post)) {
            issues.add(SeoIssue.MISSING_COVER.code());
        }
        return issues;
    }

    /** 「有 SEO」= 有记录且四个字段至少一个非空（空记录不算覆盖率）。 */
    static boolean hasSeo(PostSeo seo) {
        if (seo == null) return false;
        return isPresent(seo.getSeoTitle()) || isPresent(seo.getSeoDescription())
                || isPresent(seo.getSeoKeywords()) || isPresent(seo.getOgImageUrl());
    }

    /**
     * 「缺描述」= 没有自定义描述且文章本身也没有摘要。
     *
     * <p>严格来说前台永远有描述（会截正文前 140 字），但那种自动截断正是要提醒作者补的，
     * 所以体检把它算作缺失。</p>
     */
    static boolean hasDescription(PostSeo seo, BlogPost post) {
        return isPresent(seo == null ? null : seo.getSeoDescription()) || isPresent(post.getSummary());
    }

    /** 「缺分享图」= 既没有自定义分享图也没有文章封面（站点 Logo 兜底不算数）。 */
    static boolean hasCover(PostSeo seo, BlogPost post) {
        return isPresent(seo == null ? null : seo.getOgImageUrl()) || isPresent(post.getCoverUrl());
    }

    static String effectiveTitle(PostSeo seo, BlogPost post) {
        return nullToEmpty(SeoFallback.title(seo == null ? null : seo.getSeoTitle(), post.getTitle()));
    }

    static boolean isDuplicateTitle(BlogPost post, Set<String> duplicated) {
        String title = nullToEmpty(post.getTitle()).trim();
        return !title.isEmpty() && duplicated.contains(title);
    }

    private SeoDetailVO buildDetail(BlogPost post, PostSeo seo, Set<String> duplicated) {
        SeoDetailVO detail = new SeoDetailVO();
        SeoPostRowVO row = toRow(post, seo, duplicated);
        detail.setPostId(row.getPostId());
        detail.setTitle(row.getTitle());
        detail.setStatus(row.getStatus());
        detail.setHasSeo(row.isHasSeo());
        detail.setSeoTitle(row.getSeoTitle());
        detail.setSeoDescription(row.getSeoDescription());
        detail.setSeoKeywords(row.getSeoKeywords());
        detail.setOgImageUrl(row.getOgImageUrl());
        detail.setSource(row.getSource());
        detail.setGeneratedAt(row.getGeneratedAt());
        detail.setUpdateTime(row.getUpdateTime());
        detail.setIssues(row.getIssues());
        detail.setPostSummary(blankToNull(post.getSummary()));
        String plainBody = post.getContent() == null ? null : Jsoup.parse(post.getContent()).text();
        detail.setEffectiveTitle(effectiveTitle(seo, post));
        detail.setEffectiveDescription(nullToEmpty(SeoFallback.description(
                seo == null ? null : seo.getSeoDescription(), post.getSummary(), plainBody)));
        return detail;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    static String time(LocalDateTime value) {
        return value == null ? null : value.toString();
    }
}
