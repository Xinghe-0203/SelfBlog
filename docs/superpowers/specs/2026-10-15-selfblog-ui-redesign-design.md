# SelfBlog 全站视觉重设计 + 资源上传 + 阅读体验增强

- 日期：2026-10-15
- 状态：待评审
- 作者：opencode 会话

---

## 1. 背景与目标

用户提出四类需求：更大更圆润的视觉语言（大 R 角、胶囊选中态）、DeepSeek 风格的滚动导航栏、站点图标/头像/背景音乐全部支持后台上传、以及从 GitHub 同类项目借鉴阅读体验能力。

调查后的关键判断：这不是一个"改样式"任务，而是 5 个可独立上线但共享设计系统的子系统。本 spec 一次性覆盖全部 5 个，但实现顺序按 §8 拆成可独立验证的阶段。

### 已核实的现状（决定了设计约束）

| 发现 | 证据 | 对设计的影响 |
| --- | --- | --- |
| `site_setting` 是 KV 表 | `schema-sqlite.sql:492` `setting_key TEXT PRIMARY KEY, setting_value TEXT` | 新增设置键**零 schema 迁移** |
| 圆角 token 已存在但组件大面积硬编码 | `variables.css:279-288` 定义 scale；`Navbar.vue:114` 写死 `16px`，`PostDetail.vue:445` 写死 `13px` | 必须先收敛到 token 再放大 |
| 媒体上传只收图片/视频，且图片被重编码为 `.jpg` | `MediaServiceImpl.java:73-81` MIME 白名单；`:279` `newFilename = UUID + ".jpg"` + Thumbnailator | `.mp3`/`.svg`/`.ico` 现在传不上来，扩展 `type` 分支无解，必须新建通道 |
| `/api/admin/**` 已有全局 admin 规则 | `SecurityConfig.java:165` | 新增后台上传接口不需要改安全配置 |
| `/uploads/**` 已 permitAll | `SecurityConfig.java:161` | 上传产物可直接外链 |
| favicon 写死在静态 `index.html` | `index.html:12` `<link rel="icon" href="/favicon.svg">` | 需要运行时注入机制 |
| 阅读体验已有大半 | `PostDetail.vue` 已含 `extractToc`/`readingProgress`/`estimatedReadMinutes`/相邻文章；`App.vue:21` 已有 `BackToTop` | 只补缺的，不重写 |
| `.app-main { padding-top: 80px }` 全局写死 | `App.vue:111` | 导航栏收缩不得推挤内容，否则 `scroll-margin-top:105px` 锚点全部失效 |
| Docker 构建路径已损坏 | `Dockerfile:25,45` 期望 `backend-0.0.1-SNAPSHOT.jar`，而 `pom.xml:196` `finalName=selfblog`；且只 COPY `pom.xml`+`src`，`frontend-maven-plugin` 的 `workingDirectory` 不存在 | **不能用 docker-compose 验证本次改动**，见 §9 |

---

## 2. 总体设计

五个子系统，一条主线：**先把设计系统（token + 图标）做实，再让五个子系统都挂在上面。**

```
共享底座（先做，阶段 A）
├── variables.css 圆角 scale + 语义 token
├── Icon.vue 内置 SVG 图标集
└── useScrollState / useMarkdownEnhance 两个 composable
        │
        ├── 阶段 B  公开页滚动导航栏（Navbar.vue）
        ├── 阶段 C  后台侧边栏胶囊化 + 图标替换（AdminLayout.vue）
        ├── 阶段 D  资源上传（后端新通道 + Settings.vue 上传组件）
        ├── 阶段 E  阅读体验（PostDetail.vue）
        └── 阶段 F  圆角 token 全站扫描替换 + 动效收尾
```

设计原则：

1. **不新增任何运行时依赖。** 前端当前只有 7 个 runtime 依赖（vue/vue-router/pinia/axios/marked/dompurify + devDeps），保持这个风格。图标用手写 SVG 注册表，不引 lucide 之类。
2. **不动 `MediaServiceImpl`。** 它是文章/头像/相册链路，图片重编码是那套流程的有意行为。新功能走独立服务类。
3. **不动文章发布流**（`status=1` 直发）和现有 22 张表结构。
4. **布局高度守恒。** 导航栏的滚动变化只改视觉（模糊、底色、内边距、阴影），不改它占据的布局高度。

