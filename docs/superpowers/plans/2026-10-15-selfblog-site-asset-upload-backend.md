# 资源上传通道（后端）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给管理员提供一条独立的品牌资产上传通道（网站 logo、favicon、站长头像、背景音乐），并把 `faviconUrl` / `siteLogoUrl` 正式收录为站点设置项，使前端顶栏的品牌图标与浏览器标签图标真正可由后台上传。

**Architecture:** 不复用 `MediaService`。那条通道用 thumbnailator 把所有图片重新编码成 `.jpg`（`MediaServiceImpl:279`），`.ico`、`.svg` 与音频根本过不去；而 favicon 需要 `.ico`、logo 需要 `.svg`、音乐不是图片。因此新增一条"原样落盘"的窄通道：`AdminSiteAssetController` → `SiteAssetService` → 校验（扩展名白名单 + 体积上限 + 魔数嗅探，SVG 额外做净化）→ 写入 `uploads/site-assets/<kind>/<uuid>.<ext>`，由既有的 `/uploads/**` 静态映射对外提供。设置项走既有的 `site_setting` 键值表，**不需要任何建表语句**。

**Tech Stack:** Spring Boot 3.3.13 / Java 21 / MyBatis Plus / Jsoup（已在依赖中）/ JUnit 5 + MockMvc。

## Global Constraints

- **本机 Maven 不在 PATH 上**，`mvn` 会报 `CommandNotFoundException`。可用的调用方式是 `.m2/wrapper/dists` 里那份 3.9.12：

  ```powershell
  $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'   # 必须用 21，不要用机器上的 jdk-25
  $mvn = 'C:\Users\Zora\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd'
  & $mvn test -Dtest=SomeTest -Dskip.npm=true -Dskip.installnodenpm=true
  ```

- **后端测试必须加 `-Dskip.npm=true -Dskip.installnodenpm=true`**。`pom.xml` 的 `frontend-maven-plugin` 默认会在 `initialize`/`generate-resources` 阶段跑 `npm ci` + `npm run build`（见 `pom.xml` 中 `frontend-maven-plugin` 段），不加这两个开关每次测试都要先把整个前端构建一遍。加上之后单测 8 秒跑完。
- **上面这条也意味着：后端测试会动前端产物。** 因此**绝不要在前端文件处于半成品状态时跑 `mvn test`**（不带 skip 开关的那种）。
- **基线：`mvn test` = 386 个用例全绿 + BUILD SUCCESS**（2026-10-15 实测）。改动后不得低于这个数。
- 命令一律在 `backend/` 下执行。
- **不需要任何数据库改动**：`site_setting` 是 `setting_key` / `setting_value` 的键值表（`schema.sql`、`schema-sqlite.sql`、`src/test/resources/schema.sql` 三处都已有），新增设置项只是多插一行数据。
- 统一响应体是 `Result<T>` = `{ code, message, data, timestamp }`，成功码 200；错误一律 `throw new BusinessException(<httpStatus>, "中文提示")`，`GlobalExceptionHandler` 会把 `code` 直接当作 HTTP 状态码并回填 `message`。
- 写操作加 `@Transactional(rollbackFor = Exception.class)`，读操作加 `@Transactional(readOnly = true)`（本计划的服务只被上传接口调用，不碰数据库，因此不额外加事务注解）。
- 新控制器放在 `controller/admin/` 包下，类级同时使用 `@RequestMapping("/admin/...")` 与 `@PreAuthorize("hasRole('admin')")` —— 与 `AdminSiteSettingsController` 一致。运行时由 `WebMvcConfig.configurePathMatch` 统一加上 `/api` 前缀，**测试里的请求路径不要写 `/api`**。
- 提交信息用英文 conventional commit 前缀。每个任务结束必须 `mvn test` 全绿再提交（带 skip 开关）。

---

## 文件清单

| 文件 | 动作 |
|------|------|
| `backend/src/main/java/com/selfblog/common/enums/SiteAssetKind.java` | 新建 |
| `backend/src/test/java/com/selfblog/common/enums/SiteAssetKindTest.java` | 新建 |
| `backend/src/main/java/com/selfblog/vo/site/SiteAssetVO.java` | 新建 |
| `backend/src/main/java/com/selfblog/service/site/SiteAssetService.java` | 新建 |
| `backend/src/main/java/com/selfblog/service/site/impl/SiteAssetServiceImpl.java` | 新建 |
| `backend/src/test/java/com/selfblog/service/site/impl/SiteAssetServiceImplTest.java` | 新建 |
| `backend/src/main/java/com/selfblog/controller/admin/AdminSiteAssetController.java` | 新建 |
| `backend/src/test/java/com/selfblog/controller/AdminSiteAssetControllerTest.java` | 新建 |
| `backend/src/main/java/com/selfblog/config/web/RateLimitProperties.java` | 修改（新增限流项） |
| `backend/src/main/java/com/selfblog/service/site/impl/SiteSettingsServiceImpl.java` | 修改（收录两个设置键） |
| `backend/src/test/java/com/selfblog/service/site/impl/SiteSettingsServiceImplTest.java` | 新建 |

