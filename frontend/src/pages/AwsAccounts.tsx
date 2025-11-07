// src/pages/AwsAccounts.tsx
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { awsAccountsService, AwsAccount } from '@/services/awsAccountsService';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { useToast } from '@/hooks/useToast';
import { AddAccountWizard } from '@/components/aws-accounts/AddAccountWizard';
import {
  Plus,
  RefreshCw,
  Trash2,
  Edit2,
  Check,
  Cloud,
  CheckCircle2,
  XCircle,
  AlertCircle
} from 'lucide-react';

export const AwsAccounts = () => {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // State
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [editingAccountId, setEditingAccountId] = useState<string | null>(null);
  const [newAlias, setNewAlias] = useState('');

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
      setEditingAccountId(null);
      setNewAlias('');
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

  // Handlers
  const handleUpdateAlias = (accountId: string) => {
    if (!newAlias.trim()) {
      return;
    }
    updateAliasMutation.mutate({ id: accountId, alias: newAlias.trim() });
  };

  const getStatusBadge = (status: string) => {
    const statusConfig = {
      ACTIVE: {
        variant: 'default' as const,
        className: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
        icon: CheckCircle2
      },
      INVALID: {
        variant: 'destructive' as const,
        className: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
        icon: XCircle
      },
      EXPIRED: {
        variant: 'secondary' as const,
        className: 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400',
        icon: AlertCircle
      },
      TESTING: {
        variant: 'outline' as const,
        className: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
        icon: RefreshCw
      }
    };

    const config = statusConfig[status as keyof typeof statusConfig] || statusConfig.TESTING;
    const Icon = config.icon;

    return (
      <Badge variant={config.variant} className={config.className}>
        <Icon className="w-3 h-3 mr-1" />
        {status}
      </Badge>
    );
  };

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
            <Card
              key={account.id}
              className="hover:shadow-lg transition-shadow cursor-pointer"
              onClick={() => navigate(`/aws-accounts/${account.id}`)}
            >
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    {editingAccountId === account.id ? (
                      <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                        <Input
                          value={newAlias || ''}
                          onChange={(e) => setNewAlias(e.target.value)}
                          className="h-8"
                          placeholder="Account alias"
                        />
                        <Button
                          size="sm"
                          onClick={() => handleUpdateAlias(account.id)}
                          disabled={updateAliasMutation.isPending}
                        >
                          <Check className="w-4 h-4" />
                        </Button>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2">
                        <CardTitle className="text-lg">{account.accountAlias}</CardTitle>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            setEditingAccountId(account.id);
                            setNewAlias(account.accountAlias || '');
                          }}
                        >
                          <Edit2 className="w-3 h-3" />
                        </Button>
                      </div>
                    )}
                    <CardDescription className="font-mono text-xs">{account.accountId}</CardDescription>
                  </div>
                  {getStatusBadge(account.status)}
                </div>
              </CardHeader>
              <CardContent className="space-y-3">
                <div>
                  <Label className="text-xs text-slate-500 dark:text-slate-400">Role ARN</Label>
                  <p className="text-sm font-mono truncate text-slate-900 dark:text-white" title={account.roleArn}>
                    {account.roleArn}
                  </p>
                </div>
                {account.lastSyncedAt && (
                  <div>
                    <Label className="text-xs text-slate-500 dark:text-slate-400">Last Synced</Label>
                    <p className="text-sm text-slate-900 dark:text-white">
                      {new Date(account.lastSyncedAt).toLocaleString()}
                    </p>
                  </div>
                )}
              </CardContent>
              <CardFooter className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    testConnectionMutation.mutate(account.id);
                  }}
                  disabled={testConnectionMutation.isPending}
                  className="flex-1"
                >
                  <RefreshCw className={`w-3 h-3 mr-1 ${testConnectionMutation.isPending ? 'animate-spin' : ''}`} />
                  Test
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (confirm('Are you sure you want to disconnect this account?')) {
                      deleteAccountMutation.mutate(account.id);
                    }
                  }}
                  disabled={deleteAccountMutation.isPending}
                  className="text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20"
                >
                  <Trash2 className="w-3 h-3" />
                </Button>
              </CardFooter>
            </Card>
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