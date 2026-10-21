package com.selfblog.config;

import com.selfblog.utils.VisitorHashUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 访客/IP 伪名化密钥的启动期校验（S7）。
 *
 * <p>修复前三个服务各自写 {@code @Value("${JWT_SECRET:local-visitor-hash}")}：密钥缺失时
 * 会静默退化成一个人人可见的常量，哈希因此完全可预测；同时它复用 JWT 签名密钥，
 * 让「读过 JWT_SECRET 的人」可以离线枚举整个 IPv4 空间。</p>
 *
 * <p>现在：密钥来自 {@code app.visitor-hash-secret}（默认取 {@code JWT_SECRET}），
 * 缺失即启动失败——把配置错误暴露在启动期，而不是等到第一个访客请求才 500。
 * 派生与哈希规则见 {@link VisitorHashUtils}。</p>
 */
@Configuration
public class VisitorHashKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(VisitorHashKeyConfig.class);

    @Value("${app.visitor-hash-secret:}")
    private String visitorHashSecret;

    @PostConstruct
    public void validateVisitorHashSecret() {
        VisitorHashUtils.requireUsableSecret(visitorHashSecret);
        log.info("[VisitorHash] 访客伪名化密钥已加载（来源 app.visitor-hash-secret / VISITOR_HASH_SECRET / JWT_SECRET）");
    }
}
