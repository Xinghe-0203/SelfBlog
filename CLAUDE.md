# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**版本**: v2.0.28 | **仓库**: https://github.com/Xinghe-0203/SelfBlog

---

## 代码修改规范

**【强制】修改代码前必须完整阅读相关文件：**

1. 阅读项目文档（README.md、CLAUDE.md）
2. 修改某个模块前，完整阅读该模块的 Controller → Service → Mapper → Entity
3. 通过源码确认关联关系，不要猜测
4. 确认现有功能后再决定复用还是新增

---

## 开发荣耻

以瞎清接口为耻，以认真查询为荣。
以模糊执行为耻，以寻求确认为荣。
以想业务为耻，以人类确认为荣。
以创造接口为耻，以复用现有为荣。
以跳过验证为耻，以主动测试为荣。
以破坏架构为耻，以遵循规范为荣。
以假装理解为耻，以诚实无知为荣。
以盲目修改为耻，以谨慎重构为荣。
以忘记更文档为耻，以及时更新为荣。

---

## 项目定位

**单人作者个人博客**（v2.0.25 由多用户校园社区 `campus_blog` 改造而来）。这一根本转变决定了大部分设计：

- **访客只读**：阅读、搜索、匿名点赞/留言、浏览相册均无需登录
- **注册已关闭**：`/auth/register/*` 与 `POST /user/register` 一律抛 `BusinessException(403, "此博客不开放访客注册")`；后台登录拒绝非管理员账号
- **写入仅管理员**：`app.single-author-mode: true` 时兜底规则要求 `ROLE_admin`；`/api/admin/**` 需 `hasRole("admin")`
- **发布即上线**：作者创建文章/动态直接 `status=1`，**不要引入待审核闸门**（`approvePost` 只对历史 `status=0` 数据生效）
- 历史社区表（`blog_follow`/`blog_message`/`blog_notification`、`views/user/*`、`views/circle/*`）仍保留在代码与库中，但公开入口大多已下线；改动前先确认该路径是否仍被路由引用

**两个独立工程，没有根构建清单**：`backend/`（Spring Boot 3.3.13 + Java 21 + MyBatis Plus）与 `frontend/`（Vue 3 + Vite 6 + Pinia）。命令都要在对应目录内执行。

---

## 常用命令

### 后端（`backend/`）

本机 `mvn` **不在 PATH 上**，且 `JAVA_HOME` 指向项目不支持的 JDK 25。仓库已带 Maven Wrapper（`backend/mvnw`、`backend/mvnw.cmd`，wrapper 3.3.4 / Maven 3.9.12），已核实可用的调用方式：

```bash
cd backend
export JAVA_HOME='C:\Program Files\Java\jdk-21'
./mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"   # PowerShell 里 -D 参数要加引号
```

- **跑后端测试必须带 `-Dskip.npm=true -Dskip.installnodenpm=true`**。`pom.xml` 里的 `frontend-maven-plugin` 在 `initialize`/`generate-resources` 阶段会在 `../frontend` 执行 `npm ci` + `npm run build`：不带这两个开关，一次测试会先把整个前端重建一遍，并且**会改写前端构建产物**。带上后全套测试约 1 分钟。
- 基线：**1140 tests, 0 failures**（2026-10-22 实测；CI 还会用真实 MySQL 8 再跑一遍）。日志里大量 `[PERF] ... error:` 是 `PerformanceMonitoringAspect` 记录的正常负例断言，不是失败。
- 单测：`"$MVN" test -Dtest=AuthControllerTest -Dskip.npm=true -Dskip.installnodenpm=true`，加 `#方法名` 可只跑一条。
- 打包：`"$MVN" clean package -DskipTests` → `target/selfblog.jar`（`finalName=selfblog`）。
- 本地起服务：先把 `backend/.env.example` 复制为 `backend/.env`，填 `JWT_SECRET`（≥32 位）与 `ADMIN_PASSWORD`（≥12 位）。
- `.env` 由 `config/DotenvConfig.java` 用 Java `Properties` 解析（**不是** shell dotenv：不要加引号），查找顺序为工作目录 → 用户主目录；文件值会成为 JVM property，优先级高于系统环境变量。

### 前端（`frontend/`）

