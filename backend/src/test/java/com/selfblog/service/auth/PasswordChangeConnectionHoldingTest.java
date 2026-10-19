package com.selfblog.service.auth;

import com.selfblog.common.exception.BusinessException;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.selfblog.utils.UserContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * F9 回归测试：{@code changePassword} 的 BCrypt 校验/编码不得持有数据库连接，
 * 且拆分事务后 CAS 防覆盖语义仍然成立。
 *
 * <p>与 {@link LoginConnectionHoldingTest} 同一机制：修复前 {@code changePassword} 带
 * {@code @Transactional}，事务内既做 {@code matches} 又做 {@code encode}（cost=12 各约
 * 0.4-0.5s），整段时间独占连接；生产池较小时阻塞全站其它请求。</p>
 *
 * <p>本测试把连接池压到 1 条，用可控慢的 {@code encode} 桩把「事务外哈希计算」窗口拉长，
 * 在这个窗口内断言：Hikari 活动连接数为 0，且主线程能用唯一连接完成真实查询而不超时。
 * 若 {@code changePassword} 重新把 BCrypt 包进事务，两条断言都会失败。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pwd-change-pool-holding;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.minimum-idle=0",
        "spring.datasource.hikari.connection-timeout=2000"
})
class PasswordChangeConnectionHoldingTest {

    private static final String USERNAME = "pwdprobe_author";
    private static final String USER_EMAIL = "pwdprobe_author@example.com";
    private static final String OLD_PASSWORD = "OldPassword123!";
    private static final String NEW_PASSWORD = "NewPassword456!";

    @Autowired
    private com.selfblog.service.auth.SysUserService sysUserService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DataSource dataSource;

    /**
     * {@code sysUserMapper.delete(null)} 是逻辑删除（{@code @TableLogic}），只置
     * {@code is_deleted=1}，用户名仍占用唯一索引，第二次插入同一用户名会撞
     * {@code Unique index ... SYS_USER(USERNAME)}。这里用物理 DELETE 真正清表。
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 必须是 {@link SpyBean} 而非 {@code @MockBean}：容器启动时 {@code DataInitializer}
     * 要用同一个 encoder 的 {@code encode()} 写管理员密码，mock 会返回 null 导致启动期
     * INSERT 违反 {@code PASSWORD} 的 NOT NULL 约束。Spy 保留真实 {@code matches()}，
     * 只对 {@code encode()} 打桩。
     */
    @SpyBean
    private BCryptPasswordEncoder passwordEncoder;

    private Long userId;

    @BeforeEach
    void setUp() {
        // 物理清表：逻辑删除会保留 username 的唯一索引占用，导致重复插入失败。
        jdbcTemplate.execute("DELETE FROM sys_user");
        SysUser user = new SysUser();
        user.setUsername(USERNAME);
        user.setEmail(USER_EMAIL);
        // 真实 cost=12 哈希，matches 走真实实现，因此这里必须是可校验的编码。
        user.setPassword(new BCryptPasswordEncoder(12).encode(OLD_PASSWORD));
        user.setNickname(USERNAME);
        user.setRole("admin");
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setIsDeleted(0);
        sysUserMapper.insert(user);
        userId = user.getId();
    }

    private String currentHash() {
        return sysUserMapper.selectById(userId).getPassword();
    }

    /** 让 encode 第一次被调用时阻塞在闩锁上，随后返回真实编码结果。 */
    private void blockNextEncode(CountDownLatch entered, CountDownLatch release) {
        doAnswer(invocation -> {
            entered.countDown();
            release.await(10, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(passwordEncoder).encode(anyString());
    }

    @Test
    @DisplayName("改密 BCrypt 编码期间连接池空闲：另一线程可用唯一连接完成查询且不超时")
    void bcryptEncodingDoesNotHoldDatabaseConnection() throws Exception {
        CountDownLatch encodeEntered = new CountDownLatch(1);
        CountDownLatch releaseEncode = new CountDownLatch(1);
        blockNextEncode(encodeEntered, releaseEncode);

        final Long targetId = userId;
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread changeThread = new Thread(() -> {
            try {
                sysUserService.changePassword(targetId, OLD_PASSWORD, NEW_PASSWORD);
            } catch (Throwable t) {
                outcome.set(t);
            }
        }, "change-password-thread");
        changeThread.start();

        try {
            assertTrue(encodeEntered.await(10, TimeUnit.SECONDS),
                    "改密线程应已进入 BCrypt 编码窗口");

            HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
            assertNotNull(pool, "测试应运行在 Hikari 连接池上");
            // 读用户短事务刚结束，给连接归还留一个很短的收敛窗口。
            long idleDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (pool.getActiveConnections() > 0 && System.nanoTime() < idleDeadline) {
                Thread.sleep(10);
            }
            assertEquals(0, pool.getActiveConnections(),
                    "BCrypt 编码期间不应有活动连接（修复前 changePassword 事务会占住唯一连接）");

            long startNanos = System.nanoTime();
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM sys_user")) {
                assertTrue(resultSet.next(), "并发查询应返回结果");
                assertTrue(resultSet.getInt(1) >= 1, "并发查询应能读到已提交的测试用户");
            }
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            System.out.println("[F9-EVIDENCE] changePassword 的 BCrypt 编码期间另一线程获取连接并完成查询耗时 "
                    + elapsedMs + "ms；活动连接数=" + pool.getActiveConnections());
            assertTrue(elapsedMs < 1500,
                    "BCrypt 编码期间另一线程应在 1.5s 内取得连接，实际 " + elapsedMs + "ms");
        } finally {
            releaseEncode.countDown();
            changeThread.join(10_000);
        }

        assertFalse(changeThread.isAlive(), "改密线程应已结束");
        assertEquals(null, outcome.get(), "改密应成功，实际异常=" + outcome.get());
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, currentHash()),
                "新密码应已写入并可通过校验");
    }

