import { Card } from '../../components/base/Card';
import { ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
import { WalletAdapterNotice } from '../../features/assets/wallet/WalletAdapterNotice';
import { DepositRecordsTable } from '../../features/assets/wallet/WalletRecordsTable';
import { WalletSectionNav } from '../../features/assets/wallet/WalletSectionNav';
import { useWalletWorkspaceMock } from '../../features/assets/wallet/useWalletWorkspaceMock';
import { formatAmount } from '../../utils/format';

export function DepositHistoryPage() {
  const { t } = useI18n();
  const { data, loading, error, reload } = useWalletWorkspaceMock();

  return (
    <div className="stack assets-page">
      <PageHeader title={t('assets.depositHistoryTitle')} description={t('assets.depositHistoryDescription')} />
      <WalletSectionNav />
      <WalletAdapterNotice notice={data?.adapterNotice ?? t('assets.walletWorkspacesHint')} />

      {loading ? <LoadingState title={t('assets.loadingWalletTitle')} description={t('assets.loadingWalletDescription')} /> : null}
      {error ? <ErrorState title={t('assets.walletErrorTitle')} description={error} action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>} /> : null}

      {!loading && !error && data ? (
        <>
          <section className="assets-metrics">
            <Card title="Confirmed Deposits">
              <div className="assets-metric">
                <strong className="assets-metric__value">{data.depositHistory.filter((item) => item.status === 'Confirmed').length}</strong>
                <p className="assets-metric__hint">Mock adapter records</p>
              </div>
            </Card>
            <Card title="Pending Deposits">
              <div className="assets-metric">
                <strong className="assets-metric__value">{data.depositHistory.filter((item) => item.status === 'Pending').length}</strong>
                <p className="assets-metric__hint">Waiting for confirmations</p>
              </div>
            </Card>
            <Card title="Total Deposited">
              <div className="assets-metric">
                <strong className="assets-metric__value">{formatAmount(data.depositHistory.reduce((sum, item) => sum + item.amount, 0), 2)} USDT eq.</strong>
                <p className="assets-metric__hint">Preview only</p>
              </div>
            </Card>
          </section>

          <Card title="Deposit Records">
            <DepositRecordsTable records={data.depositHistory} />
          </Card>
        </>
      ) : null}
    </div>
  );
}
