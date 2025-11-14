import {Card, CardContent} from '@/components/ui/card';
import {ScanButton} from '@/components/ScanButton';
import {Cloud} from 'lucide-react';
import './ScanEmptyState.css';

interface NoScansEmptyStateProps {
  accountId: string;
  accountAlias: string;
  onScanStarted: (scanJobId: string) => void;
}

export const ScanEmptyState = ({
                                 accountId,
                                 accountAlias,
                                 onScanStarted
                               }: NoScansEmptyStateProps) => {
  return (
      <Card>
        <CardContent className="text-center py-12">
          <Cloud className="scan-empty-icon"/>
          <h3 className="scan-empty-title">
            No scans yet
          </h3>
          <p className="scan-empty-description">
            Start your first scan to discover resources and evaluate compliance
          </p>
          <ScanButton
              accountId={accountId}
              accountAlias={accountAlias}
              onScanStarted={onScanStarted}
          />
        </CardContent>
      </Card>
  );
};
