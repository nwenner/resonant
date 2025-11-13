export const QUERY_KEYS = {
  awsAccounts: {
    all: ['aws-accounts'] as const,
    lists: () => [...QUERY_KEYS.awsAccounts.all, 'list'] as const,
    list: (filters?: Record<string, unknown>) =>
      [...QUERY_KEYS.awsAccounts.lists(), filters] as const,
    details: () => [...QUERY_KEYS.awsAccounts.all, 'detail'] as const,
    detail: (id: string) => [...QUERY_KEYS.awsAccounts.details(), id] as const,
  },

  tagPolicies: {
    all: ['tag-policies'] as const,
    lists: () => [...QUERY_KEYS.tagPolicies.all, 'list'] as const,
    list: (filters?: Record<string, unknown>) =>
      [...QUERY_KEYS.tagPolicies.lists(), filters] as const,
    details: () => [...QUERY_KEYS.tagPolicies.all, 'detail'] as const,
    detail: (id: string) => [...QUERY_KEYS.tagPolicies.details(), id] as const,
  },

  scans: {
    all: ['scans'] as const,
    lists: () => [...QUERY_KEYS.scans.all, 'list'] as const,
    list: (filters?: Record<string, unknown>) =>
      [...QUERY_KEYS.scans.lists(), filters] as const,
    details: () => [...QUERY_KEYS.scans.all, 'detail'] as const,
    detail: (id: string) => [...QUERY_KEYS.scans.details(), id] as const,
    byAccount: (accountId: string) =>
      [...QUERY_KEYS.scans.all, 'account', accountId] as const,
  },

  violations: {
    all: ['violations'] as const,
    lists: () => [...QUERY_KEYS.violations.all, 'list'] as const,
    list: (filters?: Record<string, unknown>) =>
      [...QUERY_KEYS.violations.lists(), filters] as const,
    details: () => [...QUERY_KEYS.violations.all, 'detail'] as const,
    detail: (id: string) => [...QUERY_KEYS.violations.details(), id] as const,
  },

  resources: {
    all: ['resources'] as const,
    lists: () => [...QUERY_KEYS.resources.all, 'list'] as const,
    list: (filters?: Record<string, unknown>) =>
      [...QUERY_KEYS.resources.lists(), filters] as const,
    details: () => [...QUERY_KEYS.resources.all, 'detail'] as const,
    detail: (id: string) => [...QUERY_KEYS.resources.details(), id] as const,
  },
} as const;