export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  enabled: boolean;
}

export type UserRole = 'USER' | 'ADMIN';