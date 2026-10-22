package com.selfblog.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogComment;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.GuestComment;
import com.selfblog.mapper.BlogCommentMapper;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.GuestCommentMapper;
import com.selfblog.service.seo.SeoLlmConfig;
import com.selfblog.service.seo.SeoLlmConfigProvider;
import com.selfblog.vo.ai.AiModerationScanVO;
import com.selfblog.vo.ai.AiModerationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * C 评论审核：单条判定、批量扫描、以及留言落库后的自动审核。
 *
 * <p>三个入口共用同一套「取留言 → 拼提示词 → 调模型 → 归一化结论」流程，
 * 差别只在触发方式与结果处理：手动接口把结论返回给管理员，自动审核只在结论不是
 * {@code pass} 时把留言改回待审状态。</p>
 *
 * <p>自动审核必须绝对静默（契约 §4）：它是留言提交的副作用，任何异常都只能写日志，
 * 不能让访客的留言失败，也不能让提交变慢——所以入口是 afterCommit + {@code @Async}，
 * 且本类刻意不加 {@code @Transactional}（模型调用跨网络 IO）。</p>
 */
@Slf4j
@Service
public class AiModerationService {

    /** 契约 §4：结论取值。 */
    static final Set<String> VERDICTS = Set.of("pass", "flag", "block");

    /** 契约 §4：问题分类取值。 */
    static final Set<String> CATEGORIES = Set.of("ad", "abuse", "spam", "offtopic", "meaningless");

    static final String KIND_GUEST = "guest";
    static final String KIND_SITE = "site";

    static final int SCAN_LIMIT_DEFAULT = 20;
    static final int SCAN_LIMIT_MAX = 50;
    static final int EXCERPT_MAX = 120;
    static final String COMMENT_MISSING_MESSAGE = "评论不存在";

    /** 自动审核里「改回待审」用的状态值（0 = 待审核）。 */
    private static final int STATUS_PENDING = 0;

    private final AiAvailability availability;
    private final AiModelCaller caller;
    private final AiJson json;
    private final AiPromptBuilder prompts;
    private final GuestCommentMapper guestCommentMapper;
    private final BlogCommentMapper blogCommentMapper;
    private final BlogPostMapper postMapper;
    private final SeoLlmConfigProvider configProvider;

    public AiModerationService(AiAvailability availability, AiModelCaller caller, AiJson json,
                               AiPromptBuilder prompts, GuestCommentMapper guestCommentMapper,
                               BlogCommentMapper blogCommentMapper, BlogPostMapper postMapper,
                               SeoLlmConfigProvider configProvider) {
        this.availability = availability;
        this.caller = caller;
        this.json = json;
        this.prompts = prompts;
        this.guestCommentMapper = guestCommentMapper;
        this.blogCommentMapper = blogCommentMapper;
        this.postMapper = postMapper;
        this.configProvider = configProvider;
    }

    /** 单条判定。{@code kind} 缺省按访客留言处理。 */
    public AiModerationVO check(Long commentId, String kind) {
        SeoLlmConfig config = availability.requireUsable();
        String normalizedKind = normalizeKind(kind);
        CommentSnapshot snapshot = load(commentId, normalizedKind);
        long started = System.nanoTime();
        AiModerationVO vo = evaluate(config, snapshot);
        vo.setLatencyMs(AiWritingService.elapsedMs(started));
        return vo;
    }

    /**
     * 扫描最近公开的访客留言。
     *
     * <p>逐条串行判定、逐条隔离失败：某条模型输出不合规时记日志跳过，不让一条脏数据
     * 挡住整批结果。{@code scanned} 是实际尝试判定的条数，{@code flagged} 是其中结论
     * 不是 {@code pass} 的条数，{@code items} 只列被判有问题的那些。</p>
     */
    public AiModerationScanVO scan(Integer limit) {
        SeoLlmConfig config = availability.requireUsable();
        int size = normalizeScanLimit(limit);
        List<GuestComment> comments = guestCommentMapper.selectList(
                new LambdaQueryWrapper<GuestComment>()
                        .eq(GuestComment::getStatus, 1)
                        .orderByDesc(GuestComment::getCreateTime)
                        // 同一秒落库的留言 create_time 会完全相同，补 id 兜底保证扫描顺序稳定
                        .orderByDesc(GuestComment::getId)
                        .last("LIMIT " + size));
        AiModerationScanVO vo = new AiModerationScanVO();
        List<AiModerationScanVO.Item> items = new ArrayList<>();
        int scanned = 0;
        for (GuestComment comment : comments) {
            CommentSnapshot snapshot = new CommentSnapshot(comment.getId(), KIND_GUEST,
                    comment.getNickname(), comment.getContent(), postTitle(comment.getPostId()));
            AiModerationVO verdict;
            try {
                verdict = evaluate(config, snapshot);
            } catch (BusinessException e) {
                log.warn("[AI] 留言 {} 审核失败，跳过：{}", comment.getId(), e.getMessage());
                continue;
            }
            scanned++;
            if ("pass".equals(verdict.getVerdict())) {
                continue;
            }
            items.add(toItem(snapshot, verdict));
        }
        vo.setScanned(scanned);
        vo.setFlagged(items.size());
        vo.setItems(items);
        return vo;
    }

