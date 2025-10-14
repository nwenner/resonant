package com.wenroe.resonant.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "compliance_violations")
@Data
public class ComplianceViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aws_resource_id", nullable = false)
    private AwsResource awsResource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_policy_id", nullable = false)
    private TagPolicy tagPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false)
    private ViolationType violationType;

    /**
     * List of missing or invalid tag keys
     * Format: ["Environment", "Owner", "CostCenter"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_tags", columnDefinition = "jsonb")
    private List<String> missingTags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagPolicy.Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "remediation_action", columnDefinition = "TEXT")
    private String remediationAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    public enum ViolationType {
        MISSING_TAG,    // Required tag is missing
        INVALID_VALUE,  // Tag exists but value not in allowed list
        UNTAGGED        // Resource has no tags at all
    }

    public enum Status {
        OPEN,        // Violation detected and not resolved
        REMEDIATED,  // Tags have been added/fixed
        IGNORED,     // User chose to ignore this violation
        RESOLVED     // Violation no longer applies (policy changed, resource deleted, etc)
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public void resolve(User user, String action) {
        this.status = Status.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = user;
        this.remediationAction = action;
    }

    public void ignore(User user) {
        this.status = Status.IGNORED;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = user;
        this.remediationAction = "Violation ignored by user";
    }
}