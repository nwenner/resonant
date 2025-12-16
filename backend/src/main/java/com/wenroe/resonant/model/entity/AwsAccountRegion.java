package com.wenroe.resonant.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "aws_account_regions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"aws_account_id", "region_code"}))
@Data
public class AwsAccountRegion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "aws_account_id", nullable = false)
  private AwsAccount awsAccount;

  @Column(name = "region_code", nullable = false, length = 20)
  private String regionCode;

  @Column(nullable = false)
  private Boolean enabled = true;

  @Column(name = "last_scan_at")
  private LocalDateTime lastScanAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public void updateLastScan() {
    this.lastScanAt = LocalDateTime.now();
  }
}