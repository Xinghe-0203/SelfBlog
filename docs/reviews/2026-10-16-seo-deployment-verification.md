# 可抓取 HTML 与统一部署验证记录（2026-10-16）

## 范围与结果

本批实现已确认的设计规格（原存于 `docs/superpowers/specs/2026-10-16-crawlable-html-and-deployment-design.md`，该目录已于 2026-10-19 清理）：
公开阅读 HTML、完整分片站点地图、动态 robots、共享 canonical origin，以及 Docker / Vercel 部署适配。
**未改变全站字号与页面比例，未迁移框架，未改动现有数据库与上传目录。**

| 检查 | 实际结果 |
| --- | --- |
| 后端完整单测 | **545 测试 / 0 失败 / 0 错误 / 0 跳过** |
| 前端完整单测 | **41 文件 / 393 测试通过** |
| 前端类型 / 构建 | `vue-tsc --noEmit` 与 `vite build` 通过 |
| 部署契约测试 | `npm run test:deployment` **27/27**（Vercel 路由生成器 21 + Docker/Compose 源契约 6） |
| 本轮改动文件格式 | 仅用 `prettier --check --end-of-line auto` 检查本轮文件，通过 |
| 单 JAR 打包 | `backend/target/selfblog.jar` 构建成功，内嵌最新 `frontend/dist` |
| 真实 SQLite/JAR 联调 | **9/9 通过**（含无 JavaScript 抓取、站点地图完整性、伪造 Host、后台域名即时生效/回落、下线 404、本地代理传输） |
| 上一批浏览器回归 | **31/31**（`selfblog-pages-live.cjs` 复跑，含登录/发布/互动/相册/音乐/页面管理/七档宽度） |
| Docker 容器运行 | **未验证**（本机无 Docker 守护进程）；仅静态配置契约测试 |
| 真实 Vercel 部署 | **未验证**（无账号、域名、公网源站）；仅生成器单元测试与本地同形代理冒烟 |

## 真实联调证据（8/8）

运行方式：全新临时 SQLite 与上传目录，启动实际 `selfblog.jar`，Playwright(Chrome) 与原生 HTTP 客户端直连；
`SITE_BASE_URL` 指向本次实例的正式地址。未写入用户现有实例、数据库或上传目录。

1. **无 JavaScript 也能读取**：禁用 JS 打开 `/post/{id}` 得到 200、正文段落、`index,follow`、正确的 canonical 与 BlogPosting JSON-LD；首页链接该文章；缺失路径是真正 404 + noindex。
2. **伪造 Host 无效**：带 `Host: attacker.test` 请求 `/` 与 `/sitemap.xml`，页面 origin 标记、canonical 与站点地图仍使用配置的正式 origin，攻击者域名未出现。
3. **关于页与自建内容页**：通过真实管理 API 保存并发布草稿后，禁用 JS 也能读到正文；发布后进入站点地图。
4. **站点地图完整性**：从 `/sitemap.xml` 展开全部分片并集，覆盖接口返回的每一篇已发布文章、已发布自建页与站点页面；不含 `/search`、`/admin` 或未知文章。
5. **robots**：包含配置的后台路径（默认 `/admin`）、`/search` 与正式 sitemap 地址。
6. **下线即不可读**：`DELETE …/publication` 后，禁用 JS 访问 `/pages/{slug}` 为 404 + noindex，站点地图不再列出该页分片。
7. **Vue 接管无重复**：启用 JS 打开文章页后 `.seo-reading` 数量为 0（服务端阅读 DOM 被挂载替换），正文仍可读，canonical 保持配置 origin；`/admin` 与 `/search` 为 noindex。
8. **本地同形代理传输**：把同一套“单 catch-all 转发 + no-store + DENY”规则放在本地 Node 代理后，HTML/站点地图 200、缺失 404、`POST /api/user/login` 带体转发 200、匿名管理接口 401、`X-Frame-Options: DENY` 且无重复头。**这只证明传输与头策略一致，不代表真实 Vercel CDN/TLS 行为。**

## 联调发现并修复的产品缺陷

真实 JAR 联调（而非单测）发现两个单测结构上看不到的问题，均先复现再修复：

1. **缺失页面返回 500 而不是 404（严重）**：`ReadingPageService` 的类级只读事务与内部查询共享事务，内层
   `PageService/PostQueryService` 的 404 使事务被标记为 rollback-only，提交时抛
   `UnexpectedRollbackException`。JAR 日志中的堆栈直接定位到该异常。
   **影响**：生产环境所有缺失文章/标签/自建页都会 500，而包裹在测试事务里的单测无法观察到。
   修复：移除类级 `@Transactional`，让每个内层查询使用独立只读事务；新增
   `@Transactional(propagation = NOT_SUPPORTED)` 回归（下线后 404、未知文章/标签 404）。
