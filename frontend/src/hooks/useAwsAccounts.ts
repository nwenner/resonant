import {useQuery} from '@tanstack/react-query';
import {awsAccountsService} from '@/services/awsAccountsService';
import {QUERY_KEYS} from '@/constants/queryKeys';
import {AwsAccount} from "@/types/awsAccount";

/**
 * Query hook to fetch all AWS accounts
 */
export const useAwsAccounts = () => {
  return useQuery({
    queryKey: QUERY_KEYS.awsAccounts.all,
    queryFn: awsAccountsService.listAccounts
  });
};

/**
 * Query hook to fetch a specific AWS account by ID
 */
export const useAwsAccount = (accountId: string, enabled = true) => {
  return useQuery({
    queryKey: QUERY_KEYS.awsAccounts.detail(accountId),
    queryFn: () => awsAccountsService.getAccount(accountId),
    enabled: enabled && !!accountId
  });
};

/**
 * Query hook to fetch AWS accounts with optional filters
 * Usage: useAwsAccountsFiltered({ status: 'ACTIVE' })
 */
export const useAwsAccountsFiltered = (filters?: Record<string, unknown>) => {
  return useQuery({
    queryKey: QUERY_KEYS.awsAccounts.list(filters),
    queryFn: async () => {
      const accounts = await awsAccountsService.listAccounts();

      if (!filters) return accounts;

      return accounts.filter((account) => {
        return Object.entries(filters).every(([key, value]) => {
          return account[key as keyof AwsAccount] === value;
        });
      });
    },
  });
};