# 附录A：常见错误排查手册

> 本附录汇总 SelfBlog（v2.0.26）开发与部署过程中最常见的报错，每条错误按 **【现象】→【原因】→【解法】→【预防】** 四段展开。【现象】保留真实报错原文，方便你在自己的日志里直接搜索关键字；【原因】讲清背后的机制——对初学者来说，「为什么会这样」远比「复制哪段代码能修好」重要；【解法】给出可直接落地的步骤；【预防】总结为一条可以写进团队规范的守则。
>
> 全章按三大节组织：**A.1 后端**（Spring Boot 3.3.13 + Java 21 + MyBatis Plus 3.5.8 + Spring Security + jjwt 0.12.6 + Caffeine）、**A.2 前端**（Vue 3.5.44 + Vite 6 + TypeScript 6.0.3 strict + Pinia + Vitest + jsdom）、**A.3 环境与工具链**。每节内部再按 **编译期 / 启动期 / 运行期** 归类——这个划分本身就是排查思路：先问「代码跑起来了吗」，再问「跑起来之后在哪一步断的」。
>
> 仓库基线（写本手册时的验证状态）：后端 **588 tests / 0 failures**；前端 **43 个测试文件 / 400 tests** 全绿，外加 `lint:check`、`format:check`、`type-check`、`test:deployment`（27 项）、`build` 五道闸门；CI 四个 job 全部真实执行（backend H2 全量、frontend 全闸门、mysql 真库全量、docker 冒烟并 grep 服务端 HTML 中的 `seo-reading`）。任何一次改动后如果基线掉色，本章就是你的排障地图。

---

## A.0 通用排查方法论

在进入具体错误之前，先把方法论立起来。绝大多数初学者排查失败，不是因为知识不够，而是因为**跳步骤**：看到红色报错，直接把报错粘给搜索引擎，搜不到就换措辞再搜，十分钟过去毫无进展。下面五步法看似朴素，但它是所有「看起来很神奇的一秒定位」的唯一来源。

### A.0.1 第一步：读报错原文，从最后一个 Caused by 读起

Java 的异常栈是**洋葱结构**：最外层是「谁发现了问题」（往往是框架容器），最内层的 `Caused by:` 才是「问题的真相」。Spring Boot 启动失败时，控制台会打印一大段 `Exception encountered during context initialization` 加上几十行 `at org.springframework...`，初学者常在最外层就慌了，其实要一路滚到**最后一个** `Caused by:`。

比如一个典型的循环依赖失败，最外层是 `UnsatisfiedDependencyException`，往下剥才是真相：

```text
Caused by: org.springframework.beans.factory.BeanCurrentlyInCreationException:
Error creating bean with name 'blogPostServiceImpl':
Requested bean is currently in creation: Is there an unresolvable circular reference?
```

`Requested bean is currently in creation` 这一句才是可搜索的关键字。同理，前端报错要看浏览器 **Console 的第一行**与 **Network 里失败请求的 Response 面板**——很多「前端 bug」的真相其实是后端返回了 `{"code":401,...}` 而前端拦截器把它包装成了另一种样子。

一个刻意训练：**把报错原文读出声**。`No qualifying bean of type 'JwtUtils' available` 直译过来就是「容器里没有一个能配得上的 JwtUtils 类型的 Bean」——这句话已经把答案说了一半：要么没注册，要么注册了但没被扫描到，要么测试切片根本没把它加载进来。

### A.0.2 第二步：把报错定位到文件与行号

报错栈里每一行 `at com.selfblog.service.post.BlogPostServiceImpl.likePost(BlogPostServiceImpl.java:88)` 都是可以点击/搜索的坐标。定位规则：

- **栈里第一个 `com.selfblog.*` 帧**（而不是 `at org.springframework...`、`at java.base...`）通常就是你代码出问题的位置——框架帧只是「案发时框架正在做什么」。
- 找到文件后，**不要只看那一行**：把该方法整个读完，再看一眼它的调用方。SelfBlog 的分层是 `Controller → Service → Mapper → Entity`，且门面服务（`BlogPostService`）会把交互逻辑委托给 `PostInteractionService`——栈可能在两层之间来回跳，跳一次就代表一次委托。
- 前端 TS 报错自带 `src/xxx.ts(12,7): error TS2532: ...`，括号里是「行,列」。注意 `vue-tsc` 对 `<script setup lang="ts">` 的行号是**以整个 `.vue` 文件**计的，不是以 script 块第一行重新计数。
- 手头要常备两个搜索：后端搜 `backend/src/main/java`，前端搜 `frontend/src`。报错里的类名、配置键（如 `app.api-prefix`）、SQL 片段都是好的搜索词。

### A.0.3 第三步：构造最小复现

「在我机器上有时好有时坏」的问题永远修不完，因为它没有被固定下来。最小复现的要点是**把变量减到只剩一个**：

- 数据库问题：先用 H2（`DB_TYPE` 留空跑测试）复现，排除 MySQL/SQLite 方言因素。
- 并发/事务问题：先把 `@Async`、线程池拆掉，用单线程同步调用复现，确认逻辑本身对不对，再把异步加回去。
- 前端问题：用 vitest 只跑一个文件的一个用例（`./node_modules/.bin/vitest run src/stores/user.test.ts -t "restoreFromStorage"`）；或者只跑 `./node_modules/.bin/vite` 让页面在无后端状态下渲染（SelfBlog 的站点设置 store 有默认值兜底，布局可以独立验收）。
- 接口问题：用 `curl` 绕过前端直接打后端。这一步能一刀切开「接口本身错了」和「前端读错了」两种可能——附录里 E-B20（Result 信封）就是典型：`curl` 返回的 JSON 明明有 `data`，前端却读到 `undefined`，说明问题在前端读取姿势。

最小复现还有一个隐藏收益：**它常常在你构造的过程中就把 bug 暴露了**。因为构造复现必须精确描述前置条件，而很多 bug 就藏在「我以为的前置条件」与「实际前置条件」的缝隙里。

### A.0.4 第四步：二分法压缩搜索空间

当代码量让你无法通读时，用二分法：

- **提交二分**：`git bisect` 或手动 checkout 历史提交，找到「最后一次确定是好的」与「第一次确定是坏的」之间的那个提交。仓库的 `CHANGELOG.md` 每次改动都带「影响文件」表，可以让二分的粒度直接落到文件级。
- **代码二分**：在可疑链路的中点打断言/日志。后端在 Service 层入口打印入参与返回值；前端在 store action 与 axios 拦截器里各打一行。中点对了，问题在后半段；中点错了，在前半段——每次排除一半。
- **配置二分**：`application.yml` 里逐条注释/恢复。SelfBlog 的 `DB_TYPE`、`app.single-author-mode`、`upload.root-path` 都是能独立改变行为的开关，一次只动一个。

### A.0.5 第五步：查 CHANGELOG 与 git log

SelfBlog 有一个明确的文档契约：**改代码必须同步 `CHANGELOG.md`**，并在条目里附「影响文件」表。这意味着当你接手一个「突然不工作」的功能时，第一件事是翻 CHANGELOG 看这个文件最近被谁动过、为什么动。很多「诡异 bug」其实是三天前一次看似无关的重构（比如把某个接口从门面挪进了交互服务，顺手改了事务注解）留下的坑。

`git log -p -- path/to/file` 同样值得读。历史 diff 会告诉你这段代码**从没好过**（设计问题，要改设计）还是**最近才坏**（回归问题，要找回退点）——这两种问题的修法完全不同。

### A.0.6 两个世界的排查视角差异

最后提醒一件事：后端与前端排查的气质不同。

- 后端是**确定性系统**：同样的请求、同样的数据，结果必然相同。所以后端排查的核心是「让报错暴露出来」——把日志级别调到 DEBUG、把异常栈完整打出来、用 MockMvc 写一个失败测试来固定 bug。SelfBlog 后端 588 个测试是排查时的资产：几乎每个「这怎么会错」的问题，都能先写一个失败的测试用例，再修到它变绿。
- 前端是**状态机 + 时间线**：路由守卫、启动顺序、缓存、事件监听器、异步时序都能让「同一份代码」表现不同。所以前端排查的核心是「冻结时间线」——打开 DevTools 的 Network（勾选 Preserve log）看请求顺序，用 `createMemoryHistory()` 在测试里复刻导航序列，用 `enableAutoUnmount(afterEach)` 保证每个测试从干净状态开始。

带着这套方法论读下面的条目，你会发现每一条都是五步法的具体应用。

### A.0.7 第六步（进阶）：让系统自己开口——日志与观测

五步法是「向外挖」，观测是「让证据自己浮出来」。两个世界各有三板斧。

**后端日志三板斧：**

1. **Trace 串联**：`HttpRequestLoggingInterceptor` 对几乎所有路径（排除 `/uploads/**`、`/actuator/**` 与文档路径）记录请求日志；`JwtAuthenticationFilter` 会把请求头里的 `X-Trace-Id` 放进 MDC——同一次请求的所有日志行共享同一个 trace id，`grep <traceId> app.log` 就能串起整条链路。在「多个请求日志交织」的并发场景里，这是救命稻草：先把可疑请求的 trace id 找出来，再只看这个 id 的日志行，噪音立刻少九成。
2. **具名安全事件**：鉴权失败不是模糊的 WARN，而是带事件名的记录（如 `JWT_AUTH_FAILED`、`REFUSED_REFRESH_TOKEN`）。直接搜事件名，比搜「401」精准得多——「401」会命中一大片无关行，事件名只会命中真正的安全链路。
3. **调级与 SQL 证据**：临时加 DEBUG 用启动参数 `--logging.level.com.selfblog=DEBUG`（PowerShell 里同样注意引号规范，见 E-E02）。MyBatis 的 SQL 日志是软删除（E-B10）、分页（E-B18）、方言（E-B11）三类问题的**最终证据**——一切以「真实执行的 SQL 文本」为准，不以「我以为框架会怎么做」为准。看到日志里的 `[PERF] ... error:` 不要慌，那是监控切面的负例断言，详见 E-B24。

**前端 DevTools 三板斧：**

1. **Network（勾选 Preserve log + Disable cache）**：看请求时间线——「谁先谁后」往往就是答案。刷新请求是否只发了一次？首屏接口是否在登录态恢复之前就发出去了？点开失败请求的 Response 面板看后端信封原文，别信前端包装后的报错文案。
2. **Console + Application 面板**：Console 看拦截器吐出的 `userMessage`（axios 封装里统一了错误文案）；Application → Local Storage 检查 token 键、访客标识、主题键（`edu_theme`）是否如预期存在、格式是否正常。
3. **Vue Devtools**：检查 Pinia 五个 store（site/theme/user/music/pages）的实时状态——注意 user store 的 id 是 `'auth'`（历史原因，见 E-F14），在面板里搜 "user" 是搜不到它的。

**数据库观测一句话**：拿不准 SQL 行为时，让日志把 SQL 打出来，逐字符读——WHERE 里有没有 `is_deleted=0`、有没有 `LIMIT ?`、表名列名是否如你所想。框架的「魔法」都写在生成的 SQL 里，读 SQL 就是在读真相。

### A.0.8 五步法实战演练：一次真实的 401 排查

方法只有用过才会属于你。下面用一次典型的「登录后所有请求突然 401」完整走一遍五步，注意每一步的**产出物**——它们共同构成定位证据链。

**案情**：作者反馈：早上还能写文章，中午登录后所有后台请求返回 401，页面被弹回登录页；登录动作本身显示成功。

**第一步，读原文**。不猜、不重试。开 DevTools → Network：401 的响应体是 `{"code":401,"message":"登录凭据已升级，请重新登录"}`；Console 干净。切到后端日志，同一时间窗出现成组 `JWT_AUTH_FAILED`。两行原文已经给出两个关键事实：这是**后端主动拒绝**（不是前端 bug），且拒绝理由不是「过期」也不是「签名错」，而是一句业务文案。

**第二步，定位文件与行号**。拿这句文案当搜索词搜 `backend/src/main/java`，命中 `JwtUtils` 中 `getCredentialVersionFromToken` 的抛出点。读上下文：JWT 里有一个 `credentialVersion` claim，与用户当前密码的 HMAC 摘要比对，不一致就抛 401。到这里，「现象层」已经解释完毕——剩下的问题是：为什么今天不一致了？

**第三步，最小复现**。curl 带 token 调一个只读接口，稳定 401，且响应与前端看到的逐字节一致——排除「前端处理坏了」的可能。再解码这个 JWT（不动验签，只看 payload）：`credentialVersion` 字段存在，但值与当前用户摘要对不上。

**第四步，二分法**。变量只有两个：token 旧了，或用户记录变了。用 git stash 把工作区回到上一个 commit 重启——问题消失。二分锁定「这次改动」。

**第五步，查 CHANGELOG 与 git log**。`git log --oneline -3` 显示最近一次提交调整了 `credentialVersionFor` 的摘要输入（把用户名换成了用户 id 参与计算）。所有旧 token 的该 claim 是按旧公式算的，新公式校验必然失败——机制完全吻合「登录凭据已升级，请重新登录」这句文案的设计意图（密码或凭据要素变更后强制重登）。这不是 bug，是新安全机制对存量 token 的正常清场；前端 `failedQueue` 收到 401 后调 `handleAuthError` 弹回登录页也是**按设计工作**。

**结论与产出**：无需改代码，重新登录即可；但要给「凭据版本算法变更」类提交补一条 CHANGELOG 备注「会使所有已登录用户失效」。这次排查的五步产出物分别是：两行报错原文、一个抛出点文件坐标、一条 curl 复现命令、一个 commit 区间、一条机制解释——你向别人求助时把这五样贴出来，对方几乎不需要再问第二个问题。

---

## A.1 后端错误排查（backend/）

后端源码根在 `backend/src/main/java/com/selfblog/`。本节错误按编译期 → 启动期 → 运行期排列，编号 E-B01 起。

### A.1.1 编译期错误

#### E-B01 「release version 21 not supported」——JDK 版本不对

【现象】在 `backend/` 下执行 Maven 编译或测试，构建在 `compile` 阶段立刻失败：

```text
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:compile (default-compile) on project selfblog:
Fatal error compiling: error: release version 21 not supported
```

或者更隐蔽的一种：编译居然过了，但运行测试时抛 `UnsupportedClassVersionError`，提示类文件版本号不匹配（61.0 对应 Java 17，65.0 对应 Java 21）。

【原因】`pom.xml` 把 Java 版本钉死在 21，编译器插件会向 JDK 请求「以 release 21 编译」。而 Maven 实际用的 JDK 若低于 21（比如 17），它不认识 release 21。Maven 找 JDK 的顺序是：`JAVA_HOME` 环境变量优先，找不到才退回 PATH 里的 `java`。Windows 上常见的坑是：系统属性里配过老的 `JAVA_HOME` 指向 JDK 17 或 25，你新装的 JDK 21 根本没被 Maven 看见。用 `backend\mvnw.cmd -version` 看输出里的 `Java version` 行，那才是 Maven 真实在用的 JDK——别相信「我装了什么」，要相信「Maven 用了什么」。

【解法】把会话级 `JAVA_HOME` 指向 JDK 21 再构建。PowerShell 写法：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

想永久解决，就把系统环境变量 `JAVA_HOME` 改成 JDK 21 的安装目录（SelfBlog 明确不支持用 JDK 25 做编译运行，详见 A.3 的 E-E01）。

【预防】把「构建前 `$env:JAVA_HOME` 指向 JDK 21」写进你自己的启动脚本或 PowerShell `$PROFILE`；CI 里用 Temurin 21 的 setup 步骤显式固定版本，不依赖运行器默认值。凡是「昨天还能编译今天不行」的编译期错误，第一反应就是查 `mvnw -version`——环境漂移比代码回退常见得多。

### A.1.2 启动期错误

启动期的定义：`main` 方法已执行、Spring 容器正在创建 Bean，但还没来得及监听端口。这一段的特征是控制台出现 `APPLICATION FAILED TO START` 大字报或长长的异常栈。

#### E-B02 循环依赖三兄弟：NoSuchBeanDefinition / UnsatisfiedDependency / BeanCurrentlyInCreation

【现象】启动失败，控制台出现三种面貌相近的报错之一。最外层常见的一手：

```text
org.springframework.beans.factory.UnsatisfiedDependencyException:
Error creating bean with name 'blogPostServiceImpl' defined in file
[...\BlogPostServiceImpl.class]:
Unsatisfied dependency expressed through constructor parameter 0:
Error creating bean with name 'postInteractionServiceImpl'
```

剥到最后一层 `Caused by:`，可能是：

```text
Caused by: org.springframework.beans.factory.BeanCurrentlyInCreationException:
Error creating bean with name 'blogPostServiceImpl':
Requested bean is currently in creation: Is there an unresolvable circular reference?
```

或者链上有一环压根没注册成功时：

```text
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException:
No qualifying bean of type 'com.selfblog.service.post.PostInteractionService' available:
expected at least 1 bean which qualifies as autowire candidate
```

【原因】三个异常是同一条故障链的不同断面。Spring 创建 Bean 的流程是：实例化（调构造器）→ 属性注入 → 初始化（`@PostConstruct` 等）→ 放入单例池。**构造器注入**意味着「实例化」这一步就需要依赖对象已经在手——A 的构造器要 B，B 的构造器要 A，于是 Spring 创建 A 时转而创建 B，创建 B 又转回创建 A，形成死循环，最终以 `BeanCurrentlyInCreationException` 收场。`UnsatisfiedDependencyException` 是外层包装（「A 依赖的 B 没造出来」）；`NoSuchBeanDefinitionException` 则常出现在链上有一环压根不存在（被条件注解排除、包扫描没覆盖）的情况——此时循环还没轮到，先报「找不到」。

这里有一个初学者最容易误解的点：网上很多教程说「Spring 能解决循环依赖，靠的是三级缓存」。**三级缓存只能救字段注入/Setter 注入的环**——因为它允许先把「半成品」（实例化了、属性还没填）曝光，再回头补引用。而 SelfBlog 全项目用构造器注入（这是刻意的工程选择：依赖在编译期就可见、可 `final`、可测），构造器环意味着 A 都没实例化出来，无「半成品」可曝光，三级缓存无能为力。Spring Boot 从 2.6 起也默认禁止循环引用，直接失败而不是尝试解环。

