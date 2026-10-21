package com.selfblog.service.auth;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.auth.UserLoginRequest;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
 * S4 回归测试：密码重置成功后必须解除登录锁定，且自助重置要拒绝不可用账号。
 *
 * <p>修复前的行为：两条重置路径（自助 {@code resetPassword(String, String)} 与
 * 管理员 {@code resetPassword(Long)}）都只做 CAS 改密 + 撤销令牌，从不碰
 * {@code login_fail_count} / {@code lock_until}。于是一个因为连续输错密码被锁的账号，
 * 即使成功重置了密码，在锁定窗口结束前仍然登不进去。</p>
 *
 * <p>同时，自助路径此前既不校验账号状态也不校验锁定状态：被禁用的账号也能改密码，
 * 而仍在锁定期内的账号也能用邮箱验证码绕过锁定。现在两者都以同一句中性文案拒绝
 * （不区分具体原因，避免泄露账号状态）。</p>
 */
@SpringBootTest
@Transactional(rollbackFor = Exception.class)
class PasswordResetLockoutTest {

    private static final String USERNAME = "lockout_probe_author";
    private static final String EMAIL = "lockout_probe_author@example.com";
    private static final String OLD_PASSWORD = "OldPassword123!";
    private static final String NEW_PASSWORD = "NewPassword456!";

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtils jwtUtils;

    private Long userId;

    @BeforeEach
    void setUp() {
        sysUserMapper.delete(null);
        Mockito.reset(jwtUtils);
        when(jwtUtils.credentialVersionFor(any(SysUser.class))).thenReturn("mock.credential.version");
        when(jwtUtils.generateToken(anyLong(), anyString(), anyString(), anyString())).thenReturn("mock.jwt.token");
        when(jwtUtils.generateRefreshToken(anyLong(), anyString(), anyString(), anyString())).thenReturn("mock.refresh.token");

        SysUser user = new SysUser();
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);
        user.setPassword(passwordEncoder.encode(OLD_PASSWORD));
        user.setNickname(USERNAME);
        user.setRole("admin");
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setIsDeleted(0);
        sysUserMapper.insert(user);
        userId = user.getId();

        // 管理员重置路径会读取当前登录用户用于审计日志，因此需要放一个 admin 上下文。
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserContext(userId, "admin"), null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /** 模拟「连续失败已达上限」的锁定状态。 */
    private void lockAccount() {
        SysUser user = sysUserMapper.selectById(userId);
        user.setLoginFailCount(5);
        user.setLockUntil(LocalDateTime.now().plusMinutes(15));
        sysUserMapper.updateById(user);
    }

    private void loginWith(String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(password);
        sysUserService.login(request);
    }

    @Test
    @DisplayName("S4：管理员重置密码后锁定被解除，新密码可正常登录")
    void adminResetClearsLockoutAndNewPasswordWorks() {
        lockAccount();

        String issuedPassword = sysUserService.resetPassword(userId);
        assertNotNull(issuedPassword, "管理员重置应返回一次性随机密码");

        SysUser afterReset = sysUserMapper.selectById(userId);
        assertEquals(0, afterReset.getLoginFailCount(), "重置后失败计数必须清零");
        assertNull(afterReset.getLockUntil(), "重置后锁定时间必须清空");

        // 关键断言：锁定窗口内用新密码登录必须成功，而不是继续被 403 拦下。
        loginWith(issuedPassword);
    }

    @Test
    @DisplayName("S4：自助重置在旧锁定已过期但计数仍在时成功，并清掉残留计数")
    void selfServiceResetClearsStaleFailCountAfterLockExpiry() {
        // 锁定时间已过（账号可用），但 login_fail_count 仍是 5：修复前成功重置也不会清零，
        // 于是下一次输错一次就会立刻再次锁定。
        SysUser user = sysUserMapper.selectById(userId);
        user.setLoginFailCount(5);
        user.setLockUntil(LocalDateTime.now().minusMinutes(1));
        sysUserMapper.updateById(user);

        sysUserService.resetPassword(EMAIL, NEW_PASSWORD);

        SysUser afterReset = sysUserMapper.selectById(userId);
        assertEquals(0, afterReset.getLoginFailCount(), "自助重置后失败计数必须清零");
        assertNull(afterReset.getLockUntil(), "自助重置后锁定时间必须清空");

        loginWith(NEW_PASSWORD);
    }

    @Test
    @DisplayName("S4：自助重置拒绝已禁用账号，返回中性文案")
    void selfServiceResetRefusesDisabledAccount() {
        SysUser user = sysUserMapper.selectById(userId);
        user.setStatus(0);
        sysUserMapper.updateById(user);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.resetPassword(EMAIL, NEW_PASSWORD));

        assertEquals(403, exception.getCode());
        assertEquals("该账号当前不可用，请联系管理员", exception.getMessage());
        // 被拒后原密码必须保持不变。
        assertTrue(passwordEncoder.matches(OLD_PASSWORD, sysUserMapper.selectById(userId).getPassword()));
    }

    @Test
    @DisplayName("S4：自助重置拒绝仍在锁定窗口内的账号，返回与禁用同一句文案")
    void selfServiceResetRefusesCurrentlyLockedAccountWithSameMessage() {
        lockAccount();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sysUserService.resetPassword(EMAIL, NEW_PASSWORD));

        assertEquals(403, exception.getCode());
        // 与禁用分支共用同一句文案：调用方无法据此判断账号究竟处于哪种状态。
        assertEquals("该账号当前不可用，请联系管理员", exception.getMessage());
    }
}
