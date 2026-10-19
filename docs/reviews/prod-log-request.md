# 生产请求日志取证报告（request.log）

- **取证对象**：`C:\Users\Zora\Downloads\request.log`（1300 行，UTF-8）
- **日志来源**：`backend/src/main/java/com/selfblog/config/web/HttpRequestLoggingInterceptor.java` 输出（`[REQUEST]` / `[RESPONSE]` 两行一组）
- **取证时间**：2026-10-18
- **取证方式**：只读（read / grep / glob + 只读 PowerShell 统计），**未修改 backend/frontend 任何代码或配置**；本报告是本次唯一写入的文件
- **标注约定**：每条结论标注 **「确证」**（日志原文可直接证实）或 **「推测」**（需运行时/源码进一步验证）

---

## 1. 总量基线数字

| 指标 | 数值 | 依据 |
| --- | --- | --- |
| 日志总行数 | 1300 | 全文件行数 **确证** |
| `[REQUEST]` 行 | 650 | **确证** |
| `[RESPONSE]` 行 | 650 | **确证**（与 REQUEST 严格 1:1 配对，无孤儿行） |
| 独立客户端 IP | 28 | **确证** |
| 时间跨度 | 2026-10-17 08:14:26.429 → 16:15:22.486（约 8h01m） | **确证** |
| 已登录请求（MDC 第三槽 `[1]` = userId=1） | 197 | **确证**（`LogUtils.setUserContext`，唯一管理员） |
| 匿名请求 | 453 | **确证** |
| SLOW 告警（>3000ms） | 1 | **确证** |

**小时分布（按 REQUEST 时间戳）— 确证**

| 小时 | 08 | 09 | 10 | 11 | 12 | 13 | 14 | 15 | 16 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 请求数 | 113 | 2 | 0 | 2 | 326 | 1 | 44 | 100 | 62 |

**峰值分钟（Top 8）— 确证**：12:05=85、12:11=63、12:10=61、12:08=53、08:28=46、08:31=45、14:52=42、16:13=33。

**空闲间隔 > 20min — 确证**：08:31:43→08:52:04(20.4m)、09:13:18→11:47:12(153.9m)、12:50:46→13:44:10(53.4m)、13:44:10→14:17:35(33.4m)、14:17:35→14:42:06(24.5m)、14:52:45→15:20:26(27.7m)。

**IP 请求量 — 确证**

| IP | 请求数 | 性质 |
| --- | --- | --- |
| 82.153.135.59 | 319 | 管理员（12 点后台连续操作） |
| 61.224.74.83 | 157 | 管理员（15–16 点，发文 + 浏览） |
| 153.119.45.93 | 108 | 管理员（08:28–08:31 首访 + 登录） |
| 34.156.121.46 | 42 | **扫描器**（40 条路径字典探测） |
| 其余 24 个 IP | 各 1 条 | 爬虫 / 探测 / 单次访问 |

**延迟分位 — 确证**：MIN=1ms、P50=47ms、P90=209ms、P99=1272ms、MAX=**4710ms**；>1000ms 共 9 条，>500ms 共 26 条，>2000ms 共 3 条。

> **SLOW 阈值说明（确证，源码）**：`HttpRequestLoggingInterceptor.java:28` `SLOW_REQUEST_THRESHOLD = 3000`，仅 `duration > 3000` 才打 `SLOW`。因此 2000–3000ms 的条目**不会**带 SLOW 标记，日志中 `SLOW` 仅 1 次不代表只有 1 次慢请求。

---

## 2. 状态码直方图

| 状态码 | 条数 | 占比 |
| --- | --- | --- |
| 200 | 600 | 92.31% |
| 304 | 2 | 0.31% |
| 400 | 1 | 0.15% |
| 404 | 46 | 7.08% |
| 500 | 1 | 0.15% |
| **合计** | **650** | 100% |

**确证**：全程无 3xx（除 304）、无 401、无 403、无 429（限流未触发）、无 5xx 除唯一 1 条 500。非 2xx 合计 50 条。

**304 明细（2 条，均 `GET /`，ETag 命中）— 确证**

```
2026-10-17 08:28:48.762 [http-nio-8888-exec-6] [77658357827a4a34] [] [153.119.45.93] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET / status=304 duration=78ms clientIp=153.119.45.93
2026-10-17 12:05:07.705 [http-nio-8888-exec-2] [5420149205904b25] [] [82.153.135.59] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET / status=304 duration=54ms clientIp=82.153.135.59
```

对应 `SpaController.java:63-71` 的 ETag/If-None-Match 分支（`确证` 为设计内行为，非异常）。

---

## 3. 非 2xx 明细

### 3.1 按 IP 分布 — 确证

| IP | 状态 | 条数 |
| --- | --- | --- |
| 34.156.121.46 | 404 | 40 |
| 82.153.135.59 | 404 | 2 |
| 82.153.135.59 | 400 | 1 |
| 16.216.88.134 / 16.216.88.196 | 404 | 各 1 |
| 114.119.151.83 | 404 | 1 |
| 185.180.141.2 | 404 | 1 |
| 185.180.141.5 | **500** | 1 |
| 153.119.45.93 / 82.153.135.59 | 304 | 各 1 |

