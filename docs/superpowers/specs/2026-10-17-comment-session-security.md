# 评论限流与改密会话失效

## 范围

修复匿名评论创建限流遗漏，以及改密/重置密码后旧访问令牌和刷新令牌仍可使用的问题。不改文章缓存、草稿箱或其他产品功能。

## 已核实的现状

| 证据 | 当前行为 |
| --- | --- |
| `backend/src/main/java/com/selfblog/controller/site/GuestCommentController.java` | 创建路径为 `POST /api/guest-comments`，不带尾斜杠 |
| `backend/src/main/java/com/selfblog/config/web/RateLimitProperties.java`、`RateLimitInterceptor.java` | 配置为 `/api/guest-comments/`，原始 `startsWith` 漏掉根路径；HashMap 首个命中也不保证专用规则优先 |
| `backend/src/main/java/com/selfblog/utils/JwtUtils.java` | 黑名单和设备登记仅在内存中，无法持久撤销未登记的刷新令牌 |
| `backend/src/main/java/com/selfblog/service/auth/impl/SysUserServiceImpl.java` | 三种改密/重置路径均持久更新 BCrypt 密码；非密码操作也会用完整旧实体更新用户 |
| `backend/src/main/java/com/selfblog/controller/user/SysUserController.java` | 刷新接口自己检查令牌，重新签发访问和刷新令牌 |

## 方案

1. 限流规则按路径段边界匹配：尾斜杠规则同时覆盖根路径和子路径，最长规则优先；保留现有每 IP、每路径计数、阈值和窗口。
2. JWT 新增 `credentialVersion`：对域分离的用户 ID 和持久 BCrypt 哈希用 JWT 密钥执行 HMAC-SHA256，base64url 编码；不向令牌暴露密码或 BCrypt 哈希。认证/刷新时直接读当前用户并常量时间比较；不缓存版本。
3. 登录使用实际通过密码验证的用户快照生成同一版本的令牌对。刷新必须验证版本，并把旧刷新令牌的版本原样传给新令牌，避免并发改密时升级旧凭据。轮换辅助方法也检查撤销。
4. 用户实体密码字段禁止通用实体更新；所有密码更新通过专用 Mapper 的旧哈希条件更新，防止旧快照覆盖并发改密。首次初始化仍可插入密码，旧示例密码升级走专用更新。登录失败状态采用显式字段更新以清空锁定时间。
5. 不增加表、迁移或 Redis。升级前无版本的旧 JWT 一律失效，需重新登录。密码事务提交后，旧令牌在进程重启、其他实例清空内存黑名单后仍失效；不声称中断已授权的在途请求，也不把现有单设备登出/刷新重放内存控制升级为分布式持久会话。

## 验收

- 真实 `POST /api/guest-comments` 同 IP 前五次通过，第六次 429；其他 IP 独立；路径边界、专用规则优先有回归。
- 改密、管理员重置、邮箱重置后，旧 access/refresh（包括未登记及刷新签发的令牌）均失效，新密码登录正常。
- 新 JwtUtils 实例共享数据库及密钥时仍拒绝旧令牌；无版本、缺失用户/密码、数据库失败不能认证/签发。
- 登录/刷新遇到并发改密不升级凭据；完整旧实体写回不能恢复旧哈希；密码条件更新冲突不覆盖新密码。
- 后端完整测试通过；不触发 Maven 前端构建，不读取 `.env` 或运行中的私密数据。
