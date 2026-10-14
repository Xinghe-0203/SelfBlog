package com.selfblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置类
 * 线程池参数可通过 application.yml 中的 async.task.* 和 async.notification.* 配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.task.core-pool-size:5}")
    private int taskCorePoolSize;

    @Value("${async.task.max-pool-size:10}")
    private int taskMaxPoolSize;

    @Value("${async.task.queue-capacity:200}")
    private int taskQueueCapacity;

    @Value("${async.task.keep-alive-seconds:60}")
    private int taskKeepAliveSeconds;

    @Value("${async.notification.core-pool-size:3}")
    private int notificationCorePoolSize;

    @Value("${async.notification.max-pool-size:8}")
    private int notificationMaxPoolSize;

    @Value("${async.notification.queue-capacity:500}")
    private int notificationQueueCapacity;

    @Value("${async.notification.keep-alive-seconds:30}")
    private int notificationKeepAliveSeconds;

    /**
     * 通用异步任务执行器
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(taskCorePoolSize);
        executor.setMaxPoolSize(taskMaxPoolSize);
        executor.setQueueCapacity(taskQueueCapacity);
        executor.setThreadNamePrefix("async-task-");
        executor.setKeepAliveSeconds(taskKeepAliveSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 通知发送专用执行器
     */
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(notificationCorePoolSize);
        executor.setMaxPoolSize(notificationMaxPoolSize);
        executor.setQueueCapacity(notificationQueueCapacity);
        executor.setThreadNamePrefix("notification-");
        executor.setKeepAliveSeconds(notificationKeepAliveSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
