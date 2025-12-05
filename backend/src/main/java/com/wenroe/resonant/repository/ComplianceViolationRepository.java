package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.ComplianceViolation;
import com.wenroe.resonant.model.enums.ViolationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplianceViolationRepository extends JpaRepository<ComplianceViolation, UUID> {

  List<ComplianceViolation> findByAwsResourceId(UUID resourceId);

  List<ComplianceViolation> findByTagPolicyId(UUID policyId);

  @Query("SELECT v FROM ComplianceViolation v " +
      "JOIN v.awsResource r " +
      "JOIN r.awsAccount a " +
      "WHERE a.user.id = :userId")
  List<ComplianceViolation> findByUserId(@Param("userId") UUID userId);

  @Query("SELECT v FROM ComplianceViolation v " +
      "JOIN v.awsResource r " +
      "JOIN r.awsAccount a " +
      "WHERE a.user.id = :userId AND v.status = :status")
  List<ComplianceViolation> findViolationsByUserIdAndStatus(@Param("userId") UUID userId,
      @Param("status") ViolationStatus status);

  default List<ComplianceViolation> findOpenViolationsByUserId(UUID userId) {
    return findViolationsByUserIdAndStatus(userId, ViolationStatus.OPEN);
  }

  Optional<ComplianceViolation> findByAwsResourceIdAndTagPolicyId(UUID resourceId, UUID policyId);

  @Query("SELECT v FROM ComplianceViolation v " +
      "JOIN v.awsResource r " +
      "WHERE r.awsAccount.id = :accountId")
  List<ComplianceViolation> findByAwsAccountId(@Param("accountId") UUID accountId);

  @Query("SELECT COUNT(v) FROM ComplianceViolation v " +
      "JOIN v.awsResource r " +
      "JOIN r.awsAccount a " +
      "WHERE a.user.id = :userId AND v.status = :status")
  long countViolationsByUserIdAndStatus(@Param("userId") UUID userId,
      @Param("status") ViolationStatus status);

  default long countOpenViolationsByUserId(UUID userId) {
    return countViolationsByUserIdAndStatus(userId, ViolationStatus.OPEN);
  }

  @Query("SELECT v.tagPolicy.severity, COUNT(v) FROM ComplianceViolation v " +
      "JOIN v.awsResource r " +
      "JOIN r.awsAccount a " +
      "WHERE a.user.id = :userId AND v.status = :status " +
      "GROUP BY v.tagPolicy.severity")
  List<Object[]> countViolationsBySeverityAndStatus(@Param("userId") UUID userId,
      @Param("status") ViolationStatus status);

  default List<Object[]> countOpenViolationsBySeverity(UUID userId) {
    return countViolationsBySeverityAndStatus(userId, ViolationStatus.OPEN);
  }

  @Query("SELECT COUNT(DISTINCT v.awsResource.id) FROM ComplianceViolation v " +
      "WHERE v.awsResource.awsAccount.user.id = :userId AND v.status = :status")
  long countDistinctViolatedResourcesByUserIdAndStatus(@Param("userId") UUID userId,
      @Param("status") ViolationStatus status);

  default long countDistinctViolatedResourcesByUserId(UUID userId) {
    return countDistinctViolatedResourcesByUserIdAndStatus(userId, ViolationStatus.OPEN);
  }
}
