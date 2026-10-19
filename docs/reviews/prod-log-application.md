# 生产日志取证：application.log / application-error.log（2026-10-17）

> 取证范围：`C:\Users\Zora\Downloads\application.log`、`C:\Users\Zora\Downloads\application-error.log`。
> 交叉旁证（不属本次主取证范围，仅用于确证请求路径）：`request.log`、`performance.log`、`audit.log`。
> 本报告为**只读取证**：未修改 backend/frontend 任何代码或配置；唯一写入的文件是本文件。
> 每条结论均附原始日志行原文（时间戳 + logger 名），并标注「确证」（日志原文直接支持）或「推测」（由日志 + 源码/配置推导，尚缺运行时确认）。

---

## 1. 取证方法与数据规模

| 项 | 值 | 依据 |
| --- | --- | --- |
| application.log | 571 行 / 66342 字节 | `Get-Content -Encoding UTF8` 计数（确证） |
| application.log 时间跨度 | 2026-10-17 07:35:29.805 → 15:54:52.727 | 首行 L1、末行 L571（确证） |
| application-error.log | 442 行 / 49761 字节 | 同上（确证） |
| application-error.log 时间跨度 | 2026-10-17 08:56:58.287 → 12:10:01.898（末行 L442 为 `... 137 common frames omitted`） | L1、L291-442（确证） |
| error log 与主日志的关系 | error log 是主日志的**真子集**，且三段**逐行等长**：L1-138（138 行）= 主日志 L88-L225（138 行）；L139-290（152 行）= 主日志 L267-L418（152 行）；L291-442（152 行）= 主日志 L419-L570（152 行）。138+152+152 = 442 = error log 总行数（完全吻合） | 逐段比对 + 行数校验（确证） |
| 日志格式 | 4 段方括号 `[thread][traceId][userId][clientIp]` | 与 `backend/src/main/resources/logback-spring.xml:10-11` 的 `FILE_LOG_PATTERN` 一致（确证） |
| 该实例身份 | `SelfBlogApplication v2.0.26 using Java 25.0.3 with PID 6 (/app/selfblog.jar started by root in /app)` | 主日志 L1（确证） |

**日志格式原文（L1）**

```
2026-10-17 07:35:29.805 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication - Starting SelfBlogApplication v2.0.26 using Java 25.0.3 with PID 6 (/app/selfblog.jar started by root in /app)
```

**级别分布（确证，grep 计数）**：主日志中 `ERROR` 级仅 **3 行**（L88 / L267 / L419）；`WARN` 级仅 **2 行**（L87 / L240）；其余全部为 INFO。error log 中 `ERROR` 级同样只有 3 行（L1 / L139 / L291），另有 2 行 `Caused by`（L280 / L432）。

**中文编码**：日志文件为 UTF-8，本次用 `read` / `grep` 工具直读，中文（如「文件内容与扩展名不匹配，请上传真实文件」）**显示正常，未出现乱码**（确证）。

---

## 2. 异常聚合表（类型 × 消息指纹）

| # | 异常类型 | 消息指纹 | 计数 | 级别 | 主日志行 | error log 行 | 判定 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ① | `java.lang.IllegalArgumentException`（Tomcat 侧） | `Invalid character found in method name [...] . HTTP method names must be tokens` | 4 | INFO | L70、L226、L241、L254 | 无 | **噪声**（端口扫描/TLS 误连，非应用故障） |
| ② | `org.springframework.web.servlet.resource.NoResourceFoundException` | `No static resource favicon.ico.` | 1 | ERROR | L88-L225 | L1-L138 | **真实缺陷**（应为 404 却 500） |
| ③ | `org.springframework.web.multipart.MultipartException` → `FileCountLimitExceededException` | `Failed to parse multipart servlet request` / `Caused by: ... FileCountLimitExceededException: attachment` | 2 | ERROR | L267-L418、L419-L570 | L139-L290、L291-L442 | **真实缺陷**（应为 400 却 500） |
| ④ | `BusinessException`（业务校验负例） | `[EXCEPTION] Business: code=400, message=文件内容与扩展名不匹配，请上传真实文件` | 1 | WARN | L240 | 无 | **噪声**（HTTP 400 正确） |
| ⑤ | `HttpRequestMethodNotSupportedException` | `[EXCEPTION] MethodNotAllowed: POST not supported` | 1 | WARN | L87 | 无 | **噪声**（HTTP 405 正确） |
| ⑥ | `[PERF] ... error:`（PerformanceMonitoringAspect 负例断言） | `[PERF] SERVICE SiteAssetServiceImpl.store execution time: 32ms, error: 文件内容与扩展名不匹配，请上传真实文件` | 1 组（2 行） | ERROR（写在 performance.log） | **0 命中** | **0 命中** | **噪声**（业务负例；不进主日志） |

> ⑥ 的计数依据：`performance.log:7-8`（12:06:54.956/960）。**确证它不写入主日志**：对 application.log / application-error.log grep `PerformanceMonitoringAspect` 与 `[PERF]` 均为 0 命中。此点与 `CLAUDE.md`「日志里大量 `[PERF] ... error:` 是 `PerformanceMonitoringAspect` 记录的正常负例断言，不是失败」的记载一致。
> **确证无第 7 类异常**：对主日志 grep `FAILED|BeanCreation|UnsatisfiedDependency|BeanDefinition|NoSuchBean|ApplicationContextException|IllegalStateException`，唯一命中是 ③ 的 `Failed to parse multipart servlet request`（L268/L420）；`BeanCreation`/`UnsatisfiedDependency`/`NoSuchBean`/`ApplicationContextException`/`IllegalStateException`/`Application run failed` 均为 **0 命中** → **启动期无任何 Bean/配置/数据库异常**。另 `BindException|Address already in use|PortInUse|WebServerException|Flyway|Migration` 亦 **0 命中**（见 §4.1）。

---

## 3. 异常明细：栈顶 5 帧 + 触发请求路径 / 线程

### 3.1 ② `NoResourceFoundException: No static resource favicon.ico.`（真实缺陷）

**头部日志行原文（主日志 L88，error log L1）**

```
2026-10-17 08:56:58.287 [http-nio-8888-exec-9] [b0bbfa2b45f34738] [] [185.180.141.5] ERROR c.selfblog.common.exception.GlobalExceptionHandler - Unexpected error: 
org.springframework.web.servlet.resource.NoResourceFoundException: No static resource favicon.ico.
```

**栈顶 5 帧（主日志 L89-L93，确证）**

