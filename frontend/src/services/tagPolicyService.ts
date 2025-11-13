import api from '@/lib/api';
import {Severity} from "@/types/severity";
import {TagPolicy, TagPolicyStats} from "@/types/tagPolicy";

export interface CreateTagPolicyRequest {
  name: string;
  description: string;
  requiredTags: Record<string, string[] | null>;
  resourceTypes: string[];
  severity: Severity;
  enabled: boolean;
}

interface UpdateTagPolicyRequest {
  name?: string;
  description?: string;
  requiredTags?: Record<string, string[] | null>;
  resourceTypes?: string[];
  severity?: Severity;
  enabled?: boolean;
}

export const tagPolicyService = {
  /**
   * Get all tag policies with optional filtering
   */
  getAll: async (enabled?: boolean): Promise<TagPolicy[]> => {
    const params = enabled !== undefined ? {enabled} : {};
    const response = await api.get<TagPolicy[]>('/tag-policies', {params});
    return response.data;
  },

  /**
   * Get a single tag policy by ID
   */
  getById: async (id: string): Promise<TagPolicy> => {
    const response = await api.get<TagPolicy>(`/tag-policies/${id}`);
    return response.data;
  },

  /**
   * Create a new tag policy
   */
  create: async (data: CreateTagPolicyRequest): Promise<TagPolicy> => {
    const response = await api.post<TagPolicy>('/tag-policies', data);
    return response.data;
  },

  /**
   * Update an existing tag policy
   */
  update: async (id: string, data: UpdateTagPolicyRequest): Promise<TagPolicy> => {
    const response = await api.put<TagPolicy>(`/tag-policies/${id}`, data);
    return response.data;
  },

  /**
   * Enable a tag policy
   */
  enable: async (id: string): Promise<TagPolicy> => {
    const response = await api.post<TagPolicy>(`/tag-policies/${id}/enable`);
    return response.data;
  },

  /**
   * Disable a tag policy
   */
  disable: async (id: string): Promise<TagPolicy> => {
    const response = await api.post<TagPolicy>(`/tag-policies/${id}/disable`);
    return response.data;
  },

  /**
   * Delete a tag policy
   */
  delete: async (id: string): Promise<void> => {
    await api.delete(`/tag-policies/${id}`);
  },

  /**
   * Get tag policy statistics
   */
  getStats: async (): Promise<TagPolicyStats> => {
    const response = await api.get<TagPolicyStats>('/tag-policies/stats');
    return response.data;
  },
};