package com.selfblog.controller.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.dto.site.PageModels.*;
import com.selfblog.service.site.PageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Author-only page resources. Strict decoding is local to this API, not a global Jackson change. */
@RestController
@PreAuthorize("hasRole('admin')")
public class AdminPageController {
    private final PageService pages;
    private final ObjectMapper json;
    public AdminPageController(PageService pages, ObjectMapper json) {
        this.pages = pages;
        this.json = json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    }
    @GetMapping("/admin/pages") public Result<List<PageEditor>> list() { return Result.success(pages.listPages()); }
    @GetMapping("/admin/pages/{id}") public Result<PageEditor> editor(@PathVariable String id) { return Result.success(pages.getEditor(id)); }
    @PostMapping("/admin/pages") public Result<PageEditor> create(@RequestBody JsonNode body) {
        var request = read(body, CreatePage.class, false);
        return Result.success(pages.createPage(request.slug(), request.title()));
    }
    @PutMapping("/admin/pages/{id}/draft") public Result<PageEditor> draft(@PathVariable String id, @RequestBody JsonNode body) {
        var request = read(body, SavePage.class, true);
        return Result.success(pages.saveDraft(id, request.revision(), request.slug(), request.config()));
    }
    @PutMapping("/admin/pages/{id}/publication") public Result<PageEditor> publish(@PathVariable String id, @RequestBody JsonNode body) {
        var request = read(body, Revision.class, true);
        return Result.success(pages.publish(id, request.revision()));
    }
    @DeleteMapping("/admin/pages/{id}/publication") public Result<PageEditor> unpublish(@PathVariable String id, @RequestParam long revision) {
        return Result.success(pages.unpublish(id, revision));
    }
    @DeleteMapping("/admin/pages/{id}") public Result<Void> delete(@PathVariable String id, @RequestParam long revision) {
        pages.deletePage(id, revision);
        return Result.success();
    }
    @GetMapping("/admin/page-navigation") public Result<NavigationState> navigation() { return Result.success(pages.getNavigation()); }
    @PutMapping("/admin/page-navigation") public Result<NavigationState> navigation(@RequestBody JsonNode body) {
        var request = read(body, SaveNavigation.class, true);
        return Result.success(pages.saveNavigation(request.revision(), request.items()));
    }
    private <T> T read(JsonNode body, Class<T> type, boolean revisionRequired) {
        if (!body.isObject()) throw new BusinessException(400, "页面请求需为JSON对象");
        if (revisionRequired && (!body.has("revision") || !body.get("revision").isIntegralNumber()
                || !body.get("revision").canConvertToLong() || body.get("revision").longValue() < 0)) {
            throw new BusinessException(400, "请提供有效的页面版本号");
        }
        try { return json.treeToValue(body, type); }
        catch (JsonProcessingException | IllegalArgumentException e) { throw new BusinessException(400, "页面字段或格式不受支持，请检查输入"); }
    }
}
