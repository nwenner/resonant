package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.AwsResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AwsResourceRepository extends JpaRepository<AwsResource, UUID> {

    /**
     * Find resource by ARN.
     */
    Optional<AwsResource> findByResourceArn(String resourceArn);

    /**
     * Find all resources for an AWS account.
     */
    List<AwsResource> findByAwsAccountId(UUID accountId);

    /**
     * Find all resources for a user.
     */
    @Query("SELECT r FROM AwsResource r WHERE r.awsAccount.user.id = :userId")
    List<AwsResource> findByUserId(@Param("userId") UUID userId);

    /**
     * Find resources by type for an account.
     */
    List<AwsResource> findByAwsAccountIdAndResourceType(UUID accountId, String resourceType);

    /**
     * Count resources by type for a user.
     */
    @Query("SELECT r.resourceType, COUNT(r) FROM AwsResource r " +
            "WHERE r.awsAccount.user.id = :userId " +
            "GROUP BY r.resourceType")
    List<Object[]> countResourcesByType(@Param("userId") UUID userId);

    /**
     * Count total resources for a user.
     */
    @Query("SELECT COUNT(r) FROM AwsResource r WHERE r.awsAccount.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
}