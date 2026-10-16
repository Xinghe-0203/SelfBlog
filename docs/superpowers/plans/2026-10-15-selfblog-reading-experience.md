# 文章页阅读体验增强 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把文章阅读路径上的交互补齐——代码块复制/折叠、标题锚点、移动端目录抽屉、正文图片灯箱、按正文估算阅读时长、最后更新提示。

**Architecture:** 新增三个可单测的单元 + 一次集成：

1. `src/utils/reading.ts`：纯函数字数统计（中日韩按字 + 西文按词）；
2. `src/composables/useMarkdownEnhance.ts`：对渲染后的正文 DOM 做后处理（代码头部/复制/折叠、标题锚点、图片收集），幂等；
3. `src/components/post/MobileTocSheet.vue`：≤860px 的左下浮动胶囊 + 底部抽屉，复用同一份 toc 数据；
4. `src/views/post/PostDetail.vue`：调用增强、`IntersectionObserver` 标题高亮、灯箱接线、阅读时长与更新提示；并修复 `ImagePreview.vue` 的键盘监听 TDZ（灯箱依赖它）。

**Tech Stack:** Vue 3.5 `<script setup>` + TypeScript 6 + Vitest 4 + @vue/test-utils + jsdom。不新增依赖。

## Global Constraints

- **不加依赖**；`noUncheckedIndexedAccess`（索引访问带 `| undefined`）；`Array.prototype.at` 不可用（lib 不含 ES2022）。
- **命令在 `frontend/` 下执行**；Node 用 provisioned：`$env:PATH = "C:\…\backend\target\frontend\node;$env:PATH"`。
- 闸门：`npm run lint:check`、`npm run type-check`、`npm run test:run`、`npm run build`。
- **布局硬约束**：`.app-main` 的 `padding-top`、`PostDetail` 的 `scroll-margin-top:105px` 与 `.article-aside{top:105px}` 都不动；锚点依赖批次一已加入的 `html { scroll-padding-top: 96px }`。
- 复制按钮必须是真实 `<button>`：写进 `v-html` 的标记 Vue 不编译、DOMPurify 也会剥交互属性，因此走渲染后 DOM 后处理。

---

### Task 1: `utils/reading.ts` 字数统计

**Files:**
- Create: `frontend/src/utils/reading.ts`
- Create: `frontend/src/utils/__tests__/reading.test.ts`

**Interfaces:**
- Produces: `countWords(markdown: string): number`、`estimateReadMinutes(markdown: string, wordsPerMinute = 300): number`

- [x] **Step 1: 写失败测试**（纯 CJK / 纯英文 / 混排 / 围栏与行内代码剔除 / 图片不计数 / 链接文字计数而 URL 不计数 / 空输入）
- [x] **Step 2: 跑测试确认失败**
- [x] **Step 3: 实现**（逐行扫描围栏代码块以支持未闭合块；图片先于链接剔除；连字符只在词外剔除）
- [x] **Step 4: 测试 + type-check 通过**

### Task 2: `composables/useMarkdownEnhance.ts` DOM 后处理

**Files:**
- Create: `frontend/src/composables/useMarkdownEnhance.ts`
- Create: `frontend/src/composables/__tests__/useMarkdownEnhance.test.ts`

**Interfaces:**
- Produces: `enhanceMarkdown(root: HTMLElement, options?: { collapseHeight?: number }): string[]`（返回去重后的正文图片 src，按文档顺序）

- [x] **Step 1: 写失败测试**（头部/语言标签/复制按钮与回执、折叠与展开切换、锚点 href、图片收集去重、重复调用不叠加）
- [x] **Step 2: 跑测试确认失败**
- [x] **Step 3: 实现**（幂等；`scrollHeight > 320` 折叠；`language-(\w+)` 提取语言；`navigator.clipboard` 失败显式报"复制失败"）
- [x] **Step 4: 测试 + type-check 通过**

### Task 3: `components/post/MobileTocSheet.vue`

**Files:**
- Create: `frontend/src/components/post/MobileTocSheet.vue`
- Create: `frontend/src/components/post/__tests__/MobileTocSheet.test.ts`

**Interfaces:**
- props：`items: { id; text; level }[]`、`activeId?: string`；emits：`select: [item]`
- 不使用 Teleport、不注册全局监听；宽屏由 `@media (min-width: 861px)` 整体隐藏

