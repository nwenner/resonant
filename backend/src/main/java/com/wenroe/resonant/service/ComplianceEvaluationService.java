package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.ComplianceViolation;
import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.enums.Severity;
import com.wenroe.resonant.model.enums.ViolationStatus;
import com.wenroe.resonant.repository.ComplianceViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for evaluating resources against tag policies and managing violations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceEvaluationService {

    private final ComplianceViolationRepository violationRepository;

    /**
     * Evaluates a resource against all applicable enabled policies.
     * Returns the list of violations found (or empty if compliant).
     */
    @Transactional
    public List<ComplianceViolation> evaluateResource(AwsResource resource, List<TagPolicy> enabledPolicies) {
        List<ComplianceViolation> violations = new ArrayList<>();

        for (TagPolicy policy : enabledPolicies) {
            // Check if policy applies to this resource type
            if (!policy.appliesToResourceType(resource.getResourceType())) {
                continue;
            }

            // Evaluate the resource against this policy
            Optional<ComplianceViolation> violation = evaluateResourceAgainstPolicy(resource, policy);

            if (violation.isPresent()) {
                violations.add(violation.get());
            } else {
                // Resource is compliant - check if we need to auto-resolve existing violation
                autoResolveViolationIfExists(resource, policy);
            }
        }

        return violations;
    }

    /**
     * Evaluates a single resource against a single policy.
     * Returns a violation if non-compliant, empty if compliant.
     */
    private Optional<ComplianceViolation> evaluateResourceAgainstPolicy(AwsResource resource, TagPolicy policy) {
        Map<String, Object> violationDetails = new HashMap<>();
        List<String> missingTags = new ArrayList<>();
        Map<String, Map<String, Object>> invalidTags = new HashMap<>();

        Map<String, String> resourceTags = resource.getTags() != null ? resource.getTags() : new HashMap<>();

        // Check each required tag
        for (Map.Entry<String, List<String>> requiredTag : policy.getRequiredTags().entrySet()) {
            String tagKey = requiredTag.getKey();
            List<String> allowedValues = requiredTag.getValue();

            if (!resourceTags.containsKey(tagKey)) {
                // Tag is missing
                missingTags.add(tagKey);
            } else if (allowedValues != null && !allowedValues.isEmpty()) {
                // Tag exists but need to check if value is allowed
                String currentValue = resourceTags.get(tagKey);
                if (!allowedValues.contains(currentValue)) {
                    Map<String, Object> invalidTagInfo = new HashMap<>();
                    invalidTagInfo.put("current", currentValue);
                    invalidTagInfo.put("allowed", allowedValues);
                    invalidTags.put(tagKey, invalidTagInfo);
                }
            }
            // If allowedValues is null or empty, any value is acceptable
        }

        // If no violations found, resource is compliant
        if (missingTags.isEmpty() && invalidTags.isEmpty()) {
            return Optional.empty();
        }

        // Build violation details
        if (!missingTags.isEmpty()) {
            violationDetails.put("missingTags", missingTags);
        }
        if (!invalidTags.isEmpty()) {
            violationDetails.put("invalidTags", invalidTags);
        }

        // Check if violation already exists
        Optional<ComplianceViolation> existing = violationRepository
                .findByAwsResourceIdAndTagPolicyId(resource.getId(), policy.getId());

        if (existing.isPresent()) {
            // Update existing violation
            ComplianceViolation violation = existing.get();
            violation.setViolationDetails(violationDetails);

            // If it was resolved, reopen it
            if (violation.getStatus() == ViolationStatus.RESOLVED) {
                violation.reopen();
                log.info("Reopened violation for resource {} - policy {}",
                        resource.getResourceArn(), policy.getName());
            }

            return Optional.of(violationRepository.save(violation));
        } else {
            // Create new violation
            ComplianceViolation violation = new ComplianceViolation();
            violation.setAwsResource(resource);
            violation.setTagPolicy(policy);
            violation.setViolationDetails(violationDetails);
            violation.setStatus(ViolationStatus.OPEN);

            log.info("Created new violation for resource {} - policy {}",
                    resource.getResourceArn(), policy.getName());

            return Optional.of(violationRepository.save(violation));
        }
    }

    /**
     * Auto-resolves a violation if it exists and the resource is now compliant.
     */
    private void autoResolveViolationIfExists(AwsResource resource, TagPolicy policy) {
        Optional<ComplianceViolation> existing = violationRepository
                .findByAwsResourceIdAndTagPolicyId(resource.getId(), policy.getId());

        if (existing.isPresent()) {
            ComplianceViolation violation = existing.get();

            // Only auto-resolve if it's currently OPEN (don't touch IGNORED violations)
            if (violation.getStatus() == ViolationStatus.OPEN) {
                violation.setStatus(ViolationStatus.RESOLVED);
                violation.setResolvedAt(LocalDateTime.now());
                violationRepository.save(violation);

                log.info("Auto-resolved violation for resource {} - policy {} (now compliant)",
                        resource.getResourceArn(), policy.getName());
            }
        }
    }

    /**
     * Marks a violation as ignored by the user.
     */
    @Transactional
    public ComplianceViolation ignoreViolation(UUID violationId) {
        ComplianceViolation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("Violation not found"));

        violation.ignore();
        ComplianceViolation saved = violationRepository.save(violation);

        log.info("Marked violation {} as IGNORED", violationId);
        return saved;
    }

    /**
     * Reopens an ignored or resolved violation.
     */
    @Transactional
    public ComplianceViolation reopenViolation(UUID violationId) {
        ComplianceViolation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("Violation not found"));

        violation.reopen();
        ComplianceViolation saved = violationRepository.save(violation);

        log.info("Reopened violation {}", violationId);
        return saved;
    }

    /**
     * Gets violation statistics for a user.
     */
    public Map<String, Object> getViolationStats(UUID userId) {
        Map<String, Object> stats = new HashMap<>();

        long totalOpen = violationRepository.countOpenViolationsByUserId(userId);
        stats.put("totalOpen", totalOpen);

        // Count by severity
        List<Object[]> bySeverity = violationRepository.countOpenViolationsBySeverity(userId);
        Map<String, Long> severityCounts = new HashMap<>();
        for (Object[] row : bySeverity) {
            Severity severity = (Severity) row[0];
            Long count = (Long) row[1];
            severityCounts.put(severity.name(), count);
        }
        stats.put("bySeverity", severityCounts);

        return stats;
    }
}