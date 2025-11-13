import {useState} from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {Filter, Plus} from 'lucide-react';
import {type TagPolicy, tagPolicyService} from '@/services/tagPolicyService';
import {Button} from '../components/ui/button';
import {Layout} from '@/components/Layout';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '../components/ui/card';
import {useToast} from '@/hooks/useToast';
import {PolicyList} from '../components/tag-policy/PolicyList';
import {PolicyFormDialog} from '../components/tag-policy/PolicyFormDialog';
import {PolicyDeleteDialog} from '../components/tag-policy/PolicyDeleteDialog';
import {PolicyStats} from '../components/tag-policy/PolicyStats';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';

type FilterType = 'all' | 'enabled' | 'disabled';

export function TagPolicies() {
  const [filter, setFilter] = useState<FilterType>('all');
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState<TagPolicy | null>(null);
  const [deletingPolicy, setDeletingPolicy] = useState<TagPolicy | null>(null);

  const {toast} = useToast();
  const queryClient = useQueryClient();

  // Fetch policies based on filter
  const {data: policies = [], isLoading} = useQuery({
    queryKey: ['tag-policies', filter],
    queryFn: () => {
      if (filter === 'all') return tagPolicyService.getAll();
      return tagPolicyService.getAll(filter === 'enabled');
    },
  });

  // Fetch stats
  const {data: stats} = useQuery({
    queryKey: ['tag-policies-stats'],
    queryFn: () => tagPolicyService.getStats(),
  });

  // Enable policy mutation
  const enableMutation = useMutation({
    mutationFn: (id: string) => tagPolicyService.enable(id),
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: ['tag-policies']});
      queryClient.invalidateQueries({queryKey: ['tag-policies-stats']});
      toast({
        title: 'Policy enabled',
        description: 'The policy has been enabled successfully.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to enable the policy. Please try again.',
        variant: 'destructive',
      });
    },
  });

  // Disable policy mutation
  const disableMutation = useMutation({
    mutationFn: (id: string) => tagPolicyService.disable(id),
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: ['tag-policies']});
      queryClient.invalidateQueries({queryKey: ['tag-policies-stats']});
      toast({
        title: 'Policy disabled',
        description: 'The policy has been disabled successfully.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to disable the policy. Please try again.',
        variant: 'destructive',
      });
    },
  });

  // Delete policy mutation
  const deleteMutation = useMutation({
    mutationFn: (id: string) => tagPolicyService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: ['tag-policies']});
      queryClient.invalidateQueries({queryKey: ['tag-policies-stats']});
      setDeletingPolicy(null);
      toast({
        title: 'Policy deleted',
        description: 'The policy has been deleted successfully.',
      });
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to delete the policy. Please try again.',
        variant: 'destructive',
      });
    },
  });

  const handleToggleEnabled = (policy: TagPolicy) => {
    if (policy.enabled) {
      disableMutation.mutate(policy.id);
    } else {
      enableMutation.mutate(policy.id);
    }
  };

  const handleEdit = (policy: TagPolicy) => {
    setEditingPolicy(policy);
    setIsFormOpen(true);
  };

  const handleDelete = (policy: TagPolicy) => {
    setDeletingPolicy(policy);
  };

  const handleCreateNew = () => {
    setEditingPolicy(null);
    setIsFormOpen(true);
  };

  const handleFormSuccess = () => {
    setIsFormOpen(false);
    setEditingPolicy(null);
    queryClient.invalidateQueries({queryKey: ['tag-policies']});
    queryClient.invalidateQueries({queryKey: ['tag-policies-stats']});
  };

  return (
      <Layout>
        <div className="container mx-auto py-8 space-y-6">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold">Tag Policies</h1>
              <p className="text-muted-foreground mt-1">
                Define and manage AWS resource tagging compliance rules
              </p>
            </div>
            <Button onClick={handleCreateNew}>
              <Plus className="mr-2 h-4 w-4"/>
              Create Policy
            </Button>
          </div>

          {/* Stats */}
          <PolicyStats stats={stats} isLoading={!stats}/>

          {/* Filters and List */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>All Policies</CardTitle>
                  <CardDescription>
                    View and manage your tag compliance policies
                  </CardDescription>
                </div>
                <div className="flex items-center gap-2">
                  <Filter className="h-4 w-4 text-muted-foreground"/>
                  <Select
                      value={filter}
                      onValueChange={(value) => setFilter(value as FilterType)}
                  >
                    <SelectTrigger className="w-[150px]">
                      <SelectValue/>
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Policies</SelectItem>
                      <SelectItem value="enabled">Enabled Only</SelectItem>
                      <SelectItem value="disabled">Disabled Only</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <PolicyList
                  policies={policies}
                  isLoading={isLoading}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onToggleEnabled={handleToggleEnabled}
              />
            </CardContent>
          </Card>

          {/* Create/Edit Dialog */}
          <PolicyFormDialog
              open={isFormOpen}
              onOpenChange={setIsFormOpen}
              policy={editingPolicy}
              onSuccess={handleFormSuccess}
          />

          {/* Delete Confirmation Dialog */}
          <PolicyDeleteDialog
              open={!!deletingPolicy}
              onOpenChange={(open) => !open && setDeletingPolicy(null)}
              policy={deletingPolicy}
              onConfirm={() => deletingPolicy && deleteMutation.mutate(deletingPolicy.id)}
              isDeleting={deleteMutation.isPending}
          />
        </div>
      </Layout>
  );
}