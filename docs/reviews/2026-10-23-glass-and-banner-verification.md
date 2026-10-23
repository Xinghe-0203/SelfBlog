# 页面横幅自定义 + 玻璃感默认开启 + 后台信息架构重排 — 真机验证记录 (2026-10-23)

对应改动：`backend/.../dto/site/PageModels.java`（`PageStyle.banner`）、`backend/.../service/site/page/PageCatalog.java`（横幅校验）、
`backend/.../service/site/seo/ReadingContent.java` + `backend/src/main/resources/static/seo-reading.css`（服务端阅读 HTML）、
`backend/.../service/site/impl/SiteSettingsServiceImpl.java`（`glassMode` + 新默认值 + 三套新皮肤白名单）、
`frontend/src/stores/site.ts`（`effectiveSurfaceOpacities` 玻璃感契约、`buildBackgroundVars`）、
`frontend/src/styles/{appearance,skins,typography}.css`、`frontend/src/composables/usePageConfig.ts`、
`frontend/src/views/{Home,About}.vue`、`frontend/src/views/admin/{Settings,Seo,Dashboard}.vue`、
`frontend/src/views/admin/pages/PageEditor.vue`、`frontend/src/components/admin/LlmConnectionPanel.vue`、
`frontend/src/constants/themePresets.ts`。
CHANGELOG 条目见 `CHANGELOG.md` 的「未发布 — 页面横幅自定义 + 玻璃感默认开启 + 后台信息架构重排 (2026-10-23)」。

用户诉求（m00009）：「当前项目前端很难看，1位置请在后台可以设置背景图片和大小和内容。2位置你看看，一点半透明效果没有。
3位置也要有可以设置背景图片和大小内容。工作台这里的SEO就没必要了，刚刚我和你说的那些自定义可以放在这种页面里。
网站设置里面的内容太多了太杂乱了，请仔细整理。如果你觉得这种做前端不合适你也可以采用别的前端框架之类的，我只要你能够做好前端。」

## 为什么必须真机验证

这四项里有两项**单元测试与静态检查都抓不到**：

1. 「一点半透明效果没有」的根因是 `appearance.css` 里一条 `html:not([data-wallpaper])` 规则把三档表面压成同值，
   外加 `effectiveSurfaceOpacities()` 在无壁纸时把三档算成同一个数 —— 只有读 computed style 的 `backgroundColor` 才能看出三档是不是真的分开了；
2. 横幅是 `PageStyle` 的新字段，要穿过「后台控件 → 草稿 → 发布 → 公开 store → Vue 渲染 → 服务端阅读 HTML」六段，
   任何一段漏接都会静默降级成「没有横幅」而不是报错。

## 方法与工具链

- **浏览器**：本机 Chrome（`C:\Program Files\Google\Chrome\Application\chrome.exe`），经全局 Playwright 1.59.1
  （`C:\Users\Zora\AppData\Roaming\npm\node_modules\playwright`）以 headless 驱动；视口 **1440×1000**。
- **前后端**：Vite dev `http://localhost:3000`（**必须是 3000**，后端 `CORS_ALLOWED_ORIGINS` 只放行 3000 与 8825，
  起在 3010 会让全部 `/api` 请求 403 `Invalid CORS request`）+ 后端 `java -jar backend/target/selfblog.jar`（`:8825`）。
- **登录**：`POST /api/user/login`，字段是 `username`（**不是 `account`**），响应 `data.token`。
- **播种**：站点设置与页面配置全部经管理端 HTTP API 写入（`PUT /api/admin/site-settings`、
  `PUT /api/admin/pages/{id}/draft` + `PUT /api/admin/pages/{id}/publication`），**从未手改 `campus_blog.db`**；
  验证前把作者原设置备份到 `.tmp-gate/settings-backup.json`、原页面配置备份到 `.tmp-gate/page-config-backup.json`，
  验证后逐一还原并核对 drift。
- **探针脚本**（`.tmp-gate/`，临时目录，不随仓库提交）：`shot.cjs`、`shot-wallpaper.cjs`、`shot-admin.cjs`、
  `verify-defaults.cjs`、`verify-dark.cjs`、`verify-banner.cjs`、`restore-settings.cjs`、`probe-seo-page.cjs`。
  必须 CommonJS（`.cjs`）+ `require()` 绝对路径的 playwright；ESM import 绝对 Windows 路径会报 `ERR_UNSUPPORTED_ESM_URL_SCHEME`。
- 持久化截图见 `docs/reviews/assets/2026-10-23-*.png`（8 张）。

## 1. 结论

