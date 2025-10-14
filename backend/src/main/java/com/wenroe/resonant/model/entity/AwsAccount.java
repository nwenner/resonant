package com.wenroe.resonant.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "aws_accounts")
@Data
public class AwsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_id", nullable = false, length = 12)
    private String accountId;

    @Column(name = "account_alias", length = 100)
    private String accountAlias;

    @Column(name = "role_arn", length = 512)
    private String roleArn;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "access_key_encrypted", columnDefinition = "TEXT")
    private String accessKeyEncrypted;

    @Column(name = "secret_key_encrypted", columnDefinition = "TEXT")
    private String secretKeyEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false)
    private CredentialType credentialType = CredentialType.ROLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "last_scan_at")
    private LocalDateTime lastScanAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum CredentialType {
        ROLE,        // Cross-account IAM role (recommended)
        ACCESS_KEY   // IAM user access keys (encrypted)
    }

    public enum Status {
        ACTIVE,      // Account is working and can be scanned
        INVALID,     // Credentials are invalid
        EXPIRED,     // Credentials have expired
        TESTING      // Initial connection test in progress
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public boolean usesRole() {
        return credentialType == CredentialType.ROLE;
    }
}