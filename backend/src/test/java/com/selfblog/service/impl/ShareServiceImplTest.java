package com.selfblog.service.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.BlogShare;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.BlogShareMapper;
import com.selfblog.service.content.ShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ShareServiceImpl 单元测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShareServiceImplTest {

    @Autowired
    private ShareService shareService;

    @Autowired
    private BlogShareMapper blogShareMapper;

    @Autowired
    private BlogPostMapper blogPostMapper;

    private BlogPost testPost;

    @BeforeEach
    void setUp() {
        blogShareMapper.delete(null);
        blogPostMapper.delete(null);

        testPost = new BlogPost();
        testPost.setUserId(1L);
        testPost.setTitle("测试文章");
        testPost.setContent("测试内容");
        testPost.setStatus(1);
        testPost.setShareCount(0);
        blogPostMapper.insert(testPost);
    }

    @Test
    @DisplayName("记录分享成功")
    void recordShare_Success() {
        shareService.recordShare(testPost.getId(), 1L, "weibo");

        int count = shareService.getShareCount(testPost.getId());
        assertEquals(1, count);
    }

    @Test
    @DisplayName("记录分享 - 平台为null默认web")
    void recordShare_NullPlatform_DefaultsToWeb() {
        shareService.recordShare(testPost.getId(), 1L, null);

        int count = shareService.getShareCount(testPost.getId());
        assertEquals(1, count);
    }

    @Test
    @DisplayName("记录分享 - userId可为空（未登录用户分享）")
    void recordShare_NullUserId() {
        shareService.recordShare(testPost.getId(), null, "web");

        int count = shareService.getShareCount(testPost.getId());
        assertEquals(1, count);
    }

    @Test
    @DisplayName("记录分享 - postId为空抛400")
    void recordShare_NullPostId_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.recordShare(null, 1L, "web"));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("记录分享 - 文章不存在抛404")
    void recordShare_PostNotFound_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> shareService.recordShare(99999L, 1L, "web"));
        assertEquals(404, ex.getCode());
    }

    @Test
    @DisplayName("多次分享后分享数正确累加")
    void recordShare_MultipleTimes() {
        shareService.recordShare(testPost.getId(), 1L, "weibo");
        shareService.recordShare(testPost.getId(), 2L, "wechat");
        shareService.recordShare(testPost.getId(), 3L, "qq");

        int count = shareService.getShareCount(testPost.getId());
        assertEquals(3, count);
    }

    @Test
    @DisplayName("获取分享数 - postId为null返回0")
    void getShareCount_NullPostId() {
        assertEquals(0, shareService.getShareCount(null));
    }

    @Test
    @DisplayName("获取分享数 - 无分享记录返回0")
    void getShareCount_NoShares() {
        assertEquals(0, shareService.getShareCount(testPost.getId()));
    }

    @Test
    @DisplayName("记录分享后文章分享数正确更新")
    void recordShare_PostShareCountUpdated() {
        shareService.recordShare(testPost.getId(), 1L, "web");

        BlogPost updated = blogPostMapper.selectById(testPost.getId());
        assertEquals(1, updated.getShareCount());
    }
}
