import { useMemo, useState } from 'react';

import { Badge } from '../../../components/base/Badge';
import { SecurityVerifyModal, type VerificationMethod } from '../../../components/auth/SecurityVerifyModal';
import { formatAmount } from '../../../utils/format';
import type { WalletAsset, WalletNetworkOption } from './mockWalletService';

type WalletWithdrawPanelProps = {
  asset: WalletAsset;
  network: WalletNetworkOption['value'];
  address: string;
  available: number;
  feeRate: number;
  flatFee: number;
  eta: string;
  riskReview: string;
  securityNote: string;
  assets: WalletAsset[];
  networks: WalletNetworkOption[];
  onAssetChange: (value: WalletAsset) => void;
  onNetworkChange: (value: WalletNetworkOption['value']) => void;
};

export function WalletWithdrawPanel({
  asset,
  network,
  address,
  available,
  feeRate,
  flatFee,
  eta,
  riskReview,
  securityNote,
  assets,
  networks,
  onAssetChange,
  onNetworkChange,
}: WalletWithdrawPanelProps) {
  const [withdrawAddress, setWithdrawAddress] = useState(address);
  const [amount, setAmount] = useState('1500');
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null);
  const [verifyOpen, setVerifyOpen] = useState(false);

  const networkInfo = networks.find((item) => item.value === network) ?? networks[0];
  const fee = useMemo(() => calculateFee(Number(amount), feeRate, flatFee), [amount, feeRate, flatFee]);
  const receiveAmount = useMemo(() => calculateReceiveAmount(Number(amount), fee), [amount, fee]);
  const validationError = validateWithdraw(amount, withdrawAddress, available);

  function handleSubmit() {
    if (validationError) {
      setMessage({ tone: 'error', text: validationError });
      return;
    }

    // 這裡先開驗證視窗，代表高風險動作需要再確認，不直接模擬成功扣款。
    setVerifyOpen(true);
  }

  function handleConfirm(input: { method: VerificationMethod; code: string }) {
    setVerifyOpen(false);
    if (input.code.trim().length < 4) {
      setMessage({ tone: 'error', text: 'Verification code is required.' });
      return;
    }

    // 訊息只是在前端標示 queued 狀態，正式版本必須由 server 與 ledger 流程決定最終結果。
    setMessage({
      tone: 'success',
      text: `Mock withdrawal queued with ${input.method.toUpperCase()} verification. OL flow will use server/ Java wallet API, risk API, and ledger API.`,
    });
  }

  const activeWhitelist = true;

  return (
    <>
      <section className="card">
        {/* 提領面板刻意保留安全檢查提示，避免未來把 mock flow 誤看成 production ready。 */}
        <h2 className="card__title">Withdraw</h2>
        <p className="wallet-page__meta">
          Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API.
        </p>

        <div className="wallet-security-grid wallet-security-grid--compact">
          <div className="wallet-security-item">
            <span className="wallet-security-item__label">2FA</span>
            <Badge tone="success">Enabled</Badge>
          </div>
          <div className="wallet-security-item">
            <span className="wallet-security-item__label">Whitelist</span>
            <Badge tone={activeWhitelist ? 'success' : 'warning'}>{activeWhitelist ? 'Enabled' : 'Disabled'}</Badge>
          </div>
          <div className="wallet-security-item">
            <span className="wallet-security-item__label">Risk review</span>
            <Badge tone="warning">{riskReview}</Badge>
          </div>
          <div className="wallet-security-item">
            <span className="wallet-security-item__label">ETA</span>
            <strong>{eta}</strong>
          </div>
          <div className="wallet-security-item">
            <span className="wallet-security-item__label">Memo / Tag</span>
            <Badge tone={networkInfo?.memoRequired ? 'warning' : 'neutral'}>
              {networkInfo?.memoRequired ? 'Required' : 'Optional'}
            </Badge>
          </div>
        </div>

        <div className="wallet-form wallet-form--withdraw">
          <label className="field">
            <span className="field__label">Asset</span>
            <select className="input" value={asset} onChange={(event) => onAssetChange(event.target.value as WalletAsset)}>
              {assets.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span className="field__label">Network</span>
            <select className="input" value={network} onChange={(event) => onNetworkChange(event.target.value as WalletNetworkOption['value'])}>
              {networks.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>

          <label className="field wallet-form__span-2">
            <span className="field__label">Withdraw Address</span>
            <input className="input" value={withdrawAddress} onChange={(event) => setWithdrawAddress(event.target.value)} placeholder="Enter destination address" />
          </label>

          <label className="field">
            <span className="field__label">Amount</span>
            <input className="input" inputMode="decimal" value={amount} onChange={(event) => setAmount(event.target.value)} />
          </label>

          <div className="wallet-summary-card">
            <span className="wallet-info-card__label">Available</span>
            <strong>{formatAmount(available, asset === 'USDT' ? 2 : 4)} {asset}</strong>
          </div>
          <div className="wallet-summary-card">
            <span className="wallet-info-card__label">Fee</span>
            <strong>{formatAmount(fee, asset === 'USDT' ? 2 : 4)} {asset}</strong>
          </div>
          <div className="wallet-summary-card">
            <span className="wallet-info-card__label">Receive amount</span>
            <strong>{formatAmount(receiveAmount, asset === 'USDT' ? 2 : 4)} {asset}</strong>
          </div>
        </div>

        <p className="wallet-warning">{securityNote}</p>
        <p className="wallet-risk-hint">{message ? message.text : validationError ?? 'Withdrawals remain a mock-only adapter workflow.'}</p>

        <div className="transfer-actions">
          <button className="primary-button" type="button" onClick={handleSubmit}>
            Submit withdrawal
          </button>
        </div>
      </section>

      <SecurityVerifyModal
        open={verifyOpen}
        title="Confirm withdrawal"
        description="Use a verification code to simulate a sensitive withdrawal step."
        confirmLabel="Submit withdrawal"
        onClose={() => setVerifyOpen(false)}
        onConfirm={handleConfirm}
      />
    </>
  );
}

// 費用計算只供 UI 預覽，實際扣款仍要由後端計算與落帳。
function calculateFee(amount: number, feeRate: number, flatFee: number) {
  if (!Number.isFinite(amount) || amount <= 0) return flatFee;
  return amount * feeRate + flatFee;
}

// 預估到帳金額只是一個顯示值，不是最終結算結果。
function calculateReceiveAmount(amount: number, fee: number) {
  if (!Number.isFinite(amount) || amount <= 0) return 0;
  return Math.max(amount - fee, 0);
}

// 前端驗證只擋住明顯錯誤，不能取代 server 的風控與資金檢查。
function validateWithdraw(amount: string, address: string, available: number) {
  const numericAmount = Number(amount);
  if (!address.trim()) return 'Withdraw address is required.';
  if (!Number.isFinite(numericAmount) || numericAmount <= 0) return 'Enter a valid withdraw amount.';
  if (numericAmount > available) return 'Withdraw amount cannot exceed available balance.';
  return null;
}
