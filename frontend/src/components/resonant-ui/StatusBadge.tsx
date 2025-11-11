import { Badge } from '@/components/ui/badge';
import { CheckCircle2, XCircle, AlertCircle, RefreshCw } from 'lucide-react';

type AccountStatus = 'ACTIVE' | 'INVALID' | 'EXPIRED' | 'TESTING';

interface StatusBadgeProps {
  status: AccountStatus;
}

export const StatusBadge = ({ status }: StatusBadgeProps) => {
  const statusConfig = {
    ACTIVE: { 
      variant: 'default' as const, 
      className: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
      icon: CheckCircle2 
    },
    INVALID: { 
      variant: 'destructive' as const, 
      className: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
      icon: XCircle 
    },
    EXPIRED: { 
      variant: 'secondary' as const, 
      className: 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400',
      icon: AlertCircle 
    },
    TESTING: { 
      variant: 'outline' as const, 
      className: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
      icon: RefreshCw 
    }
  };

  const config = statusConfig[status as keyof typeof statusConfig] || statusConfig.TESTING;
  const Icon = config.icon;

  return (
    <Badge variant={config.variant} className={config.className}>
      <Icon className="w-3 h-3 mr-1" />
      {status}
    </Badge>
  );
};