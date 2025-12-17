package com.wenroe.resonant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import com.wenroe.resonant.repository.ResourceTypeSettingRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceTypeSettingService Tests")
class ResourceTypeSettingServiceTest {

  @Mock
  private ResourceTypeSettingRepository repository;

  @InjectMocks
  private ResourceTypeSettingService service;

  @Test
  @DisplayName("Should get all resource type settings")
  void testGetAll() {
    // Given
    ResourceTypeSetting setting1 = createSetting("cloudfront:distribution", "CloudFront", false);
    ResourceTypeSetting setting2 = createSetting("s3:bucket", "S3 Buckets", true);
    when(repository.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(setting1, setting2));

    // When
    List<ResourceTypeSetting> result = service.getAll();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).contains(setting1, setting2);
    verify(repository).findAllByOrderByDisplayNameAsc();
  }

  @Test
  @DisplayName("Should get only enabled resource types")
  void testGetEnabledResourceTypes() {
    // Given
    ResourceTypeSetting enabled = createSetting("s3:bucket", "S3 Buckets", true);
    when(repository.findAllByEnabledTrueOrderByDisplayNameAsc()).thenReturn(List.of(enabled));

    // When
    List<ResourceTypeSetting> result = service.getEnabledResourceTypes();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getResourceType()).isEqualTo("s3:bucket");
    assertThat(result.getFirst().getEnabled()).isTrue();
    verify(repository).findAllByEnabledTrueOrderByDisplayNameAsc();
  }

  @Test
  @DisplayName("Should return true when resource type is enabled")
  void testIsResourceTypeEnabled_WhenEnabled() {
    // Given
    ResourceTypeSetting setting = createSetting("s3:bucket", "S3 Buckets", true);
    when(repository.findByResourceType("s3:bucket")).thenReturn(Optional.of(setting));

    // When
    boolean result = service.isResourceTypeEnabled("s3:bucket");

    // Then
    assertThat(result).isTrue();
    verify(repository).findByResourceType("s3:bucket");
  }

  @Test
  @DisplayName("Should return false when resource type is disabled")
  void testIsResourceTypeEnabled_WhenDisabled() {
    // Given
    ResourceTypeSetting setting = createSetting("s3:bucket", "S3 Buckets", false);
    when(repository.findByResourceType("s3:bucket")).thenReturn(Optional.of(setting));

    // When
    boolean result = service.isResourceTypeEnabled("s3:bucket");

    // Then
    assertThat(result).isFalse();
    verify(repository).findByResourceType("s3:bucket");
  }

  @Test
  @DisplayName("Should return false when resource type does not exist")
  void testIsResourceTypeEnabled_WhenNotFound() {
    // Given
    when(repository.findByResourceType("unknown:type")).thenReturn(Optional.empty());

    // When
    boolean result = service.isResourceTypeEnabled("unknown:type");

    // Then
    assertThat(result).isFalse();
    verify(repository).findByResourceType("unknown:type");
  }

  @Test
  @DisplayName("Should update enabled status successfully")
  void testUpdateEnabled_Success() {
    // Given
    ResourceTypeSetting setting = createSetting("s3:bucket", "S3 Buckets", false);
    when(repository.findByResourceType("s3:bucket")).thenReturn(Optional.of(setting));
    when(repository.save(any(ResourceTypeSetting.class))).thenReturn(setting);

    // When
    ResourceTypeSetting result = service.updateEnabled("s3:bucket", true);

    // Then
    assertThat(result.getEnabled()).isTrue();
    verify(repository).findByResourceType("s3:bucket");
    verify(repository).save(setting);
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent resource type")
  void testUpdateEnabled_NotFound() {
    // Given
    when(repository.findByResourceType("unknown:type")).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> service.updateEnabled("unknown:type", true))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Resource type not found");

    verify(repository).findByResourceType("unknown:type");
  }

  private ResourceTypeSetting createSetting(String resourceType, String displayName,
      boolean enabled) {
    ResourceTypeSetting setting = new ResourceTypeSetting();
    setting.setId(UUID.randomUUID());
    setting.setResourceType(resourceType);
    setting.setDisplayName(displayName);
    setting.setDescription("Test description");
    setting.setEnabled(enabled);
    return setting;
  }
}