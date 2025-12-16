package com.wenroe.resonant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsAccountRegion;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.repository.AwsAccountRegionRepository;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.service.aws.AwsRegionDiscoveryService;
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
@DisplayName("AwsAccountRegionService Tests")
class AwsAccountRegionServiceTest {

  @Mock
  private AwsAccountRegionRepository regionRepository;

  @Mock
  private AwsAccountRepository accountRepository;

  @Mock
  private AwsRegionDiscoveryService regionDiscoveryService;

  @InjectMocks
  private AwsAccountRegionService regionService;

  private AwsAccount testAccount;
  private User testUser;
  private UUID accountId;
  private UUID userId;

  @BeforeEach
  void setUp() {
    accountId = UUID.randomUUID();
    userId = UUID.randomUUID();

    testUser = new User();
    testUser.setId(userId);

    testAccount = new AwsAccount();
    testAccount.setId(accountId);
    testAccount.setAccountId("123456789012");
    testAccount.setUser(testUser);
  }

  @Test
  @DisplayName("Should discover and persist regions successfully")
  void shouldDiscoverAndPersistRegions() {
    // Given
    List<String> discoveredRegions = List.of("us-east-1", "us-west-2", "eu-west-1");

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionDiscoveryService.discoverEnabledRegions(testAccount)).thenReturn(discoveredRegions);

    ArgumentCaptor<List<AwsAccountRegion>> captor = ArgumentCaptor.forClass(List.class);
    when(regionRepository.saveAll(captor.capture())).thenAnswer(i -> i.getArgument(0));

    // When
    List<AwsAccountRegion> result = regionService.discoverAndPersistRegions(accountId);

    // Then
    assertThat(result).hasSize(3);
    List<AwsAccountRegion> captured = captor.getValue();
    assertThat(captured).hasSize(3);
    assertThat(captured).allMatch(r -> r.getAwsAccount().equals(testAccount));
    assertThat(captured).allMatch(AwsAccountRegion::getEnabled);
    assertThat(captured).extracting(AwsAccountRegion::getRegionCode)
        .containsExactlyInAnyOrder("us-east-1", "us-west-2", "eu-west-1");

    verify(regionDiscoveryService).discoverEnabledRegions(testAccount);
    verify(regionRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("Should throw exception when account not found")
  void shouldThrowExceptionWhenAccountNotFound() {
    // Given
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> regionService.discoverAndPersistRegions(accountId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("AWS account not found");

    verify(regionDiscoveryService, never()).discoverEnabledRegions(any());
  }

  @Test
  @DisplayName("Should enable region successfully")
  void shouldEnableRegionSuccessfully() {
    // Given
    String regionCode = "us-east-1";
    AwsAccountRegion region = new AwsAccountRegion();
    region.setAwsAccount(testAccount);
    region.setRegionCode(regionCode);
    region.setEnabled(false);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionRepository.findByAwsAccountIdAndRegionCode(accountId, regionCode))
        .thenReturn(Optional.of(region));
    when(regionRepository.save(region)).thenReturn(region);

    // When
    AwsAccountRegion result = regionService.enableRegion(accountId, regionCode, userId);

    // Then
    assertThat(result.getEnabled()).isTrue();
    verify(regionRepository).save(region);
  }

  @Test
  @DisplayName("Should disable region successfully")
  void shouldDisableRegionSuccessfully() {
    // Given
    String regionCode = "us-east-1";
    AwsAccountRegion region = new AwsAccountRegion();
    region.setAwsAccount(testAccount);
    region.setRegionCode(regionCode);
    region.setEnabled(true);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionRepository.findByAwsAccountIdAndRegionCode(accountId, regionCode))
        .thenReturn(Optional.of(region));
    when(regionRepository.save(region)).thenReturn(region);

    // When
    AwsAccountRegion result = regionService.disableRegion(accountId, regionCode, userId);

    // Then
    assertThat(result.getEnabled()).isFalse();
    verify(regionRepository).save(region);
  }

  @Test
  @DisplayName("Should throw exception when region not found")
  void shouldThrowExceptionWhenRegionNotFound() {
    // Given
    String regionCode = "us-east-1";

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionRepository.findByAwsAccountIdAndRegionCode(accountId, regionCode))
        .thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> regionService.enableRegion(accountId, regionCode, userId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Region " + regionCode + " not found");
  }

