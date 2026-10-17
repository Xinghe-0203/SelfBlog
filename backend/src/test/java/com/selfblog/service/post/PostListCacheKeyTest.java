package com.selfblog.service.post;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.vo.post.PostListResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 回归：文章列表缓存键必须包含全部过滤维度。
 *
 * getPostList 使用 @Cacheable(trendingCache)，历史上缓存键遗漏
 * keyword/userId/tagId，导致「先查关键词 A 再查关键词 B」拿回同一条缓存。
 * 本测试在真实 Caffeine 缓存（test profile 下 cacheManager 仍生效）上验证：
 * 同分页参数、不同 keyword 必须返回不同的文章集合。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostListCacheKeyTest {

    private static final long AUTHOR_ID = 424242L;
    private static final long POST_ALPHA = 889101L;
    private static final long POST_BRAVO = 889102L;

    @Autowired
    PostQueryService queries;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void samePageDifferentKeywordsMustNotShareCacheEntry() {
        insertPost(POST_ALPHA, "玄猫观察笔记第一篇", "只谈玄猫");
        insertPost(POST_BRAVO, "橘猫饲养手册第一篇", "只谈橘猫");

        PostQueryRequest alpha = request("玄猫");
        PostQueryRequest bravo = request("橘猫");

        IPage<PostListResponse> alphaResult = queries.getPostList(alpha);
        IPage<PostListResponse> bravoResult = queries.getPostList(bravo);

        assertEquals(List.of("玄猫观察笔记第一篇"),
                alphaResult.getRecords().stream().map(PostListResponse::getTitle).toList(),
                "关键词 A 的结果必须只命中 A 文章");
        assertEquals(List.of("橘猫饲养手册第一篇"),
                bravoResult.getRecords().stream().map(PostListResponse::getTitle).toList(),
                "相同分页参数下关键词 B 若复用 A 的缓存即为本回归缺陷");
    }

    @Test
    void samePageDifferentTagIdsMustNotShareCacheEntry() {
        // 两个标签各挂一篇文章：tagId 未入键时第二次查询会复用第一次缓存
        long tagCola = 889201L;
        long tagLime = 889202L;
        insertPost(POST_ALPHA + 10, "青柠汽水评测", null);
        insertPost(POST_BRAVO + 10, "可乐冰镇评测", null);
        jdbc.update("INSERT INTO blog_tag(id, name, is_deleted) VALUES (?, '青柠汽水标签', 0)", tagCola);
        jdbc.update("INSERT INTO blog_tag(id, name, is_deleted) VALUES (?, '可乐标签', 0)", tagLime);
        jdbc.update("INSERT INTO blog_post_tag(post_id, tag_id) VALUES (?, ?)", POST_ALPHA + 10, tagCola);
        jdbc.update("INSERT INTO blog_post_tag(post_id, tag_id) VALUES (?, ?)", POST_BRAVO + 10, tagLime);

        PostQueryRequest first = request(null);
        first.setTagId(tagCola);
        PostQueryRequest second = request(null);
        second.setTagId(tagLime);

        var firstResult = queries.getPostList(first);
        var secondResult = queries.getPostList(second);

        assertEquals(List.of("青柠汽水评测"),
                firstResult.getRecords().stream().map(PostListResponse::getTitle).toList());
        assertEquals(List.of("可乐冰镇评测"),
                secondResult.getRecords().stream().map(PostListResponse::getTitle).toList(),
                "相同分页参数下 tagId 若复用前一个 tag 的缓存即为本回归缺陷");
    }

    private PostQueryRequest request(String keyword) {
        PostQueryRequest request = new PostQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setSort("latest");
        request.setKeyword(keyword);
        return request;
    }

    private void insertPost(long id, String titleWithKeyword, String keywordOnlyBody) {
        jdbc.update("""
                INSERT INTO blog_post(id, user_id, title, summary, content, status, is_deleted, view_count, like_count)
                VALUES (?, ?, ?, '', ?, 1, 0, 3, 0)
                """, id, AUTHOR_ID, titleWithKeyword, keywordOnlyBody == null ? titleWithKeyword : keywordOnlyBody);
    }
}
