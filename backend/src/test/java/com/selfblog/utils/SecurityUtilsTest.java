package com.selfblog.utils;

import com.selfblog.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityUtils 单元测试")
class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull - UserContext认证时返回用户ID")
    void getCurrentUserIdOrNull_shouldReturnUserId_whenUserContextPrincipal() {
        UserContext userContext = new UserContext(42L, "user");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userContext, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull - Long认证时返回用户ID（向后兼容）")
    void getCurrentUserIdOrNull_shouldReturnUserId_whenLongPrincipal() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                42L, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull - 未认证时返回null")
    void getCurrentUserIdOrNull_shouldReturnNull_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        assertNull(userId);
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull - 非Long非UserContext类型返回null")
    void getCurrentUserIdOrNull_shouldReturnNull_whenPrincipalNotRecognized() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "notALong", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        assertNull(userId);
    }

    @Test
    @DisplayName("getCurrentUserId - 未认证时抛出BusinessException")
    void getCurrentUserId_shouldThrow_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThrows(BusinessException.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    @DisplayName("getCurrentUserId - 认证时返回用户ID")
    void getCurrentUserId_shouldReturnUserId_whenAuthenticated() {
        UserContext userContext = new UserContext(100L, "admin");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userContext, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = SecurityUtils.getCurrentUserId();
        assertEquals(100L, userId);
    }

    @Test
    @DisplayName("getCurrentUserContext - 未认证时抛出BusinessException")
    void getCurrentUserContext_shouldThrow_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThrows(BusinessException.class, SecurityUtils::getCurrentUserContext);
    }

    @Test
    @DisplayName("getCurrentUserContext - 认证时返回UserContext")
    void getCurrentUserContext_shouldReturnUserContext_whenAuthenticated() {
        UserContext userContext = new UserContext(10L, "user");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userContext, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserContext result = SecurityUtils.getCurrentUserContext();
        assertNotNull(result);
        assertEquals(10L, result.getUserId());
        assertEquals("user", result.getRole());
    }

    @Test
    @DisplayName("getCurrentUserContextOrNull - 未认证时返回null")
    void getCurrentUserContextOrNull_shouldReturnNull_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        UserContext result = SecurityUtils.getCurrentUserContextOrNull();
        assertNull(result);
    }

    @Test
    @DisplayName("getCurrentUserContextOrNull - 认证时返回UserContext")
    void getCurrentUserContextOrNull_shouldReturnUserContext_whenAuthenticated() {
        UserContext userContext = new UserContext(5L, "user");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userContext, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserContext result = SecurityUtils.getCurrentUserContextOrNull();
        assertNotNull(result);
        assertEquals(5L, result.getUserId());
    }

    @Test
    @DisplayName("isCurrentUserAdmin - 管理员角色返回true")
    void isCurrentUserAdmin_shouldReturnTrue_whenAdmin() {
        UserContext adminContext = new UserContext(1L, "admin");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                adminContext, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(SecurityUtils.isCurrentUserAdmin());
    }

    @Test
    @DisplayName("isCurrentUserAdmin - 普通用户返回false")
    void isCurrentUserAdmin_shouldReturnFalse_whenNormalUser() {
        UserContext userContext = new UserContext(2L, "user");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userContext, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertFalse(SecurityUtils.isCurrentUserAdmin());
    }

    @Test
    @DisplayName("isCurrentUserAdmin - 未认证返回false")
    void isCurrentUserAdmin_shouldReturnFalse_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertFalse(SecurityUtils.isCurrentUserAdmin());
    }
}
