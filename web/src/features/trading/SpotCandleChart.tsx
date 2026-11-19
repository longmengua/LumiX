import { formatMarketDecimal, marketAtomic as atomic } from './marketDecimal';

/** 來源金額保持十進位字串；只有最終畫面座標使用 number。 */
export type MarketCandle = { openTime: number; open: string; high: string; low: string; close: string; volume: string; closed: boolean };

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

const averages = [{ period: 5, color: '#fbbf24' }, { period: 20, color: '#38bdf8' }, { period: 60, color: '#c084fc' }, { period: 120, color: '#fb7185' }];

/** 沿用合約圖表的版型與座標系；均線及成交量僅由已驗證的一分鐘真實 K 線推導。 */
export function SpotCandleChart({ candles, english }: { candles: MarketCandle[] | null; english: boolean }) {
  const last = candles ? candles[candles.length - 1] : undefined;
  const prices = candles?.map(item => ({ open: atomic(item.open), high: atomic(item.high), low: atomic(item.low), close: atomic(item.close), volume: atomic(item.volume) })) ?? [];
  const maximum = prices.reduce((value, item) => item.high > value ? item.high : value, prices[0]?.high ?? 0n);
  const minimum = prices.reduce((value, item) => item.low < value ? item.low : value, prices[0]?.low ?? 0n);
  const maxVolume = prices.reduce((value, item) => item.volume > value ? item.volume : value, 1n);
  const spread = maximum > minimum ? maximum - minimum : 1n;
  // 固定精度計算到畫面百分之一單位；截斷僅作用於像素，來源金額不受影響。
  const y = (value: bigint) => 24 + Number((maximum - value) * 25500n / spread) / 100;
  const width = 1000 / Math.max(prices.length, 1);
  const maLines = averages.map(({period, color}) => {
    let sum = 0n;
    let path = '';
    prices.forEach((item, index) => {
      sum += item.close;
      if (index >= period) sum -= prices[index - period].close;
      if (index >= period - 1) path += `${path ? 'L' : 'M'}${index * width + width / 2},${y(sum / BigInt(period))} `;
    });
    return {period, color, path};
  });
  return (
    <section className="market-chart spot-candles" aria-label={english ? 'One-minute reference candlestick chart' : '一分鐘參考 K 線圖'}>
      <div className="market-chart__header">
        <div><h2>{english ? 'Candlestick chart' : 'K 線圖'}</h2>
          <p className={last && atomic(last.close) >= atomic(last.open) ? 'pnl--positive' : 'pnl--negative'}>
            {last ? `${english ? 'Open' : '開'} ${formatMarketDecimal(last.open)} · ${english ? 'High' : '高'} ${formatMarketDecimal(last.high)} · ${english ? 'Low' : '低'} ${formatMarketDecimal(last.low)} · ${english ? 'Close' : '收'} ${formatMarketDecimal(last.close)}` : (english ? 'Reference K-line unavailable or synchronizing.' : '參考 K 線目前無法取得或正在同步。')}
          </p>
        </div>
        <div className="market-chart__legend" aria-label={english ? 'Moving averages' : '移動平均線'}>{averages.map(item => <span key={item.period} style={{color: item.color}}>MA{item.period}</span>)}</div>
      </div>
      <div className="market-chart__timeframes"><span className="market-chart__timeframe market-chart__timeframe--active">{english ? '1 min' : '1 分'}</span><span className="market-chart__timeframe">Binance · UTC</span></div>
      {candles && last ? <>
        <svg className="market-chart__svg spot-candles__plot" viewBox="0 0 1000 420" preserveAspectRatio="none" role="img" aria-label={english ? `Latest close ${last.close} USDT` : `最新收盤價 ${last.close} USDT`}>
          {[0, 1, 2, 3, 4].map(index => <line className="market-chart__grid-line" key={index} x1="0" x2="1000" y1={24 + index * 255 / 4} y2={24 + index * 255 / 4} />)}
          <line className="market-chart__volume-divider" x1="0" x2="1000" y1="292" y2="292" />
          {prices.map((price, index) => {
            const x = index * width + width / 2;
            const rising = price.close >= price.open;
            const top = y(rising ? price.close : price.open);
            const bottom = y(rising ? price.open : price.close);
            const volumeHeight = Number(price.volume * 7200n / maxVolume) / 100;
            return <g className={`market-chart__candle market-chart__candle--${rising ? 'up' : 'down'}`} key={candles[index].openTime}>
              <line x1={x} y1={y(price.high)} x2={x} y2={y(price.low)} />
              <rect x={x - width * 0.3} y={top} width={Math.max(width * 0.6, 1)} height={Math.max(bottom - top, 1.5)} />
              <rect className="market-chart__volume" x={x - width * 0.3} y={382 - volumeHeight} width={Math.max(width * 0.6, 1)} height={volumeHeight} />
            </g>;
          })}
          {maLines.filter(line => line.path).map(line => <path className="market-chart__ma" d={line.path} key={line.period} stroke={line.color} />)}
          <text className="market-chart__volume-label" x="0" y="282">{english ? 'Volume' : '成交量'} {formatMarketDecimal(last.volume)}</text>
        </svg>
      </> : <p className="spot-reference__chart-status" role="status">{english ? 'Waiting for live market data.' : '等待即時行情資料。'}</p>}
    </section>
  );
}
