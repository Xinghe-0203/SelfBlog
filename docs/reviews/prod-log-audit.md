# 生产安全审计日志取证报告 — audit.log 全量分析

- **日期**：2026-10-18
- **取证对象**：`C:\Users\Zora\Downloads\audit.log`（26,159 字节，**109 行**，UTF-8，mtime 2026/10/18 00:37:06）
- **交叉比对对象**：`C:\Users\Zora\Downloads\request.log`（357,774 字节，**1,300 行**）、`application.log`（66,342 字节）、`application-error.log`（49,761 字节）、`performance.log`（3,991 字节）
- **取证方式**：只读（read / grep / glob / Select-String 聚合）。**未修改 backend/frontend 任何代码或配置**；本文件是本次任务唯一写入的产物。
- **日志时间跨度**：2026-10-17 08:14:28.988 → 2026-10-17 16:15:03.267（约 8 小时）
- **运行环境**（来自 `application.log` 首行原文）：容器 `/app`，端口 8888，SQLite `jdbc:sqlite:campus_blog.db`，`No active profile set, falling back to 1 default profile: "default"` —— **即按 `default` profile 运行，而非 `prod` profile**。

> **证据标注约定**：`【确证】`= 有原始日志行/源码行直接支撑；`【推测】`= 由间接证据推断，无法由现有日志闭合证明。

---

## 0. 结论摘要（按严重度）

| # | 严重度 | 结论 | 判定 |
|---|--------|------|------|
| 1 | **严重** | **审计覆盖不足**：`audit.log` 全量仅 109 行，其中 99 行是同一个模板的 `AUTH_ENTRY_POINT`。管理端敏感操作（发布/删除/站点设置/上传/改密码）在 audit.log 中**零条记录**，`event=ADMIN_OPERATION` 计数为 **0**。 | 【确证】 |
| 2 | **严重** | **管理操作审计钩子是死代码**：`SecurityConfig.java:105` 定义了 `logAdminOperation`，`SecurityConfig.java:110` 会写 `event=ADMIN_OPERATION`，但全仓库检索**无任何调用点**。 | 【确证】 |
| 3 | **高** | **两套日志对同一事件覆盖不一致**：audit.log 记录 **99 次**认证失败（`AUTH_ENTRY_POINT` → HTTP 401），而 request.log 中 `status=401` 计数为 **0**、`status=403` 为 **0**、`status=429` 为 **0**。 | 【确证】 |
| 4 | **高** | **凭据/配置文件扫描**：`34.156.121.46` 在 3 秒内发起 42 次请求，其中 40 次 404，包含 **25 个 `.env` 变体**（外加无点号的 `/env`）与 `phpinfo`、`credentials`、`config/*.php`、`Dockerfile` 等路径探测；同 IP 在 audit.log 中产生 **77 次** 401，全部压缩在 **2.068 秒**内。 | 【确证】 |
| 5 | **中** | **登录前管理路径枚举后随即登录成功**：`82.153.135.59` 先探测 `GET /admin`、`GET /zroa`（均 404），登录前已有 4 次 401，随后 `POST /api/user/login` 返回 200，并立刻执行 2 次站点设置写入、7 次 `site-assets` 上传、1 次相册创建。 | 【确证】序列；【推测】意图 |
| 6 | **中** | **未记录登录即持有 admin 会话**：`61.224.74.83` 首条请求 15:49:48 为匿名，1.5 秒后（15:49:49.697）已带 `[1]` userId 调用 `POST /api/visitor/track`，但该 IP 在整个 request.log 中**没有任何 `POST /api/user/login`**。 | 【确证】事实；【推测】跨 IP token 复用 |
| 7 | **中** | **限流 429 完全无审计**：`RateLimitInterceptor.java:61-66` 返回 429 时不写任何日志（该类无 logger），`request.log` 中 `status=429` 为 0 —— 无法证明限流是否被触发过。 | 【确证】 |
| 8 | **中** | **审计日志不含请求目标**：99 条 `AUTH_ENTRY_POINT` 全部只有 `detail=Full authentication is required to access this resource`，**无 URI、无 method、无 userId**，无法回答「哪个接口被攻击」。 | 【确证】 |
| 9 | **低** | **两个 500 级 multipart 异常未进 request.log**：`application-error.log` 记录 12:09:49 与 12:10:01 两次 `FileCountLimitExceededException`，但 request.log 全量仅 1 条 `status=500`（favicon.ico）。 | 【确证】 |
| 10 | **低** | 注册接口调用**零记录**：request.log 中 `/register` 计数 0，audit.log 中 `[REGISTER]` 计数 0，且 `LogUtils.logRegister`（`LogUtils.java:304`）无调用点 —— 即使注册被调用也不会留痕。 | 【确证】 |

**没有证据表明发生了成功的入侵或数据泄露。** 但上述审计盲区使得「未发生」与「发生了但没记录」无法区分，这是本报告最核心的问题。

---

## 1. audit.log 内容全貌与「审计覆盖不足」的直接证据

### 1.1 文件规模

| 指标 | 值 |
|------|-----|
| 行数 | **109** |
| 字节 | 26,159 |
| 时间跨度 | 08:14:28.988 → 16:15:03.267 |
| 平均 | 约每 4.4 分钟 1 行 |

对比同一时间窗内的 `request.log`（1,300 行）：**audit.log 只有 request.log 行数的 8.4%**。

### 1.2 事件类型分布（全量聚合）

```
event=AUTH_ENTRY_POINT   => 99
event=BUSINESS_EXCEPTION => 1
```

日志方法标签分布：

```
[SECURITY]    => 100
[PERFORMANCE] => 9
```

**以下事件类型在 audit.log 中出现次数均为 0**（原始检索结果）：

```
ADMIN_OPERATION    => 0
[LOGIN]            => 0
[REGISTER]         => 0
[USER_ACTION]      => 0
[FILE_UPLOAD]      => 0
AUTH_FAILED        => 0
ACCESS_DENIED      => 0
JWT_AUTH_FAILED    => 0
REFUSED_REFRESH_TOKEN => 0
```

### 1.3 管理端敏感操作 = 零记录

request.log 确证以下管理员操作在本次时间窗内真实发生过（全部 `userId=[1]`、`status=200`），但 audit.log 中**没有任何一条对应记录**：

