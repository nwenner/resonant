import {AlertTriangle} from 'lucide-react';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../ui/alert-dialog';
import type {TagPolicy} from '@/services/tagPolicyService';

interface DeletePolicyDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  policy: TagPolicy | null;
  onConfirm: () => void;
  isDeleting: boolean;
}

export function PolicyDeleteDialog({
                                     open,
                                     onOpenChange,
                                     policy,
                                     onConfirm,
                                     isDeleting,
                                   }: DeletePolicyDialogProps) {
  if (!policy) return null;

  return (
      <AlertDialog open={open} onOpenChange={onOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-5 w-5 text-destructive"/>
              <AlertDialogTitle>Delete Policy</AlertDialogTitle>
            </div>
            <AlertDialogDescription className="space-y-2">
              <p>
                Are you sure you want to delete the policy{' '}
                <span className="font-semibold text-foreground">"{policy.name}"</span>?
              </p>
              <p>
                This action cannot be undone. Any compliance checks using this policy will
                be affected.
              </p>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction
                onClick={onConfirm}
                disabled={isDeleting}
                className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {isDeleting ? 'Deleting...' : 'Delete Policy'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
  );
}