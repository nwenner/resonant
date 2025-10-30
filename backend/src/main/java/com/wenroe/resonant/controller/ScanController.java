package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.scan.ScanJobResponse;
import com.wenroe.resonant.model.entity.ScanJob;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.service.ScanOrchestrationService;
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
 * REST controller for AWS resource scanning operations.
 */
@RestController
@RequestMapping("/api/scans")
@RequiredArgsConstructor
@Slf4j
public class ScanController {

    private final ScanOrchestrationService scanOrchestrationService;

    /**
     * Initiates a scan for an AWS account.
     */
    @PostMapping("/accounts/{accountId}")
    public ResponseEntity<ScanJobResponse> scanAccount(
            @AuthenticationPrincipal User user,
            @PathVariable UUID accountId) {

        log.info("User {} initiated scan for account {}", user.getId(), accountId);

        ScanJob scanJob = scanOrchestrationService.initiateScan(accountId, user.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ScanJobResponse.fromEntity(scanJob));
    }

    /**
     * Gets a specific scan job by ID.
     */
    @GetMapping("/{scanJobId}")
    public ResponseEntity<ScanJobResponse> getScanJob(
            @AuthenticationPrincipal User user,
            @PathVariable UUID scanJobId) {

        ScanJob scanJob = scanOrchestrationService.getScanJob(scanJobId);

        // Verify ownership
        if (!scanJob.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(ScanJobResponse.fromEntity(scanJob));
    }

    /**
     * Gets all scan jobs for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ScanJobResponse>> getAllScanJobs(
            @AuthenticationPrincipal User user) {

        log.info("Fetching all scan jobs for user {}", user.getId());

        List<ScanJob> scanJobs = scanOrchestrationService.getScanJobsByUserId(user.getId());
        List<ScanJobResponse> response = scanJobs.stream()
                .map(ScanJobResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets scan jobs for a specific AWS account.
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<ScanJobResponse>> getScanJobsForAccount(
            @AuthenticationPrincipal User user,
            @PathVariable UUID accountId) {

        log.info("Fetching scan jobs for account {}", accountId);

        List<ScanJob> scanJobs = scanOrchestrationService.getScanJobsByAccountId(accountId);

        // Verify ownership of first job (all should belong to same user via account)
        if (!scanJobs.isEmpty() && !scanJobs.get(0).getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ScanJobResponse> response = scanJobs.stream()
                .map(ScanJobResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets the most recent scan job for an account.
     */
    @GetMapping("/accounts/{accountId}/latest")
    public ResponseEntity<ScanJobResponse> getLatestScanForAccount(
            @AuthenticationPrincipal User user,
            @PathVariable UUID accountId) {

        return scanOrchestrationService.getLastScanForAccount(accountId)
                .map(scanJob -> {
                    // Verify ownership
                    if (!scanJob.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<ScanJobResponse>build();
                    }
                    return ResponseEntity.ok(ScanJobResponse.fromEntity(scanJob));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}