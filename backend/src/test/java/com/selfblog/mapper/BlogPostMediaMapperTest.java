package com.selfblog.mapper;

import com.selfblog.entity.BlogPostMedia;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 文章媒体关联的软删除语义。
 *
 * <p>编辑文章时 {@code setPostMedia} 会先 {@code deleteByPostId}（逻辑删除，is_deleted=1）
 * 再批量插入新关联；查询必须只返回未删除的行，否则编辑器/公开接口会带回已经移除的旧图。
 * 这是手写 SQL 的软删除责任（见根 CLAUDE.md：手写 SQL 必须自己带 is_deleted 条件）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BlogPostMediaMapperTest {
    @Autowired BlogPostMediaMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void replacingPostMediaDoesNotReturnSoftDeletedRows() {
        long postId = 880101L;
        mapper.batchInsert(postId, List.of(900001L, 900002L));
        assertEquals(List.of(900001L, 900002L), mediaIds(postId));

        // 模拟编辑：先逻辑删除全部，再写入新的关联
        mapper.deleteByPostId(postId);
        mapper.batchInsert(postId, List.of(900003L));

        assertEquals(List.of(900003L), mediaIds(postId), "被移除的旧关联不能继续返回");
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM blog_post_media WHERE post_id = ? AND is_deleted = 0", Integer.class, postId));
    }

    private List<Long> mediaIds(long postId) {
        return mapper.selectByPostId(postId).stream().map(BlogPostMedia::getMediaId).sorted().toList();
    }
}
