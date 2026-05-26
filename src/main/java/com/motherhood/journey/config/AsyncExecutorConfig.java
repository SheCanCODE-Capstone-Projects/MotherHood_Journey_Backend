package com.motherhood.journey.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Wraps the default @Async executor with a DelegatingSecurityContextAsyncTaskExecutor
 * so SecurityContextHolder is propagated to async threads — required for AuditService
 * to resolve the calling user when run via @Async.
 */
@Configuration
public class AsyncExecutorConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
        delegate.setCorePoolSize(2);
        delegate.setMaxPoolSize(10);
        delegate.setQueueCapacity(100);
        delegate.setThreadNamePrefix("mh-async-");
        delegate.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
