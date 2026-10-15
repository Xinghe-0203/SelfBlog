# 更新日志 (CHANGELOG)

所有重要更改都将记录在此文件。

---

## 未发布 — SEO、订阅源与字体 (2026-10-15)

参考同类开源个人博客项目（`linhaojun857/aurora`、`weiwosuoai/WeBlog`、`iszhouhua/blog` 等）后补齐的能力。
这些项目普遍具备「被订阅、被搜索、被分享」的基础设施，而本项目在此之前完全没有。

### 新增

- **RSS 2.0 订阅源** `GET /feed.xml`：输出已发布文章（`status=1`，按发布时间倒序，最多 100 篇），含标题、链接、摘要、分类与 `pubDate`
- **Atom 1.0 订阅源** `GET /atom.xml`：同一数据源的现代格式，时间戳按 RFC 3339 截断到秒
- **`GET /sitemap.xml`**：收录首页、归档、关于、相册、搜索、全部文章与全部标签
- **动态 `GET /robots.txt`**：`Sitemap` 指令必须是绝对地址，静态文件在构建时并不知道站点域名，因此改为运行时生成；同时 `Disallow: /admin` 与 `/api/`
- **`index.html` 补齐站点级 SEO/分享标签**：`description`、`canonical`、`theme-color`（浅色/深色两套）、Open Graph、Twitter Card，以及订阅源自动发现 `rel="alternate"`
- **文章页运行时更新分享卡片**：新增 `composables/useSeoMeta.ts`，进入文章详情时把 `og:title`/`og:description`/`og:image`/`article:published_time`/`article:tag` 换成该文章自己的值——单页应用所有路由共用一份 `index.html`，不这样做分享出去永远是首页标题
- **文章页 JSON-LD 结构化数据**：注入 `BlogPosting`（标题、摘要、封面、发布时间、作者、站点），让搜索结果有机会展示富摘要；用 `textContent` 写入，标题含 `</script>` 也不会破坏页面
- **上一篇 / 下一篇导航**：`GET /api/post/{id}/adjacent` 返回相邻的已发布文章（按发布时间，时间相同用 id 兜底），文章底部渲染为两张卡片。此前读完一篇文章没有任何出口，只能按浏览器后退
- 站点基地址支持 `SITE_BASE_URL` 覆盖（反向代理后 `request` 里的 host 可能不是对外域名），未配置时按 `X-Forwarded-*` 或请求推导

### Bug 修复

- **`/robots.txt` 返回 500**：`SecurityConfig` 已放行该路径，但项目里根本没有这个资源，爬虫访问只会看到一台坏掉的站点
- **`/feed.xml`、`/sitemap.xml` 返回 401**：`SpaController` 的 SPA 回退正则用 `[^\.]*` 只匹配**不含点号**的路径，带扩展名的地址不会被回退命中，直接落到末尾「仅 `ROLE_admin`」的兜底规则。已在 `SecurityConfig` 显式放行这三个地址
- **每次访问都白下载数百 KB 外部字体**：`index.html` 从 `fonts.googleapis.com` 加载 Fraunces + Plus Jakarta Sans，但 `variables.css` 的 `--font-display`/`--font-sans` 从未引用这两个字体族（引用的是 `Lyon Text`/`Newsreader`/`SF Pro Display` 等从未加载的字体）。已移除这三条 `link`，字体改用系统字体栈
- **字体栈没有中文回退**：`--font-sans` 只有 `-apple-system`/`Segoe UI` 等西文字体，中文会落到浏览器默认字体，与西文混排时字重字距不一致；`--font-display` 引用的衬线体全部不存在，实际退化为 Georgia。已补 `PingFang SC`/`Hiragino Sans GB`/`Microsoft YaHei`/`Source Han Sans SC`/`Noto Sans CJK SC` 等中文回退

### 说明

