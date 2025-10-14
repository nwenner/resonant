package com.wenroe.resonant.service.aws;

import com.wenroe.resonant.model.entity.AwsAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to test AWS account connections and validate credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsConnectionTester {

    private final AwsClientFactory clientFactory;

    /**
     * Tests the connection to an AWS account and returns connection details.
     * This validates that:
     * 1. The role can be assumed (or access keys are valid)
     * 2. Basic API calls work (STS GetCallerIdentity)
     * 3. EC2 DescribeRegions works (required for scanning)
     */
    public ConnectionTestResult testConnection(AwsAccount account) {
        ConnectionTestResult result = new ConnectionTestResult();
        result.setSuccess(false);

        try {
            // Step 1: Test STS GetCallerIdentity
            log.info("Testing connection for AWS account: {}", account.getAccountId());

            try (StsClient stsClient = clientFactory.createStsClient()) {
                // Assume the role first to get credentials
                var credentials = clientFactory.assumeRole(account);

                // Create a new STS client with the assumed role credentials
                try (StsClient assumedStsClient = StsClient.builder()
                        .credentialsProvider(() -> credentials)
                        .build()) {

                    GetCallerIdentityResponse identity = assumedStsClient.getCallerIdentity(
                            GetCallerIdentityRequest.builder().build()
                    );

                    result.setAccountId(identity.account());
                    result.setAssumedRoleArn(identity.arn());
                    result.setUserId(identity.userId());

                    log.info("Successfully validated AWS identity: Account={}, ARN={}",
                            identity.account(), identity.arn());
                }
            }

            // Step 2: Test EC2 DescribeRegions (ensures we can scan)
            try (Ec2Client ec2Client = clientFactory.createEc2ClientForRegionDiscovery(account)) {
                DescribeRegionsResponse regions = ec2Client.describeRegions(
                        DescribeRegionsRequest.builder().allRegions(false).build()
                );

                result.setAvailableRegionCount(regions.regions().size());
                log.info("Successfully listed {} AWS regions", regions.regions().size());
            }

            // Step 3: Validate account ID matches
            if (!result.getAccountId().equals(account.getAccountId())) {
                result.setSuccess(false);
                result.setErrorMessage("Account ID mismatch. Expected: " + account.getAccountId()
                        + ", Got: " + result.getAccountId());
                log.warn("Account ID mismatch for account {}", account.getAccountId());
                return result;
            }

            result.setSuccess(true);
            result.setMessage("Connection successful! Ready to scan AWS resources.");
            log.info("Connection test successful for account: {}", account.getAccountId());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Connection test failed for account {}: {}", account.getAccountId(), e.getMessage(), e);
        }

        return result;
    }

    /**
     * Result object for connection tests.
     */
    public static class ConnectionTestResult {
        private boolean success;
        private String message;
        private String errorMessage;
        private String accountId;
        private String assumedRoleArn;
        private String userId;
        private Integer availableRegionCount;
        private Map<String, Object> metadata = new HashMap<>();

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getAssumedRoleArn() {
            return assumedRoleArn;
        }

        public void setAssumedRoleArn(String assumedRoleArn) {
            this.assumedRoleArn = assumedRoleArn;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Integer getAvailableRegionCount() {
            return availableRegionCount;
        }

        public void setAvailableRegionCount(Integer availableRegionCount) {
            this.availableRegionCount = availableRegionCount;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}