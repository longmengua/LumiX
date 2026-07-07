import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';

import { useAdminAuth } from './AdminAuthProvider';

type AdminRequireAuthProps = {
  children: ReactNode;
};

export function AdminRequireAuth({ children }: AdminRequireAuthProps) {
  const { isAuthenticated } = useAdminAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    // 未登入就導回登入頁；這裡只做前端路由保護，不能取代真正的後端授權檢查。
    return <Navigate replace state={{ from: location.pathname }} to="/login" />;
  }

  return children;
}
