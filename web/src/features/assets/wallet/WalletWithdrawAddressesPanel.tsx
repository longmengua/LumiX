import { useState } from 'react';

import { Badge } from '../../../components/base/Badge';
import { SecurityVerifyModal, type VerificationMethod } from '../../../components/auth/SecurityVerifyModal';
import type { WithdrawAddressRecord, WalletAsset, WalletNetwork } from './mockWalletService';

type WalletWithdrawAddressesPanelProps = {
  addresses: WithdrawAddressRecord[];
  onCreateAddress: (address: WithdrawAddressRecord) => void;
  onToggleAddress: (id: string) => void;
  onDeleteAddress: (id: string) => void;
};

export function WalletWithdrawAddressesPanel({
  addresses,
  onCreateAddress,
  onToggleAddress,
  onDeleteAddress,
}: WalletWithdrawAddressesPanelProps) {
  const [verifyOpen, setVerifyOpen] = useState(false);
  const [draft, setDraft] = useState({
    label: '',
    asset: 'USDT' as WalletAsset,
    network: 'TRC20' as WalletNetwork,
    address: '',
    memoTag: '',
    whitelistEnabled: true,
  });
  const [message, setMessage] = useState<string | null>(null);

  function handleOpen() {
    setMessage(null);
    setVerifyOpen(true);
  }

  function handleConfirm(input: { method: VerificationMethod; code: string }) {
    setVerifyOpen(false);
    if (input.code.trim().length < 4) {
      setMessage('Verification code is required.');
      return;
    }

    // 這裡只是在本地組出新地址快照；正式環境應先通過安全驗證與 server 審核。
    const nextAddress: WithdrawAddressRecord = {
      id: `addr-${Date.now()}`,
      label: draft.label || 'New address',
      asset: draft.asset,
      network: draft.network,
      address: draft.address,
      memoTag: draft.memoTag,
      whitelistEnabled: draft.whitelistEnabled,
      riskFlag: draft.whitelistEnabled ? 'Low' : 'Medium',
      active: true,
    };

    onCreateAddress(nextAddress);
    setDraft({
      label: '',
      asset: 'USDT',
      network: 'TRC20',
      address: '',
      memoTag: '',
      whitelistEnabled: true,
    });
    setMessage(`Address created with ${input.method.toUpperCase()} verification.`);
  }

  return (
    <>
      <section className="card">
        {/* 提領地址管理屬於高風險操作，前端只負責提示與驗證流程。 */}
        <h2 className="card__title">Withdraw Addresses</h2>
        <p className="wallet-page__meta">Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API.</p>

        <div className="wallet-form wallet-form--addresses">
          <label className="field">
            <span className="field__label">Label</span>
            <input className="input" value={draft.label} onChange={(event) => setDraft((current) => ({ ...current, label: event.target.value }))} />
          </label>
          <label className="field">
            <span className="field__label">Asset</span>
            <select className="input" value={draft.asset} onChange={(event) => setDraft((current) => ({ ...current, asset: event.target.value as WalletAsset }))}>
              {['USDT', 'BTC', 'ETH'].map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span className="field__label">Chain</span>
            <select className="input" value={draft.network} onChange={(event) => setDraft((current) => ({ ...current, network: event.target.value as WalletNetwork }))}>
              {['TRC20', 'ERC20', 'BTC', 'SOL'].map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>
          <label className="field wallet-form__span-2">
            <span className="field__label">Address</span>
            <input className="input" value={draft.address} onChange={(event) => setDraft((current) => ({ ...current, address: event.target.value }))} />
          </label>
          <label className="field">
            <span className="field__label">Memo / Tag</span>
            <input className="input" value={draft.memoTag} onChange={(event) => setDraft((current) => ({ ...current, memoTag: event.target.value }))} />
          </label>
          <label className="field">
            <span className="field__label">Whitelist</span>
            <select className="input" value={draft.whitelistEnabled ? 'enabled' : 'disabled'} onChange={(event) => setDraft((current) => ({ ...current, whitelistEnabled: event.target.value === 'enabled' }))}>
              <option value="enabled">Enabled</option>
              <option value="disabled">Disabled</option>
            </select>
          </label>
        </div>

        <div className="transfer-actions">
          <button className="primary-button" type="button" onClick={handleOpen}>
            Add address
          </button>
          <p className="wallet-warning">Adding or modifying withdraw addresses requires security verification in the mock flow.</p>
        </div>

        {message ? <p className="form-message form-message--success">{message}</p> : null}
      </section>

      <section className="card">
        <h2 className="card__title">Address Book</h2>
        <div className="wallet-address-list">
          {addresses.map((item) => (
            <article className="wallet-address-row" key={item.id}>
              <div>
                <div className="wallet-address-row__title">
                  <strong>{item.label}</strong>
                  <Badge tone={item.active ? 'success' : 'warning'}>{item.active ? 'Active' : 'Disabled'}</Badge>
                </div>
                <p className="wallet-address-row__meta">
                  {item.asset} · {item.network} · {item.address}
                </p>
                <p className="wallet-address-row__meta">Memo / Tag: {item.memoTag || 'None'}</p>
              </div>
              <div className="wallet-address-row__actions">
                <Badge tone={getRiskTone(item.riskFlag)}>Risk {item.riskFlag}</Badge>
                <Badge tone={item.whitelistEnabled ? 'success' : 'warning'}>{item.whitelistEnabled ? 'Whitelisted' : 'Not whitelisted'}</Badge>
                <button className="secondary-button" type="button" onClick={() => onToggleAddress(item.id)}>
                  {item.active ? 'Disable' : 'Enable'}
                </button>
                <button className="ghost-button" type="button" onClick={() => onDeleteAddress(item.id)}>
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>

      <SecurityVerifyModal
        open={verifyOpen}
        title="Add withdraw address"
        description="Simulate the security verification required for address management."
        confirmLabel="Create address"
        onClose={() => setVerifyOpen(false)}
        onConfirm={handleConfirm}
      />
    </>
  );
}

// 風險標籤只用於 UI 呈現，不代表真實風控模型的最終判定。
function getRiskTone(risk: WithdrawAddressRecord['riskFlag']) {
  if (risk === 'Low') return 'success';
  if (risk === 'Medium') return 'warning';
  return 'danger';
}
