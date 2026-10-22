package com.selfblog.service.seo;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.seo.SeoBatchRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.PostSeo;
import com.selfblog.mapper.PostSeoMapper;
import com.selfblog.service.post.PostQueryService;
import com.selfblog.service.seo.SeoGenerationParser.SeoDraft;
import com.selfblog.vo.post.PostDetailResponse;
import com.selfblog.vo.seo.SeoBatchResultVO;
import com.selfblog.vo.seo.SeoDetailVO;
import com.selfblog.vo.seo.SeoLlmTestVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SEO 元数据的生成与持久化（模型调用 + {@code post_seo} 写入）。
 *
 * <p><b>事务边界：</b>本类刻意<b>不加</b> {@code @Transactional}。一次模型调用可能
 * 持续数十秒，把数据库事务跨越网络 IO 会长期占住连接、并在 MySQL 上放大锁等待风险。
 * 每条写操作都是单行 upsert，自身就是一个原子语句，不需要外层事务。</p>
 *
 * <p><b>失败语义：</b>「没配置」是 400（管理员操作错误），「上游失败」是 502
 * （外部依赖问题）。所有对外消息都不含 API Key 与上游原文，细节只进日志。</p>
 */
@Slf4j
@Service
public class SeoGenerationService {

    /** 单次批量最多处理的篇数（一次请求内串行发起同样多的上游调用）。 */
    public static final int BATCH_LIMIT_MAX = 50;
    /** 批量默认处理的篇数。 */
    public static final int BATCH_LIMIT_DEFAULT = 10;
    /** 显式传 postIds 时的上限。 */
    public static final int BATCH_IDS_MAX = 50;

    private final SeoLlmConfigProvider configProvider;
    private final OpenAiCompatibleLlmClient client;
    private final SeoPromptBuilder prompts;
    private final SeoGenerationParser parser;
    private final PostSeoMapper postSeoMapper;
    private final PostQueryService postQueryService;
    private final SeoAuditService audit;

    public SeoGenerationService(SeoLlmConfigProvider configProvider, OpenAiCompatibleLlmClient client,
                                SeoPromptBuilder prompts, SeoGenerationParser parser,
                                PostSeoMapper postSeoMapper, PostQueryService postQueryService,
                                SeoAuditService audit) {
        this.configProvider = configProvider;
        this.client = client;
        this.prompts = prompts;
        this.parser = parser;
        this.postSeoMapper = postSeoMapper;
        this.postQueryService = postQueryService;
        this.audit = audit;
    }

    /**
     * 为单篇文章生成并保存 SEO 元数据。
     *
     * @throws BusinessException 400 未启用/未配置，404 文章不存在，502 上游失败
     */
    public SeoDetailVO generate(Long postId) {
        BlogPost post = audit.requirePost(postId);
        SeoLlmConfig config = configProvider.current();
        requireUsable(config);
        PostDetailResponse detail = loadDetail(post);
        SeoDraft draft = callModel(config, detail);
        save(postId, draft, "llm");
        return audit.detailOrDerived(postId);
    }

    /**
     * 异步触发用：不抛任何异常，失败只记 WARN。
     *
     * <p>发布流程调用它，因此这里必须吞掉一切 —— 生成失败绝不能让文章保存失败，
     * 也不能把异常抛进线程池。</p>
     */
    public void generateQuietly(Long postId) {
        try {
            SeoLlmConfig config = configProvider.current();
            if (!config.autoEnabled()) {
                // 开关在入队后可能被关掉，这里再判一次，避免无谓的上游调用。
                return;
            }
            BlogPost post = audit.requirePost(postId);
            if (post.getStatus() == null || post.getStatus() != 1) {
                return;
            }
            PostDetailResponse detail = loadDetail(post);
            SeoDraft draft = callModel(config, detail);
            save(postId, draft, "llm");
            log.info("[SEO] 发布后自动生成完成：postId={}", postId);
        } catch (Exception e) {
            log.warn("[SEO] 发布后自动生成失败，已回落到派生 SEO：postId={}, error={}", postId, safeReason(e));
        }
    }

