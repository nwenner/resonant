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
@Table(name = "scan_jobs")
@Data
public class ScanJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aws_account_id", nullable = false)
    private AwsAccount awsAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * AWS regions scanned in this job
     * Format: ["us-east-1", "us-west-2", "eu-west-1"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> regions;

    /**
     * Resource types scanned
     * Format: ["ec2:instance", "s3:bucket", "rds:db-instance"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_types", nullable = false, columnDefinition = "jsonb")
    private List<String> resourceTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "resources_scanned", nullable = false)
    private Integer resourcesScanned = 0;

    @Column(name = "violations_found", nullable = false)
    private Integer violationsFound = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum Status {
        PENDING,    // Job created but not started
        RUNNING,    // Currently scanning
        COMPLETED,  // Successfully completed
        FAILED,     // Failed with error
        CANCELLED   // Cancelled by user
    }

    public void start() {
        this.status = Status.RUNNING;
    }

    public void complete(int resourcesScanned, int violationsFound) {
        this.status = Status.COMPLETED;
        this.resourcesScanned = resourcesScanned;
        this.violationsFound = violationsFound;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }
}