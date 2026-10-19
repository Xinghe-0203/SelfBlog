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
