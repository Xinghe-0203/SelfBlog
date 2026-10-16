# 可抓取 HTML 与统一部署验证记录（2026-10-16）

## 范围与结果

本批实现已确认的[设计规格](../superpowers/specs/2026-10-16-crawlable-html-and-deployment-design.md)：
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
