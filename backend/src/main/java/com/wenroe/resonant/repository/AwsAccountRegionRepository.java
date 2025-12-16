package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.AwsAccountRegion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AwsAccountRegionRepository extends JpaRepository<AwsAccountRegion, UUID> {

  List<AwsAccountRegion> findByAwsAccountId(UUID awsAccountId);

  List<AwsAccountRegion> findByAwsAccountIdAndEnabled(UUID awsAccountId, Boolean enabled);

  Optional<AwsAccountRegion> findByAwsAccountIdAndRegionCode(UUID awsAccountId, String regionCode);

  @Query("SELECT r FROM AwsAccountRegion r WHERE r.awsAccount.user.id = :userId")
  List<AwsAccountRegion> findByUserId(@Param("userId") UUID userId);

  @Query("SELECT r FROM AwsAccountRegion r WHERE r.awsAccount.id = :accountId AND r.enabled = true")
  List<AwsAccountRegion> findEnabledRegionsByAccountId(@Param("accountId") UUID accountId);

  long countByAwsAccountIdAndEnabled(UUID awsAccountId, Boolean enabled);
}