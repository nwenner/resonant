package com.wenroe.resonant.service.aws;

import com.wenroe.resonant.model.entity.AwsAccount;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.Region;

/**
 * Service for discovering available AWS regions for an account. Uses EC2 DescribeRegions API to
 * find all enabled regions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsRegionDiscoveryService {

  private final AwsClientFactory clientFactory;

  /**
   * Discovers all enabled AWS regions for an account. Calls EC2 DescribeRegions API from
   * us-east-1.
   *
   * @param account The AWS account to discover regions for
   * @return List of enabled region codes (e.g., ["us-east-1", "us-west-2", ...])
   */
  public List<String> discoverEnabledRegions(AwsAccount account) {
    log.info("Discovering enabled regions for AWS account: {}", account.getAccountId());

    try (Ec2Client ec2Client = clientFactory.createEc2ClientForRegionDiscovery(account)) {

      DescribeRegionsResponse response = ec2Client.describeRegions();

      List<String> regionCodes = response.regions().stream()
          .map(Region::regionName)
          .sorted()
          .collect(Collectors.toList());

      log.info("Discovered {} enabled regions for account {}: {}",
          regionCodes.size(), account.getAccountId(), regionCodes);

      return regionCodes;

    } catch (Exception e) {
      log.error("Failed to discover regions for account {}: {}",
          account.getAccountId(), e.getMessage(), e);
      throw new RuntimeException("Failed to discover AWS regions: " + e.getMessage(), e);
    }
  }
}