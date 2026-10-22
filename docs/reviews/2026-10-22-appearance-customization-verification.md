# 外观深度自定义（v2.0.29）真机验证记录

日期：2026-10-22
范围：11 个新外观设置键（排版 / 布局 / 呈现 / 动效）+ 只读路径限流修复
环境：Playwright 1.59.1 + Chromium 147（`C:/Users/Zora/AppData/Roaming/npm/node_modules/playwright`），
Vite dev `http://localhost:3000` + 后端 jar `http://localhost:8825`，
数据库用 `backend/campus_blog.db` 的副本 `.tmp-gate/verify.db`（**开发库未被写入**），
视口 1440×900 与 390×844。

## 为什么必须真机验证

这三个缺陷**单元测试与静态检查都抓不到**，只有逐元素读 computed style 才会暴露：

1. 后台写了「字体族」选项，但正文字体纹丝不动（继承输给 scoped 直接声明）；
2. 后台写了「阴影层次」选项，但三档在页面上看起来一样（内容容器根本没有 `box-shadow` 声明）；
3. 11 个键的「默认档」是否真的等于改造前，只能靠像素/读数比对。

## 探针

`.tmp-gate/verify.cjs`（8 档设置 → 逐档 `page.goto` + `page.evaluate` 读 computed style + 截图）、
`.tmp-gate/fix-verify.cjs`（修复后复验字体族与阴影）、
`.tmp-gate/shadow-probe.cjs`（逐元素枚举内容区所有带 `box-shadow` 的节点，用于定位缺陷 2）。
关键原始产物（临时目录，不随仓库提交）：`.tmp-gate/verify-result.json`、
`.tmp-gate/fix-verify-result.json`、`.tmp-gate/shadow-result.json`、`.tmp-gate/backend-full.log`。

探针要点：必须 CommonJS（`.cjs`）+ `require()` 绝对路径的 playwright；ESM import 绝对 Windows
路径会报 `ERR_UNSUPPORTED_ESM_URL_SCHEME`。登录响应字段是 **`data.token`**（不是 `accessToken`）。

## 结果一：默认档零回归（逐读数比对）

设置写入全部默认值后：

| 读数 | 值 |
|------|-----|
| 7 个 `data-ap-*` 属性 | 全为 `null` |
| 4 个 `--ap-*` 变量 | 全为空串 |
| `.intro-shell` | height 213px / padding `31px 34px` / radius 16px |
| `.intro-visual` | width 459px |
| 首页 h1 | font-size 38px |
| `.reader-layout` grid | `760px 200px` |
| `.markdown-body` | font-size 16px / line-height 28px |
| 正文段落 margin-bottom | 16px |

再改到非默认档、然后**复原默认**，逐项读数与首次默认档**完全相同** ⇒ 零回归成立。

## 结果二：字体族（修复后）

修复前：`POST_MAX` 档 `h1FontFamily` 已是衬线，但 **`bodyFontFamily` 仍是 sans**（半截效果）。
根因：`customize.css` 只在 `.app-main` 上写 `font-family` 靠继承，而 `PostDetail.vue` 有 11 处
`font-family: var(--font-display)` 的直接声明，直接声明永远赢过继承。

修复：在该作用域内**重定向 token 本身**（`--font-sans` → `--ap-font-body`、
`--font-display` → `--ap-font-heading`），既有声明自动跟随。

`.tmp-gate/fix-verify-result.json` 的 `FONT_VERDICT` 全为 `true`：

| 判据 | 结果 |
|------|------|
| serif 档正文 `font-family` 变化 | ✅ |
| serif 档 h1 / `.markdown-body h2` / `.article-end p` / `.toc-card h2` / 评论区标题变化 | ✅ 全部 |
| mono 档正文变化 | ✅ |
| serif 档代码块 `font-family` 不变 | ✅ 逐字符相同 |
| serif 档行内 `code` 不变 | ✅ 逐字符相同 |
| 导航栏字体栈三档不变 | ✅ 逐字符相同 |
| 页脚字体栈三档不变 | ✅ 逐字符相同 |

serif 实际栈：`"Iowan Old Style", "Palatino Linotype", Palatino, Georgia, "Songti SC", "Source Han Serif SC", "Noto Serif CJK SC", SimSun, serif`。

## 结果三：阴影层次（修复后）

修复前（`.tmp-gate/shadow-result.json`）：`flat_home` / `default_home` / `deep_home` **三档各只有
1 个带阴影元素，且完全相同**——`div.page-frame blog-home` 的
`rgb(227, 230, 236) 0px 0px 0px 1px inset`，来自 `appearance.css` 的描边语义，不是 `--shadow-*`。
即：后台的「阴影层次」在内容区是**承诺落空**。

