package com.selfblog.service.site.seo;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Static public reading only; raw author HTML is never a trusted template.
 *
 * <p>GFM 能力必须与前端 marked（{@code gfm: true}）一致：表格、删除线、任务列表在两侧
 * 都要正常渲染，否则同一篇文章在浏览器里正常、在服务端 HTML 里退化成纯文本。
 * 任务列表的复选框会被 Jsoup 清掉——与前端 DOMPurify 的 FORBID_TAGS 行为一致，
 * 两侧都只保留条目文字。安全回归由 {@code SafeMarkdownTest} 与共享 fixture 固定。
 */
@Component
public class SafeMarkdown {
    private static final List<Extension> EXTENSIONS = List.of(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListItemsExtension.create());

    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().extensions(EXTENSIONS)
            .escapeHtml(true).sanitizeUrls(true).build();
    private final Safelist allow = Safelist.relaxed().preserveRelativeLinks(true)
            .addTags("del")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    public String render(String source) {
        if (source == null || source.isEmpty()) return "";
        // Jsoup needs an absolute base to check relative URL protocols; keep their original spelling.
        return Jsoup.clean(renderer.render(parser.parse(source)), "https://selfblog.invalid/", allow);
    }
}
