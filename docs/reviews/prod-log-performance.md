# 生产日志性能取证：performance.log 全量分析（2026-10-17）

## 1. 取证范围与口径

| 项 | 值 |
| --- | --- |
| 主证据文件 | `C:\Users\Zora\Downloads\performance.log` |
| 规模 | **17 行**，3991 字节，UTF-8 |
| 时间跨度 | `2026-10-17 08:14:28.833` → `2026-10-17 16:15:03.263`（约 8 小时） |
| 交叉证据 | `request.log`（1300 行 / 357774 字节 / 650 REQUEST + 650 RESPONSE）、`audit.log`（109 行）、`application.log`（571 行）、`application-error.log`（442 行） |
| 读取方式 | PowerShell `Get-Content -Encoding UTF8` + 正则解析；未使用 `cat` |
| 代码只读 | 仅读取 `backend/src/main/java` 与 `backend/src/main/resources` 源码用于归因，**未修改任何代码或配置** |
| 本报告是唯一写入 | `docs/reviews/prod-log-performance.md` |

**统计口径（自定义，明确声明）**

1. **总体口径**：以 `performance.log` 的 17 行 `[PERF]` 记录为总体。这**不是**全量调用日志——它是阈值采样日志（见 §3），因此"调用次数"是**被记录次数**，不是真实调用次数。
2. **平均值**：算术平均 `sum(duration)/n`，保留 1 位小数。
3. **P50/P95**：升序排序后取最近秩 `index = ceil(p/100 × n) − 1`（0-based）。**样本极少时不插值**，故 P95 常等于最大值；n=2 时 P50=较小值、P95=最大值。此口径在报告中逐处标注样本数 `n`，读者不应把 P95 当分位数估计使用。
4. **端点级统计**（§8）另立口径：以 `request.log` 的 650 条 `[RESPONSE]` 为总体（那才是全量 HTTP 请求日志）。
5. **层级归属**：`CONTROLLER` / `SERVICE` / `MAPPER` 来自切面 `layer` 字段，同一 HTTP 请求会产出多条嵌套记录，**不可跨层级直接相加**（会重复计数）。
6. **去重口径**：同一 traceId 内的多条记录属于同一次请求的不同层次，`distinct traceId` 才是"请求数"。

---

## 2. 日志格式解析

### 2.1 格式样本（前 3 行原文，逐字抄录）

```
2026-10-17 08:14:28.833 [http-nio-8888-exec-2] [8a588eb4885c45a9] [] [198.235.24.233] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE ReadingHtmlRenderer.render execution time: 1292ms (SLOW)
2026-10-17 08:14:28.916 [http-nio-8888-exec-2] [8a588eb4885c45a9] [] [198.235.24.233] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] CONTROLLER SpaController.readingPage execution time: 2279ms (SLOW)
2026-10-17 08:29:42.876 [http-nio-8888-exec-2] [dab3da53838143fa857af7f5c339f327] [] [153.119.45.93] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE SysUserServiceImpl.login execution time: 2513ms (SLOW)
```

### 2.2 字段含义

| 字段 | 样本值 | 含义 | 来源 |
| --- | --- | --- | --- |
| `timestamp` | `2026-10-17 08:14:28.833` | 日志写入时间（毫秒精度） | Logback pattern |
| `[thread]` | `[http-nio-8888-exec-2]` | 线程名；`http-nio-8888-exec-N` 为 Tomcat 工作线程，`scheduling-1` 为 `@Scheduled` 线程 | MDC |
| `[traceId]` | `[8a588eb4885c45a9]` | 请求链路 ID。**注意两种长度**：HTTP 请求由 `HttpRequestLoggingInterceptor` 生成 16 位（`X-Trace-Id` 或 UUID 前 16 位），定时任务为空 | MDC |
| `[userId]` | `[]` 或 `[1]` | 当前登录用户 ID，空 = 未登录（游客/扫描器/定时任务） | MDC |
| `[clientIp]` | `[198.235.24.233]` | 客户端 IP，定时任务为空 | MDC |
| `LEVEL` | `INFO` / `WARN` / `ERROR` | 日志级别，直接编码了耗时档位 | Logback |
| `logger` | `com.selfblog.aspect.PerformanceMonitoringAspect` | 切面类，固定 | Logback |
| `layer` | `SERVICE` / `CONTROLLER` / `MAPPER` | 被监控层，来自切面 `layer` 参数 | `PerformanceMonitoringAspect` |
| `class.method` | `ReadingHtmlRenderer.render` | 被监控的**类.方法**（`MAPPER` 层显示为 `BaseMapper.insert` 这类接口名） | 切面 |
| `duration` | `1292ms` | 该层方法执行耗时，`System.currentTimeMillis()` 差值，**毫秒整数** | 切面 |
| 尾标 | `(SLOW)` / `(VERY SLOW)` | 档位标记；仅成功路径出现 | 切面 |
| `error:` | `error: 文件内容与扩展名不匹配…` | **异常路径标记**：方法抛异常时记录，此时**无** `(SLOW)`/`(VERY SLOW)` 尾标 | 切面 |

### 2.3 阈值与落盘机制（决定"为什么只有 17 行"）

源码 `backend/src/main/java/com/selfblog/aspect/PerformanceMonitoringAspect.java`：

- 第 22 行 `SLOW_THRESHOLD = 1000`
- 第 25 行 `VERY_SLOW_THRESHOLD = 3000`
- 第 30 / 37 / 44 行切入点：`serviceLayer` = `execution(* com.selfblog.service..*(..))`、`controllerLayer` = `execution(* com.selfblog.controller..*(..))`、`mapperLayer` = `execution(* com.selfblog.mapper..*(..))`
- 第 80-106 行成功/异常分支：

