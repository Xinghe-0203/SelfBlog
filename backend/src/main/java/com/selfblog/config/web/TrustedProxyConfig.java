package com.selfblog.config.web;

import com.selfblog.utils.LogUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 启动时把可信代理白名单注入 {@link LogUtils}（S2）。
 *
 * <p>{@code LogUtils} 是纯静态工具类（没有 Spring 依赖，服务层与静态审计调用都在用），
 * 所以配置只能通过这个 Bean 在启动时推一次。默认值见
 * {@code application.yml} 的 {@code app.trusted-proxies}，也可用环境变量
 * {@code TRUSTED_PROXY_CIDRS} 覆盖。</p>
 */
@Configuration
public class TrustedProxyConfig {

    @Value("${app.trusted-proxies:127.0.0.1/32,::1/128}")
    private String trustedProxies;

    @PostConstruct
    public void initTrustedProxies() {
        LogUtils.configureTrustedProxies(trustedProxies);
    }
}