【解法】按顺序做三件事：

1. 读 `APPLICATION FAILED TO START` 下方的环描述，Spring Boot 会画出 `A → B → C → A` 的完整路径，圈出**自己代码里**的类（排除框架注入的边）。
2. 打破环。标准做法是把环上「只读、无副作用」的那条边下沉成第三个类。SelfBlog 的实际分层就是范例：门面（`BlogPostServiceImpl`）持有交互服务（`PostInteractionServiceImpl`）的引用做编排，而交互服务**绝不回头引用门面**——它只依赖 Mapper 与更底层的服务。如果你发现自己「为了调用对方一个方法」引入了反向依赖，正确动作是把那个方法挪到查询服务或独立组件里。
3. 万不得已（例如接入第三方 starter 形成的环），可以给一环加 `@Lazy`——它注入的是代理，首次真正调用时才解析。这是止痛药：环还在，只是被延迟了，别把它当修复。

【预防】新类写依赖前先画调用方向图，保证「门面 → 查询/交互 → Mapper」单向；评审时看到 Service 互相 import 对方的接口就警惕。检测手段：`mvnw test` 的应用上下文加载本身就是环检测——保持 588 个测试可跑，环会在 CI 第一时间炸出来。

#### E-B03 JWT 密钥过短：启动就抛 WeakKeyException / IllegalStateException

【现象】两种面目。其一，容器初始化 `JwtUtils` 时直接失败：

```text
Caused by: java.lang.IllegalStateException: JWT密钥长度必须至少32个字符
```

其二，绕过了启动校验（例如密钥恰好 30 字节），在第一次签发或解析 Token 时爆出 jjwt 的原始报错：

```text
io.jsonwebtoken.security.WeakKeyException:
The signing key's size is 240 bits which is not secure enough for the JWS HS256 algorithm.
```

【原因】SelfBlog 用 HS256 对称签名，`JwtUtils` 里构造密钥的调用是 `Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))`。RFC 7518 规定 HS256 的密钥不得短于 256 位（32 字节），jjwt 0.12.6 会**强制执行**这一条——短密钥直接抛 `WeakKeyException`，不给「先跑起来再说」的机会。项目里还有一道更早的闸门：`JwtUtils` 的 `@PostConstruct validateSecret()` 在 Bean 初始化阶段检查 `secret.getBytes(StandardCharsets.UTF_8).length < 32` 就抛 `IllegalStateException`，让问题在启动期而不是第一次登录时才暴露。注意校验用的是 **UTF-8 字节数**而非字符数——纯 ASCII 时两者相等；若密钥里混了中文或全角字符，字节数会大于字符数，反而更容易过线；而「看起来够长的 20 个字母」只有 20 字节，过不了。

【解法】打开 `backend/.env`，把 `JWT_SECRET` 换成 ≥32 字节的随机串。不要用「12345678901234567890123456789012」这种顺序串，用随机熵：

```powershell
# PowerShell 生成 48 字节随机值的 Base64（约 64 字符）
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

写进 `.env` 后重启。`.env` 的加载机制见 E-B04——特别注意**不要给值加引号**：加了引号，那对引号本身会计入字节长度（反而「通过」了校验），但密钥里混进了引号字符，与其他环境算出的签名对不上，埋下 E-B14 的雷。

【预防】`.env.example` 里的占位串本身 ≥32 字节，让「照抄示例也能启动」；部署检查单加一条「启动日志无 `[Dotenv]` 告警且健康检查通过」。另外记住：**换了 JWT_SECRET，所有已签发 Token 立刻失效**（签名对不上），用户会被登出——生产轮换密钥要挑低峰期。

#### E-B04 `.env` 陷阱：引号、查找顺序与「文件值压过系统环境变量」

【现象】三个典型症状。其一，登录后签名校验失败（见 E-B14），排查发现 `JWT_SECRET` 的值里**带着一对双引号**。其二，明明在系统环境变量里设置了 `DB_PASSWORD`，应用却连不上数据库，用的是 `.env` 里的旧值。其三，`.env` 明明放在「项目某处」，应用却说找不到：

```text
[Dotenv] 未找到 .env 文件，已跳过（请参考 .env.example 配置）
```

【原因】SelfBlog 的 `.env` **不是** shell/dotenv 语义，而是 `config/DotenvConfig.java`（一个 `EnvironmentPostProcessor`，通过 `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` 注册）用 Java `Properties` 语法解析的。四个后果：

1. **不要加引号**。`Properties` 把 `JWT_SECRET="abc...def"` 这一整行按 `=` 切开，值为 `"abc...def"` **含引号共 34 字节**。shell 的「引号会被剥掉」心智模型在这里不成立。
2. **查找顺序**：先看工作目录（`user.dir`，即你启动应用的目录，通常是 `backend/`），再退到用户主目录（`user.home`）。放在别处的 `.env` 不会被读到。
3. **文件值会 `System.setProperty` 成 JVM 系统属性**，而 Spring 的属性源排序里 systemProperties 高于操作系统环境变量——所以 `.env` 文件里的值**优先于**你在系统层面设置的同名环境变量。这本意是方便本地开发覆盖，但如果你习惯用系统变量注入密码，就会出现「我明明改了系统变量它不生效」的灵异事件。
4. 好消息：解析用 `Files.newBufferedReader(envPath, StandardCharsets.UTF_8)`，中文值（如 `ADMIN_NICKNAME=站长`）不会乱码——不要被「Properties 默认 ISO-8859-1」的老知识吓到。

【解法】对着检查单逐条核对：

```properties
# backend/.env —— Properties 语法：key=value，不要引号，不要行尾分号
JWT_SECRET=这里放至少32字节的随机串
ADMIN_PASSWORD=至少12位的管理员密码
DB_TYPE=sqlite
```

- 值不加引号、不加分号；
- 想用系统环境变量时，把 `.env` 里的同名行**删掉**（而不是注释——注释行确实会被跳过，但删除更干脆）；
- 文件放在 `backend/` 下（工作目录）或用户主目录；
- 敏感键（`DB_PASSWORD`、`JWT_SECRET`、`MAIL_PASSWORD`、`DB_USERNAME`）会被 DotenvConfig 特殊处理，日志不会明文打印它们。

【预防】把三条规则写进快速开始文档：**不加引号、放 backend/ 下、文件值会覆盖系统变量**。CI 从不依赖 `.env`（GitHub Actions 用 job 级环境变量），所以 CI 与本机行为不一致时，第一件事就是 diff 本机 `.env` 与 CI 的变量表。

#### E-B05 端口被占用：8825 / 3000 起不来

【现象】启动最后一步报：

```text
***************************
APPLICATION FAILED TO START
***************************
Description:
Web server failed to start. Port 8825 was already in use.
Action:
Identify and stop the process that's listening on port 8825 or configure this
application to listen on another port.
```

前端则是 Vite 的 `Port 3000 is in use, trying another one...`（Vite 会自动换端口，但换端口后代理与后端的约定可能错位）。

【原因】Tomcat 绑定 `server.port`（SelfBlog 默认 8825）失败，通常是上一个开发实例没杀干净，或另一个进程恰好占用。Vite 的 3000 同理。占用的本质是操作系统层面一个 TCP 端口同一时刻只能被一个监听者持有。

【解法】Windows 上定位并释放：

```powershell
Get-NetTCPConnection -LocalPort 8825 -State Listen | Select-Object LocalPort, OwningProcess
Get-Process -Id (Get-NetTCPConnection -LocalPort 8825 -State Listen).OwningProcess
# 确认是残留的 java.exe 后：
Stop-Process -Id <pid>
```

临时换端口也可以：启动参数 `--server.port=8826` 或改 `application.yml`，但要同步改前端 `vite.config.ts` 里 `VITE_API_TARGET` 指向的端口，否则代理打空（见 E-F07）。

【预防】开发机用固定端口 + 脚本化启停（脚本里先查占用再启动）；`netstat -ano | findstr :8825` 是等价的老命令。Hikari 连接池与端口无关，但「SQLite 锁」是另一类占用问题，见 E-B22。

#### E-B05b 测试启动失败：`DB_TYPE` 残留把 H2 数据源顶掉了

【现象】明明在跑 `mvnw test`（应为 H2 内存库），却报 MySQL 连接失败或驱动缺失：

```text
Caused by: org.springframework.beans.factory.BeanCreationException:
Error creating bean with name 'dataSource' ...
Cannot load driver class: com.mysql.cj.jdbc.Driver
```

【原因】`DatabaseConfig` 是一个带 `@ConditionalOnProperty("DB_TYPE")` 的配置类：只要环境里**存在** `DB_TYPE` 变量（任何非空值），它就会创建真实数据源（SQLite/MySQL），顶掉测试资源里的 H2 配置。如果你之前为了本地起服务设置过系统级 `DB_TYPE=mysql`，跑测试时这个残留就会把 H2 顶掉，测试代码面对的 suddenly 是一个它没准备过的数据库。

【解法】会话里清掉再跑测试：

```powershell
Remove-Item Env:DB_TYPE -ErrorAction SilentlyContinue
backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

【预防】本机数据库切换只写在启动脚本里，不写进系统级环境变量；CI 的 mysql job 是显式传 `DB_TYPE=mysql` + `SPRING_SQL_INIT_MODE=never` 的，与本地测试互不干扰。

### A.1.3 运行期错误

#### E-B06 `@WebMvcTest` 切片启动失败：缺 `@MockBean JwtUtils`

【现象】给某个 Controller 写切片测试（`@WebMvcTest(PostController.class)`），测试**还没跑**，上下文先炸了：

```text
Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException:
Error creating bean with name 'jwtAuthenticationFilter':
Unsatisfied dependency expressed through field 'jwtUtils':
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException:
No qualifying bean of type 'com.selfblog.utils.JwtUtils' available:
expected at least 1 bean which qualifies as autowire candidate.
Dependency annotations: {@org.springframework.beans.factory.annotation.Autowired(required=true)}
```

【原因】`@WebMvcTest` 只加载 MVC 层的东西：控制器、`@ControllerAdvice`、过滤器等 Web 组件，**不加载** `@Service`、`@Repository`，也不加载普通的 `@Configuration`。但 `SecurityConfig` 引入的 `JwtAuthenticationFilter` 是 `@Component` 的 `OncePerRequestFilter`，它内部 `@Autowired private JwtUtils jwtUtils;`（字段注入）——切片把过滤器拉了进来，却不提供 `JwtUtils`（它只是个 `@Component` 工具类，不属于切片自动装配范围），字段注入找不到候选者，于是 `NoSuchBeanDefinitionException`。注意栈的表述是「`jwtAuthenticationFilter` 创建失败，因为字段 `jwtUtils` 不满足」——从报错就能读出这条依赖链。

【解法】在测试类里注册一个 Mock：

```java
@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtUtils jwtUtils;          // 喂给 JwtAuthenticationFilter
    @MockBean BlogPostService blogPostService;  // 控制器自身的依赖也要 mock

    @Test
    @WithMockUser(roles = "admin")
    void list_returnsOk() throws Exception {
        mockMvc.perform(get("/post/list")).andExpect(status().isOk());
    }
}
```

要 200 还得给身份：匿名请求会被安全链拦成 401/403，`@WithMockUser(roles = "admin")` 提供认证。SelfBlog 的管理员接口标了 `@PreAuthorize("hasRole('admin')")`，方法级安全在切片里是**生效**的——`@WithMockUser(roles = "user")` 会得到 403，这恰好可以用来写权限负例断言。

【预防】把「新建 `@WebMvcTest` 必带 `@MockBean JwtUtils`」记为模板；仓库里已有几十个切片测试都这么做，抄最近的当样板。若哪天 `JwtAuthenticationFilter` 新增了别的依赖字段，所有切片会同时炸——报错特征一致（都是 filter 创建失败），按同一模式补 Mock 即可。

#### E-B07 `@AutoConfigureMockMvc(addFilters = false)` 并不能绕过鉴权

【现象】为了「让测试跳过安全链」，有人把 `@SpringBootTest` + `@AutoConfigureMockMvc(addFilters = false)` 用在 SelfBlog 上，期望所有请求免鉴权返回 200；实测却拿到：

```text
Status expected:<200> but was:<401>
```

响应体是 401 的 JSON 信封，鉴权根本没有被绕过。

【原因】`addFilters = false` 的语义是「不注册 Spring Security 的**过滤器链**」。但 SelfBlog 的鉴权链条里，`JwtAuthenticationFilter` 只负责「有 token 就解析并设置认证」，真正把「未认证」变成 401 的是安全基础设施的授权环节与认证入口点（AuthenticationEntryPoint）。关掉过滤器链后请求以匿名身份直达，而控制器上的 `@PreAuthorize` / 安全配置仍然要求身份，最终仍以 401 落地——「关灯」不等于「开门」。这是实测结论，不是理论推演。

【解法】放弃 `addFilters = false`。需要鉴权上下文的测试用 `@WithMockUser(roles = "admin")`（或自定义 `SecurityContextFactory` 构造带 `UserContext` 的认证）；需要测公开接口的，用切片测试真实安全链——公开路径在 `SecurityConfig` 里本来就有 `permitAll` 放行。若确需「无鉴权直测业务逻辑」，把逻辑下沉到 Service 层用普通 Mockito 单测，而不是在 MVC 层关安全。

【预防】评审测试代码时看到 `addFilters = false` 直接打回；把本条实测结论写进贡献文档，避免后人再踩。

#### E-B08 401 / 403 / 404 三分法：先分清「没登录、没权限、没这条路」

【现象】前端拿到的失败响应无非三族，三者的响应体与排查方向完全不同：

```text
401 → {"code":401,"message":"...未登录/凭证无效..."}
403 → {"code":403,"message":"Access Denied"}
404 → {"timestamp":"...","status":404,"error":"Not Found","path":"/api/xxx"}
```

后端日志里对应三种根因：`io.jsonwebtoken.ExpiredJwtException: JWT expired at ...`（401 的一种）；`org.springframework.security.access.AccessDeniedException: Access is denied`（403）；`NoHandlerFoundException` 或 Spring MVC 默认的 404（404）。

【原因】三分法的机制基础：

- **401 = 认证失败**：没有携带 Token、Token 过期（`ExpiredJwtException`）、Token 签名对不上（E-B14）、Token 已进黑名单、或凭据版本不匹配——`JwtUtils` 把凭据版本做进 JWT claim，用户改密后版本升级，旧 Token 对不上就回 401「登录凭据已升级，请重新登录」。注意 `JwtAuthenticationFilter` 的容错设计：解析失败只记 `JWT_AUTH_FAILED` 告警并继续过滤链，最终由安全层统一回 401，不会 500。
- **403 = 认证成功但授权不足**：请求者身份已识别，但角色不满足 `@PreAuthorize("hasRole('admin')")` 之类的要求。SelfBlog 单作者模式下还有一层兜底：`app.single-author-mode: true` 时非 admin 一律按无权限处理，所以「我有登录态为什么还 403」的答案通常是「你登录的不是管理员」。
- **404 = 路由不存在**：路径写错、请求方法不匹配（把 POST 发到只有 GET 的映射）、或者——SelfBlog 特有的——**前缀问题**（见 E-B09）。

【解法】拿到失败响应先问三连：

1. 请求头里有没有 `Authorization: Bearer <token>`？（没有 → 401 合理）
2. Token 解出来是什么角色？（浏览器 DevTools → Application → Local Storage，或后端 DEBUG 日志）
3. 请求路径与方法，和 Controller 源码上的 `@RequestMapping` 逐字符对一遍，并确认真实请求该带 `/api` 前缀（E-B09）。

顺手把 curl 当听诊器：

```powershell
curl.exe -i http://localhost:8825/api/post/list          # 公开接口应 200
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8825/api/admin/pages
```

【预防】前端统一由 axios 拦截器处理 401（刷新/登出，见 E-F10），业务代码不要散落 401 分支；后端新增管理员接口时必须同时写「非管理员 403」的切片断言，让权限回归在 CI 暴露。

#### E-B09 404 特别篇：测试里不带 `/api` 前缀，真实请求却要带

【现象】两族互为镜像的 404。其一，切片测试按 Controller 源码上的映射写请求，返回 404：

```text
Status expected:<200> but was:<404>
PASSED: get("/post/list")     ← 测试里这样写是对的
FAILED: get("/api/post/list") ← 真实请求这样写，测试里反而 404？（其实两种都能匹配，见下）
```

其二，浏览器/前端直连后端时漏掉 `/api`：

```text
GET http://localhost:8825/post/list → 404
GET http://localhost:8825/api/post/list → 200
```

【原因】SelfBlog 的 `WebMvcConfig.configurePathMatch` 对**所有 `@RestController` 自动追加 `/api` 前缀**：

```java
configurer.addPathPrefix(apiPrefix, HandlerTypePredicate.forAnnotation(RestController.class));
```

`apiPrefix` 来自配置 `app.api-prefix`，默认 `/api`。这意味着源码里 `@GetMapping("/post/list")` 实际对外是 `/api/post/list`，但**源码里永远不要手写 `/api`**——写了就变成 `/api/api/...`。MockMvc 在 `@WebMvcTest` 切片里会走同样的路径匹配逻辑，所以测试里写不带前缀的 `/post/list` 与带前缀的 `/api/post/list` 都能命中（实测两者均通过）；但**从外部 HTTP 客户端访问时必须带 `/api`**。还有一类「特殊公民」：`SeoController` 刻意声明为 `@Controller` 而非 `@RestController`，正是为了逃出这个前缀规则，在根路径提供 `/feed.xml`、`/robots.txt`、`/sitemap.xml`。

【解法】404 时按此清单核对：

1. 请求是从哪里发出的？浏览器/前端/curl → 必须带 `/api`；MockMvc 切片 → 带不带都行，但建议与真实请求一致（带 `/api`，读测试更贴近线上行为）。
2. Controller 是不是 `@RestController`？普通 `@Controller`（如 SPA/SEO 控制器）没有前缀加成。
3. 请求方法对不对？`GET /api/post/1` 与 `POST /api/post/1` 是不同路由。
4. 新接口是不是忘了在 `SecurityConfig` 放行？——不过注意放行不足的后果是 401/403，不是 404；404 更多是路径本身的问题。

