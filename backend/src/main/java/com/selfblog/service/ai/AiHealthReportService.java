package com.selfblog.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.post.PostQueryService;
import com.selfblog.service.seo.SeoLlmConfig;
import com.selfblog.vo.ai.AiHealthReportVO;
import com.selfblog.vo.post.PostDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * F 站点内容体检。
 *
 * <p>只读体检：采样已发布文章的<b>元数据</b>（标题/摘要/分类/标签/发布时间/字数，不含正文），
 * 一次性交给模型评估。<b>单次调用不逐篇</b>——逐篇体检会让 40 篇文章变成 40 次网络请求。</p>
 *
 * <p>刻意不加 {@code @Transactional}：模型调用是跨网络 IO，不能占着数据库连接。</p>
 */
@Slf4j
@Service
public class AiHealthReportService {

    static final int LIMIT_DEFAULT = 40;
    static final int LIMIT_MAX = 80;

    static final int SUMMARY_MIN = 100;
    static final int SUMMARY_MAX = 300;
    static final int FINDINGS_MIN = 3;
    static final int FINDINGS_MAX = 8;
    static final int SCORE_MIN = 0;
    static final int SCORE_MAX = 100;

    static final String NO_PUBLISHED_MESSAGE = "还没有已发布的文章";

    private static final Set<String> SEVERITIES = Set.of("low", "medium", "high");

    private final AiAvailability availability;
    private final AiModelCaller caller;
    private final AiJson json;
    private final AiPromptBuilder prompts;
    private final BlogPostMapper postMapper;
    private final PostQueryService postQueryService;

    public AiHealthReportService(AiAvailability availability, AiModelCaller caller, AiJson json,
                                 AiPromptBuilder prompts, BlogPostMapper postMapper,
                                 PostQueryService postQueryService) {
        this.availability = availability;
        this.caller = caller;
        this.json = json;
        this.prompts = prompts;
        this.postMapper = postMapper;
        this.postQueryService = postQueryService;
    }

    /** 生成体检报告。 */
    public AiHealthReportVO report(Integer limit) {
        SeoLlmConfig config = availability.requireUsable();
        int size = normalizeLimit(limit);
        int total = countPublished();
        if (total == 0) {
            throw new BusinessException(400, NO_PUBLISHED_MESSAGE);
        }
        List<BlogPost> sampled = samplePublished(size);
        String material = buildMaterial(sampled);
        long started = System.nanoTime();
        AiHealthReportVO vo = caller.generate(config, AiPromptBuilder.HEALTH_SYSTEM_PROMPT,
                prompts.healthUserPrompt(material), this::parseReport);
        vo.setGeneratedAt(LocalDateTime.now().toString());
        vo.setSampledPosts(sampled.size());
        vo.setTotalPosts(total);
        vo.setModel(config.model());
        vo.setLatencyMs(AiWritingService.elapsedMs(started));
        return vo;
    }

    private AiHealthReportVO parseReport(String raw) {
        JsonNode root = json.read(raw);
        Integer score = AiJson.integer(root, "score");
        if (score == null) {
            throw new AiOutputException("缺少 score 字段");
        }
        AiHealthReportVO vo = new AiHealthReportVO();
        vo.setScore(Math.max(SCORE_MIN, Math.min(SCORE_MAX, score)));
        String summary = AiJson.plainText(AiJson.text(root, "summary"));
        if (summary.isEmpty()) {
            throw new AiOutputException("缺少 summary 字段");
        }
        if (summary.length() < SUMMARY_MIN) {
            throw new AiOutputException("summary 太短（" + summary.length() + " 字，要求 "
                    + SUMMARY_MIN + "-" + SUMMARY_MAX + " 字）");
        }
        vo.setSummary(AiJson.truncate(summary, SUMMARY_MAX));
        vo.setFindings(parseFindings(root.get("findings")));
        return vo;
    }

    private List<AiHealthReportVO.Finding> parseFindings(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            throw new AiOutputException("findings 必须是数组");
        }
        List<AiHealthReportVO.Finding> findings = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || !item.isObject()) {
                continue;
            }
            String title = AiJson.plainText(AiJson.text(item, "title"));
            String detail = AiJson.plainText(AiJson.text(item, "detail"));
            if (title.isEmpty() || detail.isEmpty()) {
                continue;
            }
            AiHealthReportVO.Finding finding = new AiHealthReportVO.Finding();
            finding.setTitle(AiJson.truncate(title, 60));
            finding.setDetail(AiJson.truncate(detail, 300));
            String severity = normalize(item, "severity");
            finding.setSeverity(SEVERITIES.contains(severity) ? severity : "low");
            String action = AiJson.plainText(AiJson.text(item, "action"));
            finding.setAction(action.isEmpty() ? null : AiJson.truncate(action, 200));
            finding.setCount(AiJson.integer(item, "count"));
            findings.add(finding);
            if (findings.size() >= FINDINGS_MAX) {
                break;
            }
        }
        if (findings.size() < FINDINGS_MIN) {
            throw new AiOutputException("只给出了 " + findings.size() + " 条发现，要求至少 "
                    + FINDINGS_MIN + " 条");
        }
        return findings;
    }

    private static String normalize(JsonNode node, String field) {
        String value = AiJson.text(node, field);
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /** 只取元数据列，正文用于算字数但不进提示词之外的用途。 */
    private List<BlogPost> samplePublished(int limit) {
        List<BlogPost> posts = postMapper.selectList(
                new LambdaQueryWrapper<BlogPost>()
                        .eq(BlogPost::getStatus, 1)
                        .orderByDesc(BlogPost::getCreateTime)
                        .last("LIMIT " + limit));
        return posts == null ? List.of() : posts;
    }

    private int countPublished() {
        Long count = postMapper.selectCount(
                new LambdaQueryWrapper<BlogPost>().eq(BlogPost::getStatus, 1));
        return count == null ? 0 : count.intValue();
    }

    /** 拼体检素材：每篇一行，不含正文全文（只含字数）。 */
    private String buildMaterial(List<BlogPost> posts) {
        List<Long> ids = posts.stream().map(BlogPost::getId).toList();
        java.util.Map<Long, List<PostDetailResponse.TagVO>> tags;
        try {
            tags = postQueryService.getTagsMapByPostIds(ids);
        } catch (Exception e) {
            log.warn("[AI] 读取文章标签失败，体检素材将缺少标签信息：{}", e.getClass().getSimpleName());
            tags = java.util.Map.of();
        }
        StringBuilder sb = new StringBuilder();
        for (BlogPost post : posts) {
            List<String> names = tags.getOrDefault(post.getId(), List.of()).stream()
                    .map(tag -> tag == null ? null : tag.getName())
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
            sb.append(AiPromptBuilder.healthRow(post, names)).append('\n');
        }
        return sb.toString();
    }

    static int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return LIMIT_DEFAULT;
        }
        return Math.min(limit, LIMIT_MAX);
    }
}
