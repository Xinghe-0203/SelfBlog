package com.selfblog.config.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

/**
 * 数据库配置类
 * 根据 DB_TYPE 环境变量动态切换 MySQL 或 SQLite 数据库
 * 仅在 DB_TYPE 环境变量存在时激活（测试环境不设置此变量，使用 Spring 默认 H2 数据源）
 */
@Configuration
@ConditionalOnProperty(name = "DB_TYPE")
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DB_TYPE:sqlite}")
    private String dbType;

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:3306}")
    private String dbPort;

    @Value("${DB_NAME:campus_blog}")
    private String dbName;

    @Value("${DB_USERNAME:root}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Value("${DB_URL_SQLITE:jdbc:sqlite:campus_blog.db}")
    private String sqliteUrl;

    /**
     * SQLite 连接池上限。
     *
     * <p>SQLite 在 WAL 模式下允许「多读 + 单写」并发：读者之间互不阻塞，写者与读者之间也不阻塞。
     * 因此把池从 1 提高后，收益全部落在<b>只读</b>查询上——生产 request.log 已确证同一页面会几乎
     * 同时发出 3 个互不相关的读请求（{@code GET /api/post/1/adjacent}、{@code /api/anonymous-likes/posts/1}、
     * {@code /api/guest-comments/post/1}），而 {@code maximumPoolSize=1} 会把它们强制串行，
     * 三者墙钟≈各自耗时之和（474 + 802 + 1074 ≈ 2350ms，实测）。
     *
     * <p>取 5 的依据：覆盖观测到的 3 路并发并留出余量（浏览器对同源通常并发 4~6 条连接），
     * 同时不给 SQLite 制造过多长驻连接——每条连接都有自己的页缓存（{@code cache_size=-50000}
     * 即 48MiB 上限，按需分配而非预分配），5 条的总内存上界仍可控。写请求在 WAL 下仍是单写者，
     * 多余的池容量只会让写-写竞争在 {@code busy_timeout=5000} 内排队，不会并行加速。
     */
    static final int SQLITE_POOL_SIZE = 5;

    @Bean
    public DataSource dataSource() {
        if ("sqlite".equalsIgnoreCase(dbType)) {
            return createSqliteDataSource();
        }
        return createMysqlDataSource();
    }

    private HikariDataSource createSqliteDataSource() {
        createSqliteParentDirectory(sqliteUrl);
        log.info("========================================");
        log.info("   使用 SQLite 数据库");
        log.info("   数据库路径: {}", sqliteUrl);
        log.info("========================================");

        HikariDataSource dataSource = buildSqliteDataSource(sqliteUrl, SQLITE_POOL_SIZE, 30000);

        // 初始化 SQLite Schema
        initSqliteSchema(dataSource);

        return dataSource;
    }

    /**
     * 构建 SQLite 数据源。抽成包级静态方法以便测试直接复用同一条构建路径。
     *
     * <p>关键点：SQLite 的 {@code synchronous} / {@code busy_timeout} / {@code cache_size} /
     * {@code temp_store} / {@code mmap_size} / {@code foreign_keys} 都是 <b>per-connection</b> 的，
     * 只有 {@code journal_mode=WAL} 会写进库文件而持久化。原实现只在启动时对「当时那一条」
     * 连接执行一次 PRAGMA，而 HikariCP 默认 {@code maxLifetime=1800000}（30 分钟）会把这条连接
     * 回收重建；新连接不再经过 PRAGMA，{@code synchronous} 回落到 SQLite 默认的 FULL，
     * 每次提交都要 fsync，写入从毫秒级退化到秒级（performance.log 里 2451ms 的单条 INSERT 即此）。
     *
     * <p>因此这里做两层防护：
     * <ol>
     *   <li>{@code setDataSourceProperties(...)}：把 PRAGMA 交给 sqlite-jdbc 的
     *       {@code SQLiteConfig} 在 <b>每次建立物理连接</b>时应用（探针 PROBE I 实测 gen1/gen2/gen3
     *       轮换后 7 项 PRAGMA 全部保持生效）。用普通字符串键而非 {@code org.sqlite.SQLiteConfig}，
     *       因为 sqlite-jdbc 在本项目是 {@code scope=runtime}，编译期不可引用。</li>
     *   <li>{@code maxLifetime(0)} + {@code idleTimeout(0)}：让池内连接长期驻留、不做无谓轮换（防御纵深）。
     *       两者都是 0=禁用；{@code idleTimeout} 非 0 时 HikariCP 会把 &lt;10000 的值抬到 10000。</li>
     * </ol>
     */
    static HikariDataSource buildSqliteDataSource(String jdbcUrl, int poolSize, long connectionTimeoutMs) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("SelfBlogSQLitePool");

        // SQLite 不适合高并发「写」，但 WAL 下多读并发是安全的：池 >1 让不相关的读请求真正并行
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(poolSize);
        config.setConnectionTimeout(connectionTimeoutMs);

        // 每条新建物理连接都自动应用这些 PRAGMA（per-connection 参数不能只设一次）
        config.setDataSourceProperties(sqlitePragmaProperties());

        // 防御纵深：这些连接长期驻留，不做空闲/寿命回收，避免重建时再走一遍 PRAGMA 应用路径
        config.setMaxLifetime(0);
        config.setIdleTimeout(0);

        return new HikariDataSource(config);
    }

    /**
     * SQLite 的 per-connection PRAGMA。键名与 sqlite-jdbc {@code SQLiteConfig.toProperties()} 产出的键一致
     * （探针 PROBE H 实测键名表），由驱动在建立连接时逐条应用。
     */
    static Properties sqlitePragmaProperties() {
        Properties properties = new Properties();
        properties.setProperty("journal_mode", "WAL");
        properties.setProperty("synchronous", "NORMAL");
        properties.setProperty("busy_timeout", "5000");
        properties.setProperty("cache_size", "-50000");
        properties.setProperty("temp_store", "MEMORY");
        properties.setProperty("mmap_size", "268435456");
        properties.setProperty("foreign_keys", "true");
        return properties;
    }

    /**
     * 读取当前连接生效的 PRAGMA 值（测试与诊断用）。名称取自白名单，避免拼接注入。
     */
    static String readPragma(Connection connection, String pragmaName) throws java.sql.SQLException {
        if (!java.util.Set.of("journal_mode", "synchronous", "busy_timeout", "cache_size",
                "temp_store", "foreign_keys", "mmap_size").contains(pragmaName)) {
            throw new IllegalArgumentException("不支持的 PRAGMA: " + pragmaName);
        }
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA " + pragmaName)) {
            return rs.next() ? rs.getString(1) : "<no row>";
        }
    }

    private void createSqliteParentDirectory(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:sqlite:")) {
            return;
        }
        String location = jdbcUrl.substring("jdbc:sqlite:".length()).split("\\?", 2)[0];
        if (location.isBlank() || ":memory:".equalsIgnoreCase(location) || location.startsWith("file:")) {
            return;
        }
        try {
            Path databasePath = Paths.get(location);
            if (!databasePath.isAbsolute()) {
                databasePath = Paths.get(System.getProperty("user.dir")).resolve(databasePath);
            }
            Path parent = databasePath.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new IllegalStateException("无法创建 SQLite 数据库目录", e);
        }
    }

    private HikariDataSource createMysqlDataSource() {
        log.info("========================================");
        log.info("   使用 MySQL 数据库");
        log.info("   主机: {}:{}", dbHost, dbPort);
        log.info("   数据库: {}", dbName);
        log.info("========================================");

        String useSsl = System.getProperty("DB_USE_SSL", "true");
        String url = String.format(
                "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=%s&requireSSL=%s",
                dbHost, dbPort, dbName, useSsl, useSsl
        );

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setPoolName("SelfBlogHikariCP");
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);
        config.setAutoCommit(true);

        return new HikariDataSource(config);
    }

    private void initSqliteSchema(DataSource dataSource) {
        try {
            ClassPathResource schemaResource = new ClassPathResource("schema-sqlite.sql");
            if (schemaResource.exists()) {
                DataSourceInitializer initializer = new DataSourceInitializer();
                initializer.setDataSource(dataSource);
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(schemaResource);
                populator.setSeparator(";");
                initializer.setDatabasePopulator(populator);
                initializer.afterPropertiesSet();
                log.info("SQLite Schema 初始化完成");
            } else {
                log.warn("未找到 SQLite Schema 文件: schema-sqlite.sql，请手动初始化数据库");
            }
        } catch (Exception e) {
            log.error("SQLite Schema 初始化失败: {}", e.getMessage(), e);
        }
    }
}