| 诉求 | 根因 | 验证手段 | 结论 |
|---|---|---|---|
| ① 首页介绍区可设背景图 / 尺寸 / 内容 | `.intro-shell` 的背景图写死为「第一篇文章封面」、宽度写死 43%、文案写死取站点资料 | 写入 split 横幅后读 DOM class + CSS 变量 + 截图 | **通过** |
| ② 卡片要半透明 | `appearance.css` 的 `html:not([data-wallpaper])` 把三档拉平 + `effectiveSurfaceOpacities` 无壁纸时三档同值 | 逐元素读 `backgroundColor` 与 `backdrop-filter` | **通过**（默认 0.88 + 12px 模糊，三档分开） |
| ③ 关于页作者卡可设背景图 / 尺寸 / 内容 | `.author-profile` 只有 `background: var(--surface-raised)` | 写入 centered 横幅后读 DOM class + 截图 | **通过** |
| ④ 页面编辑器的 SEO 档没必要 | 页面级 SEO 与「SEO 管理」页职责重叠 | 读 tab 数量 + 保存 payload | **通过**（只剩 内容 / 样式 / 预览） |
| ⑤ 网站设置太杂 | 7 段平铺混了五类关注点 | 读 tab 与面板标题顺序 + 截图 | **通过**（4 分组 + 12 面板） |

## 2. 玻璃感默认开启（诉求 ②）

写入默认档位（`glassMode=on`、`backgroundType=gradient`、`#f4f7fc→#e9eef7`、`surfaceOpacity=88`、`surfaceBlur=12`）后，
前台 computed style 实测：

| 元素 | 档位 | `background` | `backdrop-filter` |
|---|---|---|---|
| `.intro-shell` | paper | `color(srgb 1 1 1 / 0.88)` | `blur(12px) saturate(1.05)` |
| `.article-list` | raised | `color(srgb 0.933333 0.941177 0.956863 / 0.88)` | `blur(12px) saturate(1.05)` |
| `.sidebar-block` | raised | `color(srgb 0.933333 0.941177 0.956863 / 0.88)` | `blur(12px) saturate(1.05)` |
| `.archive-callout` | control | `color(srgb 0.905882 0.917647 0.941176 / 0.82)` | `blur(12px) saturate(1.05)` |
| `.author-profile`（关于页） | raised | `color(srgb 0.933333 0.941177 0.956863 / 0.88)` | `blur(12px) saturate(1.05)` |

`html[data-wallpaper]` 存在（默认渐变画布即算壁纸），`#selfblog[data-surface-translucent]` 与 `[data-surface-blur]` 均已设置。
**三档是三个不同的值（1.0 / 0.94 / 0.82 附近），不再是同一个白块** —— 这正是「一点半透明效果没有」的修复判据。

深色模式（点导航栏 `.theme-button`，`html.className = "dark-mode"`）：

| 元素 | `background` | 文字色 |
|---|---|---|
| `.intro-shell` | `color(srgb 0.12549 0.141176 0.172549 / 0.94)` | `rgb(237, 240, 245)` |
| `.article-list` / `.sidebar-block` | `color(srgb 0.145098 0.164706 0.2 / 0.88)` | `rgb(237, 240, 245)` |

作者真实配置（壁纸图 + `surfaceOpacity=60` + `surfaceBlur=0`）下同样成立：
`.intro-shell` = `color(srgb 1 1 1 / 0.6)`、`.sidebar-block` = `color(srgb 0.933333 0.941177 0.956863 / 0.88)`，
`backdrop-filter: none`（作者自己把模糊调成 0，属预期），照片透上来的观感符合「清透纸面」方向。

**单位陷阱（已排除，仅记录）**：`--ap-surface-*-opacity` 必须带 `%`（`site.ts:561-567` 实测确认为 `${tiers.paper}%`），
`--ap-surface-blur` 必须带 `px`。第一轮探针写成裸数字 `'88'` 导致 `color-mix(in srgb, … 88, transparent)` 非法、
整条声明失效、卡片全透明 —— 那是探针 bug，不是生产 bug。

## 3. 页面横幅（诉求 ① 与 ③）

### 首页（split 版式）

写入 `banner = {image:'/uploads/site-assets/background/8eec677bdc794cfbb02bdc7c6ae90c48.png', fit:'cover', position:'center', height:'tall', overlay:35, tone:'light', layout:'split'}` 后 DOM 实测：

| 读数 | 值 |
|---|---|
| `className` | `intro-shell intro-shell--banner intro-shell--banner-split` |
| `min-height` | `330px` |
| `data-banner-tone` | `light` |
| `--banner-height` | `330px` |
| `--banner-overlay` | `35` |
| `--banner-image` | `url("/uploads/site-assets/background/8eec677b…png")` |
| `.intro-banner` 存在 | `true` |

### 关于页（centered 版式）

写入 `height:'standard'`、`overlay:45`、`tone:'light'`、`layout:'centered'` 后：

| 读数 | 值 |
|---|---|
| `className` | `author-profile author-profile--banner author-profile--banner-centered` |
| `data-banner-tone` | `light` |
| `.profile-banner` 存在 | `true` |

还原后 `RESTORE_home` / `RESTORE_about` 均 200，config 回到原值（home `{text:{},modules:{}}`，about 保留原 Markdown 正文）。
**未配置横幅时 `.intro-shell` / `.author-profile` 的 class、style 与 DOM 与改造前逐字一致** —— 这是零回归的判据。

### 地址白名单

