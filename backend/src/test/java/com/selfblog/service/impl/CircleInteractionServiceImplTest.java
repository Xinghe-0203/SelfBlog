package com.selfblog.service.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.CircleComment;
import com.selfblog.entity.CirclePost;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.CircleCommentMapper;
import com.selfblog.mapper.CircleLikeMapper;
import com.selfblog.mapper.CirclePostMapper;
import com.selfblog.mapper.CircleRepostMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.circle.CircleInteractionService;
import com.selfblog.vo.circle.CircleCommentVO;
import com.selfblog.vo.circle.CircleLikeResultVO;
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
 * CircleInteractionServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CircleInteractionServiceImplTest {

    @Autowired
    private CircleInteractionService circleInteractionService;

    @Autowired
    private CirclePostMapper circlePostMapper;

    @Autowired
    private CircleLikeMapper circleLikeMapper;

    @Autowired
    private CircleCommentMapper circleCommentMapper;

    @Autowired
    private CircleRepostMapper circleRepostMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private SysUser userA;
    private SysUser userB;
    private CirclePost testPost;

    @BeforeEach
    void setUp() {
        circleLikeMapper.delete(null);
        circleCommentMapper.delete(null);
        circleRepostMapper.delete(null);
        circlePostMapper.delete(null);
        sysUserMapper.delete(null);

        userA = new SysUser();
        userA.setUsername("userA");
        userA.setPassword("password");
        userA.setNickname("User A");
        userA.setRole("user");
        userA.setStatus(1);
        sysUserMapper.insert(userA);

        userB = new SysUser();
        userB.setUsername("userB");
        userB.setPassword("password");
        userB.setNickname("User B");
        userB.setRole("user");
        userB.setStatus(1);
        sysUserMapper.insert(userB);

        testPost = new CirclePost();
        testPost.setUserId(userA.getId());
        testPost.setContent("测试动态内容");
        testPost.setContentType(1);
        testPost.setLikeCount(0);
        testPost.setCommentCount(0);
        testPost.setRepostCount(0);
        testPost.setViewCount(0L);
        testPost.setIsTop(0);
        testPost.setVisibility(0); // 公开
        testPost.setAllowComment(1);
        testPost.setAllowRepost(1);
        testPost.setStatus(1);
        circlePostMapper.insert(testPost);
    }

    // ==================== 点赞测试 ====================

    @Test
    @DisplayName("点赞动态成功")
    void toggleLike_Success() {
        CircleLikeResultVO result = circleInteractionService.toggleLike(testPost.getId(), userB.getId());

        assertNotNull(result);
        assertEquals("like", result.getAction());
        assertEquals(1, result.getLikeCount());
    }

    @Test
    @DisplayName("取消点赞成功")
    void toggleUnlike_Success() {
        circleInteractionService.toggleLike(testPost.getId(), userB.getId());
        CircleLikeResultVO result = circleInteractionService.toggleLike(testPost.getId(), userB.getId());

        assertNotNull(result);
        assertEquals("unlike", result.getAction());
        assertEquals(0, result.getLikeCount());
    }

    @Test
    @DisplayName("点赞不存在的动态抛404")
    void toggleLike_PostNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> circleInteractionService.toggleLike(99999L, userB.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("检查点赞状态 - 已点赞")
    void checkLikeStatus_Liked() {
        circleInteractionService.toggleLike(testPost.getId(), userB.getId());
        assertTrue(circleInteractionService.checkLikeStatus(testPost.getId(), userB.getId()));
    }

    @Test
    @DisplayName("检查点赞状态 - 未点赞")
    void checkLikeStatus_NotLiked() {
        assertFalse(circleInteractionService.checkLikeStatus(testPost.getId(), userB.getId()));
    }

    @Test
    @DisplayName("检查点赞状态 - userId为null返回false")
    void checkLikeStatus_NullUserId() {
        assertFalse(circleInteractionService.checkLikeStatus(testPost.getId(), null));
    }

    // ==================== 评论测试 ====================

    @Test
    @DisplayName("发表评论成功")
    void createComment_Success() {
        Long commentId = circleInteractionService.createComment(
                testPost.getId(), "好文！", null, null, userB.getId());

        assertNotNull(commentId);

        CircleComment saved = circleCommentMapper.selectById(commentId);
        assertEquals("好文！", saved.getContent());
    }

    @Test
    @DisplayName("发表评论 - 回复成功")
    void createComment_Reply_Success() {
        Long parentCommentId = circleInteractionService.createComment(
                testPost.getId(), "好文！", null, null, userB.getId());

        Long replyId = circleInteractionService.createComment(
                testPost.getId(), "同意！", parentCommentId, userB.getId(), userA.getId());

        assertNotNull(replyId);
        CircleComment reply = circleCommentMapper.selectById(replyId);
        assertEquals(parentCommentId, reply.getParentId());
    }

    @Test
    @DisplayName("发表评论 - 内容为空抛400")
    void createComment_EmptyContent_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> circleInteractionService.createComment(testPost.getId(), "", null, null, userB.getId()));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("发表评论 - 内容超长抛400")
    void createComment_ContentTooLong_ThrowsException() {
        String longContent = "a".repeat(501);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> circleInteractionService.createComment(testPost.getId(), longContent, null, null, userB.getId()));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("发表评论 - 动态不存在抛404")
    void createComment_PostNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> circleInteractionService.createComment(99999L, "评论", null, null, userB.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("发表评论 - 禁止评论的动态抛403")
    void createComment_CommentDisabled_ThrowsException() {
        testPost.setAllowComment(0);
        circlePostMapper.updateById(testPost);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> circleInteractionService.createComment(testPost.getId(), "评论", null, null, userB.getId()));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("获取评论列表")
    void getComments_Success() {
        circleInteractionService.createComment(testPost.getId(), "评论1", null, null, userB.getId());
        circleInteractionService.createComment(testPost.getId(), "评论2", null, null, userB.getId());

        List<CircleCommentVO> comments = circleInteractionService.getComments(testPost.getId(), userA.getId());
        assertNotNull(comments);
        assertEquals(2, comments.size());
    }

    @Test
    @DisplayName("删除不存在的评论抛404")
    void deleteComment_NotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> circleInteractionService.deleteComment(99999L, userB.getId()));
        assertEquals(404, ex.getCode());
    }

    // ==================== 转发测试 ====================

    @Test
    @DisplayName("转发动态成功")
    void repostPost_Success() {
        Long newPostId = circleInteractionService.repostPost(testPost.getId(), "转发评论", userB.getId());

        assertNotNull(newPostId);
        CirclePost newPost = circlePostMapper.selectById(newPostId);
        assertEquals(testPost.getId(), newPost.getRepostId());

        // 原动态转发数应增加
        CirclePost updatedOriginal = circlePostMapper.selectById(testPost.getId());
        assertEquals(1, updatedOriginal.getRepostCount());
    }

    @Test
    @DisplayName("转发动态 - 原动态不存在抛404")
    void repostPost_OriginalNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> circleInteractionService.repostPost(99999L, "转发", userB.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("转发动态 - 禁止转发的动态抛403")
    void repostPost_RepostDisabled_ThrowsException() {
        testPost.setAllowRepost(0);
        circlePostMapper.updateById(testPost);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> circleInteractionService.repostPost(testPost.getId(), "转发", userB.getId()));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("检查转发状态")
    void checkRepostStatus_Success() {
        assertFalse(circleInteractionService.checkRepostStatus(testPost.getId(), userB.getId()));

        circleInteractionService.repostPost(testPost.getId(), "转发", userB.getId());

        assertTrue(circleInteractionService.checkRepostStatus(testPost.getId(), userB.getId()));
    }

    @Test
    @DisplayName("检查转发状态 - userId为null返回false")
    void checkRepostStatus_NullUserId() {
        assertFalse(circleInteractionService.checkRepostStatus(testPost.getId(), null));
    }
}
