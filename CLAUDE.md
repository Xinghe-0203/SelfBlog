# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Version**: v2.0.25 | **GitHub**: https://github.com/Xinghe-0203

---

## 代码修改规范

**【强制】修改代码前必须完整阅读相关文件：**

1. 阅读项目文档（README.md、CLAUDE.md）
2. 修改某个模块前，完整阅读该模块的 Controller → Service → Mapper → Entity
3. 通过源码确认关联关系，不要猜测
4. 确认现有功能后再决定复用还是新增

---

## 开发荣耻

以瞎清接口为耻，以认真查询为荣。
以模糊执行为耻，以寻求确认为荣。
以想业务为耻，以人类确认为荣。
以创造接口为耻，以复用现有为荣。
以跳过验证为耻，以主动测试为荣。
以破坏架构为耻，以遵循规范为荣。
以假装理解为耻，以诚实无知为荣。
以盲目修改为耻，以谨慎重构为荣。
以忘记更文档为耻，以及时更新为荣。

---

## 项目架构

### 技术栈
- **后端**: Spring Boot 3 + MyBatis Plus + Java 21
- **前端**: Vue 3 + Vite + Pinia
- **数据库**: SQLite 默认（保留旧版 `campus_blog.db` 路径），MySQL 8 可选
- **缓存**: Caffeine (5个缓存实例)
- **API文档**: Knife4j (http://localhost:8825/doc.html；prod profile 禁用)

### 数据流
```
Vue SPA (`/`) / REST controllers (`/api`) → Service → Mapper → SQLite / MySQL
              ↓
          统一响应 Result<T>
```

### 认证
JWT双Token: Access(24h) + Refresh(7d)，Refresh仅用于刷新AccessToken

### 性能优化
- **数据库索引**: 16个索引覆盖高频查询
- **Caffeine缓存**: 5个缓存实例 (用户/帖子/标签/评论/配置)

### 关键文件
| 文件 | 用途 |
|------|------|
| `application.yml` | 主配置 |
| `.env` | 敏感信息 (DB密码, JWT密钥) |
| `frontend/.env` | 前端统一环境配置 (API地址、版本号) |
| `SecurityConfig.java` | 安全策略、CORS、路径权限 |
| `JwtAuthenticationFilter.java` | Token解析、黑名单检查 |

### 22张数据库表
sys_user, blog_post, blog_comment, blog_tag, blog_post_tag,
blog_like, blog_collect, blog_follow, blog_notification,
blog_trending, blog_draft, blog_draft_tag, blog_report,
blog_circle_post, blog_circle_like, blog_circle_comment, blog_circle_repost,
blog_media, blog_post_media, blog_topic, blog_message, blog_share

---

## 常用命令

### 后端 (backend/)
```bash
mvn spring-boot:run           # 启动 (端口8825，SPA 根路径，API 前缀 /api)
mvn package -DskipTests       # 前后端打包为 selfblog.jar
java -jar target/selfblog.jar
```

### 前端 (frontend/)
```bash
npm install && npm run dev     # 启动 (端口3000, 代理/api到localhost:8825)
npm run build                 # 生产构建
npm run lint                  # 代码检查
```

### Docker
```bash
docker-compose up -d          # 启动MySQL+后端
docker-compose logs -f        # 查看日志
```

---

## 首次启动账号
在 `backend/.env` 设置 `ADMIN_USERNAME`（默认 admin）和至少 12 位的 `ADMIN_PASSWORD`；没有固定默认密码。

---

## 开发注意事项

1. **用户发布内容不需要审核**: status=1 直接发布
2. **密码加密**: BCrypt强度12
3. **禁止硬编码敏感信息**: 使用环境变量
4. **每次更新代码后同步更新md文档**
5. **API文档**: 启动后访问 http://localhost:8825/doc.html；健康检查为 `/actuator/health`

---

## v2.0.22 修复记录

### 后端修复
- **Jackson时间序列化**: `application.yml` 添加 `write-dates-as-timestamps: false`，修复 LocalDateTime 序列化为数组的问题
- **昵称修改不生效**: `SysUserServiceImpl.java:427` 移除 `htmlSanitizer.sanitizePlainText()`，与注册行为一致
- **搜索不全面**: `PostQueryServiceImpl.java:217` 搜索增加 `summary` 字段匹配

### 前端修复
- **移动端宽度不一致**: `PostSearch.vue` `max-width` 改为 `var(--container-xl)`，添加 `@media 768px` 响应式
- **私信头像不显示**: `Messages.vue` 内联SVG改为 `/default-avatar.png` 静态文件
- **时间显示错乱**: Jackson 配置修复后前端时间正常
- **草稿ID提取失败**: `PostEdit.vue` 兼容 `res.data` 为数字或对象格式
- **写文章加载旧内容**: `PostEdit.vue` 移除无条件 `fetchDraft()`
- **发布后跳回编辑器**: 同上，草稿不再自动加载
- **CSS line-clamp**: 在 `utilities.css` + 7个组件中添加标准 `line-clamp` 属性
- **弹窗@提及功能**: `Circle.vue` 弹窗添加 @用户搜索和选择功能
- **弹窗多话题选择**: `selectedTopic` 改为 `selectedTopics` 数组
- **弹窗草稿保存**: 弹窗关闭时自动保存草稿，打开时恢复
- **CirclePost.vue TypeScript**: `location` 类型修复 `null` → `undefined`

### 数据库修复
- **计数器同步脚本**: 新建 `fix-counter-sync.sql`，修复 `like_count`/`collect_count` 与实际记录不一致
  - 执行方式: `sqlite3 backend/campus_blog.db < backend/src/main/resources/fix-counter-sync.sql`

### 技能系统
- 新建 `.omc/skills/` 目录，包含 4 个项目技能:
  - `vue-modal-mention` - Vue弹窗@提及功能
  - `jackson-date-fix` - Jackson日期序列化修复
  - `sqlite-counter-sync` - SQLite计数器同步
  - `draft-system-reuse` - 草稿系统复用模式

---

## v2.0.22 前端重构记录

### API层修复
- **topic.ts**: 新增 `getTopicBlogPosts` 方法（获取话题下的博客文章）
- **admin.ts**: 新增 `getCommentList`、`deleteComment` 方法（管理员评论管理）
- **user.ts**: 修复 `searchUsers` 返回类型为 `PaginatedResponse<User>`，`getDevices` 返回类型为 `ApiResponse<number>`
- **like.ts**: `getMyLikes` 添加 `PaginatedResponse<Post>` 泛型
- **collect.ts**: `getMyCollections` 添加 `PaginatedResponse<Post>` 泛型
- **media.ts**: `getUserMediaList` 添加 `PaginatedResponse<Media>` 泛型

### 新增VO类型定义
- 新建 `frontend/src/types/vo.ts`，包含 17 个后端VO对应的前端类型:
  - `PostListResponse`, `PostDetailResponse`, `CommentVO`
  - `CirclePostVO`, `CircleCommentVO`
  - `NotificationVO`, `MessageVO`, `ConversationVO`
  - `HotContentVO`, `HotTagVO`, `MediaVO`
  - `CollectItemVO`, `LikeItemVO`, `ReportVO`
  - `StatisticsVO`, `FollowStatusVO`

### 新增通用组件
- `LikeButton.vue` - 通用点赞按钮（乐观更新、防重复点击）
- `FollowButton.vue` - 通用关注按钮
- `CollectButton.vue` - 通用收藏按钮

### 新增Composables
- `usePagination.ts` - 通用分页逻辑（支持泛型、自动加载）

### Circle.vue 拆分
- **Circle.vue**: 从 2697 行精简到 882 行
- 新增 `CreatePostModal.vue` - 弹窗发布组件（含@提及、话题、草稿）
- 新增 `CirclePostCard.vue` - 动态卡片组件
- 新增 `CircleSidebar.vue` - 侧边栏组件

### 时间格式统一
- `PostEdit.vue`: 移除本地 `formatTime`，改用共享 `formatDate`
