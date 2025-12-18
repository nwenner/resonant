import {useEffect, useRef} from 'react';
import {useQuery} from '@tanstack/react-query';
import {scanService} from '@/services/scanService';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Badge} from '@/components/ui/badge';
import {Progress} from '@/components/ui/progress';
import {AlertCircle, CheckCircle, Clock, Loader2, XCircle} from 'lucide-react';
import {ScanJob} from '@/types/scanJob';
import {formatDistanceToNow} from 'date-fns';
import {useToast} from '@/hooks/useToast';
import './ScanStatusCard.css';

interface ScanStatusCardProps {
  scanJobId: string;
  onComplete?: () => void;
}

const getStatusConfig = (status: ScanJob['status']) => {
  switch (status) {
    case 'PENDING':
      return {
        icon: Clock,
        iconColor: 'status-icon-pending',
        bgColor: 'status-bg-pending',
        badgeVariant: 'secondary' as const,
      };
    case 'RUNNING':
      return {
        icon: Loader2,
        iconColor: 'status-icon-running',
        bgColor: 'status-bg-running',
        badgeVariant: 'default' as const,
        animate: true,
      };
    case 'SUCCESS':
      return {
        icon: CheckCircle,
        iconColor: 'status-icon-success',
        bgColor: 'status-bg-success',
        badgeVariant: 'default' as const,
      };
    case 'FAILED':
      return {
        icon: XCircle,
        iconColor: 'status-icon-failed',
        bgColor: 'status-bg-failed',
        badgeVariant: 'destructive' as const,
      };
  }
};

export const ScanStatusCard = ({scanJobId, onComplete}: ScanStatusCardProps) => {
  const {toast} = useToast();
  const hasNotifiedRef = useRef(false);

  const {data: scanJob, isLoading} = useQuery({
    queryKey: ['scan-job', scanJobId],
    queryFn: () => scanService.getScanJob(scanJobId),
    refetchInterval: (query) => {
      const data = query.state.data as ScanJob | undefined;
      // Poll every 3 seconds if PENDING or RUNNING
      return data?.status === 'PENDING' || data?.status === 'RUNNING' ? 3000 : false;
    },
  });

  // Call onComplete and show toast when scan finishes
  useEffect(() => {
    if (scanJob && (scanJob.status === 'SUCCESS' || scanJob.status === 'FAILED') && !hasNotifiedRef.current) {
      hasNotifiedRef.current = true;

      if (scanJob.status === 'SUCCESS') {
        toast({
          title: 'Scan Completed',
          description: `Found ${scanJob.violationsFound} violations across ${scanJob.resourcesScanned} resources`,
        });
      } else {
        toast({
          title: 'Scan Failed',
          description: scanJob.errorMessage || 'An error occurred during the scan',
          variant: 'destructive',
        });
      }

      onComplete?.();
    }
  }, [scanJob, onComplete, toast]);

  if (isLoading || !scanJob) {
    return (
        <Card>
          <CardContent className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-tertiary"/>
          </CardContent>
        </Card>
    );
  }

  const statusConfig = getStatusConfig(scanJob.status);
  const StatusIcon = statusConfig.icon;
  const isRunning = scanJob.status === 'RUNNING' || scanJob.status === 'PENDING';

  return (
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className={`p-2 rounded-lg ${statusConfig.bgColor}`}>
                <StatusIcon
                    className={`h-5 w-5 ${statusConfig.iconColor} ${statusConfig.animate ? 'animate-spin' : ''}`}
                />
              </div>
              <div>
                <CardTitle className="text-lg">
                  {isRunning ? 'Scan in Progress' : scanJob.status === 'SUCCESS' ? 'Scan Complete' : 'Scan Failed'}
                </CardTitle>
                <CardDescription>
                  {scanJob.accountAlias} •
                  Started {formatDistanceToNow(new Date(scanJob.createdAt), {addSuffix: true})}
                </CardDescription>
              </div>
            </div>
            <Badge variant={statusConfig.badgeVariant}>{scanJob.status}</Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Progress Bar */}
          {isRunning && (
              <div className="space-y-2">
                <Progress value={undefined} className="h-2"/>
                <p className="scan-progress-text">
                  Scanning resources and evaluating policies...
                </p>
              </div>
          )}

          {/* Stats Grid */}
          <div className="grid grid-cols-3 gap-4">
            <div className="scan-stat-card">
              <div className="scan-stat-value">
                {scanJob.resourcesScanned}
              </div>
              <div className="scan-stat-label">Resources Scanned
              </div>
            </div>
            <div className="scan-stat-card">
              <div className="scan-stat-value scan-stat-value-error">
                {scanJob.violationsFound}
              </div>
              <div className="scan-stat-label">Violations Found
              </div>
            </div>
            <div className="scan-stat-card">
              <div className="scan-stat-value scan-stat-value-success">
                {scanJob.violationsResolved}
              </div>
              <div className="scan-stat-label">Resolved</div>
            </div>
          </div>

          {/* Duration */}
          {scanJob.durationSeconds !== null && (
              <div
                  className="flex items-center justify-between text-sm text-secondary">
                <span>Duration:</span>
                <span className="font-medium">{scanJob.durationSeconds}s</span>
              </div>
          )}

          {/* Error Message */}
          {scanJob.status === 'FAILED' && scanJob.errorMessage && (
              <div className="scan-error-box">
                <AlertCircle className="h-5 w-5 scan-error-icon"/>
                <div>
                  <p className="scan-error-title">Error</p>
                  <p className="scan-error-message">{scanJob.errorMessage}</p>
                </div>
              </div>
          )}

          {/* Completion Time */}
          {scanJob.completedAt && (
              <div className="scan-completion-time">
                Completed {formatDistanceToNow(new Date(scanJob.completedAt), {addSuffix: true})}
              </div>
          )}
        </CardContent>
      </Card>
  );
};