| 操作 | 原始证据行（request.log） | audit.log 对应记录 |
|------|--------------------------|-------------------|
| 站点设置读取 | `L127: 2026-10-17 08:29:52.545 ... [1] [153.119.45.93] ... [REQUEST] GET /api/admin/site-settings  from IP=153.119.45.93` | 无 |
| **站点设置写入** | `L129: 2026-10-17 08:31:05.470 ... [1] [153.119.45.93] ... [REQUEST] PUT /api/admin/site-settings  from IP=153.119.45.93` | 无 |
| 站点设置写入（第 2 次） | `L421: 2026-10-17 12:06:23.183 ... [1] [82.153.135.59] ... [REQUEST] PUT /api/admin/site-settings  from IP=82.153.135.59` | 无 |
| 站点设置写入（第 3 次） | `L437: 2026-10-17 12:08:21.609 ... [1] [82.153.135.59] ... [REQUEST] PUT /api/admin/site-settings  from IP=82.153.135.59` | 无 |
| **上传 site-assets（×7）** | `L419: 2026-10-17 12:06:20.033 ... [1] [82.153.135.59] ... [REQUEST] POST /api/admin/site-assets  from IP=82.153.135.59`；`L420: ... [RESPONSE] POST /api/admin/site-assets status=200 duration=762ms clientIp=82.153.135.59` | 无 |
| **创建相册** | `L591: 2026-10-17 12:09:21.903 ... [1] [82.153.135.59] ... [REQUEST] POST /api/admin/albums  from IP=82.153.135.59`；`L592: ... status=200 duration=170ms` | 无 |
| 发布文章 | `L106: 2026-10-17 15:54:52.833 ... [1] [61.224.74.83] WARN ... [PERFORMANCE] SLOW operation=POST /api/post, duration=4713ms` | 无 |
| 访客数据读取 | `L1051: 2026-10-17 15:50:01.158 ... [1] [61.224.74.83] ... [REQUEST] GET /api/admin/visitors/summary  from IP=61.224.74.83` | 无 |

> request.log 按**实际请求次数**（只数 `[REQUEST]` 行）统计：`POST /api/admin/site-assets` **7 次**、`PUT /api/admin/site-settings` **3 次**（另有 `GET /api/admin/site-settings` 7 次）、`POST /api/admin/albums` **1 次**、`POST /api/post` **1 次** —— **audit.log 对这些操作 0 记录**。
> （若按子串匹配会把 `[REQUEST]`+`[RESPONSE]` 双行算成两倍，故此处一律以 `[REQUEST]` 行计数为准。）

### 1.4 根因：管理操作审计钩子从未被调用

`backend/src/main/java/com/selfblog/config/security/SecurityConfig.java:102-113` 原文：

```java
    /**
     * 记录管理员操作审计日志
     */
    private void logAdminOperation(String action, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        String clientIp = LogUtils.getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();
        LogUtils.logSecurityEvent("ADMIN_OPERATION",
                "action=" + action + ", uri=" + uri + ", method=" + method + ", userId=" + (userId != null ? userId : "anonymous"),
                clientIp);
    }
```

全仓库检索 `logAdminOperation\(` 的**唯一命中是它自己的定义行**（`SecurityConfig.java:105`），**没有任何调用点**。`【确证】`

同理，`backend/src/main/java/com/selfblog/utils/LogUtils.java` 中三个审计方法均无调用点：

- `LogUtils.java:207  public static void logUserAction(Long userId, String username, String action, String detail)` → `[USER_ACTION]`，**0 调用点**
- `LogUtils.java:304  public static void logRegister(String username, String clientIp, boolean success)` → `[REGISTER]`，**0 调用点**
- `LogUtils.java:305  log.info("[FILE_UPLOAD] userId={}, filename={}, size={}, success={}", ...)` 所在的 `logFileUpload` → **0 调用点**

实际被调用的只有 `LogUtils.logSecurityEvent(String event, String detail, String clientIp)`（`LogUtils.java:238-240`，写 `[SECURITY] event=..., detail=..., clientIp=...`）与 `logPerformance`（`LogUtils.java:228-237`）。

### 1.5 日志路由（确证 audit.log 为何只有这两类内容）

`backend/src/main/resources/logback-spring.xml`：

- `L117-118`：`appender name="AUDIT"` → 文件 `${LOG_PATH}/audit.log`
- `L168-171`：`ASYNC_AUDIT` 包装 `AUDIT`
- `L275-277`（**prod** profile）：`<logger name="com.selfblog.utils.LogUtils" level="INFO" additivity="false"><appender-ref ref="ASYNC_AUDIT"/></logger>`
- `L317-319`（**default** profile，本次实际生效）：同一 logger → `CONSOLE` + `ASYNC_AUDIT`

**结论**：`audit.log` 内容 = `com.selfblog.utils.LogUtils` 这一个类的全部输出。凡是**不经过 LogUtils** 的业务日志（登录成功/失败、上传、密码修改、管理操作）都写进了 `application.log`，不会进 `audit.log`。这是审计覆盖不足的结构性根因。`【确证】`

### 1.6 全部 109 行中唯一的业务异常

```
L16: 2026-10-17 12:06:55.000 [http-nio-8888-exec-2] [f5087641e44a40bcbbceb7ce03460124] [1] [82.153.135.59] WARN  com.selfblog.utils.LogUtils - [SECURITY] event=BUSINESS_EXCEPTION, detail=文件内容与扩展名不匹配，请上传真实文件, clientIp=82.153.135.59
```

> 这是 audit.log 中**唯一**一条携带真实业务语义的事件（`detail` 非模板文本）。注意其 `[1]` 即 userId=1（管理员），且 request.log `L425-426` 对应 `POST /api/admin/site-assets status=400`，`application.log L240` 对应 `[EXCEPTION] Business: code=400, message=文件内容与扩展名不匹配，请上传真实文件`。`【确证】`

---

## 2. 登录成功/失败统计与暴力破解迹象

### 2.1 登录端点调用统计（request.log）

`POST /api/user/login` 子串命中 4 次 = **2 个 REQUEST + 2 个 RESPONSE**，即**整个时间窗内只有 2 次登录请求，且均成功**：

```
L115: 2026-10-17 08:29:40.138 [http-nio-8888-exec-2] [dab3da53838143fa857af7f5c339f327] [] [153.119.45.93] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] POST /api/user/login  from IP=153.119.45.93 UA=Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Mobile Safari/537.36
L116: 2026-10-17 08:29:42.895 [http-nio-8888-exec-2] [dab3da53838143fa857af7f5c339f327] [] [153.119.45.93] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] POST /api/user/login status=200 duration=2755ms clientIp=153.119.45.93
L405: 2026-10-17 12:05:46.436 [http-nio-8888-exec-7] [a328e24504ed4a44942366052f06d5f3] [] [82.153.135.59] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] POST /api/user/login  from IP=82.153.135.59 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36
L406: 2026-10-17 12:05:47.708 [http-nio-8888-exec-7] [a328e24504ed4a44942366052f06d5f3] [] [82.153.135.59] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] POST /api/user/login status=200 duration=1272ms clientIp=82.153.135.59
```

