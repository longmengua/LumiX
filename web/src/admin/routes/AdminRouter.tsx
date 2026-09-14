import { Navigate, Route, Routes } from 'react-router-dom';

import { AdminRequireAuth } from '../auth/AdminRequireAuth';
import { AdminLayout } from '../layout/AdminLayout';
import { AdminForgotPasswordPage } from '../pages/AdminForgotPasswordPage';
import { AdminLoginPage } from '../pages/AdminLoginPage';
import { AdminAccountPage } from '../pages/AdminAccountPage';
import { AdminResetPasswordPage } from '../pages/AdminResetPasswordPage';
import { AdminConsole } from '../features/console/AdminConsole';
import { AdminUsersPage } from '../features/users/AdminUsersPage';

export function AdminRouter() {
  // 後台子路由全部由 AdminConsole 承接，方便未來把每個區塊逐步拆成真實 API 頁面。
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
              <Routes>
                <Route path="account/*" element={<AdminAccountPage />} />
                <Route index element={<AdminConsole />} />
                <Route path="users" element={<AdminUsersPage />} />
                <Route path="assets" element={<AdminConsole />} />
                <Route path="wallet" element={<AdminConsole />} />
                <Route path="spot" element={<AdminConsole />} />
                <Route path="futures" element={<AdminConsole />} />
                <Route path="margin" element={<AdminConsole />} />
                <Route path="risk" element={<AdminConsole />} />
                <Route path="market-makers" element={<AdminConsole />} />
                <Route path="insurance-fund" element={<AdminConsole />} />
                <Route path="reconciliation" element={<AdminConsole />} />
                <Route path="operation-logs" element={<AdminConsole />} />
                <Route path="settings" element={<AdminConsole />} />
                <Route path="*" element={<Navigate replace to="/" />} />
              </Routes>
            </AdminLayout>
          </AdminRequireAuth>
        }
      />
    </Routes>
  );
}
