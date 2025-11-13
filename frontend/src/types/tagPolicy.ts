import {Severity} from "@/types/severity";

export interface TagPolicy {
  id: string;
  name: string;
  description: string;
  requiredTags: Record<string, string[] | null>;
  resourceTypes: string[];
  severity: Severity;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TagPolicyStats {
  total: number;
  enabled: number;
  disabled: number;
}