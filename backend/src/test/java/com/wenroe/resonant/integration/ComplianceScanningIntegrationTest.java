package com.wenroe.resonant.integration;

import com.wenroe.resonant.model.entity.*;
import com.wenroe.resonant.repository.*;
import com.wenroe.resonant.service.ComplianceEvaluationService;
import com.wenroe.resonant.service.ScanOrchestrationService;
import com.wenroe.resonant.service.TagPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Compliance Scanning Integration Tests")
class ComplianceScanningIntegrationTest {

    @Autowired
    private ComplianceEvaluationService complianceEvaluationService;

    @Autowired
    private TagPolicyService tagPolicyService;

    @Autowired
    private ScanJobRepository scanJobRepository;

    @Autowired
    private ComplianceViolationRepository violationRepository;

    @Autowired
    private AwsResourceRepository resourceRepository;

    @Autowired
    private AwsAccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagPolicyRepository policyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private AwsAccount testAccount;
    private TagPolicy testPolicy;

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
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create AWS account
        testAccount = new AwsAccount();
        testAccount.setUser(testUser);
        testAccount.setAccountId("123456789012");
        testAccount.setAccountAlias("test-account");
        testAccount.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
        testAccount.setExternalId("test-external-id");
        testAccount.setCredentialType(AwsAccount.CredentialType.ROLE);
        testAccount.setStatus(AwsAccount.Status.ACTIVE);
        testAccount = accountRepository.save(testAccount);

        // Create tag policy
        testPolicy = new TagPolicy();
        testPolicy.setUser(testUser);
        testPolicy.setName("Production Policy");
        testPolicy.setDescription("Requires Environment and Owner tags");

        Map<String, List<String>> requiredTags = new HashMap<>();
        requiredTags.put("Environment", List.of("prod", "dev"));
        requiredTags.put("Owner", null);  // Any value accepted
        testPolicy.setRequiredTags(requiredTags);

