# 可抓取 HTML 与统一部署设计

日期：2026-10-16

状态：用户已确认架构方向；本书面规格待审阅。尚未实施或部署。

## 1. 目标与约束

在已经交付的页面管理之上，让公开文章与内容页在首次 HTTP 响应中就包含可阅读正文、正确元数据和真实状态码；补齐站点地图、Docker 与可选 Vercel 入口。

- 保留 Vue、Spring Boot、单 JAR 和现有数据库；不迁移 SSR 框架、不增加常驻 Node 服务。
- 保持已确认的导航、音乐、页面样式与比例，**不做全站放大**。
- 公开读取只使用已发布内容，不能因为访问者是管理员而向 HTML 输出草稿。
- 保留文章发布、匿名互动、密码相册、页面预览与后台登录等现有流程。
- 正式域名暂未提供，沿用可配置的 `SITE_BASE_URL`；不写死参考站域名。
- 本批建立“可以抓取”的技术条件，不承诺收录、排名或外部平台审核结果。

## 2. 已核实的现状

| 证据 | 现状与影响 |
| --- | --- |
| `frontend/index.html`、`controller/SpaController.java` | 首次响应是通用 head 与空 `#app`，公开正文依赖 JavaScript |
| `frontend/src/composables/useSeoMeta.ts` | 换页元数据已维护，但绝对地址依赖浏览器 origin，不能替代首次响应 |
| `controller/site/SeoController.java` | RSS、Atom、sitemap 共用最多 100 篇文章查询；站点地图包含搜索、遗漏自建页；robots 写死 `/admin` |
| `service/site/PageService.java`、`service/site/page/PageCatalog.java` | 已有草稿/公开分离、页面 SEO 设置、模块与样式白名单，可复用已发布配置 |
| `frontend/src/router/` | 首页已有 `?page=`；文章、标签、自建页及动态后台入口需要区分；部分历史用户路由仍注册 |
| `backend/pom.xml` | 前端构建通过 Maven 插件嵌入 JAR，产物是 `selfblog.jar`；已有 Jsoup，没有 Markdown 解析依赖 |
| `backend/Dockerfile`、`backend/docker-compose.yml` | 构建镜像未提供 Maven、上下文缺少前端、JAR 名错误；现有 Compose 选择 MySQL |
| `backend/.env.example`、`backend/DEPLOY.md` | 单 JAR 默认 SQLite；媒体包括 `uploads/` 与 `gallery-media/`，均需持久化 |
| `frontend/vercel.json` | 仍指向参考域名，上传前缀错误，未统一转发页面 HTML 与订阅源 |
| 本地环境 | Docker 命令当前不可用；不能把配置静态检查写成镜像运行通过 |

基线为 `39cface`：前端 41 文件 / 389 测试，后端 491 测试，上批隔离 SQLite/单 JAR 浏览器联调 31/31。以上是上一批证据，不代表本设计已经验证。

## 3. 方案与取舍

**采用：JAR 输出公开阅读 HTML，Vue 保留交互；Vercel 可作为统一反向代理。**

JAR 是 HTML、哈希资源、API、上传与订阅的唯一版本来源。Vercel 不再单独构建另一份 Vue 页面，避免独立构建时 HTML 引用不存在的资源；Java 和数据库始终运行在服务器或容器。

未采用的替代方案：

1. Vercel 独立静态前端加后端 HTML：需要协调两个发布产物、资源版本与回退规则，容易再次让首页回到空 SPA。
2. 迁移完整 Vue SSR/Nuxt：可统一渲染组件，但涉及运行时、路由、鉴权和部署迁移，超出本轮必要改动。
3. 只修 XML 和客户端 meta：成本低，但普通 HTTP 抓取仍没有文章正文，不能完成核心目标。

服务端阅读 HTML **不是 Vue SSR/hydration**。它是给所有访问者一致返回的安全阅读内容；Vue 启动时在原 `#app` 挂载并替换它。不能按 User-Agent 给爬虫和访客返回两套不同文章。

## 4. 请求与数据边界

职责拆分如下，实施时按现有分层安排文件：

