package com.wenroe.resonant.dto.aws;

import com.wenroe.resonant.service.aws.AwsConnectionTester;
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

    public static ConnectionTestResponse fromResult(AwsConnectionTester.ConnectionTestResult result) {
        ConnectionTestResponse response = new ConnectionTestResponse();
        response.setSuccess(result.isSuccess());
        response.setMessage(result.getMessage());
        response.setErrorMessage(result.getErrorMessage());
        response.setAccountId(result.getAccountId());
        response.setAssumedRoleArn(result.getAssumedRoleArn());
        response.setUserId(result.getUserId());
        response.setAvailableRegionCount(result.getAvailableRegionCount());
        return response;
    }
}