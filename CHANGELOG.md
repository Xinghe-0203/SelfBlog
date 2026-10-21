# 更新日志 (CHANGELOG)

所有重要更改都将记录在此文件。

---

## 未发布 — 新增 `release/` 目录，随源码提交可运行 JAR (2026-10-21)

### 说明

此前 `backend/target/` 与 `*.jar` 都在 `.gitignore` 里（构建产物不进版本控制的默认策略），
克隆仓库后必须自备 JDK 21 + Maven + Node 才能得到可运行产物。按需求新增 `release/` 目录，
把一次真实构建的 fat JAR 与源码一起提交，使没有构建环境的机器也能直接取用。

### 变更

- **新增 `release/selfblog-2.0.26.jar`**（68,440,532 B，SHA-256
  `81D2298713DA69B7634CCE7D8392E34F6D3C54B317EDDBE43D5DA006040DB247`）——Spring Boot 单 JAR，
  内嵌 Tomcat、全部后端依赖与已构建的 Vue 前端（`BOOT-INF/classes/static/` 337 个条目、
  8 个 mapper XML）。
- **新增 `release/README.md`**：说明该 JAR 的内容、运行方式（须与 `.env` 同目录）、
  与当前提交源码的对应关系、重建命令与 SHA-256 校验方式。
- **`.gitignore` 未改动**：`backend/target/` 与 `*.jar` 仍被忽略，发布副本是**显式**放进
  `release/` 的独立文件，不改变「构建产物默认不入库」的既有约定。

### 验证

重新走完整构建链后再复制，而非复用旧产物：

1. `frontend`：`npm run build` → `✓ built in 3.39s`（75 个文件）。
2. `backend`：`./mvnw package -DskipTests -Dskip.npm=true -Dskip.installnodenpm=true`
   → BUILD SUCCESS，`backend/target/selfblog.jar` 68,440,532 B。
3. **真机冒烟**（`java -jar` 起服务于 :18825，真实 SQLite 文件库，默认 profile）：
   `/actuator/health` 200、`/` 200 且 HTML 含 `seo-reading`（6079 B）、`/feed.xml` 200
   （5521 B）、`/api/post/list?page=1&size=3` 200（4227 B）、`/seo-reading.css` 200。
4. 复制到 `release/` 后比对 SHA-256，与源产物**逐字节一致**。

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `release/selfblog-2.0.26.jar` | 新增（构建产物） |
| `release/README.md` | 新增 |
| `README.md` | 新增「直接取用预构建 JAR」小节 |
| `CHANGELOG.md` | 新增条目 |

### 注意

JAR 与源码不会自动同步：源码变更后需重新构建并替换本目录下的文件，同时更新 `README.md`
里的版本号、大小与 SHA-256。GitHub 单文件上限 100 MB，当前 68 MB 在限内；继续增长到接近
上限时应改用 GitHub Releases 附件而非入库。

---

## 未发布 — 全仓深度排查：后端正确性/并发/安全加固 + 前端正确性/死代码清理 (2026-10-21)

### 说明

一次覆盖 `backend/` 与 `frontend/` 的全量缺陷排查，目标是「按证据修，不按猜测改」。
先产出三份只读审计（后端正确性 319 行、后端安全 772 行、前端正确性 210 行），
每一条发现都附「已尝试证伪」的过程；再由两轮修复批次落地，每项都补了能失败的回归测试。
后端测试从 **747 → 881（+134）**，前端 **43 文件 / 438 → 45 文件 / 444 tests**。

按严重度收敛出的根因有三类，值得单独记住（见下方「承重规则」）：

1. **MyBatis-Plus 的逻辑删除让一批「看起来对」的写法静默失效**——`updateById` 既写不了
   `is_deleted`，也读不到已软删的行。匿名点赞的「取消」、举报核实的「下架」、
   删除文章后的「计数器归零」都因此是空操作，而代码本身读起来毫无问题。
2. **`synchronized` + `@Transactional` 是错的组合**——锁在代理内层、事务在代理外层，
   锁先释放、事务后提交，两个请求都能走到「没有行 → 插入」分支。改为依赖数据库
   UNIQUE 约束 + 事务内重读仲裁。
3. **异常会污染共享事务**——类级 `@Transactional(readOnly = true)` 下的 404
   （`BusinessException`）会把事务标成 rollback-only，外层若也在事务里，提交时抛
   `UnexpectedRollbackException`，真正的 404 变成 500。

### 后端修复

- **SQL LIKE 通配符泄漏（可注入式全表匹配）**：搜索关键词里的 `%` / `_` 被原样拼进
  `LIKE`，`keyword=%` 返回全站数据。新增 `utils/SqlLikeUtils`，把 `!` 作为转义符
  （`ESCAPE '!'`），21 处关键词查询全部改为 `LIKE {0} ESCAPE '!'` + 逐字符转义。
  **转义符必须是 `!` 而不是反斜杠**：`'\'` 与 `'\\'` 在 MySQL 和 H2/SQLite 之间没有
  通用字面量写法，各自只能覆盖一半方言；`!` 三方通用且 MySQL 允许任意单字符 ESCAPE。
- **限流配额被路径变量绕过**：计数键是 `ip + ":" + path`，`/api/post/10`、`/api/post/11`
  各占一个桶，换 id 即重置配额。改为按**命中的最长前缀**归桶，并用
  `pathDepthAfter()` 保留「一级子路径」的区分度（`/api/guest-comments/post/{id}` 的读取
  不能吃掉创建配额）。另加 `/api/admin/`（60/min）与 `/api/`（60/min）两条兜底前缀，
  新接口不再默认不限流。
- **`SiteSettingsServiceImpl` 自调用让缓存彻底失效**：类内 `this.getSettings()` 绕过
  Spring 代理，`siteSettingsCache` 从不被填充——访客埋点每来一次就全表读一次
  `site_setting`。加 `@Lazy SiteSettingsService self` 走代理。
- **`FineGrainedLockManager` 会把手里的 monitor 淘汰掉（CRITICAL）**：`locks.size() >= 10000`
  时 LRU 移除**正被 `synchronized` 持有**的条目，随后 `compute()` 为该 key 新建 `Object`，
  两个线程拿到不同 monitor 同时进入临界区。改为不做淘汰（key 由数据库主键推导、规模有界）。
- **`synchronized` 锁先于事务提交释放**：`BlogLike` / `BlogCollect` / `Follow` /
  `AnonymousLike` 四处都是「锁在方法上、事务在代理外层」，并发下重复插入。
  改为去掉 `synchronized`，依赖数据库 UNIQUE 约束 + 事务内重读仲裁，并把
  `catch (DataIntegrityViolationException)` 放宽为 `catch (DataAccessException)`
  （SQLite 抛的是 `UncategorizedSQLException`，原先漏接直接 500）。
- **`updateById` 写不了 `is_deleted`**：`AnonymousPostLikeMapper` 新增显式
  `logicalDeleteById` / `logicalRestoreById` / `selectRawByPostAndVisitor` 三条原生 SQL，
  匿名点赞的「取消 / 恢复」才真正生效（此前 `findRow` 被自动追加 `is_deleted = 0`，
  恢复分支永不可达）。
- **举报核实不下架文章**：`ReportServiceImpl` 把举报标 RESOLVED 却只级联软删点赞/收藏、
  递减话题计数、通知举报人「已核实」。改为用显式 `LambdaUpdateWrapper` 软删目标文章。
- **软删除后清零计数器必然失效**：`removeById` 之后再 `update` 会被追加
  `AND is_deleted = 0` 而匹配 0 行。`deletePost` / `adminDeletePost` 改为**先**清零五个
  计数器、**再**软删，并抽出共享的 `cascadeSoftDeletePostChildren()` 级联评论/点赞/收藏/标签。
- **部分更新清空全部标签**：`tagIds` 为 `null`（没传）与空列表（明确清空）原先不可区分，
  且无条件删除在判空之前。改为 `tagPayloadPresent` 判定：`null` = 不改标签、空列表 = 清空。
- **缓存从不失效 / 从不填充**：`adminDeletePost` / `approvePost` / `rejectPost` 补
  `@CacheEvict`；`PostQueryServiceImpl` 的两个 `@Cacheable(CATEGORY_CACHE)` 因自调用从不填充，
  加 `@Lazy PostQueryService self`；`NotificationServiceImpl` 的通知列表缓存四处驱逐全错，
  新增第 9 个具名缓存 `notificationListCache` 并改用 `@Caching(evict = {...})`
  （Spring 6 的 `@CacheEvict` 不是 `@Repeatable`，不能叠加两个）。
- **Circle 读路径开写事务**：`getPostDetail` 是 `@Transactional(readOnly = true)` 却在里面
  自增浏览数。第一版把自增拆到独立短写事务（`TransactionTemplate` + `REQUIRES_NEW`），
  **这个修法本身有缺陷**：调用方（或测试事务）持有该行未提交写锁时，内层新事务会一直等锁，
  真实 MySQL 8 上 50s 后抛 `CannotAcquireLockException`（CI 的 MySQL job 因此红灯；H2 因
  锁语义宽松而假绿），且每次详情请求占两条池连接。最终改为与文章详情同形——自增移出读路径，
  由 `CircleController` 在读取之前 best-effort 调用新增的
  `CircleInteractionService.incrementViewCount`，读取本身恢复为纯只读、不写库、不开写事务；
  同时修掉 `viewCount` 为 null 时的拆箱 NPE。全仓 `REQUIRES_NEW` / `PROPAGATION_` 现为零。
- **`Page.total` 与 `records` 不同源**：评论（只取根评论却用含回复的 total）、点赞、
  收藏、关注（过滤被封禁用户）、热门内容（过滤未发布）五处都是「分页后再在 Java 里过滤」，
  客户端页数永远偏大、深页可能空页却仍报 total。改为让 total 与 records 走同一谓词。
- **`TrendingServiceImpl` 的 total 被取数窗口封顶**：`fetchSize = max(pageSize*2, 100)` 之后再
  内存合并，任何超出窗口的页码都空页。改为显式窗口 + 真实 total + 页码钳制，
  并把 `.last("LIMIT " + fetchSize)` 的字符串拼接一并收口。
- **Circle 点赞的 catch 分支会反转并发赢家的操作**：插入失败后重查若命中赢家的活跃行，
  原实现把它逻辑删除并报 `unlike`，等于把别人的点赞取消掉。
- **媒体删除/上传的事务边界**：`deleteMedia` 原先在事务内**先删文件再删行**，行删失败回滚后
  文件已不可恢复。改为先删行、文件在 `afterCommit` 里删（`TransactionSynchronizationManager`），
  删除失败记 WARN 而非静默。`uploadFiles` 原先把整批文件包在一个事务里，第 3 个失败会让
  前两个的行回滚而字节留成孤儿；改为每个文件各自短事务 + `compensateUploaded()` 补偿清理。
- **类级 `readOnly` + 内层 404 = 500**：`PageServiceImpl` 与 `SitemapService` 去掉类级
  `@Transactional(readOnly = true)`，读方法完全不开启事务（照 `ReadingPageService` 的先例）。
  反向验证：把注解加回去，`UnexpectedRollbackException: Transaction rolled back because it has
  been marked as rollback-only` 立刻复现。
- **`comment_count` 相对减法会静默漂移**：`UPDATE ... comment_count = comment_count - #{count}
  WHERE comment_count >= #{count}` 在计数偏低时匹配 0 行，越漂越高。改为
  `BlogCommentMapper.countActiveByPostId()` 取真实行数后**写绝对值**；相对减法语句标
  `@Deprecated` 保留（接口是公开面，删除属 API 破坏）。
- **H2 别名折叠导致统计接口 500**：`SELECT DATE(create_time) as date, COUNT(*) as count`
  在 H2 下未加引号的别名被折成大写，`map.get("date")` 拿到 null → NPE。两处改为
  `as "date"` / `as "count"`（SQLite/MySQL 保留小写，所以生产从未暴露）。
- **其余**：控制器上的 `@Transactional` 全部移入 service（`AdminTopicController` 净删 63 行）；
  点赞/收藏列表的分页参数拆箱与负偏移钳制；circle 计数器自增补 `CASE WHEN x < 100000000` 夹子。

### 后端安全

- **SVG 消毒器从黑名单反转为白名单**：原先只丢 `script/foreignobject/iframe/embed/object`
  并拦 `on*`，`<style>` 文本、`<use href="https://evil/x.svg#y">`、SMIL
  `<animate attributeName="onload">` 全部存活；而 SVG 由 `/uploads/site-assets/**` **同源**提供、
  无 CSP、无 `Content-Disposition`，构成存储型 XSS 可读 `localStorage` 里的 JWT。
  改为 `ALLOWED_SVG_ELEMENTS` / `ALLOWED_SVG_ATTRIBUTES` 白名单重建，`href` 只接受同文档
  `#id`；并新增 `SiteAssetSecurityHeaderInterceptor` 对该前缀下发
  `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; sandbox` + `nosniff`。
  **不做全局 CSP**：Vue SPA 依赖内联样式与构建脚本，全局 `default-src 'self'` 会白屏。
- **`X-Forwarded-For` 可伪造（限流与审计同时失效）**：`LogUtils.getClientIp` 原先信任一切
  私有网段（`10.*`、`192.168.*`、`172.16-31.*`），而 `backend/DEPLOY.md` 推荐的 Nginx 用
  `$proxy_add_x_forwarded_for`（**追加**而非覆盖）、`docker run -p 8825:8825` 的直连对端是
  网桥网关 `172.17.0.1`，两者都落在「可信」范围内，于是任何客户端都能自选 IP 绕过 10/min
  登录桶并污染审计日志。改为显式 CIDR 白名单 `TrustedProxyMatcher`
  （`app.trusted-proxies` / `TRUSTED_PROXY_CIDRS`，默认**只信任环回**），只做 IP 字面量匹配、
  不做 DNS；无效条目跳过、全部无效时回退环回而非「全信任」。
- **密码重置验证码明文写日志且发信失败后仍有效**：发信回退链末端
  `log.info("📧 邮件发送失败，验证码已生成：{}", code)` 把码写进日志，而调用方只
  `catch (MailException)`，抛出的 `BusinessException` 绕过清理 → 日志读者可用该码
  `PUT /api/auth/password/reset-password` 接管管理员。改为 `catch (Exception)` 后
  **先清 `verificationStore`/`sendTimeStore` 再重抛**，日志只留 `correlationId` 与掩码邮箱；
  「把码打到控制台」的开发行为改为 `app.mail.dev-console-code` 显式开启（默认 false）。
- **未认证邮箱枚举**：`sendCode` 对未注册邮箱回 400「该邮箱未注册」。改为恒返回中性文案、
  只对已注册地址发信；同时补 `/api/user/send-code` 之外的限流覆盖（该路径原先落不到 3/min 桶）。
- **重置密码不清登录锁定**：被锁定的账号重置成功后仍无法登录（且自助路径可用来探测账号状态）。
  两条重置路径在 CAS 写事务内清锁，自助路径对 `status != 1` 与仍锁定账号统一回中性 403。
- **图片解压缩炸弹**：上传只看前 12 字节 magic 与字节数。新增 `ImageDimensionUtils`
  （`MAX_IMAGE_PIXELS = 50_000_000`）用 `ImageIO.getImageReaders` + `getWidth/getHeight`
  读声明尺寸、**从不 `read`**，在 `ImageIO.read` 与落盘之前拒绝；相册原图另有一道头部检查。
  已知限制：JDK 无 WebP reader，无 reader 时**放行**到既有 magic/字节上限检查（不是拒绝）。
- **访客/IP 假名哈希复用 JWT 密钥**：原先是 `SHA-256(JWT_SECRET + "|" + value)` 单轮无盐，
  且 `@Value("${JWT_SECRET:local-visitor-hash}")` 有静默默认值，管理端还展示前 12 位十六进制
  作为离线枚举 IPv4 的 oracle。改为从 `VISITOR_HASH_SECRET`（缺省回落 `JWT_SECRET`）派生
  HMAC-SHA256 子密钥再哈希，`VisitorHashKeyConfig` 启动时校验（缺失/空白即启动失败）。
  无 schema 迁移：升级前的行保留旧哈希，老访客会被当作「新访客」一次，属有意接受。
- **头像/封面 URL 接受协议相对地址**：`//evil.com/x` 以 `/` 开头能过校验。改为只接受单个
  `/` 开头的同源相对路径，`//evil.com`、`/\evil.com`、`/%2F...` 一律 400。
- **异常文本回显**：媒体/站点资源上传把底层异常 `getMessage()` 直接放进响应体。改为通用文案
  并在服务端记原因。
- **全局响应头**：补 HSTS（1 年 + `includeSubDomains`）与 `X-Content-Type-Options: nosniff`。
- **只记录不修（已接受的限制）**：refresh/logout 吊销只在 JVM 内存（改密码路径是持久的）；
  游客评论自动公开；私密相册令牌走查询串；`ENV_VALIDATION_ENABLED=false` 时的遗留默认口令；
  视频分支按客户端 `Content-Type` 选路；多实例下的限流与锁语义。

### 前端修复

- **API 拦截器在无 request config 时抛 TypeError**：重试分支读 `originalRequest.url` 前加守卫，
  无 config 时直接 reject 原错误。
- **文章详情「加载更多」失败会清空已加载评论并推进页码**：改为仅在成功后推进游标。
- 新增 `composables/useLatestRequest.ts`（单调 token 守卫），统一修掉搜索 / 首页 / 标签 /
  归档 / 后台列表 / 访客页的「慢响应覆盖新响应」竞态；归档页另加按 id 去重。
- **Toast 进度条永远停住**：`updateProgress` 停止重排时没把 `animFrame` 置 null，
  `!animFrame` 守卫永久为假，后续 toast 渲染出冻结的 100% 进度条。
- **启动失败时挂着没有 router 的 app 挂载**：`bootstrap()` 的 `.catch` 直接 `app.mount()`
  导致 `<router-view>` 抛错、页面全白。改为回退前先装 router。
- **后台删除后的分页错乱**：标签 / 文章 / 评论删除后不重算 `totalPages`，`changePage` 无边界守卫。
- **`formatRelativeTime` 用 30 天当一个月**：改为真实日历月/年差值。
- **主题持久化未校验**：非法 skin 值会写进 DOM；并修掉双 rAF id 只记一个的泄漏。
- **登录/后台入口的 `redirect` 校验可被 `//evil.com` 绕过**：改用 `router.resolve()` 断言
  解析后的路径确实在后台命名空间内，并 `await router.push`。
- **`ErrorFallback()` 返回 `{ template: ... }`**：构建产物是 runtime-only 的 Vue，
  该模板会渲染空白。改为 `h()` 渲染函数。
- **`useMarkdownEnhance` 的 `revertTimer` 未在节点移除时清理**。
- **`App.vue` 永久把根元素设为 `scroll-behavior: smooth`**：此后所有程序化滚动都变成动画，
  即使 `prefers-reduced-motion`。改为只在路由过渡期间生效。
- **路由守卫对同页 hash 跳转误报 WARN**：TOC 点击被记成「导航失败」，改为只在真正失败时告警。

### 前端死代码清理

零引用即删（每条都附 grep 证据）：

- 整文件：`components/common/{Button,Input,Tag}.vue`、`views/post/{HotPosts,PostSearch}.vue`、
  `api/{share,trending}.ts`。
- 死导出：`utils/index.ts` 的 `formatNumber`/`copyToClipboard`/`generateId`/`debounce`/`storage`、
  `useSeoMeta.ts` 的 `resetSeoMeta`、`constants/index.ts` 八个未用常量、
  `stores/user.ts` 的 `detectTokenTheft` 与 unread 表面、`guards.ts` 的 `meta.guest` 死分支。
- 死 CSS：`animations.css` 等全局样式表里能证明类名 token 零引用的规则；
  动态拼接类名（`toast-${type}`、`level-${level}`、`status-${status}`）、Vue `<Transition>` name、
  运行时注入的 `.code-block*`/`.heading-anchor` 一律保留。
- **保留**：`api/message.ts`、`api/notification.ts`（后端社区接口的管理端入口，模块仍在用）、
  `backend/src/main/resources/fix-counter-sync.sql`（CHANGELOG 记载的一次性运维脚本，非代码）。

### 承重规则（改动前请先读）

1. **`updateById` 永远写不了逻辑删除列。** MyBatis-Plus 执行 `UpdateById` 时把逻辑删除列从
   SET 子句过滤掉，同时往 WHERE 追加 `is_deleted = 0`。所以
   `entity.setIsDeleted(1); mapper.updateById(entity)` 是空操作；软删之后的
   `update().set(...)` 也匹配 0 行。软删必须用 `LambdaUpdateWrapper` 或显式原生 SQL；
   软删后要改的派生值必须在软删**之前**写完。
