import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import { clearAdminSession, readAdminSession, saveAdminSession, signInAdminMock, type AdminLoginInput, type AdminSession } from './mockAdminAuthService';

type AdminAuthContextValue = {
  session: AdminSession | null;
  isAuthenticated: boolean;
  signIn: (input: AdminLoginInput) => Promise<AdminSession>;
  signOut: () => void;
};

const AdminAuthContext = createContext<AdminAuthContextValue | null>(null);

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  // 這層只保存前端 session 快照，不能當成安全來源；真正的後台授權仍要由 server 驗證。
  const [session, setSession] = useState<AdminSession | null>(() => readAdminSession());

  useEffect(() => {
    if (session) {
      saveAdminSession(session);
    } else {
      clearAdminSession();
    }
  }, [session]);

  const value = useMemo<AdminAuthContextValue>(
    () => ({
      session,
      isAuthenticated: session !== null,
      signIn: async (input: AdminLoginInput) => {
        const nextSession = await signInAdminMock(input);
        setSession(nextSession);
        return nextSession;
      },
      signOut: () => {
        clearAdminSession();
        setSession(null);
      },
    }),
    [session],
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
