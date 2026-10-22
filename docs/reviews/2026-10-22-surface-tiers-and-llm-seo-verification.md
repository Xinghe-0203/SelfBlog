# 三级表面分层重构与 LLM 辅助 SEO — 真机验证记录 (2026-10-22)

对应改动：`frontend/src/styles/appearance.css`（三档表面 token 与选择器清单）、
`frontend/src/components/pages/PageFrame.vue`（`data-page-background` 永远解析出值）、
`frontend/src/stores/site.ts`（`effectiveSurfaceOpacities` 应用层夹紧 + `data-wallpaper` 根级标志）、
`frontend/index.html`（首屏预置脚本打 `data-wallpaper`），
以及后端 `service/seo/**`、`entity/PostSeo`、`controller/admin/AdminSeoController`（LLM 辅助 SEO 的 8 个管理端端点）。
CHANGELOG 条目见 `CHANGELOG.md` 的「v2.0.27 — 外观三级表面分层重构 + LLM 辅助 SEO (2026-10-22)」。

用户诉求（m04388）：「UI页面问题相信你也能看出来，请仔细帮我重新设计UI的这些方面。另外请在后台接入LLM，自动管理SEO等各种内容」，
附三张截图：首页 / 关于页 / 文章详情，均 1919x911，一张向日葵壁纸铺满视口。

## 方法与工具链

- **浏览器**：本机 Chrome 155，经全局 Playwright **1.59.1** 以 headless 驱动；视口 **1440x900** 与 **390x844**。
  探针脚本必须写成 CommonJS `.cjs`：ESM 里 `import` 绝对 Windows 路径会报 `ERR_UNSUPPORTED_ESM_URL_SCHEME`。
- **before** = HEAD 的字节级副本（另一台 Vite 服务），**after** = 工作区改动（Vite dev :3000）。
  两个前端都把 `/api`、`/uploads` 代理到同一个后端 `:8825`。
- **播种**：壁纸与站点内容全部经**管理端 HTTP API** 写入（`PUT /api/admin/site-settings`），**从未手改 `campus_blog.db`**。
  壁纸态 = `backgroundType=image` + `backgroundOverlay=0` + `surfaceOpacity=60`（与用户截图同款）；
  回归对照 = `backgroundType=solid` + `surfaceOpacity=100`。
- **登录**：管理端登录是 `POST /api/user/login`（**不是** `/api/auth/login`，后者不存在），
  凭证取 `backend/.env` 的 `ADMIN_USERNAME`/`ADMIN_PASSWORD`（需 `.Trim()`），响应 `data.token`。
- **对比度采样**：对每个文本目标取 bbox，截取其后背景像素，取**众数色**（剔除与文字色 RGB 距离 <40 的像素），
  把文字色按 CSS 计算值合成，再算 `(L1+0.05)/(L2+0.05)`（WCAG 2.1）。
- **像素差**：`ImageChops.difference` 逐像素比较同模式、同视口的 before/after 截图。
- **无障碍降级**：Playwright 的 `page.emulateMedia()` **不支持** `prefers-contrast` / `prefers-reduced-transparency`
  （恒为 `matches:false`，用它得到的「通过」是无效证据），必须用 CDP `Emulation.setEmulatedMedia({features:[…]})`。
- 关键原始产物（临时目录，不随仓库提交）：`.tmp-ui/{before,after}/<mode>/{measure.json,contrast.json}`、
  `.tmp-ui/diff/solid-pixel-diff.json`、`.tmp-ui/compare.txt`、`.tmp-ui/UI-REPORT.md`。
  持久化截图见 `docs/reviews/assets/2026-10-22-surfaces-*.png`（8 张）。

## 1. 结论

