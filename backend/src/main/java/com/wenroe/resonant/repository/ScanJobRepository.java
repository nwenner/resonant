package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.ScanJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScanJobRepository extends JpaRepository<ScanJob, UUID> {

    /**
     * Find all scan jobs for a user.
     */
    List<ScanJob> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find all scan jobs for a specific AWS account.
     */
    List<ScanJob> findByAwsAccountIdOrderByCreatedAtDesc(UUID accountId);

    /**
     * Find the most recent scan job for an account.
     */
    Optional<ScanJob> findFirstByAwsAccountIdOrderByCreatedAtDesc(UUID accountId);

    /**
     * Find running scan jobs.
     */
    @Query("SELECT s FROM ScanJob s WHERE s.status = 'RUNNING'")
    List<ScanJob> findRunningJobs();

    /**
     * Find running scan job for a specific account.
     */
    @Query("SELECT s FROM ScanJob s WHERE s.awsAccount.id = :accountId AND s.status = 'RUNNING'")
    Optional<ScanJob> findRunningScanForAccount(@Param("accountId") UUID accountId);

    /**
     * Count total scans for a user.
     */
    long countByUserId(UUID userId);
}