修复：只给「加深」档接线 7 个内容容器（`.article-list` / `.intro-shell` / `.sidebar-block` /
`.author-profile` / `.album-card` / `.state-panel` / `.archive-callout`）→ `box-shadow: var(--shadow-xs)`。
**不给「默认」档接线是刻意的**——默认档必须逐像素等于改造前。

| 档位 | 内容容器 | `.toc-fab` | 导航栏 |
|------|----------|-----------|--------|
| flat | 全部 `none` | `none` | 不变 |
| default | 全部 `none` | `rgba(20,32,51,0.1) 0px 8px 28px` | 不变 |
| deep | 全部 `rgba(20, 32, 51, 0.06) 0px 2px 4px 0px` | `rgba(20,32,51,0.2) 0px 16px 44px` | 不变 |

`SHADOW_VERDICT` 的 `deep_*` 与 `flat_*` 判据全为 `true`。
（`navUnchanged` 字段为 `false` 是**探针自身的断言写错**：它把导航栏阴影与 `null` 比较，而导航栏
本来就有既有阴影。直接读 `SHADOW_*` 各档的 `__nav` 字段可证：六个档位的字符串**逐字符完全相同**，
即导航栏确实没有被这 11 个键影响。）

## 结果四：其余维度

| 维度 | 档位 | 读数 |
|------|------|------|
| 圆角 | round / sharp | 卡片 radius 26px / 0–8px |
| 密度 | compact / comfortable | 首页内容 gap 18px / 40px |
| 首页头图 | compact / minimal / off | hero 183px、visual 34%、h1 30px；minimal 隐藏 visual 与遮罩但**保留 h1**；off 撤外框 |
| 文章封面 | wide / hidden | `margin-inline:-24px` 出血 / `display:none` |
| 动效 | off / lively | `transitionDuration 1e-05s` / 380ms、220ms |
| 阅读栏宽 | 832 / 640 | `.reader-layout` grid `832px 200px` / `640px 200px` |
| 字号·行高·段距 | 19 / 1.9 / 2.0 | 正文 19px / 39.9px / 段距 28.8px |

**minimal 档保留 h1 是承重决定**：整段 `display:none` 会让页面失去唯一的 `<h1>`，对读屏与 SEO 都是硬伤。

## 结果五：移动端与错误

- 390×844：`document.documentElement.scrollWidth === 390`（无横向溢出）；
  compact 档 hero 264px、visual 194px / opacity 0.6；post 页 `colWidth 354`、`bodyFont 18px`。
- `PAGE_ERRORS=[]`（无 console error / pageerror）。

## 结果六：只读路径限流修复（同一 IP 实测）

| 请求 | 修复前 | 修复后 |
|------|--------|--------|
| `GET /api/post/1..12` | 第 11 次 429 | 60 次全 200，第 61 次才 429 |
| `GET /api/guest-comments/post/{id}` | 第 6 次 429 | 60/min |
| `GET /api/gallery/albums` | 60/min（合理） | 未改动 |
| `POST /api/guest-comments` | 5/min | 未放宽 |

上表的「修复后」一列在**最终打包的 `release/selfblog-2.0.29.jar`** 上复测过一次（端口 18888，
DB 用 `backend/campus_blog.db` 的副本）：连续 61 次 `GET /api/post/{1..61}` → 前 15 次 200、
16–60 次 404（文章不存在）、**第 61 次才 429**；`GET /api/site/public` 正常下发 11 个新键
（`fontFamily=system` / `baseFontSize=16` / `bodyLineHeight=1.75` / `readerWidth=760` /
`paragraphSpacing=1` / 其余 6 个枚举均为 `default`），公开设置里 `llm*` 键计数为 **0**。
即打包产物与源码闸门通过时的行为一致。

## 截图

`docs/reviews/assets/` 下 8 张（`2026-10-22-custom-*`）：default / max / min × 首页·文章详情
@1440×900，以及移动端 390×844 两张。

## 已知未覆盖

- 真实 Vercel 部署与搜索引擎收录仍未验证（与 v2.0.27 相同的既有边界）。
- 壁纸（`--ap-bg-image`）与 11 个新键的组合只抽测了默认壁纸；四级表面 × 四档头图的笛卡尔积未穷举。
- `layoutDensity` 在后台界面**不生效**（选择器限定 `.app-main:not(.app-main--admin)`），这是设计意图，不是缺陷。
