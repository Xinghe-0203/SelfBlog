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
     * 文章**详情**等只读 GET 接口限流（次/分钟）。
     *
     * 为什么单列：`/api/post/` 的 10/min 是按写路径定的（PUT/DELETE 更新与删除），
     * 但 `GET /api/post/{id}`（文章详情）同样落在这条前缀下，于是访客正常阅读
     * 第 11 篇文章就会被 429。实测（`GET /api/post/1..12`，同一 IP）：
     * 前 10 次 200、第 11 次起 429——对一个个人博客的主阅读路径是硬伤。
     * 取值对齐同样面向匿名访客的 `postList=60`：正常连读 60 篇/分钟远高于人手点击速度，
     * 而写路径仍走 `postGeneral=10`，不削弱写入防护。
     */
    private int postRead = 60;

    /**
     * 搜索**联想/高级搜索**只读 GET 接口限流（次/分钟）。
     *
     * 为什么单列：`GET /api/post/search/suggest`（标题自动补全，typeahead）此前只能落到
     * `/api/post` 的 60/min 读桶——读前缀 `/api/post` 与写前缀 `/api/post/` 去尾斜杠后等长，
     * 靠「等长时读优先」胜出。而前端联想是「停顿约 300ms 就发一次」的节奏，快速连续输入
     * 理论上限约 200/min，60/min 在持续输入十几秒后就会打满，之后同桶的搜索请求全部 429。
     * 取 120/min：覆盖正常联想节奏且仍是纯读阈值；本条只登记在**读表**里，
     * 写路径（`/api/post/` 的 PUT/DELETE 10/min）不受任何影响。
     * `GET /api/post/search/advanced`（高级搜索）同属这条更长的读前缀，一并受益。
     */
    private int postSearchRead = 120;

    /**
     * 文章**图片列表**只读 GET 接口限流（次/分钟）。
     *
     * 为什么单列：`GET /api/media/post/{postId}`（读取一篇文章的全部配图）此前只能落到
     * 写表 `/api/media/` 的 10/min（mediaUpload/mediaGeneral 都是按「上传重操作」定的），
     * 而它是纯只读浏览——读者翻看第 11 篇带图文章就会被 429。登记在**读表**里
     * 60/min（对齐同样面向匿名访客的 postRead/postList），写路径
     * （`POST /api/media/upload` 的 10/min）不受任何影响。
     */
    private int mediaPostRead = 60;

    /**
     * 访客评论**列表**读取限流（次/分钟）。
     *
     * 为什么单列：`/api/guest-comments/` 的 5/min 是给「发表评论」定的，
     * 但 `GET /api/guest-comments/post/{postId}`（公开评论列表）是纯只读浏览，
     * 也落在这条前缀下。实测（`GET /api/guest-comments/post/1..8`，同一 IP）：
     * 前 5 次 200、第 6 次起 429——读者翻看第 6 篇文章的评论区就被拦。
     * 读路径放宽到 60/min，写路径（POST 创建评论）仍保持 5/min。
     */
    private int guestCommentRead = 60;

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
     * 后台接口兜底限流（次/分钟），S8。
     *
     * 后台命名空间里只有少数几个前缀有具名阈值（albums/site-assets/pages/page-navigation），
     * 其余接口此前完全不限流。这里补一条 {@code /api/admin/} 兜底；
     * 具名前缀仍然靠「最长前缀优先」生效，不会被这条兜底覆盖。
     */
    private int adminCatchAll = 60;

    /**
     * 未命中任何具名前缀的 {@code /api/**} 兜底限流（次/分钟），S8。
     *
     * 原实现里「没匹配到任何前缀」直接放行（{@code if (!matched) return true;}），
     * 等于任何新增/未登记接口默认无限流。取 60/min 而不是更低，是因为既有用例
     * {@code prefixMustEndAtAPathSegmentBoundary} 要求同一 IP 连续 35 次访问
     * 未登记路径仍不被拦——兜底阈值必须高于它，否则会误伤正常浏览。
     */
    private int apiFallback = 60;

    /**
     * 所有限流配置的 Map 形式（路径前缀 → 每分钟上限），便于动态查找。
     *
     * 热路径：每次请求都会调用，所以这里返回构造期一次性构建的**不可变**快照，
     * 不再逐请求 new HashMap + put（原实现 33 次 put/请求）。
     * 字段可被 Spring 绑定，因此用 volatile + 失效重建而不是 final：
     * 直接在测试里 `new RateLimitProperties()` 后 setter 改值也能拿到新阈值。
     */
    private volatile Map<String, Integer> limitsSnapshot;

    /**
     * 只读路径（GET/HEAD/OPTIONS）专用阈值快照（路径前缀 → 每分钟上限）。
     *
     * 为什么需要单独一张表：限流是按**路径前缀**匹配的，同一个前缀下可能同时挂着
     * 写路径与读路径（`PUT /api/post/{id}` 更新 vs `GET /api/post/{id}` 详情），
     * 单靠路径无法区分，只能再看 HTTP 方法。写路径必须保持严格（更新/删除 10/min），
     * 读路径必须宽松（正常阅读）。
     *
     * 命中规则：先按路径做最长前缀匹配；当只读方法命中的读前缀**不短于**写前缀时，
     * 读前缀胜出（等长时读优先，即同一前缀的 GET 用读阈值、PUT/DELETE 用写阈值）。
     * 更长的具名写前缀仍然胜出，所以 `/api/post/list` 依旧走自己那条 60/min 的桶。
     */
    private volatile Map<String, Integer> readLimitsSnapshot;

    public Map<String, Integer> getAllLimits() {
        Map<String, Integer> snapshot = limitsSnapshot;
        if (snapshot == null) {
            snapshot = buildLimits();
            limitsSnapshot = snapshot;
        }
        return snapshot;
    }

    /**
     * 只读路径专用阈值（前缀 → 每分钟上限）。语义见 {@link #readLimitsSnapshot}。
     */
    public Map<String, Integer> getReadLimits() {
        Map<String, Integer> snapshot = readLimitsSnapshot;
        if (snapshot == null) {
            snapshot = buildReadLimits();
            readLimitsSnapshot = snapshot;
        }
        return snapshot;
    }

    /**
     * 供 Spring 绑定完成后（以及测试改值后）显式重建快照。
     */
    @PostConstruct
    public void refreshLimits() {
        this.limitsSnapshot = buildLimits();
        this.readLimitsSnapshot = buildReadLimits();
    }

    private Map<String, Integer> buildReadLimits() {
        Map<String, Integer> limits = new HashMap<>();
        // 文章详情/相邻/搜索等 GET 读路径：与 /api/post/ 的写阈值（10/min）同前缀，
        // 靠「等长时读优先」区分，因此这里的前缀必须与写前缀完全一致（不带尾斜杠）。
        limits.put("/api/post", postRead);
        // 搜索联想/高级搜索的 GET 读路径：比写前缀 /api/post/ 更长，靠最长前缀直接胜出，
        // 不再与文章详情共用 60/min 的 /api/post 读桶。
        limits.put("/api/post/search", postSearchRead);
        // 公开评论列表：比写前缀 /api/guest-comments 更长，靠最长前缀直接胜出。
        limits.put("/api/guest-comments/post", guestCommentRead);
        // 文章配图列表：比写前缀 /api/media 更长，靠最长前缀直接胜出，
        // GET /api/media/post/** 不再吃 /api/media 的 10/min 上传写桶。
        limits.put("/api/media/post", mediaPostRead);
        return Collections.unmodifiableMap(limits);
    }

    private Map<String, Integer> buildLimits() {
        Map<String, Integer> limits = new HashMap<>();
        limits.put("/api/user/login", login);
        limits.put("/api/user/register", register);
        // 密码找回的两个端点真实位于 /api/auth/password/**（PasswordController），
        // 此前按 /api/user/send-code、/api/user/reset-password 登记，指向了不存在的路径，
        // 等于这两个重操作（发邮件/改密码）完全不限流。预算不变（3/min、5/min）。
        limits.put("/api/auth/password/send-code", sendCode);
        limits.put("/api/auth/password/reset-password", resetPassword);
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
        // S8：后台命名空间兜底。具名 admin 前缀（albums/site-assets/pages/page-navigation）
        // 更长，靠最长前缀优先继续生效。
        limits.put("/api/admin/", adminCatchAll);
        // S8：未命中任何具名前缀的 /api/** 兜底。放在最后加入，语义上仍是「最长前缀优先」，
        // 所以它不会削弱上面任何一条具名阈值，只是把「默认放行」改成「默认有限流」。
        limits.put("/api/", apiFallback);
        return Collections.unmodifiableMap(limits);
    }
}
