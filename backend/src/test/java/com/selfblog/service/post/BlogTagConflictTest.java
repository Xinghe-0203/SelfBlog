package com.selfblog.service.post;

import com.selfblog.entity.BlogTag;
import com.selfblog.mapper.BlogTagMapper;
import com.selfblog.service.post.BlogTagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 唯一键冲突的“恢复同名标签”必须覆盖所有数据库的异常类型。
 *
 * <p>真实联调发现：Spring 只把 H2/MySQL 的唯一键冲突翻译成 {@link DuplicateKeyException}，
 * SQLite 抛出的约束错误会落到父类 {@link DataIntegrityViolationException}，
 * 只捕获子类时 SQLite 上仍然 500。这里直接把父类异常喂给服务，锁定该契约。
 */
@SpringBootTest
@ActiveProfiles("test")
class BlogTagConflictTest {
    @Autowired BlogTagService tags;
    @MockBean BlogTagMapper mapper;

    private BlogTag deletedTag(Long id, String name) {
        BlogTag tag = new BlogTag();
        tag.setId(id);
        tag.setName(name);
        tag.setIsDeleted(1);
        return tag;
    }

    private static UncategorizedSQLException sqliteConstraintViolation() {
        // 真实联调里 SQLite 的 SQLState 为 null、error code 19。
        return new UncategorizedSQLException("insert", "INSERT", new SQLException("SQLITE_CONSTRAINT_UNIQUE", null, 19));
    }

    @Test
    void createTagRecoversWhenSqliteReportsAnUncategorizedConstraintError() {
        when(mapper.insert(any(BlogTag.class))).thenThrow(sqliteConstraintViolation());
        when(mapper.selectRawByName("同名标签")).thenReturn(deletedTag(7L, "同名标签"));
        when(mapper.restoreById(anyLong())).thenReturn(1);

        BlogTag created = tags.createTag("同名标签");

        assertEquals(7L, created.getId());
        verify(mapper).restoreById(7L);
    }

    @Test
    void getOrCreateTagRecoversForSqliteStyleViolationsToo() {
        when(mapper.insert(any(BlogTag.class))).thenThrow(sqliteConstraintViolation());
        when(mapper.selectRawByName("发布标签")).thenReturn(deletedTag(9L, "发布标签"));

        assertEquals(9L, tags.getOrCreateTag("发布标签"));
        verify(mapper).restoreById(9L);
    }

    @Test
    void aPlainDuplicateKeyStillRecoversForMysqlStyleDatabases() {
        when(mapper.insert(any(BlogTag.class))).thenThrow(new DuplicateKeyException("Duplicate entry"));
        when(mapper.selectRawByName("旧标签")).thenReturn(deletedTag(11L, "旧标签"));

        assertEquals(11L, tags.getOrCreateTag("旧标签").longValue());
    }
}
