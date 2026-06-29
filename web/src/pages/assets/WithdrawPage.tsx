import { useEffect, useState } from 'react';

import { ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
import { WalletAdapterNotice } from '../../features/assets/wallet/WalletAdapterNotice';
import { WalletSecurityStatus } from '../../features/assets/wallet/WalletSecurityStatus';
import { WalletSectionNav } from '../../features/assets/wallet/WalletSectionNav';
import { WalletWithdrawPanel } from '../../features/assets/wallet/WalletWithdrawPanel';
import { useWalletWorkspaceMock } from '../../features/assets/wallet/useWalletWorkspaceMock';
import type { WalletAsset, WalletNetwork } from '../../features/assets/wallet/mockWalletService';

export function WithdrawPage() {
  const { t } = useI18n();
  const { data, loading, error, reload } = useWalletWorkspaceMock();
  const [asset, setAsset] = useState<WalletAsset>('USDT');
  const [network, setNetwork] = useState<WalletNetwork>('TRC20');

  useEffect(() => {
    if (!data) return;
    setAsset(data.withdraw.asset);
    setNetwork(data.withdraw.network);
  }, [data]);

  return (
    <div className="stack assets-page">
      <PageHeader title={t('assets.withdrawTitle')} description={t('assets.withdrawDescription')} />
      <WalletSectionNav />
      <WalletAdapterNotice notice={data?.adapterNotice ?? t('assets.walletWorkspacesHint')} />

      {loading ? <LoadingState title={t('assets.loadingWalletTitle')} description={t('assets.loadingWalletDescription')} /> : null}
      {error ? <ErrorState title={t('assets.walletErrorTitle')} description={error} action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>} /> : null}

      {!loading && !error && data ? (
        <>
          <WalletSecurityStatus
            twoFactor={data.securityStatus.twoFactor}
            whitelist={data.securityStatus.whitelist}
            riskReview={data.securityStatus.riskReview}
            riskNote={data.withdraw.riskReview}
            eta={data.withdraw.eta}
          />

          <WalletWithdrawPanel
            asset={asset}
            network={network}
            address={data.withdraw.withdrawAddress}
            available={data.withdraw.available}
            feeRate={data.withdraw.feeRate}
            flatFee={data.withdraw.flatFee}
            eta={data.withdraw.eta}
            riskReview={data.withdraw.riskReview}
            securityNote={data.withdraw.securityNote}
            assets={data.withdraw.assets}
            networks={data.withdraw.networks}
            onAssetChange={setAsset}
            onNetworkChange={setNetwork}
          />
        </>
      ) : null}
    </div>
  );
}