2. **不要用 `synchronized` 保护带事务的写。** 锁在方法上、事务在代理外层，锁先释放、
   事务后提交，两个请求都能进入「没有行 → 插入」分支。正确形状是数据库 UNIQUE 约束 +
   事务内重读仲裁，并且 catch `DataAccessException` 而不是 `DataIntegrityViolationException`
   （SQLite 抛 `UncategorizedSQLException`）。
3. **不要给「会抛 404」的读服务加类级 `@Transactional`。** 任何 RuntimeException 都会把共享事务
   标成 rollback-only，外层若也在事务里，提交时抛 `UnexpectedRollbackException`，404 变 500。
   照 `ReadingPageService` / `PageServiceImpl` / `SitemapService` 的先例：读方法不开事务。
4. **SQL LIKE 的关键词必须转义并显式 `ESCAPE`。** 统一用 `SqlLikeUtils.ESCAPE_CHAR = '!'`，
   不要用反斜杠（方言字面量不通用），也不要省略 `ESCAPE` 子句（省略后转义静默失效）。
5. **限流计数键按命中的前缀归桶，不按具体路径。** 按路径计数会让 `/api/post/{id}` 的每个 id
   各占一个桶，配额形同虚设。
6. **`X-Forwarded-For` 只在 TCP 对端位于 `app.trusted-proxies` 时才可信。** 该判断是限流、
   审计日志与访客哈希的唯一共享来源，改动前先看 `LogUtilsTest`。
