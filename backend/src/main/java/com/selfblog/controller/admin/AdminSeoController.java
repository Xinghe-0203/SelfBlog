package com.selfblog.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.result.Result;
import com.selfblog.dto.seo.SeoBatchRequest;
import com.selfblog.dto.seo.SeoUpdateRequest;
import com.selfblog.service.seo.SeoAuditService;
import com.selfblog.service.seo.SeoGenerationService;
import com.selfblog.service.seo.SeoIssue;
import com.selfblog.service.seo.SeoMetadataService;
import com.selfblog.vo.seo.SeoBatchResultVO;
import com.selfblog.vo.seo.SeoDetailVO;
import com.selfblog.vo.seo.SeoLlmTestVO;
import com.selfblog.vo.seo.SeoLlmModelsVO;
import com.selfblog.vo.seo.SeoOverviewVO;
import com.selfblog.vo.seo.SeoPostRowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员 SEO 管理控制器。
 *
 * <p>路径不写 {@code /api} 前缀：{@code WebMvcConfig} 会给所有 {@code @RestController}
 * 自动加前缀，源码里再写一遍会变成 {@code /api/api/...}。</p>
 *
 * <p>安全：整个 {@code /api/admin/**} 在 {@code SecurityConfig} 里已要求 admin，
 * 这里再逐个方法标 {@code @PreAuthorize}，与其余管理端控制器保持一致（双保险，
 * 也让方法级单测能验证 403）。</p>
 */
@Slf4j
@Tag(name = "管理员-SEO 管理", description = "文章 SEO 元数据的体检、生成与维护")
@RestController
@RequestMapping("/admin/seo")
@Validated
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminSeoController {

    private final SeoAuditService auditService;
    private final SeoGenerationService generationService;
    private final SeoMetadataService metadataService;

    /** 体检概览：覆盖率与各类问题计数，附带当前模型配置状态。 */
    @Operation(summary = "SEO 体检概览")
    @GetMapping("/overview")
    @PreAuthorize("hasRole('admin')")
    public Result<SeoOverviewVO> overview() {
        return Result.success(auditService.overview());
    }

    /**
     * 文章 SEO 列表（分页）。
     *
     * @param issue 只保留含该问题的文章；未知值按「不过滤」处理（不报错），
     *              这样前端拼 URL 时不必先校验枚举
     */
    @Operation(summary = "文章 SEO 列表")
    @GetMapping("/posts")
    @PreAuthorize("hasRole('admin')")
    public Result<Page<SeoPostRowVO>> posts(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10", name = "pageSize") @Min(1) @Max(100) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String issue) {
        return Result.success(auditService.page(pageNum, pageSize, keyword, SeoIssue.fromCode(issue)));
    }

    /**
     * 单篇文章 SEO 详情。
     *
     * <p>没有 SEO 记录时返回 404 —— 后台「文章编辑」页据此判断「尚未填写」，
     * 而不是显示一个空表单加错误提示。</p>
     */
    @Operation(summary = "文章 SEO 详情")
    @GetMapping("/posts/{postId}")
    @PreAuthorize("hasRole('admin')")
    public Result<SeoDetailVO> detail(@PathVariable Long postId) {
        return Result.success(auditService.detail(postId));
    }

    /** 手工保存 SEO 元数据（整组覆盖；空白字段落库为 NULL，回落到派生值）。 */
    @Operation(summary = "保存文章 SEO 元数据")
    @PutMapping("/posts/{postId}")
    @PreAuthorize("hasRole('admin')")
    public Result<SeoDetailVO> save(@PathVariable Long postId, @RequestBody(required = false) SeoUpdateRequest request) {
        return Result.success(metadataService.save(postId, request));
    }

    /** 清除 SEO 元数据（幂等），前台立即回落到派生标题/描述。 */
    @Operation(summary = "清除文章 SEO 元数据")
    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> delete(@PathVariable Long postId) {
        metadataService.delete(postId);
        return Result.success(null);
    }

    /** 单篇生成（覆盖手填内容，source 记为 llm）。 */
    @Operation(summary = "生成文章 SEO 元数据")
    @PostMapping("/posts/{postId}/generate")
    @PreAuthorize("hasRole('admin')")
    public Result<SeoDetailVO> generate(@PathVariable Long postId) {
        return Result.success(generationService.generate(postId));
    }

    /** 批量生成（串行，逐篇隔离失败）。 */
    @Operation(summary = "批量生成 SEO 元数据")
    @PostMapping("/batch")
    @PreAuthorize("hasRole('admin')")
    public Result<SeoBatchResultVO> batch(@RequestBody(required = false) SeoBatchRequest request) {
        return Result.success(generationService.batch(request == null ? new SeoBatchRequest() : request));
    }

    /**
     * 测试模型连通性。
     *
     * <p>永远返回 200 + {@code ok=false}：这是「诊断」接口，上游 401/超时属于正常诊断结果，
     * 用 5xx 表达会让前端只能拿到笼统的错误文案。</p>
     */
    @Operation(summary = "测试模型连通性")
    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('admin')")
    public Result<SeoLlmTestVO> testConnection() {
        return Result.success(generationService.testConnection());
    }

    /**
     * 模型候选清单（供设置页的下拉框）。
     *
     * <p>恒返回 200：上游没有 {@code GET /models} 或暂时不可达时回落到内置候选，
     * {@code source=fallback} 加 {@code reason} 说明原因。前端仍需允许手填模型名。</p>
     */
    @Operation(summary = "模型候选清单")
    @GetMapping("/models")
    @PreAuthorize("hasRole('admin')")
    public Result<SeoLlmModelsVO> models() {
        return Result.success(generationService.listModels());
    }
}
