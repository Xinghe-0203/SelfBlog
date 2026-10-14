package com.selfblog.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.Message;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.BlogNotificationMapper;
import com.selfblog.mapper.MessageMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.social.MessageService;
import com.selfblog.vo.social.ConversationVO;
import com.selfblog.vo.social.MessageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessageServiceImplTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BlogNotificationMapper blogNotificationMapper;

    private SysUser sender;
    private SysUser receiver;

    @BeforeEach
    void setUp() {
        messageMapper.delete(null);
        blogNotificationMapper.delete(null);
        sysUserMapper.delete(null);

        sender = new SysUser();
        sender.setUsername("sender");
        sender.setPassword("password");
        sender.setNickname("Sender");
        sender.setRole("user");
        sender.setStatus(1);
        sysUserMapper.insert(sender);

        receiver = new SysUser();
        receiver.setUsername("receiver");
        receiver.setPassword("password");
        receiver.setNickname("Receiver");
        receiver.setRole("user");
        receiver.setStatus(1);
        sysUserMapper.insert(receiver);
    }

    @Test
    @DisplayName("发送私信成功")
    void sendMessage_Success() {
        MessageVO vo = messageService.sendMessage(sender.getId(), receiver.getId(), "你好");

        assertNotNull(vo);
        assertNotNull(vo.getId());
        assertEquals("你好", vo.getContent());
        assertNotNull(vo.getSender());
        assertNotNull(vo.getReceiver());
    }

    @Test
    @DisplayName("发送私信 - 未登录抛401")
    void sendMessage_NotLoggedIn_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.sendMessage(null, receiver.getId(), "你好"));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("发送私信 - 接收者ID为空抛400")
    void sendMessage_NullReceiver_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.sendMessage(sender.getId(), null, "你好"));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("发送私信 - 不能给自己发抛400")
    void sendMessage_Self_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.sendMessage(sender.getId(), sender.getId(), "你好"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不能给自己发私信"));
    }

    @Test
    @DisplayName("发送私信 - 内容为空抛400")
    void sendMessage_EmptyContent_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.sendMessage(sender.getId(), receiver.getId(), ""));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("发送私信 - 内容超长抛400")
    void sendMessage_ContentTooLong_ThrowsException() {
        String longContent = "a".repeat(2001);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.sendMessage(sender.getId(), receiver.getId(), longContent));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("发送私信 - 接收者不存在抛404")
    void sendMessage_ReceiverNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.sendMessage(sender.getId(), 99999L, "你好"));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("获取收到的私信列表")
    void getReceivedMessages_Success() {
        messageService.sendMessage(sender.getId(), receiver.getId(), "消息1");
        messageService.sendMessage(sender.getId(), receiver.getId(), "消息2");

        Page<MessageVO> page = messageService.getReceivedMessages(1, 10, receiver.getId());
        assertNotNull(page);
        assertEquals(2, page.getTotal());
    }

    @Test
    @DisplayName("获取发送的私信列表")
    void getSentMessages_Success() {
        messageService.sendMessage(sender.getId(), receiver.getId(), "消息1");

        Page<MessageVO> page = messageService.getSentMessages(1, 10, sender.getId());
        assertNotNull(page);
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("标记私信为已读")
    void markAsRead_Success() {
        MessageVO vo = messageService.sendMessage(sender.getId(), receiver.getId(), "你好");

        messageService.markAsRead(vo.getId(), receiver.getId());

        Message updated = messageMapper.selectById(vo.getId());
        assertEquals(1, updated.getIsRead());
    }

    @Test
    @DisplayName("标记不存在的私信为已读抛404")
    void markAsRead_NotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.markAsRead(99999L, receiver.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("非接收者标记私信为已读抛403")
    void markAsRead_NotReceiver_ThrowsException() {
        MessageVO vo = messageService.sendMessage(sender.getId(), receiver.getId(), "你好");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.markAsRead(vo.getId(), sender.getId()));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("删除私信成功 - 发送者删除")
    void deleteMessage_Sender_Success() {
        MessageVO vo = messageService.sendMessage(sender.getId(), receiver.getId(), "你好");

        messageService.deleteMessage(vo.getId(), sender.getId());

        Message deleted = messageMapper.selectById(vo.getId());
        assertNull(deleted);
    }

    @Test
    @DisplayName("删除私信成功 - 接收者删除")
    void deleteMessage_Receiver_Success() {
        MessageVO vo = messageService.sendMessage(sender.getId(), receiver.getId(), "你好");

        messageService.deleteMessage(vo.getId(), receiver.getId());

        Message deleted = messageMapper.selectById(vo.getId());
        assertNull(deleted);
    }

    @Test
    @DisplayName("删除不存在的私信抛404")
    void deleteMessage_NotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.deleteMessage(99999L, sender.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("非关联用户删除私信抛403")
    void deleteMessage_NotRelated_ThrowsException() {
        MessageVO vo = messageService.sendMessage(sender.getId(), receiver.getId(), "你好");

        SysUser other = new SysUser();
        other.setUsername("other");
        other.setPassword("password");
        other.setRole("user");
        other.setStatus(1);
        sysUserMapper.insert(other);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.deleteMessage(vo.getId(), other.getId()));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("获取未读私信数量")
    void getUnreadCount_Success() {
        messageService.sendMessage(sender.getId(), receiver.getId(), "消息1");
        messageService.sendMessage(sender.getId(), receiver.getId(), "消息2");

        Long count = messageService.getUnreadCount(receiver.getId());
        assertEquals(2L, count);
    }

    @Test
    @DisplayName("标记会话为已读")
    void markConversationAsRead_Success() {
        messageService.sendMessage(sender.getId(), receiver.getId(), "消息1");
        messageService.sendMessage(sender.getId(), receiver.getId(), "消息2");

        assertEquals(2L, messageService.getUnreadCount(receiver.getId()));

        messageService.markConversationAsRead(receiver.getId(), sender.getId());

        assertEquals(0L, messageService.getUnreadCount(receiver.getId()));
    }

    @Test
    @DisplayName("获取会话列表")
    void getConversations_Success() {
        messageService.sendMessage(sender.getId(), receiver.getId(), "你好");

        List<ConversationVO> conversations = messageService.getConversations(sender.getId());
        assertNotNull(conversations);
        assertEquals(1, conversations.size());
        assertEquals(receiver.getId(), conversations.get(0).getConversationId());
    }

    @Test
    @DisplayName("获取会话消息列表")
    void getConversationMessages_Success() {
        messageService.sendMessage(sender.getId(), receiver.getId(), "消息1");
        messageService.sendMessage(receiver.getId(), sender.getId(), "回复1");

        Page<MessageVO> page = messageService.getConversationMessages(
                sender.getId(), receiver.getId(), 1, 10);
        assertNotNull(page);
        assertEquals(2, page.getTotal());
    }
}