2. **空标签集合的站点地图自相矛盾**：索引列出了 `sitemap-tags-1.xml`，该分片却返回 404
   （`shardCount` 用 `Math.max(1, …)` 把空集合当成 1 片）。修复：空集合没有分片，索引与分片
   使用同一判定；新增回归 `emptyCollectionsAreNotAdvertisedAndTheirShardsAreReal404`。

同时修正两类验证脚本缺陷（不是产品问题）：本地代理冒烟重复输出 `X-Frame-Options`（大小写不同的键各写一次）；联调脚本未保存 JAR 日志、未强制结束 JAR 进程（Windows 下 `kill()` 后进程仍存活，导致后续打包被文件锁阻塞）。脚本现保存日志、用 `taskkill /F /T` 收尾并改用 JDK 21 启动。

## 已覆盖的自动化契约（摘选）

- **canonical/origin**：19 项（配置优先级、非法输入、IPv6/端口、绝对地址白名单、Markdown 安全渲染）。
- **严格公开读取**：6 项（作者身份也不能读草稿、软删除、元数据不选正文、稳定分页与参数校验）。
- **阅读模型/文档**：16 项（页面族、模块、noindex、JSON-LD 转义、模板缺失 503、无重复 head）。
- **HTTP 交付**：7 项（GET/HEAD、no-store、真实 404、`/index.html` 308、伪造头、API/资源边界）。
- **发现文档**：5 项（robots 动态化、索引与分片、1001 篇跨分片完整、feed 仍 100 条上限）。
- **前端 origin**：3 项（服务端标记优先、无标记回落、畸形标记忽略）。
- **部署适配**：27 项（路由保持路径/方法/查询、no-store、Preview noindex、非法/环回 origin 失败、Docker 根上下文与持久化目录、安全头、Compose 卷名保留）。

## 与既有工作流的兼容

- 新增页面只是“首次响应可读”，Vue 挂载后行为不变；联调用启用 JS 的浏览器确认阅读 DOM 被替换且无重复。
- 后台、搜索、历史账户路径仍是启动壳 + noindex；API、上传与既有界面未改动。
- 公开 HTML 使用独立的“仅已发布”查询，不调用会（按设计）允许作者读取自己草稿的接口；阅读请求不增加阅读量、不写访客事件。
- 上一批 31 项浏览器工作流用同一脚本复跑 **31/31 通过**：管理员登录、草稿/发布、匿名互动与审核、密码相册、上传音乐跨页播放、站点设置、页面管理草稿/预览/发布/导航/版本冲突、七档宽度导航与 44px 触达全部保持。唯一改动是把“站点地图单文件直接含文章 URL”的旧断言升级为“索引列出分片、分片内含文章 URL”（新契约下的更强检查，跟随索引实际抓取）。

## 文章发布修复（2026-10-17 复查追加）

复查“文章发布各项功能”时发现并修复 4 个真实缺陷，全部先复现再修复：

1. **编辑文章后标签永久消失（严重，SQLite/MySQL）**。真实 JAR + 全新 SQLite 复现：新建带标签文章 → 标签正常；再次编辑并保留同一标签 → 标签变空；重新添加 → 仍为空。根因：`blog_post_tag` 有 `UNIQUE(post_id, tag_id)`，编辑时逻辑删除旧关联后，`INSERT IGNORE` 被仍占据唯一键的旧行静默忽略。修复为方言 upsert 并把 `is_deleted` 复位；H2 测试库补上同样的唯一键（此前缺失，所以单测不可见）。
2. **媒体关联查询未过滤软删除**：`BlogPostMediaMapper.selectByPostId` 会把“编辑文章时移除的旧配图”继续返回给编辑器与公开接口；同时该 mapper 缺少 H2 兜底语句（H2 运行直接报 `Invalid bound statement`）。已补 `is_deleted = 0` 与兜底批量插入。
3. **混用标签时新标签被静默丢弃**：编辑器同时发送 `tagIds`（已有标签）与 `tagNames`（新输入），后端旧逻辑“tagIds 非空即忽略 tagNames”。已改为并集解析（名字 `getOrCreate` 后与 id 去重合并）。
4. **写作页标签输入上限 50 字与后端 20 字不一致**：与后台标签管理页对齐为 20 字，仍以后端校验为准。
5. **同类排查（同批修复）**：标签删除后重建同名标签（唯一名称 + 逻辑删除 → 旧代码返回 null/NPE）；趋势表每日 upsert 未复位删除标记且查询未过滤软删行；文章接口 `category`/`coverImage` 缺长度校验（库列 50/500，超长报 500）；H2 测试库补齐生产已有的 7 个唯一键（此前仅 2 个），消除“生产有约束、测试没有”的盲区。
6. **唯一键冲突的异常类型不可移植（真实联调两轮才修净）**：Spring 只把 H2/MySQL 的唯一键冲突翻译成 `DuplicateKeyException`；SQLite（SQLState null、error code 19）实际抛出 `UncategorizedSQLException`。第一版只捕获 `DuplicateKeyException`、第二版只捕获 `DataIntegrityViolationException`，都在真实 SQLite 上继续 500，而本机 H2 测试是绿的。最终统一捕获 `DataAccessException` 并**只在找到冲突行时转换**。同类共 5 处（标签、页面地址预留 → 409、关注、动态点赞、用户创建、话题创建）。验证：`BlogTagConflictTest`、`PageSlugConflictTest`（使用真实 SQLite 异常形态）+ 真实 JAR 联调第 11 项。

