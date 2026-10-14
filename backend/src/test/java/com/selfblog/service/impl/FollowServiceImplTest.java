package com.selfblog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.BlogFollowMapper;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.social.FollowService;
import com.selfblog.vo.social.FollowStatusVO;
import com.selfblog.vo.user.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FollowServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FollowServiceImplTest {

    @Autowired
    private FollowService followService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BlogFollowMapper blogFollowMapper;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    private SysUser userA;
    private SysUser userB;

    @BeforeEach
    void setUp() {
        // 清理数据
        blogFollowMapper.delete(null);
        sysUserMapper.delete(null);

        // 创建测试用户
        userA = new SysUser();
        userA.setUsername("userA");
        userA.setPassword("password");
        userA.setNickname("User A");
        userA.setRole("user");
        userA.setStatus(1);
        userA.setFollowerCount(0);
        userA.setFollowingCount(0);
        sysUserMapper.insert(userA);

        userB = new SysUser();
        userB.setUsername("userB");
        userB.setPassword("password");
        userB.setNickname("User B");
        userB.setRole("user");
        userB.setStatus(1);
        userB.setFollowerCount(0);
        userB.setFollowingCount(0);
        sysUserMapper.insert(userB);
    }

    @Test
    @DisplayName("关注用户成功")
    void follow_Success() {
        FollowStatusVO result = followService.follow(userB.getId(), userA.getId());

        assertNotNull(result);
        assertTrue(result.getFollowing());
        assertEquals("follow", result.getAction());
        assertTrue(followService.isFollowing(userB.getId(), userA.getId()));
    }

    @Test
    @DisplayName("已关注时再次关注返回already_following")
    void follow_AlreadyFollowing() {
        followService.follow(userB.getId(), userA.getId());
        FollowStatusVO result = followService.follow(userB.getId(), userA.getId());

        assertNotNull(result);
        assertTrue(result.getFollowing());
        assertEquals("already_following", result.getAction());
    }

    @Test
    @DisplayName("取消关注后重新关注成功")
    void follow_AfterUnfollow_Success() {
        followService.follow(userB.getId(), userA.getId());
        followService.unfollow(userB.getId(), userA.getId());
        assertFalse(followService.isFollowing(userB.getId(), userA.getId()));

        FollowStatusVO result = followService.follow(userB.getId(), userA.getId());
        assertNotNull(result);
        assertTrue(result.getFollowing());
        assertEquals("follow", result.getAction());
    }

    @Test
    @DisplayName("不能关注自己")
    void follow_Self_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> followService.follow(userA.getId(), userA.getId()));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不能关注自己"));
    }

    @Test
    @DisplayName("关注不存在的用户抛404")
    void follow_TargetNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> followService.follow(99999L, userA.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("未登录关注抛401")
    void follow_NotLoggedIn_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> followService.follow(userB.getId(), null));
        assertEquals(401, ex.getCode());
    }

    @Test
    @DisplayName("取消关注成功")
    void unfollow_Success() {
        followService.follow(userB.getId(), userA.getId());
        FollowStatusVO result = followService.unfollow(userB.getId(), userA.getId());

        assertNotNull(result);
        assertFalse(result.getFollowing());
        assertEquals("unfollow", result.getAction());
        assertFalse(followService.isFollowing(userB.getId(), userA.getId()));
    }

    @Test
    @DisplayName("未关注时取消关注返回not_following")
    void unfollow_NotFollowing() {
        FollowStatusVO result = followService.unfollow(userB.getId(), userA.getId());

        assertNotNull(result);
        assertFalse(result.getFollowing());
        assertEquals("not_following", result.getAction());
    }

    @Test
    @DisplayName("不能取消关注自己")
    void unfollow_Self_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> followService.unfollow(userA.getId(), userA.getId()));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("取消关注不存在的用户抛404")
    void unfollow_TargetNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> followService.unfollow(99999L, userA.getId()));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("检查关注状态 - 已关注")
    void isFollowing_True() {
        followService.follow(userB.getId(), userA.getId());
        assertTrue(followService.isFollowing(userB.getId(), userA.getId()));
    }

    @Test
    @DisplayName("检查关注状态 - 未关注")
    void isFollowing_False() {
        assertFalse(followService.isFollowing(userB.getId(), userA.getId()));
    }

    @Test
    @DisplayName("检查关注状态 - null参数返回false")
    void isFollowing_NullParams() {
        assertFalse(followService.isFollowing(null, userA.getId()));
        assertFalse(followService.isFollowing(userB.getId(), null));
    }

    @Test
    @DisplayName("获取粉丝列表")
    void getFollowers_Success() {
        followService.follow(userB.getId(), userA.getId()); // A关注B → B的粉丝是A

        List<UserVO> followers = followService.getFollowers(userB.getId());
        assertNotNull(followers);
        assertEquals(1, followers.size());
        assertEquals(userA.getId(), followers.get(0).getId());
    }

    @Test
    @DisplayName("获取关注列表")
    void getFollowing_Success() {
        followService.follow(userB.getId(), userA.getId()); // A关注B → A的关注是B

        List<UserVO> following = followService.getFollowing(userA.getId());
        assertNotNull(following);
        assertEquals(1, following.size());
        assertEquals(userB.getId(), following.get(0).getId());
    }

    @Test
    @DisplayName("获取粉丝分页列表")
    void getFollowers_Paged_Success() {
        followService.follow(userB.getId(), userA.getId());

        IPage<UserVO> page = followService.getFollowers(userB.getId(), 1, 10);
        assertNotNull(page);
        assertEquals(1, page.getTotal());
        assertEquals(1, page.getRecords().size());
    }

    @Test
    @DisplayName("获取关注分页列表")
    void getFollowing_Paged_Success() {
        followService.follow(userB.getId(), userA.getId());

        IPage<UserVO> page = followService.getFollowing(userA.getId(), 1, 10);
        assertNotNull(page);
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("获取关注计数")
    void getCounts_Success() {
        followService.follow(userB.getId(), userA.getId());

        FollowService.FollowCountsVO counts = followService.getCounts(userA.getId());
        assertNotNull(counts);
        assertEquals(1, counts.getFollowingCount()); // A关注了1人

        FollowService.FollowCountsVO targetCounts = followService.getCounts(userB.getId());
        assertNotNull(targetCounts);
        assertEquals(1, targetCounts.getFollowerCount()); // B有1个粉丝
    }

    @Test
    @DisplayName("获取不存在用户的计数抛404")
    void getCounts_UserNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> followService.getCounts(99999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("获取不存在用户的粉丝列表抛404")
    void getFollowers_UserNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> followService.getFollowers(99999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("关注/取消关注后计数正确更新")
    void follow_Unfollow_CountsCorrect() {
        // A关注B
        followService.follow(userB.getId(), userA.getId());
        assertEquals(1, sysUserMapper.selectById(userB.getId()).getFollowerCount());
        assertEquals(1, sysUserMapper.selectById(userA.getId()).getFollowingCount());

        // A取消关注B
        followService.unfollow(userB.getId(), userA.getId());
        assertEquals(0, sysUserMapper.selectById(userB.getId()).getFollowerCount());
        assertEquals(0, sysUserMapper.selectById(userA.getId()).getFollowingCount());
    }
}
