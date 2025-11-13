import api from '@/lib/api';
import {AwsResource, ResourceStats} from '@/types/scan';

export const resourceService = {
  /**
   * List resources with optional type filter
   */
  listResources: async (params?: { type?: string }): Promise<AwsResource[]> => {
    const response = await api.get<AwsResource[]>('/resources', {params});
    return response.data;
  },

  /**
   * Get a specific resource by ID
   */
  getResource: async (resourceId: string): Promise<AwsResource> => {
    const response = await api.get<AwsResource>(`/resources/${resourceId}`);
    return response.data;
  },

  /**
   * Get resources for a specific account
   */
  getAccountResources: async (accountId: string): Promise<AwsResource[]> => {
    const response = await api.get<AwsResource[]>(`/resources/accounts/${accountId}`);
    return response.data;
  },

  /**
   * Get resource statistics
   */
  getResourceStats: async (): Promise<ResourceStats> => {
    const response = await api.get<ResourceStats>('/resources/stats');
    return response.data;
  },
};