import {useNavigate, useParams} from 'react-router-dom';
import {Layout} from '@/components/Layout';
import {Button} from '@/components/ui/button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {RegionSelector} from '@/components/RegionSelector';
import {ArrowLeft, Settings} from 'lucide-react';
import {useAwsAccount} from '@/hooks/useAwsAccounts';
import {useAccountRegions, useRediscoverRegions, useUpdateRegions} from '@/hooks/useAccountRegions';
import {useToast} from '@/hooks/useToast';

export const AwsAccountSettings = () => {
  const {accountId} = useParams<{ accountId: string }>();
  const navigate = useNavigate();
  const {toast} = useToast();

  const {data: account, isLoading: accountLoading} = useAwsAccount(accountId!);
  const {data: regions = [], isLoading: regionsLoading} = useAccountRegions(accountId!);
  const updateRegions = useUpdateRegions(accountId!);
  const rediscoverRegions = useRediscoverRegions(accountId!);

  const handleSaveRegions = async (enabledRegionCodes: string[]) => {
    try {
      await updateRegions.mutateAsync(enabledRegionCodes);
      toast({
        title: 'Settings saved',
        description: 'Region configuration updated successfully',
      });
    } catch (error) {
      toast({
        title: 'Save failed',
        description: 'Failed to update region configuration',
        variant: 'destructive',
      });
      throw error;
    }
  };

  const handleRediscover = async () => {
    try {
      await rediscoverRegions.mutateAsync();
      toast({
        title: 'Regions rediscovered',
        description: 'New regions have been added to your account',
      });
    } catch (error) {
      toast({
        title: 'Rediscovery failed',
        description: 'Failed to rediscover regions',
        variant: 'destructive',
      });
      throw error;
    }
  };

  if (accountLoading || regionsLoading || !account) {
    return (
        <Layout>
          <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="text-center py-12 text-muted-foreground">
              Loading account settings...
            </div>
          </div>
        </Layout>
    );
  }

  return (
      <Layout>
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="mb-6">
            <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate(`/aws-accounts/${accountId}`)}
                className="mb-4"
            >
              <ArrowLeft className="h-4 w-4 mr-2"/>
              Back to Account
            </Button>

            <div className="flex items-center gap-3">
              <Settings className="h-8 w-8 text-muted-foreground"/>
              <div>
                <h1 className="text-3xl font-bold">Account Settings</h1>
                <p className="text-muted-foreground mt-1">
                  {account.accountAlias} ({account.accountId})
                </p>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            <RegionSelector
                regions={regions}
                onSave={handleSaveRegions}
                onRediscover={handleRediscover}
                isSaving={updateRegions.isPending}
                isRediscovering={rediscoverRegions.isPending}
            />

            <Card>
              <CardHeader>
                <CardTitle>About Regions</CardTitle>
                <CardDescription>
                  Understanding AWS region configuration
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4 text-sm text-muted-foreground">
                <div>
                  <p className="font-medium text-foreground mb-1">Why configure regions?</p>
                  <p>
                    Each AWS region is an isolated geographic location. By selecting which regions
                    to scan,
                    you control where Resonant looks for resources. This helps reduce scan time and
                    costs
                    by focusing only on regions you actually use.
                  </p>
                </div>
                <div>
                  <p className="font-medium text-foreground mb-1">Rediscover regions</p>
                  <p>
                    AWS occasionally launches new regions. Use the "Rediscover" button to check for
                    new regions that weren't available when you first connected this account.
                  </p>
                </div>
                <div>
                  <p className="font-medium text-foreground mb-1">At least one region required</p>
                  <p>
                    You must have at least one region enabled to perform scans. If no regions are
                    enabled,
                    scan operations will fail with an error.
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </Layout>
  );
};