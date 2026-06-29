import { Card } from '../../components/base/Card';
import { ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { WalletAdapterNotice } from '../../features/assets/wallet/WalletAdapterNotice';
import { WithdrawRecordsTable } from '../../features/assets/wallet/WalletRecordsTable';
import { WalletSectionNav } from '../../features/assets/wallet/WalletSectionNav';
import { useWalletWorkspaceMock } from '../../features/assets/wallet/useWalletWorkspaceMock';
import { formatAmount } from '../../utils/format';

export function WithdrawHistoryPage() {
  const { data, loading, error, reload } = useWalletWorkspaceMock();

  return (
    <div className="stack assets-page">
      <PageHeader title="Withdraw History" description="Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API." />
      <WalletSectionNav />
      <WalletAdapterNotice notice={data?.adapterNotice ?? 'Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API.'} />

      {loading ? <LoadingState title="Loading withdraw history" description="Fetching mock withdrawal records..." /> : null}
      {error ? <ErrorState title="Unable to load wallet data" description={error} action={<button className="secondary-button" type="button" onClick={reload}>Retry</button>} /> : null}

      {!loading && !error && data ? (
        <>
          <section className="assets-metrics">
            <Card title="Processing">
              <div className="assets-metric">
                <strong className="assets-metric__value">{data.withdrawHistory.filter((item) => item.status === 'Processing').length}</strong>
                <p className="assets-metric__hint">Pending risk review</p>
              </div>
            </Card>
            <Card title="Completed Withdraws">
              <div className="assets-metric">
                <strong className="assets-metric__value">{data.withdrawHistory.filter((item) => item.status === 'Completed').length}</strong>
                <p className="assets-metric__hint">Mock adapter records</p>
              </div>
            </Card>
            <Card title="Total Withdrawn">
              <div className="assets-metric">
                <strong className="assets-metric__value">{formatAmount(data.withdrawHistory.reduce((sum, item) => sum + item.amount, 0), 2)} USDT eq.</strong>
                <p className="assets-metric__hint">Preview only</p>
              </div>
            </Card>
          </section>

          <Card title="Withdraw Records">
            <WithdrawRecordsTable records={data.withdrawHistory} />
          </Card>
        </>
      ) : null}
    </div>
  );
}
