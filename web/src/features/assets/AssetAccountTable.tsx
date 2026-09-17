import { useI18n } from '../../i18n';
import { formatDecimalString } from '../../utils/format';
import type { AssetProjectionAccount } from './assetProjectionApi';

type AssetAccountTableProps = {
  account: AssetProjectionAccount;
};

export function AssetAccountTable({ account }: AssetAccountTableProps) {
  const { t } = useI18n();

  return (
    <div className="asset-overview-table">
      <div className="asset-overview-table__head">
        {['assets.columnAsset', 'assets.columnAvailable', 'assets.columnLocked', 'assets.columnTotal'].map((label) => (
          <span key={label}>{t(label)}</span>
        ))}
      </div>
      <div className="asset-overview-table__body">
        {account.items.map((asset) => (
          <div className="asset-overview-table__row" key={`${asset.accountId}-${asset.assetSymbol}`}>
            <span>
              <strong>{asset.assetSymbol}</strong>
              <small className="asset-overview-table__asset-name">{asset.assetDisplayName}</small>
            </span>
            <span>{formatDecimalString(asset.available)}</span>
            <span>{formatDecimalString(asset.locked)}</span>
            <span>{formatDecimalString(asset.total)}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
