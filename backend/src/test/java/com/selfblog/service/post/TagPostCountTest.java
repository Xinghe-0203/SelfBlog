package com.selfblog.service.post;

import com.selfblog.config.cache.CaffeineCacheConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 公开标签索引的计数语义。
 *
 * <p>计数只统计**已发布**（status=1）且未删除的文章：
 * 草稿与已删除文章既不公开，计数里也不该出现它们 ——
 * 否则公开页面会泄露「有几篇草稿」。</p>
 *
 * <p>草稿这一档用 JdbcTemplate 直接写 blog_post（status=0）：
 * createPost 永远直接发布（本站没有待审流），草稿存在 blog_draft 表里，
 * 走 service 无法造出「blog_post 里 status=0 的行」。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TagPostCountTest {
    @Autowired BlogTagService tags;
    @Autowired BlogPostService posts;
    @Autowired JdbcTemplate jdbc;
    @Autowired CacheManager caches;

    @BeforeEach void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new com.selfblog.utils.UserContext(1L, "admin"), null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
        clearCache();
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        clearCache();
    }

    private void clearCache() {
        var hotTags = caches.getCache(CaffeineCacheConfig.HOT_TAGS_CACHE);
        if (hotTags != null) hotTags.clear();
        var category = caches.getCache(CaffeineCacheConfig.CATEGORY_CACHE);
        if (category != null) category.clear();
    }

    @Test
    void countsOnlyPublishedPosts() {
        var tag = tags.createTag("计数标签");
        clearCache();

        posts.createPost(request("已发布的文章", tag.getId()), 1L);
        assertEquals(1L, countOf(tag.getId()));

        // 草稿不计入：它还没公开。
        insertDraftPost("草稿文章", tag.getId());
        clearCache();
        assertEquals(1L, countOf(tag.getId()));
    }

    @Test
    void aTagWithoutPostsHasZeroInsteadOfMissing() {
        var tag = tags.createTag("空标签");
        clearCache();
        assertEquals(0L, countOf(tag.getId()));
    }

    private com.selfblog.dto.post.PostCreateRequest request(String title, Long tagId) {
        var request = new com.selfblog.dto.post.PostCreateRequest();
        request.setTitle(title);
        request.setContent("正文");
        request.setTagIds(List.of(tagId));
        return request;
    }

    /** 直接写一行 status=0 的 blog_post，并挂上标签关联。 */
    private void insertDraftPost(String title, Long tagId) {
        jdbc.update(
                "INSERT INTO blog_post (user_id, title, content, category, view_count, like_count, "
                        + "comment_count, collect_count, share_count, status, create_time, update_time, is_deleted) "
                        + "VALUES (1, ?, '草稿正文', '默认分类', 0, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                title);
        Long postId = jdbc.queryForObject("SELECT MAX(id) FROM blog_post WHERE title = ?", Long.class, title);
        jdbc.update(
                "INSERT INTO blog_post_tag (post_id, tag_id, create_time, is_deleted) "
                        + "VALUES (?, ?, CURRENT_TIMESTAMP, 0)",
                postId, tagId);
    }

    private long countOf(Long tagId) {
        return tags.listTagsWithCounts().stream()
                .filter(item -> tagId.equals(item.getId()))
                .mapToLong(item -> item.getPostCount() == null ? 0L : item.getPostCount())
                .findFirst()
                .orElse(-1L);
    }
}
