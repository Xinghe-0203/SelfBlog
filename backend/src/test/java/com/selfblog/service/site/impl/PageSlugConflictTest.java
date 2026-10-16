package com.selfblog.service.site.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.SiteSetting;
import com.selfblog.mapper.SiteSettingMapper;
import com.selfblog.service.site.PageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 地址预留的唯一键冲突必须翻译成 409，而不是 500。
 *
 * <p>真实联调发现（标签删除后重建同一根因）：SQLite 的唯一约束错误不会被 Spring
 * 翻译成 {@code DuplicateKeyException}，只捕获子类会让冲突以 500 暴露。
 */
@SpringBootTest
@ActiveProfiles("test")
class PageSlugConflictTest {
    @Autowired PageService pages;
    @MockBean SiteSettingMapper mapper;

    @Test
    void sqliteStyleIntegrityViolationOnSlugReservationBecomes409() {
        // SQLite 的约束错误是 UncategorizedSQLException（SQLState null、error code 19）。
        when(mapper.insert(any(SiteSetting.class)))
                .thenThrow(new UncategorizedSQLException("insert", "INSERT", new SQLException("SQLITE_CONSTRAINT_UNIQUE", null, 19)));

        BusinessException error = assertThrows(BusinessException.class, () -> pages.createPage("conflict-slug", "冲突页面"));

        assertEquals(409, error.getCode());
    }

    @Test
    void mysqlStyleDuplicateKeyAlsoBecomes409() {
        when(mapper.insert(any(SiteSetting.class))).thenThrow(new DuplicateKeyException("Duplicate entry"));

        BusinessException error = assertThrows(BusinessException.class, () -> pages.createPage("dup-slug", "重复页面"));

        assertEquals(409, error.getCode());
    }
}
