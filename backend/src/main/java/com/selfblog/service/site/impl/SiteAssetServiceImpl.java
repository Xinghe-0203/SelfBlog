package com.selfblog.service.site.impl;

import com.selfblog.common.enums.SiteAssetKind;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.service.site.SiteAssetService;
import com.selfblog.utils.ImageDimensionUtils;
import com.selfblog.vo.site.SiteAssetVO;
import net.coobird.thumbnailator.Thumbnails;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 站点品牌资产上传实现。
 *
 * 文件原样落盘（SVG 先净化），**不重新编码** —— 这正是它不能复用 MediaService 的原因
 * （MediaService 把所有图片压成 JPG，会毁掉 logo 的透明通道与 SVG）。
 *
 * <p>唯一的例外是**页面背景图**：它是整页铺满的装饰照片，动辄几百 KB 到数 MB，
 * 而站点背景最多按 2560px 宽显示。因此 JPEG 背景在落盘前会做一次「超宽则等比缩到
 * 2560px + 质量 0.82 重编码」；PNG/WebP 一律不动（可能带透明通道，重编码会改变观感），
 * 优化失败也**保留原文件**，绝不让上传失败。</p>
 */
@Slf4j
@Service
public class SiteAssetServiceImpl implements SiteAssetService {

    /**
     * SVG 元素白名单（按本地名匹配，因此天然覆盖 {@code x:script} 这类带前缀写法）。
     *
     * <p>【为什么是白名单】此前的五元素黑名单（script/foreignObject/iframe/embed/object）可以被
     * 真实 jsoup 1.18.3 放过的载荷绕过：{@code <style>@import url(...)</style>}、
     * {@code style="..."} 属性、{@code <use href="https://evil/x.svg#a">}、
     * {@code <use href="data:image/svg+xml;base64,...">}、
     * {@code <handler>}、{@code <animate attributeName="onload" values="alert(1)"/>}
     * ——CSS 文本从不被检查，{@code <use href>} 能拉取攻击者控制的外部 SVG，
     * SMIL 的 {@code animate attributeName="on*"} 既不以 {@code on} 开头也不命中值里的
     * 子串规则。SVG 会被 LOGO/FAVICON/AVATAR 接受并同源服务于 {@code /uploads/site-assets/**}，
     * 因此必须反过来只放行确需的图元。</p>
     */
    private static final Set<String> ALLOWED_SVG_ELEMENTS = Set.of(
            "svg", "g", "defs", "title", "desc",
            "path", "rect", "circle", "ellipse", "line", "polyline", "polygon",
            "text", "tspan",
            "lineargradient", "radialgradient", "stop", "clippath", "mask");

    /**
     * SVG 属性白名单（小写比较；写回时保留原始大小写，例如 {@code viewBox}）。
     *
     * <p>刻意不含 {@code style} 与任何 {@code on*} 属性；{@code href}/{@code xlink:href}
     * 与 {@code url(...)} 是仅有的两种 URL 载体，且都只接受同文档 {@code #id} 片段。</p>
     */
    private static final Set<String> ALLOWED_SVG_ATTRIBUTES = Set.of(
            "id", "class", "version", "viewbox", "width", "height",
            // href/xlink:href 允许出现，但下面还有一条更严的规则：只接受同文档 #id 片段。
            "href",
            "x", "y", "x1", "y1", "x2", "y2", "cx", "cy", "r", "rx", "ry",
            "d", "points", "transform", "preserveaspectratio",
            "fill", "fill-opacity", "fill-rule",
            "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin",
            "stroke-dasharray", "stroke-dashoffset", "stroke-opacity", "opacity",
            "offset", "stop-color", "stop-opacity",
            "gradientunits", "gradienttransform", "spreadmethod",
            "clip-path", "clippathunits", "mask", "maskunits",
            "font-family", "font-size", "font-weight", "font-style",
            "text-anchor", "dominant-baseline", "letter-spacing",
            "role", "aria-label", "aria-hidden");

    /** 唯一允许保留的 xmlns 声明值；其他 xmlns 一律删除。 */
    private static final Set<String> ALLOWED_XMLNS_VALUES = Set.of(
            "http://www.w3.org/2000/svg", "http://www.w3.org/1999/xlink");

    /** 匹配 CSS/SVG 里的 {@code url(...)} 引用，用于拒绝外部资源。 */
    private static final java.util.regex.Pattern URL_REFERENCE_PATTERN =
            java.util.regex.Pattern.compile("url\\(\\s*['\"]?([^)'\"]*)");

