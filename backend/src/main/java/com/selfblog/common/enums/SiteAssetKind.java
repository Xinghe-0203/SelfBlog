package com.selfblog.common.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * 站点品牌资产种类。
 *
 * 与 MediaService 分开的原因：媒体通道会用 thumbnailator 把所有图片重新编码成 jpg
 * （见 MediaServiceImpl），.ico、.svg 与音频走不通；而 favicon 需要 .ico、
 * logo 需要 .svg、背景音乐根本不是图片。
 *
 * 每种资产自带扩展名白名单与体积上限：品牌图标只用于显示，音乐可能几十 MB，
 * 共用一套阈值必然二选一地被浪费。
 */
public enum SiteAssetKind {
    /** 顶栏品牌商标，允许矢量图。 */
    LOGO("logo", Category.IMAGE, Set.of(".png", ".jpg", ".jpeg", ".webp", ".svg"), 2L * 1024 * 1024),
    /** 浏览器标签页图标，需要 .ico，也允许 .svg。 */
    FAVICON("favicon", Category.IMAGE, Set.of(".png", ".ico", ".svg"), 2L * 1024 * 1024),
    /** 站长头像，允许矢量图。 */
    AVATAR("avatar", Category.IMAGE, Set.of(".png", ".jpg", ".jpeg", ".webp", ".svg"), 2L * 1024 * 1024),
    /** 背景音乐。 */
    MUSIC("music", Category.AUDIO, Set.of(".mp3", ".ogg", ".wav", ".m4a", ".aac"), 20L * 1024 * 1024);

    /** 资产大类，决定用哪套魔数校验。 */
    public enum Category {
        IMAGE, AUDIO
    }

    private final String wireName;
    private final Category category;
    private final Set<String> extensions;
    private final long maxBytes;

    SiteAssetKind(String wireName, Category category, Set<String> extensions, long maxBytes) {
        this.wireName = wireName;
        this.category = category;
        this.extensions = extensions;
        this.maxBytes = maxBytes;
    }

    public String getWireName() {
        return wireName;
    }

    public Category getCategory() {
        return category;
    }

    /** 允许的扩展名，全部小写、带点号。 */
    public Set<String> getExtensions() {
        return extensions;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    /** 体积上限的兆字节表示，用于拼错误提示。 */
    public long getMaxMegabytes() {
        return maxBytes / (1024 * 1024);
    }

    public boolean isAudio() {
        return category == Category.AUDIO;
    }

    /** 供错误提示使用的可读格式列表，例如 ".jpg、.png、.svg"。顺序固定，便于断言。 */
    public String describeExtensions() {
        return String.join("、", extensions.stream().sorted().toList());
    }

    /**
     * 把请求里的 kind 字符串解析成枚举。大小写不敏感、容忍首尾空白；
     * 不认识的值返回 null，由调用方决定报什么错。
     */
    public static SiteAssetKind fromWireName(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(kind -> kind.wireName.equals(normalized))
                .findFirst()
                .orElse(null);
    }

    /**
     * 取文件名的小写扩展名（含点号）。
     * 没有扩展名、或点号在开头/结尾时返回空串 —— 那种文件名一律拒绝。
     */
    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        String base = filename.replaceAll("[/\\\\]", "_");
        int dot = base.lastIndexOf('.');
        if (dot <= 0 || dot == base.length() - 1) {
            return "";
        }
        return base.substring(dot).toLowerCase(Locale.ROOT);
    }
}