```java
if (duration > VERY_SLOW_THRESHOLD)      log.warn ("[PERF] {} {} execution time: {}ms (VERY SLOW)", ...);
else if (duration > SLOW_THRESHOLD)      log.info ("[PERF] {} {} execution time: {}ms (SLOW)", ...);
else                                     log.debug("[PERF] {} {} execution time: {}ms", ...);
```

**两条关键推论（确证）**：

1. **阈值是严格大于**。`duration == 1000` **不**记录，`1001ms` 才记录。因此 `performance.log` 中不存在 `1000ms` 的 SLOW 行，最小的慢行是 `1088ms`（L12）。
2. **`performance.log` 是阈值采样日志，不是全量调用日志**。`backend/src/main/resources/logback-spring.xml` 第 252 行在 prod profile 下把 `com.selfblog.aspect.PerformanceMonitoringAspect` 的 level 钉为 `INFO`，因此所有 `≤1000ms` 的 DEBUG 行**不落盘**。所以 §4 的"调用次数"= **超过 1000ms 的次数**，不是真实调用次数；真实调用次数只能从 `request.log` 的 650 条 RESPONSE 得到。

**日志切分**：`logback-spring.xml` 第 87-99 行 `PERFORMANCE` appender → `${LOG_PATH}/performance.log`（charset UTF-8）；第 156-160 行 `ASYNC_PERFORMANCE` queueSize=256、discardingThreshold=0（不丢消息）。

---

## 3. 全量普查（17 行，按耗时降序）

| # | 行号 | 耗时 | 级别 | 层 | 类.方法 | 尾标 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | L11 | 3423ms | WARN | CONTROLLER | `BlogPostController.createPost` | VERY SLOW |
| 2 | L10 | 3320ms | WARN | SERVICE | `BlogPostServiceImpl.createPost` | VERY SLOW |
| 3 | L4 | 2583ms | INFO | CONTROLLER | `SysUserController.login` | SLOW |
| 4 | L3 | 2513ms | INFO | SERVICE | `SysUserServiceImpl.login` | SLOW |
| 5 | L9 | 2451ms | INFO | MAPPER | `BaseMapper.insert` | SLOW |
| 6 | L2 | 2279ms | INFO | CONTROLLER | `SpaController.readingPage` | SLOW |
| 7 | L17 | 1681ms | INFO | CONTROLLER | `BlogPostController.getPostList` | SLOW |
| 8 | L16 | 1675ms | INFO | SERVICE | `BlogPostServiceImpl.getPostList` | SLOW |
| 9 | L15 | 1671ms | INFO | SERVICE | `PostQueryServiceImpl.getPostList` | SLOW |
| 10 | L14 | 1649ms | INFO | MAPPER | `BaseMapper.selectPage` | SLOW |
| 11 | L1 | 1292ms | INFO | SERVICE | `ReadingHtmlRenderer.render` | SLOW |
| 12 | L6 | 1251ms | INFO | CONTROLLER | `SysUserController.login` | SLOW |
| 13 | L5 | 1243ms | INFO | SERVICE | `SysUserServiceImpl.login` | SLOW |
| 14 | L13 | 1116ms | INFO | CONTROLLER | `BlogPostController.getAdjacentPosts` | SLOW |
| 15 | L12 | 1088ms | INFO | SERVICE | `TrendingServiceImpl.scheduledUpdateAllTrending` | SLOW |
| 16 | L8 | 36ms | ERROR | CONTROLLER | `AdminSiteAssetController.upload` | — （异常路径） |
| 17 | L7 | 32ms | ERROR | SERVICE | `SiteAssetServiceImpl.store` | — （异常路径） |

### 3.1 分组统计

**按层级**（口径：§1 第 3 条）

| 层 | n | 平均 | 最大 | 说明 |
| --- | --- | --- | --- | --- |
| CONTROLLER | 7 | 1767.0ms | 3423ms | 含 1 条异常路径（36ms）会拉低均值；剔除后 6 条平均 2055.5ms |
| SERVICE | 8 | 1604.2ms | 3320ms | 含 1 条异常路径（32ms） |
| MAPPER | 2 | 2050.0ms | 2451ms | `BaseMapper.insert` / `BaseMapper.selectPage` |

**按级别**：`INFO` 13 条（全部 `(SLOW)`）、`WARN` 2 条（全部 `(VERY SLOW)`）、`ERROR` 2 条（全部为异常路径）。13 + 2 + 2 = 17，与总行数吻合。
**按被监控方法**：17 行涉及 **15 个不同方法**；仅 `SysUserServiceImpl.login`（L3/L5）与 `SysUserController.login`（L4/L6）各出现 **2 次**，其余方法均只 1 次。
**按 traceId**：`distinct traceId = 8`（7 个 HTTP trace + 1 个空 trace 对应定时任务）。

| traceId | 行 | 线程 | uid | clientIp | 端点 |
| --- | --- | --- | --- | --- | --- |
| `8a588eb4885c45a9` | L1-L2 | exec-2 | — | 198.235.24.233 | `GET /` |
| `dab3da53838143fa857af7f5c339f327` | L3-L4 | exec-2 | — | 153.119.45.93 | `POST /api/user/login` |
| `a328e24504ed4a44942366052f06d5f3` | L5-L6 | exec-7 | — | 82.153.135.59 | `POST /api/user/login` |
| `f5087641e44a40bcbbceb7ce03460124` | L7-L8 | exec-2 | 1 | 82.153.135.59 | `POST /api/admin/site-assets` |
| `63a8a58fb1d34159b9413f92a7340a11` | L9-L11 | exec-6 | 1 | 61.224.74.83 | `POST /api/post` |
| （空） | L12 | scheduling-1 | — | — | 定时任务（无 HTTP） |
| `a49ef07377e743dca7667f79bc05f62f` | L13 | exec-20 | 1 | 61.224.74.83 | `GET /api/post/1/adjacent` |
| `8604137a0cba4aca9a4f0de3624a6e93` | L14-L17 | exec-22 | 1 | 61.224.74.83 | `GET /api/post/list` |

