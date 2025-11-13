import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {GettingStartedStep} from './GettingStartedStep';

interface GettingStartedGuideProps {
  hasAccounts: boolean;
  accountCount: number;
  hasPolicies: boolean;
  policyCount: number;
  enabledPolicies: number;
  onNavigateToAccounts: () => void;
  onNavigateToPolicies: () => void;
}

export const GettingStartedGuide = ({
                                      hasAccounts,
                                      accountCount,
                                      hasPolicies,
                                      policyCount,
                                      enabledPolicies,
                                      onNavigateToAccounts,
                                      onNavigateToPolicies,
                                    }: GettingStartedGuideProps) => {
  return (
      <Card>
        <CardHeader>
          <CardTitle>Getting Started</CardTitle>
          <CardDescription>Follow these steps to start monitoring your AWS
            resources</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <GettingStartedStep
              stepNumber={1}
              title="Connect AWS Account"
              description={
                hasAccounts
                    ? `${accountCount} AWS account${accountCount !== 1 ? 's' : ''} connected and ready for scanning`
                    : 'Link your AWS account using IAM roles for secure, read-only access'
              }
              isComplete={hasAccounts}
              isEnabled={true}
              actionLabel={hasAccounts ? 'Manage Accounts' : 'Connect Now'}
              onAction={onNavigateToAccounts}
              actionVariant={hasAccounts ? 'outline' : 'default'}
          />

          <GettingStartedStep
              stepNumber={2}
              title="Create Tag Policies"
              description={
                hasPolicies
                    ? `${policyCount} ${policyCount === 1 ? 'policy' : 'policies'} configured (${enabledPolicies} active)`
                    : 'Define required tags and validation rules for different resource types'
              }
              isComplete={hasPolicies}
              isEnabled={hasAccounts}
              actionLabel={hasPolicies ? 'Manage Policies' : 'Create Policy'}
              onAction={hasAccounts ? onNavigateToPolicies : undefined}
          />

          <GettingStartedStep
              stepNumber={3}
              title="Monitor Compliance"
              description="View real-time compliance status and receive alerts for violations"
              isComplete={false}
              isEnabled={hasPolicies}
              actionLabel={hasPolicies ? 'Coming Soon' : undefined}
              actionDisabled={true}
          />
        </CardContent>
      </Card>
  );
};
