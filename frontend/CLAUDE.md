# CLAUDE.md - 个人博客 Vue 3 前端 (v2.0.25)

## 项目概述

这是单人作者个人博客的前端项目，基于 Vue 3 + Vite 构建。访客只读、搜索，作者通过管理员后台写作管理。当前版本 v2.0.25。

## 常用命令

```bash
npm install        # 安装依赖
npm run dev        # 启动开发服务器 (端口 3000)
npm run build      # 构建生产版本
npm run preview    # 预览生产版本
npm run lint       # 代码检查
```

## 技术栈

- Vue 3.4.21 (Composition API)
- Vite 5.2.0
- Vue Router 4.3.0
- Pinia 2.1.7
- Axios 1.7.4
- Marked 12.0.1 (Markdown)
- Highlight.js 11.9
- DOMPurify 3.0.9 (XSS 防护)

## 项目结构

```
src/
├── api/            # API 接口 (17个模块)
├── components/     # 组件 (共20个)
│   ├── common/     # 通用组件 (11个)
│   │   ├── BackToTop.vue     # 返回顶部
│   │   ├── Button.vue        # 通用按钮
│   │   ├── Card.vue          # 通用卡片
│   │   ├── CollectButton.vue # 收藏按钮
│   │   ├── EmptyState.vue    # 空状态提示
│   │   ├── FollowButton.vue  # 关注按钮
│   │   ├── ImagePreview.vue  # 图片预览灯箱
│   │   ├── Input.vue         # 通用输入框
│   │   ├── LikeButton.vue    # 点赞按钮
│   │   ├── Modal.vue         # 通用模态框
│   │   ├── Tag.vue           # 标签组件
│   │   └── Toast.vue         # Toast通知
│   ├── circle/     # Circle组件 (4个)
│   │   ├── CirclePostCard.vue    # Circle文章卡片
│   │   ├── CircleSidebar.vue     # Circle侧边栏
│   │   ├── CreatePostModal.vue   # 创建帖子弹窗
│   │   └── QuickComposer.vue     # 快速发布器
│   └── layout/     # 布局组件 (3个)
│       ├── Footer.vue        # 页脚
│       ├── Navbar.vue        # 导航栏
│       └── PageTransition.vue # 页面过渡动画
├── composables/    # 组合式函数
│   └── useConfirm.js         # 确认对话框
├── router/         # 路由配置
├── stores/         # Pinia 状态管理 (3个store)
├── styles/         # 样式文件
├── utils/          # 工具函数
└── views/          # 页面组件 (37个)
```

## v2.0 新特性

- 清爽科技蓝 (Clean Tech Blue) UI 设计，CSS 变量驱动的设计系统 v6.0
- Circle 功能增强
- 性能优化 (懒加载、虚拟滚动)

## 设计系统

- 入口 `src/styles/main.css`，按顺序 `@import` 12 个模块；设计 token 在 `variables.css`，深色模式覆盖在 `dark-mode.css`，皮肤在 `skins.css`
- 深色模式通过 `.dark-mode` 类生效（`stores/theme.ts` 加在 `<html>`，`App.vue` 同时绑在 `#selfblog` 上）
- 皮肤通过 `#selfblog[data-skin='...']` 生效：`ocean`(默认)/`forest`/`lavender`/`sunset`/`midnight`
- 动画工具类: `.fade-in`, `.slide-up`, `.pulse`

### Token 使用约定（重要）

- **不要用 `--gray-900` / `--gray-100` 当「深色表面」**：深色模式会整体反转灰度梯度（`--gray-900` 由 `#111111` 变成 `#EBEBEB`），会让按钮、代码块在深色页面上变成刺眼的亮块
- 需要「永远深色」或「高对比实心」时使用语义 token：
  - `--code-bg` / `--code-text` / `--code-border` — 代码块，两种主题下都保持深色
  - `--contrast-bg` / `--contrast-bg-hover` / `--contrast-bg-active` / `--contrast-fg` — 高对比实心按钮（浅色模式近黑，深色模式自动反转为浅底深字）
- 组件只引用语义 token，不直接写十六进制色值

## 路由约定

- 本站为单人作者博客，访客只读：`authRoutes` 只有一条 `/login` 重定向，指向服务端下发的作者后台入口 `siteStore.adminRoot`（默认 `/admin`）。组件中访客需登录时统一 `router.push('/login')`，不要再指向硬编码路径
- `requiresAuth` / `requiresAdmin` 由 `guards.ts` 统一跳转到 `adminRoot` 并带 `redirect` 查询参数
- 举报页路由为 `/report/:type(post|comment|user)/:id(\d+)`

## 开发规范

1. 使用 Vue 3 Composition API (`<script setup>`)
2. 组件命名使用 PascalCase
3. API 调用统一通过 `src/api/` 目录
4. 状态管理使用 Pinia
5. 路由守卫处理登录验证

## API 配置

开发环境自动代理 `/api` 到 `http://localhost:8825`

## 环境变量

统一使用 `.env` 文件配置（开发/生产环境通用）：

```env
VITE_API_BASE_URL=/api          # API基础路径
VITE_APP_TITLE=个人博客          # 应用标题
VITE_APP_VERSION=2.0.25         # 版本号
VITE_API_TARGET=http://localhost:8825  # 开发环境代理目标
```

如需本地覆盖，创建 `.env.local`（不提交 git）。

## Vercel 部署

1. Root Directory 设为 `frontend`
2. Framework Preset 选 `Vite`
3. `vercel.json` 已配置 `/api` 和 `/uploads` 代理到后端

## v2.0.22 修复记录

| 问题 | 修复文件 | 修复内容 |
|------|----------|----------|
| 移动端宽度不一致 | `PostSearch.vue` | `max-width` 改为 `var(--container-xl)` + `@media` |
| 私信头像不显示 | `Messages.vue` | 内联SVG → `/default-avatar.png` |
| 草稿ID提取失败 | `PostEdit.vue` | 兼容 `res.data` 为数字或对象 |
| 写文章加载旧内容 | `PostEdit.vue` | 移除无条件 `fetchDraft()` |
| CSS line-clamp | 8个文件 | 添加标准 `line-clamp` 属性 |
| 弹窗@提及 | `Circle.vue` | 添加 mention 搜索和选择 |
| 弹窗多话题 | `Circle.vue` | `selectedTopic` → `selectedTopics` 数组 |
| 弹窗草稿 | `Circle.vue` | 关闭保存/打开恢复 |

## 注意事项

- 修改代码前先阅读相关文件
- 遵循现有的代码风格
- 每次修改后更新文档
