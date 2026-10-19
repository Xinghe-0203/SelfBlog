# 附录B：SelfBlog 源码导读地图 + 全栈术语表

> **适用版本**：SelfBlog v2.0.26。backend：Spring Boot 3.3.13 + Java 21 + MyBatis Plus 3.5.8 + Spring Security + jjwt 0.12.6 + Caffeine；frontend：Vue 3.5.44 + Vite 6 + TypeScript 6.0.3 + Pinia。仓库：https://github.com/Xinghe-0203/SelfBlog 。
>
> 本附录是整部教程的「地图页」。第一部分带你把两个工程的源码目录走一遍：每个包是干什么的、关键类有哪些、**什么时候你会来改这里**；再给出五条后端主题阅读路线与三条前端阅读路线，让你按「任务」而不是按「文件名字母序」读代码。第二部分是一本按主题分组、组内按字母排序的全栈术语表，每条术语都尽量落在 SelfBlog 的真实文件上，避免「背概念背成空中楼阁」。
>
> 两个使用建议：其一，读正文遇到陌生词时，先翻第二部分末尾的「按字母索引」，再回到对应条目与正文章节；其二，本附录的行数、类数、缓存名等数字均按当前源码实测，仓库继续演化后，请以 `pom.xml`、`package.json` 与实际目录为最高权威。

---

# 第一部分：源码导读地图

## 0. 全景：一个仓库，两个独立工程

SelfBlog 仓库没有根构建清单，也没有 npm workspace——`backend/` 与 `frontend/` 是**两个完全独立的工程**，所有命令都要进对应目录执行。它们之间只有一条正式通道：前端把请求发给 `/api/**`（开发期由 Vite 代理转发到后端 8825 端口，部署期由 `frontend/vercel.json` 生成的反向代理转发），后端统一以信封 `Result<T>{code,message,data,timestamp}` 应答。

| 工程 | 技术栈 | 构建工具 | 产物 | 源码根 |
|------|--------|----------|------|--------|
| backend | Spring Boot 3.3.13、Java 21、MyBatis Plus 3.5.8、Spring Security、jjwt 0.12.6、Caffeine | Maven（仓库自带 `mvnw` Wrapper） | `target/selfblog.jar` | `backend/src/main/java/com/selfblog/` |
| frontend | Vue 3.5.44、Vite 6、TypeScript 6.0.3、Pinia、Axios | npm + Vite | `dist/`（另有 Vercel 代理产物） | `frontend/src/` |

还有第三个容易被忽略的区域：`docs/`。教程正文在 `docs/tutorial/`（本附录所在处），架构决策记录在 `docs/decisions/`（ADR），各批次真实验证记录在 `docs/reviews/`。改代码要同步哪些文档，规则见根目录 `CLAUDE.md`。（历史规格与计划目录 `docs/superpowers/` 已于 2026-10-19 清理。）

后端源码根下的包结构一图流（括号内为实测文件数）：

```
com.selfblog
├── SelfBlogApplication.java        启动类（1）
├── annotation/                     自定义鉴权注解（2）
├── aspect/                         AOP 切面：鉴权、性能监控（2）
├── common/                         枚举 / 异常 / 统一响应（11）
├── config/                         全部配置类：security、web、db、cache 等（18）
├── controller/                     REST 控制器（40，含 admin/ 14 个）
├── dto/                            入参对象（33）
├── entity/                         数据库实体（28）
├── event/ + listener/              领域事件与监听器（4 + 1）
├── mapper/                         MyBatis Plus Mapper 接口（28）
├── service/                        业务层：六子包 + impl + seo/page（60+）
├── utils/                          工具类（10）
└── vo/                             出参对象（37）
```

## 1. 后端逐包导读

以下每包按固定三段式写：**职责**——这个包在架构里承担什么；**关键类**——类名加一句话；**什么时候你会来改这里**——把「读代码」变成「改代码」的触发条件。源码根一律省略前缀 `backend/src/main/java/com/selfblog/`。

### 1.1 启动类：SelfBlogApplication.java

**职责**：整个应用的组合注解起点。`@SpringBootApplication` 等价于三个注解的叠加：`@SpringBootConfiguration`（它本身是个配置类）、`@EnableAutoConfiguration`（开启自动配置）、`@ComponentScan`（从本包向下扫描所有组件）。它所在的位置 `com.selfblog` 决定了扫描根，所以任何新包都必须建在它之下，否则注解写得再对也不会被装配。

**关键类**：`SelfBlogApplication`——`main` 方法一行 `SpringApplication.run(...)`，没有多余逻辑；所有「启动时特别做的事」都不在这里，而在 `config/` 包的各个配置类里（这是刻意的：启动类只负责启动）。

**什么时候你会来改这里**：几乎永远不用。只有两种情况值得打开它——加 `@EnableXxx` 类的全局开关注解（如开启异步、调度），或调整扫描根。改完要确认 `mvnw test` 全绿，因为装配错误在启动期就会爆炸。

### 1.2 annotation/：自定义鉴权注解（2 个文件）

**职责**：声明式的权限标记。把「这个接口需要登录/需要管理员」从方法体里的 if 判断，提升为方法签名上的注解，让鉴权横切逻辑由切面统一处理。

**关键类**：
- `RequiresAuth`——标记方法需要已登录用户；
- `RequiresAdmin`——标记方法需要管理员角色。

**什么时候你会来改这里**：新增一个「非 `/api/admin/**` 路径但也要求管理员」的接口时。注意本仓库的惯例：管理员接口统一放 `controller/admin/` 走 URL 前缀 + `SecurityConfig` 的 `hasRole("admin")`，注解方案是补充手段而不是主手段——先看 `aspect/AuthAspect` 怎么消费这两个注解，再决定用哪条路。

### 1.3 aspect/：AOP 切面（2 个文件）

**职责**：把横切关注点（鉴权、性能监控）从业务代码里抽出来，织入到注解标记的连接点或全局切入点。

**关键类**：
- `AuthAspect`——拦截 `@RequiresAuth`/`@RequiresAdmin`，从 `SecurityUtils` 取当前用户做校验，不满足时抛 `BusinessException`；
- `PerformanceMonitoringAspect`——记录慢方法 `[PERF]` 日志。跑测试时日志里大量 `[PERF] ... error:` 是它记录的**正常负例断言输出**，不是失败。

**什么时候你会来改这里**：想给所有 Service 方法加统一计时、或给新注解加横切行为时。改切面前先想清楚切入点表达式的影响面——切面是全局放大器，一个 `execution(* com.selfblog..*(..))` 写宽了会让所有方法都被代理。

### 1.4 common/：枚举、异常与统一响应（11 个文件）

**职责**：全仓库的「公共语言」。枚举定义状态字面量，异常定义错误语义，`Result` 定义 HTTP 响应信封——三层共同保证「前后端对同一件事说同一种话」。

**关键类**：
- `enums/`（8 个）——文章状态、互动类型等业务字面量的唯一出处；
- `exception/BusinessException`——业务异常的唯一形态，构造参数 `(code, message)`，service 层抛、全局处理器接；
- `GlobalExceptionHandler`——`@RestControllerAdvice` 全局兜底：把业务 code 映射为对应 HTTP 状态，未知异常兜底 500 并记日志；
- `result/Result`——信封本体 `{code, message, data, timestamp}`，分页数据额外含 `records/total/size/current/pages`。前端 `api/index.ts` 的拦截器读的就是这个信封。

**什么时候你会来改这里**：新增业务状态（先进 `enums/`）、新增一种错误语义（抛 `BusinessException`，通常不用动处理器）、或需要给信封加全局字段时。**反模式警告**：不要在 controller 里手写 `Result.error(...)`——错误语义统一走异常通道，成功才用 `Result.ok(data)`。

### 1.5 config/：全部配置类（18 个文件）

**职责**：应用所有「可调行为」的声明处。这个包是理解 SelfBlog 行为的钥匙：安全规则、限流阈值、数据库切换、缓存 TTL 全在这里，且全部可用环境变量覆盖。

**关键类（按子包分组）**：

根级 4 个：
- `AsyncConfig`——线程池与异步执行器；
- `DotenvConfig`——解析 `backend/.env`。**注意它用 Java Properties 语法，不是 shell dotenv：值不要加引号**；查找顺序为工作目录 → 用户主目录，文件值会成为 JVM property，优先级高于系统环境变量；
- `EnvValidationConfig`——启动期校验关键配置（如 `JWT_SECRET` 强度），不达标直接拒绝启动（fail-fast）；
- `LoggingConfig`——日志行为声明。

security/ 5 个：
- `SecurityConfig`——安全中枢：`STATELESS` 会话策略；CSRF 关闭（代码注释里有完整论证：JWT 走 `Authorization` 头，跨站页面拿不到该头，伪造请求带不上凭证）；CORS 拒绝通配符结尾的 origin；`X-Frame-Options: DENY`、`Referrer-Policy: strict-origin-when-cross-origin`、`Permissions-Policy` 关闭摄像头/定位/麦克风；`/api/admin/**` 要求 `hasRole("admin")`；401/403 返回 JSON 入口；`BCryptPasswordEncoder` 强度 12（`bcrypt.strength` 可配）；`app.single-author-mode`（默认 true）参与写入类操作的兜底授权；
- `JwtAuthenticationFilter`——**只解析不拒绝**：请求头里有合法 JWT 就解析出用户并构造认证信息放进安全上下文；没有头或 token 无效则放行为匿名，拒绝权留给授权层。这个职责切分是读懂整条认证链的前提；
- `SecurityProperties`——安全相关属性的绑定类；
- `JwtSchedulerConfig`——令牌黑名单的周期清理调度；
- `XssFilter`——请求级 XSS 过滤。

web/ 4 个：
- `WebMvcConfig`——`configurePathMatch` 里 `addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class))` 给**所有** `@RestController` 自动加 `/api` 前缀。因此源码里看不到任何 `/api` 字面量，新写的 `@RestController` 也**不要**自己带 `/api`；
- `RateLimitInterceptor`——按**路径前缀**匹配 `RateLimitProperties` 的阈值表，Caffeine 计数（`expireAfterWrite(1, MINUTES)`、`maximumSize(50000)`），最长前缀匹配，键为 `ip:path`，超限返回 429 JSON。单实例内存实现，代码注释明言多实例需换 Redis；
- `RateLimitProperties`——前缀 → 阈值的配置表；
- `HttpRequestLoggingInterceptor`——请求日志。

db/ 4 个：
- `DatabaseConfig`——**三库切换的枢纽**：`@ConditionalOnProperty(name = "DB_TYPE")`，SQLite（默认，`jdbc:sqlite:campus_blog.db`，Hikari 池大小 1，自动执行 `schema-sqlite.sql`）/ MySQL（`DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD`）/ 不设置则走测试 H2；
- `MybatisPlusConfig`——注册 `VendorDatabaseIdProvider`，让 Mapper XML 支持 `databaseId="mysql"/"sqlite"` 双语句；
- `MyMetaObjectHandler`——审计字段自动填充；
- `DataInitializer`——用 `ADMIN_PASSWORD` 引导初始管理员。

cache/ 1 个：
- `CaffeineCacheConfig`——用 `SimpleCacheManager` 注册 **8 个具名缓存**，各自独立 TTL：`hotTagsCache`(10m)、`categoryCache`(30m)、`userCache`(10m)、`trendingCache`(5m)、`statsCache`(15m)、`statusCache`(2m)、`siteSettingsCache`(10m)、`visitorSummaryCache`(30s)。测试 profile 关闭缓存。

**什么时候你会来改这里**：这是你会最常回来的包。新增公开接口 → `SecurityConfig` 放行；新增限流前缀 → `RateLimitProperties` 补阈值；新增具名缓存 → `CaffeineCacheConfig` 登记 TTL；改数据库策略 → `DatabaseConfig`。改任何一个配置类前，先想清楚它在哪个 profile 生效、哪个环境变量能覆盖它。

### 1.6 controller/：REST 控制器（40 个文件，九个分组）

**职责**：HTTP 入口层。本仓库纪律：controller 只做「接参数、调 service、包信封」三件事，业务逻辑一律下沉 service。所有 `@RestController` 由 `WebMvcConfig` 自动获得 `/api` 前缀，源码路径里看不到它。

**关键类（按分组）**：
- `admin/`（14 个）——后台管理接口。受双重保护：URL 层 `/api/admin/**` 要 `hasRole("admin")`，方法层 `@PreAuthorize` 逐个把关；`AdminPostController` 是文章 CRUD 的入口；
- `auth/`（2 个）——`AuthController`（登录、登出、刷新）与 `PasswordController`（改密码；注册接口一律抛 403「此博客不开放访客注册」）；
- `post/`（5 个）——前台文章读取（列表、详情、归档等）；
- `site/`（7 个）——面向访客的站点级接口：`GuestCommentController`、`AnonymousLikeController`、`PhotoGalleryController`、`PublicPageController`、`SeoController`（**刻意是 `@Controller` 而非 `@RestController`**，以此逃过 `/api` 前缀，在根路径提供 `/feed.xml`、`/atom.xml`、`/sitemap*.xml`、`/robots.txt`）、`SiteSettingsController` 等；
- `social/`（4 个）、`content/`（5 个）——历史社区与内容域接口，公开入口大多已下线，改动前先确认路径是否仍被路由引用；
- `circle/`（1 个）、`user/`（1 个）——历史域的残余入口；
- `SpaController`——SPA 路由的服务端回应者：公开历史路由（`/`、`/about`、`/archives`、`/gallery`、`/post/{id}`、`/tag/{id}`、`/pages/{slug}`）返回由 `service/site/seo/` 组装的**可直接阅读的 HTML**（Vue 挂载后替换 `#app` 内的 `.seo-reading`），状态码真实（缺失 404、`/index.html` 308、模板缺失 503）；后台命名空间与历史账户路由返回启动壳并强制 noindex。它不再统一回退空壳 `index.html`。

**什么时候你会来改这里**：每加一个对外接口都会经过这里。三条铁律：新 `@RestController` 不要自带 `/api` 前缀；新增公开（免登录）接口必须去 `SecurityConfig` 显式放行，并检查是否落在限流前缀内；管理员接口进 `admin/` 而不是自己开路径。

### 1.7 dto/：入参对象（33 个文件）

**职责**：定义 HTTP 请求的形状。按 controller 的域分包：`auth/`(7)、`post/`(6)、`site/`(5)、`social/`(6)、`user/`(3)、`circle/`(3)、`content/`(3)。它与 `vo/` 的分界是方向：dto 进、vo 出，Entity 永远不直接暴露给前端。

**什么时候你会来改这里**：接口需要新字段时。惯例是校验注解（`@NotBlank`、`@Size`）写在 dto 上，service 里再做语义校验——两层校验各管格式与业务。
### 1.8 entity/：数据库实体（28 个文件）

**职责**：表行在 Java 世界的投影。28 个实体全部带 `@TableName` 声明表名，绝大多数带 `@TableLogic private Integer isDeleted` 软删除标记（配合 `application.yml` 的 `logic-delete-field` 全局配置，MyBatis Plus 自动在生成的 SQL 里追加 `is_deleted=0`——**但手写 SQL/XML 语句必须自己带上**，这是全仓库最容易漏的约定）。

**关键类（两个代表）**：
- `BlogPost`——文章主体：状态、计数列（views/like/comment/collect）、审计字段齐全，是「门面/查询/交互」三分架构围绕的核心实体；
- `AnonymousPostLike`——匿名点赞：`(post_id, visitor_hash)` 关联 + `is_deleted`，**没有独立计数列**——计数 = `post.likeCount` + 活跃匿名点赞数聚合，三态切换直接操作软删位。

表分两组：历史社区表（`sys_user`、`blog_post`、`blog_comment`、`blog_tag`、`blog_post_tag`、`blog_like`、`blog_collect`、`blog_share`、`blog_follow`、`blog_notification`、`blog_trending`、`blog_draft`、`blog_draft_tag`、`blog_report`、`blog_circle_*`、`blog_media`、`blog_post_media`、`blog_topic`、`blog_message`）与个人博客新增表（`site_setting`、`visitor_event`、`anonymous_post_like`、`guest_comment`、`photo_album`、`album_photo`）。

**什么时候你会来改这里**：新增业务字段时。改动要过**三库初始化策略**：SQLite 的 `schema-sqlite.sql`（`CREATE TABLE IF NOT EXISTS`，不迁移既有文件）、MySQL 的 `schema-fresh-install.sql`（只给全新空库）+ `migration-selfblog.sql`（已有库唯一通道）、H2 的测试 `schema.sql`——三处都要同步，缺一处 CI 就会替你发现。

### 1.9 event/ + listener/：领域事件（4 + 1 个文件）

**职责**：把「某事发生了」从「某事发生后要做什么」里解耦。事件是纯数据载体，监听器决定反应，发布方（service）不关心谁来听。

**关键类**：
- `CollectCreatedEvent` / `CommentCreatedEvent` / `FollowCreatedEvent` / `LikeCreatedEvent`——四个互动领域事件；
- `NotificationEventListener`——统一消费者：把互动事件转化为站内通知落库。加新监听器不需要动事件类，也不需要动发布方。

**什么时候你会来改这里**：想让某个互动触发新副作用（Webhook、统计、邮件）时——写一个新监听器订阅已有事件即可；若事件本身缺字段，才需要改 event 类并同步发布方。

### 1.10 mapper/：数据访问接口（28 个文件）

**职责**：MyBatis Plus 的 `BaseMapper<T>` 子接口层。28 个接口绝大多数**零方法**——通用 CRUD 由基类免费提供；少数带自定义方法，SQL 写在 `resources` 下的 Mapper XML。

