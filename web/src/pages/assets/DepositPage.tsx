import { useEffect, useState } from 'react';

import { Card } from '../../components/base/Card';
import { LoadingState, ErrorState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { WalletAdapterNotice } from '../../features/assets/wallet/WalletAdapterNotice';
import { WalletDepositPanel } from '../../features/assets/wallet/WalletDepositPanel';
import { DepositRecordsTable } from '../../features/assets/wallet/WalletRecordsTable';
import { WalletSectionNav } from '../../features/assets/wallet/WalletSectionNav';
import { useWalletWorkspaceMock } from '../../features/assets/wallet/useWalletWorkspaceMock';
import type { WalletAsset, WalletNetwork } from '../../features/assets/wallet/mockWalletService';

export function DepositPage() {
  const { data, loading, error, reload } = useWalletWorkspaceMock();
  const [asset, setAsset] = useState<WalletAsset>('USDT');
  const [network, setNetwork] = useState<WalletNetwork>('TRC20');

  useEffect(() => {
    if (!data) return;
    setAsset(data.deposit.asset);
    setNetwork(data.deposit.network);
  }, [data]);

  const activeDeposit = data?.deposit ?? null;

  return (
    <div className="stack assets-page">
      <PageHeader title="Deposit" description="Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API." />
      <WalletSectionNav />
      <WalletAdapterNotice notice={data?.adapterNotice ?? 'Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API.'} />

      {loading ? <LoadingState title="Loading deposit workspace" description="Fetching mock deposit address and record history..." /> : null}
      {error ? <ErrorState title="Unable to load wallet data" description={error} action={<button className="secondary-button" type="button" onClick={reload}>Retry</button>} /> : null}

      {!loading && !error && activeDeposit && data ? (
        <>
          <WalletDepositPanel
            asset={asset}
            network={network}
            address={activeDeposit.address}
            memoTag={activeDeposit.memoTag}
            minimumDeposit={activeDeposit.minimumDeposit}
            confirmationsRequired={activeDeposit.confirmationsRequired}
            riskHint={activeDeposit.riskHint}
            assets={activeDeposit.assets}
            networks={activeDeposit.networks}
            onAssetChange={setAsset}
            onNetworkChange={setNetwork}
          >
            <DepositRecordsTable records={activeDeposit.recentDeposits} />
          </WalletDepositPanel>

          <Card title="Deposit Notes">
            <div className="stack">
              <p className="wallet-warning">Never send unsupported assets or cross-chain deposits. Funds may be unrecoverable.</p>
              <p className="wallet-page__meta">Wallet records shown here are mock-only and will be replaced with server wallet callbacks, risk checks, and ledger settlement in OL.</p>
            </div>
          </Card>
        </>
      ) : null}
    </div>
  );
}
