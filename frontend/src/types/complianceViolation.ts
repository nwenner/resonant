import {Severity} from "@/types/severity";

export interface ComplianceViolation {
  id: string;
  resourceId: string;
  resourceArn: string;
  resourceType: string;
  resourceName: string;
  policyId: string;
  policyName: string;
  severity: Severity;
  status: ViolationStatus;
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

export interface ViolationStats {
  totalOpen: number;
  bySeverity: {
    LOW?: number;
    MEDIUM?: number;
    HIGH?: number;
    CRITICAL?: number;
  };
}

export type ViolationStatus = 'OPEN' | 'RESOLVED' | 'IGNORED';