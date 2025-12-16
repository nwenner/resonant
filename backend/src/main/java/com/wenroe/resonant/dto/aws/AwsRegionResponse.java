package com.wenroe.resonant.dto.aws;

import com.wenroe.resonant.model.entity.AwsAccountRegion;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class AwsRegionResponse {

  private UUID id;
  private String regionCode;
  private Boolean enabled;
  private LocalDateTime lastScanAt;
  private LocalDateTime createdAt;

  public static AwsRegionResponse fromEntity(AwsAccountRegion region) {
    AwsRegionResponse response = new AwsRegionResponse();
    response.setId(region.getId());
    response.setRegionCode(region.getRegionCode());
    response.setEnabled(region.getEnabled());
    response.setLastScanAt(region.getLastScanAt());
    response.setCreatedAt(region.getCreatedAt());
    return response;
  }
}