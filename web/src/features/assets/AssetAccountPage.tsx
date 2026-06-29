import { Card } from '../../components/base/Card';
import { EmptyState, ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import type { AssetTabKey } from './mockAssetService';
import { AssetAccountTable } from './AssetAccountTable';
import { AssetHistoryList } from './AssetHistoryList';
import { AssetSectionNav } from './AssetSectionNav';
import { useAssetOverviewMock } from './useAssetOverviewMock';

type AssetAccountPageProps = {
  accountKey: AssetTabKey;
  title: string;
  description: string;
  summaryTitle: string;
  summaryPoints: Array<{ label: string; value: string }>;
};

export function AssetAccountPage({ accountKey, title, description, summaryTitle, summaryPoints }: AssetAccountPageProps) {
  const { data, loading, error, reload } = useAssetOverviewMock();
  const account = data?.accounts.find((item) => item.key === accountKey) ?? null;
  const history = data?.history.filter((item) => item.account.toLowerCase().includes(accountKey)) ?? [];

  return (
    <div className="stack assets-page">
      <PageHeader title={title} description={description} />
      <AssetSectionNav active={accountKey} />

      {loading ? <LoadingState title={`Loading ${title.toLowerCase()}`} description="Fetching mock balances and activity..." /> : null}
      {error ? <ErrorState title="Unable to load asset data" description={error} action={<button className="secondary-button" type="button" onClick={reload}>Retry</button>} /> : null}

      {!loading && !error && data ? (
        <>
          <Card title={summaryTitle}>
            <div className="dashboard-grid dashboard-grid--three">
              {summaryPoints.map((item) => (
                <div className="stat-card" key={item.label}>
                  <span className="stat-card__label">{item.label}</span>
                  <strong>{item.value}</strong>
                </div>
              ))}
            </div>
          </Card>

          <Card title={account?.label ?? title}>
            {account ? <AssetAccountTable account={account} /> : <EmptyState title="No account data" description="This account snapshot is unavailable." />}
          </Card>

          <AssetHistoryList
            title={`Recent ${title} Activity`}
            history={history}
            emptyTitle={`No ${title.toLowerCase()} history`}
            emptyDescription={`Activity for ${title.toLowerCase()} will appear here.`}
          />
        </>
      ) : null}
    </div>
  );
}
