import {Edit, Power, PowerOff, Trash2} from 'lucide-react';
import {Badge} from '../ui/badge';
import {Button} from '../ui/button';
import {Skeleton} from '../ui/skeleton';
import {Switch} from '../ui/switch';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '../ui/table';
import {TagPolicy} from "@/types/tagPolicy.ts";
import {Severity} from "@/types/severity.ts";
import './PolicyList.css';

interface PolicyListProps {
  policies: TagPolicy[];
  isLoading: boolean;
  onEdit: (policy: TagPolicy) => void;
  onDelete: (policy: TagPolicy) => void;
  onToggleEnabled: (policy: TagPolicy) => void;
}

const severityClasses: Record<Severity, string> = {
  LOW: 'severity-badge-low',
  MEDIUM: 'severity-badge-medium',
  HIGH: 'severity-badge-high',
  CRITICAL: 'severity-badge-critical',
};

export function PolicyList({
                             policies,
                             isLoading,
                             onEdit,
                             onDelete,
                             onToggleEnabled,
                           }: PolicyListProps) {
  if (isLoading) {
    return (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
              <div key={i} className="flex items-center justify-between p-4 border rounded-lg">
                <div className="space-y-2 flex-1">
                  <Skeleton className="h-5 w-48"/>
                  <Skeleton className="h-4 w-96"/>
                </div>
                <Skeleton className="h-8 w-24"/>
              </div>
          ))}
        </div>
    );
  }

  if (policies.length === 0) {
    return (
        <div className="text-center py-12 text-muted-foreground">
          <p className="text-lg">No policies found</p>
          <p className="text-sm mt-1">Create your first tag policy to get started</p>
        </div>
    );
  }

  return (
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Description</TableHead>
              <TableHead>Severity</TableHead>
              <TableHead>Resources</TableHead>
              <TableHead className="text-center">Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {policies.map((policy) => (
                <TableRow key={policy.id}>
                  <TableCell className="font-medium max-w-[200px]">
                    <div className="truncate">{policy.name}</div>
                  </TableCell>
                  <TableCell className="max-w-[300px]">
                    <div className="truncate text-sm text-muted-foreground">
                      {policy.description}
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge className={severityClasses[policy.severity]} variant="secondary">
                      {policy.severity}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm text-muted-foreground">
                      {policy.resourceTypes.length} type{policy.resourceTypes.length !== 1 ? 's' : ''}
                    </div>
                  </TableCell>
                  <TableCell className="text-center">
                    <div className="flex items-center justify-center gap-2">
                      <Switch
                          checked={policy.enabled}
                          onCheckedChange={() => onToggleEnabled(policy)}
                      />
                      {policy.enabled ? (
                          <Power className="h-4 w-4 text-green-500"/>
                      ) : (
                          <PowerOff className="h-4 w-4 text-gray-400"/>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-2">
                      <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => onEdit(policy)}
                          title="Edit policy"
                      >
                        <Edit className="h-4 w-4"/>
                      </Button>
                      <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => onDelete(policy)}
                          title="Delete policy"
                      >
                        <Trash2 className="h-4 w-4 text-destructive"/>
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
  );
}