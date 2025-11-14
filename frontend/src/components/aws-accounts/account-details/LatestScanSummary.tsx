import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {formatDistanceToNow} from 'date-fns';
import {ScanJob} from '@/types/scanJob.ts';
import './LatestScanSummary.css';

interface LatestScanSummaryProps {
  scan: ScanJob;
}

export const LatestScanSummary = ({scan}: LatestScanSummaryProps) => {
  if (!scan.completedAt || scan.status !== 'SUCCESS') {
    return null;
  }

  return (
      <Card>
        <CardHeader>
          <CardTitle>Latest Scan Summary</CardTitle>
          <CardDescription>
            Completed {formatDistanceToNow(new Date(scan.completedAt), {addSuffix: true})}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-4">
            <div className="scan-metric">
              <div className="scan-metric-value">
                {scan.resourcesScanned}
              </div>
              <div className="scan-metric-label">
                Resources Scanned
              </div>
            </div>
            <div className="scan-metric">
              <div className="scan-metric-value scan-metric-value-violations">
                {scan.violationsFound}
              </div>
              <div className="scan-metric-label">
                Violations Found
              </div>
            </div>
            <div className="scan-metric">
              <div className="scan-metric-value scan-metric-value-resolved">
                {scan.violationsResolved}
              </div>
              <div className="scan-metric-label">
                Violations Resolved
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
  );
};