### 3.2 46 条 404 按路径 — 确证

- **扫描器字典探测 40 条**：见 §4 完整清单。
- **`/archive/` × 2**：见 §6.1。
- **`/gallery/firefly-2026/` × 1**（PetalBot 爬虫）。
- **`/sse` × 1**（python-httpx 脚本）。
- **`/admin` × 1、`/zroa` × 1**：见 §6.4，属管理员会话内的探测/拼写，非攻击。

### 3.3 唯一 400 — 确证（原文）

```
2026-10-17 12:06:54.920 [http-nio-8888-exec-2] [f5087641e44a40bcbbceb7ce03460124] [1] [82.153.135.59] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] POST /api/admin/site-assets  from IP=82.153.135.59 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36
2026-10-17 12:06:55.019 [http-nio-8888-exec-2] [f5087641e44a40bcbbceb7ce03460124] [1] [82.153.135.59] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] POST /api/admin/site-assets status=400 duration=99ms clientIp=82.153.135.59
```

详见 §6.3。

### 3.4 唯一 500 — 确证（原文）

```
2026-10-17 08:56:58.256 [http-nio-8888-exec-9] [b0bbfa2b45f34738] [] [185.180.141.5] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] GET /favicon.ico  from IP=185.180.141.5 UA=Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Safari/537.36
2026-10-17 08:56:58.311 [http-nio-8888-exec-9] [b0bbfa2b45f34738] [] [185.180.141.5] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET /favicon.ico status=500 duration=54ms clientIp=185.180.141.5
```

详见 §6.2。

---

## 4. 扫描路径清单 + IP + UA + 时间分布

### 4.1 扫描器指纹 — 确证

- **IP**：`34.156.121.46`（Google Cloud 网段特征）
- **请求数**：42（2 条正常预热 + **40 条路径字典探测**）
- **时间窗**：`14:52:41.630` → `14:52:45.363`，**约 3.7 秒内完成 40 次探测**（典型自动化扫描）
- **UA 伪造证据（确证）**：前 2 条为完整浏览器 UA，其后 40 条统一为**截断 UA**（无 Chrome 版本号、无 Safari 段）：

```
2026-10-17 14:52:41.630 ... [REQUEST] GET /  from IP=34.156.121.46 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36
2026-10-17 14:52:42.035 ... [REQUEST] GET /assets/index-BNvWXfcT.js  from IP=34.156.121.46 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36
2026-10-17 14:52:43.689 ... [REQUEST] GET /Dockerfile  from IP=34.156.121.46 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
```

**判定**：**确证为恶意/自动化扫描流量**。它先 `GET /` 与一个静态资源伪装成正常浏览器，随后切换到截断 UA 批量探测敏感路径；全部 404。

### 4.2 40 条探测路径原文（按时间顺序）— 确证

| # | 时间 | 路径 |
| --- | --- | --- |
| 1 | 14:52:43.689 | `/Dockerfile` |
| 2 | 14:52:43.694 | `/_profiler/phpinfo` |
| 3 | 14:52:43.701 | `/admin/.env` |
| 4 | 14:52:43.830 | `/app/.env` |
| 5 | 14:52:43.835 | `/application/.env` |
| 6 | 14:52:43.896 | `/back/.env` |
| 7 | 14:52:43.898 | `/backend/.env` |
| 8 | 14:52:43.969 | `/backup/.env` |
| 9 | 14:52:44.009 | `/cms/.env` |
| 10 | 14:52:44.107 | `/config/.env` |
| 11 | 14:52:44.193 | `/config/aws.yml` |
| 12 | 14:52:44.216 | `/config/config.js` |
| 13 | 14:52:44.298 | `/credentials` |
| 14 | 14:52:44.299 | `/core/.env` |
| 15 | 14:52:44.374 | `/demo/.env` |
| 16 | 14:52:44.483 | `/dev/.env` |
| 17 | 14:52:44.513 | `/env` |
| 18 | 14:52:44.690 | `/laravel/.env` |
| 19 | 14:52:44.692 | `/frontend/.env` |
| 20 | 14:52:44.699 | `/media/.env` |
| 21 | 14:52:44.699 | `/new/.env` |
| 22 | 14:52:44.704 | `/old/.env` |
| 23 | 14:52:44.710 | `/phpinfo` |
| 24 | 14:52:44.764 | `/production/.env` |
| 25 | 14:52:44.770 | `/public/.env` |
| 26 | 14:52:44.868 | `/server/.env` |
| 27 | 14:52:44.900 | `/src/.env` |
| 28 | 14:52:44.901 | `/staging/.env` |
| 29 | 14:52:44.985 | `/test/.env` |
| 30 | 14:52:44.997 | `/v1/.env` |
| 31 | 14:52:45.062 | `/v2/.env` |
| 32 | 14:52:45.089 | `/vendor/composer/installed.json` |
| 33 | 14:52:45.092 | `/web/.env` |
| 34 | 14:52:45.103 | `/wp-content/debug.log` |
| 35 | 14:52:45.104 | `/fuel/app/config/db.php` |
| 36 | 14:52:45.108 | `/fuel/app/config/production/db.php` |
| 37 | 14:52:45.109 | `/app/config/local.php` |
| 38 | 14:52:45.184 | `/mautic/app/config/local.php` |
| 39 | 14:52:45.186 | `/config/config.ini.php` |
| 40 | 14:52:45.200 | `/matomo/config/config.ini.php` |

