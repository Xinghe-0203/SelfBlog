# 全仓深度审计修复 + UI 打磨 — 验证记录 (2026-11-21)

对应改动：CHANGELOG `v2.0.30 → 六（2026-11-21）— 全仓深度审计修复 + UI 打磨 + 版本发布整理`。
本轮由**四份并行审计报告**驱动：后端正确性、前端正确性、文档准确性、UI 真机视觉（Playwright/Chrome）。

## 审计方法与覆盖

| 审计 | 方式 | 覆盖 |
|------|------|------|
| 后端 | 只读代理逐包通读（common/utils/config/controller/service/mapper/entity/dto/test） | 全部包，逐项给出 file:line + 严重级；明确列出「验证过且干净」的模块 |
| 前端 | 只读代理逐目录通读（api/stores/composables/utils/router/views/types/styles/components） | 全部目录 + 39 条警告的完整清单与逐条修法 |
| 文档 | 机械交叉核对（grep 代码验证文档声明） | README×3、CLAUDE×2、AGENTS、docs/agents×2、CHANGELOG、DEPLOY、版本字段文件 |
| UI | Vite dev + 全局 Playwright（`chromium.launch({ channel: 'chrome' })`），1440×900 + 390×844，明暗两主题，21 张全页截图 | 首页/关于/归档/搜索（含联想态）/相册/标签/404/登录；间距节奏、对比度、空态质量、暗色一致性、移动端溢出与触达 |

后端当时有一个真实后端在 8825 运行，UI 审计实际渲染了带真实数据的页面（含阅读页 `/post/6`）。

## 关键发现 → 修复对照（P1 级）

| # | 发现 | 根因 | 修复 |
|---|------|------|------|
| B1 | 话题计数器在编辑/删除时从不递减；`JSON_CONTAINS`/`json_each` 过滤对应用写入的行失效 | `blog_post.topic_ids` 挂 `JacksonTypeHandler` 无 `autoResultMap`：写入双编码（`[884002]`→`"[884002]"`）、读取不过 handler，`parseTopicIds` 永远失败 | 移除 typeHandler + 容错解析 `BlogPost.parseTopicIds`（兼容存量双编码行，零迁移）。**否决 `autoResultMap`**：读 MyBatis-Plus 3.5.8 源码，`parse()` 对 String 字段 `readValue`，单编码行会直接抛异常——且 schema 注释记载的 v2.0.16 `JSON_UNQUOTE` 迁移会把存量行变成单编码，该方案等于埋雷 |
| B2 | 清空话题/封面/草稿摘要不生效但计数器已回减（漂移） | `updateById` 写不了 NULL（NOT_NULL 策略，backend.md 记载的陷阱在计数器上的表现） | `LambdaUpdateWrapper` 显式 `set`（仅清除路径触发），草稿 summary/coverImage 同修 |
| B3 | 发邮件/改密限流前缀指向不存在路径（等于不限流） | `/api/user/send-code` 早已迁到 `/api/auth/password/*` | 前缀重指（3/min、5/min）+ `/api/media/post` 读预算 60/min |
| F1 | 正文图片上传「成功」但插入永不执行 | 前端 `UploadResponse` 读 `url/fileName`，后端 `MediaVO` 给 `fileUrl` | 类型镜像对齐 + 失败走 `toast.error` |
| U1 | 左上角黑色残影（登录页最明显） | `.skip-link` 停在 `top:-48px`，`--shadow-md` 画出元素外 ~28px | `transform: translateY(-100% - spacing)` 停靠，`tab` 聚焦时 `transform:none` |
| U2 | 移动端菜单文字透出玻璃面板 | `.functional-glass` 88% 透明 + appearance.css paper-tier 接管覆盖了 token | 移出接管清单 + token 提到 **98%**（Playwright 像素 A/B：残差 ~4/255） |

P2 级共 20 余项（503 分支、`@Size` 对齐列宽、死枚举/死导出/死 CSS/死图标、`escape` 弃用、非幂等重试、暗色画布遮罩 45→60、双搜索框同步等）全部修复，逐项见 CHANGELOG「六」。

## 验证证据（2026-11-21 实测）

- 后端全量 **1486 / 0 failures / BUILD SUCCESS**，且在 **filesystem 与 reversealphabetical 两种 runOrder** 下均绿
  （reversealphabetical 是 2026-11-07 CI 类顺序事故的复现顺序——本轮日志捕获类缺陷即以该顺序本地复现后修复）。
- 前端：`vitest run` **64 files / 667 tests 全通过**；`vue-tsc --noEmit` 0 错误；**`eslint src` 0 error / 0 warning**（基线 39 条警告清零，4 处 v-html 为 DOMPurify 消费点，用带理由的块级 disable）；`prettier --check` 通过；`test:deployment` **29/29**；`vite build` 通过。
- UI 修复的 Playwright 复验（代理实测值）：skip-link 停靠位 `top: -54.39px`、Tab 后 `top: 0` 且焦点落链；复选框 `accent-color` 生效、18×18、命中区 28.39px；禁用按钮 `rgb(243,245,248)/rgb(90,99,113)`、`opacity: 1`、`not-allowed`；`scrollbar-width: thin`；移动端 `pre` 右缘 padding 24px。
- 新增回归测试：`BlogPostServiceNullClearTest`（5）、`BlogPostTopicIdsRoundTripTest`（11，含存量双编码行播种）、`GuestCommentServiceImplTest`（13）、`apiError.test.ts`（4）等。

## 遗留与边界（诚实清单）

- `docs/tutorial/*.md`（4 文件）教学快照仍引用 2.0.25 版本号——历史教学内容，刻意不动。
- `release/selfblog-2.0.29.jar` 是 v2.0.29 制品（早于全部八批），`release/README.md` 已注明；发版时需重建 2.0.30 jar。
- CI 中 `actions/setup-node@v4` 与 `docker/build-push-action@v6` 的 Node20 弃用提示属上游未发新版（已确认 setup-node 最新主版本即 v4）；GitHub 已强制其在 Node24 运行，无功能影响。
- UI 视觉审计的截图证据存放于 `.audit/ui/shots/`（gitignore 目录，临时性）；本记录的文字结论为持久留存。
- 真实 Vercel 部署、搜索引擎收录依旧从未验证（沿用既有声明）。