横幅背景图与 `PageSeo.image` 共用同一套 `PageCatalog.imageUrl()` 校验：空值放行；长度 ≤2048；
`/uploads/` 开头且不含 `\`、`%`、`..`、CR、LF 放行；否则必须 `URI.create` 成功、有 host、scheme ∈ {http, https}、无 userInfo。
前端 `safeBannerUrl()` 同规则再挡一次（拒 `[\s"'()\\]`），因为它会被拼进 CSS `url()`。

## 4. 后台信息架构（诉求 ④ 与 ⑤）

### 工作台

`workspaceLinks` 实测 5 张卡：`["文章管理…", "页面管理 自定义首页、关于页与导航，以及站点其它独立页面。", "内容体检…", "访客记录…", "网站设置…"]`
—— **SEO 卡已移除**，换成「页面管理」（侧栏的「SEO 管理」入口保留，文章级 SEO 仍在那里）。

### 网站设置

`[data-tab]` 实测 `["profile", "appearance", "interaction", "system"]`，默认选中 `profile`。12 个面板标题顺序：
站点资料、成品主题、视觉主题、玻璃感、页面背景、外观深度自定义、互动开关、背景音乐、留言审核、后台入口、访客统计、模型接口。

- 成品主题 4 张卡实测 `["paper", "ink", "sand", "dusk"]`（纸间 / 墨青 / 暖砂 / 暮紫），每张 3 个色点预览 + 名称 + 一句说明；
- 玻璃感：`[data-setting="glassMode"]` 为 checkbox、`value="on"`、`checked=true`；「卡片不透明度」`min=60 max=92`、
  「卡片模糊」`min=0 max=40`，关闭玻璃感时两者 `disabled`；
- 「系统与安全」只剩后台入口、访客统计、模型接口三块，模型接口块只有一句指向 `${adminRoot}/seo` 的说明 —— **表单已不重复**。

### 页面编辑器

`EDITOR_TABS` 实测 `["内容", "样式", "预览"]` —— **SEO 档已删除**。样式 tab 下横幅控件
`BANNER_FIELDS` 实测 `["fit", "position", "height", "tone", "layout", "overlay"]` 六项齐全，背景图走
`SiteAssetField kind="background"`（复用站点背景图的上传通道与 8MB/png-jpg-webp 限制）。
页面级 SEO 仍生效：`usePageSeo` 继续读已存的 `config.seo`，后端 `PageCatalog` / `ReadingPageService` / `SitemapService`
也继续消费它，只是不再提供编辑入口。

### SEO 页的模型连接面板

`[data-llm-panel]` 实测存在，面板文本含「AI 辅助与 SEO / 接入任意 OpenAI 兼容接口…密钥只保存在服务端」，
控件含启用开关、服务商下拉（DeepSeek 官方接口 / 自定义）、API 地址、API Key（掩码 + 「已配置」徽标）、
模型候选下拉 + 刷新、模型名、超时、最大 token、思考强度、思考模式、测试连接与保存连接配置；页面 `console.error` 为空。
（首轮探针报 false 是**探针未带登录态**导致跳到登录页，不是代码缺陷。）

## 5. 自动化闸门

| 闸门 | 结果 |
|---|---|
| 后端 `mvnw test`（`-Dskip.npm=true -Dskip.installnodenpm=true`） | **1288 tests, 0 failures, 0 errors** + `BUILD SUCCESS` |
| 前端 `vitest run` | **55 files / 534 tests** 全通过（基线原为 51 / 504，本批新增 4 文件 30 测试） |
| `vue-tsc --noEmit` | 退出码 0 |
| `eslint src` | **0 errors**, 42 warnings（与基线完全一致，warning 全在既有文件） |
| `prettier --check "src/**/*.{ts,tsx,vue,css,json}"` | `All matched files use Prettier code style!` |
| `test:deployment` | **27 / 27** |
| `vite build` | 退出码 0，`✓ built in 3.03s` |

跑闸门前的环境准备（本机必须）：
`$env:TEMP` / `$env:TMP` 指到工作区内目录（否则 `vercel-proxy.test.mjs:13` 的 `mkdtemp` 报 `EPERM`），
`backend/target/frontend/node` 前置到 `PATH`（满足 engine-strict），用 `& .\node_modules\.bin\<tool>.cmd`
（`.ps1` 垫片被执行策略拒绝）。

## 6. 未验证项

- **真实 MySQL 与 Docker 部署**：本机没有守护进程，仍由 CI 的 `mysql` / `docker` job 覆盖。
- **真实 Vercel 部署**：没有账号与域名，`vercel.json` 的代理契约只由 `test:deployment` 覆盖。
- 服务端阅读 HTML 的横幅样式（`seo-reading.css` 的 `seo-banner-*`）已由 `ReadingHtmlRendererTest` 与
  `ReadingPageServiceTest` 断言，但**没有做逐像素的视觉比对**；Vue 挂载会整体替换 `.seo-reading`，
  爬虫与无 JS 客户端看到的是那一份。
