# 更新日志 (CHANGELOG)

所有重要更改都将记录在此文件。

---

## v2.0.26 — 评论限流与改密会话安全 (2026-10-17)

### 修复

- 匿名评论创建 `POST /api/guest-comments` 不再因配置的尾斜杠而跳过限流：使用 MVC 解码后的查找路径（去除应用上下文），路径段边界匹配同时覆盖根路径与子路径，最长规则优先；编码同一路径共用计数，保留每 IP 每路径计数和现有窗口，第 6 次创建返回 429。
- 访问与刷新 JWT 增加密码绑定的 `credentialVersion`（域分离 HMAC，不暴露 BCrypt 哈希），每次鉴权/刷新读取当前用户凭据。改密、管理员重置、邮箱重置提交后全部旧令牌失效，包括未登记设备的令牌及服务重启后的令牌；缺失版本、不可用用户、数据库错误均拒绝认证。
- 登录令牌对绑定实际验证的密码快照；刷新与轮换继承旧版本，避免并发改密把旧刷新令牌升级为新凭据。实体更新不再隐式写回密码，三种改密及旧样例密码初始化使用条件更新，防止旧实体覆盖新密码、复活旧会话。
- 升级无需 schema 迁移，升级前令牌需重新登录；多实例必须同时升级。普通登出/轮换的黑名单和限流计数仍是进程内状态，本次不扩展为持久分布式会话。

### 验证

