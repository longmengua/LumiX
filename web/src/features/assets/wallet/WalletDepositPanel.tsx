import type { ReactNode } from 'react';
import { useState } from 'react';

import { Badge } from '../../../components/base/Badge';
import type { WalletAsset, WalletNetworkOption } from './mockWalletService';

type WalletDepositPanelProps = {
  asset: WalletAsset;
  network: WalletNetworkOption['value'];
  address: string;
  memoTag: string;
  minimumDeposit: number;
  confirmationsRequired: number;
  riskHint: string;
  assets: WalletAsset[];
  networks: WalletNetworkOption[];
  onAssetChange: (value: WalletAsset) => void;
  onNetworkChange: (value: WalletNetworkOption['value']) => void;
  children: ReactNode;
};

export function WalletDepositPanel({
  asset,
  network,
  address,
  memoTag,
  minimumDeposit,
  confirmationsRequired,
  riskHint,
  assets,
  networks,
  onAssetChange,
  onNetworkChange,
  children,
}: WalletDepositPanelProps) {
  const [copyState, setCopyState] = useState<'idle' | 'copied'>('idle');

  async function handleCopy() {
    // 複製地址只是 UX 輔助；真正的入金確認必須靠鏈上與後端入帳流程。
    try {
      await navigator.clipboard.writeText(address);
      setCopyState('copied');
      window.setTimeout(() => setCopyState('idle'), 1200);
    } catch {
      setCopyState('idle');
    }
  }

  const activeNetwork = networks.find((item) => item.value === network) ?? networks[0];

  return (
    <div className="grid-split">
      <section className="card">
        {/* 入金面板只展示地址、網路與確認數，不能把它當成已入帳。 */}
        <h2 className="card__title">Deposit</h2>
        <p className="wallet-page__meta">
          Development adapter only. OL before must connect server/ Java wallet API, risk API, and ledger API.
        </p>

        <div className="wallet-form">
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
        </div>

        <div className="wallet-address-card">
          <div className="wallet-address-card__qr">QR Code Placeholder</div>
          <div className="wallet-address-card__details">
            <div className="wallet-address-card__row">
              <span>Deposit Address</span>
              <button className="ghost-button" type="button" onClick={handleCopy}>
                {copyState === 'copied' ? 'Copied' : 'Copy address'}
              </button>
            </div>
            <strong className="wallet-address-card__address">{address}</strong>
            {activeNetwork?.memoRequired ? <p className="wallet-warning">Memo / tag required for this network.</p> : <p className="wallet-warning">{memoTag}</p>}
          </div>
        </div>

        <div className="wallet-info-grid">
          <div className="wallet-info-card">
            <span className="wallet-info-card__label">Minimum deposit</span>
            <strong>{minimumDeposit} {asset}</strong>
          </div>
          <div className="wallet-info-card">
            <span className="wallet-info-card__label">Confirmations required</span>
            <strong>{confirmationsRequired}</strong>
          </div>
          <div className="wallet-info-card">
            <span className="wallet-info-card__label">Network memo</span>
            <Badge tone={activeNetwork?.memoRequired ? 'warning' : 'neutral'}>{activeNetwork?.memoRequired ? 'Required' : 'Optional'}</Badge>
          </div>
        </div>

        <p className="wallet-risk-hint">{riskHint}</p>
      </section>

      <section className="card">
        {/* 最近入金清單是 adapter 驅動的預覽資料，不是鏈上 callback 的證明。 */}
        <h2 className="card__title">Recent Deposits</h2>
        <p className="wallet-page__meta">Adapter-driven preview of recent activity. No real chain callback is performed here.</p>
        {children}
      </section>
    </div>
  );
}
