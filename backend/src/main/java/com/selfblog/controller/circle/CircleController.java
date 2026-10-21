package com.selfblog.controller.circle;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.result.Result;
import com.selfblog.dto.circle.CircleCommentRequest;
import com.selfblog.dto.circle.CirclePostCreateRequest;
import com.selfblog.dto.circle.CirclePostUpdateRequest;
import com.selfblog.service.circle.CircleInteractionService;
import com.selfblog.service.circle.CircleService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.circle.CircleCommentVO;
import com.selfblog.vo.circle.CircleLikeResultVO;
import com.selfblog.vo.circle.CirclePostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 校友圈控制器
 */
@Slf4j
@Tag(name = "校友圈", description = "校友圈动态相关接口")
@RestController
@RequestMapping("/circle")
@Validated
@RequiredArgsConstructor
public class CircleController {

    private final CircleService circleService;

    private final CircleInteractionService circleInteractionService;

    /**
     * 发布动态
     */
    @RequiresAuth
    @Operation(summary = "发布动态")
    @PostMapping("/post")
    public Result<Long> createPost(@Valid @RequestBody CirclePostCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Long postId = circleService.createPost(
                request.getContent(),
                request.getImages(),
                request.getVideos(),
                request.getLocation(),
                request.getRepostId(),
                userId,
                request.getVisibility(),
                request.getAllowComment(),
                request.getAllowRepost(),
                request.getTopicIds()
        );

        return Result.success(postId);
    }

    /**
     * 更新动态
     */
    @RequiresAuth
    @Operation(summary = "更新动态")
    @PutMapping("/post/{postId}")
    public Result<Void> updatePost(@PathVariable Long postId,
                                   @Valid @RequestBody CirclePostUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        circleService.updatePost(
                postId,
                request.getContent(),
                request.getImages(),
                request.getVideos(),
                request.getLocation(),
                request.getTopicIds(),
                request.getVisibility(),
                request.getAllowComment(),
                request.getAllowRepost(),
                userId
        );
        return Result.success(null);
    }

    /**
     * 删除动态
     */
    @RequiresAuth
    @Operation(summary = "删除动态")
    @DeleteMapping("/post/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();

        circleService.deletePost(postId, userId);
        return Result.success(null);
    }

    /**
     * 获取推荐流
     */
    @Operation(summary = "获取推荐流")
    @GetMapping("/feed/recommend")
    public Result<List<CirclePostVO>> getRecommendFeed(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        List<CirclePostVO> feed = circleService.getRecommendFeed(page, pageSize, userId);
        return Result.success(feed);
    }

    /**
     * 获取关注流
     */
    @RequiresAuth
    @Operation(summary = "获取关注流")
    @GetMapping("/feed/following")
    public Result<List<CirclePostVO>> getFollowingFeed(
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<CirclePostVO> feed = circleService.getFollowingFeed(page, pageSize, userId);
        return Result.success(feed);
    }

    /**
     * 获取动态详情
     *
     * <p>浏览量自增刻意放在读取之前、且在独立的写事务里（{@code incrementViewCount}），
     * 与 {@code BlogPostController.getPostDetail} 保持同形：读取本身是只读事务，若在
     * 只读事务内部另开写事务自增，调用方持有该行未提交写锁时会一直等到
     * innodb_lock_wait_timeout 后抛 CannotAcquireLockException。计数失败不应影响详情
     * 返回，故整体 try/catch 后继续。</p>
     */
    @Operation(summary = "获取动态详情")
    @GetMapping("/post/{postId}")
    public Result<CirclePostVO> getPostDetail(@PathVariable Long postId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        try {
            circleInteractionService.incrementViewCount(postId);
        } catch (Exception e) {
            log.warn("Failed to increment view count for circle post {}", postId, e);
        }
        CirclePostVO detail = circleService.getPostDetail(postId, userId);
        return Result.success(detail);
    }

    // ==================== 点赞相关接口 ====================

    /**
     * 点赞/取消点赞
     */
    @RequiresAuth
    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/like/{postId}")
    public Result<CircleLikeResultVO> toggleLike(@PathVariable Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        CircleLikeResultVO result = circleService.toggleLike(postId, userId);
        return Result.success(result);
    }

    /**
     * 检查是否已点赞
     */
    @Operation(summary = "检查是否已点赞")
    @GetMapping("/like/check/{postId}")
    public Result<Boolean> checkLikeStatus(@PathVariable Long postId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        Boolean isLiked = circleService.checkLikeStatus(postId, userId);
        return Result.success(isLiked);
    }

    // ==================== 评论相关接口 ====================

    /**
     * 获取动态评论列表
     */
    @Operation(summary = "获取动态评论列表")
    @GetMapping("/comment/{postId}")
    public Result<List<CircleCommentVO>> getComments(@PathVariable Long postId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        List<CircleCommentVO> comments = circleService.getComments(postId, userId);
        return Result.success(comments);
    }

    /**
     * 发表评论
     */
    @RequiresAuth
    @Operation(summary = "发表评论")
    @PostMapping("/comment")
    public Result<Long> createComment(@Valid @RequestBody CircleCommentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Long commentId = circleService.createComment(
                request.getPostId(),
                request.getContent(),
                request.getParentId(),
                request.getReplyToUserId(),
                userId
        );
        return Result.success(commentId);
    }

    /**
     * 删除评论
     */
    @RequiresAuth
    @Operation(summary = "删除评论")
    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        circleService.deleteComment(commentId, userId);
        return Result.success(null);
    }

    // ==================== 转发相关接口 ====================

    /**
     * 转发动态
     */
    @RequiresAuth
    @Operation(summary = "转发动态")
    @PostMapping("/repost/{postId}")
    public Result<Long> repostPost(
            @PathVariable Long postId,
            @RequestParam(required = false) String content) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long newPostId = circleService.repostPost(postId, content, userId);
        return Result.success(newPostId);
    }

    // ==================== 搜索相关接口 ====================

    /**
     * 搜索动态
     */
    @Operation(summary = "搜索动态")
    @GetMapping("/search")
    public Result<List<CirclePostVO>> searchPosts(
            @RequestParam @NotBlank String keyword,
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        List<CirclePostVO> results = circleService.searchPosts(keyword, page, pageSize, userId);
        return Result.success(results);
    }

    /**
     * 获取指定用户的动态列表
     */
    @Operation(summary = "获取用户动态列表")
    @GetMapping("/user/{userId}")
    public Result<IPage<CirclePostVO>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        IPage<CirclePostVO> results = circleService.getUserPosts(userId, page, pageSize, currentUserId);
        return Result.success(results);
    }

}