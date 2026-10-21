package com.selfblog.service.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.CircleLike;
import com.selfblog.entity.CirclePost;
import com.selfblog.mapper.CircleCommentMapper;
import com.selfblog.mapper.CircleLikeMapper;
import com.selfblog.mapper.CirclePostMapper;
import com.selfblog.mapper.CircleRepostMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.circle.impl.CircleInteractionServiceImpl;
import com.selfblog.service.social.FollowService;
import com.selfblog.service.social.NotificationService;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.vo.circle.CircleLikeResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 校友圈点赞切换的并发仲裁语义（C19）。
 *
 * <p>缺陷回顾：旧实现「先查活跃行、再 delete/insert」，并发时两个请求都读到「无活跃点赞」，
 * 一方 insert 成功、另一方撞唯一键进入 catch；旧 catch 又查出对手刚插入的活跃行并调用
 * {@code logicalDeleteById + decrementLikeCount}，等于把赢家的点赞悄悄取消掉，
 * 最终还返回 action=unlike。</p>
 *
 * <p>这里用 Mockito 直接构造「insert 撞唯一键」这一时刻（真实数据库单线程无法复现该竞态），
 * 断言输家：① 不改对手的点赞行；② 不动 likeCount；③ 结果按幂等成功回报。
 * 其余三条路径（原子取消、恢复旧行、重新插入）也在此覆盖。</p>
 */
class CircleLikeToggleArbitrationTest {

    private static final Long POST_ID = 9001L;
    private static final Long AUTHOR_ID = 501L;
    private static final Long LIKER_ID = 502L;

    private CircleInteractionServiceImpl service;
    private CirclePostMapper circlePostMapper;
    private CircleLikeMapper circleLikeMapper;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        service = new CircleInteractionServiceImpl();

        circlePostMapper = mock(CirclePostMapper.class);
        circleLikeMapper = mock(CircleLikeMapper.class);
        notificationService = mock(NotificationService.class);