---

## 3. 子系统设计

### 3.1 圆角 scale 与胶囊选中态

`variables.css` 的 `--radius-*` 全线上移一档：

```css
--radius-xs: 6px;  --radius-sm: 8px;  --radius: 12px;  --radius-md: 12px;
--radius-lg: 16px; --radius-xl: 20px; --radius-2xl: 24px; --radius-3xl: 32px;
```

`--radius-full`（现值 `9999px`）保持不动，胶囊选态和圆形头像都复用它——同一个值同时覆盖"小方块变圆"和"胶囊"，不必为相同数值再开新 token。

**胶囊选中态**（公开页导航 + 后台侧边栏同一套规则）：

```css
a.active {
  background: var(--primary-light);
  color: var(--primary);
  border-radius: var(--radius-full);
  padding: 6px 14px;
}
a:hover:not(.active) { background: var(--bg-secondary); border-radius: var(--radius-full); }
```

未选中态无背景，只有 hover 出现浅底——避免导航条变成一排实心色块。

**范围**：`Navbar.vue`、`Home.vue`、`PostDetail.vue`、`Archives.vue`、`About.vue`、`gallery/Gallery.vue`、`search/Search.vue`、`Footer.vue`、`components/common/*`、`AdminLayout.vue`、`Settings.vue` 及 `styles/components.css` 中的裸圆角值。**只替换 `border-radius: <裸值>`，不动 `50%`（圆形头像）和 `999px`（已是胶囊语义）**，避免大范围无意义 diff。

### 3.2 滚动导航栏（DeepSeek 风格）

新增 `frontend/src/composables/useScrollState.ts`：

```ts
export function useScrollState(threshold = 16) {
  const scrolled = ref(false)      // scrollY > threshold
  const direction = ref<'up' | 'down'>('up')
  // rAF 节流 + passive 监听；prefers-reduced-motion 下仍更新状态（纯 CSS 过渡由媒体查询关掉）
}
```

`Navbar.vue` 状态机（三态，全部由 `.is-scrolled` / `.is-hidden` class 驱动，不直接操作样式）：

| 状态 | 触发 | 表现 |
| --- | --- | --- |
| 静止 | `scrollY <= 16` | 胶囊高 `58px`，`--surface-primary` 实底，轻阴影 |
| 已滚动 | `scrollY > 16` | 高降至 `48px`，`backdrop-filter: blur(20px) saturate(1.4)`，底色走已有 token `--navbar-bg`，阴影加深，导航项间距收缩 |
| 隐藏（仅 ≤760px） | 已滚动且 `direction=down` | `transform: translateY(-120%)` |
| 归位 | `direction=up` | 立即滑回 |

**硬约束**：胶囊容器 `position: fixed; top: 14px` 不变，`padding: 0 20px` 不变，`.app-main { padding-top: 80px }` **不改**。收缩是向上缩（高度变小、顶边固定），内容零位移——`PostDetail.vue:540` 的 `scroll-margin-top:105px` 和 `:641` 的 `.article-aside{top:105px}` 因此继续有效。同时给 `html` 补 `scroll-padding-top: 96px`，让浏览器原生锚点跳转也不被导航栏盖住。

### 3.3 图标系统

新建 `frontend/src/components/common/Icon.vue`：

- 图标注册表 `icons.ts` 导出 `Record<string, string>`（`name → SVG path/片段`，统一 `viewBox="0 0 24 24"`，`fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"`，与 `Navbar.vue:186-195` 现有 svg 样式一致）
- `<Icon name="search" :size="17" />`，`currentColor` 驱动 → 深色模式自动适配，无需两套
- 首批 21 个：`logo, search, sun, moon, menu, close, home, archive, image, info, settings, gauge, file-text, tag, users, message, music, play, pause, arrow-up, external`（`logo` 是 brandMark 的兜底图标）

