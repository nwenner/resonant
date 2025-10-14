package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.ScanJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScanJobRepository extends JpaRepository<ScanJob, UUID> {
    List<ScanJob> findByAwsAccountIdOrderByStartedAtDesc(UUID awsAccountId);
    List<ScanJob> findByUserIdOrderByStartedAtDesc(UUID userId);
    List<ScanJob> findByStatus(ScanJob.Status status);

    @Query("SELECT sj FROM ScanJob sj WHERE sj.user.id = :userId ORDER BY sj.startedAt DESC")
    List<ScanJob> findRecentByUserId(@Param("userId") UUID userId);

    @Query("SELECT sj FROM ScanJob sj WHERE sj.awsAccount.id = :accountId ORDER BY sj.startedAt DESC")
    List<ScanJob> findRecentByAccountId(@Param("accountId") UUID accountId);
}
