# 文章链接跳转修复 + Markdown 渲染链路统一 — 真机验证记录 (2026-10-24)

对应改动：`frontend/src/utils/markdown.ts`（新增，统一渲染入口）、`frontend/src/views/post/PostDetail.vue`、
`frontend/src/views/post/PostEdit.vue`、`frontend/src/components/pages/MarkdownContent.vue`、
`frontend/src/styles/typography.css`、`backend/.../service/site/seo/SafeMarkdown.java`。
CHANGELOG 条目见 `CHANGELOG.md` 的「未发布 — 文章链接跳转修复 + Markdown 渲染链路统一 (2026-10-24)」。

用户诉求（m01866）：「仔细检查文章的markdown渲染引擎，样式按钮等。我现在设置的文章中的URL跳转识别，跳转到了本网站的404页面。」

## 为什么必须真机验证

主诉是「点链接跳到本站 404」，而这条链路上**单元测试只能覆盖其中一段**：

1. `marked` 对 `[文字](example.com)` 输出的是 `<a href="example.com">`（相对地址）——这是库的行为，不是我们的代码；
2. 浏览器把相对地址解析成 `https://本站/example.com`，命中 SPA 兜底路由 `/:pathMatch(.*)*` → `NotFound`；
3. 修复后还要确认「点击真的导航到外站」而不是「DOM 属性对了但点击被别处拦掉」——
   `target="_blank"` + `rel="noopener"` 下弹窗 URL 取不到，必须靠**路由拦截**证明真实目标。

因此验收标准是：真实浏览器 + 真实后端 + 真实数据库，点一次链接，看网络层去哪里。

## 方法与工具链

- **浏览器**：本机 Chrome，经全局 Playwright 1.59.1
  （`C:\Users\Zora\AppData\Roaming\npm\node_modules\playwright`，`chromium.launch({ channel: 'chrome' })`）headless 驱动；视口 **1440×1000**。
- **前后端**：Vite dev `http://localhost:3000`（**必须是 3000**，后端 `CORS_ALLOWED_ORIGINS` 只放行 3000 与 8825，
  起在 3010 会让全部 `/api` 请求 403 `Invalid CORS request`）+ `java -jar backend/target/selfblog.jar`（`:8825`）。
- **登录**：`POST /api/user/login`，字段是 `username`（**不是 `account`**），响应 `data.token`。
- **播种**：经管理端 HTTP API 建临时文章（`POST /api/post`，`status: 1`），**从未手改 `campus_blog.db`**。
  注意 `POST /api/post` 返回的 `data` 是**数字 id**，不是对象。
- **探针脚本**（`.tmp-gate/verify-markdown-link.cjs`，临时目录，不随仓库提交）：CommonJS + `require()` 绝对路径的 playwright
  （ESM import 绝对 Windows 路径会报 `ERR_UNSUPPORTED_ESM_URL_SCHEME`）。
- **清理**：`DELETE /api/post/{id}` 只做软删除（`is_deleted=1`），归档页仍会列出，因此验证后用
  `.tmp-gate/cleanup-posts.py` 物理删除临时文章及其全部子表行（`blog_comment`/`blog_post_tag`/`blog_like`/`blog_collect`/
  `blog_share`/`blog_trending`/`blog_draft`/`blog_circle_like`/`blog_circle_comment`/`blog_post_media`/`anonymous_post_like`/
  `guest_comment`/`post_seo`），库回到原始 15 篇。

## 播种内容

临时文章正文（五类链接 + 两个标题）：

```markdown
[外站裸域名](example.com/path)

[外站完整](https://example.com/path)

[归档页](/archives)

[跳到本节](#section-x)

<hi@example.com>

## 第一节

正文……

## section-x

正文……
```

## 实测结果

### 1. 渲染出的 DOM 属性

| 链接 | `href` | `target` | `rel` |
|------|--------|----------|-------|
| 外站裸域名 `[外站裸域名](example.com/path)` | `https://example.com/path` | `_blank` | `noopener noreferrer` |
| 外站完整 `[外站完整](https://example.com/path)` | `https://example.com/path` | `_blank` | `noopener noreferrer` |
| 站内 `[归档页](/archives)` | `/archives` | **无** | **无** |
| 锚点 `[跳到本节](#section-x)` | `#section-x` | **无** | **无** |
| 邮箱 `<hi@example.com>` | `mailto:hi@example.com` | `_blank` | `noopener noreferrer` |
| 标题锚点（`enhanceHeadingAnchors` 生成） | `#section-1` | 无 | 无 |

关键点：**裸域名补上了 `https://`**，站内链接与锚点**不再被强制 `target="_blank"`**。

### 2. 点击行为（网络层证据）

- **裸域名链接**：`ctx.route('https://example.com/**')` 拦截记录到
  `externalRequests: ["https://example.com/path"]` —— 真实导航目标是外站，**不是本站 404**。
  当前页仍是 `http://localhost:3000/post/{id}`（符合 `target="_blank"`）。
- **站内 `/archives`**：`afterInternalClick: "http://localhost:3000/archives"` —— SPA 路由接管，
  URL 变化、无整页刷新（截图见下）。
- **锚点 `#section-x`**：原地 `scrollIntoView`，路由不变。
- `pageErrors: []`，无控制台异常。

> 弹窗 URL 的坑：`ctx.waitForEvent('page')` 在 `rel="noopener"` 下拿到的 `popup.url()` 常为空串
> （即使 `waitForLoadState` 之后），**必须用路由拦截**才能证明真实目标。

### 3. 截图

`.tmp-gate/shots/60-post-link-fix.png`（1440×1000，207 KB）：点击站内「归档页」后落在归档页，
导航栏「归档」高亮、列表渲染正常。该截图**未归档进仓库**——它拍摄时临时文章尚未清理，
列表里还留着「链接跳转验证（可删）」条目；截图仅作验证过程证据，`docs/reviews/assets/` 只收干净状态的图。

## 自动化闸门

| 闸门 | 结果 |
|------|------|
| 后端全量 `mvnw test`（H2） | **1291 tests / 0 failures / 0 errors**，`BUILD SUCCESS`（基线 1288，本批 +3） |
| 前端全量 `vitest run` | **56 files / 554 tests 全通过**（基线 55/534，本批 +1 文件 +20 tests） |
| `vue-tsc --noEmit` | 0 错误 |
| `eslint src` | 0 error / 42 warning（与基线一致） |
| `prettier --check` | 通过 |

跑闸门前的环境准备（本机必需）：

```powershell
$env:TEMP = $env:TMP = 'C:\Users\Zora\Downloads\SelfBlog\SelfBlog\.tmp-gate'   # 否则 vercel-proxy 测试 mkdtemp EPERM
$env:PATH = 'C:\Users\Zora\Downloads\SelfBlog\SelfBlog\backend\target\frontend\node;' + $env:PATH
& .\node_modules\.bin\vitest.cmd run   # .ps1 垫片被执行策略拒绝，必须用 .cmd
```

## 未验证项

- **真实 MySQL 与 Docker 部署**：本机无 MySQL/Docker 守护进程，仍由 CI 的 `mysql`/`docker` job 覆盖。
- **服务端阅读 HTML 的点击**：`SafeMarkdown` 补协议已有单测（`SafeMarkdownTest` +3 用例），
  但没有用浏览器点过服务端渲染出的 `.seo-reading` 里的链接。
- **真实外网**：本机无外网，`https://example.com/path` 的导航由路由拦截证明，
  未验证外站页面本身能否加载。