- **登录成功：2 次**（`status=200`）——`【确证】`
- **登录失败：0 次** —— request.log 中不存在任何非 200 的登录响应。`【确证】`
- 对应 audit.log 的耗时记录（同一 traceId 印证为同一次登录）：
  - `L7: 2026-10-17 08:29:42.900 ... [dab3da53838143fa857af7f5c339f327] [] [153.119.45.93] INFO  com.selfblog.utils.LogUtils - [PERFORMANCE] operation=POST /api/user/login, duration=2762ms`
  - `L14: 2026-10-17 12:05:47.709 ... [a328e24504ed4a44942366052f06d5f3] [] [82.153.135.59] INFO  com.selfblog.utils.LogUtils - [PERFORMANCE] operation=POST /api/user/login, duration=1273ms`
  - 两次耗时 2.7s / 1.3s 属密码哈希正常量级，**无异常慢速或快速失败特征**。`【确证】`

### 2.2 关键：登录失败根本没有被记录

- audit.log 中 `event=AUTH_FAILED` = **0**、`event=JWT_AUTH_FAILED` = **0**、`event=REFUSED_REFRESH_TOKEN` = **0**。
- 源码确认失败路径确实会调用审计（`backend/src/main/java/com/selfblog/common/exception/GlobalExceptionHandler.java`）：
  - `L211: log.warn("[EXCEPTION] Authentication: {}", e.getMessage());`
  - `L212: LogUtils.logSecurityEvent("AUTH_FAILED", e.getMessage(), LogUtils.getClientIp(request));`
  - `L201: log.warn("[EXCEPTION] AccessDenied: {}", e.getMessage());`
  - `L202: LogUtils.logSecurityEvent("ACCESS_DENIED", e.getMessage(), LogUtils.getClientIp(request));`
- `backend/src/main/java/com/selfblog/config/security/JwtAuthenticationFilter.java`：
  - `L91: log.warn("JWT Authentication failed: {}", e.getMessage());`
  - `L92: LogUtils.logSecurityEvent("JWT_AUTH_FAILED", e.getMessage(), ...)`
  - `L62: LogUtils.logSecurityEvent("REFUSED_REFRESH_TOKEN", ...)`
- `backend/src/main/java/com/selfblog/service/auth/impl/SysUserServiceImpl.java` 的密码错误分支（`L181: throw new BusinessException(401, "用户名/邮箱或密码错误")`、`L188`、`L207-208: // 密码错误，使用原子操作增加失败计数`）**只使用类内 `log.warn`，不调用任何 `LogUtils.logSecurityEvent`**。
  - 该 `log.warn` 落在 `com.selfblog` logger → `FILE` appender（`logback-spring.xml:235-239`），即写入 `application.log`，**不进 audit.log**。
  - 且 `application.log` 全量检索 `用户登录失败` / `登录失败` / `401` 均为 **0 命中** —— 说明本次时间窗内**确实没有发生过登录失败**（而非发生过但未记录）。

> **判定**：本次时间窗内没有暴力破解式的密码爆破（0 次登录失败、仅 2 次登录请求）。但「登录失败」这一事件类型在 `audit.log` 中**结构性地不可能出现**，因为失败路径不经过 `LogUtils`；若换一个时间窗发生了爆破，`audit.log` 同样会是 0 条。`【确证】`（对本次数据）；`【推测】`（对未来时间窗，基于源码路径判断）

### 2.3 非管理员账号尝试

- audit.log 的 99 次 `AUTH_ENTRY_POINT` 全部 `clientIp` 为公网 IP，**无任何一条携带 username/userId**（模板见 `SecurityConfig.java:139-140`：`LogUtils.logSecurityEvent("AUTH_ENTRY_POINT", authException.getMessage(), LogUtils.getClientIp(request))`，`authException.getMessage()` 恒为 `Full authentication is required to access this resource`）。
- **因此无法从 audit.log 判断是否有人尝试用非管理员账号登录** —— 这是审计字段缺失导致的取证不可能，而非「没有发生」。`【确证】`
- 可间接观察的是：`/api/admin/**` 被 `SecurityConfig.java:165` 限定 `.requestMatchers("/api/admin/**").hasRole("admin")`，而 request.log 中所有 `/api/admin/` 请求（**122 行命中**）**全部带 `[1]` userId 且全部 `status=200`**（检索「`/api/admin/` 且 `[REQUEST]` 且不含 `] [1] [`」→ **0 命中**），`status=403` 计数为 0，`status=401` 计数为 0 —— 未授权访问 `/api/admin/**` 的事件**在 request.log 中一条都没有**。`【确证】`

### 2.4 注册接口被调用

- request.log：`/register` 子串命中 **0**。
- audit.log：`[REGISTER]` 命中 **0**。
- `LogUtils.java:304 logRegister(...)` **无调用点**（`【确证】`）。
- 相关配置（`SecurityConfig.java:158`）显式放行注册端点以便返回明确 403：
  ```java
  .requestMatchers("/api/user/register", "/api/auth/register/**", "/user/register", "/auth/register/**").permitAll()
  ```
- **判定**：本次时间窗内无人调用注册接口；且即使被调用，`logRegister` 也不会被触发，注册尝试**无审计痕迹**。`【确证】`

---

## 3. 认证失败爆发：扫描器 34.156.121.46

### 3.1 audit.log 中该 IP 的 401 爆发

```
77 次 event=AUTH_ENTRY_POINT，全部来自 34.156.121.46
首次：2026-10-17 14:52:43.033
末次：2026-10-17 14:52:45.101
跨度：2.068 秒
```

样本（首尾及中段，原文）：

