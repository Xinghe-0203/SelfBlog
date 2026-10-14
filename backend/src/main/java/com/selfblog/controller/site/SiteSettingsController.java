package com.selfblog.controller.site;

import com.selfblog.common.result.Result;
import com.selfblog.service.site.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/site")
@RequiredArgsConstructor
public class SiteSettingsController {
    private final SiteSettingsService siteSettingsService;

    @GetMapping("/public")
    public Result<Map<String, String>> getPublicSettings() {
        return Result.success(siteSettingsService.getPublicSettings());
    }
}
