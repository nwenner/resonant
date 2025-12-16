package com.wenroe.resonant.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for asynchronous execution of scan jobs.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  /**
   * Thread pool for executing scan jobs asynchronously. - Core pool size: 2 (handles normal load) -
   * Max pool size: 10 (handles burst traffic) - Queue capacity: 100 (queues additional scan
   * requests)
   */
  @Bean(name = "scanExecutor")
  public Executor scanExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("scan-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
  }
}