```
L27: 2026-10-17 14:52:43.033 [http-nio-8888-exec-9] [82a3667485824f32] [] [34.156.121.46] WARN  com.selfblog.utils.LogUtils - [SECURITY] event=AUTH_ENTRY_POINT, detail=Full authentication is required to access this resource, clientIp=34.156.121.46
L30: 2026-10-17 14:52:43.063 [http-nio-8888-exec-13] [51599cc5d0cb4620] [] [34.156.121.46] WARN  com.selfblog.utils.LogUtils - [SECURITY] event=AUTH_ENTRY_POINT, detail=Full authentication is required to access this resource, clientIp=34.156.121.46
L70: 2026-10-17 14:52:43.684 [http-nio-8888-exec-1] [2563c57624fe4606] [] [34.156.121.46] WARN  com.selfblog.utils.LogUtils - [SECURITY] event=AUTH_ENTRY_POINT, detail=Full authentication is required to access this resource, clientIp=34.156.121.46
L103: 2026-10-17 14:52:45.101 [http-nio-8888-exec-1] [9305d5d8caf0479b] [] [34.156.121.46] WARN  com.selfblog.utils.LogUtils - [SECURITY] event=AUTH_ENTRY_POINT, detail=Full authentication is required to access this resource, clientIp=34.156.121.46
```

- 并发特征：`L27-L103` 的 77 条记录分布在 **14 个不同的 Tomcat 工作线程**（`http-nio-8888-exec-1` … `exec-23`）上，是**并发批量请求**而非顺序浏览。`【确证】`
- 这是 audit.log 的 **77/99 = 78%**，即该文件的绝大部分内容由一次 2 秒的扫描事件构成。`【确证】`

### 3.2 request.log 中同一 IP 的实际探测目标

该 IP 在 request.log 中共 84 行 = **42 REQUEST + 42 RESPONSE**，状态码分布 **40×404 + 2×200**。

窗口：`14:52:41.630` → `14:52:45.363`。原始证据（节选，均为原文）：

```
L893: 2026-10-17 14:52:41.630 ... [34.156.121.46] INFO ... [REQUEST] GET /  from IP=34.156.121.46 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36
L897: 2026-10-17 14:52:43.689 ... [34.156.121.46] INFO ... [REQUEST] GET /Dockerfile  from IP=34.156.121.46 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
L898: 2026-10-17 14:52:43.694 ... [34.156.121.46] INFO ... [REQUEST] GET /_profiler/phpinfo  from IP=34.156.121.46 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
L899: 2026-10-17 14:52:43.701 ... [34.156.121.46] INFO ... [REQUEST] GET /admin/.env  from IP=34.156.121.46 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
L905: 2026-10-17 14:52:43.978 ... [34.156.121.46] WARN ... [RESPONSE] GET /_profiler/phpinfo status=404 duration=284ms clientIp=34.156.121.46
L906: 2026-10-17 14:52:44.007 ... [34.156.121.46] WARN ... [RESPONSE] GET /Dockerfile status=404 duration=318ms clientIp=34.156.121.46
L914: 2026-10-17 14:52:44.298 ... [34.156.121.46] INFO ... [REQUEST] GET /credentials  from IP=34.156.121.46
L916: 2026-10-17 14:52:44.387 ... [34.156.121.46] WARN ... [RESPONSE] GET /credentials status=404 duration=89ms clientIp=34.156.121.46
L936: 2026-10-17 14:52:44.710 ... [34.156.121.46] INFO ... [REQUEST] GET /phpinfo  from IP=34.156.121.46
L941: 2026-10-17 14:52:44.885 ... [34.156.121.46] WARN ... [RESPONSE] GET /phpinfo status=404 duration=176ms clientIp=34.156.121.46
L956: 2026-10-17 14:52:45.089 ... [34.156.121.46] INFO ... [REQUEST] GET /vendor/composer/installed.json  from IP=34.156.121.46
L969: 2026-10-17 14:52:45.223 ... [34.156.121.46] WARN ... [RESPONSE] GET /vendor/composer/installed.json status=404 duration=134ms clientIp=34.156.121.46
L894: 2026-10-17 14:52:41.752 ... [34.156.121.46] INFO ... [RESPONSE] GET / status=200 duration=122ms clientIp=34.156.121.46
L896: 2026-10-17 14:52:42.108 ... [34.156.121.46] INFO ... [RESPONSE] GET /assets/index-BNvWXfcT.js status=200 duration=73ms clientIp=34.156.121.46
L976: 2026-10-17 14:52:45.363 ... [34.156.121.46] WARN ... [RESPONSE] GET /wp-content/debug.log status=404 duration=261ms clientIp=34.156.121.46
```

**探测路径清单（25 个 `.env` 变体 + 无点号的 `/env` + 其他敏感文件）**，全部 `status=404`：

```
/admin/.env      /app/.env         /application/.env  /back/.env        /backend/.env
/backup/.env     /cms/.env         /config/.env       /core/.env        /demo/.env
/dev/.env        /env              /laravel/.env      /media/.env       /new/.env
/old/.env        /frontend/.env    /production/.env   /public/.env      /server/.env
/staging/.env    /src/.env         /test/.env         /v1/.env          /v2/.env
/web/.env
/config/aws.yml  /config/config.js /config/config.ini.php
/credentials     /Dockerfile       /phpinfo           /_profiler/phpinfo
/wp-content/debug.log
/fuel/app/config/db.php           /fuel/app/config/production/db.php
/app/config/local.php             /mautic/app/config/local.php
/matomo/config/config.ini.php
/vendor/composer/installed.json
```

> 这是典型的**多框架通用凭据窃取扫描器**特征（同时覆盖 Laravel `fuel/app/config`、Mautic、Matomo、Composer、WP debug.log、PHP `_profiler/phpinfo`）。全部返回 404，**未泄露任何文件**。`【确证】`

### 3.3 覆盖不一致（本次取证最重要的技术发现）

| 事件 | audit.log | request.log |
|------|-----------|-------------|
| 认证失败（HTTP 401） | **99 条** `AUTH_ENTRY_POINT` | **0 条** `status=401` |
| 权限拒绝（HTTP 403） | 0 条 `ACCESS_DENIED` | **0 条** `status=403` |
| 限流（HTTP 429） | 0 条 | **0 条** `status=429` |
| 34.156.121.46 请求 | 77 条 401 | 42 条（40×404 + 2×200） |

request.log 全量状态码分布（原文聚合）：

```
status=200 => 600
status=404 =>  46
status=304 =>   2
status=400 =>   1
status=500 =>   1
```

- `SecurityConfig.java:132-141` 的 `authenticationEntryPoint` 会 `response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)`（=401）并写 JSON。
- `HttpRequestLoggingInterceptor.java:63-85` 的 `afterCompletion` 会用 `response.getStatus()` 记录 `[RESPONSE] ... status={}`。
- 因此 99 次 401 **理论上都应出现在 request.log**，但实际为 0。`【确证】`（两侧计数）；**具体原因无法由现有日志确定** `【推测】`：可能是该 IP 的 401 请求在拦截器 `afterCompletion` 之前被 Spring Security 过滤器链短路，或响应提交路径绕过了 MVC 拦截器。
- **取证后果**：audit.log 只能告诉你「某 IP 认证失败了 N 次」，request.log 只能告诉你「某 IP 访问了哪些路径且大多 404」。**没有任何单一日志能把「哪个 IP 在哪个路径上失败」关联起来** —— 两个文件无法互相补全。`【确证】`

