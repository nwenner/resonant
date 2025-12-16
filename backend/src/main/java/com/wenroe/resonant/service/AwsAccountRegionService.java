package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsAccountRegion;
import com.wenroe.resonant.repository.AwsAccountRegionRepository;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.service.aws.AwsRegionDiscoveryService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing AWS account region configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsAccountRegionService {

  private final AwsAccountRegionRepository regionRepository;
  private final AwsAccountRepository accountRepository;
  private final AwsRegionDiscoveryService regionDiscoveryService;

  /**
   * Discovers and persists all available regions for an AWS account. Called after account
   * creation/connection. All regions are enabled by default.
   *
   * @param accountId The AWS account ID
   * @return List of created region entities
   */
  @Transactional
  public List<AwsAccountRegion> discoverAndPersistRegions(UUID accountId) {
    AwsAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("AWS account not found"));

    log.info("Discovering regions for AWS account: {}", account.getAccountId());

    // Discover regions via EC2 API
    List<String> regionCodes = regionDiscoveryService.discoverEnabledRegions(account);

    // Create region entities (all enabled by default)
    List<AwsAccountRegion> regions = regionCodes.stream()
        .map(regionCode -> {
          AwsAccountRegion region = new AwsAccountRegion();
          region.setAwsAccount(account);
          region.setRegionCode(regionCode);
          region.setEnabled(true);
          return region;
        })
        .collect(Collectors.toList());

    List<AwsAccountRegion> saved = regionRepository.saveAll(regions);
    log.info("Persisted {} regions for account {}", saved.size(), account.getAccountId());

    return saved;
  }

  /**
   * Gets all regions for an AWS account.
   */
  public List<AwsAccountRegion> getRegionsByAccountId(UUID accountId) {
    return regionRepository.findByAwsAccountId(accountId);
  }

  /**
   * Gets only enabled regions for an AWS account.
   */
  public List<AwsAccountRegion> getEnabledRegionsByAccountId(UUID accountId) {
    return regionRepository.findEnabledRegionsByAccountId(accountId);
  }

  /**
   * Enables a specific region for scanning.
   */
  @Transactional
  public AwsAccountRegion enableRegion(UUID accountId, String regionCode, UUID userId) {
    AwsAccount account = getAccountAndVerifyOwnership(accountId, userId);

    AwsAccountRegion region = regionRepository.findByAwsAccountIdAndRegionCode(accountId,
            regionCode)
        .orElseThrow(() -> new RuntimeException("Region " + regionCode + " not found for account"));

    region.setEnabled(true);
    AwsAccountRegion saved = regionRepository.save(region);

    log.info("Enabled region {} for account {} (user: {})", regionCode, account.getAccountId(),
        userId);
    return saved;
  }

  /**
   * Disables a specific region for scanning.
   */
  @Transactional
  public AwsAccountRegion disableRegion(UUID accountId, String regionCode, UUID userId) {
    AwsAccount account = getAccountAndVerifyOwnership(accountId, userId);

    AwsAccountRegion region = regionRepository.findByAwsAccountIdAndRegionCode(accountId,
            regionCode)
        .orElseThrow(() -> new RuntimeException("Region " + regionCode + " not found for account"));

    region.setEnabled(false);
    AwsAccountRegion saved = regionRepository.save(region);

    log.info("Disabled region {} for account {} (user: {})", regionCode, account.getAccountId(),
        userId);
    return saved;
  }

  /**
   * Updates multiple regions at once (bulk enable/disable).
   */
  @Transactional
  public List<AwsAccountRegion> updateRegions(UUID accountId, List<String> enabledRegionCodes,
      UUID userId) {
    AwsAccount account = getAccountAndVerifyOwnership(accountId, userId);

    List<AwsAccountRegion> allRegions = regionRepository.findByAwsAccountId(accountId);

    for (AwsAccountRegion region : allRegions) {
      region.setEnabled(enabledRegionCodes.contains(region.getRegionCode()));
    }

    List<AwsAccountRegion> saved = regionRepository.saveAll(allRegions);

    log.info("Updated {} regions for account {} (user: {}). {} enabled.",
        allRegions.size(), account.getAccountId(), userId, enabledRegionCodes.size());

    return saved;
  }

  /**
   * Checks if an account has at least one enabled region.
   */
  public boolean hasEnabledRegions(UUID accountId) {
    long enabledCount = regionRepository.countByAwsAccountIdAndEnabled(accountId, true);
    return enabledCount > 0;
  }

  /**
   * Rediscovers regions for an account (useful if AWS adds new regions). Only adds new regions,
   * doesn't remove existing ones.
   */
  @Transactional
  public List<AwsAccountRegion> rediscoverRegions(UUID accountId, UUID userId) {
    AwsAccount account = getAccountAndVerifyOwnership(accountId, userId);

    log.info("Rediscovering regions for AWS account: {}", account.getAccountId());

    // Get currently discovered regions
    List<String> discoveredRegions = regionDiscoveryService.discoverEnabledRegions(account);

    // Get existing region codes
    List<String> existingRegionCodes = regionRepository.findByAwsAccountId(accountId)
        .stream()
        .map(AwsAccountRegion::getRegionCode)
        .collect(Collectors.toList());

    // Find new regions
    List<String> newRegionCodes = discoveredRegions.stream()
        .filter(code -> !existingRegionCodes.contains(code))
        .collect(Collectors.toList());

    if (newRegionCodes.isEmpty()) {
      log.info("No new regions discovered for account {}", account.getAccountId());
      return List.of();
    }

    // Create new region entities (enabled by default)
    List<AwsAccountRegion> newRegions = newRegionCodes.stream()
        .map(regionCode -> {
          AwsAccountRegion region = new AwsAccountRegion();
          region.setAwsAccount(account);
          region.setRegionCode(regionCode);
          region.setEnabled(true);
          return region;
        })
        .collect(Collectors.toList());

    List<AwsAccountRegion> saved = regionRepository.saveAll(newRegions);
    log.info("Added {} new regions for account {}", saved.size(), account.getAccountId());

    return saved;
  }

  /**
   * Helper to get account and verify ownership.
   */
  private AwsAccount getAccountAndVerifyOwnership(UUID accountId, UUID userId) {
    AwsAccount account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("AWS account not found"));

    if (!account.getUser().getId().equals(userId)) {
      throw new RuntimeException("Not authorized to access this AWS account");
    }

    return account;
  }
}