回归与证据：

- `PostTagLifecycleTest`：编辑保留、移除、重新添加、`tagIds`+`tagNames` 混用（RED→GREEN）。
- `BlogPostMediaMapperTest`：替换媒体后不返回软删行（先因缺少 H2 语句 RED，再补语句+过滤 GREEN）。
- `PostEdit.test.ts`：输入上限 20 与发布载荷同时包含 id 与名字。
- 同类排查新增：`BlogTagLifecycleTest`（删除后重建恢复原行、发布流程复用已删名称）、`BlogTrendingMapperTest`（每日 upsert 复位删除标记）、`BlogPostControllerTest`（分类/封面超长返回 400 而不是数据库 500）；测试库补齐生产唯一键。
- 真实联调新增“编辑已发布文章保留标签、移除后可恢复”（第 10 项）与“标签删除后重建恢复原行”（第 11 项）；浏览器回归新增“编辑器标签输入上限 20”，单独复跑 **32/32** 通过（曾与另一套联调并行运行时，About 发布响应等待 12 秒超时；单独运行时同一检查通过，判定为资源竞争而非产品问题）。
- 当前闸门：后端 559 / 前端 394 / 部署契约 27；Docker、MySQL、真实 Vercel 仍未验证。
- 环境备注：连续多轮联调后出现 Chrome 残留进程（23 个）与一次 `ERR_NO_BUFFER_SPACE`；清理残留并顺序重跑后恢复。清理过程中误停了用户此前启动的 Vite 开发服务器（127.0.0.1:3000），已用相同命令立即重启并确认 HTTP 200。

## 站点域名后台化（2026-10-16 追加）

作者可直接在后台「网站设置 → 站点资料 → 网站域名」修改 canonical origin，无需改服务器配置：

- **优先级**：后台设置 → `SITE_BASE_URL` 环境变量 → 直连请求地址（本地开发）。保存即生效（设置缓存被清理），清空后回落到 `.env`。
- **校验**：仅接受 `http(s)` 站点 origin（可带端口）；路径、查询串、账号密码、多值等一律拒绝并提示中文原因；留空表示“未设置”。
- **坏数据安全**：绕过接口直接改坏 `site_setting` 时，服务端记录 WARN 并回落，不让整站 500。
- **测试**：`SiteOriginTest` 新增后台优先/清空回落/坏数据回落（共 18 项）；`SiteSettingsServiceImplTest` 新增可保存、可清空、7 类非法输入 400（共 10 项）；`DiscoveryControllerTest` 新增“保存后 robots/首页/sitemap 立即使用新域名、清空后回落”（后端合计 545 项）。前端 `Settings.test.ts` 覆盖字段渲染、保存载荷、`.env`/sitemap 提示与后端错误提示（前端合计 393 项）。
- **真实联调**：在第 9 项检查中通过管理 API 写入 `https://admin-configured.example.com`，断言 robots、首页 origin 标记与 sitemap 立即变化；再清空并断言回落到实例的 `SITE_BASE_URL`。
- **适用边界**：采用 Vercel 时，Vercel 项目构建期仍用它自己的 `SITE_BASE_URL` 环境变量做路由校验；运行期地址以本设置优先级为准，建议两处保持一致。

## 已知限制与未验证项

- **真实 MySQL**：未运行；站点地图/SQL 未使用 MySQL 专有语法，但未在 MySQL 实例上执行。
- **Docker 镜像**：本机无 Docker 守护进程，未构建/运行镜像；仅脚本化的源文件契约检查。
- **Vercel 实际部署**：未创建项目、未绑定域名、未验证 CDN 缓存/TLS/Preview 行为；生成器逻辑有单测，本地代理冒烟只覆盖传输形状。
- **抓取收录**：本批建立“无需 JavaScript 即可读取”与完整的发现文档，**不代表搜索引擎已收录**，也不承诺排名。
- 外部平台（搜索控制台）提交需要用户自己的账号与域名。

## 可复跑命令

```powershell
# 后端（JDK21，必须带两个 skip 参数）
mvn test "-Dskip.npm=true" "-Dskip.installnodenpm=true"

# 前端
npm run type-check; npm run test:run; npm run test:deployment; npm run build

# 单 JAR 打包（已构建过前端时）
mvn package -DskipTests "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

真实联调脚本与临时数据目录只保留在本地（`%TEMP%\opencode\`），不作为仓库 CI 入口；本文区分“已执行结果”与“未验证限制”。
