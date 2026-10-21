package com.selfblog.service.content.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.utils.ImageBombFixtures;
import com.selfblog.utils.ImageDimensionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S5 + S6 回归测试：媒体上传路径。
 *
 * <p>S5：字节上限挡不住声明尺寸炸弹，必须在落盘/解码之前按文件头拦截。
 * S6：内部异常文本不得出现在 HTTP 响应里 —— 这里用会抛 IOException 的
 * {@link MultipartFile} 桩触发失败路径，断言对外文案是固定的通用提示，
 * 且不含底层异常的原始信息。</p>
 *
 * <p>手工构造 {@code MediaServiceImpl}（不启 Spring）：这些用例全部在
 * {@code this.save(media)} 之前就失败，因此不需要数据库；只有 {@code @Value}
 * 字段需要注入。</p>
 */
@DisplayName("MediaServiceImpl（S5 尺寸炸弹 / S6 异常文案）")
class MediaServiceImplUploadTest {

    @TempDir
    Path tempDir;

    private MediaServiceImpl service;
    private File uploadDir;

    @BeforeEach
    void setUp() {
        service = new MediaServiceImpl();
        uploadDir = tempDir.resolve("uploads").toFile();
        ReflectionTestUtils.setField(service, "uploadPath", uploadDir.getAbsolutePath());
        ReflectionTestUtils.setField(service, "urlPrefix", "/uploads");
        ReflectionTestUtils.setField(service, "maxImageWidth", 1920);
        ReflectionTestUtils.setField(service, "imageQuality", 0.85);
    }

    @Test
    @DisplayName("S5：声明 40000x40000 的极小 PNG 被拒绝，且不落盘")
    void declaredSizeBombIsRejectedBeforeDecode() throws Exception {
        byte[] bomb = ImageBombFixtures.pngWithDeclaredSize(40000, 40000);
        MultipartFile file = new StubMultipartFile("bomb.png", "image/png", bomb);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.uploadMedia(file, 1L, "avatar"));

        assertEquals(400, exception.getCode());
        assertEquals(ImageDimensionUtils.TOO_LARGE_MESSAGE, exception.getMessage());
        assertEquals(0, countFiles(), "被拒绝的炸弹不得写入上传目录");
    }

    @Test
    @DisplayName("S5：正常尺寸的真实 PNG 能走完处理流程（不误伤）")
    void normalSizedImageStillProcesses() throws Exception {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(64, 48, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);

        // 该路径会走到 this.save(media)；本测试只关心图片处理本身，因此直接调用
        // processImage 的同款逻辑：这里改用反射调用私有方法，避免依赖数据库。
        File input = tempDir.resolve("input.png").toFile();
        Files.write(input.toPath(), out.toByteArray());
        File output = tempDir.resolve("output.jpg").toFile();

        ReflectionTestUtils.invokeMethod(service, "processImage", input, output);

        assertTrue(output.exists(), "正常尺寸图片应被处理成 jpg");
        assertTrue(output.length() > 0);
    }

    @Test
    @DisplayName("S6：文件头读取失败时不回显底层异常文本")
    void magicNumberFailureDoesNotLeakExceptionText() {
        MultipartFile file = new InputStreamFailingMultipartFile("avatar.png", "image/png");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.uploadMedia(file, 1L, "avatar"));

        assertEquals(400, exception.getCode());
        assertEquals("文件读取失败，无法校验文件类型", exception.getMessage());
        // 关键断言：内部异常信息（含堆栈来源的路径/消息）不得出现在对外文案里。
        assertFalse(exception.getMessage().contains("boom"),
                "不得回显底层异常文本：" + exception.getMessage());
        assertFalse(exception.getMessage().contains(tempDir.toString()),
                "不得回显内部路径：" + exception.getMessage());
    }

    @Test
    @DisplayName("S6：图片处理失败时返回通用文案，不回显底层异常")
    void imageProcessingFailureDoesNotLeakExceptionText() throws Exception {
        byte[] png = ImageBombFixtures.pngWithDeclaredSize(8, 8);
        MultipartFile file = new TransferFailingMultipartFile("avatar.png", "image/png", png);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.uploadMedia(file, 1L, "avatar"));

        assertEquals(500, exception.getCode());
        assertEquals("图片处理失败，请稍后重试", exception.getMessage());
        assertFalse(exception.getMessage().contains("disk-full"),
                "不得回显底层异常文本：" + exception.getMessage());
    }

    @Test
    @DisplayName("S6：文件保存失败时返回通用文案，不回显底层异常")
    void fileSaveFailureDoesNotLeakExceptionText() throws Exception {
        byte[] mp4 = {0, 0, 0, 0, 0x66, 0x74, 0x79, 0x70, 0, 0, 0, 0};
        MultipartFile file = new TransferFailingMultipartFile("clip.mp4", "video/mp4", mp4);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.uploadMedia(file, 1L, "article"));

        assertEquals(500, exception.getCode());
        assertEquals("文件保存失败，请稍后重试", exception.getMessage());
        assertFalse(exception.getMessage().contains("disk-full"),
                "不得回显底层异常文本：" + exception.getMessage());
    }

    private long countFiles() throws IOException {
        if (!uploadDir.exists()) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(uploadDir.toPath())) {
            return stream.filter(Files::isRegularFile).count();
        }
    }

    /** 内存中的正常 MultipartFile。 */
    private static class StubMultipartFile implements MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] content;

        StubMultipartFile(String name, String contentType, byte[] content) {
            this.name = name;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }

    /** getInputStream 抛 IOException：触发 S6 的“文件头读取失败”分支。 */
    private static class InputStreamFailingMultipartFile extends StubMultipartFile {
        InputStreamFailingMultipartFile(String name, String contentType) {
            // 内容必须非空，否则会先撞上 uploadMedia 开头的 isEmpty 校验。
            super(name, contentType, new byte[12]);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException("boom: cannot read header");
        }
    }

    /** transferTo 抛 IOException：触发 S6 的“处理/保存失败”分支。 */
    private static class TransferFailingMultipartFile extends StubMultipartFile {
        TransferFailingMultipartFile(String name, String contentType, byte[] content) {
            super(name, contentType, content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            throw new IOException("disk-full: no space left on device");
        }
    }
}
