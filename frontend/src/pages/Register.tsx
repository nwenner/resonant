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

export const Register = () => {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [error, setError] = useState('');

  const registerMutation = useMutation({
    mutationFn: authService.register,
    onSuccess: (response) => {
      setAuth(response.user, response.token);
      navigate('/dashboard');
    },
    onError: (error: AxiosError<{ message?: string }>) => {
      setError(error.response?.data?.message || 'Registration failed. Please try again.');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (formData.password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }

    registerMutation.mutate({
      name: formData.name,
      email: formData.email,
      password: formData.password,
    });
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  return (
      <div className="min-h-screen flex items-center justify-center bg-auth-gradient px-4">
        <div className="w-full max-w-md">
          <div className="flex items-center justify-center mb-8">
            <Tag className="h-10 w-10 text-icon-blue mr-3"/>
            <h1 className="text-3xl font-bold text-foreground">Resonant</h1>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Create your account</CardTitle>
              <CardDescription>Get started with AWS tag compliance</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit} className="space-y-4">
                {error && (
                    <div className="error-box">
                      {error}
                    </div>
                )}

                <div className="space-y-2">
                  <label htmlFor="name" className="text-sm font-medium text-muted-foreground">
                    Full Name
                  </label>
                  <Input
                      id="name"
                      name="name"
                      type="text"
                      placeholder="John Doe"
                      value={formData.name}
                      onChange={handleChange}
                      required
                      disabled={registerMutation.isPending}
                  />
                </div>

                <div className="space-y-2">
                  <label htmlFor="email" className="text-sm font-medium text-muted-foreground">
                    Email
                  </label>
                  <Input
                      id="email"
                      name="email"
                      type="email"
                      placeholder="you@company.com"
                      value={formData.email}
                      onChange={handleChange}
                      required
                      disabled={registerMutation.isPending}
                  />
                </div>

                <div className="space-y-2">
                  <label htmlFor="password" className="text-sm font-medium text-muted-foreground">
                    Password
                  </label>
                  <Input
                      id="password"
                      name="password"
                      type="password"
                      placeholder="••••••••"
                      value={formData.password}
                      onChange={handleChange}
                      required
                      disabled={registerMutation.isPending}
                  />
                </div>

                <div className="space-y-2">
                  <label htmlFor="confirmPassword"
                         className="text-sm font-medium text-muted-foreground">
                    Confirm Password
                  </label>
                  <Input
                      id="confirmPassword"
                      name="confirmPassword"
                      type="password"
                      placeholder="••••••••"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      required
                      disabled={registerMutation.isPending}
                  />
                </div>

                <Button
                    type="submit"
                    className="w-full"
                    disabled={registerMutation.isPending}
                >
                  {registerMutation.isPending ? 'Creating account...' : 'Create account'}
                </Button>

                <p className="text-center text-sm text-muted-foreground">
                  Already have an account?{' '}
                  <Link to="/login" className="text-link font-medium">
                    Sign in
                  </Link>
                </p>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
  );
};