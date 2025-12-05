package com.wenroe.resonant.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.wenroe.resonant.dto.compliance.ComplianceRateResponse;
import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.ComplianceViolation;
import com.wenroe.resonant.model.entity.ScanJob;
import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.model.enums.CredentialType;
import com.wenroe.resonant.model.enums.ScanStatus;
import com.wenroe.resonant.model.enums.Severity;
import com.wenroe.resonant.model.enums.UserRole;
import com.wenroe.resonant.model.enums.ViolationStatus;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.repository.ComplianceViolationRepository;
import com.wenroe.resonant.repository.ScanJobRepository;
import com.wenroe.resonant.repository.TagPolicyRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.DashboardService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Dashboard Integration Tests")
class DashboardIntegrationTest {

  @Autowired
  private DashboardService dashboardService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AwsAccountRepository accountRepository;

  @Autowired
  private AwsResourceRepository resourceRepository;

  @Autowired
  private ComplianceViolationRepository violationRepository;

  @Autowired
  private TagPolicyRepository policyRepository;

  @Autowired
  private ScanJobRepository scanJobRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User testUser;
  private AwsAccount testAccount;
  private TagPolicy testPolicy;
  private ScanJob testScanJob;

  @BeforeEach
  void setUp() {
    // Clean up
    violationRepository.deleteAll();
    scanJobRepository.deleteAll();
    resourceRepository.deleteAll();
    policyRepository.deleteAll();
    accountRepository.deleteAll();
    userRepository.deleteAll();

    // Create test user
    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setName("Test User");
    testUser.setPasswordHash(passwordEncoder.encode("password123"));
    testUser.setRole(UserRole.USER);
    testUser.setEnabled(true);
    testUser = userRepository.save(testUser);

    // Create test AWS account
    testAccount = new AwsAccount();
    testAccount.setUser(testUser);
    testAccount.setAccountId("123456789012");
    testAccount.setAccountAlias("Test Account");
    testAccount.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
    testAccount.setExternalId("external-id-12345");
    testAccount.setCredentialType(CredentialType.ROLE);
    testAccount.setStatus(AwsAccountStatus.ACTIVE);
    testAccount = accountRepository.save(testAccount);

    // Create test tag policy
    testPolicy = new TagPolicy();
    testPolicy.setUser(testUser);
    testPolicy.setName("Test Policy");
    testPolicy.setDescription("Test policy for compliance");
    Map<String, List<String>> requiredTags = new HashMap<>();
    requiredTags.put("Environment", null); // Any value allowed
    testPolicy.setRequiredTags(requiredTags);
    testPolicy.setResourceTypes(java.util.List.of("s3:bucket"));
    testPolicy.setSeverity(Severity.HIGH);
    testPolicy.setEnabled(true);
    testPolicy = policyRepository.save(testPolicy);

    // Create test scan job
    testScanJob = new ScanJob();
    testScanJob.setAwsAccount(testAccount);
    testScanJob.setUser(testUser);
    testScanJob.setStatus(ScanStatus.SUCCESS);
    testScanJob.setResourcesScanned(0);
    testScanJob.setViolationsFound(0);
    testScanJob.setViolationsResolved(0);
    testScanJob.setStartedAt(LocalDateTime.now());
    testScanJob.setCompletedAt(LocalDateTime.now());
    testScanJob = scanJobRepository.save(testScanJob);
  }

  @Test
  @DisplayName("Should return 100% compliance when no resources exist")
  void getComplianceRate_NoResources() {
    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then
    assertThat(response.getTotalResources()).isEqualTo(0L);
    assertThat(response.getCompliantResources()).isEqualTo(0L);
    assertThat(response.getNonCompliantResources()).isEqualTo(0L);
    assertThat(response.getComplianceRate()).isEqualTo(100.0);
  }

  @Test
  @DisplayName("Should return 100% compliance when all resources are compliant")
  void getComplianceRate_AllCompliant() {
    // Given - Create 3 resources with no violations
    createResource("bucket-1", Map.of("Environment", "Production"));
    createResource("bucket-2", Map.of("Environment", "Staging"));
    createResource("bucket-3", Map.of("Environment", "Development"));

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then
    assertThat(response.getTotalResources()).isEqualTo(3L);
    assertThat(response.getCompliantResources()).isEqualTo(3L);
    assertThat(response.getNonCompliantResources()).isEqualTo(0L);
    assertThat(response.getComplianceRate()).isEqualTo(100.0);
  }

