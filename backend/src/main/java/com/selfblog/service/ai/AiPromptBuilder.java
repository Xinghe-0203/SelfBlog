package com.selfblog.service.ai;

import com.selfblog.entity.BlogPost;
import com.selfblog.service.post.PostQueryService;
import com.selfblog.service.site.SiteSettingsService;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 能力的提示词与素材拼装。
 *
 * <p>所有能力的系统提示词都强调三件事：<b>只输出 JSON 对象</b>（不要 Markdown 代码块）、
 * <b>不要编造正文里没有的事实</b>、<b>用与正文一致的语言</b>。素材一律先转成纯文本再截断，
 * 避免把 HTML 标记喂给模型浪费 token。</p>
 */
@Component
public class AiPromptBuilder {

    /** 校对正文的截断上限（契约 §5：超过 6000 字截断并标记 truncated）。 */
    static final int PROOFREAD_BODY_LIMIT = 6000;

    /** 摘要/标签/标题三种写作辅助共用的正文上限。 */
    static final int WRITING_BODY_LIMIT = 3000;

    /** 评论审核里内容的最大长度，防止一条超长留言吃满上下文。 */
    static final int COMMENT_LIMIT = 2000;

    static final String SUMMARY_SYSTEM_PROMPT = """
            你是中文技术博客的编辑助手。请阅读作者的文章，写一段适合放在文章列表与分享卡片上的摘要。
            要求：
            1. 只输出一个 JSON 对象，不要 Markdown 代码块，不要任何解释。
            2. 结构为 {"summary":"..."}。
            3. summary 是 80 到 200 个中文字的纯文本，不要换行、不要列表符号、不要 Markdown 标记。
            4. 概括文章真正讲了什么，不要用「本文介绍了」这类空话开头，不要编造正文没有的内容。
            5. 语言与正文保持一致。""";

    static final String TAGS_SYSTEM_PROMPT = """
            你是中文技术博客的标签管理员。请从给定的「可选标签」里，为这篇文章挑出最贴切的标签。
            要求：
            1. 只输出一个 JSON 对象，不要 Markdown 代码块，不要任何解释。
            2. 结构为 {"tags":["标签1","标签2"]}。
            3. 只能使用「可选标签」里原样出现的名字，一个字都不能改，也不要自创新标签。
            4. 挑 3 到 6 个，按相关度从高到低排列。
            5. 如果可选标签都不太贴切，也仍然要挑出相对最接近的几个，不要返回空数组。""";

    static final String TITLES_SYSTEM_PROMPT = """
            你是中文技术博客的标题编辑。请为这篇文章拟 3 个更有吸引力的标题候选。
            要求：
            1. 只输出一个 JSON 对象，不要 Markdown 代码块，不要任何解释。
            2. 结构为 {"titles":["候选一","候选二","候选三"]}。
            3. 恰好 3 个，每个不超过 60 个中文字，三个之间必须互不相同。
            4. 保留原标题的核心信息，不要夸大、不要标题党、不要编造正文没有的内容。
            5. 语言与正文保持一致。""";

    static final String MODERATION_SYSTEM_PROMPT = """
            你是博客评论区的审核员。请判断这条访客留言是否应当公开显示。
            要求：
            1. 只输出一个 JSON 对象，不要 Markdown 代码块，不要任何解释。
            2. 结构为 {"verdict":"pass","reason":"...","categories":["ad"]}。
            3. verdict 只能是 pass（正常，可直接公开）、flag（可疑，需人工确认）、block（明显违规，应拦截）。
            4. categories 只能从 ad（广告推广）、abuse（人身攻击/辱骂）、spam（无意义刷屏）、
               offtopic（与文章无关）、meaningless（纯表情或无内容）里选，可以是空数组。
            5. reason 用一句中文说明判断依据，不超过 60 字。
            6. 正常的讨论、提问、批评、不同意见都算 pass，不要把负面评价当违规。""";

    static final String PROOFREAD_SYSTEM_PROMPT = """
            你是中文技术博客的校对员。请找出给定正文里的文字问题。
            要求：
            1. 只输出一个 JSON 对象，不要 Markdown 代码块，不要任何解释。
            2. 结构为 {"issues":[{"type":"typo","excerpt":"原文片段","suggestion":"修改建议","severity":"low"}]}。
            3. type 只能是 typo（错别字/标点）、grammar（语法/病句）、wording（用词不当/表达拗口）、
               fact（明显的事实或数据矛盾）。
            4. severity 只能是 low、medium、high。
            5. excerpt 必须是正文里原样出现的片段，不超过 120 字；suggestion 不超过 200 字。
            6. 最多列出 20 条，按严重程度从高到低排列；没有问题时返回空数组 []。
            7. 不要评价写作风格、不要提结构建议，只找具体的文字问题。""";

    static final String IMAGE_ALT_SYSTEM_PROMPT = """
            你是无障碍内容编辑。请根据图片内容，为它写一段替代文本和一句图注。
            要求：
            1. 只输出一个 JSON 对象，不要 Markdown 代码块，不要任何解释。
            2. 结构为 {"alt":"...","caption":"..."}。
            3. alt 是给读屏软件朗读的客观描述，不超过 120 个中文字，说清画面主体与关键细节，
               不要以「图片」「照片」开头，不要加入主观感受。
            4. caption 是一句简短的图注，不超过 80 个中文字，可以是空字符串。
            5. 只用中文，不要编造画面里看不到的内容。""";