---

## 4. 管理端敏感操作异常分析

### 4.1 会话时间线（全部确证）

| IP | 首次出现 | 401 次数 | 401 窗口 | 登录 | 管理员操作 |
|----|---------|---------|---------|------|-----------|
| `153.119.45.93` | 08:28:48.857 | 5 | 08:28:48 → 08:31:38 | 08:29:40 → 200 | GET site-settings、**PUT site-settings**（08:31:05）、GET pages/page-navigation/visitors |
| `82.153.135.59` | 12:05:09.365 | 14 | 12:05:09 → 12:11:22（登录前 4 次） | 12:05:46 → 200 | PUT site-settings ×2、**POST site-assets ×7**、**POST albums ×1**、GET site-settings/visitors/pages/tags/guest-comments |
| `130.12.180.170` | 13:06:15.120 | 1 | 13:06:15 单点 | 无 | 无 |
| `34.156.121.46` | 14:52:43.033 | **77** | 14:52:43 → 14:52:45 | 无 | 无（扫描器） |
| `61.224.74.83` | 15:49:59.584 | 2 | 15:49:59 → 15:50:29 | **无记录** | GET visitors/summary（15:50:01）、**POST /api/post**（15:54:52） |

### 4.2 异常 1：82.153.135.59 的「登录前管理路径枚举」

登录发生前 22 秒，该 IP 探测了非标准后台路径（原文）：

```
L326: 2026-10-17 12:05:24.225 [http-nio-8888-exec-12] [0f978b3d04a746bf] [] [82.153.135.59] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET /admin status=404 duration=31ms clientIp=82.153.135.59
L356: 2026-10-17 12:05:30.629 [http-nio-8888-exec-10] [7203fc150c5349d3] [] [82.153.135.59] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET /zroa status=404 duration=42ms clientIp=82.153.135.59
```

随后：

```
L405: 2026-10-17 12:05:46.436 ... [82.153.135.59] ... [REQUEST] POST /api/user/login  from IP=82.153.135.59 UA=...Chrome/155.0.0.0...
L406: 2026-10-17 12:05:47.708 ... [RESPONSE] POST /api/user/login status=200 duration=1272ms clientIp=82.153.135.59
L419: 2026-10-17 12:06:20.033 ... [1] [82.153.135.59] ... [REQUEST] POST /api/admin/site-assets  from IP=82.153.135.59
L420: 2026-10-17 12:06:20.795 ... [RESPONSE] POST /api/admin/site-assets status=200 duration=762ms clientIp=82.153.135.59
```

- **事实链**：`/admin` 404 → `/zroa` 404 → 登录前 4 次 401 → 登录成功 → 2 分钟内执行 2 次站点设置写入与 7 次文件上传。`【确证】`
- `/zroa` 是**无意义的随机字符串路径**，正常用户不会访问；结合紧随其后的成功登录，形态上符合「先探测后台入口，再用已获取的凭据登录」。`【推测】`（无法证明 `/zroa` 与登录成功存在因果，也无法排除是同一作者在不同设备上试错）
- 该 IP 在整个窗口共 319 行 request.log 记录，是除 `82.153.135.59` 外的最大来源，属于**主要操作会话**。
- **审计缺口**：这一整条时间线在 audit.log 中**只留下 14 条无 URI 的 401 模板行**；站点设置写入与 7 次上传**零记录**。

### 4.3 异常 2：61.224.74.83 无登录记录即持有 admin 会话

```
L983: 2026-10-17 15:49:48.124 [http-nio-8888-exec-22] [ff0b97f2d96f47bc] [] [61.224.74.83] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] GET /  from IP=61.224.74.83 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36
L984: 2026-10-17 15:49:48.210 ... [RESPONSE] GET / status=200 duration=85ms clientIp=61.224.74.83
L1001: 2026-10-17 15:49:49.697 [http-nio-8888-exec-22] [1c80c299ea8a46a6ad837ebd8503a213] [1] [61.224.74.83] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] POST /api/visitor/track  from IP=61.224.74.83 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36
L1051: 2026-10-17 15:50:01.158 [http-nio-8888-exec-22] [d578e91ca6294310b3fc18fd91d7e4a0] [1] [61.224.74.83] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] GET /api/admin/visitors/summary  from IP=61.224.74.83 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36
L1052: 2026-10-17 15:50:01.348 ... [RESPONSE] GET /api/admin/visitors/summary status=200 duration=191ms clientIp=61.224.74.83
```

- `[REQUEST] GET /` 的 MDC userId 为空（`[]`），1.5 秒后 `[REQUEST] POST /api/visitor/track` 的 MDC userId 已是 `[1]`。`【确证】`
- 该 IP 在 request.log 全量中**没有任何 `POST /api/user/login`**（检索 `61\.224\.74\.83` 且含 `/api/user/` → 0 命中）。`【确证】`
- 期间 audit.log 记录该 IP **2 次** 401（`L104: 2026-10-17 15:49:59.584 ... clientIp=61.224.74.83`、`L105: 2026-10-17 15:50:29.888 ... clientIp=61.224.74.83`）—— 注意这 2 次 401 发生在**已持有 userId=1 会话之后**（15:49:59 晚于 15:49:49 的 `[1]` 请求）。`【确证】`
- **判定**：该 IP 未登录即持有管理员会话。最合理解释是 JWT 跨 IP 复用（例如同一作者在移动网络与宽带间切换，token 存于浏览器），但**也存在凭据被盗用的可能，现有日志无法区分**。`【确证】`（事实）；`【推测】`（成因）
- **审计缺口**：token 复用本身不产生任何审计事件（无 `[LOGIN]`、无 `ADMIN_OPERATION`、无 `[USER_ACTION]`）。

### 4.4 上传异常

**（a）内容与扩展名不匹配 —— 已记录（唯一有业务语义的审计行）**