| 单元 | 职责 | 依赖/限制 |
| --- | --- | --- |
| 站点地址解析 | 验证配置，生成 canonical、媒体绝对地址及订阅链接 | 不读取正文，不处理鉴权 |
| 公开页面模型 | 分类路由，组合真实公开数据与已发布页面配置，确定状态与索引规则 | 复用 PostQueryService、PageService 等查询服务，不读取管理草稿 |
| HTML 文档渲染 | 读取打包的 index、填充 head 与阅读区、净化 Markdown | 不直接查库，不执行作者模板或脚本 |
| 发现文档服务 | 生成 sitemap 分片、robots、RSS/Atom | 分页查询公开元数据，不加载所有文章正文 |
| 部署适配 | Docker 构建与存储、Vercel 构建输出路由 | 不修改业务数据库或发布规则 |

公开 HTML 查询是只读事务；GET/HEAD 不增加阅读量、不生成访客事件、不修改任何草稿。已有浏览器阅读统计继续走原来的明确操作，不能因增加 HTML 渲染而重复计数。

文章读取明确限定 `status=1` 和未软删除；自定义 SQL 也须包含软删除条件。即使带管理员认证访问未发布文章/自建页，公开 HTML 也返回 404。相册只使用公开允许的标题/说明，不输出受保护图片地址、解锁令牌或文件路径。

## 5. 路由、状态与索引规则

下表针对直接 GET/HEAD。SPA 内换页不能追溯修改已经收到的 HTTP 状态，但必须显示正确的缺失/失败状态并维护相同的元数据规则。

| 路径 | 首次 HTML 内容 | 状态/收录 |
| --- | --- | --- |
| `/`、有效 `/?page=N` | 真实文章列表、公开导航、页面介绍和实际分页链接 | 200；遵循首页配置；第 1 页 canonical 为 `/`，其他页保留有效 page |
| `/archives` | 归档说明与有界公开文章链接列表，提供首页分页及 sitemap 入口 | 200；遵循归档配置 |
| `/about` | 已发布标题、说明、独立 Markdown 及启用的作者/站点模块 | 200；遵循关于配置 |
| `/gallery` | 页面说明和公开相册安全信息；解锁/查看仍由原交互处理 | 200；遵循相册配置 |
| `/post/{id}` | 真实标题、完整净化正文、允许显示的信息及公开阅读链接 | 已发布且存在为 200；其余 404；遵循文章模板索引配置 |
| `/tag/{id}` | 真实标签名与有界公开文章列表 | 存在为 200，允许空列表；不存在/非法 ID 为 404；遵循标签模板配置 |
| `/pages/{slug}` | 已发布内容页完整净化正文与启用模块 | 已发布且存在为 200；否则 404；遵循该页配置 |
| `/search` | 搜索说明/启动提示，不向 HTML 拼接任意查询正文 | 200，强制 noindex |
| 当前后台路径及其子路径 | 通用后台启动壳，不输出账户、编辑器或草稿数据 | 200，强制 noindex；鉴权继续由现有 API/路由守卫执行 |
| `/login`、现存用户/账户路由 | 保留现有启动、登录跳转及守卫语义，不增加服务端账户查询 | 通用壳，强制 noindex |
| 其他公开非资源路径 | 已发布 404 配置的安全提示与首页/搜索入口 | 真正 HTTP 404，强制 noindex |

- 优先识别当前后台命名空间；旧站后台为 `/pages` 时，不抢占其 `/pages/*`。
- 资源/API/Actuator/文档路径不进入 HTML 回退；缺失 JS/CSS/图片不能返回成功的 HTML。`/index.html` 作为入口别名归一到 `/`，不保留独立可收录副本。
- 首页 page 只接受正整数，按同一规则规范化；真实超出范围的非第一页返回 404。未知跟踪参数和 fragment 不进入 canonical，不生成无限查询组合。
- 无文章的首页仍可正常返回 200。普通列表首屏按现有页长呈现，归档/标签最多 100 条初始链接；不新增标签/归档分页 URL，完整发现由站点地图承担。
- 数据源/渲染故障返回安全 5xx 与 noindex，保留重试语义，不能伪装成 404 或成功空页。打包 index 缺失时 HTML 返回 503，不把空模板当作完成。

