import { useParams } from 'react-router-dom';

import { TradingWorkspace } from '../features/trading/TradingWorkspace';
import type { TradingKind } from '../features/trading/mockTradingService';

type TradingPageProps = {
  kind: TradingKind;
};

const defaultSymbols: Record<TradingKind, string> = {
  spot: 'BTC-USDT',
  futures: 'BTCUSDT-PERP',
  margin: 'BTC-USDT',
};

export function TradingPage({ kind }: TradingPageProps) {
  const params = useParams<{ symbol: string }>();
  const symbol = params.symbol ?? defaultSymbols[kind];

  return <TradingWorkspace kind={kind} symbol={symbol} />;
}

export function SpotTradingPage() {
  return <TradingPage kind="spot" />;
}

export function FuturesTradingPage() {
  return <TradingPage kind="futures" />;
}

export function MarginTradingPage() {
  return <TradingPage kind="margin" />;
}

