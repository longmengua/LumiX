import { useState } from 'react';

import { ErrorState, LoadingState } from '../../components/base/State';
import { useI18n } from '../../i18n';
import { TradingBalancePanel } from './TradingBalancePanel';
import { TradingMarketChart } from './TradingMarketChart';
import { TradingMarketHeader } from './TradingMarketHeader';
import { TradingOrderBook } from './TradingOrderBook';
import { TradingOrderForm } from './TradingOrderForm';
import { TradingOpenOrdersPanel } from './TradingOpenOrdersPanel';
import { TradingPositionsPanel } from './TradingPositionsPanel';
import { useTradingWorkspaceMock } from './useTradingWorkspaceMock';
import type { TradingKind } from './mockTradingService';

type TradingWorkspaceProps = {
  kind: TradingKind;
  symbol: string;
};

export function TradingWorkspace({ kind, symbol }: TradingWorkspaceProps) {
  const { t } = useI18n();
  const { data, loading, error, reload } = useTradingWorkspaceMock(kind, symbol);
  const [selectedBookPrice, setSelectedBookPrice] = useState<number | null>(null);

  if (loading) {
    return <LoadingState title={t('trading.loadingTitle')} description={t('trading.loadingDescription')} />;
  }

  if (error || !data) {
    return <ErrorState title={t('trading.errorTitle')} description={error ?? t('trading.errorTitle')} action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>} />;
  }

  return (
    <main className="trading-page trading-page--workspace">
      {/* 所有數據皆為本地 mock；縮小提示面積，但不隱藏尚未接入真實交易系統的事實。 */}
      <TradingMarketHeader workspace={data} />
      <p className="trading-page__adapter-note">{t('trading.market.adapterNotice')}</p>

      <div className="trading-workspace-grid">
        <TradingMarketChart symbol={data.symbol} lastPrice={data.midPrice} />

        <TradingOrderBook
          asks={data.orderBook.asks}
          baseAsset={data.baseAsset}
          bids={data.orderBook.bids}
          lastPrice={data.midPrice}
          markPrice={data.marketStats.markPrice}
          trades={data.trades}
          onSelectPrice={setSelectedBookPrice}
        />

        <aside className="trading-workspace-grid__order-stack">
          <TradingOrderForm kind={kind} workspace={data} selectedBookPrice={selectedBookPrice} />
          <TradingBalancePanel balances={data.balances} positions={data.positions} riskRatio={data.riskRatio} />
        </aside>

        <TradingPositionsPanel positions={data.positions} baseAsset={data.baseAsset} />
        <TradingOpenOrdersPanel orders={data.openOrders} />
      </div>
    </main>
  );
}
