import { FileCheck, Shield, ShieldOff } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Skeleton } from '../ui/skeleton';
import type { TagPolicyStats } from '@/services/tagPolicyService';

interface PolicyStatsProps {
  stats?: TagPolicyStats;
  isLoading: boolean;
}

export function PolicyStats({ stats, isLoading }: PolicyStatsProps) {
  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-3">
        {[1, 2, 3].map((i) => (
          <Card key={i}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-4 rounded" />
            </CardHeader>
            <CardContent>
              <Skeleton className="h-8 w-16" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  if (!stats) return null;

  const statItems = [
    {
      title: 'Total Policies',
      value: stats.total,
      icon: FileCheck,
      iconColor: 'text-blue-500',
    },
    {
      title: 'Enabled',
      value: stats.enabled,
      icon: Shield,
      iconColor: 'text-green-500',
    },
    {
      title: 'Disabled',
      value: stats.disabled,
      icon: ShieldOff,
      iconColor: 'text-gray-500',
    },
  ];

  return (
    <div className="grid gap-4 md:grid-cols-3">
      {statItems.map((item) => (
        <Card key={item.title}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{item.title}</CardTitle>
            <item.icon className={`h-4 w-4 ${item.iconColor}`} />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{item.value}</div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}