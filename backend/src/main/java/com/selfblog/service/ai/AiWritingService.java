package com.selfblog.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.BlogPostTag;
import com.selfblog.entity.BlogTag;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.BlogPostTagMapper;
import com.selfblog.mapper.BlogTagMapper;
import com.selfblog.service.seo.SeoLlmConfig;
import com.selfblog.vo.ai.AiInlineWritingVO;
import com.selfblog.vo.ai.AiLinkSuggestionVO;
import com.selfblog.vo.ai.AiLinkSuggestionsVO;
import com.selfblog.vo.ai.AiSummaryVO;
import com.selfblog.vo.ai.AiTagsVO;
import com.selfblog.vo.ai.AiTitlesVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * B 写作辅助：摘要、标签、标题、行内写作（选区润色/续写/精简/翻译）、内链建议。
 *
 * <p>这些能力都只读文章、只返回建议，<b>不写任何业务表</b>——是否采用由管理员在编辑页决定。
 * 与 {@code SeoGenerationService} 一样刻意不加 {@code @Transactional}：模型调用是跨网络 IO，
 * 不能占着数据库连接等上游。</p>
 */
@Slf4j
@Service
public class AiWritingService {

    /** 摘要长度区间（契约 §3）。 */
    static final int SUMMARY_MIN = 80;
    static final int SUMMARY_MAX = 200;

    /** 标签数量区间（契约 §3）。 */
    static final int TAGS_MIN = 3;
    static final int TAGS_MAX = 6;

    /** 送进模型的标签池上限（契约 §3：按使用频次降序取前 60）。 */
    static final int TAG_POOL_LIMIT = 60;

    /** 标题候选数量与单条长度上限（契约 §3）。 */
    static final int TITLES_COUNT = 3;
    static final int TITLE_MAX = 60;

    static final String NO_TAG_MESSAGE = "还没有任何标签，请先创建标签";
    static final String NO_TAG_OUTPUT_MESSAGE = "模型没有给出任何可用的已有标签，请重试或手动选择";

    /** 行内写作支持的选区处理方式（契约 §3 扩展：polish/continue/shorten/translate）。 */
    static final Set<String> INLINE_ACTIONS = Set.of("polish", "continue", "shorten", "translate");
    static final String INLINE_ACTION_INVALID_MESSAGE = "处理方式仅支持 polish、continue、shorten、translate";
    static final String INLINE_TEXT_BLANK_MESSAGE = "请提供需要处理的文本";
    static final String INLINE_TEXT_TOO_LONG_MESSAGE = "选区内容过长，请最多选择 3000 字符";

    /** 行内写作的选区长度上限，与 {@link AiPromptBuilder#INLINE_TEXT_LIMIT} 同源。 */
    static final int INLINE_TEXT_MAX = AiPromptBuilder.INLINE_TEXT_LIMIT;

    /** 内链候选文章：其他已发布文章按更新/创建时间倒序取前 40。 */
    static final int LINK_CANDIDATES_LIMIT = 40;

    /** 内链建议最多返回 5 条；锚文本与理由的超限只是兜底截断，不做更严的拒收。 */
    static final int LINK_SUGGESTIONS_MAX = 5;
    static final int LINK_ANCHOR_MAX = 60;
    static final int LINK_REASON_MAX = 200;

    static final String NO_OTHER_PUBLISHED_MESSAGE = "暂无其他已发布文章，无法生成内链建议";

    private final AiAvailability availability;
    private final AiModelCaller caller;
    private final AiJson json;
    private final AiPromptBuilder prompts;
    private final BlogPostMapper postMapper;
    private final BlogTagMapper tagMapper;
    private final BlogPostTagMapper postTagMapper;

    public AiWritingService(AiAvailability availability, AiModelCaller caller, AiJson json,
                            AiPromptBuilder prompts, BlogPostMapper postMapper,
                            BlogTagMapper tagMapper, BlogPostTagMapper postTagMapper) {
        this.availability = availability;
        this.caller = caller;
        this.json = json;
        this.prompts = prompts;
        this.postMapper = postMapper;
        this.tagMapper = tagMapper;
        this.postTagMapper = postTagMapper;
    }

