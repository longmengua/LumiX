import { isMarketDecimal, marketAtomic } from './marketDecimal';
import type { ReferenceBook, ReferenceLevel } from './SpotReferenceOrderBook';

export type ExternalQuotePreviewPolicy = {
  version: number; source: 'BINANCE'; platformRate: string; platformFixedUsdt: string;
  assetAdjustments: Record<string, string>; assetFixedAdjustmentsUsdt: Record<string, string>;
};

const scale = 10n ** 18n;

function signedAtomic(value: string): bigint {
  if (typeof value !== 'string' || !/^-?\d{1,18}(?:\.\d{1,18})?$/.test(value)) throw new Error('invalid quote adjustment');
  return value.startsWith('-') ? -marketAtomic(value.slice(1)) : marketAtomic(value);
}

function decimal(value: bigint): string {
  return `${value / scale}.${(value % scale).toString().padStart(18, '0')}`;
}

/**
 * 瀏覽器只顯示未授權交易的價格預覽；真正可成交價須由後端依已確認外部成交重算。
 * 使用固定精度整數，買盤向下、賣盤向上取至 18 位，避免 Number 精度損失。
 */
export function previewExternalBook(book: ReferenceBook, policy: ExternalQuotePreviewPolicy, asset: string): ReferenceBook {
  if (policy.source !== 'BINANCE' || !Number.isSafeInteger(policy.version) || policy.version <= 0
    || !policy.assetAdjustments || !policy.assetFixedAdjustmentsUsdt
    || !isMarketDecimal(policy.platformRate) || !isMarketDecimal(policy.platformFixedUsdt)) throw new Error('invalid preview policy');
  const rate = marketAtomic(policy.platformRate) + signedAtomic(policy.assetAdjustments[asset] ?? '0');
  const fixed = marketAtomic(policy.platformFixedUsdt) + signedAtomic(policy.assetFixedAdjustmentsUsdt[asset] ?? '0');
  if (rate < 0n || rate >= scale || fixed < 0n) throw new Error('invalid effective quote adjustment');
  const reprice = (levels: ReferenceLevel[], ask: boolean): ReferenceLevel[] => levels.map(level => {
    const raw = marketAtomic(level.price) * (ask ? scale + rate : scale - rate);
    // 固定 USDT 是每單位基礎幣的價差；與百分比並列，不按整筆委託金額重複扣除。
    const price = ask ? (raw + scale - 1n) / scale + fixed : raw / scale - fixed;
    if (price <= 0n) throw new Error('invalid quoted price');
    return { price: decimal(price), quantity: level.quantity };
  });
  return { healthy: book.healthy, bids: reprice(book.bids, false), asks: reprice(book.asks, true) };
}
