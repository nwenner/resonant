package com.wenroe.resonant.integration;

import com.wenroe.resonant.dto.aws.ResourceStats;
import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.AwsAccountStatus;
import com.wenroe.resonant.model.enums.CredentialType;
import com.wenroe.resonant.model.enums.UserRole;
import com.wenroe.resonant.repository.AwsAccountRepository;
import com.wenroe.resonant.repository.AwsResourceRepository;
import com.wenroe.resonant.repository.UserRepository;
import com.wenroe.resonant.service.AwsResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AwsResource Integration Tests")
class AwsResourceIntegrationTest {

    @Autowired
    private AwsResourceService resourceService;

    @Autowired
    private AwsResourceRepository resourceRepository;

    @Autowired
    private AwsAccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private AwsAccount testAccount;

    @BeforeEach
    void setUp() {
        // Clean up
        resourceRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create test AWS account
        testAccount = new AwsAccount();
        testAccount.setUser(testUser);
        testAccount.setAccountId("123456789012");
        testAccount.setAccountAlias("Test Account");
        testAccount.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
        testAccount.setExternalId("external-id-12345");
        testAccount.setCredentialType(CredentialType.ROLE);
        testAccount.setStatus(AwsAccountStatus.ACTIVE);
        testAccount = accountRepository.save(testAccount);
    }

    @Test
    @DisplayName("Should create and retrieve resource")
    void createAndRetrieveResource() {
        // Create resource
        AwsResource resource = createTestResource("s3:bucket", "my-bucket", "us-east-1");

        // Verify
        assertThat(resource).isNotNull();
        assertThat(resource.getId()).isNotNull();
        assertThat(resource.getResourceType()).isEqualTo("s3:bucket");
        assertThat(resource.getName()).isEqualTo("my-bucket");

        // Retrieve by ID
        AwsResource retrieved = resourceService.getResourceById(resource.getId());
        assertThat(retrieved.getName()).isEqualTo("my-bucket");
        assertThat(retrieved.getResourceArn()).isEqualTo("arn:aws:s3:::my-bucket");
    }

    @Test
    @DisplayName("Should get all resources for user")
    void getAllResourcesForUser() {
        // Create multiple resources
        createTestResource("s3:bucket", "bucket-1", "us-east-1");
        createTestResource("ec2:instance", "instance-1", "us-west-2");
        createTestResource("rds:db", "database-1", "eu-west-1");

        // Get all
        List<AwsResource> resources = resourceService.getAllResources(testUser.getId(), null);
        assertThat(resources).hasSize(3);
    }

    @Test
    @DisplayName("Should filter resources by type")
    void filterResourcesByType() {
        // Create resources of different types
        createTestResource("s3:bucket", "bucket-1", "us-east-1");
        createTestResource("s3:bucket", "bucket-2", "us-west-2");
        createTestResource("ec2:instance", "instance-1", "us-east-1");

        // Filter by S3
        List<AwsResource> s3Resources = resourceService.getAllResources(testUser.getId(), "s3:bucket");
        assertThat(s3Resources).hasSize(2);
        assertThat(s3Resources).allMatch(r -> r.getResourceType().equals("s3:bucket"));

        // Filter by EC2
        List<AwsResource> ec2Resources = resourceService.getAllResources(testUser.getId(), "ec2:instance");
        assertThat(ec2Resources).hasSize(1);
        assertThat(ec2Resources.getFirst().getResourceType()).isEqualTo("ec2:instance");
    }

    @Test
    @DisplayName("Should filter resources by type case-insensitive")
    void filterResourcesByTypeCaseInsensitive() {
        // Create resources
        createTestResource("s3:bucket", "bucket-1", "us-east-1");
        createTestResource("ec2:instance", "instance-1", "us-east-1");

        // Filter with different case
        List<AwsResource> resources = resourceService.getAllResources(testUser.getId(), "S3:BUCKET");
        assertThat(resources).hasSize(1);
        assertThat(resources.getFirst().getResourceType()).isEqualTo("s3:bucket");
    }

