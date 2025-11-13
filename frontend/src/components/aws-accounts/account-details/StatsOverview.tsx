import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {AlertTriangle, CheckCircle, Clock, Package} from 'lucide-react';
import {formatDistanceToNow} from 'date-fns';
import {ScanJob} from '@/types/scan';

interface StatsOverviewProps {
  resourceCount: number;
  openViolations: number;
  latestScan: ScanJob | null;
}

export const StatsOverview = ({resourceCount, openViolations, latestScan}: StatsOverviewProps) => {
  const hasScanned = latestScan !== null;

  return (
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Resources
            </CardTitle>
            <Package className="h-4 w-4 text-primary"/>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-foreground">
              {resourceCount}
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
            <AlertTriangle className="h-4 w-4 text-destructive"/>
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
            <Clock className="h-4 w-4 text-muted-foreground"/>
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
                  <CheckCircle className="h-3 w-3 mr-1 text-green-600"/>
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
  );
};