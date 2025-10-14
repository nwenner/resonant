package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.ComplianceViolation;
import com.wenroe.resonant.model.entity.TagPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplianceViolationRepository extends JpaRepository<ComplianceViolation, UUID> {
    List<ComplianceViolation> findByAwsResourceId(UUID awsResourceId);
    List<ComplianceViolation> findByTagPolicyId(UUID tagPolicyId);
    List<ComplianceViolation> findByStatus(ComplianceViolation.Status status);

    @Query("SELECT v FROM ComplianceViolation v WHERE v.awsResource.awsAccount.user.id = :userId")
    List<ComplianceViolation> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT v FROM ComplianceViolation v WHERE v.awsResource.awsAccount.user.id = :userId AND v.status = :status")
    List<ComplianceViolation> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") ComplianceViolation.Status status);

    @Query("SELECT COUNT(v) FROM ComplianceViolation v WHERE v.awsResource.awsAccount.user.id = :userId AND v.status = 'OPEN'")
    long countOpenViolationsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(v) FROM ComplianceViolation v WHERE v.awsResource.awsAccount.user.id = :userId AND v.status = 'OPEN' AND v.severity = :severity")
    long countOpenViolationsByUserIdAndSeverity(@Param("userId") UUID userId, @Param("severity") TagPolicy.Severity severity);
}
