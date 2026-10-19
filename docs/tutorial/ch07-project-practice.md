# 第七章 项目实战：从零理解 SelfBlog 全栈开发

> 前六章我们分别学了 Java 基础、Java 进阶、Spring Boot、Vue 3 与前端工程化。本章是整个教程的「毕业设计」：我们把 SelfBlog v2.0.26 这个真实项目从架构到部署完整拆开，看一个由多用户校园社区改造而来的单人作者博客，是如何把前面学到的所有知识串成一条完整的全栈链路的。读这一章时，强烈建议打开仓库源码对照阅读——本章引用的类名、路径、行为都来自真实代码，而不是教学示例。

## 7.1 项目总览与架构图

### 7.1.1 这个项目是什么

SelfBlog 是一个**单人作者个人博客**：访客可以阅读、搜索、匿名点赞、匿名留言，但注册入口已经关闭，所有写入操作（发文章、管标签、传图片、改站点设置）只属于一位管理员作者。它的前身 `campus_blog` 是一个多用户校园社区（有注册、关注、校友圈、话题、通知、草稿箱、举报……），v2.0 的改造把这个社区「瘦身」成了一个人的书房。这个背景决定了它的很多设计决策，我们会在 7.14 节专门复盘。

技术栈一览：

| 层 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.3.13 |
| 语言 | Java | 21 |
| ORM | MyBatis Plus | 3.5.8 |
| 安全 | Spring Security + JWT (jjwt) | — / 0.12.6 |
| 本地缓存 | Caffeine | — |
| 连接池 | HikariCP | — |
| 前端框架 | Vue | 3.5.44 |
| 构建工具 | Vite | 6 |
| 状态管理 | Pinia | 2.1.7 |
| 语言 | TypeScript | 6.0.3 |
| 单元测试 | Vitest | 4 |
| HTTP 客户端 | Axios | 1.7 |

### 7.1.2 整体架构图

SelfBlog 是一个**单体应用**：一个 Spring Boot JAR 同时提供 REST API 和前端页面（前端构建产物被内嵌进 JAR），这是单人博客最合理的部署形态——一个进程、一个端口、一个数据库文件就能跑。

```
                         ┌───────────────────────────────────┐
                         │        浏览器 / 爬虫 / RSS 阅读器   │
                         └────────┬───────────────┬──────────┘
                                  │               │
              页面请求（根路径）    │               │  REST 请求（/api/**）
                                  ▼               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    Spring Boot 单体应用（selfblog.jar : 8825）             │
│                                                                          │
│  ┌──────────────────────┐        ┌────────────────────────────────────┐ │
│  │  @Controller（无前缀） │        │  @RestController（自动加 /api 前缀）│ │
│  │  SpaController        │        │  BlogPostController /post          │ │
│  │   / /post/{id} /about │        │  SysUserController   /user         │ │
│  │  SeoController        │        │  Admin*Controller    /admin/**     │ │
│  │   /feed.xml /sitemap  │        │  其余 30+ 业务控制器                 │ │
│  └──────────┬───────────┘        └───────────────┬────────────────────┘ │
│             │                                    │                      │
│             │  service/site/seo/                 │  Service 三分：        │
│             │  ReadingPageService                │  门面 / 查询 / 交互     │
│             │  组装「可直接阅读的 HTML」            │  BlogPostService      │
│             │                                    │  ├ PostQueryService   │
│             ▼                                    │  └ PostInteraction    │
│  ┌──────────────────────┐                        └───────────┬──────────┘ │
│  │  HTML 模板 + ETag     │                                    ▼            │
│  │  304 / 200 / 404      │                        ┌──────────────────────┐  │
│  └──────────────────────┘                        │  Mapper（MyBatis Plus)│  │
│                                                  │  手写 SQL 双方言       │  │
│                                                  └──────────┬───────────┘  │
│  横切：JwtAuthenticationFilter → RateLimitInterceptor           │          │
│        Caffeine 8 个具名缓存 → GlobalExceptionHandler            │          │
└──────────────────────────────────────────────────────────────┼──────────┘
                                                               ▼
                                             ┌───────────────────────────┐
                                             │  数据库（三选一，DB_TYPE）   │
                                             │  SQLite（默认单文件）        │
                                             │  H2（测试内存库）           │
                                             │  MySQL 8（生产可选）        │
                                             └───────────────────────────┘
```

### 7.1.3 一条请求的完整链路

以访客打开一篇文章为例，请求从浏览器到数据库要经过这些关卡：

```
浏览器 Vue Router(/post/42)
  → Axios 拦截器（注入 Authorization / X-XSRF-TOKEN / X-Visitor-Id / X-Trace-Id）
    → Vite 代理（开发期）/ Vercel 反代（生产期）
      → Spring Security 过滤器链
        → JwtAuthenticationFilter（有 token 就解析，无 token 当匿名）
          → RateLimitInterceptor（/api/** 按 IP+路径 1 分钟窗口计数）
            → DispatcherServlet（WebMvcConfig 已把 @RestController 路径加 /api 前缀）
              → Controller（取 userId、调门面）
                → 门面 Service（CRUD + 编排）
                  → 查询 Service（@Transactional(readOnly=true) + @Cacheable）
                    → Mapper（手写 SQL 带 is_deleted=0，或 MP 方法）
                      → Hikari → SQLite/MySQL
```

### 7.1.4 为什么这样分层

**为什么 Controller 很薄？** Controller 只做三件事：参数绑定与校验（`@Valid`）、身份提取（`SecurityUtils.getCurrentUserId()`）、把 Service 结果包成 `Result<T>`。业务规则全部下沉，Controller 不写 if-else 业务判断。这样 `@WebMvcTest` 才能只 mock 一个 Service 就测完所有路由行为。

**为什么 Service 要拆成「门面 / 查询 / 交互」三个？** 这是本项目最值得学的结构决策。`BlogPostService`（门面）负责 CRUD 和跨服务编排，`PostQueryService` 只读查询，`PostInteractionService` 管计数副作用。拆开的原因是三者的**事务语义完全不同**：查询要 `readOnly=true` 让数据库优化、还能配合 `@Cacheable`；计数是写操作必须 `rollbackFor=Exception.class` 保证回滚；编排（比如发文章要同时写 post、post_tag、topic 计数）需要可回滚的写事务。如果揉在一个类里，注解只能取「最小公约数」，要么查询白付写事务的代价，要么缓存注解挂错位置。详细拆解见 7.4 节。

**为什么前端 API 层统一走 `src/api/`？** 因为 Axios 拦截器承担了鉴权注入、信封解包、401 刷新重放、错误文案转换四件横切事。业务组件只管 `postApi.getDetail(id)`，完全不用知道 token 放哪、信封长什么样、401 之后会发生什么。

**为什么单体而不是前后端分离部署？** 单人博客的流量撑不起两套运维。单体让 SPA 的 SEO HTML（7.10 节）可以直接由后端按请求组装——这是分离部署做不到（或需要额外服务）的事。

## 7.2 环境搭建与项目启动

### 7.2.1 准备 JDK 21 与 Maven Wrapper

后端要求 **JDK 21**（Boot 3.x 强制 Java 17+，本项目用了 21 的语言特性）。本机如果装了多个 JDK，要让 `JAVA_HOME` 指向 21：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
```

本机 `mvn` 不在 PATH 上，但仓库自带 Maven Wrapper（`backend/mvnw` / `backend/mvnw.cmd`，Maven 3.9.12），直接用即可。

### 7.2.2 【必读】跑后端的正确姿势

`backend/pom.xml` 里配置了 `frontend-maven-plugin`，它会在 Maven 的 `initialize` / `generate-resources` 阶段进入 `../frontend` 执行 `npm ci` + `npm run build`——也就是说，**不带开关地跑一次 `mvn test`，会先把整个前端从零构建一遍，还会改写前端构建产物**。所以后端命令必须永远带这两个参数：

```powershell
cd backend
.\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

PowerShell 里 `-D` 参数要加引号，否则会被解析错。带上开关后，588 个后端测试约 1 分钟跑完（H2 内存库，`DB_TYPE` 留空时自动启用）。

只跑一个测试类的某个方法：

```powershell
.\mvnw.cmd test "-Dtest=BlogPostControllerTest#createPost_Success_ReturnsPostId" "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

### 7.2.3 启动后端

先把 `backend/.env.example` 复制为 `backend/.env`，填两个必填项：

```properties
JWT_SECRET=this-is-a-very-long-random-secret-key-at-least-32-chars
ADMIN_PASSWORD=change-me-to-a-strong-password-12plus
```

注意 `.env` 由 `config/DotenvConfig.java` 用 **Java `Properties` 语法**解析，不是 shell dotenv——**值不要加引号**，引号会成为值的一部分。查找顺序是「工作目录 → 用户主目录」，文件里的值会注册为 JVM property，优先级**高于**系统环境变量。

然后启动：

```powershell
.\mvnw.cmd spring-boot:run "-Dskip.npm=true" "-Dskip.installnodenpm=true"
# 或者打包后运行
.\mvnw.cmd clean package "-DskipTests" "-Dskip.npm=true" "-Dskip.installnodenpm=true"
java -jar target\selfblog.jar
```

后端监听 `SERVER_PORT`（默认 8825），健康检查在 `http://localhost:8825/actuator/health`。首次启动会在 `backend/` 下生成 `campus_blog.db`——SQLite 模式下这个文件就是整个数据库。

### 7.2.4 启动前端

依赖安装（Node 引擎要求 `^22.22.2 || ^24.15.0 || >=26.0.0`，`engine-strict` 开启）：

```powershell
cd frontend
npm ci
```

启动开发服务器：

```powershell
.\node_modules\.bin\vite
```

这里有一个本项目特有的坑值得专门讲：**本机直接 `npm run dev` 会全部失败**，报错是 `'"node"' 不是内部或外部命令`。原因不在仓库，而在系统 PATH 里有一条**带不配对引号**的条目（形如 `C:\Program Files\Java\jdk-25\bin"`），`cmd.exe` 解析 PATH 时把它当成未闭合引号的开头，其后所有条目失效。`npm run` 的脚本最终由 `node_modules/.bin/*.cmd` 垫片执行，垫片结尾调用的正是裸 `node`，于是全军覆没；而 `where node` 依然能找到，因为它走的是注册表 App Paths。

两种应对：

1. **根治**：从系统 PATH 里删掉那条多余引号（推荐，一次性的系统修复）。
2. **绕过**：不经过 `npm run`，直接调用 POSIX 垫片——

```powershell
# 等价于 npm run dev / type-check / lint:check / test:run / build
.\node_modules\.bin\vite
.\node_modules\.bin\vue-tsc --noEmit
.\node_modules\.bin\eslint src
.\node_modules\.bin\vitest run
.\node_modules\.bin\vue-tsc --noEmit; .\node_modules\.bin\vite build
```

开发服务器跑在 3000 端口，`vite.config.ts` 把 `/api` 与 `/uploads` 代理到 `VITE_API_TARGET`（默认 `http://localhost:8825`）。一个很贴心的细节：**只跑前端、不跑后端也能看到完整页面**——`main.ts` 里的 `loadPublicSettings()` 对 `/api/site/public` 的 `fetch` 失败是静默吞掉的，`stores/site.ts` 有一套内置默认值（站点标题「个人博客」、皮肤 `ocean`、主色 `#165DFF`……），所以 `vite` 起来就能验收布局与样式，不被「后端没起」阻塞。

### 7.2.5 验证基线

动手改代码前后，都用这组基线对照（2026-10-17 实测）：

- 后端：588 tests / 0 failures（日志里大量 `[PERF] ... error:` 是 `PerformanceMonitoringAspect` 记录的**正常负例断言**，不是失败）
- 前端：43 个测试文件 / 400 tests 全绿，外加 `lint:check`、`format:check`、`type-check`、`test:deployment`（27/27）、`build` 五道闸门

CI 的四个 job 全部真实执行：backend（H2 全套）、frontend（Node 24.15.0 全闸门）、mysql（真实 MySQL 8 从 `schema-fresh-install.sql` 初始化后跑全套）、docker（仓库根上下文构建镜像 → SQLite 起容器 → 等 `/actuator/health` → grep 服务端首页 HTML 含 `seo-reading`）。

## 7.3 数据库设计剖析

