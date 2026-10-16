package com.selfblog.controller.site;

import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.reading-template=static/reading-fixture.html",
        "app.api-prefix=/api",
        "app.single-author-mode=true",
        "SITE_BASE_URL=https://blog.example.com"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DiscoveryControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.selfblog.service.site.SiteSettingsService settings;
    @Autowired org.springframework.cache.CacheManager caches;

    @BeforeEach void clearCache() {
        var cache = caches.getCache(com.selfblog.config.cache.CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) cache.clear();
    }

    @AfterEach void clearCacheAfter() { clearCache(); }

    @BeforeEach void data() {
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM blog_tag");
        jdbc.update("DELETE FROM site_setting WHERE setting_key IN ('adminPath') OR setting_key LIKE 'page.%'");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,summary,content,status,is_deleted,create_time) VALUES(991001,1,'一号文章','摘要一','正文一',1,0,TIMESTAMP '2026-10-10 09:00:00')");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) VALUES(991002,1,'草稿','不可见',0,0,TIMESTAMP '2026-10-11 09:00:00')");
        jdbc.update("INSERT INTO blog_tag(id,name,is_deleted) VALUES(992001,'技术',0)");
    }

    @Test void robotsFollowsTheConfiguredAdminPathAndCanonicalOrigin() throws Exception {
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disallow: /admin")))
                .andExpect(content().string(containsString("Disallow: /search")))
                .andExpect(content().string(containsString("Sitemap: https://blog.example.com/sitemap.xml")));
        settings.updateSettings(java.util.Map.of("adminPath", "author"));
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disallow: /author")))
                .andExpect(content().string(not(containsString("Disallow: /admin"))));
    }

    @Test void sitemapIndexListsRealShardsAndServesThem() throws Exception {
        String index = mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andReturn().getResponse().getContentAsString();
        assertTrue(index.contains("https://blog.example.com/sitemap-site.xml"));
        assertTrue(index.contains("https://blog.example.com/sitemap-posts-1.xml"));
        assertTrue(index.contains("https://blog.example.com/sitemap-tags-1.xml"));
        assertFalse(index.contains("sitemap-pages-1.xml"));

        String site = mvc.perform(get("/sitemap-site.xml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(site.contains("<loc>https://blog.example.com/</loc>"));
        assertTrue(site.contains("https://blog.example.com/about"));
        assertFalse(site.contains("/search"));

        String posts = mvc.perform(get("/sitemap-posts-1.xml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(posts.contains("https://blog.example.com/post/991001"));
        assertFalse(posts.contains("991002"));

        mvc.perform(get("/sitemap-posts-2.xml")).andExpect(status().isNotFound());
        mvc.perform(get("/sitemap-unknown-1.xml")).andExpect(status().isNotFound());
    }

    @Test void feedsKeepTheirOwnHundredItemLimitAndPublishedOnlyContent() throws Exception {
        List<Object[]> rows = new ArrayList<>();
        for (long id = 1000000; id < 1000110; id++) {
            rows.add(new Object[] { id, "批量文章" + id });
        }
        jdbc.batchUpdate("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) VALUES(?,1,?,'正文',1,0,TIMESTAMP '2026-10-12 09:00:00')", rows);
        String feed = mvc.perform(get("/feed.xml")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rss+xml"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(100, feed.split("<item>", -1).length - 1);
        assertFalse(feed.contains("991002"));
        assertTrue(feed.contains("https://blog.example.com/post/"));
    }
}
