// src/pages/AwsAccounts.tsx
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from '@/lib/api';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useToast } from '@/hooks/useToast';
import { 
  Plus, 
  RefreshCw, 
  Trash2, 
  Edit2, 
  Download, 
  Copy, 
  Check, 
  Cloud,
  Shield,
  CheckCircle2,
  XCircle,
  AlertCircle
} from 'lucide-react';

// Types
interface AwsAccount {
  id: string;
  accountId: string;
  accountAlias: string;
  roleArn: string;
  credentialType: string;
  status: 'ACTIVE' | 'INVALID' | 'EXPIRED' | 'TESTING';
  lastSyncedAt: string | null;
  createdAt: string;
}

interface ExternalIdResponse {
  externalId: string;
  instructions: string;
}

interface TestConnectionResponse {
  success: boolean;
  message: string;
  accountId: string;
  assumedRoleArn: string;
  availableRegionCount: number;
}

const CLOUDFORMATION_TEMPLATE = `AWSTemplateFormatVersion: '2010-09-09'
Description: 'Resonant AWS Account Integration Role'

Parameters:
  ExternalId:
    Type: String
    Description: External ID provided by Resonant
    NoEcho: true

Resources:
  ResonantRole:
    Type: AWS::IAM::Role
    Properties:
      RoleName: ResonantComplianceRole
      AssumeRolePolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Effect: Allow
            Principal:
              AWS: 'arn:aws:iam::YOUR_RESONANT_ACCOUNT:root'
            Action: 'sts:AssumeRole'
            Condition:
              StringEquals:
                'sts:ExternalId': !Ref ExternalId
      ManagedPolicyArns:
        - 'arn:aws:iam::aws:policy/ReadOnlyAccess'
      Policies:
        - PolicyName: ResonantTaggingPolicy
          PolicyDocument:
            Version: '2012-10-17'
            Statement:
              - Effect: Allow
                Action:
                  - 'tag:GetResources'
                  - 'tag:GetTagKeys'
                  - 'tag:GetTagValues'
                  - 'resourcegroupstaggingapi:*'
                Resource: '*'

Outputs:
  RoleArn:
    Description: ARN of the created IAM role
    Value: !GetAtt ResonantRole.Arn
    Export:
      Name: ResonantRoleArn`;