### 7.3.1 三种数据库，一个开关

`DB_TYPE` 环境变量决定数据源，由 `config/db/DatabaseConfig.java` 统一创建（该 Bean 带 `@ConditionalOnProperty("DB_TYPE")`）：

| DB_TYPE | 用途 | 行为 |
|---------|------|------|
| `sqlite`（默认） | 开发 / 单机部署 | `jdbc:sqlite:campus_blog.db`，Hikari **池大小 1**（SQLite 单写者），启动自动执行 `schema-sqlite.sql`（`CREATE TABLE IF NOT EXISTS`，不迁移既有文件） |
| （留空） | 测试 | 走 `src/test/resources/application.yml` + H2 内存库 + `schema.sql` |
| `mysql` | 生产可选 | 由 `schema-fresh-install.sql` 初始化，CI 有真实 MySQL 8 的验证 job |

为什么 SQLite 要把 Hikari 池限制为 1？SQLite 的并发写模型是**全库单写者**，连接池开大了反而会收到 `database is locked` 错误。单人博客的写频率极低（作者发文章、访客计数），池 1 完全够用，还天然串行化了写冲突。这是「按数据库特性裁剪连接池」的教科书案例。

### 7.3.2 两张世界观的表：历史社区表与个人博客表

打开 schema 文件，二十多张表分成气质完全不同的两组：

```sql
-- 第一组：历史社区表（v1 遗产，保留但公开入口大多已下线）
sys_user, blog_post, blog_comment, blog_tag, blog_post_tag,
blog_like, blog_collect, blog_share, blog_follow, blog_notification,
blog_trending, blog_draft, blog_draft_tag, blog_report,
blog_circle_post, blog_circle_comment, blog_topic, blog_message, blog_media ...

-- 第二组：个人博客新增表
site_setting, visitor_event, anonymous_post_like,
guest_comment, photo_album, album_photo
```

新功能都属于第二组：匿名点赞、访客留言、相册、访客统计、站点设置。历史表保留的原因见 7.14 节。

### 7.3.3 KV 表 site_setting：零迁移的站点配置

这是本项目数据库设计里最优雅的一个决定。站点设置（标题、作者简介、皮肤、后台路径 `adminPath`、评论开关……）**没有**设计成一张 30 列的宽表，而是一张 KV 表：

```sql
CREATE TABLE IF NOT EXISTS site_setting (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value TEXT
);
```

要新增一个站点设置项，比如 `footerText`，**不需要改任何 schema**——直接插一行 `('footer_text', '...')` 即可。新增键的全部工作在 `SiteSettingsServiceImpl` 里：登记键名、做值校验（比如 `adminPath` 被 `normalizeAdminPath` 约束为 `[A-Za-z0-9][A-Za-z0-9_-]{1,39}` 并拒绝保留词）。这就是「零 schema 迁移」：配置的形状演化被推迟到应用层，数据库只存不解释。

对比宽表方案：每次加一个设置项都要 `ALTER TABLE ADD COLUMN`，而 SQLite/MySQL/H2 三方言的三份 schema 文件都要同步改。KV 表把这个问题一次消灭。代价是类型安全性靠应用层校验兜底——对「值几乎全是字符串」的站点配置来说，这笔交易非常划算。**配置型数据用 KV、业务型数据用关系表**，这个分界线值得记住。

### 7.3.4 软删除：@TableLogic 与手写 SQL 的陷阱

20 多个实体都有这个字段：

```java
@TableLogic
private Integer isDeleted;
```

配合 `application.yml` 的全局配置：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

效果：MyBatis Plus 生成的 `selectById` 自动追加 `AND is_deleted = 0`，`deleteById` 变成 `UPDATE ... SET is_deleted = 1`。**但手写 XML SQL 不会享受这个待遇**——MP 只改写它自己生成的方法。看 `mapper/BlogPostMapper.xml` 里的真实写法：

```xml
<update id="incrementViewCount">
    UPDATE blog_post
    SET view_count = CASE WHEN view_count &lt; 100000000 THEN view_count + 1 ELSE view_count END
    WHERE id = #{postId} AND is_deleted = 0
</update>
```

注意两个细节：`is_deleted = 0` 是**手写**的；`CASE WHEN view_count < 100000000` 是计数上限防护（防溢出、防异常数据刷爆 int）。项目里所有计数 SQL 都带 `col > 0`（decrement 场景还有 `>= #{count}`）的下限防护，防负数。**教训：引入手写 SQL 时，忘了 `is_deleted=0` 是软删除系统里最经典的 bug 来源**——已删除的文章会"复活"出现在搜索结果里。

### 7.3.5 双方言：databaseId="mysql" / "sqlite"

全文搜索是唯一需要方言分叉的地方：

```xml
<select id="fullTextSearch" resultType="com.selfblog.entity.BlogPost">
    SELECT * FROM blog_post
    <where>
        is_deleted = 0 AND status = 1
        <if test="_databaseId == 'mysql'">
            AND MATCH(title, content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)
        </if>
        <if test="_databaseId == 'sqlite'">
            AND (title LIKE '%' || #{keyword} || '%'
                 OR content LIKE '%' || #{keyword} || '%')
        </if>
    </where>
</select>
```

`_databaseId` 由 `MybatisPlusConfig` 注册的 `VendorDatabaseIdProvider` 根据 JDBC URL 自动判定。这里有一个必须理解的规则：**H2 没有映射 id，只会走无 `databaseId` 的兜底语句**——所以不要把 MySQL 专有语法写进共享语句，否则本地测试全绿、一上 MySQL 就炸（或者反过来）。安全区只有两种写法：双方言各写一条（排序变体按 `create_time DESC`/`view_count DESC`/`like_count DESC` 各配一套），或写一条三种库都兼容的通用 SQL。

### 7.3.6 schema-fresh-install.sql 与 migration-selfblog.sql 的区别

这两个文件的存在感很容易混淆，记住一句话：**全新库用前者，已有库只能用后者**。

- `backend/schema-fresh-install.sql`：包含 `DROP TABLE IF EXISTS`，专为**全新空库**设计。它在 2026-10-17 之前叫 `schema.sql`——改名的原因很直接：一个含删表语句的文件叫这么无辜的名字，被误执行的代价太大（Docker Compose 初始化挂载的也是改名后的文件）。改名是**用命名降低误操作概率**的工程手段。
- `backend/migration-selfblog.sql`：增量迁移脚本，只包含 v1 → v2 的 `ALTER`/`INSERT`，对已有数据安全。

CI 的 `mysql` job 验证了前者：从真实 MySQL 8 空库执行 `schema-fresh-install.sql`，然后跑完整测试套件（`DB_TYPE=mysql`、`SPRING_SQL_INIT_MODE=never`）——这是仓库里唯一真实 MySQL 验证途径（开发机没有 MySQL 和 Docker 守护进程）。

## 7.4 后端一条完整链路：发布文章

现在进入本章的核心。我们跟着「作者发布一篇文章」这个请求，从 HTTP 进入数据库，把每一层代码读完。你会发现「发布文章」远不止 `INSERT` 一条记录。

### 7.4.1 Controller：薄到只剩三件事

```java
// backend/src/main/java/com/selfblog/controller/post/BlogPostController.java
@RestController
@RequestMapping("/post")        // 实际对外是 /api/post（WebMvcConfig 加前缀）
@RequiredArgsConstructor
public class BlogPostController {

    private final BlogPostService blogPostService;

    @RequiresAuth
    @PostMapping
    public Result<Long> createPost(@Valid @RequestBody PostCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        Long postId = blogPostService.createPost(request, userId, isAdmin);
        return Result.success(postId);
    }
}
```

三件事：`@Valid` 触发请求体校验（标题/内容非空、长度限制写在 DTO 上）；`SecurityUtils` 取身份（未登录直接抛 401 的 `BusinessException`）；调用门面并包信封。注意 `createPost` 有 `(request, userId)` 与 `(request, userId, isAdmin)` 两个重载——`isAdmin` 是给「管理员代操作」场景用的，门面里的归属校验会放行管理员。

还有一条容易忽略的规则：控制器源码里**不写 `/api`**。前缀由 `config/web/WebMvcConfig` 统一注入：

```java
// WebMvcConfig.java
@Override
public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        apiPrefix,
        HandlerTypePredicate.forAnnotation(RestController.class));
}
```

只有 `@RestController` 被加前缀，`@Controller`（SpaController、SeoController）不受影响——这是 7.10 节 SEO 能在根路径提供 `/feed.xml` 的前提。测试时也要记住：`@WebMvcTest` 里用 `post("/post")` 而不是 `post("/api/post")`。

### 7.4.2 门面：createPost 的完整编排

```java
// service/post/impl/BlogPostServiceImpl.java
@CacheEvict(cacheNames = {CacheNames.TRENDING_CACHE, CacheNames.STATS_CACHE},
            allEntries = true)
@Transactional(rollbackFor = Exception.class)
public Long createPost(PostCreateRequest request, Long userId, boolean isAdmin) {
    validatePostRequest(request);   // 标题/内容非空、标题≤200 字符、内容≤30 万字符

    BlogPost post = new BlogPost();
    // XSS 防护：富文本字段过白名单清洗，Markdown 走 sanitizeMarkdown
    post.setTitle(htmlSanitizer.sanitizeRichText(request.getTitle()));
    post.setSummary(htmlSanitizer.sanitizeRichText(request.getSummary()));
    post.setContent(htmlSanitizer.sanitizeMarkdown(request.getContent()));
    post.setUserId(userId);
    post.setStatus(PostStatus.PUBLISHED.getValue());   // 发布即上线，没有审核闸门
    post.setViewCount(0);
    post.setLikeCount(0);
    post.setCommentCount(0);
    post.setCollectCount(0);

    List<Long> tagIds = resolveTagIds(request.getTagIds());  // 标签名不存在则创建
    validateTagIds(tagIds);
    post.setTopicIds(request.getTopicIds() != null
            ? JSONUtil.toJsonStr(request.getTopicIds()) : null);

    this.save(post);          // INSERT blog_post
    savePostTags(post.getId(), tagIds);                    // 批量 INSERT blog_post_tag
    topicMapper.incrementPostCount(request.getTopicIds()); // 话题文章数 +1（跨表）
    return post.getId();
}
```

逐个拆解这段代码里的设计：

**① 发布即上线。** `setStatus(PostStatus.PUBLISHED.getValue())`——创建就是 `status=1`，不存在「保存 → 送审 → 过审」的流转。这是单人博客的产品决策：作者是唯一把关人，多一道审核闸门纯属自我折磨。历史上的 `approvePost` 只对 v1 遗留的 `status=0` 数据生效（见 7.14）。

**② 入口清洗。** 标题和摘要按富文本白名单清洗（`sanitizeRichText` 只保留安全标签），Markdown 正文走 `sanitizeMarkdown`。**入库前清洗**意味着无论前端 DOMPurify 做没做（7.9 节），数据库里的东西一定是干净的——纵深防御的第二层。

**③ 缓存清除的范围是取舍出来的。** `@CacheEvict` 清掉 trending/stats（更新文章还要多清 category）：新文章会让「热门榜单」「站点统计」过期，而这两个缓存的 TTL 分别是 5 分钟和 15 分钟——不清的话作者发完文章要等十几分钟才能在统计里看到。为什么不全清？因为每次 `@CacheEvict` 都是一次「缓存雪崩」风险（下一次读请求全部打回数据库），只清「误差肉眼可见」的缓存。

**④ `rollbackFor = Exception.class` 为什么必须写？** Spring 默认只对 `RuntimeException` 和 `Error` 回滚。项目统一用 `BusinessException extends RuntimeException` 所以默认也够，但 MyBatis 的受检异常、第三方库抛的 checked exception 不会触发回滚。写事务一律显式 `rollbackFor` 是本项目强制规范——「看起来多余」的注解买的是一份确定性。

**⑤ 归属校验放在门面。** `updatePost`/`deletePost` 都先查文章再校验 `!Objects.equals(userId, post.getUserId()) && !isAdmin → 403`——这条规则写一次、三个入口（含 admin 侧）复用，而不是散在各 Controller 里。

### 7.4.3 三分结构：门面、查询、交互各管什么

门面实现里能看到明显的分工痕迹——查询方法被**一行委托**出去：

