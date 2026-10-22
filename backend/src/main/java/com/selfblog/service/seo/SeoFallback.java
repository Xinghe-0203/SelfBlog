package com.selfblog.service.seo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 「自定义 SEO → 派生值」的唯一回落实现。
 *
 * <p>后台预览（{@code effectiveTitle}/{@code effectiveDescription}）与公开阅读页
 * （{@link com.selfblog.service.site.seo.ReadingPageService}）都调用这里，
 * 因此预览不可能和真实渲染漂移。任何回落规则要改只改这一处。</p>
 */
public final class SeoFallback {

    /** 正文派生描述的最大长度（沿用改造前的 140 字符）。 */
    public static final int DERIVED_DESCRIPTION_LIMIT = 140;

    private SeoFallback() {
    }

    /** 标题回落：自定义标题优先，否则文章标题。 */
    public static String title(String seoTitle, String postTitle) {
        return firstNonBlank(seoTitle, postTitle);
    }

    /**
     * 描述回落：自定义描述 → 文章摘要 → 正文纯文本截断 140 字符。
     *
     * @param plainBody 已去标签的正文纯文本；调用方拿不到正文时可传 {@code null}
     */
    public static String description(String seoDescription, String summary, String plainBody) {
        String value = firstNonBlank(seoDescription, summary);
        if (value != null) {
            return value;
        }
        if (plainBody == null || plainBody.isBlank()) {
            return null;
        }
        return plainBody.length() > DERIVED_DESCRIPTION_LIMIT
                ? plainBody.substring(0, DERIVED_DESCRIPTION_LIMIT) : plainBody;
    }

    /**
     * 关键词回落：自定义关键词（逗号分隔，中英文逗号都接受）优先，否则用文章标签。
     *
     * @return 去重后的关键词列表，永不为 {@code null}
     */
    public static List<String> keywords(String seoKeywords, List<String> tags) {
        List<String> parsed = splitKeywords(seoKeywords);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        return tags == null ? List.of() : tags.stream().filter(SeoFallback::isPresent).distinct().toList();
    }

    /** 把存储用的逗号分隔关键词拆成列表（去空、去重，保序）。 */
    public static List<String> splitKeywords(String seoKeywords) {
        if (seoKeywords == null || seoKeywords.isBlank()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String part : seoKeywords.split("[,，]")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return new ArrayList<>(values);
    }

    /** 分享图回落：SEO 指定 → 文章封面 → 站点 Logo。 */
    public static String ogImage(String ogImageUrl, String coverUrl, String siteLogoUrl) {
        return firstNonBlank(ogImageUrl, coverUrl, siteLogoUrl);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (isPresent(value)) {
                return value;
            }
        }
        return null;
    }
}
