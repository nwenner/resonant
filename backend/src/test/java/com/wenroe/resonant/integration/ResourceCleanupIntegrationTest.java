package com.wenroe.resonant.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsAccountRegion;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.ComplianceViolation;
import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import com.wenroe.resonant.model.entity.ScanJob;
import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.model.enums.CredentialType;
import com.wenroe.resonant.model.enums.Severity;
import com.wenroe.resonant.model.enums.ViolationStatus;
import com.wenroe.resonant.repository.AwsAccountRegionRepository;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.repository.ComplianceViolationRepository;
import com.wenroe.resonant.repository.ResourceTypeSettingRepository;
import com.wenroe.resonant.repository.ScanJobRepository;
import com.wenroe.resonant.repository.TagPolicyRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.ResourceCleanupService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@DisplayName("Resource Cleanup Integration Tests")
class ResourceCleanupIntegrationTest {

  @Autowired
  private ResourceCleanupService cleanupService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AwsAccountRepository awsAccountRepository;

  @Autowired
  private AwsAccountRegionRepository awsAccountRegionRepository;

  @Autowired
  private AwsResourceRepository awsResourceRepository;

  @Autowired
  private ResourceTypeSettingRepository resourceTypeSettingRepository;

  @Autowired
  private ComplianceViolationRepository violationRepository;

  @Autowired
  private TagPolicyRepository policyRepository;

  @Autowired
  private ScanJobRepository scanJobRepository;

  private User testUser;
  private AwsAccount testAccount;
  private TagPolicy testPolicy;
  private ScanJob testScanJob;

  @BeforeEach
  void setUp() {
    // Clean up
    violationRepository.deleteAll();
    awsResourceRepository.deleteAll();
    awsAccountRegionRepository.deleteAll();
    resourceTypeSettingRepository.deleteAll();
    scanJobRepository.deleteAll();
    policyRepository.deleteAll();
    awsAccountRepository.deleteAll();
    userRepository.deleteAll();

    // Create test user
    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setName("Test User");
    testUser.setPasswordHash("hash");
    testUser = userRepository.save(testUser);

    // Create test account
    testAccount = new AwsAccount();
    testAccount.setUser(testUser);
    testAccount.setAccountId("123456789012");
    testAccount.setAccountAlias("test-account");
    testAccount.setRoleArn("arn:aws:iam::123456789012:role/test");
    testAccount.setExternalId("external-123");
    testAccount.setCredentialType(CredentialType.ROLE);
    testAccount.setStatus(AwsAccountStatus.ACTIVE);
    testAccount = awsAccountRepository.save(testAccount);

    // Create regions
    createRegion("us-east-1", true);
    createRegion("us-east-2", true);

    // Create resource type settings
    createResourceTypeSetting("s3:bucket", "S3 Buckets", true);
    createResourceTypeSetting("cloudfront:distribution", "CloudFront Distributions", true);
    createResourceTypeSetting("ec2:vpc", "VPCs", true);

    // Create test policy
    testPolicy = new TagPolicy();
    testPolicy.setUser(testUser);
    testPolicy.setName("Test Policy");
    testPolicy.setDescription("Test");
    Map<String, List<String>> requiredTags = new HashMap<>();
    requiredTags.put("Environment", null);
    testPolicy.setRequiredTags(requiredTags);
    testPolicy.setResourceTypes(List.of("s3:bucket", "cloudfront:distribution", "ec2:vpc"));
    testPolicy.setSeverity(Severity.MEDIUM);
    testPolicy.setEnabled(true);
    testPolicy = policyRepository.save(testPolicy);

    // Create test scan job
    testScanJob = new ScanJob();
    testScanJob.setUser(testUser);
    testScanJob.setAwsAccount(testAccount);
    testScanJob = scanJobRepository.save(testScanJob);
  }

  @Test
  @DisplayName("Should delete resources when resource type is disabled")
  void shouldDeleteResourcesWhenTypeDisabled() {
    // Given
    AwsResource s3Resource = createResource("s3:bucket", "us-east-1", "test-bucket");
    AwsResource cloudFrontResource = createResource("cloudfront:distribution", "global",
        "test-dist");

    // Disable CloudFront
    ResourceTypeSetting cloudFrontSetting = resourceTypeSettingRepository
        .findByResourceType("cloudfront:distribution").orElseThrow();
    cloudFrontSetting.setEnabled(false);
    resourceTypeSettingRepository.save(cloudFrontSetting);

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    List<AwsResource> remaining = awsResourceRepository.findByAwsAccountId(testAccount.getId());
    assertThat(remaining).hasSize(1);
    assertThat(remaining.get(0).getResourceType()).isEqualTo("s3:bucket");
  }

  @Test
  @DisplayName("Should delete resources in disabled regions")
  void shouldDeleteResourcesInDisabledRegions() {
    // Given
    AwsResource s3East1 = createResource("s3:bucket", "us-east-1", "bucket-east-1");
    AwsResource s3East2 = createResource("s3:bucket", "us-east-2", "bucket-east-2");

    // Disable us-east-2
    AwsAccountRegion region2 = awsAccountRegionRepository
        .findByAwsAccountIdAndRegionCode(testAccount.getId(), "us-east-2").orElseThrow();
    region2.setEnabled(false);
    awsAccountRegionRepository.save(region2);

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    List<AwsResource> remaining = awsResourceRepository.findByAwsAccountId(testAccount.getId());
    assertThat(remaining).hasSize(1);
    assertThat(remaining.get(0).getRegion()).isEqualTo("us-east-1");
  }

