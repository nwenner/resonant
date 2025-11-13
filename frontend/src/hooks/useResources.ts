import { useQuery } from '@tanstack/react-query';
import { resourceService } from '@/services/resourceService';
import { QUERY_KEYS } from '@/constants/queryKeys';

/**
 * Query hook to fetch all resources with optional type filter
 */
export const useResources = (params?: { type?: string }) => {
  return useQuery({
    queryKey: QUERY_KEYS.resources.list(params),
    queryFn: () => resourceService.listResources(params),
  });
};

/**
 * Query hook to fetch a specific resource by ID
 */
export const useResource = (resourceId: string, enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.resources.detail(resourceId),
    queryFn: () => resourceService.getResource(resourceId),
    enabled: enabled && !!resourceId,
  });
};

/**
 * Query hook to fetch resources for a specific account
 */
export const useAccountResources = (accountId: string, enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.resources.byAccount(accountId),
    queryFn: () => resourceService.getAccountResources(accountId),
    enabled: enabled && !!accountId,
  });
};

/**
 * Query hook to fetch resource statistics
 */
export const useResourceStats = () => {
  return useQuery({
    queryKey: QUERY_KEYS.resources.stats,
    queryFn: resourceService.getResourceStats,
  });
};