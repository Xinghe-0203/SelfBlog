package com.selfblog.service.auth;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.auth.UserLoginRequest;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * F8 回归测试：BCrypt 密码校验不得持有数据库连接。
 *
 * <p>修复前 {@code SysUserServiceImpl.login} 带 {@code @Transactional}，而 BCrypt cost=12
 * 单次 {@code matches} 约 0.4-0.5s（生产实测登录 1.2-2.7s）。整个事务期间连接被独占，
 * SQLite 生产库连接池大小为 1，因此每次登录都会阻塞全站其它请求。</p>
 *
 * <p>本测试把连接池压到 1 条，用可控的慢 {@link BCryptPasswordEncoder} 桩把校验窗口
 * 拉长，然后在这个窗口内做两件事：</p>
 * <ol>
 *   <li>断言 Hikari 活动连接数为 0（校验期间没有事务占着连接）；</li>
 *   <li>在主线程用唯一连接完成一次真实查询，并要求它不超时（证明连接可用）。</li>
 * </ol>
 *
 * <p>若 {@code login} 重新把 BCrypt 包进事务，第 1 条会变成 1，第 2 条会撞上
 * {@code connection-timeout=2000ms} 抛 SQLException —— 两条断言都会失败。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:login-pool-holding;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.connection-timeout=2000"
})
class LoginConnectionHoldingTest {

    private static final String USERNAME = "poolprobe_author";

    @Autowired
    private com.selfblog.service.auth.SysUserService sysUserService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DataSource dataSource;

    /**
     * BCrypt 桩：真实 cost=12 一次约 0.45s，测试里用闩锁控制窗口，既不写死长等待，
     * 又能让“校验进行中”这个时刻确定可观察。
     *
     * <p>这里必须是 {@link SpyBean} 而不是 {@code @MockBean}：容器启动时
     * {@code DataInitializer} 要用同一个 encoder 的 {@code encode()} 写入管理员密码，
     * mock 会返回 null 从而让启动期 INSERT 违反 {@code PASSWORD} 的 NOT NULL 约束。
     * Spy 保留真实 {@code encode()}，只对 {@code matches()} 打桩。</p>
     */
    @SpyBean
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        sysUserMapper.delete(null);
        SysUser user = new SysUser();
        user.setUsername(USERNAME);
        // matches 被桩掉，这里的哈希内容不参与真实校验，仅用于证明密码快照被传递。
        user.setPassword("$2a$12$stubhashstubhashstubhashstubhashstubhashstubhashstubha");
        user.setNickname(USERNAME);
        user.setRole("admin");
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setIsDeleted(0);
        sysUserMapper.insert(user);
    }

    @Test
    @DisplayName("BCrypt 校验期间连接池空闲：另一线程可用唯一连接完成查询且不超时")
    void bcryptVerificationDoesNotHoldDatabaseConnection() throws Exception {
        CountDownLatch bcryptEntered = new CountDownLatch(1);
        CountDownLatch releaseBcrypt = new CountDownLatch(1);
        when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(invocation -> {
            bcryptEntered.countDown();
            releaseBcrypt.await(10, TimeUnit.SECONDS);
            return false;
        });

        UserLoginRequest request = new UserLoginRequest();
        request.setUsername(USERNAME);
        request.setPassword("WrongPassword123!");

        AtomicReference<Throwable> loginOutcome = new AtomicReference<>();
        Thread loginThread = new Thread(() -> {
            try {
                sysUserService.login(request);
            } catch (Throwable t) {
                loginOutcome.set(t);
            }
        }, "login-bcrypt-thread");
        loginThread.start();

        try {
            assertTrue(bcryptEntered.await(10, TimeUnit.SECONDS),
                    "登录线程应已进入 BCrypt 校验窗口");

            HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
            assertNotNull(pool, "测试应运行在 Hikari 连接池上");
            // 读用户事务刚结束，给连接归还留一个很短的收敛窗口（修复前这里是永久 1，不会收敛）。
            long idleDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (pool.getActiveConnections() > 0 && System.nanoTime() < idleDeadline) {
                Thread.sleep(10);
            }
            assertEquals(0, pool.getActiveConnections(),
                    "BCrypt 校验期间不应有活动连接（修复前 login 事务会占住唯一连接）");

            long startNanos = System.nanoTime();
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM sys_user")) {
                assertTrue(resultSet.next(), "并发查询应返回结果");
                assertTrue(resultSet.getInt(1) >= 1, "并发查询应能读到已提交的测试用户");
            }
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            System.out.println("[F8-EVIDENCE] BCrypt 校验期间另一线程获取连接并完成查询耗时 " + elapsedMs
                    + "ms；活动连接数=" + pool.getActiveConnections());
            assertTrue(elapsedMs < 1500,
                    "BCrypt 校验期间另一线程应在 1.5s 内取得连接，实际 " + elapsedMs + "ms");
        } finally {
            releaseBcrypt.countDown();
            loginThread.join(10_000);
        }

        assertFalse(loginThread.isAlive(), "登录线程应已结束");
        assertInstanceOf(BusinessException.class, loginOutcome.get(), "密码错误应抛 BusinessException");
        assertEquals(401, ((BusinessException) loginOutcome.get()).getCode());
    }
}
