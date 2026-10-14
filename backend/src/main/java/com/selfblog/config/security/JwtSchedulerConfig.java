package com.selfblog.config.security;

import com.selfblog.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JWT相关定时任务配置
 */
@Component
@EnableScheduling
public class JwtSchedulerConfig {

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 每小时清理一次过期Token黑名单
     * 注意：生产环境建议使用Redis存储黑名单并设置TTL自动过期
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void cleanExpiredTokens() {
        jwtUtils.cleanExpiredTokens();
    }
}