  @Test
  @DisplayName("Should cascade delete violations when resource is deleted")
  void shouldCascadeDeleteViolations() {
    // Given
    AwsResource cloudFrontResource = createResource("cloudfront:distribution", "global",
        "test-dist");

    ComplianceViolation violation = new ComplianceViolation();
    violation.setAwsResource(cloudFrontResource);
    violation.setTagPolicy(testPolicy);
    violation.setScanJob(testScanJob);
    violation.setStatus(ViolationStatus.OPEN);
    Map<String, Object> details = new HashMap<>();
    details.put("missingTags", List.of("Environment"));
    violation.setViolationDetails(details);
    violation.setDetectedAt(LocalDateTime.now());
    violationRepository.save(violation);

    // Disable CloudFront
    ResourceTypeSetting cloudFrontSetting = resourceTypeSettingRepository
        .findByResourceType("cloudfront:distribution").orElseThrow();
    cloudFrontSetting.setEnabled(false);
    resourceTypeSettingRepository.save(cloudFrontSetting);

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    assertThat(awsResourceRepository.findByAwsAccountId(testAccount.getId())).isEmpty();
    assertThat(violationRepository.findAll()).isEmpty(); // Cascade delete
  }

  @Test
  @DisplayName("Should delete global resources when no regions enabled")
  void shouldDeleteGlobalResourcesWhenNoRegionsEnabled() {
    // Given
    AwsResource cloudFrontResource = createResource("cloudfront:distribution", "global",
        "test-dist");

    // Disable all regions
    List<AwsAccountRegion> regions = awsAccountRegionRepository.findByAwsAccountId(
        testAccount.getId());
    regions.forEach(r -> {
      r.setEnabled(false);
      awsAccountRegionRepository.save(r);
    });

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    assertThat(awsResourceRepository.findByAwsAccountId(testAccount.getId())).isEmpty();
  }

  @Test
  @DisplayName("Should keep global resources when at least one region enabled")
  void shouldKeepGlobalResourcesWhenRegionEnabled() {
    // Given
    AwsResource cloudFrontResource = createResource("cloudfront:distribution", "global",
        "test-dist");

    // Disable only us-east-2
    AwsAccountRegion region2 = awsAccountRegionRepository
        .findByAwsAccountIdAndRegionCode(testAccount.getId(), "us-east-2").orElseThrow();
    region2.setEnabled(false);
    awsAccountRegionRepository.save(region2);

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    List<AwsResource> remaining = awsResourceRepository.findByAwsAccountId(testAccount.getId());
    assertThat(remaining).hasSize(1);
    assertThat(remaining.get(0).getResourceType()).isEqualTo("cloudfront:distribution");
  }

  @Test
  @DisplayName("Should handle mixed scenario with multiple deletions")
  void shouldHandleMixedScenario() {
    // Given - Create diverse resources
    AwsResource s3East1 = createResource("s3:bucket", "us-east-1", "bucket-east-1");
    AwsResource s3East2 = createResource("s3:bucket", "us-east-2", "bucket-east-2");
    AwsResource cloudFront = createResource("cloudfront:distribution", "global", "test-dist");
    AwsResource vpcEast1 = createResource("ec2:vpc", "us-east-1", "vpc-east-1");
    AwsResource vpcEast2 = createResource("ec2:vpc", "us-east-2", "vpc-east-2");

    // Disable us-east-2 region
    AwsAccountRegion region2 = awsAccountRegionRepository
        .findByAwsAccountIdAndRegionCode(testAccount.getId(), "us-east-2").orElseThrow();
    region2.setEnabled(false);
    awsAccountRegionRepository.save(region2);

    // Disable VPC resource type
    ResourceTypeSetting vpcSetting = resourceTypeSettingRepository
        .findByResourceType("ec2:vpc").orElseThrow();
    vpcSetting.setEnabled(false);
    resourceTypeSettingRepository.save(vpcSetting);

    // When
    cleanupService.cleanupOutOfScopeResources(testAccount);

    // Then
    List<AwsResource> remaining = awsResourceRepository.findByAwsAccountId(testAccount.getId());
    assertThat(remaining).hasSize(2); // Only s3East1 and cloudFront should remain
    assertThat(remaining).extracting(AwsResource::getResourceId)
        .containsExactlyInAnyOrder("bucket-east-1", "test-dist");
  }

  private void createRegion(String regionCode, boolean enabled) {
    AwsAccountRegion region = new AwsAccountRegion();
    region.setAwsAccount(testAccount);
    region.setRegionCode(regionCode);
    region.setEnabled(enabled);
    awsAccountRegionRepository.save(region);
  }

  private void createResourceTypeSetting(String resourceType, String displayName, boolean enabled) {
    ResourceTypeSetting setting = new ResourceTypeSetting();
    setting.setResourceType(resourceType);
    setting.setDisplayName(displayName);
    setting.setDescription("Test setting");
    setting.setEnabled(enabled);
    resourceTypeSettingRepository.save(setting);
  }

  private AwsResource createResource(String resourceType, String region, String resourceId) {
    AwsResource resource = new AwsResource();
    resource.setAwsAccount(testAccount);
    resource.setResourceType(resourceType);
    resource.setRegion(region);
    resource.setResourceId(resourceId);
    resource.setResourceArn("arn:aws:" + resourceType + ":" + region + ":" +
        testAccount.getAccountId() + ":" + resourceId);
    resource.setName(resourceId);
    resource.setDiscoveredAt(LocalDateTime.now());
    resource.setLastSeenAt(LocalDateTime.now());
    return awsResourceRepository.save(resource);
  }
}