**关键机制**：`MybatisPlusConfig` 注册了 `VendorDatabaseIdProvider`，XML 语句可用 `databaseId="mysql"` / `"sqlite"` 提供方言双版本；**H2 没有映射 id，只会走无 databaseId 的兜底语句**——所以 MySQL 专有语法（如 `ON DUPLICATE KEY UPDATE`）绝不能写进共享语句。

**什么时候你会来改这里**：需要复杂查询（多表关联、方言特有语法、统计聚合）时。纪律：单表操作交给 BaseMapper/QueryWrapper，进 XML 之前先问一句「QueryWrapper 真的表达不了吗」；写进 XML 的语句，SQLite 与 MySQL 各测一遍。

### 1.11 service/：业务层（全仓库的心脏，60+ 文件）

**职责**：所有业务语义的家。目录形态是「六子包 + impl + 两个 SEO 支撑子包」：`auth/`、`circle/`、`content/`、`post/`、`site/`、`social/` 各放接口，`impl/` 放实现，`site/seo/` 与 `site/page/` 放可读 HTML 与站点地图的支撑类。

**最重要的一套架构约定——「门面 / 查询 / 交互」三分**（Post 与 Circle 采用同一套拆分）：

| 角色 | 例子 | 职责 | 事务注解 |
|------|------|------|----------|
| 门面 Facade | `service/post/BlogPostService(Impl)` | CRUD、归属校验、跨服务编排；把查询/交互方法**一行委托**出去并重新标注事务注解 | 按操作读写标注 |
| 查询 Query | `PostQueryService(Impl)` | 列表/搜索/详情等只读逻辑 | `@Transactional(readOnly = true)`，`@Cacheable` 也在这里 |
| 交互 Interaction | `PostInteractionService(Impl)` | 计数与状态副作用（views/like/comment/collect） | `@Transactional(rollbackFor = Exception.class)` |

注意两个细节：门面实现与查询实现**都**继承同一个实体的 `ServiceImpl<XxxMapper, Entity>`（所以两者都能用基类的泛型方法）；新增功能必须放对位置——查询进 Query、副作用进 Interaction，**不要在门面里复制逻辑**。

**关键类（SEO 支撑，`service/site/seo/` 8 个 + `site/page/` 1 个）**：
- `ReadingPageService`——可读 HTML 的总装车间：把文章/页面数据交给模板渲染；
- `ReadingTemplate`——页面骨架模板（head/meta/JSON-LD/正文容器）；
- `ReadingContent` 与 `ReadingHtmlRenderer`——正文加工与最终 HTML 拼装（`SafeMarkdown` 白名单渲染发生在此链路上）；
- `SafeMarkdown`——Markdown→HTML 的**白名单安全渲染**，防存储型 XSS 的出库闸门；
- `PageCatalog`（在 `page/`）——公开页面目录与路由合法性判定；
- `SeoModels`——SEO 元数据模型（title/description/JSON-LD 结构）；
- `SitemapService`——站点地图：index + 每片 1000 条的分片；
- `SiteOrigin`——站点 origin 解析器，优先级：后台「网站设置 → 网站域名」(`siteBaseUrl`) → 环境变量 `SITE_BASE_URL` → 直连请求地址。配置后伪造 `Host`/`X-Forwarded-*` 不再影响 canonical/订阅/站点地图。

