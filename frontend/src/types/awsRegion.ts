export interface AwsRegion {
  id: string;
  regionCode: string;
  enabled: boolean;
  lastScanAt: string | null;
  createdAt: string;
}