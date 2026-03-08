package com.harrish.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous processing in the application.
 * Enables @Async annotation support for event listeners and other async operations.
 * 
 * Benefits:
 * - Event listeners with @Async run in separate threads, preventing blocking
 * - Main request processing (registration, blog post creation) returns immediately
 * - Side effects (logging, notifications, cache invalidation) happen asynchronously
 * - Improved application responsiveness and user experience
 * - Bounded thread pool prevents OutOfMemoryError under load
 * - SecurityContext propagation ensures security-aware async operations
 * 
 * IMPORTANT: Default SimpleAsyncTaskExecutor creates unbounded threads, which can
 * cause OutOfMemoryError. This configuration uses a bounded thread pool with:
 * - Core pool: 5 threads (event listeners are I/O-light, log-focused)
 * - Max pool: 10 threads (allows burst handling during high event load)
 * - Queue: 100 tasks (backpressure to prevent resource exhaustion)
 * - CallerRunsPolicy: Provides backpressure by running task synchronously when queue full
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configures a bounded thread pool for @Async methods.
     * 
     * Configuration rationale:
     * - Core pool: 5 threads (event listeners are I/O-light, log-focused)
     * - Max pool: 10 threads (allows burst handling during high event load)
     * - Queue: 100 tasks (backpressure to prevent resource exhaustion)
     * - CallerRunsPolicy: Provides backpressure by running task synchronously when queue full
     * - Named threads: "async-event-" for easy identification in logs/profiling
     * - SecurityContext propagation: Ensures security info available in async methods
     * 
     * Performance impact:
     * - Prevents OutOfMemoryError from unbounded thread creation
     * - Memory: ~10MB (10 threads) vs ~1GB (1000 threads under load)
     * - Thread reuse eliminates 1-2ms creation overhead per task
     * 
     * Note: For Java 21+ with Virtual Threads, consider newVirtualThreadPerTaskExecutor()
     * for I/O-bound async tasks (millions of concurrent tasks with minimal memory).
     * 
     * @return configured executor with SecurityContext propagation
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-event-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        // Wrap with DelegatingSecurityContextAsyncTaskExecutor to propagate SecurityContext
        // This ensures SecurityContextHolder.getContext() works correctly in @Async methods
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
