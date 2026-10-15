package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.vo.site.SiteAssetVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SiteAssetServiceImpl 单元测试。
 *
 * 不用 @SpringBootTest：被测对象是"校验 + 落盘"，@TempDir 就能完全控制，
 * 既不必依赖 H2 与上传目录配置，也不会往真实 uploads 目录里写东西。
 * 字段是 @Value 注入的，因此用 ReflectionTestUtils 直接塞值。
 */
@DisplayName("SiteAssetServiceImpl")
class SiteAssetServiceImplTest {

    private static final byte[] PNG_BYTES =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    /** 只含 8 字节文件头，用来拼大文件时省内存。 */
    private static final byte[] PNG_MAGIC_ONLY =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] ICO_BYTES = {0, 0, 1, 0, 1, 0, 16, 16, 0, 0};
    private static final byte[] MP3_BYTES = {'I', 'D', '3', 3, 0, 0, 0, 0};
    private static final byte[] OGG_BYTES = {'O', 'g', 'g', 'S', 0, 2, 0, 0};
    private static final byte[] WAV_BYTES = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};
    private static final byte[] M4A_BYTES = {0, 0, 0, 24, 'f', 't', 'y', 'p', 'M', '4', 'A', ' '};
    private static final byte[] WEBP_BYTES =
            {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    private static final String PLAIN_SVG =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 8 8\">"
                    + "<rect width=\"8\" height=\"8\"/></svg>";

    @TempDir
    Path tempDir;

    private SiteAssetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SiteAssetServiceImpl();
        ReflectionTestUtils.setField(service, "uploadBasePath", "uploads");
        ReflectionTestUtils.setField(service, "uploadRootPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "urlPrefix", "/uploads");
    }

    private MockMultipartFile file(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content);
    }

    private byte[] svg(String body) {
        return body.getBytes(StandardCharsets.UTF_8);
    }

    /** 从返回的 URL 反推真实落盘路径。 */
    private Path pathOf(SiteAssetVO vo) {
        String relative = vo.getUrl().substring("/uploads/".length());
        return tempDir.resolve("uploads").resolve(relative);
    }

    // ---------- 正常路径 ----------

    @Test
    @DisplayName("store_WritesFileAndReturnsPublicUrl")
    void store_WritesFileAndReturnsPublicUrl() throws Exception {
        SiteAssetVO vo = service.store(file("logo.png", PNG_BYTES), "logo");

        assertNotNull(vo);
        assertEquals("logo", vo.getKind());
        assertEquals("logo.png", vo.getOriginalName());
        assertEquals((long) PNG_BYTES.length, vo.getSize());
        assertTrue(vo.getUrl().startsWith("/uploads/site-assets/logo/"));
        assertTrue(vo.getUrl().endsWith(".png"));
        // 文件名必须是随机 UUID，不能沿用上传时的名字
        assertFalse(vo.getUrl().contains("logo.png"));

        Path written = pathOf(vo);
        assertTrue(Files.exists(written));
        assertEquals(PNG_BYTES.length, Files.size(written));
    }

    @Test
    @DisplayName("store_UsesKindSpecificDirectory")
    void store_UsesKindSpecificDirectory() {
        SiteAssetVO vo = service.store(file("a.png", PNG_BYTES), "favicon");
        assertTrue(vo.getUrl().startsWith("/uploads/site-assets/favicon/"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"logo", "favicon", "avatar"})
    @DisplayName("store_AcceptsSvgForEveryImageKind")
    void store_AcceptsSvgForEveryImageKind(String kind) {
        SiteAssetVO vo = service.store(file("brand.svg", svg(PLAIN_SVG)), kind);
        assertTrue(vo.getUrl().endsWith(".svg"));
    }

    @Test
    @DisplayName("store_AcceptsIcoForFavicon")
    void store_AcceptsIcoForFavicon() {
        SiteAssetVO vo = service.store(file("favicon.ico", ICO_BYTES), "favicon");
        assertTrue(vo.getUrl().endsWith(".ico"));
    }

    @Test
    @DisplayName("store_AcceptsPngBytesNamedIco")
    void store_AcceptsPngBytesNamedIco() {
        // 不少工具导出的 favicon.ico 其实是 PNG，两条路都要放行
        SiteAssetVO vo = service.store(file("favicon.ico", PNG_BYTES), "favicon");
        assertTrue(vo.getUrl().endsWith(".ico"));
    }

    @ParameterizedTest
    @ValueSource(strings = {".webp", ".mp3", ".ogg", ".wav", ".m4a"})
    @DisplayName("store_AcceptsEveryDeclaredAudioAndRasterFormat")
    void store_AcceptsEveryDeclaredAudioAndRasterFormat(String extension) {
        byte[] content = switch (extension) {
            case ".webp" -> WEBP_BYTES;
            case ".mp3" -> MP3_BYTES;
            case ".ogg" -> OGG_BYTES;
            case ".wav" -> WAV_BYTES;
            default -> M4A_BYTES;
        };
        String kind = ".webp".equals(extension) ? "logo" : "music";
        SiteAssetVO vo = service.store(file("asset" + extension, content), kind);
        assertTrue(vo.getUrl().endsWith(extension));
    }

    @Test
    @DisplayName("store_AcceptsAacFrameSyncAsMusic")
    void store_AcceptsAacFrameSyncAsMusic() {
        byte[] aac = {(byte) 0xFF, (byte) 0xF1, 0x50, (byte) 0x80, 0, 0, 0, 0};
        assertTrue(service.store(file("a.aac", aac), "music").getUrl().endsWith(".aac"));
    }

    // ---------- 种类与参数校验 ----------

    @Test
    @DisplayName("store_RejectsUnknownKind")
    void store_RejectsUnknownKind() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.store(file("logo.png", PNG_BYTES), "cover"));
        assertEquals(400, e.getCode());
    }

    @Test
    @DisplayName("store_RejectsNullKind")
    void store_RejectsNullKind() {
        assertThrows(BusinessException.class, () -> service.store(file("logo.png", PNG_BYTES), null));
    }

    @Test
    @DisplayName("store_RejectsEmptyFile")
    void store_RejectsEmptyFile() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.store(file("logo.png", new byte[0]), "logo"));
        assertEquals(400, e.getCode());
    }

    @Test
    @DisplayName("store_RejectsNullFile")
    void store_RejectsNullFile() {
        assertThrows(BusinessException.class, () -> service.store(null, "logo"));
    }

    @Test
    @DisplayName("store_RejectsExtensionOutsideTheKindWhitelist")
    void store_RejectsExtensionOutsideTheKindWhitelist() {
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a'};
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.store(file("logo.gif", gif), "logo"));
        assertEquals(400, e.getCode());
        assertTrue(e.getMessage().contains(".png"));
    }

    @Test
    @DisplayName("store_RejectsIcoForLogo")
    void store_RejectsIcoForLogo() {
        assertThrows(BusinessException.class, () -> service.store(file("logo.ico", ICO_BYTES), "logo"));
    }

    @Test
    @DisplayName("store_RejectsImageExtensionForMusic")
    void store_RejectsImageExtensionForMusic() {
        assertThrows(BusinessException.class, () -> service.store(file("song.png", PNG_BYTES), "music"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"logo", "brand", "no-extension"})
    @DisplayName("store_RejectsFilenamesWithoutUsableExtension")
    void store_RejectsFilenamesWithoutUsableExtension(String filename) {
        assertThrows(BusinessException.class, () -> service.store(file(filename, PNG_BYTES), "logo"));
    }

    // ---------- 魔数嗅探 ----------

    @Test
    @DisplayName("store_RejectsJpegBytesNamedPng")
    void store_RejectsJpegBytesNamedPng() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.store(file("logo.png", JPEG_BYTES), "logo"));
        assertEquals(400, e.getCode());
        assertTrue(e.getMessage().contains("内容与扩展名"));
    }

    @Test
    @DisplayName("store_RejectsHtmlDisguisedAsPng")
    void store_RejectsHtmlDisguisedAsPng() {
        byte[] html = "<html><body>hi</body></html>".getBytes(StandardCharsets.UTF_8);
        assertThrows(BusinessException.class, () -> service.store(file("logo.png", html), "logo"));
    }

    @Test
    @DisplayName("store_RejectsHtmlDisguisedAsSvg")
    void store_RejectsHtmlDisguisedAsSvg() {
        byte[] html = "<html><body>hi</body></html>".getBytes(StandardCharsets.UTF_8);
        assertThrows(BusinessException.class, () -> service.store(file("logo.svg", html), "logo"));
    }

    @Test
    @DisplayName("store_RejectsSvgBytesNamedPng")
    void store_RejectsSvgBytesNamedPng() {
        assertThrows(BusinessException.class, () -> service.store(file("logo.png", svg(PLAIN_SVG)), "logo"));
    }

    @Test
    @DisplayName("store_RejectsGarbageNamedSvg")
    void store_RejectsGarbageNamedSvg() {
        // Jsoup 的 XML 解析器对垃圾输入不抛异常，只能靠"没有 svg 根元素"来拒绝
        byte[] garbage = "not xml at all <<<>>>".getBytes(StandardCharsets.UTF_8);
        assertThrows(BusinessException.class, () -> service.store(file("logo.svg", garbage), "logo"));
    }

    @Test
    @DisplayName("store_RejectsPngBytesNamedMp3")
    void store_RejectsPngBytesNamedMp3() {
        assertThrows(BusinessException.class, () -> service.store(file("song.mp3", PNG_BYTES), "music"));
    }

    @Test
    @DisplayName("store_RejectsRiffThatIsNeitherWebpNorWav")
    void store_RejectsRiffThatIsNeitherWebpNorWav() {
        byte[] riffAvi = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'A', 'V', 'I', ' '};
        assertThrows(BusinessException.class, () -> service.store(file("song.wav", riffAvi), "music"));
    }

    // ---------- 体积上限 ----------

    @Test
    @DisplayName("store_RejectsImageOverTwoMegabytes")
    void store_RejectsImageOverTwoMegabytes() {
        byte[] oversized = new byte[3 * 1024 * 1024];
        System.arraycopy(PNG_MAGIC_ONLY, 0, oversized, 0, PNG_MAGIC_ONLY.length);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.store(file("logo.png", oversized), "logo"));
        assertEquals(400, e.getCode());
        assertTrue(e.getMessage().contains("2MB"));
    }

    @Test
    @DisplayName("store_AllowsMusicWellPastTheImageCap")
    void store_AllowsMusicWellPastTheImageCap() {
        // 3MB 超过图片上限但远低于音乐上限，证明阈值是按种类分档的
        byte[] bigMusic = new byte[3 * 1024 * 1024];
        System.arraycopy(MP3_BYTES, 0, bigMusic, 0, MP3_BYTES.length);

        SiteAssetVO vo = service.store(file("song.mp3", bigMusic), "music");
        assertEquals((long) bigMusic.length, vo.getSize());
    }

    @Test
    @DisplayName("store_RejectsMusicOverTwentyMegabytes")
    void store_RejectsMusicOverTwentyMegabytes() {
        byte[] oversized = new byte[21 * 1024 * 1024];
        System.arraycopy(MP3_BYTES, 0, oversized, 0, MP3_BYTES.length);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.store(file("song.mp3", oversized), "music"));
        assertTrue(e.getMessage().contains("20MB"));
    }

    // ---------- SVG 净化 ----------

    @Test
    @DisplayName("store_StripsScriptFromSvg")
    void store_StripsScriptFromSvg() throws Exception {
        byte[] hostile = svg("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<script>alert('xss')</script><rect width=\"4\" height=\"4\"/></svg>");

        SiteAssetVO vo = service.store(file("brand.svg", hostile), "logo");
        String written = Files.readString(pathOf(vo), StandardCharsets.UTF_8);

        assertFalse(written.toLowerCase().contains("<script"));
        assertTrue(written.contains("rect"));
    }

    @Test
    @DisplayName("store_StripsNamespacePrefixedScriptFromSvg")
    void store_StripsNamespacePrefixedScriptFromSvg() throws Exception {
        byte[] hostile = svg("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" "
                + "xmlns:x=\"http://www.w3.org/2000/svg\"><x:script>alert('xss')</x:script>"
                + "<rect width=\"4\" height=\"4\"/></svg>");

        SiteAssetVO vo = service.store(file("brand.svg", hostile), "logo");
        String written = Files.readString(pathOf(vo), StandardCharsets.UTF_8).toLowerCase();

        assertFalse(written.contains("x:script"));
        assertTrue(written.contains("rect"));
    }

    @Test
    @DisplayName("store_StripsEventHandlersFromSvg")
    void store_StripsEventHandlersFromSvg() throws Exception {
        byte[] hostile = svg("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<rect width=\"4\" height=\"4\" onload=\"alert(1)\" onclick=\"alert(2)\"/></svg>");

        SiteAssetVO vo = service.store(file("brand.svg", hostile), "logo");
        String written = Files.readString(pathOf(vo), StandardCharsets.UTF_8).toLowerCase();

        assertFalse(written.contains("onload"));
        assertFalse(written.contains("onclick"));
        assertTrue(written.contains("rect"));
    }

    @Test
    @DisplayName("store_StripsJavascriptUrlsFromSvg")
    void store_StripsJavascriptUrlsFromSvg() throws Exception {
        byte[] hostile = svg("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<a href=\"javascript:alert(1)\"><rect width=\"4\" height=\"4\"/></a></svg>");

        SiteAssetVO vo = service.store(file("brand.svg", hostile), "logo");
        String written = Files.readString(pathOf(vo), StandardCharsets.UTF_8).toLowerCase();

        assertFalse(written.contains("javascript:"));
    }

    @Test
    @DisplayName("store_StripsForeignObjectFromSvg")
    void store_StripsForeignObjectFromSvg() throws Exception {
        byte[] hostile = svg("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<foreignObject width=\"10\" height=\"10\"><body>hi</body></foreignObject>"
                + "<rect width=\"4\" height=\"4\"/></svg>");

        SiteAssetVO vo = service.store(file("brand.svg", hostile), "logo");
        String written = Files.readString(pathOf(vo), StandardCharsets.UTF_8).toLowerCase();

        assertFalse(written.contains("foreignobject"));
        assertTrue(written.contains("rect"));
    }

    @Test
    @DisplayName("store_KeepsMixedCaseSvgTagsAndViewBox")
    void store_KeepsMixedCaseSvgTagsAndViewBox() throws Exception {
        // 净化必须走 XML 解析器；用 HTML 解析器会把 linearGradient 小写成 lineargradient，图形就废了
        byte[] source = svg("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 8 8\">"
                + "<defs><linearGradient id=\"g\"><stop offset=\"0%\"/></linearGradient></defs>"
                + "<rect width=\"4\" height=\"4\" fill=\"url(#g)\"/></svg>");

        SiteAssetVO vo = service.store(file("brand.svg", source), "logo");
        String written = Files.readString(pathOf(vo), StandardCharsets.UTF_8);

        assertTrue(written.contains("linearGradient"));
        assertFalse(written.contains("lineargradient"));
        assertTrue(written.contains("viewBox"));
    }

    @Test
    @DisplayName("store_KeepsPlainSvgIntact")
    void store_KeepsPlainSvgIntact() throws Exception {
        SiteAssetVO vo = service.store(file("brand.svg", svg(PLAIN_SVG)), "logo");
        String written = Files.readString(pathOf(vo), StandardCharsets.UTF_8);

        assertTrue(written.contains("<svg"));
        assertTrue(written.contains("rect"));
        assertEquals(written.getBytes(StandardCharsets.UTF_8).length, vo.getSize());
    }
}