---

### Task 1: `SiteAssetKind` 枚举

每种资产自带扩展名白名单与体积上限。把它们放在枚举里而不是散在服务方法中，是为了让"新增一种资产"只改一处、并且能被单元测试直接覆盖。

**Files:**
- Create: `backend/src/main/java/com/selfblog/common/enums/SiteAssetKind.java`
- Test: `backend/src/test/java/com/selfblog/common/enums/SiteAssetKindTest.java`

**Interfaces:**
- Consumes: 无
- Produces: `SiteAssetKind`（枚举，值 `LOGO` / `FAVICON` / `AVATAR` / `MUSIC`）、内嵌 `SiteAssetKind.Category`（`IMAGE` / `AUDIO`）、`getWireName(): String`、`getCategory(): Category`、`getExtensions(): Set<String>`、`getMaxBytes(): long`、`getMaxMegabytes(): long`、`isAudio(): boolean`、`describeExtensions(): String`、静态 `fromWireName(String): SiteAssetKind`（不认识返回 `null`）、静态 `extensionOf(String): String`

- [ ] **Step 1: 写枚举与测试**

创建 `backend/src/main/java/com/selfblog/common/enums/SiteAssetKind.java`：

```java
package com.selfblog.common.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * 站点品牌资产种类。
 *
 * 与 MediaService 分开的原因：媒体通道会用 thumbnailator 把所有图片重新编码成 jpg
 * （见 MediaServiceImpl），.ico、.svg 与音频走不通；而 favicon 需要 .ico、
 * logo 需要 .svg、背景音乐根本不是图片。
 *
 * 每种资产自带扩展名白名单与体积上限：品牌图标只用于显示，音乐可能几十 MB，
 * 共用一套阈值必然二选一地被浪费。
 */
public enum SiteAssetKind {
    /** 顶栏品牌商标，允许矢量图。 */
    LOGO("logo", Category.IMAGE, Set.of(".png", ".jpg", ".jpeg", ".webp", ".svg"), 2L * 1024 * 1024),
    /** 浏览器标签页图标，需要 .ico，也允许 .svg。 */
    FAVICON("favicon", Category.IMAGE, Set.of(".png", ".ico", ".svg"), 2L * 1024 * 1024),
    /** 站长头像，允许矢量图。 */
    AVATAR("avatar", Category.IMAGE, Set.of(".png", ".jpg", ".jpeg", ".webp", ".svg"), 2L * 1024 * 1024),
    /** 背景音乐。 */
    MUSIC("music", Category.AUDIO, Set.of(".mp3", ".ogg", ".wav", ".m4a", ".aac"), 20L * 1024 * 1024);

    /** 资产大类，决定用哪套魔数校验。 */
    public enum Category {
        IMAGE, AUDIO
    }

    private final String wireName;
    private final Category category;
    private final Set<String> extensions;
    private final long maxBytes;

    SiteAssetKind(String wireName, Category category, Set<String> extensions, long maxBytes) {
        this.wireName = wireName;
        this.category = category;
        this.extensions = extensions;
        this.maxBytes = maxBytes;
    }

    public String getWireName() {
        return wireName;
    }

    public Category getCategory() {
        return category;
    }

    /** 允许的扩展名，全部小写、带点号。 */
    public Set<String> getExtensions() {
        return extensions;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    /** 体积上限的兆字节表示，用于拼错误提示。 */
    public long getMaxMegabytes() {
        return maxBytes / (1024 * 1024);
    }

    public boolean isAudio() {
        return category == Category.AUDIO;
    }

    /** 供错误提示使用的可读格式列表，例如 ".jpg、.png、.svg"。顺序固定，便于断言。 */
    public String describeExtensions() {
        return String.join("、", extensions.stream().sorted().toList());
    }

    /**
     * 把请求里的 kind 字符串解析成枚举。大小写不敏感、容忍首尾空白；
     * 不认识的值返回 null，由调用方决定报什么错。
     */
    public static SiteAssetKind fromWireName(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(kind -> kind.wireName.equals(normalized))
                .findFirst()
                .orElse(null);
    }

    /**
     * 取文件名的小写扩展名（含点号）。
     * 没有扩展名、或点号在开头/结尾时返回空串 —— 那种文件名一律拒绝。
     */
    public static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        String base = filename.replaceAll("[/\\\\]", "_");
        int dot = base.lastIndexOf('.');
        if (dot <= 0 || dot == base.length() - 1) {
            return "";
        }
        return base.substring(dot).toLowerCase(Locale.ROOT);
    }
}
```

创建 `backend/src/test/java/com/selfblog/common/enums/SiteAssetKindTest.java`：

