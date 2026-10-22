# DeepSeek 原生支持与 AI 辅助功能 — 真机验证记录 (2026-10-22)

对应改动：后端 `service/seo/{SeoLlmConfig,OpenAiCompatibleLlmClient,SeoLlmConfigProvider,SeoGenerationService}.java`、
`service/ai/**`（12 类）、`controller/admin/{AdminSeoController,AdminAiController,AdminPhotoAlbumController}.java`、
`service/site/impl/{GuestCommentServiceImpl,PhotoAlbumServiceImpl,SiteSettingsServiceImpl}.java`；
前端 `types/llm.ts`、`api/llm.ts`、`views/admin/{Settings,Seo,Comments,Albums,Health,Dashboard,AdminLayout}.vue`、
`views/post/PostEdit.vue`、`router/modules/admin.ts`。
CHANGELOG 条目见 `CHANGELOG.md` 的「v2.0.28 — DeepSeek 原生支持 + AI 写作/审核/校对/图片描述/站点体检 (2026-10-22)」。

用户诉求（m06868）：「LLM还能支持更多功能吗？要用deepseek的」。范围（m07040 授权）为 A–F 六组：
A 服务商原生支持、B AI 写作辅助、C AI 评论审核、D AI 内容校对、E AI 图片描述、F AI 站点内容体检。

## 方法与工具链

- **上游**：真实 DeepSeek 官方 API（`https://api.deepseek.com`，Key 由用户提供，仅在本机内存与 jar 运行时使用，
  从未写入任何仓库文件、日志或文档）。`GET /models` 返回两个模型：
  `deepseek-flash`（`input_modalities:["text","image"]`、context 1048576）与 `deepseek-v4-pro`（**仅 text**）。
