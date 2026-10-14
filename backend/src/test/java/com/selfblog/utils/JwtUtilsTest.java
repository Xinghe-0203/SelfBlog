package com.selfblog.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtils 单元测试")
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_ROLE = "user";
    private static final String TEST_SECRET = "this_is_a_very_long_secret_key_for_testing_at_least_32_chars";
    private static final Long TEST_EXPIRATION = 3600000L;
    private static final Long TEST_REFRESH_EXPIRATION = 604800000L;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expiration", TEST_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtils, "refreshExpiration", TEST_REFRESH_EXPIRATION);
        Map<String, Long> blacklist = (Map<String, Long>) ReflectionTestUtils.getField(jwtUtils, "tokenBlacklist");
        if (blacklist != null) blacklist.clear();
        Map<Long, Set<String>> devices = (Map<Long, Set<String>>) ReflectionTestUtils.getField(jwtUtils, "userDeviceTokens");
        if (devices != null) devices.clear();
    }

    @Test
    @DisplayName("generateToken - 返回非空JWT字符串")
    void generateToken_shouldReturnNonEmptyJwt() {
        String token = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("generateRefreshToken - 返回非空JWT字符串")
    void generateRefreshToken_shouldReturnNonEmptyJwt() {
        String refreshToken = jwtUtils.generateRefreshToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertEquals(3, refreshToken.split("\\.").length);
    }

    @Test
    @DisplayName("generateToken - 不同用户生成不同Token")
    void generateToken_shouldGenerateDifferentTokensForDifferentUsers() {
        String token1 = jwtUtils.generateToken(1L, "user1", "user");
        String token2 = jwtUtils.generateToken(2L, "user2", "user");
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("getUserIdFromToken - 返回正确用户ID")
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        String token = jwtUtils.generateToken(42L, TEST_USERNAME, TEST_ROLE);
        Long userId = jwtUtils.getUserIdFromToken(token);
        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("getUsernameFromToken - 返回正确用户名")
    void getUsernameFromToken_shouldReturnCorrectUsername() {
        String token = jwtUtils.generateToken(TEST_USER_ID, "testuser", TEST_ROLE);
        String username = jwtUtils.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("getRoleFromToken - 返回正确角色")
    void getRoleFromToken_shouldReturnCorrectRole() {
        String token = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, "admin");
        String role = jwtUtils.getRoleFromToken(token);
        assertEquals("admin", role);
    }

    @Test
    @DisplayName("parseToken - 有效Token返回Claims")
    void parseToken_shouldReturnClaimsForValidToken() {
        String token = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        Claims claims = jwtUtils.parseToken(token);
        assertNotNull(claims);
        assertEquals(TEST_USER_ID, claims.get("userId", Long.class));
    }

    @Test
    @DisplayName("parseToken - 无效Token抛出JwtException")
    void parseToken_shouldThrowForInvalidToken() {
        assertThrows(JwtException.class, () -> jwtUtils.parseToken("invalid.token.here"));
    }

    @Test
    @DisplayName("parseToken - null Token抛出异常")
    void parseToken_shouldThrowForNullToken() {
        assertThrows(Exception.class, () -> jwtUtils.parseToken(null));
    }

    @Test
    @DisplayName("isTokenExpired - 未过期Token返回false")
    void isTokenExpired_shouldReturnFalseForValidToken() {
        String token = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        assertFalse(jwtUtils.isTokenExpired(token));
    }

    @Test
    @DisplayName("isTokenExpired - 过期Token返回true")
    void isTokenExpired_shouldReturnTrueForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtils, "expiration", 1L);
        String token = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertTrue(jwtUtils.isTokenExpired(token));
    }

    @Test
    @DisplayName("isTokenExpired - 无效Token不视为过期")
    void isTokenExpired_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtils.isTokenExpired("invalid.token.here"));
    }

    @Test
    @DisplayName("isTokenRevoked - 新Token未被撤销")
    void isTokenRevoked_shouldReturnFalseForNewToken() {
        String token = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        assertFalse(jwtUtils.isTokenRevoked(token));
    }

    @Test
    @DisplayName("isTokenRevoked - 撤销后返回true")
    void isTokenRevoked_shouldReturnTrueAfterRevoke() {
        String token = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        jwtUtils.revokeToken(token);
        assertTrue(jwtUtils.isTokenRevoked(token));
    }

    @Test
    @DisplayName("revokeToken - null Token不抛异常")
    void revokeToken_shouldNotThrowForNull() {
        assertDoesNotThrow(() -> jwtUtils.revokeToken(null));
    }

    @Test
    @DisplayName("revokeToken - 过期Token不加入黑名单")
    void revokeToken_shouldNotBlacklistExpiredToken() {
        ReflectionTestUtils.setField(jwtUtils, "expiration", 1L);
        String token = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        jwtUtils.revokeToken(token);
        assertTrue(jwtUtils.isTokenExpired(token));
    }

    @Test
    @DisplayName("isRefreshToken - 刷新Token返回true")
    void isRefreshToken_shouldReturnTrueForRefreshToken() {
        String refreshToken = jwtUtils.generateRefreshToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        assertTrue(jwtUtils.isRefreshToken(refreshToken));
    }

    @Test
    @DisplayName("isRefreshToken - 访问Token返回false")
    void isRefreshToken_shouldReturnFalseForAccessToken() {
        String accessToken = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        assertFalse(jwtUtils.isRefreshToken(accessToken));
    }

    @Test
    @DisplayName("isRefreshToken - 无效Token返回false")
    void isRefreshToken_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtils.isRefreshToken("invalid.token.here"));
    }

    @Test
    @DisplayName("cleanExpiredTokens - 清理后有效Token仍在黑名单")
    void cleanExpiredTokens_shouldKeepValidTokens() {
        ReflectionTestUtils.setField(jwtUtils, "expiration", 1L);
        String expiredToken = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        ReflectionTestUtils.setField(jwtUtils, "expiration", TEST_EXPIRATION);
        String validToken = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        jwtUtils.revokeToken(expiredToken);
        jwtUtils.revokeToken(validToken);
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        jwtUtils.cleanExpiredTokens();
        assertTrue(jwtUtils.isTokenRevoked(validToken));
    }

    @Test
    @DisplayName("rotateRefreshToken - null Token抛出异常")
    void rotateRefreshToken_shouldThrowForNull() {
        assertThrows(Exception.class, () -> jwtUtils.rotateRefreshToken(null));
    }

    @Test
    @DisplayName("rotateRefreshToken - 非刷新Token抛出异常")
    void rotateRefreshToken_shouldThrowForNonRefreshToken() {
        String accessToken = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        assertThrows(Exception.class, () -> jwtUtils.rotateRefreshToken(accessToken));
    }

    @Test
    @DisplayName("rotateRefreshToken - 返回新刷新Token")
    void rotateRefreshToken_shouldReturnNewRefreshToken() {
        String oldRefreshToken = jwtUtils.generateRefreshToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        String newRefreshToken = jwtUtils.rotateRefreshToken(oldRefreshToken);
        assertNotNull(newRefreshToken);
        assertNotEquals(oldRefreshToken, newRefreshToken);
        assertTrue(jwtUtils.isRefreshToken(newRefreshToken));
    }

    @Test
    @DisplayName("revokeAllUserTokens - 撤销后所有设备Token被清除")
    void revokeAllUserTokens_shouldClearAllDevices() {
        String token1 = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        String token2 = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        jwtUtils.registerDeviceToken(TEST_USER_ID, token1);
        jwtUtils.registerDeviceToken(TEST_USER_ID, token2);
        jwtUtils.revokeAllUserTokens(TEST_USER_ID);
        assertEquals(0, jwtUtils.getUserDeviceCount(TEST_USER_ID));
    }

    @Test
    @DisplayName("logoutOtherDevices - 保留排除Token")
    void logoutOtherDevices_shouldKeepExcludeToken() {
        String token1 = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        String token2 = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        jwtUtils.registerDeviceToken(TEST_USER_ID, token1);
        jwtUtils.registerDeviceToken(TEST_USER_ID, token2);
        jwtUtils.logoutOtherDevices(TEST_USER_ID, token1);
        assertEquals(1, jwtUtils.getUserDeviceCount(TEST_USER_ID));
    }

    @Test
    @DisplayName("extractTokenFromRequest - 提取Bearer Token")
    void extractTokenFromRequest_shouldExtractBearerToken() {
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer my.jwt.token");
        String extracted = jwtUtils.extractTokenFromRequest(request);
        assertEquals("my.jwt.token", extracted);
    }

    @Test
    @DisplayName("extractTokenFromRequest - 无Authorization头返回null")
    void extractTokenFromRequest_shouldReturnNullWhenNoHeader() {
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        assertNull(jwtUtils.extractTokenFromRequest(request));
    }

    @Test
    @DisplayName("getUserIdFromToken - 过期Token抛出异常")
    void getUserIdFromToken_shouldThrowForExpiredToken() {
        ReflectionTestUtils.setField(jwtUtils, "expiration", 1L);
        String token = jwtUtils.generateToken(TEST_USER_ID, TEST_USERNAME, TEST_ROLE);
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertThrows(ExpiredJwtException.class, () -> jwtUtils.getUserIdFromToken(token));
    }
}
