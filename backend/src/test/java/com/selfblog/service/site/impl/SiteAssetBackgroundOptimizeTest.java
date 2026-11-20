package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.common.enums.SiteAssetKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 页面背景图的「按需瘦身」。
 *
 * <p>背景图是整页铺满的装饰照片，动辄几百 KB 到数 MB，而屏幕上最多按 2560px 宽显示。
 * 这里钉住三条边界：</p>
 * <ul>
 *   <li>超宽的 JPEG 会被缩到 2560px 宽；</li>
 *   <li>**PNG 一律不动**（可能带透明通道，重编码会让背景变色甚至变黑）；</li>
 *   <li>小图、以及优化失败时保留原文件 —— 优化是锦上添花，不能让一次上传失败。</li>
 * </ul>
 */
class SiteAssetBackgroundOptimizeTest {

    private SiteAssetServiceImpl service(Path uploadRoot) {
        SiteAssetServiceImpl impl = new SiteAssetServiceImpl();
        ReflectionTestUtils.setField(impl, "uploadRootPath", uploadRoot.toString());
        ReflectionTestUtils.setField(impl, "uploadBasePath", "uploads");
        ReflectionTestUtils.setField(impl, "urlPrefix", "/uploads");
        return impl;
    }

    /** 造一张足够宽、且噪点较多（不可压缩）的 JPEG，确保体积超过重编码阈值。 */
    private byte[] wideJpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(30, 90, 160));
        g.fillRect(0, 0, width, height);
        // 加噪点：纯色图会被 JPEG 压得极小，噪点保证体积超过 700KB 阈值。
        java.util.Random random = new java.util.Random(42);
        for (int x = 0; x < width; x += 2) {
            for (int y = 0; y < height; y += 2) {
                g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                g.fillRect(x, y, 2, 2);
            }
        }
        g.dispose();
        var out = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private byte[] solidPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(255, 255, 255, 128));
        g.fillRect(0, 0, width, height);
        g.dispose();
        var out = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void shrinksAnOversizedJpegBackground(@TempDir Path temp) throws IOException {
        SiteAssetServiceImpl impl = service(temp);
        byte[] original = wideJpeg(3000, 1600);
        assertTrue(original.length > 700 * 1024, "测试素材必须超过重编码阈值，实际 " + original.length);

        var stored = impl.store(new MockMultipartFile("file", "hero.jpg", "image/jpeg", original),
                SiteAssetKind.BACKGROUND.getWireName());

        String filename = stored.getUrl().substring(stored.getUrl().lastIndexOf('/') + 1);
        Path saved = temp.resolve("uploads").resolve("site-assets").resolve("background").resolve(filename);
        byte[] after = Files.readAllBytes(saved);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(after));
        assertEquals(2560, decoded.getWidth(), "超宽背景图必须等比缩到 2560px");
        assertTrue(after.length < original.length, "重编码后体积必须变小");
        assertEquals((long) after.length, stored.getSize(), "返回的体积必须是落盘后的真实字节数");
    }

    @Test
    void leavesPngBackgroundsUntouched(@TempDir Path temp) throws IOException {
        SiteAssetServiceImpl impl = service(temp);
        byte[] png = solidPng(200, 200);
        var stored = impl.store(new MockMultipartFile("file", "bg.png", "image/png", png),
                SiteAssetKind.BACKGROUND.getWireName());

        String filename = stored.getUrl().substring(stored.getUrl().lastIndexOf('/') + 1);
        Path saved = temp.resolve("uploads").resolve("site-assets").resolve("background").resolve(filename);
        // PNG 可能带透明通道：必须逐字节保留，不能被重编码。
        assertTrue(java.util.Arrays.equals(png, Files.readAllBytes(saved)), "PNG 背景必须原样落盘");
    }

    @Test
    void keepsSmallJpegsAsIsInsteadOfRecompressing(@TempDir Path temp) throws IOException {
        SiteAssetServiceImpl impl = service(temp);
        byte[] small = wideJpeg(640, 360);
        assertTrue(small.length < 700 * 1024);

        var stored = impl.store(new MockMultipartFile("file", "bg.jpg", "image/jpeg", small),
                SiteAssetKind.BACKGROUND.getWireName());

        String filename = stored.getUrl().substring(stored.getUrl().lastIndexOf('/') + 1);
        Path saved = temp.resolve("uploads").resolve("site-assets").resolve("background").resolve(filename);
        assertTrue(java.util.Arrays.equals(small, Files.readAllBytes(saved)),
                "小体积 JPEG 不应被重编码（换不到体积收益，只会掉画质）");
    }

    @Test
    void neverFailsTheUploadWhenOptimizationCannotDecode(@TempDir Path temp) throws IOException {
        SiteAssetServiceImpl impl = service(temp);
        // 一张只有 JPEG 文件头、正文被截断的文件：能过魔数校验，但解码必然失败。
        byte[] truncated = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x11};
        var stored = impl.store(new MockMultipartFile("file", "broken.jpg", "image/jpeg", truncated),
                SiteAssetKind.BACKGROUND.getWireName());

        String filename = stored.getUrl().substring(stored.getUrl().lastIndexOf('/') + 1);
        Path saved = temp.resolve("uploads").resolve("site-assets").resolve("background").resolve(filename);
        assertTrue(java.util.Arrays.equals(truncated, Files.readAllBytes(saved)),
                "解码失败时必须保留原文件，而不是抛异常中断上传");
        assertEquals(5L, stored.getSize());
    }

    @Test
    void nonBackgroundAssetsAreNeverReencoded(@TempDir Path temp) throws IOException {
        SiteAssetServiceImpl impl = service(temp);
        // 1600×900 噪点 JPEG ≈ 0.8MB：超过重编码阈值，但仍在 logo 的 2MB 上限内。
        byte[] logo = wideJpeg(1600, 900);
        assertTrue(logo.length > 700 * 1024 && logo.length < 2 * 1024 * 1024,
                "测试素材要落在「超过优化阈值但低于 logo 上限」区间，实际 " + logo.length);
        var stored = impl.store(new MockMultipartFile("file", "logo.jpg", "image/jpeg", logo),
                SiteAssetKind.LOGO.getWireName());

        String filename = stored.getUrl().substring(stored.getUrl().lastIndexOf('/') + 1);
        Path saved = temp.resolve("uploads").resolve("site-assets").resolve("logo").resolve(filename);
        assertTrue(java.util.Arrays.equals(logo, Files.readAllBytes(saved)),
                "只有背景图做瘦身；logo / 头像 / 图标必须原样落盘");
    }
}
