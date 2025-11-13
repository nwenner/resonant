import { Badge } from '@/components/ui/badge';
import { CheckCircle2, XCircle, AlertCircle, RefreshCw } from 'lucide-react';
import './StatusBadge.css';

type AccountStatus = 'ACTIVE' | 'INVALID' | 'EXPIRED' | 'TESTING';

interface StatusBadgeProps {
  status: AccountStatus | string; // Allow string for flexibility, but prefer typed values
}

export const StatusBadge = ({ status }: StatusBadgeProps) => {
  const statusConfig = {
    ACTIVE: {
      variant: 'default' as const,
      className: 'status-success',
      icon: CheckCircle2
    },
    INVALID: {
      variant: 'destructive' as const,
      className: 'status-error',
      icon: XCircle
    },
    EXPIRED: {
      variant: 'secondary' as const,
      className: 'status-warning',
      icon: AlertCircle
    },
    TESTING: {
      variant: 'outline' as const,
      className: 'status-info',
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