import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {awsAccountsService} from '@/services/awsAccountsService';
import type {AwsRegion} from '@/types/awsRegion';

export const useAccountRegions = (accountId: string) => {
  return useQuery<AwsRegion[]>({
    queryKey: ['account-regions', accountId],
    queryFn: () => awsAccountsService.getRegions(accountId),
    enabled: !!accountId,
  });
};

export const useUpdateRegions = (accountId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (enabledRegionCodes: string[]) =>
        awsAccountsService.updateRegions(accountId, {enabledRegionCodes}),
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: ['account-regions', accountId]});
      queryClient.invalidateQueries({queryKey: ['aws-account', accountId]});
    },
  });
};

export const useRediscoverRegions = (accountId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => awsAccountsService.rediscoverRegions(accountId),
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: ['account-regions', accountId]});
    },
  });
};