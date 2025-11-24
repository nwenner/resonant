import {useEffect, useState} from 'react';
import {AxiosError} from 'axios';
import {useMutation, useQueryClient} from '@tanstack/react-query';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle
} from '@/components/ui/dialog';
import {useToast} from '@/hooks/useToast';
import {awsAccountsService} from '@/services/awsAccountsService';
import {CLOUDFORMATION_TEMPLATE} from '@/constants/cloudformation';
import {QUERY_KEYS} from '@/constants/queryKeys';
import {WizardStep1} from './WizardStep1';
import {WizardStep2} from './WizardStep2';
import {RefreshCw} from 'lucide-react';
import {AccountFormData} from "@/types/awsAccount";
import './WizardStyles.css';

interface AddAccountWizardProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export const AddAccountWizard = ({open, onOpenChange}: AddAccountWizardProps) => {
  const {toast} = useToast();
  const queryClient = useQueryClient();

  const [step, setStep] = useState(0); // 0 = loading, 1 = instructions, 2 = form
  const [externalId, setExternalId] = useState('');
  const [copiedExternalId, setCopiedExternalId] = useState(false);
  const [formData, setFormData] = useState<AccountFormData>({
    accountId: '',
    accountAlias: '',
    roleArn: ''
  });

  // Trigger external ID generation when dialog opens
  useEffect(() => {
    if (open && step === 0) {
      generateExternalIdMutation.mutate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // Reset wizard state when dialog closes
  useEffect(() => {
    if (!open) {
      setStep(0);
      setExternalId('');
      setFormData({accountId: '', accountAlias: '', roleArn: ''});
      setCopiedExternalId(false);
    }
  }, [open]);

  const generateExternalIdMutation = useMutation({
    mutationFn: async () => {
      return await awsAccountsService.generateExternalId();
    },
    onSuccess: (data) => {
      setExternalId(data.externalId);
      setStep(1); // Move to first real step (instructions)
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to generate External ID',
        variant: 'destructive'
      });
      onOpenChange(false);
    }
  });

  const addAccountMutation = useMutation({
    mutationFn: async (payload: AccountFormData & { externalId: string }) => {
      return await awsAccountsService.createAccount(payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: QUERY_KEYS.awsAccounts.all});
      toast({
        title: 'Success',
        description: 'AWS account connected successfully'
      });
      onOpenChange(false); // Close dialog - cleanup will happen in useEffect
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      toast({
        title: 'Connection Failed',
        description: error.response?.data?.message || 'Failed to connect AWS account',
        variant: 'destructive'
      });
    }
  });

  const handleCopyExternalId = async () => {
    await navigator.clipboard.writeText(externalId);
    setCopiedExternalId(true);
    setTimeout(() => setCopiedExternalId(false), 2000);
  };

  const handleDownloadTemplate = () => {
    const blob = new Blob([CLOUDFORMATION_TEMPLATE], {type: 'text/yaml'});
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'resonant-iam-role.yaml';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const handleSubmit = () => {
    addAccountMutation.mutate({
      ...formData,
      externalId
    });
  };

  return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Connect AWS Account</DialogTitle>
            <DialogDescription>
              {step === 0 ? (
                  'Generating credentials...'
              ) : (
                  `Step ${step} of 2: ${step === 1 ? 'Deploy IAM role' : 'Enter account details'}`
              )}
            </DialogDescription>
          </DialogHeader>

          {step === 0 && (
              <div className="flex items-center justify-center py-8">
                <RefreshCw className="w-8 h-8 animate-spin wizard-loading-spinner"/>
              </div>
          )}

          {step === 1 && (
              <WizardStep1
                  externalId={externalId}
                  copiedExternalId={copiedExternalId}
                  onCopyExternalId={handleCopyExternalId}
                  onDownloadTemplate={handleDownloadTemplate}
                  onCancel={() => onOpenChange(false)}
                  onNext={() => setStep(2)}
              />
          )}

          {step === 2 && (
              <WizardStep2
                  formData={formData}
                  isSubmitting={addAccountMutation.isPending}
                  onFormChange={setFormData}
                  onBack={() => setStep(1)}
                  onSubmit={handleSubmit}
              />
          )}
        </DialogContent>
      </Dialog>
  );
};