【预防】新写 `@RestController` 时映射一律不带 `/api`；写测试时统一用带 `/api` 的路径（与生产一致）；接口联调用 Knife4j 文档页核对真实路径。

#### E-B10 软删除陷阱：手写 SQL 查出已删除数据 / `deleteById` 竟是 UPDATE

【现象】其一，列表页出现了用户已经「删除」的文章；导出报表、手写统计 SQL 里还能看到 `is_deleted=1` 的行。其二，看 MyBatis 日志发现：

```text
DEBUG com.baomidou.mybatisplus.core.MybatisPlus - ==> Preparing: UPDATE blog_post SET is_deleted=1 WHERE id=? AND is_deleted=0
```

——调用的是 `deleteById`，执行的是 UPDATE。

【原因】SelfBlog 的 20+ 个实体用 `@TableLogic private Integer isDeleted` 标记逻辑删除，并在 `application.yml` 配置了 `logic-delete-field`。生效后 MyBatis Plus 的**自动注入 SQL**（BaseMapper 的通用方法、`ServiceImpl` 的方法、QueryWrapper 生成的查询）都会自动带上 `is_deleted=0` 过滤与「删除改更新」的改写。但这个魔法**只对 MyBatis Plus 生成的 SQL 有效**：你自己在 Mapper XML 里手写的 `SELECT * FROM blog_post WHERE category_id=?`，框架管不着，已删除行照样被查出来。`deleteById` 变 UPDATE 同理——它不是 bug，是逻辑删除的定义：数据留着（可恢复/可审计），只是被标记。

【解法】

1. 手写 SQL **一律自己带 `is_deleted=0`**：`SELECT * FROM blog_post WHERE category_id=? AND is_deleted=0`。写 XML 时把这条当作 WHERE 子句的固定成员。
2. 联表查询时每张参与软删除的表都要带各自的 `is_deleted=0`，漏一张就漏一层「已删数据」。
3. 若确实需要查含已删数据（如管理员回收站），用 QueryWrapper 显式 `isNull`/自定义 SQL，不要试图「临时关掉」逻辑删除。

【预防】代码评审固定检查项：XML 里出现 `FROM blog_*` 却没有 `is_deleted` 条件的，一律打回。把「`deleteById` 是 UPDATE」写进新人上手材料，避免有人误以为删除失败而去查半天「为什么数据库里还在」。

#### E-B11 databaseId 方言：MySQL 专有 SQL 写进共享语句，H2 测试炸

【现象】本地 MySQL 跑得好好的功能，一跑 `mvnw test`（H2）就报 SQL 语法错误，形如：

```text
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException:
Syntax error in SQL statement ... expected ... [ON DUPLICATE KEY UPDATE (Line X)]
```

或 `FUNCTION ... does not exist`、`Unknown data type: "JSON"` 之类只在 MySQL 语法里存在的构造。

【原因】`MybatisPlusConfig` 注册了 `VendorDatabaseIdProvider`，Mapper XML 里同一个 `<select>` 可以写两份：带 `databaseId="mysql"` 的专有语句与带 `databaseId="sqlite"` 的专有语句，或都不带 `databaseId` 的兜底语句。关键规则是：**数据库没有对应的映射 id 时，只会执行无 `databaseId` 的兜底语句**。测试用的是 H2，H2 在 VendorDatabaseIdProvider 里的 id 不是 `mysql` 也不是 `sqlite`——于是只匹配到兜底语句；如果有人图省事把 `ON DUPLICATE KEY UPDATE` 这类 MySQL 专有语法直接写进了兜底语句，H2 一执行就语法错误。反方向的坑同样存在：把 SQLite 的 `AUTOINCREMENT` 或 H2 不支持的函数写进兜底语句，MySQL/SQLite 跑得好，测试库一上来就炸。

【解法】三步：

1. 找到报错的 statement id（异常栈或 MyBatis 报错里有 `...Mapper.insertXxx`）。
2. 打开对应 XML，检查该语句是否缺 `databaseId` 划分。把方言专有语法移进 `databaseId="mysql"` 的语句副本，另写一份 `databaseId="sqlite"`（需要时）与一份**两者都兼容**的兜底语句。
3. 兜底语句的语法要「三库通用」：避免 `ON DUPLICATE KEY UPDATE`、`INSERT IGNORE`、`JSON_函数`、反引号、`LIMIT offset,size` 之外的 MySQL 扩展。等价逻辑能用 MyBatis Plus 的 BaseMapper 方法（它们内部已按方言生成）就不要手写。

【预防】新增 XML 语句时问一句「这条 SQL 在 H2 上能不能跑」；测试基线 588 个测试会替你把关，但前提是**功能测试覆盖到该语句**——纯 XML 分支若没有测试触达，方言坑会潜伏到 CI 的 mysql job 才炸（而 mysql job 恰恰是全仓唯一真 MySQL 验证）。

#### E-B12 事务失效六场景（`@Transactional` 写了却没生效）

【现象】统一的表现是「该回滚的没回滚」：例如 `likePost` 里先加点赞再抛异常，点赞却已经入库；或「该提交的没提交」，方法里两次写库一半成功一半丢失。而方法上明明白白标着 `@Transactional`。

【原因】`@Transactional` 基于 Spring AOP 代理实现：调用先落到代理，代理负责开启事务、执行目标方法、按结果提交/回滚。任何「绕过代理」或「异常没传到代理」的写法都会让事务静默失效。SelfBlog 的高频六场景：

1. **同类自调用**：`this.methodB()` 走的是目标对象自身，不经过代理，`methodB` 上的 `@Transactional` 不生效（机制同 E-F 前端无关，另见 E-B13 的 `@Async` 同款陷阱）。
2. **非 public 方法**：CGLIB/动态代理只代理 `public` 方法，`protected`/包私有/`private` 上的 `@Transactional` 被忽略。
3. **异常被吞**：方法内部 `try { ... } catch (Exception e) { log.error(...) }`，代理永远看不到异常，只会提交。
4. **`rollbackFor` 缺省只回滚 `RuntimeException`/`Error`**：受检异常（如 `IOException`）默认**不回滚**。SelfBlog 约定交互服务标 `@Transactional(rollbackFor = Exception.class)` 就是为了堵这个口子——只写裸 `@Transactional` 是常见疏漏。
5. **传播行为误用**：`REQUIRES_NEW` 期望独立提交，却写成 `REQUIRED`，外层回滚把内层也拖下水（或反之，内层 `NOT_SUPPORTED` 把外层事务挂起，写库变成无事务裸奔）。
6. **多线程**：在方法里 `new Thread(...).executeUpdate()`——事务绑定在 ThreadLocal 的连接上，子线程拿不到父线程的事务，子线程的写操作自成事务、立即提交，父线程回滚时它不跟。

【解法】逐条对照：

1. 自调用 → 把方法挪到另一个 Bean（SelfBlog 的门面/查询/交互拆分天然解决了大部分自调用需求），或注入自身代理（`@Autowired private BlogPostService self;` 后经 `self.methodB()` 调用）。
2. 改 `public`；工具方法本就不需要事务注解。
3. 业务上确需吞掉异常又要回滚的，catch 后 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();`（或干脆重新抛出业务异常）。
4. 交互类服务统一 `@Transactional(rollbackFor = Exception.class)`；查询类 `@Transactional(readOnly = true)`。
5. 传播行为改动前画清事务边界图；不确定就保持默认 `REQUIRED`。
6. 跨线程的原子性用数据库层手段（唯一约束、原子 UPDATE 计数）或 MQ 补偿，不要指望注解跨线程。

【预防】把六场景做成评审 checklist；写「失败必须回滚」的集成测试（MockMvc/`@SpringBootTest` 里调用后断言库内无残留），让失效在 CI 暴露而不是在生产事故里暴露。

#### E-B13 `@Async` 同类调用失效：异步变同步

【现象】给方法标了 `@Async`，期望「点了按钮立刻返回，后台慢慢发邮件」；实测接口慢如同步，或调用方根本没等待它完成却也没异步执行（日志显示方法其实在主线程里跑）。

【原因】与 E-B12 场景 1 同根：AOP 代理。`@Async` 的语义是「经代理调用时，代理把任务丢进执行器并立即返回」。**同类内部调用 `this.sendMail()` 不经过代理**，异步注解被无视，方法在你眼前同步执行。另一个常见伴生坑：项目没配 `@EnableAsync` 或没自定义执行器时，Spring Boot 会给一个默认的 `SimpleAsyncTaskExecutor`（不复用线程）甚至行为可疑，异步看似生效但隐患多。

【解法】异步方法放到**独立的 Bean**（SelfBlog 的分层里适合放进专门的通知/事件组件），从另一个 Bean 经代理调用；确认启动类或配置类上有 `@EnableAsync`；需要稳定线程池时自定义 `ThreadPoolTaskExecutor` Bean 并在 `@Async("beanName")` 里点名。调试时在方法第一行打印 `Thread.currentThread().getName()`——如果还是 `main`（或测试线程名），异步就没生效。

【预防】评审规则：「`@Async` 方法必须被外部 Bean 调用」；给异步方法写测试时注意测试上下文里执行器可能同步执行（测试 profile 有独立配置），断言「最终状态」而不是「立即状态」。

#### E-B14 JWT 报错三件套：ExpiredJwtException / 签名不一致 / Refresh token 拿去访问业务接口

【现象】三族报错：

```text
io.jsonwebtoken.ExpiredJwtException: JWT expired at 2026-10-17T20:00:00Z. Current time: 2026-10-17T21:00:00Z
io.jsonwebtoken.security.SignatureException: JWT signature does not match locally computed signature. JWT validity cannot be asserted...
```

以及 401 响应体：`{"code":401,"message":"Refresh token cannot be used for API access"}`。

【原因】

- **Expired**：`jwt.expiration`（access token 时长）到期。`JwtAuthenticationFilter` 校验 `isTokenExpired` 失败后不设认证，安全层回 401——这是**设计内的正常现象**，触发前端刷新流程（E-F10），不是事故。
- **签名不一致**：签发与校验用了不同的 `JWT_SECRET`。高频成因是 E-B04 的引号陷阱（`.env` 里 `JWT_SECRET="..."` 让文件值带引号，与系统环境变量里的裸值不同源），或前后端/本地与服务器密钥不同步。签名校验是恒等比较，一个字节都不能差。
- **Refresh token 误用**：`JwtUtils` 给 refresh token 加了 `type=refresh` claim。过滤器发现「refresh token 出现在非 `/user/refresh` 的请求」时直接回 401 并记安全事件 `REFUSED_REFRESH_TOKEN`——防止有人拿长效 refresh token 冒充 access token。

【解法】

- 过期 → 属预期流程：前端应走刷新队列（E-F10），若用户被弹到登录页，检查前端刷新逻辑而不是把 token 有效期调大。
- 签名不一致 → 核对**应用启动时实际读到的密钥**：临时在启动参数加 `--logging.level.com.selfblog=DEBUG` 或在 `.env` 排查引号；确保签发（登录接口所在实例）与校验是同一份 `JWT_SECRET`。轮换密钥后所有旧 token 作废，这是预期行为。
- Refresh 误用 → 前端检查请求拦截器：是不是把 localStorage 里的 refresh token 当 access token 塞进了 `Authorization` 头。

【预防】access/refresh token 分开存（不同 localStorage 键），请求拦截器只注入 access token；`.env` 不加引号（E-B04）；多实例部署用同一密钥源（如密钥管理服务），避免「A 实例签发 B 实例不认」。

#### E-B15 CORS 报错：与其配置，不如消灭

【现象】浏览器 Console 出现经典报错：

```text
Access to XMLHttpRequest at 'http://localhost:8825/api/post/list' from origin
'http://localhost:3000' has been blocked by CORS policy:
Response to preflight request doesn't pass access control check:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

且 Network 面板里 OPTIONS 请求先于真实请求出现（预检失败）。

【原因】浏览器同源策略：协议+域名+端口任一不同即为跨域，跨域的 XHR 需服务器显式授权（响应头），带自定义头（如 `Authorization`、`X-Visitor-Id`）还会先发 OPTIONS 预检。注意：**CORS 是浏览器行为**，curl/Postman 不报 CORS；若 curl 正常而浏览器报错，方向就对了。SelfBlog 的后端虽有 `CorsConfigurationSource`（默认放行 `http://localhost:3000` 等本地源），但项目的推荐架构是**根本不让跨域发生**：

- 开发期：Vite dev server（3000）把 `/api` 代理到 8825，浏览器只和 3000 说话，同源。
- 生产期：`vercel.json` 是一个**反向代理生成器**（`framework: null` + `build-vercel-proxy.mjs`，构建成 Build Output v3 的转发规则），把前端域下的 `/api` 转发到后端 origin——前端与「API 所在域名」同源。

【解法】优先修代理而不是加 CORS 头：

1. 确认前端请求都走相对路径 `/api/...`（`baseURL` 硬编码为 `/api`，见 E-F07）。
2. 开发期核对 `vite.config.ts` 的 `VITE_API_TARGET`（默认 `http://localhost:8825`），代理 404 是另一类问题（E-F07）。
3. 生产期核对 Vercel 环境变量 `SELFBLOG_BACKEND_ORIGIN` 指向真实后端 origin（自引用/非法 origin 会在构建期失败）。
4. 真需要跨域直连（如第三方前端嵌入）时，在后端 `cors.allowed-origins` 里补允许的源——改配置而不是改代码。

【预防】新功能一律走同源代理拓扑；出现 CORS 报错先问「为什么这个请求没走代理」，而不是「怎么给后端加个 CORS 头」。

#### E-B16 限流 429：新接口不在已有前缀下，默默不限流或一上量就 429

【现象】压测或频繁刷新时拿到：

```text
HTTP/1.1 429 Too Many Requests
{"code":429,"message":"Too many requests, please try again later"}
```

或者反问题：新写的接口怎么刷都不限流，担心被打爆。

【原因】`RateLimitInterceptor` 按**路径前缀**匹配 `RateLimitProperties` 的阈值表（Caffeine 计数，1 分钟滑动窗口，超限回 429 JSON）。匹配逻辑取请求的 lookup path，在阈值表里做**最长前缀匹配**（`path.equals(base) || path.startsWith(base + "/")`）；**没匹配上任何前缀时直接放行**（`return true`）。所以「新接口不限流」的原因就是它的路径前缀不在配置表里；而「动辄 429」的原因通常是命中了过严的前缀阈值（如登录、发码这类敏感前缀刻意收得很紧）。限流计数 key 是 `客户端IP + 路径`，IP 取 `X-Forwarded-For` 第一段或对受信代理的 `X-Real-IP`，本机开发时人人都是 `127.0.0.1`，自己刷自己 60 次就会撞线。

【解法】打开 `RateLimitProperties`，阈值表分两类：精确路径（如 `/api/user/login`）与前缀（如 `/api/post/`）。新增接口：

1. 若落在已有前缀下（`/api/post/xxx` 天然在 `/api/post/` 下），确认默认阈值合适即可；
2. 若是新前缀（如 `/api/visitor/...` 初期没登记），**必须补一条阈值**，否则它永不限流；
3. 特殊路径配精确条目压过前缀条目（最长前缀匹配保证精确条目优先）。
4. 本机调试被 429 卡住：重启应用（Caffeine 缓存在内存里，重启即清零），或临时调大开发环境阈值。

【预防】新增公开接口的 checklist 加一项「限流前缀是否覆盖」；压测脚本里把 429 当作预期分支而不是失败；上线前用低频冒烟确认关键路径未被误限。

#### E-B17 Bean Validation 不生效：starter 缺失 / `@Valid` 不级联 / `@Validated` 与 `@Valid` 选错

【现象】三族。其一，DTO 上标了 `@NotBlank`、`@Size`，接口却对空字符串照单全收，没抛 400。其二，嵌套对象里的字段无效，外层校验却通过了。其三，Controller 方法参数校验用了 `@Validated`（或反之），行为与预期不符：

```text
期望: {"code":400,"message":"标题不能为空"}
实际: 正常入库
```

【原因】

1. **校验器根本不在 classpath**：`spring-boot-starter-validation` 不是 web starter 的传递依赖（Spring Boot 2.3 起被移出），没显式引入就没有实现类，`@Valid` 成了无人执行的注解——不报错、不生效，最迷惑。
2. **级联需要显式 `@Valid`**：嵌套对象字段（如 DTO 里的 `Author author`）上必须也标 `@Valid`，校验才会递归进入嵌套对象；不标就停在外层。
3. **`@Valid` 与 `@Validated` 的分工**：`@Valid`（JSR-380）用于方法参数触发级联校验、支持分组；`@Validated`（Spring）支持**分组校验**且是让**类级方法校验**（`@NotNull` 标在方法参数上配合 `@MethodValidation`）生效的开关。混合使用时行为差异容易被误读为「不生效」。

【解法】

1. `pom.xml` 确认有 `spring-boot-starter-validation`；没有就加，重启后再试。
2. 嵌套 DTO 的字段上加 `@Valid`：`@Valid private Author author;`。
3. Controller 方法参数用 `@Valid @RequestBody XxxDTO dto` 触发实体校验；需要分组时换 `@Validated(GroupA.class)`。
4. 校验失败的统一出口是 `GlobalExceptionHandler`（`MethodArgumentNotValidException` → 400 信封）；若前端拿到的是裸 500，检查异常处理器是否覆盖了对应异常类型。

【预防】新 DTO 模板自带 `@NotBlank` 等基础注解与嵌套 `@Valid`；写一条「空标题返回 400」的切片测试作为回归哨兵。

#### E-B18 MyBatis Plus 分页不生效：查了全表

【现象】调用 `Page<Post>` 的查询，`getTotal()` 返回的却不是数据库真实总数，而是当前页条数；或 SQL 日志里看不到 `LIMIT` 子句，一次查出全表几万行。

【原因】MyBatis Plus 的分页是**插件机制**（`MybatisPlusInterceptor` + `PaginationInnerInterceptor`），不是默认能力。没装插件时，`Page` 参数被当普通参数忽略，查询原样执行全表，分页对象里只有当前页数据，total 与 records 同值。另一个次要成因：多方言项目里 `PaginationInnerInterceptor` 没显式指定 `DbType`，某些版本会按第一条 SQL 自动探测，测试库（H2）与生产（MySQL）探测结果不同，表现时好时坏。

