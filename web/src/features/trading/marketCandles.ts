import type { StockChartTimeframe } from './TradingChartControls';
import { isMarketDecimal, marketAtomic } from './marketDecimal';

export type MarketCandle = { openTime: number; open: string; high: string; low: string; close: string; volume: string; closed: boolean };
const durations = {'1m':60000, '5m':300000, '15m':900000, '1h':3600000, '1d':86400000};

/** 來源的月份為 1M；模板使用 1mo 以免分鐘與月份混淆。 */
export function binanceInterval(timeframe: StockChartTimeframe): string { return timeframe === '1mo' ? '1M' : timeframe; }

function aligned(time: number, frame: StockChartTimeframe): boolean {
  const date = new Date(time);
  if (!Number.isFinite(date.getTime())) return false;
  if (frame in durations) return time % durations[frame as keyof typeof durations] === 0;
  const midnight = date.getUTCHours() === 0 && date.getUTCMinutes() === 0 && date.getUTCSeconds() === 0 && date.getUTCMilliseconds() === 0;
  return midnight && (frame === '1w' ? date.getUTCDay() === 1 : date.getUTCDate() === 1);
}
function nextOpen(time: number, frame: StockChartTimeframe): number {
  if (frame in durations) return time + durations[frame as keyof typeof durations];
  if (frame === '1w') return time + 604800000;
  const date = new Date(time);
  return Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + 1, 1);
}

/** 驗證所選週期與 UTC 連續性；月線不能用固定天數，也不能把前一週期的圖留在新選項下。 */
export function readMarketCandles(value: unknown, timeframe: StockChartTimeframe): MarketCandle[] | null {
  if (!value || typeof value !== 'object') return null;
  const feed = value as Record<string, unknown>;
  if (feed.healthy !== true || feed.interval !== binanceInterval(timeframe) || !Array.isArray(feed.candles) || feed.candles.length < 2 || feed.candles.length > 320) return null;
  let previous = -1;
  for (const item of feed.candles) {
    if (!item || typeof item !== 'object') return null;
    const candle = item as Record<string, unknown>;
    if (!Number.isSafeInteger(candle.openTime) || (candle.openTime as number) < 0 || !aligned(candle.openTime as number, timeframe)
      || (previous >= 0 && candle.openTime !== nextOpen(previous, timeframe))
      || !isMarketDecimal(candle.open) || !isMarketDecimal(candle.high) || !isMarketDecimal(candle.low)
      || !isMarketDecimal(candle.close) || !isMarketDecimal(candle.volume) || typeof candle.closed !== 'boolean') return null;
    const open = marketAtomic(candle.open), high = marketAtomic(candle.high), low = marketAtomic(candle.low), close = marketAtomic(candle.close);
    if (open <= 0n || close <= 0n || low <= 0n || high < open || high < close || low > open || low > close) return null;
    previous = candle.openTime as number;
  }
  return feed.candles as MarketCandle[];
}
