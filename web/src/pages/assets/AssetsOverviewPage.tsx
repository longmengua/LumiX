import { Card } from '../../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
import { AssetAccountTable } from '../../features/assets/AssetAccountTable';
import { AssetLedgerHistoryList } from '../../features/assets/AssetLedgerHistoryList';
import { accountLabelKeyByTab, assetTabs } from '../../features/assets/assetAccountTypes';
import { AssetSectionNav } from '../../features/assets/AssetSectionNav';
import { useAssetProjectionSnapshot } from '../../features/assets/useAssetProjectionSnapshot';

export function AssetsOverviewPage() {
  const { t } = useI18n();
  const { data, loading, errorCode, reload } = useAssetProjectionSnapshot();

  return (
    <div className="stack assets-page">
      <PageHeader
        title={t('assets.title')}
        description={t('assets.description')}
      />
      <AssetSectionNav />

      {loading ? <LoadingState title={t('assets.loadingTitle')} description={t('assets.loadingDescription')} /> : null}
      {errorCode ? <ErrorState title={t('assets.errorTitle')} description={t('assets.errorDescription')} action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>} /> : null}

      {!loading && !errorCode && data ? (
        <>
          {assetTabs.map((accountKey) => {
            const account = data.accounts.find((item) => item.key === accountKey);
            return (
              <Card key={accountKey} title={t(accountLabelKeyByTab[accountKey])}>
                {account && account.items.length > 0
                  ? <AssetAccountTable account={account} />
                  : <EmptyState title={t('assets.noProjectionTitle')} description={t('assets.noProjectionDescription')} />}
              </Card>
            );
          })}
          <AssetLedgerHistoryList />
        </>
      ) : null}
    </div>
  );
}
