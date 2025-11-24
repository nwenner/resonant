import {Button} from '@/components/ui/button';
import {CheckCircle} from 'lucide-react';

interface GettingStartedStepProps {
  stepNumber: number;
  title: string;
  description: string;
  isComplete: boolean;
  isEnabled: boolean;
  actionLabel?: string;
  onAction?: () => void;
  actionDisabled?: boolean;
  actionVariant?: 'default' | 'outline';
}

export const GettingStartedStep = ({
                                     stepNumber,
                                     title,
                                     description,
                                     isComplete,
                                     isEnabled,
                                     actionLabel,
                                     onAction,
                                     actionDisabled = false,
                                     actionVariant = 'default',
                                   }: GettingStartedStepProps) => {
  return (
      <div className={`flex items-start gap-4 ${!isEnabled ? 'opacity-50' : ''}`}>
        <div
            className={`w-8 h-8 rounded-full flex items-center justify-center text-white font-bold shrink-0 ${
                isComplete
                    ? 'bg-[hsl(var(--status-success))]'
                    : isEnabled
                        ? 'bg-[hsl(var(--semantic-blue))]'
                        : 'bg-muted'
            }`}
        >
          {isComplete ? <CheckCircle className="w-5 h-5"/> : stepNumber}
        </div>
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1">
            <h4 className="font-semibold">{title}</h4>
            {isComplete && (
                <span
                    className="px-2 py-0.5 text-xs font-medium bg-[hsl(var(--status-success))]/10 text-[hsl(var(--status-success-foreground))] rounded">
              Complete
            </span>
            )}
          </div>
          <p className="text-sm text-muted-foreground mb-2">{description}</p>
          {actionLabel && onAction && (
              <Button size="sm" variant={actionVariant} onClick={onAction}
                      disabled={actionDisabled}>
                {actionLabel}
              </Button>
          )}
        </div>
      </div>
  );
};