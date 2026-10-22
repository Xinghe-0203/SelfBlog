package com.selfblog.service.ai;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.seo.OpenAiCompatibleLlmClient;
import com.selfblog.service.site.SiteSettingsService;
import com.selfblog.vo.ai.AiImageAltVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E 图片描述的集成测试。
 *
 * <p>图片用真实文件（1x1 PNG 的最小合法字节）写到临时目录，再让服务通过
 * {@link AiImageLoader} 读出来——这样 base64、文件头嗅探、4 MiB 上限这些分支都是真跑的，
 * 不是 mock 出来的。</p>
 *
 * <p>视觉支持判定覆盖 Lead 裁定的两条路：清单明确说不支持 → fail-closed 直接 502 且不花上游调用；
 * 清单查不到 → fail-open 继续调，由上游 422 冒成同一句「不支持图像输入」。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AiImageAltService")
class AiImageAltServiceTest {

    private static final long POST_ID = 884001L;
    private static final long PHOTO_ID = 884101L;
    private static final String API_KEY = "sk-ai-image-secret-abcdefgh";

    /** 1x1 透明 PNG 的最小合法字节。 */
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8AAAwAB/AH/4w1cAAAAAElFTkSuQmCC");

    private static final String ONE_ALT = "{\"alt\":\"白色桌面上的笔记本\",\"caption\":\"写作桌面\"}";

    @TempDir
    Path tempDir;

    @Autowired
    private AiAvailability availability;
    @Autowired
    private AiModelCaller caller;
    @Autowired
    private AiJson json;
    @Autowired
    private AiPromptBuilder prompts;
    @Autowired
    private OpenAiCompatibleLlmClient client;
    @Autowired
    private com.selfblog.mapper.BlogPostMapper postMapper;
    @Autowired
    private com.selfblog.mapper.AlbumPhotoMapper albumPhotoMapper;
    @Autowired
    private SiteSettingsService settings;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CacheManager caches;

    private Path galleryDir;
    private AiImageAltService service;
    private AiStubLlmServer server;

