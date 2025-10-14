package com.wenroe.resonant.controller;

import com.wenroe.resonant.dto.aws.*;
import com.wenroe.resonant.model.entity.AwsAccount;
import com.wenroe.resonant.model.entity.User;
import com.wenroe.resonant.service.AwsAccountService;
import com.wenroe.resonant.service.aws.AwsConnectionTester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing AWS account connections.
 */
@RestController
@RequestMapping("/api/aws-accounts")
@RequiredArgsConstructor
@Slf4j
public class AwsAccountController {

    private final AwsAccountService awsAccountService;

    /**
     * Generate a new external ID for IAM role setup.
     */
    @PostMapping("/external-id")
    public ResponseEntity<ExternalIdResponse> generateExternalId() {
        String externalId = awsAccountService.generateExternalId();
        return ResponseEntity.ok(new ExternalIdResponse(externalId));
    }

    /**
     * Create a new AWS account connection with IAM role (recommended).
     */
    @PostMapping("/role")
    public ResponseEntity<AwsAccountResponse> createAccountWithRole(
            @AuthenticationPrincipal User user,
            @RequestBody AwsAccountRoleRequest request) {

        log.info("Creating AWS account connection with role for user: {}", user.getId());

        AwsAccount account = awsAccountService.createAccountWithRole(
                user.getId(),
                request.getAccountId(),
                request.getAccountAlias(),
                request.getRoleArn(),
                request.getExternalId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AwsAccountResponse.fromEntity(account));
    }

    /**
     * Test connection to an AWS account.
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<ConnectionTestResponse> testConnection(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        log.info("Testing AWS account connection: {}", id);

        AwsAccount account = awsAccountService.getAccountById(id);

        // Verify ownership
        if (!account.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AwsConnectionTester.ConnectionTestResult result = awsAccountService.testConnection(id);

        ConnectionTestResponse response = new ConnectionTestResponse();
        response.setSuccess(result.isSuccess());
        response.setMessage(result.getMessage());
        response.setErrorMessage(result.getErrorMessage());
        response.setAccountId(result.getAccountId());
        response.setAssumedRoleArn(result.getAssumedRoleArn());
        response.setUserId(result.getUserId());
        response.setAvailableRegionCount(result.getAvailableRegionCount());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all AWS accounts for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<AwsAccountResponse>> getAccounts(
            @AuthenticationPrincipal User user) {

        List<AwsAccount> accounts = awsAccountService.getAccountsByUserId(user.getId());
        List<AwsAccountResponse> response = accounts.stream()
                .map(AwsAccountResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific AWS account by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AwsAccountResponse> getAccount(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        AwsAccount account = awsAccountService.getAccountById(id);

        // Verify ownership
        if (!account.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(AwsAccountResponse.fromEntity(account));
    }

    /**
     * Update AWS account alias.
     */
    @PatchMapping("/{id}/alias")
    public ResponseEntity<AwsAccountResponse> updateAlias(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody UpdateAliasRequest request) {

        AwsAccount account = awsAccountService.getAccountById(id);

        // Verify ownership
        if (!account.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AwsAccount updated = awsAccountService.updateAccountAlias(id, request.getAlias());
        return ResponseEntity.ok(AwsAccountResponse.fromEntity(updated));
    }

    /**
     * Delete an AWS account connection.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {

        log.info("Deleting AWS account connection: {}", id);
        awsAccountService.deleteAccount(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Request DTO for updating alias.
     */
    @lombok.Data
    public static class UpdateAliasRequest {
        private String alias;
    }
}