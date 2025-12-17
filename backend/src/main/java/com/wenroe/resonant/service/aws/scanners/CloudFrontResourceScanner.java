package com.wenroe.resonant.service.aws.scanners;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.service.aws.AwsClientFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.DistributionSummary;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionResponse;
import software.amazon.awssdk.services.cloudfront.model.ListDistributionsResponse;
import software.amazon.awssdk.services.cloudfront.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.cloudfront.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.cloudfront.model.Origin;
import software.amazon.awssdk.services.cloudfront.model.Tag;
import software.amazon.awssdk.services.cloudfront.model.Tags;

/**
 * Service for scanning CloudFront distributions and their tags. CloudFront is a global service (not
 * region-specific).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudFrontResourceScanner implements ResourceScanner {

  private static final String RESOURCE_TYPE = "cloudfront:distribution";

  private final AwsClientFactory clientFactory;

  @Override
  public String getResourceType() {
    return RESOURCE_TYPE;
  }

  /**
   * Scans all CloudFront distributions for an AWS account. CloudFront is a global service.
   */
  @Override
  public List<AwsResource> scan(AwsAccount account) {
    return scanDistributions(account);
  }

  /**
   * Scans all CloudFront distributions for an AWS account. CloudFront is a global service.
   */
  public List<AwsResource> scanDistributions(AwsAccount account) {
    List<AwsResource> resources = new ArrayList<>();

    try (CloudFrontClient cfClient = clientFactory.createCloudFrontClient(account)) {

      // List all distributions
      ListDistributionsResponse response = cfClient.listDistributions();

      if (response.distributionList() == null ||
          !response.distributionList().hasItems() ||
          response.distributionList().items() == null) {
        log.info("No CloudFront distributions found in account {}", account.getAccountId());
        return resources;
      }

      List<DistributionSummary> distributions = response.distributionList().items();
      log.info("Found {} CloudFront distributions in account {}",
          distributions.size(), account.getAccountId());

      for (DistributionSummary dist : distributions) {
        try {
          AwsResource resource = scanDistribution(cfClient, account, dist);
          resources.add(resource);
          log.debug("Scanned CloudFront distribution: {} (domain: {})",
              dist.id(), dist.domainName());
        } catch (Exception e) {
          log.error("Failed to scan CloudFront distribution {}: {}",
              dist.id(), e.getMessage(), e);
        }
      }

    } catch (Exception e) {
      log.error("Failed to scan CloudFront distributions for account {}: {}",
          account.getAccountId(), e.getMessage());
      throw new RuntimeException("CloudFront scan failed: " + e.getMessage(), e);
    }

    return resources;
  }

  /**
   * Scans a single CloudFront distribution and retrieves its tags and metadata.
   */
  private AwsResource scanDistribution(CloudFrontClient cfClient, AwsAccount account,
      DistributionSummary dist) {

    String distributionId = dist.id();
    String distributionArn = dist.arn();

    AwsResource resource = new AwsResource();
    resource.setAwsAccount(account);
    resource.setResourceId(distributionId);
    resource.setResourceType(RESOURCE_TYPE);
    resource.setName(dist.domainName());
    resource.setRegion("global"); // CloudFront is global
    resource.setResourceArn(distributionArn);

    // Get tags
    Map<String, String> tags = getDistributionTags(cfClient, distributionArn);
    resource.setTags(tags);

    // Get detailed metadata
    Map<String, Object> metadata = getDistributionMetadata(cfClient, distributionId, dist);
    resource.setMetadata(metadata);

    resource.setLastSeenAt(LocalDateTime.now());

    return resource;
  }

  /**
   * Gets tags for a CloudFront distribution.
   */
  private Map<String, String> getDistributionTags(CloudFrontClient cfClient,
      String distributionArn) {
    Map<String, String> tags = new HashMap<>();

    try {
      ListTagsForResourceResponse response = cfClient.listTagsForResource(
          ListTagsForResourceRequest.builder()
              .resource(distributionArn)
              .build()
      );

      Tags cfTags = response.tags();
      if (cfTags != null && cfTags.items() != null) {
        for (Tag tag : cfTags.items()) {
          tags.put(tag.key(), tag.value());
        }
      }

      log.debug("Found {} tags for distribution {}", tags.size(), distributionArn);

    } catch (Exception e) {
      log.warn("Failed to get tags for distribution {}: {}", distributionArn, e.getMessage());
    }

    return tags;
  }

  /**
   * Gets detailed metadata for a CloudFront distribution.
   */
  private Map<String, Object> getDistributionMetadata(CloudFrontClient cfClient,
      String distributionId, DistributionSummary summary) {
    Map<String, Object> metadata = new HashMap<>();

    // Basic info from summary
    metadata.put("status", summary.status());
    metadata.put("enabled", summary.enabled());
    metadata.put("domainName", summary.domainName());
    metadata.put("priceClass", summary.priceClassAsString());
    metadata.put("httpVersion", summary.httpVersionAsString());

    if (summary.comment() != null && !summary.comment().isEmpty()) {
      metadata.put("comment", summary.comment());
    }

    // Get detailed configuration
    try {
      GetDistributionResponse response = cfClient.getDistribution(
          GetDistributionRequest.builder()
              .id(distributionId)
              .build()
      );

      if (response.distribution() != null &&
          response.distribution().distributionConfig() != null) {

        var config = response.distribution().distributionConfig();

        metadata.put("defaultRootObject", config.defaultRootObject());

        // Origins
        if (config.origins() != null && config.origins().items() != null) {
          List<String> originDomains = new ArrayList<>();
          for (Origin origin : config.origins().items()) {
            originDomains.add(origin.domainName());
          }
          metadata.put("origins", originDomains);
        }
      }

    } catch (Exception e) {
      log.debug("Failed to get detailed config for distribution {}: {}",
          distributionId, e.getMessage());
    }

    return metadata;
  }
}