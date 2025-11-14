import {LucideIcon} from 'lucide-react';
import {Card, CardContent} from '@/components/ui/card';
import {Badge} from '@/components/ui/badge';
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
      <Card className={enabled ? 'cursor-pointer hover:shadow-lg transition-shadow' : 'opacity-60'}>
        <CardContent className="p-6" onClick={enabled ? action : undefined}>
          <div className="flex items-start justify-between mb-4">
            <div className={`p-3 rounded-lg ${styles.bg}`}>
              <Icon className={`w-6 h-6 ${styles.icon}`}/>
            </div>
            {badge && (
                <Badge variant="secondary">{badge}</Badge>
            )}
          </div>
          <h3 className="text-lg font-semibold mb-2">{title}</h3>
          <p className="text-muted-foreground text-sm mb-4">{description}</p>
          {enabled ? (
              <Button variant="outline" size="sm" className="w-full">
                Open
              </Button>
          ) : (
              <Button variant="outline" size="sm" className="w-full" disabled>
                Coming Soon
              </Button>
          )}
        </CardContent>
      </Card>
  );
};