- 最终后端全量（H2）：588 项测试，0 失败、0 错误、0 跳过，`BUILD SUCCESS`；包含限流、JWT、真实 MockMvc 评论请求、改密/两种重置、新鉴权实例、独立 SQL 会话读取已提交密码及旧实体写回回归。
- 最终独立 SQLite 专项：限流与评论/会话集成共 14 项，0 失败、0 错误、0 跳过，`BUILD SUCCESS`。未在本机验证真实 MySQL 或部署运行；本次不涉及前端改动。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/src/main/java/com/selfblog/config/web/RateLimitInterceptor.java` | 路径段匹配与最长规则优先 |
| `backend/src/main/java/com/selfblog/utils/JwtUtils.java`、`backend/src/main/java/com/selfblog/config/security/JwtAuthenticationFilter.java` | 凭据版本、撤销与轮换验证，以及过滤器说明 |
| `backend/src/main/java/com/selfblog/controller/user/SysUserController.java` | 刷新继承已验证版本 |
| `backend/src/main/java/com/selfblog/service/auth/impl/SysUserServiceImpl.java` | 登录快照与三种密码条件更新 |
| `backend/src/main/java/com/selfblog/entity/SysUser.java` | 禁止通用更新写密码 |
| `backend/src/main/java/com/selfblog/mapper/SysUserMapper.java`、`backend/src/main/resources/mapper/SysUserMapper.xml` | 密码比较并更新 SQL |
| `backend/src/main/java/com/selfblog/config/db/DataInitializer.java` | 旧样例密码条件更新 |
| `backend/src/test/java/com/selfblog/config/web/RateLimitInterceptorTest.java`、`backend/src/test/java/com/selfblog/utils/JwtCredentialVersionTest.java`、`backend/src/test/java/com/selfblog/controller/CommentSessionSecurityTest.java` | 新增安全回归 |
| `backend/src/test/java/com/selfblog/utils/JwtUtilsTest.java`、`backend/src/test/java/com/selfblog/service/impl/SysUserServiceImplTest.java` | 按新签发接口调整测试桩和验证 |
| `README.md`、`backend/README.md`、`backend/DEPLOY.md` | 安全行为与升级边界 |
| `docs/superpowers/specs/2026-10-17-comment-session-security.md`、`docs/superpowers/plans/2026-10-17-comment-session-security.md` | 设计证据与实施计划 |

---

## v2.0.26 — P0–P3 加固批次：CI、Maven Wrapper、GFM 一致性与清理 (2026-10-17)

### 说明

- **CI（新增）**：`.github/workflows/ci.yml` 四个真实执行任务——`backend`（Temurin 21 + `./mvnw` + H2 全量）、`frontend`（Node 24.15.0 + `npm ci` + lint/format/type/test/deployment/build）、`mysql`（用 `backend/schema-fresh-install.sql` 初始化真实 MySQL 8 后跑全量后端测试）、`docker`（仓库根上下文构建镜像 → SQLite 起容器 → 等 `/actuator/health` → 抓取服务端渲染首页 `seo-reading`）。本机没有 MySQL/Docker 守护进程，真实 MySQL 与容器运行以 CI 为唯一已验证通道。
- **Maven Wrapper**：`backend/mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties`（only-script，wrapper 3.3.4，Maven 3.9.12，canonical `repo.maven.apache.org` 分发地址）；本地与 CI 共用同一入口，`-D` 参数在 PowerShell 中需加引号。
- **GFM 一致性**：后端 `SafeMarkdown` 增加 commonmark 表格/删除线/任务列表扩展，与前端 `marked` + DOMPurify 输出对齐；两侧共用 fixture `docs/fixtures/gfm-sample.md`（`SafeMarkdownTest` 与前端 `markdownGfm.test.ts` 同时断言）。任务列表复选框在两侧都被安全策略移除，仅保留条目文字。
- **深色首闪**：`frontend/index.html` 增加内联主题引导脚本，在 Vue 挂载与首帧之前根据 `edu_theme` 加 `html.dark-mode`；`seo-reading.css` 暗色改为 `html.dark-mode` 键控（不再用 `prefers-color-scheme`，避免与站内主题相反时二次闪烁）。新增 `src/__tests__/indexHtml.test.ts` 直接执行 `index.html?raw` 的真实脚本。
- **阅读 HTML 缓存**：200 非 SHELL 响应带 ETag + `Cache-Control: public, max-age=0, must-revalidate`，`If-None-Match` 命中返回 304；错误页与 SHELL 保持 `no-store`。回归测试用 Mapper 更新（而非 `JdbcTemplate`），否则 MyBatis 本地会话缓存会掩盖变更。
- **SEO 补强**：首页 JSON-LD 增加 `WebSite` + `SearchAction`；标签站点地图使用已发布文章的 `MAX(COALESCE(update_time, create_time))` 作为 `lastmod`（`BlogPostTagMapper.selectTagLastModified` + 类型化 DTO `TagLastModified`；H2 会把未加引号的别名转大写，不能用 `Map` 读）。
- **前端遗留清理（第二波 25 个文件）**：删除仅有旧社区页面引用的 `api/{circle,collect,comment,follow,like,report}.ts`、`types/{circle,follow}.ts`、`components/common/{CollectButton,FollowButton,LikeButton}.vue`、`views/admin/{Reports,Statistics,Users}.vue`、`views/common/Report.vue` 与 10 个 `views/user/*` 页面；保留 `views/user/PasswordChange.vue` 并在后台设置的“后台与访客”面板新增“修改密码”入口。`api/user.ts`、`stores/user.ts`、`types/comment.ts` 保留。
- **H2 唯一键对齐**：测试 schema 补齐 `sys_user` (username, email)、`blog_circle_like` (user_id, post_id)、`blog_topic` (name) 唯一约束，与 MySQL/SQLite 生产 schema 一致（此前只有 tag/like/collect/follow/post_tag/trending/draft_tag）。
- **部署文件整理**：`backend/schema.sql` → `backend/schema-fresh-install.sql`（含 `DROP TABLE`，改名降低误执行风险），同步 `docker-compose.yml` 与全部文档引用；删除无用的 `backend/entrypoint.sh` 与 `backend/.dockerignore`（构建上下文本来就是仓库根）。
- **`format:check` 修复**：`.prettierrc` `endOfLine: "lf"` → `"auto"`，一次性重写历史漂移文件；`format:check` 成为仓库级真实闸门并加入 CI。
- **AGENTS.md 拆分**：根 `AGENTS.md` 变为入口页（命令快路径、CI、项目级记录），后端/前端详细规则分别移到 `docs/agents/backend.md`、`docs/agents/frontend.md`；同步更新根 `CLAUDE.md`、`frontend/CLAUDE.md`、`README.md`（含 v2.0.26 条目与文档索引）。
- 版本：`frontend/package.json` 2.0.25 → 2.0.26；`backend/pom.xml` 0.0.1-SNAPSHOT → 2.0.26；本文件此前所有“未发布”章节一并归档到 v2.0.26。

### 验证

- 本地实测：后端 `./mvnw.cmd test -Dskip.npm=true -Dskip.installnodenpm=true` = **564 tests, 0 failures**；前端 43 文件 / 400 tests，`lint:check`（0 错误 / 43 警告）、`format:check`、`type-check`、`build`、`test:deployment` 27/27 全绿。
- CI：`mysql` 与 `docker` 任务是本仓库对真实 MySQL 与容器运行的唯一验证通道。
- 仍未验证（不当作已验证）：真实 Vercel 部署、搜索引擎实际收录。
- CI 首轮运行即抓到 4 类本地 H2 看不到的真实缺陷，已修复：MySQL 的 `TIMESTAMPADD('MINUTE', …)` 引号单位（`SysUserMapper`）、趋势 upsert 绑定 null 计数（`TrendingServiceImpl` 显式落 0，H2 测试 schema 同步补齐 NOT NULL 约束）、测试 SQL 字符串里的下划线数字字面量（MySQL 当列名）、CI 裸 `docker run` 漏传 `JWT_EXPIRATION`/`JWT_REFRESH_EXPIRATION` 导致启动校验拒启；第二轮又抓到第 5 类：MySQL 单表 UPDATE 赋值从左到右求值导致登录锁定提前一次（`SysUserMapper.incrementLoginFailCount` 调整赋值顺序）。详见 `docs/agents/backend.md` 的 “Database portability” 一节。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `.github/workflows/ci.yml` | 新增（4 任务 CI） |
| `backend/mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties` | 新增（Maven Wrapper） |
| `backend/schema.sql` → `schema-fresh-install.sql`、`docker-compose.yml`、`DEPLOY.md`、`README.md`、`CLAUDE.md` | 改名与引用同步 |
| `backend/entrypoint.sh`、`backend/.dockerignore` | 删除（无实际用途） |
| `backend/pom.xml`、`frontend/package.json`(+lock) | 版本与 GFM 依赖 |
| `backend/src/main/java/com/selfblog/{service/site/seo/*,controller/SpaController.java,mapper/BlogPostTagMapper.*,dto/site/TagLastModified.java}`、`static/seo-reading.css` | 阅读 HTML/GFM/ETag/JSON-LD/lastmod |
| `backend/src/test/resources/schema.sql` | H2 唯一键对齐 |
| `SafeMarkdownTest`、`ReadingHtmlRendererTest`、`ReadingHtmlControllerTest`、`SitemapServiceTest` | 断言更新与新增 |
| `frontend/index.html`、`src/__tests__/indexHtml.test.ts`、`src/components/pages/__tests__/markdownGfm.test.ts`、`vite.config.ts` | 首屏主题 + GFM/首屏测试 |
| `frontend/src/views/admin/Settings.vue`、`router/modules/{user,auth,index}.ts`、25 个被删文件 | 遗留清理与密码入口 |
| `docs/agents/backend.md`、`docs/agents/frontend.md`、`docs/fixtures/gfm-sample.md` | 新增（拆分文档与共享 fixture） |
| `frontend/.prettierrc` 与漂移文件 | format 闸门修复 |

---

## v2.0.26 — 死代码清理与文档同步 (2026-10-17)

### 说明

- 删除 **22 个长期零引用的前端文件**：从未注册路由的校友圈树（`router/modules/circle.ts`、`views/circle/*`、`components/circle/*`）、无入口的管理页（`CircleManagement.vue`、`TopicsManagement.vue`）、旧首页组件（`HomeFilters/HomeSidebar/PostCardList.vue`）、旧页面（`TopicDetail.vue`、`TrendingPage.vue`、`PasswordReset.vue`、`Register.vue`）、`Card.vue`、`usePagination.ts`、`vo.ts`、`api/topic.ts`、`types/topic.ts` 等；每个删除前都用引用扫描确认无任何导入（含测试）。
- 同时移除 `api/admin.ts` 中已无消费者的 circle/topic 接口封装与相关类型导出。**后端社区/话题接口与数据表保留**（仍是管理员 API 的一部分），只是删掉死掉的前端入口。
- 文档同步：`frontend/CLAUDE.md`（结构、Vercel 代理模型、删除清单，并移除过时的 v2.0.22 重复修复表）、`frontend/README.md`（结构）、根 `README.md`（文档索引补齐 `docs/`、测试数）、`backend/README.md`（测试数与前端入口说明）、`AGENTS.md`（清理记录与最新计数）。
- 验证：前端 41 文件/394 测试、类型检查、构建与 lint（0 错误；警告从 256 降至 159，因删除死代码而自然减少）；死文件扫描结果为“无未引用文件”；重打包单 JAR 后浏览器回归 **32/32** 通过。
- 未改动：后端业务代码、数据库结构、用户根 `CLAUDE.md`（其“已知问题”里关于 circle 路由的说明已过时，但该文件带有你的未提交修改，保持不动）。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| 上列 22 个前端文件 | 删除（零引用死代码） |
| `frontend/src/api/admin.ts`、`types/index.ts`、`types/admin.ts` | 移除失效的 circle/topic 封装与导出 |
| `README.md`、`frontend/README.md`、`frontend/CLAUDE.md`、`backend/README.md`、`AGENTS.md` | 文档同步 |

---

## v2.0.26 — 文章发布/编辑的标签与媒体修复 (2026-10-17)

### 说明（均为“先复现、再修复”）

- **修复：编辑文章后标签全部消失，且再也加不回来。** 真实环境（SQLite/MySQL）的 `blog_post_tag` 有 `UNIQUE(post_id, tag_id)`；编辑文章会先逻辑删除旧关联（`is_deleted=1`），随后 `INSERT IGNORE` 被旧行占据的唯一键静默拦下，标签永久失效。现改为按数据库方言 upsert 并把 `is_deleted` 复位为 0（MySQL `ON DUPLICATE KEY UPDATE`、SQLite `ON CONFLICT … DO UPDATE`、H2 `MERGE … KEY`）。H2 测试库此前缺少该唯一键，导致单测看不到问题，已补齐以与生产一致。
- **修复：`BlogPostMedia.selectByPostId` 未过滤软删除**，编辑文章替换配图后，编辑器与公开接口会继续返回已移除的旧图。查询补 `is_deleted = 0`，并补上此前缺失的 H2 兜底批量插入语句。
- **修复：同时提交 `tagIds` 与 `tagNames` 时新标签被静默丢弃。** 编辑器会同时发送两个字段（已有标签带 id、新输入只有名字），后端旧逻辑“tagIds 非空就忽略 tagNames”。现按并集解析（名字走 getOrCreate 后与 id 去重合并），混用场景不再丢标签。
- **修复：写作页标签输入上限 50 字，与后端 20 字限制不一致**（此前只修过后台标签管理页）。输入框改为 20 字，仍以后端校验为准。
- 新增回归：`PostTagLifecycleTest`（编辑保留/移除/恢复/混用）、`BlogPostMediaMapperTest`（替换媒体不返回软删行）、`PostEdit.test.ts`（输入上限与发布载荷契约）；真实单 JAR 联调新增“编辑后标签保留、可恢复”检查。
- **同类问题系统排查（同一批次）**：
  - **标签删除后重建同名标签**：`blog_tag.name` 唯一 + 逻辑删除，旧 `createTag`/`getOrCreateTag` 在唯一键冲突后按“未删除”条件查询只能拿到 null → 创建接口返回空、发文章时该标签丢失（实测 NPE/500）。改为原始查询找回同名行并恢复 `is_deleted=0`（新增 `selectRawByName`/`restoreById`）。
  - **趋势表同类缺陷**：`blog_trending` 唯一键 (post_id, date) + 逻辑删除；每日 upsert 未复位 `is_deleted`（被删过的那天永远查不到），查询也未过滤软删行。三种方言的 upsert 统一复位标记并补齐查询条件。
  - **文章接口缺长度校验**：`category`（库列 50）与 `coverImage`（库列 500）没有 DTO 约束，超长会在数据库层报 500（SQLite 甚至静默存入）。补 `@Size` 后统一返回 400。
  - **测试库对齐生产约束**：H2 补上 `blog_tag(name)`、`blog_like`、`blog_collect`、`blog_follow`、`blog_trending`、`blog_draft_tag` 的唯一键（此前只有 2 个约束），这类“生产有唯一键、测试没有”的盲区不再出现。
  - **唯一键冲突的异常类型不可移植（真实联调两轮才修净）**：Spring 只把 H2/MySQL 的唯一键冲突翻译成 `DuplicateKeyException`；SQLite 的 SQLState 为 null、error code 19，最终抛的是 `UncategorizedSQLException`。第一版只捕获 `DuplicateKeyException`（SQLite 仍 500），第二版捕获 `DataIntegrityViolationException`（仍不够），最终统一捕获可移植父类 `DataAccessException`，并且**只在真正找到冲突行时转换**，找不到就保留原始异常。同类共修复 5 处：标签创建/取用、页面地址预留（→409）、关注、动态点赞、用户创建、话题创建。
  - 排查中确认无需改动：点赞/收藏/关注早已使用“原始查询 + 恢复标记”的正确写法；`BlogPostMapper.fullTextSearch*`、社区 Circle/Topic 的少数 SQL 只有 MySQL/SQLite 变体（H2 无兜底），但当前无调用方或仅旧社区路径，未扩大改动范围。
- 全量验证：前端 41 文件/394 测试、类型检查与 Lint 通过；后端 559 测试通过；真实 JAR + SQLite 联调 11/11（新增“标签删除后重建”）、浏览器回归 32/32。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/src/main/resources/mapper/BlogPostTagMapper.xml` | 关联写入由 INSERT IGNORE 改为方言 upsert 并复位 `is_deleted` |
| `backend/src/main/resources/mapper/BlogPostMediaMapper.xml` | 查询补软删条件；补 H2 兜底批量插入 |
| `backend/.../service/post/impl/BlogPostServiceImpl.java` | `resolveTagIds` 改为 tagIds/tagNames 并集 |
| `backend/src/test/resources/schema.sql` | H2 的 `blog_post_tag` 补 `UNIQUE(post_id, tag_id)`，与生产一致 |
| `backend/src/test/.../PostTagLifecycleTest.java`、`mapper/BlogPostMediaMapperTest.java` | 新增回归 |
| `frontend/src/views/post/PostEdit.vue` | 标签输入上限对齐后端 20 字 |
| `frontend/src/views/post/__tests__/PostEdit.test.ts` | 新增输入上限与载荷契约断言 |
| `backend/.../mapper/BlogTagMapper.java`、`service/post/impl/BlogTagServiceImpl.java` | 同名标签重建时恢复原行（原始查询 + 复位标记） |
| `backend/.../mapper/BlogTrendingMapper.xml` | upsert 复位 `is_deleted`；查询补软删条件 |
| `backend/.../dto/post/PostCreateRequest.java`、`controller/BlogPostControllerTest.java` | 分类/封面长度约束与 400 断言 |
| `backend/src/test/resources/schema.sql` | 测试库补齐生产已有的唯一键（tag/like/collect/follow/trending/draft_tag） |
| `backend/.../service/post/BlogTagLifecycleTest.java`、`mapper/BlogTrendingMapperTest.java` | 新增回归 |
| `backend/.../BlogTagConflictTest.java`、`service/site/impl/PageSlugConflictTest.java` | 唯一键冲突的异常类型契约（H2/MySQL/SQLite） |
| `backend/.../service/{site/impl/PageServiceImpl,social/impl/FollowServiceImpl,circle/impl/CircleInteractionServiceImpl,auth/impl/SysUserServiceImpl,content/impl/TopicServiceImpl}.java` | 统一捕获 `DataIntegrityViolationException` |

---

## v2.0.26 — 可抓取正文与统一部署 (2026-10-16)

### 说明

- 公开历史路由（`/`、`/about`、`/archives`、`/gallery`、`/post/{id}`、`/tag/{id}`、`/pages/{slug}`）不再返回空壳 SPA：JAR 直接输出可阅读 HTML（禁用 JavaScript 也能读正文），Vue 挂载后照常接管交互。首次响应重建 title/description/robots/canonical/Open Graph/Twitter 与 BlogPosting 结构化数据，`/index.html` 308 到 `/`，未知路径返回真正 404，模板缺失返回 503。
- 新增共享 `SiteOrigin`：设置 `SITE_BASE_URL` 后，canonical、分享图、订阅源、站点地图与 robots 都使用正式对外地址，伪造的 `Host`/`X-Forwarded-*` 不再影响；前端运行时读取 `selfblog-site-origin` 标记，避免挂载后把地址改回代理域名。
- `/sitemap.xml` 改为 sitemap index + 分片（`sitemap-site.xml`、`sitemap-posts-N.xml`、`sitemap-tags-N.xml`、`sitemap-pages-N.xml`），每片最多 1000 条，覆盖**全部**已发布文章、标签与自建内容页，并遵循每页 `indexable`；RSS/Atom 保留最近 100 篇的既定上限。robots 按实际后台路径动态生成。
- 公开 HTML/站点地图只读取已发布内容：管理员身份也不会让草稿出现在响应中；阅读请求不增加阅读量或访客事件。真实联调发现并修复了“内层 404 与外层共享事务导致缺失页面返回 500”的问题（`ReadingPageService` 不再声明类级事务）。
- Vercel 改为**统一反向代理**：`frontend/vercel.json` 不再部署第二份静态前端，改为在构建期由 `scripts/build-vercel-proxy.mjs` 生成 Build Output API v3 路由，把原路径/方法/查询转发到同一源站，动态响应 `no-store`，Preview 环境附加 `X-Robots-Tag: noindex,nofollow`；非法、环回或自指的 origin 直接构建失败。
- Docker 修复为可用形态：仓库根上下文、Maven+JDK21 构建阶段、JRE 非 root 运行、`curl` 健康检查、`selfblog.jar` 正确产物、数据/上传/相册三类持久化卷；保留既有 MySQL 方案卷名，另提供 SQLite Compose。本机无 Docker 守护进程，镜像运行未验证。
- 全量验证：前端 41 文件/393 测试、类型检查、Lint 0 错误/256 警告、构建与部署契约 27/27；后端 545 测试通过。真实单 JAR + SQLite 联调 9/9（无 JS 抓取、站点地图完整性、下线 404、Vue 接管无重复、本地代理传输、后台域名即时生效与回落），见[验证记录](docs/reviews/2026-10-16-seo-deployment-verification.md)。
- 新增「网站域名」后台设置（`siteBaseUrl`）：canonical、分享图、订阅源、站点地图与 robots 的 origin 可直接在后台修改，**后台设置为准**，留空回落到 `.env` 的 `SITE_BASE_URL` 或当前访问地址；非法输入被拒绝，数据库里被改坏的值记录警告并安全回落，不会让整站 500。保存后下一次请求生效（无需重启）。
- 保持已确认的界面与比例不变；不承诺搜索引擎收录或排名。Docker、真实 Vercel 部署、MySQL 运行仍未验证。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/.../service/site/seo/{SiteOrigin,SafeMarkdown,SeoModels,ReadingTemplate,ReadingContent,ReadingHtmlRenderer,ReadingPageService,SitemapService}.java` | 新增：origin 校验、Markdown 净化、阅读模型/文档渲染与分片站点地图 |
| `backend/.../controller/{SpaController,site/SeoController}.java`、`config/security/SecurityConfig.java`、`service/post/PostQueryService(+Impl).java` | 根路径 HTML 交付、发现文档重写、仅已发布查询、静态资源放行 |
| `backend/src/main/resources/static/seo-reading.css`、`pom.xml` | 阅读区域局部样式、CommonMark 依赖 |
| `backend/Dockerfile`、`docker-compose.yml`、`docker-compose.sqlite.yml`、根 `.dockerignore` | 单 JAR 容器构建、持久化卷与安全运行 |
| `frontend/vercel.json`、`scripts/{vercel-proxy,build-vercel-proxy}.mjs`、`package.json` | 统一代理生成器与部署契约测试入口 |
| `frontend/src/composables/useSeoMeta.ts` | 优先使用服务端 canonical origin |
| `frontend/src/stores/site.ts`、`views/admin/Settings.vue` | 新增「网站域名」设置项（类型、默认值与表单） |
| `backend/.../impl/SiteSettingsServiceImpl.java`、`service/site/seo/SiteOrigin.java` | 白名单登记与校验、后台设置优先的 origin 解析 |
| 对应前后端测试 | origin/Markdown、严格公开查询、阅读文档、HTTP 契约、站点地图边界、部署适配 |
| `docs/reviews/2026-10-16-seo-deployment-verification.md`、README 分册、DEPLOY、AGENTS/CLAUDE 规则、规格/计划 | 使用与部署说明、验证范围与限制 |

---

## v2.0.26 — 逐页配置与独立内容页（第一批） (2026-10-16)

### 说明

- 新增后台页面管理：8类内置公开页/详情模板的独立文案、模块开关、受控样式与SEO字段；关于页支持独立Markdown正文。
- 新增独立内容页、草稿/发布分离、真实组件预览、下线/删除和公开导航编辑；桌面链接按可用空间进入「更多」，手机保留完整导航。
- 复用现有KV表，不修改schema；草稿不公开，数据库条件更新防止旧版本静默覆盖，slug唯一预留，配置按UTF-8字节限制。
- 用户已取消全站放大：保持当前字号、导航高度和页面比例。未配置的页面继续采用现有内容与作者主题。
- 全量验证：前端41文件/389测试、类型检查、Lint0错误/256警告、构建及本轮文件格式检查通过；后端491测试通过，含两项真实H2独立事务竞争。隔离SQLite浏览器**31/31**通过，含单JAR直连、UI确认下线/删除、390px深色预览和首屏请求顺序，见[验证记录](docs/reviews/2026-10-16-page-management-verification.md)。
- 修复首屏配置摘要与About正文的并行加载顺序问题：同发布版本合并正文，版本更新则重新获取，不让摘要擦掉正文或旧响应覆盖新版本。
- 本批完善运行时元数据，不声称搜索引擎已经收录；服务端HTML SEO及已知Docker/Vercel欠项仍属后续批次。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/.../dto/site/PageModels.java`、`service/site/PageService.java`、`service/site/page/PageCatalog.java`、`service/site/impl/PageServiceImpl.java` | 新增：类型、白名单、版本化存储与发布/导航逻辑 |
| `backend/.../controller/admin/AdminPageController.java`、`controller/site/PublicPageController.java`、安全/限流/站点设置配置 | 新增接口及权限、限流、保留路径 |
| `frontend/src/{api,types,stores}/pages*`、`composables/usePage*.ts`、`components/pages/*` | 新增：公开配置、局部样式、安全正文及元数据生命周期 |
| `frontend/src/views/admin/pages/*`、公开页面、导航、路由与启动入口 | 页面编辑/预览/导航及消费端接入 |
| 对应前后端测试 | 新增草稿隔离、版本冲突、页面模块、路由保护、预览、导航与元数据回归 |
| `docs/page-management.md`、`docs/decisions/001-versioned-pages-in-existing-kv.md`、README分册、规格/计划 | 使用指南、架构取舍、验证范围与限制 |
| `backend/DEPLOY.md` | 升级、完整KV备份及旧后台命名空间兼容说明 |

---

## v2.0.26 — 导航原型还原与前后端联调修正 (2026-10-16)

### 说明

- 导航按批准原型恢复链接紧接搜索、右侧工具前留白、浅蓝胶囊选中态、静止声条＋「听一会儿」及音乐面板布局；保留 44px 触达、真实音频状态、作者品牌与主题配置。1440px 下第一个链接从 x=885 修正为 x=472（原型 x=471）。
- 修复后台抽屉切换桌面后的焦点/滚动锁残留、关闭侧栏仍可聚焦屏幕外链接及缺失的 aria-controls 目标。
- 修复发布/手动保存成功后误报未保存；改为完整提交快照，保护请求期间的新改动与失败请求，避免自动保存与发布竞争。真实路由挂载新增相应回归。
- 标签新建/编辑与后端既有 20 字限制对齐，保留具体失败原因；后端校验不变。
- 独立复核追加并修复：恢复后续草稿时保留原文章 ID、取消发布确认后焦点回到发布按钮；均先复现失败再验证修复。
- 前端 31 文件 / 345 测试、类型检查、Lint（0 错误 / 255 警告）与构建通过；后端 463 测试通过。真实 SQLite 联调初查 13/15，修正并扩展复核场景后为 **17/17**，见[复查报告](docs/reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md)。
- 原有数据库、上传目录和运行服务未被写入；临时联调实例已关闭。MySQL、Docker、SMTP 与外部媒体未在本次真实联调中验证。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `frontend/src/components/layout/Navbar.vue`、`NavbarMusicControl.vue`、`components/common/icons.ts` | 导航/音乐原型还原与高对比轨道降级 |
| `frontend/src/views/admin/AdminLayout.vue`、`TagsManagement.vue` | 抽屉生命周期及标签校验 |
| `frontend/src/views/post/PostEdit.vue` | 提交快照、未保存保护及异步写入互斥 |
| 对应 `__tests__/*.test.ts` | 新增和强化真实路由、交互与边界回归 |
| `docs/reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md` | 新增：差异证据、缺陷复现、真实验证范围与限制 |
| `README.md`、`frontend/README.md`、`backend/README.md` | 更新：修复结论、锁定版本、验证范围及限制 |
| `frontend/CLAUDE.md`、`AGENTS.md`、设计规格与计划 | 更新：约束、实际依赖版本、代理描述与交付记录 |

---

## v2.0.26 — 清透空间重设计（三）：作者工作台 (2026-10-16)

### 说明

- 后台侧栏改为常驻紧凑结构：品牌 → 现有作者模块 → 主题切换/返回博客；导航选中态用浅色填充，触达区域 44px，不再使用文字符号图标。
- 移动端抽屉接管焦点与滚动锁：Esc 关闭并把焦点还给菜单按钮，路由切换自动收起，`aria-expanded`/`aria-controls` 完整。
- 登录页去掉仿 macOS 窗口装饰与网格背景，改为单一紧凑卡片；保留原有限制（用户名 3–20 位、密码 ≥8 位）、记住我、跳转校验与 busy 状态。
- 概览、文章、标签、访客、评论、相册、设置统一「标题 → 说明 → 内容区」层级：去掉全大写英文标签与装饰性序号/星形符号，按钮/输入框 8px、分组卡 12–16px，表格在窄屏内横向滚动而不挤压列、也不隐藏关键操作。
- 访客图表、热门页面、评论状态、相册上传与密码相册、设置分组（站点资料 / 后台与访客 / 评论与音乐 / 视觉主题）与字段语义保持不变；相册弹窗改用共享 Modal（层级、Esc、焦点回收与灯箱互不干扰）。
- 写作页保留 Markdown 编辑、撤销重做、工具栏、预览、草稿、封面、分类、标签与发布流程，只把外壳与控件收敛到同一套表面/圆角/触达标准。

### 验证

- 前端 30 文件 / 328 测试通过；类型检查、Lint（0 错误）与生产构建通过。
- 新增回归：侧栏路由/ Esc /焦点、编辑器草稿与发布、相册密码与灯箱、设置分组、公共页搜索/归档/关于/404 与 `ArticleList`。
- Chrome 实测：8 条公开路由 × 1440/1024/820/768/390px 无横向溢出、无小于 32px 的操作控件；登录 → 7 个后台入口标题与选中态、暗色切换、390px 抽屉 + 滚动锁、表格横向滚动、写作页填写均通过；无页面错误。API 为浏览器测试数据，未改动既有数据库或上传目录。

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/admin/AdminLayout.vue` | 紧凑侧栏、抽屉焦点/滚动、主题切换 |
| `frontend/src/views/admin/Dashboard.vue`、`Posts.vue`、`TagsManagement.vue`、`Visitors.vue`、`Comments.vue`、`Albums.vue`、`Settings.vue` | 统一层级与控制台样式；相册改用共享 Modal |
| `frontend/src/views/auth/Login.vue` | 去掉窗口装饰，收敛登录卡片 |
| `frontend/src/views/post/PostEdit.vue` | 编辑器外壳/工具栏/侧栏表面与圆角统一 |
| `frontend/src/views/__tests__/`、`views/post/__tests__/`、`views/gallery/__tests__/`、`views/admin/__tests__/`、`components/post/__tests__/` | 新增/扩展行为回归 |
| `README.md`、`frontend/README.md`、`frontend/CLAUDE.md` | 结构与验证记录 |

---

## v2.0.26 — 清透空间重设计（二）：公开阅读路径 (2026-10-16)

### 说明

- 新增 `ArticleList` 文章行组件（日期/分类 → 标题 → 摘要 → 标签/阅读时长，可选右侧缩略图，无封面不留空槽，缩略图加载失败自动移除），首页、搜索、标签共用同一套排版。
- 首页改为「真实站点配置的介绍区 + 单列文章列表 + 224px 作者侧栏」，侧栏在 ≤960px 收起；介绍区装饰图取首批成功加载文章中的第一张封面并在本次停留内保持不变，无封面或加载失败使用中性底色，不额外请求接口。
- 归档改成年月分组 + 文章行（保留全部翻页归档查询）；关于页改为作者资料主导；404 提供具名搜索与返回操作；页脚与返回顶部统一圆角、图标与 44px 触达，并遵循减少动效。
- 阅读页把标题/摘要/时间/阅读时长提到封面之前；正文使用系统无衬线、约 760px 行宽，侧边目录与移动端抽屉断点统一到 1020px；保留净化链路、代码复制/折叠、标题锚点、灯箱、相邻文章、匿名点赞与留言。阅读进度改为按帧合并并支持取消。
- 相册改用共享 Modal/ImagePreview：修复低于导航的遮罩层级、Esc、焦点回收与滚动锁；保留公开/密码相册与短期令牌流程，新增详情加载、失败重试与响应代际保护。

### 验证

- 新增 `ArticleList`、首页、公共页、相册与阅读页回归后，全量前端测试为 30 文件 / 328 测试通过；类型检查、Lint 与构建通过。
- Chrome 实测 1440/1024/820/768/390px：公开路由无横向溢出，封面/侧栏/目录按断点重排，灯箱高于导航且关闭后相册仍打开。

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/components/post/ArticleList.vue` | 新增：共享文章行 |
| `frontend/src/views/Home.vue`、`Archives.vue`、`About.vue`、`common/NotFound.vue`、`search/Search.vue`、`tag/TagDetail.vue` | 版式重做，复用文章行 |
| `frontend/src/views/post/PostDetail.vue`、`components/post/MobileTocSheet.vue` | 阅读层级、进度按帧合并、目录断点 |
| `frontend/src/views/gallery/Gallery.vue` | 接入共享浮层、失败重试与竞态保护 |
| `frontend/src/components/layout/Footer.vue`、`components/common/BackToTop.vue` | 统一紧凑控件与减少动效 |

---

## v2.0.26 — 清透空间重设计（一）：紧凑导航与音乐 (2026-10-16)

### 说明

- 采用已确认的灰白画布、系统无衬线字体与小圆角层级；正文实底，玻璃仅用于功能层，并提供透明度/高对比/减少动效降级。保留作者皮肤及主色，派生相应软底与 hover 色。
- 导航按品牌 → 搜索 → 图标文字链接 → 音乐/主题排列，桌面 52px、手机 50px，滚动不改高度、不隐藏；小屏保持 44px 工具触达，面板互斥并支持 Esc/外部点击/换页关闭。
- 背景音乐从右下悬浮条移入导航，唯一音频宿主不自动播放；提供播放/暂停、有效进度、音量、单曲循环、行内错误与重试。公开换页连续，进入后台暂停，替换/禁用音源阻止旧异步播放恢复。
- 弹窗、灯箱和目录使用统一主题浮层、顶部对话框键盘处理、焦点回收与滚动锁；灯箱重新打开时同步初始照片，无图片不锁页面。
- 本批只完成底座/导航/音乐，不把仍待重做的首页、公开阅读页及作者工作台计为完成。无后端/API/schema/依赖改动。

### 验证

- 前端 23 文件 / 267 测试通过；类型检查、Lint（0 错误，272 既有警告）和生产构建通过。
- Chrome 七档宽度、亮暗/减少动效、焦点与层级检查通过；生成 WAV 验证真实播放、公开换页连续、后台暂停与音源替换。API 为浏览器测试数据，未操作既有数据库或上传文件。

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/App.vue` | 常驻唯一音频宿主、稳定主题浮层与外观应用 |
| `frontend/src/styles/{variables,dark-mode,skins,base,glassmorphism,components,typography,a11y}.css` | 紧凑 token、系统字体、功能玻璃与可访问降级 |
| `frontend/src/stores/{music,site,theme}.ts` | 音频生命周期、主色推导、同步主题切换 |
| `frontend/src/components/layout/{Navbar,NavbarMusicControl,MusicPlayer}.vue` | 响应式导航与音乐面板/宿主拆分 |
| `frontend/src/composables/useDialog.ts` | 共享对话框栈、焦点与滚动管理 |
| `frontend/src/components/common/{Modal,ImagePreview,Toast}.vue`、`icons.ts`、`components/post/MobileTocSheet.vue` | 浮层接入、图标与目录适配 |
| `frontend/src/**/__tests__/` 相关测试 | 音频/导航/主题/浮层与回归用例 |
| `docs/superpowers/specs/2026-10-16-selfblog-ios-glass-redesign-design.md`、`docs/superpowers/plans/2026-10-16-compact-glass-*.md` | 已确认设计与三批实施计划 |
| `README.md`、`frontend/README.md`、`frontend/CLAUDE.md` | 已实现边界、结构与验证记录 |

---

## v2.0.26 — 修复单 JAR 模式下静态 favicon 的 401 (2026-10-16)

### 修复

- `index.html` 的默认图标兜底是 `/favicon.svg`，但 `SecurityConfig` 的静态资源放行名单只写了 `/favicon.ico`；在单 JAR（Spring 直接提供前端静态资源）模式下，浏览器请求默认 favicon 会落到 admin 兜底规则返回 401。已把 `/favicon.svg` 加入放行名单，并用打包后的 JAR 实测 `GET /favicon.svg → 200`

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/com/selfblog/config/security/SecurityConfig.java` | 静态资源放行名单新增 `/favicon.svg` |

