# 单人博客部署指南

生产环境推荐使用 **一个可执行 JAR + 一个 `.env`**。JAR 已内置 Vue 页面、Spring Boot API 和 SPA 路由回退；默认使用 SQLite 文件数据库和 Caffeine 进程内缓存。无需在运行服务器安装 Node.js、npm、MySQL 或 Redis。

## 1. 构建

构建机需要 JDK 21、Maven 3.8+ 和网络访问。`frontend-maven-plugin` 会自动安装 Node.js 24/npm、安装前端锁定依赖、构建 Vue 并将静态文件打入 JAR。

```bash
cd backend
cp .env.example .env
```

编辑 `.env`，至少设置：

```properties
DB_TYPE=sqlite
DB_URL_SQLITE=jdbc:sqlite:campus_blog.db
JWT_SECRET=<至少32位的随机字符串>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<至少12位的唯一密码>
ADMIN_PATH=admin
```

`DotenvConfig` 使用 Java Properties 读取 `.env`，值不要加引号。不要把含真实密钥的 `.env` 提交到 Git。

构建：

```bash
mvn package -DskipTests
```

产物：`target/selfblog.jar`。完整验证可执行 `mvn test`，该命令也会触发前端安装和构建；只跑测试时请加 `-Dskip.npm=true -Dskip.installnodenpm=true`，避免每次重建前端。

## 2. 运行

将 `selfblog.jar` 和配置好的 `.env` 放在运行目录中，并从该目录启动，以便 SQLite 相对路径和 `.env` 路径正确解析：

```bash
cd /opt/selfblog
java -jar selfblog.jar
```

常用地址（默认端口 8825，由 `SERVER_PORT` 决定）：

| 页面 | 地址 |
|---|---|
| 博客 | `http://localhost:8825/` |
| 作者后台 | `http://localhost:8825/admin`（默认，可在后台修改） |
| API | `http://localhost:8825/api` |
| API 文档 | `http://localhost:8825/doc.html`（默认关闭，需 `SWAGGER_ENABLED=true`） |
| 健康检查 | `http://localhost:8825/actuator/health` |

### 必须激活 prod profile

生产部署**必须**设置 `SPRING_PROFILES_ACTIVE=prod`（两个 `docker-compose*.yml` 已声明）。未激活任何 profile 时 Spring 会回落到 `default`，`application-prod.yml` 全部失效：API 文档与 Knife4j 会按 `SWAGGER_ENABLED` 的默认值暴露、management 端点不收窄、日志分流也不再与 prod 对齐。自 v2.0.26 后 API 文档默认关闭（`springdoc.enabled` / `knife4j.enable` 的默认值均为 `false`），因此即使忘记设 profile 也不会把 Swagger 直接开到公网；本地要调试时显式设 `SWAGGER_ENABLED=true`。

端口与健康检查的契约：应用监听 `SERVER_PORT`（默认 8825），镜像与 compose 的健康检查读取同一个变量，因此用 `-e SERVER_PORT=xxxx` 覆盖端口时探针会跟随，不会出现「应用监听 8888、探针打 8825」导致容器被判不健康而反复重启。

首次启动会用 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 创建管理员；没有固定默认密码。已有账号不会被覆盖，历史示例密码 `Admin123` 会在提供有效 `ADMIN_PASSWORD` 时被替换。

### 登录会话升级注意

- 本次安全修复无需数据库迁移。访问令牌和刷新令牌新增 `credentialVersion`，由持久 BCrypt 密码哈希经域分离 HMAC 派生，令牌不暴露密码哈希。
- 升级前签发的无版本令牌一律返回 401，需要重新登录。修改密码、管理员重置及邮箱重置在数据库提交后使全部旧令牌失效；重启或另一实例不会恢复它们。已经鉴权并执行中的请求不能追溯取消。
- 多实例需使用同一数据库和 `JWT_SECRET`，并同时升级，不能让旧版实例继续接受旧令牌。每次鉴权/刷新读取当前用户凭据，数据库不可用时拒绝认证，不缓存旧版本。
- 普通登出、设备管理与刷新轮换的黑名单仍是进程内状态；本次未将它们升级为跨实例持久会话存储。
- 评论创建 `POST /api/guest-comments` 现在命中每 IP 每分钟 5 次的规则，第 6 次返回 429；限流计数仍在单个进程内，代理真实 IP 需按受信代理配置正确传递。