**分类（确证）**：`.env` 家族 24 条；PHP/框架配置 8 条（`_profiler/phpinfo`、`phpinfo`、`config.ini.php`、`fuel/app/config/*`、`mautic/app/config/local.php`、`app/config/local.php`）；CI/凭据 5 条（`Dockerfile`、`credentials`、`config/aws.yml`、`config/config.js`、`vendor/composer/installed.json`）；日志/环境 3 条（`env`、`wp-content/debug.log`、`web/.env`）。

**目标特征**：典型 PHP/Laravel/WordPress/Matomo 站点凭据收集字典（**推测**：扫描器按通用字典盲打，不针对本项目的 Java/Spring 栈；本服务为 Spring Boot，无 `.env` 与 `wp-content`，故全部 404）。

### 4.3 其他自动化/爬虫 UA — 确证

| IP | 时间 | 路径 | 状态 | UA 关键片段 | 判定 |
| --- | --- | --- | --- | --- | --- |
| 198.235.24.233 | 08:14:26.429 | `/` | 200 (2557ms) | `Hello from Palo Alto Networks ... Cortex-Xpanse/Scanning-activity` | 安全厂商扫描 |
| 198.235.24.219 | 14:17:35.514 | `/` | 200 | 同上 | 安全厂商扫描 |
| 185.180.141.2 | 08:56:57.842 | `/sse` | **404** (29ms) | `python-httpx/0.28.1` | 脚本探测（SSE 端点） |
| 185.180.141.4 | 08:56:47.512 | `/` | 200 | `Chrome/123.0.6312.86`（无平台段） | 自动化探测 |
| 185.180.141.5 | 08:56:58.256 | `/favicon.ico` | **500** (54ms) | `Chrome/123.0.6312.86` | 自动化探测（见 §6.2） |
| 94.183.174.95 | 15:20:26.211 | `/` | 200 | `python-requests/2.31.0` | 脚本 |
| 16.216.88.134 | 08:52:04.963 | `/archive/ ?tag=JWT` | **404** | `Reflectionbot/1.0; +https://reflection.ai/bot` | 爬虫 |
| 16.216.88.196 | 14:42:06.702 | `/archive/ ?tag=教程` | **404** | `Reflectionbot/1.0` | 爬虫 |
| 114.119.151.83 | 12:07:50.065 | `/gallery/firefly-2026/` | **404** | `PetalBot` | 搜索引擎爬虫 |
| 114.119.150.254 | 15:58:00.140 | `/about/` | 200 (192ms) | `PetalBot` | 搜索引擎爬虫 |
| 74.7.175.188 | 15:40:10.744 | `/robots.txt` | 200 (173ms) | `OAI-SearchBot/1.4` | OpenAI 检索爬虫 |
| 3.216.210.239 | 16:02:18.677 | `/robots.txt` | 200 (90ms) | `Safari/600.2.5 (Gort)` | 爬虫 |
| 49.51.72.236 | 12:10:26.398 | `/sitemap.xml` | 200 (139ms) | iPhone Safari | 采集器 |
| 43.130.15.147 | 12:50:46.390 | `/sitemap-site.xml` | 200 (18ms) | iPhone Safari | 采集器 |
| 43.130.57.76 / 43.133.187.11 / 43.159.145.153 / 43.153.107.22 / 49.51.141.76 / 43.166.226.186 / 43.135.145.117 | 各 1 条 | `/gallery`、`/archives`、`/about`、`/` 等 | 200 | iPhone Safari 13.0.3 | 同一采集网段（腾讯云 43.x/49.x） |

**含 bot / crawler / spider / 扫描关键词的 UA 汇总（确证）**：Palo Alto Networks 扫描、Reflectionbot、PetalBot、OAI-SearchBot、`python-httpx`、`python-requests`、`Gort`、`Safari/600.2.5 (Gort)`。

### 4.4 真实攻击流量 vs 爬虫/监控爬虫 — 判定

| 类别 | IP | 证据 | 判定 |
| --- | --- | --- | --- |
| **真实恶意扫描** | 34.156.121.46 | 3.7 秒 40 条敏感路径、伪造截断 UA、全部 404 | **确证攻击流量**（漏洞扫描/凭据收集） |
| 安全厂商监控扫描 | 198.235.24.233 / .219 | UA 自报 Palo Alto Cortex-Xpanse | 合法扫描器（**确证**） |
| 搜索引擎爬虫 | 114.119.151.83 / .150.254 | UA 自报 PetalBot | 合法爬虫（**确证**） |
| AI 检索爬虫 | 74.7.175.188 | UA 自报 OAI-SearchBot | 合法爬虫（**确证**） |
| 脚本探测 | 185.180.141.x / 94.183.174.95 | `python-httpx` / `python-requests` | 自动化探测（**确证**），非攻击成功 |
| 管理员真人 | 82.153.135.59 / 61.224.74.83 / 153.119.45.93 | 登录成功后带 `[1]` 操作 `/api/admin/**`、发文、相册 | **合法会话**（**确证**，非攻击） |