---

## v2.0.26 — 视觉重设计（四）：全站圆角收尾 (2026-10-16)

批次一上调了圆角 token，但页面里仍有大量裸圆角值不跟随；本批把它们全部换成 token，让「大 R 角」真正覆盖全站。

### 说明

- **79 处裸圆角（22 个文件）按映射表替换**：3–5px → `--radius-xs`、7–9px → `--radius-sm`、10–14px → `--radius`、15–18px → `--radius-lg`、20px → `--radius-xl`；`50%`（圆形）、`999px`（胶囊）、`0`（直角）与 1–2px 微元素保持不动
- 复核扫描：排除 `var(--radius*)`、百分比、`inherit`、`0`、`1px`、`2px`、`999px` 后，全仓库不再有裸圆角值；`git diff` 抽查确认只改了 `border-radius` 数值
- **深色模式收尾**：设置页 `.alert` / `.success` 提示条的浅色硬编码改为 `--error-light` / `--success-light` + 语义文字色（`dark-mode.css` 已有暗色覆盖）

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/Home.vue`、`Archives.vue`、`post/PostDetail.vue`、`gallery/Gallery.vue`、`search/Search.vue`、`tag/TagDetail.vue`、`trending/TrendingPage.vue`、`user/Profile.vue`、`user/UserProfile.vue` | 裸圆角 → token |
| `frontend/src/views/admin/` 下 Albums / Comments / Dashboard / Posts / Reports / Settings / Statistics / TagsManagement / TopicsManagement / Users / Visitors / CircleManagement | 裸圆角 → token；Settings 另修深色提示条 |
| `frontend/src/styles/skeleton.css` | 裸圆角 → token |

---

## v2.0.26 — 视觉重设计（三）：文章页阅读体验 (2026-10-16)

把阅读路径上的交互补齐：代码块、目录、锚点、灯箱与阅读时长。

### 新增

- **代码块头部 + 复制**：`pre` 包装为 `.code-block`，头部显示语言标签与「复制」按钮（真实 `<button>`，写入剪贴板，约 1.5s 后复位）
- **长代码折叠**：超过 320px 的代码块默认折叠，底部渐隐，「展开代码 / 收起代码」切换
- **标题锚点**：每个 h1–h3 追加 `#` 锚点（悬停/键盘聚焦可见）；依赖既有的 `scroll-padding-top: 96px`，跳转不被浮动导航盖住
- **移动端目录抽屉**：≤860px 隐藏侧边目录，改为左下浮动胶囊 + 底部抽屉（复用同一份 toc 数据与高亮态）
- **正文图片灯箱**：点击正文图片打开 `ImagePreview`，多图左右切换、Esc 关闭
- **按正文估算阅读时长**：新增 `utils/reading.ts`，剔除代码块/图片后按「中日韩按字 + 西文按词」统计，替换原先按字符数除以 400 的粗算
- **最后更新提示**：`updateTime` 晚于 `createTime` 7 天以上时，meta 行展示「最后更新于 …」