## 3. 数据与备份

- SQLite 默认文件：运行目录下的 `campus_blog.db`（保持与旧版默认路径一致）。部署时把既有数据库文件放在运行目录，或在 `.env` 中将 `DB_URL_SQLITE` 指向其实际位置。新表自动创建，已有文章表不会删除或重建。
- 上传图片：`uploads/`；相册图片：`gallery-media/`。目录在文件上传时创建。
- 部署时持久化 SQLite 数据库及两个媒体目录。备份 SQLite 前建议停服；也可使用 SQLite `.backup` 命令，以包含 WAL 中尚未 checkpoint 的数据。
- 更新 JAR 不会自动迁移已有 SQLite 文件中的文章数据。

## 4. 使用已有 MySQL 数据库（可选）

设置 `.env`：

```properties
DB_TYPE=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=campus_blog
DB_USERNAME=campus_blog
DB_PASSWORD=<至少8位>
```

若是**已有数据库**，先备份，再选中当前数据库执行只新增个人博客表的迁移：

```bash
mysql -u campus_blog -p campus_blog < migration-selfblog.sql
```

`migration-selfblog.sql` 可重复执行，不会删除或修改文章表。`schema-fresh-install.sql` 是全新安装脚本（原名 `schema.sql`，改名就是为了避免误执行），包含 `DROP TABLE`；**仅用于新建的空数据库，禁止对现有数据库执行**。

## 5. 本地开发

后端默认用 SQLite。复制并配置 `.env` 后：

```bash
cd backend
mvn spring-boot:run
```

前端单独热更新：

```bash
cd frontend
npm ci                         # Node 22.22+ / 24.15+ / 26+（engine-strict）
npm run dev                    # http://localhost:3000
```

Vite 将 `/api` 代理到 `http://localhost:8825`。可用 `npm run type-check`、`npm run test:run` 和 `npm run build` 验证前端。

## 6. 反向代理（可选）

JAR 自己服务 SPA 和 API。如需 TLS/域名，可让 Nginx 将所有路径反代到同一个 Spring Boot 端口：

