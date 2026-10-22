package com.selfblog.service.seo;

import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.post.BlogPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 发布链路与 SEO 自动生成的接线：只在发布成功后派发，且派发失败绝不能影响文章保存。
 *
 * <p>用真实 {@link BlogPostService} + 真实 H2 落库，只把触发器换成 mock，
 * 这样能验证「保存成功但触发器抛异常」这条最容易写出 500 的路径。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("发布后 SEO 自动生成接线")
class BlogPostSeoHookTest {

    private static final long USER_ID = 1L;

    @Autowired
    private BlogPostService blogPostService;
    @Autowired
    private BlogPostMapper blogPostMapper;
    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private SeoAutoGenerationTrigger seoAutoGenerationTrigger;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM blog_post WHERE title LIKE 'SEO_HOOK_%'");
    }

    private PostCreateRequest request(String title) {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle(title);
        request.setContent("## 小标题\n\n正文内容，用于验证发布后的 SEO 派发。");
        request.setSummary("摘要");
        request.setCategory("后端");
        return request;
    }

    @Test
    @DisplayName("createPost_DispatchesSeoAfterPublish")
    void createPost_DispatchesSeoAfterPublish() {
        Long postId = blogPostService.createPost(request("SEO_HOOK_新建文章"), USER_ID, true);

        assertNotNull(postId);
        BlogPost stored = blogPostMapper.selectById(postId);
        assertNotNull(stored);
        assertEquals(1, stored.getStatus(), "单人博客发布即上线");
        verify(seoAutoGenerationTrigger).fireAfterCommit(postId);
    }

    @Test
    @DisplayName("createPost_SucceedsEvenWhenTriggerThrows")
    void createPost_SucceedsEvenWhenTriggerThrows() {
        doThrow(new IllegalStateException("trigger exploded")).when(seoAutoGenerationTrigger).fireAfterCommit(anyLong());

        Long postId = blogPostService.createPost(request("SEO_HOOK_触发器异常"), USER_ID, true);

        assertNotNull(postId, "SEO 派发失败不该影响文章保存");
        assertNotNull(blogPostMapper.selectById(postId));
    }

    @Test
    @DisplayName("updatePost_DispatchesOnlyForPublishedPost")
    void updatePost_DispatchesOnlyForPublishedPost() {
        Long postId = blogPostService.createPost(request("SEO_HOOK_更新文章"), USER_ID, true);

        PostCreateRequest update = request("SEO_HOOK_更新文章-改标题");
        update.setId(postId);
        blogPostService.updatePost(update, USER_ID, true);

        // 新建一次 + 更新一次
        verify(seoAutoGenerationTrigger, org.mockito.Mockito.times(2)).fireAfterCommit(postId);
    }

    @Test
    @DisplayName("updatePost_DoesNotDispatchForDraft")
    void updatePost_DoesNotDispatchForDraft() {
        Long postId = blogPostService.createPost(request("SEO_HOOK_草稿更新"), USER_ID, true);
        // 造一篇历史草稿（approvePost 只对 status=0 的老数据生效）
        jdbc.update("UPDATE blog_post SET status = 0 WHERE id = ?", postId);

        PostCreateRequest update = request("SEO_HOOK_草稿更新-改标题");
        update.setId(postId);
        blogPostService.updatePost(update, USER_ID, true);

        // 新建时派发过一次，更新草稿不再派发
        verify(seoAutoGenerationTrigger, org.mockito.Mockito.times(1)).fireAfterCommit(postId);
    }

    @Test
    @DisplayName("approvePost_DispatchesSeo")
    void approvePost_DispatchesSeo() {
        Long postId = blogPostService.createPost(request("SEO_HOOK_审核通过"), USER_ID, true);
        jdbc.update("UPDATE blog_post SET status = 0 WHERE id = ?", postId);

        blogPostService.approvePost(postId, USER_ID);

        assertEquals(1, blogPostMapper.selectById(postId).getStatus());
        verify(seoAutoGenerationTrigger, org.mockito.Mockito.times(2)).fireAfterCommit(postId);
    }

    @Test
    @DisplayName("createPost_ValidationFailureNeverTouchesTrigger")
    void createPost_ValidationFailureNeverTouchesTrigger() {
        PostCreateRequest invalid = new PostCreateRequest();
        invalid.setTitle("  ");
        invalid.setContent("正文");

        org.junit.jupiter.api.Assertions.assertThrows(com.selfblog.common.exception.BusinessException.class,
                () -> blogPostService.createPost(invalid, USER_ID, true));

        verify(seoAutoGenerationTrigger, never()).fireAfterCommit(any());
    }
}