7. **SVG 资产只走白名单。** 黑名单已被 `<style>` / `<use href>` / SMIL `<animate>` 证明可绕过。
8. **媒体文件的磁盘 I/O 不要在事务内做。** 删除要 `afterCommit`，批量上传要每个文件各自事务 +
   失败补偿，否则回滚后要么行指向不存在的文件、要么留下无主字节。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/src/main/java/com/selfblog/utils/{SqlLikeUtils,TrustedProxyMatcher,ImageDimensionUtils}.java` | 新增 |
| `backend/src/main/java/com/selfblog/config/{VisitorHashKeyConfig,web/TrustedProxyConfig,web/SiteAssetSecurityHeaderInterceptor}.java` | 新增 |
| `backend/src/main/java/com/selfblog/config/{web/RateLimitInterceptor,web/RateLimitProperties,web/WebMvcConfig,security/SecurityConfig,cache/CaffeineCacheConfig}.java` | 修改 |
| `backend/src/main/java/com/selfblog/utils/{FineGrainedLockManager,LogUtils,VisitorHashUtils}.java` | 修改 |
| `backend/src/main/java/com/selfblog/service/**`（约 30 个 service impl） | 修改（正确性/并发/安全/事务边界） |
| `backend/src/main/java/com/selfblog/mapper/**`（8 个 mapper） | 修改（显式逻辑删除、绝对值计数、计数夹子） |
| `backend/src/main/resources/mapper/{BlogPost,BlogTrending,CirclePost,SysUser}Mapper.xml` | 修改（删死语句、别名加引号、计数夹子） |
| `backend/src/main/resources/application.yml` | 修改（`app.trusted-proxies`、`app.mail.dev-console-code`、访客哈希密钥） |
| `backend/.env.example`、`backend/DEPLOY.md` | 修改（新环境变量与可信代理说明） |
| `backend/src/main/java/com/selfblog/controller/circle/CircleController.java` | 修改（浏览量自增移出只读读路径，与文章详情同形） |
| `backend/src/main/java/com/selfblog/service/circle/{CircleInteractionService,impl/CircleInteractionServiceImpl,impl/CircleQueryServiceImpl}.java` | 修改（新增 `incrementViewCount`；详情恢复纯只读，删除 `REQUIRES_NEW` 自增） |
| `backend/src/test/java/**`（约 25 个测试类，含 13 个新增） | 新增/修改（+134 tests） |
| `frontend/src/api/index.ts`、`composables/useLatestRequest.ts` | 修改/新增（重试守卫、竞态 token） |
| `frontend/src/views/**`（约 25 个视图与测试） | 修改（竞态、分页、错误路径） |
| `frontend/src/{App.vue,main.ts,router/guards.ts,router/helpers.ts}` | 修改（滚动、启动顺序、redirect 校验、错误组件） |
| `frontend/src/components/common/{Button,Input,Tag}.vue`、`views/post/{HotPosts,PostSearch}.vue`、`api/{share,trending}.ts` | 删除（零引用） |
| `frontend/src/styles/*.css` | 修改（删除可证明零引用的规则） |
| `docs/agents/backend.md`、`docs/agents/frontend.md`、`frontend/CLAUDE.md` | 修改（新增承重约定与基线） |
| `AGENTS.md`、`CLAUDE.md` | 修改（测试基线更新） |

### 验证

- 后端全量：`Tests run: 881, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`
  （基线 747 → 881，+134；日志 `backend/target/gate-mysqlfix.log`）。
- 前端六闸门全绿：`lint:check` **0 errors / 39 warnings**（死代码清理后从 43 降）、
  `format:check` 通过、`type-check` 0、`test:run` **45 files / 444 tests**、
  `test:deployment` 27/27、`build` 通过。
- 每一条 HIGH/CRITICAL 修复都配了**能失败的**回归测试；两处做了反向验证（把修复撤掉后
  测试必须红）：
  - `NotFoundInsideTransactionTest`：把类级 `@Transactional(readOnly = true)` 加回去，
    `UnexpectedRollbackException: Transaction rolled back because it has been marked as
    rollback-only` 复现（`PageServiceImpl` 3/4 用例、`SitemapService` 1/1）。
  - `Toast`「重新起进度循环」：撤掉 `animFrame = null` 后断言失败
    （`expected 2 to be greater than 2`）。
- 关键行为的实机取证（后端跑在 :8825 上）：
  - `GET /api/post/search/advanced?keyword=%25` 修复前 `total=15`（全站 15 篇）→ 修复后只命中
    含字面 `%` 的文章；`keyword=_` 同理。
  - 对 `/api/post/10`、`/api/post/11`、`/api/post/12` 各刷 12 次：修复前**每个 id 各自**
    第 11、12 次才 429（换 id 即绕过），修复后按前缀桶累计。
  - SQLite 3.50.4 上单独实证 `ESCAPE '!'` 语义：`'%'` 只命中字面百分号、`'_'` 只命中字面下划线、
    `'!'` 命中 `a!b`、`'\\'` 命中 `back\slash`（反斜杠无需转义）；省略 `ESCAPE` 子句时
    `%!%%` 仍 count=1，证明 `ESCAPE_CLAUSE` 必须随语句一起带。
  - H2 2.1.214 / 2.2.224 / 2.3.232 七个 MODE 下验证未加引号的别名被折成大写（C7/F-9 的根因）。
- **真实 MySQL 8 复核过一次并抓到真缺陷**：首次推送后 CI 的 `Backend tests (MySQL 8)` job
  红灯，唯一失败点是 `CircleQueryServiceImplTest` 两条详情用例各挂 50.03s / 50.02s 报
  `MySQLTransactionRollbackException: Lock wait timeout exceeded`——即上面那条
  `REQUIRES_NEW` 修法的自死锁。H2 job 同时是绿的（假阴性），说明**这类跨连接锁问题只有
  真实 MySQL 会暴露**。修复后已重跑后端全量 881 tests 全绿。
- **未验证**：Docker 镜像 job 与 MySQL job 的最终结论需推送后由 GitHub Actions 实际跑
  （首轮已观察到 Docker job 绿、MySQL job 红）；多实例部署下的限流与锁语义仍是单实例设计
  （已在类注释与本文档标明）。

---

## 未发布 — 首页封面面板与全站字阶收敛 (2026-10-20)

### 说明

两项长期存在但一直没被测试钉住的观感缺陷：

1. **首页在没有封面图时留出一块 43% 宽的空灰矩形。** `.intro-visual` 面板自己刷了
   `background: var(--bg-tertiary)`，所以只要「封面模块」开着，即使 `<img>` 不存在
   （首页首屏没有任何文章带封面，或封面图加载失败被 `@error` 清空），那块面板照样渲染，
   只在 `::after` 渐变下被部分盖住。
2. **字阶没有任何集中约定。** 全站 402 处 `font-size` 散落在 43 个不同取值上，
   `px` 与 `rem` 两套写法并存且互相漂移（同一层级的小字既有 `12px` 也有 `0.75rem`、
   `0.72rem`、`0.6875rem`），改一处视觉权重只能靠肉眼找。
3. **写文章页在窄视口下横向溢出。** 390px 宽时 `documentElement.scrollWidth` 达 854px，
   整页可左右拖动。根因与字号无关（见下方「顺带修复的既有缺陷」）。

### 修复

- **`Home.vue`**：封面面板的渲染条件从 `showModule('cover')` 收紧为
  `showModule('cover') && heroCover` —— 没有可用封面时整块面板不渲染，`.intro-shell`
  恢复为纯文字卡。`.intro-visual` 自身的灰底保留，因为它在「有封面」时仍是图片的占位底。
- **`Home.test.ts`**：原有「封面加载失败」用例只断言 `.intro-visual img` 消失，
  于是灰底容器漏网。改为断言 **`.intro-visual` 整块消失**（并加一条前置断言确认它在
  失败前确实存在），再翻页确认不会复活。这条断言现在是该缺陷的哨兵。
- **字阶收敛**：`variables.css` 新增 `--text-2xs: 0.6875rem`（11px），把
  `--text-xs` 起的九档补齐为十档；全站 402 处声明里 **325 处被改写**成 token
  （324 处脚本归一 + `useConfirm.ts` 内联样式 1 处），其中 74 处因并列取整方向修正
  而二次改写过，其余 77 处本来就已是 token 写法或是白名单例外。

### 顺带修复的既有缺陷

真机视觉核对时发现 `PostEdit.vue`（后台「写文章」页）在 390px 视口下横向溢出，
`scrollWidth` 854px。**这不是本次字阶改动引入的**：用探针把该页字号整体改回 HEAD 的
`13/15/21px` 后仍是 854，而只加一条 `min-width: 0` 就回到 390（决定性对照见
`docs/reviews/2026-10-20-font-scale-and-hero-visual-verification.md` §3）。

根因是 `PostEdit.vue` 的 `.edit-container` 用了裸 `1fr`（等价 `minmax(auto, 1fr)`），
网格项默认 `min-width: auto` 使 `.editor-main-wrapper` 在窄视口不收缩，于是
`@media (max-width: 768px)` 里 `.toolbar { overflow-x: auto }` 失效、溢出转嫁给整页。
仓库既有惯例（`Home.vue`、`Archives.vue`）都已写 `minmax(0, 1fr)`，`PostEdit.vue` 是漏网者。

修复两处，都在 `frontend/src/views/post/PostEdit.vue`：`:1333` 改为
`grid-template-columns: minmax(0, 1fr) 320px`；`:1429-1431` 给 `.editor-main-wrapper`
补 `min-width: 0`（带注释说明为什么）。范围核对：1440px 下 6 个后台页 `scrollWidth`
全等于视口宽；390px 下修复前只有「写文章」是 854、其余 5 页正常，修复后也归 390，
且 `.toolbar` 恢复为自身横向滚动（`scrollWidth` 820 / `clientWidth` 324），正是原设计意图。

### 承重规则（改动前请先读）

1. **并列一律取更大的那一档。** 13px 距 `--text-xs`(12) 与 `--text-sm`(14) 各 1px。
   第一版脚本在并列时取小档，把 `.btn-primary`、`.content-input`、`.modal-header h3`、
   `.section-heading h2`、`.summary-grid strong` 等 74 处**缩小到原文之下**——
   整合不得静默缩小文字。修正后的归属：`13px → --text-sm`、`15px → --text-base`、
   `17px → --text-lg`、`19px/21px/22px → --text-xl/--text-2xl`。
2. **媒体查询里的字号必须与主值一起归位。** `PostDetail.vue` 的 `≤600px` 断点、
   `Home.vue` 的 `≤600px` 断点内都有独立 `font-size` 声明，只改主值会让移动端与桌面端脱钩。
3. **只留三类非 token 写法**，其余一律 token：`base.css` 的 `html { font-size: 16px }`
   （rem 基准）、正文代码与标题锚点的 `em`（需相对父级缩放）、首页与阅读页大标题的
   `clamp()`（需随视口流动）。`useConfirm.ts` 的内联 `0.9rem` 已折为 `var(--text-sm)`。
4. **字阶是十档，唯一定义处 `variables.css`。** 新增档位要连注释一起加，不要就地写魔数。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `frontend/src/styles/variables.css` | 修改（新增 `--text-2xs: 0.6875rem`） |
| `frontend/src/views/Home.vue` | 修改（封面面板渲染条件收紧 + 自身字号归 token） |
| `frontend/src/views/__tests__/Home.test.ts` | 修改（封面失败断言改为整块面板消失） |
| `frontend/src/composables/useConfirm.ts` | 修改（内联 `0.9rem` → `var(--text-sm)`） |
| `frontend/src/**`（其余 40 个文件） | 修改（402 处 `font-size` 归一到 `--text-*` 字阶） |
| `frontend/src/views/post/PostEdit.vue` | 修改（字号归 token；另修既有移动端横向溢出：`minmax(0, 1fr)` + `.editor-main-wrapper { min-width: 0 }`） |
| `frontend/CLAUDE.md` | 修改（新增「字号一律走 `--text-*` 字阶」约定） |
| `CLAUDE.md`、`AGENTS.md`、`docs/agents/frontend.md`、`docs/agents/backend.md` | 修改（测试基线更新为后端 747 / 前端 43 文件 438 tests） |
| `docs/reviews/2026-10-20-font-scale-and-hero-visual-verification.md` | 新增（真机视觉验证记录 + 6 张对照截图） |

### 验证

- 后端全量：`Tests run: 747, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`。
- 前端六闸门全绿：`lint:check` 0 errors / 43 warnings、`format:check` 通过、
  `type-check` 0、`test:run` **43 files / 438 tests**、`test:deployment` 27/27、`build` 通过。
- 收敛后全仓 `font-size` 普查：只剩 7 处非 token 声明，全部命中上面的三类例外
  （`16px` 根字号、3 处 `em`、2 处 `clamp`、1 处 `inherit`）。
- 真机浏览器核对（Chromium headless，1440x900 与 390x844，覆盖首页有/无封面、
  文章详情、登录页、后台 Dashboard/Visitors/Albums/写文章页）：computed 字号与预期逐条
  一致（`.markdown-body` 17.2→18px、`.article-header h1` 38px、`.section-heading h2`
  19→20px、`.summary-grid strong` 22→24px、`.btn-primary` 15→16px）；无封面首页右侧
  9 个采样点全部落在 `.intro-shell` 的 `--surface-secondary`，灰底面板确认消失。
  证据：`docs/reviews/2026-10-20-font-scale-and-hero-visual-verification.md`。
- 溢出修复前后：390x844 下「写文章」页 `scrollWidth` 854 → **390**（修复后等于视口宽），
  同视口其余 5 个后台页修复前后均为 390；`.toolbar` 恢复为自身横向滚动
  （`scrollWidth` 820 / `clientWidth` 324）。
- **未验证**：真机里的逐页观感仍需人工过目（截图已留证）；字阶改动没有测试断言钉住
  —— 全仓 `*.test.ts` 中没有任何 `font-size` 断言，本轮靠真机 computed 读数兜底，
  后续若要防回退需自建断言。
---

## 未发布 — 外观自定义：后台可换页面背景与组件不透明度 (2026-10-19)

### 说明

作者此前只能选皮肤和强调色，页面背景与卡片通透度都写死在 CSS 里，想换一张背景图必须改代码。
现在「后台 → 网站设置」新增**页面背景**面板，全部外观能力都变成免改代码的设置项。

不改动的默认行为被当作硬约束：作者没碰过任何新设置时，观感与升级前逐像素一致 —— 背景继续跟随
皮肤与深色模式，卡片继续完全不透明。实现上靠「**默认值 = 不写任何 CSS 变量**」达成，而不是写一个
看起来相同的默认值（见下方承重设计）。

### 新增设置项

九条，都存在 `site_setting` KV 表里，**零 schema 迁移**：

| 键 | 取值 | 默认 |
| --- | --- | --- |
| `backgroundType` | `solid` / `gradient` / `image` | `solid` |
| `backgroundColor` | `#RRGGBB` | `#f5f6f8` |
| `backgroundGradientFrom` / `backgroundGradientTo` | `#RRGGBB` | `#f5f6f8` / `#e7eaf0` |
| `backgroundImageUrl` | `http(s)` 或 `/uploads/` 路径，可留空 | 空 |
| `backgroundOverlay` | `0-90`（遮罩浓度 %） | `0` |
| `backgroundFixed` | `true` / `false`（视差固定） | `true` |
| `surfaceOpacity` | `60-100`（卡片不透明度 %） | `100` |
| `surfaceBlur` | `0-40`（卡片背景模糊 px） | `0` |

### 承重设计（改动前请先读）

1. **默认值不产生任何 CSS 变量。** `buildBackgroundVars()` 在纯色等于默认灰 (`#f5f6f8`)、
   或未配置背景图时**不写该键**，`--ap-bg-*` 保持「未设置」；消费点写成
   `var(--ap-bg-color, var(--bg-primary))`，于是皮肤与深色模式继续说了算。
   反过来，如果默认值写成一个相同的颜色，换皮肤就会被这个作者从未设过的默认色压掉。
   **永远不要用空串来「清除」这些变量**：空串是「已设置」，会让 `var()` 的回落失效。

2. **不透明度只覆盖收窄的表面清单**，而不是全局改写 `--surface-primary`。清单（`.card`、
   `.article-list`、`.modal-content`、`.settings-panel` 等约 30 个类）写在
   `frontend/src/styles/appearance.css` 顶部注释里。刻意**不含**输入框、表格行、徽章、按钮：
   它们面积小、边界细，半透明后会与背景糊在一起反而降低可读性。清单里的类名同时被 DOM 与样式
   使用，**改名等于静默失效**。

3. **`appearance.css` 必须在 `main.css` 里最后导入**，否则选 forest/sunset 皮肤时背景不会变。

4. **深色画布撤掉作者的浅色背景。** 深色模式与 `midnight` 皮肤的文字都是浅色，浅底浅字会直接
   不可读。因此 `isDarkCanvas()` 的判据不只是 `.dark-mode` 类，**还包括 `midnight` 皮肤本身**
   （它的 `--text-primary` 就是浅色）。深色下忽略作者的纯色与渐变画布；**背景图保留** ——
   那是作者明确的视觉选择，可读性由强制加厚到至少 45% 的遮罩保证
   （`DARK_CANVAS_MIN_OVERLAY`），且只会在作者设得更厚时更厚，绝不调薄。
   `App.vue` 的 watch 因此把 `themeStore.isDarkMode` 也纳入依赖，切主题时会重算。

5. **首屏不闪烁靠 localStorage 转发，而不是靠再算一遍。** `/api/site/public` 在首次绘制前一定
   拿不到，所以 `applyAppearance()` 会把**算好的最终 CSS 值**按白名单写进 localStorage，
   `frontend/index.html` 的第二个内联脚本只做「零逻辑转发」：按同一份白名单原样
   `setProperty` 到 `<html>`，不拼接任何 CSS 取值。取值组合的唯一事实来源仍是
   `buildBackgroundVars()`。白名单是 `PREPAINT_APPEARANCE_VARS`（`frontend/src/stores/site.ts`），
   存储键 `APPEARANCE_STORAGE_KEY`，两者都有测试钉住与 `index.html` 逐字一致。

6. **背景图地址是 CSS 注入面。** 该值会被拼进 `url("…")`，能闭合引号/括号/反斜杠或截断声明的
   空白都可能撑开注入路径。后端 `SiteSettingsServiceImpl.validateValue` 拒绝这两类字符，前端
   `safeBackgroundUrl()` 再挡一次（前端不能假设数据一定来自本服务端的校验）。

7. **客户端预检不得比服务端更严。** `SiteAssetField.vue` 的 `KIND_RULES.background.maxBytes`
   与后端 `SiteAssetKind.BACKGROUND` 一致为 **8MB**。比服务端严就等于把服务端额度作废，作者会被迫
   把照片压到掉画质 —— 而那正是后端把背景图上限从 2MB 提到 8MB 要解决的问题。

### 修复

- **`SiteSettingsServiceImpl` 新增的 9 个键此前会绕过校验。** 新增 `requireIntegerInRange()`
  做统一的整型区间校验（只接受纯十进制整数，拒绝小数与正负号之外的杂质），
  `backgroundType` 限定三种取值，三个颜色键限定 `#RRGGBB`。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `frontend/src/styles/appearance.css` | 新增（外观层：背景消费点 + 表面清单 + 无障碍降级） |
| `frontend/src/styles/main.css` | 修改（最后导入 `appearance.css`） |
| `frontend/src/stores/site.ts` | 新增（`isDarkCanvas` / `buildBackgroundVars` / `persistAppearance` / `PREPAINT_APPEARANCE_VARS` / `APPEARANCE_STORAGE_KEY`） |
| `frontend/index.html` | 新增（第二个首屏内联脚本：白名单转发外观变量） |
| `frontend/src/App.vue` | 修改（watch 依赖加入 `isDarkMode`，深色切换时重算背景） |
| `frontend/src/views/admin/Settings.vue` | 新增（「页面背景」面板 + 预览 + 9 个控件） |
| `frontend/src/api/site.ts` | 修改（`SiteAssetKind` 增加 `'background'`） |
| `frontend/src/components/admin/SiteAssetField.vue` | 修改（`KIND_RULES.background`，8MB 与后端一致） |
| `backend/src/main/java/com/selfblog/common/enums/SiteAssetKind.java` | 新增（`BACKGROUND`：仅光栅格式，上限 8MB） |
| `backend/src/main/java/com/selfblog/service/site/impl/SiteSettingsServiceImpl.java` | 新增（9 个键的默认值与校验） |
| `frontend/src/stores/__tests__/site.test.ts` | 新增（+27 用例：深色画布、背景变量、持久化） |
| `frontend/src/__tests__/indexHtml.test.ts` | 新增（首屏脚本契约：顺序、白名单、畸形输入、存储抛错） |
| `frontend/src/views/admin/__tests__/Settings.test.ts` | 新增（+5 用例：面板、条件分支、提交、预览隔离） |
| `frontend/src/components/admin/__tests__/SiteAssetField.test.ts` | 新增（+3 用例：背景图 accept 与 8MB 双端一致） |
| `backend/src/test/java/com/selfblog/common/enums/SiteAssetKindTest.java` | 新增（`BACKGROUND` 契约断言） |
| `backend/src/test/java/com/selfblog/service/site/impl/SiteSettingsServiceImplTest.java` | 新增（9 个键的默认值与非法值） |

### 验证

- 后端全量：`Tests run: 747, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`。
- 前端六闸门全绿：`lint:check` 0 errors / 43 warnings、`format:check` 通过、
  `type-check` 0、`test:run` **43 files / 438 tests**（基线 400）、`test:deployment` 27/27、`build` 通过。
- **未验证**：真实浏览器里的首屏无闪烁与背景图观感、真实 8MB 照片的上传链路。
  按仓库既有界限，本机无 MySQL/Docker，这两项需人工在浏览器与 CI 之外确认。

---

## 未发布 — 修复 CI 两处红灯：Dockerfile 探针语法与启动播种幂等 (2026-10-19)

### 说明

`4689309` 推送后 CI（run 39200430765）四个 job 中两个失败，且与本次文档提交无关，是 `f8cc5be` 引入的存量缺陷在 main 上首次被 CI 覆盖：

- **`Docker image smoke test`**：`dockerfile parse error on line 69: Unknown type "CMD-SHELL" in HEALTHCHECK (try CMD)`。
- **`Backend tests (MySQL 8)`**：`Tests run: 669, Errors: 161`，首因是 `DataInitializer` 启动抛 `Duplicate entry 'test-admin' for key 'sys_user.uk_username'`，其余 160 个错误全是 Spring 上下文缓存失败阈值（1）级联跳过。

### 修复

- **`backend/Dockerfile`**：`HEALTHCHECK` 的 `CMD-SHELL` 改为 `CMD`。`CMD-SHELL` 是 **docker-compose** `healthcheck.test` 数组的关键字，Dockerfile 的 `HEALTHCHECK` 只接受 `CMD` / `NONE`；写成 `CMD-SHELL` 直接构建失败。`CMD` 用 shell 形式（非 JSON 数组）时经 `/bin/sh -c` 执行，`${SERVER_PORT:-8825}` 与 `|| exit 1` 都能正常工作，端口覆盖能力不丢。两个 compose 文件里的 `test: ["CMD-SHELL", ...]` 是合法写法，**未改动**。
- **`DataInitializer.initAdminAccount()`**：启动播种改为幂等。原逻辑用 `selectOne(username=...)` 判断存在性，而该查询受 `@TableLogic` 约束只返回 `is_deleted = 0`；一旦作者行被软删除（测试里 `sysUserMapper.delete(null)` 就是逻辑删除），查询误判「不存在」→ 再次 `INSERT` → 撞 `uk_username` 唯一索引 → `CommandLineRunner` 抛异常 → 上下文加载失败。现在改为：
  - 用新增的 `SysUserMapper.selectRawByUsername`（刻意不过滤 `is_deleted`）取原始行；
  - 命中软删除行时调 `SysUserMapper.restoreDeletedById`（`WHERE id = ? AND is_deleted = 1`）恢复该行，**保留作者已设置的密码，不重置**；
  - 命中存活行时沿用原有「旧版示例密码 `Admin123` 一次性升级」分支；
  - 确实不存在时才校验 `ADMIN_PASSWORD` 长度并插入；`insert` 外面兜一层 `catch (DataAccessException)`，捕获到后重新做一次原始查询，确认是并发/多上下文竞态才降级为跳过，否则原样抛出。
  - 兜底刻意捕获父类 `DataAccessException` 而非 `DuplicateKeyException`：H2/MySQL 抛前者子类，SQLite 的约束错误是 `UncategorizedSQLException`，只有父类跨三库可移植。
- **`frontend/scripts/__tests__/docker-config.test.mjs`**：新增两条回归断言 —— 全文件不得出现 `CMD-SHELL`，且 `HEALTHCHECK` 后必须是 `CMD curl -f` 形式。这条闸门以后能在 CI 之前拦住同类语法错误。

### 验证

- 红证（证明新测试真能拦住该 bug）：把 `DataInitializer.java` 临时还原为 `HEAD` 版本后跑 `DataInitializerIdempotencyTest#reinsertingAfterLogicalDeleteDoesNotFailStartup`，得到 `DuplicateKeyException` + `Tests run: 1, Failures: 1`，与 CI 的 MySQL 失败同因；恢复修复版后同测试通过。
- 后端全量：`Tests run: 673, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`（基线 669 + 新增 4）。
- 前端六闸门全绿：`lint:check` 0 errors/43 warnings、`format:check` 通过、`type-check` 0、`test:run` 43 files / 400 tests、`test:deployment` 27/27、`build` 5.81s。
- 未验证（本机无 Docker、无 MySQL 客户端）：真实镜像构建与真实 MySQL 8 上的全量测试，需由 CI 的 `docker` 与 `mysql` job 复核。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/Dockerfile` | 修复（HEALTHCHECK `CMD-SHELL` → `CMD`） |
| `backend/src/main/java/com/selfblog/config/db/DataInitializer.java` | 修复（启动播种幂等 + 软删除恢复 + 并发兜底） |
| `backend/src/main/java/com/selfblog/mapper/SysUserMapper.java` | 新增（`selectRawByUsername` / `restoreDeletedById`） |
| `backend/src/main/resources/mapper/SysUserMapper.xml` | 新增（对应两条无 `databaseId` 的跨库语句） |
| `backend/src/test/java/com/selfblog/config/db/DataInitializerIdempotencyTest.java` | 新增（4 个用例） |
| `frontend/scripts/__tests__/docker-config.test.mjs` | 新增（`CMD-SHELL` 与 `HEALTHCHECK ... CMD` 回归断言） |

---

## 未发布 — 仓库清理：删除无用文件与无用文档 (2026-10-19)

### 说明

按「删除全部无用文件和无用文档」的要求，先做全仓盘点取证（跟踪状态、忽略规则、引用计数、真实内容），再删除。**删除范围经用户逐项裁定**：构建产物/依赖、IDE 本地配置、无引用 ADR、历史设计规格与计划四类批准删除；`docs/tutorial/`（约 25.6 万字的正式交付物，被 README/CLAUDE/AGENTS 多处引用）经裁定保留。

### 删除

- **构建产物与依赖（约 291 MB）**：`backend/target/`（166 MB，含 `selfblog.jar`、空库 `campus_blog.db` 544 KB/29 张表全 0 行、`smoke/`）、`frontend/node_modules/`（136 MB）、`frontend/dist/`（695 KB）。全部被 gitignore 忽略且可由 `mvn package` / `npm ci` + `npm run build` 重新生成。
- **本地运行日志**：`backend/logs/`（7 个文件 885 KB，`*.log` 已被忽略）。
- **IDE 本地配置**：`.idea/`、`backend/.idea/`（共约 14 KB，被 gitignore 忽略）。
- **brainstorm 临时状态**：`.superpowers/`（17 个文件 753 KB，含过期 `server.pid` 与明文 `.last-token` 凭据文件）。
- **无引用 ADR**：`docs/decisions/002-single-origin-reading-html-and-proxy.md`（已核实全仓 0 引用，连 CHANGELOG 都未提及）。
- **历史设计规格与计划**：`docs/superpowers/`（20 个已跟踪文件，303 KB）。
- **仓库外临时日志**：`C:\Users\Zora\Downloads\_fegates.log`、`_fegates2.log`（本次前端闸门输出）。

### 修复（删除后的悬空引用）

删除会让文档中的链接失效，故同步修订了全部指向已删路径的**活文档**（CHANGELOG 中的历史条目按「历史记录」保留，不改写）：

- `CLAUDE.md`：文档维护规范第 3 条与「其他指南」段改为「设计稿放 `docs/` 下」并注明历史目录已清理。
- `README.md`：删除「设计与实施历史」表行；版式段落移除失效的设计稿链接；架构决策行去掉已删 ADR 的描述。
- `frontend/README.md`：重写段落，去掉指向已删设计稿的链接。
- `docs/decisions/001-versioned-pages-in-existing-kv.md`：实施计划链接改指仍存在的验证记录。
- `docs/reviews/2026-10-16-{navigation-fidelity-and-fullstack-audit,page-management-verification,seo-deployment-verification}.md`：三处失效规格/计划链接改为文字说明 + 指向 CHANGELOG。
- `docs/tutorial/appendix-b-source-map.md`、`ch07-project-practice.md`、`SelfBlog-全栈开发完全教程.md`：同步移除/改写对已删目录的描述（教程经用户裁定保留，不能留悬空指引）。

### 验证

- 删除前逐项核实：`git ls-files` 确认 8 个目录全部 0 跟踪文件、`git check-ignore` 确认忽略来源；`docs/decisions/002` 与 `docs/superpowers` 用全仓引用检索确认可删/已裁定。
- 删除后全仓检索 `superpowers` / `002-single-origin` / `_fegates`：剩余命中**仅为 CHANGELOG 历史条目与本次新增的「已清理」说明**，无失效链接。
- 未验证/注意：删除 `frontend/node_modules` 后前端六道闸门需先 `npm ci` 才能重跑；`backend/target/frontend/node`（CLAUDE.md 记录的 PATH 兜底 Node）随 `target/` 一并删除，下次需先跑一次 Maven 构建才会重新生成。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `.superpowers/`、`backend/logs/`、`backend/target/`、`frontend/dist/`、`frontend/node_modules/`、`.idea/`、`backend/.idea/`、`docs/superpowers/` | 删除（目录） |
| `docs/decisions/002-single-origin-reading-html-and-proxy.md` | 删除（文件） |
| `CLAUDE.md`、`README.md`、`frontend/README.md` | 修订（移除失效链接与描述） |
| `docs/decisions/001-versioned-pages-in-existing-kv.md` | 修订（链接改指验证记录） |
| `docs/reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md`、`2026-10-16-page-management-verification.md`、`2026-10-16-seo-deployment-verification.md` | 修订（失效规格链接改文字说明） |
| `docs/tutorial/appendix-b-source-map.md`、`docs/tutorial/ch07-project-practice.md`、`docs/tutorial/SelfBlog-全栈开发完全教程.md` | 修订（同步清理后的文档地图） |

---

## 未发布 — 生产日志取证与缺陷修复批次 (2026-10-18)

### 说明

对生产环境五个日志（`application.log` / `application-error.log` / `audit.log` / `performance.log` / `request.log`，2026-10-17 08:14–16:15）做了全量取证，产出四份报告 `docs/reviews/prod-log-{request,application,performance,audit}.md`，并据此修复 10 类缺陷。取证中发现的**运行形态异常**（不是代码缺陷）单独列在下面第 11 条。

### 修复

- **F1 静态资源缺失被兜底成 500**：`GET /favicon.ico` 在生产返回 500 并打完整 ERROR 堆栈（扫描器 IP 触发）。根因是 `GlobalExceptionHandler` 只注册了已被 Spring Boot 3 取代的 `NoHandlerFoundException`，而静态资源缺失抛的是 `NoResourceFoundException`，落进 `@ExceptionHandler(Exception.class)` 兜底。新增 `NoResourceFoundException` → 404（`log.warn`，不打堆栈）。
- **F1 上传部件超限被兜底成 500**：作者批量上传相册时 `MultipartException: Failed to parse multipart servlet request` + `Caused by: FileCountLimitExceededException: attachment` 也落兜底 → 500。新增 `MultipartException` → 400；识别出 `FileCountLimitExceededException` 时给出「单次上传文件数量超过上限」的可读文案。**已用测试锁定 `MaxUploadSizeExceededException`（`MultipartException` 子类）仍走自己的 400 语义，不被新处理器吞掉**。
- **F2 客户端 IP 可被伪造（审计与限流归因）**：`LogUtils.getClientIp` 只要看到 `X-Forwarded-For` 就采信，而 `RateLimitInterceptor` 自己那份实现会先校验直连对端是否受信代理——同一请求在两处得到不同 IP，攻击者可借此污染审计日志。现统一为 `LogUtils` 一份实现（仅当直连对端属 10./192.168./127.0.0.1/::1/172.16-31 才采信代理头），`RateLimitInterceptor` 改为调用它。
- **F2 限流命中零审计**：`RateLimitInterceptor` 返回 429 时没有任何日志，生产 8 小时内 429 在 `request.log` / `audit.log` 中都看不到。现写 `RATE_LIMIT_EXCEEDED` 到审计通道（含 path、method、limit、count）。
- **F2 审计钩子是死代码**：`SecurityConfig.logAdminOperation` 与 `LogUtils.logLogin/logRegister/logUserAction/logFileUpload/logApiCall` 全仓 0 调用点，导致 `audit.log` 里 `ADMIN_OPERATION` 计数为 0、登录成功/失败完全不审计。现 `/api/admin/**` 写操作在鉴权之后记录 `ADMIN_OPERATION`（status<400 视为成功），登录成功/失败接入 `LogUtils.logLogin`。
- **F2 读路径限流阈值过严**：`/api/post/` 前缀的 `postGeneral=10/min` 会命中首页列表 `GET /api/post/list`，一次页面加载就可能触发 429。为列表读路径单列阈值，不比原值更严。
- **F2 热路径重复分配**：`RateLimitProperties.getAllLimits()` 每次调用都 `new HashMap` 并填 34 条，改为构造期一次性构建不可变 Map。
- **F3 生产配置漂移**：生产日志显示 `No active profile set, falling back to "default"`，`application-prod.yml` 全部失效（Swagger/Knife4j 默认开放、management 暴露面不收窄、日志分流不对齐）。`springdoc.enabled` / `knife4j.enable` 默认值由 `true` 改为 `false`——忘记设 profile 不再等于把 API 文档开到公网，本地调试显式设 `SWAGGER_ENABLED=true`。
- **F3 健康检查端口与实际监听不一致**：`Dockerfile` 与两个 compose 的健康检查硬编码 `8825`，而生产实际监听 8888（`SERVER_PORT` 被覆盖），探针必然失败 → 容器被判不健康反复重启（日志中同一 PID 三次 `Starting SelfBlogApplication`、前两次无 `Started`）。现改为 `CMD-SHELL` 读 `${SERVER_PORT:-8825}`，端口与探针同源。
- **F3 静态资源无缓存头**：`WebMvcConfig` 只注册了 `/uploads/**`，`/assets/**` 完全没有 `Cache-Control`，生产日志里同一个哈希命名的产物被重复 200 下载 12–15 次（1300 行里仅 2 次 304）。现 `/assets/**` 用 `max-age=31536000, immutable`（Vite 产物按内容哈希命名）；`/favicon.svg`、`/seo-reading.css` 用 1 天 + `must-revalidate`；`/uploads/**` 用 1 小时（作者可覆盖同名文件，不能 immutable）；`/`、`/index.html`、SPA 壳不动。
- **F3 日志分类失效**：`default` 段把请求/性能/审计/SQL 同时写 stdout 与文件；`audit.log` 里混入 `[PERFORMANCE]` 记录（性能事件走了 `LogUtils` 自己的 logger → `ASYNC_AUDIT`）。现按段收敛 stdout 输出，并把 `logPerformance` 改为写专用 logger `com.selfblog.performance`（此前该 logger 只在 XML 里存在、Java 侧 0 引用，属死配置），`PERFORMANCE` appender 补 `ThresholdFilter=INFO` 防止 DEBUG 档涌入。
- **F5 SQLite per-connection PRAGMA 丢失**：`synchronous` / `busy_timeout` / `cache_size` / `temp_store` / `mmap_size` / `foreign_keys` 都只在启动时对「当时那一条」连接执行一次；Hikari 默认 30 分钟回收重建连接后 `synchronous` 回落到默认 FULL，每次提交都要 fsync（对应 `performance.log` 里 2451ms 的单条 INSERT）。现通过 `setDataSourceProperties` 让驱动在**每次建立物理连接**时应用，并关闭连接轮换（`maxLifetime(0)` + `idleTimeout(0)`）作防御纵深。**真实验证**：强制连接轮换后读取 PRAGMA，7 项全部保持生效。
- **F5 趋势任务逐条写**：`scheduledUpdateAllTrending` 在循环内逐条 `upsert`（1088ms）。改为批量写，保留 `databaseId` 双语句与 H2 兜底语义。
- **F6 日志注入与敏感参数**：客户端 `X-Trace-Id` 原样进 MDC（可伪造日志行）、查询串原样入日志（可能含 token/password）。现 `X-Trace-Id` 需匹配 `^[A-Za-z0-9_-]{8,64}$` 否则改用服务端 UUID；新增 `sanitizeLogValue` 剥离 CRLF/控制字符并截断；`token/access_token/refresh_token/password/secret/code/key` 等参数值掩码（大小写不敏感），不影响 `?tag=xx` 这类正常参数的可读性。
- **F7 SQLite 单连接串行化**：`maximumPoolSize=1` 把所有请求强制串行。生产日志有决定性证据——同一页面几乎同时发出的三个互不相关的读请求（`/api/post/1/adjacent`、`/api/anonymous-likes/posts/1`、`/api/guest-comments/post/1`）严格串行返回，墙钟≈三者之和（474+802+1074≈2350ms）。现池大小改为 5（WAL 下多读安全；写仍是单写者）。**实测**：3 路并发读墙钟/串行和之比由 1.07 降到 0.44，3 条互不相同的物理连接同时持有且各自 PRAGMA 生效。
- **F8/F9 BCrypt 在数据库事务内执行**：`login`、`changePassword`、两个 `resetPassword` 都在 `@Transactional` 内做 BCrypt（cost=12，本机实测 `matches≈456ms`/`encode≈360ms`），配合池大小 1 就是「登录 1.2–2.7s」的直接机制。四个方法改为「短只读事务读快照 → 事务外 BCrypt → 短写事务提交」。
- **F9 顺带修掉一个生产级安全缺陷**：原 `login` 在事务内递增失败计数后立刻抛异常，计数被回滚——**登录锁定在生产环境实际是失效的**。既有测试没发现是因为测试自身也是 `@Transactional`。现失败计数在独立短事务提交，并有测试锁定。
- **F4 占位图 7.5MB 冗余 + 缺 favicon.ico**：`default-avatar.png`、`default-cover.jpg`、`default-image.png` 三个「不同用途」的文件 SHA256 完全相同（都是 2048×2048 PNG），各 2.5MB。现分别瘦身为 10.8KB / 24.8KB / 17.2KB（-99.3%），语义与引用点不变；新增真实多尺寸 `favicon.ico`（16/32/48）并在 `index.html` 声明，配合 F1 的 404 处理器共同闭合 `/favicon.ico` 链路。

### 运维侧发现（不是代码缺陷，需部署侧确认）

- **生产运行形态与仓库声明不一致**：日志显示 `Starting SelfBlogApplication v2.0.26 using Java 25.0.3`、`No active profile set`、`Tomcat started on port 8888`，而仓库的 `pom.xml` 编译目标是 Java 21、`Dockerfile` 运行镜像是 `eclipse-temurin:21-jre`、两个 compose 声明 `SERVER_PORT=8825` + `SPRING_PROFILES_ACTIVE=prod`。即**生产镜像不是用仓库这份 Dockerfile / compose 构建启动的**（或环境变量被外部覆盖）。这一条无法在仓库内修复，只能由部署侧对齐；仓库侧能做的是把契约显式化（见 F3 的 `SWAGGER_ENABLED` 默认值与健康检查端口跟随 `SERVER_PORT`），并建议用 `docker compose -f backend/docker-compose.yml up -d --build` 重建以消除漂移。
- **凭据扫描探测**：`request.log` 中 IP `34.156.121.46` 在 3.7 秒内发出 42 条请求，其中 40 条为敏感路径字典盲打（25 个 `/.env` 变体 + `phpinfo` / `credentials` / `Dockerfile` / `vendor/composer/installed.json` / Laravel、Matomo、Mautic 配置路径），全部 404；UA 前两条伪装完整 Chrome、其后统一截断。目标栈是 PHP/Laravel/WordPress，与本项目无关，**未发现攻击成功痕迹**。项目没有全局 IP 封禁机制，每次探测都会写一条 `AUTH_ENTRY_POINT` 审计（8 小时内约 90 条）。本次未引入 IP 封禁（避免误伤与状态复杂度），如需可在反向代理层处理。

### 验证

- 后端全量（H2）：**669 项测试，0 失败、0 错误、0 跳过，`BUILD SUCCESS`**（基线 658，本批次净增 11 项）。
- 前端全闸门实测：`lint:check`、`format:check`、`type-check` 全绿，`vitest` **43 文件 / 400 tests 全过**，`test:deployment` **27/27**，`vite build` 成功。
- 关键改动均带**反向红证**（把修复临时回退，确认新测试真的失败，再还原并校验字节一致）：F8 的「事务确实独占连接」（`expected: <0> but was: <1>`）、F9 的失败计数回滚（`login_fail_count expected: <3> but was: <0>`）、F10 的性能 logger 归属、F5 的 PRAGMA 轮换。
- 并发量化证据（真跑，池强制为 1）：BCrypt 进行期间另一线程获取连接并完成查询耗时 0–1ms，活动连接数 0。
- **端到端冒烟（真实 `selfblog.jar` + 真实 SQLite 文件库 + 默认 profile，端口 18825）**：`/favicon.ico` **200**（`image/x-icon`，不再 500）、`/favicon.svg` 与 `/seo-reading.css` 200 + `max-age=86400, must-revalidate, public`、`/assets/*.js|css` 200 + `max-age=31536000, public, immutable`、`/index.html` 308、不存在路径 404、不存在的 `/assets/*.js` **404**（不再 500）。全部请求状态码为 200/308/401/404/429，**零 5xx**。
- **端到端冒烟验证的审计与限流链路**：错误密码登录 → 401 且 `[LOGIN] ... success=false, reason=bad_credentials`；正确登录 → 200 且 `success=true`（账号脱敏为 `a***n`，不再出现明文手机号）；匿名写 `/api/admin/**` → `ADMIN_OPERATION ... status=401, result=rejected, userId=anonymous`；已鉴权写 → `status=200, result=success, userId=1`（只读请求不产生审计行）。限流实测 14 次登录 = 前 6 次 200、后 8 次 429，且每次 429 都有 `RATE_LIMIT_EXCEEDED`（含 `limit=10/min, count=11..`）——此前 429 完全无审计。
- **端到端冒烟验证的日志注入防护**：非法 `X-Trace-Id: bad!trace#id` 被替换为服务端 UUID，合法 `ABCDEFGH12345678` 原样采用；`?token=SUPERSECRETVALUE&access_token=LEAK2&tag=java` 落盘为 `token=***&access_token=***&tag=java`（正常参数不受影响）；带 CRLF 的 traceId 未产生任何注入行（`INJECTED-LINE` 与原始值均 0 命中）。
- 未验证：真实 Docker 镜像构建与容器运行（本机无 Docker CLI/守护进程）、真实 MySQL 实例、真实 Vercel 部署、`prod` profile 的端到端冒烟（冒烟跑的是默认 profile）。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/src/main/java/com/selfblog/common/exception/GlobalExceptionHandler.java` | 新增 `NoResourceFoundException`(404) 与 `MultipartException`(400) 处理器 |
| `backend/src/main/java/com/selfblog/config/web/RateLimitInterceptor.java` | 复用 `LogUtils.getClientIp`；429 写审计 |
| `backend/src/main/java/com/selfblog/config/web/RateLimitProperties.java` | `getAllLimits()` 构造期不可变缓存；列表读路径阈值 |
| `backend/src/main/java/com/selfblog/utils/LogUtils.java` | 可信代理 IP 判定、日志清洗、敏感参数掩码、登录/注册审计、性能专用 logger |
| `backend/src/main/java/com/selfblog/config/security/SecurityConfig.java` | `/api/admin/**` 写操作审计钩子接线；BCrypt cost 启动日志 |
| `backend/src/main/java/com/selfblog/config/security/JwtAuthenticationFilter.java` | 审计上下文与 traceId 预置 |
| `backend/src/main/java/com/selfblog/config/web/HttpRequestLoggingInterceptor.java` | traceId 白名单、日志清洗、敏感查询串掩码 |
| `backend/src/main/java/com/selfblog/config/db/DatabaseConfig.java` | per-connection PRAGMA、连接轮换策略、池大小 1→5 |
| `backend/src/main/java/com/selfblog/service/content/impl/TrendingServiceImpl.java`、`backend/src/main/java/com/selfblog/mapper/BlogTrendingMapper.java`、`backend/src/main/resources/mapper/BlogTrendingMapper.xml` | 趋势批量写 |
| `backend/src/main/java/com/selfblog/service/auth/impl/SysUserServiceImpl.java` | `login`/`changePassword`/两个 `resetPassword` 拆分事务，BCrypt 移出事务；修复失败计数回滚 |
| `backend/src/main/java/com/selfblog/config/web/WebMvcConfig.java` | `/assets/**`、`/favicon.svg`、`/seo-reading.css`、`/uploads/**` 缓存策略 |
| `backend/src/main/resources/application.yml` | multipart 部件上限、`springdoc`/`knife4j` 默认关闭 |
| `backend/src/main/resources/logback-spring.xml` | 三段日志分流收敛、`com.selfblog.performance` logger、`PERFORMANCE` appender 阈值 |
| `backend/Dockerfile`、`backend/docker-compose.yml`、`backend/docker-compose.sqlite.yml` | 健康检查端口跟随 `SERVER_PORT`；prod profile 契约注释 |
| `backend/src/test/java/com/selfblog/common/exception/GlobalExceptionHandlerTest.java` 等 11 个新增/扩充测试类 | 新增回归与并发量化测试 |
| `frontend/public/default-avatar.png`、`default-cover.jpg`、`default-image.png`、`favicon.ico`、`frontend/index.html` | 占位图瘦身 99.3%、新增 favicon.ico 声明 |
| `README.md`、`backend/README.md`、`backend/DEPLOY.md` | 限流/审计/日志注入说明、API 文档默认关闭、prod profile 与端口契约 |
| `docs/reviews/prod-log-request.md`、`prod-log-application.md`、`prod-log-performance.md`、`prod-log-audit.md` | 四份取证报告 |

---

## 未发布 — 缓存正确性与无用代码清理 (2026-10-17)

### 修复

- **文章列表缓存串号（F1）**：`getPostList` 的缓存 key 缺少 `keyword`/`userId`/`tagId`，同一分页/分类/排序下不同关键字、不同作者、不同标签的查询会互相返回对方的结果。key 现补齐三个过滤维度。
- **编辑文章后标签/分类缓存陈旧（F6）**：`updatePost` 的 `@CacheEvict` 漏掉 `categoryCache`，改分类或标签后 `postTags:*`/`tagsByPost:*` 缓存最长 30 分钟不失效。现与 `createPost`/`deletePost` 对齐，三个缓存一起清。
- 前端 `src/api/post.ts` 删除四个无任何调用方的封装（`incrementViewCount`/`searchSuggest`/`getMyPosts`/`getMyDrafts`）；后端公开 API 均保留，浏览计数由详情页内部逻辑处理，不受影响。
- 后端删除无调用方的 `evictStatsCache`（接口 + 实现），统计缓存失效由写操作的 `@CacheEvict` 承担。
- 删除上一次会话遗留的临时笔记文件 `findings.md`/`progress.md`/`task_plan.md`（未入过 git）。

### 验证

- 后端全量（H2）：589 项测试（588 基线 + 新增 2 - 删除 1），0 失败、0 错误、0 跳过，`BUILD SUCCESS`；新增 `PostListCacheKeyTest` 证明不同 keyword/tagId 不共享缓存条目。
- 前端全闸门实测：type-check 0 错误、eslint 0 errors、prettier 全过、vitest 43 文件/400 tests 全过、vite build 成功、部署契约 27/27 通过。
- 未在本机验证真实 MySQL 或部署运行。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/src/main/java/com/selfblog/service/post/impl/PostQueryServiceImpl.java` | 列表缓存 key 补齐过滤维度 |
| `backend/src/main/java/com/selfblog/service/post/impl/BlogPostServiceImpl.java` | updatePost 清除 categoryCache |
| `backend/src/main/java/com/selfblog/service/content/StatisticsService.java`、`backend/src/main/java/com/selfblog/service/content/impl/StatisticsServiceImpl.java` | 删除无调用方的 evictStatsCache |
| `backend/src/test/java/com/selfblog/service/impl/StatisticsServiceImplTest.java` | 移除对应死代码测试 |
| `backend/src/test/java/com/selfblog/service/post/PostListCacheKeyTest.java` | 新增缓存 key 回归 |
| `frontend/src/api/post.ts` | 删除四个无调用方封装 |
| `findings.md`、`progress.md`、`task_plan.md` | 删除会话遗留临时文件 |

---

## v2.0.26 — 评论限流与改密会话安全 (2026-10-17)

### 修复

- 匿名评论创建 `POST /api/guest-comments` 不再因配置的尾斜杠而跳过限流：使用 MVC 解码后的查找路径（去除应用上下文），路径段边界匹配同时覆盖根路径与子路径，最长规则优先；编码同一路径共用计数，保留每 IP 每路径计数和现有窗口，第 6 次创建返回 429。
- 访问与刷新 JWT 增加密码绑定的 `credentialVersion`（域分离 HMAC，不暴露 BCrypt 哈希），每次鉴权/刷新读取当前用户凭据。改密、管理员重置、邮箱重置提交后全部旧令牌失效，包括未登记设备的令牌及服务重启后的令牌；缺失版本、不可用用户、数据库错误均拒绝认证。
- 登录令牌对绑定实际验证的密码快照；刷新与轮换继承旧版本，避免并发改密把旧刷新令牌升级为新凭据。实体更新不再隐式写回密码，三种改密及旧样例密码初始化使用条件更新，防止旧实体覆盖新密码、复活旧会话。
- 升级无需 schema 迁移，升级前令牌需重新登录；多实例必须同时升级。普通登出/轮换的黑名单和限流计数仍是进程内状态，本次不扩展为持久分布式会话。

### 验证

- 最终后端全量（H2）：588 项测试，0 失败、0 错误、0 跳过，`BUILD SUCCESS`；包含限流、JWT、真实 MockMvc 评论请求、改密/两种重置、新鉴权实例、独立 SQL 会话读取已提交密码及旧实体写回回归。
- 最终独立 SQLite 专项：限流与评论/会话集成共 14 项，0 失败、0 错误、0 跳过，`BUILD SUCCESS`。未在本机验证真实 MySQL 或部署运行；本次不涉及前端改动。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/src/main/java/com/selfblog/config/web/RateLimitInterceptor.java` | 路径段匹配与最长规则优先 |
| `backend/src/main/java/com/selfblog/utils/JwtUtils.java`、`backend/src/main/java/com/selfblog/config/security/JwtAuthenticationFilter.java` | 凭据版本、撤销与轮换验证，以及过滤器说明 |
| `backend/src/main/java/com/selfblog/controller/user/SysUserController.java` | 刷新继承已验证版本 |
| `backend/src/main/java/com/selfblog/service/auth/impl/SysUserServiceImpl.java` | 登录快照与三种密码条件更新 |
| `backend/src/main/java/com/selfblog/entity/SysUser.java` | 禁止通用更新写密码 |
| `backend/src/main/java/com/selfblog/mapper/SysUserMapper.java`、`backend/src/main/resources/mapper/SysUserMapper.xml` | 密码比较并更新 SQL |
| `backend/src/main/java/com/selfblog/config/db/DataInitializer.java` | 旧样例密码条件更新 |
| `backend/src/test/java/com/selfblog/config/web/RateLimitInterceptorTest.java`、`backend/src/test/java/com/selfblog/utils/JwtCredentialVersionTest.java`、`backend/src/test/java/com/selfblog/controller/CommentSessionSecurityTest.java` | 新增安全回归 |
| `backend/src/test/java/com/selfblog/utils/JwtUtilsTest.java`、`backend/src/test/java/com/selfblog/service/impl/SysUserServiceImplTest.java` | 按新签发接口调整测试桩和验证 |
| `README.md`、`backend/README.md`、`backend/DEPLOY.md` | 安全行为与升级边界 |
| `docs/superpowers/specs/2026-10-17-comment-session-security.md`、`docs/superpowers/plans/2026-10-17-comment-session-security.md` | 设计证据与实施计划 |

---

## v2.0.26 — P0–P3 加固批次：CI、Maven Wrapper、GFM 一致性与清理 (2026-10-17)

### 说明

- **CI（新增）**：`.github/workflows/ci.yml` 四个真实执行任务——`backend`（Temurin 21 + `./mvnw` + H2 全量）、`frontend`（Node 24.15.0 + `npm ci` + lint/format/type/test/deployment/build）、`mysql`（用 `backend/schema-fresh-install.sql` 初始化真实 MySQL 8 后跑全量后端测试）、`docker`（仓库根上下文构建镜像 → SQLite 起容器 → 等 `/actuator/health` → 抓取服务端渲染首页 `seo-reading`）。本机没有 MySQL/Docker 守护进程，真实 MySQL 与容器运行以 CI 为唯一已验证通道。
- **Maven Wrapper**：`backend/mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties`（only-script，wrapper 3.3.4，Maven 3.9.12，canonical `repo.maven.apache.org` 分发地址）；本地与 CI 共用同一入口，`-D` 参数在 PowerShell 中需加引号。
- **GFM 一致性**：后端 `SafeMarkdown` 增加 commonmark 表格/删除线/任务列表扩展，与前端 `marked` + DOMPurify 输出对齐；两侧共用 fixture `docs/fixtures/gfm-sample.md`（`SafeMarkdownTest` 与前端 `markdownGfm.test.ts` 同时断言）。任务列表复选框在两侧都被安全策略移除，仅保留条目文字。
- **深色首闪**：`frontend/index.html` 增加内联主题引导脚本，在 Vue 挂载与首帧之前根据 `edu_theme` 加 `html.dark-mode`；`seo-reading.css` 暗色改为 `html.dark-mode` 键控（不再用 `prefers-color-scheme`，避免与站内主题相反时二次闪烁）。新增 `src/__tests__/indexHtml.test.ts` 直接执行 `index.html?raw` 的真实脚本。
- **阅读 HTML 缓存**：200 非 SHELL 响应带 ETag + `Cache-Control: public, max-age=0, must-revalidate`，`If-None-Match` 命中返回 304；错误页与 SHELL 保持 `no-store`。回归测试用 Mapper 更新（而非 `JdbcTemplate`），否则 MyBatis 本地会话缓存会掩盖变更。
- **SEO 补强**：首页 JSON-LD 增加 `WebSite` + `SearchAction`；标签站点地图使用已发布文章的 `MAX(COALESCE(update_time, create_time))` 作为 `lastmod`（`BlogPostTagMapper.selectTagLastModified` + 类型化 DTO `TagLastModified`；H2 会把未加引号的别名转大写，不能用 `Map` 读）。
- **前端遗留清理（第二波 25 个文件）**：删除仅有旧社区页面引用的 `api/{circle,collect,comment,follow,like,report}.ts`、`types/{circle,follow}.ts`、`components/common/{CollectButton,FollowButton,LikeButton}.vue`、`views/admin/{Reports,Statistics,Users}.vue`、`views/common/Report.vue` 与 10 个 `views/user/*` 页面；保留 `views/user/PasswordChange.vue` 并在后台设置的“后台与访客”面板新增“修改密码”入口。`api/user.ts`、`stores/user.ts`、`types/comment.ts` 保留。
- **H2 唯一键对齐**：测试 schema 补齐 `sys_user` (username, email)、`blog_circle_like` (user_id, post_id)、`blog_topic` (name) 唯一约束，与 MySQL/SQLite 生产 schema 一致（此前只有 tag/like/collect/follow/post_tag/trending/draft_tag）。
- **部署文件整理**：`backend/schema.sql` → `backend/schema-fresh-install.sql`（含 `DROP TABLE`，改名降低误执行风险），同步 `docker-compose.yml` 与全部文档引用；删除无用的 `backend/entrypoint.sh` 与 `backend/.dockerignore`（构建上下文本来就是仓库根）。
- **`format:check` 修复**：`.prettierrc` `endOfLine: "lf"` → `"auto"`，一次性重写历史漂移文件；`format:check` 成为仓库级真实闸门并加入 CI。
- **AGENTS.md 拆分**：根 `AGENTS.md` 变为入口页（命令快路径、CI、项目级记录），后端/前端详细规则分别移到 `docs/agents/backend.md`、`docs/agents/frontend.md`；同步更新根 `CLAUDE.md`、`frontend/CLAUDE.md`、`README.md`（含 v2.0.26 条目与文档索引）。
- 版本：`frontend/package.json` 2.0.25 → 2.0.26；`backend/pom.xml` 0.0.1-SNAPSHOT → 2.0.26；本文件此前所有“未发布”章节一并归档到 v2.0.26。

### 验证

- 本地实测：后端 `./mvnw.cmd test -Dskip.npm=true -Dskip.installnodenpm=true` = **564 tests, 0 failures**；前端 43 文件 / 400 tests，`lint:check`（0 错误 / 43 警告）、`format:check`、`type-check`、`build`、`test:deployment` 27/27 全绿。
- CI：`mysql` 与 `docker` 任务是本仓库对真实 MySQL 与容器运行的唯一验证通道。
- 仍未验证（不当作已验证）：真实 Vercel 部署、搜索引擎实际收录。
- CI 首轮运行即抓到 4 类本地 H2 看不到的真实缺陷，已修复：MySQL 的 `TIMESTAMPADD('MINUTE', …)` 引号单位（`SysUserMapper`）、趋势 upsert 绑定 null 计数（`TrendingServiceImpl` 显式落 0，H2 测试 schema 同步补齐 NOT NULL 约束）、测试 SQL 字符串里的下划线数字字面量（MySQL 当列名）、CI 裸 `docker run` 漏传 `JWT_EXPIRATION`/`JWT_REFRESH_EXPIRATION` 导致启动校验拒启；第二轮又抓到第 5 类：MySQL 单表 UPDATE 赋值从左到右求值导致登录锁定提前一次（`SysUserMapper.incrementLoginFailCount` 调整赋值顺序）。详见 `docs/agents/backend.md` 的 “Database portability” 一节。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `.github/workflows/ci.yml` | 新增（4 任务 CI） |
| `backend/mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties` | 新增（Maven Wrapper） |
| `backend/schema.sql` → `schema-fresh-install.sql`、`docker-compose.yml`、`DEPLOY.md`、`README.md`、`CLAUDE.md` | 改名与引用同步 |
| `backend/entrypoint.sh`、`backend/.dockerignore` | 删除（无实际用途） |
| `backend/pom.xml`、`frontend/package.json`(+lock) | 版本与 GFM 依赖 |
| `backend/src/main/java/com/selfblog/{service/site/seo/*,controller/SpaController.java,mapper/BlogPostTagMapper.*,dto/site/TagLastModified.java}`、`static/seo-reading.css` | 阅读 HTML/GFM/ETag/JSON-LD/lastmod |
| `backend/src/test/resources/schema.sql` | H2 唯一键对齐 |
| `SafeMarkdownTest`、`ReadingHtmlRendererTest`、`ReadingHtmlControllerTest`、`SitemapServiceTest` | 断言更新与新增 |
| `frontend/index.html`、`src/__tests__/indexHtml.test.ts`、`src/components/pages/__tests__/markdownGfm.test.ts`、`vite.config.ts` | 首屏主题 + GFM/首屏测试 |
| `frontend/src/views/admin/Settings.vue`、`router/modules/{user,auth,index}.ts`、25 个被删文件 | 遗留清理与密码入口 |
| `docs/agents/backend.md`、`docs/agents/frontend.md`、`docs/fixtures/gfm-sample.md` | 新增（拆分文档与共享 fixture） |
| `frontend/.prettierrc` 与漂移文件 | format 闸门修复 |

---

## v2.0.26 — 死代码清理与文档同步 (2026-10-17)

### 说明

- 删除 **22 个长期零引用的前端文件**：从未注册路由的校友圈树（`router/modules/circle.ts`、`views/circle/*`、`components/circle/*`）、无入口的管理页（`CircleManagement.vue`、`TopicsManagement.vue`）、旧首页组件（`HomeFilters/HomeSidebar/PostCardList.vue`）、旧页面（`TopicDetail.vue`、`TrendingPage.vue`、`PasswordReset.vue`、`Register.vue`）、`Card.vue`、`usePagination.ts`、`vo.ts`、`api/topic.ts`、`types/topic.ts` 等；每个删除前都用引用扫描确认无任何导入（含测试）。
- 同时移除 `api/admin.ts` 中已无消费者的 circle/topic 接口封装与相关类型导出。**后端社区/话题接口与数据表保留**（仍是管理员 API 的一部分），只是删掉死掉的前端入口。
- 文档同步：`frontend/CLAUDE.md`（结构、Vercel 代理模型、删除清单，并移除过时的 v2.0.22 重复修复表）、`frontend/README.md`（结构）、根 `README.md`（文档索引补齐 `docs/`、测试数）、`backend/README.md`（测试数与前端入口说明）、`AGENTS.md`（清理记录与最新计数）。
- 验证：前端 41 文件/394 测试、类型检查、构建与 lint（0 错误；警告从 256 降至 159，因删除死代码而自然减少）；死文件扫描结果为“无未引用文件”；重打包单 JAR 后浏览器回归 **32/32** 通过。
- 未改动：后端业务代码、数据库结构、用户根 `CLAUDE.md`（其“已知问题”里关于 circle 路由的说明已过时，但该文件带有你的未提交修改，保持不动）。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| 上列 22 个前端文件 | 删除（零引用死代码） |
| `frontend/src/api/admin.ts`、`types/index.ts`、`types/admin.ts` | 移除失效的 circle/topic 封装与导出 |
| `README.md`、`frontend/README.md`、`frontend/CLAUDE.md`、`backend/README.md`、`AGENTS.md` | 文档同步 |

---

## v2.0.26 — 文章发布/编辑的标签与媒体修复 (2026-10-17)

### 说明（均为“先复现、再修复”）

- **修复：编辑文章后标签全部消失，且再也加不回来。** 真实环境（SQLite/MySQL）的 `blog_post_tag` 有 `UNIQUE(post_id, tag_id)`；编辑文章会先逻辑删除旧关联（`is_deleted=1`），随后 `INSERT IGNORE` 被旧行占据的唯一键静默拦下，标签永久失效。现改为按数据库方言 upsert 并把 `is_deleted` 复位为 0（MySQL `ON DUPLICATE KEY UPDATE`、SQLite `ON CONFLICT … DO UPDATE`、H2 `MERGE … KEY`）。H2 测试库此前缺少该唯一键，导致单测看不到问题，已补齐以与生产一致。
- **修复：`BlogPostMedia.selectByPostId` 未过滤软删除**，编辑文章替换配图后，编辑器与公开接口会继续返回已移除的旧图。查询补 `is_deleted = 0`，并补上此前缺失的 H2 兜底批量插入语句。
- **修复：同时提交 `tagIds` 与 `tagNames` 时新标签被静默丢弃。** 编辑器会同时发送两个字段（已有标签带 id、新输入只有名字），后端旧逻辑“tagIds 非空就忽略 tagNames”。现按并集解析（名字走 getOrCreate 后与 id 去重合并），混用场景不再丢标签。
- **修复：写作页标签输入上限 50 字，与后端 20 字限制不一致**（此前只修过后台标签管理页）。输入框改为 20 字，仍以后端校验为准。
- 新增回归：`PostTagLifecycleTest`（编辑保留/移除/恢复/混用）、`BlogPostMediaMapperTest`（替换媒体不返回软删行）、`PostEdit.test.ts`（输入上限与发布载荷契约）；真实单 JAR 联调新增“编辑后标签保留、可恢复”检查。
- **同类问题系统排查（同一批次）**：
  - **标签删除后重建同名标签**：`blog_tag.name` 唯一 + 逻辑删除，旧 `createTag`/`getOrCreateTag` 在唯一键冲突后按“未删除”条件查询只能拿到 null → 创建接口返回空、发文章时该标签丢失（实测 NPE/500）。改为原始查询找回同名行并恢复 `is_deleted=0`（新增 `selectRawByName`/`restoreById`）。
  - **趋势表同类缺陷**：`blog_trending` 唯一键 (post_id, date) + 逻辑删除；每日 upsert 未复位 `is_deleted`（被删过的那天永远查不到），查询也未过滤软删行。三种方言的 upsert 统一复位标记并补齐查询条件。
  - **文章接口缺长度校验**：`category`（库列 50）与 `coverImage`（库列 500）没有 DTO 约束，超长会在数据库层报 500（SQLite 甚至静默存入）。补 `@Size` 后统一返回 400。
  - **测试库对齐生产约束**：H2 补上 `blog_tag(name)`、`blog_like`、`blog_collect`、`blog_follow`、`blog_trending`、`blog_draft_tag` 的唯一键（此前只有 2 个约束），这类“生产有唯一键、测试没有”的盲区不再出现。
  - **唯一键冲突的异常类型不可移植（真实联调两轮才修净）**：Spring 只把 H2/MySQL 的唯一键冲突翻译成 `DuplicateKeyException`；SQLite 的 SQLState 为 null、error code 19，最终抛的是 `UncategorizedSQLException`。第一版只捕获 `DuplicateKeyException`（SQLite 仍 500），第二版捕获 `DataIntegrityViolationException`（仍不够），最终统一捕获可移植父类 `DataAccessException`，并且**只在真正找到冲突行时转换**，找不到就保留原始异常。同类共修复 5 处：标签创建/取用、页面地址预留（→409）、关注、动态点赞、用户创建、话题创建。
  - 排查中确认无需改动：点赞/收藏/关注早已使用“原始查询 + 恢复标记”的正确写法；`BlogPostMapper.fullTextSearch*`、社区 Circle/Topic 的少数 SQL 只有 MySQL/SQLite 变体（H2 无兜底），但当前无调用方或仅旧社区路径，未扩大改动范围。
- 全量验证：前端 41 文件/394 测试、类型检查与 Lint 通过；后端 559 测试通过；真实 JAR + SQLite 联调 11/11（新增“标签删除后重建”）、浏览器回归 32/32。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/src/main/resources/mapper/BlogPostTagMapper.xml` | 关联写入由 INSERT IGNORE 改为方言 upsert 并复位 `is_deleted` |
| `backend/src/main/resources/mapper/BlogPostMediaMapper.xml` | 查询补软删条件；补 H2 兜底批量插入 |
| `backend/.../service/post/impl/BlogPostServiceImpl.java` | `resolveTagIds` 改为 tagIds/tagNames 并集 |
| `backend/src/test/resources/schema.sql` | H2 的 `blog_post_tag` 补 `UNIQUE(post_id, tag_id)`，与生产一致 |
| `backend/src/test/.../PostTagLifecycleTest.java`、`mapper/BlogPostMediaMapperTest.java` | 新增回归 |
| `frontend/src/views/post/PostEdit.vue` | 标签输入上限对齐后端 20 字 |
| `frontend/src/views/post/__tests__/PostEdit.test.ts` | 新增输入上限与载荷契约断言 |
| `backend/.../mapper/BlogTagMapper.java`、`service/post/impl/BlogTagServiceImpl.java` | 同名标签重建时恢复原行（原始查询 + 复位标记） |
| `backend/.../mapper/BlogTrendingMapper.xml` | upsert 复位 `is_deleted`；查询补软删条件 |
| `backend/.../dto/post/PostCreateRequest.java`、`controller/BlogPostControllerTest.java` | 分类/封面长度约束与 400 断言 |
| `backend/src/test/resources/schema.sql` | 测试库补齐生产已有的唯一键（tag/like/collect/follow/trending/draft_tag） |
| `backend/.../service/post/BlogTagLifecycleTest.java`、`mapper/BlogTrendingMapperTest.java` | 新增回归 |
| `backend/.../BlogTagConflictTest.java`、`service/site/impl/PageSlugConflictTest.java` | 唯一键冲突的异常类型契约（H2/MySQL/SQLite） |
| `backend/.../service/{site/impl/PageServiceImpl,social/impl/FollowServiceImpl,circle/impl/CircleInteractionServiceImpl,auth/impl/SysUserServiceImpl,content/impl/TopicServiceImpl}.java` | 统一捕获 `DataIntegrityViolationException` |

---

## v2.0.26 — 可抓取正文与统一部署 (2026-10-16)

### 说明

- 公开历史路由（`/`、`/about`、`/archives`、`/gallery`、`/post/{id}`、`/tag/{id}`、`/pages/{slug}`）不再返回空壳 SPA：JAR 直接输出可阅读 HTML（禁用 JavaScript 也能读正文），Vue 挂载后照常接管交互。首次响应重建 title/description/robots/canonical/Open Graph/Twitter 与 BlogPosting 结构化数据，`/index.html` 308 到 `/`，未知路径返回真正 404，模板缺失返回 503。
- 新增共享 `SiteOrigin`：设置 `SITE_BASE_URL` 后，canonical、分享图、订阅源、站点地图与 robots 都使用正式对外地址，伪造的 `Host`/`X-Forwarded-*` 不再影响；前端运行时读取 `selfblog-site-origin` 标记，避免挂载后把地址改回代理域名。
- `/sitemap.xml` 改为 sitemap index + 分片（`sitemap-site.xml`、`sitemap-posts-N.xml`、`sitemap-tags-N.xml`、`sitemap-pages-N.xml`），每片最多 1000 条，覆盖**全部**已发布文章、标签与自建内容页，并遵循每页 `indexable`；RSS/Atom 保留最近 100 篇的既定上限。robots 按实际后台路径动态生成。
- 公开 HTML/站点地图只读取已发布内容：管理员身份也不会让草稿出现在响应中；阅读请求不增加阅读量或访客事件。真实联调发现并修复了“内层 404 与外层共享事务导致缺失页面返回 500”的问题（`ReadingPageService` 不再声明类级事务）。
- Vercel 改为**统一反向代理**：`frontend/vercel.json` 不再部署第二份静态前端，改为在构建期由 `scripts/build-vercel-proxy.mjs` 生成 Build Output API v3 路由，把原路径/方法/查询转发到同一源站，动态响应 `no-store`，Preview 环境附加 `X-Robots-Tag: noindex,nofollow`；非法、环回或自指的 origin 直接构建失败。
- Docker 修复为可用形态：仓库根上下文、Maven+JDK21 构建阶段、JRE 非 root 运行、`curl` 健康检查、`selfblog.jar` 正确产物、数据/上传/相册三类持久化卷；保留既有 MySQL 方案卷名，另提供 SQLite Compose。本机无 Docker 守护进程，镜像运行未验证。
- 全量验证：前端 41 文件/393 测试、类型检查、Lint 0 错误/256 警告、构建与部署契约 27/27；后端 545 测试通过。真实单 JAR + SQLite 联调 9/9（无 JS 抓取、站点地图完整性、下线 404、Vue 接管无重复、本地代理传输、后台域名即时生效与回落），见[验证记录](docs/reviews/2026-10-16-seo-deployment-verification.md)。
- 新增「网站域名」后台设置（`siteBaseUrl`）：canonical、分享图、订阅源、站点地图与 robots 的 origin 可直接在后台修改，**后台设置为准**，留空回落到 `.env` 的 `SITE_BASE_URL` 或当前访问地址；非法输入被拒绝，数据库里被改坏的值记录警告并安全回落，不会让整站 500。保存后下一次请求生效（无需重启）。
- 保持已确认的界面与比例不变；不承诺搜索引擎收录或排名。Docker、真实 Vercel 部署、MySQL 运行仍未验证。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/.../service/site/seo/{SiteOrigin,SafeMarkdown,SeoModels,ReadingTemplate,ReadingContent,ReadingHtmlRenderer,ReadingPageService,SitemapService}.java` | 新增：origin 校验、Markdown 净化、阅读模型/文档渲染与分片站点地图 |
| `backend/.../controller/{SpaController,site/SeoController}.java`、`config/security/SecurityConfig.java`、`service/post/PostQueryService(+Impl).java` | 根路径 HTML 交付、发现文档重写、仅已发布查询、静态资源放行 |
| `backend/src/main/resources/static/seo-reading.css`、`pom.xml` | 阅读区域局部样式、CommonMark 依赖 |
| `backend/Dockerfile`、`docker-compose.yml`、`docker-compose.sqlite.yml`、根 `.dockerignore` | 单 JAR 容器构建、持久化卷与安全运行 |
| `frontend/vercel.json`、`scripts/{vercel-proxy,build-vercel-proxy}.mjs`、`package.json` | 统一代理生成器与部署契约测试入口 |
| `frontend/src/composables/useSeoMeta.ts` | 优先使用服务端 canonical origin |
| `frontend/src/stores/site.ts`、`views/admin/Settings.vue` | 新增「网站域名」设置项（类型、默认值与表单） |
| `backend/.../impl/SiteSettingsServiceImpl.java`、`service/site/seo/SiteOrigin.java` | 白名单登记与校验、后台设置优先的 origin 解析 |
| 对应前后端测试 | origin/Markdown、严格公开查询、阅读文档、HTTP 契约、站点地图边界、部署适配 |
| `docs/reviews/2026-10-16-seo-deployment-verification.md`、README 分册、DEPLOY、AGENTS/CLAUDE 规则、规格/计划 | 使用与部署说明、验证范围与限制 |

---

## v2.0.26 — 逐页配置与独立内容页（第一批） (2026-10-16)

### 说明

- 新增后台页面管理：8类内置公开页/详情模板的独立文案、模块开关、受控样式与SEO字段；关于页支持独立Markdown正文。
- 新增独立内容页、草稿/发布分离、真实组件预览、下线/删除和公开导航编辑；桌面链接按可用空间进入「更多」，手机保留完整导航。
- 复用现有KV表，不修改schema；草稿不公开，数据库条件更新防止旧版本静默覆盖，slug唯一预留，配置按UTF-8字节限制。
- 用户已取消全站放大：保持当前字号、导航高度和页面比例。未配置的页面继续采用现有内容与作者主题。
- 全量验证：前端41文件/389测试、类型检查、Lint0错误/256警告、构建及本轮文件格式检查通过；后端491测试通过，含两项真实H2独立事务竞争。隔离SQLite浏览器**31/31**通过，含单JAR直连、UI确认下线/删除、390px深色预览和首屏请求顺序，见[验证记录](docs/reviews/2026-10-16-page-management-verification.md)。
- 修复首屏配置摘要与About正文的并行加载顺序问题：同发布版本合并正文，版本更新则重新获取，不让摘要擦掉正文或旧响应覆盖新版本。
- 本批完善运行时元数据，不声称搜索引擎已经收录；服务端HTML SEO及已知Docker/Vercel欠项仍属后续批次。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `backend/.../dto/site/PageModels.java`、`service/site/PageService.java`、`service/site/page/PageCatalog.java`、`service/site/impl/PageServiceImpl.java` | 新增：类型、白名单、版本化存储与发布/导航逻辑 |
| `backend/.../controller/admin/AdminPageController.java`、`controller/site/PublicPageController.java`、安全/限流/站点设置配置 | 新增接口及权限、限流、保留路径 |
| `frontend/src/{api,types,stores}/pages*`、`composables/usePage*.ts`、`components/pages/*` | 新增：公开配置、局部样式、安全正文及元数据生命周期 |
| `frontend/src/views/admin/pages/*`、公开页面、导航、路由与启动入口 | 页面编辑/预览/导航及消费端接入 |
| 对应前后端测试 | 新增草稿隔离、版本冲突、页面模块、路由保护、预览、导航与元数据回归 |
| `docs/page-management.md`、`docs/decisions/001-versioned-pages-in-existing-kv.md`、README分册、规格/计划 | 使用指南、架构取舍、验证范围与限制 |
| `backend/DEPLOY.md` | 升级、完整KV备份及旧后台命名空间兼容说明 |

---

## v2.0.26 — 导航原型还原与前后端联调修正 (2026-10-16)

### 说明

- 导航按批准原型恢复链接紧接搜索、右侧工具前留白、浅蓝胶囊选中态、静止声条＋「听一会儿」及音乐面板布局；保留 44px 触达、真实音频状态、作者品牌与主题配置。1440px 下第一个链接从 x=885 修正为 x=472（原型 x=471）。
- 修复后台抽屉切换桌面后的焦点/滚动锁残留、关闭侧栏仍可聚焦屏幕外链接及缺失的 aria-controls 目标。
- 修复发布/手动保存成功后误报未保存；改为完整提交快照，保护请求期间的新改动与失败请求，避免自动保存与发布竞争。真实路由挂载新增相应回归。
- 标签新建/编辑与后端既有 20 字限制对齐，保留具体失败原因；后端校验不变。
- 独立复核追加并修复：恢复后续草稿时保留原文章 ID、取消发布确认后焦点回到发布按钮；均先复现失败再验证修复。
- 前端 31 文件 / 345 测试、类型检查、Lint（0 错误 / 255 警告）与构建通过；后端 463 测试通过。真实 SQLite 联调初查 13/15，修正并扩展复核场景后为 **17/17**，见[复查报告](docs/reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md)。
- 原有数据库、上传目录和运行服务未被写入；临时联调实例已关闭。MySQL、Docker、SMTP 与外部媒体未在本次真实联调中验证。

### 影响文件

| 文件 | 改动类型 |
| --- | --- |
| `frontend/src/components/layout/Navbar.vue`、`NavbarMusicControl.vue`、`components/common/icons.ts` | 导航/音乐原型还原与高对比轨道降级 |
| `frontend/src/views/admin/AdminLayout.vue`、`TagsManagement.vue` | 抽屉生命周期及标签校验 |
| `frontend/src/views/post/PostEdit.vue` | 提交快照、未保存保护及异步写入互斥 |
| 对应 `__tests__/*.test.ts` | 新增和强化真实路由、交互与边界回归 |
| `docs/reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md` | 新增：差异证据、缺陷复现、真实验证范围与限制 |
| `README.md`、`frontend/README.md`、`backend/README.md` | 更新：修复结论、锁定版本、验证范围及限制 |
| `frontend/CLAUDE.md`、`AGENTS.md`、设计规格与计划 | 更新：约束、实际依赖版本、代理描述与交付记录 |

---

## v2.0.26 — 清透空间重设计（三）：作者工作台 (2026-10-16)

### 说明

- 后台侧栏改为常驻紧凑结构：品牌 → 现有作者模块 → 主题切换/返回博客；导航选中态用浅色填充，触达区域 44px，不再使用文字符号图标。
- 移动端抽屉接管焦点与滚动锁：Esc 关闭并把焦点还给菜单按钮，路由切换自动收起，`aria-expanded`/`aria-controls` 完整。
- 登录页去掉仿 macOS 窗口装饰与网格背景，改为单一紧凑卡片；保留原有限制（用户名 3–20 位、密码 ≥8 位）、记住我、跳转校验与 busy 状态。
- 概览、文章、标签、访客、评论、相册、设置统一「标题 → 说明 → 内容区」层级：去掉全大写英文标签与装饰性序号/星形符号，按钮/输入框 8px、分组卡 12–16px，表格在窄屏内横向滚动而不挤压列、也不隐藏关键操作。
- 访客图表、热门页面、评论状态、相册上传与密码相册、设置分组（站点资料 / 后台与访客 / 评论与音乐 / 视觉主题）与字段语义保持不变；相册弹窗改用共享 Modal（层级、Esc、焦点回收与灯箱互不干扰）。
- 写作页保留 Markdown 编辑、撤销重做、工具栏、预览、草稿、封面、分类、标签与发布流程，只把外壳与控件收敛到同一套表面/圆角/触达标准。

### 验证

- 前端 30 文件 / 328 测试通过；类型检查、Lint（0 错误）与生产构建通过。
- 新增回归：侧栏路由/ Esc /焦点、编辑器草稿与发布、相册密码与灯箱、设置分组、公共页搜索/归档/关于/404 与 `ArticleList`。
- Chrome 实测：8 条公开路由 × 1440/1024/820/768/390px 无横向溢出、无小于 32px 的操作控件；登录 → 7 个后台入口标题与选中态、暗色切换、390px 抽屉 + 滚动锁、表格横向滚动、写作页填写均通过；无页面错误。API 为浏览器测试数据，未改动既有数据库或上传目录。

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/admin/AdminLayout.vue` | 紧凑侧栏、抽屉焦点/滚动、主题切换 |
| `frontend/src/views/admin/Dashboard.vue`、`Posts.vue`、`TagsManagement.vue`、`Visitors.vue`、`Comments.vue`、`Albums.vue`、`Settings.vue` | 统一层级与控制台样式；相册改用共享 Modal |
| `frontend/src/views/auth/Login.vue` | 去掉窗口装饰，收敛登录卡片 |
| `frontend/src/views/post/PostEdit.vue` | 编辑器外壳/工具栏/侧栏表面与圆角统一 |
| `frontend/src/views/__tests__/`、`views/post/__tests__/`、`views/gallery/__tests__/`、`views/admin/__tests__/`、`components/post/__tests__/` | 新增/扩展行为回归 |
| `README.md`、`frontend/README.md`、`frontend/CLAUDE.md` | 结构与验证记录 |

---

## v2.0.26 — 清透空间重设计（二）：公开阅读路径 (2026-10-16)

### 说明

- 新增 `ArticleList` 文章行组件（日期/分类 → 标题 → 摘要 → 标签/阅读时长，可选右侧缩略图，无封面不留空槽，缩略图加载失败自动移除），首页、搜索、标签共用同一套排版。
- 首页改为「真实站点配置的介绍区 + 单列文章列表 + 224px 作者侧栏」，侧栏在 ≤960px 收起；介绍区装饰图取首批成功加载文章中的第一张封面并在本次停留内保持不变，无封面或加载失败使用中性底色，不额外请求接口。
- 归档改成年月分组 + 文章行（保留全部翻页归档查询）；关于页改为作者资料主导；404 提供具名搜索与返回操作；页脚与返回顶部统一圆角、图标与 44px 触达，并遵循减少动效。
- 阅读页把标题/摘要/时间/阅读时长提到封面之前；正文使用系统无衬线、约 760px 行宽，侧边目录与移动端抽屉断点统一到 1020px；保留净化链路、代码复制/折叠、标题锚点、灯箱、相邻文章、匿名点赞与留言。阅读进度改为按帧合并并支持取消。
- 相册改用共享 Modal/ImagePreview：修复低于导航的遮罩层级、Esc、焦点回收与滚动锁；保留公开/密码相册与短期令牌流程，新增详情加载、失败重试与响应代际保护。

### 验证

- 新增 `ArticleList`、首页、公共页、相册与阅读页回归后，全量前端测试为 30 文件 / 328 测试通过；类型检查、Lint 与构建通过。
- Chrome 实测 1440/1024/820/768/390px：公开路由无横向溢出，封面/侧栏/目录按断点重排，灯箱高于导航且关闭后相册仍打开。

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/components/post/ArticleList.vue` | 新增：共享文章行 |
| `frontend/src/views/Home.vue`、`Archives.vue`、`About.vue`、`common/NotFound.vue`、`search/Search.vue`、`tag/TagDetail.vue` | 版式重做，复用文章行 |
| `frontend/src/views/post/PostDetail.vue`、`components/post/MobileTocSheet.vue` | 阅读层级、进度按帧合并、目录断点 |
| `frontend/src/views/gallery/Gallery.vue` | 接入共享浮层、失败重试与竞态保护 |
| `frontend/src/components/layout/Footer.vue`、`components/common/BackToTop.vue` | 统一紧凑控件与减少动效 |

---

## v2.0.26 — 清透空间重设计（一）：紧凑导航与音乐 (2026-10-16)

### 说明

- 采用已确认的灰白画布、系统无衬线字体与小圆角层级；正文实底，玻璃仅用于功能层，并提供透明度/高对比/减少动效降级。保留作者皮肤及主色，派生相应软底与 hover 色。
- 导航按品牌 → 搜索 → 图标文字链接 → 音乐/主题排列，桌面 52px、手机 50px，滚动不改高度、不隐藏；小屏保持 44px 工具触达，面板互斥并支持 Esc/外部点击/换页关闭。
- 背景音乐从右下悬浮条移入导航，唯一音频宿主不自动播放；提供播放/暂停、有效进度、音量、单曲循环、行内错误与重试。公开换页连续，进入后台暂停，替换/禁用音源阻止旧异步播放恢复。
- 弹窗、灯箱和目录使用统一主题浮层、顶部对话框键盘处理、焦点回收与滚动锁；灯箱重新打开时同步初始照片，无图片不锁页面。
- 本批只完成底座/导航/音乐，不把仍待重做的首页、公开阅读页及作者工作台计为完成。无后端/API/schema/依赖改动。

### 验证

- 前端 23 文件 / 267 测试通过；类型检查、Lint（0 错误，272 既有警告）和生产构建通过。
- Chrome 七档宽度、亮暗/减少动效、焦点与层级检查通过；生成 WAV 验证真实播放、公开换页连续、后台暂停与音源替换。API 为浏览器测试数据，未操作既有数据库或上传文件。

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/App.vue` | 常驻唯一音频宿主、稳定主题浮层与外观应用 |
| `frontend/src/styles/{variables,dark-mode,skins,base,glassmorphism,components,typography,a11y}.css` | 紧凑 token、系统字体、功能玻璃与可访问降级 |
| `frontend/src/stores/{music,site,theme}.ts` | 音频生命周期、主色推导、同步主题切换 |
| `frontend/src/components/layout/{Navbar,NavbarMusicControl,MusicPlayer}.vue` | 响应式导航与音乐面板/宿主拆分 |
| `frontend/src/composables/useDialog.ts` | 共享对话框栈、焦点与滚动管理 |
| `frontend/src/components/common/{Modal,ImagePreview,Toast}.vue`、`icons.ts`、`components/post/MobileTocSheet.vue` | 浮层接入、图标与目录适配 |
| `frontend/src/**/__tests__/` 相关测试 | 音频/导航/主题/浮层与回归用例 |
| `docs/superpowers/specs/2026-10-16-selfblog-ios-glass-redesign-design.md`、`docs/superpowers/plans/2026-10-16-compact-glass-*.md` | 已确认设计与三批实施计划 |
| `README.md`、`frontend/README.md`、`frontend/CLAUDE.md` | 已实现边界、结构与验证记录 |

---

## v2.0.26 — 修复单 JAR 模式下静态 favicon 的 401 (2026-10-16)

### 修复

- `index.html` 的默认图标兜底是 `/favicon.svg`，但 `SecurityConfig` 的静态资源放行名单只写了 `/favicon.ico`；在单 JAR（Spring 直接提供前端静态资源）模式下，浏览器请求默认 favicon 会落到 admin 兜底规则返回 401。已把 `/favicon.svg` 加入放行名单，并用打包后的 JAR 实测 `GET /favicon.svg → 200`

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/com/selfblog/config/security/SecurityConfig.java` | 静态资源放行名单新增 `/favicon.svg` |