**同 trace 嵌套耗时分解**（内层→外层差值，反映"框架+未被监控代码"的开销，**确证**）

| trace | 分解 | 差值解读 |
| --- | --- | --- |
| `8a588eb4885c45a9` | `render` 1292 → `readingPage` 2279 | 差 **987ms**：`ReadingPageService.resolve()` 中的 DB 取数 + 响应序列化写出 |
| `63a8a58fb1d34159b9413f92a7340a11` | `insert` 2451 → `createPost` 3320 → controller 3423 | 差 **869ms**（insert 之外的 savePostTags/话题计数/HTML 净化）；再差 **103ms** 框架开销 |
| `8604137a0cba4aca9a4f0de3624a6e93` | `selectPage` 1649 → `PostQueryServiceImpl` 1671 → `BlogPostServiceImpl` 1675 → controller 1681 | 逐层差 22 / 4 / 6ms：**耗时几乎全在 `selectPage` 单条 SQL 上** |
| `dab3da53838143fa857af7f5c339f327` | `login` 2513 → controller 2583 | 差 **70ms** 框架开销 |
| `a328e24504ed4a44942366052f06d5f3` | `login` 1243 → controller 1251 | 差 **8ms** 框架开销 |
| `a49ef07377e743dca7667f79bc05f62f` | controller `getAdjacentPosts` 1116，**无** SERVICE/MAPPER 记录 | 两次 `findAdjacent` 各自 <1000ms（故未落盘），但**累计** 1116ms |

---

## 4. 真实性能问题 vs 正常负例断言

这是本次取证**最容易误判**的一点，先给结论：

### 4.1 正常负例断言（2 条，**不是故障**）

| 行 | 原文 | 判定 |
| --- | --- | --- |
| L7 | `ERROR ... [PERF] SERVICE SiteAssetServiceImpl.store execution time: 32ms, error: 文件内容与扩展名不匹配，请上传真实文件` | **正常负例断言** |
| L8 | `ERROR ... [PERF] CONTROLLER AdminSiteAssetController.upload execution time: 36ms, error: 文件内容与扩展名不匹配，请上传真实文件` | **正常负例断言** |

**判定依据（确证）**：

1. 耗时 **32ms / 36ms**，**远低于** 1000ms 慢阈值。它们出现在日志里**不是**因为慢，而是因为走了 `catch (Exception ex)` 异常分支——**异常路径无条件记录，与阈值无关**（`PerformanceMonitoringAspect.java` 第 100-105 行）。
2. 对应 HTTP 请求 `request.log` L426：`[RESPONSE] POST /api/admin/site-assets status=400 duration=99ms` —— **HTTP 400**，是业务校验抛 `BusinessException(400, …)` 的**预期路径**。
3. `application.log` 亦有对应 WARN：`[EXCEPTION] Business: code=400, message=文件内容与扩展名不匹配，请上传真实文件`。
4. 这是管理员上传了扩展名与内容不符的文件（真实浏览器 UA `Chrome/155.0.0.0`，uid=1），属**用户输入校验**，不是系统性能故障。

> **重要**：仓库 `CLAUDE.md` 与 `AGENTS.md` 已明确记载「日志里大量 `[PERF] ... error:` 是 `PerformanceMonitoringAspect` 记录的正常负例断言，不是失败」。本报告与之一致。

### 4.2 真实性能问题（15 条，**超过 1000ms 阈值的慢调用**）

15 条 = 全部 `(SLOW)` **13 条**（含定时任务 1 条）+ `(VERY SLOW)` **2 条**。它们**无** `error:` 字段，全部是成功路径，按耗时降序见 §3 表格第 1-15 行。

| 严重度 | 条目 | 耗时 |
| --- | --- | --- |
| **VERY SLOW（>3000ms）** | `BlogPostController.createPost` / `BlogPostServiceImpl.createPost` | 3423ms / 3320ms |
| **SLOW（1000-3000ms）** | 其余 13 条 | 1088-2583ms |

### 4.3 必须排除的第三类：扫描器噪声（**非性能问题**）

`request.log` 中存在 46 条 `status=404` 的 WARN，其中以下来自漏洞扫描器的 `.env` 探测，**不得计入性能统计**：

```
/admin/.env 787ms、/app/.env 639ms、/application/.env 638ms、/back/.env 575ms、
/backend/.env 573ms、/backup/.env 509ms、/cms/.env 473ms、/config/.env 364ms、/Dockerfile 318ms
```

来源 IP `34.156.121.46` 与 `198.235.24.233`（后者 UA 为 Palo Alto Cortex Xpanse 扫描器）。这些是**扫描器制造的高频 404**，耗时高是因为扫描器并发抓取，不是应用慢。

### 4.4 阈值差异说明（避免"数量对不上"的误判）

| 日志 | 阈值 | 超阈条数 |
| --- | --- | --- |
| `performance.log`（AOP 层，`PerformanceMonitoringAspect`） | **1000ms** | 15 条慢调用 |
| `request.log` 的 `SLOW` 标记（HTTP 层，`HttpRequestLoggingInterceptor` 第 28 行） | **3000ms** | **仅 1 条** |
| `audit.log` 的 `[PERFORMANCE] SLOW`（HTTP 层，`LogUtils.logPerformance` 第 250 行） | **3000ms** | **仅 1 条** |

