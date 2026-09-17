import { Route, Routes, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';

import { AdminRequireAuth } from '../auth/AdminRequireAuth';
import { AdminLayout } from '../layout/AdminLayout';
import { AdminForgotPasswordPage } from '../pages/AdminForgotPasswordPage';
import { AdminLoginPage } from '../pages/AdminLoginPage';
import { AdminAccountPage } from '../pages/AdminAccountPage';
import { AdminResetPasswordPage } from '../pages/AdminResetPasswordPage';
import { AdminConsole } from '../features/console/AdminConsole';
import { AdminErrorBoundary } from '../components/AdminErrorBoundary';
import { AdminStatusPage } from '../pages/AdminStatusPages';
import { AdminAssetsPage } from '../features/assets/AdminAssetsPage';
import {
  AdminActivitiesWorkspacePage,
  AdminFuturesWorkspacePage,
  AdminMarketMakersWorkspacePage,
  AdminOperationLogsWorkspacePage,
  AdminRiskWorkspacePage,
  AdminSettingsWorkspacePage,
  AdminSpotWorkspacePage,
  AdminUsersWorkspacePage,
  AdminWalletWorkspacePage,
} from '../pages/AdminNavWorkspaces';

export function AdminRouter() {
  // 儀表板保留既有入口；其餘頂部模組各自承接可分享的左側工作台子路由。
  return (
    <Routes>
      <Route path="login" element={<AdminLoginPage />} />
      <Route path="forgot-password" element={<AdminForgotPasswordPage />} />
      <Route path="reset-password" element={<AdminResetPasswordPage />} />
      <Route
        path="*"
        element={
          <AdminRequireAuth>
            <AdminLayout>
              <AdminRouteBoundary>
              <Routes>
                <Route path="account/*" element={<AdminAccountPage />} />
                <Route index element={<AdminConsole />} />
                <Route path="users/*" element={<AdminUsersWorkspacePage />} />
                <Route path="assets/*" element={<AdminAssetsPage />} />
                <Route path="activities/*" element={<AdminActivitiesWorkspacePage />} />
                <Route path="wallet/*" element={<AdminWalletWorkspacePage />} />
                <Route path="spot/*" element={<AdminSpotWorkspacePage />} />
                <Route path="futures/*" element={<AdminFuturesWorkspacePage />} />
                <Route path="risk/*" element={<AdminRiskWorkspacePage />} />
                <Route path="market-makers/*" element={<AdminMarketMakersWorkspacePage />} />
                <Route path="insurance-fund" element={<AdminConsole />} />
                <Route path="reconciliation" element={<AdminConsole />} />
                <Route path="operation-logs/*" element={<AdminOperationLogsWorkspacePage />} />
                <Route path="settings/*" element={<AdminSettingsWorkspacePage />} />
                <Route path="*" element={<AdminStatusPage status="not-found" />} />
              </Routes>
              </AdminRouteBoundary>
            </AdminLayout>
          </AdminRequireAuth>
        }
      />
    </Routes>
  );
}

function AdminRouteBoundary({ children }: { children: ReactNode }) {
  const location = useLocation();
  return <AdminErrorBoundary resetKey={location.pathname} fallback={<AdminStatusPage status="server" />}>{children}</AdminErrorBoundary>;
}
