import api from '@/lib/api';
import {AuthResponse, BackendAuthResponse, LoginRequest, RegisterRequest} from "@/types/auth";
import {User} from "@/types/user";

const transformAuthResponse = (backendData: BackendAuthResponse): AuthResponse => {
  return {
    token: backendData.token,
    user: {
      id: backendData.id,
      email: backendData.email,
      name: backendData.name,
      role: backendData.role || 'USER',
      enabled: backendData.enabled ?? true,
    },
  };
};

// Auth API Service
export const authService = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post<BackendAuthResponse>('/auth/login', data);
    return transformAuthResponse(response.data);
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await api.post<BackendAuthResponse>('/auth/register', data);
    return transformAuthResponse(response.data);
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },
};