package com.wenroe.resonant.dto.aws;

import com.wenroe.resonant.model.entity.AwsResource;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class AwsResourceResponse {

    private UUID id;
    private String resourceId;
    private String resourceArn;
    private String resourceType;
    private String region;
    private String name;
    private Map<String, String> tags;
    private Map<String, Object> metadata;
    private Integer tagCount;
    private LocalDateTime discoveredAt;
    private LocalDateTime lastSeenAt;

    public static AwsResourceResponse fromEntity(AwsResource resource) {
        AwsResourceResponse response = new AwsResourceResponse();
        response.setId(resource.getId());
        response.setResourceId(resource.getResourceId());
        response.setResourceArn(resource.getResourceArn());
        response.setResourceType(resource.getResourceType());
        response.setRegion(resource.getRegion());
        response.setName(resource.getName());
        response.setTags(resource.getTags());
        response.setMetadata(resource.getMetadata());
        response.setTagCount(resource.getTagCount());
        response.setDiscoveredAt(resource.getDiscoveredAt());
        response.setLastSeenAt(resource.getLastSeenAt());
        return response;
    }
}