`SeoController` 使用 `@Controller` + `@ResponseBody` 而非 `@RestController`：项目通过 `WebMvcConfig#configurePathMatch` 给所有 `@RestController` 自动加 `/api` 前缀，用后者会让订阅地址变成 `/api/feed.xml`，而订阅器与爬虫只会去根路径找。

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/.../controller/site/SeoController.java` | 新增：RSS / Atom / sitemap / robots |
| `backend/.../config/security/SecurityConfig.java` | 放行 `/feed.xml`、`/atom.xml`、`/sitemap.xml` 与 `/api/post/{id}/adjacent` |
| `backend/.../service/post/PostQueryService.java`（+Impl） | 新增 `getAdjacentPosts`：相邻文章的查询逻辑 |
| `backend/.../service/post/BlogPostService.java`（+Impl） | 按既有 facade 约定委托 `getAdjacentPosts` |
| `backend/.../controller/post/BlogPostController.java` | 新增 `GET /{id}/adjacent` |
| `backend/.../vo/post/AdjacentPostVO.java` | 新增：相邻文章 VO |
| `frontend/index.html` | 新增 SEO/OG/Twitter/订阅发现标签；移除无效的 Google Fonts |
| `frontend/src/composables/useSeoMeta.ts` | 新增：运行时维护页面级 meta 与 JSON-LD |
| `frontend/src/views/post/PostDetail.vue` | 文章加载后更新分享卡片、注入 JSON-LD、渲染上一篇/下一篇 |
| `frontend/src/api/post.ts` | 新增 `getAdjacentPosts` |
| `frontend/src/styles/variables.css` / `dark-mode.css` | 字体栈改为系统字体 + 中文回退；新增 `--like-accent*` 语义 token |

---

## 未发布 — 样式系统与路由修复 (2026-10-15)

### Bug 修复

- **十多个用户页面全部 404**: `router/index.ts` 只注册了 `homeRoutes`/`authRoutes`/`postRoutes`/`discoverRoutes`，**从未导入并展开 `userRoutes`**，导致 `router/modules/user.ts` 中定义的全部路由成为死代码：`/profile`、`/profile-edit`、`/user/:id`、`/password-change`、`/drafts`、`/collections`、`/following`、`/followers`、`/my-reports`、`/notifications`、`/messages` 均落到「页面不存在」。补上导入与展开（`vue-tsc` 不会报未使用模块，因此此前未被发现）
- **访客点赞/收藏/关注跳转到 404**: `LikeButton`/`CollectButton`/`FollowButton`/`Circle`/`CircleDetail` 等组件在访客操作时 `router.push('/login')`，但 `authRoutes` 是空数组，`/login` 没有对应路由。新增 `/login` 重定向到服务端下发的作者后台入口（`siteStore.adminRoot`），与 `guards.ts` 中 `requiresAuth` 的跳转约定一致
- **举报按钮跳转到 404**: `CircleDetail.vue` 跳转 `/report/post/{id}`，但缺少该路由，`views/common/Report.vue` 虽已实现却无法访问。新增 `/report/:type(post|comment|user)/:id(\d+)` 路由
- **`.env` 中文值乱码**: `DotenvConfig` 使用 `Properties.load(InputStream)` 按 ISO-8859-1 解码，导致 `.env.example` 中自带的 `ADMIN_NICKNAME=站长` 被读成 `ç«é¿`。改为按 UTF-8 读取
- **深色模式下代码块变成全页最亮元素**: `PostDetail.vue` 的 `.markdown-body :deep(pre)` 用 `--gray-900` 作深色背景，但深色模式会整体反转灰度梯度（`--gray-900` 变为 `#EBEBEB`），代码块因此变成刺眼的浅色块。改用不随主题反转的 `--code-bg`/`--code-text`
- **主按钮在深色模式下失去边界**: `Button.vue` 的 `.btn--primary` 硬编码 `#111111`，在 `#191919` 画布上几乎看不出按钮边界。改用语义化的 `--contrast-bg`/`--contrast-fg`，深色模式自动反转为浅底深字
- **`--gray-900` 被当作深色表面使用导致按钮意外反转**: `Login.vue`/`Register.vue` 的 `.btn-primary` 用 `--gray-900` + `--gray-800` 作背景，深色模式下变成浅色按钮。改用 `--contrast-*` 语义 token
- **登录/注册页 Logo 与导航栏不一致**: 两个页面的内联 SVG Logo 渐变硬编码为青色 `#0D9488`→`#14B8A6`，而全站主色是蓝色 `#165DFF`。改为引用 `var(--accent)`/`var(--accent-dark)`，同时让皮肤切换生效
- **`midnight` 皮肤的 `color-scheme` 从未生效**: `skins.css` 用后代选择器 `#selfblog[data-skin='midnight'] .dark-mode`，但 `App.vue` 把 `.dark-mode` 加在 `#selfblog` 自身，选择器永远匹配不到。补充同元素与后代两种形式

### 样式系统

