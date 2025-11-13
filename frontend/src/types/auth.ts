import {User} from "@/types/user";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  name: string;
  password: string;
}

export interface BackendAuthResponse {
  token: string;
  type: string; // "Bearer"
  id: string;
  name: string;
  email: string;
  role?: 'USER' | 'ADMIN';
  enabled?: boolean;
}

export interface AuthResponse {
  token: string;
  user: User;
}