// Scan Job Types
export interface ScanJob {
  id: string;
  accountId: string;
  accountAlias: string;
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
  resourcesScanned: number;
  violationsFound: number;
  violationsResolved: number;
  startedAt: string;
  completedAt: string | null;
  durationSeconds: number | null;
  errorMessage: string | null;
  createdAt: string;
}

// Compliance Violation Types
export interface ComplianceViolation {
  id: string;
  resourceId: string;
  resourceArn: string;
  resourceType: string;
  resourceName: string;
  policyId: string;
  policyName: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'OPEN' | 'RESOLVED' | 'IGNORED';
  violationDetails: {
    missingTags?: string[];
    invalidTags?: {
      [tagKey: string]: {
        current: string;
        allowed: string[];
      };
    };
  };
  detectedAt: string;
  resolvedAt: string | null;
  updatedAt: string;
}

// AWS Resource Types
export interface AwsResource {
  id: string;
  resourceId: string;
  resourceArn: string;
  resourceType: string;
  region: string;
  name: string;
  tags: Record<string, string>;
  metadata: Record<string, any>;
  tagCount: number;
  discoveredAt: string;
  lastSeenAt: string;
}

// Statistics Types
export interface ViolationStats {
  totalOpen: number;
  bySeverity: {
    LOW?: number;
    MEDIUM?: number;
    HIGH?: number;
    CRITICAL?: number;
  };
}

export interface ResourceStats {
  total: number;
  byType: Record<string, number>;
}