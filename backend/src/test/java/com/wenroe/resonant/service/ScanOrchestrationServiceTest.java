package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.*;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.model.enums.ScanStatus;
import com.wenroe.resonant.repository.*;
import com.wenroe.resonant.service.aws.scanners.S3ResourceScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private ScanOrchestrationService scanOrchestrationService;

    private User testUser;
    private AwsAccount testAccount;
    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");

        testAccount = new AwsAccount();
        testAccount.setId(accountId);
        testAccount.setUser(testUser);
        testAccount.setAccountId("123456789012");
        testAccount.setStatus(AwsAccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should initiate scan successfully")
    void initiateScan_Success() {
        // Given
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(scanJobRepository.findRunningScanForAccount(accountId)).thenReturn(Optional.empty());
        when(scanJobRepository.save(any(ScanJob.class))).thenAnswer(invocation -> {
            ScanJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });
        when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(Collections.emptyList());
        when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(Collections.emptyList());

        // When
        ScanJob result = scanOrchestrationService.initiateScan(accountId, userId);

        // Then
        assertThat(result).isNotNull();
        verify(scanJobRepository, atLeastOnce()).save(any(ScanJob.class));
    }

    @Test
    @DisplayName("Should throw exception when account not found")
    void initiateScan_AccountNotFound() {
        // Given
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AWS account not found");

        verify(scanJobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when not owner")
    void initiateScan_NotOwner() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, otherUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized to scan this AWS account");
    }

    @Test
    @DisplayName("Should throw exception when account not active")
    void initiateScan_AccountNotActive() {
        // Given
        testAccount.setStatus(AwsAccountStatus.INVALID);
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AWS account is not active");
    }

    @Test
    @DisplayName("Should throw exception when scan already running")
    void initiateScan_AlreadyRunning() {
        // Given
        ScanJob runningScan = new ScanJob();
        runningScan.setStatus(ScanStatus.RUNNING);

        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(scanJobRepository.findRunningScanForAccount(accountId))
                .thenReturn(Optional.of(runningScan));

        // When & Then
        assertThatThrownBy(() -> scanOrchestrationService.initiateScan(accountId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A scan is already running for this account");
    }

    @Test
    @DisplayName("Should execute scan and find resources")
    void executeScan_Success() {
        // Given
        ScanJob scanJob = new ScanJob();
        scanJob.setId(UUID.randomUUID());
        scanJob.setAwsAccount(testAccount);
        scanJob.setUser(testUser);
        scanJob.setStatus(ScanStatus.PENDING);

        TagPolicy policy = new TagPolicy();
        policy.setId(UUID.randomUUID());
        policy.setResourceTypes(List.of("s3:bucket"));

        AwsResource resource = new AwsResource();
        resource.setResourceArn("arn:aws:s3:::test-bucket");
        resource.setResourceType("s3:bucket");
        resource.setTags(new HashMap<>());

        when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(List.of(policy));
        when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(List.of(resource));
        when(awsResourceRepository.findByResourceArn(anyString())).thenReturn(Optional.empty());
        when(awsResourceRepository.save(any(AwsResource.class))).thenReturn(resource);
        when(complianceEvaluationService.evaluateResource(any(), any()))
                .thenReturn(Collections.emptyList());
        when(scanJobRepository.save(any(ScanJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

        // When
        scanOrchestrationService.executeScan(scanJob);

        // Then
        verify(s3ResourceScanner).scanS3Buckets(testAccount);
        verify(awsResourceRepository).save(resource);
        verify(complianceEvaluationService).evaluateResource(resource, List.of(policy));
        verify(scanJobRepository, atLeastOnce()).save(argThat(job ->
                job.getStatus() == ScanStatus.SUCCESS
        ));
    }

    @Test
    @DisplayName("Should update existing resources during scan")
    void executeScan_UpdateExistingResource() {
        // Given
        ScanJob scanJob = new ScanJob();
        scanJob.setId(UUID.randomUUID());
        scanJob.setAwsAccount(testAccount);
        scanJob.setUser(testUser);

        AwsResource existingResource = new AwsResource();
        existingResource.setId(UUID.randomUUID());
        existingResource.setResourceArn("arn:aws:s3:::test-bucket");

        AwsResource discoveredResource = new AwsResource();
        discoveredResource.setResourceArn("arn:aws:s3:::test-bucket");
        discoveredResource.setTags(Map.of("Environment", "prod"));

        when(tagPolicyService.getEnabledPoliciesByUserId(userId)).thenReturn(Collections.emptyList());
        when(s3ResourceScanner.scanS3Buckets(testAccount)).thenReturn(List.of(discoveredResource));
        when(awsResourceRepository.findByResourceArn("arn:aws:s3:::test-bucket"))
                .thenReturn(Optional.of(existingResource));
        when(awsResourceRepository.save(any(AwsResource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(scanJobRepository.save(any(ScanJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

        // When
        scanOrchestrationService.executeScan(scanJob);

        // Then
        verify(awsResourceRepository).save(argThat(resource ->
                resource.getTags() != null && resource.getTags().containsKey("Environment")
        ));
    }

    @Test
    @DisplayName("Should handle scan failure")
    void executeScan_Failure() {
        // Given
        ScanJob scanJob = new ScanJob();
        scanJob.setId(UUID.randomUUID());
        scanJob.setAwsAccount(testAccount);
        scanJob.setUser(testUser);

        when(tagPolicyService.getEnabledPoliciesByUserId(userId))
                .thenThrow(new RuntimeException("AWS error"));
        when(scanJobRepository.save(any(ScanJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        assertThatThrownBy(() -> scanOrchestrationService.executeScan(scanJob))
                .isInstanceOf(RuntimeException.class);

        verify(scanJobRepository, atLeastOnce()).save(argThat(job ->
                job.getStatus() == ScanStatus.FAILED &&
                        job.getErrorMessage() != null
        ));
    }

    @Test
    @DisplayName("Should get scan job by ID")
    void getScanJob_Success() {
        // Given
        UUID scanJobId = UUID.randomUUID();
        ScanJob scanJob = new ScanJob();
        scanJob.setId(scanJobId);

        when(scanJobRepository.findById(scanJobId)).thenReturn(Optional.of(scanJob));

        // When
        ScanJob result = scanOrchestrationService.getScanJob(scanJobId);

        // Then
        assertThat(result).isEqualTo(scanJob);
    }

    @Test
    @DisplayName("Should throw exception when scan job not found")
    void getScanJob_NotFound() {
        // Given
        UUID scanJobId = UUID.randomUUID();
        when(scanJobRepository.findById(scanJobId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> scanOrchestrationService.getScanJob(scanJobId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Scan job not found");
    }

    @Test
    @DisplayName("Should get scan jobs by user ID")
    void getScanJobsByUserId_Success() {
        // Given
        ScanJob job1 = new ScanJob();
        ScanJob job2 = new ScanJob();

        when(scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(job1, job2));

        // When
        List<ScanJob> result = scanOrchestrationService.getScanJobsByUserId(userId);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should get scan jobs by account ID")
    void getScanJobsByAccountId_Success() {
        // Given
        ScanJob job = new ScanJob();

        when(scanJobRepository.findByAwsAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(List.of(job));

        // When
        List<ScanJob> result = scanOrchestrationService.getScanJobsByAccountId(accountId);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should get last scan for account")
    void getLastScanForAccount_Success() {
        // Given
        ScanJob lastScan = new ScanJob();

        when(scanJobRepository.findFirstByAwsAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(Optional.of(lastScan));

        // When
        Optional<ScanJob> result = scanOrchestrationService.getLastScanForAccount(accountId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(lastScan);
    }
}