import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { resourceService } from '@/services/resourceService';
import { AwsResource } from '@/types/scan';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Package, ChevronDown, ChevronUp } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

interface ResourcesTableProps {
  accountId?: string;
}

export const ResourcesTable = ({ accountId }: ResourcesTableProps) => {
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [selectedResource, setSelectedResource] = useState<AwsResource | null>(null);
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const { data: resources = [], isLoading } = useQuery({
    queryKey: ['resources', accountId, typeFilter],
    queryFn: async () => {
      if (accountId) {
        return resourceService.getAccountResources(accountId);
      }
      return resourceService.listResources(
        typeFilter && typeFilter !== 'all' ? { type: typeFilter } : undefined
      );
    },
  });

  const toggleRowExpansion = (resourceId: string) => {
    setExpandedRows((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(resourceId)) {
        newSet.delete(resourceId);
      } else {
        newSet.add(resourceId);
      }
      return newSet;
    });
  };

  const resourceTypes = Array.from(new Set(resources.map((r) => r.resourceType))).sort();
  const filteredResources = typeFilter && typeFilter !== 'all'
    ? resources.filter((r) => r.resourceType === typeFilter)
    : resources;

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>AWS Resources</CardTitle>
              <CardDescription>
                {filteredResources.length} resource{filteredResources.length !== 1 ? 's' : ''} discovered
              </CardDescription>
            </div>
            {resourceTypes.length > 0 && (
              <Select value={typeFilter} onValueChange={setTypeFilter}>
                <SelectTrigger className="w-[200px]">
                  <SelectValue placeholder="All resource types" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Types</SelectItem>
                  {resourceTypes.map((type) => (
                    <SelectItem key={type} value={type}>
                      {type}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-slate-500">Loading resources...</div>
          ) : filteredResources.length === 0 ? (
            <div className="text-center py-8">
              <Package className="h-12 w-12 text-slate-300 dark:text-slate-700 mx-auto mb-3" />
              <p className="text-slate-600 dark:text-slate-400">No resources found</p>
              <p className="text-sm text-slate-500 dark:text-slate-500 mt-1">
                Run a scan to discover resources
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[40px]"></TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Region</TableHead>
                  <TableHead>Tags</TableHead>
                  <TableHead>Last Seen</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredResources.map((resource) => {
                  const isExpanded = expandedRows.has(resource.id);

                  return (
                    <React.Fragment key={resource.id}>
                      <TableRow
                        className="cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800"
                        onClick={() => toggleRowExpansion(resource.id)}
                      >
                        <TableCell>
                          {isExpanded ? (
                            <ChevronUp className="h-4 w-4 text-slate-400" />
                          ) : (
                            <ChevronDown className="h-4 w-4 text-slate-400" />
                          )}
                        </TableCell>
                        <TableCell>
                          <div>
                            <div className="font-medium text-slate-900 dark:text-white">
                              {resource.name || resource.resourceArn.split('/').pop()}
                            </div>
                            <div className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-[300px]">
                              {resource.resourceId}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="secondary">{resource.resourceType}</Badge>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-slate-600 dark:text-slate-400">
                            {resource.region}
                          </span>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">{resource.tagCount} tags</Badge>
                        </TableCell>
                        <TableCell className="text-sm text-slate-600 dark:text-slate-400">
                          {formatDistanceToNow(new Date(resource.lastSeenAt), { addSuffix: true })}
                        </TableCell>
                      </TableRow>
                      {isExpanded && (
                        <TableRow>
                          <TableCell colSpan={6} className="bg-slate-50 dark:bg-slate-800/50">
                            <div className="py-4 space-y-4">
                              {/* Tags */}
                              {Object.keys(resource.tags).length > 0 && (
                                <div>
                                  <h4 className="text-sm font-medium text-slate-900 dark:text-white mb-2">
                                    Tags:
                                  </h4>
                                  <div className="grid grid-cols-2 gap-2">
                                    {Object.entries(resource.tags).map(([key, value]) => (
                                      <div
                                        key={key}
                                        className="text-sm p-2 rounded bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                                      >
                                        <span className="font-medium text-slate-900 dark:text-white">
                                          {key}:
                                        </span>{' '}
                                        <span className="text-slate-600 dark:text-slate-400">{value}</span>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              )}

                              {/* Metadata */}
                              {Object.keys(resource.metadata).length > 0 && (
                                <div>
                                  <h4 className="text-sm font-medium text-slate-900 dark:text-white mb-2">
                                    Metadata:
                                  </h4>
                                  <div className="text-xs font-mono bg-slate-900 dark:bg-slate-950 text-slate-100 p-3 rounded overflow-x-auto">
                                    <pre>{JSON.stringify(resource.metadata, null, 2)}</pre>
                                  </div>
                                </div>
                              )}

                              {/* ARN */}
                              <div className="text-xs text-slate-500 dark:text-slate-400 pt-2 border-t border-slate-200 dark:border-slate-700">
                                <span className="font-medium">ARN:</span> {resource.resourceArn}
                              </div>
                              <div className="text-xs text-slate-500 dark:text-slate-400">
                                <span className="font-medium">Discovered:</span>{' '}
                                {formatDistanceToNow(new Date(resource.discoveredAt), { addSuffix: true })}
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

      {/* Resource Details Modal (optional) */}
      <Dialog open={!!selectedResource} onOpenChange={() => setSelectedResource(null)}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Resource Details</DialogTitle>
            <DialogDescription>{selectedResource?.name || selectedResource?.resourceArn}</DialogDescription>
          </DialogHeader>
          {selectedResource && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-slate-600 dark:text-slate-400">Type</div>
                  <Badge variant="secondary">{selectedResource.resourceType}</Badge>
                </div>
                <div>
                  <div className="text-sm text-slate-600 dark:text-slate-400">Region</div>
                  <div className="font-medium">{selectedResource.region}</div>
                </div>
                <div>
                  <div className="text-sm text-slate-600 dark:text-slate-400">Resource ID</div>
                  <div className="text-sm font-mono">{selectedResource.resourceId}</div>
                </div>
                <div>
                  <div className="text-sm text-slate-600 dark:text-slate-400">Tags</div>
                  <Badge variant="outline">{selectedResource.tagCount} tags</Badge>
                </div>
              </div>

              {Object.keys(selectedResource.tags).length > 0 && (
                <div className="pt-4 border-t border-slate-200 dark:border-slate-700">
                  <h4 className="text-sm font-medium mb-2">Tags:</h4>
                  <div className="grid grid-cols-2 gap-2">
                    {Object.entries(selectedResource.tags).map(([key, value]) => (
                      <div
                        key={key}
                        className="text-sm p-2 rounded bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700"
                      >
                        <span className="font-medium">{key}:</span> {value}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="text-xs text-slate-500 dark:text-slate-400 pt-4 border-t border-slate-200 dark:border-slate-700">
                <div className="mb-2">
                  <span className="font-medium">ARN:</span> {selectedResource.resourceArn}
                </div>
                <div>
                  <span className="font-medium">Discovered:</span>{' '}
                  {formatDistanceToNow(new Date(selectedResource.discoveredAt), { addSuffix: true })}
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
};