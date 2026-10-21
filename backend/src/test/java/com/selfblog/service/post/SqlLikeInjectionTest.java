package com.selfblog.service.post;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.dto.post.PostQueryRequest;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.utils.SqlLikeUtils;
import com.selfblog.vo.post.PostListResponse;
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
 * 回归：关键词 LIKE 必须转义通配符，否则 {@code %} / {@code _} 会匹配全部内容。
 *
 * <p>修复前 {@code GET /api/post/search/advanced?keyword=%25} 返回全站文章
 * （探针实测 total=15），因为用户输入的 {@code %} 被直接拼进 LIKE 模式。
 * 现在所有关键词 LIKE 都经 {@link SqlLikeUtils#containsPattern(String)} 转义并显式带上
 * {@code ESCAPE '!'}，因此 {@code %} 只匹配字面量百分号，普通关键词行为不变。</p>
 *
 * <p>这里用真实 H2 执行 SQL（不是断言字符串拼接），才能证明转义与 ESCAPE 子句真的生效。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("关键词 LIKE 通配符转义")
class SqlLikeInjectionTest {

    private static final long AUTHOR_ID = 447700L;

    @Autowired
    private PostQueryService queries;

    @Autowired
    private BlogTagService blogTagService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM blog_post_tag");
        jdbc.update("DELETE FROM blog_tag");
        jdbc.update("DELETE FROM blog_post");
        // 一篇文章标题含字面量 %，一篇含字面量 _，其余为普通文章。
        insertPost(882001L, "折扣 100% 达成", "正文");
        insertPost(882002L, "变量名 user_id 说明", "正文");
        insertPost(882003L, "普通文章甲", "正文");
        insertPost(882004L, "普通文章乙", "正文");

        jdbc.update("INSERT INTO blog_tag(id, name, is_deleted) VALUES (882101, '折扣 100% 标签', 0)");
        jdbc.update("INSERT INTO blog_tag(id, name, is_deleted) VALUES (882102, '普通标签', 0)");
    }

    @Test
    @DisplayName("keyword=% 只匹配标题里含字面量百分号的文章")
    void percentSignMatchesOnlyLiteralOccurrences() {
        IPage<PostListResponse> page = queries.getPostList(request("%"));

        assertEquals(List.of("折扣 100% 达成"),
                page.getRecords().stream().map(PostListResponse::getTitle).toList(),
                "未转义时 % 会命中全部文章，转义后必须只命中含字面量 % 的那一篇");
    }

    @Test
    @DisplayName("keyword=_ 只匹配标题里含字面量下划线的文章")
    void underscoreMatchesOnlyLiteralOccurrences() {
        IPage<PostListResponse> page = queries.getPostList(request("_"));

        assertEquals(List.of("变量名 user_id 说明"),
                page.getRecords().stream().map(PostListResponse::getTitle).toList(),
                "_ 在未转义时匹配任意单字符，转义后必须只命中含字面量下划线的那一篇");
    }

    @Test
    @DisplayName("转义字符本身（!）也只按字面量匹配")
    void escapeCharacterItselfIsLiteral() {
        insertPost(882005L, "感叹号 100! 结尾", "正文");

        IPage<PostListResponse> page = queries.getPostList(request("!"));

        assertEquals(List.of("感叹号 100! 结尾"),
                page.getRecords().stream().map(PostListResponse::getTitle).toList(),
                "转义字符被自身转义后必须按字面量匹配，不能变成通配");
    }

    @Test
    @DisplayName("普通关键词仍能正常检索")
    void ordinaryKeywordStillFindsItsPosts() {
        IPage<PostListResponse> page = queries.getPostList(request("普通文章"));

        assertEquals(2, page.getTotal(), "转义不能破坏常规子串搜索");
    }

    @Test
    @DisplayName("标签搜索同样不接受通配符注入")
    void tagSearchEscapesWildcards() {
        assertTrue(blogTagService.searchTags("%").stream()
                        .allMatch(tag -> tag.getName().contains("%")),
                "标签名搜索的 % 必须只匹配字面量");
        assertEquals(1, blogTagService.searchTags("%").size());
        assertEquals(1, blogTagService.searchTags("普通").size(), "普通关键词仍可检索");
    }

    private PostQueryRequest request(String keyword) {
        PostQueryRequest request = new PostQueryRequest();
        request.setPageNum(1);
        request.setPageSize(20);
        request.setSort("latest");
        request.setKeyword(keyword);
        return request;
    }

    private void insertPost(long id, String title, String content) {
        jdbc.update("INSERT INTO blog_post(id,user_id,title,summary,content,status,is_deleted,view_count,like_count) "
                + "VALUES(?,?,?,'',?,1,0,1,0)", id, AUTHOR_ID, title, content);
    }
}