```bash
cd frontend
./node_modules/.bin/vite                                  # = npm run dev，端口 3000
./node_modules/.bin/vue-tsc --noEmit                      # = npm run type-check
./node_modules/.bin/eslint src                            # = npm run lint:check
./node_modules/.bin/vitest run                            # = npm run test:run
./node_modules/.bin/vue-tsc --noEmit && ./node_modules/.bin/vite build   # = npm run build
```

- **本机 `npm run <script>` 全部失败**（`'"node"' 不是内部或外部命令`），`dev` / `build` / `test:run` 都不例外；`npm ci`、`npm install` 正常。原因不在仓库：系统 PATH 中 `C:\Program Files\Java\jdk-25\bin"` 这一项带了一个**不配对的引号**，`cmd.exe` 解析 PATH 时把它当成未闭合引号的开头，其后所有条目失效——`node` 无法被 `cmd` 解析（`where node` 仍能找到，走的是 App Paths），而 `node_modules/.bin/*.cmd` 垫片结尾正是调用裸 `node`。bash 不受影响。**因此一律直接调用 `./node_modules/.bin/<tool>`**（POSIX 垫片，可用；上面五条均已实测通过）。根治办法是删掉那条 PATH 里多余的引号；临时办法是在 PowerShell 里把 `backend/target/frontend/node` 前置到 `PATH`（该目录由 Maven 插件生成，`target/` 被清理后需重新构建才有）。
- 测试基线：**51 个文件 / 496 tests 全通过**（2026-10-22 实测）。
- 闸门：`lint:check`、`format:check`、`type-check`、`test:run`、`build` 全绿；部署适配另跑 `npm run test:deployment`（Vercel 代理生成器 + Docker/Compose 契约，27 项）。`format:check` 自 2026-10-17 起是真实可用的仓库级闸门（`.prettierrc` 改为 `endOfLine: "auto"` 并重写了一次历史漂移）。
- `tsconfig.json` 开启 `strict` + **`noUncheckedIndexedAccess`**：索引访问返回 `T | undefined`，`arr[0].foo` 过不了 `type-check`。用 `arr[0]?.foo`，或改用 `replace` 而非 `split()[0]` 这类写法。
- 开发端口 3000，`/api` 与 `/uploads` 代理到 `VITE_API_TARGET`（默认 `http://localhost:8825`）。SPA 在没有后端时也能靠 `stores/site.ts` 的默认值正常渲染（`loadPublicSettings` 的 `fetch` 失败被静默吞掉），所以**只跑 `./node_modules/.bin/vite` 就能验收布局与样式**。

---

## 后端架构

源码根：`backend/src/main/java/com/selfblog/`。

### 分层与「门面 / 查询 / 交互」三分

`Controller → Service → Mapper → Entity`，统一响应 `Result<T>{code,message,data,timestamp}`（分页数据含 `records/total/size/current/pages`）。

Post 与 Circle 采用同一套拆分，**新增功能要放对位置，不要在门面里复制逻辑**：

| 角色 | 例子 | 职责 |
|------|------|------|
| 门面 Facade | `service/post/BlogPostService(Impl)`、`service/circle/CircleService(Impl)` | CRUD、归属校验、跨服务编排；把查询/交互方法**一行委托**出去，并重新标注事务注解 |
| 查询 Query | `PostQueryService(Impl)`、`CircleQueryService(Impl)` | 列表/搜索/详情等只读逻辑，`@Transactional(readOnly = true)`，`@Cacheable` 也在这里 |
| 交互 Interaction | `PostInteractionService(Impl)`、`CircleInteractionServiceImpl` | 计数与状态副作用（views/like/comment/collect），`@Transactional(rollbackFor = Exception.class)` |

注意门面实现与查询实现**都**继承同一个实体的 `ServiceImpl<XxxMapper, Entity>`。

### 通用约定

