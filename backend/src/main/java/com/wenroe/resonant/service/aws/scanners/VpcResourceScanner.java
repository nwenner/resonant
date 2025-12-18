package com.wenroe.resonant.service.aws.scanners;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsAccountRegion;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.service.AwsAccountRegionService;
import com.wenroe.resonant.service.aws.AwsClientFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Vpc;

/**
 * Service for scanning VPCs and their tags across all enabled regions. VPCs are region-specific
 * resources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VpcResourceScanner implements ResourceScanner {

  private static final String RESOURCE_TYPE = "ec2:vpc";

  private final AwsClientFactory clientFactory;
  private final AwsAccountRegionService regionService;

  @Override
  public String getResourceType() {
    return RESOURCE_TYPE;
  }

  /**
   * Scans all VPCs across all enabled regions for an AWS account.
   */
  @Override
  public List<AwsResource> scan(AwsAccount account) {
    List<AwsResource> resources = new ArrayList<>();

    // Get enabled regions for this account
    List<AwsAccountRegion> enabledRegions = regionService.getEnabledRegionsByAccountId(
        account.getId());

    if (enabledRegions.isEmpty()) {
      log.warn("No enabled regions for account {}. Skipping VPC scan.",
          account.getAccountId());
      return resources;
    }

    log.info("Scanning VPCs in {} regions for account {}",
        enabledRegions.size(), account.getAccountId());

    // Scan each region
    for (AwsAccountRegion region : enabledRegions) {
      try {
        List<AwsResource> regionResources = scanVpcsInRegion(account, region);
        resources.addAll(regionResources);
        log.info("Found {} VPCs in region {}", regionResources.size(), region.getRegionCode());
      } catch (Exception e) {
        log.error("Failed to scan VPCs in region {}: {}", region.getRegionCode(), e.getMessage(),
            e);
      }
    }

    log.info("Found {} total VPCs across all enabled regions for account {}",
        resources.size(), account.getAccountId());

    return resources;
  }

  /**
   * Scans VPCs in a specific region.
   */
  private List<AwsResource> scanVpcsInRegion(AwsAccount account, AwsAccountRegion region) {
    List<AwsResource> resources = new ArrayList<>();

    try (Ec2Client ec2Client = clientFactory.createEc2Client(account, region.getRegionCode())) {

      DescribeVpcsResponse response = ec2Client.describeVpcs(
          DescribeVpcsRequest.builder().build()
      );

      if (!response.hasVpcs()) {
        log.debug("No VPCs found in region {}", region.getRegionCode());
        return resources;
      }

      log.debug("Found {} VPCs in region {} for account {}",
          response.vpcs().size(), region.getRegionCode(), account.getAccountId());

      for (Vpc vpc : response.vpcs()) {
        try {
          AwsResource resource = scanVpc(ec2Client, account, vpc, region.getRegionCode());
          resources.add(resource);
          log.debug("Scanned VPC: {} in region {}", vpc.vpcId(), region.getRegionCode());
        } catch (Exception e) {
          log.error("Failed to scan VPC {} in region {}: {}",
              vpc.vpcId(), region.getRegionCode(), e.getMessage(), e);
        }
      }

    } catch (Exception e) {
      log.error("Failed to list VPCs in region {}: {}", region.getRegionCode(), e.getMessage());
      throw new RuntimeException(
          "VPC scan failed in region " + region.getRegionCode() + ": " + e.getMessage(), e);
    }

    return resources;
  }

  /**
   * Scans a single VPC and retrieves its tags and metadata.
   */
  private AwsResource scanVpc(Ec2Client ec2Client, AwsAccount account, Vpc vpc, String region) {
    String vpcId = vpc.vpcId();

    AwsResource resource = new AwsResource();
    resource.setAwsAccount(account);
    resource.setResourceId(vpcId);
    resource.setResourceType(RESOURCE_TYPE);
    resource.setName(getVpcName(vpc));
    resource.setRegion(region);

    // Build ARN
    String arn = String.format("arn:aws:ec2:%s:%s:vpc/%s",
        region, account.getAccountId(), vpcId);
    resource.setResourceArn(arn);

    // Get tags
    Map<String, String> tags = getVpcTags(vpc);
    resource.setTags(tags);

    // Get metadata
    Map<String, Object> metadata = getVpcMetadata(ec2Client, vpc);
    resource.setMetadata(metadata);

    resource.setLastSeenAt(LocalDateTime.now());

    return resource;
  }

  /**
   * Gets the VPC name from tags, or returns the VPC ID if no name tag exists.
   */
  private String getVpcName(Vpc vpc) {
    if (vpc.hasTags()) {
      return vpc.tags().stream()
          .filter(tag -> "Name".equals(tag.key()))
          .map(Tag::value)
          .findFirst()
          .orElse(vpc.vpcId());
    }
    return vpc.vpcId();
  }

  /**
   * Extracts tags from a VPC.
   */
  private Map<String, String> getVpcTags(Vpc vpc) {
    Map<String, String> tags = new HashMap<>();

    if (vpc.hasTags()) {
      for (Tag tag : vpc.tags()) {
        tags.put(tag.key(), tag.value());
      }
      log.debug("Found {} tags for VPC {}", tags.size(), vpc.vpcId());
    } else {
      log.debug("No tags found for VPC {}", vpc.vpcId());
    }

    return tags;
  }

  /**
   * Gets additional metadata for a VPC.
   */
  private Map<String, Object> getVpcMetadata(Ec2Client ec2Client, Vpc vpc) {
    Map<String, Object> metadata = new HashMap<>();

    // Basic VPC properties
    metadata.put("cidrBlock", vpc.cidrBlock());
    metadata.put("state", vpc.stateAsString());
    metadata.put("isDefault", vpc.isDefault());
    metadata.put("instanceTenancy", vpc.instanceTenancyAsString());
    metadata.put("ownerId", vpc.ownerId());

    // Additional CIDR blocks
    if (vpc.hasCidrBlockAssociationSet() && vpc.cidrBlockAssociationSet().size() > 1) {
      List<String> additionalCidrs = vpc.cidrBlockAssociationSet().stream()
          .skip(1) // Skip the primary CIDR
          .map(assoc -> assoc.cidrBlock())
          .collect(Collectors.toList());
      metadata.put("additionalCidrBlocks", additionalCidrs);
    }

    // IPv6 CIDR blocks
    if (vpc.hasIpv6CidrBlockAssociationSet()) {
      List<String> ipv6Cidrs = vpc.ipv6CidrBlockAssociationSet().stream()
          .map(assoc -> assoc.ipv6CidrBlock())
          .collect(Collectors.toList());
      if (!ipv6Cidrs.isEmpty()) {
        metadata.put("ipv6CidrBlocks", ipv6Cidrs);
      }
    }

    // DHCP options ID
    if (vpc.dhcpOptionsId() != null) {
      metadata.put("dhcpOptionsId", vpc.dhcpOptionsId());
    }

    // Get subnet count
    try {
      DescribeSubnetsResponse subnetsResponse = ec2Client.describeSubnets(
          DescribeSubnetsRequest.builder()
              .filters(f -> f.name("vpc-id").values(vpc.vpcId()))
              .build()
      );
      metadata.put("subnetCount", subnetsResponse.subnets().size());
    } catch (Exception e) {
      log.debug("Failed to get subnet count for VPC {}: {}", vpc.vpcId(), e.getMessage());
    }

    return metadata;
  }
}