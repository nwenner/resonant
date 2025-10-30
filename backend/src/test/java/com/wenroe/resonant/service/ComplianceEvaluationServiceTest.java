package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.ComplianceViolation;
import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.repository.ComplianceViolationRepository;
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
@DisplayName("ComplianceEvaluationService Tests")
class ComplianceEvaluationServiceTest {

    @Mock
    private ComplianceViolationRepository violationRepository;

    @InjectMocks
    private ComplianceEvaluationService complianceEvaluationService;

    private AwsResource testResource;
    private TagPolicy testPolicy;
    private UUID resourceId;
    private UUID policyId;

    @BeforeEach
    void setUp() {
        resourceId = UUID.randomUUID();
        policyId = UUID.randomUUID();

        // Setup test resource
        testResource = new AwsResource();
        testResource.setId(resourceId);
        testResource.setResourceArn("arn:aws:s3:::test-bucket");
        testResource.setResourceType("s3:bucket");
        testResource.setTags(new HashMap<>());

        // Setup test policy
        testPolicy = new TagPolicy();
        testPolicy.setId(policyId);
        testPolicy.setName("Test Policy");

        Map<String, List<String>> requiredTags = new HashMap<>();
        requiredTags.put("Environment", List.of("prod", "dev"));
        requiredTags.put("Owner", null);
        testPolicy.setRequiredTags(requiredTags);

        testPolicy.setResourceTypes(List.of("s3:bucket"));
        testPolicy.setSeverity(TagPolicy.Severity.HIGH);
        testPolicy.setEnabled(true);
    }

