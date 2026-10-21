package com.selfblog.controller.user;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * S13 回归：头像/封面图 URL 校验不得被协议相对 URL 绕过。
 *
 * <p>修复前 {@code validateImageUrl} 里 {@code if (url.startsWith("/")) return;} 直接放行，
 * 于是 {@code //evil.example/x.png} 这种**协议相对 URL**（浏览器会按当前页面的协议解析成
 * {@code https://evil.example/x.png}）完全不经过后面的 http(s) 前缀校验与域名白名单，
 * 等于把域名白名单整段绕过；{@code /\evil.example/x} 在部分解析器里同样指向外部主机。</p>
 */
@DisplayName("S13：头像/封面图 URL 校验")
class SysUserControllerImageUrlTest {

    private static final Long USER_ID = 42L;

    private SysUserController controller;
    private SysUserService sysUserService;

    @BeforeEach
    void setUp() {
        sysUserService = Mockito.mock(SysUserService.class);
        JwtUtils jwtUtils = Mockito.mock(JwtUtils.class);
        controller = new SysUserController(sysUserService, jwtUtils);
        ReflectionTestUtils.setField(controller, "allowedAvatarDomainsConfig", "localhost,127.0.0.1,cdn.example.com");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserContext(USER_ID, "admin"), null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "拒绝协议相对/反斜杠外部地址: {0}")
    @ValueSource(strings = {
            "//evil.example/x.png",
            "/\\evil.example/x.png",
            "/%2Fevil.example/x.png",
            "/%2fevil.example/x.png"
    })
    @DisplayName("以 / 开头但实际指向外部主机的地址必须被拒绝")
    void protocolRelativeAndBackslashUrlsAreRejected(String url) {
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> controller.updateAvatar(url));
        assertEquals(400, thrown.getCode());
        assertEquals("头像URL格式无效", thrown.getMessage());
        verify(sysUserService, never()).updateAvatar(anyLong(), any());
    }

    @Test
    @DisplayName("正常的站内相对路径仍然放行")
    void sameOriginRelativePathIsStillAccepted() {
        assertDoesNotThrow(() -> controller.updateAvatar("/uploads/avatar/2026/10/20/me.png"));
        verify(sysUserService).updateAvatar(USER_ID, "/uploads/avatar/2026/10/20/me.png");
    }

    @Test
    @DisplayName("白名单内的绝对 URL 仍然放行")
    void absoluteUrlOnAnAllowedDomainIsStillAccepted() {
        assertDoesNotThrow(() -> controller.updateAvatar("https://cdn.example.com/me.png"));
        verify(sysUserService).updateAvatar(USER_ID, "https://cdn.example.com/me.png");
    }

    @Test
    @DisplayName("白名单外的绝对 URL 依旧被域名校验拦下")
    void absoluteUrlOnAnUnknownDomainIsStillRejected() {
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> controller.updateAvatar("https://evil.example/x.png"));
        assertEquals(400, thrown.getCode());
        assertEquals("头像URL域名不在允许范围内", thrown.getMessage());
    }

    @Test
    @DisplayName("封面图走同一套校验，协议相对 URL 同样被拒绝")
    void coverImageUsesTheSameValidation() {
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> controller.updateCoverImage("//evil.example/cover.png"));
        assertEquals(400, thrown.getCode());
        assertEquals("封面图URL格式无效", thrown.getMessage());
        verify(sysUserService, never()).updateCoverImage(eq(USER_ID), any());
    }
}
