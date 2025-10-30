package com.wenroe.resonant.service.aws.scanners;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.service.aws.AwsClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for scanning S3 buckets and their tags.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3ResourceScanner {

    private static final String RESOURCE_TYPE = "s3:bucket";

    private final AwsClientFactory clientFactory;

    /**
     * Scans all S3 buckets for an AWS account.
     * Note: S3 is global, but we need to use a specific region client.
     */
    public List<AwsResource> scanS3Buckets(AwsAccount account) {
        List<AwsResource> resources = new ArrayList<>();

        try (S3Client s3Client = clientFactory.createS3Client(account, "us-east-1")) {

            // List all buckets
            ListBucketsResponse bucketsResponse = s3Client.listBuckets();
            log.info("Found {} S3 buckets in account {}",
                    bucketsResponse.buckets().size(), account.getAccountId());

            for (Bucket bucket : bucketsResponse.buckets()) {
                try {
                    AwsResource resource = scanBucket(s3Client, account, bucket);
                    resources.add(resource);
                    log.debug("Scanned S3 bucket: {}", bucket.name());
                } catch (Exception e) {
                    log.error("Failed to scan S3 bucket {}: {}", bucket.name(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to scan S3 buckets for account {}: {}",
                    account.getAccountId(), e.getMessage());
            throw new RuntimeException("S3 scan failed: " + e.getMessage(), e);
        }

        return resources;
    }

    /**
     * Scans a single S3 bucket and retrieves its tags and metadata.
     */
    private AwsResource scanBucket(S3Client s3Client, AwsAccount account, Bucket bucket) {
        AwsResource resource = new AwsResource();
        resource.setAwsAccount(account);
        resource.setResourceId(bucket.name());
        resource.setResourceType(RESOURCE_TYPE);
        resource.setName(bucket.name());

        // S3 buckets don't have regions in the standard sense, but we can get location
        String region = getBucketRegion(s3Client, bucket.name());
        resource.setRegion(region);

        // Build ARN
        String arn = String.format("arn:aws:s3:::%s", bucket.name());
        resource.setResourceArn(arn);

        // Get tags
        Map<String, String> tags = getBucketTags(s3Client, bucket.name());
        resource.setTags(tags);

        // Get metadata
        Map<String, Object> metadata = getBucketMetadata(s3Client, bucket.name());
        metadata.put("creationDate", bucket.creationDate().toString());
        resource.setMetadata(metadata);

        resource.setLastSeenAt(LocalDateTime.now());

        return resource;
    }

    /**
     * Gets the region/location of an S3 bucket.
     */
    private String getBucketRegion(S3Client s3Client, String bucketName) {
        try {
            GetBucketLocationResponse locationResponse = s3Client.getBucketLocation(
                    GetBucketLocationRequest.builder()
                            .bucket(bucketName)
                            .build()
            );

            String locationConstraint = locationResponse.locationConstraintAsString();

            // S3 returns null for us-east-1, empty string for us-east-1 in some cases
            if (locationConstraint == null || locationConstraint.isEmpty() ||
                    locationConstraint.equals("null")) {
                return "us-east-1";
            }

            return locationConstraint;
        } catch (Exception e) {
            log.warn("Failed to get location for bucket {}: {}", bucketName, e.getMessage());
            return "unknown";
        }
    }

    /**
     * Gets tags for an S3 bucket.
     */
    private Map<String, String> getBucketTags(S3Client s3Client, String bucketName) {
        Map<String, String> tags = new HashMap<>();

        try {
            GetBucketTaggingResponse taggingResponse = s3Client.getBucketTagging(
                    GetBucketTaggingRequest.builder()
                            .bucket(bucketName)
                            .build()
            );

            for (Tag tag : taggingResponse.tagSet()) {
                tags.put(tag.key(), tag.value());
            }

            log.debug("Found {} tags for bucket {}", tags.size(), bucketName);

        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.debug("No tags found for bucket {}", bucketName);
            } else {
                log.warn("Failed to get tags for bucket {}: {}", bucketName, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Failed to get tags for bucket {}: {}", bucketName, e.getMessage());
        }

        return tags;
    }

    /**
     * Gets additional metadata for an S3 bucket (versioning, encryption, etc.).
     */
    private Map<String, Object> getBucketMetadata(S3Client s3Client, String bucketName) {
        Map<String, Object> metadata = new HashMap<>();

        // Get versioning status
        try {
            GetBucketVersioningResponse versioningResponse = s3Client.getBucketVersioning(
                    GetBucketVersioningRequest.builder()
                            .bucket(bucketName)
                            .build()
            );
            metadata.put("versioning", versioningResponse.statusAsString());
        } catch (Exception e) {
            log.debug("Failed to get versioning for bucket {}: {}", bucketName, e.getMessage());
        }

        // Get encryption status
        try {
            GetBucketEncryptionResponse encryptionResponse = s3Client.getBucketEncryption(
                    GetBucketEncryptionRequest.builder()
                            .bucket(bucketName)
                            .build()
            );
            if (encryptionResponse.serverSideEncryptionConfiguration() != null) {
                metadata.put("encryption", "enabled");
            }
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                metadata.put("encryption", "disabled");
            }
        } catch (Exception e) {
            log.debug("Failed to get encryption for bucket {}: {}", bucketName, e.getMessage());
        }

        // Get public access block configuration
        try {
            GetPublicAccessBlockResponse publicAccessResponse = s3Client.getPublicAccessBlock(
                    GetPublicAccessBlockRequest.builder()
                            .bucket(bucketName)
                            .build()
            );
            PublicAccessBlockConfiguration config = publicAccessResponse.publicAccessBlockConfiguration();
            metadata.put("blockPublicAcls", config.blockPublicAcls());
            metadata.put("blockPublicPolicy", config.blockPublicPolicy());
            metadata.put("ignorePublicAcls", config.ignorePublicAcls());
            metadata.put("restrictPublicBuckets", config.restrictPublicBuckets());
        } catch (Exception e) {
            log.debug("Failed to get public access block for bucket {}: {}", bucketName, e.getMessage());
        }

        return metadata;
    }
}