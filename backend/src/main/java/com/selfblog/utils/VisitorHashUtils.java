package com.selfblog.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 访客/IP 伪名化哈希（S7）。
 *
 * <p>修复前的实现是 {@code SHA-256(secret + "|" + value)}：与 JWT 签名复用同一个
 * {@code JWT_SECRET}，无域分隔，单轮哈希。读过 {@code JWT_SECRET} 的人可以离线暴力枚举
 * 整个 IPv4 空间，而后台访客列表又展示了哈希前 12 个十六进制字符，等于给了验证 oracle。</p>
 *
 * <p>现在改为：先用 HMAC-SHA256（固定标签 {@link #KEY_DERIVATION_LABEL}）从原密钥派生出
 * 一把**伪名化专用**子密钥，再用这把子密钥 HMAC 目标值。同一个 secret 因此不会被两个
 * 用途共用；派生是确定性的，同一 secret + 同一 value 永远得到同一哈希。</p>
 *
 * <p><b>兼容性（有意为之，不做 schema 迁移）</b>：升级后新写入的哈希与历史行不同。
 * 历史 {@code visitor_event.ip_hash/visitor_hash}、{@code anonymous_post_like.visitor_hash}、
 * {@code guest_comment.visitor_hash} 保留旧值，因此升级后同一访客对**旧**记录的去重与
 * 「是否已点赞」判定会失配一次（旧点赞看起来像没点过）。这是本轮刻意接受的一次性影响：
 * 换密钥语义必然改变哈希，而为此写数据迁移风险更高。</p>
 */
public final class VisitorHashUtils {

    /**
     * 密钥派生标签：把「伪名化」与「JWT 签名」两个用途在密码学上分开。
     * 改变这个字符串会改变全部新哈希，不要随意调整。
     */
    public static final String KEY_DERIVATION_LABEL = "selfblog:visitor-hash:v1";

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final HexFormat HEX = HexFormat.of();

    private VisitorHashUtils() {
    }

    /**
     * 从原始密钥派生伪名化专用子密钥。
     *
     * @throws IllegalStateException 密钥缺失或为空——绝不静默退化成公开常量
     */
    public static byte[] deriveKey(String secret) {
        requireUsableSecret(secret);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(KEY_DERIVATION_LABEL.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("无法派生访客伪名化密钥", e);
        }
    }

    /**
     * 校验密钥可用性。启动期由 {@code VisitorHashKeyConfig} 调用，做到「缺失即失败」，
     * 而不是等到第一个访客请求才发现。
     */
    public static void requireUsableSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "访客伪名化密钥缺失：请配置 app.visitor-hash-secret 或 JWT_SECRET（不再提供 local-visitor-hash 之类的公开默认值）");
        }
    }

    /**
     * 用派生子密钥做 HMAC-SHA256，返回小写十六进制。
     *
     * @param value  待伪名化的值（访客标识、IP 等）
     * @param secret 原始密钥（{@code app.visitor-hash-secret} 或 {@code JWT_SECRET}）
     */
    public static String hash(String value, String secret) {
        byte[] derivedKey = deriveKey(secret);
        String normalized = value == null ? "" : value;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(derivedKey, HMAC_ALGORITHM));
            return HEX.formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("无法计算访客伪名化哈希", e);
        }
    }
}
