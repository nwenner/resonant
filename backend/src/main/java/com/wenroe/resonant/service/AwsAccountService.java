package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.model.enums.CredentialType;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.aws.AwsConnectionTester;
import com.wenroe.resonant.service.security.CredentialEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing AWS account connections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsAccountService {

    private final AwsAccountRepository awsAccountRepository;
    private final UserRepository userRepository;
    private final CredentialEncryptionService encryptionService;
    private final AwsConnectionTester connectionTester;

    /**
     * Creates a new AWS account connection with IAM role.
     */
    @Transactional
    public AwsAccount createAccountWithRole(UUID userId, String accountId, String accountAlias,
                                            String roleArn, String externalId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if account already exists for this user
        if (awsAccountRepository.findByUserIdAndAccountId(userId, accountId).isPresent()) {
            throw new RuntimeException("AWS account " + accountId + " already connected");
        }

        AwsAccount account = new AwsAccount();
        account.setUser(user);
        account.setAccountId(accountId);
        account.setAccountAlias(accountAlias);
        account.setRoleArn(roleArn);
        account.setExternalId(externalId);
        account.setCredentialType(CredentialType.ROLE);
        account.setStatus(AwsAccountStatus.TESTING);

        AwsAccount saved = awsAccountRepository.save(account);
        log.info("Created AWS account connection for user {} with account {}", userId, accountId);

        return saved;
    }

    /**
     * Creates a new AWS account connection with access keys (encrypted).
     */
    @Transactional
    public AwsAccount createAccountWithAccessKeys(UUID userId, String accountId, String accountAlias,
                                                  String accessKey, String secretKey) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if account already exists for this user
        if (awsAccountRepository.findByUserIdAndAccountId(userId, accountId).isPresent()) {
            throw new RuntimeException("AWS account " + accountId + " already connected");
        }

        // Encrypt credentials
        String encryptedAccessKey = encryptionService.encrypt(accessKey);
        String encryptedSecretKey = encryptionService.encrypt(secretKey);

        AwsAccount account = new AwsAccount();
        account.setUser(user);
        account.setAccountId(accountId);
        account.setAccountAlias(accountAlias);
        account.setAccessKeyEncrypted(encryptedAccessKey);
        account.setSecretKeyEncrypted(encryptedSecretKey);
        account.setCredentialType(CredentialType.ACCESS_KEY);
        account.setStatus(AwsAccountStatus.TESTING);

        AwsAccount saved = awsAccountRepository.save(account);
        log.info("Created AWS account connection with access keys for user {} with account {}", userId, accountId);

        return saved;
    }

    /**
     * Generates a unique external ID for IAM role assumption.
     */
    public String generateExternalId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Tests the connection to an AWS account and updates status.
     */
    @Transactional
    public AwsConnectionTester.ConnectionTestResult testConnection(UUID accountId, UUID userId) {
        AwsAccount account = getAccountByIdAndVerifyOwnership(accountId, userId);

        log.info("Testing connection for AWS account: {} (user: {})", account.getAccountId(), userId);

        AwsConnectionTester.ConnectionTestResult result = connectionTester.testConnection(account);

        if (result.isSuccess()) {
            account.setStatus(AwsAccountStatus.ACTIVE);
            log.info("AWS account {} is now ACTIVE", account.getAccountId());
        } else {
            account.setStatus(AwsAccountStatus.INVALID);
            log.warn("AWS account {} connection failed: {}", account.getAccountId(), result.getErrorMessage());
        }

        awsAccountRepository.save(account);
        return result;
    }

    /**
     * Gets all AWS accounts for a user.
     */
    public List<AwsAccount> getAccountsByUserId(UUID userId) {
        return awsAccountRepository.findByUserId(userId);
    }

    /**
     * Gets a specific AWS account by ID and verifies user ownership.
     */
    public AwsAccount getAccountByIdAndVerifyOwnership(UUID accountId, UUID userId) {
        AwsAccount account = awsAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("AWS account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to access this AWS account");
        }

        return account;
    }

    /**
     * Gets a specific AWS account by ID (no ownership check).
     */
    public AwsAccount getAccountById(UUID accountId) {
        return awsAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("AWS account not found"));
    }

    /**
     * Updates the last scan timestamp for an account.
     */
    @Transactional
    public void updateLastScanTime(UUID accountId) {
        AwsAccount account = getAccountById(accountId);
        account.setLastScanAt(LocalDateTime.now());
        awsAccountRepository.save(account);
    }

    /**
     * Deletes an AWS account connection.
     */
    @Transactional
    public void deleteAccount(UUID accountId, UUID userId) {
        AwsAccount account = getAccountByIdAndVerifyOwnership(accountId, userId);
        awsAccountRepository.delete(account);
        log.info("Deleted AWS account {} for user {}", account.getAccountId(), userId);
    }

    /**
     * Updates account alias.
     */
    @Transactional
    public AwsAccount updateAccountAlias(UUID accountId, UUID userId, String newAlias) {
        AwsAccount account = getAccountByIdAndVerifyOwnership(accountId, userId);
        account.setAccountAlias(newAlias);
        log.info("Updated alias for AWS account {} to '{}' (user: {})", account.getAccountId(), newAlias, userId);
        return awsAccountRepository.save(account);
    }
}