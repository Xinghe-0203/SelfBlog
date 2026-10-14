# SelfBlog — 个人博客

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/version-v2.0.25-blue)](https://github.com/Xinghe-0203)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-green)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4.21-brightgreen)](https://vuejs.org/)
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
| **Vue** | 3.4.21 | UI 框架（Composition API） |
| **Vite** | 5.2.0 | 构建工具 |
| **Vue Router** | 4.3.0 | 前端路由 |
| **Pinia** | 2.1.7 | 状态管理 |
| **Axios** | 1.7.4 | HTTP 客户端 |
| **Marked** | 12.0.1 | Markdown 渲染 |
| **DOMPurify** | 3.0.9 | XSS 防护 |
| **Highlight.js** | 11.9 | 代码高亮 |
| **npm** | 9+ | 包管理工具 |

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
| **CSS** | 前端样式（Rainy Glassmorphism 设计系统） |
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
- 访客可匿名点赞、提交文章留言；评论和访客统计可在后台开关
- 支持公开相册与短期令牌保护的密码相册

### 管理后台
- 作者工作台、文章、标签、访客、评论、相册和网站设置管理
- 可调整后台路径、站点资料、主题皮肤、强调色和背景音乐
- 历史文章与社区数据表保留；作者写入与管理 API 仅管理员可访问

---

## 项目结构

```
SelfBlog/
├── backend/                    # 后端（Spring Boot 3 + Java 21）
│   ├── pom.xml                     # Maven 依赖配置
│   ├── Dockerfile                  # Docker 构建文件
│   ├── docker-compose.yml          # Docker Compose 编排
│   ├── schema.sql                  # 数据库建表脚本
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
│       ├── api/                    # API 模块（17 个）
│       ├── views/                  # 页面组件（30 个）
│       ├── components/             # 通用组件（11 个）
│       ├── stores/                 # Pinia 状态管理（3 个 Store）
│       ├── router/                 # 路由配置（模块化）
│       ├── composables/            # 组合式函数
│       ├── styles/                 # 全局样式（Rainy Glassmorphism）
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
| Node.js | 22.13+ 或 24+ | 仅构建时由 Maven 插件自动安装；单 JAR 运行无需 Node |
| MySQL | 8.0+ | 可选数据库；默认 SQLite 无需安装 |

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

如要使用 MySQL，将 `.env` 中的 `DB_TYPE` 改为 `mysql` 并填写完整连接信息。全新空数据库可按 `backend/schema.sql` 初始化；**已有数据库只运行** `backend/migration-selfblog.sql` 增加博客功能所需表。不要对有数据的数据库运行完整 `schema.sql`，该历史初始化脚本包含删表语句。

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

在原有文章和社区表之外，个人博客功能新增 `site_setting`、`visitor_event`、`anonymous_post_like`、`guest_comment`、`photo_album` 和 `album_photo`。SQLite 启动时增量初始化；现有 MySQL 数据库使用 `migration-selfblog.sql`。

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
| JWT 认证 | Access Token（24h）+ Refresh Token（7d），支持令牌轮换 |
| 令牌黑名单 | 登出时加入黑名单，防止重放攻击 |
| 登录锁定 | 5 次失败锁定 15 分钟（原子操作） |
| XSS 防护 | 服务端 Jsoup HTML 净化 + 前端 DOMPurify |
| 限流 | 23 个端点限流（RateLimitInterceptor） |
| SQL 注入 | MyBatis Plus 参数化查询 |
| CORS | 可配置白名单，生产环境禁止 `*` |
| 数据校验 | Spring Validation（@Valid、@NotBlank） |
| 博客访问控制 | 公开阅读/搜索、匿名点赞与留言；写入和管理 API 仅限管理员；注册关闭 |

---

## API 文档

启动后端后访问 Knife4j API 文档：

**http://localhost:8825/doc.html**（`prod` profile 下关闭）

公开 API 提供已发布文章、标签、搜索、访客统计采集、匿名点赞、访客评论和相册访问。文章写入、站点配置、访客/评论/相册管理等接口要求作者管理员身份；注册关闭。

---

## 文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目说明 | [README.md](README.md) | 本文档 |
| 开发指南 | [CLAUDE.md](CLAUDE.md) | Claude Code 开发规范 |
| Agent 指南 | [AGENTS.md](AGENTS.md) | AI Agent 项目指南 |
| 更新日志 | [CHANGELOG.md](CHANGELOG.md) | 版本变更记录 |
| 后端文档 | [backend/README.md](backend/README.md) | 后端详细文档 |
| 前端文档 | [frontend/README.md](frontend/README.md) | 前端详细文档 |
| 部署文档 | [backend/DEPLOY.md](backend/DEPLOY.md) | 生产环境部署指南 |
| API 文档 | http://localhost:8825/doc.html | 启动后端后访问 |

---

## 更新日志

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
