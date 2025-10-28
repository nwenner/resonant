package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.policy.CreateTagPolicyRequest;
import com.wenroe.resonant.dto.policy.TagPolicyResponse;
import com.wenroe.resonant.dto.policy.UpdateTagPolicyRequest;
import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.service.TagPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing tag compliance policies.
 */
@RestController
@RequestMapping("/api/tag-policies")
@RequiredArgsConstructor
@Slf4j
public class TagPolicyController {

    private final TagPolicyService tagPolicyService;

    /**
     * Create a new tag policy.
     */
    @PostMapping
    public ResponseEntity<TagPolicyResponse> createPolicy(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateTagPolicyRequest request) {

        log.info("Creating tag policy '{}' for user {}", request.getName(), user.getId());

        TagPolicy policy = new TagPolicy();
        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy.setRequiredTags(request.getRequiredTags());
        policy.setResourceTypes(request.getResourceTypes());
        policy.setSeverity(request.getSeverity());
        policy.setEnabled(request.getEnabled());

        TagPolicy created = tagPolicyService.createPolicy(user.getId(), policy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TagPolicyResponse.fromEntity(created));
    }

    /**
     * Get all tag policies for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<TagPolicyResponse>> getAllPolicies(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Boolean enabled) {

        log.info("Fetching tag policies for user {} (enabled={})", user.getId(), enabled);

        List<TagPolicy> policies;
        if (enabled != null && enabled) {
            policies = tagPolicyService.getEnabledPoliciesByUserId(user.getId());
        } else {
            policies = tagPolicyService.getPoliciesByUserId(user.getId());
        }

        List<TagPolicyResponse> response = policies.stream()
                .map(TagPolicyResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific tag policy by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TagPolicyResponse> getPolicy(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        log.info("Fetching tag policy {} for user {}", id, user.getId());

        TagPolicy policy = tagPolicyService.getPolicyById(id);

        // Verify ownership
        if (!policy.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(TagPolicyResponse.fromEntity(policy));
    }

    /**
     * Update an existing tag policy.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TagPolicyResponse> updatePolicy(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTagPolicyRequest request) {

        log.info("Updating tag policy {} for user {}", id, user.getId());

        TagPolicy updates = new TagPolicy();
        updates.setName(request.getName());
        updates.setDescription(request.getDescription());
        updates.setRequiredTags(request.getRequiredTags());
        updates.setResourceTypes(request.getResourceTypes());
        updates.setSeverity(request.getSeverity());
        updates.setEnabled(request.getEnabled());

        TagPolicy updated = tagPolicyService.updatePolicy(id, user.getId(), updates);
        return ResponseEntity.ok(TagPolicyResponse.fromEntity(updated));
    }

    /**
     * Enable a tag policy.
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<TagPolicyResponse> enablePolicy(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        log.info("Enabling tag policy {} for user {}", id, user.getId());

        TagPolicy enabled = tagPolicyService.enablePolicy(id, user.getId());
        return ResponseEntity.ok(TagPolicyResponse.fromEntity(enabled));
    }

    /**
     * Disable a tag policy.
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<TagPolicyResponse> disablePolicy(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        log.info("Disabling tag policy {} for user {}", id, user.getId());

        TagPolicy disabled = tagPolicyService.disablePolicy(id, user.getId());
        return ResponseEntity.ok(TagPolicyResponse.fromEntity(disabled));
    }

    /**
     * Delete a tag policy.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        log.info("Deleting tag policy {} for user {}", id, user.getId());

        tagPolicyService.deletePolicy(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get policy statistics for the user.
     */
    @GetMapping("/stats")
    public ResponseEntity<PolicyStats> getPolicyStats(
            @AuthenticationPrincipal User user) {

        long total = tagPolicyService.countPoliciesByUserId(user.getId());
        long enabled = tagPolicyService.countEnabledPoliciesByUserId(user.getId());

        PolicyStats stats = new PolicyStats();
        stats.setTotal(total);
        stats.setEnabled(enabled);
        stats.setDisabled(total - enabled);

        return ResponseEntity.ok(stats);
    }

    /**
     * Stats DTO for policy counts.
     */
    @lombok.Data
    public static class PolicyStats {
        private long total;
        private long enabled;
        private long disabled;
    }
}