---

## v2.0.26 — 视觉重设计（四）：全站圆角收尾 (2026-10-16)

批次一上调了圆角 token，但页面里仍有大量裸圆角值不跟随；本批把它们全部换成 token，让「大 R 角」真正覆盖全站。

### 说明

- **79 处裸圆角（22 个文件）按映射表替换**：3–5px → `--radius-xs`、7–9px → `--radius-sm`、10–14px → `--radius`、15–18px → `--radius-lg`、20px → `--radius-xl`；`50%`（圆形）、`999px`（胶囊）、`0`（直角）与 1–2px 微元素保持不动
- 复核扫描：排除 `var(--radius*)`、百分比、`inherit`、`0`、`1px`、`2px`、`999px` 后，全仓库不再有裸圆角值；`git diff` 抽查确认只改了 `border-radius` 数值
- **深色模式收尾**：设置页 `.alert` / `.success` 提示条的浅色硬编码改为 `--error-light` / `--success-light` + 语义文字色（`dark-mode.css` 已有暗色覆盖）

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/Home.vue`、`Archives.vue`、`post/PostDetail.vue`、`gallery/Gallery.vue`、`search/Search.vue`、`tag/TagDetail.vue`、`trending/TrendingPage.vue`、`user/Profile.vue`、`user/UserProfile.vue` | 裸圆角 → token |
| `frontend/src/views/admin/` 下 Albums / Comments / Dashboard / Posts / Reports / Settings / Statistics / TagsManagement / TopicsManagement / Users / Visitors / CircleManagement | 裸圆角 → token；Settings 另修深色提示条 |
| `frontend/src/styles/skeleton.css` | 裸圆角 → token |

---

## v2.0.26 — 视觉重设计（三）：文章页阅读体验 (2026-10-16)

把阅读路径上的交互补齐：代码块、目录、锚点、灯箱与阅读时长。

### 新增

- **代码块头部 + 复制**：`pre` 包装为 `.code-block`，头部显示语言标签与「复制」按钮（真实 `<button>`，写入剪贴板，约 1.5s 后复位）
- **长代码折叠**：超过 320px 的代码块默认折叠，底部渐隐，「展开代码 / 收起代码」切换
- **标题锚点**：每个 h1–h3 追加 `#` 锚点（悬停/键盘聚焦可见）；依赖既有的 `scroll-padding-top: 96px`，跳转不被浮动导航盖住
- **移动端目录抽屉**：≤860px 隐藏侧边目录，改为左下浮动胶囊 + 底部抽屉（复用同一份 toc 数据与高亮态）
- **正文图片灯箱**：点击正文图片打开 `ImagePreview`，多图左右切换、Esc 关闭
- **按正文估算阅读时长**：新增 `utils/reading.ts`，剔除代码块/图片后按「中日韩按字 + 西文按词」统计，替换原先按字符数除以 400 的粗算
- **最后更新提示**：`updateTime` 晚于 `createTime` 7 天以上时，meta 行展示「最后更新于 …」

