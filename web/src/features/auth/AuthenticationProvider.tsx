import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';

import * as authApi from './authApi';
import type { AuthenticatedUser } from './authApi';

type AuthenticationContextValue = {
  user: AuthenticatedUser | null;
  loading: boolean;
  sessionResolved: boolean;
  signIn(input: { email: string; password: string }): Promise<AuthenticatedUser | null>;
  completeLoginVerification(): Promise<AuthenticatedUser | null>;
  register(input: { email: string; displayName: string; password: string }): Promise<AuthenticatedUser>;
  signOut(): Promise<void>;
  changePassword(input: { currentPassword: string; newPassword: string }): Promise<AuthenticatedUser>;
  updateDisplayName(displayName: string): Promise<AuthenticatedUser>;
};

const AuthenticationContext = createContext<AuthenticationContextValue | null>(null);

/**
 * 集中管理已登入使用者投影，而不接觸 HttpOnly Cookie 內容。
 *
 * HttpOnly Cookie 不可由 JavaScript 讀取，因此 app 初始時必須以 `/me` 安全還原登入狀態。
 */
export function AuthenticationProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [sessionResolved, setSessionResolved] = useState(false);
  const sessionOperation = useRef(0);
  const initialRestoreStarted = useRef(false);

  const restoreSession = useCallback(async (): Promise<AuthenticatedUser | null> => {
    const operation = ++sessionOperation.current;
    setLoading(true);
    try {
      const current = await authApi.currentUser();
      if (sessionOperation.current === operation) {
        setUser(current);
      }
      return current;
    } catch {
      // session 過期是正常狀態；不可保留舊的前端使用者投影。
      if (sessionOperation.current === operation) {
        setUser(null);
      }
      return null;
    } finally {
      if (sessionOperation.current === operation) {
        setLoading(false);
        setSessionResolved(true);
      }
    }
  }, []);

  useEffect(() => {
    // 唯一真實來源是 server-side session；不要用 local/session storage 猜測登入狀態。
    // Strict Mode 會在開發期重跑 effect，這裡避免同一輪掛載發出重複的 /me 請求。
    if (initialRestoreStarted.current) {
      return;
    }
    initialRestoreStarted.current = true;
    void restoreSession();
  }, [restoreSession]);

  const value = useMemo<AuthenticationContextValue>(() => ({
    user,
    loading,
    sessionResolved,
    async signIn(input) {
      const result = await authApi.signIn(input);
      if (result.kind === 'verification-required') {
        // 帳密通過不是登入完成；等待 email 時不可提前投影出 user 或跳轉私有 route。
        return null;
      }
      const authenticatedUser = result.user;
      // 使 app 初始的匿名 /me 回應失效，避免它覆寫剛登入的身份。
      sessionOperation.current += 1;
      setUser(authenticatedUser);
      setLoading(false);
      setSessionResolved(true);
      return authenticatedUser;
    },
    async completeLoginVerification() {
      const authenticatedUser = await authApi.completeLoginVerification();
      if (authenticatedUser === null) return null;
      sessionOperation.current += 1;
      setUser(authenticatedUser);
      setLoading(false);
      setSessionResolved(true);
      return authenticatedUser;
    },
    async register(input) {
      const authenticatedUser = await authApi.register(input);
      // 註冊成功已核發 session，不能再讓較早的匿名查詢把它清空。
      sessionOperation.current += 1;
      setUser(authenticatedUser);
      setLoading(false);
      setSessionResolved(true);
      return authenticatedUser;
    },
    async signOut() {
      try {
        await authApi.signOut();
      } finally {
        // 即使網路中斷，也不可讓 UI 繼續把舊身份當成已登入狀態。
        sessionOperation.current += 1;
        setUser(null);
        setLoading(false);
        setSessionResolved(true);
      }
    },
    async changePassword(input) {
      const authenticatedUser = await authApi.changePassword(input);
      setUser(authenticatedUser);
      return authenticatedUser;
    },
    async updateDisplayName(displayName) {
      const authenticatedUser = await authApi.updateDisplayName(displayName);
      // 顯示名稱同時出現在 header；成功後立即同步，避免頁面內與導覽列呈現不同身份投影。
      setUser(authenticatedUser);
      return authenticatedUser;
    },
  }), [loading, sessionResolved, user]);

  return <AuthenticationContext.Provider value={value}>{children}</AuthenticationContext.Provider>;
}

export function useAuthentication() {
  const context = useContext(AuthenticationContext);
  if (context === null) {
    throw new Error('useAuthentication must be used within AuthenticationProvider');
  }
  return context;
}
