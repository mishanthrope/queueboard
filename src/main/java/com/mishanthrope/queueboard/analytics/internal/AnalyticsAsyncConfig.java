package com.mishanthrope.queueboard.analytics.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Small bounded executor dedicated to analytics; one worker keeps events in order. */
@Slf4j
@Configuration
@EnableAsync
public class AnalyticsAsyncConfig {

    @Bean(name = "analyticsExecutor")
    public ThreadPoolTaskExecutor analyticsExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("analytics-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setRejectedExecutionHandler((task, pool) -> {
            if (!pool.isShutdown()) {
                log.warn("Analytics queue is full, event dropped");
            }
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }
}
