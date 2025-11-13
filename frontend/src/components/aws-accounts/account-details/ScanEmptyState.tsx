import {Card, CardContent} from '@/components/ui/card';
import {ScanButton} from '@/components/ScanButton';
import {Cloud} from 'lucide-react';

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
          <Cloud className="h-16 w-16 text-muted-foreground/50 mx-auto mb-4"/>
          <h3 className="text-lg font-semibold text-foreground mb-2">
            No scans yet
          </h3>
          <p className="text-muted-foreground mb-4">
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