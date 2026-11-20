package com.selfblog.service.post;

import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.seo.SeoAutoGenerationTrigger;
import com.selfblog.vo.post.PostDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * blog_post.topic_ids 读写往返与话题计数回归。
 *
 * <p>回归背景：{@code BlogPost.topicIds} 曾挂 {@code JacksonTypeHandler}（String 字段），
 * createPost/updatePost 落库时会把「已是 JSON 的字符串」再编码一次，库里是
 * {@code "[884002]"}（带引号的 JSON 字符串）而非数组 {@code [884002]}；实体又没有
 * autoResultMap，读回的是原始带引号形态，{@code parseTopicIds} 解析不出任何话题 ID，
 * 于是编辑/删除文章时 post_count/trending_score 永不回减，JSON_CONTAINS/json_each
 * 查询同样失配（schema-fresh-install.sql v2.0.16 迁移注释所指的问题）。</p>
 *
 * <p>修复：String 字段不挂 typeHandler——写入侧 hutool 生成的规范数组文本按原文落库；
 * 解析统一走 {@link BlogPost#parseTopicIds(String)}，同时容忍遗留双重编码形态。
 * 本类以「service 真实读写 + jdbc 原始列断言」钉住三条链路：create 落库规范形态、
 * 编辑/删除计数恰好回减一次、遗留带引号行仍可解析并正确回减计数。</p>
 *
 * <p>约定（沿用 BlogPostServiceNullClearTest）：真实 BlogPostService + 真实 H2 落库，
 * 只把 SEO 触发器换成 mock；落库结果一律以数据库原始列为准（jdbc / mapper 重查）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("blog_post.topic_ids 读写往返与话题计数")
class BlogPostTopicIdsRoundTripTest {

    private static final long AUTHOR_ID = 449960L;
    private static final long TOPIC_A = 884011L;
    private static final long TOPIC_B = 884012L;
    private static final long POST_ID = 883301L;

    @Autowired
    BlogPostService posts;
    @Autowired
    BlogPostMapper blogPostMapper;
    @Autowired
    JdbcTemplate jdbc;

    @MockBean
    SeoAutoGenerationTrigger seoAutoGenerationTrigger;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM blog_topic");
        insertTopic(TOPIC_A, "话题A");
        insertTopic(TOPIC_B, "话题B");
    }

    private void insertTopic(long id, String name) {
        jdbc.update("INSERT INTO blog_topic(id, name, post_count, trending_score, status, is_deleted) "
                + "VALUES(?, ?, 0, 0, 1, 0)", id, name);
    }

    /** 以原始 SQL 直接落一篇文章（topicIdsJson 即库里的原始列文本，可注入遗留形态）。 */
    private long insertPost(String title, String topicIdsJson) {
        jdbc.update("DELETE FROM blog_post WHERE id = ?", POST_ID);
        jdbc.update("INSERT INTO blog_post(id, user_id, title, content, status, topic_ids, is_deleted) "
                        + "VALUES(?, ?, ?, ?, 1, ?, 0)",
                POST_ID, AUTHOR_ID, title, "正文", topicIdsJson);
        return POST_ID;
    }

    private PostCreateRequest request(String title, Long id, List<Long> topicIds) {
        PostCreateRequest request = new PostCreateRequest();
        request.setId(id);
        request.setTitle(title);
        request.setContent("正文内容");
        request.setTopicIds(topicIds);
        return request;
    }

    private String topicIdsColumn(long postId) {
        return jdbc.queryForObject("SELECT topic_ids FROM blog_post WHERE id = ?", String.class, postId);
    }

    private void presetCounters(long topicId, int count) {
        jdbc.update("UPDATE blog_topic SET post_count = ?, trending_score = ? WHERE id = ?", count, count, topicId);
    }

    private int topicCounter(long topicId, String column) {
        Integer value = jdbc.queryForObject("SELECT " + column + " FROM blog_topic WHERE id = ?", Integer.class, topicId);
        return value == null ? -1 : value;
    }

    private void assertCounters(long topicId, int expected) {
        assertEquals(expected, topicCounter(topicId, "post_count"), "post_count");
        assertEquals(expected, topicCounter(topicId, "trending_score"), "trending_score");
    }

    // ==================== 新写入路径（规范数组形态） ====================

    @Test
    @DisplayName("createPost 落库规范数组文本 [id]（不再双重编码）并恰好加一次计数")
    void createPostStoresCanonicalArrayTextAndIncrementsCounterOnce() {
        Long postId = posts.createPost(request("新文章", null, List.of(TOPIC_A)), AUTHOR_ID, true);

        assertEquals("[" + TOPIC_A + "]", topicIdsColumn(postId),
                "topic_ids 必须落库为规范 JSON 数组文本，而不是带引号的 \"[id]\" 双重编码形态");
        assertCounters(TOPIC_A, 1);
    }

    @Test
    @DisplayName("createPost 写下的行经 mapper/详情重读可解析：实体值、parseTopicIds、getTopicId、topicName 全链路")
    void createdPostRoundTripsThroughMapperAndDetail() {
        Long postId = posts.createPost(request("新文章", null, List.of(TOPIC_A)), AUTHOR_ID, true);

        BlogPost entity = blogPostMapper.selectById(postId);
        assertNotNull(entity);
        assertEquals("[" + TOPIC_A + "]", entity.getTopicIds(), "mapper 读回的实体值必须是规范数组文本");
        assertEquals(List.of(TOPIC_A), BlogPost.parseTopicIds(entity.getTopicIds()), "parseTopicIds 必须能解析");
        assertEquals(TOPIC_A, entity.getTopicId(), "getTopicId 兼容方法必须返回第一个话题");

        PostDetailResponse detail = posts.getPostDetail(postId);
        assertEquals(TOPIC_A, detail.getTopicId(), "详情响应必须带话题ID");
        assertEquals("话题A", detail.getTopicName(), "详情响应必须带话题名");
    }

    @Test
    @DisplayName("updatePost 清空话题：createPost 写下的行计数恰好减一次（修复前永不回减）")
    void updatePostRemovingTopicDecrementsCounterExactlyOnce() {
        Long postId = posts.createPost(request("旧标题", null, List.of(TOPIC_A)), AUTHOR_ID, true);
        // 计数器预置为 3：只有「恰好减一次」才会变成 2（减 0 次是 3，减多次是 1/0）
        presetCounters(TOPIC_A, 3);

        posts.updatePost(request("新标题", postId, null), AUTHOR_ID, true);

        assertNull(topicIdsColumn(postId), "清空话题后 topic_ids 必须落库为 NULL");
        assertCounters(TOPIC_A, 2);
    }

    @Test
    @DisplayName("updatePost 换话题：旧话题恰好减一次、新话题恰好加一次")
    void updatePostReplacingTopicMovesCounters() {
        Long postId = posts.createPost(request("旧标题", null, List.of(TOPIC_A)), AUTHOR_ID, true);
        presetCounters(TOPIC_A, 3);
        presetCounters(TOPIC_B, 1);

        posts.updatePost(request("新标题", postId, List.of(TOPIC_B)), AUTHOR_ID, true);

        assertEquals("[" + TOPIC_B + "]", topicIdsColumn(postId));
        assertCounters(TOPIC_A, 2);
        assertCounters(TOPIC_B, 2);
    }

    @Test
    @DisplayName("deletePost：createPost 写下的行计数恰好减一次")
    void deletePostDecrementsCounterExactlyOnce() {
        Long postId = posts.createPost(request("旧标题", null, List.of(TOPIC_A)), AUTHOR_ID, true);
        presetCounters(TOPIC_A, 3);

        posts.deletePost(postId, AUTHOR_ID, true);

        assertCounters(TOPIC_A, 2);
        Integer deleted = jdbc.queryForObject("SELECT is_deleted FROM blog_post WHERE id = ?", Integer.class, postId);
        assertNotNull(deleted);
        assertEquals(1, deleted, "文章必须被软删除");
    }

    // ==================== 遗留双重编码行（"[id]" 带引号形态） ====================

    @Test
    @DisplayName("遗留双重编码行仍可解析：mapper 原样读出带引号文本，parseTopicIds/getTopicId 解出话题")
    void legacyDoubleEncodedRowStillParses() {
        String legacyStored = "\"[" + TOPIC_B + "]\"";
        long postId = insertPost("遗留文章", legacyStored);
        assertEquals(legacyStored, topicIdsColumn(postId), "种子必须以原始遗留形态落库");

        BlogPost entity = blogPostMapper.selectById(postId);
        assertNotNull(entity);
        assertEquals(legacyStored, entity.getTopicIds(), "无 typeHandler 读取必须原样返回原始列文本");
        assertEquals(List.of(TOPIC_B), BlogPost.parseTopicIds(entity.getTopicIds()),
                "parseTopicIds 必须解掉最外层 JSON 引号后解析出话题");
        assertEquals(TOPIC_B, entity.getTopicId(), "getTopicId 兼容方法必须兼容遗留形态");
    }

    @Test
    @DisplayName("遗留双重编码行编辑清空话题：计数恰好减一次")
    void legacyDoubleEncodedRowClearDecrementsCounterExactlyOnce() {
        String legacyStored = "\"[" + TOPIC_B + "]\"";
        long postId = insertPost("遗留文章", legacyStored);
        presetCounters(TOPIC_B, 3);

        posts.updatePost(request("新标题", postId, null), AUTHOR_ID, true);

        assertNull(topicIdsColumn(postId));
        assertCounters(TOPIC_B, 2);
    }

    @Test
    @DisplayName("遗留双重编码行换话题：旧话题恰好减一次、新话题恰好加一次")
    void legacyDoubleEncodedRowReplaceMovesCounters() {
        String legacyStored = "\"[" + TOPIC_A + "]\"";
        long postId = insertPost("遗留文章", legacyStored);
        presetCounters(TOPIC_A, 3);
        presetCounters(TOPIC_B, 1);

        posts.updatePost(request("新标题", postId, List.of(TOPIC_B)), AUTHOR_ID, true);

        assertEquals("[" + TOPIC_B + "]", topicIdsColumn(postId), "新写入必须是规范形态");
        assertCounters(TOPIC_A, 2);
        assertCounters(TOPIC_B, 2);
    }

    @Test
    @DisplayName("遗留双重编码行删除：计数恰好减一次")
    void legacyDoubleEncodedRowDeleteDecrementsCounterExactlyOnce() {
        String legacyStored = "\"[" + TOPIC_B + "]\"";
        long postId = insertPost("遗留文章", legacyStored);
        presetCounters(TOPIC_B, 3);

        posts.deletePost(postId, AUTHOR_ID, true);

        assertCounters(TOPIC_B, 2);
    }

    // ==================== 多话题与防御性输入 ====================

    @Test
    @DisplayName("createPost 多话题：逐个加计数；updatePost 清空后逐个回减")
    void multipleTopicsAllCounted() {
        Long postId = posts.createPost(request("多话题文章", null, Arrays.asList(TOPIC_A, TOPIC_B)), AUTHOR_ID, true);

        assertCounters(TOPIC_A, 1);
        assertCounters(TOPIC_B, 1);
        // 存储形态断言只看首字符（MySQL JSON 列会重排空格，H2 原样；都仍是数组文本）
        String stored = topicIdsColumn(postId);
        assertNotNull(stored);
        assertEquals('[', stored.charAt(0), "多话题同样落库为 JSON 数组文本");
        assertTrue(stored.contains(String.valueOf(TOPIC_A)) && stored.contains(String.valueOf(TOPIC_B)),
                "两个话题 ID 都必须在数组文本里");

        presetCounters(TOPIC_A, 5);
        presetCounters(TOPIC_B, 5);
        posts.updatePost(request("新标题", postId, null), AUTHOR_ID, true);

        assertCounters(TOPIC_A, 4);
        assertCounters(TOPIC_B, 4);
    }

    @Test
    @DisplayName("垃圾文本/空串/null 解析防御：parseTopicIds 一律空列表，不抛异常")
    void tolerantParsingNeverThrows() {
        assertEquals(List.of(), BlogPost.parseTopicIds(null));
        assertEquals(List.of(), BlogPost.parseTopicIds(""));
        assertEquals(List.of(884012L), BlogPost.parseTopicIds(" [884012] "));
        assertEquals(List.of(884012L), BlogPost.parseTopicIds("\"[884012]\""));
        assertEquals(List.of(), BlogPost.parseTopicIds("not-json"));
        assertEquals(List.of(), BlogPost.parseTopicIds("\"just-a-string\""));
    }
}