    /**
     * 留言落库后的自动审核（由 {@link AiCommentModerationTrigger} 在事务提交后异步调用）。
     *
     * <p>任何异常都吞掉只记日志；结论不是 {@code pass} 时把留言改成待审状态。
     * {@code guest_comment} 表没有审核原因列，所以原因只写日志（改表结构不在本次范围）。</p>
     */
    public void moderateQuietly(Long commentId) {
        try {
            if (!configProvider.commentModerationEnabled()) {
                return;
            }
            GuestComment comment = guestCommentMapper.selectById(commentId);
            if (comment == null) {
                return;
            }
            SeoLlmConfig config = availability.usableOrNull();
            if (config == null) {
                return;
            }
            CommentSnapshot snapshot = new CommentSnapshot(comment.getId(), KIND_GUEST,
                    comment.getNickname(), comment.getContent(), postTitle(comment.getPostId()));
            AiModerationVO verdict = evaluate(config, snapshot);
            if ("pass".equals(verdict.getVerdict())) {
                return;
            }
            GuestComment update = new GuestComment();
            update.setId(commentId);
            update.setStatus(STATUS_PENDING);
            guestCommentMapper.updateById(update);
            log.info("[AI] 留言 {} 自动审核结论 {}，已改为待审，原因：{}",
                    commentId, verdict.getVerdict(), verdict.getReason());
        } catch (Exception e) {
            log.warn("[AI] 留言 {} 自动审核失败（已忽略，不影响留言）：{}",
                    commentId, safeReason(e));
        }
    }

    /** 调模型并把输出归一化成契约允许的取值。 */
    private AiModerationVO evaluate(SeoLlmConfig config, CommentSnapshot snapshot) {
        AiModerationVO vo = caller.generate(config, AiPromptBuilder.MODERATION_SYSTEM_PROMPT,
                prompts.moderationUserPrompt(snapshot.nickname(), snapshot.content(), snapshot.postTitle()),
                this::parseModeration);
        vo.setModel(config.model());
        return vo;
    }

    private AiModerationVO parseModeration(String raw) {
        JsonNode root = json.read(raw);
        String verdict = normalize(AiJson.text(root, "verdict"));
        if (verdict.isEmpty() || !VERDICTS.contains(verdict)) {
            throw new AiOutputException("verdict 必须是 pass、flag 或 block");
        }
        AiModerationVO vo = new AiModerationVO();
        vo.setVerdict(verdict);
        String reason = AiJson.plainText(AiJson.text(root, "reason"));
        vo.setReason(reason.isEmpty() ? defaultReason(verdict) : AiJson.truncate(reason, 120));
        vo.setCategories(parseCategories(root.get("categories")));
        return vo;
    }

    private static List<String> parseCategories(JsonNode node) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : AiJson.textList(node)) {
            String normalized = normalize(value);
            if (CATEGORIES.contains(normalized)) {
                values.add(normalized);
            }
        }
        return new ArrayList<>(values);
    }

    private static String defaultReason(String verdict) {
        return switch (verdict) {
            case "block" -> "留言内容明显违规";
            case "flag" -> "留言内容需要人工确认";
            default -> "正常留言";
        };
    }

    private CommentSnapshot load(Long commentId, String kind) {
        if (commentId == null) {
            throw new BusinessException(400, "请指定评论");
        }
        if (KIND_SITE.equals(kind)) {
            BlogComment comment = blogCommentMapper.selectById(commentId);
            if (comment == null) {
                throw new BusinessException(404, COMMENT_MISSING_MESSAGE);
            }
            return new CommentSnapshot(comment.getId(), KIND_SITE, "站内用户",
                    comment.getContent(), postTitle(comment.getPostId()));
        }
        GuestComment comment = guestCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, COMMENT_MISSING_MESSAGE);
        }
        return new CommentSnapshot(comment.getId(), KIND_GUEST, comment.getNickname(),
                comment.getContent(), postTitle(comment.getPostId()));
    }

    private String postTitle(Long postId) {
        if (postId == null) {
            return null;
        }
        try {
            BlogPost post = postMapper.selectById(postId);
            return post == null ? null : post.getTitle();
        } catch (Exception e) {
            return null;
        }
    }

    private static AiModerationScanVO.Item toItem(CommentSnapshot snapshot, AiModerationVO verdict) {
        AiModerationScanVO.Item item = new AiModerationScanVO.Item();
        item.setCommentId(snapshot.commentId());
        item.setKind(snapshot.kind());
        item.setNickname(snapshot.nickname());
        item.setExcerpt(AiJson.truncate(AiJson.plainText(snapshot.content()), EXCERPT_MAX));
        item.setVerdict(verdict.getVerdict());
        item.setReason(verdict.getReason());
        item.setCategories(verdict.getCategories());
        return item;
    }

    static String normalizeKind(String kind) {
        String value = normalize(kind);
        if (value.isEmpty()) {
            return KIND_GUEST;
        }
        if (!KIND_GUEST.equals(value) && !KIND_SITE.equals(value)) {
            throw new BusinessException(400, "评论来源只能是 guest 或 site");
        }
        return value;
    }

    static int normalizeScanLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return SCAN_LIMIT_DEFAULT;
        }
        return Math.min(limit, SCAN_LIMIT_MAX);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeReason(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    /** 审核所需的留言快照（访客留言与站内评论字段不同，先归一化再拼提示词）。 */
    private record CommentSnapshot(Long commentId, String kind, String nickname,
                                   String content, String postTitle) {
    }
}
