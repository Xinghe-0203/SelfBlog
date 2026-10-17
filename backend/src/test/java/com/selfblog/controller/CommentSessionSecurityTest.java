package com.selfblog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfblog.dto.auth.UserLoginRequest;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.utils.UserContext;
import com.selfblog.vo.user.UserLoginResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.api-prefix=/api", "app.single-author-mode=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentSessionSecurityTest {
    private static final String OLD_PASSWORD = "OldPassword123!";
    private static final String NEW_PASSWORD = "NewPassword456!";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired SysUserMapper users;
    @Autowired SysUserService service;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired JwtUtils jwt;
    @Autowired SqlSessionFactory sqlSessions;
    private SysUser author;

    @BeforeEach
    void createAuthor() {
        SecurityContextHolder.clearContext();
        author = new SysUser();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        author.setUsername("security_" + unique);
        author.setEmail(unique + "@example.test");
        author.setPassword(encoder.encode(OLD_PASSWORD));
        author.setNickname("测试作者");
        author.setRole("admin");
        author.setStatus(1);
        author.setLoginFailCount(0);
        author.setIsDeleted(0);
        users.insert(author);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void realCommentCreationAllowsFiveThenReturns429AndOtherIpStillWorks() throws Exception {
        jdbc.update("INSERT INTO blog_post(id,user_id,title,content,status,is_deleted,create_time) "
                + "VALUES(1999001,?,'限流回归文章','正文',1,0,CURRENT_TIMESTAMP)", author.getId());
        String body = """
                {"postId":1999001,"visitorId":"security-test-visitor","nickname":"读者","content":"真实评论限流回归"}
                """;
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/guest-comments").with(request -> {
                        request.setRemoteAddr("203.0.113.31"); return request;
                    }).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        }
        mvc.perform(post("/api/guest-comments").with(request -> {
                    request.setRemoteAddr("203.0.113.31"); return request;
                }).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value(429));
        mvc.perform(post(java.net.URI.create("/api/%67uest-comments")).with(request -> {
                    request.setRemoteAddr("203.0.113.31"); return request;
                }).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
        mvc.perform(post("/api/guest-comments").with(request -> {
                    request.setRemoteAddr("203.0.113.32"); return request;
                }).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        assertEquals(6, jdbc.queryForObject("SELECT COUNT(*) FROM guest_comment WHERE post_id=1999001", Integer.class));
    }

    @Test
    void changingPasswordRejectsAllOldSessionsAndNewPasswordCanLogin() throws Exception {
        UserLoginResponse old = login(OLD_PASSWORD);
        UserLoginResponse anotherDevice = login(OLD_PASSWORD);
        String unregisteredAccess = jwt.generateToken(author.getId(), author.getUsername(), "admin");
        String unregisteredRefresh = jwt.generateRefreshToken(author.getId(), author.getUsername(), "admin");
        mvc.perform(get("/api/admin/site-settings").header("Authorization", "Bearer " + old.getToken()))
                .andExpect(status().isOk());
        mvc.perform(put("/api/user/password").header("Authorization", "Bearer " + old.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"" + OLD_PASSWORD + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        assertRevokedAcrossInstances(old);
        assertRevokedAcrossInstances(anotherDevice);
        assertTrue(freshJwtInstance().isTokenRevoked(unregisteredAccess));
        assertTrue(freshJwtInstance().isTokenRevoked(unregisteredRefresh));
        mvc.perform(get("/api/admin/site-settings").header("Authorization", "Bearer " + old.getToken()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/user/refresh").header("Authorization", "Bearer " + anotherDevice.getRefreshToken()))
                .andExpect(status().isUnauthorized());
        assertNewLoginWorks(NEW_PASSWORD);
    }

    @Test
    void refreshedTokensAlsoLoseValidityAfterPasswordChange() throws Exception {
        UserLoginResponse original = login(OLD_PASSWORD);
        String payload = mvc.perform(post("/api/user/refresh")
                        .header("Authorization", "Bearer " + original.getRefreshToken()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode next = json.readTree(payload).path("data");
        String access = next.path("token").asText();
        String refresh = next.path("refreshToken").asText();
        assertFalse(jwt.isTokenRevoked(access));
        assertFalse(jwt.isTokenRevoked(refresh));
        service.changePassword(author.getId(), OLD_PASSWORD, NEW_PASSWORD);
        assertTrue(freshJwtInstance().isTokenRevoked(access));
        assertTrue(freshJwtInstance().isTokenRevoked(refresh));
        mvc.perform(post("/api/user/refresh").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administratorResetInvalidatesBothTokenTypes() {
        UserLoginResponse old = login(OLD_PASSWORD);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserContext(author.getId(), "admin"), null, List.of(new SimpleGrantedAuthority("ROLE_admin"))));
        String newPassword = service.resetPassword(author.getId());
        assertRevokedAcrossInstances(old);
        assertNewLoginWorks(newPassword);
    }

    @Test
    void emailResetInvalidatesBothTokenTypes() {
        UserLoginResponse old = login(OLD_PASSWORD);
        service.resetPassword(author.getEmail(), NEW_PASSWORD);
        assertRevokedAcrossInstances(old);
        assertNewLoginWorks(NEW_PASSWORD);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void committedPasswordChangeIsObservedByAnIndependentAuthInstanceAndSqlSession() {
        try {
            UserLoginResponse old = login(OLD_PASSWORD);
            // 无外层测试事务：服务事务在返回前真正提交；不能靠共享会话看到未提交密码。
            service.changePassword(author.getId(), OLD_PASSWORD, NEW_PASSWORD);
            UserLoginResponse current = login(NEW_PASSWORD);
            try (SqlSession session = sqlSessions.openSession(true)) {
                SysUserMapper independentUsers = session.getMapper(SysUserMapper.class);
                assertTrue(encoder.matches(NEW_PASSWORD, independentUsers.selectById(author.getId()).getPassword()));
                JwtUtils restarted = freshJwtInstance();
                ReflectionTestUtils.setField(restarted, "sysUserMapper", independentUsers);
                assertTrue(restarted.isTokenRevoked(old.getToken()));
                assertTrue(restarted.isTokenRevoked(old.getRefreshToken()));
                assertFalse(restarted.isTokenRevoked(current.getToken()));
                assertFalse(restarted.isTokenRevoked(current.getRefreshToken()));
            }
        } finally {
            // 此项特意提交真实数据，完成后清理，不污染其他测试或 CI 的 MySQL 数据。
            jdbc.update("DELETE FROM sys_user WHERE id=?", author.getId());
        }
    }

    @Test
    void staleEntityUpdateCannotRestorePasswordOrResurrectOldTokens() {
        UserLoginResponse old = login(OLD_PASSWORD);
        SysUser stale = users.selectById(author.getId());
        service.changePassword(author.getId(), OLD_PASSWORD, NEW_PASSWORD);
        stale.setNickname("并发资料更新");
        users.updateById(stale);
        SysUser current = users.selectById(author.getId());
        assertEquals("并发资料更新", current.getNickname());
        assertTrue(encoder.matches(NEW_PASSWORD, current.getPassword()));
        assertFalse(encoder.matches(OLD_PASSWORD, current.getPassword()));
        assertRevokedAcrossInstances(old);
    }

    @Test
    void stalePasswordCompareAndSetCannotOverwriteANewerPassword() {
        String oldHash = author.getPassword();
        service.changePassword(author.getId(), OLD_PASSWORD, NEW_PASSWORD);
        assertEquals(0, users.updatePasswordIfUnchanged(author.getId(), oldHash, encoder.encode("Conflicting789!")));
        assertTrue(encoder.matches(NEW_PASSWORD, users.selectById(author.getId()).getPassword()));
    }

    private UserLoginResponse login(String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername(author.getUsername());
        request.setPassword(password);
        return service.login(request);
    }

    private void assertNewLoginWorks(String password) {
        UserLoginResponse next = login(password);
        assertFalse(jwt.isTokenRevoked(next.getToken()));
        assertFalse(jwt.isTokenRevoked(next.getRefreshToken()));
        assertFalse(freshJwtInstance().isTokenRevoked(next.getToken()));
    }

    private void assertRevokedAcrossInstances(UserLoginResponse old) {
        assertTrue(jwt.isTokenRevoked(old.getToken()));
        assertTrue(jwt.isTokenRevoked(old.getRefreshToken()));
        JwtUtils restarted = freshJwtInstance();
        assertTrue(restarted.isTokenRevoked(old.getToken()));
        assertTrue(restarted.isTokenRevoked(old.getRefreshToken()));
    }

    private JwtUtils freshJwtInstance() {
        JwtUtils instance = new JwtUtils();
        ReflectionTestUtils.setField(instance, "sysUserMapper", users);
        ReflectionTestUtils.setField(instance, "secret", ReflectionTestUtils.getField(jwt, "secret"));
        ReflectionTestUtils.setField(instance, "expiration", 3_600_000L);
        ReflectionTestUtils.setField(instance, "refreshExpiration", 604_800_000L);
        return instance;
    }
}
