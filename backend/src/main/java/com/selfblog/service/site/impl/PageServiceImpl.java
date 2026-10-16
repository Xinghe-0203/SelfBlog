package com.selfblog.service.site.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.PageModels.*;
import com.selfblog.entity.SiteSetting;
import com.selfblog.mapper.SiteSettingMapper;
import com.selfblog.service.site.PageService;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.service.site.page.PageCatalog;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Versioned page documents in existing KV storage; never part of public global settings. */
@Service
@Transactional(readOnly = true)
public class PageServiceImpl implements PageService {
    private static final String DRAFT = "page.draft.", PUBLISHED = "page.published.", SLUG = "page.slug.", NAV = "page.navigation";
    private final SiteSettingMapper mapper;
    private final ObjectMapper json;
    private final SiteSettingsService settings;

    public PageServiceImpl(SiteSettingMapper mapper, ObjectMapper json, SiteSettingsService settings) {
        this.mapper = mapper;
        this.json = json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.settings = settings;
    }
    @Override public List<PageEditor> listPages() {
        var editors = new LinkedHashMap<String, PageEditor>();
        PageCatalog.BUILTINS.keySet().forEach(id -> editors.put(id, getEditor(id)));
        for (var row : rows(DRAFT)) {
            String id = row.getSettingKey().substring(DRAFT.length());
            if (!PageCatalog.builtin(id)) editors.put(id, getEditor(id));
        }
        return List.copyOf(editors.values());
    }
    @Override public PageEditor getEditor(String id) {
        PageCatalog.definition(id);
        var row = mapper.selectById(DRAFT + id);
        var draft = row == null ? defaultState(id) : decode(row, PageState.class);
        var publicRow = mapper.selectById(PUBLISHED + id);
        var published = publicRow == null ? (PageCatalog.builtin(id) ? defaultState(id) : null) : decode(publicRow, PageState.class);
        return new PageEditor(id, PageCatalog.builtin(id), PageCatalog.definition(id).label(), draft, published);
    }
    @Override public PublicPage getPublished(String id) {
        PageCatalog.definition(id);
        var row = mapper.selectById(PUBLISHED + id);
        var state = row == null ? defaultState(id) : decode(row, PageState.class);
        return publicPage(id, state, false);
    }
    @Override public PublicPage getBySlug(String slug) {
        PageCatalog.slug(slug);
        if ("pages".equals(settings.getSettings().get("adminPath"))) throw new BusinessException(404, "页面不存在");
        var reservation = mapper.selectById(SLUG + slug);
        if (reservation == null) throw new BusinessException(404, "页面不存在或尚未发布");
        var published = getPublished(reservation.getSettingValue());
        if (!slug.equals(published.slug())) throw new BusinessException(404, "页面不存在");
        return published;
    }
    @Override public PublicManifest getManifest() {
        var pages = new LinkedHashMap<String, PublicPage>();
        PageCatalog.BUILTINS.keySet().forEach(id -> pages.put(id, publicPage(id, defaultState(id), true)));
        for (var row : rows(PUBLISHED)) {
            String id = row.getSettingKey().substring(PUBLISHED.length());
            PageCatalog.definition(id);
            pages.put(id, publicPage(id, decode(row, PageState.class), true));
        }
        var links = new ArrayList<PublicNavigationItem>();
        for (var item : getNavigation().items()) {
            var page = pages.get(item.pageId());
            if (page == null) continue;
            String path = PageCatalog.builtin(page.id()) ? PageCatalog.definition(page.id()).path() : "/pages/" + page.slug();
            if (path != null) links.add(new PublicNavigationItem(item.pageId(), item.label(), path));
        }
        return new PublicManifest(List.copyOf(pages.values()), links);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public PageEditor createPage(String slug, String title) {
        if ("pages".equals(settings.getSettings().get("adminPath"))) throw new BusinessException(409, "当前后台路径为pages，请先在网站设置修改后台路径，再创建内容页");
        PageCatalog.slug(slug);
        String id = "custom-" + UUID.randomUUID().toString().replace("-", "");
        var config = new PageConfig(title, null, null, null, null, null, null);
        PageCatalog.validate(id, config);
        reserveSlug(slug, id);
        compareAndSet(DRAFT + id, null, encode(new PageState(1, 1, slug, false, now(), config)));
        return getEditor(id);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public PageEditor saveDraft(String id, long revision, String slug, PageConfig config) {
        PageCatalog.validate(id, config);
        var row = mapper.selectById(DRAFT + id);
        var current = checkRevision(id, row, revision);
        String nextSlug = current.slug();
        if (PageCatalog.builtin(id)) {
            if (slug != null && !slug.isEmpty()) throw new BusinessException(400, "内置页面地址不能修改");
        } else if (slug != null && !slug.equals(nextSlug)) {
            if (current.everPublished()) throw new BusinessException(400, "首次发布后不能修改页面地址");
            nextSlug = PageCatalog.slug(slug);
            reserveSlug(nextSlug, id);
        }
        var next = new PageState(1, revision + 1, nextSlug, current.everPublished(), now(), config);
        compareAndSet(DRAFT + id, row, encode(next));
        if (!Objects.equals(nextSlug, current.slug())) mapper.deleteById(SLUG + current.slug());
        return getEditor(id);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public PageEditor publish(String id, long revision) {
        var row = mapper.selectById(DRAFT + id);
        var current = checkRevision(id, row, revision);
        PageCatalog.validate(id, current.config());
        var next = new PageState(1, revision + 1, current.slug(), true, now(), current.config());
        compareAndSet(DRAFT + id, row, encode(next));
        compareAndSet(PUBLISHED + id, mapper.selectById(PUBLISHED + id), encode(next));
        return getEditor(id);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public PageEditor unpublish(String id, long revision) {
        requireCustom(id);
        var row = mapper.selectById(DRAFT + id);
        var current = checkRevision(id, row, revision);
        compareAndSet(DRAFT + id, row, encode(new PageState(1, revision + 1, current.slug(), current.everPublished(), now(), current.config())));
        mapper.deleteById(PUBLISHED + id);
        return getEditor(id);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public void deletePage(String id, long revision) {
        requireCustom(id);
        var row = mapper.selectById(DRAFT + id);
        var current = checkRevision(id, row, revision);
        // Claim the expected revision before deleting; concurrent publishers must conflict.
        compareAndSet(DRAFT + id, row, encode(new PageState(1, revision + 1, current.slug(), current.everPublished(), now(), current.config())));
        mapper.deleteById(PUBLISHED + id);
        mapper.deleteById(DRAFT + id);
        mapper.deleteById(SLUG + current.slug());
    }
    @Override public NavigationState getNavigation() {
        var row = mapper.selectById(NAV);
        var state = row == null ? new NavigationState(1, 0, List.of(new NavigationItem("home", "首页"), new NavigationItem("archives", "归档"), new NavigationItem("gallery", "相册"), new NavigationItem("about", "关于"))) : decode(row, NavigationState.class);
        // Keep author editing and public navigation consistent after a target goes offline.
        // The stored revision still protects the independent navigation configuration.
        var visible = state.items().stream().filter(item -> PageCatalog.builtin(item.pageId())
                || mapper.selectById(PUBLISHED + item.pageId()) != null).toList();
        return new NavigationState(state.schemaVersion(), state.revision(), visible);
    }
    @Override @Transactional(rollbackFor = Exception.class)
    public NavigationState saveNavigation(long revision, List<NavigationItem> items) {
        if (items == null || items.isEmpty() || items.size() > 30) throw new BusinessException(400, "导航需为1-30项并包含首页");
        var ids = new HashSet<String>();
        for (var item : items) {
            if (item == null || item.label() == null || item.label().isBlank() || item.label().length() > 20 || !ids.add(item.pageId())) throw new BusinessException(400, "导航名称需为1-20字，页面不能重复");
            var definition = PageCatalog.definition(item.pageId());
            if (PageCatalog.builtin(item.pageId()) && definition.path() == null) throw new BusinessException(400, "此模板不能加入导航");
            try { getPublished(item.pageId()); }
            catch (BusinessException e) { throw new BusinessException(400, "导航只能选择已发布页面"); }
        }
        if (!ids.contains("home")) throw new BusinessException(400, "导航必须保留首页");
        var row = mapper.selectById(NAV);
        long previousRevision = row == null ? 0 : decode(row, NavigationState.class).revision();
        if (revision != previousRevision) throw conflict();
        var next = new NavigationState(1, revision + 1, List.copyOf(items));
        compareAndSet(NAV, row, encode(next));
        return next;
    }
    private List<SiteSetting> rows(String prefix) {
        return mapper.selectList(new LambdaQueryWrapper<SiteSetting>().likeRight(SiteSetting::getSettingKey, prefix).orderByAsc(SiteSetting::getSettingKey));
    }
    private PageState defaultState(String id) {
        if (!PageCatalog.builtin(id)) throw new BusinessException(404, "页面不存在或尚未发布");
        return new PageState(1, 0, null, true, null, PageConfig.empty());
    }
    private PublicPage publicPage(String id, PageState state, boolean summary) {
        return new PublicPage(id, state.slug(), state.updatedAt(), summary ? state.config().withoutBody() : state.config());
    }
    private PageState checkRevision(String id, SiteSetting row, long revision) {
        PageCatalog.definition(id);
        var state = row == null ? defaultState(id) : decode(row, PageState.class);
        if (revision != state.revision() || revision < 0) throw conflict();
        return state;
    }
    private void requireCustom(String id) {
        PageCatalog.definition(id);
        if (PageCatalog.builtin(id)) throw new BusinessException(400, "内置页面不能删除或下线，可恢复默认配置");
    }
    private void reserveSlug(String slug, String id) { compareAndSet(SLUG + slug, null, id); }
    private void compareAndSet(String key, SiteSetting previous, String next) {
        try {
            if (previous == null) {
                var row = new SiteSetting(); row.setSettingKey(key); row.setSettingValue(next);
                mapper.insert(row);
            } else {
                int changed = mapper.update(null, new LambdaUpdateWrapper<SiteSetting>()
                        .eq(SiteSetting::getSettingKey, key).eq(SiteSetting::getSettingValue, previous.getSettingValue())
                        .set(SiteSetting::getSettingValue, next));
                if (changed != 1) throw conflict();
            }
        } catch (DataAccessException e) {
            // 唯一地址/版本冲突：不同数据库翻译出的异常类型不同（SQLite 是
            // UncategorizedSQLException），统一按父类捕获后返回 409。
            throw new BusinessException(409, "地址已被占用或页面已更新，请重新加载后重试");
        }
    }
    private String encode(Object value) {
        try {
            String encoded = json.writeValueAsString(value);
            if (encoded.getBytes(StandardCharsets.UTF_8).length > 48 * 1024) throw new BusinessException(400, "页面配置过大，请缩短内容（单份配置最多48KiB）");
            return encoded;
        } catch (JsonProcessingException e) { throw new BusinessException(400, "页面配置格式无效"); }
    }
    private <T> T decode(SiteSetting row, Class<T> type) {
        try {
            T value = json.readValue(row.getSettingValue(), type);
            int version = value instanceof PageState p ? p.schemaVersion() : ((NavigationState) value).schemaVersion();
            if (version != 1) throw new BusinessException(500, "页面配置版本不受支持，请联系维护者恢复备份");
            return value;
        } catch (JsonProcessingException | IllegalArgumentException e) { throw new BusinessException(500, "页面配置无法读取，请联系维护者恢复备份"); }
    }
    private static String now() { return Instant.now().toString(); }
    private static BusinessException conflict() { return new BusinessException(409, "页面已在其他窗口更新，请先重新加载；当前编辑内容尚未覆盖服务器"); }
}
