import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { StatusBadge } from '@/components/resonant-ui/StatusBadge';
import { RefreshCw, Trash2, Edit2, Check } from 'lucide-react';

export interface Account {
  id: string;
  accountId: string;
  accountAlias: string;
  status: string;
  roleArn: string;
  lastSyncedAt: string | null;
}

interface AccountCardProps {
  account: Account;
  isTestingConnection?: boolean;
  isDeletingAccount?: boolean;
  isUpdatingAlias?: boolean;
  onCardClick: (accountId: string) => void;
  onTestConnection: (accountId: string) => void;
  onDeleteAccount: (accountId: string) => void;
  onUpdateAlias: (accountId: string, newAlias: string) => void;
}

export const AccountCard = ({
  account,
  isTestingConnection = false,
  isDeletingAccount = false,
  isUpdatingAlias = false,
  onCardClick,
  onTestConnection,
  onDeleteAccount,
  onUpdateAlias
}: AccountCardProps) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editedAlias, setEditedAlias] = useState('');

  const handleStartEdit = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsEditing(true);
    setEditedAlias(account.accountAlias || '');
  };

  const handleSaveAlias = () => {
    if (editedAlias.trim()) {
      onUpdateAlias(account.id, editedAlias.trim());
      setIsEditing(false);
    }
  };

  const handleTestConnection = (e: React.MouseEvent) => {
    e.stopPropagation();
    onTestConnection(account.id);
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm('Are you sure you want to disconnect this account?')) {
      onDeleteAccount(account.id);
    }
  };

  return (
    <Card
      className="hover:shadow-lg transition-shadow cursor-pointer"
      onClick={() => onCardClick(account.id)}
    >
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="flex-1">
            {isEditing ? (
              <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                <Input
                  value={editedAlias}
                  onChange={(e) => setEditedAlias(e.target.value)}
                  className="h-8"
                  placeholder="Account alias"
                />
                <Button
                  size="sm"
                  onClick={handleSaveAlias}
                  disabled={isUpdatingAlias}
                >
                  <Check className="w-4 h-4" />
                </Button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <CardTitle className="text-lg">{account.accountAlias}</CardTitle>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleStartEdit}
                >
                  <Edit2 className="w-3 h-3" />
                </Button>
              </div>
            )}
            <CardDescription className="font-mono text-xs">{account.accountId}</CardDescription>
          </div>
          <StatusBadge status={account.status} />
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <div>
          <Label className="text-xs text-muted-foreground">Role ARN</Label>
          <p className="text-sm font-mono truncate text-foreground" title={account.roleArn}>
            {account.roleArn}
          </p>
        </div>
        {account.lastSyncedAt && (
          <div>
            <Label className="text-xs text-muted-foreground">Last Synced</Label>
            <p className="text-sm text-foreground">
              {new Date(account.lastSyncedAt).toLocaleString()}
            </p>
          </div>
        )}
      </CardContent>
      <CardFooter className="flex gap-2">
        <Button
          variant="outline"
          size="sm"
          onClick={handleTestConnection}
          disabled={isTestingConnection}
          className="flex-1"
        >
          <RefreshCw className={`w-3 h-3 mr-1 ${isTestingConnection ? 'animate-spin' : ''}`} />
          Test
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={handleDelete}
          disabled={isDeletingAccount}
          className="text-destructive hover:bg-destructive/10"
        >
          <Trash2 className="w-3 h-3" />
        </Button>
      </CardFooter>
    </Card>
  );
};