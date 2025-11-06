package com.wenroe.resonant.dto.policy;

import com.wenroe.resonant.model.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateTagPolicyRequest {

    @NotBlank(message = "Policy name is required")
    private String name;

    private String description;

    /**
     * Required tags with optional allowed values.
     * Example: {"Environment": ["dev", "staging", "prod"], "Owner": null, "CostCenter": ["eng", "sales"]}
     * null value means any value is acceptable, just the tag must exist.
     */
    @NotNull(message = "Required tags must be specified")
    @NotEmpty(message = "At least one required tag must be specified")
    private Map<String, List<String>> requiredTags;

    /**
     * AWS resource types this policy applies to.
     * Example: ["ec2:instance", "s3:bucket", "rds:db-instance", "lambda:function"]
     */
    @NotNull(message = "Resource types must be specified")
    @NotEmpty(message = "At least one resource type must be specified")
    private List<String> resourceTypes;

    @NotNull(message = "Severity must be specified")
    private Severity severity;

    private Boolean enabled = true;
}