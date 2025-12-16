package com.wenroe.resonant.dto.aws;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class UpdateRegionsRequest {

  @NotNull(message = "Enabled region codes are required")
  private List<String> enabledRegionCodes;
}