```java
// service/post/impl/BlogPostServiceImpl.java（节选）
@Override
@Transactional(readOnly = true)
public PostDetailResponse getPostDetail(Long id) {
    return postQueryService.getPostDetail(id);   // 一行委托，重新标注只读事务
}
```

三个实现类各司其职：

| 角色 | 类 | 事务注解 | 典型方法 |
|------|-----|---------|---------|
| 门面 Facade | `BlogPostServiceImpl` | 写：`@Transactional(rollbackFor)` | createPost / updatePost / deletePost（编排多表写） |
| 查询 Query | `PostQueryServiceImpl` | `@Transactional(readOnly = true)` + `@Cacheable` | getPostDetail / getPostList / advancedSearch |
| 交互 Interaction | `PostInteractionServiceImpl` | `@Transactional(rollbackFor)` | incrementViewCount / likeCount 等 |

Circle（校友圈）采用同一套拆分（`CircleService` / `CircleQueryService` / `CircleInteractionServiceImpl`），**新增功能要放对位置，不要在门面里复制查询逻辑**。注意门面实现与查询实现**都**继承同一个实体的 `ServiceImpl<XxxMapper, Entity>`——这是 MP 的用法特征。

**查询服务的代表——getPostDetail：**

```java
// service/post/impl/PostQueryServiceImpl.java
@Transactional(readOnly = true)
public PostDetailResponse getPostDetail(Long id) {
    BlogPost post = this.getById(id);
    if (post == null || post.getIsDeleted() == 1) {
        throw new BusinessException(404, "文章不存在");
    }
    Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();  // 游客不抛异常
    boolean isAuthor = currentUserId != null && currentUserId.equals(post.getUserId());
    if (post.getStatus() != PostStatus.PUBLISHED.getValue() && !isAuthor) {
        throw new BusinessException(404, "文章不存在");  // 未发布对非作者伪装成 404
    }
    return assemblePostDetail(post);   // 拼 username/nickname/avatar、tags、topicName
}
```

三个细节都值得学：`getCurrentUserIdOrNull()` 是游客场景专用——详情页要判断「当前人是不是作者」，但游客没有 id，`getCurrentUserId()` 会直接抛 401；**未发布文章对非作者返回 404 而不是 403**，避免向访客泄露「存在一篇隐藏文章」这个事实（信息泄露的经典反例是返回 403 帮攻击者确认资源存在）；`readOnly=true` 不只是文档注释，JDBC 驱动和数据库可以据此做优化（MySQL InnoDB 对只读事务省掉事务 ID 分配）。`PostQueryServiceImpl` 还用 `@Value("${DB_TYPE:mysql}")` 感知方言，并提供了 `getPublishedPostDetail` / `getPublishedMetadata` 这类只取元数据列的轻量查询给 SEO 使用。

**交互服务的代表——incrementViewCount 的防刷设计：**

```java
// service/post/impl/PostInteractionServiceImpl.java
private final ConcurrentHashMap<String, AtomicLong> viewCountCache = new ConcurrentHashMap<>();
private static final long VIEW_COUNT_INTERVAL_MS = 60_000;   // 同 key 60 秒内不重复计数
private static final int MAX_VIEW_COUNT_CACHE_SIZE = 10_000;

@Override
@Transactional(rollbackFor = Exception.class)
public void incrementViewCount(Long postId, String userKey) {
    long now = System.currentTimeMillis();
    if (viewCountCache.size() >= MAX_VIEW_COUNT_CACHE_SIZE) {
        cleanupViewCountCache();   // removeIf 清掉过期项
    }
    String cacheKey = userKey + "-" + postId;
    AtomicLong slot = viewCountCache.computeIfAbsent(cacheKey,
            k -> new AtomicLong(0));
    long lastTime;
    do {
        lastTime = slot.get();
        if (now - lastTime < VIEW_COUNT_INTERVAL_MS) {
            return;                        // 窗口内，静默忽略
        }
    } while (!slot.compareAndSet(lastTime, now));  // CAS 失败 = 有并发竞争，重试
    blogPostMapper.incrementViewCount(postId);     // 只有赢家走到这里
}
```

userKey 在 Controller 层生成：登录用户是 `"user-" + userId`，游客是 `"guest-" + ip + "-" + userAgent.hashCode()`（IP 可伪造，但结合 UA 哈希提高伪造成本）。CAS 自旋保证了并发下同一窗口只有一个请求真正写库——比 `synchronized` 轻量，比数据库唯一约束简单。注意 `MAX_VIEW_COUNT_CACHE_SIZE` 的内存上限意识：无界缓存内存泄漏是这类计数器最常见的死法。Controller 里的调用也讲究「失败不碍事」：

```java
@GetMapping("/{id}")
public Result<PostDetailResponse> getPostDetail(@PathVariable Long id, HttpServletRequest request) {
    String userKey = getUserIdentifier(request);
    try {
        blogPostService.incrementViewCount(id, userKey);
    } catch (Exception e) {
        log.warn("Failed to increment view count for post {}", id, e);  // 计数失败不影响阅读
    }
    return Result.success(blogPostService.getPostDetail(id));
}
```

### 7.4.4 请求完成后的连锁反应

一次 `POST /api/post` 成功后，系统里发生了这些事：`blog_post` 多一行、`blog_post_tag` 多 N 行、`blog_topic.post_count` 加 1、trending/stats/category 三个缓存被清空、`site_setting` 没动。下次有人访问首页，`PostQueryService` 会以缓存未命中为代价重新计算热点列表——这就是写扩散到读的完整闭环。相关的草稿接口（`/post/draft` 系列：`saveDraft`/`getLatestDraft`/`getMyDrafts`）走的是 `blog_draft` 表，与发布链路独立，作者可以先存草稿再正式发布。

## 7.5 统一响应与全局异常处理

### 7.5.1 Result 信封

所有 REST 接口返回同一个信封：

```java
// common/result/Result.java
public class Result<T> {
    private Integer code;      // 200 成功 / 401 未登录 / 403 无权限 / 500 失败
    private String message;
    private T data;
    private Long timestamp;    // 方便前端排查时序问题

    private Result() {}        // 私有构造器，只能走静态工厂

    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> success(String message, T data) { ... }
    public static <T> Result<T> error(Integer code, String message) { ... }
}
```

私有构造器 + 静态工厂强制所有响应走同一条构造路径，`timestamp` 是排查「这个响应是什么时候产生的」的免费福利。分页 data 里统一是 `records/total/size/current/pages` 五件套（`IPage` 结构）。

### 7.5.2 BusinessException：带语义的错误

```java
public class BusinessException extends RuntimeException {
    @Getter
    private final Integer code;

    public BusinessException(String message) { this(BaseErrorCode.FAILURE, message); }
    public BusinessException(Integer code, String message) { super(message); this.code = code; }
    public BusinessException(BaseErrorCode errorCode) { ... }
}
```

业务代码里随处 `throw new BusinessException(403, "此博客不开放访客注册")`，HTTP 语义和业务语义的翻译交给统一的处理器。

### 7.5.3 GlobalExceptionHandler：一张翻译表

```java
// common/exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        HttpStatus status = switch (e.getCode()) {
            case 400, 401, 403, 404, 409, 429, 500 -> HttpStatus.valueOf(e.getCode());
            default -> HttpStatus.BAD_REQUEST;
        };
        LogUtils.logSecurityEvent(...);
        return ResponseEntity.status(status).body(Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)   // @Valid 失败
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(FieldError::getDefaultMessage).orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Result.error(400, msg));
    }
    // 还有：ConstraintViolation 400 / HttpMessageNotReadable 400「请求体解析失败/参数类型不匹配」
    //       MissingServletRequestParameter 400 / NoHandlerFound 404
    //       HttpRequestMethodNotSupported 405
    //       DataIntegrityViolation + DuplicateKey 409「数据已存在，操作冲突」
    //       AccessDeniedException 403 / AuthenticationException 401
    //       MaxUploadSizeExceeded 400 / MethodArgumentTypeMismatch 400（带参数名与期望类型）
    //       IOException：ClientAbortException 静默（客户端提前断开不是错误），否则手写 500 JSON
    //       Exception 兜底 500「服务器内部错误」
}
```

设计思想有三层。**第一层：业务 code 与 HTTP status 双轨**。信封里的 code 是给前端程序判断的（`res.code === 403`），HTTP status 是给浏览器、网关、监控看的（nginx 日志、告警系统只认后者）。`switch` 只映射白名单状态码，未知 code 一律 400——不把内部语义泄露成奇怪的 HTTP 状态。**第二层：异常翻译集中化**。`DuplicateKeyException` → 409「数据已存在」这种翻译只写一次，全项目受益；对比在每个 catch 里各自翻译的错误写法。**第三层：安全日志联动**。401/403 类异常触发 `LogUtils.logSecurityEvent`，登录爆破、越权尝试会留下审计痕迹。

一个隐藏细节：`SecurityConfig` 把 `/auth/register/*` 与 `/user/register` 设为 `permitAll`，正是为了让注册关闭的 403 **业务响应**能走到这个处理器——如果不放行，请求会先被 401 entry point 拦截，访客看到的就是「请先登录」而不是「不开放注册」，语义完全错位。

## 7.6 认证授权全链路

这条链路跨前后端六个环节，是全项目最长的「一次握手」。

### 7.6.1 环节一：登录与 JWT 生成

`POST /api/user/login` 进来后，`SysUserServiceImpl.login()` 走完一套完整的防爆破流程：

```java
// service/auth/impl/SysUserServiceImpl.java（逻辑串写）
public UserLoginResponse login(UserLoginRequest request) {
    SysUser user = (含 @ ? 按邮箱 : 按用户名) 查用户;
    if (user == null) throw new BusinessException(401, "用户名/邮箱或密码错误");
    if (user.getLockUntil() 在未来) throw new BusinessException(403, "登录失败次数过多，请 N 分钟后再试");
    if (密码不匹配) { baseMapper.incrementLoginFailCount(userId, maxFailCount, lockMinutes);  // 原子 SQL
                     throw new BusinessException(401, ...); }
    if (!"admin".equals(user.getRole()))
        throw new BusinessException(403, "此博客仅开放作者后台登录");   // ★ 单作者关键行
    String credentialVersion = jwtUtils.credentialVersionFor(user);
    String token   = jwtUtils.generateToken(user.getId(), username, role, credentialVersion);
    String refresh = jwtUtils.generateRefreshToken(...);
    jwtUtils.registerDeviceToken(userId, token);           // 每用户最多 10 个设备
    return new UserLoginResponse(id, username, ..., token, refresh, role);
}
```

★ 那一行是单作者模式的门闩：即使密码对了，非 admin 角色也进不来。错误信息故意区分「密码错」（401，可重试）与「次数过多/账号禁用」（403，要等待），给用户正确的预期。失败计数用**原子 SQL**（`incrementLoginFailCount` 带锁定条件，affected==0 说明已被锁）而不是「查出来加一再写回」——后者在并发下会漏计。注册侧则全部关闭：`SysUserController.register()` 与 `AuthController` 的两个验证码端点都直接 `throw new BusinessException(403, "此博客不开放访客注册")`，连验证码邮件的发送路径都不留。

`JwtUtils.generateToken` 用的是 jjwt 0.12.6 的新版流式 API（网上老教程的 `setSubject()` 已废弃）：

```java
private String createToken(Long userId, String username, String role,
                           String credentialVersion, long expiration) {
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("userId", userId)
            .claim("username", username)
            .claim("role", role)
            .claim("credentialVersion", credentialVersion)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact();
}
```

三个值得专门讲的设计：

**credentialVersion：改密即下线。** 它是对 `userId + passwordHash` 做域分离 HMAC 的结果（`"selfblog:credential-version:v1\n" + id + "\n" + passwordHash`，以 JWT secret 为 key，HmacSHA256，Base64url 无填充）。密码一改，passwordHash 变，旧 token 的 credentialVersion 立刻对不上数据库现值——**不用黑名单、跨重启有效**地吊销了所有旧会话。这是「把撤销信息编码进 token 本身」的妙招，代价是每次校验要查一次用户表（可接受，用户表已有 `userCache` 缓存）。撤销判定以**数据库里的密码快照**为界：黑名单重启即失效，而 credentialVersion 永远有效。

