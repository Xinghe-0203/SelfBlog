package com.selfblog.service.social.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.UserStatus;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.entity.BlogNotification;
import com.selfblog.entity.Message;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.BlogNotificationMapper;
import com.selfblog.mapper.MessageMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.social.MessageService;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.utils.TimeUtils;
import com.selfblog.utils.UserConverter;
import com.selfblog.vo.social.ConversationVO;
import com.selfblog.vo.social.MessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 私信服务实现类
 */
@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BlogNotificationMapper blogNotificationMapper;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Autowired
    private CacheManager cacheManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadMsg:' + #receiverId"),
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'conversations:' + #receiverId"),
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'conversations:' + #senderId")
    })
    public MessageVO sendMessage(Long senderId, Long receiverId, String content) {
        if (senderId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (receiverId == null) {
            throw new BusinessException(400, "接收者ID不能为空");
        }
        if (Objects.equals(senderId, receiverId)) {
            throw new BusinessException(400, "不能给自己发私信");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "私信内容不能为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException(400, "私信内容不能超过2000字符");
        }

        // 校验接收者是否存在且未被封禁
        SysUser receiver = sysUserMapper.selectById(receiverId);
        if (receiver == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (receiver.getStatus() != null && receiver.getStatus() == UserStatus.DISABLED.getValue()) {
            throw new BusinessException(400, "该用户已被封禁，无法发送私信");
        }
        SysUser sender = sysUserMapper.selectById(senderId);

        // 创建私信
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(htmlSanitizer.sanitizePlainText(content));
        message.setIsRead(0);

        this.save(message);

        log.info("[AUDIT] 私信发送: messageId={}, senderId={}, receiverId={}, contentLength={}",
                message.getId(), senderId, receiverId, content.length());

        if (sender != null) {
            String notificationType = "MESSAGE";
            String title = "收到新私信";
            String notificationContent = sender.getNickname() + " 给你发送了一条私信";
            Long msgId = message.getId();
            Long sndId = senderId;
            Long rcvId = receiverId;
            String nt = notificationType;
            String t = title;
            String nc = notificationContent;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sendNotificationIfEnabled(nt, t, nc, sndId, rcvId, "MESSAGE", msgId);
                    }
                });
            } else {
                sendNotificationIfEnabled(nt, t, nc, sndId, rcvId, "MESSAGE", msgId);
            }
        }

        // 转换为VO
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        vo.setTimeAgo(TimeUtils.getTimeAgo(message.getCreateTime()));
        if (sender != null) {
            vo.setSender(UserConverter.toUserVO(sender));
        }
        if (receiver != null) {
            vo.setReceiver(UserConverter.toUserVO(receiver));
        }
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageVO> getReceivedMessages(Integer pageNum, Integer pageSize, Long userId) {
        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
               .orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = this.page(page, wrapper);

        Page<MessageVO> voPage = new Page<>(messagePage.getCurrent(), messagePage.getSize(), messagePage.getTotal());
        voPage.setRecords(convertToVOList(messagePage.getRecords(), true));

        return voPage;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageVO> getSentMessages(Integer pageNum, Integer pageSize, Long userId) {
        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSenderId, userId)
               .orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = this.page(page, wrapper);

        Page<MessageVO> voPage = new Page<>(messagePage.getCurrent(), messagePage.getSize(), messagePage.getTotal());
        voPage.setRecords(convertToVOList(messagePage.getRecords(), false));

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadMsg:' + #userId"),
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'conversations:' + #userId")
    })
    public void markAsRead(Long messageId, Long userId) {
        Message message = this.getById(messageId);
        if (message == null) {
            throw new BusinessException(404, "私信不存在");
        }
        // 接收者才能标记已读
        if (!Objects.equals(message.getReceiverId(), userId)) {
            throw new BusinessException(403, "无权操作此私信");
        }
        message.setIsRead(1);
        this.updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadMsg:' + #userId"),
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'conversations:' + #userId")
    })
    public void deleteMessage(Long messageId, Long userId) {
        Message message = this.getById(messageId);
        if (message == null) {
            throw new BusinessException(404, "私信不存在");
        }
        // 发送者或接收者都可以删除
        if (!Objects.equals(message.getSenderId(), userId) && !Objects.equals(message.getReceiverId(), userId)) {
            throw new BusinessException(403, "无权删除此私信");
        }
        // 清除对方的未读消息缓存
        Long partnerId = Objects.equals(message.getSenderId(), userId) ? message.getReceiverId() : message.getSenderId();
        if (cacheManager.getCache(CaffeineCacheConfig.USER_CACHE) != null) {
            cacheManager.getCache(CaffeineCacheConfig.USER_CACHE).evict("unreadMsg:" + partnerId);
            cacheManager.getCache(CaffeineCacheConfig.USER_CACHE).evict("conversations:" + partnerId);
        }
        // 逻辑删除
        this.removeById(messageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadMsg:' + #userId"),
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'conversations:' + #userId"),
            @CacheEvict(value = CaffeineCacheConfig.USER_CACHE, key = "'conversations:' + #partnerUserId")
    })
    public void markConversationAsRead(Long userId, Long partnerUserId) {
        // 检查是否存在会话（双方互发过消息）
        LambdaQueryWrapper<Message> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.and(w -> w.eq(Message::getSenderId, userId).eq(Message::getReceiverId, partnerUserId)
                .or(w2 -> w2.eq(Message::getSenderId, partnerUserId).eq(Message::getReceiverId, userId)));
        long conversationCount = this.count(existWrapper);
        if (conversationCount == 0) {
            return;
        }

        // 标记用户收到的未读消息为已读
        lambdaUpdate().set(Message::getIsRead, 1)
                .eq(Message::getReceiverId, userId)
                .eq(Message::getSenderId, partnerUserId)
                .eq(Message::getIsRead, 0)
                .update();
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.USER_CACHE, key = "'unreadMsg:' + #userId")
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
               .eq(Message::getIsRead, 0);
        return this.count(wrapper);
    }

    @Override
    @Cacheable(value = CaffeineCacheConfig.USER_CACHE, key = "'conversations:' + #userId", unless = "#result.size() > 100")
    @Transactional(readOnly = true)
    public List<ConversationVO> getConversations(Long userId) {
        // 查询当前用户发送或接收的所有消息，限制最近30天
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(Message::getSenderId, userId)
                .or()
                .eq(Message::getReceiverId, userId)
        );
        wrapper.ge(Message::getCreateTime, java.time.LocalDateTime.now().minusDays(30));
        wrapper.orderByDesc(Message::getCreateTime);
        wrapper.last("LIMIT 1000");

        List<Message> allMessages = this.list(wrapper);
        if (allMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // 按会话伙伴分组
        Map<Long, List<Message>> conversationMap = new HashMap<>();
        for (Message message : allMessages) {
            // 确定会话伙伴ID（对方）
            Long partnerId = Objects.equals(message.getSenderId(), userId)
                    ? message.getReceiverId()
                    : message.getSenderId();
            conversationMap.computeIfAbsent(partnerId, k -> new ArrayList<>()).add(message);
        }

        // 收集所有用户ID
        Set<Long> userIds = conversationMap.keySet();
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            sysUserMapper.selectBatchIds(userIds)
                    .forEach(user -> userMap.put(user.getId(), user));
        }

        // 构建会话列表
        List<ConversationVO> conversations = new ArrayList<>();
        for (Map.Entry<Long, List<Message>> entry : conversationMap.entrySet()) {
            Long partnerId = entry.getKey();
            List<Message> messages = entry.getValue();

            // 按时间排序获取最新消息
            messages.sort((a, b) -> {
                if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
                if (a.getCreateTime() == null) return 1;
                if (b.getCreateTime() == null) return -1;
                return b.getCreateTime().compareTo(a.getCreateTime());
            });
            Message lastMessage = messages.get(0);

            // 计算未读数（当前用户是接收者且未读的消息）
            long unreadCount = messages.stream()
                    .filter(m -> Objects.equals(m.getReceiverId(), userId) && m.getIsRead() == 0)
                    .count();

            // 获取伙伴用户信息
            SysUser partner = userMap.get(partnerId);

            ConversationVO vo = new ConversationVO();
            vo.setConversationId(partnerId);
            if (partner != null) {
                vo.setUser(UserConverter.toUserVO(partner));
            }
            vo.setLastMessage(lastMessage.getContent());
            vo.setLastMessageTime(lastMessage.getCreateTime());
            vo.setTimeAgo(TimeUtils.getTimeAgo(lastMessage.getCreateTime()));
            vo.setUnreadCount((int) unreadCount);
            vo.setMessageCount(messages.size());

            conversations.add(vo);
        }

        // 按最后消息时间排序
        conversations.sort((a, b) -> {
            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });
        return conversations;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageVO> getConversationMessages(Long userId, Long partnerUserId, Integer pageNum, Integer pageSize) {
        Page<Message> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        // 查询当前用户与伙伴用户之间的所有消息（双方都算）
        wrapper.and(w -> w
                .eq(Message::getSenderId, userId).eq(Message::getReceiverId, partnerUserId)
                .or()
                .eq(Message::getSenderId, partnerUserId).eq(Message::getReceiverId, userId)
        );
        wrapper.orderByDesc(Message::getCreateTime);

        Page<Message> messagePage = this.page(page, wrapper);

        List<MessageVO> voList = convertToVOList(messagePage.getRecords(), userId);
        Collections.reverse(voList);

        Page<MessageVO> voPage = new Page<>(messagePage.getCurrent(), messagePage.getSize(), messagePage.getTotal());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 批量转换实体为VO
     * @param messages 私信列表
     * @param isReceived 是否是收到的私信（用于判断是否需要显示receiver信息）
     */
    private List<MessageVO> convertToVOList(List<Message> messages, boolean isReceived) {
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有用户ID
        Set<Long> userIds = new HashSet<>();
        for (Message message : messages) {
            userIds.add(message.getSenderId());
            userIds.add(message.getReceiverId());
        }

        // 批量查询用户信息
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            sysUserMapper.selectBatchIds(userIds)
                    .forEach(user -> userMap.put(user.getId(), user));
        }

        // 转换VO
        return messages.stream().map(message -> {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(message, vo);
            vo.setTimeAgo(TimeUtils.getTimeAgo(message.getCreateTime()));

            // 设置发送者信息
            SysUser sender = userMap.get(message.getSenderId());
            if (sender != null) {
                vo.setSender(UserConverter.toUserVO(sender));
            }

            // 设置接收者信息
            SysUser receiver = userMap.get(message.getReceiverId());
            if (receiver != null) {
                vo.setReceiver(UserConverter.toUserVO(receiver));
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 批量转换实体为VO（用于会话消息，双向显示）
     * @param messages 私信列表
     * @param userId 当前用户ID（用于标识，但不影响显示）
     */
    private List<MessageVO> convertToVOList(List<Message> messages, Long userId) {
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有用户ID
        Set<Long> userIds = new HashSet<>();
        for (Message message : messages) {
            userIds.add(message.getSenderId());
            userIds.add(message.getReceiverId());
        }

        // 批量查询用户信息
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            sysUserMapper.selectBatchIds(userIds)
                    .forEach(user -> userMap.put(user.getId(), user));
        }

        // 转换VO
        return messages.stream().map(message -> {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(message, vo);
            vo.setTimeAgo(TimeUtils.getTimeAgo(message.getCreateTime()));

            // 设置发送者信息
            SysUser sender = userMap.get(message.getSenderId());
            if (sender != null) {
                vo.setSender(UserConverter.toUserVO(sender));
            }

            // 设置接收者信息
            SysUser receiver = userMap.get(message.getReceiverId());
            if (receiver != null) {
                vo.setReceiver(UserConverter.toUserVO(receiver));
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 发送系统通知（可选功能）
     */
    private void sendNotificationIfEnabled(String type, String title, String content, Long fromUserId, Long toUserId, String targetType, Long targetId) {
        try {
            // 不通知自己
            if (fromUserId != null && fromUserId.equals(toUserId)) {
                return;
            }

            // 校验目标用户是否存在
            if (toUserId != null) {
                SysUser targetUser = sysUserMapper.selectById(toUserId);
                if (targetUser == null) {
                    return;
                }
            }

            // 创建通知记录
            BlogNotification notification = new BlogNotification();
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(htmlSanitizer.sanitizePlainText(content));
            notification.setFromUserId(fromUserId);
            notification.setToUserId(toUserId);
            notification.setTargetType(targetType);
            notification.setTargetId(targetId);
            notification.setIsRead(0);

            // 保存通知
            blogNotificationMapper.insert(notification);

        } catch (Exception e) {
            // 通知发送失败不影响私信功能
            log.warn("发送通知失败: type={}, targetType={}, targetId={}, error={}", type, targetType, targetId, e.getMessage());
        }
    }
}