```
org.springframework.web.servlet.resource.NoResourceFoundException: No static resource favicon.ico.
	at org.springframework.web.servlet.resource.ResourceHttpRequestHandler.handleRequest(ResourceHttpRequestHandler.java:586)
	at org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter.handle(HttpRequestHandlerAdapter.java:52)
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089)
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:979)
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1014)
```

**触发线程与请求路径（确证）**：线程 `http-nio-8888-exec-9`，请求 `GET /favicon.ico`，客户端 IP `185.180.141.5`，HTTP 状态 **500**。
旁证原文（`request.log:225-226`）：

```
2026-10-17 08:56:58.256 [http-nio-8888-exec-9] [b0bbfa2b45f34738] [] [185.180.141.5] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] GET /favicon.ico  from IP=185.180.141.5 UA=Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Safari/537.36
2026-10-17 08:56:58.257 [http-nio-8888-exec-9] [b0bbfa2b45f34738] [] [185.180.141.5] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET /favicon.ico status=500 duration=54ms clientIp=185.180.141.5
```

**根因（确证）**：`GlobalExceptionHandler` 共 21 个 `@ExceptionHandler`，**没有** `NoResourceFoundException` 的处理器（只注册了 `NoHandlerFoundException`，而 Spring 6.1 起静态资源 404 已由 `NoResourceFoundException` 承载），因此落入兜底分支：

```
backend/src/main/java/com/selfblog/common/exception/GlobalExceptionHandler.java:259-263  Exception → log.error("Unexpected error: ", e) + ResponseEntity.status(500).body(Result.error("服务器内部错误"))
backend/src/main/java/com/selfblog/common/exception/GlobalExceptionHandler.java:103-107  NoHandlerFoundException → log.warn("[EXCEPTION] NotFound: {}", e.getRequestURL()) + 404   ← 未被触发
```

**资源侧确证**：glob `**/favicon*` 只命中 `backend/target/classes/static/favicon.svg`、`frontend/dist/favicon.svg`、`frontend/public/favicon.svg`，**没有 `favicon.ico`**；而 `backend/src/main/java/com/selfblog/config/security/SecurityConfig.java:189-191` 明确放行 `"/favicon.ico"`。→ 浏览器直取 `/favicon.ico` 必然落到静态资源解析失败。**这是「客户端错误被记成 ERROR + 返回 500」的语义错误**。

### 3.2 ③ 相册上传 multipart 解析失败（真实缺陷，2 次）

**头部日志行原文（主日志 L267、L419；error log L139、L291）**

```
2026-10-17 12:09:49.691 [http-nio-8888-exec-1] [ee608e561c0849049ffaaff452c2d182] [1] [82.153.135.59] ERROR c.selfblog.common.exception.GlobalExceptionHandler - Unexpected error: 
org.springframework.web.multipart.MultipartException: Failed to parse multipart servlet request

2026-10-17 12:10:01.898 [http-nio-8888-exec-6] [2a3a0968fac840a2a690ffcc3bd9d639] [1] [82.153.135.59] ERROR c.selfblog.common.exception.GlobalExceptionHandler - Unexpected error: 
org.springframework.web.multipart.MultipartException: Failed to parse multipart servlet request
```

**栈顶 5 帧（主日志 L268-L272；L420-L424 与之逐字一致，确证）**

```
org.springframework.web.multipart.MultipartException: Failed to parse multipart servlet request
	at org.springframework.web.multipart.support.StandardMultipartHttpServletRequest.handleParseFailure(StandardMultipartHttpServletRequest.java:132)
	at org.springframework.web.multipart.support.StandardMultipartHttpServletRequest.parseRequest(StandardMultipartHttpServletRequest.java:111)
	at org.springframework.web.multipart.support.StandardMultipartHttpServletRequest.<init>(StandardMultipartHttpServletRequest.java:87)
	at org.springframework.web.multipart.support.StandardServletMultipartResolver.resolveMultipart(StandardServletMultipartResolver.java:112)
	at org.springframework.web.servlet.DispatcherServlet.checkMultipart(DispatcherServlet.java:1228)
```

**根因帧（主日志 L408-L409；L560 同，确证）**

```
Caused by: org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException: attachment
	at org.apache.tomcat.util.http.fileupload.FileUploadBase.parseRequest(FileUploadBase.java:459)
```

完整根因链（主日志 L409-L418 / L561-L570，确证）：`FileUploadBase.parseRequest(459)` → `Request.parseParts(Request.java:2650)` → `Request.getParts(Request.java:2536)` → `RequestFacade.getParts(RequestFacade.java:773)` → 4 层 `HttpServletRequestWrapper.getParts(HttpServletRequestWrapper.java:315)` → `StandardMultipartHttpServletRequest.parseRequest(94)` → `... 137 common frames omitted`（error log L442）。

**触发线程（确证）**：`http-nio-8888-exec-1`（12:09:49.691）与 `http-nio-8888-exec-6`（12:10:01.898）；两次均为已登录管理员 `[1]`、同一 IP `82.153.135.59`。

**触发请求路径（推测，高置信）**：`request.log` 中以 traceId `ee608e561c0849049ffaaff452c2d182` / `2a3a0968fac840a2a690ffcc3bd9d639` 搜索**均 0 命中**，12:09:49 与 12:10:01 两时刻在 request.log 也**无任何 REQUEST/RESPONSE 行**。原因（确证时序）：multipart 解析发生在 `DispatcherServlet.checkMultipart`（L273/L425 帧），早于 `HandlerInterceptor.preHandle`，请求在进入 `HttpRequestLoggingInterceptor` 前即抛异常，故拦截器完全未记录。因此路径只能由旁证 + 源码推断：

- 旁证（`request.log:589-594`，确证）：12:09:08.511 `GET /api/admin/albums status=200`、12:09:21.903 `POST /api/admin/albums status=200`、12:09:23.557 `GET /api/admin/albums status=200` —— 管理员正在相册后台连续操作；12:10:04 起又加载 `/assets/Gallery-*.js`。
- 端点侧（确证）：`backend/src/main/java/com/selfblog/controller/admin/AdminPhotoAlbumController.java:51-52` 为 `@PostMapping("/{albumId}/photos") upload(@PathVariable Long albumId, @RequestParam("files") MultipartFile[] files)`，**无 `@Size` 上限**；对照 `backend/src/main/java/com/selfblog/controller/content/MediaController.java:100-102` 有 `@Size(min = 1, max = 10, message = "请选择1-10个文件")`。
- 前端侧（确证）：`frontend/src/views/admin/Albums.vue:174-187` `const files = Array.from(input.files || []); if (!files.length) return; await galleryApi.uploadPhotos(album.id, files)`，**未限制文件个数**；`frontend/src/api/gallery.ts:55-61` 把每个文件 append 为 `files` 字段。
- 配置侧（确证）：grep `backend/src/main/resources` 无 `max-file-count` / `maxPartCount` 覆盖，仅 `application.yml:48-49` 的 `max-file-size` / `max-request-size`。
- Tomcat 默认值（确证）：反编译 `tomcat-embed-core-10.1.42.jar` 的 `org.apache.catalina.connector.Connector` 构造器，`bipush 10 → putfield maxPartCount`；`Request.parseParts` 取 `Connector.getMaxPartCount()` 后 `FileUpload.setFileCountMax(...)`。

