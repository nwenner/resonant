package com.wenroe.resonant.service.aws.scanners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsAccountRegion;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.service.AwsAccountRegionService;
import com.wenroe.resonant.service.aws.AwsClientFactory;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpcCidrBlockAssociation;

@ExtendWith(MockitoExtension.class)
@DisplayName("VpcResourceScanner Tests")
class VpcResourceScannerTest {

  @Mock
  private AwsClientFactory clientFactory;

  @Mock
  private AwsAccountRegionService regionService;

  @Mock
  private Ec2Client ec2Client;

  private VpcResourceScanner scanner;

  private AwsAccount testAccount;
  private AwsAccountRegion usEast1Region;
  private AwsAccountRegion usWest2Region;

  @BeforeEach
  void setUp() {
    scanner = new VpcResourceScanner(clientFactory, regionService);

    testAccount = new AwsAccount();
    testAccount.setId(UUID.randomUUID());
    testAccount.setAccountId("123456789012");
    testAccount.setAccountAlias("test-account");

    usEast1Region = new AwsAccountRegion();
    usEast1Region.setRegionCode("us-east-1");
    usEast1Region.setEnabled(true);

    usWest2Region = new AwsAccountRegion();
    usWest2Region.setRegionCode("us-west-2");
    usWest2Region.setEnabled(true);
  }