**设备管理。** `userDeviceTokens` 每个 userId 最多挂 `MAX_DEVICES_PER_USER = 10` 个 token，超出挤掉最旧的（进黑名单）；token 黑名单 `tokenBlacklist` 上限 100_000 条、超限先清过期项。「登录设备管理」「退出其他设备」这两个功能在数据结构上就是每用户一个有界 Set。

**refresh token 隔离。** refresh token 多一个 `type="refresh"` claim 和独立 `jti`（UUID），轮换接口 `rotateRefreshToken` 验证 type 后**先撤销旧 token 再发新的**（保留旧 credentialVersion，防止并发改密把旧会话升级）——检测到旧 refresh 被复用即可判定泄露。解析异常的处理也有讲究：`ExpiredJwtException` → 过期，其他 `JwtException` → 无效（无效 ≠ 过期，两者走不同的客户端流程）。

### 7.6.2 环节二：前端存储（utils/crypto 的编解码）

登录成功后，`stores/user.ts` 把 token 存进 localStorage（勾选「记住我」）或 sessionStorage，但**存之前先编码**：

```ts
// frontend/src/utils/crypto.ts
export function encodeData(data: unknown): string | null {
  try {
    return btoa(unescape(encodeURIComponent(JSON.stringify(data))));
  } catch { return null; }
}
export function decodeData<T>(encoded: string): T | null {
  try {
    return JSON.parse(decodeURIComponent(escape(atob(encoded))));
  } catch { return null; }
}
```

先说清楚：**这不是加密**，btoa 三秒就能反解。它的真实作用是「防直读混淆」——打开 DevTools 一眼扫过去，storage 里躺着的不是明文 `eyJhbGci...`，防的是同事随手复制、浏览器扩展粗暴正则扫描这类低成本泄露。真正的安全边界由 JWT 的服务端签名保证。中间的 `unescape/encodeURIComponent` 组合是为了让 btoa 能处理中文（btoa 只接受 Latin1）。

`stores/user.ts`（Pinia store id 是 `'auth'`，导出别名 `useUserStore`）还做了三件事：`saveToStorage` 按 remember 标志选端并**清空另一端残留**（防止切换记住我时双端各存一份）；`validateToken` 只在本地解 JWT 的 payload 判断 `exp`（`token.split('.')` 三段 → `atob(payload)` → `JSON.parse` → `exp*1000 < Date.now()` 判定，**不发网络请求**，5 分钟内要过期就打日志等下次请求自动刷新）；`detectTokenTheft` 对比内存与 storage 的 token 指纹（`getTokenFingerprint` = 首 20 + 后 20 字符的 btoa），不一致就在请求拦截器里抛 `Token security violation` 并清理状态——token 被复制到另一个 storage 的场景会被察觉。

### 7.6.3 环节三：请求拦截器注入

```ts
// frontend/src/api/index.ts（节选）
const api = axios.create({ baseURL: '/api', timeout: 15000 });

api.interceptors.request.use((config) => {
  const token = getToken();                    // useUserStore 优先，回退 storage 解码
  if (token) config.headers.Authorization = `Bearer ${token}`;
  config.headers['X-XSRF-TOKEN'] = getXsrfToken() || generateXsrfToken();  // 32 字节 hex
  config.headers['X-Visitor-Id'] = getVisitorId();   // 匿名访客身份（7.9 节）
  config.headers['X-Trace-Id']  = generateTraceId(); // crypto.randomUUID，全链路日志串联
  return config;
});
```

每个请求带四件行李。注意 `baseURL` 是**硬编码的 `'/api'`**——代码里没有任何地方读 `VITE_API_BASE_URL`，那个环境变量只是保留位。同源路径的巧妙之处：开发期靠 Vite 代理、生产期靠 JAR 同源自带或 Vercel 反代重写，**前端代码零改动**。

### 7.6.4 环节四：后端过滤器解析

```java
// config/security/JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ... {
        LogUtils.initMdc();   // X-Trace-Id 进 MDC，日志全链路串联
        String token = jwtUtils.extractTokenFromRequest(request);
        if (token != null) {
            if (jwtUtils.isRefreshToken(token)
                    && !request.getRequestURI().endsWith("/user/refresh")) {
                // ★ refresh token 不能访问普通 API：泄露后无法长期滥用
                response.setStatus(401);
                response.getWriter().write(
                    "{\"code\":401,\"message\":\"Refresh token cannot be used for API access\"}");
                LogUtils.logSecurityEvent("REFUSED_REFRESH_TOKEN", ...);
                return;
            }
            if (!jwtUtils.isTokenExpired(token) && !jwtUtils.isTokenRevoked(token)) {
                Long userId = jwtUtils.getUserIdFromToken(token);
                String role = jwtUtils.getRoleFromToken(token);
                UserContext userContext = new UserContext(userId, role);
                var authorities = List.of(new SimpleGrantedAuthority(
                        role != null ? "ROLE_" + role : "ROLE_USER"));
                var authentication = new UsernamePasswordAuthenticationToken(
                        userContext, null, authorities);
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // 解析失败只 log.warn("JWT Authentication failed")，继续放行 → 当匿名请求处理
        }
        try { chain.doFilter(request, response); }
        finally { LogUtils.clearMdc(); }
    }
}
```

三个关键决策：**refresh token 在非刷新端点一律 401**——refresh token 有效期 7 天，一旦泄露危害远大于 access token，从过滤器层直接拒绝可以把它锁死在唯一合法用途上；**解析失败不当错误处理**，而是当匿名继续（公开接口对游客开放，token 坏了不该连累阅读）；**principal 是 `UserContext` 对象**而不是裸 userId（兼容旧版 Long principal），下游 `SecurityUtils` 才能同时拿到 id 和 role。

### 7.6.5 环节五：SecurityUtils 与环节六：@PreAuthorize

```java
// utils/SecurityUtils.java
public static Long getCurrentUserId() {
    var ctx = getCurrentUserContext();          // 匿名 → 抛 401「请先登录」
    return ctx.getUserId();
}
public static Long getCurrentUserIdOrNull() {  // 游客场景：详情页判断是否作者本人
    var ctx = getCurrentUserContextOrNull();
    return ctx != null ? ctx.getUserId() : null;
}
public static boolean isCurrentUserAdmin() { ... }
```

而方法级鉴权由 `@EnableMethodSecurity` 打开：

```java
// /api/admin/** 的控制器上
@PreAuthorize("hasRole('admin')")
public Result<...> someAdminApi(...) { ... }
```

### 7.6.6 兜底：single-author-mode

链条最后一环是安全规则的「断言」，写在 `SecurityConfig` 的授权规则最末尾：

```java
// config/security/SecurityConfig.java（节选）
@Value("${app.single-author-mode:true}") boolean singleAuthorMode;

http.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/error").permitAll()
        .requestMatchers("/auth/register/**", "/user/register").permitAll() // 让业务403可见
        .requestMatchers("/user/login", "/user/refresh", "/user/password/**").permitAll()
        .requestMatchers("/uploads/**", "/actuator/health").permitAll()
        .requestMatchers("/api/admin/**").hasRole("admin")
        .requestMatchers(HttpMethod.GET, "/site/public", "/site/pages/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/visitor/track").permitAll()
        .requestMatchers(HttpMethod.GET, "/post/{id}", "/post/{id}/adjacent",
                "/post/list", "/post/search/**").permitAll()
        .requestMatchers(HttpMethod.PUT, "/post/{id}/view").permitAll()
        // ... tag/**、media/post/**、statistics/community、trending/**、
        //     anonymous-likes/**、guest-comments/**、gallery/** 等
        .anyRequest().access((authentication, ctx) -> {
            if (singleAuthorMode) {
                // ★ 兜底：没被上面白名单放行的任何请求，都必须是管理员
                return new AuthorizationDecision(hasRoleAdmin(authentication));
            }
            return new AuthorizationDecision(isAuthenticated(authentication));
        }))
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

这行 `anyRequest().access(...)` 是**白名单模型的安全底**：`@PreAuthorize` 忘写、新接口忘了配规则，兜底规则默认拒绝。单作者模式下「未明确允许即仅管理员」——这比默认放行 + 逐个收紧的模型安全得多，新增接口时思考成本也最低（默认安全）。同文件里还有 CSRF 的关键注释值得抄进脑海：JWT 走 `Authorization` 头而非 Cookie，浏览器同源策略阻止跨域读取响应，攻击者无法强制浏览器携带该头，**CSRF 攻击面不存在**，所以 `csrf.disable()` 是成立的安全决策；将来若改用 Cookie 认证必须重新开启 CSRF。同一段配置里还有 CORS（拒绝通配符 origin、`allowCredentials(true)`、允许头含 `Authorization/X-XSRF-TOKEN/X-Visitor-Id/X-Trace-Id`）、响应头加固（`frameOptions.deny`、`ReferrerPolicy`、`permissionsPolicy` 全禁）、`SessionCreationPolicy.STATELESS`、以及自写的 401/403 JSON entry point/accessDeniedHandler——安全配置的每一块都值得逐行读一遍。

## 7.7 前端一条完整链路：访客阅读文章

### 7.7.1 从 URL 到组件

访客访问 `/post/42`，Vue Router 匹配 `router/modules/` 里注册的路由，`views/post/PostDetail.vue` 开始挂载。组件做的第一件事不是发请求，而是**乐观渲染骨架**：标题、时间、阅读时长先占位，避免整页白屏。

```vue
<!-- views/post/PostDetail.vue（示意） -->
<script setup lang="ts">
const route = useRoute();
const post = ref<PostDetailResponse | null>(null);
const error = ref('');

onMounted(async () => {
  try {
    const res = await postApi.getPostDetail(Number(route.params.id));
    // ★ 契约：res 就是后端信封 { code, message, data }
    if (res.code === 200) post.value = res.data;
  } catch (e) { error.value = '文章不存在或已删除'; }
  nextTick(() => useMarkdownEnhance(container));  // 代码复制/灯箱/锚点
});
</script>
```

`useMarkdownEnhance` 给正文做客户端增强：代码块复制与折叠、标题锚点、图片灯箱。注意这些是**增强**——正文本身在服务端 HTML（7.10 节）里就是可读的，JS 挂载后 Vue 会整体替换 `#app` 内的 `.seo-reading` 内容。阅读页还有目录侧栏与移动端抽屉（共用 1020px 断点）、`useSeoMeta` 设置 canonical（优先读 `meta[name=selfblog-site-origin]`，本地开发无标记时回落浏览器 origin）。

### 7.7.2 拦截器里的契约陷阱

`api/index.ts` 的响应拦截器是本项目最容易踩的坑：

```ts
api.interceptors.response.use(
  (response) => response.data,   // ★ 返回后端信封本身，不是 AxiosResponse
  (error) => { ... }
);
```

所以调用方写 `res.data`（信封里的业务数据）而不是 `res.data.data`（不存在的两层）。这个设计牺牲了一点点直觉，换来的是业务代码里少一层解包。同文件的错误分支还承担了完整的 401 处理：

```ts
if (error.response?.status === 401 && !error.config._retry) {
  if (error.config.url === '/user/refresh') {
    return handleAuthError();        // 刷新接口本身 401 → 直接登出，防死循环
  }
  if (isRefreshing) {
    return new Promise((resolve) => failedQueue.push({ resolve, config }));  // 排队
  }
  isRefreshing = true;
  try {
    const { data } = await api.post('/user/refresh', null,
        { headers: { Authorization: `Bearer ${refreshToken}` } });
    // 更新 storage（encodeData 编码）+ userStore.updateTokens + 更新指纹
    processQueue(null, data.data.token);   // 队列里的请求换新 token 重放
    error.config.headers.Authorization = `Bearer ${data.data.token}`;
    return api(error.config);
  } catch (e) {
    processQueue(e); return handleAuthError();
  } finally { isRefreshing = false; }
}
```

`isRefreshing + failedQueue` 的排队重放是并发 401 的标准解法：页面同时发 5 个请求都 401，只发一次刷新，5 个请求统一重放。`handleAuthError` 清理 Pinia/localStorage/指纹/XSRF（含 `clearSecurityStorage`），然后跳 `siteStore.adminRoot`（动态后台路径，见下节）。网络类失败另有 `_networkRetryCount` 限次指数延迟重试（`ERR_CANCELED` 不重试），400/403/404/429/500 分别设置 `error.userMessage` 供组件直接展示。

