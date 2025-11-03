import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { scanService } from '@/services/scanService';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { CheckCircle, XCircle, Loader2, Clock, AlertCircle } from 'lucide-react';
import { ScanJob } from '@/types/scan';
import { formatDistanceToNow } from 'date-fns';

interface ScanStatusCardProps {
  scanJobId: string;
  onComplete?: () => void;
}

const getStatusConfig = (status: ScanJob['status']) => {
  switch (status) {
    case 'PENDING':
      return {
        icon: Clock,
        color: 'text-yellow-600',
        bgColor: 'bg-yellow-100 dark:bg-yellow-900/20',
        badgeVariant: 'secondary' as const,
      };
    case 'RUNNING':
      return {
        icon: Loader2,
        color: 'text-blue-600',
        bgColor: 'bg-blue-100 dark:bg-blue-900/20',
        badgeVariant: 'default' as const,
        animate: true,
      };
    case 'SUCCESS':
      return {
        icon: CheckCircle,
        color: 'text-green-600',
        bgColor: 'bg-green-100 dark:bg-green-900/20',
        badgeVariant: 'default' as const,
      };
    case 'FAILED':
      return {
        icon: XCircle,
        color: 'text-red-600',
        bgColor: 'bg-red-100 dark:bg-red-900/20',
        badgeVariant: 'destructive' as const,
      };
  }
};

export const ScanStatusCard = ({ scanJobId, onComplete }: ScanStatusCardProps) => {
  const { data: scanJob, isLoading } = useQuery({
    queryKey: ['scan-job', scanJobId],
    queryFn: () => scanService.getScanJob(scanJobId),
    refetchInterval: (query) => {
      const data = query.state.data as ScanJob | undefined;
      // Poll every 3 seconds if PENDING or RUNNING
      return data?.status === 'PENDING' || data?.status === 'RUNNING' ? 3000 : false;
    },
  });

  // Call onComplete when scan finishes
  useEffect(() => {
    if (scanJob && (scanJob.status === 'SUCCESS' || scanJob.status === 'FAILED')) {
      onComplete?.();
    }
  }, [scanJob?.status, onComplete]);

  if (isLoading || !scanJob) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
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
                className={`h-5 w-5 ${statusConfig.color} ${statusConfig.animate ? 'animate-spin' : ''}`}
              />
            </div>
            <div>
              <CardTitle className="text-lg">
                {isRunning ? 'Scan in Progress' : scanJob.status === 'SUCCESS' ? 'Scan Complete' : 'Scan Failed'}
              </CardTitle>
              <CardDescription>
                {scanJob.accountAlias} • Started {formatDistanceToNow(new Date(scanJob.startedAt), { addSuffix: true })}
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
            <Progress value={undefined} className="h-2" />
            <p className="text-sm text-slate-600 dark:text-slate-400">
              Scanning resources and evaluating policies...
            </p>
          </div>
        )}

        {/* Stats Grid */}
        <div className="grid grid-cols-3 gap-4">
          <div className="text-center p-3 rounded-lg bg-slate-50 dark:bg-slate-800">
            <div className="text-2xl font-bold text-slate-900 dark:text-white">
              {scanJob.resourcesScanned}
            </div>
            <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">Resources Scanned</div>
          </div>
          <div className="text-center p-3 rounded-lg bg-slate-50 dark:bg-slate-800">
            <div className="text-2xl font-bold text-red-600 dark:text-red-400">
              {scanJob.violationsFound}
            </div>
            <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">Violations Found</div>
          </div>
          <div className="text-center p-3 rounded-lg bg-slate-50 dark:bg-slate-800">
            <div className="text-2xl font-bold text-green-600 dark:text-green-400">
              {scanJob.violationsResolved}
            </div>
            <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">Resolved</div>
          </div>
        </div>

        {/* Duration */}
        {scanJob.durationSeconds !== null && (
          <div className="flex items-center justify-between text-sm text-slate-600 dark:text-slate-400">
            <span>Duration:</span>
            <span className="font-medium">{scanJob.durationSeconds}s</span>
          </div>
        )}

        {/* Error Message */}
        {scanJob.status === 'FAILED' && scanJob.errorMessage && (
          <div className="flex items-start gap-2 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
            <AlertCircle className="h-5 w-5 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-red-900 dark:text-red-200">Error</p>
              <p className="text-sm text-red-700 dark:text-red-300 mt-1">{scanJob.errorMessage}</p>
            </div>
          </div>
        )}

        {/* Completion Time */}
        {scanJob.completedAt && (
          <div className="text-xs text-slate-500 dark:text-slate-400 text-center pt-2 border-t border-slate-200 dark:border-slate-700">
            Completed {formatDistanceToNow(new Date(scanJob.completedAt), { addSuffix: true })}
          </div>
        )}
      </CardContent>
    </Card>
  );
};