```nginx
server {
    listen 443 ssl;
    server_name blog.example.com;

    location / {
        proxy_pass http://127.0.0.1:8825;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

设置真实 `CORS_ALLOWED_ORIGINS`；如果站点通过同源反代访问，通常不需要开放额外跨域来源。不要直接将数据库端口暴露到公网。

### 可信代理白名单（`TRUSTED_PROXY_CIDRS`）

客户端 IP 只信任**显式列出的代理**。默认 `TRUSTED_PROXY_CIDRS=127.0.0.1/32,::1/128`（仅回环）：只有来自本机回环的请求才允许用 `X-Forwarded-For` / `X-Real-IP` / `Proxy-Client-IP` 等头改写客户端 IP，其他来源一律忽略这些头，直接用 TCP 对端地址。

这条判定同时供限流分桶、审计日志与访客去重哈希使用，所以它既是安全边界也是正确性边界。

- **上面的 Nginx 反代**：Nginx 与 JAR 同机时对端就是 `127.0.0.1`，默认值即可。若 Nginx 在另一台机器上，必须把该机器的地址加进来，例如 `TRUSTED_PROXY_CIDRS=127.0.0.1/32,::1/128,10.0.0.5/32`。
- **Docker `-p 8825:8825`**：容器看到的对端是网桥网关（通常是 `172.17.0.1`），不是回环，因此默认不信任任何转发头——客户端 IP 会退化成网关地址。这是**安全的失败模式**（伪造头不再能改限流分桶或审计记录），但所有外部访客会共用一个限流桶。要让容器拿到真实客户端 IP，请显式把网关列入白名单，并确保只有你自己的代理能连到该端口。
- 支持逗号分隔的单个 IP 或 CIDR；非法项会被跳过并记日志；若整串都解析不出有效项，则回落到默认的回环列表（永远不会退化成“信任所有”）。
- 环境变量与 `app.trusted-proxies` 配置项等价，后者可写在 `application.yml` 里。

### 访客伪名化密钥（`VISITOR_HASH_SECRET`，可选）

访客去重、`ip_hash`、匿名点赞与游客留言的访客标识都用 HMAC-SHA256 伪名化后入库，**不再明文保存 IP**。密钥解析顺序：`app.visitor-hash-secret` → 环境变量 `VISITOR_HASH_SECRET` → `JWT_SECRET`。

- 不设置也能启动（回落到 `JWT_SECRET`），但**推荐显式设置一个与 `JWT_SECRET` 不同的随机串**：两个用途共用同一把密钥时，读到 JWT 密钥的人也能离线枚举访客哈希。
- 密钥缺失或为空时**启动直接失败**（`VisitorHashKeyConfig`），不会再退化成 `local-visitor-hash` 之类的公开常量。
- 换密钥会让新写入的哈希与历史行不同：旧记录保留旧哈希，升级后同一访客对**旧**记录的去重/「已点赞」判定会失配一次。这是刻意接受的一次性影响（不做数据迁移）。

### 邮件验证码（`MAIL_*` 与开发期开关）

找回密码/注册验证码走 SMTP（见 `.env.example` 的 `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` 等）。两条安全约定：

- 发送失败时验证码**立即作废**（存储条目在抛错前删除），日志里只有 `correlationId` 与脱敏邮箱，**不打印验证码、不打印完整收件地址**。
- 本地没有 SMTP 时，把 `app.mail.dev-console-code`（环境变量 `MAIL_DEV_CONSOLE_CODE`）显式设为 `true`：程序跳过真实发信，把验证码打到日志里，便于自测。**生产环境必须保持默认 `false`**，否则任何能读日志的人都能重置后台密码。

**生产环境请显式设置 `SITE_BASE_URL`**（例如 `SITE_BASE_URL=https://blog.example.com`）。它现在是 canonical、分享图、RSS/Atom、sitemap 与 robots 的正式对外 origin：设置后，伪造的 `Host`/`X-Forwarded-*` 头不会再把地址改成内网或攻击者域名。未设置时按直连请求推导，仅适合本地开发。

自 2026-10-16 起，作者也可以在**后台「网站设置 → 站点资料 → 网站域名」**里填写同一个 origin：

- **后台设置为准**：填了就用它（保存后下一次请求立即生效）；留空则回落到 `.env` 的 `SITE_BASE_URL`，再回落到当前访问地址。
- 只能填站点地址（可带端口），不能带路径、查询串或账号密码；非法输入会被后台拒绝并给出提示。
- 即使数据库里被直接改坏，服务端也只记录警告并回落，不会让整站 500。
- 采用 Vercel 时注意：Vercel 项目**构建期**仍使用它自己的 `SITE_BASE_URL` 环境变量做路由校验；运行期的 canonical/订阅/地图地址以本设置的优先级为准，建议两处保持一致。

反代需要**原样转发所有路径**到一个 JAR：页面 HTML、`/assets/**`、`/api/**`、`/uploads/**`、`/feed.xml`、`/sitemap*.xml`、`/robots.txt`。不要把 `/uploads` 改写成 `/api/uploads`，也不要让静态服务器自己回退 SPA。

## 7. 抓取与 SEO 行为（本批新增）

- 首次响应即为可阅读 HTML：文章、关于、自建内容页正文无需 JavaScript；Vue 挂载后替换该区域，交互不变。
- 登录后台、账户类历史路径与搜索页返回 `noindex,follow`；不存在的地址返回真正的 404，`/index.html` 308 到 `/`。
- `/sitemap.xml` 是 sitemap index，分片覆盖**全部**已发布文章、标签与自建页（每片 1000 条）；订阅源仍保留最近 100 篇的常规上限。
- 公开 HTML 只读取已发布内容，不增加阅读量；管理员登录也不会让草稿出现在 HTML 或站点地图里。
- 这些改动建立“可被抓取”的技术条件，**不代表搜索引擎已经收录**，也不承诺排名。建议上线后自行在搜索平台提交 `https://<正式域名>/sitemap.xml`。

## 8. Docker（可选，二选一）

构建上下文是**仓库根目录**，镜像内含 Maven+JDK21 构建阶段与 JRE 运行阶段；运行容器为非 root，健康检查用镜像内真实安装的 `curl`。

