package com.selfblog.controller.site;

import com.selfblog.controller.admin.AdminPageController;
import com.selfblog.dto.site.PageModels.*;
import com.selfblog.service.site.PageService;
import com.selfblog.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminPageController.class, PublicPageController.class})
class PageControllerTest {
    @Autowired MockMvc mvc;
    @MockBean PageService pages;
    @MockBean JwtUtils jwt;

    @Test void publicManifestNeedsNoLogin() throws Exception {
        when(pages.getManifest()).thenReturn(new PublicManifest(List.of(), List.of()));
        mvc.perform(get("/site/pages")).andExpect(status().isOk()).andExpect(jsonPath("$.data.pages").isArray());
    }
    @Test void unpublishedSlugReturns404() throws Exception {
        when(pages.getBySlug("not-published")).thenThrow(new com.selfblog.common.exception.BusinessException(404, "页面不存在"));
        mvc.perform(get("/site/pages/by-slug/not-published")).andExpect(status().isNotFound());
    }
    @Test void anonymousManagementIsDenied() throws Exception {
        mvc.perform(get("/admin/pages")).andExpect(status().isForbidden());
        verifyNoInteractions(pages);
    }
    @Test @WithMockUser(roles = "user") void nonAdminManagementIsDenied() throws Exception {
        mvc.perform(get("/admin/pages")).andExpect(status().isForbidden());
        verifyNoInteractions(pages);
    }
    @Test @WithMockUser(roles = "admin") void adminCanListAndSaveDraft() throws Exception {
        when(pages.listPages()).thenReturn(List.of());
        mvc.perform(get("/admin/pages")).andExpect(status().isOk());
        mvc.perform(put("/admin/pages/about/draft").contentType(MediaType.APPLICATION_JSON)
            .content("{\"revision\":0,\"config\":{\"description\":\"\",\"body\":\"## 内容\"}}"))
            .andExpect(status().isOk());
        verify(pages).saveDraft(eq("about"), eq(0L), isNull(), argThat(c -> "".equals(c.description()) && "## 内容".equals(c.body())));
    }
    @Test @WithMockUser(roles = "admin") void unknownTopLevelAndNestedFieldsReturn400() throws Exception {
        for (String body : List.of("{\"revision\":0,\"execute\":true,\"config\":{}}", "{\"revision\":0,\"config\":{\"script\":\"alert(1)\"}}", "{\"revision\":0,\"config\":{\"style\":{\"css\":\"*{display:none}\"}}}")) {
            mvc.perform(put("/admin/pages/about/draft").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(pages);
    }
    @Test @WithMockUser(roles = "admin") void missingRevisionDoesNotDefaultToZero() throws Exception {
        mvc.perform(put("/admin/pages/about/draft").contentType(MediaType.APPLICATION_JSON).content("{\"config\":{}}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(pages);
    }
    @Test @WithMockUser(roles = "admin") void mutationEndpointsForwardExpectedRevision() throws Exception {
        mvc.perform(put("/admin/pages/about/publication").contentType(MediaType.APPLICATION_JSON).content("{\"revision\":2}"))
            .andExpect(status().isOk());
        verify(pages).publish("about", 2);
        mvc.perform(delete("/admin/pages/custom-id/publication").param("revision", "3")).andExpect(status().isOk());
        verify(pages).unpublish("custom-id", 3);
        mvc.perform(delete("/admin/pages/custom-id").param("revision", "4")).andExpect(status().isOk());
        verify(pages).deletePage("custom-id", 4);
    }
}