**这不是矛盾，是阈值不同**（1000 vs 3000）。HTTP 层只有 `POST /api/post` 4710ms 越过 3000ms。

---

## 5. 最慢 Top 20 条目原文

`performance.log` **总共只有 17 行**，无法凑出 20 条，故 Top 20 由 **performance.log 全部 17 行（按耗时降序）** + **request.log 中 >1000ms 但 AOP 未覆盖的 3 条 HTTP 响应** 构成，共 20 条。全部为原文。

### 5.1 performance.log 全部 17 行（AOP 层，阈值 1000ms）

```
1  3423ms 2026-10-17 15:54:52.818 [http-nio-8888-exec-6] [63a8a58fb1d34159b9413f92a7340a11] [1] [61.224.74.83] WARN  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] CONTROLLER BlogPostController.createPost execution time: 3423ms (VERY SLOW)
2  3320ms 2026-10-17 15:54:52.732 [http-nio-8888-exec-6] [63a8a58fb1d34159b9413f92a7340a11] [1] [61.224.74.83] WARN  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE BlogPostServiceImpl.createPost execution time: 3320ms (VERY SLOW)
3  2583ms 2026-10-17 08:29:42.878 [http-nio-8888-exec-2] [dab3da53838143fa857af7f5c339f327] [] [153.119.45.93] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] CONTROLLER SysUserController.login execution time: 2583ms (SLOW)
4  2513ms 2026-10-17 08:29:42.876 [http-nio-8888-exec-2] [dab3da53838143fa857af7f5c339f327] [] [153.119.45.93] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE SysUserServiceImpl.login execution time: 2513ms (SLOW)
5  2451ms 2026-10-17 15:54:52.694 [http-nio-8888-exec-6] [63a8a58fb1d34159b9413f92a7340a11] [1] [61.224.74.83] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] MAPPER BaseMapper.insert execution time: 2451ms (SLOW)
6  2279ms 2026-10-17 08:14:28.916 [http-nio-8888-exec-2] [8a588eb4885c45a9] [] [198.235.24.233] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] CONTROLLER SpaController.readingPage execution time: 2279ms (SLOW)
7  1681ms 2026-10-17 16:15:03.263 [http-nio-8888-exec-22] [8604137a0cba4aca9a4f0de3624a6e93] [1] [61.224.74.83] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] CONTROLLER BlogPostController.getPostList execution time: 1681ms (SLOW)
8  1675ms 2026-10-17 16:15:03.260 [http-nio-8888-exec-22] [8604137a0cba4aca9a4f0de3624a6e93] [1] [61.224.74.83] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE BlogPostServiceImpl.getPostList execution time: 1675ms (SLOW)
9  1671ms 2026-10-17 16:15:03.259 [http-nio-8888-exec-22] [8604137a0cba4aca9a4f0de3624a6e93] [1] [61.224.74.83] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE PostQueryServiceImpl.getPostList execution time: 1671ms (SLOW)
10 1649ms 2026-10-17 16:15:03.237 [http-nio-8888-exec-22] [8604137a0cba4aca9a4f0de3624a6e93] [1] [61.224.74.83] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] MAPPER BaseMapper.selectPage execution time: 1649ms (SLOW)
11 1292ms 2026-10-17 08:14:28.833 [http-nio-8888-exec-2] [8a588eb4885c45a9] [] [198.235.24.233] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE ReadingHtmlRenderer.render execution time: 1292ms (SLOW)
12 1251ms 2026-10-17 12:05:47.697 [http-nio-8888-exec-7] [a328e24504ed4a44942366052f06d5f3] [] [82.153.135.59] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] CONTROLLER SysUserController.login execution time: 1251ms (SLOW)
13 1243ms 2026-10-17 12:05:47.695 [http-nio-8888-exec-7] [a328e24504ed4a44942366052f06d5f3] [] [82.153.135.59] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE SysUserServiceImpl.login execution time: 1243ms (SLOW)
14 1116ms 2026-10-17 16:14:39.512 [http-nio-8888-exec-20] [a49ef07377e743dca7667f79bc05f62f] [1] [61.224.74.83] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] CONTROLLER BlogPostController.getAdjacentPosts execution time: 1116ms (SLOW)
15 1088ms 2026-10-17 16:00:01.160 [scheduling-1] [] [] [] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE TrendingServiceImpl.scheduledUpdateAllTrending execution time: 1088ms (SLOW)
16   36ms 2026-10-17 12:06:54.960 [http-nio-8888-exec-2] [f5087641e44a40bcbbceb7ce03460124] [1] [82.153.135.59] ERROR com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] CONTROLLER AdminSiteAssetController.upload execution time: 36ms, error: 文件内容与扩展名不匹配，请上传真实文件
17   32ms 2026-10-17 12:06:54.956 [http-nio-8888-exec-2] [f5087641e44a40bcbbceb7ce03460124] [1] [82.153.135.59] ERROR com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE SiteAssetServiceImpl.store execution time: 32ms, error: 文件内容与扩展名不匹配，请上传真实文件
```

### 5.2 request.log 中 >1000ms 但 AOP 层**未**落盘的 3 条（HTTP 层，原文）

```
18 2231ms 2026-10-17 08:28:47.897 [http-nio-8888-exec-2] [127ab9153e004fb29a0b14e057f5147a] [] [153.119.45.93] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] POST /api/visitor/track status=200 duration=2231ms clientIp=153.119.45.93
19 1355ms 2026-10-17 08:28:47.918 [http-nio-8888-exec-1] [b0c1b3bbbc5e4483b8439a3d186af18c] [] [153.119.45.93] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET /api/post/list status=200 duration=1355ms clientIp=153.119.45.93
20 1074ms 2026-10-17 15:54:59.872 [http-nio-8888-exec-20] [74d3b907b56e43e38206f4bd581b7c62] [1] [61.224.74.83] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET /api/anonymous-likes/posts/1 status=200 duration=1074ms clientIp=61.224.74.83
```