## 6. 安全阅读 HTML 与交互接续

1. 从 JAR 中实际构建的 `static/index.html` 创建每次请求独立文档，保留 Vite 生成的模块、CSS、预加载、图标和订阅引用。不可并发修改一个共享可变 Document。
2. 在 `#app` 内放入带标题层级、`main/article/nav/time` 的阅读内容；普通链接可在禁用 JavaScript 时使用。Vue 挂载后移除整段旧 DOM，不能产生重复正文、重复导航或双重互动。
3. 使用 Java CommonMark 解析 Markdown，再经过已有 Jsoup 的受控 HTML 白名单净化。原始 HTML 不作为可信输入；禁止脚本、事件属性、iframe、SVG、任意样式、危险 URL 和可执行嵌入。表格等非核心扩展不改变原文语义，完整增强阅读仍由现有 Vue 提供。
4. 文本/属性分别正确转义；JSON-LD 使用安全 JSON 序列化并转义可终止 script 的字符。测试必须包含 `</script>`、事件属性与脚本协议等输入。
5. 初始阅读样式仅作用于该阅读区域，复用系统字体/主题 token，不覆盖全局字号、导航或已批准组件。受控页面样式和模块开关同样作用于初始内容；不执行存储的 CSS/JavaScript。
6. 不把用户 token、管理设置、草稿、解锁状态或大份后台文档嵌入 HTML。没有公开预览路由；现有认证 srcdoc 预览保持不变。

## 7. Canonical、分享信息与运行时一致性

- `SITE_BASE_URL` 表示**访客正式站点 origin**，例如 `https://blog.example.com`，不是源站 API 地址。只接受合法 http(s) origin，可带必要端口；拒绝凭据、路径前缀、query、fragment、反斜线、控制字符及多值输入。无效显式配置需尽早报错。
- 未配置时不阻止本地/旧站启动，可从经过验证的直接请求 scheme/host/port 推导；不再直接信任原始 `X-Forwarded-Host/Proto`。生产指南要求显式设置正式 origin，以避免代理内网地址和 Host 干扰。
- 一旦配置，伪造 Host/转发头不得改变 canonical、sitemap 或分享图的站内绝对地址。默认不基于 Host 强制跳转正式域名，避免 Vercel 回源循环。
- HTML 安全携带已解析 origin，客户端元数据工具沿用它；Vite 本地开发没有该标记时回落当前浏览器 origin。不能挂载后立即把 canonical 改回源站域名。
- title、description、图片、indexable 的优先级与页面管理保持一致；文章/标签 SEO 标题只作后缀，保留真实身份。搜索、404、后台、预览及查询错误不能被配置成可收录。
- 首次响应包含正确 title、description、canonical、robots、Open Graph、Twitter；文章另有作者/日期与 BlogPosting JSON-LD。不伪造作者、更新时间或不存在的图片。
- 客户端换页须清理旧文章/结构化数据；初始与运行时不能堆叠重复 canonical/meta。允许收录的页面以页面配置为准，不把“允许”写成“已收录”。

## 8. Sitemap、robots 与订阅

### 8.1 完整而有界的站点地图

- `/sitemap.xml` 返回标准 sitemap index，指向站内分片，而不再是仅 100 篇的截断 urlset。
- 子文档使用根路径 `/sitemap-site.xml` 及 `/sitemap-posts-N.xml`、`/sitemap-tags-N.xml`、`/sitemap-pages-N.xml`，N 从 1 开始。名称含点号，不可能成为现有后台入口，避免为了新增分片命名空间而迁移用户后台路径。
- 每片最多 1000 个 URL，按稳定 ID/键排序分页。只查询必要的 URL/更新时间列，不能把所有正文一次读进内存；每个 urlset/index 都不得超过 50000 项或 50MiB 未压缩 XML，超出容量要明确报错而非静默截断。
- 纳入：首页、归档、关于、相册，以及真实公开文章、标签和已发布自建页；根据页面/模板 `indexable=false` 过滤相应目标。不纳入搜索、404、后台、预览、草稿和已删除/下线内容。
- 列表分页不逐页加入地图，文章 URL 由文章分片全量覆盖；不把任意查询参数扩展成站点地图目标。
- XML 正确转义，全部使用正式 origin；lastmod 仅取实际存在的更新时间，不能每次用当前时间假装更新。
- 非法/不存在/越界分片返回 404；稳定数据集下，全部分片并集完整、不重复，测试覆盖超过 100 篇及跨分片边界。并发发布期间采用实时分页的一致性边界，下一次抓取收敛，不宣称跨多次 HTTP 请求的数据库快照。

