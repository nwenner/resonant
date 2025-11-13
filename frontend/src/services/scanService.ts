import api from '@/lib/api';
import {AxiosError} from 'axios';
import {ScanJob} from '@/types/scanJob.ts';

export const scanService = {
  /**
   * Trigger a scan for an AWS account
   */
  triggerScan: async (accountId: string): Promise<ScanJob> => {
    const response = await api.post<ScanJob>(`/scans/accounts/${accountId}`);
    return response.data;
  },

  /**
   * Get scan job status by ID
   */
  getScanJob: async (scanJobId: string): Promise<ScanJob> => {
    const response = await api.get<ScanJob>(`/scans/${scanJobId}`);
    return response.data;
  },

  /**
   * List all scans
   */
  listScans: async (): Promise<ScanJob[]> => {
    const response = await api.get<ScanJob[]>('/scans');
    return response.data;
  },

  /**
   * Get scans for a specific account
   */
  getAccountScans: async (accountId: string): Promise<ScanJob[]> => {
    const response = await api.get<ScanJob[]>(`/scans/accounts/${accountId}`);
    return response.data;
  },

  /**
   * Get latest scan for an account
   */
  getLatestScan: async (accountId: string): Promise<ScanJob | null> => {
    try {
      const response = await api.get<ScanJob>(`/scans/accounts/${accountId}/latest`);
      return response.data;
    } catch (error: unknown) {
      if (error instanceof AxiosError && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },
};