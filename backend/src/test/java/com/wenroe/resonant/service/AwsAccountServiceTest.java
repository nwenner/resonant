package com.wenroe.resonant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.model.enums.CredentialType;
import com.wenroe.resonant.model.enums.UserRole;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.aws.AwsConnectionTester;
import com.wenroe.resonant.service.security.CredentialEncryptionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    testUser.setRole(UserRole.USER);
    testUser.setEnabled(true);

    // Setup test AWS account
    testAccount = new AwsAccount();
    testAccount.setId(accountId);
    testAccount.setUser(testUser);
    testAccount.setAccountId("123456789012");
    testAccount.setAccountAlias("Test Account");
    testAccount.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
    testAccount.setExternalId("external-id-12345");
    testAccount.setCredentialType(CredentialType.ROLE);
    testAccount.setStatus(AwsAccountStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should create AWS account with IAM role and extract account ID from ARN")
  void createAccountWithRole_Success() {
    AwsAccount savedAccount = new AwsAccount();
    savedAccount.setId(accountId);
    savedAccount.setUser(testUser);
    savedAccount.setAccountId("123456789012");
    savedAccount.setAccountAlias("Test Account");
    savedAccount.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
    savedAccount.setExternalId("external-id-12345");
    savedAccount.setCredentialType(CredentialType.ROLE);
    savedAccount.setStatus(AwsAccountStatus.TESTING);

    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    when(awsAccountRepository.findByUserIdAndAccountId(userId, "123456789012"))
        .thenReturn(Optional.empty());
    when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(savedAccount);

    AwsAccount result = awsAccountService.createAccountWithRole(
        userId,
        "Test Account",
        "arn:aws:iam::123456789012:role/TestRole",
        "external-id-12345"
    );

    assertThat(result).isNotNull();
    assertThat(result.getAccountId()).isEqualTo("123456789012");
    assertThat(result.getCredentialType()).isEqualTo(CredentialType.ROLE);

    verify(userRepository).findById(userId);
    verify(awsAccountRepository).findByUserIdAndAccountId(userId, "123456789012");
    verify(awsAccountRepository).save(any(AwsAccount.class));
  }

  @Test
  @DisplayName("Should set initial status to TESTING when creating account")
  void createAccountWithRole_InitialStatus() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    when(awsAccountRepository.findByUserIdAndAccountId(any(), any())).thenReturn(Optional.empty());
    when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

    awsAccountService.createAccountWithRole(
        userId, "Test", "arn:aws:iam::123456789012:role/Role", "ext-id"
    );

    ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
    verify(awsAccountRepository).save(accountCaptor.capture());

    AwsAccount savedAccount = accountCaptor.getValue();
    assertThat(savedAccount.getStatus()).isEqualTo(AwsAccountStatus.TESTING);
  }

  @Test
  @DisplayName("Should extract account ID from valid Role ARN")
  void createAccountWithRole_ExtractsAccountId() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    when(awsAccountRepository.findByUserIdAndAccountId(any(), any())).thenReturn(Optional.empty());
    when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

    awsAccountService.createAccountWithRole(
        userId, "Test", "arn:aws:iam::987654321098:role/MyRole", "ext-id"
    );

    ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
    verify(awsAccountRepository).save(accountCaptor.capture());

    AwsAccount savedAccount = accountCaptor.getValue();
    assertThat(savedAccount.getAccountId()).isEqualTo("987654321098");
  }

  @Test
  @DisplayName("Should throw exception for invalid Role ARN format")
  void createAccountWithRole_InvalidArnFormat() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> awsAccountService.createAccountWithRole(
        userId, "Test", "invalid-arn", "ext-id"
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid Role ARN format");

    verify(awsAccountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception for empty Role ARN")
  void createAccountWithRole_EmptyArn() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> awsAccountService.createAccountWithRole(
        userId, "Test", "", "ext-id"
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role ARN cannot be empty");

    verify(awsAccountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception for Role ARN with non-12-digit account ID")
  void createAccountWithRole_InvalidAccountIdLength() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

    assertThatThrownBy(() -> awsAccountService.createAccountWithRole(
        userId, "Test", "arn:aws:iam::12345:role/Role", "ext-id"
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid AWS account ID");

    verify(awsAccountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when user not found")
  void createAccountWithRole_UserNotFound() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> awsAccountService.createAccountWithRole(
        userId, "Test", "arn:aws:iam::123456789012:role/Role", "ext-id"
    ))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("User not found");

    verify(awsAccountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should throw exception when duplicate account for user")
  void createAccountWithRole_DuplicateAccount() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    when(awsAccountRepository.findByUserIdAndAccountId(userId, "123456789012"))
        .thenReturn(Optional.of(testAccount));

    assertThatThrownBy(() -> awsAccountService.createAccountWithRole(
        userId, "Test", "arn:aws:iam::123456789012:role/Role", "ext-id"
    ))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("AWS account 123456789012 already connected");

    verify(awsAccountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should create AWS account with access keys")
  void createAccountWithAccessKeys_Success() {
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

    AwsAccount result = awsAccountService.createAccountWithAccessKeys(
        userId,
        "123456789012",
        "Test Account",
        accessKey,
        secretKey
    );

    assertThat(result).isNotNull();
    verify(encryptionService).encrypt(accessKey);
    verify(encryptionService).encrypt(secretKey);

    ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
    verify(awsAccountRepository).save(accountCaptor.capture());

    AwsAccount savedAccount = accountCaptor.getValue();
    assertThat(savedAccount.getAccessKeyEncrypted()).isEqualTo(encryptedAccess);
    assertThat(savedAccount.getSecretKeyEncrypted()).isEqualTo(encryptedSecret);
    assertThat(savedAccount.getCredentialType()).isEqualTo(CredentialType.ACCESS_KEY);
  }

  @Test
  @DisplayName("Should generate UUID-based external ID")
  void generateExternalId_Success() {
    String externalId = awsAccountService.generateExternalId();

    assertThat(externalId).isNotNull();
    assertThat(externalId).isNotEmpty();
    assertThat(UUID.fromString(externalId)).isNotNull();
  }

  @Test
  @DisplayName("Should generate unique external IDs")
  void generateExternalId_Unique() {
    String id1 = awsAccountService.generateExternalId();
    String id2 = awsAccountService.generateExternalId();

    assertThat(id1).isNotEqualTo(id2);
  }

  @Test
  @DisplayName("Should test connection and update status to ACTIVE on success")
  void testConnection_Success() {
    AwsConnectionTester.ConnectionTestResult successResult =
        new AwsConnectionTester.ConnectionTestResult();
    successResult.setSuccess(true);
    successResult.setMessage("Connection successful");

    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(connectionTester.testConnection(testAccount)).thenReturn(successResult);
    when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

    AwsConnectionTester.ConnectionTestResult result =
        awsAccountService.testConnection(accountId, userId);

    assertThat(result.isSuccess()).isTrue();

    ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
    verify(awsAccountRepository).save(accountCaptor.capture());
    assertThat(accountCaptor.getValue().getStatus()).isEqualTo(AwsAccountStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should test connection and update status to INVALID on failure")
  void testConnection_Failure() {
    AwsConnectionTester.ConnectionTestResult failureResult =
        new AwsConnectionTester.ConnectionTestResult();
    failureResult.setSuccess(false);
    failureResult.setErrorMessage("Invalid credentials");

    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(connectionTester.testConnection(testAccount)).thenReturn(failureResult);
    when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

    AwsConnectionTester.ConnectionTestResult result =
        awsAccountService.testConnection(accountId, userId);

    assertThat(result.isSuccess()).isFalse();

    ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
    verify(awsAccountRepository).save(accountCaptor.capture());
    assertThat(accountCaptor.getValue().getStatus()).isEqualTo(AwsAccountStatus.INVALID);
  }

  @Test
  @DisplayName("Should throw exception when testing connection for non-owned account")
  void testConnection_NotOwner() {
    UUID otherUserId = UUID.randomUUID();
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    assertThatThrownBy(() -> awsAccountService.testConnection(accountId, otherUserId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Not authorized to access this AWS account");

    verify(connectionTester, never()).testConnection(any());
    verify(awsAccountRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should get accounts by user ID")
  void getAccountsByUserId_Success() {
    when(awsAccountRepository.findByUserId(userId)).thenReturn(List.of(testAccount));

    List<AwsAccount> accounts = awsAccountService.getAccountsByUserId(userId);

    assertThat(accounts).hasSize(1);
    assertThat(accounts.getFirst()).isEqualTo(testAccount);
    verify(awsAccountRepository).findByUserId(userId);
  }

  @Test
  @DisplayName("Should get account by ID with ownership verification")
  void getAccountByIdAndVerifyOwnership_Success() {
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    AwsAccount account = awsAccountService.getAccountByIdAndVerifyOwnership(accountId, userId);

    assertThat(account).isEqualTo(testAccount);
    verify(awsAccountRepository).findById(accountId);
  }

  @Test
  @DisplayName("Should throw exception when accessing non-owned account")
  void getAccountByIdAndVerifyOwnership_NotOwner() {
    UUID otherUserId = UUID.randomUUID();
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    assertThatThrownBy(
        () -> awsAccountService.getAccountByIdAndVerifyOwnership(accountId, otherUserId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Not authorized to access this AWS account");
  }

  @Test
  @DisplayName("Should get account by ID without ownership verification")
  void getAccountById_Success() {
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    AwsAccount account = awsAccountService.getAccountById(accountId);

    assertThat(account).isEqualTo(testAccount);
    verify(awsAccountRepository).findById(accountId);
  }

  @Test
  @DisplayName("Should throw exception when account not found")
  void getAccountById_NotFound() {
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> awsAccountService.getAccountById(accountId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("AWS account not found");
  }

  @Test
  @DisplayName("Should update last scan time")
  void updateLastScanTime_Success() {
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

    awsAccountService.updateLastScanTime(accountId);

    ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
    verify(awsAccountRepository).save(accountCaptor.capture());
    assertThat(accountCaptor.getValue().getLastScanAt()).isNotNull();
  }

  @Test
  @DisplayName("Should delete account with ownership verification")
  void deleteAccount_Success() {
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    doNothing().when(awsAccountRepository).delete(testAccount);

    awsAccountService.deleteAccount(accountId, userId);

    verify(awsAccountRepository).delete(testAccount);
  }

  @Test
  @DisplayName("Should throw exception when deleting non-owned account")
  void deleteAccount_NotOwner() {
    UUID otherUserId = UUID.randomUUID();
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    assertThatThrownBy(() -> awsAccountService.deleteAccount(accountId, otherUserId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Not authorized to access this AWS account");

    verify(awsAccountRepository, never()).delete(any());
  }

  @Test
  @DisplayName("Should update account alias with ownership verification")
  void updateAccountAlias_Success() {
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(awsAccountRepository.save(any(AwsAccount.class))).thenReturn(testAccount);

    awsAccountService.updateAccountAlias(accountId, userId, "New Alias");

    ArgumentCaptor<AwsAccount> accountCaptor = ArgumentCaptor.forClass(AwsAccount.class);
    verify(awsAccountRepository).save(accountCaptor.capture());
    assertThat(accountCaptor.getValue().getAccountAlias()).isEqualTo("New Alias");
  }

  @Test
  @DisplayName("Should throw exception when updating alias for non-owned account")
  void updateAccountAlias_NotOwner() {
    UUID otherUserId = UUID.randomUUID();
    when(awsAccountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    assertThatThrownBy(
        () -> awsAccountService.updateAccountAlias(accountId, otherUserId, "New Alias"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Not authorized to access this AWS account");

    verify(awsAccountRepository, never()).save(any());
  }
}