【解法】在 `MybatisPlusConfig`（已存在）里确认拦截器链：

```java
MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
// 按需为 sqlite/h2 补充对应 DbType 的分页内拦截器
```

注意插件顺序（分页插件通常放在最后）；确认后用 SQL 日志验证 `LIMIT ?` 出现且 total 是独立 COUNT 查询的结果。

【预防】把「分页必须出 LIMIT + COUNT 两条 SQL」写进查询服务的日志断言或测试；新环境首次部署跑一次大表分页冒烟。

#### E-B19 雪花 ID 传给前端精度丢失：ID 尾位变 0

【现象】后端返回 `{"id": 990001234567890123}`，前端拿到再回传或显示时变成 `990001234567890100`——末三位变了。更诡异的表象：详情页「文章不存在」（因为拿错 ID 去查库）。

【原因】MyBatis Plus 主键默认走雪花算法（ASSIGN_ID），ID 是 19 位左右的长整数。而 JavaScript 的 `Number` 是 IEEE 754 双精度浮点，安全整数上限是 2^53-1（约 9.007e15，16 位）。19 位 Long 超出后，`JSON.parse` 舍入尾数，ID 静默失真——**不报错，只丢精度**，所以排查时容易往「数据库脏数据」方向跑偏。

【解法】序列化层面把 Long 转 String。SelfBlog 的约定是统一在 Jackson 配置里把 `Long`/`long` 序列化为字符串（全局 `ToStringSerializer`），前端拿到的是 `"id": "990001234567890123"`，无精度损失；前端把 ID 当**字符串**处理（比较用 `===` 且不转数字）。排查时若发现某接口仍有数字型 19 位 ID，检查该 DTO 是否绕过了全局序列化配置（如手动 `new ObjectMapper()`）。

【预防】前端 TS 类型定义里 ID 一律 `string` 而非 `number`；后端不出现绕过全局 ObjectMapper 的手写序列化；联调时用「ID 尾位非 0」的记录做冒烟。

#### E-B20 前端读错 Result 信封：`res.data.data` 是 `undefined`

【现象】后端用 curl 验明明返回 `{"code":200,"message":"ok","data":{...}}`，前端却读到 `undefined`；控制台报 `Cannot read properties of undefined (reading 'xxx')`。

【原因】`frontend/src/api/index.ts` 的 **Axios 响应拦截器返回的是后端信封本身，不是 `AxiosResponse`**——拦截器里直接 `return response.data`（即整个 `{code,message,data}` 信封）。所以 `api.get<T>()` resolve 出来的**已经是信封**：调用方应读 `res.data`（信封的业务数据），而**不是** `res.data.data`（那是「信封的 data 再往下取一层」，而信封上并没有再嵌一层 data）。这条契约和通用 axios 心智模型相反，是 SelfBlog 最常被踩的坑。

【解法】调用代码统一写成：

```ts
const res = await api.get<PageResult<Post>>('/post/list', { params });
// res 就是 { code, message, data } —— 业务数据在 res.data
postList.value = res.data.records;
```

类型层面给 `api.get<T>()` 的 `T` 传**业务数据类型**（拦截器已剥壳），不要传信封类型再二次取 `res.data.data`。

【预防】代码评审盯着 `res.data.data` 这个模式；新页面照抄成熟页面的取数写法；若真想拿到原始 `AxiosResponse`（如读 header），那是另一条路径——别在拦截器返回值上「顺手多剥一层」。

#### E-B21 JSON 日期格式与时区不一致：差八小时

【现象】后端存库 `2026-10-17 22:30:00`，前端显示 `2026-10-18 06:30` 或 `2026-10-17T14:30:00.000+00:00`；两台机器显示差 8 小时（中国时区偏移量）。

【原因】三层叠加：数据库连接时区（JDBC URL 的 `serverTimezone`）、JVM 时区（容器里常是 UTC）、序列化格式（Jackson 默认把 `LocalDateTime` 写成 ISO-8601 数组或字符串，`Date` 写成时间戳）。任何一层不一致，最终 JSON 里的时间就会漂移。SelfBlog 用 `LocalDateTime` 与统一 Jackson 配置（固定格式、无时区转换），漂移一般出在「连接层」或「容器 TZ」。

【解法】

1. JDBC URL 显式 `serverTimezone=Asia/Shanghai`（MySQL）；
2. 容器/服务器设 `TZ=Asia/Shanghai`（Dockerfile/compose 里加环境变量）；
3. Jackson 统一日期格式（全局配置 `yyyy-MM-dd HH:mm:ss`），前端拿到后只做展示层格式化，不做时区换算（因为字符串里没有时区语义，换算必错）。

【预防】前后端契约写明「所有时间字段为无时区语义的本地时间字符串」；CI 的 docker job 冒烟可以顺手断言一个已知时间字段的输出格式。

#### E-B22 Hikari 池大小 1 与 SQLite 数据库锁：并发写变卡甚至报锁超时

【现象】本地 SQLite 模式下，两个页面同时触发写操作（如同时点赞与评论），后者长时间等待后报：

```text
org.sqlite.SQLiteException: [SQLITE_BUSY] The database file is locked
```

或日志里连接获取缓慢、超时。

【原因】SQLite 是**整库单写锁**：同一时刻只允许一个写事务。SelfBlog 的 SQLite 数据源把 Hikari 连接池设为**池大小 1**——这是配合 SQLite 语义的刻意选择（多连接并发写只会互锁，一个连接排队反而干净）。因此「并发写慢」是 SQLite 的本质行为：写操作在池里串行排队。真正的错误（`SQLITE_BUSY`）通常来自**池外**连接：另一个进程（如开着 DB Browser 工具直接改库文件）持有写锁，应用这条唯一连接也抢不到。

【解法】

- 开发期避免用第三方工具长事务锁着 `campus_blog.db`；查看数据建议用只读方式或快照副本。
- 确认没有别的 JVM 实例同时在跑（上一个没关的服务就是「池外连接」）。
- 真需要并发写的场景，换 MySQL（`DB_TYPE=mysql`），那是行级锁引擎；SQLite 定位是零运维单机。

【预防】理解并接受「SQLite + 池 1 = 写串行」是特性不是缺陷；性能测试一律在 MySQL 上做；本地频繁写库调试时用 H2 或 MySQL，别拿 SQLite 复现并发问题。

### A.1.4 高频配置与运行陷阱补遗

本小节收录不够「报错级」、但提问频率极高的问题——它们往往不以异常栈的面目出现，而以「行为不符合预期」的面目出现，更容易让初学者怀疑人生。

#### E-B23 Knife4j 文档页打不开：profile 与安全放行的双重门

【现象】访问 `http://localhost:8825/doc.html` 得到 404 或空白页；或接口在文档页里能调通、前端调用却 401，怀疑「后端行为不一致」。

【原因】两道门叠着。其一，Knife4j 在 `prod` profile 下被刻意关闭——生产环境不暴露 API 文档是安全底线，只有开发/测试 profile 能访问；其二，文档相关路径（`/doc.html`、`/swagger-resources/**`、`/v3/api-docs/**`）虽在 `HttpRequestLoggingInterceptor` 的排除名单里（不记请求日志），但「不记日志」不等于「安全放行」，SecurityConfig 没放行照样被拦。另外文档页里「试着调用」走的是浏览器直发请求，与前端代理拓扑完全无关——文档页通与不通，都不能当作前端链路好坏的证据。

【解法】确认启动时激活的 profile 不是 prod（看启动日志第一屏的 profile 行）；联调用开发 profile 起服务再访问 `/doc.html`。文档页里调不通某接口时，回到 curl 验证（E-B08），排除文档页自身的鉴权上下文干扰。

【预防】生产部署检查单加一条「/doc.html 不可访问」——API 文档泄露也是信息泄露；升级 Spring Boot / Knife4j 版本后过一遍文档页冒烟。

#### E-B24 日志里的 `[PERF] ... error:` 不是故障

【现象】跑后端测试时控制台大量刷出：

```text
[PERF] ... error: ...（堆栈或消息）
```

初学者以为是测试失败或服务异常，慌忙逐行排查，越看越慌。

【原因】这是 `PerformanceMonitoringAspect` 的正常输出：后端测试套件里有大量**负例断言**——故意制造失败请求来验证 401/403/400 行为是否符合预期。切面把这些「预期内的失败」也如实记录，于是日志观感一片红字。真正的裁决是测试计数行：`Tests run: 588, Failures: 0, Errors: 0`——以计数为准，不以日志颜色为准。

【解法】看构建结果只看两处：各测试类的计数行与最终的 `BUILD SUCCESS/FAILURE`。过滤噪音：

```powershell
backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true" | Select-String -Pattern 'Tests run:|BUILD'
```

【预防】把「`[PERF]` 是正常负例记录」写进新人 FAQ；CI 判断成败永远看退出码与计数行，不看日志观感。

#### E-B25 缓存「不更新」：Caffeine 具名缓存的 TTL 与测试隔离

【现象】管理端改了站点设置/分类/标签，前台页面刷新后仍是旧值，持续几分钟后「自己好了」；或写集成测试时，上一步刚写入的数据下一步查到的是旧值。

【原因】`CaffeineCacheConfig` 用 `SimpleCacheManager` 注册了 8 个具名缓存，各自独立 TTL：`hotTagsCache`(10m)、`categoryCache`(30m)、`userCache`(10m)、`trendingCache`(5m)、`statsCache`(15m)、`statusCache`(2m)、`siteSettingsCache`(10m)、`visitorSummaryCache`(30s)。查询服务（`PostQueryService` 等）上的 `@Cacheable` 命中期间，数据库里的变化对读方不可见——「改了没生效」多数不是写失败，而是**读命中了缓存**。测试 profile 下缓存整体关闭，所以「测试里即时生效、真实运行有延迟」的落差是设计使然。

【解法】开发期验证数据变化：重启应用（内存缓存随 JVM 清空），或等对应 TTL 过期。要定位「哪个缓存在作怪」就对着上面清单按业务对号：站点设置 → `siteSettingsCache`(10m)，分类 → `categoryCache`(30m)，热门标签 → `hotTagsCache`(10m)，趋势 → `trendingCache`(5m)，统计 → `statsCache`(15m)，状态 → `statusCache`(2m)，访客汇总 → `visitorSummaryCache`(30s)，用户 → `userCache`(10m)。若业务上不能接受延迟，检查对应写路径是否已配 `@CacheEvict`（或手动 evict）——SelfBlog 管理端的关键写路径已配，新增写接口时别漏。

【预防】建立第一反应：遇到「读旧值」先问「TTL 多少、缓存名哪个」，再怀疑写路径；新增 `@Cacheable` 必须同步回答「什么时候失效」。

#### E-B26 安全事件审计：从具名日志回放攻击面

【现象】日志里出现成组的 `JWT_AUTH_FAILED`、`REFUSED_REFRESH_TOKEN`，想知道是不是有人在试探接口。

【原因】`JwtAuthenticationFilter` 与安全链把可疑行为记成**具名事件**而不是含糊的 WARN（见 A.0.7）：解析失败 → `JWT_AUTH_FAILED`，refresh token 误用于业务接口 → `REFUSED_REFRESH_TOKEN`。配合请求级的 `X-Trace-Id` 与限流的来源 IP 记录（E-B16 的 key 里含 IP），可以从日志回放「谁、从哪、试了什么」。

【解法】按事件名聚合分桶统计来源与时间分布：

```powershell
Select-String .\logs\app.log -Pattern 'JWT_AUTH_FAILED' |
  ForEach-Object { ($_.Line -split ' ')[0].Substring(0,13) } |
  Group-Object | Sort-Object Count -Descending | Select-Object -First 10
```

单一 IP 高频 → 单点试探，考虑收紧对应前缀限流阈值；多 IP 低频 → 常规扫描噪音，确认限流在挡即可。顺手核对 `cors.allowed-origins` 没有被放宽到通配符。

【预防】养成每月扫一次安全事件分布的习惯；事件量突增时先查自己的前端是不是在循环重试（E-F10 的刷新风暴也会刷出 `JWT_AUTH_FAILED`），再断定是外部攻击。

#### E-B27 接口报错「HTTP 全是 200」或「全是 500」：GlobalExceptionHandler 的映射链

【现象】两族面目。其一：请求明明业务失败，HTTP 状态却是 200，body 里 `code` 才是真实错误码，前端必须读两层判断；其二：任何异常（包括参数错误）统统变成 HTTP 500，丢失业务语义。

【原因】SelfBlog 的错误出口是**统一**的：业务错误抛 `BusinessException(code, message)`，由 `GlobalExceptionHandler`（`@RestControllerAdvice`）把业务 code 映射为对应 HTTP 状态，未识别的异常兜底 500。两条现象分别对应这条链的两处断裂：全 200 通常是有人在 Controller 里 try-catch 后自己组了个 `Result` 错误体正常返回（绕过了 advice）；全 500 通常是异常类型不在 advice 的处理分支里（比如抛了个自定义异常但没写对应 `@ExceptionHandler`），落到兜底。

【解法】业务失败统一 `throw new BusinessException(code, message)`，不自组错误响应体；新增异常类型时先读 `GlobalExceptionHandler` 现有分支，缺哪个补哪个 `@ExceptionHandler`，映射 HTTP 状态遵循「401 未认证、403 无权限、404 不存在、400 参数、429 限流、500 兜底」。排查存量「全 200」接口：搜 `catch (Exception` 在 Controller 层的出现，逐个清理。

【预防】错误响应只有一个出口（advice）；code review 时 Controller 里出现 try-catch 包业务调用要说明理由。

#### E-B28 取当前用户抛 401：`getCurrentUserId` 与 `getCurrentUserIdOrNull` 用错场景

【现象】游客也能访问的接口（文章详情、匿名点赞）却在未登录时抛 401「未登录」；或本应「未登录返回空集合」的逻辑直接报错。前端表现为：游客打开详情页被当成未授权。

【原因】`utils.SecurityUtils` 提供两个语义不同的方法：`getCurrentUserId()` 未登录**抛 401**（面向必须登录的写操作），`getCurrentUserIdOrNull()` 未登录返回 `null`（面向游客场景，如详情页判断「当前用户是否作者本人」）。用错方法 = 语义错位：游客路径调了强校验方法。

【解法】按「这个接口游客会不会走到」选方法：公开只读接口里的用户上下文一律 `getCurrentUserIdOrNull()` 并处理 null 分支；写操作（发布、修改、删除）用 `getCurrentUserId()` 让 401 自然发生。排查存量：搜 `getCurrentUserId()` 的全部调用点，逐个确认所在接口是否在 SecurityConfig 公开放行名单里。

【预防】新增公开接口时把「游客走到这里会发生什么」写进接口注释；与 E-B08 的 401 排查呼应——401 未必是安全配置问题，也可能是代码用错了取用户方法。

#### E-B29 `/uploads` 图片 404：静态资源映射、路径拼接与代理转发三连

【现象】上传接口返回成功，库里存的 URL 是 `/uploads/xxx.png`，但浏览器访问 404；或本机能看图，部署后全 404。

【原因】`/uploads/**` 不是 Controller，而是 `WebMvcConfig.addResourceHandlers` 映射到磁盘目录 `file:{upload.root-path}/{upload.base-path}/` 的静态资源。404 有三个可能断点：① 磁盘文件的真实落点与「两个配置键拼出来的绝对路径」不一致（相对路径受启动工作目录影响——和 E-B04 的查找顺序是同族问题）；② 请求根本没到后端——Vite dev 代理与 vercel.json 反代都必须显式包含 `/uploads` 的转发规则，漏了就是前端 404；③ 部署后容器/服务器上的卷没有挂载到配置目录。

【解法】按断点排查：先看 Network 里 404 的响应是 JSON（到了后端）还是 HTML（没到后端，查代理）；到了后端再核对磁盘：把配置拼出的绝对路径完整算出来，去文件系统找刚上传的那张图，对不上就改 `upload.root-path` 为绝对路径；部署场景检查卷挂载点与配置一致。

【预防】上传根目录一律绝对路径；部署检查单加「上传一张图并回显成功」；改上传相关配置必须同步检查 Vite 代理与 vercel.json 两处转发规则。

#### E-B30 注册接口返回 403「此博客不开放访客注册」：这是设计，不是 bug

【现象】调用 `/auth/register/*` 或 `POST /user/register` 收到 403 与文案「此博客不开放访客注册」，排查者以为是安全链拦截或权限配错。

【原因】SelfBlog v2.0.x 由多用户校园社区改造为**单人作者博客**：注册通道整体关闭，上述接口在业务层直接抛 `BusinessException(403, "此博客不开放访客注册")`；后台登录也只接受管理员账号。403 来自业务判断而非 Spring Security——这正是 E-B08 三分法的一个特例：有身份（或无身份）但业务明确拒绝。

【解法】无需修复。登录使用管理员账号（`ADMIN_PASSWORD`）。若真要恢复注册，那是产品级决策，牵涉 SecurityConfig 放行、注册限流阈值、前端注册页（已删除）多处置建，不是一个开关的事。

【预防】读 CHANGELOG 与 `CLAUDE.md` 了解 v2.0 的定位转变；不要按旧教程/旧接口文档测试注册流程。

---

## A.2 前端错误排查（frontend/）

前端源码根在 `frontend/src/`。同样按编译期（类型/构建工具）→ 启动期（dev server 与运行时初始化）→ 运行期（浏览器行为、测试）组织，编号 E-F01 起。

### A.2.1 编译期 / 工具链错误

#### E-F01 `'"node"' 不是内部或外部命令`：PATH 里一个引号毁掉所有 npm run

【现象】在 `frontend/` 下执行任何 `npm run <script>`（dev/build/test:run 都不例外）报：

```text
'"node"' 不是内部或外部命令，也不是可运行的程序
或批处理文件。
```

诡异之处：`npm ci`、`npm install` 一切正常；`where node` 也能找到 node；只有 `npm run` 全灭。