### 修复

- **`ImagePreview.vue` 键盘监听 TDZ**：`watch(..., { immediate: true })` 在 `const handleKeydown` 声明前引用它，初始 `show=true` 时监听挂不上（Esc / 方向键失效）；改为函数声明并补测试

### 说明

- 标题高亮从「每次滚动遍历全部标题」改为 `IntersectionObserver`，仅在不支持的环境回退到扫描
- `MobileTocSheet` 由 `PostDetail.vue` 挂载（而非设计稿中的 App.vue）——目录数据本来就在 PostDetail，避免提升成全局状态

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/utils/reading.ts` | 新增：正文字数与阅读时长估算 |
| `frontend/src/composables/useMarkdownEnhance.ts` | 新增：DOM 后处理（代码块 / 锚点 / 图片收集） |
| `frontend/src/components/post/MobileTocSheet.vue` | 新增：移动端目录抽屉 |
| `frontend/src/views/post/PostDetail.vue` | 集成增强、灯箱、IO 高亮、时长与更新提示 |
| `frontend/src/components/common/ImagePreview.vue` | 修复键盘监听 TDZ |
| `frontend/src/utils/__tests__/reading.test.ts` | 新增：字数统计测试 |
| `frontend/src/composables/__tests__/useMarkdownEnhance.test.ts` | 新增：DOM 后处理测试 |
| `frontend/src/components/post/__tests__/MobileTocSheet.test.ts` | 新增：目录抽屉测试 |
| `frontend/src/components/common/__tests__/ImagePreview.test.ts` | 新增：键盘与滚动锁定测试 |

---

## v2.0.26 — 视觉重设计（二）：后台侧边栏 (2026-10-15)

把公开页顶栏的同一套语言（内置图标、胶囊选中、品牌资产）带进后台。

### 新增

- **后台侧栏图标化**：`⌂ ▤ ⌑ ◷ ▱ ▧ ⚙` 等文字符号全部换成内置 SVG 图标（概览 / 文章 / 标签 / 访客 / 评论 / 相册 / 设置），移动端抽屉的菜单与关闭按钮、页脚"返回博客"一并换掉
- **侧栏品牌位消费 `siteLogoUrl`**：有值显示上传的 Logo，为空回落内置图标——与公开页顶栏完全一致
- 侧栏选中项改为 `--radius-full` 胶囊，hover 未选中同样出胶囊浅底；`AdminLayout` 内的裸圆角值同步换成 token

### 说明

- 纯前端改动，不涉及接口与数据；`Settings.vue` 的 Logo 字段提示恢复为"公开页顶栏与后台侧边栏"（侧栏未接入时曾临时收窄）
- 侧栏导航仍是同一批路由，未新增页面

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/admin/AdminLayout.vue` | 图标替换、胶囊选中、品牌 logo、token 圆角 |
| `frontend/src/views/admin/Settings.vue` | Logo 提示恢复为双端描述 |
| `frontend/src/views/admin/__tests__/AdminLayout.test.ts` | 新增：品牌回落/上传、图标化、选中态测试 |

