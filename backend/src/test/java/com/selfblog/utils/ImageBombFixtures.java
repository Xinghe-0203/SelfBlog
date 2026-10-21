package com.selfblog.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 测试用的“解压缩炸弹”构造器（S5）。
 *
 * <p>只写 PNG 文件头：8 字节签名 + IHDR（声明任意尺寸）+ IEND，<b>不含任何像素数据</b>。
 * 因此字节数只有几十字节，但解码器会按声明尺寸分配堆内存。</p>
 *
 * <p>这个“只有文件头”的特性本身也是断言的一部分：如果被测代码真的调用了
 * {@code ImageReader.read}，得到的是数据提前结束的 IIOException（或 OOM），
 * 而不是“像素超限”的 400 —— 于是测试失败。</p>
 */
public final class ImageBombFixtures {

    private ImageBombFixtures() {
    }

    /** 构造声明尺寸为 {@code width x height} 的极小 PNG（无像素数据）。 */
    public static byte[] pngWithDeclaredSize(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});

        byte[] ihdr = new byte[13];
        writeInt(ihdr, 0, width);
        writeInt(ihdr, 4, height);
        ihdr[8] = 8;  // bit depth
        ihdr[9] = 2;  // color type: truecolor
        ihdr[10] = 0; // compression method
        ihdr[11] = 0; // filter method
        ihdr[12] = 0; // interlace method
        writeChunk(out, "IHDR", ihdr);
        writeChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        byte[] length = new byte[4];
        writeInt(length, 0, data.length);
        out.write(length);
        out.write(type.getBytes(StandardCharsets.US_ASCII));
        out.write(data);
        out.write(new byte[4]); // CRC：ImageIO 的 PNG 读取器不校验，写零即可
    }

    private static void writeInt(byte[] target, int offset, int value) {
        target[offset] = (byte) ((value >>> 24) & 0xFF);
        target[offset + 1] = (byte) ((value >>> 16) & 0xFF);
        target[offset + 2] = (byte) ((value >>> 8) & 0xFF);
        target[offset + 3] = (byte) (value & 0xFF);
    }
}
