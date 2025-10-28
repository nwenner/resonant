package com.wenroe.resonant.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tag_policies")
@Data
public class TagPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    /**
     * Required tags with optional allowed values
     * Format: {"Environment": ["dev", "staging", "prod"], "Owner": null, "CostCenter": ["eng", "sales"]}
     * null value = any value is acceptable, just tag must exist
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_tags", nullable = false)
    private Map<String, List<String>> requiredTags;

    /**
     * AWS resource types this policy applies to
     * Format: ["ec2:instance", "s3:bucket", "rds:db-instance", "lambda:function"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_types", nullable = false)
    private List<String> resourceTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.MEDIUM;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public boolean appliesToResourceType(String resourceType) {
        return resourceTypes.contains(resourceType);
    }

    public boolean isTagRequired(String tagKey) {
        return requiredTags.containsKey(tagKey);
    }

    public List<String> getAllowedValuesForTag(String tagKey) {
        return requiredTags.get(tagKey);
    }

    public boolean acceptsAnyValueForTag(String tagKey) {
        return requiredTags.get(tagKey) == null;
    }
}