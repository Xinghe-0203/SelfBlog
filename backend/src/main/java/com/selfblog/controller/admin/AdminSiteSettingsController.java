package com.selfblog.controller.admin;

import com.selfblog.common.result.Result;
import com.selfblog.service.site.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/site-settings")
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminSiteSettingsController {
    private final SiteSettingsService siteSettingsService;

    @GetMapping
    public Result<Map<String, String>> getSettings() {
        // 走掩码视图：密钥明文不出服务端。内部消费方仍用 getSettings()。
        return Result.success(siteSettingsService.getAdminSettings());
    }

    @PutMapping
    public Result<Map<String, String>> updateSettings(@RequestBody Map<String, Object> changes) {
        return Result.success(siteSettingsService.updateSettings(changes));
    }
}
