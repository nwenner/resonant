package com.wenroe.resonant.dto.policy;

import com.wenroe.resonant.model.entity.TagPolicy;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class TagPolicyResponse {

    private UUID id;
    private String name;
    private String description;
    private Map<String, List<String>> requiredTags;
    private List<String> resourceTypes;
    private String severity;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TagPolicyResponse fromEntity(TagPolicy policy) {
        TagPolicyResponse response = new TagPolicyResponse();
        response.setId(policy.getId());
        response.setName(policy.getName());
        response.setDescription(policy.getDescription());
        response.setRequiredTags(policy.getRequiredTags());
        response.setResourceTypes(policy.getResourceTypes());
        response.setSeverity(policy.getSeverity().name());
        response.setEnabled(policy.getEnabled());
        response.setCreatedAt(policy.getCreatedAt());
        response.setUpdatedAt(policy.getUpdatedAt());
        return response;
    }
}