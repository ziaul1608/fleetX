import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { UserDto } from '../../features/auth/types';
import { authApi } from '../../features/auth/api/authApi';

export interface AuthContextType {
  currentUser: UserDto | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<UserDto>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<UserDto | null>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<UserDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const checkSession = useCallback(async (): Promise<UserDto | null> => {
    setLoading(true);
    setError(null);
    try {
      const user = await authApi.getMe();
      setCurrentUser(user);
      return user;
    } catch (err: unknown) {
      const apiErr = err as { status?: number; errorCode?: string; message?: string };
      if (apiErr.status === 401 || apiErr.errorCode === 'UNAUTHORIZED') {
        // Standard unauthenticated state - no active session
        setCurrentUser(null);
      } else {
        // Distinct network or server error during session verification
        setError(apiErr.message || 'Unable to verify session with backend.');
        setCurrentUser(null);
      }
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkSession();
  }, [checkSession]);

  const login = useCallback(async (email: string, password: string): Promise<UserDto> => {
    setError(null);
    const loggedInUser = await authApi.login({ email, password });
    setCurrentUser(loggedInUser);
    return loggedInUser;
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    try {
      await authApi.logout();
    } finally {
      setCurrentUser(null);
    }
  }, []);

  const refreshUser = useCallback(async (): Promise<UserDto | null> => {
    return checkSession();
  }, [checkSession]);

  const value: AuthContextType = {
    currentUser,
    isAuthenticated: Boolean(currentUser),
    loading,
    error,
    login,
    logout,
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
