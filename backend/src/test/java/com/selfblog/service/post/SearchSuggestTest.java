package com.selfblog.service.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 搜索建议（标题自动补全）读路径契约。
 *
 * <p>前端 typeahead 把空列表直接视为「无建议」，所以 {@code keyword} 缺参（控制器
 * {@code required=false} 传入 null）、空白串与不足 2 个字符都必须返回**空列表**而不是
 * 抛错或返回 null；建议串只来自已发布文章的标题（存储值，前端按纯文本渲染）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("搜索建议（标题自动补全）")
class SearchSuggestTest {

    private static final long AUTHOR_ID = 447800L;

    @Autowired
    private PostQueryService queries;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM blog_post");
        insertPost(883001L, "Spring Boot 事务实战", 1, 30);
        insertPost(883002L, "Spring Security 入门", 1, 10);
        insertPost(883003L, "草稿：Spring 未发布", 0, 99);
    }

    @Test
    @DisplayName("keyword 缺参/空白一律返回空列表，前端视为「无建议」")
    void blankKeywordReturnsEmptyList() {
        assertTrue(queries.getSearchSuggestions(null).isEmpty(), "缺参（null）必须是空列表");
        assertTrue(queries.getSearchSuggestions("").isEmpty(), "空串必须是空列表");
        assertTrue(queries.getSearchSuggestions("   ").isEmpty(), "纯空白必须是空列表");
    }

    @Test
    @DisplayName("不足 2 个字符的关键词直接返回空列表")
    void singleCharacterKeywordReturnsEmptyList() {
        assertTrue(queries.getSearchSuggestions("S").isEmpty(), "单字符过短，不触发检索");
    }

    @Test
    @DisplayName("建议只来自已发布文章标题，按浏览量降序")
    void suggestionsComeFromPublishedTitlesOrderedByViews() {
        List<String> suggestions = queries.getSearchSuggestions("Spring");
        // 未发布的 883003 标题同样包含 “Spring”，若状态过滤缺失就会混进建议里。
        assertEquals(List.of("Spring Boot 事务实战", "Spring Security 入门"), suggestions,
                "只返回已发布标题（浏览量降序），草稿绝不出现");
    }

    @Test
    @DisplayName("无命中时返回空列表而不是错误")
    void noMatchReturnsEmptyList() {
        assertTrue(queries.getSearchSuggestions("不存在的关键词组合").isEmpty());
    }

    private void insertPost(long id, String title, int status, int viewCount) {
        jdbc.update("INSERT INTO blog_post(id,user_id,title,summary,content,status,is_deleted,view_count,like_count) "
                + "VALUES(?,?,?,'',?,?,0,?,0)", id, AUTHOR_ID, title, "正文", status, viewCount);
    }
}
