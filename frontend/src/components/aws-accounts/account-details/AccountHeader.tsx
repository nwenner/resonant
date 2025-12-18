import {useNavigate} from 'react-router-dom';
import {Button} from '@/components/ui/button';
import {Badge} from '@/components/ui/badge';
import {ScanButton} from '@/components/ScanButton';
import {ArrowLeft, Cloud, Settings} from 'lucide-react';
import {AwsAccount} from "@/types/awsAccount.ts";
import {useResourceTypeSettings} from '@/hooks/useResourceTypeSettings';
import './AccountHeader.css';

interface AccountHeaderProps {
  account: AwsAccount;
  isScanning: boolean;
  onScanStarted: (scanJobId: string) => void;
}

export const AccountHeader = ({account, isScanning, onScanStarted}: AccountHeaderProps) => {
  const navigate = useNavigate();
  const {data: resourceSettings = []} = useResourceTypeSettings();

  const hasEnabledResources = resourceSettings.some(s => s.enabled);

  return (
      <div className="mb-6">
        <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/aws-accounts')}
            className="mb-4"
        >
          <ArrowLeft className="h-4 w-4 mr-2"/>
          Back to Accounts
        </Button>

        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <div className="account-header-icon">
              <Cloud className="h-8 w-8 account-header-icon-text"/>
            </div>
            <div>
              <h1 className="text-3xl font-bold text-foreground">
                {account.accountAlias}
              </h1>
              <p className="text-muted-foreground mt-1">
                Account ID: {account.accountId}
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <Badge variant={account.status === 'ACTIVE' ? 'default' : 'destructive'}>
              {account.status}
            </Badge>
            <Button
                variant="outline"
                onClick={() => navigate(`/aws-accounts/${account.id}/settings`)}
            >
              <Settings className="h-4 w-4 mr-2"/>
              Settings
            </Button>
            <ScanButton
                accountId={account.id}
                accountAlias={account.accountAlias}
                disabled={isScanning}
                hasEnabledResources={hasEnabledResources}
                onScanStarted={onScanStarted}
            />
          </div>
        </div>
      </div>
  );
};