### 7.7.3 启动顺序为什么是承重的

`src/main.ts` 的 57 行代码里藏着三条时序约束：

```ts
// frontend/src/main.ts（顺序即逻辑）
const app = createApp(App);
app.use(createPinia());
// ... 指令、错误处理

await siteStore.loadPublicSettings();      // ① 先知道 adminPath
installAdminRoutes(siteStore.adminPath);   // ② 再注册后台路由
useUserStore(pinia).restoreFromStorage();  // ③ 再恢复登录态
app.use(router);                           // ④ 最后装路由（首次导航此刻发生）
app.mount('#app');
void usePagesStore(pinia).loadManifest();  // ⑤ 可选项，不阻塞启动
```

①→②：后台路径 `adminPath` 是**站点设置**（服务端下发，默认 `admin`），不知道它就注册不了 `/admin/**` 路由，作者输 `/admin` 会 404。③→④ 是最重要的一条：登录态必须在 `use(router)` 之前恢复，否则路由守卫的第一个 `beforeEach` 看到未登录，会把已登录的作者弹回登录页——「刷新一下就被登出」这类诡异 bug 的根源几乎都在这。⑤ 的 `loadManifest` 失败静默：页面配置是增强项，不能阻塞首屏。启动失败也有兜底：整个 bootstrap 包在 try/catch 里，catch 之后**仍然执行 `app.mount('#app')`**，宁可白壳也不给用户留死屏。

### 7.7.4 作者后台链路与动态注册的路由

后台路由不是写死的静态路由，而是一个工厂函数：

```ts
// frontend/src/router/modules/admin.ts
export function createAdminRoutes(adminPath: string): RouteRecordRaw[] {
  const root = `/${adminPath}`;
  const adminMeta = { requiresAuth: true, requiresAdmin: true, adminArea: true };
  return [{
    path: root, name: 'AdminEntry',
    component: () => import('@/views/admin/AdminEntry.vue'),
    children: [
      { path: '',         name: 'AdminDashboard', component: ..., meta: adminMeta },
      { path: 'posts',    name: 'AdminPosts',     component: ..., meta: adminMeta },
      { path: 'write',    name: 'AdminWrite',     component: PostEdit, meta: adminMeta },
      { path: 'edit/:id', ... },
      { path: 'tags', 'visitors', 'comments', 'albums', 'pages', 'pages/:id', 'settings', ... },
    ],
  }];
}
```

`installAdminRoutes` 把它们 `router.addRoute()` 进去，还有一个命名空间保护：如果站点设置的 `adminPath` 恰好是 `pages`，就先注册公开的 `/pages/:slug` 路由占住位置（防后台路径抢占公开命名空间）。路由守卫 `guards.ts` 统一处理 `requiresAuth`/`requiresAdmin`：未登录跳 `adminRoot` 带 `redirect` 查询参数；登录但 token 本地校验失败 → toast「登录已过期」并回登录页；非管理员 → toast「无管理员权限」并回首页；`meta.guest` 的页面对已登录用户直接跳首页。`onError` 单独处理 `ChunkLoadError`（发版后旧 chunk 404 的经典场景）。`views/auth/` 下只剩一条重定向路由：`/login` → `adminRoot`；组件里需要登录时统一 `router.push('/login')`，不要写死后台路径。

后台能成立的安全保障全在后端：`adminPath` 只是**前端的门牌号**（藏在站点设置里，`/robots.txt` 甚至会 `Disallow` 它），后端安全永远只看 `/api/admin/**`——门牌号换了名字，安全的墙一寸都没动。后台的编辑器还有一条纪律值得知道：保存状态依据**成功提交时的完整内容快照**判定，摘要/标签也计入未保存状态，旧请求不得把请求期间的新改动标成已保存；页面编辑/导航写入必须携带 `revision` 乐观锁。

## 7.8 缓存设计与一致性

### 7.8.1 八个具名缓存，八个 TTL

```java
// config/cache/CaffeineCacheConfig.java
@Configuration
@EnableCaching
public class CaffeineCacheConfig {
    public static final String HOT_TAGS_CACHE   = "hotTagsCache";
    public static final String CATEGORY_CACHE   = "categoryCache";
    public static final String USER_CACHE       = "userCache";
    public static final String TRENDING_CACHE   = "trendingCache";
    public static final String STATS_CACHE      = "statsCache";
    public static final String STATUS_CACHE     = "statusCache";
    public static final String SITE_SETTINGS_CACHE = "siteSettingsCache";
    public static final String VISITOR_SUMMARY_CACHE = "visitorSummaryCache";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
            Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(10, MINUTES)
                    .recordStats().build(),
            // ... categoryCache 500 条/30m，userCache 5000 条/10m，
            //     trendingCache 1000 条/5m，statsCache 200 条/15m，
            //     statusCache 10000 条/2m，siteSettingsCache 20 条/10m（无 recordStats），
            //     visitorSummaryCache 10 条/30s
        ));
        return manager;
    }
}
```

TTL 不是随手写的，每个都对应一种「数据能容忍多久过期」的业务判断：

| 缓存 | TTL | 容量 | 为什么是这个数 |
|------|-----|------|----------------|
| `hotTagsCache` | 10m | 1000 | 标签热度按天级变化，10 分钟延迟无感 |
| `categoryCache` | 30m | 500 | 分类几乎不变，最长 TTL 给改动最少的数据；写路径会主动 evict |
| `userCache` | 10m | 5000 | 用户资料被拼进文章详情，读极频繁；但 credentialVersion 校验依赖它，不能太长 |
| `trendingCache` | 5m | 1000 | 热门榜单要「看起来是活的」，5 分钟是新鲜感的下限 |
| `statsCache` | 15m | 200 | 站点统计，误差一刻钟完全可接受 |
| `statusCache` | 2m | 10000 | 文章状态判断，访问量最大的键空间，短 TTL + 大容量 |
| `siteSettingsCache` | 10m | 20 | 键空间极小（KV 表就几十行），但每个请求都可能读 |
| `visitorSummaryCache` | 30s | 10 | 访客大盘，30 秒聚合一次，保住数据库不被实时聚合打爆 |

两个反直觉的细节：容量最大的是 `statusCache`（10000 条）——它的键是「每个文章 id 的状态」，键空间天然大；`siteSettingsCache` 没有 `recordStats()`，因为它太小了，命中率统计没有诊断价值。测试 profile 直接关闭缓存，让单测不受缓存干扰——**缓存是性能优化，不是正确性依赖**。

### 7.8.2 @CacheEvict 的取舍

写路径清缓存的完整决策表：

```java
@CacheEvict(cacheNames = {TRENDING_CACHE, STATS_CACHE}, allEntries = true)   // createPost
@CacheEvict(cacheNames = {TRENDING_CACHE, STATS_CACHE, CATEGORY_CACHE},
            allEntries = true)                                               // updatePost
@CacheEvict(cacheNames = {TRENDING_CACHE, STATS_CACHE}, allEntries = true)   // deletePost
```

三个取舍原则浮出水面。**一是 `allEntries = true` 而不是按 key 清**：热门榜、统计这类聚合缓存无法从单个文章 id 推导受影响的 key，全清比猜便宜——代价是下一次读请求缓存未命中，对单人博客流量完全无感。**二是只清「读者能看见陈旧值」的缓存**：`userCache` 里作者昵称改了 10 分钟才生效，肉眼几乎不可察觉，不值得为它加 evict 链；`visitorSummaryCache` 30 秒自然过期，根本不需要 evict。**三是更新/删除的清空范围随受影响面走**：更新会改分类归属所以多清 `categoryCache`，创建/删除不动分类就不用——evict 链跟着数据流走，而不是「清得越多越安心」。

## 7.9 限流与安全

### 7.9.1 RateLimitInterceptor：固定窗口计数器

```java
// config/web/RateLimitInterceptor.java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    // 1 分钟写入后过期 = 固定窗口；50000 上限防内存膨胀
    private final Cache<String, AtomicInteger> counters = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {
        String path = UrlPathHelper.defaultInstance
                .getLookupPathForRequest(request);   // 与 MVC 查找路径一致，防 URI 编码绕过
        RateLimitProperties.Limit limit = matchLongestPrefix(path);
        if (limit == null) return true;              // 未配置前缀的接口不限流
        String key = getClientIp(request) + ":" + path;
        int count = counters.get(key, k -> new AtomicInteger()).incrementAndGet();
        if (count > limit.getMaxRequests()) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"code\":429,\"message\":\"Too many requests, please try again later\"}");
            return false;
        }
        return true;
    }
}
```

四个防御细节：**用 `getLookupPathForRequest` 而不是 `request.getRequestURI()`**，前者会解码 URI——否则 `%2Fapi%2Fpost` 这种编码变体就能绕过前缀匹配；**最长前缀胜出**（`base.equals(path) || path.startsWith(base + "/")`，取最长匹配），防止一条新路径被同名开头的旧阈值误伤；**计数键是 `ip:path`**，同一 IP 打不同接口各算各的；**`getClientIp` 只在 `isTrustedProxy`（10./192.168./127.0.0.1/::1/172.16-31）成立时才信任 `X-Forwarded-For` 首段与 `X-Real-IP`**——直连场景下 XFF 是请求方随便填的头，信它等于把限流钥匙交给攻击者。

注册方式（WebMvcConfig）体现了关注点分离：日志拦截器 order(1) 挂全路径（排除 /uploads、/actuator、swagger 相关），限流拦截器 order(2) 只挂 `/api/**`——静态资源和 SEO HTML 不限流，否则爬虫一多就把正经访客挡在门外。新增接口若不在已有前缀下，要在 `RateLimitProperties` 补一条阈值。

### 7.9.2 CSRF：为什么可以关，XSRF 头为什么还要带

7.6 节说了 `csrf.disable()` 的成立条件（Bearer 认证无 Cookie 自动携带）。那前端为什么还发 `X-XSRF-TOKEN`？**纵深防御 + 前向兼容**：CORS 白名单明确允许这个头，`generateXsrfToken` 用 `crypto.getRandomValues` 生成 32 字节 hex 存入 storage，每个请求自动回显。如果未来某处引入了 Cookie 会话（比如「记住此设备」），把 Spring Security 的 cookie 版 CSRF 仓库打开即可，前端一行不用改。

### 7.9.3 X-Visitor-Id：无 Cookie 的匿名身份

访客统计与匿名点赞需要区分「同一个人」，但对未登录访客不能种 Cookie（隐私合规敏感点，也污染浏览器状态）。SelfBlog 的方案分两半：

前端：首次访问生成随机 `visitorId`（`crypto.randomUUID().replace(/-/g, '')`，格式约束 `[A-Za-z0-9_-]{8,80}`），存 localStorage，之后每个请求由拦截器带 `X-Visitor-Id` 头。

后端：**从不存原始 visitorId，只存加盐哈希**——`VisitorHashUtils.hash(visitorId, hashSecret)`，其中 `hashSecret` 复用 `JWT_SECRET`（一个部署里少管一个秘密）。同一访客对同一文章的点赞状态由 `postId + visitorHash` 唯一确定（`anonymous_post_like` 表的三态切换：没点过 → 插入；有效点赞存在 → 软删即取消；软删记录存在 → 复活即再点赞，天然幂等，避免了并发重复行问题）。

为什么存哈希不存原文？visitorId 虽是随机数，但长期不变、跨请求可关联——直接存等于给每个访客发了一个**可跨表关联的持久标识符**。加盐哈希后，即使整库泄露也无法还原 visitorId、无法把两张表里的同一访客对上号。信任模型也是清醒的：这个头可以伪造，所以它只用于**聚合统计与匿名交互状态**，不用于任何鉴权或限流决策；点赞 toggle 的并发安全由单机 `synchronized` + SQLite 池大小 1 的部署形态兜底——**不是所有项目都需要分布式锁，先认清部署形态再选工具**。

### 7.9.4 DOMPurify 防 XSS：最后一道防线

XSS 的完整防线有三层。第一层在入库前（7.4.2 的 `sanitizeRichText`/`sanitizeMarkdown`）；第二层是评论这类纯文本输入走 `sanitizePlainText`（标签转义/剥离，**消毒后再校验长度**——`&lt;script&gt;` 转义后会变长，先校验后消毒会让超长脏数据溜进库）；第三层在前端渲染时：

