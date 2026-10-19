package com.selfblog.config.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 证明 SQLite 的 per-connection PRAGMA 在连接轮换后会不会丢，以及 {@link DatabaseConfig#buildSqliteDataSource} 修复后不再丢。
 *
 * <p>背景（performance.log 取证结论）：{@code synchronous} 是 per-connection 参数，
 * 新建连接默认回落 FULL(2)，每次提交都要 fsync。旧实现只在启动时对当时那一条连接执行一次 PRAGMA，
 * 而 HikariCP 默认 {@code maxLifetime=1800000ms} 会回收重建连接 → 写入偶发退化到秒级。
 *
 * <p>注意 HikariCP 会把 {@code maxLifetime < 30000} 的值重置回默认值，因此<b>不能</b>用「设一个极短
 * maxLifetime 等它自然过期」来制造轮换。本测试用 {@link HikariDataSource#evictConnection} 显式驱逐，
 * 强制下一次取连接时建立新的物理连接——这正是 HikariCP 回收连接时走的同一条路径。
 *
 * <p>本测试必须用<b>文件库</b>而非 {@code :memory:}：内存库每条连接是各自独立的数据库，
 * 无法区分「新连接」与「PRAGMA 丢失」。
 */
class SqlitePragmaPersistenceTest {

    /** SQLite PRAGMA synchronous 的取值：0=OFF, 1=NORMAL, 2=FULL, 3=EXTRA。 */
    private static final String NORMAL = "1";
    private static final String FULL = "2";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("负例对照：没有 PRAGMA 配置时，连接轮换会把 synchronous 丢回 FULL")
    void pragmaIsLostWithoutConfiguration() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("before.db");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            // 第一代连接：手工设成 NORMAL，模拟「启动时那一次 PRAGMA」
            try (Connection first = dataSource.getConnection()) {
                first.createStatement().execute("PRAGMA synchronous=NORMAL");
                assertEquals(NORMAL, DatabaseConfig.readPragma(first, "synchronous"),
                        "手工设置后第一代连接应为 NORMAL");
                dataSource.evictConnection(first);
            }

            // 第二代连接：不经过任何 PRAGMA，synchronous 回落 FULL —— 这就是线上偶发写慢的机制
            try (Connection second = dataSource.getConnection()) {
                assertEquals(FULL, DatabaseConfig.readPragma(second, "synchronous"),
                        "未配置 PRAGMA 时新连接必须回落 FULL，这正是修复要消除的行为");
            }
        }
    }

    @Test
    @DisplayName("修复后：buildSqliteDataSource 的 PRAGMA 跨连接轮换持续生效")
    void pragmaSurvivesRotationWithFactory() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("after.db");

        try (HikariDataSource dataSource = DatabaseConfig.buildSqliteDataSource(url, 1, 30000)) {
            try (Connection first = dataSource.getConnection()) {
                assertEquals(NORMAL, DatabaseConfig.readPragma(first, "synchronous"),
                        "新建连接即应为 NORMAL（由 dataSourceProperties 在建立物理连接时应用）");
                assertEquals("5000", DatabaseConfig.readPragma(first, "busy_timeout"));
                assertEquals("-50000", DatabaseConfig.readPragma(first, "cache_size"));
                assertEquals("2", DatabaseConfig.readPragma(first, "temp_store"));
                assertEquals("1", DatabaseConfig.readPragma(first, "foreign_keys"));
                assertEquals("268435456", DatabaseConfig.readPragma(first, "mmap_size"));
                assertEquals("wal", DatabaseConfig.readPragma(first, "journal_mode"));
                dataSource.evictConnection(first);
            }

            // 轮换后：7 项 PRAGMA 必须全部仍然生效（这是修复的核心断言）
            try (Connection second = dataSource.getConnection()) {
                assertEquals(NORMAL, DatabaseConfig.readPragma(second, "synchronous"),
                        "轮换后 synchronous 仍必须是 NORMAL");
                assertEquals("5000", DatabaseConfig.readPragma(second, "busy_timeout"));
                assertEquals("-50000", DatabaseConfig.readPragma(second, "cache_size"));
                assertEquals("2", DatabaseConfig.readPragma(second, "temp_store"));
                assertEquals("1", DatabaseConfig.readPragma(second, "foreign_keys"));
                assertEquals("268435456", DatabaseConfig.readPragma(second, "mmap_size"));
                assertEquals("wal", DatabaseConfig.readPragma(second, "journal_mode"));
                dataSource.evictConnection(second);
            }

            // 再轮换一代，确认不是偶然
            try (Connection third = dataSource.getConnection()) {
                assertEquals(NORMAL, DatabaseConfig.readPragma(third, "synchronous"),
                        "第三代连接 synchronous 仍必须是 NORMAL");
                assertNotNull(DatabaseConfig.readPragma(third, "journal_mode"));
            }
        }
    }

    @Test
    @DisplayName("sqlitePragmaProperties 的键名与驱动实际读取的键名一致")
    void pragmaPropertyKeysAreTheOnesTheDriverReads() {
        var properties = DatabaseConfig.sqlitePragmaProperties();
        assertEquals("WAL", properties.getProperty("journal_mode"));
        assertEquals("NORMAL", properties.getProperty("synchronous"));
        assertEquals("5000", properties.getProperty("busy_timeout"));
        assertEquals("-50000", properties.getProperty("cache_size"));
        assertEquals("MEMORY", properties.getProperty("temp_store"));
        assertEquals("268435456", properties.getProperty("mmap_size"));
        assertEquals("true", properties.getProperty("foreign_keys"));
        assertEquals(7, properties.size(), "不应夹带无关键");
    }
}
