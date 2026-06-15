/**
 * Pure helpers for exchange data normalization and REST/WebSocket transforms.
 */

import {
  type ApiResponse,
  type KlineRow,
  type MarketTickerState,
  type DecimalLike,
  type RawKline,
  type RawTicker,
  type RawTrade,
  type TradeRow
} from '../types/exchange';

/**
 * 開發時可在 HTML 上注入 `window.__EXCHANGE_API_BASE__`，
 * 讓前端從自定義後端位址取資料，不一定要綁 localhost:8080。
 */
declare global {
  interface Window {
    __EXCHANGE_API_BASE__?: string;
  }
}

/**
 * 將前端相對路徑組合成可直接呼叫的 API/WS URL。
 * 預設仍使用目前頁面的主機位址；開發模式可改用注入變數。
 */
function resolveApiBase(): string {
  const injected = window.__EXCHANGE_API_BASE__?.trim();
  if (injected) {
    return injected.replace(/\/+$/, '');
  }

  return `${window.location.protocol}//${window.location.host}`;
}

/**
 * Convert backend numeric-like fields (string/number/null) into safe floating values.
 */
export function toNumber(value: DecimalLike): number | null {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null;
  }
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

/**
 * Render timestamp as HH:mm:ss string for trade tables.
 */
export function toIsoFromTimestamp(ts: unknown): string {
  if (typeof ts !== 'string') {
    return '--';
  }
  const time = new Date(ts);
  if (Number.isNaN(time.getTime())) {
    return '--';
  }
  return time.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

/**
 * Normalize millisecond timestamp into minute bucket key used by simple candle aggregation.
 */
export function toMinuteKey(ts: unknown): string | null {
  if (typeof ts !== 'string') {
    return null;
  }
  const parsed = Date.parse(ts);
  if (Number.isNaN(parsed)) {
    return null;
  }
  return new Date(Math.floor(parsed / 60000) * 60000).toISOString();
}

export function buildWsUrl(): string {
  const base = new URL(resolveApiBase());
  const protocol = base.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${base.host}/ws/exchange`;
}

export function wsCommandSubscribe(symbol: string): Record<string, unknown> {
  return {
    type: 'subscribe.market',
    symbol: symbol.trim().toUpperCase(),
    cancelOnDisconnect: false
  };
}

export function formatDecimal(value: number | null, minDigits: number, maxDigits: number): string {
  if (value === null) {
    return '--';
  }
  return value.toLocaleString('en-US', {
    minimumFractionDigits: minDigits,
    maximumFractionDigits: maxDigits
  });
}

export function formatPrice(value: number | null): string {
  if (value === null) {
    return '--';
  }
  if (value >= 10000) return formatDecimal(value, 2, 2);
  if (value >= 1) return formatDecimal(value, 2, 4);
  return formatDecimal(value, 4, 8);
}

export function formatQty(value: number | null): string {
  if (value === null) {
    return '--';
  }
  return formatDecimal(value, 2, 6);
}

export function formatNotional(value: number | null): string {
  if (value === null) {
    return '--';
  }
  return formatDecimal(value, 2, 2);
}

/**
 * API responses in this project sometimes wrap with { ok,data }.
 * This keeps both wrapped and raw payloads usable.
 */
export function parseApiResponse<T>(payload: unknown): T | null {
  if (payload === null || typeof payload !== 'object' || Array.isArray(payload)) {
    return null;
  }
  const wrapped = payload as ApiResponse<T>;
  if (typeof wrapped.ok === 'boolean') {
    return wrapped.ok ? wrapped.data : null;
  }
  return payload as T;
}

export async function fetchJson<T>(url: string): Promise<T | null> {
  try {
    const requestUrl = /^https?:\/\//.test(url) ? url : `${resolveApiBase()}${url}`;
    const response = await fetch(requestUrl, { cache: 'no-store' });
    if (!response.ok) {
      return null;
    }
    const payload = await response.json();
    return parseApiResponse<T>(payload);
  } catch {
    return null;
  }
}

export function normalizeTicker(raw: RawTicker | null): MarketTickerState | null {
  if (!raw || !raw.symbol) {
    return null;
  }
  return {
    symbol: raw.symbol,
    lastPrice: toNumber(raw.lastPrice),
    bestBid: toNumber(raw.bestBid),
    bestAsk: toNumber(raw.bestAsk),
    volume24h: toNumber(raw.volume24h),
    high24h: toNumber(raw.high24h),
    low24h: toNumber(raw.low24h),
    updatedAt: raw.updatedAt ? raw.updatedAt : null
  };
}

export function normalizeTrade(raw: RawTrade): TradeRow | null {
  const price = toNumber(raw.price);
  const qty = toNumber(raw.qty);
  if (price === null || qty === null) {
    return null;
  }

  return {
    matchId: raw.matchId,
    side: raw.side === 'SELL' ? 'SELL' : 'BUY',
    time: toIsoFromTimestamp(raw.ts),
    price: formatPrice(price),
    qty: formatQty(qty),
    notional: formatNotional(price * qty),
    tsKey: Date.parse(raw.ts) || 0
  };
}

export function normalizeKline(raw: RawKline): KlineRow | null {
  const open = toNumber(raw.open);
  const high = toNumber(raw.high);
  const low = toNumber(raw.low);
  const close = toNumber(raw.close);
  const volume = toNumber(raw.volume);
  if (open === null || high === null || low === null || close === null || volume === null || !raw.openTime) {
    return null;
  }
  return {
    openTime: raw.openTime,
    open,
    high,
    low,
    close,
    volume
  };
}

export function upsertKlineWithTrade(prev: readonly KlineRow[], trade: RawTrade, maxRows: number): KlineRow[] {
  const minute = toMinuteKey(trade.ts);
  const price = toNumber(trade.price);
  const qty = toNumber(trade.qty);
  if (minute === null || price === null || qty === null) {
    return [...prev];
  }

  if (prev.length === 0) {
    return [
      {
        openTime: minute,
        open: price,
        high: price,
        low: price,
        close: price,
        volume: qty
      }
    ];
  }

  const next = [...prev];
  const index = next.findIndex((item) => item.openTime === minute);
  const minuteTime = Date.parse(minute);
  const firstTime = Date.parse(next[0].openTime);
  const lastTime = Date.parse(next[next.length - 1].openTime);

  if (index >= 0) {
    const source = next[index];
    next[index] = {
      ...source,
      high: Math.max(source.high, price),
      low: Math.min(source.low, price),
      close: price,
      volume: source.volume + qty
    };
    return next;
  }

  if (minuteTime > lastTime) {
    next.push({
      openTime: minute,
      open: price,
      high: price,
      low: price,
      close: price,
      volume: qty
    });
    return next.slice(-maxRows);
  }

  if (minuteTime < firstTime) {
    return next;
  }

  let insertAt = 0;
  while (insertAt < next.length && Date.parse(next[insertAt].openTime) < minuteTime) {
    insertAt += 1;
  }
  next.splice(insertAt, 0, {
    openTime: minute,
    open: price,
    high: price,
    low: price,
    close: price,
    volume: qty
  });
  return next.slice(-maxRows);
}