【原因】这是一台机器的 PATH 伤口，与仓库无关。系统 PATH 中有一项形如 `C:\Program Files\Java\jdk-25\bin"`——**末尾带了一个不配对的引号**。`cmd.exe` 解析 PATH 时把它当作未闭合引号的开始，其后所有条目全部失效。`node` 无法被 cmd 解析，而 `node_modules/.bin/*.cmd` 垫片（npm run 实际执行的脚本）结尾正是调用裸 `node`——于是 npm run 全灭。`npm ci` 正常是因为它不经过 .bin 垫片；`where node` 正常是因为它走 App Paths 注册表而不是 PATH。bash 不受影响（它不用 cmd 的解析规则）。

【解法】两条路：

1. **临时**：不跑 `npm run`，直接调用 POSIX 垫片或仓库预置的 Node。仓库约定的一组可用命令：

   ```powershell
   # 把后端构建时下载的 Node 前置到 PATH（target 清理后需重建后端才有）
   $env:PATH = 'C:\…\SelfBlog\backend\target\frontend\node;' + $env:PATH
   # 或者直接调用垫片（不依赖 PATH）：
   ./node_modules/.bin/vite            # = npm run dev
   ./node_modules/.bin/vitest run      # = npm run test:run
   ./node_modules/.bin/vue-tsc --noEmit # = npm run type-check
   ./node_modules/.bin/eslint src      # = npm run lint:check
   ```

2. **根治**：打开系统环境变量编辑器，找到 PATH 里带不配对引号的那条（`jdk-25\bin"`），删掉多余的引号，重启终端。逐项检查 PATH：任何一条「看起来带引号」的 Windows PATH 条目都是错的——Windows PATH 各项**从不**用引号包裹。

【预防】重装系统/配置环境变量后跑一次冒烟：`npm run --version` 或任意仓库脚本。把「本机五闸门用 ./node_modules/.bin/ 直调」写进前端文档的快速开始，CI 不受本机 PATH 影响。

#### E-F02 TS2532 / TS18048 / TS2339 / TS2307 / TS7016：strict 模式五连报

【现象】`vue-tsc --noEmit` 报错，五种高频编码：

```text
src/views/PostList.vue(42,18): error TS2532: Object is possibly 'undefined'.
src/stores/user.ts(88,5): error TS18048: 'token' is possibly 'undefined'.
src/api/post.ts(21,7): error TS2339: Property 'records' does not exist on type '...'.
src/main.ts(6,23): error TS2307: Cannot find module '@/stores/site' or its corresponding type declarations.
src/utils/markdown.ts(15,30): error TS7016: Implicitly has an 'any' type because it does not have a type declaration and its containing type does not have 'strict' enabled... (实际措辞为 no declaration file for module 'xxx')
```

【原因】逐个拆：

- **TS2532 / TS18048**：`tsconfig.json` 开了 `noUncheckedIndexedAccess`（索引访问返回 `T | undefined`）与 `strict`（可空类型必须收窄）。`arr[0].foo`、`map.get(k).x`、`split(':')[0]` 这类写法直接不过编译——不是编译器刁难，是它们真的可能 undefined。
- **TS2339**：属性在类型上不存在。高频根因是 E-B20 的信封契约：给 `api.get<T>()` 传错了 `T`（传了信封类型却读业务字段，或反之）。
- **TS2307**：路径别名（`@/`）没配或拼写错；也可能是把 `.vue` 文件当模块引却缺 `env.d.ts` 里的 declare module。
- **TS7016**：引入了无类型声明的第三方库（老库或无 `@types` 包）。

【解法】按码下药：

1. TS2532/18048：用可选链与空值合并收窄——`arr[0]?.foo ?? 默认值`；对「确定存在」的场景用显式守卫：

   ```ts
   const first = parts[0];
   if (first === undefined) return fallback;
   ```

   SelfBlog 推荐的习惯：**能用 `replace` 就不用 `split()[0]`**——`path.replace(/^.*:/, '')` 语义直接且不产生 undefined 元素。
2. TS2339：先看该调用点走的是哪个 API 封装，对齐信封契约（`res.data` 是业务数据）。
3. TS2307：检查 `tsconfig` 的 `paths` 与 `env.d.ts`；确认引入的是目录还是文件、有没有拼错大小写。
4. TS7016：补 `d.ts` 声明文件 `declare module 'xxx';`（有类型包就装 `@types/xxx`）；确无类型时用最小接口收窄，不要裸 `any`。

【预防】编辑器装 Volar 并开保存即校验，让 TS 错误在写代码时暴露而不是 build 时；新组件从成熟页面复制类型姿势。

#### E-F03 vue-tsc 慢/吃内存；build 与 type-check 双跑

【现象】`npm run type-check` 或 `npm run build` 时 `vue-tsc` 跑十几秒到一分钟，机器风扇狂转；CI 里 build 步骤比 test 慢一倍。

【原因】`package.json` 的 build 脚本是 `vue-tsc --noEmit && vite build`——**先完整跑一遍类型检查，再做 vite 打包**。而 type-check 脚本本身也是 `vue-tsc --noEmit`。所以「先 type-check 再 build」的流程里，vue-tsc 实际执行了**两遍**。这是刻意的质量闸门（vite 的 esbuild 转换**不做类型检查**，不跑 vue-tsc 的打包等于放弃 TS 的保护），代价是时间与内存。

【解法】

1. 本地开发流程推荐「写代码时靠 Volar 实时校验，提交前才跑一次 build（内含 type-check）」——避免重复的全量 vue-tsc。
2. 只想快速验证类型时跑 `type-check` 单脚本，别跑完整 build。
3. 真觉得慢：给 vue-tsc 加 `--incremental`（利用 .tsbuildinfo 缓存）是官方路径，但先确认与 CI 缓存策略兼容再引入；不要为了快而砍掉 build 里的类型检查。

【预防】理解两遍跑的必要性（esbuild ≠ 类型检查）；CI 保持现状——它换来的是「vite build 永远建立在类型通过之上」的保证。

#### E-F04 `npm ci` 失败：engines 校验（engine-strict）

【现象】CI 或本机执行 `npm ci` 报：

```text
npm ERR! code EENGINE
npm ERR! engine Unsupported engine
npm ERR! engine Not compatible with your version of node/npm
npm ERR! engine Required: {"node":">=24.15.0"}
npm ERR! engine Actual:   {"npm":"10.x.x","node":"20.19.0"}
```

【原因】`frontend/.npmrc` 里写着 `engine-strict=true`（同文件还有 optional/save-exact/package-lock 三项）。engine-strict 让 npm 把 `package.json` 里 `engines` 字段从「建议」升级为「硬校验」。仓库 CI 的 frontend job 用 Node 24.15.0，与 engines 对齐；本机 Node 版本落后时，`npm ci` 直接拒绝。

【解法】切换到满足 engines 的 Node。本机推荐用仓库已有的：`$env:PATH = 'C:\…\backend\target\frontend\node;' + $env:PATH`（该目录由 frontend-maven-plugin 下载，见 E-F01）；或用 nvm/fnm 装对应版本。**不要**为了「先跑起来」删掉 engine-strict——它挡住的是「本机能跑、CI 挂」的版本漂移。

【预防】前端 README 写明 Node 版本要求与本机切换方法；本地 node 版本管理器（fnm/nvm）固定 per-project 版本（`.node-version` / `.nvmrc`）。

### A.2.2 启动期错误

#### E-F05 Vite 代理 404：`VITE_API_BASE_URL` 是个没人读的变量

【现象】dev server 起来了，页面请求 `/api/post/list` 返回 404；或你以为「改了环境变量就能改后端地址」，改了 `VITE_API_BASE_URL` 却毫无效果。

【原因】两条机制叠加：

1. `frontend/src/api/index.ts` 里 axios 实例的 `baseURL` **硬编码为 `/api`**——生产靠 `vercel.json` 反代、开发靠 Vite 代理把这个前缀转发到后端。
2. `VITE_API_BASE_URL` 在仓库里**没有任何代码读取**（grep 可验证），改它纯属自我安慰。真正决定「代理转发到哪」的是 `vite.config.ts` 里的 `VITE_API_TARGET`（默认 `http://localhost:8825`）。

代理 404 的具体成因按概率排序：后端没起（8825 无监听）→ 代理 target 写错 → 请求路径少了/多了 `/api`（对齐 E-B09 的前缀规则）。

【解法】

1. 后端确认在跑：`curl.exe -i http://localhost:8825/api/post/list` 应返回 200。
2. 打开 `frontend/vite.config.ts`，确认代理块里 target 是 `VITE_API_TARGET`（或其默认值）且路径正确：
3. 需要指向别的后端实例时，设置的是 `VITE_API_TARGET`（Vite 配置里通过 `process.env` 或 `loadEnv` 读取），不是 `VITE_API_BASE_URL`。
4. 重启 dev server（vite 配置改动不热更）。

【预防】把「`VITE_API_BASE_URL` 无人读取」写进前端文档的显眼位置（本附录就是），新人改代理只看 `vite.config.ts`。

#### E-F06 后台路由 404：`installAdminRoutes` 没在首次导航前跑

【现象】登录成功后访问后台路径（如 `/admin/xxx`）拿到 404 空页；或改了 `adminPath` 站点设置后，旧路径 404、新路径也不知道去哪了。

【原因】后台路由**不在静态路由表里**。`router/modules/admin.ts` 的 `createAdminRoutes` 是一个**运行时函数**，需要拿到服务端下发的 `adminPath`（站点设置，初值取 `ADMIN_PATH` 环境变量，默认 `admin`）后调用 `installAdminRoutes(adminPath)` 动态注册。`main.ts` 的启动顺序承重：

```ts
const siteStore = useSiteStore(pinia);
await siteStore.loadPublicSettings();      // 1. 拉站点设置（含 adminPath）
installAdminRoutes(siteStore.adminPath);   // 2. 动态注册后台路由
useUserStore(pinia).restoreFromStorage();  // 3. 恢复登录态
app.use(router);                           // 4. 安装路由
app.mount('#app');                         // 5. 挂载
```

第 2 步没在第 4 步（首个导航）之前完成，导航到后台路径时路由表里就没有它——404。

【解法】保持 `main.ts` 顺序原样；新增「必须在首屏前就绪」的初始化一律放进 bootstrap 序列（`loadPublicSettings` 之后、`use(router)` 之前），不要挪到 `onMounted` 或异步回调里。测试里复现这类问题用 `createMemoryHistory` 先 `push` 到后台路径再挂载，若 404 说明路由安装时机错了。

【预防】`main.ts` 顶部注释已写明顺序承重（本附录 E-F08 再强调一次）；代码评审对 main.ts 的 diff 保持最高敏感度。

#### E-F07 已登录用户被弹回登录页：`restoreFromStorage` 时机

【现象】刷新页面后，明明 localStorage 里有 token，却瞬间跳到登录页；DevTools Network 里首屏接口带着 401 返回。

【原因】路由守卫（`router/guards.ts`）在**首次导航**时就检查 `requiresAuth`/`requiresAdmin`：若此时 Pinia 里还没有恢复登录态（store 认为未登录），守卫会把用户重定向到 `siteStore.adminRoot`。而登录态的恢复靠 `useUserStore(pinia).restoreFromStorage()`——它必须在 `app.use(router)` **之前**执行（见 E-F06 的启动序列第 3、4 步）。若有人把它挪到 `mount` 之后或异步任务里，首个 `beforeEach` 就会「误杀」已登录用户。token 本身过期是另一回事（E-F10），本条专指**恢复时机**错误。

【解法】恢复 `main.ts` 原始顺序；若要加新的启动逻辑，用「插在 restoreFromStorage 之前、不 await 的 fire-and-forget」模式，别打乱 1-5 的编号顺序。

【预防】给启动顺序写一个测试：mock store 后按正确顺序初始化，断言「带 token 的内存路由首导航不重定向」——顺序回归在 CI 就被拦下。

### A.2.3 运行期错误

#### E-F08 jsdom 陷阱五连：测试里「看起来对」实际全错

【现象】vitest + jsdom 下组件测试的五类「过不去/过得了但错的」：

1. 挂载注册了全局监听器的组件，后续测试收到前一个测试的副作用（计数翻倍、路由被推送）。
2. 用 requestAnimationFrame 的组件测试不跑回调，或断言「立即」拿到帧后状态失败。
3. 想模拟滚动却报 `Cannot assign to read only property 'scrollY'`。
4. stub 掉 RouterLink 后，测试渲染出空链接、断言丢内容，且表达不了 active 态。
5. 依赖 `matchMedia` 的代码在测试里直接抛错。

【原因】jsdom 是「浏览器 API 的最小桩」，五个坑各有机制：

1. **setup 无自动卸载**：`src/test/setup.ts` 只桩了 `matchMedia` 与 `$router/$route`，**没有**全局 `afterEach(unmount)`。组件挂载时注册的 `window.addEventListener`/路由守卫会跨测试存活。
2. **rAF 桩成同步会破坏帧语义**：真实 rAF 是「下一帧回调」；把桩写成同步执行，组件里「帧间才发生」的逻辑（进度条、动画状态机）时序全乱。正确姿势是**排队后手动 flush**。
3. **`scrollY` 是只读 getter**：直接 `window.scrollY = 100` 会抛 TypeError（jsdom 里它是 getter-only 属性）。要用 `Object.defineProperty(window, 'scrollY', { value: 100, writable: true })` 覆盖后**派发 scroll 事件**让监听器跑起来。
4. **RouterLink stub 丢 slot 与 active 态**：`stubs: { RouterLink: true }` 把真实组件换成空壳，slot 内容（链接文字）没了，`router-link-active` 类名逻辑也没了。
5. **`matchMedia` 未桩**：组件初始化时调 `window.matchMedia('(prefers-color-scheme: dark)')`，jsdom 没实现，直接 `TypeError: window.matchMedia is not a function`。

【解法】

1. 测试文件里写 `enableAutoUnmount(afterEach)`（`@vue/test-utils` 提供），或 setup.ts 统一加（本项目约定**在每个需要的测试文件里显式写**，因为 setup.ts 不做自动卸载）。
2. 桩 rAF 为「入队 + 手动 flush」：

   ```ts
   let rafQueue: FrameRequestCallback[] = [];
   beforeAll(() => {
     vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => { rafQueue.push(cb); return rafQueue.length; });
   });
   afterEach(() => { rafQueue = []; vi.unstubAllGlobals(); });
   function flushRaf() { const q = rafQueue; rafQueue = []; q.forEach(cb => cb(performance.now())); }
   ```

3. 滚动模拟三件套：`Object.defineProperty(window, 'scrollY', { value: 120, configurable: true })` → `window.dispatchEvent(new Event('scroll'))` → `await nextTick()`。
4. 用真实路由：`mount(Component, { global: { plugins: [createRouter({ history: createMemoryHistory(), routes })] } })`——slot 内容与 active 类名都是真的。
5. setup.ts 已桩 matchMedia（返回带 `matches/addEventListener/removeEventListener` 的桩）；自建测试环境时照抄这个桩。

【预防】把五条抄进前端测试规范；新组件测试模板自带 `enableAutoUnmount(afterEach)` 与真实路由 mount。

#### E-F09 暗色模式亮块：拿 `--gray-*` 当深色表面

【现象】亮色模式好好的一块「深色代码块/实心按钮」，切到暗色模式变成刺眼的**亮色块**；按钮文字（白）与新背景（亮灰）对比度崩坏。

【原因】设计系统里 `--gray-*` 是**灰度梯度**，不是「表面色」。暗色模式下梯度被**整体反转**：`--gray-900` 在亮色模式是 `#111111`（近黑），暗色模式下变成 `#EBEBEB`（近白）——原本「用最深灰当深色表面」的写法，在暗色下恰好变成「用最浅灰当深色表面」。这不是 bug，是梯度的语义本就是「相对明暗」，而「永远深色」是另一种语义。

【解法】「永远深色/高对比」的表面用**语义 token**：

- 代码块：`--code-bg` / `--code-text` / `--code-border`（暗色下依然是深底浅字）；
- 高对比实心按钮：`--contrast-bg` / `--contrast-bg-hover` / `--contrast-bg-active` / `--contrast-fg`。

排查现有代码：grep `var(--gray-9`、`var(--gray-8`（高频被误用的深灰），逐个判断语义是「随主题反转的灰度」还是「恒定深色表面」，后者改成语义 token。

【预防】`frontend/CLAUDE.md` 已有此约定；评审时「新样式里出现 `--gray-9xx` 且注释说『深色底』」一律打回。改完暗色模式人工过一遍代码块与主按钮。

#### E-F10 token 过期后请求 401：刷新队列与 `failedQueue` 的正确姿势

【现象】用户停留半小时后操作，首个请求 401；随后**一连串**并发请求要么全部 401 弹登录页，要么只刷新了一次、其余请求丢失。

【原因】access token 到期后，第一个撞上 401 的请求会触发刷新流程。若不做并发控制：N 个请求同时 401 → N 次刷新调用（刷新接口可能限流 429）→ 各自拿不同新 token 重放，乱象丛生。`frontend/src/api/index.ts` 用经典方案：模块级 `isRefreshing` 标志 + `failedQueue` 数组——第一个 401 把标志置真并开始刷新，后续 401 的请求把自己的 resolve/reject 压进 `failedQueue`；刷新成功后统一用新 token 重放队列，失败则整体走 `handleAuthError()`：清理 Pinia 与 localStorage、跳回 `adminRoot`。任何绕过这套机制的手写 401 处理（比如业务代码里自己 `location.href = '/login'`）都会制造「重复跳转/状态不一致」。

【解法】排查顺序：

1. Network 面板确认：401 后**只应有一次** `/user/refresh` 调用（若出现多次，说明 `isRefreshing` 互斥失效，检查是否有代码绕过统一 axios 实例发请求）。
2. 刷新本身 401（refresh token 也过期）→ `handleAuthError` 清理并跳登录，属预期。
3. 刷新成功但业务请求没重放 → 检查 `failedQueue` 的 resolve 是否在拦截器里被正确 `await`（重放请求要返回同一个 Promise 链，不能新建）。
4. 业务代码不要自己处理 401——统一交给拦截器。

【预防】新增 API 一律走 `api/index.ts` 的实例，不自建 axios；给「并发 401 只刷新一次」写一个集成测试（两个请求同时 401，断言 refresh 只调一次且两者都成功重放）。

