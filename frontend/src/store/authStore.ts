import {create} from 'zustand';
import {persist} from 'zustand/middleware';
import {User} from "@/types/user";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  setAuth: (user: User, token: string) => void;
  clearAuth: () => void;
  updateUser: (user: User) => void;
  validateAuth: () => void;
}

const isTokenExpired = (token: string): boolean => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const expirationTime = payload.exp * 1000;
    return Date.now() >= expirationTime;
  } catch {
    return true;
  }
};

export const useAuthStore = create<AuthState>()(
    persist(
        (set, get) => ({
          user: null,
          token: null,
          isAuthenticated: false,

          setAuth: (user: User, token: string) => {
            set({user, token, isAuthenticated: true});
          },

          clearAuth: () => {
            set({user: null, token: null, isAuthenticated: false});
          },

          updateUser: (user: User) => {
            set({user});
          },

          validateAuth: () => {
            const {token, isAuthenticated} = get();
            if (isAuthenticated && (!token || isTokenExpired(token))) {
              set({user: null, token: null, isAuthenticated: false});
            }
          }
        }),
        {
          name: 'auth-storage',
          // Validate state after hydration from localStorage
          onRehydrateStorage: () => (state) => {
            state?.validateAuth();
          },
        }
    )
);