**关键结论（确证）**：本日志**没有任何攻击成功**的痕迹——所有扫描路径返回 404，无 401/403/429 异常峰值，无 SQL 注入/路径穿越特征路径（如 `../`、`%2e%2e`），无异常大响应。唯一的 500 来自缺失的 `/favicon.ico`，与扫描无因果。

---

## 5. 最慢 30 条响应

**确证**（按 duration 降序；`SLOW` 列表示是否带 `SLOW` 标记）：

| # | 时间 | 方法 路径 | status | duration | clientIp | 已登录 | SLOW |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 15:54:52.833 | POST `/api/post` | 200 | **4710ms** | 61.224.74.83 | 是 | **是** |
| 2 | 08:29:42.895 | POST `/api/user/login` | 200 | 2755ms | 153.119.45.93 | 否 | 否 |
| 3 | 08:14:28.986 | GET `/` | 200 | 2557ms | 198.235.24.233 | 否 | 否 |
| 4 | 08:28:47.897 | POST `/api/visitor/track` | 200 | 2231ms | 153.119.45.93 | 否 | 否 |
| 5 | 16:15:03.267 | GET `/api/post/list` | 200 | 1690ms | 61.224.74.83 | 是 | 否 |
| 6 | 08:28:47.918 | GET `/api/post/list` | 200 | 1355ms | 153.119.45.93 | 否 | 否 |
| 7 | 12:05:47.708 | POST `/api/user/login` | 200 | 1272ms | 82.153.135.59 | 否 | 否 |
| 8 | 16:14:39.525 | GET `/api/post/1/adjacent` | 200 | 1134ms | 61.224.74.83 | 是 | 否 |
| 9 | 15:54:59.872 | GET `/api/anonymous-likes/posts/1` | 200 | 1074ms | 61.224.74.83 | 是 | 否 |
| 10 | 11:47:13.808 | GET `/` | 200 | 905ms | 43.135.145.117 | 否 | 否 |
| 11 | 08:28:47.463 | GET `/api/tag/list` | 200 | 900ms | 153.119.45.93 | 否 | 否 |
| 12 | 15:55:59.566 | GET `/api/guest-comments/post/1` | 200 | 865ms | 61.224.74.83 | 是 | 否 |
| 13 | 16:14:39.192 | GET `/api/guest-comments/post/1` | 200 | 813ms | 61.224.74.83 | 是 | 否 |
| 14 | 15:54:59.610 | GET `/api/guest-comments/post/1` | 200 | 802ms | 61.224.74.83 | 是 | 否 |
| 15 | 16:15:22.303 | GET `/api/guest-comments/post/1` | 200 | 794ms | 61.224.74.83 | 是 | 否 |
| 16 | 14:52:44.488 | GET `/admin/.env` | 404 | 787ms | 34.156.121.46 | 否 | 否 |
| 17 | 12:06:20.795 | POST `/api/admin/site-assets` | 200 | 762ms | 82.153.135.59 | 是 | 否 |
| 18 | 16:09:32.740 | GET `/api/post/list` | 200 | 761ms | 61.224.74.83 | 是 | 否 |
| 19 | 16:09:35.300 | GET `/api/guest-comments/post/1` | 200 | 715ms | 61.224.74.83 | 是 | 否 |
| 20 | 15:54:54.274 | GET `/api/post/list` | 200 | 674ms | 61.224.74.83 | 是 | 否 |
| 21 | 14:52:44.470 | GET `/app/.env` | 404 | 639ms | 34.156.121.46 | 否 | 否 |
| 22 | 14:52:44.471 | GET `/application/.env` | 404 | 638ms | 34.156.121.46 | 否 | 否 |
| 23 | 08:28:44.608 | GET `/api/site/public` | 200 | 636ms | 153.119.45.93 | 否 | 否 |
| 24 | 15:55:59.338 | GET `/api/post/1/adjacent` | 200 | 632ms | 61.224.74.83 | 是 | 否 |
| 25 | 14:52:44.471 | GET `/back/.env` | 404 | 575ms | 34.156.121.46 | 否 | 否 |
| 26 | 14:52:44.471 | GET `/backend/.env` | 404 | 573ms | 34.156.121.46 | 否 | 否 |
| 27 | 14:52:44.478 | GET `/backup/.env` | 404 | 509ms | 34.156.121.46 | 否 | 否 |
| 28 | 15:56:17.679 | GET `/api/guest-comments/post/1` | 200 | 486ms | 61.224.74.83 | 是 | 否 |
| 29 | 15:54:59.269 | GET `/api/post/1/adjacent` | 200 | 474ms | 61.224.74.83 | 是 | 否 |
| 30 | 14:52:44.482 | GET `/cms/.env` | 404 | 473ms | 34.156.121.46 | 否 | 否 |

