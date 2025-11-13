import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@/hooks/useToast';
import { scanService } from '@/services/scanService';
import { QUERY_KEYS } from '@/constants/queryKeys';

export const useScanOperations = () => {
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const triggerScan = useMutation({
    mutationFn: scanService.triggerScan,
    onSuccess: (data) => {
      // Invalidate scan queries for this account
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.scans.byAccount(data.accountId)
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.scans.latest(data.accountId)
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.scans.all
      });

      toast({
        title: 'Scan Started',
        description: 'AWS account scan has been initiated',
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Scan Failed',
        description: error.response?.data?.message || 'Failed to start scan',
        variant: 'destructive',
      });
    },
  });

  return { triggerScan };
};