package com.harrish.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous processing in the application.
 * Enables @Async annotation support for event listeners and other async operations.
 * 
 * Benefits:
 * - Event listeners with @Async run in separate threads, preventing blocking
 * - Main request processing (registration, blog post creation) returns immediately
 * - Side effects (logging, notifications, cache invalidation) happen asynchronously
 * - Improved application responsiveness and user experience
 * 
 * Spring will use the default SimpleAsyncTaskExecutor for async tasks.
 * For production use with high load, consider configuring a custom ThreadPoolTaskExecutor
 * with appropriate pool size, queue capacity, and rejection policy.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Using default async configuration
    // Spring will create a SimpleAsyncTaskExecutor
    // For custom configuration, uncomment the following:
    
    /*
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
    */
}
