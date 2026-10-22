package com.selfblog.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.seo.SeoLlmConfig;
import com.selfblog.vo.ai.AiProofreadBatchVO;
import com.selfblog.vo.ai.AiProofreadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * D 校对：单篇校对与批量校对。
 *
 * <p>只读文章正文、只返回问题清单，<b>不修改任何内容</b>。正文超过 6000 字时只检查前
 * 6000 字并把 {@code truncated} 置真，避免超长文章吃掉整个上下文窗口。</p>
 *
 * <p>批量模式串行逐篇调用、逐篇隔离失败：一篇失败只写进 {@code failures}，
 * 不影响其它篇。刻意不加 {@code @Transactional}——批量里会有多次网络 IO，
 * 不能全程占着数据库连接。</p>
 */
@Slf4j
@Service
public class AiProofreadService {

    /** 契约 §5：问题类型。 */
    static final Set<String> TYPES = Set.of("typo", "grammar", "wording", "fact");

    /** 契约 §5：严重程度。 */
    static final Set<String> SEVERITIES = Set.of("low", "medium", "high");

    static final int ISSUES_MAX = 20;
    static final int EXCERPT_MAX = 120;
    static final int SUGGESTION_MAX = 200;

    static final int BATCH_LIMIT_DEFAULT = 5;
    static final int BATCH_LIMIT_MAX = 10;
    static final int BATCH_IDS_MAX = 50;

    private final AiAvailability availability;
    private final AiModelCaller caller;
    private final AiJson json;
    private final AiPromptBuilder prompts;
    private final BlogPostMapper postMapper;

    public AiProofreadService(AiAvailability availability, AiModelCaller caller, AiJson json,
                              AiPromptBuilder prompts, BlogPostMapper postMapper) {
        this.availability = availability;
        this.caller = caller;
        this.json = json;
        this.prompts = prompts;
        this.postMapper = postMapper;
    }

    /** 单篇校对。 */
    public AiProofreadVO proofread(Long postId) {
        SeoLlmConfig config = availability.requireUsable();
        BlogPost post = requirePost(postId);
        return proofread(config, post);
    }

    /**
     * 批量校对。
     *
     * <p>{@code postIds} 为空时自动取最近更新的若干篇已发布文章；给了就按给定顺序处理
     * （去重、跳过空值），再用 {@code limit} 截断。结果里
     * {@code succeeded + failed == requested}。</p>
     */
    public AiProofreadBatchVO proofreadBatch(List<Long> postIds, Integer limit) {
        SeoLlmConfig config = availability.requireUsable();
        int size = normalizeBatchLimit(limit);
        List<Long> targets = resolveTargets(postIds, size);
        List<AiProofreadVO> results = new ArrayList<>();
        List<AiProofreadBatchVO.Failure> failures = new ArrayList<>();
        for (Long postId : targets) {
            try {
                results.add(proofread(config, requirePost(postId)));
            } catch (BusinessException e) {
                AiProofreadBatchVO.Failure failure = new AiProofreadBatchVO.Failure();
                failure.setPostId(postId);
                failure.setReason(e.getMessage());
                failures.add(failure);
                log.warn("[AI] 文章 {} 批量校对失败：{}", postId, e.getMessage());
            }
        }
        AiProofreadBatchVO vo = new AiProofreadBatchVO();
        vo.setRequested(targets.size());
        vo.setSucceeded(results.size());
        vo.setFailed(failures.size());
        vo.setResults(results);
        vo.setFailures(failures);
        return vo;
    }

    private AiProofreadVO proofread(SeoLlmConfig config, BlogPost post) {
        boolean truncated = AiPromptBuilder.isBodyTruncated(post);
        long started = System.nanoTime();
        List<AiProofreadVO.Issue> issues = caller.generate(config,
                AiPromptBuilder.PROOFREAD_SYSTEM_PROMPT, prompts.proofreadUserPrompt(post),
                this::parseIssues);
        AiProofreadVO vo = new AiProofreadVO();
        vo.setPostId(post.getId());
        vo.setIssues(issues);
        vo.setTruncated(truncated);
        vo.setModel(config.model());
        vo.setLatencyMs(AiWritingService.elapsedMs(started));
        return vo;
    }

