import { useI18n } from '../../i18n';
import { formatAmount, formatCurrency, formatPrice } from '../../utils/format';
import type { TradingPosition } from './mockTradingService';

type TradingPositionsPanelProps = {
  positions: TradingPosition[];
  baseAsset: string;
};

export function TradingPositionsPanel({ positions, baseAsset }: TradingPositionsPanelProps) {
  const { t } = useI18n();

  return (
    <section className="workspace-panel positions-panel">
      <div className="workspace-panel__header">
        <h2>{t('trading.positions.title')}</h2>
        <span>{t('trading.positions.current')}</span>
      </div>
      {positions.length === 0 ? (
        <p className="positions-panel__empty">{t('trading.noPositionsDescription')}</p>
      ) : (
        <div className="positions-panel__list">
          {positions.map((position) => (
            <article className="positions-panel__position" key={`${position.symbol}-${position.side}`}>
              <div>
                <strong>{position.symbol}</strong>
                <span className={position.side === 'Long' ? 'pnl--positive' : 'pnl--negative'}>{t(position.side === 'Long' ? 'trading.position.side.long' : 'trading.position.side.short')} {position.leverage}x</span>
              </div>
              <div className="positions-panel__stats">
                <span>{t('trading.table.size')} <strong>{formatAmount(position.size)} {baseAsset}</strong></span>
                <span>{t('trading.table.entry')} <strong>{formatPrice(position.entryPrice, 2)}</strong></span>
                <span>{t('trading.table.mark')} <strong>{formatPrice(position.markPrice, 2)}</strong></span>
                <span>{t('trading.table.pnl')} <strong className={position.pnl >= 0 ? 'pnl--positive' : 'pnl--negative'}>{formatCurrency(position.pnl)}</strong></span>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
