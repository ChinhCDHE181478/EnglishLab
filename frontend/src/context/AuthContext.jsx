import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { getCurrentUser } from '../api/authApi';
import { clearSession, getStoredUser, hasAccessToken } from '../utils/auth';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => (hasAccessToken() ? getStoredUser() : null));
  const [status, setStatus] = useState(() => (hasAccessToken() ? 'checking' : 'guest'));

  const saveSession = useCallback(({ accessToken, user: nextUser }) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('user', JSON.stringify(nextUser));
    setUser(nextUser);
    setStatus('authenticated');
  }, []);

  const updateUser = useCallback((nextUser) => {
    if (!nextUser) {
      localStorage.removeItem('user');
      setUser(null);
      return;
    }
    localStorage.setItem('user', JSON.stringify(nextUser));
    setUser(nextUser);
  }, []);

  const refreshUser = useCallback(async () => {
    if (!hasAccessToken()) {
      clearSession();
      setUser(null);
      setStatus('guest');
      return null;
    }

    try {
      const response = await getCurrentUser();
      updateUser(response.data);
      setStatus('authenticated');
      return response.data;
    } catch (error) {
      clearSession();
      setUser(null);
      setStatus('guest');
      throw error;
    }
  }, [updateUser]);

  const logout = useCallback(() => {
    clearSession();
    setUser(null);
    setStatus('guest');
  }, []);

  useEffect(() => {
    if (!hasAccessToken()) {
      setStatus('guest');
      setUser(null);
      return undefined;
    }

    let active = true;
    const loadCurrentUser = async () => {
      try {
        const response = await getCurrentUser();
        if (!active) return;
        updateUser(response.data);
        setStatus('authenticated');
      } catch {
        if (!active) return;
        clearSession();
        setUser(null);
        setStatus('guest');
      }
    };

    loadCurrentUser();

    return () => {
      active = false;
    };
  }, [updateUser]);

  const value = useMemo(
    () => ({
      isAuthenticated: status === 'authenticated' && Boolean(user),
      logout,
      refreshUser,
      saveSession,
      status,
      updateUser,
      user,
    }),
    [logout, refreshUser, saveSession, status, updateUser, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
};