#### E-F11 访客标识无效：缺 `X-Visitor-Id` 头返回 400

【现象】匿名点赞/留言接口返回：

```text
{"code":400,"message":"访客标识无效"}
```

DevTools 里对比正常请求，发现失败请求少了一个请求头。

【原因】匿名互动接口要求每个访客有稳定标识，请求拦截器会给**所有**请求注入 `X-Visitor-Id`（localStorage 里持久化的随机串）。返回 400 的原因通常是：标识生成/注入逻辑被绕过（比如用了独立的 axios 实例、或测试里直接 fetch）、或值被清空/格式非法。

【解法】确认请求走的是统一 API 实例（`api/index.ts`）；检查 localStorage 里访客标识键存在且非空；测试里需要模拟访客时，在请求前向 localStorage 写入合法标识（或调初始化函数生成）。

【预防】不要新建 axios 实例（见 E-F10 预防）；`X-Visitor-Id` 与 `Authorization`、`X-XSRF-TOKEN` 一样由拦截器统一注入，业务代码不碰这三个头。

#### E-F12 Pinia 测试状态泄漏：`setActivePinia(createPinia())` 每测试隔离

【现象】单跑某个 store 测试全绿，全量跑就挂；或前一个测试改的状态（登录态、站点设置、主题）渗进后一个测试的断言。报错面目多样：token 校验失败、`adminPath` 是上个测试改过的值、购物车式计数翻倍。

【原因】Pinia 的 active 实例是**模块级单例**：`setActivePinia()` 设置一次后持续存在。jsdom 的模块注册表在同一个测试文件里共享，若只在 `beforeAll` 里建一次 Pinia，所有测试用例共享同一批 store 状态——测试 A 里 `userStore.token = 'xxx'`，测试 B 一进来就「已登录」。跨文件则因 vitest 默认每文件独立 worker 而互不影响，所以「单文件绿、全量红」多指**文件内**泄漏；而「单跑绿、全量红」也可能来自全局监听器泄漏（E-F08 第 1 条），两个原因要分清。

【解法】每个测试用例（`beforeEach` 或 `beforeEach` + 每个 test 内）都重建：

```ts
import { createPinia, setActivePinia } from 'pinia';

beforeEach(() => {
  setActivePinia(createPinia());   // 每 test 一个全新容器
});
```

组件测试里 `mount` 时传 `global: { plugins: [createPinia()] }` 同理——**每次 mount 新建**，不要复用实例。需要「带初始状态」的测试，在 createPinia 之后立即显式赋值，让前置状态成为测试声明的一部分而不是上个测试的遗产。

【预防】store 测试模板第一行就是 `beforeEach(() => setActivePinia(createPinia()))`；评审时看到 `setActivePinia` 出现在 `beforeAll`/模块顶层一律打回。

#### E-F13 `useConfirm()` 的确认框不出现：组件没渲染

【现象】代码里调了 `const { confirm } = useConfirm(); await confirm({...})`，运行时确认框从不出现，Promise 也不 resolve，流程「无声卡死」。

【原因】`useConfirm()` 返回的 `ConfirmDialog` **必须渲染到模板里**——它是「状态 + UI」的组合：`useConfirm` 只是把状态钩子给你，真正画出对话框的是 `<ConfirmDialog />` 组件。页面里忘了放这个组件，confirm 调用就只是改了没人渲染的状态，Promise 永远等待。

【解法】在用到确认交互的页面模板里加上 `<ConfirmDialog />`（从 `useConfirm()` 的返回里解构或按组件文档引入）。排查时全局搜该页面是否引入渲染了该组件。

【预防】把「用 useConfirm 必渲染 ConfirmDialog」写进组件使用示例的注释里；新页面从成熟页面（如管理端删除操作所在页）复制交互样板。

#### E-F14 找不到 user store：它叫 `'auth'`

【现象】Vue Devtools 里搜 "user" 搜不到用户 store；或测试里 `useUserStore()` 与自定义 store id 冲突报错；或新人按 store 文件名（`stores/user.ts`）猜 id 结果对不上。

【原因】`stores/user.ts` 定义 store 时 id 用的是 `'auth'`（多用户社区时代的历史遗留），导出别名 `useUserStore`。文件名与 store id 不一致，按文件名检索就会扑空。另一个相关事实：token 经 `utils/crypto` 编解码存取（不是明文 localStorage），`validateToken` 只解 JWT 不发网络请求——所以「localStorage 里没有明文 token」不代表登录态丢了。

【解法】Devtools 里搜 "auth"；代码里统一用 `useUserStore` 别名，不碰 store id。排查登录态时知道两件事：存储是编码后的、校验是本地解码。

【预防】在 `stores/user.ts` 顶部注释写明 id 历史与别名的对应关系；不要「顺手改 id」——改 id 等于换存储桶，所有已登录用户的本地态会瞬间失效。

#### E-F15 公开页面配置的「幽灵草稿」：公开 store 只该有已发布内容

【现象】后台预览草稿时，前台公共接口意外返回了未发布内容；或测试断言公开 manifest 时发现混进了草稿页。

【原因】`stores/pages.ts` 的约定是**只保存已发布页面配置**，草稿绝不进公开 store。违反通常发生在：有人为图方便把草稿数据也塞进了同一 store（或同一接口的响应未按 status 过滤），预览路径与公开路径共用了取数函数却没分流。

【解法】预览走独立的预览取数路径（后台专用接口），公开 store 的填充只接受 `status=已发布` 的数据；接口层面确认 manifest 端点按发布状态过滤。

【预防】给 pages store 写断言「所有条目均已发布」的测试；评审触碰 pages store 的 diff 时检查数据来源。

#### E-F16 路由守卫重定向循环或丢 redirect 参数

【现象】未登录访问后台被踢到登录页，登录成功后却落在默认页而不是原来想去的页面；或守卫互相踢皮球，浏览器地址栏来回跳。

【原因】守卫约定：`requiresAuth`/`requiresAdmin` 未通过时跳 `siteStore.adminRoot` 并**带 `redirect` 查询参数**记录来路；登录成功后的回跳逻辑要读这个参数。问题通常出在两处：跳转时没带 `redirect`（回跳自然丢失），或回跳时直接 `router.push(redirect)` 而没校验其合法性（开放重定向风险 + 外链会导致再次弹回）。另外 `/login` 与 `views/auth/` 下的页面**只剩重定向职责**（→ `adminRoot`），组件内需要登录时统一 `router.push('/login')`，写死其他路径会绕过 `adminRoot` 的动态性（`adminPath` 可配置，见 E-F06）。

【解法】守卫跳转统一走 `router.push({ path: siteStore.adminRoot, query: { redirect: to.fullPath } })`；回跳时校验 `redirect` 必须以 `/` 开头（防开放重定向）再 push。排查循环：在守卫入口 `console.log(to.fullPath, from.fullPath)` 跑一次复现，看两个路径是否互为跳转源。

【预防】登录后回跳的合法性与「redirect 必须带回」都写进守卫的单测；组件内登录跳转只允许 `router.push('/login')` 这一种字面量。

#### E-F17 GFM/Markdown 渲染差异：预览与线上不一致

【现象】后台 Markdown 预览里表格/代码块渲染正常，线上文章页却走样（或反之）；或 XSS 担忧——担心 `marked` 直接插 HTML。

【原因】渲染链是 `marked`（Markdown → HTML）+ `DOMPurify`（消毒）。两边不一致通常因为：预览与文章页调用的是不同封装（一个过了 DOMPurify 一个没过）、或版本/配置（GFM 扩展开关）不同；直接插 HTML 的风险由 DOMPurify 兜底，但「自己 new 一个 marked 实例改了配置」就会绕开统一行为。

【解法】渲染只走统一封装（搜 `marked(` 的调用点，确认都在同一个工具模块里）；预览与文章页必须用同一个渲染函数；改 GFM 配置时两处同步。

【预防】把「Markdown 渲染唯一入口」作为评审规则；DOMPurify 的白名单变更需同步检查文章页与评论两个消费方。

#### E-F18 公开页面 SSG/预渲染与 SPA 回退：为什么有时候页面是 HTML 有时候要等 JS

【现象】同一篇文章，搜索引擎抓到的快照文字齐全（能看到正文），自己用浏览器打开却先白屏后出内容；或用 curl 打开文章页得到完整 HTML，改动前端组件后 curl 却看不到变化。

【原因】这不是玄学，是**两套渲染路径并存**：公开历史路由（`/`、`/about`、`/archives`、`/gallery`、`/post/{id}`、`/tag/{id}`、`/pages/{slug}`）由后端 `service/site/seo/` 组装**可直接阅读的 HTML**（状态码真实，缺失 404；curl 与爬虫拿到的是它）；浏览器拿到这份 HTML 后 Vue 挂载，`#app` 内的 `.seo-reading` 被前端渲染结果**替换**。所以「curl 看到的」是 SEO 版（不含你的新组件改动），「浏览器看到的」是 Vue 版。后台命名空间与历史账户路由则返回启动壳并强制 noindex。

【解法】验证 SEO HTML 用 curl（`curl -s http://localhost:8825/post/990001 | Select-String seo-reading`）；验证交互用浏览器。改了组件但 curl 结果没变——正常，SEO 模板在后端；改了 SEO 组装逻辑但浏览器没变——也可能正常，浏览器已被 Vue 接管。Docker 冒烟 job 里的 `grep seo-reading` 正是在验证前一条路径活着。

【预防】两套渲染的职责边界写进组件文档：凡是「必须被搜索引擎看到」的内容进 SEO 组装，凡是「必须可交互」的进 Vue；不要在 `.seo-reading` 里埋依赖 JS 的交互。

#### E-F19 前端测试里 `$router`/`$route` 未定义：setup 只桩了部分全局

【现象】单测里组件用了 `$router.push` 或 `$route.params`，vitest 报 `Cannot read properties of undefined (reading 'push')`；或测试通过但警告路由未注入。

【原因】`src/test/setup.ts` 只全局桩了 `matchMedia` 与 `$router/$route` 两个挂载全局。它覆盖「组件读路由参数/跳转」的最小需求，但**不是完整路由环境**：依赖 `<RouterLink>` 真实行为（active 态、slot）、依赖导航守卫、依赖 `router.currentRoute` 响应性的组件，全局桩满足不了——桩的 `$router.push` 是个空函数，导航从未发生。

【解法】轻依赖组件用全局桩即可；重路由依赖组件按 E-F08 第 4 条上真实路由（`createMemoryHistory()` + `createRouter`），mount 时通过 `global.plugins` 注入。判断标准：断言里需要「导航后」的状态，就必须真路由。

【预防】测试模板注释写明全局桩的覆盖范围；新组件测试先问「断言是否依赖真实导航结果」再选桩法。

#### E-F20 Vite 环境变量「改了不生效」：import.meta.env 的静态替换与重启要求

【现象】在 `.env.local` 里改了 `VITE_API_TARGET`（或其他 `VITE_` 变量），dev server 不重启时行为不变；或代码里 `import.meta.env.X` 在运行时是 undefined，但变量明明写了。

【原因】两族原因。其一：Vite 的环境变量在启动时读入并**静态替换**进产物，`import.meta.env.VITE_*` 不是运行时查找——改 `.env*` 必须重启 dev server（Ctrl+C 后重新 `./node_modules/.bin/vite`）。其二：只有 `VITE_` 前缀的变量才会暴露给客户端代码，写了个 `API_TARGET`（无前缀）在 `import.meta.env` 里就是不存在的；TypeScript 侧还需在 `env.d.ts`（或等价声明文件）里给 `ImportMetaEnv` 补类型，否则 strict 下读不到自定义键。

【解法】改 `.env*` 必重启；确认变量名带 `VITE_` 前缀；自定义键补 `ImportMetaEnv` 接口声明。特别提醒（呼应 E-F05）：本项目 `VITE_API_BASE_URL` 即使加了前缀也**没有任何代码读取**——baseURL 硬编码 `/api`，真正可调的旋钮是 `vite.config.ts` 里 `server.proxy` 用的 `VITE_API_TARGET`，它影响的是**代理目标**而非 `import.meta.env`。

【预防】给前端用到的环境变量维护一张「名字 | 前缀 | 谁读它 | 何时生效」表，新人上手先看表再配变量；`env.d.ts` 与实际使用同步评审。

#### E-F21 组件样式「写了没效果」：scoped、token 引用与皮肤命名空间三重过滤

【现象】给组件加了样式规则却不生效；或局部覆盖了某 token 值，页面上毫无变化；或同样的 CSS 在自己页面生效、在皮肤（如 `ocean`）下失效。

【原因】三重过滤器。① `<style scoped>` 给选择器附加 data 属性哈希，**子组件根元素之外的内部元素**不受父级 scoped 规则影响（要用 `:deep()`）；② 颜色/圆角若写死字面量而页面某处有更高优先级规则，或你以为改的是 token 实际改的是注释里的示例，自然无效——token 定义在 `variables.css`，暗色覆盖在 `dark-mode.css`，皮肤在 `skins.css`，改错文件等于没改；③ 皮肤选择器带 `#selfblog[data-skin=…]` 命名空间，优先级高于你的裸类选择器。

【解法】DevTools 的 Styles 面板看规则是否命中、被谁覆盖（划线规则旁标着来源文件）；scoped 穿透用 `:deep()`；改 token 去对文件（全局默认 → variables.css，暗色 → dark-mode.css，皮肤差异 → skins.css）；与皮肤选择器对抗时提高自己选择器特异性或干脆用语义 token（E-F09）从源头避免。

【预防】遵守 frontend 设计约定：圆角用 `--radius-*`，颜色用语义 token，不写裸值；评审样式 diff 时检查「改的文件」与「想改的东西」是否同层。

---

## A.3 环境与工具链排查

本节收录「不属于后端/前端代码，但能把人卡一下午」的环境问题。编号 E-E01 起。

### E-E01 `JAVA_HOME` 指向 JDK 25：项目明确不支持

【现象】`mvnw test` 报编译错或运行错，`mvnw -version` 显示 Java version 25；或运行期出现 JDK 25 才有的模块化限制报错（如 `InaccessibleObjectException`）。

【原因】本机系统 `JAVA_HOME` 装的是 JDK 25，而 SelfBlog 明确只支持 **JDK 21**（`pom.xml` 的 release=21，测试基线在 21 上验证）。JDK 25 编译 release 21 理论可行，但项目验证过的只有 21——新 JDK 的行为差异（更强的模块封装、默认值变化）随时可能制造「只在 25 上出现」的灵异问题。这与 A.1 的 E-B01 是同族问题：E-B01 是「太低」（17），本条是「太高」（25），解法一致。

【解法】会话级切换（推荐，不影响全局）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
backend\mvnw.cmd -version   # 确认 Java version: 21.x
```

永久方案是改系统环境变量；多 JDK 共存时建议不设全局 `JAVA_HOME`，让每个项目脚本自己设。

【预防】仓库根 `CLAUDE.md` 与 README 都写了「本机 JAVA_HOME 指向 JDK 25，须手动指到 21」的现状说明——把它当作机器的固有属性，写进个人启动脚本。

### E-E02 PowerShell 里 `-D` 参数必须加引号：`-Dskip.npm=true` 被吃掉

【现象】PowerShell 里执行 `backend\mvnw.cmd test -Dskip.npm=true`，Maven 却照跑 frontend-maven-plugin（`npm ci` + 前端构建被触发，frontend 产物被改写）；或报「未知选项」。

【原因】PowerShell 的参数解析与 cmd/bash 不同：`-Dskip.npm=true` 中的 `.` 与 `=` 会让 PowerShell 尝试把它解析为自己的参数语法，传给 mvnw.cmd 时被拆散或丢弃。带引号 `"-Dskip.npm=true"` 强制按字面量传递，Maven 才能收到完整属性。这是 PowerShell 的坑，bash 里不需要。

【解法】PowerShell 下永远加引号：

```powershell
backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

两个开关必须成对出现：只跳过 npm 构建不跳过 node 安装（或反之）都会让 frontend-maven-plugin 在错误的阶段插手。

【预防】把带引号的完整命令存成 `test-backend.ps1` 脚本或 PowerShell 别名；见 A.0 通用命令块。

### E-E03 中文乱码：GBK 与 UTF-8 的世纪之争

【现象】三族面目：其一，PowerShell 里 `Get-Content some.log` 中文显示乱码（或 `?.` 变 `锟斤拷`）；其二，后端日志/响应中文正常，但写出的 `.csv` 用 Excel 打开乱码；其三，`.env` 里的中文值（`ADMIN_NICKNAME=站长`）读进来是乱码。

【原因】Windows 中文环境的传统代码页是 GBK（cp936），而现代工具链（Java、Node、Git、本项目所有文件）一律 UTF-8。乱码 = 「按 A 编码写入、按 B 编码读取」。PowerShell 5.1 的 `Get-Content` 默认按系统 ANSI（GBK）读无 BOM 文件——UTF-8 写的文件自然读花。Excel 打开无 BOM 的 UTF-8 CSV 也会按 GBK 猜。

【解法】

1. PowerShell 5.1 读文件显式 `Get-Content -Encoding UTF8`；PowerShell 7+ 默认 UTF-8，可升级或用 `pwsh`。
2. 写 CSV 给 Excel 用：文件头加 BOM（`\ufeff`），或存成 UTF-8 with BOM 编码。
3. `.env` 中文值：`DotenvConfig` 用 UTF-8 读取（见 E-B04），只要**文件本身存为 UTF-8 无 BOM** 就不会乱码；用记事本另存时选「UTF-8」（不是「UTF-8 with BOM」）。
4. 终端输出乱码：`chcp 65001` 切换代码页，或换 Windows Terminal + PowerShell 7。

【预防】团队约定「所有文本文件一律 UTF-8 无 BOM」；`editorconfig` 里 `charset = utf-8`；给 Excel 用的导出单独加 BOM。

### E-E04 构建产物 hash 变化与旧缓存：部署后页面没更新

【现象】部署了新版本，用户浏览器里还是旧页面；或部分用户新、部分用户旧。强制刷新（Ctrl+F5）后正常。

