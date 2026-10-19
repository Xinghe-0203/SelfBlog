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
