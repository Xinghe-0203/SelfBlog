package com.selfblog.service.site.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.SiteSetting;
import com.selfblog.mapper.SiteSettingMapper;
import com.selfblog.service.site.SiteSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SiteSettingsServiceImpl implements SiteSettingsService {
    private static final Set<String> ALLOWED_KEYS = Set.of(
            "siteTitle", "siteDescription", "authorName", "authorBio", "authorAvatar",
            "adminPath", "commentsEnabled", "visitorTrackingEnabled", "musicEnabled",
            "musicUrl", "musicTitle", "themeSkin", "themePrimaryColor", "footerText",
            "faviconUrl", "siteLogoUrl", "siteBaseUrl",
            "backgroundType", "backgroundColor", "backgroundGradientFrom", "backgroundGradientTo",
            "backgroundImageUrl", "backgroundOverlay", "backgroundFixed",
            "surfaceOpacity", "surfaceBlur"
    );
    /** 背景图可选的取色模式。 */
    private static final Set<String> BACKGROUND_TYPES = Set.of("solid", "gradient", "image");
    private static final Set<String> RESERVED_PATHS = Set.of(
            "api", "assets", "uploads", "post", "archives", "about", "search", "gallery",
            "tag", "pages", "login", "password-reset", "favicon.ico", "robots.txt"
    );

    private final SiteSettingMapper settingMapper;

    @Value("${ADMIN_PATH:admin}")
    private String configuredAdminPath;

    public SiteSettingsServiceImpl(SiteSettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.SITE_SETTINGS_CACHE, key = "'all'")
    @Transactional(readOnly = true)
    public Map<String, String> getSettings() {
        Map<String, String> settings = defaults();
        for (SiteSetting row : settingMapper.selectList(null)) {
            if (ALLOWED_KEYS.contains(row.getSettingKey()) && row.getSettingValue() != null) {
                settings.put(row.getSettingKey(), row.getSettingValue());
            }
        }
        return settings;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getPublicSettings() {
        return getSettings();
    }

    @Override
    @CacheEvict(value = CaffeineCacheConfig.SITE_SETTINGS_CACHE, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> updateSettings(Map<String, ?> changes) {
        if (changes == null || changes.isEmpty()) {
            throw new BusinessException(400, "请至少修改一项设置");
        }
        for (Map.Entry<String, ?> entry : changes.entrySet()) {
            String key = entry.getKey();
            if (!ALLOWED_KEYS.contains(key) || entry.getValue() == null) {
                throw new BusinessException(400, "包含不支持的设置项");
            }
            String value = String.valueOf(entry.getValue()).trim();
            validateValue(key, value);
            SiteSetting row = settingMapper.selectById(key);
            if (row == null) {
                row = new SiteSetting();
                row.setSettingKey(key);
                row.setSettingValue(value);
                settingMapper.insert(row);
            } else {
                row.setSettingValue(value);
                settingMapper.updateById(row);
            }
        }
        return getSettings();
    }

    @Override
    public boolean isCommentsEnabled() {
        return Boolean.parseBoolean(getSettings().get("commentsEnabled"));
    }

    @Override
    public boolean isVisitorTrackingEnabled() {
        return Boolean.parseBoolean(getSettings().get("visitorTrackingEnabled"));
    }

    private Map<String, String> defaults() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("siteTitle", "个人博客");
        values.put("siteDescription", "记录技术、生活与一路上的新发现。");
        values.put("authorName", "站长");
        values.put("authorBio", "欢迎来到我的个人博客。");
        values.put("authorAvatar", "");
        values.put("faviconUrl", "");
        values.put("siteLogoUrl", "");
        values.put("siteBaseUrl", "");
        values.put("adminPath", normalizeAdminPath(configuredAdminPath));
        values.put("commentsEnabled", "true");
        values.put("visitorTrackingEnabled", "true");
        values.put("musicEnabled", "false");
        values.put("musicUrl", "");
        values.put("musicTitle", "背景音乐");
        values.put("themeSkin", "ocean");
        values.put("themePrimaryColor", "#165DFF");
        values.put("footerText", "用文字记录生活，用好奇探索世界。");
        // 外观定制：默认值是「与改造前完全一致」的观感 —— 纯色浅灰背景 + 不透明卡片。
        values.put("backgroundType", "solid");
        values.put("backgroundColor", "#f5f6f8");
        values.put("backgroundGradientFrom", "#f5f6f8");
        values.put("backgroundGradientTo", "#e7eaf0");
        values.put("backgroundImageUrl", "");
        values.put("backgroundOverlay", "0");
        values.put("backgroundFixed", "true");
        values.put("surfaceOpacity", "100");
        values.put("surfaceBlur", "0");
        return values;
    }

    private String normalizeAdminPath(String value) {
        String path = value == null ? "admin" : value.trim().replaceAll("^/+|/+$", "");
        return path.matches("[A-Za-z0-9][A-Za-z0-9_-]{1,39}") ? path : "admin";
    }

    /**
     * 数值型设置的统一校验：必须是纯十进制整数（不接受小数、正号、前后缀），且落在闭区间内。
     * 范围与文案由调用方给出，便于每个键报自己的错。
     */
    private void requireIntegerInRange(String value, int min, int max, String message) {
        if (!value.matches("-?\\d{1,9}")) throw new BusinessException(400, message);
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, message);
        }
        if (number < min || number > max) throw new BusinessException(400, message);
    }

    private void validateValue(String key, String value) {
        if (value.length() > 1000) {
            throw new BusinessException(400, "设置内容不能超过1000个字符");
        }
        switch (key) {
            case "siteTitle", "authorName", "musicTitle" -> {
                if (value.isBlank() || value.length() > 80) throw new BusinessException(400, "名称不能为空且不能超过80个字符");
            }
            case "adminPath" -> {
                String path = normalizeAdminPath(value);
                if (!path.equals(value) || RESERVED_PATHS.contains(path.toLowerCase(Locale.ROOT))) {
                    throw new BusinessException(400, "后台路径需为2-40位字母、数字、短横线或下划线，且不能与公开页面冲突");
                }
            }
            case "commentsEnabled", "visitorTrackingEnabled", "musicEnabled" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) throw new BusinessException(400, "开关值必须为 true 或 false");
            }
            case "themeSkin" -> {
                if (!Set.of("ocean", "forest", "lavender", "sunset", "midnight").contains(value)) throw new BusinessException(400, "请选择有效的页面皮肤");
            }
            case "themePrimaryColor" -> {
                if (!value.matches("#[0-9a-fA-F]{6}")) throw new BusinessException(400, "主题色需使用 #RRGGBB 格式");
            }
            case "musicUrl", "authorAvatar", "faviconUrl", "siteLogoUrl" -> {
                if (!value.isBlank() && !(value.startsWith("https://") || value.startsWith("http://") || value.startsWith("/uploads/"))) {
                    throw new BusinessException(400, "请填写 http(s) 地址或站内 /uploads/ 文件路径");
                }
            }
            case "siteBaseUrl" -> {
                // 留空 = 未设置，回落到 SITE_BASE_URL/请求地址；填写时必须是站点 origin。
                if (value.isBlank()) return;
                try {
                    com.selfblog.service.site.seo.SiteOrigin.normalize(value);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException(400, "网站域名需为 http(s) 站点地址（如 https://blog.example.com），不要包含路径、参数或账号密码");
                }
            }
            case "backgroundType" -> {
                if (!BACKGROUND_TYPES.contains(value)) throw new BusinessException(400, "背景类型必须为 solid、gradient 或 image");
            }
            case "backgroundColor", "backgroundGradientFrom", "backgroundGradientTo" -> {
                if (!value.matches("#[0-9a-fA-F]{6}")) throw new BusinessException(400, "背景色需使用 #RRGGBB 格式");
            }
            case "backgroundImageUrl" -> {
                // 留空 = 未设置背景图（配合 backgroundType=solid/gradient 使用）。
                if (value.isEmpty()) return;
                // 这个值会被前端原样拼进 CSS url("…")，引号/括号/反斜杠能闭合或转义该字符串，
                // 空白则可用于截断声明 —— 都属于 CSS 注入面，一律拒绝。
                if (!(value.startsWith("https://") || value.startsWith("http://") || value.startsWith("/uploads/"))) {
                    throw new BusinessException(400, "背景图请填写 http(s) 地址或站内 /uploads/ 文件路径");
                }
                for (int i = 0; i < value.length(); i++) {
                    char c = value.charAt(i);
                    if (Character.isWhitespace(c) || c == '\u00A0' || c == '"' || c == '\'' || c == '(' || c == ')' || c == '\\') {
                        throw new BusinessException(400, "背景图地址不能包含空白、引号、括号或反斜杠");
                    }
                }
            }
            case "backgroundOverlay" -> requireIntegerInRange(value, 0, 90, "背景遮罩需为 0-90 之间的整数");
            case "backgroundFixed" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) throw new BusinessException(400, "背景固定开关值必须为 true 或 false");
            }
            case "surfaceOpacity" -> requireIntegerInRange(value, 60, 100, "卡片不透明度需为 60-100 之间的整数");
            case "surfaceBlur" -> requireIntegerInRange(value, 0, 40, "卡片模糊半径需为 0-40 之间的整数");
            default -> { }
        }
    }
}