```ts
import DOMPurify from 'dompurify';

export function renderSafeMarkdown(raw: string): string {
  const html = marked.parse(raw) as string;
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'a', 'img',
                   'code', 'pre', 'blockquote', 'ul', 'ol', 'li', 'table', ...],
    FORBID_ATTR: ['style', 'onerror', 'onload'],
  });
}
```

注意 `v-html` 本身不是禁区——禁区是**未经消毒的 `v-html`**。Markdown 渲染器允许内嵌原生 HTML，而 `<img onerror=...>`、`<a href="javascript:...">` 都能穿过 marked，DOMPurify 的白名单把它们挡在挂载之前。三层清洗里任何一层失守，下两层还在。SQL 注入则由 MyBatis 参数化全权负责，`visitorId` 的正则白名单再补一刀——注入类攻击从格式上就不可行。

## 7.10 SEO 与服务端渲染 HTML

### 7.10.1 为什么 SPA 对 SEO 不友好

爬虫（尤其是不执行 JS 的那些）抓到 SPA 的响应是一具空壳：

```html
<div id="app"></div>
<script src="/assets/index-abc123.js"></script>
```

标题、正文、发布时间全都不在 HTML 里——它们要等 JS 下载执行、Axios 请求返回、Vue 挂载后才出现。爬虫等不等、等多久、执行不执行 JS，各家不一且随时变。对一个**以内容为核心竞争力**的博客，这是致命的。正统解法是 SSR 框架（Nuxt/Next），但那意味着重写整个前端、引入双端渲染的复杂度——对一个单体 JAR 项目过重。

### 7.10.2 SelfBlog 的答案：按请求组装的可阅读 HTML

SelfBlog 的方案是：**后端在返回 SPA 壳之前，把真实内容直接渲染进 HTML**。`controller/SpaController.java` 不再统一回退空壳 index.html：

```java
// controller/SpaController.java（@Controller，无 /api 前缀）
@GetMapping({"/",
             "/{path:^(?!api$|assets$|uploads$|actuator$|v3$|swagger-ui$|doc\\.html$)[^\\.]*$}",
             "/{path:...}/**"})
public ResponseEntity<String> readingPage(HttpServletRequest request) {
    SiteOrigin resolvedOrigin = origin.resolve(request);
    SeoModels.ReadingPage page =
        reading.resolve(request.getRequestURI(), request.getQueryString());
    return renderer.render(page, resolvedOrigin);
}
@GetMapping("/index.html")
public ResponseEntity<Void> indexAlias() {
    return ResponseEntity.status(308).location(URI.create("/")).build();  // 防双份收录
}
```

`service/site/seo/ReadingPageService.resolve()` 按路径分发：`/` 渲染首页 9 篇文章列表，`/post/{id}` 渲染文章正文，`/tag/{id}`、`/pages/{slug}`、`/archives`、`/gallery`、`/search` 各有模板；`/login`、`/profile`、`/drafts` 等 `SHELL_PATHS` 返回启动壳并强制 `noindex`（后台与个人页没有 SEO 价值）；未知路径返回真实 404 HTML。产出的 HTML 里有 `.seo-reading` 容器，正文、标题、发布时间全部是**静态文本**——禁用 JavaScript 也能完整阅读。Vue 挂载后整体替换 `#app` 内容，无缝接管交互。CI 的 docker job 里有一条断言：容器起来后 grep 首页 HTML 必须能找到 `seo-reading`——**SEO 契约被自动化测试锁住**。

这个类有一段值得刻在脑子里的注释级教训：**它刻意不声明类级 `@Transactional`**。原因是内部会调用「找不到就抛 404」的查询——如果这些查询共享同一个事务，第一个异常会把事务标记为 rollback-only，最终 404 在提交时变成 `UnexpectedRollbackException`（实测表现为 500）。让每个内层查询用自己的只读事务，404 才能安全地转换成 404。这是「事务传播与异常处理交互」的活教材。同类注释还写明只读承诺：不加阅读量、不记访客、不读管理草稿与相册解锁令牌——SEO 路径对业务零副作用。

缓存策略同样精细：

```java
if (cacheable(page)) {   // status==200 且不是 SHELL
    String etag = "\"" + sha256Hex(body).substring(0, 16) + "\"";
    if (ifNoneMatchMatches(request.getHeader("If-None-Match"), etag)) {
        return ResponseEntity.status(304).build();   // 未变更：304 省带宽
    }
    // 200 + ETag + Cache-Control "public, max-age=0, must-revalidate"
}
```

为什么是 `max-age=0, must-revalidate` 而不是长缓存？因为 HTML 内容随文章编辑随时变化，没有版本号可用；ETag（SHA-256 前 16 字节）让浏览器每次回源校验，未变更请求以 304 结束——**新鲜度与带宽的折中**。状态码哲学：缺失 404、模板缺失 503、错误页 noindex——**不拿 200 包一切**，爬虫才能正确删除已消失页面的索引。

### 7.10.3 SeoController：feed、sitemap、robots

订阅器与爬虫只访问根路径，所以 `SeoController` 必须是 **`@Controller`**（`@RestController` 会被 WebMvcConfig 自动加 `/api` 前缀，订阅器就找不到了）：

```java
@Controller
public class SeoController {
    @GetMapping(value = "/feed.xml", produces = "application/rss+xml")
    public String feed() { ... StringBuilder 拼 RSS 2.0：channel/item/guid/pubDate/category ... }

    @GetMapping("/atom.xml") ...   // Atom 1.0，published/updated（updateTime 兜底 createTime）

    @GetMapping("/sitemap.xml")
    public String sitemapIndex() { return sitemaps.index(base); }        // 索引 + 分片
    @GetMapping("/sitemap-site.xml") ...
    @GetMapping("/sitemap-{kind:[a-z]{2,10}}-{part:\\d{1,5}}.xml")
    public String shard(@PathVariable String kind, @PathVariable String part) { ... }

    @GetMapping(value = "/robots.txt", produces = "text/plain")
    public String robots() {
        return """
            User-agent: *
            Allow: /
            Disallow: /%s
            Disallow: /api/
            Disallow: /search
            Sitemap: %s/sitemap.xml
            """.formatted(adminPath, base);
    }
}
```

数据源是 `PostQueryService.getPublishedMetadata()`——一条只取元数据列（不捞 content 大字段）的轻量查询，最多 100 条按 `createTime` 倒序。三个细节：XML 拼装自带 `escape()`（转义五个 XML 实体并**丢弃 XML 1.0 非法控制字符**，保留 `\t\n\r`——正文里混进控制字符会让整个 feed 解析失败）；sitemap 是索引 + 每片 1000 条的分片结构，为文章量增长预留；`robots.txt` 动态生成，因为 `Sitemap:` 必须是绝对地址（依赖 `SiteOrigin` 解析的 base），而 `Disallow: /{adminPath}` 依赖站点设置的实时值——同时注释里点明：`Disallow` 后台入口只是防止收录，**不是鉴权替代**。`SiteOrigin` 的优先级——**后台「网站设置 → 网站域名」(`siteBaseUrl`) → 环境变量 `SITE_BASE_URL` → 直连请求地址**——配置后，伪造 `Host`/`X-Forwarded-*` 头再也影响不了 canonical、订阅链接和站点地图地址。

## 7.11 测试策略

### 7.11.1 后端切片测试的两个铁律

SelfBlog 后端 588 个测试里，Controller 层大量使用 `@WebMvcTest` 切片。第一个铁律：**必须 `@MockBean JwtUtils`**。

```java
// src/test/java/com/selfblog/controller/BlogPostControllerTest.java
@WebMvcTest(BlogPostController.class)
class BlogPostControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean BlogPostService blogPostService;
    @MockBean JwtUtils jwtUtils;              // ★ 不 mock 就启动失败
    ...
}
```

原因在装配链：`@WebMvcTest` 会加载 `SecurityConfig`，而 `SecurityConfig` 构造过滤器链时注入 `jwtAuthenticationFilter`，后者依赖真实的 `JwtUtils` Bean——切片扫描不到它（`@WebMvcTest` 默认只装 Web 层组件），Spring 启动直接报 `NoSuchBeanDefinitionException`。mock 掉 JwtUtils，过滤器链才能完整装配。

第二个铁律：**不要用 `@AutoConfigureMockMvc(addFilters = false)`**。实测它不是「绕过鉴权」而是让安全过滤链处于半装配状态，请求一律 401——想测业务逻辑反而全被挡住。正确姿势是把 `SecurityConfig` 完整拉起，再用测试上下文或注解提供身份：

```java
@BeforeEach
void setUpSecurityContext() {
    SecurityContextHolder.clearContext();
    var userContext = new UserContext(1L, "admin");        // 或 "user"
    var auth = new UsernamePasswordAuthenticationToken(
            userContext, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
}

@Test
void createPost_Success_ReturnsPostId() throws Exception {
    when(blogPostService.createPost(any(), eq(1L), eq(false))).thenReturn(100L);
    mockMvc.perform(post("/post")          // ★ 无 /api 前缀
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"测试标题","content":"正文内容"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").value(100));
}

@Test
void createPost_NotLoggedIn_Returns401() throws Exception {
    // 不灌安全上下文 → 过滤器解析不到身份 → SecurityUtils 抛 401
    mockMvc.perform(post("/post").contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"t\",\"content\":\"c\"}"))
        .andExpect(status().isUnauthorized());
}
```

四个细节：测试里的路径**不带 `/api`**（前缀是运行时 WebMvcConfig 注入的，MockMvc 走的是控制器映射前的路径）；用真实 `UserContext` 当 principal 灌上下文（等价于 `@WithMockUser(roles = "admin")` 但带着项目自己的身份类型），这样 `SecurityUtils.isCurrentUserAdmin()` 在测试里真实生效；方法级 `@PreAuthorize` 在切片里是**生效的**——非管理员 403、匿名 403 都能直接断言；每个测试前 `SecurityContextHolder.clearContext()` 防上下文串测试。

### 7.11.2 前端 jsdom 的四个陷阱

前端 400 个测试跑在 jsdom 里，`src/test/setup.ts` 只做了最小桩：

```ts
// frontend/src/test/setup.ts（全文 25 行）
import '@testing-library/jest-dom';
config.global.stubs = {};
config.global.mocks = {
  $route: { path: '/', params: {}, query: {} },
  $router: { push: vi.fn(), replace: vi.fn() },
};
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false, media: query, onchange: null,
    addListener: vi.fn(), removeListener: vi.fn(),
    addEventListener: vi.fn(), removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});
```

**陷阱一：setup 没有自动卸载。** 凡是挂载时会注册全局监听器（scroll、resize、matchMedia change）的组件，测试文件必须自己 `enableAutoUnmount(afterEach)`，否则监听器跨测试泄漏，后面的测试莫名其妙失败——而且是顺序依赖的失败，单跑又绿。

**陷阱二：rAF 不能桩成同步。** 滚动显现动画（`vScrollReveal`）依赖 requestAnimationFrame 排队。把 rAF 桩成「立即执行」会让排队语义消失；正确做法是维护一个队列，测试里手动 flush：

```ts
const rafQueue: FrameRequestCallback[] = [];
vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
  rafQueue.push(cb); return rafQueue.length;
});
// 测试中：
rafQueue.forEach((cb) => cb(performance.now())); rafQueue.length = 0;
```

**陷阱三：scrollY 不可直接赋值。** jsdom 的 `window.scrollY` 是 getter，改 `scrollY` 要用 `Object.defineProperty(window, 'scrollY', { value: 800, configurable: true })` 再派发 `scroll` 事件。

**陷阱四：路由测试用真实的 memory history。** `createRouter({ history: createMemoryHistory() })` 配合真实 RouterLink，而不是 `stubs: { RouterLink: true }`——stub 会丢掉 RouterLink 的 slot 内容（链接文字全消失），也表达不了 active 状态。`createMemoryHistory` 让「点击链接 → 路由跳转 → 断言 URL」完整可测。离开守卫这类涉及 RouterView 的测试，必须把组件挂在真实 RouterView 下才能测准。

### 7.11.3 部署契约也是测试

