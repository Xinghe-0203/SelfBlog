package com.selfblog.controller.social;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.annotation.RequiresAuth;
import com.selfblog.common.result.Result;
import com.selfblog.dto.social.SendMessageRequest;
import com.selfblog.service.social.MessageService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.social.ConversationVO;
import com.selfblog.vo.social.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 私信控制器
 */
@Tag(name = "私信管理", description = "私信相关接口")
@RestController
@RequestMapping("/message")
@Validated
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 发送私信
     */
    @RequiresAuth
    @Operation(summary = "发送私信")
    @PostMapping
    public Result<MessageVO> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long senderId = SecurityUtils.getCurrentUserId();

        MessageVO vo = messageService.sendMessage(
            senderId,
            request.getReceiverId(),
            request.getContent()
        );

        return Result.success(vo);
    }

    /**
     * 获取收到的私信列表（分页）
     */
    @RequiresAuth
    @Operation(summary = "获取收到的私信列表")
    @GetMapping("/received")
    public Result<IPage<MessageVO>> getReceivedMessages(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<MessageVO> pageResult = messageService.getReceivedMessages(pageNum, pageSize, userId);
        return Result.success(pageResult);
    }

    /**
     * 获取发送的私信列表（分页）
     */
    @RequiresAuth
    @Operation(summary = "获取发送的私信列表")
    @GetMapping("/sent")
    public Result<IPage<MessageVO>> getSentMessages(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<MessageVO> pageResult = messageService.getSentMessages(pageNum, pageSize, userId);
        return Result.success(pageResult);
    }

    /**
     * 标记私信为已读
     */
    @RequiresAuth
    @Operation(summary = "标记私信为已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        messageService.markAsRead(id, userId);
        return Result.success(null);
    }

    /**
     * 删除私信（软删除）
     */
    @RequiresAuth
    @Operation(summary = "删除私信")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        messageService.deleteMessage(id, userId);
        return Result.success(null);
    }

    /**
     * 获取未读私信数量
     */
    @RequiresAuth
    @Operation(summary = "获取未读私信数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long count = messageService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记会话中所有私信为已读
     */
    @RequiresAuth
    @Operation(summary = "标记会话中所有私信为已读")
    @PutMapping("/conversation/{partnerUserId}/read")
    public Result<Void> markConversationAsRead(@PathVariable Long partnerUserId) {
        Long userId = SecurityUtils.getCurrentUserId();
        messageService.markConversationAsRead(userId, partnerUserId);
        return Result.success(null);
    }

    /**
     * 获取会话列表
     */
    @RequiresAuth
    @Operation(summary = "获取会话列表")
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ConversationVO> conversations = messageService.getConversations(userId);
        return Result.success(conversations);
    }

    /**
     * 获取与指定用户的聊天记录
     */
    @RequiresAuth
    @Operation(summary = "获取与指定用户的聊天记录")
    @GetMapping("/conversation/{partnerUserId}")
    public Result<IPage<MessageVO>> getConversationMessages(
            @PathVariable Long partnerUserId,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<MessageVO> messages = messageService.getConversationMessages(userId, partnerUserId, pageNum, pageSize);
        return Result.success(messages);
    }
}