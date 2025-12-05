package com.wenroe.resonant.service;

import com.wenroe.resonant.dto.compliance.ComplianceRateResponse;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.repository.ComplianceViolationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

  private final AwsResourceRepository awsResourceRepository;
  private final ComplianceViolationRepository complianceViolationRepository;

  /**
   * Calculate compliance rate for a user's AWS resources.
   * <p>
   * Compliance Rate = (Total Resources - Resources with Open Violations) / Total Resources × 100
   * <p>
   * Note: One resource can have multiple violations (one per policy), so we count distinct violated
   * resources.
   */
  @Transactional(readOnly = true)
  public ComplianceRateResponse getComplianceRate(UUID userId) {
    log.debug("Calculating compliance rate for user {}", userId);

    // Get total resources count (joins through AwsAccount -> User)
    long totalResources = awsResourceRepository.countByAwsAccount_User_Id(userId);

    // Get count of distinct resources with open violations
    long nonCompliantResources = complianceViolationRepository
        .countDistinctViolatedResourcesByUserId(userId);

    // Calculate compliant resources
    long compliantResources = totalResources - nonCompliantResources;

    // Calculate compliance rate as percentage
    double complianceRate = totalResources > 0
        ? (compliantResources / (double) totalResources) * 100.0
        : 100.0; // 100% if no resources (nothing to be non-compliant)

    log.info("Compliance rate for user {}: {}/{} resources compliant ({}%)",
        userId, compliantResources, totalResources, String.format("%.1f", complianceRate));

    return new ComplianceRateResponse(
        totalResources,
        compliantResources,
        nonCompliantResources,
        complianceRate
    );
  }
}