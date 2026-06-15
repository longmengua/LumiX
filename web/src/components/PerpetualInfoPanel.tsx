import { formatPercent, formatPrice, formatQty } from '../lib/exchange';
import { PerpetualContractState } from '../types/exchange';
import { useI18n } from '../lib/i18n';

/**
 * Perpetual contract risk strip; all values come from backend snapshot so frontend does not invent risk math.
 */
interface Props {
  contract: PerpetualContractState | null;
}

export function PerpetualInfoPanel({ contract }: Props) {
  const { t } = useI18n();
  const nextFunding = contract?.nextFundingTime
    ? new Date(contract.nextFundingTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
    : '--';

  return (
    <section className="panel perp-panel">
      <div className="panel-head">
        <h2>{t('perp.title')}</h2>
        <span>{contract?.status || '--'}</span>
      </div>
      <div className="perp-grid">
        <div>
          <span>{t('perp.markPrice')}</span>
          <strong>{formatPrice(contract?.markPrice ?? null)}</strong>
        </div>
        <div>
          <span>{t('perp.indexPrice')}</span>
          <strong>{formatPrice(contract?.indexPrice ?? null)}</strong>
        </div>
        <div>
          <span>{t('perp.fundingRate')}</span>
          <strong className={(contract?.fundingRate ?? 0) < 0 ? 'down' : 'up'}>{formatPercent(contract?.fundingRate ?? null)}</strong>
        </div>
        <div>
          <span>{t('perp.nextFunding')}</span>
          <strong>{nextFunding}</strong>
        </div>
        <div>
          <span>{t('perp.leverage')}</span>
          <strong>{contract ? `${contract.defaultLeverage}x / ${contract.maxLeverage}x` : '--'}</strong>
        </div>
        <div>
          <span>{t('perp.marginMode')}</span>
          <strong>{contract?.marginMode || '--'}</strong>
        </div>
        <div>
          <span>{t('perp.contractSize')}</span>
          <strong>{contract ? `${formatQty(contract.contractSize)} ${contract.baseAsset}` : '--'}</strong>
        </div>
        <div>
          <span>{t('perp.liquidation')}</span>
          <strong>{formatPrice(contract?.estimatedLiquidationPrice ?? null)}</strong>
        </div>
      </div>
    </section>
  );
}