```
audit.log L16: 2026-10-17 12:06:55.000 ... [1] [82.153.135.59] WARN ... [SECURITY] event=BUSINESS_EXCEPTION, detail=文件内容与扩展名不匹配，请上传真实文件, clientIp=82.153.135.59
request.log L425: 2026-10-17 12:06:54.920 ... [1] [82.153.135.59] ... [REQUEST] POST /api/admin/site-assets  from IP=82.153.135.59
request.log L426: 2026-10-17 12:06:55.019 ... [1] [82.153.135.59] WARN ... [RESPONSE] POST /api/admin/site-assets status=400 duration=99ms clientIp=82.153.135.59
application.log L240: 2026-10-17 12:06:54.998 ... [1] [82.153.135.59] WARN  c.selfblog.common.exception.GlobalExceptionHandler - [EXCEPTION] Business: code=400, message=文件内容与扩展名不匹配，请上传真实文件
```

> 这是三套日志**唯一一次完全对齐**的事件。上传校验生效，返回 400，未落盘。`【确证】`

**（b）两次 multipart 500 —— 未进 request.log**

```
application-error.log L3: 2026-10-17 12:09:49.691 [http-nio-8888-exec-1] [ee608e561c0849049ffaaff452c2d182] [1] [82.153.135.59] ERROR c.selfblog.common.exception.GlobalExceptionHandler - Unexpected error: 
L4: org.springframework.web.multipart.MultipartException: Failed to parse multipart servlet request
L5: Caused by: org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException: attachment
L7: 2026-10-17 12:10:01.898 [http-nio-8888-exec-6] [2a3a0968fac840a2a690ffcc3bd9d639] [1] [82.153.135.59] ERROR c.selfblog.common.exception.GlobalExceptionHandler - Unexpected error: 
L8: org.springframework.web.multipart.MultipartException: Failed to parse multipart servlet request
L9: Caused by: org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException: attachment
```

- 两次均为 `userId=[1]` 的管理员会话，**同一 IP、12 秒内连续两次**触发 multipart 文件数量超限。`【确证】`
- request.log 全量 `status=500` 仅 **1 条**（`L226: 2026-10-17 08:56:58.311 ... [185.180.141.5] ... [RESPONSE] GET /favicon.ico status=500 duration=54ms`），**这两次 multipart 500 均无对应 `[RESPONSE]` 记录**。`【确证】`
- **推测成因**：multipart 解析在请求进入 MVC 拦截器之前抛异常，`HttpRequestLoggingInterceptor` 的 `preHandle` 已执行但 `afterCompletion` 未能记录，或异常在过滤器链中直接被 `GlobalExceptionHandler` 处理。
- **取证后果**：**500 级错误在 request.log 中系统性缺失**（1 条 vs 实际 ≥3 条），而 `audit.log` 对 500 完全无感 —— 攻击者若用畸形 multipart 触发 500，两套日志都不会留下可关联的痕迹。`【确证】`

**（c）`FileCountLimitExceededException` 本身可疑**：连续两次在同一管理员会话上触发「附件数量超限」，可能是前端一次提交多文件时的正常边界，也可能是刻意构造的解析器探测。`【推测】` 无法由现有日志区分。

### 4.5 未记录的管理操作（源码级确认）

`backend/src/main/java/com/selfblog/service/auth/impl/SysUserServiceImpl.java` 中以下敏感操作只写普通业务日志，**不进 audit.log**：

- `L544: log.info("管理员修改用户状态: adminId={}, targetUserId={}, previousStatus={}, newStatus={}", ...)`
- `L563: log.info("管理员重置用户密码: adminId={}, targetUserId={}, targetUsername={}", ...)`

`backend/src/main/java/com/selfblog/service/post/impl/BlogPostServiceImpl.java`：

- `L432: log.info("管理员删除文章: postId={}, adminId={}, title={}", postId, adminId, post.getTitle());`

`backend/src/main/java/com/selfblog/service/social/impl/BlogCommentServiceImpl.java`：

- `L392: log.info("[AUDIT] 管理员删除评论: commentId={}, adminId={}, deletedCount={}", ...)` —— 注意该行**手工写了 `[AUDIT]` 前缀**，但它走的是 `com.selfblog` logger → `FILE` appender → `application.log`，**永远进不了 `audit.log`**。

> **判定**：改密码、改用户状态、删文章、删评论这些最高危的管理操作，全部只落在 `application.log`，`audit.log` 中 0 条。`【确证】`

---

## 5. 审计盲区清单（哪些安全事件没有被记录）

| 安全事件 | 期望记录位置 | 实际记录 | 证据 |
|---------|------------|---------|------|
| **限流触发 429** | security.log / audit.log / request.log | **三处全无**。`RateLimitInterceptor.java:61-66` 设 429 并写响应体，**类内无任何 logger**；`logback-spring.xml:279-281`（prod）虽为 `com.selfblog.config.web.RateLimitInterceptor` 配了 `ASYNC_SECURITY` appender，但该类从不写日志 | 【确证】 |
| **`/.env` 与凭据文件扫描** | audit.log（应作为安全事件） | **audit.log 0 条**。扫描的 42 次请求只在 request.log 留下 `[REQUEST]`/`[RESPONSE] 404`，属普通请求日志，无安全语义 | 【确证】 |
| **未授权 `/api/admin/**` 访问** | audit.log `ACCESS_DENIED` | **0 条**。`SecurityConfig.java:142-151` 的 `accessDeniedHandler` 会写 `event=ACCESS_DENIED`，但本次 0 次触发；request.log 中 `status=403` 也为 0 | 【确证】 |
| **认证失败（401）** | audit.log + request.log | audit.log 有 99 条（无 URI）；request.log **0 条 `status=401`** | 【确证】 |
| **登录成功** | audit.log `[LOGIN]` | **0 条**。`LogUtils` 中无 `logLoginSuccess`；`SysUserServiceImpl.login`（`L155`）成功后不调用任何 `LogUtils` 审计方法 | 【确证】 |
| **登录失败** | audit.log `AUTH_FAILED` | **0 条**。密码错误分支（`SysUserServiceImpl.java:181/188/207-208`）只 `log.warn`，不经过 `LogUtils` | 【确证】 |
| **注册尝试** | audit.log `[REGISTER]` | **0 条**；`LogUtils.logRegister`（`LogUtils.java:304`）**无调用点** | 【确证】 |
| **管理端操作（发布/删除/设置/上传/相册）** | audit.log `ADMIN_OPERATION` | **0 条**；`logAdminOperation`（`SecurityConfig.java:105`）**无调用点** | 【确证】 |
| **改密码 / 改用户状态 / 删文章 / 删评论** | audit.log | **0 条**；仅 `application.log` 中的普通 `log.info` | 【确证】 |
| **文件上传** | audit.log `[FILE_UPLOAD]` | **0 条**；`logFileUpload` **无调用点**。7 次 `POST /api/admin/site-assets` 仅 request.log 有记录 | 【确证】 |
| **JWT 认证失败** | audit.log `JWT_AUTH_FAILED` | **0 条**（`JwtAuthenticationFilter.java:92` 有调用但本次未触发；其 `L91 log.warn` 走 SECURITY appender → `security.log`，**本次取证未提供该文件**） | 【确证】 |
| **refresh token 被拒** | audit.log `REFUSED_REFRESH_TOKEN` | **0 条**（`JwtAuthenticationFilter.java:62` 有调用但未触发）；request.log 中 `/api/user/refresh` 命中 **0** | 【确证】 |
| **HTTP 500 级错误** | request.log `status=500` + application-error.log | application-error.log 有 2 条 multipart 500，request.log **无对应行** | 【确证】 |
| **token 跨 IP 复用** | 任何日志 | **无此事件类型**。61.224.74.83 持有 admin 会话但无登录记录，无任何日志对此告警 | 【确证】 |
| **请求目标（URI/method）随安全事件** | audit.log 字段 | 99 条 `AUTH_ENTRY_POINT` **全部不含 uri/method**；模板 `SecurityConfig.java:139-140` 只传 `authException.getMessage()` + IP | 【确证】 |
| **userId 随安全事件** | audit.log 字段 | 99 条中 98 条 MDC userId 为 `[]`；只有 `L16` 的 `BUSINESS_EXCEPTION` 带 `[1]` | 【确证】 |
| **`security.log` 本身** | `${LOG_PATH}/security.log`（`logback-spring.xml:71-73`） | **本次取证未提供该文件**。`com.selfblog.config.security.SecurityConfig` 与 `JwtAuthenticationFilter` 的日志、以及 prod 下的限流 logger 都指向它 —— 最关键的认证/授权事件可能全在该文件里 | 【确证】缺失 |
| **`performance.log`** | `${LOG_PATH}/performance.log` | 提供了 3,991 字节，但 `PerformanceMonitoringAspect` 在 **prod** profile 走 `ASYNC_PERFORMANCE`（`logback-spring.xml:252-254`）；本次实际为 **default** profile，`LogUtils.logPerformance` 也写进 audit.log（9 条） | 【确证】 |

