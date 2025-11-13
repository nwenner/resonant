type JsonValue = string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue };

export interface AwsResource {
  id: string;
  resourceId: string;
  resourceArn: string;
  resourceType: string;
  region: string;
  name: string;
  tags: Record<string, string>;
  metadata: Record<string, JsonValue>;
  tagCount: number;
  discoveredAt: string;
  lastSeenAt: string;
}

export interface ResourceStats {
  total: number;
  byType: Record<string, number>;
}