| 指标 | 改造前 | 改造后 |
|---|---|---|
| 壁纸模式对比度不达标项（WCAG 4.5:1） | **13** | **0** |
| 暗色模式（同一张照片）不达标项 | **18** | **0** |
| midnight 皮肤（同一张照片）不达标项 | **16** | **0** |
| 纯色画布不达标项 | 4（既有缺陷） | 3（同 3 项，另 1 项在 390px 因侧栏隐藏而不再采样） |
| 直接压在壁纸上的文本节点（`bareText`） | 见 §5 | **0**（16 个页面 / 视口组合全为 0） |
| 390x844 横向溢出 | 无 | 无（24 个页面/视口组合 `scrollWidth === clientWidth`） |
| 纯色画布逐像素差异 | — | 首页 0 px、关于 0 px、文章页 432 px（**仅为阅读计数文字变化**，见 §4） |

三张截图反映的三类问题都在根因层修掉，而不是靠 `text-shadow` 或加深字色遮掩：

1. **正文直接压在照片上** → `PageFrame` 永远解析出表面，壁纸模式下走「实心纸面」档；
2. **半透明白块与不透明白块混杂（风格割裂）** → 统一为三档表面 token，截图里读错的元素全部并入同一份选择器清单；
3. **幽灵按钮 / 虚线按钮在亮照片上消失** → 壁纸模式下接管为「控件档」底色 + 实线边框。

## 2. 根因 → 修复映射

| # | 根因 | 修复 | 文件 |
|---|---|---|---|
| 1 | `PageFrame.vue` 绑定 `:data-page-background="config.style?.background"`；页面配置没有 `style.background` 时属性**整个不渲染**，而 `.page-frame` 没有任何兜底背景规则 → 整列内容画在壁纸上（截图三） | `PageFrame.vue` 用 computed 兜底：`config.style?.background ?? 'auto'`，属性**永远存在**；壁纸模式下 `auto` / `plain` 都解析为纸面档（实心 + 圆角 + 1px 内描边）。`inherit` 成为唯一、有文档的装饰性退出开关 | `frontend/src/components/pages/PageFrame.vue`、`frontend/src/styles/appearance.css` |
| 2 | `appearance.css` 的半透明表面是**27 条选择器的封闭白名单**，不在表里的（`.article-column`、`.toc-card`、`.author-profile`、`.about-site`…）保持全透明 | 白名单改为**三档表面清单**：纸面档 8 条、浮起档 25 条（含 `.author-profile`、`.toc-card`、`.adjacent-link`、`.comment-form`）、控件档 7 条。scoped 样式不再硬写 `background: var(--surface-primary)`，而是消费档位变量 | `frontend/src/styles/appearance.css` |
| 3 | 作者可设 `backgroundType=image` + `backgroundOverlay=0`：零遮罩、画布亮度不可预测，而 `--text-primary:#262a31` 固定深色；`border:1px dashed; background:transparent` 的幽灵按钮在亮照片上等于没有 | 引入 `html[data-wallpaper]` 根级标志（见 §3）；壁纸模式下控件档接管底色 + 实线边框；`--text-secondary`/`--text-muted` 在亮壁纸上各收紧一级（见 §3.3） | `frontend/src/stores/site.ts`、`frontend/src/styles/appearance.css` |
| 4 | `surfaceOpacity` 存储允许低到 60%，无法在任意照片上维持 4.5:1；深色模式下「浅色文字 + 60% 深色表面」压亮照片约 2:1 | 保留后端 60–100 存储契约，**在应用层夹紧**：`effectiveSurfaceOpacities()`，纸面恒 100%，浮起档亮/暗画布下限 88% / 94%，控件档比浮起档再低 6%（≥60%） | `frontend/src/stores/site.ts` |

## 3. 三档表面体系

### 3.1 根级标志与选择器形态（本次最关键的坑）

`data-wallpaper` 挂在 **`documentElement`（`<html>`）** 上：`frontend/index.html` 的首屏预置脚本只能在 Vue 挂载前写
`<html>`（那一刻 `#selfblog` 还不存在），运行时 `applyAppearance()` 为与它同源，也写 `document.documentElement`。
因此 CSS **必须**写成：

```css
html[data-wallpaper] #selfblog .page-frame[data-page-background='auto'] { … }
```

