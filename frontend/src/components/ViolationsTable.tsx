import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { violationService } from '@/services/violationService';
import { ComplianceViolation } from '@/types/scan';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { AlertCircle, Eye, EyeOff, RefreshCw, ChevronDown, ChevronUp } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { useToast } from "@/hooks/useToast"

interface ViolationsTableProps {
  accountId?: string;
}

const getSeverityConfig = (severity: ComplianceViolation['severity']) => {
  switch (severity) {
    case 'CRITICAL':
      return { variant: 'destructive' as const, color: 'text-red-600 dark:text-red-400' };
    case 'HIGH':
      return { variant: 'destructive' as const, color: 'text-orange-600 dark:text-orange-400' };
    case 'MEDIUM':
      return { variant: 'secondary' as const, color: 'text-yellow-600 dark:text-yellow-400' };
    case 'LOW':
      return { variant: 'secondary' as const, color: 'text-blue-600 dark:text-blue-400' };
  }
};

const getStatusConfig = (status: ComplianceViolation['status']) => {
  switch (status) {
    case 'OPEN':
      return { variant: 'destructive' as const, label: 'Open' };
    case 'RESOLVED':
      return { variant: 'default' as const, label: 'Resolved' };
    case 'IGNORED':
      return { variant: 'secondary' as const, label: 'Ignored' };
  }
};

