import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';

import { useAdminAuth } from './AdminAuthProvider';

type AdminRequireAuthProps = {
  children: ReactNode;
};

export function AdminRequireAuth({ children }: AdminRequireAuthProps) {
  const { isAuthenticated, loading } = useAdminAuth();
  const location = useLocation();

  if (loading) {
    // 必須等 server-side principal 查驗完成，否則初始載入會把有效 HttpOnly session 誤導回登入頁。
    return null;
  }

  if (!isAuthenticated) {
    // BrowserRouter 的 basename 是 /admin，因此這個相對根路徑會落在 /admin/login，不會回到客戶端登入頁。
    // 這裡只做前端路由保護，不能取代真正的後端授權檢查。
    return <Navigate replace state={{ from: `${location.pathname}${location.search}${location.hash}` }} to="/login" />;
  }

  return children;
}