第一版写成 `#selfblog[data-wallpaper] …` 是**永远不匹配的死 CSS**：分层、纸面兜底、控件档全部失效，等于原缺陷仍在。
两处写入目标现已一致，判定谓词统一为 `!!vars['--ap-bg-image']`（同时覆盖照片与作者渐变）。

`PREPAINT_APPEARANCE_VARS` **刻意未扩展**：该数组既是首屏安全边界，又被
`frontend/src/__tests__/indexHtml.test.ts` 断言逐字等于 `APPEARANCE_KEYS`；标志只由脚本自行推导。

### 3.2 三档 token 与合成数学

| 档 | 语义 | 纯色画布（基线） | 壁纸模式（亮画布） | 壁纸模式（暗画布） |
|---|---|---|---|---|
| `--surface-paper` | 页面级纸面：PageFrame、Hero 文案、空态、页脚 | `--surface-primary` | `#ffffff` @ **100%** | `#20242c` @ **100%** |
| `--surface-raised` | 纸面上的卡片：`.article-list`、`.sidebar-block`、`.author-profile`、`.toc-card`、`.adjacent-link`、`.comment-form`… | `--surface-primary` | `--bg-secondary` @ **88%** → 合成 `#f0f2f5` | `--bg-secondary` @ **94%** |
| `--surface-control` | 控件：幽灵按钮、`.about-site a`、`.like-action`、`.load-more button` | `--surface-primary` | `--bg-tertiary` @ **82%** → 合成 `#ebeef3` | `--bg-tertiary` @ **88%** |

- **纯色画布三档全部别名 `--surface-primary`**，即改造前 `.card` / `.sidebar-block` / `.like-action` 本来用的那枚 token
  ⇒ 逐像素不变（§4 实测）。这是「纯色画布零回归」红线的实现基础。
- **纸面档刻意与 `.page-frame[data-page-background='plain']` 同一枚 token**：作者显式声明 `plain` 与不声明（走 `auto` 兜底）
  必须落在同一张纸上，否则同一站点出现两种白。
- **控件档基线也是 `--surface-primary`（不是 `--bg-secondary`）**：改造前 `.like-action`、`.about-site a` 写的就是
  `--surface-primary`，基线若换成浅灰，纯色画布上它们会变色 —— 那是回归。真正的「控件比纸面深一档」只发生在壁纸模式。
- **为什么不直接用 `--surface-secondary` 做浮起档**：壁纸下纸面已是实心 `#ffffff`，`--surface-secondary`（`#f9fafc`）
  压在纯白上只差 5/255，肉眼读不出台阶，违反「必须差一档可见」。改用 `--bg-secondary` / `--bg-tertiary` 后：
  可见台阶（亮画布）纸面→浮起 **15/255**，浮起→控件 **5/255**；再叠加各自 1px 边框定义边界
  （照片上色阶差会被压缩，边界才是层次）。暗画布纸面→浮起 7/255，浮起→控件 8/255。
- **midnight 皮肤已知限制**：其 `--surface-secondary`（`#1d2c3c`）比 `--surface-primary`（`#182535`）**更暗**，
  所以浮起档在 midnight 上是一层「轻微内凹」（台阶 3/255），层次主要由控件档（13/255）承担。
  这是皮肤原有色阶方向决定的，未强行改皮肤 token（会破坏皮肤一致性）。

### 3.3 对比度夹紧数学

`effectiveSurfaceOpacities(rawOpacity, dark, wallpaper)`：

- 非壁纸：三档 = `clampInt(rawOpacity, 60, 100, 100)`（与改造前完全一致，零影响）。
- 壁纸：`paper = 100`，`raised = max(base, dark ? 94 : 88)`，`control = min(raised, max(60, raised - 6))`。

为什么纸面必须 100%：`L(#262a31)=0.023 → 对纯白 14.4:1`；`L(#67707e)=0.160 → 4.45:1`；
`L(#707a88)=0.191 → 4.35:1`（**连纯白都不到 4.5**）。纯白 1.0 已是可能的最亮表面，任何半透明层只会更暗
⇒ 弱化字色在「照片 + 半透明」下无解；只有实心纸面 + 收紧的弱化 token 才能同时满足 4.5:1 与层级。

