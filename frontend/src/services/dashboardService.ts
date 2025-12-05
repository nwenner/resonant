import api from '@/lib/api';
import type {ComplianceRate} from '@/types/complianceRate';

export const dashboardService = {
  /**
   * Get compliance rate statistics for the authenticated user.
   * Returns total resources, compliant/non-compliant counts, and compliance percentage.
   */
  getComplianceRate: async (): Promise<ComplianceRate> => {
    const response = await api.get<ComplianceRate>('/dashboard/compliance-rate');
    return response.data;
  },
};