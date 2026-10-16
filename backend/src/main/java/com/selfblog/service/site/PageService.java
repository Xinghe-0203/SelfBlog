package com.selfblog.service.site;

import com.selfblog.dto.site.PageModels.*;
import java.util.List;

public interface PageService {
    PublicManifest getManifest();
    PublicPage getPublished(String id);
    PublicPage getBySlug(String slug);
    List<PageEditor> listPages();
    PageEditor getEditor(String id);
    PageEditor createPage(String slug, String title);
    PageEditor saveDraft(String id, long revision, String slug, PageConfig config);
    PageEditor publish(String id, long revision);
    PageEditor unpublish(String id, long revision);
    void deletePage(String id, long revision);
    NavigationState getNavigation();
    NavigationState saveNavigation(long revision, List<NavigationItem> items);
}