无表面时的实测代价（同一张亮照片，L≈0.67）：`#262a31` 7.54:1（这就是旧页面「勉强能读一半」的错觉来源）、
`#5f6875` **2.95:1**、`#646d7b` **2.74:1** ⇒ 弱化档一旦离开纸面立刻崩，所以「任何文本都不许压在图上」是硬规则而非偏好。

| token | 旧值 | 旧值对纯白 | 新值 | 对纯白 | 对浮起档 | 对控件档 |
|---|---|---|---|---|---|---|
| `--text-primary` | `#262a31` | 14.40 | 不变 | 14.40 | 12.84 | 12.38 |
| `--text-secondary` | `#67707e` | 5.01 | **`#5f6875`** | 5.64 | 5.03 | **4.85** |
| `--text-muted` | `#707a88` | **4.35** | **`#646d7b`** | 5.23 | 4.66 | **4.50** |

只在 `html[data-wallpaper] #selfblog:not(.dark-mode):not([data-skin='midnight'])` 生效 —— 暗色画布不接管：
`#a7afbc` / `#939dad` 在 94% 深色纸面上本来就有 7.04 / 5.68，改深反而掉到 2:1。

## 4. 纯色画布逐像素一致性（首要回归红线）

| 页面 | 视口 | 变化像素 | 位置 | 说明 |
|---|---|---|---|---|
| home | 1440x900 | **0 / 1,296,000** | — | 完全一致 |
| about | 1440x900 | **0 / 1,296,000** | — | 完全一致 |
| post | 1440x900 | 432 / 1,296,000 | bbox (431,357)-(483,368) | 见下 |
| home | 390x844 | **0 / 329,160** | — | 完全一致 |
| about | 390x844 | **0 / 329,160** | — | 完全一致 |
| post | 390x844 | 432 / 329,160 | bbox (225,330)-(277,341) | 见下 |

**这 432 px 不是样式差异**：该 bbox 正是 `.article-meta` 里的阅读计数文字
（`6 次阅读` → `12 次阅读`），是截图脚本反复访问 `/post/14` 把它刷上去的。

另经计算样式复核：纯色画布下 light/ocean、dark/ocean、light/midnight、dark/midnight 四种组合的
`--surface-paper/raised/control` 全部等于各自的 `--surface-primary`，`.sidebar-block` / `.article-list` 背景与 HEAD 同值
（`rgb(255,255,255)` / `rgb(32,36,44)` / `rgb(24,37,53)`），`.page-frame` 保持 `rgba(0,0,0,0)` + `border-radius: 0px`。

## 5. 没有任何文本压在壁纸上（硬证据）

- **结构性**：`PageFrame` 的 `data-page-background` 永远解析出值（新测试 `PageFrame.test.ts` 5 条断言覆盖
  `{}`、`{style:{width}}`、`{style:{background:null}}`、以及四个显式值）；壁纸模式下 `auto`/`plain` 都获得实心纸面。
- **运行时**：遍历 `archives` / `search` / `tag` / `gallery` / `notfound` 共 10 个页面/视口组合，
  对每个文本节点向上找「已上色背景」祖先，凡一路到 `<body>` 都没找到的记为 `bareText` —— **10/10 为 0**。
  加上 home / about / post 的 6 个组合，共 16 个页面/视口组合 `bareText = 0`。
- **自定义页面**：`/pages/vis-verify`（`ContentPageView.vue` + `PageFrame`）同样得到 `attr=auto`、`bg=color(srgb 1 1 1)`。

## 6. 对比度前后对照（真实像素采样，WCAG 2.1）

### 6.1 壁纸模式（亮照片，overlay 0，不透明度 60）— FAIL 13 → 0