### 修复

- **`ImagePreview.vue` 键盘监听 TDZ**：`watch(..., { immediate: true })` 在 `const handleKeydown` 声明前引用它，初始 `show=true` 时监听挂不上（Esc / 方向键失效）；改为函数声明并补测试

### 说明

- 标题高亮从「每次滚动遍历全部标题」改为 `IntersectionObserver`，仅在不支持的环境回退到扫描
- `MobileTocSheet` 由 `PostDetail.vue` 挂载（而非设计稿中的 App.vue）——目录数据本来就在 PostDetail，避免提升成全局状态

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/utils/reading.ts` | 新增：正文字数与阅读时长估算 |
| `frontend/src/composables/useMarkdownEnhance.ts` | 新增：DOM 后处理（代码块 / 锚点 / 图片收集） |
| `frontend/src/components/post/MobileTocSheet.vue` | 新增：移动端目录抽屉 |
| `frontend/src/views/post/PostDetail.vue` | 集成增强、灯箱、IO 高亮、时长与更新提示 |
| `frontend/src/components/common/ImagePreview.vue` | 修复键盘监听 TDZ |
| `frontend/src/utils/__tests__/reading.test.ts` | 新增：字数统计测试 |
| `frontend/src/composables/__tests__/useMarkdownEnhance.test.ts` | 新增：DOM 后处理测试 |
| `frontend/src/components/post/__tests__/MobileTocSheet.test.ts` | 新增：目录抽屉测试 |
| `frontend/src/components/common/__tests__/ImagePreview.test.ts` | 新增：键盘与滚动锁定测试 |

---

## v2.0.26 — 视觉重设计（二）：后台侧边栏 (2026-10-15)

把公开页顶栏的同一套语言（内置图标、胶囊选中、品牌资产）带进后台。

### 新增

- **后台侧栏图标化**：`⌂ ▤ ⌑ ◷ ▱ ▧ ⚙` 等文字符号全部换成内置 SVG 图标（概览 / 文章 / 标签 / 访客 / 评论 / 相册 / 设置），移动端抽屉的菜单与关闭按钮、页脚"返回博客"一并换掉
- **侧栏品牌位消费 `siteLogoUrl`**：有值显示上传的 Logo，为空回落内置图标——与公开页顶栏完全一致
- 侧栏选中项改为 `--radius-full` 胶囊，hover 未选中同样出胶囊浅底；`AdminLayout` 内的裸圆角值同步换成 token

### 说明

- 纯前端改动，不涉及接口与数据；`Settings.vue` 的 Logo 字段提示恢复为"公开页顶栏与后台侧边栏"（侧栏未接入时曾临时收窄）
- 侧栏导航仍是同一批路由，未新增页面

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/admin/AdminLayout.vue` | 图标替换、胶囊选中、品牌 logo、token 圆角 |
| `frontend/src/views/admin/Settings.vue` | Logo 提示恢复为双端描述 |
| `frontend/src/views/admin/__tests__/AdminLayout.test.ts` | 新增：品牌回落/上传、图标化、选中态测试 |

