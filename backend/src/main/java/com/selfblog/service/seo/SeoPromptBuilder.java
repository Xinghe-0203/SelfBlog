package com.selfblog.service.seo;

import com.selfblog.dto.site.PageModels.PageConfig;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.post.PostDetailResponse;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 组装发给模型的提示词。
 *
 * <p>模型只看到「标题 + 摘要 + 正文前 2000 字纯文本 + 站点语境 + 标签/分类」，
 * 拿不到作者账号、访客数据或任何后台设置。正文先经 Jsoup 去标签再截断，
 * 避免把 HTML 标签喂给模型（既浪费 token，也容易诱导它输出 HTML）。</p>
 *
 * <p>语言跟随文章本身：站点是中文博客，但英文文章应产出英文 SEO 文案，
 * 因此提示词要求「与文章主体语言一致」而不是硬编码中文。</p>
 */
@Component
public class SeoPromptBuilder {

    /** 正文最多送入模型的纯文本长度。 */
    static final int BODY_LIMIT = 2000;

    static final String SYSTEM_PROMPT = """
            你是资深技术博客的 SEO 编辑。你的任务是为给定文章生成搜索与社交分享用的元数据。

            严格要求：
            1. 只输出一个 JSON 对象，不要输出 Markdown 代码块、解释或任何多余文字。
            2. JSON 结构必须严格为：
               {"title": "字符串", "description": "字符串", "keywords": ["字符串", "字符串", "字符串"]}
            3. title：不超过 60 个字符（中文字符按 1 个计），要点明主题，不要堆砌关键词，不要带书名号或引号。
            4. description：120-160 个字符，是一句完整、可独立阅读的话，说明文章讲了什么、读者能获得什么；不要以省略号结尾。
            5. keywords：3-8 个，每个不超过 20 个字符，按重要度排序，不要重复，不要包含标点。
            6. 输出语言必须与文章正文的主体语言一致（中文文章用中文，英文文章用英文）。
            7. 不要编造文章中不存在的技术、数据或结论。
            """;

    private final SiteSettingsService settings;

    public SeoPromptBuilder(SiteSettingsService settings) {
        this.settings = settings;
    }

    /**
     * 首次生成用的 user 消息。
     *
     * @param detail 文章详情（含标题、摘要、正文、分类与标签）
     */
    public String buildUserPrompt(PostDetailResponse detail, PageConfig config) {
        Map<String, String> values = settings.getSettings();
        StringBuilder prompt = new StringBuilder(4096);
        prompt.append("站点名称：").append(nullToEmpty(values.get("siteTitle"))).append('\n');
        prompt.append("站点简介：").append(nullToEmpty(values.get("siteDescription"))).append('\n');
        if (config != null && config.title() != null && !config.title().isBlank()) {
            prompt.append("栏目：").append(config.title()).append('\n');
        }
        prompt.append("\n文章标题：").append(nullToEmpty(detail.getTitle())).append('\n');
        prompt.append("文章分类：").append(nullToEmpty(detail.getCategory())).append('\n');
        List<String> tags = tagNames(detail);
        if (!tags.isEmpty()) {
            prompt.append("文章标签：").append(String.join("、", tags)).append('\n');
        }
        if (detail.getSummary() != null && !detail.getSummary().isBlank()) {
            prompt.append("文章摘要：").append(detail.getSummary().trim()).append('\n');
        }
        prompt.append("\n正文节选（已去除标记）：\n").append(excerpt(detail.getContent()));
        prompt.append("\n\n请按系统要求只输出 JSON 对象。");
        return prompt.toString();
    }

    /**
     * 解析/校验失败后的「修复」消息：把上一轮原始输出与具体问题回给模型，
     * 要求它只修正格式，不要改内容主旨。
     */
    public String buildRepairPrompt(String previousOutput, String problem) {
        StringBuilder prompt = new StringBuilder(1024);
        prompt.append("你上一次的输出无法通过校验，问题：").append(problem).append('\n');
        prompt.append("上一次输出如下（可能包含代码块或多余文字）：\n");
        prompt.append(previousOutput == null ? "" : previousOutput);
        prompt.append("\n\n请重新输出一个合法的 JSON 对象，结构严格为：");
        prompt.append("{\"title\": \"...\", \"description\": \"...\", \"keywords\": [\"...\", \"...\", \"...\"]}。");
        prompt.append("title 不超过 60 字符，description 为 120-160 字符，keywords 为 3-8 个且每个不超过 20 字符。");
        prompt.append("只输出 JSON，不要任何解释。");
        return prompt.toString();
    }

    /** 正文纯文本前 {@value #BODY_LIMIT} 字符；Markdown 标记也一并去掉。 */
    public static String excerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String text = Jsoup.parse(content).text();
        return text.length() > BODY_LIMIT ? text.substring(0, BODY_LIMIT) : text;
    }

    static List<String> tagNames(PostDetailResponse detail) {
        if (detail.getTags() == null) {
            return List.of();
        }
        return detail.getTags().stream()
                .map(PostDetailResponse.TagVO::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