→ **推测（高置信）**：管理员在相册页一次选择 **超过 10 张**照片，超过 Tomcat 默认 `maxFileCount=10`，被 `FileCountLimitExceededException` 拒绝。

**次要根因（确证）**：`GlobalExceptionHandler` **没有** `MultipartException` / `FileCountLimitExceededException` 处理器 → 落入 `Exception` 兜底（:259-263），把本应 400 的客户端错误打成 ERROR 并返回 500。对照 `MaxUploadSizeExceededException`（:219-221）**有**专门处理器且 `log.warn`。

### 3.3 ① `Invalid character found in method name`（噪声，4 次）

**日志行原文（主日志 L70-L72；L226、L241、L254 同型，确证）**

```
2026-10-17 08:03:13.814 [http-nio-8888-exec-1] [] [] [] INFO  org.apache.coyote.http11.Http11Processor - Error parsing HTTP request header
 Note: further occurrences of HTTP request parsing errors will be logged at DEBUG level.
java.lang.IllegalArgumentException: Invalid character found in method name [11110xf80x000x000x000x040x000x000x010x1b0x090x000x000x020x010x000x000xe80x000x000x00admin0x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x00queryBasicCfg0x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x000x00<?xml ]. HTTP method names must be tokens
	at org.apache.coyote.http11.Http11InputBuffer.parseRequestLine(Http11InputBuffer.java:403)
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:271)
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63)
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:903)
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1769)
```

**4 次出现（确证）**：L70 `08:03:13.814 [http-nio-8888-exec-1]`、L226 `09:10:15.922 [http-nio-8888-exec-6]`、L241 `12:08:18.024 [http-nio-8888-exec-13]`、L254 `12:09:10.806 [http-nio-8888-exec-13]`。后三次载荷以 `0x160x030x01...` 开头 —— `0x16 0x03` 是 TLS ClientHello 记录头，即 **HTTPS 握手字节打到了明文 HTTP 端口**；L72 还夹带 HTTP/2 preface 片段与 ASCII `admin` / `queryBasicCfg` / `<?xml`。

**判定（确证）**：这是 Tomcat 自身以 **INFO** 记录的解析拒绝（紧跟 `Note: further occurrences ... will be logged at DEBUG level.`），**不是应用异常、不返回 5xx、不写 error log**。旁证 `request.log:223-224`：08:56:57.842 `[REQUEST] GET /sse ... UA=python-httpx/0.28.1` → `[RESPONSE] GET /sse status=404 duration=29ms`（外部扫描探测）。**属噪声**。附带价值：其中 ASCII `admin` 字样是**扫描载荷**，不是凭据泄漏（见 §7）。

---

## 4. 启动期检查：Bean / 配置 / 数据库（确证：无异常，但存在反复重启）

### 4.1 三次 `Starting`、同一 PID、前两次未完成启动

**日志行原文（确证）**

```
L1  2026-10-17 07:35:29.805 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication - Starting SelfBlogApplication v2.0.26 using Java 25.0.3 with PID 6 (/app/selfblog.jar started by root in /app)
L2  2026-10-17 07:35:29.812 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication - No active profile set, falling back to 1 default profile: "default"
L8  2026-10-17 07:35:38.577 [main] [] [] [] INFO  o.s.b.w.s.c.ServletWebServerApplicationContext - Root WebApplicationContext: initialization completed in 8548 ms
L29 2026-10-17 07:35:50.222 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication - Starting SelfBlogApplication v2.0.26 using Java 25.0.3 with PID 6 (/app/selfblog.jar started by root in /app)
L30 2026-10-17 07:35:50.231 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication - No active profile set, falling back to 1 default profile: "default"
L31 2026-10-17 07:36:01.881 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication - Starting SelfBlogApplication v2.0.26 using Java 25.0.3 with PID 6 (/app/selfblog.jar started by root in /app)
L32 2026-10-17 07:36:01.885 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication - No active profile set, falling back to 1 default profile: "default"
```

- **确证**：`Starting` 出现 3 次，均为**同一 PID 6**、同一 JAR 路径；第 1、2 次都只走到 `Root WebApplicationContext: initialization completed in 8548 ms / 6398 ms` 即被重新拉起，**从未打印 `Started SelfBlogApplication`**。
- **确证无关闭记录**：grep `Stopping|Shutdown|Closing|destroy|SIGTERM` 在 application.log 中 **0 命中** → 三次重启之间没有任何优雅关闭日志（被强杀或容器重启）。
- **确证无启动期异常**：grep `Application run failed|Error creating bean|BeanCreation|UnsatisfiedDependency|NoSuchBean|ApplicationContextException` 均 **0 命中**；`IllegalStateException` 仅出现在 ② 的栈帧里（无关）。→ **启动期 Bean/配置/数据库异常：无。**
- **确证 profile 与配置不符**：日志明确 `No active profile set, falling back to 1 default profile: "default"`，而 `backend/docker-compose.yml:98` 与 `backend/docker-compose.sqlite.yml:34` 都写了 `SPRING_PROFILES_ACTIVE: prod`。→ **运行实例未以 prod profile 启动**（后果：`application.yml:112-113 knife4j.enable: true` 与 `:103/:106 SWAGGER_ENABLED` 的 prod 关闭逻辑未生效，`/doc.html` 对外可达）。

**逐项检查（确证，grep 结果）**

| 检查项 | 结果 | 依据 |
| --- | --- | --- |
| Bean 创建失败 | **无** | `BeanCreation|UnsatisfiedDependency|NoSuchBean|BeanDefinition` 0 命中 |
| 配置缺失 | **无** | `环境变量检查通过！`（L27、L57）；无 `✗` 行 |
| JWT_SECRET / ADMIN_PASSWORD | **通过** | L22-25、L52-55（详见 §5） |
| 数据库连接 | **成功** | L13-16、L43-46，全 INFO |
| Flyway / SQL 初始化 | **成功，且项目未使用 Flyway** | `Flyway|Migration` **0 命中**；SQL 初始化走 `SQLite Schema 初始化完成`（L16、L46），对应 `DatabaseConfig` 的 `ResourceDatabasePopulator` + `src/main/resources/schema-sqlite.sql`（确证） |
| 端口占用（BindException） | **无** | `BindException|Address already in use|PortInUse|WebServerException` **0 命中** → 三次重启**不是**端口冲突导致（同一容器内顺序重启，8888 每次都能绑定） |
| 关闭期异常 | **无记录** | `Stopping|Shutdown|Closing|destroy|SIGTERM` **0 命中** → 三次重启之间无优雅关闭日志（被强杀/容器重启，见 §11 局限 1） |

