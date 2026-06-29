import type { CSSProperties } from 'react';
import { formatAmount, formatCurrency, formatPercent } from '../../utils/format';
import type { AssetAccount, AssetRow, AssetTabKey } from './mockAssetService';

type AssetAccountTableProps = {
  account: AssetAccount;
};

export function AssetAccountTable({ account }: AssetAccountTableProps) {
  const columns = getAssetColumns(account.key);

  return (
    <div className="asset-overview-table">
      <div className="asset-overview-table__head" style={{ '--asset-columns': columns.gridTemplate } as CSSProperties}>
        {columns.labels.map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>
      <div className="asset-overview-table__body">
        {account.assets.map((asset) => (
          <div className="asset-overview-table__row" style={{ '--asset-columns': columns.gridTemplate } as CSSProperties} key={`${account.key}-${asset.asset}`}>
            {columns.renderers.map((renderer, index) => (
              <span key={`${asset.asset}-${columns.labels[index]}`}>{renderer(asset)}</span>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

function getAssetColumns(accountKey: AssetTabKey) {
  if (accountKey === 'futures') {
    return {
      gridTemplate: '1fr 1fr 1fr 1fr 1fr 1fr 0.9fr',
      labels: ['Asset', 'Wallet Balance', 'Available', 'Margin Used', 'Unrealized PnL', 'Equity', 'Action'],
      renderers: [
        (asset: AssetRow) => asset.asset,
        (asset: AssetRow) => formatAmount(asset.walletBalance ?? asset.total, 4),
        (asset: AssetRow) => formatAmount(asset.available, 4),
        (asset: AssetRow) => formatAmount(asset.marginUsed ?? 0, 4),
        (asset: AssetRow) => formatAmount(asset.unrealizedPnl ?? 0, 4),
        (asset: AssetRow) => formatAmount(asset.equity ?? asset.total, 4),
        () => <button className="secondary-button" type="button">Transfer</button>,
      ],
    } as const;
  }

  if (accountKey === 'margin') {
    return {
      gridTemplate: '1fr 1fr 1fr 1fr 1fr 1fr 0.9fr',
      labels: ['Asset', 'Available', 'Borrowed', 'Interest', 'Net Asset', 'Risk Ratio', 'Action'],
      renderers: [
        (asset: AssetRow) => asset.asset,
        (asset: AssetRow) => formatAmount(asset.available, 4),
        (asset: AssetRow) => formatAmount(asset.borrowed ?? 0, 4),
        (asset: AssetRow) => formatAmount(asset.interest ?? 0, 4),
        (asset: AssetRow) => formatAmount(asset.netAsset ?? asset.total, 4),
        (asset: AssetRow) => formatPercent(asset.riskRatio ?? 0, 1),
        () => <button className="secondary-button" type="button">Review</button>,
      ],
    } as const;
  }

  return {
    gridTemplate: '1fr 1fr 1fr 1fr 0.9fr',
    labels: ['Asset', 'Available', 'Frozen', 'Total', 'Estimated Value', 'Action'],
    renderers: [
      (asset: AssetRow) => asset.asset,
      (asset: AssetRow) => formatAmount(asset.available, 4),
      (asset: AssetRow) => formatAmount(asset.frozen, 4),
      (asset: AssetRow) => formatAmount(asset.total, 4),
      (asset: AssetRow) => formatCurrency(asset.estimatedValue),
      () => <button className="secondary-button" type="button">Transfer</button>,
    ],
  } as const;
}