  @Test
  @DisplayName("Should return 0% compliance when all resources are non-compliant")
  void getComplianceRate_AllNonCompliant() {
    // Given - Create 3 resources with violations
    AwsResource resource1 = createResource("bucket-1", Map.of());
    AwsResource resource2 = createResource("bucket-2", Map.of());
    AwsResource resource3 = createResource("bucket-3", Map.of());

    createViolation(resource1, ViolationStatus.OPEN);
    createViolation(resource2, ViolationStatus.OPEN);
    createViolation(resource3, ViolationStatus.OPEN);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then
    assertThat(response.getTotalResources()).isEqualTo(3L);
    assertThat(response.getCompliantResources()).isEqualTo(0L);
    assertThat(response.getNonCompliantResources()).isEqualTo(3L);
    assertThat(response.getComplianceRate()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("Should calculate correct compliance rate with mixed resources")
  void getComplianceRate_MixedCompliance() {
    // Given - 5 resources: 3 compliant, 2 non-compliant
    AwsResource resource1 = createResource("bucket-1", Map.of("Environment", "Production"));
    AwsResource resource2 = createResource("bucket-2", Map.of("Environment", "Staging"));
    AwsResource resource3 = createResource("bucket-3", Map.of());
    AwsResource resource4 = createResource("bucket-4", Map.of("Environment", "Development"));
    AwsResource resource5 = createResource("bucket-5", Map.of());

    // Create violations for resources 3 and 5
    createViolation(resource3, ViolationStatus.OPEN);
    createViolation(resource5, ViolationStatus.OPEN);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then
    assertThat(response.getTotalResources()).isEqualTo(5L);
    assertThat(response.getCompliantResources()).isEqualTo(3L);
    assertThat(response.getNonCompliantResources()).isEqualTo(2L);
    assertThat(response.getComplianceRate()).isEqualTo(60.0); // 3/5 = 60%
  }

  @Test
  @DisplayName("Should count resource with multiple violations only once")
  void getComplianceRate_MultipleViolationsPerResource() {
    // Given - 2 resources, but resource1 has 3 violations (from different policies)
    AwsResource resource1 = createResource("bucket-1", Map.of());
    AwsResource resource2 = createResource("bucket-2", Map.of("Environment", "Production"));

    // Create additional policies
    Map<String, List<String>> policy2Tags = new HashMap<>();
    policy2Tags.put("Owner", null);
    TagPolicy policy2 = createPolicy("Policy 2", policy2Tags);

    Map<String, List<String>> policy3Tags = new HashMap<>();
    policy3Tags.put("CostCenter", null);
    TagPolicy policy3 = createPolicy("Policy 3", policy3Tags);

    // Create 3 violations for resource1 (one per policy)
    createViolationWithPolicy(resource1, testPolicy, ViolationStatus.OPEN);
    createViolationWithPolicy(resource1, policy2, ViolationStatus.OPEN);
    createViolationWithPolicy(resource1, policy3, ViolationStatus.OPEN);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then - Should count resource1 only once despite 3 violations
    assertThat(response.getTotalResources()).isEqualTo(2L);
    assertThat(response.getCompliantResources()).isEqualTo(1L);
    assertThat(response.getNonCompliantResources()).isEqualTo(1L);
    assertThat(response.getComplianceRate()).isEqualTo(50.0); // 1/2 = 50%
  }

  @Test
  @DisplayName("Should exclude RESOLVED violations from non-compliant count")
  void getComplianceRate_ExcludeResolvedViolations() {
    // Given - 3 resources with violations, but 1 is resolved
    AwsResource resource1 = createResource("bucket-1", Map.of());
    AwsResource resource2 = createResource("bucket-2", Map.of());
    AwsResource resource3 = createResource("bucket-3", Map.of());

    createViolation(resource1, ViolationStatus.OPEN);
    createViolation(resource2, ViolationStatus.RESOLVED); // Not counted
    createViolation(resource3, ViolationStatus.OPEN);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then - Only 2 resources are non-compliant (resource2's violation is resolved)
    assertThat(response.getTotalResources()).isEqualTo(3L);
    assertThat(response.getCompliantResources()).isEqualTo(1L);
    assertThat(response.getNonCompliantResources()).isEqualTo(2L);
    assertThat(response.getComplianceRate()).isCloseTo(33.33,
        org.assertj.core.data.Offset.offset(0.01));
  }

  @Test
  @DisplayName("Should exclude IGNORED violations from non-compliant count")
  void getComplianceRate_ExcludeIgnoredViolations() {
    // Given - 3 resources with violations, but 1 is ignored
    AwsResource resource1 = createResource("bucket-1", Map.of());
    AwsResource resource2 = createResource("bucket-2", Map.of());
    AwsResource resource3 = createResource("bucket-3", Map.of());

    createViolation(resource1, ViolationStatus.OPEN);
    createViolation(resource2, ViolationStatus.IGNORED); // Not counted
    createViolation(resource3, ViolationStatus.OPEN);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then - Only 2 resources are non-compliant (resource2's violation is ignored)
    assertThat(response.getTotalResources()).isEqualTo(3L);
    assertThat(response.getCompliantResources()).isEqualTo(1L);
    assertThat(response.getNonCompliantResources()).isEqualTo(2L);
    assertThat(response.getComplianceRate()).isCloseTo(33.33,
        org.assertj.core.data.Offset.offset(0.01));
  }

  @Test
  @DisplayName("Should handle multiple AWS accounts for same user")
  void getComplianceRate_MultipleAccounts() {
    // Given - Create second account for same user
    AwsAccount account2 = new AwsAccount();
    account2.setUser(testUser);
    account2.setAccountId("987654321098");
    account2.setAccountAlias("Second Account");
    account2.setRoleArn("arn:aws:iam::987654321098:role/TestRole");
    account2.setExternalId("external-id-67890");
    account2.setCredentialType(CredentialType.ROLE);
    account2.setStatus(AwsAccountStatus.ACTIVE);
    account2 = accountRepository.save(account2);

    // Create resources in both accounts
    AwsResource resource1 = createResourceForAccount(testAccount, "bucket-1", Map.of());
    AwsResource resource2 = createResourceForAccount(account2, "bucket-2",
        Map.of("Environment", "Prod"));
    AwsResource resource3 = createResourceForAccount(account2, "bucket-3", Map.of());

    // Create violations
    createViolation(resource1, ViolationStatus.OPEN);
    createViolation(resource3, ViolationStatus.OPEN);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then - Should count resources from both accounts
    assertThat(response.getTotalResources()).isEqualTo(3L);
    assertThat(response.getCompliantResources()).isEqualTo(1L);
    assertThat(response.getNonCompliantResources()).isEqualTo(2L);
    assertThat(response.getComplianceRate()).isCloseTo(33.33,
        org.assertj.core.data.Offset.offset(0.01));
  }

  @Test
  @DisplayName("Should isolate compliance rate by user")
  void getComplianceRate_UserIsolation() {
    // Given - Create second user with their own account and resources
    User otherUser = new User();
    otherUser.setEmail("other@example.com");
    otherUser.setName("Other User");
    otherUser.setPasswordHash(passwordEncoder.encode("password123"));
    otherUser.setRole(UserRole.USER);
    otherUser.setEnabled(true);
    otherUser = userRepository.save(otherUser);

    AwsAccount otherAccount = new AwsAccount();
    otherAccount.setUser(otherUser);
    otherAccount.setAccountId("111111111111");
    otherAccount.setAccountAlias("Other Account");
    otherAccount.setRoleArn("arn:aws:iam::111111111111:role/OtherRole");
    otherAccount.setExternalId("other-external-id");
    otherAccount.setCredentialType(CredentialType.ROLE);
    otherAccount.setStatus(AwsAccountStatus.ACTIVE);
    otherAccount = accountRepository.save(otherAccount);

    // Create resources for test user (2 total, 1 non-compliant)
    AwsResource testResource1 = createResourceForAccount(testAccount, "bucket-1", Map.of());
    AwsResource testResource2 = createResourceForAccount(testAccount, "bucket-2",
        Map.of("Environment", "Prod"));
    createViolation(testResource1, ViolationStatus.OPEN);

    // Create resources for other user (10 total, all non-compliant)
    for (int i = 0; i < 10; i++) {
      AwsResource otherResource = createResourceForAccount(otherAccount, "other-bucket-" + i,
          Map.of());
      createViolationForResource(otherResource, otherUser);
    }

    // When - Get compliance rate for test user only
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then - Should only include test user's resources
    assertThat(response.getTotalResources()).isEqualTo(2L);
    assertThat(response.getCompliantResources()).isEqualTo(1L);
    assertThat(response.getNonCompliantResources()).isEqualTo(1L);
    assertThat(response.getComplianceRate()).isEqualTo(50.0);

    // Verify other user has different stats
    ComplianceRateResponse otherResponse = dashboardService.getComplianceRate(otherUser.getId());
    assertThat(otherResponse.getTotalResources()).isEqualTo(10L);
    assertThat(otherResponse.getComplianceRate()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("Should handle resources of different types")
  void getComplianceRate_DifferentResourceTypes() {
    // Given - Create resources of different types
    AwsResource s3Resource = createResourceOfType("s3:bucket", "my-bucket", Map.of());
    AwsResource ec2Resource = createResourceOfType("ec2:instance", "my-instance",
        Map.of("Environment", "Prod"));
    AwsResource rdsResource = createResourceOfType("rds:db", "my-database", Map.of());

    // Create violations
    createViolation(s3Resource, ViolationStatus.OPEN);
    createViolation(rdsResource, ViolationStatus.OPEN);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(testUser.getId());

    // Then
    assertThat(response.getTotalResources()).isEqualTo(3L);
    assertThat(response.getCompliantResources()).isEqualTo(1L);
    assertThat(response.getNonCompliantResources()).isEqualTo(2L);
    assertThat(response.getComplianceRate()).isCloseTo(33.33,
        org.assertj.core.data.Offset.offset(0.01));
  }

  // Helper methods

  private AwsResource createResource(String name, Map<String, String> tags) {
    return createResourceForAccount(testAccount, name, tags);
  }

  private AwsResource createResourceForAccount(AwsAccount account, String name,
      Map<String, String> tags) {
    AwsResource resource = new AwsResource();
    resource.setAwsAccount(account);
    resource.setResourceId(name + "-id");
    resource.setResourceArn("arn:aws:s3:::" + name);
    resource.setResourceType("s3:bucket");
    resource.setRegion("us-east-1");
    resource.setName(name);
    resource.setTags(tags.isEmpty() ? null : tags);
    resource.setDiscoveredAt(LocalDateTime.now());
    resource.setLastSeenAt(LocalDateTime.now());
    return resourceRepository.save(resource);
  }

  private AwsResource createResourceOfType(String resourceType, String name,
      Map<String, String> tags) {
    AwsResource resource = new AwsResource();
    resource.setAwsAccount(testAccount);
    resource.setResourceId(name + "-id");
    resource.setResourceArn("arn:aws:" + resourceType.split(":")[0] + ":::" + name);
    resource.setResourceType(resourceType);
    resource.setRegion("us-east-1");
    resource.setName(name);
    resource.setTags(tags.isEmpty() ? null : tags);
    resource.setDiscoveredAt(LocalDateTime.now());
    resource.setLastSeenAt(LocalDateTime.now());
    return resourceRepository.save(resource);
  }

  private ComplianceViolation createViolation(AwsResource resource, ViolationStatus status) {
    return createViolationWithPolicy(resource, testPolicy, status);
  }

  private ComplianceViolation createViolationWithPolicy(AwsResource resource, TagPolicy policy,
      ViolationStatus status) {
    ComplianceViolation violation = new ComplianceViolation();
    violation.setAwsResource(resource);
    violation.setTagPolicy(policy);
    violation.setScanJob(testScanJob);
    violation.setStatus(status);

    Map<String, Object> details = new HashMap<>();
    details.put("missingTags", java.util.List.of("Environment"));
    violation.setViolationDetails(details);

    violation.setDetectedAt(LocalDateTime.now());
    if (status == ViolationStatus.RESOLVED) {
      violation.setResolvedAt(LocalDateTime.now());
    }
    return violationRepository.save(violation);
  }

  private ComplianceViolation createViolationForResource(AwsResource resource, User user) {
    // Create a policy for this user
    TagPolicy policy = createPolicy("Policy for " + user.getName(),
        Map.of("Environment", java.util.Collections.emptyList()));

    ComplianceViolation violation = new ComplianceViolation();
    violation.setAwsResource(resource);
    violation.setTagPolicy(policy);
    violation.setScanJob(testScanJob);
    violation.setStatus(ViolationStatus.OPEN);

    Map<String, Object> details = new HashMap<>();
    details.put("missingTags", java.util.List.of("Environment"));
    violation.setViolationDetails(details);

    violation.setDetectedAt(LocalDateTime.now());
    return violationRepository.save(violation);
  }

  private TagPolicy createPolicy(String name, Map<String, List<String>> requiredTags) {
    TagPolicy policy = new TagPolicy();
    policy.setUser(testUser);
    policy.setName(name);
    policy.setDescription("Test policy");
    policy.setRequiredTags(requiredTags);
    policy.setResourceTypes(java.util.List.of("s3:bucket"));
    policy.setSeverity(Severity.MEDIUM);
    policy.setEnabled(true);
    return policyRepository.save(policy);
  }
}