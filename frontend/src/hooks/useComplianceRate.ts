import {useQuery} from '@tanstack/react-query';
import {dashboardService} from '@/services/dashboardService';
import type {ComplianceRate} from '@/types/complianceRate';

/**
 * Hook to fetch compliance rate statistics.
 * Auto-refreshes every 30 seconds to keep data current.
 */
export const useComplianceRate = () => {
  return useQuery<ComplianceRate>({
    queryKey: ['compliance-rate'],
    queryFn: () => dashboardService.getComplianceRate(),
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};