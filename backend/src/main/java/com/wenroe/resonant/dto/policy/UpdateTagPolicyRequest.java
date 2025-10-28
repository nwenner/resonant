package com.wenroe.resonant.dto.policy;

import com.wenroe.resonant.model.entity.TagPolicy;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UpdateTagPolicyRequest {

    private String name;
    private String description;
    private Map<String, List<String>> requiredTags;
    private List<String> resourceTypes;
    private TagPolicy.Severity severity;
    private Boolean enabled;
}