import { Badge } from '../../components/base/Badge';
import { formatAmount } from '../../utils/format';
import type { AssetTabKey } from './mockAssetService';

const accountToLabel: Record<AssetTabKey, string> = {
  spot: 'Spot Account',
  futures: 'Futures Account',
  margin: 'Margin Account',
};

type AssetTransferPanelProps = {
  fromAccount: AssetTabKey;
  toAccount: AssetTabKey;
  asset: string;
  amount: string;
  assets: string[];
  available: number;
  message: { tone: 'success' | 'error'; text: string } | null;
  validationError: string | null;
  onFromAccountChange: (value: AssetTabKey) => void;
  onToAccountChange: (value: AssetTabKey) => void;
  onAssetChange: (value: string) => void;
  onAmountChange: (value: string) => void;
  onMaxAmount: () => void;
  onSubmit: () => void;
};

export function AssetTransferPanel({
  fromAccount,
  toAccount,
  asset,
  amount,
  assets,
  available,
  message,
  validationError,
  onFromAccountChange,
  onToAccountChange,
  onAssetChange,
  onAmountChange,
  onMaxAmount,
  onSubmit,
}: AssetTransferPanelProps) {
  return (
    <div className="card">
      <h2 className="card__title">Account Transfer</h2>
      <div className="transfer-panel">
        <label className="field">
          <span className="field__label">From Account</span>
          <select className="input" value={fromAccount} onChange={(event) => onFromAccountChange(event.target.value as AssetTabKey)}>
            {(Object.keys(accountToLabel) as AssetTabKey[]).map((key) => (
              <option key={key} value={key}>
                {accountToLabel[key]}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span className="field__label">To Account</span>
          <select className="input" value={toAccount} onChange={(event) => onToAccountChange(event.target.value as AssetTabKey)}>
            {(Object.keys(accountToLabel) as AssetTabKey[]).map((key) => (
              <option key={key} value={key} disabled={key === fromAccount}>
                {accountToLabel[key]}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span className="field__label">Asset</span>
          <select className="input" value={asset} onChange={(event) => onAssetChange(event.target.value)}>
            {assets.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span className="field__label">Amount</span>
          <input
            className="input"
            inputMode="decimal"
            value={amount}
            onChange={(event) => onAmountChange(event.target.value)}
            placeholder="0.00"
          />
        </label>
      </div>

      <div className="transfer-summary">
        <div className="transfer-summary__row">
          <span>Available</span>
          <strong>
            {formatAmount(available, getFractionDigits(asset))} {asset}
          </strong>
        </div>
        <div className="transfer-summary__row">
          <span>Max button</span>
          <button className="ghost-button" type="button" onClick={onMaxAmount}>
            Max
          </button>
        </div>
        <div className="transfer-summary__row">
          <span>Security check</span>
          <Badge tone="warning">2FA required</Badge>
        </div>
      </div>

      {validationError && !message ? <p className="form-message form-message--error">{validationError}</p> : null}
      {message ? <p className={`form-message form-message--${message.tone}`}>{message.text}</p> : null}

      <div className="transfer-actions">
        <button className="primary-button" type="button" onClick={onSubmit}>
          Submit transfer
        </button>
        <p className="auth-form__hint">
          Development adapter only. OL must use server/ Java transfer API and ledger-backed settlement.
        </p>
      </div>
    </div>
  );
}

function getFractionDigits(asset: string) {
  return asset === 'USDT' ? 2 : 4;
}
