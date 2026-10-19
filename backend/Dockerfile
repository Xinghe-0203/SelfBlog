# ============================================
# SelfBlog - Dockerfile
# 多阶段构建：Build Stage + Runtime Stage
#
# 构建上下文必须是仓库根目录（backend 与 frontend 是同级目录）：
#   docker build -f backend/Dockerfile -t selfblog .
# 或使用 backend/docker-compose*.yml（已配置 context: ..）。
#
# 【端口 / profile 契约】（2026-10-18 修复配置漂移）
#   - 监听端口由 SERVER_PORT 决定，默认 8825（application.yml: server.port: ${SERVER_PORT:8825}）。
#   - 镜像 ENV SERVER_PORT=8825，EXPOSE 8825，compose 也声明 8825。
#   - HEALTHCHECK 不再硬编码端口：用 CMD-SHELL + ${SERVER_PORT:-8825}，
#     这样即使运行时用 -e SERVER_PORT=xxxx 覆盖，探针也会打同一个端口。
#   - 生产必须以 prod profile 运行（compose 已设 SPRING_PROFILES_ACTIVE=prod）。
#     未激活 profile 会回落到 "default"，使 application-prod.yml 全部失效
#     （Swagger 对公网开放、management 暴露面不收窄、日志分流失效）。
# ============================================

# ============================================
# Stage 1: Build Stage
# Maven 与 JDK 21 都来自同一个官方镜像，构建机不需要本机 Maven/Node。
# frontend-maven-plugin 会在容器内安装锁定的 Node 24 / npm 并执行 npm ci。
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# 先复制后端声明与源码；前端目录与 backend 同级，插件通过 ../frontend 找到它。
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY frontend/ frontend/

# 产出 target/selfblog.jar（finalName=selfblog），前端已内嵌为 static。
RUN mvn -B -f backend/pom.xml clean package -DskipTests

# ============================================
# Stage 2: Runtime Stage
# 只保留 JRE、JAR 与健康检查工具；非 root 运行。
# ============================================
FROM eclipse-temurin:21-jre AS runtime

# curl 供 HEALTHCHECK 使用，镜像内必须真实存在（不再声明未安装的 wget）。
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -g 1001 appgroup \
    && useradd -u 1001 -g appgroup -m -s /usr/sbin/nologin appuser

WORKDIR /app

# 复制唯一的运行产物。数据库、上传与相册媒体都放到容器外的持久化卷。
COPY --from=build /build/backend/target/selfblog.jar /app/selfblog.jar

RUN mkdir -p /app/data /app/uploads /app/gallery-media \
    && chown -R appuser:appgroup /app

# 默认环境变量（生产请通过 env_file / -e 覆盖敏感值）。
# SQLite 的位置由 DB_URL_SQLITE 决定，指南中指向 /app/data。
ENV SERVER_PORT=8825 \
    TZ=Asia/Shanghai

EXPOSE 8825

# 数据库就绪由 Compose 的 depends_on: condition: service_healthy 负责，
# 应用自身仍会在启动时校验 JWT_SECRET / ADMIN_PASSWORD / 数据库配置。
#
# 探针端口必须与 SERVER_PORT 一致：这里用 shell 形式读 ${SERVER_PORT:-8825}，
# 避免「应用实际监听 8888、探针打 8825」导致容器被判不健康并反复重启。
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
    CMD-SHELL curl -f "http://localhost:${SERVER_PORT:-8825}/actuator/health" || exit 1

USER appuser

ENTRYPOINT ["java", "-jar", "/app/selfblog.jar"]
