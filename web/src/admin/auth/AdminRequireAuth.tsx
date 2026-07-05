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
    return <Navigate replace state={{ from: location.pathname }} to="/login" />;
  }

  return children;
}
