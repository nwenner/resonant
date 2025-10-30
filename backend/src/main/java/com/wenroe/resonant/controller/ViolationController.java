package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.violation.ViolationResponse;
import com.wenroe.resonant.model.entity.ComplianceViolation;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.repository.ComplianceViolationRepository;
import com.wenroe.resonant.service.ComplianceEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing compliance violations.
 */
@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
@Slf4j
public class ViolationController {

    private final ComplianceViolationRepository violationRepository;
    private final ComplianceEvaluationService complianceEvaluationService;

    /**
     * Gets all violations for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ViolationResponse>> getAllViolations(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status) {

        log.info("Fetching violations for user {} with status filter: {}", user.getId(), status);

        List<ComplianceViolation> violations;

        if ("OPEN".equalsIgnoreCase(status)) {
            violations = violationRepository.findOpenViolationsByUserId(user.getId());
        } else {
            violations = violationRepository.findByUserId(user.getId());
        }

        List<ViolationResponse> response = violations.stream()
                .map(ViolationResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets a specific violation by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ViolationResponse> getViolation(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        ComplianceViolation violation = violationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Violation not found"));

        // Verify ownership through resource -> account -> user
        if (!violation.getAwsResource().getAwsAccount().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(ViolationResponse.fromEntity(violation));
    }

    /**
     * Gets violations for a specific AWS account.
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<ViolationResponse>> getViolationsByAccount(
            @AuthenticationPrincipal User user,
            @PathVariable UUID accountId) {

        log.info("Fetching violations for account {}", accountId);

        List<ComplianceViolation> violations = violationRepository.findByAwsAccountId(accountId);

        // Verify ownership
        if (!violations.isEmpty()) {
            UUID ownerId = violations.get(0).getAwsResource().getAwsAccount().getUser().getId();
            if (!ownerId.equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        List<ViolationResponse> response = violations.stream()
                .map(ViolationResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets violations for a specific resource.
     */
    @GetMapping("/resources/{resourceId}")
    public ResponseEntity<List<ViolationResponse>> getViolationsByResource(
            @AuthenticationPrincipal User user,
            @PathVariable UUID resourceId) {

        log.info("Fetching violations for resource {}", resourceId);

        List<ComplianceViolation> violations = violationRepository.findByAwsResourceId(resourceId);

        // Verify ownership
        if (!violations.isEmpty()) {
            UUID ownerId = violations.get(0).getAwsResource().getAwsAccount().getUser().getId();
            if (!ownerId.equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        List<ViolationResponse> response = violations.stream()
                .map(ViolationResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets violations for a specific policy.
     */
    @GetMapping("/policies/{policyId}")
    public ResponseEntity<List<ViolationResponse>> getViolationsByPolicy(
            @AuthenticationPrincipal User user,
            @PathVariable UUID policyId) {

        log.info("Fetching violations for policy {}", policyId);

        List<ComplianceViolation> violations = violationRepository.findByTagPolicyId(policyId);

        // Verify ownership
        if (!violations.isEmpty()) {
            UUID ownerId = violations.get(0).getTagPolicy().getUser().getId();
            if (!ownerId.equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        List<ViolationResponse> response = violations.stream()
                .map(ViolationResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Marks a violation as ignored.
     */
    @PostMapping("/{id}/ignore")
    public ResponseEntity<ViolationResponse> ignoreViolation(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        log.info("User {} ignoring violation {}", user.getId(), id);

        ComplianceViolation violation = violationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Violation not found"));

        // Verify ownership
        if (!violation.getAwsResource().getAwsAccount().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ComplianceViolation updated = complianceEvaluationService.ignoreViolation(id);
        return ResponseEntity.ok(ViolationResponse.fromEntity(updated));
    }

    /**
     * Reopens a violation.
     */
    @PostMapping("/{id}/reopen")
    public ResponseEntity<ViolationResponse> reopenViolation(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        log.info("User {} reopening violation {}", user.getId(), id);

        ComplianceViolation violation = violationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Violation not found"));

        // Verify ownership
        if (!violation.getAwsResource().getAwsAccount().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ComplianceViolation updated = complianceEvaluationService.reopenViolation(id);
        return ResponseEntity.ok(ViolationResponse.fromEntity(updated));
    }

    /**
     * Gets violation statistics for the user.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getViolationStats(
            @AuthenticationPrincipal User user) {

        Map<String, Object> stats = complianceEvaluationService.getViolationStats(user.getId());
        return ResponseEntity.ok(stats);
    }
}