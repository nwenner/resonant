import {useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useMutation} from '@tanstack/react-query';
import {authService} from '@/services/authService';
import {useAuthStore} from '@/store/authStore';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {Tag} from 'lucide-react';
import {AxiosError} from "axios";

export const Login = () => {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const loginMutation = useMutation({
    mutationFn: authService.login,
    onSuccess: (response) => {
      setAuth(response.user, response.token);
      navigate('/dashboard');
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      setError(error.response?.data?.message || 'Login failed. Please check your credentials.');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    loginMutation.mutate({email, password});
  };

  return (
      <div
          className="min-h-screen flex items-center justify-center bg-gradient-to-br from-auth-background to-auth-background-subtle px-4">

        <div className="w-full max-w-md">
          <div className="flex items-center justify-center mb-8">
            <Tag className="h-10 w-10 text-quick-action-primary-foreground mr-3"/>
            <h1 className="text-3xl font-bold text-foreground">Resonant</h1>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Welcome back</CardTitle>
              <CardDescription>Sign in to your account to continue</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit} className="space-y-4">
                {error && (
                    <div
                        className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md">
                      {error}
                    </div>
                )}

                <div className="space-y-2">
                  <label htmlFor="email" className="text-sm font-medium text-slate-700">
                    Email
                  </label>
                  <Input
                      id="email"
                      type="email"
                      placeholder="you@company.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      disabled={loginMutation.isPending}
                  />
                </div>

                <div className="space-y-2">
                  <label htmlFor="password" className="text-sm font-medium text-slate-700">
                    Password
                  </label>
                  <Input
                      id="password"
                      type="password"
                      placeholder="••••••••"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      disabled={loginMutation.isPending}
                  />
                </div>

                <Button
                    type="submit"
                    className="w-full"
                    disabled={loginMutation.isPending}
                >
                  {loginMutation.isPending ? 'Signing in...' : 'Sign in'}
                </Button>

                <p className="text-center text-sm text-slate-600">
                  Don't have an account?{' '}
                  <Link to="/register" className="text-blue-600 hover:text-blue-700 font-medium">
                    Sign up
                  </Link>
                </p>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
  );
};