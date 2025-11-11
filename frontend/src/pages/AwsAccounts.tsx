import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { awsAccountsService, AwsAccount } from '@/services/awsAccountsService';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { useToast } from '@/hooks/useToast';
import { AddAccountWizard } from '@/components/aws-accounts/AddAccountWizard';
import { AccountCard } from '@/components/aws-accounts/AccountCard';
import {
  Plus,
  RefreshCw,
  Cloud
} from 'lucide-react';

export const AwsAccounts = () => {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // State
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);

  // Queries
  const { data: accounts = [], isLoading } = useQuery<AwsAccount[]>({
    queryKey: ['aws-accounts'],
    queryFn: async () => {
      return await awsAccountsService.listAccounts();
    }
  });

  // Mutations
  const testConnectionMutation = useMutation({
    mutationFn: async (accountId: string) => {
      return await awsAccountsService.testConnection(accountId);
    },
    onSuccess: (data) => {
      toast({
        title: 'Connection Test Successful',
        description: `Connected to account ${data.accountId} with access to ${data.availableRegionCount} regions`
      });
      queryClient.invalidateQueries({ queryKey: ['aws-accounts'] });
    },
    onError: (error: any) => {
      toast({
        title: 'Connection Test Failed',
        description: error.response?.data?.message || 'Unable to connect to AWS account',
        variant: 'destructive'
      });
    }
  });

  const updateAliasMutation = useMutation({
    mutationFn: async ({ id, alias }: { id: string; alias: string }) => {
      return await awsAccountsService.updateAlias(id, { accountAlias: alias });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['aws-accounts'] });
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

  const deleteAccountMutation = useMutation({
    mutationFn: async (accountId: string) => {
      await awsAccountsService.deleteAccount(accountId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['aws-accounts'] });
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

  if (isLoading) {
    return (
      <Layout>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="flex items-center justify-center h-64">
            <RefreshCw className="w-8 h-8 animate-spin text-blue-600 dark:text-blue-400" />
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-slate-900 dark:text-white">AWS Accounts</h1>
              <p className="text-slate-600 dark:text-slate-400 mt-1">
                Manage your connected AWS accounts for compliance scanning
              </p>
            </div>
            <Button onClick={() => setIsAddDialogOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              Connect Account
            </Button>
          </div>

        {/* Empty State */}
        {accounts.length === 0 && (
          <Card className="border-dashed">
            <CardContent className="flex flex-col items-center justify-center py-12">
              <Cloud className="w-12 h-12 text-slate-400 dark:text-slate-600 mb-4" />
              <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-2">No AWS Accounts Connected</h3>
              <p className="text-slate-600 dark:text-slate-400 text-center mb-4">
                Connect your first AWS account to start scanning for tag compliance
              </p>
              <Button onClick={() => setIsAddDialogOpen(true)}>
                <Plus className="w-4 h-4 mr-2" />
                Connect Your First Account
              </Button>
            </CardContent>
          </Card>
        )}

        {/* Account Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {accounts.map((account) => (
            <AccountCard
              key={account.id}
              account={account}
              isTestingConnection={testConnectionMutation.isPending}
              isDeletingAccount={deleteAccountMutation.isPending}
              isUpdatingAlias={updateAliasMutation.isPending}
              onCardClick={(accountId) => navigate(`/aws-accounts/${accountId}`)}
              onTestConnection={(accountId) => testConnectionMutation.mutate(accountId)}
              onDeleteAccount={(accountId) => deleteAccountMutation.mutate(accountId)}
              onUpdateAlias={(accountId, newAlias) => updateAliasMutation.mutate({ id: accountId, alias: newAlias })}
            />
          ))}
        </div>

        {/* Add Account Wizard */}
        <AddAccountWizard 
          open={isAddDialogOpen}
          onOpenChange={setIsAddDialogOpen}
        />
        </div>
      </div>
    </Layout>
  );
};