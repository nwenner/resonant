import { useEffect } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import { Plus, X } from 'lucide-react';
import {
  tagPolicyService,
  type TagPolicy,
  type CreateTagPolicyRequest,
  type Severity,
} from '@/services/tagPolicyService';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import { Switch } from '../ui/switch';
import { useToast } from '@/hooks/useToast';
import { Badge } from '../ui/badge';

interface PolicyFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  policy: TagPolicy | null;
  onSuccess: () => void;
}

interface FormData {
  name: string;
  description: string;
  severity: Severity;
  enabled: boolean;
  resourceTypes: string[];
  requiredTags: Array<{
    key: string;
    allowedValues: string;
    anyValue: boolean;
  }>;
}

const COMMON_RESOURCE_TYPES = [
  'ec2:instance',
  's3:bucket',
  'rds:db-instance',
  'lambda:function',
  'dynamodb:table',
  'ebs:volume',
  'ecs:service',
  'eks:cluster',
  'elb:loadbalancer',
  'cloudfront:distribution',
];

export function PolicyFormDialog({
  open,
  onOpenChange,
  policy,
  onSuccess,
}: PolicyFormDialogProps) {
  const { toast } = useToast();
  const isEditing = !!policy;

  const {
    register,
    control,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<FormData>({
    defaultValues: {
      name: '',
      description: '',
      severity: 'MEDIUM',
      enabled: true,
      resourceTypes: [],
      requiredTags: [{ key: '', allowedValues: '', anyValue: false }],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'requiredTags',
  });

  const selectedResourceTypes = watch('resourceTypes');

  // Load policy data when editing
  useEffect(() => {
    if (policy) {
      const tags = Object.entries(policy.requiredTags).map(([key, values]) => ({
        key,
        allowedValues: values ? values.join(', ') : '',
        anyValue: values === null,
      }));

      reset({
        name: policy.name,
        description: policy.description,
        severity: policy.severity,
        enabled: policy.enabled,
        resourceTypes: policy.resourceTypes,
        requiredTags: tags.length > 0 ? tags : [{ key: '', allowedValues: '', anyValue: false }],
      });
    } else {
      reset({
        name: '',
        description: '',
        severity: 'MEDIUM',
        enabled: true,
        resourceTypes: [],
        requiredTags: [{ key: '', allowedValues: '', anyValue: false }],
      });
    }
  }, [policy, reset]);

  const createMutation = useMutation({
    mutationFn: (data: CreateTagPolicyRequest) => tagPolicyService.create(data),
    onSuccess: () => {
      toast({
        title: 'Policy created',
        description: 'The tag policy has been created successfully.',
      });
      onSuccess();
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to create the policy. Please try again.',
        variant: 'destructive',
      });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateTagPolicyRequest }) =>
      tagPolicyService.update(id, data),
    onSuccess: () => {
      toast({
        title: 'Policy updated',
        description: 'The tag policy has been updated successfully.',
      });
      onSuccess();
    },
    onError: () => {
      toast({
        title: 'Error',
        description: 'Failed to update the policy. Please try again.',
        variant: 'destructive',
      });
    },
  });

  const onSubmit = (data: FormData) => {
    // Convert form data to API format
    const requiredTags: Record<string, string[] | null> = {};
    data.requiredTags.forEach((tag) => {
      if (tag.key.trim()) {
        requiredTags[tag.key.trim()] = tag.anyValue
          ? null
          : tag.allowedValues
              .split(',')
              .map((v) => v.trim())
              .filter(Boolean);
      }
    });

    const payload: CreateTagPolicyRequest = {
      name: data.name,
      description: data.description,
      severity: data.severity,
      enabled: data.enabled,
      resourceTypes: data.resourceTypes,
      requiredTags,
    };

    if (isEditing && policy) {
      updateMutation.mutate({ id: policy.id, data: payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const toggleResourceType = (resourceType: string) => {
    const current = selectedResourceTypes || [];
    if (current.includes(resourceType)) {
      setValue(
        'resourceTypes',
        current.filter((t) => t !== resourceType)
      );
    } else {
      setValue('resourceTypes', [...current, resourceType]);
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? 'Edit Policy' : 'Create New Policy'}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? 'Update the tag policy configuration'
              : 'Define a new tag compliance policy for your AWS resources'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Basic Info */}
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Policy Name *</Label>
              <Input
                id="name"
                {...register('name', { required: 'Name is required' })}
                placeholder="e.g., Production Environment Policy"
              />
              {errors.name && (
                <p className="text-sm text-destructive">{errors.name.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                {...register('description')}
                placeholder="Describe what this policy enforces..."
                rows={3}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="severity">Severity *</Label>
                <Select
                  value={watch('severity')}
                  onValueChange={(value) => setValue('severity', value as Severity)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOW">Low</SelectItem>
                    <SelectItem value="MEDIUM">Medium</SelectItem>
                    <SelectItem value="HIGH">High</SelectItem>
                    <SelectItem value="CRITICAL">Critical</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="enabled">Status</Label>
                <div className="flex items-center space-x-2 h-10">
                  <Switch
                    id="enabled"
                    checked={watch('enabled')}
                    onCheckedChange={(checked) => setValue('enabled', checked)}
                  />
                  <Label htmlFor="enabled" className="cursor-pointer">
                    {watch('enabled') ? 'Enabled' : 'Disabled'}
                  </Label>
                </div>
              </div>
            </div>
          </div>

          {/* Required Tags */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Required Tags *</Label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => append({ key: '', allowedValues: '', anyValue: false })}
              >
                <Plus className="h-4 w-4 mr-1" />
                Add Tag
              </Button>
            </div>

            <div className="space-y-3">
              {fields.map((field, index) => (
                <div key={field.id} className="flex items-start gap-2 p-3 border rounded-lg">
                  <div className="flex-1 space-y-2">
                    <Input
                      {...register(`requiredTags.${index}.key`, {
                        required: 'Tag key is required',
                      })}
                      placeholder="Tag key (e.g., Environment)"
                    />
                    {!watch(`requiredTags.${index}.anyValue`) && (
                      <Input
                        {...register(`requiredTags.${index}.allowedValues`)}
                        placeholder="Allowed values (comma-separated, e.g., prod, staging)"
                      />
                    )}
                    <div className="flex items-center space-x-2">
                      <Switch
                        checked={watch(`requiredTags.${index}.anyValue`)}
                        onCheckedChange={(checked) => {
                          setValue(`requiredTags.${index}.anyValue`, checked);
                          if (checked) {
                            setValue(`requiredTags.${index}.allowedValues`, '');
                          }
                        }}
                      />
                      <Label className="text-sm text-muted-foreground">
                        Accept any value
                      </Label>
                    </div>
                  </div>
                  {fields.length > 1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => remove(index)}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Resource Types */}
          <div className="space-y-2">
            <Label>Resource Types *</Label>
            <div className="flex flex-wrap gap-2">
              {COMMON_RESOURCE_TYPES.map((resourceType) => (
                <Badge
                  key={resourceType}
                  variant={
                    selectedResourceTypes?.includes(resourceType) ? 'default' : 'outline'
                  }
                  className="cursor-pointer"
                  onClick={() => toggleResourceType(resourceType)}
                >
                  {resourceType}
                </Badge>
              ))}
            </div>
            {selectedResourceTypes?.length === 0 && (
              <p className="text-sm text-destructive">
                Select at least one resource type
              </p>
            )}
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? 'Saving...' : isEditing ? 'Update Policy' : 'Create Policy'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}