    static final String HEALTH_SYSTEM_PROMPT = """
            你是个人博客的内容运营顾问。请根据给出的站点文章元数据（不含正文），评估站点内容健康状况。
            要求：
            1. 只输出一个 JSON 对象，不要 Markdown 代码块，不要任何解释。
            2. 结构为
               {"score":72,"summary":"...","findings":[{"title":"...","detail":"...","severity":"medium","action":"...","count":12}]}。
            3. score 是 0 到 100 的整数，越高越健康。
            4. summary 是 100 到 300 个中文字的总体评价，先说结论再说主要问题。
            5. findings 给 3 到 8 条具体发现；severity 只能是 low、medium、high；
               count 是涉及的文章数量，非计数类问题填 null。
            6. 只依据给出的数据判断（例如摘要缺失、标签过于集中、分类单一、更新频率骤降、
               标题过长或雷同），不要编造数据里没有的现象。""";

    private final SiteSettingsService settings;
    private final PostQueryService postQueryService;

    public AiPromptBuilder(SiteSettingsService settings, PostQueryService postQueryService) {
        this.settings = settings;
        this.postQueryService = postQueryService;
    }

    /** 站点名与简介，作为所有提示词的背景信息。 */
    String siteContext() {
        Map<String, String> values = settings.getSettings();
        String title = nullToEmpty(values.get("siteTitle")).trim();
        String description = nullToEmpty(values.get("siteDescription")).trim();
        StringBuilder sb = new StringBuilder();
        sb.append("站点名称：").append(title.isEmpty() ? "个人博客" : title).append('\n');
        if (!description.isEmpty()) {
            sb.append("站点简介：").append(description).append('\n');
        }
        return sb.toString();
    }

    String summaryUserPrompt(BlogPost post) {
        return siteContext() + postContext(post, WRITING_BODY_LIMIT)
                + "\n请按系统要求只输出 JSON 对象。";
    }

    String tagsUserPrompt(BlogPost post, List<String> pool) {
        return siteContext() + postContext(post, WRITING_BODY_LIMIT)
                + "\n可选标签：" + String.join("、", pool)
                + "\n\n请从上面「可选标签」里挑 3 到 6 个，按系统要求只输出 JSON 对象。";
    }

    String titlesUserPrompt(BlogPost post) {
        return siteContext() + postContext(post, WRITING_BODY_LIMIT)
                + "\n请按系统要求只输出 JSON 对象。";
    }

    String moderationUserPrompt(String nickname, String content, String postTitle) {
        String body = AiJson.truncate(AiJson.plainText(content), COMMENT_LIMIT);
        return "留言昵称：" + nullToEmpty(nickname).trim() + '\n'
                + (postTitle == null || postTitle.isBlank() ? "" : "所在文章：" + postTitle + '\n')
                + "留言内容：\n" + body
                + "\n\n请按系统要求只输出 JSON 对象。";
    }

    String proofreadUserPrompt(BlogPost post) {
        String plain = AiJson.plainText(post.getContent());
        String body = AiJson.truncate(plain, PROOFREAD_BODY_LIMIT);
        return "文章标题：" + nullToEmpty(post.getTitle()).trim() + '\n'
                + "正文：\n" + body
                + "\n\n请按系统要求只输出 JSON 对象。";
    }

    /** 正文是否超过校对上限（用于置 truncated）。 */
    static boolean isBodyTruncated(BlogPost post) {
        return AiJson.plainText(post.getContent()).length() > PROOFREAD_BODY_LIMIT;
    }

    String imageAltUserPrompt(String hint) {
        StringBuilder sb = new StringBuilder(siteContext());
        if (hint != null && !hint.isBlank()) {
            sb.append("这张图的现有说明（可能为空或不准确，仅供参考）：").append(hint.trim()).append('\n');
        }
        sb.append("\n请按系统要求只输出 JSON 对象。");
        return sb.toString();
    }

    String healthUserPrompt(String material) {
        return siteContext() + "\n文章元数据：\n" + material
                + "\n请按系统要求只输出 JSON 对象。";
    }

    /** 拼「文章标题/分类/标签/摘要/正文节选」，正文转纯文本后截断。 */
    private String postContext(BlogPost post, int bodyLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("文章标题：").append(nullToEmpty(post.getTitle()).trim()).append('\n');
        if (post.getCategory() != null && !post.getCategory().isBlank()) {
            sb.append("文章分类：").append(post.getCategory().trim()).append('\n');
        }
        List<String> tags = tagNames(post.getId());
        if (!tags.isEmpty()) {
            sb.append("文章标签：").append(String.join("、", tags)).append('\n');
        }
        if (post.getSummary() != null && !post.getSummary().isBlank()) {
            sb.append("文章摘要：").append(post.getSummary().trim()).append('\n');
        }
        sb.append("正文节选（已去除标记）：\n")
                .append(AiJson.truncate(AiJson.plainText(post.getContent()), bodyLimit))
                .append('\n');
        return sb.toString();
    }

    private List<String> tagNames(Long postId) {
        try {
            return postQueryService.getTagsByPostId(postId).stream()
                    .map(tag -> tag == null ? null : tag.getName())
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 体检素材的一行：标题/摘要/分类/标签/发布时间/字数，不含正文。 */
    static String healthRow(BlogPost post, List<String> tags) {
        String summary = post.getSummary() == null || post.getSummary().isBlank()
                ? "（无摘要）" : AiJson.truncate(AiJson.plainText(post.getSummary()), 120);
        String category = post.getCategory() == null || post.getCategory().isBlank()
                ? "（未分类）" : post.getCategory().trim();
        String tagText = tags == null || tags.isEmpty() ? "（无标签）" : String.join("、", tags);
        return "- 标题：" + nullToEmpty(post.getTitle()).trim()
                + "｜分类：" + category
                + "｜标签：" + tagText
                + "｜字数：" + AiJson.plainText(post.getContent()).length()
                + "｜发布：" + time(post.getCreateTime())
                + "｜摘要：" + summary;
    }

    static String time(LocalDateTime value) {
        return value == null ? "未知" : value.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