  @Test
  @DisplayName("Should scan VPCs in single region successfully")
  void shouldScanVpcsInSingleRegion() {
    // Given
    when(regionService.getEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(List.of(usEast1Region));
    when(clientFactory.createEc2Client(testAccount, "us-east-1")).thenReturn(ec2Client);

    Vpc vpc = Vpc.builder()
        .vpcId("vpc-123")
        .cidrBlock("10.0.0.0/16")
        .state("available")
        .isDefault(false)
        .instanceTenancy("default")
        .ownerId("123456789012")
        .dhcpOptionsId("dopt-123")
        .tags(Tag.builder().key("Name").value("test-vpc").build(),
            Tag.builder().key("Environment").value("production").build())
        .build();

    DescribeVpcsResponse vpcsResponse = DescribeVpcsResponse.builder()
        .vpcs(vpc)
        .build();

    when(ec2Client.describeVpcs(any(DescribeVpcsRequest.class))).thenReturn(vpcsResponse);

    Subnet subnet = Subnet.builder()
        .subnetId("subnet-123")
        .vpcId("vpc-123")
        .build();

    DescribeSubnetsResponse subnetsResponse = DescribeSubnetsResponse.builder()
        .subnets(subnet)
        .build();

    when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(subnetsResponse);

    // When
    List<AwsResource> resources = scanner.scan(testAccount);

    // Then
    assertThat(resources).hasSize(1);

    AwsResource resource = resources.get(0);
    assertThat(resource.getResourceId()).isEqualTo("vpc-123");
    assertThat(resource.getResourceArn()).isEqualTo(
        "arn:aws:ec2:us-east-1:123456789012:vpc/vpc-123");
    assertThat(resource.getResourceType()).isEqualTo("ec2:vpc");
    assertThat(resource.getName()).isEqualTo("test-vpc");
    assertThat(resource.getRegion()).isEqualTo("us-east-1");
    assertThat(resource.getTags())
        .containsEntry("Name", "test-vpc")
        .containsEntry("Environment", "production");
    assertThat(resource.getMetadata())
        .containsEntry("cidrBlock", "10.0.0.0/16")
        .containsEntry("state", "available")
        .containsEntry("isDefault", false)
        .containsEntry("instanceTenancy", "default")
        .containsEntry("ownerId", "123456789012")
        .containsEntry("dhcpOptionsId", "dopt-123")
        .containsEntry("subnetCount", 1);

    verify(clientFactory).createEc2Client(testAccount, "us-east-1");
  }

  @Test
  @DisplayName("Should scan VPCs across multiple regions")
  void shouldScanVpcsAcrossMultipleRegions() {
    // Given
    when(regionService.getEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(List.of(usEast1Region, usWest2Region));

    // Create separate mocks for each region
    Ec2Client ec2ClientEast = org.mockito.Mockito.mock(Ec2Client.class);
    Ec2Client ec2ClientWest = org.mockito.Mockito.mock(Ec2Client.class);

    when(clientFactory.createEc2Client(testAccount, "us-east-1")).thenReturn(ec2ClientEast);
    when(clientFactory.createEc2Client(testAccount, "us-west-2")).thenReturn(ec2ClientWest);

    // Region 1 - 2 VPCs
    Vpc vpc1 = Vpc.builder()
        .vpcId("vpc-111")
        .cidrBlock("10.0.0.0/16")
        .state("available")
        .isDefault(false)
        .instanceTenancy("default")
        .ownerId("123456789012")
        .tags(Tag.builder().key("Name").value("vpc-east").build())
        .build();

    Vpc vpc2 = Vpc.builder()
        .vpcId("vpc-222")
        .cidrBlock("10.1.0.0/16")
        .state("available")
        .isDefault(true)
        .instanceTenancy("default")
        .ownerId("123456789012")
        .build();

    when(ec2ClientEast.describeVpcs(any(DescribeVpcsRequest.class)))
        .thenReturn(DescribeVpcsResponse.builder().vpcs(vpc1, vpc2).build());

    // Region 2 - 1 VPC
    Vpc vpc3 = Vpc.builder()
        .vpcId("vpc-333")
        .cidrBlock("10.2.0.0/16")
        .state("available")
        .isDefault(false)
        .instanceTenancy("dedicated")
        .ownerId("123456789012")
        .tags(Tag.builder().key("Name").value("vpc-west").build())
        .build();

    when(ec2ClientWest.describeVpcs(any(DescribeVpcsRequest.class)))
        .thenReturn(DescribeVpcsResponse.builder().vpcs(vpc3).build());

    when(ec2ClientEast.describeSubnets(any(DescribeSubnetsRequest.class)))
        .thenReturn(DescribeSubnetsResponse.builder().subnets(Collections.emptyList()).build());
    when(ec2ClientWest.describeSubnets(any(DescribeSubnetsRequest.class)))
        .thenReturn(DescribeSubnetsResponse.builder().subnets(Collections.emptyList()).build());

    // When
    List<AwsResource> resources = scanner.scan(testAccount);

    // Then
    assertThat(resources).hasSize(3);
    assertThat(resources).extracting(AwsResource::getResourceId)
        .containsExactlyInAnyOrder("vpc-111", "vpc-222", "vpc-333");
    assertThat(resources).extracting(AwsResource::getRegion)
        .contains("us-east-1", "us-east-1", "us-west-2");
    assertThat(resources).extracting(AwsResource::getName)
        .contains("vpc-east", "vpc-222", "vpc-west");

    verify(clientFactory).createEc2Client(testAccount, "us-east-1");
    verify(clientFactory).createEc2Client(testAccount, "us-west-2");
  }

  @Test
  @DisplayName("Should handle VPCs with no tags")
  void shouldHandleVpcsWithNoTags() {
    // Given
    when(regionService.getEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(List.of(usEast1Region));
    when(clientFactory.createEc2Client(testAccount, "us-east-1")).thenReturn(ec2Client);

    Vpc vpc = Vpc.builder()
        .vpcId("vpc-notags")
        .cidrBlock("172.16.0.0/16")
        .state("available")
        .isDefault(false)
        .instanceTenancy("default")
        .ownerId("123456789012")
        .build();

    when(ec2Client.describeVpcs(any(DescribeVpcsRequest.class)))
        .thenReturn(DescribeVpcsResponse.builder().vpcs(vpc).build());
    when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class)))
        .thenReturn(DescribeSubnetsResponse.builder().subnets(Collections.emptyList()).build());

    // When
    List<AwsResource> resources = scanner.scan(testAccount);

    // Then
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getTags()).isEmpty();
    assertThat(resources.get(0).getName()).isEqualTo("vpc-notags");
  }

  @Test
  @DisplayName("Should handle region with no VPCs")
  void shouldHandleRegionWithNoVpcs() {
    // Given
    when(regionService.getEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(List.of(usEast1Region));
    when(clientFactory.createEc2Client(testAccount, "us-east-1")).thenReturn(ec2Client);

    when(ec2Client.describeVpcs(any(DescribeVpcsRequest.class)))
        .thenReturn(DescribeVpcsResponse.builder().vpcs(Collections.emptyList()).build());

    // When
    List<AwsResource> resources = scanner.scan(testAccount);

    // Then
    assertThat(resources).isEmpty();
  }

  @Test
  @DisplayName("Should handle no enabled regions")
  void shouldHandleNoEnabledRegions() {
    // Given
    when(regionService.getEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(Collections.emptyList());

    // When
    List<AwsResource> resources = scanner.scan(testAccount);

    // Then
    assertThat(resources).isEmpty();
  }

  @Test
  @DisplayName("Should handle VPC with multiple CIDR blocks")
  void shouldHandleVpcWithMultipleCidrBlocks() {
    // Given
    when(regionService.getEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(List.of(usEast1Region));
    when(clientFactory.createEc2Client(testAccount, "us-east-1")).thenReturn(ec2Client);

    VpcCidrBlockAssociation cidr1 = VpcCidrBlockAssociation.builder()
        .cidrBlock("10.0.0.0/16")
        .build();

    VpcCidrBlockAssociation cidr2 = VpcCidrBlockAssociation.builder()
        .cidrBlock("10.1.0.0/16")
        .build();

    Vpc vpc = Vpc.builder()
        .vpcId("vpc-multi-cidr")
        .cidrBlock("10.0.0.0/16")
        .cidrBlockAssociationSet(cidr1, cidr2)
        .state("available")
        .isDefault(false)
        .instanceTenancy("default")
        .ownerId("123456789012")
        .build();

    when(ec2Client.describeVpcs(any(DescribeVpcsRequest.class)))
        .thenReturn(DescribeVpcsResponse.builder().vpcs(vpc).build());
    when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class)))
        .thenReturn(DescribeSubnetsResponse.builder().subnets(Collections.emptyList()).build());

    // When
    List<AwsResource> resources = scanner.scan(testAccount);

    // Then
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getMetadata())
        .containsEntry("cidrBlock", "10.0.0.0/16")
        .containsKey("additionalCidrBlocks");

    @SuppressWarnings("unchecked")
    List<String> additionalCidrs = (List<String>) resources.get(0).getMetadata()
        .get("additionalCidrBlocks");
    assertThat(additionalCidrs).containsExactly("10.1.0.0/16");
  }

  @Test
  @DisplayName("Should handle EC2 API errors in specific region")
  void shouldHandleApiErrorsInRegion() {
    // Given
    when(regionService.getEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(List.of(usEast1Region, usWest2Region));

    when(clientFactory.createEc2Client(testAccount, "us-east-1")).thenReturn(ec2Client);
    when(clientFactory.createEc2Client(testAccount, "us-west-2")).thenReturn(ec2Client);

    // Region 1 fails
    when(ec2Client.describeVpcs(any(DescribeVpcsRequest.class)))
        .thenThrow(new RuntimeException("EC2 API error"))
        .thenReturn(DescribeVpcsResponse.builder()
            .vpcs(Vpc.builder()
                .vpcId("vpc-west")
                .cidrBlock("10.0.0.0/16")
                .state("available")
                .isDefault(false)
                .instanceTenancy("default")
                .ownerId("123456789012")
                .build())
            .build());

    when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class)))
        .thenReturn(DescribeSubnetsResponse.builder().subnets(Collections.emptyList()).build());

    // When
    List<AwsResource> resources = scanner.scan(testAccount);

    // Then
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getResourceId()).isEqualTo("vpc-west");
  }

  @Test
  @DisplayName("Should return correct resource type")
  void shouldReturnCorrectResourceType() {
    // When
    String resourceType = scanner.getResourceType();

    // Then
    assertThat(resourceType).isEqualTo("ec2:vpc");
  }

  @Test
  @DisplayName("Should handle subnet count query failure gracefully")
  void shouldHandleSubnetCountFailureGracefully() {
    // Given
    when(regionService.getEnabledRegionsByAccountId(testAccount.getId()))
        .thenReturn(List.of(usEast1Region));
    when(clientFactory.createEc2Client(testAccount, "us-east-1")).thenReturn(ec2Client);

    Vpc vpc = Vpc.builder()
        .vpcId("vpc-123")
        .cidrBlock("10.0.0.0/16")
        .state("available")
        .isDefault(false)
        .instanceTenancy("default")
        .ownerId("123456789012")
        .build();

    when(ec2Client.describeVpcs(any(DescribeVpcsRequest.class)))
        .thenReturn(DescribeVpcsResponse.builder().vpcs(vpc).build());
    when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class)))
        .thenThrow(new RuntimeException("Subnet API error"));

    // When
    List<AwsResource> resources = scanner.scan(testAccount);

    // Then
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getMetadata()).doesNotContainKey("subnetCount");
  }
}