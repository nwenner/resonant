import {useQuery} from '@tanstack/react-query';
import {tagPolicyService} from '@/services/tagPolicyService';
import {QUERY_KEYS} from '@/constants/queryKeys';

/**
 * Query hook to fetch tag policy statistics
 */
export const useTagPolicyStats = () => {
  return useQuery({
    queryKey: QUERY_KEYS.tagPolicies.stats,
    queryFn: tagPolicyService.getStats
  });
};