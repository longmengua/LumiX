import { MouseEvent, useMemo, useState } from 'react';
import { formatPrice } from '../lib/exchange';
import { KlineInterval, KlineRow, KLINE_INTERVALS, MAX_CHART_CANDLES } from '../types/exchange';
import { useI18n } from '../lib/i18n';

type KlineWithTimestamp = KlineRow & {
  ms: number;
};

/**
 * Candlestick renderer with deterministic ordering and lightweight price grid.
 * K-line data is normalized to timestamps before rendering so browser redraw always follows actual sequence.
 */
interface Props {
  candles: readonly KlineRow[];
  interval: KlineInterval;
  onIntervalChange: (interval: KlineInterval) => void;
}

export function KLinePanel({ candles, interval, onIntervalChange }: Props) {
  const { t } = useI18n();
  const [tool, setTool] = useState<'crosshair' | 'hline'>('crosshair');
  const [hover, setHover] = useState<{ x: number; y: number; price: number; candle: KlineWithTimestamp | null } | null>(null);
  const [hLines, setHLines] = useState<number[]>([]);
  // Ensure candles are always in time order and clipped to last N bars.
  const visible = useMemo(() => {
    return candles
      .map((item): KlineWithTimestamp | null => {
        const ms = Date.parse(item.openTime);
        return Number.isFinite(ms) ? { ...item, ms } : null;
      })
      .filter((item): item is KlineWithTimestamp => item !== null)
      .sort((a, b) => a.ms - b.ms)
      .slice(-MAX_CHART_CANDLES);
  }, [candles]);

  // 建立 5 段固定格線作為肉眼比較參考，避免 bar 對齊飄掉。
  const stats = useMemo(() => {
    if (!visible.length) {
      return { max: 0, min: 0, span: 1, levels: [] as number[] };
    }

    const max = Math.max(...visible.map((c) => c.high));
    const min = Math.min(...visible.map((c) => c.low));
    const span = max - min || 1;
    const levels = [0, 0.25, 0.5, 0.75, 1].map((ratio) => min + span * ratio);

    return { max, min, span, levels };
  }, [visible]);

  if (!visible.length) {
    return (
      <section className="panel chart-panel">
        <div className="panel-head">
          <h2>{t('kline.title')}</h2>
          <span>{t('kline.liveUpdates')}</span>
        </div>
        <div className="kline-chart kline-empty-box" role="img" aria-label={t('kline.chartAria')}>
          <div className="kline-empty">{t('kline.empty')}</div>
        </div>
      </section>
    );
  }

  const labelEvery = Math.max(1, Math.ceil(visible.length / 6));

  const priceToY = (price: number) => ((stats.max - price) / stats.span) * 100;

  const updateHover = (event: MouseEvent<HTMLDivElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const x = Math.max(0, Math.min(rect.width, event.clientX - rect.left));
    const y = Math.max(0, Math.min(rect.height, event.clientY - rect.top));
    const price = stats.max - (y / rect.height) * stats.span;
    const index = Math.max(0, Math.min(visible.length - 1, Math.floor((x / rect.width) * visible.length)));
    setHover({ x, y, price, candle: visible[index] ?? null });
  };

  const handleChartClick = () => {
    if (tool === 'hline' && hover) {
      setHLines((current) => [...current, hover.price]);
    }
  };

  return (
      <section className="panel chart-panel">
        <div className="panel-head">
          <h2>{t('kline.title')}</h2>
          <div className="kline-toolbar">
            <div className="kline-intervals">
              {KLINE_INTERVALS.map((item) => (
                <button key={item} type="button" className={item === interval ? 'active' : ''} onClick={() => onIntervalChange(item)}>
                  {item}
                </button>
              ))}
            </div>
            <button type="button" className={tool === 'crosshair' ? 'active' : ''} onClick={() => setTool('crosshair')}>
              {t('kline.tool.crosshair')}
            </button>
            <button type="button" className={tool === 'hline' ? 'active' : ''} onClick={() => setTool('hline')}>
              {t('kline.tool.hline')}
            </button>
            <button type="button" onClick={() => setHLines([])}>
              {t('kline.tool.clear')}
            </button>
          </div>
        </div>
        <div className={`kline-chart tool-${tool}`} role="img" aria-label={t('kline.chartAria')}>
          <div
            className="kline-plot"
            onMouseMove={updateHover}
            onMouseLeave={() => setHover(null)}
            onClick={handleChartClick}
          >
            <div className="kline-grid" aria-hidden />
            <div className="kline-track">
              {visible.map((candle, index) => {
                const bullish = candle.close >= candle.open;
                const bodyTop = ((stats.max - Math.max(candle.open, candle.close)) / stats.span) * 100;
                const bodyHeight = Math.max(((Math.abs(candle.close - candle.open) / stats.span) * 100), 1.5);
                const wickTop = ((stats.max - candle.high) / stats.span) * 100;
                const wickHeight = ((candle.high - candle.low) / stats.span) * 100;

                const label = new Date(candle.ms).toLocaleTimeString('en-US', {
                  hour: '2-digit',
                  minute: '2-digit'
                });

                return (
                  <div
                    className="kline-candle"
                    key={`${candle.openTime}-${index}`}
                    title={`${new Date(candle.ms).toLocaleString('en-US')} O:${formatPrice(candle.open)} H:${formatPrice(candle.high)} L:${formatPrice(candle.low)} C:${formatPrice(candle.close)}`}
                  >
                    <span className="kline-wick" style={{ top: `${wickTop}%`, height: `${wickHeight}%` }} />
                    <span
                      className={`kline-body ${bullish ? 'up' : 'down'}`}
                      style={{
                        top: `${bodyTop}%`,
                        height: `${bodyHeight}%`
                      }}
                    />
                    <span className="kline-label" aria-hidden={index % labelEvery !== 0}>
                      {index % labelEvery === 0 ? label : ''}
                    </span>
                  </div>
                );
              })}
            </div>
            {hLines.map((price, index) => (
              <div className="kline-drawing-line" key={`${price}-${index}`} style={{ top: `${priceToY(price)}%` }} />
            ))}
            {hover ? (
              <>
                <div className="kline-crosshair-x" style={{ left: hover.x }} />
                <div className="kline-crosshair-y" style={{ top: hover.y }} />
                {hover.candle ? (
                  <div className="kline-tooltip" style={{ left: Math.min(hover.x + 12, 300), top: Math.max(hover.y - 36, 10) }}>
                    <strong>{new Date(hover.candle.ms).toLocaleString('en-US')}</strong>
                    <span>O {formatPrice(hover.candle.open)} H {formatPrice(hover.candle.high)}</span>
                    <span>L {formatPrice(hover.candle.low)} C {formatPrice(hover.candle.close)}</span>
                  </div>
                ) : null}
              </>
            ) : null}
          </div>
          <div className="kline-price-axis" aria-hidden>
            {stats.levels.map((level) => (
              <span key={level.toString()} style={{ opacity: level === stats.levels[0] ? 0.65 : 0.35 }}>
                {formatPrice(level)}
              </span>
            ))}
            {hLines.map((price, index) => (
              <strong className="kline-axis-drawing-label" key={`${price}-${index}`} style={{ top: `${priceToY(price)}%` }}>
                {formatPrice(price)}
              </strong>
            ))}
            {hover ? (
              <strong className="kline-price-tag" style={{ top: hover.y }}>
                {formatPrice(hover.price)}
              </strong>
            ) : null}
          </div>
      </div>
    </section>
  );
}