**唯一 SLOW 行原文 — 确证**

```
2026-10-17 15:54:48.120 [http-nio-8888-exec-6] [63a8a58fb1d34159b9413f92a7340a11] [1] [61.224.74.83] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] POST /api/post  from IP=61.224.74.83 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36
2026-10-17 15:54:52.833 [http-nio-8888-exec-6] [63a8a58fb1d34159b9413f92a7340a11] [1] [61.224.74.83] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] SLOW POST /api/post status=200 duration=4710ms clientIp=61.224.74.83
```

**观察（确证）**：
1. **慢请求高度集中在管理员会话**：Top 30 的 IP 构成为 `61.224.74.83` **14 条**、`34.156.121.46` 7 条、`153.119.45.93` 5 条、`82.153.135.59` 2 条、其余 2 条；其中 `61.224.74.83` 的 14 条**全部** `[1]`（已登录）且**全部 ≥15:54:52**——即该 IP 的全部慢请求都出现在 15:54:48 发文（4710ms）**当时及之后**。
2. **`/api/guest-comments/post/1` 是唯一「次次都慢」的路径**：6 次全部 ≥486ms（802/865/715/486/813/794），无一次快；对照同域管理端接口 5 次仅 5–135ms（见 §7.2）。
3. **扫描器的 404 也慢**：14:52:44 一批 `.env` 404 耗时 473–787ms，与同秒内的正常 404（`/env` 69ms、`/config/aws.yml` 75ms）形成反差——**推测**为 `SpaController` 兜底渲染 404 HTML 页（`ReadingPageService.notFound` + `ReadingHtmlRenderer`）的渲染成本，且并发探测挤占 Tomcat 线程。
4. 分位失真提示：P99=1272ms 会低估尾延迟，因为 4710ms 那条 SLOW 行的 `duration` 解析需单独处理。

---

## 6. 关键错误路径上下文

### 6.1 `GET /archive/` 404 × 2

**结论（确证）：这是外部爬虫构造的路径，不是站内路由缺失。**

```
2026-10-17 08:52:04.963 [http-nio-8888-exec-4] [05001646f1f24c9e] [] [16.216.88.134] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] GET /archive/ ?tag=JWT from IP=16.216.88.134 UA=Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; Reflectionbot/1.0; +https://reflection.ai/bot) Chrome/151.0.0.0 Safari/537.36
2026-10-17 08:52:05.018 [http-nio-8888-exec-4] [05001646f1f24c9e] [] [16.216.88.134] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET /archive/ status=404 duration=55ms clientIp=16.216.88.134
2026-10-17 14:42:06.702 [http-nio-8888-exec-18] [5de9684a84dc48ea] [] [16.216.88.196] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] GET /archive/ ?tag=%E6%95%99%E7%A8%8B from IP=16.216.88.196 UA=Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; Reflectionbot/1.0; +https://reflection.ai/bot) Chrome/151.0.0.0 Safari/537.36
2026-10-17 14:42:06.868 [http-nio-8888-exec-18] [5de9684a84dc48ea] [] [16.216.88.196] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET /archive/ status=404 duration=164ms clientIp=16.216.88.196
```

**源码佐证（确证）**：
- `ReadingPageService.java:74-87` 的路由 switch 只有 `"/"`、`"/archives"`、`"/about"`、`"/gallery"`、`"/search"`，其余走 `default → notFound(site)`；`normalize(path)` 后的 `"/archive/"` 不匹配任何分支 → 直接 `notFound`（`ReadingPageService.java:231-237`，返回 status 404）。
- 站内真实路径是 **`/archives`（复数、无尾斜杠）**：

```
2026-10-17 12:30:13.067 ... [REQUEST] GET /archives  from IP=43.133.187.11 UA=Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) ... Safari/604.1
```

- `?tag=JWT` / `?tag=教程` 是爬虫自造的查询参数（本站标签页是 `/tag/{id}`，`ReadingPageService.java:86`），**进一步支持「爬虫猜测」而非站内路由**。

**影响**：不影响真实用户；但爬虫会持续把 404 记入索引失败。**可选改进（非本次改动）**：为 `/archive/` 与 `/archives/` 增加 301 归一，或让爬虫从 sitemap 获取正确 URL。

### 6.2 `GET /favicon.ico` 500 × 1

**结论（确证 + 推测）：`/favicon.ico` 在安全配置中被放行，但应用内既无该静态资源映射、也无对应控制器，缺失资源异常落到全局兜底 `Exception` 处理器，返回 500 而不是 404。**

```
2026-10-17 08:56:58.256 [http-nio-8888-exec-9] [b0bbfa2b45f34738] [] [185.180.141.5] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] GET /favicon.ico  from IP=185.180.141.5 UA=Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.86 Safari/537.36
2026-10-17 08:56:58.311 [http-nio-8888-exec-9] [b0bbfa2b45f34738] [] [185.180.141.5] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] GET /favicon.ico status=500 duration=54ms clientIp=185.180.141.5
```

