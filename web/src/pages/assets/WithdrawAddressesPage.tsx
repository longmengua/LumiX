import { useEffect, useState } from 'react';

import { ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
import { WalletAdapterNotice } from '../../features/assets/wallet/WalletAdapterNotice';
import { WalletSectionNav } from '../../features/assets/wallet/WalletSectionNav';
import { WalletWithdrawAddressesPanel } from '../../features/assets/wallet/WalletWithdrawAddressesPanel';
import { useWalletWorkspaceMock } from '../../features/assets/wallet/useWalletWorkspaceMock';
import type { WithdrawAddressRecord } from '../../features/assets/wallet/mockWalletService';

export function WithdrawAddressesPage() {
  const { t } = useI18n();
  const { data, loading, error, reload } = useWalletWorkspaceMock();
  const [addresses, setAddresses] = useState<WithdrawAddressRecord[]>([]);

  useEffect(() => {
    if (data) {
      setAddresses(data.addresses);
    }
  }, [data]);

  return (
    <div className="stack assets-page">
      <PageHeader title={t('assets.withdrawAddressesTitle')} description={t('assets.withdrawAddressesDescription')} />
      <WalletSectionNav />
      <WalletAdapterNotice notice={data?.adapterNotice ?? t('assets.walletWorkspacesHint')} />

      {loading ? <LoadingState title={t('assets.loadingWalletTitle')} description={t('assets.loadingWalletDescription')} /> : null}
      {error ? <ErrorState title={t('assets.walletErrorTitle')} description={error} action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>} /> : null}

      {!loading && !error && data ? (
        <WalletWithdrawAddressesPanel
          addresses={addresses}
          onCreateAddress={(nextAddress) => setAddresses((current) => [nextAddress, ...current])}
          onToggleAddress={(id) => setAddresses((current) => current.map((item) => (item.id === id ? { ...item, active: !item.active } : item)))}
          onDeleteAddress={(id) => setAddresses((current) => current.filter((item) => item.id !== id))}
        />
      ) : null}
    </div>
  );
}