```java
package com.selfblog.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SiteAssetKind")
class SiteAssetKindTest {

    @ParameterizedTest
    @ValueSource(strings = {"logo", "favicon", "avatar", "music"})
    @DisplayName("fromWireName_AcceptsEveryDocumentedWireName")
    void fromWireName_AcceptsEveryDocumentedWireName(String raw) {
        SiteAssetKind kind = SiteAssetKind.fromWireName(raw);
        assertNotNull(kind);
        assertEquals(raw, kind.getWireName());
    }

    @Test
    @DisplayName("fromWireName_IsCaseInsensitiveAndTrims")
    void fromWireName_IsCaseInsensitiveAndTrims() {
        assertEquals(SiteAssetKind.LOGO, SiteAssetKind.fromWireName("  LOGO "));
        assertEquals(SiteAssetKind.MUSIC, SiteAssetKind.fromWireName("Music"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "cover", "article", "video", "logo2", "会议"})
    @DisplayName("fromWireName_RejectsUnknownValues")
    void fromWireName_RejectsUnknownValues(String raw) {
        assertNull(SiteAssetKind.fromWireName(raw));
    }

    @Test
    @DisplayName("fromWireName_RejectsNull")
    void fromWireName_RejectsNull() {
        assertNull(SiteAssetKind.fromWireName(null));
    }

    @Test
    @DisplayName("onlyMusicIsAudio")
    void onlyMusicIsAudio() {
        assertTrue(SiteAssetKind.MUSIC.isAudio());
        assertEquals(SiteAssetKind.Category.AUDIO, SiteAssetKind.MUSIC.getCategory());
        assertFalse(SiteAssetKind.LOGO.isAudio());
        assertFalse(SiteAssetKind.FAVICON.isAudio());
        assertFalse(SiteAssetKind.AVATAR.isAudio());
    }

    @Test
    @DisplayName("musicGetsTheLargerCap")
    void musicGetsTheLargerCap() {
        assertEquals(20L, SiteAssetKind.MUSIC.getMaxMegabytes());
        assertEquals(2L, SiteAssetKind.LOGO.getMaxMegabytes());
        assertEquals(2L, SiteAssetKind.FAVICON.getMaxMegabytes());
        assertEquals(2L, SiteAssetKind.AVATAR.getMaxMegabytes());
    }

    @Test
    @DisplayName("faviconAllowsIcoAndLogoAllowsSvg")
    void faviconAllowsIcoAndLogoAllowsSvg() {
        assertTrue(SiteAssetKind.FAVICON.getExtensions().contains(".ico"));
        assertTrue(SiteAssetKind.LOGO.getExtensions().contains(".svg"));
    }

    @Test
    @DisplayName("musicRejectsImageExtensions")
    void musicRejectsImageExtensions() {
        assertFalse(SiteAssetKind.MUSIC.getExtensions().contains(".png"));
    }

    @Test
    @DisplayName("describeExtensions_IsSortedAndReadable")
    void describeExtensions_IsSortedAndReadable() {
        assertEquals(".ico、.png、.svg", SiteAssetKind.FAVICON.describeExtensions());
    }

    @Test
    @DisplayName("extensionOf_LowercasesTheSuffix")
    void extensionOf_LowercasesTheSuffix() {
        assertEquals(".png", SiteAssetKind.extensionOf("Logo.PNG"));
        assertEquals(".jpeg", SiteAssetKind.extensionOf("my.photo.JPEG"));
        assertEquals(".svg", SiteAssetKind.extensionOf("a/b/my-logo.svg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"noextension", "trailing.", ".hidden", "", "  "})
    @DisplayName("extensionOf_ReturnsEmptyWhenThereIsNoUsableSuffix")
    void extensionOf_ReturnsEmptyWhenThereIsNoUsableSuffix(String name) {
        assertEquals("", SiteAssetKind.extensionOf(name));
    }

    @Test
    @DisplayName("extensionOf_RejectsNull")
    void extensionOf_RejectsNull() {
        assertEquals("", SiteAssetKind.extensionOf(null));
    }
}
```

- [ ] **Step 2: 跑测试确认通过**

```powershell
& $mvn test -Dtest=SiteAssetKindTest -Dskip.npm=true -Dskip.installnodenpm=true
```

Expected: `Tests run: 25, Failures: 0, Errors: 0`（9 个 `@Test` + `@ValueSource` 展开 4 + 7 + 5 = 16）。

> 注：`@ParameterizedTest` 在本仓库是**首次使用**，`junit-jupiter-params` 由 `spring-boot-starter-test` 传递带入，无需改 `pom.xml`。若报找不到注解，不要再加依赖，改成把参数写进 `@Test` 里逐个断言即可。

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/selfblog/common/enums/SiteAssetKind.java backend/src/test/java/com/selfblog/common/enums/SiteAssetKindTest.java
git commit -m "feat: add SiteAssetKind enum for brand asset upload limits"
```

---

### Task 2: `SiteAssetVO` 与 `SiteAssetService` 接口

**Files:**
- Create: `backend/src/main/java/com/selfblog/vo/site/SiteAssetVO.java`
- Create: `backend/src/main/java/com/selfblog/service/site/SiteAssetService.java`

**Interfaces:**
- Consumes: `SiteAssetKind`（Task 1）
- Produces: `SiteAssetVO`（Lombok `@Data`，字段 `url` / `kind` / `size` / `originalName`，另带 `@Schema`）、`SiteAssetService#store(MultipartFile file, String kind): SiteAssetVO`

- [ ] **Step 1: 写 VO**

