import { useState } from 'react';

import { ErrorState, LoadingState } from '../../components/base/State';
import { useI18n } from '../../i18n';
import { TradingWorkspaceLayout } from './TradingWorkspaceLayout';
import { TradingBalancePanel } from './TradingBalancePanel';
import { FuturesMarketChart } from './FuturesMarketChart';
import { TradingMarketHeader } from './TradingMarketHeader';
import { TradingOrderBook } from './TradingOrderBook';
import { TradingOrderForm } from './TradingOrderForm';
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

  // 合約與現貨只共用呈現骨架；此呼叫端的既有資料與互動保持原樣。
  return (
    <TradingWorkspaceLayout
      header={<TradingMarketHeader workspace={data} />}
      notice={t('trading.market.adapterNotice')}
      chart={<FuturesMarketChart symbol={data.symbol} lastPrice={data.midPrice} />}
      book={<TradingOrderBook
        asks={data.orderBook.asks}
        baseAsset={data.baseAsset}
        bids={data.orderBook.bids}
        lastPrice={data.midPrice}
        markPrice={data.marketStats.markPrice}
        trades={data.trades}
        onSelectPrice={setSelectedBookPrice}
      />}
      orderPanel={<TradingOrderForm kind={kind} workspace={data} selectedBookPrice={selectedBookPrice} />}
      accountPanel={<TradingBalancePanel balances={data.balances} positions={data.positions} openOrders={data.openOrders} riskRatio={data.riskRatio} />}
    />
  );
}
