package com.wenroe.resonant.service.aws;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.service.security.CredentialEncryptionService;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * Factory for creating AWS SDK clients with proper credential management and caching. Handles both
 * IAM role assumption and access key authentication.
 * <p>
 * Credentials are cached per account to avoid repeated AssumeRole calls. Cache expiration is set 5
 * minutes before AWS credential expiration for safety.
 */
@Component
@Slf4j
public class AwsClientFactory {

  private final CredentialEncryptionService encryptionService;
  private final Cache<UUID, AwsCredentials> credentialCache;
  private final Integer sessionDuration;

  public AwsClientFactory(
      CredentialEncryptionService encryptionService,
      @Value("${resonant.aws.session-duration:3600}") Integer sessionDuration) {
    this.encryptionService = encryptionService;
    this.sessionDuration = sessionDuration;

    // Cache credentials for (sessionDuration - 5 minutes) to ensure safety buffer
    long cacheDurationSeconds = Math.max(sessionDuration - 300, 300);

    this.credentialCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(cacheDurationSeconds))
        .maximumSize(100)
        .recordStats()
        .build();

    log.info("Initialized credential cache with {}s expiration (session: {}s, buffer: 300s)",
        cacheDurationSeconds, sessionDuration);
  }

  /**
   * Creates an STS client for role assumption. Uses default credentials from: 1. Environment
   * variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY) 2. ~/.aws/credentials file (default
   * profile or AWS_PROFILE env var) 3. IAM role (if running on EC2/ECS)
   * <p>
   * For local development, ensure AWS CLI is configured (aws configure).
   */
  public StsClient createStsClient() {
    return StsClient.builder()
        .region(Region.US_EAST_1) // STS is global, but needs a region
        .build(); // Uses default credential provider chain
  }

  /**
   * Assumes an IAM role and returns temporary credentials.
   */
  private AwsSessionCredentials assumeRole(AwsAccount account) {
    if (!account.usesRole()) {
      throw new IllegalArgumentException("Account is not configured for role assumption");
    }

    try (StsClient stsClient = createStsClient()) {
      AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
          .roleArn(account.getRoleArn())
          .roleSessionName("resonant-scan-" + System.currentTimeMillis())
          .externalId(account.getExternalId())
          .durationSeconds(sessionDuration)
          .build();

      AssumeRoleResponse response = stsClient.assumeRole(assumeRoleRequest);
      Credentials credentials = response.credentials();

      log.info("Successfully assumed role for account: {} (expires: {})",
          account.getAccountId(), credentials.expiration());

      return AwsSessionCredentials.create(
          credentials.accessKeyId(),
          credentials.secretAccessKey(),
          credentials.sessionToken()
      );

    } catch (Exception e) {
      log.error("Failed to assume role for account {}: {}",
          account.getAccountId(), e.getMessage());
      throw new RuntimeException("Failed to assume AWS role: " + e.getMessage(), e);
    }
  }

  /**
   * Resolves credentials for an AWS account with caching. For role-based accounts, credentials are
   * cached to avoid repeated AssumeRole calls. For access key accounts, credentials are decrypted
   * and cached.
   */
  public AwsCredentials resolveCredentials(AwsAccount account) {
    UUID accountId = account.getId();

    AwsCredentials cached = credentialCache.getIfPresent(accountId);
    if (cached != null) {
      log.debug("Using cached credentials for account: {}", account.getAccountId());
      return cached;
    }

    log.info("Resolving fresh credentials for account: {}", account.getAccountId());

    AwsCredentials credentials;
    if (account.usesRole()) {
      credentials = assumeRole(account);
    } else {
      // Decrypt and use access keys
      String accessKey = encryptionService.decrypt(account.getAccessKeyEncrypted());
      String secretKey = encryptionService.decrypt(account.getSecretKeyEncrypted());
      credentials = AwsBasicCredentials.create(accessKey, secretKey);
    }

    credentialCache.put(accountId, credentials);
    log.debug("Cached credentials for account: {}", account.getAccountId());

    return credentials;
  }

  /**
   * Evicts cached credentials for an account. Useful when credential errors occur or account is
   * updated.
   */
  public void evictCredentials(UUID accountId) {
    credentialCache.invalidate(accountId);
    log.info("Evicted cached credentials for account: {}", accountId);
  }

  /**
   * Clears all cached credentials.
   */
  public void clearCredentialCache() {
    credentialCache.invalidateAll();
    log.info("Cleared all cached credentials");
  }

  /**
   * Gets cache statistics for monitoring.
   */
  public String getCacheStats() {
    return credentialCache.stats().toString();
  }

  /**
   * Creates an EC2 client for the specified account and region.
   */
  public Ec2Client createEc2Client(AwsAccount account, String regionCode) {
    AwsCredentials credentials = resolveCredentials(account);

    return Ec2Client.builder()
        .region(Region.of(regionCode))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .build();
  }

  /**
   * Creates an S3 client for the specified account. If regionCode is null, enables cross-region
   * access for operations on buckets in any region. If regionCode is provided, creates a
   * region-specific client.
   */
  public S3Client createS3Client(AwsAccount account, String regionCode) {
    AwsCredentials credentials = resolveCredentials(account);

    S3ClientBuilder builder = S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials));

    if (regionCode == null) {
      // Enable cross-region access for S3 operations
      builder.region(Region.US_EAST_1);
      builder.crossRegionAccessEnabled(true);
      log.debug("Created S3 client with cross-region access enabled");
    } else {
      // Region-specific client
      builder.region(Region.of(regionCode));
      log.debug("Created S3 client for region: {}", regionCode);
    }

    return builder.build();
  }

  /**
   * Creates a CloudFront client for the specified account. CloudFront is a global service (uses
   * us-east-1 as endpoint).
   */
  public CloudFrontClient createCloudFrontClient(AwsAccount account) {
    AwsCredentials credentials = resolveCredentials(account);

    return CloudFrontClient.builder()
        .region(Region.AWS_GLOBAL)
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .build();
  }

  /**
   * Creates an RDS client for the specified account and region.
   */
  public RdsClient createRdsClient(AwsAccount account, String regionCode) {
    AwsCredentials credentials = resolveCredentials(account);

    return RdsClient.builder()
        .region(Region.of(regionCode))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .build();
  }

  /**
   * Creates an EC2 client for listing regions (uses us-east-1 as default).
   */
  public Ec2Client createEc2ClientForRegionDiscovery(AwsAccount account) {
    return createEc2Client(account, "us-east-1");
  }
}