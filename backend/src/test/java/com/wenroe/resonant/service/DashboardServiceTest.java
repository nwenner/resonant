package com.wenroe.resonant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.dto.compliance.ComplianceRateResponse;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.repository.ComplianceViolationRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

  @Mock
  private AwsResourceRepository awsResourceRepository;

  @Mock
  private ComplianceViolationRepository complianceViolationRepository;

  @InjectMocks
  private DashboardService dashboardService;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Test
  @DisplayName("Should calculate 100% compliance when no resources exist")
  void getComplianceRate_NoResources() {
    // Given
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(0L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        0L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(0L);
    assertThat(response.getCompliantResources()).isEqualTo(0L);
    assertThat(response.getNonCompliantResources()).isEqualTo(0L);
    assertThat(response.getComplianceRate()).isEqualTo(100.0);

    verify(awsResourceRepository).countByAwsAccount_User_Id(userId);
    verify(complianceViolationRepository).countDistinctViolatedResourcesByUserId(userId);
  }

  @Test
  @DisplayName("Should calculate 100% compliance when all resources are compliant")
  void getComplianceRate_AllCompliant() {
    // Given
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(10L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        0L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(10L);
    assertThat(response.getCompliantResources()).isEqualTo(10L);
    assertThat(response.getNonCompliantResources()).isEqualTo(0L);
    assertThat(response.getComplianceRate()).isEqualTo(100.0);

    verify(awsResourceRepository).countByAwsAccount_User_Id(userId);
    verify(complianceViolationRepository).countDistinctViolatedResourcesByUserId(userId);
  }

  @Test
  @DisplayName("Should calculate 0% compliance when all resources are non-compliant")
  void getComplianceRate_AllNonCompliant() {
    // Given
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(10L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        10L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(10L);
    assertThat(response.getCompliantResources()).isEqualTo(0L);
    assertThat(response.getNonCompliantResources()).isEqualTo(10L);
    assertThat(response.getComplianceRate()).isEqualTo(0.0);

    verify(awsResourceRepository).countByAwsAccount_User_Id(userId);
    verify(complianceViolationRepository).countDistinctViolatedResourcesByUserId(userId);
  }

  @Test
  @DisplayName("Should calculate correct compliance rate with mixed resources")
  void getComplianceRate_MixedCompliance() {
    // Given - 15 total resources, 3 non-compliant
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(15L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        3L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(15L);
    assertThat(response.getCompliantResources()).isEqualTo(12L);
    assertThat(response.getNonCompliantResources()).isEqualTo(3L);
    assertThat(response.getComplianceRate()).isEqualTo(80.0); // 12/15 = 80%

    verify(awsResourceRepository).countByAwsAccount_User_Id(userId);
    verify(complianceViolationRepository).countDistinctViolatedResourcesByUserId(userId);
  }

  @Test
  @DisplayName("Should calculate compliance rate with decimal precision")
  void getComplianceRate_DecimalPrecision() {
    // Given - 100 total resources, 15 non-compliant
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(100L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        15L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(100L);
    assertThat(response.getCompliantResources()).isEqualTo(85L);
    assertThat(response.getNonCompliantResources()).isEqualTo(15L);
    assertThat(response.getComplianceRate()).isEqualTo(85.0); // 85/100 = 85%

    verify(awsResourceRepository).countByAwsAccount_User_Id(userId);
    verify(complianceViolationRepository).countDistinctViolatedResourcesByUserId(userId);
  }

  @Test
  @DisplayName("Should handle single resource scenarios")
  void getComplianceRate_SingleResource() {
    // Given - 1 total resource, 0 non-compliant
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(1L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        0L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(1L);
    assertThat(response.getCompliantResources()).isEqualTo(1L);
    assertThat(response.getNonCompliantResources()).isEqualTo(0L);
    assertThat(response.getComplianceRate()).isEqualTo(100.0);
  }

  @Test
  @DisplayName("Should handle single non-compliant resource")
  void getComplianceRate_SingleNonCompliantResource() {
    // Given - 1 total resource, 1 non-compliant
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(1L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        1L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(1L);
    assertThat(response.getCompliantResources()).isEqualTo(0L);
    assertThat(response.getNonCompliantResources()).isEqualTo(1L);
    assertThat(response.getComplianceRate()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("Should calculate rate for large number of resources")
  void getComplianceRate_LargeDataset() {
    // Given - 10000 total resources, 250 non-compliant
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(10000L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        250L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(10000L);
    assertThat(response.getCompliantResources()).isEqualTo(9750L);
    assertThat(response.getNonCompliantResources()).isEqualTo(250L);
    assertThat(response.getComplianceRate()).isEqualTo(97.5); // 9750/10000 = 97.5%
  }

  @Test
  @DisplayName("Should handle exact 50% compliance rate")
  void getComplianceRate_ExactlyHalf() {
    // Given - 100 total resources, 50 non-compliant
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(100L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        50L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(100L);
    assertThat(response.getCompliantResources()).isEqualTo(50L);
    assertThat(response.getNonCompliantResources()).isEqualTo(50L);
    assertThat(response.getComplianceRate()).isEqualTo(50.0);
  }

  @Test
  @DisplayName("Should handle odd number of resources")
  void getComplianceRate_OddNumberOfResources() {
    // Given - 7 total resources, 3 non-compliant
    when(awsResourceRepository.countByAwsAccount_User_Id(userId)).thenReturn(7L);
    when(complianceViolationRepository.countDistinctViolatedResourcesByUserId(userId)).thenReturn(
        3L);

    // When
    ComplianceRateResponse response = dashboardService.getComplianceRate(userId);

    // Then
    assertThat(response.getTotalResources()).isEqualTo(7L);
    assertThat(response.getCompliantResources()).isEqualTo(4L);
    assertThat(response.getNonCompliantResources()).isEqualTo(3L);
    assertThat(response.getComplianceRate()).isCloseTo(57.14, within(0.01)); // 4/7 ≈ 57.14%
  }
}