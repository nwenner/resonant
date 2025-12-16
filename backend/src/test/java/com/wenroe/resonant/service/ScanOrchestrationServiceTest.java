package com.wenroe.resonant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.ScanJob;
import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.model.enums.ScanStatus;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.repository.ScanJobRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.aws.scanners.CloudFrontResourceScanner;
import com.wenroe.resonant.service.aws.scanners.S3ResourceScanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScanOrchestrationService Async Tests")
class ScanOrchestrationServiceAsyncTest {

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
  private CloudFrontResourceScanner cloudFrontResourceScanner;

  @Mock
  private ComplianceEvaluationService complianceEvaluationService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AwsAccountRegionService regionService;

  @InjectMocks
  private ScanOrchestrationService orchestrationService;

  @Captor
  private ArgumentCaptor<ScanJob> scanJobCaptor;

  private User testUser;
  private AwsAccount testAccount;
  private ScanJob testScanJob;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setEmail("test@example.com");

    testAccount = new AwsAccount();
    testAccount.setId(UUID.randomUUID());
    testAccount.setUser(testUser);
    testAccount.setAccountId("123456789012");
    testAccount.setAccountAlias("test-account");
    testAccount.setStatus(AwsAccountStatus.ACTIVE);
    testAccount.setRoleArn("arn:aws:iam::123456789012:role/test-role");
    testAccount.setExternalId("external-123");