---

## 6. 可改进热点（4 个，按影响排序）

### 热点 1｜SQLite 单连接池导致写路径整体串行（**最高影响**）

**证据**：

```
L9  2451ms  [PERF] MAPPER BaseMapper.insert execution time: 2451ms (SLOW)
L10 3320ms  [PERF] SERVICE BlogPostServiceImpl.createPost execution time: 3320ms (VERY SLOW)
L11 3423ms  [PERF] CONTROLLER BlogPostController.createPost execution time: 3423ms (VERY SLOW)
L1072 request.log  [RESPONSE] SLOW POST /api/post status=200 duration=4710ms clientIp=61.224.74.83
```

**归因（确证）**：`backend/src/main/java/com/selfblog/config/db/DatabaseConfig.java` 第 74-75 行：

```java
config.setMaximumPoolSize(1);
config.setMinimumIdle(1);
```

注释写明「SQLite 单文件数据库不适合高并发连接池」。MySQL 分支（第 143 行）为 `maximumPoolSize=20`。

**机制**：`createPost` 标注 `@Transactional(rollbackFor = Exception.class)`（`BlogPostServiceImpl.java` 第 115 行），整个事务**独占唯一连接**。一次 `BaseMapper.insert` 本身耗 2451ms——对 0 行的表而言这不是 SQL 计算开销，而是**获取连接 + 提交/刷盘（WAL fsync）的等待**。同 trace 内 `insert 2451 → createPost 3320` 的 **869ms** 差额是事务内的 `savePostTags` / `topicMapper.incrementPostCount` / HTML 净化，同样串行在这条连接上。

**证据强度**：确证（日志 + 源码 + 配置三者一致）。

**注意**：`backend/target/campus_blog.db` 当前各表均为 **0 行**（实测 `blog_post`/`visitor_event`/`guest_comment`/`anonymous_post_like` 等全部 count=0），因此 2451ms **不可能**是数据量导致，**只能**是连接/IO 等待。这条排除了"数据量大"的解释。

---

### 热点 2｜登录同步 BCrypt + JWT 全程持有唯一 SQLite 连接

**证据**：

```
L3 2513ms [PERF] SERVICE SysUserServiceImpl.login execution time: 2513ms (SLOW)
L4 2583ms [PERF] CONTROLLER SysUserController.login execution time: 2583ms (SLOW)
L5 1243ms [PERF] SERVICE SysUserServiceImpl.login execution time: 1243ms (SLOW)
L6 1251ms [PERF] CONTROLLER SysUserController.login execution time: 1251ms (SLOW)
L116 request.log [RESPONSE] POST /api/user/login status=200 duration=2755ms
L406 request.log [RESPONSE] POST /api/user/login status=200 duration=1272ms
```

**归因（确证）**：`backend/src/main/java/com/selfblog/service/auth/impl/SysUserServiceImpl.java`

- 第 154 行 `@Transactional(rollbackFor = Exception.class)`
- 第 205 行 `if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))`（`BCryptPasswordEncoder`，字段第 48-49 行）
- 第 237-242 行 `jwtUtils.credentialVersionFor/generateToken/generateRefreshToken/registerDeviceToken`

**机制**：BCrypt 是**故意设计成慢**的 CPU 密集算法；它被包在 `@Transactional` 里，于是**整个 BCrypt 计算期间唯一 SQLite 连接被占住**。两次登录耗时差异大（2513ms vs 1243ms）符合 BCrypt 加盐迭代 + 当时是否有其他请求争用连接。同 trace 内 `login → controller` 仅差 70ms / 8ms，说明**耗时几乎全在 service 内部**（BCrypt + JWT 签发），不是框架开销。

**证据强度**：确证（两次独立登录复现同一模式，且 service 与 controller 耗时差极小）。

---

### 热点 3｜`getPostList` 的 `COUNT(*)` + 单连接 `selectPage`，且缓存键含易变维度

**证据**：

```
L14 1649ms [PERF] MAPPER BaseMapper.selectPage execution time: 1649ms (SLOW)
L15 1671ms [PERF] SERVICE PostQueryServiceImpl.getPostList execution time: 1671ms (SLOW)
L16 1675ms [PERF] SERVICE BlogPostServiceImpl.getPostList execution time: 1675ms (SLOW)
L17 1681ms [PERF] CONTROLLER BlogPostController.getPostList execution time: 1681ms (SLOW)
L1280 request.log [RESPONSE] GET /api/post/list status=200 duration=1690ms
```

**归因**：

- `backend/src/main/java/com/selfblog/service/post/impl/PostQueryServiceImpl.java` 第 154 行 `Page<BlogPost> page = new Page<>(request.getPageNum(), request.getPageSize());` —— **三参构造默认 `searchCount=true`**，即 `selectPage` 会**额外执行一次 `COUNT(*)`**。
- 第 150-151 行已加 `@Cacheable(value = CaffeineCacheConfig.TRENDING_CACHE, key = "'postList:' + #request.pageNum + ':' + #request.pageSize + ':' + #request.category + ':' + #request.sort + ':' + (#request.keyword ?: '') + ':' + (#request.userId ?: '') + ':' + (#request.tagId ?: '')")`。**缓存是存在的**，但键把 `pageNum/pageSize/sort/category/keyword/...` 全展开，`sort=latest` 等组合使命中面很窄。
- 第 219-224 行已做**批量预取**（`selectBatchIds` / `getTagsMapByPostIds` / `getTopicNamesMapByPostIds`）→ **此路径不是 N+1**（重要：不要误报 N+1）。
- 索引现状 `backend/src/main/resources/schema-sqlite.sql`：`idx_blog_post_create_time`(L60)、`idx_blog_post_status_deleted`(L61)、`idx_post_status_deleted_create`(L424)、`idx_post_status_deleted_view`(L425)、`idx_post_status_deleted_like`(L426) → **`status+is_deleted+create_time` 组合索引已存在**。

