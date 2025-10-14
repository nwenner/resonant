package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.AwsResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AwsResourceRepository extends JpaRepository<AwsResource, UUID> {
    Optional<AwsResource> findByResourceArn(String resourceArn);
    List<AwsResource> findByAwsAccountId(UUID awsAccountId);
    List<AwsResource> findByAwsAccountIdAndRegion(UUID awsAccountId, String region);
    List<AwsResource> findByAwsAccountIdAndResourceType(UUID awsAccountId, String resourceType);

    @Query("SELECT COUNT(r) FROM AwsResource r WHERE r.awsAccount.id = :accountId")
    long countByAwsAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT r FROM AwsResource r WHERE r.awsAccount.user.id = :userId")
    List<AwsResource> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT r FROM AwsResource r WHERE r.lastSeenAt < :threshold")
    List<AwsResource> findStaleResources(@Param("threshold") LocalDateTime threshold);
}
