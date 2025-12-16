import api from '@/lib/api';
import {AwsAccount} from "@/types/awsAccount";
import {AwsRegion} from "@/types/awsRegion";

interface ExternalIdResponse {
  externalId: string;
  instructions: string;
}

interface TestConnectionResponse {
  success: boolean;
  message: string;
  accountId: string;
  assumedRoleArn: string;
  availableRegionCount: number;
}

interface CreateAccountRequest {
  accountAlias: string;
  roleArn: string;
  externalId: string;
}

interface UpdateAliasRequest {
  accountAlias: string;
}

interface UpdateRegionsRequest {
  enabledRegionCodes: string[];
}

// AWS Accounts API Service
export const awsAccountsService = {
  /**
   * Generate a new External ID for account connection
   */
  generateExternalId: async (): Promise<ExternalIdResponse> => {
    const response = await api.post<ExternalIdResponse>('/aws-accounts/external-id');
    return response.data;
  },

  /**
   * List all connected AWS accounts
   */
  listAccounts: async (): Promise<AwsAccount[]> => {
    const response = await api.get<AwsAccount[]>('/aws-accounts');
    return response.data;
  },

  /**
   * Get details for a specific AWS account
   */
  getAccount: async (accountId: string): Promise<AwsAccount> => {
    const response = await api.get<AwsAccount>(`/aws-accounts/${accountId}`);
    return response.data;
  },

  /**
   * Connect a new AWS account using IAM role
   */
  createAccount: async (request: CreateAccountRequest): Promise<AwsAccount> => {
    const response = await api.post<AwsAccount>('/aws-accounts/role', request);
    return response.data;
  },

  /**
   * Test connection to an AWS account
   */
  testConnection: async (accountId: string): Promise<TestConnectionResponse> => {
    const response = await api.post<TestConnectionResponse>(
        `/aws-accounts/${accountId}/test`
    );
    return response.data;
  },

  /**
   * Update the alias for an AWS account
   */
  updateAlias: async (accountId: string, request: UpdateAliasRequest): Promise<AwsAccount> => {
    const response = await api.patch<AwsAccount>(
        `/aws-accounts/${accountId}/alias`,
        request
    );
    return response.data;
  },

  /**
   * Delete an AWS account connection
   */
  deleteAccount: async (accountId: string): Promise<void> => {
    await api.delete(`/aws-accounts/${accountId}`);
  },

  /**
   * Get all regions for an AWS account
   */
  getRegions: async (accountId: string): Promise<AwsRegion[]> => {
    const response = await api.get<AwsRegion[]>(`/aws-accounts/${accountId}/regions`);
    return response.data;
  },

  /**
   * Update enabled regions for an AWS account (bulk update)
   */
  updateRegions: async (accountId: string, request: UpdateRegionsRequest): Promise<AwsRegion[]> => {
    const response = await api.patch<AwsRegion[]>(
        `/aws-accounts/${accountId}/regions`,
        request
    );
    return response.data;
  },

  /**
   * Enable a specific region
   */
  enableRegion: async (accountId: string, regionCode: string): Promise<AwsRegion> => {
    const response = await api.post<AwsRegion>(
        `/aws-accounts/${accountId}/regions/${regionCode}/enable`
    );
    return response.data;
  },

  /**
   * Disable a specific region
   */
  disableRegion: async (accountId: string, regionCode: string): Promise<AwsRegion> => {
    const response = await api.post<AwsRegion>(
        `/aws-accounts/${accountId}/regions/${regionCode}/disable`
    );
    return response.data;
  },

  /**
   * Rediscover regions for an AWS account
   */
  rediscoverRegions: async (accountId: string): Promise<AwsRegion[]> => {
    const response = await api.post<AwsRegion[]>(
        `/aws-accounts/${accountId}/regions/rediscover`
    );
    return response.data;
  },
};