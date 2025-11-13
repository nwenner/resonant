import {useMutation, useQueryClient} from '@tanstack/react-query';
import {useToast} from '@/hooks/useToast';
import {AxiosError} from 'axios';
import {awsAccountsService} from '@/services/awsAccountsService';
import {QUERY_KEYS} from '@/constants/queryKeys';

export const useAccountOperations = () => {
  const {toast} = useToast();
  const queryClient = useQueryClient();

  const testConnection = useMutation({
    mutationFn: async (accountId: string) => {
      return await awsAccountsService.testConnection(accountId);
    },
    onSuccess: (data) => {
      toast({
        title: 'Connection Test Successful',
        description: `Connected to account ${data.accountId} with access to ${data.availableRegionCount} regions`
      });
      queryClient.invalidateQueries({queryKey: QUERY_KEYS.awsAccounts.all});
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Connection Test Failed',
        description: error.response?.data?.message || 'Unable to connect to AWS account',
        variant: 'destructive'
      });
    }
  });

  const updateAlias = useMutation({
    mutationFn: async ({id, alias}: { id: string; alias: string }) => {
      return await awsAccountsService.updateAlias(id, {accountAlias: alias});
    },
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: QUERY_KEYS.awsAccounts.all});
      toast({
        title: 'Success',
        description: 'Account alias updated'
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to update alias',
        variant: 'destructive'
      });
    }
  });

  const deleteAccount = useMutation({
    mutationFn: async (accountId: string) => {
      await awsAccountsService.deleteAccount(accountId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: QUERY_KEYS.awsAccounts.all});
      toast({
        title: 'Success',
        description: 'AWS account disconnected'
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to delete account',
        variant: 'destructive'
      });
    }
  });

  return {
    testConnection,
    updateAlias,
    deleteAccount
  };
};