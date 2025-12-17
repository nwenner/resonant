package com.wenroe.resonant.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import com.wenroe.resonant.repository.ResourceTypeSettingRepository;
import com.wenroe.resonant.service.ResourceTypeSettingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ResourceTypeSetting Integration Tests")
class ResourceTypeSettingIntegrationTest {

  @Autowired
  private ResourceTypeSettingService service;

  @Autowired
  private ResourceTypeSettingRepository repository;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
  }

  @Test
  @DisplayName("Should get all resource type settings")
  void testGetAll() {
    // Given
    createSetting("s3:bucket", "S3 Buckets", "Amazon S3 storage buckets", true);
    createSetting("cloudfront:distribution", "CloudFront", "CloudFront CDN", false);
    createSetting("ec2:vpc", "VPCs", "Virtual Private Clouds", true);

    // When
    List<ResourceTypeSetting> result = service.getAll();

    // Then
    assertThat(result).hasSize(3);
    assertThat(result).extracting(ResourceTypeSetting::getResourceType)
        .containsExactlyInAnyOrder("s3:bucket", "cloudfront:distribution", "ec2:vpc");
  }

  @Test
  @DisplayName("Should get only enabled resource types")
  void testGetEnabledResourceTypes() {
    // Given
    createSetting("s3:bucket", "S3 Buckets", "Amazon S3 storage buckets", true);
    createSetting("cloudfront:distribution", "CloudFront", "CloudFront CDN", false);
    createSetting("ec2:vpc", "VPCs", "Virtual Private Clouds", true);

    // When
    List<ResourceTypeSetting> result = service.getEnabledResourceTypes();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).extracting(ResourceTypeSetting::getResourceType)
        .containsExactlyInAnyOrder("s3:bucket", "ec2:vpc");
    assertThat(result).allMatch(ResourceTypeSetting::getEnabled);
  }

  @Test
  @DisplayName("Should check if resource type is enabled")
  void testIsResourceTypeEnabled() {
    // Given
    createSetting("s3:bucket", "S3 Buckets", "Amazon S3 storage buckets", true);
    createSetting("cloudfront:distribution", "CloudFront", "CloudFront CDN", false);

    // When/Then
    assertThat(service.isResourceTypeEnabled("s3:bucket")).isTrue();
    assertThat(service.isResourceTypeEnabled("cloudfront:distribution")).isFalse();
    assertThat(service.isResourceTypeEnabled("unknown:type")).isFalse();
  }

  @Test
  @DisplayName("Should update enabled status")
  void testUpdateEnabled() {
    // Given
    createSetting("s3:bucket", "S3 Buckets", "Amazon S3 storage buckets", false);

    // When
    ResourceTypeSetting updated = service.updateEnabled("s3:bucket", true);

    // Then
    assertThat(updated.getEnabled()).isTrue();
    assertThat(service.isResourceTypeEnabled("s3:bucket")).isTrue();
  }

  @Test
  @DisplayName("Should disable resource type")
  void testDisableResourceType() {
    // Given
    createSetting("s3:bucket", "S3 Buckets", "Amazon S3 storage buckets", true);

    // When
    ResourceTypeSetting updated = service.updateEnabled("s3:bucket", false);

    // Then
    assertThat(updated.getEnabled()).isFalse();
    assertThat(service.isResourceTypeEnabled("s3:bucket")).isFalse();
  }

  @Test
  @DisplayName("Should toggle resource type multiple times")
  void testToggleResourceType() {
    // Given
    createSetting("s3:bucket", "S3 Buckets", "Amazon S3 storage buckets", true);

    // When/Then
    assertThat(service.isResourceTypeEnabled("s3:bucket")).isTrue();

    service.updateEnabled("s3:bucket", false);
    assertThat(service.isResourceTypeEnabled("s3:bucket")).isFalse();

    service.updateEnabled("s3:bucket", true);
    assertThat(service.isResourceTypeEnabled("s3:bucket")).isTrue();
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent resource type")
  void testUpdateEnabled_NotFound() {
    // When/Then
    assertThatThrownBy(() -> service.updateEnabled("unknown:type", true))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Resource type not found");
  }

  @Test
  @DisplayName("Should handle empty repository")
  void testGetAll_Empty() {
    // When
    List<ResourceTypeSetting> result = service.getAll();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should handle no enabled types")
  void testGetEnabledResourceTypes_AllDisabled() {
    // Given
    createSetting("s3:bucket", "S3 Buckets", "Amazon S3 storage buckets", false);
    createSetting("cloudfront:distribution", "CloudFront", "CloudFront CDN", false);

    // When
    List<ResourceTypeSetting> result = service.getEnabledResourceTypes();

    // Then
    assertThat(result).isEmpty();
  }

  private ResourceTypeSetting createSetting(String resourceType, String displayName,
      String description, boolean enabled) {
    ResourceTypeSetting setting = new ResourceTypeSetting();
    setting.setResourceType(resourceType);
    setting.setDisplayName(displayName);
    setting.setDescription(description);
    setting.setEnabled(enabled);
    return repository.save(setting);
  }
}