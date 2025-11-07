import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Separator } from '@/components/ui/separator';
import { DialogFooter } from '@/components/ui/dialog';
import { Shield, Copy, Check, Download } from 'lucide-react';

interface WizardStep1Props {
  externalId: string;
  copiedExternalId: boolean;
  onCopyExternalId: () => void;
  onDownloadTemplate: () => void;
  onCancel: () => void;
  onNext: () => void;
}

export const WizardStep1 = ({
  externalId,
  copiedExternalId,
  onCopyExternalId,
  onDownloadTemplate,
  onCancel,
  onNext
}: WizardStep1Props) => {
  return (
    <div className="space-y-4">
      <Alert className="bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
        <Shield className="w-4 h-4 text-blue-600 dark:text-blue-400" />
        <AlertDescription className="text-blue-900 dark:text-blue-100">
          Resonant uses IAM roles with read-only access to scan your resources. No destructive permissions are required.
        </AlertDescription>
      </Alert>

      <div className="space-y-3">
        <div>
          <Label>External ID (Required for CloudFormation)</Label>
          <div className="flex gap-2 mt-1">
            <Input value={externalId} readOnly className="font-mono" />
            <Button
              variant="outline"
              onClick={onCopyExternalId}
              className="shrink-0"
            >
              {copiedExternalId ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
            </Button>
          </div>
        </div>

        <Separator />

        <div className="bg-slate-50 dark:bg-slate-800 p-4 rounded-lg space-y-3">
          <h4 className="font-semibold text-slate-900 dark:text-white">Deployment Instructions</h4>
          <ol className="space-y-2 text-sm text-slate-600 dark:text-slate-400 list-decimal list-inside">
            <li>Download the CloudFormation template below</li>
            <li>Open the AWS CloudFormation console in your account</li>
            <li>Create a new stack and upload the template</li>
            <li>Paste the External ID when prompted</li>
            <li>Complete the stack creation</li>
            <li>Copy the Role ARN from stack outputs</li>
          </ol>
        </div>

        <Button onClick={onDownloadTemplate} variant="outline" className="w-full">
          <Download className="w-4 h-4 mr-2" />
          Download CloudFormation Template
        </Button>
      </div>

      <DialogFooter>
        <Button variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button onClick={onNext}>
          Next: Enter Details
        </Button>
      </DialogFooter>
    </div>
  );
};