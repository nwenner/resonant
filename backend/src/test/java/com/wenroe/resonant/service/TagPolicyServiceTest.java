package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.Severity;
import com.wenroe.resonant.repository.TagPolicyRepository;
import com.wenroe.resonant.repository.UserRepository;
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
@DisplayName("TagPolicyService Tests")
class TagPolicyServiceTest {

    @Mock
    private TagPolicyRepository tagPolicyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TagPolicyService tagPolicyService;

    private User testUser;
    private TagPolicy testPolicy;
    private UUID userId;
    private UUID policyId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        policyId = UUID.randomUUID();

        // Setup test user
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        // Setup test policy
        testPolicy = new TagPolicy();
        testPolicy.setId(policyId);
        testPolicy.setUser(testUser);
        testPolicy.setName("Test Policy");
        testPolicy.setDescription("Test Description");

        // Map.of() doesn't allow null values, so use HashMap
        Map<String, List<String>> requiredTags = new HashMap<>();
        requiredTags.put("Environment", List.of("prod", "dev"));
        requiredTags.put("Owner", null);  // null means any value is acceptable
        testPolicy.setRequiredTags(requiredTags);

        testPolicy.setResourceTypes(List.of("ec2:instance", "s3:bucket"));
        testPolicy.setSeverity(Severity.HIGH);
        testPolicy.setEnabled(true);
    }

    @Test
    @DisplayName("Should create policy successfully")
    void createPolicy_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(tagPolicyRepository.save(any(TagPolicy.class))).thenReturn(testPolicy);

        // When
        TagPolicy result = tagPolicyService.createPolicy(userId, testPolicy);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Policy");
        assertThat(result.getUser()).isEqualTo(testUser);
        verify(userRepository).findById(userId);
        verify(tagPolicyRepository).save(testPolicy);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void createPolicy_UserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tagPolicyService.createPolicy(userId, testPolicy))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(tagPolicyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when required tags empty")
    void createPolicy_EmptyRequiredTags() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        testPolicy.setRequiredTags(new HashMap<>());

        // When & Then
        assertThatThrownBy(() -> tagPolicyService.createPolicy(userId, testPolicy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one required tag must be specified");
    }

    @Test
    @DisplayName("Should throw exception when resource types empty")
    void createPolicy_EmptyResourceTypes() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        testPolicy.setResourceTypes(new ArrayList<>());

        // When & Then
        assertThatThrownBy(() -> tagPolicyService.createPolicy(userId, testPolicy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one resource type must be specified");
    }

    @Test
    @DisplayName("Should update policy successfully")
    void updatePolicy_Success() {
        // Given
        TagPolicy updates = new TagPolicy();
        updates.setName("Updated Name");
        updates.setDescription("Updated Description");
        updates.setSeverity(Severity.CRITICAL);

        when(tagPolicyRepository.findById(policyId)).thenReturn(Optional.of(testPolicy));
        when(tagPolicyRepository.save(any(TagPolicy.class))).thenReturn(testPolicy);

        // When
        TagPolicy result = tagPolicyService.updatePolicy(policyId, userId, updates);

        // Then
        assertThat(result).isNotNull();
        verify(tagPolicyRepository).findById(policyId);
        verify(tagPolicyRepository).save(testPolicy);
    }

    @Test
    @DisplayName("Should throw exception when updating non-owned policy")
    void updatePolicy_NotOwner() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        TagPolicy updates = new TagPolicy();
        updates.setName("Updated Name");

        when(tagPolicyRepository.findById(policyId)).thenReturn(Optional.of(testPolicy));

        // When & Then
        assertThatThrownBy(() -> tagPolicyService.updatePolicy(policyId, otherUserId, updates))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized to update this tag policy");

        verify(tagPolicyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get policies by user ID")
    void getPoliciesByUserId_Success() {
        // Given
        List<TagPolicy> policies = List.of(testPolicy);
        when(tagPolicyRepository.findByUserId(userId)).thenReturn(policies);

        // When
        List<TagPolicy> result = tagPolicyService.getPoliciesByUserId(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(testPolicy);
        verify(tagPolicyRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should get enabled policies by user ID")
    void getEnabledPoliciesByUserId_Success() {
        // Given
        List<TagPolicy> policies = List.of(testPolicy);
        when(tagPolicyRepository.findEnabledPoliciesByUserId(userId)).thenReturn(policies);

        // When
        List<TagPolicy> result = tagPolicyService.getEnabledPoliciesByUserId(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEnabled()).isTrue();
        verify(tagPolicyRepository).findEnabledPoliciesByUserId(userId);
    }

    @Test
    @DisplayName("Should get policy by ID")
    void getPolicyById_Success() {
        // Given
        when(tagPolicyRepository.findById(policyId)).thenReturn(Optional.of(testPolicy));

        // When
        TagPolicy result = tagPolicyService.getPolicyById(policyId);

        // Then
        assertThat(result).isEqualTo(testPolicy);
        verify(tagPolicyRepository).findById(policyId);
    }

    @Test
    @DisplayName("Should throw exception when policy not found")
    void getPolicyById_NotFound() {
        // Given
        when(tagPolicyRepository.findById(policyId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tagPolicyService.getPolicyById(policyId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tag policy not found");
    }

    @Test
    @DisplayName("Should enable policy")
    void enablePolicy_Success() {
        // Given
        testPolicy.setEnabled(false);
        when(tagPolicyRepository.findById(policyId)).thenReturn(Optional.of(testPolicy));
        when(tagPolicyRepository.save(any(TagPolicy.class))).thenReturn(testPolicy);

        // When
        TagPolicy result = tagPolicyService.enablePolicy(policyId, userId);

        // Then
        assertThat(result.getEnabled()).isTrue();
        verify(tagPolicyRepository).save(testPolicy);
    }

    @Test
    @DisplayName("Should disable policy")
    void disablePolicy_Success() {
        // Given
        when(tagPolicyRepository.findById(policyId)).thenReturn(Optional.of(testPolicy));
        when(tagPolicyRepository.save(any(TagPolicy.class))).thenReturn(testPolicy);

        // When
        TagPolicy result = tagPolicyService.disablePolicy(policyId, userId);

        // Then
        assertThat(result.getEnabled()).isFalse();
        verify(tagPolicyRepository).save(testPolicy);
    }

    @Test
    @DisplayName("Should throw exception when enabling non-owned policy")
    void enablePolicy_NotOwner() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(tagPolicyRepository.findById(policyId)).thenReturn(Optional.of(testPolicy));

        // When & Then
        assertThatThrownBy(() -> tagPolicyService.enablePolicy(policyId, otherUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized to modify this tag policy");
    }

    @Test
    @DisplayName("Should delete policy")
    void deletePolicy_Success() {
        // Given
        when(tagPolicyRepository.findById(policyId)).thenReturn(Optional.of(testPolicy));
        doNothing().when(tagPolicyRepository).delete(testPolicy);

        // When
        tagPolicyService.deletePolicy(policyId, userId);

        // Then
        verify(tagPolicyRepository).delete(testPolicy);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-owned policy")
    void deletePolicy_NotOwner() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(tagPolicyRepository.findById(policyId)).thenReturn(Optional.of(testPolicy));

        // When & Then
        assertThatThrownBy(() -> tagPolicyService.deletePolicy(policyId, otherUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized to delete this tag policy");

        verify(tagPolicyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should count policies by user ID")
    void countPoliciesByUserId_Success() {
        // Given
        List<TagPolicy> policies = List.of(testPolicy, testPolicy);
        when(tagPolicyRepository.findByUserId(userId)).thenReturn(policies);

        // When
        long count = tagPolicyService.countPoliciesByUserId(userId);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count enabled policies by user ID")
    void countEnabledPoliciesByUserId_Success() {
        // Given
        List<TagPolicy> policies = List.of(testPolicy);
        when(tagPolicyRepository.findEnabledPoliciesByUserId(userId)).thenReturn(policies);

        // When
        long count = tagPolicyService.countEnabledPoliciesByUserId(userId);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should validate required tags cannot be null on create")
    void createPolicy_RequiredTagsNull() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        testPolicy.setRequiredTags(null);

        // When & Then
        assertThatThrownBy(() -> tagPolicyService.createPolicy(userId, testPolicy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one required tag must be specified");
    }

    @Test
    @DisplayName("Should validate resource types cannot be null on create")
    void createPolicy_ResourceTypesNull() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        testPolicy.setResourceTypes(null);

        // When & Then
        assertThatThrownBy(() -> tagPolicyService.createPolicy(userId, testPolicy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one resource type must be specified");
    }
}