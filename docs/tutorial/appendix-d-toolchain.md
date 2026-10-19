# 附录D 开发工具链与效率指南

> 工欲善其事，必先利其器。本附录覆盖从零搭好 SelfBlog 开发环境的每一步、两套 IDE 的推荐配置、接口调试与数据库观察的实战手法、调试技巧与 Git 工作流。所有内容都围绕「你现在就可以跟着做」来写。

---

## D.1 开发环境搭建：从零开始

### D.1.1 JDK 21：安装与多版本管理

SelfBlog 后端钉死在 **Java 21（LTS）**。你机器上可能同时有 JDK 8、17、25 等多个版本——SelfBlog 作者的开发机就同时装着 JDK 25（系统默认）和 JDK 21，`JAVA_HOME` 一旦指错，Maven 直接报「不支持」。

**Windows 安装：**

1. 下载 Temurin JDK 21（Adoptium 发行版，免费）：https://adoptium.net/temurin/releases/?version=21
2. 安装时**不要**勾选「Set JAVA_HOME」（我们要手动管理，避免装完 25 又覆盖）
3. 手动设置系统环境变量：

```powershell
# PowerShell（管理员）永久设置
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21", "Machine")
```

4. 验证：

```bash
java -version
# openjdk version "21.x.x" ...
echo $env:JAVA_HOME
```

**多版本切换方案：**

| 工具 | 平台 | 用法 |
|------|------|------|
| SDKMAN! | Mac/Linux | `sdk install java 21-tem`、`sdk use java 21-tem`（按 shell 会话切换） |
| jenv | Mac/Linux | `jenv add <jdk路径>`、`jenv local 21`（按目录切换，写 `.java-version` 文件） |
| 手动 JAVA_HOME | Windows | 每个项目写清楚文档；或在 PowerShell profile 里做 `use-java 21` 函数 |

**⚠️ 实录教训**：SelfBlog 开发机 `JAVA_HOME` 一度指向 JDK 25，跑 `mvnw test` 直接报错。CLAUDE.md 里因此专门写死了一条命令前缀：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

**经验**：项目文档里「能跑通的完整命令」比「泛泛的安装说明」重要一万倍——把它抄进 README，团队里每个人少踩一次坑。

### D.1.2 Node.js：安装与版本管理

前端要求较新的 Node（SelfBlog CI 用 **24.15.0**，本地 22+ 均可）。**不要直接装全局 Node 后不管**——不同项目对 Node 版本要求不同，迟早冲突。

**推荐 nvm-windows（Windows）或 fnm（跨平台，更快）：**

```powershell
# nvm-windows
nvm install 24.15.0
nvm use 24.15.0

# fnm
fnm install 24
fnm use 24
```

**项目级版本锁定**：在 `frontend/` 放一个 `.nvmrc` 文件（内容一行：`24.15.0`），配合 `engine-strict`。SelfBlog 的 `frontend/package.json` 里有 `engines` 字段，npm 配置 `engine-strict=true` 后，Node 版本不对会直接拒绝安装——这是把「版本不匹配」从运行期提前到安装期的典型做法：

```json
{
  "engines": {
    "node": ">=22"
  }
}
```

```ini
# frontend/.npmrc
engine-strict=true
```

**验证：**

```bash
node -v    # v24.15.0
npm -v     # 10.x
```

### D.1.3 Git：安装与三个必改配置

```bash
git config --global user.name "你的名字"
git config --global user.email "you@example.com"
git config --global core.autocrlf true    # Windows 必设，见下
git config --global init.defaultBranch main
```

**⚠️ 换行符大坑（Windows 必读）**：Windows 传统用 CRLF（`\r\n`），Linux/macOS 用 LF（`\n`）。不配置 `core.autocrlf`，仓库里会出现「整个文件都变了」的假 diff——实际只有换行符变了。SelfBlog 仓库配合 `.gitattributes` 与 Prettier 的 `endOfLine: "auto"` 策略（见第六章），三层防护确保 diff 干净：

```gitattributes
# .gitattributes（仓库根）
* text=auto
*.bat text eol=crlf
*.sh  text eol=lf
```

