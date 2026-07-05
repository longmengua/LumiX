import type { ReactNode } from 'react';

import { AdminHeader } from './AdminHeader';
import { AdminTopNav } from './AdminTopNav';

type AdminLayoutProps = {
  children: ReactNode;
};

export function AdminLayout({ children }: AdminLayoutProps) {
  return (
    <div className="admin-app">
      <div className="admin-layout">
        <AdminHeader />
        <AdminTopNav />
        <main className="admin-main">
          <div className="admin-console">{children}</div>
        </main>
      </div>
    </div>
  );
}
