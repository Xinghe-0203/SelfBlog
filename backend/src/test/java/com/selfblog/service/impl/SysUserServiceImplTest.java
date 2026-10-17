package com.selfblog.service.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.auth.UserLoginRequest;
import com.selfblog.dto.auth.UserRegisterRequest;
import com.selfblog.vo.user.UserRegisterResponse;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.vo.user.UserLoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SysUserService 单元测试
 * 测试用户注册、登录、密码强度校验等核心功能
 */
@SpringBootTest
@Transactional(rollbackFor = Exception.class)
public class SysUserServiceImplTest {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtils jwtUtils;

    private static final String TEST_PASSWORD = "Test1234!";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        sysUserMapper.delete(null);
        // 重置Mock
        reset(jwtUtils);
        // 设置Mock JWT返回值
        when(jwtUtils.credentialVersionFor(any(SysUser.class))).thenReturn("mock.credential.version");
        when(jwtUtils.generateToken(anyLong(), anyString(), anyString(), anyString())).thenReturn("mock.jwt.token");
        when(jwtUtils.generateRefreshToken(anyLong(), anyString(), anyString(), anyString())).thenReturn("mock.refresh.token");
    }

    private SysUser createTestUser(String username, String role) {
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

    // ==================== 用户注册测试 ====================

    @Test
    @DisplayName("注册成功 - 正常注册流程")
    void testRegister_Success() {
        String username = "testuser_" + System.currentTimeMillis();
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(username);
        request.setPassword(TEST_PASSWORD);
        request.setNickname("测试用户");
        request.setEmail(TEST_EMAIL);

        UserRegisterResponse response = sysUserService.register(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(username, response.getUsername());

        // 验证用户已存入数据库
        SysUser savedUser = sysUserMapper.selectById(response.getId());
        assertNotNull(savedUser);
        assertEquals(username, savedUser.getUsername());
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, savedUser.getPassword()));
        assertEquals("user", savedUser.getRole());
        assertEquals(1, savedUser.getStatus());
    }

    @Test
    @DisplayName("注册失败 - 用户名重复（提前检查）")
    void testRegister_DuplicateUsername() {
        String username = "duplicate_" + System.currentTimeMillis();

        // 先注册一个用户
        UserRegisterRequest request1 = new UserRegisterRequest();
        request1.setUsername(username);
        request1.setPassword(TEST_PASSWORD);
        sysUserService.register(request1);

        // 尝试注册相同用户名
        UserRegisterRequest request2 = new UserRegisterRequest();
        request2.setUsername(username);
        request2.setPassword("NewPass123!");
        request2.setEmail("another@example.com");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.register(request2);
        });

        assertEquals(400, exception.getCode());
        assertEquals("注册失败，请稍后重试", exception.getMessage());
    }

    @Test
    @DisplayName("注册失败 - 邮箱重复（提前检查）")
    void testRegister_DuplicateEmail() {
        String username1 = "user1_" + System.currentTimeMillis();
        String username2 = "user2_" + System.currentTimeMillis();

        // 先注册一个用户
        UserRegisterRequest request1 = new UserRegisterRequest();
        request1.setUsername(username1);
        request1.setPassword(TEST_PASSWORD);
        request1.setEmail(TEST_EMAIL);
        sysUserService.register(request1);

        // 尝试使用相同邮箱注册另一个用户名
        UserRegisterRequest request2 = new UserRegisterRequest();
        request2.setUsername(username2);
        request2.setPassword(TEST_PASSWORD);
        request2.setEmail(TEST_EMAIL);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.register(request2);
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("邮箱已被注册"));
    }

    @Test
    @DisplayName("注册失败 - 密码强度不足（只有数字）")
    void testRegister_WeakPassword_DigitOnly() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newuser123");
        request.setPassword("12345678"); // 只有数字

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.register(request);
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("密码必须包含大小写字母、数字或特殊字符中的至少3种"));
    }

    @Test
    @DisplayName("注册失败 - 密码强度不足（只有小写字母）")
    void testRegister_WeakPassword_LowercaseOnly() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newuser123");
        request.setPassword("abcdefgh"); // 只有小写字母

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.register(request);
        });

        assertEquals(400, exception.getCode());
    }

    @Test
    @DisplayName("注册失败 - 密码强度不足（只有大写字母和数字）")
    void testRegister_WeakPassword_UppercaseAndDigitOnly() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newuser123");
        request.setPassword("ABCDEFGH1234"); // 只有大写字母和数字，缺特殊字符

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.register(request);
        });

        assertEquals(400, exception.getCode());
    }

    @Test
    @DisplayName("注册失败 - 密码长度不足（少于8位）")
    void testRegister_PasswordTooShort() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("newuser123");
        request.setPassword("Abc1!"); // 只有5位

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.register(request);
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("密码长度至少为8位"));
    }

    @Test
    @DisplayName("注册成功 - 密码包含所有类型字符")
    void testRegister_StrongPassword() {
        String username = "strong_" + System.currentTimeMillis();
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(username);
        request.setPassword("Abcdef123!@#"); // 包含大小写字母、数字、特殊字符

        assertDoesNotThrow(() -> {
            sysUserService.register(request);
        });
    }

    @Test
    @DisplayName("注册成功 - 未提供昵称时使用用户名作为昵称")
    void testRegister_NoNickname_UsesUsername() {
        String username = "nonick_" + System.currentTimeMillis();
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(username);
        request.setPassword(TEST_PASSWORD);
        // 不设置nickname

        UserRegisterResponse response = sysUserService.register(request);
        SysUser savedUser = sysUserMapper.selectById(response.getId());

        assertEquals(username, savedUser.getNickname());
    }

    // ==================== 用户登录测试 ====================

    @Test
    @DisplayName("登录成功 - 作者管理员登录")
    void testLogin_Success() {
        String username = "loginuser_" + System.currentTimeMillis();
        createTestUser(username, "admin");

        // 登录
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(TEST_PASSWORD);

        UserLoginResponse response = sysUserService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("mock.refresh.token", response.getRefreshToken());
        assertEquals(username, response.getUsername());

        // 验证JWT生成被调用
        verify(jwtUtils, times(1)).generateToken(anyLong(), eq(username), eq("admin"), eq("mock.credential.version"));
        verify(jwtUtils, times(1)).generateRefreshToken(anyLong(), eq(username), eq("admin"), eq("mock.credential.version"));
    }

    @Test
    @DisplayName("登录失败 - 普通用户不能登录作者后台")
    void testLogin_RegularUserDenied() {
        String username = "regularuser_" + System.currentTimeMillis();
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(TEST_PASSWORD);
        sysUserService.register(registerRequest);

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(TEST_PASSWORD);

        BusinessException exception = assertThrows(BusinessException.class, () -> sysUserService.login(loginRequest));

        assertEquals(403, exception.getCode());
        assertEquals("此博客仅开放作者后台登录", exception.getMessage());
        verify(jwtUtils, never()).generateToken(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void testLogin_UserNotFound() {
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("nonexistent_user_12345");
        loginRequest.setPassword(TEST_PASSWORD);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.login(loginRequest);
        });

        assertEquals(401, exception.getCode());
        assertEquals("用户名/邮箱或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_InvalidPassword() {
        String username = "pwduser_" + System.currentTimeMillis();

        // 先注册用户
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(TEST_PASSWORD);
        sysUserService.register(registerRequest);

        // 使用错误密码登录
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("WrongPassword123!");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.login(loginRequest);
        });

        assertEquals(401, exception.getCode());
        assertEquals("用户名/邮箱或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 账户已被禁用（status=0）")
    void testLogin_AccountBanned() {
        String username = "banneduser_" + System.currentTimeMillis();

        // 先注册用户
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(TEST_PASSWORD);
        UserRegisterResponse response = sysUserService.register(registerRequest);

        // 禁用账户
        SysUser user = sysUserMapper.selectById(response.getId());
        user.setStatus(0);
        sysUserMapper.updateById(user);

        // 尝试登录
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(TEST_PASSWORD);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.login(loginRequest);
        });

        assertEquals(403, exception.getCode());
        assertEquals("账号已被禁用", exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 账户被锁定（5次失败后）")
    void testLogin_AccountLocked() {
        String username = "lockuser_" + System.currentTimeMillis();

        // 先注册用户
        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(TEST_PASSWORD);
        sysUserService.register(registerRequest);

        // 使用错误密码连续登录5次
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("WrongPassword123!");

        // 前4次应该返回用户名/邮箱或密码错误
        for (int i = 0; i < 4; i++) {
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                sysUserService.login(loginRequest);
            });
            assertEquals(401, exception.getCode());
            assertEquals("用户名/邮箱或密码错误", exception.getMessage());
        }

        // 第5次失败后账户应被锁定
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.login(loginRequest);
        });

        // 账户被锁定后，登录应返回403
        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("登录失败次数过多"));
    }

    @Test
    @DisplayName("登录成功 - 失败计数重置")
    void testLogin_LoginFailCountReset() {
        String username = "resetuser_" + System.currentTimeMillis();
        SysUser user = createTestUser(username, "admin");

        // 连续失败3次
        UserLoginRequest failRequest = new UserLoginRequest();
        failRequest.setUsername(username);
        failRequest.setPassword("WrongPassword123!");

        for (int i = 0; i < 3; i++) {
            try {
                sysUserService.login(failRequest);
            } catch (BusinessException e) {
                // 忽略
            }
        }

        // 正确密码登录成功
        UserLoginRequest successRequest = new UserLoginRequest();
        successRequest.setUsername(username);
        successRequest.setPassword(TEST_PASSWORD);

        UserLoginResponse response = sysUserService.login(successRequest);
        assertNotNull(response);
        assertEquals(username, response.getUsername());
        assertEquals(0, sysUserMapper.selectById(user.getId()).getLoginFailCount());
    }

    @Test
    @DisplayName("登录失败 - 用户名为空")
    void testLogin_EmptyUsername() {
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUsername("");
        loginRequest.setPassword(TEST_PASSWORD);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.login(loginRequest);
        });

        assertEquals(400, exception.getCode());
        assertEquals("用户名或邮箱不能为空", exception.getMessage());
    }

    // ==================== 修改密码测试 ====================

    @Test
    @DisplayName("修改密码成功")
    void testChangePassword_Success() {
        String username = "pwduser_" + System.currentTimeMillis();
        SysUser user = createTestUser(username, "admin");

        // 修改密码
        String newPassword = "NewPass456!";
        sysUserService.changePassword(user.getId(), TEST_PASSWORD, newPassword);

        SysUser updatedUser = sysUserMapper.selectById(user.getId());
        assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()));
        verify(jwtUtils).revokeAllUserTokens(user.getId());
    }

    @Test
    @DisplayName("修改密码失败 - 旧密码错误")
    void testChangePassword_WrongOldPassword() {
        String username = "pwduser2_" + System.currentTimeMillis();

        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(TEST_PASSWORD);
        UserRegisterResponse registerResponse = sysUserService.register(registerRequest);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.changePassword(registerResponse.getId(), "WrongOldPass1!", "NewPass123!");
        });

        assertEquals(400, exception.getCode());
        assertEquals("旧密码不正确", exception.getMessage());
    }

    @Test
    @DisplayName("修改密码失败 - 新密码强度不足")
    void testChangePassword_WeakNewPassword() {
        String username = "pwduser3_" + System.currentTimeMillis();

        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(TEST_PASSWORD);
        UserRegisterResponse registerResponse = sysUserService.register(registerRequest);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.changePassword(registerResponse.getId(), TEST_PASSWORD, "newpass123");
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("密码必须包含大小写字母、数字或特殊字符中的至少3种"));
    }

    @Test
    @DisplayName("修改密码失败 - 新密码长度不足")
    void testChangePassword_NewPasswordTooShort() {
        String username = "pwduser4_" + System.currentTimeMillis();

        UserRegisterRequest registerRequest = new UserRegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(TEST_PASSWORD);
        UserRegisterResponse registerResponse = sysUserService.register(registerRequest);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            sysUserService.changePassword(registerResponse.getId(), TEST_PASSWORD, "Abc1!");
        });

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("新密码长度至少为8位"));
    }
}