### D.1.4 五分钟自检脚本

环境搭完，跑一遍就知道缺什么。存成 `check-env.ps1`（Windows）：

```powershell
Write-Host "=== SelfBlog 环境自检 ===" -ForegroundColor Cyan

# 1. Java
try { java -version 2>&1 | Select-Object -First 1 } catch { Write-Host "[X] Java 未安装" -ForegroundColor Red }

# 2. JAVA_HOME
if ($env:JAVA_HOME) { Write-Host "[OK] JAVA_HOME=$env:JAVA_HOME" } else { Write-Host "[!] JAVA_HOME 未设置（mvnw 需要）" -ForegroundColor Yellow }

# 3. Node
try { $nv = node -v; Write-Host "[OK] node $nv" } catch { Write-Host "[X] node 不可用（检查 PATH 引号！）" -ForegroundColor Red }

# 4. Git
try { git --version } catch { Write-Host "[X] git 未安装" -ForegroundColor Red }

# 5. 端口占用
foreach ($port in 8825, 3000) {
  $inUse = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
  if ($inUse) { Write-Host "[!] 端口 $port 已被占用" -ForegroundColor Yellow } else { Write-Host "[OK] 端口 $port 空闲" }
}
```

Mac/Linux 等价的 bash 版本：

```bash
#!/usr/bin/env bash
echo "=== SelfBlog 环境自检 ==="
java -version 2>&1 | head -1 || echo "[X] Java 未安装"
echo "JAVA_HOME=$JAVA_HOME"
node -v || echo "[X] node 不可用"
npm -v  || echo "[X] npm 不可用"
git --version || echo "[X] git 未安装"
for p in 8825 3000; do
  (lsof -i :$p >/dev/null 2>&1) && echo "[!] 端口 $p 已占用" || echo "[OK] 端口 $p 空闲"
done
```

**任何一项不过，先修环境再谈写代码。** 附录A 的环境类错误几乎全部源于这一步的隐性失败。

---

## D.2 IDE 与编辑器

### D.2.1 IntelliJ IDEA：后端主战场

Community 版免费且完全够用（Spring Boot 支持、Maven、Database 面板都有；Ultimate 多的是框架深度导航与 HTTP Client 高级功能）。

**打开项目的正确姿势**：直接 Open `backend/pom.xml` → Open as Project。首次打开 IDEA 会自动下载依赖（几分钟），右下角进度条跑完再动手。

**必备插件**（File → Settings → Plugins）：

| 插件 | 作用 | SelfBlog 关联 |
|------|------|---------------|
| Lombok | 识别 `@RequiredArgsConstructor` 等注解生成的代码 | 全项目使用，不装满屏红 |
| MybatisX | Mapper 接口 ↔ XML 双向跳转 | BlogPostMapper.java ↔ BlogPostMapper.xml |
| .env files support | 语法高亮 + 变量跳转 | backend/.env |
| GitToolBox | 行内 blame 提示 | 追查「这行为什么这么写」 |

**必须知道的操作（Top 10）：**

| 快捷键（Windows/Linux） | 功能 | 使用场景 |
|------|------|----------|
| 双击 Shift | 全局搜索任意文件 | 「那个 GlobalExceptionHandler 在哪来着」 |
| Ctrl+N | 按类名搜索 | 直接跳到 Result.java |
| Ctrl+Alt+←/→ | 返回/前进导航位置 | 深入调用链后一键回来 |
| Ctrl+B | 跳转到定义 | Controller → Service → Mapper 链路阅读神器 |
| Alt+F7 | 查找所有用法 | 「谁在调用 deleteById」 |
| Ctrl+Alt+H | 调用层次 | 「这个方法被谁、经哪条路径调用」 |
| Shift+F6 | 重命名（全项目安全重构） | 改方法名不让编译器背锅 |
| Ctrl+Alt+L | 格式化当前文件 | 提交前最后一按 |
| Ctrl+E | 最近打开的文件 | 前后端文件来回切 |
| Ctrl+Shift+F | 全项目文本搜索 | 按报错关键字搜源码 |