    @Test
    @DisplayName("CAS 语义：哈希读取与条件更新分处两事务时，过期哈希仍被 409 拒绝且绝不覆盖")
    void staleOldHashIsRejectedWith409AndNeverOverwrites() throws Exception {
        String readHash = currentHash();
        // 并发赢家要写入的哈希：在打桩之前用真实编码算出，避免主线程也被桩阻塞。
        String winnerHash = passwordEncoder.encode("Concurrent999!");

        CountDownLatch encodeEntered = new CountDownLatch(1);
        CountDownLatch releaseEncode = new CountDownLatch(1);
        blockNextEncode(encodeEntered, releaseEncode);

        final Long targetId = userId;
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread loser = new Thread(() -> {
            try {
                // 读到的快照是 readHash；matches 通过后阻塞在 encode，此时连接已归还。
                sysUserService.changePassword(targetId, OLD_PASSWORD, "AnotherPass123!");
            } catch (Throwable t) {
                outcome.set(t);
            }
        }, "change-password-loser");
        loser.start();

        try {
            assertTrue(encodeEntered.await(10, TimeUnit.SECONDS),
                    "输家线程应已进入 BCrypt 编码窗口（此时读事务已结束）");
            // 真实并发交错：输家读事务已提交结束，赢家在它计算新哈希期间改掉密码。
            assertEquals(1, sysUserMapper.updatePasswordIfUnchanged(targetId, readHash, winnerHash),
                    "并发赢家应先成功写入新密码");
        } finally {
            releaseEncode.countDown();
            loser.join(10_000);
        }

        assertFalse(loser.isAlive(), "输家线程应已结束");
        assertInstanceOf(BusinessException.class, outcome.get(), "过期哈希必须抛业务异常，而不是覆盖成功");
        BusinessException ex = (BusinessException) outcome.get();
        assertEquals(409, ex.getCode(), "CAS 失败必须仍抛 409");
        assertEquals("密码已发生变化，请重新登录后重试", ex.getMessage(), "409 消息必须不变");

        // 关键：拆分事务后 CAS 仍是唯一防覆盖保证，输家绝不能改写赢家的密码。
        String finalHash = currentHash();
        assertEquals(winnerHash, finalHash, "CAS 失败时不得改写密码");
        assertTrue(passwordEncoder.matches("Concurrent999!", finalHash), "赢家的密码必须保留");
        assertFalse(passwordEncoder.matches("AnotherPass123!", finalHash), "输家的新密码不得写入");
    }

