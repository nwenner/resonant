import {useQuery} from '@tanstack/react-query';
import {scanService} from '@/services/scanService';
import {QUERY_KEYS} from '@/constants/queryKeys';

/**
 * Query hook to fetch all scans
 */
export const useScans = () => {
  return useQuery({
    queryKey: QUERY_KEYS.scans.all,
    queryFn: scanService.listScans,
  });
};

/**
 * Query hook to fetch a specific scan job by ID
 */
export const useScanJob = (scanJobId: string, enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.scans.detail(scanJobId),
    queryFn: () => scanService.getScanJob(scanJobId),
    enabled: enabled && !!scanJobId,
  });
};

/**
 * Query hook to fetch scans for a specific account
 */
export const useAccountScans = (accountId: string, enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.scans.byAccount(accountId),
    queryFn: () => scanService.getAccountScans(accountId),
    enabled: enabled && !!accountId,
  });
};

/**
 * Query hook to fetch the latest scan for an account
 */
export const useLatestScan = (accountId: string, enabled = true) => {
  const query = useQuery({
    queryKey: QUERY_KEYS.scans.latest(accountId),
    queryFn: () => scanService.getLatestScan(accountId),
    enabled: enabled && !!accountId,
  });

  return {
    ...query,
    data: query.data ?? null,
  };
};