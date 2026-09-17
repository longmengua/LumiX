import { useEffect, useState } from 'react';

import { EmptyState, ErrorState, LoadingState } from '../../../components/base/State';
import { useI18n } from '../../../i18n';
import { fetchAdminAssetAdjustmentAudit, type AdminAssetAdjustmentAuditItem } from '../../api/adminAssetsApi';

/**
 * 資產沖銷流水的唯讀對賬面板。
 *
 * 此處只呈現 server 已核對的 immutable 證據；例外狀態只提示人工處理，不能從前端發動補帳。
 */
export function AssetAdjustmentAuditPanel() {
  const { locale, t } = useI18n();
  const [items, setItems] = useState<AdminAssetAdjustmentAuditItem[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setItems(null);
    setLoadError(null);
    void fetchAdminAssetAdjustmentAudit()
      .then((next) => { if (!cancelled) setItems(next); })
      .catch(() => { if (!cancelled) setLoadError(t('admin.assetsAuditLoadFailed')); });
    return () => { cancelled = true; };
  }, [refreshKey, t]);

  if (loadError) {
    return <ErrorState title={t('admin.assetsAuditLoadErrorTitle')} description={loadError} action={<button className="secondary-button" type="button" onClick={() => setRefreshKey((value) => value + 1)}>{t('admin.assetsAuditRetry')}</button>} />;
  }
  if (items === null) return <LoadingState title={t('admin.assetsAuditLoadingTitle')} description={t('admin.assetsAuditLoadingDescription')} />;

  return (
    <section className="admin-asset-audit" aria-label={t('admin.assetsAuditTitle')}>
      <header className="admin-asset-audit__header">
        <div>
          <h2>{t('admin.assetsAuditAdjustmentTitle')}</h2>
          <p>{t('admin.assetsAuditAdjustmentDescription')}</p>
        </div>
        <button className="admin-asset-audit__refresh" type="button" onClick={() => setRefreshKey((value) => value + 1)} aria-label={t('admin.assetsAuditRefresh')}>
          <RefreshIcon />
        </button>
      </header>
      <p className="admin-asset-audit__scope">{t('admin.assetsAuditScopeNotice')}</p>
      {items.length === 0 ? <EmptyState title={t('admin.assetsAuditEmptyTitle')} description={t('admin.assetsAuditEmptyDescription')} /> : (
        <div className="admin-asset-audit__table-wrap">
          <table className="admin-asset-audit__table">
            <thead><tr>
              <th>{t('admin.assetsAuditColumnTime')}</th>
              <th>{t('admin.assetsAuditColumnType')}</th>
              <th>{t('admin.assetsAuditColumnUser')}</th>
              <th>{t('admin.assetsAuditColumnAsset')}</th>
              <th>{t('admin.assetsAuditColumnAmount')}</th>
              <th>{t('admin.assetsAuditColumnCheck')}</th>
              <th>{t('admin.assetsAuditColumnRecord')}</th>
            </tr></thead>
            <tbody>{items.map((item) => <AuditRow key={item.auditLogId} item={item} locale={locale} t={t} />)}</tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function AuditRow({ item, locale, t }: { item: AdminAssetAdjustmentAuditItem; locale: string; t: (key: string, fallback?: string, values?: Record<string, string | number>) => string }) {
  const signedAmount = item.amount === null ? '-' : `${item.direction === 'DEBIT' ? '-' : '+'}${item.amount}`;
  const type = item.activityType === 'REVERSAL' ? t('admin.assetsAdjustmentReversal') : item.activityType === 'AIRDROP' ? t('admin.assetsAirdropType') : t('admin.assetsAuditUnknownType');
  const verified = item.reconciliationStatus === 'VERIFIED';
  return <tr>
    <td><time dateTime={item.postedAt}>{new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'short', hourCycle: 'h23' }).format(new Date(item.postedAt))}</time></td>
    <td>{type}</td>
    <td><span className="admin-asset-audit__user">{item.targetUserId}<small>{item.targetUserEmail}</small></span></td>
    <td>{item.assetSymbol ?? '-'}</td>
    <td className={item.direction === 'DEBIT' ? 'admin-asset-audit__amount admin-asset-audit__amount--negative' : 'admin-asset-audit__amount'}>{signedAmount}</td>
    <td><span className={`admin-asset-audit__status${verified ? ' admin-asset-audit__status--verified' : ' admin-asset-audit__status--exception'}`}>{verified ? t('admin.assetsAuditVerified') : t('admin.assetsAuditException')}</span></td>
    <td><span className="admin-asset-audit__record">#{item.ledgerJournalId}</span>{item.note ? <small>{item.note}</small> : null}</td>
  </tr>;
}

function RefreshIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 11a8 8 0 1 0 2 5.5M20 4v7h-7" /></svg>;
}