MySQL 方案（保留既有卷名，不会清空数据）：

```bash
docker compose -f backend/docker-compose.yml up -d --build
```

SQLite 方案（数据库与 WAL/SHM 持久化在 `sqlite_data:/app/data`）：

```bash
docker compose -f backend/docker-compose.sqlite.yml up -d --build
```

- 首次启动前把 `backend/.env.example` 复制为 `backend/.env` 并填写真实值；Compose 会把 `.env` 作为 `env_file` 传入。
- `backend/schema-fresh-install.sql`（原名 `schema.sql`）的初始化只在**空** MySQL 数据目录首次启动时执行，且脚本内部固定使用 `campus_blog` 库名；自定义库名需要自行调整 SQL。已有数据库改用 `migration-selfblog.sql`。
- 升级镜像不会自动迁移数据；持久化卷 `mysql_data`/`sqlite_data`、`app_uploads`、`app_gallery_media` 必须保留。**不要执行 `docker compose down -v`**，那会删除全部数据卷。
- 旧版镜像的 `entrypoint.sh` 与无用的 `backend/.dockerignore` 已删除（新镜像直接 `java -jar`，构建上下文本来就是仓库根目录）；环境校验仍由应用启动时执行。
- 本机没有可用 Docker 守护进程，容器镜像的构建与运行未在本地执行；已改由 `.github/workflows/ci.yml` 的 `docker` 任务在 GitHub Actions 上真实构建镜像、起容器、等健康检查并抓取服务端渲染首页。

## 9. Vercel 统一入口（可选）

Vercel 只作为**反向代理**指向已部署的 JAR，不再单独构建另一份前端；Java 与数据库仍运行在服务器/容器。

1. Vercel 项目 Root Directory 设为 `frontend` 仓库子目录，Framework Preset 选 **Other**（不要选 Vite 自动预案）。
2. 配置环境变量：
   - `SELFBLOG_BACKEND_ORIGIN`：可公网访问的源站 HTTPS origin（如 `https://origin.example.com`）。
   - `SITE_BASE_URL`：访客正式 HTTPS origin（与后端 `SITE_BASE_URL` 保持一致，如 `https://blog.example.com`）。
   - 两者必须不同，且不能是本 Vercel 部署自身；缺失或非法时构建直接失败（不会生成空站）。
3. Build Command 使用 `node scripts/build-vercel-proxy.mjs`，它会生成 Build Output API v3 的 `.vercel/output/config.json` 路由：把原路径与查询原样转发给源站，动态响应显式 `no-store`，保留 `DENY`/`nosniff` 等头，Preview 环境附带 `X-Robots-Tag: noindex,nofollow`。
4. 源站需允许 Vercel 转发过来；如需限制仅 Vercel 访问，请使用平台侧共享密钥并在源站校验。

注意事项：本仓库不做 Vercel 上的静态前端托管；没有公网源站、域名与账号时无法真实验证 CDN/TLS/部署预览，本次交付只包含生成器与其单元测试。

## 10. 页面管理升级与备份

本批页面管理不新增或修改数据库表；替换JAR后，未保存过页面配置的站点保持原有内容、字号和页面比例。后台入口增加「页面管理」，默认 `/admin/pages`；公开自建内容页为 `/pages/<slug>`。

- 更新前备份完整数据库和媒体目录。页面草稿、公开快照、地址预留及导航存于已有 `site_setting` 的 `page.draft.*`、`page.published.*`、`page.slug.*`、`page.navigation` 键，不能只备份网站设置接口。
- 如果旧后台路径正好为 `/pages`，先从原后台将入口修改为其他路径，再创建内容页；新版本不会抢占原入口。
- 内置页面恢复默认只改草稿，需保存并发布才改变线上。下线/删除内容页不会删除文章或相册。
- 预览使用同源空srcdoc和真实视口，不需要放宽 `X-Frame-Options: DENY`，也不需要对公网开放草稿接口。

单JAR浏览器联调已验证页面管理与公开内容页，详情见[验证记录](../docs/reviews/2026-10-16-page-management-verification.md)及[使用说明](../docs/page-management.md)。本批未修复旧Docker/Vercel配置，也未完成初始HTML正文SEO；不要把本地通过等同于真实公网收录。