    /** 生成 80-200 字纯文本摘要。 */
    public AiSummaryVO summary(Long postId) {
        SeoLlmConfig config = availability.requireUsable();
        BlogPost post = requirePost(postId);
        long started = System.nanoTime();
        String summary = caller.generate(config, AiPromptBuilder.SUMMARY_SYSTEM_PROMPT,
                prompts.summaryUserPrompt(post), this::parseSummary);
        AiSummaryVO vo = new AiSummaryVO();
        vo.setSummary(summary);
        vo.setModel(config.model());
        vo.setLatencyMs(elapsedMs(started));
        return vo;
    }

    /**
     * 从已有标签池里推荐 3-6 个标签。
     *
     * <p>池 = 全部未删除标签按 {@code blog_post_tag} 使用频次降序取前 60。模型只能从池里挑，
     * 池外的值一律丢弃（避免凭空造标签污染标签体系）；全被丢弃说明这次输出没用，报 502。</p>
     */
    public AiTagsVO tags(Long postId) {
        SeoLlmConfig config = availability.requireUsable();
        BlogPost post = requirePost(postId);
        List<String> pool = tagPool();
        if (pool.isEmpty()) {
            throw new BusinessException(400, NO_TAG_MESSAGE);
        }
        long started = System.nanoTime();
        List<String> picked = caller.generate(config, AiPromptBuilder.TAGS_SYSTEM_PROMPT,
                prompts.tagsUserPrompt(post, pool), raw -> parseTags(raw, pool));
        AiTagsVO vo = new AiTagsVO();
        vo.setTags(picked);
        vo.setModel(config.model());
        vo.setLatencyMs(elapsedMs(started));
        return vo;
    }

    /** 生成恰好 3 个互不相同、每个不超过 60 字的标题候选。 */
    public AiTitlesVO titles(Long postId) {
        SeoLlmConfig config = availability.requireUsable();
        BlogPost post = requirePost(postId);
        long started = System.nanoTime();
        List<String> titles = caller.generate(config, AiPromptBuilder.TITLES_SYSTEM_PROMPT,
                prompts.titlesUserPrompt(post), this::parseTitles);
        AiTitlesVO vo = new AiTitlesVO();
        vo.setTitles(titles);
        vo.setModel(config.model());
        vo.setLatencyMs(elapsedMs(started));
        return vo;
    }

    /**
     * 行内写作：对编辑器选区做润色（polish）、续写（continue）、精简（shorten）或翻译（translate）。
     *
     * <p>只处理传入的选区文本，不查文章表；处理结果只是建议文本，由管理员决定是否替换选区。
     * action 大小写不敏感但必须在四种之内，text 去空白后非空且不超过 3000 字符。</p>
     */
    public AiInlineWritingVO inlineWrite(String action, String text) {
        String normalized = normalizeInlineAction(action);
        if (text == null || text.isBlank()) {
            throw new BusinessException(400, INLINE_TEXT_BLANK_MESSAGE);
        }
        String selected = text.trim();
        if (selected.length() > INLINE_TEXT_MAX) {
            throw new BusinessException(400, INLINE_TEXT_TOO_LONG_MESSAGE);
        }
        SeoLlmConfig config = availability.requireUsable();
        long started = System.nanoTime();
        String result = caller.generate(config, AiPromptBuilder.INLINE_SYSTEM_PROMPT,
                prompts.inlineUserPrompt(AiPromptBuilder.inlineInstruction(normalized), selected),
                this::parseInlineText);
        return new AiInlineWritingVO(normalized, result, config.model(), elapsedMs(started));
    }

