package com.wenroe.resonant.service.aws.scanners;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import java.util.List;

/**
 * Common interface for AWS resource scanners. Each scanner is responsible for discovering and
 * scanning a specific AWS resource type.
 */
public interface ResourceScanner {

  /**
   * Scans resources for the given AWS account.
   *
   * @param account The AWS account to scan
   * @return List of discovered resources with tags and metadata
   */
  List<AwsResource> scan(AwsAccount account);

  /**
   * Returns the resource type(s) this scanner handles. Used for logging and metrics.
   *
   * @return Resource type identifier (e.g., "s3:bucket", "vpc:vpc")
   */
  String getResourceType();
}