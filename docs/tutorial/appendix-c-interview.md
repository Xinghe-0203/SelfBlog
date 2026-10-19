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
