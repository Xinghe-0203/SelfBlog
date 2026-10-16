package com.selfblog.service.post;

import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.service.post.BlogPostService;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.vo.post.PostDetailResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 文章发布/编辑后的标签生命周期回归。
 *
 * <p>真实环境（MySQL/SQLite）的 {@code blog_post_tag} 有 UNIQUE(post_id, tag_id)：
 * 编辑文章时关联行会被逻辑删除（is_deleted=1），如果重插只是 INSERT IGNORE，
 * 旧的已删除行会占据唯一键导致标签永久消失。H2 测试库曾缺少该唯一键，
 * 因此测试看不到这个问题；本用例依赖 schema 中同样的唯一键。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostTagLifecycleTest {
    @Autowired BlogPostService posts;
    @Autowired PostQueryService queries;
    @Autowired BlogTagService tags;
    @Autowired JdbcTemplate jdbc;
    @Autowired CacheManager caches;

    @BeforeEach @AfterEach void clearTagCache() {
        var cache = caches.getCache(CaffeineCacheConfig.CATEGORY_CACHE);
        if (cache != null) cache.clear();
    }

    @Test
    void editingAPostKeepsExistingTagsAndCanRestoreARemovedOne() {
        Long postId = posts.createPost(request("标签生命周期", "正文", List.of("回归标签")), 1L, true);
        assertEquals(List.of("回归标签"), visibleTags(postId));

        posts.updatePost(request("标签生命周期", "正文（保留标签）", List.of("回归标签"), postId), 1L, true);
        assertEquals(List.of("回归标签"), visibleTags(postId), "编辑后保留的标签必须仍然可见");

        posts.updatePost(request("标签生命周期", "正文（移除标签）", List.of(), postId), 1L, true);
        assertEquals(List.of(), visibleTags(postId), "移除标签后不应再显示");

        posts.updatePost(request("标签生命周期", "正文（重新加回）", List.of("回归标签"), postId), 1L, true);
        assertEquals(List.of("回归标签"), visibleTags(postId), "重新添加的标签必须恢复可见");
    }

    @Test
    void aNewTagNameIsNotDroppedWhenAnExistingTagIdIsAlsoSent() {
        Long existingId = tags.getOrCreateTag("已有标签");
        var request = request("混合标签文章", "正文", List.of("全新标签"));
        request.setTagIds(List.of(existingId));

        Long postId = posts.createPost(request, 1L, true);

        assertEquals(List.of("全新标签", "已有标签"), visibleTags(postId),
                "同时提交 tagIds 与 tagNames 时必须保留新标签，而不是只取其中一边");
    }

    private PostCreateRequest request(String title, String content, List<String> tagNames) {
        return request(title, content, tagNames, null);
    }

    private PostCreateRequest request(String title, String content, List<String> tagNames, Long id) {
        var request = new PostCreateRequest();
        request.setId(id);
        request.setTitle(title);
        request.setContent(content);
        request.setCategory("测试");
        request.setTagNames(tagNames.isEmpty() ? null : tagNames);
        return request;
    }

    /** 以数据库里的关联行标记为准，避免二级缓存掩盖问题。 */
    private List<String> visibleTags(Long postId) {
        List<String> markers = jdbc.queryForList(
                "SELECT tag_id FROM blog_post_tag WHERE post_id = ? AND is_deleted = 0 ORDER BY tag_id", Long.class, postId)
                .stream().map(String::valueOf).toList();
        // 关联行存在时，公开读取也必须能看到同名标签（清除缓存后走真实查询路径）。
        var cache = caches.getCache(CaffeineCacheConfig.CATEGORY_CACHE);
        if (cache != null) cache.clear();
        PostDetailResponse detail = queries.getPostDetail(postId);
        List<String> names = detail.getTags() == null ? List.of()
                : detail.getTags().stream().map(PostDetailResponse.TagVO::getName).sorted().toList();
        assertEquals(markers.size(), names.size(), "关联行与查询结果数量必须一致");
        return names;
    }
}
