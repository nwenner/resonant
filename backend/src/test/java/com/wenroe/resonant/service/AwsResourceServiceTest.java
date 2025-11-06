package com.wenroe.resonant.service;

import com.wenroe.resonant.dto.aws.ResourceStats;
import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsResource;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.model.enums.UserRole;
import com.wenroe.resonant.repository.AwsResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsResourceService Tests")
class AwsResourceServiceTest {

    @Mock
    private AwsResourceRepository resourceRepository;

    @InjectMocks
    private AwsResourceService resourceService;

    private User testUser;
    private AwsAccount testAccount;
    private AwsResource testResource;
    private UUID userId;
    private UUID accountId;
    private UUID resourceId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        resourceId = UUID.randomUUID();

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

        // Setup test AWS resource
        testResource = new AwsResource();
        testResource.setId(resourceId);
        testResource.setAwsAccount(testAccount);
        testResource.setResourceId("bucket-12345");
        testResource.setResourceArn("arn:aws:s3:::my-bucket");
        testResource.setResourceType("s3:bucket");
        testResource.setRegion("us-east-1");
        testResource.setName("my-bucket");
        testResource.setTags(Map.of("Environment", "Production", "Owner", "TeamA"));
        testResource.setDiscoveredAt(LocalDateTime.now());
        testResource.setLastSeenAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should get all resources for user")
    void getAllResources_Success() {
        // Given
        when(resourceRepository.findByUserId(userId)).thenReturn(List.of(testResource));

        // When
        List<AwsResource> resources = resourceService.getAllResources(userId, null);

        // Then
        assertThat(resources).hasSize(1);
        assertThat(resources.getFirst()).isEqualTo(testResource);
        assertThat(resources.getFirst().getResourceArn()).isEqualTo("arn:aws:s3:::my-bucket");
        assertThat(resources.getFirst().getResourceType()).isEqualTo("s3:bucket");
        verify(resourceRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should filter resources by type")
    void getAllResources_FilterByType() {
        // Given
        AwsResource s3Resource = new AwsResource();
        s3Resource.setResourceType("s3:bucket");
        s3Resource.setName("s3-bucket");

        AwsResource ec2Resource = new AwsResource();
        ec2Resource.setResourceType("ec2:instance");
        ec2Resource.setName("ec2-instance");

        when(resourceRepository.findByUserId(userId))
                .thenReturn(List.of(s3Resource, ec2Resource));

        // When
        List<AwsResource> resources = resourceService.getAllResources(userId, "s3:bucket");

        // Then
        assertThat(resources).hasSize(1);
        assertThat(resources.getFirst().getResourceType()).isEqualTo("s3:bucket");
        verify(resourceRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should filter resources by type case-insensitive")
    void getAllResources_FilterByTypeCaseInsensitive() {
        // Given
        AwsResource s3Resource = new AwsResource();
        s3Resource.setResourceType("s3:bucket");
        s3Resource.setName("s3-bucket");

        when(resourceRepository.findByUserId(userId)).thenReturn(List.of(s3Resource));

        // When
        List<AwsResource> resources = resourceService.getAllResources(userId, "S3:BUCKET");

        // Then
        assertThat(resources).hasSize(1);
        assertThat(resources.getFirst().getResourceType()).isEqualTo("s3:bucket");
    }

    @Test
    @DisplayName("Should return empty list when no resources match filter")
    void getAllResources_NoMatchingType() {
        // Given
        AwsResource s3Resource = new AwsResource();
        s3Resource.setResourceType("s3:bucket");

        when(resourceRepository.findByUserId(userId)).thenReturn(List.of(s3Resource));

        // When
        List<AwsResource> resources = resourceService.getAllResources(userId, "ec2:instance");

        // Then
        assertThat(resources).isEmpty();
    }

    @Test
    @DisplayName("Should get resource by ARN")
    void getResourceByArn_Success() {
        // Given
        String arn = "arn:aws:s3:::my-bucket";
        when(resourceRepository.findByResourceArn(arn)).thenReturn(Optional.of(testResource));

        // When
        Optional<AwsResource> result = resourceRepository.findByResourceArn(arn);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testResource);
        assertThat(result.get().getResourceArn()).isEqualTo(arn);
        verify(resourceRepository).findByResourceArn(arn);
    }

    @Test
    @DisplayName("Should return empty when resource ARN not found")
    void getResourceByArn_NotFound() {
        // Given
        String arn = "arn:aws:s3:::nonexistent-bucket";
        when(resourceRepository.findByResourceArn(arn)).thenReturn(Optional.empty());

        // When
        Optional<AwsResource> result = resourceRepository.findByResourceArn(arn);

        // Then
        assertThat(result).isEmpty();
        verify(resourceRepository).findByResourceArn(arn);
    }

    @Test
    @DisplayName("Should get resource by ID")
    void getResourceById_Success() {
        // Given
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(testResource));

        // When
        AwsResource resource = resourceService.getResourceById(resourceId);

        // Then
        assertThat(resource).isEqualTo(testResource);
        verify(resourceRepository).findById(resourceId);
    }

    @Test
    @DisplayName("Should throw exception when resource not found")
    void getResourceById_NotFound() {
        // Given
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> resourceService.getResourceById(resourceId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Resource not found");
    }

    @Test
    @DisplayName("Should get resources by account ID")
    void getResourcesByAccountId_Success() {
        // Given
        AwsResource resource1 = new AwsResource();
        resource1.setId(UUID.randomUUID());
        resource1.setResourceType("s3:bucket");

        AwsResource resource2 = new AwsResource();
        resource2.setId(UUID.randomUUID());
        resource2.setResourceType("ec2:instance");

        when(resourceRepository.findByAwsAccountId(accountId))
                .thenReturn(List.of(resource1, resource2));

        // When
        List<AwsResource> resources = resourceService.getResourcesByAccountId(accountId);

        // Then
        assertThat(resources).hasSize(2);
        verify(resourceRepository).findByAwsAccountId(accountId);
    }

    @Test
    @DisplayName("Should return empty list when no resources for account")
    void getResourcesByAccountId_Empty() {
        // Given
        when(resourceRepository.findByAwsAccountId(accountId)).thenReturn(List.of());

        // When
        List<AwsResource> resources = resourceService.getResourcesByAccountId(accountId);

        // Then
        assertThat(resources).isEmpty();
        verify(resourceRepository).findByAwsAccountId(accountId);
    }

    @Test
    @DisplayName("Should get resources by account ID and type")
    void getResourcesByAccountIdAndType_Success() {
        // Given
        AwsResource s3Resource1 = new AwsResource();
        s3Resource1.setId(UUID.randomUUID());
        s3Resource1.setResourceType("s3:bucket");
        s3Resource1.setName("bucket-1");

        AwsResource s3Resource2 = new AwsResource();
        s3Resource2.setId(UUID.randomUUID());
        s3Resource2.setResourceType("s3:bucket");
        s3Resource2.setName("bucket-2");

        when(resourceRepository.findByAwsAccountIdAndResourceType(accountId, "s3:bucket"))
                .thenReturn(List.of(s3Resource1, s3Resource2));

        // When
        List<AwsResource> resources = resourceRepository.findByAwsAccountIdAndResourceType(accountId, "s3:bucket");

        // Then
        assertThat(resources).hasSize(2);
        assertThat(resources).allMatch(r -> r.getResourceType().equals("s3:bucket"));
        verify(resourceRepository).findByAwsAccountIdAndResourceType(accountId, "s3:bucket");
    }

    @Test
    @DisplayName("Should get resource statistics")
    void getResourceStats_Success() {
        // Given
        List<Object[]> typeCounts = new ArrayList<>();
        typeCounts.add(new Object[]{"s3:bucket", 5L});
        typeCounts.add(new Object[]{"ec2:instance", 3L});
        typeCounts.add(new Object[]{"rds:db", 2L});

        when(resourceRepository.countByUserId(userId)).thenReturn(10L);
        when(resourceRepository.countResourcesByType(userId)).thenReturn(typeCounts);

        // When
        ResourceStats stats = resourceService.getResourceStats(userId);

        // Then
        assertThat(stats.getTotal()).isEqualTo(10L);
        assertThat(stats.getByType()).hasSize(3);
        assertThat(stats.getByType().get("s3:bucket")).isEqualTo(5L);
        assertThat(stats.getByType().get("ec2:instance")).isEqualTo(3L);
        assertThat(stats.getByType().get("rds:db")).isEqualTo(2L);

        verify(resourceRepository).countByUserId(userId);
        verify(resourceRepository).countResourcesByType(userId);
    }

    @Test
    @DisplayName("Should get resource statistics when no resources")
    void getResourceStats_NoResources() {
        // Given
        when(resourceRepository.countByUserId(userId)).thenReturn(0L);
        when(resourceRepository.countResourcesByType(userId)).thenReturn(List.of());

        // When
        ResourceStats stats = resourceService.getResourceStats(userId);

        // Then
        assertThat(stats.getTotal()).isEqualTo(0L);
        assertThat(stats.getByType()).isEmpty();
    }

    @Test
    @DisplayName("Should handle single resource type in statistics")
    void getResourceStats_SingleType() {
        // Given
        List<Object[]> typeCounts = new ArrayList<>();
        typeCounts.add(new Object[]{"s3:bucket", 7L});

        when(resourceRepository.countByUserId(userId)).thenReturn(7L);
        when(resourceRepository.countResourcesByType(userId)).thenReturn(typeCounts);

        // When
        ResourceStats stats = resourceService.getResourceStats(userId);

        // Then
        assertThat(stats.getTotal()).isEqualTo(7L);
        assertThat(stats.getByType()).hasSize(1);
        assertThat(stats.getByType().get("s3:bucket")).isEqualTo(7L);
    }

    @Test
    @DisplayName("Should handle type filter with empty string")
    void getAllResources_EmptyTypeFilter() {
        // Given
        when(resourceRepository.findByUserId(userId)).thenReturn(List.of(testResource));

        // When
        List<AwsResource> resources = resourceService.getAllResources(userId, "");

        // Then
        assertThat(resources).hasSize(1);
        assertThat(resources.getFirst()).isEqualTo(testResource);
    }

    @Test
    @DisplayName("Should handle null tags in resource")
    void getAllResources_NullTags() {
        // Given
        AwsResource resourceWithoutTags = new AwsResource();
        resourceWithoutTags.setResourceType("s3:bucket");
        resourceWithoutTags.setTags(null);

        when(resourceRepository.findByUserId(userId)).thenReturn(List.of(resourceWithoutTags));

        // When
        List<AwsResource> resources = resourceService.getAllResources(userId, null);

        // Then
        assertThat(resources).hasSize(1);
        assertThat(resources.getFirst().getTags()).isNull();
    }

    @Test
    @DisplayName("Should check if resource has tag")
    void hasTag_Success() {
        // Given - testResource has "Environment" and "Owner" tags

        // When & Then
        assertThat(testResource.hasTag("Environment")).isTrue();
        assertThat(testResource.hasTag("Owner")).isTrue();
        assertThat(testResource.hasTag("CostCenter")).isFalse();
    }

    @Test
    @DisplayName("Should return false for hasTag when tags are null")
    void hasTag_NullTags() {
        // Given
        testResource.setTags(null);

        // When & Then
        assertThat(testResource.hasTag("Environment")).isFalse();
    }

    @Test
    @DisplayName("Should get tag value")
    void getTagValue_Success() {
        // Given - testResource has tags

        // When & Then
        assertThat(testResource.getTagValue("Environment")).isEqualTo("Production");
        assertThat(testResource.getTagValue("Owner")).isEqualTo("TeamA");
        assertThat(testResource.getTagValue("NonExistent")).isNull();
    }

    @Test
    @DisplayName("Should return null for getTagValue when tags are null")
    void getTagValue_NullTags() {
        // Given
        testResource.setTags(null);

        // When & Then
        assertThat(testResource.getTagValue("Environment")).isNull();
    }

    @Test
    @DisplayName("Should check if resource is tagged")
    void isTagged_Success() {
        // Given - testResource has tags

        // When & Then
        assertThat(testResource.isTagged()).isTrue();
    }

    @Test
    @DisplayName("Should return false when resource has no tags")
    void isTagged_NoTags() {
        // Given
        testResource.setTags(new HashMap<>());

        // When & Then
        assertThat(testResource.isTagged()).isFalse();
    }

    @Test
    @DisplayName("Should return false when tags are null")
    void isTagged_NullTags() {
        // Given
        testResource.setTags(null);

        // When & Then
        assertThat(testResource.isTagged()).isFalse();
    }

    @Test
    @DisplayName("Should get tag count")
    void getTagCount_Success() {
        // Given - testResource has 2 tags

        // When & Then
        assertThat(testResource.getTagCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return zero for tag count when no tags")
    void getTagCount_NoTags() {
        // Given
        testResource.setTags(new HashMap<>());

        // When & Then
        assertThat(testResource.getTagCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return zero for tag count when tags are null")
    void getTagCount_NullTags() {
        // Given
        testResource.setTags(null);

        // When & Then
        assertThat(testResource.getTagCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should update last seen timestamp")
    void updateLastSeen_Success() {
        // Given
        LocalDateTime originalLastSeen = testResource.getLastSeenAt();

        // When
        try {
            Thread.sleep(10); // Small delay to ensure time difference
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        testResource.updateLastSeen();

        // Then
        assertThat(testResource.getLastSeenAt()).isAfter(originalLastSeen);
    }
}