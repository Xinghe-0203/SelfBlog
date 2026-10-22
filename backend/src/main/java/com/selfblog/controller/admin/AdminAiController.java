package com.selfblog.controller.admin;

import com.selfblog.common.result.Result;
import com.selfblog.dto.ai.AiHealthReportRequest;
import com.selfblog.dto.ai.AiImageAltRequest;
import com.selfblog.dto.ai.AiModerationCheckRequest;
import com.selfblog.dto.ai.AiModerationScanRequest;
import com.selfblog.dto.ai.AiPostRequest;
import com.selfblog.dto.ai.AiProofreadBatchRequest;
import com.selfblog.service.ai.AiHealthReportService;
import com.selfblog.service.ai.AiImageAltService;
import com.selfblog.service.ai.AiModerationService;
import com.selfblog.service.ai.AiProofreadService;
import com.selfblog.service.ai.AiWritingService;
import com.selfblog.vo.ai.AiHealthReportVO;
import com.selfblog.vo.ai.AiImageAltVO;
import com.selfblog.vo.ai.AiModerationScanVO;
import com.selfblog.vo.ai.AiModerationVO;
import com.selfblog.vo.ai.AiProofreadBatchVO;
import com.selfblog.vo.ai.AiProofreadVO;
import com.selfblog.vo.ai.AiSummaryVO;
import com.selfblog.vo.ai.AiTagsVO;
import com.selfblog.vo.ai.AiTitlesVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台 AI 能力扩展接口（写作辅助、评论审核、校对、图片描述、站点体检）。
 *
 * <p>全部端点仅管理员可用；路径前缀 {@code /api} 由 {@code WebMvcConfig} 统一添加，
 * 所以这里只写 {@code /admin/ai}。所有端点返回 {@code Result<T>} 信封，
 * 且除评论审核外<b>不写任何业务表</b>——生成结果只是建议，由管理员在编辑页决定是否采用。</p>
 */
@Slf4j
@Tag(name = "后台-AI 能力")
@RestController
@RequestMapping("/admin/ai")
@Validated
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiWritingService writingService;
    private final AiModerationService moderationService;
    private final AiProofreadService proofreadService;
    private final AiImageAltService imageAltService;
    private final AiHealthReportService healthReportService;

    /** B：生成文章摘要。 */
    @Operation(summary = "生成文章摘要", description = "返回 80-200 字纯文本摘要，不写库")
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/writing/summary")
    public Result<AiSummaryVO> summary(@RequestBody(required = false) AiPostRequest request) {
        return Result.success(writingService.summary(postIdOf(request)));
    }

    /** B：从已有标签池推荐标签。 */
    @Operation(summary = "推荐文章标签", description = "只从已有标签池里挑 3-6 个，不写库")
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/writing/tags")
    public Result<AiTagsVO> tags(@RequestBody(required = false) AiPostRequest request) {
        return Result.success(writingService.tags(postIdOf(request)));
    }

    /** B：生成标题候选。 */
    @Operation(summary = "生成标题候选", description = "返回 3 个互不相同的标题，不写库")
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/writing/titles")
    public Result<AiTitlesVO> titles(@RequestBody(required = false) AiPostRequest request) {
        return Result.success(writingService.titles(postIdOf(request)));
    }

    /** C：单条评论审核。 */
    @Operation(summary = "审核单条评论", description = "kind 取 guest（访客留言）或 site（站内评论），缺省 guest")
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/moderation/check")
    public Result<AiModerationVO> moderationCheck(
            @RequestBody(required = false) AiModerationCheckRequest request) {
        AiModerationCheckRequest safe = request == null ? new AiModerationCheckRequest() : request;
        return Result.success(moderationService.check(safe.getCommentId(), safe.getKind()));
    }

    /** C：批量扫描公开访客留言。 */
    @Operation(summary = "批量扫描访客留言", description = "只扫已公开的访客留言，limit 默认 20 上限 50")
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/moderation/scan")
    public Result<AiModerationScanVO> moderationScan(
            @RequestBody(required = false) AiModerationScanRequest request) {
        Integer limit = request == null ? null : request.getLimit();
        return Result.success(moderationService.scan(limit));
    }

    /** D：单篇校对。 */
    @Operation(summary = "校对单篇文章", description = "只读文章正文，返回最多 20 条文字问题")
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/proofread/{postId}")
    public Result<AiProofreadVO> proofread(@PathVariable Long postId) {
        return Result.success(proofreadService.proofread(postId));
    }

    /** D：批量校对。 */
    @Operation(summary = "批量校对文章", description = "串行逐篇，逐篇隔离失败；postIds 缺省时取最近更新的已发布文章")
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/proofread/batch")
    public Result<AiProofreadBatchVO> proofreadBatch(
            @RequestBody(required = false) AiProofreadBatchRequest request) {
        AiProofreadBatchRequest safe = request == null ? new AiProofreadBatchRequest() : request;
        return Result.success(proofreadService.proofreadBatch(safe.getPostIds(), safe.getLimit()));
    }

    /** E：生成图片 alt 与图注。 */
    @Operation(summary = "生成图片描述", description = "source 取 cover（文章封面）或 album（相册照片），不写库")
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/images/alt")
    public Result<AiImageAltVO> imageAlt(@RequestBody(required = false) AiImageAltRequest request) {
        AiImageAltRequest safe = request == null ? new AiImageAltRequest() : request;
        return Result.success(imageAltService.describe(
                safe.getSource(), safe.getPostId(), safe.getPhotoId()));
    }

    /** F：站点内容体检。 */
    @Operation(summary = "站点内容体检", description = "采样已发布文章元数据，一次性生成体检报告；不写库")
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/health/report")
    public Result<AiHealthReportVO> healthReport(
            @RequestBody(required = false) AiHealthReportRequest request) {
        Integer limit = request == null ? null : request.getLimit();
        return Result.success(healthReportService.report(limit));
    }

    /** 三个写作辅助端点共用同一个请求体，缺省时给个明确提示而不是空指针。 */
    private static Long postIdOf(AiPostRequest request) {
        return request == null ? null : request.getPostId();
    }
}
