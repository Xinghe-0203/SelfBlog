# 前端观感重做方案（v2.0.30）

> 背景：作者对当前前台观感不满（首页信息区没有半透明、关于页作者卡不能配背景、后台网站设置过于杂乱）。
> 本文件是这一批改动的**设计稿 + 验收清单**，实现细节以代码与 `CHANGELOG.md` 为准。

## 一、作者提的问题与真正的根因

| 编号 | 作者原话 | 表面诉求 | 真正的根因（已核实） |
|------|----------|----------|----------------------|
| 1 | 「1 位置请在后台可以设置背景图片和大小和内容」 | 首页介绍区可配背景图/尺寸/文案 | 首页介绍区 `.intro-shell` 的背景图写死为「第一篇文章的封面」、宽度写死 43%、文案写死取站点资料；页面编辑器里根本没有横幅这一档 |
| 2 | 「2 位置你看看，一点半透明效果没有」 | 卡片要有半透明 | `stores/site.ts` 的 `effectiveSurfaceOpacities()` 在**无壁纸**时把三档表面压成同一个值，`appearance.css` 又有一条 `html:not([data-wallpaper])` 规则把三档强制拉平 —— 于是纯色/渐变画布下卡片必然是实心白块 |
| 3 | 「3 位置也要有可以设置背景图片和大小内容」 | 关于页作者卡可配背景图/尺寸/文案 | `.author-profile` 只有 `background: var(--surface-raised)`，作者卡是「作者资料」而不是「视觉横幅」 |
| 4 | 「工作台这里的 SEO 就没必要了」 | 页面编辑器里的 SEO 档多余 | 页面级 SEO 与「SEO 管理」页（文章级）职责重叠，而页面编辑器是**作者配置页面的地方**，不该塞搜索元数据 |
| 5 | 「网站设置里面的内容太多了太杂乱了」 | 后台信息架构重排 | 7 段平铺表单混了「站点资料 / 外观 / 内容互动 / 系统安全 / AI 集成」五类关注点 |

## 二、设计方向

沿用仓库已有的**清透纸面**语言（`--surface-paper/raised/control` 三级表面 + 圆角 token + 语义色阶），
把它补成真正可用的系统，而不是推翻重做：

- **纸面优先**：正文永远压在纸面上，照片/渐变是画布，纸面浮在画布上。
- **三级表面台阶**：纸面（最亮）→ 卡片（次之）→ 控件（最深），任何时候都能读出层次。
- **玻璃感默认开**：卡片 88% + 12px 背景模糊；作者可调（60–92% / 0–40px），也能一键关成实心。
- **成品预设**：4 套搭配好的主题（纸间 / 墨青 / 暖砂 / 暮紫），一点即换，换完仍可微调单项。

### 2.1 主题预设（`frontend/src/constants/themePresets.ts`）

| 预设 | 皮肤 | 强调色 | 字体 | 圆角 | 阴影 | 画布渐变 | 卡片 |
|------|------|--------|------|------|------|----------|------|
| 纸间 | ocean | `#165DFF` | system | default | default | `#f4f7fc → #e9eef7` | 88% / 12px |
| 墨青 | ink | `#1F7A72` | serif | sharp | flat | `#f3f6f4 → #e4ece8` | 90% / 10px |
| 暖砂 | sand | `#C2703F` | serif | soft | deep | `#faf6f0 → #efe3d6` | 88% / 12px |
| 暮紫 | dusk | `#9D8BF5` | sans | soft | deep | 深色画布 | 88% / 12px |

预设只写**已有**的站点设置键（themeSkin / themePrimaryColor / fontFamily / radiusScale / shadowDepth /
backgroundType / backgroundGradientFrom / backgroundGradientTo / surfaceOpacity / surfaceBlur），
后端零迁移；新皮肤 `ink` / `dusk` / `sand` 需要同步进后端 `themeSkin` 白名单。

### 2.2 玻璃感的数学（`stores/site.ts`）

```
glassMode = off            → paper/raised/control 全 100，无模糊（回到实心观感）
glassMode = on, 无壁纸      → paper = min(100, base+6)，raised = base，control = base−6
glassMode = on, 有壁纸      → paper = base（半透明纸面，照片透上来）
                            raised = max(base, 88 浅 / 94 深)，control = raised−6
base = clamp(surfaceOpacity, 60, 92)
```

`92` 是上限而不是 `100`：100% 与实心没有区别，作者会以为「开了却没效果」。
**纸面从 100% 变成半透明会削弱照片上的文字对比度**，所以玻璃感开启时背景照片强制至少压 30% 遮罩
（`GLASS_MIN_OVERLAY`）—— 作者调大照常生效，调小被这个下限兜住。

## 三、页面级横幅（位置 1 与位置 3）

### 3.1 契约

`PageStyle` 增加一个可选的 `banner` 分量（`frontend/src/types/page.ts`，
后端 `dto/site/PageModels.java`）：

```
banner: {
  image?: string          // http(s):// 或 /uploads/ 开头；服务端复用 imageUrl() 白名单
  fit?: 'cover'|'contain' // 背景图适配方式
  position?: 'center'|'top'|'bottom'
  height?: 'compact'|'standard'|'tall'   // 190 / 250 / 330px
  overlay?: number        // 0–80，底图上的深色遮罩百分比
  tone?: 'auto'|'light'|'dark'            // 横幅上文字的明暗
  layout?: 'split'|'centered'|'stacked'   // 左右分栏 / 居中 / 上下堆叠
}
```

