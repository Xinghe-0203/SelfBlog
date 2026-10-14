package com.selfblog.service.site;

import java.util.Map;

public interface SiteSettingsService {
    Map<String, String> getSettings();
    Map<String, String> getPublicSettings();
    Map<String, String> updateSettings(Map<String, ?> changes);
    boolean isCommentsEnabled();
    boolean isVisitorTrackingEnabled();
}
