import { useParams } from 'react-router-dom';

import { TradingWorkspace } from '../features/trading/TradingWorkspace';
import { SpotReferenceMarket } from '../features/trading/SpotReferenceMarket';
import type { TradingKind } from '../features/trading/mockTradingService';

type TradingPageProps = {
  kind: TradingKind;
};

const defaultSymbols: Record<TradingKind, string> = {
  spot: 'BTC-USDT',
  futures: 'BTCUSDT',
};

export function TradingPage({ kind }: TradingPageProps) {
  const params = useParams<{ symbol: string }>();
  const symbol = params.symbol ?? defaultSymbols[kind];

  // 現貨只顯示真實來源的參考行情，避免開發期資料被誤認為正式委託簿。
  return kind === 'spot' ? <SpotReferenceMarket key={symbol} symbol={symbol} /> : <TradingWorkspace kind={kind} symbol={symbol} />;
}

export function SpotTradingPage() {
  return <TradingPage kind="spot" />;
}

export function FuturesTradingPage() {
  return <TradingPage kind="futures" />;
}