- **被测对象**：`backend/target/selfblog.jar` 起在 `:18830`，`DB_TYPE=sqlite` + 开发库副本。
- **驱动**：PowerShell 脚本经 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File` 执行；
  中文请求体必须用 `UTF8Encoding.GetBytes` 传 `-Body` 字节数组，中文正则的 `.ps1` 必须存为**带 BOM** 的 UTF-8
  （无 BOM 时 Windows PowerShell 按 ANSI 读，报「字符串缺少终止符」）。
- **登录**：管理端登录是 `POST /api/user/login`（**不是** `/api/auth/login`），凭证取 `backend/.env` 的
  `ADMIN_USERNAME`/`ADMIN_PASSWORD`（需 `.Trim()`），响应字段是 `data.accessToken`（len 300）。
- **公开设置端点**是 `GET /api/site/public`（**不是** `/api/site/settings`）。
- **`.env` 优先级陷阱**：`config/DotenvConfig.java` 把 `.env` 值写成 JVM property，**优先级高于进程环境变量**。
  想用独立 DB 必须改 `.env` 或在工作目录之外启动；本轮的第一次尝试因此写进了 `backend/campus_blog.db`（见 §4）。

## 1. 结论

| 组 | 端点 | 真机结果 |
|---|---|---|
| A | `GET /api/admin/seo/models` | `source=upstream`、2 个模型、`supportsImage` 判定与上游 `input_modalities` 一致 |
| A | `POST /api/admin/seo/test-connection` | `ok=true`、`latency=732ms`、`model=deepseek-flash` |
| B | `POST /admin/ai/writing/summary` | 200、161 字、2546ms，内容正确概括正文 |
| B | `POST /admin/ai/writing/tags` | 200、5 个**池内**标签（池外值被丢弃） |
| B | `POST /admin/ai/writing/titles` | 200、3 个互不相同标题 |
| C | `POST /admin/ai/moderation/scan` | 200；自动审核路径：广告留言异步转 `status=0`，正常留言保持 `status=1` |
| D | `POST /admin/ai/proofread/1` | 200、5 条 issues、`truncated=false`、2534ms（首条 `typo/medium`） |
| E | `POST /admin/ai/images/alt` | 200、描述准确、1142ms |
| F | `POST /admin/ai/health/report` | 200、`score=44`、7 findings、`sampled=15 total=15`、10858ms |
| SEO | `POST /api/admin/seo/posts/2/generate` | 200、`source=llm`、真实 title/description/keywords |
| SEO | `GET /post/2`（服务端渲染） | HTML 含自定义 `<title>`、`meta[name=description]`、`meta[name=keywords]` |

后端 **1140 tests / 0 failures / BUILD SUCCESS**（基线 1004，+136）；
前端 `lint:check` 0 errors / 42 warnings、`format:check` 通过、`type-check` 0、
`test:run` **51 files / 496 tests**、`test:deployment` 27/27、`build` ✓。

## 2. 两个必须预判的上游行为（决定性证据）

### 2.1 纯文本模型收到图片不报 HTTP 错误

把图片发给 `deepseek-v4-pro`（`input_modalities:["text"]`）→ **HTTP 200**，而 `choices[0].message.content`
里是一个错误对象 `{"error": "无法读取图片，无法处理图片"}`。**上游不会用 4xx 告诉你「这个模型看不懂图」**。

因此 E 组不能「先试再回落」，必须靠 `/models` 的 `supportsImage` 预判：

- `listModels` **失败** → **fail-open**，继续 `completeVision` 让上游裁决（自建网关可能没有 `/models` 但支持视觉）；
- `listModels` **成功**且选中模型 `supportsImage=false` → **fail-closed** `502`
  「当前模型不支持图像输入，请在设置中换用 deepseek-flash」，**不发出上游调用、不花额度**。

### 2.2 `response_format:json_object` 有文档外硬校验

DeepSeek 要求提示词里必须出现 `json` 字样，否则返回
`400 Prompt must contain the word 'json' in some form to use 'response_format' of type 'json_object'`。
**与思考模式无关**（曾误判为 thinking 导致，实测关闭思考后同样 400）。

原实现靠「400 就去掉 `response_format` 重试一次」兜底，代价是**每次多一个往返**，且第二次静默丢掉 JSON 模式
（模型可能返回带围栏的 Markdown）。修复：`complete()` 先过 `withJsonHint(messages)` —— 任一 message 内容含
`json`（大小写不敏感）则**原样返回**，否则复制列表给**最后一条**追加「请以 JSON 对象的形式输出结果。」；
400 重试分支仍用**原始** `messages`，不重复追加。

真机反证：同一提示词，修复后 `RESPONSE_FORMAT_RETRY_LOG_LINES=0`、`LOG_HAS_400=false`。

## 3. 安全性真机核对

- **公开设置零泄漏**：`GET /api/site/public` 的 `llm*` 键数为 **0**（`LLM_SETTING_KEYS` 整组过滤；
  实现上必须**复制**缓存 map 再 remove，否则缓存被改脏）。
- **密钥掩码哨兵**：管理端回读 `llmApiKey` 长度为 **8**（`SECRET_MASK = "••••••••"`，8 个 U+2022），
  提交哨兵=保留、提交 `""`=清除、其他=替换；真实 Key 从未出现在任何响应、日志或仓库文件里。
- **鉴权**：`/api/admin/ai/**` 与 `/api/admin/seo/**` 双保险（类级 `@PreAuthorize("hasRole('admin')")`
  + `SecurityConfig` 的 `/api/admin/**`）；`AdminAiControllerTest` 的 `nonAdmin_IsForbiddenOnEveryEndpoint`
  覆盖全部端点。
- **自动审核的关闭方向**：`commentModerationEnabled()` = `llmCommentModeration=true && current().configured()`，
  任何一步失败都**回到改造前的行为**（不审核、留言直接公开）。

## 4. 过程中的真实失误与处置（诚实记录）

第一次 e2e 误把数据写进了开发库 `backend/campus_blog.db`：`.env` 里的
`DB_URL_SQLITE=jdbc:sqlite:campus_blog.db` 被 `DotenvConfig` 提升为 JVM property，压过了我设的环境变量。
写入内容为 2 条 `guest_comment` 与 3 条 `post_seo`。已按精确条件删除并 `PRAGMA wal_checkpoint(TRUNCATE)`，
保留了一条先于本轮存在的记录；`PRAGMA integrity_check=ok`。该 `.db` 未被 git 跟踪（`*.db` 在 `.gitignore`）。

## 5. 未覆盖与已知限制

- **AI 生成结果的落库只有一条路径**：`album_photo.caption` 此前**没有任何写入路径**（列与 VO 都在，
  `uploadPhotos` 只写 `file_path/mime_type/sort_order`），本轮补了
  `PUT /api/admin/albums/{albumId}/photos/{photoId}`。但 `source:'cover'` 的 alt 仍未挂 UI
  （API 与类型已就绪），`/proofread/batch` 已实现而 UI 只用单篇。
- **`guest_comment` 没有审核原因列**：AI 判定理由只写日志，不回显到管理端列表。改表结构不在本次范围。
- **真实 DeepSeek 的额度与限速未做压力验证**：`/batch` 上限 50 篇串行，一次可跑 1–2 分钟，
  落在 `/api/admin/**` 的 60/min 兜底桶内，故前端对这三个长请求全部 `_noRetry`。
- **思考模式的实际收益未量化**：默认关闭（结构化任务开思考只白烧输出 token 与延迟），
  开启路径只验证了请求形状（`thinking:{type:enabled}` + `reasoning_effort`，且**不发** `temperature`）。
- **图像理解只验证了 `deepseek-flash`**：未测其他支持视觉的上游模型；E 组的多模态能力依赖
  `/models` 返回的 `input_modalities`，自建网关若不返回该字段则 fail-open 到「上游自己裁决」。
