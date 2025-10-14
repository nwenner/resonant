package com.wenroe.resonant.dto.aws;

import com.wenroe.resonant.model.entity.AwsAccount;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AwsAccountResponse {
    private UUID id;
    private String accountId;
    private String accountAlias;
    private String roleArn;
    private String credentialType;
    private String status;
    private LocalDateTime lastScanAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AwsAccountResponse fromEntity(AwsAccount account) {
        AwsAccountResponse response = new AwsAccountResponse();
        response.setId(account.getId());
        response.setAccountId(account.getAccountId());
        response.setAccountAlias(account.getAccountAlias());
        response.setRoleArn(account.getRoleArn());
        response.setCredentialType(account.getCredentialType().name());
        response.setStatus(account.getStatus().name());
        response.setLastScanAt(account.getLastScanAt());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        return response;
    }
}