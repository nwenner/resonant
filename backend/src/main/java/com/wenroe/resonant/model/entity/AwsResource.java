package com.wenroe.resonant.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "aws_resources")
@Data
public class AwsResource {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "aws_account_id", nullable = false)
  private AwsAccount awsAccount;

  @Column(name = "resource_id", nullable = false)
  private String resourceId;

  @Column(name = "resource_arn", nullable = false, unique = true, length = 512)
  private String resourceArn;

  @Column(name = "resource_type", nullable = false, length = 50)
  private String resourceType;

  @Column(nullable = false, length = 20)
  private String region;

  @Column(length = 255)
  private String name;

  /**
   * AWS tags as key-value pairs Format: {"Environment": "prod", "Owner": "john@example.com",
   * "CostCenter": "engineering"}
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column
  private Map<String, String> tags;

  /**
   * Additional resource-specific metadata Format varies by resource type: EC2: {"instanceType":
   * "t3.micro", "state": "running", "publicIp": "1.2.3.4"} S3: {"versioning": true, "encryption":
   * "AES256"} RDS: {"engine": "postgres", "engineVersion": "14.5"}
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column
  private Map<String, Object> metadata;

  /**
   * Compliance violations for this resource. Cascade delete ensures violations are removed when
   * resource is deleted.
   *
   * @OnDelete annotation adds database-level CASCADE constraint when Hibernate generates DDL.
   */
  @OneToMany(mappedBy = "awsResource", cascade = CascadeType.ALL, orphanRemoval = true)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private List<ComplianceViolation> violations = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "discovered_at", nullable = false, updatable = false)
  private LocalDateTime discoveredAt;

  @CreationTimestamp
  @Column(name = "last_seen_at", nullable = false)
  private LocalDateTime lastSeenAt;

  public boolean hasTag(String tagKey) {
    return tags != null && tags.containsKey(tagKey);
  }

  public String getTagValue(String tagKey) {
    return tags != null ? tags.get(tagKey) : null;
  }

  public boolean isTagged() {
    return tags != null && !tags.isEmpty();
  }

  public int getTagCount() {
    return tags != null ? tags.size() : 0;
  }

  public void updateLastSeen() {
    this.lastSeenAt = LocalDateTime.now();
  }
}