| 页面 | 目标 | 改造前 | 改造后 |
|---|---|---|---|
| home 1440 | `home-ghost-button`「按时间浏览文章」 | **3.30 ✗** | **12.38 ✓** |
| home 1440 | `home-sidebar-author` 作者卡正文 | **3.88 ✗** | 5.03 ✓ |
| home 1440 | `home-sidebar-tag` 标签芯片 | 4.63 | 5.22 ✓ |
| home 1440 | `home-section-heading`「最近写作」 | 7.54 | 14.40 ✓ |
| home 1440 | `home-article-summary` | 4.65 | 5.03 ✓ |
| home 1440 | `home-hero-description` | 4.79 | 5.64 ✓ |
| about 1440 | `about-site-p` 站点简介（**原本直接压照片**） | **3.30 ✗** | 5.64 ✓ |
| about 1440 | `about-body-p` 正文（**原本直接压照片**） | 7.54 | 14.40 ✓ |
| about 1440 | `about-chip` 导航芯片 | 5.01 | 12.38 ✓ |
| about 1440 | `about-bio` 作者简介 | 5.01 | 5.03 ✓ |
| post 1440 | `post-title` | 9.51 | 14.40 ✓ |
| post 1440 | `post-summary` | **2.62 ✗** | 5.64 ✓ |
| post 1440 | `post-body` 正文 | 12.18 | 14.40 ✓ |
| post 1440 | `post-meta` 元信息 | **2.49 ✗** | 5.23 ✓ |
| post 1440 | `post-toc-link` 目录链接 | **4.10 ✗** | 4.67 ✓ |
| post 390 | `post-title` / `post-summary` / `post-meta` | 7.54 / **2.62 ✗** / **2.28 ✗** | 14.40 / 5.64 / 5.23 ✓ |

390x844 下 `.home-sidebar`（<960px 隐藏）与 `.article-aside`（<1020px 隐藏）的目标标记为 `hidden` 而非参与评分（§9）。

### 6.2 暗色模式（同一张照片）— FAIL 18 → 0

| 目标 | 前 | 后 |
|---|---|---|
| `post-toc-link` | **1.29 ✗** | 5.31 ✓ |
| `post-body` | **3.38 ✗** | 13.62 ✓ |
| `post-title` | **4.13 ✗** | 13.62 ✓ |
| `post-summary` | **2.57 ✗** | 7.04 ✓ |
| `post-meta` | **3.69 ✗** | 5.68 ✓ |
| `about-site-p` | **2.14 ✗** | 7.04 ✓ |
| `home-ghost-button` | **2.14 ✗** | 11.57 ✓ |
| `home-section-heading` | **4.97 ✗** | 13.62 ✓ |
| `about-bio` | 7.04 | 6.58 ✓（浮起档 94% 深色而非实心，仍达标） |

### 6.3 midnight 皮肤（同一张照片）— FAIL 16 → 0

`post-toc-link` **1.13 ✗ → 5.08**；`post-body` **3.42 ✗ → 13.75**；`post-title` **4.19 ✗ → 13.75**；
`post-summary` **3.13 ✗ → 8.54**；`post-meta` **3.23 ✗ → 4.96**；`about-site-p` **2.60 ✗ → 8.54**；
`home-ghost-button` **2.60 ✗ → 12.39**。

### 6.4 纯色画布（回归对照）

全部目标 `same`（|Δ| < 0.005）。仍存在的 3 项**是改造前就有的**，与本次改动无关，且数值前后完全一致：

- `post-toc-link` 4.02（`#707a88` on `#f5f6f8`）
- `post-meta` 3.71（1440）/ 4.02（390）

未覆盖原因：纯色画布下 `--text-muted` 仍是 `#707a88`（4.35:1 对纯白），要修就得动全局 token，
会改变整站纯色观感 —— 超出「壁纸场景」的任务边界，且会破坏纯色逐像素红线。**建议单独立项**
（把 `--text-muted` 全局收紧到 `#646d7b`，影响面需要单独回归）。

## 7. 组件与观感

