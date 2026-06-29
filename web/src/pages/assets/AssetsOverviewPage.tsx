import { useMemo, useState } from 'react';

import { Card } from '../../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { AssetAccountTable } from '../../features/assets/AssetAccountTable';
import { AssetHistoryList } from '../../features/assets/AssetHistoryList';
import { AssetOverviewMetrics } from '../../features/assets/AssetOverviewMetrics';
import { AssetSectionNav } from '../../features/assets/AssetSectionNav';
import { useAssetOverviewMock } from '../../features/assets/useAssetOverviewMock';
import type { AssetTabKey } from '../../features/assets/mockAssetService';

export function AssetsOverviewPage() {
  const { data, loading, error, reload } = useAssetOverviewMock();
  const [activeTab, setActiveTab] = useState<AssetTabKey>('spot');
  const activeAccount = useMemo(() => data?.accounts.find((account) => account.key === activeTab) ?? null, [activeTab, data]);

  return (
    <div className="stack assets-page">
      <PageHeader
        title="Assets"
        description="A single view for spot, futures, and margin balances, plus internal account transfer controls."
      />
      <AssetSectionNav />

      {loading ? <LoadingState title="Loading assets overview" description="Fetching mock balances and transfer history..." /> : null}
      {error ? <ErrorState title="Unable to load asset data" description={error} action={<button className="secondary-button" type="button" onClick={reload}>Retry</button>} /> : null}

      {!loading && !error && data ? (
        <>
          <AssetOverviewMetrics metrics={data.metrics} />

          <Card title="Account Tabs">
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
            <p className="assets-tabs__hint">{activeAccount?.description ?? 'Select an account tab to inspect balances.'}</p>
          </Card>

          <Card title={activeAccount?.label ?? 'Account'}>
            {activeAccount ? <AssetAccountTable account={activeAccount} /> : <EmptyState title="No account selected" description="Choose a tab to view balances." />}
          </Card>

          <AssetHistoryList history={data.history} />
        </>
      ) : null}
    </div>
  );
}
