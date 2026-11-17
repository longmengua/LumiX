import { useEffect, useState } from 'react';

import { useI18n } from '../../i18n';
import { readCandles, SpotCandleChart, type MarketCandle } from './SpotCandleChart';

type Level = { price: string; quantity: string };
type Book = { healthy: boolean; bids: Level[]; asks: Level[] };
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
      // 只能使用同源 WSS；登入 cookie 留給瀏覽器處理，不放入 URL。
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
              && typeof (entry as Level).price === 'string' && typeof (entry as Level).quantity === 'string');
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

  const title = english ? 'Spot reference market' : '現貨參考行情';
  const unavailable = !supported
    ? (english ? 'This market is not supported yet.' : '此交易對目前尚未提供。')
    : (english ? 'Reference order book unavailable. Sign in and use the secure site, then retry.' : '參考委託簿目前無法取得，請登入並使用安全連線後重試。');
  return (
    <main className="trading-page trading-page--workspace spot-reference">
      <section className="workspace-panel">
        <h1>{title} · {symbol}</h1>
        <p>{english ? 'Source: Binance Spot. This is a reference book, not LumiX orders or trades.' : '來源：Binance 現貨。此為外部參考委託簿，不是 LumiX 的訂單或成交。'}</p>
        <p>{english ? 'LumiX spot order execution is not available yet.' : 'LumiX 現貨下單與成交功能尚未開放。'}</p>
        <p role="status">{state.status === 'healthy' ? (english ? 'Live' : '即時') : state.status === 'connecting' && supported && window.location.protocol === 'https:' ? (english ? 'Connecting…' : '行情連線中…') : unavailable}</p>
        {state.book && (
          <div className="trading-book__sides">
            {([['asks', english ? 'Asks' : '賣盤'], ['bids', english ? 'Bids' : '買盤']] as const).map(([side, label]) => (
              <section className="trading-book__side" key={side}>
                <h2>{label}</h2>
                <div className="trading-book__table-head"><span>{english ? 'Price (USDT)' : '價格（USDT）'}</span><span>{english ? 'Quantity' : '數量'}</span></div>
                {state.book?.[side].map((level) => <div className="trading-book__row" key={level.price}><span>{level.price}</span><span>{level.quantity}</span></div>)}
              </section>
            ))}
          </div>
        )}
      </section>
      {candles ? <SpotCandleChart candles={candles} english={english} /> : <section className="workspace-panel spot-candles"><p role="status">{english ? 'Reference K-line unavailable or synchronizing.' : '參考 K 線目前無法取得或正在同步。'}</p></section>}
    </main>
  );
}
