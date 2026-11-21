# 发布产物（release）

本目录存放可直接运行的构建产物，与源码一起提交，便于不装 JDK/Maven/Node 的机器直接取用。

| 文件 | 版本 | 大小 | SHA-256 |
|---|---|---|---|
| `selfblog-2.0.30.jar` | 2.0.30 | 67,851,640 B | `6C8FF4A78ECA6AB02228830AC1497896DF6140F100B55243466F9D685F88B2C0` |

> **版本提示（2026-11-21）**：本 JAR 对应当前源码版本 **2.0.30**（提交 `4925379`，
> 含 v2.0.30 全部六个批次：AI 写作助手、深度审计修复、UI 重做与打磨、全文订阅、
> 语法高亮等），与提交的源码一一对应。

## 这个 JAR 是什么

Spring Boot 单 JAR（fat jar）：内嵌 Tomcat、全部后端依赖、以及已构建的 Vue 前端产物
（`BOOT-INF/classes/static/` 下 82 个条目）。运行它不需要 Node、npm、Nginx 或外挂 Tomcat。

## 运行

JAR 与配置好的 `.env` 放在**同一运行目录**，并从该目录启动（SQLite 相对路径与 `.env` 查找
都依赖工作目录）：

```bash
java -jar selfblog-2.0.30.jar
```

默认端口 8825：博客 `http://localhost:8825/`，后台 `http://localhost:8825/admin`，
健康检查 `http://localhost:8825/actuator/health`。

`.env` 至少需要 `JWT_SECRET`（≥32 位）与 `ADMIN_PASSWORD`（≥12 位），否则启动即失败。
参考 `backend/.env.example`。

## 构建来源与校验

由仓库自身的构建链产出，命令（在 `backend/` 下执行）：

```bash
cd frontend && npm ci && npm run build          # 产物进 frontend/dist
cd ../backend
# 先删上一轮的静态产物：copy-resources 只增不删，否则旧 bundle 会累积进 jar
rm -rf target/classes/static
./mvnw package -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true
# 产物：backend/target/selfblog.jar
```

`backend/target/` 与 `*.jar` 被 `.gitignore` 忽略（构建产物不进版本控制的默认策略），
所以发布用的副本放在本目录并显式提交。校验：

```bash
sha256sum selfblog-2.0.30.jar
# 6c8ff4a78eca6ab02228830ac1497896df6140f100b55243466f9d685f88b2c0
```

## 注意

- 打包时若本机有正在运行的旧后端（`java -jar target/selfblog.jar`），Windows 会锁死该
  文件导致 repackage 报 `Unable to rename ... .original`——先停掉旧进程再打包。
- 生产部署请以 `prod` profile 运行（`SPRING_PROFILES_ACTIVE=prod`），否则 Swagger 与
  management 端点会按默认配置对外暴露。
- 数据库、上传目录与相册媒体不在 JAR 内，需要持久化到容器/主机卷。
- 本机没有 Docker 守护进程，Docker 镜像构建与运行未在开发机验证；CI 的 docker job 是
  唯一已验证环境。
