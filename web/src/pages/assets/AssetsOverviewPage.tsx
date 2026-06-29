import { useMemo, useState } from 'react';

import { Card } from '../../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
import { AssetAccountTable } from '../../features/assets/AssetAccountTable';
import { AssetHistoryList } from '../../features/assets/AssetHistoryList';
import { AssetOverviewMetrics } from '../../features/assets/AssetOverviewMetrics';
import { AssetSectionNav } from '../../features/assets/AssetSectionNav';
import { useAssetOverviewMock } from '../../features/assets/useAssetOverviewMock';
import type { AssetTabKey } from '../../features/assets/mockAssetService';

export function AssetsOverviewPage() {
  const { t } = useI18n();
  const { data, loading, error, reload } = useAssetOverviewMock();
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
      {error ? <ErrorState title={t('assets.errorTitle')} description={error} action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>} /> : null}

      {!loading && !error && data ? (
        <>
          <AssetOverviewMetrics metrics={data.metrics} />

          <Card title={t('assets.accountTabs')}>
            <div className="assets-tabs">
              {data.accounts.map((account) => (
                <button
                  key={account.key}
                  className={`tab-button${activeTab === account.key ? ' tab-button--active' : ''}`}
                  type="button"
                  onClick={() => setActiveTab(account.key)}
                >
                  {account.label}
                </button>
              ))}
            </div>
            <p className="assets-tabs__hint">{activeAccount?.description ?? t('assets.accountTabHint')}</p>
          </Card>

          <Card title={activeAccount?.label ?? t('account.assetTableTitle')}>
            {activeAccount ? <AssetAccountTable account={activeAccount} /> : <EmptyState title={t('assets.noAccountSelected')} description={t('assets.noAccountSelectedDescription')} />}
          </Card>

          <AssetHistoryList history={data.history} />
        </>
      ) : null}
    </div>
  );
}
