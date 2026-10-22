# SelfBlog — 个人博客

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/version-v2.0.28-blue)](https://github.com/Xinghe-0203)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-green)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5.44-brightgreen)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue)](https://www.mysql.com/)

基于 Spring Boot 3 + Vue 3 的单人作者个人博客。生产部署只需一个内置 Vue 前端的可执行 JAR 和配置好的 `.env`；默认使用 SQLite 文件数据库与进程内 Caffeine 缓存。访客可阅读、搜索文章、匿名点赞、留言和浏览相册；作者从可配置入口进入后台管理内容与站点设置。升级时保留已有文章数据。

---

## 目录

- [技术栈](#技术栈)
- [项目功能](#项目功能)
- [项目结构](#项目结构)
- [部署指南](#部署指南)
- [首次启动账号](#首次启动账号)
- [系统架构](#系统架构)
- [性能优化](#性能优化)
- [安全特性](#安全特性)
- [API 文档](#api-文档)
- [文档索引](#文档索引)
- [开源协议](#开源协议)
- [作者](#作者)

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| **Java** | 21 LTS | 编程语言 |
| **Spring Boot** | 3.3.13 | Web 框架 |
| **Spring Security** | 6.x | 认证与授权 |
| **MyBatis Plus** | 3.5.8 | ORM 框架 |
| **JJWT** | 0.12.6 | JWT 令牌生成与验证 |
| **Caffeine** | 3.2.0 | 进程内缓存 |
| **Knife4j** | 4.5.0 | API 文档（Swagger 增强） |
| **Jsoup** | 1.18.3 | XSS 防护（HTML 净化） |
| **Hutool** | 5.8.40 | Java 工具库 |
| **Lombok** | 1.18.42 | 代码简化 |
| **Maven** | 3.8+ | 构建工具 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| **TypeScript** | 6.x | 编程语言 |
| **Vue** | 3.5.44 | UI 框架（Composition API） |
| **Vite** | 6.4.4 | 构建工具 |
| **Vue Router** | 4.6.4 | 前端路由 |
| **Pinia** | 2.3.1 | 状态管理 |
| **Axios** | 1.20.0 | HTTP 客户端 |
| **Marked** | 12.0.2 | Markdown 渲染 |
| **DOMPurify** | 3.4.17 | XSS 防护 |
| **Vitest** | 4.1.11 | 单元测试 |
| **npm** | 11.x | 包管理工具（构建时自动安装） |

前端版本表按当前 `frontend/package-lock.json` 的锁定版本核对；`package.json` 中的 `^` 范围下界不等于实际安装版本。

### 数据库

| 技术 | 版本 | 说明 |
|------|------|------|
| **SQLite** | 内置 | 默认数据库，无需单独安装服务 |
| **MySQL** | 8.0+ | 可选外部数据库 |

### 编程语言

| 语言 | 用途 |
|------|------|
| **Java 21** | 后端业务逻辑、API 接口、安全认证 |
| **TypeScript** | 前端组件、状态管理、类型安全 |
| **SQL** | 数据库建表、索引、查询 |
| **CSS** | 前端样式（设计系统 v6.0「清爽科技蓝」，CSS 变量 + 深色模式 + 皮肤） |
| **XML** | MyBatis Mapper 映射、Logback 日志配置 |

---

## 项目功能

### 用户系统
- 仅作者管理员可以登录管理后台（JWT 双 Token：Access 24h + Refresh 7d）
- 访客无需账号即可阅读和搜索公开文章
- 关闭访客注册；后台登录拒绝普通用户账号；公开页面不显示登录/注册入口

### 文章系统
- 作者使用 Markdown 编辑器发布、编辑文章并管理草稿
- 标签管理、全文搜索、文章归档与阅读量统计
- 文章页提供目录、代码复制/折叠、标题锚点与图片灯箱等阅读体验
- 访客可匿名点赞、提交文章留言；评论和访客统计可在后台开关
- 支持公开相册与短期令牌保护的密码相册

### 管理后台
- 作者工作台、文章、标签、访客、评论、相册、页面和网站设置管理
- 可调整后台路径、站点资料、主题皮肤、强调色和背景音乐；站点 Logo、浏览器图标、作者头像与背景音乐支持直接上传
- 历史文章与社区数据表保留；作者写入与管理 API 仅管理员可访问

### LLM 辅助 SEO（2026-10-22）
- 后台新增「SEO 管理」页：全站收录体检（覆盖率、缺描述、标题过长、标题重复、缺封面）+ 逐篇编辑 + 批量生成
- 后台「网站设置 → AI 辅助与 SEO」接入**任意 OpenAI 兼容接口**（地址 / 密钥 / 模型 / 超时），带「测试连接」；密钥在界面上永远只显示为掩码哨兵，不落明文回显
- 单篇生成与发布后自动生成两种模式：自动生成挂在事务提交之后、跑在独立线程池，**任何模型故障都不会让文章保存失败**
- 元数据存在**独立表 `post_seo`**（不是 `blog_post` 的列）—— 仓库没有列级迁移机制，加列会静默不生效
- 前台渲染走统一回落：`post_seo` → 文章标题/摘要/封面 → 站点 Logo；`meta keywords` 优先用生成的关键词，缺省回落文章标签
- 生成失败语义清晰：未配置是 400、上游失败是 502、测试连接恒 200 + `ok:false`；对外文案一律不含密钥与上游原文
- 重试有副作用的请求（生成/批量/测试连接）在前端**显式禁用自动重试**，避免一次超时放大成三倍模型账单
- 生成请求会真实消耗模型额度，是否需要、何时重来由作者决定；不在保存/发布路径上静默改写已填好的元数据

### 外观自定义（2026-10-19）
- 后台「网站设置 → 页面背景」可换**页面背景**：纯色 / 渐变 / 背景图三种模式，背景图支持直接上传或填外链，另有遮罩浓度与固定背景开关
- 可调**组件不透明度**（60-100%）与**背景模糊**（0-40px），只作用于卡片、弹窗、设置面板等大片表面；输入框、表格行与徽章保持实心以免发糊
- **默认值不产生任何 CSS 变量**，因此从未调过这些设置的站点观感逐像素不变，背景继续跟随皮肤与深色模式
- 深色模式与 `midnight` 皮肤下自动忽略作者设置的浅色纯色/渐变（浅底浅字不可读）；背景图保留，但遮罩强制加厚到至少 45%
- 首屏不闪烁：算好的最终 CSS 值会缓存到 localStorage，由 `index.html` 的内联脚本在首次绘制前按白名单转发
- 支持 `prefers-reduced-transparency` / `prefers-contrast` / `forced-colors` 无障碍降级，代价是没有毛玻璃而不是读不清字

### 页面内容与样式配置
- 后台「页面管理」可逐页调整标题、介绍、模块、受控样式和 SEO 字段；关于页支持独立 Markdown 正文
- 可新增 `/pages/<slug>` 内容页，保存草稿与发布分离，支持预览、下线、删除及导航名称/顺序管理
- 内置页可在草稿中恢复默认；公开接口不泄露草稿，冲突版本不会静默覆盖
- **保持当前全站字号与页面比例，不实施 110% 放大**；未配置页面继续使用原内容与作者主题
- 操作与限制见[页面管理说明](docs/page-management.md)
- 上批验证：前端389 / 后端491项测试；[页面管理验证记录](docs/reviews/2026-10-16-page-management-verification.md)

### 可抓取正文与发现（SEO 基础）
- 文章、关于、自建内容页的**首次响应即为可读 HTML**（不执行 JavaScript 也能读正文）；Vue 挂载后照常接管交互
- 每页重建标题/描述、canonical、robots、Open Graph、Twitter 与文章结构化数据；后台、搜索与 404 强制 noindex
- `/sitemap.xml` 改为 index + 分片，覆盖全部已发布文章/标签/内容页（每片1000条）；RSS/Atom 仍保留最近100篇
- `robots.txt` 按实际后台路径生成；`SITE_BASE_URL` 统一 canonical/订阅/地图的对外地址
- 支持 Docker（MySQL 或 SQLite 二选一）与 **Vercel 统一反向代理**；不在 Vercel 单独部署第二份前端
- 这些是“可被抓取”的技术条件，**不代表搜索引擎已经收录**；部署细节见[部署指南](backend/DEPLOY.md)
- 本批验证：前端394 / 后端559项测试，部署契约27项；[SEO/部署验证记录](docs/reviews/2026-10-16-seo-deployment-verification.md)
- 站点正式域名可在后台「网站设置」修改（后台设置的优先级高于 `.env` 的 `SITE_BASE_URL`，留空自动回落）

### 界面与导航音乐
- 灰白阅读表面、系统字体与紧凑圆角；玻璃效果仅用于导航和临时控件，深色与作者自定义主题保留
- 52px 桌面 / 50px 手机导航，滚动时尺寸不变；搜索、菜单和音乐面板互斥，支持键盘关闭
- 音乐由导航面板控制播放、进度、音量和循环；默认不自动播放，公开换页不中断，进入后台暂停
- 弹窗、目录和灯箱共用主题浮层、焦点回收与滚动锁；不支持透明效果时回退实底

### 全站版式（2026-10-16）
- 公开页面统一「标题 → 说明 → 内容」层级：首页为真实站点介绍 + 单列文章列表 + 作者侧栏，归档按年月分组，搜索/标签复用同一套文章行，关于页以作者资料主导，404 提供具名搜索
- 阅读页把标题与元信息放在封面之前，正文约 760px 行宽，目录在 ≤1020px 转为底部抽屉；保留代码复制/折叠、锚点、灯箱、点赞与留言
- 相册改用共享弹窗与灯箱：遮罩高于导航，支持 Esc、焦点回收与滚动锁，保留密码相册与短期令牌流程
- 作者工作台统一标题与内容层级，侧栏抽屉接管焦点与滚动锁；登录页、概览、文章、标签、访客、评论、相册与设置共用 8px 控件 / 12–16px 分组

全站布局重做分三批实施：设计底座/导航/音乐、公开阅读路径、作者工作台均已完成，实现记录见 [CHANGELOG.md](CHANGELOG.md)。（当时的已确认设计稿与实施计划原存于 `docs/superpowers/`，该目录已于 2026-10-19 清理。）

**后续修正（2026-10-16）**：导航已按批准原型恢复链接分组、浅蓝胶囊选中态、声条＋「听一会儿」及音乐面板布局；修复后台抽屉、发布误报、恢复草稿重复创建、取消确认焦点丢失及标签长度问题。真实 SQLite 联调初查为 13/15，修正并增加复核场景后为 **17/17**；前端 **345** / 后端 **463** 项测试通过，类型、Lint（0 错误）及构建通过。范围、证据及未覆盖环境见[导航与前后端复查报告](docs/reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md)。

---

## 项目结构

```
SelfBlog/
├── backend/                    # 后端（Spring Boot 3 + Java 21）
│   ├── pom.xml                     # Maven 依赖配置
│   ├── Dockerfile                  # Docker 构建文件
│   ├── docker-compose.yml          # Docker Compose 编排
│   ├── schema-fresh-install.sql   # MySQL 全新空库建表脚本（含删表语句，勿对现有库执行）
│   ├── .env.example                # 环境变量模板
│   └── src/main/
│       ├── java/com/selfblog/
│       │   ├── controller/         # REST 控制器（按领域分组）
│       │   │   ├── auth/           # 认证（登录、注册、密码重置）
│       │   │   ├── user/           # 用户管理
│       │   │   ├── post/           # 文章、评论、点赞、收藏、标签
│       │   │   ├── circle/         # 校友圈
│       │   │   ├── social/         # 关注、私信、通知、举报
│       │   │   ├── content/        # 话题、热门、媒体、分享、统计
│       │   │   ├── admin/          # 作者管理 API
│       │   │   └── site/           # 设置、访客、评论和相册 API
│       │   ├── service/            # 业务逻辑层
│       │   ├── mapper/             # MyBatis Plus Mapper
│       │   ├── entity/             # 数据库实体
│       │   ├── dto/                # 请求数据传输对象
│       │   ├── vo/                 # 响应视图对象
│       │   └── config/             # 配置类（安全、缓存、数据库、Web）
│       └── resources/
│           ├── application.yml     # 主配置文件
│           ├── logback-spring.xml  # 日志配置
│           └── mapper/             # MyBatis XML 映射
│
├── frontend/                # 前端（Vue 3 + TypeScript）
│   ├── package.json                # npm 依赖配置
│   ├── vite.config.ts              # Vite 构建配置
│   ├── tsconfig.json               # TypeScript 配置
│   └── src/
│       ├── api/                    # API 模块（20 个）
│       ├── views/                  # 页面组件（45 个）
│       ├── components/             # 通用组件、导航、文章行与后台控件
│       ├── stores/                 # Pinia：site / theme / user / music
│       ├── router/                 # 路由配置（模块化）
│       ├── composables/            # 组合式函数
│       ├── styles/                 # 全局 token、实底内容与功能玻璃控件
│       ├── types/                  # TypeScript 类型定义
│       └── utils/                  # 工具函数
│
├── README.md                       # 项目说明（本文件）
├── CLAUDE.md                       # Claude Code 开发指南
├── AGENTS.md                       # AI Agent 项目指南
└── CHANGELOG.md                    # 版本更新日志
```

---

## 部署指南

### 前置要求

| 软件 | 最低版本 | 用途 |
|------|----------|------|
| JDK | 21 LTS | 后端编译与运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 22.22+ / 24.15+ / 26+ | 仅构建时由 Maven 插件自动安装；单 JAR 运行无需 Node |
| MySQL | 8.0+ | 可选数据库；默认 SQLite 无需安装 |

### 直接取用预构建 JAR（无需构建环境）

仓库的 `release/` 目录随源码提交了一次真实构建的 fat JAR，目标机器只需要 JDK 21，
不需要 Maven、Node 或 npm：

```bash
# 从仓库取 JAR
cp release/selfblog-2.0.28.jar /path/to/deployment/
cd /path/to/deployment
cp /path/to/repo/backend/.env.example .env   # 然后填写 JWT_SECRET 与 ADMIN_PASSWORD
java -jar selfblog-2.0.28.jar
```

校验与重建方式见 [`release/README.md`](release/README.md)。该 JAR 与提交时的源码一一对应，
源码变更后不会自动更新。

### 推荐：构建并运行单 JAR

构建机需要 JDK 21、Maven 3.8+ 和网络访问。Maven 会自动安装 Node/npm、构建 Vue 前端并将静态文件打入 JAR。

```bash
cd backend
cp .env.example .env
# 编辑 .env，至少填写随机 JWT_SECRET（32 位以上）和 ADMIN_PASSWORD（12 位以上）
mvn package -DskipTests
```

构建产物为 `backend/target/selfblog.jar`。将该 JAR 与已配置的 `.env` 放在同一运行目录后启动：

```bash
cd /path/to/deployment
java -jar selfblog.jar
```

默认访问地址：博客 `http://localhost:8825/`，后台 `http://localhost:8825/admin`，API `http://localhost:8825/api`。SQLite 默认沿用 `campus_blog.db` 工作目录路径；把既有数据库文件与 JAR 放在同一运行目录，或在 `.env` 中指定它的实际位置。运行时按需创建上传和相册目录；部署时请持久化备份这些数据。单 JAR 运行不需要 Node、npm、Nginx 或 MySQL。

### SQLite / MySQL 数据库

默认 `.env` 使用 SQLite，原有 SQLite 文件通过 `CREATE TABLE IF NOT EXISTS` 增量增加新表，不会重建文章表。

如要使用 MySQL，将 `.env` 中的 `DB_TYPE` 改为 `mysql` 并填写完整连接信息。全新空数据库可按 `backend/schema-fresh-install.sql` 初始化；**已有数据库只运行** `backend/migration-selfblog.sql` 增加博客功能所需表。不要对有数据的数据库运行完整 `schema-fresh-install.sql`，该脚本是全新安装脚本，包含删表语句（改名原因之一）。

### 本地前后端开发

```bash
cd backend
cp .env.example .env   # 配置 JWT_SECRET、ADMIN_PASSWORD
mvn spring-boot:run
# 另一个终端
cd ../frontend
npm ci
npm run dev            # http://localhost:3000
```

---

## 首次启动账号

没有固定的默认密码。首次启动时使用 `.env` 中的 `ADMIN_USERNAME`（默认 `admin`）和 `ADMIN_PASSWORD`（必须至少 12 位）创建作者管理员。已有管理员不会被重置；若数据库仍使用历史示例密码 `Admin123`，初始化程序会将它替换为配置的密码。后台入口默认为 `/admin`，登录后可在网站设置中修改。

---

## 系统架构

### 整体架构

```
浏览器 → Spring Boot（内置 Vue SPA + /api）→ MyBatis Plus → SQLite（默认）/ MySQL（可选）
                                          ↓
                                    Caffeine 缓存
```

### 后端分层

```
Controller（控制器）→ Service（服务）→ Mapper（数据访问）→ Entity（实体）
         ↓
    Result<T>（统一响应格式）
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1716000000000
}
```

### 核心数据库表

在原有文章和社区表之外，个人博客功能新增 `site_setting`、`visitor_event`、`anonymous_post_like`、`guest_comment`、`photo_album`、`album_photo` 和 `post_seo`。SQLite 启动时增量初始化；现有 MySQL 数据库使用 `migration-selfblog.sql`。

> `post_seo` 是**独立表**而不是 `blog_post` 的新列，因为仓库没有列级迁移机制：`migration-selfblog.sql` 只有 `CREATE TABLE IF NOT EXISTS`，`ALTER TABLE` 全仓零出现，给既有库加列会静默不生效。

---

## 性能优化

| 优化项 | 说明 |
|--------|------|
| 数据库索引 | 覆盖文章、访客、评论和相册查询 |
| Caffeine 缓存 | 进程内缓存，无需 Redis |
| 分页查询 | MyBatis Plus 分页插件 |
| 异步日志 | Logback AsyncAppender 减少 I/O 阻塞 |
| 前端优化 | 路由懒加载、代码分割、Tree Shaking |

---

## 安全特性

| 特性 | 实现方式 |
|------|----------|
| 密码加密 | BCrypt（强度 12），API 不返回密码字段 |
| JWT 认证 | Access Token（24h）+ Refresh Token（7d），支持令牌轮换；两种令牌均绑定持久密码凭据版本 |
| 改密撤销 | 修改/重置密码提交后，全部旧访问和刷新令牌失效，服务重启后仍有效；升级前无版本令牌需重新登录 |
| 令牌黑名单 | 单次登出/刷新轮换的黑名单在进程内维护；不替代改密后的持久撤销 |
| 登录锁定 | 5 次失败锁定 15 分钟（原子操作） |
| XSS 防护 | 服务端 Jsoup HTML 净化 + 前端 DOMPurify |
| 限流 | 按路径段匹配、最长规则优先（RateLimitInterceptor）；匿名评论创建每 IP 5 次/分钟，站点资源上传 30 次/分钟；命中 429 会写入审计日志 |
| 审计日志 | 后台写操作、登录成功/失败、401/403、限流命中、业务异常均落 `audit.log`；客户端 IP 只在直连对端为受信代理时才采信 `X-Forwarded-For`，避免伪造归因 |
| 日志注入防护 | 进入日志的外部可控值（`X-Trace-Id`、查询串、UA、异常消息）先过白名单/控制字符清洗，敏感查询参数（token/password 等）自动掩码 |
| SQL 注入 | MyBatis Plus 参数化查询 |
| CORS | 可配置白名单，生产环境禁止 `*` |
| 数据校验 | Spring Validation（@Valid、@NotBlank） |
| 博客访问控制 | 公开阅读/搜索、匿名点赞与留言；写入和管理 API 仅限管理员；注册关闭 |

---

## API 文档

启动后端后访问 Knife4j API 文档：

**http://localhost:8825/doc.html**（默认关闭；本地调试设 `SWAGGER_ENABLED=true` 开启）

公开 API 提供已发布文章、标签、搜索、访客统计采集、匿名点赞、访客评论和相册访问。文章写入、站点配置、访客/评论/相册管理等接口要求作者管理员身份；注册关闭。

### 站点订阅与收录

以下地址在**根路径**（不带 `/api` 前缀），供阅读器与搜索引擎抓取：

| 地址 | 说明 |
|------|------|
| `/feed.xml` | RSS 2.0 订阅源，已发布文章按时间倒序 |
| `/atom.xml` | Atom 1.0 订阅源 |
| `/sitemap.xml` | 站点地图：首页、归档、关于、相册、搜索、全部文章与标签 |
| `/robots.txt` | 动态生成，`Sitemap` 指令为绝对地址并 `Disallow: /admin`、`/api/` |

部署在反向代理之后时，用环境变量 `SITE_BASE_URL`（如 `https://blog.example.com`）指定对外域名，否则订阅源与站点地图里的链接会使用请求头推导出的地址。

---

## 文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目说明 | [README.md](README.md) | 本文档 |
| 开发指南 | [CLAUDE.md](CLAUDE.md) | Claude Code 开发规范 |
| Agent 指南 | [AGENTS.md](AGENTS.md) | AI Agent 项目指南（入口页） |
| Agent 细分指南 | [docs/agents/](docs/agents/) | `backend.md` / `frontend.md`：可复制的命令、闸门基线与易踩陷阱 |
| 更新日志 | [CHANGELOG.md](CHANGELOG.md) | 版本变更记录 |
| 后端文档 | [backend/README.md](backend/README.md) | 后端详细文档 |
| 前端文档 | [frontend/README.md](frontend/README.md) | 前端详细文档 |
| 部署文档 | [backend/DEPLOY.md](backend/DEPLOY.md) | 生产环境部署指南（含 Docker 与 Vercel） |
| 页面管理说明 | [docs/page-management.md](docs/page-management.md) | 后台逐页配置/内容页/导航使用指南 |
| 架构决策 | [docs/decisions/](docs/decisions/) | ADR：KV 版本化存储 |
| 验证记录 | [docs/reviews/](docs/reviews/) | 各批次真实验证证据与已知限制 |
| API 文档 | http://localhost:8825/doc.html | 启动后端后访问 |

---

## 更新日志

### v2.0.28 (2026-10-22)

- LLM 升级为 **DeepSeek 原生支持**：`llmProvider`（`deepseek`/`custom`）+ 一键预设、模型候选下拉（`GET /api/admin/seo/models` 探测上游 `/models`，失败回落到内置候选且恒 200）、思考模式与思考强度、`max_tokens`；402（余额不足）与 422（参数被拒）给出中文可执行文案。思考模式下**不再发 `temperature`**（上游会静默忽略）
- 新增五组 AI 能力，共 9 个 `POST /api/admin/ai/**` 端点：**写作辅助**（摘要 / 只从已有标签池挑标签 / 3 个标题候选，都不写库）、**评论审核**（单条复核 + 批量扫描 + 开启后访客留言 `afterCommit` 异步审核，命中转待审）、**内容校对**（只出问题清单，不改正文）、**图片描述**（`deepseek-flash` 图像理解，base64 内联）、**站点内容体检**（单次通读元数据，出评分与 findings）
- 修复 **`album_photo.caption` 从来没有写入路径**（列与 VO 都在，上传只写 file_path/mime_type/sort_order）→ 新增 `PUT /api/admin/albums/{albumId}/photos/{photoId}`，AI 生成的图注终于能落库
- 修复 **JSON 模式每次白吃一次 400**：DeepSeek 对 `response_format:json_object` 有文档外硬校验（提示词必须含 `json` 字样），原实现靠「400 去 `response_format` 重试」兜底，代价是每次多一个往返且第二次静默丢掉 JSON 模式；现在发请求前自动补提示词
- 修复 **图像能力只能靠 `/models` 预判**：真实 `deepseek-v4-pro`（纯文本）收到图片返回 **HTTP 200 + content 里写错误对象**，不抛 HTTP 错误；`listModels` 失败 fail-open、成功且 `supportsImage=false` 才 fail-closed 502
- 门禁：后端 **1140 测试**（本地 H2 + CI 真实 MySQL 8），前端 **51 文件 / 496 测试** + lint/format/type/build/部署契约 27/27；已用真实 DeepSeek Key 端到端验证 A–F 全部六组

### v2.0.27 (2026-10-22)

- 外观：新增 `--surface-paper` / `--surface-raised` / `--surface-control` 三级表面 token，修复自定义照片壁纸下文章正文、标题与右侧目录直接画在照片上的缺陷（`PageFrame.vue` 的背景属性此前在未配置时整个不渲染）；壁纸模式下 WCAG 对比度失败项 13→0、暗色 18→0、midnight 16→0，纯色画布与旧版像素一致
- 外观：修复首页无封面时 `.intro-visual` 留一块 43% 宽空灰矩形、关于页作者卡片与站点链接实心色块与周围半透明风格割裂、壁纸下 TOC 卡片悬空
- 后台新增 LLM 辅助 SEO：`post_seo` 独立表 + 8 个 `/api/admin/seo/**` 端点 + OpenAI 兼容客户端（JDK `HttpClient`，零新依赖），支持手工填写/模型生成/批量体检/发布后自动生成，服务端渲染输出 `meta[name=keywords]` 与独立分享图
- 后端：全仓 LIKE 通配符注入、限流按路径变量绕过、缓存自调用失效、MyBatis-Plus 逻辑删除与 `NOT_NULL` 更新策略等一批静默缺陷修复；SVG 消毒器改白名单、可信代理白名单、密码重置 OTP 不再写日志等安全加固
- 前端：清理 7 个零引用组件/模块与大批死 CSS、修复并发请求竞态与重定向校验、LLM 请求禁止自动重试
- 构建：`release/selfblog-2.0.27.jar` 随源码提交；前端版本号改由 `package.json` 构建期注入（`main.ts` 曾硬编码 `2.0.25` 落后两个版本）
- 门禁：后端 1004 测试（本地 H2 + CI 真实 MySQL 8），前端 47 文件 / 477 测试 + lint/format/type/build/部署契约 27/27；CI 四个 job 全绿

### v2.0.26 (2026-10-17)

- 新增 GitHub Actions CI（后端 H2 全量、前端全部门禁、真实 MySQL 8 全量测试、Docker 镜像健康检查与首页抓取），并补上 Maven Wrapper（`backend/mvnw` / `mvnw.cmd`）
- 服务端阅读 HTML 与浏览器共用同一套 GFM（表格/删除线/任务列表，共享 fixture `docs/fixtures/gfm-sample.md`）；首页 JSON-LD 增加 WebSite + SearchAction；标签站点地图补 `lastmod`
- 公开阅读 HTML（200）使用 ETag + `must-revalidate`，未变更请求返回 304；深色模式首屏不再闪烁（内联引导脚本先于首帧生效）
- 前端清理 25 个零引用遗留社区文件（保留 `/password-change`，并在后台设置中新增入口）；`format:check` 修复为真实可用的仓库级闸门
- `AGENTS.md` 拆分为入口页 + `docs/agents/backend.md` / `docs/agents/frontend.md`；`backend/schema.sql` 改名 `schema-fresh-install.sql`，删除无用的 `entrypoint.sh` 与 `backend/.dockerignore`
- 门禁：前端 43 文件 / 400 测试（lint/format/type/build/部署契约 27/27 全绿），后端 589 测试（本地 H2 与 CI 真实 MySQL）；CI 前两轮运行抓出并修复 5 类 H2 看不到的真实环境缺陷（MySQL `TIMESTAMPADD` 单位引号、趋势 upsert 绑定 null、SQL 中的下划线数字字面量、Docker 启动缺少 JWT 过期配置、MySQL 单表 UPDATE 赋值顺序导致锁定提前一次）

### v2.0.25 (2026-10-14)

- 将项目改名并改造为 SelfBlog 单人作者个人博客：Vue 前端与 Spring Boot API 打入同一可执行 JAR，默认使用 SQLite 与 Caffeine
- 新增匿名访客统计、匿名文章点赞、可开关访客评论、公开/密码相册及站点资料、主题和音乐设置
- 后台改为可配置路径；公开页面隐藏登录/注册入口，文章写入和管理 API 仅限作者管理员
- SQLite 通过增量建表保留原文章；为已有 MySQL 数据库提供仅新增表的迁移脚本
- 更新单 JAR、.env 与数据库升级文档

---

### v2.0.22 (2026-06-09)

**Bug 修复**
- 修复昵称修改不生效（后端 `htmlSanitizer` 过度转义）
- 修复私信头像不显示（内联SVG改为静态文件）
- 修复时间显示错乱（Jackson `write-dates-as-timestamps: false`）
- 修复草稿ID提取失败（兼容数字和对象格式）
- 修复写文章加载旧内容（移除无条件 `fetchDraft()`）
- 修复搜索不全面（增加 `summary` 字段匹配）
- 修复移动端宽度不一致（`PostSearch.vue` 响应式）

**功能增强**
- Circle 弹窗添加 @提及用户功能
- Circle 弹窗支持多话题选择
- Circle 弹窗添加草稿自动保存/恢复
- CSS 添加标准 `line-clamp` 属性

**文档**
- 统一前端环境配置（`.env` 合并）
- 添加 Vercel 部署配置（`vercel.json`）
- 添加 SQLite 计数器修复脚本
- 添加项目技能（`.omc/skills/`）

---

## 开源协议

本项目基于 **MIT License** 开源。

MIT License 是一种宽松的软件许可证，允许任何人自由使用、复制、修改、合并、发布、分发、再许可和/或出售本软件的副本，只需在所有副本或重要部分中包含版权声明和许可声明。

详见 [LICENSE](LICENSE) 文件。

---

## 作者

**刘畅 (Liu Chang)**

- GitHub：[Xinghe-0203](https://github.com/Xinghe-0203)
- 博客：[blog.starsx.top](https://blog.starsx.top/)

---

## 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot) - Java Web 框架
- [Vue.js](https://vuejs.org/) - 前端 UI 框架
- [MyBatis Plus](https://baomidou.com/) - ORM 框架
- [Vite](https://vitejs.dev/) - 前端构建工具
- [Caffeine](https://github.com/ben-manes/caffeine) - Java 缓存库