只有 `home` 与 `about` 两个内置页允许 banner（后端 `PageCatalog.validate` 拦截），
其余页面传入即报错。

### 3.2 后台入口

**页面管理 → 编辑页面 → 样式**（不是「网站设置」）。样式档新增「横幅」区块：

- 背景图：`SiteAssetField kind="background"`（服务端 `SiteAssetKind.BACKGROUND` 已存在，≤8MB，
  只收 png/jpg/jpeg/webp），同时允许粘贴外链；
- 适配 / 焦点 / 高度 / 遮罩 / 文字明暗 / 排布 六个下拉与滑杆；
- 预览复用页面编辑器既有的预览档（`PagePreviewContent` 直接挂真实 `Home.vue` / `About.vue`，
  所以所见即所得，不需要第二套预览实现）。

### 3.3 渲染

- 首页：`.intro-shell` 变成真正的横幅容器 —— 背景图铺满、遮罩压底、文案按 layout 排布。
- 关于页：`.author-profile` 同样支持横幅（背景图 + 遮罩），头像与文字层级保留。
- 服务端渲染的阅读 HTML（`ReadingContent` + `seo-reading.css`）同步支持，
  保证爬虫拿到的 HTML 也带横幅。

## 四、后台信息架构（位置 4 与 5）

### 4.1 页面编辑器

去掉 **SEO 档**（作者原话：「工作台这里的 SEO 就没必要了」）。内容档 / 样式档 / 预览档保留。
搜索收录等文章级 SEO 仍在「SEO 管理」页，那才是它该在的地方。

### 4.2 网站设置 → 四个分组

| 分组 | 收纳内容 |
|------|----------|
| 站点资料 | 网站名称/简介/作者/头像/Logo/favicon/页脚/域名 |
| 外观 | 主题预设卡、皮肤、强调色、画布背景（纯色/渐变/图片/遮罩/固定）、玻璃感开关、卡片不透明度与模糊、排版深度自定义 |
| 内容与互动 | 评论开关、背景音乐、留言审核 |
| 系统与安全 | 后台路径与登录密码、匿名访问统计、模型接口入口（指向「SEO 管理」页） |

「AI 辅助与 SEO」整段**移出网站设置**，模型连接（服务商/地址/密钥/模型/超时/思考强度/最大 token）
迁到「SEO 管理」页顶部的「模型连接」卡片，由 `frontend/src/components/admin/LlmConnectionPanel.vue` 承载，
与该页的文章级 SEO 能力放在一起。

### 4.3 工作台

「工作台」首页去掉「SEO 管理」卡（与站点级 SEO 重复），换成「页面管理」卡 ——
页面的视觉与文案才是这一版的主战场。

## 五、验收清单

2026-10-23 实测记录（真实 Spring Boot + SQLite + 作者真实设置 + Chrome 1440×1000）：

- [x] 首页介绍区可在后台换背景图、调尺寸（高度/适配/焦点）、调遮罩、排布方式，并改标题/副标题/署名
      —— 写入 `banner = { image, fit:'cover', position:'center', height:'tall', overlay:35, tone:'light', layout:'split' }`
      后 DOM 实测 `.intro-shell--banner-split`、`min-height:330px`、`--banner-overlay:35`、`data-banner-tone="light"`；
      标题/副标题/署名仍走原有 `text` 覆盖。
- [x] 关于页作者卡同样可配背景图与尺寸 —— `.author-profile--banner-centered` 渲染正确，白字压图可读。
- [x] 纯色与渐变画布下，卡片都能读出「纸面 → 卡片 → 控件」三级台阶，不再是一整块白
      —— 根因是 `appearance.css` 的 `html:not([data-wallpaper])` 规则把三档强制拉平，已改为分档；
      默认渐变画布实测 `.intro-shell` `0.88` / `.article-list` `0.88`（不同底色）/ `.archive-callout` `0.82`。
- [x] 玻璃感可一键关闭（回到完全实心），不透明度与模糊可调 —— `glassMode=off` 时三档全 100、blur 归零。
- [x] 4 套主题预设可一键切换，自定义项不被重置 —— 预设只写 10 个键，其余设置原样保留。
- [x] 页面编辑器不再有 SEO 档；文章级 SEO 仍在「SEO 管理」页，且模型连接也搬到了那里
      —— `EDITOR_TABS = ["内容","样式","预览"]`；`[data-llm-panel]` 在 SEO 页顶部实测存在。
- [x] 网站设置收敛为四个分组 —— `[data-tab] = profile / appearance / interaction / system`。
- [x] 工作台不再出现「SEO 管理」卡 —— 5 张卡为文章管理 / 页面管理 / 内容体检 / 访客记录 / 网站设置。
- [x] 前端闸门：`lint:check` / `format:check` / `type-check` / `test:run` / `test:deployment` / `build` 全绿
      —— 55 文件 / 534 tests、0 error（42 warning 与基线一致）、27/27 部署契约、构建 3.03s。
- [x] 后端闸门：`backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"` 全绿
      —— 1288 tests / 0 failures / 0 errors（基线 1254，本批新增 34）。

未验证：真实 MySQL 与 Docker 部署（本机无守护进程，仍由 CI 的 `mysql` / `docker` job 覆盖）；
真实 Vercel 部署。
