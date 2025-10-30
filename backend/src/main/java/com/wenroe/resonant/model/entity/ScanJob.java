package com.wenroe.resonant.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScanStatus status = ScanStatus.PENDING;

    @Column(name = "resources_scanned", nullable = false)
    private Integer resourcesScanned = 0;

    @Column(name = "violations_found", nullable = false)
    private Integer violationsFound = 0;

    @Column(name = "violations_resolved", nullable = false)
    private Integer violationsResolved = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ScanStatus {
        PENDING,   // Created but not started
        RUNNING,   // Currently scanning
        SUCCESS,   // Completed successfully
        FAILED     // Failed with error
    }

    public void start() {
        this.status = ScanStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void complete(int resourcesScanned, int violationsFound, int violationsResolved) {
        this.status = ScanStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
        this.resourcesScanned = resourcesScanned;
        this.violationsFound = violationsFound;
        this.violationsResolved = violationsResolved;
    }

    public void fail(String errorMessage) {
        this.status = ScanStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public boolean isRunning() {
        return status == ScanStatus.RUNNING;
    }

    public boolean isCompleted() {
        return status == ScanStatus.SUCCESS || status == ScanStatus.FAILED;
    }

    public Long getDurationSeconds() {
        if (startedAt == null || completedAt == null) {
            return null;
        }
        return java.time.Duration.between(startedAt, completedAt).getSeconds();
    }
}