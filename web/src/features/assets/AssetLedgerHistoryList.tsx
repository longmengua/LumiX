import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { useI18n } from '../../i18n';
import { formatDateTimeParts, formatDecimalString } from '../../utils/format';
import { useAssetLedgerHistory } from './useAssetLedgerHistory';

/** 正式資產總覽的歷史清單，只呈現 backend 已確認的 immutable ledger lines。 */
export function AssetLedgerHistoryList() {
  const { t } = useI18n();
  const { items, nextCursor, loading, loadingMore, errorCode, reload, loadMore } = useAssetLedgerHistory();
  if (loading) return <LoadingState title={t('assets.historyLoadingTitle')} description={t('assets.historyLoadingDescription')} />;
  if (errorCode && items.length === 0) return <ErrorState title={t('assets.historyErrorTitle')} description={t('assets.historyErrorDescription')} action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>} />;
  return (
    <section className="card asset-ledger-history" aria-labelledby="asset-ledger-history-title">
      <div className="asset-ledger-history__header"><div><h2 className="card__title" id="asset-ledger-history-title">{t('assets.historyTitle')}</h2><p>{t('assets.historyHint')}</p></div><span>{t('assets.historySourceValue')}</span></div>
      {items.length === 0 ? <EmptyState title={t('assets.historyEmptyTitle')} description={t('assets.historyEmptyDescription')} /> : (
        <div className="asset-ledger-history__list">
          {items.map((item) => { const occurred = formatDateTimeParts(item.postedAt); return <article className="asset-ledger-history__item" key={item.entryId}>
            <div><strong>{item.direction === 'CREDIT' ? t('assets.historyCredit') : t('assets.historyDebit')} · {item.assetSymbol}</strong><p>{item.accountType === 'SPOT' ? t('account.spotAccount') : t('account.futuresAccount')} · {referenceTypeLabel(item.referenceType, t)} #{item.referenceId}</p></div>
            <div><strong>{item.direction === 'CREDIT' ? '+' : '-'}{formatDecimalString(item.amount)} {item.assetSymbol}</strong><p>{occurred.date} · {occurred.time}</p></div>
          </article>; })}
        </div>
      )}
      {errorCode && items.length > 0 ? <p className="asset-ledger-history__error">{t('assets.historyMoreError')}</p> : null}
      {nextCursor ? <button className="secondary-button asset-ledger-history__more" type="button" disabled={loadingMore} onClick={loadMore}>{loadingMore ? t('assets.historyLoadingMore') : t('assets.historyLoadMore')}</button> : null}
    </section>
  );
}

function referenceTypeLabel(referenceType: string, t: (key: string, fallback?: string) => string): string {
  const labels: Record<string, string> = {
    ADJUSTMENT: 'assets.historyReferenceAdjustment',
    INTERNAL_TRANSFER: 'assets.historyReferenceTransfer',
    TRANSFER: 'assets.historyReferenceTransfer',
    DEPOSIT: 'assets.historyReferenceDeposit',
    WITHDRAWAL: 'assets.historyReferenceWithdrawal',
    ORDER: 'assets.historyReferenceOrder',
    SETTLEMENT: 'assets.historyReferenceSettlement',
  };
  return t(labels[referenceType] ?? 'assets.historyReferenceGeneric');
}
