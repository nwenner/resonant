package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.TagPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TagPolicyRepository extends JpaRepository<TagPolicy, UUID> {
    List<TagPolicy> findByUserId(UUID userId);
    List<TagPolicy> findByUserIdAndEnabled(UUID userId, Boolean enabled);

    @Query("SELECT tp FROM TagPolicy tp WHERE tp.user.id = :userId AND tp.enabled = true")
    List<TagPolicy> findEnabledPoliciesByUserId(@Param("userId") UUID userId);
}
