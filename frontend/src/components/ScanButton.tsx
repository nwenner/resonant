import {useMutation} from '@tanstack/react-query';
import {scanService} from '@/services/scanService';
import {Button} from '@/components/ui/button';
import {Loader2, PlayCircle} from 'lucide-react';
import {useToast} from "@/hooks/useToast";
import {useNavigate} from 'react-router-dom';
import {AxiosError} from 'axios';

interface ScanButtonProps {
  accountId: string;
  accountAlias: string;
  disabled?: boolean;
  hasEnabledResources?: boolean;
  onScanStarted?: (scanJobId: string) => void;
}

export const ScanButton = ({
                             accountId,
                             accountAlias,
                             disabled,
                             hasEnabledResources = true,
                             onScanStarted
                           }: ScanButtonProps) => {
  const {toast} = useToast();
  const navigate = useNavigate();

  const scanMutation = useMutation({
    mutationFn: () => scanService.triggerScan(accountId),
    onSuccess: (data) => {
      toast({
        title: 'Scan Started',
        description: `Scanning ${accountAlias} for compliance violations`,
      });
      onScanStarted?.(data.id);
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Scan Failed',
        description: error.response?.data?.message || 'Failed to start scan',
        variant: 'destructive',
      });
    },
  });

  const handleClick = () => {
    if (!hasEnabledResources) {
      toast({
        title: 'No Resource Types Enabled',
        description: 'Please enable at least one resource type in Settings before scanning.',
        variant: 'destructive',
        action: (
            <Button
                size="sm"
                variant="outline"
                onClick={() => navigate('/settings')}
            >
              Go to Settings
            </Button>
        ),
      });
      return;
    }
    scanMutation.mutate();
  };

  const isDisabled = disabled || scanMutation.isPending || !hasEnabledResources;

  return (
      <Button
          onClick={handleClick}
          disabled={isDisabled}
          size="sm"
          title={!hasEnabledResources ? 'No resource types enabled' : undefined}
      >
        {scanMutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin"/>
              Scanning...
            </>
        ) : (
            <>
              <PlayCircle className="h-4 w-4 mr-2"/>
              Scan Account
            </>
        )}
      </Button>
  );
};