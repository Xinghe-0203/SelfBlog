package com.selfblog.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全配置属性类
 * 集中管理登录锁定等安全相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 登录锁定配置
     */
    private LoginLock login = new LoginLock();

    @Data
    public static class LoginLock {
        /**
         * 最大失败次数，超过后锁定账户
         */
        private int maxFailCount = 5;

        /**
         * 锁定时间（分钟）
         */
        private int lockMinutes = 15;
    }
}