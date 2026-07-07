import type { ReactNode } from 'react';

import { AdminHeader } from './AdminHeader';
import { AdminTopNav } from './AdminTopNav';

type AdminLayoutProps = {
  children: ReactNode;
};

export function AdminLayout({ children }: AdminLayoutProps) {
  // 後台版型只負責共用殼層，避免每個管理頁重複處理 header / nav。
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