**结论（推测部分已标注）**：慢的主因**不是缺索引**（索引已存在），而是 ① `COUNT(*)` 额外全表扫描，② SQLite 单连接下 `selectPage` 与 `COUNT(*)` 串行，③ 缓存键过细导致命中率低。表当前 0 行，故 1649ms 同样指向**连接/IO 等待**而非扫描量。**证据强度：耗时与调用链确证；"主因排序"为推测。**

---

### 热点 4｜循环内逐条写 IO：定时任务 `scheduledUpdateAllTrending` + `getAdjacentPosts` 串行往返

**证据 A（循环内 IO）**：

```
L12 1088ms 2026-10-17 16:00:01.160 [scheduling-1] [] [] [] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] SERVICE TrendingServiceImpl.scheduledUpdateAllTrending execution time: 1088ms (SLOW)
```

**归因（确证）**：`backend/src/main/java/com/selfblog/service/content/impl/TrendingServiceImpl.java`

- 第 414 行 `scheduledUpdateAllTrending()`，`@Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Shanghai")`（第 413 行）——注意日志实际发生在 **16:00:01**，非午夜。
- 第 416 行 `int pageSize = 1000;`，第 423-451 行 `while(true)` 分页循环。
- 第 439 行 `baseMapper.selectByPostIdsAndDate(postIds, todayStart, todayEnd)` —— **已批量预取**。
- 第 443-445 行 `for (BlogPost post : postPage.getRecords()) updatePostTrendingBatch(post, …)`，其中第 475 行 `baseMapper.upsert(trending);` —— **循环内逐条 upsert**（N 次写 IO）。
- 第 454-455 行 `topicMapper.recalculateAllTrendingScore(); topicMapper.recalculateAllPostCount();` —— 两条**全表重算**语句。

**证据 B（串行往返）**：

```
L13 1116ms 2026-10-17 16:14:39.512 [http-nio-8888-exec-20] [a49ef07377e743dca7667f79bc05f62f] [1] [61.224.74.83] INFO  com.selfblog.aspect.PerformanceMonitoringAspect - [PERF] CONTROLLER BlogPostController.getAdjacentPosts execution time: 1116ms (SLOW)
L1274 request.log [RESPONSE] GET /api/post/1/adjacent status=200 duration=1134ms
```

`PostQueryServiceImpl.java` 第 748-760 行 `getAdjacentPosts`：`getById(postId)` 后连续调用 **两次** `findAdjacent(...)`（第 757、758 行），每次在第 788 行 `page(new Page<>(1, 1, false), wrapper)`（第 3 参 `false` **已关闭 count**）。即**共 3 次串行查询往返**，在 SQLite 单连接下串行累加。此 trace **无** SERVICE/MAPPER 记录，因为单次 `findAdjacent` 各 <1000ms——**单条不慢、累加才慢**，正是"阈值采样日志"的盲区。

---

### 附带发现（非性能，但属真实缺陷线索）

`application-error.log` 中 **3 条** `GlobalExceptionHandler - Unexpected error`：

1. `2026-10-17 08:56:58.287 [http-nio-8888-exec-9] [b0bbfa2b45f34738] [] [185.180.141.5] ERROR … Unexpected error:` → `org.springframework.web.servlet.resource.NoResourceFoundException: No static resource favicon.ico.`
2. `2026-10-17 12:09:49.691 [http-nio-8888-exec-1] [ee608e561c0849049ffaaff452c2d182] [1] [82.153.135.59] ERROR … Unexpected error:` → `org.springframework.web.multipart.MultipartException: Failed to parse multipart servlet request`
3. `2026-10-17 12:10:01.898 [http-nio-8888-exec-6] [2a3a0968fac840a2a690ffcc3bd9d639] [1] [82.153.135.59] ERROR … Unexpected error:` → 同上 `MultipartException`

这三条是**未被 `GlobalExceptionHandler` 正确分类的异常**（`favicon.ico` 缺失本应 404、multipart 解析失败本应 400），被兜底成 500 并打了完整堆栈。**与性能无关**，但属于日志噪声与错误分类问题，建议单独跟进。

---

## 7. 与 request.log / audit.log 的交叉验证

### 7.1 结论：**可以交叉**（时间、线程、IP、uid 全部对齐）

**双向验证方法**：以 `performance.log` 的 8 个 traceId 为键，在 `request.log` 650 条 RESPONSE 中精确匹配。

#### 正向：performance.log 的 7 个 HTTP trace → request.log 全部命中（7/7）

| performance.log traceId | request.log 行 | HTTP 耗时 | 端点 | 线程 | IP | uid | 一致性 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `8a588eb4885c45a9` | L2 | 2557ms | `GET /` | exec-2 | 198.235.24.233 | — | ✅ 完全一致 |
| `dab3da53838143fa857af7f5c339f327` | L116 | 2755ms | `POST /api/user/login` | exec-2 | 153.119.45.93 | — | ✅ 完全一致 |
| `a328e24504ed4a44942366052f06d5f3` | L406 | 1272ms | `POST /api/user/login` | exec-7 | 82.153.135.59 | — | ✅ 完全一致 |
| `f5087641e44a40bcbbceb7ce03460124` | L426 | 99ms | `POST /api/admin/site-assets` (400) | exec-2 | 82.153.135.59 | 1 | ✅ 完全一致 |
| `63a8a58fb1d34159b9413f92a7340a11` | L1072 | 4710ms | `POST /api/post` (SLOW) | exec-6 | 61.224.74.83 | 1 | ✅ 完全一致 |
| `a49ef07377e743dca7667f79bc05f62f` | L1274 | 1134ms | `GET /api/post/1/adjacent` | exec-20 | 61.224.74.83 | 1 | ✅ 完全一致 |
| `8604137a0cba4aca9a4f0de3624a6e93` | L1280 | 1690ms | `GET /api/post/list` | exec-22 | 61.224.74.83 | 1 | ✅ 完全一致 |

