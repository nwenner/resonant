import {ArrowRight, LucideIcon} from 'lucide-react';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Button} from '@/components/ui/button';

type QuickActionVariant = 'primary' | 'secondary' | 'tertiary';

interface QuickActionCardProps {
  title: string;
  description: string;
  icon: LucideIcon;
  variant: QuickActionVariant;
  action: () => void;
  enabled: boolean;
  badge?: string;
}

const variantStyles: Record<QuickActionVariant, { icon: string; bg: string }> = {
  primary: {
    icon: 'text-quick-action-primary-foreground',
    bg: 'bg-quick-action-primary'
  },
  secondary: {
    icon: 'text-quick-action-secondary-foreground',
    bg: 'bg-quick-action-secondary'
  },
  tertiary: {
    icon: 'text-quick-action-tertiary-foreground',
    bg: 'bg-quick-action-tertiary'
  }
};

export const QuickActionCard = ({
                                  title,
                                  description,
                                  icon: Icon,
                                  variant,
                                  action,
                                  enabled,
                                  badge,
                                }: QuickActionCardProps) => {
  const styles = variantStyles[variant];

  return (
      <Card className={enabled ? 'hover:shadow-lg transition-shadow' : 'opacity-60'}>
        <CardHeader>
          <div className="flex items-start justify-between mb-3">
            <div className={`w-10 h-10 rounded-lg ${styles.bg} flex items-center justify-center`}>
              <Icon className={`h-5 w-5 ${styles.icon}`}/>
            </div>
            {badge && (
                <span
                    className="px-2 py-1 text-xs font-semibold bg-blue-600 text-white rounded-full">
                  {badge}
                </span>
            )}
          </div>
          <CardTitle className="text-lg">{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        <CardContent>
          <Button
              variant="ghost"
              className="w-full justify-between group"
              disabled={!enabled}
              onClick={enabled ? action : undefined}
          >
            {enabled ? (badge ? 'Manage' : 'Get Started') : 'Coming Soon'}
            {enabled && (
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform"/>
            )}
          </Button>
        </CardContent>
      </Card>
  );
};
