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

产物：`target/selfblog.jar`。完整验证可执行 `mvn test`，该命令也会触发前端安装和构建。

## 2. 运行

将 `selfblog.jar` 和配置好的 `.env` 放在运行目录中，并从该目录启动，以便 SQLite 相对路径和 `.env` 路径正确解析：

```bash
cd /opt/selfblog
java -jar selfblog.jar
```

常用地址（默认端口 8825）：

| 页面 | 地址 |
|---|---|
| 博客 | `http://localhost:8825/` |
| 作者后台 | `http://localhost:8825/admin`（默认，可在后台修改） |
| API | `http://localhost:8825/api` |
| API 文档 | `http://localhost:8825/doc.html`（prod profile 下关闭） |
| 健康检查 | `http://localhost:8825/actuator/health` |

首次启动会用 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 创建管理员；没有固定默认密码。已有账号不会被覆盖，历史示例密码 `Admin123` 会在提供有效 `ADMIN_PASSWORD` 时被替换。

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

`migration-selfblog.sql` 可重复执行，不会删除或修改文章表。`schema.sql` 是历史全量初始化脚本，包含 `DROP TABLE`；**仅用于新建的空数据库，禁止对现有数据库执行**。

## 5. 本地开发

后端默认用 SQLite。复制并配置 `.env` 后：

```bash
cd backend
mvn spring-boot:run
```

前端单独热更新：

```bash
cd frontend
npm ci                         # Node 22.13+ 或 Node 24+
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
