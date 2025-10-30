package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.ComplianceViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceViolationRepository extends JpaRepository<ComplianceViolation, UUID> {

    /**
     * Find all violations for a specific resource.
     */
    List<ComplianceViolation> findByAwsResourceId(UUID resourceId);

    /**
     * Find all violations for a specific policy.
     */
    List<ComplianceViolation> findByTagPolicyId(UUID policyId);

    /**
     * Find all violations for a user's resources.
     */
    @Query("SELECT v FROM ComplianceViolation v " +
            "JOIN v.awsResource r " +
            "JOIN r.awsAccount a " +
            "WHERE a.user.id = :userId")
    List<ComplianceViolation> findByUserId(@Param("userId") UUID userId);

    /**
     * Find open violations for a user.
     */
    @Query("SELECT v FROM ComplianceViolation v " +
            "JOIN v.awsResource r " +
            "JOIN r.awsAccount a " +
            "WHERE a.user.id = :userId AND v.status = 'OPEN'")
    List<ComplianceViolation> findOpenViolationsByUserId(@Param("userId") UUID userId);

    /**
     * Find violation for a specific resource and policy combination.
     */
    Optional<ComplianceViolation> findByAwsResourceIdAndTagPolicyId(UUID resourceId, UUID policyId);

    /**
     * Find all violations for an AWS account.
     */
    @Query("SELECT v FROM ComplianceViolation v " +
            "JOIN v.awsResource r " +
            "WHERE r.awsAccount.id = :accountId")
    List<ComplianceViolation> findByAwsAccountId(@Param("accountId") UUID accountId);

    /**
     * Count open violations for a user.
     */
    @Query("SELECT COUNT(v) FROM ComplianceViolation v " +
            "JOIN v.awsResource r " +
            "JOIN r.awsAccount a " +
            "WHERE a.user.id = :userId AND v.status = 'OPEN'")
    long countOpenViolationsByUserId(@Param("userId") UUID userId);

    /**
     * Count violations by severity for a user.
     */
    @Query("SELECT v.tagPolicy.severity, COUNT(v) FROM ComplianceViolation v " +
            "JOIN v.awsResource r " +
            "JOIN r.awsAccount a " +
            "WHERE a.user.id = :userId AND v.status = 'OPEN' " +
            "GROUP BY v.tagPolicy.severity")
    List<Object[]> countOpenViolationsBySeverity(@Param("userId") UUID userId);
}