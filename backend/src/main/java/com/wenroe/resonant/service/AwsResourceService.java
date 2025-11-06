package com.wenroe.resonant.service;

import com.wenroe.resonant.dto.aws.ResourceStats;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.repository.AwsResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsResourceService {

    private final AwsResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public List<AwsResource> getAllResources(UUID userId, String type) {
        log.info("Fetching resources for user {} with type filter: {}", userId, type);

        List<AwsResource> resources = resourceRepository.findByUserId(userId);

        if (type != null && !type.isEmpty()) {
            resources = resources.stream()
                    .filter(r -> r.getResourceType().equalsIgnoreCase(type))
                    .toList();
        }

        return resources;
    }

    @Transactional(readOnly = true)
    public AwsResource getResourceById(UUID id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resource not found"));
    }

    @Transactional(readOnly = true)
    public List<AwsResource> getResourcesByAccountId(UUID accountId) {
        log.info("Fetching resources for account {}", accountId);
        return resourceRepository.findByAwsAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public ResourceStats getResourceStats(UUID userId) {
        long totalResources = resourceRepository.countByUserId(userId);
        List<Object[]> byType = resourceRepository.countResourcesByType(userId);

        ResourceStats stats = new ResourceStats();
        stats.setTotal(totalResources);

        Map<String, Long> typeCounts = byType.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
        stats.setByType(typeCounts);

        return stats;
    }
}