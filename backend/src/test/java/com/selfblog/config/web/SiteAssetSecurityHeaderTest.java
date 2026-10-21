package com.selfblog.config.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S1 响应头回归测试：站点资产路径必须带上 CSP 与 nosniff，且只作用于该路径。
 *
 * <p>净化（白名单）是上传侧的第一层；这里验证的是第二层 —— 即使某个载荷漏过净化，
 * 浏览器把 {@code /uploads/site-assets/**} 当顶层文档渲染时也不能加载外部资源、
 * 不能按内容嗅探成 HTML，并且因为 {@code sandbox} 运行在不透明源上，读不到
 * localStorage 里的 JWT。</p>
 *
 * <p>同时验证作用域：普通上传资源（{@code /uploads/**} 下的其他目录）不应带上 CSP，
 * 因为这条策略会破坏正常图片/音频的加载语义。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SiteAssetSecurityHeaderInterceptor")
class SiteAssetSecurityHeaderTest {

    /** 每个测试类一个独立上传根目录，避免写进真实 uploads 或 /tmp/test-uploads。 */
    private static final Path UPLOAD_ROOT;

    static {
        try {
            UPLOAD_ROOT = Files.createTempDirectory("site-asset-headers-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void uploadProperties(DynamicPropertyRegistry registry) {
        registry.add("upload.root-path", () -> UPLOAD_ROOT.toString());
        registry.add("upload.base-path", () -> "uploads");
    }

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void seedFiles() throws IOException {
        writeUpload("site-assets/logo/probe.svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>");
        writeUpload("site-assets/favicon/probe.svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"/>");
        writeUpload("media/probe.png", "not-really-a-png");
    }

    private void writeUpload(String relative, String content) throws IOException {
        Path target = UPLOAD_ROOT.resolve("uploads").resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("站点资产响应带 CSP 与 nosniff")
    void siteAssetsCarryCspAndNosniff() throws Exception {
        mvc.perform(get("/uploads/site-assets/logo/probe.svg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; style-src 'unsafe-inline'; sandbox"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("CSP 的 sandbox 指令必须存在（拿不到同源存储）")
    void siteAssetCspIncludesSandbox() throws Exception {
        String csp = mvc.perform(get("/uploads/site-assets/favicon/probe.svg"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("Content-Security-Policy");

        org.junit.jupiter.api.Assertions.assertNotNull(csp, "站点资产响应必须带 CSP");
        org.junit.jupiter.api.Assertions.assertTrue(csp.contains("sandbox"),
                "CSP 必须含 sandbox，否则 SVG 仍可能以本站源执行：" + csp);
        org.junit.jupiter.api.Assertions.assertTrue(csp.contains("default-src 'none'"),
                "CSP 必须默认拒绝一切外部加载：" + csp);
    }

    @Test
    @DisplayName("普通上传资源不带 CSP（作用域仅限 site-assets）")
    void otherUploadsDoNotCarryCsp() throws Exception {
        mvc.perform(get("/uploads/media/probe.png"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }
}