- **幽灵 / 虚线控件**：`html[data-wallpaper] #selfblog` 下接管 `.btn-ghost`、`.archive-callout`、`.about-site a`、
  `.load-more button`、`.load-more-comments`、`.like-action`、`.state-panel button` →
  `background: var(--surface-control)` + `1px solid var(--border-light)` + `color: var(--text-primary)`。
  **只在壁纸模式接管**：纯色画布的幽灵按钮本来就压在自己纸面上，接管会改变观感（红线）。
  期间修正：`.load-more` 在 `TagDetail.vue:112` 是**包裹 div**，早期把它写进清单等于给整块加框；
  已改为只选 `.load-more button`，并把 `PostDetail.vue` 的 `.load-more-comments` 一并纳入。
- **Hero**：删掉 `.intro-visual` 的 `background: var(--bg-tertiary)` 平铺块，图片本身即面板
  （这是 2026-10-20 那轮「hollow hero」的根治延续）；`.intro-shell` 壁纸模式下底色改为 `var(--surface-paper)`，
  `.intro-shell::after` 的遮罩渐变改为由 `var(--surface-paper)` 构成（35% / 94% / 16%），
  保证文案背后的渐变与身后纸面同一种白，任何皮肤、明暗两种模式都不出现色带。
- **层级统一**：`.article-list`、`.sidebar-block`、`.author-profile`、`.toc-card`、`.adjacent-link`、`.comment-form`、
  `.archive-list`、`.not-found-content`、`.album-card` 全部落在浮起档；页面级大面落在纸面档；芯片/按钮落控件档。
  About 作者卡（原不透明白）与相邻区域现在是同一族表面，截图二的「风格割裂」消失。
- **目录卡**：原只有上下 1px 分隔线、无底色无圆角（截图三最刺眼处），壁纸模式下补成真正的卡片
  （浮起档 + 四边 1px + `--radius-lg`）。纯色画布不接管，保留原「书页侧栏」观感。
- **Token 纪律**：新增声明只消费既有 token（`--surface-primary` / `--bg-secondary` / `--bg-tertiary` / `--radius-*` /
  `--border-light`），无裸 px 字号、无 `--gray-*` 当深色表面、无新配色；`appearance.css` 仍是 `main.css` 最后一条 import。

## 8. 无障碍降级（CDP 实测）

壁纸 + 不透明度 60，明暗各测一遍：

| 条件 | 亮画布 | 暗画布 |
|---|---|---|
| 基线 | filter `blur(8px) saturate(1.05)`，paper `#ffffff 100%`，raised `#eef0f4 88%`，control `#e7eaf0 82%` | paper `#20242c 100%`，raised `#252a33 94%`，control `#2c323d 88%` |
| `prefers-contrast: more` | filter `none`；paper/raised `#ffffff`，control `#eef0f4`；卡片实心白 | filter `none`；paper/raised `#20242c`，control `#252a33` |
| `prefers-reduced-transparency: reduce` | 同上 | 同上 |
| `forced-colors: active` | 同上，且 frame/card 全为 Canvas `rgb(255,255,255)`、`box-shadow: none` | 同上，Canvas 覆盖 |

三档在降级后全部变成**实心 token**（不再是 `color-mix`），符合「降级代价是没有毛玻璃，而不是读不清字」。

## 9. 过程中发现的两个陷阱（都会静默回归）

### 9.1 `data-wallpaper` 选择器形态

第一版 CSS 写成 `#selfblog[data-wallpaper] …`，属性却挂在 `<html>` 上 ⇒ **永远不匹配的死 CSS**，
分层、纸面兜底、控件档全部失效，等于原缺陷仍在。必须 `html[data-wallpaper] #selfblog …`。

### 9.2 深色模式特异性

`--surface-raised` 这个名字在 `variables.css:137` 与 `dark-mode.css:100` 里**已作为向后兼容别名**存在
（`--surface-raised: var(--surface-secondary)`）。基线块写成 `#selfblog { … }`（特异性 0,1,0,0）会**输给**
`dark-mode.css` 的 `#selfblog.dark-mode`（0,1,1,0）⇒ 深色模式 + 纯色画布下卡片会从 HEAD 的 `--surface-primary`
变成 `--surface-secondary`，正好击穿「纯色逐像素不变」红线。