创建 `backend/src/main/java/com/selfblog/vo/site/SiteAssetVO.java`：

```java
package com.selfblog.vo.site;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 站点品牌资产上传结果。
 */
@Data
@Schema(description = "站点品牌资产")
public class SiteAssetVO {

    @Schema(description = "文件访问URL，形如 /uploads/site-assets/logo/xxx.png")
    private String url;

    @Schema(description = "资产种类：logo / favicon / avatar / music")
    private String kind;

    @Schema(description = "落盘后的字节数")
    private Long size;

    @Schema(description = "上传时的原始文件名")
    private String originalName;
}
```

- [ ] **Step 2: 写接口**

创建 `backend/src/main/java/com/selfblog/service/site/SiteAssetService.java`：

```java
package com.selfblog.service.site;

import com.selfblog.vo.site.SiteAssetVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 站点品牌资产上传。
 */
public interface SiteAssetService {

    /**
     * 校验并原样保存一个品牌资产。
     *
     * @param file 上传文件
     * @param kind 资产种类，取值见 {@link com.selfblog.common.enums.SiteAssetKind}
     * @return 可公开访问的 URL 与元信息
     */
    SiteAssetVO store(MultipartFile file, String kind);
}
```

- [ ] **Step 3: 编译确认**

```powershell
& $mvn -q compile -Dskip.npm=true -Dskip.installnodenpm=true
```

Expected: 无输出即成功。

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/selfblog/vo/site/SiteAssetVO.java backend/src/main/java/com/selfblog/service/site/SiteAssetService.java
git commit -m "feat: add SiteAssetVO and SiteAssetService contract"
```

---

### Task 3: `SiteAssetServiceImpl` —— 校验、净化与落盘

这是本计划的核心。三道校验各自的必要性：

1. **扩展名白名单**：`.php`、`.jsp`、`.html` 之类绝不能进上传目录。
2. **体积上限**：按 `SiteAssetKind` 分档，图片 2MB、音乐 20MB（都在 `spring.servlet.multipart.max-file-size=50MB` 之下）。
3. **魔数嗅探**：扩展名是客户端给的，把 `evil.html` 改名成 `logo.png` 就能绕过第 1 条。必须比对真实文件头。

SVG 额外做净化。**必须明确它的定位**：所有渲染点都是图片上下文（`<img src>`、`<link rel="icon">`、`<audio src>`），SVG 里的脚本在那里本就不会执行，所以净化是纵深防御而非唯一防线。另外**必须用 `selectFirst("svg") == null` 来拒绝非 SVG**，不能靠捕获异常 —— 已实测 Jsoup 的 XML 解析器对垃圾输入（`not xml at all <<<>>>`）不抛异常，只是解析不出 `svg` 根元素。

**Files:**
- Create: `backend/src/main/java/com/selfblog/service/site/impl/SiteAssetServiceImpl.java`
- Test: `backend/src/test/java/com/selfblog/service/site/impl/SiteAssetServiceImplTest.java`

**Interfaces:**
- Consumes: `SiteAssetKind`（Task 1）、`SiteAssetVO` / `SiteAssetService`（Task 2）
- Produces: `SiteAssetServiceImpl`（`@Service`，实现 `SiteAssetService#store`）

- [x] **Step 1: 写实现**

创建 `backend/src/main/java/com/selfblog/service/site/impl/SiteAssetServiceImpl.java`：

```java
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
import java.util.UUID;

/**
 * 站点品牌资产上传实现。
 *
 * 文件原样落盘（SVG 先净化），不做重新编码 —— 这正是它不能复用 MediaService 的原因。
 */
@Slf4j
@Service
public class SiteAssetServiceImpl implements SiteAssetService {

    /** 这些标签都能把 SVG 变成 HTML/脚本容器，整段丢弃。 */
    private static final String DANGEROUS_SVG_SELECTOR = "script, foreignObject, iframe, embed, object";

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
     * 净化 SVG。渲染点全是图片上下文，脚本在那里不会执行，所以这是纵深防御：
     * 万一将来有人把 logo 直接 innerHTML 进页面，至少文件里已经没有可执行的壳。
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

        document.select(DANGEROUS_SVG_SELECTOR).remove();

        for (Element element : document.getAllElements()) {
            for (Attribute attribute : new ArrayList<>(element.attributes().asList())) {
                String name = attribute.getKey().toLowerCase(Locale.ROOT);
                // 去掉全部空白再比，绕过 "java\nscript:" 这类拆分写法。
                String value = attribute.getValue().replaceAll("\\s", "").toLowerCase(Locale.ROOT);
                if (name.startsWith("on")
                        || value.contains("javascript:")
                        || value.contains("data:text/html")) {
                    element.removeAttribute(attribute.getKey());
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
```

- [x] **Step 2: 写测试**

创建 `backend/src/test/java/com/selfblog/service/site/impl/SiteAssetServiceImplTest.java`：

```java
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
        byte[] aac = {(byte) 0xFF, (byte) 0xF1, 0x50, 0x80, 0, 0, 0, 0};
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
```

- [x] **Step 3: 跑测试**

