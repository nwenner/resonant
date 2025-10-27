import axios from '../lib/api';

export interface AwsAccount {
  id: string;
  accountId: string;
  accountAlias: string;
  roleArn: string;
  credentialType: string;
  status: 'ACTIVE' | 'INACTIVE' | 'ERROR';
  lastSyncedAt: string | null;
  createdAt: string;
}

export interface ExternalIdResponse {
  externalId: string;
  instructions: string;
}

export interface TestConnectionResponse {
  success: boolean;
  message: string;
  accountId: string;
  assumedRoleArn: string;
  availableRegionCount: number;
}

export interface CreateAccountRequest {
  accountId: string;
  accountAlias: string;
  roleArn: string;
  externalId: string;
}

export interface UpdateAliasRequest {
  accountAlias: string;
}

export const awsAccountsApi = {
  /**
   * Generate a new External ID for account connection
   */
  async generateExternalId(): Promise<ExternalIdResponse> {
    const { data } = await axios.post<ExternalIdResponse>('/aws-accounts/external-id');
    return data;
  },

  /**
   * List all connected AWS accounts
   */
  async listAccounts(): Promise<AwsAccount[]> {
    const { data } = await axios.get<AwsAccount[]>('/aws-accounts');
    return data;
  },

  /**
   * Get details for a specific AWS account
   */
  async getAccount(accountId: string): Promise<AwsAccount> {
    const { data } = await axios.get<AwsAccount>(`/aws-accounts/${accountId}`);
    return data;
  },

  /**
   * Connect a new AWS account using IAM role
   */
  async createAccount(request: CreateAccountRequest): Promise<AwsAccount> {
    const { data } = await axios.post<AwsAccount>('/aws-accounts/role', request);
    return data;
  },

  /**
   * Test connection to an AWS account
   */
  async testConnection(accountId: string): Promise<TestConnectionResponse> {
    const { data } = await axios.post<TestConnectionResponse>(
      `/api/aws-accounts/${accountId}/test`
    );
    return data;
  },

  /**
   * Update the alias for an AWS account
   */
  async updateAlias(accountId: string, request: UpdateAliasRequest): Promise<AwsAccount> {
    const { data } = await axios.patch<AwsAccount>(
      `/api/aws-accounts/${accountId}/alias`,
      request
    );
    return data;
  },

  /**
   * Delete an AWS account connection
   */
  async deleteAccount(accountId: string): Promise<void> {
    await axios.delete(`/api/aws-accounts/${accountId}`);
  },
};