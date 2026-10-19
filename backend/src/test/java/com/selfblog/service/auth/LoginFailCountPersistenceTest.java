package com.selfblog.service.auth;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.auth.UserLoginRequest;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.selfblog.utils.JwtUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * F8 回归测试：登录失败计数与锁定状态必须真正提交到数据库。
 *
 * <p>修复前 {@code login} 带 {@code @Transactional(rollbackFor = Exception.class)}，
 * 而失败分支在自增计数后立刻 {@code throw new BusinessException(...)} —— RuntimeException
 * 触发回滚，因此 <b>失败计数与锁定时间在生产环境从未落库</b>，登录失败次数限制实际失效。
 * 既有测试全部通过只是因为 {@code SysUserServiceImplTest} 自带 {@code @Transactional}，
 * 计数留在未提交事务里仍可被同一事务读到。</p>
 *
 * <p>本测试刻意 <b>不加</b> {@code @Transactional}，并用独立库名隔离，因此断言读到的是
 * 真实提交后的数据：修复前这些断言会失败。</p>
 *
 * <p>同时锁定 F8 的强度约束：Spring 容器里的 {@link BCryptPasswordEncoder} 仍是
 * 默认 cost=12（未被调低），并记录一次真实耗时作为证据。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:login-failcount-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class LoginFailCountPersistenceTest {

    private static final String TEST_PASSWORD = "Test1234!";
    private static final String WRONG_PASSWORD = "WrongPassword123!";

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        sysUserMapper.delete(null);
        when(jwtUtils.credentialVersionFor(any(SysUser.class))).thenReturn("mock.credential.version");
        when(jwtUtils.generateToken(anyLong(), anyString(), anyString(), anyString())).thenReturn("mock.jwt.token");
        when(jwtUtils.generateRefreshToken(anyLong(), anyString(), anyString(), anyString())).thenReturn("mock.refresh.token");
    }

    private SysUser insertUser(String username, String role) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setNickname(username);
        user.setRole(role);
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setIsDeleted(0);
        sysUserMapper.insert(user);
        return user;
    }

    private UserLoginRequest request(String username, String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private void assertWrongPasswordLogin(String username) {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.login(request(username, WRONG_PASSWORD)));
        assertEquals(401, exception.getCode());
        assertEquals("用户名/邮箱或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("失败计数提交到数据库：3 次密码错误后 login_fail_count 真实为 3")
    void failedLoginCountIsCommittedToDatabase() {
        SysUser user = insertUser("committed_failcount_author", "admin");

        for (int i = 0; i < 3; i++) {
            assertWrongPasswordLogin(user.getUsername());
        }

        SysUser reloaded = sysUserMapper.selectById(user.getId());
        assertNotNull(reloaded);
        assertEquals(3, reloaded.getLoginFailCount(),
                "登录失败计数必须在每次失败后提交（修复前被 login 的事务回滚，恒为 0）");
        assertNull(reloaded.getLockUntil(), "未达到上限时不应锁定");
    }

    @Test
    @DisplayName("锁定状态提交到数据库：第 5 次失败写入 lock_until，随后正确密码也被拒")
    void lockStateIsCommittedAndEnforcedBeforePasswordCheck() {
        SysUser user = insertUser("committed_lock_author", "admin");

        // 前 4 次失败：401，且不应锁定
        for (int i = 0; i < 4; i++) {
            assertWrongPasswordLogin(user.getUsername());
        }
        assertEquals(4, sysUserMapper.selectById(user.getId()).getLoginFailCount());
        assertNull(sysUserMapper.selectById(user.getId()).getLockUntil(), "第 4 次失败后仍不应锁定");

        // 第 5 次失败：达到 maxFailCount，返回 403 并写入 lock_until
        BusinessException fifth = assertThrows(BusinessException.class,
                () -> sysUserService.login(request(user.getUsername(), WRONG_PASSWORD)));
        assertEquals(403, fifth.getCode());
        assertTrue(fifth.getMessage().contains("登录失败次数过多"), "实际消息: " + fifth.getMessage());

        SysUser afterFifth = sysUserMapper.selectById(user.getId());
        assertEquals(5, afterFifth.getLoginFailCount(), "第 5 次失败的计数必须提交");
        assertNotNull(afterFifth.getLockUntil(), "第 5 次失败必须提交锁定时间（修复前被回滚，永不锁定）");

        // 锁定判定先于密码校验：即使密码正确也应 403，而不是 401/200
        BusinessException locked = assertThrows(BusinessException.class,
                () -> sysUserService.login(request(user.getUsername(), TEST_PASSWORD)));
        assertEquals(403, locked.getCode());
        assertTrue(locked.getMessage().contains("登录失败次数过多"),
                "锁定判定必须发生在 BCrypt 之前，实际消息: " + locked.getMessage());
    }

    @Test
    @DisplayName("成功登录重置计数并提交：login_fail_count 归零、lock_until 清空")
    void successfulLoginResetsCountAndCommits() {
        SysUser user = insertUser("committed_reset_author", "admin");

        assertWrongPasswordLogin(user.getUsername());
        assertWrongPasswordLogin(user.getUsername());
        assertEquals(2, sysUserMapper.selectById(user.getId()).getLoginFailCount());

        var response = sysUserService.login(request(user.getUsername(), TEST_PASSWORD));
        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());

        SysUser reloaded = sysUserMapper.selectById(user.getId());
        assertEquals(0, reloaded.getLoginFailCount(), "成功登录后计数必须提交为 0");
        assertNull(reloaded.getLockUntil(), "成功登录后锁定时间必须提交为 null");
    }

    @Test
    @DisplayName("密码强度未调低：生效的 BCrypt cost 仍为 12，并记录真实耗时")
    void bcryptStrengthRemainsTwelve() {
        long encodeStart = System.nanoTime();
        String hash = passwordEncoder.encode(TEST_PASSWORD);
        long encodeMs = (System.nanoTime() - encodeStart) / 1_000_000;

        long matchesStart = System.nanoTime();
        boolean matched = passwordEncoder.matches(TEST_PASSWORD, hash);
        long matchesMs = (System.nanoTime() - matchesStart) / 1_000_000;

        System.out.println("[F8-EVIDENCE] 生效 BCrypt cost 前缀=" + hash.substring(0, 7)
                + " encode=" + encodeMs + "ms matches=" + matchesMs + "ms");

        assertTrue(matched, "同一密码应校验通过");
        assertTrue(hash.startsWith("$2a$12$"),
                "BCrypt cost 不得低于 12，实际哈希前缀: " + hash.substring(0, 7));
    }
}
