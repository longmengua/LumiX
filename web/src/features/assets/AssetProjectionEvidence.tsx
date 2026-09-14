import { Badge } from '../../components/base/Badge';
import { useI18n } from '../../i18n';
import { formatTime } from '../../utils/format';
import type { AssetProjectionAccount } from './assetProjectionApi';

/** 僅呈現 API 已提供的 projection provenance，避免 UI 對未完成的 ledger runtime 做過度承諾。 */
export function AssetProjectionEvidence({ account }: { account: AssetProjectionAccount }) {
  const { t } = useI18n();
  const projectedAt = account.items.reduce<string | null>((latest, item) => {
    return latest === null || item.projectedAt > latest ? item.projectedAt : latest;
  }, null);
  const isReconciled = account.items.length > 0 && account.items.every((item) => item.freshness === 'RECONCILED');

  return (
    <section className="asset-projection-evidence" aria-label={t('assets.projectionEvidenceTitle')}>
      <div>
        <span>{t('assets.projectionSourceLabel')}</span>
        <strong>{t('assets.projectionSourceValue')}</strong>
      </div>
      <div>
        <span>{t('assets.latestProjectionLabel')}</span>
        <strong>{projectedAt ? formatTime(projectedAt) : t('assets.noProjectionTimestamp')}</strong>
      </div>
      <div>
        <span>{t('assets.reconciliationLabel')}</span>
        <Badge tone={isReconciled ? 'success' : 'warning'}>
          {t(isReconciled ? 'assets.freshnessReconciled' : 'assets.freshnessUnreconciled')}
        </Badge>
      </div>
    </section>
  );
}
