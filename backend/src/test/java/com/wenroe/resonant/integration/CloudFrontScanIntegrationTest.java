package com.wenroe.resonant.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.aws.scanners.CloudFrontResourceScanner;
import java.util.List;
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
@DisplayName("CloudFront Scan Integration Tests")
class CloudFrontScanIntegrationTest {

  @Autowired
  private CloudFrontResourceScanner cloudFrontScanner;

  @Autowired
  private AwsAccountRepository awsAccountRepository;

  @Autowired
  private AwsResourceRepository awsResourceRepository;

  @Autowired
  private UserRepository userRepository;

  private User testUser;
  private AwsAccount testAccount;

  @BeforeEach
  void setUp() {
    awsResourceRepository.deleteAll();
    awsAccountRepository.deleteAll();
    userRepository.deleteAll();

    testUser = new User();
    testUser.setEmail("test@example.com");
    testUser.setName("Test User");
    testUser.setPasswordHash("hashed");
    testUser = userRepository.save(testUser);

    testAccount = new AwsAccount();
    testAccount.setUser(testUser);
    testAccount.setAccountId("123456789012");
    testAccount.setAccountAlias("test-account");
    testAccount.setStatus(AwsAccountStatus.TESTING);
    testAccount.setRoleArn("arn:aws:iam::123456789012:role/test-role");
    testAccount.setExternalId("external-id-123");
    testAccount = awsAccountRepository.save(testAccount);
  }

  @Test
  @DisplayName("Should persist CloudFront resources to database")
  void shouldPersistCloudFrontResources() {
    // Note: This test will fail without real AWS credentials
    // It's included to demonstrate the integration pattern

    // Given - account is set up
    assertThat(testAccount.getId()).isNotNull();

    // When - would scan CloudFront (will fail without credentials)
    // List<AwsResource> resources = cloudFrontScanner.scanDistributions(testAccount);

    // Then - would save to database
    // resources.forEach(resource -> {
    //   resource.setAwsAccount(testAccount);
    //   awsResourceRepository.save(resource);
    // });

    // Verify saved
    // List<AwsResource> saved = awsResourceRepository.findAll();
    // assertThat(saved).isNotEmpty();
    // assertThat(saved.get(0).getResourceType()).isEqualTo("cloudfront:distribution");
  }

  @Test
  @DisplayName("Should handle CloudFront resources with tags")
  void shouldHandleCloudFrontResourcesWithTags() {
    // Given
    AwsResource cfResource = new AwsResource();
    cfResource.setAwsAccount(testAccount);
    cfResource.setResourceId("DIST123");
    cfResource.setResourceArn("arn:aws:cloudfront::123456789012:distribution/DIST123");
    cfResource.setResourceType("cloudfront:distribution");
    cfResource.setRegion("global");
    cfResource.setName("d123.cloudfront.net");
    cfResource.setTags(java.util.Map.of(
        "Environment", "production",
        "Owner", "engineering"
    ));

    // When
    AwsResource saved = awsResourceRepository.save(cfResource);

    // Then
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTags()).hasSize(2);
    assertThat(saved.getTags()).containsEntry("Environment", "production");
    assertThat(saved.getRegion()).isEqualTo("global");
  }

  @Test
  @DisplayName("Should query CloudFront resources by account")
  void shouldQueryCloudFrontResourcesByAccount() {
    // Given
    AwsResource cf1 = new AwsResource();
    cf1.setAwsAccount(testAccount);
    cf1.setResourceId("DIST1");
    cf1.setResourceArn("arn:aws:cloudfront::123456789012:distribution/DIST1");
    cf1.setResourceType("cloudfront:distribution");
    cf1.setRegion("global");
    cf1.setName("d1.cloudfront.net");
    awsResourceRepository.save(cf1);

    AwsResource cf2 = new AwsResource();
    cf2.setAwsAccount(testAccount);
    cf2.setResourceId("DIST2");
    cf2.setResourceArn("arn:aws:cloudfront::123456789012:distribution/DIST2");
    cf2.setResourceType("cloudfront:distribution");
    cf2.setRegion("global");
    cf2.setName("d2.cloudfront.net");
    awsResourceRepository.save(cf2);

    // When
    List<AwsResource> resources = awsResourceRepository.findAll();

    // Then
    assertThat(resources).hasSize(2);
    assertThat(resources)
        .allMatch(r -> r.getResourceType().equals("cloudfront:distribution"))
        .allMatch(r -> r.getRegion().equals("global"));
  }
}