【原因】Vite 构建给资源文件名加内容 hash（`index-B3xK9.js`），index.html 引用带 hash 的资源名。CDN/浏览器对 **index.html 的缓存策略**若过长，浏览器拿旧 html → 引用旧 hash 资源 → CDN 上旧资源已被清 → 404 或命中旧缓存。资源本身（带 hash 的文件）是「内容寻址」，可以长缓存；**index.html 必须短缓存/不缓存**。

【解法】部署层配置：index.html 响应头 `Cache-Control: no-cache`（或 `max-age=0, must-revalidate`）；带 hash 的静态资源 `Cache-Control: public, max-age=31536000, immutable`。排查用户侧问题：让用户 Ctrl+F5 / 无痕窗口确认是否缓存问题，再看 CDN 控制台的缓存刷新。

【预防】把缓存策略写进部署清单；SelfBlog 的 vercel.json 反代模式下注意后端返回的 SPA 壳 HTML 同样要 no-cache。

### E-E05 CI 与本机行为不一致：先 diff 环境再怀疑代码

【现象】本机测试全绿，CI 挂了（或反之）。报错五花八门：数据库方言、Node 版本、时区、文件路径大小写。

【原因】CI 四个 job 的环境与本机的本质差异：

- backend job：H2 内存库（本机可能残留 `DB_TYPE=mysql`，见 E-B05b）；
- mysql job：**真实 MySQL 8** + `schema-fresh-install.sql`（本机没有 MySQL——这是全仓唯一真 MySQL 验证）；
- frontend job：Node 24.15.0 + 全闸门（本机 Node 版本可能不同，见 E-F04）；
- docker job：SQLite 容器 + `/actuator/health` 探活 + grep 服务端 HTML 里的 `seo-reading`（本机无 Docker，镜像构建从未在本机验证过）。

「CI 挂本机绿」九成是环境差异，一成是**测试间状态泄漏**（本机串行跑碰巧掩盖了）。

【解法】按 job 对号入座：backend 挂 → 检查是否 H2 不支持的语法（E-B11）；mysql 挂 → 大概率方言问题被本机 H2 掩盖，修复思路是「SQL 三库兼容」；frontend 挂 → Node 版本、`npm ci` 的 lock 一致性；docker 挂 → 镜像构建或健康检查超时，查 `seo-reading` 是否真的出现在 SSR HTML 里。

【预防】重要改动提交前先在本地复现 CI 的关键条件（H2 全量测试 + Node 24 前端闸门）；把 CI 当作「唯一可信环境」，本机只做快速迭代。

### A.3.1 补充环境条目

#### E-E06 Docker/Compose 首次构建失败的排查顺序

【现象】`docker compose -f backend/docker-compose.yml up -d --build` 失败，或容器起来但 `/actuator/health` 一直不通。

【原因】仓库的 Docker 构建上下文是**仓库根**（不是 backend/），镜像内置真实 `selfblog.jar` 与数据/上传/相册卷；本机没有 Docker 守护进程，镜像从未在开发机上验证过——所以本条没有「实测答案」，只有系统化排查顺序。健康检查不通的高频原因：数据库初始化失败（compose 挂载的 `schema-fresh-install.sql` 含 DROP TABLE，只能用于全新空库——对已有卷重复挂载会因建表冲突失败）、环境变量缺失（`JWT_SECRET` 不足 32 位会直接启动失败，E-B03）。

【解法】按序检查：① 构建日志里 jar 是否真的打进镜像（上下文路径错误时 COPY 会失败或拿到旧 jar）；② `docker compose logs backend` 看启动日志有没有 `[Dotenv]` 告警或 `IllegalStateException`（对应 E-B03/E-B04）；③ 数据卷是否为空库（首次部署用全新卷 + `schema-fresh-install.sql`，已有数据只能走 `migration-selfblog.sql`）；④ 健康检查探活给足启动时间再判失败。

【预防】把「Docker 本机未验证」当作已知风险（AGENTS.md 已声明），部署到真实服务器前预留一次完整的冒烟窗口，CI 的 docker job（SQLite + health + `seo-reading` grep）是你的行为基准。

#### E-E07 Node 版本多项目冲突：给 SelfBlog 固定一个 Node

【现象】同一台机器开发多个前端项目，A 项目要 Node 20、B 项目（SelfBlog）要 Node 24+，来回切换后 `npm ci` 报 EENGINE（E-F04）或构建产物异常。

【原因】全局唯一的 Node 安装无法同时满足多个项目的 engines 声明；SelfBlog 前端经 `engine-strict=true` 硬校验。仓库的兜底来源是 frontend-maven-plugin 下载到 `backend/target/frontend/node` 的版本（与 CI 对齐），但它随 `mvn clean` 消失，不适合当长期依赖。

【解法】装一个 Node 版本管理器（fnm/nvm-windows/volta），per-project 固定版本：fnm/nvm 用项目根的 `.node-version`（若仓库没有就自建，写 `24.15.0`）；volta 直接在 package.json 里钉 `node` 字段。临时方案仍是把 `backend/target/frontend/node` 前置 PATH（见 E-F01）。

【预防】新机器初始化清单：JDK 21（E-E01）→ fnm + Node 24.15.0 → 跑通 `npm ci` 与 `mvnw test`（带双 skip 参数）再开工。

#### E-E08 Git 环境差异：行尾 CRLF 与 `format:check` 互搏

【现象】Windows 上一切正常，CI 的 `npm run format:check`（Prettier）报大面积行尾差异；或反之，本地 format 后 diff 出成片「无意义改动」。

【原因】Windows 的 Git 默认 `core.autocrlf=true`（checkout 时 LF→CRLF），而 Prettier 的 `endOfLine` 配置决定期望值。SelfBlog 的 `.prettierrc` 已改为 `endOfLine: "auto"`（2026-10-17 起为真实可用的仓库级闸门），`auto` 会沿用文件首行现有行尾风格，理论上一致；历史漂移已被一次性重写修复。剩下的冲突来自**个人 Git 配置**与仓库假设不一致（比如全局 autocrlf=input 或关掉了）。

【解法】保持仓库 `.gitattributes`（若有）与全局默认即可；出问题先 `git config core.autocrlf` 看当前值，格式化冲突时跑一次 `npm run format:check` 确认是「真差异」还是「行尾换算差异」，后者重 checkout（`git rm --cached -r . && git reset --hard`）即愈。

【预防】不动 `.prettierrc` 的 endOfLine；新成员克隆后先跑五闸门再开工，把环境差异当场暴露。

#### E-E09 Maven 本地仓库损坏：`.m2` 缓存的不完整下载

【现象】依赖下载中断后再构建，报 jar 文件无法读取/校验失败，或出现 `Failed to read artifact descriptor`，且 `mvnw clean` 无效——错误跨构建持久。

【原因】Maven 把依赖缓存在用户目录 `.m2\repository`。网络中断或进程被杀会留下**半截文件**（或 `.lastUpdated` 标记），Maven 默认认为「缓存存在」就不重新下载，坏缓存反复被使用。这与代码无关、与网络当时状态也无关——是磁盘上的坏块。

【解法】删除报错依赖对应的目录（按报错里的 groupId 路径定位，如 `.m2\repository\com\selfblog\...` 或 `org\springframework\...`），重建时 Maven 会重新下载；大面积损坏可整仓备份后清空 `.m2\repository`。注意删除操作先确认路径（`Get-ChildItem ~/.m2/repository/<路径>` 看清再删）。

【预防】下载依赖时不要强行中断构建；公司网络/代理环境下首次 clone 后先跑一次纯依赖解析（`mvnw dependency:go-offline`）再开工。

#### E-E10 时间与证书：系统时钟漂移导致的批量神秘失败

【现象】批量出现 TLS/证书相关报错（`certificate has expired`、`unable to verify the first certificate`），npm/Maven/浏览器全面失败；或 JWT 行为诡异。查看系统时间发现不对。

【原因】TLS 证书有效期依赖系统时钟。Windows 主板电池耗尽、虚拟机挂起恢复、双系统时间冲突都会让时钟漂移——证书「还没生效」或「已过期」的判定随之全错。npm registry、Maven Central 的 HTTPS 握手直接失败，表现为与网络故障 identical 的报错，但 ping 得通。

【解法】先核对系统时间（`Get-Date`）与 `time.windows.com` 同步；虚拟机场景检查挂起恢复后的时间补偿。时钟修正后重试，多数「网络故障」自愈。

【预防】VM/远程开发环境把 NTP 同步纳入初始化；排错第一步顺手看一眼系统时间——它是最便宜的一块多米诺骨牌。

---

### A.3.11 数据「删不掉」：逻辑删除后查询还在

**现象**：删除操作返回成功，列表里记录还在。

**原因**：删的是另一行（id 传错）、或列表查询是手写 SQL 没过滤、或缓存未清（A.2.13）。

**解法**：直查库看 `is_deleted` 值定位是删没删还是查没滤。

**预防**：删除接口返回受影响行数（`deleteById == 0 → 404`，GuestCommentServiceImpl 的范式）；列表一律走 MP 自带方法或带 is_deleted 条件的 XML。

### A.3.12 `site_setting` 改了但前台没变化

**现象**：后台改站点设置，前台仍旧值。

**原因**：`siteSettingsCache`（10 分钟 TTL）未失效，或前端 `stores/site.ts` 只在启动时拉一次。

**解法**：确认写操作清了缓存；前台设置类展示要接受「最长 10 分钟 + 刷新页面」的延迟，或改完设置后主动调用加载函数。

**预防**：设置类数据的新鲜度预期要在产品层面定义清楚（SelfBlog：站点设置低频变更，10 分钟可接受）。

---

## A.4 错误码速查总表

> 按关键字检索：先 Ctrl+F 搜报错原文里的关键词，命中后按「详见」列跳小节。表内小节号即本附录编号。

| 错误关键字 / 报错特征 | 一句话原因 | 详见 |
|---|---|---|
| `release version 21 not supported` | Maven 实际用的 JDK 低于 21（JAVA_HOME 指错） | A.1.1 E-B01 |
| `BeanCurrentlyInCreationException` / `Is there an unresolvable circular reference?` | 构造器注入循环依赖，三级缓存救不了 | A.1.2 E-B02 |
| `NoSuchBeanDefinitionException: No qualifying bean of type 'JwtUtils'` | `@WebMvcTest` 切片没 Mock JWT 工具 Bean | A.1.3 E-B06 |
| `expected 200 but was 401`（addFilters=false 时） | 关过滤器链≠绕过鉴权，别用这个开关 | A.1.3 E-B07 |
| `WeakKeyException` / `JWT密钥长度必须至少32个字符` | JWT_SECRET 不足 32 字节（UTF-8 计） | A.1.2 E-B03 |
| `[Dotenv] 未找到 .env 文件` | .env 不在工作目录/主目录 | A.1.2 E-B04 |
| `.env` 值带引号 / 文件值压过系统变量 | Properties 解析不剥引号；systemProperties 优先 | A.1.2 E-B04 |
| `Port 8825 was already in use` | 端口被残留进程占用 | A.1.2 E-B05 |
| 跑测试却报 MySQL 驱动错 | 残留 `DB_TYPE` 顶掉 H2 测试数据源 | A.1.2 E-B05b |
| 测试 404：`get("/api/...")` 不命中 | 切片路径不带 /api 前缀（真实请求要带） | A.1.3 E-B09 |
| 列表出现已删除数据 / `deleteById` 执行 UPDATE | 软删除：手写 SQL 忘 `is_deleted=0` | A.1.3 E-B10 |
| H2 报 MySQL 语法错（`ON DUPLICATE KEY` 等） | MySQL 专有 SQL 写进了无 databaseId 的共享语句 | A.1.3 E-B11 |
| `@Transactional` 不回滚（六场景） | 自调用/非 public/异常吞/缺 rollbackFor/传播/多线程 | A.1.3 E-B12 |
| `@Async` 方法同步执行 | 同类调用绕过 AOP 代理 | A.1.3 E-B13 |
| `ExpiredJwtException` | access token 到期，属正常刷新流程 | A.1.3 E-B14 |
| `SignatureException: JWT signature does not match` | 签发与校验密钥不一致（常因 .env 引号） | A.1.3 E-B14 |
| `Refresh token cannot be used for API access` | refresh token 被当 access token 用 | A.1.3 E-B14 |
| `blocked by CORS policy` | 跨域直连后端；应走同源代理 | A.1.3 E-B15 |
| `Too many requests`（429） | 限流前缀命中或新前缀未配阈值 | A.1.3 E-B16 |
| Bean Validation 不生效（空值入库） | 缺 validation starter / 嵌套未标 @Valid | A.1.3 E-B17 |
| 分页 total 等于页大小 / 无 LIMIT | 分页插件未装配 | A.1.3 E-B18 |
| ID 末位变 0 / 文章详情 404 | 雪花 ID 超 JS 安全整数，须 Long→String | A.1.3 E-B19 |
| `res.data.data` 是 undefined | 拦截器已返回信封，业务数据在 `res.data` | A.1.3 E-B20 |
| 前后端时间差 8 小时 | 时区/序列化层不一致 | A.1.3 E-B21 |
| `[SQLITE_BUSY] database file is locked` | SQLite 单写锁/池外连接持锁 | A.1.3 E-B22 |
| `'"node"' 不是内部或外部命令` | PATH 有不配对引号，npm run 全灭 | A.2.1 E-F01 |
| `error TS2532: Object is possibly 'undefined'` | noUncheckedIndexedAccess：索引访问可空 | A.2.1 E-F02 |
| `error TS2339: Property 'records' does not exist` | API 信封类型传错（读 res.data 而非 res.data.data） | A.2.1 E-F02 |
| `error TS2307: Cannot find module '@/...'` | 路径别名/声明缺失 | A.2.1 E-F02 |
| vue-tsc 跑两遍 / build 慢 | build 脚本内含 type-check，属刻意闸门 | A.2.1 E-F03 |
| `npm ERR! code EENGINE` / `Unsupported engine` | engine-strict 硬校验 Node 版本 | A.2.1 E-F04 |
| dev 页面 API 404 / 改 BASE_URL 无效 | `VITE_API_BASE_URL` 无人读取，看 vite.config 的 VITE_API_TARGET | A.2.2 E-F05 |
| 登录后访问后台 404 | installAdminRoutes 未在首导航前执行 | A.2.2 E-F06 |
| 已登录被弹回登录页 | restoreFromStorage 必须在 use(router) 之前 | A.2.2 E-F07 |
| 组件测试副作用跨用例存活 | setup 无自动卸载，须 enableAutoUnmount | A.2.3 E-F08 |
| rAF 回调不执行 | 不能桩成同步，要排队手动 flush | A.2.3 E-F08 |
| `Cannot assign to read only property 'scrollY'` | 只读 getter，须 defineProperty + dispatchEvent | A.2.3 E-F08 |
| 测试里链接文字丢失/无 active 态 | RouterLink stub 丢 slot，改用真实 memory 路由 | A.2.3 E-F08 |
| `window.matchMedia is not a function` | jsdom 未实现，需桩（setup.ts 已桩） | A.2.3 E-F08 |
| 暗色模式按钮/代码块变亮块 | --gray-* 暗色下反转，深色表面用语义 token | A.2.3 E-F09 |
| 401 后重复刷新/请求丢失 | 并发 401 须走 isRefreshing+failedQueue 队列 | A.2.3 E-F10 |
| `{"code":400,"message":"访客标识无效"}` | 请求未带 X-Visitor-Id（绕过了统一拦截器） | A.2.3 E-F11 |
| `mvnw` 报 JDK 25 相关错 | JAVA_HOME 须指向 JDK 21 | A.3 E-E01 |
| `-Dskip.npm=true` 不生效 | PowerShell 下 -D 参数必须加引号 | A.3 E-E02 |
| PowerShell 读文件中文乱码 | GBK vs UTF-8，显式 -Encoding UTF8 | A.3 E-E03 |
| 部署后页面不更新 | index.html 缓存策略过长，应 no-cache | A.3 E-E04 |
| 本机绿 CI 红 | 环境差异（H2/MySQL/Node 版本/Docker） | A.3 E-E05 |
| `/doc.html` 404 或空白 | prod profile 关闭文档页；或安全链未放行 | A.1.4 E-B23 |
| `[PERF] ... error:` 刷屏 | 正常负例记录，看 Tests run 计数行 | A.1.4 E-B24 |
| 改了数据前台不变 | Caffeine 缓存 TTL 内读旧值（8 个具名缓存） | A.1.4 E-B25 |
| 日志成组 `JWT_AUTH_FAILED` | 安全事件：前端风暴或外部试探 | A.1.4 E-B26 |
| 业务失败 HTTP 却是 200 | Controller 自组错误体绕过 GlobalExceptionHandler | A.1.4 E-B27 |
| 一切异常变 500 | 异常类型无对应 @ExceptionHandler，落兜底 | A.1.4 E-B27 |
| 游客接口报 401 未登录 | 误用 getCurrentUserId()，游客场景用 OrNull | A.1.4 E-B28 |
| `/uploads` 图片 404 | 资源映射路径/代理转发/卷挂载三断点 | A.1.4 E-B29 |
| 注册接口 403「不开放访客注册」 | 单人博客设计使然，非故障 | A.1.4 E-B30 |
| 单跑绿全量红（store 状态） | Pinia 未每 test 重建 | A.2.3 E-F12 |
| 确认框不出现流程卡死 | ConfirmDialog 未渲染进模板 | A.2.3 E-F13 |
| Devtools 搜不到 user store | store id 叫 'auth' | A.2.3 E-F14 |
| 公开接口漏出草稿 | pages store 只存已发布内容 | A.2.3 E-F15 |
| 登录后没回到原页面 | 守卫丢 redirect 参数 / 回跳未校验 | A.2.3 E-F16 |
| 预览与线上 Markdown 渲染不一致 | 未走 marked+DOMPurify 唯一入口 | A.2.3 E-F17 |
| curl 与浏览器看到的页面不同 | SEO 静态 HTML 与 Vue 渲染双路径并存 | A.2.3 E-F18 |
| 测试报 $router undefined | 全局桩不覆盖真实导航，须 memory 路由 | A.2.3 E-F19 |
| 环境变量改了不生效 | Vite 静态替换需重启；须 VITE_ 前缀 | A.2.3 E-F20 |
| 样式写了没效果 | scoped 穿透/token 文件层/皮肤命名空间 | A.2.3 E-F21 |
| 容器 health 一直不通 | schema 用于新库/JWT_SECRET/卷挂载 | A.3.1 E-E06 |
| 多项目 Node 版本互踩 | 用 fnm/volta 按项目固定 24.15.0 | A.3.1 E-E07 |
| CI format:check 大面积行尾差异 | autocrlf 与 endOfLine:auto 互搏 | A.3.1 E-E08 |
| `Failed to read artifact descriptor` | .m2 半截缓存，删对应目录重下 | A.3.1 E-E09 |
| 批量 TLS/证书报错 | 系统时钟漂移，先校时再排查 | A.3.1 E-E10 |

