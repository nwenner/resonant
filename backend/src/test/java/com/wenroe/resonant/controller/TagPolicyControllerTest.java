package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.policy.CreateTagPolicyRequest;
import com.wenroe.resonant.dto.policy.TagPolicyResponse;
import com.wenroe.resonant.dto.policy.UpdateTagPolicyRequest;
import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.Severity;
import com.wenroe.resonant.service.TagPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagPolicyController Tests")
class TagPolicyControllerTest {

    @Mock
    private TagPolicyService tagPolicyService;

    @InjectMocks
    private TagPolicyController tagPolicyController;

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

        Map<String, List<String>> requiredTags = new HashMap<>();
        requiredTags.put("Environment", List.of("prod", "dev"));
        requiredTags.put("Owner", null);
        testPolicy.setRequiredTags(requiredTags);

        testPolicy.setResourceTypes(List.of("ec2:instance", "s3:bucket"));
        testPolicy.setSeverity(Severity.HIGH);
        testPolicy.setEnabled(true);
    }

    @Test
    @DisplayName("Should create policy")
    void createPolicy_Success() {
        // Given
        CreateTagPolicyRequest request = new CreateTagPolicyRequest();
        request.setName("Test Policy");
        request.setDescription("Test Description");
        request.setRequiredTags(Map.of("Environment", List.of("prod")));
        request.setResourceTypes(List.of("ec2:instance"));
        request.setSeverity(Severity.HIGH);
        request.setEnabled(true);

        when(tagPolicyService.createPolicy(any(UUID.class), any(TagPolicy.class)))
                .thenReturn(testPolicy);

        // When
        ResponseEntity<?> response = tagPolicyController.createPolicy(testUser, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(TagPolicyResponse.class);

        TagPolicyResponse policyResponse = (TagPolicyResponse) response.getBody();
        assertThat(policyResponse.getName()).isEqualTo("Test Policy");

        verify(tagPolicyService).createPolicy(any(UUID.class), any(TagPolicy.class));
    }

    @Test
    @DisplayName("Should get all policies")
    void getAllPolicies_Success() {
        // Given
        when(tagPolicyService.getPoliciesByUserId(any(UUID.class)))
                .thenReturn(List.of(testPolicy));

        // When
        ResponseEntity<List<TagPolicyResponse>> response =
                tagPolicyController.getAllPolicies(testUser, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getName()).isEqualTo("Test Policy");

        verify(tagPolicyService).getPoliciesByUserId(any(UUID.class));
    }

    @Test
    @DisplayName("Should get enabled policies only")
    void getEnabledPolicies_Success() {
        // Given
        when(tagPolicyService.getEnabledPoliciesByUserId(any(UUID.class)))
                .thenReturn(List.of(testPolicy));

        // When
        ResponseEntity<List<TagPolicyResponse>> response =
                tagPolicyController.getAllPolicies(testUser, true);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        verify(tagPolicyService).getEnabledPoliciesByUserId(any(UUID.class));
    }

    @Test
    @DisplayName("Should get policy by ID")
    void getPolicy_Success() {
        // Given
        when(tagPolicyService.getPolicyById(policyId)).thenReturn(testPolicy);

        // When
        ResponseEntity<TagPolicyResponse> response =
                tagPolicyController.getPolicy(testUser, policyId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Test Policy");

        verify(tagPolicyService).getPolicyById(policyId);
    }

    @Test
    @DisplayName("Should return 403 for non-owner")
    void getPolicy_Forbidden() {
        // Given
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        when(tagPolicyService.getPolicyById(policyId)).thenReturn(testPolicy);

        // When
        ResponseEntity<TagPolicyResponse> response =
                tagPolicyController.getPolicy(otherUser, policyId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should update policy")
    void updatePolicy_Success() {
        // Given
        UpdateTagPolicyRequest request = new UpdateTagPolicyRequest();
        request.setDescription("Updated Description");

        when(tagPolicyService.updatePolicy(eq(policyId), any(UUID.class), any(TagPolicy.class)))
                .thenReturn(testPolicy);

        // When
        ResponseEntity<TagPolicyResponse> response =
                tagPolicyController.updatePolicy(testUser, policyId, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tagPolicyService).updatePolicy(eq(policyId), any(UUID.class), any(TagPolicy.class));
    }

    @Test
    @DisplayName("Should enable policy")
    void enablePolicy_Success() {
        // Given
        when(tagPolicyService.enablePolicy(eq(policyId), any(UUID.class)))
                .thenReturn(testPolicy);

        // When
        ResponseEntity<TagPolicyResponse> response =
                tagPolicyController.enablePolicy(testUser, policyId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEnabled()).isTrue();

        verify(tagPolicyService).enablePolicy(eq(policyId), any(UUID.class));
    }

    @Test
    @DisplayName("Should disable policy")
    void disablePolicy_Success() {
        // Given
        testPolicy.setEnabled(false);
        when(tagPolicyService.disablePolicy(eq(policyId), any(UUID.class)))
                .thenReturn(testPolicy);

        // When
        ResponseEntity<TagPolicyResponse> response =
                tagPolicyController.disablePolicy(testUser, policyId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEnabled()).isFalse();

        verify(tagPolicyService).disablePolicy(eq(policyId), any(UUID.class));
    }

    @Test
    @DisplayName("Should delete policy")
    void deletePolicy_Success() {
        // Given
        doNothing().when(tagPolicyService).deletePolicy(eq(policyId), any(UUID.class));

        // When
        ResponseEntity<Void> response =
                tagPolicyController.deletePolicy(testUser, policyId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(tagPolicyService).deletePolicy(eq(policyId), any(UUID.class));
    }

    @Test
    @DisplayName("Should get policy statistics")
    void getPolicyStats_Success() {
        // Given
        when(tagPolicyService.countPoliciesByUserId(any(UUID.class))).thenReturn(5L);
        when(tagPolicyService.countEnabledPoliciesByUserId(any(UUID.class))).thenReturn(3L);

        // When
        ResponseEntity<TagPolicyController.PolicyStats> response =
                tagPolicyController.getPolicyStats(testUser);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotal()).isEqualTo(5);
        assertThat(response.getBody().getEnabled()).isEqualTo(3);
        assertThat(response.getBody().getDisabled()).isEqualTo(2);

        verify(tagPolicyService).countPoliciesByUserId(any(UUID.class));
        verify(tagPolicyService).countEnabledPoliciesByUserId(any(UUID.class));
    }
}