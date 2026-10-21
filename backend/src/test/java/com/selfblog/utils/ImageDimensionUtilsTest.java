package com.selfblog.utils;

import com.selfblog.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S5 回归测试：解压缩炸弹必须在完整解码之前被拦下。
 *
 * <p>字节大小上限挡不住“声明尺寸炸弹”：一张几十字节的 PNG 可以在 IHDR 里声明
 * 40000x40000（16 亿像素）。修复前 {@code MediaServiceImpl.processImage} 直接调用
 * {@code ImageIO.read}，解码器会按声明尺寸分配堆内存。现在改为只读文件头拿
 * {@code ImageReader.getWidth/getHeight}，从不调用 {@code read}。</p>
 *
 * <p>“没有解码”这一点是用构造方式证明的：下面这些炸弹文件除了文件头之外没有任何
 * 像素数据。如果代码真的去解码，得到的会是 IIOException（数据提前结束）或 OOM，
 * 而不是我们断言的 400 + 固定文案。</p>
 */
@DisplayName("ImageDimensionUtils")
class ImageDimensionUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("声明 40000x40000 的极小 PNG 在解码前被拒绝")
    void absurdlyLargeDeclaredPngIsRejectedBeforeDecoding() throws Exception {
        byte[] bomb = pngWithDeclaredSize(40000, 40000);
        // 先确认它确实“很小”：只有文件头，没有像素数据。
        assertTrue(bomb.length < 200, "炸弹样本应只有文件头，实际 " + bomb.length + " 字节");

        Path file = tempDir.resolve("bomb.png");
        Files.write(file, bomb);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ImageDimensionUtils.validatePixelCount(file.toFile(), "bomb.png"));

        assertEquals(400, exception.getCode());
        assertEquals(ImageDimensionUtils.TOO_LARGE_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("同样的炸弹从输入流进来也被拒绝（上传分支走的就是这条）")
    void absurdlyLargeDeclaredPngFromStreamIsRejected() throws Exception {
        byte[] bomb = pngWithDeclaredSize(60000, 60000);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ImageDimensionUtils.validatePixelCount(new ByteArrayInputStream(bomb), "bomb.png"));

        assertEquals(400, exception.getCode());
        assertEquals(ImageDimensionUtils.TOO_LARGE_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("恰好等于上限的尺寸放行（边界不误伤）")
    void sizeExactlyAtTheCapIsAllowed() throws Exception {
        // 5000 * 10000 == MAX_IMAGE_PIXELS，判据是 > 上限，因此必须放行。
        long width = 5000;
        long height = ImageDimensionUtils.MAX_IMAGE_PIXELS / width;
        byte[] atCap = pngWithDeclaredSize((int) width, (int) height);

        ImageDimensionUtils.validatePixelCount(new ByteArrayInputStream(atCap), "at-cap.png");
    }

    @Test
    @DisplayName("超过上限一像素即拒绝")
    void sizeOnePixelOverTheCapIsRejected() throws Exception {
        byte[] overCap = pngWithDeclaredSize(5000, 10001);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> ImageDimensionUtils.validatePixelCount(new ByteArrayInputStream(overCap), "over-cap.png"));

        assertEquals(ImageDimensionUtils.TOO_LARGE_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("正常尺寸的真实 PNG 不受影响")
    void normalSizedRealPngPasses() throws Exception {
        Path file = tempDir.resolve("normal.png");
        BufferedImage image = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", file.toFile());

        ImageDimensionUtils.validatePixelCount(file.toFile(), "normal.png");
        ImageDimensionUtils.validatePixelCount(Files.newInputStream(file), "normal.png");
    }

    @Test
    @DisplayName("无法识别的内容直接放行（不改动既有错误语义）")
    void unrecognizedContentIsPassedThrough() throws Exception {
        // 没有任何 reader 认识它：交给调用方原有的魔数/解码分支去报错。
        ImageDimensionUtils.validatePixelCount(new ByteArrayInputStream("not an image at all".getBytes()), "junk.bin");
    }

    // ---------- 构造一个只有文件头的 PNG ----------
    // 具体构造逻辑放在 ImageBombFixtures，媒体/相册路径的测试复用同一份样本。

    private static byte[] pngWithDeclaredSize(int width, int height) throws IOException {
        return ImageBombFixtures.pngWithDeclaredSize(width, height);
    }
}