替换现有文字符号图标：`Navbar.vue`（search/theme/menu）、`AdminLayout.vue:17-36`（`⌂ ▤ ⌑ ◷ ▱ ▧ ⚙`）、`MusicPlayer.vue:5`（`Ⅱ ▶`）、`BackToTop.vue`、`Footer.vue`、`components/common/*` 中的装饰符号。

品牌资产与 UI 图标分开：UI 图标永远用内置 SVG；**站点 logo / favicon / 头像走用户上传**（§3.4）。

两个品牌资产的渲染位置在本 spec 内固定，避免实现时分叉：

- `siteLogoUrl` → 公开页 `Navbar.vue` 的 `.brand-mark`（替换现有 `✳` 文字符号）和 `AdminLayout.vue` 的 `.sidebar-brand .brand-mark`
- `faviconUrl` → 浏览器标签页图标，只在 `site.ts` 的 `applySettings()` 里做 DOM 注入

两者都没有上传值时，分别回落现有默认：brand-mark 回落内置 `Icon name="logo"`，favicon 回落 `index.html` 的静态 `/favicon.svg`。

### 3.4 资源上传（后端新通道 + 前端上传组件）

#### 后端

新文件：

- `controller/admin/AdminSiteAssetController.java`
- `service/site/SiteAssetService.java` + `service/site/impl/SiteAssetServiceImpl.java`

接口：`POST /api/admin/site-assets`，`multipart/form-data`，两个参数：`file`、`kind`。用 `@PreAuthorize("hasRole('admin')")`（与同目录 `AdminSiteSettingsController.java:17` 一致），路径天然落进 `SecurityConfig.java:165` 的 `/api/admin/**` 规则，**不需要改 `SecurityConfig`**。

`kind ∈ { logo, favicon, avatar, music }`，各自规则：

| kind | 扩展名白名单 | 大小上限 | 落盘位置 |
| --- | --- | --- | --- |
| `logo` / `avatar` | `.png`, `.jpg`, `.jpeg`, `.webp`, `.svg` | 2 MB | `uploads/site-assets/<kind>/<uuid>.<ext>` |
| `favicon` | `.png`, `.ico`, `.svg` | 2 MB | `uploads/site-assets/favicon/<uuid>.<ext>` |
| `music` | `.mp3`, `.ogg`, `.wav`, `.m4a`, `.aac` | 20 MB | `uploads/site-assets/music/<uuid>.<ext>` |

**原样字节落盘，不过 Thumbnailator** —— 这正是新通道存在的全部理由。

三重校验（缺一不可）：

1. **扩展名白名单**（防止 `.php`/`.html` 等）
2. **magic number 嗅探**，不信客户端 `Content-Type`：
   - 图片：PNG `89 50 4E 47`、JPEG `FF D8 FF`、WEBP `RIFF....WEBP`、ICO `00 00 01 00`（favicon `.ico` 同时接受 PNG 文件头）、SVG XML 中的 `<svg`
   - 音频：MP3/AAC 的 ID3 或 MPEG/ADTS 帧同步、Ogg `4F 67 67 53`、WAV `52 49 46 46`+`57 41 56 45`、M4A `ftyp` box
   - 嗅探与扩展名不一致 → 400
3. **大小上限**

**SVG 净化是必要的安全边界**：`/uploads/**` 是公开地址，用户可以直接打开上传的 SVG 作为顶层文档，在那里脚本可能以本站源执行。不能只依赖 `<img src>` / `<link rel="icon">` 等图片上下文对脚本的额外限制。上传前使用 Jsoup XML parser 保留 SVG 大小写；删除 `script` / `foreignObject` / `iframe` / `embed` / `object`（按本地名匹配，也覆盖 XML namespace 前缀）、事件属性，以及包含 `javascript:` 或 `data:text/html` 的属性值。保留常见 SVG 结构（例如 `<use href="#symbol">`），避免破坏由本地 symbol 组成的品牌图形。

