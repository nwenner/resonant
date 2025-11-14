import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Button} from '@/components/ui/button';
import {Badge} from '@/components/ui/badge';
import {ComplianceViolation} from "@/types/complianceViolation";
import './RecentViolationsPreview.css';

interface RecentViolationsPreviewProps {
  violations: ComplianceViolation[];
  openCount: number;
  onViewAll?: () => void;
}

export const RecentViolationsPreview = ({
                                          violations,
                                          openCount,
                                          onViewAll
                                        }: RecentViolationsPreviewProps) => {
  if (openCount === 0) {
    return null;
  }

  return (
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Recent Violations</CardTitle>
              <CardDescription>{openCount} open violations</CardDescription>
            </div>
            {onViewAll && (
                <Button variant="outline" size="sm" onClick={onViewAll}>
                  View All
                </Button>
            )}
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            {violations.slice(0, 5).map((violation) => (
                <div key={violation.id} className="violation-item">
                  <div>
                    <div className="violation-item-title">
                      {violation.resourceName || violation.resourceArn.split('/').pop()}
                    </div>
                    <div className="violation-item-subtitle">
                      {violation.policyName}
                    </div>
                  </div>
                  <Badge variant="destructive">{violation.severity}</Badge>
                </div>
            ))}
          </div>
        </CardContent>
      </Card>
  );
};