### 4.2 第三次启动成功序列（确证）

```
L33 2026-10-17 07:36:08.235 [main] [] [] [] INFO  o.s.boot.web.embedded.tomcat.TomcatWebServer - Tomcat initialized with port 8888 (http)
L34 2026-10-17 07:36:08.340 [main] [] [] [] INFO  org.apache.coyote.http11.Http11NioProtocol - Initializing ProtocolHandler ["http-nio-8888"]
L36 2026-10-17 07:36:08.342 [main] [] [] [] INFO  org.apache.catalina.core.StandardEngine - Starting Servlet engine: [Apache Tomcat/10.1.42]
L37 2026-10-17 07:36:08.432 [main] [] [] [] INFO  o.a.c.core.ContainerBase.[Tomcat].[localhost].[/] - Initializing Spring embedded WebApplicationContext
L38 2026-10-17 07:36:08.433 [main] [] [] [] INFO  o.s.b.w.s.c.ServletWebServerApplicationContext - Root WebApplicationContext: initialization completed in 6398 ms
L39 2026-10-17 07:36:09.130 [main] [] [] [] INFO  com.selfblog.config.db.DatabaseConfig - ========================================
L40 2026-10-17 07:36:09.132 [main] [] [] [] INFO  com.selfblog.config.db.DatabaseConfig -    使用 SQLite 数据库
L41 2026-10-17 07:36:09.137 [main] [] [] [] INFO  com.selfblog.config.db.DatabaseConfig -    数据库路径: jdbc:sqlite:campus_blog.db
L43 2026-10-17 07:36:09.157 [main] [] [] [] INFO  com.zaxxer.hikari.HikariDataSource - SelfBlogSQLitePool - Starting...
L44 2026-10-17 07:36:09.497 [main] [] [] [] INFO  com.zaxxer.hikari.pool.HikariPool - SelfBlogSQLitePool - Added connection org.sqlite.jdbc4.JDBC4Connection@45ad3cd8
L45 2026-10-17 07:36:09.502 [main] [] [] [] INFO  com.zaxxer.hikari.HikariDataSource - SelfBlogSQLitePool - Start completed.
L46 2026-10-17 07:36:09.603 [main] [] [] [] INFO  com.selfblog.config.db.DatabaseConfig - SQLite Schema 初始化完成
L47 2026-10-17 07:36:11.995 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig - ========================================
L48 2026-10-17 07:36:11.997 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    检查环境变量配置...
L49 2026-10-17 07:36:11.998 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    数据库类型: sqlite
L50 2026-10-17 07:36:11.998 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ DB_URL_SQLITE: jdbc:sqlite:campus_blog.db
L51 2026-10-17 07:36:11.999 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ SQLite 模式，跳过 MySQL 数据库配置检查
L52 2026-10-17 07:36:11.999 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ JWT_SECRET: ********
L53 2026-10-17 07:36:12.000 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ ADMIN_PASSWORD: ********
L54 2026-10-17 07:36:12.000 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ JWT_EXPIRATION: 86400000 ms
L55 2026-10-17 07:36:12.001 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ JWT_REFRESH_EXPIRATION: 604800000 ms
L57 2026-10-17 07:36:12.002 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig - 环境变量检查通过！
L59 2026-10-17 07:36:17.109 [main] [] [] [] INFO  c.selfblog.service.content.impl.MediaServiceImpl - [Media] 上传目录: /app/uploads
L60 2026-10-17 07:36:19.511 [main] [] [] [] INFO  o.s.b.a.web.servlet.WelcomePageHandlerMapping - Adding welcome page: class path resource [static/index.html]
L61 2026-10-17 07:36:20.689 [main] [] [] [] INFO  o.s.b.actuate.endpoint.web.EndpointLinksResolver - Exposing 3 endpoints beneath base path '/actuator'
L62 2026-10-17 07:36:21.207 [main] [] [] [] INFO  org.hibernate.validator.internal.util.Version - HV000001: Hibernate Validator 8.0.2.Final
L63 2026-10-17 07:36:24.313 [main] [] [] [] INFO  org.apache.coyote.http11.Http11NioProtocol - Starting ProtocolHandler ["http-nio-8888"]
L64 2026-10-17 07:36:24.432 [main] [] [] [] INFO  o.s.boot.web.embedded.tomcat.TomcatWebServer - Tomcat started on port 8888 (http) with context path '/'
L65 2026-10-17 07:36:24.545 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication - Started SelfBlogApplication in 25.45 seconds (process running for 26.898)
L66 2026-10-17 07:36:26.532 [main] [] [] [] INFO  com.selfblog.config.db.DataInitializer - 作者管理员账户初始化成功: 19709788816
L67 2026-10-17 07:36:26.583 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication -    SelfBlog启动成功！
L68 2026-10-17 07:36:26.585 [main] [] [] [] INFO  com.selfblog.SelfBlogApplication -    API文档地址：http://localhost:8825/doc.html
```

- **确证数据库正常**：SQLite 数据源创建、`Added connection`、`Start completed`、`SQLite Schema 初始化完成` 全部成功且全为 INFO，**无任何 WARN/ERROR**。
- **确证 DispatcherServlet 懒初始化正常**（L83-85，08:14:25.856，首个进入 Servlet 的请求 `http-nio-8888-exec-2`）：`Initializing Spring DispatcherServlet 'dispatcherServlet'` → `Completed initialization in 70 ms`。
- **确证 08:14 之前无任何业务请求**：启动后最早的应用侧事件是 L70（08:03:13.814，Tomcat 层拒绝的扫描载荷，**未进入 Servlet**），直到 L83（08:14:25.856）才有首个请求被 DispatcherServlet 处理。→ 07:36 启动成功至 08:14 之间约 38 分钟无业务流量（该时段的唯一访问是 L70 的端口扫描）。

### 4.3 端口不一致（配置缺陷，最高优先级）