    /** 页面背景图的最大显示宽度：超过它的部分在屏幕上永远看不到（整页铺满按视口算）。 */
    private static final int BACKGROUND_MAX_WIDTH = 2560;
    /** 背景图重编码质量：视觉上与 0.9 难以区分，体积通常只有一半。 */
    private static final double BACKGROUND_QUALITY = 0.82;
    /** 小于这个体积（700KB）的 JPEG 背景不值得重编码（会损失一点画质换不到多少体积）。 */
    private static final long BACKGROUND_BYTES_TO_OPTIMIZE = 700L * 1024;
    /** 尺寸已合规、但体积超过这个值（2MB）时仍然重编码一次。 */
    private static final long BACKGROUND_BYTES_ALWAYS_KEEP = 2L * 1024 * 1024;

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ICO_MAGIC = {0, 0, 1, 0};

    private static final byte[] RIFF_MAGIC = ascii("RIFF");
    private static final byte[] WEBP_MAGIC = ascii("WEBP");
    private static final byte[] WAVE_MAGIC = ascii("WAVE");
    private static final byte[] OGG_MAGIC = ascii("OggS");
    private static final byte[] FTYP_MAGIC = ascii("ftyp");
    private static final byte[] ID3_MAGIC = ascii("ID3");

    @Value("${upload.base-path:uploads}")
    private String uploadBasePath;

    @Value("${upload.root-path:#{systemProperties['user.dir']}}")
    private String uploadRootPath;

