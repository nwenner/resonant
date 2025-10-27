import axios from 'axios';

// Create configured axios instance
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
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const token = localStorage.getItem('token');
      const isAuthEndpoint = error.config?.url?.includes('/auth/login') || 
                            error.config?.url?.includes('/auth/register');
      
      if (token && !isAuthEndpoint) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Export the configured instance as default
export default api;