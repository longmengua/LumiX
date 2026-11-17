/** 來源的金額維持十進位字串；只有計算畫面座標後才轉成 number。 */
export type MarketCandle = { openTime: number; open: string; high: string; low: string; close: string; volume: string; closed: boolean };

function atomic(value: string): bigint {
  const [whole, fraction = ''] = value.split('.');
  return BigInt(whole + fraction.padEnd(18, '0'));
}

/** 校驗來自內部推播的資料結構，失敗時不顯示舊圖。 */
export function readCandles(value: unknown): MarketCandle[] | null {
  if (!value || typeof value !== 'object') return null;
  const feed = value as Record<string, unknown>;
  if (feed.healthy !== true || feed.interval !== '1m' || !Array.isArray(feed.candles) || feed.candles.length < 2 || feed.candles.length > 120) return null;
  const decimal = (item: unknown) => typeof item === 'string' && /^(?:\d{1,38})(?:\.\d{1,18})?$/.test(item) && item.length <= 64;
  let previous = -1;
  for (const item of feed.candles) {
    if (!item || typeof item !== 'object') return null;
    const candle = item as Record<string, unknown>;
    if (!Number.isSafeInteger(candle.openTime) || (candle.openTime as number) < 0
      || (previous >= 0 && (candle.openTime as number) - previous !== 60000)
      || !decimal(candle.open) || !decimal(candle.high) || !decimal(candle.low) || !decimal(candle.close)
      || !decimal(candle.volume) || typeof candle.closed !== 'boolean') return null;
    const o = atomic(candle.open as string), h = atomic(candle.high as string), l = atomic(candle.low as string), c = atomic(candle.close as string);
    if (o <= 0n || c <= 0n || l <= 0n || h < o || h < c || l > o || l > c) return null;
    previous = candle.openTime as number;
  }
  return feed.candles as MarketCandle[];
}

/** 使用精確整數縮放價格，避免將交易價格轉為 binary floating point。 */
export function SpotCandleChart({ candles, english }: { candles: MarketCandle[]; english: boolean }) {
  const prices = candles.map((item) => ({ open: atomic(item.open), high: atomic(item.high), low: atomic(item.low), close: atomic(item.close) }));
  const maximum = prices.reduce((value, item) => item.high > value ? item.high : value, prices[0].high);
  const minimum = prices.reduce((value, item) => item.low < value ? item.low : value, prices[0].low);
  const spread = maximum > minimum ? maximum - minimum : 1n;
  const y = (value: bigint) => 10 + Number(((maximum - value) * 21000n) / spread) / 100;
  const width = 720 / candles.length;
  const last = candles[candles.length - 1];
  return (
    <section className="workspace-panel spot-candles" aria-label={english ? 'One-minute reference candlestick chart' : '一分鐘參考 K 線圖'}>
      <div className="workspace-panel__header"><h2>{english ? '1-minute reference K-line' : '一分鐘參考 K 線'}</h2><span>Binance · UTC</span></div>
      <p className="spot-candles__summary">{english ? 'Open' : '開'} {last.open} · {english ? 'High' : '高'} {last.high} · {english ? 'Low' : '低'} {last.low} · {english ? 'Close' : '收'} {last.close} · {english ? 'Volume' : '量'} {last.volume}</p>
      <svg className="spot-candles__plot" viewBox="0 0 720 240" preserveAspectRatio="none" role="img" aria-label={english ? `Latest close ${last.close} USDT` : `最新收盤價 ${last.close} USDT`}>
        {prices.map((price, index) => {
          const x = index * width + width / 2;
          const rising = price.close >= price.open;
          const top = y(rising ? price.close : price.open);
          const bottom = y(rising ? price.open : price.close);
          return <g className={rising ? 'spot-candles__up' : 'spot-candles__down'} key={candles[index].openTime}>
            <line x1={x} y1={y(price.high)} x2={x} y2={y(price.low)} />
            <rect x={x - width * 0.3} y={top} width={Math.max(width * 0.6, 1)} height={Math.max(bottom - top, 1)} />
          </g>;
        })}
      </svg>
      <p className="spot-candles__summary">{new Date(candles[0].openTime).toISOString().slice(0, 16).replace('T', ' ')} UTC — {new Date(last.openTime).toISOString().slice(0, 16).replace('T', ' ')} UTC{!last.closed ? (english ? ' · Current candle updating' : ' · 目前 K 線更新中') : ''}</p>
    </section>
  );
}