修复：基线块补 `#selfblog.dark-mode` / `#selfblog[data-skin='midnight']` 变体，特异性打平后由
「`appearance.css` 最后导入」胜出。同一陷阱也存在于无障碍降级块，一并补齐。
修复后实测：四种「明暗 × ocean/midnight」组合三档均等于各自 `--surface-primary`（§4），三种降级在明暗两种模式下都生效（§8）。

### 9.3 度量完整性（避免误读数字）

- 早期 390x844 的 `home-sidebar-*` / `post-toc-link`「失败」是**采样假象**：目标 `display:none` 时旧脚本仍截了
  视口角落当背景。现已加零尺寸守卫，标记为 `hidden` 并从汇总中剔除。
- 早期脚本把「在折叠线以下」误判为隐藏（实际可滚动），已修正为只按 `rect.width/height < 1` 判定。
- `measure.json` 里 `hasWallpaperFlag` 显示 `null`：属性是**空字符串**（`data-wallpaper=""`），
  脚本用取属性值的方式读到 `null`；档位实际解析正确（`paper color-mix(#ffffff 100%)` 等）可反证标志已生效。
- Playwright 的 `prefers-contrast` / `prefers-reduced-transparency` 恒为 `matches:false` 属工具限制，无效；
  权威证据是 §8 的 CDP 探测。

## 10. LLM 辅助 SEO 端到端验证（真实 jar + 真实 SQLite）

在 `backend/target/selfblog.jar`（默认 profile、真实 SQLite、15 篇文章）上手工跑完整链路：

| 步骤 | 结果 |
|---|---|
| `GET /api/admin/seo/overview`（未配置） | `llmConfigured:false`、`totalPosts:15`、`withSeo:0`、`coveragePercent:0`、`missingCover:12` |
| `GET /posts?pageNum=1&pageSize=3` | `total=15 pages=5 n=3`，row0 `issues:["noSeo","missingCover"]` |
| `GET /posts/15`（无记录） | **404** `{"code":404,"message":"该文章还没有 SEO 元数据"}`（前端据此判「尚未填写」） |
| `PUT /posts/15` 手工保存 | 200，`hasSeo:true`、`source:"manual"`；再 GET 一致 |
| `POST /posts/15/generate`（未配置） | **400**「AI 辅助功能已关闭，请先在设置中启用」（先判 enabled 再判 configured） |
| `PUT` 校验：`ogImageUrl:"//evil.com/x.png"` | 400「分享图地址不能使用 // 开头的协议相对地址」 |
| `PUT` 校验：`seoTitle` 201 字符 | 400「SEO 标题不能超过 200 个字符」 |
| `GET /post/15`（服务端渲染 head，200、6261 B） | `<title>… - 个人博客</title>`、`<meta name="description">`、`<meta name="keywords" content="mobile, responsive, typography">`、`og:title`、JSON-LD `id="selfblog-article-jsonld"`（`headline` 用原始标题、`description` 用 SEO 描述） |
| `PUT` 设置密钥 → `GET` 回读 | `llmApiKey='••••••••'`（**8 个 U+2022**）、`llmBaseUrl` 存储原样带 `/chat/completions`（归一化在读取时做） |
| 再次 `PUT` 只传哨兵 + 改 model | key 保留（`llmConfigured` 仍 true）、model 更新 |
| `POST /test-connection`（上游不可达） | **恒 200** `{"ok":false,"message":"无法连接模型服务，请检查接口地址与网络",…}` |
| `POST /generate`（上游不可达） | **502** 同文案 |
| `PUT llmApiKey:""` | 清除，`llmConfigured:false` |
| `GET /api/site/public` | 26 个键，**`llmKeys=0`**（配置了 key 的状态下也仍为 0） |

og:image 与 twitter:image 在该篇未出现，因为它既无封面也未设 `ogImageUrl`，回落链末端为空 —— 符合契约
（回落链：`post_seo.og_image_url` → `blog_post.cover_url` → `siteLogoUrl` → 无）。

## 11. 已证实 / 未覆盖

### 已证实（真实 Chrome + 真实后端，截图 + 机器可读度量）