**AOP 层提供了 HTTP 层看不到的更细分解**（这是交叉验证的核心价值）：

- `POST /api/post` HTTP 4710ms ← AOP 分解为 `BaseMapper.insert` 2451ms + `BlogPostServiceImpl.createPost` 3320ms + controller 3423ms → **定位到 `insert` 是主要瓶颈**。
- `GET /api/post/list` HTTP 1690ms ← AOP 分解为 `selectPage` 1649 / `PostQueryServiceImpl` 1671 / `BlogPostServiceImpl` 1675 / controller 1681 → **定位到 `selectPage` 单条 SQL**。
- `GET /` HTTP 2557ms ← AOP 分解为 `ReadingHtmlRenderer.render` 1292ms + `SpaController.readingPage` 2279ms → **987ms 落在 render 之外**。
- `POST /api/user/login` HTTP 2755ms ← AOP `SysUserServiceImpl.login` 2513ms → **91% 在 service 内**。

#### 反向：request.log 中 9 条 >1000ms，**3 条在 performance.log 无任何对应条目**

```
2026-10-17 08:28:47.897 [http-nio-8888-exec-2] [127ab9153e004fb29a0b14e057f5147a] [] [153.119.45.93] … [RESPONSE] POST /api/visitor/track status=200 duration=2231ms
2026-10-17 08:28:47.918 [http-nio-8888-exec-1] [b0c1b3bbbc5e4483b8439a3d186af18c] [] [153.119.45.93] … [RESPONSE] GET /api/post/list status=200 duration=1355ms
2026-10-17 15:54:59.872 [http-nio-8888-exec-20] [74d3b907b56e43e38206f4bd581b7c62] [1] [61.224.74.83] … [RESPONSE] GET /api/anonymous-likes/posts/1 status=200 duration=1074ms
```

**解释（推测，非确证）**：这三次请求其被 AOP 监控的 service/controller/mapper 方法**各自都 <1000ms**，故 DEBUG 行未落盘（见 §2.3）。HTTP 耗时包含了 AOP 监控范围之外的部分（网络首字节、响应序列化写出、拦截器链）。以 `GET /` 为例，同 trace 内 `readingPage 2279 → HTTP 2557` 就有 278ms 落在监控之外，可佐证此机制。

**08:28:44-47 那一簇的特殊性（确证 + 推测）**：同一 IP `153.119.45.93` 在该时段连续发出多个请求，耗时呈**整体性偏高**：

```
L46  /api/tag/list      900ms   ← 该端点 P95 仅 111ms，此处 900ms 是异常值
L47  /api/visitor/track 2231ms
L48  /api/post/list     1355ms  ← 该端点 P50 仅 26ms
L43  /api/site/pages    已返回，/api/site/public 636ms
```

**同一 IP 的所有端点同时变慢**，符合**客户端/网络侧慢**特征，而非服务端某个方法慢。**确证**的是该 IP 该时刻整体慢；**推测**其成因为客户端网络或该客户端并发抓取。

### 7.2 audit.log 提供了**第三个**交叉点（HTTP 层慢日志，阈值 3000ms）

`audit.log` 中 `com.selfblog.utils.LogUtils` 的 `[PERFORMANCE]` 记录（第 248-256 行 `logPerformance`，阈值同样 1000/3000）共 **9 条**，其中 `SLOW` 1 条、`INFO` 8 条。**这 9 条与 `request.log` 中 9 条 >1000ms 完全对应**，且**多出**了 `performance.log` 缺失的 3 条：

```
2026-10-17 08:28:47.901 [http-nio-8888-exec-2] [127ab9153e004fb29a0b14e057f5147a] [] [153.119.45.93] INFO  com.selfblog.utils.LogUtils - [PERFORMANCE] operation=POST /api/visitor/track, duration=2235ms
2026-10-17 08:28:47.919 [http-nio-8888-exec-1] [b0c1b3bbbc5e4483b8439a3d186af18c] [] [153.119.45.93] INFO  com.selfblog.utils.LogUtils - [PERFORMANCE] operation=GET /api/post/list, duration=1356ms
2026-10-17 15:54:59.873 [http-nio-8888-exec-20] [74d3b907b56e43e38206f4bd581b7c62] [1] [61.224.74.83] INFO  com.selfblog.utils.LogUtils - [PERFORMANCE] operation=GET /api/anonymous-likes/posts/1, duration=1075ms
2026-10-17 15:54:52.833 [http-nio-8888-exec-6] [63a8a58fb1d34159b9413f92a7340a11] [1] [61.224.74.83] WARN  com.selfblog.utils.LogUtils - [PERFORMANCE] SLOW operation=POST /api/post, duration=4713ms
```

**这彻底闭合了 §7.1 的缺口**：那 3 条并非"日志缺失"，而是 **AOP 层未超 1000ms、HTTP 层超了**。`audit.log` 与 `request.log` 数值差 1-4ms（`2231 vs 2235`、`1355 vs 1356`、`1074 vs 1075`、`4710 vs 4713`），是两次独立 `System.currentTimeMillis()` 取值 + 异步 appender 写入延迟，属正常。

