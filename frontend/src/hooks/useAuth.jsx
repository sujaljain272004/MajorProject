import { createContext, useContext, useEffect, useState } from "react";

const AuthContext = createContext(null);

const TOKEN_KEY = "chargeup_token";
const USER_KEY = "chargeup_user";

function safeGet(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function safeSet(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Storage can be unavailable in some mobile/private browsing contexts.
  }
}

function safeRemove(key) {
  try {
    localStorage.removeItem(key);
  } catch {
    // Ignore storage failures and keep in-memory auth state working.
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => safeGet(TOKEN_KEY));
  const [user, setUser] = useState(() => {
    const stored = safeGet(USER_KEY);
    if (!stored) {
      return null;
    }

    try {
      return JSON.parse(stored);
    } catch {
      return null;
    }
  });

  useEffect(() => {
    if (token) {
      safeSet(TOKEN_KEY, token);
    } else {
      safeRemove(TOKEN_KEY);
    }
  }, [token]);

  useEffect(() => {
    if (user) {
      safeSet(USER_KEY, JSON.stringify(user));
    } else {
      safeRemove(USER_KEY);
    }
  }, [user]);

  const value = {
    token,
    user,
    isAuthenticated: Boolean(token),
    loginSession: (authResponse) => {
      safeSet(TOKEN_KEY, authResponse.token);
      safeSet(USER_KEY, JSON.stringify(authResponse.user));
      setToken(authResponse.token);
      setUser(authResponse.user);
    },
    logout: () => {
      safeRemove(TOKEN_KEY);
      safeRemove(USER_KEY);
      setToken(null);
      setUser(null);
    }
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
