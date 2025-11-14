import {useAuthStore} from '@/store/authStore';
import {useNavigate} from 'react-router-dom';
import {useAwsAccounts} from '@/hooks/useAwsAccounts';
import {useTagPolicyStats} from '@/hooks/useTagPolicies';
import {Layout} from '@/components/Layout';
import {StatsCard} from '@/components/shared/StatsCard';
import {QuickActionCard} from '@/components/dashboard/QuickActionCard';
import {GettingStartedGuide} from '@/components/dashboard/GettingStartedGuide';
import {Activity, AlertTriangle, Cloud, Shield, Tag} from 'lucide-react';

export const Dashboard = () => {
  const {user} = useAuthStore();
  const navigate = useNavigate();

  const {data: accounts = []} = useAwsAccounts();
  const {data: policyStats} = useTagPolicyStats();

  const hasAccounts = accounts.length > 0;
  const hasPolicies = (policyStats?.total ?? 0) > 0;
  const enabledPolicies = policyStats?.enabled ?? 0;

  const stats = [
    {
      title: 'Connected Accounts',
      value: accounts.length.toString(),
      description: hasAccounts
          ? `${accounts.length} AWS account${accounts.length !== 1 ? 's' : ''} connected`
          : 'No AWS accounts connected',
      icon: Cloud,
      variant: 'info' as const
    },
    {
      title: 'Compliance Rate',
      value: '0%',
      description: 'Awaiting first scan',
      icon: Shield,
      variant: 'success' as const
    },
    {
      title: 'Active Policies',
      value: enabledPolicies.toString(),
      description: hasPolicies
          ? `${enabledPolicies} of ${policyStats?.total ?? 0} enabled`
          : 'No policies configured',
      icon: Activity,
      variant: 'info' as const
    },
    {
      title: 'Non-Compliant',
      value: '0',
      description: 'No violations detected',
      icon: AlertTriangle,
      variant: 'warning' as const
    },
  ];

  const quickActions = [
    {
      title: 'AWS Accounts',
      description: hasAccounts
          ? `Manage ${accounts.length} connected account${accounts.length !== 1 ? 's' : ''}`
          : 'Connect and manage your AWS accounts for compliance monitoring',
      icon: Cloud,
      variant: 'primary' as const,
      action: () => navigate('/aws-accounts'),
      enabled: true,
      badge: hasAccounts ? accounts.length.toString() : undefined,
    },
    {
      title: 'Tag Policies',
      description: hasPolicies
          ? `Manage ${policyStats?.total ?? 0} tag compliance ${(policyStats?.total ?? 0) === 1 ? 'policy' : 'policies'}`
          : 'Define required tags and validation rules for your resources',
      icon: Tag,
      variant: 'secondary' as const,
      action: () => navigate('/tag-policies'),
      enabled: true,
      badge: hasPolicies ? (policyStats?.total ?? 0).toString() : undefined,
    },
    {
      title: 'Compliance Reports',
      description: 'View detailed reports and track compliance trends over time',
      icon: Shield,
      variant: 'tertiary' as const,
      action: () => {
      },
      enabled: false,
    },
  ];

  return (
      <Layout>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Welcome Section */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold">
              Welcome back, {user?.name}
            </h1>
            <p className="text-muted-foreground mt-2">
              Here's an overview of your AWS tag compliance status
            </p>
          </div>

          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            {stats.map((stat) => (
                <StatsCard key={stat.title} {...stat} />
            ))}
          </div>

          {/* Quick Actions */}
          <div className="mb-8">
            <h2 className="text-xl font-semibold mb-4">Quick Actions</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {quickActions.map((action) => (
                  <QuickActionCard key={action.title} {...action} />
              ))}
            </div>
          </div>

          {/* Getting Started Guide */}
          <GettingStartedGuide
              hasAccounts={hasAccounts}
              accountCount={accounts.length}
              hasPolicies={hasPolicies}
              policyCount={policyStats?.total ?? 0}
              enabledPolicies={enabledPolicies}
              onNavigateToAccounts={() => navigate('/aws-accounts')}
              onNavigateToPolicies={() => navigate('/tag-policies')}
          />
        </div>
      </Layout>
  );
};
