import { AdminAuthProvider } from './auth/AdminAuthProvider';
import { AdminRouter } from './routes/AdminRouter';

export function AdminApp() {
  // 後台入口先包 auth provider，再進 router；這樣授權狀態只會在同一層級流動。
  return (
    <AdminAuthProvider>
      <AdminRouter />
    </AdminAuthProvider>
  );
}