    testScanJob = new ScanJob();
    testScanJob.setId(UUID.randomUUID());
    testScanJob.setAwsAccount(testAccount);
    testScanJob.setUser(testUser);
    testScanJob.setStatus(ScanStatus.PENDING);
  }

  @Test
  @DisplayName("Should initiate scan and return PENDING scan job")
  void shouldInitiateScan() {
    // Given
    when(awsAccountRepository.findById(testAccount.getId())).thenReturn(Optional.of(testAccount));
    when(regionService.hasEnabledRegions(testAccount.getId())).thenReturn(true);
    when(scanJobRepository.findRunningScanForAccount(testAccount.getId()))
        .thenReturn(Optional.empty());
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
    when(scanJobRepository.save(any(ScanJob.class))).thenReturn(testScanJob);

    // When
    ScanJob result = orchestrationService.initiateScan(testAccount.getId(), testUser.getId());

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testScanJob.getId());
    assertThat(result.getStatus()).isEqualTo(ScanStatus.PENDING);

    verify(scanJobRepository).save(any(ScanJob.class));
  }

  @Test
  @DisplayName("Should reject scan when account not found")
  void shouldRejectScanWhenAccountNotFound() {
    // Given
    when(awsAccountRepository.findById(testAccount.getId())).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(
        () -> orchestrationService.initiateScan(testAccount.getId(), testUser.getId()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("AWS account not found");
  }

  @Test
  @DisplayName("Should reject scan when user not authorized")
  void shouldRejectScanWhenNotAuthorized() {
    // Given
    UUID otherUserId = UUID.randomUUID();
    when(awsAccountRepository.findById(testAccount.getId())).thenReturn(Optional.of(testAccount));

    // When/Then
    assertThatThrownBy(() -> orchestrationService.initiateScan(testAccount.getId(), otherUserId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Not authorized");
  }

  @Test
  @DisplayName("Should reject scan when account is not active")
  void shouldRejectScanWhenAccountInactive() {
    // Given
    testAccount.setStatus(AwsAccountStatus.INVALID);
    when(awsAccountRepository.findById(testAccount.getId())).thenReturn(Optional.of(testAccount));

    // When/Then
    assertThatThrownBy(
        () -> orchestrationService.initiateScan(testAccount.getId(), testUser.getId()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("not active");
  }

  @Test
  @DisplayName("Should reject scan when no regions enabled")
  void shouldRejectScanWhenNoRegionsEnabled() {
    // Given
    when(awsAccountRepository.findById(testAccount.getId())).thenReturn(Optional.of(testAccount));
    when(regionService.hasEnabledRegions(testAccount.getId())).thenReturn(false);

    // When/Then
    assertThatThrownBy(
        () -> orchestrationService.initiateScan(testAccount.getId(), testUser.getId()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("No regions enabled");
  }

  @Test
  @DisplayName("Should reject scan when scan already running")
  void shouldRejectScanWhenAlreadyRunning() {
    // Given
    ScanJob runningScan = new ScanJob();
    runningScan.setStatus(ScanStatus.RUNNING);

    when(awsAccountRepository.findById(testAccount.getId())).thenReturn(Optional.of(testAccount));
    when(regionService.hasEnabledRegions(testAccount.getId())).thenReturn(true);
    when(scanJobRepository.findRunningScanForAccount(testAccount.getId()))
        .thenReturn(Optional.of(runningScan));

    // When/Then
    assertThatThrownBy(
        () -> orchestrationService.initiateScan(testAccount.getId(), testUser.getId()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("scan is already running");
  }

  @Test
  @DisplayName("Should execute scan with parallel scanners")
  void shouldExecuteScanWithParallelScanners() {
    // Given
    when(scanJobRepository.findById(testScanJob.getId())).thenReturn(Optional.of(testScanJob));
    when(tagPolicyService.getEnabledPoliciesByUserId(testUser.getId()))
        .thenReturn(List.of(new TagPolicy()));

    AwsResource s3Resource = new AwsResource();
    s3Resource.setResourceArn("arn:aws:s3:::test-bucket");
    s3Resource.setResourceType("s3:bucket");

    AwsResource cfResource = new AwsResource();
    cfResource.setResourceArn("arn:aws:cloudfront::123456789012:distribution/DIST123");
    cfResource.setResourceType("cloudfront:distribution");

    when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(List.of(s3Resource));
    when(cloudFrontResourceScanner.scanDistributions(testAccount))
        .thenReturn(List.of(cfResource));

    when(awsResourceRepository.findByResourceArn(any())).thenReturn(Optional.empty());
    when(awsResourceRepository.save(any(AwsResource.class))).thenAnswer(i -> i.getArgument(0));
    when(complianceEvaluationService.evaluateResource(any(), any())).thenReturn(new ArrayList<>());

    // When
    orchestrationService.executeScan(testScanJob.getId());

    // Then
    verify(s3ResourceScanner).scanS3Buckets(testAccount);
    verify(cloudFrontResourceScanner).scanDistributions(testAccount);
    verify(awsResourceRepository, times(2)).save(any(AwsResource.class));

    // Verify scan completed successfully
    // Note: ArgumentCaptor captures same object reference, so both captures will have final state
    verify(scanJobRepository, times(2)).save(scanJobCaptor.capture());

    // Check final state after both saves
    ScanJob finalState = scanJobCaptor.getValue();
    assertThat(finalState.getStatus()).isEqualTo(ScanStatus.SUCCESS);
    assertThat(finalState.getResourcesScanned()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should handle scan execution failure")
  void shouldHandleScanExecutionFailure() {
    // Given
    when(scanJobRepository.findById(testScanJob.getId())).thenReturn(Optional.of(testScanJob));
    when(tagPolicyService.getEnabledPoliciesByUserId(testUser.getId()))
        .thenThrow(new RuntimeException("Database connection failed"));

    // When/Then
    assertThatThrownBy(() -> orchestrationService.executeScan(testScanJob.getId()))
        .isInstanceOf(RuntimeException.class);

    // Verify scan marked as failed
    verify(scanJobRepository, times(2)).save(scanJobCaptor.capture());
    List<ScanJob> savedJobs = scanJobCaptor.getAllValues();

    assertThat(savedJobs.get(1).getStatus()).isEqualTo(ScanStatus.FAILED);
    assertThat(savedJobs.get(1).getErrorMessage()).contains("Database connection failed");
  }
}