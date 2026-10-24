# CLAUDE.md - 个人博客 Vue 3 前端 (v2.0.29)

## 项目概述

这是单人作者个人博客的前端项目，基于 Vue 3 + Vite 构建。访客只读、搜索，作者通过管理员后台写作管理。当前版本 v2.0.29。

## 常用命令

```bash
npm ci                 # 安装锁定依赖（Node ^22.22.2 || ^24.15.0 || >=26.0.0，engine-strict）
npm run dev            # 启动开发服务器 (端口 3000)
npm run build          # 构建生产版本（先 vue-tsc --noEmit，再 Vite）
npm run preview        # 预览生产版本
npm run lint:check     # 只读 ESLint 检查（npm run lint 会改写文件）
npm run type-check     # vue-tsc --noEmit（必过闸门）
npm run test:run       # Vitest 单次运行（当前 56 文件 / 554 tests；原始基线 210）
npm run test:deployment # node:test：Vercel 代理生成器 + Docker/Compose 源契约（27 项）
```

> 本机直接把 `npm run <script>` 跑在系统 PATH 上会失败（PATH 里有一条不配对的引号，
> 垫片解析不到裸 `node`）。用仓库内 provisioned Node 兜底：
> `$env:PATH = "C:\…\backend\target\frontend\node;$env:PATH"`（该目录由 Maven 插件生成，
> `target/` 被清理后需先跑一次 Maven 构建）；或直接调用 `node_modules/.bin/` 下的可执行垫片。

## 技术栈

- Vue 3.5.44 (Composition API)
- Vite 6.4.4
- Vue Router 4.6.4
- Pinia 2.3.1
- Axios 1.20.0
- Marked 12.0.2 (Markdown)
- DOMPurify 3.4.17 (XSS 防护)
- TypeScript 6.0.3 + Vitest 4.1.11（单元测试，jsdom + @vue/test-utils）

以上按 package-lock.json 锁定版本核对，不把 package.json 范围下界当作实际版本。

## 项目结构

```
src/
├── api/            # API 接口（admin/gallery/index/media/message/notification/pages/post/public-interactions/seo/site/tag/user）
├── components/     # Vue 组件
│   ├── common/     # 通用组件
│   │   ├── BackToTop.vue     # 返回顶部
│   │   ├── Icon.vue          # 内置 SVG 图标（icons.ts 注册表）
│   │   ├── ImagePreview.vue  # 图片预览灯箱
│   │   ├── Modal.vue         # 通用模态框
│   │   ├── Toast.vue         # Toast 通知
│   │   └── icons.ts          # 图标注册表（新增图标改这里）
│   ├── layout/     # Navbar / Footer / NavbarMusicControl / headless MusicPlayer
│   ├── post/       # ArticleList / MobileTocSheet
│   ├── pages/      # PageFrame / MarkdownContent / ContentPageView（逐页配置与安全正文）
│   └── admin/      # SiteAssetField 等后台字段组件
├── composables/    # useDialog / useConfirm / useLatestRequest / useScrollState / useMarkdownEnhance / useSeoMeta / usePageConfig / usePageSeo / useScrollReveal
├── router/         # 路由配置（modules/ + guards）
├── stores/         # Pinia 状态管理 (site / theme / user / music / pages)
├── styles/         # 样式文件（入口 main.css，customize.css 必须最后导入）
├── types/          # TypeScript 类型定义（含 seo.ts 与 axios-augment.d.ts）
├── utils/          # 工具函数 (crypto / logger / toast / visitor / reading / navigationFit / pageCatalog)
└── views/          # 页面组件（Home/About/Archives、post、pages、admin(+pages)、gallery、search、tag、user、auth、common）
```

## 主要特性

- 单人作者博客：访客只读、搜索、匿名点赞与留言；写作与管理需要管理员登录
- 清透空间设计底座：CSS 变量 token、深色模式、8 套皮肤、内置 SVG 图标；正文实底，导航与临时控件按需使用玻璃
- 文章列表统一由 `components/post/ArticleList.vue` 渲染；首页、搜索、标签共用同一套行样式，无封面不留占位图
- 阅读页把标题/时间/阅读时长放在封面之前；目录侧栏与移动端抽屉共用 1020px 断点
- 后台使用同一套「标题 + 说明 + 内容区」层级；抽屉的焦点、Esc 与滚动锁由 `AdminLayout` 的 `useDialog` 统一负责
- 导航音乐：单一 headless 音频宿主，不自动播放，公开换页连续，进入后台暂停；控件不创建额外 audio
- 阅读体验：目录（移动端抽屉）、代码复制/折叠、标题锚点、图片灯箱、按正文估算阅读时长
- 站点品牌资源上传：Logo / 浏览器图标 / 作者头像 / 背景音乐（`SiteAssetField.vue`）
- 生产为内嵌前端的单 JAR，前端无需独立部署