| 来源 | 端口 | 原文/行号 |
| --- | --- | --- |
| 实际运行 | **8888** | L3 `Tomcat initialized with port 8888 (http)`；L4 `Initializing ProtocolHandler ["http-nio-8888"]`；L64 `Tomcat started on port 8888 (http) with context path '/'`；线程名 `http-nio-8888-exec-N`（确证） |
| `application.yml` | 8825（默认） | `backend/src/main/resources/application.yml:94  port: ${SERVER_PORT:8825}` |
| `Dockerfile` | 8825 | `backend/Dockerfile:50 ENV SERVER_PORT=8825`、`:53 EXPOSE 8825`、`:57-58 HEALTHCHECK ... curl -f http://localhost:8825/actuator/health ...` |
| compose（MySQL） | 8825 | `backend/docker-compose.yml:92 SERVER_PORT: 8825`、`:100 "127.0.0.1:8825:8825"`、`:107-109 healthcheck 8825` |
| compose（SQLite） | 8825 | `backend/docker-compose.sqlite.yml:29 SERVER_PORT: 8825`、`:36 "127.0.0.1:8825:8825"`、`:44 healthcheck 8825` |
| 启动横幅 | 8825（硬编码） | `backend/src/main/java/com/selfblog/SelfBlogApplication.java:34` 硬编码 `log.info("   API文档地址：http://localhost:8825/doc.html")` → 主日志 L68 |

→ **确证**：运行实例被注入了 `SERVER_PORT=8888`（或 `-Dserver.port=8888`），与仓库中所有配置、Dockerfile、compose、文档（`backend/DEPLOY.md:52`、`backend/README.md:56` 均写 `http://localhost:8825/actuator/health`）**全部冲突**。

**推测（中高置信）**：这正是 §4.1 前两次启动未完成的原因 —— 容器 `restart: unless-stopped` + healthcheck 打 `8825` 而应用监听 `8888`，健康检查必然失败，容器被判 unhealthy 后重启；第三次因启动更快（8.5s → 6.4s）而在 60s `start_period` 内通过（或健康检查被绕过）。**需核对部署环境实际 `SERVER_PORT` 与端口映射**。

### 4.4 镜像与构建不一致（推测）

- 日志：`v2.0.26 using Java 25.0.3`（L1/L29/L31，确证）。
- 仓库：`backend/pom.xml:18 <version>2.0.26</version>`、`:24 <java.version>21</java.version>`；`backend/Dockerfile:31 FROM eclipse-temurin:21-jre AS runtime`。
- → **推测**：该容器**不是用仓库 Dockerfile 构建的**（否则 JRE 21 不可能报 `Java 25.0.3`）。结合 `CLAUDE.md` 记载本机 `JAVA_HOME` 指向 `jdk-25`、而 `pom.xml` 要求 21，推测镜像是用本机 JDK 25 直接 `java -jar` 或自建基础镜像启动。**Java 25 未在仓库任何 CI/文档中声明为受支持运行时**，属未验证组合。

---

## 5. JWT_SECRET / ADMIN_PASSWORD 校验（确证：通过，且未泄漏明文）

**日志行原文（主日志 L17-L27；L47-L57 为第三次启动的相同序列）**

```
2026-10-17 07:35:44.224 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig - ========================================
2026-10-17 07:35:44.230 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    检查环境变量配置...
2026-10-17 07:35:44.231 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    数据库类型: sqlite
2026-10-17 07:35:44.231 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ DB_URL_SQLITE: jdbc:sqlite:campus_blog.db
2026-10-17 07:35:44.231 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ SQLite 模式，跳过 MySQL 数据库配置检查
2026-10-17 07:35:44.232 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ JWT_SECRET: ********
2026-10-17 07:35:44.232 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ ADMIN_PASSWORD: ********
2026-10-17 07:35:44.232 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ JWT_EXPIRATION: 86400000 ms
2026-10-17 07:35:44.232 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig -    ✓ JWT_REFRESH_EXPIRATION: 604800000 ms
2026-10-17 07:35:44.234 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig - ========================================
2026-10-17 07:35:44.234 [main] [] [] [] INFO  com.selfblog.config.EnvValidationConfig - 环境变量检查通过！
```

**源码对照（确证）** `backend/src/main/java/com/selfblog/config/EnvValidationConfig.java`

- `:56-57` `@PostConstruct public void validateEnvironment()`；
- `:116-122` JWT_SECRET：空 → `log.error("   ✗ JWT_SECRET 未配置")`；`length() < 32` → `✗ JWT_SECRET 长度不足（至少32位）`；否则 `✓ JWT_SECRET: ********`；
- `:125-129` ADMIN_PASSWORD：`isBlank || length() < 12` → `✗ ADMIN_PASSWORD 未配置或长度不足（至少12位）`；
- `:149-151` 汇总失败时 `log.error("环境变量配置不完整！...")` + `throw new IllegalStateException("环境变量配置不完整，启动失败")`（本次未触发）；
- `:163-166` `maskValue()` 只保留前 2 位 + `****` + 后 2 位。

→ **确证**：`JWT_SECRET`（≥32 位）与 `ADMIN_PASSWORD`（≥12 位）**均通过校验**，日志只出现掩码 `********`，**无明文密钥落盘**。

---

## 6. SQLite busy/locked、HikariPool 耗尽、连接超时（确证：均未发生）

**grep 结果（application.log / application-error.log 全部 0 命中，确证）**

| 关键词 | 命中 |
| --- | --- |
| `SQLITE_BUSY` / `database is locked` | 0 |
| `SQLTransientConnectionException` | 0 |
| `Connection is not available` / `CannotGetJdbcConnection` | 0 |
| `HikariPool.*Timeout` / `Connection timeout` | 0 |
| `RejectedExecution` | 0 |
| `OutOfMemory` | 0 |

Hikari 相关行只有两次启动的 INFO 三连（L13-15、L43-45）：`SelfBlogSQLitePool - Starting...` / `Added connection ...` / `Start completed.`，**无一条 WARN/ERROR**。

**配置事实（确证，用于评估风险而非判定故障）** `backend/src/main/java/com/selfblog/config/db/DatabaseConfig.java`

```
:69-77  HikariConfig config = new HikariConfig();
        setJdbcUrl(sqliteUrl) / setDriverClassName("org.sqlite.JDBC") / setPoolName("SelfBlogSQLitePool")
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
:83-93  PRAGMA journal_mode=WAL / synchronous=NORMAL / foreign_keys=ON / busy_timeout=5000
        / cache_size=-50000 / temp_store=MEMORY / mmap_size=268435456
:91-93  catch → log.warn("SQLite 内存/WAL 参数初始化不完整: {}", e.getMessage())   ← 本次未触发
```

