// src/pages/Dashboard.tsx
import { useAuthStore } from '@/store/authStore';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { awsAccountsService } from '@/services/awsAccountsService';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tag, Shield, Activity, AlertTriangle, Cloud, ArrowRight, CheckCircle } from 'lucide-react';

interface AwsAccount {
  id: string;
  accountId: string;
  accountAlias: string;
  status: string;
}

export const Dashboard = () => {
  const { user } = useAuthStore();
  const navigate = useNavigate();

  // Fetch AWS accounts
  const { data: accounts = [] } = useQuery<AwsAccount[]>({
    queryKey: ['aws-accounts'],
    queryFn: async () => {
      return await awsAccountsService.listAccounts();
    }
  });

  const hasAccounts = accounts.length > 0;

  const stats = [
    {
      title: 'Connected Accounts',
      value: accounts.length.toString(),
      description: hasAccounts ? `${accounts.length} AWS account${accounts.length !== 1 ? 's' : ''} connected` : 'No AWS accounts connected',
      icon: Cloud,
      color: 'text-blue-600',
      bgColor: 'bg-blue-50 dark:bg-blue-900/20',
    },
    {
      title: 'Compliance Rate',
      value: '0%',
      description: 'Awaiting first scan',
      icon: Shield,
      color: 'text-green-600',
      bgColor: 'bg-green-50 dark:bg-green-900/20',
    },
    {
      title: 'Active Policies',
      value: '0',
      description: 'No policies configured',
      icon: Activity,
      color: 'text-purple-600',
      bgColor: 'bg-purple-50 dark:bg-purple-900/20',
    },
    {
      title: 'Non-Compliant',
      value: '0',
      description: 'No violations detected',
      icon: AlertTriangle,
      color: 'text-amber-600',
      bgColor: 'bg-amber-50 dark:bg-amber-900/20',
    },
  ];

  const quickActions = [
    {
      title: 'AWS Accounts',
      description: hasAccounts 
        ? `Manage ${accounts.length} connected account${accounts.length !== 1 ? 's' : ''}` 
        : 'Connect and manage your AWS accounts for compliance monitoring',
      icon: Cloud,
      color: 'text-blue-600',
      bgColor: 'bg-blue-100 dark:bg-blue-900',
      action: () => navigate('/aws-accounts'),
      enabled: true,
      badge: hasAccounts ? accounts.length.toString() : undefined,
    },
    {
      title: 'Tag Policies',
      description: 'Define required tags and validation rules for your resources',
      icon: Tag,
      color: 'text-purple-600',
      bgColor: 'bg-purple-100 dark:bg-purple-900',
      action: () => {},
      enabled: false,
    },
    {
      title: 'Compliance Reports',
      description: 'View detailed reports and track compliance trends over time',
      icon: Shield,
      color: 'text-green-600',
      bgColor: 'bg-green-100 dark:bg-green-900',
      action: () => {},
      enabled: false,
    },
  ];

  return (
    <Layout>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome Section */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white">
            Welcome back, {user?.name?.split(' ')[0]}
          </h1>
          <p className="text-slate-600 dark:text-slate-400 mt-2">
            Here's an overview of your AWS tag compliance status
          </p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {stats.map((stat) => {
            const Icon = stat.icon;
            return (
              <Card key={stat.title}>
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium text-slate-600 dark:text-slate-400">
                    {stat.title}
                  </CardTitle>
                  <div className={`p-2 rounded-lg ${stat.bgColor}`}>
                    <Icon className={`h-4 w-4 ${stat.color}`} />
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-slate-900 dark:text-white">{stat.value}</div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">{stat.description}</p>
                </CardContent>
              </Card>
            );
          })}
        </div>

        {/* Quick Actions */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-slate-900 dark:text-white mb-4">Quick Actions</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {quickActions.map((action) => {
              const Icon = action.icon;
              return (
                <Card 
                  key={action.title}
                  className={`${action.enabled ? 'hover:shadow-lg cursor-pointer transition-shadow' : 'opacity-60'}`}
                  onClick={action.enabled ? action.action : undefined}
                >
                  <CardHeader>
                    <div className="flex items-start justify-between mb-3">
                      <div className={`w-10 h-10 rounded-lg ${action.bgColor} flex items-center justify-center`}>
                        <Icon className={`h-5 w-5 ${action.color}`} />
                      </div>
                      {action.badge && (
                        <span className="px-2 py-1 text-xs font-semibold bg-blue-600 text-white rounded-full">
                          {action.badge}
                        </span>
                      )}
                    </div>
                    <CardTitle className="text-lg">{action.title}</CardTitle>
                    <CardDescription>{action.description}</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <Button 
                      variant="ghost" 
                      className="w-full justify-between group"
                      disabled={!action.enabled}
                      onClick={action.enabled ? action.action : undefined}
                    >
                      {action.enabled ? (hasAccounts && action.title === 'AWS Accounts' ? 'Manage Accounts' : 'Get Started') : 'Coming Soon'}
                      {action.enabled && (
                        <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                      )}
                    </Button>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </div>

        {/* Getting Started Guide */}
        <Card>
          <CardHeader>
            <CardTitle>Getting Started</CardTitle>
            <CardDescription>Follow these steps to start monitoring your AWS resources</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Step 1 - Connect AWS Account */}
            <div className="flex items-start gap-4">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-white font-bold shrink-0 ${
                hasAccounts ? 'bg-green-600 dark:bg-green-500' : 'bg-blue-600 dark:bg-blue-500'
              }`}>
                {hasAccounts ? <CheckCircle className="w-5 h-5" /> : '1'}
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <h4 className="font-semibold text-slate-900 dark:text-white">Connect AWS Account</h4>
                  {hasAccounts && (
                    <span className="px-2 py-0.5 text-xs font-medium bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-400 rounded">
                      Complete
                    </span>
                  )}
                </div>
                <p className="text-sm text-slate-600 dark:text-slate-400 mb-2">
                  {hasAccounts 
                    ? `${accounts.length} AWS account${accounts.length !== 1 ? 's' : ''} connected and ready for scanning`
                    : 'Link your AWS account using IAM roles for secure, read-only access'
                  }
                </p>
                {hasAccounts ? (
                  <Button size="sm" variant="outline" onClick={() => navigate('/aws-accounts')}>
                    Manage Accounts
                  </Button>
                ) : (
                  <Button size="sm" onClick={() => navigate('/aws-accounts')}>
                    Connect Now
                  </Button>
                )}
              </div>
            </div>

            {/* Step 2 - Create Tag Policies */}
            <div className={`flex items-start gap-4 ${!hasAccounts ? 'opacity-50' : ''}`}>
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-white font-bold shrink-0 ${
                hasAccounts ? 'bg-blue-600 dark:bg-blue-500' : 'bg-slate-300 dark:bg-slate-700'
              }`}>
                2
              </div>
              <div className="flex-1">
                <h4 className="font-semibold text-slate-900 dark:text-white mb-1">Create Tag Policies</h4>
                <p className="text-sm text-slate-600 dark:text-slate-400 mb-2">
                  Define required tags and validation rules for different resource types
                </p>
                {hasAccounts && (
                  <Button size="sm" disabled>
                    Coming Soon
                  </Button>
                )}
              </div>
            </div>

            {/* Step 3 - Monitor Compliance */}
            <div className="flex items-start gap-4 opacity-50">
              <div className="w-8 h-8 bg-slate-300 dark:bg-slate-700 rounded-full flex items-center justify-center text-white font-bold shrink-0">
                3
              </div>
              <div className="flex-1">
                <h4 className="font-semibold text-slate-900 dark:text-white mb-1">Monitor Compliance</h4>
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  View real-time compliance status and receive alerts for violations
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
};