    private List<AiProofreadVO.Issue> parseIssues(String raw) {
        JsonNode root = json.read(raw);
        JsonNode node = root.get("issues");
        if (node == null) {
            node = root.get("problems");
        }
        List<AiProofreadVO.Issue> issues = new ArrayList<>();
        if (node == null || node.isNull()) {
            return issues;
        }
        if (!node.isArray()) {
            throw new AiOutputException("issues 必须是数组");
        }
        for (JsonNode item : node) {
            if (item == null || !item.isObject()) {
                continue;
            }
            String excerpt = AiJson.plainText(AiJson.text(item, "excerpt"));
            if (excerpt.isEmpty()) {
                excerpt = AiJson.plainText(AiJson.text(item, "original"));
            }
            String suggestion = AiJson.plainText(AiJson.text(item, "suggestion"));
            if (suggestion.isEmpty()) {
                suggestion = AiJson.plainText(AiJson.text(item, "fix"));
            }
            if (excerpt.isEmpty() || suggestion.isEmpty()) {
                continue;
            }
            AiProofreadVO.Issue issue = new AiProofreadVO.Issue();
            issue.setType(normalizeType(AiJson.text(item, "type")));
            issue.setExcerpt(AiJson.truncate(excerpt, EXCERPT_MAX));
            issue.setSuggestion(AiJson.truncate(suggestion, SUGGESTION_MAX));
            issue.setSeverity(normalizeSeverity(AiJson.text(item, "severity")));
            issues.add(issue);
            if (issues.size() >= ISSUES_MAX) {
                break;
            }
        }
        return issues;
    }

    /** 类型不在契约枚举内时归到 {@code wording}（最常见的表达问题），而不是整条丢弃。 */
    private static String normalizeType(String value) {
        String normalized = normalize(value);
        return TYPES.contains(normalized) ? normalized : "wording";
    }

    /** 严重程度缺失或非法时按 {@code low} 处理，避免把提示误报成高危。 */
    private static String normalizeSeverity(String value) {
        String normalized = normalize(value);
        return SEVERITIES.contains(normalized) ? normalized : "low";
    }

    static int normalizeBatchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return BATCH_LIMIT_DEFAULT;
        }
        return Math.min(limit, BATCH_LIMIT_MAX);
    }

    /**
     * 解析待校对文章：显式给 ID 就按给定顺序去重截断，否则取最近更新的已发布文章。
     */
    private List<Long> resolveTargets(List<Long> postIds, int limit) {
        if (postIds != null && !postIds.isEmpty()) {
            LinkedHashSet<Long> unique = new LinkedHashSet<>();
            for (Long postId : postIds) {
                if (postId != null) {
                    unique.add(postId);
                }
            }
            if (unique.isEmpty()) {
                throw new BusinessException(400, "请指定要校对的文章");
            }
            if (unique.size() > BATCH_IDS_MAX) {
                throw new BusinessException(400, "一次最多校对 " + BATCH_IDS_MAX + " 篇文章");
            }
            return unique.stream().limit(limit).toList();
        }
        List<BlogPost> posts = postMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BlogPost>()
                        .select(BlogPost::getId)
                        .eq(BlogPost::getStatus, 1)
                        .orderByDesc(BlogPost::getCreateTime)
                        .last("LIMIT " + limit));
        if (posts == null || posts.isEmpty()) {
            throw new BusinessException(400, "还没有已发布的文章");
        }
        return posts.stream().map(BlogPost::getId).toList();
    }

    private BlogPost requirePost(Long postId) {
        if (postId == null) {
            throw new BusinessException(400, "请指定文章");
        }
        BlogPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        return post;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
