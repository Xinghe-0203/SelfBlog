# 后台可配置站点域名设计

日期：2026-10-16

状态：已按用户在后台可配置域名（后台设置为准、`.env` 回落）实施；验证结果见[验证记录](../../reviews/2026-10-16-seo-deployment-verification.md)的“站点域名后台化”小节。

## 1. 目标

让作者在后台「网站设置」中填写站点正式地址（origin），作为 canonical、分享图、RSS/Atom、
sitemap、robots 与阅读 HTML 的站点地址来源，无需改服务器 `.env`。

- 不做数据库结构变更：复用既有 `site_setting` 键值表。
- 不改变已确认的界面与页面比例；仅在现有「站点资料」面板增加一个字段。
- 不做域名跳转（避免反向代理回源循环），不自动写入用户数据。

## 2. 取值与校验

新增设置键 `siteBaseUrl`（加入现有白名单，公开响应包含它——域名本身就是每页 canonical 的公开信息）。

优先级：

1. `site_setting.siteBaseUrl`（后台填写，写后即生效：保存会清理 `siteSettingsCache`）；
2. 环境变量 `SITE_BASE_URL`（部署默认值，适合尚未在后台填写或希望由环境托管时）；
3. 直连请求推导的 scheme/host/port（仅本地开发兜底）。

校验规则与 `SiteOrigin.normalize` 相同：必须是 `http(s)` 的站点 origin，可带端口；
拒绝账号密码、路径、查询串、fragment、反斜杠、空白与逗号多值。留空表示“未设置，回落”。
非法输入由后台保存请求返回 400 与中文提示。

若数据库中存在直接改坏的值（绕过接口写入），`SiteOrigin` 记录 WARN 并安全回落到
`.env`/请求地址，**不得让整站 500**。

## 3. 组件与职责

| 单元 | 职责 | 约束 |
| --- | --- | --- |
| `SiteSettingsServiceImpl` | 登记 `siteBaseUrl`、校验输入、保存/清除 | 不读取请求，不做域名推导 |
| `SiteOrigin` | 解析最终 origin；`normalize`/`absolute` 保持纯函数 | 每次解析按优先级读取设置缓存，不自行持久化 |
| `Settings.vue` | 展示与提交「网站域名」 | 只提交字符串；错误沿用现有提示通道 |
| `stores/site.ts` | `SiteSettings` 类型与默认值补齐 | 默认空串，不写入 localStorage 业务逻辑 |

Vercel 构建仍使用它自己的 `SITE_BASE_URL` 环境变量（构建期校验源站与公网地址），
与本设置的运行期优先级互不冲突；文档需说明两者关系。

## 4. 验收

- 后端：可保存合法域名、可清空、非法输入 400；后台值覆盖 `.env`；清空后回落 `.env`；
  坏数据安全回落；保存后 `/robots.txt`、`/`、`/sitemap.xml` 与文章 canonical 立即使用新域名。
- 前端：设置页渲染/保存该字段；后端错误原样提示；类型检查通过。
- 文档：DEPLOY/README/CHANGELOG/AGENTS 记录优先级与限制。
- 真实验证：在临时 SQLite + 单 JAR 实例上通过管理 API 改写域名，抓取 robots/sitemap/canonical 断言生效，再清空断言回落。

## 5. 非目标

框架迁移、域名跳转/强制 HTTPS、DNS/TLS 证书管理、在 Vercel 侧自动同步该值、
以本设置替代部署环境变量。
