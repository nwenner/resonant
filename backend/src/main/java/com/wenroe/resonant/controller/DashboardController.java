package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.compliance.ComplianceRateResponse;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

  private final DashboardService dashboardService;

  @GetMapping("/compliance-rate")
  public ResponseEntity<ComplianceRateResponse> getComplianceRate(
      @AuthenticationPrincipal User user) {

    log.info("Getting compliance rate for user {}", user.getId());
    ComplianceRateResponse response = dashboardService.getComplianceRate(user.getId());
    return ResponseEntity.ok(response);
  }
}