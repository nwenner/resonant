package com.wenroe.resonant.service.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.model.entity.AwsAccount;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.Region;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsRegionDiscoveryService Tests")
class AwsRegionDiscoveryServiceTest {

  @Mock
  private AwsClientFactory clientFactory;

  @Mock
  private Ec2Client ec2Client;

  @InjectMocks
  private AwsRegionDiscoveryService regionDiscoveryService;

  private AwsAccount testAccount;

  @BeforeEach
  void setUp() {
    testAccount = new AwsAccount();
    testAccount.setAccountId("123456789012");
  }

  @Test
  @DisplayName("Should discover enabled regions successfully")
  void shouldDiscoverEnabledRegions() {
    // Given
    Region region1 = Region.builder().regionName("us-east-1").build();
    Region region2 = Region.builder().regionName("us-west-2").build();
    Region region3 = Region.builder().regionName("eu-west-1").build();

    DescribeRegionsResponse response = DescribeRegionsResponse.builder()
        .regions(region1, region2, region3)
        .build();

    when(clientFactory.createEc2ClientForRegionDiscovery(testAccount)).thenReturn(ec2Client);
    when(ec2Client.describeRegions()).thenReturn(response);

    // When
    List<String> regions = regionDiscoveryService.discoverEnabledRegions(testAccount);

    // Then
    assertThat(regions).hasSize(3);
    assertThat(regions).containsExactly("eu-west-1", "us-east-1", "us-west-2"); // Sorted
    verify(clientFactory).createEc2ClientForRegionDiscovery(testAccount);
    verify(ec2Client).describeRegions();
  }

  @Test
  @DisplayName("Should handle EC2 client failure")
  void shouldHandleEc2ClientFailure() {
    // Given
    when(clientFactory.createEc2ClientForRegionDiscovery(testAccount))
        .thenThrow(new RuntimeException("Failed to assume role"));

    // When/Then
    assertThatThrownBy(() -> regionDiscoveryService.discoverEnabledRegions(testAccount))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to discover AWS regions");
  }

  @Test
  @DisplayName("Should handle empty region list")
  void shouldHandleEmptyRegionList() {
    // Given
    DescribeRegionsResponse response = DescribeRegionsResponse.builder()
        .regions(Collections.emptyList())
        .build();

    when(clientFactory.createEc2ClientForRegionDiscovery(testAccount)).thenReturn(ec2Client);
    when(ec2Client.describeRegions()).thenReturn(response);

    // When
    List<String> regions = regionDiscoveryService.discoverEnabledRegions(testAccount);

    // Then
    assertThat(regions).isEmpty();
  }

  @Test
  @DisplayName("Should sort regions alphabetically")
  void shouldSortRegionsAlphabetically() {
    // Given
    Region region1 = Region.builder().regionName("us-west-2").build();
    Region region2 = Region.builder().regionName("ap-south-1").build();
    Region region3 = Region.builder().regionName("eu-west-1").build();
    Region region4 = Region.builder().regionName("us-east-1").build();

    DescribeRegionsResponse response = DescribeRegionsResponse.builder()
        .regions(region1, region2, region3, region4)
        .build();

    when(clientFactory.createEc2ClientForRegionDiscovery(testAccount)).thenReturn(ec2Client);
    when(ec2Client.describeRegions()).thenReturn(response);

    // When
    List<String> regions = regionDiscoveryService.discoverEnabledRegions(testAccount);

    // Then
    assertThat(regions).containsExactly("ap-south-1", "eu-west-1", "us-east-1", "us-west-2");
  }
}