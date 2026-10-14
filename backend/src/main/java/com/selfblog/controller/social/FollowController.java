package com.selfblog.controller.social;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.result.Result;
import com.selfblog.dto.social.FollowRequest;
import com.selfblog.service.social.FollowService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.social.FollowStatusVO;
import com.selfblog.vo.user.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 关注控制器
 */
@Tag(name = "关注管理", description = "关注相关接口")
@RestController
@RequestMapping("/follow")
@Validated
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /**
     * 关注用户
     */
    @RequiresAuth
    @Operation(summary = "关注用户")
    @PostMapping
    public Result<FollowStatusVO> follow(@Valid @RequestBody FollowRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (request == null || request.getTargetUserId() == null) {
            throw new BusinessException(400, "目标用户ID不能为空");
        }
        FollowStatusVO result = followService.follow(request.getTargetUserId(), userId);
        return Result.success(result);
    }

    /**
     * 取消关注
     */
    @RequiresAuth
    @Operation(summary = "取消关注")
    @DeleteMapping("/{targetUserId}")
    public Result<FollowStatusVO> unfollow(@PathVariable Long targetUserId) {
        Long userId = SecurityUtils.getCurrentUserId();
        FollowStatusVO result = followService.unfollow(targetUserId, userId);
        return Result.success(result);
    }

    /**
     * 检查是否关注
     */
    @Operation(summary = "检查是否关注")
    @GetMapping("/{targetUserId}/status")
    public Result<FollowStatusVO> checkFollow(@PathVariable Long targetUserId) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        boolean following = false;
        if (userId != null) {
            following = followService.isFollowing(targetUserId, userId);
        }

        FollowStatusVO result = new FollowStatusVO();
        result.setFollowing(following);
        FollowService.FollowCountsVO targetCounts = followService.getCounts(targetUserId);
        result.setFollowerCount(targetCounts.getFollowerCount());
        result.setFollowingCount(targetCounts.getFollowingCount());

        return Result.success(result);
    }

    /**
     * 获取粉丝列表（分页）
     */
    @Operation(summary = "获取粉丝列表（分页）")
    @GetMapping("/followers/{userId}")
    public Result<IPage<UserVO>> getFollowers(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer page,
            @Parameter(description = "每页数量，默认10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        IPage<UserVO> followers = followService.getFollowers(userId, page, pageSize);
        return Result.success(followers);
    }

    /**
     * 获取关注列表（分页）
     */
    @Operation(summary = "获取关注列表（分页）")
    @GetMapping("/following/{userId}")
    public Result<IPage<UserVO>> getFollowing(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1", name = "pageNum") @Min(1) Integer page,
            @Parameter(description = "每页数量，默认10") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        IPage<UserVO> following = followService.getFollowing(userId, page, pageSize);
        return Result.success(following);
    }

    /**
     * 获取粉丝/关注数量
     */
    @Operation(summary = "获取粉丝/关注数量")
    @GetMapping("/counts/{userId}")
    public Result<FollowService.FollowCountsVO> getCounts(@PathVariable Long userId) {
        FollowService.FollowCountsVO counts = followService.getCounts(userId);
        return Result.success(counts);
    }
}