**什么时候你会来改这里**：任何业务行为的变化最终都落在这个包。判断落点的口诀：**只读进 Query、副作用进 Interaction、编排进 Facade、页面外观进 seo/**。改 `@Cacheable` 方法时同步想清楚对应的 `@CacheEvict` 在哪——缓存失效与缓存使用必须成对出现。

### 1.12 utils/：工具类（10 个文件）

**职责**：无业务语义的可复用能力。全仓库的「低层库」，被所有上层引用，自己只依赖框架。

**关键类**：
- `JwtUtils`——令牌中枢：jjwt 0.12.6 API（`Keys.hmacShaKeyFor` 构建密钥、`Jwts.parser().verifyWith(...).build().parseSignedClaims(...)` 校验）；`credentialVersionFor` 用 HmacSHA256 对域分离串 `selfblog:credential-version:v1\nuserId\npasswordHash` 计算凭证版本（只存 HMAC，不暴露 BCrypt 哈希）；内置 `ConcurrentHashMap` 令牌黑名单与 `CopyOnWriteArraySet` 设备令牌集；
- `SecurityUtils`——从安全上下文取人：`getCurrentUserId()`（未登录抛 401「请先登录」）、`getCurrentUserIdOrNull()`（游客返回 null，如详情页判断是否作者本人）、`isCurrentUserAdmin()`；principal 兼容 `UserContext` 或裸 `Long`；
- `HtmlSanitizer`——HTML 白名单消毒器，访客留言入库的第一道闸（`sanitizePlainText`）；
- `VisitorHashUtils`——访客标识加盐哈希（`hash(visitorId, hashSecret)`，secret 取 `JWT_SECRET`，缺省 `local-visitor-hash`）；
- `FineGrainedLockManager`——按资源键的细粒度锁管理；
- `StringMaskUtils`、`LogUtils`、`TimeUtils`——脱敏、日志格式、时间处理；
- `UserContext` / `UserConverter`——用户上下文载体与 entity↔vo 转换。

**什么时候你会来改这里**：想写新工具方法时，先来这里找——大概率已有（或应扩展现有类而不是新建平行类）。工具类纪律：无状态、不依赖业务包、必须有单测。

### 1.13 vo/：出参对象（37 个文件）

**职责**：HTTP 响应的形状。按域分包：`post/`(12)、`site/`(8)、`content/`(5)、`social/`(5)、`user/`(4)、`circle/`(3)。与 dto 的分界同前：dto 进、vo 出，entity 永远不直接出 API。

**什么时候你会来改这里**：前端需要新展示字段时。顺序是「vo 加字段 → service 填充 → 前端类型（`frontend/src/types/`）同步」——三处一体，只改 vo 不改前端类型是接口腐化的起点。

## 2. 五条后端主题阅读路线

按「任务」组织代码阅读：每条路线给出发顺序、每步看什么、为什么按这个顺序。文件路径省略前缀 `backend/src/main/java/com/selfblog/`。

### 路线一：认证链路（读懂「谁在请求」）

1. **`controller/auth/AuthController`**——入口长什么样：登录/登出/刷新三个端点，入参 dto、出参信封。先看接口面再下钻，知道终点才能理解中途每站。
2. **`utils/JwtUtils`**——令牌的生与死：`Keys.hmacShaKeyFor` 用 `JWT_SECRET` 构建 HMAC 密钥；签发时嵌入 userId、过期时间与 `credentialVersion`（`credentialVersionFor` 的域分离串设计让改密即全设备失效）；校验走 `Jwts.parser().verifyWith(...).build().parseSignedClaims(...)`；黑名单与设备集都在这里。**为什么第二步看它**：token 是整条链路的通货，先懂通货再懂流通。
3. **`config/security/JwtAuthenticationFilter`**——过滤器的一行职责：**只解析不拒绝**。有合法 token 就解析出用户、构造认证信息放进 `SecurityContextHolder`；没有/无效则放行为匿名。拒绝决策不在这里做——这行纪律是理解下一步的前提。
4. **`config/security/SecurityConfig`**——规则的总装配：`authorizeHttpRequests` 定义路径级授权（`/api/admin/**` 要 `hasRole("admin")`，公开接口逐条放行）；`STATELESS` 会话策略；CSRF 关闭的注释论证（JWT 走 `Authorization` 头，跨站页面拿不到该头）；CORS 拒绝通配符结尾 origin；`X-Frame-Options` 等安全头；401/403 的 JSON 入口（未认证/未授权返回结构化错误而非默认白页）；BCrypt 强度 12；`app.single-author-mode` 兜底。
5. **`utils/SecurityUtils`**——业务代码怎么取人：`getCurrentUserId()` 抛 401、`getCurrentUserIdOrNull()` 返回 null、`isCurrentUserAdmin()`；principal 兼容两种形态。**为什么第四步之后**：身份进入上下文之后，业务层才有「人」可取——这站是认证链路的终点、所有业务路线的起点。
6. **方法级授权**——`@PreAuthorize` 与 `aspect/AuthAspect`（消费 `@RequiresAuth`/`@RequiresAdmin` 注解），以及 single-author-mode 兜底如何确保「即使 URL 层漏配，写入类操作仍要求 admin」。

自测题：为什么 `JwtAuthenticationFilter` 不直接拒绝无效 token？（答：拒绝权统一留给授权层，过滤器只建立身份，职责单一便于测试与复用。）为什么改密码能让旧 token 失效？（答：token 载荷里的 credentialVersion 与库中 `credentialVersionFor(userId)` 不再一致。）

### 路线二：发布文章链路（读懂「三分架构」）

1. **`controller/admin/AdminPostController`**——入口：文章 CRUD 端点群，注意每个方法都薄到只有「接参、调 service、包信封」。
2. **`service/post/BlogPostService(Impl)`**——门面：CRUD、归属校验（「这篇文章是你的吗」）、跨服务编排；查询/交互方法在这里是**一行委托**并重标事务注解。看懂「一行委托」的形态，就懂了门面模式的本仓库用法。
3. **`service/post/PostQueryService(Impl)`**——查询实现：`@Transactional(readOnly = true)` + `@Cacheable`；列表、搜索、详情的只读逻辑。**为什么第三步**：先看读侧，因为缓存、分页、信封六件套都在读侧最集中。
4. **`service/post/PostInteractionService(Impl)`**——交互实现：views/like/comment/collect 计数副作用，`@Transactional(rollbackFor = Exception.class)`。对照第 3 步，体会「读与写分开标注事务」的理由。
5. **`mapper/BlogPostMapper` + `entity/BlogPost`**——落到底：BaseMapper 提供了什么、自定义 XML 有什么、实体字段与表列的映射（`@TableLogic isDeleted` 在此可见）。

自测题：为什么门面与查询实现都继承 `ServiceImpl<BlogPostMapper, BlogPost>`？（答：两者都需要基类泛型方法，门面额外做编排；继承同一基类避免工具方法重复。）新增「文章导出」功能该放哪？（答：只读导出进 `PostQueryService`，门面加委托方法。）

### 路线三：访客互动链路（读懂「消毒、哈希与幂等」）

1. **`controller/site/GuestCommentController`**——实测仅 31 行：留言创建与管理员查看两个端点，`@RequestMapping("/guest-comments")`。**先看它有多薄**：controller 的克制是后端能保持整洁的第一原因。
2. **`service/social/GuestCommentServiceImpl`**（约 149 行，全仓库业务密度的典范）——四个必须看清的细节：
   - **先消毒后校验**：`htmlSanitizer.sanitizePlainText(...)` 打头，再校验昵称 ≤80、内容 ≤2000——消毒先行，恶意载荷活不到校验层；
   - **visitorId 正则** `[A-Za-z0-9_-]{8,80}`：格式不合格直接拒，绝不进入哈希环节；
   - **加盐哈希只存 hash**：`VisitorHashUtils.hash(visitorId, hashSecret)`，库中无明文 visitorId；
   - **`selectBatchIds` 防 N+1**：管理员评论列表先查分页、再一次批量取全部关联用户；
   - 错误语义：`ensureCommentsEnabled` 403、`requirePublishedPost` 404、`normalizeSize` 把分页钳到上限 100。
3. **`controller/site/AnonymousLikeController` + `service/social/AnonymousLikeServiceImpl`**——幂等的教科书：`toggle` 是 `synchronized` 的**三态切换**（无记录 → insert liked=true；`is_deleted=0` → 置 1、liked=false；`=1` → 置 0、liked=true）；计数 = `post.likeCount` + 活跃匿名点赞数聚合。**为什么把它放在最后**：前两步看懂了「身份如何匿名化」，这一步才轮到「匿名身份如何并发安全」。

自测题：为什么 visitorId 要先过正则再哈希？（答：正则约束格式，哈希保证不可逆；顺序颠倒会把垃圾输入引入哈希环节。）并发双击匿名点赞会插两条记录吗？（答：不会——`synchronized` 把「检查→行动」收进同一把实例锁。）

### 路线四：SEO 与页面（读懂「同一内容，两种出口」）

1. **`controller/SpaController`**——SPA 路由的服务端回应者：公开历史路由（`/`、`/about`、`/archives`、`/gallery`、`/post/{id}`、`/tag/{id}`、`/pages/{slug}`）返回**可直接阅读的 HTML**（Vue 挂载后替换 `#app` 内的 `.seo-reading`）；状态码真实：缺失 404、`/index.html` 308、模板缺失 503；后台命名空间与历史账户路由返回启动壳并强制 noindex。它**不再**统一回退空壳 index.html——这个「曾经空壳、现在直出」的演变是理解本仓库 SEO 策略的钥匙。
2. **`service/site/seo/ReadingPageService`**——总装车间：数据如何变成完整 HTML（模板 → 内容 → 渲染器 → `SafeMarkdown` 白名单）。**为什么第二步**：SpaController 只是把活儿派出去，真正的组装在 service。
3. **`controller/site/SeoController`**——刻意的 `@Controller`（非 `@RestController`）：以此逃过 `WebMvcConfig` 只给 `@RestController` 加的 `/api` 前缀，在**根路径**提供 `/feed.xml`、`/atom.xml`、`/sitemap*.xml`、`/robots.txt`；站点地图是 index + 每片 1000 条的分片；robots 按实际 `adminPath` 生成（后台地址不泄漏给非必要爬虫规则）。
4. **`service/site/seo/SiteOrigin`**——origin 决定 canonical/feed/sitemap 里的绝对 URL；解析优先级 `siteBaseUrl` → `SITE_BASE_URL` → 请求地址，配置后伪造 `Host`/`X-Forwarded-*` 无效。

自测题：为什么 SeoController 不用 `@RestController`？（答：`addPathPrefix` 的谓词是 `forAnnotation(RestController.class)`，换 `@Controller` 才能落在根路径。）模板缺失为什么是 503 而不是 404？（答：404 表示「资源不存在」，503 表示「服务端暂时组装不了」——语义必须区分。）

### 路线五：基础设施（读懂「请求进屋前经过的五道门」）

1. **`config/web/WebMvcConfig`**——`configurePathMatch` 的 `addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class))`：全仓库 URL 的 `/api` 来源。**第一道门**：理解了它，源码里「找不到 /api」的困惑即消失。
2. **`config/db/DatabaseConfig`**——数据源选择：`@ConditionalOnProperty(name = "DB_TYPE")`；SQLite 默认（`jdbc:sqlite:campus_blog.db`、Hikari 池 1、自动执行 `schema-sqlite.sql`）；MySQL 走连接五件套；缺省走测试 H2。
3. **`config/cache/CaffeineCacheConfig`**——8 个具名缓存与各自 TTL（`hotTagsCache` 10m / `categoryCache` 30m / `userCache` 10m / `trendingCache` 5m / `statsCache` 15m / `statusCache` 2m / `siteSettingsCache` 10m / `visitorSummaryCache` 30s）。
4. **`config/web/RateLimitInterceptor`**——固定窗口限流：Caffeine 计数（1 分钟窗、`maximumSize(50000)`）、最长前缀匹配 `RateLimitProperties` 阈值、键 `ip:path`、超限 429 JSON；单实例实现，多实例需 Redis（注释明言）。
5. **`common/GlobalExceptionHandler`**——异常到 HTTP 的翻译官：`@RestControllerAdvice` 捕获 `BusinessException`（业务 code → HTTP 状态）、校验异常（400）、兜底 500。
6. **`common/result/Result`**——终点站：信封结构 `{code, message, data, timestamp}` 与分页六件套（`records/total/size/current/pages`）。

自测题：一个 GET 请求从进 Tomcat 到返回 JSON，按顺序经过哪些基础设施组件？（答：安全过滤器链 → 限流拦截器 → 路由前缀匹配 → controller → service（缓存/事务）→ mapper → GlobalExceptionHandler（若异常）→ Result 信封。）
## 3. 前端导读：frontend/src/

前端是 Vue 3.5.44 + Vite 6 + TypeScript 6.0.3 + Pinia 的标准 SPA。`src/` 下十个一级目录，各有明确分工。与后端同理，每目录给「职责 + 什么时候改」。

### 3.1 api/（15 个文件）

**职责**：后端通信的唯一出口。`index.ts` 是地基：创建 Axios 实例（`baseURL: '/api'`、`timeout: 15000`）、请求拦截器注入 `Authorization`/`X-XSRF-TOKEN`/`X-Visitor-Id`、响应拦截器**直接返回后端信封**（`api.get<T>()` 解析出的就是 `{code, message, data}`，调用方读 `res.data` 而不是 `res.data.data`）、`isRefreshing + failedQueue + processQueue` 的刷新排队重放、`RETRYABLE_CODES` 重试与 `error.userMessage` 文案、`handleAuthError()` 清理 Pinia/localStorage 并跳回 `adminRoot`。其余 14 个文件（`admin/`、`gallery/`、`media/`、`message/`、`notification/`、`pages/`、`post/`、`public-interactions/`、`share/`、`site/`、`tag/`、`trending/`、`user/`）按业务域封装端点函数。

**什么时候改**：每接一个新后端接口都在这里加封装函数——组件**不直接** import axios。注意 `baseURL` 硬编码 `/api`，`VITE_API_BASE_URL` 实际没有任何地方读取，跨域部署靠 Vite 代理与 `vercel.json` 重写解决。

### 3.2 components/（5 组：admin/common/layout/pages/post）

**职责**：可复用组件。`layout/` 是全局骨架（导航、页脚、`.app-main` 容器）；`admin/` 后台专用件；`post/` 文章展示件（含 `PostDetail` 的锚点布局，依赖 `html { scroll-padding-top }`/`scroll-margin-top`，改动前先确认锚点没被推挤）；`pages/` 自定义页面渲染件；`common/` 通用小组件。

**什么时候改**：两处以上视图需要同一段 UI 时才提升到这里；只服务一个视图的代码留在 `views/` 本地。

### 3.3 composables/（8 个）

**职责**：组合式函数——跨组件的逻辑复用层。`useConfirm`/`useDialog`（对话框；`useConfirm()` 返回的 `ConfirmDialog` 必须渲染进模板，否则确认框不出现）、`useMarkdownEnhance`（marked 增强，`main.ts` 里 `marked.setOptions({gfm:true, breaks:true})` 与之配套）、`usePageConfig`、`usePageSeo`、`useSeoMeta`（统一 document.meta）、`useScrollReveal`（滚动进入动画）、`useScrollState`。

**什么时候改**：一段带响应式状态/生命周期的逻辑被第二个组件需要时。与 utils 的分界：composable 内部用 `ref/computed/onUnmounted`，utils 是纯函数。

### 3.4 router/（index、guards、helpers + modules/ 六个模块）

**职责**：路由装配。`modules/` 下六个模块（home/post/discover/user/auth/admin）组装路由表；`helpers.ts` 封装 `lazyLoad`（`component: () => import(...)` 懒加载 + `ErrorFallback` 兜底）；`guards.ts` 是全局守卫；`index.ts` 创建 router 实例。**核心机制**：后台路由在运行时按服务端下发的 `adminPath` **动态注册**（`modules/admin.ts` 的 `createAdminRoutes`），不在首次导航前装好就会 404——这就是启动链路里 `installAdminRoutes` 必须先于 `use(router)` 的原因。

**什么时候改**：新增页面视图、调整公开路由、或改守卫规则时。铁律：组件里需要登录跳转统一 `router.push('/login')`，不要写死后台路径；`views/auth/` 下只剩重定向（`/login` → `adminRoot`）。

### 3.5 stores/（5 个）

**职责**：Pinia 全局状态。`site.ts`（站点设置、`adminPath`/`adminRoot`、皮肤与 favicon）；`theme.ts`（`localStorage['edu_theme']`、`.dark-mode` 类、`matchMedia` 监听系统主题）；`user.ts`（**store id 是 `'auth'`，导出别名 `useUserStore`**；token 经 `utils/crypto` 编解码存取；`validateToken` 只解 JWT 不请求网络）；`music.ts`（唯一音频宿主的状态）；`pages.ts`（**只保存已发布页面配置，草稿绝不进公开 store**）。另有启动期的特殊角色：SPA 在没有后端时也能靠 `site.ts` 的默认值正常渲染（`loadPublicSettings` 的 fetch 失败被静默吞掉），所以只跑 dev server 就能验收布局。

**什么时候改**：跨视图共享的新状态出现时。纪律：服务端可配置的东西先进 `site.ts`；本地偏好进 `theme.ts` 这类 localStorage 型 store。

### 3.6 styles/（14 个 css 文件）

**职责**：设计系统。`main.css` 只是 `@import` 入口；token 在 `variables.css`（颜色、`--radius-*` 圆角、`--gray-*` 灰度梯度）；暗色覆盖在 `dark-mode.css`（整体反转灰度梯度：`--gray-900` 由 `#111111` 变 `#EBEBEB`——**所以不要拿 `--gray-*` 当「深色表面」**，需要永远深色用语义 token：代码块 `--code-bg/--code-text/--code-border`、高对比按钮 `--contrast-bg/--contrast-bg-hover/--contrast-bg-active/--contrast-fg`）；皮肤在 `skins.css`（`#selfblog[data-skin=…]`，`ocean` 默认）。

**什么时候改**：调视觉时。三不原则：不写裸圆角（用 `--radius-*`）、不拿 `--gray-*` 当深色面、不动 `.app-main` 的 `padding-top` 与锚点相关属性除非通盘确认。

### 3.7 types/（11 个文件）

**职责**：后端契约的 TypeScript 投影（entity/vo 的前端镜像）与共享类型。**什么时候改**：后端 vo/dto 变化时同步——这是「三处一体」原则的第三处（vo → service → 前端 types）。

### 3.8 utils/（8 个文件）

**职责**：纯函数工具。`crypto`（token 的编解码存取——注意它是编码混淆不是加密）、`visitor`（访客 ID 生成与持久化）、`pageCatalog`、`reading`、`navigationFit`、`toast`、`logger`、`index`。

**什么时候改**：出现第二个使用者之前不新建；与 composables 的分界见 3.3。

### 3.9 views/（9 组）与 test/

**职责**：`views/` 按路由组织页面（admin/auth/common/gallery/pages/post/search/tag/user）。历史社区页（`views/user/*`）仍在仓库且路由注册，但已无公开入口，改动前先确认路径是否仍被引用；`Register.vue`、`circle/*` 等死代码已于 2026-10-17 删除。`test/` 下 `setup.ts` 是全局测试夹具：只桩了 `matchMedia` 与 `$router/$route`，**没有自动卸载**——挂载会注册全局监听器的组件，测试里要自己 `enableAutoUnmount(afterEach)`。

**什么时候改**：新增页面 = views 新文件 + router 模块注册 + （公开页）types/api 层配套。

## 4. 三条前端阅读路线

### 路线六：启动链路（`src/main.ts`，顺序是承重的）

1. **顶部副作用**——`marked.setOptions({ gfm: true, breaks: true })`：全局 Markdown 渲染基调在此定下，`useMarkdownEnhance` 依赖它。
2. **`loadPublicSettings()`**——拉公开站点设置填充 `site.ts`（失败静默吞掉，保证无后端也能渲染）。
3. **`installAdminRoutes(siteStore.adminPath)`**——按服务端下发的后台地址**动态注册后台路由**。必须在导航前完成，否则后台 404。
4. **`useUserStore().restoreFromStorage()`**——从 localStorage 恢复登录态。必须在 `use(router)` 之前，否则首个 `beforeEach` 会把已登录作者弹回登录页。
5. **`app.use(router)`** ——路由就绪。
6. **`mount('#app')`** ——挂载。
7. **`void usePagesStore(pinia).loadManifest()`**——挂载**之后**才异步拉公开页面配置摘要：它是可选的，失败不得阻塞启动。整条 bootstrap 包在 try/catch 里，异常时兜底仍要 mount。

为什么按此序读：这七步里每一对的先后都是 bug 教训换来的——先设置后路由（adminPath 才有值）、先恢复态后守卫（首跳不被误踢）、先挂载后可选增强（失败不阻塞）。读懂启动顺序，就读懂了「前端为什么不能随意调换初始化代码」。

### 路线七：请求链路（组件 → api 层 → 拦截器 → 信封）

1. **组件层**——调用 `api/` 域文件里的封装函数，拿到的直接是信封对象：`const res = await getPost(id); if (res.code === 0) use(res.data)`。
2. **api/ 域文件**——端点语义封装（URL、参数、类型标注），保持组件里不出现裸路径字符串。
3. **`api/index.ts` 请求拦截器**——注入三件套：`Authorization`（登录 token）、`X-XSRF-TOKEN`（32 字节 hex，双提交 CSRF 防线）、`X-Visitor-Id`（匿名身份）；日志里的 token 一律先做指纹脱敏（首尾各 20 字符 btoa）。
4. **响应拦截器**——正常路径直接解包信封返回；`401` 场景进入刷新协议：`isRefreshing` 标志 + `failedQueue` 队列 + `processQueue` 重放——并发多个请求只触发一次刷新，其余排队等新 token；刷新失败则 `handleAuthError()` 清理状态跳 `adminRoot`。可重试错误按 `RETRYABLE_CODES` 判定（`ERR_CANCELED` 等排除）。
5. **错误语义**——非 0 code 不抛异常而是正常返回信封，由调用方按业务处理；网络层错误带 `error.userMessage` 文案。

为什么按此序读：先看组件的「假象」（以为在拿 AxiosResponse），再看拦截器（实际返回信封），落差即本仓库最著名的契约——`res.data` vs `res.data.data` 的混淆全部源于此。

### 路线八：路由链路（modules → guards → 动态 admin）

1. **`router/modules/`**——六个模块按域拆分路由表；所有组件经 `helpers.ts` 的 `lazyLoad` 懒加载（失败有 `ErrorFallback`）。
2. **`router/guards.ts`**——全局守卫：`requiresAuth`/`requiresAdmin` 元信息不满足时跳 `siteStore.adminRoot` 并带 `redirect` 查询参数（登录后回跳）。守卫判断依赖第 4 步已恢复的登录态。
3. **动态 admin 路由**——`modules/admin.ts` 的 `createAdminRoutes(adminPath)` 在启动时被 `installAdminRoutes` 调用：后台路由的**路径本身**来自服务端设置，前端按需注册——这使后台地址可配置且不硬编码。

为什么按此序读：modules 是「静态面」，guards 是「规则面」，动态注册是「运行时面」——三层叠加才是完整的导航图；只看 modules 会完全漏掉后台路由从哪来。

## 5. 「改功能 → 动哪些文件」对照表

| 你要做的改动 | 后端文件 | 前端/配套 | 注意事项 |
|---|---|---|---|
| 加新站点设置键 | `SiteSettingsServiceImpl` 登记默认值+校验（**零 schema 迁移**，KV 表） | `stores/site.ts` + 设置页 | 记得配对 `siteSettingsCache` 的失效 |
| 新增公开接口 | controller 新方法 → `SecurityConfig` 显式放行 | `api/` 封装 | 检查 `RateLimitProperties` 前缀覆盖，不在则补阈值 |
| 新增管理员接口 | `controller/admin/` 新类/方法 | `api/admin/` + 后台视图 | 双重保护自动获得；测试须 `@MockBean JwtUtils` + `@WithMockUser(roles="admin")` |
| 新增实体/表 | `entity/` + `mapper/` + **三库 schema**（`schema-sqlite.sql`、`schema-fresh-install.sql`+`migration-selfblog.sql`、测试 `schema.sql`） | `types/` | 手写 SQL 别忘 `is_deleted=0` |
| 文章新增字段 | `BlogPost` + 三库 + dto + vo + Query/Interaction service | `types/` + 视图 | 三处一体：vo→service→types 同步 |
| 新增缓存 | `CaffeineCacheConfig` 登记具名缓存+TTL | — | `@Cacheable` 与 `@CacheEvict` 必须成对出现 |
| 新增限流前缀 | `RateLimitProperties` 补阈值 | — | 键为 `ip:path`，窗口 1 分钟 |
| 新增事件副作用 | `event/` 新事件类 + 发布方 + `listener/` 新监听器 | — | 不改事件类即可加监听者 |
| 改后台地址 | 无需动后端安全（只认 `/api/admin/**`） | 站点设置 `adminPath`（`normalizeAdminPath` 约束）+ 启动时动态注册 | 保留词拒绝；robots 按实际值生成 |
| 改站点域名/origin | 无需动代码 | 后台「网站设置 → 网站域名」(`siteBaseUrl`) | `SiteOrigin` 自动接管 canonical/feed/sitemap，伪造头失效 |
| 新增公开页面 | `PageCatalog` + `PublicPageController` + `SpaController` 路由 + seo/ 组装 | `views/pages/` + `router/modules/` | 草稿不进公开 store；缺失 404、模板缺失 503 |
| 调整视觉/皮肤 | — | `variables.css`（token）/ `skins.css`（`data-skin`）/ `dark-mode.css` | 不写裸圆角；`--gray-*` 不是深色面 |
| 新增组合式函数 | — | `composables/` + 组件 + 测试 | 全局监听要在 `onUnmounted` 清理，测试配 `enableAutoUnmount` |
| 新增依赖 | `pom.xml`（留心 `frontend-maven-plugin` 与 skip 开关） | `package.json`（`npm ci` 后跑 type-check 防幽灵依赖） | 版本钉死，文档版本号以 manifest 为准 |
| 方言特有 SQL | Mapper XML `databaseId="mysql"/"sqlite"` 双版本 | — | H2 只走无 databaseId 兜底语句，别把专有语法写进共享语句 |
| 全新 SEO 出口 | `SeoController`（保持 `@Controller` 逃 `/api`） | — | URL 集中在根路径；robots/sitemap 由 `SitemapService` 分片 |

---

# 第二部分：全栈术语表

用法说明：术语按**主题分组**（组内按字母/拼音排序），每条给出定义与「在 SelfBlog 中」的实例位置——概念只有落到文件上才真正属于你。条目互相引用时用「→ 见 xxx」。想按字母快速定位，翻本部分末尾的「按字母索引」。

---

## 6. 配置文件导读

| 文件 | 关键内容 |
|------|----------|
| `backend/src/main/resources/application.yml` | logic-delete-field、mybatis-plus 配置、app.single-author-mode |
| `backend/src/test/resources/application.yml` | 测试 profile：H2 数据源、缓存关闭 |
| `backend/.env.example` | JWT_SECRET(≥32)/ADMIN_PASSWORD(≥12)/ADMIN_PATH 模板 |
| `frontend/vite.config.ts` | server.proxy：/api、/uploads → VITE_API_TARGET(默认 8825) |
| `frontend/tsconfig.json` | strict、noUncheckedIndexedAccess、paths 别名 |
| `frontend/.prettierrc` | endOfLine:"auto"（跨平台稳定的历史教训） |
| `.github/workflows/ci.yml` | 四 job：backend/frontend/mysql/docker |
| `frontend/vercel.json` | framework:null + build 脚本生成代理 |

---

## 组一：Java 语言

**Annotation Processing（注解处理）**：编译期读取注解并生成/修改代码的机制，与运行期反射读取注解是两条不同通道。Lombok 是最常见的注解处理器。
在 SelfBlog 中：`pom.xml` 引入 Lombok（约 line 101/282/306 三处），`@Data`、`@Slf4j` 等在编译期生成 getter/setter 与日志字段——所以实体类源码里看不到这些方法，IDE 需装 Lombok 插件。

**Auto-boxing / Unboxing（自动装箱/拆箱）**：基本类型与包装类型自动互转。代价是装箱产生对象、拆箱可能 NPE。在 SelfBlog 中：实体计数列用 `Integer likeCount` 而非 `int`，因为数据库列可能为 NULL——`int` 会在拆箱时抛空指针。

**Class Loading（类加载）**：JVM 把 `.class` 装入方法区并链接初始化的过程，按需（首次主动引用）触发，双亲委派保证核心类不被篡改。在 SelfBlog 中：Spring Boot 的 `LaunchedURLClassLoader` 打包后从 `selfblog.jar` 内嵌 jar 里加载类，这就是 fat jar 能直接 `java -jar` 的原因。

**Enum（枚举）**：类型安全的常量集合，每个实例单例、可携带字段与方法。在 SelfBlog 中：`common/enums/` 下 8 个枚举定义文章状态、互动类型等业务字面量，service 与 mapper 统一引用，避免魔法数字散落。

**Exception Hierarchy（异常体系）**：`Throwable` 分 `Error`（不接）与 `Exception`；后者分受检（必须声明）与非受检 `RuntimeException`（业务异常的主流选择）。在 SelfBlog 中：`common/exception/BusinessException extends RuntimeException`——不受检，service 层随手抛、`GlobalExceptionHandler` 统一接，避免层层 throws 污染签名。

**Generics（泛型）**：编译期类型参数化，擦除后运行期无类型信息（Type Erasure）。在 SelfBlog 中：`Result<T>` 信封、`BaseMapper<T>`、`ServiceImpl<XxxMapper, Entity>` 全靠泛型把「类型正确」从运行时挪到编译期。

**Immutable（不可变对象）**：创建后状态不可改，天然线程安全，可放心共享。在 SelfBlog 中：jjwt 的 `SecretKey` 构建一次全局复用；事件类（`LikeCreatedEvent` 等）按不可变风格设计，字段构造后不再变更。

**Interface Default Method（接口默认方法）**：Java 8 起接口可带方法体，让接口演进不破坏实现类。在 SelfBlog 中：MyBatis Plus 的 `BaseMapper` 与 Spring Data 风格接口大量依赖 default method 提供开箱方法。

**Lambda & Functional Interface（Lambda 与函数式接口）**：`@FunctionalInterface` 单抽象方法接口可用 lambda 实例化，`Function/Supplier/Predicate/Consumer` 是四大内置。在 SelfBlog 中：Stream 流水线里的方法引用（如排序比较器、`map` 转换）贯穿 service 查询实现；`@Transactional` 底层事务模板同样以回调形式传递逻辑。

**Lombok**：注解处理器，编译期生成样板代码（getter/setter/构造器/builder/logger），不是运行时反射。在 SelfBlog 中：所有 dto/vo/entity 几乎都用 `@Data`；日志统一 `@Slf4j` 生成 `log` 字段，替代手工 `LoggerFactory.getLogger`。

**Record（记录类）**：Java 16+ 的不可变数据载体，一行声明字段、equals/hashCode/toString 全生成。在 SelfBlog 中：适合替换「只读出参」场景——`service/site/seo/` 的 `SeoModels` 里的 SEO 元数据对（title/description/JSON-LD 字符串）即按 record/紧凑载体风格组织。

**Reflection（反射）**：运行期检查与操作类、方法、字段的元编程能力，是框架「按名字装配」的根基，但有性能与封装破坏代价。在 SelfBlog 中：Spring 依注解（`@Service`、`@RequestMapping`）反射发现 Bean 与路由；MyBatis Plus 反射读取实体字段生成 SQL 列名——你写的 `@TableName("blog_post")` 就是给反射看的说明书。

**Sealed Class（封闭类）**：Java 17+ 用 `permits` 限定继承树，编译器可据此做穷尽检查。在 SelfBlog 中：异常体系可用 sealed + pattern matching 表达「业务错误只有这几种」，当前仓库未使用，属可选演进。

**Stream API（流）**：声明式集合处理（filter/map/reduce/collect），惰性求值、一次性消费。在 SelfBlog 中：`GuestCommentServiceImpl` 批量查询后用 stream 组装 vo 列表；`SitemapService` 把文章列表流式转成 XML 条目。

**String Handling（字符串处理）**：不可变 `String`、可变 `StringBuilder`（单线程拼接首选）。在 SelfBlog 中：`ReadingHtmlRenderer` 拼可读 HTML 时用 StringBuilder；`utils/StringMaskUtils` 做 token 指纹脱敏（首尾各 20 字符 btoa）。

**Switch Expression（switch 表达式）**：Java 14+ 的箭头语法 switch，带返回值与穷尽性检查。在 SelfBlog 中：`DatabaseConfig` 的方言分支、`GlobalExceptionHandler` 的异常映射这类「一一对应」逻辑是它的理想场景。

**Text Block（文本块）**：Java 15+ 的 `"""` 多行字符串，保留缩进控制。在 SelfBlog 中：`JwtUtils` 的域分离串 `selfblog:credential-version:v1\nuserId\npasswordHash`、schema 片段这类常量是 text block 的典型用武之地。

**Var / Type Inference（局部类型推断）**：Java 10+ 的 `var`，仅局部变量，静态类型不变。在 SelfBlog 中：泛型流水的中间变量（`var page = postMapper.selectPage(...)`）可提升可读性，但字段与公共 API 签名禁用。

## 组二：JVM 与并发

**Atomic Classes（原子类）**：`AtomicInteger/AtomicLong/AtomicReference` 基于 CAS 提供无锁原子操作。在 SelfBlog 中：`AnonymousLikeServiceImpl` 的计数与 `JwtUtils` 黑名单容量统计可用原子类承载——实际仓库选择了 `ConcurrentHashMap`/`CopyOnWriteArraySet`，因为「容器级原子性」已足够。

**Busy Waiting / Spin（自旋等待）**：循环检查条件而不让出 CPU，短临界区可减少上下文切换，长等待纯烧 CPU。在 SelfBlog 中：不使用——同步点（如 `synchronized toggle`）都交给 JVM 阻塞策略，自旋只见于 JVM 内部实现。

**CAS（Compare-And-Swap，比较并交换）**：CPU 原子指令，`expected==actual` 才写入，是无锁数据结构的地基；失败重试即「乐观并发」。在 SelfBlog 中：`ConcurrentHashMap`（令牌黑名单）内部就是 CAS + 链表/红黑树；你的代码只享受结果，不必手写。

**Context Switch（上下文切换）**：CPU 从一个线程切到另一个要保存/恢复寄存器，微秒级开销，高并发系统的主要隐性税。在 SelfBlog 中：`AsyncConfig` 线程池的核心/最大线程数就是「并行度 vs 切换成本」的权衡旋钮。

**CopyOnWrite（写时复制）**：读不加锁、写时复制整个底层数组，读多写少场景的廉价并发容器。在 SelfBlog 中：`JwtUtils` 的 `userDeviceTokens` 用 `CopyOnWriteArraySet`——每个用户的设备令牌集极少写、每次请求都可能读。

**Deadlock（死锁）**：多线程互相持锁等待，环状依赖导致永久阻塞。预防靠固定加锁顺序或超时。在 SelfBlog 中：`FineGrainedLockManager` 按资源键加锁时依赖「同一顺序获取」约定；单方法 `synchronized`（如 `toggle`）天然无死锁面。

**Executor Framework（执行器框架）**：`ExecutorService` 把任务提交与执行策略解耦，`ThreadPoolExecutor` 参数（核心数、队列、拒绝策略）决定系统行为。在 SelfBlog 中：`config/AsyncConfig` 声明应用线程池，`@Async` 方法与事件监听共享该池，避免默认 ForkJoinPool 的不可控。

**Fairness（公平锁）**：锁是否按等待顺序授予。公平锁无饥饿但吞吐低，`ReentrantLock(true)` 可选。在 SelfBlog 中：未使用——留言/点赞的锁粒度小（单条切换），吞吐优先。

**Happens-Before（先行发生原则）**：JMM 的可见性契约：unlock 先于后续 lock、`volatile` 写先于读、线程启动先于其内动作。理解它才不会「明明改了值另一个线程看不见」。在 SelfBlog 中：`RateLimitInterceptor` 的 Caffeine 计数器跨请求可见，正是 Caffeine 内部 happens-before 链的功劳。

**Immutable Publication（不可变发布）**：安全发布的最简方式——对象不可变则任何发布方式（哪怕没同步）都安全。在 SelfBlog 中：`SecretKey`、`RateLimitProperties` 等启动期构建后只读的 Bean 都按此模式，无需 volatile。

**JIT（Just-In-Time 编译）**：热点字节码运行期编译为本地码，分层编译（C1→C2）+ 方法内联是性能来源。在 SelfBlog 中：启动后几分钟内接口变快就是 JIT 热身；压测要预热，不能拿第一笔请求计时。

**JMM（Java Memory Model，Java 内存模型）**：定义多线程下读写共享内存的合法行为：可见性、有序性、`volatile`/`synchronized`/`final` 的语义。在 SelfBlog 中：`synchronized toggle` 的正确性，建立在「进入监视器读到的值不早于上次释放前写入」这条 JMM 保证上。

**JUC（java.util.concurrent）**：Java 并发工具包总称：容器（`ConcurrentHashMap`）、同步器（`CountDownLatch`、`Semaphore`）、执行器、原子类。在 SelfBlog 中：`JwtUtils` 的黑名单 `ConcurrentHashMap`、设备集 `CopyOnWriteArraySet`、事件派发的 `ThreadPoolExecutor` 全部来自 JUC——业务代码几乎没有手写 `synchronized` 之外的锁。

**Memory Leak（内存泄漏）**：对象不再使用却仍被引用（静态集合、监听器、ThreadLocal 未清理），最终 OOM。在 SelfBlog 中：`CaffeineCacheConfig` 的每个缓存都设 `expireAfterWrite`/容量上限，就是为了防止「缓存当存储用」式的慢性泄漏。

**Monitor / Intrinsic Lock（监视器锁）**：每个对象自带一把锁与等待集，`synchronized` 即使用它；同时是 JMM 可见性的边界。在 SelfBlog 中：`AnonymousLikeServiceImpl.toggle` 的 `synchronized` 方法就是拿实例监视器——单实例下让三态切换（无→insert / 0→1 / 1→0）串行化，保证幂等。

**Optimistic vs Pessimistic Locking（乐观锁/悲观锁）**：乐观假设冲突少、提交时校验（版本号/CAS）；悲观先锁再操作（`SELECT FOR UPDATE`）。在 SelfBlog 中：MyBatis Plus 的 `@Version` 乐观锁插件对实体可用；当前计数更新走「数据库原子 UPDATE（likeCount = likeCount + 1）」，属于无显式锁的原子路径；`FineGrainedLockManager` 则是应用层悲观锁的示范。

**Race Condition（竞态条件）**：正确性依赖执行时序的缺陷，典型如「检查再行动」（check-then-act）不原子。在 SelfBlog 中：匿名点赞若不用 `synchronized`，「检查记录是否存在→插入」就是教科书竞态——并发双击会插两条；`toggle` 的三态切换把检查与行动收进同一把锁。

**ThreadLocal（线程局部变量）**：每线程独立副本，常用于上下文传递（如用户身份、事务资源），用完必须 `remove()` 防泄漏。在 SelfBlog 中：Spring 的 `RequestContextHolder`/`SecurityContextHolder` 底层就是 ThreadLocal，`SecurityUtils.getCurrentUserIdOrNull()` 才能在任意 service 层无参取人。

**Thread Pool Sizing（线程池调优）**：CPU 密集 ≈ 核数，IO 密集可放大数倍；队列要有界，拒绝策略要明确。在 SelfBlog 中：`AsyncConfig` 的池参数按「通知派发是 IO 轻负载」设定小池+有界队列；SQLite 场景下写入并发本就被 Hikari 池大小 1 串行化，池再大也无谓。

**Thread Safety（线程安全）**：类在并发调用下仍保持正确性。手段按代价递增：无状态 → 不可变 → 封闭 → 锁。在 SelfBlog 中：`VisitorHashUtils`（纯函数）与 `HtmlSanitizer`（无状态）天然安全；`AnonymousLikeServiceImpl` 选择「实例级 synchronized」这种最简单但吞吐最弱的方式——单实例博客流量下，正确性优先于扩展性是对的。

**Volatile（volatile 关键字）**：保证可见性与禁止指令重排，但不保证复合操作原子。适合状态标志，不适合计数。在 SelfBlog 中：登录失败计数、开关型标志可用 `volatile boolean`；真正的计数交给数据库原子 UPDATE 或 `AtomicLong`。

**Wait/Notify（等待通知机制）**：`Object.wait()/notifyAll()` 构建线程间协作，生产者-消费者是其原型。现代替代是 `Condition` 与 `BlockingQueue`。在 SelfBlog 中：未直接使用——事件派发用线程池消费，不需要手工等待/通知。

**Work Stealing（工作窃取）**：空闲线程从他人队列尾部取任务，ForkJoinPool 的核心思想，适合分治。在 SelfBlog 中：默认 `@Async` 未指定执行器时会落到 ForkJoinPool.commonPool——这正是 `AsyncConfig` 要显式定义线程池的原因：业务调度不该挤占框架公共池。

## 组三：Spring 生态

**@Autowired / Resource（依赖注入注解）**：`@Autowired` 按类型注入，多个候选时配 `@Qualifier`；`@Resource` 按名注入。在 SelfBlog 中：service 里注入 Mapper、controller 里注入 service 全用构造器注入（推荐形态），字段注入已避开。

**AOP（Aspect-Oriented Programming，面向切面编程）**：把横切关注点（日志、鉴权、事务）从业务逻辑抽离，以切面织入。Spring AOP 基于动态代理，只拦截 Spring Bean 的 public 方法调用。在 SelfBlog 中：`aspect/AuthAspect` 拦 `@RequiresAuth`/`@RequiresAdmin` 做鉴权、`PerformanceMonitoringAspect` 全局记录慢方法 `[PERF]` 日志；`@Transactional` 本身也是 AOP 织入的事务切面。

**ApplicationContext（应用上下文）**：Spring IoC 容器的高级形态，除装配外还提供事件发布、国际化、资源加载。在 SelfBlog 中：启动即创建，`SpringApplication.run` 返回的就是它；事件机制（`event/` 四个事件 + `NotificationEventListener`）由它派发。

**Aspect（切面）**：`@Aspect` 类 = 切点（哪里织入）+ 通知（织入什么）。在 SelfBlog 中：`PerformanceMonitoringAspect` 用环绕通知包住 service 方法计时。

**Bean Lifecycle（Bean 生命周期）**：实例化 → 属性填充 → Aware 回调 → `@PostConstruct` → 使用 → `@PreDestroy`。在 SelfBlog 中：`DotenvConfig` 在早期把 `.env` 装入 JVM property，后续所有 `@Value` 才能读到；`SecurityConfig` 拉起的 `JwtAuthenticationFilter` 必须拿到 `JwtUtils` Bean——切片测试里 `@MockBean JwtUtils` 就是补这个环节。

**Boot Starter（启动器）**：「依赖即功能」的打包单位，引 starter = 引依赖 + 自动配置。在 SelfBlog 中：`spring-boot-starter-web/security/validation/actuator` 等在 `pom.xml` 一字排开，引哪个哪个生效。

**CGLIB Proxy（CGLIB 代理）**：字节码生成子类实现代理，可代理无接口类，不能代理 final 类/方法。Spring 默认对无接口 Bean 用 CGLIB。在 SelfBlog 中：service 实现类（如 `GuestCommentServiceImpl`）若无接口也照样被 `@Transactional` 代理——就是 CGLIB 在背后生成子类拦截。

**Circular Reference（循环依赖）**：A 依赖 B、B 依赖 A。Spring 用三级缓存解单例 setter 循环，构造器循环无解直接报错。在 SelfBlog 中：门面模式（`BlogPostService` 委托 `PostQueryService/PostInteractionService`）是单向依赖，刻意避免循环——这也是分层清晰的隐藏收益。

**Classpath Scanning（类路径扫描）**：`@ComponentScan` 从启动类包根扫描带 stereotype 注解的类注册为 Bean。在 SelfBlog 中：启动类位于 `com.selfblog` 包根，所有子包组件自动被发现——新包建在包根之外就扫描不到。

**Component Scan（组件扫描）**：同上，`@Component` 家族（`@Service`/`@Repository`/`@Controller`/`@Configuration`）都会被扫。在 SelfBlog 中：`mapper/` 下的接口由 `@MapperScan` 单独扫描，不走组件扫描通道。

**Conditional Beans（条件装配）**：`@Conditional` 家族按运行时条件决定 Bean 是否创建（类存在、属性匹配、profile 激活）。在 SelfBlog 中：`DatabaseConfig` 的数据源 Bean 带 `@ConditionalOnProperty(name = "DB_TYPE")`——属性在，SQLite/MySQL 数据源才创建；属性缺省走测试 H2 默认链路。

**Configuration Properties（属性绑定）**：`@ConfigurationProperties` 把 `application.yml` 前缀批量绑定到强类型对象，支持校验与 IDE 提示。在 SelfBlog 中：`RateLimitProperties`（前缀→阈值表）、`SecurityProperties`（安全参数）、`app.single-author-mode`、`bcrypt.strength` 都走绑定，环境变量可覆盖（`APP_SINGLE_AUTHOR_MODE`）。

**Constructor Injection（构造器注入）**：依赖经构造器传入，可 final、易测试、暴露依赖数量。在 SelfBlog 中：全仓库统一构造器注入——看一个类要什么依赖，看构造器签名即可，`GuestCommentServiceImpl` 的构造器就是它的「依赖清单」。

**Dependency Injection（DI，依赖注入）**：对象不自己 new 依赖，由容器注入——控制反转的具体实现。在 SelfBlog 中：controller 注 service、service 注 mapper、`JwtAuthenticationFilter` 注 `JwtUtils`，全部由容器接线。

**Environment Abstraction（环境抽象）**：`Environment` 统一 property source（JVM 属性 > 环境变量 > yml > 默认值）。在 SelfBlog 中：`DotenvConfig` 把 `.env` 提升为 JVM property 抢占优先级，因此「.env 值优先于系统环境变量」。

**IoC（Inversion of Control，控制反转）**：对象创建与依赖装配的控制权从业务代码反转给容器，DI 是其主流实现。在 SelfBlog 中：你从未在业务代码里 `new GuestCommentServiceImpl()`——它由容器创建、注入它需要的 `HtmlSanitizer`/`VisitorHashUtils`，测试时才手工 new 或用 `@MockBean`。

**Jackson（JSON 序列化库）**：Spring Boot 默认 HTTP 消息转换器，注解控制字段命名与忽略。在 SelfBlog 中：vo 上的 `@JsonProperty`/`@JsonIgnore` 控制响应形状，`LocalDateTime` 的格式化由全局配置统一。

**JDBC Template（JDBC 模板）**：Spring 对裸 JDBC 的薄封装，消灭样板代码，保留 SQL 控制权。在 SelfBlog 中：绝大多数查询走 MyBatis Plus，`JdbcTemplate` 留给偶发脚本式查询与 schema 初始化（`DataInitializer` 引导管理员）。

**Profile（多环境切面）**：`@Profile("dev")`/`spring.profiles.active` 让 Bean/配置按环境生效。在 SelfBlog 中：`application-dev.yml`/`application-prod.yml` 分环境；测试 profile 关闭缓存；Knife4j 在 prod 关闭。

**Property Source（属性源）**：配置来源的有序叠加，Spring Boot 预置了 17+ 层优先级。在 SelfBlog 中：`.env`（经 DotenvConfig）→ JVM property → 环境变量 → `application-{profile}.yml` 的链条，解释了为什么同名的系统变量会被 `.env` 覆盖。

**Proxy Pattern（代理模式）**：为对象提供代理以控制访问；Spring AOP 与 `@Transactional` 的实现本质。在 SelfBlog 中：注入的 service 实际是代理对象——`@Cacheable` 命中时代理直接返回缓存，方法体根本不执行。**自注自调不经过代理**，事务与缓存会静默失效，这是同类方法内部调用的大坑。

**Repository（仓库模式）**：在领域与数据映射之间收拢持久化逻辑。在 SelfBlog 中：MyBatis Plus 的 `BaseMapper<T>` + `ServiceImpl` 承担仓库角色，service 直接持有 Mapper——轻量级仓库形态。

**Scheduled Tasks（定时任务）**：`@Scheduled` + `@EnableScheduling` 的方法级调度。在 SelfBlog 中：`config/security/JwtSchedulerConfig` 周期清理令牌黑名单过期项，防 `ConcurrentHashMap` 无界增长。

**Slice Testing（切片测试）**：只装配某一层的测试注解（`@WebMvcTest`/`@DataJpaTest`），比全容器快一个量级。在 SelfBlog 中：管理员控制器用 `@WebMvcTest`，必须 `@MockBean JwtUtils`（否则 SecurityConfig 拉起的 jwtAuthenticationFilter 缺 Bean 启动失败）+ `@WithMockUser(roles="admin")`。

**Spring Boot Auto-configuration（自动配置）**：`@EnableAutoConfiguration` 按 classpath 与已有 Bean 决定装配什么，「约定优于配置」的工程化极致。在 SelfBlog 中：引了 `spring-boot-starter-web` 就有 Tomcat+Jackson+MVC，引了 actuator 就有 `/actuator/health`——你的 `application.yml` 只写偏离约定的部分。

**Spring Bean（Bean）**：容器管理的对象，默认单例、启动期构建。在 SelfBlog 中：`SecretKey`、`PasswordEncoder`（BCrypt 强度 12）、8 个缓存等基础设施 Bean 在 `config/` 声明，业务 Bean 靠注解扫描。

**Spring Security Filter Chain（安全过滤器链）**：十几个过滤器串成链，每个请求依次通过（CSRF→CORS→认证→授权…）。在 SelfBlog 中：`SecurityConfig` 声明这条链：`JwtAuthenticationFilter` 挂在用户名密码过滤器之前，匿名放行到授权决策；`STATELESS` 全程不建会话。

**Starter Parent / BOM（依赖管理）**：parent POM 或 BOM 锁定依赖版本，子项目免写版本号。在 SelfBlog 中：`pom.xml` 的 `spring-boot-starter-parent` 3.3.13 托管全家族版本，jjwt 0.12.6 是例外——需显式声明版本。

**Transaction Propagation（事务传播）**：事务方法嵌套时新事务的归属规则：`REQUIRED`（默认，有则加入）、`REQUIRES_NEW`（挂起外层开新事务，常用于「独立成败」的日志/通知）、`NESTED`（保存点）。在 SelfBlog 中：门面委托方法重标事务注解就是显式选择传播行为；通知落库若要求「主流程失败也不回滚通知」，就该 `REQUIRES_NEW`。

**Validation（参数校验）**：JSR-380 注解（`@NotNull`/`@Size`/`@Pattern`）+ `@Validated` 触发。在 SelfBlog 中：dto 上的校验注解是第一道闸（格式），service 内语义校验是第二道（`visitorId` 正则、长度上限）；违反时 `GlobalExceptionHandler` 转 400。

**YAML Configuration（YAML 配置）**：缩进式配置格式，`application.yml` 优于 properties 的层级表达。在 SelfBlog 中：`application.yml` + `application-dev.yml`/`application-prod.yml`；注意 yml 里 `app.api-prefix: /api` 这类键经 relaxed binding 映射到 `@ConfigurationProperties` 字段。

## 组四：Web 与 HTTP

**404 / 503 Semantics（状态码语义）**：状态码是 API 的一部分，不是「能返回数据就行」。在 SelfBlog 中：`SpaController` 公开路由缺失 404、模板缺失 503、`/index.html` 308——错误都有真实语义，而非一律 200 包错误 JSON；`GlobalExceptionHandler` 把业务 code 映射到 HTTP 状态。

**Accept / Content Negotiation（内容协商）**：客户端用 `Accept` 头声明想要的媒体类型，服务端按能力返回。在 SelfBlog 中：`SeoController` 的 XML 端点按路径而非协商返回（`/feed.xml` 固定 `application/xml`），这是对爬虫最友好的简单方案。

**Base64**：二进制→文本的编码（不是加密！），3 字节一组映射 4 字符，末尾补 `=`。在 SelfBlog 中：JWT 三段皆 Base64URL；前端 token 指纹 = 首尾各 20 字符再 btoa 脱敏。

**Cache-Control / ETag（HTTP 缓存头）**：`max-age/no-cache/no-store` 与实体标签协商缓存。在 SelfBlog 中：SPA 启动壳对后台路由强制 `noindex`；静态资源由 Vite 产物文件名哈希天然支持长缓存；`SeoController` 的 XML 响应可加短 TTL。

**Content Security Policy（CSP，内容安全策略）**：响应头白名单化页面可加载的资源来源，XSS 的纵深防御。在 SelfBlog 中：`SecurityConfig` 设了 `X-Frame-Options: DENY`、`Referrer-Policy`、`Permissions-Policy`；CSP 可按同思路扩展（注意与 Vue 内联样式、marked 渲染 HTML 的兼容）。

**Cookie 属性（Cookie Attributes）**：`HttpOnly`（JS 不可读）、`Secure`、`SameSite`（防 CSRF 的关键属性）。在 SelfBlog 中：JWT 走 `Authorization` 头而非 Cookie，因此默认无会话 Cookie；XSRF token 走 `X-XSRF-TOKEN` 头是给「未来若引入 Cookie 场景」预留的双提交防线。

**CORS（跨源资源共享）**：服务器用 `Access-Control-*` 头显式授权跨源浏览器请求；预检 `OPTIONS` 先行。在 SelfBlog 中：`SecurityConfig` 的 CORS 配置**拒绝通配符结尾的 origin**（防 `evil.example` 匹配 `*.example` 级联放行），开发期前端同源代理 `/api` 根本不触发 CORS。

**CSRF（Cross-Site Request Forgery，跨站请求伪造）**：借浏览器自动携带 Cookie 的特性冒名提交。防御：SameSite、CSRF token、纯 token 认证。在 SelfBlog 中：JWT 走 `Authorization` 头，跨站页面拿不到该头，故 `csrf().disable()`——`SecurityConfig` 注释里有完整论证。

**Envelope Pattern（信封模式）**：响应外层包统一元数据（code/message），内层才是数据，让成功失败同构。在 SelfBlog 中：`Result<T>{code,message,data,timestamp}`；前端拦截器读信封、组件读 `res.data`。

**ETag / Last-Modified（条件请求）**：`If-None-Match`/`If-Modified-Since` 命中则 304，省带宽。在 SelfBlog 中：静态资源与 SEO XML 可用；API 动态数据不强求。

**Forward Proxy vs Reverse Proxy（正向/反向代理）**：正向代理替客户端出门，反向代理替服务器接客（TLS 终结、缓存、路由）。在 SelfBlog 中：开发期 Vite 代理 `/api`→8825 是反向代理；部署期 `frontend/vercel.json` 生成的 Build Output v3 forwarding 同理——`SELFBLOG_BACKEND_ORIGIN` 指向唯一后端。

**HMAC（Hash-based MAC）**：带密钥的哈希 MAC，验证「消息完整 + 发送方持有密钥」。在 SelfBlog 中：JWT 签名是 HMAC-SHA256（HS256）；`credentialVersionFor` 用 HmacSHA256 对域分离串 `selfblog:credential-version:v1\nuserId\npasswordHash` 计算，只存 HMAC 不暴露 BCrypt 哈希。

**HTTP Methods Semantics（方法语义）**：GET 安全幂等、PUT 幂等、POST 非幂等、DELETE 幂等。在 SelfBlog 中：点赞接口 POST `/like/toggle` 破坏了 POST 非幂等惯例——但服务端 `synchronized` 三态切换保证幂等效果，语义与实现各让一步。

**Idempotency（幂等性）**：同一操作执行多次与一次效果相同。支付、点赞类接口必须有界。在 SelfBlog 中：匿名点赞 `toggle` 的三态机（无→insert / 0→1 / 1→0）+ `synchronized` 保证并发下幂等；前端重试机制（`RETRYABLE_CODES`）只对幂等接口安全。

**JWT（JSON Web Token）**：三段式（Header.Payload.Signature）自包含令牌，签名保证不可篡改，载荷仅 Base64URL 可读（不放敏感数据）。在 SelfBlog 中：`JwtUtils` 签发/校验，载荷含 userId 与过期时间；`credentialVersion` 嵌入实现改密即全设备失效。

**JWS / JWE（签名/加密 JWT）**：JWS=签名保证完整性（SelfBlog 所用），JWE=加密保证机密性。在 SelfBlog 中：用 JWS——令牌内容本就不敏感（userId），机密性靠传输层 HTTPS。

**Keep-Alive / Connection Reuse（连接复用）**：HTTP/1.1 默认长连接减少握手；HTTP/2 多路复用更进一步。在 SelfBlog 中：HikariCP 池化数据库连接是同一思想的 DB 版；前端 axios 实例复用底层连接。

**MIME Type（媒体类型）**：`application/json`、`text/html`、`application/xml` 决定处理方式。在 SelfBlog 中：`SpaController` 返回 `text/html`、API 返回 JSON、`SeoController` 返回 XML——类型即契约。

**Origin / Same-Origin Policy（同源策略）**：协议+域名+端口三元组同源才可自由互访，是浏览器安全的基石。CORS 是服务端对它的显式豁免。在 SelfBlog 中：前端永远同源请求 `/api`（开发代理、生产反代），从架构上回避跨源；`SecurityConfig` 的 CORS 配置只是兜底。

**Rate Limiting（限流）**：控制单位时间请求数保护后端。算法：固定窗口（简单、边界突刺）、滑动窗口（平滑）、令牌桶（允许突发）。在 SelfBlog 中：`RateLimitInterceptor` 是**固定窗口**（Caffeine 计数、1 分钟窗、`ip:path` 键、超限 429 JSON、最长前缀匹配阈值）；单实例内存实现，多实例需 Redis——注释明言。

**REST（Representational State Transfer）**：资源+URI+统一接口的架构风格：名词 URI、动词 HTTP 方法、状态码表达结果。在 SelfBlog 中：`/api/post/{id}`、`/api/guest-comments` 名词化命名；但点赞用 `POST /toggle` 动词化——实战中务实优先于教条。

**Session vs Token（会话与令牌）**：Session 状态在服务端（Cookie 关联），Token 状态在客户端（签名防篡改），后者天生适配水平扩展与 STATELESS。在 SelfBlog 中：`STATELESS` + JWT，服务端只存黑名单与设备集——「有状态的认证，无状态的服务」。

**SPA（Single-Page Application，单页应用）**：一次加载、前端路由切换视图，服务端只供数据。代价：首屏、SEO、深链恢复都要额外工程。在 SelfBlog 中：Vue SPA + `SpaController` 服务端补可读 HTML 双轨解决 SEO；历史路由 `/post/{id}` 直出真实内容。

**SSL/TLS Termination（TLS 终结）**：反向代理卸载 HTTPS，内网明文转发。在 SelfBlog 中：Vercel/反代层终结 TLS，后端容器只跑 HTTP；`SiteOrigin` 解析 origin 时忽略 `X-Forwarded-*` 伪造正是为此兜底。

**URI Design（URI 设计）**：资源名词化、层级表达从属、避免动词与实现细节泄漏。在 SelfBlog 中：`/api/admin/post/{id}/status` 层级清晰；后台地址 `adminPath` 是站点设置而非后端概念——后端安全只认 `/api/admin/**`。

**Web Security Headers（安全响应头）**：`X-Frame-Options`（防点击劫持）、`Referrer-Policy`、`Permissions-Policy`（关硬件能力）、HSTS。在 SelfBlog 中：`SecurityConfig` 逐一设置——`X-Frame-Options: DENY`、`strict-origin-when-cross-origin`、关摄像头/定位/麦克风。

**XSS（Cross-Site Scripting，跨站脚本）**：注入恶意脚本到他人浏览器执行。防御：输出编码、白名单消毒、CSP。在 SelfBlog 中：三层防线——`XssFilter` 请求级过滤；`GuestCommentServiceImpl` **先消毒后校验**（`HtmlSanitizer.sanitizePlainText` 打头）；前台渲染走 `SafeMarkdown` 白名单。

## 组五：数据库与 ORM（含缓存）

**BaseMapper（通用 Mapper）**：MyBatis Plus 的根接口，免费提供 insert/selectById/updateById/deleteById 等单表操作，子接口零方法即有全套 CRUD。在 SelfBlog 中：`mapper/` 下 28 个接口全部 `extends BaseMapper<XxxEntity>`，绝大多数接口一行方法体都不用写。

**Batch Processing（批处理）**：把 N 次往返合并为一次批量操作（批量 insert、`IN` 查询、`saveBatch`），是消灭 N+1 的正面手段。在 SelfBlog 中：`GuestCommentServiceImpl` 的 `getAdminComments` 用 `selectBatchIds` 一次取回全部关联用户，替代循环单查；MyBatis Plus 的 `saveBatch` 可用于种子数据。

**Cache-Aside Pattern（旁路缓存）**：读时先查缓存、未命中回源 DB 再写缓存；写时更新 DB 后失效缓存。最常用也最容易写错的缓存模式（失效时机是难点）。在 SelfBlog 中：service 层 `@Cacheable` 查询 + 写操作 `@CacheEvict` 的组合即旁路缓存——`siteSettingsCache` 在设置更新时被逐出。

**Cache Avalanche（缓存雪崩）**：大量缓存同一时刻集体过期，DB 瞬间被打穿。缓解：TTL 加随机抖动、错峰过期、多级缓存。在 SelfBlog 中：`CaffeineCacheConfig` 的 8 个缓存 TTL 各不相同（30s~30m），本身就是一种天然错峰。

**Cache Breakdown（缓存击穿）**：单个热点 key 过期瞬间，并发请求同时回源。缓解：互斥重建、逻辑过期、热点永不过期。在 SelfBlog 中：Caffeine 的 `get(key, mappingFunction)` 原子加载即互斥重建；本站热点（热门标签、站点设置）读多写少，风险本就低。

**Cache Penetration（缓存穿透）**：查询**不存在**的数据，缓存永不命中、请求全打到 DB（可被恶意利用）。缓解：空值缓存、布隆过滤器、参数校验。在 SelfBlog 中：`statusCache`(2m) 缓存存在性判断类结果；controller 层的参数校验（如 ID 格式）在进入 DB 前先拦掉明显非法请求。

**Connection Pool（连接池）**：预先建好并复用数据库连接，避免每次请求握手认证（毫秒~十毫秒级开销）。在 SelfBlog 中：HikariCP 是 Spring Boot 默认池；SQLite 场景池大小固定为 1（SQLite 单写者），MySQL/池参数随环境调整。

**CRUD（增删改查）**：Create/Read/Update/Delete 四类基本数据操作的统称，分层架构里 CRUD 代码应最薄。在 SelfBlog 中：BaseMapper 覆盖单表 CRUD，service 只写「有业务含义」的部分（归属校验、副作用、缓存失效）。

**databaseId / Multi-Dialect SQL（多方言 SQL）**：同一语句按数据库厂商切换实现，XML 里 `databaseId="mysql"/"sqlite"` 各写一份，无 id 的作为兜底。在 SelfBlog 中：`MybatisPlusConfig` 注册 `VendorDatabaseIdProvider`；**H2 没有映射 id，只走无 databaseId 的兜底语句**——MySQL 专有语法绝不能写进共享语句。

**DDL / DML（数据定义与操作语言）**：DDL 定义结构（CREATE/ALTER/DROP），DML 操作数据（INSERT/UPDATE/DELETE/SELECT）。在 SelfBlog 中：`schema-sqlite.sql`（`CREATE TABLE IF NOT EXISTS`，不迁移既有文件）与 `schema-fresh-install.sql`（含 DROP，只给全新空库）是 DDL；`migration-selfblog.sql` 是已有库唯一的升级通道。

**DTO / VO / PO / Entity（数据传输/视图/持久对象与实体）**：PO(Entity) 映射表行；DTO 承接入参；VO 塑造出参。分层的意义是「表结构变化不直接击穿到接口契约」。在 SelfBlog 中：`entity/` 28 个实体（`@TableName` + `@TableLogic isDeleted`）、`dto/` 33 个入参、`vo/` 37 个出参，三个目录互不混用；`utils/UserConverter` 之类转换器负责 entity→vo。

**Foreign Key（外键）**：声明表间引用约束，由数据库强制一致性。互联网高并发库常弃用外键（锁与迁移成本），改由应用层保证。在 SelfBlog 中：schema 里关联关系主要靠应用层维护（如 `blog_post_tag` 中间表），这要求 service 层「先校验存在再写入」的纪律。

**HikariCP**：Spring Boot 默认连接池，以字节码级优化与极低开销著称，核心参数 `maximumPoolSize`、`connectionTimeout`。在 SelfBlog 中：`DatabaseConfig` 显式配置——SQLite `maximumPoolSize=1`（单写者模型下的正确选择），MySQL 按环境变量连接五件套建池。

**Index（索引）**：加速查询的有序数据结构（B+ 树），代价是写入变慢与存储增加；最左前缀原则决定复合索引能否命中。在 SelfBlog 中：搜索走 `title LIKE`/内容匹配的高频列应有索引；`guest_comment.post_id`、`anonymous_post_like(post_id, visitor_hash)` 这类查询入口列是索引候选——加索引要连跑三库 schema 验证。

**Isolation Level（隔离级别）**：并发事务互不干扰的程度：READ UNCOMMITTED→READ COMMITTED→REPEATABLE READ→SERIALIZABLE，逐级消除脏读/不可重复读/幻读。在 SelfBlog 中：依赖各库默认级（MySQL InnoDB 可重复读、SQLite 串行写）；计数更新用原子 `UPDATE ... SET like_count = like_count + 1` 把并发正确性下沉给数据库，不靠提高隔离级。

**KV Table（键值表）**：以 `key` 为主键、`value` 存值的通用表，新增配置项零 DDL。在 SelfBlog 中：`site_setting`（`setting_key` 主键 + `setting_value`）——新增站点设置键只需在 `SiteSettingsServiceImpl` 登记并做校验，**零 schema 迁移**，这是全项目最划算的一张表。

**LRU（Least Recently Used，最近最少使用）**：淘汰最久未访问条目的缓存策略，Caffeine 以 W-TinyLFU（频率+新近度）超越朴素 LRU。在 SelfBlog 中：`RateLimitInterceptor` 的计数缓存设 `maximumSize(50000)`，满了 Caffeine 自动按新近度/频率逐出，防内存无界。

**Logic Delete / Soft Delete（逻辑删除/软删除）**：删除只打标记位（`is_deleted=1`），数据保留可恢复、可审计，代价是所有查询都要过滤。在 SelfBlog 中：20+ 实体带 `@TableLogic private Integer isDeleted` + `application.yml` 的 `logic-delete-field`，MyBatis Plus 自动在生成 SQL 里追加 `is_deleted=0`——但**手写 SQL 必须自己带上该条件**，这是最容易漏的一条。`AnonymousLikeServiceImpl` 的三态切换（0↔1）正是手工操作软删标记的实例。

**Migration Script（迁移脚本）**：对已有库做增量结构/数据变更的脚本，与「全新建库脚本」严格分离。在 SelfBlog 中：`schema-fresh-install.sql` 只用于全新空库（改名就是为了降低误执行风险），**已有库只能跑 `migration-selfblog.sql`**；Docker Compose 挂载的也是迁移件。

**MyBatis**：半自动 ORM——SQL 全手写、参数与结果映射自动化，SQL 控制权最大。在 SelfBlog 中：复杂查询（分片站点地图的统计、跨表关联）写在 `resources` 的 Mapper XML 里。

**MyBatis Plus**：在 MyBatis 上增强：通用 CRUD、条件构造器、分页插件、`@TableLogic`/`@Version`/自动填充，保留手写 SQL 能力。在 SelfBlog 中：3.5.8 版本；`ServiceImpl<XxxMapper, Entity>` 基类让门面与查询两个实现都能继承同一套泛型方法。

**N+1 Query（N+1 查询）**：查 1 次列表再循环查 N 次关联数据的性能反模式，列表页杀手。在 SelfBlog 中：`GuestCommentServiceImpl.getAdminComments` 先查分页评论，再 `selectBatchIds(visitorIds)` 一次取全部访客——1+N 变 2 次；发现列表接口慢，第一件事就是数 SQL 条数。

**ORM（Object-Relational Mapping，对象关系映射）**：在对象世界与关系表之间自动翻译，代价是复杂查询时「表达力倒挂」。在 SelfBlog 中：MyBatis Plus 管单表、XML 管复杂 SQL 的组合，是对 ORM 边界的清醒用法。

**Pagination（分页）**：大结果集按页返回。两种流派：偏移分页（LIMIT/OFFSET，简单但深翻页慢）与游标分页（where id > last，稳定但要排序键）。在 SelfBlog 中：MyBatis Plus 分页插件 + `Result` 信封里的 `records/total/size/current/pages` 六件套；管理后台列表全部偏移分页。

**Primary Key（主键）**：行的唯一标识。自增长序 vs 分布式 ID（雪花/UUID）取决于是否多写入点。在 SelfBlog 中：核心表自增主键（单实例足够），MyBatis Plus 的 `IdType` 注解声明；`visitorId` 这类外部标识则用正则约束格式后哈希入库存。

**Query Wrapper（条件构造器）**：链式构建 WHERE 条件的对象化 API（`lambdaQuery().eq(...).like(...)`），杜绝字符串拼 SQL。在 SelfBlog 中：service 查询实现大量使用 LambdaQueryWrapper——列名由方法引用推导，改字段名编译期即报错。

**Schema（模式/表结构）**：库中表、列、约束、索引的整体定义。在 SelfBlog 中：分两组维护——历史社区表（`sys_user`、`blog_post`、`blog_comment`、`blog_tag`、`blog_follow`、`blog_notification` 等）与个人博客新表（`site_setting`、`visitor_event`、`anonymous_post_like`、`guest_comment`、`photo_album`、`album_photo`）；改任何一张表都要过三库初始化策略（SQLite 自动建/MySQL 全新脚本/H2 测试脚本）。

**SELECT FOR UPDATE（悲观锁查询）**：事务内锁定选中行直到提交，天然串行化并发修改。在 SelfBlog 中：计数类「读-改-写」若不用原子 UPDATE，就应退到 `FOR UPDATE`；SQLite 单写者模型下锁语义由库自身承担，MySQL 场景才真正体现价值。

**Snowflake ID（雪花 ID）**：64 位分布式 ID = 时间戳 + 机器位 + 序列号，趋势递增、不依赖库。在 SelfBlog 中：MyBatis Plus 内置 `ASSIGN_ID`（雪花算法）可选作 `@TableId` 策略；当前单实例用自增即可，多实例部署时雪花 ID 是现成答案。

**SQL Injection（SQL 注入）**：用户输入被拼进 SQL 语义层，历史上最著名的注入类漏洞。防御：参数化查询（占位符）、最小权限、不信任拼接。在 SelfBlog 中：MyBatis 的 `#{}` 占位符全程参数化；`${}` 字符串替换只允许出现在无用户输入的内部常量处——代码评审时见 `${` 就要问一句值从哪来。

**TTL（Time To Live，存活时间）**：缓存条目的寿命，到期逐出。TTL 是「新鲜度 vs 命中率」的直接旋钮。在 SelfBlog 中：8 个具名缓存各自独立 TTL（`hotTagsCache` 10m、`statusCache` 2m、`visitorSummaryCache` 30s……），按数据变化频率分别标定，而不是一刀切。

**Transaction（事务）**：一组操作要么全成要么全败（ACID）。Spring 里 `@Transactional` 声明式开启，注意默认只回滚 RuntimeException/ Error，受检异常不回滚。在 SelfBlog 中：写操作统一 `@Transactional(rollbackFor = Exception.class)` 显式覆盖默认规则；查询方法用 `readOnly = true` 让驱动与连接池做优化。

**Type Handler（类型处理器）**：MyBatis 在 JDBC 类型与 Java 类型之间转换的插件点。在 SelfBlog 中：`LocalDateTime` 与 SQLite 文本时间的映射靠默认/自定义 handler；写枚举字段入库时也可在此收口。

**Unique Constraint（唯一约束）**：数据库层防重复的最后防线——应用层校验可被并发绕过，唯一索引不会。在 SelfBlog 中：`guest_comment` 的关联键、`site_setting.setting_key` 主键天然唯一；发布文章的唯一性修复（防重复 slug）就是「应用层检查 + 数据库唯一约束 + 捕获重复键异常」三件套，`docs/agents/backend.md` 有专门记录。

**Version Field（版本号字段）**：乐观锁的载体：更新带 `WHERE version = ?` 并 `SET version = version + 1`，影响行数为 0 即冲突。在 SelfBlog 中：MyBatis Plus `@Version` + 乐观锁插件可开箱即用；当前核心写路径冲突概率低未启用，但「管理后台并发编辑同一文章」是它的候选场景。

## 组六：安全

**Allowlist / Denylist（白名单/黑名单）**：白名单「默认拒绝、显式允许」，黑名单「默认允许、显式拒绝」——安全场景永远优先白名单。在 SelfBlog 中：`HtmlSanitizer` 白名单化标签与属性；`SecurityConfig` 的放行清单是显式 URL 白名单；CORS 拒绝通配符结尾 origin 也是白名单纪律。

**Authentication vs Authorization（认证与授权）**：认证回答「你是谁」，授权回答「你能做什么」——两者独立失败、独立测试。在 SelfBlog 中：`JwtAuthenticationFilter` 只管认证（解析 token 放身份）；授权在 `SecurityConfig` 的 `authorizeHttpRequests` 与方法级 `@PreAuthorize`——「只解析不拒绝」的职责切分就是这条边界。

**BCrypt**：专为口令设计的慢哈希（基于 Blowfish），内置随机盐、可调成本因子，抗 GPU 暴力破解。在 SelfBlog 中：`SecurityConfig` 声明 `BCryptPasswordEncoder`，强度 12（`bcrypt.strength` 可配）——每提升 1，破解成本翻倍、登录耗时也翻倍。

**Brute Force Attack（暴力破解）**：穷举口令的攻击，防御靠慢哈希、限流、锁定与告警。在 SelfBlog 中：登录接口落在限流前缀下（1 分钟窗口计数）；BCrypt 强度 12 本身就是「让每次猜测都变贵」的暴力破解税。

**Clickjacking（点击劫持）**：把你的页面塞进透明 iframe 诱导点击。防御：`X-Frame-Options: DENY` 或 CSP `frame-ancestors`。在 SelfBlog 中：`SecurityConfig` 明确设置 `X-Frame-Options: DENY`，后台界面不可被嵌框。

**Credential Version（凭证版本）**：用户级整数/哈希版本号，嵌入 JWT 载荷，改密时递增使旧 token 全设备失效。在 SelfBlog 中：`JwtUtils.credentialVersionFor` 用 HmacSHA256 对域分离串 `selfblog:credential-version:v1\nuserId\npasswordHash` 计算版本指纹——**只存 HMAC 不暴露 BCrypt 哈希**，校验 token 时比对版本不一致即拒绝。

**Defense in Depth（纵深防御）**：多层独立防线，单层失守不致全局沦陷。在 SelfBlog 中：XSS 三层（`XssFilter` 请求级 → `sanitizePlainText` 入库前 → `SafeMarkdown` 出库渲染）；权限两层（URL 层 `hasRole` + 方法层 `@PreAuthorize`）；写入两层（dto 校验注解 + service 语义校验）。

**Domain Separation（域分离）**：密码学惯例——同一密钥派生多种用途时，把「用途标签」混入被哈希消息，防跨用途重放。在 SelfBlog 中：`credentialVersionFor` 的 `selfblog:credential-version:v1\n` 前缀就是域分离串：将来任何「拿 JWT_SECRET 再派生点什么」的需求都必须带自己的域前缀。

**Double Submit Cookie（双提交 Cookie）**：CSRF 防御术——Cookie 与请求头各带同一随机值，服务端比对一致才放行（跨站攻击者读不到 Cookie）。在 SelfBlog 中：前端 axios 拦截器生成 32 字节 hex 的 XSRF token 并注入 `X-XSRF-TOKEN` 头，为未来 Cookie 化场景预留的现成防线。

**Encoding vs Encryption vs Hashing（编码/加密/哈希之辨）**：编码可逆无密钥（Base64）；加密可逆有密钥；哈希单向不可逆。混淆三者是安全评审高频扣分点。在 SelfBlog 中：JWT 载荷是编码（人人可读，别放敏感信息）；口令是哈希（BCrypt）；token 存储经前端 `utils/crypto` 编解码——各安其位。

**Fail-closed（失败关闭）**：依赖组件不可用时选择「拒绝服务」而不是「放行」。安全组件必须 fail-closed，可用性组件才谈 fail-open。在 SelfBlog 中：`EnvValidationConfig` 启动期校验 `JWT_SECRET` 不达标直接拒绝启动；`JwtAuthenticationFilter` 解析异常一律按匿名处理（交给授权层拒绝）而非放行。

**Hash（哈希）**：单向函数，任意输入映射为固定长摘要，微改输入雪崩输出。用于完整性校验、口令存储、指纹。在 SelfBlog 中：`VisitorHashUtils.hash(visitorId, hashSecret)`——访客标识不以明文入库，只存加盐哈希，库泄漏也无法还原 visitorId。

**Input Validation（输入校验）**：在信任边界上校验类型、格式、长度、范围——「校验的是形状，消毒的是内容」。在 SelfBlog 中：`visitorId` 正则 `[A-Za-z0-9_-]{8,80}` 先验格式；`normalizeSize` 把分页参数钳到上限 100；dto 注解管格式、service 语义校验管业务。

**JWT Blacklist（令牌黑名单）**：登出/改密后令牌在自然过期前仍有效，黑名单显式吊销——「有状态的认证，无状态的服务」的妥协点。在 SelfBlog 中：`JwtUtils` 内 `ConcurrentHashMap` 黑名单 + `CopyOnWriteArraySet` 设备令牌集，`JwtSchedulerConfig` 周期清理过期项防无界增长。

**Least Privilege（最小权限原则）**：主体只授予完成任务所必需的最小权限。在 SelfBlog 中：`/api/admin/**` 整体 `hasRole("admin")`，公开接口逐条白名单放行（而不是放行 `/api/**` 再拉黑）；数据库账号理论上也应区分「迁移权限」与「运行时权限」。

**OWASP**：开放Web应用安全项目，OWASP Top 10 是应用安全风险的公共清单与评审框架。在 SelfBlog 中：注入（参数化）、失效的认证（JWT 吊销链）、XSS（三层消毒）、安全配置错误（SecurityConfig 逐项加固）等条目都有对应落点——附录A 的故障排查与安全评审可直接对照 Top 10 打勾。

**Password Hashing（口令哈希）**：口令绝不明文/可逆存储，用慢哈希 + 盐（BCrypt 自带）入库。在 SelfBlog 中：BCrypt 强度 12；`credentialVersionFor` 刻意**不把 BCrypt 哈希带出库**——版本指纹是 HMAC，即使日志泄漏也反推不出口令材料。

**Password Policy（口令策略）**：长度/复杂度/更换策略。现代共识：长度优先于复杂度，NIST 已弃强制定期换密。在 SelfBlog 中：`ADMIN_PASSWORD` 要求 ≥12 位并由 `EnvValidationConfig` 启动期强制；注册关闭后普通用户口令策略退场。

**Path Traversal（路径穿越）**：以 `../` 逃出预期目录读写任意文件。防御：规范化后校验前缀、白名单文件名。在 SelfBlog 中：媒体/上传相关接口对文件名做白名单与规范化处理；静态资源映射限定根目录。

**Principal（安全主体）**：安全上下文中的「当前操作者」，可能是用户、匿名或系统。在 SelfBlog 中：`SecurityUtils` 兼容 principal 为 `UserContext` 或裸 `Long` 两种形态——从主体取 ID 前先判型，这是切片测试里 `@WithMockUser` 也能工作的原因。

**RBAC（Role-Based Access Control，基于角色的访问控制）**：权限挂角色、用户挂角色，避免逐用户授权爆炸。在 SelfBlog 中：单一 `ROLE_admin` 角色——单作者博客把 RBAC 简化到极致；`app.single-author-mode=true` 时写入类操作还有「必须 admin」的兜底规则。

**Salt（盐）**：哈希前混入的随机值，使相同口令产生不同摘要、预计算彩虹表失效。BCrypt 把盐直接编进结果串。在 SelfBlog 中：BCrypt 自动处理口令盐；`VisitorHashUtils.hash(visitorId, hashSecret)` 的 hashSecret 扮演「全局盐」角色（取自 `JWT_SECRET` 缺省 `local-visitor-hash`）。

**Sanitize vs Escape（消毒与转义）**：消毒按白名单移除危险内容（入库存的干净数据）；转义在输出语境中让字符失去元语义（HTML/JS/属性上下文各不同）。在 SelfBlog 中：入库走 `HtmlSanitizer.sanitizePlainText`（消毒）；前台渲染走 `SafeMarkdown` 与框架默认转义（escape）——两道工序不可互相替代。

**Secret Management（密钥管理）**：密钥的生成、分发、轮换与泄漏处置。规则：不入库、不进日志、可轮换。在 SelfBlog 中：`JWT_SECRET`（≥32 位）走 `.env`/环境变量，`DotenvConfig` 保证文件值成为 JVM property；`ADMIN_PASSWORD` 同理；日志侧 `StringMaskUtils`/token 指纹防止敏感串进日志。

**Single Author Mode（单作者模式）**：SelfBlog 的根本设定：`app.single-author-mode: true` 时，写入类操作在常规授权之外再要求 `ROLE_admin` 兜底——即使某接口漏了 URL 防护，模式兜底仍拦截访客写入。在 SelfBlog 中：`SecurityConfig` 读取该开关；它同时是「注册关闭（403）」语义的一部分，是理解本项目安全模型的钥匙。

**Threat Model（威胁建模）**：设计期回答「谁会攻击你、图什么、从哪进」。个人博客的典型对手：爬虫刷接口、匿名灌水、伪造 Host 抢 canonical。在 SelfBlog 中：限流防刷、先消毒后校验防灌水、`SiteOrigin` 固定 origin 防伪造——三个机制各自对应一类已建模威胁，而不是无的放矢的加固。

**Timing Attack（时序攻击）**：通过响应时间差异推断秘密（逐字符比对快慢）。防御：常数时间比较。在 SelfBlog 中：token 校验走 jjwt 库内部实现（HMAC 比较为常数时间）；自定义比对场景（指纹比对）应优先用 `MessageDigest.isEqual` 而非手写 `equals` 循环。

**Token Fingerprint（令牌指纹）**：日志与错误上报中只展示令牌的不可逆缩略（首尾片段），既能对账又不泄漏完整凭证。在 SelfBlog 中：前端 `api/index.ts` 以「首尾各 20 字符再 btoa」生成指纹写入日志；后端 `StringMaskUtils` 在服务端做同类脱敏。

**Visitor ID Hashing（访客 ID 哈希）**：匿名访客标识入库前加盐哈希，兼顾「同访客可识别」与「库泄漏不可还原」。在 SelfBlog 中：`VisitorHashUtils.hash(visitorId, hashSecret)` 产出的哈希是 `anonymous_post_like.visitor_hash` 与留言关联的存储形态——同一 visitorId 永远得到同一哈希，幂等点赞才成立。

## 组七：前端框架（Vue 生态为主）

**Composition API（组合式 API）**：`setup()`/`<script setup>` 按逻辑关注点组织代码，替代 Options API 的选项切割；配合组合式函数实现逻辑复用。在 SelfBlog 中：全仓库组件统一 `<script setup lang="ts">`；`composables/` 下 8 个组合式函数（`useConfirm`/`useDialog`/`useMarkdownEnhance`/`usePageConfig`/`usePageSeo`/`useScrollReveal`/`useScrollState`/`useSeoMeta`）是组合式 API 的复用成果。

**Composable（组合式函数）**：以 `use` 开头、封装响应式状态+生命周期逻辑的函数，是 Vue 3 逻辑复用的标准形态（替代 mixin）。在 SelfBlog 中：`useScrollReveal` 封装滚动进入动画、`useSeoMeta` 统一 document.meta 更新；注意 `useConfirm()` 返回的 `ConfirmDialog` **必须渲染到模板里**，否则确认框不出现——这是测试与实机的经典差异点。

**Computed（计算属性）**：由响应式依赖派生的缓存值，依赖不变不重算。在 SelfBlog 中：store 与组件里大量 `computed` 派生（如站点设置 → 页头标题、皮肤类名的映射），避免手写脏检查。

**Destructuring Loss（响应式解构丢失）**：对 props/reactive 解构会丢失响应性（值被复制成静态量）。解法：`toRefs`、或在模板/计算属性内保持属性访问路径。在 SelfBlog 中：组件接收 props 一律 `props.xxx` 访问而不是解构——`tsconfig` 的 `noUncheckedIndexedAccess` 与此约定共同构成两个最常踩的静态陷阱。

**Diff / Patch（差异比较与补丁）**：新旧虚拟 DOM 树逐层比对，最小化真实 DOM 操作。同层比较、key 复用节点。在 SelfBlog 中：文章列表 keyed `v-for` 用文章 ID 作 key——用数组下标作 key 会在头部插入时错位复用，是 diff 环节最常见的 bug 源。

**Directive（指令）**：带 `v-` 前缀的模板扩展（`v-if`/`v-for`/`v-model`/`v-bind`/`v-on`），可自定义。在 SelfBlog 中：表单双向绑定全走 `v-model`；权限按钮用条件渲染而非自定义指令——保持指令面最小。

**HMR（Hot Module Replacement，模块热替换）**：开发期局部替换变更模块并保留应用状态，比整页刷新快且不丢表单。在 SelfBlog 中：`vite` dev server（端口 3000）自带 HMR；改样式 token 与组件模板即时生效——客户端插件类改动则需确认 `pnpm run dev:web` 类监听器在跑，否则不会自动更新。

**Lazy Loading Route（路由懒加载）**：路由组件按需 `import()`，进入页面才加载对应 chunk，首屏只带必要代码。在 SelfBlog 中：`frontend/src/router/` 全部路由组件懒加载 `component: () => import(...)`（`modules/admin.ts` 一处文件就有 12 处），`helpers.ts` 封装 `lazyLoad` + `ErrorFallback`——chunk 加载失败有兜底 UI。

**Lifecycle Hooks（生命周期钩子）**：组件各阶段回调（`onMounted`/`onUnmounted` 等），副作用注册与清理的锚点。在 SelfBlog 中：挂载会注册全局监听器的组件（主题监听、滚动监听）必须在 `onUnmounted` 清理——前端测试约定明确要求 `enableAutoUnmount(afterEach)`，因为 jsdom setup **没有**自动卸载。

**Pinia**：Vue 官方状态库，天然 TS、去 mutation、组合式 store 风格，devtools 可追踪。在 SelfBlog 中：5 个 store——`site.ts`（站点设置、`adminPath`/`adminRoot`、皮肤与 favicon）、`theme.ts`（`localStorage['edu_theme']`、`.dark-mode`、`matchMedia` 监听）、`user.ts`（**store id 是 `'auth'`，导出别名 `useUserStore`**，token 经 `utils/crypto` 编解码存取，`validateToken` 只解 JWT 不请求网络）、`music.ts`（唯一音频宿主）、`pages.ts`（**只存已发布页面配置，草稿绝不进公开 store**）。

**props Down / events Up（单向数据流）**：父传子 props、子通知父事件，数据流向单向可追踪。在 SelfBlog 中：后台表单组件全部遵循「props 进、emit 出」，`defineProps`/`defineEmits` 编译宏声明契约。

**Reactivity System（响应式系统）**：Vue 3 用 Proxy 追踪依赖、触发更新——读取时收集、写入时派发。在 SelfBlog 中：`ref`/`reactive`/`computed` 的行为皆源于此；理解「解构丢响应」「数组索引赋值无需 `Vue.set`（Proxy 时代已修）」都要回到 Proxy 机制。

**SFC（Single-File Component，单文件组件）**：`.vue` 文件把 template/script/style 装进一个组件单元，编译器拆解为 JS 渲染函数与样式。在 SelfBlog 中：`views/` 9 组、`components/` 5 组全部 SFC；`vite-plugin-vue` 负责编译。

**Slot / Props Fallthrough（插槽与属性透传）**：插槽让父组件向子组件注入模板；未声明的 attrs 自动落到根元素。在 SelfBlog 中：布局组件（`components/layout/`）用插槽承接页面主体；测试约定**禁用 `stubs: { RouterLink: true }`**——stub 会丢掉 slot 内容，也表达不了 active 态，路由测试优先用真实 `createMemoryHistory()`。

**SSR / SSG / ISR（服务端渲染/静态生成/增量再生）**：SSR 每请求服务端出 HTML；SSG 构建期预生成；ISR 允许构建后按需再生。在 SelfBlog 中：主站是 CSR SPA，但 `SpaController` + `service/site/seo/` 为公开历史路由**服务端直出可读 HTML**（Vue 挂载后替换 `#app` 内的 `.seo-reading`）——一种「SPA + 关键页 SSR 化」的混合策略，专为 SEO 与爬虫可读性。

**State Machine Thinking（状态机思维）**：把异步流程显式建模为有限状态（idle/loading/success/error），消灭布尔标志组合爆炸。在 SelfBlog 中：`api/index.ts` 的 `isRefreshing` + `failedQueue` 就是两态刷新机的手写实现；更复杂的流程值得抽象成显式状态机。

**Vite**：基于原生 ESM 的构建工具，dev 按 need 编译（秒启），prod 用 Rollup 打包。在 SelfBlog 中：Vite 6；dev 端口 3000，`/api` 与 `/uploads` 代理到 `VITE_API_TARGET`（默认 `http://localhost:8825`）；`baseURL` 硬编码 `/api`——`VITE_API_BASE_URL` 实际**没有任何地方读取**，只有 Vite 代理与 `vercel.json` 重写生效。

**Virtual DOM（虚拟 DOM）**：用 JS 对象描述 DOM 树，变更先改虚拟树、diff 后再最小化操作真实 DOM。在 SelfBlog 中：Vue 3 编译器已大量静态提升与 patch flag 优化，虚拟 DOM 开销比 Vue 2 显著降低——不用手写优化，但要避免无 key 大列表。

**Vue Router**：官方路由器：路由表、嵌套、守卫、动态注册。在 SelfBlog 中：`router/modules/` 六个模块（home/post/discover/user/auth/admin）组装路由表，`guards.ts` 统一守卫；**后台路由在运行时按服务端下发的 `adminPath` 动态注册**（`createAdminRoutes`），不在首次导航前装好就会 404。

**Watch（侦听器）**：响应式数据的副作用观察者，`watchEffect` 自动收集依赖。在 SelfBlog 中：`theme.ts` 监听 `matchMedia` 变化同步 `.dark-mode`；URL/查询参数联动页面内容用 watch 而不是在路由守卫里做。

## 组八：构建与工程化

**Bundle Analysis（产物分析）**：可视化 chunk 构成与体积占比，找出该懒加载/该拆分的模块。在 SelfBlog 中：路由全懒加载后，`vite build` 的输出清单是第一手分析材料；第三方大件（markdown 渲染、图表）若进入首屏 chunk 就该拆。

**CHANGELOG（变更日志）**：面向使用者的时间线记录：新增/修复/破坏性变更。仓库规范：顶部追加 `## 未发布 — <标题> (日期)`，正文分「说明 / 修复」要点，并附 **`### 影响文件`** 表格（文件 + 改动类型）。在 SelfBlog 中：根目录 `CHANGELOG.md`，改代码就要更新——「以忘记更文档为耻」是仓库明文荣耻观。

**CI/CD（持续集成/持续部署）**：push 即自动构建测试部署，把「合并恐惧」变成机器例行公事。在 SelfBlog 中：`.github/workflows/ci.yml` 四个 job 全真实执行——backend 全套（H2）、frontend 全闸门、**真实 MySQL 8**（`DB_TYPE=mysql` + `schema-fresh-install.sql`，本机无 MySQL，CI 是唯一真库验证环境）、docker（构建镜像、起容器、探活 `/actuator/health`、grep 首页 HTML 的 `seo-reading`）。

**Code Review（代码评审）**：变更经第二双眼睛核对正确性与风格，兼作知识传播。在 SelfBlog 中：仓库以「开发荣耻」条款替代人工评审流程——「以想业务为耻，以人类确认为荣」「以跳过验证为耻，以主动测试为荣」，agent 协作时同样适用。

**Conventional Commits（约定式提交）**：`type(scope): message` 格式化提交信息，可机器解析生成 CHANGELOG。在 SelfBlog 中：历史提交信息可见 feat/fix 前缀惯例；与 CHANGELOG 的「未发布」区配合形成版本叙事。

**Dead Code Elimination / 死代码清理**：删除无引用代码，降低维护面；先确认「真的无引用」（动态路由、反射引用会骗过静态分析）。在 SelfBlog 中：2026-10-17 前端 legacy 清理删除 47 个文件（两波），`views/auth/Register.vue`、`views/circle/*` 等已删——但**后端对应社区接口与表刻意保留**（admin-only API 面），清理历史记录在 `docs/agents/frontend.md`。

**ESM / CommonJS（ES 模块与 CommonJS）**：浏览器原生 `import/export` 与 Node 传统 `require`。前端构建与 Tree-shaking 都以 ESM 为前提。在 SelfBlog 中：Vite 全 ESM；`build-vercel-proxy.mjs` 用 `.mjs` 后缀显式声明 ESM；依赖里出现 CJS 包时 Vite 会做互操作预打包。

**Formatter（格式化器）**：机器统一代码风格（缩进/引号/换行），消灭风格争论。在 SelfBlog 中：Prettier，`npm run format:check` 是真实仓库级闸门（`.prettierrc` 改为 `endOfLine: "auto"` 后重写过一次历史漂移）——Windows 上 CRLF/LF 之争被 `auto` 策略终结。

**Ghost Dependency（幽灵依赖）**：代码 import 了未在 `package.json` 声明、只因依赖提升而碰巧可用的包——换包管理器或提升策略变化即刻爆炸。在 SelfBlog 中：`npm run test:deployment` 与 CI 干净安装（`npm ci`）会暴露此类问题；新增 import 后跑一次 `type-check` 是最快体检。

**Gitignore Discipline（忽略清单纪律）**：构建产物、本地密钥、编辑器文件不进版本库。在 SelfBlog 中：`backend/.env`（含 `JWT_SECRET`/`ADMIN_PASSWORD`）绝不入库，只有 `.env.example` 模板；`target/`、`dist/`、`node_modules/` 全部忽略。

**Linter（静态检查器）**：不运行代码而找错误与坏味道（未用变量、错误模式）。在 SelfBlog 中：ESLint，`npm run lint:check` 是四大前端闸门之首；规则配置在 flat config（见下条）。

**Monorepo / Polyrepo（单仓多包与多仓）**：SelfBlog 是**一个仓库两个独立工程**，无根构建清单、无 npm workspace——既不是 monorepo workspace 也不是多仓。在 SelfBlog 中：后果是命令必须在 `backend/` 或 `frontend/` 各自目录执行；后端 Maven 的 `frontend-maven-plugin` 是两个工程唯一的构建期握手（所以测试必须带 `-Dskip.npm=true`）。

**package-lock / npm ci（锁定与可复现安装）**：lock 文件钉死依赖树精确版本；`npm ci` 严格按 lock 安装（比 `install` 快且不漂移）。在 SelfBlog 中：CI frontend job 用 `npm ci` + Node 24.15.0；本机 `npm run <script>` 全部失败是 PATH 引号问题（见 Node 前置 PATH 的解法），`npm ci` 本身正常。

**Path Alias（路径别名）**：`@/components/...` 替代相对路径串 `../../..`。在 SelfBlog 中：`tsconfig.json` 的 paths + Vite resolve.alias 双处声明；新增别名要两处同步，否则 type-check 过了构建挂。

**Semantic Versioning（语义化版本）**：`主.次.补` 三段式：主版本破坏兼容、次版本向后兼容新增、补丁只修 bug。在 SelfBlog 中：项目本体 v2.0.26，CHANGELOG 与 README 版本号同步维护；依赖版本则以 manifest 实际为准（文档常滞后）。

**Source Map（源码映射）**：把产物映射回源码的索引文件，报错栈与断点才能定位到 TS/Vue 源。在 SelfBlog 中：dev 构建内联 sourcemap；prod 按需产出——公开部署要不要挂 map，需权衡「可调试」与「暴露源码结构」。

**Static Type Checking（静态类型检查）**：编译期证明类型正确，把运行时 TypeError 提前到开发期。在 SelfBlog 中：TypeScript 6.0.3 + `vue-tsc --noEmit`；`tsconfig` 开 `strict` + **`noUncheckedIndexedAccess`**——索引访问返回 `T | undefined`，`arr[0].foo` 过不了 type-check，要写 `arr[0]?.foo` 或改用 `replace` 而非 `split()[0]`。

**Tree-shaking（摇树优化）**：以 ESM 静态结构为前提，打包时剔除未引用导出。在 SelfBlog 中：只 import 用到的函数（如从 lodash-es/工具模块按名导入）即可受益；`import * as` 会破坏摇树面。

**Version Pinning（版本钉死）**：依赖固定精确版本，规避「同一个 lock 不同结果」。在 SelfBlog 中：`pom.xml` 锁 Spring Boot 3.3.13、MyBatis Plus 3.5.8、jjwt 0.12.6；`package.json` 锁 Vue 3.5.44、Vite 6、TS 6.0.3——教程行文的版本断言以这些 manifest 为准。

**Workspace Discipline（工作区纪律）**：多 agent/多会话并行在同一目录工作时，文件可能随时出现或消失。在 SelfBlog 中：`docs/tutorial/` 下各章与附录由不同会话并行产出，写临时件要带独有前缀、合并后立刻校验存在性再删——「磁盘真值以最后一次验证为准」。

## 组九：测试

**Arrange-Act-Assert（准备-执行-断言）**：测试三段式结构：构造前置状态、执行被测行为、断言结果——一段一职责，读测试像读句子。在 SelfBlog 中：`AuthControllerTest` 等切片测试普遍遵循该结构；复杂前置（登录态、mock 数据）抽成 helper 保持三段清晰。

**Baseline（测试基线）**：当前必须保持的最低通过线，任何改动不得击穿。在 SelfBlog 中：后端 **588 tests / 0 failures**、前端 **43 文件 / 400 tests**，外加 `lint:check`、`format:check`、`type-check`、`test:deployment`(27/27)、`build` 全绿（2026-10-17 口径，以 CI 与最新验证记录为准）。

**Coverage（覆盖率）**：测试触及代码的比例（行/分支）。覆盖率是望不得满分的指标：100% 覆盖不等于 100% 断言。在 SelfBlog 中：仓库不设覆盖率门槛，以「关键链路必须有用例」为纪律——认证、发布、访客互动三条主链路各有专属测试文件。

**Edge Case（边界用例）**：输入域边缘：空串、超长、0、负数、Unicode、并发。在 SelfBlog 中：`normalizeSize` 把分页参数钳到上限 100、`visitorId` 正则长度 8–80、`ADMIN_PASSWORD` ≥12 位——每个阈值都应有越界用例。

**Fake（假实现）**：有真实逻辑的轻量替身（内存数据库、内存缓存），比 mock 更接近真实行为。在 SelfBlog 中：后端测试用 H2 替代真库（`DB_TYPE` 留空走 `src/test/resources/application.yml` + `schema.sql`）；Caffeine 在测试 profile 直接关闭而非替换。

**Fixture（测试夹具）**：可复用的测试前置数据与环境。在 SelfBlog 中：前端 `src/test/setup.ts` 是全局夹具——只桩了 `matchMedia` 与 `$router/$route`；改夹具前先想清楚它影响全部 43 个测试文件。

**Flaky Test（不稳定测试）**：时过时不过的测试，多半依赖真实时间/随机序/共享状态。在 SelfBlog 中：需要逐帧行为时不能把 rAF 桩成同步，要排队后手动 flush；改 `scrollY` 要 `Object.defineProperty` 再派发 `scroll` 事件——两套约定都是为消灭 flaky。

**Integration Test（集成测试）**：多个真实组件协同工作的测试，介于单元与端到端之间。在 SelfBlog 中：后端启动 Spring 上下文带 Security 链的 MVC 测试即此性质；`npm run test:deployment`（27 项）验证 Vercel 代理生成器与 Docker/Compose 契约，是部署层集成测试。

**Mock**：记录调用并按脚本回放的替身，用于隔离依赖与验证交互。在 SelfBlog 中：`@WebMvcTest` 必须 `@MockBean JwtUtils`——否则 `SecurityConfig` 拉起的 `jwtAuthenticationFilter` 找不到 Bean 直接启动失败；再加 `@WithMockUser(roles="admin")` 才返回 200。前端测试对 api 层做 vi.mock。

**MockMvc（MVC 测试工具）**：不启动真服务器、模拟 HTTP 请求打进 DispatcherServlet 的测试工具。在 SelfBlog 中：管理员控制器测试的标准载体；**不要用 `@AutoConfigureMockMvc(addFilters = false)`**——实测它返回 401 而不是绕过鉴权。

**Negative Testing（负例测试）**：主动测试「应该失败」的路径：403/404/校验拒绝/并发冲突。在 SelfBlog 中：方法级 `@PreAuthorize` 在切片里生效（非管理员 403、匿名 403 都有断言）；日志里大量 `[PERF] ... error:` 正是 `PerformanceMonitoringAspect` 记录的**正常负例断言输出**，不是失败。

**Regression Test（回归测试）**：每个修过的 bug 补一个用例，确保永不复发。在 SelfBlog 中：文章发布的唯一约束修复、评论区消毒顺序、游客身份链路等都伴随回归用例沉淀在测试目录——改老 bug 前先找它的回归用例。

**Snapshot Test（快照测试）**：把输出序列化为基线文件、变更时 diff。适合稳定结构，不适合频繁迭代 UI。在 SelfBlog 中：前端未采用快照测试，以显式断言为主——DOM 结构断言更抗重构。

**Stub（桩）**：返回固定值、不记录交互的最简替身。在 SelfBlog 中：前端 `setup.ts` 桩 `matchMedia`（jsdom 未实现）是典型 stub；后端对时间源、外部 API 的固定返回同理。

**Test Double（测试替身）**：dummy/stub/spy/mock/fake 五类替身的统称——dummy 占位、stub 回填、spy 记录、mock 断言交互、fake 带实现。在 SelfBlog 中：`@MockBean`（mock）、`setup.ts` 的 matchMedia（stub）、H2 内存库（fake）在同一个仓库里各司其职。

**Test-Driven Development（TDD，测试驱动开发）**：先写失败测试，再写最小实现令其通过，后重构——红/绿/重构循环。在 SelfBlog 中：仓库荣耻观明文「以跳过验证为耻，以主动测试为荣」；修 bug 的推荐顺序是先复现（写一个失败用例）再修复，让修复自带回归证明。

**Unit Test（单元测试）**：最小可测单元（函数/类）的隔离测试，快、定位准。在 SelfBlog 中：`HtmlSanitizer`、`SafeMarkdown`、`VisitorHashUtils`、`StringMaskUtils` 等纯函数类是理想单测对象——无依赖、无状态、输入输出即全部。

**Vitest（Vite 生态测试器）**：与 Vite 共享转换管线的测试运行器，API 兼容 Jest 且更快。在 SelfBlog 中：`npm run test:run` 即 `vitest run`，43 个文件 400 条用例；jsdom 环境跑 Vue 组件测试，注意 setup 未做自动卸载的约定（见 Fixture/Lifecycle）。

## 组十：部署与运维

**ADR（Architecture Decision Record，架构决策记录）**：记录「为什么这么设计」的时间戳文档：背景/决策/后果。在 SelfBlog 中：`docs/decisions/` 收录——但**当前行为以 CHANGELOG 与验证记录为准**，决策记录可能过时。（历史上的 `docs/superpowers/` 规格与计划目录已于 2026-10-19 清理。）

**Blue-Green Deployment（蓝绿部署）**：新旧两套环境并行，流量瞬时切换，回滚即切回。个人博客用不上全套，但其思想（可快速回滚的发布单元）仍适用。在 SelfBlog 中：单容器部署以「保留旧镜像 tag」实现最朴素回滚；SQLite 数据卷独立挂载，换镜像不丢数据。

**Build Artifact（构建产物）**：构建管线的最终交付物：`target/selfblog.jar`、`frontend/dist/`。产物应可追溯（版本号 + 构建时间），部署只认产物不认源码。在 SelfBlog 中：Maven `finalName=selfblog` 固定产物名；Docker 构建上下文是**仓库根**（`backend/Dockerfile` 在根上下文中找 jar）——`.dockerignore` 已删，构建上下文粒度靠 Dockerfile 路径控制。

**Container（容器）**：以镜像为模板的隔离运行环境，交付「环境本身」而非「环境说明书」。在 SelfBlog 中：`backend/Dockerfile` + 两个 compose 文件（仓库根上下文 + 真实 `selfblog.jar` + 数据/上传/相册卷）；**镜像构建与运行未在本机验证**（无 Docker 守护进程），CI docker job 是唯一已验证环境。

**Deployment Adapter（部署适配器）**：同一份源码面向不同托管平台的产物转换层。在 SelfBlog 中：双适配——Vercel 走 `frontend/vercel.json` 代理生成器（`framework: null` + `build-vercel-proxy.mjs` 产出 Build Output v3 forwarding，转发到 `SELFBLOG_BACKEND_ORIGIN`）；Docker/Compose 走后端镜像。`npm run test:deployment` 同时守住两份契约。

**Docker Compose（容器编排）**：一个 YAML 声明多容器（应用、数据库、反向代理）的本地编排。在 SelfBlog 中：`backend/docker-compose.yml` 挂载 `migration-selfblog.sql` 与数据卷；MySQL 场景 Compose 起库 + 应用双容器——但真机 MySQL 验证只有 CI 环境做过。

**Environment Variable（环境变量）**：运行时注入进程的配置，是 12-Factor App 的第一配置源。在 SelfBlog 中：`JWT_SECRET`（≥32 位）、`ADMIN_PASSWORD`（≥12 位）、`DB_TYPE`、`SITE_BASE_URL`、`SELFBLOG_BACKEND_ORIGIN`；`.env` 由 `DotenvConfig` 读入且**优先级高于系统环境变量**（Properties 语法，值不加引号）。

**Health Check（健康检查）**：探针端点回答「活着吗」（liveness）与「能干活吗」（readiness）。在 SelfBlog 中：`/actuator/health`（actuator starter 提供）；CI docker job 起容器后轮询它直到通过，再 grep 首页 HTML 验证 SEO 直出。

**Logging Discipline（日志纪律）**：结构化、分级、脱敏、可关。在 SelfBlog 中：`logback-spring.xml` 声明日志行为；`StringMaskUtils`/token 指纹防敏感串进日志；`LogUtils` 统一格式；`[PERF]` 慢日志来自 `PerformanceMonitoringAspect`。

**Observability（可观测性）**：日志（离散事件）、指标（聚合数值）、链路追踪（跨服务路径）三大支柱。在 SelfBlog 中：个人项目取最小集——logback 日志 + `[PERF]` 慢方法日志 + `/actuator/health`；没有指标系统，异常排查靠日志回放。

**Proxy Contract（代理契约）**：反向代理与后端之间隐含的约定：路径重写、头透传、超时、体积上限。在 SelfBlog 中：Vite dev 代理 `/api` 与 `/uploads` → `VITE_API_TARGET`；Vercel Build Output v3 forwarding → `SELFBLOG_BACKEND_ORIGIN`——`X-Forwarded-*` 的透传与 `SiteOrigin` 的防伪造直接相关。

**Read Replica / Write Forwarding（读写分离）**：读请求走副本、写走主库的水平扩展手段。在 SelfBlog 中：未使用——单实例 + SQLite/H2/MySQL 单主库，缓存（Caffeine 8 缓存）承担了读扩展职责，这是「先缓存后分库」的顺序观。

**Release Note / Changelog Discipline（发布说明纪律）**：每个可部署产物都要有对应 CHANGELOG 条目与版本号。在 SelfBlog 中：v2.0.26；「未发布」区累积、发版时定稿；影响文件表让回溯 diff 有索引。

**Resource Limits（资源限制）**：容器的 CPU/内存限额防止邻居干扰与 OOM 连锁。在 SelfBlog 中：compose 文件可配限额；SQLite 单写者 + Hikari 池 1 天然串行化写入，内存压力主要来自缓存上限（`maximumSize(50000)` 已设界）。

**Rollback（回滚）**：把系统退回上一个已知良好状态的预案。在 SelfBlog 中：镜像 tag 保留 + SQLite 卷独立 = 退镜像不退数据；`migration-selfblog.sql` 只前进不回退——数据库回滚靠「迁移前备份」，这是不可回避的运维纪律。

**Scheduled Downtime / Zero-Downtime（停机窗口与零停机）**：个人博客可接受短窗口发布；零停机要求滚动/蓝绿 + 健康检查 + 就绪探针。在 SelfBlog 中：单实例 `java -jar` 重启即停机（秒级）；Spring Boot 优雅停机（处理完在途请求再退出）值得开启。

**Single Point of Failure（单点故障）**：故障即全局不可用的组件。在 SelfBlog 中：整个后端就是单点（个人博客的理性取舍）；但关键数据有防线——SQLite 卷备份、`.env` 密钥异机保存、GitHub 托管源码。

**Site Reliability（站点可靠性实践）**：SRE 思想的轻量版：错误预算、容量规划、事后复盘（postmortem）。在 SelfBlog 中：`docs/reviews/` 的各批次验证记录就是「部署前复盘」的本地实践；已知未验证项（真实 Vercel、搜索引擎收录）都明文记录而非假装覆盖。

**TLS Certificate（TLS 证书）**：HTTPS 的身份凭证与加密材料，托管平台自动签发（Let's Encrypt 等）。在 SelfBlog 中：TLS 终结在 Vercel/反代层；后端只跑 HTTP；`SITE_BASE_URL` 必须与真实对外协议/域名一致，否则 canonical/sitemap/feed 全部产出错误 URL。

**Uptime Monitoring（可用性监控）**：外部视角的探活与告警（HTTP 200 + 延迟阈值）。在 SelfBlog 中：未内置——`/actuator/health` 可被任何外部监控服务（UptimeRobot 等）直接订阅，零代码接入。

**Volume（数据卷）**：容器外独立生存的持久化目录，容器销毁数据仍在。在 SelfBlog 中：compose 挂载三类卷——SQLite 数据库文件、上传媒体、相册文件；**备份策略 = 备份卷**，镜像与卷分离是「无状态容器 + 有状态数据」的教科书布局。

**Web Server / Embedded Server（Web 服务器与内嵌服务器）**：Spring Boot 内嵌 Tomcat（`server.port` 默认 8080，本仓库 8825），jar 即服务器，无需外挂 Tomcat。在 SelfBlog 中：`java -jar selfblog.jar` 直接起服务；生产建议置于反向代理之后（TLS、缓存、限流都由代理层补强）。

**12-Factor App（十二要素应用）**：云原生应用十二守则：配置入环境、依赖显式声明、无状态进程、日志事件流等。在 SelfBlog 中：配置（环境变量/.env）、依赖（manifest 锁版本）、日志（stdout）三要素合规；进程无状态（会话在 JWT、状态在库）——基本是按 12-Factor 的方向自然生长的。

---

## 按字母索引（A–Z）

> 仅列英文起始字母，括号内为所属组别（一 Java、二 JVM、三 Spring、四 Web、五 数据库、六 安全、七 前端、八 工程化、九 测试、十 部署）。

- **A**: Allowlist/Denylist（六）· Annotation Processing（一）· AOP（三）· Arrange-Act-Assert（九）· ADR（十）· Atomic Classes（二）· Auto-boxing（一）
- **B**: Baseline（九）· BaseMapper（五）· Batch Processing（五）· Base64（四）· BCrypt（六）· Blue-Green Deployment（十）· Brute Force Attack（六）· Build Artifact（十）· Bundle Analysis（八）· Busy Waiting（二）
- **C**: CAS（二）· Cache-Aside（五）· Cache Avalanche/Breakdown/Penetration（五）· CHANGELOG（八）· CI/CD（八）· CGLIB Proxy（三）· Clickjacking（六）· Code Review（八）· Composition API（七）· Composable（七）· Computed（七）· Conditional Beans（三）· Conventional Commits（八）· Connection Pool（五）· Container（十）· Coverage（九）· Credential Version（六）· CRUD（五）· CSRF（四）· CSP（四）· Cookie Attributes（四）· CORS（四）· Constructor Injection（三）· Context Switch（二）· CopyOnWrite（二）· Classpath Scanning（三）
- **D**: Dead Code Elimination（八）· Defense in Depth（六）· Deployment Adapter（十）· Diff/Patch（七）· Directive（七）· DTO/VO/PO/Entity（五）· Docker Compose（十）· DDL/DML（五）· Domain Separation（六）· Double Submit Cookie（六）· Deadlock（二）· Dependency Injection（三）
- **E**: Edge Case（九）· Encoding/Encryption/Hashing（六）· Environment Variable（十）· ESM/CJS（八）· ETag（四）· Executor Framework（二）· Envelope Pattern（四）
- **F**: Fail-closed（六）· Fake（九）· Flaky Test（九）· Fixture（九）· Formatter（八）· Forward/Reverse Proxy（四）
- **G**: Generics（一）· Ghost Dependency（八）· Gitignore Discipline（八）
- **H**: Happens-Before（二）· Hash（六）· HikariCP（五）· HMR（七）· HMAC（四）· Health Check（十）
- **I**: Idempotency（四）· Immutable（一）· Immutable Publication（二）· Index（五）· Input Validation（六）· Integration Test（九）· IoC（三）· Isolation Level（五）
- **J**: Jackson（三）· JDBC Template（三）· JMM（二）· JUC（二）· JIT（二）· JWT（四）· JWT Blacklist（六）· JWS/JWE（四）
- **K**: KV Table（五）· Keep-Alive（四）
- **L**: Lambda（一）· Lazy Loading Route（七）· Least Privilege（六）· Lifecycle Hooks（七）· Linter（八）· Logic Delete（五）· LRU（五）
- **M**: Memory Leak（二）· Migration Script（五）· MIME Type（四）· Mock（九）· MockMvc（九）· Monitor/Intrinsic Lock（二）· Monorepo/Polyrepo（八）· MyBatis（五）· MyBatis Plus（五）
- **N**: N+1 Query（五）· Negative Testing（九）· Node PATH（见八 workspace/package-lock 相关条目）
- **O**: Observability（十）· ORM（五）· OWASP（六）· Optimistic/Pessimistic Locking（二）· Origin/SOP（四）
- **P**: Package-lock/npm ci（八）· Pagination（五）· Password Hashing（六）· Password Policy（六）· Path Alias（八）· Path Traversal（六）· Pinia（七）· Principal（六）· Profile（三）· Property Source（三）· props/events（七）· Proxy Pattern（三）· Proxy Contract（十）
- **Q**: Query Wrapper（五）
- **R**: Race Condition（二）· RBAC（六）· Reactivity System（七）· Read Replica（十）· Record（一）· Reflection（一）· Regression Test（九）· Release Note（十）· Resource Limits（十）· REST（四）· Rollback（十）· Route Guard（七，见 Vue Router）
- **S**: Salt（六）· Sanitize vs Escape（六）· Scheduled Tasks（三）· Schema（五）· Sealed Class（一）· Secret Management（六）· Semantic Versioning（八）· Single Author Mode（六）· Single Point of Failure（十）· Site Reliability（十）· Slice Testing（三）· Snapshot Test（九）· Snowflake ID（五）· Source Map（八）· SPA（四）· SQL Injection（五）· SSR/SSG/ISR（七）· SSL/TLS Termination（四）· State Machine（七）· Static Type Checking（八）· Stream API（一）· String Handling（一）· Stub（九）· Switch Expression（一）· SFC（七）· Slot（七）
- **T**: Test Double（九）· TDD（九）· Text Block（一）· ThreadLocal（二）· Thread Pool Sizing（二）· Thread Safety（二）· Timing Attack（六）· TLS Certificate（十）· Token Fingerprint（六）· Tree-shaking（八）· TTL（五）· Transaction（五）· Transaction Propagation（三）· Threat Model（六）· Type Handler（五）
- **U**: Unit Test（九）· Uptime Monitoring（十）· Unique Constraint（五）· URI Design（四）
- **V**: Validation（三）· Var（一）· Version Field（五）· Version Pinning（八）· Vite（七）· Virtual DOM（七）· Visitor ID Hashing（六）· Volatile（二）· Vue Router（七）
- **W**: Watch（七）· Web Security Headers（四）· Web Server/Embedded（十）· Work Stealing（二）
- **X**: XSS（四）· XML（四，见 MIME/Accept）
- **Y**: YAML Configuration（三）
- **Z**: Zero-Downtime（十，见 Scheduled Downtime）

---

## 结语：怎么用这本地图

读代码的次序比读代码的努力更重要。建议的完整路径是：先用第一部分 §1.5（config/）与 §1.6（controller/）建立「请求从哪进、行为在哪定」的骨架；再沿五条后端路线中的**认证链路**走一遍——它是所有其他路线的前置；然后按你的实际任务挑路线（改文章 → 发布链路，改互动 → 访客链路，改 SEO → 页面链路），前端三条路线同理，从启动链路开始。

术语表则不是用来通读的。它按「组一 Java → 组十 部署」编排，本意是让读者在调试栈顶到底层之间跳跃时，每个名词都能在 SelfBlog 里找到自己的家。遇到本附录未收的词，优先在 `CLAUDE.md`（架构与约定）、`docs/agents/backend.md`/`frontend.md`（操作级细则）、`docs/decisions/`（为什么）里检索——它们与本附录共同构成这份教程的参考层。

最后一遍提醒：文中所有类名、行数、缓存名、TTL、测试基线均按 v2.0.26 实测。仓库继续演化后，**以 `pom.xml`、`package.json` 与实际目录为最高权威**——这也正是本附录教给你的第一条工程直觉。