## 设计系统

- 入口 `src/styles/main.css`，按顺序 `@import` **14** 个模块；设计 token 在 `variables.css`，深色模式覆盖在 `dark-mode.css`，皮肤在 `skins.css`。**`appearance.css` 与 `customize.css` 必须排在最后且 `customize.css` 更后**（作者配置的背景/不透明度/排版要压过皮肤与深色模式里同优先级的声明）
- 深色模式通过 `.dark-mode` 类生效（`stores/theme.ts` 加在 `<html>`，`App.vue` 同时绑在 `#selfblog` 上）
- 皮肤通过 `#selfblog[data-skin='...']` 生效：`ocean`(默认)/`forest`/`lavender`/`sunset`/`midnight`/`ink`/`sand`/`dusk`（`midnight` 与 `dusk` 是自带深色画布的皮肤，各自声明 `color-scheme: dark`）
- 动画工具类: `.fade-in`, `.slide-up`, `.pulse`

### Token 使用约定（重要）

- **不要用 `--gray-900` / `--gray-100` 当「深色表面」**：深色模式会整体反转灰度梯度（`--gray-900` 由 `#111111` 变成 `#EBEBEB`），会让按钮、代码块在深色页面上变成刺眼的亮块
- 需要「永远深色」或「高对比实心」时使用语义 token：
  - `--code-bg` / `--code-text` / `--code-border` — 代码块，两种主题下都保持深色
  - `--contrast-bg` / `--contrast-bg-hover` / `--contrast-bg-active` / `--contrast-fg` — 高对比实心按钮（浅色模式近黑，深色模式自动反转为浅底深字）
- 组件只引用语义 token，不直接写十六进制色值
- **字号一律走 `--text-*` 字阶，不要在组件里写裸 px / rem。** 十档，唯一定义处 `src/styles/variables.css`
  （`--text-2xs: 0.6875rem` 11px / `--text-xs` 12 / `--text-sm` 14 / `--text-base` 16 / `--text-lg` 18 /
  `--text-xl` 20 / `--text-2xl` 24 / `--text-3xl` 30 / `--text-4xl` 36 / `--text-5xl` 48）。
  只有三类例外可保留非 token 写法：`html` 根字号（`base.css` 的 `16px`，rem 的基准）、
  相对父级缩放的 `em`（正文代码与标题锚点）、以及需要随视口流动的 `clamp()`（首页与阅读页大标题）。
  新增档位要连 `variables.css` 注释一起加，不要就地写魔数。
