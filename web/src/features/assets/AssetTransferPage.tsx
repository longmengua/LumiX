import { useMemo, useState } from 'react';

import { ErrorState, LoadingState } from '../../components/base/State';
import { PageHeader } from '../../components/layout/PageHeader';
import { useI18n } from '../../i18n';
import { formatAmount } from '../../utils/format';
import { AssetHistoryList } from './AssetHistoryList';
import { AssetSectionNav } from './AssetSectionNav';
import { AssetTransferPanel } from './AssetTransferPanel';
import { useAssetOverviewMock } from './useAssetOverviewMock';
import type { AssetTabKey } from './mockAssetService';

type TransferFormState = {
  fromAccount: AssetTabKey;
  toAccount: AssetTabKey;
  asset: string;
  amount: string;
};

const initialForm: TransferFormState = {
  fromAccount: 'spot',
  toAccount: 'futures',
  asset: 'USDT',
  amount: '1500',
};

export function AssetTransferPage() {
  const { t } = useI18n();
  const { data, loading, error, reload } = useAssetOverviewMock();
  const [transferForm, setTransferForm] = useState<TransferFormState>(initialForm);
  const [transferMessage, setTransferMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null);

  // 轉帳可用額度只來自前端快照；正式版本應改成 server 驗證後再回寫結果。
  const transferAvailable = data?.transferBalances[transferForm.fromAccount][transferForm.asset] ?? 0;
  const validationError = validateTransferForm(transferForm, transferAvailable);

  const assetList = useMemo(() => data?.transferAssets ?? ['USDT'], [data]);

  function setFromAccount(fromAccount: AssetTabKey) {
    if (!data) return;

    setTransferMessage(null);
    setTransferForm((current) => {
      const nextToAccount = current.toAccount === fromAccount ? pickDifferentAccount(fromAccount) : current.toAccount;
      const asset = data.transferAssets.includes(current.asset) ? current.asset : data.transferAssets[0] ?? 'USDT';

      return {
        ...current,
        fromAccount,
        toAccount: nextToAccount,
        asset,
        amount: current.amount || formatAmount(data.transferBalances[fromAccount][asset] ?? 0, 2),
      };
    });
  }

  function setToAccount(toAccount: AssetTabKey) {
    setTransferMessage(null);
    setTransferForm((current) => ({ ...current, toAccount }));
  }

  function setAsset(asset: string) {
    if (!data) return;

    setTransferMessage(null);
    setTransferForm((current) => ({
      ...current,
      asset,
      amount: current.amount || formatAmount(data.transferBalances[current.fromAccount][asset] ?? 0, 2),
    }));
  }

  function handleMaxAmount() {
    if (!data) return;

    setTransferMessage(null);
    setTransferForm((current) => ({
      ...current,
      amount: formatAmount(data.transferBalances[current.fromAccount][current.asset] ?? 0, getFractionDigits(current.asset)),
    }));
  }

  function handleTransfer() {
    if (validationError) {
      setTransferMessage({ tone: 'error', text: validationError });
      return;
    }

    // 這裡只產生 queued 訊息，方便把 UI 流程與真正的資金異動明確切開。
    setTransferMessage({
      tone: 'success',
      text: t('assets.walletTransferQueued', undefined, {
        amount: formatAmount(Number(transferForm.amount), getFractionDigits(transferForm.asset)),
        asset: transferForm.asset,
      }),
    });
  }

  return (
    <div className="stack assets-page">
      <PageHeader
        title={t('assets.transferTitle')}
        description={t('assets.transferDescription')}
      />
      <AssetSectionNav />

      {loading ? <LoadingState title={t('assets.transferLoadingTitle')} description={t('assets.transferLoadingDescription')} /> : null}
      {error ? <ErrorState title={t('assets.transferErrorTitle')} description={error} action={<button className="secondary-button" type="button" onClick={reload}>{t('common.retry')}</button>} /> : null}

      {!loading && !error && data ? (
        <>
          <AssetTransferPanel
            fromAccount={transferForm.fromAccount}
            toAccount={transferForm.toAccount}
            asset={transferForm.asset}
            amount={transferForm.amount}
            assets={assetList}
            available={transferAvailable}
            message={transferMessage}
            validationError={validationError}
            onFromAccountChange={setFromAccount}
            onToAccountChange={setToAccount}
            onAssetChange={setAsset}
            onAmountChange={(value) => {
              setTransferMessage(null);
              setTransferForm((current) => ({ ...current, amount: value }));
            }}
            onMaxAmount={handleMaxAmount}
            onSubmit={handleTransfer}
          />

          <AssetHistoryList
            history={data.history}
            emptyTitle={t('assets.transferHistoryEmptyTitle')}
            emptyDescription={t('assets.transferHistoryEmptyDescription')}
          />
        </>
      ) : null}
    </div>
  );
}

function validateTransferForm(form: TransferFormState, available: number) {
  if (form.fromAccount === form.toAccount) {
    return 'From Account and To Account must be different.';
  }

  const amount = Number(form.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    return 'Enter a valid transfer amount.';
  }

  if (amount > available) {
    return 'Transfer amount cannot exceed available balance.';
  }

  return null;
}

function pickDifferentAccount(fromAccount: AssetTabKey): AssetTabKey {
  if (fromAccount === 'spot') return 'futures';
  if (fromAccount === 'futures') return 'spot';
  return 'spot';
}

function getFractionDigits(asset: string) {
  return asset === 'USDT' ? 2 : 4;
}
