package com.wenroe.resonant.integration;

import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.repository.TagPolicyRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.TagPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TagPolicy Integration Tests")
class TagPolicyIntegrationTest {

    @Autowired
    private TagPolicyService tagPolicyService;

    @Autowired
    private TagPolicyRepository tagPolicyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up
        tagPolicyRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should create and retrieve policy")
    void createAndRetrievePolicy() {
        // Create policy
        TagPolicy policy = new TagPolicy();
        policy.setName("Test Policy");
        policy.setDescription("Test Description");

        Map<String, List<String>> requiredTags = new HashMap<>();
        requiredTags.put("Environment", List.of("prod"));
        policy.setRequiredTags(requiredTags);

        policy.setResourceTypes(List.of("ec2:instance"));
        policy.setSeverity(TagPolicy.Severity.HIGH);
        policy.setEnabled(true);

        TagPolicy created = tagPolicyService.createPolicy(testUser.getId(), policy);

        // Verify
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Policy");

        // Retrieve
        TagPolicy retrieved = tagPolicyService.getPolicyById(created.getId());
        assertThat(retrieved.getName()).isEqualTo("Test Policy");
    }

    @Test
    @DisplayName("Should update policy")
    void updatePolicy() {
        // Create
        TagPolicy policy = createTestPolicy("Original Name", true);

        // Update
        TagPolicy updates = new TagPolicy();
        updates.setDescription("Updated Description");

        TagPolicy updated = tagPolicyService.updatePolicy(policy.getId(), testUser.getId(), updates);

        // Verify
        assertThat(updated.getDescription()).isEqualTo("Updated Description");
        assertThat(updated.getName()).isEqualTo("Original Name");
    }

    @Test
    @DisplayName("Should enable and disable policy")
    void enableDisablePolicy() {
        // Create enabled policy
        TagPolicy policy = createTestPolicy("Toggle Test", true);
        assertThat(policy.getEnabled()).isTrue();

        // Disable
        TagPolicy disabled = tagPolicyService.disablePolicy(policy.getId(), testUser.getId());
        assertThat(disabled.getEnabled()).isFalse();

        // Enable
        TagPolicy enabled = tagPolicyService.enablePolicy(policy.getId(), testUser.getId());
        assertThat(enabled.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should delete policy")
    void deletePolicy() {
        // Create
        TagPolicy policy = createTestPolicy("To Delete", true);
        assertThat(tagPolicyRepository.findAll()).hasSize(1);

        // Delete
        tagPolicyService.deletePolicy(policy.getId(), testUser.getId());

        // Verify
        assertThat(tagPolicyRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should get policies by user")
    void getPoliciesByUser() {
        // Create multiple policies
        createTestPolicy("Policy 1", true);
        createTestPolicy("Policy 2", false);
        createTestPolicy("Policy 3", true);

        // Get all
        List<TagPolicy> all = tagPolicyService.getPoliciesByUserId(testUser.getId());
        assertThat(all).hasSize(3);

        // Get enabled only
        List<TagPolicy> enabled = tagPolicyService.getEnabledPoliciesByUserId(testUser.getId());
        assertThat(enabled).hasSize(2);
    }

    @Test
    @DisplayName("Should count policies correctly")
    void countPolicies() {
        // Create policies
        createTestPolicy("Policy 1", true);
        createTestPolicy("Policy 2", true);
        createTestPolicy("Policy 3", false);

        // Count
        long total = tagPolicyService.countPoliciesByUserId(testUser.getId());
        long enabled = tagPolicyService.countEnabledPoliciesByUserId(testUser.getId());

        assertThat(total).isEqualTo(3);
        assertThat(enabled).isEqualTo(2);
    }

    private TagPolicy createTestPolicy(String name, boolean enabled) {
        TagPolicy policy = new TagPolicy();
        policy.setName(name);
        policy.setDescription("Test policy");
        policy.setRequiredTags(Map.of("Environment", List.of("prod")));
        policy.setResourceTypes(List.of("ec2:instance"));
        policy.setSeverity(TagPolicy.Severity.MEDIUM);
        policy.setEnabled(enabled);
        return tagPolicyService.createPolicy(testUser.getId(), policy);
    }
}