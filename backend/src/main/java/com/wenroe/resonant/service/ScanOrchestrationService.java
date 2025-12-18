package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.ComplianceViolation;
import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import com.wenroe.resonant.model.entity.ScanJob;
import com.wenroe.resonant.model.entity.TagPolicy;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.ScanStatus;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.repository.ScanJobRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.aws.scanners.ResourceScanner;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that orchestrates the entire scanning process for an AWS account. Coordinates resource
 * discovery, compliance evaluation, and violation tracking.
 * <p>
 * Scans execute asynchronously with parallel scanner execution for improved performance.
 * Automatically discovers and uses all ResourceScanner implementations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScanOrchestrationService {

  private final AwsAccountRepository awsAccountRepository;
  private final AwsResourceRepository awsResourceRepository;
  private final ScanJobRepository scanJobRepository;
  private final TagPolicyService tagPolicyService;
  private final ComplianceEvaluationService complianceEvaluationService;
  private final UserRepository userRepository;
  private final AwsAccountRegionService regionService;
  private final ResourceTypeSettingService resourceTypeSettingService;
  private final ResourceCleanupService resourceCleanupService;

  // Spring auto-injects all ResourceScanner implementations
  private final List<ResourceScanner> resourceScanners;

  // Self-reference for @Transactional method calls to work through Spring proxy
  // Field injection required here to avoid circular dependency during construction
  @Autowired
  @Lazy
  private ScanOrchestrationService self;

  /**
   * Initiates a scan for an AWS account. Returns the created ScanJob immediately. The actual scan
   * executes asynchronously in a background thread.
   */
  public ScanJob initiateScan(UUID accountId, UUID userId) {
    log.info("=== INITIATE SCAN: accountId={}, userId={}", accountId, userId);

    // Get account
    AwsAccount account = awsAccountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("AWS account not found"));

    // Verify ownership
    if (!account.getUser().getId().equals(userId)) {
      throw new RuntimeException("Not authorized to scan this AWS account");
    }

    // Verify account is active
    if (!account.isActive()) {
      throw new RuntimeException("AWS account is not active. Status: " + account.getStatus());
    }

    // Verify at least one region is enabled
    if (!regionService.hasEnabledRegions(accountId)) {
      throw new RuntimeException(
          "No regions enabled for scanning. Please enable at least one region.");
    }

    // Check if there's already a running scan for this account
    Optional<ScanJob> existingRunningScan = scanJobRepository.findRunningScanForAccount(accountId);
    if (existingRunningScan.isPresent()) {
      throw new RuntimeException("A scan is already running for this account");
    }

    // Get user
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    // Create scan job
    ScanJob scanJob = new ScanJob();
    scanJob.setAwsAccount(account);
    scanJob.setUser(user);
    scanJob.setStatus(ScanStatus.PENDING);
    ScanJob savedJob = scanJobRepository.save(scanJob);

    log.info("=== SCAN JOB SAVED: id={}, status={}", savedJob.getId(), savedJob.getStatus());

    // Execute scan asynchronously
    log.info("=== CALLING executeScanAsync (should return immediately)");
    self.executeScanAsync(savedJob.getId());
    log.info("=== RETURNED FROM executeScanAsync");

    return savedJob;
  }

  /**
   * Executes the actual scanning process asynchronously. Runs in a separate thread pool to avoid
   * blocking the request thread.
   */
  @Async("scanExecutor")
  public void executeScanAsync(UUID scanJobId) {
    String threadName = Thread.currentThread().getName();
    log.info("=== ASYNC STARTED: scanJobId={}, thread={}", scanJobId, threadName);

    if (!threadName.startsWith("scan-")) {
      log.error("=== ASYNC NOT WORKING! Thread name should start with 'scan-' but got: {}",
          threadName);
    }

    try {
      log.info("=== CALLING self.executeScan");
      self.executeScan(scanJobId);
      log.info("=== COMPLETED self.executeScan");
    } catch (Exception e) {
      log.error("=== ASYNC EXECUTION FAILED: scanJobId={}, error={}",
          scanJobId, e.getMessage(), e);
      self.markScanAsFailed(scanJobId, e.getMessage());
    }
  }

  /**
   * Executes the scanning process. Runs all scanners in parallel for improved performance. Uses a
   * new transaction to ensure database operations complete even if called from async context.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void executeScan(UUID scanJobId) {
    log.info("=== EXECUTE SCAN: scanJobId={}, thread={}",
        scanJobId, Thread.currentThread().getName());

    ScanJob scanJob = scanJobRepository.findById(scanJobId)
        .orElseThrow(() -> new RuntimeException("Scan job not found"));

    log.info("=== SCAN JOB FOUND: id={}, current status={}", scanJob.getId(), scanJob.getStatus());

    scanJob.start();
    log.info("=== SCAN JOB STATUS CHANGED TO: {}", scanJob.getStatus());

    scanJobRepository.save(scanJob);
    log.info("=== SCAN JOB SAVED WITH STATUS: {}", scanJob.getStatus());

    try {
      AwsAccount account = scanJob.getAwsAccount();
      UUID userId = scanJob.getUser().getId();

      // Step 0: Clean up out-of-scope resources based on current settings
      log.info("=== CLEANUP: Removing out-of-scope resources for account {}",
          account.getAccountId());
      resourceCleanupService.cleanupOutOfScopeResources(account);

      // Step 1: Get enabled policies for the user
      List<TagPolicy> enabledPolicies = tagPolicyService.getEnabledPoliciesByUserId(userId);
      log.info("Found {} enabled policies for user {}", enabledPolicies.size(), userId);

      if (enabledPolicies.isEmpty()) {
        log.warn(
            "No enabled policies found for user {}. Scan will discover resources but not check compliance.",
            userId);
      }

      // Step 2: Run all scanners in parallel
      Set<String> enabledResourceTypes = resourceTypeSettingService.getEnabledResourceTypes()
          .stream()
          .map(ResourceTypeSetting::getResourceType)
          .collect(Collectors.toSet());

      log.info(
          "Starting parallel resource scans with {} scanners for account {} (enabled types: {})",
          resourceScanners.size(), account.getAccountId(), enabledResourceTypes);

      List<CompletableFuture<List<AwsResource>>> scanFutures = new ArrayList<>();

      for (ResourceScanner scanner : resourceScanners) {
        if (enabledResourceTypes.contains(scanner.getResourceType())) {
          CompletableFuture<List<AwsResource>> future = CompletableFuture.supplyAsync(
              () -> {
                log.info("Running {} scanner for account {}",
                    scanner.getResourceType(), account.getAccountId());
                try {
                  List<AwsResource> resources = scanner.scan(account);
                  log.info("{} scanner found {} resources",
                      scanner.getResourceType(), resources.size());
                  return resources;
                } catch (Exception e) {
                  log.error("{} scanner failed: {}",
                      scanner.getResourceType(), e.getMessage(), e);
                  return List.of(); // Return empty list on error
                }
              });
          scanFutures.add(future);
        } else {
          log.info("Skipping {} scanner (disabled): {}",
              scanner.getResourceType(), account.getAccountId());
        }
      }

      if (scanFutures.isEmpty()) {
        log.info("No enabled scanners for account {} -- completing scan with zero resources",
            account.getAccountId());

        // Complete scan job with zero resources
        scanJob.complete(0, 0, 0);
        scanJobRepository.save(scanJob);
        return;
      }

      // Wait for all scanners to complete
      CompletableFuture<Void> allScans = CompletableFuture.allOf(
          scanFutures.toArray(new CompletableFuture[0]));
      allScans.join();

      // Collect all discovered resources
      List<AwsResource> discoveredResources = new ArrayList<>();
      for (CompletableFuture<List<AwsResource>> future : scanFutures) {
        discoveredResources.addAll(future.join());
      }

      log.info("Discovered {} total resources from {} scanners",
          discoveredResources.size(), resourceScanners.size());

      // Step 3: Save or update resources and evaluate compliance
      int resourcesScanned = 0;
      int violationsFound = 0;
      int violationsResolved = 0;

      for (AwsResource discovered : discoveredResources) {
        // Check if resource already exists
        Optional<AwsResource> existing = awsResourceRepository.findByResourceArn(
            discovered.getResourceArn());

        AwsResource resource;
        if (existing.isPresent()) {
          // Update existing resource
          resource = existing.get();
          resource.setTags(discovered.getTags());
          resource.setMetadata(discovered.getMetadata());
          resource.setName(discovered.getName());
          resource.setRegion(discovered.getRegion());
          resource.updateLastSeen();
          log.debug("Updated existing resource: {}", resource.getResourceArn());
        } else {
          // Save new resource
          resource = discovered;
          log.debug("Discovered new resource: {}", resource.getResourceArn());
        }

        resource = awsResourceRepository.save(resource);
        resourcesScanned++;

        // Step 4: Evaluate compliance for this resource
        List<ComplianceViolation> violations = complianceEvaluationService
            .evaluateResource(resource, enabledPolicies);

        // Link violations to this scan job
        for (ComplianceViolation violation : violations) {
          violation.setScanJob(scanJob);
        }

        violationsFound += violations.size();
      }

      // Step 5: Update account last scan time
      account.setLastScanAt(LocalDateTime.now());
      awsAccountRepository.save(account);

      // Step 6: Complete scan job
      scanJob.complete(resourcesScanned, violationsFound, violationsResolved);
      scanJobRepository.save(scanJob);

      log.info("Scan job {} completed successfully. Scanned {} resources, found {} violations",
          scanJob.getId(), resourcesScanned, violationsFound);

    } catch (Exception e) {
      log.error("Scan job {} failed: {}", scanJob.getId(), e.getMessage(), e);
      scanJob.fail(e.getMessage());
      scanJobRepository.save(scanJob);
      throw e;
    }
  }

  /**
   * Marks a scan as failed. Uses new transaction to ensure update even if outer transaction rolls
   * back.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected void markScanAsFailed(UUID scanJobId, String errorMessage) {
    try {
      ScanJob scanJob = scanJobRepository.findById(scanJobId)
          .orElse(null);

      if (scanJob != null) {
        scanJob.fail(errorMessage);
        scanJobRepository.save(scanJob);
        log.info("Marked scan job {} as FAILED", scanJobId);
      }
    } catch (Exception e) {
      log.error("Failed to mark scan {} as failed: {}", scanJobId, e.getMessage());
    }
  }

  /**
   * Gets a scan job by ID.
   */
  public ScanJob getScanJob(UUID scanJobId) {
    return scanJobRepository.findById(scanJobId)
        .orElseThrow(() -> new RuntimeException("Scan job not found"));
  }

  /**
   * Gets all scan jobs for a user.
   */
  public List<ScanJob> getScanJobsByUserId(UUID userId) {
    return scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  /**
   * Gets scan jobs for a specific AWS account.
   */
  public List<ScanJob> getScanJobsByAccountId(UUID accountId) {
    return scanJobRepository.findByAwsAccountIdOrderByCreatedAtDesc(accountId);
  }

  /**
   * Gets the most recent scan job for an account.
   */
  public Optional<ScanJob> getLastScanForAccount(UUID accountId) {
    return scanJobRepository.findFirstByAwsAccountIdOrderByCreatedAtDesc(accountId);
  }
}