    @Test
    @DisplayName("Should get resources by account ID")
    void getResourcesByAccountId() {
        // Create resources
        createTestResource("s3:bucket", "bucket-1", "us-east-1");
        createTestResource("ec2:instance", "instance-1", "us-west-2");

        // Get by account
        List<AwsResource> resources = resourceService.getResourcesByAccountId(testAccount.getId());
        assertThat(resources).hasSize(2);
        assertThat(resources).allMatch(r -> r.getAwsAccount().getId().equals(testAccount.getId()));
    }

    @Test
    @DisplayName("Should find resource by ARN")
    void findResourceByArn() {
        // Create resource
        AwsResource created = createTestResource("s3:bucket", "my-bucket", "us-east-1");

        // Find by ARN
        Optional<AwsResource> found = resourceRepository.findByResourceArn(created.getResourceArn());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("my-bucket");
    }

    @Test
    @DisplayName("Should handle resource not found")
    void resourceNotFound() {
        // Try to get non-existent resource
        assertThatThrownBy(() -> resourceService.getResourceById(java.util.UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Resource not found");
    }

    @Test
    @DisplayName("Should get resource statistics")
    void getResourceStatistics() {
        // Create resources of different types
        createTestResource("s3:bucket", "bucket-1", "us-east-1");
        createTestResource("s3:bucket", "bucket-2", "us-east-1");
        createTestResource("s3:bucket", "bucket-3", "us-west-2");
        createTestResource("ec2:instance", "instance-1", "us-east-1");
        createTestResource("ec2:instance", "instance-2", "eu-west-1");
        createTestResource("rds:db", "database-1", "us-east-1");

        // Get stats
        ResourceStats stats = resourceService.getResourceStats(testUser.getId());

        // Verify
        assertThat(stats.getTotal()).isEqualTo(6);
        assertThat(stats.getByType()).hasSize(3);
        assertThat(stats.getByType().get("s3:bucket")).isEqualTo(3);
        assertThat(stats.getByType().get("ec2:instance")).isEqualTo(2);
        assertThat(stats.getByType().get("rds:db")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should get empty statistics when no resources")
    void getEmptyStatistics() {
        // Get stats with no resources
        ResourceStats stats = resourceService.getResourceStats(testUser.getId());

        assertThat(stats.getTotal()).isEqualTo(0);
        assertThat(stats.getByType()).isEmpty();
    }

    @Test
    @DisplayName("Should handle resources with tags")
    void handleResourcesWithTags() {
        // Create resource with tags
        AwsResource resource = new AwsResource();
        resource.setAwsAccount(testAccount);
        resource.setResourceId("bucket-12345");
        resource.setResourceArn("arn:aws:s3:::tagged-bucket");
        resource.setResourceType("s3:bucket");
        resource.setRegion("us-east-1");
        resource.setName("tagged-bucket");

        Map<String, String> tags = new HashMap<>();
        tags.put("Environment", "Production");
        tags.put("Owner", "TeamA");
        tags.put("CostCenter", "Engineering");
        resource.setTags(tags);

        resource = resourceRepository.save(resource);

        // Verify tags
        AwsResource retrieved = resourceService.getResourceById(resource.getId());
        assertThat(retrieved.getTags()).hasSize(3);
        assertThat(retrieved.getTagCount()).isEqualTo(3);
        assertThat(retrieved.hasTag("Environment")).isTrue();
        assertThat(retrieved.getTagValue("Environment")).isEqualTo("Production");
        assertThat(retrieved.isTagged()).isTrue();
    }

    @Test
    @DisplayName("Should handle resources without tags")
    void handleResourcesWithoutTags() {
        // Create resource without tags
        AwsResource resource = new AwsResource();
        resource.setAwsAccount(testAccount);
        resource.setResourceId("bucket-67890");
        resource.setResourceArn("arn:aws:s3:::untagged-bucket");
        resource.setResourceType("s3:bucket");
        resource.setRegion("us-east-1");
        resource.setName("untagged-bucket");
        resource.setTags(null);

        resource = resourceRepository.save(resource);

        // Verify
        AwsResource retrieved = resourceService.getResourceById(resource.getId());
        assertThat(retrieved.getTags()).isNull();
        assertThat(retrieved.getTagCount()).isEqualTo(0);
        assertThat(retrieved.hasTag("Environment")).isFalse();
        assertThat(retrieved.getTagValue("Environment")).isNull();
        assertThat(retrieved.isTagged()).isFalse();
    }

    @Test
    @DisplayName("Should handle resource metadata")
    void handleResourceMetadata() {
        // Create resource with metadata
        AwsResource resource = new AwsResource();
        resource.setAwsAccount(testAccount);
        resource.setResourceId("bucket-metadata");
        resource.setResourceArn("arn:aws:s3:::metadata-bucket");
        resource.setResourceType("s3:bucket");
        resource.setRegion("us-east-1");
        resource.setName("metadata-bucket");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("versioning", true);
        metadata.put("encryption", "AES256");
        metadata.put("publicAccessBlock", false);
        resource.setMetadata(metadata);

        resource = resourceRepository.save(resource);

        // Verify metadata
        AwsResource retrieved = resourceService.getResourceById(resource.getId());
        assertThat(retrieved.getMetadata()).hasSize(3);
        assertThat(retrieved.getMetadata().get("versioning")).isEqualTo(true);
        assertThat(retrieved.getMetadata().get("encryption")).isEqualTo("AES256");
    }

    @Test
    @DisplayName("Should find resources by account and type")
    void findResourcesByAccountAndType() {
        // Create resources
        createTestResource("s3:bucket", "bucket-1", "us-east-1");
        createTestResource("s3:bucket", "bucket-2", "us-west-2");
        createTestResource("ec2:instance", "instance-1", "us-east-1");

        // Find by account and type
        List<AwsResource> s3Resources = resourceRepository.findByAwsAccountIdAndResourceType(
                testAccount.getId(),
                "s3:bucket"
        );

        assertThat(s3Resources).hasSize(2);
        assertThat(s3Resources).allMatch(r -> r.getResourceType().equals("s3:bucket"));
    }

    @Test
    @DisplayName("Should handle multiple accounts and users")
    void handleMultipleAccountsAndUsers() {
        // Create another user
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setName("Other User");
        otherUser.setPasswordHash(passwordEncoder.encode("password123"));
        otherUser.setRole(UserRole.USER);
        otherUser.setEnabled(true);
        otherUser = userRepository.save(otherUser);

        // Create account for other user
        AwsAccount otherAccount = new AwsAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setAccountId("987654321098");
        otherAccount.setAccountAlias("Other Account");
        otherAccount.setRoleArn("arn:aws:iam::987654321098:role/OtherRole");
        otherAccount.setExternalId("other-external-id");
        otherAccount.setCredentialType(CredentialType.ROLE);
        otherAccount.setStatus(AwsAccountStatus.ACTIVE);
        otherAccount = accountRepository.save(otherAccount);

        // Create resources for both users
        createTestResource("s3:bucket", "bucket-1", "us-east-1"); // testUser
        createResourceForAccount(otherAccount, "s3:bucket", "bucket-2", "us-west-2"); // otherUser

        // Verify isolation
        List<AwsResource> testUserResources = resourceService.getAllResources(testUser.getId(), null);
        List<AwsResource> otherUserResources = resourceService.getAllResources(otherUser.getId(), null);

        assertThat(testUserResources).hasSize(1);
        assertThat(otherUserResources).hasSize(1);
        assertThat(testUserResources.getFirst().getName()).isEqualTo("bucket-1");
        assertThat(otherUserResources.getFirst().getName()).isEqualTo("bucket-2");
    }

    private AwsResource createTestResource(String resourceType, String name, String region) {
        return createResourceForAccount(testAccount, resourceType, name, region);
    }

    private AwsResource createResourceForAccount(AwsAccount account, String resourceType, String name, String region) {
        AwsResource resource = new AwsResource();
        resource.setAwsAccount(account);
        resource.setResourceId(name + "-id");
        resource.setResourceArn("arn:aws:" + resourceType.split(":")[0] + ":::" + name);
        resource.setResourceType(resourceType);
        resource.setRegion(region);
        resource.setName(name);
        resource.setTags(Map.of("Name", name));
        return resourceRepository.save(resource);
    }
}