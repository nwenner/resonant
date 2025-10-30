package com.wenroe.resonant.dto.violation;

import com.wenroe.resonant.model.entity.ComplianceViolation;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class ViolationResponse {

    private UUID id;
    private UUID resourceId;
    private String resourceArn;
    private String resourceType;
    private String resourceName;
    private UUID policyId;
    private String policyName;
    private String severity;
    private String status;
    private Map<String, Object> violationDetails;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime updatedAt;

    public static ViolationResponse fromEntity(ComplianceViolation violation) {
        ViolationResponse response = new ViolationResponse();
        response.setId(violation.getId());
        response.setResourceId(violation.getAwsResource().getId());
        response.setResourceArn(violation.getAwsResource().getResourceArn());
        response.setResourceType(violation.getAwsResource().getResourceType());
        response.setResourceName(violation.getAwsResource().getName());
        response.setPolicyId(violation.getTagPolicy().getId());
        response.setPolicyName(violation.getTagPolicy().getName());
        response.setSeverity(violation.getTagPolicy().getSeverity().name());
        response.setStatus(violation.getStatus().name());
        response.setViolationDetails(violation.getViolationDetails());
        response.setDetectedAt(violation.getDetectedAt());
        response.setResolvedAt(violation.getResolvedAt());
        response.setUpdatedAt(violation.getUpdatedAt());
        return response;
    }
}