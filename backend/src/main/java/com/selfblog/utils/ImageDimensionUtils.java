package com.selfblog.utils;

import com.selfblog.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * 图片头尺寸校验工具（S5）。
 *
 * <p>字节大小上限挡不住“解压缩炸弹”：一张几十 KB 的 PNG 可以在 IHDR 里声明 40000x40000
 * （16 亿像素），{@code ImageIO.read} 解码时会按声明尺寸分配堆内存，直接把进程拖垮。
 * 因此必须在<b>完整解码之前</b>只读文件头拿到声明尺寸并拦截。</p>
 *
 * <p>这里只使用 {@link ImageIO#getImageReaders} + {@link ImageReader#getWidth}/
 * {@link ImageReader#getHeight}，从不调用 {@code ImageReader.read}，所以恶意文件声明的
 * 巨大尺寸不会被真正分配。若找不到任何 reader（例如内置 ImageIO 不支持 webp），
 * 直接放行，交由调用方原有的解码/魔数分支处理，不改变既有错误语义。</p>
 */
public final class ImageDimensionUtils {

    private static final Logger log = LoggerFactory.getLogger(ImageDimensionUtils.class);

    /**
     * 单张图片允许的最大像素数：5000 万像素。
     *
     * <p>足以覆盖常见相机原图（约 8000x6000），同时远低于危险量级。</p>
     */
    public static final long MAX_IMAGE_PIXELS = 50_000_000L;

    /** 像素超限时对外统一返回的提示（不泄露内部细节）。 */
    public static final String TOO_LARGE_MESSAGE = "图片尺寸过大，请上传分辨率不超过 5000 万像素的图片";

    private ImageDimensionUtils() {
    }

    /**
     * 校验文件头中声明的像素数是否超过 {@link #MAX_IMAGE_PIXELS}。
     *
     * @param file       待校验的图片文件
     * @param sourceName 日志用的来源标识（可为 null）
     * @throws BusinessException 声明像素数超限时抛出 400
     */
    public static void validatePixelCount(File file, String sourceName) throws IOException {
        try (ImageInputStream imageStream = ImageIO.createImageInputStream(file)) {
            check(imageStream, sourceName);
        }
    }

    /**
     * 校验输入流中声明的像素数是否超过 {@link #MAX_IMAGE_PIXELS}。
     *
     * <p>本方法会消费并关闭传入的流。</p>
     *
     * @param input      图片输入流
     * @param sourceName 日志用的来源标识（可为 null）
     * @throws BusinessException 声明像素数超限时抛出 400
     */
    public static void validatePixelCount(InputStream input, String sourceName) throws IOException {
        try (ImageInputStream imageStream = ImageIO.createImageInputStream(input)) {
            check(imageStream, sourceName);
        }
    }

    private static void check(ImageInputStream imageStream, String sourceName) throws IOException {
        if (imageStream == null) {
            return;
        }
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
        if (!readers.hasNext()) {
            return;
        }
        ImageReader reader = readers.next();
        try {
            reader.setInput(imageStream, true, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            long pixels = (long) width * (long) height;
            if (pixels > MAX_IMAGE_PIXELS) {
                log.warn("[Image] 图片像素超限: source={}, 声明尺寸={}x{} ({} 像素), 上限={} 像素",
                        sourceName, width, height, pixels, MAX_IMAGE_PIXELS);
                throw new BusinessException(400, TOO_LARGE_MESSAGE);
            }
        } finally {
            reader.dispose();
        }
    }
}
