package com.wenroe.resonant.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsAccountRegion;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.model.enums.CredentialType;
import com.wenroe.resonant.model.enums.UserRole;
import com.wenroe.resonant.repository.AwsAccountRegionRepository;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.AwsAccountRegionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AwsAccountRegion Integration Tests")
class AwsAccountRegionIntegrationTest {

  @Autowired
  private AwsAccountRegionService regionService;

  @Autowired
  private AwsAccountRegionRepository regionRepository;

  @Autowired
  private AwsAccountRepository accountRepository;

  @Autowired
  private UserRepository userRepository;

  private User testUser;
  private AwsAccount testAccount;

  @BeforeEach
  void setUp() {
    regionRepository.deleteAll();
    accountRepository.deleteAll();
    userRepository.deleteAll();

    testUser = createTestUser();
    testAccount = createTestAccount(testUser);
  }

  @Test
  @DisplayName("Should get all regions for account")
  void shouldGetAllRegionsForAccount() {
    // Given
    createRegion(testAccount, "us-east-1", true);
    createRegion(testAccount, "us-west-2", false);
    createRegion(testAccount, "eu-west-1", true);

    // When
    List<AwsAccountRegion> regions = regionService.getRegionsByAccountId(testAccount.getId());

    // Then
    assertThat(regions).hasSize(3);
    assertThat(regions).extracting(AwsAccountRegion::getRegionCode)
        .containsExactlyInAnyOrder("us-east-1", "us-west-2", "eu-west-1");
  }

  @Test
  @DisplayName("Should get only enabled regions")
  void shouldGetOnlyEnabledRegions() {
    // Given
    createRegion(testAccount, "us-east-1", true);
    createRegion(testAccount, "us-west-2", false);
    createRegion(testAccount, "eu-west-1", true);

    // When
    List<AwsAccountRegion> enabledRegions = regionService.getEnabledRegionsByAccountId(
        testAccount.getId());

    // Then
    assertThat(enabledRegions).hasSize(2);
    assertThat(enabledRegions).extracting(AwsAccountRegion::getRegionCode)
        .containsExactlyInAnyOrder("us-east-1", "eu-west-1");
    assertThat(enabledRegions).allMatch(AwsAccountRegion::getEnabled);
  }

  @Test
  @DisplayName("Should enable region successfully")
  void shouldEnableRegionSuccessfully() {
    // Given
    AwsAccountRegion region = createRegion(testAccount, "us-east-1", false);

    // When
    AwsAccountRegion updated = regionService.enableRegion(
        testAccount.getId(), "us-east-1", testUser.getId());

    // Then
    assertThat(updated.getId()).isEqualTo(region.getId());
    assertThat(updated.getEnabled()).isTrue();

    // Verify persistence
    AwsAccountRegion persisted = regionRepository.findById(region.getId()).orElseThrow();
    assertThat(persisted.getEnabled()).isTrue();
  }

  @Test
  @DisplayName("Should disable region successfully")
  void shouldDisableRegionSuccessfully() {
    // Given
    AwsAccountRegion region = createRegion(testAccount, "us-east-1", true);

    // When
    AwsAccountRegion updated = regionService.disableRegion(
        testAccount.getId(), "us-east-1", testUser.getId());

    // Then
    assertThat(updated.getId()).isEqualTo(region.getId());
    assertThat(updated.getEnabled()).isFalse();

    // Verify persistence
    AwsAccountRegion persisted = regionRepository.findById(region.getId()).orElseThrow();
    assertThat(persisted.getEnabled()).isFalse();
  }

  @Test
  @DisplayName("Should update multiple regions at once")
  void shouldUpdateMultipleRegions() {
    // Given
    createRegion(testAccount, "us-east-1", true);
    createRegion(testAccount, "us-west-2", true);
    createRegion(testAccount, "eu-west-1", false);
    createRegion(testAccount, "ap-south-1", false);

    List<String> enabledRegionCodes = List.of("us-east-1", "eu-west-1");

    // When
    List<AwsAccountRegion> updated = regionService.updateRegions(
        testAccount.getId(), enabledRegionCodes, testUser.getId());

    // Then
    assertThat(updated).hasSize(4);

    // Verify specific regions
    List<AwsAccountRegion> allRegions = regionRepository.findByAwsAccountId(testAccount.getId());
    assertThat(allRegions.stream()
        .filter(r -> r.getRegionCode().equals("us-east-1"))
        .findFirst().orElseThrow().getEnabled()).isTrue();
    assertThat(allRegions.stream()
        .filter(r -> r.getRegionCode().equals("us-west-2"))
        .findFirst().orElseThrow().getEnabled()).isFalse();
    assertThat(allRegions.stream()
        .filter(r -> r.getRegionCode().equals("eu-west-1"))
        .findFirst().orElseThrow().getEnabled()).isTrue();
    assertThat(allRegions.stream()
        .filter(r -> r.getRegionCode().equals("ap-south-1"))
        .findFirst().orElseThrow().getEnabled()).isFalse();
  }