    /** 与 MediaServiceImpl 保持一致；默认值与 WebMvcConfig 里 /uploads/** 的映射一致。 */
    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public SiteAssetVO store(MultipartFile file, String kind) {
        SiteAssetKind assetKind = SiteAssetKind.fromWireName(kind);
        if (assetKind == null) {
            throw new BusinessException(400, "不支持的资源类型");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        if (file.getSize() > assetKind.getMaxBytes()) {
            throw new BusinessException(400, "文件大小不能超过 " + assetKind.getMaxMegabytes() + "MB");
        }

        String extension = SiteAssetKind.extensionOf(file.getOriginalFilename());
        if (!assetKind.getExtensions().contains(extension)) {
            throw new BusinessException(400, "仅支持 " + assetKind.describeExtensions() + " 格式");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(400, "读取上传文件失败");
        }

        verifyMagicNumber(bytes, extension);

        if (".svg".equals(extension)) {
            bytes = sanitizeSvg(bytes);
        }

        if (assetKind == SiteAssetKind.BACKGROUND) {
            bytes = optimizeBackground(bytes, extension);
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path directory = Paths.get(uploadRootPath, uploadBasePath, "site-assets", assetKind.getWireName())
                .toAbsolutePath()
                .normalize();
        Path destination = directory.resolve(filename).normalize();
        // 目录名来自固定枚举、文件名是自己生成的 UUID；这里再断言一次不越出目录树。
        if (!destination.startsWith(directory)) {
            throw new BusinessException(400, "文件名不合法");
        }

        try {
            Files.createDirectories(directory);
            Files.copy(new ByteArrayInputStream(bytes), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("[SiteAsset] 文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "文件保存失败，请稍后重试");
        }

        SiteAssetVO vo = new SiteAssetVO();
        vo.setUrl(urlPrefix + "/site-assets/" + assetKind.getWireName() + "/" + filename);
        vo.setKind(assetKind.getWireName());
        vo.setSize((long) bytes.length);
        vo.setOriginalName(file.getOriginalFilename());
        return vo;
    }

    /**
     * 比对真实文件头。扩展名由客户端提供，把 evil.html 改名成 logo.png 就能过白名单，
     * 所以这里必须看内容。
     */
    private void verifyMagicNumber(byte[] bytes, String extension) {
        boolean looksRight = switch (extension) {
            case ".png" -> startsWith(bytes, PNG_MAGIC);
            case ".jpg", ".jpeg" -> startsWith(bytes, JPEG_MAGIC);
            case ".webp" -> startsWith(bytes, RIFF_MAGIC) && matchesAt(bytes, 8, WEBP_MAGIC);
            // 不少工具把 favicon.ico 直接写成一张 PNG，两种头都放行（都是安全的光栅格式）。
            case ".ico" -> startsWith(bytes, ICO_MAGIC) || startsWith(bytes, PNG_MAGIC);
            case ".svg" -> looksLikeSvg(bytes);
            case ".ogg" -> startsWith(bytes, OGG_MAGIC);
            case ".wav" -> startsWith(bytes, RIFF_MAGIC) && matchesAt(bytes, 8, WAVE_MAGIC);
            case ".m4a" -> matchesAt(bytes, 4, FTYP_MAGIC);
            // MP3 的 ID3 头/帧同步同样覆盖 AAC 的 ADTS 帧同步（0xFFF1、0xFFF9）。
            case ".mp3", ".aac" -> isMpegAudio(bytes);
            default -> false;
        };
        if (!looksRight) {
            throw new BusinessException(400, "文件内容与扩展名不匹配，请上传真实文件");
        }
    }

    /**
     * 允许 UTF-8 BOM、XML 声明、注释或 DOCTYPE 出现在 &lt;svg&gt; 之前，
     * 但不允许它们之后再夹带别的东西。
     */
    private boolean looksLikeSvg(byte[] bytes) {
        String head = new String(bytes, 0, Math.min(bytes.length, 512), StandardCharsets.UTF_8);
        String withoutBom = head.startsWith("\uFEFF") ? head.substring(1) : head;
        String lower = withoutBom.toLowerCase(Locale.ROOT);
        int svgIndex = lower.indexOf("<svg");
        if (svgIndex < 0) {
            return false;
        }
        String before = lower.substring(0, svgIndex).trim();
        return before.isEmpty()
                || before.startsWith("<?xml")
                || before.startsWith("<!--")
                || before.startsWith("<!doctype");
    }

    private boolean isMpegAudio(byte[] bytes) {
        if (startsWith(bytes, ID3_MAGIC)) {
            return true;
        }
        if (bytes.length < 2) {
            return false;
        }
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        return first == 0xFF && (second & 0xE0) == 0xE0;
    }

    /**
     * 净化 SVG：<b>白名单式</b>重建，而不是黑名单式删除。
     *
     * <p>{@code /uploads/**} 资源 URL 是公开的，也可能被直接作为顶层文档打开；
     * 那种场景下 SVG 脚本可能以本站源执行，因此不能只依赖当前 {@code <img>} / favicon
     * 等图片上下文的限制。先净化上传内容是必要的安全边界，图片上下文对脚本的额外禁用
     * 则是纵深防御。</p>
     *
     * <p>只保留 {@link #ALLOWED_SVG_ELEMENTS} 里的元素与 {@link #ALLOWED_SVG_ATTRIBUTES}
     * 里的属性；{@code href}/{@code xlink:href} 仅允许同文档 {@code #id} 片段，
     * {@code url(...)} 引用同样只允许片段，因此 {@code <use href="https://evil/x.svg#a">}
     * 与 {@code <use href="data:image/svg+xml;base64,...">} 都会被拒绝。
     * 解析器对垃圾输入不会抛异常（已实测），因此用“有没有 svg 根元素”来拒绝非 SVG。</p>
     */
    /**
     * 页面背景图的按需瘦身：**只处理 JPEG**，超宽才缩，其余格式与失败一律原样返回。
     *
     * <p>为什么只处理 JPEG：PNG/WebP 可能带透明通道，重编码成 JPEG 会丢透明（还会改变观感）；
     * SVG 是矢量。照片类背景绝大多数本来就是 JPEG，这是收益最大、风险最小的一档。</p>
     *
     * <p>三重保护：</p>
     * <ol>
     *   <li>解码前用 {@link ImageDimensionUtils#validatePixelCount} 拦「小文件声明超大尺寸」
     *       的解压缩炸弹（与 MediaService 同一道闸）；</li>
     *   <li>宽度未超限且体积已经不��（≤700KB）时直接返回原字节，不做无意义的重编码；</li>
     *   <li>任何异常都 log 后返回原字节 —— 优化是锦上添花，不能让上传失败。</li>
     * </ol>
     */
    private byte[] optimizeBackground(byte[] bytes, String extension) {
        if (!(".jpg".equals(extension) || ".jpeg".equals(extension))) return bytes;
        if (bytes.length <= BACKGROUND_BYTES_TO_OPTIMIZE) return bytes;
        try {
            ImageDimensionUtils.validatePixelCount(
                    new ByteArrayInputStream(bytes), "background" + extension);
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) return bytes;
            if (image.getWidth() <= BACKGROUND_MAX_WIDTH) {
                // 尺寸已经合规，只是体积偏大：仍然按同一档质量重编码，体积通常能降一半。
                if (bytes.length <= BACKGROUND_BYTES_ALWAYS_KEEP) return bytes;
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .width(BACKGROUND_MAX_WIDTH)
                    .outputFormat("jpg")
                    .outputQuality(BACKGROUND_QUALITY)
                    .toOutputStream(output);
            byte[] optimized = output.toByteArray();
            // 重编码后反而更大（高质量小图）就没必要替换。
            if (optimized.length >= bytes.length) return bytes;
            log.info("[SiteAsset] 背景图优化：{}KB -> {}KB（宽 {} -> {}）",
                    bytes.length / 1024, optimized.length / 1024, image.getWidth(),
                    Math.min(image.getWidth(), BACKGROUND_MAX_WIDTH));
            return optimized;
        } catch (RuntimeException | IOException e) {
            log.warn("[SiteAsset] 背景图优化失败，保留原文件: {}", e.getMessage());
            return bytes;
        }
    }

    private byte[] sanitizeSvg(byte[] bytes) {
        String raw = new String(bytes, StandardCharsets.UTF_8);
        Document document = Jsoup.parse(raw, "", Parser.xmlParser());

        Element root = document.selectFirst("svg");
        if (root == null) {
            throw new BusinessException(400, "SVG 文件缺少 <svg> 根元素");
        }

        // SVG 是 XML，攻击者可用 x:script + SVG namespace 绕过仅按无前缀标签名工作的 CSS selector。
        // 遍历快照并比较本地名，既覆盖前缀形式，也避免移除节点时修改正在遍历的集合。
        for (Element element : new ArrayList<>(document.getAllElements())) {
            String tagName = element.tagName().toLowerCase(Locale.ROOT);
            int tagPrefixSeparator = tagName.lastIndexOf(':');
            String localTagName = tagPrefixSeparator >= 0
                    ? tagName.substring(tagPrefixSeparator + 1)
                    : tagName;
            if (!ALLOWED_SVG_ELEMENTS.contains(localTagName)) {
                element.remove();
                continue;
            }

            for (Attribute attribute : new ArrayList<>(element.attributes().asList())) {
                String attributeName = attribute.getKey().toLowerCase(Locale.ROOT);
                int attributePrefixSeparator = attributeName.lastIndexOf(':');
                String localAttributeName = attributePrefixSeparator >= 0
                        ? attributeName.substring(attributePrefixSeparator + 1)
                        : attributeName;

                // xmlns / xmlns:xlink 只放行 SVG 自身的两个命名空间声明。
                if (attributeName.equals("xmlns") || attributeName.startsWith("xmlns:")) {
                    if (!ALLOWED_XMLNS_VALUES.contains(attribute.getValue().trim())) {
                        element.removeAttr(attribute.getKey());
                    }
                    continue;
                }

                if (!ALLOWED_SVG_ATTRIBUTES.contains(localAttributeName)) {
                    element.removeAttr(attribute.getKey());
                    continue;
                }

                // href/xlink:href 只允许同文档片段引用；任何外部/内联 data: 目标一律拒绝。
                if ("href".equals(localAttributeName)) {
                    if (!isSameDocumentFragment(attribute.getValue())) {
                        element.removeAttr(attribute.getKey());
                    }
                    continue;
                }

                // fill/stroke/clip-path/mask 等可以写 url(...)，但同样只允许 #id 片段。
                if (!hasOnlyLocalUrlReferences(attribute.getValue())) {
                    element.removeAttr(attribute.getKey());
                }
            }
        }

        document.outputSettings().prettyPrint(false);
        return document.outerHtml().getBytes(StandardCharsets.UTF_8);
    }

    /** {@code #id} 形式的同文档片段引用（非空、无空白、不含 {@code :}）。 */
    private boolean isSameDocumentFragment(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.length() > 1
                && trimmed.startsWith("#")
                && trimmed.indexOf(':') < 0
                && !trimmed.matches(".*\\s.*");
    }

    /** 属性值里若出现 {@code url(...)}，其目标必须是同文档 {@code #id} 片段。 */
    private boolean hasOnlyLocalUrlReferences(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        java.util.regex.Matcher matcher = URL_REFERENCE_PATTERN.matcher(value);
        while (matcher.find()) {
            if (!isSameDocumentFragment(matcher.group(1))) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAt(byte[] bytes, int offset, byte[] expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (bytes[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