---

## v2.0.26 — 站点资源上传控件（前端）(2026-10-15)

承接「资源上传通道（后端）」：后台「网站设置」页新增四个品牌资源字段，支持上传与预览。

### 新增

- **`SiteAssetField.vue` 通用资源字段**：预览 + 手动 URL 输入 + 上传按钮；按 kind 限定 `accept` 与体积上限（图片 2MB、音乐 20MB），预检失败不发请求，错误行内展示
- **上传 API** `siteApi.uploadSiteAsset()`：multipart 提交到 `POST /api/admin/site-assets`，带上传进度回调

### 说明

- 上传成功即写入表单并同步 `siteStore`（顶栏 Logo、favicon 立即换新），点「保存设置」才通过 `PUT /api/admin/site-settings` 落库；手填 URL 只写表单、保存时一并生效
- 音乐字段预览显示文件名与时长；图片字段显示方形缩略图；远端 CDN 手填 URL 的能力保留
- 前端预检与后端校验规则一致，但服务端仍是权威（扩展名白名单 + 魔数 + 体积）

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/api/site.ts` | 新增 `SiteAssetKind` / `SiteAssetUpload` 类型与 `uploadSiteAsset()` |
| `frontend/src/components/admin/SiteAssetField.vue` | 新增：站点资源上传字段组件 |
| `frontend/src/views/admin/Settings.vue` | 四个字段接入 + 乐观更新 |
| `frontend/src/api/__tests__/site.test.ts` | 新增：上传 API 单测 |
| `frontend/src/components/admin/__tests__/SiteAssetField.test.ts` | 新增：字段组件单测 |
| `frontend/src/views/admin/__tests__/Settings.test.ts` | 新增：设置页集成测试 |

---

## v2.0.26 — 资源上传通道（后端）(2026-10-15)

### 新增

- **管理员站点资源上传** `POST /api/admin/site-assets`：仅管理员可用，multipart 参数为 `file` 与 `kind`（`logo` / `favicon` / `avatar` / `music`）；成功后返回随机公开地址 `/uploads/site-assets/<kind>/<uuid>.<ext>`
- 上传校验包含扩展名白名单与文件魔数；logo、favicon、avatar 每个文件最多 2 MB，music 最多 20 MB；接口限流为每分钟 30 次
- **SVG 安全净化**：移除危险元素（含 XML 命名空间前缀形式）、事件属性和脚本 URL
- 站点设置新增并默认提供 `faviconUrl`、`siteLogoUrl`；`site_setting` 是键值表，因此无需改表或数据库迁移

### 说明

- 上传通道不复用 `MediaService`：它会把所有图片重新编码为 `.jpg`，不适用于 `.ico`、`.svg` 或音频。`.ico` 同时接受 ICO 与 PNG 文件头，因为部分 favicon 导出工具会把 PNG 字节写入 `.ico` 文件
- `/uploads/**` 是公开地址，上传的 SVG 可被直接作为顶层文档打开，脚本可能以本站源执行；因此 SVG 净化是必要的安全边界，不能只依赖图片上下文的限制
- 后端接口与设置支持已完成；后台 `Settings.vue` 上传控件及前端 API 接入留待下一批

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/com/selfblog/common/enums/SiteAssetKind.java` | 新增：资产种类、扩展名白名单与分档大小上限 |
| `backend/src/test/java/com/selfblog/common/enums/SiteAssetKindTest.java` | 新增：资产种类、扩展名与大小规则测试 |
| `backend/src/main/java/com/selfblog/vo/site/SiteAssetVO.java` | 新增：上传结果与资源元信息 |
| `backend/src/main/java/com/selfblog/service/site/SiteAssetService.java` | 新增：站点资源上传服务接口 |
| `backend/src/main/java/com/selfblog/service/site/impl/SiteAssetServiceImpl.java` | 新增：校验、SVG 净化及随机路径落盘 |
| `backend/src/test/java/com/selfblog/service/site/impl/SiteAssetServiceImplTest.java` | 新增：上传校验、大小限制与 SVG 净化测试 |
| `backend/src/main/java/com/selfblog/controller/admin/AdminSiteAssetController.java` | 新增：管理员资源上传接口 |
| `backend/src/test/java/com/selfblog/controller/AdminSiteAssetControllerTest.java` | 新增：接口、权限及错误响应测试 |
| `backend/src/main/java/com/selfblog/config/web/RateLimitProperties.java` | 修改：新增资源上传每分钟 30 次限流配置 |
| `backend/src/main/java/com/selfblog/service/site/impl/SiteSettingsServiceImpl.java` | 修改：允许并默认提供 `faviconUrl`、`siteLogoUrl` |
| `backend/src/test/java/com/selfblog/service/site/impl/SiteSettingsServiceImplTest.java` | 新增：品牌设置默认值与 URL 校验测试 |
| `backend/README.md` | 修改：更新管理员 API 数量及限流说明 |

---

## v2.0.26 — 视觉重设计（一）：设计底座与公开页导航 (2026-10-15)

站点视觉重设计的第一批落地：先把圆角 token、图标系统、滚动状态与品牌资产这些**共享底座**做好，再让公开页顶栏成为第一个消费者。后端上传通道、文章页阅读体验、后台胶囊化在后续批次。

### 新增

- **内置 SVG 图标系统** `components/common/icons.ts` + `components/common/Icon.vue`：21 个 24×24 描边图标（`logo`/`search`/`sun`/`moon`/`menu`/`close`/`home`/`archive`/`image`/`info`/`settings`/`gauge`/`file-text`/`tag`/`users`/`message`/`music`/`play`/`pause`/`arrow-up`/`external`）。全部靠 `stroke="currentColor"` 取色，深浅两套主题自动适配。**不引入任何图标库依赖**，`dependencies` 保持 7 项不变
- **`composables/useScrollState.ts`**：rAF 节流的滚动状态，返回 `{ scrolled, direction }`。滚动是高频事件，直接读 `scrollY` 会让长页面掉帧，因此同一帧内最多处理一次
- **顶栏滚动三态**：静止 → 圆角胶囊实底；下滚过阈值 → 高度收紧、转半透明 + 毛玻璃（`backdrop-filter: blur(20px) saturate(1.4)`）、阴影加深；小屏下滚 → 顶栏收走、上滚立刻归位（桌面端不隐藏，避免鼠标用户找不到导航）
- **导航选中态胶囊化**：选中项为 `--radius-full` 的浅色胶囊，未选中只在 hover 出浅底。移动端面板此前完全没有选中指示，一并补上
- **可上传的品牌商标**：`siteLogoUrl` 有值时顶栏渲染 `<img>`，为空时回落内置 `Icon name="logo"`——即"设置里可自由上传图片"的前端半边
- **`faviconUrl` 运行时注入**：`stores/site.ts` 新增 `mimeForIcon()` 与 `applyFavicon()`，按扩展名推导 MIME（`.svg`→`image/svg+xml`、`.ico`→`image/x-icon`、其余 `image/png`）并同步到 `<head>`。注入节点带 `data-runtime` 标记以便幂等清理，**`index.html` 的静态 `/favicon.svg` 永不移除**，所以"清空设置"会自然回落到默认图标
- **`html { scroll-padding-top: 96px }`**：顶栏是 `fixed` 浮动胶囊，原生锚点跳转需要给它让位，否则 `#hash` 跳转后标题会被压在导航下面

### 变更

- **圆角整体上移一档**（`--radius-xs` 4→6、`sm` 4→8、`radius`/`md` 8→12、`lg` 12→16、`xl` 16→20、`2xl` 20→24、`3xl` 24→32）。`--radius-full` 保持 `9999px`：胶囊选中态与圆形头像共用它，同一个值同时覆盖"小方块变圆"和"胶囊"，不再新增 `--radius-pill` 之类的冗余 token
- 搜索框改为全圆角胶囊，主题切换/汉堡按钮改为正圆，品牌角标由硬编码 `9px` 改用 `--radius-sm`
- 顶栏模板重写。`Navbar.vue` 里 `.write-link`/`.admin-link`/`.login-link`/`.logout-link` 四组样式是**死代码**——模板从未渲染过对应元素（登录/注册入口早先已移除），随本次重写一并删除

### Bug 修复

- **移动端搜索按钮文字竖排折行**：`.mobile-search` 里输入框 `width: 100%` 且未设 `flex`，把"搜索"按钮挤到只剩一个字宽，两个字上下排成两行。已给按钮 `flex: 0 0 auto` + `white-space: nowrap`
- **移动端导航没有任何选中指示**：桌面端有 `.nav-links a.active`，移动端面板的链接连 `active-class` 都没配

### 说明

- **布局高度守恒**是本次的硬约束：`.app-main { padding-top: 80px }`（`App.vue`）以及文章页的 `scroll-margin-top: 105px`、`.article-aside { top: 105px }` 都是按顶栏高度写死的锚点。因此滚动收缩**只改视觉高度**（`.nav-shell` 的 `min-height` 58→48），顶栏占据的布局高度始终不变，正文位置不跳动
- **jsdom 测不到的东西靠浏览器验**：胶囊圆角、毛玻璃、收缩位移都无法在单测里断言。已用 Playwright 实测计算样式与截图确认（`getComputedStyle` 取到 `border-radius: 24px`、`backdrop-filter: blur(20px) saturate(1.4)`、移动端 `opacity: 0` 且 `top: -62px`），上面两个移动端 Bug 就是这样发现的
- **第一批完成时的状态**：`faviconUrl` / `siteLogoUrl` 当时只是前端消费端；后端 `SiteSettingsServiceImpl` 的 `ALLOWED_KEYS` 与 `defaults()` 尚未收录这两个键，所以后台还存不进这两个值；后端收录与上传接口属于后续批次（现已由上方后端批次完成）
- 圆角 token 只改了 scale 本身，**全站逐处裸圆角替换与后台侧边栏胶囊化留到后续批次**，避免一次提交同时动几十个文件而无法审查

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/styles/variables.css` | 圆角 scale 整体上移一档 |
| `frontend/src/styles/base.css` | 新增 `scroll-padding-top: 96px` |
| `frontend/src/components/common/icons.ts` | 新增：21 个描边图标注册表 |
| `frontend/src/components/common/Icon.vue` | 新增：图标渲染组件（`name` / `size`） |
| `frontend/src/components/common/__tests__/icons.test.ts` | 新增：注册表结构与安全性断言（65 例） |
| `frontend/src/composables/useScrollState.ts` | 新增：rAF 节流的滚动状态 |
| `frontend/src/composables/__tests__/useScrollState.test.ts` | 新增：7 例，含每帧合并与卸载后静默 |
| `frontend/src/stores/site.ts` | 新增 `faviconUrl`/`siteLogoUrl` 键、`mimeForIcon()`、`applyFavicon()` |
| `frontend/src/stores/__tests__/site.test.ts` | 新增：11 例，含幂等注入与静态兜底不被移除 |
| `frontend/src/components/layout/Navbar.vue` | 重写：滚动三态、图标、胶囊选中、可上传 logo；修移动端两个问题 |
| `frontend/src/components/layout/__tests__/Navbar.test.ts` | 新增：10 例，用真实 memory router 断言 active class |
| `AGENTS.md` | 修正 Node 引擎范围、`format:check` 基线失败、`noUncheckedIndexedAccess`、jsdom 测试约定 |
| `docs/superpowers/specs/2026-10-15-selfblog-ui-redesign-design.md` | 新增：本次重设计的完整设计稿 |
| `docs/superpowers/plans/2026-10-15-selfblog-ui-foundation-and-navbar.md` | 新增：第一批次的实施计划与执行修订 |

---

## v2.0.26 — SEO、订阅源与字体 (2026-10-15)

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

## v2.0.26 — 样式系统与路由修复 (2026-10-15)

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
