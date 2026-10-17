package com.selfblog.utils;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 工具类
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CREDENTIAL_VERSION = "credentialVersion";

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 使用持久密码哈希作为凭据版本；只将域分离的 HMAC 放入 JWT，不暴露 BCrypt 哈希。
     * 登录必须传入实际通过密码验证的快照，不能在验证后重新读取新版密码来签发令牌。
     */
    public String credentialVersionFor(SysUser user) {
        if (user == null || user.getId() == null || !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(401, "用户凭据不可用，请重新登录");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String input = "selfblog:credential-version:v1\n" + user.getId() + "\n" + user.getPassword();
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("无法计算凭据版本", e);
        }
    }

    public String getCredentialVersionFromToken(String token) {
        String version = parseToken(token).get(CREDENTIAL_VERSION, String.class);
        if (!StringUtils.hasText(version)) {
            throw new BusinessException(401, "登录凭据已升级，请重新登录");
        }
        return version;
    }

    /**
     * Token黑名单：存储已撤销的Token
     * 生产环境应使用 Redis 实现分布式 Token 黑名单
     * 使用 ConcurrentHashMap 存储，带有过期时间，用于自动清理
     */
    private final Map<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

    /**
     * 用户设备会话映射: userId -> deviceTokens (CopyOnWriteArraySet for concurrent iteration)
     * 用于支持选择性登出特定设备
     */
    private final Map<Long, Set<String>> userDeviceTokens = new ConcurrentHashMap<>();

    /**
     * 黑名单最大容量，防止内存无限增长
     */
    private static final int BLACKLIST_MAX_SIZE = 100_000;

    private static final int MAX_DEVICES_PER_USER = 10;

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT密钥长度必须至少32个字符");
        }
    }

    /**
     * 生成 Token（包含角色）
     */
    public String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, credentialVersionFor(sysUserMapper.selectById(userId)));
    }

    public String generateToken(Long userId, String username, String role, String credentialVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put(CREDENTIAL_VERSION, credentialVersion);
        return createToken(claims, username, expiration);
    }

    /**
     * 生成刷新Token（用于获取新的访问Token）
     */
    public String generateRefreshToken(Long userId, String username, String role) {
        return generateRefreshToken(userId, username, role, credentialVersionFor(sysUserMapper.selectById(userId)));
    }

    public String generateRefreshToken(Long userId, String username, String role, String credentialVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put(CREDENTIAL_VERSION, credentialVersion);
        claims.put("type", "refresh");
        claims.put("jti", java.util.UUID.randomUUID().toString());
        return createToken(claims, username, refreshExpiration);
    }

    /**
     * 注册用户设备令牌（用于多设备管理和选择性登出）
     * @param userId 用户ID
     * @param token 访问令牌
     */
    public void registerDeviceToken(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        Set<String> devices = userDeviceTokens.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        synchronized (devices) {
            devices.add(token);
            while (devices.size() > MAX_DEVICES_PER_USER) {
                String oldestToken = devices.iterator().next();
                devices.remove(oldestToken);
                // 旧设备Token加入黑名单（记录过期时间用于自动清理）
                tokenBlacklist.put(oldestToken, getExpirationFromToken(oldestToken));
            }
        }
    }

    /**
     * 获取Token的过期时间戳（毫秒）
     */
    private Long getExpirationFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().getTime();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * 登出指定设备
     * @param userId 用户ID
     * @param token 要登出的设备令牌
     */
    public void logoutDevice(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        Set<String> devices = userDeviceTokens.get(userId);
        if (devices != null) {
            devices.remove(token);
        }
        // 加入黑名单并记录过期时间
        tokenBlacklist.put(token, getExpirationFromToken(token));
        log.info("设备登出: userId={}", userId);
    }

    /**
     * 登出除指定令牌外的所有设备
     * @param userId 用户ID
     * @param excludeToken 保留的令牌（当前设备）
     */
    public void logoutOtherDevices(Long userId, String excludeToken) {
        if (userId == null) {
            return;
        }
        Set<String> devices = userDeviceTokens.get(userId);
        if (devices != null) {
            for (String deviceToken : devices) {
                if (!deviceToken.equals(excludeToken)) {
                    tokenBlacklist.put(deviceToken, getExpirationFromToken(deviceToken));
                }
            }
            devices.clear();
            if (excludeToken != null) {
                devices.add(excludeToken);
            }
        }
        log.info("其他设备已登出: userId={}", userId);
    }

    /**
     * 获取用户设备数量
     * @param userId 用户ID
     * @return 设备数量
     */
    public int getUserDeviceCount(Long userId) {
        Set<String> devices = userDeviceTokens.get(userId);
        return devices != null ? devices.size() : 0;
    }

    /**
     * 验证刷新Token是否有效
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    private String createToken(Map<String, Object> claims, String subject, Long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 将Token加入黑名单（撤销Token）
     */
    public void revokeToken(String token) {
        if (token == null) {
            return;
        }
        try {
            parseToken(token);
            if (isTokenExpired(token)) {
                return;
            }
            // 容量检查和清理
            if (tokenBlacklist.size() >= BLACKLIST_MAX_SIZE) {
                cleanExpiredTokens();
            }
            // 使用 put 而非 add，支持过期时间记录
            tokenBlacklist.put(token, getExpirationFromToken(token));
        } catch (Exception e) {
            log.warn("撤销token失败: {}", e.getMessage());
        }
    }

    /**
     * 检查Token是否已被撤销
     */
    public boolean isTokenRevoked(String token) {
        Long blacklistedUntil = token == null ? null : tokenBlacklist.get(token);
        if (blacklistedUntil != null) {
            if (blacklistedUntil <= 0 || blacklistedUntil >= System.currentTimeMillis()) {
                return true;
            }
            tokenBlacklist.remove(token, blacklistedUntil);
        }
        try {
            Claims claims = parseToken(token);
            String version = claims.get(CREDENTIAL_VERSION, String.class);
            Long userId = claims.get("userId", Long.class);
            // 无版本的升级前令牌必须重新登录，不能自动赋予当前密码版本。
            if (!StringUtils.hasText(version) || userId == null) {
                return true;
            }
            // 不缓存：撤销以数据库密码提交为界，进程重启/另一实例也能识别旧令牌。
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null || !StringUtils.hasText(user.getPassword())
                    || Integer.valueOf(1).equals(user.getIsDeleted())
                    || Integer.valueOf(0).equals(user.getStatus())) {
                return true;
            }
            String currentVersion = credentialVersionFor(user);
            return !MessageDigest.isEqual(version.getBytes(StandardCharsets.UTF_8),
                    currentVersion.getBytes(StandardCharsets.UTF_8));
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Refresh Token轮换：使用旧refresh token获取新的refresh token
     * 旧refresh token会被标记为已使用，防止重放攻击
     */
    public String rotateRefreshToken(String oldRefreshToken) {
        if (oldRefreshToken == null) {
            throw new BusinessException(401, "Refresh token不能为空");
        }
        try {
            Claims claims = parseToken(oldRefreshToken);
            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new BusinessException(401, "无效的refresh token类型");
            }
            if (isTokenRevoked(oldRefreshToken)) {
                throw new BusinessException(401, "刷新Token已过期或已撤销");
            }
            String version = getCredentialVersionFromToken(oldRefreshToken);
            revokeToken(oldRefreshToken);
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            // 保留旧凭据版本；并发改密不能把旧刷新令牌升级成新会话。
            return generateRefreshToken(userId, username, role, version);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Refresh token轮换失败: {}", e.getMessage());
            throw new BusinessException(401, "Refresh token无效或已过期");
        }
    }

    /**
     * 密码更新后清理当前进程登记的设备。
     * 所有访问/刷新令牌的持久撤销由 credentialVersion 与数据库密码不一致保证，
     * 调用方必须先在同一事务中更新密码；此内存映射不承担跨重启的撤销职责。
     * @param userId 用户ID
     */
    public void revokeAllUserTokens(Long userId) {
        if (userId == null) {
            return;
        }
        Set<String> devices = userDeviceTokens.get(userId);
        if (devices != null) {
            for (String deviceToken : devices) {
                tokenBlacklist.put(deviceToken, getExpirationFromToken(deviceToken));
            }
            devices.clear();
        }
        log.info("已撤销用户所有Token: userId={}", userId);
    }

    /**
     * 清理过期Token黑名单
     * 注意：生产环境应使用Redis并设置TTL自动过期
     */
    public void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        tokenBlacklist.entrySet().removeIf(entry -> {
            Long expiration = entry.getValue();
            return expiration == null || expiration <= 0 || expiration < now;
        });
    }

    /**
     * 从请求中提取 Token
     * @param request HTTP请求
     * @return Token字符串，如果不存在返回null
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true; // 已过期
        } catch (io.jsonwebtoken.JwtException e) {
            // 其他 JWT 解析错误（格式错误、签名验证失败等）不算过期，而是无效
            // 这里返回 false 让调用方通过 parseToken 的结果判断
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            throw new BusinessException(401, "无效的Token");
        }
        return userId;
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从 Token 中获取用户角色
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