### 8.2 robots 与订阅兼容

- robots 按当前后台路径生成 exact/subtree 限制，同时排除 API 和搜索，指向正式 `/sitemap.xml`。后台路径修改后下一次请求立即反映；robots 不是鉴权替代。
- 页面自身的 meta/header noindex 保持必要，即使 robots 限制导致抓取器不能重新读取该页。文档解释“禁止抓取”不等于保证 URL 从搜索结果消失。
- RSS/Atom 保留各自最近 100 篇的订阅上限，不再把这个上限用于 sitemap。只包含已发布且未删除文章，使用同一 origin 解析与安全 XML。
- 页面 noindex 是搜索索引选择，不自动取消公开文章订阅；不更改已有 feed 路径或向其中加入草稿。

## 9. Docker 构建、持久化与兼容

- 从**仓库根目录**作为构建上下文，使用包含 Maven 的 JDK 21 构建阶段，保留 Maven 插件锁定的 Node/npm 与 `npm ci`；显式复制所需前后端输入，运行阶段只复制 `selfblog.jar`。
- 增加根上下文排除规则：真实 `.env`、数据库及 WAL/SHM、媒体、日志、Git、工作笔记、构建产物与 node_modules 均不进入镜像。示例配置不等于真实密钥。
- JRE 21 非 root 运行；应用数据、`uploads/`、`gallery-media/` 均有可写持久化目录。替换镜像不能顺带重建数据库或运行破坏性 schema 初始化。
- 保留现有 `backend/docker-compose.yml` 的 MySQL 部署与既有卷名称，不静默切换其数据库。补齐媒体卷、构建上下文、配置/健康检查；初始化 SQL 仅限全新空库，已有库继续使用既有迁移说明。
- 另提供明确的 SQLite Compose 入口，与 MySQL 方案二选一；SQLite 文件及其 WAL/SHM 放在同一持久化目录，两个媒体目录同样保留。不能把已有数据库自动搬迁到新卷。
- 更新指南先备份、说明旧容器未持久化相册目录的导出要求，再替换运行产物；不替用户执行 `down -v`、删除卷或修改原有数据库。
- 健康检查使用运行镜像里真实存在的工具；不能再次声明有 wget/curl 却未安装。容器测试与静态 Compose 检查分别记录。

## 10. Vercel 统一入口

### 10.1 构建时配置

- 以 `frontend` 为项目目录、Other/非 Vite 自动 SPA 预设，通过专用 Node 构建命令生成 Build Output API v3 的 `.vercel/output/config.json`。
- 不重新编译 Vue，不发布独立 static index 或另一套 assets；正常 `npm run build` 仍用于 JAR，行为不被 Vercel 构建脚本取代。
- Vercel 环境配置 `SELFBLOG_BACKEND_ORIGIN`（可访问的源站 HTTPS origin）与 `SITE_BASE_URL`（正式访客 HTTPS origin，与后端设置一致）。两者必须不同；缺失、非法、回指当前 Vercel 部署或相同 origin 时构建失败，不生成看似可用的空站。
- 配置生成发生在已验证支持的 Build Output 阶段，不假设 vercel.json destination 会自动替换环境变量，也不在 build 中修改一个已被平台提前读取的 vercel.json。
- 生成结果不提交凭据，错误日志不打印秘密；普通公网 origin 不是后台 token，也不得夹带 URL 用户名/密码。

### 10.2 路由、缓存与平台边界

