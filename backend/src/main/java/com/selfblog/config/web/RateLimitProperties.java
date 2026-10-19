package com.selfblog.config.web;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 限流配置属性类
 * 集中管理接口限流阈值配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /**
     * 登录接口限流（次/分钟）
     */
    private int login = 10;

    /**
     * 注册接口限流（次/分钟）
     */
    private int register = 5;

    /**
     * 发送验证码接口限流（次/分钟）
     */
    private int sendCode = 3;

    /**
     * 重置密码接口限流（次/分钟）
     */
    private int resetPassword = 5;

    /**
     * 用户搜索接口限流（次/分钟）
     */
    private int userSearch = 20;

    /**
     * 媒体上传接口限流（次/分钟）
     */
    private int mediaUpload = 10;

    /**
     * 校友圈发帖限流（次/分钟）
     */
    private int circlePost = 10;

    /**
     * 用户通用接口限流（次/分钟）
     */
    private int userGeneral = 30;

    /**
     * 文章通用接口限流（次/分钟）
     */
    private int postGeneral = 10;

    /**
     * 文章列表/搜索等**只读**接口限流（次/分钟）。
     *
     * 为什么单列：`/api/post/` 的 10/min 会一并命中 `GET /api/post/list`，而列表是
     * 首页与归档页的常规浏览路径（生产 request.log 中该接口被高频调用、P95≈1.35s）。
     * 真实访客翻页几次就会被 429 误伤，所以读路径必须比写路径宽松。
     * 取值依据：对齐同样面向匿名访客的 `visitor=60`，并低于站点页面读取 120；
     * 它只放宽读路径，写路径仍走 postGeneral=10，不削弱写入防护。
     */
    private int postList = 60;

    /**
     * 评论接口限流（次/分钟）
     */
    private int comment = 20;

    /**
     * 点赞接口限流（次/分钟）
     */
    private int like = 30;

    /**
     * 收藏接口限流（次/分钟）
     */
    private int collect = 30;

    /**
     * 校友圈通用限流（次/分钟）
     */
    private int circleGeneral = 10;

    /**
     * 媒体通用限流（次/分钟）
     */
    private int mediaGeneral = 10;

    /**
     * 分享接口限流（次/分钟）
     */
    private int share = 20;

    /**
     * 私信接口限流（次/分钟）
     */
    private int message = 20;

    /**
     * 认证接口限流（次/分钟）
     */
    private int auth = 20;

    /**
     * 标签接口限流（次/分钟）
     */
    private int tag = 30;

    /**
     * 话题接口限流（次/分钟）
     */
    private int topic = 30;

    /**
     * 关注接口限流（次/分钟）
     */
    private int follow = 20;

    /**
     * 趋势接口限流（次/分钟）
     */
    private int trending = 20;

    /**
     * 通知接口限流（次/分钟）
     */
    private int notification = 30;

    /**
     * 举报接口限流（次/分钟）
     */
    private int report = 10;

    /**
     * 统计接口限流（次/分钟）
     */
    private int statistics = 20;

    private int visitor = 60;

    private int anonymousLike = 30;

    private int guestComment = 5;

    private int gallery = 60;

    private int galleryUpload = 20;

    /**
     * 站点品牌资产上传限流（次/分钟）。
     * 允许单文件 20MB，比相册上传更重，所以给得比 galleryUpload 宽松一点。
     */
    private int siteAssetUpload = 30;

    /**
     * 所有限流配置的 Map 形式（路径前缀 → 每分钟上限），便于动态查找。
     *
     * 热路径：每次请求都会调用，所以这里返回构造期一次性构建的**不可变**快照，
     * 不再逐请求 new HashMap + put（原实现 33 次 put/请求）。
     * 字段可被 Spring 绑定，因此用 volatile + 失效重建而不是 final：
     * 直接在测试里 `new RateLimitProperties()` 后 setter 改值也能拿到新阈值。
     */
    private volatile Map<String, Integer> limitsSnapshot;

    public Map<String, Integer> getAllLimits() {
        Map<String, Integer> snapshot = limitsSnapshot;
        if (snapshot == null) {
            snapshot = buildLimits();
            limitsSnapshot = snapshot;
        }
        return snapshot;
    }

    /**
     * 供 Spring 绑定完成后（以及测试改值后）显式重建快照。
     */
    @PostConstruct
    public void refreshLimits() {
        this.limitsSnapshot = buildLimits();
    }

    private Map<String, Integer> buildLimits() {
        Map<String, Integer> limits = new HashMap<>();
        limits.put("/api/user/login", login);
        limits.put("/api/user/register", register);
        limits.put("/api/user/send-code", sendCode);
        limits.put("/api/user/reset-password", resetPassword);
        limits.put("/api/user/search", userSearch);
        limits.put("/api/media/upload", mediaUpload);
        limits.put("/api/circle/post", circlePost);
        limits.put("/api/user/", userGeneral);
        limits.put("/api/post/list", postList);
        limits.put("/api/post/", postGeneral);
        limits.put("/api/comment/", comment);
        limits.put("/api/like/", like);
        limits.put("/api/collect/", collect);
        limits.put("/api/circle/", circleGeneral);
        limits.put("/api/media/", mediaGeneral);
        limits.put("/api/share/", share);
        limits.put("/api/message/", message);
        limits.put("/api/auth/", auth);
        limits.put("/api/tag/", tag);
        limits.put("/api/topic/", topic);
        limits.put("/api/follow/", follow);
        limits.put("/api/trending/", trending);
        limits.put("/api/notification/", notification);
        limits.put("/api/report/", report);
        limits.put("/api/statistics/", statistics);
        limits.put("/api/visitor/", visitor);
        limits.put("/api/anonymous-likes/", anonymousLike);
        limits.put("/api/guest-comments/", guestComment);
        limits.put("/api/gallery/", gallery);
        limits.put("/api/admin/albums/", galleryUpload);
        limits.put("/api/admin/site-assets", siteAssetUpload);
        limits.put("/api/site/pages", 120);
        limits.put("/api/admin/pages", 60);
        limits.put("/api/admin/page-navigation", 30);
        return Collections.unmodifiableMap(limits);
    }
}
