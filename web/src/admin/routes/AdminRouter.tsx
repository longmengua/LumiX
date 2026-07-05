import { Navigate, Route, Routes } from 'react-router-dom';

import { AdminRequireAuth } from '../auth/AdminRequireAuth';
import { AdminLayout } from '../layout/AdminLayout';
import { AdminLoginPage } from '../pages/AdminLoginPage';
import { AdminConsole } from '../features/console/AdminConsole';

export function AdminRouter() {
  return (
    <Routes>
      <Route path="login" element={<AdminLoginPage />} />
      <Route
        path="*"
        element={
          <AdminRequireAuth>
            <AdminLayout>
              <Routes>
                <Route index element={<AdminConsole />} />
                <Route path="users" element={<AdminConsole />} />
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