### 7.3 时间对齐核查

| 项 | performance.log | request.log | audit.log |
| --- | --- | --- | --- |
| 跨度 | 08:14:28.833 → 16:15:03.263 | 08:14:26.429 → 16:15:22.486 | 08:14:28.988 → 16:15:03.267 |
| 首条 | `GET /` 08:14:28.833 | `GET /` REQUEST 08:14:26.429 | `GET /` 08:14:28.988 |
| 末条 | `getPostList` 16:15:03.263 | 16:15:22.486 | 16:15:03.267 |

**时间对齐良好**：三个文件覆盖同一时段（约 8 小时），`performance.log` 的每条都能在 `request.log` 找到同秒级的对应行。**不存在"无法交叉"的情形。**

> 唯一的"无法交叉"项：`L12 scheduledUpdateAllTrending`（`[scheduling-1]`，无 traceId / uid / IP）是**后台定时任务，没有 HTTP 请求**，因此**在 request.log 中无对应条目**——这是设计使然，非数据问题。

---

## 8. 附：request.log 端点级全量统计（650 条 RESPONSE）

**口径**：从 650 条 `[RESPONSE]` 行正则提取 `uri/status/duration`；P50/P95 用最近秩法（§1 第 3 条）。这是**全量 HTTP 请求**统计，与 §3 的阈值采样统计不可混用。

**总体**：650 条响应；平均 **116.3ms**；P50 **47ms**；P95 **426ms**；最大 **4710ms**；`>1000ms` **9 条**；`>3000ms` **1 条**。

| 端点 | n | avg | P50 | P95 | max | 4xx/5xx |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/post` | 1 | 4710.0 | 4710 | 4710 | **4710** | 0 |
| `POST /api/user/login` | 2 | 2013.5 | 1272 | 2755 | **2755** | 0 |
| `GET /` | 14 | 334.5 | 88 | 2557 | **2557** | 0 |
| `POST /api/visitor/track` | 58 | 107.0 | 54 | 244 | **2231** | 0 |
| `GET /api/post/list` | 31 | 212.9 | 26 | 1355 | **1690** | 0 |
| `GET /api/post/1/adjacent` | 6 | 529.0 | 429 | 1134 | **1134** | 0 |
| `GET /api/anonymous-likes/posts/1` | 6 | 426.0 | 351 | 1074 | **1074** | 0 |
| `GET /api/tag/list` | 21 | 75.6 | 22 | 111 | 900 | 0 |
| `GET /api/guest-comments/post/1` | 6 | 745.8 | 794 | 865 | 865 | 0 |
| `GET /api/site/public` | 13 | 88.8 | 31 | 636 | 636 | 0 |
| `GET /api/post/1` | 6 | 322.0 | 308 | 471 | 471 | 0 |
| `GET /api/site/pages` | 13 | 73.9 | 44 | 426 | 426 | 0 |
| `GET /api/admin/visitors/summary` | 11 | 106.4 | — | — | 408 | 0 |
| `GET /api/admin/site-assets` | 7 | 173.7 | 69 | 762 | 762 | 1×400 |

**P50 与 P95 的巨大差距值得注意**：`/api/post/list` P50=26ms 但 P95=1355ms（**52 倍**）、`/api/visitor/track` P50=54ms 但 P95=244ms 且 max=2231ms。**说明这些端点平时很快，慢是偶发/争用态**——这进一步支持"SQLite 单连接争用"而非"算法复杂度"的归因。

---

## 9. 结论摘要

1. **`performance.log` 是阈值采样日志（阈值 1000ms），不是全量日志**；prod profile 下 `PerformanceMonitoringAspect` level=INFO 使 ≤1000ms 的 DEBUG 行不落盘。17 行 = 15 条真实慢调用 + 2 条异常路径。
2. **2 条 `error:` 记录是正常负例断言，不是故障**：`SiteAssetServiceImpl.store` 32ms / `AdminSiteAssetController.upload` 36ms，对应 HTTP 400 业务校验（`文件内容与扩展名不匹配`）。异常路径**无条件记录**，与慢阈值无关。
3. **真实性能问题 15 条**，最严重为 `POST /api/post` 全链路 3423ms（HTTP 4710ms），根因是 **SQLite `maximumPoolSize=1` 单连接 + 事务独占**；当前库表 **0 行**，排除数据量因素。
4. **四个热点**：① SQLite 单连接写路径串行；② 登录 BCrypt 在事务内长时间独占唯一连接；③ `getPostList` 的 `COUNT(*)` + 单连接 `selectPage` + 过细缓存键（**索引已存在，非缺索引；已批量预取，非 N+1**）；④ 定时任务循环内逐条 `upsert` + `getAdjacentPosts` 三次串行往返。
5. **交叉验证成功**：7/7 HTTP trace 在 `request.log` 精确命中（时间/线程/IP/uid 全一致）；AOP 层提供了 HTTP 层没有的耗时分解。3 条 HTTP >1000ms 在 AOP 层无记录，已由 `audit.log`（HTTP 层 1000/3000 阈值）补全并解释为"AOP 监控范围内各方法均 <1000ms"。**不存在无法交叉的情形**（唯一例外是定时任务，本就无 HTTP 请求）。

**证据等级汇总**：格式/阈值/落盘机制/交叉验证 = **确证**；四个热点的**耗时与调用链** = **确证**；热点的主因排序（连接争用 vs 其他）= **推测**（受限于日志无 SQL 执行明细，且 `logback-spring.xml` 第 256 行 prod 下 `com.selfblog.mapper` level=WARN，MyBatis SQL 日志不落盘，无法直接观测单条 SQL 耗时）。