- **清理遗留青色主题残留**: `Button.vue`、`Input.vue`、`Tag.vue`、`QuickComposer.vue`、`Circle.vue`、`Messages.vue`、`Notifications.vue`、`Profile.vue`、`PasswordReset.vue`、`NotFound.vue` 中残留的旧青色/绿色调色板（`#0D9488`/`#059669`/`#047857`/`#0F766E`/`#14B8A6` 及对应 `rgba()`）统一迁移到当前「清爽科技蓝」调色板
- **新增语义化设计 token**（`variables.css` + `dark-mode.css`）:
  - `--contrast-bg` / `--contrast-bg-hover` / `--contrast-bg-active` / `--contrast-fg` — 高对比实心表面，两个主题各自显式定义，语义稳定
  - `--code-bg` / `--code-text` / `--code-border` — 代码块专用表面，深色模式下仍保持深色
  - 补充注释说明「不要用 `--gray-900` 当深色背景」，因为深色模式会反转灰度梯度

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/com/selfblog/config/DotenvConfig.java` | `.env` 改按 UTF-8 读取 |
| `frontend/src/router/modules/auth.ts` | 新增 `/login` 重定向路由 |
| `frontend/src/router/modules/user.ts` | 新增 `/report/:type/:id` 路由 |
| `frontend/src/styles/variables.css` | 新增 `--contrast-*` 与 `--code-*` token |
| `frontend/src/styles/dark-mode.css` | 深色模式下的 `--contrast-*` 与 `--code-*` 定义 |
| `frontend/src/styles/skins.css` | 修复 `midnight` 的 `color-scheme` 选择器 |
| `frontend/src/components/common/Button.vue` | 主按钮 token 化 + 清理青色残留 |
| `frontend/src/components/common/Input.vue` | 清理青色残留 |
| `frontend/src/components/common/Tag.vue` | 清理青色残留 |
| `frontend/src/views/post/PostDetail.vue` | 代码块改用 `--code-*` |
| `frontend/src/views/auth/Login.vue` | Logo 渐变与主按钮 token 化 |
| `frontend/src/views/auth/Register.vue` | Logo 渐变与主按钮 token 化 |

---

## v2.0.25 (2026-10-14)

### 个人博客与一体化部署

- 将 Vue 前端、Spring Boot API 和 SPA 路由回退打包为单个可执行 JAR；默认 SQLite 持久化和 Caffeine 进程内缓存
- 保留原有文章数据，SQLite 初始化只增量建表；增加适用于已有 MySQL 数据库的非破坏性迁移脚本
- 增加匿名访客统计、不可逆访客/IP 摘要、匿名文章点赞、可开关访客评论和后台评论管理
- 增加公开/密码相册及短期内存访问令牌保护、背景音乐播放组件
- 增加可配置后台入口、站点资料、主题皮肤与主色设置
- 关闭访客注册并隐藏公共登录入口；公开阅读及匿名互动可用，内容写入和管理 API 限管理员
- 更新前后端 README、部署文档和 `.env.example`；首次启动管理员凭据由 `.env` 配置
- 将 Vue、Axios、DOMPurify 和 Vite 升级至修复版；`npm audit` 无已知漏洞
- 修复 SPA 历史路由回退误拦 `/assets/**`，避免 JS/CSS 返回首页 HTML 导致页面空白
- 将项目目录、Java 包名、Maven 产物及 npm 包统一重命名为 SelfBlog；保留旧 SQLite 文件名以兼容现有数据

---

## v2.0.24 (2026-06-24)

### Bug 修复

- **热度榜单同一文章重复占位**: `selectHotPosts` SQL 查询 7 天数据时，`blog_trending` 表按 `(post_id, date)` 每天存一条记录，导致同一篇文章最多出现 7 次。改为自连接 + `MAX(date)` 去重，每个文章只返回最近日期的记录
- **校友圈热门动态无时间范围过滤**: `getHotContent()` 查询热门校友圈动态时缺少时间范围限制，与文章的 7 天窗口不一致。增加 `createTime >= dateStart` 过滤条件
- **SQLite 不支持 GREATEST 函数**: 3 处 Mapper XML 使用 MySQL 特有的 `GREATEST()` 函数但缺少 SQLite 方言变体，导致运行时 `SQLITE_ERROR: no such function: GREATEST`。拆分为 MySQL/SQLite/H2 三种变体：
  - `TopicMapper.xml#decrementPostCount` — MySQL 用 `GREATEST()`, SQLite 用 `MAX()`, H2 用 `CASE WHEN`
  - `SysUserMapper.xml#decrementFollowerCount` — 同上
  - `SysUserMapper.xml#decrementFollowingCount` — 同上

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/resources/mapper/BlogTrendingMapper.xml` | selectHotPosts 自连接去重 |
| `backend/src/main/java/.../service/content/impl/TrendingServiceImpl.java` | circle 帖子加时间范围过滤 |
| `backend/src/main/resources/mapper/TopicMapper.xml` | decrementPostCount 拆分 MySQL/SQLite/H2 变体 |
| `backend/src/main/resources/mapper/SysUserMapper.xml` | decrementFollowerCount/decrementFollowingCount 拆分 MySQL/SQLite/H2 变体 |

---

## v2.0.23 (2026-06-10)

### 新功能

- **QuickComposer 快速发布组件**: Circle 页面新增 Twitter/X 风格快速发布器，支持文字、图片（最多9张）、视频（最多1个，100MB）、话题选择、位置输入、Ctrl+Enter 提交
- **视频上传预览**: QuickComposer 支持视频文件选择、预览播放、上传进度条
- **渐变占位背景**: Home.vue 无封面文章卡片显示渐变灰背景，统一卡片视觉结构

### 功能增强

- **Home 布局统一**: 移除 Featured 大卡片布局，所有文章统一使用 `bento-standard` 双列网格布局
- **热门文章页面**: `PostSearch.vue` 重命名为 `HotPosts.vue`，路由改为 `/hot-posts`

### Bug 修复

- **CORS 配置**: `.env:55` 全角逗号改为半角逗号，修复跨域请求问题
- **热门标签加载失败**: `Home.vue` 修复 `response?.data?.records || []` 响应解析
- **热门标签类型错误**: `trending.ts` `getHotTags()` 返回类型修正

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `QuickComposer.vue` | 新增组件（图片/视频上传、话题、位置） |
| `Circle.vue` | 集成 QuickComposer |
| `Home.vue` | 移除 Featured 布局、统一卡片、渐变占位 |
| `HotPosts.vue` | 从 PostSearch 重命名 |
| `trending.ts` | getHotTags 类型修复 |
| `.env` | CORS 逗号修复 |

---

## v2.0.22 (2026-06-09)

### Bug 修复

- **昵称修改不生效**: `SysUserServiceImpl.java` 移除 `htmlSanitizer.sanitizePlainText()`，与注册行为一致
- **私信头像不显示**: `Messages.vue` 内联SVG改为 `/default-avatar.png` 静态文件
- **时间显示错乱**: `application.yml` 添加 `write-dates-as-timestamps: false`，修复 LocalDateTime 序列化为数组的问题
- **草稿ID提取失败**: `PostEdit.vue` 兼容 `res.data` 为数字或对象格式
- **写文章加载旧内容**: `PostEdit.vue` 移除无条件 `fetchDraft()`
- **搜索不全面**: `PostQueryServiceImpl.java` 搜索增加 `summary` 字段匹配
- **移动端宽度不一致**: `PostSearch.vue` `max-width` 改为 `var(--container-xl)` + `@media 768px` 响应式
- **CirclePost.vue TypeScript**: `location` 类型修复 `null` → `undefined`

### 功能增强

- **弹窗@提及功能**: `Circle.vue` 弹窗添加 @用户搜索和选择功能
- **弹窗多话题选择**: `selectedTopic` 改为 `selectedTopics` 数组，支持多选
- **弹窗草稿保存**: 弹窗关闭时自动保存草稿，打开时恢复
- **CSS line-clamp**: 在 `utilities.css` + 7个组件中添加标准 `line-clamp` 属性
- **在线状态UI清理**: `Messages.vue` 移除不支持的在线状态显示

### 数据库

- **计数器同步脚本**: 新建 `fix-counter-sync.sql`，修复 `like_count`/`collect_count` 与实际记录不一致
  - 执行方式: `sqlite3 backend/campus_blog.db < backend/src/main/resources/fix-counter-sync.sql`

### 基础设施

- **前端环境配置统一**: 合并 `.env.development` 和 `.env.production` 为单一 `.env` 文件
- **Vercel 部署**: 添加 `vercel.json` 配置 `/api` 和 `/uploads` 代理
- **项目技能**: 新建 `.omc/skills/` 目录，包含 4 个项目技能

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `SysUserServiceImpl.java` | 移除昵称 sanitizer |
| `PostQueryServiceImpl.java` | 搜索增加 summary 字段 |
| `application.yml` | Jackson 时间配置 |
| `PostSearch.vue` | 响应式修复 |
| `PostEdit.vue` | 草稿逻辑修复 |
| `Messages.vue` | 头像修复 + 移除在线状态 |
| `PostDetail.vue` | 移动端 padding |
| `Circle.vue` | @提及 + 多话题 + 草稿 |
| `CirclePost.vue` | TypeScript 修复 |
| `utilities.css` | line-clamp 标准属性 |
| `fix-counter-sync.sql` | 新建 |

---

## v2.0.21 (2026-06-07)

### 前端交互优化

- **PostDetail.vue 乐观更新与防抖**: 点赞、收藏、关注操作添加防抖状态（`isTogglingLike`/`isTogglingCollect`/`isTogglingFollow`），防止重复请求；采用乐观更新策略，先更新 UI 再发请求，失败时自动回滚；使用后端返回的实际 `action` 状态和计数替代前端本地翻转
- **PostCardList.vue 乐观更新与防抖**: 点赞/收藏操作添加防抖 Set（`togglingLikes`/`togglingCollects`），防止同一文章重复操作；保存操作前状态快照，失败时完整回滚 Set 和计数；使用后端返回的 `likeCount`/`collectCount` 替代前端 ±1
- **Circle.vue 校友圈点赞优化**: 添加操作前状态备份，失败时完整回滚；使用后端返回的 `action` 和 `likeCount` 确保前后端状态一致
- **状态布尔值修复**: `PostDetail.vue` 中 `isLiked`/`isCollected`/`isFollowing` 赋值添加 `!!` 确保布尔类型，避免 falsy 值导致的状态异常

### 移动端导航重构

- **Navbar.vue 移动端菜单重构**: 移动端菜单拆分为 overlay + menu 双层结构，overlay 点击关闭菜单；修复移动端菜单层级问题（dropdown z-index 修复）
- **移动端样式优化**: 移动端搜索框移除 glassmorphism 改用实体背景提升可读性；导航链接添加 `min-height: 48px` 满足触摸目标尺寸要求；添加 `-webkit-tap-highlight-color` 和 `-webkit-overflow-scrolling` 改善触摸体验
- **移动端下拉菜单适配**: 768px 断点下 dropdown 改为全宽固定定位，优化移动端个人中心下拉菜单体验

### 类型定义修复

- **follow.ts FollowCounts 接口修正**: 字段名从 `followers`/`following` 改为 `followerCount`/`followingCount`，与后端返回字段对齐；新增可选 `userId` 字段

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/post/PostDetail.vue` | 乐观更新 + 防抖 |
| `frontend/src/components/home/PostCardList.vue` | 乐观更新 + 防抖 |
| `frontend/src/views/circle/Circle.vue` | 点赞优化 + 回滚 |
| `frontend/src/components/layout/Navbar.vue` | 移动端菜单重构 |
| `frontend/src/types/follow.ts` | 类型定义修正 |
| `frontend/src/utils/__tests__/toast.test.ts` | 测试修复 |

---

## v2.0.20 (2026-06-06)

### 多数据库 Mapper XML 支持

- **MyBatis DatabaseIdProvider 多方言**: 为 4 个 Mapper XML 添加 `databaseId` 属性，支持 MySQL / SQLite / H2 三种数据库方言自动切换
- **BlogPostTagMapper.xml**: `batchInsertPostTags` 新增 SQLite 方言（`INSERT OR IGNORE`）和 H2 兼容版本
- **BlogTrendingMapper.xml**: `upsert` 新增 SQLite 方言（`INSERT OR REPLACE`）和 H2 兼容版本（`MERGE INTO`）
- **SysUserMapper.xml**: `incrementLoginFailCount` 新增 SQLite 方言（`datetime` 函数）和 H2 兼容版本（`DATEADD`）；新增 `incrementFollowerCount`、`decrementFollowerCount`、`incrementFollowingCount`、`decrementFollowingCount` 原子计数操作
- **TopicMapper.xml**: `recalculateAllTrendingScore` 和 `recalculateAllPostCount` 新增 SQLite 方言（`json_each`）和 H2 兼容版本

### 后端测试完善

- **新增 11 个 Controller 测试**: BlogCollectControllerTest、BlogTagControllerTest、FollowControllerTest、MessageControllerTest、NotificationControllerTest、PasswordControllerTest、ReportControllerTest、ShareControllerTest、StatisticsControllerTest、TopicControllerTest、TrendingControllerTest
- **新增 11 个 Service 测试**: BlogCollectServiceImplTest、BlogTagServiceImplTest、CircleInteractionServiceImplTest、CircleQueryServiceImplTest、FollowServiceImplTest、MessageServiceImplTest、NotificationServiceImplTest、ReportServiceImplTest、ShareServiceImplTest、StatisticsServiceImplTest、TopicServiceImplTest

### 前端修复

- **user.ts 类型定义更新**: User 接口字段调整，补充可选属性
- **PasswordReset.vue 密码重置页面优化**: 改进密码强度检测、验证码重发逻辑、表单校验

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/resources/mapper/BlogPostTagMapper.xml` | 多数据库方言 |
| `backend/src/main/resources/mapper/BlogTrendingMapper.xml` | 多数据库方言 |
| `backend/src/main/resources/mapper/SysUserMapper.xml` | 多数据库方言 + 新增原子操作 |
| `backend/src/main/resources/mapper/TopicMapper.xml` | 多数据库方言 |
| `backend/src/test/java/.../controller/` (11 个) | 新增 Controller 测试 |
| `backend/src/test/java/.../service/impl/` (11 个) | 新增 Service 测试 |
| `frontend/src/types/user.ts` | 类型定义 |
| `frontend/src/views/auth/PasswordReset.vue` | 页面优化 |

---

## v2.0.19 (2026-06-06)

### 邮件服务修复

- **MAIL_FROM 配置未生效**: `EmailServiceImpl` 的 `fromEmail` 只读取 `spring.mail.username`，导致发件人始终等于 SMTP 账号。改为优先读取 `mail.from`（即 `.env` 中的 `MAIL_FROM`）
- **邮件发送失败后冷却时间未重置**: `sendVerificationCode()`（密码找回）发送失败时只清理了 `verificationStore`，未清理 `sendTimeStore`，导致用户失败后仍需等待冷却。已修复为与注册验证码逻辑一致
- **邮件模板硬编码有效期**: `buildEmailTemplate()` 中写死"5 分钟"，未使用配置的 `expireMinutes`。改为动态变量
- **注释与实际不符**: `generateSecureCode()` 注释写"8 位验证码"，实际为 6 位。已修正注释
- **application.yml 配置优化**: 移除无效的 `spring.mail.properties.mail.from`，新增独立的 `mail.from` 配置项；增加 SSL 支持（`ssl.enable`）适配 QQ 邮箱 465 端口
- **CORS 端口扩展**: `.env` 中 `CORS_ALLOWED_ORIGINS` 扩展为包含所有常见本地开发端口（3000/3001/5173/4173/8080/8081/4200/4201）

### 点赞/收藏计数修复

- **前端列表页乐观更新无回滚**: `PostCardList.vue` 点击点赞/收藏后先改前端计数再发请求，失败时仅 toast 错误未回滚计数。已添加请求失败后的计数和状态回滚逻辑
- **后端状态缓存未清除**: `toggleLike`/`toggleCollect` 未清除 `STATUS_CACHE`，导致点赞后 2 分钟内状态检查返回旧值。已添加 `@CacheEvict` 注解
- **管理员删除文章未清计数**: `adminDeletePost` 软删除点赞/收藏记录但未同步减少 `blog_post` 计数。已添加重置 `like_count=0` 和 `collect_count=0`
- **普通删除文章未清计数**: `deletePost` 同样未清理关联计数。已修复
- **校友圈点赞缺少上限保护**: `CirclePostMapper.xml` 的 `incrementLikeCount` 无上限保护（文章点赞有）。已添加 `CASE WHEN like_count < 100000000`

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/.../service/auth/impl/EmailServiceImpl.java` | 邮件修复 |
| `backend/src/main/resources/application.yml` | SSL 配置 |
| `backend/.env.example` | MAIL_FROM 注释优化 |
| `frontend/src/components/home/PostCardList.vue` | 回滚逻辑 |
| `backend/src/main/java/.../service/social/impl/BlogLikeServiceImpl.java` | @CacheEvict |
| `backend/src/main/java/.../service/social/impl/BlogCollectServiceImpl.java` | @CacheEvict |
| `backend/src/main/java/.../service/post/impl/BlogPostServiceImpl.java` | 删除清计数 |
| `backend/src/main/resources/mapper/CirclePostMapper.xml` | 上限保护 |

---

## v2.0.18 (2026-06-05)

### 后端修复

- **缓存键冲突导致 ClassCastException**: `checkLikeStatus` 和 `checkCollectStatus` 共用 `STATUS_CACHE`，键格式均为 `userId:postId`，导致点赞状态缓存命中后返回 `LikeStatusVO` 给收藏接口。修复：缓存键添加前缀 `'like:'` 和 `'collect:'`
- **MyBatis Plus DatabaseIdProvider 缺失**: 新增 `DatabaseIdProvider` Bean 支持 XML 中 `databaseId` 多数据库方言（MySQL/SQLite），修复 SQLite 环境下 XML SQL 语句选择错误
- **TrendingServiceImpl UNIQUE 约束冲突**: `baseMapper.insert()` 改为 `baseMapper.upsert()`，解决 `blog_trending` 表 `UNIQUE(post_id)` 约束冲突
- **Logback 日志不写入文件**: 移除语法错误的 TurboFilter（`<filter>` 应为 `<turboFilter>`），修复 `default` profile 缺少文件 appender，修复 `dev` profile 主日志缺少 `ASYNC_FILE`

### 前端构建

- **TypeScript 编译通过**: `npm run build` 零错误
- **Vite 代理正常**: `/api` 代理到 `http://localhost:8825`，登录和 API 调用均正常

### 测试结果

- **55/55 GET 端点全部通过**（直连 + 代理双通道验证）
- **写入端点全部正常**: 文章创建、评论、点赞、收藏、私信、分享、校友圈、草稿、举报
- **预期 400 错误已验证**: "不能给自己发私信"、"不能举报自己"、"不能关注自己"

### 影响的文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/.../service/social/impl/BlogLikeServiceImpl.java` | 缓存键前缀 |
| `backend/src/main/java/.../service/social/impl/BlogCollectServiceImpl.java` | 缓存键前缀 |
| `backend/src/main/java/.../config/db/MybatisPlusConfig.java` | 新增 DatabaseIdProvider |
| `backend/src/main/java/.../service/content/impl/TrendingServiceImpl.java` | upsert 修复 |
| `backend/src/main/resources/logback-spring.xml` | TurboFilter + profile 修复 |

---

## v2.0.17 (2026-06-03)

### 后端修复

- **UserProfile.vue 关注/取消关注 API 路径错误**: `toggleFollow` 始终调用 `POST /follow`，从未调用 `DELETE /follow/{id}`。改为根据 `isFollowing` 状态分别调用 `followApi.toggleFollow`（关注）或 `followApi.unfollow`（取消关注）
- **@mentions 查询字段不匹配**: 前端插入 `@昵称`，后端 `parseMentions` 仅按 `username` 查询。改为同时查询 `nickname` 和 `username`
- **转发内容重复显示**: `repostContent` 被设为原动态内容，前端同时显示 `repostContent` 和 `repostPost.content`。改为不设置 `repostContent`
- **getMyLikes/getMyCollections 遗漏 NULL 记录**: `this.page()` 受 `@TableLogic` 影响只查 `is_deleted = 0`，遗漏历史 `is_deleted IS NULL` 记录。新增 Mapper 方法 `selectPageByUserId`/`countByUserId` 绕过 `@TableLogic`
- **新增 Mapper 方法**: `BlogLikeMapper.selectPageByUserId`/`countByUserId`、`BlogCollectMapper.selectPageByUserId`/`countByUserId`

### 前端修复

- **UserProfile.vue followerCount 使用后端实时计数**: 使用后端返回的 `followerCount` 替代本地 +/-1，避免并发场景下的计数偏差
- **PostCardList.vue 删除遗留空函数**: 删除 `openPreview` 空函数，消除死代码
- **图片预览功能为空函数**: `PostCardList.vue` 的 `openImagePreview` 和 `openPreview` 不发出事件。添加 `preview-image` emit 事件，`Home.vue` 监听并调用 `handlePreview`
- **阅读量双重计数**: `PostDetail.vue.fetchPost` 中后端 `getPostById` 已增加阅读量，前端又额外调用 `incrementViewCount`。移除前端重复调用
- **代码块复制按钮 CSS 类名不匹配**: `PostDetail.vue` 用 `copy-code-btn`，`main.css` 定义 `copy-btn`。改为统一使用 `copy-btn`
- **分页不同步到 URL**: `Home.vue.changePage` 未调用 `router.push`，浏览器前进/后退无法用于分页。添加 `router.push` 更新 URL query
- **前端忽略后端实时计数**: `PostCardList.vue`/`PostDetail.vue` 的 `toggleLike`/`toggleCollect` 使用 +/-1 乐观更新。改为使用后端返回的 `likeCount`/`collectCount`

### 测试修复与完善

- **CircleServiceImplTest 编译修复**: 测试调用 `createPost` 时参数数量不匹配（11 个参数，方法只需 10 个）。移除多余参数，新增 `createPost_WithContent_ShouldStoreContentCorrectly` 测试验证内容存储正确性
- **BlogLikeServiceImplTest 新增测试**: 新增 `testGetMyLikes_ShouldIncludeNullIsDeleted` 测试，验证 `getMyLikes` 能正确处理 `is_deleted IS NULL` 的历史遗留数据

### 影响的文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/user/UserProfile.vue` | 逻辑修复 |
| `frontend/src/components/home/PostCardList.vue` | 逻辑修复 |
| `frontend/src/views/Home.vue` | 逻辑修复 |
| `frontend/src/views/post/PostDetail.vue` | 逻辑修复 |
| `backend/src/main/java/.../service/impl/CircleServiceImpl.java` | 逻辑修复 |
| `backend/src/main/java/.../mapper/BlogLikeMapper.java` | 新增方法 |
| `backend/src/main/java/.../mapper/BlogCollectMapper.java` | 新增方法 |
| `backend/src/main/java/.../service/impl/BlogLikeServiceImpl.java` | 逻辑修复 |
| `backend/src/main/java/.../service/impl/BlogCollectServiceImpl.java` | 逻辑修复 |

---

## v2.0.16 (2026-06-03)

### 后端修复

- **MySQL 连接**: JDBC URL 添加 `allowPublicKeyRetrieval=true`，修复 `Public Key Retrieval is not allowed` 错误
- **MyBatis 配置**: 添加 `use-actual-param-name: false`，确保 `@Param` 注解优先于参数实际名称
- **`@TableLogic` 死代码修复**: `BlogLikeServiceImpl.toggleLike` 和 `BlogCollectServiceImpl.toggleCollect` 中，通过 `LambdaQueryWrapper` 查找已删除记录的代码因 `@TableLogic` 自动追加 `WHERE is_deleted = 0` 而成为死代码。改为使用自定义 SQL（`selectRawByUserAndPost`）绕过 `@TableLogic`
- **批量状态检查修复**: `checkLikeStatusBatch` 和 `checkCollectStatusBatch` 使用 `LambdaQueryWrapper` + `this.list()` 遗漏了 `is_deleted IS NULL` 的历史记录。改为使用自定义 SQL（`selectActivePostIdsByUserAndPosts`）
- **`BlogCollect` 实体修复**: `isDeleted` 字段补充默认值 `= 0`，与 `BlogLike` 实体对齐
- **`CirclePostMapper.countByTopicIds` 修复**: 删除 `@MapKey("topicId")` 注解，改为 default 方法在 Java 层完成 Map 转换，解决 `There is no getter for property named 'topicId' in 'class java.lang.Long'` 错误
- **新增 Mapper 方法**:
  - `BlogLikeMapper.selectActivePostIdsByUserAndPosts` — 批量查询用户已点赞的文章ID（绕过 `@TableLogic`）
  - `BlogCollectMapper.selectRawByUserAndPost` — 绕过 `@TableLogic` 查询收藏记录
  - `BlogCollectMapper.selectActivePostIdsByUserAndPosts` — 批量查询用户已收藏的文章ID（绕过 `@TableLogic`）

### 前端修复

- **`PostCardList.vue` emit 事件名修复**: `emit('update:likedPosts')` → `emit('update:liked-posts')`，`emit('update:collectedPosts')` → `emit('update:collected-posts')`，与父组件 Home.vue 的 kebab-case 监听器匹配
- **`PostCardList.vue` props 修复**: 不再直接修改 props 中的 Set，改为通过 `emit` 事件通知父组件更新状态

### 影响的文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/resources/application.yml` | 配置修改 |
| `backend/src/main/java/.../mapper/BlogLikeMapper.java` | 新增方法 |
| `backend/src/main/java/.../mapper/BlogCollectMapper.java` | 新增方法 |
| `backend/src/main/java/.../entity/BlogCollect.java` | 字段默认值 |
| `backend/src/main/java/.../service/impl/BlogLikeServiceImpl.java` | 逻辑重写 |
| `backend/src/main/java/.../service/impl/BlogCollectServiceImpl.java` | 逻辑重写 |
| `backend/src/main/java/.../mapper/CirclePostMapper.java` | 方法重写 |
| `frontend/src/components/home/PostCardList.vue` | 事件名修复 |
| `frontend/src/views/Home.vue` | 事件监听 |

---

## v2.0.15 (2026-05-30)

- HomeFilters.vue `filters is not defined` 运行时错误修复
- PostDetail.vue 文章评论幽灵数据问题（records 解析修复）
- Circle.vue 点赞失败无错误提示问题修复
- Circle.vue 热门动态移至左侧边栏，优化数据源
- Circle.vue 侧边栏数据每 60 秒自动刷新
- logger.js CORS 错误静默处理（/api/log 端点不存在）
- 校友圈移除标签功能（仅保留话题）

## v2.0.14 (2026-05-29)

- 密码重置字段名修复（password → newPassword）

## v2.0.13 (2026-05-28)

- 文章话题功能移除（标签功能保留）
- 校友圈话题功能修复并完善
- 话题数据路径修复（data.data.records）
- PostEdit.vue import 语句补全

## v2.0.11 (2026-05-25)

- 修复写文章选择标签时白屏的运行时错误

## v2.0.0 (2026-05-20)

- Rainy Glassmorphism UI 设计系统（水滴、涟漪、光泽动效）
- 16 项性能优化索引（查询速度提升 300 倍）
- Caffeine 缓存策略（5 个缓存实例）
- 校友圈功能增强（@mentions、位置标签、话题标签、显示切换）
- Messages 布局修复（浮动返回按钮）
- 所有页面宽度统一扩展至 1400px
