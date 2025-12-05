package com.wenroe.resonant.dto.compliance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRateResponse {

  private long totalResources;
  private long compliantResources;
  private long nonCompliantResources;
  private double complianceRate;
}