    /**
     * 内链建议：从其他已发布文章里挑 3-5 篇值得从当前文章链接过去的目标。
     *
     * <p>候选 = 除当前文章外的已发布文章，按更新/创建时间倒序取前 40。模型只能从候选里挑
     * （候选外的 postId 一律丢弃），标题一律取候选列表回填、绝不采信模型输出；候选全被丢弃
     * 是合法结果（返回空数组），不算失败。只读，不写任何业务表。</p>
     */
    public AiLinkSuggestionsVO suggestLinks(Long postId) {
        SeoLlmConfig config = availability.requireUsable();
        BlogPost post = requirePost(postId);
        List<BlogPost> candidates = loadLinkCandidates(post.getId());
        if (candidates.isEmpty()) {
            throw new BusinessException(400, NO_OTHER_PUBLISHED_MESSAGE);
        }
        Map<Long, BlogPost> candidateMap = new LinkedHashMap<>();
        for (BlogPost candidate : candidates) {
            candidateMap.put(candidate.getId(), candidate);
        }
        long started = System.nanoTime();
        List<AiLinkSuggestionVO> suggestions = caller.generate(config,
                AiPromptBuilder.LINKS_SYSTEM_PROMPT, prompts.linksUserPrompt(post, candidates),
                raw -> parseLinks(raw, candidateMap));
        return new AiLinkSuggestionsVO(suggestions, config.model(), elapsedMs(started));
    }

    private String parseSummary(String raw) {
        JsonNode root = json.read(raw);
        String summary = AiJson.plainText(AiJson.text(root, "summary"));
        if (summary.isEmpty()) {
            throw new AiOutputException("缺少 summary 字段");
        }
        if (summary.length() < SUMMARY_MIN) {
            throw new AiOutputException("summary 太短（" + summary.length() + " 字，要求 "
                    + SUMMARY_MIN + "-" + SUMMARY_MAX + " 字）");
        }
        return AiJson.truncate(summary, SUMMARY_MAX);
    }

    private List<String> parseTags(String raw, List<String> pool) {
        JsonNode root = json.read(raw);
        JsonNode node = root.get("tags");
        if (node == null) {
            node = root.get("keywords");
        }
        List<String> candidates = AiJson.textList(node);
        // 池内标签按名字索引；模型偶尔会改写大小写或加空格，这里做一次归一化匹配
        Map<String, String> canonical = new LinkedHashMap<>();
        for (String name : pool) {
            canonical.putIfAbsent(normalize(name), name);
        }
        LinkedHashSet<String> picked = new LinkedHashSet<>();
        for (String candidate : candidates) {
            String matched = canonical.get(normalize(candidate));
            if (matched != null) {
                picked.add(matched);
            }
            if (picked.size() >= TAGS_MAX) {
                break;
            }
        }
        if (picked.isEmpty()) {
            throw new AiOutputException("没有给出任何「可选标签」里的名字");
        }
        int required = Math.min(TAGS_MIN, pool.size());
        if (picked.size() < required) {
            throw new AiOutputException("只挑出了 " + picked.size() + " 个已有标签，要求至少 " + required + " 个");
        }
        return new ArrayList<>(picked);
    }

