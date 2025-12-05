// Add to types/index.ts or create types/dashboard.ts

export interface ComplianceRate {
  totalResources: number;
  compliantResources: number;
  nonCompliantResources: number;
  complianceRate: number;
}