package com.selfblog.controller.site;

import com.selfblog.common.result.Result;
import com.selfblog.dto.site.PageModels.*;
import com.selfblog.service.site.PageService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/site/pages")
public class PublicPageController {
    private final PageService pages;
    public PublicPageController(PageService pages) { this.pages = pages; }
    @GetMapping public Result<PublicManifest> manifest() { return Result.success(pages.getManifest()); }
    @GetMapping("/{id}") public Result<PublicPage> page(@PathVariable String id) { return Result.success(pages.getPublished(id)); }
    @GetMapping("/by-slug/{slug}") public Result<PublicPage> slug(@PathVariable String slug) { return Result.success(pages.getBySlug(slug)); }
}