- 圆角：按钮/输入框 8px，内容分组 12–14px，面板 16px；细长导航、圆形头像/播放键才用 `--radius-full`
- `.functional-glass` 是显式功能层，不要把旧 `.glass` 正文卡片改为半透明；透明/高对比降级在 glassmorphism.css
- `#overlay-root` 在 App 的主题根内、路由过渡外；模态层用 `useDialog`，不要另建 body 滚动计数或直接传送到裸 body
- `MusicPlayer` 是唯一音频宿主，常驻 App；共享动作在 `stores/music.ts`，NavbarMusicControl 只消费状态。不可恢复自动播放或把音频移进面板
- 文章行统一走 `components/post/ArticleList.vue`（首页/搜索/标签复用）；新增列表页面不要另写一套行标记
- 后台页面沿用 `AdminLayout` 的「标题 + 说明 + 内容区」层级；列表/表格用容器横向滚动，不要靠隐藏列或缩字号适配
- 后台抽屉只在手机打开时由 `AdminLayout` 内的 `useDialog` 管理焦点、Esc 与滚动锁；关闭时 inert，进入桌面释放模态状态。不要再用各自的 `bodyOverflowCount` 或固定 z-index 遮罩
- 导航链接紧接搜索框，弹性空白放在右侧工具前；选中态 34px 胶囊与 44px 点击区域分开，不把原型触达尺寸直接照搬
- 编辑器保存状态依据成功提交的完整内容快照；摘要/标签也计入未保存状态，旧请求不得把请求期间的新改动标成已保存。离开守卫测试必须通过 RouterView 挂载
- 标签新建/编辑上限 20 字，以后端现有校验为准；不要只改输入框上限而不校验程序提交
- 公开页面配置由 stores/pages + usePageConfig 消费；草稿只在后台编辑器/预览上下文，不能写进公开store或localStorage。PageFrame的局部变量不能覆盖全站主题，未配置时保持原比例
- 页面预览通过空srcdoc真实视口挂载相同安全组件；不要把作者正文拼进srcdoc，不创建公开草稿URL，也不要为预览放宽X-Frame-Options。预览禁止修改文档head
- 页面编辑/导航写入必须携带revision；只把提交快照标记为保存成功，跨页/卸载后忽略迟到响应。恢复默认只改草稿，发布需明确操作
- 服务端阅读HTML由JAR按请求组装（`backend/.../service/site/seo/`），Vue挂载会整体替换`#app`内的`.seo-reading`：不要给它加跨页面依赖或期望它存活。`useSeoMeta`优先读取`meta[name=selfblog-site-origin]`确定canonical/分享图origin，Vite本地开发无标记时才回落浏览器origin
- `vercel.json`不再部署第二份静态前端：`framework:null` + `scripts/build-vercel-proxy.mjs`生成Build Output v3转发到同一源站。不要恢复Vite自动预设、独立`dist`发布或写死域名；部署契约测试用`npm run test:deployment`

## 外观深度自定义（2026-10-22，v2.0.29）

- 11 个新设置键分四个维度：**排版**（`fontFamily` `system|sans|serif|mono`、`baseFontSize` 14–19、`bodyLineHeight` 1.5–2.2、`readerWidth` 640–960、`paragraphSpacing` 0–2）、**布局**（`radiusScale` `none|sharp|default|soft|round`、`shadowDepth` `flat|default|deep`、`layoutDensity` `compact|default|comfortable`）、**呈现**（`homeHeroStyle` `default|compact|minimal|off`、`postCoverStyle` `default|wide|hidden`）、**动效**（`motionIntensity` `off|subtle|default|lively`）
- 全部规则在 `src/styles/customize.css`（**排在 `appearance.css` 之后**，作者配置要压过皮肤/深色模式同优先级声明）。7 个枚举键写成 `#selfblog` 上的 `data-ap-*` 属性，4 个数值键写成 `#selfblog` 上的内联 `--ap-*` 变量（**不是 `<html>`**，否则会绕过作用域改到导航栏）；等于默认值即删掉钩子（`delete root.dataset[...]` / `removeProperty`）
- **硬约束一：导航栏与后台界面完全不动。** 所有选择器限定在 `#selfblog .app-main:not(.app-main--admin)` 内——`Navbar`（`.site-nav-wrap`）与 `Footer` 在 `.app-main` 之外，天然碰不到。改这些规则后必须跑「导航栏逐字符恒定」的真机判据（见 `docs/reviews/2026-10-22-appearance-customization-verification.md`）
- **硬约束二：默认档必须逐像素等于改造前。** 每条规则的 fallback 就是原来的硬编码值（`var(--ap-reader-width, 760px)` 里的 760px 就是 `PostDetail.vue` 原来的 760px）。不要在默认档顺手加一点「更精致」的阴影或间距——零回归靠的是这一点
- **改字体必须重定向 token，不能靠继承**：`PostDetail.vue` 等视图有 11 处 `font-family: var(--font-display)` 的**直接声明**，继承永远输。`customize.css` 在该作用域内重新赋值 `--font-sans` / `--font-display`（标题族用 `--ap-font-heading`），既有声明自动跟随。`--font-mono` **刻意不动**：代码块与行内 code 必须保持等宽
- **阴影档只给「deep」接线内容卡片**（`.article-list` / `.intro-shell` / `.sidebar-block` / `.author-profile` / `.album-card` / `.state-panel` / `.archive-callout` → `box-shadow: var(--shadow-xs)`）。**不给 default 接线是刻意的**（默认档要零回归）。不碰 `.page-frame` 的 `inset 0 0 0 1px`：那是描边语义，换成外投影会让纸面边缘糊掉
- 不覆盖 `--radius-full`（9999px）与 `50%`：胶囊导航、圆形头像/圆形按钮必须真圆
- 11 个键**不进 `PREPAINT_APPEARANCE_VARS`**：它们依赖 `#selfblog` 作用域，预置到 `<html>` 会绕过作用域把导航栏一起改掉
- `App.vue` 的外观 `watch` 依赖数组必须包含全部 11 个新键（当前 14 项），否则改设置不重新应用
- 后台面板在 `views/admin/Settings.vue` 的「外观」分组（`data-tab="appearance"`）里、面板标题「外观深度自定义」；「恢复默认外观」**只改表单不保存**（避免误触）

