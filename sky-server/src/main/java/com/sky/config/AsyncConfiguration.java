package com.sky.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 【学习笔记】异步线程池配置类
 * 面试必背：线程池核心参数
 * 
 * 为什么不用 @Async 默认的线程池？
 * 答：因为 SpringBoot 默认提供的是 SimpleAsyncTaskExecutor。
 * 它不是真的池子，每次有任务它都 new 一个全新线程去处理，如果并发一万个，
 * 就会直接创建一万个线程，极易把内存撑爆（OOM）。所以企业里强制要求自定义！
 */
@Configuration
@EnableAsync // 开启 Spring 对 @Async 功能的支持
public class AsyncConfiguration {

    @Bean("customExecutor")
    public Executor customExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：池子里雷打不动一直在的员工，哪怕没活干也待命（平时服务这个量够了）
        executor.setCorePoolSize(10); 
        // 最大线程数：遇到大促！10个人干不完了，可以临时再招临时工，总人数最多20人
        executor.setMaxPoolSize(20);  
        // 队列容量：如果20个人都在忙，还有新任务进来，就去门口排队，最多排 50 个人
        executor.setQueueCapacity(50); 
        // 线程命名前缀：出了 Bug 看日志，一看名字就知道是这批线程出错了，方便排查
        executor.setThreadNamePrefix("sky-async-pool-"); 
        // 拒绝策略：排队的 50 个人满了，最大 20 个人也满了，还有任务疯狂进怎么办？
        // CallerRunsPolicy()：不报错，也不扔掉任务，而是让“谁把事情丢进线程池的”，谁自己去执行！
        // 也就是让处理网关请求的主干 Tomcat 线程自己去干这个活。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); 
        executor.initialize();
        return executor;
    }
}
