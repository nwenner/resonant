import {useQuery} from '@tanstack/react-query';
import {violationService} from '@/services/violationService';
import {QUERY_KEYS} from '@/constants/queryKeys';

/**
 * Query hook to fetch all violations with optional status filter
 */
export const useViolations = (params?: { status?: string }) => {
  return useQuery({
    queryKey: QUERY_KEYS.violations.list(params),
    queryFn: () => violationService.listViolations(params),
  });
};

/**
 * Query hook to fetch a specific violation by ID
 */
export const useViolation = (violationId: string, enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.violations.detail(violationId),
    queryFn: () => violationService.getViolation(violationId),
    enabled: enabled && !!violationId,
  });
};

/**
 * Query hook to fetch violations for a specific account
 */
export const useAccountViolations = (accountId: string, enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.violations.byAccount(accountId),
    queryFn: () => violationService.getAccountViolations(accountId),
    enabled: enabled && !!accountId,
  });
};

/**
 * Query hook to fetch violations for a specific resource
 */
export const useResourceViolations = (resourceId: string, enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.violations.byResource(resourceId),
    queryFn: () => violationService.getResourceViolations(resourceId),
    enabled: enabled && !!resourceId,
  });
};

/**
 * Query hook to fetch violations for a specific policy
 */
export const usePolicyViolations = (policyId: string, enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.violations.byPolicy(policyId),
    queryFn: () => violationService.getPolicyViolations(policyId),
    enabled: enabled && !!policyId,
  });
};

/**
 * Query hook to fetch violation statistics
 */
export const useViolationStats = () => {
  return useQuery({
    queryKey: QUERY_KEYS.violations.stats,
    queryFn: violationService.getViolationStats,
  });
};