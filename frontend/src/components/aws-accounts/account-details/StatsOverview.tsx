import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {AlertTriangle, CheckCircle, Clock, Package} from 'lucide-react';
import {formatDistanceToNow} from 'date-fns';
import {ScanJob} from '@/types/scanJob.ts';
import './StatsOverview.css';

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
            <Package className="h-4 w-4 stats-overview-icon-resources"/>
          </CardHeader>
          <CardContent>
            <div className="stats-overview-value">
              {resourceCount}
            </div>
            <p className="stats-overview-description">
              Discovered resources
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Open Violations
            </CardTitle>
            <AlertTriangle className="h-4 w-4 stats-overview-icon-violations"/>
          </CardHeader>
          <CardContent>
            <div className="stats-overview-value">
              {openViolations}
            </div>
            <p className="stats-overview-description">
              Requires attention
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Last Scan
            </CardTitle>
            <Clock className="h-4 w-4 stats-overview-icon-scan"/>
          </CardHeader>
          <CardContent>
            <div className="stats-overview-value">
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
            <p className="stats-overview-description">
              {hasScanned && latestScan ? (
                  latestScan.status === 'SUCCESS' ? (
                      <span className="flex items-center">
                  <CheckCircle className="stats-overview-success-icon"/>
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