  @Test
  @DisplayName("Should check if account has enabled regions")
  void shouldCheckIfAccountHasEnabledRegions() {
    // Given - all regions disabled
    createRegion(testAccount, "us-east-1", false);
    createRegion(testAccount, "us-west-2", false);

    // When/Then
    assertThat(regionService.hasEnabledRegions(testAccount.getId())).isFalse();

    // Enable one region
    regionService.enableRegion(testAccount.getId(), "us-east-1", testUser.getId());

    // When/Then
    assertThat(regionService.hasEnabledRegions(testAccount.getId())).isTrue();
  }

  @Test
  @DisplayName("Should return false when no regions exist")
  void shouldReturnFalseWhenNoRegionsExist() {
    // When/Then
    assertThat(regionService.hasEnabledRegions(testAccount.getId())).isFalse();
  }

  @Test
  @DisplayName("Should throw exception when unauthorized user tries to enable region")
  void shouldThrowExceptionWhenUnauthorized() {
    // Given
    User otherUser = createTestUser();
    createRegion(testAccount, "us-east-1", false);

    // When/Then
    assertThatThrownBy(() ->
        regionService.enableRegion(testAccount.getId(), "us-east-1", otherUser.getId()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Not authorized");
  }

  @Test
  @DisplayName("Should throw exception when region not found")
  void shouldThrowExceptionWhenRegionNotFound() {
    // When/Then
    assertThatThrownBy(() ->
        regionService.enableRegion(testAccount.getId(), "nonexistent-region", testUser.getId()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Region nonexistent-region not found");
  }

  @Test
  @DisplayName("Should isolate regions between different accounts")
  void shouldIsolateRegionsBetweenAccounts() {
    // Given
    User otherUser = createTestUser();
    AwsAccount otherAccount = createTestAccount(otherUser);

    createRegion(testAccount, "us-east-1", true);
    createRegion(otherAccount, "us-east-1", false);
    createRegion(otherAccount, "us-west-2", true);

    // When
    List<AwsAccountRegion> account1Regions = regionService.getRegionsByAccountId(
        testAccount.getId());
    List<AwsAccountRegion> account2Regions = regionService.getRegionsByAccountId(
        otherAccount.getId());

    // Then
    assertThat(account1Regions).hasSize(1);
    assertThat(account2Regions).hasSize(2);

    assertThat(account1Regions.get(0).getRegionCode()).isEqualTo("us-east-1");
    assertThat(account1Regions.get(0).getEnabled()).isTrue();
  }

  @Test
  @DisplayName("Should handle updating all regions to disabled")
  void shouldHandleUpdatingAllRegionsToDisabled() {
    // Given
    createRegion(testAccount, "us-east-1", true);
    createRegion(testAccount, "us-west-2", true);

    // When
    regionService.updateRegions(testAccount.getId(), List.of(), testUser.getId());

    // Then
    List<AwsAccountRegion> regions = regionRepository.findByAwsAccountId(testAccount.getId());
    assertThat(regions).allMatch(r -> !r.getEnabled());
    assertThat(regionService.hasEnabledRegions(testAccount.getId())).isFalse();
  }

  @Test
  @DisplayName("Should handle updating all regions to enabled")
  void shouldHandleUpdatingAllRegionsToEnabled() {
    // Given
    createRegion(testAccount, "us-east-1", false);
    createRegion(testAccount, "us-west-2", false);
    createRegion(testAccount, "eu-west-1", false);

    List<String> allRegionCodes = List.of("us-east-1", "us-west-2", "eu-west-1");

    // When
    regionService.updateRegions(testAccount.getId(), allRegionCodes, testUser.getId());

    // Then
    List<AwsAccountRegion> regions = regionRepository.findByAwsAccountId(testAccount.getId());
    assertThat(regions).allMatch(AwsAccountRegion::getEnabled);
    assertThat(regionService.hasEnabledRegions(testAccount.getId())).isTrue();
  }

  private User createTestUser() {
    User user = new User();
    user.setEmail(UUID.randomUUID() + "@test.com");
    user.setName("Test User");
    user.setPasswordHash("hashed");
    user.setRole(UserRole.USER);
    user.setEnabled(true);
    return userRepository.save(user);
  }

  private AwsAccount createTestAccount(User user) {
    AwsAccount account = new AwsAccount();
    account.setUser(user);
    account.setAccountId(UUID.randomUUID().toString().substring(0, 12));
    account.setAccountAlias("Test Account");
    account.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
    account.setExternalId(UUID.randomUUID().toString());
    account.setCredentialType(CredentialType.ROLE);
    account.setStatus(AwsAccountStatus.ACTIVE);
    return accountRepository.save(account);
  }

  private AwsAccountRegion createRegion(AwsAccount account, String regionCode, boolean enabled) {
    AwsAccountRegion region = new AwsAccountRegion();
    region.setAwsAccount(account);
    region.setRegionCode(regionCode);
    region.setEnabled(enabled);
    return regionRepository.save(region);
  }
}