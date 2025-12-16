package com.wenroe.resonant.service.aws.scanners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.service.aws.AwsClientFactory;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.DistributionList;
import software.amazon.awssdk.services.cloudfront.model.DistributionSummary;
import software.amazon.awssdk.services.cloudfront.model.HttpVersion;
import software.amazon.awssdk.services.cloudfront.model.ListDistributionsResponse;
import software.amazon.awssdk.services.cloudfront.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.cloudfront.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.cloudfront.model.PriceClass;
import software.amazon.awssdk.services.cloudfront.model.Tag;
import software.amazon.awssdk.services.cloudfront.model.Tags;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloudFrontResourceScanner Tests")
class CloudFrontResourceScannerTest {

  @Mock
  private AwsClientFactory clientFactory;

  @Mock
  private CloudFrontClient cloudFrontClient;

  private CloudFrontResourceScanner scanner;

  private AwsAccount testAccount;

  @BeforeEach
  void setUp() {
    scanner = new CloudFrontResourceScanner(clientFactory);

    testAccount = new AwsAccount();
    testAccount.setAccountId("123456789012");
    testAccount.setAccountAlias("test-account");
  }

  @Test
  @DisplayName("Should scan distributions successfully")
  void shouldScanDistributions() {
    // Given
    when(clientFactory.createCloudFrontClient(testAccount)).thenReturn(cloudFrontClient);

    DistributionSummary dist1 = DistributionSummary.builder()
        .id("DIST123")
        .arn("arn:aws:cloudfront::123456789012:distribution/DIST123")
        .domainName("d123.cloudfront.net")
        .status("Deployed")
        .enabled(true)
        .priceClass(PriceClass.PRICE_CLASS_ALL)
        .httpVersion(HttpVersion.HTTP2)
        .comment("Test distribution")
        .lastModifiedTime(Instant.now())
        .build();

    DistributionList distList = DistributionList.builder()
        .items(dist1)
        .quantity(1)
        .isTruncated(false)
        .build();

    ListDistributionsResponse response = ListDistributionsResponse.builder()
        .distributionList(distList)
        .build();

    when(cloudFrontClient.listDistributions()).thenReturn(response);

    Tags tags = Tags.builder()
        .items(Tag.builder().key("Environment").value("production").build())
        .build();

    when(cloudFrontClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder().tags(tags).build());

    // When
    List<AwsResource> resources = scanner.scanDistributions(testAccount);

    // Then
    assertThat(resources).hasSize(1);

    AwsResource resource = resources.get(0);
    assertThat(resource.getResourceId()).isEqualTo("DIST123");
    assertThat(resource.getResourceArn()).isEqualTo(
        "arn:aws:cloudfront::123456789012:distribution/DIST123");
    assertThat(resource.getResourceType()).isEqualTo("cloudfront:distribution");
    assertThat(resource.getName()).isEqualTo("d123.cloudfront.net");
    assertThat(resource.getRegion()).isEqualTo("global");
    assertThat(resource.getTags()).containsEntry("Environment", "production");
    assertThat(resource.getMetadata())
        .containsEntry("status", "Deployed")
        .containsEntry("enabled", true)
        .containsEntry("domainName", "d123.cloudfront.net");

    verify(clientFactory).createCloudFrontClient(testAccount);
  }

  @Test
  @DisplayName("Should handle no distributions found")
  void shouldHandleNoDistributions() {
    // Given
    when(clientFactory.createCloudFrontClient(testAccount)).thenReturn(cloudFrontClient);

    DistributionList distList = DistributionList.builder()
        .items(Collections.emptyList())
        .quantity(0)
        .build();

    ListDistributionsResponse response = ListDistributionsResponse.builder()
        .distributionList(distList)
        .build();

    when(cloudFrontClient.listDistributions()).thenReturn(response);

    // When
    List<AwsResource> resources = scanner.scanDistributions(testAccount);

    // Then
    assertThat(resources).isEmpty();
  }

  @Test
  @DisplayName("Should handle distributions with no tags")
  void shouldHandleDistributionsWithNoTags() {
    // Given
    when(clientFactory.createCloudFrontClient(testAccount)).thenReturn(cloudFrontClient);

    DistributionSummary dist = DistributionSummary.builder()
        .id("DIST456")
        .arn("arn:aws:cloudfront::123456789012:distribution/DIST456")
        .domainName("d456.cloudfront.net")
        .status("Deployed")
        .enabled(true)
        .priceClass(PriceClass.PRICE_CLASS_100)
        .httpVersion(HttpVersion.HTTP1_1)
        .lastModifiedTime(Instant.now())
        .build();

    DistributionList distList = DistributionList.builder()
        .items(dist)
        .quantity(1)
        .build();

    when(cloudFrontClient.listDistributions()).thenReturn(
        ListDistributionsResponse.builder().distributionList(distList).build());

    Tags emptyTags = Tags.builder().items(Collections.emptyList()).build();
    when(cloudFrontClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder().tags(emptyTags).build());

    // When
    List<AwsResource> resources = scanner.scanDistributions(testAccount);

    // Then
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getTags()).isEmpty();
  }

  @Test
  @DisplayName("Should handle CloudFront API errors")
  void shouldHandleApiErrors() {
    // Given
    when(clientFactory.createCloudFrontClient(testAccount)).thenReturn(cloudFrontClient);
    when(cloudFrontClient.listDistributions())
        .thenThrow(new RuntimeException("CloudFront API error"));

    // When/Then
    assertThatThrownBy(() -> scanner.scanDistributions(testAccount))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("CloudFront scan failed");
  }

  @Test
  @DisplayName("Should scan multiple distributions")
  void shouldScanMultipleDistributions() {
    // Given
    when(clientFactory.createCloudFrontClient(testAccount)).thenReturn(cloudFrontClient);

    DistributionSummary dist1 = DistributionSummary.builder()
        .id("DIST1")
        .arn("arn:aws:cloudfront::123456789012:distribution/DIST1")
        .domainName("d1.cloudfront.net")
        .status("Deployed")
        .enabled(true)
        .priceClass(PriceClass.PRICE_CLASS_ALL)
        .httpVersion(HttpVersion.HTTP2)
        .lastModifiedTime(Instant.now())
        .build();

    DistributionSummary dist2 = DistributionSummary.builder()
        .id("DIST2")
        .arn("arn:aws:cloudfront::123456789012:distribution/DIST2")
        .domainName("d2.cloudfront.net")
        .status("InProgress")
        .enabled(false)
        .priceClass(PriceClass.PRICE_CLASS_100)
        .httpVersion(HttpVersion.HTTP1_1)
        .lastModifiedTime(Instant.now())
        .build();

    DistributionList distList = DistributionList.builder()
        .items(dist1, dist2)
        .quantity(2)
        .build();

    when(cloudFrontClient.listDistributions()).thenReturn(
        ListDistributionsResponse.builder().distributionList(distList).build());

    Tags emptyTags = Tags.builder().items(Collections.emptyList()).build();
    when(cloudFrontClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder().tags(emptyTags).build());

    // When
    List<AwsResource> resources = scanner.scanDistributions(testAccount);

    // Then
    assertThat(resources).hasSize(2);
    assertThat(resources.get(0).getResourceId()).isEqualTo("DIST1");
    assertThat(resources.get(1).getResourceId()).isEqualTo("DIST2");
    assertThat(resources.get(0).getMetadata()).containsEntry("enabled", true);
    assertThat(resources.get(1).getMetadata()).containsEntry("enabled", false);
  }
}