    private List<String> parseTitles(String raw) {
        JsonNode root = json.read(raw);
        JsonNode node = root.get("titles");
        if (node == null) {
            node = root.get("title");
        }
        List<String> candidates = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String value : AiJson.textList(node)) {
            String title = AiJson.plainText(value);
            if (!title.isEmpty()) {
                seen.add(title);
            }
        }
        for (String title : seen) {
            if (title.length() > TITLE_MAX) {
                throw new AiOutputException("标题「" + title + "」超过 " + TITLE_MAX + " 字");
            }
            candidates.add(title);
        }
        if (candidates.size() < TITLES_COUNT) {
            throw new AiOutputException("只给出了 " + candidates.size() + " 个互不相同的标题，要求 "
                    + TITLES_COUNT + " 个");
        }
        return candidates.subList(0, TITLES_COUNT);
    }

    /** 行内写作输出只要求一个非空 {@code text} 字段；内容原样返回（保留格式标记），仅去首尾空白。 */
    private String parseInlineText(String raw) {
        JsonNode root = json.read(raw);
        String text = AiJson.text(root, "text");
        if (text == null || text.isBlank()) {
            throw new AiOutputException("缺少 text 字段");
        }
        return text.trim();
    }

    /**
     * 内链建议解析：候选外的 postId 与空白锚文本整条丢弃，最多保留 5 条。
     *
     * <p>显式的空数组是模型说「没有合适的候选」，属于合法输出直接返回空列表；
     * 但 {@code links} 字段缺失或不是数组说明结构没对上，走修复重试。</p>
     */
    private List<AiLinkSuggestionVO> parseLinks(String raw, Map<Long, BlogPost> candidates) {
        JsonNode root = json.read(raw);
        JsonNode node = root.get("links");
        if (node == null) {
            node = root.get("suggestions");
        }
        if (node == null || node.isNull()) {
            throw new AiOutputException("缺少 links 数组");
        }
        if (!node.isArray()) {
            throw new AiOutputException("links 必须是数组");
        }
        List<AiLinkSuggestionVO> picked = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || !item.isObject()) {
                continue;
            }
            Long targetPostId = AiJson.longValue(item, "postId");
            String anchor = AiJson.plainText(AiJson.text(item, "anchor"));
            if (targetPostId == null || !candidates.containsKey(targetPostId) || anchor.isEmpty()) {
                continue;
            }
            BlogPost target = candidates.get(targetPostId);
            String reason = AiJson.plainText(AiJson.text(item, "reason"));
            picked.add(new AiLinkSuggestionVO(targetPostId,
                    target.getTitle() == null ? "" : target.getTitle().trim(),
                    AiJson.truncate(anchor, LINK_ANCHOR_MAX),
                    reason.isEmpty() ? null : AiJson.truncate(reason, LINK_REASON_MAX)));
            if (picked.size() >= LINK_SUGGESTIONS_MAX) {
                break;
            }
        }
        return picked;
    }

    /**
     * 标签池：全部未删除标签，按 {@code blog_post_tag} 使用频次降序取前 60。
     *
     * <p>没有现成的频次统计 Mapper 方法，而 {@code mapper/**} 不在本次写域内，
     * 因此在服务层用两次 {@code selectList} 在内存里聚合。个人博客的标签与关联行数量都很小，
     * 这样做比新增一条 MySQL/H2 都要兼容的聚合 SQL 更省事也更好测。</p>
     */
    private List<String> tagPool() {
        List<BlogTag> tags = tagMapper.selectList(null);
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> usage = new LinkedHashMap<>();
        List<BlogPostTag> relations = postTagMapper.selectList(null);
        if (relations != null) {
            for (BlogPostTag relation : relations) {
                if (relation != null && relation.getTagId() != null) {
                    usage.merge(relation.getTagId(), 1, Integer::sum);
                }
            }
        }
        List<BlogTag> sorted = new ArrayList<>(tags);
        sorted.sort(Comparator
                .comparingInt((BlogTag tag) -> usage.getOrDefault(tag.getId(), 0)).reversed()
                .thenComparing(tag -> tag.getName() == null ? "" : tag.getName()));
        return sorted.stream()
                .map(BlogTag::getName)
                .filter(name -> name != null && !name.isBlank())
                .limit(TAG_POOL_LIMIT)
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
    }

    /** action 只接受 polish/continue/shorten/translate（大小写不敏感），其他一律 400。 */
    private static String normalizeInlineAction(String action) {
        String normalized = action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
        if (!INLINE_ACTIONS.contains(normalized)) {
            throw new BusinessException(400, INLINE_ACTION_INVALID_MESSAGE);
        }
        return normalized;
    }

    /**
     * 内链候选：其他已发布文章（status=1，排除当前篇），按更新/创建时间倒序取前 40。
     *
     * <p>用 MyBatis Plus 生成查询（逻辑删除条件自动带上），只需轻量列：
     * id/标题/摘要/正文——摘要缺了要用正文开头充数，所以正文也要查。
     * 与 {@code AiProofreadService}/{@code AiHealthReportService} 同一套写法，不新增 Mapper SQL。</p>
     */
    private List<BlogPost> loadLinkCandidates(Long excludePostId) {
        List<BlogPost> posts = postMapper.selectList(
                new LambdaQueryWrapper<BlogPost>()
                        .select(BlogPost::getId, BlogPost::getTitle, BlogPost::getSummary, BlogPost::getContent)
                        .eq(BlogPost::getStatus, 1)
                        .ne(BlogPost::getId, excludePostId)
                        .orderByDesc(BlogPost::getUpdateTime, BlogPost::getCreateTime)
                        .last("LIMIT " + LINK_CANDIDATES_LIMIT));
        return posts == null ? List.of() : posts;
    }

    /** 文章必须存在（不限制发布状态：草稿也要能生成摘要与标题）。 */
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

    static Long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}