```powershell
& $mvn test -Dtest=SiteAssetServiceImplTest -Dskip.npm=true -Dskip.installnodenpm=true
```

Expected: `Tests run: 40, Failures: 0, Errors: 0`（包含新增的命名空间前缀 SVG 脚本回归用例）。

- [x] **Step 4: 确认测试没有污染真实上传目录**

```powershell
Get-ChildItem -Recurse uploads -ErrorAction SilentlyContinue | Select-Object -First 5 -ExpandProperty FullName
```

Expected: 无输出（或只有改动前就存在的内容）。测试全部写在 `@TempDir` 里，`backend/uploads` 不应出现 `site-assets`。

- [x] **Step 5: 提交**

```bash
git add backend/src/main/java/com/selfblog/service/site/impl/SiteAssetServiceImpl.java backend/src/test/java/com/selfblog/service/site/impl/SiteAssetServiceImplTest.java
git commit -m "feat: validate and store uploaded site assets"
```

---

### Task 4: `AdminSiteAssetController`

**Files:**
- Create: `backend/src/main/java/com/selfblog/controller/admin/AdminSiteAssetController.java`
- Test: `backend/src/test/java/com/selfblog/controller/AdminSiteAssetControllerTest.java`

**Interfaces:**
- Consumes: `SiteAssetService`（Task 2/3）、`SiteAssetVO`（Task 2）
- Produces: `POST /api/admin/site-assets`（multipart：`file`、`kind`）→ `Result<SiteAssetVO>`

- [ ] **Step 1: 写控制器**

创建 `backend/src/main/java/com/selfblog/controller/admin/AdminSiteAssetController.java`：

```java
package com.selfblog.controller.admin;

import com.selfblog.common.result.Result;
import com.selfblog.service.site.SiteAssetService;
import com.selfblog.vo.site.SiteAssetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 站点品牌资产上传接口。
 *
 * 单独开一条通道而不是复用 /media/upload：那条通道会用 thumbnailator
 * 把所有图片重新编码成 jpg，.ico / .svg / 音频都过不去。
 */
@Tag(name = "站点资源", description = "站点品牌资产上传")
@RestController
@RequestMapping("/admin/site-assets")
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminSiteAssetController {

    private final SiteAssetService siteAssetService;

    @Operation(summary = "上传站点品牌资产")
    @PostMapping
    public Result<SiteAssetVO> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam("kind") String kind) {
        return Result.success(siteAssetService.store(file, kind));
    }
}
```

- [ ] **Step 2: 写测试**

创建 `backend/src/test/java/com/selfblog/controller/AdminSiteAssetControllerTest.java`：

```java
package com.selfblog.controller;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.controller.admin.AdminSiteAssetController;
import com.selfblog.service.site.SiteAssetService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.vo.site.SiteAssetVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminSiteAssetController 单元测试。
 * 测试路径不写 /api —— 测试 profile 里 app.api-prefix 为空。
 *
 * 有两处必须照抄，否则测试根本跑不起来（已在本仓库实测确认）：
 *
 * 1. 必须 `@MockBean JwtUtils`。`@WebMvcTest` 切片会把 `SecurityConfig` 一并装配，
 *    它注册的 `jwtAuthenticationFilter` 依赖 `JwtUtils`；不 mock 会直接
 *    `NoSuchBeanDefinitionException` 起不来。（`AuthControllerTest` 也是这么做的。）
 * 2. 必须给 `@WithMockUser(roles = "admin")`。切片里的方法级安全**是生效的**：
 *    实测 admin → 200、普通用户 → 403、匿名 → 403。
 */
@WebMvcTest(AdminSiteAssetController.class)
@DisplayName("AdminSiteAssetController")
class AdminSiteAssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SiteAssetService siteAssetService;

    @MockBean
    private JwtUtils jwtUtils;

    private MockMultipartFile anyFile() {
        return new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("upload_ReturnsStoredAssetInEnvelope")
    void upload_ReturnsStoredAssetInEnvelope() throws Exception {
        SiteAssetVO vo = new SiteAssetVO();
        vo.setUrl("/uploads/site-assets/logo/abc123.png");
        vo.setKind("logo");
        vo.setSize(123L);
        vo.setOriginalName("logo.png");
        when(siteAssetService.store(any(), eq("logo"))).thenReturn(vo);

        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()).param("kind", "logo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value("/uploads/site-assets/logo/abc123.png"))
                .andExpect(jsonPath("$.data.kind").value("logo"))
                .andExpect(jsonPath("$.data.size").value(123));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("upload_PassesTheKindParameterThrough")
    void upload_PassesTheKindParameterThrough() throws Exception {
        when(siteAssetService.store(any(), any())).thenReturn(new SiteAssetVO());

        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()).param("kind", "music"))
                .andExpect(status().isOk());

        verify(siteAssetService).store(any(), eq("music"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("upload_SurfacesValidationFailureAsBadRequest")
    void upload_SurfacesValidationFailureAsBadRequest() throws Exception {
        when(siteAssetService.store(any(), any()))
                .thenThrow(new BusinessException(400, "仅支持 .png、.svg 格式"));

        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()).param("kind", "logo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅支持 .png、.svg 格式"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("upload_RequiresTheKindParameter")
    void upload_RequiresTheKindParameter() throws Exception {
        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("upload_RejectsNonAdminCallers")
    void upload_RejectsNonAdminCallers() throws Exception {
        // 类上的 @PreAuthorize("hasRole('admin')") 在切片里是真的会拦人的（实测 403），
        // 这条用例保证它没被误删
        mockMvc.perform(multipart("/admin/site-assets").file(anyFile()).param("kind", "logo"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 3: 跑测试**

```powershell
& $mvn test -Dtest=AdminSiteAssetControllerTest -Dskip.npm=true -Dskip.installnodenpm=true
```

Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/selfblog/controller/admin/AdminSiteAssetController.java backend/src/test/java/com/selfblog/controller/AdminSiteAssetControllerTest.java
git commit -m "feat: expose the admin site-asset upload endpoint"
```

