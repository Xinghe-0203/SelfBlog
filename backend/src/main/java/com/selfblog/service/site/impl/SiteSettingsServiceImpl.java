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
            "faviconUrl", "siteLogoUrl", "siteBaseUrl"
    );
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
        return values;
    }

    private String normalizeAdminPath(String value) {
        String path = value == null ? "admin" : value.trim().replaceAll("^/+|/+$", "");
        return path.matches("[A-Za-z0-9][A-Za-z0-9_-]{1,39}") ? path : "admin";
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
            default -> { }
        }
    }
}
