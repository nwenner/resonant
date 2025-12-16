package com.wenroe.resonant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.ComplianceViolation;
import com.wenroe.resonant.model.entity.ScanJob;
import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.model.enums.ScanStatus;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.repository.ScanJobRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.aws.scanners.S3ResourceScanner;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScanOrchestrationService Tests")
class ScanOrchestrationServiceTest {

  @Mock
  private AwsAccountRepository awsAccountRepository;

  @Mock
  private AwsResourceRepository awsResourceRepository;

  @Mock
  private ScanJobRepository scanJobRepository;

  @Mock
  private TagPolicyService tagPolicyService;

  @Mock
  private S3ResourceScanner s3ResourceScanner;

  @Mock
  private ComplianceEvaluationService complianceEvaluationService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AwsAccountRegionService regionService;

  @InjectMocks
  private ScanOrchestrationService scanOrchestrationService;

  private AwsAccount testAccount;
  private User testUser;
  private UUID accountId;
  private UUID userId;

  @BeforeEach
  void setUp() {
    accountId = UUID.randomUUID();
    userId = UUID.randomUUID();

    testUser = new User();
    testUser.setId(userId);
    testUser.setEmail("test@example.com");

    testAccount = new AwsAccount();
    testAccount.setId(accountId);
    testAccount.setAccountId("123456789012");
    testAccount.setUser(testUser);
    testAccount.setStatus(AwsAccountStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should initiate scan successfully")
  void shouldInitiateScanSuccessfully() {
    // Given
    ScanJob scanJob = new ScanJob();
    scanJob.setId(UUID.randomUUID());
    scanJob.setAwsAccount(testAccount);
    scanJob.setUser(testUser);
    scanJob.setStatus(ScanStatus.PENDING);

    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionService.hasEnabledRegions(accountId)).thenReturn(true);
    when(scanJobRepository.findRunningScanForAccount(accountId)).thenReturn(Optional.empty());
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    when(scanJobRepository.save(any(ScanJob.class))).thenReturn(scanJob);

    // Mock scanner and evaluation
    when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(List.of());
    when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(List.of());

    // When
    ScanJob result = scanOrchestrationService.initiateScan(accountId, userId);

    // Then
    assertThat(result).isNotNull();
    verify(awsAccountRepository).findById(accountId);
    verify(regionService).hasEnabledRegions(accountId);
    verify(scanJobRepository).findRunningScanForAccount(accountId);
    verify(userRepository).findById(userId);
    verify(scanJobRepository, times(3)).save(any(ScanJob.class)); // Create, start, complete
  }

  @Test
  @DisplayName("Should throw exception when account not found")
  void shouldThrowExceptionWhenAccountNotFound() {
    // Given
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("AWS account not found");

    verify(regionService, never()).hasEnabledRegions(any());
  }

  @Test
  @DisplayName("Should throw exception when user not authorized")
  void shouldThrowExceptionWhenUserNotAuthorized() {
    // Given
    UUID wrongUserId = UUID.randomUUID();
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    // When/Then
    assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, wrongUserId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Not authorized to scan this AWS account");

    verify(regionService, never()).hasEnabledRegions(any());
  }

  @Test
  @DisplayName("Should throw exception when account is not active")
  void shouldThrowExceptionWhenAccountNotActive() {
    // Given
    testAccount.setStatus(AwsAccountStatus.INVALID);
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    // When/Then
    assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("AWS account is not active");

    verify(regionService, never()).hasEnabledRegions(any());
  }

  @Test
  @DisplayName("Should throw exception when no regions enabled")
  void shouldThrowExceptionWhenNoRegionsEnabled() {
    // Given
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionService.hasEnabledRegions(accountId)).thenReturn(false);

    // When/Then
    assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("No regions enabled for scanning")
        .hasMessageContaining("Please enable at least one region");

    verify(scanJobRepository, never()).save(any(ScanJob.class));
  }

