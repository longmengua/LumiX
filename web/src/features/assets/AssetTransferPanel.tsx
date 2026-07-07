import { Badge } from '../../components/base/Badge';
import { useI18n } from '../../i18n';
import { formatAmount } from '../../utils/format';
import type { AssetTabKey } from './mockAssetService';

const accountToLabel: Record<AssetTabKey, string> = {
  spot: 'account.spotAccount',
  futures: 'account.futuresAccount',
  margin: 'account.marginAccount',
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
  const { t } = useI18n();

  return (
    <div className="card">
      {/* 這個面板只處理使用者輸入與前端驗證，不代表資金已實際轉移。 */}
      <h2 className="card__title">{t('assets.transferTitle')}</h2>
      <div className="transfer-panel">
        <label className="field">
          <span className="field__label">{t('account.fromAccount')}</span>
          <select className="input" value={fromAccount} onChange={(event) => onFromAccountChange(event.target.value as AssetTabKey)}>
            {(Object.keys(accountToLabel) as AssetTabKey[]).map((key) => (
              <option key={key} value={key}>
                {t(accountToLabel[key])}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span className="field__label">{t('account.toAccount')}</span>
          <select className="input" value={toAccount} onChange={(event) => onToAccountChange(event.target.value as AssetTabKey)}>
            {(Object.keys(accountToLabel) as AssetTabKey[]).map((key) => (
              <option key={key} value={key} disabled={key === fromAccount}>
                {t(accountToLabel[key])}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span className="field__label">{t('account.asset')}</span>
          <select className="input" value={asset} onChange={(event) => onAssetChange(event.target.value)}>
            {assets.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span className="field__label">{t('account.amount')}</span>
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
          <span>{t('assets.walletAvailable')}</span>
          <strong>
            {formatAmount(available, getFractionDigits(asset))} {asset}
          </strong>
        </div>
        <div className="transfer-summary__row">
          <span>{t('assets.maxButton')}</span>
          <button className="ghost-button" type="button" onClick={onMaxAmount}>
            {t('assets.maxButton')}
          </button>
        </div>
        <div className="transfer-summary__row">
          <span>{t('assets.securityCheck')}</span>
          <Badge tone="warning">{t('assets.twoFaRequired')}</Badge>
        </div>
      </div>

      {validationError && !message ? <p className="form-message form-message--error">{validationError}</p> : null}
      {message ? <p className={`form-message form-message--${message.tone}`}>{message.text}</p> : null}

      <div className="transfer-actions">
        <button className="primary-button" type="button" onClick={onSubmit}>
          {t('account.submitTransfer')}
        </button>
        <p className="auth-form__hint">{t('assets.walletWorkspacesHint')}</p>
      </div>
    </div>
  );
}

// USDT 使用 2 位小數，其餘資產保留較細精度，避免前端顯示誤導。
function getFractionDigits(asset: string) {
  return asset === 'USDT' ? 2 : 4;
}
