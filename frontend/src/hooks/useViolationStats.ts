import {useQuery} from '@tanstack/react-query';
import {violationService} from '@/services/violationService';
import type {ViolationStats} from '@/types/complianceViolation';

export const useViolationStats = () => {
  return useQuery<ViolationStats>({
    queryKey: ['violation-stats'],
    queryFn: () => violationService.getViolationStats(),
    refetchInterval: 30000, // Refetch every 30 seconds
  });
};