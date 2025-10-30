package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.aws.AwsResourceResponse;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.repository.AwsResourceRepository;
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
 * REST controller for AWS resources.
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Slf4j
public class AwsResourceController {

    private final AwsResourceRepository resourceRepository;

    /**
     * Gets all resources for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<AwsResourceResponse>> getAllResources(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String type) {

        log.info("Fetching resources for user {} with type filter: {}", user.getId(), type);

        List<AwsResource> resources = resourceRepository.findByUserId(user.getId());

        // Filter by type if provided
        if (type != null && !type.isEmpty()) {
            resources = resources.stream()
                    .filter(r -> r.getResourceType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        List<AwsResourceResponse> response = resources.stream()
                .map(AwsResourceResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets a specific resource by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AwsResourceResponse> getResource(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        AwsResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        // Verify ownership
        if (!resource.getAwsAccount().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(AwsResourceResponse.fromEntity(resource));
    }

    /**
     * Gets resources for a specific AWS account.
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<AwsResourceResponse>> getResourcesByAccount(
            @AuthenticationPrincipal User user,
            @PathVariable UUID accountId) {

        log.info("Fetching resources for account {}", accountId);

        List<AwsResource> resources = resourceRepository.findByAwsAccountId(accountId);

        // Verify ownership
        if (!resources.isEmpty()) {
            UUID ownerId = resources.get(0).getAwsAccount().getUser().getId();
            if (!ownerId.equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        List<AwsResourceResponse> response = resources.stream()
                .map(AwsResourceResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets resource statistics for the user.
     */
    @GetMapping("/stats")
    public ResponseEntity<ResourceStats> getResourceStats(
            @AuthenticationPrincipal User user) {

        long totalResources = resourceRepository.countByUserId(user.getId());
        List<Object[]> byType = resourceRepository.countResourcesByType(user.getId());

        ResourceStats stats = new ResourceStats();
        stats.setTotal(totalResources);

        Map<String, Long> typeCounts = byType.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
        stats.setByType(typeCounts);

        return ResponseEntity.ok(stats);
    }

    @lombok.Data
    public static class ResourceStats {
        private long total;
        private Map<String, Long> byType;
    }
}