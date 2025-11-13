export interface ScanJob {
  id: string;
  accountId: string;
  accountAlias: string;
  status: ScanStatus;
  resourcesScanned: number;
  violationsFound: number;
  violationsResolved: number;
  startedAt: string;
  completedAt: string | null;
  durationSeconds: number | null;
  errorMessage: string | null;
  createdAt: string;
}

export type ScanStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';