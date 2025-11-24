import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Alert, AlertDescription} from '@/components/ui/alert';
import {Separator} from '@/components/ui/separator';
import {DialogFooter} from '@/components/ui/dialog';
import {Check, Copy, Download, Shield} from 'lucide-react';
import './WizardStyles.css';

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
        <Alert className="wizard-alert-box">
          <Shield className="w-4 h-4 wizard-alert-icon"/>
          <AlertDescription className="wizard-alert-text">
            Resonant uses IAM roles with read-only access to scan your resources. No destructive
            permissions are required.
          </AlertDescription>
        </Alert>

        <div className="space-y-3">
          <div>
            <Label>External ID (Required for CloudFormation)</Label>
            <div className="flex gap-2 mt-1">
              <Input value={externalId} readOnly className="font-mono"/>
              <Button
                  variant="outline"
                  onClick={onCopyExternalId}
                  className="shrink-0"
              >
                {copiedExternalId ? <Check className="w-4 h-4"/> : <Copy className="w-4 h-4"/>}
              </Button>
            </div>
          </div>

          <Separator/>

          <div className="wizard-instructions-box space-y-3">
            <h4 className="wizard-instructions-title">Deployment
              Instructions</h4>
            <ol className="wizard-instructions-list space-y-2 list-decimal list-inside">
              <li>Download the CloudFormation template below</li>
              <li>Open the AWS CloudFormation console in your account</li>
              <li>Create a new stack and upload the template</li>
              <li>Paste the External ID when prompted</li>
              <li>Complete the stack creation</li>
              <li>Copy the Role ARN from stack outputs</li>
            </ol>
          </div>

          <Button onClick={onDownloadTemplate} variant="outline" className="w-full">
            <Download className="w-4 h-4 mr-2"/>
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