import type { ReactNode } from 'react';

type Props = {
  header: ReactNode;
  notice: ReactNode;
  chart: ReactNode;
  book: ReactNode;
  orderPanel: ReactNode;
  accountPanel: ReactNode;
  className?: string;
};

/** 現貨與合約共用同一個工作區骨架；資料、權限及可執行能力由各自的呼叫端提供。 */
export function TradingWorkspaceLayout({ header, notice, chart, book, orderPanel, accountPanel, className = '' }: Props) {
  return (
    <main className={`trading-page trading-page--workspace ${className}`}>
      {header}
      <p className="trading-page__adapter-note">{notice}</p>
      <div className="trading-workspace-grid">
        {chart}
        {book}
        <aside className="trading-workspace-grid__order-stack">{orderPanel}</aside>
        {accountPanel}
      </div>
    </main>
  );
}
