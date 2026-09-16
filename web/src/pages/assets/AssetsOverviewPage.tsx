import { useMemo, useState } from 'react';

import { Card } from '../../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
import { AssetAccountTable } from '../../features/assets/AssetAccountTable';
import { AssetLedgerHistoryList } from '../../features/assets/AssetLedgerHistoryList';
import { AssetOverviewMetrics } from '../../features/assets/AssetOverviewMetrics';
import { accountLabelKeyByTab, assetTabs, type AssetTabKey } from '../../features/assets/assetAccountTypes';
import { AssetSectionNav } from '../../features/assets/AssetSectionNav';
import { useAssetProjectionSnapshot } from '../../features/assets/useAssetProjectionSnapshot';

export function AssetsOverviewPage() {
  const { t } = useI18n();
  const { data, loading, errorCode, reload } = useAssetProjectionSnapshot();
  const [activeTab, setActiveTab] = useState<AssetTabKey>('spot');
  const activeAccount = useMemo(() => data?.accounts.find((account) => account.key === activeTab) ?? null, [activeTab, data]);

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
          <AssetOverviewMetrics accounts={data.accounts} />

          <Card title={t('assets.accountInventoryTitle')}>
            <div className="asset-account-inventory">{data.inventory.map((account) => <div key={account.accountId}><strong>{t(accountLabelKeyByTab[account.accountType.toLowerCase() as AssetTabKey])}</strong><span>{account.accountStatus}</span></div>)}</div>
            {data.inventory.length === 0 ? <EmptyState title={t('assets.noAccountInventoryTitle')} description={t('assets.noAccountInventoryDescription')} /> : null}
          </Card>

          <Card title={t('assets.accountTabs')}>
            <div className="assets-tabs">
              {assetTabs.map((accountKey) => (
                <button
                  key={accountKey}
                  className={`tab-button${activeTab === accountKey ? ' tab-button--active' : ''}`}
                  type="button"
                  onClick={() => setActiveTab(accountKey)}
                >
                  {t(accountLabelKeyByTab[accountKey])}
                </button>
              ))}
            </div>
            <p className="assets-tabs__hint">{t('assets.accountTabHint')}</p>
          </Card>

          <Card title={t(accountLabelKeyByTab[activeTab])}>
            {activeAccount && activeAccount.items.length > 0
              ? <AssetAccountTable account={activeAccount} />
              : <EmptyState title={t('assets.noProjectionTitle')} description={t('assets.noProjectionDescription')} />}
          </Card>
          <AssetLedgerHistoryList />
        </>
      ) : null}
    </div>
  );
}
