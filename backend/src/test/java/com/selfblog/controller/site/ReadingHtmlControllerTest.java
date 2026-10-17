package com.selfblog.controller.site;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 生产同构（/api 前缀 + 单作者模式）的根路径 HTML 契约。 */
@SpringBootTest(properties = {
        "app.reading-template=static/reading-fixture.html",
        "app.api-prefix=/api",
        "app.single-author-mode=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReadingHtmlControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.selfblog.mapper.BlogPostMapper postsMapper;

    @BeforeEach void data() {
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,summary,content,status,is_deleted,create_time,view_count) VALUES(990001,1,'可抓取文章','摘要','## 标题\n\n正文内容段落。',1,0,TIMESTAMP '2026-10-15 10:00:00',5)");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) VALUES(990002,1,'未发布','秘密不可见',0,0,TIMESTAMP '2026-10-16 10:00:00')");
    }

    @Test void publishedArticleIsReadableWithoutJavaScriptAndRevalidated() throws Exception {
        mvc.perform(get("/post/990001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(header().string("Cache-Control", "public, max-age=0, must-revalidate"))
                .andExpect(header().exists("ETag"))
                .andExpect(content().string(containsString("正文内容段落")))
                .andExpect(content().string(containsString("index,follow")))
                .andExpect(content().string(containsString("application/ld+json")));
    }

    /** 未变更的阅读页以 304 结束；改过内容之后同一个 ETag 必须重新返回 200 正文。 */
    @Test void unchangedReadingPageAnswers304AndChangedContentInvalidatesTheTag() throws Exception {
        String etag = mvc.perform(get("/post/990001"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("ETag");
        assertNotNull(etag);
        mvc.perform(get("/post/990001").header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag))
                .andExpect(header().string("Cache-Control", "public, max-age=0, must-revalidate"))
                .andExpect(content().string(""));

        // 走 Mapper 而不是 JdbcTemplate：MyBatis 的本地会话缓存在同一事务里必须被写操作清掉，
        // 否则这里读到的还是旧标题（这也正是真实编辑流程会走的路径）。
        com.selfblog.entity.BlogPost changed = new com.selfblog.entity.BlogPost();
        changed.setId(990001L);
        changed.setTitle("改过的标题");
        postsMapper.updateById(changed);
        mvc.perform(get("/post/990001").header("If-None-Match", etag))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("改过的标题")));
    }

    @Test void headMatchesGetAndDoesNotRenderABody() throws Exception {
        mvc.perform(head("/post/990001"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test void draftsAndUnknownPathsAreReal404WithNoindexHtml() throws Exception {
        for (String path : new String[] { "/post/990002", "/post/999999", "/no-such-page" }) {
            mvc.perform(get(path))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(content().string(containsString("noindex")))
                    .andExpect(content().string(not(containsString("秘密不可见"))))
                    .andExpect(content().string(not(containsString("Exception"))));
        }
    }

    @Test void adminShellKeepsTheAppWithoutLeakingData() throws Exception {
        mvc.perform(get("/admin/pages/about"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("noindex")))
                .andExpect(content().string(not(containsString("seo-reading"))))
                .andExpect(content().string(containsString("/assets/index-abc123.js")));
    }

    @Test void indexAliasRedirectsToRootWithoutASecondIndexableCopy() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isPermanentRedirect())
                .andExpect(header().string("Location", "/"));
    }

    @Test void spoofedProxyHeadersCannotChangeTheCanonicalOrigin() throws Exception {
        mvc.perform(get("/").header("X-Forwarded-Host", "attacker.test").header("X-Forwarded-Proto", "javascript"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("http://localhost/")))
                .andExpect(content().string(not(containsString("attacker.test"))));
    }

    @Test void apiAndResourceBoundariesStillReturnTheirOwnResponses() throws Exception {
        mvc.perform(get("/api/post/list")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(not(containsString("seo-reading"))));
        mvc.perform(get("/api/admin/pages")).andExpect(status().isUnauthorized());
    }
}
