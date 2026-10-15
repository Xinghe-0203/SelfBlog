package com.selfblog.controller;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.controller.admin.AdminSiteAssetController;
import com.selfblog.service.site.SiteAssetService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.vo.site.SiteAssetVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminSiteAssetController 单元测试。
 * 测试路径不写 /api —— 测试 profile 里 app.api-prefix 为空。
 *
 * 有两处必须照抄，否则测试根本跑不起来（已在本仓库实测确认）：
 *
 * 1. 必须 `@MockBean JwtUtils`。`@WebMvcTest` 切片会把 `SecurityConfig` 一并装配，
 *    它注册的 `jwtAuthenticationFilter` 依赖 `JwtUtils`；不 mock 会直接
 *    `NoSuchBeanDefinitionException` 起不来。（`AuthControllerTest` 也是这么做的。）
 * 2. 必须给 `@WithMockUser(roles = "admin")`。切片里的方法级安全**是生效的**：
 *    实测 admin → 200、普通用户 → 403、匿名 → 403。
 */
@WebMvcTest(AdminSiteAssetController.class)
@DisplayName("AdminSiteAssetController")
class AdminSiteAssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SiteAssetService siteAssetService;

    @MockBean
    private JwtUtils jwtUtils;

    private MockMultipartFile anyFile() {
        return new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("upload_ReturnsStoredAssetInEnvelope")
    void upload_ReturnsStoredAssetInEnvelope() throws Exception {
        SiteAssetVO vo = new SiteAssetVO();
        vo.setUrl("/uploads/site-assets/logo/abc123.png");
        vo.setKind("logo");
        vo.setSize(123L);
        vo.setOriginalName("logo.png");
        when(siteAssetService.store(any(), eq("logo"))).thenReturn(vo);

        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()).param("kind", "logo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value("/uploads/site-assets/logo/abc123.png"))
                .andExpect(jsonPath("$.data.kind").value("logo"))
                .andExpect(jsonPath("$.data.size").value(123));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("upload_PassesTheKindParameterThrough")
    void upload_PassesTheKindParameterThrough() throws Exception {
        when(siteAssetService.store(any(), any())).thenReturn(new SiteAssetVO());

        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()).param("kind", "music"))
                .andExpect(status().isOk());

        verify(siteAssetService).store(any(), eq("music"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("upload_SurfacesValidationFailureAsBadRequest")
    void upload_SurfacesValidationFailureAsBadRequest() throws Exception {
        when(siteAssetService.store(any(), any()))
                .thenThrow(new BusinessException(400, "仅支持 .png、.svg 格式"));

        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()).param("kind", "logo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅支持 .png、.svg 格式"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("upload_RequiresTheKindParameter")
    void upload_RequiresTheKindParameter() throws Exception {
        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("upload_RejectsNonAdminCallers")
    void upload_RejectsNonAdminCallers() throws Exception {
        // 类上的 @PreAuthorize("hasRole('admin')") 在切片里是真的会拦人的（实测 403），
        // 这条用例保证它没被误删
        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()).param("kind", "logo"))
                .andExpect(status().isForbidden());
    }
}
