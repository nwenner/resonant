import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ErrorBoundary} from '@/components/ErrorBoundary';
import {ProtectedRoute} from '@/components/ProtectedRoute';
import {Login} from '@/pages/Login';
import {Register} from '@/pages/Register';
import {Dashboard} from '@/pages/Dashboard';
import {AwsAccounts} from '@/pages/AwsAccounts';
import {AwsAccountDetail} from '@/pages/AwsAccountDetail';
import {TagPolicies} from '@/pages/TagPolicies';
import {Toaster} from '@/components/ui/toaster';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function App() {
  return (
      <ErrorBoundary>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <Routes>
              {/* Public routes */}
              <Route path="/login" element={<Login/>}/>
              <Route path="/register" element={<Register/>}/>

              {/* Protected routes - wrapped with ErrorBoundary at the parent level */}
              <Route element={<ProtectedRoute/>}>
                <Route
                    path="/dashboard"
                    element={
                      <ErrorBoundary>
                        <Dashboard/>
                      </ErrorBoundary>
                    }
                />
                <Route
                    path="/aws-accounts"
                    element={
                      <ErrorBoundary>
                        <AwsAccounts/>
                      </ErrorBoundary>
                    }
                />
                <Route
                    path="/aws-accounts/:accountId"
                    element={
                      <ErrorBoundary>
                        <AwsAccountDetail/>
                      </ErrorBoundary>
                    }
                />
                <Route
                    path="/tag-policies"
                    element={
                      <ErrorBoundary>
                        <TagPolicies/>
                      </ErrorBoundary>
                    }
                />
              </Route>

              {/* Redirect root to dashboard */}
              <Route path="/" element={<Navigate to="/dashboard" replace/>}/>
            </Routes>
          </BrowserRouter>
          <Toaster/>
        </QueryClientProvider>
      </ErrorBoundary>
  );
}

export default App;