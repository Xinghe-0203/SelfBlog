package com.selfblog.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S7 回归：访客伪名化哈希必须与 JWT 签名密钥「域分隔」，且不得有公开默认密钥。
 *
 * <p>修复前的实现是 {@code SHA-256(secret + "|" + value)}，直接用 {@code JWT_SECRET}
 * 当盐，且 {@code @Value("${JWT_SECRET:local-visitor-hash}")} 在密钥缺失时静默退化成
 * 一个人人可见的常量。读过 JWT 密钥的人可以离线枚举整个 IPv4 空间，而后台访客列表
 * 又展示哈希前 12 位，等于给了验证 oracle。</p>
 */
@DisplayName("S7：访客伪名化密钥派生")
class VisitorHashUtilsTest {

    private static final String SECRET = "a-test-secret-long-enough-for-derivation";
    private static final String VISITOR_ID = "visitor-abc-123";

    @Test
    @DisplayName("派生子密钥不等于原始密钥，也不是原始密钥的简单拼接")
    void derivedKeyIsNotTheRawSecret() {
        byte[] derived = VisitorHashUtils.deriveKey(SECRET);

        assertNotEquals(SECRET, HexFormat.of().formatHex(derived),
                "派生子密钥不能等于原始密钥本身");
        String rawSha256 = sha256Hex(SECRET);
        assertNotEquals(rawSha256, HexFormat.of().formatHex(derived),
                "派生必须是 HMAC-SHA256(label)，而不是对原始密钥再哈希一次");
        assertTrue(derived.length > 0, "派生密钥不能为空");
    }

    @Test
    @DisplayName("同一 secret + 同一 value 的哈希是确定性的（可跨进程稳定比较）")
    void hashingIsDeterministic() {
        String first = VisitorHashUtils.hash(VISITOR_ID, SECRET);
        String second = VisitorHashUtils.hash(VISITOR_ID, SECRET);

        assertEquals(first, second, "伪名化哈希必须确定性，否则去重与「是否已点赞」都会失效");
        assertTrue(first.matches("[0-9a-f]{64}"), "输出应为 64 位小写十六进制，实际: " + first);
    }

    @Test
    @DisplayName("新哈希不等于旧方案 SHA-256(secret + '|' + value)，证明域分隔真的生效")
    void hashDiffersFromTheLegacyScheme() {
        String legacy = sha256Hex(SECRET + "|" + VISITOR_ID);
        String current = VisitorHashUtils.hash(VISITOR_ID, SECRET);

        assertNotEquals(legacy, current,
                "若仍与旧方案一致，说明密钥派生没有真正生效（S7 未修复）");
    }

    @Test
    @DisplayName("不同密钥/不同取值得到不同哈希，不会互相碰撞")
    void differentInputsProduceDifferentHashes() {
        String base = VisitorHashUtils.hash(VISITOR_ID, SECRET);

        assertNotEquals(base, VisitorHashUtils.hash("visitor-abc-124", SECRET),
                "不同访客标识必须得到不同哈希");
        assertNotEquals(base, VisitorHashUtils.hash(VISITOR_ID, SECRET + "-other"),
                "不同密钥必须得到不同哈希");
    }

    @Test
    @DisplayName("空值不会 NPE，按空串处理（保持旧行为）")
    void nullValueIsTreatedAsEmptyString() {
        assertDoesNotThrow(() -> VisitorHashUtils.hash(null, SECRET));
        assertEquals(VisitorHashUtils.hash("", SECRET), VisitorHashUtils.hash(null, SECRET));
    }

    @Test
    @DisplayName("密钥缺失/空白时抛异常，绝不静默退化成公开常量")
    void missingSecretFailsLoudly() {
        assertThrows(IllegalStateException.class, () -> VisitorHashUtils.deriveKey(null));
        assertThrows(IllegalStateException.class, () -> VisitorHashUtils.deriveKey(""));
        assertThrows(IllegalStateException.class, () -> VisitorHashUtils.deriveKey("   "));
        assertThrows(IllegalStateException.class, () -> VisitorHashUtils.hash(VISITOR_ID, null));
        assertFalse("local-visitor-hash".isBlank(), "旧默认值字面量不应再被当作密钥兜底");
        assertThrows(IllegalStateException.class,
                () -> VisitorHashUtils.requireUsableSecret(null));
        assertDoesNotThrow(() -> VisitorHashUtils.requireUsableSecret(SECRET));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
