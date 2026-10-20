# 字阶收敛与首页封面面板修复 — 真机验证记录 (2026-10-20)

对应改动：`frontend/src/views/Home.vue`（封面面板渲染条件）、`frontend/src/views/__tests__/Home.test.ts`、
`frontend/src/styles/variables.css`（新增 `--text-2xs`）、`frontend/src/**` 的 402 处 `font-size` 归一，
以及 `frontend/src/views/post/PostEdit.vue` 的移动端横向溢出修复。CHANGELOG 条目见
`CHANGELOG.md` 的「未发布 — 首页封面面板与全站字阶收敛 (2026-10-20)」。

## 方法与工具链

- 浏览器：Chromium **1217**（`C:/Users/Zora/AppData/Local/ms-playwright/chromium-1217/chrome-win64/chrome.exe`），
  经全局 Playwright **1.59.1** 以 headless 驱动；视口 **1440x900** 与 **390x844**。
- **before** = 原始 `HEAD`（`7c796f0`）：由**另一台 Vite（端口 3100）**服务一份字节级相同的 `frontend/` 副本
  （已核对：副本下 165 个 `frontend/src` 文件与 `git show HEAD:...` 全部一致，0 处差异）。
- **after** = 工作区改动：由 Vite（端口 3000）服务。两个前端都把 `/api`、`/uploads` 代理到同一个
  运行中的后端 `:8825`。截图期间未重新播种、未改动已播种内容。
- 内容经**管理端 HTTP API** 播种（6 篇文章，其中 3 篇有封面 / 3 篇无封面；3 个标签；1 个页面），
  **从未手改数据库文件**。
- 关键原始产物（临时目录，不随仓库提交）：`.tmp-vis/after-results.json`、`.tmp-vis/overflow-scope.json`、
  `.tmp-vis/overflow-regression.json`、`.tmp-vis/overflow-minimal.json`、`.tmp-vis/write-recheck.json`、
  `.tmp-vis/final-check.json`、`.tmp-vis/breakage-scan.json`、`.tmp-vis/font-table.md`。
  持久化的截图见 `docs/reviews/assets/2026-10-20-*.png`。

## 1. 结论

| 项目 | 结果 |
|---|---|
| 12 个必测选择器的 computed 字号（桌面） | **11/12 与预期逐条一致**；第 12 个（`.search-prompt > span`）在 DOM 中不存在 —— 死规则，见 §4 |
| 文章详情 `.markdown-body` = 18px | 确认 |
| 编辑器 `.content-input` = 16px | 确认（两个视口） |
| `.btn-primary` = 16px | 确认（两个视口） |
| 两个视口下无溢出 / 裁切 / 标题碰撞 | 确认（在修掉写文章页溢出之后；修复前 390x844 的 `/admin/write` 横向溢出，见 §3） |
| 无封面首页右带 43% 空灰矩形 | **修复前存在，修复后完全消失**（§2） |
| 有封面首页 | 未受影响 —— 封面图照常渲染且请求 200（§2） |

## 2. 无封面首页封面面板（核心证据）

**修复前右带确实有一块 43% 宽的空灰矩形；修复后完全消失。** 三种独立方法互相印证：

**(a) DOM 存在性 + computed 样式 + 几何**

| | before 1440x900 | after 1440x900 | before 390x844 | after 390x844 |
|---|---|---|---|---|
| `.intro-visual` 元素 | 存在 | **不存在（`null`）** | 存在 | **不存在（`null`）** |
| 宽度 | 459.2px | — | 242.9px | — |
| 占 `.intro-shell` 宽度比（壳 1070px / 354px） | **42.9%** | — | **68.6%** | — |
| computed `background-color` | `rgb(231, 234, 240)` = `--bg-tertiary` | — | `rgb(231, 234, 240)` | — |
| 其内 `<img>` | **无** | 无 | **无** | 无 |

