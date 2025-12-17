import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {resourceTypeSettingService} from '@/services/resourceTypeSettingService';
import type {ResourceTypeSetting} from '@/types/resourceTypeSetting';

export const useResourceTypeSettings = () => {
  return useQuery<ResourceTypeSetting[]>({
    queryKey: ['resource-type-settings'],
    queryFn: () => resourceTypeSettingService.getAll(),
  });
};

export const useUpdateResourceTypeSetting = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({resourceType, enabled}: { resourceType: string; enabled: boolean }) =>
        resourceTypeSettingService.updateEnabled(resourceType, enabled),
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: ['resource-type-settings']});
    },
  });
};