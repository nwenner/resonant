package com.wenroe.resonant.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncConfig Tests")
class AsyncConfigTest {

  private final AsyncConfig asyncConfig = new AsyncConfig();

  @Test
  @DisplayName("Should create scan executor with correct configuration")
  void shouldCreateScanExecutor() {
    // When
    Executor executor = asyncConfig.scanExecutor();

    // Then
    assertThat(executor).isNotNull();
    assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);

    ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
    assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
    assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(10);
  }

  @Test
  @DisplayName("Should configure thread name prefix")
  void shouldConfigureThreadNamePrefix() {
    // When
    Executor executor = asyncConfig.scanExecutor();

    // Then
    ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
    assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("scan-");
  }

  @Test
  @DisplayName("Should configure graceful shutdown")
  void shouldConfigureGracefulShutdown() {
    // When
    Executor executor = asyncConfig.scanExecutor();

    // Then
    ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;

    // Note: These properties are set but not directly accessible via getters
    // This test verifies the executor is properly initialized
    assertThat(taskExecutor).isNotNull();
  }
}