42.9% 即 `frontend/src/views/Home.vue:213` 的 `width: 43%`；390px 下 `Home.vue:399-401` 的
`@media (max-width:600px)` 把它放宽到 69%，实测 68.6%。

**(b) 计数断言** `document.querySelectorAll('.intro-visual').length`：before 1 / 1，after **0 / 0**。
两个状态下 `introShellCount` 都是 1 —— 壳仍在，消失的只是空面板。除路由拦截过的 `/` 外，
还在**真实未被拦截**的 `/?page=2`（该页 6 条记录 cover 全为 `null`）上复现同一结论。

**(c) 右带像素统计**（裁切 `.intro-shell` 右侧 43%，460x213，见
[`2026-10-20-hero-nocover-before-1440x900.png`](assets/2026-10-20-hero-nocover-before-1440x900.png) /
[`2026-10-20-hero-nocover-after-1440x900.png`](assets/2026-10-20-hero-nocover-after-1440x900.png)）

| 指标 | before | after |
|---|---|---|
| 命中 `--bg-tertiary` `#e7eaf0`（±3）的像素 | 3,771（3.8%） | **28（0.0%）** |
| 命中 `--surface-secondary` `#f9fafc`（±3）的像素 | 16,017（16.3%） | **96,739（98.7%）** |
| 平均亮度 | 240.7 / 242.7 / 246.5 | 248.7 / 249.8 / 251.9 |
| 两图差异 >4/255 的像素 | — | 74,454（占该带 76.0%） |

残留的 28 个 `--bg-tertiary` 像素是字形抗锯齿边缘，不是面板。

整页对照：
[`2026-10-20-home-nocover-before-1440x900.png`](assets/2026-10-20-home-nocover-before-1440x900.png) /
[`2026-10-20-home-nocover-after-1440x900.png`](assets/2026-10-20-home-nocover-after-1440x900.png)。

**有封面首页未受影响**：`.intro-visual` 两个状态下几何相同（1440x900 为 459.2px = 42.9%；
390x844 为 242.9px = 68.6%），`<img>` 报 `complete: true`、`naturalWidth: 1200`、`naturalHeight: 800`，
6 个图片请求全部 200、0 个非 200。

修复点：`frontend/src/views/Home.vue:91` 的 `v-if="showModule('cover') && heroCover"`。

## 3. 横向溢出：修复前后

`document.documentElement.scrollWidth`：

| 视口 | 页面 | 修复前 | 修复后 |
|---|---|---|---|
| 1440x900 | write / dashboard / posts / visitors / albums / settings | 1440 | 1440 |
| 390x844 | **write** | **854**（视口 390） | **390** ✓ |
| 390x844 | dashboard / posts / visitors / albums / settings | 390 | 390 |

**该溢出是既有缺陷，不是字阶归一引入的。** 决定性对照（`.tmp-vis/overflow-regression.json`）：
把**原始 HEAD 字号**（13/15/21px）注入工作树后仍是 `scrollWidth 854`，而只加一条 `min-width: 0`
就回到 390 —— `A_asis` 854 / `B_headFontSizes` 854 / `C_minWidthZero` 390 / `D_both` 390 /
`E_minmaxOnly` 390 / `F_minWidthOnly` 390。

根因：`PostEdit.vue` 的 `.edit-container` 用裸 `1fr`（等价 `minmax(auto, 1fr)`），网格项默认
`min-width: auto` 让 `.editor-main-wrapper` 在窄视口不收缩，`@media (max-width: 768px)` 里
`.toolbar { overflow-x: auto }` 因此失效、溢出转嫁给整页。仓库既有惯例（`Home.vue`、`Archives.vue`）
已写 `minmax(0, 1fr)`，`PostEdit.vue` 是漏网者。

修复后复测（`.tmp-vis/write-recheck.json`）：

