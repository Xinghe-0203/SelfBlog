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