---

## v2.0.26 — 站点资源上传控件（前端）(2026-10-15)

承接「资源上传通道（后端）」：后台「网站设置」页新增四个品牌资源字段，支持上传与预览。

### 新增

- **`SiteAssetField.vue` 通用资源字段**：预览 + 手动 URL 输入 + 上传按钮；按 kind 限定 `accept` 与体积上限（图片 2MB、音乐 20MB），预检失败不发请求，错误行内展示
- **上传 API** `siteApi.uploadSiteAsset()`：multipart 提交到 `POST /api/admin/site-assets`，带上传进度回调

### 说明

- 上传成功即写入表单并同步 `siteStore`（顶栏 Logo、favicon 立即换新），点「保存设置」才通过 `PUT /api/admin/site-settings` 落库；手填 URL 只写表单、保存时一并生效
- 音乐字段预览显示文件名与时长；图片字段显示方形缩略图；远端 CDN 手填 URL 的能力保留
- 前端预检与后端校验规则一致，但服务端仍是权威（扩展名白名单 + 魔数 + 体积）

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/api/site.ts` | 新增 `SiteAssetKind` / `SiteAssetUpload` 类型与 `uploadSiteAsset()` |
| `frontend/src/components/admin/SiteAssetField.vue` | 新增：站点资源上传字段组件 |
| `frontend/src/views/admin/Settings.vue` | 四个字段接入 + 乐观更新 |
| `frontend/src/api/__tests__/site.test.ts` | 新增：上传 API 单测 |
| `frontend/src/components/admin/__tests__/SiteAssetField.test.ts` | 新增：字段组件单测 |
| `frontend/src/views/admin/__tests__/Settings.test.ts` | 新增：设置页集成测试 |

---

## v2.0.26 — 资源上传通道（后端）(2026-10-15)

### 新增

- **管理员站点资源上传** `POST /api/admin/site-assets`：仅管理员可用，multipart 参数为 `file` 与 `kind`（`logo` / `favicon` / `avatar` / `music`）；成功后返回随机公开地址 `/uploads/site-assets/<kind>/<uuid>.<ext>`
- 上传校验包含扩展名白名单与文件魔数；logo、favicon、avatar 每个文件最多 2 MB，music 最多 20 MB；接口限流为每分钟 30 次
- **SVG 安全净化**：移除危险元素（含 XML 命名空间前缀形式）、事件属性和脚本 URL
- 站点设置新增并默认提供 `faviconUrl`、`siteLogoUrl`；`site_setting` 是键值表，因此无需改表或数据库迁移

### 说明

- 上传通道不复用 `MediaService`：它会把所有图片重新编码为 `.jpg`，不适用于 `.ico`、`.svg` 或音频。`.ico` 同时接受 ICO 与 PNG 文件头，因为部分 favicon 导出工具会把 PNG 字节写入 `.ico` 文件
- `/uploads/**` 是公开地址，上传的 SVG 可被直接作为顶层文档打开，脚本可能以本站源执行；因此 SVG 净化是必要的安全边界，不能只依赖图片上下文的限制
- 后端接口与设置支持已完成；后台 `Settings.vue` 上传控件及前端 API 接入留待下一批

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/com/selfblog/common/enums/SiteAssetKind.java` | 新增：资产种类、扩展名白名单与分档大小上限 |
| `backend/src/test/java/com/selfblog/common/enums/SiteAssetKindTest.java` | 新增：资产种类、扩展名与大小规则测试 |
| `backend/src/main/java/com/selfblog/vo/site/SiteAssetVO.java` | 新增：上传结果与资源元信息 |
| `backend/src/main/java/com/selfblog/service/site/SiteAssetService.java` | 新增：站点资源上传服务接口 |
| `backend/src/main/java/com/selfblog/service/site/impl/SiteAssetServiceImpl.java` | 新增：校验、SVG 净化及随机路径落盘 |
| `backend/src/test/java/com/selfblog/service/site/impl/SiteAssetServiceImplTest.java` | 新增：上传校验、大小限制与 SVG 净化测试 |
| `backend/src/main/java/com/selfblog/controller/admin/AdminSiteAssetController.java` | 新增：管理员资源上传接口 |
| `backend/src/test/java/com/selfblog/controller/AdminSiteAssetControllerTest.java` | 新增：接口、权限及错误响应测试 |
| `backend/src/main/java/com/selfblog/config/web/RateLimitProperties.java` | 修改：新增资源上传每分钟 30 次限流配置 |
| `backend/src/main/java/com/selfblog/service/site/impl/SiteSettingsServiceImpl.java` | 修改：允许并默认提供 `faviconUrl`、`siteLogoUrl` |
| `backend/src/test/java/com/selfblog/service/site/impl/SiteSettingsServiceImplTest.java` | 新增：品牌设置默认值与 URL 校验测试 |
| `backend/README.md` | 修改：更新管理员 API 数量及限流说明 |

---

## v2.0.26 — 视觉重设计（一）：设计底座与公开页导航 (2026-10-15)

站点视觉重设计的第一批落地：先把圆角 token、图标系统、滚动状态与品牌资产这些**共享底座**做好，再让公开页顶栏成为第一个消费者。后端上传通道、文章页阅读体验、后台胶囊化在后续批次。

### 新增

- **内置 SVG 图标系统** `components/common/icons.ts` + `components/common/Icon.vue`：21 个 24×24 描边图标（`logo`/`search`/`sun`/`moon`/`menu`/`close`/`home`/`archive`/`image`/`info`/`settings`/`gauge`/`file-text`/`tag`/`users`/`message`/`music`/`play`/`pause`/`arrow-up`/`external`）。全部靠 `stroke="currentColor"` 取色，深浅两套主题自动适配。**不引入任何图标库依赖**，`dependencies` 保持 7 项不变
- **`composables/useScrollState.ts`**：rAF 节流的滚动状态，返回 `{ scrolled, direction }`。滚动是高频事件，直接读 `scrollY` 会让长页面掉帧，因此同一帧内最多处理一次
- **顶栏滚动三态**：静止 → 圆角胶囊实底；下滚过阈值 → 高度收紧、转半透明 + 毛玻璃（`backdrop-filter: blur(20px) saturate(1.4)`）、阴影加深；小屏下滚 → 顶栏收走、上滚立刻归位（桌面端不隐藏，避免鼠标用户找不到导航）
- **导航选中态胶囊化**：选中项为 `--radius-full` 的浅色胶囊，未选中只在 hover 出浅底。移动端面板此前完全没有选中指示，一并补上
- **可上传的品牌商标**：`siteLogoUrl` 有值时顶栏渲染 `<img>`，为空时回落内置 `Icon name="logo"`——即"设置里可自由上传图片"的前端半边
- **`faviconUrl` 运行时注入**：`stores/site.ts` 新增 `mimeForIcon()` 与 `applyFavicon()`，按扩展名推导 MIME（`.svg`→`image/svg+xml`、`.ico`→`image/x-icon`、其余 `image/png`）并同步到 `<head>`。注入节点带 `data-runtime` 标记以便幂等清理，**`index.html` 的静态 `/favicon.svg` 永不移除**，所以"清空设置"会自然回落到默认图标
- **`html { scroll-padding-top: 96px }`**：顶栏是 `fixed` 浮动胶囊，原生锚点跳转需要给它让位，否则 `#hash` 跳转后标题会被压在导航下面

### 变更

- **圆角整体上移一档**（`--radius-xs` 4→6、`sm` 4→8、`radius`/`md` 8→12、`lg` 12→16、`xl` 16→20、`2xl` 20→24、`3xl` 24→32）。`--radius-full` 保持 `9999px`：胶囊选中态与圆形头像共用它，同一个值同时覆盖"小方块变圆"和"胶囊"，不再新增 `--radius-pill` 之类的冗余 token
- 搜索框改为全圆角胶囊，主题切换/汉堡按钮改为正圆，品牌角标由硬编码 `9px` 改用 `--radius-sm`
- 顶栏模板重写。`Navbar.vue` 里 `.write-link`/`.admin-link`/`.login-link`/`.logout-link` 四组样式是**死代码**——模板从未渲染过对应元素（登录/注册入口早先已移除），随本次重写一并删除

### Bug 修复

- **移动端搜索按钮文字竖排折行**：`.mobile-search` 里输入框 `width: 100%` 且未设 `flex`，把"搜索"按钮挤到只剩一个字宽，两个字上下排成两行。已给按钮 `flex: 0 0 auto` + `white-space: nowrap`
- **移动端导航没有任何选中指示**：桌面端有 `.nav-links a.active`，移动端面板的链接连 `active-class` 都没配

### 说明

- **布局高度守恒**是本次的硬约束：`.app-main { padding-top: 80px }`（`App.vue`）以及文章页的 `scroll-margin-top: 105px`、`.article-aside { top: 105px }` 都是按顶栏高度写死的锚点。因此滚动收缩**只改视觉高度**（`.nav-shell` 的 `min-height` 58→48），顶栏占据的布局高度始终不变，正文位置不跳动
- **jsdom 测不到的东西靠浏览器验**：胶囊圆角、毛玻璃、收缩位移都无法在单测里断言。已用 Playwright 实测计算样式与截图确认（`getComputedStyle` 取到 `border-radius: 24px`、`backdrop-filter: blur(20px) saturate(1.4)`、移动端 `opacity: 0` 且 `top: -62px`），上面两个移动端 Bug 就是这样发现的
- **第一批完成时的状态**：`faviconUrl` / `siteLogoUrl` 当时只是前端消费端；后端 `SiteSettingsServiceImpl` 的 `ALLOWED_KEYS` 与 `defaults()` 尚未收录这两个键，所以后台还存不进这两个值；后端收录与上传接口属于后续批次（现已由上方后端批次完成）
- 圆角 token 只改了 scale 本身，**全站逐处裸圆角替换与后台侧边栏胶囊化留到后续批次**，避免一次提交同时动几十个文件而无法审查

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/styles/variables.css` | 圆角 scale 整体上移一档 |
| `frontend/src/styles/base.css` | 新增 `scroll-padding-top: 96px` |
| `frontend/src/components/common/icons.ts` | 新增：21 个描边图标注册表 |
| `frontend/src/components/common/Icon.vue` | 新增：图标渲染组件（`name` / `size`） |
| `frontend/src/components/common/__tests__/icons.test.ts` | 新增：注册表结构与安全性断言（65 例） |
| `frontend/src/composables/useScrollState.ts` | 新增：rAF 节流的滚动状态 |
| `frontend/src/composables/__tests__/useScrollState.test.ts` | 新增：7 例，含每帧合并与卸载后静默 |
| `frontend/src/stores/site.ts` | 新增 `faviconUrl`/`siteLogoUrl` 键、`mimeForIcon()`、`applyFavicon()` |
| `frontend/src/stores/__tests__/site.test.ts` | 新增：11 例，含幂等注入与静态兜底不被移除 |
| `frontend/src/components/layout/Navbar.vue` | 重写：滚动三态、图标、胶囊选中、可上传 logo；修移动端两个问题 |
| `frontend/src/components/layout/__tests__/Navbar.test.ts` | 新增：10 例，用真实 memory router 断言 active class |
| `AGENTS.md` | 修正 Node 引擎范围、`format:check` 基线失败、`noUncheckedIndexedAccess`、jsdom 测试约定 |
| `docs/superpowers/specs/2026-10-15-selfblog-ui-redesign-design.md` | 新增：本次重设计的完整设计稿 |
| `docs/superpowers/plans/2026-10-15-selfblog-ui-foundation-and-navbar.md` | 新增：第一批次的实施计划与执行修订 |

---

## v2.0.26 — SEO、订阅源与字体 (2026-10-15)

参考同类开源个人博客项目（`linhaojun857/aurora`、`weiwosuoai/WeBlog`、`iszhouhua/blog` 等）后补齐的能力。
这些项目普遍具备「被订阅、被搜索、被分享」的基础设施，而本项目在此之前完全没有。

### 新增

- **RSS 2.0 订阅源** `GET /feed.xml`：输出已发布文章（`status=1`，按发布时间倒序，最多 100 篇），含标题、链接、摘要、分类与 `pubDate`
- **Atom 1.0 订阅源** `GET /atom.xml`：同一数据源的现代格式，时间戳按 RFC 3339 截断到秒
- **`GET /sitemap.xml`**：收录首页、归档、关于、相册、搜索、全部文章与全部标签
- **动态 `GET /robots.txt`**：`Sitemap` 指令必须是绝对地址，静态文件在构建时并不知道站点域名，因此改为运行时生成；同时 `Disallow: /admin` 与 `/api/`
- **`index.html` 补齐站点级 SEO/分享标签**：`description`、`canonical`、`theme-color`（浅色/深色两套）、Open Graph、Twitter Card，以及订阅源自动发现 `rel="alternate"`
- **文章页运行时更新分享卡片**：新增 `composables/useSeoMeta.ts`，进入文章详情时把 `og:title`/`og:description`/`og:image`/`article:published_time`/`article:tag` 换成该文章自己的值——单页应用所有路由共用一份 `index.html`，不这样做分享出去永远是首页标题
- **文章页 JSON-LD 结构化数据**：注入 `BlogPosting`（标题、摘要、封面、发布时间、作者、站点），让搜索结果有机会展示富摘要；用 `textContent` 写入，标题含 `</script>` 也不会破坏页面
- **上一篇 / 下一篇导航**：`GET /api/post/{id}/adjacent` 返回相邻的已发布文章（按发布时间，时间相同用 id 兜底），文章底部渲染为两张卡片。此前读完一篇文章没有任何出口，只能按浏览器后退
- 站点基地址支持 `SITE_BASE_URL` 覆盖（反向代理后 `request` 里的 host 可能不是对外域名），未配置时按 `X-Forwarded-*` 或请求推导

### Bug 修复

- **`/robots.txt` 返回 500**：`SecurityConfig` 已放行该路径，但项目里根本没有这个资源，爬虫访问只会看到一台坏掉的站点
- **`/feed.xml`、`/sitemap.xml` 返回 401**：`SpaController` 的 SPA 回退正则用 `[^\.]*` 只匹配**不含点号**的路径，带扩展名的地址不会被回退命中，直接落到末尾「仅 `ROLE_admin`」的兜底规则。已在 `SecurityConfig` 显式放行这三个地址
- **每次访问都白下载数百 KB 外部字体**：`index.html` 从 `fonts.googleapis.com` 加载 Fraunces + Plus Jakarta Sans，但 `variables.css` 的 `--font-display`/`--font-sans` 从未引用这两个字体族（引用的是 `Lyon Text`/`Newsreader`/`SF Pro Display` 等从未加载的字体）。已移除这三条 `link`，字体改用系统字体栈
- **字体栈没有中文回退**：`--font-sans` 只有 `-apple-system`/`Segoe UI` 等西文字体，中文会落到浏览器默认字体，与西文混排时字重字距不一致；`--font-display` 引用的衬线体全部不存在，实际退化为 Georgia。已补 `PingFang SC`/`Hiragino Sans GB`/`Microsoft YaHei`/`Source Han Sans SC`/`Noto Sans CJK SC` 等中文回退

### 说明

`SeoController` 使用 `@Controller` + `@ResponseBody` 而非 `@RestController`：项目通过 `WebMvcConfig#configurePathMatch` 给所有 `@RestController` 自动加 `/api` 前缀，用后者会让订阅地址变成 `/api/feed.xml`，而订阅器与爬虫只会去根路径找。

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/.../controller/site/SeoController.java` | 新增：RSS / Atom / sitemap / robots |
| `backend/.../config/security/SecurityConfig.java` | 放行 `/feed.xml`、`/atom.xml`、`/sitemap.xml` 与 `/api/post/{id}/adjacent` |
| `backend/.../service/post/PostQueryService.java`（+Impl） | 新增 `getAdjacentPosts`：相邻文章的查询逻辑 |
| `backend/.../service/post/BlogPostService.java`（+Impl） | 按既有 facade 约定委托 `getAdjacentPosts` |
| `backend/.../controller/post/BlogPostController.java` | 新增 `GET /{id}/adjacent` |
| `backend/.../vo/post/AdjacentPostVO.java` | 新增：相邻文章 VO |
| `frontend/index.html` | 新增 SEO/OG/Twitter/订阅发现标签；移除无效的 Google Fonts |
| `frontend/src/composables/useSeoMeta.ts` | 新增：运行时维护页面级 meta 与 JSON-LD |
| `frontend/src/views/post/PostDetail.vue` | 文章加载后更新分享卡片、注入 JSON-LD、渲染上一篇/下一篇 |
| `frontend/src/api/post.ts` | 新增 `getAdjacentPosts` |
| `frontend/src/styles/variables.css` / `dark-mode.css` | 字体栈改为系统字体 + 中文回退；新增 `--like-accent*` 语义 token |

---

## v2.0.26 — 样式系统与路由修复 (2026-10-15)

### Bug 修复

- **十多个用户页面全部 404**: `router/index.ts` 只注册了 `homeRoutes`/`authRoutes`/`postRoutes`/`discoverRoutes`，**从未导入并展开 `userRoutes`**，导致 `router/modules/user.ts` 中定义的全部路由成为死代码：`/profile`、`/profile-edit`、`/user/:id`、`/password-change`、`/drafts`、`/collections`、`/following`、`/followers`、`/my-reports`、`/notifications`、`/messages` 均落到「页面不存在」。补上导入与展开（`vue-tsc` 不会报未使用模块，因此此前未被发现）
- **访客点赞/收藏/关注跳转到 404**: `LikeButton`/`CollectButton`/`FollowButton`/`Circle`/`CircleDetail` 等组件在访客操作时 `router.push('/login')`，但 `authRoutes` 是空数组，`/login` 没有对应路由。新增 `/login` 重定向到服务端下发的作者后台入口（`siteStore.adminRoot`），与 `guards.ts` 中 `requiresAuth` 的跳转约定一致
- **举报按钮跳转到 404**: `CircleDetail.vue` 跳转 `/report/post/{id}`，但缺少该路由，`views/common/Report.vue` 虽已实现却无法访问。新增 `/report/:type(post|comment|user)/:id(\d+)` 路由
- **`.env` 中文值乱码**: `DotenvConfig` 使用 `Properties.load(InputStream)` 按 ISO-8859-1 解码，导致 `.env.example` 中自带的 `ADMIN_NICKNAME=站长` 被读成 `ç«é¿`。改为按 UTF-8 读取
- **深色模式下代码块变成全页最亮元素**: `PostDetail.vue` 的 `.markdown-body :deep(pre)` 用 `--gray-900` 作深色背景，但深色模式会整体反转灰度梯度（`--gray-900` 变为 `#EBEBEB`），代码块因此变成刺眼的浅色块。改用不随主题反转的 `--code-bg`/`--code-text`
- **主按钮在深色模式下失去边界**: `Button.vue` 的 `.btn--primary` 硬编码 `#111111`，在 `#191919` 画布上几乎看不出按钮边界。改用语义化的 `--contrast-bg`/`--contrast-fg`，深色模式自动反转为浅底深字
- **`--gray-900` 被当作深色表面使用导致按钮意外反转**: `Login.vue`/`Register.vue` 的 `.btn-primary` 用 `--gray-900` + `--gray-800` 作背景，深色模式下变成浅色按钮。改用 `--contrast-*` 语义 token
- **登录/注册页 Logo 与导航栏不一致**: 两个页面的内联 SVG Logo 渐变硬编码为青色 `#0D9488`→`#14B8A6`，而全站主色是蓝色 `#165DFF`。改为引用 `var(--accent)`/`var(--accent-dark)`，同时让皮肤切换生效
- **`midnight` 皮肤的 `color-scheme` 从未生效**: `skins.css` 用后代选择器 `#selfblog[data-skin='midnight'] .dark-mode`，但 `App.vue` 把 `.dark-mode` 加在 `#selfblog` 自身，选择器永远匹配不到。补充同元素与后代两种形式

### 样式系统

- **清理遗留青色主题残留**: `Button.vue`、`Input.vue`、`Tag.vue`、`QuickComposer.vue`、`Circle.vue`、`Messages.vue`、`Notifications.vue`、`Profile.vue`、`PasswordReset.vue`、`NotFound.vue` 中残留的旧青色/绿色调色板（`#0D9488`/`#059669`/`#047857`/`#0F766E`/`#14B8A6` 及对应 `rgba()`）统一迁移到当前「清爽科技蓝」调色板
- **新增语义化设计 token**（`variables.css` + `dark-mode.css`）:
  - `--contrast-bg` / `--contrast-bg-hover` / `--contrast-bg-active` / `--contrast-fg` — 高对比实心表面，两个主题各自显式定义，语义稳定
  - `--code-bg` / `--code-text` / `--code-border` — 代码块专用表面，深色模式下仍保持深色
  - 补充注释说明「不要用 `--gray-900` 当深色背景」，因为深色模式会反转灰度梯度

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/com/selfblog/config/DotenvConfig.java` | `.env` 改按 UTF-8 读取 |
| `frontend/src/router/modules/auth.ts` | 新增 `/login` 重定向路由 |
| `frontend/src/router/modules/user.ts` | 新增 `/report/:type/:id` 路由 |
| `frontend/src/styles/variables.css` | 新增 `--contrast-*` 与 `--code-*` token |
| `frontend/src/styles/dark-mode.css` | 深色模式下的 `--contrast-*` 与 `--code-*` 定义 |
| `frontend/src/styles/skins.css` | 修复 `midnight` 的 `color-scheme` 选择器 |
| `frontend/src/components/common/Button.vue` | 主按钮 token 化 + 清理青色残留 |
| `frontend/src/components/common/Input.vue` | 清理青色残留 |
| `frontend/src/components/common/Tag.vue` | 清理青色残留 |
| `frontend/src/views/post/PostDetail.vue` | 代码块改用 `--code-*` |
| `frontend/src/views/auth/Login.vue` | Logo 渐变与主按钮 token 化 |
| `frontend/src/views/auth/Register.vue` | Logo 渐变与主按钮 token 化 |

---

## v2.0.25 (2026-10-14)

### 个人博客与一体化部署

- 将 Vue 前端、Spring Boot API 和 SPA 路由回退打包为单个可执行 JAR；默认 SQLite 持久化和 Caffeine 进程内缓存
- 保留原有文章数据，SQLite 初始化只增量建表；增加适用于已有 MySQL 数据库的非破坏性迁移脚本
- 增加匿名访客统计、不可逆访客/IP 摘要、匿名文章点赞、可开关访客评论和后台评论管理
- 增加公开/密码相册及短期内存访问令牌保护、背景音乐播放组件
- 增加可配置后台入口、站点资料、主题皮肤与主色设置
- 关闭访客注册并隐藏公共登录入口；公开阅读及匿名互动可用，内容写入和管理 API 限管理员
- 更新前后端 README、部署文档和 `.env.example`；首次启动管理员凭据由 `.env` 配置
- 将 Vue、Axios、DOMPurify 和 Vite 升级至修复版；`npm audit` 无已知漏洞
- 修复 SPA 历史路由回退误拦 `/assets/**`，避免 JS/CSS 返回首页 HTML 导致页面空白
- 将项目目录、Java 包名、Maven 产物及 npm 包统一重命名为 SelfBlog；保留旧 SQLite 文件名以兼容现有数据

---

## v2.0.24 (2026-06-24)

### Bug 修复

- **热度榜单同一文章重复占位**: `selectHotPosts` SQL 查询 7 天数据时，`blog_trending` 表按 `(post_id, date)` 每天存一条记录，导致同一篇文章最多出现 7 次。改为自连接 + `MAX(date)` 去重，每个文章只返回最近日期的记录
- **校友圈热门动态无时间范围过滤**: `getHotContent()` 查询热门校友圈动态时缺少时间范围限制，与文章的 7 天窗口不一致。增加 `createTime >= dateStart` 过滤条件
- **SQLite 不支持 GREATEST 函数**: 3 处 Mapper XML 使用 MySQL 特有的 `GREATEST()` 函数但缺少 SQLite 方言变体，导致运行时 `SQLITE_ERROR: no such function: GREATEST`。拆分为 MySQL/SQLite/H2 三种变体：
  - `TopicMapper.xml#decrementPostCount` — MySQL 用 `GREATEST()`, SQLite 用 `MAX()`, H2 用 `CASE WHEN`
  - `SysUserMapper.xml#decrementFollowerCount` — 同上
  - `SysUserMapper.xml#decrementFollowingCount` — 同上

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/resources/mapper/BlogTrendingMapper.xml` | selectHotPosts 自连接去重 |
| `backend/src/main/java/.../service/content/impl/TrendingServiceImpl.java` | circle 帖子加时间范围过滤 |
| `backend/src/main/resources/mapper/TopicMapper.xml` | decrementPostCount 拆分 MySQL/SQLite/H2 变体 |
| `backend/src/main/resources/mapper/SysUserMapper.xml` | decrementFollowerCount/decrementFollowingCount 拆分 MySQL/SQLite/H2 变体 |

---

## v2.0.23 (2026-06-10)

### 新功能

- **QuickComposer 快速发布组件**: Circle 页面新增 Twitter/X 风格快速发布器，支持文字、图片（最多9张）、视频（最多1个，100MB）、话题选择、位置输入、Ctrl+Enter 提交
- **视频上传预览**: QuickComposer 支持视频文件选择、预览播放、上传进度条
- **渐变占位背景**: Home.vue 无封面文章卡片显示渐变灰背景，统一卡片视觉结构

### 功能增强

- **Home 布局统一**: 移除 Featured 大卡片布局，所有文章统一使用 `bento-standard` 双列网格布局
- **热门文章页面**: `PostSearch.vue` 重命名为 `HotPosts.vue`，路由改为 `/hot-posts`

### Bug 修复

- **CORS 配置**: `.env:55` 全角逗号改为半角逗号，修复跨域请求问题
- **热门标签加载失败**: `Home.vue` 修复 `response?.data?.records || []` 响应解析
- **热门标签类型错误**: `trending.ts` `getHotTags()` 返回类型修正

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `QuickComposer.vue` | 新增组件（图片/视频上传、话题、位置） |
| `Circle.vue` | 集成 QuickComposer |
| `Home.vue` | 移除 Featured 布局、统一卡片、渐变占位 |
| `HotPosts.vue` | 从 PostSearch 重命名 |
| `trending.ts` | getHotTags 类型修复 |
| `.env` | CORS 逗号修复 |

---

## v2.0.22 (2026-06-09)

### Bug 修复

- **昵称修改不生效**: `SysUserServiceImpl.java` 移除 `htmlSanitizer.sanitizePlainText()`，与注册行为一致
- **私信头像不显示**: `Messages.vue` 内联SVG改为 `/default-avatar.png` 静态文件
- **时间显示错乱**: `application.yml` 添加 `write-dates-as-timestamps: false`，修复 LocalDateTime 序列化为数组的问题
- **草稿ID提取失败**: `PostEdit.vue` 兼容 `res.data` 为数字或对象格式
- **写文章加载旧内容**: `PostEdit.vue` 移除无条件 `fetchDraft()`
- **搜索不全面**: `PostQueryServiceImpl.java` 搜索增加 `summary` 字段匹配
- **移动端宽度不一致**: `PostSearch.vue` `max-width` 改为 `var(--container-xl)` + `@media 768px` 响应式
- **CirclePost.vue TypeScript**: `location` 类型修复 `null` → `undefined`

### 功能增强

- **弹窗@提及功能**: `Circle.vue` 弹窗添加 @用户搜索和选择功能
- **弹窗多话题选择**: `selectedTopic` 改为 `selectedTopics` 数组，支持多选
- **弹窗草稿保存**: 弹窗关闭时自动保存草稿，打开时恢复
- **CSS line-clamp**: 在 `utilities.css` + 7个组件中添加标准 `line-clamp` 属性
- **在线状态UI清理**: `Messages.vue` 移除不支持的在线状态显示

### 数据库

- **计数器同步脚本**: 新建 `fix-counter-sync.sql`，修复 `like_count`/`collect_count` 与实际记录不一致
  - 执行方式: `sqlite3 backend/campus_blog.db < backend/src/main/resources/fix-counter-sync.sql`

### 基础设施

- **前端环境配置统一**: 合并 `.env.development` 和 `.env.production` 为单一 `.env` 文件
- **Vercel 部署**: 添加 `vercel.json` 配置 `/api` 和 `/uploads` 代理
- **项目技能**: 新建 `.omc/skills/` 目录，包含 4 个项目技能

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `SysUserServiceImpl.java` | 移除昵称 sanitizer |
| `PostQueryServiceImpl.java` | 搜索增加 summary 字段 |
| `application.yml` | Jackson 时间配置 |
| `PostSearch.vue` | 响应式修复 |
| `PostEdit.vue` | 草稿逻辑修复 |
| `Messages.vue` | 头像修复 + 移除在线状态 |
| `PostDetail.vue` | 移动端 padding |
| `Circle.vue` | @提及 + 多话题 + 草稿 |
| `CirclePost.vue` | TypeScript 修复 |
| `utilities.css` | line-clamp 标准属性 |
| `fix-counter-sync.sql` | 新建 |

---

## v2.0.21 (2026-06-07)

### 前端交互优化

- **PostDetail.vue 乐观更新与防抖**: 点赞、收藏、关注操作添加防抖状态（`isTogglingLike`/`isTogglingCollect`/`isTogglingFollow`），防止重复请求；采用乐观更新策略，先更新 UI 再发请求，失败时自动回滚；使用后端返回的实际 `action` 状态和计数替代前端本地翻转
- **PostCardList.vue 乐观更新与防抖**: 点赞/收藏操作添加防抖 Set（`togglingLikes`/`togglingCollects`），防止同一文章重复操作；保存操作前状态快照，失败时完整回滚 Set 和计数；使用后端返回的 `likeCount`/`collectCount` 替代前端 ±1
- **Circle.vue 校友圈点赞优化**: 添加操作前状态备份，失败时完整回滚；使用后端返回的 `action` 和 `likeCount` 确保前后端状态一致
- **状态布尔值修复**: `PostDetail.vue` 中 `isLiked`/`isCollected`/`isFollowing` 赋值添加 `!!` 确保布尔类型，避免 falsy 值导致的状态异常

### 移动端导航重构

- **Navbar.vue 移动端菜单重构**: 移动端菜单拆分为 overlay + menu 双层结构，overlay 点击关闭菜单；修复移动端菜单层级问题（dropdown z-index 修复）
- **移动端样式优化**: 移动端搜索框移除 glassmorphism 改用实体背景提升可读性；导航链接添加 `min-height: 48px` 满足触摸目标尺寸要求；添加 `-webkit-tap-highlight-color` 和 `-webkit-overflow-scrolling` 改善触摸体验
- **移动端下拉菜单适配**: 768px 断点下 dropdown 改为全宽固定定位，优化移动端个人中心下拉菜单体验

### 类型定义修复

- **follow.ts FollowCounts 接口修正**: 字段名从 `followers`/`following` 改为 `followerCount`/`followingCount`，与后端返回字段对齐；新增可选 `userId` 字段

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/post/PostDetail.vue` | 乐观更新 + 防抖 |
| `frontend/src/components/home/PostCardList.vue` | 乐观更新 + 防抖 |
| `frontend/src/views/circle/Circle.vue` | 点赞优化 + 回滚 |
| `frontend/src/components/layout/Navbar.vue` | 移动端菜单重构 |
| `frontend/src/types/follow.ts` | 类型定义修正 |
| `frontend/src/utils/__tests__/toast.test.ts` | 测试修复 |

---

## v2.0.20 (2026-06-06)

### 多数据库 Mapper XML 支持

- **MyBatis DatabaseIdProvider 多方言**: 为 4 个 Mapper XML 添加 `databaseId` 属性，支持 MySQL / SQLite / H2 三种数据库方言自动切换
- **BlogPostTagMapper.xml**: `batchInsertPostTags` 新增 SQLite 方言（`INSERT OR IGNORE`）和 H2 兼容版本
- **BlogTrendingMapper.xml**: `upsert` 新增 SQLite 方言（`INSERT OR REPLACE`）和 H2 兼容版本（`MERGE INTO`）
- **SysUserMapper.xml**: `incrementLoginFailCount` 新增 SQLite 方言（`datetime` 函数）和 H2 兼容版本（`DATEADD`）；新增 `incrementFollowerCount`、`decrementFollowerCount`、`incrementFollowingCount`、`decrementFollowingCount` 原子计数操作
- **TopicMapper.xml**: `recalculateAllTrendingScore` 和 `recalculateAllPostCount` 新增 SQLite 方言（`json_each`）和 H2 兼容版本

### 后端测试完善

- **新增 11 个 Controller 测试**: BlogCollectControllerTest、BlogTagControllerTest、FollowControllerTest、MessageControllerTest、NotificationControllerTest、PasswordControllerTest、ReportControllerTest、ShareControllerTest、StatisticsControllerTest、TopicControllerTest、TrendingControllerTest
- **新增 11 个 Service 测试**: BlogCollectServiceImplTest、BlogTagServiceImplTest、CircleInteractionServiceImplTest、CircleQueryServiceImplTest、FollowServiceImplTest、MessageServiceImplTest、NotificationServiceImplTest、ReportServiceImplTest、ShareServiceImplTest、StatisticsServiceImplTest、TopicServiceImplTest

### 前端修复

- **user.ts 类型定义更新**: User 接口字段调整，补充可选属性
- **PasswordReset.vue 密码重置页面优化**: 改进密码强度检测、验证码重发逻辑、表单校验

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/resources/mapper/BlogPostTagMapper.xml` | 多数据库方言 |
| `backend/src/main/resources/mapper/BlogTrendingMapper.xml` | 多数据库方言 |
| `backend/src/main/resources/mapper/SysUserMapper.xml` | 多数据库方言 + 新增原子操作 |
| `backend/src/main/resources/mapper/TopicMapper.xml` | 多数据库方言 |
| `backend/src/test/java/.../controller/` (11 个) | 新增 Controller 测试 |
| `backend/src/test/java/.../service/impl/` (11 个) | 新增 Service 测试 |
| `frontend/src/types/user.ts` | 类型定义 |
| `frontend/src/views/auth/PasswordReset.vue` | 页面优化 |

---

## v2.0.19 (2026-06-06)

### 邮件服务修复

- **MAIL_FROM 配置未生效**: `EmailServiceImpl` 的 `fromEmail` 只读取 `spring.mail.username`，导致发件人始终等于 SMTP 账号。改为优先读取 `mail.from`（即 `.env` 中的 `MAIL_FROM`）
- **邮件发送失败后冷却时间未重置**: `sendVerificationCode()`（密码找回）发送失败时只清理了 `verificationStore`，未清理 `sendTimeStore`，导致用户失败后仍需等待冷却。已修复为与注册验证码逻辑一致
- **邮件模板硬编码有效期**: `buildEmailTemplate()` 中写死"5 分钟"，未使用配置的 `expireMinutes`。改为动态变量
- **注释与实际不符**: `generateSecureCode()` 注释写"8 位验证码"，实际为 6 位。已修正注释
- **application.yml 配置优化**: 移除无效的 `spring.mail.properties.mail.from`，新增独立的 `mail.from` 配置项；增加 SSL 支持（`ssl.enable`）适配 QQ 邮箱 465 端口
- **CORS 端口扩展**: `.env` 中 `CORS_ALLOWED_ORIGINS` 扩展为包含所有常见本地开发端口（3000/3001/5173/4173/8080/8081/4200/4201）

### 点赞/收藏计数修复

- **前端列表页乐观更新无回滚**: `PostCardList.vue` 点击点赞/收藏后先改前端计数再发请求，失败时仅 toast 错误未回滚计数。已添加请求失败后的计数和状态回滚逻辑
- **后端状态缓存未清除**: `toggleLike`/`toggleCollect` 未清除 `STATUS_CACHE`，导致点赞后 2 分钟内状态检查返回旧值。已添加 `@CacheEvict` 注解
- **管理员删除文章未清计数**: `adminDeletePost` 软删除点赞/收藏记录但未同步减少 `blog_post` 计数。已添加重置 `like_count=0` 和 `collect_count=0`
- **普通删除文章未清计数**: `deletePost` 同样未清理关联计数。已修复
- **校友圈点赞缺少上限保护**: `CirclePostMapper.xml` 的 `incrementLikeCount` 无上限保护（文章点赞有）。已添加 `CASE WHEN like_count < 100000000`

### 影响文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/.../service/auth/impl/EmailServiceImpl.java` | 邮件修复 |
| `backend/src/main/resources/application.yml` | SSL 配置 |
| `backend/.env.example` | MAIL_FROM 注释优化 |
| `frontend/src/components/home/PostCardList.vue` | 回滚逻辑 |
| `backend/src/main/java/.../service/social/impl/BlogLikeServiceImpl.java` | @CacheEvict |
| `backend/src/main/java/.../service/social/impl/BlogCollectServiceImpl.java` | @CacheEvict |
| `backend/src/main/java/.../service/post/impl/BlogPostServiceImpl.java` | 删除清计数 |
| `backend/src/main/resources/mapper/CirclePostMapper.xml` | 上限保护 |

---

## v2.0.18 (2026-06-05)

### 后端修复

- **缓存键冲突导致 ClassCastException**: `checkLikeStatus` 和 `checkCollectStatus` 共用 `STATUS_CACHE`，键格式均为 `userId:postId`，导致点赞状态缓存命中后返回 `LikeStatusVO` 给收藏接口。修复：缓存键添加前缀 `'like:'` 和 `'collect:'`
- **MyBatis Plus DatabaseIdProvider 缺失**: 新增 `DatabaseIdProvider` Bean 支持 XML 中 `databaseId` 多数据库方言（MySQL/SQLite），修复 SQLite 环境下 XML SQL 语句选择错误
- **TrendingServiceImpl UNIQUE 约束冲突**: `baseMapper.insert()` 改为 `baseMapper.upsert()`，解决 `blog_trending` 表 `UNIQUE(post_id)` 约束冲突
- **Logback 日志不写入文件**: 移除语法错误的 TurboFilter（`<filter>` 应为 `<turboFilter>`），修复 `default` profile 缺少文件 appender，修复 `dev` profile 主日志缺少 `ASYNC_FILE`

### 前端构建

- **TypeScript 编译通过**: `npm run build` 零错误
- **Vite 代理正常**: `/api` 代理到 `http://localhost:8825`，登录和 API 调用均正常

### 测试结果

- **55/55 GET 端点全部通过**（直连 + 代理双通道验证）
- **写入端点全部正常**: 文章创建、评论、点赞、收藏、私信、分享、校友圈、草稿、举报
- **预期 400 错误已验证**: "不能给自己发私信"、"不能举报自己"、"不能关注自己"

### 影响的文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/java/.../service/social/impl/BlogLikeServiceImpl.java` | 缓存键前缀 |
| `backend/src/main/java/.../service/social/impl/BlogCollectServiceImpl.java` | 缓存键前缀 |
| `backend/src/main/java/.../config/db/MybatisPlusConfig.java` | 新增 DatabaseIdProvider |
| `backend/src/main/java/.../service/content/impl/TrendingServiceImpl.java` | upsert 修复 |
| `backend/src/main/resources/logback-spring.xml` | TurboFilter + profile 修复 |

---

## v2.0.17 (2026-06-03)

### 后端修复

- **UserProfile.vue 关注/取消关注 API 路径错误**: `toggleFollow` 始终调用 `POST /follow`，从未调用 `DELETE /follow/{id}`。改为根据 `isFollowing` 状态分别调用 `followApi.toggleFollow`（关注）或 `followApi.unfollow`（取消关注）
- **@mentions 查询字段不匹配**: 前端插入 `@昵称`，后端 `parseMentions` 仅按 `username` 查询。改为同时查询 `nickname` 和 `username`
- **转发内容重复显示**: `repostContent` 被设为原动态内容，前端同时显示 `repostContent` 和 `repostPost.content`。改为不设置 `repostContent`
- **getMyLikes/getMyCollections 遗漏 NULL 记录**: `this.page()` 受 `@TableLogic` 影响只查 `is_deleted = 0`，遗漏历史 `is_deleted IS NULL` 记录。新增 Mapper 方法 `selectPageByUserId`/`countByUserId` 绕过 `@TableLogic`
- **新增 Mapper 方法**: `BlogLikeMapper.selectPageByUserId`/`countByUserId`、`BlogCollectMapper.selectPageByUserId`/`countByUserId`

### 前端修复

- **UserProfile.vue followerCount 使用后端实时计数**: 使用后端返回的 `followerCount` 替代本地 +/-1，避免并发场景下的计数偏差
- **PostCardList.vue 删除遗留空函数**: 删除 `openPreview` 空函数，消除死代码
- **图片预览功能为空函数**: `PostCardList.vue` 的 `openImagePreview` 和 `openPreview` 不发出事件。添加 `preview-image` emit 事件，`Home.vue` 监听并调用 `handlePreview`
- **阅读量双重计数**: `PostDetail.vue.fetchPost` 中后端 `getPostById` 已增加阅读量，前端又额外调用 `incrementViewCount`。移除前端重复调用
- **代码块复制按钮 CSS 类名不匹配**: `PostDetail.vue` 用 `copy-code-btn`，`main.css` 定义 `copy-btn`。改为统一使用 `copy-btn`
- **分页不同步到 URL**: `Home.vue.changePage` 未调用 `router.push`，浏览器前进/后退无法用于分页。添加 `router.push` 更新 URL query
- **前端忽略后端实时计数**: `PostCardList.vue`/`PostDetail.vue` 的 `toggleLike`/`toggleCollect` 使用 +/-1 乐观更新。改为使用后端返回的 `likeCount`/`collectCount`

### 测试修复与完善

- **CircleServiceImplTest 编译修复**: 测试调用 `createPost` 时参数数量不匹配（11 个参数，方法只需 10 个）。移除多余参数，新增 `createPost_WithContent_ShouldStoreContentCorrectly` 测试验证内容存储正确性
- **BlogLikeServiceImplTest 新增测试**: 新增 `testGetMyLikes_ShouldIncludeNullIsDeleted` 测试，验证 `getMyLikes` 能正确处理 `is_deleted IS NULL` 的历史遗留数据

### 影响的文件

| 文件 | 改动类型 |
|------|----------|
| `frontend/src/views/user/UserProfile.vue` | 逻辑修复 |
| `frontend/src/components/home/PostCardList.vue` | 逻辑修复 |
| `frontend/src/views/Home.vue` | 逻辑修复 |
| `frontend/src/views/post/PostDetail.vue` | 逻辑修复 |
| `backend/src/main/java/.../service/impl/CircleServiceImpl.java` | 逻辑修复 |
| `backend/src/main/java/.../mapper/BlogLikeMapper.java` | 新增方法 |
| `backend/src/main/java/.../mapper/BlogCollectMapper.java` | 新增方法 |
| `backend/src/main/java/.../service/impl/BlogLikeServiceImpl.java` | 逻辑修复 |
| `backend/src/main/java/.../service/impl/BlogCollectServiceImpl.java` | 逻辑修复 |

---

## v2.0.16 (2026-06-03)

### 后端修复

- **MySQL 连接**: JDBC URL 添加 `allowPublicKeyRetrieval=true`，修复 `Public Key Retrieval is not allowed` 错误
- **MyBatis 配置**: 添加 `use-actual-param-name: false`，确保 `@Param` 注解优先于参数实际名称
- **`@TableLogic` 死代码修复**: `BlogLikeServiceImpl.toggleLike` 和 `BlogCollectServiceImpl.toggleCollect` 中，通过 `LambdaQueryWrapper` 查找已删除记录的代码因 `@TableLogic` 自动追加 `WHERE is_deleted = 0` 而成为死代码。改为使用自定义 SQL（`selectRawByUserAndPost`）绕过 `@TableLogic`
- **批量状态检查修复**: `checkLikeStatusBatch` 和 `checkCollectStatusBatch` 使用 `LambdaQueryWrapper` + `this.list()` 遗漏了 `is_deleted IS NULL` 的历史记录。改为使用自定义 SQL（`selectActivePostIdsByUserAndPosts`）
- **`BlogCollect` 实体修复**: `isDeleted` 字段补充默认值 `= 0`，与 `BlogLike` 实体对齐
- **`CirclePostMapper.countByTopicIds` 修复**: 删除 `@MapKey("topicId")` 注解，改为 default 方法在 Java 层完成 Map 转换，解决 `There is no getter for property named 'topicId' in 'class java.lang.Long'` 错误
- **新增 Mapper 方法**:
  - `BlogLikeMapper.selectActivePostIdsByUserAndPosts` — 批量查询用户已点赞的文章ID（绕过 `@TableLogic`）
  - `BlogCollectMapper.selectRawByUserAndPost` — 绕过 `@TableLogic` 查询收藏记录
  - `BlogCollectMapper.selectActivePostIdsByUserAndPosts` — 批量查询用户已收藏的文章ID（绕过 `@TableLogic`）

### 前端修复

- **`PostCardList.vue` emit 事件名修复**: `emit('update:likedPosts')` → `emit('update:liked-posts')`，`emit('update:collectedPosts')` → `emit('update:collected-posts')`，与父组件 Home.vue 的 kebab-case 监听器匹配
- **`PostCardList.vue` props 修复**: 不再直接修改 props 中的 Set，改为通过 `emit` 事件通知父组件更新状态

### 影响的文件

| 文件 | 改动类型 |
|------|----------|
| `backend/src/main/resources/application.yml` | 配置修改 |
| `backend/src/main/java/.../mapper/BlogLikeMapper.java` | 新增方法 |
| `backend/src/main/java/.../mapper/BlogCollectMapper.java` | 新增方法 |
| `backend/src/main/java/.../entity/BlogCollect.java` | 字段默认值 |
| `backend/src/main/java/.../service/impl/BlogLikeServiceImpl.java` | 逻辑重写 |
| `backend/src/main/java/.../service/impl/BlogCollectServiceImpl.java` | 逻辑重写 |
| `backend/src/main/java/.../mapper/CirclePostMapper.java` | 方法重写 |
| `frontend/src/components/home/PostCardList.vue` | 事件名修复 |
| `frontend/src/views/Home.vue` | 事件监听 |

---

## v2.0.15 (2026-05-30)

- HomeFilters.vue `filters is not defined` 运行时错误修复
- PostDetail.vue 文章评论幽灵数据问题（records 解析修复）
- Circle.vue 点赞失败无错误提示问题修复
- Circle.vue 热门动态移至左侧边栏，优化数据源
- Circle.vue 侧边栏数据每 60 秒自动刷新
- logger.js CORS 错误静默处理（/api/log 端点不存在）
- 校友圈移除标签功能（仅保留话题）

## v2.0.14 (2026-05-29)

- 密码重置字段名修复（password → newPassword）

## v2.0.13 (2026-05-28)

- 文章话题功能移除（标签功能保留）
- 校友圈话题功能修复并完善
- 话题数据路径修复（data.data.records）
- PostEdit.vue import 语句补全

## v2.0.11 (2026-05-25)

- 修复写文章选择标签时白屏的运行时错误

## v2.0.0 (2026-05-20)

- Rainy Glassmorphism UI 设计系统（水滴、涟漪、光泽动效）
- 16 项性能优化索引（查询速度提升 300 倍）
- Caffeine 缓存策略（5 个缓存实例）
- 校友圈功能增强（@mentions、位置标签、话题标签、显示切换）
- Messages 布局修复（浮动返回按钮）
- 所有页面宽度统一扩展至 1400px
