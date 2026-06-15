import {
  formatDecimal,
  formatPrice,
  formatQty
} from '../lib/exchange';
import {
  KlineRow,
  MarketTickerState
} from '../types/exchange';
import { useI18n } from '../lib/i18n';

/**
 * Top metrics row: last price, 24h change, high/low and volume.
 * Keep this component pure to make trading-page telemetry easy to swap.
 */
interface Props {
  ticker: MarketTickerState | null;
  symbol: string;
  candles: readonly KlineRow[];
}

export function MarketRibbon({ ticker, symbol, candles }: Props) {
  const { t } = useI18n();
  const last = ticker?.lastPrice ?? candles.at(-1)?.close ?? null;
  const volume = ticker?.volume24h ?? 0;
  const high = ticker?.high24h ?? candles.reduce((acc, row) => (row.high > acc ? row.high : acc), 0);
  const low = ticker?.low24h
    ? ticker.low24h
    : candles.reduce((acc, row) => (acc === null || row.low < acc ? row.low : acc), null as number | null);
  const first = candles.length > 1 ? candles[0].open : last;
  const change = last === null || first === null || first === 0 ? null : ((last - first) / first) * 100;

  return (
    <section className="market-ribbon">
      <div>
        <div className="meta">{t('market.lastPrice')}</div>
        <div className="meta-value">{formatPrice(last)}</div>
      </div>
      <div>
        <div className="meta">{t('market.change24h')}</div>
        <div className={`meta-value ${change === null || change < 0 ? 'down' : 'up'}`}>
          {change === null ? '--' : `${change >= 0 ? '+' : ''}${formatDecimal(change, 2, 2)}%`}
        </div>
      </div>
      <div>
        <div className="meta">{t('market.high24h')}</div>
        <div className="meta-value">{high === undefined || high === 0 ? '--' : formatPrice(high)}</div>
      </div>
      <div>
        <div className="meta">{t('market.low24h')}</div>
        <div className="meta-value">{low === null ? '--' : formatPrice(low)}</div>
      </div>
      <div>
        <div className="meta">{t('market.volume', { symbol })}</div>
        <div className="meta-value">{formatQty(volume)}</div>
      </div>
    </section>
  );
}