1. 壁纸模式对比度不达标项 13 → **0**；暗色 18 → **0**；midnight 16 → **0**（§6）。
2. 纯色画布**逐像素等同改造前**（home/about 0 px；post 仅 432 px 为阅读计数文字变化）（§4）。
3. 没有任何文本节点直接压在壁纸上：16 个页面/视口组合 `bareText = 0`（§5）。
4. 三档表面在壁纸模式下**真的分层**（亮画布合成 `#ffffff` / `#f0f2f5` / `#ebeef3`），
   且四皮肤 × 明暗两模式的纯色画布三档都等于各自 `--surface-primary`（§3.2、§4）。
5. 无障碍三种降级在**明暗两种模式**下都把三档压成实心 token 并去掉模糊（§8）。
6. 390x844 无横向溢出：24 个页面/视口组合 `scrollWidth === clientWidth`。
7. LLM SEO 的 8 个管理端端点、掩码往返、校验、失败语义（400/502/恒 200）、公开设置过滤、
   服务端渲染 head 全部在真实 jar 上端到端通过（§10）。

### 未覆盖（明确说明原因）

1. **纯色画布既有的 `--text-muted` 不达标**（4.02 / 3.71），非本次引入，需独立任务（§6.4）。
2. **midnight 浮起档台阶仅 3/255**，层次靠控件档与边框承担，根治需改皮肤色阶（§3.2）。
3. **真实手机浏览器 / Safari**：仅在 Chrome 上验证；`backdrop-filter`、`color-mix()` 在旧版 Safari/Firefox 的降级未实测。
4. **作者任意照片的极端情况**：只用了「亮暖色照片」（L≈0.67）与纯色画布两档；未穷举全黑照片、低对比照片、动图。
   夹紧逻辑按最坏画布（纯白 L=1.0）推导，理论上覆盖更暗画布，但未逐张实测。
5. **真实外部 LLM 上游**：只验证了「未配置 / 不可达」两条失败路径与本地 `com.sun.net.httpserver` 桩
   （`StubLlmServer`，测试内无网络）；真实 OpenAI / DeepSeek 等上游的响应差异未在本机实测。
6. **真实 Vercel / 生产构建产物上的截图**：只测了 dev server；`vite build` 产物由闸门与 jar 冒烟覆盖。
7. **`prefers-reduced-motion`**：与本次表面体系无关，未验证。
8. 后端 `views/admin/**` 未迁移到三档 token（后台在纯色画布下本来就正常，超出「壁纸场景」边界）。
9. `MobileTocSheet.vue`、`Toast.vue`、`NavbarMusicControl.vue`、`BackToTop.vue` 等浮在画布上的控件未纳入三档体系
   （`BackToTop` 最值得后续跟进）。

## 12. 截图

持久化在 `docs/reviews/assets/`：

- [`2026-10-22-surfaces-post-before-1440x900.png`](assets/2026-10-22-surfaces-post-before-1440x900.png) / [`…-post-after-1440x900.png`](assets/2026-10-22-surfaces-post-after-1440x900.png) — 截图三：正文与目录从照片上消失，出现实心纸面 + 浮起档目录卡
- [`2026-10-22-surfaces-about-before-1440x900.png`](assets/2026-10-22-surfaces-about-before-1440x900.png) / [`…-about-after-1440x900.png`](assets/2026-10-22-surfaces-about-after-1440x900.png) — 截图二：作者卡与相邻区域并入同一族表面，风格割裂消失
- [`2026-10-22-surfaces-home-before-1440x900.png`](assets/2026-10-22-surfaces-home-before-1440x900.png) / [`…-home-after-1440x900.png`](assets/2026-10-22-surfaces-home-after-1440x900.png) — 截图一：幽灵按钮与侧栏
- [`2026-10-22-surfaces-post-after-dark-1440x900.png`](assets/2026-10-22-surfaces-post-after-dark-1440x900.png) — 暗色模式（改造前目录链接 1.29:1）
- [`2026-10-22-surfaces-post-after-390x844.png`](assets/2026-10-22-surfaces-post-after-390x844.png) — 移动端阅读页