| 视口 | `scrollWidth` | grid-template-columns | `.editor-main-wrapper` min-width | `.toolbar` scrollW / clientW | `.editor-sidebar` |
|---|---|---|---|---|---|
| 1440x900 | 1440（无溢出） | `764px 320px` | `0px` | 762 / 762（不滚动） | 320px，x=1072 —— **仍在右列** ✓ |
| 390x844 | **390**（无溢出） | `326px` | `0px` | **820 / 324，`overflow-x: auto`** ✓ | 326px（堆叠） |

即 1440px 布局未被 `minmax(0, 1fr)` 破坏：侧栏仍占 320px 列、主栏 764px。
390px 下工具栏恢复为自身横向滚动（`PostEdit.vue:1892-1896` 的原设计意图），
残留的 `overflowers`（`.toolbar-group`、`.tool-btn`，右缘 455/504）都在该容器**内部**，
`documentElement.scrollWidth` 保持 390。

截图：[`2026-10-20-write-overflow-before-390x844.png`](assets/2026-10-20-write-overflow-before-390x844.png) /
[`2026-10-20-write-overflow-after-390x844.png`](assets/2026-10-20-write-overflow-after-390x844.png)。

## 4. `.search-prompt > span` 是死规则，不是缺陷

`frontend/src/views/search/Search.vue:297-301` 为 `.search-prompt > span` 写了 `--text-5xl`（48px），
但模板空状态渲染的是 `<Icon name="search" :size="28" />` 加一个 `<p>`（`Search.vue:142-145`），
**没有这个 span**，四个格子里全部 `NOT FOUND`。原先的 `<span>⌕</span>` 在提交 `3e1249a` 被换成
Icon 组件，故该规则自 `HEAD` 起就是死代码，**无视觉影响**（空状态里可见的是 28px 细描边放大镜图标），
本次刻意不动。字阶归一只是把它的 `2.8rem` 折成了 `var(--text-5xl)`。

## 5. 字号前后对照（computed，实测）

`NOT FOUND` = 该页无匹配元素。`.markdown-body h3` 用注入节点测量（已播种正文里没有 `<h3>`）。

| 选择器 | 页面 | 1440 before | 1440 after | 390 before | 390 after | 桌面预期 |
|---|---|---|---|---|---|---|
| `.markdown-body` | 文章详情 | 17.2px | **18px** | 16px | 16px | 18px ✓ |
| `.markdown-body h1` | 文章详情 | 29.6px | **30px** | 24.8px | 24px | 30px ✓ |
| `.markdown-body h2` | 文章详情 | 24.8px | **24px** | 21.6px | 20px | 24px ✓ |
| `.markdown-body h3` | 文章详情（注入） | 20px | 20px | 20px | 20px | 20px ✓ |
| `.summary-grid strong` | 后台 Dashboard | 22px | **24px** | 22px | 24px | 24px ✓ |
| `.metrics strong` | 后台 Visitors | 22px | **24px** | 22px | 24px | 24px ✓ |
| `.section-heading h2` | 首页 | 19px | **20px** | 18px | 18px | 20px ✓ |
| `.modal-header h3` | 相册弹窗 | 17px | **18px** | 17px | 18px | 18px ✓ |
| `.btn-primary` | 登录页 | 15px | **16px** | 15px | 16px | 16px ✓ |
| `.content-input` | 写文章页 | 15px | **16px** | 15px | 16px | 16px ✓ |
| `.article-list--featured ... .article-title` | 首页 | 22px | **24px** | 20px | 20px | 24px ✓ |
| `.search-prompt > span` | 搜索空状态 | NOT FOUND | NOT FOUND | NOT FOUND | NOT FOUND | 死规则（§4） |
| `.auth-header h1` | 登录页 | 22px | **24px** | 22px | 24px | 24px ✓ |
| `.article-header h1` | 文章详情 | 38px | 38px | 28px | **30px** | 移动端断点 |
| `.intro-copy h1` | 首页 | 38px | 38px | 29px | **30px** | 移动端断点 |
| `.title-input` | 写文章页 | 24px | 24px | 21px | **20px** | 仅移动端覆盖 |

说明：

