package com.selfblog.service.post;

import com.selfblog.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class PublishedPostQueryTest {
    @Autowired PostQueryService queries;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void data() {
        jdbc.update("DELETE FROM blog_post");
        insert(880001, 1, 0); insert(880002, 0, 0); insert(880003, 1, 1);
    }
    void insert(long id, int status, int deleted) {
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,view_count) VALUES(?,42,?, '严格公开正文',?,?,7)", id, "公开测试" + id, status, deleted);
    }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }
    @Test void strictDetailDoesNotInheritAuthorDraftPrivilege() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(42L, null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
        assertEquals("严格公开正文", queries.getPostDetail(880002L).getContent());
        assertEquals(404, assertThrows(BusinessException.class, () -> queries.getPublishedPostDetail(880002L)).getCode());
    }
    @Test void guestCanReadPublishedBodyWithoutIncrementingViews() {
        assertEquals("严格公开正文", queries.getPublishedPostDetail(880001L).getContent());
        assertEquals(7L, jdbc.queryForObject("SELECT view_count FROM blog_post WHERE id=880001", Long.class));
    }
    @Test void deletedAndMissingBodiesAreNotPublic() {
        for (long id : List.of(880003L, 999999L)) assertEquals(404, assertThrows(BusinessException.class, () -> queries.getPublishedPostDetail(id)).getCode());
    }
    @Test void metadataIsPublishedOnlyAndDoesNotSelectBody() {
        var page = queries.getPublishedMetadata(1, 1000);
        assertEquals(1, page.getTotal());
        assertEquals(880001L, page.getRecords().get(0).getId());
        assertNull(page.getRecords().get(0).getContent());
        assertNotNull(page.getRecords().get(0).getTitle());
    }
    @Test void metadataPagesHaveStableUniqueIdOrder() {
        insert(880004, 1, 0); insert(880005, 1, 0);
        assertEquals(List.of(880001L, 880004L), queries.getPublishedMetadata(1, 2).getRecords().stream().map(p -> p.getId()).toList());
        assertEquals(List.of(880005L), queries.getPublishedMetadata(2, 2).getRecords().stream().map(p -> p.getId()).toList());
    }
    @Test void rejectsUnboundedOrInvalidMetadataQueries() {
        assertThrows(IllegalArgumentException.class, () -> queries.getPublishedMetadata(0, 1000));
        assertThrows(IllegalArgumentException.class, () -> queries.getPublishedMetadata(1, 1001));
    }
}
