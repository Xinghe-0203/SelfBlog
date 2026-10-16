# 全站圆角 token 替换 + 收尾 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把全站剩余的裸圆角值换成批次一上调过的圆角 token，让"大 R 角"真正覆盖所有页面；顺带收尾深色模式里明显的浅色硬编码。

**Architecture:** 纯 CSS 值替换，无逻辑变化。扫描口径排除已有 `var(--radius*)`、百分比（圆形）、`inherit`、`0`、`1px/2px`（微元素等效胶囊）与 `999px`（已是胶囊语义）。替换后靠全量闸门 + 明暗两套目视验证。

**Tech Stack:** Vue SFC 内联样式 + 全局 CSS。不新增依赖。

## Global Constraints

- **只改数值外观**：不改选择器、不改结构、不改颜色（深色收尾除外，见 Task 2）。
- 不重排代码：这些文件里很多样式是压缩的单行写法，只做字面量替换，保持原排版与格式。
- 命令在 `frontend/` 下执行；Node 用 provisioned：`$env:PATH = "C:\…\backend\target\frontend\node;$env:PATH"`。
- 闸门：`npm run lint:check`、`npm run type-check`、`npm run test:run`、`npm run build`。

### Task 1: 圆角映射替换（79 处 / 22 文件）

**映射表（确定值，不按文件微调）：**

| 裸值 | token |
| --- | --- |
| 3px / 4px / 5px | `var(--radius-xs)` |
| 7px / 8px / 9px | `var(--radius-sm)` |
| 10px / 11px / 12px / 13px / 14px | `var(--radius)` |
| 15px / 16px / 18px | `var(--radius-lg)` |
| 20px | `var(--radius-xl)` |
| `3px 3px 0 0` / `2px 2px 0 0` | `var(--radius-xs) var(--radius-xs) 0 0` |
| `5px 5px 2px 2px` | `var(--radius-xs) var(--radius-xs) 2px 2px` |

**不动**：`50%` 与百分比、`inherit`、`0`、单独的 `1px`/`2px`、`999px`、已 token 化的值。

**文件（待替换处数）**：`styles/skeleton.css`(1)、`admin/Albums.vue`(7)、`admin/CircleManagement.vue`(1)、`admin/Comments.vue`(3)、`admin/Dashboard.vue`(5)、`admin/Posts.vue`(7)、`admin/Reports.vue`(1)、`admin/Settings.vue`(8)、`admin/Statistics.vue`(4)、`admin/TagsManagement.vue`(1)、`admin/TopicsManagement.vue`(1)、`admin/Users.vue`(1)、`admin/Visitors.vue`(3)、`views/Archives.vue`(2)、`views/gallery/Gallery.vue`(6)、`views/Home.vue`(7)、`views/post/PostDetail.vue`(11)、`views/search/Search.vue`(4)、`views/tag/TagDetail.vue`(2)、`views/trending/TrendingPage.vue`(2)、`views/user/Profile.vue`(1)、`views/user/UserProfile.vue`(1)。

- [x] **Step 1: 按映射表替换**（逐文件，只动 `border-radius` 声明的裸值）
- [x] **Step 2: 扫描确认**：排除 `var(--radius`、百分比、`inherit`、`0`、`1px/2px`、`999px` 后不应再有点位

### Task 2: 深色模式收尾

- [x] **Step 1: `Settings.vue` 的提示条**：`.alert { background: #fff0ed }` 与 `.success { background: #e8f8ef; color: #16794c }` 是浅色硬编码（深色模式下会变成刺眼亮块）。改用语义 token：`var(--error-light)` / `var(--success-light)`，文字色沿用 `var(--error)` / `var(--success)`（`dark-mode.css` 已有对应覆盖）
- [x] **Step 2: 检查同批文件里同类的浅色硬编码**（仅限明显成对的语义背景），有对应 token 的替换，没有的保持原样并记录

### Task 3: 验证与文档

- [x] **Step 1: 闸门**（lint / type-check / test:run / build）
- [x] **Step 2: 浏览器明暗两套目视**：Home、PostDetail、Archives、后台 Settings/Dashboard；确认大 R 角生效、深色无亮块
- [x] **Step 3: 更新 `CHANGELOG.md`（视觉重设计（四）：全站圆角收尾）**

---

## 完成后

- 全站 `border-radius` 统一走 token；圆形、胶囊、直角语义保持不变
- 深色模式下设置页提示条不再出现浅色亮块
- 前端闸门全绿

## 执行修订（2026-10-16）

- Task 1 由子代理按映射表完成：**79 处 / 22 文件**，复核扫描为空；`git diff -U0` 抽查确认变更行全部只含 `border-radius`（未动选择器、结构与其它属性）。
- Task 2 完成：`Settings.vue` 的 `.alert / .success` 改为 `var(--error-light)` / `var(--success-light)` + 语义文字色；已确认 `dark-mode.css` 对这几个 token 有暗色覆盖。同批文件中未见其它需要替换的浅色语义硬编码。
- 验证：`lint:check` / `type-check` / `test:run`（210/210）/ `build` 全绿；浏览器在浅色与深色下回看首页、文章页、后台 Dashboard 与 Settings，截图中圆角与暗色均正常，控制台零报错（主题标记是 `<html class="dark-mode">`，不是 `data-theme`）。