- 响应与异常：`common.result.Result`、`common.exception.BusinessException(code, message)`；全局处理在 `GlobalExceptionHandler`（`@RestControllerAdvice`，把业务 code 映射为对应 HTTP 状态，兜底 500）
- 取当前用户用 `utils.SecurityUtils`：`getCurrentUserId()`（未登录抛 401）与 `getCurrentUserIdOrNull()`（游客场景用，如详情页判断是否作者本人）
- 新增公开接口必须在 `config/security/SecurityConfig.java` 显式放行；测试用无 `/api` 前缀的路径
- 实体软删除用 `@TableLogic private Integer isDeleted`（20+ 个实体）＋ `application.yml` 的 `logic-delete-field`；**手写 SQL 必须自己带上 `is_deleted=0`**
- 限流：`config/web/RateLimitInterceptor` 按**路径前缀**匹配 `RateLimitProperties`（Caffeine 计数，1 分钟窗口），超限返回 429 JSON；新增接口若不在已有前缀下，要补一条阈值

### 数据库

`DB_TYPE` 决定数据源，由 `config/db/DatabaseConfig.java` 创建（该 Bean 带 `@ConditionalOnProperty("DB_TYPE")`）：

- **SQLite（默认）**：`jdbc:sqlite:campus_blog.db`，Hikari 池大小 1，自动执行 `src/main/resources/schema-sqlite.sql`（`CREATE TABLE IF NOT EXISTS`，**不迁移既有文件**）
- **MySQL**：`schema-fresh-install.sql` 只用于全新空库；**已有库只能跑 `migration-selfblog.sql`**（`schema-fresh-install.sql` 含删表语句，改名就是为了降低误执行风险）
- **H2（测试）**：`DB_TYPE` 留空，走 `src/test/resources/application.yml` + `schema.sql`；若设置了 `DB_TYPE`，`DatabaseConfig` 会顶掉测试数据源

Mapper XML 支持 `databaseId="mysql"` / `"sqlite"` 双语句（`MybatisPlusConfig` 注册 `VendorDatabaseIdProvider`）；**H2 没有映射 id，只会走无 databaseId 的兜底语句**，所以不要把 MySQL 专有 SQL 写进共享语句。

表分两组：历史社区表（`sys_user`、`blog_post`、`blog_comment`、`blog_tag`、`blog_post_tag`、`blog_like`、`blog_collect`、`blog_share`、`blog_follow`、`blog_notification`、`blog_trending`、`blog_draft`、`blog_draft_tag`、`blog_report`、`blog_circle_*`、`blog_media`、`blog_post_media`、`blog_topic`、`blog_message`）与个人博客新增表（`site_setting`、`visitor_event`、`anonymous_post_like`、`guest_comment`、`photo_album`、`album_photo`）。

> `site_setting` 是 **KV 表**（`setting_key` 主键 + `setting_value`）。新增站点设置键**零 schema 迁移**，只需在 `SiteSettingsServiceImpl` 登记并做校验。

### 缓存

`config/cache/CaffeineCacheConfig.java` 用 `SimpleCacheManager` 注册 9 个具名缓存，各自独立 TTL：`hotTagsCache`(10m)、`categoryCache`(30m)、`userCache`(10m)、`trendingCache`(5m)、`statsCache`(15m)、`statusCache`(2m)、`siteSettingsCache`(10m)、`visitorSummaryCache`(30s)、`notificationListCache`(2m)。测试 profile 关闭缓存。

### 路由前缀、SPA 与可抓取 HTML

- REST 控制器由 `WebMvcConfig.configurePathMatch(... addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class)))` **自动加 `/api` 前缀**，源码里不写；因此新写的 `@RestController` 不要自己带 `/api`
- `controller/SpaController.java` **不再统一回退空壳 index.html**：公开历史路由（`/`、`/about`、`/archives`、`/gallery`、`/post/{id}`、`/tag/{id}`、`/pages/{slug}`）由 `service/site/seo/` 组装**可直接阅读的 HTML**（Vue 挂载后替换 `#app` 内的 `.seo-reading`），状态码真实（缺失 404、`/index.html` 308、模板缺失 503）；后台命名空间与历史账户路由返回启动壳并强制 noindex
- `SiteOrigin` 统一解析站点 origin，优先级：**后台「网站设置 → 网站域名」(`siteBaseUrl`) → 环境变量 `SITE_BASE_URL` → 直连请求地址**；配置后伪造 `Host`/`X-Forwarded-*` 不再影响 canonical/订阅/站点地图
- `controller/site/SeoController.java` 刻意是 **`@Controller` 而非 `@RestController`**，以便逃过 `/api` 前缀，在**根路径**提供 `/feed.xml`、`/atom.xml`、`/sitemap*.xml`、`/robots.txt`；站点地图是 index + 每片 1000 条的分片，robots 按实际 `adminPath` 生成
- 后台路径不是后端概念：`adminPath` 是站点设置（初值取 `ADMIN_PATH`，默认 `admin`，被 `normalizeAdminPath` 约束为 `[A-Za-z0-9][A-Za-z0-9_-]{1,39}` 并拒绝保留词）。后端安全永远只看 `/api/admin/**`
- Knife4j 在 `prod` profile 关闭；健康检查 `/actuator/health`

