import { AssetAccountPage } from '../../features/assets/AssetAccountPage';

export function MarginAssetsPage() {
  return (
    <AssetAccountPage
      accountKey="margin"
      title="Margin Account"
      description="Borrowed balances, accrued interest, and risk ratio remain isolated from spot and futures."
      summaryTitle="Margin Snapshot"
      summaryPoints={[
        { label: 'Net Asset', value: '$21,120.50 USDT' },
        { label: 'Borrowed', value: '$6,800.05 USDT' },
        { label: 'Risk Ratio', value: '62.4%' },
      ]}
    />
  );
}