## LLM 辅助 SEO（2026-10-22）

- 后台入口：侧边栏/工作台/文章列表的「SEO 管理」都指向 `${siteStore.adminRoot}/seo`；`Posts.vue` 的行操作带 `?postId=`，`Seo.vue` 的 `onMounted` 读到就直接打开编辑面板
- 三个写域各管一段，不要互相复制：`views/admin/Settings.vue`（模型连接配置 + 测试连接）、`views/admin/Seo.vue`（体检列表/单篇编辑/批量生成）、`views/post/PostEdit.vue` 侧栏（作者在写文章时顺手改本篇 SEO）
- **密钥掩码哨兵**：`SECRET_MASK = '••••••••'`（8 个 U+2022，无空格）。设置页把已存密钥原样提交回后端表示「保留」；提交空串才是清除。任何「清空输入框」的交互都必须显式写成空串，不能顺手 trim 成哨兵或反之
- `getDetail` 的 **404 表示「这篇还没有 SEO 记录」**，不是错误：`PostEdit.vue` 与 `Seo.vue` 都按「空表单」处理，只有其它状态码才展示错误文案
- 输入框 `maxlength` 用**服务端上限**（title 200 / description 400 / keywords 300 / ogImageUrl 500），不要用建议长度（60/160）当 `maxlength`——浏览器会静默截断作者的长文本。建议长度只用来在 `.char-hint` 上提示超长（`data-over` 上色）
- `generate`/`batchGenerate` 的 axios `timeout` 是 130000（批量串行最多 50 篇），`testConnection` 是 60000。**这些请求绝不能被自动重试**：重试会重复消耗模型额度
- `api/seo.ts` 的 8 个方法都返回 `ApiResponse<T>`，调用方读 `res.data`（信封）再取 `res.data.data`
- SEO 类型只从 `types/seo.ts` 直 import（`types/index.ts` 也做了 re-export，但避免循环依赖时优先直 import）

## AI 辅助功能（2026-10-22，v2.0.28）

- 类型在 `types/llm.ts`（`types/index.ts` 已 re-export），API 在 `api/llm.ts`。**全部方法都带 `_noRetry: true` 与 130000 超时**：超时被自动重试会让一次失败烧掉三倍模型额度与三倍等待时间（`ECONNABORTED` 就在 `RETRYABLE_CODES` 里，这是全站读请求的正确行为、在这里是错的）
- `views/admin/Settings.vue`：服务商下拉 + 一键预设（只改 `llmProvider`/`llmBaseUrl`/`llmModel`，**绝不动密钥输入框**）、模型候选下拉（`seoApi.listModels()`，探测失败静默清空候选并把文本输入留作兜底）、思考模式与强度、`llmMaxTokens`（0 = 不发送该字段）、评论审核开关。**密钥哨兵往返必须保持**：只有作者真的编辑过密钥输入框，提交时才允许替换 `llmApiKey`
- `views/post/PostEdit.vue` 侧栏：AI 摘要（采用 → 回填 `summary`）、标签候选 chip 多选（应用 → **并入**作者已有标签，绝不覆盖）、3 个标题候选（点击 → 填入 `title`）。**三个动作都只改本地表单**，未保存前不落库
- `views/admin/Comments.vue`：单条「AI 审核」+ 批量扫描 + verdict 徽章。**只展示判定与理由，不改评论状态**——改状态必须由作者显式操作
- `views/admin/Albums.vue`：AI alt/图注候选先落本地草稿，再由作者显式保存为照片图注（`PUT /admin/albums/{albumId}/photos/{photoId}`）或应用到相册简介
- `views/admin/Seo.vue` 有只读 AI 校对弹窗；`views/admin/Health.vue`（新建，路由 `health`，侧栏「内容体检」）渲染站点级评分与 findings
- `stores/site.ts` 带五个新 `llm*` 键（`llmProvider`/`llmThinking`/`llmReasoningEffort`/`llmMaxTokens`/`llmCommentModeration`），否则 `reactive<SiteSettings>` 缺键会直接过不了 `type-check`；**它们都不进 `PREPAINT_APPEARANCE_VARS`**（首屏内联脚本只预置外观变量）