---

### Task 5: 给 `/api/admin/site-assets` 配限流

`RateLimitInterceptor` 用 `path.startsWith(prefix)` 匹配，没有命中任何前缀时**直接放行不限制**（`RateLimitInterceptor:50`）。`/api/admin/site-assets` 目前谁也匹配不到，所以是无限额的 —— 一个允许传 20MB 文件的接口不该这样。`/api/admin/albums/` 已有先例（`galleryUpload`），照做即可。

因为 `getAllLimits()` 返回 `HashMap` 且遍历时 `break`，多个前缀同时命中时结果不确定。本条目不受影响：现有前缀里没有一个是 `/api/admin/site-assets` 的前缀。

**Files:**
- Modify: `backend/src/main/java/com/selfblog/config/web/RateLimitProperties.java`

**Interfaces:**
- Consumes: 无
- Produces: 配置项 `siteAssetUpload`（默认 30 次/分钟），映射到前缀 `/api/admin/site-assets`

- [ ] **Step 1: 新增字段**

在 `RateLimitProperties.java` 中 `private int galleryUpload = 20;`（约第 147 行）之后插入：

```java
    /**
     * 站点品牌资产上传限流（次/分钟）。
     * 允许单文件 20MB，比相册上传更重，所以给得比 galleryUpload 宽松一点。
     */
    private int siteAssetUpload = 30;
```

- [ ] **Step 2: 注册到映射表**

在 `getAllLimits()` 里 `limits.put("/api/admin/albums/", galleryUpload);`（约第 182 行）之后插入：

```java
        limits.put("/api/admin/site-assets", siteAssetUpload);
```

- [ ] **Step 3: 验证**

```powershell
& $mvn test -Dskip.npm=true -Dskip.installnodenpm=true
```

Expected: 全量 386 + 本计划新增用例，0 失败。限流表在 `@SpringBootTest` 启动时就会被构造，配置写错会导致上下文起不来，因此全量测试本身就是有效的回归验证。

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/selfblog/config/web/RateLimitProperties.java
git commit -m "feat: rate limit the site-asset upload endpoint"
```

---

### Task 6: 把 `faviconUrl` / `siteLogoUrl` 收录为设置项

前端 `stores/site.ts` 已经在消费这两个键，但后端 `ALLOWED_KEYS` 没有它们，`defaults()` 也不返回，所以后台存不进去、公开接口也拿不到 —— 这一任务补上，前端那半边才有意义。

`validateValue` 里已有 `musicUrl` / `authorAvatar` 的 URL 形状校验，两个新键用同样的规则。

**Files:**
- Modify: `backend/src/main/java/com/selfblog/service/site/impl/SiteSettingsServiceImpl.java`
- Test: `backend/src/test/java/com/selfblog/service/site/impl/SiteSettingsServiceImplTest.java`

**Interfaces:**
- Consumes: `SiteSettingsService#updateSettings(Map)` / `getSettings()`（既有）
- Produces: 设置项 `faviconUrl`、`siteLogoUrl`（默认空串，值必须是空串、`http(s)://…` 或 `/uploads/…`）

- [ ] **Step 1: 写失败测试**

创建 `backend/src/test/java/com/selfblog/service/site/impl/SiteSettingsServiceImplTest.java`：

