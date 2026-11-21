import { useParams } from 'react-router-dom';

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

  // 兩種交易頁使用同一個真實帳戶與委託面板；圖表明示現貨參考來源。
  return <SpotReferenceMarket key={`${kind}:${symbol}`} kind={kind} symbol={symbol} />;
}

export function SpotTradingPage() {
  return <TradingPage kind="spot" />;
}

export function FuturesTradingPage() {
  return <TradingPage kind="futures" />;
}

