package com.wenroe.resonant.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_job_id")
    private ScanJob scanJob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationStatus status = ViolationStatus.OPEN;

    /**
     * Details about what tags are missing or invalid.
     * Format: {
     *   "missingTags": ["Environment", "Owner"],
     *   "invalidTags": {
     *     "CostCenter": {
     *       "current": "marketing",
     *       "allowed": ["eng", "sales", "ops"]
     *     }
     *   }
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "violation_details", nullable = false)
    private Map<String, Object> violationDetails;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ViolationStatus {
        OPEN,      // Active violation
        RESOLVED,  // Resource is now compliant
        IGNORED    // User manually ignored this violation
    }

    public boolean isOpen() {
        return status == ViolationStatus.OPEN;
    }

    public void resolve() {
        this.status = ViolationStatus.OPEN;
        this.resolvedAt = LocalDateTime.now();
    }

    public void ignore() {
        this.status = ViolationStatus.IGNORED;
    }

    public void reopen() {
        this.status = ViolationStatus.OPEN;
        this.resolvedAt = null;
    }
}