**推论（推测）**：`maximumPoolSize=1` 把 SQLite 全部访问串行化到单连接。日志期间虽未出现 busy/locked 或池耗尽（`busy_timeout=5000` 提供了 5s 等待窗口，足以吸收低并发），但**写事务与并发读互相排队**，直接解释了 §8 的性能观察项（发文 4.7s、列表 1.7s）。这是**容量/延迟风险，不是本次日志中的故障**。

---

## 7. 定时任务 / 异步线程异常（确证：无）

- 代码侧存在定时/异步机制（确证）：`backend/src/main/java/com/selfblog/SelfBlogApplication.java:23 @EnableScheduling`；`config/AsyncConfig.java:17 @EnableAsync` + `:49`/`:67` 两个 `ThreadPoolTaskExecutor`；`config/security/JwtSchedulerConfig.java:13 @EnableScheduling` + `:23 @Scheduled(fixedRate = 3600000)`；`listener/NotificationEventListener.java:32/66/94/123` 四处 `@Async("notificationExecutor")`；`service/content/impl/TrendingServiceImpl.java:413 @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Shanghai")`。
- **确证日志中 0 命中**：对 application.log / application-error.log grep `scheduling-` / `task-[0-9]` / `Async` 均为 0 命中。→ **该两份日志中没有任何定时任务或异步线程的异常记录**。
- 唯一相关旁证（`performance.log:12`，确证）：

```
2026-10-17 16:00:01.160 [scheduling-1] [] [] [] INFO  ... [PERF] SERVICE TrendingServiceImpl.scheduledUpdateAllTrending execution time: 1088ms (SLOW)
```

→ `scheduling-1` 线程确实在跑，且**成功**（INFO，无 error 字段）。

- **注意取证边界（确证）**：`TrendingServiceImpl` 的定时任务是 `cron = "0 0 0 * * ?"`（每日 00:00），而本日志只覆盖 07:35-15:54，**当日 00:00 的调度不在覆盖窗口内**；`scheduling-1` 在 16:00 的出现来自 `performance.log`，属另一文件的时间窗。故「定时任务无异常」的结论**仅对本日志时间窗成立**。

---

## 8. 噪声与真实故障的判定

### 8.1 真实故障（3 项）

| 严重度 | 故障 | 证据 |
| --- | --- | --- |
| 高 | 运行端口 8888 与全仓库配置/健康检查 8825 冲突，导致启动期三次重启 + 健康检查失效 | 主日志 L1/L29/L31 vs L3/L64；`docker-compose*.yml` healthcheck 8825（§4.3） |
| 中 | `/favicon.ico` 缺失且无 `NoResourceFoundException` 处理器 → **应为 404 却返回 500 并记 ERROR** | 主日志 L88-L93；`request.log:226 status=500`（§3.1） |
| 中 | 相册上传 >10 文件触发 `FileCountLimitExceededException`，无 `MultipartException` 处理器 → **应为 400 却返回 500 并记 ERROR** | 主日志 L267-L272 / L408-L409 / L419-L424 / L560（§3.2） |

### 8.2 噪声（4 类，非故障，确证）

| 噪声 | 为何是噪声 |
| --- | --- |
| ① TLS 握手字节/扫描载荷 → `Invalid character found in method name`（4 次，INFO） | Tomcat 自身记录并拒绝，**INFO 级**、不返回 5xx、不写 error log；属端口扫描/误连 |
| ④ `Business: code=400, message=文件内容与扩展名不匹配`（1 次，WARN） | 业务校验**正确拒绝**；`request.log:425-426` 确证 `POST /api/admin/site-assets status=400 duration=99ms`；与 `AdminSiteAssetController.java:20` 类注释「把所有图片重新编码成 jpg，.ico / .svg / 音频都过不去」吻合 |
| ⑤ `MethodNotAllowed: POST not supported`（1 次，WARN） | 扫描器探测；`request.log:223-224` 旁证同类流量 `GET /sse status=404` |
| ⑥ `[PERF] ... error:`（performance.log:7-8） | `PerformanceMonitoringAspect` 以 ERROR 打印**业务负例断言**；**不进主日志/error log**（grep 0 命中）；与 `CLAUDE.md` 记载一致 |

### 8.3 性能观察项（非异常）

均来自 `performance.log` 与 `request.log`，主日志中无对应 ERROR：

```
performance.log:9-11   15:54:52.694/732/818  [PERF] MAPPER BaseMapper.insert 2451ms (SLOW)
                                              WARN [PERF] SERVICE BlogPostServiceImpl.createPost 3320ms (VERY SLOW)
                                              WARN [PERF] CONTROLLER BlogPostController.createPost 3423ms (VERY SLOW)
request.log:1072       15:54:52.833 [http-nio-8888-exec-6] ... WARN ... [RESPONSE] SLOW POST /api/post status=200 duration=4710ms clientIp=61.224.74.83
performance.log:13-17  16:14:39.512  BlogPostController.getAdjacentPosts 1116ms (SLOW)
                       16:15:03.237-263  BaseMapper.selectPage 1649ms / PostQueryServiceImpl.getPostList 1671ms
                                         / BlogPostServiceImpl.getPostList 1675ms / BlogPostController.getPostList 1681ms
performance.log:3-4    08:29:42.876/878  SysUserServiceImpl.login 2513ms (SLOW) / SysUserController.login 2583ms
performance.log:5-6    12:05:47.695/697  登录 1243ms / 1251ms
performance.log:1-2    08:14:28.833/916  ReadingHtmlRenderer.render 1292ms (SLOW) / SpaController.readingPage 2279ms (SLOW)
```

阈值来源（确证）：`backend/src/main/java/com/selfblog/config/web/HttpRequestLoggingInterceptor.java:28 SLOW_REQUEST_THRESHOLD = 3000`（>3000ms 记 `SLOW`）；`:66-95` afterCompletion 分支（ex≠null → error；duration>3000 → WARN SLOW；status≥400 → WARN；否则 INFO）。

→ **判定：性能观察项，非故障**。成因见 §6 的 `maximumPoolSize=1` 串行化（推测）。

### 8.4 重复刷屏与同一根因的重复（确证）