**跑 SelfBlog 后端的运行配置**：IDEA 右上角 Run → Edit Configurations → + → Spring Boot：
- Main class：`com.selfblog.SelfBlogApplication`（以实际启动类为准）
- Active profiles：留空（默认 SQLite）
- Environment variables：`JWT_SECRET=本地随便32位以上字符串;ADMIN_PASSWORD=本地12位以上密码`
- Before launch：什么都不加（**千万不要**加 Build 触发前端构建——理由同 mvnw 的双 skip 参数）

### D.2.2 VSCode：前端主战场

轻、快、插件生态最好。装好以下扩展后，前端体验不输 IDEA：

```json
// .vscode/extensions.json —— 推荐提交进仓库，团队/未来的你自动收到安装建议
{
  "recommendations": [
    "Vue.volar",              // Vue 3 官方语言支持（必装）
    "dbaeumer.vscode-eslint", // ESLint 实时标红
    "esbenp.prettier-vscode", // 格式化
    "bradlc.vscode-tailwindcss" // 若项目用 Tailwind（SelfBlog 未用，可选）
  ]
}
```

**工作区设置**（`.vscode/settings.json`，实现「保存即修复」）：

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": "explicit"
  },
  "eslint.validate": ["javascript", "typescript", "vue"],
  "typescript.tsdk": "frontend/node_modules/typescript/lib",
  "files.eol": "\n"
}
```

这样配置后：Ctrl+S 一按，Prettier 排版 + ESLint 自动修复（能自动修的）同时完成，`format:check` 闸门基本不可能再红。

**Volar 的 Takeover 模式提示**：如果装了内置 TS 的 VSCode（默认自带），Vue 文件里可能出现「两套 TS 服务打架」。解决方案：在扩展面板里禁用工作区的「TypeScript and JavaScript Language Features」（Volar 官方称 Takeover Mode），VSCode 状态栏能看到语言服务切换。

### D.2.3 双端联调的目录布局

一个窗口管一端，各开各的终端：

```
窗口1（IDEA）  ── backend/  ── 终端: mvnw spring-boot:run     → :8825
窗口2（VSCode）── frontend/ ── 终端: npm run dev             → :3000
                    （Vite 把 /api 代理到 :8825，浏览器只面对 :3000）
```

**纪律**：改后端代码 → 看窗口1控制台重启日志；改前端代码 → 浏览器 HMR 自动热更（连刷新都不用）；接口报错 → 窗口1日志找异常栈 + 窗口3（浏览器 DevTools）看响应体。三处日志交叉定位，是全栈开发的基本功。

---

## D.3 接口调试工具

### D.3.1 curl 最小集

五分钟上手，CI 脚本、排障都在用：

```bash
# 登录拿 token（SelfBlog 密码登录接口）
curl -X POST http://localhost:8825/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"你的密码"}'

# 带 token 访问管理端接口
curl http://localhost:8825/api/admin/posts?pageNum=1 \
  -H "Authorization: Bearer eyJhbGciOi..."

# 看响应头（排查 CORS/缓存/Content-Type）
curl -I http://localhost:8825/api/posts/1
```

**Windows cmd 的引号坑**：cmd 里单引号无效，JSON 必须用 `\"` 转义或改用 PowerShell 的 `Invoke-RestMethod` / Git Bash 的 curl。这也是推荐用下文图形化/文件化工具的原因之一。

### D.3.2 IDEA 内置 HTTP Client（.http 文件）

在 `backend/` 下建一个 `api-demo.http`：

```http
### 登录
POST http://localhost:8825/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "{{password}}"
}

### 拉文章列表（从上一步响应里手动复制 token，或用脚本变量）
GET http://localhost:8825/api/admin/posts?pageNum=1&pageSize=10
Authorization: Bearer {{token}}
```

点击行首的绿色箭头即可发送，响应直接显示在编辑器旁。`.http` 文件**可以提交进仓库**——接口文档即测试脚本，比 Postman Collection 更适合代码化维护。

