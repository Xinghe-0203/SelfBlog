package com.selfblog.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 性能监控切面
 * 自动记录Service层和Controller层方法的执行时间
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    /** 慢查询阈值（毫秒） */
    private static final long SLOW_THRESHOLD = 1000;

    /** 超慢查询阈值（毫秒） */
    private static final long VERY_SLOW_THRESHOLD = 3000;

    /**
     * 切入点：匹配所有Service层方法
     */
    @Pointcut("execution(* com.selfblog.service..*(..))")
    public void serviceLayer() {
    }

    /**
     * 切入点：匹配所有Controller层方法
     */
    @Pointcut("execution(* com.selfblog.controller..*(..))")
    public void controllerLayer() {
    }

    /**
     * 切入点：匹配所有Mapper层方法
     */
    @Pointcut("execution(* com.selfblog.mapper..*(..))")
    public void mapperLayer() {
    }

    /**
     * 环绕通知：记录Service层方法执行时间
     */
    @Around("serviceLayer()")
    public Object monitorService(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorMethod(joinPoint, "SERVICE");
    }

    /**
     * 环绕通知：记录Controller层方法执行时间
     */
    @Around("controllerLayer()")
    public Object monitorController(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorMethod(joinPoint, "CONTROLLER");
    }

    /**
     * 环绕通知：记录Mapper层方法执行时间
     */
    @Around("mapperLayer()")
    public Object monitorMapper(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorMethod(joinPoint, "MAPPER");
    }

    /**
     * 通用方法监控逻辑
     *
     * @param joinPoint 连接点
     * @param layer     层级名称（SERVICE/CONTROLLER/MAPPER）
     * @return 方法返回值
     * @throws Throwable 方法可能抛出的异常
     */
    private Object monitorMethod(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethod = className + "." + methodName;

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // 根据执行时间记录不同级别的日志
            if (duration > VERY_SLOW_THRESHOLD) {
                log.warn("[PERF] {} {} execution time: {}ms (VERY SLOW)", layer, fullMethod, duration);
            } else if (duration > SLOW_THRESHOLD) {
                log.info("[PERF] {} {} execution time: {}ms (SLOW)", layer, fullMethod, duration);
            } else {
                log.debug("[PERF] {} {} execution time: {}ms", layer, fullMethod, duration);
            }

            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[PERF] {} {} execution time: {}ms, error: {}",
                    layer, fullMethod, duration, ex.getMessage());
            throw ex;
        }
    }
}
