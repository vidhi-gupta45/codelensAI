import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User, AuthResponse } from '../types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (authData: AuthResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const savedUser = localStorage.getItem('codelens_user');
    return savedUser ? JSON.parse(savedUser) : null;
  });
  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('codelens_auth_token');
  });

  useEffect(() => {
    if (token) {
      localStorage.setItem('codelens_auth_token', token);
    } else {
      localStorage.removeItem('codelens_auth_token');
    }
  }, [token]);

  useEffect(() => {
    if (user) {
      localStorage.setItem('codelens_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('codelens_user');
    }
  }, [user]);

  const login = (authData: AuthResponse) => {
    setToken(authData.token);
    setUser({
      id: authData.userId,
      email: authData.email,
      username: authData.email ? authData.email.split('@')[0] : 'user',
    });
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('codelens_auth_token');
    localStorage.removeItem('codelens_user');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