| 组 | 计数 | 是否同一根因 | 判定 |
| --- | --- | --- | --- |
| ① `Invalid character found in method name` | 4 | **是** —— 同一 Tomcat 解析拒绝路径（`Http11InputBuffer.parseRequestLine(403)`），4 次都是外部扫描/TLS 误连，载荷各不相同 | **非刷屏**：Tomcat 自带去重（每次紧跟 `Note: further occurrences of HTTP request parsing errors will be logged at DEBUG level.`，L71/L227/L242/L255），故 4 次已是「已去重后的首次」计数，**不存在刷屏风险** |
| ③ `MultipartException` / `FileCountLimitExceededException` | 2 | **是** —— 同型异常、同一管理员（`[1]`）、同一 IP `82.153.135.59`，间隔 12 秒（12:09:49.691 → 12:10:01.898），栈帧逐字一致 | **同一根因的重复**：管理员连续两次尝试上传超量文件（第一次失败后未察觉，12 秒后重试）。**这是真实故障的唯一重复项** |
| ② `NoResourceFoundException` favicon | 1 | 单次 | 无重复 |
| ④ `Business` 400 / ⑤ `MethodNotAllowed` 405 | 各 1 | 单次 | 无重复 |
| ⑥ `[PERF] ... error:` | 1 组 2 行 | **是**（SERVICE 与 CONTROLLER 两层包装同一次业务拒绝） | 同一次负例的两层记录，非刷屏 |

**结论（确证）**：**本日志不存在应用级刷屏**（无同一异常在短时间内被重复打印数十次；级别分布 3 ERROR + 2 WARN / 571 行）。唯一需要关注的重复是 ③ 的 2 次同根因重试。**另注**：若 `GlobalExceptionHandler` 的兜底分支（`:259-263`）遇到高频 404 静态资源请求（如 favicon 被大量浏览器反复拉取），会**把每一次都打成 ERROR + 500**，此时刷屏风险由代码结构决定 —— 本次日志因 favicon 仅被访问 1 次而未暴露，属**潜在刷屏风险**（推测）。

---

## 9. 敏感信息泄漏检查（确证：无真实泄漏，1 处 PII 明文）

| 检查项 | 结果 | 依据 |
| --- | --- | --- |
| JWT / token 明文 | **无** | grep `eyJ`（JWT 前缀）、`Bearer`、`Authorization` 在 application.log：`eyJ`/`Bearer` 0 命中；`Authorization` 仅命中 Security 栈帧 `AuthorizationFilter.doFilter`（L120/L302/L454） |
| 密码明文 | **无** | `password` 命中仅 L22/L23/L52/L53 的 `********` 掩码 |
| 完整 SQL | **无** | grep `SELECT |INSERT INTO|UPDATE |DELETE FROM` 0 命中；`application.yml:82 log-impl: ${MYBATIS_SQL_LOG:org.apache.ibatis.logging.nologging.NoLoggingImpl}`；`logback-spring.xml:256-268` 中 `com.selfblog.mapper` 与 `org.hibernate.SQL` 均 `level="WARN"`（prod 段 `:322` 同） |
| audit.log 敏感项 | **无** | grep `password|token|eyJ|19709788816|Bearer` 0 命中 |
| `admin` 字样（L72） | **非泄漏** | 出现在 TLS/HTTP2 扫描载荷的 ASCII 片段中（同段还有 `queryBasicCfg`、`<?xml`），是**攻击载荷**而非应用凭据 |
| PII | **有（低危）** | 管理员登录名 `19709788816`（手机号）以明文出现：L66 `作者管理员账户初始化成功: 19709788816`、L86 / L239 `用户登录成功: username=19709788816, userId=1, email=未设置` |

登录成功日志的源码侧（确证）`backend/src/main/java/com/selfblog/service/auth/impl/SysUserServiceImpl.java`：`:244 log.info("用户登录成功: username={}, userId={}, email={}", ...)`；`:150`/`:180` 用 `StringMaskUtils.maskEmail` 掩码邮箱；`:188`/`:207` 失败日志只记账号名、**不记密码**（`:205 passwordEncoder.matches(request.getPassword(), user.getPassword())`）。

→ **建议（不改代码，仅记录）**：登录成功日志中的 `username`（即手机号）应掩码，与已有的 `maskEmail` 保持一致口径。

---

## 10. 根因列表（按严重度排序）

| # | 严重度 | 根因 | 性质 | 证据 |
| --- | --- | --- | --- | --- |
| R1 | **高** | 运行实例 `SERVER_PORT=8888` 与仓库全部配置/文档/Dockerfile/compose 的 `8825` 冲突；Docker healthcheck 打 8825 必然失败 → 启动期三次重启、健康检查失效、容器可能被反复重启 | 配置/部署缺陷（**确证冲突 + 推测因果**） | 主日志 L1/L29/L31（三次 Starting，无 `Started`）vs L3/L64（8888）；`docker-compose*.yml` healthcheck 8825 |
| R2 | **中** | `GlobalExceptionHandler` 缺 `NoResourceFoundException` 处理器（只注册已被取代的 `NoHandlerFoundException`）→ `/favicon.ico` 缺失时返回 500 并记 ERROR（应 404） | 代码缺陷（确证） | 主日志 L88-L93；`GlobalExceptionHandler.java:103-107` vs `:259-263`；无 `favicon.ico` 文件 |
| R3 | **中** | `GlobalExceptionHandler` 缺 `MultipartException` / `FileCountLimitExceededException` 处理器 → 上传超限返回 500 并记 ERROR（应 400） | 代码缺陷（确证） | 主日志 L267-L272、L408-L409、L419-L424、L560；`GlobalExceptionHandler.java` 21 个 handler 无二者 |
| R4 | **中** | 相册上传链路未限制文件个数（前端 `Albums.vue` 无上限 + 后端 `AdminPhotoAlbumController.java:51-52` 无 `@Size`），超过 Tomcat 默认 `maxFileCount=10` 即报错；对照 `MediaController.java:100-102` 有 `max=10` 校验 | 一致性缺陷（**确证**） | 主日志 L408 `FileCountLimitExceededException: attachment`；三处源码对照；`Connector` 默认 `maxPartCount=10` |
| R5 | **中** | 实际运行 profile 是 `default`（非 compose 声明的 `prod`）→ prod 专属的 Knife4j/Swagger 关闭未生效，API 文档可能对外可达 | 配置缺陷（确证 profile + 推测暴露面） | 主日志 L2/L30/L32 `No active profile set ... "default"`；`docker-compose*.yml` `SPRING_PROFILES_ACTIVE: prod`；`application.yml:103/106/113` |
| R6 | **低** | 运行镜像与仓库构建不一致：日志 `Java 25.0.3` vs `pom.xml:24 java.version=21` / `Dockerfile:31 temurin:21-jre` → 该镜像是本机 JDK 25 自建，属未验证运行时组合 | 部署一致性（**推测**） | 主日志 L1/L29/L31；`pom.xml:18/:24`；`Dockerfile:31` |
| R7 | **低** | SQLite `maximumPoolSize=1` 串行化读写 → 发文 4.7s、列表 1.1-1.7s、登录 1.2-2.5s 的持续高延迟（本次未造成故障） | 容量风险（**推测**） | `DatabaseConfig.java:75`；`performance.log:1-17`；`request.log:1072` |
| R8 | **低** | 管理员登录名（手机号 `19709788816`）明文落盘，未掩码 | PII（确证） | 主日志 L66/L86/L239；`SysUserServiceImpl.java:244`（已掩码 email 但未掩码 username） |

