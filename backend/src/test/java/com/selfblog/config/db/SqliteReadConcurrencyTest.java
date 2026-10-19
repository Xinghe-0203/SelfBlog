package com.selfblog.config.db;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 证明 {@code maximumPoolSize=1} 会把互不相关的读请求强制串行，以及把池提到 5 之后它们能真正并发。
 *
 * <p>生产 request.log 取证（同一页面、同一 IP、几乎同时发出的 3 个不相关请求）：
 * <pre>
 * GET /api/post/1/adjacent           status=200 duration=474ms
 * GET /api/guest-comments/post/1     status=200 duration=802ms
 * GET /api/anonymous-likes/posts/1   status=200 duration=1074ms
 * </pre>
 * 三者<b>严格串行</b>返回，总墙钟 ≈ 2350ms ≈ 三者耗时之和。若池 ≥3，总墙钟应接近单条最大值。
 *
 * <p>本测试用<b>真实 SQLite 临时文件库</b>（{@code @TempDir}）而不是 {@code :memory:}：
 * 内存库每条连接是各自独立的数据库，既测不出 PRAGMA 覆盖多连接，也无法体现 WAL 的多读并发。
 *
 * <p>并发度用两种独立方式度量，互为交叉验证：
 * <ol>
 *   <li><b>连接占用重叠</b>（不依赖 CPU/磁盘速度，最稳）：3 个线程在屏障处同时开始，各自从池里
 *       取一条连接并持有 {@value #HOLD_MILLIS}ms。池=1 时墙钟必然 ≈3×持有时间（串行），
 *       池=5 时墙钟 ≈1×持有时间（重叠）。</li>
 *   <li><b>真实只读查询</b>：同一查询先串行跑 3 次求和时间，再并发跑 3 次取墙钟，
 *       断言并发墙钟 &lt; 串行和的 70%。</li>
 * </ol>
 */
class SqliteReadConcurrencyTest {

    /** 每个线程持有连接的时间（毫秒）。取足够大以淹没线程调度抖动。 */
    private static final long HOLD_MILLIS = 150;

    /** 并发/串行对比的阈值：并发墙钟必须低于串行和的 70%（理想值约 33%~45%）。 */
    private static final double MAX_CONCURRENT_RATIO = 0.70;

    /**
     * 只读查询的递归深度。{@code SELECT sum(x)} 是纯 SQLite 侧 CPU 计算（聚合在 SQLite 内部完成，
     * 不产生逐行 JNI 回调），因此耗时稳定、不受磁盘抖动影响，适合用来量化并发度。
     */
    private static final int RECURSION_DEPTH = 1_000_000;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("池=5：可同时取出 3 条不同连接，且 PRAGMA 在每条上都生效")
    void poolOfFiveServesThreeDistinctConnectionsEachWithPragmas() throws Exception {
        assertEquals(5, DatabaseConfig.SQLITE_POOL_SIZE, "SQLite 池上限应为 5（有依据的小值）");
        String url = "jdbc:sqlite:" + tempDir.resolve("three-connections.db");

        try (HikariDataSource dataSource = DatabaseConfig.buildSqliteDataSource(url, DatabaseConfig.SQLITE_POOL_SIZE, 30000)) {
            List<Connection> held = new ArrayList<>();
            try {
                // 同时持有 3 条连接且都不释放：池=1 时第三条 getConnection 会直接超时失败
                for (int i = 0; i < 3; i++) {
                    Connection connection = dataSource.getConnection();
                    held.add(connection);
                }

                assertEquals(3, held.size());
                assertTrue(held.get(0) != held.get(1) && held.get(1) != held.get(2) && held.get(0) != held.get(2),
                        "3 条连接必须是不同的物理连接（同一池=1 时不可能同时取出 3 条）");
                assertNotSame(held.get(0).unwrap(Connection.class), held.get(1).unwrap(Connection.class));

                // 逐条断言 PRAGMA：一举证明「池>1」+「task-9 的 dataSourceProperties 覆盖每条新连接」
                for (int i = 0; i < held.size(); i++) {
                    Connection connection = held.get(i);
                    assertTrue(connection.isValid(5), "第 " + (i + 1) + " 条连接应有效");
                    assertEquals("1", DatabaseConfig.readPragma(connection, "synchronous"),
                            "第 " + (i + 1) + " 条连接 synchronous 应为 NORMAL(1)");
                    assertEquals("wal", DatabaseConfig.readPragma(connection, "journal_mode"),
                            "第 " + (i + 1) + " 条连接 journal_mode 应为 wal（多读并发的前提）");
                    assertEquals("5000", DatabaseConfig.readPragma(connection, "busy_timeout"),
                            "第 " + (i + 1) + " 条连接 busy_timeout 应为 5000（写竞争排队兜底）");
                    assertEquals("1", DatabaseConfig.readPragma(connection, "foreign_keys"));
                    System.out.printf("[task-11] connection#%d id=%s isValid=%s synchronous=%s journal_mode=%s busy_timeout=%s%n",
                            i + 1, System.identityHashCode(connection), connection.isValid(5),
                            DatabaseConfig.readPragma(connection, "synchronous"),
                            DatabaseConfig.readPragma(connection, "journal_mode"),
                            DatabaseConfig.readPragma(connection, "busy_timeout"));
                }
                var mxBean = dataSource.getHikariPoolMXBean();
                if (mxBean != null) {
                    System.out.printf("[task-11] poolSize(configured)=%d activeConnections=%d idleConnections=%d%n",
                            DatabaseConfig.SQLITE_POOL_SIZE, mxBean.getActiveConnections(), mxBean.getIdleConnections());
                }
            } finally {
                for (Connection connection : held) {
                    connection.close();
                }
            }
        }
    }

    @Test
    @DisplayName("修复前（池=1）：3 个并发读被强制串行，墙钟≈持有时间之和")
    void singleConnectionPoolSerializesOverlappingReads() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("pool-one.db");

        try (HikariDataSource dataSource = DatabaseConfig.buildSqliteDataSource(url, 1, 30000)) {
            long elapsed = runConcurrently(3, () -> {
                try (Connection connection = dataSource.getConnection()) {
                    assertTrue(connection.isValid(5));
                    Thread.sleep(HOLD_MILLIS);
                }
                return null;
            });

            long serialSum = 3 * HOLD_MILLIS;
            System.out.printf("[task-11] poolSize=1 concurrent-3 wallClock=%dms serialSum=%dms ratio=%.2f%n",
                    elapsed, serialSum, (double) elapsed / serialSum);
            assertTrue(elapsed >= serialSum * 0.9,
                    "池=1 时 3 个并发请求必须串行排队：墙钟应≈" + serialSum + "ms，实测 " + elapsed + "ms");
        }
    }

    @Test
    @DisplayName("修复后（池=5）：3 个并发读真正重叠，墙钟远小于串行之和")
    void fiveConnectionPoolAllowsOverlappingReads() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("pool-five.db");

        try (HikariDataSource dataSource = DatabaseConfig.buildSqliteDataSource(url, DatabaseConfig.SQLITE_POOL_SIZE, 30000)) {
            long elapsed = runConcurrently(3, () -> {
                try (Connection connection = dataSource.getConnection()) {
                    assertTrue(connection.isValid(5));
                    Thread.sleep(HOLD_MILLIS);
                }
                return null;
            });

            long serialSum = 3 * HOLD_MILLIS;
            double ratio = (double) elapsed / serialSum;
            System.out.printf("[task-11] poolSize=5 concurrent-3 wallClock=%dms serialSum=%dms ratio=%.2f%n",
                    elapsed, serialSum, ratio);
            assertTrue(ratio < MAX_CONCURRENT_RATIO,
                    "池=5 时 3 个并发请求应重叠：墙钟应<" + (long) (serialSum * MAX_CONCURRENT_RATIO)
                            + "ms，实测 " + elapsed + "ms（串行和的 " + String.format("%.2f", ratio) + "）");
        }
    }

    @Test
    @DisplayName("修复后（池=5）：3 个并发只读查询的总墙钟 < 串行之和的 70%")
    void threeConcurrentReadQueriesBeatSerialSum() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("concurrent-reads.db");

        try (HikariDataSource dataSource = DatabaseConfig.buildSqliteDataSource(url, DatabaseConfig.SQLITE_POOL_SIZE, 30000)) {
            // 串行基线：同一查询在池=5 的库上顺序跑 3 次，累加单次耗时
            long serialSum = 0;
            for (int i = 0; i < 3; i++) {
                serialSum += timeOneReadQuery(dataSource);
            }

            // 并发：屏障对齐后同时发起同一查询
            long concurrent = runConcurrently(3, () -> {
                runOneReadQuery(dataSource);
                return null;
            });

            double ratio = (double) concurrent / serialSum;
            System.out.printf("[task-11] readQuery depth=%d serialSum=%dms concurrentWallClock=%dms ratio=%.2f speedup=%.2fx%n",
                    RECURSION_DEPTH, serialSum, concurrent, ratio, (double) serialSum / concurrent);
            assertTrue(serialSum > 0, "串行基线必须可测");
            assertTrue(ratio < MAX_CONCURRENT_RATIO,
                    "3 个并发只读查询的总墙钟必须显著小于串行之和：应<" + (long) (serialSum * MAX_CONCURRENT_RATIO)
                            + "ms，实测 " + concurrent + "ms（串行和 " + serialSum + "ms）");
        }
    }

    @Test
    @DisplayName("池=5 下 schema 初始化仍正常：新建的每一代连接都能查到已建的表")
    void schemaInitializationIsVisibleToEveryPoolConnection() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("schema-pool.db");

        // 走与生产完全相同的构建路径：建池 -> initSqliteSchema（由 createSqliteDataSource 内部调用）
        DatabaseConfig config = new DatabaseConfig();
        HikariDataSource dataSource = DatabaseConfig.buildSqliteDataSource(url, DatabaseConfig.SQLITE_POOL_SIZE, 30000);
        try {
            java.lang.reflect.Method init = DatabaseConfig.class.getDeclaredMethod("initSqliteSchema", DataSource.class);
            init.setAccessible(true);
            init.invoke(config, dataSource);

            // 池>1 时 HikariCP 会按需新建连接；逐条确认它们都看得见 schema 建好的表
            List<Connection> held = new ArrayList<>();
            try {
                for (int i = 0; i < DatabaseConfig.SQLITE_POOL_SIZE; i++) {
                    held.add(dataSource.getConnection());
                }
                for (int i = 0; i < held.size(); i++) {
                    try (Statement statement = held.get(i).createStatement();
                         ResultSet rs = statement.executeQuery(
                                 "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='blog_post'")) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1),
                                "第 " + (i + 1) + " 条池连接都应能看到 initSqliteSchema 建好的 blog_post 表");
                    }
                }
                System.out.printf("[task-11] schema init OK with poolSize=%d, %d connections all see blog_post%n",
                        DatabaseConfig.SQLITE_POOL_SIZE, held.size());
            } finally {
                for (Connection connection : held) {
                    connection.close();
                }
            }
        } finally {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("写-写竞争风险：并发写在同一池内由 busy_timeout 排队，不抛 SQLITE_BUSY")
    void concurrentWritesQueueInsteadOfFailing() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("concurrent-writes.db");

        try (HikariDataSource dataSource = DatabaseConfig.buildSqliteDataSource(url, DatabaseConfig.SQLITE_POOL_SIZE, 30000)) {
            try (Connection setup = dataSource.getConnection();
                 Statement statement = setup.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS write_probe (id INTEGER PRIMARY KEY AUTOINCREMENT, v TEXT)");
            }

            // 5 个线程同时插入：SQLite 只有单写者，但 WAL + busy_timeout=5000 应让它们排队完成
            long elapsed = runConcurrently(5, () -> {
                try (Connection connection = dataSource.getConnection();
                     Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO write_probe (v) VALUES ('x')");
                }
                return null;
            });

            try (Connection verify = dataSource.getConnection();
                 Statement statement = verify.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT count(*) FROM write_probe")) {
                assertTrue(rs.next());
                assertEquals(5, rs.getInt(1), "5 个并发写应全部成功（排队而非报 SQLITE_BUSY）");
            }
            System.out.printf("[task-11] 5 concurrent writes completed in %dms with no SQLITE_BUSY%n", elapsed);
        }
    }

    /** 跑一次纯 CPU 的只读查询并返回耗时（毫秒）。 */
    private long timeOneReadQuery(HikariDataSource dataSource) throws Exception {
        long start = System.nanoTime();
        runOneReadQuery(dataSource);
        return (System.nanoTime() - start) / 1_000_000;
    }

    /** 真实只读查询：递归 CTE + 聚合，全部计算在 SQLite 内部完成。 */
    private void runOneReadQuery(HikariDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "WITH RECURSIVE cnt(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM cnt WHERE x < " + RECURSION_DEPTH + ") "
                             + "SELECT sum(x) FROM cnt")) {
            assertTrue(rs.next(), "只读查询应返回一行");
            assertTrue(rs.getLong(1) > 0, "sum 应为正数");
        }
    }

    /** 用 {@code n} 个线程在屏障处同时开始执行 {@code task}，返回总墙钟（毫秒）。 */
    private long runConcurrently(int n, Callable<Void> task) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(n);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await(30, TimeUnit.SECONDS);
                    return task.call();
                }));
            }
            long start = System.nanoTime();
            // 屏障在 submit 之后、计时开始前后即已释放；start 取在首个任务真正拿到锁之前
            for (Future<Void> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
            return (System.nanoTime() - start) / 1_000_000;
        } finally {
            pool.shutdownNow();
        }
    }
}
