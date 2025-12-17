import api from '@/lib/api';
import type {
  ResourceTypeSetting,
  UpdateResourceTypeSettingRequest
} from '@/types/resourceTypeSetting';

export const resourceTypeSettingService = {
  getAll: async (): Promise<ResourceTypeSetting[]> => {
    const response = await api.get<ResourceTypeSetting[]>('/resource-type-settings');
    return response.data;
  },

  updateEnabled: async (resourceType: string, enabled: boolean): Promise<ResourceTypeSetting> => {
    const request: UpdateResourceTypeSettingRequest = {enabled};
    const response = await api.put<ResourceTypeSetting>(
        `/resource-type-settings/${resourceType}/enabled`,
        request
    );
    return response.data;
  },
};