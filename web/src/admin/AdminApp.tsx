import { AdminAuthProvider } from './auth/AdminAuthProvider';
import { AdminRouter } from './routes/AdminRouter';

export function AdminApp() {
  return (
    <AdminAuthProvider>
      <AdminRouter />
    </AdminAuthProvider>
  );
}
