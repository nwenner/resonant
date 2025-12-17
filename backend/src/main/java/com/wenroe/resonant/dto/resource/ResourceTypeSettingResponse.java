package com.wenroe.resonant.dto.resource;

import com.wenroe.resonant.model.entity.ResourceTypeSetting;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class ResourceTypeSettingResponse {

  private UUID id;
  private String resourceType;
  private String displayName;
  private String description;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static ResourceTypeSettingResponse fromEntity(ResourceTypeSetting entity) {
    ResourceTypeSettingResponse response = new ResourceTypeSettingResponse();
    response.setId(entity.getId());
    response.setResourceType(entity.getResourceType());
    response.setDisplayName(entity.getDisplayName());
    response.setDescription(entity.getDescription());
    response.setEnabled(entity.getEnabled());
    response.setCreatedAt(entity.getCreatedAt());
    response.setUpdatedAt(entity.getUpdatedAt());
    return response;
  }
}