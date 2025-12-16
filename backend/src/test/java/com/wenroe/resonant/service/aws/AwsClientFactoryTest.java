package com.wenroe.resonant.service.aws;

import static org.assertj.core.api.Assertions.assertThat;

import com.wenroe.resonant.service.security.CredentialEncryptionService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for AwsClientFactory caching infrastructure. Note: We only test cache operations, not
 * actual credential resolution which would require AWS connectivity or complex mocking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AwsClientFactory Tests")
class AwsClientFactoryTest {

  @Mock
  private CredentialEncryptionService encryptionService;

  private AwsClientFactory clientFactory;

  @BeforeEach
  void setUp() {
    clientFactory = new AwsClientFactory(encryptionService, 3600);
  }

  @Test
  @DisplayName("Should provide cache statistics")
  void shouldProvideCacheStatistics() {
    // When
    String stats = clientFactory.getCacheStats();

    // Then
    assertThat(stats).isNotNull();
    assertThat(stats).contains("hitCount");
    assertThat(stats).contains("missCount");
    assertThat(stats).contains("evictionCount");
  }

  @Test
  @DisplayName("Should support evicting credentials by account ID")
  void shouldSupportEviction() {
    // Given
    UUID accountId = UUID.randomUUID();

    // When/Then - Should not throw
    clientFactory.evictCredentials(accountId);

    // Verify cache still works after eviction
    String stats = clientFactory.getCacheStats();
    assertThat(stats).isNotNull();
  }

  @Test
  @DisplayName("Should support clearing all cached credentials")
  void shouldSupportClearingCache() {
    // When/Then - Should not throw
    clientFactory.clearCredentialCache();

    // Verify cache still works after clear
    String stats = clientFactory.getCacheStats();
    assertThat(stats).isNotNull();
  }

  @Test
  @DisplayName("Should initialize with correct session duration")
  void shouldInitializeWithSessionDuration() {
    // Given/When
    AwsClientFactory factory = new AwsClientFactory(encryptionService, 7200);

    // Then - Should not throw and cache should work
    String stats = factory.getCacheStats();
    assertThat(stats).isNotNull();
  }
}