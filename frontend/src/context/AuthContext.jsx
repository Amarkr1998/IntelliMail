import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import * as authApi from '../api/authApi';
import * as userApi from '../api/userApi';
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from '../api/axiosClient';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const persistTokens = (accessToken, refreshToken) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  };

  const clearSession = useCallback(() => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    setUser(null);
  }, []);

  useEffect(() => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (!token) {
      setLoading(false);
      return;
    }
    userApi
      .getProfile()
      .then(setUser)
      .catch(() => clearSession())
      .finally(() => setLoading(false));
  }, [clearSession]);

  const login = async (credentials) => {
    const data = await authApi.login(credentials);
    persistTokens(data.accessToken, data.refreshToken);
    setUser(data.user);
    return data.user;
  };

  const register = async (payload) => {
    const data = await authApi.register(payload);
    persistTokens(data.accessToken, data.refreshToken);
    setUser(data.user);
    return data.user;
  };

  const loginWithGoogle = async (idToken) => {
    const data = await authApi.loginWithGoogle(idToken);
    persistTokens(data.accessToken, data.refreshToken);
    setUser(data.user);
    return data.user;
  };

  const logout = () => {
    clearSession();
  };

  const refreshProfile = async () => {
    const profile = await userApi.getProfile();
    setUser(profile);
    return profile;
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, loginWithGoogle, logout, refreshProfile }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
