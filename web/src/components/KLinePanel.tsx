import { useMemo } from 'react';
import { formatPrice } from '../lib/exchange';
import { KlineRow, MAX_CHART_CANDLES } from '../types/exchange';
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
}

export function KLinePanel({ candles }: Props) {
  const { t } = useI18n();
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

  return (
      <section className="panel chart-panel">
        <div className="panel-head">
          <h2>{t('kline.title')}</h2>
          <span>{t('kline.liveUpdates')}</span>
        </div>
        <div className="kline-chart" role="img" aria-label={t('kline.chartAria')}>
        <div className="kline-grid" aria-hidden>
          {stats.levels.map((level) => (
            <span key={level.toString()} style={{ opacity: level === stats.levels[0] ? 0.65 : 0.35 }}>
              {formatPrice(level)}
            </span>
          ))}
        </div>
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
      </div>
    </section>
  );
}
