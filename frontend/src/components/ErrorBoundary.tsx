import React, {Component, ReactNode} from 'react';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from './ui/card';
import {Button} from './ui/button';
import {AlertTriangle, RefreshCw} from 'lucide-react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {hasError: false, error: null};
  }

  static getDerivedStateFromError(error: Error): State {
    return {hasError: true, error};
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({hasError: false, error: null});
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
          <div className="min-h-screen bg-app flex items-center justify-center p-4">
            <Card className="max-w-md w-full">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-[hsl(var(--semantic-red)_/_0.15)]">
                    <AlertTriangle className="h-6 w-6 text-error"/>
                  </div>
                  <div>
                    <CardTitle>Something went wrong</CardTitle>
                    <CardDescription>
                      We encountered an unexpected error
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {this.state.error && (
                    <div
                        className="p-3 rounded-lg bg-muted text-sm font-mono text-tertiary overflow-auto max-h-32">
                      {this.state.error.message}
                    </div>
                )}
                <Button onClick={this.handleReset} className="w-full">
                  <RefreshCw className="h-4 w-4 mr-2"/>
                  Reload Application
                </Button>
              </CardContent>
            </Card>
          </div>
      );
    }

    return this.props.children;
  }
}