- 除 Vercel 保留路径外，将原路径与查询转发给同一个源站：页面 HTML、`/assets/**`、`/api/**`、`/uploads/**`、受保护相册 API、feeds、robots、sitemap 分片。上传路径保持 `/uploads`，不能改成 `/api/uploads`。
- 保持方法、请求体、认证头、媒体 Range 请求和响应状态；不额外开放 Actuator/管理接口，不将认证失败改成 SPA 成功页。
- 动态 HTML、API、发现文档与保护媒体不做共享 CDN 缓存；显式 no-store，并在入口阻止外部 rewrite 意外缓存。可变公开上传也不在本批增加长期缓存。静态优化以后单独测量，不用缓存掩盖源站依赖。
- 保留 DENY、nosniff、Referrer-Policy 等安全响应头；Vercel Preview 环境额外带 HTTP `X-Robots-Tag: noindex,nofollow`。预览与生产可选择不同源站，不能把预览写操作默认当作生产安全隔离。
- Vercel 只是入口，不在其文件系统保存 SQLite/上传，不运行 Java，不承诺源站离线时仍可读；遵守平台请求体、超时及代理限制。
- 自定义域名/TLS、域名验证和保留路径由实际平台配置决定。没有账户、域名和公网源站时，只交付生成器与验证证据，不宣称真实 Vercel 上线。

## 11. 验收与交付

### 自动测试

- 后端：路由/状态矩阵、已发布隔离、管理员也不能通过 HTML 获取草稿、软删除、模块开关、XSS/JSON-LD、伪造 Host/转发头、动态后台 robots、sitemap 完整/分片/过滤与订阅上限。
- 前端：读取服务端 origin 后 canonical 不回退、直接加载/换页的元数据一致、不残留/重复、服务端阅读 DOM 被正常替换、原交互不重复计数。
- 部署脚本：合法/非法 origin、环路、缺失配置、Build Output 路由保持原路径/方法/query、无独立 index/assets、Preview noindex、动态 no-store；正常前端构建仍独立可用。
- 全量闸门不得低于本批开始时的 389/491；保留类型检查、Lint、构建、变更文件格式检查和后端两个前端跳过参数。

### 隔离真实验证

- 使用新临时 SQLite/媒体目录和实际新 JAR；禁用 JavaScript 直接读取文章/About/自建正文，验证链接、head、404 和下线后的不可读性。
- 有 JavaScript 时复跑上一批核心登录/发布/互动/相册/音乐/页面管理工作流，以及七档宽度、暗色、键盘与 srcdoc 预览，确认不放大、不新增双重正文。
- 本地以生成的路由配置做协议/代理冒烟，只能证明本地规则与源站一致，不能代替实际 Vercel CDN、TLS 和部署预览验证。
- Docker 能运行时，补镜像构建、健康检查、SQLite 重启持久化及空 MySQL 初始化；不能运行时如实记录阻塞和可复跑命令，不写成通过。

### 文档与范围

更新 README、前后端分册、DEPLOY、CHANGELOG、架构决策和真实验证记录，注明单 JAR、Docker、Vercel 的实际验证范围。保留用户已有根 `CLAUDE.md` 修改及工作笔记，不混入功能提交。

本批不包含：框架迁移、无限页面搭建、全站视觉重做、数据库切换/清库、自动公网部署、购买域名、代登录搜索平台、保证收录/排名。

## 12. 规格自检

- 架构方向已获用户确认；本文件还需书面审阅，实施计划在审阅后编写。
- 将初始 HTML 与运行时 meta、真实状态与 SPA 导航、源站与正式 origin、配置检查与真实平台执行分别定义，避免混报。
- 保护草稿、相册、动态后台命名空间及旧 MySQL 卷，未加入任何数据清理步骤。
- 明确单一 JAR 资源版本和 Vercel Build Output 机制，不依赖未经验证的环境变量插值。
- 剩余用户域名/平台操作作为交付前置配置，而不是待猜测的代码常量；不以缺少公网权限冒称上线成功。

## 参考资料

- [Vercel 外部重写](https://vercel.com/docs/routing/rewrites)
- [Vercel Build Output API](https://vercel.com/docs/build-output-api)
- [Build Output 配置](https://vercel.com/docs/build-output-api/configuration)
- [上批页面管理验证](../../reviews/2026-10-16-page-management-verification.md)