```java
package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.config.cache.CaffeineCacheConfig;
import com.selfblog.service.site.SiteSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SiteSettingsServiceImpl 集成测试。
 * 沿用本仓库 ServiceImplTest 的既有约定：真实 H2 + @Transactional 回滚。
 *
 * 注意：`getSettings()` 带 `@Cacheable`，而 **Caffeine 缓存不参与事务回滚**。
 * 若不清理，某个用例在事务里写进缓存的值会活到事务结束之后，让后面的用例
 * 读到脏数据（而且是否复现取决于用例执行顺序）。所以每个用例前强制清缓存。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SiteSettingsServiceImpl")
class SiteSettingsServiceImplTest {

    @Autowired
    private SiteSettingsService siteSettingsService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearSettingsCache() {
        Cache cache = cacheManager.getCache(CaffeineCacheConfig.SITE_SETTINGS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("brandAssetKeys_ArePresentInDefaults")
    void brandAssetKeys_ArePresentInDefaults() {
        Map<String, String> settings = siteSettingsService.getSettings();
        assertTrue(settings.containsKey("faviconUrl"));
        assertTrue(settings.containsKey("siteLogoUrl"));
        assertEquals("", settings.get("faviconUrl"));
        assertEquals("", settings.get("siteLogoUrl"));
    }

    @Test
    @DisplayName("updateSettings_AcceptsUploadedPaths")
    void updateSettings_AcceptsUploadedPaths() {
        Map<String, String> updated = siteSettingsService.updateSettings(Map.of(
                "faviconUrl", "/uploads/site-assets/favicon/abc.png",
                "siteLogoUrl", "/uploads/site-assets/logo/def.svg"));

        assertEquals("/uploads/site-assets/favicon/abc.png", updated.get("faviconUrl"));
        assertEquals("/uploads/site-assets/logo/def.svg", updated.get("siteLogoUrl"));
    }

    @Test
    @DisplayName("updateSettings_AcceptsAbsoluteHttpUrls")
    void updateSettings_AcceptsAbsoluteHttpUrls() {
        Map<String, String> updated = siteSettingsService.updateSettings(Map.of(
                "siteLogoUrl", "https://cdn.example.com/logo.svg"));

        assertEquals("https://cdn.example.com/logo.svg", updated.get("siteLogoUrl"));
    }

    @Test
    @DisplayName("updateSettings_AllowsClearingBackToEmpty")
    void updateSettings_AllowsClearingBackToEmpty() {
        siteSettingsService.updateSettings(Map.of("siteLogoUrl", "/uploads/site-assets/logo/def.svg"));

        Map<String, String> cleared = siteSettingsService.updateSettings(Map.of("siteLogoUrl", ""));
        assertEquals("", cleared.get("siteLogoUrl"));
    }

    @Test
    @DisplayName("updateSettings_RejectsRelativeGarbageForBrandAssets")
    void updateSettings_RejectsRelativeGarbageForBrandAssets() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("siteLogoUrl", "javascript:alert(1)")));
        assertEquals(400, e.getCode());
    }

    @Test
    @DisplayName("updateSettings_RejectsPathTraversalForBrandAssets")
    void updateSettings_RejectsPathTraversalForBrandAssets() {
        assertThrows(BusinessException.class,
                () -> siteSettingsService.updateSettings(Map.of("faviconUrl", "../../etc/passwd")));
    }

    @Test
    @DisplayName("updateSettings_StillAcceptsUnrelatedKeys")
    void updateSettings_StillAcceptsUnrelatedKeys() {
        Map<String, String> updated = siteSettingsService.updateSettings(Map.of("siteTitle", "新标题"));
        assertEquals("新标题", updated.get("siteTitle"));
        assertFalse(updated.isEmpty());
    }
}
```

- [ ] **Step 2: 跑测试，确认失败**

```powershell
& $mvn test -Dtest=SiteSettingsServiceImplTest -Dskip.npm=true -Dskip.installnodenpm=true
```

Expected: FAIL。`brandAssetKeys_ArePresentInDefaults` 因缺少 `faviconUrl` 失败；`updateSettings_AcceptsUploadedPaths` 因 `"包含不支持的设置项"` 抛异常失败。

- [ ] **Step 3: 收录两个键**

在 `SiteSettingsServiceImpl.java` 的 `ALLOWED_KEYS`（第 22–26 行）末尾追加两个键，改成：

```java
    private static final Set<String> ALLOWED_KEYS = Set.of(
            "siteTitle", "siteDescription", "authorName", "authorBio", "authorAvatar",
            "adminPath", "commentsEnabled", "visitorTrackingEnabled", "musicEnabled",
            "musicUrl", "musicTitle", "themeSkin", "themePrimaryColor", "footerText",
            "faviconUrl", "siteLogoUrl"
    );
```

- [ ] **Step 4: 补默认值**

在 `defaults()` 中，把 `values.put("authorAvatar", "");`（第 104 行）之后改成：

```java
        values.put("authorAvatar", "");
        values.put("faviconUrl", "");
        values.put("siteLogoUrl", "");
```

- [ ] **Step 5: 补校验规则**

把 `validateValue` 里的 URL 分支（第 145–149 行）从

```java
            case "musicUrl", "authorAvatar" -> {
```

改成

```java
            case "musicUrl", "authorAvatar", "faviconUrl", "siteLogoUrl" -> {
```

分支体内逻辑不变。

- [ ] **Step 6: 跑测试确认通过**

```powershell
& $mvn test -Dtest=SiteSettingsServiceImplTest -Dskip.npm=true -Dskip.installnodenpm=true
```

