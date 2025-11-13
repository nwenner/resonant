import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { ScanButton } from '@/components/ScanButton';
import { ScanStatusCard } from '@/components/ScanStatusCard';
import { ViolationsTable } from '@/components/ViolationsTable';
import { ResourcesTable } from '@/components/ResourcesTable';
import { ArrowLeft, Cloud, AlertTriangle, Package, CheckCircle, Clock } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useAwsAccount } from '@/hooks/useAwsAccounts';
import { useLatestScan } from '@/hooks/useScans';
import { useAccountViolations } from '@/hooks/useViolations';
import { useAccountResources } from '@/hooks/useResources';

export const AwsAccountDetail = () => {
  const { accountId } = useParams<{ accountId: string }>();
  const navigate = useNavigate();
  const [activeScanId, setActiveScanId] = useState<string | null>(null);

  const { data: account, isLoading: accountLoading } = useAwsAccount(accountId!);
  const { data: latestScan, refetch: refetchLatestScan } = useLatestScan(accountId!);
  const { data: violations = [] } = useAccountViolations(accountId!);
  const { data: resources = [] } = useAccountResources(accountId!);

  // Set active scan ID if there's a running scan
  useEffect(() => {
    if (latestScan && (latestScan.status === 'PENDING' || latestScan.status === 'RUNNING')) {
      setActiveScanId(latestScan.id);
    }
  }, [latestScan]);

  const handleScanStarted = (scanJobId: string) => {
    setActiveScanId(scanJobId);
  };

  const handleScanComplete = () => {
    refetchLatestScan();
    setActiveScanId(null);
  };

  if (accountLoading || !account) {
    return (
      <Layout>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-center py-12">Loading account details...</div>
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
        {/* Header */}
        <div className="mb-6">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/aws-accounts')}
            className="mb-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Accounts
          </Button>

          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="p-3 bg-primary/10 rounded-lg">
                <Cloud className="h-8 w-8 text-primary" />
              </div>
              <div>
                <h1 className="text-3xl font-bold text-foreground">
                  {account.accountAlias}
                </h1>
                <p className="text-muted-foreground mt-1">
                  Account ID: {account.accountId}
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-3">
              <Badge variant={account.status === 'ACTIVE' ? 'default' : 'destructive'}>
                {account.status}
              </Badge>
              <ScanButton
                accountId={account.id}
                accountAlias={account.accountAlias}
                disabled={isScanning}
                onScanStarted={handleScanStarted}
              />
            </div>
          </div>
        </div>

        {/* Active Scan Status */}
        {activeScanId && (
          <div className="mb-6">
            <ScanStatusCard scanJobId={activeScanId} onComplete={handleScanComplete} />
          </div>
        )}

        {/* Overview Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Resources
              </CardTitle>
              <Package className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-foreground">
                {resources.length}
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                Discovered resources
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Open Violations
              </CardTitle>
              <AlertTriangle className="h-4 w-4 text-destructive" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-foreground">
                {openViolations}
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                Requires attention
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Last Scan
              </CardTitle>
              <Clock className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-foreground">
                {hasScanned && latestScan ? (
                  <span className="text-lg">
                    {formatDistanceToNow(new Date(latestScan.completedAt || latestScan.startedAt), {
                      addSuffix: true,
                    })}
                  </span>
                ) : (
                  'Never'
                )}
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                {hasScanned && latestScan ? (
                  latestScan.status === 'SUCCESS' ? (
                    <span className="flex items-center">
                      <CheckCircle className="h-3 w-3 mr-1 text-green-600" />
                      Completed successfully
                    </span>
                  ) : latestScan.status === 'FAILED' ? (
                    'Failed'
                  ) : (
                    'In progress'
                  )
                ) : (
                  'No scans yet'
                )}
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Tabs */}
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
            {/* Latest Scan Summary */}
            {latestScan && latestScan.status === 'SUCCESS' && latestScan.completedAt && (
              <Card>
                <CardHeader>
                  <CardTitle>Latest Scan Summary</CardTitle>
                  <CardDescription>
                    Completed {formatDistanceToNow(new Date(latestScan.completedAt), { addSuffix: true })}
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-3 gap-4">
                    <div className="text-center p-4 rounded-lg bg-muted">
                      <div className="text-2xl font-bold text-foreground">
                        {latestScan.resourcesScanned}
                      </div>
                      <div className="text-sm text-muted-foreground mt-1">
                        Resources Scanned
                      </div>
                    </div>
                    <div className="text-center p-4 rounded-lg bg-muted">
                      <div className="text-2xl font-bold text-destructive">
                        {latestScan.violationsFound}
                      </div>
                      <div className="text-sm text-muted-foreground mt-1">
                        Violations Found
                      </div>
                    </div>
                    <div className="text-center p-4 rounded-lg bg-muted">
                      <div className="text-2xl font-bold text-green-600">
                        {latestScan.violationsResolved}
                      </div>
                      <div className="text-sm text-muted-foreground mt-1">
                        Violations Resolved
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* No scans yet */}
            {!hasScanned && !isScanning && (
              <Card>
                <CardContent className="text-center py-12">
                  <Cloud className="h-16 w-16 text-muted-foreground/50 mx-auto mb-4" />
                  <h3 className="text-lg font-semibold text-foreground mb-2">
                    No scans yet
                  </h3>
                  <p className="text-muted-foreground mb-4">
                    Start your first scan to discover resources and evaluate compliance
                  </p>
                  <ScanButton
                    accountId={account.id}
                    accountAlias={account.accountAlias}
                    onScanStarted={handleScanStarted}
                  />
                </CardContent>
              </Card>
            )}

            {/* Recent violations preview */}
            {openViolations > 0 && (
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle>Recent Violations</CardTitle>
                      <CardDescription>{openViolations} open violations</CardDescription>
                    </div>
                    <Button variant="outline" size="sm" onClick={() => {}}>
                      View All
                    </Button>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    {violations.slice(0, 5).map((violation) => (
                      <div
                        key={violation.id}
                        className="flex items-center justify-between p-3 rounded-lg bg-muted"
                      >
                        <div>
                          <div className="font-medium text-sm text-foreground">
                            {violation.resourceName || violation.resourceArn.split('/').pop()}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            {violation.policyName}
                          </div>
                        </div>
                        <Badge variant="destructive">{violation.severity}</Badge>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}
          </TabsContent>

          <TabsContent value="resources">
            <ResourcesTable accountId={accountId} />
          </TabsContent>

          <TabsContent value="violations">
            <ViolationsTable accountId={accountId} />
          </TabsContent>
        </Tabs>
      </div>
    </Layout>
  );
};