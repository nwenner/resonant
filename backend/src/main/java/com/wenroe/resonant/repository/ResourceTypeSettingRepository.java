package com.wenroe.resonant.repository;

import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceTypeSettingRepository extends JpaRepository<ResourceTypeSetting, UUID> {

  Optional<ResourceTypeSetting> findByResourceType(String resourceType);

  List<ResourceTypeSetting> findAllByEnabledTrue();

  List<ResourceTypeSetting> findAllByOrderByDisplayNameAsc();

  List<ResourceTypeSetting> findAllByEnabledTrueOrderByDisplayNameAsc();
}