---

## 6. 请求量与会话画像（request.log 补充）

**全量状态码分布**：

```
status=200 => 600
status=404 =>  46
status=304 =>   2
status=400 =>   1
status=500 =>   1
（合计 650 条 RESPONSE，对应 650 条 REQUEST，共 1300 行）
```

**按客户端 IP 聚合（request.log 行数）**：

| IP | 行数 | 性质 |
|----|------|------|
| `82.153.135.59` | 319 | 管理员主会话（含 7 次上传、2 次站点设置写入） |
| `61.224.74.83` | 157 | 管理员会话（**无登录记录**） |
| `153.119.45.93` | 108 | 管理员会话（移动端 UA） |
| `34.156.121.46` | 42 | **凭据扫描器** |
| 其余 24 个 IP | 各 1 | 单次探测（含 `185.180.141.2/4/5`，UA=`python-httpx/0.28.1` 请求 `/sse`；`91.231.89.169/102`，Firefox UA；`198.235.24.219/233`；`114.119.150.254/151.83`；多个 `43.x`/`49.51.x` 云厂商网段） |

**其他探测样本（原文）**：

```
L220: 2026-10-17 08:52:05.018 ... [16.216.88.134] WARN ... [RESPONSE] GET /archive/ status=404 duration=55ms clientIp=16.216.88.134
L223: 2026-10-17 08:56:57.842 ... [185.180.141.2] INFO ... [REQUEST] GET /sse  from IP=185.180.141.2 UA=python-httpx/0.28.1
L224: 2026-10-17 08:56:57.871 ... [185.180.141.2] WARN ... [RESPONSE] GET /sse status=404 duration=29ms clientIp=185.180.141.2
L225: 2026-10-17 08:56:58.256 ... [185.180.141.5] INFO ... [REQUEST] GET /favicon.ico  from IP=185.180.141.5 UA=Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Safari/537.36
L226: 2026-10-17 08:56:58.311 ... [185.180.141.5] WARN ... [RESPONSE] GET /favicon.ico status=500 duration=54ms clientIp=185.180.141.5
L434: 2026-10-17 12:07:50.130 ... [114.119.151.83] WARN ... [RESPONSE] GET /gallery/firefly-2026/ status=404 duration=64ms clientIp=114.119.151.83
L892: 2026-10-17 14:42:06.868 ... [16.216.88.196] WARN ... [RESPONSE] GET /archive/ status=404 duration=164ms clientIp=16.216.88.196
```

- `GET /favicon.ico status=500` 的根因（`application-error.log`）：
  ```
  L1: 2026-10-17 08:56:58.287 ... [185.180.141.5] ERROR c.selfblog.common.exception.GlobalExceptionHandler - Unexpected error: 
  L2: org.springframework.web.servlet.resource.NoResourceFoundException: No static resource favicon.ico.
  ```
  `SecurityConfig.java:191` 已 permitAll `/favicon.ico`，但文件不存在 → 抛 `NoResourceFoundException` → 兜底 500。**该路径被扫描器批量命中时会持续产生 500**。`【确证】`

---

## 7. 判定汇总

### 7.1 确证（有原始证据）

