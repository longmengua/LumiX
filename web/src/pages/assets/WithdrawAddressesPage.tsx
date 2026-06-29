import { useEffect, useState } from 'react';

import { ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { WalletAdapterNotice } from '../../features/assets/wallet/WalletAdapterNotice';
import { WalletSectionNav } from '../../features/assets/wallet/WalletSectionNav';
import { WalletWithdrawAddressesPanel } from '../../features/assets/wallet/WalletWithdrawAddressesPanel';
import { useWalletWorkspaceMock } from '../../features/assets/wallet/useWalletWorkspaceMock';
import type { WithdrawAddressRecord } from '../../features/assets/wallet/mockWalletService';

export function WithdrawAddressesPage() {
  const { data, loading, error, reload } = useWalletWorkspaceMock();
  const [addresses, setAddresses] = useState<WithdrawAddressRecord[]>([]);

  useEffect(() => {
    if (data) {
      setAddresses(data.addresses);
    }
  }, [data]);

  return (
    <div className="stack assets-page">
      <PageHeader title="Withdraw Addresses" description="Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API." />
      <WalletSectionNav />
      <WalletAdapterNotice notice={data?.adapterNotice ?? 'Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API.'} />

      {loading ? <LoadingState title="Loading withdraw addresses" description="Fetching mock address book and whitelist settings..." /> : null}
      {error ? <ErrorState title="Unable to load wallet data" description={error} action={<button className="secondary-button" type="button" onClick={reload}>Retry</button>} /> : null}

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