返回 `{ "url": "/uploads/site-assets/favicon/<uuid>.svg", "kind": "favicon", "size": 1234, "originalName": "favicon.svg" }`。

**上传产物不做删除**：`updateSettings` 只改 KV 值，旧文件留在磁盘。理由：按 URL 反推文件路径删除有目录穿越面，且站点资源体积极小。这是有意取舍，写入 spec 备案。

#### 设置键（零迁移）

`SiteSettingsServiceImpl` 改三处：

- `ALLOWED_KEYS` 增补 `faviconUrl`、`siteLogoUrl`（`:22-26`）
- `defaults()` 增补两个空串（`:98-115`）
- `validateValue()` 增补分支：`faviconUrl`、`siteLogoUrl` 复用 `:145-149` 已有的 `musicUrl/authorAvatar` 规则（必须 `http(s)://` 或 `/uploads/`）

**不需要改任何 schema 文件**——`site_setting` 是 KV 表。

#### 前端

在现有 `src/api/site.ts` 的 `siteApi` 中增加站点资源上传方法（不新增 API 模块）：

```ts
type SiteAssetKind = 'logo' | 'favicon' | 'avatar' | 'music'
interface SiteAssetUpload {
  url: string
  kind: SiteAssetKind
  size: number
  originalName: string
}
uploadSiteAsset(
  kind: SiteAssetKind,
  file: File,
  onProgress?: (event: AxiosProgressEvent) => void
): Promise<ApiResponse<SiteAssetUpload>>
```

（沿用 `src/api/` 的 Axios 封装；拦截器返回的是业务 envelope，`response.data` 才是 payload。）上传结果字段为 `url`、`kind`、`size`、`originalName`。

`src/stores/site.ts`：

- `SiteSettings` 接口与 `defaults` 增补 `faviconUrl`、`siteLogoUrl`
- `applySettings()` 增补 favicon 运行时注入：`faviconUrl` 变化时，移除 `document.head` 中全部 `link[rel~="icon"]` / `link[rel="apple-touch-icon"]` / `link[rel="shortcut icon"]`，按扩展名推导 `type`（`svg→image/svg+xml`、`ico→image/x-icon`、其余 `image/png`）重建节点。`faviconUrl` 为空时移除自定义节点，回落到 `index.html:12` 的静态兜底。
- 注意：`main.ts:41-46` 是先 `loadPublicSettings()` 再 `app.use(router)`，favicon 注入发生在首次路由前，无闪烁。

新增通用组件 `src/components/admin/SiteAssetField.vue`，并在 `src/views/admin/Settings.vue` 接入四个字段（头像 / 站点 Logo / 网站图标 / 背景音乐）：

- 每个字段 = 上传按钮 + 即时预览 + 原 URL 输入框（保留远端 CDN 手填能力）
- `accept` 属性按 kind 限定；前端预检大小，超限直接拒绝，不走网络
- 上传成功 → 写入表单字段 → 同步 `siteStore.applySettings()`（导航栏 logo、favicon 立即换新）→ 用户点"保存设置"才真正落库
- 失败在字段下方显示行内错误，不用 toast（保留表单其他已填内容）
- 图标类预览方形、音乐类显示文件名 + 时长提示

#### 数据流

```
Settings.vue ──POST /api/admin/site-assets──▶ AdminSiteAssetController
                                                 └─ SiteAssetService: 校验三连 → 写 uploads/site-assets/<kind>/<uuid>.<ext>
                                                 └─ 返回 /uploads/... 相对路径
Settings.vue ◀── { url, kind, size, originalName }
   └─ form.faviconUrl / siteLogoUrl / authorAvatar / musicUrl = url
   └─ siteStore.applySettings()          ← 乐观更新，导航栏 logo + favicon 立即生效
   └─ 用户点击「保存设置」→ PUT /api/admin/site-settings
         └─ SiteSettingsServiceImpl.updateSettings: ALLOWED_KEYS 门禁 + validateValue + 写 KV

访客侧：main.ts bootstrap() → loadPublicSettings() → GET /api/site/public → applySettings()
```

### 3.5 阅读体验增强

