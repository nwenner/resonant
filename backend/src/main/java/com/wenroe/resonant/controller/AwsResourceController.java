package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.aws.AwsResourceResponse;
import com.wenroe.resonant.dto.aws.ResourceStats;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.util.OwnershipVerificationUtil;
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

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Slf4j
public class AwsResourceController {

    private final AwsResourceRepository resourceRepository;

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
                    .toList();
        }

        List<AwsResourceResponse> response = resources.stream()
                .map(AwsResourceResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AwsResourceResponse> getResource(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        AwsResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        if (OwnershipVerificationUtil.unverifiedOwnership(user, resource)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(AwsResourceResponse.fromEntity(resource));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<AwsResourceResponse>> getResourcesByAccount(
            @AuthenticationPrincipal User user,
            @PathVariable UUID accountId) {

        log.info("Fetching resources for account {}", accountId);

        List<AwsResource> resources = resourceRepository.findByAwsAccountId(accountId);

        if (!resources.isEmpty()) {
            UUID ownerId = resources.getFirst().getAwsAccount().getUser().getId();
            if (OwnershipVerificationUtil.unverifiedOwnershipById(ownerId, user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        List<AwsResourceResponse> response = resources.stream()
                .map(AwsResourceResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

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
}