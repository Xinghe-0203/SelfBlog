package com.selfblog.service.seo;

/**
 * SEO 体检问题码。
 *
 * <p>枚举名经 Jackson 默认序列化为字符串（{@code missingDescription} 等），
 * 与前端 {@code frontend/src/types/seo.ts} 的 {@code SeoIssue} 联合类型逐一对应；
 * <b>改名等于破坏前端契约</b>，只允许新增。</p>
 */
public enum SeoIssue {

    /** 没有自定义描述，且文章也没有摘要 —— 前台只能截正文前 140 字。 */
    MISSING_DESCRIPTION("missingDescription"),

    /** 前台实际渲染的标题超过 60 字符，搜索结果会被截断。 */
    LONG_TITLE("longTitle"),

    /** 存在另一篇已发布文章使用了完全相同的标题。 */
    DUPLICATE_TITLE("duplicateTitle"),

    /** 既没有自定义分享图，也没有文章封面。 */
    MISSING_COVER("missingCover"),

    /** 完全没有 SEO 记录（自定义字段全空）。 */
    NO_SEO("noSeo");

    private final String code;

    SeoIssue(String code) {
        this.code = code;
    }

    /** 前端契约里使用的字符串值。 */
    public String code() {
        return code;
    }

    /** 按前端传来的字符串解析；未知值返回 {@code null}（表示不按问题过滤，而不是报错）。 */
    public static SeoIssue fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        for (SeoIssue issue : values()) {
            if (issue.code.equalsIgnoreCase(value) || issue.name().equalsIgnoreCase(value)) {
                return issue;
            }
        }
        return null;
    }
}