    @BeforeEach
    void setUp() throws IOException {
        clearSiteCache();
        galleryDir = Files.createDirectories(tempDir.resolve("gallery-media"));
        service = new AiImageAltService(availability, caller, json, prompts,
                new AiImageLoader(tempDir.toString(), tempDir.toString(), galleryDir.toString()),
                client, postMapper, albumPhotoMapper);

        jdbc.update("DELETE FROM album_photo WHERE id = ?", PHOTO_ID);
        jdbc.update("DELETE FROM blog_post WHERE id = ?", POST_ID);
        jdbc.update("INSERT INTO blog_post (id, user_id, title, summary, content, category, cover_url, status, "
                        + "create_time, update_time, is_deleted) VALUES (?, 1, ?, ?, ?, ?, ?, 1, "
                        + "TIMESTAMP '2026-10-14 10:00:00', TIMESTAMP '2026-10-14 10:00:00', 0)",
                POST_ID, "写作时的桌面", "摘要", "正文", "生活", "/uploads/cover.png");
        Files.write(tempDir.resolve("cover.png"), PNG);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
            server = null;
        }
        clearSiteCache();
    }

    private void clearSiteCache() {
        var cache = caches.getCache(CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private void configureLlm(String baseUrl, String model) {
        settings.updateSettings(Map.of(
                "llmEnabled", "true",
                "llmBaseUrl", baseUrl,
                "llmApiKey", API_KEY,
                "llmModel", model,
                "llmTimeoutSeconds", "10"));
        clearSiteCache();
    }

    @Test
    @DisplayName("cover_HappyPath_SendsInlineDataUrl")
    void cover_HappyPath_SendsInlineDataUrl() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        AiImageAltVO vo = service.describe("cover", POST_ID, null);

        assertEquals("白色桌面上的笔记本", vo.getAlt());
        assertEquals("写作桌面", vo.getCaption());
        assertEquals("deepseek-flash", vo.getModel());
        assertTrue(vo.getLatencyMs() >= 0);
        String body = server.bodies().get(0);
        assertTrue(body.contains("data:image/png;base64,"), "图片要以 data URL 内联进多模态消息");
        assertTrue(body.contains("\"image_url\""), "多模态消息要有 image_url 块");
    }

    @Test
    @DisplayName("album_HappyPath_ReadsAbsoluteGalleryPath")
    void album_HappyPath_ReadsAbsoluteGalleryPath() throws Exception {
        Path photo = galleryDir.resolve("photo.png");
        Files.write(photo, PNG);
        jdbc.update("INSERT INTO album_photo (id, album_id, file_path, mime_type, caption, sort_order, "
                        + "create_time, is_deleted) VALUES (?, 1, ?, 'image/png', ?, 0, "
                        + "TIMESTAMP '2026-10-14 10:00:00', 0)",
                PHOTO_ID, photo.toString(), "旧图注");
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        AiImageAltVO vo = service.describe("album", null, PHOTO_ID);

        assertEquals("白色桌面上的笔记本", vo.getAlt());
        assertTrue(server.bodies().get(0).contains("data:image/png;base64,"),
                "album_photo.file_path 存的是绝对路径，必须按本地文件读出来");
    }

    @Test
    @DisplayName("cover_NoCoverUrl_Throws400")
    void cover_NoCoverUrl_Throws400() throws Exception {
        jdbc.update("UPDATE blog_post SET cover_url = NULL WHERE id = ?", POST_ID);
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(400, error.getCode());
        assertEquals("这篇文章还没有封面图，请先设置封面", error.getMessage());
        assertEquals(0, server.callCount());
    }

    @Test
    @DisplayName("cover_MissingPost_Throws404")
    void cover_MissingPost_Throws404() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", 99999999L, null));

        assertEquals(404, error.getCode());
        assertEquals("文章不存在", error.getMessage());
    }

    @Test
    @DisplayName("album_MissingPhoto_Throws404")
    void album_MissingPhoto_Throws404() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("album", null, 99999999L));

        assertEquals(404, error.getCode());
        assertEquals("照片不存在", error.getMessage());
    }

    @Test
    @DisplayName("cover_FileMissing_Throws400")
    void cover_FileMissing_Throws400() throws Exception {
        Files.delete(tempDir.resolve("cover.png"));
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(400, error.getCode());
        assertEquals(AiImageLoader.MISSING_FILE_MESSAGE, error.getMessage());
    }

    @Test
    @DisplayName("cover_ImageTooLarge_Throws400")
    void cover_ImageTooLarge_Throws400() throws Exception {
        byte[] huge = new byte[(int) AiImageLoader.MAX_BYTES + 1024];
        System.arraycopy(PNG, 0, huge, 0, PNG.length);
        Files.write(tempDir.resolve("cover.png"), huge);
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(400, error.getCode());
        assertEquals("图片过大，请先压缩后再试", error.getMessage());
    }

    @Test
    @DisplayName("cover_UnsupportedType_Throws400")
    void cover_UnsupportedType_Throws400() throws Exception {
        Files.write(tempDir.resolve("cover.png"), "这不是图片".getBytes());
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(400, error.getCode());
        assertEquals(AiImageLoader.UNSUPPORTED_TYPE_MESSAGE, error.getMessage(),
                "扩展名是 .png 但文件头不对，必须按文件头拒绝");
    }

    @Test
    @DisplayName("cover_PathOutsideUploadDir_Throws400")
    void cover_PathOutsideUploadDir_Throws400() throws Exception {
        Path outside = tempDir.getParent().resolve("outside-secret.png");
        Files.write(outside, PNG);
        jdbc.update("UPDATE blog_post SET cover_url = ? WHERE id = ?", outside.toString(), POST_ID);
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(400, error.getCode());
        assertEquals(AiImageLoader.UNSUPPORTED_PATH_MESSAGE, error.getMessage(),
                "站内路径必须落在上传目录或相册目录内");
    }

    @Test
    @DisplayName("modelWithoutVision_FailClosed_502WithoutUpstreamCall")
    void modelWithoutVision_FailClosed_502WithoutUpstreamCall() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        server.serveModels("{\"data\":[{\"id\":\"deepseek-v4-pro\",\"input_modalities\":[\"text\"]}]}");
        configureLlm(server.baseUrl(), "deepseek-v4-pro");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(502, error.getCode());
        assertEquals(AiImageAltService.NOT_SUPPORTED_MESSAGE, error.getMessage());
        assertEquals(0, server.callCount(), "清单已明确说不支持视觉，不该再花一次上游调用");
        assertEquals(1, server.modelCallCount());
    }

    @Test
    @DisplayName("modelWithVision_Proceeds")
    void modelWithVision_Proceeds() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        server.serveModels("{\"data\":[{\"id\":\"deepseek-flash\","
                + "\"input_modalities\":[\"text\",\"image\"]}]}");
        configureLlm(server.baseUrl(), "deepseek-flash");

        AiImageAltVO vo = service.describe("cover", POST_ID, null);

        assertEquals("白色桌面上的笔记本", vo.getAlt());
        assertEquals(1, server.callCount());
    }

    @Test
    @DisplayName("modelsUnavailable_FailOpen_StillCallsVision")
    void modelsUnavailable_FailOpen_StillCallsVision() throws Exception {
        // 桩默认 /v1/models 返回 404，模拟自建网关没实现这个端点
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        AiImageAltVO vo = service.describe("cover", POST_ID, null);

        assertEquals("白色桌面上的笔记本", vo.getAlt());
        assertEquals(1, server.modelCallCount(), "先试过模型清单");
        assertEquals(1, server.callCount(), "清单失败时按「未知支持」继续尝试视觉调用");
    }

    @Test
    @DisplayName("upstreamRejectsParams_TranslatedToNotSupportedMessage")
    void upstreamRejectsParams_TranslatedToNotSupportedMessage() throws Exception {
        // 模型清单不可用（fail-open），上游用 422 拒绝 → 必须翻译成契约 §6 的文案
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.status(422, "{\"error\":\"image input not supported\"}"));
        configureLlm(server.baseUrl(), "deepseek-v4-pro");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(502, error.getCode());
        assertEquals(AiImageAltService.NOT_SUPPORTED_MESSAGE, error.getMessage());
    }

    @Test
    @DisplayName("upstreamServerError_KeepsOriginalMessage")
    void upstreamServerError_KeepsOriginalMessage() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.status(500, "{\"error\":\"boom\"}"));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(502, error.getCode());
        assertEquals("模型服务暂时不可用，请稍后重试", error.getMessage(),
                "服务端故障不该被误报成「模型不支持图像」");
    }

    @Test
    @DisplayName("altAndCaption_TruncatedToContractLimits")
    void altAndCaption_TruncatedToContractLimits() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(
                "{\"alt\":\"" + "描".repeat(300) + "\",\"caption\":\"" + "注".repeat(200) + "\"}"));
        configureLlm(server.baseUrl(), "deepseek-flash");

        AiImageAltVO vo = service.describe("cover", POST_ID, null);

        assertEquals(AiImageAltService.ALT_MAX, vo.getAlt().length());
        assertEquals(AiImageAltService.CAPTION_MAX, vo.getCaption().length());
    }

    @Test
    @DisplayName("blankCaption_BecomesNull")
    void blankCaption_BecomesNull() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"alt\":\"一张桌面\",\"caption\":\"  \"}"));
        configureLlm(server.baseUrl(), "deepseek-flash");

        AiImageAltVO vo = service.describe("cover", POST_ID, null);

        assertEquals("一张桌面", vo.getAlt());
        assertEquals(null, vo.getCaption(), "空图注返回 null，前端才好判断是否展示");
    }

    @Test
    @DisplayName("missingAlt_RepairsThenFails502")
    void missingAlt_RepairsThenFails502() throws Exception {
        server = new AiStubLlmServer((index, body) ->
                AiStubLlmServer.Reply.ok("{\"caption\":\"只有图注\"}"));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(502, error.getCode());
        assertEquals("模型返回的内容不符合要求，请重试或手动填写", error.getMessage());
        assertEquals(2, server.callCount(), "修复重试也要带原图");
        assertTrue(server.bodies().get(1).contains("data:image/png;base64,"));
    }

    @Test
    @DisplayName("invalidSource_Throws400")
    void invalidSource_Throws400() throws Exception {
        server = new AiStubLlmServer((index, body) -> AiStubLlmServer.Reply.ok(ONE_ALT));
        configureLlm(server.baseUrl(), "deepseek-flash");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("avatar", POST_ID, null));

        assertEquals(400, error.getCode());
        assertEquals(AiImageAltService.SOURCE_INVALID_MESSAGE, error.getMessage());
    }

    @Test
    @DisplayName("aiDisabled_Throws400BeforeReadingFile")
    void aiDisabled_Throws400BeforeReadingFile() throws Exception {
        settings.updateSettings(Map.of("llmEnabled", "false"));
        clearSiteCache();

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.describe("cover", POST_ID, null));

        assertEquals(400, error.getCode());
        assertEquals("AI 辅助功能已关闭，请先在设置中启用", error.getMessage());
    }

    @Test
    @DisplayName("sniff_RecognizesAllAllowedFormats")
    void sniff_RecognizesAllAllowedFormats() {
        assertEquals("image/png", AiImageLoader.sniff(PNG));
        assertEquals("image/jpeg", AiImageLoader.sniff(
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}));
        assertEquals("image/gif", AiImageLoader.sniff(
                new byte[]{'G', 'I', 'F', '8', '9', 'a'}));
        byte[] webp = new byte[16];
        webp[0] = 'R';
        webp[1] = 'I';
        webp[2] = 'F';
        webp[3] = 'F';
        webp[8] = 'W';
        webp[9] = 'E';
        webp[10] = 'B';
        webp[11] = 'P';
        assertEquals("image/webp", AiImageLoader.sniff(webp));
        assertEquals("application/octet-stream", AiImageLoader.sniff(new byte[]{1, 2, 3, 4}));
        assertFalse("image/png".equals(AiImageLoader.sniff(new byte[]{1, 2, 3, 4})));
    }
}
