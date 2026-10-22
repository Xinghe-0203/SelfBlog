package com.selfblog.service.site;

import java.util.Map;

public interface SiteSettingsService {
    Map<String, String> getSettings();

    Map<String, String> getPublicSettings();

    /**
     * 管理端读取：与 {@link #getSettings()} 相同，但密钥型设置被替换成掩码
     * （已配置）或空串（未配置）。密钥的明文永不离开服务端。
     */
    Map<String, String> getAdminSettings();

    Map<String, String> updateSettings(Map<String, ?> changes);

    boolean isCommentsEnabled();

    boolean isVisitorTrackingEnabled();
}
