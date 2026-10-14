# ============================================
# SelfBlog - Dockerfile
# 多阶段构建：Build Stage + Runtime Stage
# ============================================

# ============================================
# Stage 1: Build Stage
# 使用 JDK 21 构建应用
# ============================================
FROM eclipse-temurin:21-jdk AS build

# 设置工作目录
WORKDIR /build

# 复制 Maven 配置文件（利用 Docker 缓存层）
COPY pom.xml .

# 下载依赖（单独一层，利用缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用（跳过测试以加快构建速度）
RUN mvn clean package -DskipTests

# ============================================
# Stage 2: Runtime Stage
# 使用 JRE 21 运行应用（镜像更小）
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# ============================================
# 安全配置：创建非 root 用户运行应用
# ============================================
# 创建应用目录
RUN mkdir -p /app/uploads && \
    addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 设置工作目录
WORKDIR /app

# 复制构建产物（从上一个阶段）
COPY --from=build /build/target/backend-0.0.1-SNAPSHOT.jar app.jar

# 复制初始化脚本
COPY entrypoint.sh /app/entrypoint.sh

# 创建上传目录（持久化）并设置权限
RUN chown -R appuser:appgroup /app

# 安装 wget 用于健康检查（非 root 用户可执行）
RUN apk add --no-cache wget mysql-client

# 设置生产环境Profile
ENV SPRING_PROFILES_ACTIVE=prod

# ============================================
# 环境变量配置
# ============================================
# 默认环境变量（生产环境应通过 -e 参数或 env_file 覆盖）
ENV SERVER_PORT=8825 \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC" \
    TZ=Asia/Shanghai

# 暴露端口
EXPOSE 8825

# ============================================
# 健康检查配置
# ============================================
# 检查应用健康状态：等待 30 秒后开始检查，每 10 秒检查一次，超时 5 秒
# 如果连续 3 次检查失败则认为容器不健康
# 使用 Spring Actuator health 端点而非 Knife4j API 文档页面
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8825/actuator/health >/dev/null 2>&1 || exit 1

# ============================================
# 启动配置
# ============================================
# 设置入口点脚本执行权限（必须在 USER 之前执行）
RUN chmod +x /app/entrypoint.sh

# 切换到非 root 用户
USER appuser

# 入口脚本处理环境变量和数据库迁移
ENTRYPOINT ["/app/entrypoint.sh"]