1. `audit.log` 全量 **109 行 / 26,159 字节**，99 条 `AUTH_ENTRY_POINT` + 1 条 `BUSINESS_EXCEPTION` + 9 条 `PERFORMANCE`；`ADMIN_OPERATION`/`[LOGIN]`/`[REGISTER]`/`[USER_ACTION]`/`[FILE_UPLOAD]`/`AUTH_FAILED`/`ACCESS_DENIED`/`JWT_AUTH_FAILED`/`REFUSED_REFRESH_TOKEN` **全部为 0**。
2. **审计覆盖严重不足**：7 次 `POST /api/admin/site-assets`、3 次 `PUT /api/admin/site-settings`、1 次 `POST /api/admin/albums`、1 次 `POST /api/post`（均按 `[REQUEST]` 行计数）、以及若干 `GET /api/admin/**` 全部真实发生（request.log `status=200`、MDC `[1]`），audit.log **零记录**。
3. 根因：`logAdminOperation`（`SecurityConfig.java:105`）、`logUserAction`（`LogUtils.java:207`）、`logRegister`（`LogUtils.java:304`）、`logFileUpload`（`LogUtils.java:305`）**均无调用点**；`audit.log` 只接收 `com.selfblog.utils.LogUtils` 这一个 logger 的输出（`logback-spring.xml:275-277` / `317-319`）。
4. 登录：**成功 2 次、失败 0 次**；无暴力破解证据。但失败路径（`SysUserServiceImpl.java:181/188/207-208`）不经 `LogUtils`，**结构性地不会进 audit.log**。
5. 注册：**0 次调用**；且 `logRegister` 无调用点 → 注册尝试无审计。
6. 扫描：`34.156.121.46` 42 次请求 / 40 次 404，含 **25 个 `.env` 变体**（另加 `/env`）与 `phpinfo`、`credentials`、`Dockerfile`、Composer/Mautic/Matomo/Laravel 配置探测；同 IP 在 audit.log 产生 **77 次 401，跨度 2.068 秒，跨 20+ 线程**。**未泄露任何文件**。
7. 覆盖不一致：audit.log **99 次 401** vs request.log **0 条 `status=401`**、0 条 `403`、0 条 `429`。
8. `82.153.135.59`：`GET /admin` 404 → `GET /zroa` 404 → 登录前 4 次 401 → 登录 200 → 2 分钟内 `PUT site-settings` ×2 + `POST site-assets` ×7 + `POST albums` ×1。
9. `61.224.74.83`：首条匿名请求 1.5 秒后即带 `[1]` 会话，**该 IP 无任何登录请求**。
10. 上传：1 次内容/扩展名不匹配（400，三套日志对齐）；2 次 `FileCountLimitExceededException`（500，**未进 request.log**）。
11. 限流 429 **无任何日志**（`RateLimitInterceptor.java` 无 logger）。
12. 500 级错误在 request.log 中系统性缺失（1 条 favicon vs application-error.log 的 2 条 multipart）。
13. 审计行**不含 URI/method/userId**（98/99 条 MDC userId 为空）。
14. 本次以 **`default` profile** 运行（`application.log` 首行），非 `prod`；`security.log` 未提供。
15. `favicon.ico` 缺失导致 500（`NoResourceFoundException: No static resource favicon.ico.`），可被扫描器批量触发。

### 7.2 推测（无法由现有日志闭合）

- `82.153.135.59` 的 `/admin` → `/zroa` → 登录成功序列，**形态上**像后台入口探测 + 凭据使用，但无法排除同一作者多设备试错。`【推测】`
- `61.224.74.83` 未登录即持有 admin 会话，最可能是 JWT 跨 IP 复用（同一作者网络切换），**亦可能是凭据被盗用**；现有日志无法区分。`【推测】`
- 99 次 401 未出现在 request.log 的具体机制（Spring Security 过滤器链短路 vs 响应提交绕过 MVC 拦截器）无法由日志确定。`【推测】`
- 两次 `FileCountLimitExceededException` 是正常多文件边界还是解析器探测，无法区分。`【推测】`

---

## 8. 建议（按优先级，均为防御性加固；本次未实施）

> 以下为**建议**，本次取证任务**未修改任何代码或配置**。

1. **让管理操作审计真正生效**：在 `SecurityConfig` 的过滤链中（或专门的 `OncePerRequestFilter`）对 `/api/admin/**` 的**所有非 GET 方法**调用现有 `logAdminOperation(action, request)`；否则该方法是死代码。`【最高优先】`
2. **把登录/注册/上传/改密纳入 `audit.log`**：为 `SysUserServiceImpl.login` 的成功与失败分支分别补 `logUserAction` / `logSecurityEvent("AUTH_FAILED", ...)`；为 `logRegister`、`logFileUpload` 补调用点。当前这四个方法全部无调用者。
3. **为 `AUTH_ENTRY_POINT` / `ACCESS_DENIED` 增加 URI + method + userId 字段**（`SecurityConfig.java:139-140`、`149-150`），否则安全事件无法定位到具体接口。
4. **限流触发必须留痕**：在 `RateLimitInterceptor.java:61-66` 返回 429 前调用 `LogUtils.logSecurityEvent("RATE_LIMITED", path + " count=" + current, ip)`；`logback-spring.xml:279-281` 已为其配好 `ASYNC_SECURITY` appender，只差代码侧调用。
5. **修复 request.log 的 401/403/500 缺失**：核查 `HttpRequestLoggingInterceptor.afterCompletion`（`HttpRequestLoggingInterceptor.java:63-85`）在 Spring Security 短路与 multipart 解析失败场景下是否被执行；当前 99 次 401、2 次 500 均未落盘。
6. **凭据扫描告警**：对命中 `.env` / `phpinfo` / `credentials` / `Dockerfile` / `vendor/composer` / `config/*.php` 的路径统一记 `event=CREDENTIAL_SCAN` 并接入告警，而不是当作普通 404。
7. **补齐 `favicon.ico`** 以消除可被批量触发的 500（`application-error.log` 已给出确切根因）。
8. **部署到 `prod` profile**：本次实际运行在 `default`，`RateLimitInterceptor` 的 SECURITY appender、`org.springframework.security` 的 WARN→ASYNC_SECURITY 等 prod 专属路由（`logback-spring.xml:269-281`）均未生效。同时容器使用 **Java 25.0.3**，而仓库目标为 **Java 21**（`CLAUDE.md`）。
9. **归档 `security.log`**：`logback-spring.xml:71-73` 定义的安全日志文件是认证/授权事件的主要去处；建议与 `audit.log` 一同纳入常规取证范围。

---

## 附：取证方法与边界

- 工具：`read`（全文逐段）、`grep`、`Select-String` 聚合（事件计数、按 IP 分组、状态码分布、按行号提取）。
- **只读**：未对 `C:\Users\Zora\Downloads\audit.log` 或任何 backend/frontend 文件执行写操作；本文件 `docs/reviews/prod-log-audit.md` 是唯一写入。
- **UTF-8 提示**：`audit.log` 与 `request.log` 内容为 ASCII 英文，无乱码；`application.log` 中的中文由 `Get-Content` 读取时出现乱码（如 `浣跨敤 SQLite 鏁版嵁搴?`），但不影响 ASCII 关键字检索（`[EXCEPTION]`、`Unexpected error`、类名等）。
- **未获取的文件**：`security.log`（`logback-spring.xml:71-73` 定义）未在 `C:\Users\Zora\Downloads` 中提供 —— 认证/授权事件的主要归档位置缺失，可能显著改变本报告对第 4/5 节的判定。`【确证】缺失`
- 日志行号引用均指对应文件的物理行号（`audit.log` 1-109、`request.log` 1-1300）。