export const AwsAccounts = () => {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  
  // State
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [addStep, setAddStep] = useState(1);
  const [externalId, setExternalId] = useState<string>('');
  const [copiedExternalId, setCopiedExternalId] = useState(false);
  const [formData, setFormData] = useState({
    accountId: '',
    accountAlias: '',
    roleArn: ''
  });
  const [editingAccountId, setEditingAccountId] = useState<string | null>(null);
  const [newAlias, setNewAlias] = useState('');

  // Queries
  const { data: accounts = [], isLoading } = useQuery<AwsAccount[]>({
    queryKey: ['aws-accounts'],
    queryFn: async () => {
      const { data } = await axios.get('/api/aws-accounts');
      return data;
    }
  });

  // Mutations
  const generateExternalIdMutation = useMutation({
    mutationFn: async () => {
      const { data } = await axios.post<ExternalIdResponse>('/api/aws-accounts/external-id');
      return data;
    },
    onSuccess: (data) => {
      setExternalId(data.externalId);
      setAddStep(2);
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to generate External ID',
        variant: 'destructive'
      });
    }
  });

  const addAccountMutation = useMutation({
    mutationFn: async (payload: typeof formData & { externalId: string }) => {
      const { data } = await axios.post('/api/aws-accounts/role', payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['aws-accounts'] });
      toast({
        title: 'Success',
        description: 'AWS account connected successfully'
      });
      handleCloseDialog();
    },
    onError: (error: any) => {
      toast({
        title: 'Connection Failed',
        description: error.response?.data?.message || 'Failed to connect AWS account',
        variant: 'destructive'
      });
    }
  });

  const testConnectionMutation = useMutation({
    mutationFn: async (accountId: string) => {
      const { data } = await axios.post<TestConnectionResponse>(`/api/aws-accounts/${accountId}/test`);
      return data;
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
      const { data } = await axios.patch(`/api/aws-accounts/${id}/alias`, { accountAlias: alias });
      return data;
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
      await axios.delete(`/api/aws-accounts/${accountId}`);
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
  const handleStartAddAccount = () => {
    setIsAddDialogOpen(true);
    setAddStep(1);
    generateExternalIdMutation.mutate();
  };

  const handleCloseDialog = () => {
    setIsAddDialogOpen(false);
    setAddStep(1);
    setExternalId('');
    setFormData({ accountId: '', accountAlias: '', roleArn: '' });
    setCopiedExternalId(false);
  };

  const handleCopyExternalId = async () => {
    await navigator.clipboard.writeText(externalId);
    setCopiedExternalId(true);
    setTimeout(() => setCopiedExternalId(false), 2000);
  };

  const handleDownloadTemplate = () => {
    if (!formData.accountId.trim()) {
      toast({
        title: 'Enter Account ID First',
        description: 'Please enter your AWS Account ID in Step 3 before downloading the template',
        variant: 'destructive'
      });
      return;
    }
    
    // For Phase 1: Use the same account ID (user assumes role in their own account)
    const customizedTemplate = CLOUDFORMATION_TEMPLATE.replace(
      'YOUR_RESONANT_ACCOUNT',
      formData.accountId
    );
    
    const blob = new Blob([customizedTemplate], { type: 'text/yaml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'resonant-role.yaml';
    a.click();
    URL.revokeObjectURL(url);
    
    toast({
      title: 'Template Downloaded',
      description: 'CloudFormation template configured for your account'
    });
  };

  const handleSubmitAccount = () => {
    if (!formData.accountId || !formData.accountAlias || !formData.roleArn) {
      toast({
        title: 'Validation Error',
        description: 'All fields are required',
        variant: 'destructive'
      });
      return;
    }
    addAccountMutation.mutate({ ...formData, externalId });
  };

  const handleUpdateAlias = (accountId: string) => {
    if (!newAlias.trim()) return;
    updateAliasMutation.mutate({ id: accountId, alias: newAlias });
  };

  const getStatusBadge = (status: AwsAccount['status']) => {
    const variants = {
      ACTIVE: { 
        icon: CheckCircle2, 
        text: 'Active', 
        className: 'bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-400' 
      },
      TESTING: { 
        icon: RefreshCw, 
        text: 'Testing', 
        className: 'bg-blue-100 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400',
        animate: true
      },
      INVALID: { 
        icon: XCircle, 
        text: 'Invalid', 
        className: 'bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-400' 
      },
      EXPIRED: { 
        icon: AlertCircle, 
        text: 'Expired', 
        className: 'bg-amber-100 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400' 
      }
    };
    
    // Default to INVALID if status is unexpected
    const config = variants[status] || variants.INVALID;
    const Icon = config.icon;
    
    return (
      <Badge className={config.className}>
        <Icon className={`w-3 h-3 mr-1 ${config.animate ? 'animate-spin' : ''}`} />
        {config.text}
      </Badge>
    );
  };

  // Empty State
  if (!isLoading && accounts.length === 0) {
    return (
      <Layout>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h1 className="text-3xl font-bold text-slate-900 dark:text-white">AWS Accounts</h1>
              <p className="text-slate-600 dark:text-slate-400 mt-1">
                Connect and manage your AWS accounts for compliance monitoring
              </p>
            </div>
          </div>

          <Card className="max-w-2xl mx-auto mt-12">
            <CardHeader className="text-center">
              <div className="mx-auto w-12 h-12 bg-blue-100 dark:bg-blue-900/20 rounded-full flex items-center justify-center mb-4">
                <Cloud className="w-6 h-6 text-blue-600 dark:text-blue-400" />
              </div>
              <CardTitle>Connect Your First AWS Account</CardTitle>
              <CardDescription>
                Start monitoring your AWS resources for tag compliance by connecting an account
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="bg-slate-50 dark:bg-slate-800 p-4 rounded-lg space-y-2">
                <div className="flex items-start gap-2">
                  <Shield className="w-5 h-5 text-blue-600 dark:text-blue-400 mt-0.5" />
                  <div>
                    <p className="font-medium text-slate-900 dark:text-white">Secure IAM Role Integration</p>
                    <p className="text-sm text-slate-600 dark:text-slate-400">Uses AWS IAM roles with read-only access</p>
                  </div>
                </div>
                <div className="flex items-start gap-2">
                  <CheckCircle2 className="w-5 h-5 text-green-600 dark:text-green-400 mt-0.5" />
                  <div>
                    <p className="font-medium text-slate-900 dark:text-white">No Long-Term Credentials</p>
                    <p className="text-sm text-slate-600 dark:text-slate-400">Temporary credentials via STS AssumeRole</p>
                  </div>
                </div>
              </div>
            </CardContent>
            <CardFooter>
              <Button onClick={handleStartAddAccount} className="w-full" size="lg">
                <Plus className="w-4 h-4 mr-2" />
                Connect AWS Account
              </Button>
            </CardFooter>
          </Card>
        </div>

        {/* Add Account Dialog */}
        <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Connect AWS Account</DialogTitle>
              <DialogDescription>
                Step {addStep} of 3: {addStep === 1 ? 'Generating credentials' : addStep === 2 ? 'Deploy IAM role' : 'Enter account details'}
              </DialogDescription>
            </DialogHeader>

            {addStep === 1 && (
              <div className="flex items-center justify-center py-8">
                <RefreshCw className="w-8 h-8 animate-spin text-blue-600 dark:text-blue-400" />
              </div>
            )}

            {addStep === 2 && (
              <div className="space-y-4">
                <Alert className="bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
                  <Shield className="w-4 h-4 text-blue-600 dark:text-blue-400" />
                  <AlertDescription className="text-blue-900 dark:text-blue-100">
                    Resonant uses IAM roles with read-only access to scan your resources. No destructive permissions are required.
                  </AlertDescription>
                </Alert>

                <div className="space-y-3">
                  <div>
                    <Label>External ID (Required for CloudFormation)</Label>
                    <div className="flex gap-2 mt-1">
                      <Input value={externalId} readOnly className="font-mono" />
                      <Button
                        variant="outline"
                        onClick={handleCopyExternalId}
                        className="shrink-0"
                      >
                        {copiedExternalId ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                      </Button>
                    </div>
                  </div>

                  <Separator />

                  <div className="bg-slate-50 dark:bg-slate-800 p-4 rounded-lg space-y-3">
                    <h4 className="font-semibold text-slate-900 dark:text-white">Deployment Instructions</h4>
                    <ol className="space-y-2 text-sm text-slate-600 dark:text-slate-400 list-decimal list-inside">
                      <li>Copy the External ID above (you'll need it in step 4)</li>
                      <li>Click "Next: Enter Details" below and enter your AWS Account ID</li>
                      <li>Return to this step and download the CloudFormation template</li>
                      <li>Open the AWS CloudFormation console in your account</li>
                      <li>Create a new stack and upload the template</li>
                      <li>Paste the External ID when prompted</li>
                      <li>Complete the stack creation</li>
                      <li>Copy the Role ARN from stack outputs</li>
                    </ol>
                  </div>

                  <Button onClick={handleDownloadTemplate} variant="outline" className="w-full">
                    <Download className="w-4 h-4 mr-2" />
                    Download CloudFormation Template
                  </Button>
                </div>

                <DialogFooter>
                  <Button variant="outline" onClick={handleCloseDialog}>
                    Cancel
                  </Button>
                  <Button onClick={() => setAddStep(3)}>
                    Next: Enter Details
                  </Button>
                </DialogFooter>
              </div>
            )}

            {addStep === 3 && (
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="accountId">AWS Account ID *</Label>
                  <Input
                    id="accountId"
                    placeholder="123456789012"
                    value={formData.accountId}
                    onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="accountAlias">Account Alias *</Label>
                  <Input
                    id="accountAlias"
                    placeholder="Production AWS"
                    value={formData.accountAlias}
                    onChange={(e) => setFormData({ ...formData, accountAlias: e.target.value })}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="roleArn">IAM Role ARN *</Label>
                  <Input
                    id="roleArn"
                    placeholder="arn:aws:iam::123456789012:role/ResonantComplianceRole"
                    value={formData.roleArn}
                    onChange={(e) => setFormData({ ...formData, roleArn: e.target.value })}
                    className="font-mono text-sm"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Find this in your CloudFormation stack outputs
                  </p>
                </div>

                <DialogFooter>
                  <Button variant="outline" onClick={() => setAddStep(2)}>
                    Back
                  </Button>
                  <Button
                    onClick={handleSubmitAccount}
                    disabled={addAccountMutation.isPending}
                  >
                    {addAccountMutation.isPending && <RefreshCw className="w-4 h-4 mr-2 animate-spin" />}
                    Connect Account
                  </Button>
                </DialogFooter>
              </div>
            )}
          </DialogContent>
        </Dialog>
      </Layout>
    );
  }

  // Main View (with accounts)
  return (
    <Layout>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-slate-900 dark:text-white">AWS Accounts</h1>
            <p className="text-slate-600 dark:text-slate-400 mt-1">
              {accounts.length} connected account{accounts.length !== 1 ? 's' : ''}
            </p>
          </div>
          <Button onClick={handleStartAddAccount}>
            <Plus className="w-4 h-4 mr-2" />
            Add Account
          </Button>
        </div>

        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {accounts.map((account) => (
            <Card key={account.id}>
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    {editingAccountId === account.id ? (
                      <div className="flex items-center gap-2">
                        <Input
                          value={newAlias}
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
                          onClick={() => {
                            setEditingAccountId(account.id);
                            setNewAlias(account.accountAlias);
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
                  onClick={() => testConnectionMutation.mutate(account.id)}
                  disabled={testConnectionMutation.isPending}
                  className="flex-1"
                >
                  <RefreshCw className={`w-3 h-3 mr-1 ${testConnectionMutation.isPending ? 'animate-spin' : ''}`} />
                  Test
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
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

        {/* Add Account Dialog */}
        <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Connect AWS Account</DialogTitle>
              <DialogDescription>
                Step {addStep} of 3: {addStep === 1 ? 'Generating credentials' : addStep === 2 ? 'Deploy IAM role' : 'Enter account details'}
              </DialogDescription>
            </DialogHeader>

            {addStep === 1 && (
              <div className="flex items-center justify-center py-8">
                <RefreshCw className="w-8 h-8 animate-spin text-blue-600 dark:text-blue-400" />
              </div>
            )}

            {addStep === 2 && (
              <div className="space-y-4">
                <Alert className="bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
                  <Shield className="w-4 h-4 text-blue-600 dark:text-blue-400" />
                  <AlertDescription className="text-blue-900 dark:text-blue-100">
                    Resonant uses IAM roles with read-only access to scan your resources. No destructive permissions are required.
                  </AlertDescription>
                </Alert>

                <div className="space-y-3">
                  <div>
                    <Label>External ID (Required for CloudFormation)</Label>
                    <div className="flex gap-2 mt-1">
                      <Input value={externalId} readOnly className="font-mono" />
                      <Button
                        variant="outline"
                        onClick={handleCopyExternalId}
                        className="shrink-0"
                      >
                        {copiedExternalId ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                      </Button>
                    </div>
                  </div>

                  <Separator />

                  <div className="bg-slate-50 dark:bg-slate-800 p-4 rounded-lg space-y-3">
                    <h4 className="font-semibold text-slate-900 dark:text-white">Deployment Instructions</h4>
                    <ol className="space-y-2 text-sm text-slate-600 dark:text-slate-400 list-decimal list-inside">
                      <li>Download the CloudFormation template below</li>
                      <li>Open the AWS CloudFormation console in your account</li>
                      <li>Create a new stack and upload the template</li>
                      <li>Paste the External ID when prompted</li>
                      <li>Complete the stack creation</li>
                      <li>Copy the Role ARN from stack outputs</li>
                    </ol>
                  </div>

                  <Button onClick={handleDownloadTemplate} variant="outline" className="w-full">
                    <Download className="w-4 h-4 mr-2" />
                    Download CloudFormation Template
                  </Button>
                </div>

                <DialogFooter>
                  <Button variant="outline" onClick={handleCloseDialog}>
                    Cancel
                  </Button>
                  <Button onClick={() => setAddStep(3)}>
                    Next: Enter Details
                  </Button>
                </DialogFooter>
              </div>
            )}

            {addStep === 3 && (
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="accountId">AWS Account ID *</Label>
                  <Input
                    id="accountId"
                    placeholder="123456789012"
                    value={formData.accountId}
                    onChange={(e) => setFormData({ ...formData, accountId: e.target.value })}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="accountAlias">Account Alias *</Label>
                  <Input
                    id="accountAlias"
                    placeholder="Production AWS"
                    value={formData.accountAlias}
                    onChange={(e) => setFormData({ ...formData, accountAlias: e.target.value })}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="roleArn">IAM Role ARN *</Label>
                  <Input
                    id="roleArn"
                    placeholder="arn:aws:iam::123456789012:role/ResonantComplianceRole"
                    value={formData.roleArn}
                    onChange={(e) => setFormData({ ...formData, roleArn: e.target.value })}
                    className="font-mono text-sm"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Find this in your CloudFormation stack outputs
                  </p>
                </div>

                <DialogFooter>
                  <Button variant="outline" onClick={() => setAddStep(2)}>
                    Back
                  </Button>
                  <Button
                    onClick={handleSubmitAccount}
                    disabled={addAccountMutation.isPending}
                  >
                    {addAccountMutation.isPending && <RefreshCw className="w-4 h-4 mr-2 animate-spin" />}
                    Connect Account
                  </Button>
                </DialogFooter>
              </div>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </Layout>
  );
};