`PostDetail.vue` 已有（**不动**）：目录提取 `:201-208`、阅读进度 `:233-244`、预计阅读时长 `:58-61`、上一篇/下一篇 `:127-136`、SEO meta + JSON-LD、图片 `loading="lazy"`、`BackToTop`。

新增（按 GitHub 同类项目 butterfly / theme-hope 的通用做法）：

| 能力 | 实现要点 |
| --- | --- |
| 代码块头部 | marked 自定义 renderer 给 `pre` 包一层 `<div class="code-block">` + `<span class="code-lang">`；**复制按钮在 `v-html` 之后用 DOM 后处理追加成真实 button**——不能写进 `v-html`（Vue 不编译，DOMPurify 也会剥） |
| 长代码折叠 | DOM 后处理时判断 `scrollHeight > 320` → 加 `.is-collapsed` + "展开代码"按钮 |
| 标题锚点 | DOM 后处理给每个标题追加 `<a class="heading-anchor">`，点击写 `location.hash`；依赖 §3.2 的 `scroll-padding-top` 才能不被导航栏盖住 |
| 移动端目录 | 现状是文章顶部一个 `max-height:180px` 的小块（`:715-726`），体验差。改为左下浮动胶囊按钮 + 底部抽屉，复用同一份 `toc` 数据 |
| 滚动监听 | `updateReadingProgress` 现在每次 scroll 遍历全部标题（`:238-243`），长文是热点。标题高亮改 `IntersectionObserver`；阅读进度保留 rAF 节流（每帧最多一次，成本可忽略） |
| 正文图片灯箱 | 复用现成 `components/common/ImagePreview.vue`（props 就是 `images`/`show`/`initialIndex`）。DOM 后处理时收集正文图 URL，点击打开 |
| 字数统计 | 从 `post.content`（Markdown 源码）统计：先去代码块、再去 Markdown 标记（`#`、`*`、`` ` ``、`[]()`、`![]()`），再按"中日韩字符按字 + 西文按词"分别计数后相加。替换现有的 `estimatedReadMinutes` 粗算（现在按 `content.length/400`，把 Markdown 语法和中英文混排都算错了） |
| 最后更新提示 | `updateTime` 存在且晚于 `createTime` 7 天以上时，在 meta 行后显示"最后更新于 …" |

新增 `src/composables/useMarkdownEnhance.ts` 集中承载所有 DOM 后处理（代码块、锚点、折叠、灯箱收集），`PostDetail.vue` 只负责调用与状态。保持单文件职责。

---

## 4. 组件与文件清单

### 新增

| 文件 | 职责 |
| --- | --- |
| `frontend/src/composables/useScrollState.ts` | 滚动状态（scrolled/direction），rAF 节流 |
| `frontend/src/composables/useMarkdownEnhance.ts` | 正文 DOM 后处理：代码头部/复制/折叠、标题锚点、图片灯箱收集 |
| `frontend/src/components/common/Icon.vue` | 图标渲染组件 |
| `frontend/src/components/common/icons.ts` | 内置 SVG 注册表 |
| `frontend/src/components/post/MobileTocSheet.vue` | 移动端目录抽屉 |
| `frontend/src/components/admin/SiteAssetField.vue` | 通用站点资源上传字段、预览与手动 URL 输入 |
| `backend/.../controller/admin/AdminSiteAssetController.java` | 上传接口 |
| `backend/.../service/site/SiteAssetService.java` | 上传服务接口 |
| `backend/.../service/site/impl/SiteAssetServiceImpl.java` | 校验 + magic 嗅探 + SVG 净化 + 落盘 |
| `backend/src/test/java/com/selfblog/controller/AdminSiteAssetControllerTest.java` | 上传接口测试 |

### 修改

| 文件 | 改动 |
| --- | --- |
| `frontend/src/styles/variables.css` | 圆角 scale 上移一档（不改 `--radius-full`） |
| `frontend/src/styles/base.css` | `html { scroll-padding-top }` |
| `frontend/src/components/layout/Navbar.vue` | 滚动三态、图标、胶囊选中、品牌 logo 用 `siteLogoUrl` |
| `frontend/src/components/layout/MusicPlayer.vue` | 图标 + 大 R 角重设计 |
| `frontend/src/components/common/BackToTop.vue` | 换 Icon 组件 + radius token |
| `frontend/src/api/site.ts` | 增加站点资源上传 API |
| `frontend/src/views/admin/AdminLayout.vue` | 侧边栏图标 + 胶囊选中 |
| `frontend/src/views/admin/Settings.vue` | 四个上传字段 + 预览 |
| `frontend/src/stores/site.ts` | 两个新键 + favicon 运行时注入 |
| `frontend/src/views/post/PostDetail.vue` | §3.5 全部 + 移动端目录 + 灯箱 |
| `frontend/src/App.vue` | 挂载 MobileTocSheet（仅文章页） |
| `frontend/index.html` | 静态 favicon 保留作兜底，补注释说明运行时注入 |
| `backend/.../service/site/impl/SiteSettingsServiceImpl.java` | ALLOWED_KEYS / defaults / validateValue |
| 其余 `views/**`、`components/common/**`、`styles/components.css` | 裸圆角 → token |

**不需要改**：`schema.sql` / `schema-sqlite.sql` / `src/test/resources/schema.sql`（KV 表，新键零迁移）、`SecurityConfig.java`、`MediaServiceImpl.java`、`pom.xml`、`package.json`。

---

## 5. 错误处理

| 场景 | 行为 |
| --- | --- |
| `kind` 不在白名单 | `BusinessException(400, "不支持的资源类型")` |
| `kind` 不支持 | `BusinessException(400, "不支持的资源类型")` |
| 文件为空 | `BusinessException(400, "请选择要上传的文件")` |
| 扩展名不在当前 kind 白名单 | `BusinessException(400, "仅支持 … 格式")`；服务端不以客户端 MIME 为准 |
| magic number 与扩展名不一致 | `BusinessException(400, "文件内容与扩展名不匹配，请上传真实文件")` |
| 超过大小上限 | `BusinessException(400, "文件大小不能超过 NMB")` |
| 非 admin 调用 | 既有安全链路拒绝；MVC 测试中普通用户为 403 |
| SVG 不含 `<svg>` 元素 | `BusinessException(400, "SVG 文件缺少 <svg> 根元素")` |
| 落盘 IO 失败 | `BusinessException(500, "文件保存失败: …")` |
| 前端预检失败（扩展名/大小不合规） | 字段下方行内错误，不发请求；`accept` 仅作文件选择提示，服务端仍独立校验 |
| 上传成功但设置保存失败 | 上传的 URL 已写入表单，用户重试保存即可；不影响已保存的旧值 |
| `faviconUrl` 为空 | 移除自定义 link，回落静态兜底 |
| 上传的 URL 404（文件被外部删除） | 浏览器自然破图；不做前端探测（避免额外请求） |

---

## 6. 测试策略

### 后端

后端分层测试覆盖：

- `SiteAssetKindTest`：kind、各自扩展名和大小规则；`SiteAssetServiceImplTest`：白名单、图片/音乐大小上限、magic number、写入随机 URL、SVG 净化（包括 namespace 前缀脚本）与普通 SVG 大小写保留。
- `AdminSiteAssetControllerTest`：管理员成功、kind 转发、服务业务错误、缺少参数、普通用户 403。
- `SiteSettingsServiceImplTest`：`faviconUrl` / `siteLogoUrl` 默认值、可保存 URL 与 `javascript:` 等非法值拒绝；每个测试前清空站点设置缓存。

配合完整回归：`mvn test`（H2 + test schema，`DB_TYPE` 保持不填）。

### 前端

新增单测：`useScrollState.test.ts`（阈值/方向/rAF）、`site.test.ts`（favicon link 增删与 type 推导）、`useMarkdownEnhance.test.ts`（代码头部注入、长文折叠、锚点）。

站点资源上传批次还应覆盖：`site.ts` 上传方法的 FormData 字段与进度回调；`SiteAssetField.test.ts` 的扩展名/大小预检、手动 URL 编辑、预览、上传成功和行内错误；Settings 集成中四个字段到对应设置键的绑定，以及上传后即时应用、保存后持久化。

既有测试必须保持通过。全量校验：

```sh
cd frontend && npm run lint:check && npm run type-check && npm run test:run && npm run build
```

### 手动验证（dev 模式）

1. 起 backend（`mvn spring-boot:run`）+ frontend（`npm run dev`）
2. 滚动验证导航栏三态；375px 宽验证自动隐藏/归位
3. 后台上传四类资源，验证即时预览 + 保存 + 刷新后仍在
4. 访客侧验证 favicon 已替换、导航栏 logo 已替换
5. 浅色/深色两套主题各过一遍；`prefers-reduced-motion` 开/关各一遍
6. 文章页验证代码复制、长代码折叠、锚点跳转不被导航栏盖住、移动端目录抽屉、图片灯箱
7. 现有 `mvn test` 与 `npm run test:run` 全绿

---

## 7. 已知风险与取舍

1. **圆角扫描面大**，`views/**` 有 20+ 文件。风险是深色模式下大面积视觉回归。缓解：先只改 token（阶段 A），全站目视确认后再做逐文件替换；每完成一个 view 单独看一次。
2. **`.app-main { padding-top: 80px }` 写死**。本设计用"视觉收缩、布局高度守恒"绕开，但这是 brittle 的——若将来导航栏真的要变占位高度，`PostDetail` 的 `105px` 常量必须同步。已在 §3.2 记为硬约束。
3. **上传产物不删除**，磁盘只增不减。站点资源体积极小（图标 KB 级、音乐 MB 级），换一次主题攒几个文件可忽略，换取的是零目录穿越面。
4. **SVG 可被直接作为顶层文档打开**。因 `/uploads/**` 公开，SVG 净化是必要安全边界；当前 `<img>` / `<link rel=icon>` 图像上下文的脚本限制只是额外防护，不能取代服务端净化。
5. **单 JAR 模式需要重新构建**：`pom.xml:202` 把 `frontend/dist` 打进 JAR 的 `static`，前端改动不跑到 `target` 里就不生效。
6. **本 spec 未覆盖 Docker 路径**（见 §9）。

---

## 8. 实现顺序（可独立验证）

| 阶段 | 内容 | 独立验证点 |
| --- | --- | --- |
| A | token scale + `Icon.vue` + 两个 composable | 单测绿，页面无回归 |
| B | `Navbar.vue` 滚动三态 + 图标 + 胶囊 | 公开页滚动态目视 |
| C | `AdminLayout.vue` 侧边栏 | 后台目视 |
| D1 | 后端上传通道（已完成） | 分层后端测试 + 全量 Maven 回归 |
| D2 | `Settings.vue` 四个资源字段与上传控件 | API/组件测试 + 管理员页手动上传并保存四种 kind |
| E | `PostDetail.vue` 阅读体验 | 文章页目视 + 单测 |
| F | 全站圆角 token 替换 + 动效 + 深色模式收尾 | 全站目视 + `npm run build` |

每阶段独立可回滚，不要求一次全上。

---

## 9. 范围外 / 已知损坏

- **Dockerfile 与 docker-compose 路径已损坏，本次不使用它们验证**。`Dockerfile:45` 期望 `backend-0.0.1-SNAPSHOT.jar`，实际 `pom.xml:196` 的 `finalName` 是 `selfblog`；且 Docker 构建只 COPY `pom.xml` 和 `src`，`frontend-maven-plugin`（`pom.xml:214` `workingDirectory=${project.basedir}/../frontend`）在镜像内根本找不到前端目录，`mvn package` 会失败。修复它属于独立任务，不在本 spec 内。
- 不改文章发布审核流（`status=1` 直发）。
- 不改 22 张既有表结构。
- 不新增运行时依赖。
- 不做通知/私信/关注等社交功能的视觉重设计（本轮只覆盖公开阅读路径 + 后台设置/布局）。
