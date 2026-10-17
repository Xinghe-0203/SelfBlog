package com.selfblog.service.site.seo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.PageModels.PageConfig;
import com.selfblog.service.site.PageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** 站点地图完整性：超过订阅源 100 篇上限后仍不丢 URL、不重复、不泄露草稿。 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SitemapServiceTest {
    private static final Pattern LOC = Pattern.compile("<loc>([^<]+)</loc>");
    private static final String BASE = "https://blog.example.com";

    @Autowired SitemapService sitemaps;
    @Autowired JdbcTemplate jdbc;
    @Autowired PageService pages;
    @Autowired ObjectMapper json;

    @BeforeEach void clean() {
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM site_setting WHERE setting_key LIKE 'page.%'");
    }

    @Test void allPublishedPostsAppearExactlyOnceAcrossShardsBeyondOneHundred() {
        List<Object[]> rows = new ArrayList<>();
        for (long index = 0; index < 1001; index++) rows.add(new Object[] { 2_000_000L + index, "批量文章" + index });
        jdbc.batchUpdate("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) "
                + "VALUES(?,1,?,'正文',1,0,TIMESTAMP '2026-10-01 08:00:00')", rows);
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) VALUES(2999999,1,'待审草稿','x',0,0,TIMESTAMP '2026-10-01 08:00:00')");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) VALUES(2999998,1,'已删除','x',1,1,TIMESTAMP '2026-10-01 08:00:00')");

        String shardOne = sitemaps.shard("posts", 1, BASE);
        String shardTwo = sitemaps.shard("posts", 2, BASE);
        assertEquals(1000, occurrences(shardOne));
        assertEquals(1, occurrences(shardTwo));
        Set<String> locs = new LinkedHashSet<>(locs(shardOne));
        locs.addAll(locs(shardTwo));
        assertEquals(1001, locs.size());
        assertTrue(locs.contains(BASE + "/post/2000000"));
        assertTrue(locs.contains(BASE + "/post/2001000"));
        assertTrue(locs.stream().noneMatch(loc -> loc.contains("2999999") || loc.contains("2999998")));

        assertEquals(404, assertThrows(BusinessException.class, () -> sitemaps.shard("posts", 3, BASE)).getCode());
        String index = sitemaps.index(BASE);
        assertTrue(index.contains(BASE + "/sitemap-posts-1.xml"));
        assertTrue(index.contains(BASE + "/sitemap-posts-2.xml"));
        assertFalse(index.contains("sitemap-posts-3.xml"));
    }

    @Test void perPageIndexabilityExcludesSearchAndDisabledPagesLikeTheSitemapContract() {
        pages.saveDraft("about", 0, null, config(Map.of("seo", Map.of("indexable", false))));
        pages.publish("about", 1);
        String site = sitemaps.shard("site", 1, BASE);
        assertTrue(site.contains(BASE + "/"));
        assertTrue(site.contains(BASE + "/archives"));
        assertFalse(site.contains("/about"));
        assertFalse(site.contains("/search"));
        assertFalse(site.contains("/admin"));

        var created = pages.createPage("projects", "我的项目");
        pages.saveDraft(created.id(), created.draft().revision(), null, config(Map.of("title", "我的项目", "body", "正文")));
        pages.publish(created.id(), pages.getEditor(created.id()).draft().revision());
        String pagesShard = sitemaps.shard("pages", 1, BASE);
        assertTrue(pagesShard.contains(BASE + "/pages/projects"));
        assertTrue(sitemaps.index(BASE).contains("sitemap-pages-1.xml"));

        pages.unpublish(created.id(), pages.getEditor(created.id()).draft().revision());
        assertEquals(404, assertThrows(BusinessException.class, () -> sitemaps.shard("pages", 1, BASE)).getCode());
        assertFalse(sitemaps.index(BASE).contains("sitemap-pages-1.xml"));
    }

    @Test void emptyCollectionsAreNotAdvertisedAndTheirShardsAreReal404() {
        // 索引不能列出一个永远不会成功返回的分片。
        String index = sitemaps.index(BASE);
        assertFalse(index.contains("sitemap-tags-1.xml"), "empty tag collection must not be advertised");
        assertFalse(index.contains("sitemap-posts-1.xml"), "no published posts must not be advertised");
        assertEquals(404, assertThrows(BusinessException.class, () -> sitemaps.shard("tags", 1, BASE)).getCode());
        assertEquals(404, assertThrows(BusinessException.class, () -> sitemaps.shard("pages", 1, BASE)).getCode());
    }

    /** 标签分片带 lastmod：取该标签下最新一篇已发布文章的时间；草稿与空标签不产生 lastmod。 */
    @Test void tagShardsCarryLastModifiedFromTheirNewestPublishedPost() {
        jdbc.update("INSERT INTO blog_tag(id,name,is_deleted) VALUES(880001,'服务端渲染',0)");
        jdbc.update("INSERT INTO blog_tag(id,name,is_deleted) VALUES(880002,'空标签',0)");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time,update_time) "
                + "VALUES(880011,1,'旧文','x',1,0,TIMESTAMP '2026-10-01 08:00:00',TIMESTAMP '2026-10-02 08:00:00')");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time,update_time) "
                + "VALUES(880012,1,'新文','x',1,0,TIMESTAMP '2026-10-05 08:00:00',TIMESTAMP '2026-10-09 12:30:00')");
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time,update_time) "
                + "VALUES(880013,1,'草稿','x',0,0,TIMESTAMP '2026-12-01 08:00:00',TIMESTAMP '2026-12-01 08:00:00')");
        jdbc.update("INSERT INTO blog_post_tag(post_id,tag_id,is_deleted) VALUES(880011,880001,0)");
        jdbc.update("INSERT INTO blog_post_tag(post_id,tag_id,is_deleted) VALUES(880012,880001,0)");
        jdbc.update("INSERT INTO blog_post_tag(post_id,tag_id,is_deleted) VALUES(880013,880002,0)");

        String shard = sitemaps.shard("tags", 1, BASE);
        assertTrue(shard.contains("<url><loc>" + BASE + "/tag/880001</loc><lastmod>2026-10-09</lastmod></url>"),
                "标签 lastmod 必须取最新已发布文章的时间");
        assertTrue(shard.contains("<url><loc>" + BASE + "/tag/880002</loc></url>"),
                "没有已发布文章的标签仍然可访问，只是没有 lastmod");
        assertTrue(sitemaps.index(BASE).contains("sitemap-tags-1.xml"));
    }

    private static int occurrences(String xml) {
        return xml.split("<url>", -1).length - 1;
    }

    private static List<String> locs(String xml) {
        List<String> values = new ArrayList<>();
        Matcher matcher = LOC.matcher(xml);
        while (matcher.find()) values.add(matcher.group(1));
        return values;
    }

    private PageConfig config(Map<String, Object> values) {
        return json.convertValue(values, PageConfig.class);
    }
}