export const ViolationsTable = ({ accountId }: ViolationsTableProps) => {
  const [statusFilter, setStatusFilter] = useState<string>('OPEN');
  const [selectedViolation, setSelectedViolation] = useState<ComplianceViolation | null>(null);
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const { data: violations = [], isLoading } = useQuery({
    queryKey: ['violations', accountId, statusFilter],
    queryFn: async () => {
      if (accountId) {
        return violationService.getAccountViolations(accountId);
      }
      return violationService.listViolations(
        statusFilter && statusFilter !== 'all' ? { status: statusFilter } : undefined
      );
    },
  });

  const ignoreMutation = useMutation({
    mutationFn: (violationId: string) => violationService.ignoreViolation(violationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['violations'] });
      toast({
        title: 'Violation Ignored',
        description: 'The violation has been marked as ignored',
      });
      setSelectedViolation(null);
    },
    onError: (error: any) => {
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to ignore violation',
        variant: 'destructive',
      });
    },
  });

  const reopenMutation = useMutation({
    mutationFn: (violationId: string) => violationService.reopenViolation(violationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['violations'] });
      toast({
        title: 'Violation Reopened',
        description: 'The violation has been reopened',
      });
      setSelectedViolation(null);
    },
    onError: (error: any) => {
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to reopen violation',
        variant: 'destructive',
      });
    },
  });

  const toggleRowExpansion = (violationId: string) => {
    setExpandedRows((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(violationId)) {
        newSet.delete(violationId);
      } else {
        newSet.add(violationId);
      }
      return newSet;
    });
  };

  const filteredViolations = statusFilter && statusFilter !== 'all'
    ? violations.filter((v) => v.status === statusFilter)
    : violations;

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Compliance Violations</CardTitle>
              <CardDescription>
                {filteredViolations.length} violation{filteredViolations.length !== 1 ? 's' : ''} found
              </CardDescription>
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Filter by status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="OPEN">Open</SelectItem>
                <SelectItem value="RESOLVED">Resolved</SelectItem>
                <SelectItem value="IGNORED">Ignored</SelectItem>
                <SelectItem value="all">All Statuses</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-slate-500">Loading violations...</div>
          ) : filteredViolations.length === 0 ? (
            <div className="text-center py-8">
              <AlertCircle className="h-12 w-12 text-slate-300 dark:text-slate-700 mx-auto mb-3" />
              <p className="text-slate-600 dark:text-slate-400">No violations found</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[40px]"></TableHead>
                  <TableHead>Resource</TableHead>
                  <TableHead>Policy</TableHead>
                  <TableHead>Severity</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Detected</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredViolations.map((violation) => {
                  const isExpanded = expandedRows.has(violation.id);
                  const severityConfig = getSeverityConfig(violation.severity);
                  const statusConfig = getStatusConfig(violation.status);

                  return (
                    <React.Fragment key={violation.id}>
                      <TableRow className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800">
                        <TableCell onClick={() => toggleRowExpansion(violation.id)}>
                          {isExpanded ? (
                            <ChevronUp className="h-4 w-4 text-slate-400" />
                          ) : (
                            <ChevronDown className="h-4 w-4 text-slate-400" />
                          )}
                        </TableCell>
                        <TableCell>
                          <div>
                            <div className="font-medium text-slate-900 dark:text-white">
                              {violation.resourceName || violation.resourceArn.split('/').pop()}
                            </div>
                            <div className="text-xs text-slate-500 dark:text-slate-400">
                              {violation.resourceType}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm">{violation.policyName}</div>
                        </TableCell>
                        <TableCell>
                          <Badge variant={severityConfig.variant}>{violation.severity}</Badge>
                        </TableCell>
                        <TableCell>
                          <Badge variant={statusConfig.variant}>{statusConfig.label}</Badge>
                        </TableCell>
                        <TableCell className="text-sm text-slate-600 dark:text-slate-400">
                          {formatDistanceToNow(new Date(violation.detectedAt), { addSuffix: true })}
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setSelectedViolation(violation)}
                          >
                            View Details
                          </Button>
                        </TableCell>
                      </TableRow>
                      {isExpanded && (
                        <TableRow>
                          <TableCell colSpan={7} className="bg-slate-50 dark:bg-slate-800/50">
                            <div className="py-4 space-y-3">
                              {violation.violationDetails.missingTags && violation.violationDetails.missingTags.length > 0 && (
                                <div>
                                  <h4 className="text-sm font-medium text-slate-900 dark:text-white mb-2">
                                    Missing Tags:
                                  </h4>
                                  <div className="flex flex-wrap gap-2">
                                    {violation.violationDetails.missingTags.map((tag) => (
                                      <Badge key={tag} variant="outline">
                                        {tag}
                                      </Badge>
                                    ))}
                                  </div>
                                </div>
                              )}
                              {violation.violationDetails.invalidTags && Object.keys(violation.violationDetails.invalidTags).length > 0 && (
                                <div>
                                  <h4 className="text-sm font-medium text-slate-900 dark:text-white mb-2">
                                    Invalid Tags:
                                  </h4>
                                  <div className="space-y-2">
                                    {Object.entries(violation.violationDetails.invalidTags).map(([key, value]) => (
                                      <div key={key} className="text-sm">
                                        <span className="font-medium">{key}:</span>
                                        <span className="text-red-600 dark:text-red-400 ml-2">
                                          {value.current}
                                        </span>
                                        <span className="text-slate-500 dark:text-slate-400 mx-2">→</span>
                                        <span className="text-green-600 dark:text-green-400">
                                          Allowed: {value.allowed.join(', ')}
                                        </span>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              )}
                              <div className="text-xs text-slate-500 dark:text-slate-400 pt-2 border-t border-slate-200 dark:border-slate-700">
                                ARN: {violation.resourceArn}
                              </div>
                            </div>
                          </TableCell>
                        </TableRow>
                      )}
                    </React.Fragment>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Violation Details Modal */}
      <Dialog open={!!selectedViolation} onOpenChange={() => setSelectedViolation(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Violation Details</DialogTitle>
            <DialogDescription>
              {selectedViolation?.resourceName || selectedViolation?.resourceArn}
            </DialogDescription>
          </DialogHeader>
          {selectedViolation && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-slate-600 dark:text-slate-400">Policy</div>
                  <div className="font-medium">{selectedViolation.policyName}</div>
                </div>
                <div>
                  <div className="text-sm text-slate-600 dark:text-slate-400">Severity</div>
                  <Badge variant={getSeverityConfig(selectedViolation.severity).variant}>
                    {selectedViolation.severity}
                  </Badge>
                </div>
                <div>
                  <div className="text-sm text-slate-600 dark:text-slate-400">Status</div>
                  <Badge variant={getStatusConfig(selectedViolation.status).variant}>
                    {getStatusConfig(selectedViolation.status).label}
                  </Badge>
                </div>
                <div>
                  <div className="text-sm text-slate-600 dark:text-slate-400">Detected</div>
                  <div className="text-sm">
                    {formatDistanceToNow(new Date(selectedViolation.detectedAt), { addSuffix: true })}
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-slate-200 dark:border-slate-700 space-y-4">
                {selectedViolation.violationDetails.missingTags && selectedViolation.violationDetails.missingTags.length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2">Missing Tags:</h4>
                    <div className="flex flex-wrap gap-2">
                      {selectedViolation.violationDetails.missingTags.map((tag) => (
                        <Badge key={tag} variant="outline">
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}
                {selectedViolation.violationDetails.invalidTags && Object.keys(selectedViolation.violationDetails.invalidTags).length > 0 && (
                  <div>
                    <h4 className="text-sm font-medium mb-2">Invalid Tags:</h4>
                    <div className="space-y-2">
                      {Object.entries(selectedViolation.violationDetails.invalidTags).map(([key, value]) => (
                        <div key={key} className="text-sm">
                          <div className="font-medium">{key}</div>
                          <div className="text-red-600 dark:text-red-400">Current: {value.current}</div>
                          <div className="text-green-600 dark:text-green-400">
                            Allowed: {value.allowed.join(', ')}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              <div className="flex gap-2 pt-4">
                {selectedViolation.status === 'OPEN' && (
                  <Button
                    variant="outline"
                    onClick={() => ignoreMutation.mutate(selectedViolation.id)}
                    disabled={ignoreMutation.isPending}
                  >
                    <EyeOff className="h-4 w-4 mr-2" />
                    Ignore
                  </Button>
                )}
                {(selectedViolation.status === 'IGNORED' || selectedViolation.status === 'RESOLVED') && (
                  <Button
                    variant="outline"
                    onClick={() => reopenMutation.mutate(selectedViolation.id)}
                    disabled={reopenMutation.isPending}
                  >
                    <RefreshCw className="h-4 w-4 mr-2" />
                    Reopen
                  </Button>
                )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
};