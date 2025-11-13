import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { formatDistanceToNow } from 'date-fns';
import { ScanJob } from '@/types/scan';

interface LatestScanSummaryProps {
  scan: ScanJob;
}

export const LatestScanSummary = ({ scan }: LatestScanSummaryProps) => {
  if (!scan.completedAt || scan.status !== 'SUCCESS') {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Latest Scan Summary</CardTitle>
        <CardDescription>
          Completed {formatDistanceToNow(new Date(scan.completedAt), { addSuffix: true })}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-3 gap-4">
          <div className="text-center p-4 rounded-lg bg-muted">
            <div className="text-2xl font-bold text-foreground">
              {scan.resourcesScanned}
            </div>
            <div className="text-sm text-muted-foreground mt-1">
              Resources Scanned
            </div>
          </div>
          <div className="text-center p-4 rounded-lg bg-muted">
            <div className="text-2xl font-bold text-destructive">
              {scan.violationsFound}
            </div>
            <div className="text-sm text-muted-foreground mt-1">
              Violations Found
            </div>
          </div>
          <div className="text-center p-4 rounded-lg bg-muted">
            <div className="text-2xl font-bold text-green-600">
              {scan.violationsResolved}
            </div>
            <div className="text-sm text-muted-foreground mt-1">
              Violations Resolved
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};