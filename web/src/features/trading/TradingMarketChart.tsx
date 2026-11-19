import { useI18n } from '../../i18n';
import { useTradingChartPreferences } from './ChartPreferences';
import { TradingChartControls } from './TradingChartControls';
import type { MarketCandle } from './marketCandles';
import { formatMarketDecimal, marketAtomic } from './marketDecimal';

/** 現貨與合約唯一圖表模板；來源在外層，繪圖、均線與週期控制只維護這一份。 */
export function TradingMarketChart({ candles, sourceLabel }: { candles: MarketCandle[] | null; sourceLabel: string }) {
  const { t } = useI18n();
  const { timeframe, setTimeframe, averages, setAverages } = useTradingChartPreferences();
  const allPrices = candles?.map(item => ({open: marketAtomic(item.open), high: marketAtomic(item.high), low: marketAtomic(item.low), close: marketAtomic(item.close), volume: marketAtomic(item.volume)})) ?? [];
  const start = Math.max(allPrices.length - 160, 0);
  const visible = candles?.slice(start) ?? [];
  const prices = allPrices.slice(start);
  const last = visible[visible.length - 1];
  const lines = averages.filter(item => item.enabled).map(item => {
    let sum = 0n;
    const values = allPrices.map((price, index) => {
      sum += price.close;
      if (index >= item.period) sum -= allPrices[index - item.period].close;
      // 不足期間保留空值，禁止補零或縮短週期而畫出不存在的均線。
      return index >= item.period - 1 ? sum / BigInt(item.period) : null;
    }).slice(start);
    return {...item, values};
  });
  let maximum = prices[0]?.high ?? 0n, minimum = prices[0]?.low ?? 0n;
  for (const price of prices) { if (price.high > maximum) maximum = price.high; if (price.low < minimum) minimum = price.low; }
  for (const line of lines) for (const value of line.values) {
    if (value !== null) { if (value > maximum) maximum = value; if (value < minimum) minimum = value; }
  }
  const spread = maximum > minimum ? maximum - minimum : 1n;
  const maxVolume = prices.reduce((value, price) => price.volume > value ? price.volume : value, 1n);
  // 僅最終像素比值轉為 number；價量、均線累計及縮放前的數值均保持固定精度整數。
  const y = (value: bigint) => 24 + Number((maximum - value) * 25500n / spread) / 100;
  const width = 1000 / Math.max(prices.length, 1);
  function path(values: Array<bigint | null>) {
    let result = '', started = false;
    values.forEach((value, index) => {
      if (value === null) { started = false; return; }
      result += `${started ? 'L' : 'M'}${index * width + width / 2},${y(value)} `;
      started = true;
    });
    return result;
  }
  return (
    <section className="market-chart" aria-label={t('trading.chart.title')} data-timeframe={timeframe}>
      <div className="market-chart__header">
        <div><h2>{t('trading.chart.title')}</h2><p className={last && marketAtomic(last.close) >= marketAtomic(last.open) ? 'pnl--positive' : 'pnl--negative'}>
          {last ? `${t('trading.chart.open')} ${formatMarketDecimal(last.open)} · ${t('trading.chart.high')} ${formatMarketDecimal(last.high)} · ${t('trading.chart.low')} ${formatMarketDecimal(last.low)} · ${t('trading.chart.close')} ${formatMarketDecimal(last.close)}` : t('trading.chart.waitingData')}
        </p></div>
        <div className="market-chart__legend" aria-label={t('trading.chart.movingAverages')}>
          <span>{sourceLabel}</span>
          {lines.map(line => <span key={line.id} style={{color:line.color}}>MA{line.period}{line.values.every(value => value === null) ? ` · ${t('trading.chart.insufficientHistory')}` : ''}</span>)}
          {lines.length === 0 && <span>{t('trading.chart.averagesHidden')}</span>}
        </div>
      </div>
      <TradingChartControls timeframe={timeframe} onTimeframeChange={setTimeframe} averages={averages} onAveragesChange={setAverages} />
      {last ? <div className="market-chart__plot"><svg className="market-chart__svg" viewBox="0 0 1000 420" preserveAspectRatio="none" role="img" aria-label={t('trading.chart.candleAriaLabel')}>
        {[0,1,2,3,4].map(index => <line className="market-chart__grid-line" key={index} x1="0" x2="1000" y1={24 + index * 255 / 4} y2={24 + index * 255 / 4} />)}
        <line className="market-chart__volume-divider" x1="0" x2="1000" y1="292" y2="292" />
        {prices.map((price,index) => {
          const x = index * width + width / 2, rising = price.close >= price.open;
          const top = y(rising ? price.close : price.open), bottom = y(rising ? price.open : price.close);
          const volumeHeight = Number(price.volume * 7200n / maxVolume) / 100;
          return <g className={`market-chart__candle market-chart__candle--${rising ? 'up' : 'down'}`} key={visible[index].openTime}>
            <line x1={x} x2={x} y1={y(price.high)} y2={y(price.low)} />
            <rect x={x-width*.3} y={top} width={Math.max(width*.6,1)} height={Math.max(bottom-top,1.5)} />
            <rect className="market-chart__volume" x={x-width*.3} y={382-volumeHeight} width={Math.max(width*.6,1)} height={volumeHeight} />
          </g>;
        })}
        {lines.map(line => {
          const values = line.values.filter((value): value is bigint => value !== null);
          return values.length === 1 ? <circle className="market-chart__ma-point" key={line.id} cx={width*(line.values.length-.5)} cy={y(values[0])} r="2" fill={line.color} />
            : <path className="market-chart__ma" key={line.id} d={path(line.values)} stroke={line.color} />;
        })}
      </svg><span className="market-chart__volume-caption">{t('trading.chart.volume')} {formatMarketDecimal(last.volume)}</span></div> : <p className="spot-reference__chart-status" role="status">{t('trading.chart.waitingData')}</p>}
    </section>
  );
}
