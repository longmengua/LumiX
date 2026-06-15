import { PositionRow } from '../types/exchange';
import { useI18n } from '../lib/i18n';

/**
 * Position summary card; currently driven by static fixtures
 * to keep the first stable production-like render before full account APIs are wired.
 */
interface Props {
  rows: readonly PositionRow[];
}

export function PositionPanel({ rows }: Props) {
  const { t } = useI18n();
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>{t('positions.title')}</h2>
      </div>
      <div className="table-head">
        <span>{t('positions.heading.market')}</span>
        <span>{t('positions.heading.side')}</span>
        <span>{t('positions.heading.qty')}</span>
        <span>{t('positions.heading.entry')}</span>
        <span>{t('positions.heading.margin')}</span>
        <span>{t('positions.heading.pnl')}</span>
      </div>
      {rows.map((position) => (
        <div className="table-row" key={`${position.market}-${position.side}`}>
          <span>{position.market}</span>
          <span className={position.side === 'LONG' ? 'up' : 'down'}>{position.side}</span>
          <span>{position.qty}</span>
          <span>{position.entry}</span>
          <span>{position.margin}</span>
          <span>{position.pnl}</span>
        </div>
      ))}
    </section>
  );
}
