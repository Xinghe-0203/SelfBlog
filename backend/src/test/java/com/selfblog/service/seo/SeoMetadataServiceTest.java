package com.selfblog.service.seo;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.seo.SeoUpdateRequest;
import com.selfblog.entity.PostSeo;
import com.selfblog.mapper.PostSeoMapper;
import com.selfblog.vo.seo.SeoDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手工维护 SEO 元数据的写路径：校验、空白归一、source 语义、生成时间保留。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SeoMetadataService")
class SeoMetadataServiceTest {

    private static final long POST_ID = 881001L;

    @Autowired
    private SeoMetadataService metadataService;
    @Autowired
    private SeoAuditService auditService;
    @Autowired
    private PostSeoMapper postSeoMapper;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM post_seo WHERE post_id = ?", POST_ID);
        jdbc.update("DELETE FROM blog_post WHERE id = ?", POST_ID);
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, cover_url, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, ?, 1, "
                        + "TIMESTAMP '2026-10-15 10:00:00', TIMESTAMP '2026-10-15 10:00:00', 0)",
                POST_ID, "手工 SEO 测试文章", "文章摘要", "正文内容", "后端", "/uploads/cover.jpg");
    }

    private static SeoUpdateRequest request(String title, String description, String keywords, String image) {
        SeoUpdateRequest request = new SeoUpdateRequest();
        request.setSeoTitle(title);
        request.setSeoDescription(description);
        request.setSeoKeywords(keywords);
        request.setOgImageUrl(image);
        return request;
    }

    @Test
    @DisplayName("save_StoresManualFields")
    void save_StoresManualFields() {
        SeoDetailVO detail = metadataService.save(POST_ID,
                request("自定义标题", "自定义描述", "关键词甲,关键词乙", "/uploads/share.png"));

        assertEquals("manual", detail.getSource());
        assertEquals("自定义标题", detail.getSeoTitle());
        assertEquals("自定义描述", detail.getSeoDescription());
        assertEquals("关键词甲,关键词乙", detail.getSeoKeywords());
        assertEquals("/uploads/share.png", detail.getOgImageUrl());
        assertTrue(detail.isHasSeo());
        assertEquals("自定义标题", detail.getEffectiveTitle());
        assertEquals("自定义描述", detail.getEffectiveDescription());
        // 手工保存不该伪造 generatedAt
        assertNull(detail.getGeneratedAt());
        assertNotNull(detail.getUpdateTime());

        PostSeo stored = postSeoMapper.selectById(POST_ID);
        assertNotNull(stored);
        assertEquals("manual", stored.getSource());
        assertNotNull(stored.getUpdateTime());
    }

    @Test
    @DisplayName("save_BlankFieldsBecomeNull")
    void save_BlankFieldsBecomeNull() {
        SeoDetailVO detail = metadataService.save(POST_ID, request("   ", "", null, "  "));

        assertNull(detail.getSeoTitle());
        assertNull(detail.getSeoDescription());
        assertNull(detail.getSeoKeywords());
        assertNull(detail.getOgImageUrl());
        assertFalse(detail.isHasSeo(), "全空记录不该算「有 SEO」");
        // 全空时回落到派生值
        assertEquals("手工 SEO 测试文章", detail.getEffectiveTitle());
        assertEquals("文章摘要", detail.getEffectiveDescription());
    }

    @Test
    @DisplayName("save_ClearsPreviouslyStoredFields")
    void save_ClearsPreviouslyStoredFields() {
        metadataService.save(POST_ID, request("自定义标题", "自定义描述", "关键词甲", "/uploads/share.png"));

        // 第二次保存把描述与分享图清空（其余保留）。这是「更新已有行」的路径：
        // MyBatis-Plus 默认 NOT_NULL 更新策略会把 null 字段从 SET 里剔除，
        // 用 updateById(entity) 实现时这一步会静默失败，回读还是旧值。
        SeoDetailVO detail = metadataService.save(POST_ID, request("自定义标题", "", "关键词甲", "   "));

        assertNull(detail.getSeoDescription(), "清空描述必须真的落成 NULL");
        assertNull(detail.getOgImageUrl(), "清空分享图必须真的落成 NULL");
        assertEquals("自定义标题", detail.getSeoTitle());
        assertEquals("关键词甲", detail.getSeoKeywords());

        // 直接查库确认，避免只信 VO（VO 由同一个 mapper 回读，但仍然更硬）
        PostSeo stored = postSeoMapper.selectById(POST_ID);
        assertNull(stored.getSeoDescription());
        assertNull(stored.getOgImageUrl());
        assertEquals("自定义标题", stored.getSeoTitle());
        assertEquals("manual", stored.getSource());
    }

    @Test
    @DisplayName("save_TrimsWhitespace")
    void save_TrimsWhitespace() {
        SeoDetailVO detail = metadataService.save(POST_ID, request("  标题  ", "  描述  ", "  a , b  ", " /uploads/x.png "));

        assertEquals("标题", detail.getSeoTitle());
        assertEquals("描述", detail.getSeoDescription());
        assertEquals("a , b", detail.getSeoKeywords());
        assertEquals("/uploads/x.png", detail.getOgImageUrl());
    }

    @Test
    @DisplayName("save_RejectsOverlongFields")
    void save_RejectsOverlongFields() {
        assertEquals(400, assertThrows(BusinessException.class, () -> metadataService.save(POST_ID,
                request("t".repeat(201), null, null, null))).getCode());
        assertEquals(400, assertThrows(BusinessException.class, () -> metadataService.save(POST_ID,
                request(null, "d".repeat(401), null, null))).getCode());
        assertEquals(400, assertThrows(BusinessException.class, () -> metadataService.save(POST_ID,
                request(null, null, "k".repeat(301), null))).getCode());
        assertEquals(400, assertThrows(BusinessException.class, () -> metadataService.save(POST_ID,
                request(null, null, null, "/uploads/" + "x".repeat(500)))).getCode());
    }

    @Test
    @DisplayName("save_AcceptsOnlyHttpOrRootRelativeImages")
    void save_AcceptsOnlyHttpOrRootRelativeImages() {
        assertEquals("https://cdn.example.com/a.png", metadataService.save(POST_ID,
                request(null, null, null, "https://cdn.example.com/a.png")).getOgImageUrl());
        assertEquals("/uploads/a.png", metadataService.save(POST_ID,
                request(null, null, null, "/uploads/a.png")).getOgImageUrl());

        assertEquals(400, assertThrows(BusinessException.class, () -> metadataService.save(POST_ID,
                request(null, null, null, "//evil.example.com/a.png"))).getCode());
        assertEquals(400, assertThrows(BusinessException.class, () -> metadataService.save(POST_ID,
                request(null, null, null, "javascript:alert(1)"))).getCode());
        assertEquals(400, assertThrows(BusinessException.class, () -> metadataService.save(POST_ID,
                request(null, null, null, "/\\evil.example.com/a.png"))).getCode());
        assertEquals(400, assertThrows(BusinessException.class, () -> metadataService.save(POST_ID,
                request(null, null, null, "uploads/a.png"))).getCode());
    }

    @Test
    @DisplayName("save_KeepsGeneratedAtFromLlmRun")
    void save_KeepsGeneratedAtFromLlmRun() {
        PostSeo generated = new PostSeo();
        generated.setPostId(POST_ID);
        generated.setSeoTitle("模型标题");
        generated.setSource("llm");
        LocalDateTime generatedAt = LocalDateTime.of(2026, 10, 20, 9, 30);
        generated.setGeneratedAt(generatedAt);
        postSeoMapper.insert(generated);

        SeoDetailVO detail = metadataService.save(POST_ID, request("手工标题", null, null, null));

        assertEquals("manual", detail.getSource());
        assertEquals(generatedAt.toString(), detail.getGeneratedAt(),
                "手工编辑不该抹掉「上次模型生成时间」");
    }

    @Test
    @DisplayName("save_MissingPost_Throws404")
    void save_MissingPost_Throws404() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> metadataService.save(999_999_997L, request("标题", null, null, null)));

        assertEquals(404, error.getCode());
    }

    @Test
    @DisplayName("delete_ClearsRowAndFallsBackToDerived")
    void delete_ClearsRowAndFallsBackToDerived() {
        metadataService.save(POST_ID, request("自定义标题", "自定义描述", null, null));

        metadataService.delete(POST_ID);

        assertNull(postSeoMapper.selectById(POST_ID));
        SeoDetailVO detail = auditService.detailOrDerived(POST_ID);
        assertFalse(detail.isHasSeo());
        assertEquals("手工 SEO 测试文章", detail.getEffectiveTitle());
    }

    @Test
    @DisplayName("delete_IsIdempotent")
    void delete_IsIdempotent() {
        metadataService.delete(POST_ID);
        metadataService.delete(POST_ID);

        assertNull(postSeoMapper.selectById(POST_ID));
    }

    @Test
    @DisplayName("detail_Throws404WhenNoSeoRow")
    void detail_Throws404WhenNoSeoRow() {
        BusinessException error = assertThrows(BusinessException.class, () -> auditService.detail(POST_ID));

        assertEquals(404, error.getCode());
    }
}
