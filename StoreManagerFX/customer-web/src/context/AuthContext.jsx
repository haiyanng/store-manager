import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { loadCurrentUser, login as loginRequest, logout as logoutRequest } from '../api/auth';

const TOKEN_KEY = 'customer_web_token';
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(null);
  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(Boolean(localStorage.getItem(TOKEN_KEY)));
  const [authError, setAuthError] = useState('');

  useEffect(() => {
    let active = true;

    async function hydrateSession() {
      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const session = await loadCurrentUser();
        if (!active) {
          return;
        }
        setUser(session.user);
        setRole(session.role);
        setAuthError('');
      } catch (error) {
        if (!active) {
          return;
        }
        localStorage.removeItem(TOKEN_KEY);
        setToken(null);
        setUser(null);
        setRole(null);
        setAuthError(error.response?.data?.message || 'Session expired.');
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    hydrateSession();

    return () => {
      active = false;
    };
  }, [token]);

  async function signIn(email, password) {
    const session = await loginRequest(email, password);
    localStorage.setItem(TOKEN_KEY, session.token);
    setToken(session.token);
    setUser(session.user);
    setRole(session.role);
    setAuthError('');
    return session;
  }

  async function signOut() {
    try {
      if (token) {
        await logoutRequest();
      }
    } finally {
      localStorage.removeItem(TOKEN_KEY);
      setToken(null);
      setUser(null);
      setRole(null);
    }
  }

  const value = useMemo(
    () => ({
      token,
      user,
      role,
      loading,
      authError,
      isAuthenticated: Boolean(token && user),
      signIn,
      signOut,
    }),
    [token, user, role, loading, authError]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }

  return context;
}
