import {useViolationStats} from '@/hooks/useViolationStats';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {AlertTriangle, Loader2} from 'lucide-react';
import {useNavigate} from 'react-router-dom';
import type {Severity} from '@/types/severity';

const severityColors: Record<Severity, string> = {
  CRITICAL: 'text-red-600 dark:text-red-400',
  HIGH: 'text-orange-600 dark:text-orange-400',
  MEDIUM: 'text-yellow-600 dark:text-yellow-400',
  LOW: 'text-blue-600 dark:text-blue-400',
};

const severityLabels: Record<Severity, string> = {
  CRITICAL: 'Critical',
  HIGH: 'High',
  MEDIUM: 'Medium',
  LOW: 'Low',
};

const severityOrder: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

export function NonCompliantCard() {
  const {data: stats, isLoading, error} = useViolationStats();
  const navigate = useNavigate();

  if (isLoading) {
    return (
        <Card className="cursor-pointer hover:shadow-lg transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Non-Compliant Resources</CardTitle>
            <AlertTriangle className="h-4 w-4 text-muted-foreground"/>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-center h-20">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground"/>
            </div>
          </CardContent>
        </Card>
    );
  }

  if (error) {
    return (
        <Card className="cursor-pointer hover:shadow-lg transition-shadow">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Non-Compliant Resources</CardTitle>
            <AlertTriangle className="h-4 w-4 text-muted-foreground"/>
          </CardHeader>
          <CardContent>
            <div className="text-sm text-muted-foreground">Failed to load</div>
          </CardContent>
        </Card>
    );
  }

  const totalOpen = stats?.totalOpen || 0;
  const bySeverity = stats?.bySeverity || {};

  return (
      <Card
          className="cursor-pointer hover:shadow-lg transition-shadow"
          onClick={() => navigate('/aws-accounts')}
      >
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Non-Compliant Resources</CardTitle>
          <AlertTriangle className="h-4 w-4 text-muted-foreground"/>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{totalOpen}</div>
          <CardDescription className="text-xs text-muted-foreground">
            Open violations
          </CardDescription>

          {totalOpen > 0 && (
              <div className="mt-4 space-y-2">
                {severityOrder.map((severity) => {
                  const count = bySeverity[severity] || 0;
                  if (count === 0) return null;

                  return (
                      <div key={severity} className="flex items-center justify-between text-sm">
                        <span className={severityColors[severity]}>{severityLabels[severity]}</span>
                        <span className="font-medium">{count}</span>
                      </div>
                  );
                })}
              </div>
          )}
        </CardContent>
      </Card>
  );
}