    /**
     * 批量生成：串行，逐篇隔离失败。
     *
     * <p>串行而不是并发：多数 OpenAI 兼容服务的免费/低档配额对并发非常敏感，
     * 并发只会把「部分 429」变成「大部分 429」，而管理员真正需要的是「多试几次总能补完」。</p>
     */
    public SeoBatchResultVO batch(SeoBatchRequest request) {
        SeoLlmConfig config = configProvider.current();
        requireUsable(config);

        List<Long> candidates = resolveCandidates(request);
        boolean onlyMissing = Boolean.TRUE.equals(request.getOnlyMissing());
        SeoBatchResultVO result = new SeoBatchResultVO();
        List<SeoBatchResultVO.Failure> failures = new ArrayList<>();

        for (Long postId : candidates) {
            BlogPost post;
            try {
                post = audit.requirePost(postId);
            } catch (BusinessException e) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            if (post.getStatus() == null || post.getStatus() != 1) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            if (onlyMissing && SeoAuditService.hasSeo(postSeoMapper.selectById(postId))) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            result.setRequested(result.getRequested() + 1);
            try {
                SeoDraft draft = callModel(config, loadDetail(post));
                save(postId, draft, "llm");
                result.setSucceeded(result.getSucceeded() + 1);
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                failures.add(new SeoBatchResultVO.Failure(postId, safeReason(e)));
                log.warn("[SEO] 批量生成单篇失败：postId={}, error={}", postId, safeReason(e));
            }
        }
        result.setFailures(failures);
        return result;
    }

