import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import './StatsCard.css';
import {StatItem} from "@/types/statItem.ts";

export const StatsCard = ({
                            title,
                            value,
                            description,
                            icon: Icon,
                            variant,
                            onClick,
                            className
                          }: StatItem) => {
  return (
      <Card onClick={onClick} className={className}>
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