**源码链（确证）**：
- `SecurityConfig.java:191-194` 明确放行 `"/favicon.ico"` → **500 不是鉴权导致**（若是鉴权，应为 401/403，日志中无此类状态码）。
- `WebMvcConfig.java:66-77` 的 `addResourceHandlers` **只注册了 `/uploads/**`**，无 `/favicon.ico` 映射。
- `glob` 核实 `backend/src/main/resources/static/` 下**唯一文件是 `seo-reading.css`** → classpath 无 `favicon.ico`。
- `SpaController.java:52-56` 的路径正则 `[^\.]*$` **排除含点号的路径**，`/favicon.ico` 不进入阅读页控制器 → 该请求只能落到静态资源解析，失败后抛 `NoResourceFoundException`。
- `GlobalExceptionHandler.java` 的 20 个 `@ExceptionHandler` 中**没有 `NoResourceFoundException` 处理器**（grep 确证，仅 `NoHandlerFoundException` 在 :103）→ Spring Boot 3 的 `NoResourceFoundException` 落到 `:259 @ExceptionHandler(Exception.class)` 兜底 → **500**（**推测**：这是本条的成因，需运行时验证）。

**对照证据（确证）**：`/favicon.svg` 三次请求**全部 200**，说明站点真实 favicon 是 SVG，`.ico` 从未提供：

```
2026-10-17 08:28:43.995 ... [REQUEST] GET /favicon.svg  from IP=153.119.45.93 UA=... Chrome/154.0.0.0 Safari/537.36
2026-10-17 09:13:18.007 ... [REQUEST] GET /favicon.svg  from IP=91.231.89.102 UA=... Firefox/134.0
```

**影响（推测）**：浏览器/爬虫自动请求 `/favicon.ico` 会得到 500，污染错误率与监控；且 500 无缓存，每次都会重复。**可选改进（非本次改动）**：为 `NoResourceFoundException` 增加 404 处理器，或提供 `/favicon.ico` 重定向到 `/favicon.svg`。

### 6.3 `POST /api/admin/site-assets` 400 × 1

**结论（确证 + 推测）：管理员上传重试过程中的一次参数/内容校验失败，前后 6 次同路径请求全部 200。**

```
2026-10-17 12:06:54.920 [http-nio-8888-exec-2] [f5087641e44a40bcbbceb7ce03460124] [1] [82.153.135.59] INFO  c.s.config.web.HttpRequestLoggingInterceptor - [REQUEST] POST /api/admin/site-assets  from IP=82.153.135.59 UA=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36
2026-10-17 12:06:55.019 [http-nio-8888-exec-2] [f5087641e44a40bcbbceb7ce03460124] [1] [82.153.135.59] WARN  c.s.config.web.HttpRequestLoggingInterceptor - [RESPONSE] POST /api/admin/site-assets status=400 duration=99ms clientIp=82.153.135.59
```

**前后 6 次同路径全部 200（确证）**：

```
2026-10-17 12:06:20.795 ... [RESPONSE] POST /api/admin/site-assets status=200 duration=762ms
2026-10-17 12:06:46.648 ... [RESPONSE] POST /api/admin/site-assets status=200 duration=57ms
2026-10-17 12:07:00.579 ... [RESPONSE] POST /api/admin/site-assets status=200 duration=59ms
2026-10-17 12:07:35.177 ... [RESPONSE] POST /api/admin/site-assets status=200 duration=69ms
2026-10-17 12:07:43.912 ... [RESPONSE] POST /api/admin/site-assets status=200 duration=160ms
2026-10-17 12:07:51.636 ... [RESPONSE] POST /api/admin/site-assets status=200 duration=10ms
```

**候选 400 分支（确证存在、无法区分，故标推测）**：
- `AdminSiteAssetController.java:33-35` `upload(@RequestParam("file") MultipartFile file, @RequestParam("kind") String kind)` → 缺 `file`/`kind` 抛 `MissingServletRequestPartException` / `MissingServletRequestParameterException`，两者均在 `GlobalExceptionHandler` 映射为 400。
- `SiteAssetServiceImpl.java:124-142` `verifyMagicNumber()` 按扩展名比对文件头，不匹配抛 `BusinessException(400, "文件内容与扩展名不匹配，请上传真实文件")`。
- `SiteAssetKind.java:21` `FAVICON("favicon", Category.IMAGE, Set.of(".png", ".ico", ".svg"), 2MB)` → 超过 2MB 或扩展名不在集合内亦为 400。
- 日志只记录状态码，**不含响应体**，因此无法从本日志区分具体分支（**推测**）。从时间线看（12:06:20 成功 762ms → 12:06:46 成功 57ms → 12:06:54 **失败** → 12:07:00 成功），**最可能是管理员连续试传不同文件时的一次魔数/扩展名不匹配**（**推测**）。

**影响（确证）**：仅 1 次、99ms、已登录管理员自身操作，**非攻击**，无业务影响。

### 6.4 `/admin` 与 `/zroa` 404 的归属 — 确证