        ReflectionTestUtils.setField(service, "circlePostMapper", circlePostMapper);
        ReflectionTestUtils.setField(service, "circleLikeMapper", circleLikeMapper);
        ReflectionTestUtils.setField(service, "circleCommentMapper", mock(CircleCommentMapper.class));
        ReflectionTestUtils.setField(service, "circleRepostMapper", mock(CircleRepostMapper.class));
        ReflectionTestUtils.setField(service, "sysUserMapper", mock(SysUserMapper.class));
        ReflectionTestUtils.setField(service, "htmlSanitizer", mock(HtmlSanitizer.class));
        ReflectionTestUtils.setField(service, "notificationService", notificationService);
        ReflectionTestUtils.setField(service, "followService", mock(FollowService.class));
    }

    @Test
    @DisplayName("insert 撞唯一键时输家不得取消赢家的点赞，也不得改动计数")
    void losingRequestDoesNotCancelTheWinnersLike() {
        CirclePost post = visiblePost(1);
        when(circlePostMapper.selectById(POST_ID)).thenReturn(post, post);
        // 没有活跃点赞可取消、也没有旧行可恢复 → 走到 insert 分支
        when(circleLikeMapper.logicalDeleteActiveByUserAndPost(LIKER_ID, POST_ID)).thenReturn(0);
        when(circleLikeMapper.logicalRestoreByUserAndPost(LIKER_ID, POST_ID)).thenReturn(0);
        when(circleLikeMapper.insert(any(CircleLike.class)))
                .thenThrow(new DuplicateKeyException("UNIQUE(user_id, post_id)"));

        CircleLikeResultVO result = service.toggleLike(POST_ID, LIKER_ID);

        // 赢家已经点赞成功，输家只能回报「已点赞」，不能反过来取消
        assertEquals("like", result.getAction());
        assertEquals(1, result.getLikeCount());
        // 仲裁原语只尝试了一次（取消自己那条并不存在的活跃点赞，返回 0）；
        // 关键是不能用 logicalDeleteById 去删对手的行，也不能减计数
        verify(circleLikeMapper, times(1)).logicalDeleteActiveByUserAndPost(LIKER_ID, POST_ID);
        verify(circleLikeMapper, never()).logicalDeleteById(anyLong());
        verify(circlePostMapper, never()).decrementLikeCount(anyLong());
        verify(circlePostMapper, never()).incrementLikeCount(anyLong());
        verify(notificationService, never()).sendNotification(
                anyString(), anyString(), anyString(), anyLong(), anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("有活跃点赞时走原子取消路径：只减计数，不碰恢复与插入")
    void activeLikeIsCancelledByTheAtomicDelete() {
        CirclePost post = visiblePost(0);
        when(circlePostMapper.selectById(POST_ID)).thenReturn(post, post);
        when(circleLikeMapper.logicalDeleteActiveByUserAndPost(LIKER_ID, POST_ID)).thenReturn(1);

        CircleLikeResultVO result = service.toggleLike(POST_ID, LIKER_ID);

        assertEquals("unlike", result.getAction());
        assertEquals(0, result.getLikeCount());
        verify(circlePostMapper, times(1)).decrementLikeCount(POST_ID);
        verify(circlePostMapper, never()).incrementLikeCount(anyLong());
        verify(circleLikeMapper, never()).logicalRestoreByUserAndPost(anyLong(), anyLong());
        verify(circleLikeMapper, never()).insert(any(CircleLike.class));
    }

    @Test
    @DisplayName("存在逻辑删除的旧行时恢复它并只加一次计数")
    void deletedRowIsRestoredInsteadOfInserted() {
        CirclePost post = visiblePost(1);
        when(circlePostMapper.selectById(POST_ID)).thenReturn(post, post);
        when(circleLikeMapper.logicalDeleteActiveByUserAndPost(LIKER_ID, POST_ID)).thenReturn(0);
        when(circleLikeMapper.logicalRestoreByUserAndPost(LIKER_ID, POST_ID)).thenReturn(1);

        CircleLikeResultVO result = service.toggleLike(POST_ID, LIKER_ID);

        assertEquals("like", result.getAction());
        assertEquals(1, result.getLikeCount());
        verify(circlePostMapper, times(1)).incrementLikeCount(POST_ID);
        verify(circleLikeMapper, never()).insert(any(CircleLike.class));
        verify(circlePostMapper, never()).decrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("首次点赞插入新行，并给作者发一条通知")
    void firstLikeInsertsARowAndNotifiesTheAuthor() {
        CirclePost post = visiblePost(1);
        when(circlePostMapper.selectById(POST_ID)).thenReturn(post, post);
        when(circleLikeMapper.logicalDeleteActiveByUserAndPost(LIKER_ID, POST_ID)).thenReturn(0);
        when(circleLikeMapper.logicalRestoreByUserAndPost(LIKER_ID, POST_ID)).thenReturn(0);
        when(circleLikeMapper.insert(any(CircleLike.class))).thenReturn(1);

        CircleLikeResultVO result = service.toggleLike(POST_ID, LIKER_ID);

        assertEquals("like", result.getAction());
        assertEquals(1, result.getLikeCount());
        verify(circlePostMapper, times(1)).incrementLikeCount(POST_ID);
        verify(notificationService, times(1)).sendNotification(
                eq("LIKE"), anyString(), anyString(), eq(LIKER_ID), eq(AUTHOR_ID), eq("POST"), eq(POST_ID));
    }

    @Test
    @DisplayName("作者给自己点赞时不发通知")
    void selfLikeDoesNotNotify() {
        CirclePost post = visiblePost(1);
        when(circlePostMapper.selectById(POST_ID)).thenReturn(post, post);
        when(circleLikeMapper.logicalDeleteActiveByUserAndPost(AUTHOR_ID, POST_ID)).thenReturn(0);
        when(circleLikeMapper.logicalRestoreByUserAndPost(AUTHOR_ID, POST_ID)).thenReturn(0);
        when(circleLikeMapper.insert(any(CircleLike.class))).thenReturn(1);

        CircleLikeResultVO result = service.toggleLike(POST_ID, AUTHOR_ID);

        assertEquals("like", result.getAction());
        verify(notificationService, never()).sendNotification(
                anyString(), anyString(), anyString(), anyLong(), anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("动态不存在时抛 404，且不产生任何写入")
    void missingPostIsRejectedBeforeAnyWrite() {
        when(circlePostMapper.selectById(POST_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.toggleLike(POST_ID, LIKER_ID));

        assertEquals(404, ex.getCode());
        verify(circleLikeMapper, never()).logicalDeleteActiveByUserAndPost(anyLong(), anyLong());
        verify(circlePostMapper, never()).incrementLikeCount(anyLong());
    }

    /**
     * 一条公开、已发布、点赞数为 {@code likeCount} 的动态。
     */
    private CirclePost visiblePost(int likeCount) {
        CirclePost post = new CirclePost();
        post.setId(POST_ID);
        post.setUserId(AUTHOR_ID);
        post.setStatus(1);
        post.setVisibility(0);
        post.setLikeCount(likeCount);
        return post;
    }
}