`npm run test:deployment`（node:test，27 项）把部署配置本身当测试对象：Vercel 代理生成器的输出结构、Docker/Compose 文件的源契约（构建上下文、卷挂载、健康检查）都在断言之列。思想：**部署配置和代码一样会腐化，也一样可以用测试钉住**。每次修 bug 先补一个能复现它的测试，再修到绿——588/400 两个基线就是这么维护下来的。

## 7.12 部署

### 7.12.1 Docker Compose：仓库根上下文

SQLite 方案（单人博客首选）：

```yaml
# backend/docker-compose.sqlite.yml
services:
  app:
    build:
      context: ..               # ★ 上下文是仓库根，不是 backend/
      dockerfile: backend/Dockerfile
    env_file: .env
    environment:
      DB_TYPE: sqlite
      DB_URL_SQLITE: jdbc:sqlite:/app/data/campus_blog.db   # 库文件进卷
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "127.0.0.1:8825:8825"
    volumes:
      - sqlite_data:/app/data
      - app_uploads:/app/uploads          # 上传图片
      - app_gallery_media:/app/gallery-media   # 相册媒体
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8825/actuator/health"]
      start_period: 60s
volumes:
  sqlite_data: {}
  app_uploads: {}
  app_gallery_media: {}
```

`context: ..` 是关键决策：镜像里要**同时构建 backend 与内嵌 frontend**（最终产物是内嵌前端产物的单 JAR），上下文必须是仓库根才能同时看到两个目录（`entrypoint.sh` 与独立前端部署因此被删除）。三个卷缺一不可：数据库文件与 WAL/SHM 同目录持久化，两个媒体目录丢了图就没了——**升级镜像不清卷是底线**。使用：

```bash
docker compose -f backend/docker-compose.sqlite.yml up -d --build
```

MySQL 方案（`backend/docker-compose.yml`）多一个 `mysql:8.0` 服务，把 `schema-fresh-install.sql` 挂到 `/docker-entrypoint-initdb.d/01-init.sql:ro`——注意 MySQL 官方镜像的初始化脚本**只在空数据目录首次启动时执行**，已有库请改跑 `migration-selfblog.sql`；`app` 服务 `depends_on: {mysql: {condition: service_healthy}}`，网络 `selfblog-network`。两套方案二选一，不要同时启动。诚实声明：开发机没有 Docker 守护进程，构建与运行由 CI 的 `docker` job 验证（构建镜像 → SQLite 起容器 → 等 `/actuator/health` → grep 服务端首页 HTML 含 `seo-reading`），真实服务器部署前请自行再验证一遍。

### 7.12.2 Vercel：代理生成器，不是第二份前端

`frontend/vercel.json` 全文只有 5 行：

```json
{
  "framework": null,
  "buildCommand": "node scripts/build-vercel-proxy.mjs",
  "installCommand": "node -e \"console.log('Proxy deployment needs only Node built-ins')\""
}
```

这不是「在 Vercel 上部署一份 Vite 前端」，而是一个**代理生成器**：`framework: null` 关掉 Vercel 的 Vite 自动预设（否则它会尝试 `npm run build` 出一份 dist），`build-vercel-proxy.mjs` 只用 Node 内置模块生成 Build Output v3 的路由配置，把**所有路径、所有方法**转发到唯一源站（运行中的 selfblog JAR）。架构含义：世界上只有一份前端（JAR 内嵌的），Vercel 只是一个带 HTTPS 和 CDN 的反向代理——彻底消除了「两份前端版本漂移」这类经典事故。构建时强制校验两个环境变量：

- `SELFBLOG_BACKEND_ORIGIN`：源站 HTTPS origin（JAR 的地址）
- `SITE_BASE_URL`：正式访客 origin（Vercel 域名）

两者相同或缺失直接构建失败（自引用转发会死循环）。Preview 环境自带 `X-Robots-Tag: noindex,nofollow`，预览部署不会被搜索引擎收录。`npm run test:deployment` 的 27 项断言钉住了这整套契约。

### 7.12.3 SITE_BASE_URL 的第二重身份

这个环境变量还出现在后端：它是 `SiteOrigin` 解析站点 origin 的第二优先级（后台设置 → 环境变量 → 直连请求）。把后端放在反向代理后面时必须显式设置，否则按请求 Host 推导的 canonical、RSS 链接、sitemap 地址可能拿到代理的内部地址。一句话总结：**「这个站点对外的正式地址是什么」必须有一处权威配置，其余地方推导出来的都是临时值**。生产 profile 还要记得：Knife4j 在 `prod` 下自动关闭，`SWAGGER_ENABLED` 不要在生产打开。

## 7.13 从零实现一个新功能：阅读时长估算

现在把前 12 节的一切串起来：走一遍给文章加「阅读时长估算」的完整流程。前端其实已有按正文估算的阅读时长（`utils/reading`），我们借这个例子演示「把估算搬到后端持久化」的规范工作流——这顺便回答一个架构问题：估算逻辑放哪？（答案见第 5 步。）

### 第 1 步：设计（先想清楚，再动手）

数据流：发布/更新文章时按内容长度估算 → 存进 `blog_post.reading_minutes` → 详情接口返回 → 前端显示「约 N 分钟」。变更面：三份 schema（SQLite/MySQL/H2）、实体、门面、响应 VO、前端组件、两套测试、CHANGELOG。更大的改动按仓库惯例先写设计再实现：先写 spec（含「已核实的现状」证据表）、再拆带复选框的任务清单，然后动手（这类文档历史上放在 `docs/superpowers/`，该目录已于 2026-10-19 清理）。本例规模适中，直接开工但依然先列清单。动手前按仓库强制规范**完整读一遍 Controller → Service → Mapper → Entity 链路**——确认现有行为，不靠猜。

### 第 2 步：数据库字段

`schema-sqlite.sql` / `schema-fresh-install.sql` 的 `blog_post` 表加：

```sql
reading_minutes INTEGER NOT NULL DEFAULT 0,
```

已有库走 `migration-selfblog.sql` 追加：

```sql
ALTER TABLE blog_post ADD COLUMN reading_minutes INTEGER NOT NULL DEFAULT 0;
```

MySQL 的等价语法写进 `schema-fresh-install.sql` 对应位置，**三个 schema 文件必须同步改**，漏一个 CI 才会告诉你（mysql job 会炸）。

### 第 3 步：实体与 DTO

```java
// entity/BlogPost.java
/** 阅读时长（分钟），按正文长度估算，发布时计算 */
private Integer readingMinutes;

// dto/post/PostCreateRequest.java —— 无需改动：它由服务端计算，不接受客户端传入
```

接受客户端传值是反面教材：伪造 `readingMinutes: 999999` 会污染数据。**可派生的字段一律服务端算**。

### 第 4 步：门面里的计算与持久化

```java
// service/post/impl/BlogPostServiceImpl.java
// 中文约 400 字/分钟，英文约 200 词/分钟，取两者较大者并至少 1 分钟
private int estimateReadingMinutes(String content) {
    if (content == null || content.isBlank()) return 1;
    int cjk = 0, words = 0;
    for (String token : content.split("\\s+")) {
        if (token.matches("[\\x00-\\x7F]+")) words++;
    }
    for (int i = 0; i < content.length(); i++) {
        if (Character.UnicodeScript.of(content.charAt(i))
                == Character.UnicodeScript.HAN) cjk++;
    }
    int byCjk = (int) Math.ceil(cjk / 400.0);
    int byWords = (int) Math.ceil(words / 200.0);
    return Math.max(1, Math.max(byCjk, byWords));
}

// createPost / updatePost 里，save 之前：
post.setReadingMinutes(estimateReadingMinutes(post.getContent()));
```

放门面而不是查询服务：它是**写时派生**的数据，属于「编排」职责；查询服务只负责读出来。放在写路径还有个好处——历史文章靠下次编辑时自然修正，无需跑数据迁移脚本。

### 第 5 步：接口返回

```java
// vo/post/PostDetailResponse.java
private Integer readingMinutes;
```

`assemblePostDetail` 里拷贝即可。**这里回答第 1 步的问题**：估算逻辑放后端是因为它要进 RSS、SEO HTML、未来的 API 消费方——前端算一次只有浏览器看得到。前端已有的本地估算保留作兜底（旧数据 `readingMinutes` 为 0 时用它）。

### 第 6 步：前端展示

```vue
<!-- views/post/PostDetail.vue 的元信息行 -->
<span class="meta-item">
  <Icon name="clock" :size="14" />
  {{ post.readingMinutes ? `约 ${post.readingMinutes} 分钟` : estimateLocal(post.content) }}
</span>
```

注意本仓库的 `noUncheckedIndexedAccess` 严格模式（`tsconfig.json`）：索引访问返回 `T | undefined`，`arr[0].foo` 过不了 type-check，要 `arr[0]?.foo` 或改用 `replace` 之类的写法。

### 第 7 步：测试

```java
// 后端：BlogPostServiceImplTest 中
@Test
void estimateReadingMinutes_ChineseContent_ReturnsByCjkSpeed() {
    String content = "汉".repeat(800);          // 800 汉字 → 2 分钟
    assertEquals(2, invokeEstimate(content));
}
@Test
void estimateReadingMinutes_Blank_ReturnsAtLeastOne() {
    assertEquals(1, invokeEstimate(""));
}
```

```ts
// 前端：PostDetail 测试中，mock postApi.getPostDetail 返回 readingMinutes: 3，
// 断言渲染出「约 3 分钟」；readingMinutes 为 0 时回退本地估算。
```

全绿后跑全套闸门：后端 `mvnw test`（带两个 skip 标志，保持 588+ 全过），前端 `lint:check / format:check / type-check / test:run / build` 五道。任何一项红了都不算完成。

### 第 8 步：文档与 CHANGELOG

按仓库强制规范，在 `CHANGELOG.md` 顶部追加：

```markdown
## 未发布 — 文章阅读时长估算落库 (2026-10-XX)

### 说明
- 发布/更新文章时按正文长度估算阅读时长并持久化到 blog_post.reading_minutes
- 详情接口返回 readingMinutes；前端优先展示服务端值，旧数据回退本地估算
- 无需数据迁移：历史文章在下次编辑时自动补算

### 影响文件
| 文件 | 改动类型 |
|------|---------|
| backend/src/main/java/com/selfblog/entity/BlogPost.java | 修改 |
| backend/src/main/java/com/selfblog/service/post/impl/BlogPostServiceImpl.java | 修改 |
| backend/src/main/resources/schema-sqlite.sql | 修改 |
| backend/src/main/resources/schema-fresh-install.sql | 修改 |
| backend/src/main/resources/migration-selfblog.sql | 修改 |
| backend/src/test/java/.../BlogPostServiceImplTest.java | 修改 |
| frontend/src/views/post/PostDetail.vue | 修改 |
```

受影响的分册 README（backend/frontend 的接口清单）同步更新。至此一个功能走完「schema → 实体 → 服务 → 接口 → 前端 → 测试 → 文档」全环节——**没有一步是可选的**，这正是这个仓库的工作流纪律（「以跳过验证为耻，以主动测试为荣；以忘记更文档为耻，以及时更新为荣」）。

## 7.14 项目演进与架构决策复盘

### 7.14.1 从社区到书房：三个关键取舍

v1 的 `campus_blog` 是标准多用户社区：注册、登录、关注、校友圈、话题、通知、草稿、举报。改造成 v2 单人博客时，每一刀都砍在一个产品判断上：

**取舍一：注册关闭，而不是「限制注册」。** `SysUserController.register()` 的实现是一行 `throw new BusinessException(403, "此博客不开放访客注册")`，`AuthController` 的两个验证码端点同样直接抛 403。为什么不做成「可配置开关」？因为多一个配置就多一个测试矩阵维度（开/关 × 各接口），而单人博客**永远**不会开放注册——YAGNI：为不存在的需求付复杂度是最贵的浪费。注意 `SecurityConfig` 仍把这几个端点 `permitAll`：要让访客收到明确的 403 业务文案，而不是被 401 entry point 拦截后一脸茫然。

**取舍二：发布即上线，不引入待审核。** `createPost` 直接 `status=1`。v1 的审核流（`approvePost`/`rejectPost`/`getReviewList`）只对历史 `status=0` 数据生效。多人社区里审核是内容质量控制；单人博客里作者是唯一作者也是唯一审核员，「自己审自己」是纯粹的仪式。删除一个流转环节，等于删除一整套状态机、两种列表页和一批测试。

