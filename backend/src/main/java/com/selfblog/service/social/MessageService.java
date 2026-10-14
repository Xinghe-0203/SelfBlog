package com.selfblog.service.social;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.entity.Message;
import com.selfblog.vo.social.ConversationVO;
import com.selfblog.vo.social.MessageVO;

import java.util.List;

/**
 * 私信服务接口
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送私信
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param content 私信内容
     * @return 发送的私信
     */
    MessageVO sendMessage(Long senderId, Long receiverId, String content);

    /**
     * 获取收到的私信列表（分页）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param userId 当前用户ID
     * @return 分页私信列表
     */
    Page<MessageVO> getReceivedMessages(Integer pageNum, Integer pageSize, Long userId);

    /**
     * 获取发送的私信列表（分页）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param userId 当前用户ID
     * @return 分页私信列表
     */
    Page<MessageVO> getSentMessages(Integer pageNum, Integer pageSize, Long userId);

    /**
     * 标记私信为已读
     * @param messageId 私信ID
     * @param userId 当前用户ID
     */
    void markAsRead(Long messageId, Long userId);

    /**
     * 删除私信（软删除）
     * @param messageId 私信ID
     * @param userId 当前用户ID
     */
    void deleteMessage(Long messageId, Long userId);

    /**
     * 标记会话中所有私信为已读
     * @param userId 当前用户ID
     * @param partnerUserId 对方用户ID
     */
    void markConversationAsRead(Long userId, Long partnerUserId);

    /**
     * 获取未读私信数量
     * @param userId 当前用户ID
     * @return 未读数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 获取会话列表（按会话伙伴分组）
     * @param userId 当前用户ID
     * @return 会话列表
     */
    List<ConversationVO> getConversations(Long userId);

    /**
     * 获取与指定用户的聊天记录（分页）
     * @param userId 当前用户ID
     * @param partnerUserId 会话伙伴用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页消息列表
     */
    Page<MessageVO> getConversationMessages(Long userId, Long partnerUserId, Integer pageNum, Integer pageSize);
}