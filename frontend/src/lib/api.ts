import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');

    // Don't add token to public auth endpoints
    const isPublicEndpoint = config.url?.includes('/auth/login') ||
                             config.url?.includes('/auth/register');

    if (token && !isPublicEndpoint) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle 401 errors
// Response interceptor to handle 401 errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Only redirect on 401 if we're not already on the login/register page
    // and if there was a token (meaning we were authenticated)
    if (error.response?.status === 401) {
      const token = localStorage.getItem('token');
      const isAuthEndpoint = error.config?.url?.includes('/auth/login') || 
                            error.config?.url?.includes('/auth/register');
      
      // Only redirect if we had a token and this isn't a login/register attempt
      if (token && !isAuthEndpoint) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Types
export interface User {
  id: string;
  email: string;
  name: string;
  role: 'USER' | 'ADMIN';
  enabled: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  name: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

// Auth API
export const authApi = {
  login: (data: LoginRequest) =>
    api.post<AuthResponse>('/auth/login', data),
  
  register: (data: RegisterRequest) =>
    api.post<AuthResponse>('/auth/register', data),
  
  getCurrentUser: () =>
    api.get<User>('/auth/me'),
};

// Users API
export const usersApi = {
  getAll: () => api.get<User[]>('/users'),
  getById: (id: string) => api.get<User>(`/users/${id}`),
  update: (id: string, data: Partial<User>) => api.put<User>(`/users/${id}`, data),
  delete: (id: string) => api.delete(`/users/${id}`),
};

export default api;