  @Test
  @DisplayName("Should throw exception when user not authorized")
  void shouldThrowExceptionWhenUserNotAuthorized() {
    // Given
    UUID wrongUserId = UUID.randomUUID();
    String regionCode = "us-east-1";

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));

    // When/Then
    assertThatThrownBy(() -> regionService.enableRegion(accountId, regionCode, wrongUserId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Not authorized");
  }

  @Test
  @DisplayName("Should update multiple regions at once")
  void shouldUpdateMultipleRegions() {
    // Given
    List<String> enabledRegionCodes = List.of("us-east-1", "eu-west-1");

    AwsAccountRegion region1 = createRegion("us-east-1", true);
    AwsAccountRegion region2 = createRegion("us-west-2", true);
    AwsAccountRegion region3 = createRegion("eu-west-1", false);

    List<AwsAccountRegion> allRegions = List.of(region1, region2, region3);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionRepository.findByAwsAccountId(accountId)).thenReturn(allRegions);
    when(regionRepository.saveAll(allRegions)).thenReturn(allRegions);

    // When
    List<AwsAccountRegion> result = regionService.updateRegions(accountId, enabledRegionCodes,
        userId);

    // Then
    assertThat(result).hasSize(3);
    assertThat(region1.getEnabled()).isTrue();  // us-east-1: stays enabled
    assertThat(region2.getEnabled()).isFalse(); // us-west-2: becomes disabled
    assertThat(region3.getEnabled()).isTrue();  // eu-west-1: becomes enabled
    verify(regionRepository).saveAll(allRegions);
  }

  @Test
  @DisplayName("Should check if account has enabled regions")
  void shouldCheckIfAccountHasEnabledRegions() {
    // Given
    when(regionRepository.countByAwsAccountIdAndEnabled(accountId, true)).thenReturn(3L);

    // When
    boolean hasEnabled = regionService.hasEnabledRegions(accountId);

    // Then
    assertThat(hasEnabled).isTrue();
  }

  @Test
  @DisplayName("Should return false when no enabled regions")
  void shouldReturnFalseWhenNoEnabledRegions() {
    // Given
    when(regionRepository.countByAwsAccountIdAndEnabled(accountId, true)).thenReturn(0L);

    // When
    boolean hasEnabled = regionService.hasEnabledRegions(accountId);

    // Then
    assertThat(hasEnabled).isFalse();
  }

  @Test
  @DisplayName("Should rediscover and add new regions only")
  void shouldRediscoverAndAddNewRegionsOnly() {
    // Given
    List<String> discoveredRegions = List.of("us-east-1", "us-west-2", "ap-south-1");

    AwsAccountRegion existingRegion1 = createRegion("us-east-1", true);
    AwsAccountRegion existingRegion2 = createRegion("us-west-2", false);
    List<AwsAccountRegion> existingRegions = List.of(existingRegion1, existingRegion2);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionDiscoveryService.discoverEnabledRegions(testAccount)).thenReturn(discoveredRegions);
    when(regionRepository.findByAwsAccountId(accountId)).thenReturn(existingRegions);

    ArgumentCaptor<List<AwsAccountRegion>> captor = ArgumentCaptor.forClass(List.class);
    when(regionRepository.saveAll(captor.capture())).thenAnswer(i -> i.getArgument(0));

    // When
    List<AwsAccountRegion> result = regionService.rediscoverRegions(accountId, userId);

    // Then
    assertThat(result).hasSize(1);
    List<AwsAccountRegion> captured = captor.getValue();
    assertThat(captured).hasSize(1);
    assertThat(captured.get(0).getRegionCode()).isEqualTo("ap-south-1");
    assertThat(captured.get(0).getEnabled()).isTrue();
  }

  @Test
  @DisplayName("Should return empty list when no new regions discovered")
  void shouldReturnEmptyListWhenNoNewRegionsDiscovered() {
    // Given
    List<String> discoveredRegions = List.of("us-east-1", "us-west-2");

    AwsAccountRegion existingRegion1 = createRegion("us-east-1", true);
    AwsAccountRegion existingRegion2 = createRegion("us-west-2", false);
    List<AwsAccountRegion> existingRegions = List.of(existingRegion1, existingRegion2);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
    when(regionDiscoveryService.discoverEnabledRegions(testAccount)).thenReturn(discoveredRegions);
    when(regionRepository.findByAwsAccountId(accountId)).thenReturn(existingRegions);

    // When
    List<AwsAccountRegion> result = regionService.rediscoverRegions(accountId, userId);

    // Then
    assertThat(result).isEmpty();
    verify(regionRepository, never()).saveAll(anyList());
  }

  private AwsAccountRegion createRegion(String regionCode, boolean enabled) {
    AwsAccountRegion region = new AwsAccountRegion();
    region.setAwsAccount(testAccount);
    region.setRegionCode(regionCode);
    region.setEnabled(enabled);
    return region;
  }
}