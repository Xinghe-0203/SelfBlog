package com.selfblog.service.ai;

import com.selfblog.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

/**
 * 把图片地址读成可以直接塞进多模态消息的 {@code data:} URL。
 *
 * <p>三种来源统一在这里收口：</p>
 * <ul>
 *   <li>站内上传路径 {@code /uploads/xxx} → 解析到 {@code upload.base-path} 目录下；</li>
 *   <li>http(s) 绝对地址 → 服务端下载；</li>
 *   <li>相册照片的绝对文件路径 → {@code album_photo.file_path} 存的就是 {@code gallery-media}
 *       下的绝对路径，不是对外 URL，所以按本地文件处理，但必须落在上传目录或相册目录内。</li>
 * </ul>
 *
 * <p>只放行 JPEG/PNG/GIF/WebP 四种格式，按文件头判定（扩展名可以骗人）。
 * 上限 4 MiB —— 更大的图 base64 之后会撑爆模型请求体，让管理员先压缩。</p>
 */
@Slf4j
@Component
public class AiImageLoader {

    /** 契约 §6：单张图片上限 4 MiB。 */
    static final long MAX_BYTES = 4L * 1024 * 1024;

    static final String TOO_LARGE_MESSAGE = "图片过大，请先压缩后再试";
    static final String UNSUPPORTED_PATH_MESSAGE = "图片路径不受支持，只支持站内上传目录或 http(s) 地址";
    static final String UNSUPPORTED_TYPE_MESSAGE = "只支持 JPEG、PNG、GIF、WebP 格式的图片";
    static final String MISSING_FILE_MESSAGE = "图片文件不存在或无法读取";

    private static final String UPLOAD_URL_PREFIX = "/uploads";
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(10);
    private static final Set<String> ALLOWED_MIME =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final Path uploadDir;
    private final Path galleryDir;
    private final HttpClient http;

    public AiImageLoader(@Value("${upload.base-path:./uploads}") String uploadBasePath,
                         @Value("${upload.root-path:#{systemProperties['user.dir']}}") String uploadRootPath,
                         @Value("${upload.gallery-path:gallery-media}") String galleryPath) {
        this.uploadDir = resolveDir(uploadBasePath, uploadRootPath);
        this.galleryDir = resolveDir(galleryPath, uploadRootPath);
        this.http = HttpClient.newBuilder()
                .connectTimeout(FETCH_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** 读成 {@code data:{mime};base64,...}；任何不可用都转成 400 让管理员自己改。 */
    public String toDataUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, UNSUPPORTED_PATH_MESSAGE);
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return fetchRemote(trimmed);
        }
        if (lower.startsWith(UPLOAD_URL_PREFIX)) {
            return fromUploadUrl(trimmed);
        }
        return fromLocalPath(trimmed);
    }

    private String fromUploadUrl(String value) {
        String relative = value.substring(UPLOAD_URL_PREFIX.length());
        int query = relative.indexOf('?');
        if (query >= 0) {
            relative = relative.substring(0, query);
        }
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        Path target = uploadDir.resolve(relative).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new BusinessException(400, UNSUPPORTED_PATH_MESSAGE);
        }
        return encode(readFile(target));
    }

    private String fromLocalPath(String value) {
        Path raw = Paths.get(value);
        Path target = (raw.isAbsolute() ? raw : uploadDir.getFileSystem().getPath(
                System.getProperty("user.dir")).resolve(raw)).normalize().toAbsolutePath();
        if (!target.startsWith(uploadDir) && !target.startsWith(galleryDir)) {
            throw new BusinessException(400, UNSUPPORTED_PATH_MESSAGE);
        }
        return encode(readFile(target));
    }

    private byte[] readFile(Path file) {
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new BusinessException(400, MISSING_FILE_MESSAGE);
        }
        try {
            long size = Files.size(file);
            if (size > MAX_BYTES) {
                throw new BusinessException(400, TOO_LARGE_MESSAGE);
            }
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length > MAX_BYTES) {
                throw new BusinessException(400, TOO_LARGE_MESSAGE);
            }
            requireAllowedType(sniff(bytes));
            return bytes;
        } catch (IOException e) {
            log.warn("[AI] 读取图片失败：{}", e.getClass().getSimpleName());
            throw new BusinessException(400, MISSING_FILE_MESSAGE);
        }
    }

    private String fetchRemote(String url) {
        HttpResponse<InputStream> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(FETCH_TIMEOUT)
                    .header("Accept", "image/*")
                    .GET()
                    .build();
            response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(400, "图片下载被中断，请稍后重试");
        } catch (Exception e) {
            log.warn("[AI] 下载图片失败：{}", e.getClass().getSimpleName());
            throw new BusinessException(400, "图片下载失败，请检查地址是否可访问");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            closeQuietly(response.body());
            throw new BusinessException(400, "图片下载失败（HTTP " + response.statusCode() + "），请检查地址是否可访问");
        }
        long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (declared > MAX_BYTES) {
            closeQuietly(response.body());
            throw new BusinessException(400, TOO_LARGE_MESSAGE);
        }
        byte[] bytes;
        try (InputStream in = response.body()) {
            bytes = readBounded(in);
        } catch (IOException e) {
            log.warn("[AI] 读取远程图片失败：{}", e.getClass().getSimpleName());
            throw new BusinessException(400, "图片下载失败，请检查地址是否可访问");
        }
        requireAllowedType(sniff(bytes));
        return encode(bytes);
    }

    /** 按上限读流：超过 4 MiB 立刻停止，避免把超大响应读进内存。 */
    private static byte[] readBounded(InputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if (out.size() > MAX_BYTES) {
                throw new BusinessException(400, TOO_LARGE_MESSAGE);
            }
        }
        return out.toByteArray();
    }

    private static void requireAllowedType(String mime) {
        if (!ALLOWED_MIME.contains(mime)) {
            throw new BusinessException(400, UNSUPPORTED_TYPE_MESSAGE);
        }
    }

    private static String encode(byte[] bytes) {
        String mime = sniff(bytes);
        requireAllowedType(mime);
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    /** 按文件头判定格式；认不出来返回 {@code application/octet-stream}（随后会被拒绝）。 */
    static String sniff(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return "application/octet-stream";
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "image/gif";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private static Path resolveDir(String value, String rootPath) {
        Path path = Paths.get(value == null || value.isBlank() ? "." : value);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        Path root = Paths.get(rootPath == null || rootPath.isBlank()
                ? System.getProperty("user.dir") : rootPath);
        return root.resolve(path).toAbsolutePath().normalize();
    }

    private static void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {
            // 关闭失败不影响主流程
        }
    }
}
