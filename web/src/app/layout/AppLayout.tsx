import type { ReactNode } from 'react';

type AppLayoutProps = {
  header: ReactNode;
  sidebar?: ReactNode;
  children: ReactNode;
};

export function AppLayout({ header, sidebar, children }: AppLayoutProps) {
  // 共用版型只負責排列區塊，不應承擔頁面行為或業務規則。
  return (
    <div className="app-layout">
      {header}
      <div className="app-layout__main app-layout__body">
        {sidebar ? <aside className="app-layout__sidebar">{sidebar}</aside> : null}
        <main className="app-layout__content">
          <div className="app-layout__content-inner">{children}</div>
        </main>
      </div>
    </div>
  );
}
