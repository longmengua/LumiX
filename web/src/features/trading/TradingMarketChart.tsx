import { useState } from 'react';

import { useI18n } from '../../i18n';
import { formatAmount, formatPercent, formatPrice } from '../../utils/format';
import {
  calculateSimpleMovingAverage,
  getMockMarketCandles,
  type MarketTimeframe,
  type TradingCandle,
} from './mockTradingService';

type TradingMarketChartProps = {
  symbol: string;
  lastPrice: number;
};

const timeframeKeys: Array<{ value: MarketTimeframe; labelKey: string }> = [
  { value: 'intraday', labelKey: 'trading.chart.timeframe.intraday' },
  { value: '1m', labelKey: 'trading.chart.timeframe.1m' },
  { value: '5m', labelKey: 'trading.chart.timeframe.5m' },
  { value: '15m', labelKey: 'trading.chart.timeframe.15m' },
  { value: '1h', labelKey: 'trading.chart.timeframe.1h' },
  { value: '4h', labelKey: 'trading.chart.timeframe.4h' },
  { value: '1d', labelKey: 'trading.chart.timeframe.1d' },
  { value: '1w', labelKey: 'trading.chart.timeframe.1w' },
  { value: '1mo', labelKey: 'trading.chart.timeframe.1mo' },
  { value: '1q', labelKey: 'trading.chart.timeframe.1q' },
];

const movingAverageStyles = [
  { period: 5, color: '#fbbf24' },
  { period: 20, color: '#38bdf8' },
  { period: 60, color: '#c084fc' },
  { period: 120, color: '#fb7185' },
] as const;

const chartWidth = 1000;
const chartHeight = 520;
const priceTop = 28;
const priceHeight = 330;
const volumeTop = 388;
const volumeHeight = 92;

function createLinePath(values: Array<number | null>, minPrice: number, maxPrice: number) {
  const scaleY = (value: number) => priceTop + ((maxPrice - value) / (maxPrice - minPrice || 1)) * priceHeight;
  let path = '';
  let started = false;

  values.forEach((value, index) => {
    if (value === null) {
      started = false;
      return;
    }

    const x = (index / Math.max(values.length - 1, 1)) * chartWidth;
    path += `${started ? 'L' : 'M'}${x.toFixed(2)},${scaleY(value).toFixed(2)} `;
    started = true;
  });

  return path;
}

function getVisibleCandles(candles: TradingCandle[]) {
  // 保留足夠長的可視區間，MA120 才能在日線上呈現，而不是被較短的視窗截斷。
  return candles.slice(-160);
}

export function TradingMarketChart({ symbol, lastPrice }: TradingMarketChartProps) {
  const { t } = useI18n();
  const [timeframe, setTimeframe] = useState<MarketTimeframe>('1d');
  const allCandles = getMockMarketCandles(symbol, timeframe, lastPrice);
  const startIndex = Math.max(allCandles.length - 160, 0);
  const candles = getVisibleCandles(allCandles);
  const lastCandle = candles[candles.length - 1];
  const firstCandle = candles[0];
  const change = firstCandle ? ((lastCandle.close - firstCandle.open) / firstCandle.open) * 100 : 0;
  const minPrice = Math.min(...candles.map((candle) => candle.low));
  const maxPrice = Math.max(...candles.map((candle) => candle.high));
  const maxVolume = Math.max(...candles.map((candle) => candle.volume), 1);
  const candleWidth = chartWidth / Math.max(candles.length, 1);
  const maLines = movingAverageStyles.map(({ period, color }) => ({
    period,
    color,
    values: calculateSimpleMovingAverage(allCandles, period).slice(startIndex),
  }));
  const priceY = (value: number) => priceTop + ((maxPrice - value) / (maxPrice - minPrice || 1)) * priceHeight;

  return (
    <section className="market-chart" aria-label={t('trading.chart.title')}>
      <div className="market-chart__header">
        <div>
          <h2>{t('trading.chart.title')}</h2>
          <p className={change >= 0 ? 'pnl--positive' : 'pnl--negative'}>
            {t('trading.chart.open')} {formatPrice(lastCandle.open, 2)} · {t('trading.chart.high')} {formatPrice(lastCandle.high, 2)} · {t('trading.chart.low')} {formatPrice(lastCandle.low, 2)} · {t('trading.chart.close')} {formatPrice(lastCandle.close, 2)} · {formatPercent(change)}
          </p>
        </div>
        <div className="market-chart__legend" aria-label={t('trading.chart.movingAverages')}>
          {movingAverageStyles.map(({ period, color }) => <span key={period} style={{ '--line-color': color } as React.CSSProperties}>MA{period}</span>)}
        </div>
      </div>

      <div className="market-chart__timeframes" role="tablist" aria-label={t('trading.chart.timeframes')}>
        {timeframeKeys.map((item) => (
          <button
            className={`market-chart__timeframe${timeframe === item.value ? ' market-chart__timeframe--active' : ''}`}
            key={item.value}
            type="button"
            role="tab"
            aria-selected={timeframe === item.value}
            onClick={() => setTimeframe(item.value)}
          >
            {t(item.labelKey)}
          </button>
        ))}
      </div>

      <svg className="market-chart__svg" viewBox={`0 0 ${chartWidth} ${chartHeight}`} role="img" aria-label={t('trading.chart.candleAriaLabel')}>
        {[0, 1, 2, 3, 4].map((index) => {
          const y = priceTop + (priceHeight / 4) * index;
          return <line className="market-chart__grid-line" key={y} x1="0" x2={chartWidth} y1={y} y2={y} />;
        })}
        <line className="market-chart__volume-divider" x1="0" x2={chartWidth} y1={volumeTop - 18} y2={volumeTop - 18} />
        {candles.map((candle, index) => {
          const x = (index * candleWidth) + candleWidth / 2;
          const rising = candle.close >= candle.open;
          const bodyTop = priceY(Math.max(candle.open, candle.close));
          const bodyHeight = Math.max(Math.abs(priceY(candle.open) - priceY(candle.close)), 1.5);
          const volumeHeightValue = (candle.volume / maxVolume) * volumeHeight;
          return (
            <g key={candle.time} className={rising ? 'market-chart__candle market-chart__candle--up' : 'market-chart__candle market-chart__candle--down'}>
              <line x1={x} x2={x} y1={priceY(candle.high)} y2={priceY(candle.low)} />
              <rect x={x - Math.max(candleWidth * 0.3, 1)} y={bodyTop} width={Math.max(candleWidth * 0.6, 2)} height={bodyHeight} />
              <rect className="market-chart__volume" x={x - Math.max(candleWidth * 0.3, 1)} y={volumeTop + volumeHeight - volumeHeightValue} width={Math.max(candleWidth * 0.6, 2)} height={volumeHeightValue} />
            </g>
          );
        })}
        {maLines.map((line) => <path className="market-chart__ma" d={createLinePath(line.values, minPrice, maxPrice)} key={line.period} stroke={line.color} />)}
        <text className="market-chart__volume-label" x="0" y={volumeTop - 28}>{t('trading.chart.volume')} {formatAmount(lastCandle.volume, 2)}</text>
      </svg>
    </section>
  );
}
