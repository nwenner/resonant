package com.wenroe.resonant.dto.aws;

import lombok.Data;

@Data
public class ConnectionTestResponse {
    private boolean success;
    private String message;
    private String errorMessage;
    private String accountId;
    private String assumedRoleArn;
    private String userId;
    private Integer availableRegionCount;
}