两条 404 都来自**管理员本人的会话**（同一 IP 随后登录成功并进入后台），不是扫描：

```
2026-10-17 12:05:24.193 ... [REQUEST] GET /admin  from IP=82.153.135.59 UA=... Chrome/155.0.0.0 Safari/537.36
2026-10-17 12:05:24.225 ... WARN ... [RESPONSE] GET /admin status=404 duration=31ms clientIp=82.153.135.59
2026-10-17 12:05:30.586 ... [REQUEST] GET /zroa  from IP=82.153.135.59 UA=... Chrome/155.0.0.0 Safari/537.36
2026-10-17 12:05:30.629 ... WARN ... [RESPONSE] GET /zroa status=404 duration=42ms clientIp=82.153.135.59
2026-10-17 12:05:38.252 ... [REQUEST] GET /zora  from IP=82.153.135.59 UA=... Chrome/155.0.0.0 Safari/537.36
```

**关键对照（确证）**：同一 IP 在 08:29 时段访问 `/admin` 得到 **200**：

```
2026-10-17 08:29:08.890 ... [RESPONSE] GET /admin status=200 duration=21ms clientIp=153.119.45.93
```

→ **说明 `adminPath` 在 08:29 与 12:05 之间被改成了 `zora`**（**推测**，与 12:05:38 `GET /zora` → 200、以及 `ReadingPageService.java:72-73` 的 `adminRoot = "/" + settings.getOrDefault("adminPath","admin")` 逻辑一致）。`/zroa` 是管理员把 `zora` 敲错（**推测**）。

---

## 7. 反复慢路径的根因候选

### 7.1 `/api/post/list`（31 条，avg 213ms，累计 6600ms — 累计最高）— 确证数据 + 推测根因

**查询参数与耗时（确证，逐条关联）**

`?pageNum=1&pageSize=9&sort=latest`（20 条）：
`1355, 18, 9, 77, 33, 26, 15, 7, 161, 10, 102, 674, 10, 14, 22, 299, 13, 11, 1690, 98` ms

`?pageNum=1&pageSize=100&sort=latest`（11 条）：
`52, 230, 17, 350, 761, 224, 12, 13, 14, 11, 272` ms

**关键反例（确证）**：同一参数在同一分钟内既有极快也有极慢 —— `pageSize=9` 在 16:13:45.504 为 **13ms**、在 16:15:03.267 为 **1690ms**（间隔约 78 秒）。`pageSize=100` 在 12:10:13.721 为 **17ms**、在 16:09:32.740 为 **761ms**。
→ **pageSize 不是决定因素**（**确证**）。

**根因候选（按可能性排序，均标推测）**
1. **冷路径/预热效应**：慢的样本集中在「一段时间空闲后的首个请求」——如 08:28:47.918（首次访问）1355ms、16:15:03.267（16:13 突发后静默约 74s）1690ms。与 SQLite 连接池（`DatabaseConfig.java:75` `config.setMaximumPoolSize(1)`）、Hikari 连接重建、JIT/Caffeine 缓存冷启动一致（**推测**）。
2. **SQLite 单连接串行化**：`DatabaseConfig.java:74-75` 注释明写「SQLite 单文件数据库不适合高并发连接池」并设 `maximumPoolSize(1)`；在管理员同一秒发起多条 API（如 16:13:38–16:13:49 约 20 条）时，DB 请求排队 → 个别请求被放大到数百 ms（**推测**）。
3. **`GET /`（首页）与 `/api/post/list` 同源慢**：`ReadingPageService.java:95-102` 首页调用 `posts.getPostList(request)`（`HOME_PAGE_SIZE = 9`），`GET /` 的 14 条 avg 334ms、max 2557ms 与该接口的慢样本时间高度重合（08:14:28.986、11:47:13.808 等）。**推测**：SpaController 服务端渲染 HTML（`ReadingHtmlRenderer` + `SafeMarkdown`）叠加 DB 查询，成本高于纯 JSON 接口。

### 7.2 `/api/guest-comments/post/1`（6 条，avg **746ms**，全部来自同一 IP/会话）— 确证为稳定慢

耗时序列（确证）：`802, 865, 715, 486, 813, 794` ms —— **无一次 < 400ms**，且全部带 `[1]`、全部 `61.224.74.83`。
→ **确证为「反复慢路径」而非偶发**。

**关键判别证据（确证）**：同一留言域的**管理端列表**接口 `GET /api/admin/guest-comments?pageNum=1&pageSize=20` 共 5 次，全部**极快**：

```
2026-10-17 12:09:07.083 | status=200 duration=135ms clientIp=82.153.135.59
2026-10-17 12:11:47.827 | status=200 duration=5ms   clientIp=82.153.135.59
2026-10-17 12:11:50.970 | status=200 duration=7ms   clientIp=82.153.135.59
2026-10-17 12:12:12.799 | status=200 duration=17ms  clientIp=82.153.135.59
2026-10-17 12:12:22.497 | status=200 duration=6ms   clientIp=82.153.135.59
```

