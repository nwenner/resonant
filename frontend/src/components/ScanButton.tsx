import { useMutation } from '@tanstack/react-query';
import { scanService } from '@/services/scanService';
import { Button } from '@/components/ui/button';
import { PlayCircle, Loader2 } from 'lucide-react';
import { useToast } from "@/hooks/useToast"

interface ScanButtonProps {
  accountId: string;
  accountAlias: string;
  disabled?: boolean;
  onScanStarted?: (scanJobId: string) => void;
}

export const ScanButton = ({ accountId, accountAlias, disabled, onScanStarted }: ScanButtonProps) => {
  const { toast } = useToast();

  const scanMutation = useMutation({
    mutationFn: () => scanService.triggerScan(accountId),
    onSuccess: (data) => {
      toast({
        title: 'Scan Complete!',
        description: `Scanned ${accountAlias} for compliance violations`,
      });
      onScanStarted?.(data.id);
    },
    onError: (error: any) => {
      toast({
        title: 'Scan Failed',
        description: error.response?.data?.message || 'Failed to start scan',
        variant: 'destructive',
      });
    },
  });

  return (
    <Button
      onClick={() => scanMutation.mutate()}
      disabled={disabled || scanMutation.isPending}
      size="sm"
    >
      {scanMutation.isPending ? (
        <>
          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
          Scanning...
        </>
      ) : (
        <>
          <PlayCircle className="h-4 w-4 mr-2" />
          Scan Account
        </>
      )}
    </Button>
  );
};