    @Test
    @DisplayName("改密成功路径：新密码写入、旧密码失效")
    void successfulChangeWritesNewHash() {
        sysUserService.changePassword(userId, OLD_PASSWORD, NEW_PASSWORD);
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, currentHash()), "新密码应生效");
        assertFalse(passwordEncoder.matches(OLD_PASSWORD, currentHash()), "旧密码应失效");
    }

    @Test
    @DisplayName("语义回归：旧密码错 400、用户不存在 404、弱密码 400 且不落库")
    void validationSemanticsUnchanged() {
        BusinessException wrongOld = assertThrows(BusinessException.class,
                () -> sysUserService.changePassword(userId, "WrongOldPass1!", NEW_PASSWORD));
        assertEquals(400, wrongOld.getCode());
        assertEquals("旧密码不正确", wrongOld.getMessage());

        BusinessException missing = assertThrows(BusinessException.class,
                () -> sysUserService.changePassword(999999L, OLD_PASSWORD, NEW_PASSWORD));
        assertEquals(404, missing.getCode());
        assertEquals("用户不存在", missing.getMessage());

        BusinessException weak = assertThrows(BusinessException.class,
                () -> sysUserService.changePassword(userId, OLD_PASSWORD, "weakpass"));
        assertEquals(400, weak.getCode(), "弱密码必须在 CAS 之前被拒");
        assertTrue(passwordEncoder.matches(OLD_PASSWORD, currentHash()), "弱密码被拒后原密码不得改变");
    }

    // ==================== F9：两个 resetPassword 重载的并发量化证据 ====================

    @Test
    @DisplayName("管理员重置密码(resetPassword(Long)) BCrypt 编码期间连接池空闲")
    void adminResetBcryptEncodingDoesNotHoldDatabaseConnection() throws Exception {
        CountDownLatch encodeEntered = new CountDownLatch(1);
        CountDownLatch releaseEncode = new CountDownLatch(1);
        blockNextEncode(encodeEntered, releaseEncode);

        final Long targetId = userId;
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        AtomicReference<String> issuedPassword = new AtomicReference<>();
        Thread resetThread = new Thread(() -> {
            // SecurityContextHolder 默认 THREADLOCAL，resetPassword(Long) 的成功日志要读当前管理员，
            // 因此必须在本线程内设置上下文，不能在主线程设置后跨线程复用。
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    new UserContext(1L, "admin"), null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
            try {
                issuedPassword.set(sysUserService.resetPassword(targetId));
            } catch (Throwable t) {
                outcome.set(t);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }, "reset-password-by-id-thread");
        resetThread.start();

        try {
            assertTrue(encodeEntered.await(10, TimeUnit.SECONDS),
                    "重置线程应已进入 BCrypt 编码窗口");
            assertPoolIsFreeAndQueryable("resetPassword(Long)");
        } finally {
            releaseEncode.countDown();
            resetThread.join(10_000);
        }

        assertFalse(resetThread.isAlive(), "重置线程应已结束");
        assertEquals(null, outcome.get(), "重置应成功，实际异常=" + outcome.get());
        assertNotNull(issuedPassword.get(), "应返回随机新密码");
        assertTrue(passwordEncoder.matches(issuedPassword.get(), currentHash()),
                "返回的随机密码应能通过真实校验");
    }

    @Test
    @DisplayName("邮箱重置密码(resetPassword(String,String)) BCrypt 编码期间连接池空闲")
    void emailResetBcryptEncodingDoesNotHoldDatabaseConnection() throws Exception {
        CountDownLatch encodeEntered = new CountDownLatch(1);
        CountDownLatch releaseEncode = new CountDownLatch(1);
        blockNextEncode(encodeEntered, releaseEncode);

        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread resetThread = new Thread(() -> {
            try {
                sysUserService.resetPassword(USER_EMAIL, NEW_PASSWORD);
            } catch (Throwable t) {
                outcome.set(t);
            }
        }, "reset-password-by-email-thread");
        resetThread.start();

        try {
            assertTrue(encodeEntered.await(10, TimeUnit.SECONDS),
                    "邮箱重置线程应已进入 BCrypt 编码窗口");
            assertPoolIsFreeAndQueryable("resetPassword(String,String)");
        } finally {
            releaseEncode.countDown();
            resetThread.join(10_000);
        }

        assertFalse(resetThread.isAlive(), "邮箱重置线程应已结束");
        assertEquals(null, outcome.get(), "邮箱重置应成功，实际异常=" + outcome.get());
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, currentHash()), "新密码应已生效");
    }

    /**
     * 在「BCrypt 编码进行中」这个确定时刻断言连接池真的空闲：活动连接数为 0，
     * 且当前线程能用唯一连接完成一次真实查询而不超时。
     */
    private void assertPoolIsFreeAndQueryable(String methodLabel) throws Exception {
        HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
        assertNotNull(pool, "测试应运行在 Hikari 连接池上");
        // 读用户短事务刚结束，给连接归还留一个很短的收敛窗口。
        long idleDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (pool.getActiveConnections() > 0 && System.nanoTime() < idleDeadline) {
            Thread.sleep(10);
        }
        assertEquals(0, pool.getActiveConnections(),
                methodLabel + " 的 BCrypt 编码期间不应有活动连接（修复前事务会占住唯一连接）");

        long startNanos = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM sys_user")) {
            assertTrue(resultSet.next(), "并发查询应返回结果");
            assertTrue(resultSet.getInt(1) >= 1, "并发查询应能读到已提交的测试用户");
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        System.out.println("[F9-EVIDENCE] " + methodLabel + " 的 BCrypt 编码期间另一线程获取连接并完成查询耗时 "
                + elapsedMs + "ms；活动连接数=" + pool.getActiveConnections());
        assertTrue(elapsedMs < 1500,
                methodLabel + " 的 BCrypt 编码期间另一线程应在 1.5s 内取得连接，实际 " + elapsedMs + "ms");
    }
}