---

## 前端架构

### 启动顺序（`src/main.ts`，顺序是承重的）

`loadPublicSettings()` → `installAdminRoutes(siteStore.adminPath)` → `restoreFromStorage()` → `app.use(router)` → `mount('#app')`。

- 后台路由**在运行时按服务端下发的 `adminPath` 动态注册**（`router/modules/admin.ts` 的 `createAdminRoutes`），不在首次导航前装好就会 404
- 鉴权状态必须在 `use(router)` 之前恢复，否则首个 `beforeEach` 会把已登录作者弹回登录页
- 挂载后才异步拉取公开页面配置摘要（`void usePagesStore(pinia).loadManifest()`）；它是可选的，失败不得阻塞启动

### API 契约（最容易踩的一条）

`src/api/index.ts` 的 Axios 响应拦截器**返回的是后端信封本身，不是 `AxiosResponse`**：`api.get<T>()` 解析出的就是 `{ code, message, data }`，所以调用方读 `res.data` 而不是 `res.data.data`。同文件还负责：请求拦截注入 `Authorization`/`X-XSRF-TOKEN`/`X-Visitor-Id`、`isRefreshing` + `failedQueue` 的刷新排队重放、`handleAuthError()` 清理 Pinia/localStorage 并跳回 `adminRoot`、以及重试与 `error.userMessage` 文案。

`baseURL` 硬编码为 `/api`——`VITE_API_BASE_URL` 实际**没有任何地方读取**，只靠 Vite 代理与 `vercel.json` 重写生效。

### 路由与状态

- 路由由 `router/modules/` 组装，守卫在 `router/guards.ts`；`requiresAuth`/`requiresAdmin` 跳 `siteStore.adminRoot` 并带 `redirect` 查询参数
- `views/auth/` 下只剩重定向：`/login` → `adminRoot`；组件里需要登录时统一 `router.push('/login')`，不要写死路径
- Store 五个：`site.ts`（站点设置、`adminPath`/`adminRoot`、皮肤与 favicon）、`theme.ts`（`localStorage['edu_theme']`、`.dark-mode`、`matchMedia` 监听）、`user.ts`（store id 是 `'auth'`，导出别名 `useUserStore`，token 经 `utils/crypto` 编解码存取，`validateToken` 只解 JWT 不请求网络）、`music.ts`（唯一音频宿主的状态）、`pages.ts`（**只保存已发布页面配置**，草稿绝不进公开 store）
- `useConfirm()` 返回的 `ConfirmDialog` **必须渲染到模板里**，否则确认框不出现

### 设计系统

`src/styles/main.css` 只是 `@import` 入口，token 在 `variables.css`，暗色覆盖在 `dark-mode.css`，皮肤在 `skins.css`（`#selfblog[data-skin=…]`，`ocean` 默认）。

- **不要拿 `--gray-*` 当「深色表面」**：暗色模式会整体反转灰度梯度（`--gray-900` 由 `#111111` 变 `#EBEBEB`），按钮和代码块会变成刺眼亮块。需要「永远深色 / 高对比」时用语义 token：代码块 `--code-bg/--code-text/--code-border`，高对比实心按钮 `--contrast-bg/--contrast-bg-hover/--contrast-bg-active/--contrast-fg`
- 圆角一律用 `--radius-*` token，不要再写裸值
- 布局硬约束：`.app-main` 的 `padding-top` 与 `PostDetail` 的锚点依赖 `html { scroll-padding-top }` / `scroll-margin-top`，改动前先确认锚点没被推挤

