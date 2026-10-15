package com.selfblog.controller.admin;

import com.selfblog.common.result.Result;
import com.selfblog.service.site.SiteAssetService;
import com.selfblog.vo.site.SiteAssetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 站点品牌资产上传接口。
 *
 * 单独开一条通道而不是复用 /media/upload：那条通道会用 thumbnailator
 * 把所有图片重新编码成 jpg，.ico / .svg / 音频都过不去。
 */
@Tag(name = "站点资源", description = "站点品牌资产上传")
@RestController
@RequestMapping("/admin/site-assets")
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminSiteAssetController {

    private final SiteAssetService siteAssetService;

    @Operation(summary = "上传站点品牌资产")
    @PostMapping
    public Result<SiteAssetVO> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam("kind") String kind) {
        return Result.success(siteAssetService.store(file, kind));
    }
}
