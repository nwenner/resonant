package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.repository.TagPolicyRepository;
import com.wenroe.resonant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing tag compliance policies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagPolicyService {

    private final TagPolicyRepository tagPolicyRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new tag policy for a user.
     */
    @Transactional
    public TagPolicy createPolicy(UUID userId, TagPolicy policy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        policy.setUser(user);

        // Validate required tags is not empty
        if (policy.getRequiredTags() == null || policy.getRequiredTags().isEmpty()) {
            throw new IllegalArgumentException("At least one required tag must be specified");
        }

        // Validate resource types is not empty
        if (policy.getResourceTypes() == null || policy.getResourceTypes().isEmpty()) {
            throw new IllegalArgumentException("At least one resource type must be specified");
        }

        TagPolicy saved = tagPolicyRepository.save(policy);
        log.info("Created tag policy '{}' for user {}", saved.getName(), userId);
        return saved;
    }

    /**
     * Updates an existing tag policy.
     */
    @Transactional
    public TagPolicy updatePolicy(UUID policyId, UUID userId, TagPolicy updatedPolicy) {
        TagPolicy existing = tagPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Tag policy not found"));

        // Verify ownership
        if (!existing.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this tag policy");
        }

        // Update fields
        if (updatedPolicy.getName() != null) {
            existing.setName(updatedPolicy.getName());
        }
        if (updatedPolicy.getDescription() != null) {
            existing.setDescription(updatedPolicy.getDescription());
        }
        if (updatedPolicy.getRequiredTags() != null) {
            if (updatedPolicy.getRequiredTags().isEmpty()) {
                throw new IllegalArgumentException("At least one required tag must be specified");
            }
            existing.setRequiredTags(updatedPolicy.getRequiredTags());
        }
        if (updatedPolicy.getResourceTypes() != null) {
            if (updatedPolicy.getResourceTypes().isEmpty()) {
                throw new IllegalArgumentException("At least one resource type must be specified");
            }
            existing.setResourceTypes(updatedPolicy.getResourceTypes());
        }
        if (updatedPolicy.getSeverity() != null) {
            existing.setSeverity(updatedPolicy.getSeverity());
        }
        if (updatedPolicy.getEnabled() != null) {
            existing.setEnabled(updatedPolicy.getEnabled());
        }

        TagPolicy saved = tagPolicyRepository.save(existing);
        log.info("Updated tag policy '{}' ({})", saved.getName(), policyId);
        return saved;
    }

    /**
     * Gets all tag policies for a user.
     */
    public List<TagPolicy> getPoliciesByUserId(UUID userId) {
        return tagPolicyRepository.findByUserId(userId);
    }

    /**
     * Gets only enabled tag policies for a user.
     */
    public List<TagPolicy> getEnabledPoliciesByUserId(UUID userId) {
        return tagPolicyRepository.findEnabledPoliciesByUserId(userId);
    }

    /**
     * Gets a specific tag policy by ID.
     */
    public TagPolicy getPolicyById(UUID policyId) {
        return tagPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Tag policy not found"));
    }

    /**
     * Enables a tag policy.
     */
    @Transactional
    public TagPolicy enablePolicy(UUID policyId, UUID userId) {
        TagPolicy policy = getPolicyById(policyId);

        // Verify ownership
        if (!policy.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this tag policy");
        }

        policy.setEnabled(true);
        TagPolicy saved = tagPolicyRepository.save(policy);
        log.info("Enabled tag policy '{}' ({})", saved.getName(), policyId);
        return saved;
    }

    /**
     * Disables a tag policy.
     */
    @Transactional
    public TagPolicy disablePolicy(UUID policyId, UUID userId) {
        TagPolicy policy = getPolicyById(policyId);

        // Verify ownership
        if (!policy.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this tag policy");
        }

        policy.setEnabled(false);
        TagPolicy saved = tagPolicyRepository.save(policy);
        log.info("Disabled tag policy '{}' ({})", saved.getName(), policyId);
        return saved;
    }

    /**
     * Deletes a tag policy.
     * Note: This will cascade delete all violations associated with this policy.
     */
    @Transactional
    public void deletePolicy(UUID policyId, UUID userId) {
        TagPolicy policy = getPolicyById(policyId);

        // Verify ownership
        if (!policy.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this tag policy");
        }

        tagPolicyRepository.delete(policy);
        log.info("Deleted tag policy '{}' ({})", policy.getName(), policyId);
    }

    /**
     * Counts total policies for a user.
     */
    public long countPoliciesByUserId(UUID userId) {
        return tagPolicyRepository.findByUserId(userId).size();
    }

    /**
     * Counts enabled policies for a user.
     */
    public long countEnabledPoliciesByUserId(UUID userId) {
        return tagPolicyRepository.findEnabledPoliciesByUserId(userId).size();
    }
}