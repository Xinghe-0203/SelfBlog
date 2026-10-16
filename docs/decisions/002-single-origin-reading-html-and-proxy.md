# ADR-002: 单一 JAR 提供阅读 HTML，Vercel 作为可选统一入口

## 状态

架构方向 Accepted（2026-10-16 用户确认）；[书面规格](../superpowers/specs/2026-10-16-crawlable-html-and-deployment-design.md)待审阅，实现未开始。

## 背景

博客已有 Vue 单页应用、页面管理及内嵌前端的 Spring Boot JAR。客户端 metadata 无法保证不执行 JavaScript 的抓取器看到正文，现有 Vercel 配置仅转发部分 API/媒体且指向参考域名。需要在不迁移框架、不改变作者现有视觉与发布流程的前提下，使公开内容可抓取并同时支持服务器、Docker 与 Vercel 入口。

## 决定

JAR 为所有访客返回相同的公开阅读 HTML 与元数据；Vue 正常启动后替换初始阅读 DOM，保留全部交互。这是阅读 fallback，不称为 Vue SSR 或 hydration。渲染只消费公开模型，未发布内容即使由管理员访问也不能输出。

JAR 同时拥有 HTML、哈希资源、API、媒体与发现文档的版本。Vercel 可统一代理到该 JAR，不再独立部署另一份静态前端；通过构建时验证 origin 并生成 Build Output API v3 路由，避免把环境插值当作平台承诺。生产运行仍只需 Java，不增加 Node 渲染服务。

正式访客 origin 由 `SITE_BASE_URL` 指定；源站 origin 与它分开，避免 canonical 指向容器/后端地址或代理自身产生环路。配置了正式 origin 后不使用来访 Host/原始转发头覆盖它。

## 替代方案

- **完整 Vue SSR/Nuxt**：组件复用更完整，但需要迁移部署、路由和鉴权运行方式，当前收益不足以抵消风险。
- **Vercel 静态前端与后端分别发布**：可以独立缓存前端，但会引入 HTML/资源发布一致性、SPA 回退和元数据分歧问题，本轮不采用。
- **仅客户端 meta 与 sitemap**：改动小，却没有解决首次 HTML 正文缺失。

## 后果

- 服务端与客户端要共同遵守标题、模块、样式、indexable、canonical 契约，以测试防止语义漂移；不追求两套 DOM 逐像素一致。
- 初始读取不增加访问计数、不泄露管理/解锁状态。Markdown 解析后必须以白名单净化；HTML、属性与 JSON-LD 各用适当转义。
- 关闭 JavaScript 也能阅读公开正文；互动、密码解锁与增强阅读仍需要 JavaScript。
- Vercel 仍依赖外部源站在线，且不能用来托管 Java、SQLite 或持久化上传；其平台限制和真实部署需要单独验证。
- HTML/API/保护媒体不加共享缓存，先保证发布/下线与权限正确。缓存优化不夹带在本批交付中。
- Docker 保留现有 MySQL 方案并单列 SQLite 入口，不自动切换或搬迁原有数据；运行不可验证时明确标记限制。