---

## 11. 取证局限与未覆盖项（诚实声明）

1. **本日志不是完整运行史**：主日志出现 3 次 `Starting` 却 **0 条** `Stopping/Shutdown`，末行（L571，15:54:52.727 文章创建成功）后直接结束 → 采集到的是容器 stdout/json-file 日志的**截断/滚动片段**（compose 配 `logging.options.max-size: 50m`、`max-file: '5'`，`application.yml:206 max-file-size: 100MB`）。**不能据此断言服务稳定性或可用性**。
2. **R1/R6 的因果与镜像来源均为推测**，需核对部署环境（实际 `SERVER_PORT`、端口映射、镜像构建命令/基础镜像）才能确证。
3. **③ 的触发端点（相册上传）为高置信推测**：multipart 解析早于 `HandlerInterceptor.preHandle`，`request.log` 无该请求的任何 REQUEST/RESPONSE 行；但两次均带 `userId=1` + 同一管理员 IP + 同型异常 + 相册页旁证 + 端点无 `@Size`，推断链条完整。**要 100% 确证需应用侧在 `checkMultipart` 失败时也记录请求 URI**（当前代码不会）。
4. **未覆盖时间窗**：`TrendingServiceImpl` 的 `@Scheduled(cron = "0 0 0 * * ?")`（每日 00:00）与 `JwtSchedulerConfig` 的 `fixedRate = 3600000` 在本次日志窗口（07:35-15:54）内无对应记录，**其执行情况不在本报告结论范围内**。
5. **未执行**：未运行任何构建/测试（任务要求只读取证，且日志非本机服务产物）；本报告的源码行号均通过 read/grep 直读确认，未做编译验证。
6. **中文编码**：UTF-8 直读正常；若下游系统以 GBK 读取会乱码，属读取方式问题而非日志内容问题。

---

## 12. 证据索引（原始行号速查）

| 结论 | application.log | application-error.log | 其他文件 |
| --- | --- | --- | --- |
| 三次启动、无关闭 | L1、L29、L31（Starting）；L8、L38（WebApplicationContext） | — | — |
| 启动成功序列 | L33-L69 | — | — |
| 端口 8888 | L3、L4、L33、L64 | — | `docker-compose.yml:92/100/107-109`、`docker-compose.sqlite.yml:29/36/44`、`Dockerfile:50/53/57-58`、`application.yml:94` |
| profile=default | L2、L30、L32 | — | `docker-compose*.yml` `SPRING_PROFILES_ACTIVE: prod` |
| 环境变量校验通过 | L17-L27、L47-L57 | — | `EnvValidationConfig.java:56-166` |
| SQLite/Hikari 正常 | L13-L16、L43-L46 | — | `DatabaseConfig.java:69-93` |
| ① TLS 扫描（4 次） | L70-L82、L226-L238、L241-L253、L254-L266 | — | `request.log:223-224` |
| ② favicon 500 | L88-L225（栈 L89-93） | L1-L138 | `request.log:225-226` |
| ④ 上传业务拒绝 400 | L240 | — | `request.log:425-426` |
| ⑤ 405 扫描 | L87 | — | — |
| ③ multipart 失败 ×2 | L267-L418、L419-L570（根因帧 L408、L560） | L139-L290、L291-L442（`Caused by` L280、L432） | `request.log:589-594`（旁证） |
| 性能观察项 | — | — | `performance.log:1-17`、`request.log:1072` |
| 定时任务成功 | — | — | `performance.log:12` |
| PII 明文 | L66、L86、L239 | — | `SysUserServiceImpl.java:244` |
| 重复/刷屏判定 | L70/L226/L241/L254（①×4，已去重）；L267/L419（③×2 同根因） | L1/L139/L291 | — |

---

## 13. 对任务 6 问的逐条作答（速查）

| 任务问项 | 结论 | 详见 |
| --- | --- | --- |
| 1) 全部 ERROR/WARN 清单 + 类型×指纹聚合 + 同根因重复 | ERROR 3 条（②③③）、WARN 2 条（④⑤）；聚合表 6 类；唯一同根因重复是 ③×2 | §2、§8.4 |
| 2) 每类异常栈顶 5 帧 + 请求路径/线程 + 根因（确证/推测分开） | ① Tomcat parse（L73-77，exec-1/6/13）；② `ResourceHttpRequestHandler`（L89-93，exec-9，GET /favicon.ico）；③ `StandardMultipartHttpServletRequest`（L268-272，exec-1/exec-6，端点=相册上传·推测） | §3.1、§3.2、§3.3 |
| 3) 启动期/关闭期异常（Bean、配置、JWT/ADMIN_PASSWORD、DB、Flyway/SQL、端口占用） | **全部无异常**：Bean/配置 0 命中；JWT_SECRET/ADMIN_PASSWORD 通过；SQLite 连接+Schema 成功；项目未用 Flyway（0 命中）；无 BindException/端口占用。**但存在 3 次启动、前 2 次未完成**（端口 8888 vs 8825 冲突·推测）；关闭期**无任何记录**（0 命中） | §4.1、§4.3、§5 |
| 4) 定时/异步异常（scheduling、async、池耗尽、Hikari、busy/locked、超时） | **全部无**：主日志 `scheduling-`/`Async` 0 命中；`SQLITE_BUSY`/`SQLTransient`/`Connection is not available`/`RejectedExecution`/`OutOfMemory` 0 命中；Hikari 全 INFO | §6、§7 |
| 5) 级别与噪声（刷屏 / 负例记成 ERROR / 敏感信息） | 无刷屏（§8.4）；⑥ `[PERF]` 负例记成 ERROR 但**只进 performance.log**；敏感信息**无 token/密码/完整 SQL 泄漏**，仅 1 处手机号 PII | §8.2、§8.4、§9 |
| 6) 版本/profile/运行形态 | `v2.0.26`、`Java 25.0.3`、profile=**default**（非 prod）、端口 **8888**、DB=**SQLite**（`jdbc:sqlite:campus_blog.db`）、容器 `/app`、PID 6 | §1、§4.3、§4.4、§10 |
