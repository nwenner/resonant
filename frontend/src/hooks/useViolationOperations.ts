import {useMutation, useQueryClient} from '@tanstack/react-query';
import {useToast} from '@/hooks/useToast';
import {violationService} from '@/services/violationService';
import {QUERY_KEYS} from '@/constants/queryKeys';

export const useViolationOperations = () => {
  const {toast} = useToast();
  const queryClient = useQueryClient();

  const ignoreViolation = useMutation({
    mutationFn: violationService.ignoreViolation,
    onSuccess: (data) => {
      // Invalidate all violation queries
      queryClient.invalidateQueries({queryKey: QUERY_KEYS.violations.all});
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.violations.byAccount(data.resourceId)
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.violations.stats
      });

      toast({
        title: 'Violation Ignored',
        description: 'The violation has been marked as ignored',
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to ignore violation',
        variant: 'destructive',
      });
    },
  });

  const reopenViolation = useMutation({
    mutationFn: violationService.reopenViolation,
    onSuccess: (data) => {
      // Invalidate all violation queries
      queryClient.invalidateQueries({queryKey: QUERY_KEYS.violations.all});
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.violations.byAccount(data.resourceId)
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.violations.stats
      });

      toast({
        title: 'Violation Reopened',
        description: 'The violation has been reopened',
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to reopen violation',
        variant: 'destructive',
      });
    },
  });

  return {ignoreViolation, reopenViolation};
};