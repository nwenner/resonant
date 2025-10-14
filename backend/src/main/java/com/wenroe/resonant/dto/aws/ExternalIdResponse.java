package com.wenroe.resonant.dto.aws;

import lombok.Data;

@Data
public class ExternalIdResponse {
    private String externalId;
    private String instructions;

    public ExternalIdResponse(String externalId) {
        this.externalId = externalId;
        this.instructions = "Use this External ID when creating your IAM role in AWS. " +
                "This prevents the 'confused deputy' security issue.";
    }
}