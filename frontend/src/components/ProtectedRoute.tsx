import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

export const ProtectedRoute = () => {
  const { isAuthenticated, token } = useAuthStore((state) => ({
    isAuthenticated: state.isAuthenticated,
    token: state.token,
  }));

  if (!isAuthenticated || !token) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
};