---

## 文档维护（改代码就要做）

1. **`CHANGELOG.md`**：在顶部追加 `## 未发布 — <标题> (日期)`，正文写「说明 / 修复」要点，并附 **`### 影响文件`** 表格（文件 + 改动类型）。已有条目可为格式范例。
2. 受影响的分册文档：`README.md`、`backend/README.md`、`frontend/README.md`、`backend/DEPLOY.md`。**注意这些文档的版本号与结构清单常落后于代码，以 `pom.xml` / `package.json` / 实际目录为准。**
3. 较大的改动先写设计再实现：设计稿（含「已核实的现状」证据表）与带 `- [ ]` 复选框的可独立验收任务清单放在 `docs/` 下（历史上曾用 `docs/superpowers/specs/` 与 `docs/superpowers/plans/`，该目录已于 2026-10-19 清理）。

---

## 写测试

- **后端 `@WebMvcTest` 测管理员控制器**：必须 `@MockBean JwtUtils`，否则 `SecurityConfig` 拉起的 `jwtAuthenticationFilter` 找不到 Bean 直接启动失败；再加 `@WithMockUser(roles = "admin")` 才返回 200。方法级 `@PreAuthorize` **在切片里是生效的**（非管理员 403、匿名 403）。不要用 `@AutoConfigureMockMvc(addFilters = false)`——实测它返回 401 而不是绕过鉴权。
- **前端 jsdom**：`src/test/setup.ts` 只桩了 `matchMedia` 和 `$router/$route`，**没有**做自动卸载；凡挂载会注册全局监听器的组件，测试里要自己 `enableAutoUnmount(afterEach)`。需要逐帧行为时不能桩成同步 rAF，要排队后手动 flush；改 `scrollY` 要用 `Object.defineProperty` 再派发 `scroll`。路由优先用真实的 `createMemoryHistory()`，别用 `stubs: { RouterLink: true }`（stub 会丢掉 slot 内容，也表达不了 active 态）。

---

## 其他指南

- `AGENTS.md`：更细的本机构建环境与验证笔记（Maven/Node 目录、切片测试配方、Playwright、页面/SEO 不变量），与本文互补。
- `frontend/CLAUDE.md`：前端设计 token 使用约定、路由约定与页面配置/预览约束。
- `docs/page-management.md`：后台「页面管理」使用说明；`docs/decisions/` 是 ADR；`docs/reviews/` 是各批次真实验证记录。（历史规格与计划目录 `docs/superpowers/` 已于 2026-10-19 清理，**当前行为以 CHANGELOG 与验证记录为准**。）

---

## 已知问题（改动前先看）

- **Docker 镜像未在本机验证**：`backend/Dockerfile` 与两个 compose 文件已改为“仓库根上下文 + 真实 `selfblog.jar` + 数据/上传/相册卷”，但本机没有 Docker 守护进程，**镜像构建与运行仍未验证**；部署前请自行 `docker compose -f backend/docker-compose.yml up -d --build` 验证。
- **真实 Vercel 部署未验证**：`frontend/vercel.json` 只做统一反向代理（`framework: null` + `scripts/build-vercel-proxy.mjs`），需要 `SELFBLOG_BACKEND_ORIGIN` 与 `SITE_BASE_URL` 两个环境变量；没有账号/域名时无法真机验证。
- **MySQL 运行时只在 CI 验证**：本机没有 MySQL/Docker 守护进程，全量套件只能在 H2 上跑；真实 MySQL 8 由 CI 的 `mysql` job 覆盖（从 `schema-fresh-install.sql` 初始化后跑全量）。**H2 会假绿**：跨连接的锁问题（如 `REQUIRES_NEW` 自增等待调用方未提交的行锁）在 H2 上不报错、在真实 MySQL 上等满 `innodb_lock_wait_timeout` 后抛 `CannotAcquireLockException`——改事务边界后必须等 MySQL job 的结论，别只看 H2 绿。
- `views/user/*` 等历史社区页仍在仓库中（路由仍注册），但已无公开入口；`views/auth/Register.vue`、`views/circle/*`、`components/circle/*` 等死代码已于 2026-10-17 删除（见 CHANGELOG）。后端对应的社区/话题接口与数据表保留。