        testPolicy.setResourceTypes(List.of("s3:bucket"));
        testPolicy.setSeverity(TagPolicy.Severity.HIGH);
        testPolicy.setEnabled(true);
        testPolicy = tagPolicyService.createPolicy(testUser.getId(), testPolicy);
    }

    @Test
    @DisplayName("Should detect violation for untagged resource")
    void detectViolation_UntaggedResource() {
        // Create untagged resource
        AwsResource resource = createS3Resource("test-bucket", new HashMap<>());

        // Evaluate compliance
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(resource, List.of(testPolicy));

        // Verify violation was created
        assertThat(violations).hasSize(1);
        ComplianceViolation violation = violations.get(0);
        assertThat(violation.getStatus()).isEqualTo(ComplianceViolation.ViolationStatus.OPEN);
        assertThat(violation.getTagPolicy().getId()).isEqualTo(testPolicy.getId());

        @SuppressWarnings("unchecked")
        Map<String, Object> details = violation.getViolationDetails();
        assertThat(details).containsKey("missingTags");

        @SuppressWarnings("unchecked")
        List<String> missingTags = (List<String>) details.get("missingTags");
        assertThat(missingTags).containsExactlyInAnyOrder("Environment", "Owner");
    }

    @Test
    @DisplayName("Should detect violation for invalid tag value")
    void detectViolation_InvalidTagValue() {
        // Create resource with wrong Environment value
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "staging");  // Not in allowed values
        tags.put("Owner", "john@test.com");

        AwsResource resource = createS3Resource("test-bucket", tags);

        // Evaluate compliance
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(resource, List.of(testPolicy));

        // Verify violation was created
        assertThat(violations).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> details = violations.get(0).getViolationDetails();
        assertThat(details).containsKey("invalidTags");
    }

    @Test
    @DisplayName("Should not create violation for compliant resource")
    void noViolation_CompliantResource() {
        // Create compliant resource
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "prod");
        tags.put("Owner", "john@test.com");

        AwsResource resource = createS3Resource("test-bucket", tags);

        // Evaluate compliance
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(resource, List.of(testPolicy));

        // Verify no violations
        assertThat(violations).isEmpty();
        assertThat(violationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should auto-resolve violation when resource becomes compliant")
    void autoResolve_WhenCompliant() {
        // Create initial untagged resource and violation
        AwsResource resource = createS3Resource("test-bucket", new HashMap<>());
        complianceEvaluationService.evaluateResource(resource, List.of(testPolicy));

        assertThat(violationRepository.findAll()).hasSize(1);
        assertThat(violationRepository.findAll().get(0).getStatus())
                .isEqualTo(ComplianceViolation.ViolationStatus.OPEN);

        // Update resource to be compliant
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "prod");
        tags.put("Owner", "john@test.com");
        resource.setTags(tags);
        resource.updateLastSeen();  // Update timestamp
        resourceRepository.save(resource);

        // Re-evaluate
        complianceEvaluationService.evaluateResource(resource, List.of(testPolicy));

        // Verify violation was auto-resolved
        List<ComplianceViolation> violations = violationRepository.findAll();
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).getStatus())
                .isEqualTo(ComplianceViolation.ViolationStatus.RESOLVED);
        assertThat(violations.get(0).getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not auto-resolve ignored violations")
    void noAutoResolve_IgnoredViolations() {
        // Create violation and ignore it
        AwsResource resource = createS3Resource("test-bucket", new HashMap<>());
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(resource, List.of(testPolicy));

        UUID violationId = violations.get(0).getId();
        complianceEvaluationService.ignoreViolation(violationId);

        // Make resource compliant
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "prod");
        tags.put("Owner", "john@test.com");
        resource.setTags(tags);
        resource.updateLastSeen();  // Update timestamp
        resourceRepository.save(resource);

        // Re-evaluate
        complianceEvaluationService.evaluateResource(resource, List.of(testPolicy));

        // Verify violation is still IGNORED
        ComplianceViolation violation = violationRepository.findById(violationId).orElseThrow();
        assertThat(violation.getStatus()).isEqualTo(ComplianceViolation.ViolationStatus.IGNORED);
    }

    @Test
    @DisplayName("Should reopen resolved violation when non-compliant again")
    void reopen_WhenNonCompliantAgain() {
        // Create NON-compliant resource (violation created)
        AwsResource resource = createS3Resource("test-bucket", new HashMap<>());

        complianceEvaluationService.evaluateResource(resource, List.of(testPolicy));
        assertThat(violationRepository.findAll()).hasSize(1);
        UUID violationId = violationRepository.findAll().get(0).getId();

        // Make it compliant (auto-resolve)
        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "prod");
        tags.put("Owner", "john@test.com");
        resource.setTags(tags);
        resource.updateLastSeen();  // Update timestamp
        resourceRepository.save(resource);
        complianceEvaluationService.evaluateResource(resource, List.of(testPolicy));

        ComplianceViolation violation = violationRepository.findById(violationId).orElseThrow();
        assertThat(violation.getStatus()).isEqualTo(ComplianceViolation.ViolationStatus.RESOLVED);

        // Make it non-compliant again
        resource.setTags(new HashMap<>());
        resource.updateLastSeen();  // Update timestamp
        resourceRepository.save(resource);
        complianceEvaluationService.evaluateResource(resource, List.of(testPolicy));

        // Verify violation was reopened
        violation = violationRepository.findById(violationId).orElseThrow();
        assertThat(violation.getStatus()).isEqualTo(ComplianceViolation.ViolationStatus.OPEN);
        assertThat(violation.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("Should track violations by multiple policies")
    void multiplePolicy_Violations() {
        // Create second policy
        TagPolicy policy2 = new TagPolicy();
        policy2.setUser(testUser);
        policy2.setName("Cost Center Policy");
        policy2.setRequiredTags(Map.of("CostCenter", List.of("eng", "sales")));
        policy2.setResourceTypes(List.of("s3:bucket"));
        policy2.setSeverity(TagPolicy.Severity.MEDIUM);
        policy2.setEnabled(true);
        policy2 = tagPolicyService.createPolicy(testUser.getId(), policy2);

        // Create resource that violates both policies
        AwsResource resource = createS3Resource("test-bucket", new HashMap<>());

        // Evaluate against both policies
        List<ComplianceViolation> violations = complianceEvaluationService
                .evaluateResource(resource, List.of(testPolicy, policy2));

        // Verify 2 violations created
        assertThat(violations).hasSize(2);
        assertThat(violationRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should get violation statistics")
    void getViolationStats_Success() {
        // Create multiple violations with different severities
        createS3Resource("bucket1", new HashMap<>());
        createS3Resource("bucket2", new HashMap<>());

        TagPolicy mediumPolicy = new TagPolicy();
        mediumPolicy.setUser(testUser);
        mediumPolicy.setName("Medium Policy");

        Map<String, List<String>> mediumRequiredTags = new HashMap<>();
        mediumRequiredTags.put("Team", null);  // null = any value accepted
        mediumPolicy.setRequiredTags(mediumRequiredTags);

        mediumPolicy.setResourceTypes(List.of("s3:bucket"));
        mediumPolicy.setSeverity(TagPolicy.Severity.MEDIUM);
        mediumPolicy.setEnabled(true);
        mediumPolicy = tagPolicyService.createPolicy(testUser.getId(), mediumPolicy);

        List<AwsResource> resources = resourceRepository.findByAwsAccountId(testAccount.getId());
        for (AwsResource resource : resources) {
            complianceEvaluationService.evaluateResource(resource, List.of(testPolicy, mediumPolicy));
        }

        // Get stats
        Map<String, Object> stats = complianceEvaluationService.getViolationStats(testUser.getId());

        // Verify
        assertThat(stats.get("totalOpen")).isEqualTo(4L);  // 2 resources * 2 policies

        @SuppressWarnings("unchecked")
        Map<String, Long> bySeverity = (Map<String, Long>) stats.get("bySeverity");
        assertThat(bySeverity.get("HIGH")).isEqualTo(2L);
        assertThat(bySeverity.get("MEDIUM")).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should prevent duplicate violations for same resource-policy")
    void preventDuplicateViolations() {
        // Create resource and initial violation
        AwsResource resource = createS3Resource("test-bucket", new HashMap<>());
        complianceEvaluationService.evaluateResource(resource, List.of(testPolicy));

        assertThat(violationRepository.findAll()).hasSize(1);
        UUID firstViolationId = violationRepository.findAll().get(0).getId();

        // Re-evaluate (should update existing, not create new)
        complianceEvaluationService.evaluateResource(resource, List.of(testPolicy));

        assertThat(violationRepository.findAll()).hasSize(1);
        assertThat(violationRepository.findAll().get(0).getId()).isEqualTo(firstViolationId);
    }

    private AwsResource createS3Resource(String bucketName, Map<String, String> tags) {
        AwsResource resource = new AwsResource();
        resource.setAwsAccount(testAccount);
        resource.setResourceId(bucketName);
        resource.setResourceArn("arn:aws:s3:::" + bucketName);
        resource.setResourceType("s3:bucket");
        resource.setRegion("us-east-1");
        resource.setName(bucketName);
        resource.setTags(tags);
        resource.setMetadata(new HashMap<>());
        return resourceRepository.save(resource);
    }
}