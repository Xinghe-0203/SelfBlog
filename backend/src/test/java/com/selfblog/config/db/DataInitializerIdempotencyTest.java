package com.selfblog.config.db;

import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 启动播种必须对「被逻辑删除的同名管理员账户」幂等。
 *
 * <p>真实 CI 证据（run 39200430765 的 {@code Backend tests (MySQL 8)}）：669 tests / 161 errors。
 * 首因是
 * <pre>
 * org.springframework.dao.DuplicateKeyException:
 *   ### Cause: java.sql.SQLIntegrityConstraintViolationException:
 *   Duplicate entry 'test-admin' for key 'sys_user.uk_username'
 *   ### The error may involve com.selfblog.mapper.SysUserMapper.insert-Inline
 * 栈: com.selfblog.config.db.DataInitializer.initAdminAccount(DataInitializer.java:53)
 * </pre>
 * 机制：{@code SysUser.isDeleted} 带 {@code @TableLogic}，多个测试类在 {@code @BeforeEach} 调
 * {@code sysUserMapper.delete(null)} 是<b>逻辑删除</b>（{@code is_deleted=1}），用户名仍占用
 * {@code uk_username} 唯一索引；下一个 Spring 上下文启动时旧实现用过滤 {@code is_deleted} 的
 * {@code selectOne} 查不到 → 再次 INSERT → 撞唯一键 → 上下文加载失败 → 之后每个新上下文级联失败。
 * 这同时是真实生产缺陷：作者账户被软删除后重启会直接崩溃循环。
 *
 * <p><b>刻意不加 {@code @Transactional}</b>：播种必须真提交，才能观察到「逻辑删除后重启」的真实语义。
 * 清理一律用 {@code JdbcTemplate} 物理 DELETE——逻辑删除会保留唯一索引占用。
 */
@SpringBootTest
class DataInitializerIdempotencyTest {

    /** 与 backend/src/test/resources/application.yml:70-71 的 ADMIN_USERNAME/ADMIN_PASSWORD 一致。 */
    private static final String ADMIN_USERNAME = "test-admin";
    private static final String ADMIN_PASSWORD = "Test-Admin-Password-1234";

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearUsersPhysically() {
        jdbcTemplate.execute("DELETE FROM sys_user");
    }

    @AfterEach
    void leaveAdminAccountLive() {
        // 物理清表后重新播种，绝不把 test-admin 留成软删除状态给其它测试类。
        jdbcTemplate.execute("DELETE FROM sys_user");
        dataInitializer.run();
    }

    @Test
    void reinsertingAfterLogicalDeleteDoesNotFailStartup() {
        // 首次启动：播种成功。
        dataInitializer.run();
        assertNotNull(userId(), "首次播种后应存在管理员账户");
        assertEquals(0, isDeleted(), "首次播种的行必须是未删除状态");
        String passwordBefore = password();

        // 模拟 LoginConnectionHoldingTest 等测试类的 @BeforeEach：逻辑删除（用户名仍占唯一索引）。
        sysUserMapper.delete(null);
        assertEquals(1, isDeleted(), "sysUserMapper.delete(null) 应是逻辑删除");
        assertEquals(1, rowCount(), "逻辑删除不释放唯一索引，行仍在表中");

        // 修复前：这里抛 DuplicateKeyException，应用启动失败（CI 的 161 errors 首因）。
        assertDoesNotThrow(() -> dataInitializer.run(), "逻辑删除后再次启动不得撞 uk_username");

        assertEquals(0, isDeleted(), "重启后该行应被恢复为未删除");
        assertEquals(1, rowCount(), "不得插入重复行");
        assertEquals(passwordBefore, password(), "恢复被软删除账户时不得重置作者密码");
    }

    @Test
    void secondStartupOnLiveRowIsNoop() {
        dataInitializer.run();
        String passwordBefore = password();

        assertDoesNotThrow(() -> dataInitializer.run());
        assertDoesNotThrow(() -> dataInitializer.run());

        assertEquals(1, rowCount(), "已存在的管理员账户不得被重复插入");
        assertEquals(passwordBefore, password(), "已存在时不得改写密码哈希");
        assertEquals(0, isDeleted());
    }

    // ---- 并发竞态兜底（insert 撞唯一键后重新查询）的纯单元验证 ----
    // 用独立实例而非注入的 Spring Bean，避免污染被缓存的共享上下文。

    @Test
    void insertRaceRecoversWhenTheRowAppearsConcurrently() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        // 第一次查：不存在 → 走 insert；insert 撞唯一键（另一实例刚插入/恢复）；catch 内再查：已存在。
        SysUser concurrent = new SysUser();
        concurrent.setId(42L);
        concurrent.setUsername(ADMIN_USERNAME);
        concurrent.setIsDeleted(0);
        when(mapper.selectRawByUsername(ADMIN_USERNAME)).thenReturn(null, concurrent);
        when(mapper.insert(any(SysUser.class))).thenThrow(
                new DuplicateKeyException("Duplicate entry '" + ADMIN_USERNAME + "' for key 'sys_user.uk_username'"));

        DataInitializer initializer = newInitializer(mapper);

        assertDoesNotThrow(() -> initializer.run(), "竞态兜底应在唯一键冲突后识别已存在的行而不是崩溃");
        verify(mapper, org.mockito.Mockito.times(2)).selectRawByUsername(ADMIN_USERNAME);
    }

    @Test
    void insertRaceRethrowsWhenTheRowIsStillMissing() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        // 两次查都是 null：冲突不是「同名行已存在」造成的，真实错误必须原样抛出，不得吞掉。
        when(mapper.selectRawByUsername(ADMIN_USERNAME)).thenReturn(null, null);
        when(mapper.insert(any(SysUser.class))).thenThrow(new DuplicateKeyException("real failure"));

        DataInitializer initializer = newInitializer(mapper);

        DuplicateKeyException thrown = assertThrows(DuplicateKeyException.class, () -> initializer.run());
        assertEquals("real failure", thrown.getMessage());
    }

    private DataInitializer newInitializer(SysUserMapper mapper) {
        DataInitializer initializer = new DataInitializer();
        ReflectionTestUtils.setField(initializer, "sysUserMapper", mapper);
        ReflectionTestUtils.setField(initializer, "passwordEncoder", new BCryptPasswordEncoder(4));
        ReflectionTestUtils.setField(initializer, "adminUsername", ADMIN_USERNAME);
        ReflectionTestUtils.setField(initializer, "adminPassword", ADMIN_PASSWORD);
        ReflectionTestUtils.setField(initializer, "adminNickname", "站长");
        return initializer;
    }

    // ---- 绕过 @TableLogic 的原始查询（JdbcTemplate），用于观察真实落库状态 ----

    private Long userId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, ADMIN_USERNAME);
    }

    private int isDeleted() {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM sys_user WHERE username = ?", Integer.class, ADMIN_USERNAME);
        return value == null ? 0 : value;
    }

    private String password() {
        return jdbcTemplate.queryForObject(
                "SELECT password FROM sys_user WHERE username = ?", String.class, ADMIN_USERNAME);
    }

    private int rowCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ?", Integer.class, ADMIN_USERNAME);
        return count == null ? 0 : count;
    }
}
