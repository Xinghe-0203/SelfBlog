package com.selfblog.utils;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.controller.user.SysUserController;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.service.auth.impl.SysUserServiceImpl;
import com.selfblog.dto.auth.UserLoginRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtCredentialVersionTest {
    private static final String SECRET = "test-secret-at-least-32-bytes-for-credential-versions";
    private final AtomicReference<String> password = new AtomicReference<>();
    private SysUserMapper mapper;
    private JwtUtils jwt;

    @BeforeEach
    void setUp() {
        password.set("stored-old-bcrypt-hash");
        mapper = mock(SysUserMapper.class);
        when(mapper.selectById(1L)).thenAnswer(invocation -> user(password.get()));
        jwt = newInstance();
    }

    @Test
    void changedPasswordRevokesBothTokenTypesWithoutDeviceRegistrationOrBlacklist() {
        String access = jwt.generateToken(1L, "author", "admin");
        String refresh = jwt.generateRefreshToken(1L, "author", "admin");
        assertFalse(jwt.isTokenRevoked(access));
        assertFalse(jwt.isTokenRevoked(refresh));
        password.set("stored-new-bcrypt-hash");
        assertTrue(jwt.isTokenRevoked(access));
        assertTrue(jwt.isTokenRevoked(refresh));
        assertThrows(BusinessException.class, () -> jwt.rotateRefreshToken(refresh));
        assertFalse(jwt.isTokenRevoked(jwt.generateToken(1L, "author", "admin")));
    }

    @Test
    void freshProcessWithSameDatabaseAndSecretStillRejectsOldTokens() {
        String access = jwt.generateToken(1L, "author", "admin");
        String refresh = jwt.generateRefreshToken(1L, "author", "admin");
        JwtUtils otherInstanceBeforeChange = newInstance();
        assertFalse(otherInstanceBeforeChange.isTokenRevoked(access));
        password.set("stored-new-bcrypt-hash");
        JwtUtils restarted = newInstance();
        assertTrue(otherInstanceBeforeChange.isTokenRevoked(refresh));
        assertTrue(restarted.isTokenRevoked(access));
        assertTrue(restarted.isTokenRevoked(refresh));
        assertFalse(restarted.isTokenRevoked(restarted.generateRefreshToken(1L, "author", "admin")));
    }

    @Test
    void legacyAndMalformedVersionsCannotAuthenticateOrRotate() {
        String legacy = rawRefresh(null);
        String malformed = rawRefresh(42);
        String empty = rawRefresh("");
        for (String token : new String[]{legacy, malformed, empty, "not-a-token"}) {
            assertTrue(jwt.isTokenRevoked(token));
            assertThrows(BusinessException.class, () -> jwt.rotateRefreshToken(token));
        }
        assertTrue(jwt.isTokenRevoked(null));
    }

    @Test
    void deletedDisabledOrMissingCredentialsFailClosed() {
        String token = jwt.generateToken(1L, "author", "admin");
        when(mapper.selectById(1L)).thenReturn(null);
        assertTrue(jwt.isTokenRevoked(token));
        when(mapper.selectById(1L)).thenReturn(user(null));
        assertTrue(jwt.isTokenRevoked(token));
        SysUser disabled = user(password.get());
        disabled.setStatus(0);
        when(mapper.selectById(1L)).thenReturn(disabled);
        assertTrue(jwt.isTokenRevoked(token));
        SysUser deleted = user(password.get());
        deleted.setIsDeleted(1);
        when(mapper.selectById(1L)).thenReturn(deleted);
        assertTrue(jwt.isTokenRevoked(token));
    }

    @Test
    void databaseFailureCannotAuthorizeOrIssueReplacement() {
        String token = jwt.generateRefreshToken(1L, "author", "admin");
        when(mapper.selectById(1L)).thenThrow(new DataAccessResourceFailureException("test database unavailable"));
        assertThrows(DataAccessResourceFailureException.class, () -> jwt.isTokenRevoked(token));
        assertThrows(BusinessException.class, () -> jwt.rotateRefreshToken(token));
        assertThrows(DataAccessResourceFailureException.class, () -> jwt.generateToken(1L, "author", "admin"));
    }

    @Test
    void versionDoesNotExposeBcryptHashAndSeparatesUsers() {
        String version = jwt.credentialVersionFor(user(password.get()));
        assertEquals(43, version.length());
        assertFalse(version.contains(password.get()));
        SysUser other = user(password.get());
        other.setId(2L);
        assertNotEquals(version, jwt.credentialVersionFor(other));
        assertFalse(jwt.parseToken(jwt.generateToken(1L, "author", "admin")).toString().contains(password.get()));
    }

    @Test
    void rotationRejectsReplayAndKeepsOriginalVersion() {
        String old = jwt.generateRefreshToken(1L, "author", "admin");
        String next = jwt.rotateRefreshToken(old);
        assertEquals(jwt.getCredentialVersionFromToken(old), jwt.getCredentialVersionFromToken(next));
        assertThrows(BusinessException.class, () -> jwt.rotateRefreshToken(old));
    }

    @Test
    void resetBetweenRefreshValidationAndIssuanceCannotUpgradeCredentialVersion() {
        JwtUtils racing = spy(jwt);
        String old = racing.generateRefreshToken(1L, "author", "admin");
        doAnswer(invocation -> {
            invocation.callRealMethod();
            password.set("concurrently-reset-bcrypt-hash");
            return null;
        }).when(racing).revokeToken(old);
        String next = racing.rotateRefreshToken(old);
        assertEquals(racing.getCredentialVersionFromToken(old), racing.getCredentialVersionFromToken(next));
        assertTrue(racing.isTokenRevoked(next));
    }

    @Test
    void controllerRefreshKeepsOldVersionIfPasswordChangesAfterValidation() {
        JwtUtils racing = spy(jwt);
        String old = racing.generateRefreshToken(1L, "author", "admin");
        doAnswer(invocation -> {
            invocation.callRealMethod();
            password.set("concurrently-reset-bcrypt-hash");
            return null;
        }).when(racing).revokeToken(old);
        SysUserService service = mock(SysUserService.class);
        when(service.getById(1L)).thenReturn(user(password.get()));
        SysUserController controller = new SysUserController(service, racing);
        var response = controller.refreshToken("Bearer " + old).getData();
        assertTrue(racing.isTokenRevoked(response.getToken()));
        assertTrue(racing.isTokenRevoked(response.getRefreshToken()));
    }

    @Test
    void loginSignsThePasswordSnapshotActuallyVerifiedNotANewerPassword() {
        SysUser snapshot = user(password.get());
        when(mapper.selectOne(any())).thenReturn(snapshot);
        when(mapper.selectOne(any(), anyBoolean())).thenReturn(snapshot);
        BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);
        when(encoder.matches("OldPassword123!", snapshot.getPassword())).thenAnswer(invocation -> {
            password.set("concurrently-reset-bcrypt-hash");
            return true;
        });
        SysUserServiceImpl service = new SysUserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "jwtUtils", jwt);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("author");
        request.setPassword("OldPassword123!");
        var response = service.login(request);
        assertTrue(jwt.isTokenRevoked(response.getToken()));
        assertTrue(jwt.isTokenRevoked(response.getRefreshToken()));
    }

    private JwtUtils newInstance() {
        JwtUtils instance = new JwtUtils();
        ReflectionTestUtils.setField(instance, "secret", SECRET);
        ReflectionTestUtils.setField(instance, "expiration", 3_600_000L);
        ReflectionTestUtils.setField(instance, "refreshExpiration", 604_800_000L);
        ReflectionTestUtils.setField(instance, "sysUserMapper", mapper);
        return instance;
    }

    private SysUser user(String hash) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("author");
        user.setPassword(hash);
        user.setRole("admin");
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setLoginFailCount(0);
        return user;
    }

    private String rawRefresh(Object version) {
        var builder = Jwts.builder().subject("author").claim("userId", 1L).claim("type", "refresh")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000));
        if (version != null) builder.claim("credentialVersion", version);
        return builder.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