---

### A.4.1 速查表的正确用法

这张表按「错误关键字 → 一句话原因 → 详见小节」组织，服务于两个场景：**按报错查**（你已经看到了具体报错，扫表定位小节）与**按症状查**（没有报错，只有「不对劲」，从症状列反向找）。使用时三个提醒：报错原文可能被包装过，优先搜**最内层 Caused by** 或浏览器 Console 第一行的原文片段；同一表象可能对应多条错误（比如 404 横跨 E-B08/E-B09/E-E05/E-F05），按「最近改过什么」选最可疑的一条先查；查到小节后**务必读完【原因】段再动手**——只复制【解法】的命令而不知道机制，换一个面目就会再踩一次。

## A.5 综合排查实录：六个完整案例

速查表解决「已知报错查原因」，本节解决更难的情况——**报错只是表象，真实病因藏在三层之外**。以下六个案例全部取材于 SelfBlog 开发过程中的真实排障经历，每个案例完整走一遍 A.0 的方法论。建议初学者按顺序通读，体会「线索如何一步步收敛」。

### 实录一：新接口联调 404，文档与代码都「没错」

**背景**：作者新增了一个公开接口「获取相册封面列表」，Controller 写了 `@GetMapping("/album/covers")`，Knife4j 文档里能看到，curl 直打后端也通；但前端页面请求时 Network 面板显示 404。

**排查过程**：第一步读报错——Network 里 404 的请求 URL 是 `http://localhost:3000/album/covers`，响应体是 Vite dev server 返回的 SPA index.html（这是 SPA 服务端回退：找不到的资源返回前端壳）。第二步定位：URL 里**没有 `/api`**！对照前端取数代码，发现是同事新写的组件里没有走统一的 `api` 实例，而是手写了 `axios.get('/album/covers')`——绕过了 baseURL 硬编码的 `/api`。第三步最小复现：把请求改成走统一实例后 404 变成了 200。第四步收尾：又检查了后端——`AlbumController` 是 `@RestController`，源码映射 `/album/covers` 经 `WebMvcConfig` 自动加前缀后对外是 `/api/album/covers`（E-B09），而代理把 `/api` 转发到 8825（E-F05），链路闭合。

**根因**：绕过统一 API 实例的「顺手请求」丢失了 `/api` 前缀；SPA 回退把 404 伪装成了「页面正常返回」，让 Network 状态码成了唯一线索。

**修复与收获**：删掉手写 axios，改走统一实例；收获两条守则——「前端永远只有一个 axios 出口」（与 E-F10/E-F11 的预防呼应），以及「SPA 环境里 404 要看响应体是 JSON 还是 HTML，HTML 说明请求根本没到后端」。

### 实录二：登录半小时后被弹回登录页，刷新「风暴」拖垮了后端

**背景**：用户反馈写文章写到一半（约 30 分钟后）被弹回登录页，草稿丢失；同时后端日志出现成串 `JWT_AUTH_FAILED` 与 429。

**排查过程**：先看后端日志——`JWT_AUTH_FAILED` 集中爆发，时间间隔只有几十毫秒，说明**前端在并发重试**（不是外部扫描）。再开 DevTools 复现：把 token 有效期临时调短，等它过期后同时触发三个请求——Network 显示 `/user/refresh` 被**调用了三次**。查 `api/index.ts`，`isRefreshing` + `failedQueue` 机制存在，但业务侧有一个组件为了「提前取数」自己 `import axios from 'axios'` 新建了实例——独立实例没有刷新队列，401 后直接裸奔重试，与统一实例的刷新流程互相踩踏（E-F10 预防条款的 violation）。同时后台写接口频繁 401 撞上限流前缀阈值（E-B16），429 又触发前端错误重试，雪上加霜。

**根因**：双 axios 实例破坏了「全局只刷新一次」的互斥；草稿自动保存的高频请求放大了风暴。

**修复与收获**：统一实例 + 自动保存改用防抖与失败静默重试；收获是把「唯一 axios 出口」从约定升级为 lint 级约束（禁止直接 import axios 使用裸实例的 grep 检查），并给刷新接口确认了独立于普通前缀的限流预算。

### 实录三：CI 的 mysql job 突然红了，本机 H2 却全绿

**背景**：一次「很安全」的 PR——只加了一条手写统计 SQL——本机 588 测试全过，CI 的 mysql job 却在运行某测试时炸出 `JdbcSQLSyntaxErrorException` 风格的语法错（MySQL 方言报错）。

**排查过程**：读 CI 日志锁定失败的 statement id；打开 Mapper XML，发现新 SQL 写在**无 `databaseId` 的共享语句**里，用了 `DATE_FORMAT()` 与反引号——MySQL 专有构造（E-B11）。本机为什么绿？测试全量跑在 H2 上，而这条语句恰好在 H2 兜底路径上**没有被任何测试触达**（功能测试只覆盖了 service 层 mock，没有真正执行 XML）。这正是 E-E05 说的「CI 是唯一真 MySQL 验证」。顺带自查：同文件里还有一条手写查询漏了 `is_deleted=0`（E-B10），CI 没炸但属于侥幸。

**根因**：方言专有 SQL 写进共享语句 + XML 分支缺乏测试触达，两个隐患叠加，只有真库 job 能暴露。

**修复与收获**：SQL 改写为三库兼容写法（`CONVERT`/标准函数），补一条真执行 XML 的 H2 集成测试兜底；收获是新增手写 SQL 的 checklist 增加两项——「H2 能不能跑」与「有没有测试真的执行它」。

### 实录四：`npm run` 全灭的一个下午

**背景**：换新电脑后克隆仓库，`npm ci` 成功，但 `npm run dev` 报 `'"node"' 不是内部或外部命令`；`where node` 正常，`npm -v` 正常（E-F01 的完整病征）。

**排查过程**：先按直觉重装 Node——无效。再读报错：引号包着 `'node'` 说明 cmd 把「带引号的 node」当成了命令名，这是 PATH 解析被污染的典型特征。`echo %PATH%` 逐行检查，发现末尾一条 `C:\Program Files\Java\jdk-25\bin"` 带着不配对引号——它之后的所有 PATH 条目在 cmd 眼里全部失效（E-F01 根因）。`where node` 能找到是因为 App Paths 机制不走 PATH。用 `./node_modules/.bin/vite` 直调垫片立刻可用，证实诊断。顺藤摸瓜还发现这条坏 PATH 来自某 JDK 安装器的历史残留；同时注意到 PowerShell 里跑 Maven 时 `-Dskip.npm=true` 也因同类解析问题需要引号（E-E02）——两个「引号坑」在同一天出现。

**根因**：PATH 单条目引号不配对，cmd 解析中断；与仓库代码无关。

**修复与收获**：删掉坏条目根治；同时把「`./node_modules/.bin/` 直调五连」记为机器的应急方案。收获是理解了 Windows 上 `where`、App Paths 与 PATH 三套查找机制的区别——「where 找得到」不等于「cmd 跑得动」。

### 实录五：测试单跑全绿，全量跑红

**背景**：给后台页面写了三个组件测试，单独跑全过；全量 `test:run` 时第二个、第三个必挂，报错是路由被意外推送与「Cannot read properties of undefined」。

**排查过程**：先怀疑 Pinia 状态泄漏（E-F12），加了 `beforeEach(() => setActivePinia(createPinia()))`——仍然挂，说明还有别的状态源。再对照 E-F08 的五陷阱清单逐条排查：组件在 `onMounted` 里注册了 `window` 滚动监听与 rAF 循环，而 `setup.ts` 没有自动卸载——第一个测试的监听器活着，滚动事件让第一个组件实例继续改 store，污染后续断言；rAF 被某个测试桩成了同步执行，时序错乱。补上 `enableAutoUnmount(afterEach)`、rAF 改「入队 + 手动 flush + afterEach 清队」后全量通过。最后顺手把滚动模拟改成 `Object.defineProperty + dispatchEvent`（E-F08 第 3 条），消除只读赋值报错。

**根因**：全局监听器跨用例存活 + rAF 桩方式错误，两个独立泄漏叠加；Pinia 重建只解决了一半。

**修复与收获**：模板固化「三件套」（Pinia 重建、enableAutoUnmount、unstubAllGlobals）；收获是建立「单跑绿全量红 → 先列状态源清单再逐个找重置点」的固定动线，而不是乱改断言。

### 实录六：暗色模式上线后，评论区出现了「白色补丁」

**背景**：暗色模式功能上线后，用户截图反馈评论区下方有一块刺眼的白色卡片，亮色模式完全正常。

**排查过程**：DevTools 选中白块，Computed 面板看到 `background-color: var(--gray-900)`——亮色下是 `#111111`（近黑），符合「深色卡片」的设计意图；切暗色后再看，`--gray-900` 被解析为 `#EBEBEB`——梯度整体反转（E-F09），「深色」瞬间变「刺眼亮色」。根因清晰后走修复：这是「永远深色」语义（代码引用块），改用 `--code-bg/--code-text/--code-border`；同页排查又发现主 CTA 按钮用了 `--gray-800` 当底色，改为 `--contrast-*` 系列语义 token。改完在亮暗两种模式下人工过一遍评论区与按钮，再跑一次视觉相关组件测试确认无回归。

**根因**：把「灰度梯度」当「表面色」使用；亮色模式的巧合掩盖了语义错位。

**修复与收获**：组件库层面给「深色表面」加注释与示例；收获是一条可搜索的判断句——「**灰度会反转，语义不反转**：写样式前先问这个颜色在暗色下应该变成什么」。

### 实录之外的通用启示

六个案例共享一个模式：**第一报错 ≠ 根因**。404 的根因可能是前端绕过了统一实例；「被登出」的根因可能是双 axios 实例；「CI 红」的根因是测试从未触达的 XML 分支；「npm run 全灭」的根因在仓库之外；「测试红」的根因是状态泄漏的组合拳；「白色补丁」的根因是设计 token 的语义错位。方法论（A.0）之所以值得先学，就是因为它保证你**沿着证据走**而不是沿着第一印象走。把六个案例的「修复与收获」合起来看，你会发现高质量的排查最终都沉淀为两类资产：可执行的检查单（checklist），与可自动化的哨兵（测试/lint/CI）——这正是下一节要谈的「把排错变成资产」。

---

## A.6 附录延伸：三类「慢性病」的深度剖析

前三节的错误是「急性病」——报错明确、修完即愈。本节剖析三类更磨人的「慢性病」：它们不报错，或者报的错与真实病因相距甚远，值得初学者提前认识、老手对照自查。

### A.6.1 慢性病一：环境漂移——「上周还能跑」系列

**病征**：没有任何代码改动，但构建/测试/启动行为变了。报错面貌包括 E-B01（release 21 not supported）、E-E01（JDK 25）、E-F04（EENGINE）、E-E08（行尾）。

**病理**：开发环境不是冻结的。系统更新、PATH 变更、全局工具升级、另一个项目的需求覆盖了你的版本——环境每天都在漂，代码没动不代表系统没动。Windows 的 PATH 是重灾区：一个不配对引号（E-F01）就能让 npm run 全灭，而你盯了一下午代码。

**治疗**：环境差异排查固定三连：`mvnw -version`（Maven 眼中的 JDK）、`node -v` + `npm ci --dry-run`（Node 与引擎约束）、`git config core.autocrlf`（行尾）。三连输出贴进提问，一半的环境问题当场现形。

**疫苗**：项目级固定版本（JAVA_HOME 脚本、fnm/.node-version、.gitattributes），新机器初始化走 E-E07 的清单；把「环境冒烟」作为克隆仓库后的第一个动作。

### A.6.2 慢性病二：契约漂移——「两边各说各话」系列

**病征**：前后端各自的单测全绿，联调却崩。典型：E-B20/E-F02（信封读取层数错）、E-B09（/api 前缀一边带一边不带）、E-B21（日期时区两边理解不同）、E-F11（访客标识头一边发一边查无）、E-F17（Markdown 渲染两处封装不同步）。

**病理**：契约只存在于两拨人（或两份代码）的默契里，而默契会腐烂。SelfBlog 有几处「反直觉契约」是重灾区：axios 拦截器返回的是信封本身（`res.data` 即业务数据）、`@RestController` 自动加 `/api` 前缀（源码映射不带）、`VITE_API_BASE_URL` 名字像开关实际无人读取、store id 叫 `auth` 文件名却是 `user.ts`。这些契约**没有编译器兜底**——读错层、写错前缀都能通过 type-check，只在运行时炸。

**治疗**：契约问题用「端到端最小链路」验证：curl 打后端 → DevTools 看前端实际发出的 URL 与头 → 对比两者差异。差异就是契约破裂点。修改契约时（比如有人想真的启用 `VITE_API_BASE_URL`），必须同时改消费方与文档，并在 CHANGELOG 的「影响文件」表里列出两侧文件。

**疫苗**：把反直觉契约写成显式断言——后端切片测试里带 `/api` 前缀请求（对齐生产路径）、前端 API 层类型以信封契约为准、部署冒烟脚本核对 `VITE_API_TARGET` 与后端端口。CI 四个 job 已经覆盖了「H2 全量 / 前端全闸门 / 真库 / Docker 冒烟」，让契约破裂在 merge 前可见。

### A.6.3 慢性病三：状态泄漏——「第一个测试定终身」系列

**病征**：单跑测试绿、全量跑红；或页面用久了行为诡异（内存涨、事件重复触发、确认框弹两次）。涉及 E-F08（jsdom 五陷阱）、E-F12（Pinia 泄漏）、E-B25（缓存放大的旧数据）。

**病理**：有状态的东西（模块级单例、全局监听器、内存缓存、Pinia active 实例、jsdom 的 window）默认**活到进程结束**。测试与浏览器都是长寿命进程，第一个写入状态的使用者就「定终身」，后来者读到的是遗产。这类问题最阴险的形态是**时序依赖**：只有全量跑（或用户操作到第 N 次）时才触发，单跑永远复现不了。

**治疗**：给每种状态找到它的「重置点」——Pinia 用 `setActivePinia(createPinia())` 每 test 重建（E-F12）；组件用 `enableAutoUnmount(afterEach)`（E-F08）；rAF 用「入队 + 手动 flush + afterEach 清队」（E-F08）；缓存在测试 profile 里关闭（框架已做）；浏览器的重复监听器用 DevTools 的 `getEventListeners(window)` 检查。找不到重置点的状态，才是真正的架构问题——比如模块级可变变量应该降级为实例字段或 store。

**疫苗**：新测试模板自带三件套（beforeEach 重建 Pinia、enableAutoUnmount、unstubAllGlobals）；review 测试 diff 时问一句「这个测试假设了什么前置状态」。

### A.6.4 排错心态：三个「不要」

1. **不要在没有最小复现前动手改代码**。改了也验证不了，还污染现场。
2. **不要把「巧合修好了」当修复**。重试后通过的问题必须找到根因，否则它会在生产以十倍代价回来。
3. **不要一个人闷两小时**。两小时是你的止损线：把 A.0 的五步法输出（报错原文、已排除项、最小复现、环境三连结果）整理成一段话，发给同事或社区——整理过程本身经常让你发现答案。

### A.6.5 把你的新错误写进本手册：四段式模板

本手册是活的文档，你排掉的每个新错都值得按统一格式沉淀。直接复制下面的骨架填写：

````markdown
#### E-XNN 错误短标题：一句话点名机制

【现象】
```text
（真实报错原文，保留可 grep 的关键字）
```
（一两句交代触发场景）

【原因】机制讲清楚：谁在什么层次、为什么拒绝/失败。
（对初学者补一句类比或「翻译成人话」）

【解法】可直接落地的步骤，命令给完整形式（含 PowerShell 引号）。

【预防】一条可写进团队规范/检查单的守则。
````

填写时的三条质量标准：现象段必须是**你真实看到的原文**（不是凭记忆的转述），这是手册「grep 可搜」价值的来源；原因段必须回答「为什么」，如果只能写「删了就好了」，说明还没找到根因，先回 A.0 第三步；预防段要能变成一条自动化检查（测试、lint、CI 项）或一句能贴进 code review 的守则。写完后：核对小节编号（E-B 后端 / E-F 前端 / E-E 环境）、在 A.4 速查总表加一行索引、若该错误影响了测试基线或构建流程，同步更新 `CHANGELOG.md` 与相关 README——这是 SelfBlog 文档维护的既有约定。

### A.6.6 收束：排查能力的三个层次

回看整本手册，排查能力其实分三层，对应你成长的三个阶段。**第一层：认错误**——看到报错能对号入座，速查表与四段式条目服务这一层，多数初学者卡在这里，症状是把报错当独立事件死记。**第二层：读机制**——理解每一类拒绝背后的系统结构（安全链顺序、代理拓扑、状态生命周期），报错换了面目也能推导出来，A.1/A.2 各条目的【原因】段与 A.6 三类慢性病服务这一层。**第三层：造哨兵**——不满足于修掉眼前的问题，而是让同类问题下次自动暴露：一条测试、一个 lint 规则、一项 CI 检查、一张检查单。SelfBlog 仓库本身（588 + 400 个测试、五道闸门、四个真实执行的 CI job）就是第三层思维的产物，而 A.5 六个实录里每一次的「修复与收获」，都是从第一层迈向第三层的台阶。愿你的报错永远可搜、可复现、可修复——更愿它们越来越少。

至此，本附录的全部内容结束。愿你的报错永远可搜、可复现、可修复。
