import { AssetAccountPage } from '../../features/assets/AssetAccountPage';

export function FuturesAssetsPage() {
  return (
    <AssetAccountPage
      accountKey="futures"
      title="Futures Account"
      description="Futures wallet balance, margin used, and unrealized PnL are tracked separately."
      summaryTitle="Futures Snapshot"
      summaryPoints={[
        { label: 'Wallet Balance', value: '$33,311.25 USDT' },
        { label: 'Margin Used', value: '$22,040.11 USDT' },
        { label: 'Unrealized PnL', value: '+$860.52 USDT' },
      ]}
    />
  );
}
