package com.selfblog.service.site.impl;

import com.selfblog.common.enums.SiteAssetKind;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.service.site.SiteAssetService;
import com.selfblog.vo.site.SiteAssetVO;
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
 * 文件原样落盘（SVG 先净化），不做重新编码 —— 这正是它不能复用 MediaService 的原因。
 */
@Slf4j
@Service
public class SiteAssetServiceImpl implements SiteAssetService {

    /** 这些标签都能把 SVG 变成 HTML/脚本容器，整段丢弃；按本地名匹配以覆盖带前缀的 XML 标签。 */
    private static final Set<String> DANGEROUS_SVG_ELEMENTS =
            Set.of("script", "foreignobject", "iframe", "embed", "object");

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
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
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
     * 净化 SVG。{@code /uploads/**} 资源 URL 是公开的，也可能被直接作为顶层文档打开；
     * 那种场景下 SVG 脚本可能以本站源执行，因此不能只依赖当前 {@code <img>} / favicon 等图片上下文的限制。
     * 先净化上传内容是必要的安全边界，图片上下文对脚本的额外禁用则是纵深防御。
     *
     * 解析器对垃圾输入不会抛异常（已实测），因此用"有没有 svg 根元素"来拒绝非 SVG。
     */
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
            if (DANGEROUS_SVG_ELEMENTS.contains(localTagName)) {
                element.remove();
                continue;
            }

            for (Attribute attribute : new ArrayList<>(element.attributes().asList())) {
                String attributeName = attribute.getKey().toLowerCase(Locale.ROOT);
                int attributePrefixSeparator = attributeName.lastIndexOf(':');
                String localAttributeName = attributePrefixSeparator >= 0
                        ? attributeName.substring(attributePrefixSeparator + 1)
                        : attributeName;
                // 去掉全部空白再比，绕过 "java\nscript:" 这类拆分写法。
                String value = attribute.getValue().replaceAll("\\s", "").toLowerCase(Locale.ROOT);
                if (localAttributeName.startsWith("on")
                        || value.contains("javascript:")
                        || value.contains("data:text/html")) {
                    element.removeAttr(attribute.getKey());
                }
            }
        }

        document.outputSettings().prettyPrint(false);
        return document.outerHtml().getBytes(StandardCharsets.UTF_8);
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