**取舍三：历史社区表保留，公开入口下线。** `blog_follow`、`blog_notification`、`blog_circle_*`、`views/user/*` 这些表和后端接口**都没删**——它们保留为 admin-only 的 API 面（`/api/admin/**` 的兜底规则保护着）。删表的风险是：历史数据丢失、迁移脚本复杂化、以及「以为没用了结果某个角落还在引用」的连锁事故。保留的成本只是几张空表。对应地，前端死代码（`views/circle/*`、`views/auth/Register.vue`、`components/home/*`、`Card.vue`、`usePagination` 等 47 个文件）在 2026-10-17 分两波删除——**前端删、后端留**的不对称是刻意的：前端死代码零成本（没路由引用就是纯垃圾），后端接口有数据依赖。

### 7.14.2 改造遗留的架构指纹

这次改造在代码里留下了可辨认的指纹，读代码时可以当「地质层」看：

- `PostDetailResponse` 里同时有 `username/nickname/avatar`（社区时代的多用户展示）和单人博客专属字段
- 访客点赞/留言靠 `guest-{ip}-{ua}` 或哈希后的 visitorId 标识，因为访客没有账号体系了；点赞总数还要把历史登录点赞计数与匿名点赞表**相加聚合**——两套点赞体系并存是改造期项目的典型中间态
- `SHELL_PATHS` 里 `/drafts`、`/collections`、`/following` 这些路由返回启动壳——页面还在注册，公开导航已不可达
- 审核相关接口（`getReviewList`/`approvePost`）还在，但只对历史 `status=0` 数据有意义

理解了这些，你就能读懂「为什么这个项目长这样」而不只是「这个项目长这样」——**架构决策复盘的价值在于把历史约束从代码里解码出来**，避免后来的维护者好心「清理」掉其实有意的保留，或者反过来给死路径续命。

### 7.14.3 给后来者的决策清单

把 SelfBlog 的决策经验抽象成可迁移的原则：

1. **身份决定架构**。单人站点不需要注册、审核、多租户隔离，砍掉它们换来的是安全面缩小（只有一个管理员）、状态机消失（发布即上线）、运维简化（单 JAR 单库）。
2. **白名单优于黑名单**。`anyRequest().access(...)` 的默认拒绝模型让「忘配权限」的后果是「不可用」而非「泄露」。
3. **写时派生优于读时计算**，当派生结果要被多端消费时（RSS、SEO、API）。
4. **KV 表用于配置，宽表用于业务**。site_setting 的零迁移不是银弹，但对「键少、值杂、变更频繁」的配置域是完美匹配。
5. **命名即文档**。`schema-fresh-install.sql` 的改名证明：一个危险文件的危险性能靠名字降低。
6. **保留比删除便宜，直到证明相反**。社区表保留至今没造成任何事故，而误删引发的迁移噩梦真实存在于每个大项目的历史里。
7. **诚实标注未验证项**。Docker、Vercel、MySQL 的运行时验证缺口被明确写进文档，而不是假装全绿——元数据不是证明。

## 7.15 新人上手 48 小时路线图

假设你 clone 了 SelfBlog，要在两天内达到「敢改代码、能提 PR」的水平——这是新成员加入任何项目都要解决的问题，SelfBlog 是一个可复制的样板。路线图按「先跑起来 → 先看主线 → 再动手 → 最后补理论」设计，顺序不能乱。

### 第一阶段：让项目跑起来（第 1～2 小时）

目标不是理解，是**获得一个可反复重置的验证环境**——之后每学一个点都在这里验证。

1. 装 JDK 21（`JAVA_HOME` 指向它），确认 `backend\mvnw.cmd -v` 能报 Maven 3.9.x
2. 复制 `backend/.env.example` 为 `backend/.env`，填 `JWT_SECRET`（≥32 位）与 `ADMIN_PASSWORD`（≥12 位）
3. 跑后端测试：`backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"`——588 个测试全绿，说明环境无恙；**这两个 skip 参数以后每次都要带**，否则 Maven 会顺手重建前端
4. 起后端（`mvnw.cmd spring-boot:run` 带同样双参数），确认 `http://localhost:8825/actuator/health` 返回 UP
5. 起前端：cd frontend 后**直接调 `./node_modules/.bin/vite`**（本机 npm run 可能因 PATH 引号问题全挂，见附录 D），打开 http://localhost:3000

验收标准：博客首页能打开，`/admin` 能登录。卡住就查附录 A 排查手册，不要跳过——**第一天的问题清单就是你对这个项目的第一份理解**。

### 第二阶段：走读两条主线（第 3～6 小时）

不看全部代码，只走两条「全栈线」，每条线从浏览器出发一路点到数据库：

**线一：访客读文章**（第 3～4 小时）
`views/post/PostDetail.vue` → `api/post.ts` →（Axios 拦截器 `api/index.ts`）→ `BlogPostController` → `BlogPostService`（注意一行委托给 QueryService 的门面模式）→ `PostQueryService`（@Cacheable 在这里）→ `BlogPostMapper` → SQLite 文件。配套阅读：本教程第 3 章 §3.7（MVC）与 §3.12（MyBatis Plus）、第 5 章 §5.6（Axios 封装）。

**线二：管理员发文**（第 5～6 小时）
登录页提交 → `AuthController` → JWT 签发 → 前端存 token（`stores/user.ts` + `utils/crypto`）→ 发文表单 → `AdminPostController`（@PreAuthorize）→ 门面 → 库。配套阅读：第 3 章 §3.13（Security+JWT）、第 7 章 §7.4/§7.6。

走读方法：**开 IDEA 的 Ctrl+B 跳转链，手边开着 SQLite 工具直查表**（附录 D §D.4），每个接口都亲眼看到数据怎么变。两条线走完，你对项目的理解已超过 80% 只读文档的人。

### 第三阶段：完成一个端到端小改动（第 7～10 小时）

在真实改动里学会全部协作纪律。选一个「加字段」级别的任务，例如给文章列表加「字数统计」：

1. **先读链路再动手**（仓库铁律）：Post → Controller → Service → Mapper → Entity 完整读一遍
2. 改库：SQLite 手动 `ALTER TABLE` + 三份 schema 同步（`schema-sqlite.sql` / `schema-fresh-install.sql` / `migration-selfblog.sql`），理解为什么同一字段要写三处
3. 改后端：Entity 加字段 → 让 MetaObjectHandler 或业务代码填充 → VO 透出
4. 改前端：`types/` 加类型 → api 层 → 列表组件展示
5. **跑全部门禁**：后端 588 测试、前端 lint/format/type-check/43 文件 400 测试/build
6. **更新文档**：CHANGELOG.md 顶部追加「未发布」条目（含影响文件表）+ 受影响 README

这个练习的价值：一次走完「库 → 后端 → 前端 → 测试 → 文档」全流程，之后接任何任务都是它的变体。

### 第四阶段：建立自己的安全网（第 11～14 小时）

- **跑一遍测试切片配方**：挑一个 `@WebMvcTest`（如 AuthControllerTest）读懂三要素——`@MockBean JwtUtils`、`@WithMockUser(roles="admin")`、路径不带 `/api`；再跑一个 Vitest 组件测试，注意 `enableAutoUnmount(afterEach)`
- **读一份 ADR**（docs/decisions/），理解「为什么这么设计」被记录下来的机制
- **故意弄坏一次**：改坏一个 Controller 再看 GlobalExceptionHandler 怎么兜底、测试怎么红——**知道项目怎么失败，才算知道它怎么工作**
- 通读本教程第 7 章 §7.9（安全清单）与附录 A（错误排查手册）的高频条目

### 第二天下午起：按需深入 + 长期习惯

之后按任务需要补理论（第 1～6 章），但建立三个终身习惯：

1. **改代码前先读链路**，确认现状，绝不猜测
2. **文档与代码同步提交**——CHANGELOG 里那条「影响文件」表格就是项目的心跳记录
3. **凡是「应该能行」的都没验证过**——跑一遍才算数（这条纪律让 SelfBlog 的 CI 四个 job 全部真实执行）

48 小时后你应该能独立接小任务。这份路线图本身也可以迁移到任何 Spring Boot + Vue 项目：换掉目录名，方法论不变。

## 本章总结

本章我们完整走读了一个真实的全栈项目，把前六章的知识点钉在了具体代码上。回顾一遍这条主线：

- **架构**：单体 JAR 同时服务 SPA 与 REST；`@RestController` 自动加 `/api` 前缀，`@Controller`（Spa/Seo）留在根路径——一个注解类型的差异撑起了整个 SEO 方案。
- **分层**：Controller 薄壳、Service 三分（门面编排 / 查询只读缓存 / 交互计数回滚）、Mapper 手写 SQL 自带 `is_deleted=0` 与双方言；`databaseId` 方言隔离 + H2 走兜底语句的规则。
- **契约**：`Result` 信封 + `BusinessException` + `GlobalExceptionHandler` 的集中翻译；前端拦截器返回信封本身，调用方读 `res.data` 而不是 `res.data.data`。
- **安全**：JWT 全链路（生成 → 编码存储 → 拦截注入 → 过滤器解析 → SecurityUtils → @PreAuthorize），credentialVersion 实现改密即下线，refresh token 被锁死在唯一端点，`single-author-mode` 兜底默认拒绝；限流防编码绕过与伪造 XFF，三层清洗防 XSS，visitorId 加盐哈希保隐私。
- **体验与 SEO**：八缓存各按数据新鲜度容忍度定 TTL，evict 链跟着数据流走；服务端组装可阅读 HTML + ETag 协商缓存 + 真实状态码；feed/sitemap/robots 齐备且以 `SiteOrigin` 为唯一地址权威。
- **质量与交付**：切片测试两铁律（@MockBean JwtUtils、禁用 addFilters=false），jsdom 四陷阱（自动卸载、rAF 队列、scrollY defineProperty、真实 memory history），部署配置进测试；Docker 仓库根上下文 + 三卷持久化，Vercel 只做代理生成器。
- **工作流**：新功能走完 schema → 实体 → 服务 → 接口 → 前端 → 测试 → CHANGELOG 全环节，无一步可选。

更重要的是 7.14 节的方法论：**读懂一个项目，要读出它的历史与取舍**。SelfBlog 的每一处「奇怪」设计——关闭的注册接口、保留的社区表、刻意不加的事务注解、故意不改的宽容计数——都是一次明确的产品决策或踩坑教训的化石。愿你接下来的每一次动手改代码前，都先问一句「这里为什么是这样」。

## 参考资源

- **仓库内文档**：`CLAUDE.md` 与 `AGENTS.md`（构建命令与架构约定的权威）、`docs/agents/backend.md` / `docs/agents/frontend.md`（分册深潜笔记）、`docs/decisions/`（ADR 架构决策记录）、`docs/reviews/`（真实验证记录）、`CHANGELOG.md`（每次变更的证据链）
- **官方文档**：[Spring Boot 3.3 Reference](https://docs.spring.io/spring-boot/index.html)、[Spring Security Architecture](https://docs.spring.io/spring-security/reference/)、[MyBatis Plus 官方文档](https://baomidou.com)、[jjwt 0.12.x README](https://github.com/jwtk/jjwt)（新版流式 API 的唯一权威）、[Vue 3 文档](https://cn.vuejs.org)、[Pinia](https://pinia.vuejs.org)、[Vitest](https://vitest.dev)、[Caffeine](https://github.com/ben-manes/caffeine)、[DOMPurify](https://github.com/cure53/DOMPurify)
- **规范与标准**：[RSS 2.0](https://rssboard.org/rss-specification)、Atom RFC 4287、[Sitemaps 协议](https://www.sitemaps.org)、robots.txt RFC 9309、[Vercel Build Output v3](https://vercel.com/docs/build-output-api/v3)（vercel.json 代理生成器的底层协议）
- **安全背景**：[OWASP Top 10](https://owasp.org/www-project-top-ten/)、[OWASP Input Validation Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)
- **本教程前序章节**：第三章 Spring Boot 框架（Security 与事务的原理基础）、第五章 Vue 3（响应式与组合式 API）、第六章 前端工程化（Vite/TS/Vitest 工具链细节）
