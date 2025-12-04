import {useMutation, UseMutationOptions, useQueryClient} from '@tanstack/react-query';
import {useToast} from './useToast';
import {AxiosError} from 'axios';

interface ApiErrorResponse {
  message?: string;
}

interface MutationWithToastOptions<TData, TVariables> {
  mutationFn: (variables: TVariables) => Promise<TData>;
  invalidateKeys?: (string | readonly string[])[];
  successMessage?: string;
  errorMessage?: string;
  onSuccess?: (data: TData, variables: TVariables) => void;
  onError?: (error: AxiosError<ApiErrorResponse>, variables: TVariables) => void;
}

export const useMutationWithToast = <TData = unknown, TVariables = void>(
    options: MutationWithToastOptions<TData, TVariables>
) => {
  const queryClient = useQueryClient();
  const {toast} = useToast();

  return useMutation<TData, AxiosError<ApiErrorResponse>, TVariables>({
    mutationFn: options.mutationFn,
    onSuccess: (data, variables) => {
      // Invalidate queries
      if (options.invalidateKeys) {
        options.invalidateKeys.forEach((key) => {
          queryClient.invalidateQueries({queryKey: key as string[]});
        });
      }

      // Show success toast
      if (options.successMessage) {
        toast({
          title: 'Success',
          description: options.successMessage,
        });
      }

      // Call custom onSuccess
      options.onSuccess?.(data, variables);
    },
    onError: (error, variables) => {
      // Show error toast
      const message = options.errorMessage ||
          error.response?.data?.message ||
          'An error occurred. Please try again.';

      toast({
        title: 'Error',
        description: message,
        variant: 'destructive',
      });

      // Call custom onError
      options.onError?.(error, variables);
    },
  } as UseMutationOptions<TData, AxiosError<ApiErrorResponse>, TVariables>);
};