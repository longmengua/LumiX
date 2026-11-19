import { useEffect, useState } from 'react';

import { Badge } from '../../components/base/Badge';
import { useI18n } from '../../i18n';
import { readCandles, SpotCandleChart, type MarketCandle } from './SpotCandleChart';

import { TradingWorkspaceLayout } from './TradingWorkspaceLayout';
import { SpotReferenceOrderBook, type ReferenceLevel as Level, type ReferenceBook as Book } from './SpotReferenceOrderBook';
import { SpotUnavailableOrderPanel, SpotUnavailableAccountPanel } from './SpotTradingUnavailablePanels';
import { formatMarketDecimal, isMarketDecimal } from './marketDecimal';
type State = { status: 'connecting' | 'unavailable' | 'healthy'; book: Book | null };

/** Binance 的參考行情與未來 LumiX 自身的成交及委託簿必須分開呈現。 */
export function SpotReferenceMarket({ symbol }: { symbol: string }) {
  const { locale } = useI18n();
  const normalized = symbol.toUpperCase().replace('-', '');
  const supported = normalized === 'BTCUSDT' || normalized === 'ETHUSDT';
  const [state, setState] = useState<State>({ status: 'connecting', book: null });
  const [candles, setCandles] = useState<MarketCandle[] | null>(null);
  const english = locale === 'en-US';

  useEffect(() => {
    if (!supported || window.location.protocol !== 'https:') {
      setState({ status: 'unavailable', book: null });
      setCandles(null);
      return;
    }
    let stopped = false;
    let retry: number | undefined;
    let watchdog: number | undefined;
    let socket: WebSocket | undefined;
    let lastMessage = 0;

    function connect() {
      if (stopped) return;
      setState({ status: 'connecting', book: null });
      setCandles(null);
      lastMessage = Date.now();
      // 公開參考行情只連同源 WSS；不將任何帳戶憑證放入 URL。
      socket = new WebSocket(`wss://${window.location.host}/api/v1/market-data/stream`);
      socket.onmessage = (event) => {
        try {
          const payload: unknown = JSON.parse(String(event.data));
          if (!payload || typeof payload !== 'object') throw new Error('invalid feed');
          if ((payload as Record<string, unknown>).source !== 'BINANCE_SPOT') throw new Error('invalid source');
          const item = (payload as Record<string, unknown>)[normalized];
          if (!item || typeof item !== 'object') throw new Error('missing symbol');
          const value = item as Record<string, unknown>;
          const validLevels = (levels: unknown): levels is Level[] => Array.isArray(levels)
            && levels.length <= 20 && levels.every((entry: unknown) => !!entry && typeof entry === 'object'
              && isMarketDecimal((entry as Level).price) && isMarketDecimal((entry as Level).quantity));
          if (typeof value.healthy !== 'boolean' || !validLevels(value.bids) || !validLevels(value.asks)) throw new Error('invalid book');
          lastMessage = Date.now();
          setState(value.healthy ? { status: 'healthy', book: { healthy: true, bids: value.bids, asks: value.asks } }
            : { status: 'unavailable', book: null });
          const candleGroup = (payload as Record<string, unknown>).candles;
          setCandles(candleGroup && typeof candleGroup === 'object' ? readCandles((candleGroup as Record<string, unknown>)[normalized]) : null);
        } catch {
          // 訊息格式錯誤時移除舊價位，避免使用者誤認其仍為即時行情。
          setState({ status: 'unavailable', book: null });
          setCandles(null);
          socket?.close();
        }
      };
      socket.onerror = () => { setState({ status: 'unavailable', book: null }); setCandles(null); };
      socket.onclose = () => {
        if (!stopped) {
          setState({ status: 'unavailable', book: null });
          setCandles(null);
          retry = window.setTimeout(connect, 3000);
        }
      };
    }
    connect();
    watchdog = window.setInterval(() => {
      if (lastMessage > 0 && Date.now() - lastMessage > 5000) {
        setState({ status: 'unavailable', book: null });
        setCandles(null);
        socket?.close();
        lastMessage = 0;
      }
    }, 1000);
    return () => {
      stopped = true;
      if (retry !== undefined) window.clearTimeout(retry);
      if (watchdog !== undefined) window.clearInterval(watchdog);
      socket?.close();
    };
  }, [normalized, supported]);

  const baseAsset = normalized.endsWith('USDT') ? normalized.slice(0, -4) : normalized;
  const last = candles ? candles[candles.length - 1] : undefined;
  const unavailable = !supported
    ? (english ? 'This market is not supported yet.' : '此交易對目前尚未提供。')
    : (english ? 'Reference market unavailable. Reconnecting…' : '參考行情目前無法取得，正在重新連線…');
  const status = state.status === 'healthy' ? (english ? 'Live' : '即時')
    : state.status === 'connecting' && supported && window.location.protocol === 'https:' ? (english ? 'Connecting…' : '行情連線中…') : unavailable;
  // 摘要只呈現本次真實資料；一分鐘高低價及成交量不可冒充 24 小時統計。
  const metrics = [
    {label: english ? 'Best bid' : '最佳買價', value: state.book?.bids[0]?.price},
    {label: english ? 'Best ask' : '最佳賣價', value: state.book?.asks[0]?.price},
    {label: english ? '1m high' : '1 分最高', value: last?.high},
    {label: english ? '1m low' : '1 分最低', value: last?.low},
    {label: english ? `1m volume (${baseAsset})` : `1 分成交量（${baseAsset}）`, value: last?.volume},
    {label: english ? '1m open' : '1 分開盤', value: last?.open},
  ];
  const header = <header className="market-header">
    <div className="market-header__identity"><span className="market-header__brand">LumiX</span><div>
      <div className="market-header__instrument-row"><h1>{baseAsset}/USDT</h1><Badge tone="warning">{english ? 'Reference market' : '參考行情'}</Badge></div>
      <p>Binance · {english ? 'Spot' : '現貨'}</p>
    </div></div>
    <div className="market-header__price"><span>{english ? 'Latest reference price' : '最新參考價格'}</span><strong>{last ? formatMarketDecimal(last.close) : '—'}</strong><em role="status" className={state.status === 'healthy' ? 'pnl--positive' : ''}>{status}</em></div>
    <div className="market-header__metrics">
      {metrics.map(item => <div className="market-header__metric" key={item.label}><span>{item.label}</span><strong>{item.value ? formatMarketDecimal(item.value) : '—'}</strong></div>)}
      <div className="market-header__metric market-header__metric--compact"><span>{english ? 'Candle time (UTC)' : 'K 線時間（UTC）'}</span><strong>{last ? new Date(last.openTime).toISOString().slice(11, 16) : '—'}</strong></div>
    </div>
  </header>;
  return (
    <TradingWorkspaceLayout
      className="spot-reference"
      header={header}
      notice={english ? 'Source: Binance Spot. External reference data; LumiX spot order execution is not available yet.' : '來源：Binance 現貨。此為外部參考行情；LumiX 現貨下單尚未開放。'}
      chart={<SpotCandleChart candles={candles} english={english} />}
      book={<SpotReferenceOrderBook book={state.book} lastPrice={last?.close ?? null} baseAsset={baseAsset} english={english} status={status} />}
      orderPanel={<SpotUnavailableOrderPanel baseAsset={baseAsset} english={english} />}
      accountPanel={<SpotUnavailableAccountPanel english={english} />}
    />
  );
}
