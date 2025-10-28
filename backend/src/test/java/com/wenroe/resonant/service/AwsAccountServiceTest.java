package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.aws.AwsConnectionTester;
import com.wenroe.resonant.service.security.CredentialEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsAccountService Tests")
class AwsAccountServiceTest {

    @Mock
    private AwsAccountRepository awsAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CredentialEncryptionService encryptionService;

    @Mock
    private AwsConnectionTester connectionTester;

    @InjectMocks
    private AwsAccountService awsAccountService;

    private User testUser;
    private AwsAccount testAccount;
    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        // Setup test user
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);

        // Setup test AWS account
        testAccount = new AwsAccount();
        testAccount.setId(accountId);
        testAccount.setUser(testUser);
        testAccount.setAccountId("123456789012");
        testAccount.setAccountAlias("Test Account");
        testAccount.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
        testAccount.setExternalId("external-id-12345");
        testAccount.setCredentialType(AwsAccount.CredentialType.ROLE);
        testAccount.setStatus(AwsAccount.Status.ACTIVE);
    }

    @Test
    @DisplayName("Should create AWS account with IAM role")
    void createAccountWithRole_Success() {
        // Given
        AwsAccount savedAccount = new AwsAccount();
        savedAccount.setId(accountId);
        savedAccount.setUser(testUser);
        savedAccount.setAccountId("123456789012");
        savedAccount.setAccountAlias("Test Account");
        savedAccount.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
        savedAccount.setExternalId("external-id-12345");
        savedAccount.setCredentialType(AwsAccount.CredentialType.ROLE);
        savedAccount.setStatus(AwsAccount.Status.TESTING); // What service actually sets

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(awsAccountRepository.findByUserIdAndAccountId(userId, "123456789012"))
                .thenReturn(Optional.empty());
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(savedAccount);

        // When
        AwsAccount result = awsAccountService.createAccountWithRole(
                userId,
                "123456789012",
                "Test Account",
                "arn:aws:iam::123456789012:role/TestRole",
                "external-id-12345"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo("123456789012");
        assertThat(result.getCredentialType()).isEqualTo(AwsAccount.CredentialType.ROLE);
        // Don't assert status here - it's tested in the next test

        verify(userRepository).findById(userId);
        verify(awsAccountRepository).findByUserIdAndAccountId(userId, "123456789012");
        verify(awsAccountRepository).save(any(AwsAccount.class));
    }

    @Test
    @DisplayName("Should set initial status to TESTING when creating account")
    void createAccountWithRole_InitialStatus() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(awsAccountRepository.findByUserIdAndAccountId(any(), any())).thenReturn(Optional.empty());
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

        // When
        awsAccountService.createAccountWithRole(
                userId, "123456789012", "Test", "arn:aws:iam::123456789012:role/Role", "ext-id"
        );

        // Then - Verify status is TESTING
        ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
        verify(awsAccountRepository).save(accountCaptor.capture());

        AwsAccount savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getStatus()).isEqualTo(AwsAccount.Status.TESTING);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void createAccountWithRole_UserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> awsAccountService.createAccountWithRole(
                userId, "123456789012", "Test", "arn", "ext-id"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(awsAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when duplicate account for user")
    void createAccountWithRole_DuplicateAccount() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(awsAccountRepository.findByUserIdAndAccountId(userId, "123456789012"))
                .thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> awsAccountService.createAccountWithRole(
                userId, "123456789012", "Test", "arn", "ext-id"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AWS account 123456789012 already connected");

        verify(awsAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create AWS account with access keys")
    void createAccountWithAccessKeys_Success() {
        // Given
        String accessKey = "AKIAIOSFODNN7EXAMPLE";
        String secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
        String encryptedAccess = "encrypted-access-key";
        String encryptedSecret = "encrypted-secret-key";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(awsAccountRepository.findByUserIdAndAccountId(userId, "123456789012"))
                .thenReturn(Optional.empty());
        when(encryptionService.encrypt(accessKey)).thenReturn(encryptedAccess);
        when(encryptionService.encrypt(secretKey)).thenReturn(encryptedSecret);
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

        // When
        AwsAccount result = awsAccountService.createAccountWithAccessKeys(
                userId,
                "123456789012",
                "Test Account",
                accessKey,
                secretKey
        );

        // Then
        assertThat(result).isNotNull();
        verify(encryptionService).encrypt(accessKey);
        verify(encryptionService).encrypt(secretKey);

        // Verify encrypted keys are stored
        ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
        verify(awsAccountRepository).save(accountCaptor.capture());

        AwsAccount savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getAccessKeyEncrypted()).isEqualTo(encryptedAccess);
        assertThat(savedAccount.getSecretKeyEncrypted()).isEqualTo(encryptedSecret);
        assertThat(savedAccount.getCredentialType()).isEqualTo(AwsAccount.CredentialType.ACCESS_KEY);
    }

    @Test
    @DisplayName("Should encrypt credentials before storing")
    void createAccountWithAccessKeys_EncryptsCredentials() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(awsAccountRepository.findByUserIdAndAccountId(any(), any())).thenReturn(Optional.empty());
        when(encryptionService.encrypt("plain-access")).thenReturn("encrypted-access");
        when(encryptionService.encrypt("plain-secret")).thenReturn("encrypted-secret");
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

        // When
        awsAccountService.createAccountWithAccessKeys(
                userId, "123456789012", "Test", "plain-access", "plain-secret"
        );

        // Then
        verify(encryptionService).encrypt("plain-access");
        verify(encryptionService).encrypt("plain-secret");
    }

    @Test
    @DisplayName("Should generate valid external ID")
    void generateExternalId_Success() {
        // When
        String externalId = awsAccountService.generateExternalId();

        // Then
        assertThat(externalId).isNotNull();
        assertThat(externalId).isNotEmpty();
        assertThat(UUID.fromString(externalId)).isNotNull(); // Should be valid UUID
    }

    @Test
    @DisplayName("Should generate unique external IDs")
    void generateExternalId_Unique() {
        // When
        String id1 = awsAccountService.generateExternalId();
        String id2 = awsAccountService.generateExternalId();

        // Then
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("Should test connection and update status to ACTIVE on success")
    void testConnection_Success() {
        // Given
        AwsConnectionTester.ConnectionTestResult successResult =
                new AwsConnectionTester.ConnectionTestResult();
        successResult.setSuccess(true);
        successResult.setMessage("Connection successful");

        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(connectionTester.testConnection(testAccount)).thenReturn(successResult);
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

        // When
        AwsConnectionTester.ConnectionTestResult result =
                awsAccountService.testConnection(accountId);

        // Then
        assertThat(result.isSuccess()).isTrue();

        // Verify status updated to ACTIVE
        ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
        verify(awsAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getStatus()).isEqualTo(AwsAccount.Status.ACTIVE);
    }

    @Test
    @DisplayName("Should test connection and update status to INVALID on failure")
    void testConnection_Failure() {
        // Given
        AwsConnectionTester.ConnectionTestResult failureResult =
                new AwsConnectionTester.ConnectionTestResult();
        failureResult.setSuccess(false);
        failureResult.setErrorMessage("Invalid credentials");

        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(connectionTester.testConnection(testAccount)).thenReturn(failureResult);
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

        // When
        AwsConnectionTester.ConnectionTestResult result =
                awsAccountService.testConnection(accountId);

        // Then
        assertThat(result.isSuccess()).isFalse();

        // Verify status updated to INVALID
        ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
        verify(awsAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getStatus()).isEqualTo(AwsAccount.Status.INVALID);
    }

    @Test
    @DisplayName("Should get accounts by user ID")
    void getAccountsByUserId_Success() {
        // Given
        when(awsAccountRepository.findByUserId(userId)).thenReturn(List.of(testAccount));

        // When
        List<AwsAccount> accounts = awsAccountService.getAccountsByUserId(userId);

        // Then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0)).isEqualTo(testAccount);
        verify(awsAccountRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should get account by ID")
    void getAccountById_Success() {
        // Given
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When
        AwsAccount account = awsAccountService.getAccountById(accountId);

        // Then
        assertThat(account).isEqualTo(testAccount);
        verify(awsAccountRepository).findById(accountId);
    }

    @Test
    @DisplayName("Should throw exception when account not found")
    void getAccountById_NotFound() {
        // Given
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> awsAccountService.getAccountById(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AWS account not found");
    }

    @Test
    @DisplayName("Should update last scan time")
    void updateLastScanTime_Success() {
        // Given
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

        // When
        awsAccountService.updateLastScanTime(accountId);

        // Then
        ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
        verify(awsAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getLastScanAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete account with ownership verification")
    void deleteAccount_Success() {
        // Given
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        doNothing().when(awsAccountRepository).delete(testAccount);

        // When
        awsAccountService.deleteAccount(accountId, userId);

        // Then
        verify(awsAccountRepository).delete(testAccount);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-owned account")
    void deleteAccount_NotOwner() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> awsAccountService.deleteAccount(accountId, otherUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized to delete this AWS account");

        verify(awsAccountRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should update account alias")
    void updateAccountAlias_Success() {
        // Given
        when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

        // When
        AwsAccount result = awsAccountService.updateAccountAlias(accountId, "New Alias");

        // Then
        ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
        verify(awsAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getAccountAlias()).isEqualTo("New Alias");
    }
}