Expected: `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 7: 提交**

```bash
git add backend/src/main/java/com/selfblog/service/site/impl/SiteSettingsServiceImpl.java backend/src/test/java/com/selfblog/service/site/impl/SiteSettingsServiceImplTest.java
git commit -m "feat: register faviconUrl and siteLogoUrl as site settings"
```

---

### Task 7: 全量校验与文档

- [ ] **Step 1: 全量测试**

```powershell
& $mvn test -Dskip.npm=true -Dskip.installnodenpm=true
```

Expected: `Tests run: 386 + 新增`，`Failures: 0, Errors: 0`，`BUILD SUCCESS`。

- [ ] **Step 2: 确认没有多余产物**

```powershell
git status --porcelain
```

Expected: 只有本计划预期的 Java 文件改动；**不应出现 `backend/uploads/site-assets`** 或任何 `.png` / `.mp3` 之类被误提交的文件。

- [ ] **Step 3: 更新 `CHANGELOG.md`**

按仓库既有格式（中文，`新增` / `说明` / `影响文件` 分节）在文件顶部新增一节 `## 未发布 — 资源上传通道（后端）(2026-10-15)`，内容需包含：

- **新增**：`POST /api/admin/site-assets`（`kind` ∈ `logo` / `favicon` / `avatar` / `music`）；三道校验（扩展名白名单、按种类分档的体积上限、魔数嗅探）；SVG 净化；限流 30 次/分钟；设置项 `faviconUrl` / `siteLogoUrl`。
- **说明**：
  - **为什么不复用 `MediaService`** —— 那条通道用 thumbnailator 把所有图片重新编码成 `.jpg`，`.ico`、`.svg`、音频都过不去；favicon 需要 `.ico`、logo 需要 `.svg`。
  - **为什么不用建表** —— `site_setting` 是 `setting_key` / `setting_value` 键值表，新增设置项只是多一行数据，`schema.sql`、`schema-sqlite.sql`、`src/test/resources/schema.sql` 三处都不需要改。
  - **SVG 净化的定位是纵深防御** —— 所有渲染点都是图片上下文（`<img src>`、`<link rel="icon">`、`<audio src>`），脚本在那里不会执行；净化是为了防止将来有人把 logo 直接注入 HTML。
  - **`.ico` 同时接受 ICO 与 PNG 两种文件头** —— 不少工具导出的 `favicon.ico` 实际是 PNG。
  - **前端仍需下一批次接入** —— 本次只到接口与设置项为止，后台「站点设置」页还没有上传入口。
- **影响文件**：把本计划文件清单里的 11 个文件逐条列出并写明改动类型。

- [ ] **Step 4: 检查 `backend/README.md` 是否需要更新**

```powershell
Select-String -Path backend\README.md -Pattern 'site-settings|media/upload|接口|API' | Select-Object -First 15
```

如果 README 里维护了管理端接口清单，把 `POST /admin/site-assets` 加进去；如果它没有这类清单，**不要**为了本计划硬造一节，跳过并在汇报里说明。

- [ ] **Step 5: 提交**

```bash
git add CHANGELOG.md backend/README.md
git commit -m "docs: log the backend site-asset upload channel"
```

---

## 完成后

后端这一批结束时应当具备：

- `POST /api/admin/site-assets` 可用，`kind` 决定校验规则与落盘目录
- 上传结果可通过 `/uploads/site-assets/<kind>/<uuid>.<ext>` 直接访问（由 `WebMvcConfig` 既有的 `/uploads/**` 映射提供）
- `faviconUrl` / `siteLogoUrl` 可通过 `PUT /api/admin/site-settings` 保存，并通过公开接口 `GET /api/site/public` 下发

**手动验证（可选，需要真实起服务）**：`mvn spring-boot:run` 依赖 `.env` 里的 `JWT_SECRET` 与 `ADMIN_PASSWORD`，且会带上前端构建。若要做端到端验证，用 `multipart` 发一个 `kind=favicon` 的 PNG，确认返回的 URL 能 `GET` 到 200。注意 AGENTS.md 记录的约定：copy `.env.example` → `.env`，值不要加引号。

**下一批次（前端）**：`frontend/src/api/site.ts` 增加 `uploadSiteAsset()`，新增 `SiteAssetField.vue`（保留手动填 URL 的能力 + 上传按钮 + 预览），并在 `frontend/src/views/admin/Settings.vue` 接入四个字段。

## 执行修订

### Task 3（2026-10-15）

- 实际测试数为 **40**：按原计划 39 个用例外，代码审查发现 SVG 中带命名空间前缀的 `<x:script>` 能绕过按 CSS 标签名删除的逻辑，新增 `store_StripsNamespacePrefixedScriptFromSvg` 回归用例。该测试先失败、修改后通过。
- Jsoup 1.18.3 的 `Element` 没有 `removeAttribute(String)`；实现改用其真实 API `removeAttr(String)`。原计划示例代码中的该调用无法编译。
- AAC 测试夹具的字面量 `0x80` 超出 Java `byte` 的正值范围，需写成 `(byte) 0x80`；否则测试源码无法编译。
- SVG 危险元素现在按元素名的本地部分匹配，因此无前缀和命名空间前缀形式都会移除；事件属性也按本地属性名判断。解析仍使用 Jsoup XML parser，保留 `linearGradient` / `viewBox` 大小写。
- 验证：`SiteAssetServiceImplTest` **40/40 通过**；检查确认测试没有创建 `backend/uploads/site-assets`。
