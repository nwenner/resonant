package com.wenroe.resonant.service;

import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import com.wenroe.resonant.repository.ResourceTypeSettingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceTypeSettingService {

  private final ResourceTypeSettingRepository repository;

  /**
   * Get all resource type settings.
   */
  public List<ResourceTypeSetting> getAll() {
    return repository.findAll();
  }

  /**
   * Get all enabled resource types.
   */
  public List<ResourceTypeSetting> getEnabledResourceTypes() {
    return repository.findAllByEnabledTrue();
  }

  /**
   * Check if a resource type is enabled.
   */
  public boolean isResourceTypeEnabled(String resourceType) {
    return repository.findByResourceType(resourceType)
        .map(ResourceTypeSetting::getEnabled)
        .orElse(false);
  }

  /**
   * Update the enabled status of a resource type.
   */
  @Transactional
  public ResourceTypeSetting updateEnabled(String resourceType, boolean enabled) {
    ResourceTypeSetting setting = repository.findByResourceType(resourceType)
        .orElseThrow(() -> new RuntimeException("Resource type not found: " + resourceType));

    setting.setEnabled(enabled);
    ResourceTypeSetting updated = repository.save(setting);

    log.info("Updated resource type {} enabled status to {}", resourceType, enabled);
    return updated;
  }
}