## 路由约定

- 本站为单人作者博客，访客只读：`authRoutes` 只有一条 `/login` 重定向，指向服务端下发的作者后台入口 `siteStore.adminRoot`（默认 `/admin`）。组件中访客需登录时统一 `router.push('/login')`，不要再指向硬编码路径
- `requiresAuth` / `requiresAdmin` 由 `guards.ts` 统一跳转到 `adminRoot` 并带 `redirect` 查询参数
- 举报页路由为 `/report/:type(post|comment|user)/:id(\d+)`

## 开发规范

1. 使用 Vue 3 Composition API (`<script setup>`)
2. 组件命名使用 PascalCase
3. API 调用统一通过 `src/api/` 目录
4. 状态管理使用 Pinia
5. 路由守卫处理登录验证

## API 配置

开发环境自动代理 `/api` 到 `http://localhost:8825`

## 环境变量

统一使用 `.env` 文件配置（开发/生产环境通用）：

```env
VITE_APP_TITLE=个人博客                     # 应用标题（代码实际消费）
VITE_API_TARGET=http://localhost:8825       # 开发环境代理目标（vite.config.ts 消费）
VITE_API_BASE_URL=/api                      # 保留位：axios 在 src/api/index.ts 硬编码 /api，未被消费
VITE_APP_VERSION=2.0.29                     # 保留位：当前未被代码消费；真实版本号由 vite.config.ts 从 package.json 注入 __APP_VERSION__
```

如需本地覆盖，创建 `.env.local`（不提交 git）。

## Vercel 部署（可选）

Vercel 只作为**统一反向代理**指向已部署的 JAR，不在平台侧再构建一份前端：

1. Root Directory 设为 `frontend`，Framework Preset 选 **Other**（不要选 Vite 自动预案）。
2. 配置 `SELFBLOG_BACKEND_ORIGIN`（源站 HTTPS origin）与 `SITE_BASE_URL`（正式访客 origin），两者必须不同，否则构建失败。
3. Build Command 使用 `node scripts/build-vercel-proxy.mjs`，生成 Build Output v3 路由把全部路径/方法转发到源站；Preview 环境自带 `X-Robots-Tag: noindex,nofollow`。

生产环境的推荐方式仍是后端单 JAR 直接内嵌前端（不走 Vercel）；完整步骤见 [backend/DEPLOY.md](../backend/DEPLOY.md) 第 9 节。

## 已删除的历史死代码（2026-10-17）

以下文件曾长期无人引用（从未注册路由或早已被替换），已删除；`CHANGELOG.md` 记录完整清单：

- `router/modules/circle.ts`、`views/circle/*`、`components/circle/*`（校友圈 UI 从未注册路由）
- `views/admin/{CircleManagement,TopicsManagement}.vue`、`api/topic.ts`、`types/topic.ts`
- `views/{topic/TopicDetail,trending/TrendingPage,auth/PasswordReset,auth/Register}.vue`
- `components/home/*`（旧首页组件，现由 `views/Home.vue` 自行实现）
- `components/common/Card.vue`、`composables/usePagination.ts`、`types/vo.ts`

2026-10-21 又删了七个零引用整文件：`components/common/{Button,Input,Tag}.vue`、`views/post/{HotPosts,PostSearch}.vue`、`api/{share,trending}.ts`（另删了 `utils/index.ts` 的 `formatNumber`/`copyToClipboard`/`generateId`/`debounce`/`storage` 等死导出与约 200 行死 CSS）。`api/message.ts` 与 `api/notification.ts` **刻意保留**：它们对应仍存在的后端管理端接口，只是暂时没有前端入口。

后端对应的社区/话题接口与数据表仍保留（管理员 API 未删除），只是不再有对应前端入口。

## 注意事项

- 修改代码前先阅读相关文件
- 遵循现有的代码风格
- 每次修改后更新文档
