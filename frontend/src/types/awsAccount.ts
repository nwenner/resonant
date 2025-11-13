export interface AwsAccount {
  id: string;
  accountId: string;
  accountAlias: string;
  roleArn: string;
  credentialType: string;
  status: AccountInstrumentationStatus;
  lastSyncedAt: string | null;
  createdAt: string;
}

export type AccountInstrumentationStatus = 'ACTIVE' | 'INVALID' | 'EXPIRED' | 'TESTING';