### D.3.3 浏览器 DevTools：Network 面板是前端联调的显微镜

F12 → Network，然后触发一次请求，重点看四样东西：

1. **请求头三件套**：`Authorization`（token 对不对）、`X-Visitor-Id`（游客身份）、`X-XSRF-TOKEN`——SelfBlog 拦截器注入的头一目了然
2. **响应体信封**：`{code: 0, message: "success", data: ...}` 还是报错信封？业务码是 0 还是 403？
3. **状态码 vs 业务码**：HTTP 200 但业务 code=500 的「双轨」是 SelfBlog 设计（信封模式），别只看绿色状态码
4. **右键 → Copy → Copy as cURL**：把浏览器里复现的请求原样搬到终端，报 bug 时贴给后端最清楚

### D.3.4 完整调试案例：从登录到管理端一条龙

```bash
# 1. 启动后端后，先验证活着
curl http://localhost:8825/actuator/health
# → {"status":"UP"}

# 2. 登录（.env 里的 ADMIN_PASSWORD）
curl -s -X POST http://localhost:8825/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"..."}'
# → {"code":0,"message":"success","data":{"token":"eyJ...","refreshToken":"..."},"timestamp":...}

# 3. 复制 data.token，访问受保护接口
curl -s http://localhost:8825/api/admin/posts \
  -H "Authorization: Bearer eyJ..." 
# → {"code":0,...}

# 4. 故意不带 token，验证 401 信封
curl -s http://localhost:8825/api/admin/posts
# → {"code":401,"message":"未登录或登录已过期",...}
```

第 4 步「故意做错」非常重要——**先看清错误长什么样，真出错时才认得出来**。这也正是附录A 存在的意义。

---

## D.4 数据库工具与数据观察

### D.4.1 SQLite：零安装的默认库

SelfBlog 默认用 SQLite，整个数据库就是**一个文件**：`backend/campus_blog.db`。

**推荐工具**：
- **DB Browser for SQLite**（免费图形界面）：打开文件即可浏览表、跑 SQL
- **IDEA Database 面板**：+ → Data Source → SQLite → 选文件路径。无需驱动下载（IDEA 自带）

**三个值得马上做的观察**：

```sql
-- 1. 看 KV 表：站点设置零迁移的秘密
SELECT * FROM site_setting;
-- setting_key | setting_value —— 新增设置键不用改表结构

-- 2. 看软删除：删除的行还在表里
SELECT id, title, is_deleted FROM blog_post ORDER BY update_time DESC LIMIT 10;
-- is_deleted=1 的行就是「已删除」——deleteById 实际是 UPDATE

-- 3. 看访客哈希：隐私设计的落地
SELECT nickname, visitor_hash, status, create_time FROM guest_comment ORDER BY id DESC LIMIT 10;
-- visitor_hash 是加盐哈希，原始 visitorId 从不入库
```

### D.4.2 H2（测试库）与 MySQL（CI）

- **H2**：只在测试里活。跑 `mvnw test` 时内存建库销库，不用管；想看测试期间的库，在测试配置里把 JDBC URL 的 `MEM` 换成文件模式即可（一般没必要）
- **MySQL**：本机没有也不影响开发（三库方言由 `databaseId` 兜底）；真实 MySQL 验证交给 CI 的 mysql job（见第六章 6.7 节）

### D.4.3 一条值得记住的 SQL 习惯

排查数据问题时，**先看「应该有而没有」再看「不该有而有」**：

```sql
-- 应该有而没有：这篇文章为什么没显示？先查状态与软删
SELECT id, title, status, is_deleted FROM blog_post WHERE id = 42;
-- status=0 是未发布；is_deleted=1 是已删；都对才轮到查接口

-- 不该有而有：重复数据（唯一约束问题的现场）
SELECT post_id, visitor_id, COUNT(*) c FROM anonymous_post_like
WHERE is_deleted = 0 GROUP BY post_id, visitor_id HAVING c > 1;
```

---

## D.5 调试技巧

### D.5.1 后端：断点只是起点