→ **同一数据域、请求量更大的管理端（pageSize=20）只要 5–17ms，而公开端 `post/1`（pageSize=10）却要 486–865ms**。这把「DB 连接池排队」单独解释排除掉一部分（**推测**），**更指向公开端接口自身的查询逻辑/缺少针对 `post_id` 的过滤或索引**。

**根因候选（推测）**：
1. 公开端按 postId 过滤时**未命中索引**（缺 `(post_id, is_deleted)` 复合索引），导致全表扫描后再内存过滤；
2. **N+1 查询**（逐条留言查用户/回复关系）；
3. 与 `anonymous-likes`、`adjacent` 共享同一 post 详情聚合逻辑，故多条路径同时慢（见 7.3）。
**需进一步验证**：查 `GuestComment` 相关 Mapper XML 的 SQL 与索引，以及管理端是否走了不同的查询方法。

### 7.3 `/api/post/1/adjacent` 与 `/api/anonymous-likes/posts/1` — 确证为伴生慢

- `/api/post/1/adjacent`：6 条 `474, 632, 213, 292, 1134, 429` ms（avg 529ms）
- `/api/anonymous-likes/posts/1`：6 条 `1074, 351, 67, 388, 403, 273` ms（avg 426ms）
- `/api/post/1`：6 条 avg 322ms，max 471ms

**确证**：这三条与 `guest-comments` 在 15:54:59 与 16:14:38–39 几乎**同秒并发**触发（文章详情页加载），彼此耗时正相关 → **推测**为同一详情页聚合请求集共享 DB 连接（池大小 1）造成的排队效应，而非各自独立慢。

### 7.4 `POST /api/visitor/track`（58 条 — 最频繁路径）— 确证仅首条异常

58 条耗时中仅首条 `2231ms` 异常，其余 `8–247ms`（多数 <100ms）。→ **确证**为冷启动首条，不是持续慢。avg 107ms、累计 6206ms 主要由高频次数贡献。

### 7.5 `POST /api/user/login`（2 条，avg **2014ms**）— 确证

`08:29:42.895 → 2755ms`（该 IP 首次访问）、`12:05:47.708 → 1272ms`。
→ **推测**：BCrypt 密码校验成本（CPU 密集，与并发无关）+ 首次请求冷启动。两次均 200 成功，无暴力破解迹象（全程仅 2 次 login，无 401）。

---

## 8. 结论汇总（按严重度排序）

| 级别 | 结论 | 证据 | 状态 |
| --- | --- | --- | --- |
| **高** | 存在真实漏洞扫描：IP `34.156.121.46` 在 3.7 秒内探测 40 条敏感路径（`.env`/凭据/PHP 配置），伪造截断 UA；全部 404，未成功 | §4.1–4.2 | 确证 |
| **中** | `/favicon.ico` 返回 **500** 而非 404：放行但无资源、无控制器，缺失异常落兜底 `Exception` 处理器 | §6.2 | 确证现象 / 推测成因 |
| **中** | `/api/guest-comments/post/1` 为稳定慢路径（6/6 ≥486ms，avg 746ms），疑似 N+1 或全量查询 | §7.2 | 确证现象 / 推测成因 |
| **中** | `/api/post/list` 与 `GET /` 尾延迟可达 1690–2557ms，同参数快慢两极，疑似冷启动 + SQLite 单连接（`maximumPoolSize=1`）串行化 | §7.1 | 确证现象 / 推测成因 |
| **低** | `POST /api/post` 单次 4710ms（唯一 SLOW），发生在管理员发文时 | §5 | 确证 |
| **低** | 爬虫以错误路径 `/archive/` 与 `/gallery/firefly-2026/` 持续产生 404（Reflectionbot、PetalBot） | §6.1、§4.3 | 确证 |
| **低** | `POST /api/admin/site-assets` 单次 400，为管理员上传重试中的一次校验失败 | §6.3 | 确证现象 / 推测分支 |
| **信息** | 12 点 326 条峰值是**单个管理员后台操作**（非攻击/非爬虫）；16:13 的 33 条/分钟同理 | §1、§4.4 | 确证 |
| **信息** | 限流未触发（无 429）、鉴权未失败（无 401/403），安全边界在日志范围内有效 | §2 | 确证 |

---

## 9. 本次取证的边界与未验证项

1. **无响应体**：`HttpRequestLoggingInterceptor` 只记录状态码与耗时，无法区分 400 的具体校验分支、也无法看到 404/500 的响应内容（已在 §6 标注为推测）。
2. **无法确认 500 的实际异常类型**：`/favicon.ico` 的成因需查应用日志（`GlobalExceptionHandler` 的 `log.error`）或运行时复现。
3. **慢路径根因未做运行时验证**：需结合 SQL 慢查询日志、`EXPLAIN`、连接池指标（Hikari `activeConnections`）确认，本报告只给出候选。
4. **`adminPath` 变更**（`admin` → `zora`）由 `/admin` 的 200→404 与 `/zora` 的 200 反推，未核对 `site_setting` 表实际值。
5. **本报告未修改任何 backend/frontend 代码或配置**，§6/§7 的「可选改进」仅为建议，需另行立项。
