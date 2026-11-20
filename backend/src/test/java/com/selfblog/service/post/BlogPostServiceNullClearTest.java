package com.selfblog.service.post;

import com.selfblog.dto.post.PostCreateRequest;
import com.selfblog.dto.post.SaveDraftRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.BlogDraftMapper;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.mapper.TopicMapper;
import com.selfblog.service.seo.SeoAutoGenerationTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 「清空可空列」的落库回归（updatePost 的话题/封面、saveDraft 的摘要/封面）。
 *
 * <p>回归背景：{@code updateById(entity)} 的默认字段策略是 NOT_NULL，null 字段会被从
 * SET 子句里整个剔除。旧实现清空话题时先减掉了计数、再把实体上的 topicIds 置 null 走
 * {@code updateById}——列被剔除，{@code topic_ids} 仍挂着旧数组，计数与数据从此漂移；
 * 封面与草稿摘要/封面的「清除」同样静默失效。修复后清空路径改走
 * {@code update(null, wrapper)} 显式 {@code .set(col, null)}（参考 SeoMetadataService.save）。</p>
 *
 * <p>约定（沿用 BlogPostSeoHookTest）：真实 BlogPostService + 真实 H2 落库，只把 SEO 触发器
 * 换成 mock。清空结果一律以数据库原始列为准（jdbc / mapper 重查），绝不读被服务就地
 * 改过的实体对象。</p>
 *
 * <p><b>范围说明（探针实测，另案处理）</b>：{@code createPost} 经 {@code BlogPost.topicIds}
 * 上的 JacksonTypeHandler 落库时会把「已是 JSON 的字符串」再编码一次（库里是
 * {@code "[884002]"}——带引号的 JSON 字符串），而 {@code BlogPost} 没有
 * {@code autoResultMap}，读回来的是原始带引号形态，{@code parseTopicIds} 解析不出任何
 * 话题 ID。这是与本次「清空列」无关的**既有**读写不对称缺陷；因此涉及计数器的用例
 * 直接以 jdbc 种子写入可解析的原始 {@code [id]} 形态（真实「文章已有话题」的数据状态），
 * 不依赖那条损坏的往返链路。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("文章/草稿可空列清空（wrapper 显式 set NULL）")
class BlogPostServiceNullClearTest {

    private static final long AUTHOR_ID = 449950L;
    private static final long TOPIC_A = 884001L;
    private static final long TOPIC_B = 884002L;
    private static final long POST_A = 883101L;
    private static final long POST_B = 883102L;

    @Autowired
    BlogPostService posts;
    @Autowired
    BlogPostMapper blogPostMapper;
    @Autowired
    BlogDraftMapper blogDraftMapper;
    @Autowired
    TopicMapper topicMapper;
    @Autowired
    JdbcTemplate jdbc;

    @MockBean
    SeoAutoGenerationTrigger seoAutoGenerationTrigger;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM blog_draft_tag");
        jdbc.update("DELETE FROM blog_draft");
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

    private void insertPost(long id, String title, String topicIdsJson, String coverUrl) {
        jdbc.update("INSERT INTO blog_post(id, user_id, title, content, status, topic_ids, cover_url, is_deleted) "
                        + "VALUES(?, ?, ?, ?, 1, ?, ?, 0)",
                id, AUTHOR_ID, title, "正文", topicIdsJson, coverUrl);
    }

    private PostCreateRequest updateRequest(Long id, String title) {
        PostCreateRequest request = new PostCreateRequest();
        request.setId(id);
        request.setTitle(title);
        request.setContent("更新后的正文");
        return request;
    }

    private String topicIdsColumn(long postId) {
        return jdbc.queryForObject("SELECT topic_ids FROM blog_post WHERE id = ?", String.class, postId);
    }

    private String coverColumn(long postId) {
        return jdbc.queryForObject("SELECT cover_url FROM blog_post WHERE id = ?", String.class, postId);
    }

    private int topicPostCount(long topicId) {
        Integer count = jdbc.queryForObject("SELECT post_count FROM blog_topic WHERE id = ?", Integer.class, topicId);
        return count == null ? -1 : count;
    }

