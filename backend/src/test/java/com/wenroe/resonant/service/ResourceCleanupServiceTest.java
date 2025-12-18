package com.wenroe.resonant.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsAccountRegion;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import com.wenroe.resonant.repository.AwsAccountRegionRepository;
import com.wenroe.resonant.repository.AwsResourceRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceCleanupService Tests")
class ResourceCleanupServiceTest {

  @Mock
  private AwsResourceRepository awsResourceRepository;

  @Mock
  private AwsAccountRegionRepository awsAccountRegionRepository;

  @Mock
  private ResourceTypeSettingService resourceTypeSettingService;

  @InjectMocks
  private ResourceCleanupService cleanupService;

  private AwsAccount testAccount;

  @BeforeEach
  void setUp() {
    testAccount = new AwsAccount();
    testAccount.setId(UUID.randomUUID());
    testAccount.setAccountId("123456789012");
  }

  @Test
  @DisplayName("Should delete resources with disabled resource type")
  void shouldDeleteResourcesWithDisabledResourceType() {
    // Given
    List<ResourceTypeSetting> enabledTypes = List.of(
        createResourceTypeSetting("s3:bucket")
    );
    List<AwsAccountRegion> enabledRegions = List.of(
        createRegion("us-east-1"),
        createRegion("us-east-2")
    );

    AwsResource s3Resource = createResource("s3:bucket", "us-east-1");
    AwsResource cloudFrontResource = createResource("cloudfront:distribution", "global");

    when(resourceTypeSettingService.getEnabledResourceTypes()).thenReturn(enabledTypes);
    when(awsAccountRegionRepository.findEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(enabledRegions);
    when(awsResourceRepository.findByAwsAccountIdWithViolations(testAccount.getId()))
        .thenReturn(List.of(s3Resource, cloudFrontResource));

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    verify(awsResourceRepository).delete(cloudFrontResource); // CloudFront disabled
    verify(awsResourceRepository, never()).delete(s3Resource); // S3 enabled
  }

  @Test
  @DisplayName("Should delete regional resources in disabled regions")
  void shouldDeleteRegionalResourcesInDisabledRegions() {
    // Given
    List<ResourceTypeSetting> enabledTypes = List.of(
        createResourceTypeSetting("s3:bucket")
    );
    List<AwsAccountRegion> enabledRegions = List.of(
        createRegion("us-east-1")
    );

    AwsResource s3East1 = createResource("s3:bucket", "us-east-1");
    AwsResource s3East2 = createResource("s3:bucket", "us-east-2");

    when(resourceTypeSettingService.getEnabledResourceTypes()).thenReturn(enabledTypes);
    when(awsAccountRegionRepository.findEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(enabledRegions);
    when(awsResourceRepository.findByAwsAccountIdWithViolations(testAccount.getId()))
        .thenReturn(List.of(s3East1, s3East2));

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    verify(awsResourceRepository).delete(s3East2); // us-east-2 disabled
    verify(awsResourceRepository, never()).delete(s3East1); // us-east-1 enabled
  }

  @Test
  @DisplayName("Should delete global resources when no regions enabled")
  void shouldDeleteGlobalResourcesWhenNoRegionsEnabled() {
    // Given
    List<ResourceTypeSetting> enabledTypes = List.of(
        createResourceTypeSetting("cloudfront:distribution")
    );
    List<AwsAccountRegion> enabledRegions = List.of(); // No regions enabled

    AwsResource cloudFrontResource = createResource("cloudfront:distribution", "global");

    when(resourceTypeSettingService.getEnabledResourceTypes()).thenReturn(enabledTypes);
    when(awsAccountRegionRepository.findEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(enabledRegions);
    when(awsResourceRepository.findByAwsAccountIdWithViolations(testAccount.getId()))
        .thenReturn(List.of(cloudFrontResource));

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    verify(awsResourceRepository).delete(cloudFrontResource); // No regions enabled
  }

  @Test
  @DisplayName("Should keep global resources when at least one region enabled")
  void shouldKeepGlobalResourcesWhenRegionsEnabled() {
    // Given
    List<ResourceTypeSetting> enabledTypes = List.of(
        createResourceTypeSetting("cloudfront:distribution")
    );
    List<AwsAccountRegion> enabledRegions = List.of(
        createRegion("us-east-1")
    );

    AwsResource cloudFrontResource = createResource("cloudfront:distribution", "global");

    when(resourceTypeSettingService.getEnabledResourceTypes()).thenReturn(enabledTypes);
    when(awsAccountRegionRepository.findEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(enabledRegions);
    when(awsResourceRepository.findByAwsAccountIdWithViolations(testAccount.getId()))
        .thenReturn(List.of(cloudFrontResource));

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    verify(awsResourceRepository, never()).delete(any()); // CloudFront kept
  }

  @Test
  @DisplayName("Should delete multiple out-of-scope resources")
  void shouldDeleteMultipleOutOfScopeResources() {
    // Given
    List<ResourceTypeSetting> enabledTypes = List.of(
        createResourceTypeSetting("s3:bucket")
    );
    List<AwsAccountRegion> enabledRegions = List.of(
        createRegion("us-east-1")
    );

    AwsResource s3East1 = createResource("s3:bucket", "us-east-1");
    AwsResource s3East2 = createResource("s3:bucket", "us-east-2");
    AwsResource cloudFront = createResource("cloudfront:distribution", "global");
    AwsResource vpc = createResource("ec2:vpc", "us-east-1");

    when(resourceTypeSettingService.getEnabledResourceTypes()).thenReturn(enabledTypes);
    when(awsAccountRegionRepository.findEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(enabledRegions);
    when(awsResourceRepository.findByAwsAccountIdWithViolations(testAccount.getId()))
        .thenReturn(List.of(s3East1, s3East2, cloudFront, vpc));

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    verify(awsResourceRepository).delete(s3East2); // Wrong region
    verify(awsResourceRepository).delete(cloudFront); // Type disabled
    verify(awsResourceRepository).delete(vpc); // Type disabled
    verify(awsResourceRepository, never()).delete(s3East1); // In scope
    verify(awsResourceRepository, times(3)).delete(any());
  }

  @Test
  @DisplayName("Should not delete any resources when all in scope")
  void shouldNotDeleteWhenAllInScope() {
    // Given
    List<ResourceTypeSetting> enabledTypes = List.of(
        createResourceTypeSetting("s3:bucket"),
        createResourceTypeSetting("cloudfront:distribution")
    );
    List<AwsAccountRegion> enabledRegions = List.of(
        createRegion("us-east-1"),
        createRegion("us-east-2")
    );

    AwsResource s3East1 = createResource("s3:bucket", "us-east-1");
    AwsResource s3East2 = createResource("s3:bucket", "us-east-2");
    AwsResource cloudFront = createResource("cloudfront:distribution", "global");

    when(resourceTypeSettingService.getEnabledResourceTypes()).thenReturn(enabledTypes);
    when(awsAccountRegionRepository.findEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(enabledRegions);
    when(awsResourceRepository.findByAwsAccountIdWithViolations(testAccount.getId()))
        .thenReturn(List.of(s3East1, s3East2, cloudFront));

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    verify(awsResourceRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should handle empty resource list")
  void shouldHandleEmptyResourceList() {
    // Given
    when(resourceTypeSettingService.getEnabledResourceTypes()).thenReturn(List.of());
    when(awsAccountRegionRepository.findEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(List.of());
    when(awsResourceRepository.findByAwsAccountIdWithViolations(testAccount.getId()))
        .thenReturn(List.of());

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    verify(awsResourceRepository, never()).delete(any());
  }

  private ResourceTypeSetting createResourceTypeSetting(String resourceType) {
    ResourceTypeSetting setting = new ResourceTypeSetting();
    setting.setId(UUID.randomUUID());
    setting.setResourceType(resourceType);
    setting.setDisplayName(resourceType);
    setting.setEnabled(true);
    return setting;
  }

  private AwsAccountRegion createRegion(String regionCode) {
    AwsAccountRegion region = new AwsAccountRegion();
    region.setId(UUID.randomUUID());
    region.setAwsAccount(testAccount);
    region.setRegionCode(regionCode);
    region.setEnabled(true);
    return region;
  }

  private AwsResource createResource(String resourceType, String region) {
    AwsResource resource = new AwsResource();
    resource.setId(UUID.randomUUID());
    resource.setAwsAccount(testAccount);
    resource.setResourceType(resourceType);
    resource.setRegion(region);
    resource.setResourceArn(
        "arn:aws:" + resourceType + "::" + testAccount.getAccountId() + ":test");
    resource.setResourceId("test-" + UUID.randomUUID());
    return resource;
  }
}