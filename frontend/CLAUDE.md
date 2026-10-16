# CLAUDE.md - 个人博客 Vue 3 前端 (v2.0.25)

## 项目概述

这是单人作者个人博客的前端项目，基于 Vue 3 + Vite 构建。访客只读、搜索，作者通过管理员后台写作管理。当前版本 v2.0.25。

## 常用命令

```bash
npm ci                 # 安装锁定依赖（Node ^22.22.2 || ^24.15.0 || >=26.0.0，engine-strict）
npm run dev            # 启动开发服务器 (端口 3000)
npm run build          # 构建生产版本（先 vue-tsc --noEmit，再 Vite）
npm run preview        # 预览生产版本
npm run lint:check     # 只读 ESLint 检查（npm run lint 会改写文件）
npm run type-check     # vue-tsc --noEmit（必过闸门）
npm run test:run       # Vitest 单次运行（当前 41 文件 / 394 tests；原始基线 210）
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
├── api/            # API 接口 (20 个模块)
├── components/     # Vue 组件
│   ├── common/     # 通用组件
│   │   ├── BackToTop.vue     # 返回顶部
│   │   ├── Button.vue        # 通用按钮
│   │   ├── Card.vue          # 通用卡片
│   │   ├── CollectButton.vue # 收藏按钮
│   │   ├── FollowButton.vue  # 关注按钮
│   │   ├── Icon.vue          # 内置 SVG 图标
│   │   ├── ImagePreview.vue  # 图片预览灯箱
│   │   ├── Input.vue         # 通用输入框
│   │   ├── LikeButton.vue    # 点赞按钮
│   │   ├── Modal.vue         # 通用模态框
│   │   ├── Tag.vue           # 标签组件
│   │   └── Toast.vue         # Toast 通知
│   ├── circle/     # Circle 组件 (4 个)
│   │   ├── CirclePostCard.vue    # Circle 文章卡片
│   │   ├── CircleSidebar.vue     # Circle 侧边栏
│   │   ├── CreatePostModal.vue   # 创建帖子弹窗
│   │   └── QuickComposer.vue     # 快速发布器
│   ├── home/       # 首页组件 (HomeFilters, HomeSidebar, PostCardList)
│   ├── layout/     # Footer / Navbar / NavbarMusicControl / headless MusicPlayer
│   ├── post/       # ArticleList / MobileTocSheet
│   └── admin/      # 后台组件 (SiteAssetField)
├── composables/    # useDialog / useConfirm / useScrollState / useMarkdownEnhance / useSeoMeta / usePagination / useScrollReveal
├── router/         # 路由配置
├── stores/         # Pinia 状态管理 (site / theme / user / music)
├── styles/         # 样式文件（入口 main.css）
├── utils/          # 工具函数 (crypto / logger / toast / visitor / reading)
└── views/          # 页面组件 (45 个)
```

## 主要特性

- 单人作者博客：访客只读、搜索、匿名点赞与留言；写作与管理需要管理员登录
- 清透空间设计底座：CSS 变量 token、深色模式、5 套皮肤、内置 SVG 图标；正文实底，导航与临时控件按需使用玻璃
- 文章列表统一由 `components/post/ArticleList.vue` 渲染；首页、搜索、标签共用同一套行样式，无封面不留占位图
- 阅读页把标题/时间/阅读时长放在封面之前；目录侧栏与移动端抽屉共用 1020px 断点
- 后台使用同一套「标题 + 说明 + 内容区」层级；抽屉的焦点、Esc 与滚动锁由 `AdminLayout` 的 `useDialog` 统一负责
- 导航音乐：单一 headless 音频宿主，不自动播放，公开换页连续，进入后台暂停；控件不创建额外 audio
- 阅读体验：目录（移动端抽屉）、代码复制/折叠、标题锚点、图片灯箱、按正文估算阅读时长
- 站点品牌资源上传：Logo / 浏览器图标 / 作者头像 / 背景音乐（`SiteAssetField.vue`）
- 生产为内嵌前端的单 JAR，前端无需独立部署

## 设计系统

- 入口 `src/styles/main.css`，按顺序 `@import` 12 个模块；设计 token 在 `variables.css`，深色模式覆盖在 `dark-mode.css`，皮肤在 `skins.css`
- 深色模式通过 `.dark-mode` 类生效（`stores/theme.ts` 加在 `<html>`，`App.vue` 同时绑在 `#selfblog` 上）
- 皮肤通过 `#selfblog[data-skin='...']` 生效：`ocean`(默认)/`forest`/`lavender`/`sunset`/`midnight`
- 动画工具类: `.fade-in`, `.slide-up`, `.pulse`

### Token 使用约定（重要）

- **不要用 `--gray-900` / `--gray-100` 当「深色表面」**：深色模式会整体反转灰度梯度（`--gray-900` 由 `#111111` 变成 `#EBEBEB`），会让按钮、代码块在深色页面上变成刺眼的亮块
- 需要「永远深色」或「高对比实心」时使用语义 token：
  - `--code-bg` / `--code-text` / `--code-border` — 代码块，两种主题下都保持深色
  - `--contrast-bg` / `--contrast-bg-hover` / `--contrast-bg-active` / `--contrast-fg` — 高对比实心按钮（浅色模式近黑，深色模式自动反转为浅底深字）
- 组件只引用语义 token，不直接写十六进制色值
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
VITE_APP_VERSION=2.0.25                     # 保留位：当前未被代码消费
```

如需本地覆盖，创建 `.env.local`（不提交 git）。

## Vercel 部署（可选）

1. Root Directory 设为 `frontend`
2. Framework Preset 选 `Vite`
3. `vercel.json` 已把 `/api` 与 `/uploads` 重写到已配置的后端域名

生产环境的推荐方式仍是后端单 JAR 直接内嵌前端（不走 Vercel）。

## v2.0.22 修复记录

| 问题 | 修复文件 | 修复内容 |
|------|----------|----------|
| 移动端宽度不一致 | `PostSearch.vue` | `max-width` 改为 `var(--container-xl)` + `@media` |
| 私信头像不显示 | `Messages.vue` | 内联SVG → `/default-avatar.png` |
| 草稿ID提取失败 | `PostEdit.vue` | 兼容 `res.data` 为数字或对象 |
| 写文章加载旧内容 | `PostEdit.vue` | 移除无条件 `fetchDraft()` |
| CSS line-clamp | 8个文件 | 添加标准 `line-clamp` 属性 |
| 弹窗@提及 | `Circle.vue` | 添加 mention 搜索和选择 |
| 弹窗多话题 | `Circle.vue` | `selectedTopic` → `selectedTopics` 数组 |
| 弹窗草稿 | `Circle.vue` | 关闭保存/打开恢复 |

## 注意事项

- 修改代码前先阅读相关文件
- 遵循现有的代码风格
- 每次修改后更新文档