    @Test
    @DisplayName("Should detect missing tag violation")
    void evaluateResource_MissingTag() {
        // Given - resource has no tags
        when(violationRepository.findByAwsResourceIdAndTagPolicyId(resourceId, policyId))
                .thenReturn(Optional.empty());
        when(violationRepository.save(any(ComplianceViolation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(testResource, List.of(testPolicy));

        // Then
        assertThat(violations).hasSize(1);
        ComplianceViolation violation = violations.get(0);
        assertThat(violation.getStatus()).isEqualTo(ComplianceViolation.ViolationStatus.OPEN);

        @SuppressWarnings("unchecked")
        Map<String, Object> details = violation.getViolationDetails();
        assertThat(details).containsKey("missingTags");

        @SuppressWarnings("unchecked")
        List<String> missingTags = (List<String>) details.get("missingTags");
        assertThat(missingTags).contains("Environment", "Owner");

        verify(violationRepository).save(any(ComplianceViolation.class));
    }

    @Test
    @DisplayName("Should detect invalid tag value violation")
    void evaluateResource_InvalidValue() {
        // Given - resource has tag with wrong value
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "staging");  // Not in allowed values
        tags.put("Owner", "john@test.com");
        testResource.setTags(tags);

        when(violationRepository.findByAwsResourceIdAndTagPolicyId(resourceId, policyId))
                .thenReturn(Optional.empty());
        when(violationRepository.save(any(ComplianceViolation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(testResource, List.of(testPolicy));

        // Then
        assertThat(violations).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> details = violations.get(0).getViolationDetails();
        assertThat(details).containsKey("invalidTags");

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> invalidTags = (Map<String, Map<String, Object>>) details.get("invalidTags");
        assertThat(invalidTags).containsKey("Environment");
        assertThat(invalidTags.get("Environment").get("current")).isEqualTo("staging");

        verify(violationRepository).save(any(ComplianceViolation.class));
    }

    @Test
    @DisplayName("Should not create violation when resource is compliant")
    void evaluateResource_Compliant() {
        // Given - resource has all required tags with valid values
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "prod");
        tags.put("Owner", "john@test.com");
        testResource.setTags(tags);

        when(violationRepository.findByAwsResourceIdAndTagPolicyId(resourceId, policyId))
                .thenReturn(Optional.empty());

        // When
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(testResource, List.of(testPolicy));

        // Then
        assertThat(violations).isEmpty();
        verify(violationRepository, never()).save(any(ComplianceViolation.class));
    }

    @Test
    @DisplayName("Should auto-resolve existing violation when resource becomes compliant")
    void evaluateResource_AutoResolve() {
        // Given - resource is now compliant
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "prod");
        tags.put("Owner", "john@test.com");
        testResource.setTags(tags);

        ComplianceViolation existingViolation = new ComplianceViolation();
        existingViolation.setId(UUID.randomUUID());
        existingViolation.setStatus(ComplianceViolation.ViolationStatus.OPEN);
        existingViolation.setAwsResource(testResource);
        existingViolation.setTagPolicy(testPolicy);

        when(violationRepository.findByAwsResourceIdAndTagPolicyId(resourceId, policyId))
                .thenReturn(Optional.of(existingViolation));
        when(violationRepository.save(any(ComplianceViolation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(testResource, List.of(testPolicy));

        // Then
        assertThat(violations).isEmpty();
        verify(violationRepository).save(argThat(v ->
                v.getStatus() == ComplianceViolation.ViolationStatus.RESOLVED &&
                        v.getResolvedAt() != null
        ));
    }

    @Test
    @DisplayName("Should not auto-resolve ignored violations")
    void evaluateResource_DontResolveIgnored() {
        // Given - resource is compliant but violation is IGNORED
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "prod");
        tags.put("Owner", "john@test.com");
        testResource.setTags(tags);

        ComplianceViolation ignoredViolation = new ComplianceViolation();
        ignoredViolation.setId(UUID.randomUUID());
        ignoredViolation.setStatus(ComplianceViolation.ViolationStatus.IGNORED);

        when(violationRepository.findByAwsResourceIdAndTagPolicyId(resourceId, policyId))
                .thenReturn(Optional.of(ignoredViolation));

        // When
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(testResource, List.of(testPolicy));

        // Then
        assertThat(violations).isEmpty();
        verify(violationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reopen resolved violation if non-compliant again")
    void evaluateResource_ReopenResolved() {
        // Given - resource has missing tags again
        ComplianceViolation resolvedViolation = new ComplianceViolation();
        resolvedViolation.setId(UUID.randomUUID());
        resolvedViolation.setStatus(ComplianceViolation.ViolationStatus.RESOLVED);
        resolvedViolation.setAwsResource(testResource);
        resolvedViolation.setTagPolicy(testPolicy);

        when(violationRepository.findByAwsResourceIdAndTagPolicyId(resourceId, policyId))
                .thenReturn(Optional.of(resolvedViolation));
        when(violationRepository.save(any(ComplianceViolation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(testResource, List.of(testPolicy));

        // Then
        assertThat(violations).hasSize(1);
        verify(violationRepository).save(argThat(v ->
                v.getStatus() == ComplianceViolation.ViolationStatus.OPEN &&
                        v.getResolvedAt() == null
        ));
    }

    @Test
    @DisplayName("Should skip policies that don't apply to resource type")
    void evaluateResource_SkipNonApplicablePolicy() {
        // Given - policy for EC2, resource is S3
        testPolicy.setResourceTypes(List.of("ec2:instance"));

        // When
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(testResource, List.of(testPolicy));

        // Then
        assertThat(violations).isEmpty();
        verify(violationRepository, never()).findByAwsResourceIdAndTagPolicyId(any(), any());
    }

    @Test
    @DisplayName("Should accept null allowed values as any value")
    void evaluateResource_NullAllowedValues() {
        // Given - Owner tag has null allowed values (any value OK)
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "prod");
        tags.put("Owner", "anything-is-fine");
        testResource.setTags(tags);

        when(violationRepository.findByAwsResourceIdAndTagPolicyId(resourceId, policyId))
                .thenReturn(Optional.empty());

        // When
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(testResource, List.of(testPolicy));

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should ignore violation")
    void ignoreViolation_Success() {
        // Given
        UUID violationId = UUID.randomUUID();
        ComplianceViolation violation = new ComplianceViolation();
        violation.setId(violationId);
        violation.setStatus(ComplianceViolation.ViolationStatus.OPEN);

        when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));
        when(violationRepository.save(any(ComplianceViolation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceViolation result = complianceEvaluationService.ignoreViolation(violationId);

        // Then
        assertThat(result.getStatus()).isEqualTo(ComplianceViolation.ViolationStatus.IGNORED);
        verify(violationRepository).save(violation);
    }

    @Test
    @DisplayName("Should reopen violation")
    void reopenViolation_Success() {
        // Given
        UUID violationId = UUID.randomUUID();
        ComplianceViolation violation = new ComplianceViolation();
        violation.setId(violationId);
        violation.setStatus(ComplianceViolation.ViolationStatus.RESOLVED);

        when(violationRepository.findById(violationId)).thenReturn(Optional.of(violation));
        when(violationRepository.save(any(ComplianceViolation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ComplianceViolation result = complianceEvaluationService.reopenViolation(violationId);

        // Then
        assertThat(result.getStatus()).isEqualTo(ComplianceViolation.ViolationStatus.OPEN);
        assertThat(result.getResolvedAt()).isNull();
        verify(violationRepository).save(violation);
    }

    @Test
    @DisplayName("Should throw exception when violation not found")
    void ignoreViolation_NotFound() {
        // Given
        UUID violationId = UUID.randomUUID();
        when(violationRepository.findById(violationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> complianceEvaluationService.ignoreViolation(violationId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Violation not found");
    }

    @Test
    @DisplayName("Should get violation stats")
    void getViolationStats_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        when(violationRepository.countOpenViolationsByUserId(userId)).thenReturn(5L);
        when(violationRepository.countOpenViolationsBySeverity(userId))
                .thenReturn(List.of(
                        new Object[]{TagPolicy.Severity.HIGH, 3L},
                        new Object[]{TagPolicy.Severity.MEDIUM, 2L}
                ));

        // When
        Map<String, Object> stats = complianceEvaluationService.getViolationStats(userId);

        // Then
        assertThat(stats.get("totalOpen")).isEqualTo(5L);

        @SuppressWarnings("unchecked")
        Map<String, Long> bySeverity = (Map<String, Long>) stats.get("bySeverity");
        assertThat(bySeverity.get("HIGH")).isEqualTo(3L);
        assertThat(bySeverity.get("MEDIUM")).isEqualTo(2L);
    }
}