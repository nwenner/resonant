package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsAccountRegion;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import com.wenroe.resonant.repository.AwsAccountRegionRepository;
import com.wenroe.resonant.repository.AwsResourceRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for cleaning up AWS resources that are no longer in scope based on current settings.
 * Resources are removed when: 1. Their resource type is disabled globally 2. Their region is
 * disabled for the account (regional resources only) 3. Global resources when no regions are
 * enabled for the account
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceCleanupService {

  // Resource types that are global (not region-specific)
  private static final Set<String> GLOBAL_RESOURCE_TYPES = Set.of(
      "cloudfront:distribution",
      "iam:role",
      "iam:user",
      "iam:policy",
      "route53:hostedzone"
  );
  private final AwsResourceRepository awsResourceRepository;
  private final AwsAccountRegionRepository awsAccountRegionRepository;
  private final ResourceTypeSettingService resourceTypeSettingService;

  /**
   * Clean up resources that are no longer in scope for the given account. This is called at the
   * start of each scan to reconcile the database with current settings.
   */
  @Transactional
  public void cleanupOutOfScopeResources(AwsAccount account) {
    log.info("Starting cleanup of out-of-scope resources for account {}", account.getAccountId());

    // Get current enabled settings
    Set<String> enabledResourceTypes = resourceTypeSettingService.getEnabledResourceTypes()
        .stream()
        .map(ResourceTypeSetting::getResourceType)
        .collect(Collectors.toSet());

    Set<String> enabledRegions = awsAccountRegionRepository
        .findEnabledRegionsByAccountId(account.getId())
        .stream()
        .map(AwsAccountRegion::getRegionCode)
        .collect(Collectors.toSet());

    log.debug("Enabled resource types: {}", enabledResourceTypes);
    log.debug("Enabled regions for account {}: {}", account.getAccountId(), enabledRegions);

    // Get all existing resources for this account (with violations loaded for cascade delete)
    List<AwsResource> existingResources = awsResourceRepository.findByAwsAccountIdWithViolations(
        account.getId());
    log.debug("Found {} existing resources for account {}", existingResources.size(),
        account.getAccountId());

    int deletedCount = 0;

    for (AwsResource resource : existingResources) {
      boolean shouldDelete = false;
      String reason = null;

      // Check if resource type is disabled
      if (!enabledResourceTypes.contains(resource.getResourceType())) {
        shouldDelete = true;
        reason = "resource type disabled";
      }
      // Check if resource is in a disabled region (for regional resources)
      else if (!isGlobalResource(resource) && !enabledRegions.contains(resource.getRegion())) {
        shouldDelete = true;
        reason = "region disabled";
      }
      // Check if global resource but no regions enabled (orphaned global resource)
      else if (isGlobalResource(resource) && enabledRegions.isEmpty()) {
        shouldDelete = true;
        reason = "no regions enabled for global resource";
      }

      if (shouldDelete) {
        log.info("Deleting resource {} (type: {}, region: {}) - reason: {}",
            resource.getResourceArn(),
            resource.getResourceType(),
            resource.getRegion(),
            reason);

        awsResourceRepository.delete(resource);
        deletedCount++;
      }
    }

    log.info("Cleanup completed for account {}. Deleted {} out-of-scope resources",
        account.getAccountId(), deletedCount);
  }

  /**
   * Determine if a resource is global (not region-specific).
   */
  private boolean isGlobalResource(AwsResource resource) {
    return GLOBAL_RESOURCE_TYPES.contains(resource.getResourceType());
  }
}