    /** 连通性测试：永不抛异常，失败折成 {@code ok=false}。 */
    public SeoLlmTestVO testConnection() {
        SeoLlmTestVO vo = new SeoLlmTestVO();
        SeoLlmConfig config = configProvider.current();
        vo.setModel(config.model() == null ? "" : config.model());
        if (!config.configured()) {
            vo.setOk(false);
            vo.setLatencyMs(0L);
            vo.setMessage("尚未配置接口地址或 API Key，请先在设置中填写。");
            return vo;
        }
        long started = System.nanoTime();
        try {
            OpenAiCompatibleLlmClient.ProbeResult probe = client.probe(config);
            vo.setLatencyMs((System.nanoTime() - started) / 1_000_000L);
            vo.setOk(probe.ok());
            vo.setMessage(probe.ok() ? "连接成功。" : "连接成功但没有返回内容，请确认模型名是否正确。");
        } catch (Exception e) {
            vo.setLatencyMs((System.nanoTime() - started) / 1_000_000L);
            vo.setOk(false);
            vo.setMessage(truncate(redact(safeReason(e), config.apiKey()), 200));
        }
        return vo;
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private void requireUsable(SeoLlmConfig config) {
        if (!config.enabled()) {
            throw new BusinessException(400, "AI 辅助功能已关闭，请先在设置中启用");
        }
        if (!config.configured()) {
            throw new BusinessException(400, "尚未配置模型，请先在设置中填写接口地址与 API Key");
        }
    }

    private PostDetailResponse loadDetail(BlogPost post) {
        // 走查询服务而不是自己拼：标签、分类、昵称等提示词素材都在它里面组装。
        return postQueryService.getPostDetail(post.getId());
    }

    /** 一次模型调用 + 一次修复重试。 */
    private SeoDraft callModel(SeoLlmConfig config, PostDetailResponse detail) {
        String userPrompt = prompts.buildUserPrompt(detail, null);
        String output;
        try {
            output = client.complete(config, List.of(
                    new OpenAiCompatibleLlmClient.Message("system", SeoPromptBuilder.SYSTEM_PROMPT),
                    new OpenAiCompatibleLlmClient.Message("user", userPrompt)));
        } catch (LlmUpstreamException e) {
            throw upstreamFailure(e);
        }
        try {
            return parser.parse(output);
        } catch (SeoGenerationParser.InvalidSeoOutputException first) {
            log.info("[SEO] 模型输出未通过校验，发起一次修复重试：postId={}, problem={}",
                    detail.getId(), first.getMessage());
            String repaired;
            try {
                repaired = client.complete(config, List.of(
                        new OpenAiCompatibleLlmClient.Message("system", SeoPromptBuilder.SYSTEM_PROMPT),
                        new OpenAiCompatibleLlmClient.Message("user", userPrompt),
                        new OpenAiCompatibleLlmClient.Message("assistant", output),
                        new OpenAiCompatibleLlmClient.Message("user", prompts.buildRepairPrompt(output, first.getMessage()))));
            } catch (LlmUpstreamException e) {
                throw upstreamFailure(e);
            }
            try {
                return parser.parse(repaired);
            } catch (SeoGenerationParser.InvalidSeoOutputException second) {
                log.warn("[SEO] 修复重试仍未通过校验：postId={}, problem={}", detail.getId(), second.getMessage());
                throw new BusinessException(502, "模型返回的内容不符合要求，请重试或手动填写");
            }
        }
    }

    /** 上游异常 → 502；消息沿用客户端给出的安全文案（不含密钥与上游原文）。 */
    private BusinessException upstreamFailure(LlmUpstreamException e) {
        return new BusinessException(502, e.getMessage());
    }

    /**
     * 单行 upsert。
     *
     * <p>空字符串一律落成 {@code NULL}：否则「有行但字段为空」会被判定成有 SEO，
     * 前台又回落到派生值，出现「后台说已生成、前台没变化」的错位。</p>
     */
    private void save(Long postId, SeoDraft draft, String source) {
        PostSeo row = postSeoMapper.selectById(postId);
        boolean insert = row == null;
        if (insert) {
            row = new PostSeo();
            row.setPostId(postId);
        }
        row.setSeoTitle(SeoAuditService.blankToNull(draft.title()));
        row.setSeoDescription(SeoAuditService.blankToNull(draft.description()));
        row.setSeoKeywords(SeoAuditService.blankToNull(draft.keywords()));
        row.setSource(source);
        row.setGeneratedAt(LocalDateTime.now());
        if (insert) {
            postSeoMapper.insert(row);
        } else {
            postSeoMapper.updateById(row);
        }
    }

    /** 解析本次要处理的文章：显式 postIds 优先，否则按 limit 取最近未生成/全部。 */
    private List<Long> resolveCandidates(SeoBatchRequest request) {
        Set<Long> ids = new LinkedHashSet<>();
        if (request.getPostIds() != null && !request.getPostIds().isEmpty()) {
            for (Long id : request.getPostIds()) {
                if (id != null && id > 0) ids.add(id);
                if (ids.size() >= BATCH_IDS_MAX) break;
            }
            return new ArrayList<>(ids);
        }
        int limit = request.getLimit() == null ? BATCH_LIMIT_DEFAULT : request.getLimit();
        limit = Math.max(1, Math.min(BATCH_LIMIT_MAX, limit));
        boolean onlyMissing = Boolean.TRUE.equals(request.getOnlyMissing());
        var published = audit.publishedMetadata();
        var seoByPost = audit.seoByPostId();
        published.sort((a, b) -> {
            int byTime = compareTimeDesc(a.getCreateTime(), b.getCreateTime());
            return byTime != 0 ? byTime : Long.compare(b.getId(), a.getId());
        });
        for (BlogPost post : published) {
            if (onlyMissing && SeoAuditService.hasSeo(seoByPost.get(post.getId()))) continue;
            ids.add(post.getId());
            if (ids.size() >= limit) break;
        }
        return new ArrayList<>(ids);
    }

    private static int compareTimeDesc(LocalDateTime a, LocalDateTime b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return b.compareTo(a);
    }

    /** 把可能携带敏感信息的异常转成安全短句。 */
    private static String safeReason(Exception e) {
        if (e instanceof BusinessException be && be.getMessage() != null) {
            return be.getMessage();
        }
        if (e instanceof LlmUpstreamException && e.getMessage() != null) {
            return e.getMessage();
        }
        return "生成失败，请稍后重试";
    }

    /** 兜底脱敏：即便上游文案里意外带出密钥，也不让它在响应里出现。 */
    private static String redact(String message, String apiKey) {
        if (message == null) return "";
        if (apiKey == null || apiKey.isBlank()) return message;
        return message.replace(apiKey, "***");
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) + "…" : value;
    }
}