| 技术 | 怎么用 | 场景 |
|------|--------|------|
| 行断点 | 行号旁单击 | 基础 |
| 条件断点 | 右键断点 → Condition 填表达式 | 循环里只停 `id == 42` 那次，别按 100 次 F9 |
| 日志断点 | 右键断点 → 取消 Suspend，只打印 | 不暂停流程地「加日志」，改完即删 |
| Evaluate Expression | 断点暂停时 Alt+F8 | 现场执行任意表达式，如 `mapper.selectById(42L)` |
| 异常断点 | Breakpoints → + → Java Exception Breakpoints | 抛 `BusinessException` 时自动暂停，直接看到「谁抛的」 |

**Spring 特有的三板斧**：

1. **看自动配置报告**：启动加 `--debug`（或环境变量 `DEBUG=true`），日志里出现 **CONDITIONS EVALUATION REPORT**， Positive/Negative 两栏告诉每个 `@ConditionalOnX` 为什么命中/不命中。排查「我的 Bean 为什么没生效」第一现场
2. **Actuator 看运行时**：`/actuator/beans` 列出全部 Bean 与依赖关系（dev profile 开），`/actuator/health` 看健康
3. **AOP 是否生效**：对可疑 Bean 断点，看 IDEA 变量面板里的对象类名——是 `XxxServiceImpl$$EnhancerBySpringCGLIB$$xxxx` 说明代理生效了，是原始类名说明你正踩在「同类调用失效」坑上（见第三章 3.5 节）

### D.5.2 前端：三件套

1. **Vue Devtools 浏览器扩展**：实时看组件树、每个组件的 props/state、Pinia 全部 store 的当前值。调 store 状态问题的第一工具
2. **debugger 语句**：代码里写 `debugger;`，开着 DevTools 刷新页面即自动断住——比找行号打断点快
3. **Source Map**：Vite dev 模式默认开；生产构建的报错栈也能通过 sourcemap 映射回源码（`vite build --sourcemap` 生成，部署时注意别把 map 文件暴露到公网）

**「断点不生效」排查顺序**：浏览器缓存了旧 JS（Ctrl+Shift+R 强刷）→ HMR 状态损坏（重启 dev server）→ 断点打在编译产物上（确认 sourcemap 生效）。

### D.5.3 跨端联调的系统化姿势

以「游客点赞数字不对」为例的标准排查链：

```
浏览器 Network 面板：请求发出去了吗？头带了 X-Visitor-Id 吗？响应 data.likeCount 是多少？
        ↓ 请求没问题
后端日志（[PERF] 切面 + SQL 日志）：收到请求了吗？执行的 SQL 长什么样？selectCount 返回几？
        ↓ SQL 有问题
数据库直查：anonymous_post_like 里这个 post_id 的行是什么状态？is_deleted？
        ↓ 数据没问题
前端渲染：LikeButton 组件的 props/computed 收到的值对吗？（Vue Devtools 看）
```

每一站都有明确的「通过/不通过」判断，不通过就地解决——**全栈开发者的排障优势就是把链路切成单点**。

---

## D.6 Git 工作流

### D.6.1 SelfBlog 的分支与提交约定

个人项目用最简单的模型：`main` 保持可发布，功能开 `feature/xxx` 分支，完成后 PR 合回（自己审自己也要走 PR——CI 会跑四条流水线，这是唯一值得的仪式感）。

**Commit message 约定**（Conventional Commits 简化版）：

```
feat(post): 文章详情增加预计阅读时长
fix(auth): 修复刷新令牌并发重放导致的偶发 401
docs(tutorial): 新增第三章 Spring Boot 全解
chore(deps): 升级 vite 到 6.4.4
```

类型 + 作用域 + 短描述。提交粒度：**一个意图一个提交**（修复 + 格式化不要混在一个提交里）。

### D.6.2 .gitignore 要点（前后端一体仓库）

```gitignore
# 依赖与构建
node_modules/
dist/
target/

# 环境与密钥（绝不入库！）
.env
*.env.local

# 运行时数据
campus_blog.db
uploads/

# IDE（只提交共享配置，不提交个人配置）
.idea/
.vscode/
```