  @Test
  @DisplayName("Should throw exception when scan already running")
  void shouldThrowExceptionWhenScanAlreadyRunning() {
    // Given
    ScanJob existingScan = new ScanJob();
    existingScan.setId(UUID.randomUUID());
    existingScan.setStatus(ScanStatus.RUNNING);

    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionService.hasEnabledRegions(accountId)).thenReturn(true);
    when(scanJobRepository.findRunningScanForAccount(accountId))
        .thenReturn(Optional.of(existingScan));

    // When/Then
    assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("A scan is already running for this account");

    verify(userRepository, never()).findById(any());
  }

  @Test
  @DisplayName("Should throw exception when user not found")
  void shouldThrowExceptionWhenUserNotFound() {
    // Given
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionService.hasEnabledRegions(accountId)).thenReturn(true);
    when(scanJobRepository.findRunningScanForAccount(accountId)).thenReturn(Optional.empty());
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  @DisplayName("Should execute scan and discover resources")
  void shouldExecuteScanAndDiscoverResources() {
    // Given
    ScanJob scanJob = createScanJob();
    List<TagPolicy> policies = List.of(createTagPolicy());
    List<AwsResource> discoveredResources = List.of(
        createResource("bucket-1"),
        createResource("bucket-2")
    );

    when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(policies);
    when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(discoveredResources);
    when(awsResourceRepository.findByResourceArn(any())).thenReturn(Optional.empty());
    when(awsResourceRepository.save(any(AwsResource.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(complianceEvaluationService.evaluateResource(any(), anyList()))
        .thenReturn(List.of());
    when(awsAccountRepository.save(testAccount)).thenReturn(testAccount);
    when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(i -> i.getArgument(0));

    // When
    scanOrchestrationService.executeScan(scanJob);

    // Then
    verify(s3ResourceScanner).scanS3Buckets(testAccount);
    verify(awsResourceRepository, times(2)).save(any(AwsResource.class));
    verify(complianceEvaluationService, times(2)).evaluateResource(any(), eq(policies));
    verify(awsAccountRepository).save(testAccount);

    assertThat(scanJob.getStatus()).isEqualTo(ScanStatus.SUCCESS);
    assertThat(scanJob.getResourcesScanned()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should update existing resources during scan")
  void shouldUpdateExistingResourcesDuringScan() {
    // Given
    ScanJob scanJob = createScanJob();
    AwsResource existingResource = createResource("bucket-1");
    existingResource.setId(UUID.randomUUID());
    AwsResource discoveredResource = createResource("bucket-1");
    Map<String, String> newTags = new HashMap<>();
    newTags.put("Environment", "Production");
    discoveredResource.setTags(newTags);

    when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(List.of());
    when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(List.of(discoveredResource));
    when(awsResourceRepository.findByResourceArn(discoveredResource.getResourceArn()))
        .thenReturn(Optional.of(existingResource));
    when(awsResourceRepository.save(any(AwsResource.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(complianceEvaluationService.evaluateResource(any(), anyList()))
        .thenReturn(List.of());
    when(awsAccountRepository.save(testAccount)).thenReturn(testAccount);
    when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(i -> i.getArgument(0));

    // When
    scanOrchestrationService.executeScan(scanJob);

    // Then
    ArgumentCaptor<AwsResource> captor = ArgumentCaptor.forClass(AwsResource.class);
    verify(awsResourceRepository).save(captor.capture());

    AwsResource saved = captor.getValue();
    assertThat(saved.getId()).isEqualTo(existingResource.getId());
    assertThat(saved.getTags()).isEqualTo(newTags);
  }

  @Test
  @DisplayName("Should track violations found during scan")
  void shouldTrackViolationsFoundDuringScan() {
    // Given
    ScanJob scanJob = createScanJob();
    List<AwsResource> resources = List.of(createResource("bucket-1"));
    List<ComplianceViolation> violations = List.of(
        createViolation(),
        createViolation()
    );

    when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(List.of());
    when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(resources);
    when(awsResourceRepository.findByResourceArn(any())).thenReturn(Optional.empty());
    when(awsResourceRepository.save(any(AwsResource.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(complianceEvaluationService.evaluateResource(any(), anyList()))
        .thenReturn(violations);
    when(awsAccountRepository.save(testAccount)).thenReturn(testAccount);
    when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(i -> i.getArgument(0));

    // When
    scanOrchestrationService.executeScan(scanJob);

    // Then
    assertThat(scanJob.getViolationsFound()).isEqualTo(2);
    assertThat(scanJob.getStatus()).isEqualTo(ScanStatus.SUCCESS);
  }

  @Test
  @DisplayName("Should update account last scan time")
  void shouldUpdateAccountLastScanTime() {
    // Given
    ScanJob scanJob = createScanJob();
    LocalDateTime beforeScan = LocalDateTime.now();

    when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(List.of());
    when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(List.of());
    when(awsAccountRepository.save(testAccount)).thenReturn(testAccount);
    when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(i -> i.getArgument(0));

    // When
    scanOrchestrationService.executeScan(scanJob);

    // Then
    ArgumentCaptor<AwsAccount> captor = ArgumentCaptor.forClass(AwsAccount.class);
    verify(awsAccountRepository).save(captor.capture());

    AwsAccount saved = captor.getValue();
    assertThat(saved.getLastScanAt()).isNotNull();
    assertThat(saved.getLastScanAt()).isAfterOrEqualTo(beforeScan);
  }

  @Test
  @DisplayName("Should fail scan job on exception")
  void shouldFailScanJobOnException() {
    // Given
    ScanJob scanJob = createScanJob();

    when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(List.of());
    when(s3ResourceScanner.scanS3Buckets(testAccount))
        .thenThrow(new RuntimeException("S3 scan failed"));
    when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(i -> i.getArgument(0));

    // When/Then
    assertThatThrownBy(() -> scanOrchestrationService.executeScan(scanJob))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("S3 scan failed");

    assertThat(scanJob.getStatus()).isEqualTo(ScanStatus.FAILED);
    assertThat(scanJob.getErrorMessage()).contains("S3 scan failed");
  }

  @Test
  @DisplayName("Should link violations to scan job")
  void shouldLinkViolationsToScanJob() {
    // Given
    ScanJob scanJob = createScanJob();
    scanJob.setId(UUID.randomUUID());
    List<AwsResource> resources = List.of(createResource("bucket-1"));
    ComplianceViolation violation1 = createViolation();
    ComplianceViolation violation2 = createViolation();
    List<ComplianceViolation> violations = List.of(violation1, violation2);

    when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(List.of());
    when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(resources);
    when(awsResourceRepository.findByResourceArn(any())).thenReturn(Optional.empty());
    when(awsResourceRepository.save(any(AwsResource.class)))
        .thenAnswer(i -> i.getArgument(0));
    when(complianceEvaluationService.evaluateResource(any(), anyList()))
        .thenReturn(violations);
    when(awsAccountRepository.save(testAccount)).thenReturn(testAccount);
    when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(i -> i.getArgument(0));

    // When
    scanOrchestrationService.executeScan(scanJob);

    // Then
    assertThat(violation1.getScanJob()).isEqualTo(scanJob);
    assertThat(violation2.getScanJob()).isEqualTo(scanJob);
  }

  @Test
  @DisplayName("Should get scan job by id")
  void shouldGetScanJobById() {
    // Given
    UUID scanJobId = UUID.randomUUID();
    ScanJob scanJob = createScanJob();
    scanJob.setId(scanJobId);

    when(scanJobRepository.findById(scanJobId)).thenReturn(Optional.of(scanJob));

    // When
    ScanJob result = scanOrchestrationService.getScanJob(scanJobId);

    // Then
    assertThat(result).isEqualTo(scanJob);
    verify(scanJobRepository).findById(scanJobId);
  }

  @Test
  @DisplayName("Should throw exception when scan job not found")
  void shouldThrowExceptionWhenScanJobNotFound() {
    // Given
    UUID scanJobId = UUID.randomUUID();
    when(scanJobRepository.findById(scanJobId)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> scanOrchestrationService.getScanJob(scanJobId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Scan job not found");
  }

  @Test
  @DisplayName("Should get scan jobs by user id")
  void shouldGetScanJobsByUserId() {
    // Given
    List<ScanJob> scanJobs = List.of(createScanJob(), createScanJob());
    when(scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(scanJobs);

    // When
    List<ScanJob> result = scanOrchestrationService.getScanJobsByUserId(userId);

    // Then
    assertThat(result).hasSize(2);
    verify(scanJobRepository).findByUserIdOrderByCreatedAtDesc(userId);
  }

  @Test
  @DisplayName("Should get scan jobs by account id")
  void shouldGetScanJobsByAccountId() {
    // Given
    List<ScanJob> scanJobs = List.of(createScanJob(), createScanJob());
    when(scanJobRepository.findByAwsAccountIdOrderByCreatedAtDesc(accountId))
        .thenReturn(scanJobs);

    // When
    List<ScanJob> result = scanOrchestrationService.getScanJobsByAccountId(accountId);

    // Then
    assertThat(result).hasSize(2);
    verify(scanJobRepository).findByAwsAccountIdOrderByCreatedAtDesc(accountId);
  }

  @Test
  @DisplayName("Should get last scan for account")
  void shouldGetLastScanForAccount() {
    // Given
    ScanJob lastScan = createScanJob();
    when(scanJobRepository.findFirstByAwsAccountIdOrderByCreatedAtDesc(accountId))
        .thenReturn(Optional.of(lastScan));

    // When
    Optional<ScanJob> result = scanOrchestrationService.getLastScanForAccount(accountId);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(lastScan);
  }

  @Test
  @DisplayName("Should return empty when no scans for account")
  void shouldReturnEmptyWhenNoScansForAccount() {
    // Given
    when(scanJobRepository.findFirstByAwsAccountIdOrderByCreatedAtDesc(accountId))
        .thenReturn(Optional.empty());

    // When
    Optional<ScanJob> result = scanOrchestrationService.getLastScanForAccount(accountId);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should warn when no policies enabled")
  void shouldWarnWhenNoPoliciesEnabled() {
    // Given
    ScanJob scanJob = createScanJob();

    when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(List.of());
    when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(List.of());
    when(awsAccountRepository.save(testAccount)).thenReturn(testAccount);
    when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(i -> i.getArgument(0));

    // When
    scanOrchestrationService.executeScan(scanJob);

    // Then
    assertThat(scanJob.getStatus()).isEqualTo(ScanStatus.SUCCESS);
    // Should still complete successfully, just with 0 violations
    assertThat(scanJob.getViolationsFound()).isEqualTo(0);
  }

  private ScanJob createScanJob() {
    ScanJob scanJob = new ScanJob();
    scanJob.setAwsAccount(testAccount);
    scanJob.setUser(testUser);
    scanJob.setStatus(ScanStatus.PENDING);
    return scanJob;
  }

  private AwsResource createResource(String name) {
    AwsResource resource = new AwsResource();
    resource.setAwsAccount(testAccount);
    resource.setResourceId(name);
    resource.setResourceArn("arn:aws:s3:::" + name);
    resource.setResourceType("s3:bucket");
    resource.setRegion("us-east-1");
    resource.setName(name);
    resource.setTags(new HashMap<>());
    resource.setMetadata(new HashMap<>());
    return resource;
  }

  private TagPolicy createTagPolicy() {
    TagPolicy policy = new TagPolicy();
    policy.setId(UUID.randomUUID());
    policy.setName("Test Policy");
    policy.setEnabled(true);
    return policy;
  }

  private ComplianceViolation createViolation() {
    ComplianceViolation violation = new ComplianceViolation();
    violation.setId(UUID.randomUUID());
    return violation;
  }
}