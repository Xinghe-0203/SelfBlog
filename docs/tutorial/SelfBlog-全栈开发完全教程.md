# SelfBlog 全栈开发完全教程

## —— 以一个真实上线的个人博客项目学习 Java · Spring Boot · TypeScript · Vue 3

**版本**：v2.0 配套（SelfBlog v2.0.26） | **适用读者**：有一定编程基础、想系统学会「Java 后端 + 现代前端」全栈开发的初学者与转行者

---

## 为什么是这本教程

市面上不缺 Java 教程，也不缺 Vue 教程。缺的是**一条从零到真实项目上线的完整路径**——大多数教程停在了语法演示，而真实工程里最难的部分恰恰在语法之外：

- 为什么统一响应要用 `Result<T>` 信封，而不是直接返回实体？
- 为什么软删除要 `@TableLogic`，手写 SQL 时又为什么必须带上 `is_deleted = 0`？
- 为什么前端启动顺序错了，已登录用户会被弹回登录页？
- 为什么 API 拦截器返回的是「信封本身」，调用方读 `res.data` 而不是 `res.data.data`？

这些问题的答案，全部藏在 **SelfBlog** 这个真实项目里。本教程的每一章都以它为标本：所有代码示例都能在仓库源码中找到原型，所有「陷阱」都是开发过程中真实踩过的坑。你学到的不是玩具代码，而是一个经历了 588 个后端测试、400 个前端测试、四条 CI 流水线检验的工程实践。

**SelfBlog 技术栈一览：**

| 层 | 技术 | 版本 |
|------|------|------|
| 后端语言 | Java | 21（LTS） |
| 后端框架 | Spring Boot | 3.3.13 |
| ORM | MyBatis Plus | 3.5.8 |
| 安全 | Spring Security + JWT（jjwt） | 0.12.6 |
| 缓存 | Caffeine | 8 个具名缓存 |
| 数据库 | SQLite / H2 / MySQL 三库切换 | DB_TYPE 环境变量 |
| 前端框架 | Vue | 3.5.44 |
| 构建工具 | Vite | 6 |
| 前端语言 | TypeScript | 6.0.3（strict + noUncheckedIndexedAccess） |
| 状态管理 | Pinia | 5 个 store |
| 测试 | JUnit 5 / Vitest + jsdom | 588 + 400 tests |
| CI | GitHub Actions | 四个真实 job |

---

## 学习路径建议

本教程共七章，按依赖关系组织。**不要跳章**——后一章的案例会直接引用前一章的概念：

```
第一章 Java 语言基础          ← 一切的地基：语法、OOP、集合、异常、IO
        ↓
第二章 Java 进阶特性          ← 多线程、JVM、反射、泛型、注解、JDBC（Spring 的前置知识）
        ↓
第三章 Spring Boot 后端开发   ← IoC/AOP/配置/MVC/MyBatis Plus/事务/Security/缓存（核心章）
        ↓
第四章 TypeScript             ← 类型思维：从 JS 的自由到 TS 的约束
        ↓
第五章 Vue 3                  ← 组合式 API、Router、Pinia、Axios 拦截器、启动顺序
        ↓
第六章 前端工程化             ← Vite/ESLint/Prettier/Vitest/CI/部署（工程化的「隐形一半」）
        ↓
第七章 项目实战               ← 用发布文章、访客评论、匿名点赞三个功能串起全部知识
```

**三种读者，三种走法：**

1. **完全新手**：从第一章逐章读到第七章，每章的代码都要亲手跑一遍。预计 8–12 周。
2. **会 Java、学前端**：第一章快速过（重点看 1.7 集合选型与 1.8 异常实践），第三章精读，第四~七章精读。预计 4–6 周。
3. **会前端、补后端**：第一、二章选读（重点 2.4 反射与注解、2.6 JDBC），第三、七章精读。预计 4–6 周。

**每章的结构约定**：知识点讲解 → 可运行代码 → SelfBlog 真实案例 → 章末总结与参考资源。标 ⚠️ 的段落是「真实踩坑记录」，请格外留意。

**十二周参考计划**（按每天 1.5～2 小时估算，可按自己的节奏伸缩）：

| 周次 | 内容 | 里程碑自检 |
|------|------|-----------|
| 第 1 周 | 第一章 1.1–1.5（环境/语法/OOP） | 能独立写出带继承与接口的小程序 |
| 第 2 周 | 第一章 1.6–1.9（API/集合/异常/IO） | 能说清 ArrayList 与 HashMap 的选型依据 |
| 第 3 周 | 第一章 1.10–1.13（Lambda/Maven/JUnit）+ 第二章 2.1–2.2 | 能用 JUnit 给一个类写测试 |
| 第 4 周 | 第二章 2.3–2.6（注解反射/泛型/JVM） | 能解释「Spring 为什么能扫描注解」 |
| 第 5 周 | 第二章 2.7–2.13（Java 21/JDBC/测试/设计模式） | 能手写一个动态代理示例 |
| 第 6 周 | 第三章 3.1–3.7（IoC/AOP/配置/MVC） | 能说清 @Autowired 背后发生了什么 |
| 第 7 周 | 第三章 3.8–3.9（MyBatis Plus/事务） | 能解释软删除与手写 SQL 的 is_deleted=0 |
| 第 8 周 | 第三章 3.10–3.21（Security/缓存/测试/打包） | 能跑通 SelfBlog 后端全部测试 |
| 第 9 周 | 第四章（TypeScript 全章） | 能读懂 SelfBlog 前端 types/ 全部类型 |
| 第 10 周 | 第五章（Vue 3 全章） | 能复述 main.ts 七步启动顺序及原因 |
| 第 11 周 | 第六章（工程化全章） | 能独立跑通前端五道闸门 |
| 第 12 周 | 第七章 + 附录自测 | **能独立为 SelfBlog 添加一个新功能** |

每周最后一天花 30 分钟回看本周 ⚠️ 段落与章末总结——**教程的复利来自重复暴露**，第一遍读懂和第三遍能复述是两种掌握程度。

---

## 如何使用随书项目

```bash
# 克隆 SelfBlog 仓库
git clone https://github.com/Xinghe-0203/SelfBlog.git
cd SelfBlog

# 后端（需要 JDK 21，仓库自带 Maven Wrapper）
cd backend
./mvnw spring-boot:run "-Dskip.npm=true" "-Dskip.installnodenpm=true"

# 前端（另开终端）
cd frontend
npm ci
npm run dev        # http://localhost:3000
```

环境变量、端口、PATH 常见坑，见第七章 7.2 节与第六章 6.2 节的实录。

---

## 目录

- **第一章 Java 语言基础**：环境搭建、基本语法、面向对象（封装/继承/多态/抽象类与接口）、常用 API（String/日期）、集合框架（含选型速查表与 N+1 实战）、异常体系与最佳实践、泛型、枚举、IO 与 NIO.2、Lambda 与 Stream、Maven 与单元测试入门、`Result<T>` 泛型信封案例
- **第二章 Java 进阶特性**：多线程与线程池、synchronized/Lock/volatile、ThreadLocal、CompletableFuture、JVM 内存区域与垃圾回收、类加载机制、反射与动态代理（Spring AOP 的地基）、Java 21 虚拟线程与新语法、JDBC 与连接池、事务隔离级别、JUnit 5 与 Mockito、设计模式实战精选、HTTP 协议要点、加密与 JWT 原理
- **第三章 Spring Boot 后端开发**：自动配置与条件装配、IoC 与三种注入、Bean 生命周期与循环依赖、AOP 五种通知与实战、配置体系与 `.env`、RESTful 与统一响应、全局异常处理、Bean Validation、MyBatis Plus 全解（软删除/分页/方言/自动填充）、事务传播与失效场景、Spring Security + JWT 认证全链路、Caffeine 缓存、限流、日志、定时任务、Actuator、切片测试配方、打包与部署
- **第四章 TypeScript**：从 JS 到 TS 的心智转变、基础类型与收窄、接口与类型别名、函数与重载、类与访问修饰符、泛型与工具类型、类型守卫与判别联合、条件类型与映射类型、装饰器（与 Spring 注解对照）、tsconfig strict 逐项精讲、Vue 3 集成、API 层类型化实战、11 种常见编译错误急救手册
- **第五章 Vue 3**：模板语法与指令、组合式 API（ref/computed/watch/生命周期）、组件通信与插槽、Vue Router 4（含动态注册后台路由）、Pinia 状态管理、Axios 拦截器完整还原（信封契约/Token 刷新排队/401 处理）、应用启动的承重顺序、五个 store 实读、组件测试
- **第六章 前端工程化**：Node.js 与 npm（含 PATH 引号惨案实录）、Vite 构建与代理、ESLint 9 扁平配置、Prettier、TS 集成与路径别名、Vitest 单元测试与 jsdom 五陷阱、GitHub Actions 四任务 CI、质量闸门体系、依赖管理、部署形态（Vercel 代理生成器与 Docker）
- **第七章 项目实战**：需求裁剪与单作者模式、数据库设计与三库初始化、发布文章后端全链路（门面/查询/交互三分）、统一响应与异常翻译、认证全链路（防爆破/改密下线/四头注入）、前端阅读链路与启动顺序、缓存与限流、SEO 可阅读 HTML、测试策略、部署、新功能开发完整流程（以阅读时长估算为例）、架构复盘

**四个附录**（工具性质，随用随查，不必按序阅读）：

- **附录A 常见错误排查手册**：通用排查方法论 + 后端（Spring/MyBatis Plus/JWT/事务）/ 前端（Node/TS/Vite/Vitest）/ 数据库 / 部署共 40+ 条错误，每条按「现象→原因→解法→预防」四段式展开，附心法八条
- **附录B 源码导读地图 + 全栈术语表**：后端 12 个包与前端 10 个目录的职责地图、八条主题阅读路线（认证/发文/访客互动/SEO/启动/请求/路由/基础设施）、150+ 条术语速查（每条附 SelfBlog 实例位置）
- **附录C 面试题精选与自测参考答案**：90 道面试题（Java/集合并发/JVM/Spring/数据库/TS+工程化/Vue 七节，每题含考察点与答案要点）+ 各章自测题 35 道与参考答案
- **附录D 工具链实战**：环境搭建与自检脚本、IDEA/VSCode 提效配置、curl 与 DevTools 调试、SQLite 直查数据、断点调试进阶、Git 协作与事故恢复、效率肌肉记忆清单

---

## 约定与免责

- 本教程所有源码引用以仓库实际代码为准；教程写作时项目版本为 v2.0.26，后续版本的行为变化请以 `CHANGELOG.md` 为准。
- 书中「未验证」的声明（如真实 Vercel 部署、Docker 运行）是工程诚信的一部分：验证记录与代码同等重要，没测过的东西必须如实标注。
- 教程代码均可自由复制修改，用于学习目的时无需署名。

祝学习顺利。当读完第七章、能独立为 SelfBlog 添加一个新功能时，你就不再是「学过 Java 和 Vue 的人」，而是「做过全栈开发的工程师」了。

---

---

# 第一章 Java 基础

## 1.1 Java 语言概述

Java 是由 Sun Microsystems（现已被 Oracle 收购）于 1995 年发布的一门面向对象编程语言。Java 的设计理念是"一次编写，到处运行"（Write Once, Run Anywhere），这得益于 Java 虚拟机（JVM）的跨平台特性。

### 1.1.1 Java 的特点

**跨平台性**：Java 程序编译后生成字节码（.class 文件），可以在任何安装了 JVM 的平台上运行。这使得 Java 成为企业级应用开发的首选语言。

**面向对象**：Java 是一门纯粹的面向对象编程语言，一切皆对象。它支持封装、继承、多态等面向对象的特性。

**安全性**：Java 提供了多层次的安全机制，包括字节码验证、安全管理器、沙箱模型等，使得 Java 特别适合开发安全敏感的应用。

**健壮性**：Java 的强类型机制、异常处理、垃圾回收等特性，使得 Java 程序更加健壮，减少了内存泄漏和指针错误等问题。

**多线程支持**：Java 内置了多线程支持，提供了丰富的并发编程 API，使得开发高性能并发程序更加容易。

**丰富的类库**：Java 拥有庞大的标准类库和第三方库生态系统，涵盖了网络编程、数据库连接、图形界面、XML 处理等各个领域。

### 1.1.2 Java 的版本演进

Java 自 1995 年发布以来，经历了多次重大更新：

- **Java 5（2004）**：引入了泛型、注解、枚举、自动装箱/拆箱、可变参数等特性
- **Java 8（2014）**：引入了 Lambda 表达式、Stream API、新的日期时间 API、接口默认方法等
- **Java 11（2018）**：LTS 版本，引入了局部变量类型推断（var）、新的 HTTP 客户端等
- **Java 14（2020）**：switch 表达式转正，NPE 报错信息变得友好（会告诉你哪个变量是 null）
- **Java 15（2020）**：文本块（Text Blocks）转正，多行字符串有了正式写法
- **Java 16（2021）**：record 类、instanceof 模式匹配转正
- **Java 17（2021）**：LTS 版本，密封类（sealed）转正
- **Java 21（2023）**：LTS 版本，引入了虚拟线程、记录模式、switch 模式匹配、分代 ZGC 等重要特性

本项目（SelfBlog）使用的是 **Java 21**，这是一个长期支持（LTS）版本，提供了许多现代化特性。本章后续讲解的 switch 表达式、record、sealed、文本块、模式匹配等新语法，都可以直接在 Java 21 项目中使用——你也会在 SelfBlog 源码里陆续遇到它们。

### 1.1.3 Java 的应用领域

Java 广泛应用于以下领域：

- **企业级应用**：Spring Boot、Spring Cloud 等框架使得 Java 成为企业级应用开发的首选
- **Web 开发**：Servlet、JSP、Spring MVC 等技术栈
- **移动开发**：Android 应用开发（虽然现在 Kotlin 越来越流行）
- **大数据**：Hadoop、Spark 等大数据框架都是用 Java/Scala 编写的
- **微服务**：Spring Boot + Spring Cloud 微服务架构
- **云原生**：Quarkus、Micronaut 等云原生框架

### 1.1.4 JVM、JRE 与 JDK 的关系

初学者经常混淆三个概念，这里一次讲清楚：

- **JVM（Java Virtual Machine）**：Java 虚拟机，负责执行字节码。正是 JVM 屏蔽了底层操作系统的差异，同一份 .class 文件才能跑在 Windows、Linux、macOS 上——这就是"一次编写，到处运行"的实现原理。
- **JRE（Java Runtime Environment）**：Java 运行环境 = JVM + Java 核心类库。只想**运行**现成的 Java 程序，装 JRE 就够。
- **JDK（Java Development Kit）**：Java 开发工具包 = JRE + 开发工具（编译器 javac、打包工具 jar、文档工具 javadoc、调试器 jdb 等）。要**开发** Java 程序，必须装 JDK。

三者的包含关系是 **JDK ⊃ JRE ⊃ JVM**：

```
┌──────────────────────────────────────────┐
│ JDK（开发工具包）                          │
│  ┌──────────────────────────────────────┐ │
│  │ JRE（运行环境）                        │ │
│  │  ┌──────────────────────────────────┐│ │
│  │  │ JVM（虚拟机）                     ││ │
│  │  └──────────────────────────────────┘│ │
│  │  + 核心类库（java.lang、java.util…）   │ │
│  └──────────────────────────────────────┘ │
│  + javac、jar、javadoc、jdb 等开发工具      │
└──────────────────────────────────────────┘
```

Oracle 从 Java 11 起不再单独提供 JRE 下载，安装 JDK 即可获得全部能力——1.2 节安装的就是 JDK。日常说的"装 Java"，装的都是 JDK。

## 1.2 环境搭建

### 1.2.1 安装 JDK 21

JDK（Java Development Kit）是 Java 开发工具包，包含了编译器（javac）、运行时环境（JRE）和开发工具。

**Windows 平台安装步骤：**

1. 访问 Oracle 官网或 Adoptium（Eclipse Temurin）下载 JDK 21
2. 下载对应平台的安装包（Windows x64 Installer）
3. 运行安装程序，按照提示完成安装
4. 配置环境变量：
   - 新建 `JAVA_HOME` 环境变量，值为 JDK 安装路径（如 `C:\Program Files\Java\jdk-21`）
   - 在 `Path` 环境变量中添加 `%JAVA_HOME%\bin`

**为什么要配置 `JAVA_HOME`？**

很多 Java 生态工具（Maven、Gradle、Tomcat、IDEA 等）并不直接在 `Path` 里找 `java`，而是优先读取 `JAVA_HOME` 来定位 JDK。把"JDK 在哪里"集中定义到一个变量里，`Path` 只需要引用 `%JAVA_HOME%\bin`，将来升级或切换 JDK 时改这一处即可，所有工具自动跟着走。

**多版本 JDK 共存与管理**

实际开发中经常需要在一台机器上维护多个 JDK（老项目要 Java 8，SelfBlog 要 Java 21），Windows 下推荐的做法是：

1. 每个版本装到独立目录：`C:\Program Files\Java\jdk-8`、`C:\Program Files\Java\jdk-17`、`C:\Program Files\Java\jdk-21`
2. `JAVA_HOME` 只指向其中一个作为"系统默认版本"
3. 需要切换时，在**当前终端会话**里临时覆盖 `JAVA_HOME`，不影响全局设置

```powershell
# PowerShell：临时把当前窗口的 JAVA_HOME 指向 JDK 21
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'

# 验证当前会话用的是哪个 JDK
& "$env:JAVA_HOME\bin\java" -version
```

Linux/macOS 用户可以用 SDKMAN 管理多版本：`sdk install java 21-tem`（安装 Temurin 21）、`sdk use java 21-tem`（当前会话切换）、`sdk default java 21-tem`（设为默认），比手工改环境变量方便得多。

**验证安装：**

```bash
java -version
javac -version
```

输出应显示类似以下内容：

```
java version "21.0.x" 2023-xx-xx LTS
Java(TM) SE Runtime Environment (build 21.0.x+xx-LTS-xxx)
Java HotSpot(TM) 64-Bit Server VM (build 21.0.x+xx-LTS-xxx, mixed mode, sharing)
```

注意两条命令都要检查：`java -version` 验证运行时，`javac -version` 验证编译器。只配好了 `java` 而没配好 `javac`，说明 `Path` 指向的是 JRE 而不是 JDK。

### 1.2.2 常见环境问题排查

环境问题是初学者遇到的第一道坎，而且报错信息往往和真实原因相距甚远。下面是最常见的几类问题及定位方法。

**问题一：`'javac' 不是内部或外部命令`**

典型的 `Path` 配置错误。`javac.exe` 位于 JDK 的 `bin` 目录下，报这个错说明命令行找不到它。逐项检查：

1. `JAVA_HOME` 是否存在且指向 JDK 根目录（不是 `bin`，不是 JRE，末尾不要多余的反斜杠）
2. `Path` 里是否添加了 `%JAVA_HOME%\bin`（写成 `JAVA_HOME\bin`、丢了两侧百分号是最常见笔误）
3. 修改环境变量后**必须重开一个终端窗口**——已在运行的终端不会感知变化，IDEA 也要重启
4. 用 `echo %JAVA_HOME%`（cmd）或 `echo $env:JAVA_HOME`（PowerShell）确认变量确实生效

**问题二：`java -version` 显示的版本不对**

明明装了 JDK 21，命令行却显示 Java 8 或其他版本。常见原因：

- `C:\Windows\System32\` 下存在旧的 `java.exe`，而 `System32` 在 `Path` 中排在 `%JAVA_HOME%\bin` 前面。把 `%JAVA_HOME%\bin` 移到 `Path` **最上面**，或删除 System32 里的旧文件
- 安装 Oracle JDK 时勾选了"公开 JRE"，`Path` 里留下了 `C:\Program Files (x86)\Common Files\Oracle\Java\javapath` 条目，优先级更高。同样下移或删除

用 `where java`（Windows）或 `which java`（Linux/macOS）列出全部候选路径，排第一的就是实际生效的那个。

**问题三：构建报 `Unsupported class file major version`**

JVM 版本低于编译目标版本时会出现——例如用 JDK 17 跑要求 Java 21 的项目，字节码版本号对不上。看构建日志开头打印的 Java version，对照 `JAVA_HOME` 检查即可定位。

SelfBlog 的开发环境就出现过：机器默认 `JAVA_HOME` 指向 JDK 25，而项目只支持 Java 21，于是每次构建前都要先执行 `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'`。这是多版本共存场景的标准操作，习惯它。

**问题四：路径包含空格导致命令出错**

Windows 默认把 JDK 装到 `C:\Program Files\Java\jdk-21`，路径里的空格在脚本中容易出问题。规范做法是变量值带引号：`$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'`，或把 JDK 装到不含空格的目录（如 `C:\Java\jdk-21`）。

**问题五：PowerShell 下 Maven 的 -D 参数报错**

PowerShell 会对参数做二次解析，直接写 `mvnw.cmd test -Dskip.npm=true` 会把 `-D` 参数拆坏，必须加引号：

```powershell
backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

cmd 与 Linux shell 下不需要引号。**记住：在 SelfBlog 里跑后端 Maven 命令，永远带上这两个参数**（原因见 1.12.6 节与第三章）。

**问题六：中文乱码 / "编码 GBK 的不可映射字符"**

中文 Windows 的命令行默认 GBK 编码，源码文件通常是 UTF-8。`javac` 编译含中文注释的源码报"编码 GBK 的不可映射字符"时，加参数指定编码：`javac -encoding UTF-8 HelloWorld.java`。Maven 项目则在 pom.xml 里统一配置 `project.build.sourceEncoding=UTF-8`（SelfBlog 已配置），一劳永逸。

### 1.2.3 安装 Maven

Maven 是 Java 项目的构建和依赖管理工具。本项目使用 Maven Wrapper（mvnw），无需单独安装 Maven。

**Maven Wrapper 的使用：**

```bash
# Windows
mvnw.cmd clean install

# Linux/Mac
./mvnw clean install
```

Maven Wrapper 会自动下载项目所需的 Maven 版本，确保构建环境的一致性。

> **SelfBlog 实战提醒**：本仓库 `mvnw` 对应 Maven 3.9.12，且 `JAVA_HOME` 必须指向 JDK 21（JDK 25 不被支持）。PowerShell 下临时设置：
>
> ```powershell
> $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
> backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
> ```
>
> 两个 `-Dskip` 参数跑任何 Maven 命令都要带（原因见第三章 3.18：前端插件会在构建早期重建整个前端）。

Maven 的完整入门——pom.xml 结构、依赖管理、生命周期、常用命令、Maven Wrapper 原理——统一放在本章 **1.12 节**讲解，那里会结合 SelfBlog 的真实 pom 逐段拆解。

### 1.2.4 安装 IntelliJ IDEA

IntelliJ IDEA 是 JetBrains 开发的 Java 集成开发环境（IDE），是 Java 开发的首选工具。

**安装步骤：**

1. 访问 JetBrains 官网下载 IntelliJ IDEA
2. 选择 Community（免费）或 Ultimate（付费）版本——开发 Spring Boot 项目建议用 Ultimate（学生可免费申请授权），Community 足够完成本章的基础学习
3. 安装并激活
4. 配置 JDK：File → Project Structure → SDKs → 添加 JDK 21

**常用插件：**

- Lombok：支持 Lombok 注解
- MyBatisX：MyBatis 开发辅助
- Rainbow Brackets：彩色括号匹配
- GitToolBox：Git 增强工具

**初学者最值得用的功能：**

- **Live Templates**：输入 `psvm` 回车生成 main 方法、`sout` 生成 `System.out.println`、`fori` 生成标准 for 循环，本章所有示例都能快速敲出来
- **快捷键**：`Shift+F6` 重命名（所有引用同步改）、`Ctrl+Alt+L` 格式化、`Alt+Enter` 快速修复——用惯后效率翻倍
- **断点调试器**：`F8` 单步、`F9` 放行，配合 Watches 面板观察变量变化，是理解程序执行流程的最佳方式
- **Maven 面板**：右侧 Maven 工具窗双击任意生命周期阶段即可执行，等价于命令行但不用敲命令

### 1.2.5 轻量级替代：VS Code

如果电脑配置有限，或只是想写点小程序，VS Code 是不错的轻量选择：

1. 安装微软官方的 **Extension Pack for Java** 扩展包（含语言支持、调试器、Maven 支持、测试运行器、项目向导六个插件）
2. 打开任意 `.java` 文件，扩展会提示配置 JDK，选择已装的 JDK 21 即可
3. 通过 `Java: Configure Java Runtime` 命令管理多个 JDK

VS Code 的优势是启动快、内存占用低；劣势是对 Spring Boot 的支持（Bean 导航、profile 管理、数据库工具）不如 IDEA Ultimate 完善。**学习本章两者都够用，后续阅读 SelfBlog 源码推荐 IDEA。**

## 1.3 第一个 Java 程序

让我们从经典的"Hello World"程序开始：

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

**代码解析：**

- `public class HelloWorld`：定义一个公共类，类名必须与文件名一致
- `public static void main(String[] args)`：程序入口点，JVM 从这里开始执行
- `System.out.println`：标准输出流，打印字符串并换行

**编译和运行：**

```bash
# 编译
javac HelloWorld.java

# 运行
java HelloWorld
```

Java 11 之后还可以直接运行单个源文件：`java HelloWorld.java`，编译和运行一步完成（内部仍是先编译后运行，只是 .class 文件不留在磁盘上），对初学者非常友好。

### 1.3.1 以 SelfBlog 项目为例

SelfBlog 项目的启动类是 `SelfBlogApplication.java`：

```java
package com.selfblog;

import com.selfblog.config.DotenvConfig;
import com.selfblog.config.security.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class
})
@Import(SecurityConfig.class)
@EnableScheduling
public class SelfBlogApplication {

    private static final Logger log = LoggerFactory.getLogger(SelfBlogApplication.class);

    public static void main(String[] args) {
        DotenvConfig.load();
        log.info("========================================");
        SpringApplication.run(SelfBlogApplication.class, args);
        log.info("   SelfBlog启动成功！");
        log.info("   API文档地址：http://localhost:8825/doc.html");
        log.info("========================================");
    }
}
```

**代码解析：**

- `@SpringBootApplication`：Spring Boot 核心注解，包含了 `@SpringBootConfiguration`、`@EnableAutoConfiguration` 和 `@ComponentScan`
- `exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class}`：排除 Spring Security 的自动配置，因为项目使用了自定义的安全配置
- `@Import(SecurityConfig.class)`：导入自定义的安全配置类
- `@EnableScheduling`：启用定时任务支持
- `DotenvConfig.load()`：在 Spring Boot 启动前加载 .env 文件
- `SpringApplication.run()`：启动 Spring Boot 应用

对比两个 main 方法可以看出：**无论是 20 行的入门程序还是上万行的 Spring Boot 项目，入口都是同一个 `public static void main(String[] args)`**。区别只在于入口里做了什么——SelfBlog 在启动 Spring 容器之前先加载了 .env 配置文件，之后的所有复杂行为都交给 Spring 框架接管。

### 1.3.2 JShell：交互式学习环境

JDK 9 起自带交互式编程环境 JShell，输入一行代码立即看到结果，特别适合验证语法细节（无需建类、无需编译）：

```bash
$ jshell
|  Welcome to JShell -- Version 21
|  For an introduction type: /help intro

jshell> 1 + 2
$1 ==> 3

jshell> "Hello".toUpperCase()
$2 ==> "HELLO"

jshell> int[] arr = {5, 2, 8}
arr ==> int[3] { 5, 2, 8 }

jshell> java.util.Arrays.toString(arr)
$4 ==> "[5, 2, 8]"

jshell> /exit
|  Goodbye
```

JShell 会自动为未命名的结果生成 `$1`、`$2` 这样的变量，支持 Tab 补全，`/exit` 退出。本章的很多示例适合先在 JShell 里快速试一遍，再写进完整程序。

## 1.4 基本语法

### 1.4.1 数据类型

Java 的数据类型分为两大类：**基本数据类型**和**引用数据类型**。

**基本数据类型（8 种）：**

| 类型 | 大小 | 范围 | 默认值 |
|------|------|------|--------|
| byte | 1 字节 | -128 ~ 127 | 0 |
| short | 2 字节 | -32768 ~ 32767 | 0 |
| int | 4 字节 | -2^31 ~ 2^31-1 | 0 |
| long | 8 字节 | -2^63 ~ 2^63-1 | 0L |
| float | 4 字节 | 约 ±3.4E38 | 0.0f |
| double | 8 字节 | 约 ±1.7E308 | 0.0d |
| char | 2 字节 | 0 ~ 65535 | '\u0000' |
| boolean | 1 位 | true/false | false |

**引用数据类型：**

- 类（Class）
- 接口（Interface）
- 数组（Array）
- 枚举（Enum）
- 注解（Annotation）

**代码示例：**

```java
public class DataTypes {
    public static void main(String[] args) {
        // 整数类型
        byte b = 100;
        short s = 10000;
        int i = 100000;
        long l = 10000000000L;
        
        // 浮点类型
        float f = 3.14f;
        double d = 3.141592653589793;
        
        // 字符类型
        char c = 'A';
        char chinese = '中';
        
        // 布尔类型
        boolean flag = true;
        
        // 字符串（引用类型）
        String str = "Hello, Java!";
        
        System.out.println("byte: " + b);
        System.out.println("short: " + s);
        System.out.println("int: " + i);
        System.out.println("long: " + l);
        System.out.println("float: " + f);
        System.out.println("double: " + d);
        System.out.println("char: " + c);
        System.out.println("boolean: " + flag);
        System.out.println("String: " + str);
    }
}
```

**两个必须养成的习惯：**

1. **long 字面量加大写 L**：`10000000000L` 的 `L` 不能省，否则字面量被当作 int，超出 int 范围直接编译错误。也不要用小写 `l`——它和数字 `1` 几乎无法区分。
2. **不要用 float/double 处理金额**：二进制浮点数无法精确表示 0.1，`System.out.println(0.1 + 0.2)` 输出的是 `0.30000000000000004`。涉及金额一律用 `BigDecimal`，且用字符串构造：`new BigDecimal("0.1")`（用 double 构造会把误差一起带进去）。

**类型转换：**

```java
public class TypeConversion {
    public static void main(String[] args) {
        // 自动类型转换（小 → 大，隐式）
        int i = 100;
        long l = i;       // int → long，自动
        double d = l;     // long → double，自动
        System.out.println(d);  // 100.0

        // 强制类型转换（大 → 小，显式，可能丢失精度）
        double pi = 3.14159;
        int intPi = (int) pi;   // 直接截断小数，不是四舍五入
        System.out.println(intPi);  // 3

        byte b = (byte) 130;    // 130 超出 byte 范围，溢出
        System.out.println(b);  // -126（按补码回绕，不报错更危险）

        // char 与 int 互转（按 Unicode 码点）
        char c = 'A';
        int code = c;                    // 65
        char next = (char) (code + 1);   // 'B'
        System.out.println(code + " " + next);

        // 表达式中的类型提升
        byte x = 10, y = 20;
        // byte z = x + y;          // 编译错误！x + y 已提升为 int
        byte z = (byte) (x + y);    // 正确：先算再强转
        System.out.println(z);      // 30
    }
}
```

记一条规律：**小的塞给大的自动转，大的塞给小的必须强转且可能丢数据**。强转编译器不拦你，运行结果对不对要自己负责——`(byte) 130` 得到 `-126` 就是典型例子。

### 1.4.2 变量与常量

**变量**是程序运行过程中可以改变的量。

```java
public class VariableDemo {
    public static void main(String[] args) {
        // 变量声明
        int age;
        // 变量赋值
        age = 25;
        // 变量声明并赋值
        String name = "张三";
        
        // 变量可以重新赋值
        age = 26;
        System.out.println("姓名：" + name + "，年龄：" + age);
    }
}
```

**常量**是使用 `final` 关键字修饰的变量，一旦赋值就不能再改变。

```java
public class ConstantDemo {
    // 类常量
    public static final double PI = 3.141592653589793;
    public static final String APP_NAME = "SelfBlog";
    
    public static void main(String[] args) {
        // 局部常量
        final int MAX_SIZE = 100;
        // MAX_SIZE = 200; // 编译错误：不能修改常量
        
        System.out.println("PI = " + PI);
        System.out.println("APP_NAME = " + APP_NAME);
        System.out.println("MAX_SIZE = " + MAX_SIZE);
    }
}
```

**命名规范**（Java 社区通用约定，SelfBlog 代码全程遵守）：

- 变量、方法：小驼峰（`userName`、`getPostById`）
- 常量：全大写加下划线（`MAX_SIZE`、`DEFAULT_PAGE_SIZE`）
- 类、接口：大驼峰（`BlogPostService`）
- 包名：全小写（`com.selfblog.config`）

初学者最常见的两个命名坏味道，现在就纠正：一是**拼音混杂**（`yongHuMing`、`wzTitle`），半年后连自己都看不懂；二是**无意义缩写**（`a`、`tmp2`、`flag1`）。判断标准很简单——变量名要能让读代码的人不看上下文就明白用途。SelfBlog 的源码里你会看到 `isDeleted`、`viewCount`、`publishedAt` 这类"见名知义"的字段名，这正是长期可维护代码的基本素养。

**变量三要素与内存图景**

每个变量都有三要素：**类型**（决定占多少内存、能存什么值）、**名字**（引用内存位置的标签）、**值**（内存里实际的内容）。基本类型变量直接存值（`int age = 25` 里 age 这个"盒子"里装的就是 25）；引用类型变量存的是对象的地址（`String name = "张三"` 里 name 存的是字符串对象在堆中的位置）。这一区别在 1.4.3 讲 `==` 比较、在 1.5 讲对象传参时会反复用到，现在先埋个伏笔。

**常量**是使用 `final` 关键字修饰的变量，一旦赋值就不能再改变。

当右边类型显而易见时，可用 `var` 让编译器推断，减少重复：

```java
var list = new ArrayList<String>();  // 推断为 ArrayList<String>
var map  = new HashMap<String, Integer>();
var name = "SelfBlog";               // 推断为 String

// 注意：var 只能用于带初始值的局部变量
// var x;           // 编译错误：没有初始值无法推断
// var y = null;    // 编译错误：null 无法推断
// 成员变量、方法参数、返回值都不能用 var
```

`var` 不是动态类型——变量类型在编译期就固定了，之后不能赋别的类型的值。SelfBlog 使用 Java 21，代码里可以放心用 var，但建议只在类型名很长且显而易见时用（如泛型集合）；`int age = 25` 这种就不必了。

### 1.4.3 运算符

Java 的运算符分为以下几类：

**算术运算符：**

```java
public class ArithmeticOperators {
    public static void main(String[] args) {
        int a = 10, b = 3;
        
        System.out.println("a + b = " + (a + b));  // 13
        System.out.println("a - b = " + (a - b));  // 7
        System.out.println("a * b = " + (a * b));  // 30
        System.out.println("a / b = " + (a / b));  // 3（整数除法，直接丢弃小数）
        System.out.println("a % b = " + (a % b));  // 1（取余）
        
        // 想要小数结果，先把其中一个转成浮点数
        System.out.println((double) a / b);        // 3.3333333333333335
        
        // 整数除以 0 抛异常；浮点数除以 0 得到 Infinity / NaN
        // System.out.println(a / 0);              // ArithmeticException
        System.out.println(10.0 / 0);              // Infinity
        System.out.println(0.0 / 0.0);             // NaN
        
        // 自增自减
        int c = 5;
        System.out.println("c++ = " + (c++));  // 5（先使用，后自增）
        System.out.println("++c = " + (++c));  // 7（先自增，后使用）
        
        // 复合赋值运算符（自带隐式类型转换）
        short s = 10;
        // s = s + 1;   // 编译错误：s + 1 提升为 int，不能直接赋回 short
        s += 1;         // 正确：等价于 s = (short)(s + 1)
        System.out.println(s);  // 11
    }
}
```

**关系运算符：**

```java
public class RelationalOperators {
    public static void main(String[] args) {
        int a = 10, b = 20;
        
        System.out.println("a == b: " + (a == b));  // false
        System.out.println("a != b: " + (a != b));  // true
        System.out.println("a > b: " + (a > b));    // false
        System.out.println("a < b: " + (a < b));    // true
        System.out.println("a >= b: " + (a >= b));  // false
        System.out.println("a <= b: " + (a <= b));  // true
    }
}
```

注意：`==` 对基本类型比较**值**，对引用类型比较**地址**（是否为同一个对象）。比较对象内容要用 `equals()`——这是初学者最常见的 bug 来源，详见 1.6.1 节字符串比较与 1.5.7 节 equals 重写规范。

**逻辑运算符：**

```java
public class LogicalOperators {
    public static void main(String[] args) {
        boolean x = true, y = false;
        
        System.out.println("x && y: " + (x && y));  // false（逻辑与）
        System.out.println("x || y: " + (x || y));  // true（逻辑或）
        System.out.println("!x: " + (!x));          // false（逻辑非）
        
        // 短路运算
        int a = 5, b = 10;
        // 如果第一个条件为 false，第二个条件不会执行
        boolean result = (a > 10) && (++b > 5);
        System.out.println("b = " + b);  // 10（b 没有自增）
        
        // 短路的实际用途：先用 && 保证对象非空，再访问其属性，避免 NPE
        String name = null;
        // name != null && name.length() > 0 —— name 为 null 时不会执行 length()
        
        // & 和 | 不短路：两个操作数都会计算
        int c = 10;
        boolean r2 = (a > 10) & (++c > 5);
        System.out.println("c = " + c);  // 11（没有短路，c 自增了）
    }
}
```

**位运算符：**

```java
public class BitwiseOperators {
    public static void main(String[] args) {
        int a = 5;   // 二进制：0101
        int b = 3;   // 二进制：0011
        
        System.out.println("a & b: " + (a & b));   // 1（按位与：同 1 才是 1）
        System.out.println("a | b: " + (a | b));   // 7（按位或：有 1 就是 1）
        System.out.println("a ^ b: " + (a ^ b));   // 6（按位异或：不同才是 1）
        System.out.println("~a: " + (~a));         // -6（按位取反）
        System.out.println("a << 2: " + (a << 2)); // 20（左移，相当于 ×4）
        System.out.println("a >> 2: " + (a >> 2)); // 1（右移，相当于 ÷4，符号位保留）
        System.out.println("-8 >>> 1: " + (-8 >>> 1)); // 2147483644（无符号右移，高位补 0）
    }
}
```

位运算在业务代码里最常见的用途是**用一个 int 管理一组开关标志**：

```java
public class PermissionFlags {
    // 每个权限占一个二进制位
    public static final int READ    = 1;       // 0001
    public static final int WRITE   = 1 << 1;  // 0010
    public static final int PUBLISH = 1 << 2;  // 0100
    public static final int ADMIN   = 1 << 3;  // 1000

    public static void main(String[] args) {
        int userPerms = READ | WRITE;   // 授予读写权限 → 0011

        // 判断是否拥有某权限：按位与结果非 0 即拥有
        boolean canRead = (userPerms & READ) != 0;    // true
        boolean isAdmin = (userPerms & ADMIN) != 0;   // false

        // 追加权限
        userPerms |= PUBLISH;   // 0111
        // 移除权限
        userPerms &= ~WRITE;    // 0101

        System.out.println("canRead=" + canRead + ", isAdmin=" + isAdmin);
        System.out.println("当前权限位：" + Integer.toBinaryString(userPerms));

        // 其他常用技巧
        int n = 13;
        System.out.println((n & 1) == 1 ? "奇数" : "偶数");  // &1 判奇偶
        System.out.println(n << 1);   // 26，左移一位 = 乘 2
        System.out.println(n >> 1);   // 6，右移一位 = 除 2 取整
    }
}
```

日常业务里更多用枚举或 boolean 字段表达权限，位运算胜在省内存、可组合，操作系统和协议解析领域用得很多。学习位运算的主要目标是**看懂别人的代码**。

**三元运算符：**

```java
public class TernaryOperator {
    public static void main(String[] args) {
        int a = 10, b = 20;
        // 条件 ? 值1 : 值2
        int max = (a > b) ? a : b;
        System.out.println("最大值：" + max);  // 20
        
        // 嵌套三元运算符
        int score = 85;
        String grade = (score >= 90) ? "优秀" : 
                       (score >= 80) ? "良好" : 
                       (score >= 60) ? "及格" : "不及格";
        System.out.println("成绩等级：" + grade);  // 良好
    }
}
```

**运算符优先级（简表）**：从高到低大致为 `()` → `++`/`--`/`!` → `*`/`/`/`%` → `+`/`-` → `<<`/`>>` → 关系运算符 → `==`/`!=` → `&` → `^` → `|` → `&&` → `||` → 三元 → 赋值。**不要背这张表**——用括号把意图表达清楚，既不会错也方便别人读：

```java
// 不推荐：依赖记忆
boolean ok = a + b > c * 2 && d != 0;
// 推荐：加括号，意图一目了然
boolean ok = ((a + b) > (c * 2)) && (d != 0);
```

### 1.4.4 流程控制

**if-else 语句：**

```java
public class IfElseDemo {
    public static void main(String[] args) {
        int score = 85;
        
        if (score >= 90) {
            System.out.println("优秀");
        } else if (score >= 80) {
            System.out.println("良好");
        } else if (score >= 60) {
            System.out.println("及格");
        } else {
            System.out.println("不及格");
        }
        
        // 嵌套 if
        int age = 25;
        boolean hasLicense = true;
        if (age >= 18) {
            if (hasLicense) {
                System.out.println("可以开车");
            } else {
                System.out.println("需要先考驾照");
            }
        } else {
            System.out.println("未达到法定年龄");
        }
    }
}
```

**switch 语句（三种写法演进）：**

switch 是最能体现"Java 在变现代"的语法点之一。同一个需求，三种写法对比着看：

```java
public class SwitchDemo {
    public static void main(String[] args) {
        int day = 3;
        
        // ========== 写法一：传统 switch（Java 1.0 至今） ==========
        switch (day) {
            case 1:
                System.out.println("星期一");
                break;               // 忘了 break 会"穿透"到下一个 case！
            case 2:
                System.out.println("星期二");
                break;
            case 3:
                System.out.println("星期三");
                break;
            default:
                System.out.println("其他");
        }
        
        // ========== 写法二：箭头语法（Java 14+） ==========
        // 不用 break、没有穿透、case 还能写多个值
        switch (day) {
            case 1 -> System.out.println("星期一");
            case 2 -> System.out.println("星期二");
            case 3 -> System.out.println("星期三");
            case 6, 7 -> System.out.println("周末");
            default -> System.out.println("其他");
        }
        
        // ========== 写法三：switch 表达式（Java 14+） ==========
        // switch 本身产生一个值，可以直接赋给变量
        String dayName = switch (day) {
            case 1 -> "星期一";
            case 2 -> "星期二";
            case 3 -> "星期三";
            default -> "其他";
        };
        System.out.println(dayName);
        
        // 箭头右侧需要多行逻辑时，用 yield 返回值
        int hours = switch (day) {
            case 1, 2, 3, 4, 5 -> {
                System.out.println("工作日");
                yield 8;         // yield 相当于"表达式版的 return"
            }
            default -> {
                System.out.println("休息日");
                yield 0;
            }
        };
        System.out.println("工作 " + hours + " 小时");
    }
}
```

**传统写法的三大陷阱**：忘记 `break` 导致 fall-through（穿透执行后续 case）、各 case 共享变量作用域、漏写 default 导致没有分支命中。**箭头语法一次性解决全部三个问题**，新代码一律推荐箭头写法；传统写法只需认识，用于维护老代码。

switch 支持的类型：`byte`、`short`、`char`、`int` 及其包装类、枚举（Java 5+）、`String`（Java 7+）。不支持 `long`、`float`、`double`、`boolean`。

**switch 模式匹配（Java 21 转正）**

Java 21 把 switch 的模式匹配转正，分支条件从"值相等"扩展到"类型判断"，配合 1.5.9 节的密封类威力更大：

```java
public class SwitchPatternMatching {
    static String describe(Object obj) {
        return switch (obj) {
            case null                 -> "空值";             // 可以直接匹配 null！
            case Integer i when i > 0 -> "正整数: " + i;      // 类型模式 + when 守卫
            case Integer i            -> "非正整数: " + i;
            case String s             -> "字符串，长度 " + s.length();
            case int[] arr            -> "int 数组，长度 " + arr.length;
            default                   -> "其他类型: " + obj.getClass().getSimpleName();
        };
    }

    public static void main(String[] args) {
        System.out.println(describe(42));        // 正整数: 42
        System.out.println(describe(-7));        // 非正整数: -7
        System.out.println(describe("hello"));   // 字符串，长度 5
        System.out.println(describe(null));      // 空值
    }
}
```

传统写法要先 `if (obj instanceof Integer)` 再强制转换 `(Integer) obj`；模式匹配把"判断类型 + 声明变量"合成一步，代码少一半，类型安全不变。

**for 循环：**

```java
public class ForLoopDemo {
    public static void main(String[] args) {
        // 传统 for 循环
        for (int i = 0; i < 5; i++) {
            System.out.println("i = " + i);
        }
        
        // 增强 for 循环（for-each）
        int[] numbers = {1, 2, 3, 4, 5};
        for (int num : numbers) {
            System.out.println("num = " + num);
        }
        
        // 嵌套循环（打印九九乘法表）
        for (int i = 1; i <= 9; i++) {
            for (int j = 1; j <= i; j++) {
                System.out.print(j + "×" + i + "=" + (i * j) + "\t");
            }
            System.out.println();
        }
    }
}
```

**while 和 do-while 循环：**

```java
public class WhileLoopDemo {
    public static void main(String[] args) {
        // while 循环
        int i = 0;
        while (i < 5) {
            System.out.println("while: i = " + i);
            i++;
        }
        
        // do-while 循环（至少执行一次）
        int j = 0;
        do {
            System.out.println("do-while: j = " + j);
            j++;
        } while (j < 5);
        
        // break 和 continue
        for (int k = 0; k < 10; k++) {
            if (k == 5) {
                break;  // 跳出循环
            }
            if (k % 2 == 0) {
                continue;  // 跳过本次循环
            }
            System.out.println("k = " + k);
        }
    }
}
```

**带标签的 break 与 continue：**

普通 `break` 只能跳出**最近一层**循环。嵌套循环里想直接退出外层循环，Java 提供了标签语法——在循环前放一个标签名，`break 标签名` 即可跳出指定循环：

```java
public class LabeledBreakDemo {
    public static void main(String[] args) {
        // 场景：在二维数组中查找目标值，找到后一次性退出两层循环
        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        int target = 5;
        boolean found = false;

        outer:                      // 标签写在循环语句之前
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] == target) {
                    found = true;
                    break outer;    // 直接跳出外层循环
                }
            }
        }
        System.out.println("找到 " + target + "？" + found);

        // 标签 continue：跳过外层循环的本次迭代
        outer2:
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                if (j == i) {
                    continue outer2;   // 对角线元素跳过，直接处理下一行
                }
                System.out.print("(" + i + "," + j + ") ");
            }
        }
        // 输出：(1,2) (1,3) (2,1) (2,3) (3,1) (3,2)
    }
}
```

不用的标签时，通常要引入布尔标志变量并在每层循环里判断，写法更啰嗦。标签在真实项目里不算高频，但语法不难，遇到复杂嵌套循环时很有用。

**for-each 与传统 for 的选择**

前面反复出现的 `for (int num : arr2)` 是增强 for（for-each，Java 5 引入），它只负责"逐个取出元素"，拿不到下标、也不能在遍历中修改结构。选择经验：**只读遍历**用 for-each（最简洁、不会越界）；**需要下标**（如 `arr2[i]` 与 `arr2[i+1]` 比较）、**要替换元素**、**要倒序或跳步**时用传统 for。集合的 for-each 底层是迭代器，这个机制在 1.7.6 节展开。

### 1.4.5 数组

数组是存储相同类型数据的集合。

```java
public class ArrayDemo {
    public static void main(String[] args) {
        // 数组声明和初始化
        int[] arr1 = new int[5];  // 声明长度为 5 的数组
        int[] arr2 = {1, 2, 3, 4, 5};  // 直接初始化
        int[] arr3 = new int[]{1, 2, 3, 4, 5};  // 完整写法
        
        // 访问数组元素
        System.out.println("arr2[0] = " + arr2[0]);  // 1
        System.out.println("arr2.length = " + arr2.length);  // 5
        
        // 修改数组元素
        arr2[0] = 100;
        System.out.println("arr2[0] = " + arr2[0]);  // 100
        
        // 数组遍历
        for (int i = 0; i < arr2.length; i++) {
            System.out.println("arr2[" + i + "] = " + arr2[i]);
        }
        
        // 增强 for 循环遍历
        for (int num : arr2) {
            System.out.println("num = " + num);
        }
        
        // 二维数组
        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
        
        // 数组工具类
        int[] nums = {5, 2, 8, 1, 9};
        java.util.Arrays.sort(nums);  // 排序
        System.out.println("排序后：" + java.util.Arrays.toString(nums));
        
        int index = java.util.Arrays.binarySearch(nums, 8);  // 二分查找
        System.out.println("8 的索引：" + index);
    }
}
```

数组是**定长**的——创建后长度不可变，需要动态扩容时只能创建新数组再复制，或直接改用 1.7 节的集合类。访问越界会抛 `ArrayIndexOutOfBoundsException`。

### 1.4.6 Arrays 工具类完整用法

`java.util.Arrays` 是操作数组的静态工具类，把上面零散的用法汇总成一份完整速查：

```java
import java.util.Arrays;
import java.util.Comparator;

public class ArraysUtilDemo {
    public static void main(String[] args) {
        // ===== 打印 =====
        int[] arr = {5, 2, 8, 1, 9};
        System.out.println(Arrays.toString(arr));      // [5, 2, 8, 1, 9]
        // 直接 System.out.println(arr) 打印的是 "[I@1b6d3586" 这样的哈希串！
        int[][] deep = {{1, 2}, {3, 4}};
        System.out.println(Arrays.deepToString(deep)); // [[1, 2], [3, 4]]

        // ===== 排序 =====
        int[] toSort = {5, 2, 8, 1, 9};
        Arrays.sort(toSort);                          // 基本类型：双轴快速排序
        System.out.println(Arrays.toString(toSort));  // [1, 2, 5, 8, 9]

        Integer[] objs = {5, 2, 8};
        Arrays.sort(objs, Comparator.reverseOrder()); // 对象数组可自定义比较器
        System.out.println(Arrays.toString(objs));    // [8, 5, 2]

        // ===== 查找（必须先排序——二分查找的前提） =====
        int idx = Arrays.binarySearch(toSort, 8);
        System.out.println("索引：" + idx);            // 3

        // ===== 填充与复制 =====
        int[] filled = new int[5];
        Arrays.fill(filled, 7);
        System.out.println(Arrays.toString(filled));  // [7, 7, 7, 7, 7]

        int[] copied = Arrays.copyOf(arr, 8);         // 复制并扩到 8 位，多余补 0
        System.out.println(Arrays.toString(copied));
        int[] part = Arrays.copyOfRange(arr, 1, 4);   // [下标1, 下标4)，左闭右开
        System.out.println(Arrays.toString(part));    // [2, 8, 1]

        // ===== 比较 =====
        int[] a = {1, 2, 3}, b = {1, 2, 3};
        System.out.println(a.equals(b));           // false！数组继承 Object 的 equals，比地址
        System.out.println(Arrays.equals(a, b));   // true，逐元素比较
        System.out.println(Arrays.compare(a, b));  // 0 相等；负数 a 小；正数 a 大

        // ===== 转集合与转流 =====
        System.out.println(Arrays.asList("A", "B", "C"));  // 与 List.of 的区别见 1.7.8
        int sum = Arrays.stream(arr).sum();                // 转成流做聚合运算
        System.out.println("总和：" + sum);                 // 25

        // ===== 批量计算（Java 8+） =====
        Arrays.setAll(toSort, i -> i * 10);    // 按下标生成元素
        System.out.println(Arrays.toString(toSort));  // [0, 10, 20, 30, 40]
    }
}
```

三条高频经验：**打印数组永远用 `Arrays.toString`**；**比较数组内容永远用 `Arrays.equals`**；**`binarySearch` 之前必须先 `sort`**，否则结果不可预期。

### 1.4.7 Math 类

`java.lang.Math` 提供常用数学运算的静态方法，无需 import（java.lang 包自动引入）：

```java
public class MathDemo {
    public static void main(String[] args) {
        // 绝对值、最值
        System.out.println(Math.abs(-5));        // 5
        System.out.println(Math.max(3, 9));      // 9
        System.out.println(Math.min(3, 9));      // 3

        // 幂与开方
        System.out.println(Math.pow(2, 10));     // 1024.0
        System.out.println(Math.sqrt(16));       // 4.0
        System.out.println(Math.cbrt(27));       // 3.0（立方根）

        // 取整三兄弟
        System.out.println(Math.ceil(3.2));      // 4.0（向上取整）
        System.out.println(Math.floor(3.8));     // 3.0（向下取整）
        System.out.println(Math.round(3.5));     // 4（四舍五入）
        System.out.println(Math.round(-3.5));    // -3（注意：向正无穷方向取整）

        // 随机数：[0.0, 1.0) 的 double
        double r = Math.random();
        System.out.println(r);
        // 生成 [1, 100] 的随机整数：先放大 100 倍得到 [0,100)，再 +1
        int dice = (int) (Math.random() * 100) + 1;
        System.out.println("随机数：" + dice);

        // 常量
        System.out.println(Math.PI);
        System.out.println(Math.E);
    }
}
```

两个容易踩的细节：`Math.round(-3.5)` 的结果是 -3 而不是 -4（round 是"加 0.5 再向下取整"，即向正无穷方向靠）；`pow/sqrt` 返回 double，赋给 int 要显式强转。分页计算是 Math 在业务里最典型的应用——"总页数 = ceil(总记录数 / 每页大小)"，写成 `(total + size - 1) / size` 的整数运算版也能达到同样效果，SelfBlog 分页响应里的 `pages` 字段就是它算出来的。

需要更强能力（指定范围、随机布尔值、可复现种子）时用 `java.util.Random` 类；生成安全敏感的随机数（令牌、密钥）用 `java.security.SecureRandom`。SelfBlog 生成 JWT 密钥这类场景就应使用 SecureRandom 而非 Math.random()。

### 1.4.8 Scanner：从控制台读取输入

控制台交互程序用 `java.util.Scanner` 读取输入：

```java
import java.util.Scanner;

public class ScannerDemo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("请输入你的名字：");
        String name = scanner.nextLine();        // 读取一整行

        System.out.print("请输入你的年龄：");
        int age = scanner.nextInt();             // 读取一个整数

        System.out.println("你好，" + name + "，明年你 " + (age + 1) + " 岁");

        // 带校验的读取：先判断再读取，避免输入不匹配直接抛异常
        System.out.print("请输入一个数字：");
        if (scanner.hasNextInt()) {
            int n = scanner.nextInt();
            System.out.println("平方是：" + n * n);
        } else {
            System.out.println("这不是整数！");
        }

        scanner.close();  // 用完关闭
    }
}
```

**`nextInt()` 之后接 `nextLine()` 的经典陷阱：**

```java
int age = scanner.nextInt();      // 只消费了数字，行尾的"回车"还留在缓冲区
String name = scanner.nextLine(); // 读到空字符串（把残留的回车读走了）
```

解决办法：`nextInt()` 之后额外补一句 `scanner.nextLine();` 把残留换行"吃掉"；或者统一用 `nextLine()` 读成字符串再 `Integer.parseInt()` 转换，推荐后者，语义更清晰。

Scanner 学习阶段够用。真实项目（包括 SelfBlog）的输入来自 HTTP 请求体、配置文件、数据库，而不是控制台——Spring Boot 会把请求参数自动绑定到方法参数上，不需要手写解析。

## 1.5 面向对象

写到这里，先建立一个总览：**面向对象（OOP）是把"数据"和"操作这些数据的行为"捆绑成对象来组织代码的思想**。它有三大特性——封装（1.5.2，隐藏细节、保护数据）、继承（1.5.3，复用与扩展）、多态（1.5.4，同一调用不同行为）；抽象类与接口（1.5.5）是定义"契约"的工具，内部类（1.5.6）解决特殊作用域问题，record/sealed（1.5.8/1.5.9）是 Java 16/17 对"纯数据"与"受限继承"的现代化补强。本节所有例子都为理解 SelfBlog 的 `Service → Mapper → Entity` 分层做铺垫：实体类靠封装，Service 接口靠多态（面向接口编程），全局异常处理器靠注解（1.11）。

### 1.5.1 类与对象

类是对象的模板，对象是类的实例。

```java
// 定义一个类
public class Person {
    // 成员变量（属性）
    private String name;
    private int age;
    
    // 构造方法
    public Person() {
        // 无参构造
    }
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // Getter 和 Setter 方法
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        if (age > 0 && age < 150) {
            this.age = age;
        } else {
            System.out.println("年龄不合法");
        }
    }
    
    // 成员方法
    public void sayHello() {
        System.out.println("大家好，我是" + name + "，今年" + age + "岁");
    }
    
    // 重写 toString 方法
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + "}";
    }
}

// 使用类
public class PersonTest {
    public static void main(String[] args) {
        // 创建对象
        Person p1 = new Person();
        p1.setName("张三");
        p1.setAge(25);
        p1.sayHello();
        
        Person p2 = new Person("李四", 30);
        p2.sayHello();
        
        System.out.println(p1);  // 自动调用 toString()
        System.out.println(p2);
    }
}
```

### 1.5.2 封装

封装是面向对象的三大特性之一，它将对象的属性和方法隐藏起来，只对外提供公共的访问方式。

```java
public class EncapsulationDemo {
    public static void main(String[] args) {
        BankAccount account = new BankAccount("123456", 1000.0);
        
        // 不能直接访问私有属性
        // account.balance = 2000.0;  // 编译错误
        
        // 通过公共方法访问
        account.deposit(500.0);
        account.withdraw(200.0);
        System.out.println("余额：" + account.getBalance());
        
        // 尝试非法操作
        account.withdraw(2000.0);  // 余额不足
        account.deposit(-100.0);   // 存款金额不能为负
    }
}

class BankAccount {
    private String accountNumber;  // 私有属性
    private double balance;
    
    public BankAccount(String accountNumber, double balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }
    
    // 公共方法：存款
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            System.out.println("存款成功，存入：" + amount);
        } else {
            System.out.println("存款金额必须大于 0");
        }
    }
    
    // 公共方法：取款
    public void withdraw(double amount) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
            System.out.println("取款成功，取出：" + amount);
        } else {
            System.out.println("余额不足或金额不合法");
        }
    }
    
    // 公共方法：查询余额
    public double getBalance() {
        return balance;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
}
```

**为什么封装值得多写这些代码？**

上面 BankAccount 比直接暴露 `public double balance` 多写了不少行，但换来三个实打实的好处：

1. **不变量保护**：余额永远不为负、存取金额必须合法——这些"对象始终成立的规则"只在类内的方法里实现一次，所有调用方无法绕过。如果 balance 是 public 的，任何一行 `account.balance = -999` 都能破坏规则，且你永远找不到是谁改的
2. **内部实现可自由更换**：哪天要把 double 换成 BigDecimal（1.4.1 提过金额不该用浮点数），只要 getter 还返回数值，所有调用方一行都不用改
3. **可控的访问面**：只读字段只给 getter 不给 setter，写操作集中在少数方法上，排查"谁改了这个值"就有迹可循

JavaBean 约定（getter/setter 命名）不只是风格——Spring 的依赖注入、Jackson 的 JSON 序列化、MyBatis 的结果映射都按这套命名**通过反射**（1.11.2 节）找属性。SelfBlog 实体类的 `getId()/setId()` 之所以"看起来多余却必须存在"，就是这个原因。

### 1.5.3 继承

继承是面向对象的另一个重要特性，它允许一个类继承另一个类的属性和方法。

```java
// 父类
public class Animal {
    protected String name;
    protected int age;
    
    public Animal(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public void eat() {
        System.out.println(name + "正在吃东西");
    }
    
    public void sleep() {
        System.out.println(name + "正在睡觉");
    }
    
    public void makeSound() {
        System.out.println(name + "发出声音");
    }
}

// 子类
public class Dog extends Animal {
    private String breed;
    
    public Dog(String name, int age, String breed) {
        super(name, age);  // 调用父类构造方法
        this.breed = breed;
    }
    
    // 重写父类方法
    @Override
    public void makeSound() {
        System.out.println(name + "汪汪叫");
    }
    
    // 子类特有方法
    public void fetch() {
        System.out.println(name + "正在捡球");
    }
}

// 子类
public class Cat extends Animal {
    private boolean isIndoor;
    
    public Cat(String name, int age, boolean isIndoor) {
        super(name, age);
        this.isIndoor = isIndoor;
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + "喵喵叫");
    }
    
    public void climb() {
        System.out.println(name + "正在爬树");
    }
}

// 测试类
public class InheritanceTest {
    public static void main(String[] args) {
        Dog dog = new Dog("旺财", 3, "金毛");
        dog.eat();        // 继承自父类
        dog.sleep();      // 继承自父类
        dog.makeSound();  // 重写的方法
        dog.fetch();      // 子类特有方法
        
        Cat cat = new Cat("咪咪", 2, true);
        cat.eat();
        cat.sleep();
        cat.makeSound();
        cat.climb();
        
        // 多态
        Animal animal1 = new Dog("来福", 2, "哈士奇");
        Animal animal2 = new Cat("小白", 1, false);
        
        animal1.makeSound();  // 来福汪汪叫
        animal2.makeSound();  // 小白喵喵叫
    }
}
```

**继承的几条铁律**（每条都对应一个编译错误或运行时陷阱）：

1. **Java 单继承**：一个类只能 `extends` 一个父类；多能力的组合用接口（1.5.5）实现
2. **构造顺序**：子类构造器第一行隐式或显式调用 `super(...)`——先有父类部分，再有子类部分。父类若只有带参构造器而没有无参构造器，子类构造器**必须**显式写 `super(...)`
3. **`@Override` 必写**：它让编译器替你检查"确实重写了父类方法"——方法名拼错、参数不同时立刻报错，否则就成了一个意外的新方法
4. **访问权限不能缩小**：父类 `public` 方法重写后不能变 `protected`
5. **组合优于继承**：继承是"is-a"（狗是一种动物）才用；"has-a"（汽车有引擎）用组合（把引擎作为字段）。滥用继承会让子类背上父类全部包袱，这是《Effective Java》反复强调的设计原则

SelfBlog 的 Service 层能体现第 5 条：`BlogPostServiceImpl` 继承 MyBatis Plus 的 `ServiceImpl` 获得 CRUD 能力，同时把查询/交互职责**委托**给独立的 `PostQueryService`/`PostInteractionService`（组合），而不是把所有逻辑层层继承下去。

### 1.5.4 多态

多态是指同一个方法调用，由于对象不同而表现出不同的行为。

```java
public class PolymorphismDemo {
    public static void main(String[] args) {
        // 向上转型（自动类型转换）
        Shape circle = new Circle(5);
        Shape rectangle = new Rectangle(4, 6);
        
        // 同一个方法调用，不同对象表现出不同行为
        System.out.println("圆面积：" + circle.getArea());
        System.out.println("矩形面积：" + rectangle.getArea());
        
        // 多态数组
        Shape[] shapes = new Shape[3];
        shapes[0] = new Circle(3);
        shapes[1] = new Rectangle(4, 5);
        shapes[2] = new Triangle(3, 4);
        
        for (Shape shape : shapes) {
            System.out.println(shape.getClass().getSimpleName() + " 面积：" + shape.getArea());
        }
    }
}

// 抽象类
abstract class Shape {
    public abstract double getArea();
    
    public void printArea() {
        System.out.println("面积是：" + getArea());
    }
}

class Circle extends Shape {
    private double radius;
    
    public Circle(double radius) {
        this.radius = radius;
    }
    
    @Override
    public double getArea() {
        return Math.PI * radius * radius;
    }
}

class Rectangle extends Shape {
    private double width;
    private double height;
    
    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }
    
    @Override
    public double getArea() {
        return width * height;
    }
}

class Triangle extends Shape {
    private double base;
    private double height;
    
    public Triangle(double base, double height) {
        this.base = base;
        this.height = height;
    }
    
    @Override
    public double getArea() {
        return 0.5 * base * height;
    }
}
```

**多态到底"多"在哪？**

逐层拆解上面这段代码发生的两件事：

1. **向上转型（自动）**：`shapes[0] = new Circle(5)` 把子类对象赋给父类引用——"圆形是一种形状"，语义自然，永远安全
2. **动态绑定**：`shape.getArea()` 调的是哪个方法，不看变量声明的类型 Shape，而看**运行时对象的真实类型**——这就是多态的核心。同一个调用语句，数组里装 Circle 就算圆面积，装 Triangle 就算三角形面积

它解决的工程问题是**消除 if-else 类型判断**：没有多态时你得写 `if (shape instanceof Circle) ... else if (shape instanceof Rectangle) ...`，每加一种图形全部判断处都要改；有了多态，新增图形只需新增一个子类，主流程一行不动——这就是"对扩展开放、对修改关闭"的开闭原则最朴素的形态。SelfBlog 里"不同类型的内容（文章/相册/页面）各自实现渲染逻辑"用的正是这个思路。

**instanceof 模式匹配（Java 16+）**：确实需要判断类型时，新写法把"判断 + 强转"合成一步：

```java
if (shape instanceof Circle c) {
    // 这里 c 已经是 Circle 类型，直接用，无需 (Circle) shape 强转
    System.out.println("半径：" + c.radius);
}
```

### 1.5.5 抽象类与接口

**抽象类**：

```java
// 抽象类
public abstract class Employee {
    protected String name;
    protected String id;
    protected double baseSalary;
    
    public Employee(String name, String id, double baseSalary) {
        this.name = name;
        this.id = id;
        this.baseSalary = baseSalary;
    }
    
    // 抽象方法（没有方法体）
    public abstract double calculateSalary();
    
    // 具体方法
    public void showInfo() {
        System.out.println("姓名：" + name);
        System.out.println("工号：" + id);
        System.out.println("基本工资：" + baseSalary);
        System.out.println("实发工资：" + calculateSalary());
    }
}

// 正式员工
public class FullTimeEmployee extends Employee {
    private double bonus;
    
    public FullTimeEmployee(String name, String id, double baseSalary, double bonus) {
        super(name, id, baseSalary);
        this.bonus = bonus;
    }
    
    @Override
    public double calculateSalary() {
        return baseSalary + bonus;
    }
}

// 兼职员工
public class PartTimeEmployee extends Employee {
    private int hours;
    private double hourlyRate;
    
    public PartTimeEmployee(String name, String id, double baseSalary, int hours, double hourlyRate) {
        super(name, id, baseSalary);
        this.hours = hours;
        this.hourlyRate = hourlyRate;
    }
    
    @Override
    public double calculateSalary() {
        return baseSalary + hours * hourlyRate;
    }
}
```

**接口**：

```java
// 接口定义
public interface Flyable {
    // 常量（默认 public static final）
    int MAX_HEIGHT = 10000;
    
    // 抽象方法（默认 public abstract）
    void fly();
    
    // 默认方法（Java 8+）
    default void land() {
        System.out.println("安全着陆");
    }
    
    // 静态方法（Java 8+）
    static void checkWeather() {
        System.out.println("检查天气状况");
    }
}

// 另一个接口
public interface Swimmable {
    void swim();
}

// 实现接口
public class Bird implements Flyable {
    private String name;
    
    public Bird(String name) {
        this.name = name;
    }
    
    @Override
    public void fly() {
        System.out.println(name + "在天上飞翔");
    }
}

// 实现多个接口
public class Duck implements Flyable, Swimmable {
    private String name;
    
    public Duck(String name) {
        this.name = name;
    }
    
    @Override
    public void fly() {
        System.out.println(name + "在低空飞行");
    }
    
    @Override
    public void swim() {
        System.out.println(name + "在水中游泳");
    }
}

// 测试
public class InterfaceTest {
    public static void main(String[] args) {
        Bird bird = new Bird("老鹰");
        bird.fly();
        bird.land();  // 调用默认方法
        
        Duck duck = new Duck("鸭子");
        duck.fly();
        duck.swim();
        
        // 接口的多态
        Flyable flyable = new Bird("鸽子");
        flyable.fly();
        
        // 调用静态方法
        Flyable.checkWeather();
    }
}
```

**抽象类 vs 接口，怎么选**：

| | 抽象类 | 接口 |
|---|---|---|
| 关系语义 | "是什么"（is-a） | "能做什么"（can-do） |
| 字段 | 任意成员变量 | 只能 `public static final` 常量 |
| 构造器 | 有 | 无 |
| 方法实现 | 任意 | default/static（Java 8+） |
| 数量限制 | 单继承 | 一类可实现多个接口 |

经验法则：**共享状态和模板代码 → 抽象类**（Employee 把 name/id/salary 与 showInfo() 模板沉到基类）；**纯粹的行为契约 → 接口**（Flyable 不关心实现者是谁）。Java 单继承决定了"契约族谱"必须用接口铺开——这正是 Spring 大量面向接口编程的原因：`BlogPostService` 是接口、`BlogPostServiceImpl` 是实现，Controller 依赖接口而非实现类，测试时才能用 Mockito 换一个实现（第二章详讲）。

### 1.5.6 内部类

内部类是定义在另一个类内部的类。

```java
public class OuterClass {
    private String outerField = "外部类字段";
    private static String staticOuterField = "静态外部类字段";
    
    // 成员内部类
    public class InnerClass {
        public void display() {
            System.out.println("访问外部类字段：" + outerField);
        }
    }
    
    // 静态内部类
    public static class StaticInnerClass {
        public void display() {
            System.out.println("访问静态外部类字段：" + staticOuterField);
            // 不能直接访问非静态外部类字段
        }
    }
    
    // 局部内部类
    public void method() {
        String localField = "局部变量";
        
        class LocalInnerClass {
            public void display() {
                System.out.println("访问局部变量：" + localField);
            }
        }
        
        LocalInnerClass inner = new LocalInnerClass();
        inner.display();
    }
    
    // 匿名内部类
    public void anonymousClass() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("匿名内部类执行");
            }
        };
        runnable.run();
    }
}

// 测试
public class InnerClassTest {
    public static void main(String[] args) {
        // 创建成员内部类实例
        OuterClass outer = new OuterClass();
        OuterClass.InnerClass inner = outer.new InnerClass();
        inner.display();
        
        // 创建静态内部类实例
        OuterClass.StaticInnerClass staticInner = new OuterClass.StaticInnerClass();
        staticInner.display();
        
        // 调用包含局部内部类的方法
        outer.method();
        
        // 调用包含匿名内部类的方法
        outer.anonymousClass();
    }
}
```

### 1.5.7 Object 类核心方法：equals、hashCode 与 toString

Java 里所有类都隐式继承 `Object`。它定义的三个方法几乎是面试和实操的高频考点。

**toString()：对象的自我介绍**

`System.out.println(obj)`、字符串拼接 `"" + obj` 都会自动调用 `toString()`。不重写时输出 `类名@十六进制哈希`（如 `Person@1b6d3586`），毫无信息量。重写规范：**简短、包含关键业务字段、格式统一**（1.5.1 的 Person 已经示范）。IDEA 里 `Alt+Insert` → toString() 可自动生成；Lombok 的 `@ToString` 等价。

有一个安全细节值得从一开始就养成习惯：**toString 里不要放进敏感字段**。密码、token、身份证号一旦进了 toString，它就会随日志、异常消息、调试输出四处扩散——SelfBlog 的用户实体 toString 里只有 id 和 username，password 字段即使参与序列化也不会出现在可读输出里。

**equals() 与 hashCode()：必须成对重写**

`==` 比较引用地址；`equals()` 语义上是"内容相等"。不重写时 `equals` 就是 `==`。重写必须遵守契约：

- **equals 对称、传递、一致**：`a.equals(b)` 与 `b.equals(a)` 结果相同
- **重写 equals 必须同时重写 hashCode**：契约要求"equals 相等的两个对象，hashCode 必须相同"。否则把对象放进 `HashSet`/`HashMap` 后行为错乱——两个"相等"的对象因 hash 不同被放进不同的桶，`contains()` 查不到、`remove()` 删不掉

```java
import java.util.Objects;

public class User {
    private final Long id;
    private String username;

    public User(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;              // 1. 自反捷径
        if (o == null || getClass() != o.getClass()) return false;  // 2. 类型检查
        User user = (User) o;                    // 3. 强转
        return Objects.equals(id, user.id)       // 4. 逐字段比较，用 Objects.equals 防 NPE
                && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);       // 与 equals 用同一批字段
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
}
```

```java
import java.util.HashSet;
import java.util.Set;

public class EqualsHashCodeDemo {
    public static void main(String[] args) {
        User u1 = new User(1L, "admin");
        User u2 = new User(1L, "admin");
        System.out.println(u1 == u2);          // false：不同对象
        System.out.println(u1.equals(u2));     // true：内容相等

        Set<User> users = new HashSet<>();
        users.add(u1);
        users.add(u2);
        System.out.println(users.size());      // 1（hashCode 相同 + equals 相等 → 去重）
        // 若只重写 equals 不重写 hashCode，这里会得到 2！
    }
}
```

**与 SelfBlog 的联系**：Map 键、Set 去重、MyBatis Plus 实体比较都依赖这套契约。实际项目更常用的做法是让 IDE 生成，或用 Lombok 的 `@EqualsAndHashCode`，其原理与上面手写完全一致。

### 1.5.8 record 类：一行定义不可变数据类（Java 16+）

上面 User 类写了 30 行，其中 25 行是模板代码。Java 16 的 record 用一行完成同样的事：

```java
// 声明：括号里是"组件列表"（相当于 final 字段）
public record PostSummary(Long id, String title, int views) {}
```

编译器自动为你生成：

- 每个**组件**一个 `private final` 字段（整个类隐式 final，不可继承）
- **访问器**（不是 getName 而是 `title()` 这种与字段同名的形式）
- **全参构造器**
- 基于全部组件的 **equals() / hashCode() / toString()**——1.5.7 的契约自动满足

```java
public class RecordDemo {
    public static void main(String[] args) {
        PostSummary post = new PostSummary(1L, "Java 基础", 1200);

        System.out.println(post.id());       // 1（访问器无 get 前缀）
        System.out.println(post.title());    // Java 基础
        System.out.println(post);            // PostSummary[id=1, title=Java 基础, views=1200]

        // record 是不可变的：没有 setter
        // post.views(999);                  // 编译错误

        // 紧凑构造器：校验参数的推荐位置
        // record Version(int major, int minor) {
        //     Version {                       // 紧凑构造器：不带参数列表
        //         if (major < 0) throw new IllegalArgumentException("major < 0");
        //     }
        // }

        // 解构（Java 21 记录模式）：直接拆出字段
        if (post instanceof PostSummary(Long id, String title, int views)) {
            System.out.println("id=" + id + ", views=" + views);
        }
    }
}
```

**record 适合什么**：DTO（接口传输对象）、值对象（坐标、金额区间）、多返回值、Map 的复合键——凡"纯数据载体、创建后不变"的场景。**不适合**：需要可变状态的实体（SelfBlog 的 `BlogPost` 实体要被 MyBatis Plus 回填自增主键、软删除字段要更新，所以是带 setter 的普通类）；JPA/MyBatis 实体与 record 的配合有诸多限制，别急着替换。

### 1.5.9 密封类 sealed：受控的继承树（Java 17+）

`final` 拒绝一切继承，普通类放任一切继承——sealed 位于两者之间：**只允许指定的子类**：

```java
// 父类：permits 列出全部合法子类
public sealed interface Shape permits Circle, Rectangle, Triangle {}

// 子类三选一：final（不许再继承）/ sealed（继续限类）/ non-sealed（重新放开）
record Circle(double radius) implements Shape {}
record Rectangle(double w, double h) implements Shape {}
non-sealed class FreeShape implements Shape {}   // 放开后任何人可继承

public class SealedDemo {
    // sealed + switch 模式匹配 = 穷尽性检查（exhaustiveness）
    static double area(Shape shape) {
        // 编译器知道 Shape 只有这三种实现：
        // 少写任何一个分支 → 编译错误（而不是运行时漏处理！）
        return switch (shape) {
            case Circle c       -> Math.PI * c.radius() * c.radius();
            case Rectangle(double w, double h) -> w * h;   // 记录模式直接解构
            case Triangle t     -> 0;   // 示例中省略实现
            case FreeShape f    -> 0;
        };
    }

    public static void main(String[] args) {
        System.out.println(area(new Circle(1)));         // 3.14159...
        System.out.println(area(new Rectangle(3, 4)));   // 12.0
    }
}
```

sealed 的价值在于**把"类型系统的完整性"交给编译器**：新增一个实现类时，所有 switch 该父类的地方立刻编译报错提醒你补分支。配合 record 和 switch 模式匹配（1.4.4 节），这三者构成 Java 21 处理"有限状态/消息类型"的现代组合拳。

## 1.6 常用类

### 1.6.1 String 类

String 类是 Java 中最常用的类之一，用于表示字符串。

```java
public class StringDemo {
    public static void main(String[] args) {
        // 创建字符串
        String s1 = "Hello";
        String s2 = new String("World");
        String s3 = new String(new char[]{'J', 'a', 'v', 'a'});
        
        // 字符串长度
        System.out.println("s1 长度：" + s1.length());
        
        // 字符串连接
        String s4 = s1 + " " + s2;
        String s5 = s1.concat(" ").concat(s2);
        System.out.println(s4);
        System.out.println(s5);
        
        // 字符串比较
        System.out.println("s1.equals(s2): " + s1.equals(s2));
        System.out.println("s1.equalsIgnoreCase(s2): " + s1.equalsIgnoreCase(s2));
        System.out.println("s1.compareTo(s2): " + s1.compareTo(s2));
        
        // 字符串查找
        String str = "Hello, World!";
        System.out.println("包含 'World': " + str.contains("World"));
        System.out.println("索引 'World': " + str.indexOf("World"));
        System.out.println("最后索引 'o': " + str.lastIndexOf("o"));
        System.out.println("以 'Hello' 开头: " + str.startsWith("Hello"));
        System.out.println("以 '!' 结尾: " + str.endsWith("!"));
        
        // 字符串截取
        System.out.println("substring(7): " + str.substring(7));
        System.out.println("substring(0, 5): " + str.substring(0, 5));
        
        // 字符串替换
        System.out.println("replace: " + str.replace("World", "Java"));
        
        // 字符串分割
        String[] parts = str.split(", ");
        for (String part : parts) {
            System.out.println("part: " + part);
        }
        
        // 大小写转换
        System.out.println("toUpperCase: " + str.toUpperCase());
        System.out.println("toLowerCase: " + str.toLowerCase());
        
        // 去除空白
        String s6 = "  Hello World  ";
        System.out.println("trim: '" + s6.trim() + "'");
        
        // 字符串判断
        System.out.println("isEmpty: " + "".isEmpty());
        System.out.println("isBlank: " + "  ".isBlank());
        
        // 字符串格式化
        String formatted = String.format("姓名：%s，年龄：%d", "张三", 25);
        System.out.println(formatted);
        
        // 字符串构建器
        StringBuilder sb = new StringBuilder();
        sb.append("Hello");
        sb.append(" ");
        sb.append("World");
        sb.insert(5, ",");
        sb.delete(5, 6);
        sb.reverse();
        System.out.println("StringBuilder: " + sb.toString());
    }
}
```

**String 的不可变性原理**

String 是不可变类（immutable class）的教科书实现，看它的三个关键字段：

```java
public final class String {          // final：不能被继承，防止子类破坏不可变性
    private final char[] value;      // final：引用不可重新指向（JDK 9+ 已改为 byte[]）
    // ...所有方法都不修改 value，需要"修改"时一律返回新 String
}
```

不可变带来三个好处：

1. **线程安全**：多线程共享字符串无需加锁
2. **可以缓存 hashCode**：String 的 hash 字段第一次计算后就缓存起来，所以 String 特别适合做 HashMap 的 key（1.7.4 节的 Map 以 String 为键，正是依赖这一点）
3. **字符串常量池得以实现**：见下文

动手验证不可变：`str.replace("World", "Java")` 之后打印 `str` 本身，你会发现它还是 `"Hello, World!"`——replace 返回了一个**新** String，原对象纹丝未动。初学者最常犯的错就是忘接返回值：`str.trim(); str.toUpperCase();` 连写两行，最后发现 str 什么都没变。所有"修改"字符串的 API（replace/trim/toUpperCase/concat）都是这个套路：返回新对象。

**自研不可变类的四步配方**（String 就是这么做的，record 自动做到）：类加 final；字段全部 private final；不提供 setter；"修改"方法一律返回新实例。写配置类、缓存键时套用这个配方，能省掉一整类并发 bug。

**字符串常量池**

JVM 在堆里维护一个字符串常量池（String Pool），相同的字面量字符串全局只存一份：

```java
public class StringPoolDemo {
    public static void main(String[] args) {
        String a = "hello";            // 字面量 → 查常量池，没有就放入池中
        String b = "hello";            // 常量池里已有 → 直接复用
        System.out.println(a == b);    // true：同一个对象

        String c = new String("hello"); // new 强制在堆上创建新对象
        System.out.println(a == c);     // false：c 是堆上的另一个对象
        System.out.println(a.equals(c));// true：内容相同

        // 经典面试题：编译期可确定 → 池化；含变量 → 运行时拼接 → 新对象
        String d = "he" + "llo";        // 编译期折叠为 "hello"，指向常量池
        System.out.println(a == d);     // true
        String e = "he";
        String f = e + "llo";           // e 是变量，运行时拼接，产生新对象
        System.out.println(a == f);     // false
    }
}
```

记住结论：**比较字符串内容永远用 `equals()`，`==` 只在确认比较常量池引用时才有意义**。同理，`Integer`、`Long` 等包装类之间的 `==` 也有缓存陷阱，见 1.6.3 节。

常量池的实用推论：**字面量字符串不用担心重复创建**——一篇文章的标签 `"Java"` 在代码里出现一百次，堆里也只有一个对象。真正要警惕的是循环拼接（1.6.2 详述）和 `new String(...)`（没有特殊理由不要用）。另外 `String.intern()` 可以把运行时产生的字符串手动放入常量池复用，但日常业务代码很少需要它。

**文本块（Text Blocks，Java 15+）**

多行字符串以前要靠 `\n` 和 `+` 拼接，Java 15 的文本块用三重引号原样保留多行内容：

```java
public class TextBlockDemo {
    public static void main(String[] args) {
        // 传统写法：难读、难维护
        String json1 = "{\n" +
                "  \"name\": \"SelfBlog\",\n" +
                "  \"version\": 2\n" +
                "}";

        // 文本块：所见即所得，缩进以最后闭合引号的位置为基准
        String json2 = """
                {
                  "name": "SelfBlog",
                  "version": 2
                }
                """;
        System.out.println(json1);
        System.out.println(json2);

        // 支持内嵌转义：\s 保留尾部空格，\ 行尾续行
        String query = """
                SELECT id, title, views \
                FROM blog_post \
                WHERE is_deleted = 0
                """;
        System.out.println(query);

        // 常用方法：formatted() 相当于 String.format 的实例版
        String html = """
                <h1>%s</h1>
                <p>%s</p>
                """.formatted("标题", "正文");
        System.out.println(html);
    }
}
```

文本块配合 `.formatted()` 生成 JSON、SQL、HTML 模板非常顺手——SelfBlog 后端拼 JSON-LD 结构化数据时就用得上这种写法。

### 1.6.2 StringBuilder 与 StringBuffer

StringBuilder 和 StringBuffer 用于创建可变的字符串。

```java
public class StringBuilderDemo {
    public static void main(String[] args) {
        // StringBuilder（线程不安全，性能高）
        StringBuilder sb = new StringBuilder();
        sb.append("Hello");
        sb.append(" ");
        sb.append("World");
        sb.insert(5, ",");
        sb.replace(0, 5, "Hi");
        sb.delete(0, 3);
        sb.reverse();
        System.out.println("StringBuilder: " + sb.toString());
        
        // StringBuffer（线程安全，性能较低）
        StringBuffer sbf = new StringBuffer();
        sbf.append("Hello");
        sbf.append(" ");
        sbf.append("World");
        System.out.println("StringBuffer: " + sbf.toString());
        
        // 性能对比
        long start = System.currentTimeMillis();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb2.append(i);
        }
        long end = System.currentTimeMillis();
        System.out.println("StringBuilder 耗时：" + (end - start) + "ms");
        
        start = System.currentTimeMillis();
        StringBuffer sbf2 = new StringBuffer();
        for (int i = 0; i < 100000; i++) {
            sbf2.append(i);
        }
        end = System.currentTimeMillis();
        System.out.println("StringBuffer 耗时：" + (end - start) + "ms");
    }
}
```

**String 的不可变性**是理解上面性能对比的前提：`String` 对象一旦创建不可修改，任何「修改」（concat、replace、+）都是创建新对象。循环里用 `+` 拼接，每次循环都产生一个新 String + 复制全部旧内容——10 万次循环就是 O(n²) 的灾难。`StringBuilder` 内部维护可扩容的字符数组，追加是 O(1) 均摊。

两者的 API 完全一致，唯一区别是 `StringBuffer` 的所有方法都加了 `synchronized`（线程安全）而 `StringBuilder` 没有。所以对比代码里 StringBuffer 略慢；而"线程安全"在拼接场景下几乎用不到——**单线程环境里 StringBuffer 的加锁纯属开销**。

**实用口诀**：

- 单线程拼字符串（99% 的场景）→ `StringBuilder`
- 多线程共享拼装 → `StringBuffer`（实际业务极少见）
- 少量固定拼接 → 直接 `+`（编译器会自动优化成 StringBuilder）
- 2026 年的现代写法：JDK 21 里字符串模板类特性仍在预览（JEP 459），暂用 StringBuilder 即可

> **与 SelfBlog 的联系**：`ReadingHtmlRenderer`（SEO 的 HTML 组装）这类「把几十个片段拼成页面」的代码，内部全是 StringBuilder——逐段 append 标题、正文、JSON-LD。用 `+` 拼会先在常量池堆出一堆短命对象。

### 1.6.3 包装类

包装类是将基本数据类型封装成对象的类。

```java
public class WrapperDemo {
    public static void main(String[] args) {
        // 自动装箱（Autoboxing）：基本类型 → 包装类
        Integer i1 = 100;  // 等价于 Integer i1 = Integer.valueOf(100);
        
        // 自动拆箱（Unboxing）：包装类 → 基本类型
        int i2 = i1;  // 等价于 int i2 = i1.intValue();
        
        // 包装类的常用方法
        System.out.println("Integer.MAX_VALUE: " + Integer.MAX_VALUE);
        System.out.println("Integer.MIN_VALUE: " + Integer.MIN_VALUE);
        
        // 类型转换
        String str = "123";
        int num = Integer.parseInt(str);
        System.out.println("parseInt: " + num);
        
        String doubleStr = "3.14";
        double d = Double.parseDouble(doubleStr);
        System.out.println("parseDouble: " + d);
        
        // 基本类型转字符串
        String s1 = String.valueOf(123);
        String s2 = Integer.toString(123);
        System.out.println("valueOf: " + s1);
        System.out.println("toString: " + s2);
        
        // 进制转换
        System.out.println("toBinaryString: " + Integer.toBinaryString(10));
        System.out.println("toHexString: " + Integer.toHexString(255));
        System.out.println("toOctalString: " + Integer.toOctalString(8));
        
        // 包装类的缓存
        Integer a = 127;
        Integer b = 127;
        System.out.println("a == b (127): " + (a == b));  // true（缓存范围 -128~127）
        
        Integer c = 128;
        Integer d2 = 128;
        System.out.println("c == d2 (128): " + (c == d2));  // false（超出缓存范围）
        
        // 所有包装类
        System.out.println("Byte: " + Byte.SIZE + " bits");
        System.out.println("Short: " + Short.SIZE + " bits");
        System.out.println("Integer: " + Integer.SIZE + " bits");
        System.out.println("Long: " + Long.SIZE + " bits");
        System.out.println("Float: " + Float.SIZE + " bits");
        System.out.println("Double: " + Double.SIZE + " bits");
        System.out.println("Character: " + Character.SIZE + " bits");
    }
}
```

**包装类存在的三个理由**：

1. **泛型要求引用类型**：`List<int>` 不合法，必须写 `List<Integer>`（1.7.5 节）；集合、`Optional<Integer>` 都是如此
2. **null 表达"无值"**：数据库字段允许 NULL 时，实体字段用 `Integer` 才能区分"0"和"没填"。SelfBlog 实体里 `views`（不该为空）用 `Integer` 配合默认值，而允许为空的关联 ID 都用包装类型——用 `int` 接 NULL 值会直接抛 NPE
3. **携带工具方法**：`parseInt`、`toBinaryString`、`MAX_VALUE` 都挂在包装类上

**缓存陷阱**务必记牢：`Integer.valueOf` 对 -128~127 返回缓存的同一个对象，之外的值每次新建。所以两个 `Integer` 比较内容**永远用 `equals()`**，`==` 只对 127 以内碰巧为 true——这是生产事故的经典来源（配置里写 200、测试环境写 100，一个环境通过一个环境失败）。自动拆箱还有一个隐藏 NPE：`Integer x = null; int y = x;` 编译通过、运行时炸。

### 1.6.4 日期时间 API

Java 8 引入了新的日期时间 API（java.time 包）。

```java
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateTimeDemo {
    public static void main(String[] args) {
        // LocalDate：日期
        LocalDate today = LocalDate.now();
        System.out.println("今天：" + today);
        
        LocalDate birthday = LocalDate.of(2000, 1, 1);
        System.out.println("生日：" + birthday);
        
        System.out.println("年：" + today.getYear());
        System.out.println("月：" + today.getMonthValue());
        System.out.println("日：" + today.getDayOfMonth());
        System.out.println("星期：" + today.getDayOfWeek());
        System.out.println("一年中的第几天：" + today.getDayOfYear());
        
        // LocalTime：时间
        LocalTime now = LocalTime.now();
        System.out.println("现在时间：" + now);
        
        LocalTime time = LocalTime.of(14, 30, 0);
        System.out.println("指定时间：" + time);
        
        System.out.println("时：" + now.getHour());
        System.out.println("分：" + now.getMinute());
        System.out.println("秒：" + now.getSecond());
        
        // LocalDateTime：日期时间
        LocalDateTime dateTime = LocalDateTime.now();
        System.out.println("现在日期时间：" + dateTime);
        
        LocalDateTime specificDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        System.out.println("指定日期时间：" + specificDateTime);
        
        // 日期时间格式化
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatted = dateTime.format(formatter);
        System.out.println("格式化：" + formatted);
        
        // 解析字符串为日期时间
        LocalDateTime parsed = LocalDateTime.parse("2024-01-01 12:00:00", formatter);
        System.out.println("解析：" + parsed);
        
        // 日期时间计算
        LocalDate nextWeek = today.plusWeeks(1);
        LocalDate lastMonth = today.minusMonths(1);
        System.out.println("下周：" + nextWeek);
        System.out.println("上月：" + lastMonth);
        
        // 日期时间差
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        long days = ChronoUnit.DAYS.between(start, end);
        System.out.println("天数差：" + days);
        
        // Instant：时间戳
        Instant instant = Instant.now();
        System.out.println("时间戳：" + instant);
        System.out.println("毫秒：" + instant.toEpochMilli());
        
        // Duration：时间间隔
        LocalTime start1 = LocalTime.of(9, 0);
        LocalTime end1 = LocalTime.of(17, 30);
        Duration duration = Duration.between(start1, end1);
        System.out.println("工作时长：" + duration.toHours() + "小时");
        
        // Period：日期间隔
        LocalDate start2 = LocalDate.of(2020, 1, 1);
        LocalDate end2 = LocalDate.of(2024, 1, 1);
        Period period = Period.between(start2, end2);
        System.out.println("间隔：" + period.getYears() + "年" + period.getMonths() + "月" + period.getDays() + "天");
    }
}
```

**为什么弃用 Date、拥抱 java.time？**

| 旧 API（java.util.Date/Calendar） | 新 API（java.time） |
|-----------------------------------|---------------------|
| 可变对象（线程不安全） | 不可变对象（天然线程安全，所有 plus/minus 返回新对象） |
| 月份从 0 开始（1 月 = 0） | 月份就是 1–12 |
| API 混乱（Calendar.add/set 风格） | 语义化 API（plusDays/minusMonths） |
| 无时区区分 | LocalDate/LocalDateTime/ZonedDateTime 职责分明 |

> **与 SelfBlog 的联系**：项目实体的 `createTime/updateTime` 全部用 `LocalDateTime`（MyBatis Plus `MetaObjectHandler` 自动填充 `LocalDateTime.now()`）；前端 JSON 交互格式统一 `yyyy-MM-dd HH:mm:ss`（`application.yml` 里 `spring.jackson.date-format` 配置）。如果看到 `new Date()` 老写法的教程，那是 JDK 8 之前的知识，直接跳过。

**三组"计算时间差"的 API 别混用**：`Duration` 量"时长"（两个 LocalTime/Instant 之间，精确到纳秒）；`Period` 量"日历差"（两个 LocalDate 之间，按年月日拆分）；`ChronoUnit.X.between(a, b)` 两者通吃、直接给出单个单位的数值。经验：算"这期教程看了多久"用 Duration，算"作者写博客几年了"用 Period，算"距发布过了多少天"用 ChronoUnit.DAYS。

**易错点**：`LocalDateTime.parse("2024-01-01")` 不带 formatter 只能解析 ISO 格式（`yyyy-MM-dd`）；自定义格式必须显式传 `DateTimeFormatter`，否则 `DateTimeParseException`。另一个坑是 `DateTimeFormatter` 的 pattern 字母大小写敏感：`mm` 是分钟、`MM` 才是月份，`hh` 是 12 小时制、`HH` 是 24 小时制——格式串写错不会报错，只是输出悄悄变得莫名其妙。

## 1.7 集合框架

### 1.7.1 集合框架概述

Java 集合框架是一套用于存储和操作对象组的类和接口。数组的局限你已经体会过（1.4.5）：**长度固定**、只能放同一类型、没有现成的查找/插入/删除方法。集合（Collection）补齐了这一切——动态扩容、丰富的 API、针对不同场景优化过的数据结构。它与泛型（1.7.5）是一对搭档：集合负责"装"，泛型负责"装什么"。

```
Collection
├── List（有序，可重复）
│   ├── ArrayList
│   ├── LinkedList
│   └── Vector
├── Set（无序，不可重复）
│   ├── HashSet
│   ├── LinkedHashSet
│   └── TreeSet
└── Queue（队列）
    ├── PriorityQueue
    └── Deque

Map（键值对）
├── HashMap
├── LinkedHashMap
├── TreeMap
└── Hashtable
```

**选型速查表**（初学者最需要的判断力）：

| 需求 | 选择 | 理由 |
|------|------|------|
| 有序列表、按下标访问、尾部增删 | ArrayList | 底层数组，随机访问 O(1)，90% 场景的正确答案 |
| 头尾频繁增删 | ArrayDeque / LinkedList | 链表/循环数组头部操作 O(1) |
| 去重、无序 | HashSet | 哈希表，add/contains O(1) |
| 去重、保插入顺序 | LinkedHashSet | 哈希表 + 双向链表 |
| 去重、按自然序/比较器排序 | TreeSet | 红黑树，O(log n) |
| 键值映射、无序 | HashMap | 同 HashSet |
| 键值映射、保序 | LinkedHashMap | LRU 缓存可基于它实现 |
| 键值映射、按 key 排序 | TreeMap | 红黑树 |
| 多线程读写 | ConcurrentHashMap / CopyOnWriteArrayList | 见 1.7.4 末尾的并发提示 |

> **与 SelfBlog 的联系**：管理端列表页「批量补文章标题」的真实代码就是集合选型的实战——先把本页评论的 `postId` 收集进 `Set<Long>` 去重（HashSet），再 `selectBatchIds` 一次查回所有文章放进 `Map<Long, BlogPost>`（HashMap），最后遍历评论按 key 取标题。如果用嵌套循环（每条评论查一次库），20 条评论就是 20 次 SQL——这就是 N+1 查询问题，集合选型 + 一次批量查询直接消灭它。

### 1.7.2 List 集合

```java
import java.util.*;

public class ListDemo {
    public static void main(String[] args) {
        // ArrayList：基于数组实现，查询快，增删慢
        List<String> list = new ArrayList<>();
        
        // 添加元素
        list.add("Java");
        list.add("Python");
        list.add("C++");
        list.add(1, "JavaScript");  // 在指定位置插入
        
        System.out.println("List: " + list);
        
        // 获取元素
        System.out.println("第一个元素：" + list.get(0));
        
        // 修改元素
        list.set(2, "Go");
        System.out.println("修改后：" + list);
        
        // 删除元素
        list.remove(0);  // 按索引删除
        list.remove("Python");  // 按对象删除
        System.out.println("删除后：" + list);
        
        // 遍历
        // 方式1：for 循环
        for (int i = 0; i < list.size(); i++) {
            System.out.println("for: " + list.get(i));
        }
        
        // 方式2：增强 for 循环
        for (String lang : list) {
            System.out.println("for-each: " + lang);
        }
        
        // 方式3：迭代器
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            System.out.println("iterator: " + iterator.next());
        }
        
        // 方式4：Lambda 表达式（Java 8+）
        list.forEach(lang -> System.out.println("lambda: " + lang));
        
        // 方式5：方法引用
        list.forEach(System.out::println);
        
        // LinkedList：基于链表实现，增删快，查询慢
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("A");
        linkedList.add("B");
        linkedList.addFirst("First");
        linkedList.addLast("Last");
        System.out.println("LinkedList: " + linkedList);
        
        // 作为栈使用
        linkedList.push("Stack");
        System.out.println("push 后：" + linkedList);
        System.out.println("pop: " + linkedList.pop());
        
        // 作为队列使用
        linkedList.offer("Queue");
        System.out.println("offer 后：" + linkedList);
        System.out.println("poll: " + linkedList.poll());
    }
}
```

**ArrayList 的扩容机制**（面试常问，也能解释"预估容量"的性能技巧）：内部数组初始为空，第一次 add 时分配容量 10；装满后按 **1.5 倍**扩容并整体复制到新数组。所以"往 10 万个元素里逐个 add"会发生多次整组复制——已知大小时 `new ArrayList<>(100_000)` 一次性给足容量，就能省掉这些搬迁。同理，**中间位置插入/删除要整体挪动后面的元素**（O(n)），这就是"查询快、增删慢"的真实含义。

**List 互转的两条惯用法**：集合转数组用 `list.toArray(new String[0])`；数组转 List 用 `Arrays.asList(...)` 或 `List.of(...)`——但两者都**不是** ArrayList，不能 add/remove，想得到可变副本要再包一层 `new ArrayList<>(...)`（细节对比见 1.7.8）。

### 1.7.3 Set 集合

```java
import java.util.*;

public class SetDemo {
    public static void main(String[] args) {
        // HashSet：基于 HashMap 实现，无序，不可重复
        Set<String> set = new HashSet<>();
        set.add("Java");
        set.add("Python");
        set.add("C++");
        set.add("Java");  // 重复元素不会被添加
        
        System.out.println("HashSet: " + set);
        System.out.println("大小：" + set.size());
        System.out.println("包含 'Java': " + set.contains("Java"));
        
        // 遍历
        for (String lang : set) {
            System.out.println("for-each: " + lang);
        }
        
        // LinkedHashSet：保持插入顺序
        Set<String> linkedSet = new LinkedHashSet<>();
        linkedSet.add("Java");
        linkedSet.add("Python");
        linkedSet.add("C++");
        System.out.println("LinkedHashSet: " + linkedSet);
        
        // TreeSet：自然排序
        Set<String> treeSet = new TreeSet<>();
        treeSet.add("Java");
        treeSet.add("Python");
        treeSet.add("C++");
        System.out.println("TreeSet: " + treeSet);
        
        // TreeSet 自定义排序
        Set<Integer> customTreeSet = new TreeSet<>((a, b) -> b - a);  // 降序
        customTreeSet.add(5);
        customTreeSet.add(2);
        customTreeSet.add(8);
        System.out.println("自定义排序 TreeSet: " + customTreeSet);
        
        // 去重示例
        List<String> list = Arrays.asList("A", "B", "A", "C", "B");
        Set<String> uniqueSet = new HashSet<>(list);
        System.out.println("去重后：" + uniqueSet);
    }
}
```

**三个 Set 怎么选**：

| 实现 | 底层 | 顺序 | 典型场景 |
|------|------|------|----------|
| `HashSet` | 哈希表（数组+链表/红黑树） | 无 | 去重、存在性判断（`contains` O(1)） |
| `LinkedHashSet` | 哈希表 + 双向链表 | 插入顺序 | 去重且要保序（如"最近访问标签"） |
| `TreeSet` | 红黑树 | 排序 | 需要有序遍历/范围查询（O(log n)） |

去重依赖 `equals()`/`hashCode()` 契约——放自定义对象进 Set 前必须按 1.5.7 节成对重写这两个方法，否则"两个内容相同的对象"会被当成两个元素。

**与 SelfBlog 的联系**：判断"这篇 post 属于哪些 tag"、"用户收藏集合"这类存在性问题，`Set.contains` 是 O(1)；如果错用 `List.contains`，数据量一大就退化成 O(n) 的循环扫描。热点标签去重后再按访问量排序的场景，则是 `HashSet` 去重 → 转 List 排序的两步组合。

### 1.7.4 Map 集合

```java
import java.util.*;

public class MapDemo {
    public static void main(String[] args) {
        // HashMap：基于哈希表实现，键无序
        Map<String, Integer> map = new HashMap<>();
        
        // 添加键值对
        map.put("Java", 1);
        map.put("Python", 2);
        map.put("C++", 3);
        map.put("Java", 10);  // 键重复，值会被覆盖
        
        System.out.println("HashMap: " + map);
        
        // 获取值
        System.out.println("Java 的值：" + map.get("Java"));
        System.out.println("不存在的键：" + map.get("Go"));  // null
        System.out.println("不存在的键（带默认值）：" + map.getOrDefault("Go", 0));
        
        // 判断
        System.out.println("包含键 'Java': " + map.containsKey("Java"));
        System.out.println("包含值 2: " + map.containsValue(2));
        
        // 删除
        map.remove("C++");
        System.out.println("删除后：" + map);
        
        // 大小
        System.out.println("大小：" + map.size());
        
        // 遍历
        // 方式1：遍历键
        for (String key : map.keySet()) {
            System.out.println("key: " + key + ", value: " + map.get(key));
        }
        
        // 方式2：遍历值
        for (Integer value : map.values()) {
            System.out.println("value: " + value);
        }
        
        // 方式3：遍历键值对
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println("entry: " + entry.getKey() + " = " + entry.getValue());
        }
        
        // 方式4：Lambda 表达式
        map.forEach((key, value) -> System.out.println("lambda: " + key + " = " + value));
        
        // LinkedHashMap：保持插入顺序
        Map<String, Integer> linkedMap = new LinkedHashMap<>();
        linkedMap.put("Java", 1);
        linkedMap.put("Python", 2);
        linkedMap.put("C++", 3);
        System.out.println("LinkedHashMap: " + linkedMap);
        
        // TreeMap：键自然排序
        Map<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("Java", 1);
        treeMap.put("Python", 2);
        treeMap.put("C++", 3);
        System.out.println("TreeMap: " + treeMap);
        
        // 统计单词出现次数
        String text = "hello world hello java world java";
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : text.split(" ")) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }
        System.out.println("单词统计：" + wordCount);
    }
}
```

**HashMap 的三个必懂细节**：

1. **`getOrDefault` / `putIfAbsent` / `computeIfAbsent`**：少写一层 if。上面统计单词若用 `computeIfAbsent` 更简洁：
   ```java
   wordCount.computeIfAbsent(word, k -> new ArrayList<>()).add(line);  // 分组收集的惯用法
   ```
2. **key 的 hashCode/equals 契约**：用自定义对象当 key，必须同时重写 `hashCode()` 与 `equals()`（Lombok 的 `@EqualsAndHashCode` 或 IDE 生成），否则「相同的 key」在 Map 看来是两个
3. **HashMap 非线程安全**：多线程 put 可能丢数据甚至死循环（JDK 7 时代）；并发场景用 `ConcurrentHashMap`，不要用古老的 `Hashtable`（全表锁，性能差，已淘汰）

**四种遍历怎么选**：只要键值对一起用，`entrySet` 的 for-each 最高效（一次取全）；只要 key 用 `keySet()`，只要值就 `values()`；Lambda 风格 `forEach((k, v) -> ...)` 更紧凑，但循环体超过两三行时传统 for-each 更易调试（可打断点）。**不要在遍历中直接 put/remove**——原因见 1.7.6 的 fail-fast。

**与 SelfBlog 的联系**：`LinkedHashMap` 支持 accessOrder 模式，是实现 LRU 缓存的经典底座——SelfBlog 用 Caffeine（底层思想相同）做热点缓存；`TreeMap` 按键排序的特性适合"按日期归档文章"这类有序遍历需求。

### 1.7.5 泛型

泛型提供了编译时类型安全检查机制。

```java
import java.util.*;

public class GenericDemo {
    public static void main(String[] args) {
        // 泛型类的使用
        Box<String> stringBox = new Box<>("Hello");
        Box<Integer> intBox = new Box<>(123);
        
        System.out.println("String Box: " + stringBox.getItem());
        System.out.println("Integer Box: " + intBox.getItem());
        
        // 泛型方法
        Integer[] intArray = {1, 2, 3, 4, 5};
        String[] strArray = {"A", "B", "C"};
        
        printArray(intArray);
        printArray(strArray);
        
        // 泛型集合
        List<Integer> intList = new ArrayList<>();
        intList.add(1);
        intList.add(2);
        // intList.add("3");  // 编译错误
        
        // 通配符
        List<?> unknownList = new ArrayList<String>();
        List<? extends Number> numberList = new ArrayList<Integer>();
        List<? super Integer> intSuperList = new ArrayList<Number>();
        
        // 泛型擦除
        List<String> strList = new ArrayList<>();
        List<Integer> integerList = new ArrayList<>();
        System.out.println("strList class: " + strList.getClass());
        System.out.println("integerList class: " + integerList.getClass());
        System.out.println("类型相同: " + (strList.getClass() == integerList.getClass()));
    }
    
    // 泛型方法
    public static <T> void printArray(T[] array) {
        for (T element : array) {
            System.out.print(element + " ");
        }
        System.out.println();
    }
}

// 泛型类
class Box<T> {
    private T item;
    
    public Box(T item) {
        this.item = item;
    }
    
    public T getItem() {
        return item;
    }
    
    public void setItem(T item) {
        this.item = item;
    }
}

// 泛型接口
interface Container<T> {
    void add(T item);
    T get(int index);
}

// 实现泛型接口
class StringContainer implements Container<String> {
    private List<String> items = new ArrayList<>();
    
    @Override
    public void add(String item) {
        items.add(item);
    }
    
    @Override
    public String get(int index) {
        return items.get(index);
    }
}
```

**泛型的两个核心价值**（结合 SelfBlog 的 `Result<T>` 理解）：

1. **编译期类型安全**：`List<Post> posts` 里塞不进一个 User——写错了编译期就报错，而不是运行时 ClassCastException
2. **消除强制转换**：`Post p = posts.get(0)` 直接就是 Post 类型，不用 `(Post)` 强转

`Result<T>` 正是泛型的教科书案例：**信封结构（code/message/data）对所有接口完全相同，只有 data 的类型随接口变化**。`Result<List<Post>>`、`Result<PostVO>`、`Result<Void>`（只回状态不带数据）——一个泛型类覆盖全部接口的返回值，这就是「对变化的抽象」。

### 1.7.6 Iterator 迭代器与 fail-fast 机制

for-each 的底层就是 Iterator。理解它才能解释"遍历时能不能删元素"这个经典问题：

```java
import java.util.*;

public class IteratorDemo {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>(List.of("a", "b", "c", "d"));

        // Iterator 三步：hasNext() 判断 → next() 取出 →（可选）remove() 删除
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if (s.equals("b")) {
                it.remove();      // ✅ 迭代器自己的 remove，安全
            }
        }
        System.out.println(list); // [a, c, d]

        // ❌ fail-fast 演示：遍历中用集合自己的方法改结构
        try {
            for (String s : list) {
                if (s.equals("c")) {
                    list.remove(s);   // ConcurrentModificationException！
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("fail-fast 触发：" + e.getMessage());
        }
    }
}
```

**fail-fast 原理**：ArrayList 内部维护一个 `modCount` 修改计数器，每次 add/remove 都递增；迭代器创建时记住当时的值，每次 `next()` 都核对——发现不一致立刻抛 `ConcurrentModificationException`。它是一种"尽力而为"的检测机制（单线程误用和多线程并发都能触发），**目的是尽早暴露 bug，而不是保证并发安全**。

多线程并发访问的正确选择是 `ConcurrentHashMap`、`CopyOnWriteArrayList` 等并发容器；单线程"边遍历边删"的推荐写法是 Iterator.remove()，或下面这种 removeIf：

```java
// Java 8+ 的 removeIf：内部用的就是 Iterator.remove，一行搞定条件删除
list.removeIf(s -> s.equals("c"));
```

### 1.7.7 Collections 工具类

`java.util.Collections` 是集合的静态工具类（对应数组的 Arrays）：

```java
import java.util.*;

public class CollectionsDemo {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(List.of(5, 2, 8, 1));

        // 排序与查找（对象列表用 sort；binarySearch 同样要求先排序）
        Collections.sort(list);
        System.out.println(Collections.binarySearch(list, 8));   // 2

        // 逆序与洗牌
        Collections.reverse(list);
        System.out.println(list);
        Collections.shuffle(list);          // 随机打乱（抽奖、测试数据）

        // 最值
        System.out.println(Collections.max(list));
        System.out.println(Collections.min(list));

        // 不可变空集合 / 单元素集合（返回的集合禁止修改，见 1.7.8）
        List<String> empty = Collections.emptyList();
        Set<String> single = Collections.singleton("only");

        // 不可变视图：包装后任何修改操作抛 UnsupportedOperationException
        List<Integer> readOnly = Collections.unmodifiableList(list);
        // readOnly.add(9);   // 运行时异常

        // 线程安全包装（老 API，性能一般；现代项目优先并发容器）
        List<Integer> sync = Collections.synchronizedList(new ArrayList<>());
    }
}
```

### 1.7.8 不可变集合：List.of / Map.of / Set.of（Java 9+）

Java 9 提供了创建**真正不可变集合**的工厂方法，语义上优于 `Collections.unmodifiableXxx` 的视图包装：

```java
import java.util.*;

public class ImmutableCollectionDemo {
    public static void main(String[] args) {
        // 创建：最多 10 个元素的便捷重载，更多元素用 ofEntries
        List<String> langs = List.of("Java", "Python", "Go");
        Set<Integer> nums = Set.of(1, 2, 3);              // 元素不能重复
        Map<String, Integer> scores = Map.of("Java", 95, "Go", 88);
        Map<String, Integer> big = Map.ofEntries(
                Map.entry("a", 1), Map.entry("b", 2));

        // 三条铁律：不可增删改、不允许 null 元素、与可变集合互转
        // langs.add("C#");      // UnsupportedOperationException
        // langs.set(0, "x");    // 同上：set 也不行
        // List.of("a", null);   // NullPointerException，创建时就抛

        List<String> mutable = new ArrayList<>(langs);    // 需要"可变副本"时显式复制
        mutable.add("C#");
        System.out.println(mutable);
        System.out.println(langs);                        // 原集合毫发无损
    }
}
```

**`List.of` 与 `Arrays.asList` 的关键区别**：

| | `List.of(...)` | `Arrays.asList(...)` |
|---|---|---|
| set 修改元素 | ❌ 不支持 | ✅ 支持（改的是原数组） |
| add/remove | ❌ | ❌（长度固定） |
| null 元素 | ❌ 创建即 NPE | ✅ 允许 |
| 底层 | 专用不可变实现 | 包装原数组 |

新代码统一用 `List.of` / `Map.of` / `Set.of`；它们返回"值不可变"的实例，用作常量集合、方法默认值、Stream 的固定输入都很安全。SelfBlog 这类需要防御性编程的场合（比如把分类列表作为只读配置返回给前端），不可变集合能从根上杜绝"调用方偷偷改了共享数据"的问题。

## 1.8 异常处理

### 1.8.1 异常概述

异常是程序运行过程中出现的错误或异常情况。

```
Throwable
├── Error（错误，程序无法处理）
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── ...
└── Exception（异常，程序可以处理）
    ├── RuntimeException（运行时异常）
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   ├── ClassCastException
    │   ├── IllegalArgumentException
    │   └── ...
    └── 非运行时异常（受检异常）
        ├── IOException
        ├── SQLException
        ├── ClassNotFoundException
        └── ...
```

### 1.8.2 异常处理

```java
public class ExceptionDemo {
    public static void main(String[] args) {
        // try-catch-finally
        try {
            int result = 10 / 0;
            System.out.println("结果：" + result);
        } catch (ArithmeticException e) {
            System.out.println("算术异常：" + e.getMessage());
        } catch (Exception e) {
            System.out.println("其他异常：" + e.getMessage());
        } finally {
            System.out.println("finally 块总是执行");
        }
        
        // 多重 catch
        try {
            int[] arr = new int[5];
            arr[10] = 100;  // ArrayIndexOutOfBoundsException
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("数组越界：" + e.getMessage());
        } catch (Exception e) {
            System.out.println("其他异常：" + e.getMessage());
        }
        
        // try-with-resources（Java 7+）
        try (java.io.FileReader reader = new java.io.FileReader("test.txt")) {
            int data = reader.read();
            System.out.println("读取：" + data);
        } catch (java.io.IOException e) {
            System.out.println("IO 异常：" + e.getMessage());
        }
        
        // 抛出异常
        try {
            checkAge(15);
        } catch (Exception e) {
            System.out.println("年龄检查：" + e.getMessage());
        }
    }
    
    public static void checkAge(int age) throws Exception {
        if (age < 18) {
            throw new Exception("年龄必须大于等于 18 岁");
        }
        System.out.println("年龄合法");
    }
}
```

**try-with-resources 详解**

上面 try 的小括号里声明资源，是 Java 7 引入的 try-with-resources 语法。它解决的问题是：**资源（文件、流、连接）必须关闭，而手写 close() 容易漏**。传统写法的坑在于 finally 里还要再套一层 try-catch：

```java
// 传统写法：三行资源代码，六行收尾代码
FileReader reader = null;
try {
    reader = new FileReader("test.txt");
    int data = reader.read();
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (reader != null) {
        try {
            reader.close();      // close 自己也会抛 IOException，还得再套一层
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// try-with-resources：资源在 try 小括号里声明，自动关闭，顺序与声明相反
try (FileReader reader = new FileReader("test.txt");
     BufferedReader br = new BufferedReader(reader)) {   // 可以声明多个，分号分隔
    System.out.println(br.readLine());
} catch (IOException e) {
    e.printStackTrace();
}
// 无论正常结束还是抛异常，reader 和 br 都会被自动关闭（后声明的先关）
```

自动关闭的实现原理：任何实现了 `AutoCloseable` 接口（只有一个 `void close()` 方法）的对象都能放进 try 小括号。编译器把这段代码"翻译"成带 finally 的等价代码并插入 close 调用。自己写资源类时实现 AutoCloseable 即可接入这套机制。

**throw 与 throws 的区别**：`throws` 用在方法签名上声明"本方法可能抛出这些异常，我不处理，交给调用者"；`throw` 是语句，在方法体内**实际抛出**一个异常对象。上面 `checkAge` 方法签名上的 `throws Exception` 是声明，方法体里的 `throw new Exception(...)` 是抛出动作。

### 1.8.3 自定义异常

```java
// 自定义异常类
public class BusinessException extends RuntimeException {
    private int code;
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
}

// 使用自定义异常
public class CustomExceptionDemo {
    public static void main(String[] args) {
        try {
            login("admin", "wrong_password");
        } catch (BusinessException e) {
            System.out.println("错误码：" + e.getCode());
            System.out.println("错误信息：" + e.getMessage());
        }
    }
    
    public static void login(String username, String password) {
        if (!"admin".equals(username)) {
            throw new BusinessException(401, "用户名不存在");
        }
        if (!"correct_password".equals(password)) {
            throw new BusinessException(403, "密码错误");
        }
        System.out.println("登录成功");
    }
}
```

**自定义异常的最佳实践**（SelfBlog 的 BusinessException 就是按这套规范设计的）：

1. **继承 `RuntimeException` 而不是 `Exception`**。受检异常会强迫每一层方法签名写 `throws`、每个调用点写 try-catch，而业务异常（"文章不存在"、"密码错误"）恰恰希望"一路向上抛，直到全局处理器统一接住"。非受检异常让业务代码保持干净——这是实际项目的普遍选择。
2. **携带业务错误码**：`code` 字段让前端能按码分支处理（如 401 跳登录页），而不必解析消息文本。
3. **提供 `message` 之外，保留 cause 构造器**：捕获底层异常（如 IOException）转抛时，把原始异常作为 cause 传入，日志里才能看到完整因果链。
4. **异常消息面向人**：`"文章不存在"` 直接可以展示给用户或写进日志，不要写 `"error_10086"` 这种机器码当消息。
5. **不要用异常做流程控制**：异常构造时要填充线程栈（开销大），`if (obj != null)` 能解决的判断就别靠 `catch (NullPointerException)`。

### 1.8.4 以 SelfBlog 项目为例

SelfBlog 项目中的异常处理：

```java
// BusinessException.java
public class BusinessException extends RuntimeException {
    private final int code;
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
}

// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error(500, "服务器内部错误");
    }
}

// 使用示例
@Service
public class BlogPostServiceImpl {
    public BlogPost getPostById(Long id) {
        BlogPost post = mapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        return post;
    }
}
```

这套设计的完整数据流：**Service 抛出 BusinessException → 一路向上穿透 Controller → GlobalExceptionHandler 统一捕获 → 按异常里的 code 生成 `Result.error(code, message)` 返回给前端**。业务代码因此可以专注"什么情况算错"，而"错了怎么展示"集中在一个类里处理。

GlobalExceptionHandler 的 `@RestControllerAdvice` 注解是"对所有 @RestController 生效的通知"，它正是 1.11 节注解与反射机制的受益者——Spring 在启动时扫描这个类、记录每个 `@ExceptionHandler` 能处理哪些异常类型，运行时根据抛出的异常类型反射查表分发。理解了这一点，后面学习 Spring 的事件、拦截器、AOP 都会觉得眼熟。

### 1.8.5 异常处理最佳实践（重要）

**1. 捕获具体异常，不要一把抓**

```java
// ❌ 坏习惯：什么都捕 Exception，掩盖真实问题
try { ... } catch (Exception e) { }

// ✅ 好：只捕你预料到的、能处理的
try {
    mapper.insert(post);
} catch (DuplicateKeyException e) {
    throw new BusinessException(400, "标题已存在");
}
// 预料之外的异常让它往上飞，由全局处理器兜底
```

**2. 不要用异常做流程控制**

```java
// ❌ 坏味道：拿异常当 if-else 用（异常构造+栈快照很昂贵）
try {
    return Integer.parseInt(input);
} catch (NumberFormatException e) {
    return 0;
}

// ✅ 好：先判断
return input != null && input.matches("\\d+") ? Integer.parseInt(input) : 0;
```

**3. either 抛出去 or 记下来，别吞**

```java
// ❌ 最恶劣的写法：吞异常。出错时连排查线索都没有
catch (IOException e) { }

// ❌ 次恶劣：只打印不处理也不上抛，调用方以为成功了
catch (IOException e) { e.printStackTrace(); }

// ✅ 至少二选一：包装成业务异常上抛 / 记日志后再决定
catch (IOException e) {
    log.error("读取文章文件失败 id={}", id, e);
    throw new BusinessException(500, "文章加载失败");
}
```

**4. finally / try-with-resources 里不要 return**

finally 中的 return 会**覆盖** try 块的返回值和异常，让调用方对失败毫无感知。资源关闭交给 try-with-resources（1.8.2 已示范）。

**5. 异常信息要带上下文**

```java
// ❌ "格式错误" —— 三个月后没人知道哪错了
throw new BusinessException(400, "格式错误");
// ✅ 带上定位线索
throw new BusinessException(400, "文章状态非法: id=" + id + ", status=" + status);
```

> **与 SelfBlog 的联系**：SelfBlog 的异常体系只有两层——`BusinessException`（可预期的业务异常，带 code）与兜底 `Exception`（未知异常，只记日志不暴露细节给前端）。业务代码里**到处都是** `throw new BusinessException(404, "文章不存在")`，这就是受检/非受检之争在真实项目里的答案：非受检 + 全局兜底，代码干净且不漏。

## 1.9 IO 流

### 1.9.1 IO 流概述

IO 流用于处理数据的输入和输出。

```
IO 流
├── 字节流（8 位）
│   ├── InputStream
│   │   ├── FileInputStream
│   │   ├── ByteArrayInputStream
│   │   └── BufferedInputStream
│   └── OutputStream
│       ├── FileOutputStream
│       ├── ByteArrayOutputStream
│       └── BufferedOutputStream
└── 字符流（16 位）
    ├── Reader
    │   ├── FileReader
    │   ├── InputStreamReader
    │   └── BufferedReader
    └── Writer
        ├── FileWriter
        ├── OutputStreamWriter
        └── BufferedWriter
```

### 1.9.2 字节流

```java
import java.io.*;

public class ByteStreamDemo {
    public static void main(String[] args) {
        // 文件输入流
        try (FileInputStream fis = new FileInputStream("input.txt")) {
            int data;
            while ((data = fis.read()) != -1) {
                System.out.print((char) data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 文件输出流
        try (FileOutputStream fos = new FileOutputStream("output.txt")) {
            String content = "Hello, World!";
            fos.write(content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 缓冲字节流（推荐）
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream("input.txt"));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("output.txt"))) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 复制文件
        copyFile("source.txt", "destination.txt");
    }
    
    public static void copyFile(String source, String dest) {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("文件复制成功");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### 1.9.3 字符流

```java
import java.io.*;

public class CharStreamDemo {
    public static void main(String[] args) {
        // 文件读取
        try (FileReader reader = new FileReader("input.txt")) {
            int data;
            while ((data = reader.read()) != -1) {
                System.out.print((char) data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 文件写入
        try (FileWriter writer = new FileWriter("output.txt")) {
            writer.write("Hello, World!");
            writer.write("\n");
            writer.write("你好，世界！");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 缓冲字符流（推荐）
        try (BufferedReader br = new BufferedReader(new FileReader("input.txt"));
             BufferedWriter bw = new BufferedWriter(new FileWriter("output.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 转换流（处理字符编码）
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream("input.txt"), "UTF-8");
             OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("output.txt"), "UTF-8")) {
            char[] buffer = new char[1024];
            int charsRead;
            while ((charsRead = isr.read(buffer)) != -1) {
                osw.write(buffer, 0, charsRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### 1.9.4 序列化

序列化是将对象转换为字节流的过程，反序列化是将字节流转换为对象的过程。

```java
import java.io.*;

// 可序列化的类
class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private int age;
    private transient String password;  // transient 关键字修饰的字段不会被序列化
    
    public Person(String name, int age, String password) {
        this.name = name;
        this.age = age;
        this.password = password;
    }
    
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + ", password='" + password + "'}";
    }
}

public class SerializationDemo {
    public static void main(String[] args) {
        // 序列化
        Person person = new Person("张三", 25, "123456");
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("person.dat"))) {
            oos.writeObject(person);
            System.out.println("对象序列化成功");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 反序列化
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("person.dat"))) {
            Person deserializedPerson = (Person) ois.readObject();
            System.out.println("反序列化对象：" + deserializedPerson);
            // password 为 null，因为 transient 字段不会被序列化
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
```

### 1.9.5 NIO.2：Files 工具类（现代写法）

Java 7 引入的 `java.nio.file` 是今天操作文件的首选——一行顶旧 IO 十行：

```java
import java.nio.file.*;
import java.util.List;

public class FilesDemo {
    public static void main(String[] args) throws Exception {
        Path path = Path.of("posts.txt");          // Path 取代 File

        // 读整个文件（小文件一次拿）
        String all = Files.readString(path);
        List<String> lines = Files.readAllLines(path);

        // 写文件（CREATE/TRUNCATE/WRITE 是标准打开选项）
        Files.writeString(path, "新的内容\n", StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        // 追加
        Files.writeString(path, "追加一行\n", StandardOpenOption.APPEND);

        // 逐行惰性读取（大文件友好，用完要关）
        try (var stream = Files.lines(path)) {
            stream.filter(l -> l.contains("Java")).forEach(System.out::println);
        }

        // 目录/文件操作
        Files.createDirectories(Path.of("upload/2026/10"));   // 递归建目录
        Files.copy(path, Path.of("backup.txt"), StandardCopyOption.REPLACE_EXISTING);
        Files.move(path, Path.of("renamed.txt"));
        boolean exists = Files.exists(path);
        long size = Files.size(path);

        // 遍历目录树
        try (var walk = Files.walk(Path.of("upload"))) {
            walk.filter(Files::isRegularFile)
                .forEach(p -> System.out.println(p));
        }
    }
}
```

> **与 SelfBlog 的联系**：图片上传接口保存文件就是这一套——`Files.createDirectories` 建上传目录（按日期分桶）、UUID 重命名后 `Files.copy(multipartFile.getInputStream(), target)` 落盘。上传路径由用户名拼接时还要防**路径穿越攻击**（`../`），所以先用 UUID 重命名、扩展名白名单过滤。

## 1.10 Lambda 表达式、Stream API 与 Optional

Java 8 引入的函数式特性是现代 Java 代码的底色，SelfBlog 的查询、分组、转换逻辑大量使用它们，必须熟练掌握。

### 1.10.1 Lambda 表达式与方法引用

Lambda 本质是**函数式接口**（只有一个抽象方法的接口）的简写。SelfBlog 及 Spring 源码里大量函数式接口只需认识这四个：

| 接口 | 抽象方法 | 用途 | 示例 |
|------|----------|------|------|
| `Predicate<T>` | `boolean test(T t)` | 判断 | `s -> s.isEmpty()` |
| `Function<T,R>` | `R apply(T t)` | 转换 | `s -> s.length()` |
| `Consumer<T>` | `void accept(T t)` | 消费 | `s -> System.out.println(s)` |
| `Supplier<T>` | `T get()` | 生产 | `() -> new ArrayList<>()` |

```java
import java.util.*;
import java.util.function.*;

public class LambdaDemo {
    public static void main(String[] args) {
        // 匿名内部类 → Lambda 的演进
        Runnable oldWay = new Runnable() {
            @Override
            public void run() {
                System.out.println("旧写法：匿名内部类");
            }
        };
        Runnable newWay = () -> System.out.println("新写法：Lambda");
        oldWay.run();
        newWay.run();

        // Lambda 省略规则：参数类型可推断则省略；单参数可省括号；单语句可省 return 和大括号
        Comparator<String> byLength = (a, b) -> a.length() - b.length();
        List<String> names = new ArrayList<>(List.of("Charlie", "Al", "Bob"));
        names.sort(byLength);
        System.out.println(names);   // [Al, Bob, Charlie]

        // 方法引用：Lambda 只调用一个已存在方法时的进一步简写
        List<String> upper = names.stream()
                .map(String::toUpperCase)      // s -> s.toUpperCase() 的简写
                .toList();
        System.out.println(upper);

        Consumer<String> printer = System.out::println;  // System.out 对象的 println 方法
        printer.accept("方法引用");

        Supplier<List<String>> maker = ArrayList::new;   // 构造器引用
        System.out.println(maker.get());
    }
}
```

方法引用四种形式：`类名::静态方法`（`Integer::parseInt`）、`对象::实例方法`（`System.out::println`）、`类名::实例方法`（`String::toUpperCase`，第一个参数作接收者）、`类名::new`（`ArrayList::new`）。读代码时把 `X::y` 还原成 `x -> x.y(...)` 就不陌生了。

### 1.10.2 Stream API 基础

Stream 是对集合数据的**声明式**处理管道：`集合 → 中间操作（惰性，可串联）→ 终端操作（触发执行）`。声明式的意思是：你描述"做什么"（过滤、映射、收集），而不是"怎么循环、怎么暂存中间结果"——那些样板代码被框架吃掉了。对比 1.7.4 里手写 for 循环的单词统计：同一个需求，Stream 版是"分割 → 按值分组 → 计数"三步自然语言式表达：

```java
Map<String, Long> counts = Arrays.stream(text.split(" "))
        .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
```

```java
import java.util.*;
import java.util.stream.*;

public class StreamDemo {
    public static void main(String[] args) {
        List<String> words = List.of("Java", "Python", "C++", "JavaScript", "Go");

        // 经典管道：筛选 → 转换 → 收集
        List<String> result = words.stream()          // 1. 得到流
                .filter(w -> w.length() > 3)          // 2. 中间操作：过滤（可多个）
                .map(String::toUpperCase)             // 3. 中间操作：一对一转换
                .sorted()                             // 4. 中间操作：排序
                .collect(Collectors.toList());        // 5. 终端操作：收集
        System.out.println(result);   // [JAVASCRIPT, JAVA, PYTHON]

        // 其他常用中间/终端操作
        List<Integer> nums = List.of(5, 3, 8, 1, 9, 3);
        System.out.println(nums.stream().distinct().toList());       // 去重 [5, 3, 8, 1, 9]
        System.out.println(nums.stream().limit(3).toList());         // 前 3 个 [5, 3, 8]
        System.out.println(nums.stream().skip(4).toList());          // 跳过前 4 个 [9, 3]
        System.out.println(nums.stream().sorted().findFirst());      // Optional[1]
        System.out.println(nums.stream().anyMatch(n -> n > 8));      // true
        System.out.println(nums.stream().allMatch(n -> n > 0));      // true
        System.out.println(nums.stream().count());                   // 6

        // 数值流聚合
        System.out.println(nums.stream().mapToInt(Integer::intValue).sum());     // 32
        System.out.println(nums.stream().max(Integer::compareTo));               // Optional[9]
        System.out.println(nums.stream().mapToInt(n -> n).average());            // OptionalDouble[4.83...]

        // flatMap：一对多展开（"压平"嵌套）
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3, 4));
        List<Integer> flat = nested.stream()
                .flatMap(List::stream)
                .toList();
        System.out.println(flat);   // [1, 2, 3, 4]
    }
}
```

三条使用要点：

1. **流是一次性的**——终端操作之后流就关闭了，再用抛 `IllegalStateException`；需要二次处理就重新 `.stream()`
2. **中间操作是惰性的**——不遇到终端操作，filter/map 一行都不执行
3. **不要在流操作里有副作用**——`forEach` 里改外部变量是反模式，收集结果用 `collect`/`toList()`

### 1.10.3 Collector：分组、分区与拼接

`Collectors` 工具类提供强大的"收集器"，把流元素按各种规则归拢。SelfBlog 的文章列表按分类分组、按月份归档，都是分组收集的典型场景：

```java
import java.util.*;
import java.util.stream.*;

public class CollectorDemo {
    record Post(String title, String category, int views) {}

    public static void main(String[] args) {
        List<Post> posts = List.of(
                new Post("Java 入门",   "技术", 1200),
                new Post("旅行日记",    "生活",  800),
                new Post("Stream 指南", "技术", 2500),
                new Post("咖啡笔记",    "生活",  300),
                new Post("云原生漫谈",  "技术",  950));

        // ===== 分组 groupingBy：按 key 归类 =====
        Map<String, List<Post>> byCategory =
                posts.stream().collect(Collectors.groupingBy(Post::category));
        System.out.println(byCategory.keySet());   // [技术, 生活]

        // 分组 + 计数
        Map<String, Long> countByCategory =
                posts.stream().collect(Collectors.groupingBy(Post::category, Collectors.counting()));
        System.out.println(countByCategory);       // {技术=3, 生活=2}

        // 分组 + 求和 / 求平均（下游收集器可以层层嵌套）
        Map<String, Integer> viewsByCategory =
                posts.stream().collect(Collectors.groupingBy(Post::category,
                        Collectors.summingInt(Post::views)));
        System.out.println(viewsByCategory);       // {技术=4650, 生活=1100}

        // ===== 分区 partitioningBy：按 boolean 分成两组 =====
        Map<Boolean, List<Post>> parts =
                posts.stream().collect(Collectors.partitioningBy(p -> p.views() > 1000));
        System.out.println("热门: " + parts.get(true).size());    // 2
        System.out.println("普通: " + parts.get(false).size());   // 3

        // ===== 拼接 joining =====
        String titles = posts.stream()
                .map(Post::title)
                .collect(Collectors.joining("、", "《", "》"));   // 分隔符、前缀、后缀
        System.out.println(titles);
        // 《Java 入门、旅行日记、Stream 指南、咖啡笔记、云原生漫谈》

        // ===== 其他常用收集器 =====
        System.out.println(posts.stream()
                .collect(Collectors.mapping(Post::title, Collectors.toList())));  // 先转换再收集
        System.out.println(posts.stream()
                .collect(Collectors.toMap(Post::title, Post::views)));  // 转成键值 Map
        // toMap 注意：key 重复会抛 IllegalStateException，用合并函数兜底：
        // Collectors.toMap(k, v, (v1, v2) -> v1)
    }
}
```

记忆方式：**groupingBy 按"值"分多组，partitioningBy 按真伪分两组，joining 把流拼成字符串**，三者都能嵌套下游收集器做二次聚合。

**与 SelfBlog 的联系**：侧边栏"分类 + 每分类文章数"、归档页"按年月分组"在 SQL 里是 GROUP BY，在内存里就是 `Collectors.groupingBy`。两者何时用哪个：数据已在库里、纯聚合 → 让数据库 GROUP BY（少传输数据）；数据已在内存（如缓存里的列表）要二次重组 → groupingBy。`toMap` 是 DTO 组装的常客——把 `List<Category>` 转成 `Map<Long, String>`（id → 名称），配合 1.7.1 提到的批量查询消灭 N+1。

### 1.10.4 并行流：适用场景与陷阱

`parallelStream()` 把数据切成多段用多核并行处理。**但并行不是免费的**，默认用公共 ForkJoinPool，用错场景反而更慢甚至出错：

```java
import java.util.*;
import java.util.stream.*;

public class ParallelStreamDemo {
    public static void main(String[] args) {
        // ===== 适合并行的场景 =====
        // 大数据量 + 无状态 + 可独立计算（如 CPU 密集的数值计算）
        List<Long> numbers = new ArrayList<>();
        for (long i = 1; i <= 10_000_000; i++) numbers.add(i);

        long t1 = System.currentTimeMillis();
        long sum1 = numbers.stream().mapToLong(Long::longValue).sum();
        long t2 = System.currentTimeMillis();
        long sum2 = numbers.parallelStream().mapToLong(Long::longValue).sum();
        long t3 = System.currentTimeMillis();
        System.out.println("串行: " + (t2 - t1) + "ms, 并行: " + (t3 - t2) + "ms");

        // ===== 陷阱一：共享可变状态（结果错误） =====
        List<String> unsafe = new ArrayList<>();
        List.of("a", "b", "c").parallelStream().forEach(unsafe::add);
        // unsafe.size() 可能 < 3：多个线程同时 ArrayList.add 丢数据
        // 正确做法：收集结果交给 collect，别在 forEach 里手动往 List 里塞

        // ===== 陷阱二：小数据量并行反而更慢 =====
        // 任务拆分、线程调度、结果合并都有开销；几百个元素串行可能快一个数量级

        // ===== 陷阱三：阻塞操作占满公共线程池 =====
        // parallelStream 默认用 JVM 全局 ForkJoinPool.commonPool()
        // 在其中做 HTTP 请求/DB 查询会拖垮其他并行任务，甚至死锁
        // IO 密集任务请用线程池或 Java 21 的虚拟线程，别用并行流
    }
}
```

决策口诀：**默认串行流；数据量大、CPU 密集、各元素互不干扰时才考虑并行流；涉及 IO 或共享状态一律不用**。SelfBlog 的业务逻辑（查库、拼装响应）属于 IO 密集，全部用串行流——这也是大多数 Web 后端的正确选择。

### 1.10.5 Optional：正确表达"可能没有值"

`Optional<T>` 是一个可能装着值、也可能为空的容器，用它替代返回 `null`，让"可能没有"进入方法签名，编译器就能帮你检查：

```java
import java.util.*;
import java.util.Optional;

public class OptionalDemo {
    public static void main(String[] args) {
        List<String> names = List.of("Alice", "Bob", "Carol");

        // 创建 Optional 的三种方式
        Optional<String> full  = Optional.of("hello");          // 值不能为 null，否则 NPE
        Optional<String> nullable = Optional.ofNullable(null);  // 值可为 null
        Optional<String> empty = Optional.empty();              // 显式空

        // ===== 反模式：Optional 当 if-else 用（不如直接判 null） =====
        Optional<String> found = names.stream().filter(n -> n.startsWith("B")).findFirst();
        // ❌ isPresent() + get()：只是把 null 检查换了个马甲，还多了 NPE 风险
        if (found.isPresent()) {
            System.out.println(found.get());
        }
        // ✅ 正确姿势一：ifPresent——有值才执行
        found.ifPresent(n -> System.out.println("找到：" + n));
        // ✅ 正确姿势二：orElse / orElseGet——提供默认值
        String name = found.orElse("游客");
        String computed = found.orElseGet(() -> "默认" + System.nanoTime());
        // ✅ 正确姿势三：orElseThrow——没有就抛异常（SelfBlog 风格）
        // String mustExist = found.orElseThrow(() -> new BusinessException(404, "不存在"));
        // ✅ 正确姿势四：map 链式转换，全程无需判空
        int len = found.map(String::length).orElse(0);
        System.out.println(name + " " + len);

        // Stream 终端操作大量返回 Optional，正是它最常见的来源
        Optional<Integer> max = List.of(3, 7, 2).stream().max(Integer::compareTo);
        max.ifPresent(m -> System.out.println("最大值：" + m));
    }
}
```

使用守则：

1. **返回值用 Optional，字段和参数不用**（Optional 本意就是"方法可能没有结果"）
2. **永远不要 `get()` 裸取**——先 ifPresent / orElse / orElseThrow 消费它
3. **不要 `Optional.of(null)`**——需要容纳 null 用 `ofNullable`
4. SelfBlog 的 `SecurityUtils.getCurrentUserIdOrNull()` 与 `getCurrentUserId()`（未登录抛 401）正是两种风格的对照：可空场景返回 null 语义，必存在场景抛业务异常

## 1.11 注解与反射入门

学习 Spring 之前必须先懂这两样——Spring 的核心魔法（`@Service`、`@Autowired`、`@RestControllerAdvice`）全部建立在它们之上。

### 1.11.1 注解：给代码贴标签

注解（Annotation）是贴在类、方法、字段上的**元数据标签**，本身不执行任何逻辑，只提供信息，由**框架在运行时读取并做出反应**：

```java
import java.lang.annotation.*;
import java.lang.reflect.Method;

// 1. 用 @interface 定义注解
@Retention(RetentionPolicy.RUNTIME)   // 保留到运行时（反射才能读到）
@Target(ElementType.METHOD)           // 只能贴在方法上
public @interface LogExecutionTime {
    String value() default "";        // 注解参数，带 default 可以不填
}

// 2. 在业务方法上使用
class ReportService {
    @LogExecutionTime("生成阅读报告")
    public void generate() {
        System.out.println("报告生成中...");
    }
}

// 3. 框架侧：反射读取注解并执行附加逻辑
public class AnnotationDemo {
    public static void main(String[] args) throws Exception {
        Method m = ReportService.class.getMethod("generate");
        if (m.isAnnotationPresent(LogExecutionTime.class)) {   // 反射：检查注解是否存在
            LogExecutionTime anno = m.getAnnotation(LogExecutionTime.class);
            System.out.println("拦截到方法 " + m.getName()
                    + "，注解值=" + anno.value());             // 框架在这里织入计时/AOP 逻辑
        }
        m.invoke(new ReportService());   // 反射调用方法
    }
}
```

三个元注解（"描述注解的注解"）必须理解：`@Retention` 决定注解存活到哪（SOURCE 源码即弃 / CLASS 字节码保留 / RUNTIME 运行时可反射读取——**Spring 全用 RUNTIME**）；`@Target` 限制可贴的位置；`@Inherited` 允许子类继承。

**注解为什么能"生效"**：注解本身只是数据标签，像便签纸贴在类上。真正让它产生效果的是两类机制——**运行时反射**（Spring 的 `@Component`、SelfBlog 的 `@RestControllerAdvice`：框架扫描到便签后按标签内容行动）和**编译期注解处理器**（Lombok 的 `@Getter`：编译时直接改写字节码生成方法，所以运行期反而不需要反射）。理解这两条路线，就明白"注解 + 框架"的一切魔法：标签是声明，框架是解释器。

认识 SelfBlog 里常见的内置注解：

| 注解 | 作用 |
|------|------|
| `@Override` | 编译器校验确实重写了父类方法（拼写错了立刻报错） |
| `@Deprecated` | 标记过时元素，调用处编译警告 |
| `@SuppressWarnings("unchecked")` | 压制指定警告 |
| `@FunctionalInterface` | 声明函数式接口（1.10.1） |
| `@SafeVarargs` | 声明泛型可变参数安全 |

### 1.11.2 反射：运行时解剖类

反射（Reflection）让程序在**运行时**检查和操作类：拿到 Class 对象后可以读字段、调方法、建实例——哪怕编译时根本不知道这个类叫什么。

```java
import java.lang.reflect.*;

class User {
    private String name = "张三";
    public void sayHello() { System.out.println("Hello, " + name); }
    private void secret()  { System.out.println("这是私有方法"); }
}

public class ReflectionDemo {
    public static void main(String[] args) throws Exception {
        // 获取 Class 对象的三种方式（同一份 Class，全局唯一）
        Class<?> c1 = User.class;                          // 类名字面量
        Class<?> c2 = new User().getClass();               // 对象的 getClass()
        Class<?> c3 = Class.forName("User");               // 全限定名字符串（框架最常用）

        System.out.println(c1.getName());                  // 类全名
        for (Field f : c1.getDeclaredFields()) {           // 所有字段（含私有）
            System.out.println("字段: " + f.getName());
        }
        for (Method m : c1.getDeclaredMethods()) {         // 所有方法
            System.out.println("方法: " + m.getName());
        }

        // 运行时创建实例并调用方法
        Object user = c1.getDeclaredConstructor().newInstance();
        Method hello = c1.getMethod("sayHello");
        hello.invoke(user);                                // 等价于 ((User) user).sayHello()

        // 暴力访问私有成员（setAccessible(true) 绕过访问检查）
        Method secret = c1.getDeclaredMethod("secret");
        secret.setAccessible(true);
        secret.invoke(user);
        Field nameField = c1.getDeclaredField("name");
        nameField.setAccessible(true);
        System.out.println("私有字段 name = " + nameField.get(user));
    }
}
```

**为什么 Spring 离不开反射**：`@Service` 标注的类是 Spring 在启动时通过反射实例化的（Spring 自己 new，你的代码从不写 `new UserService()`）；`@Autowired` 是 Spring 反射读取字段类型、再反射注入实例；MyBatis Plus 的 `BaseMapper` 是运行时为 Mapper 接口生成代理实现。**框架用反射换来了"你的代码不用关心装配过程"**；代价是反射调用比直接调用慢一些、编译器无法检查，所以框架内部有大量优化，业务代码则应当少直接用反射。

反射还有一位轻量搭档：`instanceof` 模式匹配（Java 16+），判断类型并绑定变量的语法糖：

```java
Object obj = "hello";
if (obj instanceof String s) {          // 判断+强转+声明一步完成
    System.out.println(s.length());
}
```

## 1.12 Maven 入门

SelfBlog 后端是标准 Maven 项目，本节用它的真实配置讲解 Maven 的核心概念。

### 1.12.1 Maven 是什么

Maven 解决三个问题：**依赖管理**（声明"我要什么"，自动下载 jar 及其依赖）、**标准化构建**（统一 compile/test/package 流程）、**项目结构规范**（约定优于配置）。它以 `pom.xml`（Project Object Model）描述项目。

"约定优于配置"值得展开一句：Maven 规定主代码放 `src/main/java`、资源放 `src/main/resources`、测试放 `src/test/java`，所有插件都默认按这个布局工作。你不需要（也不应该）告诉 Maven"我的代码在哪"——目录即配置。IDEA 新建 Maven 项目时生成的骨架正是这套约定。

### 1.12.2 pom.xml 结构：以 SelfBlog 为例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>

    <!-- parent：继承 Spring Boot 官方父 POM，获得依赖版本仲裁与插件默认配置 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.13</version>
        <relativePath/>
    </parent>

    <!-- 坐标（GAV）：Maven 世界的唯一地址 -->
    <groupId>com.selfblog</groupId>       <!-- 组织/公司倒写域名 -->
    <artifactId>selfblog</artifactId>     <!-- 项目名 -->
    <version>2.0.25</version>             <!-- 版本 -->

    <properties>
        <java.version>21</java.version>   <!-- 编译目标；parent 已定义 sourceEncoding=UTF-8 -->
    </properties>

    <dependencies>
        <!-- 每个 starter 打包了一组配套依赖，引入即用 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>       <!-- Web MVC + 内嵌 Tomcat -->
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>3.5.7</version>   <!-- 不在 parent 管理范围内的要自己写版本 -->
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>    <!-- 只参与编译，不进最终包 -->
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>        <!-- 只在 src/test 里可见 -->
        </dependency>
    </dependencies>

    <build>
        <finalName>selfblog</finalName>   <!-- 打包产物名：target/selfblog.jar -->
        <plugins>
            <!-- frontend-maven-plugin：构建时顺带构建前端（引出 1.12.6 的大坑） -->
        </plugins>
    </build>
</project>
```

要点拆解：

- **GAV 坐标**：`groupId:artifactId:version` 三元组唯一定位一个构件。中央仓库里数百万 jar 都靠它寻址
- **parent 继承**：`spring-boot-starter-parent` 集中管理了上千个常用依赖的版本号——引入 `spring-boot-starter-web` **不用写 version**，写错版本反而破坏兼容性。只有 parent 没管理的依赖（如 mybatis-plus）才需要显式给版本
- **scope 四大常客**：`compile`（默认，编译+运行都要）、`provided`（编译要，运行环境自带，如 Lombok）、`runtime`（编译不要，运行要，如 JDBC 驱动）、`test`（只在测试可见，如 JUnit）
- **依赖传递**：A 依赖 B、B 依赖 C，引入 A 时 C 也会自动进来。冲突时 Maven 按"最短路径优先"仲裁

### 1.12.3 依赖管理常用技巧

```bash
# 查看依赖树（排查"某个类从哪个 jar 来的"必备）
mvnw dependency:tree
```

排除传递依赖（IDEA 里对着 dependency:tree 的结果定位后复制）：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>legacy-lib</artifactId>
    <exclusions>
        <exclusion>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

本地开发的依赖会缓存在用户目录 `.m2\repository` 下，断网时只要缓存里有就能构建。

### 1.12.4 构建生命周期与常用命令

Maven 有三套独立生命周期：`clean`（清理）、`default`（核心构建）、`site`（文档）。`default` 的关键阶段按序执行：

```
validate → compile → test → package → verify → install → deploy
```

执行某个阶段时，**之前所有阶段自动执行**——`package` 之前必然先 compile、test。常用命令（在 `backend/` 目录下）：

```bash
# 编译主代码
mvnw compile

# 跑测试（SelfBlog 必须加两个 -D 参数，原因见 1.12.6）
mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"

# 只跑一个测试类 / 一个方法
mvnw.cmd test "-Dtest=AuthControllerTest" "-Dskip.npm=true" "-Dskip.installnodenpm=true"

# 打包（跳过测试）→ target/selfblog.jar
mvnw.cmd clean package -DskipTests

# 安装到本地仓库（供其他本地项目依赖）
mvnw.cmd clean install -DskipTests
```

### 1.12.5 仓库与依赖解析顺序

Maven 找依赖的顺序：**本地仓库**（`.m2`，缓存）→ **中央仓库**（https://repo.maven.apache.org）→ 项目里配置的**远程私服**（公司内网常见）。第一次下载的构件会缓存到本地，之后离线也能构建。

### 1.12.6 Maven Wrapper 与 SelfBlog 的两个 -D 参数

**Maven Wrapper（mvnw）是什么**：把构建工具本身也纳入版本管理的机制。仓库里提交 `mvnw.cmd`（Windows 脚本）、`mvnw`（shell 脚本）、`.mvn/wrapper/`（下载器配置）三个文件，任何人 clone 后无需预装 Maven——第一次运行 `mvnw.cmd` 时它按 `.mvn/wrapper/maven-wrapper.properties` 里记录的版本（SelfBlog 是 Maven 3.9.12）自动下载并使用。这保证了**全世界的机器用完全相同的 Maven 版本构建**，也顺便解决了"本机没装 Maven"的问题。`mvnw.cmd` 需要 `JAVA_HOME` 指向 JDK 21 才能运行 SelfBlog 的构建。

**两个 -D 参数的由来**：SelfBlog 的 pom.xml 配置了 `frontend-maven-plugin`，它绑定在 `initialize` 和 `generate-resources` 阶段，会在 `../frontend` 目录执行 `npm ci` + `npm run build`——本意是"一条命令连前端一起构建"。但代价是：**不带参数跑一次测试，整个前端被重建一遍**，还要改写前端构建产物。所以本仓库的约定是：

```powershell
backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

- `-Dskip.npm=true`：跳过 npm 执行阶段
- `-Dskip.installnodenpm=true`：跳过下载/安装 Node 和 npm 本身

PowerShell 中 `-D` 参数必须加引号（原因见 1.2.2 问题五）。CI 的 GitHub Actions 用的是 Linux shell + `./mvnw`，同样带这两个开关。

### 1.12.7 单元测试初体验：JUnit 5 入门

Maven 的标准目录里，`src/main/java` 放主代码、`src/test/java` 放测试代码。测试类与被测类**同名包**（这样 package-private 成员也能访问）。SelfBlog 后端有 588 个测试，是全项目最硬的质量底线。

**最小编写套路**：

```java
// src/test/java/com/selfblog/utils/VisitorHashUtilsTest.java
package com.selfblog.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("访客哈希工具")
class VisitorHashUtilsTest {

    @Test
    @DisplayName("同输入产生同哈希（可关联访客身份）")
    void hash_isDeterministic() {
        String h1 = VisitorHashUtils.hash("visitor-123", "salt");
        String h2 = VisitorHashUtils.hash("visitor-123", "salt");
        assertEquals(h1, h2);
    }

    @Test
    @DisplayName("不同盐产生不同哈希（防彩虹表）")
    void hash_saltMatters() {
        assertNotEquals(
            VisitorHashUtils.hash("visitor-123", "salt-a"),
            VisitorHashUtils.hash("visitor-123", "salt-b"));
    }

    @Test
    @DisplayName("非法 visitorId 抛出业务异常")
    void hash_rejectsBadId() {
        assertThrows(IllegalArgumentException.class,
            () -> VisitorHashUtils.hash(null, "salt"));
    }
}
```

**三个关键词**：

1. **@Test** 标注测试方法（JUnit 5 不再要求 public，方法名可任意，用 @DisplayName 写人话）
2. **断言**：`assertEquals`（相等）、`assertTrue`（真）、`assertThrows`（异常）、`assertNotNull`（非空）。断言失败即测试失败，并打印期望值 vs 实际值
3. **AAA 结构**：Arrange 准备数据 → Act 执行动作 → Assert 验证结果。每个测试只测一个行为

**运行**：`mvnw test`（全量）或 `mvnw test "-Dtest=VisitorHashUtilsTest"`（单类），方法级再加 `#方法名`。IDEA 里点方法旁的绿色三角更直接。

**为什么测试值得写**：不是「为了覆盖率」，而是**重构的胆量**——SelfBlog 做「门面/查询/交互」三分架构梳理时，588 个测试全程绿着改完。没有测试的重构叫赌博，有测试的重构叫工程。第二章 2.11 节讲 Mockito 模拟依赖，第三章 3.17 节讲 Spring 环境下的切片测试。

**最后一个通用话题：泛型通配符，读懂源码的门票**。`List<Object>` 与 `List<String>` 没有父子关系（泛型不协变），于是需要通配符衔接。只需记住两个方向的口诀：**读用 extends，写用 super**（PECS）。

```java
// 生产者：只往外读 → extends。任何 Number 子类的列表都能传进来，读出来当 Number 用
double sum(List<? extends Number> nums) {
    double s = 0;
    for (Number n : nums) s += n.doubleValue();   // 只读，不能 add
    return s;
}
sum(List.of(1, 2, 3));        // List<Integer> ✓
sum(List.of(1.5, 2.5));       // List<Double> ✓

// 消费者：只往里写 → super。可以往 List<Number> 或 List<Object> 里塞 Integer
void drain(Collection<? super Integer> sink) {
    sink.add(42);
}
```

在 SelfBlog 源码里最常见的其实是两者都不需要的情况——`Result<T>`、`IPage<T>` 的 T 由调用点直接确定；需要通配符的场景集中在工具类与框架层（如 `Map<String, ? extends Serializable>` 接收各类参数 Map）。初学阶段记住：**看到 `? extends` 是「只读视图」，看到 `? super` 是「可写容器」，自己写业务先老实写具体类型，泛型不明确再逐步泛化**。

## 1.13 总结

本章介绍了 Java 的基础知识，包括：

1. **Java 语言概述**：特点、版本演进、应用领域、JVM/JRE/JDK 关系
2. **环境搭建**：JDK 21 安装与多版本管理、环境问题排查、Maven Wrapper、IDEA / VS Code
3. **基本语法**：数据类型、变量、运算符（含位运算）、流程控制（含 switch 三种写法与模式匹配）、数组与 Arrays 工具类
4. **面向对象**：类与对象、封装、继承、多态、抽象类、接口、内部类、record、sealed
5. **常用类**：String（不可变性与常量池）、StringBuilder、包装类、日期时间 API
6. **集合框架**：List、Set、Map、泛型、Iterator 与 fail-fast、Collections 工具类、不可变集合
7. **异常处理**：try-catch-finally、try-with-resources、自定义异常、异常最佳实践
8. **IO 流**：字节流、字符流、序列化、NIO.2 Files 工具类
9. **Lambda 与 Stream**：函数式接口、流管道、Collector、并行流陷阱、Optional
10. **注解与反射**：为学习 Spring 打基础
11. **Maven 入门**：pom.xml、依赖管理、生命周期、Maven Wrapper

**给初学者的三点提醒**（都来自 SelfBlog 真实代码）：

- 业务代码 90% 的异常应该是**非受检异常**：抛出业务语义明确的 `BusinessException(404, "文章不存在")`，由全局处理器统一兜底，而不是层层 try-catch
- 资源关闭一律 **try-with-resources**；文件操作优先 **Files 工具类**
- 集合选择先想清楚**要按什么查**：按序号 → ArrayList，按键 → HashMap，去重 → HashSet；怀疑并发就无脑 `ConcurrentHashMap`

通过本章的学习，你应该掌握了 Java 编程的基础知识，能够编写简单的 Java 程序。在接下来的章节中，我们将深入学习 Java 的高级特性，包括多线程、反射、注解、Lambda 表达式和 Stream API 等。

---

**参考资源：**

- [Oracle Java 官方文档](https://docs.oracle.com/en/java/)
- [Java 21 新特性](https://openjdk.org/projects/jdk/21/)
- [SelfBlog 项目源码](https://github.com/Xinghe-0203/SelfBlog)

---

# 第二章 Java 进阶

上一章我们打好了 Java 语法基础，本章进入 Java 进阶部分。这一章的内容量较大，它是从「会写 Java」到「理解 Spring Boot 为什么这样设计」之间最重要的桥梁：

- **多线程与并发**：线程池、锁、内存可见性、异步编排——理解 Spring 的 `@Async`、Tomcat 的请求处理都靠它
- **反射与注解**：Spring 的依赖注入、MyBatis Plus 的 `@TableLogic`、JUnit 的 `@Test` 全部建立在它们之上
- **JVM 基础**：内存区域、垃圾回收、类加载——排查线上问题、读懂启动参数的前提
- **Java 21 新特性**：虚拟线程、记录模式、switch 模式匹配——SelfBlog 使用的正是 Java 21
- **JDBC 与事务**：为第三章理解 MyBatis Plus、`@Transactional` 打基础
- **HTTP 与 RESTful**：为 Spring MVC 的接口设计打基础
- **加密与安全**：BCrypt、JWT——理解 SelfBlog 登录鉴权的原理
- **单元测试与设计模式**：SelfBlog 后端 588 个测试背后的工程方法论

每个知识点都配有可运行的代码示例，并结合 SelfBlog 项目（Spring Boot 3.3.13 + Java 21 + MyBatis Plus + Caffeine）讲解真实用法。

## 2.1 多线程编程

### 2.1.1 线程概述

线程是程序执行的最小单位。Java 提供了丰富的多线程编程支持。

**进程与线程的区别：**

- **进程**：操作系统资源分配的基本单位，拥有独立的内存空间
- **线程**：CPU 调度的基本单位，同一进程内的线程共享内存空间

为什么后端开发离不开多线程？以 SelfBlog 为例：Tomcat 默认用约 200 个工作线程处理 HTTP 请求，也就是说同一时刻可以有 200 个请求被并行处理；文章浏览量更新、性能日志记录等任务也在各自的线程里运行。如果没有多线程，博客只能一个请求处理完再处理下一个，任何一个人打开慢页面，所有人都得排队。

**线程的生命周期（6 种状态）：**

| 状态 | 含义 | 触发方式 |
|------|------|----------|
| NEW | 已创建，尚未启动 | `new Thread(...)` 后 |
| RUNNABLE | 可运行（可能在执行，也可能在等 CPU 时间片） | 调用 `start()` 后 |
| BLOCKED | 等待获取 synchronized 锁 | 竞争锁失败 |
| WAITING | 无限期等待，需被显式唤醒 | `wait()`、`join()`、`LockSupport.park()` |
| TIMED_WAITING | 限时等待 | `sleep(ms)`、`wait(ms)`、`join(ms)` |
| TERMINATED | 执行结束 | `run()` 正常或异常返回 |

**常用方法速查：**

```java
Thread t = new Thread(task, "worker-1");
t.start();                 // 启动线程（让 JVM 调度它，不是立即执行）
// t.run();                // 错误用法！这只是在当前线程里普通地调用方法，不会新开线程
Thread.sleep(100);         // 当前线程暂停 100ms（不释放已持有的锁）
t.join();                  // 当前线程等待 t 执行结束再继续
t.interrupt();             // 给 t 打一个「中断标记」，是否响应由 t 自己决定
t.setDaemon(true);         // 设为守护线程（必须在 start 前调用；JVM 不等守护线程结束）
Thread.currentThread();    // 拿到「当前正在执行这段代码的线程」
```

三个最常见的坑：

1. **`start()` 和 `run()`**：直接调 `run()` 不会产生新线程，只是普通方法调用。
2. **`sleep()` 不释放锁**：线程抱着 synchronized 锁睡觉，其他线程只能干等；`wait()` 才会释放锁。
3. **中断是「协作式」的**：`interrupt()` 只是设置标志位。线程内部要么定期检查 `Thread.currentThread().isInterrupted()`，要么捕获 `InterruptedException` 后决定退出还是忽略。强杀线程的 `Thread.stop()` 早已废弃，因为它可能让对象处于不一致状态。

**Java 中创建线程的方式：**

1. 继承 `Thread` 类
2. 实现 `Runnable` 接口
3. 实现 `Callable` 接口
4. 使用线程池

### 2.1.2 继承 Thread 类

```java
public class ThreadDemo {
    public static void main(String[] args) {
        // 创建线程
        MyThread t1 = new MyThread("线程1");
        MyThread t2 = new MyThread("线程2");
        
        // 启动线程
        t1.start();
        t2.start();
        
        // 主线程继续执行
        for (int i = 0; i < 5; i++) {
            System.out.println("主线程：" + i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class MyThread extends Thread {
    public MyThread(String name) {
        super(name);
    }
    
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(getName() + "：" + i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

继承 `Thread` 的缺点很明显：Java 是单继承，继承了 `Thread` 就无法再继承其他类；而且「线程」和「任务」耦合在一起。因此实际开发中更推荐下面两种方式。

### 2.1.3 实现 Runnable 接口

```java
public class RunnableDemo {
    public static void main(String[] args) {
        // 创建 Runnable 对象
        MyRunnable runnable = new MyRunnable();
        
        // 创建线程
        Thread t1 = new Thread(runnable, "线程1");
        Thread t2 = new Thread(runnable, "线程2");
        
        // 启动线程
        t1.start();
        t2.start();
        
        // 使用 Lambda 表达式
        Thread t3 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Lambda 线程：" + i);
            }
        }, "Lambda线程");
        t3.start();
    }
}

class MyRunnable implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(Thread.currentThread().getName() + "：" + i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

`Runnable` 把「任务」和「线程」解耦了：同一个任务对象可以交给多个线程、交给线程池，甚至虚拟线程（见 2.7.1 节）去执行。这也是为什么线程池的 `execute()` 方法接收的参数类型就是 `Runnable`。

### 2.1.4 实现 Callable 接口

`Callable` 与 `Runnable` 类似，但它可以返回结果并抛出异常。

```java
import java.util.concurrent.*;

public class CallableDemo {
    public static void main(String[] args) throws Exception {
        // 创建 Callable
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                int sum = 0;
                for (int i = 1; i <= 100; i++) {
                    sum += i;
                }
                Thread.sleep(1000);
                return sum;
            }
        };
        
        // 使用 FutureTask 包装
        FutureTask<Integer> futureTask = new FutureTask<>(callable);
        Thread thread = new Thread(futureTask);
        thread.start();
        
        // 获取结果（会阻塞直到结果返回）
        Integer result = futureTask.get();
        System.out.println("计算结果：" + result);
        
        // 使用 Lambda 表达式
        FutureTask<Integer> futureTask2 = new FutureTask<>(() -> {
            int product = 1;
            for (int i = 1; i <= 10; i++) {
                product *= i;
            }
            return product;
        });
        
        new Thread(futureTask2).start();
        System.out.println("10的阶乘：" + futureTask2.get());
    }
}
```

注意 `get()` 会**阻塞调用线程**直到任务完成。它还有带超时的重载：`futureTask.get(3, TimeUnit.SECONDS)`——超时抛出 `TimeoutException`，这在调用外部接口时非常有用，可以避免无限等待拖垮整个调用链。

### 2.1.5 线程池

线程池可以复用线程，减少线程创建和销毁的开销。

线程为什么值得「池化」？因为创建一个线程的成本并不低：要在操作系统内核申请栈空间、注册调度实体，通常耗时毫秒级；高并发下每个请求都新建线程，光是创建销毁的开销就可能压垮系统。线程池提前创建好一批线程反复使用，任务来了就领一个线程执行，执行完归还。

```java
import java.util.concurrent.*;

public class ThreadPoolDemo {
    public static void main(String[] args) {
        // 创建固定大小的线程池
        ExecutorService fixedPool = Executors.newFixedThreadPool(5);
        
        // 创建单线程池
        ExecutorService singlePool = Executors.newSingleThreadExecutor();
        
        // 创建缓存线程池
        ExecutorService cachedPool = Executors.newCachedThreadPool();
        
        // 创建定时任务线程池
        ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(3);
        
        // 提交任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            fixedPool.submit(() -> {
                System.out.println("任务 " + taskId + " 执行，线程：" + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        
        // 定时任务
        scheduledPool.schedule(() -> {
            System.out.println("延迟 3 秒执行");
        }, 3, TimeUnit.SECONDS);
        
        // 周期性任务
        scheduledPool.scheduleAtFixedRate(() -> {
            System.out.println("每 2 秒执行一次");
        }, 0, 2, TimeUnit.SECONDS);
        
        // 关闭线程池
        fixedPool.shutdown();
        singlePool.shutdown();
        cachedPool.shutdown();
        scheduledPool.shutdown();
    }
}
```

`Executors` 的四个快捷工厂方法写起来很方便，但**生产环境不建议直接使用**，原因见下一节的七参数详解——它们的默认参数在生产环境几乎都是定时炸弹。

另外，`shutdown()` 与 `shutdownNow()` 要分清：`shutdown()` 不再接收新任务，但会把队列里的存量任务执行完；`shutdownNow()` 会尝试中断正在执行的任务并返回未执行的任务列表。优雅关闭的标准姿势是先 `shutdown()`，再 `awaitTermination()` 等待，超时才 `shutdownNow()`。

### 2.1.6 ThreadPoolExecutor 七参数详解与拒绝策略

`ThreadPoolExecutor` 是线程池的真正实现，`Executors` 里那些工厂方法只是对它的封装。理解它的一组构造参数，就理解了线程池的全部行为：

```java
public ThreadPoolExecutor(int corePoolSize,          // 1. 核心线程数
                          int maximumPoolSize,       // 2. 最大线程数
                          long keepAliveTime,        // 3. 空闲线程存活时间
                          TimeUnit unit,             // 4. 存活时间单位
                          BlockingQueue<Runnable> workQueue,   // 5. 任务队列
                          ThreadFactory threadFactory,         // 6. 线程工厂
                          RejectedExecutionHandler handler) {  // 7. 拒绝策略
    ...
}
```

**七个参数逐一拆解：**

1. **corePoolSize（核心线程数）**：池中常驻的线程数量。任务来了即使其他核心线程空闲，也只排队而不新建线程——直到线程数达到 corePoolSize。默认核心线程不会回收，除非设置 `allowCoreThreadTimeOut(true)`。
2. **maximumPoolSize（最大线程数）**：线程数的上限。只有当**队列满了**之后，才会继续创建「非核心线程」直到这个上限。
3. **keepAliveTime + 4. unit（存活时间）**：非核心线程空闲超过这个时间就被回收。核心线程默认不受影响。
5. **workQueue（任务队列）**：核心线程忙不过来时，任务先进入这里排队。**必须用有界队列**（如 `ArrayBlockingQueue(100)`），无界队列会让任务无限堆积直到内存溢出。
6. **threadFactory（线程工厂）**：自定义线程的名称、优先级、是否守护线程。给线程起有意义的名字（如 `SelfBlog-async-1`），出问题时日志和线程 dump 才能一眼定位。
7. **handler（拒绝策略）**：队列满且线程数已达上限时，新任务的处理方式。

**任务提交的完整流程**（这个顺序很多人记反，务必注意「先排队、后扩容」）：

```
提交任务 execute(task)
   │
   ├─ 当前线程数 < corePoolSize？ ──是──> 新建核心线程执行
   │            否
   ├─ 队列未满？ ──────────────是──> 入队等待
   │            否
   ├─ 当前线程数 < maximumPoolSize？─是──> 新建非核心线程执行
   │            否
   └─ 执行拒绝策略 handler.rejectedExecution(...)
```

**为什么「队列满了才扩容」反直觉但合理？** 线程是昂贵资源，队列是廉价缓冲。线程池的哲学是：能复用就不新建，能排队就不扩容——只有在队列也扛不住时，才付出创建新线程的代价。

**一个把七个参数用全的示例：**

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolExecutorDemo {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                                     // corePoolSize：常驻 2 个线程
                5,                                     // maximumPoolSize：最多 5 个
                60L, TimeUnit.SECONDS,                 // 非核心线程空闲 60 秒回收
                new ArrayBlockingQueue<>(10),          // 有界队列，容量 10
                new ThreadFactory() {                  // 线程工厂：起个能认出来的名字
                    private final AtomicInteger seq = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "biz-pool-" + seq.getAndIncrement());
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        );

        for (int i = 1; i <= 20; i++) {
            final int taskId = i;
            executor.execute(() -> {
                System.out.println("任务 " + taskId + " 由 " + Thread.currentThread().getName() + " 执行");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 优雅关闭：先不再收新任务，等存量任务跑完，超时才强停
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

**四种内置拒绝策略：**

| 策略 | 行为 | 适用场景 |
|------|------|----------|
| `AbortPolicy`（默认） | 抛出 `RejectedExecutionException` | 希望调用方明确感知失败并自行处理 |
| `CallerRunsPolicy` | 让提交任务的线程自己执行 | 希望「自动降速背压」，不丢任务（SelfBlog 异步线程池用的就是它） |
| `DiscardPolicy` | 静默丢弃新任务 | 允许丢失的日志上报、埋点类任务 |
| `DiscardOldestPolicy` | 丢弃队首最老任务，为新任务腾位置 | 只关心最新数据的场景 |

拒绝策略也可以自定义——实现 `RejectedExecutionHandler` 接口即可，比如先记日志再决定补偿方案：

```java
RejectedExecutionHandler handler = (r, pool) -> {
    System.err.println("线程池已饱和，队列大小：" + pool.getQueue().size()
            + "，活动线程：" + pool.getActiveCount());
    // 兜底方案：同步执行、写消息队列、落库重试……根据业务选择
    throw new RejectedExecutionException("任务被拒绝");
};
```

**为什么不用 `Executors` 的快捷方法？** 阿里巴巴《Java 开发手册》对此有强制规定：

- `newFixedThreadPool` / `newSingleThreadExecutor`：使用**无界的** `LinkedBlockingQueue`，任务堆积时内存会被慢慢吃光，最终 OOM；
- `newCachedThreadPool` / `newScheduledThreadPool`：最大线程数是 `Integer.MAX_VALUE`，突发流量下可能创建海量线程，把 CPU 和内存打爆。

**线程数如何估算？** 经验公式：CPU 密集型任务约为「核心数 + 1」，IO 密集型任务约为「核心数 × (1 + 等待时间/计算时间)」。例如 SelfBlog 的异步任务大多是写日志、刷缓存这类 IO 操作，等待占比高，线程数可以设得比 CPU 核心数大不少。但公式只是起点，**最终以压测为准**。

### 2.1.7 线程同步：synchronized 与 Lock

当多个线程访问共享资源时，需要同步机制来保证数据一致性。

先看为什么需要同步：`count++` 看似一行代码，实际是「读出 count → 加 1 → 写回」三步操作。两个线程同时读到同一个旧值、各自加 1、先后写回，结果就会少加一次——这就是**竞态条件**。

**synchronized 关键字：**

```java
public class SynchronizedDemo {
    private int count = 0;
    
    // 同步方法
    public synchronized void increment() {
        count++;
    }
    
    // 同步代码块
    public void incrementBlock() {
        synchronized (this) {
            count++;
        }
    }
    
    // 同步静态方法
    public static synchronized void staticMethod() {
        // 同步代码
    }
    
    public static void main(String[] args) throws InterruptedException {
        SynchronizedDemo demo = new SynchronizedDemo();
        
        // 创建 1000 个线程，每个线程增加 1000 次
        Thread[] threads = new Thread[1000];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    demo.increment();
                }
            });
        }
        
        // 启动所有线程
        for (Thread t : threads) {
            t.start();
        }
        
        // 等待所有线程完成
        for (Thread t : threads) {
            t.join();
        }
        
        System.out.println("最终 count：" + demo.count);  // 1000000
    }
}
```

把上面代码里的 `synchronized` 去掉再跑一次，结果几乎必然小于 1000000，而且每次都不同——这就是不同步的代价。

`synchronized` 的三种用法锁的对象不同，要分清：

- **同步方法** `synchronized void m()`：锁的是当前实例 `this`——不同实例之间互不影响；
- **同步代码块** `synchronized (lock)`：锁的是括号里指定的对象，**推荐这种**，因为锁的范围越小，性能越好；
- **同步静态方法** `static synchronized void m()`：锁的是类的 Class 对象，全局只有一把。

**Lock 接口：**

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockDemo {
    private int count = 0;
    private Lock lock = new ReentrantLock();
    
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();  // 必须在 finally 中释放锁
        }
    }
    
    public int getCount() {
        return count;
    }
    
    public static void main(String[] args) throws InterruptedException {
        LockDemo demo = new LockDemo();
        
        Thread[] threads = new Thread[1000];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    demo.increment();
                }
            });
        }
        
        for (Thread t : threads) {
            t.start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        System.out.println("最终 count：" + demo.getCount());
    }
}
```

`unlock()` 必须写在 `finally` 里——否则临界区一旦抛异常，锁永远无法释放，所有等待线程永久卡死。

**synchronized 与 ReentrantLock 怎么选？**

| 维度 | synchronized | ReentrantLock |
|------|--------------|---------------|
| 层级 | JVM 内置关键字 | JDK 类库（`java.util.concurrent.locks`） |
| 释放锁 | 自动（出临界区即释放，包括异常） | 手动 `unlock()`，必须放 `finally` |
| 可重入 | 支持（同一线程可重复获取同一把锁） | 支持 |
| 公平锁 | 不支持（只有非公平） | 可选 `new ReentrantLock(true)` |
| 可中断 | 不可（等锁时 `interrupt()` 无效） | `lockInterruptibly()` 支持等锁时被中断 |
| 超时获取 | 不支持 | `tryLock(3, TimeUnit.SECONDS)` |
| 条件变量 | 一个（`wait/notify`） | 多个 `Condition`，可分组唤醒 |
| 性能 | JDK 6 之后经大量优化，差距很小 | 相当 |

结论：**默认用 `synchronized`**（简单、不会忘记释放、JVM 还能持续优化它）；只有需要「可中断、超时、公平、多条件」这些高级特性时才用 `ReentrantLock`。

**顺带了解：synchronized 的锁优化。** JDK 6 之后 synchronized 并不「重」：JVM 会根据竞争程度让锁逐步升级——无锁 → 偏向锁（只有一个线程访问时，仅记录线程 ID）→ 轻量级锁（CAS 自旋）→ 重量级锁（操作系统互斥量）。JDK 15 起偏向锁已被默认废弃，但轻量级锁优化依然有效。理解「锁升级」的意义在于：**无竞争或低竞争的 synchronized 开销非常小**，不要因为「怕慢」而滥用各种无锁技巧。

**线程间协作：wait / notify。** 锁解决「互斥」，`wait/notify` 解决「协作」——一个线程等待条件，另一个线程满足条件后唤醒它：

```java
public class WaitNotifyDemo {
    private static final Object lock = new Object();
    private static boolean ready = false;

    public static void main(String[] args) throws InterruptedException {
        // 等待方
        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                while (!ready) {          // 必须 while 而不是 if，防止虚假唤醒
                    try {
                        lock.wait();      // 释放锁并进入等待（这是它和 sleep 的关键区别）
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("条件满足，继续执行");
            }
        });
        waiter.start();

        Thread.sleep(100);
        // 通知方
        synchronized (lock) {
            ready = true;
            lock.notifyAll();             // 唤醒所有等待该锁的线程
        }
    }
}
```

要点：`wait/notify` 必须在 synchronized 块内调用（否则抛 `IllegalMonitorStateException`）；判断条件要用 `while` 循环包裹；优先 `notifyAll()` 而不是 `notify()`，避免唤醒了「错的线程」导致信号丢失。日常开发中更推荐 `java.util.concurrent` 里的现成工具（`BlockingQueue`、`CountDownLatch` 等），很少需要手写 wait/notify。

### 2.1.8 volatile 与内存可见性

先看一个「看起来永远不会停」的程序：

```java
public class VolatileDemo {
    // 去掉 volatile 试一试：子线程很可能永远感知不到 flag 变化，死循环
    private static volatile boolean flag = false;

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (!flag) {
                // 忙等待
            }
            System.out.println("worker 感知到 flag 变化，退出循环");
        });
        worker.start();

        Thread.sleep(1000);
        flag = true;   // 主线程修改
        System.out.println("main 已把 flag 置为 true");
    }
}
```

**为什么需要 volatile？** 为了性能，每个线程并不直接读写主内存，而是先操作自己工作内存（CPU 缓存）里的副本，何时同步回主内存是不确定的。线程 A 修改变量后，线程 B 可能长时间读到的还是旧值——这就是**可见性问题**。`volatile` 强制：写操作立即刷回主内存，读操作直接从主内存读，保证一个线程的写对其他线程立即可见。

**volatile 的保证与不保证：**

1. **可见性**：一个线程修改后，其他线程立即可见（上面的示例）。
2. **有序性**：禁止指令重排。编译器和 CPU 为了优化会把指令打乱顺序，volatile 通过插入内存屏障阻止跨过它的重排。
3. **不保证原子性**：`volatile int count` 上的 `count++` 依然是「读-改-写」三步，两个线程交错执行仍会丢更新。需要原子性请用 `AtomicInteger`（内部基于 CAS，见下文）或加锁。

指令重排最著名的坑出现在双重检查锁单例（2.12.1 节的 `SingletonLazy`）中：`instance = new SingletonLazy()` 实际分三步——分配内存、初始化对象、把引用指向内存。第 2、3 步可能被重排，导致另一个线程拿到一个「非 null 但还没初始化完」的对象。所以 DCL 里的 `instance` 必须加 `volatile`。

**volatile vs Atomic vs synchronized：**

```java
private volatile boolean running = true;        // 状态开关：volatile 够用
private final AtomicInteger counter = new AtomicInteger();  // 计数：CAS 原子类
private int shared = 0;                          // 复合逻辑：synchronized / Lock
```

`AtomicInteger` 的原理是 **CAS（Compare-And-Set）**：一条 CPU 原子指令，语义是「如果内存中的值还等于期望值 E，就把它改成 V，否则失败重试」。它是无锁算法的基础，冲突少时性能远高于加锁；代价是冲突激烈时自旋重试浪费 CPU，且存在「ABA 问题」（值经历了 A→B→A，CAS 感知不到中间变化，必要时用 `AtomicStampedReference` 加版本号）。

**happens-before 一句话版**：JMM（Java 内存模型）给程序员提供的核心承诺——如果操作 A happens-before 操作 B，那么 A 的结果对 B 可见。程序顺序规则、锁的解锁先于加锁、volatile 写先于读等都是 happens-before 规则。初学阶段不必背诵全部规则，记住「跨线程共享的可变状态，要么 volatile/原子类，要么锁」即可。

**SelfBlog 中的对应物**：Web 应用里大量「共享状态」其实被托管给了框架——请求上下文存在 ThreadLocal（下一节）、缓存交给 Caffeine、计数交给数据库。自己写 `volatile` 的机会不多，但理解它是看懂 JDK 源码与并发面试题的基本功。

### 2.1.9 ThreadLocal：原理与内存泄漏风险

`ThreadLocal` 提供「线程封闭」：同一个变量，每个线程看到的是自己独立的副本，天然线程安全。

```java
import java.text.SimpleDateFormat;
import java.util.Date;

public class ThreadLocalDemo {
    // 场景一：让非线程安全的对象变成线程安全（SimpleDateFormat 是经典案例）
    private static final ThreadLocal<SimpleDateFormat> FORMATTER =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    // 场景二：贯穿调用链的「隐式上下文」——典型如当前登录用户
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    public static void main(String[] args) {
        Runnable task = () -> {
            try {
                CURRENT_USER_ID.set(Thread.currentThread().getId());
                // 整条调用链上的任何代码都能直接读到当前用户，不需要层层传参
                String now = FORMATTER.get().format(new Date());
                System.out.println("线程 " + CURRENT_USER_ID.get() + " 格式化时间：" + now);
            } finally {
                FORMATTER.remove();       // 用完必须清理！原因见下文
                CURRENT_USER_ID.remove();
            }
        };
        new Thread(task, "T-1").start();
        new Thread(task, "T-2").start();
    }
}
```

**底层原理**：值并不存在 `ThreadLocal` 对象里，而是存在每个 `Thread` 对象内部的 `ThreadLocalMap` 里：

```
Thread 对象
 └─ ThreadLocal.ThreadLocalMap threadLocals      // 每个 Thread 一张
     ├─ Entry[ key = ThreadLocal 对象(弱引用), value = 值(强引用) ]
     └─ Entry[ key = ThreadLocal 对象(弱引用), value = 值(强引用) ]
```

`ThreadLocal.set(v)` 实际是 `Thread.currentThread().threadLocals.set(this, v)`——key 是 ThreadLocal 自己，value 是你放的值。所以「线程隔离」的本质是：**数据挂在各自线程的 Map 上，key 相同、value 各异**。

**内存泄漏风险**：这张 Map 的 Entry 的 key 是**弱引用**。当外部不再持有某个 ThreadLocal 的强引用时（比如静态字段被清空），key 会在下次 GC 后变成 null，但 value 是强引用，仍然挂在 Map 里——这条 `key=null` 的「陈旧条目」只有等这张 Map 后续被操作时才可能被顺手清理。在**线程池**场景下问题被放大：线程长期存活、反复复用，Map 一直不销毁，泄漏的 value 就会持续积累。

**两条铁律**：

1. **用完必调 `remove()`**，而且放在 `finally` 里；
2. **线程池中更隐蔽的危害是「脏数据」**：线程复用后，下一个任务会读到上一个任务 set 的值。想象异步线程池里的任务 A 设置了 `CURRENT_USER_ID = 1001` 而没清理，任务 B 原本应该读到 null，却拿到了 1001——安全漏洞就是这么来的。

**框架里的 ThreadLocal**：Spring MVC 的 `RequestContextHolder`、Spring Security 的 `SecurityContextHolder`，底层都是 ThreadLocal——这就是为什么 `SecurityUtils.getCurrentUserId()` 可以在任何一层代码里「凭空」拿到当前用户：请求线程在过滤器里把用户信息放进了 ThreadLocal。框架在请求结束时负责清理；自己写 ThreadLocal 时，清理责任就在你自己。

补充两点：线程池中想「从父线程传值到子线程」，用 `InheritableThreadLocal` 也不够（线程池复用线程，不总是新建），生产级方案是阿里的 `TransmittableThreadLocal`；虚拟线程（2.7.1 节）数量可达百万级，每个都带一张 ThreadLocalMap，滥用同样会造成内存压力——JDK 官方建议虚拟线程场景减少 ThreadLocal 使用。

### 2.1.10 并发集合

Java 提供了线程安全的并发集合类。

```java
import java.util.concurrent.*;
import java.util.*;

public class ConcurrentCollectionDemo {
    public static void main(String[] args) {
        // ConcurrentHashMap：线程安全的 HashMap
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put("Java", 1);
        concurrentMap.put("Python", 2);
        concurrentMap.putIfAbsent("Go", 3);
        concurrentMap.computeIfPresent("Java", (k, v) -> v + 10);
        System.out.println("ConcurrentHashMap: " + concurrentMap);
        
        // CopyOnWriteArrayList：线程安全的 ArrayList
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
        cowList.add("A");
        cowList.add("B");
        cowList.add("C");
        System.out.println("CopyOnWriteArrayList: " + cowList);
        
        // BlockingQueue：阻塞队列
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(10);
        
        // 生产者
        new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    queue.put("消息" + i);
                    System.out.println("生产：消息" + i);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        
        // 消费者
        new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    String msg = queue.take();
                    System.out.println("消费：" + msg);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        
        // ConcurrentSkipListMap：跳表实现的有序 Map
        ConcurrentSkipListMap<Integer, String> skipListMap = new ConcurrentSkipListMap<>();
        skipListMap.put(3, "C");
        skipListMap.put(1, "A");
        skipListMap.put(2, "B");
        System.out.println("ConcurrentSkipListMap: " + skipListMap);
    }
}
```

**选型要点（比记住 API 更重要）：**

- **ConcurrentHashMap**：读几乎无锁，写按桶加细粒度锁（JDK 8 后是 CAS + synchronized）。注意 `size()` 在并发场景只是估值；复合操作（先判断再写入）要用 `putIfAbsent`、`computeIfAbsent` 这类原子方法，而不是自己写 `if (!map.containsKey(k)) map.put(k, v)`——两行之间照样有竞态。
- **CopyOnWriteArrayList**：每次修改都复制整个底层数组，适合「读极多、写极少」的配置列表、监听器列表；频繁写则性能崩溃。
- **BlockingQueue**：`put()` 队列满时阻塞、`take()` 队列空时阻塞，天然是生产者-消费者模型——线程池内部的任务队列就是它（`ThreadPoolExecutor` 第 5 个参数）。
- 对比记忆：`Collections.synchronizedMap(new HashMap<>())` 只是把所有方法套上 synchronized，性能远差于 ConcurrentHashMap；`Hashtable` 则是远古遗产，直接不碰。

### 2.1.11 CompletableFuture

`CompletableFuture` 是 Java 8 引入的异步编程工具。

```java
import java.util.concurrent.*;

public class CompletableFutureDemo {
    public static void main(String[] args) throws Exception {
        // 创建异步任务
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Hello";
        });
        
        // 链式调用
        CompletableFuture<String> future2 = future1.thenApply(s -> s + " World");
        System.out.println(future2.get());  // Hello World
        
        // 组合多个任务
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> "Hello")
            .thenCombine(CompletableFuture.supplyAsync(() -> " World"), (s1, s2) -> s1 + s2);
        System.out.println(future3.get());  // Hello World
        
        // 异常处理
        CompletableFuture<String> future4 = CompletableFuture.supplyAsync(() -> {
            if (true) {
                throw new RuntimeException("出错了");
            }
            return "Success";
        }).exceptionally(ex -> {
            System.out.println("异常：" + ex.getMessage());
            return "Default";
        });
        System.out.println(future4.get());  // Default
        
        // 并行执行多个任务
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            CompletableFuture.runAsync(() -> System.out.println("任务1")),
            CompletableFuture.runAsync(() -> System.out.println("任务2")),
            CompletableFuture.runAsync(() -> System.out.println("任务3"))
        );
        allFutures.get();  // 等待所有任务完成
        
        // 任意一个完成即可
        CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(
            CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                return "任务1完成";
            }),
            CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(500); } catch (InterruptedException e) {}
                return "任务2完成";
            })
        );
        System.out.println(anyFuture.get());  // 任务2完成
    }
}
```

先把这一家子的命名规律理清楚，API 就不用死记了：

- 后缀 **Apply**：转换结果，有入参有返回（`thenApply`、`handle`）；
- 后缀 **Accept**：消费结果，有入参无返回（`thenAccept`）；
- 后缀 **Run**：不关心上一步结果，只接着干活（`thenRun`）；
- **Async** 结尾的重载：这一步在新线程执行，否则通常由前一步的完成线程执行；
- **Compose**：衔接「本身返回 CompletableFuture」的异步调用，避免出现 `CompletableFuture<CompletableFuture<T>>` 套娃；**Combine**：合并两个独立任务的结果。

### 2.1.12 CompletableFuture 异步编排实战：结合 Spring @Async

上一节示例里的 `supplyAsync` 有个生产环境陷阱：**不指定线程池时，任务全部跑在 `ForkJoinPool.commonPool()` 上**——这个公共池的并行度默认是「CPU 核心数 − 1」，全 JVM 共享。如果你的异步任务里夹着慢 IO（查库、调第三方接口），就会把公共池占满，拖累所有使用它的代码（包括并行流，见 2.5.5 节）。

正确姿势是把自定义线程池传进去：

```java
ExecutorService bizPool = new ThreadPoolExecutor(
        4, 8, 60L, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(100),
        new ThreadPoolExecutor.CallerRunsPolicy());

CompletableFuture<List<String>> posts =
        CompletableFuture.supplyAsync(() -> queryHotPosts(), bizPool);
CompletableFuture<Long> views =
        CompletableFuture.supplyAsync(() -> sumViews(), bizPool);

// 两个查询并行跑，总耗时 ≈ max(查文章, 查浏览量)，而不是二者相加
CompletableFuture.allOf(posts, views).join();
System.out.println(posts.join() + " / " + views.join());
```

**在 Spring Boot 中，这件事通常换一种写法：`@Async`。**

第一步，定义业务线程池（SelfBlog 的做法，见 2.13.2 节的 `AsyncConfig`）：

```java
@Configuration
public class AsyncConfig {
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("SelfBlog-");
        // 队列满且线程满时，让调用线程自己执行：天然限流，不丢任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

第二步，在方法上标注 `@Async("taskExecutor")`，指定用这个池：

```java
@Service
public class DashboardQueryService {

    @Async("taskExecutor")
    public CompletableFuture<List<PostVO>> loadHotPosts() {
        // 这里运行在 SelfBlog- 线程池里，返回值被包装成 CompletableFuture
        List<PostVO> hot = postQueryService.hotPosts(10);
        return CompletableFuture.completedFuture(hot);
    }

    @Async("taskExecutor")
    public CompletableFuture<Long> loadTotalViews() {
        return CompletableFuture.completedFuture(statsService.totalViews());
    }
}
```

第三步，在调用方自由编排：

```java
CompletableFuture<List<PostVO>> posts = dashboardQueryService.loadHotPosts();
CompletableFuture<Long> views = dashboardQueryService.loadTotalViews();
CompletableFuture.allOf(posts, views).join();     // 两个异步任务并行

DashboardVO vo = new DashboardVO(posts.join(), views.join());
```

**使用 @Async 必须知道的四件事：**

1. **开启开关**：在配置类上加 `@EnableAsync`（Spring Boot 项目加一次即可）。
2. **自调用失效**：同一个类里 `this.someAsyncMethod()` 不走代理、不会异步。原因和 `@Transactional` 一样——`@Async` 靠动态代理拦截（见 2.2.5 节），绕过代理就绕过了切面。解决办法是把异步方法挪到另一个 Bean。
3. **返回值类型**：`void`（发射后不管）、`CompletableFuture<T>` / `Future<T>`（可等待结果）。`void` 任务的异常不会抛回调用方，默认被记日志吞掉，重要任务建议实现 `AsyncUncaughtExceptionHandler` 统一处理。
4. **默认池的问题**：不指定名字时使用 Spring Boot 自动配置的 `applicationTaskExecutor`；「什么都不配直接用 @Async」在多核机器上勉强能用，但显式定义业务线程池是更可控的工程实践。

实际场景举例：SelfBlog 管理端首页需要「热门文章 + 浏览统计」等好几份数据，串行查库是多次往返相加；用 CompletableFuture 把几个查询扔进线程池并行执行，接口耗时从「各次之和」降为「最慢的一次」——这是异步编排最典型、收益最直接的用法。

## 2.2 反射机制

### 2.2.1 反射概述

反射是指在运行时动态地获取类的信息并操作对象的能力。

正常代码在编译期就确定了「调用谁的哪个方法」；反射把这件事推迟到运行时——拿到一个 `Class` 对象，就能查它的构造器、字段、方法，甚至绕过 private 限制直接操作。听起来「破坏封装」，但它是一切 Java 框架的地基：

- **Spring**：扫描 `@Component` 类名 → 反射创建实例 → 反射读取字段/方法上的 `@Autowired`、`@Value` 完成注入；
- **MyBatis Plus**：反射读取实体类的 `@TableField`、`@TableLogic` 注解，自动拼出 `WHERE is_deleted = 0`；
- **Jackson**：反射读写对象属性，完成 JSON 序列化；
- **JUnit**：反射调用你标注了 `@Test` 的方法。

反射的代价也要心里有数：方法查找需要遍历，参数要装箱拆箱，JIT 难以深度优化，所以**反射比直接调用慢**。框架的通用解法是「启动时反射一次，缓存 `Method`/`Field` 对象，后续复用」；JDK 还提供 `MethodHandle` 与 Lambda 元工厂（`LambdaMetafactory`）作为更快的替代。作为业务开发者，你写反射的机会很少，但读懂框架源码离不开它。

### 2.2.2 获取 Class 对象

```java
public class ReflectionDemo {
    public static void main(String[] args) throws Exception {
        // 方式1：类名.class
        Class<?> clazz1 = String.class;
        
        // 方式2：对象.getClass()
        String str = "Hello";
        Class<?> clazz2 = str.getClass();
        
        // 方式3：Class.forName()
        Class<?> clazz3 = Class.forName("java.lang.String");
        
        System.out.println("clazz1: " + clazz1.getName());
        System.out.println("clazz2: " + clazz2.getName());
        System.out.println("clazz3: " + clazz3.getName());
        
        // 基本类型的 Class
        int.class;
        Integer.TYPE;
        
        // 数组的 Class
        int[].class;
        String[].class;
    }
}
```

三种方式拿到的都是同一个 Class 对象（每个类在 JVM 中只有一份），`clazz1 == clazz2 == clazz3` 为 true。区别在适用场景：`类名.class` 编译期确定，最快；`对象.getClass()` 手里已有实例；`Class.forName()` 只有字符串（典型如配置文件里写的类名、JDBC 驱动名），且会**触发类加载与静态初始化**——早期的 JDBC 代码都要写一句 `Class.forName("com.mysql.cj.jdbc.Driver")` 就是利用这个副作用注册驱动（JDBC 4.0 起 SPI 自动加载，这句已可省略）。

### 2.2.3 反射操作

```java
import java.lang.reflect.*;

public class ReflectionOperationDemo {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("java.lang.String");
        
        // 获取构造方法
        Constructor<?>[] constructors = clazz.getConstructors();
        for (Constructor<?> constructor : constructors) {
            System.out.println("构造方法：" + constructor);
        }
        
        // 获取方法
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            System.out.println("方法：" + method.getName());
        }
        
        // 获取字段
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            System.out.println("字段：" + field.getName() + "，类型：" + field.getType());
        }
        
        // 创建对象
        Object obj = clazz.getDeclaredConstructor(String.class).newInstance("Hello");
        System.out.println("创建的对象：" + obj);
        
        // 调用方法
        Method lengthMethod = clazz.getMethod("length");
        Object result = lengthMethod.invoke(obj);
        System.out.println("调用 length() 结果：" + result);
        
        // 访问私有字段
        Field valueField = clazz.getDeclaredField("value");
        valueField.setAccessible(true);  // 设置可访问
        Object value = valueField.get(obj);
        System.out.println("私有字段 value：" + value);
    }
}
```

几个易混 API 的规律：`getXxx()` 只能拿到 **public 及继承来的**成员；`getDeclaredXxx()` 拿到**本类声明的全部**（含 private，但不含继承的）。要操作 private 成员，先 `setAccessible(true)` 关闭访问检查。

**`Method.invoke` 详解**——这是反射最核心的一个方法，签名是：

```java
Object invoke(Object obj, Object... args) throws IllegalAccessException,
        IllegalArgumentException, InvocationTargetException
```

- 第一个参数是**接收者**：调用实例方法时传对象，调用静态方法传 `null`；
- 可变参数传入实参，基本类型要装箱（`int` → `Integer`），所以反射调用频繁装箱拆箱、影响性能；
- 目标方法内部抛出的异常被包在 `InvocationTargetException` 里，需要 `e.getCause()` 才能拿到真实异常——读框架日志时经常看到它，别被它骗了，真正的错误在 cause 里；
- JDK 9+ 的模块系统下，对未开放模块的私有成员调用 `setAccessible(true)` 会抛 `InaccessibleObjectException`，这就是为什么一些老框架在新 JDK 上要加 `--add-opens` 启动参数。

一个「反射调用 + 缓存 Method」的迷你示例，模拟框架的常见写法：

```java
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MethodCache {
    private static final Map<String, Method> CACHE = new ConcurrentHashMap<>();

    public static Object call(Object target, String methodName, Object... args) throws Exception {
        Method m = CACHE.computeIfAbsent(target.getClass().getName() + "#" + methodName, k -> {
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new IllegalArgumentException("方法不存在：" + methodName);
        });
        return m.invoke(target, args);   // 复用缓存的 Method，避免每次遍历查找
    }
}
```

### 2.2.4 动态代理

动态代理是反射的重要应用，Spring AOP 的底层实现就是动态代理。

先想清楚要解决的问题：给 100 个方法的执行前后都加上日志/权限/耗时统计，难道要写 100 遍 try-finally？静态代理要为每个类手写包装类，照样繁琐。动态代理在**运行期**自动生成代理类，把「增强逻辑」集中写在一处。

```java
import java.lang.reflect.*;

// 接口
interface UserService {
    void addUser(String name);
    String getUser(int id);
}

// 实现类
class UserServiceImpl implements UserService {
    @Override
    public void addUser(String name) {
        System.out.println("添加用户：" + name);
    }
    
    @Override
    public String getUser(int id) {
        return "用户" + id;
    }
}

// 动态代理
class ProxyHandler implements InvocationHandler {
    private Object target;
    
    public ProxyHandler(Object target) {
        this.target = target;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("方法调用前：" + method.getName());
        Object result = method.invoke(target, args);
        System.out.println("方法调用后：" + method.getName());
        return result;
    }
}

public class DynamicProxyDemo {
    public static void main(String[] args) {
        // 创建目标对象
        UserService target = new UserServiceImpl();
        
        // 创建代理对象
        UserService proxy = (UserService) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            new ProxyHandler(target)
        );
        
        // 调用代理方法
        proxy.addUser("张三");
        String user = proxy.getUser(1);
        System.out.println("获取用户：" + user);
    }
}
```

`Proxy.newProxyInstance` 在运行期动态生成了一个 `$Proxy0` 类（继承 `Proxy`、实现 `UserService`），它把每个方法调用都转发给 `InvocationHandler.invoke()`。于是「方法调用前后做点什么」只需写一次。这正是 2.13.1 节 SelfBlog `PerformanceMonitoringAspect` 的工作原理：你在 `@Around` 里写的增强逻辑，被 Spring 织入到代理对象里。

### 2.2.5 JDK 动态代理与 CGLIB：Spring AOP 的两条路线

Spring AOP 只有两种代理手段，理解它们的差异，很多「AOP 不生效」的问题就能自己诊断。

**CGLIB 代理示例**（Spring 已把 CGLIB 内置到 `spring-core` 中，包名为 `org.springframework.cglib`）：

```java
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

class UserServiceImpl {   // 注意：没有任何接口
    public void addUser(String name) {
        System.out.println("添加用户：" + name);
    }
}

public class CglibDemo {
    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(UserServiceImpl.class);          // 生成目标类的子类
        enhancer.setCallback((MethodInterceptor) (obj, method, params, proxy) -> {
            System.out.println("前置增强：" + method.getName());
            Object result = proxy.invokeSuper(obj, params);      // 调用父类（即目标）方法
            System.out.println("后置增强");
            return result;
        });

        UserServiceImpl proxy = (UserServiceImpl) enhancer.create();
        proxy.addUser("李四");
    }
}
```

**两者对比：**

| 维度 | JDK 动态代理 | CGLIB |
|------|--------------|-------|
| 实现基础 | 接口 + 反射（运行期生成实现同接口的 `$Proxy0`） | 继承（运行期生成目标类的子类） |
| 硬性要求 | 目标类必须实现接口 | 类不能是 `final`，被拦截的方法不能是 `final`/`private`/`static` |
| 方法调用方式 | `Method.invoke`（反射调用，参数装箱） | `FastClass` 机制按下标直接调用，无需反射 |
| 代理对象的「身份」 | 只「像」目标类型（实现了相同接口） | 「就是」目标类型的子类，可强转到目标类 |
| 无法增强的场景 | 目标没有接口时完全不可用 | 构造器会被调用两次（生成子类实例），final 方法直接绕过代理 |

**Spring AOP 的选择逻辑**：默认情况下，目标类实现了接口就用 JDK 动态代理，否则用 CGLIB。而 Spring Boot 从 2.x 起把 `spring.aop.proxy-target-class` 默认设为 `true`，即**一律使用 CGLIB**——接口的有无不影响代理行为，行为更一致。

**这解释了两个经典现象：**

1. **`this` 调用不走切面**：代理增强发生在「外部经代理对象调用」时；类内部 `this.method()` 用的是原始对象，`@Transactional`、`@Async`、`@Cacheable`、自定义 `@Aspect` 全部失效。解决办法：拆到另一个 Bean，或注入自身代理（`AopContext.currentProxy()`，需开启 `exposeProxy`）。
2. **final 方法上的注解失效**：CGLIB 靠子类重写来织入，`final` 方法无法被重写，只能原样放行。所以不要把标注 `@Transactional` 的方法写成 `final`。

顺带一提 SelfBlog 的真实架构：文章与动态的「门面 Service 把查询/交互方法一行委托出去并重新标注 `@Transactional`」，委托调用发生在**跨 Bean** 的场景下，正好避开了自调用陷阱——这既是设计模式问题（2.12 节），也是代理机制问题。

## 2.3 注解

### 2.3.1 注解概述

注解是 Java 5 引入的一种元数据形式，可以为代码添加额外的信息。

把这句话展开：**注解本身不包含任何业务逻辑，它只是贴在类、方法、字段上的「标签」**。贴上标签之后，如果没有任何代码去读取这个标签，它就只是一个装饰；一旦有框架（或你自己写的代码）在运行期读到它，标签就开始「指挥」程序行为。这就是为什么注解要和 2.2 节的反射放在一起学——**注解定义了「是什么」，反射负责「读出来」，二者的组合是几乎所有 Java 框架的地基**：

- JUnit 读到方法上的 `@Test`，才知道这个方法是一个测试用例（2.11 节正式学习）；
- Spring 读到类上的 `@Service`，才知道要把它注册成一个 Bean；
- MyBatis Plus 读到字段上的 `@TableLogic`，才知道查询时要自动拼上 `is_deleted = 0`；
- Spring Security 读到方法上的 `@PreAuthorize("hasRole('admin')")`，才知道调用前要检查权限。

从语法上看，注解是一种特殊的接口：定义注解用 `@interface`，编译后同样生成 `.class` 文件，所有注解都隐式继承自 `java.lang.annotation.Annotation`。但使用注解不需要 `implements`，直接用 `@注解名` 标注即可。

注解按照「信息保留到什么时候」分为三类，由元注解 `@Retention` 控制（下一小节马上讲到）：

| 保留策略 | 保留到 | 典型例子 | 谁来处理 |
|----------|--------|----------|----------|
| `SOURCE` | 只在源码里，编译后丢弃 | `@Override`、Lombok 的 `@Data` | 编译器或注解处理器 |
| `CLASS` | 保留到 class 文件，运行期不可见 | （默认值，实际很少用） | 字节码工具（如 ASM） |
| `RUNTIME` | 一直保留，运行期可反射读取 | `@Test`、`@Deprecated` | 反射代码、Spring 等框架 |

> 记住一个高频结论：**想在运行期用反射读到的注解，必须声明 `@Retention(RetentionPolicy.RUNTIME)`**。忘了加它是自定义注解最常见的 bug——代码不报错，只是注解「莫名其妙不生效」。

### 元注解：注解上的注解

定义注解时套在它头上的注解叫元注解（meta-annotation），JDK 内置五个：

**① `@Target`——这个注解能贴在哪里**

| ElementType 取值 | 能标注的位置 |
|------------------|--------------|
| `TYPE` | 类、接口、枚举 |
| `METHOD` | 方法 |
| `FIELD` | 字段（含枚举常量） |
| `PARAMETER` | 方法参数 |
| `CONSTRUCTOR` | 构造器 |
| `LOCAL_VARIABLE` | 局部变量 |
| `ANNOTATION_TYPE` | 注解类型本身 |
| `PACKAGE` | 包（写在 package-info.java） |

不写 `@Target` 则默认「哪里都能贴」；要限定多个位置时用数组形式 `{ElementType.METHOD, ElementType.TYPE}`（见 2.3.3 的示例）。

**② `@Retention`——信息保留到什么时候**

即上表的三种策略，`RUNTIME` 是框架开发的事实标准。

**③ `@Documented`——出现在 Javadoc 里**

加上它，`javadoc` 工具生成的文档会包含该注解。

**④ `@Inherited`——子类继承父类类上的注解**

```java
import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@interface Tag { }

@Tag
class Parent { }

class Child extends Parent { }   // Child 没有直接标注 @Tag

// Child.class.getAnnotation(Tag.class) != null —— 通过继承拿到了
```

注意局限：`@Inherited` 只对**类继承**生效，接口实现和方法重写**不继承**注解。

**⑤ `@Repeatable`——同一个位置可重复标注**

```java
import java.lang.annotation.*;

@Repeatable(Schedules.class)
@Retention(RetentionPolicy.RUNTIME)
@interface Schedule { String cron(); }

@Retention(RetentionPolicy.RUNTIME)
@interface Schedules { Schedule[] value(); }

@Schedule(cron = "0 0 2 * * ?")
@Schedule(cron = "0 30 2 * * ?")
class BackupJob { }   // 同一个类上贴了两张同类型标签
```

### 2.3.2 内置注解

```java
public class BuiltInAnnotationDemo {
    // @Override：标记方法重写父类方法
    @Override
    public String toString() {
        return "BuiltInAnnotationDemo";
    }
    
    // @Deprecated：标记已过时
    @Deprecated
    public void oldMethod() {
        System.out.println("此方法已过时");
    }
    
    // @SuppressWarnings：抑制编译器警告
    @SuppressWarnings("unchecked")
    public void suppressWarning() {
        List list = new ArrayList();
        list.add("Hello");
    }
    
    // @FunctionalInterface：标记函数式接口
    @FunctionalInterface
    interface MyFunctionalInterface {
        void doSomething();
    }
}
```

几个内置注解值得多说一句：

- **`@Override`** 是 `SOURCE` 级注解，唯一作用是让编译器帮你检查「父类里真的有这个方法吗」。方法名拼错（比如把 `toString` 写成 `tostring`）时，没有 `@Override` 会静默变成一个新方法，有 `@Override` 则直接编译报错。**重写方法永远带上它**，这是 SelfBlog 后端代码遵循的惯例。
- **`@Deprecated`** 自 Java 9 起可以携带属性：`@Deprecated(since = "2.0", forRemoval = true)`，并在 Javadoc 里用 `@deprecated` 标签说明替代方案。IDE 会给调用处画删除线。
- **`@SuppressWarnings`** 常用参数值有 `"unchecked"`（泛型）、`"deprecation"`（使用了过时 API）、`"all"`。能用窄化类型解决（如给局部变量正确的泛型）就别压制警告，确实要压制时加注释说明原因。

### 2.3.3 自定义注解

```java
import java.lang.annotation.*;

// 定义注解
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAnnotation {
    String value() default "";
    int count() default 1;
    String[] tags() default {};
}

// 使用注解
@MyAnnotation(value = "测试", count = 3, tags = {"Java", "Spring"})
public class MyAnnotationDemo {
    
    @MyAnnotation("方法注解")
    public void doSomething() {
        System.out.println("执行方法");
    }
    
    public static void main(String[] args) throws Exception {
        // 获取类上的注解
        Class<?> clazz = MyAnnotationDemo.class;
        if (clazz.isAnnotationPresent(MyAnnotation.class)) {
            MyAnnotation annotation = clazz.getAnnotation(MyAnnotation.class);
            System.out.println("value: " + annotation.value());
            System.out.println("count: " + annotation.count());
            System.out.println("tags: " + String.join(", ", annotation.tags()));
        }
        
        // 获取方法上的注解
        Method method = clazz.getMethod("doSomething");
        if (method.isAnnotationPresent(MyAnnotation.class)) {
            MyAnnotation annotation = method.getAnnotation(MyAnnotation.class);
            System.out.println("方法注解 value: " + annotation.value());
        }
    }
}
```

自定义注解有几条语法要点，示例代码里已经全部用到：

1. **属性以无参方法形式声明**，如 `String value() default "";`，反射框架按方法名读取属性值；
2. **属性类型只能是**：基本类型、`String`、`Class`、枚举、注解，以及它们的数组——不能是 `List`、`Map` 或任意自定义类型；
3. **`default` 默认值必须是编译期常量**（不能调用方法、不能 `new` 对象）；
4. **属性名为 `value` 且只赋这一个值时可以省略属性名**：`@MyAnnotation("方法注解")` 等价于 `@MyAnnotation(value = "方法注解")`。所以设计注解时把最核心的属性命名为 `value` 是通行惯例——Spring 的 `@Cacheable("userCache")`、JUnit 的 `@DisplayName("登录测试")` 都遵循它。

### 2.3.4 注解的运行时处理

理解了 2.2 节的反射，注解的运行期处理就水到渠成，固定三步：**定义 `RUNTIME` 注解 → 在目标上标注 → 用反射读出并执行逻辑**。下面我们亲手实现一个迷你测试框架，它就是 JUnit 的雏形：

```java
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleTest {
    String description() default "";
}
```

```java
public class StringUtilsTest {
    @SimpleTest(description = "反转字符串")
    public void testReverse() {
        assert "cba".equals(new StringBuilder("abc").reverse().toString());
    }

    @SimpleTest
    public void testEmpty() {
        assert "".isEmpty();
    }

    public void helper() { }   // 没有 @SimpleTest，运行器会跳过它
}
```

```java
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SimpleTestRunner {
    public static void run(Class<?> testClass) throws Exception {
        Object instance = testClass.getDeclaredConstructor().newInstance();
        List<String> failures = new ArrayList<>();
        int passed = 0;

        for (Method method : testClass.getDeclaredMethods()) {
            SimpleTest st = method.getAnnotation(SimpleTest.class);
            if (st == null) {
                continue;   // 只执行标注了 @SimpleTest 的方法
            }
            try {
                method.invoke(instance);   // 2.2.3 节的 Method.invoke
                passed++;
                System.out.println("通过: " + method.getName()
                        + (st.description().isEmpty() ? "" : "（" + st.description() + "）"));
            } catch (Exception e) {
                failures.add(method.getName() + " -> " + e.getCause());
            }
        }
        System.out.println("共 " + (passed + failures.size()) + " 个测试，通过 "
                + passed + " 个，失败 " + failures.size() + " 个");
        failures.forEach(f -> System.out.println("失败: " + f));
    }

    public static void main(String[] args) throws Exception {
        run(StringUtilsTest.class);
    }
}
```

对照这段代码回看 JUnit：`@Test` 注解本身不做任何事，真正执行测试的是测试运行器——它用反射扫描类的方法、找到带 `@Test` 的方法逐个调用并统计结果，外加 `@BeforeEach` 这类生命周期注解控制执行顺序。2.11 节学 JUnit 5 时，你可以随时回想这个 30 行的迷你实现。

除了运行期处理，还有一条编译期路线：**注解处理器（APT）**在编译阶段扫描注解并生成代码。Lombok 就是典型——它的 `@Getter`/`@Data` 是 `SOURCE` 级注解，编译时直接修改语法树生成 getter/setter，运行期根本不存在这些注解。而 Spring 扫描 `@Component` 用的是另一条路：以字节码读取技术（ASM）轻量解析 class 文件元数据，避免过早加载类。同一套注解，三种处理时机（源码、字节码、运行期），框架各有取舍。

### 2.3.5 以 SelfBlog 项目为例

SelfBlog 项目中使用了大量注解：

```java
// 自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAdmin {
}

// 使用注解
@RestController
@RequestMapping("/api/admin")
public class AdminPostController {
    
    @RequiresAdmin
    @GetMapping("/posts")
    public Result<List<PostVO>> listPosts() {
        // ...
    }
}

// 注解处理器（AOP）
@Aspect
@Component
public class AuthAspect {
    @Before("@annotation(requiresAdmin)")
    public void checkAdmin(JoinPoint joinPoint, RequiresAdmin requiresAdmin) {
        // 检查是否为管理员
    }
}
```

对这段示例做三点真实的补充说明：

1. **`/api` 前缀不用手写**：SelfBlog 通过 `WebMvcConfig.configurePathMatch` 给所有 `@RestController` 统一自动加上 `/api` 前缀，控制器源码里只写 `/admin`、`/posts` 这样的相对路径。示例代码是教学化的写法。
2. **真实的权限校验交给 Spring Security**：实际代码里管理员接口的防护是 `SecurityConfig` 中 `/api/admin/**` 要求 `hasRole("admin")` 加方法级 `@PreAuthorize`，而不是上面这个自定义 `@RequiresAdmin` 切面。但两者机制同源：`@PreAuthorize` 同样是「注解 + 切面拦截」——Spring Security 用 AOP 在方法调用前读取注解表达式并求值。理解了本节内容，你就理解了 `@PreAuthorize` 的工作原理。
3. **自定义注解 + AOP 在 SelfBlog 的真实落地**是 `PerformanceMonitoringAspect`（2.13.1 节）：它用 `@Around("@annotation(...)")` 拦截标注了性能日志注解的方法记录耗时——正是 `AuthAspect` 示例的同款写法。

最后用一张表俯瞰 SelfBlog 里的注解生态，每一行都是「注解标记 + 框架处理」的契约：

| 注解 | 谁来处理 | 效果 |
|------|----------|------|
| `@RestController` / `@Service` / `@Component` | Spring IoC 容器 | 类被注册为 Bean |
| `@Autowired` / 构造器注入 | Spring 依赖注入 | 按类型装配依赖 |
| `@TableLogic` | MyBatis Plus | 字段 `isDeleted` 走软删除，查询自动拼 `is_deleted = 0` |
| `@Cacheable("hotTagsCache")` | Spring Cache + Caffeine | 方法结果按 key 缓存（SelfBlog 的 `CaffeineCacheConfig` 注册了 8 个具名缓存） |
| `@Async("applicationTaskExecutor")` | Spring + 线程池 | 方法丢进线程池异步执行（2.1.12 节） |
| `@Transactional(rollbackFor = Exception.class)` | Spring 事务（AOP 实现） | 方法包在事务里，异常回滚 |
| `@Aspect` + `@Around` / `@Before` | Spring AOP | 定义切面拦截逻辑 |
| `@Test` / `@BeforeEach` | JUnit 5 运行器 | 标记测试方法与生命周期钩子（2.11 节） |

> 一个来自真实项目的坑：`@TableLogic` 这类注解只对 **MyBatis Plus 生成的 SQL** 生效。SelfBlog 的规范里明确写着「手写 SQL 必须自己带上 `is_deleted=0`」——注解负责标记，语义终究要有人实现，而实现方只认识它认识的执行路径。

## 2.4 Lambda 表达式与函数式接口

### 2.4.1 Lambda 表达式

Lambda 表达式是 Java 8 引入的特性，用于简化匿名内部类的写法。

```java
public class LambdaDemo {
    public static void main(String[] args) {
        // 传统写法
        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                System.out.println("传统写法");
            }
        };
        
        // Lambda 写法
        Runnable runnable2 = () -> System.out.println("Lambda 写法");
        
        // 带参数的 Lambda
        Comparator<Integer> comparator = (a, b) -> a - b;
        
        // 多行 Lambda
        Runnable runnable3 = () -> {
            System.out.println("第一行");
            System.out.println("第二行");
        };
        
        // 方法引用
        Runnable runnable4 = System.out::println;
        
        // 使用 Lambda 遍历
        List<String> list = Arrays.asList("A", "B", "C");
        list.forEach(item -> System.out.println(item));
        list.forEach(System.out::println);
    }
}
```

Lambda 只能赋值给**函数式接口**（见下一小节）。参数类型通常省略，由编译器从目标类型推断。

**方法引用的四种形式**（`::` 是「进一步省略的 Lambda」）：

| 形式 | 示例 | 等价 Lambda |
|------|------|-------------|
| 对象::实例方法 | `System.out::println` | `s -> System.out.println(s)` |
| 类::静态方法 | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| 类::实例方法 | `String::toUpperCase` | `s -> s.toUpperCase()` |
| 类::new | `PostVO::new` | `() -> new PostVO()` |

一个实用的判断：当 Lambda 体只是「把参数原封不动传给某个方法」时，几乎总能改写成方法引用，可读性更好。注意 Lambda 捕获的局部变量必须是事实最终变量（effectively final）——因为 Lambda 可能在别的线程执行，变量若允许被修改会产生竞态；需要「可变状态」时用数组包装或 `AtomicInteger`。

### 2.4.2 函数式接口

函数式接口是只有一个抽象方法的接口。

```java
import java.util.function.*;

public class FunctionalInterfaceDemo {
    public static void main(String[] args) {
        // Consumer<T>：接收一个参数，无返回值
        Consumer<String> consumer = s -> System.out.println(s);
        consumer.accept("Hello");
        
        // Supplier<T>：无参数，返回一个值
        Supplier<String> supplier = () -> "Hello";
        System.out.println(supplier.get());
        
        // Function<T, R>：接收一个参数，返回一个值
        Function<String, Integer> function = s -> s.length();
        System.out.println(function.apply("Hello"));
        
        // Predicate<T>：接收一个参数，返回 boolean
        Predicate<String> predicate = s -> s.length() > 5;
        System.out.println(predicate.test("Hello World"));
        
        // BiFunction<T, U, R>：接收两个参数，返回一个值
        BiFunction<String, String, String> biFunction = (s1, s2) -> s1 + s2;
        System.out.println(biFunction.apply("Hello", " World"));
        
        // 自定义函数式接口
        @FunctionalInterface
        interface MyFunction {
            int apply(int a, int b);
        }
        
        MyFunction add = (a, b) -> a + b;
        MyFunction multiply = (a, b) -> a * b;
        System.out.println("加法：" + add.apply(3, 4));
        System.out.println("乘法：" + multiply.apply(3, 4));
    }
}
```

四大核心接口的记忆法，按「有没有参数、有没有返回值」划分：

| 接口 | 签名 | 口诀 | 典型用途 |
|------|------|------|----------|
| `Supplier<T>` | `() -> T` | 只出不进 | 惰性求值、工厂；Caffeine/ThreadLocal 的 `withInitial(() -> ...)` |
| `Consumer<T>` | `T -> ()` | 只进不出 | `forEach`、回调 |
| `Function<T,R>` | `T -> R` | 有进有出 | `map`、类型转换 |
| `Predicate<T>` | `T -> boolean` | 进来验货 | `filter`、条件判断 |

`@FunctionalInterface` 注解本身是可选的，但建议写上：它让编译器帮你检查「只有一个抽象方法」，防止后人无意间加方法破坏接口。回顾 2.1 节：`Runnable`、`Callable<T>`、`Comparator<T>` 都是函数式接口，这正是线程创建可以写成 Lambda 的原因。

## 2.5 Stream API

### 2.5.1 Stream 概述

Stream API 是 Java 8 引入的用于处理集合数据的函数式编程工具。

Stream 的三个核心特征：

1. **不存数据**：Stream 只是数据源的一条「流水线」，本身不存储元素；
2. **不改源头**：中间操作都返回新 Stream，原集合不受影响；
3. **惰性求值**：中间操作只是「记录步骤」，直到终止操作（`collect`、`forEach`……）出现才真正执行——所以上面 2.1.2 节的无限流也不会真的无限跑。

一次典型的使用是「声明式地描述要什么」：从文章列表里筛出已发布的、按浏览量倒序、取前 10 篇的标题。用 for 循环要写三层嵌套和临时变量，用 Stream 每一步都对应一个业务动词。

### 2.5.2 创建 Stream

```java
import java.util.*;
import java.util.stream.*;

public class StreamCreationDemo {
    public static void main(String[] args) {
        // 从集合创建
        List<String> list = Arrays.asList("A", "B", "C");
        Stream<String> stream1 = list.stream();
        
        // 从数组创建
        String[] array = {"A", "B", "C"};
        Stream<String> stream2 = Arrays.stream(array);
        
        // 使用 Stream.of()
        Stream<String> stream3 = Stream.of("A", "B", "C");
        
        // 创建无限流
        Stream<Integer> stream4 = Stream.iterate(0, n -> n + 2);
        Stream<Double> stream5 = Stream.generate(Math::random);
        
        // 从文件创建
        // Stream<String> stream6 = Files.lines(Paths.get("file.txt"));
    }
}
```

无限流必须配合 `limit()` 截断使用，如 `Stream.iterate(0, n -> n + 2).limit(10)` 生成前 10 个偶数。

### 2.5.3 中间操作

```java
import java.util.*;
import java.util.stream.*;

public class StreamIntermediateDemo {
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // filter：过滤
        List<Integer> evens = numbers.stream()
            .filter(n -> n % 2 == 0)
            .collect(Collectors.toList());
        System.out.println("偶数：" + evens);
        
        // map：映射
        List<Integer> squares = numbers.stream()
            .map(n -> n * n)
            .collect(Collectors.toList());
        System.out.println("平方：" + squares);
        
        // flatMap：扁平化映射
        List<List<Integer>> nested = Arrays.asList(
            Arrays.asList(1, 2, 3),
            Arrays.asList(4, 5, 6),
            Arrays.asList(7, 8, 9)
        );
        List<Integer> flat = nested.stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
        System.out.println("扁平化：" + flat);
        
        // distinct：去重
        List<Integer> distinct = Arrays.asList(1, 2, 2, 3, 3, 3).stream()
            .distinct()
            .collect(Collectors.toList());
        System.out.println("去重：" + distinct);
        
        // sorted：排序
        List<Integer> sorted = numbers.stream()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
        System.out.println("降序：" + sorted);
        
        // limit：限制数量
        List<Integer> limited = numbers.stream()
            .limit(5)
            .collect(Collectors.toList());
        System.out.println("前5个：" + limited);
        
        // skip：跳过
        List<Integer> skipped = numbers.stream()
            .skip(5)
            .collect(Collectors.toList());
        System.out.println("跳过前5个：" + skipped);
        
        // peek：查看（调试用）
        numbers.stream()
            .peek(n -> System.out.println("处理：" + n))
            .filter(n -> n > 5)
            .collect(Collectors.toList());
    }
}
```

`flatMap` 是最容易困惑的一个。记忆口诀：**map 是「一对一」，flatMap 是「一对多再拍平」**。典型场景：每篇文章有若干标签，`posts.stream().flatMap(p -> p.getTags().stream())` 直接得到所有标签的平铺流；若是 `map`，得到的会是「标签列表的流」，还要再拆一层。

### 2.5.4 终止操作

```java
import java.util.*;
import java.util.stream.*;

public class StreamTerminalDemo {
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // forEach：遍历
        numbers.stream().forEach(System.out::println);
        
        // count：计数
        long count = numbers.stream().count();
        System.out.println("数量：" + count);
        
        // reduce：归约
        int sum = numbers.stream().reduce(0, Integer::sum);
        System.out.println("总和：" + sum);
        
        // max/min：最大/最小值
        Optional<Integer> max = numbers.stream().max(Integer::compareTo);
        Optional<Integer> min = numbers.stream().min(Integer::compareTo);
        System.out.println("最大值：" + max.get());
        System.out.println("最小值：" + min.get());
        
        // anyMatch/allMatch/noneMatch：匹配
        boolean anyEven = numbers.stream().anyMatch(n -> n % 2 == 0);
        boolean allPositive = numbers.stream().allMatch(n -> n > 0);
        boolean noneNegative = numbers.stream().noneMatch(n -> n < 0);
        System.out.println("有偶数：" + anyEven);
        System.out.println("全为正数：" + allPositive);
        System.out.println("没有负数：" + noneNegative);
        
        // findFirst/findAny：查找
        Optional<Integer> first = numbers.stream().findFirst();
        Optional<Integer> any = numbers.stream().findAny();
        System.out.println("第一个：" + first.get());
        System.out.println("任意一个：" + any.get());
        
        // collect：收集
        List<Integer> list = numbers.stream().collect(Collectors.toList());
        Set<Integer> set = numbers.stream().collect(Collectors.toSet());
        Map<Integer, Integer> map = numbers.stream()
            .collect(Collectors.toMap(n -> n, n -> n * n));
        
        // 分组
        Map<Boolean, List<Integer>> partitioned = numbers.stream()
            .collect(Collectors.partitioningBy(n -> n % 2 == 0));
        System.out.println("偶数：" + partitioned.get(true));
        System.out.println("奇数：" + partitioned.get(false));
        
        // 分区
        Map<String, List<Integer>> grouped = numbers.stream()
            .collect(Collectors.groupingBy(n -> n > 5 ? "大于5" : "小于等于5"));
        System.out.println("分组：" + grouped);
        
        // 统计
        IntSummaryStatistics stats = numbers.stream()
            .collect(Collectors.summarizingInt(Integer::intValue));
        System.out.println("统计：" + stats);
        System.out.println("平均值：" + stats.getAverage());
        System.out.println("最大值：" + stats.getMax());
        System.out.println("最小值：" + stats.getMin());
        System.out.println("总和：" + stats.getSum());
        
        // 连接字符串
        String joined = numbers.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", "));
        System.out.println("连接：" + joined);
    }
}
```

三个初学者高频踩坑点：

1. **`Collectors.toMap` 遇到重复 key 直接抛 `IllegalStateException`**。安全写法是提供合并函数：`toMap(Post::getCategoryId, p -> p, (a, b) -> a)`。
2. **`Optional` 别裸调 `get()`**。优先 `orElse(默认值)`、`orElseGet(() -> 计算默认值)`、`orElseThrow(() -> new BusinessException(...))`——SelfBlog 的 `Result` 体系里，查不到文章就应抛业务异常而不是 NPE。
3. **`groupingBy` 的 key 不能为 null**，否则抛 NPE；分组键可能为空时先用 `Optional.ofNullable(...).orElse("未知")` 兜底。

**结合 SelfBlog 的真实场景**：首页「热门标签」需要统计每张标签被多少篇已发布文章使用，按次数倒序取前 N——这正是 Stream 的主场：

```java
// postQueryService 内部示意
Map<Long, Long> tagUsage = publishedPosts.stream()
        .flatMap(post -> post.getTagIds().stream())     // 拍平成标签 ID 流
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

List<Long> hotTagIds = tagUsage.entrySet().stream()
        .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
        .limit(limit)
        .map(Map.Entry::getKey)
        .toList();                                       // Java 16+ 可直接 toList()
```

### 2.5.5 并行流

```java
import java.util.*;
import java.util.stream.*;

public class ParallelStreamDemo {
    public static void main(String[] args) {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            numbers.add(i);
        }
        
        // 串行流
        long start1 = System.currentTimeMillis();
        long sum1 = numbers.stream().mapToLong(Integer::longValue).sum();
        long end1 = System.currentTimeMillis();
        System.out.println("串行流耗时：" + (end1 - start1) + "ms");
        
        // 并行流
        long start2 = System.currentTimeMillis();
        long sum2 = numbers.parallelStream().mapToLong(Integer::longValue).sum();
        long end2 = System.currentTimeMillis();
        System.out.println("并行流耗时：" + (end2 - start2) + "ms");
    }
}
```

`parallelStream()` 底层用 ForkJoinPool 把数据切成多段并行处理，但**它默认共享全 JVM 唯一的 `ForkJoinPool.commonPool()`**，与 2.1.12 节提到的 CompletableFuture 默认池是同一个。两条实践纪律：

1. 并行流只适合「数据量大 + 单元素计算重 + 无 IO」的纯 CPU 任务；小数据量并行化的线程协调开销反而更慢，含 IO 的任务会占死公共池；
2. Web 服务器里不要让并行流跑在请求线程上做重活——需要并行就用显式线程池 + CompletableFuture，池的规模和行为才可控。

## 2.6 JVM 基础

写 Spring Boot 应用前不需要成为 JVM 调优专家，但必须建立一张「JVM 里有什么」的地图：内存怎么分区、对象死没死由谁判断、类从哪来、参数怎么调。SelfBlog 的启动脚本、Docker 镜像和排查手册里都会反复用到这些概念。

### 2.6.1 运行时内存区域

**线程私有（随线程生灭）：**

| 区域 | 存什么 | 可能的异常 |
|------|--------|-----------|
| **程序计数器** | 当前线程执行到的字节码行号 | 唯一不会 OOM 的区域 |
| **虚拟机栈** | 方法调用的栈帧（局部变量表、操作数栈、方法出口） | 递归太深 → `StackOverflowError`；扩展失败 → OOM |
| **本地方法栈** | native 方法（如 `Object.hashCode` 的 JNI 实现）的调用栈 | 同上 |

**线程共享（随 JVM 生灭）：**

| 区域 | 存什么 | 可能的异常 |
|------|--------|-----------|
| **堆（Heap）** | 所有对象实例与数组；GC 的主战场；分新生代（Eden + 两个 Survivor）与老年代 | `java.lang.OutOfMemoryError: Java heap space` |
| **方法区（JDK 8+ 实现：元空间 Metaspace）** | 类元信息、运行时常量池、静态变量（静态变量实际随 Class 对象存堆中） | `OutOfMemoryError: Metaspace`——典型诱因是动态生成大量类（CGLIB 代理、Groovy 脚本） |

一张脑内地图：

```
JVM 内存
├─ 线程私有
│   ├─ 程序计数器
│   ├─ 虚拟机栈        ← StackOverflowError（无限递归）
│   └─ 本地方法栈
└─ 线程共享
    ├─ 堆              ← OOM: heap space（对象太多/泄漏）
    │   ├─ 新生代（Eden + Survivor0 + Survivor1）
    │   └─ 老年代
    └─ 元空间（本地内存） ← OOM: Metaspace（类太多）
```

两个与日常直接相关的结论：

1. **`-Xss` 与 `-Xmx` 的数量关系**决定了「能开多少线程」：每个线程都要申请一块栈内存，1MB 栈 × 1000 线程 = 1GB。虚拟线程（2.7.1 节）之所以能开百万级，正是因为它们不独占 OS 线程、栈存放在堆上按需伸缩。
2. **直接内存（堆外内存）**：NIO 的 `DirectByteBuffer`、一些网络框架会使用堆外内存，受 `-XX:MaxDirectMemorySize` 限制。它的 OOM 报错不带 heap 字样，排查时别找错地方。

**对象的一生（分代模型）**：对象先在 Eden 区出生 → Eden 满了触发 Minor GC，存活对象进 Survivor 区并在两块 Survivor 之间来回复制，每熬过一次 GC 年龄 +1 → 年龄达到阈值（默认 15）或大对象直接进老年代 → 老年代满了触发 Major GC / Full GC。这个设计基于**分代假设**：绝大多数对象朝生夕死（如请求里的临时 VO、Stream 中间对象），把「易死」和「长寿」分开管理，能让每次 GC 只处理一小块内存。

### 2.6.2 垃圾回收基础

**怎么判断对象该死？** 主流 JVM 用**可达性分析**：从 GC Roots（栈中局部变量、静态字段、常量、JNI 引用等）出发沿引用链走，走不到的对象就是垃圾。它比「引用计数」可靠——后者处理不了循环引用。这也解释了 2.1.9 节 ThreadLocal 的弱引用设计：Entry 的 key 是弱引用，GC 时不会把 ThreadLocal 对象算进可达集合，key 就会变 null，JVM 借此清理陈旧条目。

四种引用强度，从强到弱：**强引用**（普通赋值，绝不回收）→ **软引用**（内存不足才回收，适合缓存）→ **弱引用**（下次 GC 必回收，如 ThreadLocalMap 的 key、`WeakHashMap`）→ **虚引用**（仅用于跟踪回收动作）。

**常见垃圾收集器概览**（记住定位即可，参数调优是专门领域）：

| 收集器 | 代际定位 | 特点 | 适用 |
|--------|----------|------|------|
| Serial / ParNew | 新生代 | 单线程 / 多线程复制算法；Stop-The-World | 小内存、客户端 |
| Parallel Scavenge | 新生代 | 吞吐量优先 | 批处理（JDK 8 默认） |
| **CMS** | 老年代 | 并发标记清除，停顿短但产生内存碎片、并发失败退化 | JDK 9 已废弃、14 移除；老系统还会见到 |
| **G1** | 整堆 | 把堆切成等大 Region，可预测停顿、整体标记整理局部复制；`-XX:MaxGCPauseMillis` 设目标停顿 | 大堆、低延迟通用首选（**JDK 9+ 默认，SelfBlog 默认就是它**） |
| **ZGC** | 整堆 | 着色指针 + 读屏障，停顿亚毫秒级，支持 TB 级堆 | 超大堆、极致低延迟（JDK 21 转正的生产可用状态） |

理解 GC 的最小知识集：**STW（Stop-The-World）**——标记/整理的关键阶段需要暂停所有业务线程，GC 调优的绝大部分工作就是把 STW 停顿压短、频率压低。SelfBlog 这类博客应用堆小、对象生命周期简单，G1 默认参数即可，无需特意调优。

**GC 日志速查**（排查问题先开日志再谈优化）：

```
-Xlog:gc*:file=gc.log:time,uptime,level,tags     # JDK 9+ 统一日志（Java 21 用法）
-XX:+HeapDumpOnOutOfMemoryError                  # OOM 时自动转储堆快照
-XX:HeapDumpPath=/data/dumps/                    # 快照保存位置
```

拿到 gc.log 与堆转储后，用 `jvisualvm`、Eclipse MAT 或 GCEasy 分析；SelfBlog 的 Docker 部署建议加上 `HeapDumpOnOutOfMemoryError`，OOM 时留下现场。

### 2.6.3 类加载机制与双亲委派

类的生命周期：**加载 → 验证 → 准备 → 解析 → 初始化 → 使用 → 卸载**。前三步合称「加载阶段」。几个关键细节：

- **准备阶段**给静态变量分配内存并赋**零值**（`static int a = 1` 此刻 a 是 0），真正的 1 要等**初始化阶段**执行 `<clinit>()` 才赋上；
- **初始化是懒触发**的：首次 `new`、首次访问静态成员、反射调用、启动类（main 所在类）等才触发；`ClassName.staticFinal` 这种编译期常量甚至不触发初始化；
- JAR 包里如果没有 `main`，Spring Boot 的 fat jar 是靠 `java -jar` 约定的 `JarLauncher` 把 `BOOT-INF/classes` 与 `BOOT-INF/lib` 挂进类路径再启动主类——这也是 SelfBlog 镜像启动命令的背后机制。

**三种类加载器：**

| 加载器 | 加载范围 |
|--------|----------|
| Bootstrap ClassLoader（C++ 实现） | `JAVA_HOME/lib` 核心库（`java.lang.*` 等） |
| Extension / Platform ClassLoader（JDK 9+ 改名平台类加载器） | 扩展目录 / 平台库 |
| Application ClassLoader | classpath（应用自己的类、第三方依赖） |

**双亲委派模型**：类加载请求先逐级上抛——Application → Platform → Bootstrap；父加载器能加载就加载，不能才回落到自己。层次结构如下：

```
Bootstrap（java.lang.String 这些核心类）
    ↑ 委派
Platform（JDK 9+ 平台类加载器）
    ↑ 委派
Application（你的 com.selfblog.* 类）
    ↑ 委派
自定义 ClassLoader（如 Tomcat 每个应用一个、Spring Boot 的 JarLauncher）
```

**为什么这样设计？** 两个目的：其一，**安全**——任何人自己写一个 `java.lang.String` 也加载不进来（Bootstrap 层已经加载过并缓存），核心类无法被篡改替换；其二，**唯一性**——同一个类只会被加载一次，`Class` 对象全 JVM 唯一，`instanceof`、类型转换才不会出现「同名类两个版本」的混乱。

类加载器的「缓存」就是 `<类, 加载器>` 的对应关系常驻内存，这也是**热部署/热更新框架的原理**：丢弃旧 ClassLoader、新建一个重新加载 class 文件——旧类与新类即使字节码相同，Class 对象也不同，所以热部署后旧对象与新类 `instanceof` 会失败，这是热部署不彻底的根源。

Tomcat 为了多应用隔离故意**打破**双亲委派（WebAppClassLoader 先自己加载应用内类）；OSGi、模块系统（JPMS）则是更完整的模块化方案。日常业务了解结论即可：**默认遵守双亲委派，除非你在写容器**。

### 2.6.4 常用 JVM 参数

按用途分类记忆，比硬背清单有效：

```bash
# —— 内存 ——
-Xms512m                    # 初始堆大小（建议与 Xmx 相同，避免运行期扩容抖动）
-Xmx1g                      # 最大堆大小
-Xss512k                    # 每个线程的栈大小（默认 1M；影响可创建线程数）
-XX:MaxMetaspaceSize=256m   # 元空间上限（防动态生成类打爆内存）
-XX:MaxDirectMemorySize=64m # 堆外直接内存上限

# —— GC ——
-XX:+UseG1GC                        # 指定 G1（JDK 9+ 默认，可不写）
-XX:MaxGCPauseMillis=100            # G1 目标停顿毫秒数
-XX:+UseZGC                         # 换用 ZGC（JDK 21 生产可用）
-Xlog:gc*:file=gc.log:time,uptime   # GC 统一日志

# —— 诊断 ——
-XX:+HeapDumpOnOutOfMemoryError             # OOM 自动转储
-XX:HeapDumpPath=/data/dumps/               # 转储路径
-XX:+ExitOnOutOfMemoryError                 # OOM 直接退出（让容器重启接管）
-Xlog:class+load:file=classload.log         # 类加载日志（排查 NoClassDefFoundError）

# —— 其他常用 ——
-Dspring.profiles.active=prod   # 系统属性（Spring Boot 最常用）
-XX:+UseStringDeduplication     # G1 下重复字符串去重
```

**SelfBlog 的实际用法**：它的 `Dockerfile` 以容器方式启动 jar，容器内存受 cgroup 限制，堆参数要么显式写 `-Xmx`，要么交给 JDK 的容器感知（JDK 10+ 默认按容器配额自动设置，通常自动值更稳）；同时建议 `-XX:+ExitOnOutOfMemoryError` 配合容器的健康检查与重启策略，让 OOM 后快速恢复而不是僵死。

**诊断工具箱**（都随 JDK 21 自带，无需安装）：

| 命令 | 作用 |
|------|------|
| `jps -l` | 列出本机 Java 进程及主类（先拿 PID） |
| `jinfo -flags <pid>` | 查看运行中 JVM 的生效参数 |
| `jstat -gc <pid> 1000` | 每秒打印一次各区容量与 GC 次数（看 GC 是否频繁） |
| `jmap -histo:live <pid>` | 堆内对象直方图（看哪类对象最多） |
| `jmap -dump:live,format=b,file=heap.hprof <pid>` | 手动转储堆快照 |
| `jstack <pid>` | 线程转储（排查死锁、慢请求时线程卡在哪） |
| `jcmd <pid> VM.flags` | 综合诊断入口（JDK 推荐替代上面多数命令） |

一次典型排查流程：收到 OOM 告警 → `HeapDumpOnOutOfMemoryError` 已经留下 hprof → MAT 打开看 Dominator Tree → 定位到某个静态 Map 无限增长 → 回到代码确认是缓存无上限 → 换 Caffeine 并设置 maximumSize 与过期时间。SelfBlog 的 `CaffeineCacheConfig` 给 8 个具名缓存都设置了 TTL，正是这条经验的落地。


## 2.7 Java 21 新特性

SelfBlog 使用的正是 Java 21（LTS 版本）。Java 21 聚集了一批改变日常写法的新特性，本节挑三个对后端开发最有价值的展开：虚拟线程、Record 与 Record Pattern、Switch 模式匹配。

### 2.7.1 虚拟线程

回忆 2.1 节的内容：我们一直说的「线程」，准确名称是**平台线程（Platform Thread）**——它由操作系统调度，每个线程占用约 1MB 栈内存，创建和上下文切换的成本都不低。Tomcat 默认 200 个工作线程，就是出于「线程很贵，必须复用」的权衡。

但后端服务的真实形态是：**绝大多数请求的时间都花在等待 I/O 上**——等数据库返回、等远程接口响应、等磁盘读写。等待期间，昂贵的平台线程被白白占着，什么都没干。这就是虚拟线程要解决的问题。

**虚拟线程的本质**：把「线程」这个概念从操作系统手里接管过来，由 JVM 自己调度。虚拟线程是轻量级实体，成千上万个虚拟线程共享少量平台线程（称为**载体线程，Carrier Thread**）来执行：

- 虚拟线程执行到阻塞点（如等待数据库）时，JVM 把它的栈从载体线程上**卸载（unmount）**下来存进堆里，载体线程立刻去跑别的虚拟线程；
- I/O 就绪后，JVM 再把栈**装载（mount）**回某个可用的载体线程继续执行。

一次「线程阻塞」从「占着一个 1MB 栈的操作系统线程干等」变成「堆里的一段可恢复数据 + 载体线程无缝切换」，成本从 KB 级内存加系统调用，降到几乎为零。

**用法一：直接创建**

```java
// 方式一：Thread.ofVirtual() 工厂
Thread vt = Thread.ofVirtual().name("reader-", 0).start(() -> {
    System.out.println("虚拟线程运行中: " + Thread.currentThread());
});

// 方式二：Thread.startVirtualThread 快捷方法
Thread.startVirtualThread(() -> System.out.println("hello virtual"));

// 方式三：执行器（每个任务一个虚拟线程，无需池化）
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
        int taskId = i;
        executor.submit(() -> {
            Thread.sleep(1000);          // 模拟 I/O 等待
            return "task-" + taskId;
        });
    }
}   // try-with-resources 关闭时会等待所有任务完成
```

运行第一段代码，输出类似 `VirtualThread[#22,reader-0]/runnable@ForkJoinPool-1-worker-1`：`#22` 是虚拟线程编号，`@` 后面的 `ForkJoinPool-1-worker-1` 才是承载它的载体线程。

**用法二：与 Spring Boot 3.2+ 集成（重点）**

SelfBlog 基于 Spring Boot 3.3.13，只需一行配置即可启用：

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true   # Spring Boot 3.2+ 有效
```

这行配置的效果是：**Tomcat 的请求处理线程全部换成虚拟线程**——每来一个请求，创建一个新的虚拟线程去处理，处理完即销毁。带来的架构转变值得仔细体会：

- **「一个请求一个线程」重新变得可行**。以前 200 个平台线程意味着最多并发处理 200 个请求，超出部分排队；换成虚拟线程后，1 万个并发请求就是 1 万个虚拟线程，内存开销以 KB 计。
- **不需要为吞吐量调大线程池**。I/O 密集型场景下，虚拟线程天然把「并发请求数」和「载体线程数」解耦了。
- **阻塞式代码重新流行**。以前为了压榨线程，我们把同步代码改造成 `CompletableFuture` 异步链（2.1.11 节）；有了虚拟线程，「用同步的写法获得异步的吞吐」——代码更好读，调试栈更直观。

三条必须记住的使用纪律：

1. **永远不要池化虚拟线程**。它们廉价到用完即弃，`Executors.newVirtualThreadPerTaskExecutor()` 的名字就是在告诉你「每个任务一个」；池化它们既无收益又引入排队瓶颈。
2. **CPU 密集型任务不受益**。虚拟线程解决的是「等待」，不是「计算」。8 核机器上同时只有 8 个线程真正在算，虚拟线程再多也没用——CPU 密集场景请继续用固定大小的平台线程池（2.1.6 节）。
3. **警惕 pinned（钉住）问题**。在 JDK 21 中，虚拟线程在 `synchronized` 块内阻塞时**不会卸载**，而是钉在载体线程上——大量这种情况会耗尽载体线程，虚拟线程退化。热点阻塞路径上改用 `ReentrantLock`（2.1.7 节）可以规避（JDK 24 已修复此问题，但 Java 21 用户仍需注意）。同理，虚拟线程里慎用 `ThreadLocal` 缓存大对象——百万个虚拟线程意味着百万份拷贝。

对 SelfBlog 的实际评估：它是 I/O 密集型应用（读库、写缓存、渲染 HTML），虚拟线程契合度高；但要注意**数据库连接池仍是硬瓶颈**——1 万个虚拟线程同时涌向 HikariCP（默认池大小只有十几个连接），最终还是要排队拿连接。虚拟线程把「线程不够」变成了「连接不够」，瓶颈转移了但没消失。这也是 SelfBlog 对 SQLite 数据源显式配置池大小为 1 的背景下，虚拟线程带来的收益有限、风险（连接争用放大）反而更值得警惕的原因——**技术选型要看清自己的瓶颈在哪一环**。

### 2.7.2 Record 与 Record Pattern

`record` 是 Java 16 转正的数据载体类。一行声明，编译器自动生成构造器、访问器、`equals`/`hashCode`/`toString`：

```java
// 传统的 DTO：构造器、getter、equals、hashCode、toString 五件套，几十行样板代码
// record：一行搞定
public record PostSummary(Long id, String title, Long views) { }

// 使用
PostSummary post = new PostSummary(1L, "Java 并发入门", 1024L);
post.title();        // 访问器不带 get 前缀，直接用字段名
post.views();
```

要点与边界：

- record 的字段全是 `final`，**不可变**；不能继承其他类（隐式继承 `java.lang.Record`），可以实现接口；
- 可以声明**紧凑构造器**做参数校验，不用重复写参数赋值：

```java
public record PostSummary(Long id, String title, Long views) {
    public PostSummary {                     // 紧凑构造器：没有参数列表
        if (views == null || views < 0) {
            throw new IllegalArgumentException("views 不能为负");
        }
        title = title.strip();               // 可以直接给参数重新赋值
    }
}
```

- SelfBlog 场景示例——缓存与统计：

```java
// Caffeine 缓存里的不可变快照，天然线程安全
public record VisitorSummary(long pv, long uv, LocalDateTime refreshedAt) { }

// Stream 聚合结果
Map<String, Long> tagCount = posts.stream()
        .collect(Collectors.groupingBy(PostSummary::title, Collectors.counting()));
```

**Record Pattern（Java 21 转正，JEP 440）**是 record 的配套解构语法：`instanceof` 判定成功后**直接把字段拆成变量**，省掉手动强转和逐个调用访问器：

```java
// 传统写法
if (obj instanceof PostSummary) {
    PostSummary p = (PostSummary) obj;
    System.out.println(p.title() + " / " + p.views());
}

// Record Pattern：类型判断 + 解构一步到位
if (obj instanceof PostSummary(String title, Long views)) {
    System.out.println(title + " / " + views);   // title、views 已是解构出的变量
}

// 支持嵌套解构
record Author(String name, Integer level) { }
record PostWithAuthor(String title, Author author) { }

if (obj instanceof PostWithAuthor(String title, Author(String name, Integer level))) {
    System.out.println(name + " 写了 " + title + "（Lv." + level + "）");
}
```

顺带记住 **JEP 441 的 `instanceof` 模式匹配**（Java 16 转正，Java 21 的基础组件）：

```java
// 判定 + 强转合并为一行，且 obj 后面必须满足恒真条件才编译通过
if (obj instanceof String s) {
    System.out.println(s.length());   // 这里 s 已是 String，无需 (String) obj
}
```

### 2.7.3 Switch 模式匹配

Java 21 转正了 **Switch 的模式匹配（JEP 441）**，Switch 从「只能按整数/枚举/字符串分派」进化为「按类型与条件分派」，并且编译器会检查**穷举性**——所有可能情况都覆盖了才编译通过。

```java
// 1. 按类型分派 + Record Pattern 解构
static String describe(Object obj) {
    return switch (obj) {
        case PostSummary(Long id, String title, long views) when views > 10000
                -> "爆款文章：" + title;
        case PostSummary(Long id, String title, long views)
                -> "文章：" + title + "（" + views + " 浏览）";
        case String s                       -> "字符串：" + s;
        case Integer i when i < 0           -> "负整数";
        case null                           -> "空值";        // 直接处理 null，无需前置判空
        default                             -> "未知类型";
    };
}
```

三个关键语法点：

- **`when` 守卫**：在 case 后追加布尔条件做二次筛选，命中则执行，否则继续向下匹配；
- **`case null`**：传统 Switch 遇到 `null` 直接 NPE，模式匹配可以显式声明 `case null` 分支；
- **穷举检查**：配合 `sealed` 密封接口使用威力最大——

```java
// sealed：封闭继承树，编译器知道全部子类型
public sealed interface EventHandler permits LikeEvent, CommentEvent, ViewEvent { }
public record LikeEvent(Long postId, Long userId) implements EventHandler { }
public record CommentEvent(Long postId, String content) implements EventHandler { }
public record ViewEvent(Long postId) implements EventHandler { }

// 不写 default 也能编译通过：编译器证明三个子类型全部覆盖
// 以后新增一种事件类型，所有 switch 不改就编译报错——把「漏处理」从运行期事故变成编译期错误
static void handle(EventHandler event) {
    switch (event) {
        case LikeEvent(Long postId, Long userId)   -> likeService.record(postId, userId);
        case CommentEvent(Long postId, String c)   -> commentService.save(postId, c);
        case ViewEvent(Long postId)                -> viewService.increase(postId);
    }
}
```

SelfBlog 场景：访客行为处理（浏览/点赞/留言）正是「一种事件一种处理」的形状，`sealed` + Switch 模式匹配能保证未来新增行为类型时所有分派点都被编译器强制更新——这是把设计模式中「多态分派」用语言特性直接表达出来的例子。

## 2.8 JDBC 与数据库编程基础

第三章将学习 MyBatis Plus，而 MyBatis Plus 的底层就是 JDBC。本节补上这一层地基：JDBC 的基本流程、`PreparedStatement` 防 SQL 注入的原理、连接池为什么存在（HikariCP 为什么快）、以及事务的隔离级别与 Spring 传播行为——它们是理解 `@Transactional` 的前置知识。

### 2.8.1 JDBC 基本流程

JDBC（Java Database Connectivity）是 JDK 内置的数据库访问标准接口。各数据库厂商提供实现（驱动 jar），应用面向接口编程：

```java
import java.sql.*;

public class JdbcDemo {
    public static void main(String[] args) throws Exception {
        // 1. 加载驱动（JDBC 4.0+ 由 SPI 自动完成，老代码里的 Class.forName 可以省略）
        // Class.forName("com.mysql.cj.jdbc.Driver");

        String url = "jdbc:mysql://localhost:3306/selfblog";
        // 2. 建立连接   3. 创建语句   4. 执行   5. 处理结果   6. 释放资源
        try (Connection conn = DriverManager.getConnection(url, "root", "password");
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, title, views FROM blog_post WHERE status = ?"));
        {
            ps.setInt(1, 1);                                   // 绑定参数，下标从 1 开始
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String title = rs.getString("title");
                    long views = rs.getLong("views");
                    System.out.println(id + " " + title + " " + views);
                }
            }
        }
    }
}
```

六个步骤里，最有现代感的是 **try-with-resources**：`Connection`、`Statement`、`ResultSet` 都实现了 `AutoCloseable`，写在 try 的括号里就能保证无论正常返回还是异常都按声明逆序自动关闭——资源泄漏的经典源头被语法消灭了。

对 SelfBlog 而言，你几乎不会手写这段代码：MyBatis Plus 把 3~5 步封装成 `BaseMapper` 的泛型方法，把「结果集 → 实体」的映射自动化（`BlogPost` 实体 ↔ `blog_post` 表）。但封装下的每一行 SQL 仍走这条路径，理解它才能读懂慢 SQL 日志和连接池监控指标。

### 2.8.2 PreparedStatement 与 SQL 注入

先看漏洞是怎么产生的。字符串拼接 SQL：

```java
// 危险代码：用户输入直接拼进 SQL
String username = request.getParam("username");
String password = request.getParam("password");
String sql = "SELECT * FROM sys_user WHERE username = '" + username
           + "' AND password = '" + password + "'";
```

攻击者在登录框输入用户名 `admin`、密码 `' OR '1'='1`，拼出来的 SQL 是：

```sql
SELECT * FROM sys_user WHERE username = 'admin' AND password = '' OR '1'='1'
-- '1'='1' 恒真， WHERE 条件整体为真 → 任意密码登录
```

更狠的输入甚至能拼接 `'; DROP TABLE sys_user; --` 删库。**注入的本质：用户输入被当成了 SQL 语法的一部分**。

`PreparedStatement` 的防注入原理是**语义分离**：

```java
String sql = "SELECT * FROM sys_user WHERE username = ? AND password = ?";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, username);   // 参数在 SQL 语义已定型之后才传入
    ps.setString(2, password);
    try (ResultSet rs = ps.executeQuery()) { ... }
}
```

执行时分两步：先把**带占位符的 SQL 模板**发给数据库预编译，语法树定型——`?` 的位置在语法上就是一个「值」，永远不可能变成关键字或新条件；参数值随后单独传输，驱动负责转义，数据库只把它当**纯数据**。就算用户输入 `' OR '1'='1`，也只会匹配到「用户名恰好是这串字符」的记录，注入无从谈起。副产品是**预编译语句可复用**：同一模板反复执行时数据库无需重新解析，批量写入时性能明显更好（配合 `addBatch`/`executeBatch`）。

在 SelfBlog 里的对应关系：MyBatis Plus 的所有条件构造器（`LambdaQueryWrapper.eq(...)`）生成的都是参数化 SQL；XML Mapper 里要用 `#{}`（预编译占位符）而**不是 `${}`（字符串替换，存在注入风险）**——`${}` 只在「列名、表名等无法参数化的位置」且值由代码白名单控制时才允许使用。这是代码审查里的一票否决项。

### 2.8.3 连接池：HikariCP 为什么快

数据库连接的建立成本极高：TCP 三次握手、TLS 协商（如果开启）、数据库认证、会话初始化，一次几十毫秒——而一次查询本身可能只要 1 毫秒。让每个请求都新建连接，等于每单都重新开一家店。**连接池**的思路是：启动时创建一批连接常驻，请求来了借一个、用完还回去。

Spring Boot 2.0 起，HikariCP 是官方默认连接池，SelfBlog 的 `DatabaseConfig` 创建数据源时用的就是它。它快的工程原因（按影响排序）：

1. **`ConcurrentBag` 无锁数据结构**：连接的借还走 ThreadLocal 优先匹配 + 无锁 CAS，避免传统池的重量级锁竞争；
2. **`FastList` 替代 `ArrayList`**：去掉每次 `get` 的边界检查，并利用「连接关闭时按逆序归还」的规律从尾部遍历，检查次数减半；
3. **字节码级优化**：代理类用 Javassist 生成，比 JDK 反射代理的调用开销低；
4. **极简设计**：没有后台回收线程池、没有不必要的功能，代码量只有同类产品的一小部分，bug 面与心智负担都更小。

关于池大小有一个反直觉的结论：**不是越大越快**。连接数超过数据库的并发处理能力后，只会增加上下文切换和锁排队。HikariCP 官方给出的经验公式是 `connections = cores * 2 + 磁盘数`——4 核数据库大约 10 个连接就饱和了。SelfBlog 的 SQLite 数据源把 Hikari 池大小配置为 1（SQLite 单写者模型的必然选择），MySQL 环境则用默认值——池大小永远取决于存储引擎的真实并发能力，而不是「越大越保险」。

### 2.8.4 事务的隔离级别

事务保证一组 SQL 要么全部成功、要么全部回滚（ACID 中的 A）。并发执行的事务相互「看见」对方的中间状态，会产生三类读问题：

| 问题 | 现象 | 例子 |
|------|------|------|
| 脏读 | 读到别人**未提交**的数据 | 事务 A 把浏览量改成 9999 未提交，事务 B 读到 9999，A 回滚——B 拿着假数据做统计 |
| 不可重复读 | 同一事务内两次读**同一行**结果不同 | A 两次读文章 1 的标题，中间 B 改了并提交——A 的两次结果不一致（针对 UPDATE） |
| 幻读 | 同一事务内两次**同样范围查询**行数不同 | A 统计状态=1 的文章 10 篇，中间 B 新发布 1 篇——A 再统计变成 11 篇（针对 INSERT） |

SQL 标准定义四个隔离级别，逐级用锁/多版本（MVCC）消灭上述问题：

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 说明 |
|----------|------|------------|------|------|
| READ UNCOMMITTED | 可能 | 可能 | 可能 | 几乎不用 |
| READ COMMITTED | 不会 | 可能 | 可能 | Oracle/PostgreSQL 默认 |
| REPEATABLE READ | 不会 | 不会 | 可能* | **MySQL InnoDB 默认**（其 MVCC+间隙锁大幅缓解了幻读） |
| SERIALIZABLE | 不会 | 不会 | 不会 | 完全串行，性能最差 |

*标准如此，InnoDB 实现优于标准，面试常考。

JDBC 层面设置隔离级别：

```java
conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
conn.setAutoCommit(false);        // 关闭自动提交，开启手动事务
try {
    // ... 多条 SQL ...
    conn.commit();                // 全部成功才提交
} catch (Exception e) {
    conn.rollback();              // 任一失败整体回滚
    throw e;
}
```

在 Spring 里，这些样板被 `@Transactional` 收编（AOP 实现，见 2.12.3 节代理模式）。SelfBlog 的约定值得对照记忆：查询方法标 `@Transactional(readOnly = true)`，交互方法标 `@Transactional(rollbackFor = Exception.class)`——后者显式指定 `rollbackFor`，因为 Spring 默认只对 `RuntimeException` 和 `Error` 回滚，受检异常默认提交，不写就是隐患。**隔离级别由数据库决定，传播行为由 Spring 决定**，下面看后者。

### 2.8.5 Spring 事务传播行为速览

传播行为回答的问题是：**一个事务方法调用另一个事务方法时，事务怎么合并？**这是理解 Service 之间互相调用的关键。七个传播行为，前三个最常用：

| 传播行为 | 外层有事务时 | 外层无事务时 | 典型场景 |
|----------|--------------|--------------|----------|
| `REQUIRED`（默认） | 加入当前事务 | 新建事务 | 绝大多数业务方法 |
| `REQUIRES_NEW` | **挂起外层事务**，新建独立事务 | 新建事务 | 操作日志、审计——不管主业务成败，日志必须独立落库 |
| `NESTED` | 在当前事务内设保存点 | 新建事务 | 子操作失败可单独回滚到保存点，主事务仍可继续 |
| `SUPPORTS` | 加入当前事务 | 非事务执行 | 只读辅助方法 |
| `NOT_SUPPORTED` | 挂起当前事务，非事务执行 | 非事务执行 | 大批量操作不想占事务连接 |
| `MANDATORY` | 加入当前事务 | 抛异常 | 强制要求调用方带事务的内部方法 |
| `NEVER` | 抛异常 | 非事务执行 | 明确拒绝在事务内运行 |

结合一个 SelfBlog 式的例子理解 `REQUIRES_NEW` 的价值：发布文章的主流程要更新文章、刷新标签统计、写操作日志。刷新统计失败不应该让「日志也没写」，于是日志方法标注 `@Transactional(propagation = Propagation.REQUIRES_NEW)`——它的事务独立提交或回滚，主流程的成败影响不到它。

最后复习事务失效的经典场景（都源于 2.2.5 节的代理原理）：同类内部 `this` 调用不走代理、方法不是 `public`、异常被 `catch` 吃掉、类没被 Spring 管理、以及**没写 `rollbackFor = Exception.class` 时抛受检异常**。写 `@Transactional` 时对照这张清单过一遍，能避开百分之九十的事故。


## 2.8.5 并发编程实战：从 synchronized 到虚拟线程

SelfBlog 里唯一一处手写并发原语是 `AnonymousLikeServiceImpl.toggle` 的 `synchronized`——但理解它为什么「够用」、什么时候「不够用」，是后端工程师的分水岭。

### 2.8.5.1 为什么需要并发控制：一个真实的竞态

匿名点赞的 toggle 逻辑「查 → 判断 → 改」三步**不是原子的**：

```
时刻 T1: 请求A 查询 → 无点赞记录
时刻 T2: 请求B 查询 → 无点赞记录        ← A 还没插入
时刻 T3: 请求A 插入点赞记录
时刻 T4: 请求B 插入点赞记录            ← 重复点赞！
```

这就是**竞态条件（race condition）**：结果取决于线程交错顺序。读多写少的普通查询无所谓，但「查了再写」的检查-再行动（check-then-act）模式必须串行化。

### 2.8.5.2 synchronized：SelfBlog 的诚实选择

```java
public synchronized AnonymousLikeVO toggle(Long postId, String visitorId) {
    // 查 → 判断 → 插入/软删/复活 三步在同一把锁内完成
}
```

为什么这个方案在 SelfBlog 里是对的？

1. **单机部署**：一个 JVM 进程，synchronized 的进程内互斥完全覆盖所有请求
2. **连接池大小 1**：SQLite + Hikari 池 1 本来就把数据库访问串行化了，应用层再加锁是双保险
3. **临界区极短**：锁内只有几次内存操作 + 一条 insert/update，微秒级，不会成为吞吐瓶颈

**什么时候 synchronized 不够用**：多实例部署（锁只在各自 JVM 生效）→ 要换数据库唯一约束或分布式锁；临界区内有慢 IO（远程调用）→ 锁竞争会把吞吐拖垮。**SelfBlog 若迁 MySQL 多实例，正确做法是在 (post_id, visitor_hash) 上建唯一约束，靠数据库兜底幂等**——应用层锁永远只解决单机问题。

### 2.8.5.3 线程安全的三条正路（按成本递增）

| 手段 | 原理 | 适用 | 代价 |
|------|------|------|------|
| **无状态** | 不共享可变状态，天然线程安全 | Service 单例（Spring Bean 默认要求） | 零成本，首选 |
| **不可变** | 状态创建后不变（String、Record、final 字段） | 配置对象、值对象 | 需要设计克制 |
| **正确同步** | synchronized/Lock/原子类 | 真有共享可变状态 | 心智负担+性能，最后手段 |

Spring 要求单例 Bean 无状态（2.1.4 节讲过）正是在用第一条路：**成员变量只放注入的协作者（无状态），请求数据全部走方法参数与局部变量（线程栈私有）**。SelfBlog 全部 40+ 个 Service 遵守这条纪律，只有 anonymous-like 一处显式加锁。

### 2.8.5.4 线程池：@Async 背后的工人调度

2.11 节（ch03 §3.14.2）讲过 @Async 要配线程池。这里补线程池的**参数语义**（面试必考，实战必用）：

```java
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    4,                              // corePoolSize：常驻工人数
    8,                              // maximumPoolSize：忙不过来时最多扩到几人
    60, TimeUnit.SECONDS,           // 临时工空闲 60s 退出
    new ArrayBlockingQueue<>(100),  // 有界队列！任务先排队
    new ThreadPoolExecutor.CallerRunsPolicy()   // 队列满+线程满 → 提交者自己干（背压）
);
```

执行顺序必须背下来：**core 满 → 入队列 → 队列满 → 扩到 max → max 满触发拒绝策略**。常见误区是「先扩线程再排队」——实际相反，所以队列给无界（LinkedBlockingQueue 默认）会让 max 参数形同虚设，内存最终被撑爆。

拒绝策略四种：AbortPolicy（抛异常，默认）、CallerRunsPolicy（调用者执行，天然限速，SelfBlog 选它）、DiscardPolicy（静默丢弃，危险）、DiscardOldestPolicy（丢最老任务）。

### 2.8.5.5 虚拟线程：Java 21 的并发范式转变

传统平台线程 1:1 映射 OS 线程，阻塞一个系统调用就白占一个「贵」线程（默认 1MB 栈）；虚拟线程由 JVM 调度，阻塞时**让出载体线程**，百万并发也不怕：

```java
// SelfBlog 若有「批量抓取文章外链预览」需求：
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Preview>> futures = urls.stream()
        .map(url -> executor.submit(() -> fetchPreview(url)))   // 每任务一个虚拟线程
        .toList();
    // 阻塞等待没关系——虚拟线程阻塞近乎免费，这是 IO 密集任务的天堂
}
```

**适用边界**：IO 密集（网络/DB 调用多）→ 虚拟线程大赢；CPU 密集 → 线程数没意义，核数才是上限。**它不替代线程池思维**，而是让「一个请求一个线程」的朴素模型重新变得可行。注意 synchronized 里的阻塞**不会**让出载体线程（JDK 21 限制，pinning 问题），热点锁路径改用 ReentrantLock 才能吃到虚拟线程红利。

### 2.8.5.6 CompletableFuture：异步编排的正确姿势

```java
// 并行拉三个数据源，全部完成后聚合——串行 3s → 并行 1s
CompletableFuture<PostVO> postF = CompletableFuture.supplyAsync(() -> postQuery.get(id), pool);
CompletableFuture<List<TagVO>> tagsF = CompletableFuture.supplyAsync(() -> tagQuery.byPost(id), pool);
CompletableFuture<Long> likesF  = CompletableFuture.supplyAsync(() -> likeQuery.count(id), pool);

PostDetailVO detail = postF.thenCombine(tagsF, (post, tags) -> post.setTags(tags))
                            .thenCombine(likesF, (post, likes) -> post.setLikeCount(likes))
                            .join();     // 必须传自定义 pool；默认 ForkJoinPool.commonPool 不可控
```

记住三件事：**supplyAsync 必传自定义线程池**；`thenApply`（同步转换）/`thenCompose`（异步串联，避免 Future 套 Future）/`thenCombine`（合并两个独立 Future）三兄弟的分工；`exceptionally` 兜底单链异常，`handle` 同时管正常与异常。

---


## 2.9 网络编程基础：HTTP 与 RESTful

第三章的 Spring MVC 是「把 HTTP 请求映射到 Java 方法」的框架。在学它之前，先把 HTTP 协议本身的要点和 RESTful 设计风格过一遍——接口设计的品味，是在这一节养成的。

### 2.9.1 HTTP 协议要点

HTTP 是基于 TCP 的**请求-响应**协议：客户端发一个请求报文，服务端回一个响应报文，一次连接一轮。HTTP/1.1 默认通过 `Connection: keep-alive` 复用 TCP 连接，HTTP/2 起引入二进制分帧与多路复用。

**请求报文结构**：

```text
POST /api/auth/login HTTP/1.1          ← 请求行：方法 + 路径 + 版本
Host: localhost:8825                   ← 请求头开始
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Length: 46

{"username":"admin","password":"***"}  ← 请求体
```

**响应报文结构**：

```text
HTTP/1.1 200 OK                        ← 状态行：版本 + 状态码 + 短语
Content-Type: application/json

{"code":200,"message":"成功","data":{...}}
```

**常用方法**（Spring MVC 按方法把请求路由到不同处理方法）：

| 方法 | 语义 | 幂等* | SelfBlog 中的例子 |
|------|------|-------|--------------------|
| GET | 读取资源 | 是 | `GET /posts/{id}` 文章详情 |
| POST | 创建资源 / 提交处理 | 否 | `POST /guest-comments` 新留言 |
| PUT | 全量更新 | 是 | `PUT /admin/posts/{id}` 整篇更新 |
| PATCH | 部分更新 | 否 | 改单个字段（项目里常用 PUT 代替） |
| DELETE | 删除资源 | 是 | `DELETE /admin/posts/{id}` |

*幂等：同一请求执行一次和执行多次，效果相同。GET/PUT/DELETE 幂等是「语义约定」，浏览器与代理据此决定是否重发请求。

**状态码五类**，后端最常用的一批：

| 状态码 | 含义 | SelfBlog 中的真实用法 |
|--------|------|------------------------|
| 200 OK | 成功 | 常规返回 |
| 201 Created | 创建成功 | 新建资源成功时 |
| 204 No Content | 成功但无响应体 | 删除成功 |
| 301/308 | 永久重定向 | `/index.html` → `/` 的 308 规范化 |
| 304 Not Modified | 缓存仍有效 | 静态资源协商缓存 |
| 400 Bad Request | 参数错误 | 校验失败 |
| 401 Unauthorized | 未认证 | 未登录调用需要登录的接口 |
| 403 Forbidden | 已认证但无权限 | 非管理员访问 `/api/admin/**` |
| 404 Not Found | 资源不存在 | 打开一篇已删除的文章 |
| 429 Too Many Requests | 触发限流 | `RateLimitInterceptor` 超限返回 429 JSON |
| 500 Internal Server Error | 服务端错误 | `GlobalExceptionHandler` 的兜底 |

一个值得注意的设计差异：SelfBlog 的统一响应是 `Result<T>{code, message, data, timestamp}` 信封，HTTP 状态码与业务 code 是**两套编号**——HTTP 200 携带 `{"code": 401, ...}` 也存在（由 `GlobalExceptionHandler` 把业务 code 映射到对应 HTTP 状态）。读到第三章再回头看这个权衡：信封让前端只需解析一种结构，状态码让基础设施（网关、监控、浏览器）能正确判断成败，两者配合而非互斥。

**关键请求头/响应头**：

| 头 | 方向 | 作用 | SelfBlog 例子 |
|----|------|------|---------------|
| `Content-Type` | 双向 | 正文媒体类型 | `application/json;charset=UTF-8` |
| `Authorization` | 请求 | 认证凭据 | `Bearer <JWT>`（2.10 节） |
| `Cookie` / `Set-Cookie` | 请求/响应 | 会话凭据与小状态 | 记住登录态 |
| `Cache-Control` | 响应 | 缓存策略 | `no-store` 强制后台壳不缓存 |
| `X-Forwarded-For` | 请求 | 经代理时传递真实 IP | 反向代理后取客户端 IP |
| `X-Forwarded-Proto` | 请求 | 经代理时传递原始协议 | HTTPS 判断 |

### 2.9.2 Cookie 与 Session

HTTP 本身无状态——服务器不记得「上一个请求和这一个来自同一个人」。Cookie 与 Session 是补上状态的经典组合：

- **Cookie**：服务器通过 `Set-Cookie` 响应头让浏览器存一小段键值对，之后浏览器对同域的每个请求自动带上 `Cookie` 头。它是「存在客户端的状态」，有大小（约 4KB）与数量限制，可设置 `Expires`/`Max-Age`（有效期）、`HttpOnly`（禁止 JS 读取，防 XSS 偷取）、`Secure`（仅 HTTPS 传输）、`SameSite`（限制跨站携带，防 CSRF）。
- **Session**：状态存在**服务端**，Cookie 里只存一个不透明的会话 ID（如 `JSESSIONID`）。每个请求服务器凭 ID 找回会话数据。它解决了「敏感状态不能放客户端」的问题，代价是服务端要存会话、多实例部署时要解决会话共享。

传统 Session 方案在单体应用简单可靠；但「状态在服务端」意味着横向扩容要么粘性路由（同一用户固定打到同一实例），要么集中存储（Redis 存 Session）。**无状态的令牌方案（JWT，2.10.3 节）正是为了绕开这些代价**——SelfBlog 选择 JWT：服务端不存会话，令牌自证身份，天然适配部署。

### 2.9.3 RESTful 风格设计

REST（Representational State Transfer）是一种**用 HTTP 本身的语义设计接口**的风格，核心原则三条：

1. **资源为中心的 URL**：URL 是名词、是资源的标识，不含动词——动作由 HTTP 方法表达：

```text
GET    /api/posts              文章列表（分页）
GET    /api/posts/42           42 号文章详情
POST   /api/admin/posts        新建文章
PUT    /api/admin/posts/42     更新 42 号文章
DELETE /api/admin/posts/42     删除 42 号文章
GET    /api/posts/42/comments  42 号文章的评论（资源的子资源）
```

对照反例感受差距：`/getUserById?id=42`（URL 里写动词）、`/deleteUser`（动作进 URL）、`/api/getPostList`（GET/POST 全用 POST）。RESTful 版本里，「对同一资源做什么」只看方法，URL 结构即 API 文档。

2. **无状态**：每个请求携带全部凭据（如 `Authorization: Bearer <JWT>`），服务端不依赖「上一次请求」的记忆。SelfBlog 每个管理接口都带 JWT，就是无状态原则的落地。

3. **统一接口 + 分层表达**：请求用 `Accept` 表达想要的表示，响应用标准状态码表达结果；资源之间用引用关联（评论里返回 `postId` 而不是内嵌整篇文章）。

设计 checklist（SelfBlog 的真实约定对照）：

- [x] URL 用复数名词：`/posts` 而非 `/post`；
- [x] 动作用 HTTP 方法，路径不出现动词；
- [x] 层级表达从属：`/posts/{id}/comments`；
- [x] 过滤/分页用查询参数：`?page=1&size=10&keyword=java&status=1`；
- [x] 版本与公共前缀集中管理：SelfBlog 由 `WebMvcConfig` 统一加 `/api` 前缀，控制器源码不写；
- [x] 认证走请求头而非 URL 参数：`Authorization: Bearer`，避免凭据进日志；
- [x] 错误响应结构统一：`Result` 信封 + 全局异常处理器，前端永远解析同一种错误格式；
- [x] 公开与管理接口分开：`/api/**` 公开只读，`/api/admin/**` 需管理员角色——权限边界画在 URL 结构上，`SecurityConfig` 一条规则覆盖一片。

REST 不是强制标准而是风格共识，实践中允许务实偏离（比如批量删除用 `POST /admin/posts/batch-delete` 因为要带请求体且语义复杂）。**判断标准是：另一个开发者不看文档能否猜对一半接口**——能，就是好设计。第三章的 Spring MVC 会把这些 URL 设计映射成 `@GetMapping`/`@PostMapping` 注解，本节打好的地基到时候直接受益。

## 2.10 加密与安全基础

SelfBlog 的登录体系由三块技术拼成：BCrypt 存密码、JWT 做无状态令牌、Spring Security 做访问控制（第三章展开）。本节讲清前两块的原理，以及对称/非对称加密的常识——它们是理解一切「登录为什么安全」的前提。

### 2.10.1 对称与非对称加密

| 维度 | 对称加密 | 非对称加密 |
|------|----------|------------|
| 密钥 | 加密解密同一把 | 公钥加密、私钥解密（成对） |
| 代表算法 | AES、SM4 | RSA、ECC、SM2 |
| 速度 | 快（硬件加速，GB/s 级） | 慢（比对称慢几个数量级） |
| 密钥分发难题 | 双方都要拿到同一把密钥，怎么安全地送过去？ | 公钥可以公开，不存在分发难题 |
| 典型用途 | 大批量数据加密 | 密钥交换、数字签名 |

两者互补：TLS（HTTPS）握手用非对称加密安全地协商出**会话密钥**，之后全部数据用对称加密传输——用对了各自的优点。直观类比：非对称加密是「敞开的信箱，任何人都可投信，只有持钥匙的主人能取」；对称加密是「两人共用的保险箱，快，但钥匙得先安全地人手一把」。

### 2.10.2 BCrypt：密码为什么不能「加密」存储

先把一个常见误区钉死：**数据库里绝不能存「可还原」的密码**。无论 AES 还是 Base64（后者根本不是加密，是编码），可还原就意味着密钥或算法一旦泄漏，所有用户密码裸奔，而且员工/攻击者能看到原文。

正确做法是存**单向哈希**：`hash = H(password)`，正向计算容易，反向推算在计算上不可行。但普通哈希（MD5、SHA-256）直接存密码仍然不行——攻击者拿着**彩虹表**（海量「明文 → 哈希」的预计算对照表）反查，短密码秒破；就算没有彩虹表，GPU 每秒能算上百亿次哈希，暴力枚举快得惊人。

BCrypt 是**专为密码设计的慢哈希**，四个关键设计：

1. **自动加盐（salt）**：每次哈希生成随机盐混入计算，同一密码每次得到的哈希都不同——彩虹表彻底失效（表无法预知盐值）；
2. **刻意缓慢**：内置可调成本因子（cost），如 10 表示 2^10 轮运算，单次约几十毫秒。用户登录一次毫无感知，攻击者暴力枚举的成本被放大百万倍；
3. **成本因子可随硬件升级**：把 10 调成 12 就能对抗硬件进步，算法自适应；
4. **自校验格式**：哈希串里带着算法版本、成本因子、盐，验证时全部可提取，无需额外存储：

```text
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 │  │  └────── 盐（22 字符）──────┘└──────── 哈希值 ────────┘
 │  └ 成本因子 2^10
 └ 算法版本
```

用法（SelfBlog 登录体系的真实做法，Spring Security 内置支持）：

```java
// 注册/修改密码时
String hashed = new BCryptPasswordEncoder().encode(rawPassword);   // 每次 encode 结果都不同——盐是随机的
userRepository.savePassword(userId, hashed);

// 登录校验时：绝不能「哈希后比较字符串」（盐每次不同，结果必然不等）
boolean matches = passwordEncoder.matches(rawPassword, storedHash); // 正确：交给 matches 提取盐重算比对
```

注意 `matches` 的语义：从存储串里取出盐和成本因子，用同样的盐把输入密码再哈希一遍，比较结果。**「同一密码每次哈希值不同，但仍能验证」**，这就是加盐慢哈希的精妙之处。

### 2.10.3 JWT 结构原理

JWT（JSON Web Token）是 SelfBlog 登录态的载体。登录成功后服务端签发一个令牌，之后每个请求经 `Authorization: Bearer <JWT>` 头携带，服务端**验签通过即信**——不在任何地方存会话。

JWT 是三段 Base64URL 编码用 `.` 连接的字符串：

```text
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiI0MiIsInJvbGUiOiJhZG1pbiJ9 . SflKxwRJSMeKKF2QT4f...
└──── ① Header ────┘  └───── ② Payload ─────┘  └──── ③ Signature ────┘
```

- **① Header**：声明签名算法与令牌类型，`{"alg":"HS256","typ":"JWT"}`；
- **② Payload**：业务声明（claims），如 `{"sub":"42","role":"admin","exp":1735689600,"iat":1735686000}`。`exp` 过期时间、`iat` 签发时间是标准字段，`sub`/`role` 由应用自定义；
- **③ Signature**：对「前两段的拼接」用密钥做 HMAC-SHA256 签名：`HMAC(base64(header) + "." + base64(payload), secret)`。

**验签是 JWT 安全性的全部**：篡改 Payload 中任何字符（比如把 `role":"admin"` 改成自己想要的值），签名就对不上，服务端直接拒绝。密钥只存在服务端（SelfBlog 通过 `.env` 的 `JWT_SECRET` 注入，要求 ≥32 位），拿不到密钥就伪造不出合法签名。

两个必须刻在脑子里的特性：

1. **Payload 是明文的**。Base64 是编码不是加密，任何人拿到令牌都能解码读出所有 claims——**绝不能往 JWT 里放密码、密钥等敏感信息**。放用户 ID、角色这类「可公开但不许篡改」的信息没问题。
2. **签发后无法主动作废**。无状态是双刃剑：服务端不存会话，也就没有「注销」可删——令牌没到期就一直有效（除非服务端引入黑名单，那又回到了有状态）。所以 JWT 要配短有效期 + 刷新令牌机制，SelfBlog 前端 `validateToken` 只本地解码判断过期、请求拦截器负责续期重放，正是这套机制的工程化。

一个 SelfBlog 式的完整链路复盘：登录 → `AuthController` 校验 BCrypt → 签发含 `userId` 与角色的 JWT → 前端存 localStorage（经 `utils/crypto` 编解码）→ 后续请求带 `Authorization` 头 → `JwtAuthenticationFilter` 验签并解析出用户 → `SecurityUtils.getCurrentUserId()` 从安全上下文取人（2.13.3 节）→ `SecurityConfig` 按 URL 决定放行或拒绝。这条链横跨本节（BCrypt/JWT）、2.9 节（HTTP 头）与第三章（Spring Security 全景）——单点知识串成链，才算真懂登录。


## 2.11 单元测试入门

SelfBlog 后端有 **588 个测试、0 失败**作为基线（CI 还会用真实 MySQL 8 再跑一遍），前端另有 43 个文件 400 个测试。这套测试不是负担，而是这个单人项目敢持续重构的底气——每次改动后一条命令验证一切照旧。本节讲 JUnit 5 与 Mockito 的入门，让第三章开始你写的每一行代码都带测试。

### 2.11.1 JUnit 5 基础

JUnit 5 由三部分组成：测试引擎、编程模型（注解与断言）、运行平台。Spring Boot 的 `spring-boot-starter-test` 已自动引入，无需额外依赖。

**生命周期与基本结构**：

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("文章服务测试")
class PostServiceTest {

    private PostService postService;

    @BeforeAll
    static void initAll() {
        // 所有测试开始前执行一次（必须是 static）
    }

    @BeforeEach
    void setUp() {
        // 每个测试方法前都执行——保证每个测试从干净状态开始
        postService = new PostService();
    }

    @Test
    @DisplayName("浏览量小于阈值时不标记热门")
    void shouldNotMarkHotWhenViewsBelowThreshold() {
        boolean hot = postService.isHot(999L);
        assertFalse(hot, "999 浏览不应标记热门");
    }

    @Test
    @DisplayName("浏览量超过阈值时标记热门")
    void shouldMarkHotWhenViewsAboveThreshold() {
        assertTrue(postService.isHot(2000L));
    }

    @AfterEach
    void tearDown() { /* 每个测试后执行 */ }

    @AfterAll
    static void tearDownAll() { /* 所有测试结束后执行一次 */ }
}
```

核心注解速查：

| 注解 | 作用 |
|------|------|
| `@Test` | 标记测试方法（2.3.4 节自己实现过它的原理） |
| `@DisplayName` | 测试报告里显示的中文描述 |
| `@BeforeEach` / `@AfterEach` | 每个测试方法前/后执行（夹具准备与清理） |
| `@BeforeAll` / `@AfterAll` | 整个测试类前后各执行一次（须 static） |
| `@Disabled("原因")` | 暂时跳过 |
| `@ParameterizedTest` | 参数化测试，见下 |

一个关键认知：**每个 `@Test` 方法都运行在全新的测试类实例上**（JUnit 默认 `PER_METHOD` 生命周期），所以测试方法之间不会通过实例字段互相污染——这是「测试相互独立」这一纪律的语言层保障。

**断言（Assertions）**：

```java
assertEquals(10, result);                    // 相等（预期值在前，实际值在后）
assertNotEquals(0, list.size());
assertTrue(post.isHot(), "额外说明文案");      // 布尔断言
assertFalse(post.isDeleted());
assertNull(userByNullId);
assertNotNull(createdPost);
assertThrows(BusinessException.class,         // 断言抛出指定异常
        () -> postService.getById(999L));     // lambda 形式的可执行体
assertAll("文章校验",                          // 分组断言：全部执行再统一报告
        () -> assertEquals("标题", post.getTitle()),
        () -> assertEquals(1, post.getStatus()));
assertIterableEquals(expected, actual);       // 集合逐元素比较
```

`assertThrows` 值得单独一提：它同时验证了「错误路径」——不仅代码要会在该错的时候错，还得错成预期的类型。SelfBlog 测试里大量出现「非管理员 403、匿名 403」这类负例断言，日志里成串的 `[PERF] ... error:` 大多是这类正常负例，不是失败。

**参数化测试**——同一逻辑喂多组数据，消灭复制粘贴的测试方法：

```java
@ParameterizedTest
@ValueSource(strings = {"", "   ", "\t\n"})
@DisplayName("空白标题一律拒绝")
void shouldRejectBlankTitle(String badTitle) {
    assertThrows(BusinessException.class, () -> postService.create(badTitle, "正文"));
}

@ParameterizedTest
@CsvSource({
        "1,    false",
        "1000, false",
        "1001, true",
        "9999, true"
})
@DisplayName("浏览量阈值边界（1000 为界）")
void thresholdBoundary(long views, boolean expected) {
    assertEquals(expected, postService.isHot(views));
}
```

`@CsvSource` 的这组数据专门覆盖**边界值**（1000 与 1001）——边界是 bug 高发区，测试的价值密度最高。常用数据源还有 `@NullSource`、`@EmptySource`、`@MethodSource`（复杂对象用）。

### 2.11.2 Mockito 基础

单元测试的铁律是**只测当前单元**：测 Service 时，Mapper 层不该真连数据库——慢（每个测试起数据库）、脆（数据变化导致误报）、窄（无法模拟数据库挂了的情况）。Mockito 用「替身对象」隔离依赖：

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private BlogPostMapper postMapper;          // 模拟的依赖

    @InjectMocks
    private PostQueryServiceImpl postService;   // 被测对象，Mockito 自动注入 @Mock

    @Test
    void shouldReturnPostWhenExists() {
        // given：先「打桩」——规定模拟对象被调用时的行为
        BlogPost post = new BlogPost();
        post.setId(1L);
        post.setTitle("虚拟线程入门");
        when(postMapper.selectById(1L)).thenReturn(post);

        // when：调用被测方法
        BlogPost result = postService.getById(1L);

        // then：验证结果与交互
        assertNotNull(result);
        assertEquals("虚拟线程入门", result.getTitle());
        verify(postMapper, times(1)).selectById(1L);   // 交互次数也对
    }

    @Test
    void shouldThrowWhenNotFound() {
        when(postMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> postService.getById(999L));
    }
}
```

三段式 `given / when / then` 是可读性最好的测试结构，值得作为固定习惯。Mockito 常用 API 一览：

| API | 作用 |
|-----|------|
| `when(mock.method(args)).thenReturn(v)` | 打桩：调用时返回 v |
| `when(...).thenThrow(new XxxException())` | 调用时抛异常（模拟故障） |
| `any()` / `anyLong()` / `eq(1L)` | 参数匹配器（混用时全参数都要用匹配器） |
| `verify(mock, times(n)).method(...)` | 验证交互发生了 n 次 |
| `@Spy` | 部分模拟：真对象 + 可覆写指定方法 |

### 2.11.3 @Mock 与 @MockBean 的区别

这两个名字很像的注解作用域完全不同，混用是 Spring 测试的新手重灾区：

| 维度 | `@Mock`（Mockito） | `@MockBean`（Spring Boot） |
|------|--------------------|-----------------------------|
| 作用域 | 纯 Java 对象级，**不启动 Spring 容器** | 替换容器里的 Bean 定义 |
| 速度 | 极快（毫秒级） | 慢（要装配应用上下文） |
| 典型搭配 | `@ExtendWith(MockitoExtension.class)` + 直接 new 被测对象 | `@WebMvcTest` / `@SpringBootTest` 切片测试 |
| 适用场景 | 测 Service 的纯业务逻辑 | Mock 掉「容器自动装配链路上绕不开」的 Bean |

SelfBlog 测试代码里有一条来自实践的注记，正好作为真实案例：**管理员控制器的 `@WebMvcTest` 切片测试必须 `@MockBean JwtUtils`**——因为 `SecurityConfig` 装配时拉起 `jwtAuthenticationFilter`，过滤器依赖 `JwtUtils`；不 Mock 掉它，整个上下文启动直接失败。这类「不 Mock 就起不来」的依赖，就是 `@MockBean` 的用武之地；而业务校验逻辑（`isHot` 的阈值判断）用 `@Mock` 就够了，跑得飞快。**测试金字塔的分层原则**：能用纯 Mockito 的就不起 Spring 容器，容器级测试留给「装配正确性」的验证。

### 2.11.4 TDD 思想简介

TDD（Test-Driven Development）把常规流程倒过来：**先写测试，再写实现**。一个循环三步，称为「红-绿-重构」：

1. **红**：写一个会失败的测试——它描述你**还没实现**的行为。此刻测试编译通过、运行失败，失败信息就是你的待办清单；
2. **绿**：用**最简单**的方式让测试通过（哪怕实现丑陋）；
3. **重构**：测试是安全网，放心整理实现——提取方法、消除重复、改善命名，每一步都跑测试。

一个最小的例子（先写测试）：

```java
@Test
@DisplayName("未登录用户无法收藏文章")
void guestCannotCollect() {
    SecurityUtils mocked = mockGuestContext();          // 模拟匿名
    assertThrows(BusinessException.class,
            () -> collectService.collect(42L));          // 此刻方法还不存在，编译失败=红灯
}
```

红灯逼你先想清楚 API 长什么样（方法名、参数、异常类型），实现只是随后补上的细节。TDD 的深层价值有三个：**可测试性倒逼设计**（不好测的代码必然是耦合重的代码）、**回归保护即时生效**（测试与实现同生共死）、**活文档**（测试即使用说明书，永远与代码同步）。不必教条地处处 TDD——SelfBlog 的 588 个测试也并非全部先于实现——但「每个功能补齐测试、每次修 bug 先写复现测试」的纪律，是这个项目能在单人维护下保持基线永远绿色的根本原因。修 bug 前先写一个能复现 bug 的失败测试，修复后它自动变成回归守卫——这一条习惯，胜过读十篇测试教程。


## 2.12 设计模式实战精选

学设计模式最常见的坑是背概念——把 23 种模式的类图背得滚瓜烂熟，写代码时一个也想不起来。本节换一种学法：**每个模式从「不用它会写出什么烂代码」讲起**，再结合 Spring/SelfBlog 的真实落地。原 2.6 节的六个模式全部保留在此并深度扩写。

### 2.12.1 单例模式

**动机**：一个类只需要一个实例——全局配置、线程池管理器、缓存管理器。不用单例的后果：每个 `new` 都产生一份副本，状态分裂、资源浪费，甚至引发并发 bug。

原文给出四种写法，我们逐一分析线程安全性与优劣：

```java
// 饿汉式
public class SingletonEager {
    private static final SingletonEager INSTANCE = new SingletonEager();
    
    private SingletonEager() {}
    
    public static SingletonEager getInstance() {
        return INSTANCE;
    }
}

// 懒汉式（线程安全）
public class SingletonLazy {
    private static volatile SingletonLazy instance;
    
    private SingletonLazy() {}
    
    public static SingletonLazy getInstance() {
        if (instance == null) {
            synchronized (SingletonLazy.class) {
                if (instance == null) {
                    instance = new SingletonLazy();
                }
            }
        }
        return instance;
    }
}

// 静态内部类（推荐）
public class SingletonHolder {
    private SingletonHolder() {}
    
    private static class Holder {
        private static final SingletonHolder INSTANCE = new SingletonHolder();
    }
    
    public static SingletonHolder getInstance() {
        return Holder.INSTANCE;
    }
}

// 枚举（最安全）
public enum SingletonEnum {
    INSTANCE;
    
    public void doSomething() {
        System.out.println("单例方法");
    }
}
```

**四种写法逐一点评**：

1. **饿汉式**：类加载时就创建实例（2.6.3 节的类加载机制保证 `<clinit>` 由 JVM 加锁，天然线程安全）。缺点是不管用不用都创建——如果构造开销大且从未使用，就是浪费。
2. **懒汉式（双重检查锁 DCL）**：第一次 `getInstance` 才创建。两个 `if` 缺一不可：外层避免每次都抢锁，内层防止两个线程先后通过外层判断后重复创建。**`volatile` 不可省略**——`instance = new SingletonLazy()` 不是原子操作，它分「分配内存 → 初始化 → 赋值引用」三步，若无 volatile，指令重排可能让别的线程拿到一个「已赋值但未初始化完成」的对象（2.1.8 节 volatile 的实战意义就在此）。
3. **静态内部类**：兼顾懒加载与线程安全——`Holder` 类只在第一次 `getInstance` 时才被加载，JVM 类加载锁保证单例，且没有任何同步代码块。**常规场景的首选**。
4. **枚举**：《Effective Java》力荐。天然防反射攻击（反射无法调用枚举构造器）、防反序列化破坏单例（枚举序列化由 JVM 特殊处理），写法最简。缺点是不能懒加载、不能继承。

还有一种被广泛使用但值得警惕的写法——**自持有的 `INSTANCE` 静态字段单例**：

```java
public class Singleton {
    public static final Singleton INSTANCE = new Singleton();   // 饿汉式的变体
    private Singleton() {}
}
```

它与饿汉式本质相同。真正要警惕的是**用可变的静态字段承载全局状态**：测试无法隔离、并发修改难排查。所以进入 Spring 世界后有了更重要的结论——**Spring 容器的默认 Bean 作用域就是单例（singleton）**，`@Service`/`@Component` 注册的 Bean 全容器一份。但它与传统单例不同：Spring 的单例是「容器级注册表保证唯一」，不是「私有构造器 + 静态方法」，因此**无状态的单例 Bean 才是安全默认**（SelfBlog 的 Service 全是无状态单例，请求相关数据通过参数传递或 `SecurityContextHolder` 获取——后者正是 2.1.9 节 ThreadLocal 的框架级应用）。

### 2.12.2 工厂模式

**动机**：把「选择哪个实现类」的分支逻辑从业务代码里赶出去。对比原文的三种形态：

```java
// 简单工厂
public class SimpleFactory {
    public static Product createProduct(String type) {
        switch (type) {
            case "A": return new ProductA();
            case "B": return new ProductB();
            default: throw new IllegalArgumentException("未知类型");
        }
    }
}

// 工厂方法
public interface ProductFactory {
    Product createProduct();
}

public class ProductAFactory implements ProductFactory {
    @Override
    public Product createProduct() {
        return new ProductA();
    }
}

// 抽象工厂
public interface AbstractFactory {
    ProductA createProductA();
    ProductB createProductB();
}

public class ConcreteFactory implements AbstractFactory {
    @Override
    public ProductA createProductA() {
        return new ProductA();
    }
    
    @Override
    public ProductB createProductB() {
        return new ProductB();
    }
}
```

演进逻辑值得体会：**简单工厂**把 `new` 收拢到一个静态方法，新增产品要改 switch（违反开闭原则）；**工厂方法**把每种产品的工厂各自封装，新增产品 = 新增一个工厂类，不改旧代码；**抽象工厂**解决「一族相关产品」的成套创建问题。

而 **Spring IoC 容器本身就是一个超级工厂**：`@Component` 类注册进容器，`@Autowired` 声明「我要一个这种类型的实例」，容器负责创建与装配。你在 SelfBlog 里几乎从不手写 `new UserService()`——「对象怎么来的」这个细节被容器接管，工厂模式从「手写模式」升级为「基础设施」。理解这一点，你就明白为什么说 Spring 的本质是工厂 + 依赖注入。

### 2.12.3 代理模式

**动机**：在不改动目标类的前提下，给方法调用织入通用逻辑（日志、事务、缓存、权限）。原文用静态代理引入概念：

```java
// 静态代理
interface Subject {
    void request();
}

class RealSubject implements Subject {
    @Override
    public void request() {
        System.out.println("真实对象处理请求");
    }
}

class Proxy implements Subject {
    private RealSubject realSubject;
    
    @Override
    public void request() {
        if (realSubject == null) {
            realSubject = new RealSubject();
        }
        System.out.println("代理前处理");
        realSubject.request();
        System.out.println("代理后处理");
    }
}

// 动态代理（见 2.2.4 节）
```

静态代理的痛点一眼可见：每个目标类都要手写一个代理类，方法一多就是体力劳动。**动态代理**（2.2.4、2.2.5 节已深入：JDK 接口代理与 CGLIB 子类代理）在运行期自动生成代理类，一个 `InvocationHandler` 拦截所有方法——这正是 **Spring AOP 的实现方式**。

SelfBlog 的 `PerformanceMonitoringAspect`（下一节 2.13.1）就是代理模式的直接应用：容器里注入的其实是包着目标 Bean 的代理对象，调用先穿过切面逻辑再到达真实方法。2.2.5 节的两个经典现象（`this` 自调用失效、`final` 方法失效）全部源于代理的本质——**增强只发生在「经代理对象调用」时**。

### 2.12.4 观察者模式

**动机**：一件事发生，多个相关方都要响应，但发布者不该知道（也不该依赖）响应者是谁。原文的 JDK 经典实现：

```java
import java.util.*;

// 观察者接口
interface Observer {
    void update(String message);
}

// 主题接口
interface Subject {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObservers(String message);
}

// 具体主题
class ConcreteSubject implements Subject {
    private List<Observer> observers = new ArrayList<>();
    
    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }
    
    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }
    
    @Override
    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }
}

// 具体观察者
class ConcreteObserver implements Observer {
    private String name;
    
    public ConcreteObserver(String name) {
        this.name = name;
    }
    
    @Override
    public void update(String message) {
        System.out.println(name + " 收到消息：" + message);
    }
}

// 测试
public class ObserverPatternDemo {
    public static void main(String[] args) {
        ConcreteSubject subject = new ConcreteSubject();
        
        Observer observer1 = new ConcreteObserver("观察者1");
        Observer observer2 = new ConcreteObserver("观察者2");
        
        subject.attach(observer1);
        subject.attach(observer2);
        
        subject.notifyObservers("新消息");
    }
}
```

这段代码的精髓在 `ConcreteSubject` 与 `Observer` 的关系：**主题只认识接口，不认识任何具体观察者**——新增一种响应方，主题代码零改动。这是「针对接口编程」最直观的胜利。

**Spring 事件机制是它的企业级版本**，Spring Boot 应用启动完成时发布 `ApplicationReadyEvent`，监听器模式如下：

```java
// 1. 自定义事件（可以携带任意数据）
public class PostPublishedEvent {
    private final Long postId;
    private final String title;
    public PostPublishedEvent(Long postId, String title) {
        this.postId = postId;
        this.title = title;
    }
    // getter 省略
}

// 2. 发布（发布者不关心谁在听）
service.publishPost(post);
applicationEventPublisher.publishEvent(new PostPublishedEvent(post.getId(), post.getTitle()));

// 3. 订阅（@EventListener 自动注册，多个监听器互不影响）
@Component
public class PostPublishListeners {

    @EventListener
    public void refreshTrending(PostPublishedEvent event) {
        // 刷新热门榜单缓存
    }

    @Async                          // 异步监听：不阻塞发布主流程（2.1.12 节的线程池在此生效）
    @EventListener
    public void sendNotify(PostPublishedEvent event) {
        // 订阅提醒等耗时操作
    }
}
```

对比手写版的优势一目了然：注册、线程模型、异常隔离、生命周期管理全部由框架接管。**观察者模式的边界也要心里有数**：监听器抛异常默认会影响同步发布流程（除非 `@Async` 或显式处理）；事件风暴会让调用链变得难以追踪。规则很简单——**强一致的主流程直接调用，弱关联的副反应走事件**。

### 2.12.5 策略模式

**动机**：同一个行为有多种实现，且需要在运行时切换。原文的经典实现：

```java
// 策略接口
interface Strategy {
    int execute(int a, int b);
}

// 具体策略
class AddStrategy implements Strategy {
    @Override
    public int execute(int a, int b) {
        return a + b;
    }
}

class SubtractStrategy implements Strategy {
    @Override
    public int execute(int a, int b) {
        return a - b;
    }
}

class MultiplyStrategy implements Strategy {
    @Override
    public int execute(int a, int b) {
        return a * b;
    }
}

// 上下文
class Context {
    private Strategy strategy;
    
    public Context(Strategy strategy) {
        this.strategy = strategy;
    }
    
    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }
    
    public int executeStrategy(int a, int b) {
        return strategy.execute(a, b);
    }
}

// 测试
public class StrategyPatternDemo {
    public static void main(String[] args) {
        Context context = new Context(new AddStrategy());
        System.out.println("加法：" + context.executeStrategy(10, 5));
        
        context.setStrategy(new SubtractStrategy());
        System.out.println("减法：" + context.executeStrategy(10, 5));
        
        context.setStrategy(new MultiplyStrategy());
        System.out.println("乘法：" + context.executeStrategy(10, 5));
    }
}
```

在 Spring 里这个模式化繁为简——**注入同一接口的多个实现**：

```java
public interface SortStrategy {
    boolean support(String key);
    List<PostSummary> sort(List<PostSummary> posts);
}

@Component
public class ViewsSortStrategy implements SortStrategy {
    public boolean support(String key) { return "views".equals(key); }
    public List<PostSummary> sort(List<PostSummary> posts) {
        return posts.stream().sorted(Comparator.comparingLong(PostSummary::views).reversed()).toList();
    }
}

@Component
public class LatestSortStrategy implements SortStrategy {
    public boolean support(String key) { return "latest".equals(key); }
    public List<PostSummary> sort(List<PostSummary> posts) {
        return posts.stream().sorted(Comparator.comparing(PostSummary::id).reversed()).toList();
    }
}

@Service
public class PostListService {
    private final List<SortStrategy> strategies;   // Spring 自动注入所有实现

    public PostListService(List<SortStrategy> strategies) {
        this.strategies = strategies;
    }

    public List<PostSummary> sortBy(String key, List<PostSummary> posts) {
        return strategies.stream()
                .filter(s -> s.support(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的排序: " + key))
                .sort(posts);
    }
}
```

`List<SortStrategy>` 注入消灭了工厂类与注册代码：**新增策略 = 新增一个 `@Component` 类**，其余一切自动生效。「多实现注入 + support 分派」是 Spring 项目里最高频的模式用法，也可以用 `Map<String, SortStrategy>`（key 为 Bean 名）做直查分派。对照 2.7.3 节的 Switch 模式匹配——一个用类型系统分派，一个用 Spring 容器分派，思想同源：**把「if-else 堆叠」改成「可插拔单元」**。

### 2.12.6 模板方法模式

**动机**：多个流程「骨架相同、细节各异」，把骨架上提到父类、细节留给子类。原文实现：

```java
// 抽象类
abstract class AbstractTemplate {
    // 模板方法（定义算法骨架）
    public final void templateMethod() {
        step1();
        step2();
        step3();
    }
    
    // 具体方法
    private void step1() {
        System.out.println("步骤1");
    }
    
    // 抽象方法（由子类实现）
    protected abstract void step2();
    
    // 钩子方法（可选重写）
    protected void step3() {
        System.out.println("步骤3（默认实现）");
    }
}

// 具体实现
class ConcreteTemplateA extends AbstractTemplate {
    @Override
    protected void step2() {
        System.out.println("步骤2（实现A）");
    }
}

class ConcreteTemplateB extends AbstractTemplate {
    @Override
    protected void step2() {
        System.out.println("步骤2（实现B）");
    }
    
    @Override
    protected void step3() {
        System.out.println("步骤3（重写实现B）");
    }
}

// 测试
public class TemplateMethodDemo {
    public static void main(String[] args) {
        AbstractTemplate templateA = new ConcreteTemplateA();
        templateA.templateMethod();
        
        System.out.println("---");
        
        AbstractTemplate templateB = new ConcreteTemplateB();
        templateB.templateMethod();
    }
}
```

注意模板方法上的 `final`：骨架顺序是契约，子类只准填空不准改流程。三个抽象层次（必须实现的抽象方法 / 可选覆盖的钩子方法 / 不许动的具体方法）是理解此模式的关键。

**JdbcTemplate 是这个模式的教科书案例**。「获取连接 → 开启事务 → 执行 → 处理结果 → 关闭资源」的骨架完全固定，变的只有「SQL 与结果映射」：

```java
// 骨架：JdbcTemplate 固定了资源管理与异常转换，调用方只填「核心一步」
List<PostSummary> posts = jdbcTemplate.query(
        "SELECT id, title, views FROM blog_post WHERE status = ?",
        (rs, rowNum) -> new PostSummary(rs.getLong("id"), rs.getString("title"), rs.getLong("views")),
        1);
```

回调写法（传 lambda/RowMapper）其实是模板方法的现代化身——**用组合（传入策略对象）替代了继承（子类重写）**。同理，`RestTemplate`、`KafkaTemplate`、MyBatis 的 `SqlSessionTemplate` 全是这个家族：`XxxTemplate` 命名的类，几乎都是「骨架固定 + 细节回调」的模板方法实践。Spring 事务的 `TransactionTemplate` 也是——第三章学 `@Transactional` 时，它的底层骨架与 2.8.4 节的手写事务代码一一对应。


## 2.13 以 SelfBlog 项目为例

前三节的语法知识，在 SelfBlog 里都有真实落点。本节把三个最有代表性的代码片段放回项目语境，看「进阶特性如何在工程里协作」。

### 2.13.1 性能监控切面

SelfBlog 项目使用 AOP 实现性能监控：

```java
@Aspect
@Component
public class PerformanceMonitoringAspect {
    
    @Around("@annotation(com.selfblog.annotation.PerformanceLog)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();
        
        String methodName = joinPoint.getSignature().getName();
        long duration = end - start;
        
        if (duration > 1000) {
            log.warn("[PERF] {} 执行耗时：{}ms", methodName, duration);
        }
        
        return result;
    }
}
```

逐层拆解这个切面用到的本章知识：

- **注解驱动**（2.3 节）：`@annotation(...)` 切点表示「只拦截标注了 `@PerformanceLog` 的方法」——运行期反射读注解 + 动态代理拦截的组合拳；
- **动态代理**（2.2.4/2.2.5 节）：`joinPoint.proceed()` 之前之后就是「代理前处理 / 代理后处理」，`@Around` 把方法调用整个包住，与静态代理示例的结构完全同构；
- **线程安全**（2.1 节）：切面本身无实例状态（`start`/`duration` 都是局部变量），多个请求并发穿过也不会互相干扰——单例 Bean 无状态化的直接体现；
- **日志里的意外收获**：跑 SelfBlog 后端测试时刷屏的 `[PERF] ... error:` 不是报错，而是这个切面记录的**正常负例断言**（测试故意让某些路径变慢/抛错来验证监控行为）。读懂一条框架日志，前提是懂它背后的机制。

想更贴近生产，可以把 `System.currentTimeMillis()` 换成 `System.nanoTime()`（单调时钟，不受系统时间调整影响），并考虑用 `CompletableFuture` 把慢日志异步落盘（2.1.11 节）。

### 2.13.2 线程池配置

SelfBlog 项目使用线程池处理异步任务：

```java
@Configuration
public class AsyncConfig {
    
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("SelfBlog-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

现在你可以完全读懂它（对照 2.1.6 节的七参数）：核心 5、队列 100、最大 10 的组合意味着——**这台服务的异步任务在满负荷时会先堆 100 个进队列，队列满了才扩到 10 个线程，再满就触发 `CallerRunsPolicy`**（提交线程自己跑，用「发布文章的线程亲自干异步活」换一份背压保护）。`SelfBlog-` 前缀让日志与 `jstack` 线程转储一眼可辨（2.6.4 节工具箱的用武之地）。`@Async` 方法默认就走这个名为 `taskExecutor` 的池（2.1.12 节演示了标注与验证方法）。

一个思考题留给你：如果 SelfBlog 升级到 `spring.threads.virtual.enabled=true`（2.7.1 节），这个 `AsyncConfig` 应该怎么调整？提示——虚拟线程不该被池化，但「限制并发数以保护下游」的需求依然存在，Semaphore（信号量）/限流器是新的答案。

### 2.13.3 工具类

SelfBlog 项目中的 `SecurityUtils` 工具类：

```java
public class SecurityUtils {
    
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        return (Long) authentication.getPrincipal();
    }
    
    public static Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
```

两个方法各司其职，风格差异是设计功力：`getCurrentUserId()` **失败即抛**（401），用在「必须登录」的写接口；`getCurrentUserIdOrNull()` 吞掉异常返回 `null`，用在「游客可访问」的读接口（详情页判断是否作者本人）。异常语义与业务语义严格对应。

藏在背后的机制是本章 2.1.9 节的 ThreadLocal：`SecurityContextHolder` 把当前请求的认证信息存在 ThreadLocal 里，同一个请求内任意代码都能取到，请求结束由 Spring Security 的过滤器清理（线程池复用线程，不清理就是串号事故）。也正因如此，**把它传给异步线程时必须显式传递用户上下文**（或改用 TransmittableThreadLocal/InheritableThreadLocal 评估）——ThreadLocal 的继承规则与线程池的复用模型冲突，2.1.9 节的内存泄漏风险在这里具象化。

## 2.14 总结

本章介绍了 Java 的高级特性，包括：

1. **多线程编程**：Thread、Runnable、Callable、线程池（七参数与拒绝策略）、synchronized 与 Lock、volatile 与内存可见性、ThreadLocal、并发集合、CompletableFuture 与 Spring `@Async`
2. **反射机制**：Class 类、Field/Method/Constructor 操作、Method.invoke、JDK 动态代理与 CGLIB（Spring AOP 的基础）
3. **注解**：元注解、内置注解、自定义注解、运行时处理（迷你测试框架）、SelfBlog 注解生态
4. **Lambda 表达式与函数式接口**：Consumer、Supplier、Function、Predicate
5. **Stream API**：创建、中间操作、终止操作、并行流纪律
6. **JVM 基础**：运行时内存区域、垃圾回收与收集器（CMS/G1/ZGC）、类加载与双亲委派、常用 JVM 参数与诊断工具
7. **Java 21 新特性**：虚拟线程（`spring.threads.virtual.enabled`）、Record 与 Record Pattern、Switch 模式匹配与 sealed
8. **JDBC 与数据库编程**：PreparedStatement 防注入、HikariCP 连接池、事务隔离级别与 Spring 传播行为
9. **网络编程基础**：HTTP 方法/状态码/头/Cookie/Session、RESTful 设计
10. **加密与安全基础**：对称/非对称加密、BCrypt 慢哈希、JWT 结构与验签
11. **单元测试**：JUnit 5 生命周期与断言、参数化测试、Mockito 与 `@Mock`/`@MockBean`、TDD 与 SelfBlog 588 测试的工程价值
12. **设计模式**：单例、工厂、代理（Spring AOP）、观察者（Spring 事件）、策略（Spring 多实现注入）、模板方法（JdbcTemplate）

通过本章的学习，你应该掌握了 Java 的高级特性，能够编写更加高效、优雅的 Java 程序。更重要的是，你已经具备了读懂 Spring Boot「魔法」的能力：`@Service` 背后是 IoC 工厂，`@Transactional` 背后是动态代理与传播行为，`@Cacheable` 背后是 Caffeine 单例缓存，`@Test` 背后是反射与注解处理，`spring.threads.virtual.enabled` 背后是虚拟线程的装载与卸载。在接下来的章节中，我们将学习 Spring Boot 框架，这是 Java 企业级开发的核心框架——而它的每一个部件，都是本章内容的延伸。

---

**参考资源：**

- [Oracle Java 并发编程指南](https://docs.oracle.com/javase/tutorial/essential/concurrency/)
- [Java 8 Stream API 官方文档](https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html)
- [Effective Java（第3版）](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/)
- [SelfBlog 项目源码](https://github.com/Xinghe-0203/SelfBlog)
- [JEP 444: Virtual Threads（Java 21 虚拟线程）](https://openjdk.org/jeps/444)
- [JEP 440: Record Patterns](https://openjdk.org/jeps/440) / [JEP 441: Pattern Matching for switch](https://openjdk.org/jeps/441)
- [HikariCP GitHub Wiki（含池大小计算）](https://github.com/brettwooldridge/HikariCP/wiki)
- [JUnit 5 官方用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 官方文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [OWASP SQL Injection Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html)
- [RFC 7519: JSON Web Token](https://datatracker.ietf.org/doc/html/rfc7519)

---

# 第三章 Spring Boot 框架

> 本章是整个教程的「承重墙」：前面两章的 Java 基础与进阶，都将在这里变成真正能跑起来的 Web 应用。我们以 SelfBlog 项目的真实后端代码（Spring Boot 3.3.13 + Java 21 + MyBatis Plus 3.5.8 + Spring Security + JWT）为主线，把 Spring Boot 的核心机制逐个讲透。

## 3.1 Spring 与 Spring Boot 概述

### 3.1.1 Spring 框架简介

Spring 是 Java 企业级开发的事实标准框架，由 Rod Johnson 于 2003 年创建。它最核心的贡献是两个思想：

- **IoC（Inversion of Control，控制反转）**：对象的创建和依赖组装交给容器管理，而不是对象自己 `new`
- **AOP（Aspect-Oriented Programming，面向切面编程）**：把日志、事务、性能统计这类「横切关注点」从业务代码中抽离出去

**Spring 框架的主要模块：**

| 模块 | 说明 |
|------|------|
| Spring Core | IoC 容器与依赖注入，一切的基石 |
| Spring AOP | 面向切面编程 |
| Spring MVC | Web 层框架，处理 HTTP 请求 |
| Spring JDBC / ORM | 数据库访问 |
| Spring Transaction | 声明式事务管理 |
| Spring Security | 认证与授权 |
| Spring Test | 测试支持 |

### 3.1.2 Spring Boot 解决了什么问题

传统 Spring 项目（SSM 时代）要写大量 XML 配置、手动管理几十个依赖的版本兼容。Spring Boot 的答案是四个词：

1. **自动配置（Auto-configuration）**：检测到 classpath 里有某个依赖，就自动帮你配好相应的 Bean
2. **起步依赖（Starter）**：`spring-boot-starter-web` 一个依赖把 Web 开发需要的十几个 jar 一次带齐，版本兼容由 Spring 官方保证
3. **内嵌服务器**：内置 Tomcat，应用打成 jar 直接 `java -jar` 运行，不用部署 WAR 到外部 Tomcat
4. **生产就绪**：Actuator 提供健康检查、指标监控等端点

### 3.1.3 版本约定

- Spring Boot 2.x：基于 Spring Framework 5、Java 8+
- Spring Boot 3.x：2022 年发布，基于 Spring Framework 6，**要求 Java 17+**，全面迁移到 Jakarta EE 9+（包名从 `javax.*` 改为 `jakarta.*`）

SelfBlog 使用 **Spring Boot 3.3.13 + Java 21**，所以代码里看到的是 `jakarta.servlet.*` 而不是 `javax.servlet.*`。这是很多老教程会把初学者带偏的第一个坑。

### 3.1.4 启动类与 @SpringBootApplication

每个 Spring Boot 应用都有一个启动类：

```java
@SpringBootApplication
public class SelfBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(SelfBlogApplication.class, args);
    }
}
```

`@SpringBootApplication` 是一个组合注解，等于三个注解之和：

| 组成注解 | 作用 |
|----------|------|
| `@SpringBootConfiguration` | 本质是 `@Configuration`，声明这是一个配置类 |
| `@EnableAutoConfiguration` | 开启自动配置魔法 |
| `@ComponentScan` | 扫描启动类所在包及其子包下的所有组件 |

**注意最后一行**：`@ComponentScan` 默认只扫描启动类所在包及子包。SelfBlog 的启动类在 `com.selfblog` 包下，所以所有代码都必须放在 `com.selfblog` 及其子包里，放到外面就不受容器管理了——这是初学者常见的「我的 Bean 为 null」问题的根源。

### 3.1.5 自动配置的原理（一分钟版本）

自动配置靠的是**条件注解**：Spring Boot 在启动时会读取 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件里登记的一长串自动配置类，然后逐个用条件注解判断是否生效：

```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)          // classpath 里有 DataSource 才生效
@ConditionalOnMissingBean(DataSource.class)    // 你自己没配 DataSource 才生效
public class DataSourceAutoConfiguration {
    // 自动创建数据源
}
```

理解这一条，你就理解了 Spring Boot 的全部「魔法」：**约定优于配置，但你随时可以覆盖约定**——自己写一个同类型 Bean，自动配置就退位。

### 3.1.6 启动时到底发生了什么：SelfBlog 启动流程走读

`SpringApplication.run()` 这一行背后是一条固定的流水线。以 SelfBlog 为例，把启动日志与源码对上号：

```
1. 解析 .env                  → DotenvConfig 把 JWT_SECRET 等读成 JVM property
                                 （Properties 格式，值里不能带引号）
2. 判定应用类型               → Servlet Web 应用，准备 DispatcherServlet
3. 加载 application.yml       → 数据源、MyBatis Plus、app.single-author-mode 等就位
4. @ComponentScan 扫描        → 从 SelfBlogApplication 所在包向下，
                                 收集 @Service/@Component/@Configuration 全家
5. 加载自动配置               → 读 AutoConfiguration.imports 清单，
                                 按 @ConditionalOnClass 等条件筛选（3.1.5）
6. 条件装配数据源             → DatabaseConfig 的 @ConditionalOnProperty("DB_TYPE")
                                 决定 SQLite / H2 / MySQL 三选一
7. 执行 schema 初始化         → SQLite 跑 schema-sqlite.sql（IF NOT EXISTS 幂等）
8. Bean 生命周期              → 按依赖序实例化：Mapper → Service（三分模型）
                                 → Controller → Filter（JwtAuthenticationFilter）
9. @PostConstruct 回调        → 各配置类的初始化逻辑
10. Web 服务器启动            → Tomcat 监听 8825，/api 前缀规则挂载
11. 打印 Started SelfBlogApplication
```

**两个值得记住的验证手段**：

- **看启动报告**：加 `--debug` 参数（或 `debug: true` 配置）启动，控制台会输出 **CONDITIONS EVALUATION REPORT**——每个自动配置类「匹配了/没匹配、为什么」一目了然，是排查「某个 Starter 没生效」的第一工具（附录 D.5 有实战）。
- **看 Bean 全家福**：`application.yml` 加 `logging.level.org.springframework.beans=INFO` 并在启动类加一句 `(context.getBeanDefinitionNames())`，能看到容器里全部 Bean 名单——「我的类到底有没有被扫到」一查便知。

**启动顺序与依赖的关系**：Spring 按依赖图拓扑排序实例化 Bean，所以「Controller 拿到的一定是完整装配好的 Service」。这也解释了构造器循环依赖为什么必炸（3.3.3）——拓扑序里两者谁先谁后都缺对方，无解。

## 3.2 IoC 容器与依赖注入

### 3.2.1 传统方式 vs IoC

```java
// 传统方式：对象自己创建依赖，耦合死了
public class BlogPostService {
    private BlogPostMapper mapper = new BlogPostMapperImpl();  // 写死实现
}

// IoC 方式：依赖由容器注入，随时可以换成 mock 实现做测试
public class BlogPostService {
    private final BlogPostMapper mapper;
    public BlogPostService(BlogPostMapper mapper) {  // 构造器注入
        this.mapper = mapper;
    }
}
```

好比「自己做饭（自己 new，想换食材得自己跑菜市场）」和「点外卖（容器配送，你只管声明要什么）」的区别。

### 3.2.2 三种注入方式

**方式一：构造器注入（推荐）**

```java
@Service
public class BlogPostServiceImpl implements BlogPostService {
    private final PostQueryService queryService;
    private final PostInteractionService interactionService;

    // Spring 4.3+：只有一个构造方法时，@Autowired 可以省略
    public BlogPostServiceImpl(PostQueryService queryService,
                               PostInteractionService interactionService) {
        this.queryService = queryService;
        this.interactionService = interactionService;
    }
}
```

构造器注入的优势：依赖不可变（final）、不会 null、依赖多了一看构造器就知道这个类职责过重——它是最好的「坏味道探测器」。

**方式二：字段注入（简单但不推荐做主力）**

```java
@Service
public class BlogPostServiceImpl {
    @Autowired
    private BlogPostMapper blogPostMapper;
}
```

写起来快，但脱离容器无法单测、字段不能 final、容易藏起过多依赖。小项目或教程代码里常见，生产代码建议少用。

**方式三：Setter 注入**

```java
@Service
public class BlogPostServiceImpl {
    private BlogPostMapper mapper;

    @Autowired
    public void setMapper(BlogPostMapper mapper) {
        this.mapper = mapper;
    }
}
```

适合「可选依赖」：依赖缺失时应用也能跑。

### 3.2.3 声明 Bean 的常用注解

| 注解 | 用途 |
|------|------|
| `@Component` | 通用组件 |
| `@Service` | 业务逻辑层（语义化，功能同 @Component） |
| `@Repository` | 数据访问层，额外会把数据库异常翻译为 DataAccessException |
| `@Controller` / `@RestController` | Web 层 |
| `@Configuration` | 配置类，里面的 `@Bean` 方法返回值注册为 Bean |
| `@Bean` | 方法级注解，用方法返回值作为 Bean（多用于第三方类，如 Caffeine CacheManager） |

### 3.2.4 同类型多个 Bean 的冲突与选择

当接口有多个实现类时，按类型注入会报 `NoUniqueBeanDefinitionException`，解决办法有三种：

```java
public interface NotificationSender {
    void send(String message);
}

@Service("emailSender")
public class EmailNotificationSender implements NotificationSender { ... }

@Service("smsSender")
public class SmsNotificationSender implements NotificationSender { ... }
```

```java
// 办法一：@Primary 设默认
@Primary
@Service("emailSender")
public class EmailNotificationSender implements NotificationSender { ... }

// 办法二：@Qualifier 按名字注入
@Service
public class OrderService {
    public OrderService(@Qualifier("smsSender") NotificationSender sender) { ... }
}

// 办法三：注入全部实现，自己按条件选（SelfBlog 按配置切换数据源的场景就是这么想的）
@Service
public class OrderService {
    private final Map<String, NotificationSender> senders;  // key 是 Bean 名字
    public OrderService(List<NotificationSender> all) {
        this.senders = all.stream().collect(
            Collectors.toMap(s -> s.getClass().getSimpleName(), Function.identity()));
    }
}
```

### 3.2.5 @Value 与 @ConfigurationProperties

```java
// 单个值注入
@Value("${jwt.secret}")
private String secret;

@Value("${jwt.expiration:86400000}")  // 冒号后是默认值
private Long expiration;
```

批量配置推荐用 `@ConfigurationProperties`，类型安全、IDE 可跳转：

```java
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    /** 是否启用限流 */
    private boolean enabled = true;
    /** 每分钟最大请求数 */
    private int limit = 100;
    /** 需要限流的路径前缀 */
    private List<String> pathPrefixes = new ArrayList<>();
}
```

```yaml
app:
  rate-limit:
    enabled: true
    limit: 100
    path-prefixes:
      - /api/auth
      - /api/comment
```

### 3.2.6 条件装配：以 SelfBlog 的 DatabaseConfig 为例

SelfBlog 用一个 `DB_TYPE` 环境变量切换 SQLite/H2/MySQL 三种数据库，靠的就是条件装配：

```java
@Configuration
@ConditionalOnProperty("DB_TYPE")   // 只有设置了 DB_TYPE 时这个配置类才生效
public class DatabaseConfig {
    // 根据 DB_TYPE 的值创建对应数据源，覆盖 Spring Boot 默认的自动配置
}
```

这套机制的含义是：**DB_TYPE 留空（本地测试）→ 走默认 H2 数据源；DB_TYPE=sqlite → 走 SQLite 配置；DB_TYPE=mysql → 走 MySQL 配置**。一份代码，三种环境，靠条件注解切换。

常用的条件注解一览：

| 注解 | 生效条件 |
|------|----------|
| `@ConditionalOnProperty(name="x", havingValue="y")` | 配置项 x 等于 y |
| `@ConditionalOnClass(X.class)` | classpath 存在某个类 |
| `@ConditionalOnMissingBean(X.class)` | 容器里还没有这个 Bean |
| `@Profile("prod")` | 当前激活的 profile 是 prod |
| `@ConditionalOnWebApplication` | 是 Web 应用 |

## 3.3 Bean 的作用域与生命周期

### 3.3.1 作用域

| 作用域 | 说明 |
|--------|------|
| singleton | **默认**。整个容器一个实例。无状态的服务类用它最合适 |
| prototype | 每次注入/获取都创建新实例 |
| request | 每个 HTTP 请求一个实例（Web 环境） |
| session | 每个 Session 一个实例（Web 环境） |

```java
@Component
@Scope("prototype")
public class ExportTask { ... }
```

**为什么 SelfBlog 的 Service 全是单例也没问题？** 因为它们都是「无状态」的——不把请求数据存在成员变量里，所有请求数据都通过方法参数传递。反过来，如果你在一个单例 Service 的成员变量里存了当前登录用户，并发下数据就会串掉。这是初学者最容易踩的并发坑。

### 3.3.2 生命周期回调

```java
@Component
public class CacheWarmer {

    @PostConstruct              // 属性注入完成后执行，最常用
    public void init() {
        log.info("预热热点标签缓存");
    }

    @PreDestroy                 // 容器关闭前执行
    public void cleanup() {
        log.info("释放资源");
    }
}
```

完整的生命周期顺序：实例化 → 属性注入 → Aware 回调（BeanNameAware 等）→ `@PostConstruct` → InitializingBean.afterPropertiesSet → 初始化方法 → **使用** → `@PreDestroy` → DisposableBean.destroy → 销毁方法。

### 3.3.3 循环依赖

如果一个 Bean 的 `@PostConstruct` 里需要用到另一个 Bean，直接注入即可，Spring 会自动先创建被依赖的 Bean。但**循环依赖**（A 依赖 B、B 又依赖 A）时：字段注入 Spring 能用三级缓存化解，构造器注入则会直接报错 `BeanCurrentlyInCreationException`。遇到循环依赖，正确做法不是硬解，而是重构——把公共依赖抽成第三个类。

## 3.4 AOP 面向切面编程

### 3.4.1 为什么需要 AOP

假设每个 Controller 方法都要记录执行耗时，不用 AOP 时你必须在每个方法里写：

```java
long start = System.currentTimeMillis();
try {
    // 业务代码
} finally {
    log.info("耗时 {}ms", System.currentTimeMillis() - start);
}
```

几十个方法写几十遍，一旦要改日志格式就是全量修改。AOP 的思路：把这段「通用逻辑」抽成一个切面，用表达式告诉它「管住哪些方法」，业务代码一行都不用动。

### 3.4.2 核心概念速查

| 概念 | 英文 | 通俗解释 |
|------|------|----------|
| 切面 | Aspect | 通用逻辑的载体类（@Aspect） |
| 连接点 | Join Point | 程序执行中可插入的点（Spring AOP 里就是方法调用） |
| 切点 | Pointcut | 用表达式筛选「哪些方法要被拦截」 |
| 通知 | Advice | 拦截后执行的动作（前/后/环绕/返回/异常） |
| 织入 | Weaving | 把切面应用到目标对象的过程（Spring 在运行期用动态代理完成） |

### 3.4.3 五种通知

```java
@Aspect
@Component
public class LoggingAspect {

    // 前置通知：方法执行前
    @Before("execution(* com.selfblog.service..*.*(..))")
    public void before(JoinPoint jp) {
        log.info("调用 {}，参数 {}", jp.getSignature().toShortString(), jp.getArgs());
    }

    // 返回通知：正常返回后
    @AfterReturning(pointcut = "execution(* com.selfblog.service..*.*(..))",
                    returning = "result")
    public void afterReturning(JoinPoint jp, Object result) { ... }

    // 异常通知：抛异常后
    @AfterThrowing(pointcut = "execution(* com.selfblog.service..*.*(..))",
                   throwing = "ex")
    public void afterThrowing(JoinPoint jp, Exception ex) { ... }

    // 后置通知：无论是否异常都执行（类似 finally）
    @After("execution(* com.selfblog.service..*.*(..))")
    public void after(JoinPoint jp) { ... }

    // 环绕通知：功能最强，能控制是否执行原方法、能改参数、能改返回值
    @Around("execution(* com.selfblog.service..*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();          // 放行，执行原方法
        } finally {
            log.info("耗时 {}ms", System.currentTimeMillis() - start);
        }
    }
}
```

### 3.4.4 切点表达式速查

```java
execution(* com.selfblog.service..*.*(..))
//         ↑    ↑              ↑ ↑
//       任意返回值  service 及子包 任意类 任意方法 任意参数

// 常见写法：
execution(* com.selfblog.controller..*(..))      // 所有控制器方法
execution(* com.selfblog.service.BlogPostService.get*(..))  // 特定类的 get 开头方法
@annotation(com.selfblog.annotation.PerformanceLog)  // 被指定注解标记的方法
within(com.selfblog.service..*)                   // 指定包下的所有连接点
```

`..` 出现在包名中间表示「任意层级的子包」，出现在参数位置表示「任意个数的参数」。

### 3.4.5 实战：SelfBlog 的性能监控切面

SelfBlog 定义了一个自定义注解 `@PerformanceLog`，标记在需要监控的方法上：

```java
// annotation/PerformanceLog.java —— 自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceLog {
}
```

```java
// aspect/PerformanceMonitoringAspect.java —— 切面实现
@Aspect
@Component
public class PerformanceMonitoringAspect {

    @Around("@annotation(com.selfblog.annotation.PerformanceLog)")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            String name = pjp.getSignature().toShortString();
            if (cost > 1000) {
                log.warn("[PERF] {} 执行耗时 {}ms，超过阈值", name, cost);
            } else {
                log.info("[PERF] {} 执行耗时 {}ms", name, cost);
            }
            return result;
        } catch (Throwable e) {
            log.error("[PERF] {} 执行异常，耗时 {}ms",
                pjp.getSignature().toShortString(),
                System.currentTimeMillis() - start, e);
            throw e;    // 异常必须重新抛出，否则调用方以为成功了
        }
    }
}
```

**三个要点：**

1. 环绕通知里**必须调用 `pjp.proceed()`**，否则原方法根本不会执行
2. catch 之后**必须重新抛出异常**，吞掉异常会让调用方误判
3. SelfBlog 的测试日志里大量 `[PERF] ... error:` 是这个切面记录的**正常负例断言**，不是测试失败——第一次跑测试别被吓到

### 3.4.6 AOP 失效的场景（重要！）

Spring AOP 基于动态代理：调用方拿着的是代理对象，代理对象在真正调用目标方法前后插入逻辑。因此：

```java
@Service
public class OrderService {
    @Transactional
    public void create() { ... }

    @Transactional
    public void batch() {
        this.create();   // ⚠️ 内部调用！this 是原始对象不是代理对象，事务/切面失效！
    }
}
```

**绕过办法**：注入自己（`@Lazy private OrderService self;` 后调用 `self.create()`），或者把 `create()` 挪到另一个类。记住结论：**同一个类内部方法互调，切面和事务注解都不生效**。

## 3.5 配置体系

### 3.5.1 application.yml

```yaml
server:
  port: 8825

spring:
  application:
    name: selfblog
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true     # 数据库下划线列名映射 Java 驼峰属性
  global-config:
    db-config:
      logic-delete-field: isDeleted        # 全局逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0
```

**优先级**（高 → 低）：命令行参数 → JVM 系统属性 → 环境变量 → application-{profile}.yml → application.yml。SelfBlog 的 `DotenvConfig` 会把 `.env` 文件内容加载为 JVM 属性，因此 `.env` 里的值优先级高于系统环境变量——这是排配置问题时的重要知识。

### 3.5.2 多环境 Profile

```yaml
# application.yml 公共配置
spring:
  profiles:
    active: dev    # 默认激活 dev
```

```yaml
# application-dev.yml 开发环境
spring:
  datasource:
    driver-class-name: org.sqlite.JDBC
    url: jdbc:sqlite:campus_blog.db
```

```yaml
# application-prod.yml 生产环境
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/selfblog
```

启动时用 `--spring.profiles.active=prod` 切换。`${MYSQL_HOST:localhost}` 语法表示读环境变量，没有则用默认值。

### 3.5.3 .env 文件：SelfBlog 的敏感配置方案

数据库密码、JWT 密钥绝不能提交进 Git。SelfBlog 的做法：

1. 仓库里放 `.env.example`（无真实值，可提交）
2. 本地复制为 `.env`（填真实值，被 .gitignore 排除）
3. 启动时 `DotenvConfig` 用 **Java Properties 格式**解析 `.env`，查找顺序为工作目录 → 用户主目录

```properties
JWT_SECRET=this-is-a-very-long-secret-key-at-least-32-chars
ADMIN_PASSWORD=change-me-to-a-strong-password
```

**注意**：SelfBlog 的 `.env` 是 Java Properties 格式，**不要加引号**！写成 `JWT_SECRET="abc"` 会把引号本身当值的一部分，导致 JWT 校验失败这种诡异 bug。另外文件值会成为 JVM property，优先级高于系统环境变量。

## 3.6 Web 层：Spring MVC

### 3.6.1 请求处理流程

一次 HTTP 请求进入 Spring Boot 后的旅程：

```
浏览器 → 内嵌 Tomcat → DispatcherServlet（前端控制器，统一入口）
       → HandlerMapping（根据 URL 找到对应的 Controller 方法）
       → 拦截器链（preHandle）
       → 参数解析 + Controller 方法执行
       → 返回值处理（@RestController → JSON 序列化）
       → 拦截器链（afterCompletion）
       → 响应
```

### 3.6.2 RESTful 风格控制器

```java
@RestController                 // = @Controller + @ResponseBody，返回值直接序列化为 JSON
@RequestMapping("/posts")       // 类级前缀（运行时自动补 /api，见 3.6.3）
public class BlogPostController {

    private final BlogPostService postService;

    /** GET /api/posts?page=1&size=10 —— 分页列表 */
    @GetMapping
    public Result<IPage<PostVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(postService.listPosts(page, size));
    }

    /** GET /api/posts/123 —— 详情 */
    @GetMapping("/{id}")
    public Result<PostVO> detail(@PathVariable Long id) {
        return Result.success(postService.getPostById(id));
    }

    /** POST /api/posts —— 创建 */
    @PostMapping
    public Result<PostVO> create(@RequestBody @Valid PostCreateRequest request) {
        return Result.success(postService.createPost(request));
    }

    /** PUT /api/posts/123 —— 更新 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid PostUpdateRequest request) {
        postService.updatePost(id, request);
        return Result.success();
    }

    /** DELETE /api/posts/123 —— 删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success();
    }
}
```

| 注解 | 从哪取值 |
|------|----------|
| `@PathVariable` | URL 路径变量 `/posts/{id}` |
| `@RequestParam` | 查询参数 `?page=1` |
| `@RequestBody` | 请求体 JSON → 对象（POST/PUT） |
| `@RequestHeader` | 请求头 |
| `@CookieValue` | Cookie |

### 3.6.3 /api 前缀：自动加路径前缀

SelfBlog 有一个非常优雅的设计：**源码里的 Controller 不写 `/api` 前缀，运行时由配置统一加上**：

```java
// config/WebMvcConfig.java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
            "/api",
            HandlerTypePredicate.forAnnotation(RestController.class)  // 只对 @RestController 生效
        );
    }
}
```

好处：写 Controller 时少打字、前端代理统一转发 `/api`、以后要改前缀只改一处。

**连带规则**：写 `@WebMvcTest` 切片测试时，MockMvc 请求的路径**不带** `/api` 前缀；而通过完整 HTTP 栈（真启动应用）测试时要带。`SeoController` 刻意用 `@Controller` 而非 `@RestController`，就是为了逃过这个前缀，在根路径提供 `/feed.xml`、`/sitemap.xml`、`/robots.txt`。

### 3.6.4 统一响应 Result&lt;T&gt;

前后端分离项目必须约定统一的响应格式，SelfBlog 的信封：

```java
@Data
public class Result<T> {
    private Integer code;       // 业务状态码：200 成功，其他见异常约定
    private String message;     // 提示信息
    private T data;             // 业务数据
    private Long timestamp;     // 时间戳

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    public static <T> Result<T> success() { return success(null); }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        return r;
    }
}
```

分页数据放在 data 里，结构固定为 `records/total/size/current/pages`（records 是当前页数据列表，total 是总条数），前端所有分页组件都按这个契约解析。

**业务码 vs HTTP 状态码**：HTTP 200 只代表「请求送达且服务端处理完成」，业务成败看 `code` 字段。但 SelfBlog 的 `GlobalExceptionHandler` 会把业务异常映射到对应的 HTTP 状态（401/403/404/429/500），两边都不含糊。

### 3.6.5 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：可预期，warn 级别即可，把业务码透传给前端 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        HttpStatus http = switch (e.getCode()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(http)
            .body(Result.error(e.getCode(), e.getMessage()));
    }

    /** 参数校验失败：@Valid 抛出的 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + " " + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Result.error(400, msg));
    }

    /** 兜底：未预期异常，不暴露堆栈给前端（安全！），只返回 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("系统异常", e);   // 完整堆栈只进服务端日志
        return ResponseEntity.internalServerError()
            .body(Result.error(500, "服务器开小差了，请稍后再试"));
    }
}
```

配套的业务异常：

```java
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}

// 使用：抛出即返回，不需要层层 if-else 向上传递错误
if (post == null) {
    throw new BusinessException(404, "文章不存在");
}
```

设计要点：`BusinessException` 继承 `RuntimeException`（非受检异常），业务代码里到处抛也不用到处声明 throws，代码保持干净；错误信息在异常产生的**最近位置**组装，那里才掌握完整的上下文。

### 3.6.6 拦截器 vs 过滤器

| 维度 | Filter 过滤器 | HandlerInterceptor 拦截器 |
|------|--------------|--------------------------|
| 规范 | Servlet 规范 | Spring MVC 提供 |
| 执行时机 | DispatcherServlet **之前** | HandlerMapping 之后、Controller 前后 |
| 能否拿到 Handler 信息 | 不能 | 能（知道要执行哪个 Controller 方法） |
| 典型用途 | 编码、认证（JWT） | 限流、日志、权限 |

SelfBlog 的选择：**JWT 认证用 Filter**（Spring Security 体系内），**限流用 Interceptor**（需要按路径前缀细粒度配置）。

### 3.6.7 文件上传

```java
@PostMapping("/upload")
public Result<String> upload(@RequestParam MultipartFile file) {
    if (file.isEmpty()) {
        throw new BusinessException(400, "请选择文件");
    }
    // 白名单校验扩展名，绝不能信任前端传来的文件名
    String original = file.getOriginalFilename();
    String ext = StringUtils.getFilenameExtension(original);
    if (!Set.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) {
        throw new BusinessException(400, "不支持的文件类型");
    }
    // 用 UUID 重命名，防止路径穿越与重名
    String savedName = UUID.randomUUID() + "." + ext;
    Path target = uploadRoot.resolve(savedName);
    file.transferTo(target);
    return Result.success("/uploads/" + savedName);
}
```

上传大小限制在 `application.yml` 的 `spring.servlet.multipart` 中配置。**安全三原则**：校验类型、重命名、限制大小。

## 3.7 参数校验

### 3.7.1 Bean Validation

引入 `spring-boot-starter-validation` 后，在 DTO 上声明约束注解：

```java
@Data
public class PostCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 200, message = "摘要最长 200 字")
    private String summary;

    @Pattern(regexp = "^(https?://)?.*\\.(jpg|png|webp)$",
             message = "封面必须是图片地址")
    private String coverImage;

    @NotNull(message = "必须指定分类")
    private Long categoryId;
}
```

在 Controller 入口打开校验开关：

```java
@PostMapping
public Result<PostVO> create(@RequestBody @Valid PostCreateRequest request) {
    ...
}
```

`@Valid` 校验失败会抛 `MethodArgumentNotValidException`，由全局异常处理器统一接住转成 400。**Controller 里的业务代码从此不用再写「if (title == null) return 错误」这种样板**。

### 3.7.2 @Valid 与 @Validated 的区别

- `@Valid`（JSR 标准）：支持嵌套校验（对象里的对象也会校验），但**不支持分组**
- `@Validated`（Spring 扩展）：支持分组校验，可以用在类上配合方法级校验

```java
// 嵌套校验：列表里的每个元素也会被校验
public class PostUpdateRequest {
    @Valid
    @NotEmpty
    private List<@NotNull TagRef> tags;
}
```

**原则**：格式类校验（非空、长度、正则）放 Bean Validation；业务类校验（分类是否存在、是否重复）放 Service 层。别把数据库查询塞进校验注解里。

## 3.8 数据访问：MyBatis Plus

### 3.8.1 从 MyBatis 到 MyBatis Plus

MyBatis 需要手写每一条 SQL，灵活但重复劳动多。MyBatis Plus（MP）的口号是「只做增强不做改变」：单表 CRUD 一行 SQL 都不用写，复杂查询仍然回到 MyBatis 的 XML 里写。

### 3.8.2 实体类与主键策略

```java
@Data
@TableName("blog_post")
public class BlogPost {

    @TableId(type = IdType.ASSIGN_ID)   // 雪花 ID：分布式安全的趋势递增长整型
    private Long id;

    private String title;
    private String content;
    private Long authorId;
    private Integer status;              // 1 已发布
    private Integer viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic                          // 逻辑删除标记
    private Integer isDeleted;
}
```

主键策略对比：`AUTO`（数据库自增，分库分表会冲突）、`ASSIGN_ID`（MP 雪花算法，默认）、`INPUT`（手动指定）、`UUID`（字符串，索引性能差）。单机博客用 AUTO 也完全没问题，SelfBlog 的表两者都有。

### 3.8.3 Mapper：继承 BaseMapper 白拿 17 个方法

```java
public interface BlogPostMapper extends BaseMapper<BlogPost> {

    // 复杂查询才写自定义方法 + XML
    IPage<PostListVO> selectPostPage(IPage<PostListVO> page,
                                     @Param("categoryId") Long categoryId,
                                     @Param("keyword") String keyword);
}
```

`insert / deleteById / updateById / selectById / selectList / selectPage ...` 全部自动拥有。**重要约定**：BaseMapper 的方法会自动拼接 `is_deleted = 0`；但**手写 SQL（XML 里的）必须自己带上 `is_deleted = 0`**，否则会查出已删除数据——这是逻辑删除机制的经典坑。

### 3.8.4 分页插件

```java
@Configuration
@MapperScan("com.selfblog.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

```java
Page<BlogPost> page = new Page<>(pageNo, pageSize);  // 第几页、每页几条
IPage<BlogPost> result = blogPostMapper.selectPage(page, wrapper);

result.getRecords();  // 当前页数据
result.getTotal();    // 总条数（插件自动执行 count 语句）
result.getPages();    // 总页数
```

分页插件会自动把你的 SQL 改写成 count 语句 + LIMIT 语句，前端拿到的 total 永远是准确值。

### 3.8.5 条件构造器

```java
// QueryWrapper：字段名用字符串（重构不安全）
QueryWrapper<BlogPost> qw = new QueryWrapper<>();
qw.eq("status", 1)
  .like("title", keyword)
  .ge("create_time", startTime)
  .orderByDesc("create_time");

// LambdaQueryWrapper：方法引用，编译期检查（推荐）
LambdaQueryWrapper<BlogPost> lw = new LambdaQueryWrapper<>();
lw.eq(BlogPost::getStatus, 1)
  .like(StringUtils.hasText(keyword), BlogPost::getTitle, keyword)  // 第一个参数是 boolean：为 false 时这个条件根本不拼进 SQL
  .orderByDesc(BlogPost::getCreateTime);

List<BlogPost> posts = blogPostMapper.selectList(lw);
```

`like(condition, column, value)` 的第一个 boolean 参数是「条件开关」，实现动态查询不用写 if-else 拼装，非常顺手。

### 3.8.6 字段自动填充

创建时间、更新时间这类字段每次都要手动 set 太啰嗦，用 `MetaObjectHandler` 自动填：

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 3.8.7 多数据库方言：databaseId

SelfBlog 同时支持 SQLite（默认）、H2（测试）、MySQL（生产）。有些 SQL 语法各库不同，MP 通过 `VendorDatabaseIdProvider` 给每条语句标记方言：

```xml
<select id="selectHotPosts" resultType="PostVO" databaseId="mysql">
    SELECT * FROM blog_post WHERE is_deleted = 0
    ORDER BY view_count DESC LIMIT #{n}
</select>

<select id="selectHotPosts" resultType="PostVO" databaseId="sqlite">
    SELECT * FROM blog_post WHERE is_deleted = 0
    ORDER BY view_count DESC LIMIT #{n}
</select>
```

**关键认知**：H2 没有配置 databaseId 映射，只会走**没有 databaseId 属性的兜底语句**。所以不要把 MySQL 专有 SQL 写进共享语句，否则测试环境（H2）直接报语法错。

**三种数据库的初始化策略**也不同：

| 数据库 | 初始化方式 | 注意 |
|--------|-----------|------|
| SQLite（默认） | `schema-sqlite.sql`，`CREATE TABLE IF NOT EXISTS` | 只建表不迁移既有文件 |
| H2（测试） | `src/test/resources/schema.sql` | 每次测试全新内存库 |
| MySQL（生产） | `schema-fresh-install.sql` 只用于**全新空库**；已有库只能跑 `migration-selfblog.sql` | fresh 版含 `DROP TABLE`，改名就是为了降低误执行风险 |

## 3.9 事务管理

### 3.9.1 @Transactional 基础

```java
@Service
public class PostInteractionServiceImpl implements PostInteractionService {

    /** 计数与状态副作用：一个方法里改多张表，必须原子完成 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long postId, Long userId) {
        blogLikeMapper.insert(new BlogLike(postId, userId));
        blogPostMapper.incrementLikeCount(postId);   // 两步要么都成功，要么都回滚
    }
}
```

**三个必懂参数：**

| 参数 | 默认 | 建议 |
|------|------|------|
| `rollbackFor` | 只回滚 RuntimeException 和 Error | **显式写 `rollbackFor = Exception.class`**，否则捕获了受检异常也不回滚 |
| `readOnly` | false | 查询方法写 `@Transactional(readOnly = true)`，驱动可以优化、语义也清晰 |
| `propagation` | REQUIRED | 见下表 |

### 3.9.2 传播行为

| 传播行为 | 含义 |
|----------|------|
| REQUIRED（默认） | 有事务就加入，没有就新建 |
| REQUIRES_NEW | 挂起当前事务，新建独立事务（如「记录操作日志，业务回滚了日志也要保留」） |
| NESTED | 嵌套事务，外层回滚则内层也回滚，内层回滚不影响外层 |
| SUPPORTS / NOT_SUPPORTED / MANDATORY / NEVER | 特殊场景使用 |

### 3.9.3 事务失效的六大场景（面试高频，实战踩坑）

1. **同类内部调用**：`this.methodB()` 不走代理，@Transactional 失效（见 3.4.6）
2. **方法不是 public**：Spring 事务代理只拦 public 方法
3. **异常被 try-catch 吞掉**：Spring 看不到异常就不回滚
4. **rollbackFor 未覆盖受检异常**：默认不回滚 IOException 这类异常
5. **数据库引擎不支持事务**：MySQL 的 MyISAM 引擎不支持事务
6. **类没被 Spring 管理**：自己 `new` 出来的对象上标注解毫无意义

### 3.9.4 SelfBlog 的「门面 / 查询 / 交互」三分与事务注解

SelfBlog 的 Post 与 Circle 服务都按职责拆成三份，**每个实现类都继承同一个实体的 `ServiceImpl<XxxMapper, Entity>`**：

| 角色 | 类 | 职责 | 事务注解 |
|------|-----|------|----------|
| 门面 Facade | `BlogPostService(Impl)` | CRUD、归属校验、跨服务编排；把查询/交互方法**一行委托**出去并重新标注事务 | `@Transactional` |
| 查询 Query | `PostQueryService(Impl)` | 列表/搜索/详情等只读逻辑，`@Cacheable` 也在这里 | `@Transactional(readOnly = true)` |
| 交互 Interaction | `PostInteractionService(Impl)` | views/like/comment/collect 计数副作用 | `@Transactional(rollbackFor = Exception.class)` |

```java
// 门面里的一行委托：逻辑不复制，事务重新标注
@Override
@Transactional(rollbackFor = Exception.class)
public void likePost(Long postId) {
    postInteractionService.likePost(postId, SecurityUtils.getCurrentUserId());
}
```

为什么这么拆？一个实体对应一个上帝 Service 是所有中型项目的通病：几千行、改一处怕碰坏全身。三分之后每个类职责单一、事务策略清晰，**新增功能要放对位置，不要在门面里复制逻辑**。这也顺带解决了内部调用失效问题——门面调交互是跨类调用，天然走代理。

#### 事务边界设计三问（写每个 Service 方法前过一遍）

1. **这个方法该有事务吗**？只读查询标注 `@Transactional(readOnly = true)`——不只是省写锁的声明，Hibernate 层还会关脏检查、驱动层可能做读优化（SelfBlog 的 QueryService 全部遵守）。写操作才用 `rollbackFor = Exception.class` 的读写事务。
2. **边界画在哪**？一条铁律：**事务从第一个写操作开始，到最后一个写操作结束**。把 RPC 调用、文件 IO、消息发送圈进事务，等于让数据库连接在慢 IO 期间白白挂起（连接池 1 的 SQLite 场景下更是直接阻塞全部请求）。SelfBlog 的做法是 IO 出事务、走事件（§3.14.3）。
3. **失败半径多大**？toggle 点赞里「插记录」与「改计数」要么都成要么都不成——同一事务；「发通知」失败不该回滚点赞——出事务（AFTER_COMMIT 监听）。**把「必须原子」的留在事务里，把「尽力而为」的挪出去**，这句话就是事务设计的全部。

## 3.10 Spring Security 与 JWT 认证

### 3.10.1 认证与授权

- **认证（Authentication）**：你是谁？——登录
- **授权（Authorization）**：你能干什么？——权限

SelfBlog 的模型：访客只读（无需登录），管理员登录后可写。授权角色只有一个 `ROLE_admin`。

### 3.10.2 JWT 原理

JWT（JSON Web Token）是一串自包含的签名字符串，三段式结构 `Header.Payload.Signature`：

```
eyJhbGciOiJIUzI1NiJ9          ← Header：算法（Base64URL）
.eyJzdWIiOiIxIiwiZXhwIjoxNzF9 ← Payload：用户ID、过期时间等声明
.SflKxwRJSMeKKF2QT4fwpMeJf36  ← Signature：用密钥对前两段的 HMAC 签名
```

**无状态认证**的好处：服务端不存 Session，任何一台服务器都能独立验签，天然适合水平扩展。代价：**签发后无法主动作废**（改密码要重签、黑名单要自己实现）。Payload 只是 Base64 编码**不是加密**，别把敏感信息放进去。

SelfBlog 用 **jjwt 0.12.6**（新版 API 与网上老教程的 0.9.x 写法完全不同，注意区分）：

```java
@Component
public class JwtUtils {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long expirationMs) {
        // 密钥必须 ≥ 32 字节，HS256 才安全
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** 签发：jjwt 0.12 链式 API */
    public String generateToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(userId))            // 主体：用户ID
            .claim("username", username)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationMs)))
            .signWith(key)                              // 自动根据 key 长度选算法
            .compact();
    }

    /** 解析：验签 + 过期检查一步完成，失败抛异常 */
    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

前端配套：Vue 端的 `stores/user.ts` 把 token 经 `utils/crypto` 编解码后存 localStorage，并在请求拦截器里注入 `Authorization: Bearer <token>` 头（详见第七章全链路剖析）。

### 3.10.3 JWT 认证过滤器

Spring Security 的过滤器链里，我们在 `UsernamePasswordAuthenticationFilter` 之前插入自己的 JWT 过滤器：

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        // 无 token 或格式不对 → 直接放行，交给后续授权规则决定 401/403
        // （公开接口本来就无需登录，过滤器只负责「有 token 就解析」）
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtUtils.parseToken(token);   // 验签 + 过期检查
            Long userId = Long.valueOf(claims.getSubject());

            // 加载用户权限并构建认证对象
            UserDetails userDetails =
                userDetailsService.loadUserByUsername(claims.get("username", String.class));
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException e) {
            // token 无效/过期：不抛 500，清空上下文让请求以匿名身份继续
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }
}
```

**设计精髓**：过滤器只负责「解析并设置认证信息」，**不负责拒绝请求**。拒绝是授权规则（`authorizeHttpRequests`）的职责——公开接口带不带 token 都能访问，就是靠这种职责分离实现的。

### 3.10.4 安全配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // 开启 @PreAuthorize 方法级注解
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())     // 前后端分离 + 无 Cookie 会话，CSRF 风险低
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("admin")
                .anyRequest().permitAll()      // 其余公开（访客只读博客）
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, resp, ex) ->   // 未认证 → 401 JSON
                    writeJson(resp, 401, "请先登录"))
                .accessDeniedHandler((req, resp, ex) ->        // 已认证但无权限 → 403 JSON
                    writeJson(resp, 403, "无权访问"))
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();    // 自带盐值，强度默认 10
    }
}
```

**新增公开接口的规则**：每个公开接口必须在 `SecurityConfig` 显式放行，这是 SelfBlog 的硬约定——「默认拒绝，显式放行」永远比「默认放行」安全。

### 3.10.5 取当前用户：SecurityUtils

Controller/Service 里频繁需要「当前登录用户」，SelfBlog 封装成工具类：

```java
public class SecurityUtils {

    /** 未登录直接抛 401 业务异常，用于「必须登录」的接口 */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException(401, "请先登录");
        }
        return extractUserId(auth);
    }

    /** 游客场景：未登录返回 null（如文章详情页判断是否作者本人） */
    public static Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (BusinessException e) {
            return null;
        }
    }
}
```

两个方法对应两种语义，**该用哪个想清楚**：管理接口用 `getCurrentUserId()`（没登录就该 401）；公开页面用 `getCurrentUserIdOrNull()`（游客看文章是正常业务流）。

### 3.10.6 单作者模式与登录防线

SelfBlog 从多用户社区改造为单人博客后，安全模型大幅简化但防线加固：

- 注册接口全部抛 `BusinessException(403, "此博客不开放访客注册")`
- `app.single-author-mode: true` 时，兜底规则要求所有写操作账号必须 `ROLE_admin`
- 登录时校验：非管理员账号即使密码正确也拒绝登录后台

这套「发布即上线」的模型让文章创建直接 `status = 1`，**没有**待审核闸门——个人博客的作者就是管理员，审核流程纯属多余。这是「按业务形态裁剪功能」的好例子：架构不是越复杂越专业，合适才专业。

## 3.11 缓存

### 3.11.1 缓存注解三件套

```java
// 查询缓存：先查缓存，未命中才执行方法，结果写入缓存
@Cacheable(value = "hotTagsCache", key = "'v1'")
public List<TagVO> getHotTags() { ... }

// 更新时清缓存：删除数据后，让下次查询重新加载
@CacheEvict(value = "categoryCache", allEntries = true)
public void updateCategory(Category c) { ... }

// 更新并刷新缓存：方法返回值覆盖缓存
@CachePut(value = "siteSettingsCache", key = "'all'")
public SiteSettings updateSettings(...) { ... }
```

### 3.11.2 Caffeine 与多缓存实例配置

Caffeine 是 Java 性能最好的本地缓存库（基于 W-TinyLFU 淘汰算法）。SelfBlog 用 `SimpleCacheManager` 注册 **8 个具名缓存，各自独立 TTL**：

```java
@Configuration
public class CaffeineCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
            cache("hotTagsCache",       10, TimeUnit.MINUTES),   // 热点标签
            cache("categoryCache",      30, TimeUnit.MINUTES),   // 分类（变化少）
            cache("userCache",          10, TimeUnit.MINUTES),   // 用户信息
            cache("trendingCache",       5, TimeUnit.MINUTES),   // 趋势（越热越要新）
            cache("statsCache",         15, TimeUnit.MINUTES),   // 统计
            cache("statusCache",         2, TimeUnit.MINUTES),   // 状态（容忍短暂旧值）
            cache("siteSettingsCache",  10, TimeUnit.MINUTES),   // 站点设置
            cache("visitorSummaryCache",        TimeUnit.SECONDS, 30)   // 访客摘要（高频更新）
        ));
        return manager;
    }

    private CaffeineCache cache(String name, long time, TimeUnit unit) {
        return new CaffeineCache(name, Caffeine.newBuilder()
            .expireAfterWrite(time, unit)
            .maximumSize(1_000)
            .build());
    }
}
```

**为什么要分 8 个？** 因为不同数据的「可容忍旧值时长」完全不同：站点分类一天不变可以缓存 30 分钟，访客统计 30 秒前的数据就没法看了。一刀切的 TTL 要么太短浪费缓存、要么太长展示错误数据。**TTL 的本质是「业务对数据新鲜度的容忍度」**。

### 3.11.3 缓存一致性的朴素策略

没有 Redis 发布订阅、没有 binlog 监听，SelfBlog 的一致性策略简单有效：

1. **短 TTL 兜底**：就算忘了清缓存，最多脏 TTL 那么久
2. **写操作主动 `@CacheEvict`**：更新分类就清 `categoryCache`
3. **测试 profile 关闭缓存**：保证单测断言的是真实数据

### 3.11.4 缓存三大经典问题：穿透、击穿、雪崩

面试高频，更是生产事故三兄弟。用 SelfBlog 的场景逐个讲透：

**① 缓存穿透：查「根本不存在的数据」，缓存永远挡不住**

攻击者拿不存在的文章 id=99999999 疯狂请求。流程：缓存没有 → 查库没有 → **不缓存空值** → 下一次照样查库。每次请求都穿透到数据库。

```
请求 → 缓存未命中 → 数据库也没有 → 不写入缓存 → 下次还是全链路查
```

解法（按成本从低到高）：

```java
// 解法一：缓存空值（最常用）——把「数据库里没有」也变成可缓存的事实
PostVO vo = postMapper.selectById(id);
if (vo == null) {
    cache.put(key, PostVO.EMPTY, Duration.ofMinutes(2)); // 空值缓存短 TTL
    throw new BusinessException(404, "文章不存在");
}
// SelfBlog 的实现路径：ReadingPageService 对缺失文章返回真实 404 页，
// 且 SeoController 对缺失路径也回 404（而非空壳 200），从根上减少「查询不存在资源」的动机

// 解法二：参数合法性前置拦截——id 必须是正整数，负数/超长直接 400
if (id == null || id <= 0 || id > 10_000_000_000L) {
    throw new BusinessException(400, "参数非法");   // 根本不查库
}

// 解法三：布隆过滤器（海量数据才值得）——启动时把全部文章 id 喂进过滤器，
// 「过滤器说不存在」100% 不存在，直接拒绝；「说存在」才查库（有极小概率误判）
```

**② 缓存击穿：一个「热点 key」过期瞬间，并发请求全部打到数据库**

爆款文章的缓存恰好在整点过期，同一瞬间 500 个并发请求同时发现「缓存没有」→ 500 个请求同时查库。单机 SQLite（池大小 1）当场排队卡死。

```
T0: 缓存过期
T1: 请求A 缓存未命中 → 查库
T2: 请求B 缓存未命中 → 查库   ← 同一个 key，重复查
T3: 请求C 缓存未命中 → 查库   ← 雪崩式重复查询
```

解法：

```java
// 解法一：互斥锁——同一 key 只放一个请求去查库，其他等它写回缓存
// SelfBlog 对应的真实实现是 AnonymousLikeServiceImpl 的 synchronized：
// 单机 + 池大小 1 的诚实方案（第三章 7.5 有完整剖析）
synchronized (key.intern()) {          // 注意 intern() 是简化写法，生产建议 Guava Striped
    // double-check：拿到锁后再查一次缓存，可能前一个请求已经写回
    PostVO cached = cache.getIfPresent(key);
    if (cached != null) return cached;
    // 查库 + 写缓存
}

// 解法二：逻辑过期——物理上永不过期，值里带过期时间，
// 发现逻辑过期后异步刷新，期间返回旧值（牺牲实时性，换取零击穿）

// 解法三：热点 key 永不过期 + 主动失效（更新时 @CacheEvict）
// SelfBlog 的 hotTagsCache(10m) 走的就是「短 TTL + 写时清除」组合，TTL 足够短，
// 击穿窗口天然很小
```

**③ 缓存雪崩：一大批 key 同一时间集体过期（或缓存实例宕机）**

初始化时把 1000 个分类一起灌进缓存、TTL 都设 30 分钟 → 30 分钟后集体失效 → 数据库瞬间被打爆。

解法：

```java
// 解法一：TTL 加随机抖动——让过期时间散开
long ttl = BASE_TTL + ThreadLocalRandom.current().nextLong(0, 60); // 基础 30m + 0~60s 抖动

// 解法二：分级 TTL（SelfBlog 的直接答案）——8 个具名缓存各自独立 TTL
// hotTagsCache(10m) / categoryCache(30m) / trendingCache(5m) / visitorSummaryCache(30s)
// 天然错峰，雪崩无从谈起

// 解法三：多级兜底——缓存全挂时限流降级（RateLimitInterceptor 收紧阈值），
// 核心接口走静态默认值（前端 store/site.ts 的默认站点配置就是这一层的思想：
// 后端挂了页面照样渲染）
```

**一张表记住三兄弟：**

| 问题 | 本质 | 典型场景 | SelfBlog 的对应手段 |
|------|------|----------|---------------------|
| 穿透 | 查不存在的数据，缓存形同虚设 | 恶意扫描不存在的 id | 真实 404 + 参数前置校验 |
| 击穿 | 热点 key 过期瞬间的并发冲击 | 爆款文章缓存到期 | synchronized 互斥 + 短 TTL 窗口小 |
| 雪崩 | 大量 key 同时失效 | 统一 TTL 集中到期 | 分级差异化 TTL（8 个缓存） |

## 3.12 限流：RateLimitInterceptor

公开接口（点赞、留言）必须限流，否则脚本一秒一万次请求直接把库打挂。SelfBlog 用拦截器 + Caffeine 计数实现固定窗口限流：

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitProperties properties;
    private final Cache<String, AtomicLong> counters = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)     // 1 分钟窗口，自动清理
        .build();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!properties.isEnabled()) return true;

        String path = request.getRequestURI();
        // 按路径前缀匹配阈值配置
        int limit = properties.matchLimit(path);
        if (limit <= 0) return true;

        // 维度：客户端标识（登录用户 or IP）+ 请求路径
        String client = resolveClientKey(request);
        String key = client + ":" + path;

        AtomicLong counter = counters.get(key, k -> new AtomicLong());
        if (counter.incrementAndGet() > limit) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }

    private String resolveClientKey(HttpServletRequest request) {
        // 优先用登录用户 ID，未登录退回 IP
        Long userId = SecurityUtils.getCurrentUserIdOrNull();
        return userId != null ? "u:" + userId : "ip:" + request.getRemoteAddr();
    }
}
```

注册到拦截器链：

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")
            .order(1);       // 数字越小越先执行，限流应尽早
}
```

**新增接口若不在已有前缀下，要补一条阈值**——SelfBlog 的明确约定。限流配置按前缀集中管理，一眼能看清全局的速率策略。

## 3.13 日志

### 3.13.1 门面与实现

Spring Boot 默认日志框架：**门面 SLF4J + 实现 Logback**。写代码永远面向门面：

```java
// Lombok 的 @Slf4j 最省事，自动生成 private static final Logger log = ...
@Slf4j
@Service
public class PostQueryServiceImpl implements PostQueryService {
    public PostVO getPost(Long id) {
        log.debug("查询文章 id={}", id);       // 参数化占位，不要用字符串拼接
        try {
            ...
        } catch (Exception e) {
            log.error("查询文章失败 id={}", id, e);   // 异常对象作最后一个参数，自动打印堆栈
            throw e;
        }
    }
}
```

**三个习惯**：

1. 占位符 `{}` 而不是 `+` 拼接——避免无谓的字符串构造，且能被日志系统优化
2. 异常对象传进 log 方法的最后一个参数
3. 日志级别：ERROR（系统故障）< WARN（可恢复异常/业务违规）< INFO（关键业务动作）< DEBUG（调试细节）

### 3.13.2 日志配置

```yaml
logging:
  level:
    root: INFO
    com.selfblog.mapper: DEBUG     # 开发期打印 SQL
  file:
    name: logs/selfblog.log
  logback:
    rollingpolicy:
      file-name-pattern: logs/selfblog-%d{yyyy-MM-dd}.%i.log
      max-file-size: 50MB
```

生产环境严禁 root 级别 DEBUG（磁盘会爆炸），只对需要排查的包临时开 DEBUG。

### 3.13.3 日志实战：PerformanceMonitoringAspect 为什么要打「error 级负例」

跑 SelfBlog 测试时你会看到大量 `[PERF] ... error:` 日志——**它们不是故障**，而是切面在记录「预期中的异常路径」：测试故意制造慢查询、故意抛异常来验证切面的异常通知与耗时统计是否正确。这带来两个实践启示：

1. **日志要有可检索的统一前缀**（`[PERF]`）：CI 与人都能 grep；没有前缀的日志在大海捞针时等于没有
2. **正常与异常路径都要留下证据**：只记成功请求的切面是「晴天日志」，出了问题全是盲区

另一个真实细节：`@Transactional` 的事务边界调试可临时开 `logging.level.org.springframework.jdbc=DEBUG`，日志里会明确出现「Initiating transaction commit / rollback」字样——比猜测可靠得多（A.2.12 排查事务失效的第一步）。

## 3.14 异步与定时任务

### 3.14.1 @Scheduled 定时任务

```java
@Component
@Slf4j
public class TrendingTask {

    /** 每 5 分钟刷新一次趋势榜 */
    @Scheduled(fixedDelay = 5 * 60 * 1000)     // 上次执行【完成后】再计时 5 分钟
    public void refreshTrending() {
        log.info("刷新趋势榜");
        ...
    }

    /** 每天凌晨 2 点清理过期访客事件 */
    @Scheduled(cron = "0 0 2 * * ?")           // 秒 分 时 日 月 周
    public void cleanExpiredEvents() { ... }
}
```

启动类要加 `@EnableScheduling`。`fixedRate`（上次**开始**时计时）与 `fixedDelay`（上次**结束**时计时）的区别要想清楚，任务耗时不可控时 fixedDelay 更安全。

### 3.14.2 @Async 异步执行

发邮件、写访客日志这类「不必同步等待结果」的操作，异步化能显著降低接口响应时间：

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}

@Service
public class VisitorLogService {
    @Async("taskExecutor")
    public void recordVisit(VisitorEvent event) {
        // 写库等耗时操作，调用方不等它
    }
}
```

**同样的失效规则**：@Async 方法必须被「从外部调用」才走代理，同类内部调用直接同步执行。另外提一句方向：Spring Boot 3.2+ 可以用 `spring.threads.virtual.enabled=true` 一键启用 Java 21 的虚拟线程处理请求，I/O 密集型博客场景吞吐会有明显提升（虚拟线程详见第二章）。

### 3.14.3 事件机制：@Async + @TransactionalEventListener 的组合拳

SelfBlog 的 `event/` 与 `listener/` 包提供了一堂完整的解耦课。业务背景：用户评论文章后要给作者发站内通知。**通知不是评论业务的一部分**——评论失败通知也该失败，但通知失败绝不能连累评论。把「发通知」塞进评论 Service 里，就制造了不该有的耦合。

**第一步：定义事件**（`event/CommentCreatedEvent.java`，真实源码）：

```java
@Getter
public class CommentCreatedEvent extends ApplicationEvent {
    private final Long commentId;
    private final Long commenterUserId;  // 评论者用户ID
    private final Long postAuthorUserId; // 文章作者用户ID
    private final Long postId;
    private final String content;
    private final boolean isReply;       // 是否是回复

    public CommentCreatedEvent(Long commentId, Long commenterUserId, Long postAuthorUserId,
                               Long postId, String content, boolean isReply) {
        super(commentId);
        this.commentId = commentId;
        // ... 字段赋值
    }
}
```

事件就是一个携带业务快照的不可变对象。注意它携带的是**数据**而不是实体引用——监听方拿到的是发布时刻的状态。

**第二步：业务代码发布事件**（评论 Service 里）：

```java
applicationEventPublisher.publishEvent(
    new CommentCreatedEvent(comment.getId(), comment.getUserId(),
                            post.getUserId(), comment.getPostId(),
                            comment.getContent(), isReply));
// 发布完立即返回，评论流程对「谁在听、做什么」一无所知
```

**第三步：监听器在事务提交后异步消费**（`listener/NotificationEventListener.java`）：

```java
@Async("notificationExecutor")                              // 异步：不占评论线程
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // 提交后才执行
public void handleCommentCreated(CommentCreatedEvent event) {
    try {
        if (Objects.equals(event.getCommenterUserId(), event.getPostAuthorUserId())) {
            return;                                          // 不给自己发通知
        }
        // ... 组装标题正文，调用 notificationService
    } catch (Exception e) {
        log.error("发送评论通知失败: commentId={}, error={}",
                  event.getCommentId(), e.getMessage());     // 吞异常：通知失败不影响业务
    }
}
```

这段代码浓缩了三个关键决策，每个都值得背下来：

1. **为什么是 `AFTER_COMMIT` 而不是普通 `@EventListener`**？普通监听器**同步执行在事务提交之前**——若监听器里也查库，查不到未提交的评论；更糟的是事务回滚时通知已经发出去了（作者收到「有人评论你」，点进去评论却是 404）。`AFTER_COMMIT` 保证「数据一定落库了才通知」。
2. **为什么还要叠加 `@Async`**？AFTER_COMMIT 只解决「时机」，监听默认仍在发布者的线程里同步执行，发通知的耗时照样拖慢评论接口。两个注解各管一维：**时机归 @TransactionalEventListener，线程归 @Async**，缺一不可。
3. **为什么 try-catch 吞掉全部异常**？异步线程里抛出的异常不会反馈给用户，只会污染日志线程池；而通知本就是「尽力而为」的副业——捕获、记日志、继续，是旁路逻辑的标准姿态。

一个隐蔽的坑：`@TransactionalEventListener` 监听的事件若在**无事务**上下文中发布，默认**不会触发**（它只挂在事务提交点上）。评论发布事件在事务方法里没问题，但如果某处重构后事务没了，通知会静默消失——这时给它加 `fallbackExecution = true` 可以让无事务时也执行。排查「通知偶尔收不到」时，先查事务边界。

三个类各归其位：**事件 = 数据快递单，发布方 = 只管寄，监听方 = 只管收**。把「主流程必须成功、旁路尽力而为」的逻辑用事件切开，是 Spring 里成本最低的解耦手段。



### 3.15.1 Actuator 健康检查

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

暴露 `/actuator/health`，容器编排（Docker healthcheck、K8s liveness probe）都用它判断进程是否存活。SelfBlog 的 CI docker job 就是轮询 `/actuator/health` 等容器就绪的。

### 3.15.2 Knife4j / SpringDoc 接口文档

引入 Knife4j 后访问 `/doc.html` 即可看到全项目接口的可视化文档并在线调试。Controller 方法上加注解增强可读性：

```java
@Operation(summary = "获取文章详情")
@GetMapping("/{id}")
public Result<PostVO> detail(@Parameter(description = "文章ID") @PathVariable Long id) { ... }
```

**SelfBlog 的约定**：Knife4j 在 `prod` profile 关闭（不把内部接口暴露到公网），只在 dev/test 开。

## 3.16 服务端推送：WebSocket 与 SSE（了解级）

### 3.16.1 什么时候需要推送

HTTP 的默认模型是「请求—响应」：客户端不问，服务端不答。但有些场景需要服务端**主动**通知：

- 聊天消息、实时协作（多人编辑同一文档）
- 通知推送（「你的文章有人评论了」）
- 进度条（长任务执行进度）

三条技术路线对比：

| 方案 | 方向 | 复杂度 | 适用 |
|------|------|--------|------|
| 轮询（定时拉） | 单向 | ★ | 数据变化不频繁、容忍延迟 |
| SSE（Server-Sent Events） | 服务端→客户端单向 | ★★ | 通知、进度、行情 |
| WebSocket | 全双工 | ★★★ | 聊天、协作、游戏 |

### 3.16.2 SSE：比 WebSocket 更简单的单向推送

「有新评论」这类通知是**纯单向**的——服务端→客户端。此时 SSE 就够了，而且它天生建在 HTTP 上：

```java
@RestController
public class NotificationController {

    // 每个连接一个 Emitter；客户端断开时 onComplete/onTimeout 会回调
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/notifications/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);   // 0 = 不超时
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    // 评论产生时（比如在 GuestCommentServiceImpl.createComment 里）调用
    public void pushNewComment(Long postId, String nickname) {
        String payload = postId + "|" + nickname;
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().name("new-comment").data(payload));
            } catch (IOException ex) {
                emitters.remove(e);   // 连接已死，清理
            }
        }
    }
}
```

前端只要一个浏览器原生 API，不需要任何库：

```typescript
const es = new EventSource('/api/notifications/subscribe')
es.addEventListener('new-comment', (e) => {
  console.log('有新评论：', e.data)
})
// EventSource 断线会自动重连——比 WebSocket 省心的地方
```

注意 SSE 走 HTTP/1.1 时每个连接占一个 TCP 连接，浏览器同域名只允许 6 个并发 HTTP/1.1 连接（HTTP/2 无此限制）；Nginx 反代时要关 `proxy_buffering`，否则事件会被缓冲住不实时。

### 3.16.3 SelfBlog 的取舍：轮询为什么够用

SelfBlog **没有用任何推送技术**，这是深思熟虑后的决定：

1. **场景不匹配**：单人博客没有「多用户实时协作」需求，评论数、点赞数晚几秒更新完全无感
2. **架构成本**：SSE/WebSocket 需要 Nginx 特殊配置、监控告警、连接管理；SQLite 池大小 1 的前提下，长连接的收益更低
3. **替代方案够好**：前端在文章详情页停留时按需刷新互动数据（进入页面拉一次、点赞操作后拉一次），Caffeine 缓存兜住读压力

> **选型心法**：技术方案不是越先进越好，而是「复杂度预算」花在刀刃上。SelfBlog 把复杂度留给了 SEO、安全、测试基线这些回报明确的领域，而把实时性这种「低回报高成本」的需求用最朴素的方式解决。**学架构先学取舍，再学实现。**

## 3.17 Spring Boot 测试

### 3.17.1 测试切片的选择

| 注解 | 范围 | 适用 |
|------|------|------|
| `@SpringBootTest` | 启动完整上下文 | 端到端、集成测试，慢 |
| `@WebMvcTest(XxxController.class)` | 只装载 Web 层，Service 全部 mock | Controller 层单测，快 |
| `@DataJpaTest` 等数据切片 | 只装载数据层 | Repository 测试 |
| 纯 Mockito（无 Spring） | 无容器 | Service 逻辑单测，最快 |

### 3.17.2 SelfBlog 的控制器切片测试配方（含血泪教训）

```java
@WebMvcTest(AdminPostController.class)
@WithMockUser(roles = "admin")           // 模拟已登录管理员
class AdminPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogPostService postService;

    @MockBean
    private JwtUtils jwtUtils;           // ★ 必须！原因见下

    @Test
    void listPosts_shouldReturn200() throws Exception {
        mockMvc.perform(get("/admin/posts"))     // ★ 切片测试路径不带 /api 前缀
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

**三个不写就会失败的细节**：

1. **必须 `@MockBean JwtUtils`**：`@WebMvcTest` 会装载 `SecurityConfig`，它拉起的 `jwtAuthenticationFilter` 依赖 `JwtUtils` Bean。切片没扫描到真实实现，不 mock 就直接「Bean 找不到，上下文启动失败」。
2. **不要用 `@AutoConfigureMockMvc(addFilters = false)` 绕鉴权**：实测它不会「关掉过滤器」，而是请求直接返回 401。正确姿势是 `@WithMockUser(roles = "admin")`。
3. 方法级 `@PreAuthorize` **在切片里是生效的**：可以顺手写两个负例测试验证 403（非管理员）与 401（匿名）。

SelfBlog 后端维持 **588 个测试、0 失败** 的基线，每个 bug 修复都先补一个能复现它的测试——这是重构时敢动手的底气来源。日志里大量 `[PERF] ... error:` 是性能切面的正常负例输出，不是失败。

### 3.17.3 测试的 application.yml

`src/test/resources/application.yml` 覆盖主配置：数据源指向 H2 内存库、测试 profile 关闭缓存。若设置了 `DB_TYPE` 环境变量，`DatabaseConfig` 会顶掉测试数据源——所以**跑测试前确认没导出 DB_TYPE**。

## 3.18 打包与运行

```powershell
# 打包（跳过测试时也要带 skip 参数防止 frontend-maven-plugin 重建前端）
cd backend
.\mvnw.cmd clean package "-DskipTests" "-Dskip.npm=true" "-Dskip.installnodenpm=true"

# 产物：target/selfblog.jar（pom 里 finalName=selfblog）
java -jar target/selfblog.jar --spring.profiles.active=prod
```

**跑测试的两个必带参数**：SelfBlog 的 `pom.xml` 配了 `frontend-maven-plugin`，在 `initialize` 阶段会在 `../frontend` 执行 `npm ci` + `npm run build`。不带 `-Dskip.npm=true -Dskip.installnodenpm=true`，一次 `mvn test` 会先把整个前端重建一遍并**改写前端构建产物**。

本机 `JAVA_HOME` 需指向 JDK 21（项目不支持 JDK 25）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

### 3.18.1 生产运行清单（部署前的 8 项检查）

`java -jar` 能跑起来只是第一关。上生产前过一遍这张清单：

| # | 检查项 | 怎么做 |
|---|--------|--------|
| 1 | `.env` 就位且无引号 | JWT_SECRET ≥32 位、ADMIN_PASSWORD ≥12 位；对照 `.env.example` |
| 2 | profile 正确 | `--spring.profiles.active=prod`（Knife4j 自动关闭） |
| 3 | 数据库策略明确 | 全新库跑 `schema-fresh-install.sql`；已有库只跑 `migration-selfblog.sql`（它含 DROP，方向搞反会清空数据） |
| 4 | `SITE_BASE_URL` 已设置 | 否则 sitemap/canonical/订阅链接退化为直连请求地址，可被伪造 Host 污染 |
| 5 | ADMIN_PATH 已改 | 默认 `admin` 会被扫描器枚举；改成随机串并检查不违反 `[A-Za-z0-9][A-Za-z0-9_-]{1,39}` 约束 |
| 6 | 限流阈值复核 | RateLimitProperties 各前缀是否匹配实际流量 |
| 7 | 健康检查可用 | `curl /actuator/health` 返回 UP；Docker/compose 的 healthcheck 依赖它 |
| 8 | 日志落盘与轮转 | logs/ 目录可写、rollingpolicy 生效、磁盘余量够 |

**工程诚信条款**：本仓库的 Docker 镜像构建与运行**从未在本机验证**（无 Docker 守护进程），CI 的 docker job 只验证「镜像可构建、容器可启动、健康检查可通过」。真实部署请在你的环境里完整走一遍 compose 流程并自查以上 8 项——文档必须如实记录验证边界，这是工程与玩具的分界线。

## 3.19 常见错误排查手册

| 现象 | 大概率原因 | 解法 |
|------|-----------|------|
| Bean 注入为 null | 类没被扫描（不在启动包子包下）| 检查包结构 |
| 事务不回滚 | 内部调用 / 异常被吞 / 未写 rollbackFor | 逐条核对 3.9.3 |
| `BeanCurrentlyInCreationException` | 构造器循环依赖 | 抽公共依赖为第三类 |
| JWT 401 但 token 正确 | secret 不一致（.env 加了引号）| 去掉引号重试 |
| 切片测试启动失败找不到 JwtUtils | 忘了 `@MockBean JwtUtils` | 补上 |
| 切片测试全 401 | 用了 addFilters=false 或没加 @WithMockUser | 按 3.17.2 配方写 |
| 查出已删除数据 | 手写 SQL 忘了 `is_deleted = 0` | XML 里补条件 |
| H2 测试报 SQL 语法错 | MySQL 专有 SQL 没写兜底语句 | 拆 databaseId |
| 测试时数据库不对 | 导出了 DB_TYPE 环境变量 | 清掉再跑 |
| 429 | 触发限流 | 检查 RateLimitProperties 前缀配置 |
| mvn test 变慢且前端产物变了 | 忘带 `-Dskip.npm=true` | 补上两个 skip 参数 |

## 3.20 综合实战：给博客加一个「文章阅读时长统计」

前面二十节都是分知识点讲，这一节把零件组装成机器：从需求到落库的完整决策链。这是「学完 Spring Boot 后如何动手」的示范——**也是 ch07 全栈实战的后端预演**。

**需求**：统计每篇文章的真实阅读时长（访客在页面停留 ≥N 秒记一次），后台能看每篇的总时长与平均时长。

**第一步：需求裁剪（最重要的三分钟）**

问三个问题：谁用？（匿名访客上报）多准？（容许重复上报，宁可信其无）多实时？（后台看，分钟级足够）。于是砍掉：登录绑定、去重协议、实时推送——只留「POST 上报 + 聚合查询」。

**第二步：接口与存储设计**

```
POST /api/posts/{postId}/reading-duration        # 公开，Body: { "duration": 37 }
     → Result<Void>
```

存储方案 A：article 表加 reading_total 字段——最简单，但丢失明细、且高并发写单行成热点。
存储方案 B：独立明细表 reading_duration(id, post_id, duration, create_time)——可回溯、可审计，聚合用 SUM。

选 B（SelfBlog 的 visitor_event 表就是这一思路的落地）。DDL 要点：`post_id` 建索引（聚合一律走它）、无唯一约束（天然容忍重复上报）。

**第三步：分层落位（对照 3.9.4 三分模型）**

```java
// Controller——controller/post/ReadingDurationController.java
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class ReadingDurationController {

    private final PostInteractionService postInteractionService;

    @PostMapping("/{postId}/reading-duration")
    public Result<Void> report(@PathVariable Long postId,
                               @RequestBody ReadingDurationRequest req) {
        postInteractionService.recordReadingDuration(postId, req.getDuration());
        return Result.ok();
    }
}
// 放行：SecurityConfig 加 /posts/*/reading-duration；
// 限流：RateLimitProperties 确认该路径落在已有公开写前缀下，否则补一条阈值

// Interaction Service——写副作用的家，不在门面、不在查询
@Service
@RequiredArgsConstructor
public class PostInteractionServiceImpl implements PostInteractionService {

    private final ReadingDurationMapper readingDurationMapper;
    private final BlogPostMapper postMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordReadingDuration(Long postId, Integer duration) {
        // 参数防线：文章必须存在且已发布；时长夹进合法区间防脏数据
        BlogPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(404, "文章不存在");
        }
        int safe = (duration == null) ? 0 : Math.min(Math.max(duration, 0), 3600);
        if (safe < 5) {
            return;                      // 小于 5 秒视为误触，静默丢弃
        }
        ReadingDuration record = new ReadingDuration();
        record.setPostId(postId);
        record.setDuration(safe);
        readingDurationMapper.insert(record);   // createTime 由 MetaObjectHandler 自动填充
    }
}

// Query Service——聚合查询放查询服务，readOnly 事务 + 缓存
@Transactional(readOnly = true)
public ReadingStatsVO getReadingStats(Long postId) {
    Long total = readingDurationMapper.sumDuration(postId);     // XML: SELECT IFNULL(SUM(duration),0)
    Long count = readingDurationMapper.countByPostId(postId);
    return ReadingStatsVO.of(postId, total,
            count == 0 ? 0 : total / count);                    // 平均时长
}
```

**第四步：测试（一个功能至少三条）**

```java
@WebMvcTest(controllers = ReadingDurationController.class)
@MockBean JwtUtils jwtUtils;                          // 切片配方三件套（3.17.2）
@WithMockUser(roles = "admin") 或匿名场景按放行路径测试

// ① 正常上报 200；② duration=-5 → 0 上库（夹取）；③ 文章不存在 → 404 信封
```

**第五步：自查清单（新功能上线的肌肉记忆）**

- [ ] SecurityConfig 放行路径对吗？公开接口要不要限流阈值？
- [ ] 手写 SQL（SUM 那条 XML）将来会不会踩 is_deleted？（明细表无软删可豁免，写明理由）
- [ ] 写路径 rollbackFor 拉满？查询路径 readOnly？
- [ ] 参数夹取了吗？（duration 合法区间）恶意大值会打爆表吗？
- [ ] 聚合接口要缓存吗？（后台低频看 → 不缓存，理由记录在案）
- [ ] 测试三条：正常/边界/不存在
- [ ] 文档三件套：CHANGELOG 未发布条目 + 影响文件表 + 相关 README

**复盘**：这个功能没有一行「高深」代码，全部零件来自本章各节——条件放行、三分模型、参数校验、异常信封、自动填充、切片测试。**Spring Boot 的学习终点不是背注解，而是拿到需求后能沿着「需求裁剪 → 接口设计 → 分层落位 → 测试 → 文档」的轨道一路走完**。第 7 章会把这条轨道拉通到前端。

## 3.21 总结

本章以 SelfBlog 为蓝本覆盖了 Spring Boot 开发的完整知识链：

1. **自动配置与条件装配**：理解魔法，才能掌控魔法（@ConditionalOnProperty 切数据源）
2. **IoC/DI**：构造器注入为主，同类型多 Bean 用 @Primary/@Qualifier
3. **AOP**：自定义注解 + 环绕通知实现性能监控；警惕内部调用失效
4. **Web 层**：RESTful、自动 /api 前缀、Result 信封、全局异常、参数校验、文件上传
5. **MyBatis Plus**：单表零 SQL、逻辑删除的 is_deleted=0 约定、多库方言
6. **事务**：三分服务模型（门面/查询/交互）+ 各自的事务策略 + 六大失效场景
7. **安全**：JWT（jjwt 0.12 新 API）+ 过滤器链 + 单作者模式防线
8. **缓存与限流**：8 个具名缓存的差异化 TTL、Caffeine 固定窗口限流
9. **测试**：@WebMvcTest 配方（@MockBean JwtUtils + @WithMockUser）
10. **打包**：mvnw + skip.npm 双参数、jar 直接运行

下一章我们切换赛道，进入前端世界：TypeScript。

---

**参考资源：**

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Security 架构](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- [MyBatis Plus 官方文档](https://baomidou.com/)
- [jjwt GitHub 仓库](https://github.com/jwtk/jjwt)（0.12 API 示例最权威）
- [Caffeine Wiki](https://github.com/ben-manes/caffeine/wiki)
- [Bean Validation 规范](https://jakarta.ee/specifications/bean-validation/)
- [SelfBlog 项目源码](https://github.com/Xinghe-0203/SelfBlog)

---

# 第四章 TypeScript 全面入门

> 「SelfBlog 项目学习教程」第四章。本章承接第三章的 Spring Boot 后端，正式进入 SelfBlog 前端（Vue 3.5 + Vite 6 + TypeScript 6.0.3 + Pinia + Axios）的 TypeScript 世界。所有示例除特别说明外，都可以直接对应到 `frontend/` 目录下的真实代码。

## 本章导学

学完本章，你应该能够：

1. 独立搭建 TypeScript 开发环境，看懂并修改 `tsconfig.json`；
2. 熟练使用 TS 的基础类型、接口、泛型来描述一个 API 的输入输出；
3. 理解 SelfBlog 前端为什么在 `tsconfig.json` 里开启 `strict` 和 `noUncheckedIndexedAccess`，以及在这套严格规则下如何正确写代码；
4. 读懂并仿写 SelfBlog 的 API 层（`frontend/src/api/index.ts` 的 Axios 封装），让「后端响应信封」在前端拥有完整的类型表达；
5. 遇到常见编译错误（TS2532、TS2307、TS2345……）时不再慌张，能自己定位并修复。

> **学习建议**：TypeScript 不是一个新语言，而是 JavaScript + 类型标注。你不需要先精通 JS 才能学 TS——反过来，在项目里写 TS 的过程，正是同时补齐两者最好的方式。本章所有代码都建议动手敲一遍，而不是只「看懂」。

---

## 4.1 TypeScript 简介与开发环境搭建

### 4.1.1 为什么需要 TypeScript？

先看一段每个 JS 开发者都写过的「事故代码」：

```ts
// user.js —— 纯 JavaScript
function getDisplayName(user) {
  return user.nikeName.toUpperCase() // 拼写错了：应该是 nickname
}

getDisplayName({ nickname: '张三' })
// 运行时才炸：TypeError: Cannot read properties of undefined (reading 'toUpperCase')
```

这段代码的问题不在于「写错拼写」——谁都会犯错——而在于**错误的暴露时机**：它要等到代码真正跑起来、用户真的点到那个按钮，才会崩溃。而且崩溃的位置（浏览器控制台里某一行的 `undefined`）往往离真正的病因（拼写错误）十万八千里。

JavaScript 是**动态类型**语言：变量的类型在运行时才确定，编译器完全不关心 `user` 里有没有 `nickname`。就像两个人口头约好「你把资料发我」，结果你发过去他打不开——出了事，谁也说不清当初到底约定了什么。

TypeScript 的解法是把口头约定变成**白纸黑字的合同**：

```ts
// user.ts —— TypeScript
interface User {
  nickname: string
}

function getDisplayName(user: User): string {
  return user.nikeName.toUpperCase()
  //              ~~~~~~~~ 编辑器立刻画红线：
  //              Property 'nikeName' does not exist on type 'User'. Did you mean 'nickname'?
}
```

**还没运行，错误就已经被拦下来了。** 这就是 TypeScript 的核心价值：

| 维度 | 纯 JavaScript | TypeScript |
|------|--------------|------------|
| 错误暴露时机 | 运行时（用户可能先撞上） | 编译时（保存文件的那一刻） |
| 重构体验 | 改一处字段名，全项目 grep 碰运气 | 改类型定义，所有不兼容处立刻报错 |
| 编辑器体验 | 补全靠猜 | 精确补全属性、方法、参数 |
| 团队协作 | 读代码猜结构 | 类型即文档 |
| 学个新库 | 翻文档看参数 | `.d.ts` 类型文件就是最权威的文档 |

一句话总结：**TypeScript 不改变你的程序怎么运行，它改变的是你「发现错误有多早」。**

### 4.1.2 TypeScript 和 JavaScript 的关系

三句话讲清楚：

1. **TS 是 JS 的超集**：所有合法 JS 代码都是合法 TS 代码，你可以把 `.js` 改名为 `.ts` 一行不改，项目照样能跑（只是没有类型保护）。在这个基础上，TS 只做加法：类型标注、接口、泛型、枚举……
2. **类型会被「擦除」**：TS 编译器最终把代码转译回纯 JS，所有 `interface`、类型标注、泛型参数统统消失。运行时只有 JS——**类型只存在于编译期，运行时不存在**。这句话后面理解很多现象（比如为什么 Vue 的 props 类型挡不住运行时传错值）都靠它。
3. **TS 不负责提速**：它检查代码、转译代码，但不优化运行性能。性能优化交给 Vite/esbuild/浏览器。

SelfBlog 前端的技术栈里，TypeScript 的版本是 **6.0.3**（见 `frontend/package.json` 的 `"typescript": "^6.0.3"`），类型检查由 **vue-tsc 3.3.3** 完成（它是给 TypeScript 加上「认识 `.vue` 文件」能力的官方包装）。

### 4.1.3 安装与第一次编译

TypeScript 编译器命令叫 `tsc`（TypeScript Compiler）。在一个空目录里体验一下：

```bash
# 安装（作为开发依赖，不进生产包）
npm install --save-dev typescript

# 确认安装成功，打印版本号
npx tsc --version
```

写第一个文件 `hello.ts`：

```ts
const author: string = 'Zora'
const postCount: number = 42

console.log(`作者 ${author} 已发布 ${postCount} 篇文章`)
```

编译并运行：

```bash
npx tsc hello.ts        # 生成 hello.js（类型标注被擦除）
node hello.js           # 输出：作者 Zora 已发布 42 篇文章
```

打开生成的 `hello.js`，你会看到 `: string`、`: number` 全都不见了——这就是「类型擦除」。开发时把两个命令挂成 watch 模式更省心：

```bash
npx tsc hello.ts --watch   # 文件一保存就重新检查、重新编译
```

### 4.1.4 tsconfig.json：给整个项目立规矩

真实项目不会一个文件一个文件地编译，而是用一个 `tsconfig.json` 描述「这个目录下所有 TS 文件怎么检查」。运行 `npx tsc --init` 可以生成一份带全量注释的模板——注释多到吓人，其实常用的就十来项：

```jsonc
{
  "compilerOptions": {
    "target": "ES2020",          // 编译成哪个年代的 JS
    "module": "ESNext",          // 生成什么模块格式（import/export）
    "lib": ["ES2020", "DOM"],    // 可用的内置 API 声明（浏览器 + ES2020）
    "outDir": "./dist",          // 编译产物输出目录
    "strict": true,              // 打开严格模式全家桶（强烈推荐）
    "moduleResolution": "bundler", // 模块解析策略：配合打包器
    "esModuleInterop": true,     // 兼容 CommonJS 的默认导出
    "skipLibCheck": true,        // 跳过 node_modules 里 .d.ts 的检查（提速）
    "sourceMap": true            // 生成 sourceMap 方便调试
  },
  "include": ["src"]             // 检查哪些文件
}
```

常用选项速查表：

| 选项 | 作用 | 不设会怎样 |
|------|------|-----------|
| `target` | 产物的 JS 语法版本（ES5/ES2020/ESNext） | 旧浏览器可能看不懂新语法 |
| `module` | 产物模块格式（CommonJS/ESNext） | 与打包器/Node 运行方式不匹配 |
| `lib` | 类型层面的「内置 API 清单」 | 用 `Promise`、`Map` 等会报「找不到名称」 |
| `strict` | 一整套严格检查的总开关 | 大量潜在错误被放行 |
| `outDir` / `noEmit` | 产物放哪 / 只检查不产出 | —— |
| `paths` | 路径别名（如 `@/` → `src/`） | 只能写相对路径 |
| `include` / `exclude` | 检查范围 | 默认包含所有 `.ts` 文件 |

有一个初学者最容易困惑的点要提前说破：**在 Vue + Vite 项目里，`tsc`（实际是 `vue-tsc`）只当「质检员」，不当「流水线工人」**。SelfBlog 的 `package.json` 里：

```json
"build": "vue-tsc --noEmit && vite build"
```

`--noEmit` 意思是「只检查类型，不要产出任何 JS 文件」；真正把 TS 转译成浏览器能跑的 JS 的，是 Vite 内置的 esbuild。质检员和工人分工明确：

- **`vue-tsc`（质检员）**：仔细读类型，发现问题就拒绝放行（报错退出）；
- **esbuild（工人）**：只管快速把代码转译、打包，不做类型检查。

所以「类型报错」永远发生在 `npm run type-check` 或 `npm run build` 时，而 `npm run dev` 开发服务器为了速度通常只转译不检查——**别以为 dev 页面能跑就万事大吉，类型闸门在 type-check 和 build 那里等着呢。**

---

## 4.2 基础类型

### 4.2.1 类型标注与类型推断

给变量声明类型的最基本语法是「冒号 + 类型」：

```ts
let title: string = 'SelfBlog 学习教程'
let views: number = 1024
let published: boolean = true
```

但真正的高手写 TS，反而很少写这种标注，因为 TS 的**类型推断**足够聪明——声明时直接赋值，类型自动确定：

```ts
let title = 'SelfBlog 学习教程'   // 推断为 string
let views = 1024                  // 推断为 number
views = '很多'                    // ❌ Type 'string' is not assignable to type 'number'
```

经验法则：**局部变量靠推断，函数参数和返回值必须显式标注**。函数签名是「合同」，合同必须白纸黑字：

```ts
// 参数 + 返回值都标注：这就是这个函数的「使用说明书」
function formatReadingTime(minutes: number): string {
  return `${minutes} 分钟`
}
```

### 4.2.2 string / number / boolean 与数组

三个原始类型大家最熟悉，重点说数组。数组类型有两种等价写法：

```ts
// 写法一：T[]（推荐，简短）
const posts: string[] = ['第一篇', '第二篇']

// 写法二：Array<T>（泛型写法，类型参数一长时更易读）
const tags: Array<{ id: number; name: string }> = []
```

SelfBlog 的真实代码里（`frontend/src/views/Home.vue`）：

```ts
const posts = ref<Post[]>([])   // 一个装 Post 对象的响应式数组
const tags = ref<Tag[]>([])
```

数组开箱自带一条陷阱：**访问不存在的下标，JS 返回 `undefined`，TS 默认假装不知道**。`arr[5].name` 默认能通过类型检查，运行时却可能炸——SelfBlog 用 `noUncheckedIndexedAccess` 关掉了这个漏洞，4.10 节重点讲。

### 4.2.3 元组（Tuple）：长度和每个位置都固定的数组

数组是「一堆同类型的东西」，元组是「一排位置各司其职」：

```ts
// 数组：任意多个数字
const scores: number[] = [90, 85, 77]

// 元组：恰好两个元素，第一个是 number，第二个是 string
const postSummary: [number, string] = [42, 'TypeScript 入门']

postSummary[0].toFixed()  // ✅ TS 知道[0]是 number
postSummary[1].length     // ✅ TS 知道[1]是 string
```

典型用途是函数一次返回多个值：

```ts
function parseJwt(token: string): [header: string, payload: string, signature: string] | null {
  const parts = token.split('.')
  if (parts.length !== 3) return null
  return [parts[0], parts[1], parts[2]]
}
```

注意元组支持**给每个位置命名**（`header`、`payload`……），编辑器会在调用处提示名字，比裸的 `[string, string, string]` 可读性强得多。

### 4.2.4 枚举（Enum）与它的替代品

枚举把一组相关的常量收进一个命名空间：

```ts
enum PostStatus {
  Draft = 0,     // 未发布
  Published = 1  // 已发布
}

function getStatusText(status: PostStatus): string {
  return status === PostStatus.Published ? '已发布' : '草稿'
}
```

- **数字枚举**：不写值则从 0 自增；反过来可以从数字反查名字（`PostStatus[1] === 'Published'`）。
- **字符串枚举**：每个成员显式赋字符串，调试时日志可读，更推荐：

```ts
enum Theme {
  Light = 'light',
  Dark = 'dark'
}
```

不过要说明：**SelfBlog 前端大量使用「字符串字面量联合类型」而不是枚举**：

```ts
// frontend/src/types/common.ts —— 真实代码
export type SortOrder = 'asc' | 'desc'

// post.ts 里的 PostStatus 也是 type 定义的联合类型
```

两种写法都能表达「值只能取这几个」，取舍如下：

| | enum | 字面量联合 `'a' | 'b'` |
|---|------|----------------------|
| 编译产物 | 生成真实的 JS 对象（有运行时代价） | 什么都不生成，纯类型 |
| 运行时反查 | 支持 | 不支持 |
| 与 JS 生态互操作 | 需要导入枚举对象 | 字符串直接用 |
| 类型安全 | 好 | 好 |

`enum` 是 TS 独有的运行时构造，在「类型擦除」哲学下显得有点特殊；字面量联合更轻、更贴近 JS。新代码没有特殊理由时，**优先字面量联合**。

### 4.2.5 any：关掉烟雾报警器的类型

`any` 表示「放弃类型检查，随便吧」：

```ts
let data: any = JSON.parse(text)
data.foo.bar.baz          // 编译通过，运行时生死有命
data().anything()         // 编译也通过！
```

`any` 能通过任何检查、赋给任何类型、从任何类型赋来——它不是「万能类型」，而是**检查盲区**。比喻：`any` 就像把家里的烟雾报警器拆了，世界确实清净了，但起火也没人报信。JSON.parse、第三方无类型库、历史遗留代码……用到 `any` 的地方应该被逐个消灭，团队约定是**每次写 `any` 前先问自己三遍：我真的拿不到类型吗？**

### 4.2.6 unknown：诚实的 any

`unknown` 是 `any` 的安全版：**接受任何值，但用之前必须先「验明正身」（收窄）**：

```ts
function parse(text: string): unknown {
  return JSON.parse(text)
}

const data = parse('{"title":"hello"}')
data.title            // ❌ 'data' is of type 'unknown'
data = 123            // ✅ unknown 可以被赋任何值

if (typeof data === 'object' && data !== null && 'title' in data) {
  console.log((data as { title: string }).title)  // 收窄后才能用
}
```

这个「必须先检查才能用」的约束正是它的价值：**接收方被迫面对「数据可能不是我想的那样」这个事实**。SelfBlog 的 API 层里，处理「后端返回的原始信封」时就用了这个思路（4.13 节实战）。经验法则：**值从「不可信来源」进来（网络、JSON.parse、用户输入），类型标注用 `unknown`，不用 `any`。**

### 4.2.7 void 与 null / undefined

- **`void`**：函数「没有有意义的返回值」。约定俗成：回调/副作用函数标注 `void`。

```ts
function log(msg: string): void {
  console.log(msg)
  // 没有 return，或 return; —— 不允许 return 一个值
}
```

- **`null`**：「有意的空」（这里明确没有值）；
- **`undefined`**：「还没有值」（压根没赋过 / 属性不存在 / 函数没返回）。

在 `strict` 模式（含 `strictNullChecks`）下，`null` 和 `undefined` 不再是「任何类型的隐藏成员」：

```ts
let name: string = 'Zora'
name = null   // ❌ strict 模式下报错：Type 'null' is not assignable to type 'string'

let nickname: string | null = null   // 想允许空，就显式写进联合类型
nickname = '星河'
```

这是 strict 模式最大的价值之一：**「可能为空」这件事在类型上无处遁形**。你之后会反复看到 SelfBlog 代码里的 `user.value?.role`、`posts.value = []` 这样的写法，全是在和「可能为空」打交道。

### 4.2.8 never：永不发生的类型

`never` 表示「这个位置根本不会有值」：

- 永远抛错的函数；
- 永远不会执行到结尾的函数（死循环）；
- 类型系统在穷尽检查后判定「不可能」的分支。

它最实用的场景是**穷尽检查**——配合字面量联合，给 `switch` 上保险：

```ts
type Shape = 'circle' | 'square'

function area(shape: Shape): number {
  switch (shape) {
    case 'circle': return 1
    case 'square': return 1
    default:
      // 如果将来有人给 Shape 加了 'triangle' 却忘了改这里，
      // 这一行就会编译报错：'triangle' 不能赋给 never
      const _exhaustive: never = shape
      return _exhaustive
  }
}
```

原理：所有成员都被处理完后，`default` 分支里 `shape` 的类型被收窄为「剩下的成员」，即 `never`；只要还有漏网之鱼，它就不是 `never`，赋值就会报错。**用类型系统逼着未来的自己把新增情况处理完整**，这是 TS 优雅之处。

---

## 4.3 接口与类型别名

### 4.3.1 interface：给对象画结构图

`interface` 描述「一个对象长什么样」——有哪些属性、什么类型：

```ts
interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  role: 'admin' | 'user'
}
```

SelfBlog 前端 `frontend/src/types/` 目录就是这么组织类型的：按业务域拆成 `user.ts`、`post.ts`、`comment.ts` 等文件，再由 `types/index.ts` 统一 re-export。写后端对接代码前，先翻这个目录——**类型定义就是前后端的「合同文本」**。

### 4.3.2 可选属性与只读属性

```ts
interface Post {
  id: number
  title: string
  summary?: string        // 可选：可以有，可以没有（类型自动含 undefined）
  readonly createdAt: string  // 只读：初始化后不许改
}

const post: Post = { id: 1, title: 'TS 入门' }        // ✅ summary 没写，OK
post.createdAt = '2026-01-01'                        // ❌ Cannot assign to 'createdAt'
console.log(post.summary?.length)                    // 可选属性访问要用 ?.
```

`summary?: string` 等价于 `summary: string | undefined`。读它的地方必须处理「不存在」的情况——`?.` 或先判断。

### 4.3.3 索引签名：属性名不固定的对象

有些对象的键在写类型时根本列不全，比如 SelfBlog 后端的 `site_setting` 表就是一张 KV 表（`setting_key` + `setting_value`）。前端把它建模成键值对就要用**索引签名**：

```ts
interface SiteSettings {
  [key: string]: string     // 任意 string 键 → string 值
}

const settings: SiteSettings = {
  siteName: 'SelfBlog',
  footer: '© 2026'
}
```

更常用的是内置工具类型 `Record`，语义完全相同：

```ts
const settings: Record<string, string> = { /* 同上 */ }
```

注意：**索引签名一旦声明，所有「具名」属性的类型都必须兼容它**：

```ts
interface Mixed {
  [key: string]: string
  port: number   // ❌ number 不兼容索引签名的 string
}
```

### 4.3.4 接口里的函数类型

接口可以描述「这个对象上有个函数」，两种写法等价：

```ts
interface Fetcher {
  // 方法简写
  get(url: string): Promise<string>
  // 属性 + 函数类型（可以重命名复用）
  post: (url: string, body: unknown) => Promise<string>
}
```

专门的函数类型建议先起名再使用，可读性好：

```ts
type Comparator<T> = (a: T, b: T) => number

function sortPosts(posts: Post[], by: Comparator<Post>) { /* ... */ }
```

### 4.3.5 extends：接口的继承

```ts
interface Entity {
  id: number
  createdAt: string
}

interface Post extends Entity {
  title: string
}

// 相当于 Post = Entity 的所有成员 + title
```

可以同时继承多个接口，用逗号分隔。SelfBlog 的类型体系大量用到这种「基础类型 + 扩展」的模式，例如分页：先定义「分页壳」，再套任意业务数据（见 4.6 泛型一节）。

### 4.3.6 type：类型别名

`type` 给**任何类型**（不只是对象）起名字：

```ts
// 联合类型——interface 表达不了
type ID = number | string

// 元组
type Point = [number, number]

// 从现有类型推导
type PostTitles = Post['title']

// 条件类型（4.8 节）
type Maybe<T> = T | null | undefined
```

### 4.3.7 interface vs type：怎么选？

两者在「描述对象结构」上几乎完全等价，可以互相替换，但有差异：

| 能力 | interface | type |
|------|-----------|------|
| 描述对象/函数 | ✅ | ✅ |
| 联合、元组、字面量、条件类型 | ❌ | ✅ |
| extends 继承 | ✅（可继承多接口） | ✅（交叉 `&`） |
| 合并声明（同名自动合并） | ✅（同名 interface 自动合并成员） | ❌（重名直接报错） |
| 给「已存在的库」补充成员 | ✅ 靠声明合并 | ❌ |

**SelfBlog 的约定（也是社区主流约定）**：

- 对象/类的「形状」用 `interface`——报错信息里 interface 的名字会保留，可读性更好，也留了扩展余地；
- 联合、元组、工具类型、从现有类型推导的「计算型类型」用 `type`；
- 团队内保持一致即可，不必教条。

### 4.3.8 实战拆解：SelfBlog 的通用类型层

`frontend/src/types/common.ts` 是全项目类型的「地基」，全文不长，值得逐行读懂：

```ts
/** API 统一响应格式 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页请求参数 */
export interface PaginationParams {
  pageNum?: number
  pageSize?: number
}

/** 分页响应数据 */
export interface PaginatedData<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 分页 API 响应 */
export type PaginatedResponse<T> = ApiResponse<PaginatedData<T>>

/** 排序方式 */
export type SortOrder = 'asc' | 'desc'

/** 通用 ID 类型 */
export type ID = number | string

/** 通用键值对 */
export type Recordable<T = unknown> = Record<string, T>

/** 可为空 */
export type Nullable<T> = T | null

/** 可选 */
export type Optional<T> = T | undefined
```

看点：`ApiResponse<T = unknown>` 里的 `= unknown` 是**默认类型参数**——不关心业务数据时写 `ApiResponse` 即可，等价于 `ApiResponse<unknown>`；`PaginatedResponse<T>` 展示了类型组合的威力：**分页响应 = 信封 + 分页壳 + 业务数据**，三块积木拼出来的类型在编辑器里可以一路点进去看定义。`types/index.ts` 再把它们与各业务域类型统一导出，项目里任何地方 `import type { ... } from '@/types'` 就够了。

---

## 4.4 函数与类型

### 4.4.1 参数、返回值与推断

```ts
function add(a: number, b: number): number {
  return a + b
}
```

返回值类型通常可以省略——函数体简单时 TS 会推断；但**导出给别人的公共函数建议显式写返回值**：一是当作文档，二是防止函数体改动后返回类型「悄悄变了」却没人发现。

### 4.4.2 可选参数、默认参数、剩余参数

```ts
// 可选参数：必须排在必选参数后面
function greet(name: string, prefix?: string): string {
  return `${prefix ?? '你好'}，${name}`
}

// 默认参数：自带默认值，调用方可省略
function page(pageNum = 1, pageSize = 10) { /* ... */ }

// 剩余参数：把多余的实参收成数组
function sum(...nums: number[]): number {
  return nums.reduce((acc, n) => acc + n, 0)
}
```

注意可选参数 `prefix?: string` 的类型是 `string | undefined`，函数体内直接 `prefix.length` 会报错——老老实实用 `??` 或判空。**TS 的严格模式和现实生活是一致的：没给的东西，用之前先确认有。**

### 4.4.3 函数类型表达式与调用签名

```ts
// 函数类型表达式：描述「一个函数」
type Mapper = (value: string, index: number) => string

// 对象「可被调用」：调用签名
interface ApiClient {
  (url: string, config?: RequestInit): Promise<Response>
  timeout: number   // 还能带属性——既是函数又是对象
}
```

后者听起来冷门，其实你天天在用：Axios 实例 `api` 本身「可调用」（`api(config)`），又挂着 `get`/`post` 属性和 `interceptors`，Axios 的类型定义正是这么写的。

### 4.4.4 函数重载

同一个函数名，按**不同的参数组合**有不同的返回类型——TS 用「重载签名 + 一个实现」表达：

```ts
// 重载签名（可以有多个，只描述，不实现）
function formatDate(date: string): string
function formatDate(date: Date): string
function formatDate(date: number): string

// 实现签名：参数类型取所有重载的并集，内部自己收窄
function formatDate(date: string | Date | number): string {
  return new Date(date).toLocaleDateString('zh-CN')
}

formatDate('2026-01-01')  // ✅
formatDate(new Date())    // ✅
formatDate(true)          // ❌ 没有任何重载匹配 boolean
```

一个真实世界的例子就是 Axios 的泛型方法签名（简化）：

```ts
get<T = any, R = AxiosResponse<T>>(url: string, config?): Promise<R>
```

两个类型参数 `T`（业务数据类型）与 `R`（最终返回类型）配合，让「我发一个请求，拿到一个信封」这种模式能被精确描述——4.13 节实战会用到这个细节。

### 4.4.5 this 与 noImplicitThis

`this` 是 JS 的老大难：指向谁取决于「怎么被调用」而不是「写在哪」。TS 在 strict 模式下开启 `noImplicitThis`：**凡是 TS 推断不出 `this` 指向的地方，直接报错**，把运行时事故变成编译期错误：

```ts
const counter = {
  count: 0,
  up() { this.count++ },          // ✅ 对象方法，this 是 counter
  delayed() {
    setTimeout(function () {
      this.count++                // ❌ noImplicitThis：这里的 this 是 unknown
    }, 1000)
  },
  delayedFixed() {
    setTimeout(() => {
      this.count++                // ✅ 箭头函数捕获外层 this
    }, 1000)
  }
}
```

实践守则三条：

1. 对象方法里要用 this，就用普通函数语法（`up() {...}`），让 TS 能推断；
2. 回调里需要外层 this，用**箭头函数**；
3. 实在复杂的场景（事件处理器、类），宁可先 `const self = this` 或显式绑定，也别留 `any` 的 this。

### 4.4.6 void 返回值的一个特殊规则

把「返回值是 void 的函数类型」赋给「有返回值的函数」是允许的：

```ts
type Handler = () => void

const h: Handler = () => 123   // ✅ 允许！调用方约定「我不看返回值」
```

语义是「调用方声明不消费返回值」，所以实现者返回什么都行。这个规则让 `arr.forEach(...)` 之类的回调既能返回值又不破坏约定。反过来（把「必返回 string」赋给「void」位置之外的地方）则不行。

---

## 4.5 类

### 4.5.1 传统写法回顾

JS 的 class 语法 TS 全盘继承：

```ts
class Post {
  title: string          // 字段声明
  views = 0              // 带初值，类型推断为 number

  constructor(title: string) {
    this.title = title
  }

  summarize(): string {
    return `${this.title}（${this.views} 次浏览）`
  }
}
```

TS 在此之上的增强，才是本章重点。

### 4.5.2 访问修饰符：public / private / protected

```ts
class Account {
  public username: string      // 默认就是 public，一般不写
  private passwordHash: string // 只类内部可访问
  protected role: string       // 类内部 + 子类可访问

  constructor(username: string, passwordHash: string, role: string) {
    this.username = username
    this.passwordHash = passwordHash
    this.role = role
  }
}

const acc = new Account('zora', 'xxx', 'admin')
acc.username          // ✅
acc.passwordHash      // ❌ Property 'passwordHash' is private
```

要点：

- 这是**编译期检查**，编译产物里没有任何「私有」——运行时想访问照样能访问。真想运行时也拦住，用 JS 原生的 `#` 私有字段（`#passwordHash`），TS 同样支持；
- `private` 不是安全机制，是**沟通机制**：告诉同事「这个字段是内部实现，别碰」。

### 4.5.3 readonly：只读成员

```ts
class Config {
  readonly baseUrl: string
  constructor(baseUrl: string) {
    this.baseUrl = baseUrl
  }
  update() {
    this.baseUrl = '/api2'   // ❌ Cannot assign to 'baseUrl' because it is read-only
  }
}
```

初始化（声明处或构造器内）之外不许赋值。适合放配置、ID 这类「生下来就不该变」的数据。

### 4.5.4 参数属性：一行顶四行

构造器参数前加修饰符，TS 会**自动生成同名字段并赋值**：

```ts
class Config {
  constructor(
    public readonly baseUrl: string,
    private timeout: number
  ) {}   // 不用手写 this.baseUrl = baseUrl
}
```

等价于 4.5.2 里手写的完整版。`readonly` 和 `public/private/protected` 可以组合。SelfBlog 这类函数式风格为主的 Vue 前端里类写得不多，但后端 Java 世界全是类——学这一节顺便理解 Java 的字段封装，一举两得。

### 4.5.5 存取器 get / set

```ts
class Temperature {
  private celsius = 0
  get fahrenheit(): number { return this.celsius * 1.8 + 32 }
  set fahrenheit(value: number) { this.celsius = (value - 32) / 1.8 }
}
```

getter/setter 让「字段」和「计算属性」对调用方长得一样，内部实现随便换。

### 4.5.6 抽象类：半成品图纸

`abstract` 类不能直接 `new`，只能被继承；它声明「子类必须实现什么」：

```ts
abstract class StorageAdapter {
  abstract get(key: string): string | null   // 只声明，不实现
  abstract set(key: string, value: string): void

  // 也可以有已实现的公共逻辑，子类直接继承
  remove(key: string): void {
    this.set(key, '')
  }
}

class LocalStorageAdapter extends StorageAdapter {
  get(key: string): string | null { return localStorage.getItem(key) }
  set(key: string, value: string): void { localStorage.setItem(key, value) }
}
```

比喻：普通类是「施工图」，照着就能盖房子；抽象类是「半成品图纸」——框架结构画好了，几间房留白，标着「继承者必须自己设计」。SelfBlog 后端 Spring 里大量 Service 实现继承同一个 `ServiceImpl` 基类，就是同样的思想。

### 4.5.7 implements：类必须满足某个接口

`extends` 是「继承实现」，`implements` 是「立军令状」——我保证长这个样：

```ts
interface Serializable {
  serialize(): string
}

class Post implements Serializable {
  constructor(public title: string) {}
  serialize(): string { return JSON.stringify({ title: this.title }) }
  // 如果忘写 serialize，编译立刻报错
}
```

可以同时 `extends Xxx implements A, B`。**接口负责「对外承诺」，抽象类负责「共享实现」**，两者经常配合使用。

### 4.5.8 一个相关配置：useDefineForClassFields

SelfBlog 的 `tsconfig.json` 里有一行 `"useDefineForClassFields": true`。它控制类字段用 ES2022 的 `Object.defineProperty` 语义（「定义」）还是老式的赋值语义（「设置」）。差别在「子类字段覆盖父类同名字段」「字段与 getter/setter 同名」这类边角场景才显现，日常写业务几乎感知不到——知道它是「让类字段行为符合最新 JS 标准」的开关即可，别删它，Vue 3 依赖这个行为。

---

## 4.6 泛型

### 4.6.1 为什么需要泛型：模具思想

先看一个不够用的函数：

```ts
function identity(value: number): number {
  return value
}
```

只支持 number。想支持 string 就再写一个？想支持所有类型就上 `any`？——`any` 会把「进去是什么类型、出来还是那个类型」这条信息抹掉：

```ts
function identityAny(value: any): any {
  return value
}
const n = identityAny(42)
n.toFixed()   // ❌ 不报错！n 是 any，检查全关。运行时万一是 string 就炸了
```

泛型解决的就是这个问题：**类型参数化**——把类型当参数传进去，进出关系被完整保留：

```ts
function identity<T>(value: T): T {
  return value
}

const a = identity(42)        // T 被推断为 number，a: number
const b = identity('hello')   // T 被推断为 string，b: string
```

比喻：`any` 版函数是「黑箱子，扔进啥出啥，出来是不是原来的东西不保证」；泛型函数是「模具」——扔进圆形的料，出来的保证是圆形。**泛型的本质：让「输入类型决定输出类型」这条链路在类型系统里活下来。**

### 4.6.2 泛型函数的常见形态

```ts
// 多个类型参数
function pair<A, B>(first: A, second: B): [A, B] {
  return [first, second]
}

// 类型参数不必出现在参数里，可显式指定
function typedArray<T = string>(length: number): T[] {
  return Array.from({ length })
}

// 约束（下一小节）
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key]
}
```

SelfBlog 里你能找到的真实泛型函数是 `utils/crypto.ts` 的 `decodeData<T>`（`stores/user.ts:103` 的用法）：

```ts
// 解码存储数据时显式告诉 TS：解开之后应当是什么类型
const decodedUser = decodeData<User>(savedUser)
```

### 4.6.3 泛型接口

这是 SelfBlog 前端最核心的泛型实践——统一响应信封：

```ts
// frontend/src/types/common.ts
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}
```

后端所有接口都返回这个信封，变化的只有 `data`：

```ts
const res1: ApiResponse<Post> = await postApi.getPostById(1)
//   res1.data 的类型是 Post，编辑器补全 title/content/...

const res2: ApiResponse<void> = await postApi.deletePost(1)
//   res2.data 是 void（删除操作没有业务数据）
```

配套的分页壳也是泛型：

```ts
export interface PaginatedData<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export type PaginatedResponse<T> = ApiResponse<PaginatedData<T>>
```

`PaginatedResponse<Post>` 一层套一层读出来就是：`{ code, message, data: { records: Post[], total, ... } }`。**泛型的可组合性**在这里体现得淋漓尽致。

### 4.6.4 泛型类

```ts
class ResultBox<T> {
  constructor(private value: T) {}
  map<U>(fn: (v: T) => U): ResultBox<U> {
    return new ResultBox<U>(fn(this.value))
  }
  get(): T { return this.value }
}

const box = new ResultBox(42).map(n => n.toString())
box.get().toUpperCase()   // ✅ get() 返回 string
```

`map` 返回 `ResultBox<U>`：链条上每一步的类型变换都被记录。Spring 后端的 `Result<T>` 响应类与之同构。

### 4.6.5 泛型约束 extends：给类型参数划底线

裸的 `<T>` 上，TS 只知道「是某种类型」，什么都不许做。约束（constraint）告诉它「至少要有这些成员」：

```ts
function logLength<T extends { length: number }>(value: T): T {
  console.log(value.length)   // ✅ 有约束，TS 知道一定有 length
  return value
}

logLength('hello')      // ✅ string 有 length
logLength([1, 2, 3])    // ✅ 数组有 length
logLength(42)           // ❌ number 没有 length
```

最常见的约束搭档是 `keyof`——「T 的键的联合」，保证传入的 key 一定合法：

```ts
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key]
}

const post: Post = { id: 1, title: 'TS', /* ... */ }
getProperty(post, 'title')   // ✅ 返回 string
getProperty(post, 'titles')  // ❌ 'titles' 不是 Post 的键，编译期就被抓住
```

`T[K]` 叫**索引访问类型**，返回「T 里 K 这个键对应的值类型」——参数类型和返回类型联动起来了。

### 4.6.6 常用泛型工具类型（必须背下来的六个）

TS 内置了一批工具类型，全部基于泛型实现。以下六个在业务代码里出现频率最高：

**Partial\<T>** —— 所有属性变可选。典型场景：「更新接口只传要改的字段」：

```ts
interface Post { id: number; title: string; content: string; tags: string[] }

type PostPatch = Partial<Post>
// { id?: number; title?: string; content?: string; tags?: string[] }

function updatePost(id: number, patch: PostPatch) { /* ... */ }
updatePost(1, { title: '新标题' })   // ✅ 只传一个字段也合法
```

**Required\<T>** —— 反过来，所有属性变必填（把 `?:` 摘掉）。

**Pick\<T, K>** —— 从 T 里**挑**出 K 里的键：

```ts
type PostPreview = Pick<Post, 'id' | 'title'>
// { id: number; title: string }
```

列表页只需要缩略字段时用它，别把整个 Post 抱着到处跑。

**Omit\<T, K>** —— 从 T 里**剔除** K 里的键，剩下全要：

```ts
type CreatePostInput = Omit<Post, 'id' | 'views' | 'createdAt'>
// id/views/createdAt 由服务端生成，创建时本来就不该传
```

`Pick` 和 `Omit` 互为镜像：键少用 `Pick`（白名单），键多用 `Omit`（黑名单）。

**Record\<K, V>** —— 键集合为 K、值类型为 V 的字典：

```ts
const statusText: Record<number, string> = {
  0: '草稿',
  1: '已发布'
}
// 更妙的是键也可以是字面量联合，逼你写全：
type PostStatus = 'draft' | 'published'
const map: Record<PostStatus, string> = { draft: '草稿', published: '已发布' }
// 少写一个键都会报错——Record + 字面量联合 = 「必须写全的映射表」
```

**ReturnType\<F>** —— 拿到函数 F 的返回类型：

```ts
function createDefaultPost() {
  return { title: '', content: '', tags: [] as string[] }
}
type NewPost = ReturnType<typeof createDefaultPost>
// { title: string; content: string; tags: string[] }
```

`typeof` 在这里取「值的类型」，`ReturnType` 再从函数类型里抽出返回值类型。**当类型可以「从实现推导」时，别手抄一遍**——实现改了，类型自动跟着变。

**先声明类型还是先写实现？** 经验：对外契约（API 层、组件 props、store 的公开状态）**先写类型**；内部工具函数**先写实现，再用工具类型导出**。两种方向都要会。

---

## 4.7 类型收窄与类型守卫

联合类型（`string | number`、`Post | null`）给了表达力，但也带来问题：拿到一个 `Post | null`，什么都能做吗？当然不能——**收窄（narrowing）**就是 TS 根据你写的检查逻辑，自动把联合类型「缩小」到具体分支的能力。这是 strict 模式下写代码的基本功。

### 4.7.1 typeof 收窄

```ts
function format(value: string | number): string {
  if (typeof value === 'string') {
    return value.trim()        // 这个分支里 value: string
  }
  return value.toFixed(2)      // 这个分支里 value: number
}
```

TS 分析控制流，`if` 里 `typeof` 判断过后，类型就按判断结果收窄；进入 `else`（或 if 之后），则收窄为剩下的联合成员。这套「控制流分析」同样作用于 `return`、`throw`、`break`：

```ts
function get(id: number): Post | null {
  const post = cache.get(id)
  if (post === null) throw new Error('文章不存在')
  return post       // ✅ 已排除 null，这里就是 Post
}
```

### 4.7.2 真值收窄与可选链

`if (value)` 会把 `null`、`undefined`、`''`、`0`、`false`、`NaN` 全部排除——方便但危险：`0` 和 `''` 是合法业务值时会被误伤。**有明确语义就用明确判断**：

```ts
// SelfBlog stores/user.ts 的真实写法
const isAdmin = computed(() => user.value?.role === 'admin')
```

`?.`（可选链）在 `user.value` 为 `null/undefined` 时短路返回 `undefined`，而不是抛错。它和 `??`（空值合并：仅在前值为 `null/undefined` 时取后值）是天作之合：

```ts
const nickname = user.value?.nickname ?? user.value?.username ?? '匿名'
```

对比一个典型误用：`count?.toString() ?? '0'`，当 `count` 为 `0` 时是安全的（`0?.toString()` 正常返回 `'0'`），但如果写成 `count || '0'`，`0` 也会被替换——语义不同，选择要谨慎。

### 4.7.3 相等与字面量收窄

```ts
type Status = 'loading' | 'success' | 'error'

function render(status: Status): string {
  if (status === 'loading') return '加载中…'
  if (status === 'success') return '成功'
  return '失败'    // 剩下的只可能是 'error'
}
```

字面量联合在比较中被逐个排除。这就是 4.2.4 说「字面量联合优于枚举」的底气之一：**收窄分析对它完全友好**。

### 4.7.4 in 与 instanceof

```ts
// in：检查属性存在性
interface Bird { fly(): void }
interface Fish { swim(): void }

function move(animal: Bird | Fish) {
  if ('fly' in animal) animal.fly()
  else animal.swim()
}

// instanceof：检查类的原型
function handleError(err: unknown) {
  if (err instanceof AxiosError) {
    console.log(err.response?.status)   // 收窄为 AxiosError，有 response 属性
  }
}
```

### 4.7.5 自定义守卫：`is` 谓词

当收窄逻辑复杂或需要复用时，抽成一个函数——但普通的 boolean 返回值**不会**让 TS 收窄，需要类型谓词 `value is Type`：

```ts
// 普通函数：TS 不知道 true 意味着什么
function isPost(value: unknown): boolean {
  return typeof value === 'object' && value !== null && 'title' in value
}

// 类型守卫：true ⇒ value 收窄为 Post
function isPost(value: unknown): value is Post {
  return typeof value === 'object' && value !== null && 'title' in value
}

function show(data: unknown) {
  if (isPost(data)) {
    console.log(data.title)   // ✅ 收窄为 Post
  }
}
```

`is` 谓词本质是「**我向编译器承诺：这个函数返回 true 时，参数一定是这个类型**」——承诺是否兑现，编译器不验证（函数体里的逻辑是你自己的事），所以守卫函数要写得老老实实。

SelfBlog 的 `api/index.ts:93` 里有个近亲——用结构类型参数代替 `is`：

```ts
function isRetryableError(error: { code?: string; response?: unknown; message?: string }): boolean {
  if (error.code === 'ERR_CANCELED') return false
  return !error.response && (/* ... */)
}
```

它不收窄类型，只复用「判断逻辑」；调用方传进来的 AxiosError 天然满足这个结构。什么时候用 boolean、什么时候用 `is`？**需要收窄时用 `is`，只需要复用判断逻辑时用 boolean**。

### 4.7.6 可辨识联合（Discriminated Union）：状态机建模神器

如果联合的每个成员都有一个**字面量类型的公共键**（discriminant，判别键），TS 的收窄会精确到「一键切换分支」：

```ts
type RequestState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; message: string }

function view<T>(state: RequestState<T>): string {
  switch (state.status) {
    case 'idle':    return '待开始'
    case 'loading': return '加载中'
    case 'success': return `数据：${JSON.stringify(state.data)}`  // 只有这里有 data
    case 'error':   return state.message                          // 只有这里有 message
  }
}
```

`state.status` 取值互斥，每个分支里 TS 只给你该分支的成员——`loading` 分支里写 `state.data` 直接编译报错。**把「不同状态携带不同数据」建模成可辨识联合，非法状态在类型上就不可能存在**，这比「一个对象上 data 和 error 都可空，用时再各查一遍」安全得多。SelfBlog 相邻文章接口的返回（`prev/next` 各自可为 `null`，见 `api/post.ts:49`）就是靠 `| null` 明确表达「可能没有」，配合 `?.` 安全消费。

---

## 4.8 高级类型

这一节的工具平时用不上，读别人的类型定义（Vue、Axios 的 `.d.ts`）时全靠它们。目标是**看懂**，其次才是写。

### 4.8.1 联合类型与交叉类型

- **联合 `|`**：「或」——`string | number`，取值可以是其一；
- **交叉 `&`**：「且」——`A & B`，同时具备 A 和 B 的全部成员：

```ts
interface Loggable { log(): void }
interface Serializable { serialize(): string }

type Service = Loggable & Serializable
```

交叉类型常被当成「interface 的 extends 的 type 版本」；两个接口有同名但类型不同的属性时，交叉结果该属性变成 `never`，算是一个警告信号。

### 4.8.2 字面量类型：把值当类型

字符串/数字字面量本身可以当类型，只允许完全相等的值：

```ts
type Direction = 'up' | 'down' | 'left' | 'right'
type Port = 8825            // SelfBlog 开发后端端口
type HttpCode = 200 | 401 | 403 | 404 | 429 | 500

function goto(dir: Direction) { /* ... */ }
goto('up')    // ✅
goto('Up')    // ❌ 大小写都不放过
```

单看没多大用，组合起来威力巨大——`Record<PostStatus, string>` 必须写全映射、可辨识联合的判别键、模板字面量类型的原料，全是它。

### 4.8.3 索引访问类型

在**类型层面**做下标操作，取出子类型：

```ts
type PostTitle = Post['title']            // string
type PostTagList = Post['tags']           // string[]

// 对数组/元组用 number 索引，取出元素类型
type PostListItem = Post[][number]        // Post

// SelfBlog 真实用例（api/index.ts:97）：
// RETRYABLE_CODES 是一个 as const 的字符串数组常量，
// (typeof RETRYABLE_CODES)[number] 取出「它的所有元素构成的联合类型」
const codes: (typeof RETRYABLE_CODES)[number][] = ['ECONNABORTED', 'ETIMEDOUT']
```

`(typeof X)[number]` 这个组合值得背下来：**从常量数组推导出「合法值」的联合类型**，常量加一项，类型自动跟着变。

### 4.8.4 条件类型

类型层面的三元表达式：

```ts
type IsString<T> = T extends string ? true : false

type A = IsString<'hello'>   // true
type B = IsString<42>        // false
```

裸类型参数上的条件类型是**分布式**的——联合类型会被拆开逐个判断再重新合并：

```ts
type ToArray<T> = T extends any ? T[] : never
type R = ToArray<string | number>   // string[] | number[]（不是 (string|number)[]！）
```

这个特性可以被利用（把联合「洗」成想要的形状），也可以用方括号包起来禁用：`[T] extends [any]`。

### 4.8.5 infer：在条件类型里「捕获」类型

`infer` 在 `extends` 的模式里声明一个「待捕获」的占位符，匹配成功时捕获到的类型可以在 true 分支使用：

```ts
// 内置 ReturnType 的简化实现
type MyReturnType<T> = T extends (...args: any[]) => infer R ? R : never

// 捕获 Promise 的内部类型
type Awaited<T> = T extends Promise<infer V> ? V : T
type Data = Awaited<Promise<Post>>   // Post
```

读法：「如果 T 长得像一个函数（或 Promise），就把它的返回值（内部值）命名为 R（V），然后……」。所有内置工具类型的源码都是条件类型 + infer 的组合，读懂这一对，`type-fest` 这类类型库就不再是天书。

### 4.8.6 映射类型与 as 重映射

「对类型的每个键做同样的变换」：

```ts
type Optional<T> = { [K in keyof T]?: T[K] }        // 这就是 Partial 的实现
type ReadonlyVersion<T> = { readonly [K in keyof T]: T[K] }

// as 子句：映射时改键名
type Getters<T> = {
  [K in keyof T as `get${Capitalize<string & K>}`]: () => T[K]
}
type PostGetters = Getters<Pick<Post, 'id' | 'title'>>
// { getId: () => number; getTitle: () => string }
```

### 4.8.7 模板字面量类型

类型层面的字符串拼接，由字面量联合驱动：

```ts
type EventName = 'click' | 'focus'
type HandlerName = `on${Capitalize<EventName>}`
// 'onClick' | 'onFocus'

// 约束路由路径
type ApiPath = `/post/${number}` | `/tag/${number}`
function fetchApi(path: ApiPath) { /* ... */ }
fetchApi('/post/1')    // ✅
fetchApi('/user/1')    // ❌
```

Vue 源码里大量使用它约束组件事件名与插槽名。日常业务写得少，认得出即可。

---

## 4.9 装饰器

### 4.9.1 装饰器是什么

装饰器（Decorator）是一种「附加到类/方法/属性/参数上的特殊语法」，在**声明处**给目标添加元数据或包装行为：

```ts
function readonly(target: unknown, key: string) { /* ... */ }

class Article {
  @readonly
  id = 1
}
```

要启用 TS 的传统装饰器（ SelfBlog 这种 Vue 项目里其实不开，见 4.9.4），需要在 `tsconfig.json` 配置：

```jsonc
{
  "compilerOptions": {
    "experimentalDecorators": true,
    "emitDecoratorMetadata": true   // 设计元数据（配合 reflect-metadata，Spring 风格依赖注入需要）
  }
}
```

带 `experimental` 前缀是因为它当年基于还没定稿的提案。TS 5.0 起也支持了 ECMAScript 正式标准的装饰器（Stage 3），两者语法与能力有差异，本文按国内后端生态（NestJS/Spring 风格）常用的**实验性装饰器**讲解。

### 4.9.2 四种装饰器速览

**类装饰器**——拿到类构造函数，可以修改/替换它：

```ts
function Controller(path: string): ClassDecorator {
  return (target) => {
    // 把路由前缀记录到类的元数据里
    Reflect.defineMetadata('basePath', path, target)
  }
}

@Controller('/post')
class PostController {}
```

**方法装饰器**——拿到原型、方法名、属性描述符，可以包一层（日志、权限、限流）：

```ts
function Log(): MethodDecorator {
  return (target, key, descriptor) => {
    const original = descriptor.value as (...args: unknown[]) => unknown
    descriptor.value = function (...args: unknown[]) {
      console.log(`调用 ${String(key)}`, args)
      return original.apply(this, args)
    }
  }
}

class PostService {
  @Log()
  delete(id: number) { /* ... */ }
}
```

**属性装饰器 / 参数装饰器**——分别拿到「挂在哪个类的哪个属性/第几个参数」，通常只用来记录元数据（配合 `emitDecoratorMetadata`，供 IoC 容器反射使用）：

```ts
class PostService {
  @Autowired
  private mapper!: PostMapper       // 属性装饰器：标记「这里要注入」

  list(@Param('id') id: number) {}  // 参数装饰器：标记参数来源
}
```

### 4.9.3 和 Spring 注解对照

如果你完成了第三章，下面的对照表会让你恍然大悟——**Spring 的注解和 TS 装饰器解决的是同一类问题：在不污染业务代码的前提下，把横切关注点（路由、鉴权、日志、注入）声明上去**：

| Spring（Java 注解，运行时反射） | NestJS/TS（装饰器） | 作用 |
|------|------|------|
| `@RestController("/post")` | `@Controller('post')` | 声明控制器与路由前缀 |
| `@GetMapping("/list")` | `@Get('list')` | 方法级路由 |
| `@Autowired` / 构造器注入 | `@Inject()` | 依赖注入 |
| `@Transactional` | `@Transactional()` | 事务/切面 |
| `@Cacheable("hotTags")` | `@Cacheable('hotTags')` | 缓存 |

差别在于实现机制：Java 注解是**运行时反射**读取的；TS 装饰器是**编译期**执行的真实 JS 函数（配合 reflect-metadata 在运行时存取元数据）。

### 4.9.4 SelfBlog 前端为什么不用装饰器？

这是个重要的架构决定，理由有四：

1. **Vue 3 的 Composition API 时代，装饰器已非主流**。Vue 2 + class-component 时代装饰器（`@Component`、`@Prop`）流行过；Vue 3 的 `<script setup>` 用**编译器宏**（`defineProps`、`defineEmits`）替代了装饰器的角色——宏是编译器内置的，无需任何 flag，类型推断还更好（4.12 节）。
2. **状态管理用 Pinia 的 setup store**（函数 + `ref`/`computed`），不需要 class + `@Inject` 式的依赖注入。
3. **装饰器仍是 `experimental`**，标准与实验版并存，混用有迁移成本；项目没有需要装饰器才能解决的场景。
4. **可读性**：装饰器把行为「藏」进声明处，对不熟悉的读者是隐式魔法；显式代码（组合式函数、普通函数调用）更符合 SelfBlog 前端「一眼看懂」的风格。

结论：**前端不用，但要看得懂**——一是将来读 NestJS 式的 TS 后端、二是理解 Spring 注解的「声明式」思想时，装饰器都是绕不开的一课。

---

## 4.10 tsconfig 深入：SelfBlog 的严格模式实战

现在是本章分量最重的一节。先完整看一遍 SelfBlog 前端的 `frontend/tsconfig.json`：

```jsonc
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "ignoreDeprecations": "6.0",

    /* Bundler mode */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "preserve",

    /* Linting */
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true,

    /* Path aliases */
    "baseUrl": ".",
    "paths": { "@/*": ["src/*"] },

    /* Vue */
    "types": ["vite/client"]
  },
  "include": [
    "src/**/*.ts", "src/**/*.tsx", "src/**/*.vue", "src/**/*.d.ts", "env.d.ts"
  ],
  "exclude": ["node_modules", "dist"]
}
```

### 4.10.1 strict 全家桶

`"strict": true` 不是单一检查，而是一个开关组，打开后默认启用以下项目（了解每个的名字，报错时能对号入座）：

| 成员 | 作用 | 一句话 |
|------|------|--------|
| `noImplicitAny` | 禁止悄悄的 any | 推断不出的地方必须显式标注 |
| `strictNullChecks` | null/undefined 不再混入其他类型 | 「可能为空」必须显式建模 |
| `strictFunctionTypes` | 函数参数双向协变 → 严格逆变 | 回调参数类型更严谨 |
| `strictBindCallApply` | bind/call/apply 参数检查 | 不许乱传参 |
| `strictPropertyInitialization` | 类字段必须初始化 | 构造器里没赋值的非可选字段报错 |
| `noImplicitThis` | this 类型不明时报错 | 见 4.4.5 |
| `alwaysStrict` | 产物顶层加 "use strict" | —— |
| `useUnknownInCatchVariables` | catch 的 err 是 unknown 而非 any | 处理错误前必须收窄 |

`strictPropertyInitialization` 常坑新手：

```ts
class User {
  name!: string        // ! 非空断言：我保证构造后（或靠装饰器）一定赋值
  age?: number         // 声明为可选也行
  constructor() {
    // name 若既没赋值也没 ! 或 ?，这里报 TS2564
  }
}
```

`useUnknownInCatchVariables` 值得单独点赞：`catch (e)` 里的 `e` 不再是 any，而是 unknown——你被迫先 `e instanceof Error` 收窄再读 `message`，避免「以为捕获的是 Error 结果是字符串」的经典事故。

### 4.10.2 重点：noUncheckedIndexedAccess

SelfBlog 在 strict 之外**额外**开启了这一项（它不在 strict 组里）。它改变的是一条最容易被忽视的规则：**通过索引（下标/键）访问得到的值，类型自动加上 `| undefined`**。

先看默认行为（不开这个选项）有多坑：

```ts
const arr = ['a', 'b', 'c']
arr[10].toUpperCase()
// 默认：✅ 编译通过（TS 认为 arr[10] 是 string）
// 运行时：💥 Cannot read properties of undefined (reading 'toUpperCase')
```

TS 的默认假设是「下标一定有效」——这显然是谎言。开启 `noUncheckedIndexedAccess` 后，类型系统终于说了实话：

```ts
const arr: string[] = ['a', 'b', 'c']
const item = arr[0]
// item: string | undefined   ← 不再骗你

item.toUpperCase()         // ❌ TS18048 / TS2532：'item' is possibly 'undefined'
arr[0].foo                 // ❌ 同理，arr[0].foo 编译不过！
```

它对**对象索引**同样生效：

```ts
const map: Record<string, number> = { a: 1 }
const v = map['b']        // number | undefined（键可能不存在）
```

而循环遍历**不受影响**——`for...of` / `for...of index` 遍历到的是「确实存在的元素」：

```ts
for (const s of arr) console.log(s.length)   // ✅ s: string，没有 undefined
arr.forEach((s) => console.log(s.length))    // ✅ 同理
```

**正确写法清单**（SelfBlog 仓库里都能找到原型）：

```ts
// ① 可选链：允许「没有就 undefined」继续传递
const name = posts[0]?.title

// ② 先守卫再使用
const first = posts[0]
if (first) {
  console.log(first.title)
}

// ③ 空值合并给默认值
const page = Number(route.query.page ?? 1)

// ④ 断言前先自己验证——SelfBlog stores/user.ts 的真实代码：
const parts = token.value.split('.')
if (parts.length === 3) {
  // 已经验证了长度，这里的 ! 非空断言才有底气
  const payload = JSON.parse(atob(parts[1]!.replace(/-/g, '+').replace(/_/g, '/')))
}
```

注意第 ④ 条：`!`（non-null assertion）不是「消除问题」，而是「我对编译器发誓这里不是 undefined」——发誓前必须先有验证逻辑，否则就是把运行时崩溃换了个地方。**能用收窄（①②③）就别用断言（④）**。

这个选项初学时会觉得「处处报错、寸步难行」，但坚持一周后你会发现：项目里「Cannot read properties of undefined」这类运行时崩溃几乎绝迹。SelfBlog 全仓库在 `type-check` 闸门下保持零 `undefined` 事故，这个开关功不可没。

### 4.10.3 其他 Linting 开关

```jsonc
"noUnusedLocals": true,             // 声明了没用的局部变量报错（TS6133）
"noUnusedParameters": true,         // 声明了没用的函数参数报错（想保留用 _ 前缀）
"noFallthroughCasesInSwitch": true  // case 穿透报错（漏写 break）
```

三个都是「把粗心扼杀在编译期」的消防栓，配合 ESLint 形成双保险。

### 4.10.4 Bundler 模式相关

```jsonc
"moduleResolution": "bundler",      // 按「打包器视角」解析模块：支持 package.json exports、
                                    // 无扩展名导入等 Vite 时代的新玩法
"allowImportingTsExtensions": true, // 允许 import xxx from './a.ts'（仅限 noEmit 项目）
"isolatedModules": true,            // 每个文件必须能被单独转译——这是 esbuild 模式的要求：
                                    // 所以跨模块「只导类型」必须写 import type（见 4.11）
"moduleDetection": "force",         // 所有文件都当模块处理，避免全局变量互相污染
"noEmit": true,                     // tsc 只检查不产出（转译交给 Vite）
```

`isolatedModules` 与 `import type` 的关系要刻进肌肉记忆：esbuild 转译单个文件时**看不到其他文件**，无法知道 `import { Post } from '@/types'` 里的 Post 是「类型」还是「真值」；写 `import type` 明确告诉它「编译时删掉这一行」。SelfBlog 所有类型导入都遵循此规范（如 `api/post.ts` 顶部的 `import type { ApiResponse, ... } from '@/types'`）。

### 4.10.5 路径别名与 include

```jsonc
"baseUrl": ".",
"paths": { "@/*": ["src/*"] }       // '@/stores/user' → 'src/stores/user'
```

别名让深层 import 不再 `../../..` 地爬楼梯。注意 `paths` 只影响 **tsc/vue-tsc** 的类型解析；运行时路径替换由 **Vite** 的 `resolve.alias` 负责——两边都要配，缺一边就会出现「编辑器补全正常、构建报错」或反之的诡异现象。`"types": ["vite/client"]` 则把 Vite 的全局类型（`import.meta.env` 等）纳入检查，`include` 把 `.vue` 文件圈进类型检查范围——这正是 `vue-tsc` 存在的理由。

---

## 4.11 模块、命名空间与声明文件

### 4.11.1 ES 模块与 type-only 导入

现代前端模块体系就是 ES Module：`export` / `import`。TS 只加了两个语法糖：

```ts
// 导出一个类型
export interface Post { /* ... */ }

// 只导入类型：编译后整行消失
import type { Post } from '@/types'

// 混合导入：类型和值各归各
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
//            ^^^^^^^^ 值导入        ^^^^ 类型导入
```

这正是 `api/index.ts:5` 的第一行。`import type` 不是风格洁癖，而是 `isolatedModules` 环境下的**正确性要求**（4.10.4 已解释）。

### 4.11.2 命名空间：基本退役

`namespace` 是 ES 模块普及前 TS 的代码组织方案（`namespace Utils { export function a() {} }`），在模块化项目里**没有用武之地**——模块本身就是隔离单元。今天它只活在这些地方：旧代码迁移、`declare global` 声明全局类型、以及某些以全局脚本方式加载的库的 `.d.ts` 里。SelfBlog 全项目零 namespace，见到时知道是「老古董」即可。

### 4.11.3 声明文件 .d.ts：类型的外交部

`.d.ts` 是**只含类型、不含实现**的文件。三种典型场景：

**① 给 JS 库补类型**。npm 包普遍自带类型；若某个库没有（控制台警告 `Could not find a declaration file for module 'xxx'`），可以装社区维护的 `@types/xxx`，实在没有就自己写：

```ts
// src/types/untyped-lib.d.ts
declare module 'untyped-lib' {
  export function render(el: HTMLElement, options?: { theme?: string }): void
}
```

**② 声明非 JS 资源模块**。SelfBlog 的 `frontend/env.d.ts` 里就有真实例子：

```ts
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}
```

它告诉 TS：「凡是 `import Xxx from './Xxx.vue'`，都当作一个 Vue 组件处理」。有了这段，`.vue` 导入才不报 TS2307。

**③ 扩充全局类型与环境类型**。`env.d.ts` 的后半部分给 `import.meta.env` 上了户口：

```ts
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TARGET?: string
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

之后代码里写 `import.meta.env.VITE_APP_TITLE`，编辑器精确补全这四个键；敲一个不存在的 `VITE_API_PORT` 立刻报错——**环境变量这种纯字符串配置也能被类型保护**。

### 4.11.4 三斜线指令与 declare

- **三斜线指令**：`.d.ts` 文件顶部的特殊注释，声明对其他类型包的依赖。`env.d.ts` 第一行 `/// <reference types="vite/client" />` 的意思是「把 vite/client 包的类型引入本文件」。常见指令就三种：`types`、`path`、`lib`，日常只需要认识它们。
- **`declare`**：声明「这个东西存在，但实现不在这」——`declare const VERSION: string`（全局变量）、`declare module 'x'`（模块形状）、`declare namespace`（老式全局对象）。它是「对外部世界的类型描述」的万能动词。

记忆锚点：**`.ts` 描述「我们的代码」，`.d.ts` 描述「别人的代码/环境」**。

---

## 4.12 TS 与 Vue 3 结合

SelfBlog 前端全部使用 `<script setup lang="ts">`。这一节把 Vue 3 里所有和类型打交道的 API 过一遍，全部用项目真实代码。

### 4.12.1 defineProps：泛型写法是唯一正解

声明 props 的类型，不用运行时对象（`{ title: String }`），用**编译器宏 + 泛型**：

```vue
<!-- components/pages/MarkdownContent.vue（真实代码，简化） -->
<script setup lang="ts">
defineProps<{ source: string }>()
</script>
```

需要默认值时配合 `withDefaults`——`ArticleList.vue:7` 的真实签名：

```ts
const props = withDefaults(
  defineProps<{
    posts: Post[]
    featured?: boolean
    heading?: 'h2' | 'h3'      // 字面量联合：连「用哪个标题层级」都被约束
  }>(),
  {
    featured: false,
    heading: 'h2'
  }
)
```

也可以把 props 类型抽成独立 interface（`Button.vue` 的 `ButtonProps`），复杂组件更整洁。要点：

- 泛型参数里写的**就是 TS 类型**：`Post[]`、`'h2' | 'h3'` 随便用；
- 可选属性 `featured?: boolean` 对应「父组件可以不传」；
- 父组件传错类型，`vue-tsc` 直接报错——模板里的类型检查也是真的；
- **类型检查替代不了运行时校验**：TS 类型编译后即消失，运行时传进来的还是 JS 值。涉及外部输入（URL 参数、localStorage）时，组件内仍需防御性检查。

### 4.12.2 defineEmits：把事件签名写成函数类型

```vue
<!-- components/common/ImagePreview.vue（真实代码） -->
<script setup lang="ts">
const emit = defineEmits<{ close: [] }>()
// close: [] —— 事件名 close，参数为空元组
</script>

<!-- components/layout/NavbarMusicControl.vue（真实代码） -->
<script setup lang="ts">
const emit = defineEmits<{
  toggle: [trigger: HTMLElement]   // 事件 toggle，携带一个 HTMLElement 参数
  close: []
}>()
</script>
```

这个「事件名: 参数元组」的写法是 Vue 3.3+ 的新语法，比旧的「函数签名风格」更直观。收益：`emit('togle')` 拼错立刻报错；父组件模板里 `@toggle="handler"` 的 handler 参数类型自动推断为 `HTMLElement`。

### 4.12.3 ref / computed / 响应式数据的类型

`ref` 会**自动推断**，但初始值是 `null/undefined` 时必须显式给类型参数：

```ts
// views/Home.vue（真实代码）
const posts = ref<Post[]>([])           // Ref<Post[]>，类型可省略（初值已表达）

// stores/user.ts（真实代码）
const token = ref<string | null>(null)  // 必须写 <string | null>，否则推断成 Ref<null>
const user = ref<User | null>(null)
```

`.value` 之后的使用，就回到 4.7 的收窄世界：`user.value?.role`、`posts.value[0]?.title`。`computed` 的类型从返回值自动推断，一般不用标注：

```ts
const isLoggedIn = computed(() => !!token.value && !!user.value)  // ComputedRef<boolean>
```

`reactive` 则要求**直接给初始值**（不能只给类型），类型由初值推断；需要「空壳 + 后填充」的模式时，让每个字段都容纳空值（`null` 或 `undefined`），别用 `as` 硬拗。

### 4.12.4 模板中的类型

`<template>` 里的表达式同样被 `vue-tsc` 检查：

```vue
<script setup lang="ts">
const props = defineProps<{ posts: Post[] }>()
</script>

<template>
  <!-- v-for 变量自动是 Post 类型 -->
  <article v-for="post in posts" :key="post.id">
    <!-- post.title 有补全；写成 post.titel 会报错 -->
    <h2>{{ post.title }}</h2>
    <!-- noUncheckedIndexedAccess 在模板里同样生效：posts[0] 是 Post | undefined -->
    <p>{{ posts[0]?.title }}</p>
  </article>
</template>
```

模板事件、插槽、组件 props 的类型都来自组件定义——类型定义写得越准，模板里白嫖的检查越多。

### 4.12.5 Pinia setup store 的类型

`stores/user.ts` 的 store 是「函数 + 响应式状态」构成，类型完全自动：

```ts
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)
  const isAdmin = computed(() => user.value?.role === 'admin')  // ComputedRef<boolean>

  function updateTokens(t: string, r: string) { /* ... */ }

  return { token, user, isAdmin, updateTokens }
})
export const useUserStore = useAuthStore   // 历史别名
```

调用方 `const store = useUserStore()` 之后，`store.token`、`store.isAdmin`、`store.updateTokens` 全部有精确类型——**setup store 几乎不需要手写类型，这是它优于 options store 的地方之一**。注意给 store 补类型时（如 `decodeData<User>(savedUser)`），引用的是 `@/types` 里的业务类型，而不是在 store 里重新发明。

### 4.12.6 vue-tsc：让类型检查覆盖 .vue

`tsc` 不认识 `.vue` 单文件组件，Vue 官方的 **vue-tsc** 是包了一层、能解析 SFC 的 TypeScript 编译器。SelfBlog 把它做成了两个 npm 闸门：

```bash
npm run type-check    # vue-tsc --noEmit：纯类型检查
npm run build         # vue-tsc --noEmit && vite build：先检查再打包
```

CI 和本地开发都以 `type-check` 全绿为底线。配合 `vitest`（测试也用 TS 写）、ESLint（typescript-eslint 规则），形成完整的前端质量闸门。

---

## 4.13 实战：用 TS 重写 SelfBlog 的 API 层

前面所有知识在这一节汇成一次完整的实战。目标：理解并仿写 `frontend/src/api/` 的类型体系——这也是本项目「TS 到底带来了什么」的最佳展品。

### 4.13.1 先立合同：后端统一响应信封

SelfBlog 后端（第三章）所有接口统一返回 `Result<T>{code, message, data, timestamp}` 信封：`code` 为 200 表示业务成功，非 200 业务失败；分页时 `data` 里是 `{records, total, pageNum, pageSize, pages}`。

前端的「合同文本」就是 `frontend/src/types/common.ts`：

```ts
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PaginatedData<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export type PaginatedResponse<T> = ApiResponse<PaginatedData<T>>
```

> 细节讨论：后端信封其实还有 `timestamp` 字段，前端 `ApiResponse` 没有声明它。TS 对「对象多出来的字段」是宽容的（结构化类型：声明了三个字段，实际对象多带一个 `timestamp` 完全合法），所以不声明不影响编译；但「合同」上写明会更完整——你也可以试着给它加上 `timestamp?: number`，体会「前端类型是后端契约的镜像」这句话。

### 4.13.2 核心契约：拦截器把 AxiosResponse 拆成信封

这是 SelfBlog API 层**最容易踩坑、也最能体现类型价值**的一条契约。看 `api/index.ts:174` 的响应拦截器：

```ts
api.interceptors.response.use(
  (response) => {
    logger.debug('Response success', { url: response.config.url, status: response.status })
    return response.data        // ★ 返回的是 AxiosResponse 的 data——后端信封本身！
  },
  /* 错误分支略 */
)
```

`response` 原本类型是 `AxiosResponse`（含 `status`、`headers`、`data` 等），拦截器把它**拆包**，只返回 `response.data`——即后端信封 `{code, message, data}`。这带来一个调用层的重要事实：

```ts
// ❌ 错误认知（Axios 默认行为）：
const res = await api.get('/post/1')
res.data.data.title    // 不是这样！res 已经是信封了

// ✅ 正确用法（SelfBlog 契约）：
const res = await api.get('/post/1')
res.data.title         // res 是信封 ApiResponse<Post>，res.data 才是业务数据
```

用类型语言重述：拦截器把 `Promise<AxiosResponse<T>>` 变成了 `Promise<ApiResponse<T>>`。**运行时行为变了，如果不改类型，类型就成了谎言**——接下来看类型怎么跟上。

### 4.13.3 Axios 泛型签名与「上下文推断」

Axios 的 `get`/`post` 等方法有个双泛型签名（简化）：

```ts
get<T = any, R = AxiosResponse<T>>(url: string, config?: AxiosRequestConfig): Promise<R>
```

`T` 是业务数据类型，`R` 是方法最终解析出的类型，**默认** `R = AxiosResponse<T>`。关键机制：`R` 支持**上下文推断**——当你显式声明了函数/变量的返回类型，`R` 会被反推为那个类型，而不是用默认值：

```ts
function getPostById(id: number): Promise<ApiResponse<Post>> {
  return api.get(`/post/${id}`)   // T 从 URL 侧拿不到，R 被上下文推成 ApiResponse<Post>
}
```

这行能编译通过且类型正确，靠的正是这个细节：拦截器已经在运行时改写了返回值，类型声明再由调用方补上，**运行时与类型系统重新对齐**。这也是一个通用的工程教训：**凡是「中间件/拦截器改写了返回形状」的地方，类型必须有人显式接住**，否则 TS 只能按默认假设撒谎。

### 4.13.4 带类型的请求方法：api/post.ts 精读

`frontend/src/api/post.ts` 是这个模式的完整落地，逐行精读：

```ts
import api from './index'
import type {
  ApiResponse, PaginatedResponse, Post,
  CreatePostRequest, UpdatePostRequest, PostListParams, PostSearchParams, Draft
} from '@/types'

export const postApi = {
  /** 创建文章：入参是「创建请求」类型，出参信封里装着 Post */
  createPost(data: CreatePostRequest): Promise<ApiResponse<Post>> {
    return api.post('/post', data)
  },

  updatePost(id: number | string, data: UpdatePostRequest): Promise<ApiResponse<Post>> {
    return api.put(`/post/${id}`, data)
  },

  /** 删除没有业务数据：ApiResponse 默认 T = unknown */
  deletePost(id: number | string): Promise<ApiResponse> {
    return api.delete(`/post/${id}`)
  },

  getPostById(id: number | string): Promise<ApiResponse<Post>> {
    return api.get(`/post/${id}`)
  },

  /** 相邻文章：内联定义嵌套结构，prev/next 都可能不存在 */
  getAdjacentPosts(id: number | string): Promise<
    ApiResponse<{
      prev: { id: number; title: string } | null
      next: { id: number; title: string } | null
    }>
  > {
    return api.get(`/post/${id}/adjacent`)
  },

  /** 列表：分页信封 */
  getPostList(params: PostListParams): Promise<PaginatedResponse<Post>> {
    return api.get('/post/list', { params })
  },

  saveDraft(data: CreatePostRequest): Promise<ApiResponse<Draft>> {
    return api.post('/post/draft', data)
  },

  getLatestDraft(): Promise<ApiResponse<Draft>> {
    return api.get('/post/draft/latest')
  },

  advancedSearch(params: PostSearchParams): Promise<PaginatedResponse<Post>> {
    return api.get('/post/search/advanced', { params })
  }
}
```

拆出三条设计规律，写任何项目的 API 层都适用：

1. **请求入参建类型**（`CreatePostRequest`、`PostListParams`）——「这个接口要什么」是契约的一半；
2. **返回值声明到信封的 `data` 泛型**（`ApiResponse<Post>`、`PaginatedResponse<Post>`）——「会还给你什么」是另一半；
3. **方法体一行**：URL 拼接 + 转发，逻辑全部沉淀在拦截器层，API 模块保持「类型密集、逻辑稀薄」。

### 4.13.5 教学版重写：把拦截器的类型讲透

真实 `api/index.ts` 有 396 行（token 刷新排队、指纹校验、重试、访客 ID……），不适合教学。下面抽出一个保留类型骨架的教学版，你可以对照源文件读：

```ts
import axios, {
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig
} from 'axios'
import type { ApiResponse } from '@/types'

const api: AxiosInstance = axios.create({
  baseURL: '/api',        // 硬编码 /api：开发靠 Vite 代理、生产靠 JAR 内嵌/vercel.json 转发
  timeout: 15000
})

// ── 请求拦截器：注入鉴权头 ────────────────────────────────
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = readToken()                       // 省略：store 优先、storage 兜底
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Visitor-Id'] = getVisitorId() // 匿名点赞/留言的访客标识
  config.headers['X-Trace-Id'] = generateTraceId()
  return config
})

// ── 响应拦截器：拆包 + 错误加工 ───────────────────────────
api.interceptors.response.use(
  (response) => response.data,                    // ★ 拆包：返回后端信封
  async (error: AxiosError<ApiResponse<unknown>>) => {
    // 给错误对象挂上「给用户看的文案」，类型上用声明合并扩展 AxiosError
    error.userMessage =
      error.response?.data?.message ?? fallbackMessage(error.response?.status)
    return Promise.reject(error)
  }
)

export default api
```

两个类型细节值得展开：

**① 扩展 AxiosError**。拦截器给 `error` 挂了 `userMessage` 属性，但 `AxiosError` 类型上没有它。真实项目里调用方仍要读它，于是需要一个类型扩展。最小方案是接口声明合并：

```ts
// types/axios.d.ts
import 'axios'

declare module 'axios' {
  export interface AxiosError {
    userMessage?: string
  }
}
```

之后全项目 `error.userMessage` 都有类型，编辑器可补全。这就是 4.3.7 表格里「interface 声明合并」的用武之地。

**② 401 刷新队列的类型**。Token 过期时，并发请求要排队等新 token——`failedQueue` 是一个「等待 promise 被外界 resolve」的队列，它的类型直接描述了这个协议：

```ts
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

const processQueue = (error: unknown, token: string | null = null): void => {
  failedQueue.forEach((prom) => {
    if (error) prom.reject(error)
    else prom.resolve(token!)   // processQueue(null, token) 时 token 必非空，! 有过程性保证
  })
  failedQueue = []
}
```

`resolve`/`reject` 的参数类型就是「这个 promise 最终交付什么」的合同——`resolve: (token: string) => void` 一眼看懂「队首等待者拿到的是新 token」。刷新成功后 `processQueue(null, newToken)`、失败 `processQueue(refreshError, null)`，两条路径的类型都严丝合缝。

### 4.13.6 错误处理类型：让 catch 里的世界也有类型

调用方怎么安全地处理错误？先看类型，再给范式：

```ts
// ① 后端业务失败（HTTP 200 但 code != 200）：
const res = await postApi.getPostById(1)
if (res.code !== 200) {
  showToast(res.message)     // res.message: string，有类型
  return
}
render(res.data)             // 走到这里 res.data: Post

// ② HTTP 层错误（4xx/5xx/网络失败）走 reject 分支：
try {
  const res = await postApi.getPostList({ pageNum: 1 })
} catch (err) {
  // err 是 unknown（strict + useUnknownInCatchVariables），先收窄：
  if (axios.isAxiosError(err)) {
    showToast(err.userMessage ?? err.message)   // isAxiosError 是官方类型守卫
  } else {
    showToast('未知错误')
  }
}
```

`axios.isAxiosError(err)` 内部就是一个 `err is AxiosError` 的自定义守卫（4.7.5 的官方范例）。而 `err.userMessage` 之所以有类型，靠的是 4.13.5 的声明合并。**错误处理链路上每个环节——信封 code、拦截器加工的 userMessage、catch 里的收窄——都有类型参与**，这就是「API 层被 TS 重写后」该有的样子。

### 4.13.7 调用方视角：类型如何改变组件代码

最后站在消费端看完整收益（`Home.vue` 思路）：

```ts
import { postApi } from '@/api/post'
import type { Post } from '@/types'

const posts = ref<Post[]>([])

onMounted(async () => {
  const res = await postApi.getPostList({ pageNum: 1, pageSize: 10 })
  posts.value = res.data.records     // ✅ res.data 是 PaginatedData<Post>
  // 如果这里手滑写成 res.data.data.records：
  // ❌ TS2339：Property 'records' does not exist on type 'unknown'
  // 类型系统当场告诉你「你多拆了一层信封」
})
```

把 4.13.2 的契约再压缩成一句给未来的自己：**在 SelfBlog 前端，「await 拿到的」就是信封，`.data` 之后才是业务数据；少一层会拿到信封本身，多一层会拿到 unknown。写错任何一边，vue-tsc 都会拦下你。**

---

## 4.14 常见编译错误与排查

最后是一份「急救手册」：SelfBlog 前端开发中最常遇到的编译错误、错误信息原文与修复方法。遇到报错先来这张表对号入座。

**TS2532 / TS18048 —— 「可能是 undefined/null」**

```text
error TS2532: Object is possibly 'undefined'.
error TS18048: 'user.value' is possibly 'undefined'.
```

最常见错误，几乎总是 `strict` + `noUncheckedIndexedAccess` 在尽职（4.10.2）。修复优先级：`?.` 可选链 > 显式守卫 `if (x)` > `??` 默认值 > 有验证逻辑后的 `!` 断言。

```ts
const title = posts[0]?.title                    // ①
if (first) { use(first.title) }                  // ②
const page = Number(route.query.page ?? 1)       // ③
```

**TS2339 —— 属性不存在**

```text
error TS2339: Property 'records' does not exist on type 'unknown'.
error TS2339: Property 'nikeName' does not exist on type 'User'. Did you mean 'nickname'?
```

两种成因：类型错了（拼错、用错层级，如 4.13.7 的 `res.data.data`）或类型缺了（接口里没这个字段）。注意 `Did you mean` 提示——TS 的纠错建议通常就是答案。若确信字段存在但类型没跟上（比如后端新加了字段），去 `src/types/` 把契约补上。

**TS2307 —— 找不到模块**

```text
error TS2307: Cannot find module './PostCard.vue' or its corresponding type declarations.
error TS2307: Cannot find module '@/stores/user'.
```

`.vue` 导入报错：检查 `env.d.ts` 的 `declare module '*.vue'` 是否还在（4.11.3）；`@/` 别名报错：检查 `tsconfig.json` 的 `baseUrl`/`paths` 与 Vite 的 `resolve.alias` 两边是否一致。路径大小写也要与磁盘一致，Windows 不区分大小写但 TS 区分。

**TS2304 —— 找不到名称**

```text
error TS2304: Cannot find name 'ref'.
```

忘了 `import { ref } from 'vue'`。和 TS2307 的区别：TS2307 是「模块找不到」，TS2304 是「标识符找不到」。

**TS7006 —— 隐式 any**

```text
error TS7006: Parameter 'item' implicitly has an 'any' type.
```

回调参数在上下文推断失效（如对 `any[]` 遍历、解构了无类型对象）时出现。修复：给源头补类型（数组用 `T[]` 标注），而不是给参数补 `any`——治本不治标。

**TS2345 —— 参数类型不匹配**

```text
error TS2345: Argument of type 'string | undefined' is not assignable to
parameter of type 'string'.
```

联合类型传给了更窄的参数。说明调用方可能传来空值——先收窄（`?.`、守卫、`??`），而不是急着 `!`。

**TS2322 —— 类型不可赋值**

```text
error TS2322: Type 'string' is not assignable to type 'number'.
```

赋值两侧类型对不上，从报错里「来源类型 → 目标类型」两端各自排查。若来自 props/接口响应，往往意味着契约（类型定义）和实际数据不一致，修契约不修编译器。

**TS2564 —— 类字段未初始化**

```text
error TS2564: Property 'title' has no initializer and is not definitely
assigned in the constructor.
```

`strictPropertyInitialization` 的功劳。修复：构造器里赋值，或 `title?: string`，或确有保证时 `title!: string`。

**TS6133 —— 未使用的变量**

```text
error TS6133: 'x' is declared but its value is never read.
```

`noUnusedLocals`/`noUnusedParameters` 的功劳。删掉它；参数确需占位时命名成 `_` 开头。

**TS7016 —— 模块缺少类型声明**

```text
error TS7016: Could not find a declaration file for module 'untyped-lib'.
```

第三方库没带类型。按顺序：找 `@types/untyped-lib` → 自己写最小 `declare module 'untyped-lib'`（4.11.3）→（下策）`// @ts-expect-error` 并留注释。

**TS2769 —— 重载都不匹配**

```text
error TS2769: No overload matches this call.
```

泛型函数/重载函数的参数组合对不上号。展开报错详情看「候选签名们」期望什么，最常见于 axios 调用（config 对象字段写错名）。

**通用排查流程**：

1. 读错误码 + 错误文本，先查本表；
2. 把鼠标悬停报错处，看编辑器给出的「期望类型 vs 实际类型」；
3. 怀疑类型定义时，`Ctrl+点击` 跳进类型定义看原文；
4. 报错指向 `.vue` 模板内部时，用 `npm run type-check` 拿完整输出（dev 模式不检查类型）；
5. 最后的手段才是 `as`/`@ts-expect-error`——每次使用都要能向 code review 解释为什么类型系统在这「失明」。

---

## 4.16 后端类型对照：为什么 Java 程序员学 TS 又快又慢

SelfBlog 恰好是一座「双语桥梁」：后端 Java 21，前端 TS 6。两边类型系统的同与异，值得专门对照一次——会 Java 的人学 TS 常常「三天上手，三个月才真正转弯」。

### 4.16.1 相同的心智，直接迁移

| 概念 | Java | TypeScript | 迁移提示 |
|------|------|-----------|---------|
| 接口约定 | `interface GuestCommentRequest { String nickname; }` | `interface GuestCommentRequest { nickname: string }` | 几乎一一对应 |
| 泛型 | `Result<T>`、`List<TagVO>` | `Result<T>`、`TagVO[]` | TS 泛型擦除方式不同但用法同 |
| 可选性 | `@Nullable` / Optional | `field?:` / `T \| undefined` | TS 的可选是**编译期强制的**，比注解真 |
| 联合场景 | sealed interface + pattern matching（Java 21） | 联合类型 + 收窄 | 4.7 节守卫 ↔ Java switch pattern |
| 访问控制 | private/public | private/public/#私有字段 | 语义相近 |

`SelfBlog 后端 Result<T> 信封`与前端 `api.get<Result<T>>` 能严丝合缝，正是因为两边泛型思维一致——**同一个类型在两次编译里各管一段**。

### 4.16.2 必须转弯的四个观念

1. **TS 类型只活在编译期**。Java 的泛型擦除至少反射还在；TS 编译成 JS 后**类型完全消失**，运行时没有任何痕迹。所以「运行时校验」必须另做（SelfBlog 用 zod 式手工守卫与后端 Bean Validation 各管一段）——类型再严也挡不住网络上来的脏数据。
2. **结构类型（structural typing）取代名义类型**。Java 里两个类字段完全相同也不能互赋（名义类型）；TS 里只要**形状匹配**就能赋值。这带来灵活性（POJO 互转免转换），也带来「鸭子类型的意外匹配」——`isDeleted` 字段都有的两个接口可能悄悄互通，改字段名要三思。
3. **联合类型是平面的**。Java 表达「成功或失败」要 sealed interface + 两个 record；TS 一个 `Result = Ok | Err` 就够，收窄靠 `if ('err' in r)`。写多了你会发现 **TS 的代数数据类型（ADT）比 Java 的类层级更轻**。
4. **null 语义两套体系**。Java 的 null 是运行时陷阱（NPE），靠 Optional 自律；TS 在 `strictNullChecks` 下把 null/undefined **纳入类型系统**，`T | undefined` 直接写进签名。后端 VO 里的可空字段（如 `post.summary`），到前端类型里应该显式 `string | null` 而不是 `string?`——前者逼调用方处理，后者容易漏。

### 4.16.3 一个贯穿两端的实战：状态码

后端 Java：`BusinessException` 携带 int code；前端 TS 需要同一套常量。SelfBlog 的做法是在 `types/` 里**收窄为字面量联合**而非裸 number：

```typescript
// 后端语义：40101 未登录 / 40301 无权限 / 40401 资源不存在（节选）
type BizCode = 200 | 400 | 401 | 403 | 404 | 429 | 500 | 40101 | 40301 | 40401

function isAuthError(code: number): code is 401 | 40101 {   // 类型守卫同时收窄值域
  return code === 401 || code === 40101
}
```

裸 `number` 时 `code === 40102` 这种手滑编译器不吭声；字面量联合让「拼错的状态码」直接编译报错。**把后端的魔法数字在前端建模成字面量联合**，是两端共享类型心智最便宜的一招——这也回答了一个常见疑问：有了运行时校验还需要类型吗？类型管「我写的代码对不对」，校验管「别人发来的数据对不对」，各司其职。

---

## 本章总结

这一章我们从「为什么需要类型」一路写到「重写整个 API 层」，把 SelfBlog 前端的 TypeScript 实践完整拆了一遍。用一张图回顾知识地图：

```text
类型基础            组合与抽象           类型体操（认读为主）      工程实战
────────           ─────────           ─────────────────       ─────────
原始类型/数组/元组   interface/type      条件类型/infers         tsconfig 严格模式
enum vs 字面量联合  extends/implements  映射类型/模板字面量       noUncheckedIndexedAccess
any vs unknown     泛型函数/接口/类      可辨识联合/守卫 is       模块与 .d.ts
void/never         工具类型六大件        装饰器（读得懂）          Vue3 类型集成
null/undefined     索引访问类型          ——                     API 层信封类型
```

必须带走的十句话：

1. **TS 是开发期的质检员**：类型擦除后运行时只有 JS，「可能为空」要靠类型建模而非祈祷。
2. **函数签名必须显式标注**，局部变量放心交给推断。
3. **`unknown` 替代 `any`**：不可信数据先验明正身再使用。
4. **对象用 interface，联合/计算类型用 type**，别教条但要一致。
5. **泛型保住「进出类型一致」的链路**；`ApiResponse<T>`/`PaginatedData<T>` 是 SelfBlog 的地基。
6. **可辨识联合把非法状态挡在类型之外**；穷尽检查用 `never` 兜底。
7. **`noUncheckedIndexedAccess` 让 `arr[0]` 诚实地返回 `T | undefined`**——用 `?.`、守卫、`??`，别滥用 `!`。
8. **Vue 3 里 `defineProps`/`defineEmits` 泛型写法 + `vue-tsc`** 才是完整的类型检查链。
9. **SelfBlog API 层的契约**：拦截器拆包后 `await` 拿到的就是 `{code, message, data}` 信封，调用方读 `res.data`，不是 `res.data.data`。
10. **类型是合同**：后端改字段，前端先改 `types/`——契约先行，编译器帮你把所有失联点找出来。

下一章（第六章《前端工程化》）将把镜头拉远：Vite 的构建流水线、ESLint/Prettier 闸门、Vitest 测试体系如何围绕本章的类型体系协同运转。

## 4.15 类型在真实项目中的组织：从单文件到类型层

小项目把类型随手写在组件里没问题；SelfBlog 的规模（5 个 store、30+ 组件、40+ 接口）已经需要**类型层**的自觉。这一节讲类型文件怎么组织、怎么演进。

### 4.15.1 types/ 目录的分层原则

```
frontend/src/types/
├── common.ts        # 全站地基：Result<T>、分页结构、通用枚举
├── post.ts          # 文章域：PostVO、PostDetail、PostRequest...
├── comment.ts       # 评论域
└── ...              # 一个业务域一个文件，与 api/ 目录平行对应
```

三条纪律：

1. **common.ts 只放跨域契约**：`Result<T>` 信封、`PageResult<T>`（records/total/size/current/pages）、时间戳类型别名。域类型往上放会导致循环依赖
2. **api 文件与类型文件一一对应**：`api/post.ts` 用的类型全部来自 `types/post.ts`——看 API 文件的 import 就知道它依赖哪些契约
3. **类型与后端同步演化**：后端 VO 加字段 → 前端类型同步加（可选字段用 `?` 标注过渡）→ `npm run type-check` 把所有失联点找出来。**类型是前后端之间的正式合同**

### 4.15.2 泛型工具的「自建层」：从 Result<T> 派生一切

SelfBlog 的 `types/common.ts` 展示了一个重要模式——**用泛型派生减少重复**：

```typescript
// 地基：后端信封的镜像
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 派生：分页数据是「信封里的 data」的固定形状
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 使用侧：一个接口的类型 = Result<业务数据>，分页接口 = Result<PageResult<业务数据>>
export function getPostDetail(id: number) {
  return http.get<Result<PostVO>>(`/posts/${id}`)
}
export function getPostPage(pageNum: number) {
  return http.get<Result<PageResult<PostSummaryVO>>>('/posts', { params: { pageNum } })
}
```

注意 `Result<T = unknown>` 的**默认泛型参数**：忘了填 T 时退化为 unknown 而不是 any——忘了写类型会得到严格模式的保护，而不是静默放弃检查。这是「让错误发生在编译期」哲学的小小体现。

### 4.15.3 何时用 unknown 收敛边界：外部数据的检疫站

类型系统的铁律：**编译期类型只约束「你写的」，不约束「运行时进来的」**。localStorage、HTTP 响应、URL 参数都是外部数据，进系统前要过检疫：

```typescript
// ❌ 直接断言：后端改了结构，这里就是定时炸弹
const cfg = JSON.parse(localStorage.getItem('config')!) as SiteConfig

// ✅ 检疫模式：unknown 进 → 运行时验证 → 受信类型出
function isSiteConfig(v: unknown): v is SiteConfig {
  return (
    typeof v === 'object' && v !== null &&
    'siteName' in v && typeof (v as { siteName: unknown }).siteName === 'string'
  )
}
const raw: unknown = JSON.parse(localStorage.getItem('config') ?? 'null')
const cfg: SiteConfig | null = isSiteConfig(raw) ? raw : null   // 检疫失败给默认值
```

SelfBlog 的 `stores/site.ts` 在后端挂掉时页面照常渲染，靠的就是这个思路——**外部数据永远可能缺失或畸形，类型建模必须把「不可信」表达出来，而不是用 `as` 把它藏起来**。

### 4.15.4 从 JS 项目渐进迁移 TS 的路线图

很多读者面对的不是新项目而是存量 JS 项目。渐进迁移的顺序（SelfBlog 的骨架天然支持）：

1. **`allowJs: true` + `checkJs: false`**：TS 项目先接纳 .js 文件共存，不检查
2. **入口层先行**：`api/`、`types/`、`utils/` 这些被所有组件依赖的底层先 TS 化——它们的类型收益最大（调用方全部自动获得检查）
3. **`strict: false` 起步，逐项打开**：先 `strictNullChecks`（收益最大），再 `noImplicitAny`，最后 `noUncheckedIndexedAccess`（最严但最吵）
4. **Vue SFC 逐个转**：`<script setup lang="ts">` 一个个改，`vue-tsc --noEmit` 逐步消灭红色
5. **CI 闸门锁死**：type-check 进闸门后，TS 化的部分永远不会再退回 JS

**心法**：迁移的目标不是「100% TS」，而是「**边界类型 100% 准确**」。内部实现可以慢慢来，但 API 层、store 状态的形状必须先行——边界准确了，内部重构是安全的；边界含糊，内部再严谨也是裸奔。

## 参考资源

- **TypeScript 官方手册**（中文版）：https://www.typescriptlang.org/zh/docs/handbook/intro.html —— The Basics、Narrowing、Generics、Decorators 各章与本章一一对应
- **TSConfig 参考**：https://www.typescriptlang.org/tsconfig —— `strict`、`noUncheckedIndexedAccess`、`isolatedModules` 的权威说明
- **TypeScript 深水区：类型体操**：https://github.com/type-challenges/type-challenges —— 想精通条件类型/infer/模板字面量，从 easy 刷起
- **Vue 3 与 TypeScript**：https://cn.vuejs.org/guide/typescript/overview.html —— `defineProps`/`defineEmits` 泛型写法官方指南
- **vue-tsc 与 Vue 语言工具**：https://github.com/vuejs/language-tools —— `.vue` 类型检查的实现方
- **Axios 类型文档（源码即文档）**：https://github.com/axios/axios/blob/v1.x/index.d.ts —— `get<T, R>` 双泛型与 `isAxiosError` 守卫的原始出处
- **Total TypeScript 免费教程**：https://www.totaltypescript.com/tutorials —— Matt Pocock 的交互式练习，收窄与泛型章节尤其好
- **本项目参照代码**：`frontend/src/types/common.ts`、`frontend/src/api/index.ts`、`frontend/src/api/post.ts`、`frontend/tsconfig.json`、`frontend/env.d.ts` —— 本章所有真实例子的出处，读文档不如读自家代码

---

# 第五章 Vue 3 全家桶实战

在前面的章节里，我们已经用 Spring Boot 把后端 API 搭了起来。本章开始进入 SelfBlog 的前端世界。我们将以 SelfBlog 的真实源码为蓝本，系统学习 **Vue 3 全家桶**：核心的 Composition API、模板语法、组件体系、Vue Router 4 路由、Pinia 状态管理、Axios 网络请求封装，以及测试与性能优化。学完本章，你应当能够独立读懂并扩展 SelfBlog 的前端代码。

SelfBlog 前端使用的技术栈版本如下（以 `frontend/package.json` 与 `package-lock.json` 锁定版本为准）：

| 依赖 | 版本 | 作用 |
| --- | --- | --- |
| vue | 3.5.44 | 渐进式 JavaScript UI 框架 |
| vite | 6.4.4 | 开发服务器与构建工具 |
| vue-router | 4.3.0 | 官方路由 |
| pinia | 2.1.7 | 官方状态管理 |
| axios | 1.7.4 | HTTP 客户端 |
| marked + dompurify | 12.x / 3.x | Markdown 渲染与 XSS 防护 |
| typescript + vue-tsc | 6.0.3 / 3.3.3 | 类型检查 |
| vitest + @vue/test-utils | 4.1.8 / 2.4.10 | 单元测试（jsdom 环境） |

---

## 5.1 Vue 3 简介与环境搭建

### 5.1.1 什么是 Vue 3：渐进式框架

Vue 是一款用于构建用户界面的**渐进式框架**。所谓"渐进式"，是指你可以按需采用它的能力：最简单时，只把 Vue 当作一个模板引擎嵌进现有页面；往后需要组件化，就引入单文件组件；页面多了需要路由，就加 Vue Router；数据共享复杂了，再加 Pinia。每一层都是可选的，不必一开始就接受整套框架体系。

Vue 3 相比 Vue 2 的几个关键变化：

1. **Composition API（组合式 API）**：以函数的方式组织逻辑，让"同一个功能的代码"聚在一起，逻辑可以抽成可复用的组合式函数（composable）。
2. **性能更好**：重写的响应式系统用 `Proxy` 替代 `Object.defineProperty`，能侦测到属性新增/删除和数组索引变化；编译期做了大量静态提升、补丁标记（patch flag）优化，更新更快、内存更省。
3. **更好的 TypeScript 支持**：整套 API 都是类型友好的，`defineProps<T>()` 直接从泛型推导 props 类型。
4. **`<script setup>` 语法糖**：让单文件组件的写法更简洁，SelfBlog 全部组件都采用这种写法。

### 5.1.2 Composition API 与 Options API

同一个组件可以用两种风格书写。先看 **Options API**（选项式）：

```vue
<script>
export default {
  data() {
    return { count: 0, doubled: 0 }
  },
  computed: {
    double() {
      return this.count * 2
    }
  },
  methods: {
    increment() {
      this.count++
    }
  },
  mounted() {
    console.log('组件已挂载')
  }
}
</script>

<template>
  <button @click="increment">点了 {{ count }} 次，翻倍是 {{ double }}</button>
</template>
```

再看等价的 **Composition API**（组合式）：

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const count = ref(0)
const double = computed(() => count.value * 2)

function increment(): void {
  count.value++
}

onMounted(() => {
  console.log('组件已挂载')
})
</script>

<template>
  <button @click="increment">点了 {{ count }} 次，翻倍是 {{ double }}</button>
</template>
```

两者能力完全等价，区别在于**代码组织方式**：

- Options API 按"选项类型"归类（数据都进 `data`，方法都进 `methods`），组件小时直观，但组件一大，同一个功能的代码就被撕成了几段，在数千行的组件里来回跳转非常痛苦。
- Composition API 按"功能"归类，`ref`/`computed`/函数/监听器写在一起；而且逻辑可以整体抽离成 `useXxx()` 函数，在多个组件间复用。

**如何选择**：新项目、需要 TypeScript、需要复用逻辑，用 Composition API；维护老的 Vue 2 风格代码库，或只是写几个极简的演示组件，Options API 也完全没问题。SelfBlog 前端规范明确要求"使用 Vue 3 Composition API（`<script setup>`）"（见 `frontend/CLAUDE.md`），全书后续示例都以这种风格为主。

### 5.1.3 用 Vite 创建项目

**Vite**（法语"快"，读 /vit/）是 Vue 作者尤雨溪开发的构建工具。开发阶段它基于浏览器原生 ES Module，按需编译被请求的模块，所以冷启动几乎是毫秒级的；热更新（HMR）也只替换改动的那一小块，改完代码浏览器瞬间就能看到效果。生产构建则用 Rollup（Vite 6 里也可用 esbuild 压缩）打包出高度优化的静态资源。

创建一个 Vue 3 + TypeScript 项目的推荐方式：

```bash
# 官方脚手架 create-vue，会交互式询问要不要 Router / Pinia / Vitest 等
npm create vue@latest my-blog

# 或者只用 Vite 模板
npm create vite@latest my-blog -- --template vue-ts

cd my-blog
npm install
npm run dev   # 默认 http://localhost:5173
```

create-vue 会问一系列问题：是否使用 TypeScript（建议选是）、是否引入 Vue Router、是否引入 Pinia、是否引入 Vitest 单元测试、是否引入 ESLint/Prettier 等。SelfBlog 选择了以上全部。

启动后你会看到两个关键命令：

```bash
npm run dev      # 开发服务器（SelfBlog 固定端口 3000）
npm run build    # 先 vue-tsc --noEmit 类型检查，再 vite build 打包
```

SelfBlog 的 `frontend/package.json` 里还能看到几个工程化脚本，它们构成了前端的"质量闸门"：`lint:check`（ESLint 只读检查）、`format:check`（Prettier 格式检查）、`type-check`（`vue-tsc --noEmit`）、`test:run`（Vitest 单次运行）。每次提交前这五项都应该保持绿色——这是保证"43 个测试文件 / 400 个测试"基线不倒退的习惯。

### 5.1.4 认识项目结构：以 SelfBlog 为例

一个 Vite + Vue 3 项目的入口是根目录的 `index.html`——它**不是**由某段代码生成的，而就是应用的宿主页面，其中有一行 `<div id="app"></div>`，Vue 应用最终会挂载到这个节点上。真正的 JS 入口由 `index.html` 里的 `<script type="module" src="/src/main.ts">` 引入。

SelfBlog 前端 `frontend/src` 的目录结构如下：

```
src/
├── main.ts          # 应用入口：创建应用、装配插件、启动引导
├── App.vue          # 根组件（主题根 #selfblog、导航、路由出口）
├── api/             # Axios 封装与各业务 API 模块（index.ts / site.ts / post.ts ...）
├── components/      # 通用组件（common/、layout/、post/、pages/、admin/）
├── composables/     # 组合式函数（useDialog / useConfirm / useSeoMeta / useScrollReveal ...）
├── router/          # 路由（index.ts、guards.ts、modules/ 按业务拆分）
├── stores/          # Pinia 状态（site / theme / user / music / pages）
├── styles/          # 样式（main.css 入口 + variables.css 设计 token + dark-mode.css ...）
├── types/           # TypeScript 类型定义
├── utils/           # 工具（crypto / logger / toast / visitor / reading）
├── constants/       # 常量（如 STORAGE_KEY_PREFIX = 'edu_'）
├── views/           # 页面级组件（Home / About / Archives / admin/ ...）
└── test/            # 测试全局配置 setup.ts
```

几个值得记住的分层约定：

- **`views/` 放"页面级"组件**（一个路由对应一个），**`components/` 放可复用组件**（被页面引用）。
- **`api/` 统一收口网络请求**：页面不直接写 `axios.get(...)`，而是调用 `src/api/` 里导出的语义化函数。
- **`stores/` 只放需要跨组件共享的状态**，组件私有的临时状态仍留在组件里。
- `vite.config.ts` 里配置了两个对本项目至关重要的东西：**路径别名** `@` → `src`（所以到处能看到 `import x from '@/stores/site'`），以及**开发代理**——`/api` 与 `/uploads` 都转发到 `VITE_API_TARGET`（默认 `http://localhost:8825`，即后端端口），这样开发时不存在跨域问题。

```ts
// frontend/vite.config.ts（节选）
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: { alias: { '@': resolve(__dirname, 'src') } },
    server: {
      port: 3000,
      proxy: {
        '/api': { target: env.VITE_API_TARGET || 'http://localhost:8825', changeOrigin: true },
        '/uploads': { target: env.VITE_API_TARGET || 'http://localhost:8825', changeOrigin: true }
      }
    }
  }
})
```

---

## 5.2 模板语法

Vue 的模板就是"增强版 HTML"：在原生 HTML 的基础上加入插值和指令（以 `v-` 开头的特殊属性）。模板会被编译成渲染函数，最终生成虚拟 DOM。

### 5.2.1 文本插值

双大括号 `{{ }}` 是最基本的插值，里面可以放**任何 JavaScript 表达式**（不是语句）：

```vue
<template>
  <p>{{ title }}</p>
  <p>{{ count > 0 ? `共 ${count} 篇` : '暂无文章' }}</p>
  <p>{{ new Date().getFullYear() }}</p>
  <!-- 注意：下面这些是非法的 -->
  <!-- {{ if (ok) { return 'a' } }}   语句不行 -->
  <!-- {{ const a = 1 }}              声明不行 -->
</template>
```

插值内容会被当作纯文本渲染——Vue 自动做了 HTML 转义，所以 `{{ userInput }}` 是**天然防 XSS** 的。如果你确实要渲染富文本 HTML，必须用 `v-html`，并且像 SelfBlog 那样先用 DOMPurify 消毒：

```vue
<!-- SelfBlog 渲染 Markdown 正文时：marked 转 HTML，DOMPurify 消毒，才允许 v-html -->
<article v-html="sanitizedBody"></article>
```

### 5.2.2 属性绑定 v-bind 与简写 `:`

HTML 属性想引用 JS 变量，不能用 `{{ }}`（那会被当成字面字符串），要用 `v-bind`，简写为一个冒号：

```vue
<template>
  <!-- 完整写法 -->
  <a v-bind:href="postUrl">文章链接</a>
  <!-- 简写 -->
  <a :href="postUrl">文章链接</a>

  <!-- 动态拼 class：对象语法 -->
  <div :class="{ 'tag--closable': closable, 'tag--disabled': disabled }"></div>
  <!-- 数组语法 -->
  <span :class="['tag', `tag--${variant}`, `tag--${size}`]"></span>

  <!-- 绑定布尔属性 -->
  <button :disabled="loading">保存</button>

  <!-- 动态属性名（方括号） -->
  <div :[attrName]="attrValue"></div>

  <!-- 绑定整个对象展开成多个属性 -->
  <input v-bind="{ type: 'text', placeholder: hint }" />
</template>
```

SelfBlog 的 `components/common/Tag.vue` 就大量使用这些语法：`:class` 数组 + 模板字符串拼出 BEM 风格类名，`:disabled` 绑定按钮状态。

### 5.2.3 事件绑定 v-on 与简写 `@`

`v-on` 监听 DOM 事件，简写为 `@`：

```vue
<template>
  <button @click="increment">+1</button>

  <!-- 内联表达式（简单逻辑可以直接写） -->
  <button @click="count++">内联自增</button>

  <!-- 带参数与事件对象：原生事件对象 $event -->
  <button @click="say('hi', $event)">带参调用</button>

  <!-- 修饰符链 -->
  <form @submit.prevent="onSubmit">提交不刷新页面</form>
  <div @click.stop="onClickInner">阻止冒泡</div>
  <a @click.prevent.stop="onLinkClick">阻止默认行为且阻止冒泡</a>
  <input @keyup.enter="onEnter" @keyup.esc="onEsc" />
  <button @click.once="init">只触发一次</button>
</template>
```

常用修饰符：`.prevent`（`event.preventDefault()`）、`.stop`（`stopPropagation()`）、`.once`、`.self`、按键类的 `.enter`/`.esc`/`.tab`，以及系统修饰键 `.ctrl`/`.alt`/`.shift`/`.meta`。

### 5.2.4 条件渲染：v-if 与 v-show

```vue
<template>
  <!-- v-if：惰性渲染，条件为假时元素根本不存在于 DOM -->
  <p v-if="type === 'a'">A</p>
  <p v-else-if="type === 'b'">B</p>
  <p v-else>其他</p>

  <!-- v-show：永远渲染，只是切换 display:none -->
  <p v-show="isVisible">我只是在隐藏</p>
</template>
```

**如何选择**：

- `v-if` 是真正的条件渲染，切换时有完整的创建/销毁开销；初始条件大概率不为真时用它（渲染得更少）。
- `v-show` 只切换 CSS，适合频繁切换显示状态的元素（比如 SelfBlog 返回顶部按钮 `BackToTop.vue` 那种滚动后才出现的控件）。
- `v-if` 可以用在 `<template>` 标签上做"无渲染包装"，`v-show` 不行（`template` 不产生真实 DOM）。

### 5.2.5 列表渲染 v-for 与 key

```vue
<script setup lang="ts">
import { ref } from 'vue'

interface Article {
  id: number
  title: string
  category?: string
}

const articles = ref<Article[]>([
  { id: 1, title: 'Vue 3 上手' },
  { id: 2, title: 'Pinia 入门', category: '前端' }
])
</script>

<template>
  <!-- 数组：(item, index) -->
  <li v-for="(article, index) in articles" :key="article.id">
    {{ index + 1 }}. {{ article.title }}
  </li>

  <!-- 对象：(value, key, index) -->
  <li v-for="(value, key) in siteSettings" :key="key">{{ key }}: {{ value }}</li>

  <!-- 数字：渲染 1..10 -->
  <span v-for="n in 10" :key="n">{{ n }} </span>
</template>
```

**`key` 的意义**：Vue 更新列表时默认采用"就地复用"策略，逐个比对同位置的节点。如果列表会插入、删除、排序，不带 key 或用 `index` 当 key，会导致节点错位复用——比如带输入框的列表删除中间一项后，输入框内容"串行"了。给每项一个**稳定且唯一**的 `key`（通常是后端 id），Vue 就能精确识别哪个节点应该移动/复用/删除。这是面试高频题，也是最容易踩的坑之一：**不要用数组下标当 key，除非列表纯静态渲染且永不重排**。

### 5.2.6 表单绑定 v-model

`v-model` 是"值绑定 + 事件监听"的语法糖：

```vue
<input v-model="text" />
<!-- 等价于 -->
<input :value="text" @input="text = ($event.target as HTMLInputElement).value" />
```

不同表单元素对应不同事件：`input`/`textarea` 用 `input` 事件，复选框/单选框/下拉框用 `change` 事件。修饰符：

```vue
<input v-model.lazy="draft" />     <!-- change 事件才同步，而非每次输入 -->
<input v-model.number="age" />     <!-- 自动转数字 -->
<input v-model.trim="username" />  <!-- 自动去首尾空格 -->
<textarea v-model="bio"></textarea>

<!-- 复选框绑定数组：勾选的值收进数组 -->
<input type="checkbox" value="vue" v-model="stack" />
```

**一个初学者常见误区**：`v-model` 绑定的变量必须是响应式的（`ref` 或 `reactive` 的属性）。如果直接绑定 props 上的值，Vue 会警告"Cannot mutate props"——正确做法见 5.4.4 节的组件 v-model。

### 5.2.7 综合示例：一个搜索框组件

把本节知识串起来，写一个 SelfBlog 风格的搜索输入组件：

```vue
<script setup lang="ts">
import { ref } from 'vue'

const keyword = ref('')
const history = ref<string[]>([])

function search(): void {
  const kw = keyword.value.trim()
  if (!kw) return
  history.value = [kw, ...history.value.filter((h) => h !== kw)].slice(0, 5)
  keyword.value = kw
}
</script>

<template>
  <form class="search" @submit.prevent="search">
    <input
      v-model.trim="keyword"
      type="search"
      placeholder="搜索文章…"
      @keyup.esc="keyword = ''"
    />
    <button type="submit" :disabled="!keyword">搜索</button>
  </form>
  <ul v-if="history.length" class="search-history">
    <li v-for="item in history" :key="item" @click="keyword = item">{{ item }}</li>
  </ul>
  <p v-else class="search-empty">暂无搜索历史</p>
</template>
```

---

## 5.3 组合式 API 核心

### 5.3.1 `<script setup>`：setup 语法糖

`<script setup>` 是 Composition API 在单文件组件里的编译期语法糖，比标准的 `setup()` 函数更简洁：

```vue
<script setup lang="ts">
import { ref } from 'vue'
import Tag from '@/components/common/Tag.vue'

const msg = ref('hello')
// 顶层声明的变量、函数、导入的组件——全部自动暴露给模板，不用 return
function reverse(): void {
  msg.value = msg.value.split('').reverse().join('')
}
</script>

<template>
  <p>{{ msg }}</p>
  <button @click="reverse">反转</button>
  <!-- 导入的组件直接用，不用注册 components -->
  <Tag variant="primary">标签</Tag>
</template>
```

它带来三重好处：一是不用写 `return { ... }`，二是导入的组件直接可用，三是编译器能做**更激进的静态分析**（比如把静态节点提升出渲染函数），性能更好。`lang="ts"` 则让整个组件获得完整类型检查。

### 5.3.2 ref 与 reactive：两种响应式数据

**`ref`** 可以包装任何类型的值（原始值、对象、函数都行），返回一个带 `.value` 属性的引用对象：

```ts
import { ref } from 'vue'

const count = ref(0)
const user = ref({ name: 'Zora', tags: ['vue'] })

count.value++              // 修改要通过 .value
user.value.name = 'Zora'   // 对象内部依然是响应式的（ref 内部用了 reactive）
```

**`reactive`** 只接受对象（含数组、Map/Set），返回原对象的 Proxy，访问时不需要 `.value`：

```ts
import { reactive } from 'vue'

const form = reactive({ title: '', tags: [] as string[] })
form.title = '新文章'      // 直接改
form.tags.push('vue')      // 数组方法也正常
```

**实践建议（SelfBlog 的实际取向）**：

- 统一优先用 `ref`。原因有二：其一，`ref` 能包装任意类型（数字、字符串、布尔、对象通吃）；其二，`ref` 可以**整体替换**（`state.value = newState`），而 `reactive` 不能替换引用（替换后就断了响应式）。SelfBlog 的 `stores/pages.ts` 里 `const pages = ref<Record<string, PublicPage>>({})`，每次加载清单后整体赋值 `pages.value = Object.fromEntries(...)`，就是靠整体替换实现的。
- `reactive` 适合"表单"这类字段固定的局部对象，且**绝不解构**（原因见下）。

**`reactive` 的头号大坑——解构失活**：

```ts
const state = reactive({ count: 0 })
let { count } = state        // ❌ 解构出的是普通数字，与原对象断开
count++                      // 不会触发任何更新

const { count } = toRefs(state)  // ✅ 用 toRefs 解构，得到 ref
```

### 5.3.3 ref 的解包规则

"解包"指访问 `ref` 时不需要写 `.value` 的场景。规则值得专门背下来：

```ts
// 1) 模板顶层 ref 自动解包
const count = ref(0)
// 模板里写 {{ count }} 而不是 {{ count.value }}

// 2) reactive 对象的属性位置会解包
const state = reactive({ count })
state.count++        // ✅ 不用 .value

// 3) 数组和集合里【不会】解包
const list = [count]
list[0].value++      // ⚠️ 必须 .value

// 4) 普通函数里、普通对象字面量里都不会自动解包
const obj = { count }        // obj.count 是 ref，要 obj.count.value
function logIt() {
  console.log(count.value)   // JS 代码里永远要 .value
}

// 5) 解构 reactive 会丢响应式；解构 ref 组成的对象需要 toRefs
```

初学者最常见的报错就是"值不更新"或"拿到的是 ref 对象而不是值"——九成是解包规则没理清。口诀：**JS 代码里 `.value`，模板与 reactive 属性位不用**。

### 5.3.4 toRef 与 toRefs

```ts
import { reactive, toRef, toRefs } from 'vue'

const state = reactive({ count: 0, name: 'Zora' })

// toRef：为某个属性创建一个与源保持同步的 ref
const countRef = toRef(state, 'count')
countRef.value++      // state.count 同步变成 1
state.count++         // countRef.value 同步变成 2

// toRefs：把整个 reactive 对象转成"全是 ref 的普通对象"，常用于安全解构
const { count, name } = toRefs(state)

// Pinia 里同样重要：store 返回的是 reactive 对象，
// 直接解构会失活，必须用 storeToRefs（见 5.7 节）
```

### 5.3.5 computed 计算属性

```ts
import { ref, computed } from 'vue'

const articles = ref<Article[]>([])
const keyword = ref('')

// getter：基于依赖自动缓存
const filtered = computed(() =>
  keyword.value
    ? articles.value.filter((a) => a.title.includes(keyword.value))
    : articles.value
)

// 可写 computed（setter）：很少用但要会
const fullName = computed({
  get: () => `${firstName.value} ${lastName.value}`,
  set: (value) => {
    [firstName.value, lastName.value] = value.split(' ')
  }
})
```

`computed` 与"用函数返回结果"的区别：**缓存**。只有依赖的响应式数据变化时才重新计算，模板里多处引用也只算一次；普通方法每次渲染都会执行。另外注意：`computed` 里不要做副作用（发请求、改别的状态），那是 `watch` 的职责。SelfBlog 的 `stores/site.ts` 里 `adminRoot = computed(() => '/' + adminPath.value)`、`isMusicEnabled = computed(() => settings.value.musicEnabled === 'true' && !!settings.value.musicUrl)` 都是最典型的派生状态。

### 5.3.6 watch 侦听器

```ts
import { ref, watch } from 'vue'

const keyword = ref('')

// 侦听单个 ref
watch(keyword, (newVal, oldVal) => {
  console.log(`从 "${oldVal}" 变成 "${newVal}"`)
})

// 立即执行一次 + 防抖搜索
watch(keyword, async (kw) => {
  if (!kw) return
  await new Promise((r) => setTimeout(r, 300))
  const res = await searchApi.list({ keyword: kw })
  results.value = res.data
}, { immediate: true })

// 侦听 reactive 对象：隐式深度侦听，且 newVal/oldVal 不相等（同一引用）
const form = reactive({ title: '' })
watch(form, () => console.log('form 变了'), { deep: true })

// 侦听 getter：精准侦听某个字段
watch(() => form.title, (t) => console.log('title:', t))

// 一次侦听多个源
watch([keyword, page], ([kw, p]) => fetchList(kw, p))

// 停止侦听：watch 返回停止函数；组件卸载时同步创建的侦听器会自动停止
const stop = watch(keyword, onSearch)
stop()
```

关键选项：`immediate`（创建时立即执行一次回调）、`deep`（深度遍历对象内部变化）、`flush: 'post'`（回调延迟到 DOM 更新之后，回调里能拿到更新后的 DOM）。

**坑位提醒**：直接 `watch(reactiveObj)` 时回调里新旧值是同一个对象引用；想拿到"哪一项变了"，用 getter 形式 `watch(() => obj.field, ...)`。

### 5.3.7 watchEffect：自动追踪副作用

```ts
import { watchEffect, ref } from 'vue'

const keyword = ref('a')
const page = ref(1)

// 不写侦听目标，回调里用到谁就自动追踪谁；创建时立即执行一次
const stop = watchEffect(async (onCleanup) => {
  const kw = keyword.value
  const p = page.value
  const controller = new AbortController()
  onCleanup(() => controller.abort())   // 依赖再变时，取消上一次未完成请求
  const res = await fetch(`/api/post/list?keyword=${kw}&page=${p}`, {
    signal: controller.signal
  })
  results.value = await res.json()
})
```

**watch 与 watchEffect 怎么选**：

- 需要明确知道"谁变了"、需要新旧值对比、回调里只想对特定数据做反应 → `watch`。
- 多个依赖共同决定一个副作用、想要"立即执行 + 自动追踪" → `watchEffect`。
- 回调是异步函数时，`watchEffect` 的自动追踪只覆盖**同步执行的那一段**——异步之后的依赖不会被追踪。这也是上例把依赖先读进局部变量的原因。

### 5.3.8 生命周期钩子

Composition API 里通过 `onXxx` 函数注册钩子，必须**在 `setup` 同步执行期间**调用（不能放进 `setTimeout` 或普通函数里）：

```ts
import { onMounted, onUpdated, onUnmounted, onBeforeUnmount } from 'vue'

// <script setup> 中没有 beforeCreate / created，
// 它们的时机就是 <script setup> 本身的执行
onMounted(() => {
  console.log('DOM 已挂载，可以安全访问 ref / window')
})

onBeforeUnmount(() => {
  // 卸载前清理：移除全局监听、清定时器、断开 Observer
})

onUnmounted(() => {
  console.log('组件已卸载')
})
```

完整对照表：

| Options API | Composition API | 说明 |
| --- | --- | --- |
| beforeCreate / created | （`<script setup>` 本身） | 组件实例初始化前后 |
| beforeMount | onBeforeMount | 首次渲染前 |
| mounted | onMounted | 挂载完成，可访问 DOM |
| beforeUpdate | onBeforeUpdate | 响应式数据变了、DOM 更新前 |
| updated | onUpdated | DOM 更新完成 |
| beforeUnmount | onBeforeUnmount | 卸载前（做清理的地方） |
| unmounted | onUnmounted | 卸载完成 |
| activated / deactivated | onActivated / onDeactivated | 被 `<keep-alive>` 缓存的组件激活/失活 |

**最容易踩的坑**：在 `onMounted` 里给 `window`/`document` 注册了监听器却忘了在 `onBeforeUnmount` 里移除——组件反复进出路由后监听器越积越多，这就是内存泄漏。SelfBlog 的测试规范特别强调这一点：凡是挂载会注册全局监听器的组件，测试里要自己 `enableAutoUnmount(afterEach)`（见 5.13 节）。`stores/theme.ts` 也在 `destroyTheme()` 里专门移除 `matchMedia` 的 `change` 监听，并在重新初始化前先调用它清理旧监听。

---

## 5.4 组件基础

### 5.4.1 单文件组件 SFC 的三段结构

单文件组件（Single-File Component）把一个组件的**结构（template）、逻辑（script）、样式（style）**封装进一个 `.vue` 文件。以 SelfBlog 的 `components/common/Tag.vue` 为例（节选）：

```vue
<script setup lang="ts">
import { computed } from 'vue'

export interface TagProps {
  variant?: 'default' | 'primary' | 'success' | 'danger'
  size?: 'sm' | 'md'
  closable?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<TagProps>(), {
  variant: 'default',
  size: 'sm',
  closable: false,
  disabled: false
})

const emit = defineEmits<{ close: []; click: [event: MouseEvent] }>()

const classes = computed(() => [
  'tag',
  `tag--${props.variant}`,
  { 'tag--closable': props.closable, 'tag--disabled': props.disabled }
])
</script>

<template>
  <span :class="classes" @click="emit('click', $event)">
    <span class="tag__content"><slot /></span>
    <button v-if="closable" class="tag__close" @click.stop="emit('close')">✕</button>
  </span>
</template>

<style scoped>
.tag { display: inline-flex; align-items: center; }
</style>
```

要点：

- `<style scoped>` 给样式加上"作用域"：编译后每个元素带 `data-v-xxx` 属性，选择器自动限定在当前组件，不会污染全局。需要穿透到子组件时用 `:deep(.child-class)`。
- 组件命名用 PascalCase（`Tag.vue`、`ArticleList.vue`），模板里既可写 `<Tag />` 也可写 `<tag />`，建议统一 PascalCase。

### 5.4.2 props：定义与校验

父子组件通信的第一通道。props 是**单向数据流**：父传子，子不应该直接改。

TypeScript 泛型写法（SelfBlog 全用这种）：

```ts
interface Props {
  postId: number
  title: string
  commentsEnabled?: boolean
}

// 仅类型声明（编译期检查）
const props = defineProps<Props>()

// 带默认值
const props = withDefaults(defineProps<Props>(), {
  commentsEnabled: true
})
```

运行时对象写法（能做更细的运行时校验，两种可以按需选用）：

```ts
const props = defineProps({
  postId: { type: Number, required: true },
  title: { type: String, default: '' },
  tags: {
    type: Array as PropType<string[]>,
    default: () => [],
    validator: (value: string[]) => value.length <= 20
  }
})
```

三条纪律：

1. **不要在子组件里改 props**。需要基于 props 派生，用 `computed`；需要"用户改过之后的副本"，用 `ref(props.modelValue)` 再通过事件交回父层。
2. 对象/数组类型的默认值必须是**工厂函数**：`default: () => []`，否则所有实例共享同一个数组。
3. props 名在模板里用 kebab-case（`post-id`），在 `defineProps` 里用 camelCase（`postId`）。

### 5.4.3 emits：自定义事件

子向父通信用事件。`defineEmits` 声明组件会抛出哪些事件（以及参数类型）：

```ts
const emit = defineEmits<{
  close: []
  click: [event: MouseEvent]
  change: [id: number, value: string]
}>()

emit('close')
emit('change', 1, 'new')
```

`Tag.vue` 里关闭按钮的写法 `@click.stop="emit('close')"` 中，`.stop` 很关键：不阻止冒泡的话，点关闭按钮会先触发自己的 `close`、又冒泡到外层触发 `click`，父组件的"点标签"逻辑就被误触发了。

使用方：

```vue
<Tag variant="primary" closable @close="removeTag(tag)">{{ tag }}</Tag>
```

### 5.4.4 在组件上使用 v-model：双向绑定组件

原生表单有 `v-model`，自定义组件也可以有。组件上的 `v-model="x"` 展开为：

```vue
<MyInput v-model="title" />
<!-- 等价于 -->
<MyInput :modelValue="title" @update:modelValue="(v) => (title = v)" />
```

所以组件内部要做两件事：接收 `modelValue` prop、抛出 `update:modelValue` 事件：

```vue
<script setup lang="ts">
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

function onInput(e: Event): void {
  emit('update:modelValue', (e.target as HTMLInputElement).value)
}
</script>
<template>
  <input :value="props.modelValue" @input="onInput" />
</template>
```

Vue 3.4 起有更省事的 `defineModel()`（SelfBlog 的 Vue 3.5 完全支持）：

```vue
<script setup lang="ts">
const title = defineModel<string>({ required: true })
</script>
<template>
  <input v-model="title" />
</template>
```

一个组件还可以支持**多个 v-model**（用参数区分）：

```vue
<PageEditor v-model:title="draft.title" v-model:summary="draft.summary" />
<!-- 组件内：defineProps 含 title/summary，emits 含 'update:title' / 'update:summary' -->
```

### 5.4.5 插槽：默认、具名与作用域

插槽让父组件向子组件的指定位置"注入"模板内容，是组件通用化的利器。

```vue
<!-- Modal.vue（子） -->
<template>
  <div class="modal">
    <header><slot name="title">默认标题</slot></header>
    <div class="modal-body"><slot /></div>
    <footer><slot name="footer"></slot></footer>
  </div>
</template>
```

```vue
<!-- 使用方（父） -->
<Modal show>
  <template #title>删除确认</template>
  <p>确定要删除这篇文章吗？</p>
  <template #footer>
    <button @click="cancel">取消</button>
    <button @click="confirm">确定</button>
  </template>
</Modal>
```

- 不带 `name` 的 `<slot />` 是**默认插槽**，父组件的剩余内容都进这里；`#title` 是 `v-slot:title` 的缩写，对应**具名插槽**。
- **作用域插槽**让子组件把内部数据"递"给插槽内容用：

```vue
<!-- ArticleList.vue（子） -->
<template>
  <ul>
    <li v-for="article in articles" :key="article.id">
      <slot :article="article" :index="index" />
    </li>
  </ul>
</template>
```

```vue
<!-- 父：v-slot 接收一个"插槽 props 对象" -->
<ArticleList :articles="list">
  <template #default="{ article }">
    <h3>{{ article.title }}</h3>
  </template>
</ArticleList>
```

作用域插槽的本质是"**内容由父组件定，数据由子组件给**"，渲染列表、表格、树这类通用组件时几乎必用。

### 5.4.6 依赖注入：provide / inject

当数据需要跨越很多层传递（比如主题配置从根组件传到深层叶子），逐层 props 会产生"props 钻透"（prop drilling）。`provide/inject` 让祖先直接供给、后代直接取用：

```vue
<!-- 祖先组件 -->
<script setup lang="ts">
import { provide, ref, type InjectionKey } from 'vue'

interface ThemeContext {
  isDark: Ref<boolean>
  toggle: () => void
}

// 用 InjectionKey 获得 / inject 两侧的类型安全
const ThemeKey: InjectionKey<ThemeContext> = Symbol('theme')

const isDark = ref(false)
provide(ThemeKey, { isDark, toggle: () => (isDark.value = !isDark.value) })
</script>
```

```vue
<!-- 任意深度的后代组件 -->
<script setup lang="ts">
import { inject } from 'vue'
const theme = inject(ThemeKey)
// theme?.toggle()
</script>
```

注意两点：注入值最好保持响应式（传 ref 或 reactive）；`inject` 可以带默认值 `inject(ThemeKey, defaultValue)` 防止忘记 provide 时拿到 `undefined`。SelfBlog 的主题、皮肤等**全局**状态走的是 Pinia（更适合全局单例），`provide/inject` 更多用在"组件树局部"的上下文（比如后台抽屉的弹层管理由 `AdminLayout` 统一用 `useDialog` 提供）。

---

## 5.5 组件通信全景

把常见通信方式放在一张表里，按"数据流向 + 适用距离"选型：

| 场景 | 方式 | 一句话要点 |
| --- | --- | --- |
| 父 → 子 | `props` | 单向数据流，子不改父 |
| 子 → 父 | `emits` 自定义事件 | `emit('update:x')` 配合 v-model 可双向 |
| 父 ↔ 子（表单类） | 组件 `v-model` | `modelValue` + `update:modelValue` |
| 父 → 子（模板注入） | 插槽 / 作用域插槽 | 内容父定、数据子给 |
| 祖先 → 深层后代 | `provide / inject` | 避免逐层传递，注意响应式 |
| 兄弟组件 | 共同父组件中转；或直接上 Pinia | 简单场景经父转发，复杂共享状态用 store |
| 任意组件 ↔ 任意组件 | **Pinia** | 全局单一数据源，见 5.7 |
| 跨页面/跨路由 | Pinia + localStorage/sessionStorage | 如 SelfBlog 的主题、token |
| 全局 API 层 | `src/api/` 模块函数 | 与组件树无关，谁需要谁调用 |

**何时该用 Pinia**，SelfBlog 给出了很好的判断标准——看状态是否需要"跨组件、跨路由、甚至跨会话共享"：

- `theme.ts`（暗色模式）：导航栏按钮和页面样式都要读 → 需要 store，且持久化到 `localStorage`。
- `user.ts`（登录态）：守卫、请求拦截器、后台页面都要读 → 必须 store。
- `pages.ts`（公开页面导航）：Navbar 和各页面都要读 → store。
- 一个按钮的 loading、一个弹窗开关 → 不需要 store，留在组件里。

反模式也要警惕：**不要把 store 当成"另一个组件"**——store 里只放状态与围绕状态的行为，不放 DOM 操作和视图细节；SelfBlog 的 `stores/music.ts` 是个例外中的例外，它是刻意设计的"唯一音频宿主"（见 5.12.4）。

---

## 5.6 Vue Router 4

### 5.6.1 安装与基本配置

```bash
npm install vue-router@4
```

```ts
// src/router/index.ts
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupGuards } from './guards'
import homeRoutes from './modules/home'
import authRoutes from './modules/auth'
import postRoutes from './modules/post'

const routes: RouteRecordRaw[] = [
  ...homeRoutes,
  ...authRoutes,
  ...postRoutes,
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/common/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition          // 浏览器前进后退：恢复原位置
    if (to.hash) return { el: to.hash, behavior: 'smooth' }  // 带锚点：平滑滚动到锚点
    return { top: 0 }                                 // 默认回顶部
  }
})

setupGuards(router)
export default router
```

两种 history 模式：`createWebHistory()` 是干净的 URL（`/post/1`），需要服务器把所有路径回退到 `index.html`（SelfBlog 后端的 `SpaController` 与服务端 SEO HTML 就承担了这个职责）；`createWebHashHistory()` 是 `/#/post/1`，部署最省事但不美观。**生产环境务必配置服务端回退**，否则刷新 `#` 后面的路径会 404。

路由组件用 `() => import(...)` 懒加载（按路由分包），SelfBlog 的 `router/helpers.ts` 提供了 `lazyLoad` 帮助函数统一这层写法。

### 5.6.2 动态路由匹配

```ts
{ path: '/post/:id', name: 'PostDetail', component: () => import('@/views/post/PostDetail.vue') }
{ path: '/tag/:id(\\d+)', name: 'TagPage', component: () => import('@/views/tag/TagPage.vue') }
{ path: '/report/:type(post|comment|user)/:id(\\d+)', name: 'Report' }
```

组件里通过 `useRoute()` 拿参数：`route.params.id`（注意它是 `string | string[]`，需要 `Number(route.params.id)` 转数字；SelfBlog 用正则约束 `:id(\d+)` 保证参数一定是数字串）。同一个组件在不同参数间跳转（`/post/1` → `/post/2`）**不会重新挂载**，组件实例被复用，需要用 `watch(() => route.params.id, ...)` 响应变化。

### 5.6.3 嵌套路由

```ts
// SelfBlog router/modules/admin.ts（节选）
export function createAdminRoutes(adminPath: string): RouteRecordRaw[] {
  const root = `/${adminPath}`
  const adminMeta = { requiresAuth: true, requiresAdmin: true, adminArea: true }

  return [
    {
      path: root,
      name: 'AdminEntry',
      component: lazyLoad(() => import('@/views/admin/AdminEntry.vue')),
      children: [
        { path: 'dashboard', name: 'AdminDashboard', component: lazyLoad(() => import('@/views/admin/Dashboard.vue')), meta: { ...adminMeta, title: '后台概览' } },
        { path: 'posts', name: 'AdminPosts', component: lazyLoad(() => import('@/views/admin/Posts.vue')), meta: { ...adminMeta, title: '文章管理' } },
        { path: 'write', name: 'AdminPostCreate', component: lazyLoad(() => import('@/views/post/PostEdit.vue')), meta: { ...adminMeta, title: '发布文章' } },
        { path: 'edit/:id', name: 'AdminPostEdit', component: lazyLoad(() => import('@/views/post/PostEdit.vue')), meta: { ...adminMeta, title: '编辑文章' } },
        { path: 'settings', name: 'AdminSettings', component: lazyLoad(() => import('@/views/admin/Settings.vue')), meta: { ...adminMeta, title: '网站设置' } }
      ]
    }
  ]
}
```

子路由渲染在父组件的 `<RouterView />` 里；父路由不写 `path` 的空字符串子路由即"默认子页"。注意 SelfBlog 的后台根路径**不是硬编码的 `/admin`**，而是服务端下发的 `adminPath` 动态拼出来的（见 5.11）。

### 5.6.4 编程式导航

```ts
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()   // 用来"跳转"
const route = useRoute()     // 用来"读当前路由信息"

router.push('/post/1')
router.push({ name: 'PostDetail', params: { id: 1 } })
router.push({ path: '/search', query: { keyword: 'vue' } })   // → /search?keyword=vue
router.replace('/login')   // 不留历史记录
router.back(); router.forward(); router.go(-2)

// 读参数
console.log(route.params.id, route.query.keyword, route.fullPath)
```

`push` 带 `query` 对象时**不要**自己拼字符串；读 `query` 时记住值可能是 `undefined`（`?keyword` 可以不带值），TypeScript 下要做空值处理。

### 5.6.5 路由元信息 meta 与导航守卫

`meta` 是每个路由可自带的任意数据，最常见用途是"这个页面需要登录吗"。SelfBlog 的守卫 `router/guards.ts` 是标准范本：

```ts
// frontend/src/router/guards.ts（节选，有删减）
export function setupGuards(router: Router): void {
  router.beforeEach(async (to, from) => {
    const userStore = useUserStore()
    const siteStore = useSiteStore()

    // 统一维护 document.title
    document.title = to.meta.title
      ? `${to.meta.title as string} - ${siteStore.settings.siteTitle}`
      : siteStore.settings.siteTitle

    // 需要登录而未登录 → 跳 adminRoot 并带上 redirect 查询参数
    if ((to.meta.requiresAuth || to.meta.requiresAdmin) && !userStore.isLoggedIn) {
      return { path: siteStore.adminRoot, query: { redirect: to.fullPath } }
    }

    // 已登录但 token 已过期（本地解析 JWT 的 exp）
    if (userStore.isLoggedIn && to.meta.requiresAuth) {
      const isValid = await userStore.validateToken()
      if (!isValid) {
        userStore.resetState()
        toast.warning('登录已过期，请重新登录')
        return { path: siteStore.adminRoot, query: { redirect: to.fullPath } }
      }
    }

    // requiresAdmin 但非管理员 → 弹回首页
    if (to.meta.requiresAdmin && userStore.isLoggedIn && !userStore.isAdmin) {
      toast.error('无管理员权限')
      return { name: 'Home' }
    }
  })

  router.onError((error) => {
    // 懒加载分包失败（发版后旧 chunk 404）→ 提示刷新
    if (error.name === 'ChunkLoadError' || error.message?.includes('Loading chunk')) {
      toast.error('页面加载失败，请刷新重试')
      router.push({ name: 'Home' })
    }
  })
}
```

要点解析：

- 守卫**返回值即导航决策**：返回 `false` 取消导航；返回路由地址对象则重定向；什么都不返回则放行。这比老式 `next()` 回调更直观。
- `meta.requiresAuth` / `requiresAdmin` 在 `router/modules/admin.ts` 里通过 `adminMeta` 对象批量铺到每个后台子路由上，守卫只读 `to.meta`，路由定义与鉴权逻辑解耦。
- `redirect` 查询参数记录"本来想去哪"，登录成功后可以跳回去——这是自研登录页最常忘的细节。
- 跳转目标写的是 `siteStore.adminRoot`（服务端下发的后台路径），**不是硬编码 `/admin`**。SelfBlog 规范明确要求"组件里需要登录时统一 `router.push('/login')`"，而 `/login` 路由本身是个重定向到 `adminRoot` 的转发器，两者配合避免任何地方写死后台路径。

守卫全家福：`beforeEach`（全局前置，最常用）、`beforeResolve`（解析完异步组件后）、`afterEach`（无 `next`，做埋点/标题）；路由独享的 `beforeEnter`；组件内的 `onBeforeRouteUpdate` / `onBeforeRouteLeave`（SelfBlog 的编辑页用 `onBeforeRouteLeave` 实现"未保存提醒"）。

### 5.6.6 动态注册路由 addRoute

路由不仅能在启动时静态声明，还能在**运行时**添加：

```ts
// frontend/src/router/index.ts（节选）
export function installAdminRoutes(adminPath: string): void {
  // 防御：如果后台路径被设成 pages，就不再占用 /pages/:slug 命名空间
  if (adminPath !== 'pages' && !router.hasRoute('ContentPage')) {
    router.addRoute({
      path: '/pages/:slug',
      name: 'ContentPage',
      component: () => import('@/views/pages/ContentPage.vue'),
      meta: { title: '内容页' }
    })
  }
  for (const route of createAdminRoutes(adminPath)) {
    router.addRoute(route)
  }
}
```

配套 API：`router.hasRoute(name)` 查存在性、`router.removeRoute(name)` 移除、`router.getRoutes()` 列出全部。**重复 addRoute 同名路由是"替换"而不是报错**，但先 `hasRoute` 判断能让意图更清晰。动态注册的典型场景：权限驱动的菜单路由、多租户路径、以及 SelfBlog 这种"后台路径由服务端配置决定"的设计——没有服务端响应之前，前端根本不知道要注册到哪个路径。

### 5.6.7 RouterLink 与活动状态

```vue
<RouterLink to="/">首页</RouterLink>
<RouterLink :to="{ name: 'PostDetail', params: { id: post.id } }">详情</RouterLink>
<!-- 激活态类：RouterLinkActive 两个类自动维护 -->
```

**测试提醒**（SelfBlog 实测结论）：单测里不要用 `stubs: { RouterLink: true }`——stub 会丢掉插槽内容，也表达不了 active 状态。应该用真实的 router：`createRouter({ history: createMemoryHistory(), routes })` 挂上后再 mount（见 5.13）。

---

## 5.7 Pinia 状态管理

### 5.7.1 为什么需要状态管理，为什么是 Pinia

当多个互不相邻的组件需要同一份数据（登录态、主题、导航配置），props/事件链会变成面条。状态管理库把这份数据放到**组件树之外的单例**里，谁都能读写，且读写过程可追踪。

Pinia 是 Vue 官方推荐（Vuex 的继任者）：API 极简（没有 mutation，action 里直接改）、完整 TypeScript 推导、天然支持 Composition API 写法、devtools 集成好、支持 SSR。SelfBlog 五个 store（site/theme/user/music/pages）全部用 Pinia 组合式写法。

### 5.7.2 defineStore 与组合式写法

```ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 组合式写法：id + setup 函数
export const useCounterStore = defineStore('counter', () => {
  // ref  → state
  const count = ref(0)
  // computed → getter
  const double = computed(() => count.value * 2)
  // 函数 → action
  function increment(): void {
    count.value++
  }
  return { count, double, increment }
})
```

对照的 options 写法（了解即可）：

```ts
export const useCounterStore = defineStore('counter', {
  state: () => ({ count: 0 }),
  getters: { double: (state) => state.count * 2 },
  actions: { increment() { this.count++ } }
})
```

组合式写法的好处：与 `<script setup>` 心智一致、TS 类型自动推导、可以在 store 里用其他 composable。唯一要求：**所有 state/getter/action 必须在 return 里返回**，漏返回就等于不存在。Pinia 要求 store id 全局唯一——SelfBlog 的 `user.ts` 里 store id 是 `'auth'`，导出的名字却是 `useUserStore`（兼容别名），这提示我们 **store id 与函数名是两回事**，devtools 里显示的是 id。

### 5.7.3 在组件里使用 store

```vue
<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ❌ 直接解构 reactive 对象会丢失响应式
// const { user, isLoggedIn } = userStore

// ✅ state / getter 用 storeToRefs 解构
const { user, isLoggedIn } = storeToRefs(userStore)

// ✅ action / 普通函数直接解构没问题
const { logout } = userStore

// 模板里也可以直接 userStore.xxx 使用
</script>
```

`storeToRefs` 是初学 Pinia 必记的 API——它只把 state 和 getter 转成 ref，跳过函数。

### 5.7.4 $patch、$subscribe 与 $onAction

```ts
const userStore = useUserStore()

// $patch：批量修改，只触发一次订阅通知（比多次赋值高效）
userStore.$patch({ user: newUser, remember: true })
userStore.$patch((state) => {
  state.unreadNotifications += 3
})

// $subscribe：监听 state 变化（做日志/持久化）
userStore.$subscribe((mutation, state) => {
  localStorage.setItem('user-snapshot', JSON.stringify(state))
})

// $onAction：监听 action 调用（埋点、统一 loading）
userStore.$onAction(({ name, args, after, onError }) => {
  const start = Date.now()
  after((result) => console.log(`${name} 成功，耗时 ${Date.now() - start}ms`))
  onError((err) => console.error(`${name} 失败`, err))
})
```

### 5.7.5 在组件外使用 store

只要 **pinia 实例已被安装**（`app.use(pinia)` 之后），任何模块都可以直接调用 store。SelfBlog 的 `api/index.ts` 就在 Axios 拦截器里使用 store：

```ts
// 请求拦截器（api/index.ts 节选）
let token: string | null = null
try {
  const userStore = useUserStore()
  token = userStore.token
} catch {
  // pinia 尚未初始化时回退到 storage
  token = getToken(STORAGE_KEY_PREFIX + 'token')
}
```

两个工程细节值得学习：一是拦截器在应用启动早期可能执行，此时 pinia 还没装上，所以用 try/catch **优雅回退**；二是优先从 store 读 token（保持与 reactive 状态同步），storage 只是兜底。

如果需要在**应用启动前**（`app.use(pinia)` 之前）使用 store，可以显式传入 pinia 实例：SelfBlog 的 `main.ts` 里 `const siteStore = useSiteStore(pinia)` 就是这种写法——先 `createPinia()` 拿到实例，把它传给 store 调用，而不依赖 app 安装。

### 5.7.6 持久化

Pinia 本身不持久化，常见两条路：

1. 插件 `pinia-plugin-persistedstate`：`defineStore('id', { state, persist: true })`，配置灵活，适合大量 store。
2. **手动持久化**（SelfBlog 采用）：在需要的 action 里显式写 storage，配合编解码工具。好处是每个字段"存哪里、怎么加密、何时清"完全可控。

SelfBlog 的存储工具链：`utils/crypto.ts` 提供 `encodeData/decodeData`（对敏感值做编解码，避免 token 裸奔在 storage 里）、`safeGetItem/safeSetItem/safeRemoveItem`（封装 try/catch，隐私模式或 storage 被禁时不抛错）。`constants/index.ts` 里 `STORAGE_KEY_PREFIX = 'edu_'` 统一了所有键前缀。5.12 节会看到各 store 的具体持久化策略。

---

## 5.8 Axios 网络请求封装

### 5.8.1 基本用法

```ts
import axios from 'axios'

// GET
const res = await axios.get('/api/post/1')
// POST
await axios.post('/api/post', { title: '新文章' })
// 并发
const [a, b] = await axios.all([axios.get('/api/a'), axios.get('/api/b')])
// 取消
const controller = new AbortController()
axios.get('/api/slow', { signal: controller.signal })
controller.abort()
```

但业务项目**绝不直接用全局 axios**，而是 `axios.create()` 一个实例，统一配置并挂拦截器。

### 5.8.2 创建实例

```ts
// frontend/src/api/index.ts（节选）
const api: AxiosInstance = axios.create({
  baseURL: '/api',     // 所有请求自动带 /api 前缀
  timeout: 15000       // 15 秒超时
})
```

注意 `baseURL` 是**硬编码的 `/api`**——项目里的环境变量 `VITE_API_BASE_URL` 实际上没有任何代码消费，请求能否到达后端靠的是开发期 Vite 代理和生产期 `vercel.json` 反向代理（或同源单 JAR 部署）。这提醒我们：**配置项写了不等于生效**，审查时要追一下读取点。

### 5.8.3 响应拦截器：返回后端"信封"而非 AxiosResponse

后端统一返回 `{ code, message, data, timestamp }` 信封结构（`Result<T>`）。SelfBlog 的响应拦截器做了一件必须理解的事——**把 `response.data`（信封）直接作为拦截器返回值**：

```ts
api.interceptors.response.use(
  (response) => {
    logger.debug('Response success', { url: response.config.url, status: response.status })
    return response.data          // ← 关键：返回信封本身
  },
  async (error) => { /* 见下文 */ }
)
```

于是调用方拿到的类型要写对：

```ts
// ❌ 错误心智：以为拿到的是 AxiosResponse
const res = await api.get('/post/1')
// res.data.data  ← 多剥了一层，undefined

// ✅ 正确心智：api.get<T>() 解析出的就是 { code, message, data }
const res = await api.get<Post>('/post/1')
console.log(res.data)        // 这才是 Post 对象
console.log(res.message)     // 后端消息
```

`types/` 里的 `ApiResponse<T>` 就是对应的 TS 类型。**这一条是 SelfBlog 前端最容易踩的契约**，所有 API 模块（`api/post.ts`、`api/site.ts`……）与所有页面都遵守"拦截器已剥掉一层 HTTP 壳，`res.data` 即业务数据"的约定。

### 5.8.4 请求拦截器：注入鉴权与追踪头

```ts
api.interceptors.request.use((config) => {
  // 1) 优先从 store 读取 token（与响应式状态同步；pinia 未就绪时回退 storage）
  let token: string | null = null
  try {
    token = useUserStore().token
  } catch {
    token = getToken(STORAGE_KEY_PREFIX + 'token')   // decodeData(safeGetItem(...))
  }

  // 2) Token 指纹校验：检测 token 是否被复制到其他域使用
  if (token && !verifyTokenFingerprint(token)) {
    logger.error('Token fingerprint mismatch - possible theft attempt')
    useUserStore().resetState()
    throw new Error('Token security violation')
  }

  // 3) 注入 Authorization
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }

  // 4) CSRF 防护：X-XSRF-TOKEN（无则生成并保存）
  const xsrfToken = getXsrfToken() || generateXsrfToken()
  if (xsrfToken) config.headers['X-XSRF-TOKEN'] = xsrfToken

  // 5) 访客标识与日志追踪
  config.headers['X-Visitor-Id'] = getVisitorId()
  config.headers['X-Trace-Id'] = generateTraceId()

  return config
})
```

这一层把"每个请求都要带的东西"集中处理，业务代码从此不需要关心鉴权头。`getToken` 的实现也值得一读：`decodeData(safeGetItem(key)) || decodeData(safeGetItem(key, 'sessionStorage'))`——先试 `localStorage` 再试 `sessionStorage`，因为"记住我"勾选与否决定了 token 存在哪端。

### 5.8.5 401 与无感刷新：isRefreshing + failedQueue

Access Token 过期时（后端返回 401），最佳体验是**自动用 Refresh Token 换新 token 并重放原请求**，用户毫无感知。难点在于并发：一页上同时发了 5 个请求都撞上 401，不能刷新 5 次。SelfBlog 的解法是经典的"单飞（single-flight）+ 排队重放"：

```ts
// api/index.ts（节选，401 处理）
let isRefreshing = false
let failedQueue: Array<{ resolve: (token: string) => void; reject: (error: unknown) => void }> = []

// 刷新完成后，放行（或拒绝）排队中的所有请求
const processQueue = (error: unknown, token: string | null = null): void => {
  failedQueue.forEach((prom) => (error ? prom.reject(error) : prom.resolve(token!)))
  failedQueue = []
}

if (error.response?.status === 401 && !originalRequest._retry) {
  // 刷新接口自身 401 → refreshToken 也失效，直接登出
  if (originalRequest.url === '/user/refresh') {
    await handleAuthError()
    return Promise.reject(error)
  }

  // 已有刷新在进行 → 后来的 401 请求入队等待新 token
  if (isRefreshing) {
    return new Promise<string>((resolve, reject) => {
      failedQueue.push({ resolve, reject })
    }).then((token) => {
      originalRequest.headers.Authorization = `Bearer ${token}`
      return api(originalRequest)      // 用新 token 重放
    })
  }

  originalRequest._retry = true        // 防止重放再次 401 时死循环
  isRefreshing = true
  try {
    const refreshTokenValue = useUserStore().refreshToken ?? getToken(STORAGE_KEY_PREFIX + 'refreshToken')
    if (!refreshTokenValue) throw new Error('No refresh token')

    const response = await api.post('/user/refresh', null, {
      headers: { Authorization: `Bearer ${refreshTokenValue}` }
    })
    const { token: newToken, refreshToken: newRefreshToken } =
      (response as unknown as ApiResponse<{ token: string; refreshToken: string }>).data ?? {}

    // 双写：storage（编解码）+ store（响应式），并更新指纹
    const storage = useUserStore().remember ? localStorage : sessionStorage
    storage.setItem(STORAGE_KEY_PREFIX + 'token', encodeData(newToken)!)
    storage.setItem(STORAGE_KEY_PREFIX + 'refreshToken', encodeData(newRefreshToken)!)
    useUserStore().updateTokens(newToken, newRefreshToken)

    processQueue(null, newToken)                 // 放行队列
    originalRequest.headers.Authorization = `Bearer ${newToken}`
    return api(originalRequest)                  // 重放当前请求
  } catch (refreshError) {
    processQueue(refreshError, null)             // 队列全体失败
    await handleAuthError()
    return Promise.reject(refreshError)
  } finally {
    isRefreshing = false                         // 无论如何复位闸门
  }
}
```

理解这段代码的四个支点：

1. `_retry` 标记防止"重放又 401 → 再刷新"的无限循环。
2. `isRefreshing` 保证同一时刻只有一个刷新请求在飞。
3. `failedQueue` 把并发撞上 401 的请求挂起成 Promise，刷新成功后统一拿到新 token 重放；失败则统一 reject。
4. `finally` 里复位 `isRefreshing`，否则一次异常会让后续所有 401 永远排队。

### 5.8.6 handleAuthError：清理与跳转

刷新彻底失败（refreshToken 也无效）时，需要"登出"并送用户回登录页：

```ts
async function handleAuthError(): Promise<void> {
  // 1) 清空两端的认证数据（localStorage + sessionStorage）
  safeRemoveItem(STORAGE_KEY_PREFIX + 'token')
  safeRemoveItem(STORAGE_KEY_PREFIX + 'refreshToken')
  safeRemoveItem(STORAGE_KEY_PREFIX + 'user')
  // ...sessionStorage 同理，clearSecurityStorage() 再清指纹与 xsrf

  // 2) 复位 Pinia（store 不可用时静默忽略）
  try {
    useUserStore().resetState()
  } catch { /* store may not be available */ }

  // 3) 跳回 adminRoot（动态后台路径），带 redirect 参数；路由不可用时整页跳转兜底
  const adminRoot = `/${(await import('@/stores/site')).useSiteStore().adminPath}`
  if (window.location.pathname !== adminRoot) {
    try {
      const { default: router } = await import('@/router')
      const redirectPath = window.location.pathname + window.location.search
      router.push({ path: adminRoot, query: { redirect: redirectPath !== adminRoot ? redirectPath : undefined } })
    } catch {
      window.location.href = adminRoot
    }
  }
}
```

注意 `await import(...)` 动态导入的用意：**延迟加载模块、避免循环依赖**（api ↔ store ↔ router 互相引用时，顶层 import 会形成环）。

### 5.8.7 网络错误重试与统一错误文案

响应拦截器还承担两类杂务：

- **网络层重试**：对"无响应"类错误（`Network Error`、`ECONNABORTED` 超时等，`RETRYABLE_CODES` 白名单 + 取消除外）自动重试，`MAX_RETRIES = 2` 次，退避间隔 `RETRY_DELAY * attempt`（1000ms × 次数）。重试计数挂在 `config._networkRetryCount` 上。**只有幂等的读请求适合自动重试**——SelfBlog 网络层重试针对的是无响应错误，写操作（POST 提交）在后端幂等设计前提下才安全，理解这一边界很重要。
- **`error.userMessage` 文案**：按 HTTP 状态映射中文提示（400「请求参数错误」、429「操作过于频繁」、403「没有访问权限」、404「请求的资源不存在」、500「服务器内部错误」），并优先采用后端 `response.data.message`。组件层拿到错误直接 `toast.error(err.userMessage)`，不必各自写一遍文案映射。

---

## 5.9 自定义指令与组合式函数

### 5.9.1 自定义指令 directives

当你要"直接操作 DOM 元素"（聚焦、滚动显隐、拖拽），指令比组件更贴切。指令是一个带生命周期钩子的对象：

```ts
import type { Directive } from 'vue'

interface RevealOptions { threshold?: number; once?: boolean }

const vScrollReveal: Directive<HTMLElement, RevealOptions | undefined> = {
  mounted(el, binding) {
    // el：绑定元素；binding.value：传给指令的值；binding.modifiers：修饰符
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          el.classList.add('visible')
          if (binding.value?.once !== false) observer.unobserve(el)
        }
      })
    }, { threshold: binding.value?.threshold ?? 0.1 })
    observer.observe(el)
  }
}
```

```vue
<template>
  <div v-scroll-reveal class="fade-in-up">滚动到可视区时浮现</div>
  <div v-scroll-reveal="{ threshold: 0.3 }">自定义阈值</div>
</template>
```

全局注册（SelfBlog 在 `main.ts` 中）：

```ts
import { vScrollReveal } from './composables/useScrollReveal'
app.directive('scroll-reveal', vScrollReveal)
```

`<script setup>` 里声明 `const vFocus = {...}` 则自动注册为局部指令 `v-focus`（变量名前缀 `v` 即指令名）。常用钩子：`created`、`beforeMount`、`mounted`、`beforeUpdate`、`updated`、`beforeUnmount`、`unmounted`——与组件生命周期同名但作用于"被绑定元素"。

### 5.9.2 组合式函数 composable

**组合式函数**是"用了 Composition API 的可复用函数"，约定以 `use` 开头。它是指令之外的另一种复用维度：复用的是**有状态的逻辑**（响应式数据 + 行为），而不是 DOM 操作。

```ts
// composables/useScrollState.ts（示意）
import { ref, onMounted, onBeforeUnmount } from 'vue'

export function useScrollState() {
  const scrolled = ref(false)
  const onScroll = () => (scrolled.value = window.scrollY > 10)
  onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
  onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
  return { scrolled }
}
```

使用：

```vue
<script setup lang="ts">
const { scrolled } = useScrollState()
</script>
<template>
  <nav :class="{ 'nav--scrolled': scrolled }">…</nav>
</template>
```

SelfBlog 的 `composables/` 目录是一套很好的教科书：`useSeoMeta` / `usePageSeo`（页面 SEO 元信息）、`useMarkdownEnhance`（代码复制/折叠增强）、`usePageConfig`（逐页配置消费）、`useDialog`（弹层焦点、Esc 与滚动锁）、`useScrollReveal`（同时导出 composable 版本和指令版本 `vScrollReveal`）。

组合式函数三条纪律：

1. **在 `setup`/`<script setup>` 中同步调用**（内部要注册生命周期钩子）；
2. **输入输出尽量响应式**（参数收 `MaybeRef`，返回 ref/computed）；
3. **自清理**：内部注册的全局监听/Observer/定时器必须在 `onBeforeUnmount`/`onUnmounted` 里撤销，否则组件卸载后变成幽灵回调。

### 5.9.3 案例：useConfirm 的"函数式确认框"

SelfBlog 的 `composables/useConfirm.ts` 展示了一个精巧模式——**用 composable 返回"行为 + 渲染件"**：

```ts
export function useConfirm() {
  const isOpen = ref(false)
  const msg = ref('')
  const dialogTitle = ref('')
  let resolver: ((value: boolean) => void) | null = null

  onBeforeUnmount(() => {           // 组件卸载时未决的 Promise 兑现为 false
    resolver?.(false)
    resolver = null
  })

  const confirm = (message: string, title = '确认操作'): Promise<boolean> => {
    resolver?.(false)               // 已有未决确认 → 先按取消结算
    msg.value = message
    dialogTitle.value = title
    isOpen.value = true
    return new Promise<boolean>((resolve) => (resolver = resolve))
  }

  const ConfirmDialog = defineComponent({ /* 渲染 Modal + 确认/取消按钮，
      点击后 isOpen=false 并 resolver(true/false) */ })

  return { confirm, ConfirmDialog }
}
```

使用方式：

```vue
<script setup lang="ts">
import { useConfirm } from '@/composables/useConfirm'
const { confirm, ConfirmDialog } = useConfirm()

async function removePost(): Promise<void> {
  const ok = await confirm('确定删除这篇文章吗？')
  if (ok) await postApi.remove(id)
}
</script>
<template>
  <button @click="removePost">删除</button>
  <!-- 必须渲染到模板里，否则确认框不出现 —— SelfBlog 文档特别强调 -->
  <ConfirmDialog />
</template>
```

它把"命令式调用（`await confirm()`）"与"声明式渲染（`<ConfirmDialog />`）"缝合起来：调用侧代码干净得像原生 `window.confirm`，渲染侧又完全可控。注意卸载钩子里的 `resolver(false)`——组件被销毁时不能留下永远 pending 的 Promise，否则 `await` 之后的代码永远不会执行，这是异步生命周期管理的细节功力。

---

## 5.10 Vue 3 性能与进阶

### 5.10.1 v-memo：模板级记忆化

```vue
<!-- 仅当 item.id、item.text、selectedId 任一变化时才重新 diff 这个子树 -->
<div v-for="item in list" :key="item.id" v-memo="[item.id, item.text, item.id === selectedId]">
  <p>{{ item.text }}</p>
  <span>{{ item.id === selectedId ? '已选中' : '' }}</span>
</div>
```

`v-memo` 缓存一段模板的虚拟 DOM，依赖数组不变时整段跳过 diff。适合"超长列表中的少数项频繁变化"的场景。注意：**依赖数组必须覆盖子树里用到的所有响应式数据**，漏一个就会出现"界面不更新"的诡异 bug——它是高阶优化，默认不要到处加。

### 5.10.2 shallowRef 与 markRaw

Vue 3 的深度响应式有成本：大对象、第三方实例被 Proxy 包裹既慢又可能出问题。

```ts
import { shallowRef, markRaw, triggerRef } from 'vue'

// shallowRef：只有 .value 的【整体替换】触发更新，内部属性不追踪
const bigList = shallowRef<HugeItem[]>([])
bigList.value = [...bigList.value, item]   // ✅ 整体替换触发更新
bigList.value.push(item)                    // ❌ 不触发更新（内部未代理）
triggerRef(bigList)                         // 手动强制触发

// markRaw：标记一个对象永不转为响应式（放图表实例、地图实例等）
import * as echarts from 'echarts'
const chart = markRaw(echarts.init(el))
```

选择原则：数据量大且总是整体替换 → `shallowRef`；存放带复杂内部状态的外部实例（Chart、Editor、Audio）→ `markRaw`。SelfBlog 的 `stores/music.ts` 中 `let audio: HTMLAudioElement | null` 干脆用了**普通模块级变量**而非 ref——因为 audio 元素是外部宿主对象，其状态通过手动维护的 `playing/loading/currentTime` 等 ref 投影给 UI，这是"哪些该响应式、哪些不该"的清醒示范。

### 5.10.3 defineAsyncComponent 与路由懒加载

```ts
import { defineAsyncComponent } from 'vue'

const HeavyEditor = defineAsyncComponent({
  loader: () => import('@/components/admin/HeavyEditor.vue'),
  loadingComponent: Spinner,       // 加载中占位
  errorComponent: ErrorBox,        // 加载失败占位
  delay: 200,                      // 超过 200ms 才显示 loading（防闪烁）
  timeout: 30000                   // 超时视为失败
})
```

路由组件天然支持懒加载（`() => import(...)`），Vite 会自动按路由切分 chunk。SelfBlog 的 `vite.config.ts` 还用 `manualChunks` 手动分了组：`vendor: ['vue', 'vue-router', 'pinia']` 与 `markdown: ['marked', 'dompurify']`——把变更频率低的框架代码与 Markdown 库分离开，利于浏览器长期缓存。

### 5.10.4 Suspense：等待异步依赖

```vue
<template>
  <Suspense>
    <template #default>
      <Dashboard />   <!-- setup 是 async 的组件：内部 await 数据 -->
    </template>
    <template #fallback>
      <p>加载中…</p>
    </template>
  </Suspense>
</template>
```

`<script setup>` 可以写 `await`（组件变成异步组件），`Suspense` 负责在其就绪前展示 fallback。Suspense 仍属实验特性，生产使用要保持关注版本行为；SelfBlog 未使用它，而是采用"先挂载、数据在 `onMounted`/store action 里异步拉取"的模式，配合骨架屏，对初学者更直白。

### 5.10.5 Teleport：渲染到组件树之外

```vue
<template>
  <!-- 弹层 DOM 传送到 #overlay-root，但逻辑仍在本组件 -->
  <Teleport to="#overlay-root">
    <div v-if="show" class="modal">…</div>
  </Teleport>
</template>
```

价值：弹层样式不再被父级 `overflow: hidden`、`transform`、`z-index` 层叠上下文困住。SelfBlog 特别约定：模态层挂到主题根内的 `#overlay-root`，**不要传送到裸 `body`**——这样弹层仍在主题与皮肤的作用域内（`#selfblog[data-skin=…]` 的变量对它生效），暗色模式也不需要重复适配。

### 5.10.6 keep-alive：组件状态保鲜

```vue
<template>
  <RouterView v-slot="{ Component }">
    <KeepAlive :include="['PostList']" :max="10">
      <component :is="Component" />
    </KeepAlive>
  </RouterView>
</template>
```

被缓存的组件切换时不销毁，状态（滚动位置、表单草稿）原地保留；对应生命周期 `onActivated`/`onDeactivated` 取代 `onMounted`/`onUnmounted` 的部分职责（回到页面时刷新数据放在 `onActivated`）。`include/exclude` 按组件名过滤，`max` 限制缓存数量防内存膨胀。注意：一旦被缓存，`onUnmounted` 里的清理不会在切换时执行——全局监听若注册在 `onMounted`，对称的移除要考虑 `onDeactivated`。

### 5.10.7 过渡与动画：Transition 与 TransitionGroup

Vue 内置了两个过渡组件，用**声明式**的方式给元素和列表加动画，不需要手写监听器：

```vue
<template>
  <!-- 单元素/组件过渡：v-if、v-show 切换时自动应用过渡类 -->
  <Transition name="fade">
    <p v-if="visible">淡入淡出</p>
  </Transition>

  <!-- 列表过渡：配合 v-for，移除项有离场动画 -->
  <TransitionGroup name="list" tag="ul">
    <li v-for="item in items" :key="item.id">{{ item.text }}</li>
  </TransitionGroup>
</template>

<style scoped>
/* name="fade" 对应 fade-enter-* / fade-leave-* 六个类 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 列表：入场、离场、移动三种类 */
.list-enter-active {
  transition: all 0.3s ease;
}
.list-leave-active {
  transition: all 0.2s ease;
  position: absolute; /* 离场项脱离文档流，其余项才能平滑移动 */
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
.list-move {
  transition: transform 0.3s ease; /* 其余项"让位"的移动动画 */
}
</style>
```

工作原理：`Transition` 在元素插入/移除的时机自动追加 `v-enter-from → v-enter-active → v-enter-to`（入场）与 `v-leave-from → v-leave-active → v-leave-to`（离场）六个类，动画完成后移除。常见坑位：

1. **`TransitionGroup` 的子项必须有 `key`**，否则动画与复用都会错乱；
2. 离场动画要求元素"还占着位置"才能渐隐，配合 `position: absolute` 让其余项用 `.list-move` 平滑补位；
3. `mode="out-in"` / `mode="in-out"` 控制新旧元素交替顺序，路由过渡 `<RouterView v-slot="{ Component }"><Transition mode="out-in"><component :is="Component" /></Transition></RouterView>` 最常用 `out-in`，避免两个页面重叠闪跳；
4. 用 CSS `animation` 时写 `*-enter-active` 的 keyframes 即可；用 JS 钩子（`@before-enter`/`@enter`/`@leave`）可以做复杂序列动画，但别忘了在 `@enter`/`@leave` 里调用 `done()`。

SelfBlog 没有引入动画库，而是把常用动效沉淀成全局工具类（`.fade-in`、`.slide-up`、`.pulse`，见 `styles/`）配合 `v-scroll-reveal` 指令的 `.visible` 类切换——**CSS 过渡 + 类切换**已覆盖绝大多数场景，Transition 组件留给条件渲染和路由切换这类"元素生灭"的时机。

---

## 5.11 实战案例：剖析 SelfBlog 的启动流程

现在把所有知识串起来，逐行读 SelfBlog 的 `src/main.ts`——这个文件只有 57 行，但**每一行的顺序都是承重的**：

```ts
// frontend/src/main.ts（节选，保留顺序）
const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

app.directive('scroll-reveal', vScrollReveal)          // 全局指令
app.config.errorHandler = (err, _instance, info) => {  // 全局错误兜底 → logger
  logger.error('Vue error', { error: (err as Error).message, info })
}
window.addEventListener('unhandledrejection', ...)     // 全局 Promise 兜底
window.onerror = (...)

async function bootstrap() {
  const siteStore = useSiteStore(pinia)        // ① 显式传 pinia：此时还没 app.mount
  await siteStore.loadPublicSettings()         // ② 等待站点设置（含 adminPath）
  installAdminRoutes(siteStore.adminPath)      // ③ 按服务端下发的路径注册后台路由

  useUserStore(pinia).restoreFromStorage()     // ④ 恢复登录态（必须在 use(router) 之前！）
  app.use(router)                              // ⑤ 安装路由 → 触发首次导航与 beforeEach
  app.mount('#app')                            // ⑥ 挂载

  void usePagesStore(pinia).loadManifest()     // ⑦ 可选的页面配置，异步、不阻塞
  logger.info('Application initialized', ...)
}

void bootstrap().catch((error) => {
  logger.error('Application bootstrap failed', { error: (error as Error).message })
  app.mount('#app')                            // 引导失败也要挂载：宁可显示默认站点
})
```

**为什么顺序不能乱？逐条拆解：**

1. **② 必须在 ③ 之前**：`loadPublicSettings()` 用 `fetch('/api/site/public')` 拉取站点设置，其中 `adminPath` 决定后台入口路径（默认 `admin`，站点设置页可改）。没拿到它就不知道要把后台路由注册到哪里。
2. **③ 必须在 ⑤ 之前**：后台路由**不在静态路由表里**，而是 `installAdminRoutes` 里循环 `router.addRoute(...)` 动态注册的。如果先 `app.use(router)`，路由器会立即执行**首次导航**——此时后台路由还不存在，作者直接访问 `/admin/dashboard` 就会 404。这就是"运行时动态注册路由"的实战代价：**addRoute 必须发生在首次导航前**。
3. **④ 必须在 ⑤ 之前**：`restoreFromStorage()` 从 localStorage/sessionStorage 解码出 token 与用户对象写进 Pinia。若在 `use(router)` 之后才恢复，首次导航执行 `beforeEach` 时 `userStore.isLoggedIn` 还是 `false`，守卫会把**其实已经登录的作者**重定向回后台入口（登录页）——"每次刷新都弹回登录页"这类经典 bug 的根源就在这个顺序。
4. **⑦ 放在 mount 之后且用 `void` 不 await**：页面配置（自定义导航、内容页）是**可选增强**，拉取失败时站点照常可用（`pages.ts` 的 `loadManifest` 内部 catch 吞掉错误）。它不该阻塞登录态恢复与首屏渲染；`void` 前缀则显式表达"我故意不等待这个 Promise"。
5. **bootstrap 的 catch 里兜底 mount**：即使站点设置拉取流程抛出意外，也要把 App 挂上去——SelfBlog 的 store 全部带默认值（`site.ts` 的 `defaults` 对象），断网时用默认站点标题与 `/admin` 后台路径照常渲染。

再看 `loadPublicSettings` 内部的健壮性处理，同样值得抄进自己的项目：

```ts
async function loadPublicSettings(): Promise<void> {
  try {
    const response = await fetch('/api/site/public', { headers: { Accept: 'application/json' } })
    if (!response.ok) return                      // 非 2xx：静默保留本地默认值
    const envelope = (await response.json()) as { data?: Partial<SiteSettings> }
    if (envelope.data) applySettings(envelope.data)   // 应用设置 + 同步 title/favicon/皮肤
  } catch {
    // Keep the local defaults available when the API is temporarily offline.
  }
}
```

启动期请求的特点是"失败不许炸启动"，所以这里用的是原生 `fetch` 而不是带拦截器的 `api` 实例——避免 401 刷新队列等机制在应用尚未就绪时被牵连。

### 5.11.1 启动顺序的"反证实验"：打乱会发生什么

理解顺序最有效的方法是推演"打乱后会坏"。把四种典型错误排列逐一想一遍：

```text
错误排列 A：先 app.use(router) 再 loadPublicSettings()
→ 首次导航发生时 adminPath 还是默认值。假设服务端配置的后台路径是 "console"，
  作者收藏夹里的 /console/dashboard 在首次导航时没有对应路由记录 → 404。
  随后 installAdminRoutes 把后台路由注册到了默认 /admin 下，
  与服务端期望彻底错位，刷新也无法自愈。

错误排列 B：先 app.use(router) 再 restoreFromStorage()
→ 首次 beforeEach 执行时 userStore.token 还是 null，isLoggedIn === false。
  守卫把 /admin/dashboard 重定向到 adminRoot 并附上 redirect 参数。
  紧接着 restoreFromStorage() 恢复了登录态——但导航已经判过了，
  作者被弹到后台入口页。这就是"每次刷新都要求重新登录"的经典 bug，
  而且很难排查：因为单独看守卫和 store 都"没有错"。

错误排列 C：await loadManifest() 放在 mount 之前
→ 页面配置接口慢（或后端恰好重启）时，整个应用白屏等它。
  它明明是"可选增强"，却拿到了"阻塞主流程"的权力。
  正确做法是 mount 之后 void usePagesStore(pinia).loadManifest()。

错误排列 D：bootstrap 里不 await loadPublicSettings()，直接继续
→ installAdminRoutes(siteStore.adminPath) 拿到的是默认 'admin'，
  等价于排列 A。注意这里"await 了"与"没 await 但代码顺序对"是两回事——
  JS 的 async 函数里，忘写 await 的调用会立刻返回 Promise，
  后续语句照常执行，顺序在物理上就乱了。
```

还有一个隐蔽的时序点：`bootstrap().catch(...)` 里的兜底 `app.mount('#app')`。如果 `loadPublicSettings` 或路由注册抛出意外（比如 fetch 实现异常、`crypto` 不可用），不兜底的话用户看到的就是**永久白屏**；兜底后至少默认站点能渲染。这体现了一条启动期工程原则：**启动链路上的每一环都应该有"降级但仍可用"的出口**——store 默认值、`!response.ok` 静默返回、catch 吞错，三层防线缺一不可。

最后补充一个初学者常问的问题：为什么 `useSiteStore(pinia)` 要把 pinia 当参数传？因为 store 的内部实现依赖"当前激活的 Pinia 实例"。在组件里调用时，Pinia 能从组件实例上自动推断出这个实例；但在 `bootstrap` 这种**组件树之外**的代码里，只能显式传入。`app.use(pinia)` 之后全局有一个"活动实例"，普通模块代码就可以不带参数调用了——`api/index.ts` 的拦截器里 `useUserStore()` 不传参，正是依赖了这一机制（并以 try/catch 兜住"pinia 还没装"的极端时序）。

---

## 5.12 实战案例：SelfBlog 的五个 Pinia Store

### 5.12.1 site.ts：站点设置与后台路径

```ts
export const useSiteStore = defineStore('site', () => {
  const settings = ref<SiteSettings>({ ...defaults })
  const adminPath = computed(() => settings.value.adminPath || 'admin')
  const adminRoot = computed(() => `/${adminPath.value}`)
  const isCommentsEnabled = computed(() => settings.value.commentsEnabled === 'true')
  const isMusicEnabled = computed(() => settings.value.musicEnabled === 'true' && !!settings.value.musicUrl)

  function applySettings(input: Partial<SiteSettings> | Record<string, string>): void {
    settings.value = { ...settings.value, ...input }
    applyAppearance()      // 同步皮肤 data-skin 与 --site-primary 变量到 #selfblog
    document.title = settings.value.siteTitle || defaults.siteTitle
    applyFavicon(settings.value.faviconUrl || '')   // 幂等注入/清理 <link rel=icon>
  }
  // loadPublicSettings 见 5.11
})
```

看点：**KV 型站点设置零迁移**——后端 `site_setting` 是 KV 表，前端用 `Partial<SiteSettings>` 合并，新增设置键不需要动数据结构；`adminPath/adminRoot` 用 computed 派生并处处复用（守卫、API 层、`/login` 重定向全都读它），保证"后台路径"只有一个事实来源。`isMusicEnabled` 这类布尔派生把 `'true'`/`'false'` **字符串**转成真布尔——KV 表存的都是字符串，转换收敛在 store 一处。

### 5.12.2 theme.ts：暗色模式与系统偏好

```ts
type ThemeMode = 'light' | 'dark' | 'system'

export const useThemeStore = defineStore('theme', () => {
  const isDarkMode = ref(false)
  const theme = ref<ThemeMode>('light')
  let mediaQuery: MediaQueryList | null = null
  let mediaHandler: (() => void) | null = null

  const currentTheme = computed<'light' | 'dark'>(() => {
    if (theme.value === 'system') {
      return (mediaQuery || window.matchMedia('(prefers-color-scheme: dark)')).matches
        ? 'dark' : 'light'
    }
    return theme.value
  })

  function initTheme(): void {
    let savedTheme: ThemeMode = 'light'
    try {
      savedTheme = (localStorage.getItem(STORAGE_KEY_PREFIX + 'theme') as ThemeMode) || 'light'
    } catch { /* storage unavailable */ }
    theme.value = savedTheme
    destroyTheme()                 // 先清理旧监听，防重复注册
    applyTheme()
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaHandler = () => { if (theme.value === 'system') applyTheme() }
    mediaQuery.addEventListener('change', mediaHandler)
  }

  function applyTheme(): void {
    isDarkMode.value = currentTheme.value === 'dark'
    document.documentElement.classList.toggle('dark-mode', isDarkMode.value)
  }

  function toggleTheme(): void {   // light → dark → system 三态循环
    theme.value = theme.value === 'light' ? 'dark' : theme.value === 'dark' ? 'system' : 'light'
    try { localStorage.setItem(STORAGE_KEY_PREFIX + 'theme', theme.value) } catch {}
    applyTheme()
  }
})
```

要点：持久化键是 `edu_theme`（`STORAGE_KEY_PREFIX + 'theme'`）；`system` 模式靠 `matchMedia('(prefers-color-scheme: dark)')` 的 `change` 监听跟随系统；暗色通过在 `<html>` 上挂 `.dark-mode` 类生效（`dark-mode.css` 会整体反转灰度梯度）。所有 storage 访问都包 try/catch——隐私模式下 `localStorage` 会抛异常，**偏好设置绝不能让应用崩溃**。`initTheme` 先调 `destroyTheme()` 清旧监听再注册新的，避免重复初始化造成监听器叠加。

### 5.12.3 user.ts：认证状态（store id 是 'auth'）

```ts
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const user = ref<User | null>(null)
  const remember = ref(false)

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  function saveToStorage(): void {
    const storage = remember.value ? localStorage : sessionStorage
    const otherStorage = remember.value ? sessionStorage : localStorage
    // 清掉另一端的旧数据，防止切换"记住我"后残留旧 token
    otherStorage.removeItem(STORAGE_KEY_PREFIX + 'token')
    /* ... */
    storage.setItem(STORAGE_KEY_PREFIX + 'token', encodeData(token.value)!)
  }

  function restoreFromStorage(): void { /* 解码并恢复，失败则 clearStorage() */ }

  async function validateToken(): Promise<boolean> {
    // 只解析 JWT 的 payload.exp 做本地过期判断，不发网络请求
    const parts = token.value!.split('.')
    const payload = JSON.parse(atob(parts[1]!.replace(/-/g, '+').replace(/_/g, '/')))
    return !(payload.exp && payload.exp * 1000 < Date.now())
  }
  /* login / logout / updateTokens / detectTokenTheft ... */
})

// 兼容别名：全站引用的都是这个名字
export const useUserStore = useAuthStore
```

四个设计决策值得咀嚼：

1. **store id 是 `'auth'`，对外别名 `useUserStore`**——再次印证 id 与导出名是两回事；改 id 会让 devtools 里历史记录"搬家"，用别名保持 API 稳定。
2. **token 存取经过 `utils/crypto` 的 `encodeData/decodeData`**，不裸奔在 storage；"记住我"决定存 localStorage（跨会话）还是 sessionStorage（关标签即失效），并且两端互斥清理。
3. **`validateToken` 是纯本地校验**（base64 解 payload 看 `exp`），不请求网络——守卫里用它做"秒级过期判断"，真正的服务端鉴权交给每个请求的 401 → 刷新链路。剩余寿命小于 5 分钟时仅打日志"下次请求会刷新"。
4. **`detectTokenTheft` 与 API 层的指纹校验呼应**：storage 里的 token 与内存不一致即判定可能被篡改，直接重置状态。

### 5.12.4 music.ts：唯一音频宿主

```ts
export const useMusicStore = defineStore('music', () => {
  const source = ref(''); const title = ref('背景音乐')
  const playing = ref(false); const loading = ref(false); const error = ref('')
  const currentTime = ref(0); const duration = ref(0)
  const volume = ref(0.65); const loop = ref(true)
  let audio: HTMLAudioElement | null = null   // ← 外部宿主：不是 ref，markRaw 都不用

  function attach(element: HTMLAudioElement | null): void {
    // 把 <audio> 元素接到 store：注册 guarded 监听器（playing/pause/ended/
    // waiting/loadedmetadata/timeupdate/error），把媒体事件翻译成上面的 ref 状态
  }

  function configure(configuration: MusicConfiguration): void {
    const next = configuration.enabled && /^(https?:\/\/|\/uploads\/)/i.test(url) ? url : ''
    if (next === source.value) return          // 换页/重复下发不重置播放器
    attach(null); source.value = next
  }

  function setSuspended(value: boolean): void {
    if (suspended === value) return
    suspended = value
    if (value) pause()                          // 进入后台 → 暂停
  }
  /* play/pause/seek/setVolume/setLoop ... */
})
```

这是"store 管理外部资源"的高级范例：全站**只有一个 `<audio>` 元素**（常驻 `App.vue` 的 headless `MusicPlayer` 组件），Navbar 的播放控件、后台的启停设置都只是读写这个 store，不存在第二个音频实例打架；换路由时音乐连续播放（公开页），进入后台自动暂停（`setSuspended`）；音量/循环偏好持久化在 `selfblog-music-preferences`。它还演示了大量防御性细节：`revision` 计数让过期的 `play()` Promise 失效、`NotAllowedError`（浏览器自动播放限制）翻译成友好提示、storage 全程 try/catch。

### 5.12.5 pages.ts：只保存已发布页面配置

```ts
export const usePagesStore = defineStore('pages', () => {
  const pages = ref<Record<string, PublicPage>>({})
  const navigation = ref<PublicNavigationItem[]>([...defaultNavigation])
  let manifestRequest = 0   // 请求代际计数：只认最新一次清单请求

  async function loadManifest(): Promise<void> {
    const request = ++manifestRequest
    try {
      const response = await pageApi.getManifest()
      if (request !== manifestRequest) return   // 迟到的旧响应直接丢弃
      pages.value = Object.fromEntries(response.data.pages.map((page) => [page.id, page]))
      navigation.value = response.data.navigation
    } catch { /* 可选配置：失败保持现状 */ }
  }
  /* loadPage / loadSlug，同样带代际守卫 */
})
```

三条铁律：**只存已发布（published）配置，草稿绝不进公开 store**，也不写 localStorage（草稿只活在后台编辑器/预览上下文）；清单与单页请求都带**代际计数防竞态**——先发出的慢请求不能覆盖后发出的新数据；`defaultNavigation` 兜底，后端没配置导航时导航栏照样渲染"首页/归档/相册/关于"。

---

## 5.13 测试 Vue 组件（Vitest + @vue/test-utils）

SelfBlog 前端用 **Vitest 4** + **@vue/test-utils 2** + jsdom，维护着 **43 个测试文件 / 400 个测试**的基线。Vitest 与 Vite 共享同一套配置与转换管线，`vite.config.ts` 里的 `test` 字段就是它的配置：

```ts
// vite.config.ts（节选）
test: {
  globals: true,                        // describe/it/expect 不用 import
  environment: 'jsdom',                 // 模拟浏览器 DOM
  setupFiles: ['src/test/setup.ts'],    // 每个测试文件前执行
  include: ['src/**/*.{test,spec}.{js,ts,jsx,tsx}'],
  coverage: { provider: 'v8', include: ['src/**/*.{js,ts,vue}'] }
}
```

全局配置 `src/test/setup.ts` 刻意保持最小：

```ts
import '@testing-library/jest-dom'
import { config } from '@vue/test-utils'
import { vi } from 'vitest'

config.global.stubs = {}
config.global.mocks = {
  $route: { path: '/', params: {}, query: {} },
  $router: { push: vi.fn(), replace: vi.fn() }
}

// jsdom 没有 matchMedia，只桩这一个浏览器 API
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false, media: query, onchange: null,
    addListener: vi.fn(), removeListener: vi.fn(),
    addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn()
  }))
})
```

写一个组件测试（`BackToTop.test.ts` 是仓库里的真实例子）：

```ts
import { afterEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, mount } from '@vue/test-utils'
import BackToTop from '../BackToTop.vue'

enableAutoUnmount(afterEach)                 // 每个用例后自动卸载组件
afterEach(() => vi.restoreAllMocks())

describe('back to top motion', () => {
  it('reduced motion 时不用平滑滚动', async () => {
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: true } as MediaQueryList)
    const scroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    const wrapper = mount(BackToTop)
    await wrapper.get('button').trigger('click')
    expect(scroll).toHaveBeenCalledWith({ top: 0, behavior: 'auto' })
  })
})
```

从 SelfBlog 的实践中沉淀下来的四条测试军规：

1. **挂载会注册全局监听器的组件，必须自己 `enableAutoUnmount(afterEach)`**。测试环境没有自动卸载，组件不卸载，`onMounted` 里挂到 `window` 的监听器就会泄漏到后面的用例，造成"单跑通过、全跑挂掉"的幽灵失败。
2. **路由优先用真实的 `createMemoryHistory()`**，不要 `stubs: { RouterLink: true }`。stub 会丢掉 RouterLink 的插槽内容，也表达不了 active 状态；内存 history 不改地址栏，是测试里跑真路由的标准姿势：

   ```ts
   const router = createRouter({ history: createMemoryHistory(), routes })
   await router.push('/') ; await router.isReady()
   mount(App, { global: { plugins: [router] } })
   ```
3. **jsdom 缺口只桩最小集合**：目前只有 `matchMedia`（主题监听、reduced-motion 检测都依赖它）。遇到 `IntersectionObserver`、`ResizeObserver` 等再按需在用例里补，不预装大而全的 polyfill，免得掩盖真实问题。
4. **异步渲染要 await**：改状态后 `await nextTick()` 或对触发用 `await wrapper.find('button').trigger('click')`；涉及 rAF 的动画逻辑不能桩成同步，要排队后手动 flush；改 `window.scrollY` 用 `Object.defineProperty(window, 'scrollY', { value: 100, configurable: true })` 再派发 `scroll` 事件。

Store 测试更简单：Pinia store 本身就是函数集合，`setActivePinia(createPinia())` 后直接调用 `useXxxStore()` 断言状态流转，不需要挂载任何组件（`stores/__tests__/theme.test.ts`、`music.test.ts` 都是这么写的）。

再补一个真实项目里绕不开的场景——**测试会注册全局监听的 store 逻辑**（以主题监听为例）：

```ts
import { describe, it, expect, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '../theme'

afterEach(() => {
  vi.restoreAllMocks()
  setActivePinia(createPinia())   // 每个用例用全新的 pinia，避免状态串场
})

describe('theme store', () => {
  it('toggleTheme 在 light → dark → system 间循环并写入 storage', () => {
    setActivePinia(createPinia())
    const theme = useThemeStore()

    const storageSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {})
    theme.toggleTheme()
    expect(theme.theme).toBe('dark')
    theme.toggleTheme()
    expect(theme.theme).toBe('system')
    expect(storageSpy).toHaveBeenCalledWith('edu_theme', 'system')
  })
})
```

这里的两个细节都来自真实教训：`vi.spyOn(Storage.prototype, 'setItem')` 要 mock 成空实现，否则 jsdom 的 storage 行为可能在不同用例间留下脏数据；`setActivePinia(createPinia())` 放进 `afterEach`，因为 Pinia 的状态是模块级单例，上一个个用例改过的登录态会让下一个个用例"凭空已登录"。断言里直接写 `'edu_theme'` 而不是引用常量，是刻意为之——**测试要钉死的是对外可见的存储键名**，将来有人改常量导致键名漂移时，测试会第一时间报警。

---

## 5.14 常见错误与最佳实践清单

**响应式陷阱**

1. JS 里忘写 `.value`：`count++` 而不是 `count.value++`，界面纹丝不动且无报错。
2. 解构 `reactive` 对象丢响应式——用 `toRefs`（store 用 `storeToRefs`）。
3. 对 `reactive` 的数组/对象属性做整体替换后丢失追踪；需要整体替换请用 `ref`。
4. `watch` 直接侦听 `reactive` 对象时新旧值是同一引用；要字段级对比用 getter 形式。
5. 大数组高频更新卡顿：考虑 `shallowRef` + 整体替换，或 `v-memo`。

**模板与组件**

6. `v-for` 用 `index` 当 key，列表重排后状态错位；用稳定业务 id。
7. `v-if` 和 `v-for` 同标签连用（Vue 3 中 `v-if` 先执行，拿不到遍历变量）；先 `<template v-for>` 再内部 `v-if`。
8. 在子组件里直接改 props；正确姿势是 emit 或 `defineModel`。
9. 事件监听/定时器/Observer 在 `onMounted` 注册却不在 `onBeforeUnmount` 移除——内存泄漏。
10. `v-html` 渲染用户内容不过 DOMPurify——XSS。

**路由与状态**

11. 在 `<script setup>` 里用 `this.$router`——组合式 API 里用 `useRouter()/useRoute()`（且 `route` 不是响应式解构友好的，监听参数用 `watch(() => route.params.id, ...)`）。
12. 动态 `addRoute` 发生在 `app.use(router)` 之后——首次导航 404（SelfBlog 启动顺序专门为此设计）。
13. 刷新后登录态丢失：忘了在路由安装前 `restoreFromStorage()`，首个 `beforeEach` 把人弹回登录页。
14. Pinia 解构丢响应式：`const { x } = store` 要改 `storeToRefs(store)`。
15. store id 与导出名混为一谈（SelfBlog：id `'auth'`，导出 `useUserStore`），devtools 排查时注意。

**网络层**

16. 以为 `api.get()` 返回 AxiosResponse，多剥一层 `res.data.data`——SelfBlog 拦截器已返回信封，读 `res.data` 即可。
17. 并发 401 触发多次刷新：必须 `isRefreshing` + `failedQueue` 单飞排队；`finally` 里忘了复位 `isRefreshing` 是变体 bug。
18. 刷新重放没加 `_retry` 标记，401 → 刷新 → 重放 401 死循环。
19. 启动期请求（站点设置）复用业务 axios 实例，被 401 逻辑牵连炸掉启动——启动期用裸 `fetch` + 全静默兜底。

**工程化**

20. `tsconfig` 开 `noUncheckedIndexedAccess` 后 `arr[0].foo` 过不了类型检查——用 `arr[0]?.foo`，或 `replace` 替代 `split()[0]`（SelfBlog 的 `mimeForIcon` 是范例）。
21. storage 访问裸奔不包 try/catch，隐私模式直接崩；统一走 `safeGetItem` 一类封装并加键前缀。
22. 每次改代码后不跑闸门——`lint:check`、`format:check`、`type-check`、`test:run`、`build` 五项全绿再提交，守住"43 文件 / 400 测试"基线。

**最佳实践小结**

- 组合式 API + `<script setup>` + TypeScript 为默认风格；逻辑复用优先抽 composable（`use` 前缀、自清理）。
- 状态分层：组件私有状态留在组件；跨组件/跨路由/跨会话才进 Pinia；持久化字段明确"存哪端、怎么编码、何时清"。
- 网络层集中封装：信封契约、鉴权注入、刷新单飞、错误文案统一在 `api/index.ts` 一处完成。
- 启动顺序是契约：**拉设置 → 注册动态路由 → 恢复登录态 → 装路由 → 挂载 → 异步可选配置**。
- 测试守最小桩原则：jsdom 只补真实缺口，路由用真 router，监听器组件配 `enableAutoUnmount`。

---

## 本章总结

本章以 SelfBlog 真实源码为主线，完整过了一遍 Vue 3 全家桶：

1. **语言与工具**：Vue 3 渐进式框架、Composition API 与 Options API 的取舍、Vite 的开发体验与项目结构。
2. **模板语法**：插值、`v-bind`/`v-on` 及简写、`v-if`/`v-show`、`v-for` 与 key、`v-model` 与修饰符。
3. **组合式 API**：`ref`/`reactive` 及解包规则、`toRef`/`toRefs`、`computed`、`watch`/`watchEffect`、生命周期与清理责任。
4. **组件体系**：SFC 三段结构、props 校验、emits、组件 v-model、三种插槽、provide/inject，以及一张组件通信选型表。
5. **路由与状态**：Vue Router 4 的动态/嵌套路由、编程式导航、meta 守卫与动态 `addRoute`；Pinia 的组合式 store、`$patch`/`$subscribe`、组件外使用与持久化策略。
6. **网络层**：Axios 实例与拦截器、`{code,message,data}` 信封契约、`isRefreshing + failedQueue` 无感刷新、`handleAuthError` 清理跳转。
7. **复用与性能**：自定义指令、组合式函数、`v-memo`、`shallowRef`/`markRaw`、异步组件、Suspense、Teleport、keep-alive。
8. **实战剖析**：`main.ts` 承重的启动顺序及其原因；五个 store（site/theme/user/music/pages）各自的设计决策。
9. **质量保障**：Vitest + @vue/test-utils 的四条军规，以及 22 条常见错误清单。

下一章我们将继续深入前端工程化：构建优化、部署契约与 CI 闸门。

---

## 5.15 进阶专题：组合式函数（Composables）的设计模式

5.9 节入门了组合式函数，这一节把它讲成**体系**——这是 Vue 3 代码复用的核心形态，也是面试高频考点。

### 5.15.1 为什么是组合式函数：从 mixin 的失败说起

Vue 2 的 mixin 有三个致命伤：**来源不明**（组件里的 `this.loading` 从哪个 mixin 来的？）、**命名冲突**（两个 mixin 都有 `fetchData`）、**隐式耦合**（mixin 依赖组件里的某个 data，删了组件字段 mixin 就静默坏掉）。组合式函数把这三个问题全部消灭：

```typescript
// composables/useLike.ts——来源显式（import 进来的）、命名空间隔离（返回值解构改名）、
// 依赖显式（参数列表就是全部依赖）
export function useLike(postId: Ref<number>) {
  const liked = ref(false)
  const likeCount = ref(0)
  const submitting = ref(false)

  async function toggle() {
    if (submitting.value) return
    submitting.value = true
    const prev = liked.value
    // 乐观更新：先改 UI，失败回滚（SelfBlog LikeButton 的核心逻辑）
    liked.value = !liked.value
    likeCount.value += prev ? -1 : 1
    try {
      const res = await api.post(`/anonymous-likes/posts/${postId.value}`)
      liked.value = res.data.liked            // 服务端校准
      likeCount.value = res.data.likeCount
    } catch {
      liked.value = prev                       // 回滚
      likeCount.value += prev ? 1 : -1
    } finally {
      submitting.value = false
    }
  }

  return { liked, likeCount, submitting, toggle }
}
```

组件里三行接入：`const { liked, likeCount, toggle } = useLike(toRef(props, 'id'))`——**逻辑与 UI 分离**，点赞逻辑可以脱离组件单测。

### 5.15.2 设计规范五条（Vue 官方约定 + SelfBlog 实践）

1. **命名 `useXxx`**：约定即文档，看到 use 前缀就知道返回的是响应式状态 + 操作函数
2. **入参支持 ref 与裸值**：用 `toValue()`（Vue 3.3+）统一解包，`useLike(toRef(props,'id'))` 与 `useLike(123)` 都能用
3. **返回普通对象而非 reactive**：调用方解构不丢响应性（`useXxx()` 返回 reactive 对象再解构就全断了）；配合 `storeToRefs` 的原理一脉相承
4. **副作用必须在 onUnmounted 清理**：事件监听、定时器、EventSource——组合式函数里注册的，也要在组合式函数里卸载
5. **异步状态三件套齐备**：`data / loading / error` 缺一不可，调用方不用自己包 try-catch

### 5.15.3 输入输出范式：useXxx 的四种形状

```typescript
// 形状一：状态 + 操作（useLike）——交互逻辑
// 形状二：数据获取（useXxxQuery）——loading/error/data 三件套
export function usePostList(pageNum: Ref<number>) {
  const { data, loading, error } = useAsyncData(
    () => api.getPosts(pageNum.value),
    { watch: [pageNum] }               // pageNum 变化自动重新拉取
  )
  return { records: computed(() => data.value?.records ?? []), loading, error }
}

// 形状三：浏览器 API 封装（useDarkMode / useMediaQuery）——与环境打交道
export function useDarkMode() {
  const isDark = ref(matchMedia('(prefers-color-scheme: dark)').matches)
  const mq = matchMedia('(prefers-color-scheme: dark)')
  const handler = (e: MediaQueryListEvent) => { isDark.value = e.matches }
  onMounted(() => mq.addEventListener('change', handler))
  onUnmounted(() => mq.removeEventListener('change', handler))   // 规范第 4 条
  return { isDark }
}

// 形状四：全局单例（useUserStore 这类 Pinia store 本质是全局组合式函数）
// 何时需要「跨组件共享」：同一条数据被 3+ 个无父子关系的组件使用时，从 composable 升级为 store
```

**composable 与 store 的分界线**：`useLike` 是**组件级**的（每个文章详情页一份独立状态），`useUserStore` 是**应用级**的（全局唯一）。判断标准：「这份状态离开组件还有意义吗？」——登录态有（store），某页的点赞提交中有（composable）。

### 5.15.4 竞态防护：组合式函数的必修课

数据获取型 composable 必须处理**竞态**（race condition）：快速切换分页时，page=2 的慢响应可能覆盖 page=3 的快响应，展示错数据。SelfBlog 的 `stores/pages.ts` 用「代际号（manifestRequest）」防竞态，组合式函数版：

```typescript
export function useSearch(keyword: Ref<string>) {
  const results = ref<SearchItem[]>([])
  let requestSeq = 0                     // 代际号：每次请求自增

  async function run() {
    const seq = ++requestSeq             // 领号
    const res = await api.search(keyword.value)
    if (seq !== requestSeq) return       // 过号：已不是最新请求，丢弃
    results.value = res.data
  }
  watch(keyword, run)

  return { results }
}
```

**代际号的本质**：给每个异步操作发号，落账时验号——号不对说明自己已经过期。这比「取消上一个请求」（AbortController）更简单通用，SelfBlog 的 music store 用 revision 防「过期 Promise 回写」是同一个思想。

---

## 5.16 响应式原理：ref 为什么能「自动更新」

用了一整章的 `ref`/`reactive`，这一节掀开桌布看底牌——理解原理不是为了造轮子，而是为了**预判边界**（什么情况不会更新）。

### 5.16.1 Proxy：Vue 3 的响应式地基

Vue 2 用 `Object.defineProperty` 劫持属性的 get/set，有三个先天缺陷：新增/删除属性侦测不到（所以要有 `Vue.set`）、数组索引赋值侦测不到（所以 `arr[0] = x` 不更新）、初始化时递归全部属性（大对象初始化慢）。Vue 3 换用 **Proxy** 代理整个对象：

```typescript
const state = reactive({ count: 0 })
// 大致等价于（简化版）：
const proxy = new Proxy(target, {
  get(target, key) {
    track(target, key)        // 读取时：记录「谁在用我」
    return Reflect.get(target, key)
  },
  set(target, key, value) {
    Reflect.set(target, key, value)
    trigger(target, key)      // 写入时：通知所有「用我的人」
    return true
  }
})
```

两步闭环：**track（依赖收集）**——组件渲染函数读了 `state.count`，渲染函数就被记为 count 的依赖；**trigger（派发更新）**——写入 `state.count = 1` 时，通知所有依赖重新执行。`ref` 的原理同源，只是对基本类型没有对象可代理，包了一层 `{ value }` 再对这层对象做代理——这就是为什么必须 `.value`。

### 5.16.2 从原理预判「为什么不更新」的五种情况

| 场景 | 原因 | 正确写法 |
|------|------|---------|
| 解构 reactive 对象后用 | 解构取走的是**普通值**，脱离了 Proxy | `toRefs(state)` 或整体使用 |
| props 解构后用 | 同上，props 是 reactive | 4.x 响应式 Props 解构可用，或 `toRef(props, 'x')` |
| 替换整个 reactive 对象 | Proxy 挂在原对象上，换对象断链 | 逐字段赋值，或改用 `ref` |
| 直接改数组元素不更新 | （Vue 3 已修复——Proxy 全拦截，此条是 Vue 2 遗留认知） | Vue 3 放心用 `arr[i] = x` |
| ref 忘了 `.value` | 拿到的是包装对象不是值 | 模板里自动解包，JS 里必须 `.value` |

SelfBlog 的 `stores/pages.ts` 用 `manifestRequest` 代际号、`stores/music.ts` 用 revision，都是建立在「写入必触发更新」这一保证之上的——**原理清楚了，模式才用得笃定**。

### 5.16.3 computed 与 watch：两种响应的消费姿态

- **computed**：声明「值由什么算来」，**有缓存**——依赖不变不重算，模板里反复用也只算一次。适合「派生数据」（SelfBlog 的 `siteStore.adminRoot` 派生自 `adminPath`）。
- **watch**：声明「变化后做什么**副作用**」，无缓存、可异步。适合「请求、DOM 操作、localStorage 写入」（SelfBlog 的 `themeStore` watch 暗色状态写 `edu_theme`）。

判断口诀：**能用 computed 就不用 watch**——computed 是数据流（声明式、可缓存、无时序坑），watch 是事件流（命令式、有先后）。见到的「watch 里改另一个 ref 再被第三个 watch 监听」的链式写法，几乎都能用 computed 一条流收干净。

### 5.16.4 响应式的代价：性能心智

Proxy 拦截一切读写，单次开销微小但**遍历大列表渲染时会被放大**。三条节流纪律：大列表渲染前先用 computed 算好，避免模板里调用昂贵方法（每次重渲染都执行）；超长列表用虚拟滚动（6.8.10 的诚实评估）；**深层大对象只读不追踪**用 `shallowRef`/`markRaw`（SelfBlog 的 music store 存音频元数据对象时同理——原始 AudioBuffer 这类重型对象绝不该进深层响应式）。

理解原理的最高境界是**不再把它当魔法**：每个「为什么界面没刷新」的 bug，都能在 track/trigger 的模型里找到唯一解释。

---

## 参考资源

- [Vue 3 官方文档（中文）](https://cn.vuejs.org/)
- [Vue Router 4 官方文档](https://router.vuejs.org/zh/)
- [Pinia 官方文档](https://pinia.vuejs.org/zh/)
- [Vite 官方文档](https://cn.vitejs.dev/)
- [Axios 官方文档](https://axios-http.com/zh/docs/intro)
- [Vitest 官方文档](https://cn.vitest.dev/)
- [Vue Test Utils 官方文档](https://test-utils.vuejs.org/zh/)
- [TypeScript 官方文档](https://www.typescriptlang.org/docs/)
- [SelfBlog 项目源码](https://github.com/Xinghe-0203/SelfBlog)
- SelfBlog 前端关键源文件：`frontend/src/main.ts`、`frontend/src/api/index.ts`、`frontend/src/router/index.ts`、`frontend/src/router/guards.ts`、`frontend/src/stores/*.ts`、`frontend/vite.config.ts`、`frontend/src/test/setup.ts`

---

# 第六章 前端工程化

> 本章以 SelfBlog 项目（Vue 3.5 + Vite 6 + TypeScript 6.0 + ESLint + Prettier + Vitest）为实战案例，系统讲解现代前端工程化的完整链路。读者应具备基本的 HTML/CSS/JavaScript 知识和 Vue 3 基础。

---

## 6.1 Node.js 与 npm 基础

### 6.1.1 Node.js 版本管理

现代前端项目对 Node.js 版本有严格要求。SelfBlog 项目通过 `package.json` 的 `engines` 字段声明了版本约束：

```json
{
  "engines": {
    "node": "^22.22.2 || ^24.15.0 || >=26.0.0"
  }
}
```

这个约束的含义是：Node.js 版本必须满足 `22.22.2` 及以上（但小于 `23.0.0`）、或 `24.15.0` 及以上（但小于 `25.0.0`）、或 `26.0.0` 及以上。

**为什么需要版本管理？**

不同 Node.js 版本对 ES 模块、原生 API、构建工具链的支持不同。Vite 6 要求 Node.js 18+ 或 20+，而 TypeScript 6.0 的某些特性需要更新的运行时支持。版本管理确保团队所有成员和 CI 环境使用一致的运行时。

**nvm（Node Version Manager）实战：**

```bash
# 安装 nvm（Windows 使用 nvm-windows）
# 安装指定版本
nvm install 22.22.2
nvm install 24.15.0

# 切换版本
nvm use 22.22.2

# 查看已安装版本
nvm list

# 在项目根目录创建 .nvmrc 文件
echo "22.22.2" > .nvmrc
nvm use  # 自动读取 .nvmrc
```

**fnm（Fast Node Manager）替代方案：**

```bash
# 安装 fnm
winget install Schniz.fnm

# 使用 fnm
fnm install 22
fnm use 22
fnm default 22
```

### 6.1.2 包管理：npm 核心概念

npm（Node Package Manager）是 Node.js 的默认包管理器。理解其核心概念是前端工程化的基础。

**package.json 结构解析（以 SelfBlog 为例）：**

```json
{
  "name": "selfblog",
  "version": "2.0.26",
  "private": true,
  "type": "module",
  "scripts": { ... },
  "dependencies": {
    "axios": "^1.7.4",
    "dompurify": "^3.0.9",
    "marked": "^12.0.1",
    "pinia": "^2.1.7",
    "vue": "^3.5.44",
    "vue-router": "^4.3.0"
  },
  "devDependencies": {
    "@eslint/js": "^9.39.4",
    "@testing-library/jest-dom": "^6.9.1",
    "@testing-library/vue": "^8.1.0",
    "@vitejs/plugin-vue": "^5.0.4",
    "@vue/test-utils": "^2.4.10",
    "@vue/tsconfig": "^0.9.1",
    "eslint": "^9.39.4",
    "eslint-config-prettier": "^10.1.8",
    "eslint-plugin-vue": "^10.9.1",
    "jsdom": "^29.1.1",
    "prettier": "^3.8.3",
    "typescript": "^6.0.3",
    "typescript-eslint": "^8.60.1",
    "vite": "^6.4.4",
    "vitest": "^4.1.8",
    "vue-tsc": "^3.3.3"
  }
}
```

**关键字段说明：**

| 字段 | 说明 |
|------|------|
| `name` | 包名，发布到 npm 时的标识 |
| `version` | 语义化版本号（SemVer） |
| `private` | `true` 表示私有包，不会被意外发布 |
| `type` | `"module"` 表示使用 ES 模块（import/export） |
| `scripts` | 可通过 `npm run <name>` 执行的命令 |
| `dependencies` | 生产环境依赖 |
| `devDependencies` | 开发环境依赖（构建、测试、检查工具） |
| `engines` | 运行时版本约束 |

**语义化版本（SemVer）规则：**

版本号格式为 `主版本.次版本.修订版本`（如 `2.0.26`）：

- **主版本（Major）**：不兼容的 API 变更
- **次版本（Minor）**：向下兼容的功能新增
- **修订版本（Patch）**：向下兼容的问题修复

**版本范围符号：**

| 符号 | 含义 | 示例 |
|------|------|------|
| `^` | 兼容主版本 | `^3.5.44` → `>=3.5.44 <4.0.0` |
| `~` | 兼容次版本 | `~3.5.44` → `>=3.5.44 <3.6.0` |
| `>=` | 大于等于 | `>=2.0.0` |
| `*` | 任意版本 | `*` → 最新版本 |
| 无符号 | 精确版本 | `3.5.44` → 仅 `3.5.44` |

**npm install vs npm ci：**

```bash
# npm install：安装依赖，可能更新 package-lock.json
npm install

# npm ci：严格按 package-lock.json 安装，不修改锁文件
# 速度更快，适合 CI 环境
npm ci
```

`npm ci` 的优势：
1. **确定性**：严格按锁文件安装，保证所有环境一致
2. **速度**：跳过依赖解析，直接安装
3. **安全**：先删除 `node_modules` 再安装，避免残留
4. **CI 友好**：如果锁文件与 `package.json` 不一致会报错

### 6.1.3 脚本运行与 npm scripts

npm scripts 是前端工程化的核心枢纽。SelfBlog 定义了完整的脚本链：

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "build:vercel-proxy": "node scripts/build-vercel-proxy.mjs",
    "test:deployment": "node --test scripts/__tests__/*.test.mjs",
    "preview": "vite preview",
    "lint": "eslint src --fix",
    "lint:check": "eslint src",
    "format": "prettier --write \"src/**/*.{ts,tsx,vue,css,json}\"",
    "format:check": "prettier --check \"src/**/*.{ts,tsx,vue,css,json}\"",
    "type-check": "vue-tsc --noEmit",
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage",
    "test:ui": "vitest --ui"
  }
}
```

**脚本分类与用途：**

| 类别 | 脚本 | 用途 |
|------|------|------|
| 开发 | `dev` | 启动 Vite 开发服务器（端口 3000） |
| 构建 | `build` | 类型检查 + 生产构建 |
| 构建 | `build:vercel-proxy` | 生成 Vercel 代理配置 |
| 检查 | `lint:check` | ESLint 只读检查 |
| 检查 | `format:check` | Prettier 格式检查 |
| 检查 | `type-check` | TypeScript 类型检查 |
| 修复 | `lint` | ESLint 自动修复 |
| 修复 | `format` | Prettier 自动格式化 |
| 测试 | `test:run` | Vitest 单次运行 |
| 测试 | `test:coverage` | 带覆盖率报告 |
| 测试 | `test:deployment` | 部署契约测试 |

**脚本执行顺序（build 链）：**

```bash
npm run build
# 等价于：
# 1. vue-tsc --noEmit  （类型检查）
# 2. vite build         （生产构建）
```

`&&` 表示前一个命令成功（退出码 0）才执行下一个。如果类型检查失败，构建不会执行。

**环境变量注入：**

```bash
# 开发环境
VITE_API_TARGET=http://localhost:8825 npm run dev

# 或者使用 .env 文件
echo "VITE_API_TARGET=http://localhost:8825" > .env
npm run dev
```

### 6.1.4 包管理器对比：npm vs pnpm vs yarn

| 特性 | npm | pnpm | yarn |
|------|-----|------|------|
| 安装速度 | 中等 | 最快 | 快 |
| 磁盘占用 | 高 | 低（硬链接） | 中 |
| 幽灵依赖 | 有 | 无（严格隔离） | 有 |
| 锁文件 | package-lock.json | pnpm-lock.yaml | yarn.lock |
| Monorepo | 支持（workspaces） | 原生支持 | 支持（workspaces） |

SelfBlog 使用 npm，因为它是 Node.js 内置的，无需额外安装，且 `package-lock.json` 提供了足够的确定性。

### 6.1.5 依赖背后的真相：package-lock.json 与幽灵依赖

**package-lock.json 为什么必须提交进仓库**：`package.json` 里的 `^1.7.4` 表示「1.7.4 及以上、2.0 以下都可以」——每次安装可能拿到不同的小版本。锁文件把整棵依赖树**精确到版本号 + 下载地址 + 完整性哈希**固定下来。不提交锁文件的团队，会在某个周一发现「我这里好好的，你那里装出来就是坏的」。SelfBlog CI 里用 `npm ci`（而非 `npm install`）正是为了**严格按锁文件装，装不对就报错**：

| 命令 | 行为 | 适用 |
|------|------|------|
| `npm install` | 按 package.json 语义化范围解析，可能更新锁文件 | 本地开发加新依赖 |
| `npm ci` | 删掉 node_modules 按锁文件精确安装，不改锁文件 | CI、重置环境 |

**幽灵依赖（Phantom Dependencies）**：你只装了 `vite`，但代码里 `import { something } from 'esbuild'` 居然能跑——因为 npm 的扁平化 node_modules 把**依赖的依赖**也提升到了顶层。这些「你没声明却能 import」的包就是幽灵依赖：某天 vite 升级换了内部依赖，你的代码凭空编译失败。pnpm 用严格隔离（每个包只能看见自己声明的依赖）从结构上消灭它；npm 项目则靠纪律：**import 只允许 package.json 里声明过的包**（ESLint 的 `no-extraneous` 规则可以机器检查）。

### 6.1.6 Monorepo 认知：SelfBlog 为什么是「两个工程」而不是 monorepo

SelfBlog 的 `backend/` 与 `frontend/` 是两个完全独立的工程：后端 Maven 管依赖、前端 npm 管依赖，**没有根 package.json、没有 workspace 配置**。这是刻意的选择：

- **部署形态决定**：后端打 jar、前端产物内嵌 jar，两端生命周期不同步，强行 monorepo 只增加构建复杂度
- **技术栈无共享**：Java 与 TS 没有共享代码，monorepo 的核心收益（跨包原子提交、共享构建配置）在这里为零

什么时候值得上 monorepo（pnpm workspace / turborepo / nx）：多个前端包共享组件库、多个服务共享类型定义、需要原子化跨包变更时。**判断标准是「有没有真实共享」，不是「看起来高大上」。**

### 6.1.7 package.json 逐字段精读：以 SelfBlog 的声明为例

`package.json` 是前端工程化的「宪法」，值得逐字段读一遍。以下是 SelfBlog `frontend/package.json` 的完整骨架（版本号以仓库实际为准）：

```json
{
  "name": "selfblog",
  "version": "2.0.26",
  "private": true,
  "type": "module",
  "scripts": { "...": "见 6.1.3" },
  "dependencies": {
    "axios": "^1.7.4",
    "dompurify": "^3.0.9",
    "marked": "^12.0.1",
    "pinia": "^2.1.7",
    "vue": "^3.5.44",
    "vue-router": "^4.3.0"
  },
  "devDependencies": {
    "eslint": "^9.39.4",
    "prettier": "^3.8.3",
    "typescript": "^6.0.3",
    "vite": "^6.4.4",
    "vitest": "^4.1.8"
  }
}
```

逐字段说明：

| 字段 | 作用 | SelfBlog 的取值理由 |
|------|------|--------------------|
| `name` / `version` | 包标识 | 个人项目不发布 npm，仅作标识 |
| `private: true` | **禁止发布到 npm 仓库** | 应用类项目的标准防误操作 |
| `type: "module"` | 声明 ESM：`.js` 文件按 ES 模块解析 | Vite 生态默认 ESM；`scripts/*.mjs` 构建脚本因此可用顶层 `import` |
| `engines` | 声明 Node 版本要求 | 配合 `.npmrc` 的 `engine-strict=true`，版本不符直接报错（见 6.1.1） |

**dependencies 与 devDependencies 的分界线**：前者是「应用运行时需要的包」——axios（请求）、vue/pinia/vue-router（框架三件套）、marked + dompurify（Markdown 渲染，产物里真实包含，所以必须在 dependencies）；后者是「开发与构建时需要的包」——ESLint/Prettier/TS/Vite/Vitest 装与不装只影响开发体验和构建流程，不影响运行时行为。判别问题只有一句话：**构建产物里会不会出现它的代码？会 → dependencies，不会 → devDependencies。**

常见误区：把 Vite 放进 dependencies（多数部署平台无所谓，但语义错了）；或者以为 devDependencies 在生产不装——`npm install --production` 才会跳过它们，普通 `npm install` 全都装。**SelfBlog CI 前端 job 用 `npm ci` 装全部依赖**，因为构建与测试恰恰是 CI 的主业。

**semver（语义化版本）速读**：`主版本.次版本.修订号`——主版本变更允许破坏兼容，次版本向下兼容地加功能，修订号只修 bug。`package.json` 里的前缀决定自动升级的边界：

```
^1.7.4   允许 1.x.x 内升级（>=1.7.4 <2.0.0）——npm install 的默认值
~1.7.4   只允许修订号升级（>=1.7.4 <1.8.0）——更保守
1.7.4    锁死精确版本
* / latest  任何版本——危险，几乎不该出现在工程里
```

`^` 的哲学是「信任次版本兼容承诺」，把精确性交给锁文件（6.1.5）：package.json 表达**意图**，package-lock.json 表达**事实**。

**node_modules 与依赖树速写**：`npm install` 完成后，`node_modules/.bin/` 存放所有包的可执行垫片（`vite`、`vitest`、`eslint`……npm scripts 里的裸命令就是从这里解析的——这也是 6.2.x PATH 惨案的物理现场：垫片脚本最终调用 `node`，PATH 坏了它就找不到）。每装一个包，npm 同时安装它声明的全部依赖，递归成树；npm v3+ 用**扁平化**把公共依赖尽量提升到顶层以减少重复——代价就是幽灵依赖（6.1.5）。想知道「为什么会有这个包」时用 `npm ls <pkg>`：它会打印从直接依赖到该包的完整引用链，排查版本冲突（同一包多个版本并存）的第一工具。

```bash
npm ls marked          # marked 被谁依赖、装了几个版本
npm dedupe             # 尝试合并重复版本，缩小依赖树
npm outdated           # 列出「已装版本 / 想要版本 / 最新版本」三列，升级前先看
```

**npm scripts 的进阶细节**（补 6.1.3）：`pre`/`post` 前缀钩子会自动执行（`pretest` 先于 `test`）；`npm run` 会把 `node_modules/.bin` 临时加进 PATH，所以脚本里能直接写 `vite` 而不用全路径；`npm test`、`npm start`、`npm stop` 是内置别名可省略 `run`；脚本间传参用 `--` 分隔——`npm run test -- --coverage` 的 `--coverage` 会透传给 vitest。环境变量注入见 6.1.3 结尾（`NODE_ENV`、`npm_package_*` 系列由 npm 自动提供）。

---

## 6.2 Vite 构建工具

### 6.2.1 Vite 的核心理念

Vite（法语"快"的意思）由尤雨溪创建，是下一代前端构建工具。其核心理念是**利用浏览器原生 ES 模块**，在开发环境下无需打包，实现毫秒级冷启动。

**传统打包工具（Webpack）的问题：**

```
启动时间 = 项目大小 × 打包速度
项目越大，启动越慢
```

**Vite 的解决方案：**

```
开发环境：浏览器原生 ESM + 按需编译
生产环境：Rollup 打包（Tree-shaking + 代码分割）
```

### 6.2.2 vite.config.ts 完整解析

SelfBlog 的 `vite.config.ts` 是一个功能完整的配置示例：

```typescript
/// <reference types="vitest" />
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['src/test/setup.ts'],
      include: ['src/**/*.{test,spec}.{js,ts,jsx,tsx}'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json', 'html'],
        include: ['src/**/*.{js,ts,vue}'],
        exclude: ['src/main.ts', 'src/**/*.test.ts', 'src/**/*.spec.ts']
      }
    },
    server: {
      port: 3000,
      fs: {
        allow: ['..']
      },
      proxy: {
        '/api': {
          target: env.VITE_API_TARGET || 'http://localhost:8825',
          changeOrigin: true
        },
        '/uploads': {
          target: env.VITE_API_TARGET || 'http://localhost:8825',
          changeOrigin: true
        }
      }
    },
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: false,
      minify: 'esbuild',
      rollupOptions: {
        output: {
          manualChunks: {
            vendor: ['vue', 'vue-router', 'pinia'],
            markdown: ['marked', 'dompurify']
          }
        }
      }
    }
  }
})
```

**配置项详解：**

**1. `defineConfig` 函数形式**

```typescript
// 对象形式（静态配置）
export default defineConfig({ ... })

// 函数形式（动态配置，可读取 mode 和 env）
export default defineConfig(({ mode }) => {
  return { ... }
})
```

函数形式的优势是可以根据 `mode`（development/production/test）和环境变量动态调整配置。

**2. `loadEnv` 加载环境变量**

```typescript
const env = loadEnv(mode, process.cwd(), '')
// 第三个参数 '' 表示加载所有环境变量（包括非 VITE_ 前缀的）
// 默认只加载 VITE_ 前缀的变量
```

**3. 插件系统**

```typescript
import vue from '@vitejs/plugin-vue'

plugins: [vue()]
```

`@vitejs/plugin-vue` 是 Vue 3 的官方 Vite 插件，负责：
- 编译 `.vue` 单文件组件（SFC）
- 处理 `<template>`、`<script>`、`<style>` 块
- 支持 HMR（热模块替换）

**4. 路径别名**

```typescript
resolve: {
  alias: {
    '@': resolve(__dirname, 'src')
  }
}
```

使用 `@` 代替 `src` 目录，避免相对路径过深：

```typescript
// 不使用别名
import { api } from '../../../api/index'

// 使用别名
import { api } from '@/api/index'
```

**5. 开发服务器配置**

```typescript
server: {
  port: 3000,
  fs: {
    allow: ['..']  // 允许访问父目录（用于读取仓库根共享的 fixture）
  },
  proxy: {
    '/api': {
      target: env.VITE_API_TARGET || 'http://localhost:8825',
      changeOrigin: true
    }
  }
}
```

**代理配置详解：**

| 选项 | 说明 |
|------|------|
| `target` | 代理目标地址 |
| `changeOrigin` | 修改请求头中的 `Host` 为目标地址 |
| `rewrite` | 重写路径（如 `rewrite: (path) => path.replace(/^\/api/, '')`） |
| `secure` | 是否验证 SSL 证书 |

**为什么需要代理？**

浏览器同源策略（CORS）阻止跨域请求。开发时前端运行在 `localhost:3000`，后端在 `localhost:8825`，通过 Vite 代理可以绕过 CORS 限制。

**6. 生产构建配置**

```typescript
build: {
  outDir: 'dist',           // 输出目录
  assetsDir: 'assets',      // 静态资源子目录
  sourcemap: false,         // 不生成 sourcemap（生产环境）
  minify: 'esbuild',        // 使用 esbuild 压缩（比 terser 快）
  rollupOptions: {
    output: {
      manualChunks: {
        vendor: ['vue', 'vue-router', 'pinia'],
        markdown: ['marked', 'dompurify']
      }
    }
  }
}
```

**代码分割（Code Splitting）：**

`manualChunks` 将第三方库拆分为独立 chunk：

```
dist/
├── assets/
│   ├── index-[hash].js      # 业务代码
│   ├── vendor-[hash].js     # Vue 生态（vue, vue-router, pinia）
│   └── markdown-[hash].js   # Markdown 渲染（marked, dompurify）
```

**为什么手动分割？**

1. **缓存优化**：第三方库变化频率低，浏览器可长期缓存
2. **并行加载**：多个 chunk 可并行下载
3. **首屏优化**：只加载必要的 chunk

### 6.2.3 Vite 插件生态

**常用插件：**

```typescript
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [
    vue(),
    // 其他插件
  ]
})
```

| 插件 | 用途 |
|------|------|
| `@vitejs/plugin-vue` | Vue 3 SFC 支持 |
| `@vitejs/plugin-react` | React JSX 支持 |
| `vite-plugin-eslint` | 开发时 ESLint 检查 |
| `vite-plugin-pwa` | PWA 支持 |
| `vite-plugin-compression` | 构建时 gzip/brotli 压缩 |
| `vite-plugin-html` | HTML 模板处理 |

### 6.2.4 环境变量与模式

Vite 支持多环境配置：

```
.env                # 所有环境
.env.local          # 所有环境（不提交 git）
.env.development    # 开发环境
.env.production     # 生产环境
.env.test           # 测试环境
```

**SelfBlog 的环境变量：**

```env
# .env
VITE_APP_TITLE=个人博客
VITE_API_TARGET=http://localhost:8825
VITE_API_BASE_URL=/api
VITE_APP_VERSION=2.0.25
```

**在代码中使用：**

```typescript
// 通过 import.meta.env 访问
const apiTarget = import.meta.env.VITE_API_TARGET
const isDev = import.meta.env.DEV      // 是否开发模式
const isProd = import.meta.env.PROD    // 是否生产模式
const mode = import.meta.env.MODE     // 当前模式
```

**类型声明（env.d.ts）：**

```typescript
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TARGET?: string
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

### 6.2.5 开发服务器与 HMR

**启动开发服务器：**

```bash
npm run dev
# 或
npx vite
```

**HMR（热模块替换）工作原理：**

```
1. 修改文件
2. Vite 检测文件变化
3. 通过 WebSocket 通知浏览器
4. 浏览器请求更新的模块
5. 只替换变化的模块，不刷新页面
```

**Vue SFC 的 HMR：**

```vue
<template>
  <div>{{ message }}</div>
</template>

<script setup lang="ts">
const message = 'Hello'
</script>

<style>
div { color: red; }
</style>
```

修改 `<template>` 时，Vite 只更新模板部分，保留组件状态。修改 `<style>` 时，只替换 CSS，不重新渲染组件。

### 6.2.x 本机环境的真实陷阱：PATH 引号惨案（来自 SelfBlog 开发机实录）

教程里的命令在你机器上「跑不起来」时，先怀疑环境本身。这是一个真实的、极其隐蔽的案例：

**症状**：`npm run dev`、`npm run build`、`npm run test:run` 全部失败，报错 `'"node"' 不是内部或外部命令`。但 `npm ci`、`npm install` 正常；`where node` 也能找到 node。

**根因**：系统 PATH 里有一条 `C:\Program Files\Java\jdk-25\bin"`——末尾多了个**不配对的引号**。`cmd.exe` 解析 PATH 时把它当成一段未闭合引号的开头，其后所有条目全部失效。`node` 本身靠 App Paths 侥幸能找到，但 `node_modules/.bin/*.cmd` 垫片脚本结尾调用的是裸 `node`，于是全军覆没。bash 不受影响（解析规则不同）。

**临时解法**（SelfBlog 仓库采用）：绕开 npm 脚本层，直接调用 POSIX 垫片：

```bash
cd frontend
./node_modules/.bin/vite              # = npm run dev
./node_modules/.bin/vue-tsc --noEmit  # = npm run type-check
./node_modules/.bin/eslint src        # = npm run lint:check
./node_modules/.bin/vitest run        # = npm run test:run
./node_modules/.bin/vite build        # 生产构建
```

**根治解法**：删掉 PATH 里那条多余的引号。**教训**：错误信息指向「node 不存在」时，别急着重装 Node——先用 `echo %PATH%`（cmd）逐条目视检查引号、分号是否配对完整。环境变量损坏的症状往往离根因很远。

### 6.2.y Vite 为什么快：esbuild 预构建与原生 ESM 深入

6.2.1 给了结论，这里补「为什么」。两个关键词撑起 Vite 的开发态速度：

**① esbuild 预构建（依赖预处理）**。首次启动（或升级依赖后）时，Vite 扫描入口的 `import`，把 `node_modules` 里的第三方包（vue、axios、marked…）用 **esbuild**（Go 语言编写，比 JS 系打包器快一个数量级）打包成单个 ESM 文件，缓存到 `node_modules/.vite`。它同时解决三个问题：

- **格式问题**：不少包发布的是 CommonJS 格式，浏览器原生 ESM 不认识——预构建把 CJS 转成 ESM；
- **请求数问题**：一个包内部可能拆成几百个小模块文件（如 lodash-es），逐个请求会把开发服务器打成瀑布——预构建「合多为一」；
- **速度问题**：依赖基本不变，缓存复用后二次启动几乎零成本；依赖变了 Vite 会自动重新预构建，排障时也可 `npx vite --force` 强制重跑。

**② 原生 ESM 开发服务器（源码按需编译）**。你写的 `src/` 模块不打包：每个模块就是一个 URL，浏览器请求 `/src/main.ts` 时 Vite 才按需把它编译成 JS 返回——**请求谁编译谁，没被请求的模块根本不参与**。因此启动时间与项目总大小无关，只与当前页面用到的模块数有关；HMR 快也是同一原理：改一个文件只需失效并重编译这一个模块，不像 Webpack 要重算整片受影响的依赖图。

一句话分工：**预构建管「第三方依赖」（启动时一次、缓存复用），按需编译管「你的源码」（请求时逐个）**。代价是开发态的请求瀑布——一个模块 import 十个依赖就是十连请求，HTTP/1.1 六连接限制下更明显；好在开发服务器走 localhost、HTTP/1.1 keep-alive 复用连接，实际可接受。

**生产构建为什么换 Rollup**：开发态要「快与随缘」，生产态要「最优产物」——Tree-shaking 摇掉未引用代码、按路由/手动策略做代码分割、成熟的 chunk 命名与资源内联。Vite 已把压缩交给 esbuild（`build.minify: 'esbuild'` 默认值，见 6.2.2），但整体打包仍是 Rollup。想验证 esbuild 的存在感：`npm run build` 输出里的转换速度、以及 6.8.6 产物分析中 chunk 体积——用 esbuild 压缩比 terser 快数倍且体积接近。

**环境变量与模式的工程细节**（补 6.2.4 的实战面）：

- `VITE_` 前缀是**安全边界**：只有带前缀的变量会注入 `import.meta.env` 暴露给客户端代码——任何变量一旦加 `VITE_` 就要当成「会被打包进产物、可被任何访客看到」的公开信息，密钥绝对不能放（放后端 `.env`）。
- `.env.local` / `.env.*.local` 默认被 gitignore——放个人本机覆盖值（如指向不同的 `VITE_API_TARGET`），团队共享的默认值放 `.env`。
- 优先级从高到低：`process` 环境变量 > `.env.[mode].local` > `.env.[mode]` > `.env.local` > `.env`——CI 里注入的环境变量会覆盖文件值，这正是 GitHub Actions 能「改环境不改文件」的原因。
- SelfBlog 的 `vite.config.ts` 用 `loadEnv(mode, process.cwd(), '')` 第三个参数空串加载**全部**变量（含不带 `VITE_` 前缀的），因为配置文件本身是 Node 进程、不进浏览器，读取 `VITE_API_TARGET` 决定代理目标不需要它对客户端可见（见 6.2.2 配置解析）。

### 6.2.z 构建产物分析与优化清单

```bash
cd frontend
./node_modules/.bin/vite build          # 先看默认产物
npx vite preview                        # 本地起服务预览 dist，验证的不是源码而是产物
```

`vite build` 输出的 chunk 清单就是最轻量的分析工具：`dist/assets/index-xxxx.js`（入口+路由）、`vendor-xxxx.js`（manualChunks 切出的框架包）、`markdown-xxxx.js`（marked+dompurify）……每个 chunk 后面跟着 gzip 后体积。要更细的可视化可加 `rollup-plugin-visualizer` 生成 treemap（SelfBlog 未装——按需原则，chunk 清单已够定位异常）。

SelfBlog 的切包策略（`manualChunks`）与理由：

```typescript
manualChunks: {
  vendor: ['vue', 'vue-router', 'pinia'],   // 框架三件套：几乎不变，强缓存收益最大化
  markdown: ['marked', 'dompurify']         // 渲染库：只有文章详情页真正用到，
}                                            // 单独成 chunk 让首页访客不必下载
```

优化清单（按收益排序）：

1. **查异常 chunk**：某个 chunk 突然变大，八成是哪个组件整包引入了全家桶库——`import debounce from 'lodash/debounce'` 改子路径引入，或换原生 `Intl`/`crypto` API；
2. **动态 import 路由组件**：Vue Router 的 `() => import('...')` 让每个页面各自成 chunk，首屏只载入口——SelfBlog 路由已如此；
3. **`sourcemap: false`**（生产默认）：sourcemap 方便排错但体积翻倍，公开站点关掉，需要时本地用 `--sourcemap` 临时构建；
4. **别手动优化压缩**：`minify: 'esbuild'` 默认已是速度与体积的甜点，Gzip/Brotli 交给反代（Vercel 自动、Nginx 见 6.8.9）。

**原则收束**：产物优化先「看」（chunk 清单/可视化）再「动」，每动一步用 6.8.10 的 Lighthouse 复测——没有测量就没有优化。

---

## 6.3 ESLint 代码检查

### 6.3.1 ESLint 概述

ESLint 是 JavaScript/TypeScript 的静态代码分析工具，用于发现代码中的潜在问题并强制执行编码规范。

**ESLint 能做什么：**

- 发现未使用的变量
- 检测未定义的变量
- 强制一致的代码风格
- 发现潜在 bug（如 `==` vs `===`）
- 强制执行最佳实践

### 6.3.2 Flat Config（ESLint 9+）

ESLint 9 引入了 Flat Config（`eslint.config.js`），替代了旧的 `.eslintrc` 格式。SelfBlog 使用 Flat Config：

```javascript
import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'
import globals from 'globals'

export default tseslint.config(
  // Global ignores
  {
    ignores: ['dist/**', 'node_modules/**', '*.d.ts']
  },

  // Browser globals
  {
    languageOptions: {
      globals: {
        ...globals.browser
      }
    }
  },

  // Base JS rules
  js.configs.recommended,

  // TypeScript rules
  ...tseslint.configs.recommended,

  // Vue rules
  ...pluginVue.configs['flat/essential'],

  // Prettier (disables conflicting rules)
  prettier,

  // Custom rules
  {
    files: ['**/*.{ts,tsx,vue}'],
    rules: {
      // TypeScript
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/consistent-type-imports': ['error', { prefer: 'type-imports' }],

      // Vue
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'warn',

      // General
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'warn'
    }
  },

  // Vue file specific parser
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser
      }
    }
  }
)
```

**配置结构解析：**

**1. 全局忽略（ignores）**

```javascript
{
  ignores: ['dist/**', 'node_modules/**', '*.d.ts']
}
```

这些路径下的文件不会被 ESLint 检查。

**2. 全局变量（globals）**

```javascript
{
  languageOptions: {
    globals: {
      ...globals.browser  // window, document, navigator 等
    }
  }
}
```

声明浏览器全局变量，避免 `no-undef` 规则误报。

**3. 推荐规则集**

```javascript
js.configs.recommended,           // JavaScript 推荐规则
...tseslint.configs.recommended,  // TypeScript 推荐规则
...pluginVue.configs['flat/essential'],  // Vue 推荐规则
```

**4. Prettier 集成**

```javascript
prettier,  // eslint-config-prettier
```

`eslint-config-prettier` 会关闭所有与 Prettier 格式化冲突的 ESLint 规则。这是 ESLint 与 Prettier 协作的关键。

**5. 自定义规则**

```javascript
{
  files: ['**/*.{ts,tsx,vue}'],
  rules: {
    '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    '@typescript-eslint/no-explicit-any': 'warn',
    '@typescript-eslint/consistent-type-imports': ['error', { prefer: 'type-imports' }],
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'warn',
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    'no-debugger': 'warn'
  }
}
```

**规则严重级别：**

| 级别 | 含义 | 退出码 |
|------|------|--------|
| `'off'` / `0` | 关闭规则 | - |
| `'warn'` / `1` | 警告（不中断构建） | 0 |
| `'error'` / `2` | 错误（中断构建） | 1 |

**常用规则说明：**

| 规则 | 说明 |
|------|------|
| `@typescript-eslint/no-unused-vars` | 未使用的变量 |
| `@typescript-eslint/no-explicit-any` | 禁止使用 `any` 类型 |
| `@typescript-eslint/consistent-type-imports` | 强制使用 `import type` |
| `vue/multi-word-component-names` | 组件名必须多单词 |
| `vue/no-v-html` | 警告使用 `v-html`（XSS 风险） |
| `no-console` | 限制 `console` 使用 |
| `no-debugger` | 禁止 `debugger` 语句 |

**6. Vue 文件解析器**

```javascript
{
  files: ['**/*.vue'],
  languageOptions: {
    parserOptions: {
      parser: tseslint.parser  // 使用 TypeScript 解析器解析 <script> 块
    }
  }
}
```

Vue SFC 的 `<script>` 块可能包含 TypeScript，需要指定 TypeScript 解析器。

### 6.3.3 自动修复

ESLint 支持自动修复（`--fix`）：

```bash
# 自动修复
npm run lint
# 等价于：eslint src --fix

# 只检查不修复
npm run lint:check
# 等价于：eslint src
```

**可自动修复的规则：**

- 代码风格（缩进、引号、分号）
- 未使用的导入
- 简单的语法问题

**不可自动修复的规则：**

- 逻辑错误
- 类型错误
- 复杂的代码问题

### 6.3.4 与 Prettier 集成

**为什么需要同时使用 ESLint 和 Prettier？**

- **ESLint**：代码质量检查（发现 bug、强制最佳实践）
- **Prettier**：代码格式化（统一的代码风格）

**集成方式：**

```javascript
// eslint.config.js
import prettier from 'eslint-config-prettier'

export default tseslint.config(
  // ... 其他配置
  prettier  // 必须放在最后，关闭冲突规则
)
```

**eslint-config-prettier 的作用：**

它会关闭所有与 Prettier 格式化冲突的 ESLint 规则，例如：
- `indent`（缩进）
- `quotes`（引号）
- `semi`（分号）
- `max-len`（行宽）

这样 ESLint 专注于代码质量，Prettier 专注于代码格式，互不干扰。

### 6.3.5 编辑器集成

**VS Code 设置（.vscode/settings.json）：**

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": "explicit"
  },
  "eslint.validate": [
    "javascript",
    "typescript",
    "vue"
  ]
}
```

**保存时自动修复：**

1. 保存文件
2. Prettier 格式化代码
3. ESLint 自动修复可修复的问题

---

## 6.4 Prettier 代码格式化

### 6.4.1 Prettier 概述

Prettier 是一个"有主见"的代码格式化工具，支持 JavaScript、TypeScript、CSS、HTML、JSON、Markdown 等多种语言。

**Prettier 的设计哲学：**

- **一致性**：所有代码看起来像同一个人写的
- **无配置**：提供合理的默认值，减少团队争论
- **不可协商**：格式化规则不可自定义（少数选项除外）

### 6.4.2 .prettierrc 配置解析

SelfBlog 的 Prettier 配置：

```json
{
  "semi": false,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "none",
  "printWidth": 100,
  "bracketSpacing": true,
  "arrowParens": "always",
  "endOfLine": "auto",
  "vueIndentScriptAndStyle": false
}
```

**配置项详解：**

| 选项 | 值 | 说明 |
|------|-----|------|
| `semi` | `false` | 不在语句末尾添加分号 |
| `singleQuote` | `true` | 使用单引号（而非双引号） |
| `tabWidth` | `2` | 缩进宽度为 2 个空格 |
| `trailingComma` | `none` | 不在数组/对象末尾添加逗号 |
| `printWidth` | `100` | 每行最大字符数 |
| `bracketSpacing` | `true` | 对象字面量括号内添加空格 |
| `arrowParens` | `always` | 箭头函数参数始终加括号 |
| `endOfLine` | `auto` | 行尾符自动检测（兼容 Windows/Linux） |
| `vueIndentScriptAndStyle` | `false` | 不缩进 Vue SFC 的 `<script>` 和 `<style>` |

**格式化效果对比：**

```javascript
// 格式化前
const obj={name:'test',age:18}
function greet(name){return `Hello, ${name}`}

// 格式化后
const obj = { name: 'test', age: 18 }
function greet(name) {
  return `Hello, ${name}`
}
```

### 6.4.3 命令行使用

```bash
# 格式化所有文件
npm run format
# 等价于：prettier --write "src/**/*.{ts,tsx,vue,css,json}"

# 检查格式（不修改文件）
npm run format:check
# 等价于：prettier --check "src/**/*.{ts,tsx,vue,css,json}"

# 格式化单个文件
npx prettier --write src/main.ts

# 检查单个文件
npx prettier --check src/main.ts
```

### 6.4.4 编辑器集成

**VS Code 插件：**

1. 安装 `Prettier - Code formatter` 插件
2. 设置为默认格式化工具

```json
{
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.formatOnSave": true
}
```

**保存时自动格式化：**

```json
{
  "editor.formatOnSave": true
}
```

### 6.4.5 .prettierignore

```gitignore
# 构建输出
dist
node_modules

# 锁文件
package-lock.json
pnpm-lock.yaml
yarn.lock

# 文档
*.md

# 第三方代码
vendor
```

### 6.4.6 Prettier 与 ESLint 的协作

**分工明确：**

| 工具 | 职责 |
|------|------|
| Prettier | 代码格式（缩进、引号、分号、行宽） |
| ESLint | 代码质量（未使用变量、潜在 bug、最佳实践） |

**协作流程：**

```
1. 保存文件
2. Prettier 格式化代码风格
3. ESLint 检查代码质量
4. ESLint 自动修复可修复的问题
```

**为什么需要 eslint-config-prettier？**

ESLint 的一些规则与 Prettier 冲突（如缩进、引号），`eslint-config-prettier` 会关闭这些冲突规则，让两个工具和平共处。

---

## 6.5 TypeScript 集成

### 6.5.1 TypeScript 概述

TypeScript 是 JavaScript 的超集，添加了静态类型系统。它能在编译时发现类型错误，提高代码质量和开发效率。

**TypeScript 的优势：**

- **类型安全**：编译时发现类型错误
- **智能提示**：IDE 提供准确的自动补全
- **重构支持**：类型系统使重构更安全
- **文档作用**：类型即文档

### 6.5.2 tsconfig.json 完整解析

SelfBlog 的 TypeScript 配置：

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "ignoreDeprecations": "6.0",

    /* Bundler mode */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "preserve",

    /* Linting */
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true,

    /* Path aliases */
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    },

    /* Vue */
    "types": ["vite/client"]
  },
  "include": [
    "src/**/*.ts",
    "src/**/*.tsx",
    "src/**/*.vue",
    "src/**/*.d.ts",
    "env.d.ts"
  ],
  "exclude": ["node_modules", "dist"]
}
```

**配置项详解：**

**1. 目标与模块**

| 选项 | 值 | 说明 |
|------|-----|------|
| `target` | `ES2020` | 编译目标为 ES2020 |
| `module` | `ESNext` | 使用 ES 模块 |
| `moduleResolution` | `bundler` | 打包工具模式（Vite/Webpack） |
| `lib` | `["ES2020", "DOM", "DOM.Iterable"]` | 包含的库类型 |

**2. Bundler 模式**

```json
{
  "moduleResolution": "bundler",
  "allowImportingTsExtensions": true,
  "isolatedModules": true,
  "moduleDetection": "force",
  "noEmit": true
}
```

这些选项是为打包工具（Vite）设计的：
- `moduleResolution: "bundler"`：支持打包工具的模块解析
- `allowImportingTsExtensions`：允许导入 `.ts` 扩展名
- `isolatedModules`：确保每个文件可独立编译
- `noEmit`：不生成输出文件（由 Vite 负责打包）

**3. 严格模式**

```json
{
  "strict": true,
  "noUnusedLocals": true,
  "noUnusedParameters": true,
  "noFallthroughCasesInSwitch": true,
  "noUncheckedIndexedAccess": true
}
```

`strict: true` 启用所有严格检查选项：

| 选项 | 说明 |
|------|------|
| `strict` | 启用所有严格检查 |
| `noUnusedLocals` | 报告未使用的局部变量 |
| `noUnusedParameters` | 报告未使用的函数参数 |
| `noFallthroughCasesInSwitch` | 报告 switch 语句的 fall-through |
| `noUncheckedIndexedAccess` | 索引访问返回 `T \| undefined` |

**`noUncheckedIndexedAccess` 详解：**

```typescript
const arr = [1, 2, 3]
const first = arr[0]  // 类型为 number | undefined

// 错误：可能为 undefined
console.log(first.toFixed(2))

// 正确：使用可选链
console.log(first?.toFixed(2))

// 正确：使用非空断言（确定不为空时）
console.log(first!.toFixed(2))
```

**4. 路径别名**

```json
{
  "baseUrl": ".",
  "paths": {
    "@/*": ["src/*"]
  }
}
```

与 Vite 的 `resolve.alias` 对应：

```typescript
// vite.config.ts
resolve: {
  alias: {
    '@': resolve(__dirname, 'src')
  }
}
```

**5. Vue 类型支持**

```json
{
  "types": ["vite/client"]
}
```

`vite/client` 提供了 Vite 特有的类型定义（如 `import.meta.env`）。

### 6.5.3 vue-tsc 类型检查

`vue-tsc` 是 Vue 官方的 TypeScript 类型检查工具，它在 `tsc` 的基础上增加了对 `.vue` 文件的支持。

**类型检查命令：**

```bash
npm run type-check
# 等价于：vue-tsc --noEmit
```

**`--noEmit` 的作用：**

只进行类型检查，不生成输出文件。因为 Vite 负责实际的打包工作，TypeScript 只需要检查类型。

**类型检查流程：**

```
1. vue-tsc 读取 tsconfig.json
2. 解析所有 .ts/.tsx/.vue 文件
3. 检查类型错误
4. 报告错误（不生成文件）
```

### 6.5.4 TypeScript 与 Vue 3

**`<script setup lang="ts">` 的优势：**

```vue
<template>
  <div>{{ message }}</div>
</template>

<script setup lang="ts">
// 类型推断
const message = 'Hello'  // 类型为 string

// 类型注解
const count: number = 0

// 接口定义
interface User {
  name: string
  age: number
}

const user: User = {
  name: 'Alice',
  age: 25
}

// 泛型
function identity<T>(arg: T): T {
  return arg
}

const result = identity<string>('Hello')
</script>
```

**Props 类型定义：**

```vue
<script setup lang="ts">
// 方式一：运行时声明
const props = defineProps<{
  title: string
  count?: number
}>()

// 方式二：类型声明（推荐）
interface Props {
  title: string
  count?: number
}
const props = defineProps<Props>()
</script>
```

**Emits 类型定义：**

```vue
<script setup lang="ts">
const emit = defineEmits<{
  (e: 'change', value: string): void
  (e: 'update', id: number): void
}>()

// 使用
emit('change', 'new value')
</script>
```

**Ref 类型：**

```typescript
import { ref } from 'vue'

// 自动推断类型
const count = ref(0)  // Ref<number>

// 显式类型声明
const user = ref<User | null>(null)

// 访问值
count.value++
console.log(user.value?.name)
```

### 6.5.5 类型声明文件（.d.ts）

**env.d.ts：**

```typescript
/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TARGET?: string
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

**`declare module '*.vue'` 的作用：**

让 TypeScript 理解 `.vue` 文件的导入。Vue SFC 不是标准的 TypeScript 文件，需要声明模块类型。

**`ImportMetaEnv` 的作用：**

为 `import.meta.env` 提供类型提示，避免访问不存在的环境变量时报错。

### 6.5.6 现代 JavaScript 语法补强：ES6+ 特性清单

SelfBlog 代码里铺满了 ES2015 以来的语法。它们常被「当作空气」，但每个都有明确语义，值得集中过一遍——尤其 `?.` 与 `??` 这对组合，是 `noUncheckedIndexedAccess` 严格模式下的生存技能（见 6.5.2）。

**解构赋值（Destructuring）**——从对象/数组中按模式取值：

```typescript
// 对象解构：props 声明与 store 使用处随处可见
const { records, total } = res.data                  // 分页信封拆包
const { name, age = 18 } = user                      // 带默认值
let { name: userName } = user                        // 重命名

// 数组解构
const [first, second] = list                          // 注意：noUncheckedIndexedAccess 下 first/second 类型含 undefined
const [head, ...rest] = list                          // rest 收集剩余项

// 函数参数解构（Vue 组合式 API 的标准形态）
function usePagination({ page = 1, size = 10 }: Options = {}) { /* ... */ }
```

**展开运算符（Spread / Rest）**——`...` 既是展开也是收集：

```typescript
// 展开：创建新对象而非修改旧对象（Pinia/响应式系统友好）
const next = { ...state, page: 2 }                    // 浅拷贝 + 覆盖字段
const merged = [...oldList, newItem]                  // 追加而不 push

// 注意浅拷贝边界：嵌套对象仍共享引用
const copy = { ...user }                              // copy.address === user.address
// Rest 参数：收集函数剩余实参
function logAll(level: string, ...args: unknown[]) { /* ... */ }
```

**可选链 `?.` 与空值合并 `??`**——SecurityUtils 风格的「可能为空」防御（前端对应物）：

```typescript
const city = user?.address?.city          // 任一环节 null/undefined 就返回 undefined，不抛 TypeError
const port = config.port ?? 3000          // 仅在 null/undefined 时取默认值
const first = arr.at(0)?.name ?? '未知'   // 典型组合：索引访问 + 可选链 + 兜底

// 关键区别：?? 与 || 不同——0、''、false 是合法值不该被吞
const volume = 0
volume || 100   // 100（错误！0 被当成假值）
volume ?? 100   // 0（正确）
```

这解释了 6.5.2 的规则：`noUncheckedIndexedAccess` 让 `arr[0].foo` 编译报错（返回类型是 `T | undefined`），惯用修法正是 `arr[0]?.foo ?? fallback`。SelfBlog 真实案例：`import.meta.env.VITE_API_TARGET ?? 'http://localhost:8825'`——用 `??` 而不是 `||`，虽然此处恰好是字符串，但语义更准确：只有「确实没配置」才回落默认值。

**Promise 与 async/await 深入**——axios 拦截器（6.8.2 提到的刷新排队）就是 Promise 状态机的教科书案例：

```typescript
// async 函数总是返回 Promise；await 只在 async 函数（或顶层）可用
async function load(): Promise<Post> {
  const res = await api.get<Post>(`/post/${id}`)   // res 就是 { code, message, data } 信封
  if (res.code !== 200) throw new Error(res.message)
  return res.data
}

// 串行 vs 并行：无依赖的请求应并行
const [post, comments] = await Promise.all([loadPost(), loadComments()])

// Promise.all vs allSettled：一个失败全炸 vs 全部出结果（成败并存）
const results = await Promise.allSettled([loadA(), loadB()])
const ok = results.filter(r => r.status === 'fulfilled')

// 错误处理：try/catch 捕获 await 的拒绝
try { await load() } catch (e) { /* 拦截器里 handleAuthError 已处理 401 */ }
```

三个高频误区：①循环里 `await`（串行慢）应改 `Promise.all`；②`forEach` 不认 `await`——回调里的 Promise 无人等待，要用 `for...of`；③async 函数抛出的错误是 Promise 拒绝，忘了 `await` 它就静默丢失。

**模块系统：ESM vs CommonJS**——SelfBlog 前端全 ESM（`"type": "module"`），但理解对照表才能看懂构建脚本与老文档：

```javascript
// ESM（浏览器原生 + Vite/Node 现代标准）：静态、可 Tree-shaking
import { marked } from 'marked'
export function render(src: string) { /* ... */ }
export default markdownIt

// CommonJS（Node 传统格式，构建脚本 .mjs 之外的老 .js 可能遇到）
const path = require('path')
module.exports = { buildVercelProxy }
```

关键差异：ESM 的 `import` 是**静态声明**（编译期可知依赖图，Rollup 才能摇树），CJS 的 `require` 是**运行时调用**（可以写在 if 里，代价是无法静态分析）。`import()` 动态导入是 ESM 的例外——返回 Promise，正是路由懒加载的底层机制。Node 里 `.mjs` 后缀或 `type: "module"` 走 ESM，`.cjs` 强制 CJS；Vite 项目的 `scripts/*.mjs` 用 `.mjs` 后缀就是为了在 `"type"` 缺省时也明确 ESM 身份。

### 6.5.7 CSS3 与 HTML5 基础补强：语义化、布局与设计 Token

工程化教程不该假装样式不存在——SelfBlog 的设计系统恰是 CSS 现代特性的集中示范。先补 HTML 侧：

**HTML5 语义化标签**——用「这是什么」而非「长什么样」命名结构，搜索引擎与读屏软件都靠它：

```html
<body>
  <header>   <!-- 站点头：logo、导航 -->
    <nav aria-label="主导航">…</nav>
  </header>
  <main>     <!-- 每页唯一；<h1> 也应唯一 -->
    <article>  <!-- 独立可分发的内容单元：一篇文章、一条动态 -->
      <h1>文章标题</h1>
      <section>  <!-- 主题分组，通常配标题 -->
        <h2>小节</h2>
      </section>
    </article>
    <aside>    <!-- 与主内容弱相关：侧栏、推荐阅读 -->
  </main>
  <footer>   <!-- 站点脚：备案、版权 -->
</body>
```

三个原则：①**布局容器优先语义标签**，`<div>` 只留给纯粹的样式包装；②`<article>` 内可以再有 `<section>`，判断标准是「脱离页面还有没有意义」——博客文章是，装饰性卡片不是；③无障碍属性（`aria-label` 等）修饰交互控件，`<button>` 别写成 `<div @click>`——后者键盘无法聚焦，读屏不识别。Vue SFC 里写法相同，模板就是 HTML。

**Flexbox 与 Grid 布局详解**——现代 CSS 布局的两根支柱，分工是：**Flex 一维流（一行或一列），Grid 二维面（行×列同时定）**：

```css
/* Flexbox：导航栏、工具条、卡片内的水平排布 */
.site-header {
  display: flex;
  justify-content: space-between; /* 主轴：两端对齐 */
  align-items: center;            /* 交叉轴：垂直居中 */
  gap: 16px;                      /* 子项间距，取代 margin hack */
}
.card-list {
  display: flex;
  flex-wrap: wrap;                /* 换行 */
  gap: 16px;
}
.card-list > * { flex: 1 1 240px; }  /* 伸展/收缩/基准：每张卡至少 240px，可伸缩 */

/* Grid：页面骨架、相册网格、后台表单 */
.page-layout {
  display: grid;
  grid-template-columns: 240px 1fr;   /* 固定侧栏 + 自适应主区 */
  grid-template-rows: auto 1fr auto;  /* 头/身/脚 */
  min-height: 100vh;
}
.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); /* 响应式：不需要媒体查询 */
  gap: 12px;
}
```

选择经验：**先问「是一行还是一张表」**。一行内的对齐/分布用 Flex；需要「列宽互相约束」（如 `1fr` 与固定列共舞）或单元跨越行列时用 Grid。两者可以嵌套：Grid 定页面骨架，每个格子里再用 Flex 排内容——SelfBlog 的 `layout.css` 正是这种组合。此外 Flex 的 `flex: 1` 三值缩写（grow/shrink/basis）与 Grid 的 `minmax()` 是响应式布局的两把钥匙，`auto-fill + minmax` 更是「无媒体查询的响应式网格」标准答案。

**CSS 变量与主题切换**——这是 SelfBlog 设计系统的核心机制，也是工程化「 token 思维」的最佳教材。先看架构：`src/styles/main.css` 只是 `@import` 入口，真正分三层：

```css
/* variables.css —— 基础 token 层：:root 与 #selfblog 上定义全量变量 */
:root, #selfblog {
  --bg-primary: #f5f6f8;        /* 画布背景 */
  --surface-primary: #ffffff;   /* 卡片表面 */
  --text-primary: #262a31;      /* 正文 */
  --accent: #165dff;            /* 品牌蓝 */
  --radius-lg: 12px;            /* 圆角 token */
  --code-bg: #1d2129;           /* 代码块（永远深色） */
  --contrast-bg: #111111;       /* 高对比表面（主按钮） */
}

/* dark-mode.css —— 暗色覆盖层：同名变量重新赋值，选择器特异性略高 */
.dark-mode, #selfblog.dark-mode {
  --bg-primary: #171a20;
  --surface-primary: #20242c;
  --text-primary: #edf0f5;
  --gray-900: #ebebeb;          /* ← 注意！灰度梯度整体反转 */
}

/* skins.css —— 皮肤层：#selfblog[data-skin=…] 属性选择器换肤 */
#selfblog[data-skin='forest']:not(.dark-mode)  { --accent: #2d7c54; /* … */ }
#selfblog[data-skin='sunset']:not(.dark-mode)  { --accent: #c45b3e; /* … */ }
#selfblog[data-skin='midnight']                { --bg-primary: #101823; /* … */ }
```

切换的实现因此轻得不可思议——**只改一个类或一个属性，全部颜色联动**（见 6.8.12 的重绘/重排分析）：

```typescript
// stores/theme.ts（要点摘录）
document.documentElement.classList.add('dark-mode')        // 暗色 = 加根类
localStorage.setItem(STORAGE_KEY_PREFIX + 'theme', theme)  // 偏好持久化
window.matchMedia('(prefers-color-scheme: dark)').matches   // 跟随系统
// skins：site.ts 把后台选的皮肤写到 #selfblog 根元素的 data-skin 属性
```

**陷阱清单：`--gray-*` 不是「深色表面」**。SelfBlog 的暗色模式把灰度梯度**整体反转**：`--gray-900` 在浅色模式是 `#111111`（最深），在暗色模式变成 `#EBEBEB`（最浅）——因为它的语义是「梯度上最深的一档」，暗色模式里「最深的一档」应该是浅灰白。如果你写 `background: var(--gray-900)` 想要「深色按钮/深色代码块」，暗色模式下就会得到刺眼的亮块。正确做法是用**语义稳定**的 token：

| 需求 | 正确 token | 说明 |
|------|-----------|------|
| 永远深色的代码块 | `--code-bg` / `--code-text` / `--code-border` | 浅色 `#1d2129`、暗色 `#161616`，**两种模式都深** |
| 高对比实心按钮 | `--contrast-bg` / `--contrast-bg-hover` / `--contrast-bg-active` / `--contrast-fg` | 浅色下深底白字，暗色下反转成浅底黑字——**语义是「最强对比」而非「最深颜色」** |
| 圆角 | `--radius-*`（`--radius-xs` 4px 到 `--radius-full` 9999px） | 圆角一律用 token，别写裸 `8px`——设计系统升级时一键全局调整 |

区分两类 token 的口诀：**梯度 token（`--gray-*`）描述「在同一色阶里的位置」，语义 token（`--code-*`/`--contrast-*`）描述「这个 UI 元素的角色」**。写组件样式时永远按角色取色，按位置取色迟早踩反转陷阱。

**CSS 变量的进阶技巧**（SelfBlog 真实用法）：`--accent-light: color-mix(in srgb, var(--accent) 10%, var(--surface-primary))`——用 `color-mix()` 从主色**派生**浅色变体，换肤时只改 `--accent` 一个值，hover 底色、边框淡色全部自动跟随；暗色模式的 `--accent-dark` 混入 `white` 而非 `black`（深底上提亮更自然）。这就是 token 体系的美感：**改一处，动全局**。

---

## 6.6 单元测试：Vitest 框架

### 6.6.1 Vitest 概述

Vitest 是 Vite 官方的测试框架，与 Vite 共享配置，提供极速的测试体验。

**Vitest 的优势：**

- **速度快**：利用 Vite 的模块系统，无需打包
- **兼容性好**：兼容 Jest API，迁移成本低
- **TypeScript 支持**：原生支持 TypeScript
- **HMR**：测试代码修改后自动重新运行

### 6.6.2 Vitest 配置

SelfBlog 在 `vite.config.ts` 中配置 Vitest：

```typescript
/// <reference types="vitest" />
import { defineConfig } from 'vite'

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{js,ts,jsx,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['src/**/*.{js,ts,vue}'],
      exclude: ['src/main.ts', 'src/**/*.test.ts', 'src/**/*.spec.ts']
    }
  }
})
```

**配置项详解：**

| 选项 | 说明 |
|------|------|
| `globals` | 全局注入 `describe`、`it`、`expect` 等 API |
| `environment` | 测试环境（`jsdom` 模拟浏览器 DOM） |
| `setupFiles` | 测试前执行的文件 |
| `include` | 测试文件匹配模式 |
| `coverage` | 覆盖率配置 |

### 6.6.3 测试环境：jsdom

**jsdom 是什么？**

jsdom 是一个纯 JavaScript 实现的 DOM 环境，可以在 Node.js 中模拟浏览器 DOM API。

**为什么需要 jsdom？**

Vue 组件需要 DOM 环境才能渲染和测试。在 Node.js 中没有 `document`、`window` 等浏览器 API，jsdom 提供了这些 API 的模拟实现。

**jsdom 的局限性：**

- 不支持布局（`getBoundingClientRect` 返回 0）
- 不支持 `window.matchMedia`（需要手动 mock）
- 不支持 `IntersectionObserver`（需要手动 mock）

### 6.6.4 测试 setup 文件

SelfBlog 的 `src/test/setup.ts`：

```typescript
import '@testing-library/jest-dom'
import { config } from '@vue/test-utils'
import { vi } from 'vitest'

// 全局测试配置
config.global.stubs = {}
config.global.mocks = {
  $route: { path: '/', params: {}, query: {} },
  $router: { push: vi.fn(), replace: vi.fn() }
}

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
})
```

**setup 文件的作用：**

1. **导入 jest-dom 匹配器**：提供 `toBeInTheDocument()`、`toHaveTextContent()` 等匹配器
2. **配置全局 mocks**：为所有测试提供 `$route` 和 `$router` 的 mock
3. **Mock 浏览器 API**：提供 `window.matchMedia` 的 mock 实现

#### jsdom 陷阱清单（SelfBlog 实测踩过的每一个）

jsdom 不是浏览器。以下差异每一个都让 SelfBlog 的测试「第一次跑就挂」，全部有成熟解法：

**陷阱一：setup 只桩了 matchMedia 和 $router/$route，没有自动卸载组件。**

凡挂载时会注册全局监听器（window scroll / resize / matchMedia change）的组件，测试之间会互相污染——上一个测试注册的监听器还活着，下一个测试里触发 scroll 事件，幽灵监听器就执行了。解法是**每个测试文件显式开启自动卸载**：

```typescript
import { enableAutoUnmount, mount } from '@vue/test-utils'
import { afterEach } from 'vitest'

enableAutoUnmount(afterEach)   // 每个 afterEach 自动销毁所有已挂载组件
```

**陷阱二：逐帧行为（requestAnimationFrame）不能桩成同步调用。**

BackToTop 这类「滚动渐显」组件常用 rAF 节流。若把 rAF mock 成 `(cb) => cb()`（同步执行），依赖「下一帧才更新」的逻辑全部错乱。正确姿势是**排队 + 手动 flush**：

```typescript
const rafQueue: FrameRequestCallback[] = []
vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
  rafQueue.push(cb)
  return rafQueue.length
})
// 需要推进一帧时：
rafQueue.forEach((cb) => cb(performance.now()))
rafQueue.length = 0
```

**陷阱三：改 scrollY 不能直接赋值。**

`window.scrollY = 500` 在 jsdom 里要么无效要么报错（只读 getter）。必须用 `Object.defineProperty` 覆盖，再手动派发事件：

```typescript
Object.defineProperty(window, 'scrollY', { value: 800, writable: true, configurable: true })
window.dispatchEvent(new Event('scroll'))
await wrapper.vm.$nextTick()   // Vue 的 DOM 更新是异步的，必须 nextTick
```

**陷阱四：不要 stub RouterLink。**

`stubs: { RouterLink: true }` 图省事，但 stub 会丢掉 RouterLink 的 slot 内容，也表达不了 active 态（`router-link-active` class）。SelfBlog 的约定：**路由测试一律用真实 router**：

```typescript
import { createRouter, createMemoryHistory } from 'vue-router'

const router = createRouter({
  history: createMemoryHistory(),   // 内存路由：不碰地址栏，测试专用
  routes,
})
const wrapper = mount(Component, { global: { plugins: [router] } })
```

**陷阱五：Pinia 测试要手动隔离。**

每个测试用 `setActivePinia(createPinia())` 建全新实例，否则 store 状态在测试间泄漏（上个测试改的 token 这个测试还能看到）：

```typescript
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach } from 'vitest'

beforeEach(() => {
  setActivePinia(createPinia())
})
```

### 6.6.5 组件测试基础

**安装依赖：**

```bash
npm install -D vitest @vue/test-utils @testing-library/vue jsdom
```

**基本测试示例：**

```typescript
// src/components/Button.test.ts
import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import Button from './Button.vue'

describe('Button', () => {
  it('renders correctly', () => {
    const wrapper = mount(Button, {
      props: {
        label: 'Click me'
      }
    })

    expect(wrapper.text()).toContain('Click me')
  })

  it('emits click event', async () => {
    const wrapper = mount(Button, {
      props: {
        label: 'Click me'
      }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
```

**@vue/test-utils 核心 API：**

| API | 说明 |
|-----|------|
| `mount` | 完整挂载组件（包括子组件） |
| `shallowMount` | 浅挂载（子组件被 stub） |
| `wrapper.text()` | 获取组件文本内容 |
| `wrapper.html()` | 获取组件 HTML |
| `wrapper.find(selector)` | 查找元素 |
| `wrapper.findAll(selector)` | 查找所有匹配元素 |
| `wrapper.props()` | 获取 props |
| `wrapper.emitted()` | 获取触发的事件 |
| `wrapper.trigger(event)` | 触发事件 |
| `wrapper.setValue(value)` | 设置表单值 |

### 6.6.6 Mock 技术

**Mock 函数：**

```typescript
import { vi } from 'vitest'

// 创建 mock 函数
const mockFn = vi.fn()

// 设置返回值
mockFn.mockReturnValue('mocked value')

// 设置实现
mockFn.mockImplementation((arg: string) => arg.toUpperCase())

// 断言调用
expect(mockFn).toHaveBeenCalled()
expect(mockFn).toHaveBeenCalledWith('arg')
expect(mockFn).toHaveBeenCalledTimes(1)
```

**Mock 模块：**

```typescript
import { vi } from 'vitest'

// Mock 整个模块
vi.mock('@/api/index', () => ({
  api: {
    get: vi.fn().mockResolvedValue({ data: {} }),
    post: vi.fn().mockResolvedValue({ data: {} })
  }
}))

// 在测试中使用
import { api } from '@/api/index'

it('should call api', async () => {
  await api.get('/posts')
  expect(api.get).toHaveBeenCalledWith('/posts')
})
```

**Mock 路由：**

```typescript
import { mount } from '@vue/test-utils'
import { vi } from 'vitest'

const wrapper = mount(Component, {
  global: {
    mocks: {
      $route: { path: '/posts', params: { id: '1' }, query: {} },
      $router: { push: vi.fn(), replace: vi.fn() }
    }
  }
})
```

### 6.6.7 覆盖率报告

**生成覆盖率报告：**

```bash
npm run test:coverage
# 等价于：vitest run --coverage
```

**覆盖率配置：**

```typescript
coverage: {
  provider: 'v8',           // 使用 V8 引擎的覆盖率
  reporter: ['text', 'json', 'html'],  // 报告格式
  include: ['src/**/*.{js,ts,vue}'],    // 包含的文件
  exclude: ['src/main.ts', 'src/**/*.test.ts', 'src/**/*.spec.ts']  // 排除的文件
}
```

**覆盖率指标：**

| 指标 | 说明 |
|------|------|
| Statements | 语句覆盖率 |
| Branches | 分支覆盖率 |
| Functions | 函数覆盖率 |
| Lines | 行覆盖率 |

**查看报告：**

```bash
# 文本报告（终端输出）
npm run test:coverage

# HTML 报告（浏览器打开）
open coverage/index.html
```

### 6.6.8 测试最佳实践

**测试命名：**

```typescript
// 推荐：描述行为
it('should display error message when API fails', () => { })

// 不推荐：描述实现
it('test api error', () => { })
```

**AAA 模式（Arrange-Act-Assert）：**

```typescript
it('should add item to list', () => {
  // Arrange（准备）
  const wrapper = mount(Component)
  const input = wrapper.find('input')

  // Act（执行）
  await input.setValue('new item')
  await wrapper.find('button').trigger('click')

  // Assert（断言）
  expect(wrapper.text()).toContain('new item')
})
```

**避免测试实现细节：**

```typescript
// 不推荐：测试内部状态
expect(wrapper.vm.internalState).toBe('value')

// 推荐：测试用户可见的行为
expect(wrapper.text()).toContain('expected text')
```

---

## 6.7 部署与 CI/CD

### 6.7.1 GitHub Actions 基础

GitHub Actions 是 GitHub 提供的 CI/CD 服务，通过 YAML 文件定义工作流。

**工作流结构：**

```yaml
name: CI                    # 工作流名称
on:                        # 触发条件
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:       # 手动触发

jobs:                      # 任务
  job1:
    runs-on: ubuntu-latest   # 运行环境
    steps:                 # 步骤
      - uses: actions/checkout@v4
      - run: echo "Hello"
```

### 6.7.2 SelfBlog CI 工作流解析

SelfBlog 的 `.github/workflows/ci.yml` 包含四个任务：

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  backend:
    name: Backend tests (H2)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Run backend tests
        working-directory: backend
        run: ./mvnw -B -ntp test -Dskip.npm=true -Dskip.installnodenpm=true

  frontend:
    name: Frontend checks
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '24.15.0'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint:check
      - run: npm run format:check
      - run: npm run type-check
      - run: npm run test:run
      - run: npm run test:deployment
      - run: npm run build

  mysql:
    name: Backend tests (MySQL 8)
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: campus_blog
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping -h 127.0.0.1 -u root --password=root"
          --health-interval=10s
          --health-timeout=5s
          --health-retries:12
    env:
      DB_TYPE: mysql
      DB_HOST: 127.0.0.1
      DB_PORT: '3306'
      DB_NAME: campus_blog
      DB_USERNAME: root
      DB_PASSWORD: root
      SPRING_SQL_INIT_MODE: never
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Initialize MySQL with the fresh-install script
        run: mysql --protocol=tcp -h 127.0.0.1 -P 3306 -u root -proot < backend/schema-fresh-install.sql
      - name: Run backend tests against MySQL
        working-directory: backend
        run: ./mvnw -B -ntp test -Dskip.npm=true -Dskip.installnodenpm=true

  docker:
    name: Docker image smoke test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - name: Build image (repository-root context, like Compose uses)
        uses: docker/build-push-action@v6
        with:
          context: .
          file: backend/Dockerfile
          load: true
          push: false
          tags: selfblog:ci
          cache-from: type=gha
          cache-to: type=gha,mode=max
      - name: Start container and wait for the real app
        run: |
          docker run -d --name selfblog-ci -p 8825:8825 \
            -e DB_TYPE=sqlite \
            -e JWT_SECRET=ci-only-jwt-secret-0123456789abcdefghijklmn \
            -e JWT_EXPIRATION=86400000 \
            -e JWT_REFRESH_EXPIRATION=604800000 \
            -e ADMIN_PASSWORD=ci-only-admin-password \
            selfblog:ci
          ready=0
          for attempt in $(seq 1 60); do
            if curl -fsS http://127.0.0.1:8825/actuator/health > /dev/null; then
              echo "healthy after ${attempt} attempt(s)"
              ready=1
              break
            fi
            if [ "$(docker inspect -f '{{.State.Running}}' selfblog-ci 2>/dev/null)" != "true" ]; then
              echo "container exited early"
              docker logs selfblog-ci
              exit 1
            fi
            sleep 5
          done
          if [ "$ready" != "1" ]; then
            echo "container never became healthy"
            docker logs selfblog-ci
            exit 1
          fi
          curl -fsS http://127.0.0.1:8825/actuator/health
          curl -fsS http://127.0.0.1:8825/ | grep -q 'seo-reading'
      - name: Container logs on failure
        if: failure()
        run: docker logs selfblog-ci || true
      - name: Cleanup
        if: always()
        run: docker rm -f selfblog-ci || true
```

**工作流详解：**

**1. 触发条件**

```yaml
on:
  push:
    branches: [main]      # 推送到 main 分支
  pull_request:           # 拉取请求
  workflow_dispatch:      # 手动触发
```

**2. 并发控制**

```yaml
concurrency:
  group: ci-${{ github.workspace }}-${{ github.ref }}
  cancel-in-progress: true
```

同一分支的多个工作流运行时，取消之前的运行，只保留最新的。

**3. 前端任务**

```yaml
frontend:
  name: Frontend checks
  runs-on: ubuntu-latest
  defaults:
    run:
      working-directory: frontend
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with:
        node-version: '24.15.0'
        cache: npm
        cache-dependency-path: frontend/package-lock.json
    - run: npm ci
    - run: npm run lint:check
    - run: npm run format:check
    - run: npm run type-check
    - run: npm run test:run
    - run: npm run test:deployment
    - run: npm run build
```

**前端 CI 流程：**

1. 检出代码
2. 设置 Node.js 24.15.0（带缓存）
3. 安装依赖（`npm ci`）
4. ESLint 检查
5. Prettier 检查
6. TypeScript 类型检查
7. 单元测试
8. 部署契约测试
9. 生产构建

**4. MySQL 服务**

```yaml
services:
  mysql:
    image: mysql:8.0
    env:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: campus_blog
    ports:
      - 3306:3306
    options: >-
      --health-cmd="mysqladmin ping -h 127.0.0.1 -u root --password=root"
      --health-interval=10s
      --health-timeout=5s
      --health-retries:12
```

GitHub Actions 的 `services` 允许在任务中启动辅助容器（如数据库）。

**5. Docker 任务**

```yaml
docker:
  name: Docker image smoke test
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - uses: docker/setup-buildx-action@v3
    - name: Build image
      uses: docker/build-push-action@v6
      with:
        context: .
        file: backend/Dockerfile
        load: true
        push: false
        tags: selfblog:ci
```

**Docker 构建流程：**

1. 检出代码
2. 设置 Buildx
3. 构建镜像（不推送）
4. 启动容器
5. 等待健康检查
6. 验证服务端渲染 HTML

### 6.7.3 Docker 部署

**Dockerfile 示例：**

```dockerfile
# 多阶段构建
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**docker-compose.yml 示例：**

```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: backend/Dockerfile
    ports:
      - "8825:8825"
    environment:
      - DB_TYPE=sqlite
      - JWT_SECRET=${JWT_SECRET}
      - ADMIN_PASSWORD=${ADMIN_PASSWORD}
    volumes:
      - ./data:/app/data
      - ./uploads:/app/uploads
```

### 6.7.4 Vercel 代理部署

SelfBlog 的 Vercel 配置是一个**代理生成器**，而不是静态前端部署：

```json
{
  "framework": null,
  "buildCommand": "node scripts/build-vercel-proxy.mjs",
  "installCommand": "node -e \"console.log('Proxy deployment needs only Node built-ins')\""
}
```

**为什么使用代理模式？**

SelfBlog 的生产部署是后端单 JAR 内嵌前端，Vercel 只作为反向代理，不在平台侧再构建一份前端。

**构建脚本（build-vercel-proxy.mjs）：**

```javascript
import { fileURLToPath } from 'node:url'
import { writeProxyOutput } from './vercel-proxy.mjs'

try {
  await writeProxyOutput(process.env, fileURLToPath(new URL('../.vercel/output/', import.meta.url)))
  console.log('Vercel proxy routing generated; HTML and assets remain owned by the source JAR.')
} catch (error) {
  console.error(error instanceof Error ? error.message : 'Vercel proxy configuration failed')
  process.exitCode = 1
}
```

**环境变量：**

| 变量 | 说明 |
|------|------|
| `SELFBLOG_BACKEND_ORIGIN` | 源站 HTTPS origin |
| `SITE_BASE_URL` | 正式访客 origin |

**部署步骤：**

1. Root Directory 设为 `frontend`
2. Framework Preset 选 **Other**
3. 配置环境变量
4. Build Command 使用 `node scripts/build-vercel-proxy.mjs`

**Build Output v3 速读**：`build-vercel-proxy.mjs` 生成的 `.vercel/output/` 是 Vercel 的开放部署格式——`config.json` 声明路由与变量，`static/` 放静态文件。对纯代理场景，关键只有一条：**把所有路径（`/api/*`、`/uploads/*`、页面路由）全部 rewrite 转发到 `SELFBLOG_BACKEND_ORIGIN` 这一个 origin**——产物校验脚本会拒绝空 origin、非法 URL 与自引用 origin（指向 Vercel 自己会造成无限回环），这正是 `npm run test:deployment` 契约测试存在的意义：部署配置也是代码，也要有测试。`framework: null`（Preset 选 Other）的含义是**关掉 Vercel 对前端的自动构建猜测**——不识别框架、不跑 `npm run build`、不上传静态前端，一切由我们的生成器说了算。

**Nginx 配置示例（自托管静态 + 反代的对照）**——如果不走 Vercel，同样的「静态托管 + API 反代」拓扑用 Nginx 实现：

```nginx
server {
    listen 80;
    server_name blog.example.com;

    # SPA 静态资源：带哈希的产物强缓存一年（对应 6.8.12 缓存策略）
    location /assets/ {
        root /var/www/selfblog/dist;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # 入口 HTML：协商缓存，绝不强缓存
    location = /index.html {
        root /var/www/selfblog/dist;
        add_header Cache-Control "no-cache";
    }

    # SPA 路由回退：找不到的路径回 index.html，交给 Vue Router
    location / {
        root /var/www/selfblog/dist;
        try_files $uri $uri/ /index.html;
    }

    # API 与上传文件反代到后端（同源拓扑，免 CORS）
    location /api/     { proxy_pass http://127.0.0.1:8825; }
    location /uploads/ { proxy_pass http://127.0.0.1:8825; }

    # Gzip：文本类资源压缩后再传
    gzip on;
    gzip_types text/css application/javascript application/json image/svg+xml;
}
```

三个配置要点：`try_files $uri $uri/ /index.html` 是 SPA 部署的签名行（没有它，刷新 `/post/42` 直接 404）；`immutable` 告诉浏览器「这一年里文件名带哈希、内容永不变」，配合协商缓存的 HTML 才是完整闭环；`/uploads/` 也要反代——用户上传的图片存在后端卷里，不在前端静态目录。

### 6.7.5 部署契约测试

SelfBlog 使用 `node:test` 进行部署契约测试：

```bash
npm run test:deployment
# 等价于：node --test scripts/__tests__/*.test.mjs
```

**测试内容：**

- Vercel 代理生成器输出验证
- Docker/Compose 源契约验证
- 环境变量验证

### 6.7.6 前端工程的「五道闸门」：为什么全都要绿

SelfBlog 前端定义了五条必须全绿的闸门，这是现代前端工程的标准化质量体系，值得逐个理解「它在防什么」：

| 闸门 | 命令 | 防住的事故 |
|------|------|-----------|
| Lint | `npm run lint:check` | 未使用变量、错误 hooks 依赖数组、危险 API（防「代码能跑但埋雷」） |
| Format | `npm run format:check` | 格式不一致（防 diff 噪音淹没真实改动、防代码审查浪费时间在空格上） |
| Type-check | `npm run type-check` | 类型错误（防 `undefined` 上取属性这种运行时崩溃） |
| Unit tests | `npm run test:run` | 逻辑回归（防「改 A 坏 B」） |
| Build | `npm run build` | vue-tsc + vite 全量编译（防「本地 dev 正常、构建才炸」的增量编译漏洞） |

**为什么 lint 和 type-check 不能互相替代**：ESLint 管不了类型（`str.foo` 这种拼写错误它不认识），vue-tsc 管不了风格与逻辑陷阱（条件分支漏 return 它不报）。**两者正交，缺一不可。**

**本地与 CI 的关系**：CI 只是把本地闸门原样跑一遍。养成 `git push` 前本地先全绿的肌肉记忆，比「推上去看 CI 脸色」高效得多——CI 排队一轮常常 5 分钟起步，本地跑全套不到 1 分钟。

**format:check 的一个真实演化**：SelfBlog 的 `.prettierrc` 曾因 `endOfLine` 默认值跨平台漂移（Windows CRLF vs Linux LF）导致 CI 偶发红。2026-10-17 改为 `endOfLine: "auto"` 并一次性重写历史漂移后，该闸门才真正稳定。**教训：格式规则必须跨平台确定性，否则闸门会变成随机红绿灯。**

---

## 6.8 项目实战：SelfBlog 工程化全景

### 6.8.1 项目架构总览

```
SelfBlog/
├── backend/                 # Spring Boot 3.3 + Java 21
│   ├── src/main/java/       # 后端源码
│   ├── src/main/resources/  # 配置文件
│   ├── src/test/            # 测试代码
│   ├── Dockerfile           # Docker 镜像
│   └── pom.xml              # Maven 配置
├── frontend/                # Vue 3.5 + Vite 6
│   ├── src/                 # 前端源码
│   │   ├── api/             # API 接口
│   │   ├── components/      # 组件
│   │   ├── composables/     # 组合式函数
│   │   ├── router/          # 路由
│   │   ├── stores/          # Pinia 状态
│   │   ├── styles/          # 样式
│   │   ├── types/           # 类型定义
│   │   ├── utils/           # 工具函数
│   │   └── views/           # 页面组件
│   ├── scripts/             # 构建脚本
│   ├── public/              # 静态资源
│   ├── vite.config.ts       # Vite 配置
│   ├── tsconfig.json        # TypeScript 配置
│   ├── eslint.config.js     # ESLint 配置
│   ├── .prettierrc          # Prettier 配置
│   ├── vercel.json          # Vercel 配置
│   └── package.json         # 依赖配置
├── .github/workflows/       # GitHub Actions
│   └── ci.yml               # CI 工作流
└── docs/                    # 文档
```

### 6.8.2 开发工作流

**本地开发流程：**

```bash
# 1. 启动后端（端口 8825）
cd backend
./mvnw spring-boot:run

# 2. 启动前端（端口 3000）
cd frontend
npm run dev

# 3. 访问 http://localhost:3000
```

**代码检查流程：**

```bash
# 1. 类型检查
npm run type-check

# 2. ESLint 检查
npm run lint:check

# 3. Prettier 检查
npm run format:check

# 4. 自动修复
npm run lint
npm run format

# 5. 运行测试
npm run test:run

# 6. 生产构建
npm run build
```

### 6.8.3 质量闸门

SelfBlog 定义了严格的质量闸门：

| 闸门 | 命令 | 说明 |
|------|------|------|
| 类型检查 | `npm run type-check` | TypeScript 类型必须正确 |
| 代码检查 | `npm run lint:check` | ESLint 规则必须通过 |
| 格式检查 | `npm run format:check` | Prettier 格式必须一致 |
| 单元测试 | `npm run test:run` | 所有测试必须通过 |
| 部署测试 | `npm run test:deployment` | 部署契约必须满足 |
| 生产构建 | `npm run build` | 构建必须成功 |

**CI 执行顺序：**

```
npm ci
  ↓
npm run lint:check
  ↓
npm run format:check
  ↓
npm run type-check
  ↓
npm run test:run
  ↓
npm run test:deployment
  ↓
npm run build
```

### 6.8.4 依赖管理策略

**生产依赖（dependencies）：**

```json
{
  "dependencies": {
    "axios": "^1.7.4",
    "dompurify": "^3.0.9",
    "marked": "^12.0.1",
    "pinia": "^2.1.7",
    "vue": "^3.5.44",
    "vue-router": "^4.3.0"
  }
}
```

**开发依赖（devDependencies）：**

```json
{
  "devDependencies": {
    "@eslint/js": "^9.39.4",
    "@testing-library/jest-dom": "^6.9.1",
    "@testing-library/vue": "^8.1.0",
    "@vitejs/plugin-vue": "^5.0.4",
    "@vue/test-utils": "^2.4.10",
    "@vue/tsconfig": "^0.9.1",
    "eslint": "^9.39.4",
    "eslint-config-prettier": "^10.1.8",
    "eslint-plugin-vue": "^10.9.1",
    "jsdom": "^29.1.1",
    "prettier": "^3.8.3",
    "typescript": "^6.0.3",
    "typescript-eslint": "^8.60.1",
    "vite": "^6.4.4",
    "vitest": "^4.1.8",
    "vue-tsc": "^3.3.3"
  }
}
```

**依赖分类原则：**

| 类别 | 说明 | 示例 |
|------|------|------|
| 生产依赖 | 运行时需要的库 | vue, vue-router, pinia, axios |
| 开发依赖 | 开发/构建/测试工具 | vite, eslint, prettier, vitest |

### 6.8.5 环境配置

**开发环境（.env）：**

```env
VITE_APP_TITLE=个人博客
VITE_API_TARGET=http://localhost:8825
VITE_API_BASE_URL=/api
VITE_APP_VERSION=2.0.25
```

**生产环境：**

生产环境的前端构建后内嵌到后端 JAR 中，不需要单独的环境变量配置。

### 6.8.6 构建产物分析

**构建输出结构：**

```
dist/
├── index.html              # 入口 HTML
├── assets/
│   ├── index-[hash].js     # 业务代码
│   ├── vendor-[hash].js    # Vue 生态
│   ├── markdown-[hash].js  # Markdown 渲染
│   └── index-[hash].css    # 样式
└── favicon.ico             # 站点图标
```

**构建优化：**

1. **代码分割**：第三方库独立 chunk
2. **Tree-shaking**：移除未使用的代码
3. **压缩**：esbuild 压缩 JS/CSS
4. **哈希**：文件名包含内容哈希，支持长期缓存

### 6.8.7 错误处理与监控

**前端错误处理：**

```typescript
// src/api/index.ts
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 响应拦截器
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 统一错误处理
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 未授权，跳转登录
          break
        case 403:
          // 禁止访问
          break
        case 404:
          // 资源不存在
          break
        case 500:
          // 服务器错误
          break
      }
    }
    return Promise.reject(error)
  }
)
```

### 6.8.8 性能优化入门：代码分割与请求缓存

> 本节是通用入门；SelfBlog 的完整性能清单（构建产物/运行时/网络/测量四层）见 6.8.10。

**代码分割：**

```typescript
// 路由懒加载
const routes = [
  {
    path: '/',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/about',
    component: () => import('@/views/About.vue')
  }
]
```

**图片优化：**

```vue
<template>
  <!-- 使用 WebP 格式 -->
  <img src="@/assets/image.webp" alt="description" loading="lazy">
</template>
```

**缓存策略：**

```typescript
// 使用浏览器缓存
const cache = new Map()

async function fetchData(key: string) {
  if (cache.has(key)) {
    return cache.get(key)
  }
  const data = await api.get(key)
  cache.set(key, data)
  return data
}
```

### 6.8.9 部署形态：Vercel 代理生成器与 Docker（SelfBlog 实战）

SelfBlog 的部署有一个非常容易误解的设计：**`frontend/vercel.json` 不是静态站点配置，而是一个反向代理生成器**。

```json
{
  "framework": null,
  "buildCommand": "node scripts/build-vercel-proxy.mjs",
  "routes": "（由脚本生成 Build Output v3 格式的转发规则）"
}
```

- `framework: null`：明确告诉 Vercel「别猜，我不需要你注入默认构建」
- `scripts/build-vercel-proxy.mjs`：构建期生成 Build Output v3 的 forwarding 规则，把 `/api/*` 与 `/uploads/*` 转发到唯一后端 origin；**无效或自引用的 origin 直接让部署失败**（fail fast 优于上线后 404）
- 需要两个环境变量：`SELFBLOG_BACKEND_ORIGIN`（后端地址）与 `SITE_BASE_URL`（站点规范域名）

为什么这么做？前后端分离部署时，前端域名与后端 API 域名不同源会有 CORS 问题。统一反代理后浏览器看到的一切请求都同源——**用部署拓扑消灭 CORS，而不是在代码里到处配 CORS 头**。这套适配器有专门的契约测试：`npm run test:deployment`（27 项断言，同时覆盖 Vercel 代理生成器与 Docker/Compose 契约）。

**Docker 部署则相反**：单容器包含前后端——`backend/Dockerfile` 以仓库根为构建上下文，把已构建的 `selfblog.jar`（内嵌前端产物）与数据卷、上传卷、相册卷一起编排，`docker compose up -d --build` 一条命令起全栈，健康检查靠 `/actuator/health`。

> 诚实声明（SelfBlog 文档的惯例）：真实 Vercel 部署与搜索引擎收录从未真机验证过，`vercel.json` 的正确性由契约测试背书；Docker 镜像在无 Docker 守护进程的本机也未验证过。**验证记录与代码同等重要——「我没测过」必须写进文档**，这是工程诚信的一部分。

### 6.8.10 前端性能优化：SelfBlog 的务实清单

性能优化最忌「上来就背清单」。正确的姿势：**先测量，再优化，优化完再测量**。SelfBlog 的做法展示了单人博客场景下「够用且有效」的取舍。

**① 构建产物层面（一次性投入，长期收益）**

```
构建策略                SelfBlog 的做法                     效果
路由懒加载              每个页面 () => import('...')        首屏只加载访问到的 chunk
Tree-shaking            全 ESM 依赖（Vite 生产走 Rollup）    未引用代码不进产物
依赖瘦身                审查 package.json，能原生就原生      少一个依赖少一份体积
生产构建压缩            Vite 默认 esbuild 压缩              开箱即得
```

验证手段：`npm run build` 后看产物清单里每个 chunk 的体积，突然变大的 chunk 要问为什么。**警惕**：某个组件 import 了一个全家桶库（如只用了 `lodash` 一个函数却整包引入），改 `import debounce from 'lodash/debounce'` 就能瘦身。

**② 运行时层面（响应性纪律）**

- **computed 缓存派生值**：模板里写 `{{ filteredList.length }}` 每次 render 都重算；提到 `computed` 里只依赖变化时重算
- **v-for 必带稳定 key**：用业务 id 而非数组下标——下标 key 在列表头部插入时会让 Vue 复错节点
- **大列表虚拟化**：博客文章归档页几百条尚可直渲染；上千条上虚拟列表（SelfBlog 未用，规模不需要——诚实评估，不为炫技上方案）
- **事件防抖**：搜索框输入触发请求用 `debounce(300)`；SelfBlog 的滚动监听用 `passive: true` 的原生监听而不是 Vue 响应式
- **组件级缓存**：路由出口 `<KeepAlive>` 包住归档页这类「进出频繁、重渲染昂贵」的页面

**③ 网络层面（利用后端已有的设计）**

- **HTTP 缓存**：静态资源 Vite 产物带 hash 文件名 → `Cache-Control: max-age=31536000, immutable`；`index.html` 不缓存 → 版本更新立即生效
- **接口聚合**：页面需要的多个数据尽量一个接口返回（后端 `Result` 信封天然支持多数据块），减少请求往返
- **缓存兜底**：`stores/site.ts` 的默认值设计让「后端挂了页面照样渲染」——这同时是可用性与「感知性能」的双保险

**④ 测量工具（没有测量就没有优化）**

```bash
npm run build          # 看 chunk 体积分布
# Chrome DevTools:
#   Lighthouse 面板    → 性能评分 + LCP/FCP/CLS 三大指标
#   Performance 面板   → 录制交互，看长任务（>50ms 的红色块）
#   Network 面板       → 按体积排序，找异常大的资源
```

**单人博客的边界**：不追求 100 分 Lighthouse，追求「文章秒开、交互不卡」。SelfBlog 把主要精力花在 SEO 可读 HTML（对爬虫与低网速用户的意义远大于锦上添花的动画）——**性能优化的第一原则：优化对用户可感知的部分**。

### 6.8.11 前端安全实战：XSS、令牌存储与游客身份

安全不是后端一个人的事。前端是所有用户输入的**第一现场**，也是令牌的**保管员**。

**① XSS：前端防线的三层**

XSS（跨站脚本）的本质：攻击者的字符串被浏览器当成了代码。三层防线各司其职：

```typescript
// 第一层：框架默认转义——Vue 模板插值自动 HTML 转义
// {{ user.nickname }} 即使内容是 <script>... 也只是文本，不会执行
// 危险点只有一个：v-html——它绕过转义，把字符串当 HTML 插入
<div v-html="renderedMarkdown"></div>   // 只允许「可信来源+已消毒」的内容走这里
```

```typescript
// 第二层：入库前消毒是后端的事（SelfBlog 的 htmlSanitizer.sanitizePlainText），
// 但前端也要防御「来自后端的数据」被二次污染——比如评论内容里带 <img onerror>
// 规则：凡是要 v-html 的内容，必须经过 DOMPurify 这类库清洗
import DOMPurify from 'dompurify'
const safe = DOMPurify.sanitize(rawHtml)
```

```typescript
// 第三层：CSP（内容安全策略）——即使前两层失守，浏览器拒绝执行未授权来源的脚本
// 由后端响应头下发：Content-Security-Policy: script-src 'self'
```

SelfBlog 的分工：**后端消毒入库（GuestCommentServiceImpl 先 sanitize 再校验）→ 前端默认插值渲染 → SEO 可读 HTML 由 SafeMarkdown 白名单渲染**。三层各挡一次。

**② 令牌存储：localStorage vs cookie 的诚实权衡**

| 方案 | XSS 可读 | CSRF 风险 | SelfBlog 的选择 |
|------|----------|-----------|-----------------|
| localStorage | ✗ 可被读走 | 无（不自动携带） | ✅ 采用 + 双重防御 |
| HttpOnly cookie | ✓ 读不到 | 有（自动携带，需 CSRF 防御） | 后端备有 XSRF-TOKEN 机制 |

localStorage 怕 XSS，cookie 怕 CSRF——没有完美方案，只有**纵深防御**：SelfBlog 用 localStorage + Axios 自动附加 `X-XSRF-TOKEN` 头（cookie 里的 XSRF token 由后端校验），同时用严格的 XSS 三层防线压低「token 被读走」的概率。**安全是概率工程，不是非黑即白。**

**②的展开：CSRF「双提交」（Double Submit）到底防住了什么**。先看攻击面：CSRF 之所以存在，是因为**浏览器在任何跨站请求里都会自动携带目标域的 cookie**——恶意网站放一个 `<form action="https://blog.com/api/admin/post/delete" method="POST">` 再用脚本提交，受害者的浏览器就带着合法 cookie 发出了删除请求，网站无法区分「这是恶意站点代发的」。防御思路因此分两派：让请求「自带不可伪造的凭证」。

**双提交模式**正是后者的经典实现——同一个随机 token 存两份：一份在 **cookie**（`XSRF-TOKEN`），一份由前端 JS 读出后放进**请求头**（`X-XSRF-TOKEN`，Axios 对 cookie 名 `XSRF-TOKEN` 自动这么干）。后端只收「头与 cookie 值相等」的请求。它为什么有效：**跨站攻击者能强迫浏览器带上 cookie（这是浏览器行为，拦不住），却读不到 cookie**（同源策略禁止跨域 JS 读取他域文档/存储），自然也放不进自定义头——两个值对不上，请求被拒。

```text
正常用户：cookie XSRF-TOKEN=abc  +  头 X-XSRF-TOKEN=abc（JS 从 cookie 读出再放头里）→ 匹配 ✅
攻击者：  cookie XSRF-TOKEN=abc  +  无头（读不到 cookie，放不了头）        → 401/403 ❌
```

SameSite cookie 属性（`SameSite=Lax/Strict`）是补充防线：让浏览器干脆不给跨站请求带 cookie。二者叠加后 CSRF 在现代浏览器里已很难得手。

**CSP 概念展开**：`Content-Security-Policy` 是服务器下发给浏览器的「白名单清单」，核心指令如 `default-src 'self'; script-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'`——分别是脚本/图片/样式允许的来源。它对 XSS 的意义是**兜底**：即使前两层失守、恶意 `<script>` 真被插进页面，浏览器发现来源不在白名单也会拒绝执行。Vue SPA 的常见痛点是 `'unsafe-inline'`（Vue 的运行时注入与内联样式需要它，削弱了 CSP 强度），更严的做法是 nonce/hash 方案。SelfBlog 的 CSP 由后端响应头下发，前端只需理解：**CSP 报错（Console 里的 `Refused to …`）不是 bug，是防线在工作**。

**③ token 的生命周期管理（stores/user.ts 的三个细节）**

- **存取经过编码**：token 经 `utils/crypto` 编解码后进 localStorage——不是加密，是防「一眼明文」的最低限
- **本地校验优先**：`validateToken` 只解 JWT 的 exp 判断过期，不联网——避免每次路由跳转都打后端
- **失效即清理**：拦截器 `handleAuthError()` 清 Pinia + localStorage 并跳 `adminRoot`，绝不让半死状态残留

**④ 游客身份：X-Visitor-Id 的设计**

匿名点赞/评论需要「不登录也能识别同一个人」。SelfBlog 的方案：

```typescript
// utils/visitor.ts 的思想：
function getOrCreateVisitorId(): string {
  let id = localStorage.getItem('visitor_id')
  if (!id) {
    id = crypto.randomUUID()          // 浏览器原生 UUID，无需后端参与
    localStorage.setItem('visitor_id', id)
  }
  return id
}
// Axios 请求拦截器给公开写接口统一附加 X-Visitor-Id 头
```

配套的**后端信任边界**（决定这套方案是否安全的关键）：visitorId 只作为「关联键」——后端校验格式白名单（`[A-Za-z0-9_-]{8,80}`）、只存加盐哈希（`VisitorHashUtils.hash(visitorId, secret)`）、权限永远与服务端校验挂钩。**客户端生成的任何身份标识都不可信，可信的只有它经过服务端约束后的形态。**

**⑤ 前端安全自查清单**

- [ ] 有没有 v-html？它的内容来源可信且消毒过吗？
- [ ] 敏感数据有没有出现在 URL / 日志 / 错误上报里？
- [ ] 登出时清干净了吗（Pinia + localStorage + 内存变量三处）？
- [ ] 路由守卫的鉴权判断只是 UX——**每个接口后端都重新校验了吗**（前端鉴权永远可绕过，它只负责体验）？
- [ ] 依赖有没有已知漏洞（`npm audit` 进 CI 是加分项）？

---

### 6.8.12 浏览器与 HTTP：前端工程的底层契约

工程化工具解决了「代码怎么组织」，但代码最终跑在浏览器里、数据走 HTTP——理解这两层底层契约，很多「玄学问题」会变成「常识问题」。

**一次页面加载的完整旅程**（SelfBlog 首页为例）：DNS 解析 → TCP/TLS 握手 → 请求 `index.html`（后端 SpaController/SEO 路由返回）→ 解析 HTML 构建 DOM → 遇到 `<script type="module">` 加载 JS（Vite 开发态按需、生产态打包）→ JS 执行到 `mount('#app')` → Vue 渲染 → Axios 发 `/api/*` 拿数据 → 二次渲染。**首屏时间的大头通常在 JS 体积与接口串联**，这解释了 6.8.10 为什么把「构建产物」列为优化第一层。

**HTTP 缓存：构建工具哈希文件名的底层依据**。浏览器缓存分两层：

- **强缓存**：响应头 `Cache-Control: max-age=31536000` 有效期内**不发请求**直接用本地副本。Vite 生产构建给每个产物文件名加内容哈希（`index-C8fH2x.js`），内容一变文件名就变——所以可以放心让 JS/CSS **强缓存一年**。
- **协商缓存**：强缓存过期后带 `ETag`/`If-None-Match` 问服务器「变了没」，没变返回 304 省流量，变了返回新内容。`index.html` 必须走协商缓存（它引用着会变的哈希文件名），否则用户拿旧 HTML 引用已删除的旧 JS 就白屏。

一句话记住 SelfBlog 式约定：**带哈希的文件强缓存，入口 HTML 协商缓存**。这也是为什么不能用「改了代码用户要清缓存」来搪塞线上问题——缓存策略写错了，才是要用户清缓存的原因。

**同源策略与 CORS：为什么开发要用代理**。浏览器规定：协议、域名、端口任一不同即**跨域**，跨域响应默认被浏览器拦下（请求其实发出去了，是响应被扣）。`http://localhost:3000`（Vite）调 `http://localhost:8825`（Spring Boot）就是跨域。解法两条：后端加 CORS 响应头，或**让请求看起来同源**——SelfBlog 两条都用了：开发态 Vite 代理把 `/api` 转发到 8825（浏览器只看到同源的 3000），生产态 `vercel.json` 反向代理同理。**用部署拓扑消灭 CORS**（6.8.9 节）不是取巧，是生产环境更安全的正统做法： cookie/token 不暴露给第三方域，也省掉一堆 `Access-Control-Allow-*` 配置。

**连接限制：HTTP/1.1 的六连接现实**。同一域名下 HTTP/1.1 最多 6 个并发 TCP 连接，第 7 个请求排队。这解释了三件事：① 6.8.10 强调雪碧图/合并请求的历史由来；② ch03 §3.16 讲 SSE 时「EventSource 占一个常驻连接，开多了挤占并发额度」的原因；③ HTTP/2 多路复用（单连接并行多请求）为什么是质变——SelfBlog 部署在反代之后，若反代与浏览器协商出 HTTP/2，前端无需改一行代码就吃到红利。

**浏览器渲染管线与 CSS token 的关系**：浏览器把 CSS 解析成 CSSOM、HTML 解析成 DOM，二者合成渲染树 → 布局（回流）→ 绘制（重绘）。**改 `width`/`top` 触发回流（贵），改颜色/透明度只触发重绘（便宜），改 `transform`/`opacity` 走合成层（最便宜）**。SelfBlog 的皮肤系统（`skins.css` 的 `#selfblog[data-skin=…]`）与暗色模式（`.dark-mode` 根类 + CSS 变量覆盖）之所以高效，正因为它们只切换**变量与类名**——浏览器只需对受影响元素重算样式，不触碰布局。6.5.7 提到的「不要拿 `--gray-*` 当深色表面」的暗色反转问题，本质就是变量在 `dark-mode.css` 里被整体重定义——理解了变量的级联机制，设计 token 才不会用错。

**存储五兄弟速查**（前端持久化的全部选项）：

| 机制 | 容量 | 生命周期 | 是否随请求自动发送 | SelfBlog 用途 |
|------|------|---------|------------------|--------------|
| localStorage | ~5MB | 永久（手动删） | 否 | token（经 `utils/crypto` 编解码）、`edu_theme` |
| sessionStorage | ~5MB | 标签页关闭 | 否 | 一次性向导状态 |
| cookie | ~4KB | 可设过期 | **每次同源请求自动带上** | 服务端会话（SelfBlog 未用） |
| IndexedDB | 大 | 永久 | 否 | 离线应用、音乐缓存（未用，够用原则） |
| 内存（Pinia） | - | 刷新即失 | 否 | 运行时状态真身，localStorage 只是「恢复现场」的备份 |

token 放 localStorage 还是 cookie 的权衡在 6.8.11 已展开：XSS 可读 localStorage vs CSRF 打 cookie，SelfBlog 用「localStorage + Bearer 头」并把防线压在输出消毒与 CSP 上。理解这张表，就能判断任何「存哪儿」的问题。

**DevTools 调试技巧与 Lighthouse 评估**——6.8.10 已给出面板速查（Performance/Network/Lighthouse），这里补三个高频排查手法：

- **Network 面板抓接口问题**：勾选 `Disable cache`（DevTools 打开期间禁用缓存）排除缓存干扰；右键列勾选 `Initiator` 看请求由哪行代码发出（定位重复请求）；status 列分颜色——灰色 304 是协商缓存生效（正常），红色 4xx/5xx 才是真错误；Slow/瓶颈排序定位慢接口后，到 Timing 标签区分「等待服务器（TTFB）」还是「下载大响应」。
- **Elements 面板调试 Vue**：Vue DevTools 浏览器插件可看组件树与 Pinia 状态；没装插件时，Elements 里选中 DOM 后控制台 `$0` 就是该元素，配合 `__VUE_DEVTOOLS_GLOBAL_HOOK__` 或直接在代码里 `console.log` store 响应式对象（Pinia store 打印出来是可展开的 Proxy）。
- **Lighthouse 读数姿势**：性能分不是唯一指标——展开指标看 **LCP**（最大内容绘制，SelfBlog 首屏通常是标题区，超过 2.5s 才算差）、**CLS**（累计布局偏移，图片/骨架屏导致的跳动，SelfBlog 用 skeleton.css 预占位控制）、**TBT**（长任务阻塞）。注意：本地跑分总是偏高，Vercel 生产环境 + 无缓存设备才是真实分数；调试 Motivation 里「Opportunities/Diagnostics」每一项都给了具体建议与预计节省。

## 6.9 版本控制与协作：Git 工程化

前六章的所有代码变化，最终都要落成一次次提交。Git 命令在附录 B.3 已列速查表，这里补「**协作层面的工程化**」——分支模型、提交信息规范与仓库卫生。

**分支模型（单人项目也有用）**。SelfBlog 是单人作者项目，最贴合的是 **GitHub Flow**：`main` 永远可部署，任何改动开短命分支 → 提 PR → CI 绿 → 合并删除。比 Git Flow（develop/release/hotfix 多分支并行）轻得多，适合独立开发者与小团队。纪律只有两条：①分支寿命越短越好（几天内合并，避免长期分叉的合并地狱）；②**CI 必须全绿才允许合并**——6.7.6 的五道闸门正是为此存在。

```bash
git switch -c feat/comment-emoji    # 创建并切换（新语法，替代 checkout -b）
# …开发、commit…
git push -u origin feat/comment-emoji
# GitHub 上开 PR，等 ci.yml 四个 job 全绿，merge，然后：
git switch main && git pull && git branch -d feat/comment-emoji
```

**提交信息规范：Conventional Commits**。格式 `<type>(<scope>): <subject>`，type 常用 `feat`（新功能）/ `fix`（修复）/ `docs`（文档）/ `style`（格式）/ `refactor`（重构）/ `test` / `chore`（构建与杂务）。好处不只是好看：CHANGELOG 自动化、语义化版本升降级可推断（`feat` → 次版本 +1，带 `!` 或 `BREAKING CHANGE` 脚注 → 主版本 +1）、`git log --oneline` 一眼读出演进史。

```bash
git commit -m "feat(post): 文章详情支持代码块高亮主题切换"
git commit -m "fix(auth): 刷新令牌并发时重复请求问题（isRefreshing 排队）"
git commit -m "docs!: 调整目录结构为两工程制"   # ! 表示 breaking change
```

**.gitignore 与仓库卫生**。原则：**生成物不入库**——`node_modules/`、`dist/`、构建缓存、`.env`（密钥！`.env.example` 才入库）、编辑器目录。SelfBlog 特别注意两处：`backend/target/`（Maven 产物）与 `frontend/dist/`；而 `backend/campus_blog.db`（SQLite 数据文件）按需决定——开发库想共享就提交，生产库绝不入库。`.gitignore` 只对未跟踪文件生效，已被跟踪的文件要 `git rm --cached <file>` 移出索引。

**.editorconfig**。跨编辑器统一缩进/换行/编码的 humble 配置，放在仓库根，VS Code / JetBrains / Vim 都认：

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
indent_style = space
indent_size = 2

[*.md]
trim_trailing_whitespace = false   # Markdown 行尾双空格是换行语法
```

它与 Prettier 分工：EditorConfig 管**所有文件**的基础属性（缩进、换行符、编码），Prettier 管**代码文件**的风格细节——两层配合后，`endOfLine: "auto"` 在 Windows 上才真正稳定（文件层面 LF 已被统一，Prettier 只需按现存风格跟随）。

**单人项目的提交纪律**：没有队友不等于没有规范——三个月后的你就是队友。每次提交聚焦一件事（一个 `feat` 或一个 `fix`），提交前跑一遍五道闸门（6.7.6），绝不提交 `console.log` 调试残留与注释掉的死代码（要删就真删，Git 历史会替你记住）。回滚靠 `git revert`（生成反向提交，历史完整可追溯）而不是 `reset --force` 推公共分支——单人项目一旦开了 Actions，`main` 就是公共分支。

**六个 6.9.x 小节**最后来一次总收束——

### 6.9.1 工程化清单

**项目初始化：**

- [ ] 选择合适的 Node.js 版本
- [ ] 初始化 `package.json`
- [ ] 配置 `.gitignore`
- [ ] 配置 `.editorconfig`

**开发环境：**

- [ ] 配置 Vite
- [ ] 配置 TypeScript
- [ ] 配置 ESLint
- [ ] 配置 Prettier
- [ ] 配置路径别名

**代码质量：**

- [ ] 启用 TypeScript 严格模式
- [ ] 配置 ESLint 规则
- [ ] 配置 Prettier 规则
- [ ] 配置编辑器自动修复

**测试：**

- [ ] 配置 Vitest
- [ ] 编写单元测试
- [ ] 配置覆盖率报告
- [ ] 设置测试阈值

**CI/CD：**

- [ ] 配置 GitHub Actions
- [ ] 设置质量闸门
- [ ] 配置自动部署
- [ ] 配置监控告警

### 6.9.2 常见问题与解决方案

**问题 1：Node.js 版本不兼容**

```bash
# 错误
Error: The engine "node" is incompatible with this module.

# 解决方案
nvm install 22.22.2
nvm use 22.22.2
```

**问题 2：依赖安装失败**

```bash
# 清除缓存
npm cache clean --force

# 删除 node_modules 和锁文件
rm -rf node_modules package-lock.json

# 重新安装
npm install
```

**问题 3：ESLint 与 Prettier 冲突**

```javascript
// eslint.config.js
import prettier from 'eslint-config-prettier'

export default tseslint.config(
  // ... 其他配置
  prettier  // 必须放在最后
)
```

**问题 4：TypeScript 类型错误**

```typescript
// 错误：Object is possibly 'undefined'
const value = arr[0].name

// 解决方案 1：可选链
const value = arr[0]?.name

// 解决方案 2：非空断言
const value = arr[0]!.name

// 解决方案 3：类型守卫
if (arr[0]) {
  const value = arr[0].name
}
```

### 6.9.3 推荐工具链

| 类别 | 工具 | 版本 |
|------|------|------|
| 运行时 | Node.js | 22.22.2+ |
| 包管理 | npm | 10+ |
| 构建工具 | Vite | 6+ |
| 类型系统 | TypeScript | 6+ |
| 代码检查 | ESLint | 9+ |
| 代码格式化 | Prettier | 3+ |
| 测试框架 | Vitest | 4+ |
| 组件测试 | @vue/test-utils | 2+ |
| DOM 模拟 | jsdom | 29+ |

### 6.9.4 进一步学习

**官方文档：**

- [Vite 官方文档](https://vitejs.dev/)
- [TypeScript 官方文档](https://www.typescriptlang.org/)
- [ESLint 官方文档](https://eslint.org/)
- [Prettier 官方文档](https://prettier.io/)
- [Vitest 官方文档](https://vitest.dev/)
- [GitHub Actions 官方文档](https://docs.github.com/en/actions)

**社区资源：**

- [Vue 3 官方文档](https://vuejs.org/)
- [Pinia 官方文档](https://pinia.vuejs.org/)
- [Vue Router 官方文档](https://router.vuejs.org/)

---

## 附录 A：配置文件模板

### A.1 package.json

```json
{
  "name": "my-project",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview",
    "lint": "eslint src --fix",
    "lint:check": "eslint src",
    "format": "prettier --write \"src/**/*.{ts,tsx,vue,css,json}\"",
    "format:check": "prettier --check \"src/**/*.{ts,tsx,vue,css,json}\"",
    "type-check": "vue-tsc --noEmit",
    "test": "vitest",
    "test:run": "vitest run",
    "test:coverage": "vitest run --coverage"
  },
  "dependencies": {
    "vue": "^3.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "typescript": "^6.0.0",
    "vite": "^6.0.0",
    "vue-tsc": "^3.0.0",
    "eslint": "^9.0.0",
    "prettier": "^3.0.0",
    "vitest": "^4.0.0"
  }
}
```

### A.2 vite.config.ts

```typescript
/// <reference types="vitest" />
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts']
  },
  server: {
    port: 3000
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    minify: 'esbuild'
  }
})
```

### A.3 tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    },
    "types": ["vite/client"]
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue", "src/**/*.d.ts"],
  "exclude": ["node_modules", "dist"]
}
```

### A.4 eslint.config.js

```javascript
import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'
import globals from 'globals'

export default tseslint.config(
  {
    ignores: ['dist/**', 'node_modules/**', '*.d.ts']
  },
  {
    languageOptions: {
      globals: {
        ...globals.browser
      }
    }
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  prettier,
  {
    files: ['**/*.{ts,tsx,vue}'],
    rules: {
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      '@typescript-eslint/no-explicit-any': 'warn',
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'warn'
    }
  }
)
```

### A.5 .prettierrc

```json
{
  "semi": false,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "none",
  "printWidth": 100,
  "bracketSpacing": true,
  "arrowParens": "always",
  "endOfLine": "auto"
}
```

### A.6 .github/workflows/ci.yml

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

jobs:
  frontend:
    name: Frontend checks
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint:check
      - run: npm run format:check
      - run: npm run type-check
      - run: npm run test:run
      - run: npm run build
```

---

## 附录 B：常用命令速查

### B.1 npm 命令

```bash
# 安装依赖
npm install                    # 安装所有依赖
npm install <package>          # 安装生产依赖
npm install -D <package>       # 安装开发依赖
npm ci                         # 严格按锁文件安装

# 卸载依赖
npm uninstall <package>

# 更新依赖
npm update                     # 更新所有依赖
npm update <package>           # 更新指定依赖

# 查看依赖
npm list                       # 查看已安装依赖
npm list --depth=0             # 查看顶层依赖
npm outdated                   # 查看过期依赖

# 运行脚本
npm run <script>               # 运行脚本
npm run <script> -- <args>     # 运行脚本并传参

# 其他
npm init                       # 初始化 package.json
npm init -y                    # 快速初始化
```

### B.2 Vite 命令

```bash
# 开发
npm run dev                    # 启动开发服务器
npx vite                       # 启动开发服务器
npx vite --port 3001           # 指定端口

# 构建
npm run build                  # 生产构建
npx vite build                 # 生产构建
npx vite build --mode test     # 指定模式构建

# 预览
npm run preview                # 预览生产构建
npx vite preview               # 预览生产构建
```

### B.3 Git 命令

```bash
# 初始化
git init                       # 初始化仓库
git clone <url>                # 克隆仓库

# 基本操作
git add <file>                 # 暂存文件
git commit -m "message"        # 提交
git push                       # 推送
git pull                       # 拉取

# 分支操作
git branch                     # 查看分支
git checkout <branch>          # 切换分支
git checkout -b <branch>       # 创建并切换分支
git merge <branch>             # 合并分支

# 查看状态
git status                     # 查看状态
git log                        # 查看提交历史
git diff                       # 查看差异
```

---

## 附录 C：术语表

| 术语 | 说明 |
|------|------|
| CI/CD | 持续集成/持续部署 |
| ESM | ES 模块（ECMAScript Modules） |
| HMR | 热模块替换（Hot Module Replacement） |
| SFC | 单文件组件（Single File Component） |
| SemVer | 语义化版本（Semantic Versioning） |
| Tree-shaking | 移除未使用的代码 |
| V8 | Google 的 JavaScript 引擎 |
| jsdom | JavaScript 实现的 DOM |
| mock | 模拟对象/函数 |
| stub | 桩对象/函数 |
| fixture | 测试固件 |

---

**本章完**

> 下一章将深入讲解 SelfBlog 后端工程化实践，包括 Spring Boot 配置、数据库设计、API 设计、测试策略等内容。

---

# 第七章 项目实战：从零理解 SelfBlog 全栈开发

> 前六章我们分别学了 Java 基础、Java 进阶、Spring Boot、Vue 3 与前端工程化。本章是整个教程的「毕业设计」：我们把 SelfBlog v2.0.26 这个真实项目从架构到部署完整拆开，看一个由多用户校园社区改造而来的单人作者博客，是如何把前面学到的所有知识串成一条完整的全栈链路的。读这一章时，强烈建议打开仓库源码对照阅读——本章引用的类名、路径、行为都来自真实代码，而不是教学示例。

## 7.1 项目总览与架构图

### 7.1.1 这个项目是什么

SelfBlog 是一个**单人作者个人博客**：访客可以阅读、搜索、匿名点赞、匿名留言，但注册入口已经关闭，所有写入操作（发文章、管标签、传图片、改站点设置）只属于一位管理员作者。它的前身 `campus_blog` 是一个多用户校园社区（有注册、关注、校友圈、话题、通知、草稿箱、举报……），v2.0 的改造把这个社区「瘦身」成了一个人的书房。这个背景决定了它的很多设计决策，我们会在 7.14 节专门复盘。

技术栈一览：

| 层 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.3.13 |
| 语言 | Java | 21 |
| ORM | MyBatis Plus | 3.5.8 |
| 安全 | Spring Security + JWT (jjwt) | — / 0.12.6 |
| 本地缓存 | Caffeine | — |
| 连接池 | HikariCP | — |
| 前端框架 | Vue | 3.5.44 |
| 构建工具 | Vite | 6 |
| 状态管理 | Pinia | 2.1.7 |
| 语言 | TypeScript | 6.0.3 |
| 单元测试 | Vitest | 4 |
| HTTP 客户端 | Axios | 1.7 |

### 7.1.2 整体架构图

SelfBlog 是一个**单体应用**：一个 Spring Boot JAR 同时提供 REST API 和前端页面（前端构建产物被内嵌进 JAR），这是单人博客最合理的部署形态——一个进程、一个端口、一个数据库文件就能跑。

```
                         ┌───────────────────────────────────┐
                         │        浏览器 / 爬虫 / RSS 阅读器   │
                         └────────┬───────────────┬──────────┘
                                  │               │
              页面请求（根路径）    │               │  REST 请求（/api/**）
                                  ▼               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    Spring Boot 单体应用（selfblog.jar : 8825）             │
│                                                                          │
│  ┌──────────────────────┐        ┌────────────────────────────────────┐ │
│  │  @Controller（无前缀） │        │  @RestController（自动加 /api 前缀）│ │
│  │  SpaController        │        │  BlogPostController /post          │ │
│  │   / /post/{id} /about │        │  SysUserController   /user         │ │
│  │  SeoController        │        │  Admin*Controller    /admin/**     │ │
│  │   /feed.xml /sitemap  │        │  其余 30+ 业务控制器                 │ │
│  └──────────┬───────────┘        └───────────────┬────────────────────┘ │
│             │                                    │                      │
│             │  service/site/seo/                 │  Service 三分：        │
│             │  ReadingPageService                │  门面 / 查询 / 交互     │
│             │  组装「可直接阅读的 HTML」            │  BlogPostService      │
│             │                                    │  ├ PostQueryService   │
│             ▼                                    │  └ PostInteraction    │
│  ┌──────────────────────┐                        └───────────┬──────────┘ │
│  │  HTML 模板 + ETag     │                                    ▼            │
│  │  304 / 200 / 404      │                        ┌──────────────────────┐  │
│  └──────────────────────┘                        │  Mapper（MyBatis Plus)│  │
│                                                  │  手写 SQL 双方言       │  │
│                                                  └──────────┬───────────┘  │
│  横切：JwtAuthenticationFilter → RateLimitInterceptor           │          │
│        Caffeine 8 个具名缓存 → GlobalExceptionHandler            │          │
└──────────────────────────────────────────────────────────────┼──────────┘
                                                               ▼
                                             ┌───────────────────────────┐
                                             │  数据库（三选一，DB_TYPE）   │
                                             │  SQLite（默认单文件）        │
                                             │  H2（测试内存库）           │
                                             │  MySQL 8（生产可选）        │
                                             └───────────────────────────┘
```

### 7.1.3 一条请求的完整链路

以访客打开一篇文章为例，请求从浏览器到数据库要经过这些关卡：

```
浏览器 Vue Router(/post/42)
  → Axios 拦截器（注入 Authorization / X-XSRF-TOKEN / X-Visitor-Id / X-Trace-Id）
    → Vite 代理（开发期）/ Vercel 反代（生产期）
      → Spring Security 过滤器链
        → JwtAuthenticationFilter（有 token 就解析，无 token 当匿名）
          → RateLimitInterceptor（/api/** 按 IP+路径 1 分钟窗口计数）
            → DispatcherServlet（WebMvcConfig 已把 @RestController 路径加 /api 前缀）
              → Controller（取 userId、调门面）
                → 门面 Service（CRUD + 编排）
                  → 查询 Service（@Transactional(readOnly=true) + @Cacheable）
                    → Mapper（手写 SQL 带 is_deleted=0，或 MP 方法）
                      → Hikari → SQLite/MySQL
```

### 7.1.4 为什么这样分层

**为什么 Controller 很薄？** Controller 只做三件事：参数绑定与校验（`@Valid`）、身份提取（`SecurityUtils.getCurrentUserId()`）、把 Service 结果包成 `Result<T>`。业务规则全部下沉，Controller 不写 if-else 业务判断。这样 `@WebMvcTest` 才能只 mock 一个 Service 就测完所有路由行为。

**为什么 Service 要拆成「门面 / 查询 / 交互」三个？** 这是本项目最值得学的结构决策。`BlogPostService`（门面）负责 CRUD 和跨服务编排，`PostQueryService` 只读查询，`PostInteractionService` 管计数副作用。拆开的原因是三者的**事务语义完全不同**：查询要 `readOnly=true` 让数据库优化、还能配合 `@Cacheable`；计数是写操作必须 `rollbackFor=Exception.class` 保证回滚；编排（比如发文章要同时写 post、post_tag、topic 计数）需要可回滚的写事务。如果揉在一个类里，注解只能取「最小公约数」，要么查询白付写事务的代价，要么缓存注解挂错位置。详细拆解见 7.4 节。

**为什么前端 API 层统一走 `src/api/`？** 因为 Axios 拦截器承担了鉴权注入、信封解包、401 刷新重放、错误文案转换四件横切事。业务组件只管 `postApi.getDetail(id)`，完全不用知道 token 放哪、信封长什么样、401 之后会发生什么。

**为什么单体而不是前后端分离部署？** 单人博客的流量撑不起两套运维。单体让 SPA 的 SEO HTML（7.10 节）可以直接由后端按请求组装——这是分离部署做不到（或需要额外服务）的事。

## 7.2 环境搭建与项目启动

### 7.2.1 准备 JDK 21 与 Maven Wrapper

后端要求 **JDK 21**（Boot 3.x 强制 Java 17+，本项目用了 21 的语言特性）。本机如果装了多个 JDK，要让 `JAVA_HOME` 指向 21：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
```

本机 `mvn` 不在 PATH 上，但仓库自带 Maven Wrapper（`backend/mvnw` / `backend/mvnw.cmd`，Maven 3.9.12），直接用即可。

### 7.2.2 【必读】跑后端的正确姿势

`backend/pom.xml` 里配置了 `frontend-maven-plugin`，它会在 Maven 的 `initialize` / `generate-resources` 阶段进入 `../frontend` 执行 `npm ci` + `npm run build`——也就是说，**不带开关地跑一次 `mvn test`，会先把整个前端从零构建一遍，还会改写前端构建产物**。所以后端命令必须永远带这两个参数：

```powershell
cd backend
.\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

PowerShell 里 `-D` 参数要加引号，否则会被解析错。带上开关后，588 个后端测试约 1 分钟跑完（H2 内存库，`DB_TYPE` 留空时自动启用）。

只跑一个测试类的某个方法：

```powershell
.\mvnw.cmd test "-Dtest=BlogPostControllerTest#createPost_Success_ReturnsPostId" "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

### 7.2.3 启动后端

先把 `backend/.env.example` 复制为 `backend/.env`，填两个必填项：

```properties
JWT_SECRET=this-is-a-very-long-random-secret-key-at-least-32-chars
ADMIN_PASSWORD=change-me-to-a-strong-password-12plus
```

注意 `.env` 由 `config/DotenvConfig.java` 用 **Java `Properties` 语法**解析，不是 shell dotenv——**值不要加引号**，引号会成为值的一部分。查找顺序是「工作目录 → 用户主目录」，文件里的值会注册为 JVM property，优先级**高于**系统环境变量。

然后启动：

```powershell
.\mvnw.cmd spring-boot:run "-Dskip.npm=true" "-Dskip.installnodenpm=true"
# 或者打包后运行
.\mvnw.cmd clean package "-DskipTests" "-Dskip.npm=true" "-Dskip.installnodenpm=true"
java -jar target\selfblog.jar
```

后端监听 `SERVER_PORT`（默认 8825），健康检查在 `http://localhost:8825/actuator/health`。首次启动会在 `backend/` 下生成 `campus_blog.db`——SQLite 模式下这个文件就是整个数据库。

### 7.2.4 启动前端

依赖安装（Node 引擎要求 `^22.22.2 || ^24.15.0 || >=26.0.0`，`engine-strict` 开启）：

```powershell
cd frontend
npm ci
```

启动开发服务器：

```powershell
.\node_modules\.bin\vite
```

这里有一个本项目特有的坑值得专门讲：**本机直接 `npm run dev` 会全部失败**，报错是 `'"node"' 不是内部或外部命令`。原因不在仓库，而在系统 PATH 里有一条**带不配对引号**的条目（形如 `C:\Program Files\Java\jdk-25\bin"`），`cmd.exe` 解析 PATH 时把它当成未闭合引号的开头，其后所有条目失效。`npm run` 的脚本最终由 `node_modules/.bin/*.cmd` 垫片执行，垫片结尾调用的正是裸 `node`，于是全军覆没；而 `where node` 依然能找到，因为它走的是注册表 App Paths。

两种应对：

1. **根治**：从系统 PATH 里删掉那条多余引号（推荐，一次性的系统修复）。
2. **绕过**：不经过 `npm run`，直接调用 POSIX 垫片——

```powershell
# 等价于 npm run dev / type-check / lint:check / test:run / build
.\node_modules\.bin\vite
.\node_modules\.bin\vue-tsc --noEmit
.\node_modules\.bin\eslint src
.\node_modules\.bin\vitest run
.\node_modules\.bin\vue-tsc --noEmit; .\node_modules\.bin\vite build
```

开发服务器跑在 3000 端口，`vite.config.ts` 把 `/api` 与 `/uploads` 代理到 `VITE_API_TARGET`（默认 `http://localhost:8825`）。一个很贴心的细节：**只跑前端、不跑后端也能看到完整页面**——`main.ts` 里的 `loadPublicSettings()` 对 `/api/site/public` 的 `fetch` 失败是静默吞掉的，`stores/site.ts` 有一套内置默认值（站点标题「个人博客」、皮肤 `ocean`、主色 `#165DFF`……），所以 `vite` 起来就能验收布局与样式，不被「后端没起」阻塞。

### 7.2.5 验证基线

动手改代码前后，都用这组基线对照（2026-10-17 实测）：

- 后端：588 tests / 0 failures（日志里大量 `[PERF] ... error:` 是 `PerformanceMonitoringAspect` 记录的**正常负例断言**，不是失败）
- 前端：43 个测试文件 / 400 tests 全绿，外加 `lint:check`、`format:check`、`type-check`、`test:deployment`（27/27）、`build` 五道闸门

CI 的四个 job 全部真实执行：backend（H2 全套）、frontend（Node 24.15.0 全闸门）、mysql（真实 MySQL 8 从 `schema-fresh-install.sql` 初始化后跑全套）、docker（仓库根上下文构建镜像 → SQLite 起容器 → 等 `/actuator/health` → grep 服务端首页 HTML 含 `seo-reading`）。

## 7.3 数据库设计剖析

### 7.3.1 三种数据库，一个开关

`DB_TYPE` 环境变量决定数据源，由 `config/db/DatabaseConfig.java` 统一创建（该 Bean 带 `@ConditionalOnProperty("DB_TYPE")`）：

| DB_TYPE | 用途 | 行为 |
|---------|------|------|
| `sqlite`（默认） | 开发 / 单机部署 | `jdbc:sqlite:campus_blog.db`，Hikari **池大小 1**（SQLite 单写者），启动自动执行 `schema-sqlite.sql`（`CREATE TABLE IF NOT EXISTS`，不迁移既有文件） |
| （留空） | 测试 | 走 `src/test/resources/application.yml` + H2 内存库 + `schema.sql` |
| `mysql` | 生产可选 | 由 `schema-fresh-install.sql` 初始化，CI 有真实 MySQL 8 的验证 job |

为什么 SQLite 要把 Hikari 池限制为 1？SQLite 的并发写模型是**全库单写者**，连接池开大了反而会收到 `database is locked` 错误。单人博客的写频率极低（作者发文章、访客计数），池 1 完全够用，还天然串行化了写冲突。这是「按数据库特性裁剪连接池」的教科书案例。

### 7.3.2 两张世界观的表：历史社区表与个人博客表

打开 schema 文件，二十多张表分成气质完全不同的两组：

```sql
-- 第一组：历史社区表（v1 遗产，保留但公开入口大多已下线）
sys_user, blog_post, blog_comment, blog_tag, blog_post_tag,
blog_like, blog_collect, blog_share, blog_follow, blog_notification,
blog_trending, blog_draft, blog_draft_tag, blog_report,
blog_circle_post, blog_circle_comment, blog_topic, blog_message, blog_media ...

-- 第二组：个人博客新增表
site_setting, visitor_event, anonymous_post_like,
guest_comment, photo_album, album_photo
```

新功能都属于第二组：匿名点赞、访客留言、相册、访客统计、站点设置。历史表保留的原因见 7.14 节。

### 7.3.3 KV 表 site_setting：零迁移的站点配置

这是本项目数据库设计里最优雅的一个决定。站点设置（标题、作者简介、皮肤、后台路径 `adminPath`、评论开关……）**没有**设计成一张 30 列的宽表，而是一张 KV 表：

```sql
CREATE TABLE IF NOT EXISTS site_setting (
    setting_key   VARCHAR(100) PRIMARY KEY,
    setting_value TEXT
);
```

要新增一个站点设置项，比如 `footerText`，**不需要改任何 schema**——直接插一行 `('footer_text', '...')` 即可。新增键的全部工作在 `SiteSettingsServiceImpl` 里：登记键名、做值校验（比如 `adminPath` 被 `normalizeAdminPath` 约束为 `[A-Za-z0-9][A-Za-z0-9_-]{1,39}` 并拒绝保留词）。这就是「零 schema 迁移」：配置的形状演化被推迟到应用层，数据库只存不解释。

对比宽表方案：每次加一个设置项都要 `ALTER TABLE ADD COLUMN`，而 SQLite/MySQL/H2 三方言的三份 schema 文件都要同步改。KV 表把这个问题一次消灭。代价是类型安全性靠应用层校验兜底——对「值几乎全是字符串」的站点配置来说，这笔交易非常划算。**配置型数据用 KV、业务型数据用关系表**，这个分界线值得记住。

### 7.3.4 软删除：@TableLogic 与手写 SQL 的陷阱

20 多个实体都有这个字段：

```java
@TableLogic
private Integer isDeleted;
```

配合 `application.yml` 的全局配置：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

效果：MyBatis Plus 生成的 `selectById` 自动追加 `AND is_deleted = 0`，`deleteById` 变成 `UPDATE ... SET is_deleted = 1`。**但手写 XML SQL 不会享受这个待遇**——MP 只改写它自己生成的方法。看 `mapper/BlogPostMapper.xml` 里的真实写法：

```xml
<update id="incrementViewCount">
    UPDATE blog_post
    SET view_count = CASE WHEN view_count &lt; 100000000 THEN view_count + 1 ELSE view_count END
    WHERE id = #{postId} AND is_deleted = 0
</update>
```

注意两个细节：`is_deleted = 0` 是**手写**的；`CASE WHEN view_count < 100000000` 是计数上限防护（防溢出、防异常数据刷爆 int）。项目里所有计数 SQL 都带 `col > 0`（decrement 场景还有 `>= #{count}`）的下限防护，防负数。**教训：引入手写 SQL 时，忘了 `is_deleted=0` 是软删除系统里最经典的 bug 来源**——已删除的文章会"复活"出现在搜索结果里。

### 7.3.5 双方言：databaseId="mysql" / "sqlite"

全文搜索是唯一需要方言分叉的地方：

```xml
<select id="fullTextSearch" resultType="com.selfblog.entity.BlogPost">
    SELECT * FROM blog_post
    <where>
        is_deleted = 0 AND status = 1
        <if test="_databaseId == 'mysql'">
            AND MATCH(title, content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)
        </if>
        <if test="_databaseId == 'sqlite'">
            AND (title LIKE '%' || #{keyword} || '%'
                 OR content LIKE '%' || #{keyword} || '%')
        </if>
    </where>
</select>
```

`_databaseId` 由 `MybatisPlusConfig` 注册的 `VendorDatabaseIdProvider` 根据 JDBC URL 自动判定。这里有一个必须理解的规则：**H2 没有映射 id，只会走无 `databaseId` 的兜底语句**——所以不要把 MySQL 专有语法写进共享语句，否则本地测试全绿、一上 MySQL 就炸（或者反过来）。安全区只有两种写法：双方言各写一条（排序变体按 `create_time DESC`/`view_count DESC`/`like_count DESC` 各配一套），或写一条三种库都兼容的通用 SQL。

### 7.3.6 schema-fresh-install.sql 与 migration-selfblog.sql 的区别

这两个文件的存在感很容易混淆，记住一句话：**全新库用前者，已有库只能用后者**。

- `backend/schema-fresh-install.sql`：包含 `DROP TABLE IF EXISTS`，专为**全新空库**设计。它在 2026-10-17 之前叫 `schema.sql`——改名的原因很直接：一个含删表语句的文件叫这么无辜的名字，被误执行的代价太大（Docker Compose 初始化挂载的也是改名后的文件）。改名是**用命名降低误操作概率**的工程手段。
- `backend/migration-selfblog.sql`：增量迁移脚本，只包含 v1 → v2 的 `ALTER`/`INSERT`，对已有数据安全。

CI 的 `mysql` job 验证了前者：从真实 MySQL 8 空库执行 `schema-fresh-install.sql`，然后跑完整测试套件（`DB_TYPE=mysql`、`SPRING_SQL_INIT_MODE=never`）——这是仓库里唯一真实 MySQL 验证途径（开发机没有 MySQL 和 Docker 守护进程）。

## 7.4 后端一条完整链路：发布文章

现在进入本章的核心。我们跟着「作者发布一篇文章」这个请求，从 HTTP 进入数据库，把每一层代码读完。你会发现「发布文章」远不止 `INSERT` 一条记录。

### 7.4.1 Controller：薄到只剩三件事

```java
// backend/src/main/java/com/selfblog/controller/post/BlogPostController.java
@RestController
@RequestMapping("/post")        // 实际对外是 /api/post（WebMvcConfig 加前缀）
@RequiredArgsConstructor
public class BlogPostController {

    private final BlogPostService blogPostService;

    @RequiresAuth
    @PostMapping
    public Result<Long> createPost(@Valid @RequestBody PostCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        Long postId = blogPostService.createPost(request, userId, isAdmin);
        return Result.success(postId);
    }
}
```

三件事：`@Valid` 触发请求体校验（标题/内容非空、长度限制写在 DTO 上）；`SecurityUtils` 取身份（未登录直接抛 401 的 `BusinessException`）；调用门面并包信封。注意 `createPost` 有 `(request, userId)` 与 `(request, userId, isAdmin)` 两个重载——`isAdmin` 是给「管理员代操作」场景用的，门面里的归属校验会放行管理员。

还有一条容易忽略的规则：控制器源码里**不写 `/api`**。前缀由 `config/web/WebMvcConfig` 统一注入：

```java
// WebMvcConfig.java
@Override
public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        apiPrefix,
        HandlerTypePredicate.forAnnotation(RestController.class));
}
```

只有 `@RestController` 被加前缀，`@Controller`（SpaController、SeoController）不受影响——这是 7.10 节 SEO 能在根路径提供 `/feed.xml` 的前提。测试时也要记住：`@WebMvcTest` 里用 `post("/post")` 而不是 `post("/api/post")`。

### 7.4.2 门面：createPost 的完整编排

```java
// service/post/impl/BlogPostServiceImpl.java
@CacheEvict(cacheNames = {CacheNames.TRENDING_CACHE, CacheNames.STATS_CACHE},
            allEntries = true)
@Transactional(rollbackFor = Exception.class)
public Long createPost(PostCreateRequest request, Long userId, boolean isAdmin) {
    validatePostRequest(request);   // 标题/内容非空、标题≤200 字符、内容≤30 万字符

    BlogPost post = new BlogPost();
    // XSS 防护：富文本字段过白名单清洗，Markdown 走 sanitizeMarkdown
    post.setTitle(htmlSanitizer.sanitizeRichText(request.getTitle()));
    post.setSummary(htmlSanitizer.sanitizeRichText(request.getSummary()));
    post.setContent(htmlSanitizer.sanitizeMarkdown(request.getContent()));
    post.setUserId(userId);
    post.setStatus(PostStatus.PUBLISHED.getValue());   // 发布即上线，没有审核闸门
    post.setViewCount(0);
    post.setLikeCount(0);
    post.setCommentCount(0);
    post.setCollectCount(0);

    List<Long> tagIds = resolveTagIds(request.getTagIds());  // 标签名不存在则创建
    validateTagIds(tagIds);
    post.setTopicIds(request.getTopicIds() != null
            ? JSONUtil.toJsonStr(request.getTopicIds()) : null);

    this.save(post);          // INSERT blog_post
    savePostTags(post.getId(), tagIds);                    // 批量 INSERT blog_post_tag
    topicMapper.incrementPostCount(request.getTopicIds()); // 话题文章数 +1（跨表）
    return post.getId();
}
```

逐个拆解这段代码里的设计：

**① 发布即上线。** `setStatus(PostStatus.PUBLISHED.getValue())`——创建就是 `status=1`，不存在「保存 → 送审 → 过审」的流转。这是单人博客的产品决策：作者是唯一把关人，多一道审核闸门纯属自我折磨。历史上的 `approvePost` 只对 v1 遗留的 `status=0` 数据生效（见 7.14）。

**② 入口清洗。** 标题和摘要按富文本白名单清洗（`sanitizeRichText` 只保留安全标签），Markdown 正文走 `sanitizeMarkdown`。**入库前清洗**意味着无论前端 DOMPurify 做没做（7.9 节），数据库里的东西一定是干净的——纵深防御的第二层。

**③ 缓存清除的范围是取舍出来的。** `@CacheEvict` 清掉 trending/stats（更新文章还要多清 category）：新文章会让「热门榜单」「站点统计」过期，而这两个缓存的 TTL 分别是 5 分钟和 15 分钟——不清的话作者发完文章要等十几分钟才能在统计里看到。为什么不全清？因为每次 `@CacheEvict` 都是一次「缓存雪崩」风险（下一次读请求全部打回数据库），只清「误差肉眼可见」的缓存。

**④ `rollbackFor = Exception.class` 为什么必须写？** Spring 默认只对 `RuntimeException` 和 `Error` 回滚。项目统一用 `BusinessException extends RuntimeException` 所以默认也够，但 MyBatis 的受检异常、第三方库抛的 checked exception 不会触发回滚。写事务一律显式 `rollbackFor` 是本项目强制规范——「看起来多余」的注解买的是一份确定性。

**⑤ 归属校验放在门面。** `updatePost`/`deletePost` 都先查文章再校验 `!Objects.equals(userId, post.getUserId()) && !isAdmin → 403`——这条规则写一次、三个入口（含 admin 侧）复用，而不是散在各 Controller 里。

### 7.4.3 三分结构：门面、查询、交互各管什么

门面实现里能看到明显的分工痕迹——查询方法被**一行委托**出去：

```java
// service/post/impl/BlogPostServiceImpl.java（节选）
@Override
@Transactional(readOnly = true)
public PostDetailResponse getPostDetail(Long id) {
    return postQueryService.getPostDetail(id);   // 一行委托，重新标注只读事务
}
```

三个实现类各司其职：

| 角色 | 类 | 事务注解 | 典型方法 |
|------|-----|---------|---------|
| 门面 Facade | `BlogPostServiceImpl` | 写：`@Transactional(rollbackFor)` | createPost / updatePost / deletePost（编排多表写） |
| 查询 Query | `PostQueryServiceImpl` | `@Transactional(readOnly = true)` + `@Cacheable` | getPostDetail / getPostList / advancedSearch |
| 交互 Interaction | `PostInteractionServiceImpl` | `@Transactional(rollbackFor)` | incrementViewCount / likeCount 等 |

Circle（校友圈）采用同一套拆分（`CircleService` / `CircleQueryService` / `CircleInteractionServiceImpl`），**新增功能要放对位置，不要在门面里复制查询逻辑**。注意门面实现与查询实现**都**继承同一个实体的 `ServiceImpl<XxxMapper, Entity>`——这是 MP 的用法特征。

**查询服务的代表——getPostDetail：**

```java
// service/post/impl/PostQueryServiceImpl.java
@Transactional(readOnly = true)
public PostDetailResponse getPostDetail(Long id) {
    BlogPost post = this.getById(id);
    if (post == null || post.getIsDeleted() == 1) {
        throw new BusinessException(404, "文章不存在");
    }
    Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();  // 游客不抛异常
    boolean isAuthor = currentUserId != null && currentUserId.equals(post.getUserId());
    if (post.getStatus() != PostStatus.PUBLISHED.getValue() && !isAuthor) {
        throw new BusinessException(404, "文章不存在");  // 未发布对非作者伪装成 404
    }
    return assemblePostDetail(post);   // 拼 username/nickname/avatar、tags、topicName
}
```

三个细节都值得学：`getCurrentUserIdOrNull()` 是游客场景专用——详情页要判断「当前人是不是作者」，但游客没有 id，`getCurrentUserId()` 会直接抛 401；**未发布文章对非作者返回 404 而不是 403**，避免向访客泄露「存在一篇隐藏文章」这个事实（信息泄露的经典反例是返回 403 帮攻击者确认资源存在）；`readOnly=true` 不只是文档注释，JDBC 驱动和数据库可以据此做优化（MySQL InnoDB 对只读事务省掉事务 ID 分配）。`PostQueryServiceImpl` 还用 `@Value("${DB_TYPE:mysql}")` 感知方言，并提供了 `getPublishedPostDetail` / `getPublishedMetadata` 这类只取元数据列的轻量查询给 SEO 使用。

**交互服务的代表——incrementViewCount 的防刷设计：**

```java
// service/post/impl/PostInteractionServiceImpl.java
private final ConcurrentHashMap<String, AtomicLong> viewCountCache = new ConcurrentHashMap<>();
private static final long VIEW_COUNT_INTERVAL_MS = 60_000;   // 同 key 60 秒内不重复计数
private static final int MAX_VIEW_COUNT_CACHE_SIZE = 10_000;

@Override
@Transactional(rollbackFor = Exception.class)
public void incrementViewCount(Long postId, String userKey) {
    long now = System.currentTimeMillis();
    if (viewCountCache.size() >= MAX_VIEW_COUNT_CACHE_SIZE) {
        cleanupViewCountCache();   // removeIf 清掉过期项
    }
    String cacheKey = userKey + "-" + postId;
    AtomicLong slot = viewCountCache.computeIfAbsent(cacheKey,
            k -> new AtomicLong(0));
    long lastTime;
    do {
        lastTime = slot.get();
        if (now - lastTime < VIEW_COUNT_INTERVAL_MS) {
            return;                        // 窗口内，静默忽略
        }
    } while (!slot.compareAndSet(lastTime, now));  // CAS 失败 = 有并发竞争，重试
    blogPostMapper.incrementViewCount(postId);     // 只有赢家走到这里
}
```

userKey 在 Controller 层生成：登录用户是 `"user-" + userId`，游客是 `"guest-" + ip + "-" + userAgent.hashCode()`（IP 可伪造，但结合 UA 哈希提高伪造成本）。CAS 自旋保证了并发下同一窗口只有一个请求真正写库——比 `synchronized` 轻量，比数据库唯一约束简单。注意 `MAX_VIEW_COUNT_CACHE_SIZE` 的内存上限意识：无界缓存内存泄漏是这类计数器最常见的死法。Controller 里的调用也讲究「失败不碍事」：

```java
@GetMapping("/{id}")
public Result<PostDetailResponse> getPostDetail(@PathVariable Long id, HttpServletRequest request) {
    String userKey = getUserIdentifier(request);
    try {
        blogPostService.incrementViewCount(id, userKey);
    } catch (Exception e) {
        log.warn("Failed to increment view count for post {}", id, e);  // 计数失败不影响阅读
    }
    return Result.success(blogPostService.getPostDetail(id));
}
```

### 7.4.4 请求完成后的连锁反应

一次 `POST /api/post` 成功后，系统里发生了这些事：`blog_post` 多一行、`blog_post_tag` 多 N 行、`blog_topic.post_count` 加 1、trending/stats/category 三个缓存被清空、`site_setting` 没动。下次有人访问首页，`PostQueryService` 会以缓存未命中为代价重新计算热点列表——这就是写扩散到读的完整闭环。相关的草稿接口（`/post/draft` 系列：`saveDraft`/`getLatestDraft`/`getMyDrafts`）走的是 `blog_draft` 表，与发布链路独立，作者可以先存草稿再正式发布。

## 7.5 统一响应与全局异常处理

### 7.5.1 Result 信封

所有 REST 接口返回同一个信封：

```java
// common/result/Result.java
public class Result<T> {
    private Integer code;      // 200 成功 / 401 未登录 / 403 无权限 / 500 失败
    private String message;
    private T data;
    private Long timestamp;    // 方便前端排查时序问题

    private Result() {}        // 私有构造器，只能走静态工厂

    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> success(String message, T data) { ... }
    public static <T> Result<T> error(Integer code, String message) { ... }
}
```

私有构造器 + 静态工厂强制所有响应走同一条构造路径，`timestamp` 是排查「这个响应是什么时候产生的」的免费福利。分页 data 里统一是 `records/total/size/current/pages` 五件套（`IPage` 结构）。

### 7.5.2 BusinessException：带语义的错误

```java
public class BusinessException extends RuntimeException {
    @Getter
    private final Integer code;

    public BusinessException(String message) { this(BaseErrorCode.FAILURE, message); }
    public BusinessException(Integer code, String message) { super(message); this.code = code; }
    public BusinessException(BaseErrorCode errorCode) { ... }
}
```

业务代码里随处 `throw new BusinessException(403, "此博客不开放访客注册")`，HTTP 语义和业务语义的翻译交给统一的处理器。

### 7.5.3 GlobalExceptionHandler：一张翻译表

```java
// common/exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        HttpStatus status = switch (e.getCode()) {
            case 400, 401, 403, 404, 409, 429, 500 -> HttpStatus.valueOf(e.getCode());
            default -> HttpStatus.BAD_REQUEST;
        };
        LogUtils.logSecurityEvent(...);
        return ResponseEntity.status(status).body(Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)   // @Valid 失败
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(FieldError::getDefaultMessage).orElse("参数校验失败");
        return ResponseEntity.badRequest().body(Result.error(400, msg));
    }
    // 还有：ConstraintViolation 400 / HttpMessageNotReadable 400「请求体解析失败/参数类型不匹配」
    //       MissingServletRequestParameter 400 / NoHandlerFound 404
    //       HttpRequestMethodNotSupported 405
    //       DataIntegrityViolation + DuplicateKey 409「数据已存在，操作冲突」
    //       AccessDeniedException 403 / AuthenticationException 401
    //       MaxUploadSizeExceeded 400 / MethodArgumentTypeMismatch 400（带参数名与期望类型）
    //       IOException：ClientAbortException 静默（客户端提前断开不是错误），否则手写 500 JSON
    //       Exception 兜底 500「服务器内部错误」
}
```

设计思想有三层。**第一层：业务 code 与 HTTP status 双轨**。信封里的 code 是给前端程序判断的（`res.code === 403`），HTTP status 是给浏览器、网关、监控看的（nginx 日志、告警系统只认后者）。`switch` 只映射白名单状态码，未知 code 一律 400——不把内部语义泄露成奇怪的 HTTP 状态。**第二层：异常翻译集中化**。`DuplicateKeyException` → 409「数据已存在」这种翻译只写一次，全项目受益；对比在每个 catch 里各自翻译的错误写法。**第三层：安全日志联动**。401/403 类异常触发 `LogUtils.logSecurityEvent`，登录爆破、越权尝试会留下审计痕迹。

一个隐藏细节：`SecurityConfig` 把 `/auth/register/*` 与 `/user/register` 设为 `permitAll`，正是为了让注册关闭的 403 **业务响应**能走到这个处理器——如果不放行，请求会先被 401 entry point 拦截，访客看到的就是「请先登录」而不是「不开放注册」，语义完全错位。

## 7.6 认证授权全链路

这条链路跨前后端六个环节，是全项目最长的「一次握手」。

### 7.6.1 环节一：登录与 JWT 生成

`POST /api/user/login` 进来后，`SysUserServiceImpl.login()` 走完一套完整的防爆破流程：

```java
// service/auth/impl/SysUserServiceImpl.java（逻辑串写）
public UserLoginResponse login(UserLoginRequest request) {
    SysUser user = (含 @ ? 按邮箱 : 按用户名) 查用户;
    if (user == null) throw new BusinessException(401, "用户名/邮箱或密码错误");
    if (user.getLockUntil() 在未来) throw new BusinessException(403, "登录失败次数过多，请 N 分钟后再试");
    if (密码不匹配) { baseMapper.incrementLoginFailCount(userId, maxFailCount, lockMinutes);  // 原子 SQL
                     throw new BusinessException(401, ...); }
    if (!"admin".equals(user.getRole()))
        throw new BusinessException(403, "此博客仅开放作者后台登录");   // ★ 单作者关键行
    String credentialVersion = jwtUtils.credentialVersionFor(user);
    String token   = jwtUtils.generateToken(user.getId(), username, role, credentialVersion);
    String refresh = jwtUtils.generateRefreshToken(...);
    jwtUtils.registerDeviceToken(userId, token);           // 每用户最多 10 个设备
    return new UserLoginResponse(id, username, ..., token, refresh, role);
}
```

★ 那一行是单作者模式的门闩：即使密码对了，非 admin 角色也进不来。错误信息故意区分「密码错」（401，可重试）与「次数过多/账号禁用」（403，要等待），给用户正确的预期。失败计数用**原子 SQL**（`incrementLoginFailCount` 带锁定条件，affected==0 说明已被锁）而不是「查出来加一再写回」——后者在并发下会漏计。注册侧则全部关闭：`SysUserController.register()` 与 `AuthController` 的两个验证码端点都直接 `throw new BusinessException(403, "此博客不开放访客注册")`，连验证码邮件的发送路径都不留。

`JwtUtils.generateToken` 用的是 jjwt 0.12.6 的新版流式 API（网上老教程的 `setSubject()` 已废弃）：

```java
private String createToken(Long userId, String username, String role,
                           String credentialVersion, long expiration) {
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("userId", userId)
            .claim("username", username)
            .claim("role", role)
            .claim("credentialVersion", credentialVersion)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact();
}
```

三个值得专门讲的设计：

**credentialVersion：改密即下线。** 它是对 `userId + passwordHash` 做域分离 HMAC 的结果（`"selfblog:credential-version:v1\n" + id + "\n" + passwordHash`，以 JWT secret 为 key，HmacSHA256，Base64url 无填充）。密码一改，passwordHash 变，旧 token 的 credentialVersion 立刻对不上数据库现值——**不用黑名单、跨重启有效**地吊销了所有旧会话。这是「把撤销信息编码进 token 本身」的妙招，代价是每次校验要查一次用户表（可接受，用户表已有 `userCache` 缓存）。撤销判定以**数据库里的密码快照**为界：黑名单重启即失效，而 credentialVersion 永远有效。

**设备管理。** `userDeviceTokens` 每个 userId 最多挂 `MAX_DEVICES_PER_USER = 10` 个 token，超出挤掉最旧的（进黑名单）；token 黑名单 `tokenBlacklist` 上限 100_000 条、超限先清过期项。「登录设备管理」「退出其他设备」这两个功能在数据结构上就是每用户一个有界 Set。

**refresh token 隔离。** refresh token 多一个 `type="refresh"` claim 和独立 `jti`（UUID），轮换接口 `rotateRefreshToken` 验证 type 后**先撤销旧 token 再发新的**（保留旧 credentialVersion，防止并发改密把旧会话升级）——检测到旧 refresh 被复用即可判定泄露。解析异常的处理也有讲究：`ExpiredJwtException` → 过期，其他 `JwtException` → 无效（无效 ≠ 过期，两者走不同的客户端流程）。

### 7.6.2 环节二：前端存储（utils/crypto 的编解码）

登录成功后，`stores/user.ts` 把 token 存进 localStorage（勾选「记住我」）或 sessionStorage，但**存之前先编码**：

```ts
// frontend/src/utils/crypto.ts
export function encodeData(data: unknown): string | null {
  try {
    return btoa(unescape(encodeURIComponent(JSON.stringify(data))));
  } catch { return null; }
}
export function decodeData<T>(encoded: string): T | null {
  try {
    return JSON.parse(decodeURIComponent(escape(atob(encoded))));
  } catch { return null; }
}
```

先说清楚：**这不是加密**，btoa 三秒就能反解。它的真实作用是「防直读混淆」——打开 DevTools 一眼扫过去，storage 里躺着的不是明文 `eyJhbGci...`，防的是同事随手复制、浏览器扩展粗暴正则扫描这类低成本泄露。真正的安全边界由 JWT 的服务端签名保证。中间的 `unescape/encodeURIComponent` 组合是为了让 btoa 能处理中文（btoa 只接受 Latin1）。

`stores/user.ts`（Pinia store id 是 `'auth'`，导出别名 `useUserStore`）还做了三件事：`saveToStorage` 按 remember 标志选端并**清空另一端残留**（防止切换记住我时双端各存一份）；`validateToken` 只在本地解 JWT 的 payload 判断 `exp`（`token.split('.')` 三段 → `atob(payload)` → `JSON.parse` → `exp*1000 < Date.now()` 判定，**不发网络请求**，5 分钟内要过期就打日志等下次请求自动刷新）；`detectTokenTheft` 对比内存与 storage 的 token 指纹（`getTokenFingerprint` = 首 20 + 后 20 字符的 btoa），不一致就在请求拦截器里抛 `Token security violation` 并清理状态——token 被复制到另一个 storage 的场景会被察觉。

### 7.6.3 环节三：请求拦截器注入

```ts
// frontend/src/api/index.ts（节选）
const api = axios.create({ baseURL: '/api', timeout: 15000 });

api.interceptors.request.use((config) => {
  const token = getToken();                    // useUserStore 优先，回退 storage 解码
  if (token) config.headers.Authorization = `Bearer ${token}`;
  config.headers['X-XSRF-TOKEN'] = getXsrfToken() || generateXsrfToken();  // 32 字节 hex
  config.headers['X-Visitor-Id'] = getVisitorId();   // 匿名访客身份（7.9 节）
  config.headers['X-Trace-Id']  = generateTraceId(); // crypto.randomUUID，全链路日志串联
  return config;
});
```

每个请求带四件行李。注意 `baseURL` 是**硬编码的 `'/api'`**——代码里没有任何地方读 `VITE_API_BASE_URL`，那个环境变量只是保留位。同源路径的巧妙之处：开发期靠 Vite 代理、生产期靠 JAR 同源自带或 Vercel 反代重写，**前端代码零改动**。

### 7.6.4 环节四：后端过滤器解析

```java
// config/security/JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ... {
        LogUtils.initMdc();   // X-Trace-Id 进 MDC，日志全链路串联
        String token = jwtUtils.extractTokenFromRequest(request);
        if (token != null) {
            if (jwtUtils.isRefreshToken(token)
                    && !request.getRequestURI().endsWith("/user/refresh")) {
                // ★ refresh token 不能访问普通 API：泄露后无法长期滥用
                response.setStatus(401);
                response.getWriter().write(
                    "{\"code\":401,\"message\":\"Refresh token cannot be used for API access\"}");
                LogUtils.logSecurityEvent("REFUSED_REFRESH_TOKEN", ...);
                return;
            }
            if (!jwtUtils.isTokenExpired(token) && !jwtUtils.isTokenRevoked(token)) {
                Long userId = jwtUtils.getUserIdFromToken(token);
                String role = jwtUtils.getRoleFromToken(token);
                UserContext userContext = new UserContext(userId, role);
                var authorities = List.of(new SimpleGrantedAuthority(
                        role != null ? "ROLE_" + role : "ROLE_USER"));
                var authentication = new UsernamePasswordAuthenticationToken(
                        userContext, null, authorities);
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // 解析失败只 log.warn("JWT Authentication failed")，继续放行 → 当匿名请求处理
        }
        try { chain.doFilter(request, response); }
        finally { LogUtils.clearMdc(); }
    }
}
```

三个关键决策：**refresh token 在非刷新端点一律 401**——refresh token 有效期 7 天，一旦泄露危害远大于 access token，从过滤器层直接拒绝可以把它锁死在唯一合法用途上；**解析失败不当错误处理**，而是当匿名继续（公开接口对游客开放，token 坏了不该连累阅读）；**principal 是 `UserContext` 对象**而不是裸 userId（兼容旧版 Long principal），下游 `SecurityUtils` 才能同时拿到 id 和 role。

### 7.6.5 环节五：SecurityUtils 与环节六：@PreAuthorize

```java
// utils/SecurityUtils.java
public static Long getCurrentUserId() {
    var ctx = getCurrentUserContext();          // 匿名 → 抛 401「请先登录」
    return ctx.getUserId();
}
public static Long getCurrentUserIdOrNull() {  // 游客场景：详情页判断是否作者本人
    var ctx = getCurrentUserContextOrNull();
    return ctx != null ? ctx.getUserId() : null;
}
public static boolean isCurrentUserAdmin() { ... }
```

而方法级鉴权由 `@EnableMethodSecurity` 打开：

```java
// /api/admin/** 的控制器上
@PreAuthorize("hasRole('admin')")
public Result<...> someAdminApi(...) { ... }
```

### 7.6.6 兜底：single-author-mode

链条最后一环是安全规则的「断言」，写在 `SecurityConfig` 的授权规则最末尾：

```java
// config/security/SecurityConfig.java（节选）
@Value("${app.single-author-mode:true}") boolean singleAuthorMode;

http.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/error").permitAll()
        .requestMatchers("/auth/register/**", "/user/register").permitAll() // 让业务403可见
        .requestMatchers("/user/login", "/user/refresh", "/user/password/**").permitAll()
        .requestMatchers("/uploads/**", "/actuator/health").permitAll()
        .requestMatchers("/api/admin/**").hasRole("admin")
        .requestMatchers(HttpMethod.GET, "/site/public", "/site/pages/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/visitor/track").permitAll()
        .requestMatchers(HttpMethod.GET, "/post/{id}", "/post/{id}/adjacent",
                "/post/list", "/post/search/**").permitAll()
        .requestMatchers(HttpMethod.PUT, "/post/{id}/view").permitAll()
        // ... tag/**、media/post/**、statistics/community、trending/**、
        //     anonymous-likes/**、guest-comments/**、gallery/** 等
        .anyRequest().access((authentication, ctx) -> {
            if (singleAuthorMode) {
                // ★ 兜底：没被上面白名单放行的任何请求，都必须是管理员
                return new AuthorizationDecision(hasRoleAdmin(authentication));
            }
            return new AuthorizationDecision(isAuthenticated(authentication));
        }))
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

这行 `anyRequest().access(...)` 是**白名单模型的安全底**：`@PreAuthorize` 忘写、新接口忘了配规则，兜底规则默认拒绝。单作者模式下「未明确允许即仅管理员」——这比默认放行 + 逐个收紧的模型安全得多，新增接口时思考成本也最低（默认安全）。同文件里还有 CSRF 的关键注释值得抄进脑海：JWT 走 `Authorization` 头而非 Cookie，浏览器同源策略阻止跨域读取响应，攻击者无法强制浏览器携带该头，**CSRF 攻击面不存在**，所以 `csrf.disable()` 是成立的安全决策；将来若改用 Cookie 认证必须重新开启 CSRF。同一段配置里还有 CORS（拒绝通配符 origin、`allowCredentials(true)`、允许头含 `Authorization/X-XSRF-TOKEN/X-Visitor-Id/X-Trace-Id`）、响应头加固（`frameOptions.deny`、`ReferrerPolicy`、`permissionsPolicy` 全禁）、`SessionCreationPolicy.STATELESS`、以及自写的 401/403 JSON entry point/accessDeniedHandler——安全配置的每一块都值得逐行读一遍。

## 7.7 前端一条完整链路：访客阅读文章

### 7.7.1 从 URL 到组件

访客访问 `/post/42`，Vue Router 匹配 `router/modules/` 里注册的路由，`views/post/PostDetail.vue` 开始挂载。组件做的第一件事不是发请求，而是**乐观渲染骨架**：标题、时间、阅读时长先占位，避免整页白屏。

```vue
<!-- views/post/PostDetail.vue（示意） -->
<script setup lang="ts">
const route = useRoute();
const post = ref<PostDetailResponse | null>(null);
const error = ref('');

onMounted(async () => {
  try {
    const res = await postApi.getPostDetail(Number(route.params.id));
    // ★ 契约：res 就是后端信封 { code, message, data }
    if (res.code === 200) post.value = res.data;
  } catch (e) { error.value = '文章不存在或已删除'; }
  nextTick(() => useMarkdownEnhance(container));  // 代码复制/灯箱/锚点
});
</script>
```

`useMarkdownEnhance` 给正文做客户端增强：代码块复制与折叠、标题锚点、图片灯箱。注意这些是**增强**——正文本身在服务端 HTML（7.10 节）里就是可读的，JS 挂载后 Vue 会整体替换 `#app` 内的 `.seo-reading` 内容。阅读页还有目录侧栏与移动端抽屉（共用 1020px 断点）、`useSeoMeta` 设置 canonical（优先读 `meta[name=selfblog-site-origin]`，本地开发无标记时回落浏览器 origin）。

### 7.7.2 拦截器里的契约陷阱

`api/index.ts` 的响应拦截器是本项目最容易踩的坑：

```ts
api.interceptors.response.use(
  (response) => response.data,   // ★ 返回后端信封本身，不是 AxiosResponse
  (error) => { ... }
);
```

所以调用方写 `res.data`（信封里的业务数据）而不是 `res.data.data`（不存在的两层）。这个设计牺牲了一点点直觉，换来的是业务代码里少一层解包。同文件的错误分支还承担了完整的 401 处理：

```ts
if (error.response?.status === 401 && !error.config._retry) {
  if (error.config.url === '/user/refresh') {
    return handleAuthError();        // 刷新接口本身 401 → 直接登出，防死循环
  }
  if (isRefreshing) {
    return new Promise((resolve) => failedQueue.push({ resolve, config }));  // 排队
  }
  isRefreshing = true;
  try {
    const { data } = await api.post('/user/refresh', null,
        { headers: { Authorization: `Bearer ${refreshToken}` } });
    // 更新 storage（encodeData 编码）+ userStore.updateTokens + 更新指纹
    processQueue(null, data.data.token);   // 队列里的请求换新 token 重放
    error.config.headers.Authorization = `Bearer ${data.data.token}`;
    return api(error.config);
  } catch (e) {
    processQueue(e); return handleAuthError();
  } finally { isRefreshing = false; }
}
```

`isRefreshing + failedQueue` 的排队重放是并发 401 的标准解法：页面同时发 5 个请求都 401，只发一次刷新，5 个请求统一重放。`handleAuthError` 清理 Pinia/localStorage/指纹/XSRF（含 `clearSecurityStorage`），然后跳 `siteStore.adminRoot`（动态后台路径，见下节）。网络类失败另有 `_networkRetryCount` 限次指数延迟重试（`ERR_CANCELED` 不重试），400/403/404/429/500 分别设置 `error.userMessage` 供组件直接展示。

### 7.7.3 启动顺序为什么是承重的

`src/main.ts` 的 57 行代码里藏着三条时序约束：

```ts
// frontend/src/main.ts（顺序即逻辑）
const app = createApp(App);
app.use(createPinia());
// ... 指令、错误处理

await siteStore.loadPublicSettings();      // ① 先知道 adminPath
installAdminRoutes(siteStore.adminPath);   // ② 再注册后台路由
useUserStore(pinia).restoreFromStorage();  // ③ 再恢复登录态
app.use(router);                           // ④ 最后装路由（首次导航此刻发生）
app.mount('#app');
void usePagesStore(pinia).loadManifest();  // ⑤ 可选项，不阻塞启动
```

①→②：后台路径 `adminPath` 是**站点设置**（服务端下发，默认 `admin`），不知道它就注册不了 `/admin/**` 路由，作者输 `/admin` 会 404。③→④ 是最重要的一条：登录态必须在 `use(router)` 之前恢复，否则路由守卫的第一个 `beforeEach` 看到未登录，会把已登录的作者弹回登录页——「刷新一下就被登出」这类诡异 bug 的根源几乎都在这。⑤ 的 `loadManifest` 失败静默：页面配置是增强项，不能阻塞首屏。启动失败也有兜底：整个 bootstrap 包在 try/catch 里，catch 之后**仍然执行 `app.mount('#app')`**，宁可白壳也不给用户留死屏。

### 7.7.4 作者后台链路与动态注册的路由

后台路由不是写死的静态路由，而是一个工厂函数：

```ts
// frontend/src/router/modules/admin.ts
export function createAdminRoutes(adminPath: string): RouteRecordRaw[] {
  const root = `/${adminPath}`;
  const adminMeta = { requiresAuth: true, requiresAdmin: true, adminArea: true };
  return [{
    path: root, name: 'AdminEntry',
    component: () => import('@/views/admin/AdminEntry.vue'),
    children: [
      { path: '',         name: 'AdminDashboard', component: ..., meta: adminMeta },
      { path: 'posts',    name: 'AdminPosts',     component: ..., meta: adminMeta },
      { path: 'write',    name: 'AdminWrite',     component: PostEdit, meta: adminMeta },
      { path: 'edit/:id', ... },
      { path: 'tags', 'visitors', 'comments', 'albums', 'pages', 'pages/:id', 'settings', ... },
    ],
  }];
}
```

`installAdminRoutes` 把它们 `router.addRoute()` 进去，还有一个命名空间保护：如果站点设置的 `adminPath` 恰好是 `pages`，就先注册公开的 `/pages/:slug` 路由占住位置（防后台路径抢占公开命名空间）。路由守卫 `guards.ts` 统一处理 `requiresAuth`/`requiresAdmin`：未登录跳 `adminRoot` 带 `redirect` 查询参数；登录但 token 本地校验失败 → toast「登录已过期」并回登录页；非管理员 → toast「无管理员权限」并回首页；`meta.guest` 的页面对已登录用户直接跳首页。`onError` 单独处理 `ChunkLoadError`（发版后旧 chunk 404 的经典场景）。`views/auth/` 下只剩一条重定向路由：`/login` → `adminRoot`；组件里需要登录时统一 `router.push('/login')`，不要写死后台路径。

后台能成立的安全保障全在后端：`adminPath` 只是**前端的门牌号**（藏在站点设置里，`/robots.txt` 甚至会 `Disallow` 它），后端安全永远只看 `/api/admin/**`——门牌号换了名字，安全的墙一寸都没动。后台的编辑器还有一条纪律值得知道：保存状态依据**成功提交时的完整内容快照**判定，摘要/标签也计入未保存状态，旧请求不得把请求期间的新改动标成已保存；页面编辑/导航写入必须携带 `revision` 乐观锁。

## 7.8 缓存设计与一致性

### 7.8.1 八个具名缓存，八个 TTL

```java
// config/cache/CaffeineCacheConfig.java
@Configuration
@EnableCaching
public class CaffeineCacheConfig {
    public static final String HOT_TAGS_CACHE   = "hotTagsCache";
    public static final String CATEGORY_CACHE   = "categoryCache";
    public static final String USER_CACHE       = "userCache";
    public static final String TRENDING_CACHE   = "trendingCache";
    public static final String STATS_CACHE      = "statsCache";
    public static final String STATUS_CACHE     = "statusCache";
    public static final String SITE_SETTINGS_CACHE = "siteSettingsCache";
    public static final String VISITOR_SUMMARY_CACHE = "visitorSummaryCache";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
            Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(10, MINUTES)
                    .recordStats().build(),
            // ... categoryCache 500 条/30m，userCache 5000 条/10m，
            //     trendingCache 1000 条/5m，statsCache 200 条/15m，
            //     statusCache 10000 条/2m，siteSettingsCache 20 条/10m（无 recordStats），
            //     visitorSummaryCache 10 条/30s
        ));
        return manager;
    }
}
```

TTL 不是随手写的，每个都对应一种「数据能容忍多久过期」的业务判断：

| 缓存 | TTL | 容量 | 为什么是这个数 |
|------|-----|------|----------------|
| `hotTagsCache` | 10m | 1000 | 标签热度按天级变化，10 分钟延迟无感 |
| `categoryCache` | 30m | 500 | 分类几乎不变，最长 TTL 给改动最少的数据；写路径会主动 evict |
| `userCache` | 10m | 5000 | 用户资料被拼进文章详情，读极频繁；但 credentialVersion 校验依赖它，不能太长 |
| `trendingCache` | 5m | 1000 | 热门榜单要「看起来是活的」，5 分钟是新鲜感的下限 |
| `statsCache` | 15m | 200 | 站点统计，误差一刻钟完全可接受 |
| `statusCache` | 2m | 10000 | 文章状态判断，访问量最大的键空间，短 TTL + 大容量 |
| `siteSettingsCache` | 10m | 20 | 键空间极小（KV 表就几十行），但每个请求都可能读 |
| `visitorSummaryCache` | 30s | 10 | 访客大盘，30 秒聚合一次，保住数据库不被实时聚合打爆 |

两个反直觉的细节：容量最大的是 `statusCache`（10000 条）——它的键是「每个文章 id 的状态」，键空间天然大；`siteSettingsCache` 没有 `recordStats()`，因为它太小了，命中率统计没有诊断价值。测试 profile 直接关闭缓存，让单测不受缓存干扰——**缓存是性能优化，不是正确性依赖**。

### 7.8.2 @CacheEvict 的取舍

写路径清缓存的完整决策表：

```java
@CacheEvict(cacheNames = {TRENDING_CACHE, STATS_CACHE}, allEntries = true)   // createPost
@CacheEvict(cacheNames = {TRENDING_CACHE, STATS_CACHE, CATEGORY_CACHE},
            allEntries = true)                                               // updatePost
@CacheEvict(cacheNames = {TRENDING_CACHE, STATS_CACHE}, allEntries = true)   // deletePost
```

三个取舍原则浮出水面。**一是 `allEntries = true` 而不是按 key 清**：热门榜、统计这类聚合缓存无法从单个文章 id 推导受影响的 key，全清比猜便宜——代价是下一次读请求缓存未命中，对单人博客流量完全无感。**二是只清「读者能看见陈旧值」的缓存**：`userCache` 里作者昵称改了 10 分钟才生效，肉眼几乎不可察觉，不值得为它加 evict 链；`visitorSummaryCache` 30 秒自然过期，根本不需要 evict。**三是更新/删除的清空范围随受影响面走**：更新会改分类归属所以多清 `categoryCache`，创建/删除不动分类就不用——evict 链跟着数据流走，而不是「清得越多越安心」。

## 7.9 限流与安全

### 7.9.1 RateLimitInterceptor：固定窗口计数器

```java
// config/web/RateLimitInterceptor.java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    // 1 分钟写入后过期 = 固定窗口；50000 上限防内存膨胀
    private final Cache<String, AtomicInteger> counters = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {
        String path = UrlPathHelper.defaultInstance
                .getLookupPathForRequest(request);   // 与 MVC 查找路径一致，防 URI 编码绕过
        RateLimitProperties.Limit limit = matchLongestPrefix(path);
        if (limit == null) return true;              // 未配置前缀的接口不限流
        String key = getClientIp(request) + ":" + path;
        int count = counters.get(key, k -> new AtomicInteger()).incrementAndGet();
        if (count > limit.getMaxRequests()) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"code\":429,\"message\":\"Too many requests, please try again later\"}");
            return false;
        }
        return true;
    }
}
```

四个防御细节：**用 `getLookupPathForRequest` 而不是 `request.getRequestURI()`**，前者会解码 URI——否则 `%2Fapi%2Fpost` 这种编码变体就能绕过前缀匹配；**最长前缀胜出**（`base.equals(path) || path.startsWith(base + "/")`，取最长匹配），防止一条新路径被同名开头的旧阈值误伤；**计数键是 `ip:path`**，同一 IP 打不同接口各算各的；**`getClientIp` 只在 `isTrustedProxy`（10./192.168./127.0.0.1/::1/172.16-31）成立时才信任 `X-Forwarded-For` 首段与 `X-Real-IP`**——直连场景下 XFF 是请求方随便填的头，信它等于把限流钥匙交给攻击者。

注册方式（WebMvcConfig）体现了关注点分离：日志拦截器 order(1) 挂全路径（排除 /uploads、/actuator、swagger 相关），限流拦截器 order(2) 只挂 `/api/**`——静态资源和 SEO HTML 不限流，否则爬虫一多就把正经访客挡在门外。新增接口若不在已有前缀下，要在 `RateLimitProperties` 补一条阈值。

### 7.9.2 CSRF：为什么可以关，XSRF 头为什么还要带

7.6 节说了 `csrf.disable()` 的成立条件（Bearer 认证无 Cookie 自动携带）。那前端为什么还发 `X-XSRF-TOKEN`？**纵深防御 + 前向兼容**：CORS 白名单明确允许这个头，`generateXsrfToken` 用 `crypto.getRandomValues` 生成 32 字节 hex 存入 storage，每个请求自动回显。如果未来某处引入了 Cookie 会话（比如「记住此设备」），把 Spring Security 的 cookie 版 CSRF 仓库打开即可，前端一行不用改。

### 7.9.3 X-Visitor-Id：无 Cookie 的匿名身份

访客统计与匿名点赞需要区分「同一个人」，但对未登录访客不能种 Cookie（隐私合规敏感点，也污染浏览器状态）。SelfBlog 的方案分两半：

前端：首次访问生成随机 `visitorId`（`crypto.randomUUID().replace(/-/g, '')`，格式约束 `[A-Za-z0-9_-]{8,80}`），存 localStorage，之后每个请求由拦截器带 `X-Visitor-Id` 头。

后端：**从不存原始 visitorId，只存加盐哈希**——`VisitorHashUtils.hash(visitorId, hashSecret)`，其中 `hashSecret` 复用 `JWT_SECRET`（一个部署里少管一个秘密）。同一访客对同一文章的点赞状态由 `postId + visitorHash` 唯一确定（`anonymous_post_like` 表的三态切换：没点过 → 插入；有效点赞存在 → 软删即取消；软删记录存在 → 复活即再点赞，天然幂等，避免了并发重复行问题）。

为什么存哈希不存原文？visitorId 虽是随机数，但长期不变、跨请求可关联——直接存等于给每个访客发了一个**可跨表关联的持久标识符**。加盐哈希后，即使整库泄露也无法还原 visitorId、无法把两张表里的同一访客对上号。信任模型也是清醒的：这个头可以伪造，所以它只用于**聚合统计与匿名交互状态**，不用于任何鉴权或限流决策；点赞 toggle 的并发安全由单机 `synchronized` + SQLite 池大小 1 的部署形态兜底——**不是所有项目都需要分布式锁，先认清部署形态再选工具**。

### 7.9.4 DOMPurify 防 XSS：最后一道防线

XSS 的完整防线有三层。第一层在入库前（7.4.2 的 `sanitizeRichText`/`sanitizeMarkdown`）；第二层是评论这类纯文本输入走 `sanitizePlainText`（标签转义/剥离，**消毒后再校验长度**——`&lt;script&gt;` 转义后会变长，先校验后消毒会让超长脏数据溜进库）；第三层在前端渲染时：

```ts
import DOMPurify from 'dompurify';

export function renderSafeMarkdown(raw: string): string {
  const html = marked.parse(raw) as string;
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'a', 'img',
                   'code', 'pre', 'blockquote', 'ul', 'ol', 'li', 'table', ...],
    FORBID_ATTR: ['style', 'onerror', 'onload'],
  });
}
```

注意 `v-html` 本身不是禁区——禁区是**未经消毒的 `v-html`**。Markdown 渲染器允许内嵌原生 HTML，而 `<img onerror=...>`、`<a href="javascript:...">` 都能穿过 marked，DOMPurify 的白名单把它们挡在挂载之前。三层清洗里任何一层失守，下两层还在。SQL 注入则由 MyBatis 参数化全权负责，`visitorId` 的正则白名单再补一刀——注入类攻击从格式上就不可行。

## 7.10 SEO 与服务端渲染 HTML

### 7.10.1 为什么 SPA 对 SEO 不友好

爬虫（尤其是不执行 JS 的那些）抓到 SPA 的响应是一具空壳：

```html
<div id="app"></div>
<script src="/assets/index-abc123.js"></script>
```

标题、正文、发布时间全都不在 HTML 里——它们要等 JS 下载执行、Axios 请求返回、Vue 挂载后才出现。爬虫等不等、等多久、执行不执行 JS，各家不一且随时变。对一个**以内容为核心竞争力**的博客，这是致命的。正统解法是 SSR 框架（Nuxt/Next），但那意味着重写整个前端、引入双端渲染的复杂度——对一个单体 JAR 项目过重。

### 7.10.2 SelfBlog 的答案：按请求组装的可阅读 HTML

SelfBlog 的方案是：**后端在返回 SPA 壳之前，把真实内容直接渲染进 HTML**。`controller/SpaController.java` 不再统一回退空壳 index.html：

```java
// controller/SpaController.java（@Controller，无 /api 前缀）
@GetMapping({"/",
             "/{path:^(?!api$|assets$|uploads$|actuator$|v3$|swagger-ui$|doc\\.html$)[^\\.]*$}",
             "/{path:...}/**"})
public ResponseEntity<String> readingPage(HttpServletRequest request) {
    SiteOrigin resolvedOrigin = origin.resolve(request);
    SeoModels.ReadingPage page =
        reading.resolve(request.getRequestURI(), request.getQueryString());
    return renderer.render(page, resolvedOrigin);
}
@GetMapping("/index.html")
public ResponseEntity<Void> indexAlias() {
    return ResponseEntity.status(308).location(URI.create("/")).build();  // 防双份收录
}
```

`service/site/seo/ReadingPageService.resolve()` 按路径分发：`/` 渲染首页 9 篇文章列表，`/post/{id}` 渲染文章正文，`/tag/{id}`、`/pages/{slug}`、`/archives`、`/gallery`、`/search` 各有模板；`/login`、`/profile`、`/drafts` 等 `SHELL_PATHS` 返回启动壳并强制 `noindex`（后台与个人页没有 SEO 价值）；未知路径返回真实 404 HTML。产出的 HTML 里有 `.seo-reading` 容器，正文、标题、发布时间全部是**静态文本**——禁用 JavaScript 也能完整阅读。Vue 挂载后整体替换 `#app` 内容，无缝接管交互。CI 的 docker job 里有一条断言：容器起来后 grep 首页 HTML 必须能找到 `seo-reading`——**SEO 契约被自动化测试锁住**。

这个类有一段值得刻在脑子里的注释级教训：**它刻意不声明类级 `@Transactional`**。原因是内部会调用「找不到就抛 404」的查询——如果这些查询共享同一个事务，第一个异常会把事务标记为 rollback-only，最终 404 在提交时变成 `UnexpectedRollbackException`（实测表现为 500）。让每个内层查询用自己的只读事务，404 才能安全地转换成 404。这是「事务传播与异常处理交互」的活教材。同类注释还写明只读承诺：不加阅读量、不记访客、不读管理草稿与相册解锁令牌——SEO 路径对业务零副作用。

缓存策略同样精细：

```java
if (cacheable(page)) {   // status==200 且不是 SHELL
    String etag = "\"" + sha256Hex(body).substring(0, 16) + "\"";
    if (ifNoneMatchMatches(request.getHeader("If-None-Match"), etag)) {
        return ResponseEntity.status(304).build();   // 未变更：304 省带宽
    }
    // 200 + ETag + Cache-Control "public, max-age=0, must-revalidate"
}
```

为什么是 `max-age=0, must-revalidate` 而不是长缓存？因为 HTML 内容随文章编辑随时变化，没有版本号可用；ETag（SHA-256 前 16 字节）让浏览器每次回源校验，未变更请求以 304 结束——**新鲜度与带宽的折中**。状态码哲学：缺失 404、模板缺失 503、错误页 noindex——**不拿 200 包一切**，爬虫才能正确删除已消失页面的索引。

### 7.10.3 SeoController：feed、sitemap、robots

订阅器与爬虫只访问根路径，所以 `SeoController` 必须是 **`@Controller`**（`@RestController` 会被 WebMvcConfig 自动加 `/api` 前缀，订阅器就找不到了）：

```java
@Controller
public class SeoController {
    @GetMapping(value = "/feed.xml", produces = "application/rss+xml")
    public String feed() { ... StringBuilder 拼 RSS 2.0：channel/item/guid/pubDate/category ... }

    @GetMapping("/atom.xml") ...   // Atom 1.0，published/updated（updateTime 兜底 createTime）

    @GetMapping("/sitemap.xml")
    public String sitemapIndex() { return sitemaps.index(base); }        // 索引 + 分片
    @GetMapping("/sitemap-site.xml") ...
    @GetMapping("/sitemap-{kind:[a-z]{2,10}}-{part:\\d{1,5}}.xml")
    public String shard(@PathVariable String kind, @PathVariable String part) { ... }

    @GetMapping(value = "/robots.txt", produces = "text/plain")
    public String robots() {
        return """
            User-agent: *
            Allow: /
            Disallow: /%s
            Disallow: /api/
            Disallow: /search
            Sitemap: %s/sitemap.xml
            """.formatted(adminPath, base);
    }
}
```

数据源是 `PostQueryService.getPublishedMetadata()`——一条只取元数据列（不捞 content 大字段）的轻量查询，最多 100 条按 `createTime` 倒序。三个细节：XML 拼装自带 `escape()`（转义五个 XML 实体并**丢弃 XML 1.0 非法控制字符**，保留 `\t\n\r`——正文里混进控制字符会让整个 feed 解析失败）；sitemap 是索引 + 每片 1000 条的分片结构，为文章量增长预留；`robots.txt` 动态生成，因为 `Sitemap:` 必须是绝对地址（依赖 `SiteOrigin` 解析的 base），而 `Disallow: /{adminPath}` 依赖站点设置的实时值——同时注释里点明：`Disallow` 后台入口只是防止收录，**不是鉴权替代**。`SiteOrigin` 的优先级——**后台「网站设置 → 网站域名」(`siteBaseUrl`) → 环境变量 `SITE_BASE_URL` → 直连请求地址**——配置后，伪造 `Host`/`X-Forwarded-*` 头再也影响不了 canonical、订阅链接和站点地图地址。

## 7.11 测试策略

### 7.11.1 后端切片测试的两个铁律

SelfBlog 后端 588 个测试里，Controller 层大量使用 `@WebMvcTest` 切片。第一个铁律：**必须 `@MockBean JwtUtils`**。

```java
// src/test/java/com/selfblog/controller/BlogPostControllerTest.java
@WebMvcTest(BlogPostController.class)
class BlogPostControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean BlogPostService blogPostService;
    @MockBean JwtUtils jwtUtils;              // ★ 不 mock 就启动失败
    ...
}
```

原因在装配链：`@WebMvcTest` 会加载 `SecurityConfig`，而 `SecurityConfig` 构造过滤器链时注入 `jwtAuthenticationFilter`，后者依赖真实的 `JwtUtils` Bean——切片扫描不到它（`@WebMvcTest` 默认只装 Web 层组件），Spring 启动直接报 `NoSuchBeanDefinitionException`。mock 掉 JwtUtils，过滤器链才能完整装配。

第二个铁律：**不要用 `@AutoConfigureMockMvc(addFilters = false)`**。实测它不是「绕过鉴权」而是让安全过滤链处于半装配状态，请求一律 401——想测业务逻辑反而全被挡住。正确姿势是把 `SecurityConfig` 完整拉起，再用测试上下文或注解提供身份：

```java
@BeforeEach
void setUpSecurityContext() {
    SecurityContextHolder.clearContext();
    var userContext = new UserContext(1L, "admin");        // 或 "user"
    var auth = new UsernamePasswordAuthenticationToken(
            userContext, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
}

@Test
void createPost_Success_ReturnsPostId() throws Exception {
    when(blogPostService.createPost(any(), eq(1L), eq(false))).thenReturn(100L);
    mockMvc.perform(post("/post")          // ★ 无 /api 前缀
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"测试标题","content":"正文内容"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").value(100));
}

@Test
void createPost_NotLoggedIn_Returns401() throws Exception {
    // 不灌安全上下文 → 过滤器解析不到身份 → SecurityUtils 抛 401
    mockMvc.perform(post("/post").contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"t\",\"content\":\"c\"}"))
        .andExpect(status().isUnauthorized());
}
```

四个细节：测试里的路径**不带 `/api`**（前缀是运行时 WebMvcConfig 注入的，MockMvc 走的是控制器映射前的路径）；用真实 `UserContext` 当 principal 灌上下文（等价于 `@WithMockUser(roles = "admin")` 但带着项目自己的身份类型），这样 `SecurityUtils.isCurrentUserAdmin()` 在测试里真实生效；方法级 `@PreAuthorize` 在切片里是**生效的**——非管理员 403、匿名 403 都能直接断言；每个测试前 `SecurityContextHolder.clearContext()` 防上下文串测试。

### 7.11.2 前端 jsdom 的四个陷阱

前端 400 个测试跑在 jsdom 里，`src/test/setup.ts` 只做了最小桩：

```ts
// frontend/src/test/setup.ts（全文 25 行）
import '@testing-library/jest-dom';
config.global.stubs = {};
config.global.mocks = {
  $route: { path: '/', params: {}, query: {} },
  $router: { push: vi.fn(), replace: vi.fn() },
};
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false, media: query, onchange: null,
    addListener: vi.fn(), removeListener: vi.fn(),
    addEventListener: vi.fn(), removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});
```

**陷阱一：setup 没有自动卸载。** 凡是挂载时会注册全局监听器（scroll、resize、matchMedia change）的组件，测试文件必须自己 `enableAutoUnmount(afterEach)`，否则监听器跨测试泄漏，后面的测试莫名其妙失败——而且是顺序依赖的失败，单跑又绿。

**陷阱二：rAF 不能桩成同步。** 滚动显现动画（`vScrollReveal`）依赖 requestAnimationFrame 排队。把 rAF 桩成「立即执行」会让排队语义消失；正确做法是维护一个队列，测试里手动 flush：

```ts
const rafQueue: FrameRequestCallback[] = [];
vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
  rafQueue.push(cb); return rafQueue.length;
});
// 测试中：
rafQueue.forEach((cb) => cb(performance.now())); rafQueue.length = 0;
```

**陷阱三：scrollY 不可直接赋值。** jsdom 的 `window.scrollY` 是 getter，改 `scrollY` 要用 `Object.defineProperty(window, 'scrollY', { value: 800, configurable: true })` 再派发 `scroll` 事件。

**陷阱四：路由测试用真实的 memory history。** `createRouter({ history: createMemoryHistory() })` 配合真实 RouterLink，而不是 `stubs: { RouterLink: true }`——stub 会丢掉 RouterLink 的 slot 内容（链接文字全消失），也表达不了 active 状态。`createMemoryHistory` 让「点击链接 → 路由跳转 → 断言 URL」完整可测。离开守卫这类涉及 RouterView 的测试，必须把组件挂在真实 RouterView 下才能测准。

### 7.11.3 部署契约也是测试

`npm run test:deployment`（node:test，27 项）把部署配置本身当测试对象：Vercel 代理生成器的输出结构、Docker/Compose 文件的源契约（构建上下文、卷挂载、健康检查）都在断言之列。思想：**部署配置和代码一样会腐化，也一样可以用测试钉住**。每次修 bug 先补一个能复现它的测试，再修到绿——588/400 两个基线就是这么维护下来的。

## 7.12 部署

### 7.12.1 Docker Compose：仓库根上下文

SQLite 方案（单人博客首选）：

```yaml
# backend/docker-compose.sqlite.yml
services:
  app:
    build:
      context: ..               # ★ 上下文是仓库根，不是 backend/
      dockerfile: backend/Dockerfile
    env_file: .env
    environment:
      DB_TYPE: sqlite
      DB_URL_SQLITE: jdbc:sqlite:/app/data/campus_blog.db   # 库文件进卷
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "127.0.0.1:8825:8825"
    volumes:
      - sqlite_data:/app/data
      - app_uploads:/app/uploads          # 上传图片
      - app_gallery_media:/app/gallery-media   # 相册媒体
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8825/actuator/health"]
      start_period: 60s
volumes:
  sqlite_data: {}
  app_uploads: {}
  app_gallery_media: {}
```

`context: ..` 是关键决策：镜像里要**同时构建 backend 与内嵌 frontend**（最终产物是内嵌前端产物的单 JAR），上下文必须是仓库根才能同时看到两个目录（`entrypoint.sh` 与独立前端部署因此被删除）。三个卷缺一不可：数据库文件与 WAL/SHM 同目录持久化，两个媒体目录丢了图就没了——**升级镜像不清卷是底线**。使用：

```bash
docker compose -f backend/docker-compose.sqlite.yml up -d --build
```

MySQL 方案（`backend/docker-compose.yml`）多一个 `mysql:8.0` 服务，把 `schema-fresh-install.sql` 挂到 `/docker-entrypoint-initdb.d/01-init.sql:ro`——注意 MySQL 官方镜像的初始化脚本**只在空数据目录首次启动时执行**，已有库请改跑 `migration-selfblog.sql`；`app` 服务 `depends_on: {mysql: {condition: service_healthy}}`，网络 `selfblog-network`。两套方案二选一，不要同时启动。诚实声明：开发机没有 Docker 守护进程，构建与运行由 CI 的 `docker` job 验证（构建镜像 → SQLite 起容器 → 等 `/actuator/health` → grep 服务端首页 HTML 含 `seo-reading`），真实服务器部署前请自行再验证一遍。

### 7.12.2 Vercel：代理生成器，不是第二份前端

`frontend/vercel.json` 全文只有 5 行：

```json
{
  "framework": null,
  "buildCommand": "node scripts/build-vercel-proxy.mjs",
  "installCommand": "node -e \"console.log('Proxy deployment needs only Node built-ins')\""
}
```

这不是「在 Vercel 上部署一份 Vite 前端」，而是一个**代理生成器**：`framework: null` 关掉 Vercel 的 Vite 自动预设（否则它会尝试 `npm run build` 出一份 dist），`build-vercel-proxy.mjs` 只用 Node 内置模块生成 Build Output v3 的路由配置，把**所有路径、所有方法**转发到唯一源站（运行中的 selfblog JAR）。架构含义：世界上只有一份前端（JAR 内嵌的），Vercel 只是一个带 HTTPS 和 CDN 的反向代理——彻底消除了「两份前端版本漂移」这类经典事故。构建时强制校验两个环境变量：

- `SELFBLOG_BACKEND_ORIGIN`：源站 HTTPS origin（JAR 的地址）
- `SITE_BASE_URL`：正式访客 origin（Vercel 域名）

两者相同或缺失直接构建失败（自引用转发会死循环）。Preview 环境自带 `X-Robots-Tag: noindex,nofollow`，预览部署不会被搜索引擎收录。`npm run test:deployment` 的 27 项断言钉住了这整套契约。

### 7.12.3 SITE_BASE_URL 的第二重身份

这个环境变量还出现在后端：它是 `SiteOrigin` 解析站点 origin 的第二优先级（后台设置 → 环境变量 → 直连请求）。把后端放在反向代理后面时必须显式设置，否则按请求 Host 推导的 canonical、RSS 链接、sitemap 地址可能拿到代理的内部地址。一句话总结：**「这个站点对外的正式地址是什么」必须有一处权威配置，其余地方推导出来的都是临时值**。生产 profile 还要记得：Knife4j 在 `prod` 下自动关闭，`SWAGGER_ENABLED` 不要在生产打开。

## 7.13 从零实现一个新功能：阅读时长估算

现在把前 12 节的一切串起来：走一遍给文章加「阅读时长估算」的完整流程。前端其实已有按正文估算的阅读时长（`utils/reading`），我们借这个例子演示「把估算搬到后端持久化」的规范工作流——这顺便回答一个架构问题：估算逻辑放哪？（答案见第 5 步。）

### 第 1 步：设计（先想清楚，再动手）

数据流：发布/更新文章时按内容长度估算 → 存进 `blog_post.reading_minutes` → 详情接口返回 → 前端显示「约 N 分钟」。变更面：三份 schema（SQLite/MySQL/H2）、实体、门面、响应 VO、前端组件、两套测试、CHANGELOG。更大的改动按仓库惯例先写设计再实现：先写 spec（含「已核实的现状」证据表）、再拆带复选框的任务清单，然后动手（这类文档历史上放在 `docs/superpowers/`，该目录已于 2026-10-19 清理）。本例规模适中，直接开工但依然先列清单。动手前按仓库强制规范**完整读一遍 Controller → Service → Mapper → Entity 链路**——确认现有行为，不靠猜。

### 第 2 步：数据库字段

`schema-sqlite.sql` / `schema-fresh-install.sql` 的 `blog_post` 表加：

```sql
reading_minutes INTEGER NOT NULL DEFAULT 0,
```

已有库走 `migration-selfblog.sql` 追加：

```sql
ALTER TABLE blog_post ADD COLUMN reading_minutes INTEGER NOT NULL DEFAULT 0;
```

MySQL 的等价语法写进 `schema-fresh-install.sql` 对应位置，**三个 schema 文件必须同步改**，漏一个 CI 才会告诉你（mysql job 会炸）。

### 第 3 步：实体与 DTO

```java
// entity/BlogPost.java
/** 阅读时长（分钟），按正文长度估算，发布时计算 */
private Integer readingMinutes;

// dto/post/PostCreateRequest.java —— 无需改动：它由服务端计算，不接受客户端传入
```

接受客户端传值是反面教材：伪造 `readingMinutes: 999999` 会污染数据。**可派生的字段一律服务端算**。

### 第 4 步：门面里的计算与持久化

```java
// service/post/impl/BlogPostServiceImpl.java
// 中文约 400 字/分钟，英文约 200 词/分钟，取两者较大者并至少 1 分钟
private int estimateReadingMinutes(String content) {
    if (content == null || content.isBlank()) return 1;
    int cjk = 0, words = 0;
    for (String token : content.split("\\s+")) {
        if (token.matches("[\\x00-\\x7F]+")) words++;
    }
    for (int i = 0; i < content.length(); i++) {
        if (Character.UnicodeScript.of(content.charAt(i))
                == Character.UnicodeScript.HAN) cjk++;
    }
    int byCjk = (int) Math.ceil(cjk / 400.0);
    int byWords = (int) Math.ceil(words / 200.0);
    return Math.max(1, Math.max(byCjk, byWords));
}

// createPost / updatePost 里，save 之前：
post.setReadingMinutes(estimateReadingMinutes(post.getContent()));
```

放门面而不是查询服务：它是**写时派生**的数据，属于「编排」职责；查询服务只负责读出来。放在写路径还有个好处——历史文章靠下次编辑时自然修正，无需跑数据迁移脚本。

### 第 5 步：接口返回

```java
// vo/post/PostDetailResponse.java
private Integer readingMinutes;
```

`assemblePostDetail` 里拷贝即可。**这里回答第 1 步的问题**：估算逻辑放后端是因为它要进 RSS、SEO HTML、未来的 API 消费方——前端算一次只有浏览器看得到。前端已有的本地估算保留作兜底（旧数据 `readingMinutes` 为 0 时用它）。

### 第 6 步：前端展示

```vue
<!-- views/post/PostDetail.vue 的元信息行 -->
<span class="meta-item">
  <Icon name="clock" :size="14" />
  {{ post.readingMinutes ? `约 ${post.readingMinutes} 分钟` : estimateLocal(post.content) }}
</span>
```

注意本仓库的 `noUncheckedIndexedAccess` 严格模式（`tsconfig.json`）：索引访问返回 `T | undefined`，`arr[0].foo` 过不了 type-check，要 `arr[0]?.foo` 或改用 `replace` 之类的写法。

### 第 7 步：测试

```java
// 后端：BlogPostServiceImplTest 中
@Test
void estimateReadingMinutes_ChineseContent_ReturnsByCjkSpeed() {
    String content = "汉".repeat(800);          // 800 汉字 → 2 分钟
    assertEquals(2, invokeEstimate(content));
}
@Test
void estimateReadingMinutes_Blank_ReturnsAtLeastOne() {
    assertEquals(1, invokeEstimate(""));
}
```

```ts
// 前端：PostDetail 测试中，mock postApi.getPostDetail 返回 readingMinutes: 3，
// 断言渲染出「约 3 分钟」；readingMinutes 为 0 时回退本地估算。
```

全绿后跑全套闸门：后端 `mvnw test`（带两个 skip 标志，保持 588+ 全过），前端 `lint:check / format:check / type-check / test:run / build` 五道。任何一项红了都不算完成。

### 第 8 步：文档与 CHANGELOG

按仓库强制规范，在 `CHANGELOG.md` 顶部追加：

```markdown
## 未发布 — 文章阅读时长估算落库 (2026-10-XX)

### 说明
- 发布/更新文章时按正文长度估算阅读时长并持久化到 blog_post.reading_minutes
- 详情接口返回 readingMinutes；前端优先展示服务端值，旧数据回退本地估算
- 无需数据迁移：历史文章在下次编辑时自动补算

### 影响文件
| 文件 | 改动类型 |
|------|---------|
| backend/src/main/java/com/selfblog/entity/BlogPost.java | 修改 |
| backend/src/main/java/com/selfblog/service/post/impl/BlogPostServiceImpl.java | 修改 |
| backend/src/main/resources/schema-sqlite.sql | 修改 |
| backend/src/main/resources/schema-fresh-install.sql | 修改 |
| backend/src/main/resources/migration-selfblog.sql | 修改 |
| backend/src/test/java/.../BlogPostServiceImplTest.java | 修改 |
| frontend/src/views/post/PostDetail.vue | 修改 |
```

受影响的分册 README（backend/frontend 的接口清单）同步更新。至此一个功能走完「schema → 实体 → 服务 → 接口 → 前端 → 测试 → 文档」全环节——**没有一步是可选的**，这正是这个仓库的工作流纪律（「以跳过验证为耻，以主动测试为荣；以忘记更文档为耻，以及时更新为荣」）。

## 7.14 项目演进与架构决策复盘

### 7.14.1 从社区到书房：三个关键取舍

v1 的 `campus_blog` 是标准多用户社区：注册、登录、关注、校友圈、话题、通知、草稿、举报。改造成 v2 单人博客时，每一刀都砍在一个产品判断上：

**取舍一：注册关闭，而不是「限制注册」。** `SysUserController.register()` 的实现是一行 `throw new BusinessException(403, "此博客不开放访客注册")`，`AuthController` 的两个验证码端点同样直接抛 403。为什么不做成「可配置开关」？因为多一个配置就多一个测试矩阵维度（开/关 × 各接口），而单人博客**永远**不会开放注册——YAGNI：为不存在的需求付复杂度是最贵的浪费。注意 `SecurityConfig` 仍把这几个端点 `permitAll`：要让访客收到明确的 403 业务文案，而不是被 401 entry point 拦截后一脸茫然。

**取舍二：发布即上线，不引入待审核。** `createPost` 直接 `status=1`。v1 的审核流（`approvePost`/`rejectPost`/`getReviewList`）只对历史 `status=0` 数据生效。多人社区里审核是内容质量控制；单人博客里作者是唯一作者也是唯一审核员，「自己审自己」是纯粹的仪式。删除一个流转环节，等于删除一整套状态机、两种列表页和一批测试。

**取舍三：历史社区表保留，公开入口下线。** `blog_follow`、`blog_notification`、`blog_circle_*`、`views/user/*` 这些表和后端接口**都没删**——它们保留为 admin-only 的 API 面（`/api/admin/**` 的兜底规则保护着）。删表的风险是：历史数据丢失、迁移脚本复杂化、以及「以为没用了结果某个角落还在引用」的连锁事故。保留的成本只是几张空表。对应地，前端死代码（`views/circle/*`、`views/auth/Register.vue`、`components/home/*`、`Card.vue`、`usePagination` 等 47 个文件）在 2026-10-17 分两波删除——**前端删、后端留**的不对称是刻意的：前端死代码零成本（没路由引用就是纯垃圾），后端接口有数据依赖。

### 7.14.2 改造遗留的架构指纹

这次改造在代码里留下了可辨认的指纹，读代码时可以当「地质层」看：

- `PostDetailResponse` 里同时有 `username/nickname/avatar`（社区时代的多用户展示）和单人博客专属字段
- 访客点赞/留言靠 `guest-{ip}-{ua}` 或哈希后的 visitorId 标识，因为访客没有账号体系了；点赞总数还要把历史登录点赞计数与匿名点赞表**相加聚合**——两套点赞体系并存是改造期项目的典型中间态
- `SHELL_PATHS` 里 `/drafts`、`/collections`、`/following` 这些路由返回启动壳——页面还在注册，公开导航已不可达
- 审核相关接口（`getReviewList`/`approvePost`）还在，但只对历史 `status=0` 数据有意义

理解了这些，你就能读懂「为什么这个项目长这样」而不只是「这个项目长这样」——**架构决策复盘的价值在于把历史约束从代码里解码出来**，避免后来的维护者好心「清理」掉其实有意的保留，或者反过来给死路径续命。

### 7.14.3 给后来者的决策清单

把 SelfBlog 的决策经验抽象成可迁移的原则：

1. **身份决定架构**。单人站点不需要注册、审核、多租户隔离，砍掉它们换来的是安全面缩小（只有一个管理员）、状态机消失（发布即上线）、运维简化（单 JAR 单库）。
2. **白名单优于黑名单**。`anyRequest().access(...)` 的默认拒绝模型让「忘配权限」的后果是「不可用」而非「泄露」。
3. **写时派生优于读时计算**，当派生结果要被多端消费时（RSS、SEO、API）。
4. **KV 表用于配置，宽表用于业务**。site_setting 的零迁移不是银弹，但对「键少、值杂、变更频繁」的配置域是完美匹配。
5. **命名即文档**。`schema-fresh-install.sql` 的改名证明：一个危险文件的危险性能靠名字降低。
6. **保留比删除便宜，直到证明相反**。社区表保留至今没造成任何事故，而误删引发的迁移噩梦真实存在于每个大项目的历史里。
7. **诚实标注未验证项**。Docker、Vercel、MySQL 的运行时验证缺口被明确写进文档，而不是假装全绿——元数据不是证明。

## 7.15 新人上手 48 小时路线图

假设你 clone 了 SelfBlog，要在两天内达到「敢改代码、能提 PR」的水平——这是新成员加入任何项目都要解决的问题，SelfBlog 是一个可复制的样板。路线图按「先跑起来 → 先看主线 → 再动手 → 最后补理论」设计，顺序不能乱。

### 第一阶段：让项目跑起来（第 1～2 小时）

目标不是理解，是**获得一个可反复重置的验证环境**——之后每学一个点都在这里验证。

1. 装 JDK 21（`JAVA_HOME` 指向它），确认 `backend\mvnw.cmd -v` 能报 Maven 3.9.x
2. 复制 `backend/.env.example` 为 `backend/.env`，填 `JWT_SECRET`（≥32 位）与 `ADMIN_PASSWORD`（≥12 位）
3. 跑后端测试：`backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"`——588 个测试全绿，说明环境无恙；**这两个 skip 参数以后每次都要带**，否则 Maven 会顺手重建前端
4. 起后端（`mvnw.cmd spring-boot:run` 带同样双参数），确认 `http://localhost:8825/actuator/health` 返回 UP
5. 起前端：cd frontend 后**直接调 `./node_modules/.bin/vite`**（本机 npm run 可能因 PATH 引号问题全挂，见附录 D），打开 http://localhost:3000

验收标准：博客首页能打开，`/admin` 能登录。卡住就查附录 A 排查手册，不要跳过——**第一天的问题清单就是你对这个项目的第一份理解**。

### 第二阶段：走读两条主线（第 3～6 小时）

不看全部代码，只走两条「全栈线」，每条线从浏览器出发一路点到数据库：

**线一：访客读文章**（第 3～4 小时）
`views/post/PostDetail.vue` → `api/post.ts` →（Axios 拦截器 `api/index.ts`）→ `BlogPostController` → `BlogPostService`（注意一行委托给 QueryService 的门面模式）→ `PostQueryService`（@Cacheable 在这里）→ `BlogPostMapper` → SQLite 文件。配套阅读：本教程第 3 章 §3.7（MVC）与 §3.12（MyBatis Plus）、第 5 章 §5.6（Axios 封装）。

**线二：管理员发文**（第 5～6 小时）
登录页提交 → `AuthController` → JWT 签发 → 前端存 token（`stores/user.ts` + `utils/crypto`）→ 发文表单 → `AdminPostController`（@PreAuthorize）→ 门面 → 库。配套阅读：第 3 章 §3.13（Security+JWT）、第 7 章 §7.4/§7.6。

走读方法：**开 IDEA 的 Ctrl+B 跳转链，手边开着 SQLite 工具直查表**（附录 D §D.4），每个接口都亲眼看到数据怎么变。两条线走完，你对项目的理解已超过 80% 只读文档的人。

### 第三阶段：完成一个端到端小改动（第 7～10 小时）

在真实改动里学会全部协作纪律。选一个「加字段」级别的任务，例如给文章列表加「字数统计」：

1. **先读链路再动手**（仓库铁律）：Post → Controller → Service → Mapper → Entity 完整读一遍
2. 改库：SQLite 手动 `ALTER TABLE` + 三份 schema 同步（`schema-sqlite.sql` / `schema-fresh-install.sql` / `migration-selfblog.sql`），理解为什么同一字段要写三处
3. 改后端：Entity 加字段 → 让 MetaObjectHandler 或业务代码填充 → VO 透出
4. 改前端：`types/` 加类型 → api 层 → 列表组件展示
5. **跑全部门禁**：后端 588 测试、前端 lint/format/type-check/43 文件 400 测试/build
6. **更新文档**：CHANGELOG.md 顶部追加「未发布」条目（含影响文件表）+ 受影响 README

这个练习的价值：一次走完「库 → 后端 → 前端 → 测试 → 文档」全流程，之后接任何任务都是它的变体。

### 第四阶段：建立自己的安全网（第 11～14 小时）

- **跑一遍测试切片配方**：挑一个 `@WebMvcTest`（如 AuthControllerTest）读懂三要素——`@MockBean JwtUtils`、`@WithMockUser(roles="admin")`、路径不带 `/api`；再跑一个 Vitest 组件测试，注意 `enableAutoUnmount(afterEach)`
- **读一份 ADR**（docs/decisions/），理解「为什么这么设计」被记录下来的机制
- **故意弄坏一次**：改坏一个 Controller 再看 GlobalExceptionHandler 怎么兜底、测试怎么红——**知道项目怎么失败，才算知道它怎么工作**
- 通读本教程第 7 章 §7.9（安全清单）与附录 A（错误排查手册）的高频条目

### 第二天下午起：按需深入 + 长期习惯

之后按任务需要补理论（第 1～6 章），但建立三个终身习惯：

1. **改代码前先读链路**，确认现状，绝不猜测
2. **文档与代码同步提交**——CHANGELOG 里那条「影响文件」表格就是项目的心跳记录
3. **凡是「应该能行」的都没验证过**——跑一遍才算数（这条纪律让 SelfBlog 的 CI 四个 job 全部真实执行）

48 小时后你应该能独立接小任务。这份路线图本身也可以迁移到任何 Spring Boot + Vue 项目：换掉目录名，方法论不变。

## 本章总结

本章我们完整走读了一个真实的全栈项目，把前六章的知识点钉在了具体代码上。回顾一遍这条主线：

- **架构**：单体 JAR 同时服务 SPA 与 REST；`@RestController` 自动加 `/api` 前缀，`@Controller`（Spa/Seo）留在根路径——一个注解类型的差异撑起了整个 SEO 方案。
- **分层**：Controller 薄壳、Service 三分（门面编排 / 查询只读缓存 / 交互计数回滚）、Mapper 手写 SQL 自带 `is_deleted=0` 与双方言；`databaseId` 方言隔离 + H2 走兜底语句的规则。
- **契约**：`Result` 信封 + `BusinessException` + `GlobalExceptionHandler` 的集中翻译；前端拦截器返回信封本身，调用方读 `res.data` 而不是 `res.data.data`。
- **安全**：JWT 全链路（生成 → 编码存储 → 拦截注入 → 过滤器解析 → SecurityUtils → @PreAuthorize），credentialVersion 实现改密即下线，refresh token 被锁死在唯一端点，`single-author-mode` 兜底默认拒绝；限流防编码绕过与伪造 XFF，三层清洗防 XSS，visitorId 加盐哈希保隐私。
- **体验与 SEO**：八缓存各按数据新鲜度容忍度定 TTL，evict 链跟着数据流走；服务端组装可阅读 HTML + ETag 协商缓存 + 真实状态码；feed/sitemap/robots 齐备且以 `SiteOrigin` 为唯一地址权威。
- **质量与交付**：切片测试两铁律（@MockBean JwtUtils、禁用 addFilters=false），jsdom 四陷阱（自动卸载、rAF 队列、scrollY defineProperty、真实 memory history），部署配置进测试；Docker 仓库根上下文 + 三卷持久化，Vercel 只做代理生成器。
- **工作流**：新功能走完 schema → 实体 → 服务 → 接口 → 前端 → 测试 → CHANGELOG 全环节，无一步可选。

更重要的是 7.14 节的方法论：**读懂一个项目，要读出它的历史与取舍**。SelfBlog 的每一处「奇怪」设计——关闭的注册接口、保留的社区表、刻意不加的事务注解、故意不改的宽容计数——都是一次明确的产品决策或踩坑教训的化石。愿你接下来的每一次动手改代码前，都先问一句「这里为什么是这样」。

## 参考资源

- **仓库内文档**：`CLAUDE.md` 与 `AGENTS.md`（构建命令与架构约定的权威）、`docs/agents/backend.md` / `docs/agents/frontend.md`（分册深潜笔记）、`docs/decisions/`（ADR 架构决策记录）、`docs/reviews/`（真实验证记录）、`CHANGELOG.md`（每次变更的证据链）
- **官方文档**：[Spring Boot 3.3 Reference](https://docs.spring.io/spring-boot/index.html)、[Spring Security Architecture](https://docs.spring.io/spring-security/reference/)、[MyBatis Plus 官方文档](https://baomidou.com)、[jjwt 0.12.x README](https://github.com/jwtk/jjwt)（新版流式 API 的唯一权威）、[Vue 3 文档](https://cn.vuejs.org)、[Pinia](https://pinia.vuejs.org)、[Vitest](https://vitest.dev)、[Caffeine](https://github.com/ben-manes/caffeine)、[DOMPurify](https://github.com/cure53/DOMPurify)
- **规范与标准**：[RSS 2.0](https://rssboard.org/rss-specification)、Atom RFC 4287、[Sitemaps 协议](https://www.sitemaps.org)、robots.txt RFC 9309、[Vercel Build Output v3](https://vercel.com/docs/build-output-api/v3)（vercel.json 代理生成器的底层协议）
- **安全背景**：[OWASP Top 10](https://owasp.org/www-project-top-ten/)、[OWASP Input Validation Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)
- **本教程前序章节**：第三章 Spring Boot 框架（Security 与事务的原理基础）、第五章 Vue 3（响应式与组合式 API）、第六章 前端工程化（Vite/TS/Vitest 工具链细节）

---

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

---

# 附录B：SelfBlog 源码导读地图 + 全栈术语表

> **适用版本**：SelfBlog v2.0.26。backend：Spring Boot 3.3.13 + Java 21 + MyBatis Plus 3.5.8 + Spring Security + jjwt 0.12.6 + Caffeine；frontend：Vue 3.5.44 + Vite 6 + TypeScript 6.0.3 + Pinia。仓库：https://github.com/Xinghe-0203/SelfBlog 。
>
> 本附录是整部教程的「地图页」。第一部分带你把两个工程的源码目录走一遍：每个包是干什么的、关键类有哪些、**什么时候你会来改这里**；再给出五条后端主题阅读路线与三条前端阅读路线，让你按「任务」而不是按「文件名字母序」读代码。第二部分是一本按主题分组、组内按字母排序的全栈术语表，每条术语都尽量落在 SelfBlog 的真实文件上，避免「背概念背成空中楼阁」。
>
> 两个使用建议：其一，读正文遇到陌生词时，先翻第二部分末尾的「按字母索引」，再回到对应条目与正文章节；其二，本附录的行数、类数、缓存名等数字均按当前源码实测，仓库继续演化后，请以 `pom.xml`、`package.json` 与实际目录为最高权威。

---

# 第一部分：源码导读地图

## 0. 全景：一个仓库，两个独立工程

SelfBlog 仓库没有根构建清单，也没有 npm workspace——`backend/` 与 `frontend/` 是**两个完全独立的工程**，所有命令都要进对应目录执行。它们之间只有一条正式通道：前端把请求发给 `/api/**`（开发期由 Vite 代理转发到后端 8825 端口，部署期由 `frontend/vercel.json` 生成的反向代理转发），后端统一以信封 `Result<T>{code,message,data,timestamp}` 应答。

| 工程 | 技术栈 | 构建工具 | 产物 | 源码根 |
|------|--------|----------|------|--------|
| backend | Spring Boot 3.3.13、Java 21、MyBatis Plus 3.5.8、Spring Security、jjwt 0.12.6、Caffeine | Maven（仓库自带 `mvnw` Wrapper） | `target/selfblog.jar` | `backend/src/main/java/com/selfblog/` |
| frontend | Vue 3.5.44、Vite 6、TypeScript 6.0.3、Pinia、Axios | npm + Vite | `dist/`（另有 Vercel 代理产物） | `frontend/src/` |

还有第三个容易被忽略的区域：`docs/`。教程正文在 `docs/tutorial/`（本附录所在处），架构决策记录在 `docs/decisions/`（ADR），各批次真实验证记录在 `docs/reviews/`。改代码要同步哪些文档，规则见根目录 `CLAUDE.md`。（历史规格与计划目录 `docs/superpowers/` 已于 2026-10-19 清理。）

后端源码根下的包结构一图流（括号内为实测文件数）：

```
com.selfblog
├── SelfBlogApplication.java        启动类（1）
├── annotation/                     自定义鉴权注解（2）
├── aspect/                         AOP 切面：鉴权、性能监控（2）
├── common/                         枚举 / 异常 / 统一响应（11）
├── config/                         全部配置类：security、web、db、cache 等（18）
├── controller/                     REST 控制器（40，含 admin/ 14 个）
├── dto/                            入参对象（33）
├── entity/                         数据库实体（28）
├── event/ + listener/              领域事件与监听器（4 + 1）
├── mapper/                         MyBatis Plus Mapper 接口（28）
├── service/                        业务层：六子包 + impl + seo/page（60+）
├── utils/                          工具类（10）
└── vo/                             出参对象（37）
```

## 1. 后端逐包导读

以下每包按固定三段式写：**职责**——这个包在架构里承担什么；**关键类**——类名加一句话；**什么时候你会来改这里**——把「读代码」变成「改代码」的触发条件。源码根一律省略前缀 `backend/src/main/java/com/selfblog/`。

### 1.1 启动类：SelfBlogApplication.java

**职责**：整个应用的组合注解起点。`@SpringBootApplication` 等价于三个注解的叠加：`@SpringBootConfiguration`（它本身是个配置类）、`@EnableAutoConfiguration`（开启自动配置）、`@ComponentScan`（从本包向下扫描所有组件）。它所在的位置 `com.selfblog` 决定了扫描根，所以任何新包都必须建在它之下，否则注解写得再对也不会被装配。

**关键类**：`SelfBlogApplication`——`main` 方法一行 `SpringApplication.run(...)`，没有多余逻辑；所有「启动时特别做的事」都不在这里，而在 `config/` 包的各个配置类里（这是刻意的：启动类只负责启动）。

**什么时候你会来改这里**：几乎永远不用。只有两种情况值得打开它——加 `@EnableXxx` 类的全局开关注解（如开启异步、调度），或调整扫描根。改完要确认 `mvnw test` 全绿，因为装配错误在启动期就会爆炸。

### 1.2 annotation/：自定义鉴权注解（2 个文件）

**职责**：声明式的权限标记。把「这个接口需要登录/需要管理员」从方法体里的 if 判断，提升为方法签名上的注解，让鉴权横切逻辑由切面统一处理。

**关键类**：
- `RequiresAuth`——标记方法需要已登录用户；
- `RequiresAdmin`——标记方法需要管理员角色。

**什么时候你会来改这里**：新增一个「非 `/api/admin/**` 路径但也要求管理员」的接口时。注意本仓库的惯例：管理员接口统一放 `controller/admin/` 走 URL 前缀 + `SecurityConfig` 的 `hasRole("admin")`，注解方案是补充手段而不是主手段——先看 `aspect/AuthAspect` 怎么消费这两个注解，再决定用哪条路。

### 1.3 aspect/：AOP 切面（2 个文件）

**职责**：把横切关注点（鉴权、性能监控）从业务代码里抽出来，织入到注解标记的连接点或全局切入点。

**关键类**：
- `AuthAspect`——拦截 `@RequiresAuth`/`@RequiresAdmin`，从 `SecurityUtils` 取当前用户做校验，不满足时抛 `BusinessException`；
- `PerformanceMonitoringAspect`——记录慢方法 `[PERF]` 日志。跑测试时日志里大量 `[PERF] ... error:` 是它记录的**正常负例断言输出**，不是失败。

**什么时候你会来改这里**：想给所有 Service 方法加统一计时、或给新注解加横切行为时。改切面前先想清楚切入点表达式的影响面——切面是全局放大器，一个 `execution(* com.selfblog..*(..))` 写宽了会让所有方法都被代理。

### 1.4 common/：枚举、异常与统一响应（11 个文件）

**职责**：全仓库的「公共语言」。枚举定义状态字面量，异常定义错误语义，`Result` 定义 HTTP 响应信封——三层共同保证「前后端对同一件事说同一种话」。

**关键类**：
- `enums/`（8 个）——文章状态、互动类型等业务字面量的唯一出处；
- `exception/BusinessException`——业务异常的唯一形态，构造参数 `(code, message)`，service 层抛、全局处理器接；
- `GlobalExceptionHandler`——`@RestControllerAdvice` 全局兜底：把业务 code 映射为对应 HTTP 状态，未知异常兜底 500 并记日志；
- `result/Result`——信封本体 `{code, message, data, timestamp}`，分页数据额外含 `records/total/size/current/pages`。前端 `api/index.ts` 的拦截器读的就是这个信封。

**什么时候你会来改这里**：新增业务状态（先进 `enums/`）、新增一种错误语义（抛 `BusinessException`，通常不用动处理器）、或需要给信封加全局字段时。**反模式警告**：不要在 controller 里手写 `Result.error(...)`——错误语义统一走异常通道，成功才用 `Result.ok(data)`。

### 1.5 config/：全部配置类（18 个文件）

**职责**：应用所有「可调行为」的声明处。这个包是理解 SelfBlog 行为的钥匙：安全规则、限流阈值、数据库切换、缓存 TTL 全在这里，且全部可用环境变量覆盖。

**关键类（按子包分组）**：

根级 4 个：
- `AsyncConfig`——线程池与异步执行器；
- `DotenvConfig`——解析 `backend/.env`。**注意它用 Java Properties 语法，不是 shell dotenv：值不要加引号**；查找顺序为工作目录 → 用户主目录，文件值会成为 JVM property，优先级高于系统环境变量；
- `EnvValidationConfig`——启动期校验关键配置（如 `JWT_SECRET` 强度），不达标直接拒绝启动（fail-fast）；
- `LoggingConfig`——日志行为声明。

security/ 5 个：
- `SecurityConfig`——安全中枢：`STATELESS` 会话策略；CSRF 关闭（代码注释里有完整论证：JWT 走 `Authorization` 头，跨站页面拿不到该头，伪造请求带不上凭证）；CORS 拒绝通配符结尾的 origin；`X-Frame-Options: DENY`、`Referrer-Policy: strict-origin-when-cross-origin`、`Permissions-Policy` 关闭摄像头/定位/麦克风；`/api/admin/**` 要求 `hasRole("admin")`；401/403 返回 JSON 入口；`BCryptPasswordEncoder` 强度 12（`bcrypt.strength` 可配）；`app.single-author-mode`（默认 true）参与写入类操作的兜底授权；
- `JwtAuthenticationFilter`——**只解析不拒绝**：请求头里有合法 JWT 就解析出用户并构造认证信息放进安全上下文；没有头或 token 无效则放行为匿名，拒绝权留给授权层。这个职责切分是读懂整条认证链的前提；
- `SecurityProperties`——安全相关属性的绑定类；
- `JwtSchedulerConfig`——令牌黑名单的周期清理调度；
- `XssFilter`——请求级 XSS 过滤。

web/ 4 个：
- `WebMvcConfig`——`configurePathMatch` 里 `addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class))` 给**所有** `@RestController` 自动加 `/api` 前缀。因此源码里看不到任何 `/api` 字面量，新写的 `@RestController` 也**不要**自己带 `/api`；
- `RateLimitInterceptor`——按**路径前缀**匹配 `RateLimitProperties` 的阈值表，Caffeine 计数（`expireAfterWrite(1, MINUTES)`、`maximumSize(50000)`），最长前缀匹配，键为 `ip:path`，超限返回 429 JSON。单实例内存实现，代码注释明言多实例需换 Redis；
- `RateLimitProperties`——前缀 → 阈值的配置表；
- `HttpRequestLoggingInterceptor`——请求日志。

db/ 4 个：
- `DatabaseConfig`——**三库切换的枢纽**：`@ConditionalOnProperty(name = "DB_TYPE")`，SQLite（默认，`jdbc:sqlite:campus_blog.db`，Hikari 池大小 1，自动执行 `schema-sqlite.sql`）/ MySQL（`DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD`）/ 不设置则走测试 H2；
- `MybatisPlusConfig`——注册 `VendorDatabaseIdProvider`，让 Mapper XML 支持 `databaseId="mysql"/"sqlite"` 双语句；
- `MyMetaObjectHandler`——审计字段自动填充；
- `DataInitializer`——用 `ADMIN_PASSWORD` 引导初始管理员。

cache/ 1 个：
- `CaffeineCacheConfig`——用 `SimpleCacheManager` 注册 **8 个具名缓存**，各自独立 TTL：`hotTagsCache`(10m)、`categoryCache`(30m)、`userCache`(10m)、`trendingCache`(5m)、`statsCache`(15m)、`statusCache`(2m)、`siteSettingsCache`(10m)、`visitorSummaryCache`(30s)。测试 profile 关闭缓存。

**什么时候你会来改这里**：这是你会最常回来的包。新增公开接口 → `SecurityConfig` 放行；新增限流前缀 → `RateLimitProperties` 补阈值；新增具名缓存 → `CaffeineCacheConfig` 登记 TTL；改数据库策略 → `DatabaseConfig`。改任何一个配置类前，先想清楚它在哪个 profile 生效、哪个环境变量能覆盖它。

### 1.6 controller/：REST 控制器（40 个文件，九个分组）

**职责**：HTTP 入口层。本仓库纪律：controller 只做「接参数、调 service、包信封」三件事，业务逻辑一律下沉 service。所有 `@RestController` 由 `WebMvcConfig` 自动获得 `/api` 前缀，源码路径里看不到它。

**关键类（按分组）**：
- `admin/`（14 个）——后台管理接口。受双重保护：URL 层 `/api/admin/**` 要 `hasRole("admin")`，方法层 `@PreAuthorize` 逐个把关；`AdminPostController` 是文章 CRUD 的入口；
- `auth/`（2 个）——`AuthController`（登录、登出、刷新）与 `PasswordController`（改密码；注册接口一律抛 403「此博客不开放访客注册」）；
- `post/`（5 个）——前台文章读取（列表、详情、归档等）；
- `site/`（7 个）——面向访客的站点级接口：`GuestCommentController`、`AnonymousLikeController`、`PhotoGalleryController`、`PublicPageController`、`SeoController`（**刻意是 `@Controller` 而非 `@RestController`**，以此逃过 `/api` 前缀，在根路径提供 `/feed.xml`、`/atom.xml`、`/sitemap*.xml`、`/robots.txt`）、`SiteSettingsController` 等；
- `social/`（4 个）、`content/`（5 个）——历史社区与内容域接口，公开入口大多已下线，改动前先确认路径是否仍被路由引用；
- `circle/`（1 个）、`user/`（1 个）——历史域的残余入口；
- `SpaController`——SPA 路由的服务端回应者：公开历史路由（`/`、`/about`、`/archives`、`/gallery`、`/post/{id}`、`/tag/{id}`、`/pages/{slug}`）返回由 `service/site/seo/` 组装的**可直接阅读的 HTML**（Vue 挂载后替换 `#app` 内的 `.seo-reading`），状态码真实（缺失 404、`/index.html` 308、模板缺失 503）；后台命名空间与历史账户路由返回启动壳并强制 noindex。它不再统一回退空壳 `index.html`。

**什么时候你会来改这里**：每加一个对外接口都会经过这里。三条铁律：新 `@RestController` 不要自带 `/api` 前缀；新增公开（免登录）接口必须去 `SecurityConfig` 显式放行，并检查是否落在限流前缀内；管理员接口进 `admin/` 而不是自己开路径。

### 1.7 dto/：入参对象（33 个文件）

**职责**：定义 HTTP 请求的形状。按 controller 的域分包：`auth/`(7)、`post/`(6)、`site/`(5)、`social/`(6)、`user/`(3)、`circle/`(3)、`content/`(3)。它与 `vo/` 的分界是方向：dto 进、vo 出，Entity 永远不直接暴露给前端。

**什么时候你会来改这里**：接口需要新字段时。惯例是校验注解（`@NotBlank`、`@Size`）写在 dto 上，service 里再做语义校验——两层校验各管格式与业务。
### 1.8 entity/：数据库实体（28 个文件）

**职责**：表行在 Java 世界的投影。28 个实体全部带 `@TableName` 声明表名，绝大多数带 `@TableLogic private Integer isDeleted` 软删除标记（配合 `application.yml` 的 `logic-delete-field` 全局配置，MyBatis Plus 自动在生成的 SQL 里追加 `is_deleted=0`——**但手写 SQL/XML 语句必须自己带上**，这是全仓库最容易漏的约定）。

**关键类（两个代表）**：
- `BlogPost`——文章主体：状态、计数列（views/like/comment/collect）、审计字段齐全，是「门面/查询/交互」三分架构围绕的核心实体；
- `AnonymousPostLike`——匿名点赞：`(post_id, visitor_hash)` 关联 + `is_deleted`，**没有独立计数列**——计数 = `post.likeCount` + 活跃匿名点赞数聚合，三态切换直接操作软删位。

表分两组：历史社区表（`sys_user`、`blog_post`、`blog_comment`、`blog_tag`、`blog_post_tag`、`blog_like`、`blog_collect`、`blog_share`、`blog_follow`、`blog_notification`、`blog_trending`、`blog_draft`、`blog_draft_tag`、`blog_report`、`blog_circle_*`、`blog_media`、`blog_post_media`、`blog_topic`、`blog_message`）与个人博客新增表（`site_setting`、`visitor_event`、`anonymous_post_like`、`guest_comment`、`photo_album`、`album_photo`）。

**什么时候你会来改这里**：新增业务字段时。改动要过**三库初始化策略**：SQLite 的 `schema-sqlite.sql`（`CREATE TABLE IF NOT EXISTS`，不迁移既有文件）、MySQL 的 `schema-fresh-install.sql`（只给全新空库）+ `migration-selfblog.sql`（已有库唯一通道）、H2 的测试 `schema.sql`——三处都要同步，缺一处 CI 就会替你发现。

### 1.9 event/ + listener/：领域事件（4 + 1 个文件）

**职责**：把「某事发生了」从「某事发生后要做什么」里解耦。事件是纯数据载体，监听器决定反应，发布方（service）不关心谁来听。

**关键类**：
- `CollectCreatedEvent` / `CommentCreatedEvent` / `FollowCreatedEvent` / `LikeCreatedEvent`——四个互动领域事件；
- `NotificationEventListener`——统一消费者：把互动事件转化为站内通知落库。加新监听器不需要动事件类，也不需要动发布方。

**什么时候你会来改这里**：想让某个互动触发新副作用（Webhook、统计、邮件）时——写一个新监听器订阅已有事件即可；若事件本身缺字段，才需要改 event 类并同步发布方。

### 1.10 mapper/：数据访问接口（28 个文件）

**职责**：MyBatis Plus 的 `BaseMapper<T>` 子接口层。28 个接口绝大多数**零方法**——通用 CRUD 由基类免费提供；少数带自定义方法，SQL 写在 `resources` 下的 Mapper XML。

**关键机制**：`MybatisPlusConfig` 注册了 `VendorDatabaseIdProvider`，XML 语句可用 `databaseId="mysql"` / `"sqlite"` 提供方言双版本；**H2 没有映射 id，只会走无 databaseId 的兜底语句**——所以 MySQL 专有语法（如 `ON DUPLICATE KEY UPDATE`）绝不能写进共享语句。

**什么时候你会来改这里**：需要复杂查询（多表关联、方言特有语法、统计聚合）时。纪律：单表操作交给 BaseMapper/QueryWrapper，进 XML 之前先问一句「QueryWrapper 真的表达不了吗」；写进 XML 的语句，SQLite 与 MySQL 各测一遍。

### 1.11 service/：业务层（全仓库的心脏，60+ 文件）

**职责**：所有业务语义的家。目录形态是「六子包 + impl + 两个 SEO 支撑子包」：`auth/`、`circle/`、`content/`、`post/`、`site/`、`social/` 各放接口，`impl/` 放实现，`site/seo/` 与 `site/page/` 放可读 HTML 与站点地图的支撑类。

**最重要的一套架构约定——「门面 / 查询 / 交互」三分**（Post 与 Circle 采用同一套拆分）：

| 角色 | 例子 | 职责 | 事务注解 |
|------|------|------|----------|
| 门面 Facade | `service/post/BlogPostService(Impl)` | CRUD、归属校验、跨服务编排；把查询/交互方法**一行委托**出去并重新标注事务注解 | 按操作读写标注 |
| 查询 Query | `PostQueryService(Impl)` | 列表/搜索/详情等只读逻辑 | `@Transactional(readOnly = true)`，`@Cacheable` 也在这里 |
| 交互 Interaction | `PostInteractionService(Impl)` | 计数与状态副作用（views/like/comment/collect） | `@Transactional(rollbackFor = Exception.class)` |

注意两个细节：门面实现与查询实现**都**继承同一个实体的 `ServiceImpl<XxxMapper, Entity>`（所以两者都能用基类的泛型方法）；新增功能必须放对位置——查询进 Query、副作用进 Interaction，**不要在门面里复制逻辑**。

**关键类（SEO 支撑，`service/site/seo/` 8 个 + `site/page/` 1 个）**：
- `ReadingPageService`——可读 HTML 的总装车间：把文章/页面数据交给模板渲染；
- `ReadingTemplate`——页面骨架模板（head/meta/JSON-LD/正文容器）；
- `ReadingContent` 与 `ReadingHtmlRenderer`——正文加工与最终 HTML 拼装（`SafeMarkdown` 白名单渲染发生在此链路上）；
- `SafeMarkdown`——Markdown→HTML 的**白名单安全渲染**，防存储型 XSS 的出库闸门；
- `PageCatalog`（在 `page/`）——公开页面目录与路由合法性判定；
- `SeoModels`——SEO 元数据模型（title/description/JSON-LD 结构）；
- `SitemapService`——站点地图：index + 每片 1000 条的分片；
- `SiteOrigin`——站点 origin 解析器，优先级：后台「网站设置 → 网站域名」(`siteBaseUrl`) → 环境变量 `SITE_BASE_URL` → 直连请求地址。配置后伪造 `Host`/`X-Forwarded-*` 不再影响 canonical/订阅/站点地图。

**什么时候你会来改这里**：任何业务行为的变化最终都落在这个包。判断落点的口诀：**只读进 Query、副作用进 Interaction、编排进 Facade、页面外观进 seo/**。改 `@Cacheable` 方法时同步想清楚对应的 `@CacheEvict` 在哪——缓存失效与缓存使用必须成对出现。

### 1.12 utils/：工具类（10 个文件）

**职责**：无业务语义的可复用能力。全仓库的「低层库」，被所有上层引用，自己只依赖框架。

**关键类**：
- `JwtUtils`——令牌中枢：jjwt 0.12.6 API（`Keys.hmacShaKeyFor` 构建密钥、`Jwts.parser().verifyWith(...).build().parseSignedClaims(...)` 校验）；`credentialVersionFor` 用 HmacSHA256 对域分离串 `selfblog:credential-version:v1\nuserId\npasswordHash` 计算凭证版本（只存 HMAC，不暴露 BCrypt 哈希）；内置 `ConcurrentHashMap` 令牌黑名单与 `CopyOnWriteArraySet` 设备令牌集；
- `SecurityUtils`——从安全上下文取人：`getCurrentUserId()`（未登录抛 401「请先登录」）、`getCurrentUserIdOrNull()`（游客返回 null，如详情页判断是否作者本人）、`isCurrentUserAdmin()`；principal 兼容 `UserContext` 或裸 `Long`；
- `HtmlSanitizer`——HTML 白名单消毒器，访客留言入库的第一道闸（`sanitizePlainText`）；
- `VisitorHashUtils`——访客标识加盐哈希（`hash(visitorId, hashSecret)`，secret 取 `JWT_SECRET`，缺省 `local-visitor-hash`）；
- `FineGrainedLockManager`——按资源键的细粒度锁管理；
- `StringMaskUtils`、`LogUtils`、`TimeUtils`——脱敏、日志格式、时间处理；
- `UserContext` / `UserConverter`——用户上下文载体与 entity↔vo 转换。

**什么时候你会来改这里**：想写新工具方法时，先来这里找——大概率已有（或应扩展现有类而不是新建平行类）。工具类纪律：无状态、不依赖业务包、必须有单测。

### 1.13 vo/：出参对象（37 个文件）

**职责**：HTTP 响应的形状。按域分包：`post/`(12)、`site/`(8)、`content/`(5)、`social/`(5)、`user/`(4)、`circle/`(3)。与 dto 的分界同前：dto 进、vo 出，entity 永远不直接出 API。

**什么时候你会来改这里**：前端需要新展示字段时。顺序是「vo 加字段 → service 填充 → 前端类型（`frontend/src/types/`）同步」——三处一体，只改 vo 不改前端类型是接口腐化的起点。

## 2. 五条后端主题阅读路线

按「任务」组织代码阅读：每条路线给出发顺序、每步看什么、为什么按这个顺序。文件路径省略前缀 `backend/src/main/java/com/selfblog/`。

### 路线一：认证链路（读懂「谁在请求」）

1. **`controller/auth/AuthController`**——入口长什么样：登录/登出/刷新三个端点，入参 dto、出参信封。先看接口面再下钻，知道终点才能理解中途每站。
2. **`utils/JwtUtils`**——令牌的生与死：`Keys.hmacShaKeyFor` 用 `JWT_SECRET` 构建 HMAC 密钥；签发时嵌入 userId、过期时间与 `credentialVersion`（`credentialVersionFor` 的域分离串设计让改密即全设备失效）；校验走 `Jwts.parser().verifyWith(...).build().parseSignedClaims(...)`；黑名单与设备集都在这里。**为什么第二步看它**：token 是整条链路的通货，先懂通货再懂流通。
3. **`config/security/JwtAuthenticationFilter`**——过滤器的一行职责：**只解析不拒绝**。有合法 token 就解析出用户、构造认证信息放进 `SecurityContextHolder`；没有/无效则放行为匿名。拒绝决策不在这里做——这行纪律是理解下一步的前提。
4. **`config/security/SecurityConfig`**——规则的总装配：`authorizeHttpRequests` 定义路径级授权（`/api/admin/**` 要 `hasRole("admin")`，公开接口逐条放行）；`STATELESS` 会话策略；CSRF 关闭的注释论证（JWT 走 `Authorization` 头，跨站页面拿不到该头）；CORS 拒绝通配符结尾 origin；`X-Frame-Options` 等安全头；401/403 的 JSON 入口（未认证/未授权返回结构化错误而非默认白页）；BCrypt 强度 12；`app.single-author-mode` 兜底。
5. **`utils/SecurityUtils`**——业务代码怎么取人：`getCurrentUserId()` 抛 401、`getCurrentUserIdOrNull()` 返回 null、`isCurrentUserAdmin()`；principal 兼容两种形态。**为什么第四步之后**：身份进入上下文之后，业务层才有「人」可取——这站是认证链路的终点、所有业务路线的起点。
6. **方法级授权**——`@PreAuthorize` 与 `aspect/AuthAspect`（消费 `@RequiresAuth`/`@RequiresAdmin` 注解），以及 single-author-mode 兜底如何确保「即使 URL 层漏配，写入类操作仍要求 admin」。

自测题：为什么 `JwtAuthenticationFilter` 不直接拒绝无效 token？（答：拒绝权统一留给授权层，过滤器只建立身份，职责单一便于测试与复用。）为什么改密码能让旧 token 失效？（答：token 载荷里的 credentialVersion 与库中 `credentialVersionFor(userId)` 不再一致。）

### 路线二：发布文章链路（读懂「三分架构」）

1. **`controller/admin/AdminPostController`**——入口：文章 CRUD 端点群，注意每个方法都薄到只有「接参、调 service、包信封」。
2. **`service/post/BlogPostService(Impl)`**——门面：CRUD、归属校验（「这篇文章是你的吗」）、跨服务编排；查询/交互方法在这里是**一行委托**并重标事务注解。看懂「一行委托」的形态，就懂了门面模式的本仓库用法。
3. **`service/post/PostQueryService(Impl)`**——查询实现：`@Transactional(readOnly = true)` + `@Cacheable`；列表、搜索、详情的只读逻辑。**为什么第三步**：先看读侧，因为缓存、分页、信封六件套都在读侧最集中。
4. **`service/post/PostInteractionService(Impl)`**——交互实现：views/like/comment/collect 计数副作用，`@Transactional(rollbackFor = Exception.class)`。对照第 3 步，体会「读与写分开标注事务」的理由。
5. **`mapper/BlogPostMapper` + `entity/BlogPost`**——落到底：BaseMapper 提供了什么、自定义 XML 有什么、实体字段与表列的映射（`@TableLogic isDeleted` 在此可见）。

自测题：为什么门面与查询实现都继承 `ServiceImpl<BlogPostMapper, BlogPost>`？（答：两者都需要基类泛型方法，门面额外做编排；继承同一基类避免工具方法重复。）新增「文章导出」功能该放哪？（答：只读导出进 `PostQueryService`，门面加委托方法。）

### 路线三：访客互动链路（读懂「消毒、哈希与幂等」）

1. **`controller/site/GuestCommentController`**——实测仅 31 行：留言创建与管理员查看两个端点，`@RequestMapping("/guest-comments")`。**先看它有多薄**：controller 的克制是后端能保持整洁的第一原因。
2. **`service/social/GuestCommentServiceImpl`**（约 149 行，全仓库业务密度的典范）——四个必须看清的细节：
   - **先消毒后校验**：`htmlSanitizer.sanitizePlainText(...)` 打头，再校验昵称 ≤80、内容 ≤2000——消毒先行，恶意载荷活不到校验层；
   - **visitorId 正则** `[A-Za-z0-9_-]{8,80}`：格式不合格直接拒，绝不进入哈希环节；
   - **加盐哈希只存 hash**：`VisitorHashUtils.hash(visitorId, hashSecret)`，库中无明文 visitorId；
   - **`selectBatchIds` 防 N+1**：管理员评论列表先查分页、再一次批量取全部关联用户；
   - 错误语义：`ensureCommentsEnabled` 403、`requirePublishedPost` 404、`normalizeSize` 把分页钳到上限 100。
3. **`controller/site/AnonymousLikeController` + `service/social/AnonymousLikeServiceImpl`**——幂等的教科书：`toggle` 是 `synchronized` 的**三态切换**（无记录 → insert liked=true；`is_deleted=0` → 置 1、liked=false；`=1` → 置 0、liked=true）；计数 = `post.likeCount` + 活跃匿名点赞数聚合。**为什么把它放在最后**：前两步看懂了「身份如何匿名化」，这一步才轮到「匿名身份如何并发安全」。

自测题：为什么 visitorId 要先过正则再哈希？（答：正则约束格式，哈希保证不可逆；顺序颠倒会把垃圾输入引入哈希环节。）并发双击匿名点赞会插两条记录吗？（答：不会——`synchronized` 把「检查→行动」收进同一把实例锁。）

### 路线四：SEO 与页面（读懂「同一内容，两种出口」）

1. **`controller/SpaController`**——SPA 路由的服务端回应者：公开历史路由（`/`、`/about`、`/archives`、`/gallery`、`/post/{id}`、`/tag/{id}`、`/pages/{slug}`）返回**可直接阅读的 HTML**（Vue 挂载后替换 `#app` 内的 `.seo-reading`）；状态码真实：缺失 404、`/index.html` 308、模板缺失 503；后台命名空间与历史账户路由返回启动壳并强制 noindex。它**不再**统一回退空壳 index.html——这个「曾经空壳、现在直出」的演变是理解本仓库 SEO 策略的钥匙。
2. **`service/site/seo/ReadingPageService`**——总装车间：数据如何变成完整 HTML（模板 → 内容 → 渲染器 → `SafeMarkdown` 白名单）。**为什么第二步**：SpaController 只是把活儿派出去，真正的组装在 service。
3. **`controller/site/SeoController`**——刻意的 `@Controller`（非 `@RestController`）：以此逃过 `WebMvcConfig` 只给 `@RestController` 加的 `/api` 前缀，在**根路径**提供 `/feed.xml`、`/atom.xml`、`/sitemap*.xml`、`/robots.txt`；站点地图是 index + 每片 1000 条的分片；robots 按实际 `adminPath` 生成（后台地址不泄漏给非必要爬虫规则）。
4. **`service/site/seo/SiteOrigin`**——origin 决定 canonical/feed/sitemap 里的绝对 URL；解析优先级 `siteBaseUrl` → `SITE_BASE_URL` → 请求地址，配置后伪造 `Host`/`X-Forwarded-*` 无效。

自测题：为什么 SeoController 不用 `@RestController`？（答：`addPathPrefix` 的谓词是 `forAnnotation(RestController.class)`，换 `@Controller` 才能落在根路径。）模板缺失为什么是 503 而不是 404？（答：404 表示「资源不存在」，503 表示「服务端暂时组装不了」——语义必须区分。）

### 路线五：基础设施（读懂「请求进屋前经过的五道门」）

1. **`config/web/WebMvcConfig`**——`configurePathMatch` 的 `addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class))`：全仓库 URL 的 `/api` 来源。**第一道门**：理解了它，源码里「找不到 /api」的困惑即消失。
2. **`config/db/DatabaseConfig`**——数据源选择：`@ConditionalOnProperty(name = "DB_TYPE")`；SQLite 默认（`jdbc:sqlite:campus_blog.db`、Hikari 池 1、自动执行 `schema-sqlite.sql`）；MySQL 走连接五件套；缺省走测试 H2。
3. **`config/cache/CaffeineCacheConfig`**——8 个具名缓存与各自 TTL（`hotTagsCache` 10m / `categoryCache` 30m / `userCache` 10m / `trendingCache` 5m / `statsCache` 15m / `statusCache` 2m / `siteSettingsCache` 10m / `visitorSummaryCache` 30s）。
4. **`config/web/RateLimitInterceptor`**——固定窗口限流：Caffeine 计数（1 分钟窗、`maximumSize(50000)`）、最长前缀匹配 `RateLimitProperties` 阈值、键 `ip:path`、超限 429 JSON；单实例实现，多实例需 Redis（注释明言）。
5. **`common/GlobalExceptionHandler`**——异常到 HTTP 的翻译官：`@RestControllerAdvice` 捕获 `BusinessException`（业务 code → HTTP 状态）、校验异常（400）、兜底 500。
6. **`common/result/Result`**——终点站：信封结构 `{code, message, data, timestamp}` 与分页六件套（`records/total/size/current/pages`）。

自测题：一个 GET 请求从进 Tomcat 到返回 JSON，按顺序经过哪些基础设施组件？（答：安全过滤器链 → 限流拦截器 → 路由前缀匹配 → controller → service（缓存/事务）→ mapper → GlobalExceptionHandler（若异常）→ Result 信封。）
## 3. 前端导读：frontend/src/

前端是 Vue 3.5.44 + Vite 6 + TypeScript 6.0.3 + Pinia 的标准 SPA。`src/` 下十个一级目录，各有明确分工。与后端同理，每目录给「职责 + 什么时候改」。

### 3.1 api/（15 个文件）

**职责**：后端通信的唯一出口。`index.ts` 是地基：创建 Axios 实例（`baseURL: '/api'`、`timeout: 15000`）、请求拦截器注入 `Authorization`/`X-XSRF-TOKEN`/`X-Visitor-Id`、响应拦截器**直接返回后端信封**（`api.get<T>()` 解析出的就是 `{code, message, data}`，调用方读 `res.data` 而不是 `res.data.data`）、`isRefreshing + failedQueue + processQueue` 的刷新排队重放、`RETRYABLE_CODES` 重试与 `error.userMessage` 文案、`handleAuthError()` 清理 Pinia/localStorage 并跳回 `adminRoot`。其余 14 个文件（`admin/`、`gallery/`、`media/`、`message/`、`notification/`、`pages/`、`post/`、`public-interactions/`、`share/`、`site/`、`tag/`、`trending/`、`user/`）按业务域封装端点函数。

**什么时候改**：每接一个新后端接口都在这里加封装函数——组件**不直接** import axios。注意 `baseURL` 硬编码 `/api`，`VITE_API_BASE_URL` 实际没有任何地方读取，跨域部署靠 Vite 代理与 `vercel.json` 重写解决。

### 3.2 components/（5 组：admin/common/layout/pages/post）

**职责**：可复用组件。`layout/` 是全局骨架（导航、页脚、`.app-main` 容器）；`admin/` 后台专用件；`post/` 文章展示件（含 `PostDetail` 的锚点布局，依赖 `html { scroll-padding-top }`/`scroll-margin-top`，改动前先确认锚点没被推挤）；`pages/` 自定义页面渲染件；`common/` 通用小组件。

**什么时候改**：两处以上视图需要同一段 UI 时才提升到这里；只服务一个视图的代码留在 `views/` 本地。

### 3.3 composables/（8 个）

**职责**：组合式函数——跨组件的逻辑复用层。`useConfirm`/`useDialog`（对话框；`useConfirm()` 返回的 `ConfirmDialog` 必须渲染进模板，否则确认框不出现）、`useMarkdownEnhance`（marked 增强，`main.ts` 里 `marked.setOptions({gfm:true, breaks:true})` 与之配套）、`usePageConfig`、`usePageSeo`、`useSeoMeta`（统一 document.meta）、`useScrollReveal`（滚动进入动画）、`useScrollState`。

**什么时候改**：一段带响应式状态/生命周期的逻辑被第二个组件需要时。与 utils 的分界：composable 内部用 `ref/computed/onUnmounted`，utils 是纯函数。

### 3.4 router/（index、guards、helpers + modules/ 六个模块）

**职责**：路由装配。`modules/` 下六个模块（home/post/discover/user/auth/admin）组装路由表；`helpers.ts` 封装 `lazyLoad`（`component: () => import(...)` 懒加载 + `ErrorFallback` 兜底）；`guards.ts` 是全局守卫；`index.ts` 创建 router 实例。**核心机制**：后台路由在运行时按服务端下发的 `adminPath` **动态注册**（`modules/admin.ts` 的 `createAdminRoutes`），不在首次导航前装好就会 404——这就是启动链路里 `installAdminRoutes` 必须先于 `use(router)` 的原因。

**什么时候改**：新增页面视图、调整公开路由、或改守卫规则时。铁律：组件里需要登录跳转统一 `router.push('/login')`，不要写死后台路径；`views/auth/` 下只剩重定向（`/login` → `adminRoot`）。

### 3.5 stores/（5 个）

**职责**：Pinia 全局状态。`site.ts`（站点设置、`adminPath`/`adminRoot`、皮肤与 favicon）；`theme.ts`（`localStorage['edu_theme']`、`.dark-mode` 类、`matchMedia` 监听系统主题）；`user.ts`（**store id 是 `'auth'`，导出别名 `useUserStore`**；token 经 `utils/crypto` 编解码存取；`validateToken` 只解 JWT 不请求网络）；`music.ts`（唯一音频宿主的状态）；`pages.ts`（**只保存已发布页面配置，草稿绝不进公开 store**）。另有启动期的特殊角色：SPA 在没有后端时也能靠 `site.ts` 的默认值正常渲染（`loadPublicSettings` 的 fetch 失败被静默吞掉），所以只跑 dev server 就能验收布局。

**什么时候改**：跨视图共享的新状态出现时。纪律：服务端可配置的东西先进 `site.ts`；本地偏好进 `theme.ts` 这类 localStorage 型 store。

### 3.6 styles/（14 个 css 文件）

**职责**：设计系统。`main.css` 只是 `@import` 入口；token 在 `variables.css`（颜色、`--radius-*` 圆角、`--gray-*` 灰度梯度）；暗色覆盖在 `dark-mode.css`（整体反转灰度梯度：`--gray-900` 由 `#111111` 变 `#EBEBEB`——**所以不要拿 `--gray-*` 当「深色表面」**，需要永远深色用语义 token：代码块 `--code-bg/--code-text/--code-border`、高对比按钮 `--contrast-bg/--contrast-bg-hover/--contrast-bg-active/--contrast-fg`）；皮肤在 `skins.css`（`#selfblog[data-skin=…]`，`ocean` 默认）。

**什么时候改**：调视觉时。三不原则：不写裸圆角（用 `--radius-*`）、不拿 `--gray-*` 当深色面、不动 `.app-main` 的 `padding-top` 与锚点相关属性除非通盘确认。

### 3.7 types/（11 个文件）

**职责**：后端契约的 TypeScript 投影（entity/vo 的前端镜像）与共享类型。**什么时候改**：后端 vo/dto 变化时同步——这是「三处一体」原则的第三处（vo → service → 前端 types）。

### 3.8 utils/（8 个文件）

**职责**：纯函数工具。`crypto`（token 的编解码存取——注意它是编码混淆不是加密）、`visitor`（访客 ID 生成与持久化）、`pageCatalog`、`reading`、`navigationFit`、`toast`、`logger`、`index`。

**什么时候改**：出现第二个使用者之前不新建；与 composables 的分界见 3.3。

### 3.9 views/（9 组）与 test/

**职责**：`views/` 按路由组织页面（admin/auth/common/gallery/pages/post/search/tag/user）。历史社区页（`views/user/*`）仍在仓库且路由注册，但已无公开入口，改动前先确认路径是否仍被引用；`Register.vue`、`circle/*` 等死代码已于 2026-10-17 删除。`test/` 下 `setup.ts` 是全局测试夹具：只桩了 `matchMedia` 与 `$router/$route`，**没有自动卸载**——挂载会注册全局监听器的组件，测试里要自己 `enableAutoUnmount(afterEach)`。

**什么时候改**：新增页面 = views 新文件 + router 模块注册 + （公开页）types/api 层配套。

## 4. 三条前端阅读路线

### 路线六：启动链路（`src/main.ts`，顺序是承重的）

1. **顶部副作用**——`marked.setOptions({ gfm: true, breaks: true })`：全局 Markdown 渲染基调在此定下，`useMarkdownEnhance` 依赖它。
2. **`loadPublicSettings()`**——拉公开站点设置填充 `site.ts`（失败静默吞掉，保证无后端也能渲染）。
3. **`installAdminRoutes(siteStore.adminPath)`**——按服务端下发的后台地址**动态注册后台路由**。必须在导航前完成，否则后台 404。
4. **`useUserStore().restoreFromStorage()`**——从 localStorage 恢复登录态。必须在 `use(router)` 之前，否则首个 `beforeEach` 会把已登录作者弹回登录页。
5. **`app.use(router)`** ——路由就绪。
6. **`mount('#app')`** ——挂载。
7. **`void usePagesStore(pinia).loadManifest()`**——挂载**之后**才异步拉公开页面配置摘要：它是可选的，失败不得阻塞启动。整条 bootstrap 包在 try/catch 里，异常时兜底仍要 mount。

为什么按此序读：这七步里每一对的先后都是 bug 教训换来的——先设置后路由（adminPath 才有值）、先恢复态后守卫（首跳不被误踢）、先挂载后可选增强（失败不阻塞）。读懂启动顺序，就读懂了「前端为什么不能随意调换初始化代码」。

### 路线七：请求链路（组件 → api 层 → 拦截器 → 信封）

1. **组件层**——调用 `api/` 域文件里的封装函数，拿到的直接是信封对象：`const res = await getPost(id); if (res.code === 0) use(res.data)`。
2. **api/ 域文件**——端点语义封装（URL、参数、类型标注），保持组件里不出现裸路径字符串。
3. **`api/index.ts` 请求拦截器**——注入三件套：`Authorization`（登录 token）、`X-XSRF-TOKEN`（32 字节 hex，双提交 CSRF 防线）、`X-Visitor-Id`（匿名身份）；日志里的 token 一律先做指纹脱敏（首尾各 20 字符 btoa）。
4. **响应拦截器**——正常路径直接解包信封返回；`401` 场景进入刷新协议：`isRefreshing` 标志 + `failedQueue` 队列 + `processQueue` 重放——并发多个请求只触发一次刷新，其余排队等新 token；刷新失败则 `handleAuthError()` 清理状态跳 `adminRoot`。可重试错误按 `RETRYABLE_CODES` 判定（`ERR_CANCELED` 等排除）。
5. **错误语义**——非 0 code 不抛异常而是正常返回信封，由调用方按业务处理；网络层错误带 `error.userMessage` 文案。

为什么按此序读：先看组件的「假象」（以为在拿 AxiosResponse），再看拦截器（实际返回信封），落差即本仓库最著名的契约——`res.data` vs `res.data.data` 的混淆全部源于此。

### 路线八：路由链路（modules → guards → 动态 admin）

1. **`router/modules/`**——六个模块按域拆分路由表；所有组件经 `helpers.ts` 的 `lazyLoad` 懒加载（失败有 `ErrorFallback`）。
2. **`router/guards.ts`**——全局守卫：`requiresAuth`/`requiresAdmin` 元信息不满足时跳 `siteStore.adminRoot` 并带 `redirect` 查询参数（登录后回跳）。守卫判断依赖第 4 步已恢复的登录态。
3. **动态 admin 路由**——`modules/admin.ts` 的 `createAdminRoutes(adminPath)` 在启动时被 `installAdminRoutes` 调用：后台路由的**路径本身**来自服务端设置，前端按需注册——这使后台地址可配置且不硬编码。

为什么按此序读：modules 是「静态面」，guards 是「规则面」，动态注册是「运行时面」——三层叠加才是完整的导航图；只看 modules 会完全漏掉后台路由从哪来。

## 5. 「改功能 → 动哪些文件」对照表

| 你要做的改动 | 后端文件 | 前端/配套 | 注意事项 |
|---|---|---|---|
| 加新站点设置键 | `SiteSettingsServiceImpl` 登记默认值+校验（**零 schema 迁移**，KV 表） | `stores/site.ts` + 设置页 | 记得配对 `siteSettingsCache` 的失效 |
| 新增公开接口 | controller 新方法 → `SecurityConfig` 显式放行 | `api/` 封装 | 检查 `RateLimitProperties` 前缀覆盖，不在则补阈值 |
| 新增管理员接口 | `controller/admin/` 新类/方法 | `api/admin/` + 后台视图 | 双重保护自动获得；测试须 `@MockBean JwtUtils` + `@WithMockUser(roles="admin")` |
| 新增实体/表 | `entity/` + `mapper/` + **三库 schema**（`schema-sqlite.sql`、`schema-fresh-install.sql`+`migration-selfblog.sql`、测试 `schema.sql`） | `types/` | 手写 SQL 别忘 `is_deleted=0` |
| 文章新增字段 | `BlogPost` + 三库 + dto + vo + Query/Interaction service | `types/` + 视图 | 三处一体：vo→service→types 同步 |
| 新增缓存 | `CaffeineCacheConfig` 登记具名缓存+TTL | — | `@Cacheable` 与 `@CacheEvict` 必须成对出现 |
| 新增限流前缀 | `RateLimitProperties` 补阈值 | — | 键为 `ip:path`，窗口 1 分钟 |
| 新增事件副作用 | `event/` 新事件类 + 发布方 + `listener/` 新监听器 | — | 不改事件类即可加监听者 |
| 改后台地址 | 无需动后端安全（只认 `/api/admin/**`） | 站点设置 `adminPath`（`normalizeAdminPath` 约束）+ 启动时动态注册 | 保留词拒绝；robots 按实际值生成 |
| 改站点域名/origin | 无需动代码 | 后台「网站设置 → 网站域名」(`siteBaseUrl`) | `SiteOrigin` 自动接管 canonical/feed/sitemap，伪造头失效 |
| 新增公开页面 | `PageCatalog` + `PublicPageController` + `SpaController` 路由 + seo/ 组装 | `views/pages/` + `router/modules/` | 草稿不进公开 store；缺失 404、模板缺失 503 |
| 调整视觉/皮肤 | — | `variables.css`（token）/ `skins.css`（`data-skin`）/ `dark-mode.css` | 不写裸圆角；`--gray-*` 不是深色面 |
| 新增组合式函数 | — | `composables/` + 组件 + 测试 | 全局监听要在 `onUnmounted` 清理，测试配 `enableAutoUnmount` |
| 新增依赖 | `pom.xml`（留心 `frontend-maven-plugin` 与 skip 开关） | `package.json`（`npm ci` 后跑 type-check 防幽灵依赖） | 版本钉死，文档版本号以 manifest 为准 |
| 方言特有 SQL | Mapper XML `databaseId="mysql"/"sqlite"` 双版本 | — | H2 只走无 databaseId 兜底语句，别把专有语法写进共享语句 |
| 全新 SEO 出口 | `SeoController`（保持 `@Controller` 逃 `/api`） | — | URL 集中在根路径；robots/sitemap 由 `SitemapService` 分片 |

---

# 第二部分：全栈术语表

用法说明：术语按**主题分组**（组内按字母/拼音排序），每条给出定义与「在 SelfBlog 中」的实例位置——概念只有落到文件上才真正属于你。条目互相引用时用「→ 见 xxx」。想按字母快速定位，翻本部分末尾的「按字母索引」。

---

## 6. 配置文件导读

| 文件 | 关键内容 |
|------|----------|
| `backend/src/main/resources/application.yml` | logic-delete-field、mybatis-plus 配置、app.single-author-mode |
| `backend/src/test/resources/application.yml` | 测试 profile：H2 数据源、缓存关闭 |
| `backend/.env.example` | JWT_SECRET(≥32)/ADMIN_PASSWORD(≥12)/ADMIN_PATH 模板 |
| `frontend/vite.config.ts` | server.proxy：/api、/uploads → VITE_API_TARGET(默认 8825) |
| `frontend/tsconfig.json` | strict、noUncheckedIndexedAccess、paths 别名 |
| `frontend/.prettierrc` | endOfLine:"auto"（跨平台稳定的历史教训） |
| `.github/workflows/ci.yml` | 四 job：backend/frontend/mysql/docker |
| `frontend/vercel.json` | framework:null + build 脚本生成代理 |

---

## 组一：Java 语言

**Annotation Processing（注解处理）**：编译期读取注解并生成/修改代码的机制，与运行期反射读取注解是两条不同通道。Lombok 是最常见的注解处理器。
在 SelfBlog 中：`pom.xml` 引入 Lombok（约 line 101/282/306 三处），`@Data`、`@Slf4j` 等在编译期生成 getter/setter 与日志字段——所以实体类源码里看不到这些方法，IDE 需装 Lombok 插件。

**Auto-boxing / Unboxing（自动装箱/拆箱）**：基本类型与包装类型自动互转。代价是装箱产生对象、拆箱可能 NPE。在 SelfBlog 中：实体计数列用 `Integer likeCount` 而非 `int`，因为数据库列可能为 NULL——`int` 会在拆箱时抛空指针。

**Class Loading（类加载）**：JVM 把 `.class` 装入方法区并链接初始化的过程，按需（首次主动引用）触发，双亲委派保证核心类不被篡改。在 SelfBlog 中：Spring Boot 的 `LaunchedURLClassLoader` 打包后从 `selfblog.jar` 内嵌 jar 里加载类，这就是 fat jar 能直接 `java -jar` 的原因。

**Enum（枚举）**：类型安全的常量集合，每个实例单例、可携带字段与方法。在 SelfBlog 中：`common/enums/` 下 8 个枚举定义文章状态、互动类型等业务字面量，service 与 mapper 统一引用，避免魔法数字散落。

**Exception Hierarchy（异常体系）**：`Throwable` 分 `Error`（不接）与 `Exception`；后者分受检（必须声明）与非受检 `RuntimeException`（业务异常的主流选择）。在 SelfBlog 中：`common/exception/BusinessException extends RuntimeException`——不受检，service 层随手抛、`GlobalExceptionHandler` 统一接，避免层层 throws 污染签名。

**Generics（泛型）**：编译期类型参数化，擦除后运行期无类型信息（Type Erasure）。在 SelfBlog 中：`Result<T>` 信封、`BaseMapper<T>`、`ServiceImpl<XxxMapper, Entity>` 全靠泛型把「类型正确」从运行时挪到编译期。

**Immutable（不可变对象）**：创建后状态不可改，天然线程安全，可放心共享。在 SelfBlog 中：jjwt 的 `SecretKey` 构建一次全局复用；事件类（`LikeCreatedEvent` 等）按不可变风格设计，字段构造后不再变更。

**Interface Default Method（接口默认方法）**：Java 8 起接口可带方法体，让接口演进不破坏实现类。在 SelfBlog 中：MyBatis Plus 的 `BaseMapper` 与 Spring Data 风格接口大量依赖 default method 提供开箱方法。

**Lambda & Functional Interface（Lambda 与函数式接口）**：`@FunctionalInterface` 单抽象方法接口可用 lambda 实例化，`Function/Supplier/Predicate/Consumer` 是四大内置。在 SelfBlog 中：Stream 流水线里的方法引用（如排序比较器、`map` 转换）贯穿 service 查询实现；`@Transactional` 底层事务模板同样以回调形式传递逻辑。

**Lombok**：注解处理器，编译期生成样板代码（getter/setter/构造器/builder/logger），不是运行时反射。在 SelfBlog 中：所有 dto/vo/entity 几乎都用 `@Data`；日志统一 `@Slf4j` 生成 `log` 字段，替代手工 `LoggerFactory.getLogger`。

**Record（记录类）**：Java 16+ 的不可变数据载体，一行声明字段、equals/hashCode/toString 全生成。在 SelfBlog 中：适合替换「只读出参」场景——`service/site/seo/` 的 `SeoModels` 里的 SEO 元数据对（title/description/JSON-LD 字符串）即按 record/紧凑载体风格组织。

**Reflection（反射）**：运行期检查与操作类、方法、字段的元编程能力，是框架「按名字装配」的根基，但有性能与封装破坏代价。在 SelfBlog 中：Spring 依注解（`@Service`、`@RequestMapping`）反射发现 Bean 与路由；MyBatis Plus 反射读取实体字段生成 SQL 列名——你写的 `@TableName("blog_post")` 就是给反射看的说明书。

**Sealed Class（封闭类）**：Java 17+ 用 `permits` 限定继承树，编译器可据此做穷尽检查。在 SelfBlog 中：异常体系可用 sealed + pattern matching 表达「业务错误只有这几种」，当前仓库未使用，属可选演进。

**Stream API（流）**：声明式集合处理（filter/map/reduce/collect），惰性求值、一次性消费。在 SelfBlog 中：`GuestCommentServiceImpl` 批量查询后用 stream 组装 vo 列表；`SitemapService` 把文章列表流式转成 XML 条目。

**String Handling（字符串处理）**：不可变 `String`、可变 `StringBuilder`（单线程拼接首选）。在 SelfBlog 中：`ReadingHtmlRenderer` 拼可读 HTML 时用 StringBuilder；`utils/StringMaskUtils` 做 token 指纹脱敏（首尾各 20 字符 btoa）。

**Switch Expression（switch 表达式）**：Java 14+ 的箭头语法 switch，带返回值与穷尽性检查。在 SelfBlog 中：`DatabaseConfig` 的方言分支、`GlobalExceptionHandler` 的异常映射这类「一一对应」逻辑是它的理想场景。

**Text Block（文本块）**：Java 15+ 的 `"""` 多行字符串，保留缩进控制。在 SelfBlog 中：`JwtUtils` 的域分离串 `selfblog:credential-version:v1\nuserId\npasswordHash`、schema 片段这类常量是 text block 的典型用武之地。

**Var / Type Inference（局部类型推断）**：Java 10+ 的 `var`，仅局部变量，静态类型不变。在 SelfBlog 中：泛型流水的中间变量（`var page = postMapper.selectPage(...)`）可提升可读性，但字段与公共 API 签名禁用。

## 组二：JVM 与并发

**Atomic Classes（原子类）**：`AtomicInteger/AtomicLong/AtomicReference` 基于 CAS 提供无锁原子操作。在 SelfBlog 中：`AnonymousLikeServiceImpl` 的计数与 `JwtUtils` 黑名单容量统计可用原子类承载——实际仓库选择了 `ConcurrentHashMap`/`CopyOnWriteArraySet`，因为「容器级原子性」已足够。

**Busy Waiting / Spin（自旋等待）**：循环检查条件而不让出 CPU，短临界区可减少上下文切换，长等待纯烧 CPU。在 SelfBlog 中：不使用——同步点（如 `synchronized toggle`）都交给 JVM 阻塞策略，自旋只见于 JVM 内部实现。

**CAS（Compare-And-Swap，比较并交换）**：CPU 原子指令，`expected==actual` 才写入，是无锁数据结构的地基；失败重试即「乐观并发」。在 SelfBlog 中：`ConcurrentHashMap`（令牌黑名单）内部就是 CAS + 链表/红黑树；你的代码只享受结果，不必手写。

**Context Switch（上下文切换）**：CPU 从一个线程切到另一个要保存/恢复寄存器，微秒级开销，高并发系统的主要隐性税。在 SelfBlog 中：`AsyncConfig` 线程池的核心/最大线程数就是「并行度 vs 切换成本」的权衡旋钮。

**CopyOnWrite（写时复制）**：读不加锁、写时复制整个底层数组，读多写少场景的廉价并发容器。在 SelfBlog 中：`JwtUtils` 的 `userDeviceTokens` 用 `CopyOnWriteArraySet`——每个用户的设备令牌集极少写、每次请求都可能读。

**Deadlock（死锁）**：多线程互相持锁等待，环状依赖导致永久阻塞。预防靠固定加锁顺序或超时。在 SelfBlog 中：`FineGrainedLockManager` 按资源键加锁时依赖「同一顺序获取」约定；单方法 `synchronized`（如 `toggle`）天然无死锁面。

**Executor Framework（执行器框架）**：`ExecutorService` 把任务提交与执行策略解耦，`ThreadPoolExecutor` 参数（核心数、队列、拒绝策略）决定系统行为。在 SelfBlog 中：`config/AsyncConfig` 声明应用线程池，`@Async` 方法与事件监听共享该池，避免默认 ForkJoinPool 的不可控。

**Fairness（公平锁）**：锁是否按等待顺序授予。公平锁无饥饿但吞吐低，`ReentrantLock(true)` 可选。在 SelfBlog 中：未使用——留言/点赞的锁粒度小（单条切换），吞吐优先。

**Happens-Before（先行发生原则）**：JMM 的可见性契约：unlock 先于后续 lock、`volatile` 写先于读、线程启动先于其内动作。理解它才不会「明明改了值另一个线程看不见」。在 SelfBlog 中：`RateLimitInterceptor` 的 Caffeine 计数器跨请求可见，正是 Caffeine 内部 happens-before 链的功劳。

**Immutable Publication（不可变发布）**：安全发布的最简方式——对象不可变则任何发布方式（哪怕没同步）都安全。在 SelfBlog 中：`SecretKey`、`RateLimitProperties` 等启动期构建后只读的 Bean 都按此模式，无需 volatile。

**JIT（Just-In-Time 编译）**：热点字节码运行期编译为本地码，分层编译（C1→C2）+ 方法内联是性能来源。在 SelfBlog 中：启动后几分钟内接口变快就是 JIT 热身；压测要预热，不能拿第一笔请求计时。

**JMM（Java Memory Model，Java 内存模型）**：定义多线程下读写共享内存的合法行为：可见性、有序性、`volatile`/`synchronized`/`final` 的语义。在 SelfBlog 中：`synchronized toggle` 的正确性，建立在「进入监视器读到的值不早于上次释放前写入」这条 JMM 保证上。

**JUC（java.util.concurrent）**：Java 并发工具包总称：容器（`ConcurrentHashMap`）、同步器（`CountDownLatch`、`Semaphore`）、执行器、原子类。在 SelfBlog 中：`JwtUtils` 的黑名单 `ConcurrentHashMap`、设备集 `CopyOnWriteArraySet`、事件派发的 `ThreadPoolExecutor` 全部来自 JUC——业务代码几乎没有手写 `synchronized` 之外的锁。

**Memory Leak（内存泄漏）**：对象不再使用却仍被引用（静态集合、监听器、ThreadLocal 未清理），最终 OOM。在 SelfBlog 中：`CaffeineCacheConfig` 的每个缓存都设 `expireAfterWrite`/容量上限，就是为了防止「缓存当存储用」式的慢性泄漏。

**Monitor / Intrinsic Lock（监视器锁）**：每个对象自带一把锁与等待集，`synchronized` 即使用它；同时是 JMM 可见性的边界。在 SelfBlog 中：`AnonymousLikeServiceImpl.toggle` 的 `synchronized` 方法就是拿实例监视器——单实例下让三态切换（无→insert / 0→1 / 1→0）串行化，保证幂等。

**Optimistic vs Pessimistic Locking（乐观锁/悲观锁）**：乐观假设冲突少、提交时校验（版本号/CAS）；悲观先锁再操作（`SELECT FOR UPDATE`）。在 SelfBlog 中：MyBatis Plus 的 `@Version` 乐观锁插件对实体可用；当前计数更新走「数据库原子 UPDATE（likeCount = likeCount + 1）」，属于无显式锁的原子路径；`FineGrainedLockManager` 则是应用层悲观锁的示范。

**Race Condition（竞态条件）**：正确性依赖执行时序的缺陷，典型如「检查再行动」（check-then-act）不原子。在 SelfBlog 中：匿名点赞若不用 `synchronized`，「检查记录是否存在→插入」就是教科书竞态——并发双击会插两条；`toggle` 的三态切换把检查与行动收进同一把锁。

**ThreadLocal（线程局部变量）**：每线程独立副本，常用于上下文传递（如用户身份、事务资源），用完必须 `remove()` 防泄漏。在 SelfBlog 中：Spring 的 `RequestContextHolder`/`SecurityContextHolder` 底层就是 ThreadLocal，`SecurityUtils.getCurrentUserIdOrNull()` 才能在任意 service 层无参取人。

**Thread Pool Sizing（线程池调优）**：CPU 密集 ≈ 核数，IO 密集可放大数倍；队列要有界，拒绝策略要明确。在 SelfBlog 中：`AsyncConfig` 的池参数按「通知派发是 IO 轻负载」设定小池+有界队列；SQLite 场景下写入并发本就被 Hikari 池大小 1 串行化，池再大也无谓。

**Thread Safety（线程安全）**：类在并发调用下仍保持正确性。手段按代价递增：无状态 → 不可变 → 封闭 → 锁。在 SelfBlog 中：`VisitorHashUtils`（纯函数）与 `HtmlSanitizer`（无状态）天然安全；`AnonymousLikeServiceImpl` 选择「实例级 synchronized」这种最简单但吞吐最弱的方式——单实例博客流量下，正确性优先于扩展性是对的。

**Volatile（volatile 关键字）**：保证可见性与禁止指令重排，但不保证复合操作原子。适合状态标志，不适合计数。在 SelfBlog 中：登录失败计数、开关型标志可用 `volatile boolean`；真正的计数交给数据库原子 UPDATE 或 `AtomicLong`。

**Wait/Notify（等待通知机制）**：`Object.wait()/notifyAll()` 构建线程间协作，生产者-消费者是其原型。现代替代是 `Condition` 与 `BlockingQueue`。在 SelfBlog 中：未直接使用——事件派发用线程池消费，不需要手工等待/通知。

**Work Stealing（工作窃取）**：空闲线程从他人队列尾部取任务，ForkJoinPool 的核心思想，适合分治。在 SelfBlog 中：默认 `@Async` 未指定执行器时会落到 ForkJoinPool.commonPool——这正是 `AsyncConfig` 要显式定义线程池的原因：业务调度不该挤占框架公共池。

## 组三：Spring 生态

**@Autowired / Resource（依赖注入注解）**：`@Autowired` 按类型注入，多个候选时配 `@Qualifier`；`@Resource` 按名注入。在 SelfBlog 中：service 里注入 Mapper、controller 里注入 service 全用构造器注入（推荐形态），字段注入已避开。

**AOP（Aspect-Oriented Programming，面向切面编程）**：把横切关注点（日志、鉴权、事务）从业务逻辑抽离，以切面织入。Spring AOP 基于动态代理，只拦截 Spring Bean 的 public 方法调用。在 SelfBlog 中：`aspect/AuthAspect` 拦 `@RequiresAuth`/`@RequiresAdmin` 做鉴权、`PerformanceMonitoringAspect` 全局记录慢方法 `[PERF]` 日志；`@Transactional` 本身也是 AOP 织入的事务切面。

**ApplicationContext（应用上下文）**：Spring IoC 容器的高级形态，除装配外还提供事件发布、国际化、资源加载。在 SelfBlog 中：启动即创建，`SpringApplication.run` 返回的就是它；事件机制（`event/` 四个事件 + `NotificationEventListener`）由它派发。

**Aspect（切面）**：`@Aspect` 类 = 切点（哪里织入）+ 通知（织入什么）。在 SelfBlog 中：`PerformanceMonitoringAspect` 用环绕通知包住 service 方法计时。

**Bean Lifecycle（Bean 生命周期）**：实例化 → 属性填充 → Aware 回调 → `@PostConstruct` → 使用 → `@PreDestroy`。在 SelfBlog 中：`DotenvConfig` 在早期把 `.env` 装入 JVM property，后续所有 `@Value` 才能读到；`SecurityConfig` 拉起的 `JwtAuthenticationFilter` 必须拿到 `JwtUtils` Bean——切片测试里 `@MockBean JwtUtils` 就是补这个环节。

**Boot Starter（启动器）**：「依赖即功能」的打包单位，引 starter = 引依赖 + 自动配置。在 SelfBlog 中：`spring-boot-starter-web/security/validation/actuator` 等在 `pom.xml` 一字排开，引哪个哪个生效。

**CGLIB Proxy（CGLIB 代理）**：字节码生成子类实现代理，可代理无接口类，不能代理 final 类/方法。Spring 默认对无接口 Bean 用 CGLIB。在 SelfBlog 中：service 实现类（如 `GuestCommentServiceImpl`）若无接口也照样被 `@Transactional` 代理——就是 CGLIB 在背后生成子类拦截。

**Circular Reference（循环依赖）**：A 依赖 B、B 依赖 A。Spring 用三级缓存解单例 setter 循环，构造器循环无解直接报错。在 SelfBlog 中：门面模式（`BlogPostService` 委托 `PostQueryService/PostInteractionService`）是单向依赖，刻意避免循环——这也是分层清晰的隐藏收益。

**Classpath Scanning（类路径扫描）**：`@ComponentScan` 从启动类包根扫描带 stereotype 注解的类注册为 Bean。在 SelfBlog 中：启动类位于 `com.selfblog` 包根，所有子包组件自动被发现——新包建在包根之外就扫描不到。

**Component Scan（组件扫描）**：同上，`@Component` 家族（`@Service`/`@Repository`/`@Controller`/`@Configuration`）都会被扫。在 SelfBlog 中：`mapper/` 下的接口由 `@MapperScan` 单独扫描，不走组件扫描通道。

**Conditional Beans（条件装配）**：`@Conditional` 家族按运行时条件决定 Bean 是否创建（类存在、属性匹配、profile 激活）。在 SelfBlog 中：`DatabaseConfig` 的数据源 Bean 带 `@ConditionalOnProperty(name = "DB_TYPE")`——属性在，SQLite/MySQL 数据源才创建；属性缺省走测试 H2 默认链路。

**Configuration Properties（属性绑定）**：`@ConfigurationProperties` 把 `application.yml` 前缀批量绑定到强类型对象，支持校验与 IDE 提示。在 SelfBlog 中：`RateLimitProperties`（前缀→阈值表）、`SecurityProperties`（安全参数）、`app.single-author-mode`、`bcrypt.strength` 都走绑定，环境变量可覆盖（`APP_SINGLE_AUTHOR_MODE`）。

**Constructor Injection（构造器注入）**：依赖经构造器传入，可 final、易测试、暴露依赖数量。在 SelfBlog 中：全仓库统一构造器注入——看一个类要什么依赖，看构造器签名即可，`GuestCommentServiceImpl` 的构造器就是它的「依赖清单」。

**Dependency Injection（DI，依赖注入）**：对象不自己 new 依赖，由容器注入——控制反转的具体实现。在 SelfBlog 中：controller 注 service、service 注 mapper、`JwtAuthenticationFilter` 注 `JwtUtils`，全部由容器接线。

**Environment Abstraction（环境抽象）**：`Environment` 统一 property source（JVM 属性 > 环境变量 > yml > 默认值）。在 SelfBlog 中：`DotenvConfig` 把 `.env` 提升为 JVM property 抢占优先级，因此「.env 值优先于系统环境变量」。

**IoC（Inversion of Control，控制反转）**：对象创建与依赖装配的控制权从业务代码反转给容器，DI 是其主流实现。在 SelfBlog 中：你从未在业务代码里 `new GuestCommentServiceImpl()`——它由容器创建、注入它需要的 `HtmlSanitizer`/`VisitorHashUtils`，测试时才手工 new 或用 `@MockBean`。

**Jackson（JSON 序列化库）**：Spring Boot 默认 HTTP 消息转换器，注解控制字段命名与忽略。在 SelfBlog 中：vo 上的 `@JsonProperty`/`@JsonIgnore` 控制响应形状，`LocalDateTime` 的格式化由全局配置统一。

**JDBC Template（JDBC 模板）**：Spring 对裸 JDBC 的薄封装，消灭样板代码，保留 SQL 控制权。在 SelfBlog 中：绝大多数查询走 MyBatis Plus，`JdbcTemplate` 留给偶发脚本式查询与 schema 初始化（`DataInitializer` 引导管理员）。

**Profile（多环境切面）**：`@Profile("dev")`/`spring.profiles.active` 让 Bean/配置按环境生效。在 SelfBlog 中：`application-dev.yml`/`application-prod.yml` 分环境；测试 profile 关闭缓存；Knife4j 在 prod 关闭。

**Property Source（属性源）**：配置来源的有序叠加，Spring Boot 预置了 17+ 层优先级。在 SelfBlog 中：`.env`（经 DotenvConfig）→ JVM property → 环境变量 → `application-{profile}.yml` 的链条，解释了为什么同名的系统变量会被 `.env` 覆盖。

**Proxy Pattern（代理模式）**：为对象提供代理以控制访问；Spring AOP 与 `@Transactional` 的实现本质。在 SelfBlog 中：注入的 service 实际是代理对象——`@Cacheable` 命中时代理直接返回缓存，方法体根本不执行。**自注自调不经过代理**，事务与缓存会静默失效，这是同类方法内部调用的大坑。

**Repository（仓库模式）**：在领域与数据映射之间收拢持久化逻辑。在 SelfBlog 中：MyBatis Plus 的 `BaseMapper<T>` + `ServiceImpl` 承担仓库角色，service 直接持有 Mapper——轻量级仓库形态。

**Scheduled Tasks（定时任务）**：`@Scheduled` + `@EnableScheduling` 的方法级调度。在 SelfBlog 中：`config/security/JwtSchedulerConfig` 周期清理令牌黑名单过期项，防 `ConcurrentHashMap` 无界增长。

**Slice Testing（切片测试）**：只装配某一层的测试注解（`@WebMvcTest`/`@DataJpaTest`），比全容器快一个量级。在 SelfBlog 中：管理员控制器用 `@WebMvcTest`，必须 `@MockBean JwtUtils`（否则 SecurityConfig 拉起的 jwtAuthenticationFilter 缺 Bean 启动失败）+ `@WithMockUser(roles="admin")`。

**Spring Boot Auto-configuration（自动配置）**：`@EnableAutoConfiguration` 按 classpath 与已有 Bean 决定装配什么，「约定优于配置」的工程化极致。在 SelfBlog 中：引了 `spring-boot-starter-web` 就有 Tomcat+Jackson+MVC，引了 actuator 就有 `/actuator/health`——你的 `application.yml` 只写偏离约定的部分。

**Spring Bean（Bean）**：容器管理的对象，默认单例、启动期构建。在 SelfBlog 中：`SecretKey`、`PasswordEncoder`（BCrypt 强度 12）、8 个缓存等基础设施 Bean 在 `config/` 声明，业务 Bean 靠注解扫描。

**Spring Security Filter Chain（安全过滤器链）**：十几个过滤器串成链，每个请求依次通过（CSRF→CORS→认证→授权…）。在 SelfBlog 中：`SecurityConfig` 声明这条链：`JwtAuthenticationFilter` 挂在用户名密码过滤器之前，匿名放行到授权决策；`STATELESS` 全程不建会话。

**Starter Parent / BOM（依赖管理）**：parent POM 或 BOM 锁定依赖版本，子项目免写版本号。在 SelfBlog 中：`pom.xml` 的 `spring-boot-starter-parent` 3.3.13 托管全家族版本，jjwt 0.12.6 是例外——需显式声明版本。

**Transaction Propagation（事务传播）**：事务方法嵌套时新事务的归属规则：`REQUIRED`（默认，有则加入）、`REQUIRES_NEW`（挂起外层开新事务，常用于「独立成败」的日志/通知）、`NESTED`（保存点）。在 SelfBlog 中：门面委托方法重标事务注解就是显式选择传播行为；通知落库若要求「主流程失败也不回滚通知」，就该 `REQUIRES_NEW`。

**Validation（参数校验）**：JSR-380 注解（`@NotNull`/`@Size`/`@Pattern`）+ `@Validated` 触发。在 SelfBlog 中：dto 上的校验注解是第一道闸（格式），service 内语义校验是第二道（`visitorId` 正则、长度上限）；违反时 `GlobalExceptionHandler` 转 400。

**YAML Configuration（YAML 配置）**：缩进式配置格式，`application.yml` 优于 properties 的层级表达。在 SelfBlog 中：`application.yml` + `application-dev.yml`/`application-prod.yml`；注意 yml 里 `app.api-prefix: /api` 这类键经 relaxed binding 映射到 `@ConfigurationProperties` 字段。

## 组四：Web 与 HTTP

**404 / 503 Semantics（状态码语义）**：状态码是 API 的一部分，不是「能返回数据就行」。在 SelfBlog 中：`SpaController` 公开路由缺失 404、模板缺失 503、`/index.html` 308——错误都有真实语义，而非一律 200 包错误 JSON；`GlobalExceptionHandler` 把业务 code 映射到 HTTP 状态。

**Accept / Content Negotiation（内容协商）**：客户端用 `Accept` 头声明想要的媒体类型，服务端按能力返回。在 SelfBlog 中：`SeoController` 的 XML 端点按路径而非协商返回（`/feed.xml` 固定 `application/xml`），这是对爬虫最友好的简单方案。

**Base64**：二进制→文本的编码（不是加密！），3 字节一组映射 4 字符，末尾补 `=`。在 SelfBlog 中：JWT 三段皆 Base64URL；前端 token 指纹 = 首尾各 20 字符再 btoa 脱敏。

**Cache-Control / ETag（HTTP 缓存头）**：`max-age/no-cache/no-store` 与实体标签协商缓存。在 SelfBlog 中：SPA 启动壳对后台路由强制 `noindex`；静态资源由 Vite 产物文件名哈希天然支持长缓存；`SeoController` 的 XML 响应可加短 TTL。

**Content Security Policy（CSP，内容安全策略）**：响应头白名单化页面可加载的资源来源，XSS 的纵深防御。在 SelfBlog 中：`SecurityConfig` 设了 `X-Frame-Options: DENY`、`Referrer-Policy`、`Permissions-Policy`；CSP 可按同思路扩展（注意与 Vue 内联样式、marked 渲染 HTML 的兼容）。

**Cookie 属性（Cookie Attributes）**：`HttpOnly`（JS 不可读）、`Secure`、`SameSite`（防 CSRF 的关键属性）。在 SelfBlog 中：JWT 走 `Authorization` 头而非 Cookie，因此默认无会话 Cookie；XSRF token 走 `X-XSRF-TOKEN` 头是给「未来若引入 Cookie 场景」预留的双提交防线。

**CORS（跨源资源共享）**：服务器用 `Access-Control-*` 头显式授权跨源浏览器请求；预检 `OPTIONS` 先行。在 SelfBlog 中：`SecurityConfig` 的 CORS 配置**拒绝通配符结尾的 origin**（防 `evil.example` 匹配 `*.example` 级联放行），开发期前端同源代理 `/api` 根本不触发 CORS。

**CSRF（Cross-Site Request Forgery，跨站请求伪造）**：借浏览器自动携带 Cookie 的特性冒名提交。防御：SameSite、CSRF token、纯 token 认证。在 SelfBlog 中：JWT 走 `Authorization` 头，跨站页面拿不到该头，故 `csrf().disable()`——`SecurityConfig` 注释里有完整论证。

**Envelope Pattern（信封模式）**：响应外层包统一元数据（code/message），内层才是数据，让成功失败同构。在 SelfBlog 中：`Result<T>{code,message,data,timestamp}`；前端拦截器读信封、组件读 `res.data`。

**ETag / Last-Modified（条件请求）**：`If-None-Match`/`If-Modified-Since` 命中则 304，省带宽。在 SelfBlog 中：静态资源与 SEO XML 可用；API 动态数据不强求。

**Forward Proxy vs Reverse Proxy（正向/反向代理）**：正向代理替客户端出门，反向代理替服务器接客（TLS 终结、缓存、路由）。在 SelfBlog 中：开发期 Vite 代理 `/api`→8825 是反向代理；部署期 `frontend/vercel.json` 生成的 Build Output v3 forwarding 同理——`SELFBLOG_BACKEND_ORIGIN` 指向唯一后端。

**HMAC（Hash-based MAC）**：带密钥的哈希 MAC，验证「消息完整 + 发送方持有密钥」。在 SelfBlog 中：JWT 签名是 HMAC-SHA256（HS256）；`credentialVersionFor` 用 HmacSHA256 对域分离串 `selfblog:credential-version:v1\nuserId\npasswordHash` 计算，只存 HMAC 不暴露 BCrypt 哈希。

**HTTP Methods Semantics（方法语义）**：GET 安全幂等、PUT 幂等、POST 非幂等、DELETE 幂等。在 SelfBlog 中：点赞接口 POST `/like/toggle` 破坏了 POST 非幂等惯例——但服务端 `synchronized` 三态切换保证幂等效果，语义与实现各让一步。

**Idempotency（幂等性）**：同一操作执行多次与一次效果相同。支付、点赞类接口必须有界。在 SelfBlog 中：匿名点赞 `toggle` 的三态机（无→insert / 0→1 / 1→0）+ `synchronized` 保证并发下幂等；前端重试机制（`RETRYABLE_CODES`）只对幂等接口安全。

**JWT（JSON Web Token）**：三段式（Header.Payload.Signature）自包含令牌，签名保证不可篡改，载荷仅 Base64URL 可读（不放敏感数据）。在 SelfBlog 中：`JwtUtils` 签发/校验，载荷含 userId 与过期时间；`credentialVersion` 嵌入实现改密即全设备失效。

**JWS / JWE（签名/加密 JWT）**：JWS=签名保证完整性（SelfBlog 所用），JWE=加密保证机密性。在 SelfBlog 中：用 JWS——令牌内容本就不敏感（userId），机密性靠传输层 HTTPS。

**Keep-Alive / Connection Reuse（连接复用）**：HTTP/1.1 默认长连接减少握手；HTTP/2 多路复用更进一步。在 SelfBlog 中：HikariCP 池化数据库连接是同一思想的 DB 版；前端 axios 实例复用底层连接。

**MIME Type（媒体类型）**：`application/json`、`text/html`、`application/xml` 决定处理方式。在 SelfBlog 中：`SpaController` 返回 `text/html`、API 返回 JSON、`SeoController` 返回 XML——类型即契约。

**Origin / Same-Origin Policy（同源策略）**：协议+域名+端口三元组同源才可自由互访，是浏览器安全的基石。CORS 是服务端对它的显式豁免。在 SelfBlog 中：前端永远同源请求 `/api`（开发代理、生产反代），从架构上回避跨源；`SecurityConfig` 的 CORS 配置只是兜底。

**Rate Limiting（限流）**：控制单位时间请求数保护后端。算法：固定窗口（简单、边界突刺）、滑动窗口（平滑）、令牌桶（允许突发）。在 SelfBlog 中：`RateLimitInterceptor` 是**固定窗口**（Caffeine 计数、1 分钟窗、`ip:path` 键、超限 429 JSON、最长前缀匹配阈值）；单实例内存实现，多实例需 Redis——注释明言。

**REST（Representational State Transfer）**：资源+URI+统一接口的架构风格：名词 URI、动词 HTTP 方法、状态码表达结果。在 SelfBlog 中：`/api/post/{id}`、`/api/guest-comments` 名词化命名；但点赞用 `POST /toggle` 动词化——实战中务实优先于教条。

**Session vs Token（会话与令牌）**：Session 状态在服务端（Cookie 关联），Token 状态在客户端（签名防篡改），后者天生适配水平扩展与 STATELESS。在 SelfBlog 中：`STATELESS` + JWT，服务端只存黑名单与设备集——「有状态的认证，无状态的服务」。

**SPA（Single-Page Application，单页应用）**：一次加载、前端路由切换视图，服务端只供数据。代价：首屏、SEO、深链恢复都要额外工程。在 SelfBlog 中：Vue SPA + `SpaController` 服务端补可读 HTML 双轨解决 SEO；历史路由 `/post/{id}` 直出真实内容。

**SSL/TLS Termination（TLS 终结）**：反向代理卸载 HTTPS，内网明文转发。在 SelfBlog 中：Vercel/反代层终结 TLS，后端容器只跑 HTTP；`SiteOrigin` 解析 origin 时忽略 `X-Forwarded-*` 伪造正是为此兜底。

**URI Design（URI 设计）**：资源名词化、层级表达从属、避免动词与实现细节泄漏。在 SelfBlog 中：`/api/admin/post/{id}/status` 层级清晰；后台地址 `adminPath` 是站点设置而非后端概念——后端安全只认 `/api/admin/**`。

**Web Security Headers（安全响应头）**：`X-Frame-Options`（防点击劫持）、`Referrer-Policy`、`Permissions-Policy`（关硬件能力）、HSTS。在 SelfBlog 中：`SecurityConfig` 逐一设置——`X-Frame-Options: DENY`、`strict-origin-when-cross-origin`、关摄像头/定位/麦克风。

**XSS（Cross-Site Scripting，跨站脚本）**：注入恶意脚本到他人浏览器执行。防御：输出编码、白名单消毒、CSP。在 SelfBlog 中：三层防线——`XssFilter` 请求级过滤；`GuestCommentServiceImpl` **先消毒后校验**（`HtmlSanitizer.sanitizePlainText` 打头）；前台渲染走 `SafeMarkdown` 白名单。

## 组五：数据库与 ORM（含缓存）

**BaseMapper（通用 Mapper）**：MyBatis Plus 的根接口，免费提供 insert/selectById/updateById/deleteById 等单表操作，子接口零方法即有全套 CRUD。在 SelfBlog 中：`mapper/` 下 28 个接口全部 `extends BaseMapper<XxxEntity>`，绝大多数接口一行方法体都不用写。

**Batch Processing（批处理）**：把 N 次往返合并为一次批量操作（批量 insert、`IN` 查询、`saveBatch`），是消灭 N+1 的正面手段。在 SelfBlog 中：`GuestCommentServiceImpl` 的 `getAdminComments` 用 `selectBatchIds` 一次取回全部关联用户，替代循环单查；MyBatis Plus 的 `saveBatch` 可用于种子数据。

**Cache-Aside Pattern（旁路缓存）**：读时先查缓存、未命中回源 DB 再写缓存；写时更新 DB 后失效缓存。最常用也最容易写错的缓存模式（失效时机是难点）。在 SelfBlog 中：service 层 `@Cacheable` 查询 + 写操作 `@CacheEvict` 的组合即旁路缓存——`siteSettingsCache` 在设置更新时被逐出。

**Cache Avalanche（缓存雪崩）**：大量缓存同一时刻集体过期，DB 瞬间被打穿。缓解：TTL 加随机抖动、错峰过期、多级缓存。在 SelfBlog 中：`CaffeineCacheConfig` 的 8 个缓存 TTL 各不相同（30s~30m），本身就是一种天然错峰。

**Cache Breakdown（缓存击穿）**：单个热点 key 过期瞬间，并发请求同时回源。缓解：互斥重建、逻辑过期、热点永不过期。在 SelfBlog 中：Caffeine 的 `get(key, mappingFunction)` 原子加载即互斥重建；本站热点（热门标签、站点设置）读多写少，风险本就低。

**Cache Penetration（缓存穿透）**：查询**不存在**的数据，缓存永不命中、请求全打到 DB（可被恶意利用）。缓解：空值缓存、布隆过滤器、参数校验。在 SelfBlog 中：`statusCache`(2m) 缓存存在性判断类结果；controller 层的参数校验（如 ID 格式）在进入 DB 前先拦掉明显非法请求。

**Connection Pool（连接池）**：预先建好并复用数据库连接，避免每次请求握手认证（毫秒~十毫秒级开销）。在 SelfBlog 中：HikariCP 是 Spring Boot 默认池；SQLite 场景池大小固定为 1（SQLite 单写者），MySQL/池参数随环境调整。

**CRUD（增删改查）**：Create/Read/Update/Delete 四类基本数据操作的统称，分层架构里 CRUD 代码应最薄。在 SelfBlog 中：BaseMapper 覆盖单表 CRUD，service 只写「有业务含义」的部分（归属校验、副作用、缓存失效）。

**databaseId / Multi-Dialect SQL（多方言 SQL）**：同一语句按数据库厂商切换实现，XML 里 `databaseId="mysql"/"sqlite"` 各写一份，无 id 的作为兜底。在 SelfBlog 中：`MybatisPlusConfig` 注册 `VendorDatabaseIdProvider`；**H2 没有映射 id，只走无 databaseId 的兜底语句**——MySQL 专有语法绝不能写进共享语句。

**DDL / DML（数据定义与操作语言）**：DDL 定义结构（CREATE/ALTER/DROP），DML 操作数据（INSERT/UPDATE/DELETE/SELECT）。在 SelfBlog 中：`schema-sqlite.sql`（`CREATE TABLE IF NOT EXISTS`，不迁移既有文件）与 `schema-fresh-install.sql`（含 DROP，只给全新空库）是 DDL；`migration-selfblog.sql` 是已有库唯一的升级通道。

**DTO / VO / PO / Entity（数据传输/视图/持久对象与实体）**：PO(Entity) 映射表行；DTO 承接入参；VO 塑造出参。分层的意义是「表结构变化不直接击穿到接口契约」。在 SelfBlog 中：`entity/` 28 个实体（`@TableName` + `@TableLogic isDeleted`）、`dto/` 33 个入参、`vo/` 37 个出参，三个目录互不混用；`utils/UserConverter` 之类转换器负责 entity→vo。

**Foreign Key（外键）**：声明表间引用约束，由数据库强制一致性。互联网高并发库常弃用外键（锁与迁移成本），改由应用层保证。在 SelfBlog 中：schema 里关联关系主要靠应用层维护（如 `blog_post_tag` 中间表），这要求 service 层「先校验存在再写入」的纪律。

**HikariCP**：Spring Boot 默认连接池，以字节码级优化与极低开销著称，核心参数 `maximumPoolSize`、`connectionTimeout`。在 SelfBlog 中：`DatabaseConfig` 显式配置——SQLite `maximumPoolSize=1`（单写者模型下的正确选择），MySQL 按环境变量连接五件套建池。

**Index（索引）**：加速查询的有序数据结构（B+ 树），代价是写入变慢与存储增加；最左前缀原则决定复合索引能否命中。在 SelfBlog 中：搜索走 `title LIKE`/内容匹配的高频列应有索引；`guest_comment.post_id`、`anonymous_post_like(post_id, visitor_hash)` 这类查询入口列是索引候选——加索引要连跑三库 schema 验证。

**Isolation Level（隔离级别）**：并发事务互不干扰的程度：READ UNCOMMITTED→READ COMMITTED→REPEATABLE READ→SERIALIZABLE，逐级消除脏读/不可重复读/幻读。在 SelfBlog 中：依赖各库默认级（MySQL InnoDB 可重复读、SQLite 串行写）；计数更新用原子 `UPDATE ... SET like_count = like_count + 1` 把并发正确性下沉给数据库，不靠提高隔离级。

**KV Table（键值表）**：以 `key` 为主键、`value` 存值的通用表，新增配置项零 DDL。在 SelfBlog 中：`site_setting`（`setting_key` 主键 + `setting_value`）——新增站点设置键只需在 `SiteSettingsServiceImpl` 登记并做校验，**零 schema 迁移**，这是全项目最划算的一张表。

**LRU（Least Recently Used，最近最少使用）**：淘汰最久未访问条目的缓存策略，Caffeine 以 W-TinyLFU（频率+新近度）超越朴素 LRU。在 SelfBlog 中：`RateLimitInterceptor` 的计数缓存设 `maximumSize(50000)`，满了 Caffeine 自动按新近度/频率逐出，防内存无界。

**Logic Delete / Soft Delete（逻辑删除/软删除）**：删除只打标记位（`is_deleted=1`），数据保留可恢复、可审计，代价是所有查询都要过滤。在 SelfBlog 中：20+ 实体带 `@TableLogic private Integer isDeleted` + `application.yml` 的 `logic-delete-field`，MyBatis Plus 自动在生成 SQL 里追加 `is_deleted=0`——但**手写 SQL 必须自己带上该条件**，这是最容易漏的一条。`AnonymousLikeServiceImpl` 的三态切换（0↔1）正是手工操作软删标记的实例。

**Migration Script（迁移脚本）**：对已有库做增量结构/数据变更的脚本，与「全新建库脚本」严格分离。在 SelfBlog 中：`schema-fresh-install.sql` 只用于全新空库（改名就是为了降低误执行风险），**已有库只能跑 `migration-selfblog.sql`**；Docker Compose 挂载的也是迁移件。

**MyBatis**：半自动 ORM——SQL 全手写、参数与结果映射自动化，SQL 控制权最大。在 SelfBlog 中：复杂查询（分片站点地图的统计、跨表关联）写在 `resources` 的 Mapper XML 里。

**MyBatis Plus**：在 MyBatis 上增强：通用 CRUD、条件构造器、分页插件、`@TableLogic`/`@Version`/自动填充，保留手写 SQL 能力。在 SelfBlog 中：3.5.8 版本；`ServiceImpl<XxxMapper, Entity>` 基类让门面与查询两个实现都能继承同一套泛型方法。

**N+1 Query（N+1 查询）**：查 1 次列表再循环查 N 次关联数据的性能反模式，列表页杀手。在 SelfBlog 中：`GuestCommentServiceImpl.getAdminComments` 先查分页评论，再 `selectBatchIds(visitorIds)` 一次取全部访客——1+N 变 2 次；发现列表接口慢，第一件事就是数 SQL 条数。

**ORM（Object-Relational Mapping，对象关系映射）**：在对象世界与关系表之间自动翻译，代价是复杂查询时「表达力倒挂」。在 SelfBlog 中：MyBatis Plus 管单表、XML 管复杂 SQL 的组合，是对 ORM 边界的清醒用法。

**Pagination（分页）**：大结果集按页返回。两种流派：偏移分页（LIMIT/OFFSET，简单但深翻页慢）与游标分页（where id > last，稳定但要排序键）。在 SelfBlog 中：MyBatis Plus 分页插件 + `Result` 信封里的 `records/total/size/current/pages` 六件套；管理后台列表全部偏移分页。

**Primary Key（主键）**：行的唯一标识。自增长序 vs 分布式 ID（雪花/UUID）取决于是否多写入点。在 SelfBlog 中：核心表自增主键（单实例足够），MyBatis Plus 的 `IdType` 注解声明；`visitorId` 这类外部标识则用正则约束格式后哈希入库存。

**Query Wrapper（条件构造器）**：链式构建 WHERE 条件的对象化 API（`lambdaQuery().eq(...).like(...)`），杜绝字符串拼 SQL。在 SelfBlog 中：service 查询实现大量使用 LambdaQueryWrapper——列名由方法引用推导，改字段名编译期即报错。

**Schema（模式/表结构）**：库中表、列、约束、索引的整体定义。在 SelfBlog 中：分两组维护——历史社区表（`sys_user`、`blog_post`、`blog_comment`、`blog_tag`、`blog_follow`、`blog_notification` 等）与个人博客新表（`site_setting`、`visitor_event`、`anonymous_post_like`、`guest_comment`、`photo_album`、`album_photo`）；改任何一张表都要过三库初始化策略（SQLite 自动建/MySQL 全新脚本/H2 测试脚本）。

**SELECT FOR UPDATE（悲观锁查询）**：事务内锁定选中行直到提交，天然串行化并发修改。在 SelfBlog 中：计数类「读-改-写」若不用原子 UPDATE，就应退到 `FOR UPDATE`；SQLite 单写者模型下锁语义由库自身承担，MySQL 场景才真正体现价值。

**Snowflake ID（雪花 ID）**：64 位分布式 ID = 时间戳 + 机器位 + 序列号，趋势递增、不依赖库。在 SelfBlog 中：MyBatis Plus 内置 `ASSIGN_ID`（雪花算法）可选作 `@TableId` 策略；当前单实例用自增即可，多实例部署时雪花 ID 是现成答案。

**SQL Injection（SQL 注入）**：用户输入被拼进 SQL 语义层，历史上最著名的注入类漏洞。防御：参数化查询（占位符）、最小权限、不信任拼接。在 SelfBlog 中：MyBatis 的 `#{}` 占位符全程参数化；`${}` 字符串替换只允许出现在无用户输入的内部常量处——代码评审时见 `${` 就要问一句值从哪来。

**TTL（Time To Live，存活时间）**：缓存条目的寿命，到期逐出。TTL 是「新鲜度 vs 命中率」的直接旋钮。在 SelfBlog 中：8 个具名缓存各自独立 TTL（`hotTagsCache` 10m、`statusCache` 2m、`visitorSummaryCache` 30s……），按数据变化频率分别标定，而不是一刀切。

**Transaction（事务）**：一组操作要么全成要么全败（ACID）。Spring 里 `@Transactional` 声明式开启，注意默认只回滚 RuntimeException/ Error，受检异常不回滚。在 SelfBlog 中：写操作统一 `@Transactional(rollbackFor = Exception.class)` 显式覆盖默认规则；查询方法用 `readOnly = true` 让驱动与连接池做优化。

**Type Handler（类型处理器）**：MyBatis 在 JDBC 类型与 Java 类型之间转换的插件点。在 SelfBlog 中：`LocalDateTime` 与 SQLite 文本时间的映射靠默认/自定义 handler；写枚举字段入库时也可在此收口。

**Unique Constraint（唯一约束）**：数据库层防重复的最后防线——应用层校验可被并发绕过，唯一索引不会。在 SelfBlog 中：`guest_comment` 的关联键、`site_setting.setting_key` 主键天然唯一；发布文章的唯一性修复（防重复 slug）就是「应用层检查 + 数据库唯一约束 + 捕获重复键异常」三件套，`docs/agents/backend.md` 有专门记录。

**Version Field（版本号字段）**：乐观锁的载体：更新带 `WHERE version = ?` 并 `SET version = version + 1`，影响行数为 0 即冲突。在 SelfBlog 中：MyBatis Plus `@Version` + 乐观锁插件可开箱即用；当前核心写路径冲突概率低未启用，但「管理后台并发编辑同一文章」是它的候选场景。

## 组六：安全

**Allowlist / Denylist（白名单/黑名单）**：白名单「默认拒绝、显式允许」，黑名单「默认允许、显式拒绝」——安全场景永远优先白名单。在 SelfBlog 中：`HtmlSanitizer` 白名单化标签与属性；`SecurityConfig` 的放行清单是显式 URL 白名单；CORS 拒绝通配符结尾 origin 也是白名单纪律。

**Authentication vs Authorization（认证与授权）**：认证回答「你是谁」，授权回答「你能做什么」——两者独立失败、独立测试。在 SelfBlog 中：`JwtAuthenticationFilter` 只管认证（解析 token 放身份）；授权在 `SecurityConfig` 的 `authorizeHttpRequests` 与方法级 `@PreAuthorize`——「只解析不拒绝」的职责切分就是这条边界。

**BCrypt**：专为口令设计的慢哈希（基于 Blowfish），内置随机盐、可调成本因子，抗 GPU 暴力破解。在 SelfBlog 中：`SecurityConfig` 声明 `BCryptPasswordEncoder`，强度 12（`bcrypt.strength` 可配）——每提升 1，破解成本翻倍、登录耗时也翻倍。

**Brute Force Attack（暴力破解）**：穷举口令的攻击，防御靠慢哈希、限流、锁定与告警。在 SelfBlog 中：登录接口落在限流前缀下（1 分钟窗口计数）；BCrypt 强度 12 本身就是「让每次猜测都变贵」的暴力破解税。

**Clickjacking（点击劫持）**：把你的页面塞进透明 iframe 诱导点击。防御：`X-Frame-Options: DENY` 或 CSP `frame-ancestors`。在 SelfBlog 中：`SecurityConfig` 明确设置 `X-Frame-Options: DENY`，后台界面不可被嵌框。

**Credential Version（凭证版本）**：用户级整数/哈希版本号，嵌入 JWT 载荷，改密时递增使旧 token 全设备失效。在 SelfBlog 中：`JwtUtils.credentialVersionFor` 用 HmacSHA256 对域分离串 `selfblog:credential-version:v1\nuserId\npasswordHash` 计算版本指纹——**只存 HMAC 不暴露 BCrypt 哈希**，校验 token 时比对版本不一致即拒绝。

**Defense in Depth（纵深防御）**：多层独立防线，单层失守不致全局沦陷。在 SelfBlog 中：XSS 三层（`XssFilter` 请求级 → `sanitizePlainText` 入库前 → `SafeMarkdown` 出库渲染）；权限两层（URL 层 `hasRole` + 方法层 `@PreAuthorize`）；写入两层（dto 校验注解 + service 语义校验）。

**Domain Separation（域分离）**：密码学惯例——同一密钥派生多种用途时，把「用途标签」混入被哈希消息，防跨用途重放。在 SelfBlog 中：`credentialVersionFor` 的 `selfblog:credential-version:v1\n` 前缀就是域分离串：将来任何「拿 JWT_SECRET 再派生点什么」的需求都必须带自己的域前缀。

**Double Submit Cookie（双提交 Cookie）**：CSRF 防御术——Cookie 与请求头各带同一随机值，服务端比对一致才放行（跨站攻击者读不到 Cookie）。在 SelfBlog 中：前端 axios 拦截器生成 32 字节 hex 的 XSRF token 并注入 `X-XSRF-TOKEN` 头，为未来 Cookie 化场景预留的现成防线。

**Encoding vs Encryption vs Hashing（编码/加密/哈希之辨）**：编码可逆无密钥（Base64）；加密可逆有密钥；哈希单向不可逆。混淆三者是安全评审高频扣分点。在 SelfBlog 中：JWT 载荷是编码（人人可读，别放敏感信息）；口令是哈希（BCrypt）；token 存储经前端 `utils/crypto` 编解码——各安其位。

**Fail-closed（失败关闭）**：依赖组件不可用时选择「拒绝服务」而不是「放行」。安全组件必须 fail-closed，可用性组件才谈 fail-open。在 SelfBlog 中：`EnvValidationConfig` 启动期校验 `JWT_SECRET` 不达标直接拒绝启动；`JwtAuthenticationFilter` 解析异常一律按匿名处理（交给授权层拒绝）而非放行。

**Hash（哈希）**：单向函数，任意输入映射为固定长摘要，微改输入雪崩输出。用于完整性校验、口令存储、指纹。在 SelfBlog 中：`VisitorHashUtils.hash(visitorId, hashSecret)`——访客标识不以明文入库，只存加盐哈希，库泄漏也无法还原 visitorId。

**Input Validation（输入校验）**：在信任边界上校验类型、格式、长度、范围——「校验的是形状，消毒的是内容」。在 SelfBlog 中：`visitorId` 正则 `[A-Za-z0-9_-]{8,80}` 先验格式；`normalizeSize` 把分页参数钳到上限 100；dto 注解管格式、service 语义校验管业务。

**JWT Blacklist（令牌黑名单）**：登出/改密后令牌在自然过期前仍有效，黑名单显式吊销——「有状态的认证，无状态的服务」的妥协点。在 SelfBlog 中：`JwtUtils` 内 `ConcurrentHashMap` 黑名单 + `CopyOnWriteArraySet` 设备令牌集，`JwtSchedulerConfig` 周期清理过期项防无界增长。

**Least Privilege（最小权限原则）**：主体只授予完成任务所必需的最小权限。在 SelfBlog 中：`/api/admin/**` 整体 `hasRole("admin")`，公开接口逐条白名单放行（而不是放行 `/api/**` 再拉黑）；数据库账号理论上也应区分「迁移权限」与「运行时权限」。

**OWASP**：开放Web应用安全项目，OWASP Top 10 是应用安全风险的公共清单与评审框架。在 SelfBlog 中：注入（参数化）、失效的认证（JWT 吊销链）、XSS（三层消毒）、安全配置错误（SecurityConfig 逐项加固）等条目都有对应落点——附录A 的故障排查与安全评审可直接对照 Top 10 打勾。

**Password Hashing（口令哈希）**：口令绝不明文/可逆存储，用慢哈希 + 盐（BCrypt 自带）入库。在 SelfBlog 中：BCrypt 强度 12；`credentialVersionFor` 刻意**不把 BCrypt 哈希带出库**——版本指纹是 HMAC，即使日志泄漏也反推不出口令材料。

**Password Policy（口令策略）**：长度/复杂度/更换策略。现代共识：长度优先于复杂度，NIST 已弃强制定期换密。在 SelfBlog 中：`ADMIN_PASSWORD` 要求 ≥12 位并由 `EnvValidationConfig` 启动期强制；注册关闭后普通用户口令策略退场。

**Path Traversal（路径穿越）**：以 `../` 逃出预期目录读写任意文件。防御：规范化后校验前缀、白名单文件名。在 SelfBlog 中：媒体/上传相关接口对文件名做白名单与规范化处理；静态资源映射限定根目录。

**Principal（安全主体）**：安全上下文中的「当前操作者」，可能是用户、匿名或系统。在 SelfBlog 中：`SecurityUtils` 兼容 principal 为 `UserContext` 或裸 `Long` 两种形态——从主体取 ID 前先判型，这是切片测试里 `@WithMockUser` 也能工作的原因。

**RBAC（Role-Based Access Control，基于角色的访问控制）**：权限挂角色、用户挂角色，避免逐用户授权爆炸。在 SelfBlog 中：单一 `ROLE_admin` 角色——单作者博客把 RBAC 简化到极致；`app.single-author-mode=true` 时写入类操作还有「必须 admin」的兜底规则。

**Salt（盐）**：哈希前混入的随机值，使相同口令产生不同摘要、预计算彩虹表失效。BCrypt 把盐直接编进结果串。在 SelfBlog 中：BCrypt 自动处理口令盐；`VisitorHashUtils.hash(visitorId, hashSecret)` 的 hashSecret 扮演「全局盐」角色（取自 `JWT_SECRET` 缺省 `local-visitor-hash`）。

**Sanitize vs Escape（消毒与转义）**：消毒按白名单移除危险内容（入库存的干净数据）；转义在输出语境中让字符失去元语义（HTML/JS/属性上下文各不同）。在 SelfBlog 中：入库走 `HtmlSanitizer.sanitizePlainText`（消毒）；前台渲染走 `SafeMarkdown` 与框架默认转义（escape）——两道工序不可互相替代。

**Secret Management（密钥管理）**：密钥的生成、分发、轮换与泄漏处置。规则：不入库、不进日志、可轮换。在 SelfBlog 中：`JWT_SECRET`（≥32 位）走 `.env`/环境变量，`DotenvConfig` 保证文件值成为 JVM property；`ADMIN_PASSWORD` 同理；日志侧 `StringMaskUtils`/token 指纹防止敏感串进日志。

**Single Author Mode（单作者模式）**：SelfBlog 的根本设定：`app.single-author-mode: true` 时，写入类操作在常规授权之外再要求 `ROLE_admin` 兜底——即使某接口漏了 URL 防护，模式兜底仍拦截访客写入。在 SelfBlog 中：`SecurityConfig` 读取该开关；它同时是「注册关闭（403）」语义的一部分，是理解本项目安全模型的钥匙。

**Threat Model（威胁建模）**：设计期回答「谁会攻击你、图什么、从哪进」。个人博客的典型对手：爬虫刷接口、匿名灌水、伪造 Host 抢 canonical。在 SelfBlog 中：限流防刷、先消毒后校验防灌水、`SiteOrigin` 固定 origin 防伪造——三个机制各自对应一类已建模威胁，而不是无的放矢的加固。

**Timing Attack（时序攻击）**：通过响应时间差异推断秘密（逐字符比对快慢）。防御：常数时间比较。在 SelfBlog 中：token 校验走 jjwt 库内部实现（HMAC 比较为常数时间）；自定义比对场景（指纹比对）应优先用 `MessageDigest.isEqual` 而非手写 `equals` 循环。

**Token Fingerprint（令牌指纹）**：日志与错误上报中只展示令牌的不可逆缩略（首尾片段），既能对账又不泄漏完整凭证。在 SelfBlog 中：前端 `api/index.ts` 以「首尾各 20 字符再 btoa」生成指纹写入日志；后端 `StringMaskUtils` 在服务端做同类脱敏。

**Visitor ID Hashing（访客 ID 哈希）**：匿名访客标识入库前加盐哈希，兼顾「同访客可识别」与「库泄漏不可还原」。在 SelfBlog 中：`VisitorHashUtils.hash(visitorId, hashSecret)` 产出的哈希是 `anonymous_post_like.visitor_hash` 与留言关联的存储形态——同一 visitorId 永远得到同一哈希，幂等点赞才成立。

## 组七：前端框架（Vue 生态为主）

**Composition API（组合式 API）**：`setup()`/`<script setup>` 按逻辑关注点组织代码，替代 Options API 的选项切割；配合组合式函数实现逻辑复用。在 SelfBlog 中：全仓库组件统一 `<script setup lang="ts">`；`composables/` 下 8 个组合式函数（`useConfirm`/`useDialog`/`useMarkdownEnhance`/`usePageConfig`/`usePageSeo`/`useScrollReveal`/`useScrollState`/`useSeoMeta`）是组合式 API 的复用成果。

**Composable（组合式函数）**：以 `use` 开头、封装响应式状态+生命周期逻辑的函数，是 Vue 3 逻辑复用的标准形态（替代 mixin）。在 SelfBlog 中：`useScrollReveal` 封装滚动进入动画、`useSeoMeta` 统一 document.meta 更新；注意 `useConfirm()` 返回的 `ConfirmDialog` **必须渲染到模板里**，否则确认框不出现——这是测试与实机的经典差异点。

**Computed（计算属性）**：由响应式依赖派生的缓存值，依赖不变不重算。在 SelfBlog 中：store 与组件里大量 `computed` 派生（如站点设置 → 页头标题、皮肤类名的映射），避免手写脏检查。

**Destructuring Loss（响应式解构丢失）**：对 props/reactive 解构会丢失响应性（值被复制成静态量）。解法：`toRefs`、或在模板/计算属性内保持属性访问路径。在 SelfBlog 中：组件接收 props 一律 `props.xxx` 访问而不是解构——`tsconfig` 的 `noUncheckedIndexedAccess` 与此约定共同构成两个最常踩的静态陷阱。

**Diff / Patch（差异比较与补丁）**：新旧虚拟 DOM 树逐层比对，最小化真实 DOM 操作。同层比较、key 复用节点。在 SelfBlog 中：文章列表 keyed `v-for` 用文章 ID 作 key——用数组下标作 key 会在头部插入时错位复用，是 diff 环节最常见的 bug 源。

**Directive（指令）**：带 `v-` 前缀的模板扩展（`v-if`/`v-for`/`v-model`/`v-bind`/`v-on`），可自定义。在 SelfBlog 中：表单双向绑定全走 `v-model`；权限按钮用条件渲染而非自定义指令——保持指令面最小。

**HMR（Hot Module Replacement，模块热替换）**：开发期局部替换变更模块并保留应用状态，比整页刷新快且不丢表单。在 SelfBlog 中：`vite` dev server（端口 3000）自带 HMR；改样式 token 与组件模板即时生效——客户端插件类改动则需确认 `pnpm run dev:web` 类监听器在跑，否则不会自动更新。

**Lazy Loading Route（路由懒加载）**：路由组件按需 `import()`，进入页面才加载对应 chunk，首屏只带必要代码。在 SelfBlog 中：`frontend/src/router/` 全部路由组件懒加载 `component: () => import(...)`（`modules/admin.ts` 一处文件就有 12 处），`helpers.ts` 封装 `lazyLoad` + `ErrorFallback`——chunk 加载失败有兜底 UI。

**Lifecycle Hooks（生命周期钩子）**：组件各阶段回调（`onMounted`/`onUnmounted` 等），副作用注册与清理的锚点。在 SelfBlog 中：挂载会注册全局监听器的组件（主题监听、滚动监听）必须在 `onUnmounted` 清理——前端测试约定明确要求 `enableAutoUnmount(afterEach)`，因为 jsdom setup **没有**自动卸载。

**Pinia**：Vue 官方状态库，天然 TS、去 mutation、组合式 store 风格，devtools 可追踪。在 SelfBlog 中：5 个 store——`site.ts`（站点设置、`adminPath`/`adminRoot`、皮肤与 favicon）、`theme.ts`（`localStorage['edu_theme']`、`.dark-mode`、`matchMedia` 监听）、`user.ts`（**store id 是 `'auth'`，导出别名 `useUserStore`**，token 经 `utils/crypto` 编解码存取，`validateToken` 只解 JWT 不请求网络）、`music.ts`（唯一音频宿主）、`pages.ts`（**只存已发布页面配置，草稿绝不进公开 store**）。

**props Down / events Up（单向数据流）**：父传子 props、子通知父事件，数据流向单向可追踪。在 SelfBlog 中：后台表单组件全部遵循「props 进、emit 出」，`defineProps`/`defineEmits` 编译宏声明契约。

**Reactivity System（响应式系统）**：Vue 3 用 Proxy 追踪依赖、触发更新——读取时收集、写入时派发。在 SelfBlog 中：`ref`/`reactive`/`computed` 的行为皆源于此；理解「解构丢响应」「数组索引赋值无需 `Vue.set`（Proxy 时代已修）」都要回到 Proxy 机制。

**SFC（Single-File Component，单文件组件）**：`.vue` 文件把 template/script/style 装进一个组件单元，编译器拆解为 JS 渲染函数与样式。在 SelfBlog 中：`views/` 9 组、`components/` 5 组全部 SFC；`vite-plugin-vue` 负责编译。

**Slot / Props Fallthrough（插槽与属性透传）**：插槽让父组件向子组件注入模板；未声明的 attrs 自动落到根元素。在 SelfBlog 中：布局组件（`components/layout/`）用插槽承接页面主体；测试约定**禁用 `stubs: { RouterLink: true }`**——stub 会丢掉 slot 内容，也表达不了 active 态，路由测试优先用真实 `createMemoryHistory()`。

**SSR / SSG / ISR（服务端渲染/静态生成/增量再生）**：SSR 每请求服务端出 HTML；SSG 构建期预生成；ISR 允许构建后按需再生。在 SelfBlog 中：主站是 CSR SPA，但 `SpaController` + `service/site/seo/` 为公开历史路由**服务端直出可读 HTML**（Vue 挂载后替换 `#app` 内的 `.seo-reading`）——一种「SPA + 关键页 SSR 化」的混合策略，专为 SEO 与爬虫可读性。

**State Machine Thinking（状态机思维）**：把异步流程显式建模为有限状态（idle/loading/success/error），消灭布尔标志组合爆炸。在 SelfBlog 中：`api/index.ts` 的 `isRefreshing` + `failedQueue` 就是两态刷新机的手写实现；更复杂的流程值得抽象成显式状态机。

**Vite**：基于原生 ESM 的构建工具，dev 按 need 编译（秒启），prod 用 Rollup 打包。在 SelfBlog 中：Vite 6；dev 端口 3000，`/api` 与 `/uploads` 代理到 `VITE_API_TARGET`（默认 `http://localhost:8825`）；`baseURL` 硬编码 `/api`——`VITE_API_BASE_URL` 实际**没有任何地方读取**，只有 Vite 代理与 `vercel.json` 重写生效。

**Virtual DOM（虚拟 DOM）**：用 JS 对象描述 DOM 树，变更先改虚拟树、diff 后再最小化操作真实 DOM。在 SelfBlog 中：Vue 3 编译器已大量静态提升与 patch flag 优化，虚拟 DOM 开销比 Vue 2 显著降低——不用手写优化，但要避免无 key 大列表。

**Vue Router**：官方路由器：路由表、嵌套、守卫、动态注册。在 SelfBlog 中：`router/modules/` 六个模块（home/post/discover/user/auth/admin）组装路由表，`guards.ts` 统一守卫；**后台路由在运行时按服务端下发的 `adminPath` 动态注册**（`createAdminRoutes`），不在首次导航前装好就会 404。

**Watch（侦听器）**：响应式数据的副作用观察者，`watchEffect` 自动收集依赖。在 SelfBlog 中：`theme.ts` 监听 `matchMedia` 变化同步 `.dark-mode`；URL/查询参数联动页面内容用 watch 而不是在路由守卫里做。

## 组八：构建与工程化

**Bundle Analysis（产物分析）**：可视化 chunk 构成与体积占比，找出该懒加载/该拆分的模块。在 SelfBlog 中：路由全懒加载后，`vite build` 的输出清单是第一手分析材料；第三方大件（markdown 渲染、图表）若进入首屏 chunk 就该拆。

**CHANGELOG（变更日志）**：面向使用者的时间线记录：新增/修复/破坏性变更。仓库规范：顶部追加 `## 未发布 — <标题> (日期)`，正文分「说明 / 修复」要点，并附 **`### 影响文件`** 表格（文件 + 改动类型）。在 SelfBlog 中：根目录 `CHANGELOG.md`，改代码就要更新——「以忘记更文档为耻」是仓库明文荣耻观。

**CI/CD（持续集成/持续部署）**：push 即自动构建测试部署，把「合并恐惧」变成机器例行公事。在 SelfBlog 中：`.github/workflows/ci.yml` 四个 job 全真实执行——backend 全套（H2）、frontend 全闸门、**真实 MySQL 8**（`DB_TYPE=mysql` + `schema-fresh-install.sql`，本机无 MySQL，CI 是唯一真库验证环境）、docker（构建镜像、起容器、探活 `/actuator/health`、grep 首页 HTML 的 `seo-reading`）。

**Code Review（代码评审）**：变更经第二双眼睛核对正确性与风格，兼作知识传播。在 SelfBlog 中：仓库以「开发荣耻」条款替代人工评审流程——「以想业务为耻，以人类确认为荣」「以跳过验证为耻，以主动测试为荣」，agent 协作时同样适用。

**Conventional Commits（约定式提交）**：`type(scope): message` 格式化提交信息，可机器解析生成 CHANGELOG。在 SelfBlog 中：历史提交信息可见 feat/fix 前缀惯例；与 CHANGELOG 的「未发布」区配合形成版本叙事。

**Dead Code Elimination / 死代码清理**：删除无引用代码，降低维护面；先确认「真的无引用」（动态路由、反射引用会骗过静态分析）。在 SelfBlog 中：2026-10-17 前端 legacy 清理删除 47 个文件（两波），`views/auth/Register.vue`、`views/circle/*` 等已删——但**后端对应社区接口与表刻意保留**（admin-only API 面），清理历史记录在 `docs/agents/frontend.md`。

**ESM / CommonJS（ES 模块与 CommonJS）**：浏览器原生 `import/export` 与 Node 传统 `require`。前端构建与 Tree-shaking 都以 ESM 为前提。在 SelfBlog 中：Vite 全 ESM；`build-vercel-proxy.mjs` 用 `.mjs` 后缀显式声明 ESM；依赖里出现 CJS 包时 Vite 会做互操作预打包。

**Formatter（格式化器）**：机器统一代码风格（缩进/引号/换行），消灭风格争论。在 SelfBlog 中：Prettier，`npm run format:check` 是真实仓库级闸门（`.prettierrc` 改为 `endOfLine: "auto"` 后重写过一次历史漂移）——Windows 上 CRLF/LF 之争被 `auto` 策略终结。

**Ghost Dependency（幽灵依赖）**：代码 import 了未在 `package.json` 声明、只因依赖提升而碰巧可用的包——换包管理器或提升策略变化即刻爆炸。在 SelfBlog 中：`npm run test:deployment` 与 CI 干净安装（`npm ci`）会暴露此类问题；新增 import 后跑一次 `type-check` 是最快体检。

**Gitignore Discipline（忽略清单纪律）**：构建产物、本地密钥、编辑器文件不进版本库。在 SelfBlog 中：`backend/.env`（含 `JWT_SECRET`/`ADMIN_PASSWORD`）绝不入库，只有 `.env.example` 模板；`target/`、`dist/`、`node_modules/` 全部忽略。

**Linter（静态检查器）**：不运行代码而找错误与坏味道（未用变量、错误模式）。在 SelfBlog 中：ESLint，`npm run lint:check` 是四大前端闸门之首；规则配置在 flat config（见下条）。

**Monorepo / Polyrepo（单仓多包与多仓）**：SelfBlog 是**一个仓库两个独立工程**，无根构建清单、无 npm workspace——既不是 monorepo workspace 也不是多仓。在 SelfBlog 中：后果是命令必须在 `backend/` 或 `frontend/` 各自目录执行；后端 Maven 的 `frontend-maven-plugin` 是两个工程唯一的构建期握手（所以测试必须带 `-Dskip.npm=true`）。

**package-lock / npm ci（锁定与可复现安装）**：lock 文件钉死依赖树精确版本；`npm ci` 严格按 lock 安装（比 `install` 快且不漂移）。在 SelfBlog 中：CI frontend job 用 `npm ci` + Node 24.15.0；本机 `npm run <script>` 全部失败是 PATH 引号问题（见 Node 前置 PATH 的解法），`npm ci` 本身正常。

**Path Alias（路径别名）**：`@/components/...` 替代相对路径串 `../../..`。在 SelfBlog 中：`tsconfig.json` 的 paths + Vite resolve.alias 双处声明；新增别名要两处同步，否则 type-check 过了构建挂。

**Semantic Versioning（语义化版本）**：`主.次.补` 三段式：主版本破坏兼容、次版本向后兼容新增、补丁只修 bug。在 SelfBlog 中：项目本体 v2.0.26，CHANGELOG 与 README 版本号同步维护；依赖版本则以 manifest 实际为准（文档常滞后）。

**Source Map（源码映射）**：把产物映射回源码的索引文件，报错栈与断点才能定位到 TS/Vue 源。在 SelfBlog 中：dev 构建内联 sourcemap；prod 按需产出——公开部署要不要挂 map，需权衡「可调试」与「暴露源码结构」。

**Static Type Checking（静态类型检查）**：编译期证明类型正确，把运行时 TypeError 提前到开发期。在 SelfBlog 中：TypeScript 6.0.3 + `vue-tsc --noEmit`；`tsconfig` 开 `strict` + **`noUncheckedIndexedAccess`**——索引访问返回 `T | undefined`，`arr[0].foo` 过不了 type-check，要写 `arr[0]?.foo` 或改用 `replace` 而非 `split()[0]`。

**Tree-shaking（摇树优化）**：以 ESM 静态结构为前提，打包时剔除未引用导出。在 SelfBlog 中：只 import 用到的函数（如从 lodash-es/工具模块按名导入）即可受益；`import * as` 会破坏摇树面。

**Version Pinning（版本钉死）**：依赖固定精确版本，规避「同一个 lock 不同结果」。在 SelfBlog 中：`pom.xml` 锁 Spring Boot 3.3.13、MyBatis Plus 3.5.8、jjwt 0.12.6；`package.json` 锁 Vue 3.5.44、Vite 6、TS 6.0.3——教程行文的版本断言以这些 manifest 为准。

**Workspace Discipline（工作区纪律）**：多 agent/多会话并行在同一目录工作时，文件可能随时出现或消失。在 SelfBlog 中：`docs/tutorial/` 下各章与附录由不同会话并行产出，写临时件要带独有前缀、合并后立刻校验存在性再删——「磁盘真值以最后一次验证为准」。

## 组九：测试

**Arrange-Act-Assert（准备-执行-断言）**：测试三段式结构：构造前置状态、执行被测行为、断言结果——一段一职责，读测试像读句子。在 SelfBlog 中：`AuthControllerTest` 等切片测试普遍遵循该结构；复杂前置（登录态、mock 数据）抽成 helper 保持三段清晰。

**Baseline（测试基线）**：当前必须保持的最低通过线，任何改动不得击穿。在 SelfBlog 中：后端 **588 tests / 0 failures**、前端 **43 文件 / 400 tests**，外加 `lint:check`、`format:check`、`type-check`、`test:deployment`(27/27)、`build` 全绿（2026-10-17 口径，以 CI 与最新验证记录为准）。

**Coverage（覆盖率）**：测试触及代码的比例（行/分支）。覆盖率是望不得满分的指标：100% 覆盖不等于 100% 断言。在 SelfBlog 中：仓库不设覆盖率门槛，以「关键链路必须有用例」为纪律——认证、发布、访客互动三条主链路各有专属测试文件。

**Edge Case（边界用例）**：输入域边缘：空串、超长、0、负数、Unicode、并发。在 SelfBlog 中：`normalizeSize` 把分页参数钳到上限 100、`visitorId` 正则长度 8–80、`ADMIN_PASSWORD` ≥12 位——每个阈值都应有越界用例。

**Fake（假实现）**：有真实逻辑的轻量替身（内存数据库、内存缓存），比 mock 更接近真实行为。在 SelfBlog 中：后端测试用 H2 替代真库（`DB_TYPE` 留空走 `src/test/resources/application.yml` + `schema.sql`）；Caffeine 在测试 profile 直接关闭而非替换。

**Fixture（测试夹具）**：可复用的测试前置数据与环境。在 SelfBlog 中：前端 `src/test/setup.ts` 是全局夹具——只桩了 `matchMedia` 与 `$router/$route`；改夹具前先想清楚它影响全部 43 个测试文件。

**Flaky Test（不稳定测试）**：时过时不过的测试，多半依赖真实时间/随机序/共享状态。在 SelfBlog 中：需要逐帧行为时不能把 rAF 桩成同步，要排队后手动 flush；改 `scrollY` 要 `Object.defineProperty` 再派发 `scroll` 事件——两套约定都是为消灭 flaky。

**Integration Test（集成测试）**：多个真实组件协同工作的测试，介于单元与端到端之间。在 SelfBlog 中：后端启动 Spring 上下文带 Security 链的 MVC 测试即此性质；`npm run test:deployment`（27 项）验证 Vercel 代理生成器与 Docker/Compose 契约，是部署层集成测试。

**Mock**：记录调用并按脚本回放的替身，用于隔离依赖与验证交互。在 SelfBlog 中：`@WebMvcTest` 必须 `@MockBean JwtUtils`——否则 `SecurityConfig` 拉起的 `jwtAuthenticationFilter` 找不到 Bean 直接启动失败；再加 `@WithMockUser(roles="admin")` 才返回 200。前端测试对 api 层做 vi.mock。

**MockMvc（MVC 测试工具）**：不启动真服务器、模拟 HTTP 请求打进 DispatcherServlet 的测试工具。在 SelfBlog 中：管理员控制器测试的标准载体；**不要用 `@AutoConfigureMockMvc(addFilters = false)`**——实测它返回 401 而不是绕过鉴权。

**Negative Testing（负例测试）**：主动测试「应该失败」的路径：403/404/校验拒绝/并发冲突。在 SelfBlog 中：方法级 `@PreAuthorize` 在切片里生效（非管理员 403、匿名 403 都有断言）；日志里大量 `[PERF] ... error:` 正是 `PerformanceMonitoringAspect` 记录的**正常负例断言输出**，不是失败。

**Regression Test（回归测试）**：每个修过的 bug 补一个用例，确保永不复发。在 SelfBlog 中：文章发布的唯一约束修复、评论区消毒顺序、游客身份链路等都伴随回归用例沉淀在测试目录——改老 bug 前先找它的回归用例。

**Snapshot Test（快照测试）**：把输出序列化为基线文件、变更时 diff。适合稳定结构，不适合频繁迭代 UI。在 SelfBlog 中：前端未采用快照测试，以显式断言为主——DOM 结构断言更抗重构。

**Stub（桩）**：返回固定值、不记录交互的最简替身。在 SelfBlog 中：前端 `setup.ts` 桩 `matchMedia`（jsdom 未实现）是典型 stub；后端对时间源、外部 API 的固定返回同理。

**Test Double（测试替身）**：dummy/stub/spy/mock/fake 五类替身的统称——dummy 占位、stub 回填、spy 记录、mock 断言交互、fake 带实现。在 SelfBlog 中：`@MockBean`（mock）、`setup.ts` 的 matchMedia（stub）、H2 内存库（fake）在同一个仓库里各司其职。

**Test-Driven Development（TDD，测试驱动开发）**：先写失败测试，再写最小实现令其通过，后重构——红/绿/重构循环。在 SelfBlog 中：仓库荣耻观明文「以跳过验证为耻，以主动测试为荣」；修 bug 的推荐顺序是先复现（写一个失败用例）再修复，让修复自带回归证明。

**Unit Test（单元测试）**：最小可测单元（函数/类）的隔离测试，快、定位准。在 SelfBlog 中：`HtmlSanitizer`、`SafeMarkdown`、`VisitorHashUtils`、`StringMaskUtils` 等纯函数类是理想单测对象——无依赖、无状态、输入输出即全部。

**Vitest（Vite 生态测试器）**：与 Vite 共享转换管线的测试运行器，API 兼容 Jest 且更快。在 SelfBlog 中：`npm run test:run` 即 `vitest run`，43 个文件 400 条用例；jsdom 环境跑 Vue 组件测试，注意 setup 未做自动卸载的约定（见 Fixture/Lifecycle）。

## 组十：部署与运维

**ADR（Architecture Decision Record，架构决策记录）**：记录「为什么这么设计」的时间戳文档：背景/决策/后果。在 SelfBlog 中：`docs/decisions/` 收录——但**当前行为以 CHANGELOG 与验证记录为准**，决策记录可能过时。（历史上的 `docs/superpowers/` 规格与计划目录已于 2026-10-19 清理。）

**Blue-Green Deployment（蓝绿部署）**：新旧两套环境并行，流量瞬时切换，回滚即切回。个人博客用不上全套，但其思想（可快速回滚的发布单元）仍适用。在 SelfBlog 中：单容器部署以「保留旧镜像 tag」实现最朴素回滚；SQLite 数据卷独立挂载，换镜像不丢数据。

**Build Artifact（构建产物）**：构建管线的最终交付物：`target/selfblog.jar`、`frontend/dist/`。产物应可追溯（版本号 + 构建时间），部署只认产物不认源码。在 SelfBlog 中：Maven `finalName=selfblog` 固定产物名；Docker 构建上下文是**仓库根**（`backend/Dockerfile` 在根上下文中找 jar）——`.dockerignore` 已删，构建上下文粒度靠 Dockerfile 路径控制。

**Container（容器）**：以镜像为模板的隔离运行环境，交付「环境本身」而非「环境说明书」。在 SelfBlog 中：`backend/Dockerfile` + 两个 compose 文件（仓库根上下文 + 真实 `selfblog.jar` + 数据/上传/相册卷）；**镜像构建与运行未在本机验证**（无 Docker 守护进程），CI docker job 是唯一已验证环境。

**Deployment Adapter（部署适配器）**：同一份源码面向不同托管平台的产物转换层。在 SelfBlog 中：双适配——Vercel 走 `frontend/vercel.json` 代理生成器（`framework: null` + `build-vercel-proxy.mjs` 产出 Build Output v3 forwarding，转发到 `SELFBLOG_BACKEND_ORIGIN`）；Docker/Compose 走后端镜像。`npm run test:deployment` 同时守住两份契约。

**Docker Compose（容器编排）**：一个 YAML 声明多容器（应用、数据库、反向代理）的本地编排。在 SelfBlog 中：`backend/docker-compose.yml` 挂载 `migration-selfblog.sql` 与数据卷；MySQL 场景 Compose 起库 + 应用双容器——但真机 MySQL 验证只有 CI 环境做过。

**Environment Variable（环境变量）**：运行时注入进程的配置，是 12-Factor App 的第一配置源。在 SelfBlog 中：`JWT_SECRET`（≥32 位）、`ADMIN_PASSWORD`（≥12 位）、`DB_TYPE`、`SITE_BASE_URL`、`SELFBLOG_BACKEND_ORIGIN`；`.env` 由 `DotenvConfig` 读入且**优先级高于系统环境变量**（Properties 语法，值不加引号）。

**Health Check（健康检查）**：探针端点回答「活着吗」（liveness）与「能干活吗」（readiness）。在 SelfBlog 中：`/actuator/health`（actuator starter 提供）；CI docker job 起容器后轮询它直到通过，再 grep 首页 HTML 验证 SEO 直出。

**Logging Discipline（日志纪律）**：结构化、分级、脱敏、可关。在 SelfBlog 中：`logback-spring.xml` 声明日志行为；`StringMaskUtils`/token 指纹防敏感串进日志；`LogUtils` 统一格式；`[PERF]` 慢日志来自 `PerformanceMonitoringAspect`。

**Observability（可观测性）**：日志（离散事件）、指标（聚合数值）、链路追踪（跨服务路径）三大支柱。在 SelfBlog 中：个人项目取最小集——logback 日志 + `[PERF]` 慢方法日志 + `/actuator/health`；没有指标系统，异常排查靠日志回放。

**Proxy Contract（代理契约）**：反向代理与后端之间隐含的约定：路径重写、头透传、超时、体积上限。在 SelfBlog 中：Vite dev 代理 `/api` 与 `/uploads` → `VITE_API_TARGET`；Vercel Build Output v3 forwarding → `SELFBLOG_BACKEND_ORIGIN`——`X-Forwarded-*` 的透传与 `SiteOrigin` 的防伪造直接相关。

**Read Replica / Write Forwarding（读写分离）**：读请求走副本、写走主库的水平扩展手段。在 SelfBlog 中：未使用——单实例 + SQLite/H2/MySQL 单主库，缓存（Caffeine 8 缓存）承担了读扩展职责，这是「先缓存后分库」的顺序观。

**Release Note / Changelog Discipline（发布说明纪律）**：每个可部署产物都要有对应 CHANGELOG 条目与版本号。在 SelfBlog 中：v2.0.26；「未发布」区累积、发版时定稿；影响文件表让回溯 diff 有索引。

**Resource Limits（资源限制）**：容器的 CPU/内存限额防止邻居干扰与 OOM 连锁。在 SelfBlog 中：compose 文件可配限额；SQLite 单写者 + Hikari 池 1 天然串行化写入，内存压力主要来自缓存上限（`maximumSize(50000)` 已设界）。

**Rollback（回滚）**：把系统退回上一个已知良好状态的预案。在 SelfBlog 中：镜像 tag 保留 + SQLite 卷独立 = 退镜像不退数据；`migration-selfblog.sql` 只前进不回退——数据库回滚靠「迁移前备份」，这是不可回避的运维纪律。

**Scheduled Downtime / Zero-Downtime（停机窗口与零停机）**：个人博客可接受短窗口发布；零停机要求滚动/蓝绿 + 健康检查 + 就绪探针。在 SelfBlog 中：单实例 `java -jar` 重启即停机（秒级）；Spring Boot 优雅停机（处理完在途请求再退出）值得开启。

**Single Point of Failure（单点故障）**：故障即全局不可用的组件。在 SelfBlog 中：整个后端就是单点（个人博客的理性取舍）；但关键数据有防线——SQLite 卷备份、`.env` 密钥异机保存、GitHub 托管源码。

**Site Reliability（站点可靠性实践）**：SRE 思想的轻量版：错误预算、容量规划、事后复盘（postmortem）。在 SelfBlog 中：`docs/reviews/` 的各批次验证记录就是「部署前复盘」的本地实践；已知未验证项（真实 Vercel、搜索引擎收录）都明文记录而非假装覆盖。

**TLS Certificate（TLS 证书）**：HTTPS 的身份凭证与加密材料，托管平台自动签发（Let's Encrypt 等）。在 SelfBlog 中：TLS 终结在 Vercel/反代层；后端只跑 HTTP；`SITE_BASE_URL` 必须与真实对外协议/域名一致，否则 canonical/sitemap/feed 全部产出错误 URL。

**Uptime Monitoring（可用性监控）**：外部视角的探活与告警（HTTP 200 + 延迟阈值）。在 SelfBlog 中：未内置——`/actuator/health` 可被任何外部监控服务（UptimeRobot 等）直接订阅，零代码接入。

**Volume（数据卷）**：容器外独立生存的持久化目录，容器销毁数据仍在。在 SelfBlog 中：compose 挂载三类卷——SQLite 数据库文件、上传媒体、相册文件；**备份策略 = 备份卷**，镜像与卷分离是「无状态容器 + 有状态数据」的教科书布局。

**Web Server / Embedded Server（Web 服务器与内嵌服务器）**：Spring Boot 内嵌 Tomcat（`server.port` 默认 8080，本仓库 8825），jar 即服务器，无需外挂 Tomcat。在 SelfBlog 中：`java -jar selfblog.jar` 直接起服务；生产建议置于反向代理之后（TLS、缓存、限流都由代理层补强）。

**12-Factor App（十二要素应用）**：云原生应用十二守则：配置入环境、依赖显式声明、无状态进程、日志事件流等。在 SelfBlog 中：配置（环境变量/.env）、依赖（manifest 锁版本）、日志（stdout）三要素合规；进程无状态（会话在 JWT、状态在库）——基本是按 12-Factor 的方向自然生长的。

---

## 按字母索引（A–Z）

> 仅列英文起始字母，括号内为所属组别（一 Java、二 JVM、三 Spring、四 Web、五 数据库、六 安全、七 前端、八 工程化、九 测试、十 部署）。

- **A**: Allowlist/Denylist（六）· Annotation Processing（一）· AOP（三）· Arrange-Act-Assert（九）· ADR（十）· Atomic Classes（二）· Auto-boxing（一）
- **B**: Baseline（九）· BaseMapper（五）· Batch Processing（五）· Base64（四）· BCrypt（六）· Blue-Green Deployment（十）· Brute Force Attack（六）· Build Artifact（十）· Bundle Analysis（八）· Busy Waiting（二）
- **C**: CAS（二）· Cache-Aside（五）· Cache Avalanche/Breakdown/Penetration（五）· CHANGELOG（八）· CI/CD（八）· CGLIB Proxy（三）· Clickjacking（六）· Code Review（八）· Composition API（七）· Composable（七）· Computed（七）· Conditional Beans（三）· Conventional Commits（八）· Connection Pool（五）· Container（十）· Coverage（九）· Credential Version（六）· CRUD（五）· CSRF（四）· CSP（四）· Cookie Attributes（四）· CORS（四）· Constructor Injection（三）· Context Switch（二）· CopyOnWrite（二）· Classpath Scanning（三）
- **D**: Dead Code Elimination（八）· Defense in Depth（六）· Deployment Adapter（十）· Diff/Patch（七）· Directive（七）· DTO/VO/PO/Entity（五）· Docker Compose（十）· DDL/DML（五）· Domain Separation（六）· Double Submit Cookie（六）· Deadlock（二）· Dependency Injection（三）
- **E**: Edge Case（九）· Encoding/Encryption/Hashing（六）· Environment Variable（十）· ESM/CJS（八）· ETag（四）· Executor Framework（二）· Envelope Pattern（四）
- **F**: Fail-closed（六）· Fake（九）· Flaky Test（九）· Fixture（九）· Formatter（八）· Forward/Reverse Proxy（四）
- **G**: Generics（一）· Ghost Dependency（八）· Gitignore Discipline（八）
- **H**: Happens-Before（二）· Hash（六）· HikariCP（五）· HMR（七）· HMAC（四）· Health Check（十）
- **I**: Idempotency（四）· Immutable（一）· Immutable Publication（二）· Index（五）· Input Validation（六）· Integration Test（九）· IoC（三）· Isolation Level（五）
- **J**: Jackson（三）· JDBC Template（三）· JMM（二）· JUC（二）· JIT（二）· JWT（四）· JWT Blacklist（六）· JWS/JWE（四）
- **K**: KV Table（五）· Keep-Alive（四）
- **L**: Lambda（一）· Lazy Loading Route（七）· Least Privilege（六）· Lifecycle Hooks（七）· Linter（八）· Logic Delete（五）· LRU（五）
- **M**: Memory Leak（二）· Migration Script（五）· MIME Type（四）· Mock（九）· MockMvc（九）· Monitor/Intrinsic Lock（二）· Monorepo/Polyrepo（八）· MyBatis（五）· MyBatis Plus（五）
- **N**: N+1 Query（五）· Negative Testing（九）· Node PATH（见八 workspace/package-lock 相关条目）
- **O**: Observability（十）· ORM（五）· OWASP（六）· Optimistic/Pessimistic Locking（二）· Origin/SOP（四）
- **P**: Package-lock/npm ci（八）· Pagination（五）· Password Hashing（六）· Password Policy（六）· Path Alias（八）· Path Traversal（六）· Pinia（七）· Principal（六）· Profile（三）· Property Source（三）· props/events（七）· Proxy Pattern（三）· Proxy Contract（十）
- **Q**: Query Wrapper（五）
- **R**: Race Condition（二）· RBAC（六）· Reactivity System（七）· Read Replica（十）· Record（一）· Reflection（一）· Regression Test（九）· Release Note（十）· Resource Limits（十）· REST（四）· Rollback（十）· Route Guard（七，见 Vue Router）
- **S**: Salt（六）· Sanitize vs Escape（六）· Scheduled Tasks（三）· Schema（五）· Sealed Class（一）· Secret Management（六）· Semantic Versioning（八）· Single Author Mode（六）· Single Point of Failure（十）· Site Reliability（十）· Slice Testing（三）· Snapshot Test（九）· Snowflake ID（五）· Source Map（八）· SPA（四）· SQL Injection（五）· SSR/SSG/ISR（七）· SSL/TLS Termination（四）· State Machine（七）· Static Type Checking（八）· Stream API（一）· String Handling（一）· Stub（九）· Switch Expression（一）· SFC（七）· Slot（七）
- **T**: Test Double（九）· TDD（九）· Text Block（一）· ThreadLocal（二）· Thread Pool Sizing（二）· Thread Safety（二）· Timing Attack（六）· TLS Certificate（十）· Token Fingerprint（六）· Tree-shaking（八）· TTL（五）· Transaction（五）· Transaction Propagation（三）· Threat Model（六）· Type Handler（五）
- **U**: Unit Test（九）· Uptime Monitoring（十）· Unique Constraint（五）· URI Design（四）
- **V**: Validation（三）· Var（一）· Version Field（五）· Version Pinning（八）· Vite（七）· Virtual DOM（七）· Visitor ID Hashing（六）· Volatile（二）· Vue Router（七）
- **W**: Watch（七）· Web Security Headers（四）· Web Server/Embedded（十）· Work Stealing（二）
- **X**: XSS（四）· XML（四，见 MIME/Accept）
- **Y**: YAML Configuration（三）
- **Z**: Zero-Downtime（十，见 Scheduled Downtime）

---

## 结语：怎么用这本地图

读代码的次序比读代码的努力更重要。建议的完整路径是：先用第一部分 §1.5（config/）与 §1.6（controller/）建立「请求从哪进、行为在哪定」的骨架；再沿五条后端路线中的**认证链路**走一遍——它是所有其他路线的前置；然后按你的实际任务挑路线（改文章 → 发布链路，改互动 → 访客链路，改 SEO → 页面链路），前端三条路线同理，从启动链路开始。

术语表则不是用来通读的。它按「组一 Java → 组十 部署」编排，本意是让读者在调试栈顶到底层之间跳跃时，每个名词都能在 SelfBlog 里找到自己的家。遇到本附录未收的词，优先在 `CLAUDE.md`（架构与约定）、`docs/agents/backend.md`/`frontend.md`（操作级细则）、`docs/decisions/`（为什么）里检索——它们与本附录共同构成这份教程的参考层。

最后一遍提醒：文中所有类名、行数、缓存名、TTL、测试基线均按 v2.0.26 实测。仓库继续演化后，**以 `pom.xml`、`package.json` 与实际目录为最高权威**——这也正是本附录教给你的第一条工程直觉。

---

# 附录C：面试题与自测题

> 本附录分两部分：**C.1 面试 90 题**按主题分组，每题附「答题要点」（面试官想听到什么）；**C.2 综合自测 35 题**供学完全书后检验，答案要点统一放在 C.3。题目全部围绕 SelfBlog 涉及的真实技术，杜绝「背了没用上」的悬空八股。

## C.1 面试题 90 题（按主题分组）

### Java 基础（1-15）

**1. == 和 equals 的区别？**
要点：== 比引用（基本类型比值）；equals 默认同 ==，重写后比内容。必须连带讲 hashCode 契约：重写 equals 必须重写 hashCode，否则 HashMap 行为错乱。

**2. String 为什么设计成不可变？**
要点：字符串常量池复用、hashCode 可缓存、线程安全、安全性（文件路径/类名不可被篡改）。代价是拼接产生新对象，循环拼接必须换 StringBuilder。

**3. HashMap 的底层结构与扩容机制？**
要点：数组+链表/红黑树（链长 ≥8 且数组 ≥64 树化）；默认容量 16、负载因子 0.75；扩容翻倍并 rehash（JDK 8 后按高位比特拆分链表）。追问点：容量为什么是 2 的幂（位运算取模）。

**4. ArrayList 和 LinkedList 的区别与选型？**
要点：ArrayList 连续内存随机访问 O(1)、中间插入 O(n)；LinkedList 双向链表头尾插 O(1)、随机访问 O(n)。实践结论：绝大多数场景 ArrayList 更快（缓存友好），LinkedList 的「插入快」仅在已知节点的迭代器位置成立。

**5. final / finally / finalize？**
要点：final 修饰类不可继承/方法不可重写/变量不可重赋值；finally 是异常处理的必然执行块（别在里面 return）；finalize 已废弃（Java 9 起），清理资源用 try-with-resources。

**6. 接口和抽象类的区别？什么时候用哪个？**
要点：接口是「能力契约」（可多实现、Java 8 后有 default 方法）、抽象类是「部分实现的模板」（单继承、有状态）。SelfBlog 例：Service 接口 + Impl 抽象层级；MyBatis Plus 的 ServiceImpl 是模板复用的范例。

**7. 重载和重写的区别？**
要点：重载（overload）同类同名不同参、编译期决定；重写（override）子类覆盖父类同签名方法、运行期多态绑定。返回类型可以协变。

**8. Java 的异常体系？受检与非受检的区别？**
要点：Throwable → Error（不该捕获）与 Exception；RuntimeException 及其子类非受检。SelfBlog 的选择：BusinessException extends RuntimeException——业务异常不该强迫调用方 try-catch，统一交给 GlobalExceptionHandler。

**9. 装箱缓存的坑？**
要点：Integer 缓存 -128~127，`Integer a=127,b=127; a==b` 为 true，128 时为 false。比较包装类型永远用 equals 或先拆箱。

**10. 深拷贝与浅拷贝？**
要点：浅拷贝复制对象但共享引用字段；深拷贝递归复制。实现：拷贝构造器、序列化、各字段手动 clone。

**11. static 关键字的全部用法？**
要点：静态字段（类级共享）、静态方法（无 this，不能访问实例成员）、静态代码块（类初始化）、静态内部类（不持外部引用，可安全用于单例 Holder）。

**12. 为什么 Java 是「值传递」？**
要点：Java 永远传值——传对象时传的是「引用的拷贝」，方法内 reassign 不影响外部，但通过引用修改对象内容会影响。经典考题：swap 两个 Integer 为什么失效。

**13. 枚举实现单例为什么天然线程安全且防反射/反序列化攻击？**
要点：枚举实例由 JVM 保证只初始化一次；反射创建枚举会抛 IllegalArgumentException；反序列化由 name() 恢复而非新建。

**14. hashCode 与 equals 的契约？**
要点：equals 相等则 hashCode 必须相等；hashCode 相等不推出 equals（碰撞）。违反后果：HashMap 里「存进去找不到」。

**15. String.intern() 的作用？**
要点：把字符串放入/引用常量池。面试常配 `new String("a")+new String("b")==ab` 类考题——JDK 7+ 常量池在堆中，intern 可能引用堆上已有对象。

### 集合与并发（16-25）

**16. fail-fast 与 fail-safe？**
要点：ArrayList/HashMap 迭代时被结构性修改抛 ConcurrentModificationException（modCount 检查）；CopyOnWriteArrayList 等按快照迭代是 fail-safe。SelfBlog 场景：SSE emitter 列表若用普通 ArrayList 并发增删就会踩中。

**17. ConcurrentHashMap 为什么线程安全且高效？**
要点：JDK 8 起 CAS + synchronized 锁单个桶头节点，代替分段锁；读无锁；size 用 CounterCell 分散计数。对照 Hashtable 全表锁的低效。

**18. synchronized 与 ReentrantLock 的区别？**
要点：synchronized JVM 内建、自动释放、优化后性能不差（偏向锁/轻量级锁/自旋）；ReentrantLock 功能全（可中断/超时/公平/多 Condition）、必须 finally 手动 unlock。SelfBlog 的 AnonymousLikeServiceImpl.toggle 用 synchronized 是「单机小并发」的够用解。

**19. volatile 的语义与局限？**
要点：保证可见性与禁止指令重排，**不保证原子性**（count++ 仍会丢更新）。典型应用：DCL 单例的实例字段、状态标志位。

**20. 线程池的核心参数与拒绝策略？**
要点：corePoolSize/maximumPoolSize/keepAliveTime/workQueue/threadFactory/RejectedExecutionHandler；四种拒绝策略（Abort/CallerRuns/Discard/DiscardOldest）。SelfBlog 的 AsyncConfig 用 CallerRunsPolicy：池满时由提交线程自己执行，天然反压不丢任务。

**21. ThreadLocal 的原理与内存泄漏风险？**
要点：每个 Thread 有 ThreadLocalMap，key 是 ThreadLocal 的弱引用、value 是强引用——线程池场景线程不死，value 泄漏，用完必须 remove()。

**22. CompletableFuture 与 Future 的区别？**
要点：Future 只能 get 阻塞；CompletableFuture 支持回调链（thenApply/thenCompose/thenCombine）、异常传播（exceptionally）与手动完成，是编排异步依赖的正确工具。

**23. 什么是 CAS？ABA 问题怎么解决？**
要点：Compare-And-Swap 硬件原子指令；ABA 用版本号解决（AtomicStampedReference）。

**24. HashMap 在并发下的问题？**
要点：JDK 7 头插法扩容可成环死循环；JDK 8 改尾插不成环但仍丢数据——并发场景直接 ConcurrentHashMap。

**25. 如何让三个线程顺序执行？**
要点：join()、单线程池、CompletableFuture.thenCompose 链、CountDownLatch 串联。说清各自适用层级（简单/任务编排/复杂依赖图）。

### JVM（26-33）

**26. JVM 内存区域划分？哪些线程共享？**
要点：堆、方法区（元空间）共享；虚拟机栈、本地方法栈、程序计数器线程私有。OOM 多发于堆；StackOverflow 多发于栈。

**27. 对象什么时候进入老年代？**
要点：年龄阈值（默认 15）、大对象直接进（-XX:PretenureSizeThreshold）、动态年龄判断（同龄对象超 Survivor 一半）、Survivor 放不下。

**28. 如何判断对象可以被回收？**
要点：可达性分析（GC Roots：栈帧局部变量、静态字段、JNI 引用等）；引用计数（Java 不用，循环引用问题）。

**29. G1 收集器的特点？**
要点：Region 化堆、可预测停顿目标（-XX:MaxGCPauseMillis）、Mixed GC 回收老年代优先级按垃圾比例。JDK 9+ 默认。

**30. 排查 OOM 的步骤？**
要点：保留现场（-XX:+HeapDumpOnOutOfMemoryError）→ jmap/jstat 看堆概况 → MAT 分析支配树找大对象 → 定位持有链。SelfBlog 教程附录 D 有工具箱实操。

**31. 双亲委派模型是什么？为什么这样设计？**
要点：加载请求逐级上抛，父加载器无法完成才自己加载。保证核心类唯一与安全（自定义 java.lang.String 不会被加载）。打破方式：SPI（线程上下文类加载器）、热部署。

**32. 类加载过程？**
要点：加载（读字节码）→ 验证 → 准备（静态字段分配默认值）→ 解析（符号引用转直接引用）→ 初始化（<clinit>，静态赋值与静态块）。触发初始化的时机（new、访问静态字段、反射等）。

**33. 频繁 Full GC 怎么排查？**
要点：jstat -gcutil 看各代占用与 FGC 频率 → 判断是内存不足/内存泄漏/大对象/元空间扩展 → 对症：调堆、修泄漏、拆大对象。

### Spring / Spring Boot（34-60）

**34. Spring IoC 的理解？**
要点：对象创建与依赖装配的控制权从业务代码反转给容器；收益是解耦与可测试（注入 mock）。结合「构造器注入优于字段注入」讲：依赖不可变、必要依赖强制、便于单测。

**35. Bean 的生命周期？**
要点：实例化 → 属性填充 → Aware 回调 → BeanPostProcessor 前置 → @PostConstruct/afterPropertiesSet → 自定义 init → BeanPostProcessor 后置（**AOP 代理在此生成**）→ 使用 → @PreDestroy 销毁。

**36. 三级缓存解决循环依赖的原理？为什么构造器注入不行？**
要点：一级成品、二级半成品、三级 ObjectFactory（提前暴露引用以支持 AOP 代理替换）。构造器注入在实例化阶段就需要对方，任何缓存都帮不上——只能 @Lazy 或重构。

**37. @Autowired 的注入过程？**
要点：先按类型（byType）找候选 → 多个时按字段/参数名匹配 → @Qualifier/@Primary 优先。找齐多个匹配 → NoUniqueBeanDefinitionException。

**38. Spring Boot 自动配置的原理？**
要点：@SpringBootApplication 三合一；@EnableAutoConfiguration 读 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（2.7+ 新位置）→ 按各配置类上的 @ConditionalOnClass/@ConditionalOnMissingBean 过滤。SelfBlog 例：DatabaseConfig 用 @ConditionalOnProperty("DB_TYPE") 实现三库切换。

**39. @Conditional 系列注解知道哪些？**
要点：OnClass（classpath 有某类）、OnMissingBean（用户没自定义才生效——starter 的谦让逻辑）、OnProperty（配置开关）。

**40. 事务传播行为 REQUIRED 与 REQUIRES_NEW 的区别？**
要点：REQUIRED 加入当前事务（一起回滚）；REQUIRES_NEW 挂起当前、开新事务（独立提交/回滚）。典型场景：操作日志「业务回滚了日志也要留」用 REQUIRES_NEW。

**41. 事务失效的场景（至少说出四个）？**
要点：① 同类内部调用（不走代理）；② 非 public 方法；③ 异常被 catch 吞掉；④ 默认不回滚受检异常（没写 rollbackFor）；⑤ 传播行为配错；⑥ 数据库引擎不支持。展开任一条的原理（AOP 代理机制）是加分项。

**42. Spring AOP 的实现原理？什么时候用 JDK 代理，什么时候 CGLIB？**
要点：接口默认 JDK 动态代理（实现接口）、无接口用 CGLIB 子类代理；Boot 2.x 起默认全 CGLIB。验证方法：打印 bean.getClass() 看到 `$$SpringCGLIB$$` 后缀（附录 D.5 实操）。

**43. Spring MVC 处理请求的流程？**
要点：DispatcherServlet 收请求 → HandlerMapping 找 Handler（含拦截器链）→ HandlerAdapter 调用 Controller → HttpMessageConverter 序列化 → 返回ModelAndView/响应体 → 异常走 HandlerExceptionResolver。

**44. 拦截器与过滤器的区别？**
要点：Filter 在 Servlet 容器层（before DispatcherServlet）、能过滤所有请求（含静态资源）、由容器管理；Interceptor 在 Spring MVC 层、能拿到 Handler 信息、由 Spring 管理、可用注入。执行顺序：Filter → Interceptor → AOP。

**45. @RestControllerAdvice 的作用？**
要点：全局增强器三用法：@ExceptionHandler 统一异常、@InitBinder 绑定定制、@ModelAttribute 全局属性。SelfBlog 的 GlobalExceptionHandler 把 BusinessException 的业务码映射到 HTTP 状态并兜底 500 不漏堆栈。

**46. Bean 作用域有哪些？单例的线程安全问题？**
要点：singleton（默认）/prototype/request/session/application。单例+可变状态=线程隐患，所以 Service 保持无状态（依赖注入的都是无状态协作对象），SelfBlog 全项目遵守此纪律。

**47. @Value 与 @ConfigurationProperties 的区别？**
要点：@Value 单值、支持 SpEL、松散绑定弱；@ConfigurationProperties 批量绑定、类型安全、支持校验与 IDE 提示。SelfBlog 的 RateLimitProperties 是标准范例。

**48. Spring Boot 的启动流程？**
要点：SpringApplication.run → 推断应用类型 → 加载 ApplicationContextInitializer/Listener → 准备 Environment（配置源合并）→ 创建容器 → refresh（含自动配置）→ 回调 Runner。能讲出 refresh 里「onRefresh→finishBeanFactoryInitialization」的位置即可。

**49. BeanFactory 与 FactoryBean 的区别？**
要点：BeanFactory 是容器接口；FactoryBean 是「生产 Bean 的 Bean」（getObject()），MyBatis 的 SqlSessionFactoryBean、代理接口的 MapperFactoryBean 都靠它。

**50. @Async 的原理与失效场景？**
要点：AOP 代理把方法提交线程池；同类内部调用失效、返回值需 Future/void、默认 SimpleAsyncTaskExecutor（不复用线程！）要自定义线程池。SelfBlog 的 AsyncConfig 定制了 CallerRunsPolicy。

**51. 如何自定义 Starter？**
要点：autoconfigure 模块（@ConditionalOnMissingBean 的 XxxService + properties 类）+ starter 模块（聚合依赖）+ AutoConfiguration.imports 注册。讲清「默认给实现、用户可覆盖」的谦让哲学。

**52. Spring 事件机制的应用场景？**
要点：ApplicationEventPublisher 发布、@EventListener 消费；解耦「主流程与副流程」（发文成功→发通知）。跨事务语义要注意（默认同步同事务）。

**53. 循环依赖除了三级缓存还有哪些解法？**
要点：@Lazy 注入（注入代理）、setter 注入、重构抽第三类（首选——依赖成环说明职责划分有问题）。

**54. Spring Security 的过滤器链理解？**
要点：SecurityFilterProxy 桥接容器与 Spring；十几层 Filter 依次处理（SecurityContext/CORS/认证/授权…）；自定义 JWT 过滤器插入 UsernamePasswordAuthenticationFilter 之前。SelfBlog 的 JwtAuthenticationFilter「只解析不拒绝」——把「是否放行」的决定权留给授权规则。

**55. JWT 的结构与时效设计？**
要点：Header.Payload.Signature；Payload Base64URL 编码**可解码非加密**（别放敏感信息）；access token 短命 + refresh token 长命。SelfBlog 单体博客简化为单一 token + 前端本地 exp 校验（validateToken 只解码不联网）。

**56. 如何防止接口被恶意刷？**
要点：分层防线——限流（固定窗口/滑动窗口/令牌桶）→ 参数校验 → 行为验证（频率分析）→ 幂等设计。SelfBlog：RateLimitInterceptor 按路径前缀 1 分钟窗口 + Caffeine 计数 + 429 JSON。

**57. 缓存穿透/击穿/雪崩的区别与对策？**
要点：穿透=查不存在数据（空值缓存/布隆过滤器/参数前置校验）；击穿=热点 key 过期瞬间并发冲击（互斥重建/逻辑过期）；雪崩=大量 key 同时失效（TTL 抖动/分级 TTL/多级兜底）。SelfBlog 用差异化 TTL 8 缓存天然错峰。全见 3.11.4。

**58. 接口幂等性的实现方案？**
要点：唯一约束兜底、token 去重、状态机（只允许合法迁移）、乐观锁版本号。SelfBlog 范例：匿名点赞三态收敛（同一行软删/复活）天然幂等。

**59. 为什么读服务要 readOnly 事务？**
要点：驱动/容器可做优化（不脏检查、只读路由）、语义明确、防误写。SelfBlog 的 Query 服务全部 @Transactional(readOnly=true)。

**60. 单体应用的分层架构，Service 层为什么要再拆门面/查询/交互？**
要点：职责单一（CRUD 编排 vs 只读查询 vs 副作用计数）、事务边界清晰（readOnly vs rollbackFor）、缓存注解落位明确。这是 SelfBlog 最大的架构特色，能主动展开是亮点。

### 数据库与 MyBatis（61-72）

**61. MyBatis 中 #{} 与 ${} 的区别？**
要点：#{} 预编译占位符防注入；${} 字符串拼接只用于表名/列名等动态标识符（且必须白名单校验）。答出「ORDER BY 列名只能 ${} + 白名单」是亮点。

**62. MyBatis 一级/二级缓存？**
要点：一级 SqlSession 级默认开启（Spring 集成下每次请求新 session，基本无感）；二级 namespace 级需配置、跨 session、更新会清。Spring Boot 项目通常直接用 Spring Cache 替代二级缓存。

**63. 逻辑删除的优缺点？**
要点：优点可追溯/可恢复/外键友好；缺点查询都要带条件（手写 SQL 易漏 is_deleted=0）、表膨胀。SelfBlog 用 @TableLogic + MetaObjectHandler + 「XML 必带 is_deleted=0」纪律应对。

**64. 什么是 N+1 查询？怎么解决？**
要点：列表 1 次查询 + 循环内每项 1 次关联查询。解法：批量查（selectBatchIds 收集去重 id）+ 内存 Map 关联（SelfBlog getAdminComments 的写法）、JOIN 一次取回、或 MP 的关联插件。

**65. 数据库索引的原理？为什么用 B+ 树？**
要点：B+ 树矮胖（3 层约 2000 万行）、叶子链表利于范围查询、非叶子节点不存数据使单页容纳更多键。对比哈希（无范围）、B 树（范围查询需回溯）。

**66. 聚簇索引与非聚簇索引？什么是回表？**
要点：InnoDB 主键索引叶子存整行（聚簇）；二级索引叶子存主键值，命中后需回表取整行。覆盖索引（查询列全在索引中）可免回表。

**67. 什么是最左前缀原则？**
要点：联合索引 (a,b,c) 只能按 a / a,b / a,b,c 命中；b 单独或 b,c 无法使用。设计联合索引时把等值查询列放前、范围列放最后。

**68. 事务的四大特性（ACID）与隔离级别？**
要点：原子/一致/隔离/持久；隔离级别由弱到强 读未提交（脏读）→ 读已提交（不可重复读）→ 可重复读（MySQL 默认，幻读靠间隙锁抑制）→ 串行化。

**69. 悲观锁与乐观锁？**
要点：悲观=先锁再操作（SELECT ... FOR UPDATE）；乐观=版本号 CAS，冲突时重试。读多写少用乐观，写冲突激烈用悲观。SelfBlog 的点赞计数用「应用层串行化（synchronized）+ 计数冗余」的诚实方案，可对比展开。

**70. 慢 SQL 优化的一般步骤？**
要点：慢查询日志定位 → EXPLAIN 分析（type/rows/Extra，防全表扫描与 filesort）→ 加/改索引或改写 SQL → 验证。说出「先量再优、别拍脑袋加索引」的流程观。

**71. 为什么连接池是必需的？HikariCP 快在哪？**
要点：TCP+认证握手昂贵，池化复用；Hikari 用 FastList 代替 ArrayList、字节码优化代理、无锁设计。SelfBlog SQLite 池大小 1 的特殊语境（单文件库串行写）。

**72. 分库分表什么时候才需要？**
要点：单表数据量/B+树层级/索引效率的实际拐点（经验值千万级）+ 单机 IO/内存瓶颈。反问「个人博客需要吗」展示判断力——SelfBlog 明确不需要，过度设计是负资产。

### TypeScript / 前端工程（73-84）

**73. interface 与 type 的区别？**
要点：interface 可声明合并、可 extends、描述对象/类；type 是类型别名（联合/元组/条件类型都能表达）。惯例：对象形状用 interface、组合运算用 type。SelfBlog 的 types/common.ts 两者都有。

**74. unknown 与 any 的区别？**
要点：any 放弃检查可任意赋值；unknown 是「顶类型」，赋值前必须收窄——类型安全的 any。IIFE 收窄 `typeof e === 'string' ? e : String(e)` 是常用模式。

**75. 类型收窄的手段有哪些？**
要点：typeof/instanceof/in/字面量相等/判空/自定义类型谓词 `x is T`。SelfBlog 的 isRetryableError(err): err is NetworkError 是真实范例。

**76. 泛型的作用与常用工具类型？**
要点：类型的参数化。Partial/Required/Pick/Omit/Record/ReturnType 必须信手拈来，说得出各自的映射类型实现。

**77. TS 枚举与字面量联合类型的取舍？**
要点：enum 有运行时对象（可反向映射）、const enum 内联；字面量联合零运行时、与 Record 联用获得穷举检查。现代 TS 更倾向字面量联合 + as const。

**78. noUncheckedIndexedAccess 解决什么问题？**
要点：arr[0] 类型变 T|undefined，强制处理「空数组取值」。SelfBlog 全项目开启，用 ?. 或提前 return 应对——换来线上零「undefined 上取属性」崩溃。

**79. 装饰器的原理？Spring 注解与它的对照？**
要点：装饰器是接收目标并返回目标的函数（实验性提案，TS 5 起标准化推进中）。对照：@Component≈类装饰器注册、@Inject≈参数装饰器——理解 Spring 注解不是魔法，是元数据 + 反射。

**80. Vite 为什么启动快？**
要点：dev 用浏览器原生 ESM 按需编译（只编译当前路由碰到的模块），不做全量打包；依赖预构建（esbuild，Go 编写比 JS 快 10-100 倍）；HMR 精确到模块替换。build 时才切换 Rollup 全量打包。

**81. Tree-shaking 的条件？**
要点：ESM 静态结构（import 必须顶层、路径静态）+ 副作用声明（package.json sideEffects）。CJS 动态 require 无法摇。这就是「前端库推荐 ESM 版本」的原因。

**82. ESLint 与 Prettier 为什么都用？**
要点：ESLint 管质量规则（bug 前兆），Prettier 管格式（统一排版），两者正交。冲突由 eslint-config-prettier 关掉 ESLint 的格式规则解决。SelfBlog 五道闸门见 6.7.6。

**83. 前端如何做错误监控？**
要点：window.onerror/unhandledrejection 全局捕获 → 上报（含 source map 版本）→ 聚合分析。Axios 层统一 error.userMessage 文案映射（SelfBlog 拦截器已实现）。

**84. 什么是 CSRF？前后端分离项目怎么防？**
要点：借用户已登录身份伪造请求。STATELESS API 用 token 头（无 cookie 自动免疫）；用 cookie 则 SameSite + CSRF token。SelfBlog：Authorization 头 + X-XSRF-TOKEN 双通道，拦截器自动注入。

### Vue（85-90）

**85. ref 与 reactive 的区别与选择？**
要点：ref 包装任意值（.value 访问、赋值整体替换保持响应性）；reactive 只对象（解构丢响应性）。惯例：基础值/需整体替换用 ref，状态聚合用 reactive/computed 组合。Pinia 里用 ref 风格更普遍。

**86. computed 与 watch 的区别？**
要点：computed 声明派生值（缓存、同步求值）；watch 声明副作用（异步、可拿新旧值）。误用信号：在 watch 里给 ref 赋值（多半该用 computed）；在 computed 里发请求（副作用必须 watch/watchEffect）。

**87. Vue 3 组合式 API 解决了 Options API 的什么问题？**
要点：跨生命周期的同一逻辑分散在 data/methods/computed 各处（「跳来跳去」）；组合式按逻辑关注点聚拢 + 可抽离 composable 复用。大组件与逻辑复用场景收益最大。

**88. Pinia 相比 Vuex 好在哪？SelfBlog 的 store 设计？**
要点：无 mutation、天然 TS 支持、组合式写法、模块化即多个 store。SelfBlog 五 store 职责表（site 站点/theme 主题/user 鉴权/music 音频/pages 页面清单）；id='auth' 的命名坑；storeToRefs 保持解构响应性。

**89. 路由守卫的执行顺序与鉴权方案？**
要点：全局 beforeEach → 路由独享 beforeEnter → 组件内 beforeRouteEnter。SelfBlog：meta.requiresAuth/requiresAdmin 标记 + 全局守卫校验 + redirect 参数回跳；adminPath 动态注册的运行时路由是特色考点。

**90. 前端如何实现「乐观更新」？**
要点：先改本地状态立刻反馈 → 发请求 → 成功对齐服务端 / 失败回滚 + 提示。SelfBlog 的 LikeButton：本地翻转 → toggle → 失败回滚 + 服务端计数校准。能讲出「校准而非信任服务端原样覆盖」是深度信号。

## C.2 综合自测 35 题（学完全书后检验）

**基础概念（1-8）**

1. JDK、JRE、JVM 三者的包含关系是什么？SelfBlog 为什么指定 JDK 21？
2. `String s = new String("ab")` 创建了几个对象？
3. ArrayList 扩容机制？为什么遍历多、增删少的场景选它？
4. try-with-resources 相比 try-finally 的优势？举一个 SelfBlog 会用到的场景。
5. Lambda 表达式的本质是什么？它和匿名内部类的区别？
6. 为什么 `Integer a=128, b=128; a==b` 是 false？
7. 受检异常与非受检异常的设计意图？BusinessException 属于哪种、为什么？
8. 枚举比常量类强在哪里？

**Java 进阶（9-14）**

9. 双亲委派模型的作用？什么场景会打破它？
10. G1 与 CMS 的核心区别？JDK 21 默认收集器是什么？
11. 线程池提交任务后，core、queue、max 的扩张顺序是什么？
12. synchronized 锁升级的路径是什么？
13. volatile 能替代锁吗？给一个必须用锁的例子。
14. ThreadLocal 在线程池里为什么会泄漏？怎么防？

**Spring（15-22）**

15. 描述一个 Bean 从创建到销毁的完整生命周期（说出 AOP 代理生成的时机）。
16. 为什么构造器注入是官方推荐？字段注入的两个坏处？
17. @ConditionalOnMissingBean 的「谦让」逻辑是什么？为什么 starter 离不开它？
18. SelfBlog 如何用 @ConditionalOnProperty 实现 SQLite/H2/MySQL 三库切换？
19. 事务传播行为 REQUIRED 和 NESTED 的区别？
20. 列出至少 4 个事务失效场景，并解释「同类内部调用失效」的根本原因。
21. @Around 环绕通知里忘记调用 proceed() 会发生什么？
22. @Value("${JWT_SECRET:local-visitor-hash}") 这个写法各部分是什么含义？

**Web 与安全（23-28）**

23. 一个 HTTP 请求进入 Spring Boot 后经过哪些组件（过滤器/拦截器/控制器）？
24. Result<T> 信封里各字段的语义？为什么分页还要 records/total 等额外字段？
25. JwtAuthenticationFilter「只解析不拒绝」的设计好处是什么？
26. JWT 的 Payload 为什么不能放密码？
27. SecurityUtils.getCurrentUserId() 与 getCurrentUserIdOrNull() 分别用在什么场景？
28. 固定窗口限流的「窗口边界突刺」问题是什么？SelfBlog 为什么接受它？

**数据库（29-32）**

29. @TableLogic 修饰的字段，MyBatis Plus 自带方法与手写 XML 的行为差异是什么？
30. 写出「查询某文章下已审核评论，按时间倒序，防注入」的 LambdaQueryWrapper 代码。
31. databaseId 机制下，H2 测试为什么只会走无 databaseId 的语句？
32. 分页插件没配置时 selectPage 会发生什么？怎么验证分页真的生效？

**前端（33-35）**

33. main.ts 的启动顺序为什么是「settings → adminRoutes → restore → use(router) → mount」？交换前两步会发生什么？
34. api/index.ts 拦截器返回后端信封本身，这对调用方代码意味着什么？「res.data.data」错在哪？
35. LikeButton 的乐观更新分几步？每一步防什么问题？

## C.3 自测题答案要点

**1** JDK ⊃ JRE ⊃ JVM；21 是 LTS 且支持虚拟线程，SelfBlog pom 锁定 21。
**2** 常量池若无 "ab" 则 2 个（池中一个 + 堆中一个）；有则 1 个。
**3** 1.5 倍扩容 + Arrays.copyOf；连续内存缓存友好、随机访问 O(1)。
**4** 自动关闭资源、异常抑制不丢失；如文件上传的临时流。
**5** 函数式接口实例的语法糖；this 语义不同（Lambda 无独立 this）。
**6** 超出缓存范围 [-128,127] 各自 new。
**7** 受检强迫处理、适合可恢复场景；非受检表达程序/业务错误；BusinessException 是非受检——统一上抛给全局处理器而非逐层 try-catch。
**8** 类型安全、可带字段方法、防反射/序列化绕过。
**9** 防核心类被篡改、保证唯一；SPI/热部署/容器场景打破（线程上下文类加载器）。
**10** G1 Region 化可预测停顿 vs CMS 并发标记清除有碎片；JDK 21 默认 G1（ZGC 为低延迟备选）。
**11** core 满 → 入队 → 队满 → 扩到 max → 拒绝策略。
**12** 无锁 → 偏向 → 轻量级（CAS 自旋）→ 重量级（monitor）。
**13** 不能：volatile 无原子性；count++ 或 check-then-act 必须锁或 CAS。
**14** key 弱引用 value 强引用、线程不死 value 不回收；用完 remove() 或 try-finally。
**15** 实例化→填充→Aware→BPP 前→@PostConstruct→init→BPP 后（代理生成）→使用→@PreDestroy。
**16** 不可变、强制依赖显式化、便于构造 mock；字段注入隐藏依赖、难单测、易循环依赖。
**17** 用户已定义同类型 Bean 时自动退位；starter 的「可覆盖默认值」机制。
**18** DatabaseConfig @ConditionalOnProperty("DB_TYPE") 三分支创建对应数据源；不设默认 SQLite。
**19** REQUIRED 共生共死；NESTED 保存点，内层可独立回滚到保存点。
**20** 见 C.1 第 41 题要点；根因 = this 调用绕过 AOP 代理，事务拦截器根本没机会介入。
**21** 目标方法永不执行，通知返回的值（null）直接成为调用结果。
**22** 读配置项 JWT_SECRET，不存在时用默认值 local-visitor-hash（本地开发兜底，配合 visitor 哈希盐）。
**23** Filter 链（含 Security 过滤器链）→ DispatcherServlet → Interceptor preHandle → Controller →（返回）→ Interceptor postHandle → 序列化 → afterCompletion。
**24** code 业务码/message/data/timestamp；分页把「一页记录 + 定位信息」整体表达，前端无需再算偏移。
**25** 过滤器只负责「认出你是谁」，放行与否交给 authorizeHttpRequests 集中决策——单一职责，规则可审计。
**26** Base64 可逆解码，非加密；敏感信息应放签名 Claim 或根本不放。
**27** 前者强制登录（抛 401），用于必须登录的写操作；后者游客语义（返回 null 走游客分支），如详情页判断是否作者本人。
**28** 窗口边界两倍流量突刺；实现简单内存可控，博客场景可接受，高要求换滑动窗口/令牌桶。
**29** 自带方法自动拼 is_deleted=0；XML 完全不感知，必须手写条件。
**30** `new LambdaQueryWrapper<GuestComment>().eq(GuestComment::getPostId, postId).eq(GuestComment::getStatus, 1).orderByDesc(GuestComment::getCreateTime)`。
**31** VendorDatabaseIdProvider 识别不出 H2 的 databaseId，匹配不到带 databaseId 的语句时回退无标记语句；MySQL 专有语法放共享语句就会炸。
**32** 静默退化查全表；断言 pageSize=1 时 SQL 日志出现 COUNT + LIMIT 两条，或返回的 size=1 而非 total。
**33** 公开设置决定 adminPath（动态路由的路径依据），必须先于路由注册；restore 必须先于 use(router) 否则首帧守卫把已登录用户弹回登录页。交换前两步：settings 拉取失败时 adminPath 用默认值，真实自定义路径 404。
**34** 调用方拿到的已是 {code,message,data}，取数据用 res.data；res.data.data 多拆一层返回 undefined。
**35** 三步：本地状态先翻转（即时反馈）→ 请求成功后用服务端值校准（防漂移）→ 失败回滚并提示（防假状态）。乐观的是「反馈延迟」，不是「数据正确性」。

---

**使用建议**：面试前按主题过一遍 C.1 的「要点」栏；自测时先答再翻 C.3——答错的知识点回到对应章节重读，比多刷十道新题有效。

---

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
