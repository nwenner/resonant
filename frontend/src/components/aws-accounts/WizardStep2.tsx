import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {DialogFooter} from '@/components/ui/dialog';
import {RefreshCw} from 'lucide-react';

interface AccountFormData {
  accountId: string;
  accountAlias: string;
  roleArn: string;
}

interface WizardStep2Props {
  formData: AccountFormData;
  isSubmitting: boolean;
  onFormChange: (data: AccountFormData) => void;
  onBack: () => void;
  onSubmit: () => void;
}

export const WizardStep2 = ({
                              formData,
                              isSubmitting,
                              onFormChange,
                              onBack,
                              onSubmit
                            }: WizardStep2Props) => {
  // Validate all required fields are filled
  const isFormValid =
      formData.accountId.trim().length > 0 &&
      formData.accountAlias.trim().length > 0 &&
      formData.roleArn.trim().length > 0;

  return (
      <div className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="accountId">AWS Account ID *</Label>
          <Input
              id="accountId"
              placeholder="123456789012"
              value={formData.accountId}
              onChange={(e) => onFormChange({...formData, accountId: e.target.value})}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="accountAlias">Account Alias *</Label>
          <Input
              id="accountAlias"
              placeholder="Production AWS"
              value={formData.accountAlias}
              onChange={(e) => onFormChange({...formData, accountAlias: e.target.value})}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="roleArn">IAM Role ARN *</Label>
          <Input
              id="roleArn"
              placeholder="arn:aws:iam::123456789012:role/ResonantComplianceRole"
              value={formData.roleArn}
              onChange={(e) => onFormChange({...formData, roleArn: e.target.value})}
              className="font-mono text-sm"
          />
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Find this in your CloudFormation stack outputs
          </p>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onBack}>
            Back
          </Button>
          <Button
              onClick={onSubmit}
              disabled={isSubmitting || !isFormValid}
          >
            {isSubmitting && <RefreshCw className="w-4 h-4 mr-2 animate-spin"/>}
            Connect Account
          </Button>
        </DialogFooter>
      </div>
  );
};