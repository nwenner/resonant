package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.aws.AwsAccountResponse;
import com.wenroe.resonant.dto.aws.AwsAccountRoleRequest;
import com.wenroe.resonant.dto.aws.AwsRegionResponse;
import com.wenroe.resonant.dto.aws.ConnectionTestResponse;
import com.wenroe.resonant.dto.aws.ExternalIdResponse;
import com.wenroe.resonant.dto.aws.UpdateAliasRequest;
import com.wenroe.resonant.dto.aws.UpdateRegionsRequest;
import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.AwsAccountRegion;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.service.AwsAccountRegionService;
import com.wenroe.resonant.service.AwsAccountService;
import com.wenroe.resonant.service.aws.AwsConnectionTester;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aws-accounts")
@RequiredArgsConstructor
@Slf4j
public class AwsAccountController {

  private final AwsAccountService awsAccountService;
  private final AwsAccountRegionService regionService;

  @PostMapping("/external-id")
  public ResponseEntity<ExternalIdResponse> generateExternalId() {
    String externalId = awsAccountService.generateExternalId();
    return ResponseEntity.ok(new ExternalIdResponse(externalId));
  }

  @PostMapping("/role")
  public ResponseEntity<AwsAccountResponse> createAccountWithRole(
      @AuthenticationPrincipal User user,
      @RequestBody AwsAccountRoleRequest request) {

    log.info("Creating AWS account connection with role for user: {}", user.getId());

    AwsAccount account = awsAccountService.createAccountWithRole(
        user.getId(),
        request.getAccountAlias(),
        request.getRoleArn(),
        request.getExternalId()
    );

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(AwsAccountResponse.fromEntity(account));
  }

  @PostMapping("/{id}/test")
  public ResponseEntity<ConnectionTestResponse> testConnection(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id) {

    log.info("Testing AWS account connection: {} (user: {})", id, user.getId());

    AwsConnectionTester.ConnectionTestResult result = awsAccountService.testConnection(id,
        user.getId());

    return ResponseEntity.ok(ConnectionTestResponse.fromResult(result));
  }

  @GetMapping
  public ResponseEntity<List<AwsAccountResponse>> getAccounts(
      @AuthenticationPrincipal User user) {

    log.info("Fetching AWS accounts for user: {}", user.getId());

    List<AwsAccount> accounts = awsAccountService.getAccountsByUserId(user.getId());
    List<AwsAccountResponse> response = accounts.stream()
        .map(AwsAccountResponse::fromEntity)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<AwsAccountResponse> getAccount(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id) {

    log.info("Fetching AWS account: {} (user: {})", id, user.getId());

    AwsAccount account = awsAccountService.getAccountByIdAndVerifyOwnership(id, user.getId());

    return ResponseEntity.ok(AwsAccountResponse.fromEntity(account));
  }

  @PatchMapping("/{id}/alias")
  public ResponseEntity<AwsAccountResponse> updateAlias(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id,
      @RequestBody UpdateAliasRequest request) {

    log.info("Updating alias for AWS account: {} (user: {})", id, user.getId());

    AwsAccount updated = awsAccountService.updateAccountAlias(id, user.getId(),
        request.getAccountAlias());

    return ResponseEntity.ok(AwsAccountResponse.fromEntity(updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAccount(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id) {

    log.info("Deleting AWS account connection: {} (user: {})", id, user.getId());

    awsAccountService.deleteAccount(id, user.getId());

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/regions")
  public ResponseEntity<List<AwsRegionResponse>> getRegions(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id) {

    log.info("Fetching regions for AWS account: {} (user: {})", id, user.getId());

    List<AwsAccountRegion> regions = regionService.getRegionsByAccountId(id);
    List<AwsRegionResponse> response = regions.stream()
        .map(AwsRegionResponse::fromEntity)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/{id}/regions/rediscover")
  public ResponseEntity<List<AwsRegionResponse>> rediscoverRegions(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id) {

    log.info("Rediscovering regions for AWS account: {} (user: {})", id, user.getId());

    List<AwsAccountRegion> newRegions = regionService.rediscoverRegions(id, user.getId());
    List<AwsRegionResponse> response = newRegions.stream()
        .map(AwsRegionResponse::fromEntity)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/regions")
  public ResponseEntity<List<AwsRegionResponse>> updateRegions(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id,
      @RequestBody UpdateRegionsRequest request) {

    log.info("Updating regions for AWS account: {} (user: {})", id, user.getId());

    List<AwsAccountRegion> updated = regionService.updateRegions(id,
        request.getEnabledRegionCodes(), user.getId());
    List<AwsRegionResponse> response = updated.stream()
        .map(AwsRegionResponse::fromEntity)
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/{id}/regions/{regionCode}/enable")
  public ResponseEntity<AwsRegionResponse> enableRegion(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id,
      @PathVariable String regionCode) {

    log.info("Enabling region {} for AWS account: {} (user: {})", regionCode, id, user.getId());

    AwsAccountRegion updated = regionService.enableRegion(id, regionCode, user.getId());

    return ResponseEntity.ok(AwsRegionResponse.fromEntity(updated));
  }

  @PostMapping("/{id}/regions/{regionCode}/disable")
  public ResponseEntity<AwsRegionResponse> disableRegion(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id,
      @PathVariable String regionCode) {

    log.info("Disabling region {} for AWS account: {} (user: {})", regionCode, id, user.getId());

    AwsAccountRegion updated = regionService.disableRegion(id, regionCode, user.getId());

    return ResponseEntity.ok(AwsRegionResponse.fromEntity(updated));
  }
}