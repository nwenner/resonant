import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAwsAccounts} from '@/hooks/useAwsAccounts';
import {useAccountOperations} from '@/hooks/useAccountOperations';
import {Layout} from '@/components/Layout';
import {Button} from '@/components/ui/button';
import {Card, CardContent} from '@/components/ui/card';
import {AddAccountWizard} from '@/components/aws-accounts/AddAccountWizard';
import {AccountCard} from '@/components/aws-accounts/AccountCard';
import {Cloud, Plus, RefreshCw} from 'lucide-react';

``

export const AwsAccounts = () => {
  const navigate = useNavigate();
  const {data: accounts = [], isLoading} = useAwsAccounts();
  const {testConnection, updateAlias, deleteAccount} = useAccountOperations();

  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);

  if (isLoading) {
    return (
        <Layout>
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="flex items-center justify-center h-64">
              <RefreshCw className="w-8 h-8 animate-spin text-icon-blue"/>
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
                <h1 className="text-3xl font-bold text-foreground">AWS Accounts</h1>
                <p className="text-muted-foreground mt-1">
                  Manage your connected AWS accounts for compliance scanning
                </p>
              </div>
              <Button onClick={() => setIsAddDialogOpen(true)}>
                <Plus className="w-4 h-4 mr-2"/>
                Connect Account
              </Button>
            </div>

            {/* Empty State */}
            {accounts.length === 0 && (
                <Card className="border-dashed">
                  <CardContent className="flex flex-col items-center justify-center py-12">
                    <Cloud className="w-12 h-12 text-muted-foreground mb-4"/>
                    <h3 className="text-lg font-semibold text-foreground mb-2">No AWS Accounts
                      Connected</h3>
                    <p className="text-muted-foreground text-center mb-4">
                      Connect your first AWS account to start scanning for tag compliance
                    </p>
                    <Button onClick={() => setIsAddDialogOpen(true)}>
                      <Plus className="w-4 h-4 mr-2"/>
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
                      isTestingConnection={testConnection.isPending}
                      isDeletingAccount={deleteAccount.isPending}
                      isUpdatingAlias={updateAlias.isPending}
                      onCardClick={(accountId) => navigate(`/aws-accounts/${accountId}`)}
                      onTestConnection={(accountId) => testConnection.mutate(accountId)}
                      onDeleteAccount={(accountId) => deleteAccount.mutate(accountId)}
                      onUpdateAlias={(accountId, newAlias) => updateAlias.mutate({
                        id: accountId,
                        alias: newAlias
                      })}
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