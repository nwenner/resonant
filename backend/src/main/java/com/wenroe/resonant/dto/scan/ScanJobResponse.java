package com.wenroe.resonant.dto.scan;

import com.wenroe.resonant.model.entity.ScanJob;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ScanJobResponse {

    private UUID id;
    private UUID accountId;
    private String accountAlias;
    private String status;
    private Integer resourcesScanned;
    private Integer violationsFound;
    private Integer violationsResolved;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationSeconds;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static ScanJobResponse fromEntity(ScanJob scanJob) {
        ScanJobResponse response = new ScanJobResponse();
        response.setId(scanJob.getId());
        response.setAccountId(scanJob.getAwsAccount().getId());
        response.setAccountAlias(scanJob.getAwsAccount().getAccountAlias());
        response.setStatus(scanJob.getStatus().name());
        response.setResourcesScanned(scanJob.getResourcesScanned());
        response.setViolationsFound(scanJob.getViolationsFound());
        response.setViolationsResolved(scanJob.getViolationsResolved());
        response.setStartedAt(scanJob.getStartedAt());
        response.setCompletedAt(scanJob.getCompletedAt());
        response.setDurationSeconds(scanJob.getDurationSeconds());
        response.setErrorMessage(scanJob.getErrorMessage());
        response.setCreatedAt(scanJob.getCreatedAt());
        return response;
    }
}