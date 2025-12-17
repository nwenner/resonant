export interface ResourceTypeSetting {
  id: string;
  resourceType: string;
  displayName: string;
  description: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateResourceTypeSettingRequest {
  enabled: boolean;
}