    @Test
    @DisplayName("updatePost 清空话题：topic_ids 落库为 NULL，计数恰好减一次")
    void clearingTopicsPersistsNullAndDecrementsCounterOnce() {
        // “文章已有话题”的状态：topic_ids 挂着数组、计数器为 2（减两次才会露馅）
        insertPost(POST_A, "旧标题", "[" + TOPIC_A + "]", "/uploads/cover-a.jpg");
        jdbc.update("UPDATE blog_topic SET post_count = 2 WHERE id = ?", TOPIC_A);

        posts.updatePost(updateRequest(POST_A, "新标题"), AUTHOR_ID, true);

        assertNull(topicIdsColumn(POST_A), "清空话题后 topic_ids 必须真的落库为 NULL");
        assertEquals(1, topicPostCount(TOPIC_A), "计数必须恰好减一次（2 → 1）");
        // mapper 重查（不是被服务就地改过的实体）也必须看到 NULL
        BlogPost reread = blogPostMapper.selectById(POST_A);
        assertNotNull(reread);
        assertNull(reread.getTopicIds(), "mapper 重读的实体同样不得带旧话题数组");
    }

    @Test
    @DisplayName("updatePost 封面置 null：cover_url 落库为 NULL 而不是保留旧值")
    void clearingCoverPersistsNull() {
        insertPost(POST_B, "旧标题", null, "/uploads/old-cover.jpg");

        posts.updatePost(updateRequest(POST_B, "新标题"), AUTHOR_ID, true);

        assertNull(coverColumn(POST_B), "封面清除必须真的落库为 NULL");
    }

    @Test
    @DisplayName("updatePost 非空更新行为不变：话题与封面照常写入，计数不变")
    void nonNullTopicAndCoverUpdateIsUnchanged() {
        insertPost(POST_A, "旧标题", "[" + TOPIC_B + "]", "/uploads/old.jpg");
        jdbc.update("UPDATE blog_topic SET post_count = 1 WHERE id = ?", TOPIC_B);

        PostCreateRequest request = updateRequest(POST_A, "新标题");
        request.setCoverImage("/uploads/new.jpg");
        request.setTopicIds(List.of(TOPIC_B));
        posts.updatePost(request, AUTHOR_ID, true);

        assertEquals("/uploads/new.jpg", coverColumn(POST_A), "非空封面照常更新");
        assertNotNull(topicIdsColumn(POST_A), "非空话题照常写入");
        assertEquals(1, topicPostCount(TOPIC_B), "相同话题集不增不减");
    }

    @Test
    @DisplayName("saveDraft 摘要/封面置 null：两列落库为 NULL（按 draftId 更新）")
    void draftSummaryAndCoverClearPersistNull() {
        SaveDraftRequest create = new SaveDraftRequest();
        create.setTitle("草稿一");
        create.setContent("草稿正文");
        create.setSummary("旧摘要");
        create.setCoverImage("/uploads/old-cover.jpg");
        Long draftId = posts.saveDraft(AUTHOR_ID, create);
        assertNotNull(blogDraftMapper.selectById(draftId).getSummary());

        SaveDraftRequest update = new SaveDraftRequest();
        update.setDraftId(draftId);
        update.setTitle("草稿二");
        update.setContent("草稿正文二");
        // summary / coverImage 省略（null）：必须真的清空
        posts.saveDraft(AUTHOR_ID, update);

        var stored = blogDraftMapper.selectById(draftId);
        assertEquals("草稿二", stored.getTitle(), "非空字段照常更新");
        assertNull(stored.getSummary(), "摘要必须真的落库为 NULL");
        assertNull(stored.getCoverImage(), "封面必须真的落库为 NULL");
    }

    @Test
    @DisplayName("saveDraft 非空摘要/封面更新行为不变")
    void draftNonNullUpdateIsUnchanged() {
        SaveDraftRequest create = new SaveDraftRequest();
        create.setTitle("草稿一");
        create.setContent("草稿正文");
        create.setSummary("旧摘要");
        create.setCoverImage("/uploads/old-cover.jpg");
        Long draftId = posts.saveDraft(AUTHOR_ID, create);

        SaveDraftRequest update = new SaveDraftRequest();
        update.setDraftId(draftId);
        update.setTitle("草稿二");
        update.setContent("草稿正文二");
        update.setSummary("新摘要");
        update.setCoverImage("/uploads/new-cover.jpg");
        posts.saveDraft(AUTHOR_ID, update);

        var stored = blogDraftMapper.selectById(draftId);
        assertEquals("新摘要", stored.getSummary());
        assertEquals("/uploads/new-cover.jpg", stored.getCoverImage());
    }
}