- [x] **Step 1: 写失败测试**（按条渲染与 level class、active 高亮、选择后 emit 并关闭、胶囊打开、背板关闭）
- [x] **Step 2: 跑测试确认失败**
- [x] **Step 3: 实现**（左下胶囊 + 底部抽屉；圆角与配色全走 token）
- [x] **Step 4: 测试 + type-check 通过**

### Task 4: `PostDetail.vue` 集成 + `ImagePreview` 键盘修复

**Files:**
- Modify: `frontend/src/views/post/PostDetail.vue`
- Modify: `frontend/src/components/common/ImagePreview.vue`
- Create: `frontend/src/components/common/__tests__/ImagePreview.test.ts`

- [x] **Step 1: 修复 `ImagePreview` 的 TDZ**：`watch(..., { immediate: true })` 在 `const handleKeydown` 声明前引用它，初始 `show=true` 时监听挂不上（浏览器端验证实际复现：Esc 关不掉灯箱、控制台报 `Cannot access 'handleKeydown' before initialization`）。改为函数声明（提升）并补两条测试
- [x] **Step 2: 集成到 `PostDetail`**：
  - `estimateReadMinutes` 替换原 `content.length / 400`
  - `lastUpdated`：`updateTime` 晚于 `createTime` 超过 7 天才显示
  - 内容渲染后（`nextTick` + `setHeadingIds`）调用 `enhanceMarkdown` 并保存图片列表
  - 内容容器上做事件委托打开灯箱；`ImagePreview` 用 `v-if` 重新挂载（其 `currentIndex` 只读一次 `initialIndex`）
  - `IntersectionObserver` 标题高亮（`rootMargin: '-110px 0px -60% 0px'`），仅在 `typeof IntersectionObserver === 'undefined'` 时回退到旧的滚动扫描；卸载时 disconnect
  - ≤860px 隐藏 `.article-aside`，由 `PostDetail` 挂载 `MobileTocSheet`（**偏离设计稿**：设计稿写在 App.vue，目录数据本来就在 PostDetail，就近挂载避免提升为全局状态）
- [x] **Step 3: 闸门**（lint / type-check / 全量测试 / build）

### Task 5: 端到端验证与文档

- [x] **Step 1: 浏览器验证**（临时文章 → 验证 → 删除）：
  - 2 个代码块（js/css 语言标签、复制按钮点击变"已复制"）
  - 长代码块默认 `is-collapsed`，"展开代码"→ 取消折叠并变"收起代码"
  - 3 个标题锚点（`#section-0` 起）
  - 侧边目录 3 条；点击正文图片灯箱打开（1 / 1），Esc 关闭
  - 390px 视口：浮动胶囊可见、侧边目录隐藏、抽屉打开/选择关闭
  - 控制台零报错
- [x] **Step 2: 清理**：删除临时验证文章（含软删除行），确认数据库只剩 4 篇真实文章
- [x] **Step 3: 更新 `CHANGELOG.md` 与设计稿中的 App.vue 行**

---

## 完成后

- 文章页：复制、折叠、锚点、灯箱、移动端目录、准确阅读时长、最后更新提示全部可用
- 前端闸门全绿（210 tests）

## 执行修订（2026-10-16）

- Task 1–4 由子代理按本计划实施；主会话审查后在端到端验证中发现并修复了 `ImagePreview.vue` 的 TDZ 缺陷（计划写完后才踩到，Task 4 Step 1 即为此修复）。
- 折叠阈值 `320px` 同时写在 `useMarkdownEnhance` 默认值与 `PostDetail` 的 `max-height` 里，两处必须同步（代码内有注释互指）。
- `MobileTocSheet` 挂在 `PostDetail`（见 Task 4），设计稿 `frontend/src/App.vue | 挂载 MobileTocSheet（仅文章页）` 已改为"本批无改动"。
- 验证数据已清理：临时文章（id 5–9，含软删除行）从 `blog_post` 硬删除；`uploads/` 无新增文件。
- 最终闸门：`lint:check` / `type-check` / `test:run`（**210/210**）/ `build` 全绿；浏览器端到端结果为 0 控制台错误。