`backend/.env.example` 入库、`.env` 忽略——**模板进仓库，真实值永进仓库**，这是密钥管理的铁律（CI 用 repository secrets，本地用 .env）。

### D.6.3 三种常见事故的正确恢复姿势

```bash
# 1. 提交信息写错了（还没 push）
git commit --amend -m "新的信息"

# 2. 改错了一半想丢弃工作区改动
git restore .            # 丢弃所有未暂存改动（危险操作，先确认）

# 3. 回滚上一次提交但保留代码改动
git reset --soft HEAD~1  # 改动回到暂存区，重新组织提交

# 4. 任何「完蛋了」的时刻
git reflog               # 所有 HEAD 移动历史都在，找到好时候的哈希
git reset --hard <hash>  # 回去（同样危险，先 git branch backup 分支保底）
```

**纪律**：`--hard` 与 `push -f` 是 Git 里的核武器，按下前先 `git branch backup-$(date +%s)` 造个保底分支，成本为零，后悔药保真。

---

## D.7 效率习惯清单

**每天用得上的：**

- [ ] 开工先拉最新代码：`git pull --rebase`
- [ ] 改代码前先跑一遍现状（测试或页面），确认「改动前是绿的」——否则改坏了都不知道
- [ ] 小步提交，每个提交都是可运行状态
- [ ] 接口改动先在 .http / DevTools 里验证，再写前端调用
- [ ] 顺手把「今天踩的坑」记进笔记——本教程的附录A 就是这么攒出来的

**每周一次的：**

- [ ] `npm outdated` / Maven versions 看依赖更新（只升补丁版本，大版本单独评估）
- [ ] 跑全量闸门（前端六连 + 后端全套），确认基线没退化
- [ ] 清理分支：`git branch --merged` 后删除已合并分支

**需要养成的肌肉记忆：**

| 场景 | 反应 |
|------|------|
| 看到 401 | 先查 token 有没有带、有没有过期，再怀疑配置 |
| 看到 CORS 报错 | 想代理方案，不要去后端加 `@CrossOrigin` |
| npm 命令全部失败 | 先查 PATH（引号！），再重装 |
| SQL 结果不对 | 先查 `is_deleted` 与 `status`，再怀疑代码 |
| 测试突然挂了 | 先跑一次确认不是环境抖动，看报错原文第一行 |

---

## D.8 本章总结

1. **环境四件套**：JDK 21（JAVA_HOME）、Node 22+（nvm 管理）、Git（autocrlf）、自检脚本——搭环境的时间永远花得值
2. **IDE 双开**：IDEA 管后端、VSCode 管前端，保存自动格式化 + ESLint 修复让闸门常绿
3. **接口调试**：curl 打底、.http 文件入库、DevTools Network 是联调显微镜；「故意做错一次」记住错误的样子
4. **数据观察**：SQLite 就是一个文件，KV 表与软删除直接 SELECT 就能看懂设计
5. **调试哲学**：把全链路切成单点，每一站有明确的通过/不通过判据；条件断点与异常断点是 Java 调试的两大杠杆
6. **Git 纪律**：小步提交、危险操作先造保底分支、密钥永进仓库
7. **效率的本质**：不是手速，是「每个重复动作都找到一次性解决它的工具或习惯」

## D.9 参考资源

- [Adoptium Temurin（JDK 下载）](https://adoptium.net/)
- [SDKMAN! 官网](https://sdkman.io/) / [fnm（Node 版本管理）](https://github.com/Schniz/fnm)
- [IntelliJ IDEA 文档](https://www.jetbrains.com/help/idea/)
- [Volar（Vue 官方 VSCode 扩展）](https://github.com/vuejs/language-tools)
- [Chrome DevTools 文档](https://developer.chrome.com/docs/devtools/)
- [Vue Devtools](https://devtools.vuejs.org/)
- [Conventional Commits 规范](https://www.conventionalcommits.org/zh-hans/)
- [DB Browser for SQLite](https://sqlitebrowser.org/)

---