- `.section-heading h2` 桌面 20px、390px 仍 18px —— `frontend/src/views/Home.vue:406-408` 的
  `@media (max-width:600px)` 把它钉在 `--text-lg`，移动端覆盖**未被改动**。
- `.article-header h1` / `.intro-copy h1` 桌面是随视口流动的 `clamp()`（1440px 下 38px，刻意保留），
  390px 下是固定档位（28→30 / 29→30）。
- `.metrics strong` / `.summary-grid strong` 在**两个视口**都是 22→24：
  `frontend/src/views/admin/Dashboard.vue:224` 与 `frontend/src/views/admin/Visitors.vue:333`
  的移动端覆盖也一并归到了字阶上。
- 归一后全仓普查（独立复算）：**403 处声明、16 个不同取值，仅 7 处非 token**，全部命中三类例外 ——
  `frontend/src/styles/base.css:11`（根 `16px`）、`frontend/src/styles/base.css:99`（`inherit`）、
  `frontend/src/styles/typography.css:129`（`0.875em`）、`frontend/src/views/post/PostDetail.vue:873`
  （`clamp(28px, 3.2vw, 38px)`）、`frontend/src/views/post/PostDetail.vue:1042`（`0.7em`）、
  `frontend/src/views/post/PostDetail.vue:1064`（`0.88em`）、`frontend/src/views/Home.vue:197`（`clamp(...)`）。

## 6. 实际观察到的布局破损

| 观察 | 证据 |
|---|---|
| **`/admin/write` 在 390x844 横向溢出**（仅修复前；`scrollWidth` 854 vs 视口 390），修复后为 390 | §3 的两组 JSON 与前后截图 |
| `.markdown-body` 代码块在 390px 超出视口（`code.language-js` 宽 400.3px、右缘 436.3） | **已被容器收住，不是页面破损**：父 `<pre>` 有 `overflow-x: auto`（scrollW 434 / clientW 352），外层 `div.code-block` 为 `overflow-x: hidden`；`documentElement.scrollWidth` 保持 390 |

其余全部干净：7 个页面 × 2 个视口 × 8 个标题选择器，`clipped=false` / `overflowsViewport=false`；
各页 `documentElement.scrollWidth === clientWidth`；除下方那条已知的 `visitor/track` 403 外，
控制台无其它错误。唯一的两行换行是 390px 下 `.intro-copy h1` 的正常句子换行，非截断。

## 7. 本轮**未**验证的事项

- 只测了 **1440x900 与 390x844**，没有测平板/中间断点（`PostEdit` 的 768px / 1020px 覆盖只读代码、未实测）。
- 只用 Chromium，**未测** Firefox / WebKit / 深色模式 / `prefers-reduced-motion`。
- 抓了 10 个页面、扫了 7 个，**未覆盖** `/archives`、`/gallery`、`/about`、`/tag/:id`、`/pages/:slug`、
  带结果的搜索页，以及后台 posts/tags/comments/pages/settings。
- **没有**逐条扫描全部 402 处字号声明（只按抽样选择器真机读数 + 全仓静态普查）。
- `.markdown-body h3` 是注入节点证据，不是真实内容。
- **没有**做无障碍 / 对比度 / 焦点态检查；hero 之外的其他 9 组只做整页截图，**没有**像素级 diff。
- 前后两组截图里各有 13 个 `POST /api/visitor/track` 的 **403**（每次页面加载 1 个，
  修复前后一致；`backend/.../config/security/SecurityConfig.java:161` 已放行该路径）——
  **不是本次改动引入的回归**，但其被拒的根因**未查明**，属独立问题。
- 视觉核对**全部跑在 Vite dev server 上，从未跑过生产构建**。
- 字号改动**没有任何测试断言钉住**：全仓 `*.test.ts` 中不存在 `font-size` 断言。
  唯一新增的哨兵是 `frontend/src/views/__tests__/Home.test.ts` 里对 `.intro-visual` 整块消失的断言。
