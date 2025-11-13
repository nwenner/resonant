import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {LucideIcon} from 'lucide-react';
import './StatsCard.css';

interface StatsCardProps {
  title: string;
  value: string;
  description: string;
  icon: LucideIcon;
  variant: 'success' | 'error' | 'warning' | 'info';
}

export const StatsCard = ({
                            title,
                            value,
                            description,
                            icon: Icon,
                            variant
                          }: StatsCardProps) => {
  return (
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <CardTitle className="text-sm font-medium text-muted-foreground">
            {title}
          </CardTitle>
          <div className={`stats-card-icon stats-card-icon-${variant}`}>
            <Icon className={`h-4 w-4 stats-card-icon-text-${variant}`}/>
          </div>
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{value}</div>
          <p className="text-xs text-muted-foreground mt-1">{description}</p>
        </CardContent>
      </Card>
  );
};
