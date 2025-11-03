import api from '@/lib/api';
import { ComplianceViolation, ViolationStats } from '@/types/scan';

export const violationService = {
  /**
   * List violations with optional status filter
   */
  listViolations: async (params?: { status?: string }): Promise<ComplianceViolation[]> => {
    const response = await api.get<ComplianceViolation[]>('/violations', { params });
    return response.data;
  },

  /**
   * Get a specific violation by ID
   */
  getViolation: async (violationId: string): Promise<ComplianceViolation> => {
    const response = await api.get<ComplianceViolation>(`/violations/${violationId}`);
    return response.data;
  },

  /**
   * Get violations for a specific account
   */
  getAccountViolations: async (accountId: string): Promise<ComplianceViolation[]> => {
    const response = await api.get<ComplianceViolation[]>(`/violations/accounts/${accountId}`);
    return response.data;
  },

  /**
   * Get violations for a specific resource
   */
  getResourceViolations: async (resourceId: string): Promise<ComplianceViolation[]> => {
    const response = await api.get<ComplianceViolation[]>(`/violations/resources/${resourceId}`);
    return response.data;
  },

  /**
   * Get violations for a specific policy
   */
  getPolicyViolations: async (policyId: string): Promise<ComplianceViolation[]> => {
    const response = await api.get<ComplianceViolation[]>(`/violations/policies/${policyId}`);
    return response.data;
  },

  /**
   * Ignore a violation
   */
  ignoreViolation: async (violationId: string): Promise<ComplianceViolation> => {
    const response = await api.post<ComplianceViolation>(`/violations/${violationId}/ignore`);
    return response.data;
  },

  /**
   * Reopen a violation
   */
  reopenViolation: async (violationId: string): Promise<ComplianceViolation> => {
    const response = await api.post<ComplianceViolation>(`/violations/${violationId}/reopen`);
    return response.data;
  },

  /**
   * Get violation statistics
   */
  getViolationStats: async (): Promise<ViolationStats> => {
    const response = await api.get<ViolationStats>('/violations/stats');
    return response.data;
  },
};