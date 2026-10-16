package com.selfblog.service.site.seo;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/** Static public reading only; raw author HTML is never a trusted template. */
@Component
public class SafeMarkdown {
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();
    private final Safelist allow = Safelist.relaxed().preserveRelativeLinks(true)
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    public String render(String source) {
        if (source == null || source.isEmpty()) return "";
        // Jsoup needs an absolute base to check relative URL protocols; keep their original spelling.
        return Jsoup.clean(renderer.render(parser.parse(source)), "https://selfblog.invalid/", allow);
    }
}
