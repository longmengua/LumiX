import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import { fetchAdminSession, type AdminSession } from '../api/adminSessionApi';
import { signOutAdmin } from '../api/adminAuthenticationApi';
import { ADMIN_UNAUTHORIZED_EVENT } from '../api/adminError';

type AdminAuthContextValue = {
  session: AdminSession | null;
  isAuthenticated: boolean;
  loading: boolean;
  signOut: () => Promise<void>;
};

const AdminAuthContext = createContext<AdminAuthContextValue | null>(null);

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  // 每次掛載都由 HttpOnly session 向 server 重新確認，避免沿用可被竄改的瀏覽器儲存資料。
  const [session, setSession] = useState<AdminSession | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void fetchAdminSession()
      .then((nextSession) => {
        if (active) setSession(nextSession);
      })
      .catch(() => {
        // 未登入、尚未啟用或權限不足都不應在 browser 區分，以免洩漏管理員狀態。
        if (active) setSession(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    // 任一 admin API 收到 401 時只撤銷本地投影，RequireAuth 會保留 return path 導回專用登入頁。
    const invalidate = () => setSession(null);
    window.addEventListener(ADMIN_UNAUTHORIZED_EVENT, invalidate);
    return () => window.removeEventListener(ADMIN_UNAUTHORIZED_EVENT, invalidate);
  }, []);

  const value = useMemo<AdminAuthContextValue>(
    () => ({
      session,
      isAuthenticated: session !== null,
      loading,
      signOut: async () => {
        // 管理端只能呼叫自己的登出 endpoint，避免被 Nginx 隔離的前台 API 造成假登出。
        await signOutAdmin().catch(() => undefined);
        setSession(null);
      },
    }),
    [loading, session],
  );

  return <AdminAuthContext.Provider value={value}>{children}</AdminAuthContext.Provider>;
}

export function useAdminAuth() {
  const context = useContext(AdminAuthContext);
  if (!context) {
    throw new Error('useAdminAuth must be used within AdminAuthProvider');
  }
  return context;
}
