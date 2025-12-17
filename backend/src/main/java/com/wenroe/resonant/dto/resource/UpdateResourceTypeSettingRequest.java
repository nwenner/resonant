package com.wenroe.resonant.dto.resource;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateResourceTypeSettingRequest {

  @NotNull(message = "Enabled is required")
  private Boolean enabled;
}