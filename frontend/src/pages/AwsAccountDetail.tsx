import {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {Layout} from '@/components/Layout';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Badge} from '@/components/ui/badge';
import {ScanStatusCard} from '@/components/ScanStatusCard';
import {ViolationsTable} from '@/components/ViolationsTable';
import {ResourcesTable} from '@/components/ResourcesTable';
import {AccountHeader} from '@/components/aws-accounts/account-details/AccountHeader';
import {StatsOverview} from '@/components/aws-accounts/account-details/StatsOverview';
import {LatestScanSummary} from '@/components/aws-accounts/account-details/LatestScanSummary';
import {ScanEmptyState} from '@/components/aws-accounts/account-details/ScanEmptyState';
import {
  RecentViolationsPreview
} from '@/components/aws-accounts/account-details/RecentViolationsPreview';
import {useAwsAccount} from '@/hooks/useAwsAccounts';
import {useLatestScan} from '@/hooks/useScans';
import {useAccountViolations} from '@/hooks/useViolations';
import {useAccountResources} from '@/hooks/useResources';

export const AwsAccountDetail = () => {
  const {accountId} = useParams<{ accountId: string }>();
  const [activeScanId, setActiveScanId] = useState<string | null>(null);

  const {data: account, isLoading: accountLoading} = useAwsAccount(accountId!);
  const {data: latestScan, refetch: refetchLatestScan} = useLatestScan(accountId!);
  const {data: violations = [], refetch: refetchViolations} = useAccountViolations(accountId!);
  const {data: resources = [], refetch: refetchResources} = useAccountResources(accountId!);

  useEffect(() => {
    if (latestScan && (latestScan.status === 'PENDING' || latestScan.status === 'RUNNING')) {
      setActiveScanId(latestScan.id);
    }
  }, [latestScan]);

  const handleScanStarted = (scanJobId: string) => {
    setActiveScanId(scanJobId);
  };

  const handleScanComplete = () => {
    Promise.all([
      refetchLatestScan(),
      refetchViolations(),
      refetchResources()
    ]).catch(() => {
      // Handle error silently
    });
    setActiveScanId(null);
  };

  if (accountLoading || !account) {
    return (
        <Layout>
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="text-center py-12 text-muted-foreground">Loading account details...
            </div>
          </div>
        </Layout>
    );
  }

  const openViolations = violations.filter((v) => v.status === 'OPEN').length;
  const isScanning = activeScanId !== null;
  const hasScanned = latestScan !== null;

  return (
      <Layout>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <AccountHeader
              account={account}
              isScanning={isScanning}
              onScanStarted={handleScanStarted}
          />

          {activeScanId && (
              <div className="mb-6">
                <ScanStatusCard scanJobId={activeScanId} onComplete={handleScanComplete}/>
              </div>
          )}

          <StatsOverview
              resourceCount={resources.length}
              openViolations={openViolations}
              latestScan={latestScan}
          />

          <Tabs defaultValue="overview" className="space-y-6">
            <TabsList>
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="resources">
                Resources
                {resources.length > 0 && (
                    <Badge variant="secondary" className="ml-2">
                      {resources.length}
                    </Badge>
                )}
              </TabsTrigger>
              <TabsTrigger value="violations">
                Violations
                {openViolations > 0 && (
                    <Badge variant="destructive" className="ml-2">
                      {openViolations}
                    </Badge>
                )}
              </TabsTrigger>
            </TabsList>

            <TabsContent value="overview" className="space-y-6">
              {latestScan && <LatestScanSummary scan={latestScan}/>}

              {!hasScanned && !isScanning && (
                  <ScanEmptyState
                      accountId={account.id}
                      accountAlias={account.accountAlias}
                      onScanStarted={handleScanStarted}
                  />
              )}

              <RecentViolationsPreview
                  violations={violations}
                  openCount={openViolations}
              />
            </TabsContent>

            <TabsContent value="resources">
              <ResourcesTable accountId={accountId}/>
            </TabsContent>

            <TabsContent value="violations">
              <ViolationsTable accountId={accountId}/>
            </TabsContent>
          </Tabs>
        </div>
      </Layout>
  );
};